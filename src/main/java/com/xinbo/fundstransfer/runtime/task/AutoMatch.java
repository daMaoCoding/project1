package com.xinbo.fundstransfer.runtime.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.component.spring.SpringContextUtils;
import com.xinbo.fundstransfer.domain.DynamicSpecifications;
import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.SearchFilter.Operator;
import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.entity.BizIncomeRequest;
import com.xinbo.fundstransfer.domain.entity.BizOutwardTask;
import com.xinbo.fundstransfer.domain.entity.BizTransactionLog;
import com.xinbo.fundstransfer.domain.enums.*;
import com.xinbo.fundstransfer.domain.pojo.IncomeAuditWs;
import com.xinbo.fundstransfer.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 已弃用，老代码, 入款匹配，当有入款请求或者银行流水抓取入库时启用此线程匹配
 * 
 *
 *
 */
@Deprecated
public class AutoMatch implements Runnable {
	Logger log = LoggerFactory.getLogger(this.getClass());
	private BizIncomeRequest incomeRequest;
	private BizBankLog bankLog;
	private IncomeRequestService incomeRequestService;
	private OutwardTaskService outwardTaskService;
	private BankLogService bankLogService;
	private TransactionLogService transactionLogService;
	private RedisService redisService;
	private BizOutwardTask outwardTask;
	private AccountService accountService;

	public AutoMatch(BizBankLog bankLog, BizIncomeRequest incomeRequest) {
		this.bankLog = bankLog;
		this.incomeRequest = incomeRequest;
		incomeRequestService = SpringContextUtils.getBean(IncomeRequestService.class);
		outwardTaskService = SpringContextUtils.getBean(OutwardTaskService.class);
		bankLogService = SpringContextUtils.getBean(BankLogService.class);
		transactionLogService = SpringContextUtils.getBean(TransactionLogService.class);
		redisService = SpringContextUtils.getBean(RedisService.class);
		accountService = SpringContextUtils.getBean(AccountService.class);
	}

	@Override
	public void run() {
		boolean matched = false;
		// 传递给前端的帐号信息，前端根据自己选择的盘口与帐号来确定是否要刷新页面数据，只刷该事件类型的数据
		IncomeAuditWs noticeEntity = new IncomeAuditWs();
		try {
			// 匹配二种情况触发：流水进来，入款请求(或系统中转类型)进来
			if (null != bankLog) {
				// 入款, 银行流水有二种可能：1、+入款（可能是系统中转则会有二条银行流水，from,to帐号同时产生流水），2、-出款
				if (bankLog.getAmount().floatValue() > 0) {
					// 匹配规则：to帐号一样 --金额相同 --支付码确认码相同
					Specification<BizIncomeRequest> specification = DynamicSpecifications.build(null,
							BizIncomeRequest.class, new SearchFilter("toId", Operator.EQ, bankLog.getFromAccount()),
							// new SearchFilter("memberRealName", Operator.EQ,
							// bankLog.getToAccountOwner()),
							new SearchFilter("status", Operator.EQ, IncomeRequestStatus.Matching.getStatus()),
							new SearchFilter("amount", Operator.EQ, bankLog.getAmount()));
					// List<BizIncomeRequest> incomes =
					// incomeRequestService.findAll(specification);
					List<BizIncomeRequest> incomes = null;
					if (null != incomes && incomes.size() > 0) {
						incomeRequest = incomes.get(0);
						matched = true;
					}
				} else {
					// 流水金额为负数-，先匹配出款请求
					Specification<BizOutwardTask> specification = DynamicSpecifications.build(null,
							BizOutwardTask.class, new SearchFilter("accountId", Operator.EQ, bankLog.getFromAccount()),
							new SearchFilter("status", Operator.EQ, OutwardTaskStatus.Deposited.getStatus()),
							new SearchFilter("amount", Operator.EQ, Math.abs(bankLog.getAmount().floatValue())));
					List<BizOutwardTask> entitys = outwardTaskService.findList(specification,
							new Sort(Sort.Direction.DESC, "id"));
					// 若出款没匹配上，则可能是系统中转产生的流水
					if (null != entitys && entitys.size() > 0) {
						outwardTask = entitys.get(0);
						matched = true;
						// 若是出款，匹配到流水，更新状态
						outwardTask.setStatus(OutwardTaskStatus.Matched.getStatus());
						outwardTaskService.update(outwardTask);
					} else {
						// 向系统中转匹配：from帐号一样 --金额相同 --支付码确认码相同
						Specification<BizIncomeRequest> filter = DynamicSpecifications.build(null,
								BizIncomeRequest.class,
								new SearchFilter("fromAccount", Operator.EQ, bankLog.getFromAccount()),
								new SearchFilter("status", Operator.EQ, IncomeRequestStatus.Matching.getStatus()),
								new SearchFilter("amount", Operator.EQ, Math.abs(bankLog.getAmount().floatValue())));
						// List<BizIncomeRequest> incomes =
						// incomeRequestService.findAll(filter);
						List<BizIncomeRequest> incomes = null;
						if (null != incomes && incomes.size() > 0) {
							incomeRequest = incomes.get(0);
							matched = true;
						}
					}
					// 未匹配任何提单记录，需要发出告警，对于转出的流水一定严管，涉及资金流失
					if (!matched) {
						// TODO
					}
				}
				noticeEntity.setAccountId(bankLog.getFromAccount());
				noticeEntity.setIncomeAuditWsFrom(IncomeAuditWsEnum.FromInBankLog.ordinal());
				noticeEntity.setMessage("");
			} else if (null != incomeRequest) {
				Specification<BizBankLog> specification = DynamicSpecifications.build(null, BizBankLog.class,
						// new SearchFilter("toAccountOwner", Operator.EQ,
						// incomeRequest.getMemberRealName()),
						new SearchFilter("fromAccount", Operator.EQ, incomeRequest.getToId()),
						new SearchFilter("status", Operator.EQ, BankLogStatus.Matching.getStatus()),
						new SearchFilter("amount", Operator.EQ, incomeRequest.getAmount()));
				List<BizBankLog> logs = bankLogService.findAll(specification);
				if (null != logs && logs.size() > 0) {
					bankLog = logs.get(0);
					matched = true;
				}
				noticeEntity.setAccount(incomeRequest.getToAccount());
				noticeEntity.setAccountId(incomeRequest.getToId());
				noticeEntity.setIncomeAuditWsFrom(IncomeAuditWsEnum.FromIncomeReq.ordinal());
			}
			if (matched) {
				BizTransactionLog o = new BizTransactionLog();
				// 插入系统流水,判断是转入还是转出，小于0转出
				if (bankLog.getAmount().floatValue() < 0) {
					o.setOrderId(outwardTask.getId());
					o.setOperator(outwardTask.getOperator());
					o.setToAccount(0);// 会员帐号在系统中不存在，置0
					o.setFromAccount(outwardTask.getAccountId());// 来自哪个帐号
					o.setType(TransactionLogType.OUTWARD.getType());
				} else {
					increment(incomeRequest.getToId());
					if (IncomeRequestType.isPlatform(incomeRequest.getType())) {
						o.setFromAccount(0);// 会员帐号在系统中不存在，置0
					} else {
						// TODO
						// o.setFromAccount(accountService.(incomeRequest.getFromAccount()));
					}
					o.setToAccount(bankLog.getFromAccount());
					o.setOrderId(incomeRequest.getId());
					o.setType(incomeRequest.getType());
					o.setOperator(incomeRequest.getOperator());
				}
				o.setAmount(bankLog.getAmount());
				o.setToBanklogId(bankLog.getId());
				o.setConfirmor(1);// 系统管理员,用户表第一个记录
				o.setCreateTime(new Date());
				transactionLogService.save(o);
				log.info("Matched! OrderId:{}, Bank log id:{}",
						outwardTask == null ? incomeRequest.getOrderNo() : outwardTask.getId() + "(taskId)",
						bankLog.getId());
				noticeEntity.setIncomeAuditWsFrom(IncomeAuditWsEnum.FromMatched.ordinal());
				if (IncomeRequestType.isPlatform(o.getType())) {
					// 向平台反馈--> 确认
					// incomeRequestService.reportStatus2Platform(IncomeRequestStatus.Matched.getStatus(),
					// incomeRequest.getId(),
					// IncomeRequestStatus.Matched.getMsg(),
					// incomeRequest.getOrderNo(),
					// incomeRequest.getHandicap(),
					// incomeRequest.getMemberCode(), incomeRequest.getToId());
				}
			}
			// 未匹配，且入款类型为第三方，第三方当前不用对帐，直接匹配插入系统交易记录表
			if (null != incomeRequest
					&& incomeRequest.getType().intValue() == IncomeRequestType.PlatFromThird.getType()) {
				BizTransactionLog o = new BizTransactionLog();
				o.setFromAccount(0);// 会员帐号在系统中不存在，置0
				o.setToAccount(incomeRequest.getToId());
				o.setOrderId(incomeRequest.getId());
				o.setType(incomeRequest.getType());
				o.setOperator(incomeRequest.getOperator());
				o.setConfirmor(1);// 系统管理员,用户表第一个记录
				o.setAmount(incomeRequest.getAmount());
				o.setFee(incomeRequest.getFee());
				o.setCreateTime(new Date());
				o.setRemark(incomeRequest.getRemark());
				transactionLogService.save(o);
				log.info("Income Third-party! OrderNo:{}", incomeRequest.getOrderNo());
				increment(incomeRequest.getToId());
			} else {
				ObjectMapper mapper = new ObjectMapper();
				// 广播消息通知前端有新的记录，browser通过websocket事件刷新内容
				// redisService.convertAndSend(PatternTopicEnum.BROADCAST.toString(),
				// IncomeRequestWebSocketEndpoint.class.getName() +
				// mapper.writeValueAsString(noticeEntity));
				// log.info("Income --> {}",
				// mapper.writeValueAsString(noticeEntity));
			}
		} catch (Exception e) {
			log.error("AutoMatch", e);
		}
	}

	/**
	 * 为指定帐号递增每日入款
	 * 
	 * @param accountId
	 */
	private void increment(int accountId) {
		// 更新当日入款数
		redisService.increment(RedisKeys.AMOUNT_SUM_BY_DAILY_INCOME, String.valueOf(accountId),
				incomeRequest.getAmount().floatValue());
		Long expire = redisService.getFloatRedisTemplate().boundHashOps(RedisKeys.AMOUNT_SUM_BY_DAILY_INCOME)
				.getExpire();
		if (null != expire && expire.longValue() < 0) {
			// 当日清零
			long currentTimeMillis = System.currentTimeMillis();
			long expireTime = CommonUtils.getExpireTime4AmountDaily() - currentTimeMillis;
			redisService.getFloatRedisTemplate().expire(RedisKeys.AMOUNT_SUM_BY_DAILY_INCOME, expireTime,
					TimeUnit.MILLISECONDS);
			log.debug("Reset IncomeDailyTotal expire : {}", expireTime);
		}
	}

}
