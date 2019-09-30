package com.xinbo.fundstransfer.runtime.task;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.report.SystemAccountManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.component.spring.SpringContextUtils;
import com.xinbo.fundstransfer.domain.entity.BizIncomeRequest;
import com.xinbo.fundstransfer.service.IncomeRequestService;
import com.xinbo.fundstransfer.service.RedisService;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * 入款请求处理
 * 
 * 
 *
 */
public class IncomeHandler implements Runnable {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private IncomeRequestService incomeRequestService;
	private RedisService redisService;
	private ObjectMapper mapper;
	private boolean isRuning = true;
	private SystemAccountManager systemAccountManager;

	public IncomeHandler() {
		try {
			mapper = new ObjectMapper();
			incomeRequestService = SpringContextUtils.getBean(IncomeRequestService.class);
			redisService = SpringContextUtils.getBean(RedisService.class);
			systemAccountManager = SpringContextUtils.getBean(SystemAccountManager.class);
		} catch (Exception e) {
			logger.error("", e);
		}
	}

	@Override
	public void run() {
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		ListOperations operations = template.opsForList();
		while (isRuning) {
			Object entity = null;
			try {
				if (systemAccountManager.checkRight4Accounting()) {
					TimeUnit.SECONDS.sleep(120);
					continue;
				}
				entity = operations.rightPop(RedisKeys.INCOME_REQUEST);
				if (entity == null) {
					ThreadLocalRandom random = ThreadLocalRandom.current();
					TimeUnit.SECONDS.sleep(random.nextInt(2, 5));
					continue;
				}
				String entityStr = entity.toString();
				logger.debug("从redis 队列INCOME_REQUEST 获取的信息:{}", entityStr);
				String[] str = entityStr.split("#");
				if (str.length > 4) {
					// 一个订单有多个收款账号的情况
					List<BizIncomeRequest> multi = incomeRequestService.findByCacheStrMultiToAccount(str);
					if (!CollectionUtils.isEmpty(multi)) {
						incomeRequestService.match4OrdersByAliInBankAccount(multi);
					}

				} else {
					BizIncomeRequest res = incomeRequestService.findOneByCacheStr(str);
					logger.info("根据参数 :{} 查询出的结果:{}", entity, res);
					if (!ObjectUtils.isEmpty(res)) {
						incomeRequestService.save(res, false);
					}
				}

			} catch (Exception e) {
				logger.error("incomeHander save error: {}", entity, e);
			}
		}
		logger.warn(">>>>>>>>>>> IncomeHandler finished");
	}

	public void stop() {
		isRuning = false;
	}
}
