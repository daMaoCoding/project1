package com.xinbo.fundstransfer.runtime.task;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import com.xinbo.fundstransfer.service.IncomeAuditWechatInService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.component.spring.SpringContextUtils;
import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.entity.BizIncomeRequest;
import com.xinbo.fundstransfer.domain.entity.BizOutwardTask;
import com.xinbo.fundstransfer.domain.entity.BizTransactionLog;
import com.xinbo.fundstransfer.domain.entity.BizWechatLog;
import com.xinbo.fundstransfer.domain.entity.BizWechatRequest;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.enums.BankLogStatus;
import com.xinbo.fundstransfer.domain.enums.IncomeAuditWsEnum;
import com.xinbo.fundstransfer.domain.enums.IncomeRequestStatus;
import com.xinbo.fundstransfer.domain.enums.IncomeRequestType;
import com.xinbo.fundstransfer.domain.enums.TransactionLogType;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.runtime.MemCacheUtils;
import com.xinbo.fundstransfer.service.AccountService;

/**
 * 微信流水处理
 * 
 * @author 007
 *
 */
public class WechatLogHandler implements Runnable {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private ObjectMapper mapper = new ObjectMapper();
	private IncomeAuditWechatInService incomeAuditWechatInService;
	private boolean isRuning = true;

	public WechatLogHandler() {
		try {
			incomeAuditWechatInService = SpringContextUtils.getBean(IncomeAuditWechatInService.class);
		} catch (Exception e) {
			logger.error("", e);
		}
	}

	@Override
	public void run() {
		logger.info("begin handler wechat log ...");
		while (isRuning) {
			for (;;) {
				try {
					String json = MemCacheUtils.getInstance().getWechatlogs().poll();
					if (StringUtils.isNotEmpty(json)) {
						BizWechatLog o = mapper.readValue(json, BizWechatLog.class);
						SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						// 校验是否存在流水
						List<Object[]> wechatList = incomeAuditWechatInService.findRepeatWechatLog(o.getFromAccount(),
								o.getAmount(), o.getBalance(), sd.format(o.getTradingTime()));
						if (null == wechatList || !(wechatList.size() > 0)) {
							o.setCreateTime(new Date());
							o = incomeAuditWechatInService.save(o);
							logger.info("Wechatlog saved[DB]: {}", o);
							// 自动匹配
							if (null != o && o.getStatus() == BankLogStatus.Matching.getStatus().intValue()) {
								match(o);
							}
						} else {
							logger.debug("Wecahtlog already exists. {}", o);
						}
					} else {
						Thread.sleep(3000);
						logger.trace("No data, sleep 3000 ms");
					}
				} catch (Exception e) {
					logger.error("Wechatlog Error", e);
				}
			}
		}
		logger.warn(">>>>>>>>>>> WechatLogHandler finished");
	}

	private void match(BizWechatLog wechatLog) throws Exception {
		boolean matched = false;
		Object[] incomeRequest = null;
		// 转入, 银行流水有二种可能：1、+入款（可能是系统中转则会有二条银行流水，from,to帐号同时产生流水），2、-出款
		if (wechatLog.getAmount().floatValue() > 0) {
			/** 入款与流水记录匹配最大时间间隔（小时） */
			Integer validIntervalTimeHour = Integer
					.parseInt(MemCacheUtils.getInstance().getSystemProfile().getOrDefault("INCOME_MATCH_HOURS", "2"));
			// 有的流水没有具体的时间 只有当天的日期，无法匹配，这里做转换。
			if (wechatLog.getTradingTime().toString().indexOf("00:00:00") > -1) {
				wechatLog.setTradingTime(wechatLog.getCreateTime());
			}
			List<Object[]> incomes;
			SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			// 匹配规则：入款帐号相同--》入款金额相等--》入款时间在设置范围-->状态是未匹配
			incomes = incomeAuditWechatInService.findWechatRequest(wechatLog.getFromAccount(), wechatLog.getAmount(),
					sd.format(wechatLog.getTradingTime()), validIntervalTimeHour);
			logger.debug("Matching! FromAccount:{},TradingTime:{},Type:{},Amount:{},isNull:{}",
					wechatLog.getFromAccount(), wechatLog.getTradingTime(),
					Math.abs(wechatLog.getAmount().floatValue()), (null == incomes || incomes.size() <= 0));
			if (null != incomes && incomes.size() > 0) {
				matched = true;
				incomeRequest = incomes.get(0);
			}
			if (matched) {
				// 向平台确定且更新状态
				try {
					String remarkNew = CommonUtils.genRemark((String) incomeRequest[7], "匹配成功!", new Date(), "系统");
					// incomeAuditWechatInService.wechatAck(wechatLog, new
					// Integer(String.valueOf(incomeRequest[3])),
					// new Integer(String.valueOf(incomeRequest[11])), (String)
					// incomeRequest[8],
					// IncomeRequestStatus.Matched.getMsg(), remarkNew,
					// new Integer(String.valueOf(incomeRequest[0])));
				} catch (Exception e) {
					logger.debug("WechatLog 匹配出错！{} ", e);
					e.printStackTrace();
				}
			}
		}
	}

	public void stop() {
		isRuning = false;
	}

}
