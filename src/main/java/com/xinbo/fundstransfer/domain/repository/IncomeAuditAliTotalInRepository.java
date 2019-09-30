package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.xinbo.fundstransfer.SqlConstants;
import com.xinbo.fundstransfer.domain.entity.AccountStatistics;
import com.xinbo.fundstransfer.domain.entity.BizAliLog;

public interface IncomeAuditAliTotalInRepository
		extends BaseRepository<BizAliLog, Long> {

	// 根据参数统计入款支付宝未匹配的流水
	@Query(nativeQuery = true, value = "select A.from_account,count(1),B.handicap_id,B.account from biz_alipay_log A,biz_account B "
			+ " where A.from_account=B.id and"
			+ " A.status=0 and A.create_time between ?3 and ?4 and (?1=0 or B.handicap_id=?1) and (?2=null or B.account like concat('%',?2,'%')) group by from_account"
			, countQuery = SqlConstants.statisticalAliLog)
	Page<Object> statisticalAliLog(int handicap, String AliNumber, String startTime, String endTime,
			Pageable pageable);
	
	//查询支付宝入款已经匹配的单子
	@Query(nativeQuery = true, value = "select A.id,A.handicap,A.member_name,A.order_no,A.amount,B.depositor,A.alipayid,date_format(A.update_time,'%Y-%m-%d %H:%i:%s'),A.remark,date_format(A.create_time,'%Y-%m-%d %H:%i:%s')"
				+" from biz_alipay_request A,biz_alipay_log B,biz_account C"
				+" where A.update_time between ?2 and ?3 and (?4 is null or A.member_name like concat('%',?4,'%')) and (?5 is null or A.order_no =?5)"
				+" and (?8 is null or B.depositor like concat('%',?8,'%')) and (?9 is null or C.account like concat('%',?9,'%'))"
				+" and (?6=0 or A.amount>=?6) and (?7=0 or A.amount<=?7) and (?1=0 or A.handicap=?1)"
				+" and A.alipay_log_id=B.id and A.alipayid=C.id and A.status=1 and B.status=1"
				, countQuery = SqlConstants.findAliMatched)
	Page<Object> findAliMatched(int handicap, String startTime, String endTime,String member, String orderNo, 
			BigDecimal fromAmount, BigDecimal toAmount, String payer,String AliNumber,Pageable pageable);
    // 统计
	@Query(nativeQuery = true, value = SqlConstants.totalFindAliMatched)
	java.lang.Object[] totalFindAliMatched(int handicap, String startTime, String endTime,String member, String orderNo,
			BigDecimal fromAmount, BigDecimal toAmount, String payer,String AliNumber);
	
	//查询支付宝入款取消的单
	@Query(nativeQuery = true, value = "select A.id,A.handicap,A.member_name,A.order_no,A.amount,A.alipayid,date_format(A.update_time,'%Y-%m-%d %H:%i:%s'),A.remark"
				+" from biz_alipay_request A "
				+" where A.update_time between ?2 and ?3 and (?4 is null or A.member_name like concat('%',?4,'%')) and (?5 is null or A.order_no =?5)"
				+" and (?6=0 or A.amount>=?6) and (?7=0 or A.amount<=?7) and (?1=0 or A.handicap=?1)"
				+" and A.status=3 "
				, countQuery = SqlConstants.findAliCanceled)
	Page<Object> findAliCanceled(int handicap, String startTime, String endTime,String member, String orderNo, 
			BigDecimal fromAmount, BigDecimal toAmount, Pageable pageable);
    // 统计
	@Query(nativeQuery = true, value = SqlConstants.totalFindAliCanceled)
	java.lang.Object[] totalFindAliCanceled(int handicap, String startTime, String endTime,String member, String orderNo,
			BigDecimal fromAmount, BigDecimal toAmount);
	
	//查询支付宝未认领
	@Query(nativeQuery = true, value = "select A.id,A.from_account,A.status,date_format(A.trading_time,'%Y-%m-%d %H:%i:%s')trading_time,A.amount,A.balance,A.remark,A.summary,A.depositor,date_format(A.create_time,'%Y-%m-%d %H:%i:%s')create_time,B.handicap_id "
				+" from biz_alipay_log A,biz_account B "
				+" where (?2 is null or A.create_time between ?2 and ?3)"
				+" and (abs(TIMESTAMPDIFF(hour,now(),A.create_time))) >=24 and (abs(TIMESTAMPDIFF(hour,now(),A.create_time)))<=48 and A.status=0"
				+" and (?1=0 or B.handicap_id=?1) and (?4 is null or A.depositor like concat('%',?4,'%')) and (?5 is null or B.account like concat('%',?5,'%'))"
				+" and (?6=0 or A.amount>=?6) and (?7=0 or A.amount<=?7) and A.from_account=B.id "
				, countQuery = SqlConstants.findAliUnClaim)
	Page<Object> findAliUnClaim(int handicap, String startTime, String endTime,String member, String AliNo, 
			BigDecimal fromAmount, BigDecimal toAmount, Pageable pageable);
    // 统计
	@Query(nativeQuery = true, value = SqlConstants.totalFindAliUnClaim)
	java.lang.Object[] totalFindAliUnClaim(int handicap, String startTime, String endTime,String member, String AliNo,
			BigDecimal fromAmount, BigDecimal toAmount);
	
	
	//根据支付宝账号id查询支付宝入款单信息
	@Query(nativeQuery = true, value = "select id,alipayid,level,handicap,status,amount,date_format(create_time,'%Y-%m-%d %H:%i:%s'),remark,order_no,operator,member_name,member_id  "
				+" from biz_alipay_request "
				+" where alipayid=?1 and create_time between ?2 and ?3 and status=0 "
				+" and (?4 is null or member_name like concat('%',?4,'%')) and (?5 is null or order_no =?5)"
				+" and (?6=0 or amount>=?6) and (?7=0 or amount<=?7) "
				, countQuery = SqlConstants.findAliMBAndInvoice)
	Page<Object> findInvoice(int AliId, String startTime, String endTime, String member,String orderNo, 
			BigDecimal fromAmount, BigDecimal toAmount,Pageable pageable);
    // 统计
	@Query(nativeQuery = true, value = SqlConstants.totalfindAliMBAndInvoice)
	java.lang.Object[] totalFindInvoice(int AliId, String startTime, String endTime, String member,String orderNo, 
			BigDecimal fromAmount, BigDecimal toAmount);
	
	//根据支付宝账号id查询未匹配的流水
	@Query(nativeQuery = true, value = "select id,from_account,status,date_format(trading_time,'%Y-%m-%d %H:%i:%s'),amount,balance,remark,summary,depositor,date_format(create_time,'%Y-%m-%d %H:%i:%s') "
				+" from biz_alipay_log"
				+" where from_account=?1 and create_time between ?2 and ?3 and status=0 "
				+" and (?4 is null or depositor like concat('%',?4,'%')) and (?5=0 or amount>=?5) and (?6=0 or amount<=?6)"
				, countQuery = SqlConstants.findAliBankLogMatch)
	Page<Object> findBankLogMatch(int AliId, String startTime, String endTime, String payer, 
			BigDecimal fromAmount, BigDecimal toAmount,Pageable pageable);
    // 统计
	@Query(nativeQuery = true, value = SqlConstants.totalFindAliBankLogMatch)
	java.lang.Object[] totalFindBankLogMatch(int AliId, String startTime, String endTime, String payer, 
			BigDecimal fromAmount, BigDecimal toAmount);
	
}