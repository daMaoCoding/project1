package com.xinbo.fundstransfer.runtime.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.component.spring.SpringContextUtils;
import com.xinbo.fundstransfer.domain.entity.BizOutwardRequest;
import com.xinbo.fundstransfer.domain.enums.UserProfileKey;
import com.xinbo.fundstransfer.report.SystemAccountManager;
import com.xinbo.fundstransfer.runtime.MemCacheUtils;
import com.xinbo.fundstransfer.service.AllocateOutwardTaskService;
import com.xinbo.fundstransfer.service.BlackListService;
import com.xinbo.fundstransfer.service.OutwardRequestService;
import com.xinbo.fundstransfer.service.RedisService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 出款请求处理
 * 
 *
 *
 */
public class OutwardHandler implements Runnable {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private OutwardRequestService outwardRequestService;
	private AllocateOutwardTaskService allocateOutwardTaskService;
	private RedisService redisService;
	private ObjectMapper mapper;
	private boolean isRuning = true;
	private BlackListService blackListService;
	private SystemAccountManager systemAccountManager;

	public OutwardHandler() {
		try {
			mapper = new ObjectMapper();
			outwardRequestService = SpringContextUtils.getBean(OutwardRequestService.class);
			redisService = SpringContextUtils.getBean(RedisService.class);
			allocateOutwardTaskService = SpringContextUtils.getBean(AllocateOutwardTaskService.class);
			blackListService = SpringContextUtils.getBean(BlackListService.class);
			systemAccountManager = SpringContextUtils.getBean(SystemAccountManager.class);
		} catch (Exception e) {
			logger.error("", e);
		}
	}

	@Override
	public void run() {
		logger.info("begin handler outward request...");
		while (isRuning) {
			try {
				if(systemAccountManager.checkRight4Accounting()){
					TimeUnit.SECONDS.sleep(120);
					continue;
				}
				Object entity = redisService.leftPop(RedisTopics.OUTWARD_REQUEST);
				if (entity == null) {
					Random rand = new Random();
					int sleepTime = (rand.nextInt(5) + 2) * 1000;
					Thread.sleep(sleepTime);
					logger.trace("No data, sleep {}ms", sleepTime);
					continue;
				}

				BizOutwardRequest o = mapper.readValue(String.valueOf(entity), BizOutwardRequest.class);
				if (null == outwardRequestService.findByHandicapAndOrderNo(o.getHandicap(), o.getOrderNo())) {
					/** 1-是否大额出款,先获取系统设置，检查，超出人工审 */
					int largeAmount = 5000;
					try {
						largeAmount = Integer.parseInt(MemCacheUtils.getInstance().getSystemProfile()
								.get(UserProfileKey.OUTDRAW_LIMIT_APPROVE.getValue()));
					} catch (Exception e1) {
						logger.error("获取大额出款系统配置异常，采用默认值5000, key :{}",
								UserProfileKey.OUTDRAW_LIMIT_APPROVE.getValue());
					}
					// 大额出款直接入库 或者 是黑名单
					boolean isSuspectMember = blackListService.isBlackList(o.getToAccount(), o.getToAccountOwner());
					boolean isBigAmount = o.getAmount().intValue() >= largeAmount;
					if (isBigAmount || isSuspectMember) {
						logger.info("大额出款:{} 或者属于黑名单之列:{}. orderNo is {} ", isBigAmount, isSuspectMember,
								o.getOrderNo());
						if (isSuspectMember) {
							o.setReview("该会员信息在黑名单之列,谨慎审核!");
						}
						if (o.getAmount().intValue() >= largeAmount) {
							o.setReview(!ObjectUtils.isEmpty(o.getReview())
									? new StringBuilder(o.getReview()).append(";且大额出款!").toString()
									: "大额出款;");
						}
						outwardRequestService.save(o);
					} else {
						outwardRequestService.autoCheckOutwardRequest(o).subscribe(result -> {
							// 返回空字符串表示审核通过，其它为不通过原因 || allocateOutwardTaskService.checkFirst(result)
							if (StringUtils.isEmpty(result)) {
								logger.debug("{} 自动审核通过", o.getOrderNo());
								outwardRequestService.approve(o, null, StringUtils.trimToEmpty(result),
										o.getMemberCode(), o.getOrderNo());
								logger.info("Outward approved. orderNo is {} ", o.getOrderNo());
							} else {
								logger.debug("{},审核不通过：{}", o.getOrderNo(), result);
								o.setReview(result);
								outwardRequestService.save(o);
								logger.info("Outward audit no-go, orderNo: {}, result: {} ", o.getOrderNo(), result);
							}
						});
					}
					logger.info("Outward[DB] orderNo: {}", o.getOrderNo());
				} else {
					logger.info("Outward error, orderNo: {} already exist.", o.getOrderNo());
				}
			} catch (Exception e) {
				logger.error("", e);
			}
		}
		logger.warn(">>>>>>>>>>> OutwardHandler finished");
	}

	public void stop() {
		isRuning = false;
	}
}
