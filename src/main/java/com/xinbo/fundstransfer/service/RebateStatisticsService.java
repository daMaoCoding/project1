package com.xinbo.fundstransfer.service;

import java.util.Map;

import org.springframework.data.domain.PageRequest;

public interface RebateStatisticsService {
	Map<String, Object> showRebateStatistics(String startDate, String endDate, PageRequest pageRequest)
			throws Exception;

	Map<String, Object> showRebateUserByType(String startDate, String queryType, String rebateUser, String bankType,
			 String account, String owner, Integer[] status, String alias, PageRequest pageRequest) throws Exception;
	
	void scheduleStatisticsReabte() throws Exception;
}
