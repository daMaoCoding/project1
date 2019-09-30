package com.xinbo.fundstransfer.runtime.task;

import com.xinbo.fundstransfer.report.turn.TurnHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xinbo.fundstransfer.component.spring.SpringContextUtils;

import java.util.concurrent.TimeUnit;

public class OutwardTaskTurnHandler implements Runnable {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private TurnHandler turnHandler;
	private boolean isRuning = true;

	public OutwardTaskTurnHandler() {
		try {
			turnHandler = SpringContextUtils.getBean(TurnHandler.class);
		} catch (Exception e) {
			logger.error("", e);
		}
	}

	@Override
	public void run() {
		logger.info("begin handler OutwardTaskTurn ...");
		while (isRuning) {
			try {
				turnHandler.exe();
				TimeUnit.MINUTES.sleep(1);
			} catch (Exception e) {
			}
		}
		logger.warn(">>>>>>>>>>> handler OutwardTaskTurn finished");
	}

	public void stop() {
		isRuning = false;
	}
}
