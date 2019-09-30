package com.xinbo.fundstransfer.domain.repository;
import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.xinbo.fundstransfer.SqlConstants;
import com.xinbo.fundstransfer.domain.entity.AccountStatistics;

public interface FinLessStatisticsRepository
		extends BaseRepository<AccountStatistics, Long> {

	
	/**
	 * 亏损统计    冻结卡统计
	 * @param handicap
	 * @param level
	 * @param account
	 * @param fristTime
	 * @param lastTime
	 * @param fieldval
	 * @param whereTransactionValue
	 * @param pageable
	 * @return
	 */
	@Query(nativeQuery = true, value = "select "
				+"ifnull(A.alias,'无')alias,"
				+"A.handicap_id,"
				+"(select name from biz_handicap where id=A.handicap_id)handicapname,"
				+"(select GROUP_CONCAT(name,'|') from biz_level where id in (select level_id from biz_account_level where account_id=A.id)  and (?2=0 or (handicap_id in (?1) and id=?2))) levelname,"
				+"A.type,"
				+"A.account,"
				+"A.bank_name,"
				+"A.bank_balance,A.id,ifnull(A.owner,'无'),ifnull(A.bank_type,'无'),A.remark,A.curr_sys_level,A.balance" 
				+" from " 
				+" biz_account A"
				+" where A.status=3"
				+" and A.handicap_id in (?1)"
				+" and (?2 is null or A.account like concat('%',?2,'%'))"
				+" and (?3 is null or unix_timestamp(A.update_time) between unix_timestamp(?3) and unix_timestamp(?4))"
				+" and (?5 is null or unix_timestamp(A.update_time) between unix_timestamp(?5) and unix_timestamp(?6))"
				+" and (?8 is null or A.type in (?7))",countQuery = SqlConstants.SEARCH_FROSTLESS_COUNTQUERY)
	Page<Object> queyFinFrostlessStatistics(List<Integer> handicapList,String account,String fristTime,String lastTime,String fieldval,
			String whereTransactionValue,List<Integer> types,String cartype,Pageable pageable);
	//查询总计
	@Query(nativeQuery = true, value =SqlConstants.totalFinFrostlessStatistics)
	java.lang.Object[] totalFinFrostlessStatistics(List<Integer> handicapList,String account,String fristTime,String lastTime,String fieldval,String whereTransactionValue,List<Integer> types,String cartype);
	
	
	@Query(nativeQuery = true, value = "select "
			+"ifnull(A.alias,'无')alias,"
			+"A.handicap_id,"
			+"(select name from biz_handicap where id=A.handicap_id)handicapname,"
			+"(select GROUP_CONCAT(name,'|') from biz_level where id in (select level_id from biz_account_level where account_id=A.id)  and (?2=0 or (handicap_id in (?1) and id=?2))) levelname,"
			+"A.type,"
			+"A.account,"
			+"A.bank_name,"
			+"B.bal,A.id,ifnull(A.owner,'无'),ifnull(A.bank_type,'无'),A.remark,A.curr_sys_level,A.balance,"
			+"date_format(B.create_time,'%Y-%m-%d %H:%i:%s')time,B.operator,B.remark re,B.status pendStatus,B.id pendingId,A.status,B.amount,B.defrost_type,A.sub_type" 
			+" from " 
			+" biz_account A,biz_account_trace B"
			+" where A.id=B.account_id and B.status in (?9) and (Case When ?10=6 then (A.owner like concat('%','3天未启用','%'))  else (?10 is null or B.status=?10) end) and (?11 is null or B.defrost_type=?11)"
			+" and A.handicap_id in (?1)"
			+" and (?2 is null or A.account like concat('%',?2,'%'))"
			+" and (?12 is null or A.alias like concat('%',?12,'%'))"
			+" and (?13 is null or A.flag=?13)"
			+" and (?3 is null or B.create_time between ?3 and ?4)"
			+" and (?5 is null or B.create_time between ?5 and ?6)"
			+" and (?8 is null or A.type in (?7)) order by time desc",countQuery = SqlConstants.SEARCH_PENDING_COUNTQUERY)
	Page<Object> queyFinFrostlessPending(List<Integer> handicapList,String account,String fristTime,String lastTime,String fieldval,
			String whereTransactionValue,List<Integer> types,String cartype,List<Integer> status,String statusType,String jdType,String alias,String classification, Pageable pageable);
	//查询总计
	@Query(nativeQuery = true, value =SqlConstants.totalFinFrostlessPending)
	java.lang.Object[] totalFinFrostlessPending(List<Integer> handicapList,String account,String fristTime,String lastTime,String fieldval,String whereTransactionValue,List<Integer> types,String cartype,List<Integer> status,String statusType,String jdType,String alias,String classification);
	
	
	@Query(nativeQuery = true, value = "select id,account_id,remark,date_format(time,'%Y-%m-%d %H:%i:%s')time,operator from biz_account_extra where account_id=?1",countQuery = SqlConstants.FINDHISTORY)
	Page<Object> findHistory(int accountId,Pageable pageable);
	
	@Query(nativeQuery = true, value = "select count(1) from biz_account_trace where account_id=?1 and ('all'=?2 or status in (0,1,2)) limit 1")
	int findCountsById(int accountId,String type);
	
	@Query(nativeQuery = true, value = "select date_format(create_time,'%Y-%m-%d %H:%i:%s')create_time from biz_bank_log where from_account=?1 order by create_time desc limit 1")
	String findCarCountsById(int accountId);
	
	@Query(nativeQuery = true, value = "select date_format(create_time,'%Y-%m-%d %H:%i:%s')create_time from biz_third_request where to_account=?1 order by create_time desc limit 1")
	String findThirdCountsById(String accountId);
	
	@Query(nativeQuery = true, value = "select remark from biz_account_trace where id=?1")
	String findOldRemark(Long id);
	
	@Query(nativeQuery = true, value = "select status from biz_account_trace where id=?1")
	String findStatus(Long id);
	
	@Query(nativeQuery = true, value = "select bal from biz_account_trace where id=?1")
	BigDecimal findBal(Long id);
	
	//处理
	@Modifying
	@Query(nativeQuery = true, value = "update biz_account_trace set operator=?1,remark=?2,amount=?4,status=2,defrost_type=?5 where id=?3")
	void jieDongMoney(Integer uid, String remark, Long id,BigDecimal amount,String type);
	
	//完成
	@Modifying
	@Query(nativeQuery = true, value = "update biz_account_trace set remark=?1,status=?3 where id=?2")
	void accomplish(String remark, Long id,String status);
	
	//驳回
	@Modifying
	@Query(nativeQuery = true, value = "update biz_account_trace set remark=?1,status=?3,amount=0 where id=?2")
	void accomplishAmount(String remark, Long id,String status);
	
	//金流处理
	@Modifying
	@Query(nativeQuery = true, value = "update biz_account_trace set remark=?1,status=1 where id=?2")
	void cashflow(String remark, Long id);
	
	//冻结时候添加
	@Modifying
	@Query(nativeQuery = true, value = "insert into biz_account_trace(account_id,create_time,status,bal) values (?1,now(),0,?2)")
	void addTrace(Integer id,BigDecimal bankBalance);
	
	//修改账号状态
	@Modifying
	@Query(nativeQuery = true, value = "update biz_account set status=?2,update_time=now() where id=?1")
	void UpdateAccountStatusByid(Long id,int status);
	
	@Modifying
	@Query(nativeQuery = true, value = "update biz_account set balance=?2 where id=?1")
	void UpdateAccountBankBalance(Long id,BigDecimal amount);
	
	@Modifying
	@Query(nativeQuery = true, value = "insert into biz_account_extra(account_id,remark,time,operator) values (?1,?2,now(),?3)")
	void saveAccountExtra(Long id,String remark,String uid);
	
	
	@Query(nativeQuery = true, value = "select "
		 +" (select name from biz_handicap where id=A.handicap_id)handicapname,"
		 +"C.type,"
		 +"C.member_real_name,"
		 +"C.order_no,"
		 +"C.amount,"
		 +"IFNULL(B.fee,0)fee,"
		 +"date_format(C.create_time,'%Y-%m-%d %H:%i:%s')create_time,"
		 +"C.remark"
		 +" from "  
		 +" biz_account A,biz_transaction_log B,biz_income_request C "
		 +" where A.id=B.to_account and B.order_id=C.id and A.id=?6 and (?1 is null or C.member_real_name like concat('%',?1,'%')) "
		 +" and (?2 is null or unix_timestamp(C.update_time) between unix_timestamp(?2) and unix_timestamp(?3)) "
		 +" and (?4=0 or (C.amount>=?4 and C.amount<=?5)) and C.status in (1,4,5) and B.type=?7 and C.type=?7",countQuery = SqlConstants.SEARCH_FINDFININSTATMATCH_COUNTQUERY)
     Page<Object> findFinInStatMatch(String memberrealnamet,String fristTime,String lastTime,BigDecimal startamount,BigDecimal endamount,int id,int type,Pageable pageable);
	 //查询总计
	 @Query(nativeQuery = true, value =SqlConstants.totalfindFinInStatMatch)
	 java.lang.Object[] totalfindFinInStatMatch(String memberrealnamet,String fristTime,String lastTime,BigDecimal startamount,BigDecimal endamount,int id,int type);
	
}