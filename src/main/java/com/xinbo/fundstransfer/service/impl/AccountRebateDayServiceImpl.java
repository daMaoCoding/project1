package com.xinbo.fundstransfer.service.impl;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.xinbo.fundstransfer.domain.entity.BizAccountRebateDay;
import com.xinbo.fundstransfer.domain.entity.BizAccountReturnSummary;
import com.xinbo.fundstransfer.domain.repository.AccountRebateDayRepository;
import com.xinbo.fundstransfer.domain.repository.AccountRebateDayReturnSummary;
import com.xinbo.fundstransfer.service.AccountRebateDayService;

@Service
public class AccountRebateDayServiceImpl implements AccountRebateDayService {
	@Autowired
	AccountRebateDayRepository rebateDayDao;
	@Autowired
	AccountRebateDayReturnSummary rebateReturnSummaryDao;
	@PersistenceContext
	private EntityManager entityMgr;

	private static final Cache<String, Object> rebateCache = CacheBuilder.newBuilder().maximumSize(5000)
			.expireAfterWrite(10, TimeUnit.SECONDS).build();

	@Override
	@Transactional
	public BizAccountRebateDay saveAndFlash(BizAccountRebateDay rebateDay) {
		return rebateDayDao.saveAndFlush(rebateDay);
	}

	@Override
	@Transactional
	public BizAccountReturnSummary saveAndFlash(BizAccountReturnSummary rebateDay) {
		return rebateReturnSummaryDao.saveAndFlush(rebateDay);
	}

	/**
	 * 获取累计获得佣金和累计出款流水
	 *
	 * @return
	 */
	public Map<String, Map<String,BigDecimal>> getTotalRebateAndOutFlow() {
		Object object = rebateCache.getIfPresent("TOTAL_REBATE_AND_OUTFLOW");
		if (Objects.isNull(object)) {
			Map<String, Map<String,BigDecimal>> result = new HashMap<>();
			String fmt = "select t1.mobile,sum(case when t.activity_amount is null then t.amount else 0 end) total_rebate,sum(t.total_amount) total_flow from biz_account_rebate_day t,biz_account t1 where t.account = t1.id and t1.flag = '2' and t1.mobile is not null  group by t1.mobile";
			entityMgr.createNativeQuery(fmt).getResultList().forEach(p -> {
				Object[] vals = (Object[]) p;
				Map<String,BigDecimal> total = new HashMap<>();
				total.put("totalRebate",BigDecimal.valueOf((Double) vals[1]));
				total.put("totalFlow",BigDecimal.valueOf((Double) vals[2]));
				result.put((String) vals[0], total);
			});
			rebateCache.put("TOTAL_REBATE_AND_OUTFLOW", result);
			return result;
		} else {
			return (Map<String, Map<String,BigDecimal>>) object;
		}
	}
}
