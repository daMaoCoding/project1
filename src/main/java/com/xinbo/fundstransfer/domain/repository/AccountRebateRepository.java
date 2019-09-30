package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import com.xinbo.fundstransfer.domain.entity.BizAccountRebate;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.xinbo.fundstransfer.SqlConstants;

public interface AccountRebateRepository
		extends BaseRepository<BizAccountRebate, Long> {
	BizAccountRebate findByTid(String tid);

	BizAccountRebate findById2(Long id);

	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_account_rebate set remark=?2,status=?3 where id=?1")
	void saveRemark(Long id,String remark,int status);

	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_account_rebate set remark=?2,status=?3,update_time=now() where id=?1 and status=?4")
	void updRemarkOrStatus(Long id,String remark,int afterStatus,int beforeStatus);

	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_account_rebate set remark=?2,status=?3,screenshot=?5,update_time=now() where id=?1 and status=?4")
	void updRemarkOrStatus(Long id,String remark,int afterStatus,int beforeStatus,String screenshot);

	@Query(nativeQuery = true, value = "select * from ( SELECT A.id ,"
		       +"A.uid ,"
		       +"A.tid ,"
		       +"A.account_id ,"
		       +"A.to_account ,"
		       +"A.to_holder ,"
		       +"A.to_account_type ,"
		       +"A.to_account_info ,"
		       +"A.amount ,"
		       +"A.balance ,"
		       +"A.status ,"
		       +"date_format(A.create_time,'%Y-%m-%d %H:%i:%s')create_time,"
		       +"date_format(A.update_time,'%Y-%m-%d %H:%i:%s') update_time,"
		       +"A.remark,A.handicap, "
		       +"date_format(A.asign_time,'%Y-%m-%d %H:%i:%s') asign_time,A.operator,A.screenshot, "
		       +" TIMESTAMPDIFF(SECOND,A.asign_time,now()) differenceMinutes,A.type,B.user_name "
		  +"FROM  biz_account_rebate A,biz_rebate_user B where A.uid=B.uid and A.status in (?1) and ((?2='rebate' or ?2='check' or ?2='canceled') or A.account_id is null) and (?2!='rebate' or A.account_id is not null)"
		  +" and (?3 is null or A.tid like concat('%',?3,'%')) and (?10 is null or B.user_name like concat('%',?10,'%')) and (?4 is null or A.amount>=?4) and (?5 is null or A.amount<=?5)"
		  +" and (?6 is null or A.update_time between ?6 and ?7) and (?8=0 or A.handicap=?8) and A.handicap in (?9)"
		  +" and (?11 is null or (?11='2' or (A.type is null or A.type=1)) and (?11='1' or A.type=2)))A",countQuery = SqlConstants.SEARCH_FINDREBATE_COUNTQUERY)
	Page<Object> findRebate(List<Integer> status,String type,String orderNo,String fromAmount,String toAmount,String fristTime, String lastTime,int handicap,List<Integer> handicapList,String uName,String rebateType, Pageable pageable);
	//查询总计
	@Query(nativeQuery = true, value =SqlConstants.totalFindRebate)
	java.lang.Object[] totalFindRebate(List<Integer> status,String type,String orderNo,String fromAmount,String toAmount,String fristTime, String lastTime,int handicap,List<Integer> handicapList,String uName,String rebateType);

	
	@Query(nativeQuery = true, value = "select A.calc_time,sum(A.total_amount)total_amount,(sum(A.amount)+sum(ifnull(A.activity_amount,0))+sum(ifnull(A.agent_amount,0)))amount,count(distinct B.mobile)counts,A.status,A.remark"
			+" from fundsTransfer.biz_account_return_summary A,fundsTransfer.biz_account B where A.account=B.id and A.amount>0 and (?1 is null or calc_time between ?1 and ?2) and (?3 is null or A.status=?3) group by calc_time,A.status,A.remark order by calc_time desc",countQuery = SqlConstants.SEARCH_FINDREBATE_COMMISSION)
	Page<Object> findAuditCommission(String fristTime, String lastTime,String results, Pageable pageable);
	//查询总计
	@Query(nativeQuery = true, value =SqlConstants.totalFindAuditCommission)
	java.lang.Object[] totalFindAuditCommission(String fristTime, String lastTime,String results);
	
	@Query(nativeQuery = true, value = "select A.*,D.user_name from ( select account,sum(total_amount),(sum(amount)+sum(ifnull(activity_amount,0))+sum(ifnull(agent_amount,0)))amounts "
		+" from fundsTransfer.biz_account_return_summary where calc_time=?3 group by account ) A"
		+" left join"
		+" ( select A.id,C.user_name,A.bank_type from biz_account A,biz_account_more B,biz_rebate_user C"
		+" where A.mobile=B.moible and B.uid=C.uid) D on A.account=D.id where (?1 is null or D.user_name like CONCAT('%',?1,'%')) and (?2 is null or D.bank_type=?2) and (?4=0 or A.amounts>=?4) and (?5=0 or A.amounts<=?5) and A.amounts>0 ",countQuery = SqlConstants.SEARCH_FINDREBATE_DRTAIL)
	Page<Object> findDetail(String rebateUser, String bankType,String caclTime,BigDecimal startAmount,BigDecimal endAmount, Pageable pageable);
	//查询总计
	@Query(nativeQuery = true, value =SqlConstants.totalFindAuditDETAIL)
	java.lang.Object[] totalFindDetail(String rebateUser, String bankType,String caclTime,BigDecimal startAmount,BigDecimal endAmount);
	
	@Query(nativeQuery = true, value = "select A.calc_time,ifnull(B.counts,0),ifnull(B.amounts,0),A.rebateAmounts,A.remark from " 
		+" (select date_format(A.calc_time, '%Y-%m-%d')calc_time,sum(ifnull(A.balance,0))rebateAmounts,A.remark"
		+" from biz_account_return_summary A,fundsTransfer.biz_account B where A.account=B.id and A.status=1 group by date_format(calc_time, '%Y-%m-%d'),remark)A"
		+" left join (select date_format(create_time, '%Y-%m-%d' )create_time,count(distinct uid)counts,sum(amount)amounts from "
		+" fundsTransfer.biz_account_rebate where (status=1 or status=5) group by date_format(create_time, '%Y-%m-%d')) B on A.calc_time=B.create_time where (?1 is null or A.calc_time between ?1 and ?2) order by A.calc_time desc",countQuery = SqlConstants.SEARCH_FINDREBATE_COMPLETE)
	Page<Object> findComplete(String fristTime, String lastTime, Pageable pageable);
	//查询总计
	@Query(nativeQuery = true, value =SqlConstants.totalFindAuditComplete)
	java.lang.Object[] totalFindComplete(String fristTime, String lastTime);
	
	
	@Query(nativeQuery = true, value = "select V.user_name,V.amount,ifnull(N.amounts,0),V.amount am from(select T.uid,T.user_name,sum(T.amount)amount from (select A.*,D.user_name,D.uid from (select account,sum(ifnull(balance,0)) amount"
		+" from biz_account_return_summary where calc_time=?2 group by account ) A"
		+" left join"
		+" (select A.id,C.user_name,C.uid,A.bank_type from fundsTransfer.biz_account A,fundsTransfer.biz_account_more B,fundsTransfer.biz_rebate_user C"
		+" where A.mobile=B.moible and B.uid=C.uid ) D on A.account=D.id order by D.user_name,D.uid desc) T group by T.user_name,T.uid)V"
		+" left join"
		+" (select sum(amount)amounts,uid from "
		+" fundsTransfer.biz_account_rebate where (status=1 or status=5) and date_format(create_time, '%Y-%m-%d')=?2 group by uid) N on V.uid=N.uid where (?1 is null or V.user_name like CONCAT('%',?1,'%'))",countQuery = SqlConstants.SEARCH_FINDREBATE_COMPlETEDRTAIL)
	Page<Object> findCompleteDetail(String rebateUser,String caclTime, Pageable pageable);
	//查询总计
	@Query(nativeQuery = true, value =SqlConstants.totalFindCompleteDetail)
	java.lang.Object[] totalFindCompleteDetail(String rebateUser,String caclTime);
	
	
	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_account_rebate set account_id=?2,operator=?3,remark=?4,asign_time=now() where id=?1 and account_id is null and tid!=?5 and status=?6")
	void allocAccount(Long id, Integer fromId,Integer operator, String remark, Long tid, Integer status);

	@Query(nativeQuery = true, value = "select * from "
			+ " biz_account_rebate "
			+ " where (?5=2 or status=1) and (?5=1 or status=0) and (?6=2 or (type=1 or type is null)) and (?6=1 or type=2) and account_id=?1 and amount=?2 "
			+ " and (?4=2 or to_account=?3 or to_account LIKE CONCAT('%',?3)) and (?4=1 or to_holder=?3 ) order by create_time desc limit 1")
	BizAccountRebate findRebateByBankLog(int fromAccountId, Float amount, String toAccountOrtoAccountOwner, int type, int cardType,int orderType);

	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_account_rebate set status=?2,update_time=now() where id=?1")
	void updateStatusById(Long id, Integer status);

	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_account_rebate set status=?3,remark=?4 where id=?1 and status=?2")
	int updateStatusAndRemark(long id, int oriStatus, int desStatus, String remark);

	@Query(nativeQuery = true, value = "select * from biz_account_rebate t where t.status=?2 and t.account_id=?1 limit 1")
	BizAccountRebate applyTask4Robot(int accountId, int status);

	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_account_rebate set remark=?2,status=0,account_id=null,operator=null where id=?1 and status in (0,6)")
	void reAssignDrawing(Long id,String remark);


	@Query(nativeQuery = true, value = "select * from biz_account_rebate where tid=?1 order by id desc limit 1")
	BizAccountRebate findLatestByTid(String tid);

	@Query(nativeQuery = true, value = "select * from biz_account_rebate t join biz_account t1 on t.account_id = t1.id where t.status = 0 and t1.flag = 2 and t.asign_time < ?1")
	List<BizAccountRebate> getExpireRebateTask(String expireTime);

	@Query(nativeQuery = true, value = "select * from biz_account_rebate t where t.status=?1 and t.account_id is not null and t.update_time <?2 limit 1000")
	List<BizAccountRebate> getExpireUnknown(int status,String expireTime);
	
	@Query(nativeQuery = true, value = "select * from biz_account_rebate bar where bar.status = 0 and bar.account_id is null and bar.operator is null and bar.create_time >= ?1")
	List<BizAccountRebate> getAllUndepositTask(String dayTime);
	
	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_account_return_summary set status=?2,remark=?3 where calc_time=?1")
	void saveAudit(String caclTime, int status, String remark);
	
	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_account_return_summary set status=0,remark=?2 where calc_time=?1")
	void updateAudit(String caclTime, String remark);
	
	@Query(nativeQuery = true, value = "select remark from biz_account_return_summary where calc_time=?1 limit 1")
	String findRemark(String caclTime);
	
	@Query(nativeQuery = true, value = "select status from biz_account_return_summary where calc_time=?1 limit 1")
	String findStatus(String caclTime);
	
	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_account_return_summary set total_amount=?3,amount=?4 where calc_time=?1 and account=?2")
	void updateAmount(String caclTime, int accId,BigDecimal totalAmount,BigDecimal amount);
	
	@Query(nativeQuery = true, value = "select bar.id from biz_account_rebate bar where bar.status = 0 and bar.account_id is null and bar.operator is null")
	List<Long> findRebateTaskStatusByIds();
	
	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "insert into biz_account_rebate_blacklist (account,calc_time,remark) values(?1,?2,?3)")
	void addBlackList(String account,String caclTime,String remark);
	
	@Query(nativeQuery = true, value = "select account from biz_account_rebate_blacklist where calc_time=?1")
	List<Integer> findBlackList(String caclTime);
	
	@Query(nativeQuery = true, value = "select * from ( SELECT A.id ,"
		       +"A.uid ,"
		       +"A.tid ,"
		       +"A.account_id ,"
		       +"A.to_account ,"
		       +"A.to_holder ,"
		       +"A.to_account_type ,"
		       +"A.to_account_info ,"
		       +"A.amount ,"
		       +"A.balance ,"
		       +"A.status ,"
		       +"date_format(A.create_time,'%Y-%m-%d %H:%i:%s')create_time,"
		       +"date_format(A.update_time,'%Y-%m-%d %H:%i:%s') update_time,"
		       +"A.remark,A.handicap, "
		       +"date_format(A.asign_time,'%Y-%m-%d %H:%i:%s') asign_time,A.operator,A.screenshot, "
		       +" TIMESTAMPDIFF(SECOND,A.asign_time,now()) differenceMinutes,A.type,B.user_name "
		  +"FROM  biz_account_rebate A,biz_rebate_user B where A.uid=B.uid and A.status in (?1) "
		  +" and (?2 is null or A.tid like concat('%',?2,'%')) and A.type=2 and (?7 is null or B.user_name like concat('%',?7,'%')) and (?3 is null or A.amount>=?3) and (?4 is null or A.amount<=?4)"
		  +" and (?5=0 or A.handicap=?5) and A.handicap in (?6))A",countQuery = SqlConstants.SEARCH_FINDDERATING_COUNTQUERY)
	Page<Object> findDerating(List<Integer> status,String orderNo,String fromAmount,String toAmount,int handicap,List<Integer> handicapList,String uname, Pageable pageable);
	//查询总计
	@Query(nativeQuery = true, value =SqlConstants.totalFindDerating)
	java.lang.Object[] totalFindDerating(List<Integer> status,String orderNo,String fromAmount,String toAmount,int handicap,List<Integer> handicapList,String uname);
	
	@Query(nativeQuery = true, value = "select sum(ifnull(amount,0)) from biz_account_rebate where status=888 and uid=?1")
	BigDecimal deratingAmounts(String uid);
	
}
