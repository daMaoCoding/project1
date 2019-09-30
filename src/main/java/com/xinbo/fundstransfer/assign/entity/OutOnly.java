package com.xinbo.fundstransfer.assign.entity;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.assign.Constants;
import com.xinbo.fundstransfer.assign.Trans2OutAccount;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.AllocatingOutwardTask;

import lombok.extern.slf4j.Slf4j;

/**
 * 专注出款卡（PC或返利网）
 * 
 * @author Administrator
 *
 */
@Slf4j
public class OutOnly extends OutAccount {
	private int needMoney;
	private int priority;
	private int balance;

	public OutOnly(int outType, AccountBaseInfo info, int accountId, int levelId, int type, int zone, int handicapId,
			int amount) {
		super(outType, info, accountId, levelId, type, zone, handicapId, amount);
	}

	/**
	 * 是否正在入款卡补充余额中
	 * 
	 * @return
	 */
	public boolean isTransferingIn() {
		return otherCache.isCachedInvSetIn(this.accountId);
	}

	/**
	 * 是否正在第三方下发补充余额中
	 * 
	 * @return
	 */
	public boolean isThirdTransferingIn() {
		// TODO:
		return false;
	}

	/**
	 * 余额不足时从入款卡补充余额
	 */
	public Optional<Integer> canTransInWhenTooLitlleBalance(Trans2OutAccount inAccount, Set<String> black, Boolean peerTrans) {
		log.debug(">>OutOnly Start transInWhenTooLitlleBalance outAccount id:{}, inAccount id:{}", this.accountId,
				inAccount.getAccountId());
		if (this.accountBaseInfo.getStatus() != AccountStatus.Normal.getStatus()) {
			log.debug(">>OutOnly transInWhenTooLitlleBalance outAccount id:{}, inAccount id:{} status not Normal.",
					this.accountId, inAccount.getAccountId());
			return Optional.empty();
		}
		if (inAccount.getZone() != this.zone) {
			log.debug(">>OutOnly transInWhenTooLitlleBalance outAccount id:{}, inAccount id:{} zone not equal.",
					this.accountId, inAccount.getAccountId());
			return Optional.empty();
		}
		if (this.accountBaseInfo.getCurrSysLevel() == null || inAccount.getLevelId() != this.accountBaseInfo.getCurrSysLevel()) {
			log.debug(">>OutOnly transInWhenTooLitlleBalance outAccount id:{}, inAccount id:{} level not equal.",
					this.accountId, inAccount.getAccountId());
			return Optional.empty();
		}
		// 检查是否开启区分盘口出款、下发
		boolean dsctIn = CommonUtils.checkDistHandicapAllocateOutAndIn(inAccount.getHandicapId());
		boolean dsctOut = CommonUtils.checkDistHandicapAllocateOutAndIn(this.handicapId);
		if ((dsctIn || dsctOut) && inAccount.getType() != Constants.BINDCOMMON) {
			// verify hanicap
			if (inAccount.getHandicapId() != this.handicapId) {
				return Optional.empty();
			}
		}
		// 校验黑名单
		if (!checkBlack(black, String.valueOf(inAccount.getAccountId()), this.accountId)) {
			log.debug("TransBlack{} InForce frId:{}", this.accountId, inAccount.getAccountId());
			return Optional.empty();
		}
		// 检测 同行转账
		if (!allOTaskSer.checkPeer(peerTrans, inAccount.getAccountBaseInfo(), this.accountBaseInfo)) {
			log.debug("TransPeer{} InForce", this.accountId);
			return Optional.empty();
		}
		// verify maintenance
		if (allOTaskSer.checkMaintain(inAccount.getAccountBaseInfo().getBankType())) {
			log.debug(">>OutOnly checkMaintain outAccount id:{}, inAccount id:{}", this.accountId,
					inAccount.getAccountId());
			return Optional.empty();
		}
		Integer toInt = Math.min(inAccount.getAmount() - getInBankMinBalance(inAccount.getAccountBaseInfo()),
				this.needMoney);
		log.debug(">>InAccount transfer amout:{}", toInt);
		// verify the transfer lowest limit amount.
		if (toInt < Constants.MAX_TOLERANCE) {
			log.debug(">>OutOnly outAccountId:{} The required amount is less than 500.", this.accountId);
			return Optional.empty();
		}
		return Optional.of(toInt);
	}

	/**
	 * 余额不足时从入款卡补充余额
	 */
	public boolean transInWhenTooLitlleBalance(Trans2OutAccount inAccount, Set<String> black, Boolean peerTrans) {
		Optional<Integer> toIntOpt = canTransInWhenTooLitlleBalance(inAccount, black, peerTrans);
		if(!toIntOpt.isPresent()) {
			return false;
		}
		Integer toInt = toIntOpt.get();
		try {
			lockTrans(inAccount.getAccountId(), this.accountId, AppConstants.USER_ID_4_ADMIN, toInt);
			log.info("alloc4AliOver >> O( {} , {} ) transFr{} transTo{} transInt:{}", inAccount.getAccountId(),
					this.accountId, inAccount.getAccountId(), this.accountId, toInt);
			redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_NEW_APPLY_BY_FROM)
					.remove(String.valueOf(this.accountId));
			redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_NEW_APPLY_BY_FROM)
					.remove(String.valueOf(inAccount.getAccountId()));
			// post operation for to account
			ldel4ONeed(String.valueOf(this.accountId));
			log.debug(">>OutOnly end transInWhenTooLitlleBalance outAccount id:{}, inAccount id:{}", this.accountId,
					inAccount.getAccountId());

			inAccount.setRemain(inAccount.getAmount() - toInt);
			inAccount.setUsed(true);
			return true;
		} catch (Exception e) {
			log.debug("alloc4AliOver  >> lock trans fail. frId:{},toId:{},amt:{}", inAccount.getAccountId(),
					this.accountId, toInt);
			return false;
		}
	}

	/**
	 * 余额不足时从第三方下发补充余额
	 */
	public void transInThridWhenTooLittleBalance() {
		// TODO:
	}

	/**
	 * 余额多时下发到其它类型的出款卡
	 */
	public void transOutWhenTooMuchBalance() {
		// TODO:
	}

	@Override
	int checkCanOutTask(AllocatingOutwardTask task, Integer level) {
		log.debug("checkCanOutTask>> check can out task {}", task.getTaskId());
		String allocStrategy = buildAllocateStrategy();
		if (Objects.equals(AppConstants.OUTTASK_ALLOCATE_TO_MANULE, allocStrategy)) {
			log.debug("checkCanOutTask>> OUTTASK_ALLOCATE_TO_MANULE failed");
			return Constants.NOT_OK;
		}
		if (task.getManualOut() != Constants.ROBOT_OUT_YES || Objects.equals(task.getFirstOut(), 1)) {
			log.debug("checkCanOutTask>> ROBOT_OUT_YES failed");
			return Constants.NOT_OK;
		}
		Integer zone = handicapService.findZoneByHandiId(task.getZone());
		if (this.zone != zone) {
			log.debug("checkCanOutTask>> zone failed");
			return Constants.NOT_OK;
		}
		Integer handicap = task.getHandicap();
		boolean taskDistHandicap = CommonUtils.checkDistHandicapAllocateOutAndIn(handicap);
		boolean targetDistHandicap = CommonUtils.checkDistHandicapAllocateOutAndIn(this.getHandicapId());
		if ((taskDistHandicap || targetDistHandicap) && !Objects.equals(handicap, this.getHandicapId())) {
			log.debug("checkCanOutTask>> handicap failed");
			return Constants.NOT_OK;
		}
		if ((Constants.TARGET_TYPE_ROBOT == this.outType && !checkRobot(this.accountId))
				|| (Constants.TARGET_TYPE_MOBILE == this.outType && !checkMobile(this.accountId))) {
			redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOCATING_NEW_OUTWARD_TARGET)
					.remove(String.format(Constants.QUEUE_FORMAT_USER_OR_ROBOT, this.outType, this.accountId));
			log.debug("checkCanOutTask>> TARGET_TYPE_ROBOT failed");
			return Constants.NOT_OK;
		}
		int random = (int) (Math.random() * 100 + 101);
		if (this.amount - task.getTaskAmount() <= random) {
			int subAmt = this.amount - task.getTaskAmount().intValue();
			if (task.getTaskAmount() >= CommonUtils.getTaskAmountCanAllocatToMobile()
					&& subAmt < -CommonUtils.getTaskAmountMinSubAmtMobile()
					&& subAmt > -CommonUtils.getTaskAmountMaxSubAmtMobile()
					&& accountChangeService.margin(accountBaseInfo)>task.getTaskAmount()) {
				log.debug("checkCanOutTask>> OK_BUT_BALANCE success");
				return Constants.OK_BUT_BALANCE;
			}
			log.debug("checkCanOutTask>> amount failed");
			return Constants.NOT_OK;
		}
		log.debug("checkCanOutTask>> check can out task {} pass check", task.getTaskId());
		return Constants.OK;
	}

	@Override
	AllocatingOutwardTask setFromIdOperator(AllocatingOutwardTask task, Integer level, Boolean peerTrans, String tarBanks) {
		log.debug("setFromIdOperator>> allocate task {}", task.getTaskId());
		boolean mergeLevel = CommonUtils.isMergeMidInner();
		// 黑名单集合
		Set<String> black = otherCache.getCachedBLACK();
		// 最后出款信息
		Map<Object, Object> last = otherCache.getCachedLAST();
		if(!checkBank(tarBanks)) {
			log.debug("setFromIdOperator>> checkBank failed");
			return null;
		}
		if(!checkLevel(level, this.accountBaseInfo, false, mergeLevel)) {
			log.debug("setFromIdOperator>> checkLevel failed");
			return null;
		}
		if(!checkTrans(peerTrans, this.accountBaseInfo.getBankType(), task.getToAccountBank())) {
			log.debug("setFromIdOperator>> checkTrans failed");
			return null;
		}
		if(!checkTask(last, this.accountId, task.getMemBank(), task.getTaskAmount())) {
			log.debug("setFromIdOperator>> checkTask failed");
			return null;
		}
		if(!checkLimitOutOne(task, this.accountBaseInfo)) {
			log.debug("setFromIdOperator>> checkLimitOutOne failed");
			return null;
		}
		if(!checkBlack(this.accountId, this.amount, task, black)) {
			log.debug("setFromIdOperator>> checkBlack failed");
			return null;
		}

		task.setFromId(this.accountId);
		task.setOperator(null);
		log.debug("allocate>> target robot type check pass,task {},target id {}", task, task.getFromId());
		return task;
	}

	@Override
	public Integer getManualOut() {
		return Constants.ROBOT_OUT_YES;
	}

	public int getNeedMoney() {
		return needMoney;
	}

	public void setNeedMoney(int needMoney) {
		this.needMoney = needMoney;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public int getBalance() {
		return balance;
	}

	public void setBalance(int balance) {
		this.balance = balance;
	}
	
	private Integer getInBankMinBalance(AccountBaseInfo base) {
		int minBalance;
		if (base.getMinBalance() != null) {
			minBalance = base.getMinBalance().intValue();
		} else {
			minBalance =  (int) (Math.random() * 100 + 101);
		}
		return minBalance;
	}

	public void calculateNeed() {
		this.needMoney = accountChangeService.currCredits(this.accountBaseInfo);
		log.debug(">> accountChangeService currCredits needMoney :{}", needMoney);
	}

	@Override
	public String toString() {
		return "OutOnly [needMoney=" + needMoney + ", priority=" + priority + ", outType=" + outType + ", accountId="
				+ accountId + "]";
	}
}
