package com.xinbo.fundstransfer.report.fail;

import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.enums.BankLogStatus;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalTrans;
import com.xinbo.fundstransfer.domain.repository.SysLogRepository;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.SystemAccountCommon;
import com.xinbo.fundstransfer.report.acc.ErrorHandler;
import com.xinbo.fundstransfer.report.store.StoreHandler;
import com.xinbo.fundstransfer.report.up.Report;
import com.xinbo.fundstransfer.report.up.ReportCheck;
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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public abstract class FailCheck {

	/* 处理上报 注解前缀 */
	public static final String PREFIX_FAIL_CHECK = "FAIL_CHECK";

	protected static int Refunding = BankLogStatus.Refunding.getStatus();
	protected static int Refunded = BankLogStatus.Refunded.getStatus();
	protected static int Interest = BankLogStatus.Interest.getStatus();
	protected static int Fee = BankLogStatus.Fee.getStatus();

	protected static int Inbank = AccountType.InBank.getTypeId();
	protected static int BindWechat = AccountType.BindWechat.getTypeId();
	protected static int BindAli = AccountType.BindAli.getTypeId();
	protected static int ThirdCommon = AccountType.ThirdCommon.getTypeId();
	protected static int BindCommon = AccountType.BindCommon.getTypeId();
	protected static int OutBank = AccountType.OutBank.getTypeId();
	@Autowired
	@Lazy
	protected AccountService accSer;
	@Autowired
	protected ErrorHandler errorHandler;
	@Autowired
	protected SysLogService sysLogSer;
	@Autowired
	protected SysLogRepository sysLogDao;
	@Autowired
	protected BankLogService bankLogSer;
	@Autowired
	protected IncomeRequestService inReqSer;
	@Autowired
	protected SystemAccountCommon systemAccountCommon;
	@Autowired
	protected RedisTemplate accountingRedisTemplate;
	@Autowired
	protected StringRedisTemplate accountingStringRedisTemplate;
	@Autowired
	protected StoreHandler storeHandler;

	protected abstract boolean deal(StringRedisTemplate template, AccountBaseInfo base, FailHandler handler,
			EntityNotify param, ReportCheck check);

	protected void push(Integer accId, String v, long occurTime) {
		if (Objects.isNull(accId) || StringUtils.isBlank(v))
			return;
		String k = RedisKeys.genKey4SysBalLogs(accId);
		accountingStringRedisTemplate.boundZSetOps(k).add(v, occurTime);
		accountingStringRedisTemplate.boundZSetOps(k).removeRangeByScore(36000000, occurTime - 1800000);
		accountingStringRedisTemplate.boundZSetOps(k).expire(3, TimeUnit.HOURS);
	}

	protected SysBalTrans findRefundTrans(BizBankLog lg, List<BizBankLog> bankList, List<SysBalTrans> sysList) {
		if (Objects.isNull(lg) || Objects.isNull(lg.getAmount()))
			return null;
		BigDecimal TR_AMT = SysBalUtils.radix2(lg.getAmount()).abs();
		String TO_OWN_2 = SysBalUtils.last2letters(lg.getToAccountOwner());
		String TO_ACC_3 = SysBalUtils.last3letters(lg.getToAccount());
		List<SysBalTrans> reList = sysList.stream()
				.filter(p -> TR_AMT.compareTo(p.getAmt()) == 0 && SysBalUtils.noneExpire(p, Report.EXPIRE_MILLIS_REFUND)
						&& SysBalUtils.valid(p, Report.VALID_MILLIS_CURBAL) && (SysBalUtils.radix(p)
								|| StringUtils.isNotBlank(TO_OWN_2) && Objects.equals(TO_OWN_2, p.getToOwn2Last())
								|| StringUtils.isNotBlank(TO_ACC_3) && Objects.equals(TO_ACC_3, p.getToAcc3Last())))
				.sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o2.getGetTm() - o1.getGetTm()))
				.collect(Collectors.toList());
		if (!CollectionUtils.isEmpty(reList))
			return reList.get(0);
		SysBalTrans ret = null;
		int position = bankList.indexOf(lg);
		if (position < 0)
			return null;
		int stPeriod = position + 1, edPeriod = position + (SysBalUtils.radix(TR_AMT) ? 15 : 2);
		int endIndex = bankList.size() - 1;
		for (int index = stPeriod; index <= edPeriod; index++) {
			if (index > endIndex)
				break;
			BizBankLog bank = bankList.get(index);
			if (Objects.isNull(bank) || Objects.isNull(bank.getAmount()))
				continue;
			if (TR_AMT.add(SysBalUtils.radix2(bank.getAmount())).compareTo(BigDecimal.ZERO) != 0)
				continue;
			ret = findRefundByLastLog(bank, sysList);
			if (Objects.nonNull(ret))
				break;
		}
		return ret;
	}

	private SysBalTrans findRefundByLastLog(BizBankLog lg, List<SysBalTrans> hisList) {
		BigDecimal TR_AMT = SysBalUtils.radix2(lg.getAmount()).abs();
		String TO_ACC_3 = SysBalUtils.last3letters(lg.getToAccount());
		String TO_OWN_2 = SysBalUtils.last2letters(lg.getToAccountOwner());
		List<SysBalTrans> reList = hisList.stream()
				.filter(p -> TR_AMT.compareTo(p.getAmt()) == 0 && (SysBalUtils.radix(p)
						|| StringUtils.isNotBlank(TO_OWN_2) && Objects.equals(TO_OWN_2, p.getToOwn2Last())
						|| StringUtils.isNotBlank(TO_ACC_3) && Objects.equals(TO_ACC_3, p.getToAcc3Last())))
				.sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o2.getAckTm() - o1.getAckTm()))
				.collect(Collectors.toList());
		if (!CollectionUtils.isEmpty(reList))
			return reList.get(0);
		if (Objects.nonNull(lg.getTaskId()) && lg.getTaskId() > 0) {
			reList = hisList.stream()
					.filter(p -> Objects.equals(p.getTaskId(), lg.getTaskId())
							&& Objects.equals(p.getTaskType(), lg.getTaskType()))
					.sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o2.getGetTm() - o1.getGetTm()))
					.collect(Collectors.toList());
			return CollectionUtils.isEmpty(reList) ? null : reList.get(0);
		}
		return null;
	}
}
