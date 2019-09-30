package com.xinbo.fundstransfer.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

public interface FinTransStatService {

	/**
	 * 中转明细
	 * 
	 * @param whereAccount
	 *            汇入账号
	 * @param fristTime
	 *            时间控件开始时间
	 * @param lastTime
	 *            时间控件结束时间
	 * @param fieldval
	 *            时间单选按钮开始时间
	 * @param whereTransactionValue
	 *            时间单选按钮结束时间
	 * @param type
	 *            标识查询哪 个数据类型（入款银行卡中转、支付宝、微信、第三方）
	 * @param pageRequest
	 * @return
	 * @throws Exception
	 */
	Map<String, Object> findFinTransStat(String whereAccount, String fristTime, String lastTime, String fieldval,
			String whereTransactionValue, String type, String accountOwner, String bankType, int handicap,
			PageRequest pageRequest) throws Exception;

	Map<String, Object> findFinTransStatFromClearDate(String whereAccount, String fristTime, String lastTime,
			String fieldval, String whereTransactionValue, String type, String accountOwner, String bankType,
			List<Integer> handicapList, PageRequest pageRequest) throws Exception;

	/**
	 * 中转明细》明细
	 * 
	 * @param orderno
	 *            订单号
	 * @param fristTime
	 *            开始时间
	 * @param lastTime
	 *            结束时间
	 * @param startamount
	 *            开始金额
	 * @param endamount
	 *            结束金额
	 * @param accountid
	 *            账号id
	 * @param type
	 *            标识查询哪个类型（入款银行卡中转、支付宝、微信、第三方）
	 * @param serytype
	 *            标识哪个流水（系统流水、银行流水）
	 * @param pageRequest
	 * @return
	 * @throws Exception
	 */
	Map<String, Object> finTransStatMatch(String orderno, String fristTime, String lastTime, BigDecimal startamount,
			BigDecimal endamount, int accountid, int type, String serytype, int status, int handicap,
			List<Integer> handicapIds, PageRequest pageRequest) throws Exception;

	/**
	 * 查询入款第三方中转数据的导出
	 * 
	 * @param fristTime
	 * @param lastTime
	 * @param accountid
	 * @return
	 * @throws Exception
	 */
	List<Object[]> finThirdPartyTransit(String fristTime, String lastTime, int accountid, int handicap,
			List<Integer> handicaps) throws Exception;

	/**
	 * 出入卡清算
	 * 
	 * @param whereAccount
	 * @param fristTime
	 * @param lastTime
	 * @param fieldval
	 * @param whereTransactionValue
	 * @param type
	 * @param accountOwner
	 * @param bankType
	 * @param handicap
	 * @param pageRequest
	 * @return
	 * @throws Exception
	 */
	Map<String, Object> FinCardLiquidation(String whereAccount, String fristTime, String lastTime, String fieldval,
			String whereTransactionValue, String type, String accountOwner, String bankType, List<Integer> handicapList,
			String cartype, String status, PageRequest pageRequest) throws Exception;

}
