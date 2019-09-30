package com.xinbo.fundstransfer.service;

import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;

public interface FinMoreStatStatService {

	/**
	 * 出入 财务汇总
	 * 
	 * @param handicap
	 *            盘口
	 * @param fristTime
	 *            开始时间
	 * @param lastTime
	 *            结束时间
	 * @param fieldval
	 *            开始时间
	 * @param whereTransactionValue
	 *            结束时间
	 * @param pageRequest
	 * @return
	 * @throws Exception
	 */
	Map<String, Object> findMoreStat(int handicap, String fristTime, String lastTime, String fieldval,
			String whereTransactionValue, PageRequest pageRequest) throws Exception;

	Map<String, Object> findMoreStatFromClearDate(List<Integer> handicapList, String fristTime, String lastTime,
			String fieldval, String whereTransactionValue, PageRequest pageRequest) throws Exception;

	Map<String, Object> finmorestatFromClearDateRealTime(List<Integer> handicapList, String fristTime, String lastTime,
			PageRequest pageRequest) throws Exception;

	Map<String, Object> findOutThird(String fristTime, String lastTime, PageRequest pageRequest) throws Exception;

	Map<String, Object> findIncomRequest(String fristTime, String lastTime, PageRequest pageRequest) throws Exception;

	Map<String, Object> findLossAmounts(String fristTime, String lastTime, PageRequest pageRequest) throws Exception;

	Map<String, Object> findThridIncom(String fristTime, String lastTime, PageRequest pageRequest) throws Exception;

	Map<String, Object> findfreezeCard(String fristTime, String lastTime, PageRequest pageRequest) throws Exception;

	Map<String, Object> findThirdIncomCounts(String fristTime, String lastTime, PageRequest pageRequest)
			throws Exception;

	/**
	 * 出入财务汇总》明细
	 * 
	 * @param handicap
	 *            盘口
	 * @param level
	 *            层级
	 * @param fristTime
	 *            时间控件开始时间
	 * @param lastTime
	 *            时间控件结束时间
	 * @param fieldval
	 *            时间单选开始时间
	 * @param whereTransactionValue
	 *            时间单选结束时间
	 * @param pageRequest
	 * @return
	 * @throws Exception
	 */
	Map<String, Object> findMoreLevelStat(int handicap, int level, String fristTime, String lastTime, String fieldval,
			String whereTransactionValue, PageRequest pageRequest) throws Exception;

	Map<String, Object> findOutPerson(String startTime, String endTime, PageRequest pageRequest) throws Exception;

}
