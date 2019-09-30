package com.xinbo.fundstransfer.service.impl;

import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.service.RedisService;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.*;
import org.springframework.util.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class RedisServiceImpl implements RedisService {

	@Autowired	private StringRedisTemplate                  stringRedisTemplate;
	@Autowired	private RedisTemplate                        redisTemplate;
	@Autowired	private RedisTemplate                        ysfRedisTemplate;
	@Autowired	private StringRedisTemplate                  ysfStringRedisTemplate;
	@Autowired  private RedisTemplate                        jsonRedisTemplate;
	@Autowired  private StringRedisTemplate                  jsonStringRedisTemplate;
	@Autowired  private Redisson                             redisson;
	@Autowired  private RedisTemplate<String, String>        floatRedisTemplate;

	@Override
	public String generateToken(String uid) {
		String token = UUID.randomUUID().toString();
		stringRedisTemplate.boundValueOps(uid).set(token, AppConstants.TOKEN_EXPIRES, TimeUnit.MILLISECONDS);
		return token;
	}

	@Override
	public boolean checkToken(String uid, String token) {
		if (!stringRedisTemplate.hasKey(uid)) {
			return false;
		}
		if (!token.equals(stringRedisTemplate.boundValueOps(uid).get())) {
			return false;
		}
		return true;
	}

	@Override
	public void delete(String key) {
		redisTemplate.delete(key);
		if (key instanceof String) {
			stringRedisTemplate.delete(key);
		}
	}

	@Override
	public void delete(Collection keys) {
		if (CollectionUtils.isEmpty(keys)) {
			return;
		}
		redisTemplate.delete(keys);
		stringRedisTemplate.delete(keys);
	}

	@Override
	public String getString(String key) {
		if (stringRedisTemplate.hasKey(key)) {
			return stringRedisTemplate.boundValueOps(key).get();
		}
		return "";
	}

	@Override
	public void setString(String key, String value) {
		stringRedisTemplate.boundValueOps(key).set(value, AppConstants.TOKEN_EXPIRES, TimeUnit.MILLISECONDS);
	}

	@Override
	public void setString(String key, String value, long expires, TimeUnit timeUnit) {
		stringRedisTemplate.boundValueOps(key).set(value, expires, timeUnit);
	}

	@Override
	public void convertAndSend(String channel, String message) {
		stringRedisTemplate.convertAndSend(channel, message);
	}

	@Override
	public Object getHash(String key, Object field) {
		return redisTemplate.boundHashOps(key).get(field);
	}

	@Override
	public void putHash(String key, Map<?, ?> map) {
		redisTemplate.boundHashOps(key).putAll(map);
	}

	@Override
	public <T> void rightPush(String k, T v) {
		redisTemplate.boundListOps(k).rightPush(v);
	}

	@Override
	public <T> void rightPushAll(String k, T[] v) {
		redisTemplate.boundListOps(k).rightPushAll(k, v);
	}

	@Override
	public <T> List<T> lRang(String k, int from, int to) {
		return (List<T>) redisTemplate.boundListOps(k).range(from, to);
	}

	@Override
	public Object leftPop(String k) {
		return redisTemplate.boundListOps(k).leftPop();
	}

	@Override
	public Object get(String key) {
		return redisTemplate.boundValueOps(key).get();
	}

	@Override
	public void set(String key, Object value, long timeout, TimeUnit unit) {
		redisTemplate.boundValueOps(key).set(value, timeout, unit);
	}

	@Override
	public Set keys(String pattern) {
		return redisTemplate.keys(pattern);
	}

	@Override
	public Object increment(String key, String hashKey, double delta) {
		return getFloatRedisTemplate().opsForHash().increment(key, hashKey, delta);
	}

	@Override
	public RedisTemplate getRedisTemplate() {
		return redisTemplate;
	}

	@Override
	public RedisTemplate getYsfRedisTemplate() {
		return ysfRedisTemplate;
	}

	@Override
	public StringRedisTemplate getYsfStringRedisTemplate(){
		return ysfStringRedisTemplate;
	}



	@Override
	public RedisTemplate<String, String> getFloatRedisTemplate() {
		return floatRedisTemplate;
	}

	@Override
	public StringRedisTemplate getStringRedisTemplate() {
		return stringRedisTemplate;
	}


	//=====================================================================================================
	//-------------------------------------请注意操作的是哪个redis-db,以下内容都为默认db0--------------------
	//=====================================================================================================

	/**
	 * 存在key
	 */
	@Override
	public boolean existsKey(String key) {
		return jsonRedisTemplate.hasKey(key);
	}



	/**
	 * 重名名key，如果newKey已经存在，则newKey的原值被覆盖
	 */
	@Override
	public void renameKey(String oldKey, String newKey) {
		jsonRedisTemplate.rename(oldKey, newKey);
	}




	/**
	 * newKey不存在时才重命名
	 * @return 修改成功返回true
	 */
	@Override
	public boolean renameKeyNotExist(String oldKey, String newKey) {
		return jsonRedisTemplate.renameIfAbsent(oldKey, newKey);
	}



	/**
	 * 删除key
	 */
	@Override
	public void deleteKey(String key) {
		jsonRedisTemplate.delete(key);
	}



	/**
	 * 删除多个key
	 */
	@Override
	public void deleteKey(String... keys) {
		Set<String> kSet = Stream.of(keys).map(k -> k).collect(Collectors.toSet());
		jsonRedisTemplate.delete(kSet);
	}


	/**
	 * 删除Key的集合
	 */
	@Override
	public void deleteKey(Collection<String> keys) {
		Set<String> kSet = keys.stream().map(k -> k).collect(Collectors.toSet());
		jsonRedisTemplate.delete(kSet);
	}



	/**
	 * 设置key的超时
	 */
	@Override
	public void expireKey(String key, long time, TimeUnit timeUnit) {
		jsonRedisTemplate.expire(key, time, timeUnit);
	}


	/**
	 * 指定key在指定的日期过期
	 */
	@Override
	public void expireKeyAt(String key, Date date) {
		jsonRedisTemplate.expireAt(key, date);
	}


	/**
	 * 查询key超时时间
	 */
	@Override
	public long getKeyExpire(String key, TimeUnit timeUnit) {
		return jsonRedisTemplate.getExpire(key, timeUnit);
	}



	/**
	 * 将key设置为永久有效
	 */
	@Override
	public void persistKey(String key) {
		jsonRedisTemplate.persist(key);
	}


	/**
	 *  返回 ZSetOperations
	 */
	@Override
	public <V> ZSetOperations<String,V> getZSetOperations(){
		return jsonRedisTemplate.opsForZSet();
	}



	/**
	 *  返回 setOperations
	 */
	@Override
	public <V> SetOperations<String,V> getSetOperations(){
		return jsonRedisTemplate.opsForSet();
	}


	/**
	 *  返回 listOperations
	 */
	@Override
	public <V> ListOperations<String,V> getListOperations(){
		return jsonRedisTemplate.opsForList();
	}


	/**
	 *  返回 hashOperations
	 */
	@Override
	public <HK,HV>HashOperations<String,HK,HV> getHashOperations(){
		return jsonRedisTemplate.opsForHash();
	}


	/**
	 *  返回 valueOperations
	 */
	@Override
	public <T>ValueOperations<String,T> getValueOperations(){
		return jsonRedisTemplate.opsForValue();
	}


	/**
	 *  返回 FastJson-redisTemplate
	 */
	@Override
	public <T>RedisTemplate<String,T> getJsonRedisTemplate(){
		return jsonRedisTemplate;
	}


	/**
	 *  返回 FastJson-redisTemplate
	 */
	@Override
	public StringRedisTemplate getJsonStringRedisTemplate(){
		return jsonStringRedisTemplate;
	}



	/**
	 * redisson 客户端
	 */
	@Override
	public RedissonClient getRedisson(){
		return  redisson;
	}


	/**
	 * redisson 锁
	 */
	@Override
	public RLock getRedisLock(String lockKeyName){
		if(redisson!=null)
			return redisson.getLock(lockKeyName);
		throw new RuntimeException("Redis错误。");
	}




}
