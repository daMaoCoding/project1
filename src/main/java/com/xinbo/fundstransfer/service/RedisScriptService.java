package com.xinbo.fundstransfer.service;

import org.springframework.data.redis.core.script.RedisScript;

public interface RedisScriptService {
	  <T> RedisScript<T> getRedisScriptInstance(Class<T> retType, String sha, String script);
}
