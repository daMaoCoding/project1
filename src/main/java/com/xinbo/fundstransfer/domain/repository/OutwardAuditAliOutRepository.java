package com.xinbo.fundstransfer.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.xinbo.fundstransfer.SqlConstants;
import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import com.xinbo.fundstransfer.domain.entity.BizOutwardRequest;

public interface OutwardAuditAliOutRepository extends BaseRepository<BizOutwardRequest, Long> {

	/**
	 * 获取正在匹配的支付宝出款单
	 */
	@Query(nativeQuery = true, value = "SELECT\r\n" + "	bor.id,\r\n" + "	h. NAME AS handicapName,\r\n"
			+ "	l. NAME AS levelName,\r\n" + "	bor.member,\r\n" + "	bor.order_no,\r\n" + "	date_format(\r\n"
			+ "		bor.create_time,\r\n" + "		'%Y-%m-%d %H:%i:%s'\r\n" + "	),\r\n" + "	bor.amount\r\n"
			+ "FROM\r\n" + "	fundsTransfer.biz_outward_request bor,\r\n" + "	fundsTransfer.biz_handicap h,\r\n"
			+ "	fundsTransfer.biz_level l\r\n" + "WHERE\r\n" + "	1 = 1\r\n" + "AND bor.handicap = h.id\r\n"
			+ "AND bor. level = l.id\r\n" + "AND bor. STATUS = 7\r\n" + "AND bor. type = 1\r\n"
			+ "AND (:handicap IS NULL OR bor.handicap =:handicap)\r\n"
			+ "AND (:level IS NULL OR :level=0 OR bor. LEVEL =:level)\r\n" + "AND (\r\n" + "	:member IS NULL\r\n"
			+ "	OR bor.member LIKE concat('%' ,:member, '%')\r\n" + ")\r\n" + "AND (\r\n" + "	:orderNo IS NULL\r\n"
			+ "	OR bor.order_no LIKE concat('%' ,:orderNo, '%')\r\n" + ")\r\n" + "AND bor.create_time >= :timeStart\r\n"
			+ "AND  bor.create_time <= :timeEnd", countQuery = SqlConstants.aliOutToMatch)
	Page<Object> aliOutToMatch(@Param("handicap") Integer handicap, @Param("level") Integer level,
			@Param("member") String member, @Param("orderNo") String orderNo, @Param("timeStart") String timeStart,
			@Param("timeEnd") String timeEnd, Pageable pageable);

	// 统计 获取正在匹配的支付宝出款单 总金额
	@Query(nativeQuery = true, value = SqlConstants.totalAmountAliOutToMatch)
	java.lang.Object[] totalAmountAliOutToMatch(@Param("handicap") Integer handicap, @Param("level") Integer level,
			@Param("member") String member, @Param("orderNo") String orderNo, @Param("timeStart") String timeStart,
			@Param("timeEnd") String timeEnd);

	/**
	 * 获取失败的支付宝出款单
	 */
	@Query(nativeQuery = true, value = "SELECT\r\n" + "	bor.id,\r\n" + "	h. NAME AS handicapName,\r\n"
			+ "	l. NAME AS levelName,\r\n" + "	bor.member,\r\n" + "	bor.order_no,\r\n" + "	date_format(\r\n"
			+ "		bor.create_time,\r\n" + "		'%Y-%m-%d %H:%i:%s'\r\n" + "	),\r\n" + "	bor.amount,\r\n"
			+ " date_format(\r\n" + "		bor.update_time,\r\n" + "		'%Y-%m-%d %H:%i:%s'\r\n" + "	)\r\n"
			+ "FROM\r\n" + "	fundsTransfer.biz_outward_request bor,\r\n" + "	fundsTransfer.biz_handicap h,\r\n"
			+ "	fundsTransfer.biz_level l\r\n" + "WHERE\r\n" + "	1 = 1\r\n" + "AND bor.handicap = h.id\r\n"
			+ "AND bor. level = l.id\r\n" + "AND bor. STATUS = 10\r\n" + "AND bor. type = 1\r\n"
			+ "AND (:handicap IS NULL OR bor.handicap =:handicap)\r\n" + "AND (:level IS NULL OR :level=0 OR bor. LEVEL =:level)\r\n" + "AND (\r\n"
			+ "	:member IS NULL\r\n" + "	OR bor.member LIKE concat('%' ,:member, '%')\r\n" + ")\r\n" + "AND (\r\n"
			+ "	:orderNo IS NULL\r\n" + "	OR bor.order_no LIKE concat('%' ,:orderNo, '%')\r\n" + ")\r\n"
			+ "AND bor.create_time >= :timeStart\r\n" + "AND bor.create_time <= :timeEnd", countQuery = SqlConstants.aliOutFail)
	Page<Object> aliOutFail(@Param("handicap") Integer handicap, @Param("level") Integer level,
			@Param("member") String member, @Param("orderNo") String orderNo, @Param("timeStart") String timeStart,
			@Param("timeEnd") String timeEnd, Pageable pageable);

	// 统计 获取失败的支付宝出款单 总金额
	@Query(nativeQuery = true, value = SqlConstants.totalAmountAliOutFail)
	java.lang.Object[] totalAmountAliOutFail(@Param("handicap") Integer handicap, @Param("level") Integer level,
			@Param("member") String member, @Param("orderNo") String orderNo, @Param("timeStart") String timeStart,
			@Param("timeEnd") String timeEnd);

	/**
	 * 获取进行中的支付宝出款单
	 */
	@Query(nativeQuery = true, value = "SELECT id,handicapName,levelName,member,orderNo,create_time,createTimeStr,updateTimeStr,amount,GROUP_CONCAT(toMember) as toMember \r\n" + 
			",GROUP_CONCAT(toOrderNo)as toOrderNo ,GROUP_CONCAT(toHandicapName)as toHandicapName ,GROUP_CONCAT(toLevelName)as toLevelName \r\n" + 
			",GROUP_CONCAT(toAmount)as toAmount ,sum(toAmount)as toAmountSum \r\n" + 
			"from\r\n" + 
			" (\r\n" + 
			"SELECT\r\n" + 
			"	bor.id,\r\n" + 
			"	h. NAME AS handicapName,\r\n" + 
			"	l. NAME AS levelName,\r\n" + 
			"	bor.member as member,\r\n" + 
			"	bor.order_no as orderNo,\r\n" + 
			" bor.create_time,\r\n" + 
			"	date_format(\r\n" + 
			"		bor.create_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	) as createTimeStr,\r\n" + 
			" date_format(\r\n" + 
			"		bor.update_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	)as updateTimeStr,\r\n" + 
			"	bor.amount,\r\n" + 
			"  ir.member_user_name as toMember,\r\n" + 
			"  ir.order_no as toOrderNo,\r\n" + 
			"  th.name as toHandicapName,\r\n" + 
			"  tl.name as toLevelName,\r\n" + 
			"  ir.amount as toAmount\r\n" + 
			"  \r\n" + 
			"FROM\r\n" + 
			"  fundsTransfer.biz_outward_request bor,\r\n" + 
			"  fundsTransfer.biz_chatpay_log cl,\r\n" + 
			"	fundsTransfer.biz_income_request ir,\r\n" + 
			"	fundsTransfer.biz_handicap h,\r\n" + 
			"	fundsTransfer.biz_level l,\r\n" + 
			"	fundsTransfer.biz_handicap th,\r\n" + 
			"	fundsTransfer.biz_level tl\r\n" + 
			"WHERE\r\n" + 
			"1 = 1\r\n" + 
			"AND bor.order_no = cl.outward_order_no\r\n" + 
			"AND cl.income_order_no = ir.order_no\r\n" + 
			"AND ir.handicap = th.id\r\n" + 
			"AND ir.LEVEL = tl.id \r\n" + 
			"AND bor.handicap = h.id\r\n" + 
			"AND bor.LEVEL = l.id\r\n" + 
			"AND bor. STATUS = 8\r\n" + 
			"AND bor. type = 1\r\n" + 
			"and bor.member LIKE concat('%' ,:member, '%')\r\n" + 
			"and bor.order_no LIKE concat('%' ,:outOrderNo, '%')\r\n" + 
			"AND (:handicap IS NULL OR bor.handicap =:handicap)\r\n" + 
			"AND (:level IS NULL OR :level = 0 OR bor. LEVEL =:level) \r\n" + 
			"AND ir.member_user_name LIKE concat('%' ,:toMember, '%')\r\n" + 
			"AND ir.order_no LIKE concat('%' ,:inOrderNo, '%')\r\n" + 
			"AND (:toHandicapRadio IS NULL OR :toHandicapRadio = 0 OR cl.income_type =:toHandicapRadio)\r\n" + 
			"AND bor.create_time >= :timeStart\r\n" + 
			"AND bor.create_time <= :timeEnd\r\n" + 
			"\r\n" + 
			"union \r\n" + 
			"\r\n" + 
			"SELECT\r\n" + 
			"	bor.id,\r\n" + 
			"	h. NAME AS handicapName,\r\n" + 
			"	l. NAME AS levelName,\r\n" + 
			"	bor.member as member,\r\n" + 
			"	bor.order_no as orderNo,\r\n" + 
			"  bor.create_time,\r\n" + 
			"	date_format(\r\n" + 
			"		bor.create_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	) as createTimeStr,\r\n" + 
			" date_format(\r\n" + 
			"		bor.update_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	)as updateTimeStr,\r\n" + 
			"	bor.amount,\r\n" + 
			"  a.account as toMember,\r\n" + 
			"  bat.code as toOrderNo,\r\n" + 
			"  '返利网' as toHandicapName,\r\n" + 
			"  '返利网' as toLevelName,\r\n" + 
			"  bat.money as toAmount\r\n" + 
			"  \r\n" + 
			"FROM\r\n" + 
			"  fundsTransfer.biz_outward_request bor,\r\n" + 
			"  fundsTransfer.biz_chatpay_log cl,\r\n" + 
			"  fundsTransfer.biz_account_translog bat,\r\n" + 
			"  fundsTransfer.biz_account a,\r\n" + 
			"	fundsTransfer.biz_handicap h,\r\n" + 
			"	fundsTransfer.biz_level l\r\n" + 
			"\r\n" + 
			"WHERE\r\n" + 
			"1 = 1\r\n" + 
			"AND bor.order_no = cl.outward_order_no\r\n" + 
			"AND cl.income_order_no = bat.id\r\n" + 
			"AND bat.account_id = a.id\r\n" + 
			"AND bor.handicap = h.id\r\n" + 
			"AND bor.LEVEL = l.id\r\n" + 
			"AND bor. STATUS = 8\r\n" + 
			"AND bor. type = 1\r\n" + 
			"and a.account LIKE concat('%' ,:toMember, '%')\r\n" + 
			"and bat.code LIKE concat('%' ,:inOrderNo, '%')\r\n" + 
			"AND ( :handicap IS NULL OR bor.handicap =:handicap)\r\n" + 
			"AND (:level IS NULL OR :level = 0 OR bor. LEVEL =:level) \r\n" + 
			"AND bor.member LIKE concat('%' ,:member, '%')\r\n" + 
			"AND bor.order_no LIKE concat('%' ,:outOrderNo, '%')\r\n" + 
			"AND (:toHandicapRadio IS NULL OR :toHandicapRadio = 0 OR cl.income_type =:toHandicapRadio)\r\n" + 
			"AND bor.create_time >= :timeStart\r\n" + 
			"AND bor.create_time <= :timeEnd\r\n" + 
			"\r\n" + 
			") orderAll GROUP BY id,handicapName,levelName,member,orderNo,create_time,createTimeStr,updateTimeStr,amount\r\n" + 
			"\r\n" + 
			"\r\n" + 
			"\r\n" + 
			"", countQuery = SqlConstants.aliOutMatched)
	Page<Object> aliOutMatched(@Param("handicap") Integer handicap, @Param("level") Integer level,
			@Param("member") String member, @Param("toMember") String toMember, @Param("inOrderNo") String inOrderNo,
			@Param("outOrderNo") String outOrderNo, @Param("toHandicapRadio") Integer toHandicapRadio,
			@Param("timeStart") String timeStart, @Param("timeEnd") String timeEnd, Pageable pageable);

	// 统计 获取进行中的支付宝出款单 总金额
	@Query(nativeQuery = true, value = SqlConstants.totalAmountAliOutMatched)
	java.lang.Object[] totalAmountAliOutMatched(@Param("handicap") Integer handicap, @Param("level") Integer level,
			@Param("member") String member, @Param("toMember") String toMember, @Param("inOrderNo") String inOrderNo,
			@Param("outOrderNo") String outOrderNo, @Param("toHandicapRadio") Integer toHandicapRadio,
			@Param("timeStart") String timeStart, @Param("timeEnd") String timeEnd);

	// 统计 获取进行中的支付宝出款单 付款总金额
	@Query(nativeQuery = true, value = SqlConstants.totalToAmountAliOutMatched)
	java.lang.Object[] totalToAmountAliOutMatched(@Param("handicap") Integer handicap, @Param("level") Integer level,
			@Param("member") String member, @Param("toMember") String toMember, @Param("inOrderNo") String inOrderNo,
			@Param("outOrderNo") String outOrderNo, @Param("toHandicapRadio") Integer toHandicapRadio,
			@Param("timeStart") String timeStart, @Param("timeEnd") String timeEnd);

	/**
	 * 获取成功的支付宝出款单
	 */
	@Query(nativeQuery = true, value = "SELECT id,handicapName,levelName,member,orderNo,create_time,createTimeStr,updateTimeStr,amount,GROUP_CONCAT(toMember) as toMember \r\n" + 
			",GROUP_CONCAT(toOrderNo)as toOrderNo ,GROUP_CONCAT(toHandicapName)as toHandicapName ,GROUP_CONCAT(toLevelName)as toLevelName \r\n" + 
			",GROUP_CONCAT(toAmount)as toAmount ,sum(toAmount)as toAmountSum \r\n" + 
			"from\r\n" + 
			" (\r\n" + 
			"SELECT\r\n" + 
			"	bor.id,\r\n" + 
			"	h. NAME AS handicapName,\r\n" + 
			"	l. NAME AS levelName,\r\n" + 
			"	bor.member as member,\r\n" + 
			"	bor.order_no as orderNo,\r\n" + 
			" bor.create_time,\r\n" + 
			"	date_format(\r\n" + 
			"		bor.create_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	) as createTimeStr,\r\n" + 
			" date_format(\r\n" + 
			"		bor.update_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	)as updateTimeStr,\r\n" + 
			"	bor.amount,\r\n" + 
			"  ir.member_user_name as toMember,\r\n" + 
			"  ir.order_no as toOrderNo,\r\n" + 
			"  th.name as toHandicapName,\r\n" + 
			"  tl.name as toLevelName,\r\n" + 
			"  ir.amount as toAmount\r\n" + 
			"  \r\n" + 
			"FROM\r\n" + 
			"  fundsTransfer.biz_outward_request bor,\r\n" + 
			"  fundsTransfer.biz_chatpay_log cl,\r\n" + 
			"	fundsTransfer.biz_income_request ir,\r\n" + 
			"	fundsTransfer.biz_handicap h,\r\n" + 
			"	fundsTransfer.biz_level l,\r\n" + 
			"	fundsTransfer.biz_handicap th,\r\n" + 
			"	fundsTransfer.biz_level tl\r\n" + 
			"WHERE\r\n" + 
			"1 = 1\r\n" + 
			"AND bor.order_no = cl.outward_order_no\r\n" + 
			"AND cl.income_order_no = ir.order_no\r\n" + 
			"AND ir.handicap = th.id\r\n" + 
			"AND ir.LEVEL = tl.id \r\n" + 
			"AND bor.handicap = h.id\r\n" + 
			"AND bor.LEVEL = l.id\r\n" + 
			"AND bor. STATUS = 9\r\n" + 
			"AND bor. type = 1\r\n" + 
			"and bor.member LIKE concat('%' ,:member, '%')\r\n" + 
			"and bor.order_no LIKE concat('%' ,:outOrderNo, '%')\r\n" + 
			"AND (:handicap IS NULL OR bor.handicap =:handicap)\r\n" + 
			"AND (:level IS NULL OR :level = 0 OR bor. LEVEL =:level) \r\n" + 
			"AND ir.member_user_name LIKE concat('%' ,:toMember, '%')\r\n" + 
			"AND ir.order_no LIKE concat('%' ,:inOrderNo, '%')\r\n" + 
			"AND (:toHandicapRadio IS NULL OR :toHandicapRadio = 0 OR cl.income_type =:toHandicapRadio)\r\n" + 
			"AND bor.create_time >= :timeStart\r\n" + 
			"AND bor.create_time <= :timeEnd\r\n" + 
			"\r\n" + 
			"union \r\n" + 
			"\r\n" + 
			"SELECT\r\n" + 
			"	bor.id,\r\n" + 
			"	h. NAME AS handicapName,\r\n" + 
			"	l. NAME AS levelName,\r\n" + 
			"	bor.member as member,\r\n" + 
			"	bor.order_no as orderNo,\r\n" + 
			"  bor.create_time,\r\n" + 
			"	date_format(\r\n" + 
			"		bor.create_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	) as createTimeStr,\r\n" + 
			" date_format(\r\n" + 
			"		bor.update_time,\r\n" + 
			"		'%Y-%m-%d %H:%i:%s'\r\n" + 
			"	)as updateTimeStr,\r\n" + 
			"	bor.amount,\r\n" + 
			"  a.account as toMember,\r\n" + 
			"  bat.code as toOrderNo,\r\n" + 
			"  '返利网' as toHandicapName,\r\n" + 
			"  '返利网' as toLevelName,\r\n" + 
			"  bat.money as toAmount\r\n" + 
			"  \r\n" + 
			"FROM\r\n" + 
			"  fundsTransfer.biz_outward_request bor,\r\n" + 
			"  fundsTransfer.biz_chatpay_log cl,\r\n" + 
			"  fundsTransfer.biz_account_translog bat,\r\n" + 
			"  fundsTransfer.biz_account a,\r\n" + 
			"	fundsTransfer.biz_handicap h,\r\n" + 
			"	fundsTransfer.biz_level l\r\n" + 
			"\r\n" + 
			"WHERE\r\n" + 
			"1 = 1\r\n" + 
			"AND bor.order_no = cl.outward_order_no\r\n" + 
			"AND cl.income_order_no = bat.id\r\n" + 
			"AND bat.account_id = a.id\r\n" + 
			"AND bor.handicap = h.id\r\n" + 
			"AND bor.LEVEL = l.id\r\n" + 
			"AND bor. STATUS = 9\r\n" + 
			"AND bor. type = 1\r\n" + 
			"and a.account LIKE concat('%' ,:toMember, '%')\r\n" + 
			"and bat.code LIKE concat('%' ,:inOrderNo, '%')\r\n" + 
			"AND ( :handicap IS NULL OR bor.handicap =:handicap)\r\n" + 
			"AND (:level IS NULL OR :level = 0 OR bor. LEVEL =:level) \r\n" + 
			"AND bor.member LIKE concat('%' ,:member, '%')\r\n" + 
			"AND bor.order_no LIKE concat('%' ,:outOrderNo, '%')\r\n" + 
			"AND (:toHandicapRadio IS NULL OR :toHandicapRadio = 0 OR cl.income_type =:toHandicapRadio)\r\n" + 
			"AND bor.create_time >= :timeStart\r\n" + 
			"AND bor.create_time <= :timeEnd\r\n" + 
			"\r\n" + 
			") orderAll GROUP BY id,handicapName,levelName,member,orderNo,create_time,createTimeStr,updateTimeStr,amount\r\n" + 
			"\r\n" + 
			"\r\n" + 
			"\r\n" + 
			"", countQuery = SqlConstants.aliOutSuccess)
	Page<Object> aliOutSuccess(@Param("handicap") Integer handicap, @Param("level") Integer level,
			@Param("member") String member, @Param("toMember") String toMember, @Param("inOrderNo") String inOrderNo,
			@Param("outOrderNo") String outOrderNo, @Param("toHandicapRadio") Integer toHandicapRadio,
			@Param("timeStart") String timeStart, @Param("timeEnd") String timeEnd, Pageable pageable);

	// 统计 获取成功的支付宝出款单 总金额
	@Query(nativeQuery = true, value = SqlConstants.totalAmountAliOutSuccess)
	java.lang.Object[] totalAmountAliOutSuccess(@Param("handicap") Integer handicap, @Param("level") Integer level,
			@Param("member") String member, @Param("toMember") String toMember, @Param("inOrderNo") String inOrderNo,
			@Param("outOrderNo") String outOrderNo, @Param("toHandicapRadio") Integer toHandicapRadio,
			@Param("timeStart") String timeStart, @Param("timeEnd") String timeEnd);

	// 统计 获取成功的支付宝出款单 付款总金额
	@Query(nativeQuery = true, value = SqlConstants.totalToAmountAliOutSuccess)
	java.lang.Object[] totalToAmountAliOutSuccess(@Param("handicap") Integer handicap, @Param("level") Integer level,
			@Param("member") String member, @Param("toMember") String toMember, @Param("inOrderNo") String inOrderNo,
			@Param("outOrderNo") String outOrderNo, @Param("toHandicapRadio") Integer toHandicapRadio,
			@Param("timeStart") String timeStart, @Param("timeEnd") String timeEnd);
}