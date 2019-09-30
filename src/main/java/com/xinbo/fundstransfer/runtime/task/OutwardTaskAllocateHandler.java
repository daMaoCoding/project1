package com.xinbo.fundstransfer.runtime.task;

import com.xinbo.fundstransfer.service.AllocateOutwardTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.spring.SpringContextUtils;

/**
 * 出款任务分配
 * 
 * @author Eden
 *
 */
public class OutwardTaskAllocateHandler implements Runnable {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private AllocateOutwardTaskService outwardTaskAllocateService;
	private volatile boolean isRuning = true;

	public OutwardTaskAllocateHandler() {
		try {
			outwardTaskAllocateService = SpringContextUtils.getBean(AllocateOutwardTaskService.class);
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
		logger.info("begin handler outward task allocate...");
		while (isRuning) {
			try {
				outwardTaskAllocateService.schedule();
			} catch (Exception e) {
				logger.error("", e);
				try {
					Thread.sleep(2000L);
				} catch (InterruptedException e1) {
					logger.error("", e1);
				}
			}
		}
		logger.warn("outward task allocate finished");
	}

	public void stop() {
		isRuning = false;
	}
}
