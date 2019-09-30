package com.xinbo.fundstransfer.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface AccountStatisticsService {

	/**
	 * 
	 * @param whereBankValue
	 *            单选按钮 时间搓开始时间
	 * @param whereTransactionValue
	 *            单选按钮 时间搓结束时间
	 * @param whereAccount
	 *            账号
	 * @param fristTime
	 *            时间控件开始时间
	 * @param lastTime
	 *            时间控件结束时间
	 * @param pageRequest
	 * @return
	 * @throws Exception
	 */
	Map<String, Object> findAccountStatistics(String whereBankValue, String whereTransactionValue, String whereAccount,
			String fristTime, String lastTime, String accountOwner, String bankType, int handicap,
			PageRequest pageRequest) throws Exception;

	Map<String, Object> AccountStatisticsFromClearDate(String whereBankValue, String whereTransactionValue,
			String whereAccount, String fristTime, String lastTime, String accountOwner, String bankType,
			List<Integer> handicapList, String cartype, PageRequest pageRequest) throws Exception;

	/**
	 * 查询系统明细
	 * 
	 * @param accountid
	 *            账号id
	 * @param accountname
	 *            会员账号
	 * @param ptfristTime
	 *            父页面开始时间
	 * @param ptlastTime
	 *            父页面结束时间
	 * @param pageRequest
	 * @return
	 * @throws Exception
	 */
	Map<String, Object> findFinOutStatSys(int accountid, String accountname, String ptfristTime, String ptlastTime,
			BigDecimal startamount, BigDecimal endamount, int restatus, int tastatus, PageRequest pageRequest)
			throws Exception;

	/**
	 * 查询银行明细
	 * 
	 * @param accountid
	 *            账号id
	 * @param toaccountowner
	 *            开户人
	 * @param toaccount
	 *            收款账号
	 * @param ptfristTime
	 *            父页面开始时间
	 * @param ptlastTime
	 *            父页面结束时间
	 * @param pageRequest
	 * @return
	 * @throws Exception
	 */
	Map<String, Object> findFinOutStatFlow(int accountid, String toaccountowner, String toaccount, String ptfristTime,
			String ptlastTime, BigDecimal startamount, BigDecimal endamount, int bkstatus, int typestatus,
			PageRequest pageRequest) throws Exception;

	/**
	 * 查询银行明细详情信息
	 * 
	 * @param id
	 *            收款账号id
	 * @param pageRequest
	 * @return
	 * @throws Exception
	 */
	Page<Object> findFinOutStatFlowDetails(int id, PageRequest pageRequest) throws Exception;

	/**
	 * 出款明细>按盘口统计
	 * 
	 * @param whereHandicap
	 *            盘口
	 * @param whereLevel
	 *            层级
	 * @param fristTime
	 *            时间控件 时间搓开始时间
	 * @param lastTime
	 *            时间控件 时间搓结束时间
	 * @param fieldvalHandicap
	 *            时间单选按钮开始时间
	 * @param whereTransactionValue
	 *            时间单选按钮结束时间
	 * @param pageRequest
	 * @return
	 * @throws Exception
	 */
	Map<String, Object> findAccountStatisticsHandicap(int whereHandicap, int whereLevel, String fristTime,
			String lastTime, String fieldvalHandicap, String whereTransactionValue, PageRequest pageRequest)
			throws Exception;

	Map<String, Object> findAccountStatisticsHandicapFromClearDate(List<Integer> handicapList, int whereLevel,
			String fristTime, String lastTime, String fieldvalHandicap, String whereTransactionValue,
			PageRequest pageRequest) throws Exception;

	/**
	 * 出款明细>按盘口统计>明细
	 * 
	 * @param handicap
	 *            盘口
	 * @param level
	 *            层级
	 * @param member
	 *            会员名称
	 * @param fristTime
	 *            时间控件 时间搓开始时间
	 * @param lastTime
	 *            时间控件 时间搓结束时间
	 * @param startamount
	 *            金额范围开始值
	 * @param endamount
	 *            金额范围结束值
	 * @param type
	 * @param rqhandicap
	 *            父页面传过来的盘口值
	 * @param id
	 *            盘口id
	 * @param parentstartAndEndTimeToArray
	 *            父页面传过来的时间控件数组值
	 * @param parentfieldval
	 *            父页面传过来的时间单选按钮值
	 * @return
	 * @throws JsonProcessingException
	 */
	Map<String, Object> findFinOutStatMatch(int handicap, int level, String member, String fristTime, String lastTime,
			BigDecimal startamount, BigDecimal endamount, String type, int rqhandicap, int id, int restatus,
			int tastatus, List<Integer> handicaps, PageRequest pageRequest) throws Exception;
}
