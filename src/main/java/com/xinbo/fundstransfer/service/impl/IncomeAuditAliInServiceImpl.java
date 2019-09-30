package com.xinbo.fundstransfer.service.impl;

import com.xinbo.fundstransfer.component.net.http.RequestBodyParser;
import com.xinbo.fundstransfer.component.net.http.cloud.HttpClientCloud;
import com.xinbo.fundstransfer.component.net.http.cloud.ReqCoudBodyParser;
import com.xinbo.fundstransfer.component.spring.SpringContextUtils;
import com.xinbo.fundstransfer.domain.entity.BizAliLog;
import com.xinbo.fundstransfer.domain.repository.IncomeAuditAliInRepository;
import com.xinbo.fundstransfer.service.IncomeAuditAliInService;
import rx.functions.Action1;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncomeAuditAliInServiceImpl implements IncomeAuditAliInService {
	static final Logger log = LoggerFactory.getLogger(IncomeAuditAliInServiceImpl.class);
	@Autowired
	private IncomeAuditAliInRepository incomeAuditAliInRepository;
	@Autowired
	RequestBodyParser requestBodyParser;

	@Override
	public Object statisticalAliLog(int pageNo, List<String> handicapList, String AliNumber, String startTime,
			String endTime, String pageSise) throws Exception {
		Object[] ret = new Object[1];
		Action1<Object> post = response -> ret[0] = response;
		Action1<Throwable> throwable = e -> log.error("查询正在匹配的支付宝入款失败.", e);
		ReqCoudBodyParser req = SpringContextUtils.getBean(ReqCoudBodyParser.class);
		HttpClientCloud.getInstance().getCloudService()
				.statisticalAliLog(
						req.generalStatisticalAliLogGet(pageNo, handicapList, AliNumber, startTime, endTime, pageSise))
				.subscribe(post, throwable);
		return ret[0];
	}

	@Override
	public Object findAliMatched(int pageNo, List<String> handicapList, String startTime, String endTime, String member,
			String orderNo, BigDecimal fromAmount, BigDecimal toAmount, String payer, String AliNumber, int status,
			String pageSize) throws Exception {
		Object[] ret = new Object[1];
		Action1<Object> post = response -> ret[0] = response;
		Action1<Throwable> throwable = e -> log.error("查询已经匹配的支付宝入款失败.", e);
		ReqCoudBodyParser req = SpringContextUtils.getBean(ReqCoudBodyParser.class);
		HttpClientCloud
				.getInstance().getCloudService().findAliMatched(req.findAliMatched(pageNo, handicapList, startTime,
						endTime, member, orderNo, fromAmount, toAmount, AliNumber, status, pageSize))
				.subscribe(post, throwable);
		return ret[0];
	}

	@Override
	public Object findAliCanceled(int pageNo, List<String> handicapList, String startTime, String endTime,
			String member, String orderNo, BigDecimal fromAmount, BigDecimal toAmount, String pageSise)
			throws Exception {
		Object[] ret = new Object[1];
		Action1<Object> post = response -> ret[0] = response;
		Action1<Throwable> throwable = e -> log.error("查询已经取消的支付宝入款失败.", e);
		ReqCoudBodyParser req = SpringContextUtils.getBean(ReqCoudBodyParser.class);
		HttpClientCloud.getInstance().getCloudService().findAliCanceled(req.findAliCanceled(pageNo, handicapList,
				startTime, endTime, member, orderNo, fromAmount, toAmount, pageSise)).subscribe(post, throwable);
		return ret[0];
	}

	@Override
	public Object findAliUnClaim(int pageNo, List<String> handicapList, String startTime, String endTime, String member,
			String AliNo, BigDecimal fromAmount, BigDecimal toAmount, String pageSise) throws Exception {
		Object[] ret = new Object[1];
		Action1<Object> post = response -> ret[0] = response;
		Action1<Throwable> throwable = e -> log.error("查询支付宝未认领的流水失败.", e);
		ReqCoudBodyParser req = SpringContextUtils.getBean(ReqCoudBodyParser.class);
		HttpClientCloud.getInstance().getCloudService().findAliUnClaim(
				req.findAliUnClaim(pageNo, handicapList, startTime, endTime, AliNo, fromAmount, toAmount, pageSise))
				.subscribe(post, throwable);
		return ret[0];
	}

	@Override
	public List<Object[]> findRepeatAliLog(int fromAccount, BigDecimal amount, BigDecimal balance, String tradingTime)
			throws Exception {
		return incomeAuditAliInRepository.findRepeatAliLog(fromAccount, amount, balance, tradingTime);
	}

	@Transactional
	@Override
	public void saveAliLog(BizAliLog AliLog) throws Exception {
		SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		incomeAuditAliInRepository.saveAliLog(AliLog.getFromAccount(), sd.format(AliLog.getTradingTime()),
				AliLog.getAmount(), AliLog.getBalance(), AliLog.getSummary(), AliLog.getDepositor());

	}

	@Override
	public Object findMBAndInvoice(String pageSise, String account, String startTime, String endTime, String member,
			String orderNo, BigDecimal fromAmount, BigDecimal toAmount, String payer, int invoicePageNo,
			int banklogPageNo) throws Exception {
		Object[] ret = new Object[1];
		Action1<Object> post = response -> ret[0] = response;
		Action1<Throwable> throwable = e -> log.error("查询支付宝流水和入款单接口失败.", e);
		ReqCoudBodyParser req = SpringContextUtils.getBean(ReqCoudBodyParser.class);
		HttpClientCloud.getInstance().getCloudService()
				.findAliMBAndInvoice(req.generalAliMBAndInvoiceGet(account, startTime, endTime, member, orderNo,
						fromAmount, toAmount, payer, invoicePageNo, banklogPageNo, pageSise))
				.subscribe(post, throwable);
		return ret[0];
	}

	@Override
	public Object AliInMatch(int sysRequestId, int bankFlowId, String matchRemark, String userName) throws Exception {
		Object[] ret = new Object[1];
		Action1<Object> post = response -> ret[0] = response;
		Action1<Throwable> throwable = e -> log.error("支付宝手动匹配调用接口失败.", e);
		ReqCoudBodyParser req = SpringContextUtils.getBean(ReqCoudBodyParser.class);
		HttpClientCloud.getInstance().getCloudService()
				.aliPayAck(req.generalWechatAckGet(sysRequestId, bankFlowId, matchRemark, userName))
				.subscribe(post, throwable);
		return ret[0];
	}

	@Override
	public List<Object[]> findAliRequest(int AliId, BigDecimal amount, String tradingTime,
			Integer validIntervalTimeHour) throws Exception {
		SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return incomeAuditAliInRepository.findAliRequest(AliId, amount,
				sd.format((DateUtils.addHours(sd.parse(tradingTime), -1))),
				sd.format((DateUtils.addHours(sd.parse(tradingTime), validIntervalTimeHour))));
	}

	@Transactional
	@Override
	public BizAliLog save(BizAliLog entity) {
		return incomeAuditAliInRepository.save(entity);
	}

	@Transactional
	@Override
	public void aLiPayAck(BizAliLog aliLog, int handicapId, int memberCode, String orderNo, String remark,
			String remarkWrap, int requestId) {
		/*
		 * // new and old api compatibility String handicap =
		 * handicapService.findFromCacheById(handicapId).getCode();
		 * 
		 * // if //
		 * (Arrays.asList(AppConstants.OLD_PLATFORM_HANDICAP.split(",")).
		 * contains(handicap)) // { HttpClient httpClientInstance =
		 * HttpClient.getInstance();
		 * httpClientInstance.getIPlatformService(handicapId)
		 * .DepositMoneyLock(requestBodyParser.general(handicapId, memberCode,
		 * orderNo, remark)) .subscribe((data) -> {
		 * log.info("AliPay Lock success. orderNo: {}, response: {}", orderNo,
		 * data); if (data.contains("\"Result\":1")) {// success
		 * httpClientInstance.getIPlatformService(handicapId)
		 * .DepositMoneyAudit(requestBodyParser.general(handicapId, memberCode,
		 * orderNo, remark)) .subscribe((data1) -> { log.
		 * info("AliPay Income acknowledged success, orderNo: {}, response: {}",
		 * orderNo, data1); if (data1.contains("\"Result\":1")) { // 确认成功修改状态
		 * try { AliInMatch(requestId, aliLog.getId(), remarkWrap); } catch
		 * (Exception e1) { e1.printStackTrace();
		 * log.error("AliPay 向平台确认后修改状态失败！: " + orderNo, e1); } } }, (e) -> {
		 * log.error("AliPay Income acknowledged error. orderNo: " + orderNo,
		 * e); }); } }, (e) -> log.error("AliPay Lock error. orderNo: " +
		 * orderNo, e));
		 */
		// } else {

		// HttpClientNew.getInstance().getPlatformServiceApi()
		// .depositAck(requestBodyParser.buildRequestBody(handicap, orderNo,
		// remark)).subscribe(o -> {
		// log.info("AliPay (new)Income acknowledged success, orderNo: {},
		// response: {}", orderNo,
		// o.getMessage());
		// // 返回 1 表示匹配成功 成功匹配消息类型 3
		// if (o.getStatus() == 1) {
		// // 确认成功修改状态
		// try {
		// AliInMatch(requestId, aliLog.getId(), remarkWrap);
		// } catch (Exception e1) {
		// log.error("AliPay (new)Income acknowledged error. orderNo:{},error{}
		// " + orderNo, e1);
		// e1.printStackTrace();
		// }
		// }
		// }, e -> {
		// log.error("AliPay (new)Income acknowledged error. orderNo:{},error{}
		// " + orderNo, e);
		// });
		// }
	}

	@Override
	public List<Object[]> getAliRequestByid(Long id) {
		return incomeAuditAliInRepository.getAliRequestByid(id);
	}

	@Override
	public List<Object[]> getAliLogByid(Long id) {
		return incomeAuditAliInRepository.getAliLogByid(id);
	}

	@Override
	public Object updateRemarkById(long sysRequestId, String remark, String type, String userName) throws Exception {
		Object[] ret = new Object[1];
		Action1<Object> post = response -> ret[0] = response;
		Action1<Throwable> throwable = e -> log.error("支付宝添加备注调用接口失败.", e);
		ReqCoudBodyParser req = SpringContextUtils.getBean(ReqCoudBodyParser.class);
		HttpClientCloud.getInstance().getCloudService()
				.customerAddRemarkAliPay(req.generalRemarkGet(sysRequestId, remark, type, userName))
				.subscribe(post, throwable);
		return ret[0];
	}

	@Override
	public Object updateAliPayLogRemarkById(long bankLogId, String remark, String type, String userName)
			throws Exception {
		Object[] ret = new Object[1];
		Action1<Object> post = response -> ret[0] = response;
		Action1<Throwable> throwable = e -> log.error("支付宝添加备注调用接口失败.", e);
		ReqCoudBodyParser req = SpringContextUtils.getBean(ReqCoudBodyParser.class);
		HttpClientCloud.getInstance().getCloudService()
				.customerAddRemarkAliPay(req.generalRemarkGet(bankLogId, remark, type, userName))
				.subscribe(post, throwable);
		return ret[0];
	}

	/** 取消订单并通知平台 */
	@Override
	public Object cancelAndCallFlatform(int incomeRequestId, String handicap, String orderNo, String remark,
			String userName) throws Exception {
		Object[] ret = new Object[1];
		Action1<Object> post = response -> ret[0] = response;
		Action1<Throwable> throwable = e -> log.error("支付宝取消订单调用接口失败.", e);
		ReqCoudBodyParser req = SpringContextUtils.getBean(ReqCoudBodyParser.class);
		HttpClientCloud.getInstance().getCloudService()
				.aliPaydepositCancel(req.generalCancelGet(incomeRequestId, handicap, orderNo, remark, userName))
				.subscribe(post, throwable);
		return ret[0];
	}

	@Transactional
	@Override
	public Object updateTimeById(int id, Date time) throws Exception {
		Object[] ret = new Object[1];
		Action1<Object> post = response -> ret[0] = response;
		Action1<Throwable> throwable = e -> log.error("支付宝订单隐藏调用接口失败.", e);
		ReqCoudBodyParser req = SpringContextUtils.getBean(ReqCoudBodyParser.class);
		HttpClientCloud.getInstance().getCloudService().UpdateAliPayDepositTime(req.generalUpTimeGet(id, time))
				.subscribe(post, throwable);
		return ret[0];
	}

	@Override
	public Object generateAliPayRequestOrder(String memberAccount, BigDecimal amount, String account, String remark,
			String createTime, int bankLogId, String handicap, String userName) throws Exception {
		Object[] ret = new Object[1];
		Action1<Object> post = response -> ret[0] = response;
		Action1<Throwable> throwable = e -> log.error("支付宝补单调用接口失败.", e);
		ReqCoudBodyParser req = SpringContextUtils.getBean(ReqCoudBodyParser.class);
		HttpClientCloud.getInstance().getCloudService().generateRequestOrder(req.generateRequestOrder(memberAccount,
				amount, account, remark, createTime, bankLogId, handicap, userName, 1)).subscribe(post, throwable);
		return ret[0];
	}

	/**
	 * 获取正在匹配的支付宝入款单
	 * @param pageRequest 分页对象
	 * @param handicap 盘口
	 * @param level 层级
	 * @param incomeMember 入款会员
	 * @param incomeOrder 入款单号
	 * @param timeStart 入款提单时间开始
	 * @param timeEnd 入款提单时间结束
	 * @return
	 */
	@Override
	public Map<String,Object> aliIncomeToMatch(PageRequest pageRequest, Integer handicap, Integer level, String incomeMember,
			String incomeOrder,String timeStart,String timeEnd) {
		Map<String,Object> map = new HashMap<String,Object>();
		Page<Object> dataToPage = incomeAuditAliInRepository.aliIncomeToMatch(  handicap,  level,  incomeMember,
				 incomeOrder, timeStart, timeEnd,pageRequest);
		Object[] o = incomeAuditAliInRepository.totalAmountAliIncomeToMatch(  handicap,  level,  incomeMember,
				 incomeOrder, timeStart, timeEnd);
		map.put("page", dataToPage);
		map.put("totalAmount", o[0]);
		return map;
	}
	
	/**
	 * 获取失败的支付宝入款单
	 * @param pageRequest 分页对象
	 * @param handicap 盘口
	 * @param level 层级
	 * @param incomeMember 入款会员
	 * @param incomeOrder 入款单号
	 * @param timeStart 入款提单时间开始
	 * @param timeEnd 入款提单时间结束
	 * @return
	 */
	@Override
	public Map<String,Object> aliIncomeFail(PageRequest pageRequest, Integer handicap, Integer level, String incomeMember,
			String incomeOrder,String timeStart,String timeEnd) {
		Map<String,Object> map = new HashMap<String,Object>();
		Page<Object> dataToPage = incomeAuditAliInRepository.aliIncomeFail(  handicap,  level,  incomeMember,
				 incomeOrder, timeStart, timeEnd,pageRequest);
		Object[] o = incomeAuditAliInRepository.totalAmountAliIncomeFail(  handicap,  level,  incomeMember,
				 incomeOrder, timeStart, timeEnd);
		map.put("page", dataToPage);
		map.put("totalAmount", o[0]);
		return map;
	}

	/**
	 * 获取成功的支付宝入款单
	 * @param pageRequest 分页对象
	 * @param handicap 盘口
	 * @param level 层级
	 * @param member 入款会员
	 * @param toMember 收款会员
	 * @param inOrderNo 入款单号
	 * @param outOrderNo 收款单号
	 * @param toHandicapRadio 收款类型：全部0，盘口1，返利网2
	 * @param timeStart 入款提单时间开始
	 * @param timeEnd 入款提单时间结束
	 * @return
	 */
	@Override
	public Map<String, Object> aliIncomeSuccess(Pageable pageable, Integer handicap, Integer level, String member,
			String toMember, String inOrderNo, String outOrderNo, Integer toHandicapRadio, String timeStart,
			String timeEnd) {
		Map<String,Object> map = new HashMap<String,Object>();
		Page<Object> dataToPage = incomeAuditAliInRepository.aliIncomeSuccess( handicap,  level,  member,
				toMember, inOrderNo,outOrderNo,toHandicapRadio,timeStart,timeEnd,pageable);
		Object[] totalAmount = incomeAuditAliInRepository.totalAmountAliIncomeSuccess( handicap,  level,  member,
				toMember, inOrderNo,outOrderNo,toHandicapRadio,timeStart,timeEnd);
		Object[] totalToAmount = incomeAuditAliInRepository.totalToAmountAliIncomeSuccess( handicap,  level,  member,
				toMember, inOrderNo,outOrderNo,toHandicapRadio,timeStart,timeEnd);
		map.put("page", dataToPage);
		map.put("totalAmount", totalAmount[0]);
		map.put("totalToAmount", totalToAmount[0]);
		return map;
	}

	/**
	 * 获取进行的支付宝入款单
	 * @param pageRequest 分页对象
	 * @param handicap 盘口
	 * @param level 层级
	 * @param member 入款会员
	 * @param toMember 收款会员
	 * @param inOrderNo 入款单号
	 * @param outOrderNo 收款单号
	 * @param toHandicapRadio 收款类型：全部0，盘口1，返利网2
	 * @param timeStart 入款提单时间开始
	 * @param timeEnd 入款提单时间结束
	 * @return
	 */
	@Override
	public Map<String, Object> aliIncomeMatched(Pageable pageable, Integer handicap, Integer level, String member,
			String toMember, String inOrderNo, String outOrderNo, Integer toHandicapRadio, String timeStart,
			String timeEnd) {
		Map<String,Object> map = new HashMap<String,Object>();
		Page<Object> dataToPage = incomeAuditAliInRepository.aliIncomeMatched( handicap,  level,  member,
				toMember, inOrderNo,outOrderNo,toHandicapRadio,timeStart, timeEnd,pageable);
		Object[] totalAmount = incomeAuditAliInRepository.totalAmountAliIncomeMatched(handicap,  level,  member,
				toMember, inOrderNo,outOrderNo,toHandicapRadio,timeStart,timeEnd);
		Object[] totalToAmount = incomeAuditAliInRepository.totalToAmountAliIncomeMatched(handicap,  level,  member,
				toMember, inOrderNo,outOrderNo,toHandicapRadio,timeStart,timeEnd);
		map.put("page", dataToPage);
		map.put("totalAmount", totalAmount[0]);
		map.put("totalToAmount", totalToAmount[0]);
		return map;
	}
	
}
