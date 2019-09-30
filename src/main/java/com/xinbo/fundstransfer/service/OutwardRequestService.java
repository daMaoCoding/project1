package com.xinbo.fundstransfer.service;

import com.xinbo.fundstransfer.domain.entity.BizOutwardRequest;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import rx.Observable;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public interface OutwardRequestService {
	List<Object[]> statisticsCompanyExpenditure(List<Integer> handicap, Date startTime, Date endTime);

	BigDecimal sumCompanyExpend(Integer handicap, Integer operator, BigDecimal amountStart, BigDecimal amountEnd,
			List<Integer> outAccountId, String purpose, Date startTime, Date endTime);

	Long countCompanyExpend(Integer handicap, Integer operator, BigDecimal amountStart, BigDecimal amountEnd,
			List<Integer> outAccountId, String purpose, Date startTime, Date endTime);

	List<Object[]> queryCompanyExpend(Pageable pageable, Integer handicap, Integer operator, BigDecimal amountStart,
			BigDecimal amountEnd, List<Integer> outAccountId, String purpose, Date startTime, Date endTime);

	void splitReqAndGenerateTask(BizOutwardRequest o);

	List<BizOutwardRequest> findAllByStatus(Integer status);

	Page<Object[]> quickQuery(String member, String orderNo, String startTime, String endTime, Integer handicap,
			Pageable pageable);

	List<Object[]> quickQueryForOut(String startTime, String endTime, String member, String orderNo,
			List<Integer> handicapList);

	Long quickQueryCountForOut(String member, String orderNo);

	BigDecimal[] quickQuerySumForOut(String member, String orderNo);

	List<BizOutwardRequest> findAll(Specification<BizOutwardRequest> specification);

	BizOutwardRequest get(Long id);

	/**
	 * 根据盘口与订单号查找唯一记录
	 *
	 * @param handicap
	 * @param orderNo
	 * @return
	 */
	BizOutwardRequest findByHandicapAndOrderNo(int handicap, String orderNo);

	BizOutwardRequest save(BizOutwardRequest entity);

	BizOutwardRequest update(BizOutwardRequest entity);

	/** 新增公司用款 */
	int addCompanyExpend(String orderNo, Integer handicap, BigDecimal amount, String remark, Integer reviewer,
			String toAccount, String toAccountOwner, String toAccountBank, String review);

	/** 更新公司用款 */
	int updateForCompanyExpend(Long reqId, String remarks, Integer userId);

	int updateById(Long reqId, Integer userId);

	/** 主管审核通过，根据指定reqId和状态为3(主管处理)且不是1(审核通过)的记录 */
	int updateByIdAndStatus(Long reqId, Integer userId);

	void delete(Long id);

	/**
	 * 获取出款审核 出款审核汇总 总记录
	 */
	Long getOutwardRequestCount(List<Integer> handicapList, Integer levelId, Integer[] status, String member,
			String orderNo, Integer reviwer[], String reviewerType, String startTime, String endTime,
			BigDecimal fmonery, BigDecimal tomoney);

	/**
	 * 获取出款审核 出款审核汇总 无分页查询记录
	 */
	Page<BizOutwardRequest> findOutwardRequestPageNoCount(List<Integer> handicapList, Integer levelId, Integer[] status,
			String member, String orderNo, Integer[] reviwer, String reviewerType, String startTime, String endTime,
			BigDecimal fmonery, BigDecimal tomoney, PageRequest pageRequest);

	/**
	 * 获取出款审核 出款审核汇总 总金额
	 */
	String getOutwardRequestSumAmount(List<Integer> handicapList, Integer levelId, Integer[] status, String member,
			String orderNo, Integer[] reviwer, String reviewerType, String startTime, String endTime,
			BigDecimal fmonery, BigDecimal tomoney);

	/**
	 * 获取出款请求审核,调用者注意：无出款请求审核时，会返回null
	 *
	 * @param userid
	 *            用户表主键ID
	 * @return
	 */
	BizOutwardRequest getApproveTask(int userid);

	/**
	 * 审核通过
	 */
	void approve(BizOutwardRequest o, Integer userid, String remark, String memberCode, String orderNo);

	/**
	 * 更新状态
	 *
	 * @param id
	 * @param status
	 * @param remark
	 * @param memberCode
	 * @param orderNo
	 */
	void reportStatus2Platform(Long id, int status, String remark, String memberCode, String orderNo, SysUser operator);

	/**
	 * 根据出款请求主键id查询 ，出款稽核详细信息
	 *
	 * @param id
	 *            出款请求主键id
	 * @return json
	 */
	String getOutwardDetails(Long id);

	/**
	 * 根据to_account查询出款请求 审核通过的数据
	 *
	 * @param account
	 * @param status
	 * @return
	 */

	List<BizOutwardRequest> findByAccountAndStatusAndAmount(String account, List<Integer> status, Float amount,
			int type);

	/**
	 * 自动审核出款请求，返回审核结果，返回空字符串表示审核通过，其它为不通过信息
	 *
	 * @param entity
	 * @return
	 */
	Observable<String> autoCheckOutwardRequest(BizOutwardRequest entity);

	/**
	 * 查询存在回冲数据的单
	 */
	List<Object> quickBackToRush(String startTime, String endTime);

	BizOutwardRequest findByOrderNo(String orderNo);

	List<BizOutwardRequest> findOrdersForNotify();

	BizOutwardRequest findByCreateTimeAndMember(Date createTime);

	/**
	 * 保存第三方账号出款信息到记录表
	 * 
	 * @param fromId
	 * @param amount
	 * @param req
	 * @param user
	 * @param remark
	 */
	void saveThirdOut(int fromId, BigDecimal amount, BizOutwardRequest req, SysUser user, String remark);

	void saveThirdOutWithFee(int fromId, BigDecimal amount, BigDecimal fee, BizOutwardRequest req, SysUser user,
			String remark);

	/**
	 * 保存第三方账号出现金信息
	 * 
	 * @param fromId
	 * @param toAccount
	 * @param toAccountOwner
	 * @param toAccountBank
	 * @param amount
	 * @param fee
	 * @param remark
	 * @param user
	 */
	void saveThirdCashOut(int fromId, String toAccount, String toAccountOwner, String toAccountBank, String amount,
			String fee, String remark, SysUser user);

	/**
	 * 根据taskid，将task的数据放到redis中
	 *
	 * @param taskId
	 */
	void rpush(long taskId);
}
