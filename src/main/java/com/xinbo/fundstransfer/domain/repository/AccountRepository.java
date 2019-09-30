package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.SqlConstants;
import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;

public interface AccountRepository extends BaseRepository<BizAccount, Integer> {
	@Modifying(clearAutomatically = true)
	@Query(nativeQuery = true, value = "select id from biz_account where status=1 and type=1 and sub_type in(0,1,2,3) and passage_id=:passageId and 1 !=:time")
	List<Integer> findPassageIdAndAccountIdsMap(@Param("passageId") Long passageId, @Param("time") Long time);

	@Query(nativeQuery = true, value = "select * from biz_account where status=1 and type=1 and sub_type in(0,1,2,3) and passage_id=:passageId and 1 !=:time")
	List<BizAccount> findAccountByPassageId(@Param("passageId") Long passageId, @Param("time") Long time);

	@Query(nativeQuery = true, value = "select id from biz_account where status=1 and type=1 and sub_type in(0,1,2,3,4) and handicap_id=:handicapid and curr_sys_level =:currsyslevel and 1 !=:time")
	List<Integer> findAccountByHandicapCurrSysLevel(@Param("handicapid") Integer handicapId,
			@Param("currsyslevel") Integer currSysLevel, @Param("time") Long time);

	List<BizAccount> findByStatusAndHandicapIdAndSubTypeAndAccountInAndMinInAmountGreaterThanEqual(Integer status,
			Integer handicapId, Integer subType, List<String> accounts, BigDecimal amount);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query(value = "select  a from BizAccount a where  a.id in(:ids) ")
	List<BizAccount> findByAccountIdsForUpdate(@Param("ids") List<Integer> ids);

	@Query(value = "select   a from BizAccount a where a.type=1  and  a.account in(:accounts) ")
	List<BizAccount> findByAccounts(@Param("accounts") List<String> accounts);

	List<BizAccount> findByTypeAndPassageId(Integer type, Long passageId);

	/**
	 * 描述:根据盘口id 账号类型 子类型 查询账号信息
	 * 
	 * @param handicapId
	 * @param type
	 * @param subType
	 * @return
	 */
	List<BizAccount> findByHandicapIdAndTypeAndSubTypeAndStatus(Integer handicapId, Integer type, Integer subType,
			Integer status);

	List<BizAccount> findByIdIn(Integer[] ids);

	BizAccount findByAccountAndHandicapIdAndBankType(String account, Integer handicap, String bankType);

	List<BizAccount> findAllByTypeIn(Integer[] type);

	BizAccount findById2(Integer id);

	List<BizAccount> findByAlias(String alias);

	List<BizAccount> findByAliasAndTypeIn(String alias, List<Integer> types);

	BizAccount findByAccountAndSubType(String account, Integer subType);

	BizAccount findByHandicapIdAndAccount(int handicapId, String account);

	BizAccount findByHandicapIdAndAccountAndBankType(int handicapId, String account, String bankName);

	List<BizAccount> findByTypeAndAccount(Integer type, String account);

	/**
	 * 更新账号信息 旧更新时间:a.update_time=(case when ?2 = 3 then now() else a.update_time
	 * end)
	 */
	@Modifying(clearAutomatically = true)
	@Query(nativeQuery = true, value = "update biz_account a set a.handicap_id=?1,a.status=?2,a.account=?3,a.bank_name=?4, a.owner=?5,a.type=?6,a.bank_type=?7,a.limit_in=?8 ,a.limit_out=?9,a.alias=?10,a.modifier=?11,a.holder=?12,a.curr_sys_level=?13,a.lowest_out=?14, a.update_time=now(),a.limit_balance=?16,a.peak_balance=?17,a.remark=?18,a.limit_out_one=?19,a.mobile=?20,a.sub_type=?21 where id=?15")
	int updateBaseInfo(Integer handicapId, Integer status, String account, String bankName, String owner, Integer type,
			String bankType, Integer limitIn, Integer limitOut, String alias, Integer modifier, Integer holder,
			Integer currSysLevel, Integer lowestOut, Integer accountId, Integer limitBalance, Integer peakBalance,
			String remark, Integer limitOutOne, String mobile, Integer subType);

	@Modifying(clearAutomatically = true)
	@Query(nativeQuery = true, value = "update biz_account a set a.balance=ifnull(a.balance,0)+?1  where a.id=?2")
	int addBalance(BigDecimal balance, Integer accountId);

	@Transactional
	@Modifying(clearAutomatically = true)
	@Query(nativeQuery = true, value = "update biz_account a set a.bank_balance=?1,a.balance =case when a.balance is null then ?1 else a.balance end  where a.id=?2")
	int updateBankBalance(BigDecimal bankBalance, int id);

	@Query(nativeQuery = true, value = "select distinct a.id from biz_account a,biz_account_level l1,biz_account_level l2 where a.type=1 and a.id=l1.account_id and  l1.level_id = l2.level_id and l2.account_id=?1")
	List<Object> getAccountIdList(Integer accountId);

	List<BizAccount> findByAccount(String account);

	@Query(nativeQuery = true, value = "SELECT distinct b.id  from fundsTransfer.biz_account b,fundsTransfer.biz_account b2  where b.type=1 and  b.status in(3,4) and  b.id <>:accountId and b.handicap_id=b2.handicap_id and b2.curr_sys_level = b.curr_sys_level and b2.id=:accountId")
	List<Integer> findAccountIdInSameLevel(@Param("accountId") Integer accountId);

	@Query(nativeQuery = true, value = " select distinct id from biz_account WHERE alias =:accountAlias or  account like CONCAT('%',:accountAlias) or id =:accountAlias  ")
	List<Integer> queryAccountIdsByAlias(@Param("accountAlias") String accountAlias);

	/**
	 * 获取账号表当前最大的编号
	 *
	 * @return
	 */
	@Query(nativeQuery = true, value = " SELECT max(CAST(alias as signed)) FROM biz_account ")
	String getMaxAlias();

	/**
	 * 查询出款账号总记录数
	 */
	@Query(nativeQuery = true, value = " SELECT count(1) from biz_account where 1=1 and status=1 and (:type is null or status=:type) and bank_balance >0  and (:userId is null or holder=:userId) and (:accountNo is null or account like :accountNo ) ")
	Long count4OutwardAsign(@Param("userId") Integer userId, @Param("type") Integer type,
			@Param("accountNo") String accountNo);

	/**
	 * 对账号进行彻底删除 1清除账号与账号绑定关系
	 *
	 * @param accountId
	 */
	@Modifying
	@Query(nativeQuery = true, value = "delete from biz_account_binding where id>0 and ( account_id=?1 or bind_account_id=?1 ) ;")
	void deleteAndClear_1(Integer accountId);

	/**
	 * 对账号进行彻底删除 2账号与层级的绑定关系
	 *
	 * @param accountId
	 */
	@Modifying
	@Query(nativeQuery = true, value = "delete from biz_account_level where id>0 and account_id=?1 ;")
	void deleteAndClear_2(Integer accountId);

	/**
	 * 对账号进行彻底删除 3操作记录表
	 *
	 * @param accountId
	 */
	@Modifying
	@Query(nativeQuery = true, value = "delete from biz_account_extra where id>0 and account_id=?1 ;")
	void deleteAndClear_3(Integer accountId);

	/**
	 * 对账号进行彻底删除 4平台同步信息
	 *
	 * @param accountId
	 */
	@Modifying
	@Query(nativeQuery = true, value = "delete from biz_account_sync where id>0 and account_id=?1 ;")
	void deleteAndClear_4(Integer accountId);

	/**
	 * 对账号进行彻底删除 5出款操作日志
	 *
	 * @param accountId
	 */
	@Modifying
	@Query(nativeQuery = true, value = "delete from biz_outward_log where id>0 and from_id=?1 ;")
	void deleteAndClear_5(Integer accountId);

	/**
	 * 对账号进行彻底删除 6帐号每日清算汇总报表
	 *
	 * @param accountId
	 */
	@Modifying
	@Query(nativeQuery = true, value = "delete from biz_report where id>0 and account_id=?1 ;")
	void deleteAndClear_6(Integer accountId);

	/**
	 * 对账号进行彻底删除 7账号表
	 *
	 * @param accountId
	 */
	@Modifying
	@Query(nativeQuery = true, value = "delete from biz_account where id=?1 ;")
	void deleteAndClear_7(Integer accountId);

	/**
	 * 对账号进行彻底删除 8账号冻结跟踪表
	 *
	 * @param accountId
	 */
	@Modifying
	@Query(nativeQuery = true, value = "delete from biz_account_trace where id>0 and account_id=?1 ;")
	void deleteAndClear_8(Integer accountId);

	@Modifying
	@Query(nativeQuery = true, value = "delete from biz_account_rebate_day where id>0 and account=?1 ;")
	void deleteAndClear_9(Integer accountId);

	@Modifying
	@Query(nativeQuery = true, value = "delete from biz_account_rebate where id>0 and uid=?1 ;")
	void deleteAndClear_10(String uid);

	@Modifying
	@Query(nativeQuery = true, value = "delete from biz_account_more where id>0 and moible=?1 ;")
	void deleteAndClear_11(String mobile);

	@Modifying
	@Query(nativeQuery = true, value = "delete from biz_income_request where id>0 and type=401 and to_id=?1 ;")
	void deleteAndClear_12(Integer accountId);

	// 查询出第三方下发单记录
	@Query(nativeQuery = true, value = "select from_id,from_account,to_id,to_account,order_no,amount,fee,status,date_format(create_time,'%Y-%m-%d %H:%i:%s') from biz_income_request where create_time between ?2 and ?3 and from_id=?1", countQuery = SqlConstants.SEARCH_FINDISSUEDTHIRD_COUNTQUERY)
	Page<Object> findIssuedThird(int fromId, String startTime, String endTime, Pageable pageable);

	// 查询出第三方出现金记录
	@Query(nativeQuery = true, value = "select from_account,to_account,to_account_owner,to_account_bank,amount,fee,remark,date_format(create_time,'%Y-%m-%d %H:%i:%s'),operator from biz_third_out where create_time between ?2 and ?3 and handicap is null and from_account=?1", countQuery = SqlConstants.SEARCH_FINDENCASHTHIRD_COUNTQUERY)
	Page<Object> findEncashThird(int fromId, String startTime, String endTime, Pageable pageable);

	// 查询出第三方出给会员的记录
	@Query(nativeQuery = true, value = "select from_account,to_account,to_account_owner,to_account_bank,amount,fee,remark,date_format(create_time,'%Y-%m-%d %H:%i:%s'),operator,handicap,order_no,member,level from biz_third_out where create_time between ?2 and ?3 and handicap is not null and from_account=?1 and (?4 is null or handicap=?4)", countQuery = SqlConstants.SEARCH_FINDMEMBERSTHIRD_COUNTQUERY)
	Page<Object> findMembersThird(int fromId, String startTime, String endTime, String handicapCode, Pageable pageable);

	// 查询出款卡银行流水
	@Query(nativeQuery = true, value = "select * from biz_bank_log where from_account in (select id from biz_account where type=5 and handicap_id in (?4) and (?3 is null or bank_type like concat('%',?3,'%'))) and create_time between ?1 and ?2")
	Page<Object> findOutBankLog(String startTime, String endTime, String bankType, List<Integer> handicaps,
			Pageable pageable);

	// 查询所有银行卡七点的余额
	@Query(nativeQuery = true, value = "select B.owner,B.bank_type,A.balance,B.handicap_id,B.type,A.account_id from fundsTransfer.biz_report A,fundsTransfer.biz_account B where A.time=?1 and handicap_id in (?2) and A.account_id=B.id")
	Page<Object> find7TimeBalance(String startTime, List<Integer> handicaps, Pageable pageable);

	// 修改出款在用银行卡当日出款限额
	@Modifying
	@Query(nativeQuery = true, value = "update biz_account set limit_out=(case when curr_sys_level=1 then (case when ?1 is null then limit_out else ?1 end) when curr_sys_level=2 then (case when ?3 is null then limit_out else ?3 end) when curr_sys_level=4 then (case when ?2 is null then limit_out else ?2 end) when curr_sys_level=8 then (case when ?4 is null then limit_out else ?4 end) end) where type=?5 and status=?6 and (''=?7 or '0'=?7 and (flag is null or flag =0) or '1'=?7 and flag =1) and (?8 is null or bank_type =?8 )")
	void updateOuterLimit(Integer outerLimit, Integer middleLimit, Integer innerLimit, Integer specifyLimit,
			Integer type, Integer status, String flag, String bankType);

	@Modifying
	@Query(nativeQuery = true, value = "update biz_account set limit_out=(case when curr_sys_level=1 then (case when ?1 is null then limit_out else ?1 end) when curr_sys_level=2 then (case when ?3 is null then limit_out else ?3 end) when curr_sys_level=4 then (case when ?2 is null then limit_out else ?2 end) when curr_sys_level=8 then (case when ?4 is null then limit_out else ?4 end) end) where type=?5 and status=?6 and handicap_id=?7  and (''=?8 or '0'=?8 and (flag is null or flag =0) or '1'=?8 and flag =1) and (?9 is null or bank_type =?9 )")
	void updateOuterLimit(Integer outerLimit, Integer middleLimit, Integer innerLimit, Integer specifyLimit,
			Integer type, Integer status, Integer handicapId, String flag, String bankType);

	/**
	 * sub_type=0:任何类型入款卡修改额度后，就不是云闪付入款卡等，转为普通入款卡
	 * 
	 * @param outEnable
	 * @param bankIds
	 * @param limitPercentage
	 * @param minBalance
	 */
	@Modifying
	@Query(nativeQuery = true, value = "update biz_account set out_enable=:outEnable , limit_percentage=:limitPercentage , min_balance=:minBalance ,sub_type=:subType where id = :bankIds")
	void batchUpdateLimit(@Param("outEnable") Integer outEnable, @Param("bankIds") Integer bankIds,
			@Param("limitPercentage") String limitPercentage, @Param("minBalance") String minBalance,
			@Param("subType") Integer subType);

	@Transactional
	@Modifying(clearAutomatically = true)
	@Query(nativeQuery = true, value = "update biz_account a set a.gps=?2  where a.id=?1")
	int updateGPS(Integer accountId, String gps);

	// 查询备用卡系统下发记录
	@Query(nativeQuery = true, value = "select A.to_banklog_id,A.from_account from biz_transaction_log A,biz_account B "
			+ " where  A.type=103 and B.type in (12,13) and A.to_account=B.id and A.create_time between ?1 and ?2")
	List<Object> findTransfers(String startTime, String endTime);

	@Query(nativeQuery = true, value = " SELECT A.id,A.account,A.alias ,A.bank_type,A.owner,A.status,"
			+ " B.counts,H.name " + SqlConstants.queryFilter_findIncomeAccountOrderByBankLog
			+ " ORDER BY counts DESC, A.status ASC ", countQuery = "select " + " count(1) "
					+ SqlConstants.queryFilter_findIncomeAccountOrderByBankLog)
	Page<Object> findIncomeAccountOrderByBankLog(@Param("handicapList") Integer[] handicapList,
			@Param("account") String account, @Param("alias") String alias, @Param("bankType") String bankType,
			@Param("owner") String owner, @Param("search_IN_flag") Integer[] search_IN_flag,
			@Param("accountStatusList") Integer[] accountStatusList, @Param("pageable") Pageable pageable);

	@Query(nativeQuery = true, value = "select id from biz_account where type in (:type) and handicap_id in (:handicaps)")
	List<Integer> findAccountList(@Param("type") List<Integer> type, @Param("handicaps") List<Integer> handicaps);

	@Query(value = "select id from BizAccount where flag is not null and flag = 2 and status!=6")
	List<Integer> findAccountId4Rebate();

	@Query(nativeQuery = true, value = "select  id, " + "account," + "bank_name," + "status," + "type," + "owner,"
			+ "balance," + "bank_balance," + "handicap_id, " + "limit_in," + "limit_out," + "alias, " + "creator,"
			+ "date_format(create_time,'%Y-%m-%d %H:%i:%s')create_time,"
			+ "date_format(update_time,'%Y-%m-%d %H:%i:%s')update_time," + "modifier," + "bank_type," + "holder,"
			+ "sign," + "hook," + "curr_sys_level," + "usable_balance," + "hub,    remark, " + "bing, " + "lowest_out, "
			+ "limit_balance, " + "peak_balance," + "limit_out_one, " + "mobile,    gps," + "flag,"
			+ "limit_out_one_low," + "limit_out_count"
			+ " from biz_account where (?3='goldener' or status=-2) and (?3='fin' or status in (3,4)) and (?3='fin' or  (update_time<date_add(now(),interval -30 day)))"
			+ " and (?1=0 or handicap_id=?1) and (?2 is null or alias like concat('%',?2,'%'))"
			+ " and (?4 is null or flag=?4) and (?5 is null or status=?5) order by update_time", countQuery = SqlConstants.SEARCH_FINDDELETEACCOUNT_COUNTQUERY)
	Page<Object> findDeleteAccount(int handicap, String alias, String type, String flag, String status,
			Pageable pageable);

	// 查询总计
	@Query(nativeQuery = true, value = SqlConstants.totalFindDeleteAccount)
	java.lang.Object[] totalFindDeleteAccount(int handicap, String alias, String type, String flag, String status);

	@Modifying(clearAutomatically = true)
	@Query(nativeQuery = true, value = "update biz_account set status=?2 where id in(?1)")
	void updateAcountById(List<String> accountIds, int status);

	@Transactional
	@Modifying(clearAutomatically = true)
	@Query(nativeQuery = true, value = "update biz_account a1,biz_account a2 SET a1.balance=case when a1.balance is null then null else a1.balance-?1-?4 end,a2.balance=case when a2.balance is null then null else a2.balance+?1 end where a1.id=?2 and a2.id=?3")
	int updSysBal(BigDecimal balance, Integer frId, Integer toId, BigDecimal fee);

	@Transactional
	@Modifying(clearAutomatically = true)
	@Query(nativeQuery = true, value = "update biz_account a SET a.balance=case when a.balance is null then null else a.balance-?1-?3 end where a.id=?2")
	int updSysBal(BigDecimal balance, Integer frId, BigDecimal fee);

	@Transactional
	@Modifying(clearAutomatically = true)
	@Query(nativeQuery = true, value = "update biz_account set balance=null where id>0 and type!=2 ")
	int sysBalInit();

	@Query(nativeQuery = true, value = "select id from biz_account where status = ?1 and holder is null and type in (1,5,8,13,14)")
	List<Integer> findAllByStatus(int status);

	@Query(nativeQuery = true, value = "select t.*  from biz_account t where t.flag = 2 and t.type in (?1) "
			+ " and t.status in (?2) " + " and t.handicap_id in (?3) "
			+ " and (?4 is null or t.curr_sys_level in (?4)) " + " and (?5 is null or t.bank_type = ?5) "
			+ " and (?6 is null or t.account like concat('%',?6,'%')) "
			+ " and (?7 is null or t.owner like concat('%',?7,'%')) " + " and (?8 is null or t.alias = ?8) "
			+ " and exists( select 1 from biz_account_more t1 where t.mobile = t1.moible and t1.margin <= 1000) order by t.id ", countQuery = "select count(1)  from biz_account t where t.flag = 2 and t.type in (?1) "
					+ " and t.status in (?2) " + " and t.handicap_id in (?3) "
					+ " and (?4 is null or t.curr_sys_level in (?4)) " + " and (?5 is null or t.bank_type = ?5) "
					+ " and (?6 is null or t.account like concat('%',?6,'%')) "
					+ " and (?7 is null or t.owner like concat('%',?7,'%')) " + " and (?8 is null or t.alias = ?8) "
					+ " and exists( select 1 from biz_account_more t1 where t.mobile = t1.moible and t1.margin <= 1000)")
	Page<BizAccount> findToBeRaisePage(List<Integer> type, List<Integer> status, List<Integer> handicapId,
			List<Integer> currSysLevel, String bankType, String account, String owner, String alias, Pageable pageable);

	@Query(nativeQuery = true, value = "select sum(bank_balance)  from biz_account t where t.flag = 2 and t.type in (?1) "
			+ " and t.status in (?2) " + " and t.handicap_id in (?3) "
			+ " and (?4 is null or t.curr_sys_level in (?4)) " + " and (?5 is null or t.bank_type = ?5) "
			+ " and (?6 is null or t.account like concat('%',?6,'%')) "
			+ " and (?7 is null or t.owner like concat('%',?7,'%')) " + " and (?8 is null or t.alias = ?8) "
			+ " and exists( select 1 from biz_account_more t1 where t.mobile = t1.moible and t1.margin <= 1000)")
	BigDecimal getTotalBankBalance(List<Integer> type, List<Integer> status, List<Integer> handicapId,
			List<Integer> currSysLevel, String bankType, String account, String owner, String alias);

	@Query(nativeQuery = true, value = "select ifnull(sum(peak_balance),0.00) from biz_account where id in (?1)")
	BigDecimal getTotalAmountsByAc(List<String> accountsList);

	@Transactional
	@Modifying(clearAutomatically = true)
	@Query(nativeQuery = true, value = "update biz_account set flag=?2 where id=?1")
	void updateFlagById(Integer id, Integer flag);

	@Query(nativeQuery = true, value = "select * from biz_account where type = '5' and status = '1' and holder is not null and flag = '0'")
	List<BizAccount> findOutAccList4Manual();

	@Query(nativeQuery = true, value = "select * from ( select D.*,A.* from biz_account A left join "
			+ " (select B.linelimit,C.uid,C.user_name,B.moible,B.margin,B.remark morRemark from biz_account_more B,biz_rebate_user C where B.uid=C.uid) D "
			+ " on A.mobile=D.moible where flag=2 "
			+ " and (?1 is null or A.handicap_id=?1) and (?2 is null or A.bank_type like concat('%',?2,'%'))"
			+ " and A.type in (?3) and (?12 is null or A.sub_type=?12) "
			+ " and (?4 is null or A.alias=?4) and (?5 is null or A.account like concat('%',?5,'%')) and (?6 is null or A.owner like concat('%',?6,'%'))"
			+ " and (?7 is null or A.curr_sys_level=?7) and A.status in (?8) and (?9 is null or D.user_name like concat('%',?9,'%'))"
			+ " order by margin desc,uid desc) N where (?10 is null or N.margin>=?10) and (?11 is null or N.margin<=?11)", countQuery = SqlConstants.SEARCH_SHOWREBATE_USER)
	Page<Object> showRebateUser(String handicapId, String bankType, Integer[] type, String alias, String account,
			String owner, String currSysLevel, Integer[] status, String rebateUser, BigDecimal startAmount,
			BigDecimal endAmount, String subType, Pageable pageable);

	@Query(nativeQuery = true, value = "select sum(N.bank_balance) as bankBalanceTotal,sum(N.margin) as marginTotal from ( select D.margin,A.bank_balance from biz_account A left join "
			+ " (select B.linelimit,C.uid,C.user_name,B.moible,B.margin,B.remark morRemark from biz_account_more B,biz_rebate_user C where B.uid=C.uid) D "
			+ " on A.mobile=D.moible where flag=2 "
			+ " and (?1 is null or A.handicap_id=?1) and (?2 is null or A.bank_type like concat('%',?2,'%'))"
			+ " and A.type in (?3) and (?12 is null or A.sub_type=?12) "
			+ " and (?4 is null or A.alias=?4) and (?5 is null or A.account like concat('%',?5,'%')) and (?6 is null or A.owner like concat('%',?6,'%'))"
			+ " and (?7 is null or A.curr_sys_level=?7) and A.status in (?8) and (?9 is null or D.user_name like concat('%',?9,'%'))"
			+ " order by margin desc,uid desc) N where (?10 is null or N.margin>=?10) and (?11 is null or N.margin<=?11)")
	java.lang.Object[] getBankBalanceTotal(String handicapId, String bankType, Integer[] type, String alias,
			String account, String owner, String currSysLevel, Integer[] status, String rebateUser,
			BigDecimal startAmount, BigDecimal endAmount, String subType);

	@Modifying
	@Query(nativeQuery = true, value = "update biz_account set sub_type =3,out_enable=0 where id =:bankIds")
	void batchUpdateToQuickPay(@Param("bankIds") Integer bankIds);

	@Query(value = " call ClearingDataProc(:inParam1)", nativeQuery = true)
	void executeStored(@Param("inParam1") String data);

	@Modifying
	@Query(nativeQuery = true, value = "DELETE FROM biz_income_request WHERE status=3 and create_time <:data")
	void cleanIncomeDate(@Param("data") String data);

	@Query(value = " call deleteBusinessData()", nativeQuery = true)
	void deleteBusinessData();
}