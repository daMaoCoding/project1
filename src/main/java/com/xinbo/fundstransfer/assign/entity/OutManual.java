package com.xinbo.fundstransfer.assign.entity;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.assign.Constants;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.AllocatingOutwardTask;

/**
 * 人工出款
 * 
 * @author Administrator
 *
 */
public class OutManual extends OutAccount {
	public OutManual(AccountBaseInfo info, int accountId, int levelId, int type, int zone, int handicapId, int amount) {
		super(Constants.TARGET_TYPE_USER, info, accountId, levelId, type, zone, handicapId, amount);
	}

	@Override
	int checkCanOutTask(AllocatingOutwardTask task, Integer level) {
		String allocStrategy = buildAllocateStrategy();
		boolean globalManual = AppConstants.OUTTASK_ALLOCATE_TO_MANULE.equals(allocStrategy);
		// 既不是“全局人工出款开关处于打开状态”, task 又不是 “人工任务”。 就不能通过人工出款卡出款
		if (!globalManual && task.getManualOut() != Constants.MANUAL_OUT_YES
				&& !Objects.equals(task.getFirstOut(), 1)) {
			return Constants.NOT_OK;
		}
		if (!checkManual(this.accountId)) {
			redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOCATING_NEW_OUTWARD_TARGET)
					.remove(String.format(Constants.QUEUE_FORMAT_USER_OR_ROBOT, this.outType, this.accountId));
			return Constants.NOT_OK;
		}
		if (!checkUserDataPermission(accountId, task.getHandicap())) {
			return Constants.NOT_OK;
		}
		return Constants.OK;
	}

	@Override
	AllocatingOutwardTask setFromIdOperator(AllocatingOutwardTask task, Integer level, Boolean peerTrans, String tarBanks) {
		boolean mergeLevel = CommonUtils.isMergeMidInner();
		// 黑名单集合
		Set<String> black = otherCache.getCachedBLACK();
		// 最后出款信息
		Map<Object, Object> last = otherCache.getCachedLAST();
		Map<Integer, List<BizAccount>> userAccMap = otherCache.getCachedUSERACCMAP();
		List<BizAccount> acnts4User = userAccMap == null ? null : userAccMap.get(this.accountId);
		// 区域内是否区分盘口 true:区分盘口;不区分盘口
		boolean distHandi = handicapService.checkDistHandi(task.getZone());
		acnts4User = checkOutAcc4Manual(distHandi, task, acnts4User);
		if (Objects.nonNull(acnts4User) && acnts4User.size() > 0) {
			for (BizAccount acnt : acnts4User) {
				int bal = acnt == null || acnt.getBankBalance() == null ? 0 : acnt.getBankBalance().intValue();
				AccountBaseInfo acntbase = accountService.getFromCacheById(acnt.getId());
				if (acnt != null && checkBank(tarBanks, acntbase) && checkLevel(level, acntbase, true, mergeLevel)
						&& checkTrans(peerTrans, acnt.getBankType(), task.getToAccountBank())
						&& checkTask(last, acnt.getId(), task.getMemBank(), task.getTaskAmount())
						&& checkLimitOutOne(task, acntbase) && checkBlack(acnt.getId(), bal, task, black)) {
					task.setFromId(acnt.getId());
					task.setOperator(this.accountId);
					return task;
				}
			}
		}
		return null;
	}
	
	@Override
	public Integer getManualOut() {
		return Constants.MANUAL_OUT_YES;
	}
}
