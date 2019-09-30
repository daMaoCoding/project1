package com.xinbo.fundstransfer.report.acc;

import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.*;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalTrans;
import com.xinbo.fundstransfer.domain.repository.AccountRebateRepository;
import com.xinbo.fundstransfer.domain.repository.OutwardRequestRepository;
import com.xinbo.fundstransfer.domain.repository.OutwardTaskRepository;
import com.xinbo.fundstransfer.domain.repository.SysLogRepository;
import com.xinbo.fundstransfer.report.*;
import com.xinbo.fundstransfer.report.store.StoreHandler;
import com.xinbo.fundstransfer.service.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 异常流水处理基类
 */
public abstract class Error {

	protected static final Logger log = LoggerFactory.getLogger(Error.class);

	/* 问题排查注解前缀 */
	public static final String ERROR_ACC = "ERROR_ACC";

	@Autowired
	@Lazy
	protected AccountService accSer;
	@Autowired
	protected OutwardTaskService oTaskSer;
	@Autowired
	protected AccountRebateService accRebateSer;
	@Autowired
	protected RebateApiService rebateApiSer;
	@Autowired
	protected SysUserService userSer;
	@Autowired
	protected SysLogService sysLogSer;
	@Autowired
	protected SysInvstService sysInvstSer;
	@Autowired
	protected HandicapService handicapSer;
	@Autowired
	protected SysLogRepository sysLogDao;
	@Autowired
	protected SysErrService sysErrSer;
	@Autowired
	protected AccountRebateRepository accRebateDao;
	@Autowired
	protected OutwardTaskRepository outwardTaskDao;
	@Autowired
	protected OutwardRequestRepository oReqDao;
	@Autowired
	private FinLessStatService finLessStatSer;
	@Autowired
	private BankLogService bankLogService;
	@Autowired
	protected StoreHandler storeHandler;
	@Autowired
	protected SystemAccountCommon systemAccountCommon;
	@Autowired
	@Lazy
	protected AllocateOutwardTaskService allocOTaskSer;
	@Autowired
	protected RedisTemplate accountingRedisTemplate;
	@Autowired
	protected StringRedisTemplate accountingStringRedisTemplate;

	protected static final int Refunding = BankLogStatus.Refunding.getStatus();
	protected static final int Refunded = BankLogStatus.Refunded.getStatus();

	protected static final int InBank = AccountType.InBank.getTypeId();
	protected static final int InThird = AccountType.InThird.getTypeId();
	protected static final int OutBank = AccountType.OutBank.getTypeId();
	protected static final int ReserveBank = AccountType.ReserveBank.getTypeId();

	protected abstract String deal(Long errorId, int target, Object data, String remark, SysUser operator,
			String[] others, List<ActionDeStruct> actionDeStructList);

	protected ErrorOpp genOpp(BizBankLog lg) {
		if (Objects.isNull(lg))
			return new ErrorOpp();
		AccountBaseInfo base = accSer.getFromCacheById(lg.getFromAccount());
		Integer handicap = Objects.nonNull(base) ? base.getHandicapId() : null;
		return new ErrorOpp(null, handicap, lg.getToAccount(), lg.getToAccountOwner(), null, null);
	}

	protected ErrorOpp genOpp(AccountBaseInfo t) {
		if (Objects.isNull(t))
			return new ErrorOpp();
		return new ErrorOpp(t.getId(), t.getHandicapId(), t.getAccount(), t.getOwner(), null, null);
	}

	protected ErrorOpp genOpp(BizOutwardTask t) {
		if (Objects.isNull(t))
			return new ErrorOpp();
		BizHandicap handicap = handicapSer.findFromCacheByCode(t.getHandicap());
		Integer handicapId = Objects.nonNull(handicap) ? handicap.getId() : null;
		return new ErrorOpp(null, handicapId, t.getToAccount(), t.getToAccountOwner(), t.getId(), t.getOrderNo());
	}

	protected ErrorOpp genOpp(BizAccountRebate t) {
		if (Objects.isNull(t))
			return new ErrorOpp();
		return new ErrorOpp(null, t.getHandicap(), t.getToAccount(), t.getToHolder(), t.getId(), t.getTid());
	}

	/**
	 * 检测：流水金额
	 */
	protected String checkBankLogAmount(boolean positive, BigDecimal amount, BigDecimal upLimit) {
		if (Objects.isNull(amount))
			return StringUtils.EMPTY;
		boolean plus = amount.compareTo(BigDecimal.ZERO) > 0;
		if (plus && !positive)
			return "流水金额不能大于0";
		if (!plus && positive)
			return "流水金额不能小于0";
		if (upLimit != null && amount.abs().compareTo(upLimit.abs()) > 0) {
			return "流水金额大于" + upLimit;
		}
		return StringUtils.EMPTY;
	}

	/**
	 * 检测 流水是否已经被处理
	 */
	protected String checkBankLog(Integer accId, Long bankLogId) {
		if (Objects.isNull(accId) || Objects.isNull(bankLogId))
			return StringUtils.EMPTY;
		List<BizSysInvst> invstList = sysInvstSer.findByAccountIdAndBankLogId(accId, bankLogId);
		return !CollectionUtils.isEmpty(invstList) ? "该流水已经被处理" : StringUtils.EMPTY;
	}

	/**
	 * 检测 BizSysErr信息
	 */
	protected String checkError(BizSysErr err, SysUser operator) {
		if (err == null)
			return "该记录不存在";
		if (!Objects.equals(operator.getId(), err.getCollector()))
			return "操作者与锁定者不符";
		if (!Objects.equals(err.getStatus(), SysErrStatus.Locked.getStatus()))
			return "该记录未被锁定或已被处理";
		return StringUtils.EMPTY;
	}

	/**
	 * 降低兼职人员信用额度
	 */
	protected String reduceCredit(Integer accId, BigDecimal margin, String uid, String remark) {
		if (accId == null || margin == null)
			return StringUtils.EMPTY;
		try {
			boolean ret = finLessStatSer.derating(accId, margin.abs(), uid, remark);
			return ret ? StringUtils.EMPTY : "降低兼职人员信用额度失败";
		} catch (Exception e) {
			return "降低兼职人员信用额度失败";
		}
	}

	protected String saveErrorBankLog(BizBankLog lg, SysUser operator, String action) {
		String opr = Objects.isNull(operator) ? "系统" : operator.getUid();
		lg.setRemark(opr + "-异常流水" + StringUtils.trimToEmpty(action) + StringUtils.trimToEmpty(lg.getRemark()));
		if (Objects.equals(lg.getStatus(), BankLogStatus.Matching.getStatus())) {
			AccountBaseInfo base = accSer.getFromCacheById(lg.getFromAccount());
			if (Objects.isNull(base) || !Objects.equals(AccountType.InBank.getTypeId(), base.getType())
					|| lg.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
				lg.setStatus(BankLogStatus.Disposed.getStatus());
			}
		}
		bankLogService.save(lg);
		return StringUtils.EMPTY;
	}

	/**
	 * 出款任务：重新分配
	 */
	protected boolean deal4Outward(AccountBaseInfo base, Long taskId, Date bkTdTm, String remark, SysBalTrans ts) {
		if (!SystemAccountConfiguration.needAutoAssignIfTransactionFail())
			return false;
		if (Objects.isNull(base) || Objects.isNull(taskId))
			return false;
		BizOutwardTask task = oTaskSer.findById(taskId);
		if (Objects.isNull(task))
			return false;
		if (Objects.equals(OutwardTaskStatus.Invalid.getStatus(), task.getStatus())) {
			log.info(
					"SB{} ERROR{} [ RE-ASSIGN OUTWARD INVALID ] >> the task has already been recreated by robot or someone. taskId:{} ,amount:{} ,toAccount:{} toOwner:{} remark: {}",
					base.getId(), taskId, taskId, task.getAmount(), task.getToAccount(), task.getToAccountOwner(),
					remark);
			return true;
		}
		if (Objects.equals(OutwardTaskStatus.ManageCancel.getStatus(), task.getStatus())) {
			log.info(
					"SB{} ERROR{} [ RE-ASSIGN OUTWARD MANAGE-CANCEL ] >> monitor task status. taskId:{} ,amount:{} ,toAccount:{} toOwner:{} remark: {}",
					base.getId(), taskId, taskId, task.getAmount(), task.getToAccount(), task.getToAccountOwner(),
					remark);
			return true;
		}
		if (Objects.equals(OutwardTaskStatus.ManageRefuse.getStatus(), task.getStatus())) {
			log.info(
					"SB{} ERROR{} [ RE-ASSIGN OUTWARD MANAGE-REFUSE ] >> monitor task status. taskId:{} ,amount:{} ,toAccount:{} toOwner:{} remark: {}",
					base.getId(), taskId, taskId, task.getAmount(), task.getToAccount(), task.getToAccountOwner(),
					remark);
			return true;
		}
		BizOutwardRequest req = Objects.nonNull(task.getOutwardRequestId())
				? oReqDao.findOne(task.getOutwardRequestId()) : null;
		if (Objects.nonNull(req)) {
			if (Objects.equals(OutwardRequestStatus.Canceled.getStatus(), req.getStatus())) {
				log.info(
						"SB{} ERROR{} [ RE-ASSIGN OUTWARD MANAGE-REFUSE ] >> monitor req status. taskId:{} ,amount:{} ,toAccount:{} toOwner:{} remark: {}",
						base.getId(), taskId, taskId, task.getAmount(), task.getToAccount(), task.getToAccountOwner(),
						remark);
				return true;
			}
			if (Objects.equals(OutwardRequestStatus.Reject.getStatus(), req.getStatus())) {
				log.info(
						"SB{} ERROR{} [ RE-ASSIGN OUTWARD MANAGE-REFUSE ] >> monitor req status. taskId:{} ,amount:{} ,toAccount:{} toOwner:{} remark: {}",
						base.getId(), taskId, taskId, task.getAmount(), task.getToAccount(), task.getToAccountOwner(),
						remark);
				return true;
			}
		}
		// 此任务在 正在分配队列 中
		if (Objects.isNull(task.getAccountId())) {
			log.info(
					"SB{} ERROR{} [ RE-ASSIGN OUTWARD ] >> the task has already been inserted into allocating queue. taskId:{} ,amount:{} ,toAccount:{} toOwner:{}  remark: {}",
					base.getId(), taskId, taskId, task.getAmount(), task.getToAccount(), task.getToAccountOwner(),
					remark);
			return true;
		}
		// 此任务已经分配出去
		// 1.分配账号 与原账号不一致 或 2.分配时间大于流水交易时间 或 3.任务状态为正在出款
		Date asignTm = task.getAsignTime();
		if (!Objects.equals(task.getAccountId(), base.getId())
				|| Objects.nonNull(asignTm) && Objects.nonNull(bkTdTm)
						&& (Math.abs(SystemAccountUtils.currentDayStartMillis() - bkTdTm.getTime()) > 10000)
						&& asignTm.getTime() > bkTdTm.getTime()
				|| Objects.equals(OutwardTaskStatus.Undeposit.getStatus(), task.getStatus())
						&& (Objects.isNull(task.getAccountId())
								|| !Objects.equals(task.getAccountId(), base.getId()))) {
			log.info(
					"SB{} ERROR{} [ RE-ASSIGN OUTWARD ] >> the task has already been allocated to the other account [] . taskId:{} ,amount:{} ,toAccount:{} toOwner:{} remark: {}",
					base.getId(), taskId, task.getAccountId(), taskId, task.getAmount(), task.getToAccount(),
					task.getToAccountOwner(), remark);
			return true;
		}
		try {
			// 未知转主管处理
			if (Objects.equals(OutwardTaskStatus.Unknown.getStatus(), task.getStatus())) {
				allocOTaskSer.alterStatusToMgr(task, null, remark, task.getScreenshot());
				log.info(
						"SB{} ERROR{} [ TRANSFER-MANAGER OUTWARD ] >>  amount:{} ,toAccount:{} toOwner:{}  orderNo: {}  remark: {}",
						base.getId(), taskId, task.getAmount(), task.getToAccount(), task.getToAccountOwner(),
						task.getOrderNo(), remark);
				return true;
			}
			boolean transout = SysBalUtils.transOut(base, ts);
			systemAccountCommon.invalid(base, task, remark, transout);
			log.info("SB{} ERROR{} [ RE-ASSIGN OUTWARD ] >>  amount:{} ,toAccount:{} toOwner:{} transout:{} remark: {}",
					base.getId(), taskId, task.getAmount(), task.getToAccount(), task.getToAccountOwner(), transout,
					remark);
		} catch (Exception e) {
			log.info(
					"SB{} ERROR{} [ RE-ASSIGN OUTWARD ] >> occur exception when allocate the task . amount:{} ,toAccount:{} toOwner:{} remark: {}",
					base.getId(), taskId, task.getAmount(), task.getToAccount(), task.getToAccountOwner(), remark);
		}
		return true;
	}

	/**
	 * 返利提现：直接取消该任务
	 */
	protected boolean deal4Rebate(AccountBaseInfo base, Long taskId, Date bkTdTm, String remark, SysBalTrans ts) {
		if (!SystemAccountConfiguration.needAutoAssignIfTransactionFail())
			return false;
		if (Objects.isNull(base) || Objects.isNull(taskId))
			return false;
		BizAccountRebate task = accRebateSer.findById(taskId);
		if (Objects.isNull(task))
			return false;
		if (Objects.equals(OutwardTaskStatus.Invalid.getStatus(), task.getStatus())) {
			log.info(
					"SB{} ERROR{} [ RE-ASSIGN REBATE INVALID ] >> the task has already been recreated by robot or someone. taskId:{} ,amount:{} ,toAccount:{} toOwner:{}",
					base.getId(), taskId, taskId, task.getAmount(), task.getToAccount(), task.getToHolder());
			return true;
		}
		// 此任务在 正在分配队列 中
		if (Objects.isNull(task.getAccountId())) {
			log.info(
					"SB{} ERROR{} [ RE-ASSIGN REBATE ] >> the task has already been inserted into allocating queue. taskId:{} ,amount:{} ,toAccount:{} toOwner:{}",
					base.getId(), taskId, taskId, task.getAmount(), task.getToAccount(), task.getToHolder());
			return true;
		}
		// 此任务已经分配出去
		// 1.分配账号 与原账号不一致 或 2.分配时间大于流水交易时间 或 3.任务状态为正在出款
		Date asignTm = task.getAsignTime();
		if (!Objects.equals(task.getAccountId(), base.getId())
				|| Objects.nonNull(asignTm) && Objects.nonNull(bkTdTm)
						&& (Math.abs(SystemAccountUtils.currentDayStartMillis() - bkTdTm.getTime()) > 10000)
						&& asignTm.getTime() > bkTdTm.getTime()
				|| Objects.equals(OutwardTaskStatus.Undeposit.getStatus(), task.getStatus())
						&& (Objects.isNull(task.getAccountId())
								|| !Objects.equals(task.getAccountId(), base.getId()))) {
			log.info(
					"SB{} ERROR{} [ RE-ASSIGN REBATE ] >> the task has already been allocated to the other account [] . taskId:{} ,amount:{} ,toAccount:{} toOwner:{}",
					base.getId(), taskId, task.getAccountId(), taskId, task.getAmount(), task.getToAccount(),
					task.getToHolder());
			return true;
		}
		// 此任务已经取消
		if (Objects.equals(OutwardTaskStatus.ManageCancel.getStatus(), task.getStatus())) {
			log.info(
					"SB{} ERROR{} [ RE-ASSIGN REBATE ] >> the task has already been cancelled . taskId:{} ,amount:{} ,toAccount:{} toOwner:{}",
					base.getId(), taskId, task.getAccountId(), taskId, task.getAmount(), task.getToAccount(),
					task.getToHolder());
			return true;
		}
		SysUser sys = userSer.findFromCacheById(AppConstants.USER_ID_4_ADMIN);
		// 未知转主管处理
		if (Objects.equals(OutwardTaskStatus.Unknown.getStatus(), task.getStatus())) {
			rebateApiSer.fail(taskId, remark, sys);
			return true;
		}
		boolean transout = SysBalUtils.transOut(base, ts);
		if (transout) {
			rebateApiSer.cancel(taskId, remark, sys, true);
		} else {
			rebateApiSer.fail(taskId, remark, sys);
		}
		return true;
	}
}
