package com.xinbo.fundstransfer.assign.entity;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.xinbo.fundstransfer.domain.enums.InBankSubType;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.util.CollectionUtils;

import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.assign.Constants;
import com.xinbo.fundstransfer.assign.OtherCache;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.domain.enums.AccountFlag;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.TransLock;
import com.xinbo.fundstransfer.service.AccountChangeService;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.AllocateOutwardTaskService;
import com.xinbo.fundstransfer.service.HandicapService;
import com.xinbo.fundstransfer.service.RedisService;
import com.xinbo.fundstransfer.service.SysDataPermissionService;
import com.xinbo.fundstransfer.service.SysUserService;

/**
 * 
 * @author Administrator
 *
 */
public class Account {
	protected AccountService accountService;
	protected RedisService redisService;
	protected HandicapService handicapService;
	protected SysUserService userService;
	protected SysDataPermissionService sysDataPermissionService;
	protected AllocateOutwardTaskService allOTaskSer;
	protected OtherCache otherCache;
	protected String CURR_VERSION;
	protected AccountChangeService accountChangeService;
	protected AccountBaseInfo accountBaseInfo;
	protected int accountId;
	protected int levelId;
	protected int type;
	protected int zone;
	protected int handicapId;
	protected int amount;

	public Account() {

	}

	public Account(AccountBaseInfo accountBaseInfo, int accountId, int levelId, int type, int zone, int handicapId,
			int amount) {
		this.accountBaseInfo = accountBaseInfo;
		this.accountId = accountId;
		this.levelId = levelId;
		this.type = type;
		this.zone = zone;
		this.handicapId = handicapId;
		this.amount = amount;
	}

	public void init(OtherCache otherCache, AccountService accountService, RedisService redisService,
			HandicapService handicapService, SysUserService userService,
			SysDataPermissionService sysDataPermissionService, AllocateOutwardTaskService allOTaskSer,
			String CURR_VERSION, AccountChangeService accountChangeService) {
		this.otherCache = otherCache;
		this.accountService = accountService;
		this.redisService = redisService;
		this.handicapService = handicapService;
		this.userService = userService;
		this.sysDataPermissionService = sysDataPermissionService;
		this.allOTaskSer = allOTaskSer;
		this.CURR_VERSION = CURR_VERSION;
		this.accountChangeService = accountChangeService;
	}

	public Optional<Account> generate(AccountBaseInfo info, int accountId, int levelId, int type, int zone,
			int handicapId, int amount, RedisService redisService) {
		switch (AccountType.findByTypeId(info.getType())) {
		case InBank:
		case InThird:
		case InAli:
		case InWechat:
			if (Objects.equals(info.getSubType(), InBankSubType.IN_BANK_YSF.getSubType())) {
				return Optional.of(new OutYunSF(info, accountId, levelId, type, zone, handicapId, amount));
			}
			if (!Objects.equals(info.getFlag(), AccountFlag.PC.getTypeId())) {
				Object model = redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_NEW_MODEL)
						.get(info.getId().toString());
				int modelVal = model == null ? Constants.YSF_MODEL_IN : Integer.valueOf(model.toString());
				if (modelVal == Constants.YSF_MODEL_OUT) {
					return Optional.of(new OutInTurn(info, accountId, levelId, type, zone, handicapId, amount));
				} else {
					return Optional.of(new InOnly(info, accountId, levelId, type, zone, handicapId, amount));
				}
			} else {
				return Optional.of(new InOnly(info, accountId, levelId, type, zone, handicapId, amount));
			}
		case BindCommon:
			return Optional.of(new InOnly(info, accountId, levelId, type, zone, handicapId, amount));
		case ReserveBank:
			return Optional.of(new ReserveAccount(info, accountId, levelId, type, zone, handicapId, amount));
		case OutBank:
		case OutThird:
		case CashBank:
			if (info.getFlag() == AccountFlag.PC.getTypeId().intValue()) {
				return Optional.of(new OutOnly(Constants.TARGET_TYPE_ROBOT, info, accountId, levelId, type, zone,
						handicapId, amount));
			} else {
				return Optional.of(new OutOnly(Constants.TARGET_TYPE_MOBILE, info, accountId, levelId, type, zone,
						handicapId, amount));
			}

		default:
			return Optional.empty();
		}
	}

	public Optional<OutAccount> generate(Integer outType, AccountBaseInfo info, int accountId, int levelId, int type,
			int zone, int handicapId, int amount) {
		switch (outType) {
		case Constants.TARGET_TYPE_USER:
			return Optional.of(new OutManual(info, accountId, levelId, type, zone, handicapId, amount));
		case Constants.TARGET_TYPE_THIRD:
			return Optional.of(new OutThridPart(info, accountId, levelId, type, zone, handicapId, amount));
		case Constants.TARGET_TYPE_ROBOT:
		case Constants.TARGET_TYPE_MOBILE:
			if (Objects.equals(info.getType(), Constants.OUTBANK)) {
				return Optional.of(new OutOnly(outType, info, accountId, levelId, type, zone, handicapId, amount));
			}
			if (Objects.equals(info.getType(), Constants.INBANK)
					&& Objects.equals(info.getSubType(), InBankSubType.IN_BANK_YSF.getSubType())) {
				return Optional.of(new OutYunSF(info, accountId, levelId, type, zone, handicapId, amount));
			} 
			if (Objects.equals(info.getType(), Constants.INBANK)
					&& !info.getFlag().equals(AccountFlag.PC.getTypeId())) {
				return Optional.of(new OutInTurn(info, accountId, levelId, type, zone, handicapId, amount));
			}
			return Optional.empty();
		default:
			return Optional.empty();
		}
	}

	/**
	 * 获取余额告警值
	 *
	 * @return
	 */
	protected int buildLimitBalance() {
		int minBal = CommonUtils.getReserveTOReserveMinAmount();
		if (Objects.isNull(accountBaseInfo) || Objects.isNull(accountBaseInfo.getLimitBalance())) {
			return minBal;
		}
		return accountBaseInfo.getLimitBalance();
	}

	/**
	 * 获取:账号预留余额设置</br>
	 * 生产环境：isRobot ? TRANS_ROBOT_BAL : TRANS_MANUAL_BAL</br>
	 * 测试环境：BigDecimal.ONE
	 */
	protected BigDecimal buildMinBal(boolean isRobot) {
		if (CommonUtils.checkProEnv(CURR_VERSION)) {
			return isRobot ? new BigDecimal(50) : new BigDecimal(20);
		} else {
			return BigDecimal.ONE;
		}
	}

	protected boolean checkBlack(Set<String> black, String frAcc, int toId) {
		boolean ret = CollectionUtils.isEmpty(black)
				|| (black.stream().filter(p -> p.startsWith(RedisKeys.gen4TransBlack(Constants.WILD_CARD_ACCOUNT, toId, 0))
						|| p.startsWith(RedisKeys.gen4TransBlack(frAcc, toId, 0))).count() == 0);
		return ret;
	}

	protected void lockTrans(Object fromId, Integer toId, Integer operator, Integer transInt) throws Exception {
		if (fromId == null || toId == null || operator == null || transInt == null) {
			throw new Exception("lock trans record failed , due to incompleteness of input information.");
		}
		llock(fromId, toId, operator, transInt, Constants.LOCK_ROBOT_CLAIM_SECONDS);
	}

	/**
	 * 执行 lua script: 锁定
	 */
	protected void llock(Object fromId, Integer toId, Integer operator, Integer transInt, Integer expTM)
			throws Exception {
		String ret = redisService.getStringRedisTemplate().execute(new RedisScript<String>() {
			@Override
			public String getSha1() {
				return "LUA_SCRIPT_ALLOC_TRANS_LOCK";
			}

			@Override
			public Class<String> getResultType() {
				return String.class;
			}

			@Override
			public String getScriptAsString() {
				return CommonUtils.getTransLockFromTo() ? Constants.LUA_SCRIPT_ALLOC_TRANS_LOCK_
						: Constants.LUA_SCRIPT_ALLOC_TRANS_LOCK;
			}
		}, null, String.valueOf(fromId), String.valueOf(toId), String.valueOf(operator), String.valueOf(transInt),
				String.valueOf(AppConstants.USER_ID_4_ADMIN), String.valueOf(expTM),
				String.valueOf(Constants.LOCK_MANUAL_SECONDS), RedisKeys.genPattern4TransferAccountLock_from(fromId),
				RedisKeys.genPattern4TransferAccountLock_to(toId),
				RedisKeys.genPattern4TransferAccountLock_operator(operator), RedisKeys.FROM_ACCOUNT_TRANS_RADIX_AMOUNT,
				RedisKeys.genPattern4TransferAccountLock(fromId, toId, operator),
				String.valueOf(System.currentTimeMillis()), String.valueOf(TransLock.STATUS_ALLOC),
				String.valueOf(TransLock.STATUS_ACK), String.valueOf(TransLock.STATUS_DEL));
		if (!Objects.equals("ok", ret)) {
			throw new Exception(String.format("the account %s already locked.", String.valueOf(fromId)));
		}
	}

	protected void ldel4ONeed(String tar) {
		redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_NEW_OUT_NEED_ORI).remove(tar);
	}

	public AccountBaseInfo getAccountBaseInfo() {
		return accountBaseInfo;
	}

	public int getAccountId() {
		return accountId;
	}

	public void setAccountId(int accountId) {
		this.accountId = accountId;
	}

	public int getLevelId() {
		return levelId;
	}

	public void setLevelId(int levelId) {
		this.levelId = levelId;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getZone() {
		return zone;
	}

	public void setZone(int zone) {
		this.zone = zone;
	}

	public int getHandicapId() {
		return handicapId;
	}

	public void setHandicapId(int handicapId) {
		this.handicapId = handicapId;
	}

	public int getAmount() {
		return amount;
	}

	public void setAmount(int amount) {
		this.amount = amount;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + accountId;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Account other = (Account) obj;
		if (accountId != other.accountId)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Account [accountId=" + accountId + ", levelId=" + levelId + ", type=" + type + ", zone=" + zone + ", handicapId=" + handicapId + ", amount=" + amount + "]";
	}

	protected int buildHandi(Integer handi) {
		return Objects.isNull(handi) ? Constants.WILD_CARD_HANDI : handi;
	}
}