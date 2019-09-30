package com.xinbo.fundstransfer.service;

import com.xinbo.fundstransfer.domain.entity.BizAccountRebate;
import com.xinbo.fundstransfer.domain.entity.BizOutwardRequest;
import com.xinbo.fundstransfer.domain.entity.BizOutwardTask;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.TransferEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public interface AllocateOutwardTaskService {
	/**
	 * 常量标识：出款任务分类</br>
	 * 首次出款：FIRT_OUT_YES <br/>
	 * 机器出款:ROBOT_OUT_YES</br>
	 * 人工出款：MANUAL_OUT_YES<br/>
	 * 第三方出款:THIRD_OUT_YES</br>
	 */
	int FIRT_OUT_YES = 1, ROBOT_OUT_YES = 0, MANUAL_OUT_YES = 1, THIRD_OUT_YES = 3;

	/**
	 * 常量：所有盘口标识；任意盘口
	 */
	int WILD_CARD_HANDICAP = 0;

	String FIN_ACKED = "执行确认";

	/**
	 * 描述:出款请求审批之后生成出款任务
	 *
	 * @param req
	 *            出款请求订单信息
	 * @param task
	 *            生成的出款任务信息
	 * @param third
	 *            该出款任务是否需要第三方出款
	 */
	void rpush(BizOutwardRequest req, BizOutwardTask task, boolean third);

	/**
	 * 停止接单
	 * <p>
	 * 广播接受函数接口
	 * </p>
	 *
	 * @param tarId
	 *            {@code from==0} : userId {@code from==1}:accountId
	 * @param from
	 *            : 0 人工出款/第三方出款 1：cabana出款
	 */
	void suspend(int tarId, boolean real, int from);

	/**
	 * 分配 出款任务
	 */
	void schedule() throws InterruptedException;

	/**
	 * 取消排队 (机器人)
	 */
	void cancelQueue4Robot(List<Integer> accountIdList);

	/**
	 * 取消排队 (机器人)
	 */
	void cancelQueue4Robot(Integer accountId);

	/**
	 * 申请 出款任务 （机器）
	 */
	TransferEntity applyTask4Robot(int accountId, BigDecimal bankBalance);

	/**
	 * 申请 出款任务 （机器） 新
	 */
	TransferEntity applyTask4RobotNew(int accountId, BigDecimal bankBalance);

	/**
	 * 申请 出款任务 (人工)
	 */
	BizOutwardTask applyTask4User(int userId);

	/**
	 * 申请 出款任务 （手机）
	 */
	TransferEntity applyTask4Mobile(int accountId, BigDecimal bankBalance);

	/**
	 * 申请 出款任务 （手机） 新
	 */
	TransferEntity applyTask4MobileNew(int accountId, BigDecimal bankBalance);

	/**
	 * 确认 转账结果(机器出款)
	 */
	void ack4Robot(TransferEntity entity);

	/**
	 * 确认 转账结果 (人工出款)
	 */
	void ack4User(Long taskId, Integer operator, String remark, Integer accountId, boolean thirdOut,
			String platPayCode);

	/**
	 * 备注 (客服)
	 */
	void remark4Custom(long taskId, SysUser operator, String remark);

	void remark4Mgr(int accId, boolean isManual, boolean isThird, SysUser operator, String remark) throws Exception;

	/**
	 * 备注 (主管)</br>
	 * 此处不要加事务处理，万一出现数据不一致情况，人工排查。</br>
	 * 加事务可能会出现重复出款现象
	 */
	void remark4Mgr(long taskId, boolean isManual, boolean isThird, SysUser operator, String[] bankTypeArr,
			String remark) throws Exception;

	/**
	 * 备注(财务确认)
	 *
	 * @throws Exception
	 *             操作者为空</br>
	 *             任务不存在;</br>
	 *             该任务未匹配流水</br>
	 *             出款请求不存在</br>
	 *             该任务非公司用款</br>
	 */
	void remark4Ack(long taskId, SysUser operator, String remark) throws Exception;

	/**
	 * 转无效 bankType指定新任务出款账号类型 drawManner 指定出款方式 人工 机器 第三方
	 */
	void alterStatusToInvalid(long taskId, SysUser operator, String remark, String[] bankType, String drawManner);

	/**
	 * 转失败
	 */
	void alterStatusToFail(long taskId, SysUser operator, String remark);

	/**
	 * 转主管
	 */
	void alterStatusToMgr(int accId, String remark);

	/**
	 * 转主管
	 */
	void alterStatusToMgr(long taskId, SysUser operator, String remark, String screenshot);

	/**
	 * 转主管
	 */
	void alterStatusToMgr(BizOutwardTask task, SysUser operator, String remark, String screenshot);

	void sreenshot(BizOutwardTask task, String sreenshot);

	/**
	 * 转未知
	 */
	void alterStatusToUnknown(long taskId, SysUser operator, String remark, String screenshot);

	/**
	 * 转取消
	 */
	void alterStatusToCancel(Long reqId, Long taskId, SysUser operator, String remark);

	/**
	 * 转拒接
	 */
	void alterStatusToRefuse(Long reqId, Long taskId, SysUser operator, String remark);

	/**
	 * 转已匹配<br/>
	 * bankFlowId 可以为空
	 */
	void alterStatusToMatched(Long taskId, Long bankFlowId, SysUser operator, String remark);

	/**
	 * 从待排查转完成
	 * <p>
	 * 1.如果该任务 有流水与其匹配，则该任务状态从待排查变为已匹配</br>
	 * 2.如果该任务 无流水与其匹配，则该任务状态从待排查变为完成出款
	 * </p>
	 *
	 * @param taskId
	 *            任务ID
	 * @param operator
	 *            操作人
	 * @param remarkOri
	 *            备注
	 */
	void ToFinishFromFailOrManagerDeal(long taskId, SysUser operator, String remarkOri);

	/**
	 * 重置出款任务分配停止分配时间
	 */
	void alterHaltTimeToNull();

	/**
	 * 修改银行维护信息
	 */
	void alterMaintainBank();

	/**
	 * 出款完成通知平台
	 * <p>
	 * 1.检测出款请求是否已确认，已确认：直接返回；未确认：流程继续</br>
	 * 2.检测出款请求的所有任务，是否全部完成出款，否：直接返回；是：流程继续</br>
	 * 3.通知平台，该出款请求已完成出款</br>
	 * 4.根据第三步返回结果，会写出款请求状态
	 * </p>
	 *
	 * @param operator
	 *            操作人ID
	 * @param req
	 *            出款请求
	 */
	void noticePlatIfFinished(Integer operator, BizOutwardRequest req);

	/**
	 * 第三方代付 调用通知平台
	 *
	 * @param task
	 * @param req
	 * @param platPayCode
	 */
	void callPlatForDaifuSuccessOut(BizOutwardTask task, BizOutwardRequest req, String platPayCode);

	/**
	 * 检测银行间转账
	 *
	 * @param peerTrans
	 *            toBank是否处于同行转账模式
	 * @param fromBank
	 *            汇出银行
	 * @param toBank
	 *            汇入银行
	 * @return true:fromBank可以转账到toBank；false:fromBank不可以转账到toBank
	 */
	boolean checkTrans(Boolean peerTrans, String fromBank, String toBank);

	/**
	 * 检测同行转账
	 * <p>
	 * 1.检测同行转账模式是否处于打开状态,否：返回null,是：流程继续</br>
	 * 2.检测该银行是否处于同行转账模式中,否：返回false,是：返回true</br>
	 * </p>
	 *
	 * @param currBank
	 *            当前银行全称 或 当前银行简称
	 * @return null:同行转账模式关闭；true:同行转账模式打开且处于同行转账模式；false:同行转账模式打开且不处于同行转账模式
	 */
	Boolean checkPeerTrans(String currBank);

	/**
	 * 检测当前银行是否处于维护状态
	 *
	 * @param currBank
	 *            当前银行
	 * @return true:维护中；false:非维护中
	 */
	boolean checkMaintain(String currBank);

	/**
	 * 检查是否存在流水
	 */
	Long checkBankLog(long taskId);

	boolean checkThird(int userId);

	boolean checkManual(BizOutwardRequest req, Float taskAmt);

	/***
	 *
	 * @return true:fr->to 可以转账;false:不可以转账
	 */
	boolean checkPeer(Boolean frPeer, AccountBaseInfo fr, AccountBaseInfo to);

	/**
	 * 检测首次出款
	 * <p>
	 * 检测出款审核信息中是否包含"首次出款"或"出款银行卡不一致"
	 * </p>
	 *
	 * @param review
	 *            出款请求审核记录
	 */
	boolean checkFirst(String review);

	boolean checkBank(String bankType);

	/**
	 * 获取第三方最低限额
	 */
	int findThirdLowBal();

	/**
	 * 外层拆单金额
	 */
	int findSplitOut();

	/**
	 * 内层拆单金额
	 */
	int findSplitIn();

	/**
	 * set the attribute value for <tt>MergeLevel</tt> program.
	 *
	 * @param broad
	 *            true:broadcast the <tt>MergeLevel</tt> program settings to all web
	 *            server hosts;false:update the native host's
	 *            <tt>MergeLevel</tt>program settings;
	 * @param on
	 *            0:close the <tt>MergeLevel</tt> program;1:open the
	 *            <tt>MergeLevel</tt> program;
	 * @param durTm
	 *            the hours the <tt>MergeLevel</tt> program lasts from now on.
	 * @see this#getMergeLevel(int)
	 */
	void setMergeLevel(boolean broad, int zone, boolean on, int durTm);

	/**
	 * Returns the attribute value setting for <tt>MergeLevel</tt>.If the attributes
	 * haven't been set or expired in cache, then this method returns
	 * <code>new long[]{0,0,0}</code>
	 *
	 * <p>
	 * the return value is a long array which contains only 3 elements. the first
	 * one :0:close the <tt>MergeLevel</tt> program;1:open the <tt>MergeLevel</tt>
	 * program; the second one:the hours the <tt>MergeLevel</tt> program lasts if
	 * the program is opened</br>
	 * the third one:the deadline's milliseconds
	 *
	 * @return the attribute value setting for <tt>MergeLevel</tt> or
	 *         <code>new long[]{0,0,0}</code> if the attributes haven't been set or
	 *         expired in cache.
	 * @see this#setMergeLevel(boolean, int, boolean, int)
	 */
	long[] getMergeLevel(int zone);

	/**
	 * store account outward time.
	 *
	 * @param accId
	 *            account's identity
	 * @param taskId
	 *            task's identity
	 */
	void recordLog(Integer accId, Long taskId);

	// 正在出款的任务重新分配
	void reAssignDrawingTask(BizOutwardTask task, boolean manner, String[] targetBankType, SysUser sysUser,
			String remark);

	/**
	 * 放 返利任务 到 分配队列
	 */
	void rpush(BizAccountRebate req, boolean isManual);

	/**
	 * 更新task信息，暂时只是更新出款耗时和转账截图
	 *
	 * @param task
	 */
	void updateBizOutwardTask(BizOutwardTask task, TransferEntity entity);

	boolean checkInv(AccountBaseInfo base, Set<Integer> ids);

	/**
	 * 获取余额告警值
	 *
	 * @param base
	 * @return
	 */
	int buildLimitBalance(AccountBaseInfo base);

	/**
	 * 增加或删除已分配对象ID
	 *
	 * @param atedId
	 * @param addOrRemove
	 */
	void addOrRemoveAllocated(Integer atedId, boolean addOrRemove);

	/**
	 * 超时任务甩出
	 */
	void turnExpireOutwardTask() throws InterruptedException;

	/**
	 * 判断是否人工出款
	 * 
	 * @param req
	 * @param taskAmt
	 * @return
	 */
	boolean isManualTask(BizOutwardRequest req, Float taskAmt);

	Object getAllocNeetCache(String key);
}
