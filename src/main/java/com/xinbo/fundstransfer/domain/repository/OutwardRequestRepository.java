package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import com.xinbo.fundstransfer.domain.entity.BizOutwardRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public interface OutwardRequestRepository extends BaseRepository<BizOutwardRequest, Long> {
	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = " INSERT INTO biz_outward_request (order_no,handicap,member,amount,create_time,status,remark,reviewer,to_account,to_account_owner,to_account_bank,review,member_code,to_account_name,level,update_time,time_consuming)"
			+ " VALUES (:orderNo,:handicap,null,:amount,now(),0,:remark,:reviewer,:toAccount,:toAccountOwner,:toAccountBank,:review,null,null,null,null,null) ")
	int addCompanyExpend(@Param("orderNo") String orderNo, @Param("handicap") Integer handicap,
			@Param("amount") BigDecimal amount, @Param("remark") String remark, @Param("reviewer") Integer reviewer,
			@Param("toAccount") String toAccount, @Param("toAccountOwner") String toAccountOwner,
			@Param("toAccountBank") String toAccountBank, @Param("review") String review);

	/**
	 * 更新公司用款
	 */
	@Modifying
	@Transactional(isolation = Isolation.SERIALIZABLE)
	@Query(nativeQuery = true, value = " update biz_outward_request set remark=:remarks,reviewer=:userId,status=1, time_consuming = ((now() - create_time)/1000) where id=:reqId ")
	int updateForCompanyExpend(@Param("reqId") Long reqId, @Param("remarks") String remarks,
			@Param("userId") Integer userId);

	/**
	 * @param reqId
	 *            获取出款请求的时候更新指定reqId的出款请求 并且 reviewer 为 null的记录
	 * @param userId
	 *            更新为指定审核人
	 * @return
	 */
	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = " update biz_outward_request set reviewer=:userId where id=:reqId and reviewer is null ")
	int updateById(@Param("reqId") Long reqId, @Param("userId") Integer userId);

	/**
	 * @param reqId
	 *            主管审核通过的时候更新指定reqId记录 并且状态status为3且不是1的记录
	 * @param userId
	 *            主管id
	 * @return
	 */
	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = " update biz_outward_request set status=1, reviewer=:userId where id=:reqId and status!=1")
	int updateByIdAndStatus(@Param("reqId") Long reqId, @Param("userId") Integer userId);

	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_outward_request set status=?4,remark=?3,member_code=?2,update_time=now(),reviewer=?9, time_consuming=?10 where id=?1 and status!=?5 and status!=?6 and status!=?7 and status!=?8")
	void cancelOrRefuse(long reqId, String memberCode, String remark, int finalStatus, int statusCanceled,
			int statusReject, int statusAcknowledged, int statusFailure, int operatorId, int consumtime);

	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_outward_request set status=?4,remark=?3,member_code=?2,update_time=now(),reviewer=?9 where id=?1 and status!=?5 and status!=?6 and status!=?7 and status!=?8")
	void cancelOrRefuse(long reqId, String memberCode, String remark, int finalStatus, int statusCanceled,
			int statusReject, int statusAcknowledged, int statusFailure, int operatorId);

	/**
	 * 根据审核人ID与任务状态查询历史记录
	 *
	 * @param userid
	 * @return
	 */
	@Query(nativeQuery = true, value = " SELECT * from biz_outward_request where 1=1 and status= 0 and member is not null  and  reviewer=?1 ")
	List<BizOutwardRequest> findByReviewerAndStatus(int userid);

	/**
	 * 根据盘口与订单号查找唯一记录
	 *
	 * @param handicap
	 * @param orderNo
	 * @return
	 */
	BizOutwardRequest findByHandicapAndOrderNo(int handicap, String orderNo);

	/**
	 * 获取待审核的出款请求：有盘口层级限制 ，获取一条， member不为 null表示平台提单 <br>
	 * ,amount desc
	 * 
	 * @return
	 */
	@Query(nativeQuery = true, value = "select * from biz_outward_request where handicap in (?1) and level in (?2) and reviewer is null and status = 0  and member is not null  and (?3 is null or amount <=?3) order by create_time asc  limit 1")
	BizOutwardRequest getApproveTaskIn(List<Integer> handicap, List<Integer> level, BigDecimal amountLimit);

	/**
	 * 获取待审核的出款请求:没有盘口层级限制 ，获取一条， member不为 null表示平台提单 <br>
	 * ,amount desc
	 * 
	 * @return
	 */
	@Query(nativeQuery = true, value = "select * from biz_outward_request where reviewer is null and status = 0 and member is not null and (?1 is null or amount <=?1) order by  create_time asc  limit 1")
	BizOutwardRequest getApproveTask(BigDecimal amountLimit);

	/**
	 * 获取已经审核通过的出款请求数据
	 *
	 * @param account
	 * @param status
	 * @return
	 */
	@Query(nativeQuery = true, value = "select A.* from " + " biz_outward_request A,biz_outward_task B "
			+ " where A.id=B.outward_request_id and B.status=1 "
			+ " and (?4=2 or A.amount=?3 ) and (?4=1 or A.amount>?3 ) and A.to_account=?1 and A.status in (?2) order by A.create_time desc ")
	List<BizOutwardRequest> findByAccountAndStatusAndAmount(String account, List<Integer> status, Float amount,
			int type);

	/**
	 * 获取已经审核通过的出款请求数据
	 */
	@Query(nativeQuery = true, value = "select A.* from " + " biz_outward_request A,biz_outward_task B "
			+ " where A.id=B.outward_request_id and B.status=1 "
			+ " and (?4=2 or A.amount=?3 ) and (?4=1 or A.amount>?3 ) and A.to_account_owner=?1 and A.status in (?2) order by A.create_time desc ")
	List<BizOutwardRequest> findByAccountOwnerAndStatusAndAmount(String accountOwner, List<Integer> status,
			Float amount, int type);

	@Query(value = "select r.id,r.orderNo , r.handicap, r.level,r.amount,r.status,r.createTime ,r.member,t.amount,t.status,t.asignTime,t.remark ,t.accountId,r.updateTime,r.toAccountOwner,t.id"
			+ " from BizOutwardRequest r,BizOutwardTask t "
			+ " where t.outwardRequestId = r.id  and (:orderNo is null or r.orderNo like :orderNo ) "
			+ " and (:member is null or r.member like :member)  and (:startTime is null or (r.updateTime between :startTime and :endTime and t.status not in (7)))"
			+ " and (:handicap=0 or r.handicap=:handicap)")
	Page<Object[]> quickQuery(Pageable pageable, @Param("member") String member, @Param("orderNo") String orderNo,
			@Param("startTime") Date startTime, @Param("endTime") Date endTime, @Param("handicap") Integer handicap);

	/**
	 * 快捷查询 出款记录
	 */
	@Query(nativeQuery = true, value = "select r.id,t.id as taskId , r.handicap, r.level,r.order_no , r.amount,t.amount as taskAmount , r.status,t.status as taskStatus ,r.create_time,r.time_consuming  ,t.asign_time ,t.time_consuming as taskTimeConsuming ,r.member,r.remark as reqRemark ,t.remark as taskRemark ,t.screenshot , t.account_id "
			+ " from biz_outward_request r  LEFT JOIN biz_outward_task t "
			+ " on r.id= t.outward_request_id and r.order_no = t.order_no  where 1=1 and r.create_time between (:startTime) and (:endTime) and (:orderNo is null or r.order_no =:orderNo ) and (:member is null or r.member =:member) and r.handicap in(:handicap) order by r.create_time desc limit 500 ")
	List<Object[]> quickQueryForOut(@Param("member") String member, @Param("orderNo") String orderNo,
			@Param("handicap") List<Integer> handicap, @Param("startTime") Date startTime,
			@Param("endTime") Date endTime);

	/**
	 * 快捷查询 出款总记录数
	 */
	@Query(nativeQuery = true, value = "select count(r.id) from biz_outward_request r  LEFT JOIN biz_outward_task t "
			+ " on r.id= t.outward_request_id and r.order_no = t.order_no where  1=1 and (:orderNo is null or r.order_no like :orderNo ) and (:member is null or r.member like :member)  ")
	Long quickQueryCountForOut(@Param("member") String member, @Param("orderNo") String orderNo);

	/**
	 * 快捷查询 出款总金额
	 */
	@Query(nativeQuery = true, value = " select sum(r.amount) as reqAmount ,sum(t.amount) as taskAmount from biz_outward_request r  LEFT JOIN biz_outward_task t "
			+ " on r.id= t.outward_request_id and r.order_no = t.order_no where  1=1 and (:orderNo is null or r.order_no like :orderNo ) and (:member is null or r.member like :member)  ")
	BigDecimal quickQueryReqSumForOut(@Param("member") String member, @Param("orderNo") String orderNo);

	/**
	 * 快捷查询 出款总金额
	 */
	@Query(nativeQuery = true, value = " select sum(t.amount) as taskAmount from biz_outward_request r  LEFT JOIN biz_outward_task t "
			+ " on r.id= t.outward_request_id and r.order_no = t.order_no where  1=1 and (:orderNo is null or r.order_no like :orderNo ) and (:member is null or r.member like :member) ")
	BigDecimal quickQueryTaskSumForOut(@Param("member") String member, @Param("orderNo") String orderNo);

	List<BizOutwardRequest> findAllByStatus(Integer status);

	/**
	 * 回去存在回冲数据的单据
	 *
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	@Query(nativeQuery = true, value = "select D.order_no from"
			+ " (select A.account,A.bank_type,B.from_account,B.amount,B.to_account_owner,B.summary "
			+ " from fundsTransfer.biz_account A,fundsTransfer.biz_bank_log B"
			+ " where A.id=B.from_account and A.type=5 and B.amount%1=0 and B.amount>0 and B.create_time between ?1 and ?2) C,fundsTransfer.biz_outward_task D,biz_outward_request re"
			+ " where re.create_time between ?1 and ?2 and C.to_account_owner=D.to_account_owner and C.amount=D.amount and C.from_account=D.account_id and D.outward_request_id=re.id and DATE_FORMAT(re.create_time,'%Y-%m-%d')!=DATE_FORMAT(re.update_time,'%Y-%m-%d')")
	List<Object> quickBackToRush(String startTime, String endTime);

	String querySqlALL = " select r.id, r.handicap,r.review,r.order_no,r.amount as reqAmount,t.amount,r.reviewer,t.operator,t.account_id,r.to_account,t.status ,r.create_time,t.asign_time,r.remark,t.screenshot, r.to_account_owner,r.to_account_bank,t.id as taskId ,r.status as reqStatus ,t.remark as taskRemark ,t.time_consuming "
			+ " from biz_outward_request r left join biz_outward_task t "
			+ " on t.outward_request_id = r.id where 1=1 and (:startTime is null or r.create_time >= :startTime ) and ( :endTime is null or r.create_time <= :endTime )  and  r.member is null and r.status not in (2,3)  and (:purpose is null or r.review like :purpose)  "
			+ "and ((:operator is null or (1=:operator and t.operator is null and t.account_id is not null )) or (:operator is null or (2=:operator and t.operator is not null and t.account_id is not null))) "
			+ " and (:handicap is null or r.handicap=:handicap) and (:amountStart is null or r.amount >=:amountStart ) and ( :amountEnd is null or r.amount <= :amountEnd ) order by r.create_time desc  ";

	String querySqlALLWithAccount = " select r.id, r.handicap,r.review,r.order_no,r.amount as reqAmount,t.amount,r.reviewer,t.operator,t.account_id,r.to_account,t.status ,r.create_time,t.asign_time,r.remark,t.screenshot, r.to_account_owner,r.to_account_bank,t.id as taskId ,r.status as reqStatus ,t.remark as taskRemark ,t.time_consuming "
			+ " from biz_outward_request r left join biz_outward_task t "
			+ " on t.outward_request_id = r.id where 1=1 and (:startTime is null or r.create_time >= :startTime ) and ( :endTime is null or r.create_time <= :endTime ) and  r.member is null and r.status not in (2,3)  and t.account_id in (:outAccountIdList ) and (:purpose is null or r.review like :purpose)  "
			+ "and ((:operator is null or (1=:operator and t.operator is null and t.account_id is not null )) or (:operator is null or (2=:operator and t.operator is not null and t.account_id is not null))) "
			+ " and (:handicap is null or r.handicap=:handicap) and (:amountStart is null or r.amount >=:amountStart ) and ( :amountEnd is null or r.amount <= :amountEnd ) order by r.create_time desc ";

	/**
	 * 公司用款
	 */
	@Query(nativeQuery = true, value = querySqlALL)
	List<Object[]> queryCompanyExpendALL(Pageable pageable, @Param("handicap") Integer handicap,
			@Param("operator") Integer operator, @Param("amountStart") BigDecimal amountStart,
			@Param("amountEnd") BigDecimal amountEnd, @Param("purpose") String purpose,
			@Param("startTime") Date startTime, @Param("endTime") Date endTime);

	/**
	 * 公司用款根据账号查询
	 */
	@Query(nativeQuery = true, value = querySqlALLWithAccount)
	List<Object[]> queryCompanyExpendALLWithAccount(Pageable pageable, @Param("handicap") Integer handicap,
			@Param("operator") Integer operator, @Param("amountStart") BigDecimal amountStart,
			@Param("amountEnd") BigDecimal amountEnd, @Param("outAccountIdList") List<Integer> outAccountIdList,
			@Param("purpose") String purpose, @Param("startTime") Date startTime, @Param("endTime") Date endTime);

	String countSqlAllNoAccount = "select COUNT (1)  from biz_outward_request r left join biz_outward_task t "
			+ " on t.outward_request_id = r.id where 1=1 and (:startTime is null or r.create_time >=:startTime ) and ( :endTime is null or r.create_time <= :endTime ) and r.member is null and r.status not in (2,3)  and (:purpose is null or r.review like :purpose)  "
			+ "and ((:operator is null or (1=:operator and t.operator is null and t.account_id is not null )) or (:operator is null or (2=:operator and t.operator is not null and t.account_id is not null))) "
			+ " and (:handicap is null or r.handicap=:handicap) and (:amountStart is null or r.amount >=:amountStart ) and ( :amountEnd is null or r.amount <= :amountEnd ) ";
	String countSqlAllWithAccount = "select COUNT (1)  from biz_outward_request r left join biz_outward_task t "
			+ " on t.outward_request_id = r.id where 1=1 and (:startTime is null or r.create_time >=:startTime ) and ( :endTime is null or r.create_time <= :endTime ) and r.member is null and r.status not in (2,3) and  t.account_id in (:outAccountId ) and (:purpose is null or r.review like :purpose)  "
			+ "and ((:operator is null or (1=:operator and t.operator is null and t.account_id is not null )) or (:operator is null or (2=:operator and t.operator is not null and t.account_id is not null))) "
			+ " and (:handicap is null or r.handicap=:handicap) and (:amountStart is null or r.amount >=:amountStart ) and ( :amountEnd is null or r.amount <= :amountEnd ) ";

	/**
	 * 公司用款总记录数
	 */
	@Query(nativeQuery = true, value = countSqlAllNoAccount)
	Long countCompanyExpendAll(@Param("handicap") Integer handicap, @Param("operator") Integer operator,
			@Param("amountStart") BigDecimal amountStart, @Param("amountEnd") BigDecimal amountEnd,
			@Param("purpose") String purpose, @Param("startTime") Date startTime, @Param("endTime") Date endTime);

	/**
	 * 公司用款总记录数
	 */
	@Query(nativeQuery = true, value = countSqlAllWithAccount)
	Long countCompanyExpendAllWithAccount(@Param("handicap") Integer handicap, @Param("operator") Integer operator,
			@Param("amountStart") BigDecimal amountStart, @Param("amountEnd") BigDecimal amountEnd,
			@Param("outAccountId") List<Integer> outAccountId, @Param("purpose") String purpose,
			@Param("startTime") Date startTime, @Param("endTime") Date endTime);

	String sumSqlAll = "select sum(r.amount)  from biz_outward_request r left join biz_outward_task t "
			+ " on t.outward_request_id = r.id where 1=1 and (:startTime is null or r.create_time >=:startTime ) and ( :endTime is null or r.create_time <= :endTime ) and r.member is null and r.status not in (2,3)  and (:purpose is null or r.review like :purpose)  "
			+ "and ((:operator is null or (1=:operator and t.operator is null and t.account_id is not null )) or (:operator is null or (2=:operator and t.operator is not null and t.account_id is not null))) "
			+ " and (:handicap is null or r.handicap=:handicap) and (:amountStart is null or r.amount >=:amountStart ) and ( :amountEnd is null or r.amount <= :amountEnd ) ";

	String sumSqlAllWithAccount = "select sum(r.amount)  from biz_outward_request r left join biz_outward_task t "
			+ " on t.outward_request_id = r.id where 1=1  and (:startTime is null or r.create_time >=:startTime ) and ( :endTime is null or r.create_time <= :endTime )  and r.member is null and r.status not in (2,3) and  t.account_id in (:outAccountId ) and (:purpose is null or r.review like :purpose) "
			+ "and ((:operator is null or (1=:operator and t.operator is null and t.account_id is not null )) or (:operator is null or (2=:operator and t.operator is not null and t.account_id is not null))) "
			+ " and (:handicap is null or r.handicap=:handicap) and (:amountStart is null or r.amount >=:amountStart ) and ( :amountEnd is null or r.amount <= :amountEnd ) ";

	/**
	 * 公司用款总金额
	 */
	@Query(nativeQuery = true, value = sumSqlAll)
	BigDecimal sumCompanyExpendAll(@Param("handicap") Integer handicap, @Param("operator") Integer operator,
			@Param("amountStart") BigDecimal amountStart, @Param("amountEnd") BigDecimal amountEnd,
			@Param("purpose") String purpose, @Param("startTime") Date startTime, @Param("endTime") Date endTime);

	/**
	 * 公司用款总金额
	 */
	@Query(nativeQuery = true, value = sumSqlAllWithAccount)
	BigDecimal sumCompanyExpendAllWithAccount(@Param("handicap") Integer handicap, @Param("operator") Integer operator,
			@Param("amountStart") BigDecimal amountStart, @Param("amountEnd") BigDecimal amountEnd,
			@Param("outAccountId") List<Integer> outAccountId, @Param("purpose") String purpose,
			@Param("startTime") Date startTime, @Param("endTime") Date endTime);

	/**
	 * 统计公司用款
	 */
	@Query(nativeQuery = true, value = "select  substring_index(r.review,'-',1),count(r.review),r.handicap,h.name,sum(r.amount) FROM biz_outward_request r left join biz_handicap h on r.handicap=h.id where 1=1 and  r.member is null "
			+ " and (:handicap is null or r.handicap in (:handicap))  "
			+ " and (:startTime is null or r.create_time >= :startTime)  "
			+ " and (:endTime is null or r.create_time <= :endTime)  "
			+ " group by r.handicap,substring_index(r.review,'-',1)")
	List<Object[]> statisticsCompanyExpenditure(@Param("handicap") List<Integer> handicap,
			@Param("startTime") Date startTime, @Param("endTime") Date endTime);

	BizOutwardRequest findByOrderNo(String orderNo);

	@Query(nativeQuery = true, value = "select * from  biz_outward_request where status=?1 and create_time>date_sub(NOW(),interval 2 day)")
	List<BizOutwardRequest> findByStatusEquals(Integer Status);

	BizOutwardRequest findByCreateTimeEqualsAndMemberIsNull(Date createTime);

	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "INSERT INTO biz_third_out (from_account,to_account,to_account_owner,to_account_bank,amount,remark,handicap,order_no,member,level,create_time,operator) "
			+ " VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10,now() ,?11) ")
	void saveThirdOut(int fromId, String toAccount, String toAccountOwner, String toAccountBank, BigDecimal amount,
			String remark, String handicap, String orderNo, String member, String level, String operator);

	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "INSERT INTO biz_third_out (from_account,to_account,to_account_owner,to_account_bank,amount,remark,handicap,order_no,member,level,create_time,operator,fee) "
			+ " VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10,now() ,?11,?12) ")
	void saveThirdOutWithFee(int fromId, String toAccount, String toAccountOwner, String toAccountBank,
			BigDecimal amount, String remark, String handicap, String orderNo, String member, String level,
			String operator, BigDecimal fee);

	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "INSERT INTO biz_third_out (from_account,to_account,to_account_owner,to_account_bank,amount,fee,remark,handicap,order_no,member,level,create_time,operator) "
			+ " VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, null, null, null,null,now() ,?8) ")
	void saveThirdCashOut(int fromId, String toAccount, String toAccountOwner, String toAccountBank, String amount,
			String fee, String remark, String user);
}