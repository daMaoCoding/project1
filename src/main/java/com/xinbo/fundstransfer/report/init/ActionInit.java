package com.xinbo.fundstransfer.report.init;

import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.repository.AccountRepository;
import com.xinbo.fundstransfer.domain.repository.SysErrRepository;
import com.xinbo.fundstransfer.domain.repository.SysInvstRepository;
import com.xinbo.fundstransfer.report.store.StoreHandler;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.SysLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

public abstract class ActionInit {

	public static final String PREFIX_ACTION_INIT = "INIT_ACTION_";

	public static final String ACTION_INIT_TYPE_StaticAll = "StaticAll";

	public static final String ACTION_INIT_TYPE_StaticIndividual = "StaticIndividual";

	public static final String ACTION_INIT_TYPE_DynamicAll = "DynamicAll";

	public static final String ACTION_INIT_TYPE_DynamicIndividual = "DynamicIndividual";

	public static final String ACTION_INIT_TYPE_InitError = "InitError";

	@Autowired
	protected AccountService accSer;
	@Autowired
	protected AccountRepository accDao;
	@Autowired
	protected SysErrRepository sysErrDao;
	@Autowired
	protected SysInvstRepository sysInvstDao;
	@Autowired
	protected SysLogService sysLogSer;
	@Autowired
	protected RedisTemplate accountingRedisTemplate;
	@Autowired
	protected StringRedisTemplate accountingStringRedisTemplate;
	@Autowired
	protected StoreHandler storeHandler;

	protected abstract boolean deal(AccountBaseInfo base, InitHandler handler, InitParam param);
}
