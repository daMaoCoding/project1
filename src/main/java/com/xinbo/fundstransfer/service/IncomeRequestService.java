package com.xinbo.fundstransfer.service;

import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.ThirdDrawFailStatisticInputDTO;
import com.xinbo.fundstransfer.domain.pojo.ThirdDrawFailStatisticOutputDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface IncomeRequestService {

	List<BizIncomeRequest> saveCollection(List<BizIncomeRequest> list);

	BizIncomeRequest findByOrderNoAndHandicapAndAmount(String orderNo, Integer handicapId, BigDecimal amount);

	/**
	 * 根据id更新 会员realName
	 * 
	 * @param realName
	 * @param id
	 */
	void updateRealName(String realName, Long id);

	/**
	 * 根据用户id获取上一次使用的最新第三方账号id
	 * 
	 * @param userId
	 * @return
	 */
	Integer getThirdIdLastUsedByUserId(Integer userId);

	/**
	 * 保存用户下发最新使用的第三方账号id
	 * 
	 * @param thirdId
	 * @param userId
	 */
	void saveThirdIdUsedLatest(Integer thirdId, Integer userId);

	/**
	 * 根据id 更新 更新时间和耗时
	 * 
	 * @param id
	 * @param updateTime
	 * @param timeConsuming
	 */
	void updateTimeconsumingAndUpdateTime(Long id, Date updateTime, Long timeConsuming);

	List<Integer> findFinishedToIdByUserId(Integer userId);

	BizIncomeRequest saveAndFlush(BizIncomeRequest incomeRequest);

	BizIncomeRequest findOneThirdDrawRecord(Integer toId, Integer amount);

	List<ThirdDrawFailStatisticOutputDTO> findThirdDrawFailDetail(ThirdDrawFailStatisticInputDTO inputDTO);

	List<ThirdDrawFailStatisticOutputDTO> findThirdDrawFail(ThirdDrawFailStatisticInputDTO inputDTO);

	BizIncomeRequest saveThirdDraw(BizIncomeRequest draw);

	/**
	 * 通过 缓存信息 查询 单个订单
	 * 
	 * @param cacheStr
	 *            订单号#盘口id#类型
	 * @return
	 */
	BizIncomeRequest findOneByCacheStr(String[] cacheStr);

	/**
	 * 通过 缓存信息 查询一个单号对应多个收款账号的 入款记录
	 * 
	 * @param cacheStr
	 * @return
	 */
	List<BizIncomeRequest> findByCacheStrMultiToAccount(String[] cacheStr);

	/**
	 * 根据订单号查询订单
	 * 
	 * @param orderNo
	 * @return
	 */
	List<BizIncomeRequest> findByOrderNo(String orderNo);

	/**
	 * 查询微信账号记录:根据状态,是否有流水,是否有订单,盘口,层级,账号
	 */
	List<Object> findAlipayAccount(PageRequest pageRequest, Integer handicap, Integer level, String account,
			List<Integer> status, Integer type);

	/**
	 * 查询微信账号总记录:根据状态,是否有流水,是否有订单,盘口,层级,账号
	 */
	Long countAlipayAccount(Integer handicap, Integer level, String account, List<Integer> status, Integer type);

	/**
	 * 取消订单并通知平台
	 */
	boolean cancelAndCallFlatform(Integer status, Long incomeRequestId, String remark, String orderNo, Integer handicap,
			Integer accountId, String memberCode, SysUser sysUser);

	/**
	 * 先提流水再补提单金额不一致的，以流水金额为准补单子自动匹配再取消原来的单子
	 */
	boolean matchForInconsistentAmount(BizHandicap bizHandicap, String name, String memberAccount, String amount,
			AccountBaseInfo accountBaseInfo, Integer type, String remark, String remarkWrap, BizBankLog bankLog,
			BizIncomeRequest incomeRequest, SysUser loginUser);

	Object getMatchedInfo(Long requestId);

	String customerSendMsg(Long requestId, Long accountId, String message, String customerName);

	String generateIncomeRequestOrder(Integer pfTypeSub, BizHandicap bizHandicap, String name, String memberAccount,
			String amount, AccountBaseInfo accountBaseInfo, Integer type, String remark, String userName);

	String getSumAmountForCompanyIn(Integer status, Integer[] type, Integer[] toAccountIdList, Integer[] handicap,
			Integer level, String orderNo, String member, String payMan, BigDecimal fromMoney, BigDecimal toMoney,
			String startTime, String endTime, Integer operator);

	BizIncomeRequest findById(Long id);

	BizIncomeRequest getBizIncomeRequestByIdForUpdate(Long id);

	BigDecimal[] findThirdSumAmountByConditions(Integer status, Integer[] handicap, Integer level, String member,
			String orderNO, String toAccount, BigDecimal fromMoney, BigDecimal toMoney, String startTime,
			String endTime);

	/**
	 * 查询入款审核 第三方入款 已匹配或者未匹配 toAccount 商号
	 */
	Long findThirdMatchedOrUnMatchCount(Integer status, Integer[] handicap, Integer level, String member,
			String orderNo, String toAccount, BigDecimal fromMoney, BigDecimal toMoney, String startTime,
			String endTime, PageRequest pageRequest);

	/**
	 * 查询入款审核 第三方入款 已匹配或者未匹配 toAccount 商号
	 */
	Page<BizThirdRequest> findThirdMatchedOrUnMatchNoCount(Integer status, Integer[] handicap, Integer level,
			String member, String orderNo, String toAccount, BigDecimal fromMoney, BigDecimal toMoney, String startTime,
			String endTime, PageRequest pageRequest);

	/**
	 * 查询公司入款(已匹配 已取消) 总记录数
	 */
	String findMatchedOrCanceledCompanyInCount(Integer status, Integer[] handicap, Integer level,
			Integer[] toAccountIds, String member, BigDecimal fromMoney, BigDecimal toMoney, String startTime,
			String endTime, String orderNo, Integer operator);

	/**
	 * 查询公司入款(已匹配 已取消) 总金额
	 */
	String findMatchedOrCanceledCompanyInSum(Integer status, Integer[] handicap, Integer level, Integer[] toAccountIds,
			String member, BigDecimal fromMoney, BigDecimal toMoney, String startTime, String endTime, String orderNo,
			Integer operator);

	/**
	 * 分页查询公司入款(已匹配 已取消) 无总记录数
	 */
	Page<BizIncomeRequest> findMatchedOrCanceledCompanyInPageNocount(Integer status, Integer[] handicap, Integer level,
			Integer[] toAccountIds, String member, BigDecimal fromMoney, BigDecimal toMoney, String startTime,
			String endTime, String orderNo, Integer operator, PageRequest pageRequest);

	/**
	 * 有分页 无总记录数 查询入款提单
	 */
	Page<BizIncomeRequest> findCompanyInNoCount(String payMan, Integer status, Integer[] accountId, Integer level,
			String orderNo, String member, BigDecimal fromMoney, BigDecimal toMoney, String startTime, String endTime,
			PageRequest pageRequest);

	/**
	 * 查询正在提单的总记录数
	 */
	String findMatchingRequestCount(String payMan, int status, Integer[] accountId, Integer level, String orderNo,
			String member, String fromMoney, String toMoney, String startTime, String endTime, PageRequest pageRequest);

	/**
	 * 查询正在提单的总金额
	 */
	String findMatchingInComeRequestSum(String payMan, int status, Integer[] accountId, Integer level, String orderNo,
			String member, String fromMoney, String toMoney, String startTime, String endTime);

	List<BizIncomeRequest> findAll(Object[] parms);

	Page<BizIncomeRequest> findAll(Specification<BizIncomeRequest> specification, Pageable pageable) throws Exception;

	BigDecimal[] findAmountAndFeeByTotal(SearchFilter[] filterToArray);

	BizIncomeRequest get(Long id);

	List<BizIncomeRequest> findByIdList(List<Long> idList);

	/**
	 * 根据盘口与订单号查找唯一记录
	 *
	 * @param handicap
	 * @param orderNo
	 * @return
	 */
	BizIncomeRequest findByHandicapAndOrderNo(int handicap, String orderNo);

	BizIncomeRequest save(BizIncomeRequest entity, boolean needSaveFirst);

	/**
	 * 入款订单保存入库
	 * 
	 * @param o
	 * @return
	 */
	String saveOnly(BizIncomeRequest o);

	void match4OrdersByAliInBankAccount(List<BizIncomeRequest> toMatchIncomeReqList);

	void update4AliBankInOrders(List<Long> list, int updateType, String orderNo);

	BizIncomeRequest update(BizIncomeRequest entity);

	/**
	 * 取消入款
	 *
	 * @param handicap
	 * @param orderNoList
	 */
	void cancelOrder(Integer handicap, List<String> orderNoList);

	void delete(Long id);

	/**
	 * 匹配操作
	 *
	 * @param bankLog
	 *            银行流水
	 * @param incomeRequest
	 *            入款请求
	 * @param remark
	 *            备注信息
	 * @param confirmor
	 *            确认人
	 */
	void match(BizBankLog bankLog, BizIncomeRequest incomeRequest, String remark, String remarkWrap, SysUser confirmor);

	/**
	 * 支转银流水匹配订单后处理
	 *
	 * @param matchedReq
	 *            已匹配的订单记录
	 * @param bankLog
	 *            已匹配的流水
	 */
	void match4FlowsByAliInBankAccount(BizIncomeRequest matchedReq, BizBankLog bankLog);

	/**
	 * 根据订单号查询未匹配的订单记录
	 *
	 * @param orderNo
	 */
	List<BizIncomeRequest> findUnmatchedListByOrderNo(String orderNo);

	/**
	 * 根据入款提单记录的toId 检测是否这些toId是否是支转银收款账号
	 *
	 * @param list
	 * @return
	 */
	boolean checkAliInAccountByToIds(List<Integer> list);

	/**
	 * 向平台确认入款请求
	 *
	 * @param bankLog
	 *            银行流水
	 * @param incomeRequest
	 *            入款请求
	 * @param remark
	 *            备注信息
	 * @param confirmor
	 *            确认人
	 */
	boolean ack(BizBankLog bankLog, BizIncomeRequest incomeRequest, String remark, String remarkWrap,
			Integer confirmor);

	void moneyBeUsed(Integer handicapCode, String cardNo, Number money);

	BizIncomeRequest reject2CurrSys(long incomeId, String remark, SysUser operator);

	/**
	 * 保存停止接单原因
	 *
	 * @param remark
	 * @param username
	 * @return
	 * @throws Exception
	 */
	Map<String, Object> SaveStopOrder(String remark, String username, String type) throws Exception;

	/**
	 * 查询停止接单原因
	 *
	 * @param username
	 * @return
	 * @throws Exception
	 */
	Map<String, Object> SearStopOrder(String username, String type, String fristTime, String lastTime,
			PageRequest pageRequest) throws Exception;

	/**
	 * 查询指定盘口所有超时时间的公司（银行卡）入款请求，单次返回100条订单号集合
	 *
	 * @param handicap
	 * @param timeout
	 * @return
	 */
	List<String> findAllTimeout(Integer handicap, Date timeout, Date end);

	/**
	 * 取消指定盘口所有超时时间的公司（银行卡）入款请求
	 *
	 * @param handicap
	 * @param timeout
	 */
	void cancelAllTimeout(Integer handicap, Date timeout);

	/**
	 * 根据fromId toId 修改对应冗余账号信息，账号特殊修改时调用
	 */
	void updateAccount(Integer accountId, String account);

	Map<String, Object> findMatchedBySQL(String memberUsername, String fristTime, String lastTime,
			BigDecimal startamount, BigDecimal endamount, String orderNo, String toAccount, String operatorUid,
			String manual, String robot, List<Integer> handicapList, Pageable pageable);

	/**
	 * 根据toId，修改toId的当日转入累计金额
	 *
	 * @param toId
	 * @param amount
	 */
	void incrementAmount(Integer toId, BigDecimal amount);

	BizIncomeRequest findRebateLimit(BigDecimal amount, int fromAcount, String startTime, String endTime);

	void updateStatusById(Long id, Integer status);

	BizIncomeRequest findIncome(int accountId, String toAccountOwner, BigDecimal amount, String startTime,
			String endTime, int status);

	/**
	 * 入款流水整数金额 取消原订单 重新生成订单
	 *
	 * @param bankLog
	 * @param incom
	 */
	boolean generateOrderForIntegerBankAmount(BizBankLog bankLog, BizIncomeRequest incom);

	int findIncomeCounts(int accountId, String startTime, String endTime, int status, BigDecimal amount);

	void dealOccuTempLimit(BizBankLog bankLog);

	/**
	 * 保存公司用款记录
	 *
	 * @param handicap
	 *            那个盘口的公司用款（无:则：为空）
	 *
	 * @param th3Account
	 *            第三方账号
	 * @param oppBankType
	 *            收款方银行类型
	 * @param oppAccount
	 *            收款方银行账号
	 * @param oppOwner
	 *            收款方姓名
	 * @param amt
	 *            汇款金额 (大于零)
	 * @param fee
	 *            汇款手续费(大于等于零)
	 * @param operator
	 *            操作者 （not null）
	 * @param remark
	 *            备注
	 */
	BizIncomeRequest registCompanyExpense(BizHandicap handicap, BizAccount th3Account, String oppBankType,
			String oppAccount, String oppOwner, BigDecimal amt, BigDecimal fee, SysUser operator, String remark);

	/**
	 * 确认：公司用款转账失败
	 *
	 * @param inReqId
	 *            参考
	 *            {@link IncomeRequestService#registCompanyExpense(BizHandicap,BizAccount, String, String, String, BigDecimal, BigDecimal, SysUser, String)}
	 *            返回值的主键
	 * @param operator
	 *            确认人
	 * @param remark
	 *            备注
	 */
	void rollBackCompanyExpense(Long inReqId, SysUser operator, String remark);

	/**
	 * 确认：公司用款转账成功
	 *
	 * @param inReqId
	 *            参考
	 *            {@link IncomeRequestService#registCompanyExpense(BizHandicap,BizAccount, String, String, String, BigDecimal, BigDecimal, SysUser, String)}
	 *            返回值的主键
	 * @param operator
	 *            确认人
	 * @param remark
	 *            备注
	 */
	void commitCompanyExpense(Long inReqId, SysUser operator, String remark);
}
