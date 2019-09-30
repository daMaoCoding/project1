package com.xinbo.fundstransfer.component.spring;

import com.xinbo.fundstransfer.service.impl.AllocateIncomeAccountServiceImpl;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

import com.xinbo.fundstransfer.component.net.socket.MinaMonitorServer;
import com.xinbo.fundstransfer.runtime.ConcurrentUtils;

/**
 * 系统退出时资源销毁
 * 
 *
 *
 */
@Component
public class ApplicationExitCodeGenerator implements DisposableBean, ExitCodeGenerator {
	Logger log = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private MinaMonitorServer minaMonitorServer;

	public static boolean RUN = true;

	@Override
	public int getExitCode() {
		log.info("......>>>>>>>>>>>>>>>>> Exit.");
		return 0;
	}

	@Override
	public void destroy() throws Exception {
		ApplicationExitCodeGenerator.RUN = false;
		log.info("......>>>>>>>>>>>>>>>>> Application PreDestroy...");
		AllocateIncomeAccountServiceImpl.APP_STOP = true;
		minaMonitorServer.getAcceptor().unbind();
		minaMonitorServer.getAcceptor().dispose();
		ConcurrentUtils.getInstance().shutdownNow();
		log.info("休眠3秒钟，等待线程结束手头任务");
		TimeUnit.SECONDS.sleep(3);
		log.info("......>>>>>>>>>>>>>>>>> Application destroy.");
	}

}
