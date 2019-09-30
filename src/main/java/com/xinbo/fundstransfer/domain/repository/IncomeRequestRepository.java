package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.SqlConstants;
import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import com.xinbo.fundstransfer.domain.entity.BizIncomeRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public interface IncomeRequestRepository extends BaseRepository<BizIncomeRequest, Long> {

	BizIncomeRequest findDistinctByOrderNoAndAmountAndHandicap(String orderNo, BigDecimal amount, Integer handicapId);

	@Modifying
	@Query(nativeQuery = true, value = " update biz_income_request set member_real_name=?2 where id=?1")
	void updateRealNameById(Long id, String realName);

	@Modifying
	@Query(nativeQuery = true, value = " update biz_income_request set update_time=?2 ,time_consuming=?3 where id=?1")
	void updateTimeConsumingAndUpdateTime(Long id, Date updateTime, Long timeConsume);

	BizIncomeRequest findFirstByToIdAndStatusOrderByCreateTimeDesc(Integer toId, Integer status);

	List<BizIncomeRequest> findByOrderNo(String orderNo);

	BizIncomeRequest findByHandicapAndOrderNo(int handicap, String orderNo);

	BizIncomeRequest findById2(Long id);

	List<BizIncomeRequest> findByOrderNoAndStatus(String orderNo, int status);

	List<BizIncomeRequest> findByIdIn(List<Long> idList);

	@Lock(value = LockModeType.PESSIMISTIC_WRITE)
	@Query(value = "select req from BizIncomeRequest req where req.id in (:ids) and req.orderNo =:orderNo and status=0 ")
	List<BizIncomeRequest> getIncomReqListByIdAndOrderNo4Update(@Param("ids") List<Long> id,
			@Param("orderNo") String orderNo);

	@Lock(value = LockModeType.PESSIMISTIC_WRITE)
	@Query(value = "select j from BizIncomeRequest j where j.id =:id ")
	BizIncomeRequest getBizIncomeRequestByIdForUpdate(@Param("id") Long id);

	// 根据username 更新停止接单记录
	@Modifying
	@Query(nativeQuery = true, value = "insert into biz_time_sheet (username,create_time,remark,type) values (?1,?3,?2,?4)")
	void SaveStopOrder(String username, String remark, Date date, String type);

	@Query(nativeQuery = true, value = "select id,username,date_format(create_time,'%Y-%m-%d %H:%i:%s')create_time,remark,type from biz_time_sheet where "
			+ "(?1 is null or username like concat('%',?1,'%')) and (?2 is null or type=?2) and (?3 is null or create_time between ?3 and ?4)", countQuery = "select count(1) from biz_time_sheet where "
					+ "(?1 is null or username like concat('%',?1,'%')) and (?2 is null or type=?2) and (?3 is null or create_time between ?3 and ?4)")
	Page<Object> SearStopOrder(String username, String type, String fristTime, String lastTime, Pageable pageable);

	@Modifying
	@Query(nativeQuery = true, value = "update biz_income_request set status=3 where handicap=?1 and order_no in (?2)")
	void cancelOrder(Integer handicap, List<String> orderNoList);

	@Modifying
	@Query(nativeQuery = true, value = "update biz_income_request set from_account=:account where from_id=:accountId and id>0 ;")
	void updateAccountFrom(@Param("accountId") Integer accountId, @Param("account") String account);

	@Modifying
	@Query(nativeQuery = true, value = "update biz_income_request set to_account=:account where to_id=:accountId and id>0 ;")
	void updateAccountTo(@Param("accountId") Integer accountId, @Param("account") String account);

	@Query(nativeQuery = true, value = "select * from biz_income_request where (?7=2 or to_id=?1) and (?7=1 or from_id=?1) and create_time between ?2 and ?3"
			+ " and (status=?4 or status=?5 or (?8=1 or status=3)) and amount=?6 order by status")
	List<BizIncomeRequest> findAllByParms(int account, String starTime, String endTime, int status, int statu,
			BigDecimal amount, int type, int accountType);

	@Query(nativeQuery = true, value = "select * from biz_income_request where to_id=?1 and type in (101,102,103,113) and create_time between ?2 and ?3"
			+ " and (status=?4 or status=?5 or (?7=1 or status=3)) and amount=?6 order by status asc,create_time desc")
	List<BizIncomeRequest> findAllByParmsByType(int account, String starTime, String endTime, int status, int statu,
			BigDecimal amount, int accountType);

	/**
	 * 查询指定盘口所有超时时间的公司（银行卡）入款请求，单次返回100条订单号集合
	 * 
	 * @param handicap
	 * @param start
	 * @param end
	 * @return
	 */
	@Query(nativeQuery = true, value = "select distinct order_no from biz_income_request where status=0 and type=3 and handicap=?1 and create_time between ?2 and ?3 limit 100")
	List<String> findAllTimeout(Integer handicap, Date start, Date end);

	/**
	 * 取消指定盘口所有超时时间的公司（银行卡）入款请求
	 *
	 * @param handicap
	 * @param timeout
	 */
	@Modifying
	@Query(nativeQuery = true, value = "update biz_income_request set status=3 where status=0 and type=3 and handicap=?1 and create_time<=?2")
	void cancelAllTimeout(Integer handicap, Date timeout);

	@Query(nativeQuery = true, value = "select " + " r.handicap," + " r.from_account ," + " r.from_id ,"
			+ " r.member_user_name ," + " r.member_real_name ," + " r.to_account ," + " r.to_id ," + " r.amount ,"
			+ " r.order_no ," + " u.uid ," + " r.update_time," + " r.create_time ," + " r.remark "
			+ SqlConstants.queryFilter_incomeMacthed, countQuery = "select " + " count(1) "
					+ SqlConstants.queryFilter_incomeMacthed)
	Page<Object> findMatchedBySQL(@Param("memberUsername") String memberUsername, @Param("fristTime") String fristTime,
			@Param("lastTime") String lastTime, @Param("startamount") BigDecimal startamount,
			@Param("endamount") BigDecimal endamount, @Param("orderNo") String orderNo,
			@Param("toAccount") String toAccount, @Param("operatorUid") String operatorUid,
			@Param("manual") String manual, @Param("robot") String robot,
			@Param("handicapList") List<Integer> handicapList, Pageable pageable);

	// 查询总计
	@Query(nativeQuery = true, value = "select sum(r.amount) " + SqlConstants.queryFilter_incomeMacthed)
	Object[] findMatchedBySQL_AmountTotal(@Param("memberUsername") String memberUsername,
			@Param("fristTime") String fristTime, @Param("lastTime") String lastTime,
			@Param("startamount") BigDecimal startamount, @Param("endamount") BigDecimal endamount,
			@Param("orderNo") String orderNo, @Param("toAccount") String toAccount,
			@Param("operatorUid") String operatorUid, @Param("manual") String manual, @Param("robot") String robot,
			@Param("handicapList") List<Integer> handicapList);

	@Query(value = "select distinct h.name,l.name,r.memberUserName,r.toAccount,r.amount,t.amount,(r.amount-t.amount),r.orderNo,a.owner,concat(a.bankType,a.bankName) ,b.remark,r.createTime,b.createTime,t.createTime,r.memberRealName	 "
			+ "from BizIncomeRequest r ,BizBankLog b ,BizTransactionLog t,BizHandicap h,BizLevel l,BizAccount a  where 1=1 and r.id=:requestId and t.orderId=:requestId and t.toBanklogId is not null and t.toBanklogId=b.id and h.id = r.handicap and l.id = r.level and a.id = t.toAccount ")
	Object getMatchedInfo(@Param("requestId") Long requestId);

	/**
	 * 查询微信账号记录:根据状态,是否有流水,是否有订单,盘口,层级,账号
	 */
	@Query(nativeQuery = true, value = "SELECT  a.id,a.account,a.balance,a.curr_sys_level,a.status,\n"
			+ "(CASE WHEN (:handicap is null ) then null else (select h.name from fundsTransfer.biz_handicap h where a.handicap_id=h.id and h.id=:handicap) END )as handicapName,\n"
			+ "(case when (:level is null ) then null else (select GROUP_CONCAT(l.name) from fundsTransfer.biz_account_level al,fundsTransfer.biz_level l where al.account_id=a.id and al.level_id=l.id and l.id=:level ) end) as levelName \n"
			+ "FROM fundsTransfer.biz_account a ,fundsTransfer.biz_handicap h,fundsTransfer.biz_level l  where 1=1 and a.type=4 and (:status is null or a.status in(:status))  and (:handicap is null or  h.id=:handicap ) and (:level is null or l.id=:level) \n"
			+ "and (:type is NULL or ((1=:type or 3=:type) and exists(select b.id from fundsTransfer.biz_bank_log b where b.from_account = a.id and b.status=0 ) )\n"
			+ "or ((2=:type or 3=:type) and exists(select r.id from fundsTransfer.biz_income_request r where r.to_id = a.id and r.status=0 ) )) and (:account is null or a.account=:account)  order by a.status  ")
	List<Object> findAlipayAccount(Pageable pageable, @Param("handicap") Integer handicap,
			@Param("level") Integer level, @Param("account") String account, @Param("status") List<Integer> status,
			@Param("type") Integer type);

	/**
	 * 查询微信账号总记录:根据状态,是否有流水,是否有订单,盘口,层级,账号
	 */
	@Query(nativeQuery = true, value = "SELECT count(1) \n"
			+ "FROM fundsTransfer.biz_account a ,fundsTransfer.biz_handicap h,fundsTransfer.biz_level l  where 1=1 and a.type=3 and (:status is null or a.status in(:status))  and (:handicap is null or  h.id=:handicap ) and (:level is null or l.id=:level) \n"
			+ "and (:type is NULL or ((1=:type or 3=:type) and exists(select b.id from fundsTransfer.biz_bank_log b where b.from_account = a.id and b.status=0 ) )\n"
			+ "or ((2=:type or 3=:type) and exists(select r.id from fundsTransfer.biz_income_request r where r.to_id = a.id and r.status=0 ) )) and (:account is null or a.account=:account)  order by a.status ")
	Long countAlipayAccount(@Param("handicap") Integer handicap, @Param("level") Integer level,
			@Param("account") String account, @Param("status") List<Integer> status, @Param("type") Integer type);

	// 查询总计
	@Query(nativeQuery = true, value = "select to_id,sum(case when status=0 then 1 else 0 end)mapping,sum(case when status=1 then 1 else 0 end)mapped,sum(case when status=3 then 1 else 0 end)cancel "
			+ " from fundsTransfer.biz_income_request where create_time between ?1 and ?2 and type=103 group by to_id")
	List<Object> statisticsInto(String startTime, String endTime);

	// 根据金额、状态未匹配、入款卡一样查询记录
	@Query(nativeQuery = true, value = "select * from biz_income_request where create_time>(select DATE_ADD(now(),INTERVAL -150 minute)) and status=0 and to_id=?1 and amount=?2")
	List<BizIncomeRequest> findIncomeCounts(int toId, BigDecimal amount);

	// 根据金额 查找兼职提额记录
	@Query(nativeQuery = true, value = "select * from biz_income_request where to_id=?2 and type=401 and amount=?1 and status=0 and create_time between ?3 and ?4 order by create_time desc limit 1")
	BizIncomeRequest findRebateLimit(BigDecimal amount, int fromAccount, String startTime, String endTime);

	@Modifying
	@Query(nativeQuery = true, value = "update biz_income_request set status=?2,update_time=now() where id=?1")
	void updateStatusById(Long id, Integer status);

	@Query(nativeQuery = true, value = "select * from biz_income_request where to_id=?1 and member_real_name=?2 and FLOOR(amount)=?3 and status=?6 and create_time between ?4 and ?5 limit 1")
	BizIncomeRequest findIncome(int accountId, String toAccountOwner, BigDecimal amount, String startTime,
			String endTime, int status);

	@Query(nativeQuery = true, value = "select count(1) from biz_income_request where to_id=?1 and amount=?5 and status=?4 and create_time between ?2 and ?3")
	int findIncomeCounts(int accountId, String startTime, String endTime, int status, BigDecimal amount);

	@Query(nativeQuery = true, value = "select * from biz_income_request where from_id=?1 and amount=?2 and create_time>?3 and create_time<?4 and type>=100")
	List<BizIncomeRequest> find4LatestByFrom(int accountId, BigDecimal amt, Date stTm, Date edTm);

	@Query(nativeQuery = true, value = "select * from biz_income_request where to_id=?1 and amount=?2 and create_time>?3 and create_time<?4 and type>=100")
	List<BizIncomeRequest> find4LatestByTo(int accountId, BigDecimal amt, Date stTm, Date edTm);
	
	@Query(nativeQuery = true, value = "select * from biz_income_request where chat_pay_bzm=?1 and amount=?2 and create_time>?3 and create_time<?4")
	List<BizIncomeRequest> find4BzmAmountCreateTime(String bzm, BigDecimal amt, Date stTm, Date edTm);

}