package com.xinbo.fundstransfer.runtime.task;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.xinbo.fundstransfer.report.SystemAccountManager;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import com.xinbo.fundstransfer.component.net.http.RequestBodyParser;
import com.xinbo.fundstransfer.component.net.http.v2.HttpClientNew;
import com.xinbo.fundstransfer.component.spring.SpringContextUtils;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.service.AllocateIncomeAccountService;
import com.xinbo.fundstransfer.service.HandicapService;
import com.xinbo.fundstransfer.service.IncomeRequestService;

/**
 * 取消入款(公司银行卡入款)
 * 
 * 
 *
 */
public class CancelIncomeRequestTask implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(CancelIncomeRequestTask.class);
	private IncomeRequestService incomeRequestService;
	private HandicapService handicapService;
	private RequestBodyParser requestBodyParser;
	private SystemAccountManager systemAccountManager;

	private long period;
	private AllocateIncomeAccountService allocateIncomeAccountService;

	public CancelIncomeRequestTask(long period) {
		try {
			this.period = period;
			incomeRequestService = SpringContextUtils.getBean(IncomeRequestService.class);
			handicapService = SpringContextUtils.getBean(HandicapService.class);
			requestBodyParser = SpringContextUtils.getBean(RequestBodyParser.class);
			allocateIncomeAccountService = SpringContextUtils.getBean(AllocateIncomeAccountService.class);
			systemAccountManager = SpringContextUtils.getBean(SystemAccountManager.class);
		} catch (Exception e) {
			logger.error("", e);
		}
	}

	@Override
	public void run() {
		boolean flag = allocateIncomeAccountService.checkHostRunRight();
		logger.debug("Begin cancel income request..., whether has the right ?:{}", flag);
		if (!flag) {
			logger.info("本tomcat无权限执行任务CancelIncomeRequestTask.");
			return;
		}
		if (systemAccountManager.checkRight4Accounting()) {
			return;
		}
		try {
			List<BizHandicap> handicaps = handicapService.findAllToList();
			if (CollectionUtils.isEmpty(handicaps)) {
				return;
			}
			// 计算超时时间
			Date timeout = DateUtils.addHours(new Date(), -(int) period);
			// 时间2个小时之前 且是2天之后的订单
			Date time2Days = DateUtils.addDays(new Date(), -2);
			// 所有盘口迭代查找，不同业主的接口不一样
			for (BizHandicap o : handicaps) {
				boolean hasData = false;
				List<String> cancelOrders = incomeRequestService.findAllTimeout(o.getId(), time2Days, timeout);
				if (!CollectionUtils.isEmpty(cancelOrders)) {
					hasData = true;
					String orderSeparated = String.join(",", cancelOrders);
					logger.info("Begin cancel income handicap:{},orderNos:{}", o.getCode(), orderSeparated);
					HttpClientNew.getInstance().getPlatformServiceApi()
							.depositCancel(requestBodyParser.buildRequestBody(o.getCode(), orderSeparated, "批量取消"))
							.subscribe(data -> {
								logger.info("Batch cancel success, response: {}, orders:({})", data, orderSeparated);
								// 若成功，则更新数据库
								incomeRequestService.cancelOrder(o.getId(), cancelOrders);
							}, e -> logger.error("(new)Batch cancel error. ", e));

				} else {
					logger.info("No batch cancel data.本轮定时任务未找到超时入款订单，盘口：{}", o.getCode());
				}
				// 一个盘口调用间隔5秒
				if (hasData)
					TimeUnit.SECONDS.sleep(2);
			}
			// 执行完成取消入款订单之后 执行出款审核汇总通知平台
			// List<BizOutwardRequest> list = outwardRequestService
			// .findAllByStatus(OutwardRequestStatus.Failure.getStatus());
			// if (list != null && list.size() > 0) {
			// for (BizOutwardRequest bizOutwardRequest : list) {
			// try {
			// // 操作人传null 表示系统
			// outwardTaskAllocateService.noticePlatIfFinished(null, bizOutwardRequest);
			// } catch (Exception e) {
			// logger.error("2小时通知平台失败 :参数:{},{}", bizOutwardRequest.getOrderNo(), e);
			// }
			// }
			// }

		} catch (Exception e) {
			logger.error("Batch cancel incomeRequest error.", e);
		}
		logger.info(">>>>>>>>>>> CancelIncomeRequestTask finished.");
	}

}
