package com.xinbo.fundstransfer.report.init.classify;

import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.report.init.ActionInit;
import com.xinbo.fundstransfer.report.init.InitAnnotation;
import com.xinbo.fundstransfer.report.init.InitHandler;
import com.xinbo.fundstransfer.report.init.InitParam;
import org.apache.shiro.util.CollectionUtils;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 所有账号动态初始化
 */
@InitAnnotation(ActionInit.PREFIX_ACTION_INIT + ActionInit.ACTION_INIT_TYPE_DynamicAll)
public class InitDynamicAll extends ActionInit {

	@Override
	protected boolean deal(AccountBaseInfo base, InitHandler handler, InitParam param) {
		if (Objects.isNull(handler))
			return false;
		// 删除所有系统余额 历史痕迹
		boolean ret = handler.initStaticAll();
		if (!ret)
			return false;
		// 获取时时对账账号
		List<SearchFilter> filterToList = new ArrayList<>();
		// 需要时时对账的账号分类：入款卡，出款卡，备用卡，下发卡；状态：非冻结，删除状态
		List<Integer> typeInList = new ArrayList<>();
		typeInList.add(AccountType.InBank.getTypeId());
		typeInList.add(AccountType.OutBank.getTypeId());
		typeInList.add(AccountType.ReserveBank.getTypeId());
		typeInList.add(AccountType.BindAli.getTypeId());
		typeInList.add(AccountType.BindWechat.getTypeId());
		typeInList.add(AccountType.ThirdCommon.getTypeId());
		typeInList.add(AccountType.BindCommon.getTypeId());
		filterToList.add(new SearchFilter("type", SearchFilter.Operator.IN, typeInList.toArray()));
		filterToList.add(new SearchFilter("status", SearchFilter.Operator.NOTEQ, AccountStatus.Freeze.getStatus()));
		filterToList.add(new SearchFilter("status", SearchFilter.Operator.NOTEQ, AccountStatus.Delete.getStatus()));
		SearchFilter[] filterToArray = filterToList.toArray(new SearchFilter[filterToList.size()]);
		List<Integer> idList = accSer.findAccountIdList(filterToArray);
		if (CollectionUtils.isEmpty(idList))
			return true;
		String l = String.valueOf(System.currentTimeMillis());
		for (Integer id : idList)
			accountingStringRedisTemplate.boundHashOps(RedisKeys.ACC_SYS_INIT).put(String.valueOf(id), l);
		return true;
	}
}
