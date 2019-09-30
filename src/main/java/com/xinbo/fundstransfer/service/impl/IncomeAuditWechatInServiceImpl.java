package com.xinbo.fundstransfer.service.impl;

import com.xinbo.fundstransfer.component.net.http.RequestBodyParser;
import com.xinbo.fundstransfer.component.net.http.cloud.HttpClientCloud;
import com.xinbo.fundstransfer.component.net.http.cloud.ReqCoudBodyParser;
import com.xinbo.fundstransfer.component.spring.SpringContextUtils;
import com.xinbo.fundstransfer.domain.entity.BizWechatLog;
import com.xinbo.fundstransfer.domain.repository.IncomeAuditWechatInRepository;
import com.xinbo.fundstransfer.service.IncomeAuditWechatInService;
import rx.functions.Action1;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncomeAuditWechatInServiceImpl implements IncomeAuditWechatInService {
	static final Logger log = LoggerFactory.getLogger(IncomeAuditWechatInServiceImpl.class);
	@Autowired
	private IncomeAuditWechatInRepository incomeAuditWechatInRepository;
	@Autowired
	RequestBodyParser requestBodyParser;

	@Override
	public Object statisticalWechatLog(String pageNo, List<String> handicapList, String wechatNumber, String startTime,
			String endTime, String pageSize) throws Exception {
		Object[] ret = new Object[1];
		Action1<Object> post = response -> ret[0] = response;
		Action1<Throwable> throwable = e -> log.error("查询正在匹配的微信入款失败.", e);
		ReqCoudBodyParser req = SpringContextUtils.getBean(ReqCoudBodyParser.class);
		HttpClientCloud.getInstance().getCloudService()
				.findWechatLogByWechar(
						req.generalWechatLogGet(pageNo, handicapList, wechatNumber, startTime, endTime, pageSize))
				.subscribe(post, throwable);
		return ret[0];
	}

	@Override
	public Object findWechatMatched(int pageNo, List<String> handicapList, String startTime, String endTime,
			String member, String orderNo, BigDecimal fromAmount, BigDecimal toAmount, String wechatNumber, int status,
			String pageSise) throws Exception {
		Object[] ret = new Object[1];
		Action1<Object> post = response -> ret[0] = response;
		Action1<Throwable> throwable = e -> log.error("查询已经匹配的微信入款失败.", e);
		ReqCoudBodyParser req = SpringContextUtils.getBean(ReqCoudBodyParser.class);
		HttpClientCloud.getInstance()
				.getCloudService().findWechatMatched(req.findWechatMatched(pageNo, handicapList, startTime, endTime,
						member, orderNo, fromAmount, toAmount, wechatNumber, status, pageSise))
				.subscribe(post, throwable);
		return ret[0];
	}

	@Override
	public Object findWechatCanceled(int pageNo, List<String> handicapList, String startTime, String endTime,
			String member, String orderNo, BigDecimal fromAmount, BigDecimal toAmount, String pageSise)
			throws Exception {
		Object[] ret = new Object[1];
		Action1<Object> post = response -> ret[0] = response;
		Action1<Throwable> throwable = e -> log.error("查询已经取消的微信入款失败.", e);
		ReqCoudBodyParser req = SpringContextUtils.getBean(ReqCoudBodyParser.class);
		HttpClientCloud
				.getInstance().getCloudService().findWechatCanceled(req.findWechatCanceled(pageNo, handicapList,
						startTime, endTime, member, orderNo, fromAmount, toAmount, pageSise))
				.subscribe(post, throwable);
		return ret[0];
	}

	@Override
	public Object findWechatUnClaim(int pageNo, List<String> handicapList, String startTime, String endTime,
			String wechatNo, BigDecimal fromAmount, BigDecimal toAmount, String pageSise) throws Exception {
		Object[] ret = new Object[1];
		Action1<Object> post = response -> ret[0] = response;
		Action1<Throwable> throwable = e -> log.error("查询微信未认领的流水失败.", e);
		ReqCoudBodyParser req = SpringContextUtils.getBean(ReqCoudBodyParser.class);
		HttpClientCloud.getInstance().getCloudService().findWechatUnClaim(req.findWechatUnClaim(pageNo, handicapList,
				startTime, endTime, wechatNo, fromAmount, toAmount, pageSise)).subscribe(post, throwable);
		return ret[0];
	}

	@Override
	public List<Object[]> findRepeatWechatLog(int fromAccount, BigDecimal amount, BigDecimal balance,
			String tradingTime) throws Exception {
		return incomeAuditWechatInRepository.findRepeatWechatLog(fromAccount, amount, balance, tradingTime);
	}

	@Transactional
	@Override
	public void saveWechatLog(BizWechatLog wechatLog) throws Exception {
		SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		incomeAuditWechatInRepository.saveWechatLog(wechatLog.getFromAccount(), sd.format(wechatLog.getTradingTime()),
				wechatLog.getAmount(), wechatLog.getBalance(), wechatLog.getSummary(), wechatLog.getDepositor());

	}

	@Override
	public Object findMBAndInvoice(String account, String startTime, String endTime, String member, String orderNo,
			BigDecimal fromAmount, BigDecimal toAmount, String payer, String invoicePageNo, String logPageNo,
			String PageSize) throws Exception {
		Object[] ret = new Object[1];
		Action1<Object> post = response -> ret[0] = response;
		Action1<Throwable> throwable = e -> log.error("查询微信流水和入款单接口失败.", e);
		ReqCoudBodyParser req = SpringContextUtils.getBean(ReqCoudBodyParser.class);
		HttpClientCloud.getInstance()
				.getCloudService().findMBAndInvoice(req.generalMBAndInvoiceGet(account, startTime, endTime, member,
						orderNo, fromAmount, toAmount, payer, invoicePageNo, logPageNo, PageSize))
				.subscribe(post, throwable);
		return ret[0];
	}

	@Transactional
	@Override
	public void wechatInMatch(int sysRequestId, int bankFlowId, String matchRemark) throws Exception {
		incomeAuditWechatInRepository.updateWecahtSysByid(sysRequestId, bankFlowId, matchRemark, 1);
		incomeAuditWechatInRepository.updateWecahtBankByid(sysRequestId, bankFlowId);
	}

	@Override
	public List<Object[]> findWechatRequest(int wechatId, BigDecimal amount, String tradingTime,
			Integer validIntervalTimeHour) throws Exception {
		SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return incomeAuditWechatInRepository.findWechatRequest(wechatId, amount,
				sd.format((DateUtils.addHours(sd.parse(tradingTime), -1))),
				sd.format((DateUtils.addHours(sd.parse(tradingTime), validIntervalTimeHour))));
	}

	@Transactional
	@Override
	public BizWechatLog save(BizWechatLog entity) {
		return incomeAuditWechatInRepository.save(entity);
	}

	@Override
	public List<Object[]> getWechatRequestByid(Long wecahtId) {
		return incomeAuditWechatInRepository.getWechatRequestByid(wecahtId);
	}

	@Override
	public Object updateRemarkById(long sysRequestId, String remark, String type, String userName) throws Exception {
		Object[] ret = new Object[1];
		Action1<Object> post = response -> ret[0] = response;
		Action1<Throwable> throwable = e -> log.error("微信添加备注调用接口失败.", e);
		ReqCoudBodyParser req = SpringContextUtils.getBean(ReqCoudBodyParser.class);
		HttpClientCloud.getInstance().getCloudService()
				.customerAddRemarkWecaht(req.generalRemarkGet(sysRequestId, remark, type, userName))
				.subscribe(post, throwable);
		return ret[0];
	}

	@Override
	public List<Object[]> getWechatLogByid(Long id) {
		return incomeAuditWechatInRepository.getWechatLogByid(id);
	}

	@Override
	public Object updateWecahtLogRemarkById(long id, String remark, String type, String userName) throws Exception {
		Object[] ret = new Object[1];
		Action1<Object> post = response -> ret[0] = response;
		Action1<Throwable> throwable = e -> log.error("微信添加备注调用接口失败.", e);
		ReqCoudBodyParser req = SpringContextUtils.getBean(ReqCoudBodyParser.class);
		HttpClientCloud.getInstance().getCloudService()
				.customerAddRemarkWecaht(req.generalRemarkGet(id, remark, type, userName)).subscribe(post, throwable);
		return ret[0];

	}

	@Override
	public Object wechatAck(int sysRequestId, int bankFlowId, String matchRemark, String userName) throws Exception {
		Object[] ret = new Object[1];
		Action1<Object> post = response -> ret[0] = response;
		Action1<Throwable> throwable = e -> log.error("微信手动匹配调用接口失败.", e);
		ReqCoudBodyParser req = SpringContextUtils.getBean(ReqCoudBodyParser.class);
		HttpClientCloud.getInstance().getCloudService()
				.wechatAck(req.generalWechatAckGet(sysRequestId, bankFlowId, matchRemark, userName))
				.subscribe(post, throwable);
		return ret[0];
	}

	@Override
	public Object cancelAndCallFlatform(int incomeRequestId, String handicap, String orderNo, String remark,
			String userName) throws Exception {
		Object[] ret = new Object[1];
		Action1<Object> post = response -> ret[0] = response;
		Action1<Throwable> throwable = e -> log.error("微信订单取消调用接口失败.", e);
		ReqCoudBodyParser req = SpringContextUtils.getBean(ReqCoudBodyParser.class);
		HttpClientCloud.getInstance().getCloudService()
				.wechatdepositCancel(req.generalCancelGet(incomeRequestId, handicap, orderNo, remark, userName))
				.subscribe(post, throwable);
		return ret[0];
	}

	@Override
	public Object updateTimeById(int id, Date time) throws Exception {
		Object[] ret = new Object[1];
		Action1<Object> post = response -> ret[0] = response;
		Action1<Throwable> throwable = e -> log.error("微信订单隐藏调用接口失败.", e);
		ReqCoudBodyParser req = SpringContextUtils.getBean(ReqCoudBodyParser.class);
		HttpClientCloud.getInstance().getCloudService().UpdateWechatDepositTime(req.generalUpTimeGet(id, time))
				.subscribe(post, throwable);
		return ret[0];
	}

	@Override
	public Object generateWecahtRequestOrder(String memberAccount, BigDecimal amount, String account, String remark,
			String createTime, int bankLogId, String handicap, String userName) throws Exception {
		Object[] ret = new Object[1];
		Action1<Object> post = response -> ret[0] = response;
		Action1<Throwable> throwable = e -> log.error("微信补单调用接口失败.", e);
		ReqCoudBodyParser req = SpringContextUtils.getBean(ReqCoudBodyParser.class);
		HttpClientCloud.getInstance().getCloudService().generateRequestOrder(req.generateRequestOrder(memberAccount,
				amount, account, remark, createTime, bankLogId, handicap, userName, 2)).subscribe(post, throwable);
		return ret[0];
	}

}
