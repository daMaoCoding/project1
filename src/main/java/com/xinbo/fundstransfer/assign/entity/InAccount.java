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
 * 入款卡
 * 
 * @author Administrator
 *
 */
@Slf4j
public class InAccount extends Account implements Trans2OutAccount {

	public InAccount(AccountBaseInfo info, int accountId, int levelId, int type, int zone, int handicapId, int amount) {
		super(info, accountId, levelId, type, zone, handicapId, amount);
	}

	/**
	 * 余额超过10万时全部下发到备用卡
	 */
	public boolean transReserveWhenTooMuchBalance(ReserveAccount reserveAccount, Set<String> evading, int minBal) {
		log.debug(">>InAccount Start transInWhenTooLitlleBalance inAccount id:{}, reserveAccount id:{}",
				this.getAccountId(), reserveAccount.getAccountId());
		// verify account status
		if (Constants.NORMAL != this.accountBaseInfo.getStatus()) {
			log.debug(
					">>InAccount transInWhenTooLitlleBalance inAccount id:{}, reserveAccount id:{} status not Normal.",
					this.getAccountId(), reserveAccount.getAccountId());
			return false;
		}
		if (reserveAccount.getZone() != this.zone) {
			log.debug(">>InAccount transInWhenTooLitlleBalance inAccount id:{}, reserveAccount id:{} zone not equal.",
					this.getAccountId(), reserveAccount.getAccountId());
			return false;
		}
		if (this.accountBaseInfo.getCurrSysLevel() == null || reserveAccount.getLevelId() != this.accountBaseInfo.getCurrSysLevel()) {
			log.debug(">>InAccount transInWhenTooLitlleBalance inAccount id:{}, reserveAccount id:{} level not equal.",
					this.getAccountId(), reserveAccount.getAccountId());
			return false;
		}
		// 检查是否开启区分盘口出款、下发
		boolean dsctIn = CommonUtils.checkDistHandicapAllocateOutAndIn(reserveAccount.getHandicapId());
		boolean reserveOut = CommonUtils.checkDistHandicapAllocateOutAndIn(this.handicapId);
		if (dsctIn || reserveOut) {
			// verify hanicap
			if (reserveAccount.getHandicapId() != this.handicapId) {
				return false;
			}
		}
		// verify peer agency tranfer
		if (Objects.nonNull(allOTaskSer.checkPeerTrans(reserveAccount.accountBaseInfo.getBankType()))) {
			return false;
		}
		// verify maintenance
		if (allOTaskSer.checkMaintain(this.accountBaseInfo.getBankType())) {
			log.debug(">>InAccount checkMaintain inAccount id:{}, reserveAccount id:{}", this.getAccountId(),
					reserveAccount.getAccountId());
			return false;
		}
		Integer tInt = this.amount - minBal;
		log.debug(">>InAccount transfer amout:{}", tInt);
		if (tInt > 49000) {
			tInt = 49000;
		}
		// verify the transfer lowest limit amount.
		if (tInt < Constants.MAX_TOLERANCE) {
			log.debug(">>InAccount outAccountId:{} The required amount is less than 500.", this.getAccountId());
			return false;
		}
		try {
			lockTrans(this.accountId, reserveAccount.getAccountId(), AppConstants.USER_ID_4_ADMIN, tInt);
			log.info("Alloc4Trans >> O( {} , {} ) transFr{} transTo{} transInt:{}", this.accountId,
					reserveAccount.getAccountId(), this.accountId, reserveAccount.getAccountId(), tInt);
			// post operation for from account
			if (this.amount >= buildHigh(this.accountBaseInfo)) {
				redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_NEW_APPLY_BY_FROM)
						.remove(String.valueOf(this.getAccountId()));
				redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_NEW_APPLY_BY_FROM)
						.remove(String.valueOf(reserveAccount.getAccountId()));
				log.debug("AllocNewApplyByFrom  >> delete the fr-acc allocated. frId:{},toId:{},transInt:{}",
						reserveAccount.getAccountId(), reserveAccount.getAccountId(), tInt);
			}
			return true;
		} catch (Exception e) {
			log.debug("alloc4ONeed  >> lock trans fail. frId:{},toId:{},amt:{}", this.getAccountId(),
					reserveAccount.getAccountId(), tInt);
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

	private volatile boolean isUsed = false;
	private volatile int remain = 0;
	
	public boolean isUsed() {
		return isUsed;
	}

	public void setUsed(boolean isUsed) {
		this.isUsed = isUsed;
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
		return "InAccount [accountId=" + accountId + ", levelId=" + levelId + ", type=" + type + ", zone=" + zone
				+ ", handicapId=" + handicapId + ", amount=" + amount + "]";
	}
}
