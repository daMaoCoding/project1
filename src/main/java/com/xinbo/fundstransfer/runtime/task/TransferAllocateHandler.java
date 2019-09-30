package com.xinbo.fundstransfer.runtime.task;

import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.service.AllocateTransService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xinbo.fundstransfer.component.spring.SpringContextUtils;

/**
 * 下发任务调度
 * 
 * @author 000
 *
 */
public class TransferAllocateHandler implements Runnable {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private AllocateTransService allocateTransService;
	private volatile boolean isRuning = true;

	public TransferAllocateHandler() {
		try {
			allocateTransService = SpringContextUtils.getBean(AllocateTransService.class);
		} catch (Exception e) {
			logger.error("", e);
		}
	}

	@Override
	public void run() {
		String enableHandicap = CommonUtils.getDistHandicapNewVersion();
		if ("ALL".equals(enableHandicap)) {
			return;
		}
		logger.info("begin handler transfer allocate...");
		if (AppConstants.NEW_TRANSFER) {
			while (isRuning) {
				try {
					allocateTransService.schedule();
				} catch (Exception e) {
					logger.error("error:hander transfer allocate.", e);
					try {
						Thread.sleep(2000L);
					} catch (InterruptedException e1) {
						logger.error("", e1);
					}
				}
			}
		}
		logger.warn("transfer allocate finished");
	}

	public void stop() {
		isRuning = false;
	}
}
