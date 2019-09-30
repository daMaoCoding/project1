package com.xinbo.fundstransfer.service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Repository;

public interface RedisService {

	/**
	 * 根据KEY获取一个值
	 * 
	 * @param key
	 * @return
	 */
	Object get(String key);

	/**
	 * 设置一个值
	 * 
	 * @param key
	 * @param value
	 * @param timeout
	 *            有效期
	 * @param timeUnit
	 *            时间单位
	 */
	void set(String key, Object value, long timeout, TimeUnit timeUnit);

	/**
	 * 根据正则获取所有KEY
	 * 
	 * @param pattern
	 * @return
	 */
	Set keys(String pattern);

	/**
	 * 根据KEY获取VALUE
	 * 
	 * @param key
	 * @return
	 */
	String getString(String key);

	/**
	 * 设置一个string type值
	 * 
	 * @param key
	 * @param value
	 */
	void setString(String key, String value);

	/**
	 * 设置一个string type值
	 * 
	 * @param key
	 * @param value
	 * @param expires
	 *            过期时间
	 * @param timeUnit
	 *            时间单位
	 */
	void setString(String key, String value, long expires, TimeUnit timeUnit);

	/**
	 * 删除指定key
	 * 
	 * @param key
	 */
	void delete(String key);

	void delete(Collection keys);

	/**
	 * 为指定的KEY产生一个TOKEN设置有效期并缓存 ,TOKEN的有效期是固定的，即TOKEN产生的时间后7天内有效
	 * 
	 * @param uid
	 * @return token
	 */
	String generateToken(String uid);

	/**
	 * 验证用户TOKEN
	 * 
	 * @param uid
	 * @param token
	 * @return boolean true:valid false:invalid
	 */
	boolean checkToken(String uid, String token);

	/**
	 * 发送topic消息
	 * 
	 * @param channel
	 * @param message
	 */
	void convertAndSend(String channel, String message);

	/**
	 * 获得hash数据类型
	 * 
	 * @param key
	 * @param field
	 * @return
	 */
	Object getHash(String key, Object field);

	/**
	 * 设置hash数据
	 * 
	 * @param key
	 * @param field
	 * @return
	 */
	void putHash(String key, Map<?, ?> map);

	/**
	 * 设置list数据类型
	 * 
	 * @param k
	 * @param v
	 * @return
	 */
	<T> void rightPush(String k, T v);

	<T> void rightPushAll(String k, T[] v);

	/**
	 * 从List中取值
	 */
	<T> List<T> lRang(String k, int from, int to);

	/**
	 * 获取list数据
	 * 
	 * @param key
	 * @return
	 */
	Object leftPop(String k);

	/**
	 * 将哈希字段的浮点值按给定数值增加或减少 Increment value of a hash hashKey by the given delta.
	 * Parameters:key must not be null. hashKey must not be null. delta
	 * 
	 * @return
	 */
	Object increment(String key, String hashKey, double delta);

	/**
	 * 默认 RedisTemplate
	 */
	RedisTemplate getRedisTemplate();


	/**
	 * 云闪付-随机金额的-RedisTemplate
	 */
	RedisTemplate getYsfRedisTemplate();


	/**
	 * 云闪付-随机金额的-StringRedisTemplate
	 */
	StringRedisTemplate getYsfStringRedisTemplate();



	/**
	 * 默认 StringRedisTemplate
	 * 
	 * @return
	 */
	StringRedisTemplate getStringRedisTemplate();

	RedisTemplate<String, String> getFloatRedisTemplate();





	/**
	 * 存在key
	 */
	 boolean existsKey(String key);



	/**
	 * 重名名key，如果newKey已经存在，则newKey的原值被覆盖
	 */
	void renameKey(String oldKey, String newKey) ;


	/**
	 * newKey不存在时才重命名
	 * @return 修改成功返回true
	 */
	boolean renameKeyNotExist(String oldKey, String newKey) ;



	/**
	 * 删除key
	 */
	void deleteKey(String key);



	/**
	 * 删除多个key
	 */
	void deleteKey(String... keys);


	/**
	 * 删除Key的集合
	 */
	void deleteKey(Collection<String> keys);



	/**
	 * 设置key的超时
	 */
	void expireKey(String key, long time, TimeUnit timeUnit) ;

	/**
	 * 指定key在指定的日期过期
	 */
	void expireKeyAt(String key, Date date) ;


	/**
	 * 查询key超时时间
	 */
	long getKeyExpire(String key, TimeUnit timeUnit) ;



	/**
	 * 将key设置为永久有效
	 */
	void persistKey(String key) ;


	/**
	 *  返回 ZSetOperations
	 */
	<V> ZSetOperations<String,V> getZSetOperations();


	/**
	 *  返回 setOperations
	 */
	<V> SetOperations<String,V> getSetOperations();


	/**
	 *  返回 listOperations
	 */
	<V> ListOperations<String,V> getListOperations();


	/**
	 *  返回 hashOperations
	 */
	<HK,HV>HashOperations<String,HK,HV> getHashOperations();


	/**
	 *  返回 valueOperations
	 */
	<T>ValueOperations<String,T> getValueOperations();


	/**
	 *  返回 FastJson-redisTemplate
	 */
	<T>RedisTemplate<String,T> getJsonRedisTemplate();

	/**
	 * 返回 FastJson-StringRedisTemplate
	 */
	StringRedisTemplate getJsonStringRedisTemplate();


	/**
	 * redisson 客户端
	 */
	RedissonClient getRedisson();


	/**
	 * redisson 锁
	 */
	RLock getRedisLock(String lockKeyName);


}
