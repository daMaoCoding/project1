package com.xinbo.fundstransfer.domain.repository;
import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.web.bind.annotation.DeleteMapping;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.xinbo.fundstransfer.SqlConstants;
import com.xinbo.fundstransfer.domain.entity.AccountStatistics;
public interface FinBalanceStatRepository extends BaseRepository<AccountStatistics, Long> {

	
	//暂时把可用余额、未下发余额 屏蔽了
	@Query(nativeQuery = true, value = "select id,type,ifnull(sum(bankUse),0),ifnull(sum(bankStop),0),ifnull(sum(bankCanUse),0),ifnull(sum(sysUse),0),ifnull(sum(sysStop),0),ifnull(sum(sysCanUse),0) from (select 1 id,'入款卡' type,(case when status in (1,5) then IFNULL(sum(bank_balance),0) end)'bankUse',(case when status=4 then IFNULL(sum(bank_balance),0) end)'bankStop',(case when status=3 then IFNULL(sum(bank_balance),0) end)'bankCanUse',(case when status in (1,5) then IFNULL(sum(balance),0) end)'sysUse',(case when status=4 then IFNULL(sum(balance),0) end)'sysStop',(case when status=3 then IFNULL(sum(balance),0) end)'sysCanUse' from biz_account where type=1 and status in (1,2,3,4,5) group by status)A group by id"
			+" union"
			+" select id,type,ifnull(sum(bankUse),0),ifnull(sum(bankStop),0),ifnull(sum(bankCanUse),0),ifnull(sum(sysUse),0),ifnull(sum(sysStop),0),ifnull(sum(sysCanUse),0) from (select 2 id,'出款卡' type,(case when status in (1,5) then IFNULL(sum(bank_balance),0) end)'bankUse',(case when status=4 then IFNULL(sum(bank_balance),0) end)'bankStop',(case when status=3 then IFNULL(sum(bank_balance),0) end)'bankCanUse',(case when status in (1,5) then IFNULL(sum(balance),0) end)'sysUse',(case when status=4 then IFNULL(sum(balance),0) end)'sysStop',(case when status=3 then IFNULL(sum(balance),0) end)'sysCanUse' from biz_account where type=5 and status in (1,2,3,4,5) group by status)A group by id"
			+" union"
			+" select id,type,ifnull(sum(bankUse),0),ifnull(sum(bankStop),0),ifnull(sum(bankCanUse),0),ifnull(sum(sysUse),0),ifnull(sum(sysStop),0),ifnull(sum(sysCanUse),0) from (select 3 id,'下发卡' type,(case when status in (1,5) then IFNULL(sum(bank_balance),0) end)'bankUse',(case when status=4 then IFNULL(sum(bank_balance),0) end)'bankStop',(case when status=3 then IFNULL(sum(bank_balance),0) end)'bankCanUse',(case when status in (1,5) then IFNULL(sum(balance),0) end)'sysUse',(case when status=4 then IFNULL(sum(balance),0) end)'sysStop',(case when status=3 then IFNULL(sum(balance),0) end)'sysCanUse' from biz_account where type=13 and status in (1,2,3,4,5) group by status)A group by id"
			+" union"
			+" select id,type,ifnull(sum(bankUse),0),ifnull(sum(bankStop),0),ifnull(sum(bankCanUse),0),ifnull(sum(sysUse),0),ifnull(sum(sysStop),0),ifnull(sum(sysCanUse),0) from (select 4 id,'备用卡' type,(case when status in (1,5) then IFNULL(sum(bank_balance),0) end)'bankUse',(case when status=4 then IFNULL(sum(bank_balance),0) end)'bankStop',(case when status=3 then IFNULL(sum(bank_balance),0) end)'bankCanUse',(case when status in (1,5) then IFNULL(sum(balance),0) end)'sysUse',(case when status=4 then IFNULL(sum(balance),0) end)'sysStop',(case when status=3 then IFNULL(sum(balance),0) end)'sysCanUse' from biz_account where type=8 and status in (1,2,3,4,5) group by status)A group by id"
			+" union"
			+" select id,type,ifnull(sum(bankUse),0),ifnull(sum(bankStop),0),ifnull(sum(bankCanUse),0),ifnull(sum(sysUse),0),ifnull(sum(sysStop),0),ifnull(sum(sysCanUse),0) from (select 5 id,'现金卡' type,(case when status in (1,5) then IFNULL(sum(bank_balance),0) end)'bankUse',(case when status=4 then IFNULL(sum(bank_balance),0) end)'bankStop',(case when status=3 then IFNULL(sum(bank_balance),0) end)'bankCanUse',(case when status in (1,5) then IFNULL(sum(balance),0) end)'sysUse',(case when status=4 then IFNULL(sum(balance),0) end)'sysStop',(case when status=3 then IFNULL(sum(balance),0) end)'sysCanUse' from biz_account where type=9 and status in (1,2,3,4,5) group by status)A group by id"
			//+" union"
			//+" select id,type,ifnull(sum(bankUse),0),ifnull(sum(bankStop),0),ifnull(sum(bankCanUse),0),ifnull(sum(sysUse),0),ifnull(sum(sysStop),0),ifnull(sum(sysCanUse),0) from (select 6 id,'公司入款余额' type,(case when status in (1,5) then IFNULL(sum(bank_balance),0) end)'bankUse',(case when status=4 then IFNULL(sum(bank_balance),0) end)'bankStop',(case when status=3 then IFNULL(sum(bank_balance),0) end)'bankCanUse',(case when status in (1,5) then IFNULL(sum(balance),0) end)'sysUse',(case when status=4 then IFNULL(sum(balance),0) end)'sysStop',(case when status=3 then IFNULL(sum(balance),0) end)'sysCanUse' from biz_account where type in (1,3,4) and status in (1,2,3,4,5) group by status)A group by id"
			+" union"
			+" select id,type,ifnull(sum(bankUse),0),ifnull(sum(bankStop),0),ifnull(sum(bankCanUse),0),ifnull(sum(sysUse),0),ifnull(sum(sysStop),0),ifnull(sum(sysCanUse),0) from (select 7 id,'第三方入款余额' type,(case when status in (1,5) then IFNULL(sum(bank_balance),0) end)'bankUse',(case when status=4 then IFNULL(sum(bank_balance),0) end)'bankStop',(case when status=3 then IFNULL(sum(bank_balance),0) end)'bankCanUse',(case when status in (1,5) then IFNULL(sum(balance),0) end)'sysUse',(case when status=4 then IFNULL(sum(balance),0) end)'sysStop',(case when status=3 then IFNULL(sum(balance),0) end)'sysCanUse' from biz_account where type=2 and status in (1,2,3,4,5) group by status)A group by id"
			//+" union "
			//+"select 7 id,'未下发余额',IFNULL(sum(bank_balance),0) from biz_account where type in (1,2,3,4) and status in (1,2,4,5)"
			//+" union "
			//+"select 8 id,'可用余额',IFNULL(sum(bank_balance),0) from biz_account where type=5 and status=1"+
		,countQuery =SqlConstants.totalfinBalanceStat)
	Page<Object> queyfinBalanceStat(Pageable pageable);
	
	@Query(nativeQuery = true, value = "select * from (select id,type,time,ifnull(sum(bankUse),0),ifnull(sum(bankStop),0),ifnull(sum(bankCanUse),0),ifnull(sum(sysUse),0),ifnull(sum(sysStop),0),ifnull(sum(sysCanUse),0) from (select 1 id,'入款卡' type,(case when status in (1,5) then IFNULL(sum(B.balance),0) end)'bankUse',(case when status=4 then IFNULL(sum(B.balance),0) end)'bankStop',(case when status=3 then IFNULL(sum(B.balance),0) end)'bankCanUse',(case when status in (1,5) then IFNULL(sum(B.sys_balance),0) end)'sysUse',(case when status=4 then IFNULL(sum(B.sys_balance),0) end)'sysStop',(case when status=3 then IFNULL(sum(B.sys_balance),0) end)'sysCanUse',date_format(B.time, '%Y-%m-%d')time from biz_account A,biz_report B where A.id=B.account_id and A.type=1 and A.status in (1,2,3,4,5) and (?1=0 or A.handicap_id=?1) and (?2 is null or (date_format(B.time, '%Y-%m-%d') between ?2 and ?3))  group by A.status,B.time)A group by id,time"
			+" union"
			+" select id,type,time,ifnull(sum(bankUse),0),ifnull(sum(bankStop),0),ifnull(sum(bankCanUse),0),ifnull(sum(sysUse),0),ifnull(sum(sysStop),0),ifnull(sum(sysCanUse),0) from (select 2 id,'出款卡' type,(case when status in (1,5) then IFNULL(sum(B.balance),0) end)'bankUse',(case when status=4 then IFNULL(sum(B.balance),0) end)'bankStop',(case when status=3 then IFNULL(sum(B.balance),0) end)'bankCanUse',(case when status in (1,5) then IFNULL(sum(B.sys_balance),0) end)'sysUse',(case when status=4 then IFNULL(sum(B.sys_balance),0) end)'sysStop',(case when status=3 then IFNULL(sum(B.sys_balance),0) end)'sysCanUse',date_format(B.time, '%Y-%m-%d')time from biz_account A,biz_report B where A.id=B.account_id and A.type=5 and A.status in (1,2,3,4,5) and (?1=0 or A.handicap_id=?1) and (?2 is null or (date_format(B.time, '%Y-%m-%d') between ?2 and ?3))  group by A.status,B.time)A group by id,time"
			+" union"
			+" select id,type,time,ifnull(sum(bankUse),0),ifnull(sum(bankStop),0),ifnull(sum(bankCanUse),0),ifnull(sum(sysUse),0),ifnull(sum(sysStop),0),ifnull(sum(sysCanUse),0) from (select 3 id,'下发卡' type,(case when status in (1,5) then IFNULL(sum(B.balance),0) end)'bankUse',(case when status=4 then IFNULL(sum(B.balance),0) end)'bankStop',(case when status=3 then IFNULL(sum(B.balance),0) end)'bankCanUse',(case when status in (1,5) then IFNULL(sum(B.sys_balance),0) end)'sysUse',(case when status=4 then IFNULL(sum(B.sys_balance),0) end)'sysStop',(case when status=3 then IFNULL(sum(B.sys_balance),0) end)'sysCanUse',date_format(B.time, '%Y-%m-%d')time from biz_account A,biz_report B where A.id=B.account_id and A.type=13 and A.status in (1,2,3,4,5) and (?1=0 or A.handicap_id=?1) and (?2 is null or (date_format(B.time, '%Y-%m-%d') between ?2 and ?3))  group by A.status,B.time)A group by id,time"
			+" union"
			+" select id,type,time,ifnull(sum(bankUse),0),ifnull(sum(bankStop),0),ifnull(sum(bankCanUse),0),ifnull(sum(sysUse),0),ifnull(sum(sysStop),0),ifnull(sum(sysCanUse),0) from (select 4 id,'备用卡' type,(case when status in (1,5) then IFNULL(sum(B.balance),0) end)'bankUse',(case when status=4 then IFNULL(sum(B.balance),0) end)'bankStop',(case when status=3 then IFNULL(sum(B.balance),0) end)'bankCanUse',(case when status in (1,5) then IFNULL(sum(B.sys_balance),0) end)'sysUse',(case when status=4 then IFNULL(sum(B.sys_balance),0) end)'sysStop',(case when status=3 then IFNULL(sum(B.sys_balance),0) end)'sysCanUse',date_format(B.time, '%Y-%m-%d')time from biz_account A,biz_report B where A.id=B.account_id and A.type=8 and A.status in (1,2,3,4,5) and (?1=0 or A.handicap_id=?1) and (?2 is null or (date_format(B.time, '%Y-%m-%d') between ?2 and ?3))  group by A.status,B.time)A group by id,time"
			+" union"
			+" select id,type,time,ifnull(sum(bankUse),0),ifnull(sum(bankStop),0),ifnull(sum(bankCanUse),0),ifnull(sum(sysUse),0),ifnull(sum(sysStop),0),ifnull(sum(sysCanUse),0) from (select 5 id,'现金卡' type,(case when status in (1,5) then IFNULL(sum(B.balance),0) end)'bankUse',(case when status=4 then IFNULL(sum(B.balance),0) end)'bankStop',(case when status=3 then IFNULL(sum(B.balance),0) end)'bankCanUse',(case when status in (1,5) then IFNULL(sum(B.sys_balance),0) end)'sysUse',(case when status=4 then IFNULL(sum(B.sys_balance),0) end)'sysStop',(case when status=3 then IFNULL(sum(B.sys_balance),0) end)'sysCanUse',date_format(B.time, '%Y-%m-%d')time from biz_account A,biz_report B where A.id=B.account_id and A.type=9 and A.status in (1,2,3,4,5) and (?1=0 or A.handicap_id=?1) and (?2 is null or (date_format(B.time, '%Y-%m-%d') between ?2 and ?3))  group by A.status,B.time)A group by id,time"
			//+" union"
			//+" select id,type,time,ifnull(sum(bankUse),0),ifnull(sum(bankStop),0),ifnull(sum(bankCanUse),0),ifnull(sum(sysUse),0),ifnull(sum(sysStop),0),ifnull(sum(sysCanUse),0) from (select 6 id,'公司入款余额' type,(case when status in (1,5) then IFNULL(sum(B.balance),0) end)'bankUse',(case when status=4 then IFNULL(sum(B.balance),0) end)'bankStop',(case when status=3 then IFNULL(sum(B.balance),0) end)'bankCanUse',(case when status in (1,5) then IFNULL(sum(B.sys_balance),0) end)'sysUse',(case when status=4 then IFNULL(sum(B.sys_balance),0) end)'sysStop',(case when status=3 then IFNULL(sum(B.sys_balance),0) end)'sysCanUse',date_format(B.time, '%Y-%m-%d')time from biz_account A,biz_report B where A.id=B.account_id and A.type in (1,3,4) and A.status in (1,2,3,4,5) and (?1=0 or A.handicap_id=?1) and (?2 is null or (date_format(B.time, '%Y-%m-%d') between ?2 and ?3))  group by A.status,B.time)A group by id,time"
			+" union"
			+" select id,type,time,ifnull(sum(bankUse),0),ifnull(sum(bankStop),0),ifnull(sum(bankCanUse),0),ifnull(sum(sysUse),0),ifnull(sum(sysStop),0),ifnull(sum(sysCanUse),0) from (select 7 id,'第三方入款余额' type,(case when status in (1,5) then IFNULL(sum(B.balance),0) end)'bankUse',(case when status=4 then IFNULL(sum(B.balance),0) end)'bankStop',(case when status=3 then IFNULL(sum(B.balance),0) end)'bankCanUse',(case when status in (1,5) then IFNULL(sum(B.sys_balance),0) end)'sysUse',(case when status=4 then IFNULL(sum(B.sys_balance),0) end)'sysStop',(case when status=3 then IFNULL(sum(B.sys_balance),0) end)'sysCanUse',date_format(B.time, '%Y-%m-%d')time from biz_account A,biz_report B where A.id=B.account_id and A.type=2 and A.status in (1,2,3,4,5) and (?1=0 or A.handicap_id=?1) and (?2 is null or (date_format(B.time, '%Y-%m-%d') between ?2 and ?3))  group by A.status,B.time)A group by id,time)A order by time desc"
			//+" union "
			//+"select 7 id,'未下发余额',IFNULL(sum(bank_balance),0) from biz_account where type in (1,2,3,4) and status in (1,2,4,5)"
			//+" union "
			//+"select 8 id,'可用余额',IFNULL(sum(bank_balance),0) from biz_account where type=5 and status=1"+
		,countQuery =SqlConstants.totalfinBalanceStat)
	Page<Object> finbalanceEveryDay(int handicapId,String startTime,String endTime,Pageable pageable);
	
	@Query(nativeQuery = true, value = "select A.id,ifnull(B.balance,0),ifnull(B.sys_balance,0),A.status,date_format(B.time, '%Y-%m-%d') from biz_account A,biz_report B "
			+" where A.id=B.account_id and A.type in (?5) and (?1 is null or A.account like concat('%',?1,'%')) and (?2 is null or A.bank_type=?2) and A.status in (?6) and (?3=0 or A.handicap_id=?3)  and date_format(B.time, '%Y-%m-%d') = ?4"
			+" order by abs((ifnull(B.balance,0)-ifnull(B.sys_balance,0))) desc",countQuery = SqlConstants.SEARCH_FINDREBATE_DEAL)
	Page<Object> findBalanceDetail(String account, String bankType, int handicap, String time, List<Integer> type,
			List<Integer> statuss, Pageable pageable);
	//查询总计
	@Query(nativeQuery = true, value =SqlConstants.totalFindDetail)
	java.lang.Object[] totalFindBalanceDetail(String account, String bankType, int handicap, String time, List<Integer> type,List<Integer> statuss);
	
	
	/**
	 * 余额明细>明细 //入款账号的明细
	 * @param id
	 * @param account
	 * @param pageable
	 * @return
	 */
	@Query(nativeQuery = true, value = "select A.id,A.account,A.type,A.bank_name,IFNULL(A.balance,0)balance,IFNULL(A.bank_balance,0)bank_balance"
			+",ifnull(A.alias,'无')alias,ifnull(A.owner,'无'),ifnull(A.bank_type,'无')"
			+" from " 
			+"biz_account A"  
			+" where type=1 and status in (?2) and (?1 is null or A.account like concat('%',?1,'%')) and (?3 is null or A.bank_type=?3)"
			+" order by abs((IFNULL(A.bank_balance,0)-IFNULL(A.balance,0))) desc",countQuery = SqlConstants.SEARCH_QUEYFINBALANCESTATCARD_COUNTQUERY)
	Page<Object> queyfinBalanceStatCardIn(String account,List<Integer> statuss,String bankType, Pageable pageable);
	//查询总计
	@Query(nativeQuery = true, value =SqlConstants.totalfinBalanceStatCard)
	java.lang.Object[] totalfinBalanceStatCardIn(String account,List<Integer> statuss,String bankType);
	
	
	//出款账号的明细
	@Query(nativeQuery = true, value = "select A.id,A.account,A.type,A.bank_name,IFNULL(A.balance,0)balance,IFNULL(A.bank_balance,0)bank_balance"
			+",ifnull(A.alias,'无')alias,ifnull(A.owner,'无'),ifnull(A.bank_type,'无')"
			+" from " 
			+"biz_account A"  
			+" where type=5 and status in (?2) and (?1 is null or A.account like concat('%',?1,'%')) and (?3 is null or A.bank_type=?3)"
			+" order by abs((IFNULL(A.bank_balance,0)-IFNULL(A.balance,0))) desc",countQuery = SqlConstants.SEARCH_QUEYFINBALANCESTATCARD_COUNTQUERYOUT)
	Page<Object> queyfinBalanceStatCardOut(String account,List<Integer> statuss,String bankType, Pageable pageable);
	//查询总计
	@Query(nativeQuery = true, value =SqlConstants.totalfinBalanceStatCardOUT)
	java.lang.Object[] totalfinBalanceStatCardOut(String account,List<Integer> statuss,String bankType);
	
	
	//备用金卡的明细&//现金卡的明细 根据type去判断&//第三方入款余额明细
	@Query(nativeQuery = true, value = "select A.id,A.account,A.type,A.bank_name,IFNULL(A.balance,0)balance,IFNULL(A.bank_balance,0)bank_balance"
			+",ifnull(A.alias,'无')alias,ifnull(A.owner,'无'),ifnull(A.bank_type,'无')"
			+" from " 
			+"biz_account A"  
			+" where type in (?1) and status in (?3) and (?2 is null or A.account like concat('%',?2,'%')) and (?4 is null or A.bank_type=?4)"
			+" order by abs((IFNULL(A.bank_balance,0)-IFNULL(A.balance,0))) desc",countQuery = SqlConstants.SEARCH_QUEYFINBALANCESTATCARD_COUNTQUERYReserveBank)
	Page<Object> queyfinBalanceStatCardReserveBank(List<Integer> type,String account,List<Integer> statuss,String bankType, Pageable pageable);
	//查询总计
	@Query(nativeQuery = true, value =SqlConstants.totalfinBalanceStatCardReserveBank)
	java.lang.Object[] totalfinBalanceStatCardReserveBank(List<Integer> type,String account,List<Integer> statuss,String bankType);
	
	
	//公司入款余额明细
	@Query(nativeQuery = true, value = "select A.id,A.account,A.type,A.bank_name,IFNULL(A.balance,0)balance,IFNULL(A.bank_balance,0)bank_balance"
			+",ifnull(A.alias,'无')alias,ifnull(A.owner,'无'),ifnull(A.bank_type,'无')"
			+" from " 
			+"biz_account A"  
			+" where type in (1,3,4) and status=?2 and (?1 is null or A.account like concat('%',?1,'%'))",countQuery = SqlConstants.SEARCH_QUEYFINBALANCESTATCARD_COUNTQUERYcompany)
	Page<Object> queyfinBalanceStatCardcompany(String account,int status,Pageable pageable);
	//查询总计
	@Query(nativeQuery = true, value =SqlConstants.totalfinBalanceStatCardcompany)
	java.lang.Object[] totalfinBalanceStatCardcompany(String account,int status);
	
	@Query(nativeQuery = true, value = "select A.id,A.account,A.type,A.bank_name,IFNULL(A.balance,0)balance,IFNULL(A.bank_balance,0)bank_balance"
			+" from " 
			+"biz_account A"  
			+" where type=2 and status in (?2) and (?1 is null or A.account like concat('%',?1,'%')) and (?3 is null or A.bank_type=?3)"
			+" order by abs((IFNULL(A.bank_balance,0)-IFNULL(A.balance,0))) desc",countQuery = SqlConstants.SEARCH_QUEYFINBALANCESTATCARD_COUNTQUERYNotissued)
	Page<Object> queyfinBalanceStatCardNotissued(String account,List<Integer> statuss,String bankType, Pageable pageable);
	//查询总计
	@Query(nativeQuery = true, value =SqlConstants.totalfinBalanceStatCardNotissued)
	java.lang.Object[] totalfinBalanceStatCardNotissued(String account,List<Integer> statuss,String bankType);
	
	/**
	 * 余额明细>明细>系统明细 入款账号
	 * @param to_account
	 * @param from_account
	 * @param fristTime
	 * @param lastTime
	 * @param startamount
	 * @param endamount
	 * @param accountid
	 * @param id
	 * @param type
	 * @param pageable
	 * @return
	 */
	////入款账号数据源
	@Query(nativeQuery = true, value = "select A.*, ifnull(B.username,'') operatorname  from ( select 1, "  
			  +" A.to_account," 
			  +"B.member_real_name from_accountname," 
			  +"A.from_account," 
			  +"account to_accountname," 
			  +"IFNULL(A.amount,0)amount," 
			  +"IFNULL(A.fee,0)fee," 
			  +"date_format(A.create_time,'%Y-%m-%d %H:%i:%s') create_time," 
			  +"B.remark," 
			  +"A.operator"
			  +" from "   
			  +" biz_transaction_log A,biz_income_request B,biz_account C" 
			  +" where A.order_id=B.id and C.id=A.to_account and A.type in(1,2,3,4)  and B.type in (1,2,3,4) and B.status in (1,4,5)"
			  +" and (?1 is null or B.member_real_name like concat('%',?1,'%')) and (?2 is null or account like concat('%',?2,'%'))"
			  +" and (?5=0 or (A.amount>=?5 and A.amount<=?6)) and (?3 is null or A.create_time between ?3 and ?4) and (?7=0 or A.to_account=?7)) A"
            +" LEFT JOIN sys_user B on A.operator=B.id order by A.create_time desc",countQuery = SqlConstants.SEARCH_QUEYFINTRANSBALANCESYS_COUNTQUERY)
	Page<Object> queyfinTransBalanceInSys(String to_account,String from_account,String fristTime,
			String lastTime,BigDecimal startamount,BigDecimal endamount,int accountid,Pageable pageable);
	//查询总计
	@Query(nativeQuery = true, value =SqlConstants.totalfinTransBalanceSys)
	java.lang.Object[] totalfinTransBalanceInSys(String to_account,String from_account,String fristTime,
			String lastTime,BigDecimal startamount,BigDecimal endamount,int accountid);
	
	//出款账号系统明细
	@Query(nativeQuery = true, value = "select * from ( select "
			+"A.id,"
			+"A.to_account,"
			+"(select account from biz_account where id=A.from_account) from_accountname,"
			+"A.from_account,"
			+"C.to_account to_accountname,"
			+"IFNULL(A.amount,0)amount,"
			+"IFNULL(A.fee,0)fee,"
			+"date_format(A.create_time,'%Y-%m-%d %H:%i:%s') create_time,"
			+"B.remark,"
			+"A.operator,"
			+"(select username from sys_user where id=A.operator)operatorname"
			+" from " 
			+" biz_transaction_log A,biz_outward_task B,fundsTransfer.biz_outward_request C"
			+" where A.order_id=B.id and B.outward_request_id=C.id and A.type=0  and B.status=5 and C.status in (5,6) "
			+" and (?3 is null or unix_timestamp(A.create_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+" and (?5=0 or (A.amount>=?5 and A.amount<=?6))) A where (?7=0 or A.from_account=?7) and (?1 is null or A.from_accountname like concat('%',?1,'%')) and (?2 is null or A.to_accountname like concat('%',?2,'%'))",countQuery = SqlConstants.SEARCH_QUEYFINTRANSBALANCEOUTSYS_COUNTQUERY)
	Page<Object> queyfinTransBalanceOutSys(String to_account,String from_account,String fristTime,
			String lastTime,BigDecimal startamount,BigDecimal endamount,int accountid,Pageable pageable);
	//查询总计
	@Query(nativeQuery = true, value =SqlConstants.totalfinTransBalanceOutSys)
	java.lang.Object[] totalfinTransBalanceOutSys(String to_account,String from_account,String fristTime,
			String lastTime,BigDecimal startamount,BigDecimal endamount,int accountid);
	
	@Query(nativeQuery = true, value = "select * from ( select "
			+"A.id,"
			+"A.to_account,"
			+"(select account from biz_account where id=A.from_account) from_accountname,"
			+"A.from_account,"
			+"(select account from biz_account where id=A.to_account)to_accountname,"
			+"IFNULL(A.amount,0)amount,"
			+"IFNULL(A.fee,0)fee,"
			+"date_format(A.create_time,'%Y-%m-%d %H:%i:%s') create_time,"
			+"B.remark,"
			+"A.operator,"
			+"(select username from sys_user where id=A.operator)operatorname"
			+" from " 
			+" biz_transaction_log A,biz_outward_task B"
			+" where A.order_id=B.id and A.type=0  and B.status=5"
			+" and (?3 is null or unix_timestamp(A.create_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+" and (?5=0 or (A.amount>=?5 and A.amount<=?6))) A where (?7=0 or A.from_account=?7) and (?1 is null or A.from_accountname like concat('%',?1,'%')) and (?2 is null or A.to_accountname like concat('%',?2,'%'))",countQuery = SqlConstants.SEARCH_QUEYFINTRANSBALANCEOUTSYS_COUNTQUERY)
	Page<Object> queyfinTransBalanceReserveBankSys(String to_account,String from_account,String fristTime,
			String lastTime,BigDecimal startamount,BigDecimal endamount,int accountid,Pageable pageable);
	//查询总计
	@Query(nativeQuery = true, value =SqlConstants.totalfinTransBalanceOutSys)
	java.lang.Object[] totalfinTransBalanceReserveBankSys(String to_account,String from_account,String fristTime,
			String lastTime,BigDecimal startamount,BigDecimal endamount,int accountid);
	
	
	//现金卡系统明细
	@Query(nativeQuery = true, value = "select * from ( select "
			+"A.id,"
			+"A.to_account,"
			+"B.member_real_name from_accountname,"
			+"A.from_account,"
			+"(select account from biz_account where id=A.to_account)to_accountname,"
			+"IFNULL(A.amount,0)amount,"
			+"IFNULL(A.fee,0)fee,"
			+"date_format(A.create_time,'%Y-%m-%d %H:%i:%s') create_time,"
			+"B.remark,"
			+"A.operator,"
			+"ifnull((select username from sys_user where id=A.operator),'')operatorname"
			+" from " 
			+" biz_transaction_log A,biz_income_request B"
			+" where A.order_id=B.id and B.status in (1,4,5)"
			+" and (?3 is null or unix_timestamp(A.create_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+" and (?5=0 or (A.amount>=?5 and A.amount<=?6))) A where (?7=0 or A.to_account=?7) and (?1 is null or A.from_accountname like concat('%',?1,'%')) and (?2 is null or A.to_accountname like concat('%',?2,'%'))",countQuery = SqlConstants.SEARCH_QUEYFINTRANSBALANCESYS_COUNTQUERYCashBank)
	Page<Object> queyfinTransBalanceCashBankSys(String to_account,String from_account,String fristTime,
			String lastTime,BigDecimal startamount,BigDecimal endamount,int accountid,Pageable pageable);
	//查询总计
	@Query(nativeQuery = true, value =SqlConstants.totalfinTransBalanceSysCashBank)
	java.lang.Object[] totalfinTransBalanceCashBankSys(String to_account,String from_account,String fristTime,
			String lastTime,BigDecimal startamount,BigDecimal endamount,int accountid);
	
	//公司入款余额系统明细
	@Query(nativeQuery = true, value = "select * from ( select "
			+"A.id,"
			+"A.to_account,"
			+"B.member_real_name from_accountname,"
			+"A.from_account,"
			+"(select account from biz_account where id=A.to_account)to_accountname,"
			+"IFNULL(A.amount,0)amount,"
			+"IFNULL(A.fee,0)fee,"
			+"date_format(A.create_time,'%Y-%m-%d %H:%i:%s') create_time,"
			+"B.remark,"
			+"A.operator,"
			+"ifnull((select username from sys_user where id=A.operator),'')operatorname"
			+" from " 
			+" biz_transaction_log A,biz_income_request B"
			+" where A.order_id=B.id and A.type in (1,2,3) and B.type in (1,2,3) and B.status in (1,4,5)"
			+" and (?3 is null or unix_timestamp(A.create_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+" and (?5=0 or (A.amount>=?5 and A.amount<=?6))) A where (?7=0 or A.to_account=?7) and (?1 is null or A.from_accountname like concat('%',?1,'%')) and (?2 is null or A.to_accountname like concat('%',?2,'%'))",countQuery = SqlConstants.SEARCH_QUEYFINTRANSBALANCESYS_COUNTQUERYCompany)
	Page<Object> queyfinTransBalanceCompanySys(String to_account,String from_account,String fristTime,
			String lastTime,BigDecimal startamount,BigDecimal endamount,int accountid,Pageable pageable);
	//查询总计
	@Query(nativeQuery = true, value =SqlConstants.totalfinTransBalanceSysCompany)
	java.lang.Object[] totalfinTransBalanceCompanySys(String to_account,String from_account,String fristTime,
			String lastTime,BigDecimal startamount,BigDecimal endamount,int accountid);
	
	//第三方入款余额系统明细
	@Query(nativeQuery = true, value = "select A.id,A.to_account,A.from_accountname,A.from_account,A.to_accountname,A.amount,"
			+"A.fee,A.create_time,A.remark,A.operator,ifnull(B.username,'') from ( select * from ( select "
			+"B.id,"
			+"B.to_account,"
			+"B.member_user_name from_accountname,"
			+"B.from_account,"
			+"B.to_account to_accountname,"
			+"IFNULL(B.amount,0)amount,"
			+"IFNULL(B.fee,0)fee,"
			+"date_format(B.create_time,'%Y-%m-%d %H:%i:%s') create_time,"
			+"B.remark,"
			+"333 operator"
			+" from " 
			+" biz_third_request B"
			+" where "
			+" (?3 is null or unix_timestamp(B.create_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+" and (?5=0 or (B.amount>=?5 and B.amount<=?6))) A where (?7=0 or A.to_account=?7) and (?1 is null or A.from_accountname like concat('%',?1,'%')) and (?2 is null or A.to_accountname like concat('%',?2,'%')))A"
			+" LEFT JOIN sys_user B on A.operator=B.id",countQuery = SqlConstants.SEARCH_QUEYFINTRANSBALANCESYS_COUNTQUERYThird)
	Page<Object> queyfinTransBalanceThirdSys(String to_account,String from_account,String fristTime,
			String lastTime,BigDecimal startamount,BigDecimal endamount,String accountname,Pageable pageable);
	//查询总计
	@Query(nativeQuery = true, value =SqlConstants.totalfinTransBalanceSysThird)
	java.lang.Object[] totalfinTransBalanceThirdSys(String to_account,String from_account,String fristTime,
			String lastTime,BigDecimal startamount,BigDecimal endamount,String accountname);
	
	
	//未下发余额系统明细
	@Query(nativeQuery = true, value = "select * from ( select "
			+"A.id,"
			+"A.to_account,"
			+"B.member_real_name from_accountname,"
			+"A.from_account,"
			+"(select account from biz_account where id=A.to_account)to_accountname,"
			+"IFNULL(A.amount,0)amount,"
			+"IFNULL(A.fee,0)fee,"
			+"date_format(A.create_time,'%Y-%m-%d %H:%i:%s') create_time,"
			+"B.remark,"
			+"A.operator,"
			+"(select username from sys_user where id=A.operator)operatorname"
			+" from " 
			+" biz_transaction_log A,biz_income_request B"
			+" where A.order_id=B.id and A.type in (1,2,3,4) and B.type in (1,2,3,4) and B.status in (1,4,5)"
			+" and (?3 is null or unix_timestamp(A.create_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+" and (?5=0 or (A.amount>=?5 and A.amount<=?6))) A where (?7=0 or A.to_account=?7) and (?1 is null or A.from_accountname like concat('%',?1,'%')) and (?2 is null or A.to_accountname like concat('%',?2,'%'))",countQuery = SqlConstants.SEARCH_QUEYFINTRANSBALANCESYS_COUNTQUERYNotissued)
	Page<Object> queyfinTransBalanceNotissuedSys(String to_account,String from_account,String fristTime,
			String lastTime,BigDecimal startamount,BigDecimal endamount,int accountid,Pageable pageable);
	//查询总计
	@Query(nativeQuery = true, value =SqlConstants.totalfinTransBalanceSysNotissued)
	java.lang.Object[] totalfinTransBalanceNotissuedSys(String to_account,String from_account,String fristTime,
			String lastTime,BigDecimal startamount,BigDecimal endamount,int accountid);
	
	
	//备用金卡系统明细
	@Query(nativeQuery = true, value = "select * from ( select * from ( select "
			+"A.id,"
			+"A.to_account,"
			+"B.member_real_name from_accountname,"
			+"A.from_account,"
			+"(select account from biz_account where id=A.to_account)to_accountname,"
			+"IFNULL(A.amount,0)amount,"
			+"IFNULL(A.fee,0)fee,"
			+"date_format(A.create_time,'%Y-%m-%d %H:%i:%s') create_time,"
			+"B.remark,"
			+"A.operator,"
			+"(select username from sys_user where id=A.operator)operatorname"
			+" from " 
			+" biz_transaction_log A,biz_income_request B"
			+" where A.order_id=B.id and B.status in (1,4,5)"
			+" and (?3 is null or unix_timestamp(A.create_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+" and (?5=0 or (A.amount>=?5 and A.amount<=?6))) A where (?7=0 or A.to_account=?7) and (?1 is null or A.from_accountname like concat('%',?1,'%')) and (?2 is null or A.to_accountname like concat('%',?2,'%'))"
			+" union "
			+"select * from ( select "
			+"C.id,"
			+"C.to_account,"
			+"(select account from biz_account where id=C.from_account) from_accountname,"
			+"C.from_account,"
			+"A.to_account to_accountname,"
			+"IFNULL(C.amount,0)amount,"
			+"IFNULL(C.fee,0)fee,"
			+"date_format(C.create_time,'%Y-%m-%d %H:%i:%s') create_time,"
			+"B.remark,"
			+"C.operator,"
			+"(select username from sys_user where id=C.operator)operatorname"
			+" from " 
			+" biz_transaction_log C,biz_outward_task B,fundsTransfer.biz_outward_request A"
			+" where C.order_id=B.id and B.outward_request_id=A.id and C.type=0  and B.status=5 and A.status in (5,6) "
			+" and (?3 is null or unix_timestamp(C.create_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+" and (?5=0 or (C.amount>=?5 and C.amount<=?6))) C where (?7=0 or C.from_account=?7) and (?1 is null or C.from_accountname like concat('%',?1,'%')) and (?2 is null or C.to_accountname like concat('%',?2,'%'))) as A",countQuery = SqlConstants.SEARCH_QUEYFINTRANSBALANCEPettycash_COUNTQUERY)
	Page<Object> queyfinTransBalanceReservePettycashSys(String to_account,String from_account,String fristTime,
			String lastTime,BigDecimal startamount,BigDecimal endamount,int accountid,Pageable pageable);
	//查询总计
	@Query(nativeQuery = true, value =SqlConstants.totalfinTransBalancePettycash)
	java.lang.Object[] totalfinTransBalanceReservePettycashSys(String to_account,String from_account,String fristTime,
			String lastTime,BigDecimal startamount,BigDecimal endamount,int accountid);
	
	/**
	 * 余额明细>明细>银行明细
	 * @param to_account
	 * @param from_account
	 * @param fristTime
	 * @param lastTime
	 * @param startamount
	 * @param endamount
	 * @param accountid
	 * @param id
	 * @param type
	 * @param pageable
	 * @return
	 */
	////入款账号数据源
	@Query(nativeQuery = true, value = "select * from (select "
			+"A.id,"
			+"A.from_account,"
			+"A.to_account,"
			+"(select account from biz_account where id=A.from_account)from_accountname,"
			+"IFNULL(A.amount,0)amount,"
			+"IFNULL(0.00,0)fee,"
			+"date_format(A.trading_time,'%Y-%m-%d %H:%i:%s') trading_time "
			+" from " 
			+" biz_bank_log A where A.status=1 and A.amount>0) A "
			+" where (?1 is null or A.to_account like concat('%',?1,'%')) and (?2 is null or A.from_accountname like concat('%',?2,'%'))"
			+" and (?3 is null or unix_timestamp(A.trading_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+" and (?5=0 or (A.amount>=?5 and A.amount<=?6)) and A.from_account=?7",countQuery = SqlConstants.SEARCH_QUEYFINTRANSBALANCEBANK_COUNTQUERY)
	Page<Object> queyfinTransBalanceInBank(String to_account,String from_account,String fristTime,
			String lastTime,BigDecimal startamount,BigDecimal endamount,int accountid,Pageable pageable);
	//查询总计
	@Query(nativeQuery = true, value =SqlConstants.totalfinTransBalanceBank)
	java.lang.Object[] totalfinTransBalanceInBank(String to_account,String from_account,String fristTime,
			String lastTime,BigDecimal startamount,BigDecimal endamount,int accountid);
	
	
	////出款账号数据源
	@Query(nativeQuery = true, value = "select * from (select "
			+"A.id,"
			+"A.from_account,"
			+"(select account from biz_account where id=A.from_account)from_accountname,"
			+"A.to_account,"
			+"IFNULL(A.amount,0)amount,"
			+"IFNULL(0.00,0)fee,"
			+"date_format(A.trading_time,'%Y-%m-%d %H:%i:%s') trading_time "
			+" from " 
			+" biz_bank_log A where A.status=1 and A.amount<0) A "
			+" where (?1 is null or A.from_accountname like concat('%',?1,'%')) and (?2 is null or A.to_account like concat('%',?2,'%'))"
			+" and (?3 is null or unix_timestamp(A.trading_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+" and (?5=0 or (A.amount>=?5 and A.amount<=?6)) and A.from_account=?7",countQuery = SqlConstants.SEARCH_QUEYFINTRANSBALANCEOUTBANK_COUNTQUERY)
	Page<Object> queyfinTransBalanceOutBank(String to_account,String from_account,String fristTime,
			String lastTime,BigDecimal startamount,BigDecimal endamount,int accountid,Pageable pageable);
	//查询总计
	@Query(nativeQuery = true, value =SqlConstants.totalfinTransBalanceOUTBank)
	java.lang.Object[] totalfinTransBalanceOutBank(String to_account,String from_account,String fristTime,
			String lastTime,BigDecimal startamount,BigDecimal endamount,int accountid);
	
	
	////备用金卡数据源
	@Query(nativeQuery = true, value = "select * from (select * from (select "
			+"A.id,"
			+"A.from_account,"
			+"A.to_account,"
			+"(select account from biz_account where id=A.from_account)from_accountname,"
			+"IFNULL(A.amount,0)amount,"
			+"IFNULL(0.00,0)fee,"
			+"date_format(A.trading_time,'%Y-%m-%d %H:%i:%s') trading_time "
			+" from " 
			+" biz_bank_log A where A.status=1 and A.amount>0) A "
			+" where (?1 is null or A.to_account like concat('%',?1,'%')) and (?2 is null or A.from_accountname like concat('%',?2,'%'))"
			+" and (?3 is null or unix_timestamp(A.trading_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+" and (?5=0 or (A.amount>=?5 and A.amount<=?6)) and A.from_account=?7"
			+" union "
			+"select * from (select "
			+"A.id,"
			+"A.from_account,"
			+"(select account from biz_account where id=A.from_account)from_accountname,"
			+"A.to_account,"
			+"IFNULL(A.amount,0)amount,"
			+"IFNULL(0.00,0)fee,"
			+"date_format(A.trading_time,'%Y-%m-%d %H:%i:%s') trading_time "
			+" from " 
			+" biz_bank_log A where A.status=1 and A.amount<0) A "
			+" where (?1 is null or A.from_accountname like concat('%',?1,'%')) and (?2 is null or A.to_account like concat('%',?2,'%'))"
			+" and (?3 is null or unix_timestamp(A.trading_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+" and (?5=0 or (A.amount>=?5 and A.amount<=?6)) and A.from_account=?7) as A",countQuery = SqlConstants.SEARCH_QUEYFINTRANSBALANCEBANKReservePettycash_COUNTQUERY)
	Page<Object> queyfinTransBalanceReservePettycashBank(String to_account,String from_account,String fristTime,
			String lastTime,BigDecimal startamount,BigDecimal endamount,int accountid,Pageable pageable);
	//查询总计
	@Query(nativeQuery = true, value =SqlConstants.totalfinTransBalanceReservePettycashBank)
	java.lang.Object[] totalfinTransBalanceReservePettycashBank(String to_account,String from_account,String fristTime,
			String lastTime,BigDecimal startamount,BigDecimal endamount,int accountid);
	
	
	////现金卡数据源
	@Query(nativeQuery = true, value = "select * from (select "
			+"A.id,"
			+"A.from_account,"
			+"A.to_account,"
			+"(select account from biz_account where id=A.from_account)from_accountname,"
			+"IFNULL(A.amount,0)amount,"
			+"IFNULL(0.00,0)fee,"
			+"date_format(A.trading_time,'%Y-%m-%d %H:%i:%s') trading_time "
			+" from " 
			+" biz_bank_log A where A.status=1 and A.amount>0) A "
			+" where (?1 is null or A.to_account like concat('%',?1,'%')) and (?2 is null or A.from_accountname like concat('%',?2,'%'))"
			+" and (?3 is null or unix_timestamp(A.trading_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+" and (?5=0 or (A.amount>=?5 and A.amount<=?6)) and A.from_account=?7",countQuery = SqlConstants.SEARCH_QUEYFINTRANSBALANCECashBANK_COUNTQUERY)
	Page<Object> queyfinTransBalanceCashBank(String to_account,String from_account,String fristTime,
			String lastTime,BigDecimal startamount,BigDecimal endamount,int accountid,Pageable pageable);
	//查询总计
	@Query(nativeQuery = true, value =SqlConstants.totalfinTransBalanceCashBank)
	java.lang.Object[] totalfinTransBalanceCashBank(String to_account,String from_account,String fristTime,
			String lastTime,BigDecimal startamount,BigDecimal endamount,int accountid);
	
	
	
	////公司入款余额数据源
	@Query(nativeQuery = true, value = "select * from (select "
			+"A.id,"
			+"A.from_account,"
			+"A.to_account,"
			+"(select account from biz_account where id=A.from_account)from_accountname,"
			+"IFNULL(A.amount,0)amount,"
			+"IFNULL(0.00,0)fee,"
			+"date_format(A.trading_time,'%Y-%m-%d %H:%i:%s') trading_time "
			+" from " 
			+" biz_bank_log A where A.status=1 and A.amount>0) A "
			+" where (?1 is null or A.to_account like concat('%',?1,'%')) and (?2 is null or A.from_accountname like concat('%',?2,'%'))"
			+" and (?3 is null or unix_timestamp(A.trading_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+" and (?5=0 or (A.amount>=?5 and A.amount<=?6)) and A.from_account=?7",countQuery = SqlConstants.SEARCH_QUEYFINTRANSBALANCECompanyBANK_COUNTQUERY)
	Page<Object> queyfinTransBalanceCompanyBank(String to_account,String from_account,String fristTime,
			String lastTime,BigDecimal startamount,BigDecimal endamount,int accountid,Pageable pageable);
	//查询总计
	@Query(nativeQuery = true, value =SqlConstants.totalfinTransBalanceCompanyBank)
	java.lang.Object[] totalfinTransBalanceCompanyBank(String to_account,String from_account,String fristTime,
			String lastTime,BigDecimal startamount,BigDecimal endamount,int accountid);
	
	
	
	////第三方入款余额数据源
	@Query(nativeQuery = true, value = "select * from (select "
			+"A.id,"
			+"A.from_account,"
			+"A.to_account,"
			+"(select account from biz_account where id=A.from_account)from_accountname,"
			+"IFNULL(A.amount,0)amount,"
			+"IFNULL(A.fee,0)fee,"
			+"date_format(A.trading_time,'%Y-%m-%d %H:%i:%s') trading_time "
			+" from " 
			+" biz_bank_log A where A.type =4 and A.status=1 and A.amount>0) A "
			+" where (?1 is null or A.to_account like concat('%',?1,'%')) and (?2 is null or A.from_accountname like concat('%',?2,'%'))"
			+" and (?3 is null or unix_timestamp(A.trading_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+" and (?5=0 or (A.amount>=?5 and A.amount<=?6)) and A.from_account=?7",countQuery = SqlConstants.SEARCH_QUEYFINTRANSBALANCEThirdBANK_COUNTQUERY)
	Page<Object> queyfinTransBalanceThirdBank(String to_account,String from_account,String fristTime,
			String lastTime,BigDecimal startamount,BigDecimal endamount,int accountid,Pageable pageable);
	//查询总计
	@Query(nativeQuery = true, value =SqlConstants.totalfinTransBalanceThirdBank)
	java.lang.Object[] totalfinTransBalanceThirdBank(String to_account,String from_account,String fristTime,
			String lastTime,BigDecimal startamount,BigDecimal endamount,int accountid);
	
	
	//未下发余额数据源
	@Query(nativeQuery = true, value = "select * from (select "
			+"A.id,"
			+"A.from_account,"
			+"A.to_account,"
			+"(select account from biz_account where id=A.from_account)from_accountname,"
			+"IFNULL(A.amount,0)amount,"
			+"IFNULL(A.fee,0)fee,"
			+"date_format(A.trading_time,'%Y-%m-%d %H:%i:%s') trading_time "
			+" from " 
			+" biz_bank_log A where A.type in (1,2,3,4) and A.status=1 and A.amount>0) A "
			+" where (?1 is null or A.to_account like concat('%',?1,'%')) and (?2 is null or A.from_accountname like concat('%',?2,'%'))"
			+" and (?3 is null or unix_timestamp(A.trading_time) between unix_timestamp(?3) and unix_timestamp(?4))"
			+" and (?5=0 or (A.amount>=?5 and A.amount<=?6)) and A.from_account=?7",countQuery = SqlConstants.SEARCH_QUEYFINTRANSBALANCENotissuedBANK_COUNTQUERY)
	Page<Object> queyfinTransBalanceNotissuedBank(String to_account,String from_account,String fristTime,
			String lastTime,BigDecimal startamount,BigDecimal endamount,int accountid,Pageable pageable);
	//查询总计
	@Query(nativeQuery = true, value =SqlConstants.totalfinTransBalanceNotissuedBank)
	java.lang.Object[] totalfinTransBalanceNotissuedBank(String to_account,String from_account,String fristTime,
			String lastTime,BigDecimal startamount,BigDecimal endamount,int accountid);
	
	//查询没有匹配的数据
	@Query(nativeQuery = true, value = "select D.account,C.type accounttype,D.counts,C.status from (select C.account,count(1) counts from (select " 
					 +" B.account"
					 +" from biz_income_request A,biz_account B where A.to_id=B.id and A.status in (0,2) "
					 +" and (?1 is null or unix_timestamp(A.create_time)<=unix_timestamp(?1))"
					 +" union all "
					 +" select "
					 +" C.account "
					 +" from biz_outward_request A,biz_outward_task B,biz_account C where B.account_id=C.id and A.id=B.outward_request_id and B.status in (0,1,2,6,8)"
					 +" and (?1 is null or unix_timestamp(A.create_time)<=unix_timestamp(?1))) C group by C.account)D,biz_account C "
					 +" where D.account=C.account ",countQuery = SqlConstants.ClearAccountDate)
	Page<Object> ClearAccountDate(String fristTime,Pageable pageable);
	
	//查询没有匹配的数据
	@Query(nativeQuery = true, value = "select D.account,C.type accounttype,D.counts from (select C.account,count(1) counts from (select " 
					 +"(select account from biz_account where A.to_id=id)account"
					 +" from biz_income_request A where A.status in (0,2) "
					 +" and (?1 is null or unix_timestamp(A.create_time)<=unix_timestamp(?1))"
					 +" union all "
					 +" select "
					 +" (select account from biz_account where B.account_id=id)account "
					 +" from biz_outward_request A,biz_outward_task B where A.id=B.outward_request_id and A.status in (0,1,3)"
					 +" and (?1 is null or unix_timestamp(A.create_time)<=unix_timestamp(?1))) C group by C.account)D,biz_account C "
					 +" where D.account=C.account ")
	List<Object> ClearAccountDate(String fristTime);
	
	//删除入款表的数据
	@Modifying 
	@Query(nativeQuery = true, value = "delete from biz_income_request where create_time<=?1 limit 10000")
	void DeleteAccounIncomtDate(String time);
	
	//删除出款请求表的数据
	@Modifying 
	@Query(nativeQuery = true, value = "delete from biz_outward_request where create_time<=?1 limit 10000")
	void DeleteAccountOutwardRequestDate(String time);
	
	//删除出款任务表的数据
	@Modifying 
	@Query(nativeQuery = true, value = "delete from biz_outward_task where asign_time<=?1 limit 10000")
	void DeleteAccountOutwardTaskDate(String time);
	
	//删除账号交易流水表数据进行删除
	@Modifying 
	@Query(nativeQuery = true, value = "delete from biz_transaction_log where create_time<=?1 limit 10000")
	void DeleteTransactionDate(String time);
	
	//删除银行流水表数据进行删除
	@Modifying 
	@Query(nativeQuery = true, value = "delete from biz_bank_log where create_time<=?1 limit 10000")
	void DeletebankLogDate(String time);
	
	//删除银行流水表数据进行删除
	@Modifying 
	@Query(nativeQuery = true, value = "delete from biz_third_request where create_time<=?1 limit 10000")
	void DeleteThirdRequestDate(String time);
	
	//检查是否还有需要删除的数据
	@Query(nativeQuery = true, value = "(select id from fundsTransfer.biz_income_request where create_time <=?1 limit 1)"
			+" union" 
			+" (select id from fundsTransfer.biz_outward_request where create_time <=?1 limit 1)"
			+" union" 
			+" (select id from fundsTransfer.biz_outward_task where asign_time <=?1 limit 1)"
			+" union" 
			+" (select id from fundsTransfer.biz_transaction_log where create_time <=?1 limit 1)"
			+" union" 
			+" (select id from fundsTransfer.biz_bank_log where create_time <=?1 limit 1)"
			+" union" 
			+" (select id from fundsTransfer.biz_third_request where create_time <=?1 limit 1)")
	List<Object> checkData(String time);

	
}