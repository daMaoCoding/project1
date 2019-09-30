/**
 * 
 */
package com.xinbo.fundstransfer.unionpay.ysf.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.Striped;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.ResponseData;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import com.xinbo.fundstransfer.service.RedisService;
import com.xinbo.fundstransfer.unionpay.ysf.entity.YSFQRrequestEntity;
import com.xinbo.fundstransfer.unionpay.ysf.entity.YSFQrCodeEntity;
import com.xinbo.fundstransfer.unionpay.ysf.service.YSFService;
import com.xinbo.fundstransfer.unionpay.ysf.util.YSFLocalCacheUtil;
import com.xinbo.fundstransfer.utils.randutil.InAccountRandUtil;
import com.xinbo.fundstransfer.utils.randutil.JedisLock;

import lombok.extern.slf4j.Slf4j;

/**
 * @author blake
 *
 */
@Slf4j
@Component
public class YSFQrcodeGenServiceImpl {

	@Autowired @Lazy
	private YSFService ySFService;
	
	@Autowired
	RedisService redisService;
	
	@Autowired
	private YSFLocalCacheUtil ySFLocalCacheUtil;
	
	@Value("${funds.transfer.ysf.qr.time-out:60000}")
	private Integer																		timeOut;
	private Striped<Lock>																lockStripes					= Striped.lock(1024);
	private Cache<String, CompletableFuture<YSFQrCodeEntity>>							resultGenYunSfQrCode;
	private Cache<String, LinkedBlockingQueue<Map<String, Object>>>						commandGenYunSfQrCode;
	private static final Integer														WAITING_TIMEOUT				= 1000;													// 等待时间，如果1S获取不到锁，就获取失败
	private static final Integer														EXPIRE_TIMEOUT				= 60000;												// 如果1分钟都没有处理完，redis锁也会失效
	private static String																GENYUNSFQRCODE_DEVICE_LOCK	= "zrand:genyunsfqrcode:lock:%s";
	//批量生成云闪付二维码
	private Cache<Object,Object>    currentYunSfArrQrCodeCache;
	private Cache<Object,Object>    countYunSfArrQrCodeCache;	
	
	@PostConstruct
	public void initialize() {
		//批量生成云闪付二维码
		CacheBuilder<Object, Object> builder23 = CacheBuilder.newBuilder().expireAfterWrite(timeOut, TimeUnit.MILLISECONDS);
		currentYunSfArrQrCodeCache = builder23.build();
		CacheBuilder<Object, Object> builder24 = CacheBuilder.newBuilder().expireAfterWrite(timeOut, TimeUnit.MILLISECONDS);
		countYunSfArrQrCodeCache = builder24.build();
		// 生成云闪付二维码
		CacheBuilder<Object, Object> builder7 = CacheBuilder.newBuilder().expireAfterWrite(timeOut, TimeUnit.MILLISECONDS);
		commandGenYunSfQrCode = builder7.build();
		CacheBuilder<Object, Object> builder77 = CacheBuilder.newBuilder().expireAfterWrite(timeOut, TimeUnit.MILLISECONDS);
		resultGenYunSfQrCode = builder77.build();
	}
	
	public YSFQrCodeEntity genYunSfQrCode(Integer handicapId,String bankAccount, String expectAmounts) throws InterruptedException, TimeoutException, ExecutionException {
		String id = String.format("%s_%s",bankAccount, expectAmounts);
		Lock lock = lockStripes.get(id);
		try {
			lock.lock();
			CompletableFuture<YSFQrCodeEntity> completableFuture = new CompletableFuture<>();
			resultGenYunSfQrCode.put(id, completableFuture);

			LinkedBlockingQueue<Map<String, Object>> values = commandGenYunSfQrCode.getIfPresent(id);
			if (ObjectUtils.isEmpty(values)) {
				values = new LinkedBlockingQueue<Map<String, Object>>();
				commandGenYunSfQrCode.put(id, values);
			}
			HashMap<String, Object> param = new HashMap<String, Object>();
			param.put("id", id);
			param.put("bankAccount", bankAccount);
			param.put("expectAmounts", expectAmounts);
			param.put("handicapId", handicapId);
			if (!values.offer(param, 1, TimeUnit.SECONDS)) {
				log.error("生成银联云闪付二维码失败: {}", id);
			}
			return completableFuture.get(ySFLocalCacheUtil.getQrProcessTimeOutSeconds()*1000, TimeUnit.MILLISECONDS);
		} finally {
			lock.unlock();
		}
	}
	
	@Scheduled(fixedDelay = 100)
	public void cacheConsume() {
		execute(commandGenYunSfQrCode, GENYUNSFQRCODE_DEVICE_LOCK, (id,handicapId,value, bankAccount) -> {
			CompletableFuture<YSFQrCodeEntity> future = resultGenYunSfQrCode.getIfPresent(id);
			String bankCard = (String) value.get("bankAccount");
			BigDecimal money = new BigDecimal(value.get("expectAmounts").toString());
			try {
				log.info("下发要求云闪付银行卡号{}生成金额为{}的二维码",bankAccount,money);
				YSFQrCodeEntity rs = getYunSfOnQrCode(handicapId,bankCard, money);
				if (!ObjectUtils.isEmpty(future)) {
					future.complete(rs);
				} else {
					log.info("下发要求云闪付银行卡号{}生成金额为{}的二维码出错: 返回结果超过 {} ms",bankAccount,money,timeOut);
				}
			} catch (Exception e) {
				future.completeExceptionally(e);
			}
		});
	}

	private void execute(Cache<String, LinkedBlockingQueue<Map<String, Object>>> cache, String lockName, DeviceCallback callback) {
		for (Entry<String, LinkedBlockingQueue<Map<String, Object>>> entry : cache.asMap().entrySet()) {
			String bankAccount = entry.getKey();
			LinkedBlockingQueue<Map<String, Object>> queue = entry.getValue();
			if (ObjectUtils.isEmpty(bankAccount) || ObjectUtils.isEmpty(queue) || queue.size() <= 0) {
				continue;
			}
			new Thread(() -> {
				StringRedisTemplate jedis = redisService.getYsfStringRedisTemplate();
				try {
					JedisLock lock = new JedisLock(jedis, String.format(lockName, bankAccount), WAITING_TIMEOUT, EXPIRE_TIMEOUT);
					try {
						if (lock.acquire()) {
							List<Map<String, Object>> values = new ArrayList<Map<String, Object>>();
							queue.drainTo(values);
							for (Map<String, Object> value : values) {
								String id = (String) value.get("id");
								Integer handicapId = (Integer) value.get("handicapId");
								callback.doDevice(id, handicapId, value, bankAccount);
							}
						}
					} finally {
						lock.release();
					}
				} catch (InterruptedException ex) {
					log.error("execute 异步下发时产生异常",ex);
				} 
			}).start();
		}
	}

	/**app生成云闪付二维码
     * @param bankCard 银行卡号
     * @param money   金额 
     * @return
     */
	private YSFQrCodeEntity getYunSfOnQrCode(Integer handicapId, String bankCard, BigDecimal money) throws InterruptedException {
		Assert.isTrue(bankCard!=null,"银行卡号不能为空");
        Assert.isTrue(!ObjectUtils.isEmpty(money),"生成金额不能为空");

        log.info("云闪付银行卡 {} 的 生产金额未{}的二维码,(-->开始)发送消息....", bankCard,money);
		CountDownLatch latch = new CountDownLatch(1);
		String expectAmount = InAccountRandUtil.moneyFormat(money);
		String key = String.format("%s_%s",bankCard, expectAmount);
		countYunSfArrQrCodeCache.put(key, latch);
		 
		YSFQRrequestEntity requestEntity = new YSFQRrequestEntity();
		requestEntity.setBankAccount(bankCard);
		requestEntity.setExpectAmounts(expectAmount);
		requestEntity.setHandicapId(handicapId);
		Map<String,String> ysfAccountPwd = ySFService.findOtherAccountByBankAccount(bankCard);
		if(ObjectUtils.isEmpty(ysfAccountPwd)) {
			log.error("未能根据云闪付绑定的银行卡号查询到云闪付账号");
			return null;
		}
		Entry<String, String> ysfAccountPwdEntry = ysfAccountPwd.entrySet().iterator().next();
		requestEntity.setYsfAccount(ysfAccountPwdEntry.getKey());
		requestEntity.setLoginPWD(ysfAccountPwdEntry.getValue());
		ResponseData<?> appResponse = ySFService.call4GenerateQRs(requestEntity);
		if(GeneralResponseData.ResponseStatus.SUCCESS.getValue() ==appResponse.getStatus()) {
			log.info("云闪付银行卡 {} 的 生产金额未{}的二维码,发送消息(完成<--)", bankCard,money);
			latch.await(timeOut, TimeUnit.MILLISECONDS);
			YSFQrCodeEntity qrCodeEntity = (YSFQrCodeEntity) currentYunSfArrQrCodeCache.getIfPresent(key);
			currentYunSfArrQrCodeCache.invalidate(key);
			if(ObjectUtils.isEmpty(qrCodeEntity)){
				return null;
			}
			return qrCodeEntity;
		}else {
			log.error("云闪付银行卡 {} 的 生产金额未{}的二维码,未能成功发送消息，错误信息:{}", bankCard,money,appResponse.getMessage());
			return null;
		}
	}
	
	public void onYunSfOnQrCode(YSFQrCodeEntity qrCodeEntity) throws InterruptedException {
        log.info("redis 收到云闪付二维码消息 ：{})", ObjectMapperUtils.serialize(qrCodeEntity));
        String key = String.format("%s_%s", qrCodeEntity.getBindedBankAccount(),InAccountRandUtil.moneyFormat(qrCodeEntity.getMoney()));
		currentYunSfArrQrCodeCache.put(key,qrCodeEntity);
		Object latch = countYunSfArrQrCodeCache.getIfPresent(key);
		if(!ObjectUtils.isEmpty(latch)) {
			((CountDownLatch)latch).countDown();
		}
	}
	
	@FunctionalInterface
	interface DeviceCallback {
		void doDevice(String id,Integer handicapId, Map<String, Object> value, String bankAccount);
	}
	
	
}
