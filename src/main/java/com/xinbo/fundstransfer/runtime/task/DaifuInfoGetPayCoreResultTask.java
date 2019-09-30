package com.xinbo.fundstransfer.runtime.task;

import com.xinbo.fundstransfer.component.spring.SpringContextUtils;
import com.xinbo.fundstransfer.daifucomponent.service.impl.DaifuServiceImpl;
import com.xinbo.fundstransfer.report.SystemAccountManager;
import com.xinbo.fundstransfer.service.AllocateIncomeAccountService;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * 请求payCore获取代付结果任务
 * 
 * @author blake
 *
 */
@Slf4j
public class DaifuInfoGetPayCoreResultTask implements Runnable {

	private AllocateIncomeAccountService allocateIncomeAccountService;
	private DaifuServiceImpl daifuServiceImpl;
	private SystemAccountManager systemAccountManager;

	public DaifuInfoGetPayCoreResultTask() {
		this.allocateIncomeAccountService = SpringContextUtils.getBean(AllocateIncomeAccountService.class);
		this.daifuServiceImpl = SpringContextUtils.getBean(DaifuServiceImpl.class);
		this.systemAccountManager = SpringContextUtils.getBean(SystemAccountManager.class);
	}

	@Override
	public void run() {
		boolean flag = allocateIncomeAccountService.checkHostRunRight();
		if (!flag) {
			log.debug("无权限执行请求payCore获取代付订单结果的任务:{}", flag);
			return;
		}
		if (systemAccountManager.checkRight4Accounting()) {
			return;
		}
		// 同步代付听到的支付结果
		daifuServiceImpl.getPayCoreResult4Task();
	}
}
