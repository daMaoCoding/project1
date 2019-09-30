package com.xinbo.fundstransfer.restful;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.xinbo.fundstransfer.component.net.http.HttpClient;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.service.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * 接收mysql請求，通知平台，起缓冲作用，快速响应mysql的请求
 * 
 * 
 *
 */
@RestController
@RequestMapping("/r/bridge")
public class BridgePlatformController extends BaseController {
	private static final Logger log = LoggerFactory.getLogger(BridgePlatformController.class);
	@Autowired
	private RedisService redisService;

	/**
	 * 出款全部完成，通知平台
	 * 
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping(value = "/complete", method = { RequestMethod.GET, RequestMethod.POST })
	public String complete(@RequestParam(value = "id", required = true) int outwardId) {

		HttpClient.getInstance().getITestService().greeting("Test").observeOn(Schedulers.io())
				.subscribe(new Action1<SimpleResponseData>() {
					@Override
					public void call(SimpleResponseData data) {
						log.info(data.getMessage());
						redisService.convertAndSend(RedisTopics.BROADCAST, data.getMessage());
					}
				}, new Action1<Throwable>() {
					@Override
					public void call(Throwable e) {
						log.error("BridgePlatform error.", e);
					}
				});

		return "PONG";

	}

}
