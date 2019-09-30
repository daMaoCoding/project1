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
import com.xinbo.fundstransfer.domain.entity.BizWechatLog;

public interface IncomeAuditWechatTotalInRepository
		extends BaseRepository<BizWechatLog, Long> {

	// 根据参数统计入款微信未匹配的流水
	@Query(nativeQuery = true, value = "select A.from_account,count(1),B.handicap_id,B.account from biz_wechat_log A,biz_account B "
			+ " where A.from_account=B.id and"
			+ " A.status=0 and A.create_time between ?3 and ?4 and (?1=0 or B.handicap_id=?1) and (?2=null or B.account like concat('%',?2,'%')) group by from_account"
			, countQuery = SqlConstants.statisticalWechatLog)
	Page<Object> statisticalWechatLog(int handicap, String wechatNumber, String startTime, String endTime,
			Pageable pageable);
	
	//查询微信入款已经匹配的单子
	@Query(nativeQuery = true, value = "select A.id,A.handicap,A.member_name,A.order_no,A.amount,B.depositor,A.wechatid,date_format(A.update_time,'%Y-%m-%d %H:%i:%s'),A.remark,date_format(A.create_time,'%Y-%m-%d %H:%i:%s')"
				+" from biz_wechat_request A,biz_wechat_log B,biz_account C"
				+" where A.update_time between ?2 and ?3 and (?4 is null or A.member_name like concat('%',?4,'%')) and (?5 is null or A.order_no =?5)"
				+" and (?8 is null or B.depositor like concat('%',?8,'%')) and (?9 is null or C.account like concat('%',?9,'%'))"
				+" and (?6=0 or A.amount>=?6) and (?7=0 or A.amount<=?7) and (?1=0 or A.handicap=?1)"
				+" and A.wechat_log_id=B.id and A.wechatid=C.id and A.status=1 and B.status=1"
				, countQuery = SqlConstants.findWechatMatched)
	Page<Object> findWechatMatched(int handicap, String startTime, String endTime,String member, String orderNo, 
			BigDecimal fromAmount, BigDecimal toAmount, String payer,String wechatNumber,Pageable pageable);
    // 统计
	@Query(nativeQuery = true, value = SqlConstants.totalFindWechatMatched)
	java.lang.Object[] totalFindWechatMatched(int handicap, String startTime, String endTime,String member, String orderNo,
			BigDecimal fromAmount, BigDecimal toAmount, String payer,String wechatNumber);
	
	//查询微信入款取消的单
	@Query(nativeQuery = true, value = "select A.id,A.handicap,A.member_name,A.order_no,A.amount,A.wechatid,date_format(A.update_time,'%Y-%m-%d %H:%i:%s'),A.remark"
				+" from biz_wechat_request A "
				+" where A.update_time between ?2 and ?3 and (?4 is null or A.member_name like concat('%',?4,'%')) and (?5 is null or A.order_no =?5)"
				+" and (?6=0 or A.amount>=?6) and (?7=0 or A.amount<=?7) and (?1=0 or A.handicap=?1)"
				+" and A.status=3 "
				, countQuery = SqlConstants.findWechatCanceled)
	Page<Object> findWechatCanceled(int handicap, String startTime, String endTime,String member, String orderNo, 
			BigDecimal fromAmount, BigDecimal toAmount, Pageable pageable);
    // 统计
	@Query(nativeQuery = true, value = SqlConstants.totalFindWechatCanceled)
	java.lang.Object[] totalFindWechatCanceled(int handicap, String startTime, String endTime,String member, String orderNo,
			BigDecimal fromAmount, BigDecimal toAmount);
	
	//查询微信入款取消的单
	@Query(nativeQuery = true, value = "select A.id,A.from_account,A.status,date_format(A.trading_time,'%Y-%m-%d %H:%i:%s')trading_time,A.amount,A.balance,A.remark,A.summary,A.depositor,date_format(A.create_time,'%Y-%m-%d %H:%i:%s')create_time,B.handicap_id "
				+" from biz_wechat_log A,biz_account B "
				+" where (?2 is null or A.create_time between ?2 and ?3)"
				+" and (abs(TIMESTAMPDIFF(hour,now(),A.create_time))) >=24 and (abs(TIMESTAMPDIFF(hour,now(),A.create_time)))<=48 and A.status=0"
				+" and (?1=0 or B.handicap_id=?1) and (?4 is null or A.depositor like concat('%',?4,'%')) and (?5 is null or B.account like concat('%',?5,'%'))"
				+" and (?6=0 or A.amount>=?6) and (?7=0 or A.amount<=?7) and A.from_account=B.id "
				, countQuery = SqlConstants.findWechatUnClaim)
	Page<Object> findWechatUnClaim(int handicap, String startTime, String endTime,String member, String wechatNo, 
			BigDecimal fromAmount, BigDecimal toAmount, Pageable pageable);
    // 统计
	@Query(nativeQuery = true, value = SqlConstants.totalFindWechatUnClaim)
	java.lang.Object[] totalFindWechatUnClaim(int handicap, String startTime, String endTime,String member, String wechatNo,
			BigDecimal fromAmount, BigDecimal toAmount);
	
	//根据微信账号id查询微信入款单信息
	@Query(nativeQuery = true, value = "select id,wechatid,level,handicap,status,amount,date_format(create_time,'%Y-%m-%d %H:%i:%s'),remark,order_no,operator,member_name,member_id "
				+" from biz_wechat_request "
				+" where wechatid=?1 and create_time between ?2 and ?3 and status=0 "
				+" and (?4 is null or member_name like concat('%',?4,'%')) and (?5 is null or order_no =?5)"
				+" and (?6=0 or amount>=?6) and (?7=0 or amount<=?7) "
				, countQuery = SqlConstants.findMBAndInvoice)
	Page<Object> findInvoice(int wechatId, String startTime, String endTime, String member,String orderNo, 
			BigDecimal fromAmount, BigDecimal toAmount,Pageable pageable);
    // 统计
	@Query(nativeQuery = true, value = SqlConstants.totalfindMBAndInvoice)
	java.lang.Object[] totalFindInvoice(int wechatId, String startTime, String endTime, String member,String orderNo, 
			BigDecimal fromAmount, BigDecimal toAmount);
	
	//根据微信账号id查询未匹配的流水
	@Query(nativeQuery = true, value = "select id,from_account,status,date_format(trading_time,'%Y-%m-%d %H:%i:%s'),amount,balance,remark,summary,depositor,date_format(create_time,'%Y-%m-%d %H:%i:%s') "
				+" from biz_wechat_log"
				+" where from_account=?1 and create_time between ?2 and ?3 and status=0 "
				+" and (?4 is null or depositor like concat('%',?4,'%')) and (?5=0 or amount>=?5) and (?6=0 or amount<=?6)"
				, countQuery = SqlConstants.findBankLogMatch)
	Page<Object> findBankLogMatch(int wechatId, String startTime, String endTime, String payer, 
			BigDecimal fromAmount, BigDecimal toAmount,Pageable pageable);
    // 统计
	@Query(nativeQuery = true, value = SqlConstants.totalFindBankLogMatch)
	java.lang.Object[] totalFindBankLogMatch(int wechatId, String startTime, String endTime, String payer, 
			BigDecimal fromAmount, BigDecimal toAmount);
	
	
}