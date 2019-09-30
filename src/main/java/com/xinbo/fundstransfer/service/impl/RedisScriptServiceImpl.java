package com.xinbo.fundstransfer.service.impl;

import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import com.xinbo.fundstransfer.service.RedisScriptService;

/**
 * common class for getting a new RedisScript instance, you just inject this
 * service wherever you want to get a new instance and call the method
 * RedisScriptServiceImpl#getRedisScriptInstance
 */
@Service
public class RedisScriptServiceImpl implements RedisScriptService {

	public RedisScriptServiceImpl() {

	}

	@Override
	public final <T> RedisScript<T> getRedisScriptInstance(Class<T> retType, String sha, String script) {
		RedisScript<T> redisScript = new DefaultRedisScript<T>( script, retType);
//		RedisScript redisScript = new RedisScript() {
//			@Override
//			public String getSha1() {
//				return sha;
//			}
//
//			@Override
//			public Class getResultType() {
//				return retType.getClass();
//			}
//
//			@Override
//			public String getScriptAsString() {
//				return script;
//			}
//		};
		return redisScript;
	}

}
