package com.xinbo.fundstransfer.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import com.xinbo.fundstransfer.domain.entity.BizWechatLog;

public interface IncomeAuditWechatInService {

	/**
	 * 根据参数统计入款微信未匹配的流水
	 * 
	 * @param handicap
	 * @param wechatNumber
	 * @param type
	 * @param startTime
	 * @param endTime
	 * @param pageRequest
	 * @return
	 * @throws Exception
	 */
	Object statisticalWechatLog(String pageNo, List<String> handicapList, String wechatNumber, String startTime,
			String endTime, String pageSize) throws Exception;

	/**
	 * 根据微信号查询流水和入款单信息
	 * 
	 * @param wechatId
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
	Object findMBAndInvoice(String account, String startTime, String endTime, String member, String orderNo,
			BigDecimal fromAmount, BigDecimal toAmount, String payer, String invoicePageNo, String logPageNo,
			String PageSize) throws Exception;

	/**
	 * 查询微信入款已经匹配的数据
	 * 
	 * @param handicap
	 * @param startTime
	 * @param endTime
	 * @param member
	 * @param orderNo
	 * @param fromAmount
	 * @param toAmount
	 * @param payer
	 * @param wechatNumber
	 * @param type
	 * @param pageRequest
	 * @return
	 * @throws Exception
	 */
	Object findWechatMatched(int pageNo, List<String> handicapList, String startTime, String endTime, String member,
			String orderNo, BigDecimal fromAmount, BigDecimal toAmount, String wechatNumber, int status,
			String pageSise) throws Exception;

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
	Object findWechatCanceled(int pageNo, List<String> handicapList, String startTime, String endTime, String member,
			String orderNo, BigDecimal fromAmount, BigDecimal toAmount, String pageSise) throws Exception;

	/**
	 * 查询未认领的流水
	 * 
	 * @param handicap
	 * @param startTime
	 * @param endTime
	 * @param member
	 * @param wechatNo
	 * @param fromAmount
	 * @param toAmount
	 * @param pageRequest
	 * @return
	 * @throws Exception
	 */
	Object findWechatUnClaim(int pageNo, List<String> handicapList, String startTime, String endTime, String wechatNo,
			BigDecimal fromAmount, BigDecimal toAmount, String pageSise) throws Exception;

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
	List<Object[]> findRepeatWechatLog(int fromAccount, BigDecimal amount, BigDecimal balance, String tradingTime)
			throws Exception;

	/**
	 * 保存流水
	 * 
	 * @return
	 * 
	 * @throws Exception
	 */
	void saveWechatLog(BizWechatLog wechatLog) throws Exception;

	/**
	 * 微信入款手动匹配操作
	 * 
	 * @param sysRequestId
	 * @param bankFlowId
	 * @param matchRemark
	 * @throws Exception
	 */
	void wechatInMatch(int sysRequestId, int bankFlowId, String matchRemark) throws Exception;

	/**
	 * 查找订单
	 * 
	 * @param wechatId
	 * @param amount
	 * @param tradingTime
	 * @return
	 * @throws Exception
	 */
	List<Object[]> findWechatRequest(int wechatId, BigDecimal amount, String tradingTime, Integer validIntervalTimeHour)
			throws Exception;

	BizWechatLog save(BizWechatLog entity);

	/**
	 * 根据id查询微信入款订单
	 * 
	 * @param wecahtId
	 * @return
	 */
	List<Object[]> getWechatRequestByid(Long id);

	/**
	 * 根据id查询微信流水
	 * 
	 * @param wecahtId
	 * @return
	 */
	List<Object[]> getWechatLogByid(Long id);

	Object updateRemarkById(long id, String remark, String type, String userName) throws Exception;

	Object updateWecahtLogRemarkById(long id, String remark, String type, String userName) throws Exception;

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
	Object wechatAck(int sysRequestId, int bankFlowId, String matchRemark, String userName) throws Exception;

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
	Object generateWecahtRequestOrder(String memberAccount, BigDecimal amount, String account, String remark,
			String createTime, int bankLogId, String handicap, String userName) throws Exception;
}
