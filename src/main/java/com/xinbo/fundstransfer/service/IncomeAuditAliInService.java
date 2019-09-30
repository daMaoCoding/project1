package com.xinbo.fundstransfer.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.xinbo.fundstransfer.domain.entity.BizAliLog;

public interface IncomeAuditAliInService {

	/**
	 * 根据参数统计入款支付宝未匹配的流水
	 * 
	 * @param handicap
	 * @param AliNumber
	 * @param type
	 * @param startTime
	 * @param endTime
	 * @param pageRequest
	 * @return
	 * @throws Exception
	 */
	Object statisticalAliLog(int pageNo, List<String> handicapList, String AliNumber, String startTime, String endTime,
			String pageSise) throws Exception;

	/**
	 * 根据支付宝号查询流水和入款单信息
	 * 
	 * @param AliId
	 * @param startTime
	 * @param endTime
	 * @param member
	 * @param orderNo
	 * @param fromAmount
	 * @param toAmount
	 * @param payer
	 * @param invoicePageRequest
	 * @param banklogPageRequest
	 * @return
	 * @throws Exception
	 */
	Object findMBAndInvoice(String pageSise, String account, String startTime, String endTime, String member,
			String orderNo, BigDecimal fromAmount, BigDecimal toAmount, String payer, int invoicePageNo,
			int banklogPageNo) throws Exception;

	/**
	 * 查询支付宝入款已经匹配的数据
	 * 
	 * @param handicap
	 * @param startTime
	 * @param endTime
	 * @param member
	 * @param orderNo
	 * @param fromAmount
	 * @param toAmount
	 * @param payer
	 * @param AliNumber
	 * @param type
	 * @param pageRequest
	 * @return
	 * @throws Exception
	 */
	Object findAliMatched(int pageNo, List<String> handicapList, String startTime, String endTime, String member,
			String orderNo, BigDecimal fromAmount, BigDecimal toAmount, String payer, String AliNumber, int status,
			String pageSize) throws Exception;

	/**
	 * 查询取消的单
	 * 
	 * @param handicap
	 * @param startTime
	 * @param endTime
	 * @param member
	 * @param orderNo
	 * @param fromAmount
	 * @param toAmount
	 * @param pageRequest
	 * @return
	 * @throws Exception
	 */
	Object findAliCanceled(int pageNo, List<String> handicapList, String startTime, String endTime, String member,
			String orderNo, BigDecimal fromAmount, BigDecimal toAmount, String pageSise) throws Exception;

	/**
	 * 查询未认领的流水
	 * 
	 * @param handicap
	 * @param startTime
	 * @param endTime
	 * @param member
	 * @param AliNo
	 * @param fromAmount
	 * @param toAmount
	 * @param pageRequest
	 * @return
	 * @throws Exception
	 */
	Object findAliUnClaim(int pageNo, List<String> handicapList, String startTime, String endTime, String member,
			String AliNo, BigDecimal fromAmount, BigDecimal toAmount, String pageSise) throws Exception;

	/**
	 * 查询是否存在重复的流水
	 * 
	 * @param fromAccount
	 * @param amount
	 * @param balance
	 * @param tradingTime
	 * @return
	 * @throws Exception
	 */
	List<Object[]> findRepeatAliLog(int fromAccount, BigDecimal amount, BigDecimal balance, String tradingTime)
			throws Exception;

	/**
	 * 保存流水
	 * 
	 * @return
	 * 
	 * @throws Exception
	 */
	void saveAliLog(BizAliLog AliLog) throws Exception;

	/**
	 * 支付宝入款手动匹配操作
	 * 
	 * @param sysRequestId
	 * @param bankFlowId
	 * @param matchRemark
	 * @throws Exception
	 */
	Object AliInMatch(int sysRequestId, int bankFlowId, String matchRemark, String userName) throws Exception;

	/**
	 * 查找订单
	 * 
	 * @param AliId
	 * @param amount
	 * @param tradingTime
	 * @return
	 * @throws Exception
	 */
	List<Object[]> findAliRequest(int AliId, BigDecimal amount, String tradingTime, Integer validIntervalTimeHour)
			throws Exception;

	BizAliLog save(BizAliLog entity);

	/**
	 * 根据id查询支付宝入款订单
	 * 
	 * @param wecahtId
	 * @return
	 */
	List<Object[]> getAliRequestByid(Long id);

	/**
	 * 根据id查询支付宝流水
	 * 
	 * @param wecahtId
	 * @return
	 */
	List<Object[]> getAliLogByid(Long id);

	Object updateRemarkById(long id, String remark, String type, String userName) throws Exception;

	Object updateAliPayLogRemarkById(long id, String remark, String type, String userName) throws Exception;

	/**
	 * 向平台确定
	 * 
	 * @param AliLog
	 * @param handicap
	 * @param memberCode
	 * @param orderNo
	 * @param remark
	 * @throws Exception
	 */
	void aLiPayAck(BizAliLog AliLog, int handicap, int memberCode, String orderNo, String remark, String remarkWrap,
			int requestId) throws Exception;

	/** 取消订单并通知平台 */
	Object cancelAndCallFlatform(int incomeRequestId, String handicap, String orderNo, String remark, String userName)
			throws Exception;

	/**
	 * 隐藏时候修改时间
	 * 
	 * @param id
	 * @param time
	 */
	Object updateTimeById(int id, Date time) throws Exception;

	/** 补提单 */
	Object generateAliPayRequestOrder(String memberAccount, BigDecimal amount, String account, String remark,
			String createTime, int bankLogId, String handicap, String userName) throws Exception;

	/**
	 * 获取正在匹配的支付宝入款单
	 * @param pageRequest 分页对象
	 * @param handicap 盘口
	 * @param level 层级
	 * @param incomeMember 入款会员
	 * @param incomeOrder 入款单号
	 * @param timeStart 入款提单时间开始
	 * @param timeEnd 入款提单时间结束
	 * @return
	 */
	Map<String,Object> aliIncomeToMatch(PageRequest pageRequest, Integer handicap, Integer level, String incomeMember,String incomeOrder,String timeStart,String timeEnd);

	/**
	 * 获取失败的支付宝入款单
	 * @param pageRequest 分页对象
	 * @param handicap 盘口
	 * @param level 层级
	 * @param incomeMember 入款会员
	 * @param incomeOrder 入款单号
	 * @param timeStart 入款提单时间开始
	 * @param timeEnd 入款提单时间结束
	 * @return
	 */
	Map<String,Object> aliIncomeFail(PageRequest pageRequest, Integer handicap, Integer level, String incomeMember,String incomeOrder,String timeStart,String timeEnd);
	
	/**
	 * 获取成功的支付宝入款单
	 * @param pageRequest 分页对象
	 * @param handicap 盘口
	 * @param level 层级
	 * @param member 入款会员
	 * @param toMember 收款会员
	 * @param inOrderNo 入款单号
	 * @param outOrderNo 收款单号
	 * @param toHandicapRadio 收款类型：全部0，盘口1，返利网2
	 * @param timeStart 入款提单时间开始
	 * @param timeEnd 入款提单时间结束
	 * @return
	 */
	Map<String,Object> aliIncomeSuccess(Pageable pageable, Integer handicap, Integer level, String member,String toMember,String inOrderNo,String outOrderNo,Integer toHandicapRadio,String timeStart,String timeEnd);
	
	/**
	 * 获取进行的支付宝入款单
	 * @param pageRequest 分页对象
	 * @param handicap 盘口
	 * @param level 层级
	 * @param member 入款会员
	 * @param toMember 收款会员
	 * @param inOrderNo 入款单号
	 * @param outOrderNo 收款单号
	 * @param toHandicapRadio 收款类型：全部0，盘口1，返利网2
	 * @param timeStart 入款提单时间开始
	 * @param timeEnd 入款提单时间结束
	 * @return
	 */
	Map<String,Object> aliIncomeMatched(Pageable pageable, Integer handicap, Integer level, String member,String toMember,String inOrderNo,String outOrderNo,Integer toHandicapRadio,String timeStart,String timeEnd);

}
