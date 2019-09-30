package com.xinbo.fundstransfer.domain.repository;

import java.math.BigDecimal;
import java.util.List;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.xinbo.fundstransfer.SqlConstants;
import com.xinbo.fundstransfer.domain.entity.AccountStatistics;

public interface FinInStatisticsRepository
		extends BaseRepository<AccountStatistics, Long> {

	/**
	 * 入款明细 银行卡、微信、支付宝
	 * 
	 * @param whereBankValue
	 * @param whereTransactionValue
	 * @param whereAccount
	 * @param fristTime
	 * @param lastTime
	 * @param pageable
	 * @return
	 */
	@Query(nativeQuery = true, value = "select * from (select ifnull(D.amounts,0)amounts,ifnull(D.fees,0)fees,ifnull(D.count_,0)count_,C.* "
			+ "from (select " 
			+ "A.id,B.name,"
			+ "(select GROUP_CONCAT(name,'|') from biz_level where id in (select level_id from biz_account_level where account_id=A.id)  and (?2=0 or (handicap_id in (?1) and id=?2))) levelname,"
			+ "A.account,ifnull(A.bank_balance,0),ifnull(A.alias,'无')alias,ifnull(A.owner,'无'),ifnull(A.bank_type,'无')"
			+ " from biz_account A,biz_handicap B "
			+ "where A.handicap_id=B.id and A.type in (?8) and A.handicap_id in (?1) and (?3 is null or A.account like concat('%',?3,'%'))"
			+ " and (?11 is null or A.owner like concat('%',?11,'%')) and (?12 is null or A.bank_type like concat('%',?12,'%'))) C"
			+ " left join " 
			+ " (select " 
			+ " C.to_id," 
			+ "sum(C.amount)amounts," 
			+ "sum(0)fees,"
			+ "count(1) count_ " 
			+ " from " 
			+ " biz_income_request C "
			+ " where C.type=?10 and (?2=0 or C.level=?2)"
			+ " and (?4 is null or C.update_time between ?4 and ?5) "
			+ " and (?6 is null or C.update_time between ?6 and ?7) group by C.to_id) D on C.id=D.to_id) A "
			+ " where (0 = 0 or levelname=?9) and A.count_!=0"
			, countQuery = SqlConstants.SEARCH_QUEYFININSTATISTICS_COUNTQUERY)
	Page<Object> queyFinInStatistics(List<Integer> handicapList, int level, String whereAccount, String fristTime, String lastTime,
			String fieldval, String whereTransactionValue, List<Integer> types, String handicapname, int trantype,
			String accountOwner, String bankType, Pageable pageable);

	// 查询总计
	@Query(nativeQuery = true, value = SqlConstants.totalFinInStatistics)
	java.lang.Object[] totalFinInStatistics(List<Integer> handicapList, int level, String whereAccount, String fristTime,
			String lastTime, String fieldval, String whereTransactionValue, List<Integer> types, String handicapname,
			int trantype, String accountOwner, String bankType);

	@Query(nativeQuery = true, value = "select distinct * from ( select B.account_handicap,A.account,A.id,A.alias,"
			+ " A.owner,A.bank_type,B.balance,B.income,B.income_count,B.income_persons,B.income_sys,B.income_sys_count,"
			+ " (select GROUP_CONCAT(name,'|') from biz_level where id in (select level_id from biz_account_level where account_id=A.id)) levelname"
			+ " from biz_account A,biz_report B "
			+ " where B.time between ?3 and ?4 and A.type in (?5) and A.id=B.account_id and B.account_handicap in (?1)"
			+ " and (?2 is null or A.account like concat('%',?2,'%'))"
			+ " and (?6 is null or A.owner like concat('%',?6,'%'))"
			+ " and (?7 is null or A.bank_type like concat('%',?7,'%'))"
			+ " and (?8!='Bankcard' or ROUND(B.income)!=B.income))A"
			, countQuery = SqlConstants.SEARCH_QUEYFININSTATISTICS_COUNTQUERY_FROM_CLEAR_DATE)
	Page<Object> queyFinInStatisticsFromClearDate(List<Integer> handicapList, String whereAccount, String fieldval,
			String whereTransactionValue, List<Integer> types, String accountOwner, String bankType,String type, Pageable pageable);

	// 查询总计
	@Query(nativeQuery = true, value = SqlConstants.totalFinInStatisticsFromClearDate)
	java.lang.Object[] totalFinInStatisticsFromClearDate(List<Integer> handicapList, String whereAccount, String fieldval,
			String whereTransactionValue, List<Integer> types, String accountOwner, String bankType,String type);

	/**
	 * 下发银行卡
	 * 
	 * @param whereAccount
	 * @param fristTime
	 * @param lastTime
	 * @param fieldval
	 * @param whereTransactionValue
	 * @param types
	 * @param handicapname
	 * @param trantype
	 * @param pageable
	 * @return
	 */
	@Query(nativeQuery = true, value = "select * from (select ifnull(D.amounts,0)amounts,ifnull(D.fees,0)fees,ifnull(D.count_,0)count_,C.* "
			+ "from (select " 
			+ "A.id,"
			+ "A.account,ifnull(A.bank_balance,0),ifnull(A.alias,'无')alias,ifnull(A.owner,'无'),ifnull(A.bank_type,'无')"
			+ " from biz_account A "
			+ "where A.status in (1,3,4,5) and A.type in (?6) and (?1 is null or A.account like concat('%',?1,'%'))"
			+ " and (?8 is null or A.owner like concat('%',?8,'%')) and (?9 is null or A.bank_type like concat('%',?9,'%'))) C"
			+ " left join " 
			+ " (select " 
			+ " B.to_account," 
			+ "sum(B.amount)amounts," 
			+ "sum(B.fee)fees,"
			+ "count(1) count_ " 
			+ " from " 
			+ " biz_transaction_log B,biz_income_request C "
			+ " where B.order_id=C.id and B.type=?7 " 
			+ " and (?2 is null or C.update_time between ?2 and ?3) "
			+ " and (?4 is null or C.update_time between ?4 and ?5) group by B.to_account) D on C.id=D.to_account) A "
			+ " where A.count_!=0"
			, countQuery = SqlConstants.SEARCH_QUEYFININSTATISTICSSENDCARD_COUNTQUERY)
	Page<Object> queyFinInSendCardStatistics(String whereAccount, String fristTime, String lastTime, String fieldval,
			String whereTransactionValue, List<Integer> types, int trantype, String accountOwner, String bankType,
			Pageable pageable);

	// 查询总计
	@Query(nativeQuery = true, value = SqlConstants.totalFinInStatisticsSendCard)
	java.lang.Object[] totalFinInSendCardStatistics(String whereAccount, String fristTime, String lastTime,
			String fieldval, String whereTransactionValue, List<Integer> types, int trantype, String accountOwner,
			String bankType);

	/**
	 * 入款明细 第三方
	 * 
	 * @param whereBankValue
	 * @param whereTransactionValue
	 * @param whereAccount
	 * @param fristTime
	 * @param lastTime
	 * @param pageable
	 * @return
	 */
	@Query(nativeQuery = true, value = "select * from (select C.*,ifnull(D.amounts,0)amounts,ifnull(D.fees,0)fees,ifnull(D.count_,0)count_ "
			+ "from (select " + "A.id,B.name,B.id handicap_id,"
			+ "ifnull((select GROUP_CONCAT(name,'|') from biz_level where id in (select level_id from biz_account_level where account_id=A.id)  and (?2=0 or (handicap_id in (?1) and id=?2))),'无') levelname,"
			+ "A.account,ifnull(A.bank_balance,0)" 
			+ " from biz_account A,biz_handicap B "
			+ "where A.handicap_id=B.id and A.status in (1,3,4,5) and A.type=?8 and A.handicap_id in (?1) and (?3 is null or A.account like concat('%',?3,'%'))) C"
			+ " left join " 
			+ " (select C.handicap," 
			+ " C.to_account," 
			+ "sum(C.amount)amounts," 
			+ "sum(C.fee)fees,"
			+ "count(1) count_ " 
			+ " from " 
			+ " biz_third_request C "
			+ " where C.handicap in (?1) and (?2=0 or C.level=?2)"
			+ " and (?4 is null or C.ack_time between ?4 and ?5) "
			+ " and (?6 is null or C.ack_time between ?6 and ?7) and C.amount>0 group by C.to_account,C.handicap) D on C.account=D.to_account and C.handicap_id=D.handicap) A where A.amounts>0"
			, countQuery = SqlConstants.SEARCH_QUEYFINTHIRDINSTATISTICS_COUNTQUERY)
	Page<Object> queyFinThirdInStatistics(List<Integer> handicapList, int level, String whereAccount, String fristTime,
			String lastTime, String fieldval, String whereTransactionValue, int type, Pageable pageable);

	// 查询总计
	@Query(nativeQuery = true, value = SqlConstants.totalFinInThirdStatistics)
	java.lang.Object[] totalFinThirdInStatistics(List<Integer> handicapList, int level, String whereAccount, String fristTime,
			String lastTime, String fieldval, String whereTransactionValue, int type);

	// 查询系统明细
	@Query(nativeQuery = true, value = "select "
			+ " (select name from biz_handicap where id=A.handicap_id)handicapname," + "C.type," + "C.member_user_name,"
			+ "C.order_no," + "C.amount," + "IFNULL(0.00,0)fee,"
			+ "date_format(C.create_time,'%Y-%m-%d %H:%i:%s')create_time," + "C.remark" + " from "
			+ " biz_account A,biz_income_request C "
			+ " where (?1 is null or C.member_user_name like concat('%',?1,'%')) "
			+ " and (?2 is null or C.update_time between ?2 and ?3) "
			+ " and (?4=0 or (C.amount>=?4 and C.amount<=?5)) and C.status=1 and A.id=C.to_id and A.id=?6 and C.type=?7"
			, countQuery = SqlConstants.SEARCH_FINDFININSTATMATCH_COUNTQUERY)
	Page<Object> findFinInStatMatch(String memberrealnamet, String fristTime, String lastTime, BigDecimal startamount,
			BigDecimal endamount, int id, int type, Pageable pageable);

	// 查询总计
	@Query(nativeQuery = true, value = SqlConstants.totalfindFinInStatMatch)
	java.lang.Object[] totalfindFinInStatMatch(String memberrealnamet, String fristTime, String lastTime,
			BigDecimal startamount, BigDecimal endamount, int id, int type);

	// 查询银行明细
	@Query(nativeQuery = true, value = "select to_account,to_account_owner,abs(amount),ifnull(remark,''),"
			+ " summary,date_format(trading_time,'%Y-%m-%d %H:%i:%s')trading_time,status,date_format(create_time,'%Y-%m-%d %H:%i:%s')create_time "
			+ " ,balance" 
			+ " from biz_bank_log where create_time between ?1 and ?2"
			+ " and (?3=0 or (abs(amount)>=?3 and abs(amount)<=?4)) and (?7=0 or ((?7=-1 or amount>0) and (?7=1 or amount<0))) and from_account=?5 and (?6=9999 or status=?6)"
			, countQuery = SqlConstants.SEARCH_FINDFININSTATMATCHBANK_COUNTQUERY)
	Page<Object> findMatchBankByAccountid(String fristTime, String lastTime, BigDecimal startamount,
			BigDecimal endamount, int accountid, int status, int typestatus, Pageable pageRequest);

	// 查询总计
	@Query(nativeQuery = true, value = SqlConstants.totalfindFinInStatMatchBank)
	java.lang.Object[] totalfindMatchBankByAccountid(String fristTime, String lastTime, BigDecimal startamount,
			BigDecimal endamount, int accountid, int status, int typestatus);

	/**
	 * 下发卡收入明细
	 * 
	 * @param memberrealnamet
	 * @param fristTime
	 * @param lastTime
	 * @param startamount
	 * @param endamount
	 * @param id
	 * @param type
	 * @param pageable
	 * @return
	 */
	@Query(nativeQuery = true, value = "select * from ( select " 
	        + " C.from_account,"
			+ "(select bank_name from biz_account where id=C.from_id)bankname," 
	        + "C.type," 
			+ "C.order_no,"
			+ "C.amount," 
			+ "IFNULL(B.fee,0)fee," 
			+ "date_format(C.create_time,'%Y-%m-%d %H:%i:%s')create_time,"
			+ "C.remark," 
			+ "C.id,C.from_id,(select handicap_id from biz_account where id=C.from_id)handicap_id"
			+ " from " 
			+ " biz_account A,biz_transaction_log B,biz_income_request C "
			+ " where A.id=B.to_account and B.order_id=C.id and A.id=?6 "
			+ " and (?2 is null or C.create_time between ?2 and ?3) "
			+ " and (?4=0 or (C.amount>=?4 and C.amount<=?5)) and C.status in (1,3,4,5) and B.type=?7 and C.type=?7) A where (?1 is null or A.bankname like concat('%',?1,'%')) and (?8=0 or A.handicap_id=?8) "
			, countQuery = SqlConstants.SEARCH_FINDSENDCARDMATCH_COUNTQUERY)
	Page<Object> findSendCardMatch(String memberrealnamet, String fristTime, String lastTime, BigDecimal startamount,
			BigDecimal endamount, int id, int type, int handicap, Pageable pageable);

	// 查询总计
	@Query(nativeQuery = true, value = SqlConstants.totalfindSendCardMatch)
	java.lang.Object[] totalfindSendCardMatch(String memberrealnamet, String fristTime, String lastTime,
			BigDecimal startamount, BigDecimal endamount, int id, int type, int handicap);

	/**
	 * 第三方 明细
	 * 
	 * @param memberrealnamet
	 * @param fristTime
	 * @param lastTime
	 * @param startamount
	 * @param endamount
	 * @param id
	 * @param type
	 * @param pageable
	 * @return
	 */
	@Query(nativeQuery = true, value = "select "
			+ " (select name from biz_handicap where id=A.handicap_id)handicapname," + "4 type," + "C.member_user_name,"
			+ "C.order_no," + "C.amount," + "IFNULL(C.fee,0)fee,"
			+ "date_format(C.create_time,'%Y-%m-%d %H:%i:%s')create_time," + "C.remark" + " from "
			+ " biz_account A,biz_third_request C "
			+ " where A.account=C.to_account and A.id=?6 and (?1 is null or C.member_user_name like concat('%',?1,'%')) "
			+ " and (?2 is null or C.ack_time between ?2 and ?3) " + " and (?4=0 or (C.amount>=?4 and C.amount<=?5))"
			, countQuery = SqlConstants.SEARCH_FINDFININTHIRDSTATMATCH_COUNTQUERY)
	Page<Object> findFinInThirdStatMatch(String memberrealnamet, String fristTime, String lastTime,
			BigDecimal startamount, BigDecimal endamount, int id, Pageable pageable);

	// 查询总计
	@Query(nativeQuery = true, value = SqlConstants.totalfindFinInThirdStatMatch)
	java.lang.Object[] totalfindFinInThirdStatMatch(String memberrealnamet, String fristTime, String lastTime,
			BigDecimal startamount, BigDecimal endamount, int id);

	@Query(nativeQuery = true, value = "select " + "B.NAME handicapname," + "C.NAME levelname," + "A.member_user_name,"
			+ "A.order_no," + "D.bank_name," + "A.to_account," + "A.amount,"
			+ "date_format(A.create_time,'%Y-%m-%d %H:%i:%s')create_time," + "A.to_id"
			+ " from biz_income_request A,biz_handicap B,biz_level C,biz_account D " + " where "
			+ " (?1=0 or A.handicap=?1) and (?2=0 or A.level=?2) and (?3 is null or A.member_user_name like concat('%',?3,'%'))"
			+ " and (?4 is null or D.bank_name like concat('%',?4,'%'))"
			+ " and (?7 is null or A.to_account like concat('%',?7,'%')) and (?8=0 or (A.amount>=?8 and A.amount<=?9))"
			+ " and A.handicap=B.id and A.level=C.id and A.to_id=D.id" + " and A.type=4 and A.status=1"
			+ " and (?5 is null or unix_timestamp(A.update_time) between unix_timestamp(?5) and unix_timestamp(?6))"
			, countQuery = SqlConstants.SEARCH_QUEYINCOMETHIRD_COUNTQUERY)
	Page<Object> queyIncomeThirdd(int handicap, int level, String accountt, String thirdaccountt, String fristTime,
			String lastTime, String toaccountt, BigDecimal startamount, BigDecimal endamount, Pageable pageRequest);

	@Query(nativeQuery = true, value = "select " + "A.handicap,A.level,A.member_user_name,"
			+ "A.order_no,A.to_id,A.to_account,A.amount,date_format(A.create_time,'%Y-%m-%d %H:%i:%s') create_time"
			+ " from biz_income_request A " + " where "
			+ " (?1=0 or A.handicap=?1) and (?2=0 or A.level=?2) and (?3 is null or A.member_user_name like concat('%',?3,'%'))"
			+ " and (?7 is null or A.to_account like concat('%',?7,'%')) and (?8=0 or (A.amount>=?8 and A.amount<=?9))"
			+ " and (?4 is null or exists (select id from biz_account where id=A.to_id and bank_name like concat('%',?4,'%')))"
			+ " and A.status=1 and A.type=4"
			+ " and unix_timestamp(A.update_time) between unix_timestamp(?5) and unix_timestamp(?6) "
			, countQuery = SqlConstants.SEARCH_QUEYINCOMETHIRD_COUNTQUERY)
	Page<Object> queyIncomeThird(int handicap, int level, String accountt, String thirdaccountt, String fristTime,
			String lastTime, String toaccountt, BigDecimal startamount, BigDecimal endamount, Pageable pageRequest);

	// 查询盘口
	@Query(nativeQuery = true, value = "select name from biz_handicap where id=?1 limit 1")
	String queyhandicap(int handicapid);

	// 查询层级
	@Query(nativeQuery = true, value = "select name from biz_level where id=?1 limit 1")
	String queylevel(int levelid);

	// 查询第三方账号
	@Query(nativeQuery = true, value = "select bank_name from biz_account where id=?1 limit 1")
	String queythirdaccount(int id);

	// 查询总计
	@Query(nativeQuery = true, value = SqlConstants.totalqueyIncomeThird)
	java.lang.Object[] totalqueyIncomeThird(int handicap, int level, String accountt, String thirdaccountt,
			String fristTime, String lastTime, String toaccountt, BigDecimal startamount, BigDecimal endamount);

	@Query(nativeQuery = true, value = "select A.id,A.handicap,B.name handicapname,"
			+ "A.level,C.name,A.amount,A.create_time,A.type,A.order_no,A.to_account,ifnull(D.bank_name,D.bank_type),D.type banktype,A.member_real_name "
			+ " FROM " + " biz_income_request A,biz_handicap B,biz_level C,biz_account D"
			+ " where A.handicap=B.id and A.level=C.id and A.to_id=D.id and A.type=3"
			+ " and A.member_user_name=?1 and A.create_time between ?3 and ?4 and A.to_id=?2"
			, countQuery = SqlConstants.SEARCH_FINDINCOMEBYACCOUNT_COUNTQUERY)
	Page<Object> findIncomeByAccount(String member, int toid, String fristTime, String lastTime, Pageable pageable);

	// 查询总计
	@Query(nativeQuery = true, value = SqlConstants.findIncomeByAccount)
	java.lang.Object[] totalfindIncomeByAccount(String member, int toid, String fristTime, String lastTime);

}