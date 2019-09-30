package com.xinbo.fundstransfer.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.pojo.OutwardTaskTotalInputDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.xinbo.fundstransfer.domain.entity.BizOutwardTask;
import com.xinbo.fundstransfer.domain.pojo.PageOutwardTaskCheck;

public interface OutwardTaskService {

	BizOutwardTask findByIdAndStatusIn(Long id);

	/** 根据 任务id集合 查询任务状态 */
	Map<Long, Object[]> findStatusRemarkByTaskId(List<Long> taskId);

	/** 根据订单号 盘口 查询出款任务 */
	BizOutwardTask findByOrderNoAndHandicapCode(String orderNo, String handicapCode);

	/** 第三方出款 处理 */
	void thirdOutwardTaskDeal(BizOutwardTask bizOutwardTask, SysUser operator, Long taskId, Integer userId,
			String remark, Integer fromAccountId, String platPayCode);

	// 手机完成出款总金额
	Double sumPhoneOut(OutwardTaskTotalInputDTO inputDTO, Integer[] fromAccount, Integer[] operatorIds);

	// 手机完成出款总记录
	Long countPhoneOut(OutwardTaskTotalInputDTO inputDTO, Integer[] fromAccount, Integer[] operatorIds);

	// 手机完成出款记录
	List<Object[]> findPhoneOut(OutwardTaskTotalInputDTO inputDTO, Integer[] fromAccount, Integer[] operatorIds);

	List<BizOutwardTask> findByRequestId(Long outwardRequestId);

	List<Map<String, Object>> queryInfoByIdList(List<Long> idList);

	List<?> findListJoinUser();

	BizOutwardTask save(BizOutwardTask entity);

	BizOutwardTask update(BizOutwardTask entity);

	BizOutwardTask findById(Long id);

	/**
	 * 条件查询
	 */
	List<BizOutwardTask> findList(Specification<BizOutwardTask> specification, Sort sort);

	/**
	 * 出款任务 出款任务汇总 页签 完成 失败出款 不带总记录数
	 */
	Page<BizOutwardTask> findOutwardTaskPageNoCount(List<String> handicapCodeList, String level, String orderNo,
			String drawType, String member, Integer[] operators, Integer[] accountId, String startTime, String endTime,
			BigDecimal fromMoney, BigDecimal toMoney, String operatorType, Integer[] status, PageRequest pageRequest,
			String sysLevel);

	/**
	 * 出款任务 出款任务汇总 页签 完成 失败出款 总金额
	 */
	String getOutwardTaskSum(List<String> handicapCodeList, String level, String orderNo, String member,
			String drawType, Integer[] operators, Integer[] accountId, String startTime, String endTime,
			BigDecimal fromMoney, BigDecimal toMoney, String operatorType, Integer[] status, String sysLevel);

	/**
	 * 总记录数
	 */
	Long getOutwardTaskCount(List<String> handicapCodeList, String level, String orderNo, String member,
			String drawType, Integer[] operators, Integer[] accountId, String startTime, String endTime,
			BigDecimal fromMoney, BigDecimal toMoney, String operatorType, Integer[] status, String sysLevel);

	/**
	 * 分页获取
	 */
	PageOutwardTaskCheck findPage4Check(String operator, Long outwardTaskId, Integer fromAccountId, Integer handicapId,
			Integer levelId, Integer status, Date startTime, Date endTime, BigDecimal minAmount, BigDecimal maxAmount,
			String toAccount, Pageable pageable);

	/**
	 * 总计 BigDecimal[0]=>totalAmount,BigDecimal[1]=>totalFee
	 */
	BigDecimal[] findTotal4Check(Long outwardTaskId, Integer fromAccountId, Integer handicapId, Integer levelId,
			Integer status, Date startTime, Date endTime, BigDecimal minAmount, BigDecimal maxAmount, String toAccount);

	/**
	 * 根据银行流水记录查询对应的出款任务
	 */
	BizOutwardTask findOutwardTask(int fromAccountId, Float amount, String toAccount, int type, int cardType,
			int useLike);

	/**
	 * 新增会员到永久出款队列
	 *
	 * @param toAccountNo
	 */
	void manualOutMoney(String toAccountNo);

	/**
	 * 删除会员从永久出款队列
	 *
	 * @param toAccountNo
	 */
	void cancelArtificial(String toAccountNo);

	/**
	 * 检测 会员是否在永久出款队列中
	 *
	 * @param toAccountNo
	 * @return
	 */
	boolean checkManualOut4Member(String toAccountNo);

	BizOutwardTask findReuseTask(int fromAccountId, Float amount, String toAccount, int type, int status);

	void updateStatusById(Long id, int status);

	void transToOther(Long taskId, SysUser operator, Integer transferToOther, String remark);

	List<SysUser> outwardUserList(SysUser operator);
}
