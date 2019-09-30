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
 * 单个账号静态初始化
 */
@InitAnnotation(ActionInit.PREFIX_ACTION_INIT + ActionInit.ACTION_INIT_TYPE_StaticIndividual)
public class InitStaticIndividual extends ActionInit {

	@Override
	protected boolean deal(AccountBaseInfo base, InitHandler handler, InitParam param) {
		if (Objects.isNull(base) || Objects.isNull(handler))
			return false;
		BizAccount acc = accDao.findById2(base.getId());
		if (Objects.isNull(acc))
			return false;
		String tar = String.valueOf(acc.getId());
		accountingStringRedisTemplate.boundHashOps(RedisKeys.SYS_BAL_OUT).delete(tar);
		accountingStringRedisTemplate.boundHashOps(RedisKeys.SYS_BAL_IN).delete(tar);
		accountingStringRedisTemplate.boundHashOps(RedisKeys.REAL_BAL_BENCHMARK).delete(tar);
		handler.recordToFail(acc);
		acc.setBalance(acc.getBankBalance());
		accDao.saveAndFlush(acc);
		return true;
	}
}
