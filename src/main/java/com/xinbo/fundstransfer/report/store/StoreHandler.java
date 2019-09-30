package com.xinbo.fundstransfer.report.store;

import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.SysLogStatus;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalTrans;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.exception.*;
import com.xinbo.fundstransfer.report.up.ReportInvstError;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StoreHandler {
	private static final Logger logger = LoggerFactory.getLogger(StoreHandler.class);
	@Lazy
	@Autowired
	MemoryManager memoryManager;
	@Lazy
	@Autowired
	DataBaseManager dataBaseManager;

	public boolean finishLoad() {
		return memoryManager.finishLoad();
	}

	public List<BizSysLog> findSysLogFromCache(Integer accountId) {
		return memoryManager.findSysLog(accountId);
	}

	public List<BizBankLog> findBankLogFromCache(Integer accountId) {
		return memoryManager.findBankLog(accountId);
	}

	/**
	 * 重复流水判定 （非相同流水，相同流水是指：流水的ID相同）</br>
	 * 判断标准：与工具上报的最近一条数据 1.转账金额一样 2.转账后的余额不为0.00，且相等 3.taskId为空或为0 4.id 不一致
	 */
	public boolean duplicate(BizBankLog lg) {
		try {
			if (Objects.isNull(lg) || lg.getFromAccount() == 0 || Objects.nonNull(lg.getTaskId()) && lg.getTaskId() > 0)
				return false;
			List<BizBankLog> logList = memoryManager.findBankLog(lg.getFromAccount());
			if (CollectionUtils.isEmpty(logList))
				return false;
			if (SysBalUtils.radix2(lg.getBalance()).compareTo(BigDecimal.ZERO) != 0) {
				Date tm = lg.getTradingTime();
				String own = SysBalUtils.last2letters(lg.getToAccountOwner());
				String acc = SysBalUtils.last3letters(lg.getToAccount());
				BigDecimal amt = SysBalUtils.radix2(lg.getAmount()), bal = SysBalUtils.radix2(lg.getBalance());
				for (int index = 0; index < logList.size(); index++) {
					if (index > 100)
						break;
					BizBankLog lg1 = logList.get(index);
					if (Objects.isNull(lg1) || Objects.equals(lg.getId(), lg1.getId()))
						continue;
					BigDecimal amt1 = SysBalUtils.radix2(lg1.getAmount());
					if (amt.abs().compareTo(amt1.abs()) != 0)
						continue;
					if (amt.add(amt1).compareTo(BigDecimal.ZERO) == 0)
						break;
					Date tm1 = lg1.getTradingTime();
					if (Objects.nonNull(tm) && Objects.nonNull(tm1) && Math.abs(tm.getTime() - tm1.getTime()) > 1000)
						break;
					if (bal.compareTo(SysBalUtils.radix2(lg1.getBalance())) == 0
							&& Objects.equals(own, SysBalUtils.last2letters(lg1.getToAccountOwner()))
							&& Objects.equals(acc, SysBalUtils.last3letters(lg1.getToAccount())))
						return true;
					break;
				}
			}
			BizBankLog lg0 = logList.get(0);
			if (Objects.equals(lg.getId(), lg0.getId())
					|| SysBalUtils.radix2(lg.getAmount()).compareTo(SysBalUtils.radix2(lg0.getAmount())) != 0)
				return false;
			BigDecimal lgBal = SysBalUtils.radix2(lg.getBalance());
			boolean ret = lgBal.compareTo(BigDecimal.ZERO) != 0
					&& lgBal.compareTo(SysBalUtils.radix2(lg0.getBalance())) == 0;
			if (ret)
				logger.info("SB{} [ DUPLICATE BANK STATEMENT ] deal id: {} duplicateId: {} amount: {} toowner: {}",
						lg.getFromAccount(), lg.getId(), lg0.getId(), lg.getAmount(), lg.getToAccountOwner());
			return ret;
		} finally {
			memoryManager.addBankLog(lg);
		}
	}

	public List<BizSysLog> findSysAll(Integer accountId, int type, Long orderId) {
		if (Objects.isNull(accountId) || Objects.isNull(orderId))
			return Collections.EMPTY_LIST;
		List<BizSysLog> ret = findSysLogFromCache(accountId).stream()
				.filter(p -> Objects.equals(p.getType(), type) && Objects.equals(p.getOrderId(), orderId))
				.collect(Collectors.toList());
		if (!CollectionUtils.isEmpty(ret))
			return ret;
		return dataBaseManager.findSysAll(accountId, type, orderId);
	}

	public List<BizSysLog> findSysAll(Integer accountId, BigDecimal amount, Long bankLogId) {
		if (Objects.isNull(accountId) || Objects.isNull(amount) || Objects.isNull(bankLogId))
			return Collections.EMPTY_LIST;
		BigDecimal amt = SysBalUtils.radix2(amount);
		List<BizSysLog> ret = findSysLogFromCache(accountId).stream()
				.filter(p -> amt.compareTo(p.getAmount()) == 0 && Objects.equals(p.getBankLogId(), bankLogId))
				.collect(Collectors.toList());
		if (!CollectionUtils.isEmpty(ret))
			return ret;
		return dataBaseManager.findSysAll(accountId, amount, bankLogId);
	}

	public BizSysLog findSysOne(Integer accountId, Long sysLogId) {
		if (Objects.isNull(accountId) || Objects.equals(accountId, 0) || Objects.isNull(sysLogId)
				|| Objects.equals(sysLogId, 0))
			return null;
		Optional<BizSysLog> optional = findSysLogFromCache(accountId).stream()
				.filter(p -> Objects.equals(p.getId(), sysLogId)).findFirst();
		if (optional.isPresent())
			return optional.get();
		return dataBaseManager.findSysOne(sysLogId);
	}

	public BizSysLog find4Fee(int accountId, String stTm) {
		Optional<BizSysLog> optional = findSysLogFromCache(accountId).stream()
				.filter(p -> Objects.equals(SysLogStatus.Valid.getStatusId(), p.getStatus())
						&& p.getAmount().compareTo(BigDecimal.ZERO) < 0 && Objects.nonNull(p.getSuccessTime()))
				.sorted((o1, o2) -> SysBalUtils
						.oneZeroMinus(o2.getSuccessTime().getTime() - o1.getSuccessTime().getTime()))
				.findFirst();
		if (optional.isPresent())
			return optional.get();
		return dataBaseManager.find4Fee(accountId, stTm);
	}

	@Retryable(value = {
			SysLogInvstErrorException.class }, maxAttempts = 10, backoff = @Backoff(delay = 1000, multiplier = 1))
	public void invstError(AccountBaseInfo base, SysUser operator, ReportInvstError invstError) {
		if (Objects.isNull(base) || Objects.isNull(operator) || Objects.isNull(invstError))
			return;
		try {
			BizSysLog sys = dataBaseManager.invst(invstError, base, operator);
			if (Objects.nonNull(sys))
				memoryManager.addSysLog(sys);
		} catch (Exception e) {
			String msg = StringUtils.trimToEmpty(e.getMessage()) + StringUtils.trimToEmpty(e.getLocalizedMessage());
			logger.error(
					"SB{} [ InvstError Exception  ] >> target: {} diff: {} sysBal: {} bankBal: {} remark: {} errorId: {}  message : {}",
					base.getId(), base.getId(), invstError.getDiff(), invstError.getSysBal(), invstError.getBankBal(),
					invstError.getRemark(), invstError.getErrorId(), msg);
			throw new SysLogInvstErrorException(msg);
		}
	}

	@Recover
	public void recore(SysLogInvstErrorException exception, AccountBaseInfo base, SysUser operator,
			ReportInvstError invstError) {
	}

	@Retryable(value = {
			SysLogRefundException.class }, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 1))
	public long[] refund(SysBalTrans trans, BizBankLog log, BigDecimal[] bs) {
		long[] ret = new long[] { 0, 0 };
		if (Objects.isNull(trans))
			return ret;
		try {
			BizSysLog ref = dataBaseManager.refund(trans, log, bs);
			if (Objects.nonNull(ref)) {
				memoryManager.addSysLog(ref);
				ret[0] = ref.getId();
			}
		} catch (Exception e) {
			String msg = StringUtils.trimToEmpty(e.getMessage()) + StringUtils.trimToEmpty(e.getLocalizedMessage());
			logger.error("SB{} [ Refund Exception  ] >> target: {} amt: {}  message : {}", trans.getFrId(),
					trans.getFrId(), trans.getAmt(), msg);
			throw new SysLogRefundException(msg);
		}
		return ret;
	}

	@Recover
	public void recore(SysLogRefundException exception, SysBalTrans trans, BizBankLog log, BigDecimal[] bs) {
	}

	@Retryable(value = { SysLogFromException.class }, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 1))
	public long[] transFrom(SysBalTrans trans, BigDecimal fee, BizBankLog log, BigDecimal[] bs) {
		long[] ret = new long[] { trans.getSysLgId(), trans.getOppSysLgId() };
		try {
			BizSysLog ref = dataBaseManager.transFrom(trans, fee, log, bs);
			if (Objects.nonNull(ref)) {
				memoryManager.addSysLog(ref);
				ret[0] = ref.getId();
				ret[1] = trans.getOppSysLgId();
			}
		} catch (Exception e) {
			String msg = StringUtils.trimToEmpty(e.getMessage()) + StringUtils.trimToEmpty(e.getLocalizedMessage());
			logger.error("SB{} [ From Exception  ] >> target: {} amt: {}  message : {}", trans.getFrId(),
					trans.getFrId(), trans.getAmt(), msg);
			throw new SysLogFromException(msg);
		}
		return ret;
	}

	@Recover
	public void recore(SysLogFromException exception, SysBalTrans trans, BigDecimal fee, BizBankLog log,
			BigDecimal[] bs) {
	}

	@Retryable(value = { SysLogToException.class }, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 1))
	public long[] transTo(SysBalTrans trans, BizBankLog log, BigDecimal[] bs) {
		long[] ret = new long[] { trans.getSysLgId(), trans.getOppSysLgId() };
		try {
			BizSysLog ref = dataBaseManager.transTo(trans, log, bs);
			if (Objects.nonNull(ref)) {
				memoryManager.addSysLog(ref);
				ret[0] = trans.getSysLgId();
				ret[1] = ref.getId();
			}
		} catch (Exception e) {
			String msg = StringUtils.trimToEmpty(e.getMessage()) + StringUtils.trimToEmpty(e.getLocalizedMessage());
			logger.error("SB{} [ To Exception  ] >> target: {} amt: {}  message : {}", trans.getToId(), trans.getToId(),
					trans.getAmt(), msg);
			throw new SysLogToException(msg);
		}
		return ret;
	}

	@Recover
	public void recore(SysLogToException exception, SysBalTrans trans, BizBankLog log, BigDecimal[] bs) {
	}

	@Retryable(value = {
			SysLogInBankException.class }, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 1))
	public long[] transInBank(Integer accId, BizBankLog log, BigDecimal[] bs) {
		long[] ret = new long[] { 0, 0 };
		if (Objects.isNull(log))
			return ret;
		try {
			BizSysLog ref = dataBaseManager.transInBank(accId, log, bs);
			if (Objects.nonNull(ref)) {
				memoryManager.addSysLog(ref);
				ret[1] = ref.getId();
			}
		} catch (Exception e) {
			String msg = StringUtils.trimToEmpty(e.getMessage()) + StringUtils.trimToEmpty(e.getLocalizedMessage());
			logger.error("SB{} [ InBank Exception  ] >> target: {} amt: {}  message : {}", log.getFromAccount(),
					log.getFromAccount(), log.getAmount(), msg);
			throw new SysLogInBankException(msg);
		}
		return ret;
	}

	@Recover
	public void recore(SysLogInBankException exception, Integer accId, BizBankLog log, BigDecimal[] bs) {
	}

	@Retryable(value = { SysLogTh3Exception.class }, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 1))
	public long[] transTh3(SysBalTrans trans, BizBankLog log, BigDecimal[] bs) {
		long[] ret = new long[] { 0, 0 };
		if (Objects.isNull(trans))
			return ret;
		try {
			BizSysLog ref = dataBaseManager.transTh3(trans, log, bs);
			if (Objects.nonNull(ref)) {
				memoryManager.addSysLog(ref);
				ret[1] = ref.getId();
			}
		} catch (Exception e) {
			String msg = StringUtils.trimToEmpty(e.getMessage()) + StringUtils.trimToEmpty(e.getLocalizedMessage());
			logger.error("SB{} [ Th3 Exception  ] >> target: {} amt: {}  message : {}", trans.getToId(),
					trans.getToId(), trans.getAmt(), msg);
			throw new SysLogTh3Exception(msg);
		}
		return ret;
	}

	@Recover
	public void recore(SysLogTh3Exception exception, SysBalTrans trans, BizBankLog log, BigDecimal[] bs) {
	}

	@Retryable(value = {
			SysLogInterestException.class }, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 1))
	public long[] transInterest(Integer accId, BizBankLog log, BigDecimal[] bs) {
		long[] ret = new long[] { 0, 0 };
		if (Objects.isNull(log))
			return ret;
		try {
			BizSysLog ref = dataBaseManager.transInterest(accId, log, bs);
			if (Objects.nonNull(ref)) {
				memoryManager.addSysLog(ref);
				ret[1] = ref.getId();
			}
		} catch (Exception e) {
			String msg = StringUtils.trimToEmpty(e.getMessage()) + StringUtils.trimToEmpty(e.getLocalizedMessage());
			logger.error("SB{} [ Interest Exception  ] >> target: {} amt: {}  message : {}", log.getFromAccount(),
					log.getFromAccount(), log.getAmount(), msg);
			throw new SysLogInterestException(msg);
		}
		return ret;
	}

	@Recover
	public void recore(SysLogInterestException exception, Integer accId, BizBankLog log, BigDecimal[] bs) {
	}

	@Retryable(value = {
			SysLogEnhanceCreditException.class }, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 1))
	public long[] enhanceCredit(BizBankLog log, BizIncomeRequest req, BigDecimal[] bs) {
		long[] ret = new long[] { 0, 0 };
		if (Objects.isNull(log))
			return ret;
		try {
			BizSysLog ref = dataBaseManager.enhanceCredit(log, req, bs);
			if (Objects.nonNull(ref)) {
				memoryManager.addSysLog(ref);
				ret[1] = ref.getId();
			}
		} catch (Exception e) {
			String msg = StringUtils.trimToEmpty(e.getMessage()) + StringUtils.trimToEmpty(e.getLocalizedMessage());
			logger.error("SB{} [ Enhance Credit Exception  ] >> target: {} amt: {}  message : {}", log.getFromAccount(),
					log.getFromAccount(), log.getAmount(), msg);
			throw new SysLogEnhanceCreditException(msg);
		}
		return ret;
	}

	@Recover
	public void recore(SysLogEnhanceCreditException exception, BizBankLog log, BizIncomeRequest req, BigDecimal[] bs) {
	}

	@Retryable(value = { SysLogInitException.class }, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 1))
	public long[] init(BizAccount acc, String summary, SysUser operator) {
		long[] ret = new long[] { 0, 0 };
		if (Objects.isNull(acc))
			return ret;
		try {
			BizSysLog ref = dataBaseManager.init(acc, summary, operator);
			if (Objects.nonNull(ref)) {
				memoryManager.addSysLog(ref);
				ret[0] = ref.getId();
				ret[1] = ref.getId();
			}
		} catch (Exception e) {
			String msg = StringUtils.trimToEmpty(e.getMessage()) + StringUtils.trimToEmpty(e.getLocalizedMessage());
			logger.error("SB{} [ Init Exception  ] >> target: {} bankBalance: {}  message : {}", acc.getId(),
					acc.getId(), acc.getBankBalance(), msg);
			throw new SysLogInitException(msg);
		}
		return ret;
	}

	@Recover
	public void recore(SysLogInitException exception, BizAccount acc, String summary, SysUser operator) {
	}

	@Retryable(value = {
			SysLogSaveAndFlushException.class }, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 1))
	public BizSysLog saveAndFlush(BizSysLog sysLog) {
		BizSysLog ref = null;
		try {
			ref = dataBaseManager.saveAndFlush(sysLog);
			if (Objects.nonNull(ref)) {
				memoryManager.addSysLog(ref);
			}
		} catch (Exception e) {
			String msg = StringUtils.trimToEmpty(e.getMessage()) + StringUtils.trimToEmpty(e.getLocalizedMessage());
			logger.error("SB{} [ saveAndFlush Exception  ] >> target: {} bankBalance: {}  message : {}",
					sysLog.getAccountId(), sysLog.getAccountId(), sysLog.getAmount(), msg);
			throw new SysLogInitException(msg);
		}
		return ref;
	}

	@Recover
	public void recore(SysLogSaveAndFlushException exception, BizSysLog sysLog) {
	}

	/**
	 * @param diff
	 *            差额（银行余额减去系统余额）
	 */
	@Retryable(value = {
			SysLogUpdateByBatchException.class }, maxAttempts = 10, backoff = @Backoff(delay = 1000, multiplier = 1))
	public void updateByBatch(AccountBaseInfo base, BigDecimal diff, List<BizSysLog> newList, List<BizSysLog> hisList) {
		try {
			dataBaseManager.updateByBatch(base, diff, newList);
		} catch (Exception e) {
			String msg = StringUtils.trimToEmpty(e.getMessage()) + StringUtils.trimToEmpty(e.getLocalizedMessage());
			throw new SysLogUpdateByBatchException(msg);
		}
		for (BizSysLog item : newList)
			memoryManager.addSysLog(dataBaseManager.saveAndFlush(item));
	}

	@Recover
	public void recore(SysLogUpdateByBatchException exception, AccountBaseInfo base, BigDecimal chgAmt,
			List<BizSysLog> newList, List<BizSysLog> hisList) {
		if (Objects.nonNull(hisList))
			hisList.forEach(p -> memoryManager.addSysLog(p));
	}

	/**
	 * 此处千万不要加事务
	 */
	@Retryable(value = { SetSysBalException.class }, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 1))
	public BigDecimal[] setSysBal(StringRedisTemplate template, int target, BigDecimal amt, BigDecimal fee,
			boolean real) {
		BigDecimal[] ret = new BigDecimal[] { null, null };
		try {
			if (Objects.isNull(template))
				return ret;
			template.boundHashOps(RedisKeys.SYS_BAL_LASTTIME).put(String.valueOf(target),
					String.valueOf(System.currentTimeMillis()));
			BizAccount acc = dataBaseManager.updSysBal(target, amt, fee, real);
			if (Objects.nonNull(acc)) {
				ret[0] = acc.getBankBalance();
				ret[1] = acc.getBalance();
			}
		} catch (Exception e) {
			String message = StringUtils.trimToEmpty(e.getMessage()) + StringUtils.trimToEmpty(e.getLocalizedMessage());
			logger.error("SB{} [ SETSYSBAL Exception  ] >> target: {} amt: {} fee: {} real: {}  message : {}", target,
					target, amt, fee, real, e.getMessage());
			throw new SetSysBalException(message);
		}
		return ret;
	}
}
