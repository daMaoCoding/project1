package com.xinbo.fundstransfer.domain.repository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.persistence.LockModeType;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.xinbo.fundstransfer.SqlConstants;
import com.xinbo.fundstransfer.domain.entity.BizBankLog;

public interface BankLogRepository extends BaseRepository<BizBankLog, Long> {
	/**
	 * 统计入款银行卡 未匹配流水总数量 默认是当日 如果传入时间 则根据实际时间查询
	 * 
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	@Query(nativeQuery = true, value = " select from_account,count(id) from biz_bank_log where create_time between ?1 and ?2 and status=0 and amount>0 group by from_account ")
	List<Object[]> countsUnmatchFlows(Date startTime, Date endTime);

	List<BizBankLog> findFirst5000ByIdBetween(long stardId, long endId);

	@Query(nativeQuery = true, value = "select t.* from biz_bank_log t where from_account=?1 and id<?2 order by id desc limit 1")
	BizBankLog findByIdLessThanEqual(Integer fromAccount, Long id);

	/**
	 * 根据账号id,当前时间与交易时间差查询匹配中金额大于零的数据的金额总和
	 *
	 * @param hours
	 * @return
	 */
	@Query(nativeQuery = true, value = SqlConstants.querySumAmountCondition)
	String querySumAmountCondition(Integer fromId, int hours);

	@Query(nativeQuery = true, value = SqlConstants.queryIncomeTotal)
	String queryIncomeTotal(Integer fromId);

	@Query(nativeQuery = true, value = SqlConstants.queryOutTotal)
	String queryOutTotal(Integer fromId);

	@Lock(value = LockModeType.PESSIMISTIC_WRITE)
	@Query(value = "select j from BizBankLog j where j.id =:id ")
	BizBankLog getBizBankLogByIdForUpdate(@Param("id") Long id);

	List<BizBankLog> findByFromAccountAndAmount(int fromAccount, BigDecimal amount);

	/** 查询冲正 */
	@Query(nativeQuery = true, value = " SELECT  b.id ,b.from_account,a.alias,a.account,b.trading_time,b.create_time as createTime,b.amount,a.bank_balance,b.to_account,b.to_account_owner,b.summary,b.remark as remarkLog,h.name ,a.bank_type  \n "
			+ " FROM fundsTransfer.biz_bank_log b join fundsTransfer.biz_account a join fundsTransfer.biz_handicap h \n"
			+ " on b.from_account =a.id and a.handicap_id = h.id "
			+ " where 1=1 AND b.status in (:status) and  h.code in(:handicap)  "
			+ " and (:fromAccount is null or a.alias like :fromAccount or a.account like :fromAccount ) "
			+ " and (:orderNo is null or b.remark like :orderNo ) and (:operator is null or b.remark like :operator ) "
			+ " and (:amountStart is null or b.amount >= :amountStart ) and (:amountEnd is null or b.amount <= :amountEnd )"
			+ " and (:startTime is null or b.create_time >=:startTime ) and (:endTime is null  or b.create_time <=:endTime )"
			+ " ORDER by b.create_time DESC  ")
	List<Object[]> queryBackWashBankLong(Pageable pageable, @Param("handicap") List<String> handicap,
			@Param("fromAccount") String fromAccount, @Param("orderNo") String orderNo,
			@Param("operator") String operator, @Param("status") List<Integer> status,
			@Param("amountStart") BigDecimal amountStart, @Param("amountEnd") BigDecimal amountEnd,
			@Param("startTime") Date startTime, @Param("endTime") Date endTime);

	/** 查询冲正总记录数 */
	@Query(nativeQuery = true, value = " SELECT  COUNT(b.id)  \n"
			+ " FROM fundsTransfer.biz_bank_log b join fundsTransfer.biz_account a join fundsTransfer.biz_handicap h \n"
			+ " on b.from_account =a.id and a.handicap_id = h.id "
			+ " where 1=1 AND b.status  in (:status) and  h.code in(:handicap)  "
			+ " and (:fromAccount is null or a.alias like :fromAccount or a.account like :fromAccount ) "
			+ " and (:orderNo is null or b.remark like :orderNo ) and (:operator is null or b.remark like :operator ) "
			+ " and (:amountStart is null or b.amount >= :amountStart ) and (:amountEnd is null or b.amount <= :amountEnd )"
			+ " and (:startTime is null or b.create_time >=:startTime ) and (:endTime is null  or b.create_time <=:endTime )")
	Long countBackWashBankLong(@Param("handicap") List<String> handicap, @Param("fromAccount") String fromAccount,
			@Param("orderNo") String orderNo, @Param("operator") String operator, @Param("status") List<Integer> status,
			@Param("amountStart") BigDecimal amountStart, @Param("amountEnd") BigDecimal amountEnd,
			@Param("startTime") Date startTime, @Param("endTime") Date endTime);

	/** 查询冲正总金额 */
	@Query(nativeQuery = true, value = " SELECT sum(b.amount) \n"
			+ " FROM fundsTransfer.biz_bank_log b join fundsTransfer.biz_account a join fundsTransfer.biz_handicap h \n"
			+ " on b.from_account =a.id and a.handicap_id = h.id "
			+ " where 1=1 AND b.status in (:status) and h.code in(:handicap)  "
			+ " and (:fromAccount is null or a.alias like :fromAccount or a.account like :fromAccount ) "
			+ " and (:orderNo is null or b.remark like :orderNo ) and (:operator is null or b.remark like :operator ) "
			+ " and (:amountStart is null or b.amount >= :amountStart ) and (:amountEnd is null or b.amount <= :amountEnd )"
			+ " and (:startTime is null or b.create_time >=:startTime ) and (:endTime is null  or b.create_time <=:endTime )")
	BigDecimal sumBackWashBankLong(@Param("handicap") List<String> handicap, @Param("fromAccount") String fromAccount,
			@Param("orderNo") String orderNo, @Param("operator") String operator, @Param("status") List<Integer> status,
			@Param("amountStart") BigDecimal amountStart, @Param("amountEnd") BigDecimal amountEnd,
			@Param("startTime") Date startTime, @Param("endTime") Date endTime);

	/**
	 * 银行流水
	 * 
	 * @param pageable
	 * @param toAccount
	 * @param fromAccount
	 * @param minAmount
	 * @param maxAmount
	 * @param startTime
	 * @param endTime
	 * @param status
	 * @return
	 */
	@Query(nativeQuery = true, value = "SELECT id,from_account, "
			+ " date_format(trading_time,'%Y-%m-%d %H:%i:%s') trading_time, "
			+ " date_format(create_time,'%Y-%m-%d %H:%i:%s') create_time, "
			+ " amount,balance,to_account,to_account_owner,summary,remark,status  " + SqlConstants.queryFilter_bankLog
			+ " AND (:status is null  or status=:status) ", countQuery = "select " + " count(1) "
					+ SqlConstants.queryFilter_bankLog + " AND (:status is null  or status=:status) ")
	Page<Object> bankLogList(@Param("pageable") Pageable pageable, @Param("toAccount") String toAccount,
			@Param("toAccountOwner") String toAccountOwner, @Param("fromAccount") int fromAccount,
			@Param("minAmount") BigDecimal minAmount, @Param("maxAmount") BigDecimal maxAmount,
			@Param("startTime") Date startTime, @Param("endTime") Date endTime, @Param("status") Integer status);

	/**
	 * 只查询匹配中
	 * 
	 * @param pageable
	 * @param toAccount
	 * @param fromAccount
	 * @param minAmount
	 * @param maxAmount
	 * @param startTime
	 * @param endTime
	 * @param status
	 * @return
	 */
	@Query(nativeQuery = true, value = "SELECT id,from_account, "
			+ " date_format(trading_time,'%Y-%m-%d %H:%i:%s') trading_time, "
			+ " date_format(create_time,'%Y-%m-%d %H:%i:%s') create_time, "
			+ " amount,balance,to_account,to_account_owner,summary,remark,status  " + SqlConstants.queryFilter_bankLog
			+ " AND status=0 " + " AND create_time > NOW() - INTERVAL 24 HOUR ", countQuery = "select " + " count(1) "
					+ SqlConstants.queryFilter_bankLog + " AND status=0 "
					+ " AND create_time > NOW() - INTERVAL 24 HOUR ")
	Page<Object> bankLogListStatus0(@Param("pageable") Pageable pageable, @Param("toAccount") String toAccount,
			@Param("toAccountOwner") String toAccountOwner, @Param("fromAccount") int fromAccount,
			@Param("minAmount") BigDecimal minAmount, @Param("maxAmount") BigDecimal maxAmount,
			@Param("startTime") Date startTime, @Param("endTime") Date endTime);

	/**
	 * 只查询未认领
	 * 
	 * @param pageable
	 * @param toAccount
	 * @param fromAccount
	 * @param minAmount
	 * @param maxAmount
	 * @param startTime
	 * @param endTime
	 * @param status
	 * @return
	 */
	@Query(nativeQuery = true, value = "SELECT id,from_account, "
			+ " date_format(trading_time,'%Y-%m-%d %H:%i:%s') trading_time, "
			+ " date_format(create_time,'%Y-%m-%d %H:%i:%s') create_time, "
			+ " amount,balance,to_account,to_account_owner,summary,remark,status  " + SqlConstants.queryFilter_bankLog
			+ " AND status=0 " + " AND create_time <= NOW() - INTERVAL 24 HOUR ", countQuery = "select " + " count(1) "
					+ SqlConstants.queryFilter_bankLog + " AND status=0 "
					+ " AND create_time <= NOW() - INTERVAL 24 HOUR ")
	Page<Object> bankLogListStatus3(@Param("pageable") Pageable pageable, @Param("toAccount") String toAccount,
			@Param("toAccountOwner") String toAccountOwner, @Param("fromAccount") int fromAccount,
			@Param("minAmount") BigDecimal minAmount, @Param("maxAmount") BigDecimal maxAmount,
			@Param("startTime") Date startTime, @Param("endTime") Date endTime);

	/**
	 * 只查询入款未认领
	 * 
	 * @param pageable
	 * @param toAccount
	 * @param fromAccount
	 * @param minAmount
	 * @param maxAmount
	 * @param startTime
	 * @param endTime
	 * @param status
	 * @return
	 */
	@Query(nativeQuery = true, value = "SELECT log.id bankLogIds,a.id fromAccountId, "
			+ " date_format(log.trading_time,'%Y-%m-%d %H:%i:%s') tradingTime, "
			+ " date_format(log.create_time,'%Y-%m-%d %H:%i:%s') createTime, "
			+ " log.amount,log.balance,log.to_account,log.to_account_owner,log.summary,log.remark,log.status,a.account  "
			+ SqlConstants.queryFilter_noOwner4Income, countQuery = "select " + " count(1) "
					+ SqlConstants.queryFilter_noOwner4Income)
	Page<Object> bankLogListStatus4(@Param("pageable") Pageable pageable, @Param("toAccount") String toAccount,
			@Param("toAccountOwner") String toAccountOwner, @Param("fromAccountNO") String fromAccountNO,
			@Param("remark") String remark, @Param("minAmount") BigDecimal minAmount,
			@Param("maxAmount") BigDecimal maxAmount, @Param("startTime") Date startTime,
			@Param("endTime") Date endTime, @Param("handicapIdToList") List<Integer> handicapIdToList);

	/** 金额总计 入款未认领 */
	@Query(nativeQuery = true, value = "SELECT sum(log.amount)  " + SqlConstants.queryFilter_noOwner4Income)
	BigDecimal bankLogList_sumAmount4(@Param("toAccount") String toAccount,
			@Param("toAccountOwner") String toAccountOwner, @Param("fromAccountNO") String fromAccountNO,
			@Param("remark") String remark, @Param("minAmount") BigDecimal minAmount,
			@Param("maxAmount") BigDecimal maxAmount, @Param("startTime") Date startTime,
			@Param("endTime") Date endTime, @Param("handicapIdToList") List<Integer> handicapIdToList);

	/** 金额总计 */
	@Query(nativeQuery = true, value = "SELECT sum(case when amount>0 then amount end)a,sum(case when amount<0 then amount end)b,sum(abs(amount)) c  "
			+ SqlConstants.queryFilter_bankLog + " AND (:status is null  or status=:status) ")
	List<Object> bankLogList_sumAmount(@Param("toAccount") String toAccount,
			@Param("toAccountOwner") String toAccountOwner, @Param("fromAccount") int fromAccount,
			@Param("minAmount") BigDecimal minAmount, @Param("maxAmount") BigDecimal maxAmount,
			@Param("startTime") Date startTime, @Param("endTime") Date endTime, @Param("status") Integer status);

	/** 金额总计 匹配中 */
	@Query(nativeQuery = true, value = "SELECT sum(case when amount>0 then amount end)a,sum(case when amount<0 then amount end)b,sum(abs(amount)) c  "
			+ SqlConstants.queryFilter_bankLog + " AND status=0 " + " AND create_time > NOW() - INTERVAL 24 HOUR ")
	List<Object> bankLogList_sumAmountStatus0(@Param("toAccount") String toAccount,
			@Param("toAccountOwner") String toAccountOwner, @Param("fromAccount") int fromAccount,
			@Param("minAmount") BigDecimal minAmount, @Param("maxAmount") BigDecimal maxAmount,
			@Param("startTime") Date startTime, @Param("endTime") Date endTime);

	/** 金额总计 未认领 */
	@Query(nativeQuery = true, value = "SELECT sum(case when amount>0 then amount end)a,sum(case when amount<0 then amount end)b,sum(abs(amount)) c  "
			+ SqlConstants.queryFilter_bankLog + " AND status=0 " + " AND create_time <= NOW() - INTERVAL 24 HOUR ")
	List<Object> bankLogList_sumAmountStatus3(@Param("toAccount") String toAccount,
			@Param("toAccountOwner") String toAccountOwner, @Param("fromAccount") int fromAccount,
			@Param("minAmount") BigDecimal minAmount, @Param("maxAmount") BigDecimal maxAmount,
			@Param("startTime") Date startTime, @Param("endTime") Date endTime);

	/** 统计未匹配流水的数量 */
	@Query(nativeQuery = true, value = "SELECT from_account,count(1) FROM fundsTransfer.biz_bank_log where 1=1 and (:startTime is null or create_time >=:startTime) and (:endTime is null or create_time <=:endTime) and status=0 and amount>=0 and from_account in(:fromAccountIdList)  group  by from_account   ")
	List<Object[]> countUnmatchBankLogs(@Param("fromAccountIdList") List<Integer> fromAccountIdList,
			@Param("startTime") Date startTime, @Param("endTime") Date endTime);

	/** 查询需要导出的下发数据 */
	@Query(nativeQuery = true, value = "select B.id,B.from_account,B.trading_time,B.amount,B.status,B.remark,B.to_account,B.to_account_owner,B.balance,B.create_time,B.summary,B.update_time,A.from_account trFromAccount from fundsTransfer.biz_transaction_log A right join (select * from fundsTransfer.biz_bank_log where create_time between ?1 and ?2 "
			+ " and from_account in ( select id from fundsTransfer.biz_account where type in (10,11,12,13) and handicap_id in (?3)))B on A.to_banklog_id=B.id")
	List<Object> findSenderCard(String starTime, String endTime, List<Integer> handicaps);

	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_bank_log set status=?2,update_time=now() where id=?1")
	void updateBankLog(Long id, Integer status);

	@Query(nativeQuery = true, value = "select * from biz_bank_log where from_account=?1 and create_time>=?2 and create_time<=?3 and amount<0 and status=1")
	List<BizBankLog> findRebateRecord(int fromAccount, Date greaterTime, Date littleTime);

	@Query(nativeQuery = true, value = "select * from biz_bank_log where from_account=?1 and create_time between ?3 and ?4 and amount=?2 and status=0 limit 1")
	BizBankLog findRebateLimitBankLog(int accountId, BigDecimal amount, String startTime, String endTime);

	/** 查询交易时间对应账号的流水总数 **/
	@Query(nativeQuery = true, value = "select count(1) from biz_bank_log where from_account=?1 and trading_time >= ?2 and trading_time <= ?3")
	Long getTotalBankLog(int accountId, String startTime, String endTime);

	@Query(nativeQuery = true, value = "select * from biz_bank_log where trading_time between ?1 and ?2 and from_account=?3 and status=?4 and amount=?5 and substring(summary,1,3)=?6")
	List<BizBankLog> finBanks(Date tradingStart, Date tradingEnd, int fromAccountId, int status, BigDecimal amount,
			String owner);

	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_bank_log set commission=?2 where id=?1")
	void updateCsById(Long id, BigDecimal commission);

	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_bank_log set status=?2,remark=?3 where id=?1")
	void updateStatusRm(Long id, Integer status, String remark);

	@Query(nativeQuery = true, value = "select ifnull(sum(commission),0.00) from biz_bank_log where from_account=?1 and trading_time >= ?2 and trading_time <= ?3")
	BigDecimal findTotalAmount(int fromAccount, String startTime, String endTime);

	@Query(nativeQuery = true, value = "select count(1) from biz_bank_log where from_account=?1 and amount=?2 and status=0 and create_time between ?3 and ?4 limit 1")
	int finCounts(int fromId, BigDecimal amount, String startTime, String endTime);

	/** 查询当天日期内产生的流水总数 **/
	@Query(nativeQuery = true, value = "select count(1) from biz_bank_log bbl where bbl.from_account=?1 and bbl.amount<=-11 and bbl.status='1' and to_days(bbl.trading_time) = to_days(now())")
	int getDateTotalBankLog(int accountId);
	
	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_bank_log set balance=?2 where id=?1")
	void updateBalanceByid(Long id, BigDecimal balance);
	
	@Query(nativeQuery = true, value = "select ifnull(sum(commission),0.00) from biz_bank_log where from_account in (?1) and create_time>=?2 and create_time<=?3 and amount<0 and status=1")
	BigDecimal findRebateBanks(List<Integer> fromAccount, Date greaterTime, Date littleTime);
}