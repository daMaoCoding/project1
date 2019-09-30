package com.xinbo.fundstransfer.domain.repository;
import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.xinbo.fundstransfer.SqlConstants;
import com.xinbo.fundstransfer.domain.entity.AccountStatistics;

public interface FinMoreStatRepository
		extends BaseRepository<AccountStatistics, Long> {

	@Query(nativeQuery = true, value = "select "
			+" A.handicap,A.name,IFNULL(A.count_in,0),IFNULL(A.amount_in_balance,0),IFNULL(A.amount_in_actualamount,0),"
			+" IFNULL(B.count_out,0),IFNULL(B.count_out_fee,0),IFNULL(B.amount_out_balance,0),IFNULL(B.amount_out_actualamount,0),"
			+" (IFNULL(A.amount_in_actualamount,0)-IFNULL(B.amount_out_actualamount,0)-IFNULL(B.count_out_fee,0))profit,IFNULL(A.countinps,0),IFNULL(B.countoutps,0) "
			+" from ( select D.handicap,D.name,D.count_in,D.amount_in_balance,E.amount_in_actualamount,D.countinps" 
			+" from " 
			+" (select A.handicap,B.name,sum(count_in)count_in,sum(amount_in_balance)amount_in_balance,sum(countinps) countinps from (select A.handicap,count(1)count_in,sum(A.amount)amount_in_balance,count(DISTINCT A.member_code) countinps "
			+" from biz_income_request A"
			+" where A.type>=1 and type<=100 and A.status!=3 and (?1=0 or handicap=?1) "
			+" and (?2 is null or A.create_time between ?2 and ?3) "
			+" and (?4 is null or A.create_time between ?4 and ?5)"
			+" GROUP BY handicap union select A.handicap,count(1)count_in,sum(A.amount)amount_in_balance,count(DISTINCT A.member_code) countinps "
			+" from biz_third_request A"
			+" where (?1=0 or handicap=?1) "
			+" and (?2 is null or A.ack_time between ?2 and ?3) "
			+" and (?4 is null or A.ack_time between ?4 and ?5)"
			+" GROUP BY handicap)A,biz_handicap B where A.handicap=B.id and B.status=1 group by handicap) D " 
			+" LEFT JOIN "
			+" (select sum(A.amount_in_actualamount)amount_in_actualamount,A.handicap from(select IFNULL(sum(B.amount),0)amount_in_actualamount,B.handicap " 
			+" from biz_income_request B,biz_transaction_log C "
			+" where B.id=C.order_id and B.status in (1,4,5) and C.type>=1 and C.type<=100 and (?1=0 or B.handicap=?1) "
			+" and (?2 is null or B.update_time between ?2 and ?3) "
			+" and (?4 is null or B.update_time between ?4 and ?5)"
			+" GROUP BY B.handicap union select IFNULL(sum(B.amount),0)amount_in_actualamount,B.handicap"   
			+" from biz_third_request B "
			+"   where (?1=0 or B.handicap=?1) and "
			+"  (?2 is null or B.ack_time between ?2 and ?3) and (?4 is null or B.ack_time between ?4 and ?5)"
			+"  GROUP BY B.handicap)A group by A.handicap)E on D.handicap=E.handicap) A"
			+" LEFT JOIN"
			+" (select B.handicap,B.count_out,C.count_out_fee,B.amount_out_balance,C.amount_out_actualamount,B.countoutps" 
			+" from "
			+" (select IFNULL(count(1),0)count_out,IFNULL(SUM(A.amount),0)amount_out_balance,A.handicap,count(DISTINCT A.member_code) countoutps "
			+" from biz_outward_request A"
			+" where A.status!=4 and (?1=0 or A.handicap=?1) "
			+" and (?2 is null or A.update_time between ?2 and ?3) "
			+" and (?4 is null or A.update_time between ?4 and ?5)"
			+" GROUP BY A.handicap) B "
			+" LEFT JOIN"
			+" (select IFNULL(sum(C.amount),0)amount_out_actualamount,B.handicap,IFNULL(sum(D.fee),0)count_out_fee "
			+" from biz_outward_request B,biz_outward_task C,biz_transaction_log D"
			+" where B.id=C.outward_request_id and C.id=D.order_id and D.type=0 and B.status in (5,6) and C.status=5 and (?1=0 or B.handicap=?1) "
			+" and (?2 is null or B.update_time between ?2 and ?3) "
			+" and (?4 is null or B.update_time between ?4 and ?5)"
			+" GROUP BY B.handicap)C ON B.handicap=C.handicap) B"
			+" ON A.handicap=B.handicap",countQuery = SqlConstants.SEARCH_QUEYFINDMORESTAT_COUNTQUERY)
	Page<Object> queyfindMoreStat(int handicap,String fristTime,String lastTime,String fieldval,
			String whereTransactionValue,Pageable pageable);
	//查询总计
	@Query(nativeQuery = true, value =SqlConstants.totalfindMoreStat)
	java.lang.Object[] totalfindMoreStat(int handicap,String fristTime,String lastTime,String fieldval,
			String whereTransactionValue);
	
	@Query(nativeQuery = true, value = "select I.*,J.outward_persons,J.outward_sys_count,J.fee,J.outward_sys,J.loss_count,abs(J.loss) from ( select "
			+"B.account_handicap,"
			+"sum(B.outward_persons)outward_persons,"
			+"sum(outward_sys_count)outward_sys_count,"
			+"sum(fee)fee,"
			+"sum(B.outward_sys)outward_sys,"
			+"sum(B.loss_count)loss_count,"
			+"sum(B.loss)loss"
			+" from biz_account A,biz_report B" 
			+" where A.id=B.account_id and (A.type in (5,14) or (A.type=1 and ROUND(B.outward_sys)=B.outward_sys)) and B.time between ?2 and ?3"
			+" group by B.account_handicap)J"
			+" left join"
			+" (select account_handicap,sum(income_persons)income_persons,sum(income_count)income_count,sum(income)income"
			+" from ( select" 
			+" B.account_handicap,"
			+"sum(B.income_persons)income_persons,"
			+"sum(B.income_sys_count)income_count,"
			+"sum(B.income_sys)income"
			+" from biz_account A,biz_report B" 
			+" where A.id=B.account_id and A.type=1 and B.time between ?2 and ?3"
			+" group by B.account_handicap"
			+")A group by account_handicap)I on J.account_handicap=I.account_handicap where J.account_handicap in (?1)",countQuery = SqlConstants.SEARCH_QUEYFINDMORESTAT_COUNTQUERY)
	Page<Object> queyfindMoreStatFromClearDate(List<Integer> handicapList,String fristTime,String lastTime,Pageable pageable);
	
	@Query(nativeQuery = true, value = "select sum(outward_sys),handicap,sum(outward_sys_count),sum(income),sum(income_count),sum(income_persons),sum(fee),sum(outward_persons) from biz_report where handicap is not null and time between ?1 and ?2 group by handicap" ,countQuery = SqlConstants.SEARCH_QUEYFINDMORESTAT_COUNTQUERYY)
	Page<Object> findOutThird(String fristTime,String lastTime,Pageable pageable);
	
	@Query(nativeQuery = true, value = "select handicap,ifnull(SUM(cast(amount as DECIMAL(18,2))),0)  from biz_income_request "
			+" where status=1 and type<100 and update_time BETWEEN ?1 and ?2 group by handicap" ,countQuery = SqlConstants.SEARCH_QUEYFINDMORESTAT_COUNTQUERYY)
	Page<Object> findIncomRequest(String fristTime,String lastTime,Pageable pageable);
	
	@Query(nativeQuery = true, value = "select B.handicap_id,count(1),sum(A.bal-ifnull(A.amount,0)) from biz_account_trace A,biz_account B where A.account_id=B.id"
				+" and A.create_time between ?1 and ?2 group by B.handicap_id" ,countQuery = SqlConstants.SEARCH_QUEYFINDMORESTAT_COUNTQUERYY)
	Page<Object> findfreezeCard(String fristTime,String lastTime,Pageable pageable);
	
	@Query(nativeQuery = true, value = "select handicap,count(1) from biz_third_request where ack_time between ?1 and ?2 group by handicap",countQuery = SqlConstants.SEARCH_QUEYFINDMORESTAT_COUNTQUERYY)
	Page<Object> findThirdIncomCounts(String fristTime,String lastTime,Pageable pageable);
	
	
	//查询入款人数返回
	@Query(nativeQuery = true, value ="select count(1) from (select count(1) from biz_income_request where"
		+" (?2 is null or unix_timestamp(update_time) between unix_timestamp(?2) and unix_timestamp(?3)) "
		+" and (?4 is null or unix_timestamp(update_time) between unix_timestamp(?4) and unix_timestamp(?5) and handicap=?1 and type>=1 and type<=100 ) group by member_code)A")
	int queyfindMoreStatCountinps(int id,String fristTime,String lastTime,String fieldval,String whereTransactionValue);
	//查询出款人数返回
	@Query(nativeQuery = true, value ="select count(1)from(select count(1) from biz_outward_request where "
		+" (?2 is null or unix_timestamp(update_time) between unix_timestamp(?2) and unix_timestamp(?3)"
		+" and (?4 is null or unix_timestamp(update_time) between unix_timestamp(?4) and unix_timestamp(?5)) and handicap=?1) group by member_code)A")
	int queyfindMoreStatCountoutps(int id,String fristTime,String lastTime,String fieldval,String whereTransactionValue);
	
	/**
	 * 查询出入汇总统计详情
	 * @param handicap
	 * @param level
	 * @param fristTime
	 * @param lastTime
	 * @param fieldval
	 * @param whereTransactionValue
	 * @param pageable
	 * @return
	 */
	@Query(nativeQuery = true, value = "select A.handicap,A.handicapname,A.levalname,"
			    +" IFNULL(A.count_in,0),IFNULL(A.amount_in_balance,0),IFNULL(A.amount_in_actualamount,0),"
				+" IFNULL(B.count_out,0),IFNULL(B.count_out_fee,0),IFNULL(B.amount_out_balance,0),IFNULL(B.amount_out_actualamount,0),"
				+" IFNULL((IFNULL(A.amount_in_actualamount,0)-IFNULL(B.amount_out_actualamount,0)-IFNULL(B.count_out_fee,0)),0)profit,IFNULL(A.countinps,0),IFNULL(B.countoutps,0) "
				+" from (select D.handicap,D.handicapname,D.level,D.levalname,D.count_in,D.amount_in_balance,E.amount_in_actualamount,D.countinps"
				+" from " 
				+" (select A.handicap,C.name handicapname,A.level,B.name levalname,sum(count_in)count_in,sum(A.amount_in_balance)amount_in_balance,sum(A.countinps) countinps from (select A.handicap,A.level,count(1)count_in,sum(A.amount)amount_in_balance,count(DISTINCT A.member_code) countinps "
				+" from biz_income_request A"
				+" where A.type>=1 and type<=100 and A.status!=3 and A.handicap=?1 and (?2=0 or A.level=?2) "
				+" and (?3 is null or A.create_time between ?3 and ?4) "
				+" and (?5 is null or A.create_time between ?5 and ?6)"
				+" GROUP BY level union select A.handicap,A.level,count(1)count_in,sum(A.amount)amount_in_balance,count(DISTINCT A.member_code) countinps "
				+" from biz_third_request A"
				+" where A.handicap=?1 and (?2=0 or A.level=?2) "
				+" and (?3 is null or A.create_time between ?3 and ?4) "
				+" and (?5 is null or A.create_time between ?5 and ?6)"
				+" GROUP BY level)A,biz_level B,biz_handicap C where A.level=B.id and A.handicap=C.id and A.handicap=?1 GROUP BY A.level)D"
				+" LEFT JOIN"
				+" (select sum(A.amount_in_actualamount)amount_in_actualamount,A.level from (select IFNULL(sum(B.amount),0)amount_in_actualamount,B.level"
				+" from biz_income_request B,biz_transaction_log C"
				+" where B.id=C.order_id and B.status in (1,4,5) and C.type>=1 and C.type<=100 and B.handicap=?1 and (?2=0 or B.level=?2)"
				+" and (?3 is null or B.update_time between ?3 and ?4) "
				+" and (?5 is null or B.update_time between ?5 and ?6)"
				+" GROUP BY B.level union select IFNULL(sum(B.amount),0)amount_in_actualamount,B.level"
				+" from biz_third_request B"
				+" where B.handicap=?1 and (?2=0 or B.level=?2)"
				+" and (?3 is null or B.ack_time between ?3 and ?4) "
				+" and (?5 is null or B.ack_time between ?5 and ?6)"
				+" GROUP BY B.level) A group by A.level)E on D.level=E.level)A"
				+" LEFT JOIN"
				+" (select B.level,B.count_out,C.count_out_fee,B.amount_out_balance,C.amount_out_actualamount,B.countoutps"
				+" from" 
				+" (select IFNULL(count(1),0)count_out,IFNULL(SUM(A.amount),0)amount_out_balance,A.level,count(DISTINCT A.member_code) countoutps"
				+" from biz_outward_request A"
				+" where A.status!=4 and A.handicap=?1 and (?2=0 or A.level=?2) " 
				+" and (?3 is null or A.create_time between ?3 and ?4) "
				+" and (?5 is null or A.create_time between ?5 and ?6)"
				+" GROUP BY A.level) B" 
				+" LEFT JOIN"
				+" (select IFNULL(sum(C.amount),0)amount_out_actualamount,B.level,IFNULL(sum(D.fee),0)count_out_fee" 
				+" from biz_outward_request B,biz_outward_task C,biz_transaction_log D"
				+" where B.id=C.outward_request_id and C.id=D.order_id and D.type=0 and B.status in (5,6) and C.status=5 and B.handicap=?1 and (?2=0 or B.level=?2)"
				+" and (?3 is null or B.update_time between ?3 and ?4) "
				+" and (?5 is null or B.update_time between ?5 and ?6)"
				+" GROUP BY B.level)C ON B.level=C.level) B"
				+" ON A.level=B.level",countQuery = SqlConstants.SEARCH_QUEYFINDMORELEVELSTAT_COUNTQUERY)
	Page<Object> queyfindMoreLevelStat(int handicap,int level,String fristTime,String lastTime,String fieldval,
			String whereTransactionValue,Pageable pageable);
	//查询总计
	@Query(nativeQuery = true, value =SqlConstants.totalfindMoreLevelStat)
	java.lang.Object[] totalfindMoreLevelStat(int handicap,int level,String fristTime,String lastTime,String fieldval,
			String whereTransactionValue);
	
	//查询入款人数返回
	@Query(nativeQuery = true, value ="select count(1) from (select count(1) from biz_income_request where handicap=?2 and level=?1 and type>=1 and type<=100 "
		+" and (?3 is null or unix_timestamp(update_time) between unix_timestamp(?3) and unix_timestamp(?4)) "
		+" and (?5 is null or unix_timestamp(update_time) between unix_timestamp(?5) and unix_timestamp(?6)) group by member_code) ta")
	int queyfindMoreLevelStatCountinps(int level,int handicap,String fristTime,String lastTime,String fieldval,String whereTransactionValue);
	//查询出款人数返回
	@Query(nativeQuery = true, value ="select count(1) from (select count(1) from biz_outward_request where handicap=?2 and level=?1 "
		+" and (?3 is null or unix_timestamp(update_time) between unix_timestamp(?3) and unix_timestamp(?4)"
		+" and (?5 is null or unix_timestamp(update_time) between unix_timestamp(?5) and unix_timestamp(?6))) group by member_code) ta")
	int queyfindMoreLevelStatCountoutps(int level,int handicap,String fristTime,String lastTime,String fieldval,String whereTransactionValue);
	
	//查询出款人数返回
	@Query(nativeQuery = true, value ="select handicap,count(1) from (select distinct member,handicap from fundsTransfer.biz_outward_request where create_time between ?1 and ?2)A group by handicap"
			,countQuery = SqlConstants.SEARCH_OUTPERSON_COUNTQUERY)
	Page<Object> findOutPerson(String startTime,String endTime,Pageable pageable);
	
	@Query(nativeQuery = true, value = "select T.*,Y.* from ( select E.*,R.* from ( select Q.*,W.* from  (select handicap in_handicap,count(distinct member_user_name)people,count(1)counts,ifnull(SUM(cast(amount as DECIMAL(18,2))),0)amounts from biz_income_request" 
		+" where status=1 and type<100 and update_time BETWEEN ?2 and ?3 group by handicap)Q"
		+" left join"
		+" (select B.handicap out_handicap,count(distinct B.member),count(distinct B.order_no) ,sum(A.amount) from fundsTransfer.biz_outward_task A,fundsTransfer.biz_outward_request B where B.update_time between ?2 and ?3"
		+" and A.status in (1,5,6) and A.outward_request_id=B.id  group by B.handicap) W on Q.in_handicap=W.out_handicap)E"
		+" left join"
		+" (select sum(amounts),handicap_id "
		+" from (select B.handicap_id,ifnull(SUM(cast(A.amount as DECIMAL(18,2))),0) amounts from biz_bank_log A,biz_account B"
		 +" where A.from_account=B.id and A.status=5 and A.amount<0 and A.trading_time between ?2 and ?3 group by A.from_account)H group by H.handicap_id)R on E.out_handicap=R.handicap_id)T"
		 +" left join"
		+" (select B.handicap_id loss_handicap,count(1)losscounts,sum(A.bal-ifnull(A.amount,0)) lossamounts from biz_account_trace A,biz_account B where A.account_id=B.id"
		 +" and A.create_time between ?2 and ?3 group by B.handicap_id)Y on T.in_handicap=Y.loss_handicap where T.in_handicap in (?1)",countQuery = SqlConstants.SEARCH_QUEYFINDMORESTAT_COUNTQUERY)
	Page<Object> finmorestatFromClearDateRealTime(List<Integer> handicapList,String fristTime,String lastTime,Pageable pageable);
	
	@Query(nativeQuery = true, value = "select handicap,sum(amount)amounts,count(1)counts,count(DISTINCT member_user_name)people,sum(fee)fee"
			   +" from biz_third_request where ack_time between ?1 and ?2 GROUP BY handicap" ,countQuery = SqlConstants.SEARCH_QUEYFINDMORESTAT_COUNTQUERYY)
	Page<Object> findThridIncom(String fristTime,String lastTime,Pageable pageable);
	
	@Query(nativeQuery = true, value = "select handicap_id,sum(amounts),sum(counts) from ( select B.handicap_id,ifnull(SUM(cast(amount as DECIMAL(18,2))),0)amounts,count(1)counts from biz_bank_log A,biz_account B"
      +" where A.from_account=B.id and A.status in(9,10,11) and A.amount<0 and A.trading_time between ?1 and ?2 group by A.from_account)R group by R.handicap_id" ,countQuery = SqlConstants.SEARCH_QUEYFINDMORESTAT_COUNTQUERYY)
	Page<Object> findLossAmounts(String fristTime,String lastTime,Pageable pageable);

}