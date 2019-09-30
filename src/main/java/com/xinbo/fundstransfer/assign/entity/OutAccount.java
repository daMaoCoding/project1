package com.xinbo.fundstransfer.assign.entity;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.assign.Constants;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.enums.InBankSubType;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.AllocatingOutwardTask;
import com.xinbo.fundstransfer.domain.pojo.UserCategory;
import com.xinbo.fundstransfer.runtime.MemCacheUtils;
import com.xinbo.fundstransfer.service.AllocateTransferService;
import com.xinbo.fundstransfer.service.impl.AllocateOutwardTaskServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 出款卡
 * 
 * @author Administrator
 *
 */
@Slf4j
public abstract class OutAccount extends Account {
	/**
	 * 十秒内停止接单的用户
	 */
	private static final Cache<Integer, Integer> SUSPEND_OPERATOR = CacheBuilder.newBuilder().maximumSize(5000)
			.expireAfterWrite(20, TimeUnit.MINUTES).build();

	private static final Cache<Integer, Integer> SUSPEND_ACCOUNT = CacheBuilder.newBuilder().maximumSize(5000)
			.expireAfterWrite(20, TimeUnit.MINUTES).build();

	private static volatile Map<String, Long[]> BANK_TYPE_HALT_SERVICE = new ConcurrentHashMap<>();

	/*
	 * 用户 TARGET_TYPE_USER = 0 （2个）, 机器 TARGET_TYPE_ROBOT = 1（19）, TARGET_TYPE_START
	 * = 2, 第三方 TARGET_TYPE_THIRD = 3（2）, 手机 TARGET_TYPE_MOBILE = 4（211）
	 */
	protected int outType;

	public OutAccount(int outType, AccountBaseInfo info, int accountId, int levelId, int type, int zone, int handicapId,
			int amount) {
		super(info, accountId, levelId, type, zone, handicapId, amount);
		this.outType = outType;
	}
	public int getOutType(){
		return this.outType;
	}

	/**
	 * 分配出款任务
	 * 
	 * @param task
	 * @return
	 */
	public AllocatingOutwardTask allocate(AllocatingOutwardTask task, Integer level, Boolean peerTrans, int maxRound, String tarBanks) {
		task.setTarget(String.format(Constants.QUEUE_FORMAT_USER_OR_ROBOT, this.outType, this.accountId));
		AllocatingOutwardTask allocatedTask = setFromIdOperator(task, level, peerTrans, tarBanks);
		if (allocatedTask != null) {
			setScore(allocatedTask, maxRound);
		}
		return allocatedTask;
	}

	private void setScore(AllocatingOutwardTask task, int maxRound) {
		log.debug("setScore>> allocate task {}", task.getTaskId());
		if (task.getFromId() != null && task.getOperator() == null) {
			Double score = redisService.getStringRedisTemplate().boundZSetOps(Constants.OUT_ACCOUNT_ORDERED)
					.score(task.getFromId().toString());
			if (score == null) {
				score = 0d;
			}
			Object outedObj = redisService.getFloatRedisTemplate().boundHashOps(Constants.OUT_ACCOUNT_MONEY)
					.get(task.getFromId().toString());
			Float outedMoney = 0f;
			if (outedObj != null) {
				outedMoney = (Float) outedObj;
			}
			Integer margin = 0;
			if (CommonUtils.outwardIsBankLogUniformAllocation()) {
				margin = accountChangeService.creditLimit(this.accountBaseInfo);
			} else {
				margin = CommonUtils.outwardFixedAmount();
			}
			int bankTypeRound = score.intValue();
			int round = bankTypeRound > 10 ? Integer.parseInt((bankTypeRound+"").substring(1)) : 0;
			float outed = outedMoney.floatValue() + task.getTaskAmount().floatValue();
			if (outed > margin) {
				round++;
				outed = 0f;
				if (maxRound > round + 1) {
					log.debug("account outtask round change from {} to {}", round, maxRound);
					round = maxRound;
				}
			}
			// 账号优先级：云闪付入款卡 > 先入后出入款卡 > 出款卡
			int bankTypeOrder = 3;
			if (this.accountBaseInfo == null) {
				bankTypeOrder = 1;
			} else {
				if (ObjectUtils.isEmpty(this.accountBaseInfo.getType())) {
					bankTypeOrder = 3;
				} else if (AccountType.InBank.getTypeId().equals(this.accountBaseInfo.getType())) {
					round = 0;
					if(ObjectUtils.isEmpty(this.accountBaseInfo.getSubType()) ) {
						bankTypeOrder = 2;
					} else {
						if(this.accountBaseInfo.getSubType().equals(InBankSubType.IN_BANK_YSF.getSubType())) {
							bankTypeOrder = 1;	
						} else {
							bankTypeOrder = 2;
						}
					}
				}
				
			}
			redisService.getStringRedisTemplate().boundZSetOps(Constants.OUT_ACCOUNT_ORDERED).add(task.getFromId().toString(),
					Double.valueOf(String.format("%d%d.%d", bankTypeOrder, round, System.currentTimeMillis())));
			redisService.getFloatRedisTemplate().boundHashOps(Constants.OUT_ACCOUNT_MONEY).put(task.getFromId().toString(), outed);

			Long nextExpire = CommonUtils.getExpireTime4AmountDaily() - System.currentTimeMillis();
			Long hisExpire = redisService.getStringRedisTemplate().boundZSetOps(Constants.OUT_ACCOUNT_ORDERED).getExpire();
			if (hisExpire != null && hisExpire < 0) {// 当日清零
				redisService.getStringRedisTemplate().expire(Constants.OUT_ACCOUNT_ORDERED, nextExpire, TimeUnit.MILLISECONDS);
			}
			Long hisExpire1 = redisService.getFloatRedisTemplate().boundHashOps(Constants.OUT_ACCOUNT_MONEY).getExpire();
			if (hisExpire1 != null && hisExpire1 < 0) {// 当日清零
				redisService.getFloatRedisTemplate().expire(Constants.OUT_ACCOUNT_MONEY, nextExpire, TimeUnit.MILLISECONDS);
			}
			
		}
	}

	abstract AllocatingOutwardTask setFromIdOperator(AllocatingOutwardTask task, Integer level, Boolean peerTrans, String tarBanks);

	/**
	 * 是否满足出款任务
	 * 
	 * @param task
	 * @param level
	 * @param refund
	 *            true：只找返利网的卡；false：只找非返利网的卡
	 * @return Constants.NOT_OK -1 不满足条件 / Constants.OK 1 满足条件 /
	 *         Constants.OK_BUT_BALANCE 2 PC端余额不足但是满足其它条件
	 */
	public int canOutTask(AllocatingOutwardTask task, Integer level) {
		int result = checkCanOutTask(task, level);
		return result;
	}

	/**
	 * 子类检查是否能否出款
	 * 
	 * @return
	 */
	abstract int checkCanOutTask(AllocatingOutwardTask task, Integer level);

	public abstract Integer getManualOut();
	
	/**
	 * 检测 银行类别
	 */
	protected boolean checkBank(String tarBanks, AccountBaseInfo p) {
		return (StringUtils.isBlank(tarBanks) || tarBanks.contains(p.getBankType())) && checkBank(p.getBankType());
	}

	protected boolean checkBank(String bankType) {
		if (!CommonUtils.isEnableCheckBank()) {
			return true;
		}
		if (StringUtils.isBlank(bankType) || !"民生银行,交通银行,华夏银行,建设银行,中国银行,工商银行,内蒙古银行,农业银行".contains(bankType))
			return true;
		Long[] stTmEdTm = BANK_TYPE_HALT_SERVICE.get(bankType);
		Long stTm = null, edTm = null;
		if (Objects.nonNull(stTmEdTm) && stTmEdTm.length >= 2) {
			stTm = stTmEdTm[0];
			edTm = stTmEdTm[1];
		}
		long curr = System.currentTimeMillis();
		if (stTm != null && edTm != null && curr < edTm)
			return curr < stTm;
		Calendar cal = Calendar.getInstance();
		int stD = 0, stH = 0, stM = 0, stS = 0, edD = 0, edH = 0, edM = 0, edS = 0;
		if ("民生银行".contains(bankType)) {// 0:00:00-02:00:00->~-02:04:59
			stD = cal.get(Calendar.DAY_OF_YEAR) - 1;
			stH = 23;
			stM = 56;
			stS = 59;
			edD = cal.get(Calendar.DAY_OF_YEAR);
			edH = 2;
			edM = 4;
			edS = 59;
		} else if ("交通银行".contains(bankType)) {// 00:00:00-01:00:00->~-01:04:59
			stD = cal.get(Calendar.DAY_OF_YEAR) - 1;
			stH = 23;
			stM = 56;
			stS = 59;
			edD = cal.get(Calendar.DAY_OF_YEAR);
			edH = 1;
			edM = 4;
			edS = 59;
		} else if ("华夏银行".contains(bankType)) {// 21:30:00-24:00:00->21:26:59-00:04:59
			stD = cal.get(Calendar.DAY_OF_YEAR);
			stH = 21;
			stM = 26;
			stS = 59;
			edD = cal.get(Calendar.DAY_OF_YEAR) + 1;
			edH = 0;
			edM = 4;
			edS = 59;
		} else if ("建设银行".contains(bankType)) {// 22:00:00-24:00:00->21:56:59-00:04:59
			stD = cal.get(Calendar.DAY_OF_YEAR);
			stH = 21;
			stM = 56;
			stS = 59;
			edD = cal.get(Calendar.DAY_OF_YEAR) + 1;
			edH = 0;
			edM = 4;
			edS = 59;
		} else if ("工商银行".contains(bankType)) {// 23:30:00-24:00:00->23:26:59-00:04:59
			stD = cal.get(Calendar.DAY_OF_YEAR);
			stH = 23;
			stM = 26;
			stS = 59;
			edD = cal.get(Calendar.DAY_OF_YEAR) + 1;
			edH = 0;
			edM = 4;
			edS = 59;
		} else if ("内蒙古银行".contains(bankType)) {// 20:30:00-24:00:00->20:26:59-00:04:59
			stD = cal.get(Calendar.DAY_OF_YEAR);
			stH = 20;
			stM = 26;
			stS = 59;
			edD = cal.get(Calendar.DAY_OF_YEAR) + 1;
			edH = 0;
			edM = 4;
			edS = 59;
		} else if ("中国银行".contains(bankType)) {// 22:00:00-03:00:00->21:56:59-03:04:59
			stD = cal.get(Calendar.DAY_OF_YEAR);
			stH = 21;
			stM = 56;
			stS = 59;
			edD = cal.get(Calendar.DAY_OF_YEAR) + 1;
			edH = 3;
			edM = 4;
			edS = 59;
		} else if ("农业银行".contains(bankType)) {// 下午4点、晚上12点整，银行维护
			stD = cal.get(Calendar.DAY_OF_YEAR);// 下午4点
			stH = 15;
			stM = 56;
			stS = 59;
			edD = cal.get(Calendar.DAY_OF_YEAR);
			edH = 16;
			edM = 4;
			edS = 59;
		}
		cal.set(Calendar.DAY_OF_YEAR, stD);
		cal.set(Calendar.HOUR_OF_DAY, stH);
		cal.set(Calendar.MINUTE, stM);
		cal.set(Calendar.SECOND, stS);
		stTm = cal.getTimeInMillis();
		cal.set(Calendar.DAY_OF_YEAR, edD);
		cal.set(Calendar.HOUR_OF_DAY, edH);
		cal.set(Calendar.MINUTE, edM);
		cal.set(Calendar.SECOND, edS);
		edTm = cal.getTimeInMillis();
		if (edTm <= curr) {
			if ("农业银行".contains(bankType)) {// 晚上12点整
				cal = Calendar.getInstance();
				cal.set(Calendar.HOUR_OF_DAY, 23);
				cal.set(Calendar.MINUTE, 56);
				cal.set(Calendar.SECOND, 59);
				stTm = cal.getTimeInMillis();
				cal.add(Calendar.DAY_OF_YEAR, 1);
				cal.set(Calendar.HOUR_OF_DAY, 0);
				cal.set(Calendar.MINUTE, 4);
				cal.set(Calendar.SECOND, 59);
				edTm = cal.getTimeInMillis();
			} else {
				stTm = stTm + 86400000;
				edTm = edTm + 86400000;
			}
		}
		if (Objects.isNull(stTmEdTm)) {
			BANK_TYPE_HALT_SERVICE.put(bankType, new Long[] { stTm, edTm });
		} else {
			stTmEdTm[0] = stTm;
			stTmEdTm[1] = edTm;
		}
		return curr < stTm;
	}

	protected String buildAllocateStrategy() {
		return MemCacheUtils.getInstance().getSystemProfile().getOrDefault("OUTWARD_TASK_ALLOCATE_STRATEGY", "0");
	}

	/**
	 * 检测 账号
	 */
	protected boolean checkRobot(int accountId) {
		AccountBaseInfo base = accountService.getFromCacheById(accountId);
		Integer type = base.getType();
		boolean isEnableIn = CommonUtils.checkEnableInBankHandicap(base.getHandicapId());
		return base != null && base.getHolder() == null && type != null
				&& AccountStatus.Normal.getStatus().equals(base.getStatus())
				&& (Constants.OUTBANK == type || Constants.OUTTHIRD == type
						|| (isEnableIn && (Constants.RESERVEBANK == type || Constants.INBANK == type
								|| Constants.BINDCUSTOMER == type)));
	}

	/**
	 * 检测 用户
	 */
	protected boolean checkManual(int userId) {
		SysUser user = userService.findFromCacheById(userId);
		return user != null && user.getCategory() != null
				&& com.xinbo.fundstransfer.domain.pojo.UserCategory.Outward.getCode() == user.getCategory()
				&& userService.online(userId) && SUSPEND_OPERATOR.getIfPresent(userId) == null;
	}

	/**
	 * 检测 账号
	 */
	protected boolean checkMobile(int accountId) {
		AccountBaseInfo base = accountService.getFromCacheById(accountId);
		Integer type = base.getType();
		return type != null && AccountStatus.Normal.getStatus().equals(base.getStatus())
				&& (Constants.OUTBANK == type || Constants.OUTTHIRD == type || Constants.INBANK == type
						|| Constants.RESERVEBANK == type || Constants.BINDCUSTOMER == type)
				&& base.getFlag() != null && (base.getFlag() == 1 || base.getFlag() == 2)
				&& SUSPEND_ACCOUNT.getIfPresent(accountId) == null;
	}

	/**
	 * 检测 第三方出款用户
	 */
	protected boolean checkThird(int userId) {
		SysUser user = userService.findFromCacheById(userId);
		return AppConstants.OUTDREW_THIRD && user != null && user.getCategory() != null
				&& UserCategory.Finance.getCode() == user.getCategory() && userService.online(userId)
				&& SUSPEND_OPERATOR.getIfPresent(userId) == null;
	}

	/**
	 * 检测 内外层
	 */
	protected boolean checkLevel(int lTask, AccountBaseInfo acnt, boolean isMunal, boolean mergeLevel) {
		if (isMunal)
			return true;
		Integer levelP = acnt.getCurrSysLevel() == null ? Constants.OUTTER : acnt.getCurrSysLevel();
		return mergeLevel
				? ((lTask & levelP) == Constants.OUTTER || (lTask & levelP) == Constants.DESIGNATED
						|| lTask != Constants.OUTTER && levelP != Constants.OUTTER && lTask != Constants.DESIGNATED
								&& levelP != Constants.DESIGNATED)
				: ((lTask & levelP) == Constants.OUTTER || (lTask & levelP) == Constants.INNER
						|| (lTask & levelP) == Constants.MIDDLE || (lTask & levelP) == Constants.DESIGNATED);
	}

	/**
	 * 检测银行间转账
	 *
	 * @param peerTrans
	 *            toBank是否处于同行转账模式
	 * @param fromBank
	 *            汇出银行
	 * @param toBank
	 *            汇入银行
	 * @return true:fromBank可以转账到toBank；false:fromBank不可以转账到toBank
	 */
	protected boolean checkTrans(Boolean peerTrans, String fromBank, String toBank) {
		return peerTrans == null || (peerTrans && StringUtils.isNotBlank(fromBank) && StringUtils.isNotBlank(toBank))
				&& (toBank.contains(fromBank) || fromBank.equals("云南农信") && "云南省农村信用社".contains(toBank));
	}

	/**
	 * 检测 防止该出款卡连续给同一银行卡出相同金额
	 *
	 * @param last
	 *            上次出款记录集
	 * @param frId
	 *            出款卡ID
	 * @param acc
	 *            出款账号
	 * @param amt
	 *            出款金额
	 * @return true:可以出款；false:不可以出款
	 */
	protected boolean checkTask(Map<Object, Object> last, Integer frId, String acc, Float amt) {
		if (last != null && frId != null && acc != null && amt != null) {
			Object val = last.get(String.valueOf(frId));
			return !(val != null && val.toString().endsWith(acc + ":" + amt.intValue()));
		}
		return true;
	}

	/**
	 * 检测出款账号单笔最高/最低限额
	 *
	 * @param acc
	 *            账号
	 * @param tasking
	 *            待分配任务
	 * @return true:可以出款;false:不可以出款
	 */
	protected boolean checkLimitOutOne(AllocatingOutwardTask tasking, AccountBaseInfo acnt) {
		return (!(acnt.getLimitOutOne() != null && acnt.getLimitOutOne() > 0
				&& (((acnt.getFlag() != null && acnt.checkMobile()) && acnt.getLimitOutOne() < tasking.getTaskAmount())
						|| (!acnt.checkMobile() && (acnt.getLimitOutOne() + 5000) <= tasking.getTaskAmount()))))
				&& (!(acnt != null && acnt.getLimitOutOneLow() != null && acnt.getLimitOutOneLow() > 0
						&& acnt.getLimitOutOneLow() > tasking.getTaskAmount()));
	}

	/**
	 * 下发黑名单检测
	 *
	 * @param accId
	 *            账号
	 * @param bal
	 *            accId余额
	 * @param tasking
	 *            待分配任务
	 * @param black
	 *            黑名单列表
	 * @return true:通过检测；</br>
	 *         false:未通过检测</br>
	 */
	protected boolean checkBlack(int accId, int bal, AllocatingOutwardTask tasking, Set<String> black) {
		return (checkBlack(black, AllocateTransferService.WILD_CARD_ACCOUNT, accId) || (bal > tasking.getTaskAmount()))
				&& checkBlack(black, accId, tasking.getTaskId());
	}

	private boolean checkBlack(Set<String> black, int frId, int toId) {
		boolean ret = CollectionUtils.isEmpty(black) || (black.stream()
				.filter(p -> p.startsWith(RedisKeys.gen4TransBlack(Constants.WILD_CARD_ACCOUNT, toId, 0))
						|| p.startsWith(RedisKeys.gen4TransBlack(frId, toId, 0)))
				.count() == 0);
		return ret;
	}

	private boolean checkBlack(Set<String> black, int frAcc, long taskId) {
		boolean ret = CollectionUtils.isEmpty(black)
				|| (black.stream().filter(p -> p.startsWith(RedisKeys.gen4TransBlack(Constants.WILD_CARD_ACCOUNT, taskId, 0))
						|| p.startsWith(RedisKeys.gen4TransBlack(frAcc, taskId, 0))).count() == 0);
		return ret;
	}

	protected List<BizAccount> checkOutAcc4Manual(boolean distHandi, AllocatingOutwardTask allocating,
			List<BizAccount> accList) {
		if (accList == null || accList.size() == 0) {
			return null;
		}
		if (distHandi) {
			accList = accList.stream()
					.filter(p -> Objects.isNull(p.getHandicapId())
							|| Objects.equals(p.getHandicapId(), Constants.WILD_CARD_HANDICAP)
							|| Objects.equals(p.getHandicapId(), allocating.getHandicap()))
					.collect(Collectors.toList());
		}
		return accList;
	}

	protected boolean checkAllocateStrategy(String allocStrategy, int tarType) {
		/**
		 * 1、OUTTASK_ALLOCATE_AMT_FIRST PC和手机都可以出款<br>
		 * 2、OUTTASK_ALLOCATE_PC_FIRST PC和手机都可以出款<br>
		 * 3、OUTTASK_ALLOCATE_MOBILE_FIRSTPC和手机都可以出款<br>
		 * 4、OUTTASK_ALLOCATE_ONLY_PC 仅PC可以出款<br>
		 * 5、OUTTASK_ALLOCATE_ONLY_MOBILE 仅手机可以出款<br>
		 */
		return ((AppConstants.OUTTASK_ALLOCATE_AMT_FIRST.equals(allocStrategy)
				&& (Constants.TARGET_TYPE_ROBOT == tarType || Constants.TARGET_TYPE_MOBILE == tarType))
				|| ((AppConstants.OUTTASK_ALLOCATE_PC_FIRST.equals(allocStrategy)
						|| AppConstants.OUTTASK_ALLOCATE_MOBILE_FIRST.equals(allocStrategy))
						&& (Constants.TARGET_TYPE_ROBOT == tarType || Constants.TARGET_TYPE_MOBILE == tarType))
				|| (AppConstants.OUTTASK_ALLOCATE_ONLY_PC.equals(allocStrategy)
						&& Constants.TARGET_TYPE_ROBOT == tarType)
				|| (AppConstants.OUTTASK_ALLOCATE_ONLY_MOBILE.equals(allocStrategy)
						&& Constants.TARGET_TYPE_MOBILE == tarType));
	}

	/**
	 * check if <tt>MergeLevel</tt> program already started.
	 *
	 * @return <code>true</code> if the <tt>MergeLevel</tt> program already
	 *         started,otherwise return <code>false</code>
	 * @see this#setMergeLevel(boolean, int, boolean, int) ;
	 * @see this#getMergeLevel(int)
	 */
	protected boolean checkMergeLevel(AllocatingOutwardTask tasking, Integer l) {
		Cache<String, Long> MERGE_LEVEL = AllocateOutwardTaskServiceImpl.MERGE_LEVEL_SET[handicapService
				.findZoneByHandiId(tasking.getZone())];
		return l != Constants.OUTTER && l != Constants.DESIGNATED
				&& Objects.equals(tasking.getManualOut(), Constants.ROBOT_OUT_YES)
				&& (Objects.nonNull(MERGE_LEVEL)
						&& Objects.nonNull(MERGE_LEVEL.getIfPresent(Constants.ALLOC_MERGE_LEVEL_LAST_TIME))
						&& Objects.nonNull(MERGE_LEVEL.getIfPresent(Constants.ALLOC_MERGE_LEVEL_DEADLINE)));
	}

	/**
	 * 检测用户是否有盘口的数据权限
	 *
	 * @param userId
	 * @param handicap
	 * @return
	 */
	protected boolean checkUserDataPermission(Integer userId, Integer handicap) {
		if (userId == null) {
			return false;
		}
		String userHandicap = sysDataPermissionService.findUserHandicapFromCache(userId);
		log.debug("Alloc checkUserData userId {} handicap {} data right {}", userId, handicap, userHandicap);
		return userHandicap.contains(";" + handicap + ";");
	}
	
	@Override
	public String toString() {
		return "OutAccount [outType=" + outType + ", accountId=" + accountId + ", levelId=" + levelId + ", type=" + type + ", zone=" + zone + ", handicapId=" + handicapId
				+ ", amount=" + amount + "]";
	}
}
