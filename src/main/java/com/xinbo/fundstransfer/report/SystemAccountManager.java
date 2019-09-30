package com.xinbo.fundstransfer.report;

import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.SysErrStatus;
import com.xinbo.fundstransfer.domain.pojo.AccInvstDoing;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalPush;
import com.xinbo.fundstransfer.domain.pojo.TransferEntity;
import com.xinbo.fundstransfer.report.acc.ErrorAlarm;
import com.xinbo.fundstransfer.report.acc.ErrorHandler;
import com.xinbo.fundstransfer.report.init.InitHandler;
import com.xinbo.fundstransfer.report.up.ReportInitParam;
import com.xinbo.fundstransfer.report.up.ReportInvstError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.*;

/**
 * 系统账目公共接口调用
 */
@Service
public class SystemAccountManager {
	protected static final Logger logger = LoggerFactory.getLogger(SystemAccountManager.class);
	@Lazy
	@Autowired
	private SystemAccountCommon common;
	@Autowired
	@Lazy
	private InitHandler initHandler;
	@Autowired
	private RedisTemplate accountingRedisTemplate;
	@Autowired
	private StringRedisTemplate accountingStringRedisTemplate;
	@Lazy
	@Autowired
	private ErrorHandler errorHandler;
	@Autowired
	private SystemAccountOutterAdapter outterAdapter;

	public boolean checkRight4Accounting() {
		return common.checkHostRunRight();
	}

	/**
	 * 当工具取走转账任务时，进行注册
	 *
	 * @param transferEntity
	 *            转账实体
	 * @param before
	 *            汇款账号转账前余额
	 */
	public void regist(TransferEntity transferEntity, BigDecimal before) {
		if (SystemAccountConfiguration.mainOperatingSwitch())
			outterAdapter.regist(accountingStringRedisTemplate, transferEntity, before);
	}

	/**
	 * 第三方提现：注册
	 *
	 * @param acc3th
	 *            第三方账号
	 * @param order
	 *            第三方提现订单信息
	 */
	public void registTh3(BizAccount acc3th, BizIncomeRequest order) {
		if (SystemAccountConfiguration.mainOperatingSwitch())
			outterAdapter.registTh3(accountingStringRedisTemplate, acc3th, order);
	}

	/**
	 * 人工出款：注册
	 *
	 * @param task
	 *            出款任务
	 */
	public void registMan(BizOutwardTask task) {
		if (SystemAccountConfiguration.mainOperatingSwitch())
			outterAdapter.registMan(accountingStringRedisTemplate, task);
	}

	@Transactional
	public void cancel(BizIncomeRequest incomeRequest) {
		if (SystemAccountConfiguration.mainOperatingSwitch())
			common.cancel(accountingStringRedisTemplate, incomeRequest);
	}

	/**
	 * 系统账目悬浮部分
	 */
	public List<BizSysLog> suspend(AccountBaseInfo base) {
		return outterAdapter.suspend(accountingStringRedisTemplate, base);
	}

	/**
	 * 工具端信息上报</br>
	 * 转账实体，银行余额，银行流水,银行流水抓取时间
	 *
	 * @param entity
	 *            工具端信息封装
	 */
	public void rpush(SysBalPush entity) {
		if (!SystemAccountConfiguration.mainOperatingSwitch())
			return;
		if (entity == null || entity.getTarget() == 0)
			return;
		if (SysBalPush.CLASSIFY_BANK_LOGS_TIME == entity.getClassify()) {
			common.crawlTime4BankStatement(accountingStringRedisTemplate, entity.getTarget(),
					System.currentTimeMillis());
			return;
		}
		if (SysBalPush.CLASSIFY_BANK_LOGS == entity.getClassify())
			common.crawlTime4BankStatement(accountingStringRedisTemplate, entity.getTarget(),
					System.currentTimeMillis());
		outterAdapter.rpush(accountingRedisTemplate, entity.getClassify(), entity.getTarget(),
				ObjectMapperUtils.serialize(entity));
	}

	/**
	 * 账号初始化 </br>
	 * 把银行余额赋值给系统余额
	 */
	@Transactional
	public void initByErrorId(Long errorId, SysUser operator, String remark) {
		initHandler.initByErr(errorId, operator, remark);
	}

	/**
	 * 根据账号ID初始化
	 */
	@Transactional
	public void initByAccountId(Integer accId, SysUser operator) throws Exception {
		initHandler.initByAcc(accId, operator);
	}

	@Transactional
	public void invstRemark(Long errId, SysUser operator, String remark) {
		Integer accId = common.invstRemark(accountingStringRedisTemplate, errId, operator, remark);
		if (Objects.nonNull(accId) && accId != 0)
			rpush(new SysBalPush(accId, SysBalPush.CLASSIFY_INIT,
					new ReportInitParam(accId, operator.getId(), remark)));

	}

	@Transactional
	public void invstError(Long errId, List<AccInvstDoing> doingList, SysUser operator, String remark, SysErrStatus st)
			throws Exception {
		ReportInvstError ret = common.invstError(accountingStringRedisTemplate, errorHandler, errId, doingList,
				operator, remark, st);
		if (Objects.nonNull(ret))
			this.rpush(new SysBalPush(ret.getAccId(), SysBalPush.CLASSIFY_INVST_ERROR, ret));
	}

	@Transactional
	public void transErrorToOther(Long errorId, SysUser operator, String otherUser, String remark) throws Exception {
		outterAdapter.transErrorToOther(errorId, operator, otherUser, remark);
	}

	public Set<Integer> accountingException() {
		Set<Integer> result = new HashSet<>();
		if (!SystemAccountConfiguration.mainOperatingSwitch()
				|| !SystemAccountConfiguration.needOpenService4AccountingException()
				|| !SystemAccountConfiguration.needOpenService4ManualSurveyIfAccountException())
			return result;
		Set<Integer> inclusiveHandicaps = SystemAccountConfiguration.hadicapSetByOpenService4AccountingException();
		return outterAdapter.accountingException(accountingStringRedisTemplate, inclusiveHandicaps);
	}

	public Set<Integer> accountingSuspend() {
		Set<Integer> result = new HashSet<>();
		if (!SystemAccountConfiguration.mainOperatingSwitch()
				|| !SystemAccountConfiguration.needOpenService4AccountingException()
				|| !SystemAccountConfiguration.needOpenService4ManualSurveyIfAccountException())
			return result;
		return outterAdapter.accountingSuspend(accountingStringRedisTemplate);
	}

	public Set<Integer> accountingExceedInCountLimit() {
		Set<Integer> result = new HashSet<>();
		if (!SystemAccountConfiguration.mainOperatingSwitch()
				|| !SystemAccountConfiguration.needOpenService4AccountingException()
				|| !SystemAccountConfiguration.needOpenService4ManualSurveyIfAccountException())
			return result;
		return outterAdapter.accountingExceedInCountLimit(accountingStringRedisTemplate);
	}

	public boolean check4AccountingIn(Integer accountId) {
		return true;
	}

	public boolean check4AccountingOut(Integer accountId) {
		return true;
	}

	public Set<Integer> alarm4AccountingInOut(boolean isOutward) {
		return Collections.EMPTY_SET;
	}

}
