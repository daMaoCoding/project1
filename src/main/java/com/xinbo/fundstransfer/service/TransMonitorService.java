package com.xinbo.fundstransfer.service;

import com.xinbo.fundstransfer.domain.entity.BizIncomeRequest;
import com.xinbo.fundstransfer.domain.pojo.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TransMonitorService {

	/**
	 * report the transaction's result.
	 * 
	 * @param entity
	 *            transaction's information
	 */
	void reportTransResult(TransferEntity entity);

	/**
	 * report the transaction's success result
	 * 
	 * @param taskId
	 *            outward task's identity
	 */
	void reportTransResult(Long taskId);

	/**
	 * report the transaction's success result by income request.
	 * 
	 * @param o
	 *            income request
	 */
	void reportTransResult(BizIncomeRequest o);

	/**
	 * reset the MonitorRisk information.
	 *
	 * <p>
	 * the account need to be reconciled for transaction records
	 * <p>
	 * the method would clean the account's transaction acknowledge records ,
	 * the last reconciled result except the last reconciled snapshot.
	 *
	 * @param accId
	 *            the account's identity to be reset
	 */
	void resetMonitorRisk(int accId);

	/**
	 * report account's real balance
	 * 
	 * @param id
	 *            the account's ID
	 * @param realBal
	 *            the account's real balance at present.
	 */
	void reportRealBal(Integer id, BigDecimal realBal);

	/**
	 * get account's system balance and alarm information.
	 *
	 * @param accList
	 *            the account's ID collection.
	 * @return key:id </br>
	 *         value[0] the account's system balance </br>
	 *         value[1] </br>
	 *         BigDecimal.ZERO :no alarm.</br>
	 *         BigDecimal.ONE :has alarm.</br>
	 */
	Map<Integer, BigDecimal[]> findSysBalAndAlarm(List<Integer> accList);

	/**
	 * set the result of Monitor transaction records.
	 * 
	 * @param acc
	 *            the account be monitored
	 * @param lastRealBal
	 *            the last real balance.
	 * @param thisRealBal
	 *            the real balance at the present.
	 * @param ret
	 *            {@code true} if the transaction records are monitored
	 *            successfully;{@code false} otherwise
	 * @param retList
	 *            the list of transaction records
	 */
	void setMonitorRiskResult(AccountBaseInfo acc, BigDecimal lastRealBal, BigDecimal thisRealBal, boolean ret,
			List<TransAck> retList);

	/**
	 * get the result of transaction records monitored latest.
	 *
	 * @param accId
	 *            the account be monitored
	 */
	TransMonitorResult<TransAckResult> getMonitorRiskResult(int accId) throws IOException;

	/**
	 * Tells whether or not the specified account has matching bank flows
	 * 
	 * @param accId
	 *            the specified account's identity
	 *
	 * @return <code>true</code> if and only if the specified account doesn't
	 *         have matching bank flows;<code>false</code> otherwise.
	 */
	boolean checkAccAlarm4Flow(int accId);

	/**
	 * get current sysbal of account
	 *
	 * @param id
	 * @return
	 */
	BigDecimal getSysBal(Integer id);

	/**
	 * update the account sysbal
	 *
	 * @param id
	 * @param sysBal
	 */
	void setSysBal(Integer id, BigDecimal sysBal);

	/**
	 * account's ID that has been in Risk status by monitoring the real balance
	 * transformation.
	 */
	Set<Integer> buildAcc4MonitorRisk();
}
