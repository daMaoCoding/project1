package com.xinbo.fundstransfer.runtime.task;

import com.xinbo.fundstransfer.report.up.ReportHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xinbo.fundstransfer.component.spring.SpringContextUtils;

/**
 * 出款请求处理
 *
 * @author Dom
 *
 */
public class SysBalHandler implements Runnable {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private ReportHandler reportHandler;
	private boolean isRuning = true;

	public SysBalHandler() {
		try {
            reportHandler = SpringContextUtils.getBean(ReportHandler.class);
		} catch (Exception e) {
			logger.error("", e);
		}
	}

	@Override
	public void run() {
		logger.info("begin handler SysBal ...");
		while (isRuning) {
			try {
                reportHandler.deal();
			} catch (Exception e) {
				try {
					Thread.sleep(2000L);
				} catch (InterruptedException e1) {
					logger.error("", e1);
				}
				logger.error("", e);
			}
		}
		logger.warn(">>>>>>>>>>> handler SysBal finished");

	}

	public void stop() {
		isRuning = false;
	}
}
