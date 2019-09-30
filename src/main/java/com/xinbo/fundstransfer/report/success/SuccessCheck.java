package com.xinbo.fundstransfer.report.success;

import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.enums.BankLogStatus;
import com.xinbo.fundstransfer.domain.enums.InBankSubType;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.repository.IncomeRequestRepository;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.store.StoreHandler;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import com.xinbo.fundstransfer.report.up.ReportYSF;
import com.xinbo.fundstransfer.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

public abstract class SuccessCheck {

	/* 处理上报 注解前缀 */
	public static final String PREFIX_SUCCESS_CHECK = "SUCCESS_CHECK_";

	/**
	 * 云闪付对账处理
	 */
	public static final String SUCCESS_CHECK_TYPE_COMMON_YUNSF = "COMMOIN_YUNSF";

	/**
	 * 入款卡：会员入款
	 */
	public static final String SUCCESS_CHECK_TYPE_INBANK_MEMBER_INCOME = "INBANK_MEMBER_INCOME";

	public static final String SUCCESS_CHECK_TYPE_COMMON_ENHANCECREDIT = "COMMON_ENHANCECREDIT";

	public static final String SUCCESS_CHECK_TYPE_COMMON_RESULTEQ1 = "COMMON_RESULTEQ1";

	public static final String SUCCESS_CHECK_TYPE_COMMON_MATCHEDOUTWARD = "COMMON_MATCHEDOUTWARD";

	public static final String SUCCESS_CHECK_TYPE_COMMON_MATCHEDOUTWARD_ = "COMMON_MATCHEDOUTWARD_";

	public static final String SUCCESS_CHECK_TYPE_OTHERS_INBYBAL = "OTHERS_INBYBAL";

	public static final String SUCCESS_CHECK_TYPE_COMMON_WITHDRAW = "COMMON_WITHDRAW";

	public static final String SUCCESS_CHECK_TYPE_COMMON_WITHDRAW_ = "COMMON_WITHDRAW_";

	public static final String SUCCESS_CHECK_TYPE_COMMON_INTEREST = "COMMON_INTEREST";

	public static final String SUCCESS_CHECK_TYPE_INBANK_OUT_BY_BAL = "INBANK_OUT_BY_BAL";

	public static final String SUCCESS_CHECK_TYPE_INBANK_OUT_BY_ENTITY = "INBANK_OUT_BY_ENTITY";

	public static final String SUCCESS_CHECK_TYPE_OTHERS_MATCHED_3TH = "OTHERS_MATCHED_3TH";

	public static final String SUCCESS_CHECK_TYPE_OTHERS_DEPOSIT = "OTHERS_DEPOSIT";

	public static final String SUCCESS_CHECK_TYPE_OTHERS_MATCHED_DEPOSIT_IN_DB = "MATCHED_DEPOSIT_IN_DB";

	public static final String SUCCESS_CHECK_TYPE_OTHERS_OUTBYBAL = "OTHERS_OUTBYBAL";

	public static final String SUCCESS_CHECK_TYPE_OTHERS_INOUTBYBAL = "OTHERS_INOUTBYBAL";

	public static final String SUCCESS_CHECK_TYPE_OTHERS_INBYBAL_ = "OTHERS_INBYBAL_";

	public static final String SUCCESS_CHECK_TYPE_OTHERS_OUTBYBAL_ = "OTHERS_OUTBYBAL_";

	public static final String SUCCESS_CHECK_TYPE_OTHERS_EQBYENTITY = "OTHERS_EQBYENTITY";

	public static final String SUCCESS_CHECK_TYPE_OTHERS_EQANDINBYENTITY = "OTHERS_EQANDINBYENTITY";

	public static final String SUCCESS_CHECK_TYPE_COMMON_MATCHED_WITHDRAW_INDB_WITHOUT_ORDER = "COMMON_MATCHED_WITHDRAW_INDB_WITHOUT_ORDER";

	public static final String SUCCESS_CHECK_TYPE_OTHERS_MATCHED_DEPOSIT_INDB_WITHOUT_ORDER = "OTHERS_MATCHED_DEPOSIT_INDB_WITHOUT_ORDER";

	public static final String SUCCESS_CHECK_TYPE_OTHERS_MANUAL_OUTWARD = "OTHERS_MANUAL_OUTWARD";
	/**
	 * 入款卡-云闪付：APP漏抓流水,程序自动填充一笔入款系统账目，当流水抓上来时，数据处理
	 */
	public static final String SUCCESS_CHECK_TYPE_INBANK_YunSFAbsentIncomeByBankLog = "YunSFAbsentIncomeByBankLog";

	public static final String SUCCESS_CHECK_TYPE_INBANK_YunSFInOutByEntity = "YunSFInOutByEntity";
	/**
	 * 入款卡-云闪付：根据工具端上报的银行余额确认会员入款记录
	 */
	public static final String SUCCESS_CHECK_TYPE_INBANK_YunSFIncomeByBalanceSuccess = "YunSFIncomeByBalanceSuccess";

	public static final int ACK_FR = 1;
	public static final int ACK_TO = 2;

	protected static final Logger logger = LoggerFactory.getLogger(SuccessCheck.class);

	protected static int Refunding = BankLogStatus.Refunding.getStatus();
	protected static int Refunded = BankLogStatus.Refunded.getStatus();
	protected static int Interest = BankLogStatus.Interest.getStatus();
	protected static int Fee = BankLogStatus.Fee.getStatus();

	protected static int InBank = AccountType.InBank.getTypeId();
	protected static int BindWechat = AccountType.BindWechat.getTypeId();
	protected static int BindAli = AccountType.BindAli.getTypeId();
	protected static int ThirdCommon = AccountType.ThirdCommon.getTypeId();
	protected static int BindCommon = AccountType.BindCommon.getTypeId();
	protected static int OutBank = AccountType.OutBank.getTypeId();
	protected static int InThird = AccountType.InThird.getTypeId();
	protected static int OutThird = AccountType.OutThird.getTypeId();

	protected static int IN_BANK_YSF = InBankSubType.IN_BANK_YSF.getSubType();
	protected static int IN_BANK_YSF_MIX = InBankSubType.IN_BANK_YSF_MIX.getSubType();

	@Autowired
	protected StoreHandler storeHandler;
	@Autowired
	protected IncomeRequestRepository incomeDao;
	@Autowired
	@Lazy
	protected AccountService accSer;

	protected abstract boolean deal(StringRedisTemplate template, AccountBaseInfo base, SuccessHandler handler,
			SuccessParam param, ReportCheck check);

	protected BizBankLog bankLog(ReportYSF ysf) {
		BizBankLog result = new BizBankLog();
		result.setId(ysf.getFlogId());
		result.setCreateTime(new Date(ysf.getCrawlTm()));
		result.setTradingTime(new Date(ysf.getCrawlTm()));
		result.setFromAccount(ysf.getFromAccount());
		result.setTaskId(ysf.getTaskId());
		result.setTaskType(ysf.getTaskType());
		result.setOrderNo(ysf.getOrderNo());
		result.setAmount(ysf.getAmount());
		result.setSummary(ysf.getSummary());
		result.setToAccountOwner(ysf.getOppOwner());
		result.setToAccount(ysf.getOppAccount());
		return result;
	}
}
