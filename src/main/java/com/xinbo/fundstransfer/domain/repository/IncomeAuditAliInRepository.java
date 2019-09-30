package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.xinbo.fundstransfer.SqlConstants;
import com.xinbo.fundstransfer.domain.entity.AccountStatistics;
import com.xinbo.fundstransfer.domain.entity.BizAliLog;

public interface IncomeAuditAliInRepository extends BaseRepository<BizAliLog, Long> {

	// 根据参数统计入款支付宝未匹配的流水
	@Query(nativeQuery = true, value = "select A.from_account,count(1),B.handicap_id,B.account from biz_alipay_log A,biz_account B "
			+ " where A.from_account=B.id and"
			+ " A.status=0 and A.create_time between ?3 and ?4 and (?1=0 or B.handicap_id=?1) and (?2=null or B.account like concat('%',?2,'%')) group by from_account", countQuery = SqlConstants.statisticalAliLog)
	Page<Object> statisticalAliLog(int handicap, String AliNumber, String startTime, String endTime, Pageable pageable);

	// 查询支付宝入款已经匹配的单子
	@Query(nativeQuery = true, value = "select A.id,A.handicap,A.member_name,A.order_no,A.amount,B.depositor,A.alipayid,date_format(A.update_time,'%Y-%m-%d %H:%i:%s'),A.remark"
			+ " from biz_alipay_request A,biz_alipay_log B,biz_account C"
			+ " where A.update_time between ?2 and ?3 and (?4 is null or A.member_name like concat('%',?4,'%')) and (?5 is null or A.order_no =?5)"
			+ " and (?8 is null or B.depositor like concat('%',?8,'%')) and (?9 is null or C.account like concat('%',?9,'%'))"
			+ " and (?6=0 or A.amount>=?6) and (?7=0 or A.amount<=?7) and (?1=0 or A.handicap=?1)"
			+ " and A.alipay_log_id=B.id and A.alipayid=C.id and A.status=1 and B.status=1", countQuery = SqlConstants.findAliMatched)
	Page<Object> findAliMatched(int handicap, String startTime, String endTime, String member, String orderNo,
			BigDecimal fromAmount, BigDecimal toAmount, String payer, String AliNumber, Pageable pageable);

	// 统计
	@Query(nativeQuery = true, value = SqlConstants.totalFindAliMatched)
	java.lang.Object[] totalFindAliMatched(int handicap, String startTime, String endTime, String member,
			String orderNo, BigDecimal fromAmount, BigDecimal toAmount, String payer, String AliNumber);

	// 查询支付宝入款取消的单
	@Query(nativeQuery = true, value = "select A.id,A.handicap,A.member_name,A.order_no,A.amount,A.alipayid,date_format(A.update_time,'%Y-%m-%d %H:%i:%s'),A.remark"
			+ " from biz_alipay_request A "
			+ " where A.update_time between ?2 and ?3 and (?4 is null or A.member_name like concat('%',?4,'%')) and (?5 is null or A.order_no =?5)"
			+ " and (?6=0 or A.amount>=?6) and (?7=0 or A.amount<=?7) and (?1=0 or A.handicap=?1)"
			+ " and A.status=3 ", countQuery = SqlConstants.findAliCanceled)
	Page<Object> findAliCanceled(int handicap, String startTime, String endTime, String member, String orderNo,
			BigDecimal fromAmount, BigDecimal toAmount, Pageable pageable);

	// 统计
	@Query(nativeQuery = true, value = SqlConstants.totalFindAliCanceled)
	java.lang.Object[] totalFindAliCanceled(int handicap, String startTime, String endTime, String member,
			String orderNo, BigDecimal fromAmount, BigDecimal toAmount);

	// 查询支付宝入款取消的单
	@Query(nativeQuery = true, value = "select A.id,A.from_account,A.status,date_format(A.trading_time,'%Y-%m-%d %H:%i:%s')trading_time,A.amount,A.balance,A.remark,A.summary,A.depositor,date_format(A.create_time,'%Y-%m-%d %H:%i:%s')create_time,B.handicap_id "
			+ " from biz_alipay_log A,biz_account B " + " where A.create_time between ?2 and ?3"
			+ " and (abs(TIMESTAMPDIFF(hour,now(),A.create_time))) >=24 and (abs(TIMESTAMPDIFF(hour,now(),A.create_time)))<=48 and A.status=0"
			+ " and (?1=0 or B.handicap_id=?1) and (?4 is null or A.depositor like concat('%',?4,'%')) and (?5 is null or B.account like concat('%',?5,'%'))"
			+ " and (?6=0 or A.amount>=?6) and (?7=0 or A.amount<=?7) and A.from_account=B.id ", countQuery = SqlConstants.findAliUnClaim)
	Page<Object> findAliUnClaim(int handicap, String startTime, String endTime, String member, String AliNo,
			BigDecimal fromAmount, BigDecimal toAmount, Pageable pageable);

	// 统计
	@Query(nativeQuery = true, value = SqlConstants.totalFindAliUnClaim)
	java.lang.Object[] totalFindAliUnClaim(int handicap, String startTime, String endTime, String member, String AliNo,
			BigDecimal fromAmount, BigDecimal toAmount);

	// 查询是否存在重复的流水
	@Query(nativeQuery = true, value = "select id,from_account,status,trading_time,amount,balance,remark,summary,depositor,create_time from biz_alipay_log"
			+ " where from_account=?1 and amount=?2 and balance=?3 and trading_time=?4")
	List<Object[]> findRepeatAliLog(int fromAccount, BigDecimal amount, BigDecimal balance, String tradingTime);

	// 保存支付宝流水
	@Modifying
	@Query(nativeQuery = true, value = "insert into biz_alipay_log(from_account,trading_time,amount,balance,summary,depositor,create_time,status) values (?1,?2,?3,?4,?5,?6,now(),0)")
	void saveAliLog(int fromAccount, String tradingTime, BigDecimal amount, BigDecimal balance, String summary,
			String depositor);

	// 根据支付宝账号id查询支付宝入款单信息
	@Query(nativeQuery = true, value = "select id,alipayid,level,handicap,status,amount,date_format(create_time,'%Y-%m-%d %H:%i:%s'),remark,order_no,operator,member_name,member_id  "
			+ " from biz_alipay_request " + " where alipayid=?1 and create_time between ?2 and ?3 and status=0 "
			+ " and (?4 is null or member_name like concat('%',?4,'%')) and (?5 is null or order_no =?5)"
			+ " and (?6=0 or amount>=?6) and (?7=0 or amount<=?7) ", countQuery = SqlConstants.findAliMBAndInvoice)
	Page<Object> findInvoice(int AliId, String startTime, String endTime, String member, String orderNo,
			BigDecimal fromAmount, BigDecimal toAmount, Pageable pageable);

	// 统计
	@Query(nativeQuery = true, value = SqlConstants.totalfindAliMBAndInvoice)
	java.lang.Object[] totalFindInvoice(int AliId, String startTime, String endTime, String member, String orderNo,
			BigDecimal fromAmount, BigDecimal toAmount);

	// 根据支付宝账号id查询未匹配的流水
	@Query(nativeQuery = true, value = "select id,from_account,status,date_format(trading_time,'%Y-%m-%d %H:%i:%s'),amount,balance,remark,summary,depositor,date_format(create_time,'%Y-%m-%d %H:%i:%s') "
			+ " from biz_alipay_log" + " where from_account=?1 and create_time between ?2 and ?3 and status=0 "
			+ " and (?4 is null or depositor like concat('%',?4,'%')) and (?5=0 or amount>=?5) and (?6=0 or amount<=?6)", countQuery = SqlConstants.findAliBankLogMatch)
	Page<Object> findBankLogMatch(int AliId, String startTime, String endTime, String payer, BigDecimal fromAmount,
			BigDecimal toAmount, Pageable pageable);

	// 统计
	@Query(nativeQuery = true, value = SqlConstants.totalFindAliBankLogMatch)
	java.lang.Object[] totalFindBankLogMatch(int AliId, String startTime, String endTime, String payer,
			BigDecimal fromAmount, BigDecimal toAmount);

	// 修改支付宝入款单状态
	@Modifying
	@Query(nativeQuery = true, value = "update biz_alipay_request set status=?4,alipay_log_id=?2,remark=?3,update_time=now() where id=?1")
	void updateAliSysByid(int sysRequestId, int bankFlowId, String matchRemark, int status);

	// 修改支付宝入款流水状态
	@Modifying
	@Query(nativeQuery = true, value = "update biz_alipay_log set status=1,alipay_request_id=?1 where id=?2")
	void updateAliBankByid(int sysRequestId, int bankFlowId);

	// 查询支付宝入款单
	@Query(nativeQuery = true, value = "select id,alipayid,level,handicap,status,amount,create_time,remark,order_no,operator,member_name,member_id "
			+ " from biz_alipay_request where alipayid=?1 and amount=?2 and create_time between ?3 and ?4 and status=0 order by create_time desc")
	List<Object[]> findAliRequest(int AliId, BigDecimal amount, String fromTime, String endTime);

	// 根据id查询入款信息
	@Query(nativeQuery = true, value = "select id,alipayid,level,handicap,status,amount,create_time,remark,order_no,operator,member_name,member_id  "
			+ " from biz_alipay_request where id=?1")
	List<Object[]> getAliRequestByid(Long id);

	// 修改备注
	@Modifying
	@Query(nativeQuery = true, value = "update biz_alipay_request set remark=?2 where id=?1")
	void updateRemarkById(Long id, String matchRemark);

	// 根据id查询支付宝流水
	@Query(nativeQuery = true, value = "select id,from_account,status,trading_time,amount,balance,remark,summary,depositor,create_time from biz_alipay_log"
			+ " where id=?1")
	List<Object[]> getAliLogByid(long id);

	// 修改备注
	@Modifying
	@Query(nativeQuery = true, value = "update biz_alipay_log set remark=?2 where id=?1")
	void updateAliPayLogRemarkById(Long id, String matchRemark);

	// 修改提单时间
	@Modifying
	@Query(nativeQuery = true, value = "update biz_alipay_request set create_time=?2 where id=?1")
	void updateCreateTimeById(int id, Date createTime);

	/**
	 * 获取正在匹配的支付宝入款单
	 */
	@Query(nativeQuery = true, value = "select\r\n" + "        ir.id,\r\n" + "        h.name AS handicapName,\r\n"
			+ "        l.name AS levelName,\r\n" + "        ir.member_user_name,\r\n" + "        ir.order_no,\r\n"
			+ "        date_format(ir.create_time,'%Y-%m-%d %H:%i:%s'),\r\n" + "        ir.amount" + "    FROM\r\n"
			+ "        fundsTransfer.biz_income_request ir,\r\n" + "        fundsTransfer.biz_handicap h,\r\n"
			+ "        fundsTransfer.biz_level l  \r\n" + "    WHERE\r\n" + "        1 = 1  \r\n"
			+ "        AND ir.handicap = h.id  \r\n" + "        AND ir.level = l.id AND ir. type = 1  AND ir.STATUS = 0  \r\n"
			+ "        and (?1 is null or ir.handicap =?1)" + "      and (?2 is null or ?2 = 0 or ir.level =?2)"
			+ "      and (?3 ='' or ir.member_user_name like concat('%',?3,'%'))"
			+ "      and (?4 ='' or ir.order_no like concat('%',?4,'%')) "
			+ "      and ir.create_time between ?5 and ?6 \r\n", countQuery = SqlConstants.aliIncomeToMatch)
	Page<Object> aliIncomeToMatch(Integer handicap, Integer incomeLevel, String incomeMember, String incomeOrder,
			String timeStart, String timeEnd, Pageable pageable);

	// 统计 获取正在匹配的支付宝入款单 总金额
	@Query(nativeQuery = true, value = SqlConstants.totalAmountAliIncomeToMatch)
	java.lang.Object[] totalAmountAliIncomeToMatch(Integer handicap, Integer incomeLevel, String incomeMember,
			String incomeOrder, String timeStart, String timeEnd);

	/**
	 * 获取失败的支付宝入款单
	 */
	@Query(nativeQuery = true, value = "select\r\n" + "        ir.id,\r\n" + "        h.name AS handicapName,\r\n"
			+ "        l.name AS levelName,\r\n" + "        ir.member_user_name,\r\n" + "        ir.order_no,\r\n"
			+ "        date_format(ir.create_time,'%Y-%m-%d %H:%i:%s'),\r\n"
			+ "        ir.amount, date_format(ir.update_time,'%Y-%m-%d %H:%i:%s')" + "    FROM\r\n"
			+ "        fundsTransfer.biz_income_request ir,\r\n" + "        fundsTransfer.biz_handicap h,\r\n"
			+ "        fundsTransfer.biz_level l  \r\n" + "    WHERE\r\n" + "        1 = 1  \r\n"
			+ "        AND ir.handicap = h.id  \r\n" + "        AND ir.level = l.id "
			+ "       AND (ir.STATUS = 3 or ir.STATUS = 5) AND ir. type = 1 \r\n" + "        and (?1 is null or ir.handicap =?1)"
			+ "      and (?2 is null or ?2 = 0 or ir.level =?2)"
			+ "      and (?3 ='' or ir.member_user_name like concat('%',?3,'%'))"
			+ "      and (?4 ='' or ir.order_no like concat('%',?4,'%')) "
			+ "      and ir.create_time between ?5 and ?6 \r\n", countQuery = SqlConstants.aliIncomeFail)
	Page<Object> aliIncomeFail(Integer handicap, Integer incomeLevel, String incomeMember, String incomeOrder,
			String timeStart, String timeEnd, Pageable pageable);

	// 统计 获取失败的支付宝入款单 总金额
	@Query(nativeQuery = true, value = SqlConstants.totalAmountAliIncomeFail)
	java.lang.Object[] totalAmountAliIncomeFail(Integer handicap, Integer incomeLevel, String incomeMember,
			String incomeOrder, String timeStart, String timeEnd);

	/**
	 * 获取进行中的支付宝入款单
	 */
	@Query(nativeQuery = true, value = "SELECT * from\r\n" + 
			" (\r\n" + 
			"SELECT\r\n" + 
			"	ir.id,\r\n" + 
			"	h. NAME AS handicapName,\r\n" + 
			"	l. NAME AS levelName,\r\n" + 
			"	ir.member_user_name as member,\r\n" + 
			"	ir.order_no as orderNo,\r\n" + 
			"   ir.create_time as create_time,"+
			"	date_format(\r\n" + 
			"		ir.create_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	) as createTimeStr,\r\n" + 
			" date_format(\r\n" + 
			"		ir.update_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	)as updateTimeStr,\r\n" + 
			"	ir.amount,\r\n" + 
			"  bor.member as toMember,\r\n" + 
			"  bor.order_no as toOrderNo,\r\n" + 
			"  th.name as toHandicapName,\r\n" + 
			"  tl.name as toLevelName,\r\n" + 
			"  bor.amount as toAmount\r\n" + 
			"  \r\n" + 
			"FROM\r\n" + 
			"	fundsTransfer.biz_income_request ir,\r\n" + 
			"  fundsTransfer.biz_chatpay_log cl,\r\n" + 
			"	fundsTransfer.biz_outward_request bor,\r\n" + 
			"	fundsTransfer.biz_handicap h,\r\n" + 
			"	fundsTransfer.biz_level l,\r\n" + 
			"	fundsTransfer.biz_handicap th,\r\n" + 
			"	fundsTransfer.biz_level tl\r\n" + 
			"WHERE\r\n" + 
			"1 = 1\r\n" + 
			"AND ir.order_no = cl.income_order_no\r\n" + 
			"AND cl.outward_order_no = bor.order_no\r\n" + 
			"AND bor.handicap = th.id\r\n" + 
			"AND bor.LEVEL = tl.id \r\n" + 
			"AND ir.handicap = h.id\r\n" + 
			"AND ir.LEVEL = l.id\r\n" + 
			"AND ir. STATUS = 1\r\n" + 
			"AND ir. type = 1 AND cl. status = 0 \r\n" + 
			"and bor.member LIKE concat('%' ,:toMember, '%')\r\n" + 
			"and bor.order_no LIKE concat('%' ,:outOrderNo, '%')\r\n" + 
			"AND ( ir.handicap =:handicap)\r\n" + 
			"AND (:level IS NULL OR :level = 0 OR ir. LEVEL =:level) \r\n" + 
			"AND ir.member_user_name LIKE concat('%' ,:member, '%')\r\n" + 
			"AND ir.order_no LIKE concat('%' ,:inOrderNo, '%')\r\n" + 
			"AND (:toHandicapRadio IS NULL OR :toHandicapRadio = 0 OR cl.outward_type =:toHandicapRadio)\r\n" + 
			"AND ir.create_time >= :timeStart\r\n" + 
			"AND ir.create_time <= :timeEnd\r\n" + 
			"union \r\n" + 
			"\r\n" + 
			"SELECT\r\n" + 
			"	ir.id,\r\n" + 
			"	h. NAME AS handicapName,\r\n" + 
			"	l. NAME AS levelName,\r\n" + 
			"	ir.member_user_name as member,\r\n" + 
			"	ir.order_no as orderNo,\r\n" + 
			"	ir.create_time as create_time,\r\n" + 
			"	date_format(\r\n" + 
			"		ir.create_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	) as createTimeStr,\r\n" + 
			" date_format(\r\n" + 
			"		ir.update_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	)as updateTimeStr,\r\n" + 
			"	ir.amount,\r\n" + 
			"  a.account as toMember,\r\n" + 
			"  bat.code as toOrderNo,\r\n" + 
			"  '返利网' as toHandicapName,\r\n" + 
			"  '返利网' as toLevelName,\r\n" + 
			"  bat.money as toAmount\r\n" + 
			"  \r\n" + 
			"FROM\r\n" + 
			"	fundsTransfer.biz_income_request ir,\r\n" + 
			"  fundsTransfer.biz_chatpay_log cl,\r\n" + 
			"  fundsTransfer.biz_account_translog bat,\r\n" + 
			"  fundsTransfer.biz_account a,\r\n" + 
			"	fundsTransfer.biz_handicap h,\r\n" + 
			"	fundsTransfer.biz_level l\r\n" + 
			"\r\n" + 
			"WHERE\r\n" + 
			"1 = 1\r\n" + 
			"AND ir.order_no = cl.income_order_no\r\n" + 
			"AND cl.outward_order_no = bat.id\r\n" + 
			"AND bat.account_id = a.id\r\n" + 
			"AND ir.handicap = h.id\r\n" + 
			"AND ir.LEVEL = l.id\r\n" + 
			"AND ir. STATUS = 1\r\n" + 
			"AND ir. type = 1 AND cl. status = 0\r\n" + 
			"and a.account LIKE concat('%' ,:toMember, '%')\r\n" + 
			"and bat.code LIKE concat('%' ,:outOrderNo, '%')\r\n" + 
			"AND ( ir.handicap =:handicap)\r\n" + 
			"AND (:level IS NULL OR :level = 0 OR ir. LEVEL =:level) \r\n" + 
			"AND ir.member_user_name LIKE concat('%' ,:member, '%')\r\n" + 
			"AND ir.order_no LIKE concat('%' ,:inOrderNo, '%')\r\n" + 
			"AND (:toHandicapRadio IS NULL OR :toHandicapRadio = 0 OR cl.outward_type =:toHandicapRadio)\r\n" + 
			"AND ir.create_time >= :timeStart\r\n" + 
			"AND ir.create_time <= :timeEnd\r\n" + 
			") orderAll"
			, countQuery = SqlConstants.aliIncomeMatched)
	Page<Object> aliIncomeMatched(@Param("handicap") Integer handicap, @Param("level") Integer level, @Param("member") String member, @Param("toMember") String toMember, @Param("inOrderNo") String inOrderNo,
			 @Param("outOrderNo")String outOrderNo, @Param("toHandicapRadio") Integer toHandicapRadio,@Param("timeStart") String timeStart, @Param("timeEnd") String timeEnd, Pageable pageable);

	// 统计 获取进行中的支付宝入款单 总金额
	@Query(nativeQuery = true, value = SqlConstants.totalAmountAliIncomeMatched)
	java.lang.Object[] totalAmountAliIncomeMatched(@Param("handicap") Integer handicap, @Param("level") Integer level, @Param("member") String member, @Param("toMember") String toMember, @Param("inOrderNo") String inOrderNo,
			 @Param("outOrderNo")String outOrderNo, @Param("toHandicapRadio") Integer toHandicapRadio,@Param("timeStart") String timeStart, @Param("timeEnd") String timeEnd);

	// 统计 获取进行中的支付宝入款单 收款总金额
	@Query(nativeQuery = true, value = SqlConstants.totalToAmountAliIncomeMatched)
	java.lang.Object[] totalToAmountAliIncomeMatched(@Param("handicap") Integer handicap, @Param("level") Integer level, @Param("member") String member, @Param("toMember") String toMember, @Param("inOrderNo") String inOrderNo,
			 @Param("outOrderNo")String outOrderNo, @Param("toHandicapRadio") Integer toHandicapRadio,@Param("timeStart") String timeStart, @Param("timeEnd") String timeEnd);

	/**
	 * 获取成功的支付宝入款单
	 */
	@Query(nativeQuery = true, value = "SELECT * from\r\n" + 
			" (\r\n" + 
			"SELECT\r\n" + 
			"	ir.id,\r\n" + 
			"	h. NAME AS handicapName,\r\n" + 
			"	l. NAME AS levelName,\r\n" + 
			"	ir.member_user_name as member,\r\n" + 
			"	ir.order_no as orderNo,\r\n" + 
			" ir.create_time,\r\n" + 
			"	date_format(\r\n" + 
			"		ir.create_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	) as createTimeStr,\r\n" + 
			" date_format(\r\n" + 
			"		ir.update_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	)as updateTimeStr,\r\n" + 
			"	ir.amount,\r\n" + 
			"  bor.member as toMember,\r\n" + 
			"  bor.order_no as toOrderNo,\r\n" + 
			"  th.name as toHandicapName,\r\n" + 
			"  tl.name as toLevelName,\r\n" + 
			"  bor.amount as toAmount\r\n" + 
			"  \r\n" + 
			"FROM\r\n" + 
			"	fundsTransfer.biz_income_request ir,\r\n" + 
			"  fundsTransfer.biz_chatpay_log cl,\r\n" + 
			"	fundsTransfer.biz_outward_request bor,\r\n" + 
			"	fundsTransfer.biz_handicap h,\r\n" + 
			"	fundsTransfer.biz_level l,\r\n" + 
			"	fundsTransfer.biz_handicap th,\r\n" + 
			"	fundsTransfer.biz_level tl\r\n" + 
			"WHERE\r\n" + 
			"1 = 1\r\n" + 
			"AND ir.order_no = cl.income_order_no\r\n" + 
			"AND cl.outward_order_no = bor.order_no\r\n" + 
			"AND bor.handicap = th.id\r\n" + 
			"AND bor.LEVEL = tl.id \r\n" + 
			"AND ir.handicap = h.id\r\n" + 
			"AND ir.LEVEL = l.id\r\n" + 
			"AND ir. STATUS = 4\r\n" + 
			"AND ir. type = 1 AND cl. status = 1 \r\n" + 
			"and bor.member LIKE concat('%' ,:toMember, '%')\r\n" + 
			"and bor.order_no LIKE concat('%' ,:outOrderNo, '%')\r\n" + 
			"AND ( ir.handicap =:handicap)\r\n" + 
			"AND (:level IS NULL OR :level = 0 OR ir. LEVEL =:level) \r\n" + 
			"AND ir.member_user_name LIKE concat('%' ,:member, '%')\r\n" + 
			"AND ir.order_no LIKE concat('%' ,:inOrderNo, '%')\r\n" + 
			"AND (:toHandicapRadio IS NULL OR :toHandicapRadio = 0 OR cl.outward_type =:toHandicapRadio)\r\n" + 
			"AND ir.create_time >= :timeStart\r\n" + 
			"AND ir.create_time <= :timeEnd\r\n" + 
			"union \r\n" + 
			"\r\n" + 
			"SELECT\r\n" + 
			"	ir.id,\r\n" + 
			"	h. NAME AS handicapName,\r\n" + 
			"	l. NAME AS levelName,\r\n" + 
			"	ir.member_user_name as member,\r\n" + 
			"	ir.order_no as orderNo,\r\n" + 
			"  ir.create_time,\r\n" + 
			"	date_format(\r\n" + 
			"		ir.create_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	) as createTimeStr,\r\n" + 
			" date_format(\r\n" + 
			"		ir.update_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	)as updateTimeStr,\r\n" + 
			"	ir.amount,\r\n" + 
			"  a.account as toMember,\r\n" + 
			"  bat.code as toOrderNo,\r\n" + 
			"  '返利网' as toHandicapName,\r\n" + 
			"  '返利网' as toLevelName,\r\n" + 
			"  bat.money as toAmount\r\n" + 
			"  \r\n" + 
			"FROM\r\n" + 
			"	fundsTransfer.biz_income_request ir,\r\n" + 
			"  fundsTransfer.biz_chatpay_log cl,\r\n" + 
			"  fundsTransfer.biz_account_translog bat,\r\n" + 
			"  fundsTransfer.biz_account a,\r\n" + 
			"	fundsTransfer.biz_handicap h,\r\n" + 
			"	fundsTransfer.biz_level l\r\n" + 
			"\r\n" + 
			"WHERE\r\n" + 
			"1 = 1\r\n" + 
			"AND ir.order_no = cl.income_order_no\r\n" + 
			"AND cl.outward_order_no = bat.id\r\n" + 
			"AND bat.account_id = a.id\r\n" + 
			"AND ir.handicap = h.id\r\n" + 
			"AND ir.LEVEL = l.id\r\n" + 
			"AND ir. STATUS = 4\r\n" + 
			"AND ir. type = 1 AND cl. status = 1 \r\n" + 
			"and a.account LIKE concat('%' ,:toMember, '%')\r\n" + 
			"and bat.code LIKE concat('%' ,:outOrderNo, '%')\r\n" + 
			"AND ( ir.handicap =:handicap)\r\n" + 
			"AND (:level IS NULL OR :level = 0 OR ir. LEVEL =:level) \r\n" + 
			"AND ir.member_user_name LIKE concat('%' ,:member, '%')\r\n" + 
			"AND ir.order_no LIKE concat('%' ,:inOrderNo, '%')\r\n" + 
			"AND (:toHandicapRadio IS NULL OR :toHandicapRadio = 0 OR cl.outward_type =:toHandicapRadio)\r\n" + 
			"AND ir.create_time >= :timeStart\r\n" + 
			"AND ir.create_time <= :timeEnd\r\n" + 
			") orderAll\r\n" 
			, countQuery = SqlConstants.aliIncomeSuccess)
	Page<Object> aliIncomeSuccess(@Param("handicap") Integer handicap, @Param("level") Integer level, @Param("member") String member, @Param("toMember") String toMember, @Param("inOrderNo") String inOrderNo,
			 @Param("outOrderNo")String outOrderNo, @Param("toHandicapRadio") Integer toHandicapRadio,@Param("timeStart") String timeStart, @Param("timeEnd") String timeEnd, Pageable pageable);

	// 统计 获取成功的支付宝入款单 总金额
	@Query(nativeQuery = true, value = SqlConstants.totalAmountAliIncomeSuccess)
	java.lang.Object[] totalAmountAliIncomeSuccess(@Param("handicap") Integer handicap, @Param("level") Integer level, @Param("member") String member, @Param("toMember") String toMember, @Param("inOrderNo") String inOrderNo,
			 @Param("outOrderNo")String outOrderNo, @Param("toHandicapRadio") Integer toHandicapRadio,@Param("timeStart") String timeStart, @Param("timeEnd") String timeEnd);

	// 统计 获取成功的支付宝入款单 收款总金额
	@Query(nativeQuery = true, value = SqlConstants.totalToAmountAliIncomeSuccess)
	java.lang.Object[] totalToAmountAliIncomeSuccess(@Param("handicap") Integer handicap, @Param("level") Integer level, @Param("member") String member, @Param("toMember") String toMember, @Param("inOrderNo") String inOrderNo,
			 @Param("outOrderNo")String outOrderNo, @Param("toHandicapRadio") Integer toHandicapRadio,@Param("timeStart") String timeStart, @Param("timeEnd") String timeEnd);
}