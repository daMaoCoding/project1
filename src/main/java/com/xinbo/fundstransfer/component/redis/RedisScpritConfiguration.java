package com.xinbo.fundstransfer.component.redis;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

/**
 * Created by on 2018/9/1.
 */
@Configuration
public class RedisScpritConfiguration {
	// -------------------第三方账号提现 锁定解锁目标账号 查询锁定账号 脚本加载 start ---------
	@Bean
	@Qualifier(value = "lockScript")
	public RedisScript<Long> lockScript() {
		DefaultRedisScript redisScript = new DefaultRedisScript();
		redisScript.setScriptSource(
				new ResourceScriptSource(new ClassPathResource("luascripts/lock4ThirdInAccountDrawScript.lua")));
		redisScript.setResultType(Long.class);
		return redisScript;
	}

	@Bean
	@Qualifier(value = "thirdDrawTasklockScript")
	public RedisScript<Long> thirdDrawTasklockScript() {
		DefaultRedisScript redisScript = new DefaultRedisScript();
		redisScript.setScriptSource(
				new ResourceScriptSource(new ClassPathResource("luascripts/thirdDrawTaskLockScript.lua")));
		redisScript.setResultType(Long.class);
		return redisScript;
	}

	@Bean
	@Qualifier(value = "unlockScript")
	public RedisScript<Long> unlockScript() {
		DefaultRedisScript redisScript = new DefaultRedisScript();
		redisScript.setScriptSource(
				new ResourceScriptSource(new ClassPathResource("luascripts/unlock4ThirdInAccountDrawScript.lua")));
		redisScript.setResultType(Long.class);
		return redisScript;
	}

	@Bean
	@Qualifier(value = "searchAllLockedAccIdsScript")
	public RedisScript<String> searchAllLockedAccIdsScript() {
		DefaultRedisScript redisScript = new DefaultRedisScript();
		redisScript.setScriptSource(
				new ResourceScriptSource(new ClassPathResource("luascripts/searchAllLockedAccoutIdsScript.lua")));
		redisScript.setResultType(String.class);
		return redisScript;
	}

	@Bean
	@Qualifier(value = "searchLockedAccIdsByCurrentUserScript")
	public RedisScript<String> searchLockedAccIdsByCurrentUserScript() {
		DefaultRedisScript redisScript = new DefaultRedisScript();
		redisScript.setScriptSource(
				new ResourceScriptSource(new ClassPathResource("luascripts/searchLockAccIdsByUserIdFrmIdScript.lua")));
		redisScript.setResultType(String.class);
		return redisScript;
	}

	@Bean
	@Qualifier(value = "searchLockerByAccountIdScript")
	public RedisScript<String> searchLockerByAccountIdScript() {
		DefaultRedisScript redisScript = new DefaultRedisScript();
		redisScript.setScriptSource(
				new ResourceScriptSource(new ClassPathResource("luascripts/searchLockerByAccountIdScript.lua")));
		redisScript.setResultType(String.class);
		return redisScript;
	}

	/** ---------------------------入款卡下发 相关脚本---------------------------- */
	@Bean
	@Qualifier(value = "updateAccountRealBalanceScript")
	public RedisScript<String> updateAccountRealBalanceScript() {
		DefaultRedisScript redisScript = new DefaultRedisScript();
		redisScript.setScriptSource(
				new ResourceScriptSource(new ClassPathResource("luascripts/updateAccountRealBalaceScript.lua")));
		redisScript.setResultType(String.class);
		return redisScript;
	}

	/**
	 * ---------------------------新公司入款 1.5.8 接口 选择最近最少使用的卡返回
	 * 相关脚本----------------------------
	 */
	@Bean
	@Qualifier(value = "selectAccountToPayScript")
	public RedisScript<String> selectAccountToPayScript() {
		DefaultRedisScript redisScript = new DefaultRedisScript();
		redisScript.setScriptSource(
				new ResourceScriptSource(new ClassPathResource("luascripts/selectAccountToPayScript.lua")));
		redisScript.setResultType(String.class);
		return redisScript;
	}
}
