package com.xinbo.fundstransfer.assign.entity;

import java.util.Objects;
import java.util.Set;

import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.assign.Constants;
import com.xinbo.fundstransfer.assign.Trans2OutAccount;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;

import lombok.extern.slf4j.Slf4j;

/**
 * 备用卡
 * 
 * @author Administrator
 *
 */
@Slf4j
public class ReserveAccount extends Account implements Trans2OutAccount {

	public ReserveAccount(AccountBaseInfo info, int accountId, int levelId, int type, int zone, int handicapId, int amount) {
		super(info, accountId, levelId, type, zone, handicapId, amount);
	}

	/**
	 * 下发给出款卡
	 */
	public boolean transReserveWhenTooMuchBalance(ReserveAccount reserveOutAccount, Set<String> evading, int minBal) {
		// verify account status
		if (Constants.NORMAL != this.accountBaseInfo.getStatus()) {
			return false;
		}
		if (reserveOutAccount.getZone() != this.zone) {
			return false;
		}
		if (this.accountBaseInfo.getCurrSysLevel() == null || reserveOutAccount.getLevelId() != this.accountBaseInfo.getCurrSysLevel()) {
			return false;
		}
		// 检查是否开启区分盘口出款、下发
		boolean dsctIn = CommonUtils.checkDistHandicapAllocateOutAndIn(reserveOutAccount.getHandicapId());
		boolean reserveOut = CommonUtils.checkDistHandicapAllocateOutAndIn(this.handicapId);
		if (dsctIn || reserveOut) {
			// verify hanicap
			if (reserveOutAccount.getHandicapId() != this.handicapId) {
				return false;
			}
		}
		// verify peer agency tranfer
		if (Objects.nonNull(allOTaskSer.checkPeerTrans(reserveOutAccount.accountBaseInfo.getBankType()))) {
			return false;
		}
		// verify maintenance
		if (allOTaskSer.checkMaintain(this.accountBaseInfo.getBankType())) {
			return false;
		}
		Integer tInt = this.amount - minBal;
		if (tInt > 49000) {
			tInt = 49000;
		}
		// verify the transfer lowest limit amount.
		if (tInt < Constants.MAX_TOLERANCE) {
			return false;
		}
		try {
			lockTrans(this.accountId, reserveOutAccount.getAccountId(), AppConstants.USER_ID_4_ADMIN, tInt);
			log.info("Alloc4Trans >> O( {} , {} ) transFr{} transTo{} transInt:{}", this.accountId,
					reserveOutAccount.getAccountId(), this.accountId, reserveOutAccount.getAccountId(), tInt);
			// post operation for from account
			if (this.amount >= buildHigh(this.accountBaseInfo)) {
				redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_NEW_APPLY_BY_FROM)
						.remove(String.valueOf(this.getAccountId()));
				redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_NEW_APPLY_BY_FROM)
						.remove(String.valueOf(reserveOutAccount.getAccountId()));
				log.debug("AllocNewApplyByFrom  >> delete the fr-acc allocated. frId:{},toId:{},transInt:{}",
						reserveOutAccount.getAccountId(), reserveOutAccount.getAccountId(), tInt);
			}
			return true;
		} catch (Exception e) {
			log.debug("alloc4ONeed  >> lock trans fail. frId:{},toId:{},amt:{}", this.getAccountId(),
					reserveOutAccount.getAccountId(), tInt);
			return false;
		}
	}

	/**
	 * get the high limit balance of out account
	 * <p>
	 * <code>if base.getLimitBalance == null || base.getLimitBalance < 0  </code>
	 * <br/>
	 * 12000 as the result
	 * </p>
	 *
	 * @see AccountBaseInfo#getLimitBalance()
	 */
	private Integer buildHigh(AccountBaseInfo base) {
		return Objects.nonNull(base) && base.getLimitBalance() != null && base.getLimitBalance() > 0
				? base.getLimitBalance()
				: 12000;
	}

	private volatile boolean isInUsed = false;
	private volatile boolean isOutUsed = false;
	private volatile int remain = 0;
	public boolean isInUsed() {
		return isInUsed;
	}

	public void setInUsed(boolean isInUsed) {
		this.isInUsed = isInUsed;
	}

	public boolean isUsed() {
		return isOutUsed;
	}

	public void setUsed(boolean isOutUsed) {
		this.isOutUsed = isOutUsed;
	}

	@Override
	public void setRemain(int remain) {
		this.remain = remain;
	}
	
	@Override
	public void minusRemain(int amount) {
		this.remain = this.remain - amount;
	}
	
	@Override
	public int getRemain() {
		return this.remain;
	}
	
	@Override
	public String toString() {
		return "ReserveAccount [accountId=" + accountId + ", levelId=" + levelId + ", type="
				+ type + ", zone=" + zone + ", handicapId=" + handicapId + ", amount=" + amount + "]";
	}
}
