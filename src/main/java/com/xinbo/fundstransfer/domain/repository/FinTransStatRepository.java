package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.xinbo.fundstransfer.SqlConstants;
import com.xinbo.fundstransfer.domain.entity.AccountStatistics;

public interface FinTransStatRepository
		extends BaseRepository<AccountStatistics, Long> {

	/**
	 * 中转明细
	 * 
	 * @param whereAccount
	 * @param fristTime
	 * @param lastTime
	 * @param fieldval
	 * @param whereTransactionValue
	 * @param type
	 * @param pageable
	 * @return
	 */
	@Query(nativeQuery = true, value = "select A.account,A.type,A.accountid,A.alias,A.owner,A.bank_type,ifnull(A.amounts,0) inAmounts,ifnull(A.fees,0) inFees,ifnull(A.count_,0) inCounts,abs(ifnull(B.amounts,0)),ifnull(B.fees,0),ifnull(B.count_,0),ifnull(A.handicap_id,0) from ("
			+ " select"
			+ " ac.handicap_id,ac.account,ac.alias,ac.owner,ac.bank_type,ac.type,re.from_id accountid,SUM(re.amount)amounts,SUM(re.fee)fees,count(1)count_"
			+ " from" 
			+ " biz_income_request re,biz_account ac"
			+ " where re.status=1 and ac.type in (?4) and re.from_id=ac.id and re.type in (?5) and re.update_time BETWEEN ?2 and ?3"
			+ " and (?1 is null or ac.account like concat('%',?1,'%')) and (?6 is null or ac.owner like concat('%',?6,'%'))"
			+ " and (?7 is null or ac.bank_type like concat('%',?7,'%'))" 
			+ " and (?8=0 or ac.handicap_id=?8)"
			+ " GROUP BY re.from_id)A" 
			+ " left join" 
			+ " (select"
			+ " lo.from_account accountid,SUM(lo.amount)amounts,SUM(0.0)fees,count(1)count_" + " from"
			+ " biz_bank_log lo,biz_account ac"
			+ " where lo.status=1 and lo.from_account=ac.id and lo.amount<0 and ac.type in (?4) and lo.trading_time BETWEEN ?2 and ?3"
			+ " GROUP BY lo.from_account)B on A.accountid=B.accountid"
			, countQuery = SqlConstants.SEARCH_QUEYFINDFINTRANSSTAT_COUNTQUERY)
	Page<Object> queyfindFinTransStat(String account, String fristTime, String lastTime, List<Integer> accountTypes,
			List<Integer> incomeType, String accountOwner, String bankType, int handicap, Pageable pageable);

	// 查询总计
	@Query(nativeQuery = true, value = SqlConstants.totalfindFinTransStat)
	java.lang.Object[] totalfindFinTransStat(String account, String fristTime, String lastTime,
			List<Integer> accountTypes, List<Integer> incomeType, String accountOwner, String bankType, int handicap);
	
	@Query(nativeQuery = true, value = "select B.account_handicap,A.account,A.id,A.alias,"
			 +"A.owner,A.bank_type,B.balance,B.outward,B.fee,B.outward_count,B.outward_sys,B.outward_sys_count"
			 +" from biz_account A,biz_report B" 
			 +" where B.time between ?2 and ?3 and (?1 is null or A.account like concat('%',?1,'%')) "
			 +" and (?5 is null or A.owner like concat('%',?5,'%'))"
			 +" and (?6 is null or A.bank_type like concat('%',?6,'%'))"
			 +" and (?8!='Bankcard' or ROUND(B.outward_sys)!=B.outward_sys)"
			 +" and B.account_handicap in (?7)"
			 +" and A.type in (?4) and A.id=B.account_id order by B.outward_sys desc"
			, countQuery = SqlConstants.SEARCH_QUEYFINDFINTRANSSTAT_COUNTQUERY_FROM_CLEAR_DATE)
	Page<Object> queyfindFinTransStatFromClearDate(String account, String fristTime, String lastTime, List<Integer> accountTypes,
			 String accountOwner, String bankType, List<Integer> handicapList,String type, Pageable pageable);

	// 查询总计
	@Query(nativeQuery = true, value = SqlConstants.totalfindFinTransStatFromClearDate)
	java.lang.Object[] totalfindFinTransStatFromClearDate(String account, String fristTime, String lastTime,
			List<Integer> accountTypes, String accountOwner, String bankType, List<Integer> handicapList,String type);

	// 第三方中转
	@Query(nativeQuery = true, value = "select A.account,A.type,A.accountid,A.alias,A.owner,A.bank_type,ifnull(A.amounts,0) inAmounts,ifnull(A.fees,0) inFees,ifnull(A.count_,0) inCounts,ifnull(A.handicap_id,0),A.bank_name from ("
			+ " select"
			+ " ac.bank_name,ac.handicap_id,ac.account,ac.alias,ac.owner,ac.bank_type,ac.type,re.from_id accountid,SUM(re.amount)amounts,SUM(re.fee)fees,count(1)count_"
			+ " from" 
			+ " biz_income_request re,biz_account ac"
			+ " where ac.type in (?4) and re.from_id=ac.id and re.type in (?5) and re.create_time BETWEEN ?2 and ?3"
			+ " and (?1 is null or ac.account like concat('%',?1,'%')) and (?6 is null or ac.owner like concat('%',?6,'%'))"
			+ " and (?7 is null or ac.bank_type like concat('%',?7,'%'))" 
			+ " and (?8=0 or ac.handicap_id=?8) and re.status=1"
			+ " GROUP BY re.from_id)A" 
			, countQuery = SqlConstants.SEARCH_QUEYFINDTHIRD_COUNTQUERY)
	Page<Object> queyfindthird(String account, String fristTime, String lastTime, List<Integer> accountTypes,
			List<Integer> incomeType, String accountOwner, String bankType, int handicap, Pageable pageable);

	// 查询总计
	@Query(nativeQuery = true, value = SqlConstants.totalfindthird)
	java.lang.Object[] totalfindthird(String account, String fristTime, String lastTime, List<Integer> accountTypes,
			List<Integer> incomeType, String accountOwner, String bankType, int handicap);

	/**
	 * 中转明细>流水明细
	 * 
	 * @param orderno
	 * @param fristTime
	 * @param lastTime
	 * @param startamount
	 * @param endamount
	 * @param accountid
	 * @param type
	 * @param pageable
	 * @return
	 */
	@Query(nativeQuery = true, value = "select " 
	        + "C.order_no," 
			+ "A.from_account," 
	        + "A.to_account,"
			+ "(select account from biz_account where id=C.to_id)from_accountname," 
	        + "IFNULL(A.amount,0)amount,"
			+ "IFNULL(0.0,0)fee," 
			+ "date_format(A.trading_time,'%Y-%m-%d %H:%i:%s')trading_time," 
			+ "C.remark,"
			+ "date_format(A.create_time,'%Y-%m-%d %H:%i:%s')create_time" 
			+ " from "
			+ " biz_bank_log A,biz_transaction_log B,biz_income_request C " 
			+ " where C.status=1" 
			+ " and  A.status=1"
			+ " and (?1 is null or C.order_no =?1) " 
			+ " and C.update_time between ?2 and ?3"
			+ " and A.amount>0 and A.status=1 and A.id=B.to_banklog_id and B.order_id=C.id and C.from_id=?6"
			+ " and (?4=0 or (A.amount>=?4 and A.amount<=?5))"
			, countQuery = SqlConstants.SEARCH_FINTRANSSTATMATCHBANK_COUNTQUERY)
	Page<Object> finTransStatMatchBank(String orderno, String fristTime, String lastTime, BigDecimal startamount,
			BigDecimal endamount, int accountid, Pageable pageable);

	// 查询总计
	@Query(nativeQuery = true, value = SqlConstants.totalfinTransStatMatchBank)
	java.lang.Object[] totalfinTransStatMatchBank(String orderno, String fristTime, String lastTime,
			BigDecimal startamount, BigDecimal endamount, int accountid);

	/**
	 * 中转明细>系统明细
	 * 
	 * @param orderno
	 * @param fristTime
	 * @param lastTime
	 * @param startamount
	 * @param endamount
	 * @param accountid
	 * @param type
	 * @param pageable
	 * @return
	 */
	@Query(nativeQuery = true, value = "select " 
	        + "B.order_no," 
			+ "B.from_id,"
			+ "B.from_account from_accountname,"
			+ "A.account," 
			+ "IFNULL(B.amount,0)amount,"
			+ "IFNULL(B.fee,0)fee," 
			+ "date_format(B.update_time,'%Y-%m-%d %H:%i:%s')create_time," 
			+ "B.remark,B.to_id,B.status,A.handicap_id "
			+ " from " 
			+ " biz_income_request B,biz_account A "
			+ " where (?8=9999 or B.status=?8) and (?6=0 or (B.from_id=?6)) and (?6!=0 or (B.type in (?11))) "
			+ " and (?1 is null or B.order_no =?1) "
			+ " and (?7=103 or (?2 is null or B.update_time between ?2 and ?3)) and (?7!=103 or (?2 is null or B.update_time between ?2 and ?3))"
			+ " and (?4=0 or (B.amount>=?4 and B.amount<=?5)) and (?9=0 or A.handicap_id=?9) and A.handicap_id in (?10) and B.to_id=A.id"
			, countQuery = SqlConstants.SEARCH_FINTRANSSTATMATCHSYS_COUNTQUERY)
	Page<Object> finTransStatMatchSys(String orderno, String fristTime, String lastTime, BigDecimal startamount,
			BigDecimal endamount, int accountid,int type,int status,int handicap,List<Integer> handicapIds,List<Integer> types, Pageable pageable);

	// 查询总计
	@Query(nativeQuery = true, value = SqlConstants.totalfinTransStatMatchSys)
	java.lang.Object[] totalfinTransStatMatchSys(String orderno, String fristTime, String lastTime,
			BigDecimal startamount, BigDecimal endamount, int accountid,int type,int status,int handicap);
	
	// 中转隔天排查
	@Query(nativeQuery = true, value = "select A.* from ( select id,from_id,to_id,amount,order_no,status,"
			+" date_format(create_time,'%Y-%m-%d %H:%i:%s')create_time,date_format(update_time,'%Y-%m-%d %H:%i:%s')update_time from biz_income_request where "
			+" create_time between ?1 and ?2" 
			+" and (update_time>?2 or status=0 or update_time>(select concat(if((date_format(create_time,'%Y-%m-%d')=date_format(update_time,'%Y-%m-%d')),date_format(date_add(create_time,interval 1 day),'%Y-%m-%d'),date_format(update_time,'%Y-%m-%d')), ' 06:59:59')))"
			+" and type in (106,107))A where ((date_format(A.create_time,'%Y-%m-%d')!=date_format(A.update_time,'%Y-%m-%d')) or A.update_time is null)" 
			, countQuery = SqlConstants.SEARCH_SCREENING_COUNTQUERY)
	Page<Object> queyScreening( String fristTime, String lastTime, Pageable pageable);

	// 查询总计
	@Query(nativeQuery = true, value = SqlConstants.totalScreening)
	java.lang.Object[] totalScreening(String fristTime, String lastTime);
	
	
	
	@Query(nativeQuery = true,value = "select " 
			+" re.handicap,re.level,ac.account,ac.bank_name,ac.bank_type,re.order_no,re.amount,re.fee,"
			+"re.status,re.create_time,re.update_time ,re.to_id"
			+" from"
			+" biz_income_request re,biz_account ac"
			+" where re.create_time between ?1 and ?2 and re.from_id=ac.id and ac.handicap_id in (?5) and (?3=88888888 or re.from_id=?3) and (?4=0 or re.handicap=?4) and re.type=103")
	List<Object[]> finThirdPartyTransit(String fristTime, String lastTime,int accountid,int handicap,List<Integer> handicaps);
	
	/**
	 * 出入卡清算
	 * @param account
	 * @param fristTime
	 * @param lastTime
	 * @param accountTypes
	 * @param incomeType
	 * @param accountOwner
	 * @param bankType
	 * @param handicap
	 * @param pageable
	 * @return
	 */
	@Query(nativeQuery = true, value = "select A.*,C.balance from (select A.handicap_id,A.account,A.type,A.id,A.alias, "
			  +" A.owner,A.bank_type, B.*,A.status from biz_account A,(select sum(B.income),sum(B.outward),sum(B.fee),sum(B.income_count),sum(B.outward_count),sum(B.outward_persons) "
			  +" ,sum(B.income_persons),sum(B.fee_count),sum(B.loss),sum(B.loss_count),sum(B.income_sys),sum(B.income_sys_count),sum(B.outward_sys),sum(B.outward_sys_count),B.account_id"
			  +" from fundsTransfer.biz_report B where B.time between ?2 and ?3 group by B.account_id)B where A.id=B.account_id" 
			 +" and (?1 is null or A.account like concat('%',?1,'%')) "
			 +" and (?4 is null or A.owner like concat('%',?4,'%'))"
			 +" and (?5 is null or A.bank_type like concat('%',?5,'%'))"
			 +" and A.handicap_id in (?6)"
			 +" and A.type in (?7) and (?8!=3 or account_id in (?9))"
			 +" and (?8=0 or A.status=?8))A left join (select account_id,sum(balance)balance from biz_report where time=(select DATE_ADD(DATE_SUB(CURDATE(),INTERVAL 1 DAY),INTERVAL 7 HOUR) from dual) group by account_id) C on A.account_id=C.account_id order by C.balance desc"
			 , countQuery = SqlConstants.SEARCH_QUEYFINCARDLIQUIDATION_COUNTQUERY)
	Page<Object> queyFinCardLiquidation(String account, String fristTime, String lastTime,
			String accountOwner, String bankType, List<Integer> handicapList,List<Integer> types,int status,List<Integer> frozenCardId, Pageable pageable);

	// 查询总计
	@Query(nativeQuery = true, value = SqlConstants.totalqueyFinCardLiquidation)
	java.lang.Object[] totalFinCardLiquidation(String account, String fristTime, String lastTime,
			 String accountOwner, String bankType, List<Integer> handicapList,List<Integer> types,int status,List<Integer> frozenCardId);
	
	// 备用卡入款数据存在双倍，查询数据除以一半
	@Query(nativeQuery = true, value = SqlConstants.minusDate)
	java.lang.Object[] minusDate(String account, String fristTime, String lastTime,
			 String accountOwner, String bankType, List<Integer> handicapList,List<Integer> types,int status);
	
	
	@Query(nativeQuery = true, value = "select account_id from biz_account_trace where create_time between ?1 and ?2")
	List<Integer> findFrozenCardId(String fristTime, String lastTime);

}