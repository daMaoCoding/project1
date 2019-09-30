package com.xinbo.fundstransfer.service.impl;

import java.util.concurrent.TimeUnit;

import com.xinbo.fundstransfer.service.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.xinbo.fundstransfer.component.spring.SpringContextUtils;

/**
 * 系统架构是单点(多点集群的话获取锁和释放锁需要LUA脚本)redis主,两台web服务器，所以RUD操作会发生并发.
 */
public class RedisDistributedLockUtils {

	private static Logger logger = LoggerFactory.getLogger(RedisDistributedLockUtils.class);
	private static StringRedisTemplate redisTemplate;
	/**
	 * 分布式锁的键值
	 */
	private String lockKey; // 锁的键值(确保key不一样，防止误删除)
	private int expireMilliseconds = 10 * 1000; // 锁超时，防止线程在入锁以后，无限的执行等待
	private int timeoutMilliseconds = 10 * 1000; // 锁等待，防止线程饥饿
	private volatile boolean locked = false; // 是否已经获取锁

	/**
	 * 获取指定键值的锁,同时设置获取锁超时时间和锁过期时间 注意设置超时时间不能太长
	 * 
	 * @param lockKey
	 *            锁的键值
	 * @param timeoutMsecs
	 *            获取锁超时时间 单位毫秒
	 * @param expireMsecs
	 *            锁失效时间 单位毫秒
	 */
	public RedisDistributedLockUtils(String lockKey, int timeoutMsecs, int expireMsecs) {
		this.lockKey = assembleKey(lockKey);
		this.timeoutMilliseconds = timeoutMsecs;
		this.expireMilliseconds = expireMsecs;
	}

	private static String assembleKey(String lockKey) {
		return String.format("lock_%s", lockKey);
	}

	/**
	 * 获取锁 返回true 获取不到 返回 false
	 * 
	 * @return
	 */
	public synchronized boolean acquireLock() {
		int timeout = timeoutMilliseconds;
		if (redisTemplate == null) {
			redisTemplate = SpringContextUtils.getBean(RedisServiceImpl.class).getStringRedisTemplate();
		}
		try {
			while (!locked && timeout >= 0) {
				// 如果没获取到锁，并且获取锁没超时
				long expires = System.currentTimeMillis() + expireMilliseconds + 1;
				String expiresStr = String.valueOf(expires); // 锁到期时间

				if (redisTemplate.opsForValue().setIfAbsent(lockKey, expiresStr)) {
					redisTemplate.expire(lockKey, expireMilliseconds, TimeUnit.MILLISECONDS);
					locked = true;
					return true;
				}

				String currentValueStr = redisTemplate.opsForValue().get(lockKey); // redis里的时间
				if (currentValueStr != null && Long.parseLong(currentValueStr) < System.currentTimeMillis()) {
					// 判断是否为空，不为空的情况下，如果被其他线程设置了值，则第二个条件判断是过不去的
					// lock is expired

					String oldValueStr = redisTemplate.opsForValue().getAndSet(lockKey, expiresStr);
					// 获取上一个锁到期时间，并设置现在的锁到期时间，
					// 只有一个线程才能获取上一个线上的设置时间，因为redis的getSet是同步的
					if (oldValueStr != null && oldValueStr.equals(currentValueStr)) {
						// 如这个时候，多个线程恰好都到了这里，但是只有一个线程的设置值和当前值相同，他才有权利获取锁
						// lock acquired
						locked = true;
						return true;
					}
				}
				timeout -= 1000;
				Thread.sleep(100);
			}
		} catch (Exception e) {
			logger.error("release lock due to error", e);
		}
		return false;
	}

	/**
	 * 释放锁
	 */
	public void releaseLock() {
		if (redisTemplate == null) {
			redisTemplate = SpringContextUtils.getBean(RedisServiceImpl.class).getStringRedisTemplate();
		}
		try {
			if (locked) {
				String currentValueStr = redisTemplate.opsForValue().get(lockKey); // redis里的时间
				// 校验是否超过有效期，如果不在有效期内，那说明当前锁已经失效，不能进行删除锁操作
				if (currentValueStr != null && Long.parseLong(currentValueStr) > System.currentTimeMillis()) {
					redisTemplate.delete(lockKey);
					locked = false;
				}
			}
		} catch (Exception e) {
			logger.error("release lock due to error", e);
		}
	}
}
