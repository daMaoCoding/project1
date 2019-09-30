/**
 * 
 */
package com.xinbo.fundstransfer.runtime.task;

import java.util.List;

import com.xinbo.fundstransfer.report.SystemAccountManager;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.xinbo.fundstransfer.component.net.http.v2.HttpClientNew;
import com.xinbo.fundstransfer.component.spring.SpringContextUtils;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.service.AllocateIncomeAccountService;
import com.xinbo.fundstransfer.service.HandicapService;
import com.xinbo.fundstransfer.service.RedisService;

import lombok.extern.slf4j.Slf4j;

/**
 * 定时获取平台各业主（盘口）的支付回调地址任务
 * 
 * @author blake
 *
 */
@Slf4j
public class HandicapNotifyUrlTask implements Runnable {
	
	/**
	 * 各盘口支付回调地址缓存key
	 */
	public static final String HANDICAP_NOTIFY_URL="HANDICAP_NOTIFY_URL";
	private AllocateIncomeAccountService allocateIncomeAccountService;
	private HandicapService handicapService;
	private RedisService redisService;
	private SystemAccountManager systemAccountManager;
	
	public HandicapNotifyUrlTask() {
		this.allocateIncomeAccountService = SpringContextUtils.getBean(AllocateIncomeAccountService.class);
		this.handicapService = SpringContextUtils.getBean(HandicapService.class);
		this.redisService = SpringContextUtils.getBean(RedisService.class);
		this.systemAccountManager = SpringContextUtils.getBean(SystemAccountManager.class);
	}

	@Override
	public void run() {
		boolean flag = allocateIncomeAccountService.checkHostRunRight();
		if (!flag) {
			log.debug("无权限执行同步盘口第三方出款（代付）通道配置:{}", flag);
			return;
		}
		if(systemAccountManager.checkRight4Accounting()){
			return;
		}
		synchHandicapNotifyUr();
	}

	// 向平台发起同步未同步过来的入款订单
	private void synchHandicapNotifyUr() {
		if(ObjectUtils.isEmpty(redisService)) {
			log.error("尝试进行盘口支付地址回调同步任务，但是获取到的redisService为空，任务停止");
			return;
		}
		List<BizHandicap> list = handicapService.findByStatusEqual(1);
		if (list != null && list.size() > 0) {
			list.stream().filter(p -> (!p.getCode().equals("MANILA") && !p.getCode().equals("TAIWAN"))).forEach(p -> {
				try {
					HttpClientNew.getInstance().getPlatformServiceApi().getNotifyUrlByhandicap(p.getCode())
							.subscribe(data -> {
								log.debug("同步盘口：{}的支付回调地址,结果:{}", p.getCode(), data);
								if(data.getStatus()==1) {
									log.debug("获取到{}的支付回调地址为{}",p.getCode(),data.getMessage());
									if(!StringUtils.isEmpty(data.getMessage())) {
										log.debug("更新缓存{}中key={}的value={}",HANDICAP_NOTIFY_URL,p.getCode(),data.getMessage());
										this.redisService.getStringRedisTemplate().boundHashOps(HANDICAP_NOTIFY_URL).put(p.getCode(), data.getMessage());
									}else {
										log.debug("更新缓存{}删除key={}",HANDICAP_NOTIFY_URL,p.getCode());
										this.redisService.getStringRedisTemplate().boundHashOps(HANDICAP_NOTIFY_URL).delete(p.getCode());
									}
								}
							}, e -> {
								log.error("同步盘口：{}的支付回调地址，将不更新缓存中盘口的回调地址（如果缓存中存在）,失败:{}", p.getCode(), e.getStackTrace());
							});
				} catch (Exception e) {
					log.error("尝试同步{}的支付回调地址时产生异常，异常信息：{}",p.getCode(),e);
				}
			});
		}
	}

}
