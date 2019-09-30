package com.xinbo.fundstransfer.assign.entity;

import com.xinbo.fundstransfer.assign.Constants;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.AllocatingOutwardTask;

import lombok.extern.slf4j.Slf4j;

/**
 * 第三方出款
 * 
 * @author Administrator
 *
 */
@Slf4j
public class OutThridPart extends OutAccount {
	public OutThridPart(AccountBaseInfo info, int accountId, int levelId, int type, int zone, int handicapId, int amount) {
		super(Constants.TARGET_TYPE_THIRD, info, accountId, levelId, type, zone, handicapId, amount);
	}

	@Override
	int checkCanOutTask(AllocatingOutwardTask task, Integer level) {
		if (task.getManualOut() != Constants.THIRD_OUT_YES) {
			return Constants.NOT_OK;
		}
		if (!checkThird(this.accountId)) {
			redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOCATING_NEW_OUTWARD_TARGET)
					.remove(String.format(Constants.QUEUE_FORMAT_USER_OR_ROBOT, this.outType, this.accountId));
			return Constants.NOT_OK;
		}
		if(!checkUserDataPermission(accountId,task.getHandicap())){
			return Constants.NOT_OK;
		}
		return Constants.OK;
	}

	@Override
	AllocatingOutwardTask setFromIdOperator(AllocatingOutwardTask task, Integer level, Boolean peerTrans, String tarBanks) {
		log.debug("allocate>> target third type check pass,task {},target {}", task, this.accountId);
		task.setOperator(this.accountId);
		task.setFromId(null);
		return task;
	}
	
	@Override
	public Integer getManualOut() {
		return Constants.THIRD_OUT_YES;
	}		
}
