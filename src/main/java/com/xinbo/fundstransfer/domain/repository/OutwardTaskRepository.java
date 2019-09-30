package com.xinbo.fundstransfer.domain.repository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.xinbo.fundstransfer.domain.entity.BizAccountRebate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.xinbo.fundstransfer.domain.entity.BizOutwardTask;

public interface OutwardTaskRepository extends JpaRepository<BizOutwardTask, Long>,
		JpaSpecificationExecutor<BizOutwardTask>, PagingAndSortingRepository<BizOutwardTask, Long> {
	@Query(nativeQuery = true, value = "select * from biz_outward_task where id=(?1) and status in(0,2,6) limit 1")
	BizOutwardTask findByIdAndStatus(Long id);

	@Query(nativeQuery = true, value = "select id,status ,remark,operator from biz_outward_task where 1=1 and id in(?1)   ")
	List<Object[]> findIdAndStatusAndRemark(List<Long> taskId);

	@Query(nativeQuery = true, value = "SELECT a.countUnfinish,c.countFinished ,d.countALL from (select count(1) countUnfinish from biz_outward_task b where b.outward_request_id=?1 and b.operator =?2 and b.status=0) a ,(select count(1) countFinished from biz_outward_task b where b.outward_request_id=?1 and b.operator =?2 and b.status <>0) c,\n"
			+ "(select count(1) countALL from biz_outward_task b where b.outward_request_id=?1 and b.operator =?2 ) d")
	Object getUnfinishedSplitedTasks(Long outwardRequestId, int operator);

	@Query(nativeQuery = true, value = "SELECT a.countUnfinish,c.countFinished ,d.countALL from (select count(1) countUnfinish from biz_outward_task b where b.outward_request_id=?1 and b.operator =?2 and b.status=2) a ,(select count(1) countFinished from biz_outward_task b where b.outward_request_id=?1 and b.operator =?2 and b.status <>2) c,\n"
			+ "(select count(1) countALL from biz_outward_task b where b.outward_request_id=?1 and b.operator =?2 ) d")
	Object getUnfinishedSplitedTasksByStatus(Long outwardRequestId, int operator);

	@Query(nativeQuery = true, value = "select * from biz_outward_task where status = 0 limit 5;")
	List<BizOutwardTask> test();

	List<BizOutwardTask> findByOutwardRequestId(Long outwardRequestId, Sort sort);

	@Query(nativeQuery = true, value = "select b.operator ,count(b.operator)  from biz_outward_task b INNER JOIN sys_user u WHERE"
			+ " b.operator = u.id AND u.category=1 GROUP BY b.operator  HAVING count(b.operator)>0  ")
	List<?> findListJoinUser();

	@Query(value = "SELECT \n" + "\t   task.id  outwardTaskId,\n" + "\t   task.outward_request_id outwardRequestId,\n"
			+ "\t   task.account_id fromAccountId ,\n" + "\t   task.amount amount,\n"
			+ "\t   task.asign_time asignTime,\n" + "\t   REQ.handicap handicapId, \n" + "\t   REQ.level levelId, \n"
			+ "\t   REQ.member  memberUserName ,\n" + "\t   REQ.to_account_bank toAccountBankName,\n"
			+ "\t   REQ.to_account toAccount,\n" + "\t   REQ.to_account_owner  toAccountOwner,\n "
			+ "\t   task.operator taskOperator,\n" + "\t task.remark taskRemark,\n" + "\t task.status taskStatus,\n"
			+ "\t task.screenshot screenshot,\n" + "\t REQ.order_no orderNo \n" + "\tFROM biz_outward_task task \n"
			+ "\tLEFT JOIN biz_outward_request REQ ON task.outward_request_id=REQ.id\n" + "\tWHERE 1=1 AND \n"
			+ "\t(:outwardTaskId IS NULL OR task.id=:outwardTaskId) AND\n"
			+ "\t(:fromAccountId IS NULL OR task.account_id=:fromAccountId) AND\n"
			+ "\t(:handicapId IS NULL OR REQ.handicap=:handicapId) AND\n"
			+ "\t(:levelId IS NULL OR REQ.level=:levelId ) AND \n" + "\t(:status IS NULL OR task.status=:status)AND\n"
			+ "\t(:toAccount IS NULL OR REQ.to_account=:toAccount)AND\n"
			+ "\t(:minAmount IS NULL OR task.amount >=:minAmount )AND\n"
			+ "\t(:maxAmount IS NULL OR task.amount <=:maxAmount)AND\n"
			+ "\t(:startTime IS NULL OR task.asign_time >=:startTime )AND\n"
			+ "\t(:endTime IS NULL OR task.asign_time <=:endTime) ", countQuery = "SELECT \n"
			+ "   count(1)\n" + "FROM biz_outward_task task \n"
			+ "LEFT JOIN biz_outward_request REQ ON task.outward_request_id=REQ.id\n" + "WHERE 1=1 AND \n"
			+ "(:outwardTaskId IS NULL OR task.id=:outwardTaskId) AND\n"
			+ "(:fromAccountId IS NULL OR task.account_id=:fromAccountId) AND\n"
			+ "(:handicapId IS NULL OR REQ.handicap=:handicapId) AND\n"
			+ "(:levelId IS NULL OR REQ.level=:levelId ) AND \n"
			+ "(:status IS NULL OR task.status=:status)AND\n"
			+ "(:toAccount IS NULL OR REQ.to_account=:toAccount)AND\n"
			+ "\t(:minAmount IS NULL OR task.amount >=:minAmount )AND\n"
			+ "\t(:maxAmount IS NULL OR task.amount <=:maxAmount)AND\n"
			+ "\t(:startTime IS NULL OR task.asign_time >=:startTime )AND\n"
			+ "\t(:endTime IS NULL OR task.asign_time <=:endTime)", nativeQuery = true)
	Page<Object> findAllForCheck(@Param("outwardTaskId") Long outwardTaskId,
								 @Param("fromAccountId") Integer fromAccountId, @Param("handicapId") Integer handicapId,
								 @Param("levelId") Integer levelId, @Param("status") Integer status, @Param("startTime") Date startTime,
								 @Param("endTime") Date endTime, @Param("minAmount") BigDecimal minAmount,
								 @Param("maxAmount") BigDecimal maxAmount, @Param("toAccount") String toAccount, Pageable pageable);

	@Query(value = "SELECT \n" + "\t   sum(task.amount) totalAmount,\n" + "\t   sum(0) totalFee \n"
			+ "\tFROM biz_outward_task task \n"
			+ "\tLEFT JOIN biz_outward_request REQ ON task.outward_request_id=REQ.id\n" + "\tWHERE 1=1 AND \n"
			+ "\t(:outwardTaskId IS NULL OR task.id=:outwardTaskId) AND\n"
			+ "\t(:fromAccountId IS NULL OR task.account_id=:fromAccountId) AND\n"
			+ "\t(:handicapId IS NULL OR REQ.handicap=:handicapId) AND\n"
			+ "\t(:levelId IS NULL OR REQ.level=:levelId ) AND \n" + "\t(:status IS NULL OR task.status=:status)AND\n"
			+ "\t(:toAccount IS NULL OR REQ.to_account=:toAccount)AND\n"
			+ "\t(:minAmount IS NULL OR task.amount >=:minAmount )AND\n"
			+ "\t(:maxAmount IS NULL OR task.amount <=:maxAmount)AND\n"
			+ "\t(:startTime IS NULL OR task.asign_time >=:startTime )AND\n"
			+ "\t(:endTime IS NULL OR task.asign_time <=:endTime)", nativeQuery = true)
	java.lang.Object[] queryAmountAndFeeForCheckByTotal(@Param("outwardTaskId") Long outwardTaskId,
														@Param("fromAccountId") Integer fromAccountId, @Param("handicapId") Integer handicapId,
														@Param("levelId") Integer levelId, @Param("status") Integer status, @Param("startTime") Date startTime,
														@Param("endTime") Date endTime, @Param("minAmount") BigDecimal minAmount,
														@Param("maxAmount") BigDecimal maxAmount, @Param("toAccount") String toAccount);

	BizOutwardTask findById2(Long id);

	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_outward_task set status=?2,account_id=null,operator=null,asign_time=null,remark=?4 where id=?1 and status=?3")
	void undepositFromMaintain(long taskId, int statusUndeposit, int statusMaintain, String remark);

	@Query(nativeQuery = true, value = "select t.* from biz_outward_task t,biz_outward_request r where t.outward_request_id =r.id and t.status=?1 and (r.to_account_bank IS NULL OR r.to_account_bank = '' OR ?2 IS NULL OR ?2 = '' OR NOT locate(r.to_account_bank,?2))  limit 15")
	List<BizOutwardTask> findNotMaintain(int statusMaintain, String maitainBanks);

	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_outward_task set status=?2,remark=?4 where id=?1 and status=?3 and account_id is null")
	void maintain(long taskId, int statusMaintain, int statusUndeposit, String remark);

	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_outward_task set account_id=?2,operator=?3,remark=?6,asign_time=now(),outward_pay_type=?8 where id=?1 and account_id is null and operator is null and status!=?4 and status!=?5 and status=?7")
	int allocAccount(long taskId, Integer accountId, Integer operator, int statusManageCancel, int statusManageRefuse,
					 String remark, int statusUndeposit,int outwardPayType);

//	@Modifying
//	@Transactional
//	@Query(nativeQuery = true, value = "update biz_outward_task set status=?3,remark=?4,time_consuming=?6 where id=?2 or (outward_request_id=?1 and id!=?2 and account_id is null) or (outward_request_id=?1 and id!=?2 and status=?5)")
//	void cancelOrRefuse(long reqId, long taskId, int status, String remark, int statusManagerDeal, int timeConsume);

	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_outward_task set status=?2,remark=?3,time_consuming=?4 where id=?1")
	void cancelOrRefuseById(long taskId, int status, String remark, int timeConsume);
	
	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_outward_task set status=?3,remark=?4,time_consuming=?5 where outward_request_id=?1 and id!=?2 and account_id is null")
	void cancelOrRefuseByRequestIdAndNotAccountId(long reqId, long taskId, int status, String remark, int timeConsume);	
	
	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_outward_task set status=?3,remark=?4,time_consuming=?6 where outward_request_id=?1 and id!=?2 and status=?5")
	void cancelOrRefuseByRequestIdAndStatus(long reqId, long taskId, int status, String remark, int statusManagerDeal, int timeConsume);	
	
	@Query(nativeQuery = true, value = "select * from biz_outward_task t where t.outward_request_id=?1 and t.account_id is not null")
	List<BizOutwardTask> findAllocated(long reqId);

	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_outward_task set status=?3,remark=?2,operator=null,account_id=null, asign_time=now() where id=?1 and status=?4")
	void alterStatusToMgr(long taskId, String remark, int statusManager, int statusUndeposit);

	@Query(nativeQuery = true, value = "select * from biz_outward_task t where t.status=?2 and t.operator=?1  order by asign_time asc limit 1")
	BizOutwardTask applyTask4User(int userId, int status);

	@Query(nativeQuery = true, value = "select * from biz_outward_task t where t.status=?2 and t.account_id=?1 and t.operator is null limit 1")
	BizOutwardTask applyTask4Robot(int accountId, int status);

	@Query(nativeQuery = true, value = "select * from biz_outward_task t where t.status=?2 and t.account_id=?1 limit 1")
	BizOutwardTask applyTask(int accountId, int status);

	List<BizOutwardTask> findByOutwardRequestId(Long reqId);

	/**
	 * 获取已经审核通过的出款请求数据 手机转账 or to_account LIKE CONCAT('%',?3)
	 */
	@Query(nativeQuery = true, value = "select * from " + " biz_outward_task "
			+ " where (?5=2 or status=1) and (?5=1 or status=0) and account_id=?1 and amount=?2 "
			+ " and (?4=2 or to_account=?3 or to_account LIKE CONCAT('%',?3)) and (?4=1 or to_account_owner=?3 ) order by asign_time desc limit 1")
	BizOutwardTask findOutwardTask(int fromAccountId, Float amount, String toAccountOrtoAccountOwner, int type,
								   int cardType);
	
	/**
	 * 获取已经审核通过的出款请求数据 手机转账 or to_account LIKE CONCAT('%',?3)
	 */
	@Query(nativeQuery = true, value = "select * from " + " biz_outward_task "
			+ " where (?5=2 or status=1) and (?5=1 or status=0) and account_id=?1 and amount=?2 "
			+ " and (?4=2 or to_account=?3 or to_account LIKE CONCAT('%',?3)) and (?4=1 or to_account_owner LIKE CONCAT('%',?3,'%')) order by asign_time desc limit 1")
	BizOutwardTask findOutwardTaskUseLike(int fromAccountId, Float amount, String toAccountOrtoAccountOwner, int type,
								   int cardType);

	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_outward_task set status=?3,remark=?4,screenshot=?5 where id=?1 and status=?2")
	int updateStatusAndRemark(long id, int oriStatus, int desStatus, String remark, String screenshot);

	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_outward_task set status=?3,remark=?4,screenshot=?5,time_consuming=?6 where id=?1 and status=?2")
	int updateStatusAndRemark(long id, int oriStatus, int desStatus, String remark, String screenshot,
							  long timeConsume);

	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_outward_task set status=?3,remark=?4 where id=?1 and status=?2 and operator is null and account_id is null")
	int updateStatusAndRemarkForUndeposit(long id, int oriStatus, int desStatus, String remark);

	/**
	 * 获取未出款的大任务
	 */
	@Query(nativeQuery = true, value = "select t.* from biz_outward_task t inner join biz_outward_request t1 "
			+ " on (t.outward_request_id = t1.id and t.status = '0' and t.account_id is null and t.operator is null and t.amount >= ?1 and t.amount <= ?2) "
			+ " inner join biz_handicap t2 on (t.handicap = t2.code and t2.zone = ?3 ) where instr(t.remark,?4) =0 and instr(t.remark,'重新分配') =0 and t2.id not in (?5) order by t.amount desc limit 1")
	BizOutwardTask findBigOutwardTask(int min, int max, int zone, String alias, String[] handicapids);

	/**
	 * 获取同行未出款的大任务
	 */
	@Query(nativeQuery = true, value = "select t.* from biz_outward_task t inner join biz_outward_request t1 "
			+ " on (t.outward_request_id = t1.id and instr(t1.to_account_bank,?4)>0 and t.status = '0' and t.account_id is null and t.operator is null and t.amount >= ?1 and t.amount <= ?2) "
			+ " inner join biz_handicap t2 on (t.handicap = t2.code and t2.zone = ?3 ) where instr(t.remark,?5) =0 and instr(t.remark,'重新分配') =0 and t2.id not in (?6) order by t.amount desc limit 1")
	BizOutwardTask findSameBankBigOutwardTask(int min, int max, int zone, String bankName, String alias,
											  String[] handicaps);

	@Query(nativeQuery = true, value = "select * from " + " biz_outward_task "
			+ " where status=?5 and account_id=?1 and amount=?2 "
			+ " and (?4=2 or to_account=?3 or to_account LIKE CONCAT('%',?3)) and (?4=1 or to_account_owner=?3 ) order by asign_time desc limit 1")
	BizOutwardTask findReuseTask(int fromAccountId, Float amount, String toAccountOrtoAccountOwner, int type,
								 int status);

	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_outward_task set status=?2 where id=?1")
	void updateStatusById(long id, int status);

	@Query(nativeQuery = true, value = "select * from biz_outward_task t where t.status = '0' and t.operator is null and t.account_id is not null and t.asign_time is not null")
	List<BizOutwardTask> getAllAutoTask();

	@Query(nativeQuery = true, value = "select * from biz_outward_task where order_no=?1 order by id desc limit 1")
	BizOutwardTask findLatestByOrderNo(String orderNo);

	@Query(nativeQuery = true, value = "select t.* from biz_outward_task t join biz_account t1 on t.account_id = t1.id where t.status = 0 and t1.flag = 2 and t.operator is null and t.asign_time < ?1")
	List<BizOutwardTask> getExpireOutwardTask(String expireTime);

	@Query(nativeQuery = true, value = "select * from biz_outward_task t where t.status=?1 and t.account_id is not null and t.operator is null and asign_time is not null and (UNIX_TIMESTAMP(t.asign_time)+time_consuming) <?2 limit 300")
	List<BizOutwardTask> getExpireUnknown(int status, long expireSeconds);

	BizOutwardTask findDistinctByOrderNoAndHandicap(String orderNo, String handicapCode);
	
	@Query(nativeQuery = true, value = "SELECT t.id FROM biz_outward_task t INNER JOIN biz_outward_request t1 ON t.outward_request_id = t1.id WHERE	t. STATUS = 0 AND t.account_id IS NULL AND t.operator IS NULL AND t1.create_time >= ?1")
	List<Long> getAllUndepositTask(String dayTime);
	
	@Query(nativeQuery = true, value = "select id from biz_outward_task t where t.asign_time is null and t.account_id is null and t.operator is null and t.status = 0")
	List<Long> findOutwardTaskStatusByIds();
}