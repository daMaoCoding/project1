package com.xinbo.fundstransfer.assign.entity;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.assign.Constants;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.AllocatingOutwardTask;

import lombok.extern.slf4j.Slf4j;

/**
 * 入款变出款卡（返利网）
 * 
 * @author Administrator
 *
 */
@Slf4j
public class OutInTurn extends OutAccount {

	public OutInTurn(AccountBaseInfo info, int accountId, int levelId, int type, int zone, int handicapId, int amount) {
		super(Constants.TARGET_TYPE_MOBILE, info, accountId, levelId, type, zone, handicapId, amount);
	}

	/**
	 * 余额太多时变为出款卡
	 */
	public void swithToOutWhenTooMuchBalance() {
		// TODO: 入款卡少的时候，要一键转出， 并且不能变出款卡
	}

	public void swithToInWhenTooLittleBalance() {
	}

	@Override
	int checkCanOutTask(AllocatingOutwardTask task, Integer level) {
		String allocStrategy = buildAllocateStrategy();
		if (Objects.equals(AppConstants.OUTTASK_ALLOCATE_TO_MANULE, allocStrategy)) {
			log.debug("allocStrategy is OUTTASK_ALLOCATE_TO_MANULE {}", allocStrategy);
			return Constants.NOT_OK;
		}
		if (task.getManualOut() != Constants.ROBOT_OUT_YES || Objects.equals(task.getFirstOut(), 1)) {
			log.debug("ROBOT_OUT_YES or FirstOut, {}, {}", task.getManualOut(), task.getFirstOut());
			return Constants.NOT_OK;
		}
		Integer zone = handicapService.findZoneByHandiId(task.getZone());
		if (this.zone != zone) {
			log.debug("ZONE not equal {}, {}", this.zone, zone);
			return Constants.NOT_OK;
		}
		Integer handicap = task.getHandicap();
		boolean taskDistHandicap = CommonUtils.checkDistHandicapAllocateOutAndIn(handicap);
		boolean targetDistHandicap = CommonUtils.checkDistHandicapAllocateOutAndIn(this.getHandicapId());
		if ((taskDistHandicap || targetDistHandicap) && !Objects.equals(handicap, this.getHandicapId())) {
			log.debug("DistHandicap not equal {}, {}, {}, {}", taskDistHandicap, targetDistHandicap, handicap, this.getHandicapId());
			return Constants.NOT_OK;
		}
		if (!checkMobile(this.accountId)) {
			redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOCATING_NEW_OUTWARD_TARGET)
					.remove(String.format(Constants.QUEUE_FORMAT_USER_OR_ROBOT, this.outType, this.accountId));
			log.debug("outtype is not same {}", this.outType);			
			return Constants.NOT_OK;
		}
		if (this.levelId != level) {
			boolean mergeLevel = checkMergeLevel(task, level);
			if (!mergeLevel) {
				log.debug("mergeLevel check failed {}, {}", this.levelId, level);
				return Constants.NOT_OK;
			}
			if (Constants.OUTTER == this.levelId || Constants.DESIGNATED == this.levelId) {
				log.debug("level is not inner {}", this.levelId);
				return Constants.NOT_OK;
			}
		}
		Object model = redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_NEW_MODEL).get(this.accountId + "");
		int modelVal = model == null ? Constants.YSF_MODEL_IN : Integer.valueOf(model.toString());
		if (modelVal != Constants.YSF_MODEL_OUT) {
			log.debug("YSF_MODEL_OUT is false");	
			return Constants.NOT_OK;
		}
		int random = (int) (Math.random() * 100 + 101);
		if (this.amount - task.getTaskAmount() >= random) {
			log.debug("amount check failed {}, {}, {}", this.amount, task.getTaskAmount(), random);
			return Constants.INBANK_OK;
		}
		return Constants.NOT_OK;
	}

	@Override
	AllocatingOutwardTask setFromIdOperator(AllocatingOutwardTask task, Integer level, Boolean peerTrans, String tarBanks) {
		boolean mergeLevel = CommonUtils.isMergeMidInner();
		// 黑名单集合
		Set<String> black = otherCache.getCachedBLACK();
		// 最后出款信息
		Map<Object, Object> last = otherCache.getCachedLAST();
		if(checkBank(tarBanks) == false) {
			log.debug("checkBank failed: {}", tarBanks);
			return null;
		}
		if(checkLevel(level, this.accountBaseInfo, false, mergeLevel) == false) {
			log.debug("checkLevel failed: {}, {}", level, mergeLevel);
			return null;
		}
		if(checkTrans(peerTrans, this.accountBaseInfo.getBankType(), task.getToAccountBank()) == false) {
			log.debug("checkTrans failed: {}, {}", peerTrans, this.accountBaseInfo.getBankType(), task.getToAccountBank());
			return null;
		}
		if(checkTask(last, this.accountId, task.getMemBank(), task.getTaskAmount()) == false) {
			log.debug("checkTask failed: {}, {}", peerTrans, this.accountBaseInfo.getBankType(), task.getToAccountBank());
			return null;
		}
		if(checkLimitOutOne(task, this.accountBaseInfo) == false) {
			log.debug("checkLimitOutOne failed");
			return null;
		}
		if (checkBlack(this.accountId, this.amount, task, black) == false) {
			log.debug("checkBlack failed");
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
}
