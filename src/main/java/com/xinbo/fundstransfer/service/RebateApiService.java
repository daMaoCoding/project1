package com.xinbo.fundstransfer.service;

import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.pojo.TransferEntity;
import com.xinbo.fundstransfer.restful.v3.pojo.ResV3DailyLogItem;

import java.math.BigDecimal;
import java.util.List;

public interface RebateApiService {

	/**
	 * @param commissionTime
	 *            time format :'yyyy-MM-dd'
	 */
	void commissionDaily(String commissionTime);

	void commissionDailyReturnsummary(String commissionTime, int type);

	/**
	 * part-time duty personal's bank account commission daily
	 *
	 * @param acc
	 *            part-time personal's bank account
	 * @param total
	 *            bank statement stroke amount
	 * @param amount
	 *            commission
	 * @param time
	 *            calculate time ,time format 'yyyy-MM-dd'
	 * @return {@code true} revoke successfully ,otherwise, {@code false}
	 */
	boolean commissionDaily(BizAccountMore more, String acc, BigDecimal total, BigDecimal amount, String time,
			List<ResV3DailyLogItem> tid, int accId);

	boolean commissionDailyReturnsummary(BizAccountMore more, String acc, BigDecimal total, BigDecimal amount,
			String time, List<ResV3DailyLogItem> tid, int accId, List<Integer> doneList, int type);

	void logs(List<BizBankLog> logs);

	/**
	 * confirm bank account credit limit for part-time duty personal.
	 *
	 * @param bankLog
	 *            bank statement
	 * @param req
	 *            upgrade limit request
	 */
	void ackCreditLimit(BizBankLog bankLog, BizIncomeRequest req);

	/**
	 * confirm bank account credit limit for part-time duty personal.
	 *
	 * @param acc
	 *            part-time duty personal's bank account
	 * @param amount
	 *            bank account credit limit
	 * @return {@code true} confirm success ,otherwise, {@code false}
	 */
	boolean ackCreditLimit(String acc, BigDecimal amount, String tid);

	/**
	 * confirm part-time duty personal commission remittance
	 *
	 * @param uid
	 *            part-time user identity
	 * @param amount
	 *            remittance amount
	 * @param balance
	 *            total commission
	 * @param tid
	 *            Rebate System business id
	 * @param msg
	 *            remark
	 * @return {@code true} confirm success ,otherwise, {@code false}
	 */
	boolean ackRemittance(String uid, BigDecimal amount, BigDecimal balance, String tid, String msg);

	/**
	 * cancel part-time duty personal commission remittance
	 *
	 * @param uid
	 *            part-time user identity
	 * @param amount
	 *            remittance amount
	 * @param balance
	 *            total commission
	 * @param tid
	 *            Rebate System business id
	 * @param msg
	 *            remark
	 * @return {@code true} cancel success ,otherwise, {@code false}
	 */
	boolean cancelRemittance(String uid, BigDecimal amount, BigDecimal balance, String tid, String msg);

	/**
	 * confirm part-time duty personal commission remittance
	 *
	 * @param rebateId
	 *            {@link com.xinbo.fundstransfer.domain.entity.BizAccountRebate}#id
	 *            [not null]
	 * @param remark
	 *            description [not null]
	 * @param operator
	 *            who send the cancel directive. [not null]
	 */
	void confirm(Long rebateId, String remark, SysUser operator);

	/**
	 * robot confirm part-time duty personal commission remittance@param
	 * rebateId
	 * {@link com.xinbo.fundstransfer.domain.entity.BizAccountRebate}#id [not
	 * null]
	 */
	void confirmByRobot(TransferEntity entity);

	/**
	 * cancel part-time duty personal commission remittance
	 *
	 * @param rebateId
	 *            {@link com.xinbo.fundstransfer.domain.entity.BizAccountRebate}#id
	 *            [not null]
	 * @param remark
	 *            description [not null]
	 * @param operator
	 *            who send the cancel directive. [not null]
	 */
	void cancel(Long rebateId, String remark, SysUser operator, boolean forece);

	/**
	 * remark part-time duty personal commission remittance
	 *
	 * @param rebateId
	 *            {@link com.xinbo.fundstransfer.domain.entity.BizAccountRebate}#id
	 *            [not null]
	 * @param remark
	 *            description [not null]
	 * @param operator
	 *            who send the cancel directive. [not null]
	 */
	void remark(Long rebateId, String remark, SysUser operator);

	/**
	 * alter the duty {@code rebateId} to Pending investigation status.
	 *
	 * @param rebateId
	 *            {@link com.xinbo.fundstransfer.domain.entity.BizAccountRebate}#id
	 *            [not null]
	 * @param remark
	 *            description [not null]
	 * @param operator
	 *            who send the failure directive. [not null]
	 */
	void fail(Long rebateId, String remark, SysUser operator);

	void failByRobot(TransferEntity entity);

	void match(Long rebateId, Long bankLogId, String remark, SysUser operator);

	boolean checkAckLimit(Integer accId);

	/**
	 * report auditing account information to rebate system.
	 *
	 * @param audit
	 *            {@code true} pass audit, otherwise,{@code false}
	 */
	void auditAcc(boolean audit, String oriAcc, String oriOwner, String currAcc, String currOwner);

	BizAccountRebate findById(Long rebateId);

	void ackRechargelimit(BizAccountMore acc, float amount, String tid);

	String getUserByUid(String moible);

	void finish(Long rebateId, String remark, SysUser operator);

	boolean timelyManner(boolean audit, String oriAcc, String oriOwner, String currAcc, String currOwner);

	boolean derate(String account, float amount, float balance, Long logid, String tid);

	void activation(String account, String uid, String status, String cardNo[], String message);

	void usersDevice(String mobile, String ip, String deviceId);

	BigDecimal deductAmount(String uid, BigDecimal amount, String type, String acc, String remark);

	void rebateUserStatus(String account);

	void joinFlwActivity(String uid, String activityNumber);

	boolean limitCancel(String uid, BigDecimal balance, String msg, String tid);
}
