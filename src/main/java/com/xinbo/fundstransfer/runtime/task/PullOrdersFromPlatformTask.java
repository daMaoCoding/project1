package com.xinbo.fundstransfer.runtime.task;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.net.http.RequestBodyParser;
import com.xinbo.fundstransfer.component.net.http.v2.HttpClientNew;
import com.xinbo.fundstransfer.component.net.http.v2.PlatformServiceApi;
import com.xinbo.fundstransfer.component.spring.SpringContextUtils;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.BizOutwardRequest;
import com.xinbo.fundstransfer.domain.enums.OutwardRequestStatus;
import com.xinbo.fundstransfer.service.AllocateIncomeAccountService;
import com.xinbo.fundstransfer.service.HandicapService;
import com.xinbo.fundstransfer.service.OutwardRequestService;
import com.xinbo.fundstransfer.service.RedisService;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by Owner on 2018/5/15. 5 分钟从新平台自动同步出入款订单 对出款成功但是确认失败的订单发起通知
 */
@Slf4j
public class PullOrdersFromPlatformTask implements Runnable {
	private AllocateIncomeAccountService allocateIncomeAccountService;
	private RequestBodyParser requestBodyParser;
	private HandicapService handicapService;
	private OutwardRequestService outwardRequestService;
	private RedisService redisService;
	// 同步出款任务开关key
	private static final String SYNCH_SWITCH_KEY = "synchOrdersSwitchKey";

	public PullOrdersFromPlatformTask() {
		allocateIncomeAccountService = SpringContextUtils.getBean(AllocateIncomeAccountService.class);
		requestBodyParser = SpringContextUtils.getBean(RequestBodyParser.class);
		handicapService = SpringContextUtils.getBean(HandicapService.class);
		outwardRequestService = SpringContextUtils.getBean(OutwardRequestService.class);
		redisService = SpringContextUtils.getBean(RedisService.class);
	}

	@Override
	public void run() {
		try {
			boolean flag = allocateIncomeAccountService.checkHostRunRight();
			if (!flag) {
				log.debug("无权限执行2分钟同步出款订单和通知失败确认:{}", flag);
				return;
			}
			StringRedisTemplate redisTemplate = redisService.getStringRedisTemplate();
			boolean key = redisTemplate.hasKey(SYNCH_SWITCH_KEY), runAble = false;
			if (key) {
				// val 1 定时任务执行 2 定时任务不执行
				String val = redisTemplate.opsForValue().get(SYNCH_SWITCH_KEY);
				if (StringUtils.isNotBlank(val)) {
					if ("1".equals(val))
						runAble = true;
					else
						return;
				}
			}
			if (runAble) {
				// 同步出款订单
				synchOutReqOrdersByType2();
			} else {
				log.debug("同步出款订单定时任务是否开启:{}", runAble);
			}
			// 确认失败的出款订单,5分钟重新发起确认
			notifyPlatformForAckFailedOrders();
		} catch (Exception e) {
			log.error("定时2分钟同步入款出款订单失败 e :", e);
		}
	}

	// 向平台发起同步未同步过来的出款订单
	private void synchOutReqOrdersByType2() {
		List<BizHandicap> list = handicapService.findByStatusEqual(1);
		if (list != null && list.size() > 0) {
			list.stream().filter(p -> (!p.getCode().equals("MANILA") && !p.getCode().equals("TAIWAN")))
					.forEach(p -> HttpClientNew.getInstance().getPlatformServiceApi()
							.sync(requestBodyParser.buildSyncRequestBody(p.getCode(), 2))
							.subscribe(data -> log.info("同步盘口：{}的出款订单,结果:{}", p.getCode(), data),
									e -> log.error("同步盘口：{}出款订单,失败:{}", p.getCode(), e)));
		}
	}

	// 确认失败的出款5分钟重新发起确认
	private void notifyPlatformForAckFailedOrders() throws InterruptedException {
		List<BizOutwardRequest> list1 = outwardRequestService.findOrdersForNotify();
		if (list1 != null && list1.size() > 0) {
			PlatformServiceApi platformServiceApi = HttpClientNew.getInstance().getPlatformServiceApi();
			for (int i = 0; i < list1.size(); i++) {
				Thread.sleep(100);
				BizOutwardRequest req = list1.get(i);
				String handicap = handicapService.findFromCacheById(req.getHandicap()).getCode();
				// 过滤旧平台的单子
				if (!Arrays.asList("cp699,wcp,ysc".split(",")).contains(handicap)) {
					Date now = new Date();
					platformServiceApi
							.WithdrawalAck(requestBodyParser.buildRequestBody(handicap, req.getOrderNo(), "系统定时确认"))
							.subscribe(res -> {
								log.info("系统2分钟发起确认:orderNo:{},Message:{},Status:{}", req.getOrderNo(),
										res.getMessage(), res.getStatus());
								if (res.getStatus() == 1) {
									req.setStatus(OutwardRequestStatus.Acknowledged.getStatus());
									if (req.getRemark().length() < 950) {
										req.setRemark(CommonUtils.genRemark(req.getRemark(),
												"通知平台响应成功:" + res.getMessage(), now, "系统定时确认"));
									}

								} else {
									// remark 字段 1000 个字符长度大约只能存950左右的字符
									if (req.getRemark().length() < 950) {
										String newRemark = CommonUtils.genRemark(req.getRemark(),
												"通知平台响应失败:" + res.getMessage(), now, "系统定时确认");
										if (newRemark.length() < 980) {
											req.setRemark(newRemark);
										}
									}
								}
								req.setUpdateTime(now);
								outwardRequestService.update(req);
							});
				}
			}
		}
	}
}
