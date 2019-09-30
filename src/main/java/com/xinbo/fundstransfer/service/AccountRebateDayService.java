package com.xinbo.fundstransfer.service;

import com.xinbo.fundstransfer.domain.entity.BizAccountRebateDay;
import com.xinbo.fundstransfer.domain.entity.BizAccountReturnSummary;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Created by Owner on 2018/11/28.
 */
public interface AccountRebateDayService {

	BizAccountRebateDay saveAndFlash(BizAccountRebateDay rebateDay);

	BizAccountReturnSummary saveAndFlash(BizAccountReturnSummary rebateDay);

	Map<String,Map<String,BigDecimal>> getTotalRebateAndOutFlow();
}
