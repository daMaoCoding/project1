package com.xinbo.fundstransfer.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.xinbo.fundstransfer.domain.entity.BizThirdLog;

/**
 * 第三方流水
 */
public interface BizThirdLogService {

	Page<BizThirdLog> page(String orderNo, String channel, String startTime, String endTime, String startMoney,
			String endMoney, List<Integer> fromAccountIdList, PageRequest pageRequest);

	Page<BizThirdLog> pageNoCount(String orderNo, String channel, String startTime, String endTime, String startMoney,
			String endMoney, List<Integer> fromAccountIdList, PageRequest pageRequest);

	List sum(String orderNo, String channel, String startTime, String endTime, String startMoney, String endMoney,
			List<Integer> fromAccountIdList, PageRequest pageRequest);
}
