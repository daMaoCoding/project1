package com.xinbo.fundstransfer.component.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.runtime.ConcurrentUtils;
import com.xinbo.fundstransfer.service.RedisService;

/**
 * spring 启动完成后事件监听器处理
 *
 *
 *
 */
@Component
public class ApplicationInitListener implements ApplicationRunner {

	Logger log = LoggerFactory.getLogger(this.getClass());
	private static boolean SYS_INIT = false;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		if (!SYS_INIT) {
			SYS_INIT = true;
			ConcurrentUtils.getInstance().startHandler();
			log.debug(">>>>>>>>>>>>>>>>>> after startup of spring.");
			// 系统广播：WEB-应用启动
			SpringContextUtils.getBean(RedisService.class).convertAndSend(RedisTopics.SYS_REBOOT,
					CommonUtils.getInternalIp());
		}
	}
}
