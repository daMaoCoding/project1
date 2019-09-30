package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.xinbo.fundstransfer.SqlConstants;
import com.xinbo.fundstransfer.domain.entity.AccountStatistics;

public interface AccountStatisticsRepository
		extends BaseRepository<AccountStatistics, Long> {

	// 查询统计系统出款记录
	@Query(nativeQuery = true, value = "select ta.account_id accountid,ac.account,ac.alias,ac.owner,ac.bank_type,SUM(ta.amount)taAmount,count(1)count_ from"
			+ " biz_account ac,biz_outward_request re,biz_outward_task ta "
			+ " where re.id=ta.outward_request_id and re.status in(5,6) and ta.status=5 and ac.id=ta.account_id"
			+ " and (?1 is null or ac.account like concat('%',?1,'%')) "
			+ " and (?2 is null or re.update_time between ?2 and ?3) "
			+ " and (?4 is null or ac.owner like concat('%',?4,'%'))"
			+ " and (?5 is null or ac.bank_type like concat('%',?5,'%'))" 
			+ " GROUP BY ta.account_id "
			, countQuery = SqlConstants.SEARCH_QUEYACCOUNTSTATISTICS_COUNTQUERY)
	Page<Object> queyAccountSystem(String Account, String sysTemFristTime, String sysTemEndTime, String accountOwner,
			String bankType, Pageable pageable);

	// 查询统计 银行卡出款记录
	@Query(nativeQuery = true, value = "select lo.from_account accountid,ac.owner,ac.bank_type,SUM(ABS(amount))bkAmount,SUM(0.0)bkFee,count(1)count_ from "
			+ " biz_account ac,biz_bank_log lo" + " where lo.from_account=ac.id "
			+ " and (?1 is null or ac.account like concat('%',?1,'%')) "
			+ " and (?2 is null or lo.trading_time between ?2 and ?3) and lo.amount<0"
			+ " and lo.status=1 and ac.type=5 "
			+ " and (?4 is null or ac.owner like concat('%',?4,'%'))"
			+ " and (?5 is null or ac.bank_type like concat('%',?5,'%'))" 
			+ " GROUP BY lo.from_account"
			, countQuery = SqlConstants.SEARCH_QUEYACCOUNTSTATISTICS_COUNTQUERYS)
	Page<Object> queyAccountBank(String Account, String bankStartTime, String bankEndTime, String accountOwner,
			String bankType, Pageable pageable);

	// 统计
	@Query(nativeQuery = true, value = SqlConstants.totalAccountSystem)
	java.lang.Object[] totalAccountSystem(String Account, String sysTemFristTime, String sysTemEndTime,
			String accountOwner, String bankType);

	// 统计
	@Query(nativeQuery = true, value = SqlConstants.totalAccountBank)
	java.lang.Object[] totalAccountBank(String Account, String bankStartTime, String bankEndTime, String accountOwner,
			String bankType);
	
	
    // 按账号 查询出款明细记录
	@Query(nativeQuery = true, value = "select A.account_id,A.account,A.alias,A.owner,A.bank_type,ifnull(A.taAmount,0),ifnull(A.count_,0),ifnull(B.bkAmount,0),ifnull(B.bkFee,0),ifnull(B.count_,0),A.handicap_id from "
				+"( select A.id account_id,A.account,A.alias,A.owner,A.bank_type,B.taAmount,B.count_,A.handicap_id from biz_account A left join "
				+" (select ta.account_id,SUM(ta.amount)taAmount,count(1)count_ from "
				+"biz_outward_request re,biz_outward_task ta "
				+" where re.id=ta.outward_request_id and re.status in(5,6) and ta.status=5 "
				+" and re.update_time BETWEEN ?2 and ?3"
				+" GROUP BY ta.account_id)B on A.id=B.account_id where A.type=5 and (A.status in (1,4,5) or (A.status=3 and date_add(A.update_time, interval 2 day)>now()))"
				+" and (?1 is null or A.account like concat('%',?1,'%'))"
				+" and (?4 is null or A.owner like concat('%',?4,'%'))"
				+" and (?5 is null or A.bank_type like concat('%',?5,'%'))) A LEFT JOIN "
				+"(select lo.from_account,SUM(ABS(amount))bkAmount,SUM(0.0)bkFee,count(1)count_ from "
				+"biz_bank_log lo "
				+"where lo.trading_time BETWEEN ?2 and ?3 and lo.amount<0 "
				+"and lo.status=1 GROUP BY from_account)B on A.account_id=B.from_account where (?6=0 or A.handicap_id=?6) order by taAmount desc"
				, countQuery = SqlConstants.SEARCH_QUEYACCOUNTSTATISTICS_COUNTQUERY)
	Page<Object> queyAccountStatistics(String Account, String sysTemFristTime, String sysTemEndTime, String accountOwner,
			String bankType,int handicap, Pageable pageable);
    // 统计
	@Query(nativeQuery = true, value = SqlConstants.totalAccountBank)
	java.lang.Object[] totalAccountStatistics(String Account, String bankStartTime, String bankEndTime, String accountOwner,
			String bankType,int handicap);
	
	 // 按账号 查询出款明细记录
		@Query(nativeQuery = true, value = "select A.account,A.bank_name,A.owner,A.handicap_id,A.alias,A.bank_type,"
					+"B.account_id, B.income, B.outward, B.fee, B.income_count, B.balance,"
					+"B.outward_count, B.outward_persons, B.income_persons,B.time," 
					+"B.fee_count, B.loss, B.loss_count, B.income_sys, B.income_sys_count, B.outward_sys, B.outward_sys_count"
					+" from biz_account A,biz_report B" 
					+" where B.time between ?2 and ?3 and (A.type in (5,14) or (A.type=1 and ROUND(B.outward_sys)=B.outward_sys)) and A.id=B.account_id "
					+" and (?1 is null or A.account like concat('%',?1,'%')) and (?4 is null or A.owner like concat('%',?4,'%'))"
					+" and (?5 is null or A.bank_type like concat('%',?5,'%'))"
					+" and B.account_handicap in (?6)"
					+" and (?7 is null or A.type=?7)"
					+" order by B.outward_sys desc"
					, countQuery = SqlConstants.SEARCH_QUEYACCOUNTSTATISTICS_COUNTQUERY_CLEARDATE)
		Page<Object> queyAccountStatisticsFromClearDate(String Account, String sysTemFristTime, String sysTemEndTime, String accountOwner,
				String bankType,List<Integer> handicapList,String cartype, Pageable pageable);
	    // 统计
		@Query(nativeQuery = true, value = SqlConstants.totalAccountBankFromClearDate)
		java.lang.Object[] totalAccountStatisticsFromClearDate(String Account, String bankStartTime, String bankEndTime, String accountOwner,
				String bankType,List<Integer> handicapList,String cartype);

	/**
	 * 出款明细>系统明细
	 * 
	 * @param accountid
	 * @param pageable
	 * @return
	 */
	@Query(nativeQuery = true, value = "select " 
			+ "(select name from biz_handicap where id=C.handicap) handicapname,"
			+ "(select name from biz_level where id=C.level and status=1) levelname," 
			+ "C.member,"
			+ "(select account from biz_account where id=B.account_id) accountname," 
			+ "C.to_account," 
			+ "B.amount,"
			+ "ifnull(0.0,0)fee," 
			+ "(select username from sys_user where id=B.operator) operatorname,"
			+ "date_format(C.update_time,'%Y-%m-%d %H:%i:%s') asigntime,"
			+ "B.order_no,B.to_account_owner,C.status restatus,B.status tastatus"
			+ " from " 
			+ "biz_outward_task B,biz_outward_request C"
			+ " where B.outward_request_id=C.id and (?8=9999 or B.status=?8) and (?7=9999 or C.status=?7) and B.account_id=?1 and (?2 is null or C.member like concat('%',?2,'%'))  "
			+ " and C.update_time between ?3 and ?4 and (?5=0 or (B.amount>=?5 and B.amount<=?6))"
			, countQuery = SqlConstants.SEARCH_QUEYFINDFINOUTSTATSYS_COUNTQUERY)
	Page<Object> queyfindFinOutStatSys(int accountid, String accountname, String ptfristTime, String ptlastTime, BigDecimal startamount,BigDecimal endamount,
			int restatus,int tastatus,Pageable pageable);

	@Query(nativeQuery = true, value = SqlConstants.totalqueyfindFinOutStatSys)
	java.lang.Object[] totalqueyfindFinOutStatSys(int accountid, String accountname, String ptfristTime,
			String ptlastTime,BigDecimal startamount,BigDecimal endamount,int restatus,int tastatus);

	/**
	 * 出款明细>银行明细
	 * 
	 * @param accountid
	 * @param accountname
	 * @param pageable
	 * @return
	 */
	@Query(nativeQuery = true, value = "select" 
	        + "(select account from biz_account where id=?1) account,"
			+ "B.to_account,abs(B.amount) amount,ifnull(0.0,0) fee,B.status,3 type,ifnull(B.remark,''),B.to_account_owner,"
			+ "date_format(B.trading_time,'%Y-%m-%d %H:%i:%s')," 
			+ "B.id,"
			+ "date_format(B.create_time,'%Y-%m-%d %H:%i:%s')create_time,B.balance,B.summary" 
			+ " from " 
			+ "biz_bank_log B"
			+ " where (?6=0 or (abs(B.amount)>=?6 and abs(B.amount)<=?7)) and (?8=9999 or B.status=?8) and (?9=0 or ((?9=-1 or  B.amount>0) and (?9=1 or  B.amount<0))) and B.from_account=?1 and (?2 is null or B.to_account_owner like concat('%',?2,'%')) and (?3 is null or B.to_account like concat('%',?3,'%')) "
			+ " and B.create_time between ?4 and ?5 "
			, countQuery = SqlConstants.SEARCH_QUEYFINDFINOUTSTATFLOW_COUNTQUERY)
	Page<Object> queyfindFinOutStatFlow(int accountid, String toaccountowner, String toaccount, String ptfristTime,
			String ptlastTime,BigDecimal startamount, BigDecimal endamount,int bkstatus,int typestatus,Pageable pageable);

	@Query(nativeQuery = true, value = SqlConstants.totalqueyfindFinOutStatFlow)
	// 查询总计进行返回处理显示
	java.lang.Object[] totalqueyfindFinOutStatFlow(int accountid, String toaccountowner, String toaccount,
			String ptfristTime, String ptlastTime,BigDecimal startamount, BigDecimal endamount,int bkstatus,int typestatus);

	/**
	 * 出款明细>银行明细>详情
	 * 
	 * @param transactionNo
	 * @return
	 */
	@Query(nativeQuery = true, value = "select " + "(select name from biz_handicap where id=D.handicap) handicapname,"
			+ "(select name from biz_level where id=D.level and status=1) levelname," + "D.member,"
			+ "D.to_account toaccount," + "D.to_account_name toaccountname," + "A.to_account_owner toaccountowner,"
			+ "A.to_account atoaccount," + "abs(A.amount) amount," + "ifnull(0.0,0)fee,"
			+ "date_format(A.trading_time,'%Y-%m-%d %H:%i:%s') tradingtime," + "B.type," + "D.amount damount,"
			+ "ifnull(B.fee,0) bfee,"
			+ "date_format((date_add(C.asign_time, interval C.time_consuming second)),'%Y-%m-%d %H:%i:%s') asigntime,"
			+ "(select username from sys_user where id=C.operator) operatorname,"
			+ "(select username from sys_user where id=B.confirmor) confirmor,"
			+ "(select account from biz_account where id=C.account_id)accountname,"
			+ "(select bank_name from biz_account where id=C.account_id)bankname,"
			+ "(select owner from biz_account where id=C.account_id)owner" + " from "
			+ "biz_bank_log A,biz_transaction_log B,biz_outward_task C,biz_outward_request D"
			+ " where A.id=B.from_banklog_id and B.order_id=C.id and C.outward_request_id=D.id "
			+ " and A.status=1 and A.amount<0 and B.type=0 and C.status=5 and D.status in (5,6) and A.id=?1"
			, countQuery = SqlConstants.SEARCH_QUEYFINDFINOUTSTATFLOWDETAILS_COUNTQUERY)
	Page<Object> queyfindFinOutStatFlowDetails(int id, Pageable pageable);

	/**
	 * 出款明细>按盘口统计
	 * 
	 * @param whereHandicap
	 * @param whereLevel
	 * @param fristTime
	 * @param lastTime
	 * @param fieldvalHandicap
	 * @param whereTransactionValue
	 * @param pageable
	 * @return
	 */
	@Query(nativeQuery = true, value = "select " 
	        + "(select name from biz_handicap where id=A.handicap)handicapname,"
			+ "A.handicap handicappno," 
			+ "sum(B.amount) amount," 
			+ "ifnull(0.0,0)fee," 
			+ "count(1) count" 
			+ " from "
			+ " biz_outward_request A,biz_outward_task B "
			+ " where A.id=B.outward_request_id and B.status=5 and A.status in (5,6) and (?1 is null or unix_timestamp(A.update_time) between unix_timestamp(?1) and unix_timestamp(?2))"
			+ " and (?3 is null or unix_timestamp(date_format(date_add(B.asign_time, interval B.time_consuming second),'%Y-%m-%d %H:%i:%s')) between unix_timestamp(?3) and unix_timestamp(?4))"
			+ "group by A.handicap"
			, countQuery = SqlConstants.SEARCH_QUEYACCOUNTSTATISTICSHANDICAP_COUNTQUERY)
	Page<Object> queyAccountStatisticsHandicap(String fristTime, String lastTime, String fieldvalHandicap,
			String whereTransactionValue, Pageable pageable);

	@Query(nativeQuery = true, value = SqlConstants.totalqueyAccountStatisticsHandicap)
	// 查询总计进行返回处理显示
	java.lang.Object[] totalqueyAccountStatisticsHandicap(String fristTime, String lastTime, String fieldvalHandicap,
			String whereTransactionValue);
	
	@Query(nativeQuery = true, value = "select B.account_handicap,sum(B.outward_sys),sum(fee),sum(outward_sys_count)"
			+" from biz_account A,biz_report B "
			+" where A.id=B.account_id and (A.type in (5,14) or (A.type=1 and ROUND(B.outward_sys)=B.outward_sys)) and B.time between ?2 and ?3 and  B.account_handicap in (?1)"
			+" group by B.account_handicap"
			, countQuery = SqlConstants.SEARCH_QUEYACCOUNTSTATISTICSHANDICAP_COUNTQUERY_FROMCLEARDATE)
	Page<Object> queyAccountStatisticsHandicapFromClearDate(List<Integer> handicapList,String fieldvalHandicap,
			String whereTransactionValue, Pageable pageable);

	@Query(nativeQuery = true, value = SqlConstants.totalqueyAccountStatisticsHandicapFromClearDate)
	// 查询总计进行返回处理显示
	java.lang.Object[] totalqueyAccountStatisticsHandicapFromClearDate(List<Integer> handicapList, String fieldvalHandicap,
			String whereTransactionValue);

	/**
	 * 出款明细>按盘口统计(不按盘口去统计，直接根据盘口、层级以及相关条件去查询)
	 * 
	 * @param whereHandicap
	 * @param whereLevel
	 * @param fristTime
	 * @param lastTime
	 * @param fieldvalHandicap
	 * @param whereTransactionValue
	 * @param pageable
	 * @return
	 */
	@Query(nativeQuery = true, value = "select " 
			+ "(select name from biz_handicap where id=A.handicap)handicapname,"
			+ "A.handicap handicappno," 
			+ "(select name from biz_level where id=A.level and status=1) levelname,"
			+ "A.level levelno," 
			+ "B.amount," 
			+ "ifnull(0.0,0)fee," 
			+ "A.id" 
			+ " from "
			+ " biz_outward_request A ,biz_outward_task B"
			+ " where A.id=B.outward_request_id and B.status=5 and A.status in (5,6) and (?1=0 or A.handicap=?1) and (?2=0 or A.level=?2) and (?3 is null or unix_timestamp(date_format(date_add(B.asign_time, interval B.time_consuming second),'%Y-%m-%d %H:%i:%s')) between unix_timestamp(?3) and unix_timestamp(?4))"
			+ " and (?5 is null or unix_timestamp(date_format(date_add(B.asign_time, interval B.time_consuming second),'%Y-%m-%d %H:%i:%s')) between unix_timestamp(?5) and unix_timestamp(?6))"
			, countQuery = SqlConstants.SEARCH_QUEYACCOUNTSTATISTICSBYHANDICAPANDLEVEL_COUNTQUERY)
	Page<Object> queyAccountStatisticsByHandicapAndLevel(int whereHandicap, int whereLevel, String fristTime,
			String lastTime, String fieldvalHandicap, String whereTransactionValue, Pageable pageable);

	@Query(nativeQuery = true, value = SqlConstants.queyAccountStatisticsByHandicapAndLevel)
	// 查询总计进行返回处理显示
	java.lang.Object[] totalqueyAccountStatisticsByHandicapAndLevel(int whereHandicap, int whereLevel, String fristTime,
			String lastTime, String fieldvalHandicap, String whereTransactionValue);

	/**
	 * 出款明细>按盘口统计>明细(根据盘口去查询对应的id 然后根据id去查询相关的信息)
	 * 
	 * @param handicap
	 * @param level
	 * @param member
	 * @param fristTime
	 * @param lastTime
	 * @param startamount
	 * @param endamount
	 * @param rqhandicap
	 * @param pageRequest
	 * @return
	 */
	@Query(nativeQuery = true, value = "select A.*,B.from_banklog_id,date_format(B.create_time,'%Y-%m-%d %H:%i:%s') bankcreate_time from (select " 
			 +"A.handicap,"
			 +"A.member," 
			 +"A.order_no,"
			 +"date_format(A.create_time,'%Y-%m-%d %H:%i:%s') create_time,"
             +"date_format(A.update_time,'%Y-%m-%d %H:%i:%s') update_time," 
			 +"A.amount reamount," 
			 +"B.amount taamount,"
			 +"B.operator," 
			 +"A.to_account,"
             +"A.to_account_owner,"
             +"A.status restatus,"
             +"B.status tastatus,"
             +"B.id,B.account_id,B.remark"
			 +" from " 
			 +" biz_outward_request A,biz_outward_task B"
			 +" where (?1=0 or A.handicap=?1) and (?2=0 or A.level=?2) and (?6=0 or (A.amount>=?6 and A.amount<=?7)) and (?3 is null or A.member like concat('%',?3,'%'))"
			 +" and (?8=0 or A.handicap=?8) and A.handicap in (?12) and (?9=0 or  A.id=?9) and (?10=9999 or A.status=?10) and (?11=9999 or B.status=?11) and "
			 +" (Case When A.update_time is null then A.create_time else A.update_time end) between ?4 and ?5 and A.id=B.outward_request_id)A"
             +" left join "
             +" (select from_banklog_id,order_id,create_time from biz_transaction_log where create_time between ?4 and date_add(?5,interval 1 day) and type=0 ) B on A.id= B.order_id"
			, countQuery = SqlConstants.SEARCH_QUEYACCOUNTMATCHBYHANDICAP_COUNTQUERY)
	Page<Object> queyAccountMatchByhandicap(int handicap, int level, String member, String fristTime, String lastTime,
			BigDecimal startamount, BigDecimal endamount, int rqhandicap, int id,int restatus,int tastatus,List<Integer>handicaps, Pageable pageable);

	@Query(nativeQuery = true, value = SqlConstants.totalqueyAccountMatchByhandicap)
	// 查询去重的总计进行返回 因为存在拆单的现象 可能父表存在多条同样的记录
	java.lang.Object[] totalqueyAccountMatchByhandicap(int handicap, int level, String member, String fristTime,
			String lastTime, BigDecimal startamount, BigDecimal endamount, int rqhandicap, int id,int restatus,int tastatus);

	@Query(nativeQuery = true, value = SqlConstants.totalqueyAccountMatchByhandicap1)
	// 查询子表总计进行返回
	java.lang.Object[] totalqueyAccountMatchByhandicap1(int handicap, int level, String member, String fristTime,
			String lastTime, BigDecimal startamount, BigDecimal endamount, int rqhandicap, int id,int restatus,int tastatus);

	/**
	 * 出款明细>按盘口统计>明细(根据id去查询相关的信息) 暂时没用这个方法了
	 * 
	 * @param id
	 * @param pageRequest
	 * @return
	 */
	@Query(nativeQuery = true, value = "select \n"
			+ "(select name from biz_handicap where id=A.handicap)handicapname, \n" + "\t A.handicap handicappno, \n"
			+ "\t A.member, \n" + "\t A.order_no, \n"
			+ "\t date_format(A.create_time,'%Y-%m-%d %H:%i:%s') create_time, \n" + "\t A.amount amounts, \n"
			+ "\t B.amount, \n" + "\t C.fee, \n" + "\t (select username from sys_user where id=B.operator) operator, \n"
			+ "\t A.to_account, \n" + "\t A.to_account_owner, \n"
			+ "\t (select username from sys_user where id=C.operator) operatorczr, \n"
			+ "\t (select username from sys_user where id=C.confirmor) confirmor \n" + "\t from \n"
			+ "\t biz_outward_request A,biz_outward_task B,biz_transaction_log C \n"
			+ "\t where A.id=B.outward_request_id and B.id=C.order_id and A.id=?1"
			, countQuery = "select count(1) from (select \n"
					+ "(select name from biz_handicap where id=A.handicap)handicapname, \n"
					+ "\t A.handicap handicappno, \n" + "\t A.member, \n" + "\t A.order_no, \n"
					+ "\t date_format(A.create_time,'%Y-%m-%d %H:%i:%s') create_time, \n" + "\t A.amount amounts, \n"
					+ "\t B.amount, \n" + "\t C.fee, \n"
					+ "\t (select username from sys_user where id=B.operator) operator, \n" + "\t A.to_account, \n"
					+ "\t A.to_account_owner, \n"
					+ "\t (select username from sys_user where id=C.operator) operatorczr, \n"
					+ "\t (select username from sys_user where id=C.confirmor) confirmor \n" + "\t from \n"
					+ "\t biz_outward_request A,biz_outward_task B,biz_transaction_log C \n"
					+ "\t where A.id=B.outward_request_id and B.id=C.order_id and A.id=?1) as total")
	Page<Object> queyAccountMatchByid(int id, Pageable pageable);

	/**
	 * 根据账号查询 入款 各状态的数据数量（状态，0：等待匹配，1：已匹配，2：无法匹配， 3：已取消）
	 * 
	 * @param whereBankValue
	 * @param whereTransactionValue
	 * @param whereAccount
	 * @param fristTime
	 * @param lastTime
	 * @return
	 */
	@Query(nativeQuery = true, value = SqlConstants.queryCountByToAccount)
	java.lang.Object[] queryCountByToAccount(Integer accountId);

	/**
	 * 根据账号查询 出款 各状态的数据数量（状态，0：等待匹配，1：已匹配，2：无法匹配， 3：已取消）
	 * 
	 * @param account
	 * @return
	 */
	@Query(nativeQuery = true, value = SqlConstants.queryCountByFromAccount)
	java.lang.Object[] queryCountByFromAccount(Integer accountId);

	/**
	 * 根据转账的出款账号和入款账号查询 各状态的交易数据数量（状态，0：等待匹配，1：已匹配，2：无法匹配， 3：已取消）
	 * 
	 * @param whereBankValue
	 * @param whereTransactionValue
	 * @param whereAccount
	 * @param fristTime
	 * @param lastTime
	 * @return
	 */
	@Query(nativeQuery = true, value = SqlConstants.queryCountByFromAccountAndToAccount)
	java.lang.Object[] queryCountByFromAccountAndToAccount(Integer fromId, Integer toId);

	@Query(nativeQuery = true, value = SqlConstants.queryCountByAccount4Outward)
	java.lang.Object[] queryCountByAccount4Outward(Integer accountId);
}