package com.xinbo.fundstransfer.report.init;

import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizSysLog;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.enums.SysLogType;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.fail.FailHandler;
import com.xinbo.fundstransfer.report.store.StoreHandler;
import com.xinbo.fundstransfer.service.AccountService;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.util.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.*;

@Component
public class InitHandler extends ApplicationObjectSupport {
	private static final Map<String, ActionInit> dealMap = new LinkedHashMap<>();
	@Autowired
	private FailHandler failHandler;
	@Autowired
	private AccountService accSer;
	@Autowired
	private StoreHandler storeHandler;
	@Autowired
	private StringRedisTemplate accountingStringRedisTemplate;

	@PostConstruct
	public void init() {
		Map<String, Object> map = super.getApplicationContext().getBeansWithAnnotation(InitAnnotation.class);
		map.forEach((k, v) -> dealMap.put(k, (ActionInit) v));
	}

	public boolean initStaticAll() {
		return dealMap.get(ActionInit.PREFIX_ACTION_INIT + ActionInit.ACTION_INIT_TYPE_StaticAll).deal(null, this,
				null);
	}

	@Transactional
	public boolean initStaticIndividual(int accId) {
		AccountBaseInfo base = accSer.getFromCacheById(accId);
		return dealMap.get(ActionInit.PREFIX_ACTION_INIT + ActionInit.ACTION_INIT_TYPE_StaticIndividual).deal(base,
				this, null);
	}

	@Transactional
	public void initDynamicIndividual(int accId, SysUser operator, String remark) {
		List<BizSysLog> hisList = storeHandler.findSysLogFromCache(accId);
		if (!CollectionUtils.isEmpty(hisList)) {
			BizSysLog first = hisList.get(0);
			// 如果前一条记录账目已经处于：对账成功状态，不应该初始化
			if (SysBalUtils.radix2(first.getBalance()).compareTo(SysBalUtils.radix2(first.getBankBalance())) == 0)
				return;
			// 如果前一条记录账目已经初始化数据，则：不应该继续初始化
			if (Objects.equals(SysLogType.Init.getTypeId(), first.getType()))
				return;
		}
		BizAccount acc = accSer.getById(accId);
		// 如果该账号不存在，不应该继续初始化
		if (Objects.isNull(acc))
			return;
		long[] sg = storeHandler.init(acc, remark, operator);
		if (Objects.isNull(sg) || sg.length == 2 && sg[0] == 0 && sg[1] == 0)
			return;
		dealMap.get(ActionInit.PREFIX_ACTION_INIT + ActionInit.ACTION_INIT_TYPE_DynamicIndividual)
				.deal(accSer.getFromCacheById(accId), this, null);
	}

	@Transactional
	public void dynamicInitIfNeed(int accId, BigDecimal sysBal, BigDecimal bankBal) {
		if (accId == 0 || Objects.isNull(sysBal) || Objects.isNull(bankBal))
			return;
		String tar = String.valueOf(accId);
		if (SysBalUtils.radix2(sysBal).compareTo(SysBalUtils.radix2(bankBal)) == 0) {
			accountingStringRedisTemplate.boundHashOps(RedisKeys.ACC_SYS_INIT).delete(tar);
		} else {
			String l = (String) accountingStringRedisTemplate.boundHashOps(RedisKeys.ACC_SYS_INIT).get(tar);
			if (StringUtils.isNotBlank(l)) {
				dealMap.get(ActionInit.PREFIX_ACTION_INIT + ActionInit.ACTION_INIT_TYPE_DynamicIndividual)
						.deal(accSer.getFromCacheById(accId), this, null);
			}
		}
	}

	@Transactional
	public boolean initByErr(Long errorId, SysUser operator, String remark) {
		return dealMap.get(ActionInit.PREFIX_ACTION_INIT + ActionInit.ACTION_INIT_TYPE_InitError).deal(null, this,
				new InitParam(operator, remark, errorId));
	}

	@Transactional
	public boolean initByAcc(Integer accId, SysUser operator) {
		Objects.requireNonNull(accId);
		return dealMap.get(ActionInit.PREFIX_ACTION_INIT + ActionInit.ACTION_INIT_TYPE_InitError)
				.deal(accSer.getFromCacheById(accId), this, new InitParam(operator, null, null));
	}

	public void recordToFail(BizAccount acc) {
		if (Objects.nonNull(acc) && Objects.nonNull(acc.getBankBalance()))
			failHandler.record(acc.getId(), acc.getBankBalance());
	}
}
