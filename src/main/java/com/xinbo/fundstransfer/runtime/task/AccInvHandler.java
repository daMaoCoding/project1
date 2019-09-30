package com.xinbo.fundstransfer.runtime.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AccInvHandler implements Runnable {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private boolean isRuning = true;

	public AccInvHandler() {

	}

	@Override
	public void run() {
		logger.info("begin handler AccInv ...");
		logger.warn(">>>>>>>>>>> handler AccInv finished");

	}

	public void stop() {
		isRuning = false;
	}
}
