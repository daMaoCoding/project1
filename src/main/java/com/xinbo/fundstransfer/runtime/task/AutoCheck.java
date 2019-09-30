package com.xinbo.fundstransfer.runtime.task;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.component.net.http.HttpClient;
import com.xinbo.fundstransfer.component.net.http.RequestBodyParser;
import com.xinbo.fundstransfer.component.spring.SpringContextUtils;
import com.xinbo.fundstransfer.domain.entity.BizOutwardRequest;
import com.xinbo.fundstransfer.domain.enums.UserProfileKey;
import com.xinbo.fundstransfer.domain.pojo.WithdrawAuditInfo;
import com.xinbo.fundstransfer.runtime.MemCacheUtils;
import com.xinbo.fundstransfer.service.OutwardRequestService;
import com.xinbo.fundstransfer.service.RedisService;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * 出款审核，已弃用，老代码
 * 
 *
 *
 */
@Deprecated
public class AutoCheck implements Runnable {
	Logger logger = LoggerFactory.getLogger(this.getClass());
	private BizOutwardRequest bizOutwardRequest;
	private OutwardRequestService outwardRequestService;
	private RedisService redisService;
	private RequestBodyParser requestBodyParser;
	private ObjectMapper mapper = new ObjectMapper();

	public AutoCheck(BizOutwardRequest bizOutwardRequest) {
		this.bizOutwardRequest = bizOutwardRequest;
		outwardRequestService = SpringContextUtils.getBean(OutwardRequestService.class);
		redisService = SpringContextUtils.getBean(RedisService.class);
		requestBodyParser = SpringContextUtils.getBean(RequestBodyParser.class);
	}

	private Observable<Boolean> getOutwardRequestInfo(BizOutwardRequest entity) {
		return Observable.create(new Observable.OnSubscribe<Boolean>() {
			@Override
			public void call(Subscriber<? super Boolean> subscriber) {
				HttpClient
						.getInstance().getIPlatformService(entity.getHandicap()).WithdrawAuditInfo(requestBodyParser
								.general(entity.getHandicap(), entity.getMemberCode(), entity.getOrderNo(), ""))
						.observeOn(Schedulers.io()).subscribe(new Action1<String>() {
							@Override
							public void call(String data) {
								logger.info("Outward WithdrawAuditInfo, orderNo: {}, response: {}", entity.getOrderNo(),
										data);
								WithdrawAuditInfo o = null;
								try {
									ObjectMapper mapper = new ObjectMapper();
									o = mapper.readValue(data, WithdrawAuditInfo.class);
								} catch (Exception e2) {
									logger.error("Outward WithdrawAuditInfo JsonParseException. {}",
											e2.getLocalizedMessage());
								}
								// success
								if (null != o && o.getResult() == 1) {
									boolean approved = true;
									// 1-是否大额出款,先获取系统设置，检查，超出人工审
									int largeAmount = 50000;
									try {
										largeAmount = Integer.parseInt(MemCacheUtils.getInstance().getSystemProfile()
												.get(UserProfileKey.OUTDRAW_LIMIT_APPROVE.getValue()));
									} catch (Exception e1) {
										logger.error("获取大额出款系统配置异常，采用默认值50000, key :{}",
												UserProfileKey.OUTDRAW_LIMIT_APPROVE.getValue());
									}
									StringBuilder sb = new StringBuilder();
//									if (o.getfWithdrawAmount() >= largeAmount) {
//										approved = false;
//										sb.append("大额出款; ");
//									}
									// 2-上次出款银行卡与本次出款银行卡不一致，人工审
									if (!bizOutwardRequest.getToAccount().equals(o.getsUpWithdrawCard())) {
										approved = false;
										sb.append("本次出款与上次出款银行卡不一致; ");
									}
									// 3-首次出款，人工审
									if (o.getiWithdrawStatus() == 1) {
										approved = false;
										sb.append("首次出款; ");
									}
									// 4-检查打码情况，上次出款后余额+本次总入款（包括第三方，公司入款，优惠等）+盈利-本次出款金额=本次出款后余额，不符合规则，人工审
									// if (o.getfUpWithdrawBalance() - o.getfUpWithdrawAmount() != o
									// .getfWithdrawBalance()) {
									// approved = false;
									// sb.append("打码异常，请人工核查; ");
									// }
									// 5-小于等于1倍的打码量，人工审
									if (o.getiWithdrawStatus() == 1) {
										approved = false;
										sb.append("小于等于1倍的打码量; ");
									}
									// 若通过则审核通过
									if (approved) {
//										outwardRequestService.approve(bizOutwardRequest.getId(), 1, "",
//												bizOutwardRequest.getMemberCode(), bizOutwardRequest.getOrderNo());
										logger.info("Outward approved. orderNo is {} ", bizOutwardRequest.getOrderNo());
									} else {
										bizOutwardRequest.setReview(sb.toString());
										outwardRequestService.update(bizOutwardRequest);
										logger.info("Outward audit, orderNo: {}, result: {} ",
												bizOutwardRequest.getOrderNo(), sb.toString());

									}
								}
								subscriber.onNext(true);
							}
						}, new Action1<Throwable>() {
							@Override
							public void call(Throwable e) {
								logger.error("Outward WithdrawAuditInfo error. orderNo: " + entity.getOrderNo(), e);
								subscriber.onError(new RuntimeException("WithdrawAuditInfo fail." + e));
							}
						});
			}
		});
	}

	@Override
	public void run() {
		// 从平台获取审核信息
		getOutwardRequestInfo(bizOutwardRequest).subscribeOn(Schedulers.io()).retryWhen(attempts -> {
			return attempts.zipWith(Observable.range(1, AppConstants.RECONNECTION_TIMES), (n, i) -> i).flatMap(i -> {
				logger.info("Outward >> delay retry get outward audit information by "
						+ i * AppConstants.RECONNECTION_INTERVAL + "second(s)");
				return Observable.timer(i * AppConstants.RECONNECTION_INTERVAL, TimeUnit.SECONDS);
			});
		}).forEach(System.out::println);
	}
}
