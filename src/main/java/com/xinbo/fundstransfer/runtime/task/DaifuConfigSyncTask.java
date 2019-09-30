package com.xinbo.fundstransfer.runtime.task;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import com.xinbo.fundstransfer.component.net.http.RequestBodyParser;
import com.xinbo.fundstransfer.component.net.http.v2.HttpClientNew;
import com.xinbo.fundstransfer.component.spring.SpringContextUtils;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.report.SystemAccountManager;
import com.xinbo.fundstransfer.service.AllocateIncomeAccountService;
import com.xinbo.fundstransfer.service.HandicapService;

import lombok.extern.slf4j.Slf4j;

/**
 * 出款通道信息同步task
 * @author blake
 *
 */
@Slf4j
public class DaifuConfigSyncTask implements Runnable {

	private AllocateIncomeAccountService allocateIncomeAccountService;
	private HandicapService handicapService;
	private RequestBodyParser requestBodyParser;
	private SystemAccountManager systemAccountManager;

	public DaifuConfigSyncTask() {
		this.allocateIncomeAccountService = SpringContextUtils.getBean(AllocateIncomeAccountService.class);
		this.handicapService = SpringContextUtils.getBean(HandicapService.class);
		this.requestBodyParser = SpringContextUtils.getBean(RequestBodyParser.class);
		this.systemAccountManager = SpringContextUtils.getBean(SystemAccountManager.class);
	}

	@Override
	public void run() {
		boolean flag = allocateIncomeAccountService.checkHostRunRight();
		if (!flag) {
			log.debug("无权限执行同步第三方出款（代付）通道配置:{}", flag);
			return;
		}
		if(systemAccountManager.checkRight4Accounting()){
			return;
		}
		// 同步第三方出款（代付）通道配置
		daifuConfigSync();
	}

	// 向平台发起同步第三方出款（代付）通道配置
	private void daifuConfigSync() {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		long i = (long) (random.nextInt(10, 30));// 10-30随机数
		List<BizHandicap> list = handicapService.findByStatusEqual(1);
		if (list != null && list.size() > 0) {
			list.stream().filter(p -> (!p.getCode().equals("MANILA") && !p.getCode().equals("TAIWAN"))).forEach(p -> {
				HttpClientNew.getInstance().getPlatformServiceApi()
						.sync(requestBodyParser.buildSyncRequestBody(p.getCode(), 5))
						.subscribe(data -> log.info("同步盘口：{}的第三方出款（代付）通道配置,结果:{}", p.getCode(), data),
								e -> log.error("同步盘口：{}的第三方出款（代付）通道配置,失败:{}", p.getCode(), e.getStackTrace()));
				try {
					TimeUnit.SECONDS.sleep(i);
				} catch (InterruptedException e) {
					log.error("同步第三方出款（代付）通道配置,e:", e);
				}
			});
		}
	}

}
