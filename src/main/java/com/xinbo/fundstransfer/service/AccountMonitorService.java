package com.xinbo.fundstransfer.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.pojo.TransRec;

/**
 * 账号监控
 * 
 * @author Eden
 * @since 1.8
 */
public interface AccountMonitorService {

	/**
	 * 分页获取未匹配的流水
	 * <p>
	 * 流水包含：转入，转出流水
	 * </p>
	 * 
	 * @param handicapId
	 *            盘口ID
	 * @param accType
	 *            账号分类
	 * @param statArr
	 *            流水状态
	 * @param bankType
	 *            银行类型
	 * @param aliasLike
	 *            编号 </br>
	 *            inclusive null
	 * @param amtBtw
	 *            汇款金额[amountMin,amountMax]</br>
	 *            amtBtw,amountMin,amountMax inclusive null
	 * @param timeBtw
	 *            交易时间[transTimeStart,transTimeEnd]</br>
	 *            timeBtw,transTimeStart,transTimeEnd inclusive null </br>
	 *            format:yyyy-MM-dd HH:mm:ss
	 * @param transIn0Out1
	 *            0 入账流水 1 出账流水
	 * @param doing0OrDone1
	 *            0 待处理;1:已处理
	 * @param pageable
	 *            分页信息
	 */
	Page<BizBankLog> findMatchingFlowPage4Acc(Integer handicapId, Integer accType, Integer[] statArr, String bankType,
			String aliasLike, BigDecimal[] amtBtw, String[] timeBtw, Integer transIn0Out1, int doing0OrDone1,
			Pageable pageable);

	/**
	 * 根据流水获取操作记录
	 * <p>
	 * 一周之内
	 * </p>
	 * 
	 * @param flowId
	 *            流水ID
	 * @return 操作记录集
	 */
	List<TransRec> findRecList4Acc(long flowId);

	/**
	 * 根据收款流水，获取汇款流水
	 * 
	 * @param toFlowId
	 *            收款流水ID
	 * @return 汇款流水
	 */
	List<BizBankLog> findFrFlowList4ToFlow(long toFlowId);

	List<BizBankLog> findToFlowList4FrFlow(long frFlowId);

	/**
	 * 修改流水为回冲待审
	 * 
	 * @param flowId
	 *            银行流水ID
	 * @param operator
	 *            操作人
	 * @param remark
	 *            备注
	 * @throws Exception
	 *             流水不存在 或 已处理
	 */
	void alterFlowToRefunding(long flowId, SysUser operator, String remark) throws Exception;

	/**
	 * 修改流水为费用流水
	 *
	 * @param flowId
	 *            银行流水ID
	 * @param operator
	 *            操作人
	 * @param remark
	 *            备注
	 * @throws Exception
	 *             流水不存在 或 已处理 或 金额超出50元时
	 */
	void alterFlowToFee(long flowId, SysUser operator, String remark) throws Exception;

	/**
	 * 修改流水为已匹配
	 * 
	 * @param flowId
	 *            流水ID
	 * @param recId
	 *            操作记录ID
	 * @param operator
	 *            操作人
	 * @param remark
	 *            备注
	 * @throws Exception
	 *             流水不存在；操作者不存在;流水已处理;下发记录不存在;下发提单金额与流水金额不符
	 */
	void alterFlowToMatched(long flowId, long recId, SysUser operator, String remark) throws Exception;

	/**
	 * 修改流水为利息/结息
	 * <p>
	 * 修改流水状态;</br>
	 * 对应账号: 系统余额=系统余额+额外收入</br>
	 * </p>
	 * 
	 * @param flowId
	 *            银行流水ID
	 * @param operator
	 *            操作人
	 * @param remark
	 *            备注
	 * @throws Exception
	 *             流水不存在；该流水已处理；金额为负；金额大于50元
	 */
	void alterFlowToInterest(long flowId, SysUser operator, String remark) throws Exception;

	/**
	 * 修改流水为亏损
	 *
	 * @param flowId
	 *            银行流水ID
	 * @param reasonCode
	 *            原因编码
	 * @param operator
	 *            操作人
	 * @param remark
	 *            备注
	 * @throws Exception
	 *             流水不存在 或 已处理 或 金额为正
	 */
	void alterFlowToDeficit(long flowId, int reasonCode, SysUser operator, String remark) throws Exception;

	/**
	 * 把该流水标记为外部资金
	 *
	 * @param flowId
	 *            汇款流水
	 * @param operator
	 *            操作者
	 * @param remark
	 *            备注
	 * @throws Exception
	 *             流水不存在</br>
	 *             流水已处理</br>
	 *             金额为负</br>
	 */
	void alterFlowToExtFunds(long flowId, SysUser operator, String remark) throws Exception;

	void alterFlowToDisposed(long flowId, SysUser operator, String remark) throws Exception;

	Integer alterFlowToInvalid(long flowId, SysUser operator);

	/**
	 * 补下发提单
	 * 
	 * @param fromFlowId
	 *            汇款流水
	 * @param toFlowId
	 *            收款流水
	 * @param operator
	 *            操作者
	 * @param remark
	 *            备注
	 * @throws Exception
	 *             流水不存在；</br>
	 *             账号不存在；</br>
	 *             汇款账号类型不正确；</br>
	 *             收款账号类型不正确;</br>
	 *             汇款流水金额不能为正;</br>
	 *             收款流水金额不能为负;</br>
	 *             汇款金额与收款金额必须要不相等；</br>
	 *             收款账号与流水不匹配</br>
	 *             汇款流水已处理</br>
	 *             收款流水已处理</br>
	 */
	void makeUpRec4Issue(long fromFlowId, long toFlowId, SysUser operator, String remark) throws Exception;

	/**
	 * 银行流水添加备注
	 * 
	 * @param flowId
	 *            流水ID
	 * @param operator
	 *            开户人
	 * @param remark
	 *            备注
	 * @throws Exception
	 *             流水不存在</br>
	 *             操着者不存在</br>
	 *             备注不能未空</br>
	 */
	void remark4Flow(long flowId, SysUser operator, String remark) throws Exception;

	/**
	 *
	 * @param status
	 * @param level
	 * @param accType
	 * @param bankType
	 * @param pageRequest
	 * @return
	 * @throws Exception
	 */
	Map<String, Object> listBizIncomeRequest(Integer[] status, Integer[] level, Integer accType, String bankType,
			String startTime, String endTime, SysUser sysUser, PageRequest pageRequest, String accRef, Integer[] fromType) throws Exception;
}
