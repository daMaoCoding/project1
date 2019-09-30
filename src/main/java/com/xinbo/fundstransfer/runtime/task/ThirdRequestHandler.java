package com.xinbo.fundstransfer.runtime.task;

import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.component.spring.SpringContextUtils;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizThirdRequest;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.repository.AccountRepository;
import com.xinbo.fundstransfer.service.AccountExtraService;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.RedisService;
import com.xinbo.fundstransfer.service.ThirdRequestService;
import com.xinbo.fundstransfer.utils.ServiceDomain;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 第三方入款请求处理
 * 
 * 
 *
 */
@Slf4j
@Component
public class ThirdRequestHandler implements Runnable {
	private ThirdRequestService thirdRequestService;
	private RedisService redisService;
	private AccountService accountService;
	private AccountExtraService accountExtraService;
	private AccountRepository accountRepository;
	private volatile boolean isRuning = true;
	private static boolean checkHostRunRight = false;

	@Value("${service.tag}")
	public void setServiceTag(String serviceTag) {
		if (Objects.nonNull(serviceTag)) {
			checkHostRunRight = ServiceDomain.valueOf(serviceTag) == ServiceDomain.INNER;
		}
	}

	public ThirdRequestHandler() {
		try {
			thirdRequestService = SpringContextUtils.getBean(ThirdRequestService.class);
			redisService = SpringContextUtils.getBean(RedisService.class);
			accountService = SpringContextUtils.getBean(AccountService.class);
			accountExtraService = SpringContextUtils.getBean(AccountExtraService.class);
			accountRepository = SpringContextUtils.getBean(AccountRepository.class);
		} catch (Exception e) {
			log.error("", e);
		}
	}

	/**
	 * 更新第三方当日入款数:总入款、手续费、实际入款
	 */
	private void incrementThirdAmount(Integer toId, BigDecimal amount, String type) {
		try {
			if (type.equals("total"))
				redisService.increment(RedisKeys.AMOUNT_SUM_BY_DAILY_THIRD_TOTAL_INCOME, String.valueOf(toId),
						amount.floatValue());
			else if (type.equals("fee"))
				redisService.increment(RedisKeys.AMOUNT_SUM_BY_DAILY_THIRD_FEE_INCOME, String.valueOf(toId),
						amount.floatValue());
			else
				redisService.increment(RedisKeys.AMOUNT_SUM_BY_DAILY_THIRD_AMOUNT_INCOME, String.valueOf(toId),
						amount.floatValue());
			Long expire = redisService.getFloatRedisTemplate().boundHashOps(RedisKeys.AMOUNT_SUM_BY_DAILY_INCOME)
					.getExpire();
			if (null != expire && expire < 0) {
				// 当日清零
				long currentTimeMillis = System.currentTimeMillis();
				long expireTime = CommonUtils.getExpireTime4AmountDaily() - currentTimeMillis;
				redisService.getFloatRedisTemplate().expire(RedisKeys.AMOUNT_SUM_BY_DAILY_THIRD_TOTAL_INCOME,
						expireTime, TimeUnit.MILLISECONDS);
				redisService.getFloatRedisTemplate().expire(RedisKeys.AMOUNT_SUM_BY_DAILY_THIRD_FEE_INCOME, expireTime,
						TimeUnit.MILLISECONDS);
				redisService.getFloatRedisTemplate().expire(RedisKeys.AMOUNT_SUM_BY_DAILY_THIRD_AMOUNT_INCOME,
						expireTime, TimeUnit.MILLISECONDS);
				log.debug("Reset incrementThirdAmount expire : {}", expireTime);
			}
		} catch (Exception e) {
			log.error("incrementThirdAmount error:", e);
		}
	}

	@Override
	public void run() {
		while (isRuning) {
			try {
				log.debug("是否有权限 checkHostRunRight :{}", checkHostRunRight);
				if (!checkHostRunRight) {
					TimeUnit.SECONDS.sleep(120);
					continue;
				}
				log.debug("third income run");
				Object entity = redisService.getStringRedisTemplate().opsForList()
						.leftPop(RedisKeys.INCOME_THIRD_REQUEST);
				if (entity == null) {
					ThreadLocalRandom random = ThreadLocalRandom.current();
					TimeUnit.SECONDS.sleep(random.nextInt(2, 5));
					continue;
				}
				BizThirdRequest o = thirdRequestService.findOneByCacheStr(entity.toString());
				log.debug("线程id:{} 参数:{} 查询结果:{}", Thread.currentThread().getId(), entity.toString(), o);
				if (null == o) {
					continue;
				}
				// 第三方入款扣手续费 ，查询是否已经设置费率，如果有就按照设置的来，如果没有则默认3%
				// o.getFromAccount()表示商家名称
				AccountBaseInfo account = accountService.getFromCacheByHandicapIdAndAccountAndBankName(o.getHandicap(),
						o.getToAccount(), o.getFromAccount());
				BizAccount account2 = null;
				if (account == null) {
					log.info("账号信息缓存不存在:{}", o.getToAccount());
					account2 = accountRepository.findByHandicapIdAndAccountAndBankType(o.getHandicap(),
							o.getToAccount(), o.getFromAccount());
					if (account2 == null) {
						log.info("账号信息不存在:{}", o.getToAccount());
						return;
					}
				}
				Integer accountId = account == null ? account2.getId() : account.getId();
				// 保存每笔第三方的手续费
				// o.setFee(o.getAmount().multiply(new
				// BigDecimal(rate)).setScale(2, BigDecimal.ROUND_HALF_UP));
				// thirdRequestService.save(o); 先入库再执行其他操作
				// 保存当日入款总金额
				incrementThirdAmount(accountId, o.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP), "total");
				// 保存当日入款手续费
				incrementThirdAmount(accountId, o.getFee().setScale(2, BigDecimal.ROUND_HALF_UP), "fee");
				// 保存当日实际入款金额
				incrementThirdAmount(accountId,
						o.getAmount().subtract(o.getFee()).setScale(2, BigDecimal.ROUND_HALF_UP), "amount");
				// 修改第三方余额
				accountExtraService.updateThirdAccountBl(accountId,
						o.getAmount().subtract(o.getFee()).setScale(2, BigDecimal.ROUND_HALF_UP));

			} catch (Exception e) {
				log.error("第三方订单入库之后 定时任务执行其他操作 异常 :", e);
			}
		}
		log.warn(">>>>>>>>>>> ThirdRequestHandler finished");
	}

	public void stop() {
		isRuning = false;
	}
}
