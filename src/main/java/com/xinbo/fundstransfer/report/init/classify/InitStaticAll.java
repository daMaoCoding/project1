package com.xinbo.fundstransfer.report.init.classify;

import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalTrans;
import com.xinbo.fundstransfer.report.init.ActionInit;
import com.xinbo.fundstransfer.report.init.InitAnnotation;
import com.xinbo.fundstransfer.report.init.InitHandler;
import com.xinbo.fundstransfer.report.init.InitParam;

import java.util.Set;

/**
 * 所有账号静态初始化
 */
@InitAnnotation(ActionInit.PREFIX_ACTION_INIT + ActionInit.ACTION_INIT_TYPE_StaticAll)
public class InitStaticAll extends ActionInit {

	@Override
	protected boolean deal(AccountBaseInfo base, InitHandler handler, InitParam param) {
		accountingRedisTemplate.delete(RedisTopics.SYS_BAL_RPUSH);
		Set<String> keys = accountingStringRedisTemplate.keys("SYS_BAL:*");
		if (keys != null && keys.size() > 0)
			accountingStringRedisTemplate.delete(keys);
		keys = accountingStringRedisTemplate.keys(SysBalTrans.genPatternSus());
		if (keys != null && keys.size() > 0)
			accountingStringRedisTemplate.delete(keys);
		accountingStringRedisTemplate.delete("SYS_BAL_ALARM");
		accountingStringRedisTemplate.delete(RedisKeys.SYS_BAL_OUT);
		accountingStringRedisTemplate.delete(RedisKeys.SYS_BAL_IN);
		accountingStringRedisTemplate.delete("SYS_INVIST");
		accountingStringRedisTemplate.delete("SYS_BAL_REAL");
		accountingStringRedisTemplate.delete(RedisKeys.REAL_BAL_BENCHMARK);
		accountingStringRedisTemplate.delete(RedisKeys.ACC_SYS_INIT);
		accDao.sysBalInit();
		return true;
	}
}
