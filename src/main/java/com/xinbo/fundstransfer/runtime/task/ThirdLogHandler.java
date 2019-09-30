package com.xinbo.fundstransfer.runtime.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.component.spring.SpringContextUtils;
import com.xinbo.fundstransfer.domain.DynamicSpecifications;
import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.entity.BizThirdLog;
import com.xinbo.fundstransfer.runtime.MemCacheUtils;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.AllocateTransService;
import com.xinbo.fundstransfer.service.AllocateTransferService;
import com.xinbo.fundstransfer.service.ThirdLogService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 第三方流水处理
 * 
 * 
 *
 */
public class ThirdLogHandler implements Runnable {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private ObjectMapper mapper = new ObjectMapper();
	private ThirdLogService thirdLogService;
	private AccountService accountService;
	private AllocateTransService allocateTransService;
	private AllocateTransferService allocateTransferService;
	private boolean isRuning = true;

	public ThirdLogHandler() {
		try {
			allocateTransService = SpringContextUtils.getBean(AllocateTransService.class);
			allocateTransferService = SpringContextUtils.getBean(AllocateTransferService.class);
			thirdLogService = SpringContextUtils.getBean(ThirdLogService.class);
			accountService = SpringContextUtils.getBean(AccountService.class);
		} catch (Exception e) {
			logger.error("", e);
		}
	}

	@Override
	public void run() {
		logger.info("begin handler third log ...");
		while (isRuning) {
			try {
				String json = MemCacheUtils.getInstance().getThirdlogs().poll();
				if (StringUtils.isNotEmpty(json)) {
					ToolResponseData data = mapper.readValue(json, ToolResponseData.class);
					for (BizThirdLog o : data.getThirdlogs()) {
						List<SearchFilter> filters = new ArrayList<SearchFilter>();
						filters.add(new SearchFilter("fromAccount", SearchFilter.Operator.EQ, o.getFromAccount()));
						filters.add(new SearchFilter("orderNo", SearchFilter.Operator.EQ, o.getOrderNo()));
						Specification<BizThirdLog> specification = DynamicSpecifications.build(BizThirdLog.class,
								filters.toArray(new SearchFilter[filters.size()]));
						Page<BizThirdLog> finds = thirdLogService.findAll(specification, null);
						if (null == finds || !finds.hasContent()) {
							thirdLogService.save(o);
							logger.info("Thirdlog saved[DB]: {}", o);
						} else {
							logger.debug("Thirdlog already exists. {}", o.getOrderNo());
						}
					}
					// 检查余额，是否需要告警或自动下发
					Integer accountId = data.getBanklogs().get(0).getFromAccount();
					BigDecimal bankBal = new BigDecimal(data.getBalance());
					// 监控余额,是否下发
					logger.info("ThirdlogMonitor AccountId={},balance:{} usableBalance:{}", accountId,
							data.getBalance(), data.getUsableBalance());
					if (AppConstants.NEW_TRANSFER) {
						allocateTransService.applyRelBal(accountId, bankBal);
					} else {
						allocateTransferService.applyRelBal(accountId, bankBal, false);
					}
				} else {
					Thread.sleep(3000);
					logger.trace("No data, sleep 3000 ms");
				}
			} catch (Exception e) {
				try {
					TimeUnit.SECONDS.sleep(5);
				} catch (InterruptedException e1) {

				}
				logger.error("", e);
			}
		}
		logger.warn(">>>>>>>>>>> ThirdLogHandler finished");
	}

	public void stop() {
		isRuning = false;
	}

}
