package com.xinbo.fundstransfer.report.fail;

import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalTrans;
import com.xinbo.fundstransfer.domain.pojo.TransferEntity;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.acc.ErrorHandler;
import com.xinbo.fundstransfer.report.acc.SysAccPush;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import com.xinbo.fundstransfer.service.AccountService;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.util.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 该模块 {@link com.xinbo.fundstransfer.report.fail}
 * 为确认转账任务失败模块,原理：通过工具端多次上报银行流水，与银行余额，确认一笔转账任务失败。
 * <p>
 * 以下为余额确认转账失败原理：</br>
 * 场景一</br>
 * 假设存在账号A，转账前余额 8000.00， 转账 2000.00</br>
 * 1.2019-04-22 08:57:20 工具(账号A)从服务端取走转账任务 2000.00 </br>
 * 2.2019-04-22 08:59:10 工具(账号A)完成转账任务并上报 转账前 8000.00，转账后 00.00</br>
 * 3.2019-04-22 09:00:10 工具(账号A) 余额上报 8000.00</br>
 * 4.2019-04-22 09:01:11 工具(账号A) 余额上报 8000.00</br>
 * 5.2019-04-22 09:02:01 工具(账号A) 余额上报 8000.00</br>
 * 如果出现此现象，上报余额始终等于转账前余额 则认为 工具(账号A)转账失败</br>
 * 场景二</br>
 * 假设存在账号A, 转账前系统余额 8000.00， 银行余额 8000.00 转账任务 2000.00</br>
 * 1.2019-04-22 08:57:20 工具(账号A)从服务端取走转账任务 2000.00 </br>
 * 2.2019-04-22 08:59:10 工具(账号A)完成转账任务并上报 转账前 8000.00，转账后 00.00</br>
 * 3.2019-04-22 09:00:10 工具(账号A) 余额上报 8000.00</br>
 * 4.2019-04-22 09:01:11 工具(账号A) 余额上报 8000.00</br>
 * 5.2019-04-22 09:02:01 工具(账号A) 余额上报 8000.00</br>
 * 如果出现此场景, 上报余额时，系统余额始终等于银行余额，则任务 工具(账号A)转账失败</br>
 * <p>
 * 以下为流水确认转账失败原理:（暂时不上线，此部分）</br>
 */
@Component
public class FailHandler extends ApplicationObjectSupport {
	private static final Map<String, FailCheck> dealMap = new LinkedHashMap<>();
	@Autowired
	@Lazy
	private AccountService accSer;
	@Autowired
	private ErrorHandler errorHandler;
	@Autowired
	protected RedisTemplate accountingRedisTemplate;
	@Autowired
	protected StringRedisTemplate accountingStringRedisTemplate;

	@PostConstruct
	public void init() {
		Map<String, Object> map = super.getApplicationContext().getBeansWithAnnotation(FailAnnotation.class);
		map.forEach((k, v) -> dealMap.put(k, (FailCheck) v));
	}

	/**
	 * 确认转账失败时，调用此接口
	 */
	public void fail(StringRedisTemplate template, SysBalTrans ts, BizBankLog bankLog, String remark) {
		String kOut = (String) template.boundHashOps(RedisKeys.SYS_BAL_OUT).get(String.valueOf(ts.getFrId()));
		if (StringUtils.isNotEmpty(kOut) && SysBalUtils.equal(new SysBalTrans(kOut), ts)) {
			template.boundHashOps(RedisKeys.SYS_BAL_OUT).delete(String.valueOf(ts.getFrId()));
		}
		String kIn = (String) template.boundHashOps(RedisKeys.SYS_BAL_IN).get(String.valueOf(ts.getToId()));
		if (StringUtils.isNotEmpty(kIn) && SysBalUtils.equal(new SysBalTrans(kIn), ts)) {
			template.boundHashOps(RedisKeys.SYS_BAL_IN).delete(String.valueOf(ts.getToId()));
		}
		template.delete(ts.getMsg());
		if (SysBalTrans.REGIST_WAY_MAN == ts.getRegistWay())
			template.boundValueOps(SysBalTrans.genMsg(ts)).set(StringUtils.EMPTY, 30, TimeUnit.SECONDS);
		ts.setFrBankLog(Objects.nonNull(bankLog) ? bankLog : null);
		errorHandler.handle(new SysAccPush(ts.getFrId(), SysAccPush.ActionInvalidTransfer, ts, remark),null);
	}

	/**
	 * 记录原始数据：系统余额初始化
	 */
	public void record(Integer accId, BigDecimal bankBal) {
		if (Objects.isNull(accId) || Objects.isNull(bankBal))
			return;
		AccountBaseInfo base = accSer.getFromCacheById(accId);
		if (Objects.isNull(base))
			return;
		EntityNotify param = new EntityNotify(Common.WATCHER_4_INIT_BAL, base.getId(), base, null, null, null, null,
				bankBal);
		dealMap.get(FailCheck.PREFIX_FAIL_CHECK + Common.WATCHER_4_INIT_BAL).deal(accountingStringRedisTemplate, base,
				this, param, null);
	}

	/**
	 * 记录原始数据：银行余额上报
	 */
	public void record(AccountBaseInfo base, BigDecimal bankBal, BigDecimal sysBal, long occurTime, ReportCheck check) {
		if (Objects.isNull(base) || Objects.isNull(base.getId()) || Objects.isNull(bankBal) || Objects.isNull(sysBal)
				|| Objects.isNull(check))
			return;
		occurTime = occurTime == 0 ? System.currentTimeMillis() : occurTime;
		EntityNotify param = new EntityNotify(Common.WATCHER_4_BANK_BALANCE, base.getId(), base, sysBal, bankBal,
				occurTime, check);
		dealMap.get(FailCheck.PREFIX_FAIL_CHECK + Common.WATCHER_4_BANK_BALANCE).deal(accountingStringRedisTemplate,
				base, this, param, null);
	}

	/**
	 * 失败订单确认（后一笔转账成功，确认前一笔是否成功）
	 */
	public void record(AccountBaseInfo base, ReportCheck check) {
		if (Objects.isNull(base) || Objects.isNull(check) || CollectionUtils.isEmpty(check.getTrans()))
			return;
		EntityNotify param = new EntityNotify(Common.WATCHER_4_CHECK_SUCCESS, base.getId(), base, null, check, null,
				null, null);
		dealMap.get(FailCheck.PREFIX_FAIL_CHECK + Common.WATCHER_4_CHECK_SUCCESS).deal(accountingStringRedisTemplate,
				base, this, param, check);
	}

	/**
	 * 根据工具端转账实体确认是否转账失败
	 * <p>
	 * 1.result ==3</br>
	 * 2.失败特殊关键字： 等待 & 无结果转出
	 * </p>
	 * 
	 * @return {@code true} 该转账任务失败 ，{@code false} 该转账判定结果待定
	 */
	public boolean invalid(AccountBaseInfo base, TransferEntity entity, ReportCheck check) {
		if (Objects.isNull(base) || Objects.isNull(entity) || Objects.isNull(check))
			return true;
		// 转账实体 result ==3 处理
		EntityNotify param = new EntityNotify(Common.WATCHER_4_RESULT_EQ_3, base.getId(), base, entity, check, null,
				null, null);
		boolean ret = dealMap.get(FailCheck.PREFIX_FAIL_CHECK + Common.WATCHER_4_RESULT_EQ_3)
				.deal(accountingStringRedisTemplate, base, this, param, check);
		if (ret)
			return true;
		// 转账实体 备注中含有特殊关键字处理
		param = new EntityNotify(Common.WATCHER_4_KEY_WORDS, base.getId(), base, entity, check, null, null, null);
		return dealMap.get(FailCheck.PREFIX_FAIL_CHECK + Common.WATCHER_4_KEY_WORDS).deal(accountingStringRedisTemplate,
				base, this, param, check);
	}

	public boolean refund(AccountBaseInfo base, BizBankLog lg, BizBankLog lastBankLog, ReportCheck check) {
		if (Objects.isNull(base))
			return false;
		EntityNotify param = new EntityNotify(Common.WATCHER_4_KEY_REFUND_0, base.getId(), base, null, check, lg,
				lastBankLog, null);
		return dealMap.get(FailCheck.PREFIX_FAIL_CHECK + Common.WATCHER_4_KEY_REFUND_0)
				.deal(accountingStringRedisTemplate, base, this, param, check);
	}

	public boolean refund1(AccountBaseInfo base, BizBankLog lg, ReportCheck check) {
		if (Objects.isNull(base))
			return false;
		EntityNotify param = new EntityNotify(Common.WATCHER_4_KEY_REFUND_1, base.getId(), base, null, check, lg, null,
				null);
		return dealMap.get(FailCheck.PREFIX_FAIL_CHECK + Common.WATCHER_4_KEY_REFUND_1)
				.deal(accountingStringRedisTemplate, base, this, param, check);
	}

	public boolean refund2(AccountBaseInfo base, BizBankLog lg, BizBankLog lastBankLog, ReportCheck check) {
		if (Objects.isNull(base))
			return false;
		EntityNotify param = new EntityNotify(Common.WATCHER_4_KEY_REFUND_2, base.getId(), base, null, check, lg,
				lastBankLog, null);
		return dealMap.get(FailCheck.PREFIX_FAIL_CHECK + Common.WATCHER_4_KEY_REFUND_2)
				.deal(accountingStringRedisTemplate, base, this, param, check);
	}

	public boolean refundMatcheInMemery(AccountBaseInfo base, BizBankLog lg, ReportCheck check) {
		if (Objects.isNull(base))
			return false;
		EntityNotify param = new EntityNotify(Common.WATCHER_4_KEY_REFUND_MATCHED_IN_MEMERY, base.getId(), base, null,
				check, lg, null, null);
		return dealMap.get(FailCheck.PREFIX_FAIL_CHECK + Common.WATCHER_4_KEY_REFUND_MATCHED_IN_MEMERY)
				.deal(accountingStringRedisTemplate, base, this, param, check);
	}

	public boolean inBankIncomeTest(AccountBaseInfo base, BizBankLog lg, ReportCheck check) {
		if (Objects.isNull(base))
			return false;
		EntityNotify param = new EntityNotify(Common.WATCHER_4_KEY_IN_BANK_INCOME_TEST, base.getId(), base, null, check,
				lg, null, null);
		return dealMap.get(FailCheck.PREFIX_FAIL_CHECK + Common.WATCHER_4_KEY_IN_BANK_INCOME_TEST)
				.deal(accountingStringRedisTemplate, base, this, param, check);
	}

	public boolean incomeDuplicateMatch(AccountBaseInfo base, BizBankLog lg, ReportCheck check) {
		if (Objects.isNull(base))
			return false;
		EntityNotify param = new EntityNotify(Common.WATCHER_4_KEY_IN_BANK_INCOME_DUPLICATE_MATCHED, base.getId(), base,
				null, check, lg, null, null);
		return dealMap.get(FailCheck.PREFIX_FAIL_CHECK + Common.WATCHER_4_KEY_IN_BANK_INCOME_DUPLICATE_MATCHED)
				.deal(accountingStringRedisTemplate, base, this, param, check);
	}
}
