package com.xinbo.fundstransfer.runtime.task;

import com.xinbo.fundstransfer.component.net.http.RequestBodyParser;
import com.xinbo.fundstransfer.component.net.http.v2.HttpClientNew;
import com.xinbo.fundstransfer.component.spring.SpringContextUtils;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.service.AllocateIncomeAccountService;
import com.xinbo.fundstransfer.service.HandicapService;
import com.xinbo.fundstransfer.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 间隔一定时间向平台发起入款订单同步: 时间: 每个盘口间隔10-30秒 随机
 */
@Slf4j
public class PullIncomeOrdersTask implements Runnable {
	private AllocateIncomeAccountService allocateIncomeAccountService;
	private HandicapService handicapService;
	private RequestBodyParser requestBodyParser;
	private RedisService redisService;
	// 同步入款任务开关key
	private static final String SYNCH_INCOMEORDERS_SWITCH_KEY = "synchIncomeOrdersSwitchKey";

	public PullIncomeOrdersTask() {
		this.allocateIncomeAccountService = SpringContextUtils.getBean(AllocateIncomeAccountService.class);
		this.handicapService = SpringContextUtils.getBean(HandicapService.class);
		this.requestBodyParser = SpringContextUtils.getBean(RequestBodyParser.class);
		redisService = SpringContextUtils.getBean(RedisService.class);
	}

	@Override
	public void run() {
		boolean flag = allocateIncomeAccountService.checkHostRunRight();
		if (!flag) {
			log.debug("无权限执行同步入款订单:{}", flag);
			return;
		}
		boolean runAble = getSwitchKeyVal(SYNCH_INCOMEORDERS_SWITCH_KEY);
		if (runAble) {
			// 同步出款订单
			synchOutReqOrdersByType1();
		} else {
			log.debug("同步出款订单定时任务是否开启:{}", runAble);
		}

	}

	public final boolean getSwitchKeyVal(String key) {
		StringRedisTemplate redisTemplate;
		try {
			redisTemplate = redisService.getStringRedisTemplate();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		boolean exist = redisTemplate.hasKey(key);
		if (exist) {
			String val = redisTemplate.opsForValue().get(key);
			if (StringUtils.isNotBlank(val)) {
				if ("1".equals(val))
					return true;
				else
					return false;
			}
		}
		return false;
	}

	// 向平台发起同步未同步过来的入款订单
	private void synchOutReqOrdersByType1() {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		long i = (long) (random.nextInt(10, 30));// 10-30随机数
		List<BizHandicap> list = handicapService.findByStatusEqual(1);
		if (list != null && list.size() > 0) {
			list.stream().filter(p -> (!p.getCode().equals("MANILA") && !p.getCode().equals("TAIWAN"))).forEach(p -> {
				HttpClientNew.getInstance().getPlatformServiceApi()
						.sync(requestBodyParser.buildSyncRequestBody(p.getCode(), 1))
						.subscribe(data -> log.info("同步盘口：{}的入款订单,结果:{}", p.getCode(), data),
								e -> log.error("同步盘口：{}的入款订单,失败:{}", p.getCode(), e.getStackTrace()));
				try {
					TimeUnit.SECONDS.sleep(i);
				} catch (InterruptedException e) {
					log.error("同步入款订单失败,e:", e);
				}
			});
		}
	}
}
