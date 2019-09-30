package com.xinbo.fundstransfer.service;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.data.domain.PageRequest;

public interface FinBalanceStatService {

	/**
	 * 余额明细
	 * 
	 * @param pageRequest
	 * @return
	 * @throws Exception
	 */
	Map<String, Object> finBalanceStat(PageRequest pageRequest) throws Exception;

	Map<String, Object> finbalanceEveryDay(int handicapId, String startTime, String endTime, PageRequest pageRequest)
			throws Exception;

	Map<String, Object> findBalanceDetail(String account, String bankType, int handicap, String time, int type,
			int status, PageRequest pageRequest) throws Exception;

	/**
	 * 余额明细>明细
	 * 
	 * @param id
	 *            标识不同的数据源
	 * @param account
	 *            账号
	 * @param pageRequest
	 * @return
	 * @throws Exception
	 */
	Map<String, Object> finBalanceStatCard(int id, String account, int status, String bankType, PageRequest pageRequest)
			throws Exception;

	/**
	 * 余额明细>明细>系统明细
	 * 
	 * @param to_account
	 *            汇出账号
	 * @param from_account
	 *            汇入账号
	 * @param fristTime
	 *            开始日期
	 * @param lastTime
	 *            结束日期
	 * @param startamount
	 *            开始金额
	 * @param endamount
	 *            结束金额
	 * @param accountid
	 *            账号id
	 * @param id
	 *            标识不同的数据源（入款账号、出款账号、备用金、现金卡..）
	 * @param type
	 *            标识 系统流水 or 银行流水
	 * @param pageRequest
	 * @return
	 * @throws Exception
	 */
	Map<String, Object> finTransBalanceSys(String to_account, String from_account, String fristTime, String lastTime,
			BigDecimal startamount, BigDecimal endamount, int accountid, int id, String type, String accounttype,
			String accountname, PageRequest pageRequest) throws Exception;

	/**
	 * 清算数据，查询是否还存在没有匹配的数据
	 * 
	 * @param fristTime
	 *            时间
	 * @param pageRequest
	 *            分页参数
	 * @return
	 * @throws Exception
	 */
	Map<String, Object> ClearAccountDate(String fristTime, PageRequest pageRequest) throws Exception;

	/**
	 * 删除已经匹配的数据
	 * 
	 * @param fristTime
	 *            时间
	 * @return
	 * @throws Exception
	 */
	Map<String, Object> DeleteAccountDate(String fristTime) throws Exception;

	void delFolder(String path, String startTime) throws Exception;

}
