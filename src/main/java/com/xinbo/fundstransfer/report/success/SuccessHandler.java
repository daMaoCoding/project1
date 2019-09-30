package com.xinbo.fundstransfer.report.success;

import com.google.common.util.concurrent.Striped;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalTrans;
import com.xinbo.fundstransfer.domain.pojo.TransferEntity;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.SystemAccountCommon;
import com.xinbo.fundstransfer.report.init.InitHandler;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@Component
public class SuccessHandler extends ApplicationObjectSupport {
	private static final Map<String, SuccessCheck> dealMap = new LinkedHashMap<>();

	private static final Striped<Lock> striped = Striped.lazyWeakLock(1024);
	@Autowired
	private SystemAccountCommon systemAccountCommon;
	@Autowired
	private InitHandler initHandler;

	@PostConstruct
	public void init() {
		Map<String, Object> map = super.getApplicationContext().getBeansWithAnnotation(SuccessAnnotation.class);
		map.forEach((k, v) -> dealMap.put(k, (SuccessCheck) v));
	}

	public boolean yunSF(StringRedisTemplate template, AccountBaseInfo base, ReportCheck check) {
		return dealMap.get(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_COMMON_YUNSF)
				.deal(template, base, this, new SuccessParam(null, null, null, null, null, null, null), check);
	}

	/**
	 * 入款卡：会员入款确认
	 */
	public boolean inBankMemberIncome(StringRedisTemplate template, AccountBaseInfo base, BizBankLog lg,
			ReportCheck check) {
		return dealMap.get(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_INBANK_MEMBER_INCOME)
				.deal(template, base, this, new SuccessParam(lg, null, null, null, null, null, null), check);
	}

	/**
	 * 兼职人员提升信用额度 :确认
	 */
	public boolean commonEnhanceCredit(StringRedisTemplate template, AccountBaseInfo base, BizBankLog lg,
			ReportCheck check) {
		return dealMap.get(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_COMMON_ENHANCECREDIT)
				.deal(template, base, this, new SuccessParam(lg, null, null, null, null, null, null), check);
	}

	/**
	 * 转账结果result ==1 ，确认
	 */
	public boolean commonResultEq1(StringRedisTemplate template, AccountBaseInfo base, TransferEntity entity,
			ReportCheck check) {
		return dealMap.get(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_COMMON_RESULTEQ1)
				.deal(template, base, this, new SuccessParam(null, null, entity, null, null, null, null), check);
	}

	/**
	 * 出款/返利 流水确认订单
	 */
	public boolean commonMatchedOutward(StringRedisTemplate template, AccountBaseInfo base, BizBankLog lg,
			BigDecimal fee, ReportCheck check) {
		return dealMap.get(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_COMMON_MATCHEDOUTWARD)
				.deal(template, base, this, new SuccessParam(lg, null, null, null, fee, null, null), check);
	}

	/**
	 * 出款/返利 流水确认数据库订单
	 */
	public boolean commonMatchedOutward_(StringRedisTemplate template, AccountBaseInfo base, BizBankLog lg,
			BigDecimal fee, ReportCheck check) {
		return dealMap.get(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_COMMON_MATCHEDOUTWARD_)
				.deal(template, base, this, new SuccessParam(lg, null, null, null, fee, null, null), check);
	}

	/**
	 * 转入按照余额确认
	 */
	public boolean othersInByBal(StringRedisTemplate template, AccountBaseInfo base, BigDecimal benchmark,
			BigDecimal realBal, ReportCheck check) {
		return dealMap.get(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_OTHERS_INBYBAL)
				.deal(template, base, this, new SuccessParam(null, null, null, realBal, null, benchmark, null), check);
	}

	/**
	 * 入按照流水匹配确认
	 */
	public boolean commonWithdraw(StringRedisTemplate template, AccountBaseInfo base, BizBankLog lg, BigDecimal fee,
			ReportCheck check) {
		return dealMap.get(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_COMMON_WITHDRAW)
				.deal(template, base, this, new SuccessParam(lg, null, null, null, fee, null, null), check);
	}

	/**
	 * 流水确认数据库订单
	 */
	public boolean commonWithdraw_(StringRedisTemplate template, AccountBaseInfo base, BizBankLog lg, BigDecimal fee,
			ReportCheck check) {
		return dealMap.get(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_COMMON_WITHDRAW_)
				.deal(template, base, this, new SuccessParam(lg, null, null, null, fee, null, null), check);
	}

	/**
	 * 结息处理
	 */
	public boolean commonInterest(StringRedisTemplate template, AccountBaseInfo base, BizBankLog lg,
			ReportCheck check) {
		return dealMap.get(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_COMMON_INTEREST)
				.deal(template, base, this, new SuccessParam(lg, null, null, null, null, null, null), check);
	}

	/**
	 * 入款卡 余额上报转出确认.
	 */
	public boolean inbankOutByBal(StringRedisTemplate template, AccountBaseInfo base, BigDecimal realBal,
			BizAccount acc, ReportCheck check) {
		return dealMap.get(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_INBANK_OUT_BY_BAL)
				.deal(template, base, this, new SuccessParam(null, null, null, realBal, null, null, acc), check);
	}

	/**
	 * 入款卡 余额上报转出确认.
	 */
	public boolean inbankOutByEntity(StringRedisTemplate template, AccountBaseInfo base, TransferEntity entity,
			ReportCheck check) {
		return dealMap.get(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_INBANK_OUT_BY_ENTITY)
				.deal(template, base, this, new SuccessParam(null, null, entity, null, null, null, null), check);
	}

	/**
	 * 第三方下发确认（依靠收款账号流水确认Redis中的转账记录 ）</br>
	 * 收款账号：下发卡；出款卡
	 */
	public boolean othersMatched3Th(StringRedisTemplate template, AccountBaseInfo base, BizBankLog lg,
			ReportCheck check) {
		return dealMap.get(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_OTHERS_MATCHED_3TH)
				.deal(template, base, this, new SuccessParam(lg, null, null, null, null, null, null), check);
	}

	/**
	 * 其他卡转入确认
	 */
	public boolean othersDeposit(StringRedisTemplate template, AccountBaseInfo base, BizBankLog lg, ReportCheck check) {
		return dealMap.get(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_OTHERS_DEPOSIT)
				.deal(template, base, this, new SuccessParam(lg, null, null, null, null, null, null), check);
	}

	/**
	 * 依靠收款账号（下发卡；出款卡）流水匹配数据库下发订单</br>
	 * 1.内部下发。2.第三方下发
	 */
	public boolean othersMatchedDepositInDB(StringRedisTemplate template, AccountBaseInfo base, BizBankLog lg,
			ReportCheck check) {
		return dealMap
				.get(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_OTHERS_MATCHED_DEPOSIT_IN_DB)
				.deal(template, base, this, new SuccessParam(lg, null, null, null, null, null, null), check);
	}

	/**
	 * 其他卡转出 余额上报确认
	 */
	public boolean othersOutByBal(StringRedisTemplate template, AccountBaseInfo base, BigDecimal benchmark,
			BigDecimal realBal, ReportCheck check) {
		return dealMap.get(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_OTHERS_OUTBYBAL)
				.deal(template, base, this, new SuccessParam(null, null, null, realBal, null, benchmark, null), check);
	}

	/**
	 * 其他卡转入转出 被余额确认
	 */
	public boolean othersInOutByBal(StringRedisTemplate template, AccountBaseInfo base, BigDecimal benchmark,
			BigDecimal realBal, ReportCheck check) {
		return dealMap.get(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_OTHERS_INOUTBYBAL)
				.deal(template, base, this, new SuccessParam(null, null, null, realBal, null, benchmark, null), check);
	}

	/**
	 * 转入按照余额确认(人工)
	 */
	public boolean othersInByBal_(StringRedisTemplate template, AccountBaseInfo base, BigDecimal benchmark,
			BigDecimal realBal, ReportCheck check) {
		return dealMap.get(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_OTHERS_INBYBAL_)
				.deal(template, base, this, new SuccessParam(null, null, null, realBal, null, benchmark, null), check);
	}

	/**
	 * 转出按照余额确认(人工)
	 */
	public boolean othersOutByBal_(StringRedisTemplate template, AccountBaseInfo base, BigDecimal benchmark,
			BigDecimal realBal, ReportCheck check) {
		return dealMap.get(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_OTHERS_OUTBYBAL_)
				.deal(template, base, this, new SuccessParam(null, null, null, realBal, null, benchmark, null), check);
	}

	/**
	 * 实体上报 EQ 确认
	 */
	public boolean othersEqByEntity(StringRedisTemplate template, AccountBaseInfo base, BigDecimal benchmark,
			TransferEntity entity, ReportCheck check) {
		return dealMap.get(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_OTHERS_EQBYENTITY)
				.deal(template, base, this, new SuccessParam(null, null, entity, null, null, benchmark, null), check);
	}

	/**
	 * 实体上报 EQ AND IN 确认
	 */
	public boolean othersEqAndInByEntity(StringRedisTemplate template, AccountBaseInfo base, BigDecimal benchmark,
			TransferEntity entity, ReportCheck check) {
		return dealMap.get(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_OTHERS_EQANDINBYENTITY)
				.deal(template, base, this, new SuccessParam(null, null, entity, null, null, benchmark, null), check);
	}

	public boolean commonMatchedWithdrawInDBWithoutOrder(StringRedisTemplate template, AccountBaseInfo base,
			BizBankLog lg, BigDecimal fee, ReportCheck check) {
		return dealMap
				.get(SuccessCheck.PREFIX_SUCCESS_CHECK
						+ SuccessCheck.SUCCESS_CHECK_TYPE_COMMON_MATCHED_WITHDRAW_INDB_WITHOUT_ORDER)
				.deal(template, base, this, new SuccessParam(lg, null, null, null, fee, null, null), check);
	}

	public boolean othersMatchedDepositInDBWithoutOrder(StringRedisTemplate template, AccountBaseInfo base,
			BizBankLog lg, ReportCheck check) {
		return dealMap
				.get(SuccessCheck.PREFIX_SUCCESS_CHECK
						+ SuccessCheck.SUCCESS_CHECK_TYPE_OTHERS_MATCHED_DEPOSIT_INDB_WITHOUT_ORDER)
				.deal(template, base, this, new SuccessParam(lg, null, null, null, null, null, null), check);
	}

	public boolean othersManualOutward(StringRedisTemplate template, AccountBaseInfo base, BizBankLog lg,
			BigDecimal fee, ReportCheck check) {
		return dealMap.get(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_OTHERS_MANUAL_OUTWARD)
				.deal(template, base, this, new SuccessParam(lg, null, null, null, fee, null, null), check);
	}

	/**
	 * 入款卡-云闪付：APP漏抓流水,程序自动填充一笔入款系统账目，当流水抓上来时，数据处理
	 */
	public boolean yunSFAbsentIncomeByBankLog(StringRedisTemplate template, AccountBaseInfo base, BizBankLog lg,
			ReportCheck check) {
		return dealMap
				.get(SuccessCheck.PREFIX_SUCCESS_CHECK
						+ SuccessCheck.SUCCESS_CHECK_TYPE_INBANK_YunSFAbsentIncomeByBankLog)
				.deal(template, base, this, new SuccessParam(lg, null, null, null, null, null, null), check);
	}

	public boolean yunSFInOutByEntity(StringRedisTemplate template, AccountBaseInfo base, TransferEntity entity,
			ReportCheck check) {
		return dealMap
				.get(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_INBANK_YunSFInOutByEntity)
				.deal(template, base, this, new SuccessParam(null, null, entity, null, null, null, null), check);
	}

	/**
	 * 入款卡-云闪付：根据工具端上报的银行余额确认会员入款记录
	 */
	public boolean yunSFIncomeByBalance(StringRedisTemplate template, AccountBaseInfo base, BigDecimal realBal,
			ReportCheck check) {
		return dealMap
				.get(SuccessCheck.PREFIX_SUCCESS_CHECK
						+ SuccessCheck.SUCCESS_CHECK_TYPE_INBANK_YunSFIncomeByBalanceSuccess)
				.deal(template, base, this, new SuccessParam(null, null, null, realBal, null, null, null), check);
	}

	public String reWriteMsg(StringRedisTemplate template, SysBalTrans ts, long expire, TimeUnit unit) {
		Lock lock = striped.get(SysBalUtils.uuid(ts));
		try {
			lock.lock();
			template.delete(ts.getMsg());
			String k = SysBalTrans.genMsg(ts);
			template.boundValueOps(k).set(StringUtils.EMPTY, expire, unit);
			ts.setMsg(k);
			return k;
		} finally {
			lock.unlock();
		}
	}

	public String deStruct(StringRedisTemplate template, SysBalTrans ts, BigDecimal[] bals, int ack) {
		String msg = null;
		if (SuccessCheck.ACK_FR == ack) {
			// 更新转账任务信息
			if (ts.getToId() == 0 && ts.getBankLgId() == 0
					|| ts.getToId() != 0 && (ts.getBankLgId() == 0 || ts.getOppBankLgId() == 0)) {
				msg = reWriteMsg(template, ts, SysBalUtils.expireAck(ts.getGetTm()), TimeUnit.MINUTES);
			} else {
				msg = reWriteMsg(template, ts, SysBalUtils.expireEnd(), TimeUnit.MINUTES);
			}
			// 人工处理的任务，不需要通过程序后继操作
			if (SysBalTrans.REGIST_WAY_MAN != ts.getRegistWay() && SysBalTrans.REGIST_WAY_MAN_MGR != ts.getRegistWay())
				systemAccountCommon.confirm(ts);
			// 清理转入|转出记录
			String kOut = (String) template.boundHashOps(RedisKeys.SYS_BAL_OUT).get(String.valueOf(ts.getFrId()));
			if (StringUtils.isNotEmpty(kOut) && SysBalUtils.equal(new SysBalTrans(kOut), ts)) {
				template.boundHashOps(RedisKeys.SYS_BAL_OUT).delete(String.valueOf(ts.getFrId()));
			}
			if (ts.getToId() != 0 && !ts.ackTo()) {
				String kIn = (String) template.boundHashOps(RedisKeys.SYS_BAL_IN).get(String.valueOf(ts.getToId()));
				if (StringUtils.isNotEmpty(kIn) && SysBalUtils.equal(new SysBalTrans(kIn), ts)) {
					template.boundHashOps(RedisKeys.SYS_BAL_IN).put(String.valueOf(ts.getToId()), msg);
				}
			}
		} else if (SuccessCheck.ACK_TO == ack) {
			// 更新转账任务信息
			if (ts.getFrId() == 0 && ts.getOppBankLgId() == 0
					|| ts.getFrId() != 0 && (ts.getBankLgId() == 0 || ts.getOppBankLgId() == 0)) {
				msg = reWriteMsg(template, ts, SysBalUtils.expireAck(ts.getGetTm()), TimeUnit.MINUTES);
			} else {
				msg = reWriteMsg(template, ts, SysBalUtils.expireEnd(), TimeUnit.MINUTES);
			}
			// 清理转入|转出记录
			String kIn = (String) template.boundHashOps(RedisKeys.SYS_BAL_IN).get(String.valueOf(ts.getToId()));
			if (StringUtils.isNotEmpty(kIn) && SysBalUtils.equal(new SysBalTrans(kIn), ts)) {
				template.boundHashOps(RedisKeys.SYS_BAL_IN).delete(String.valueOf(ts.getToId()));
			}
			if (ts.getFrId() != 0 && !ts.ackFr()) {
				String kOut = (String) template.boundHashOps(RedisKeys.SYS_BAL_OUT).get(String.valueOf(ts.getFrId()));
				if (StringUtils.isNotEmpty(kOut) && SysBalUtils.equal(new SysBalTrans(kOut), ts)) {
					template.boundHashOps(RedisKeys.SYS_BAL_OUT).put(String.valueOf(ts.getFrId()), msg);
				}
			}
		}
		{
			// 系统余额动态初始化
			if (Objects.nonNull(bals) && bals.length == 2) {
				if (SuccessCheck.ACK_FR == ack && ts.getFrId() != 0)
					initHandler.dynamicInitIfNeed(ts.getFrId(), bals[1], bals[0]);
				if (SuccessCheck.ACK_TO == ack && ts.getToId() != 0)
					initHandler.dynamicInitIfNeed(ts.getToId(), bals[1], bals[0]);
			}

		}
		return msg;
	}
}
