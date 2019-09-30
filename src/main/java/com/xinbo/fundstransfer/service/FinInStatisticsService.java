package com.xinbo.fundstransfer.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;

public interface FinInStatisticsService {

	/**
	 * 入款明细
	 * 
	 * @param handicap
	 *            盘口
	 * @param level
	 *            层级
	 * @param whereAccount
	 *            收款账号
	 * @param fristTime
	 *            时间控件开始时间值
	 * @param lastTime
	 *            时间控件结束时间值
	 * @param fieldval
	 *            时间单选按钮开始值
	 * @param whereTransactionValue
	 *            时间单选按钮结束值
	 * @param type
	 *            标识查询哪个数据源（银行卡、微信、支付宝、第三方）
	 * @param handicapname
	 *            盘口name值
	 * @param pageRequest
	 * @return
	 * @throws Exception
	 */
	Map<String, Object> findFinInStatistics(List<Integer> handicapList, int level, String whereAccount,
			String fristTime, String lastTime, String fieldval, String whereTransactionValue, String type,
			String handicapname, String accountOwner, String bakType, PageRequest pageRequest) throws Exception;

	Map<String, Object> findFinInStatisticsFromClearDate(List<Integer> handicapList, int level, String whereAccount,
			String fristTime, String lastTime, String fieldval, String whereTransactionValue, String type,
			String handicapname, String accountOwner, String bakType, PageRequest pageRequest) throws Exception;

	/**
	 * 入款明细》明细
	 * 
	 * @param memberrealnamet
	 *            汇出人
	 * @param fristTime
	 *            开始时间
	 * @param lastTime
	 *            结束时间
	 * @param startamount
	 *            开始金额
	 * @param endamount
	 *            结束金额
	 * @param id
	 *            账号id
	 * @param type
	 *            标识查询哪个数据源（银行卡、微信、支付宝、第三方）
	 * @param pageRequest
	 * @return
	 * @throws Exception
	 */
	Map<String, Object> findFinInStatMatch(String memberrealnamet, String fristTime, String lastTime,
			BigDecimal startamount, BigDecimal endamount, int id, String type, int handicap, PageRequest pageRequest)
			throws Exception;

	/**
	 * 银行明细
	 * 
	 * @param fristTime
	 * @param lastTime
	 * @param startamount
	 * @param endamount
	 * @param accountid
	 * @param status
	 * @param pageRequest
	 * @return
	 * @throws Exception
	 */
	Map<String, Object> findFinInStatMatchBank(String fristTime, String lastTime, BigDecimal startamount,
			BigDecimal endamount, int accountid, int status, String InStatOrTransStat, int typestatus,
			PageRequest pageRequest) throws Exception;

	Map<String, Object> findIncomeThird(int handicap, int level, String accountt, String thirdaccountt,
			String fristTime, String lastTime, String toaccountt, BigDecimal startamount, BigDecimal endamount,
			String type, PageRequest pageRequest) throws Exception;

	Map<String, Object> findIncomeByAccount(String member, int toid, String fristTime, String lastTime,
			PageRequest pageRequest) throws Exception;

	/**
	 * 统计微信支付宝入款数据
	 * 
	 * @param pageNo
	 * @param handicapCode
	 * @param account
	 * @param fristTime
	 * @param lastTime
	 * @param type
	 * @param pageSize
	 * @return
	 * @throws Exception
	 */
	Object CountReceipts(int pageNo, String handicapCode, String account, String fristTime, String lastTime,
			String type, String pageSize) throws Exception;

	/**
	 * 查看微信、支付宝的系统明细
	 * 
	 * @param pageNo
	 * @param account
	 * @param fristTime
	 * @param lastTime
	 * @param startamount
	 * @param endamount
	 * @param orderNo
	 * @param type
	 * @param pageSize
	 * @return
	 * @throws Exception
	 */
	Object sysDetail(int pageNo, String account, String fristTime, String lastTime, BigDecimal startamount,
			BigDecimal endamount, String orderNo, String type, String pageSize) throws Exception;

}
