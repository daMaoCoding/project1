package com.xinbo.fundstransfer.report.store;

import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.enums.BankLogStatus;
import com.xinbo.fundstransfer.domain.enums.SysLogStatus;
import com.xinbo.fundstransfer.domain.enums.SysLogType;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalTrans;
import com.xinbo.fundstransfer.domain.repository.*;
import com.xinbo.fundstransfer.report.SystemAccountUtils;
import com.xinbo.fundstransfer.report.up.ReportInvstError;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.HandicapService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class DataBaseManager {
	@Autowired
	private SysLogRepository sysLogDao;
	@Autowired
	@Lazy
	private AccountRepository accDao;
	@Autowired
	private AccountService accSer;
	@Autowired
	private OutwardTaskRepository outwardTaskDao;
	@Autowired
	private AccountRebateRepository accountRebateDao;
	@Autowired
	private HandicapService handicapSer;
	@Autowired
	IncomeRequestRepository incomeRequestDao;
	@Autowired
	SysInvstRepository sysInvstDao;

	@Transactional
	public BizSysLog invst(ReportInvstError invstError, AccountBaseInfo base, SysUser operator) {
		if (Objects.isNull(invstError) || Objects.isNull(base) || Objects.isNull(operator))
			return null;
		BizSysLog ref = new BizSysLog();
		ref.setAccountId(base.getId());
		ref.setBankLogId(null);
		ref.setAmount(invstError.getDiff());
		ref.setFee(BigDecimal.ZERO);
		ref.setOppId(null);
		ref.setOppHandicap(base.getHandicapId());
		ref.setOppAccount(null);
		ref.setOppOwner(operator.getUid());
		ref.setOrderId(null);
		ref.setOrderNo(SystemAccountUtils.generateId());
		ref.setType(SysLogType.InvstError.getTypeId());
		ref.setStatus(SysLogStatus.Valid.getStatusId());
		ref.setCreateTime(new Date());
		ref.setSummary(invstError.getRemark() + "-" + invstError.getErrorId());
		ref.setSuccessTime(ref.getCreateTime());
		ref.setRemark(null);
		ref.setBankBalance(invstError.getBankBal());
		ref.setBalance(invstError.getSysBal());
		return sysLogDao.saveAndFlush(ref);
	}

	@Transactional
	public BizSysLog refund(SysBalTrans trans, BizBankLog log, BigDecimal[] bs) {
		if (Objects.isNull(trans) || trans.getSysLgId() == 0)
			return null;
		BizSysLog sys = sysLogDao.findOne(trans.getSysLgId());
		if (Objects.isNull(sys))
			return null;
		// sys.setStatus(SysLogStatus.Invalid.getStatusId());
		// sys.setUpdateTime(new Date());
		// sysLogDao.saveAndFlush(sys);
		BizSysLog ref = new BizSysLog();
		ref.setAccountId(sys.getAccountId());
		ref.setBankLogId(Objects.isNull(log) ? null : log.getId());
		ref.setAmount(sys.getAmount().abs());
		ref.setFee(Objects.isNull(sys.getFee()) ? BigDecimal.ZERO : sys.getFee().abs());
		ref.setOppId(sys.getOppId());
		ref.setOppHandicap(sys.getOppHandicap());
		ref.setOppAccount(sys.getOppAccount());
		ref.setOppOwner(sys.getOppOwner());
		ref.setOrderId(sys.getOrderId());
		ref.setOrderNo(sys.getOrderNo());
		// ref.setType(SysLogType.Refund.getTypeId());
		// ref.setStatus(SysLogStatus.Invalid.getStatusId());
		// ref.setCreateTime(new Date());
		// ref.setSummary(log.getSummary());
		ref.setType(sys.getType());
		ref.setStatus(SysLogStatus.Valid.getStatusId());
		ref.setCreateTime(sys.getCreateTime());
		ref.setSummary("冲正-" + StringUtils.trimToEmpty(Objects.isNull(log) ? StringUtils.EMPTY : log.getSummary()));
		ref.setSuccessTime(new Date());
		ref.setRemark(null);
		ref.setBankBalance(bs[0]);
		ref.setBalance(bs[1]);
		return sysLogDao.saveAndFlush(ref);
	}

	@Transactional
	public BizSysLog transFrom(SysBalTrans trans, BigDecimal fee, BizBankLog log, BigDecimal[] bs) {
		if (trans.getFrId() == 0)
			return null;
		if (trans.getSysLgId() != 0) {
			BizSysLog sys = sysLogDao.findOne(trans.getSysLgId());
			if (Objects.equals(sys.getStatus(), SysLogStatus.NoOwner.getStatusId())) {
				sys.setStatus(SysLogStatus.Valid.getStatusId());
				sys.setUpdateTime(new Date());
				sys.setSuccessTime(sys.getUpdateTime());
				sys = sysLogDao.saveAndFlush(sys);
			}
			return sys;
		}
		BizSysLog ref = new BizSysLog();
		ref.setAccountId(trans.getFrId());
		ref.setBankLogId(Objects.isNull(log) ? null : log.getId());
		ref.setAmount(trans.getAmt().abs().negate());
		ref.setFee(Objects.isNull(fee) ? BigDecimal.ZERO : fee.abs().negate());
		if (trans.getToId() != 0) {
			AccountBaseInfo toBase = accSer.getFromCacheById(trans.getToId());
			if (Objects.nonNull(toBase)) {
				ref.setOppId(trans.getToId());
				ref.setOppAccount(toBase.getAccount());
				ref.setOppHandicap(toBase.getHandicapId());
				ref.setOppOwner(toBase.getOwner());
				ref.setType(SysLogType.Transfer.getTypeId());
			}
		} else {
			if (SysBalTrans.TASK_TYPE_OUTREBATE == trans.getTaskType()) {
				BizAccountRebate task = accountRebateDao.findById2(trans.getTaskId());
				if (Objects.nonNull(task)) {
					ref.setOppHandicap(task.getHandicap());
					ref.setOppAccount(task.getToAccount());
					ref.setOppOwner(task.getToHolder());
					ref.setOrderId(task.getId());
					ref.setOrderNo(task.getTid());
					ref.setType(SysLogType.Rebate.getTypeId());
				}
			} else if (SysBalTrans.TASK_TYPE_OUTMEMEBER == trans.getTaskType()) {
				BizOutwardTask task = outwardTaskDao.findById2(trans.getTaskId());
				if (Objects.nonNull(task)) {
					BizHandicap handicap = handicapSer.findFromCacheByCode(task.getHandicap());
					if (Objects.nonNull(handicap))
						ref.setOppHandicap(handicap.getId());
					ref.setOppAccount(task.getToAccount());
					ref.setOppOwner(task.getToAccountOwner());
					ref.setOrderId(task.getId());
					ref.setOrderNo(task.getOrderNo());
					ref.setType(SysLogType.Outward.getTypeId());
				}
			}
		}
		if (StringUtils.isEmpty(ref.getOrderNo())) {
			if (trans.getOppSysLgId() != 0) {
				BizSysLog opp = sysLogDao.findOne(trans.getOppSysLgId());
				ref.setOrderNo(opp.getOrderNo());
				ref.setOrderId(opp.getOrderId());
			}
		}
		ref.setRemark(null);
		if (trans.getAckByCurrResult1() == SysBalTrans.ACK_ACK) {
			ref.setSummary("转账结果状态1确认");
		} else {
			ref.setSummary(Objects.isNull(log) ? null : log.getSummary());
		}
		ref.setCreateTime(trans.getGetTm() != 0 ? new Date(trans.getGetTm()) : new Date());
		if (trans.ackFr()) {
			ref.setStatus(SysLogStatus.Valid.getStatusId());
			ref.setSuccessTime(new Date());
			ref.setBankBalance(bs[0]);
			ref.setBalance(bs[1]);
		} else {
			ref.setStatus(SysLogStatus.Valid.getStatusId());
		}
		if (StringUtils.isBlank(ref.getOrderNo()))
			ref.setOrderNo(SystemAccountUtils.generateId());
		return sysLogDao.saveAndFlush(ref);
	}

	@Transactional
	public BizSysLog transTo(SysBalTrans trans, BizBankLog log, BigDecimal[] bs) {
		if (trans.getToId() == 0)
			return null;
		if (trans.getOppSysLgId() != 0) {
			BizSysLog opp = sysLogDao.findOne(trans.getOppSysLgId());
			if (Objects.equals(opp.getStatus(), SysLogStatus.NoOwner.getStatusId())) {
				opp.setUpdateTime(new Date());
				opp.setStatus(SysLogStatus.Valid.getStatusId());
				opp.setSuccessTime(opp.getUpdateTime());
				sysLogDao.saveAndFlush(opp);
			}
			return opp;
		}
		BizSysLog opp = new BizSysLog();
		opp.setAccountId(trans.getToId());
		opp.setBankLogId(Objects.isNull(log) ? null : log.getId());
		opp.setAmount(trans.getAmt().abs());
		opp.setFee(BigDecimal.ZERO);
		if (trans.getFrId() != 0) {
			AccountBaseInfo frBase = accSer.getFromCacheById(trans.getFrId());
			if (Objects.nonNull(frBase)) {
				opp.setOppId(trans.getFrId());
				opp.setOppHandicap(frBase.getHandicapId());
				opp.setType(SysLogType.Transfer.getTypeId());
				opp.setOppOwner(frBase.getOwner());
				if (Objects.equals(AccountType.InThird.getTypeId(), frBase.getType())
						|| Objects.equals(AccountType.OutThird.getTypeId(), frBase.getType())) {
					opp.setOppAccount(genAccountSimp(frBase.getAccount()));
					opp.setSummary(genSummaryTh3(frBase.getBankName(), frBase.getAccount()));
				} else {
					opp.setOppAccount(frBase.getAccount());
				}
			}
		}
		if (trans.getSysLgId() != 0) {
			BizSysLog sys = sysLogDao.findOne(trans.getSysLgId());
			if (Objects.nonNull(sys)) {
				opp.setOrderNo(sys.getOrderNo());
				opp.setOrderId(sys.getOrderId());
			}
		}
		opp.setRemark(null);
		if (StringUtils.isEmpty(opp.getSummary()))
			opp.setSummary(Objects.isNull(log) ? null : log.getSummary());
		opp.setCreateTime(trans.getGetTm() != 0 ? new Date(trans.getGetTm()) : new Date());
		if (trans.ackTo()) {
			opp.setStatus(SysLogStatus.Valid.getStatusId());
			opp.setSuccessTime(new Date());
			opp.setBankBalance(bs[0]);
			opp.setBalance(bs[1]);
		} else {
			opp.setStatus(SysLogStatus.NoOwner.getStatusId());
		}
		if (StringUtils.isBlank(opp.getOrderNo()))
			opp.setOrderNo(SystemAccountUtils.generateId());
		opp = sysLogDao.saveAndFlush(opp);
		return opp;
	}

	@Transactional
	public BizSysLog transInBank(Integer accId, BizBankLog log, BigDecimal[] bs) {
		if (Objects.isNull(accId) || Objects.isNull(log))
			return null;
		AccountBaseInfo base = accSer.getFromCacheById(accId);
		if (Objects.isNull(base))
			return null;
		BizSysLog ref = new BizSysLog();
		ref.setAccountId(accId);
		ref.setBankLogId(log.getId());
		ref.setAmount(log.getAmount().abs());
		ref.setFee(BigDecimal.ZERO);
		ref.setOppId(null);
		ref.setOppAccount(log.getToAccount());
		ref.setOppOwner(log.getToAccountOwner());
		ref.setOppHandicap(base.getHandicapId());
		ref.setType(SysLogType.Income.getTypeId());
		ref.setOrderNo(log.getOrderNo());
		ref.setOrderId(log.getTaskId());
		ref.setRemark(null);
		ref.setSummary(log.getSummary());
		ref.setCreateTime(new Date());
		ref.setStatus(SysLogStatus.Valid.getStatusId());
		ref.setSuccessTime(ref.getCreateTime());
		ref.setBankBalance(bs[0]);
		ref.setBalance(bs[1]);
		return sysLogDao.saveAndFlush(ref);
	}

	@Transactional
	public BizSysLog transTh3(SysBalTrans trans, BizBankLog log, BigDecimal[] bs) {
		BizAccount accTh3 = accSer.getById(trans.getFrId());
		BizIncomeRequest orderTh3 = incomeRequestDao.findById2(trans.getTaskId());
		if (Objects.isNull(orderTh3) || Objects.isNull(accTh3))
			return null;
		BizSysLog lg = new BizSysLog();
		lg.setAccountId(orderTh3.getToId());
		lg.setBankLogId(Objects.isNull(log) ? null : log.getId());
		lg.setAmount(trans.getAmt());
		lg.setFee(null);
		lg.setOppId(accTh3.getId());
		lg.setOppHandicap(accTh3.getHandicapId());
		lg.setOppAccount(genAccountSimp(accTh3.getAccount()));
		lg.setOppOwner(accTh3.getOwner());
		lg.setOrderId(orderTh3.getId());
		lg.setOrderNo(null);
		lg.setType(SysLogType.Transfer.getTypeId());
		lg.setCreateTime(trans.getGetTm() != 0 ? new Date(trans.getGetTm()) : new Date());
		lg.setSummary(genSummaryTh3(accTh3.getBankName(), accTh3.getAccount()));
		lg.setRemark(null);
		lg.setStatus(SysLogStatus.Valid.getStatusId());
		lg.setBankBalance(bs[0]);
		lg.setBalance(bs[1]);
		lg.setSuccessTime(lg.getCreateTime());
		if (StringUtils.isBlank(lg.getOrderNo()))
			lg.setOrderNo(SystemAccountUtils.generateId());
		lg = sysLogDao.saveAndFlush(lg);
		return sysLogDao.saveAndFlush(lg);
	}

	@Transactional
	public BizSysLog transInterest(Integer accId, BizBankLog log, BigDecimal[] bs) {
		if (Objects.isNull(accId) || Objects.isNull(log)
				|| !Objects.equals(log.getStatus(), BankLogStatus.Interest.getStatus()))
			return null;
		AccountBaseInfo base = accSer.getFromCacheById(accId);
		if (Objects.isNull(base))
			return null;
		BizSysLog lg = new BizSysLog();
		lg.setAccountId(accId);
		lg.setBankLogId(log.getId());
		lg.setAmount(log.getAmount().abs());
		lg.setFee(null);
		lg.setType(SysLogType.Interest.getTypeId());
		lg.setOppHandicap(base.getHandicapId());
		lg.setOppAccount(log.getToAccount());
		lg.setOppOwner(log.getToAccountOwner());
		lg.setCreateTime(new Date());
		lg.setSummary(log.getSummary());
		lg.setRemark(null);
		lg.setStatus(SysLogStatus.Valid.getStatusId());
		lg.setBankBalance(bs[0]);
		lg.setBalance(bs[1]);
		lg.setSuccessTime(lg.getCreateTime());
		if (StringUtils.isBlank(lg.getOrderNo()))
			lg.setOrderNo(SystemAccountUtils.generateId());
		lg = sysLogDao.saveAndFlush(lg);
		return sysLogDao.saveAndFlush(lg);
	}

	@Transactional
	public BizSysLog enhanceCredit(BizBankLog log, BizIncomeRequest req, BigDecimal[] bs) {
		BizSysLog lg = new BizSysLog();
		lg.setAccountId(log.getFromAccount());
		lg.setBankLogId(log.getId());
		lg.setAmount(log.getAmount().abs());
		lg.setType(SysLogType.Transfer.getTypeId());
		lg.setOppHandicap(req.getHandicap());
		lg.setOppAccount(log.getToAccount());
		lg.setOppOwner(log.getToAccountOwner());
		lg.setCreateTime(req.getCreateTime());
		lg.setSummary("兼职提额-" + StringUtils.trimToEmpty(log.getSummary()));
		lg.setStatus(SysLogStatus.Valid.getStatusId());
		lg.setBankBalance(bs[0]);
		lg.setBalance(bs[1]);
		lg.setSuccessTime(lg.getCreateTime());
		lg.setOrderId(req.getId());
		lg.setOrderNo(req.getOrderNo());
		return sysLogDao.saveAndFlush(lg);
	}

	@Transactional
	public BizSysLog init(BizAccount acc, String summary, SysUser operator) {
		if (Objects.isNull(acc) || Objects.isNull(acc.getBalance()) || Objects.isNull(acc.getBankBalance())
				|| acc.getBalance().compareTo(acc.getBankBalance()) == 0)
			return null;
		String summaryStr = "初始化" + (Objects.isNull(operator) ? StringUtils.EMPTY : ("-" + operator.getUid())) + "-"
				+ StringUtils.trimToEmpty(summary);
		Integer opratorId = Objects.isNull(operator) ? AppConstants.USER_ID_4_ADMIN : operator.getId();
		BizSysLog lg = new BizSysLog();
		lg.setAccountId(acc.getId());
		lg.setBankLogId(null);
		lg.setAmount(acc.getBankBalance().subtract(acc.getBalance()));
		lg.setType(SysLogType.Init.getTypeId());
		lg.setOppHandicap(acc.getHandicapId());
		lg.setOppAccount(acc.getAccount());
		lg.setOppOwner(acc.getOwner());
		lg.setCreateTime(new Date());
		lg.setOperator(opratorId);
		lg.setSummary(summaryStr);
		lg.setStatus(SysLogStatus.Valid.getStatusId());
		lg.setBankBalance(acc.getBankBalance());
		lg.setBalance(acc.getBankBalance());
		lg.setSuccessTime(lg.getCreateTime());
		lg.setOrderId(null);
		lg.setOrderNo(null);
		if (StringUtils.isBlank(lg.getOrderNo()))
			lg.setOrderNo(SystemAccountUtils.generateId());
		return sysLogDao.saveAndFlush(lg);
	}

	@Transactional
	public BizSysLog saveAndFlush(BizSysLog sysLog) {
		return sysLogDao.saveAndFlush(sysLog);
	}

	@Transactional
	public List<BizSysLog> updateByBatch(AccountBaseInfo base, BigDecimal diff, List<BizSysLog> dataList) {
		List<BizSysLog> ret = new ArrayList<>();
		if (Objects.isNull(dataList))
			return ret;
		if (Objects.nonNull(base) && Objects.nonNull(diff) && diff.compareTo(BigDecimal.ZERO) != 0)
			updSysBal(base.getId(), diff, null, diff.compareTo(BigDecimal.ZERO) < 0);
		for (BizSysLog item : dataList) {
			if (Objects.nonNull(item))
				ret.add(sysLogDao.saveAndFlush(item));
		}
		return ret;
	}

	@Transactional
	public BizAccount updSysBal(int target, BigDecimal amt, BigDecimal fee, boolean real) {
		if (target == 0 || Objects.isNull(amt) || amt.compareTo(BigDecimal.ZERO) == 0)
			return null;
		amt = amt.abs();
		fee = Objects.isNull(fee) ? BigDecimal.ZERO : fee.setScale(2, BigDecimal.ROUND_HALF_UP).abs();
		if (real)
			accDao.updSysBal(amt, target, fee);
		else
			accDao.updSysBal(amt.negate(), target, fee.negate());
		return accDao.findById2(target);
	}

	@Transactional
	public BizSysLog findSysOne(long sysLogId) {
		return sysLogDao.findOne(sysLogId);
	}

	@Transactional
	public BizSysLog find4Fee(int accountId, String stTm) {
		return sysLogDao.find4Fee(accountId, stTm);
	}

	@Transactional
	public List<BizSysLog> findSysAll(Integer accountId, BigDecimal amount, Long bankLogId) {
		return sysLogDao.findByAccountIdAndAmountAndBankLogId(accountId, amount, bankLogId);
	}

	@Transactional
	public List<BizSysLog> findSysAll(Integer accountId, int type, Long orderId) {
		return sysLogDao.findByAccountIdAndTypeAndOrderId(accountId, type, orderId);
	}

	private String genAccountSimp(String acc) {
		acc = StringUtils.trimToEmpty(acc);
		return String.format("...%s", (acc.length() <= 5 ? acc : acc.substring(acc.length() - 5)));
	}

	private String genSummaryTh3(String bankName, String acc) {
		return String.format("%s%s", StringUtils.trimToEmpty(bankName), genAccountSimp(acc));
	}
}
