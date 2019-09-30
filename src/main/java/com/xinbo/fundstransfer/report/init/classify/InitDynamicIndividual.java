package com.xinbo.fundstransfer.report.init.classify;

import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.report.init.ActionInit;
import com.xinbo.fundstransfer.report.init.InitAnnotation;
import com.xinbo.fundstransfer.report.init.InitHandler;
import com.xinbo.fundstransfer.report.init.InitParam;

import java.util.Objects;

/**
 * 指定账号动态初始化
 */
@InitAnnotation(ActionInit.PREFIX_ACTION_INIT + ActionInit.ACTION_INIT_TYPE_DynamicIndividual)
public class InitDynamicIndividual extends ActionInit {

	@Override
	protected boolean deal(AccountBaseInfo base, InitHandler handler, InitParam param) {
		if (Objects.isNull(base) || Objects.isNull(handler))
			return false;
		Integer accId = base.getId();
		boolean ret = handler.initStaticIndividual(base.getId());
		if (!ret)
			return false;
		accountingStringRedisTemplate.boundHashOps(RedisKeys.ACC_SYS_INIT).put(String.valueOf(accId),
				String.valueOf(System.currentTimeMillis()));
		return true;
	}
}
