package com.xinbo.fundstransfer.assign;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.assign.entity.Account;
import com.xinbo.fundstransfer.assign.entity.InAccount;
import com.xinbo.fundstransfer.assign.entity.OutAccount;
import com.xinbo.fundstransfer.assign.entity.OutOnly;
import com.xinbo.fundstransfer.assign.entity.ReserveAccount;
import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.service.AccountChangeService;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.AllocateOutwardTaskService;
import com.xinbo.fundstransfer.service.HandicapService;
import com.xinbo.fundstransfer.service.RedisService;
import com.xinbo.fundstransfer.service.SysDataPermissionService;
import com.xinbo.fundstransfer.service.SysUserService;

@Component
public class AccountFactory {
	@Autowired @Lazy
	private AccountService accountService;
	@Autowired
	protected RedisService redisService;
	@Autowired
	protected HandicapService handicapService;
	@Autowired
	protected SysUserService userService;
	@Autowired
	protected SysDataPermissionService sysDataPermissionService;
	@Autowired
	protected AllocateOutwardTaskService allOTaskSer;	
	@Autowired
	protected OtherCache otherCache;
	@Autowired
	private AccountChangeService accountChangeService;
	@Value("${funds.transfer.version}")
	private String CURR_VERSION;	
	
	/**
	 * 从redis的AllocatingOutwardTaskTarget_中的记录来生成出款卡，用于分配出款任务
	 * @param get
	 * @return
	 */
	public Optional<OutAccount> generateOutAccount4Out(ZSetOperations.TypedTuple<String> info) {
		if(ObjectUtils.isEmpty(info.getValue()) || ObjectUtils.isEmpty(info.getScore())) {
			return Optional.empty();
		}
		String[] keys = info.getValue().split(":");
		Integer accountId = Integer.parseInt(keys[1]);
		Integer outType = Integer.parseInt(keys[0]);
		AccountBaseInfo base = null;
		if(outType != Constants.TARGET_TYPE_USER && outType != Constants.TARGET_TYPE_THIRD) {
			base = accountService.getFromCacheById(accountId);
		}
		// [0]tarVal[1]内层/外层/中层[2]tarType[3]区域[4]handicapId[5]amount
		int[] inf = descore(outType, accountId, info.getScore());
		if(ObjectUtils.isEmpty(accountId) || ObjectUtils.isEmpty(inf[1]) || ObjectUtils.isEmpty(inf[2]) || ObjectUtils.isEmpty(inf[3]) || ObjectUtils.isEmpty(inf[4]) || ObjectUtils.isEmpty(inf[5]) ) {
			return Optional.empty();
		}		
		//base, accountId, levelId, type, zone, handicapId, amount		
		Optional<OutAccount> accountOpt = new Account().generate(outType, base, accountId, inf[1], inf[2], inf[3], inf[4], inf[5]);
		if(accountOpt.isPresent()) {
			OutAccount account = accountOpt.get();
			account.init(otherCache, accountService, redisService, handicapService, userService, sysDataPermissionService, allOTaskSer, CURR_VERSION, accountChangeService);
			return Optional.of(account);
		}
		return Optional.empty();
	}
	
	/**
	 * 从 redis的AllocApplyByFrom中的记录来生成入款卡，将该入款卡的余额下发到合适的出款卡
	 * @param info
	 * @return
	 */		
	public Optional<InAccount> generateInAccount(ZSetOperations.TypedTuple<String> info) {
		if(ObjectUtils.isEmpty(info.getValue()) || ObjectUtils.isEmpty(info.getScore())) {
			return Optional.empty();
		}
		Integer accountId = Integer.parseInt(info.getValue());
		AccountBaseInfo base = accountService.getFromCacheById(accountId);
		// [0]type [1]zone [2]level [3]handicap [4]bal
		Integer[] inf = deScore4Fr(info.getScore());
		if(ObjectUtils.isEmpty(base) || ObjectUtils.isEmpty(accountId) || ObjectUtils.isEmpty(inf[2]) || ObjectUtils.isEmpty(inf[0]) || ObjectUtils.isEmpty(inf[1]) || ObjectUtils.isEmpty(inf[3]) || ObjectUtils.isEmpty(inf[4]) ) {
			return Optional.empty();
		}
		if(!base.getStatus().equals(AccountStatus.Normal.getStatus())) {
			return Optional.empty();
		}
		Optional<Account> accountOpt = new Account().generate(base, accountId, inf[2], inf[0], inf[1], inf[3], inf[4], redisService);
		if(accountOpt.isPresent() && accountOpt.get() instanceof InAccount) {
			InAccount account = (InAccount) accountOpt.get();
			account.init(otherCache, accountService, redisService, handicapService, userService, sysDataPermissionService, allOTaskSer, CURR_VERSION, accountChangeService);
			return Optional.of(account);
		} 
		return Optional.empty();
	}

	/**
	 * 从 redis的AllocApplyByFrom中的记录来生成备用卡，将该备用卡的余额下发到合适的出款卡
	 * @param info
	 * @return
	 */
	public Optional<ReserveAccount> generateReserveAccount(ZSetOperations.TypedTuple<String> info) {
		if(ObjectUtils.isEmpty(info.getValue()) || ObjectUtils.isEmpty(info.getScore())) {
			return Optional.empty();
		}
		Integer accountId = Integer.parseInt(info.getValue());
		AccountBaseInfo base = accountService.getFromCacheById(accountId);
		// [0]type [1]zone [2]level [3]handicap [4]bal
		Integer[] inf = deScore4Fr(info.getScore());
		if(ObjectUtils.isEmpty(base) || ObjectUtils.isEmpty(accountId) || ObjectUtils.isEmpty(inf[2]) || ObjectUtils.isEmpty(inf[0]) || ObjectUtils.isEmpty(inf[1]) || ObjectUtils.isEmpty(inf[3]) || ObjectUtils.isEmpty(inf[4]) ) {
			return Optional.empty();
		}		
		if(!base.getStatus().equals(AccountStatus.Normal.getStatus())) {
			return Optional.empty();
		}
		Optional<Account> accountOpt = new Account().generate(base, accountId, inf[2], inf[0], inf[1], inf[3], inf[4], redisService);
		if(accountOpt.isPresent() && accountOpt.get() instanceof ReserveAccount) {
			ReserveAccount account = (ReserveAccount) accountOpt.get();
			account.init(otherCache, accountService, redisService, handicapService, userService, sysDataPermissionService, allOTaskSer, CURR_VERSION, accountChangeService);
//			account.calculateOverflow();
			return Optional.of(account);
		}
		return Optional.empty();
	}

	/**
	 * 从 redis的AllocApplyByFrom中的记录来生成出款卡，用在将大额入款卡的全部下发到该出款卡，和找出不差钱但是没有超过上限的出款卡
	 * @param info
	 * @return
	 */		
	public Optional<OutOnly> generateOutAccount(ZSetOperations.TypedTuple<String> info, Map<Object, Object> outAccRealBalMap) {
		if(ObjectUtils.isEmpty(info.getValue()) || ObjectUtils.isEmpty(info.getScore())) {
			return Optional.empty();
		}
		Integer accountId = Integer.parseInt(info.getValue());
		AccountBaseInfo base = accountService.getFromCacheById(accountId);
		// [0]type [1]zone [2]level [3]handicap [4]bal
		Integer[] inf = deScore4Fr(info.getScore());
		if(ObjectUtils.isEmpty(base) || ObjectUtils.isEmpty(accountId) || ObjectUtils.isEmpty(inf[2]) || ObjectUtils.isEmpty(inf[0]) || ObjectUtils.isEmpty(inf[1]) || ObjectUtils.isEmpty(inf[3]) || ObjectUtils.isEmpty(inf[4]) ) {
			return Optional.empty();
		}
		if(!base.getStatus().equals(AccountStatus.Normal.getStatus())) {
			return Optional.empty();
		}
		Optional<Account> accountOpt = new Account().generate(base, accountId, inf[2], inf[0], inf[1], inf[3], inf[4], redisService);
		if(accountOpt.isPresent() && accountOpt.get() instanceof OutOnly) {
			OutOnly account = (OutOnly)accountOpt.get();
			account.init(otherCache, accountService, redisService, handicapService, userService, sysDataPermissionService, allOTaskSer, CURR_VERSION, accountChangeService);
			account.calculateNeed();
			if(outAccRealBalMap.get(accountId.toString()) == null) {
				return Optional.empty();
			}
			account.setBalance(new BigDecimal(String.valueOf(outAccRealBalMap.get(accountId.toString()))).intValue());
			return Optional.of(account);
		} 
		return Optional.empty();
	}	
	
	/**
	 * 从 redis的AllocApplyByFrom中的记录来生成出款卡，用在将大额入款卡的全部下发到该出款卡，和找出不差钱但是没有超过上限的出款卡
	 * @param info（取FROM中的出款卡缓存起来）
	 * @return
	 */		
	public Optional<OutOnly> generateOutAccount(ZSetOperations.TypedTuple<String> info) {
		if(ObjectUtils.isEmpty(info.getValue()) || ObjectUtils.isEmpty(info.getScore())) {
			return Optional.empty();
		}
		Integer accountId = Integer.parseInt(info.getValue());
		AccountBaseInfo base = accountService.getFromCacheById(accountId);
		// [0]type [1]zone [2]level [3]handicap [4]bal
		Integer[] inf = deScore4Fr(info.getScore());
		if(ObjectUtils.isEmpty(base) || ObjectUtils.isEmpty(accountId) || ObjectUtils.isEmpty(inf[2]) || ObjectUtils.isEmpty(inf[0]) || ObjectUtils.isEmpty(inf[1]) || ObjectUtils.isEmpty(inf[3]) || ObjectUtils.isEmpty(inf[4]) ) {
			return Optional.empty();
		}
		if(!base.getStatus().equals(AccountStatus.Normal.getStatus())) {
			return Optional.empty();
		}
		Optional<Account> accountOpt = new Account().generate(base, accountId, inf[2], inf[0], inf[1], inf[3], inf[4], redisService);
		if(accountOpt.isPresent() && accountOpt.get() instanceof OutOnly) {
			OutOnly account = (OutOnly)accountOpt.get();
			account.init(otherCache, accountService, redisService, handicapService, userService, sysDataPermissionService, allOTaskSer, CURR_VERSION, accountChangeService);
			account.calculateNeed();
			return Optional.of(account);
		} 
		return Optional.empty();
	}	
	
	/**
	 * 从redis的AllocOutNeedOri中的记录（包含所需金额）来生成出款卡，用于补钱
	 * @param need
	 * @return
	 */
	public Optional<OutOnly> generateOutOnlyAccount(Number[] need, Map<Object, Object> outAccRealBalMap) {
		// [0]ID;[1]score;[2]priority;[3]tm;[4]need		
		Integer accountId = need[0].intValue();
		
		AccountBaseInfo base = accountService.getFromCacheById(accountId);
		if(ObjectUtils.isEmpty(base) || ObjectUtils.isEmpty(base.getCurrSysLevel()) || ObjectUtils.isEmpty(base.getType()) || ObjectUtils.isEmpty(base.getHandicapId())) {
			return Optional.empty();
		}
		if(!base.getStatus().equals(AccountStatus.Normal.getStatus())) {
			return Optional.empty();
		}
		int toZone = handicapService.findZoneByHandiId(base.getHandicapId());
		//base, accountId, levelId, type, zone, handicapId, amount
		Optional<Account> accountOpt = new Account().generate(base, accountId, base.getCurrSysLevel(), base.getType(), toZone, base.getHandicapId(), 0, redisService);
		if(accountOpt.isPresent() && accountOpt.get() instanceof OutOnly) {
			Account account = accountOpt.get();
			account.init(otherCache, accountService, redisService, handicapService, userService, sysDataPermissionService, allOTaskSer, CURR_VERSION, accountChangeService);
			OutOnly out = (OutOnly) account;
			out.setNeedMoney(need[4].intValue());
			out.setPriority(need[2].intValue());
			if(outAccRealBalMap.get(accountId.toString()) == null) {
				return Optional.empty();
			}
			out.setBalance(new BigDecimal(String.valueOf(outAccRealBalMap.get(accountId.toString()))).intValue());
			return Optional.of(out);
		} 
		return Optional.empty();
	}
	
	/**
	 * 解析下发账号分值
	 *
	 * @return Number[0] type </br>
	 *         Number[1] zone </br>
	 *         Number[2] l</br>
	 *         Number[3] handi</br>
	 *         Number[4] bal</br>
	 * @see this#deScore4Out(Double)
	 */
	public Integer[] deScore4Fr(Double score) {
		String sc = CommonUtils.getNumberFormat(8, 8).format(score);
		return new Integer[] { Integer.valueOf(sc.substring(0, 2)), Integer.valueOf(sc.substring(2, 4)),
				Integer.valueOf(sc.substring(4, 5)), Integer.valueOf(sc.substring(5, 8)),
				Integer.valueOf(sc.substring(9, 17)) };
	}		
	
	/**
	 * 解析分值
	 * <p>
	 * 参数<code>score</code>必须由函数</br>
	 * <code>this#score4zset(int l, int target, int handicapId, BigDecimal amount)</code>
	 * 生成
	 * </p>
	 *
	 * @return int[0] tarVal</br>
	 *         int[1] 内层/外层/中层 l</br>
	 *         int[2] tarType</br>
	 *         int[3] 区域</br>
	 *         int[4] handicapId</br>
	 *         int[5] amount</br>
	 */
	private int[] descore(int tarType, int tarVal, double score) {
		int[] infs = descore(score);
		int[] des = Arrays.copyOf(new int[] { tarVal }, infs.length + 1);
		System.arraycopy(infs, 0, des, 1, infs.length);
		des[2] = tarType;
		return des;
	}	
	
	/**
	 * 解析分值
	 * <p>
	 * 参数<code>score</code>必须由函数</br>
	 * <code>this#score4zset(int l, int target, int handicapId, BigDecimal amount)</code>
	 * 生成
	 * </p>
	 *
	 * @param score
	 *            分值
	 * @return int[0] 内层/外层/中层 l</br>
	 *         int[1] tarType</br>
	 *         int[2] 区域</br>
	 *         int[3] handicapId</br>
	 *         int[4] amount</br>
	 */
	private int[] descore(double score) {
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumIntegerDigits(7);
		nf.setMinimumIntegerDigits(7);
		nf.setMaximumFractionDigits(8);
		nf.setMinimumFractionDigits(8);
		nf.setGroupingUsed(false);
		String sc = nf.format(score);
		return new int[] { Integer.parseInt(sc.substring(0, 1)), Integer.parseInt(sc.substring(1, 2)),
				Integer.parseInt(sc.substring(2, 4)), Integer.parseInt(sc.substring(4, 7)),
				Integer.parseInt(sc.substring(8, 16)) };
	}	
}
