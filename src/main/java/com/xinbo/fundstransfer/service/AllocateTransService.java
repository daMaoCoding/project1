package com.xinbo.fundstransfer.service;

import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.MonitorStat;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.enums.CurrentSystemLevel;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.TransLock;
import com.xinbo.fundstransfer.domain.pojo.TransTo;
import com.xinbo.fundstransfer.domain.pojo.TransferEntity;
import com.xinbo.fundstransfer.service.impl.TransMonitorServiceImpl;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface AllocateTransService {

	/**
	 * 常量：所有账号
	 */
	int WILD_CARD_ACCOUNT = 0;

	void schedule() throws InterruptedException;

	/**
	 * get the transfer task
	 * <p>
	 * from account includes InBank|ReserveBank|IssueBank account
	 * </p>
	 * 
	 * @param fromId
	 *            from account ID
	 * @param reBal
	 *            inclusive null
	 * @return transfer information </br>
	 *         if no available transfer at present, null as result.
	 */
	TransferEntity applyByFrom(int fromId, BigDecimal reBal);

	TransferEntity applyByFromNew(int fromId, BigDecimal reBal);

	/**
	 * get the transfer task by cloud funds transfer system
	 * <p>
	 * if {@code  relBal == null || relBal.compareTo(BigDecimal.ZERO)<=0},
	 * {@code null} would be as result.
	 *
	 * @param bindType
	 *            10:weichat;11：alipay
	 *
	 * @param acc
	 *            the from-account's card number.
	 * @param handi
	 *            handicap code defined by platform system.
	 * @param l
	 *            current system level.
	 * @param relBal
	 *            the real balance at present.
	 * @return TransferEntity#getToAccountId();</br>
	 *         TransferEntity#getAccount();</br>
	 *         TransferEntity#getOwner();</br>
	 *         TransferEntity#getBankType();</br>
	 *         TransferEntity#getBankAddr();</br>
	 *         TransferEntity#getAcquireTime();</br>
	 *         TransferEntity#getAmount();</br>
	 * @see BizHandicap#getCode()
	 * @see CurrentSystemLevel#getValue()
	 * @see com.xinbo.fundstransfer.domain.enums.AccountType#BindAli
	 * @see com.xinbo.fundstransfer.domain.enums.AccountType#BindWechat
	 */
	TransferEntity applyByCloud(int bindType, String acc, String handi, Integer l, BigDecimal relBal);

	/**
	 * acknowledge the robot transfer result.
	 * 
	 * @param entity
	 *            transfer entity
	 *
	 */
	void ackByRobot(TransferEntity entity);

	/**
	 * confirm the result of transaction from frAcc account to toId account.
	 * 
	 * @param orderNo
	 *            the order No
	 * @param handicapCode
	 *            the handicap code defined by plat form system.
	 * @param frAcc
	 *            the card number of frAcc
	 * @param toId
	 *            the identity of toAcc defined in funds transfer system.
	 * @param amt
	 *            transfer amount.
	 * @param acquireTime
	 *            the time the cloud funds transfer system claim task.
	 * @param ret
	 *            true: transfer success</br>
	 *            false: transfer failure</br>
	 * @see TransferEntity#getAcquireTime()
	 */
	void ackByCloud(String orderNo, String handicapCode, String frAcc, int toId, BigDecimal amt, long acquireTime,
			boolean ret);

	/***
	 * report the real balance of account
	 *
	 * @param id
	 *            identity of account.
	 * @param bal
	 *            the real balance of account
	 */
	void applyRelBal(int id, BigDecimal bal);

	/***
	 * report the real balance of account
	 *
	 * @param id
	 *            identity of account.
	 * @param bal
	 *            the real balance of account
	 *
	 * @param repTm
	 *            report time
	 */
	void applyRelBal(int id, BigDecimal bal, Long repTm, boolean updApiInvokeTm);

	/**
	 * set the upper limit of out account
	 * <p>
	 * <code>if on == false </code> the params upLim,durTm are in invalid.
	 * <code>if on == true</code> the params upLim ,durTm are valid
	 * </p>
	 * 
	 * @param on
	 *            true:start </br>
	 *            false:stop
	 * @param upLim
	 *            the upper limit of out account
	 * @param triglimit
	 *            the trigger limit of out account
	 * @param durTm
	 *            time of duration. mintue is time unit.</br>
	 */
	void setUpLim4ONeed(int zone, boolean on, int upLim, int triglimit, int durTm);

	/**
	 * get the upper limit of out account
	 * 
	 * @return int[0] 1:start;otherwise:0</br>
	 *         int[1] upLim </br>
	 *         int[2] trigLim</br>
	 *         int[3] durTm</br>
	 *         int[4] expire Time</br>
	 * @see this#setUpLim4ONeed(int,boolean, int,int, int)
	 */
	long[] getUpLim4ONeed(int zone);

	/**
	 * get the information that transfer to to-account
	 * 
	 * @param toId
	 *            to-account's ID</br>
	 *            exclusive :OutBank
	 */
	List<TransTo> buildTransTo(int toId);

	/**
	 * get transfer statistics information
	 */
	List<MonitorStat> buildTransStat(SysUser operator);

	/**
	 * get all transaction which transfer to out-account at present.
	 *
	 * @param operator
	 *            the operator at current action.
	 * @see com.xinbo.fundstransfer.domain.pojo.TransTo
	 */
	List<TransTo> buildTransTo4Transing(SysUser operator);

	/**
	 * get all out-account needing funds at present.
	 * 
	 * @param operator
	 *            the operator at current action.
	 * @see com.xinbo.fundstransfer.domain.pojo.TransTo
	 */
	List<TransTo> buildTransTo4Needing(SysUser operator);

	/**
	 * fill up remark to TransLock
	 *
	 * @param operator
	 *            the user filling up remark to TransLock
	 * @param orderNo
	 *            orderNo the TransLock's order Number.
	 * @param remark
	 *            the remark from the operator
	 */
	void remark4TransLock(SysUser operator, String orderNo, String remark);

	/**
	 * fill up remark to Trans Request.
	 * 
	 * @param operator
	 *            the user filling up remark to Trans Request.
	 * @param reqId
	 *            the identity of the trans request.
	 * @param remark
	 *            the remark from the operator
	 *
	 */
	void remark4TransReq(SysUser operator, Long reqId, String remark);

	/**
	 * cancel the transaction acknowledge record
	 * 
	 * @param operator
	 *            the operator at present
	 * 
	 * @param reqId
	 *            the income request ID
	 */
	void cancelTransAck(SysUser operator, long reqId);

	/**
	 * get the account's real balance in cache
	 * <p>
	 * if the real balance doesn't exist. null as result.
	 * </p>
	 *
	 * @param accId
	 *            the account's ID
	 * @return the account's real balance
	 */
	@SuppressWarnings("unused")
	BigDecimal buildRealBalInCache(Integer accId);

	BigDecimal[] buildFlowMatching(int accId);

	/**
	 * get all account's ID that has the matching flows transferred to other
	 * account.
	 */
	Set<Integer> buildAcc4FlowOutMatching();

	Map<Integer, BigDecimal[]> buildFlowOutMatching();

	/**
	 * get all factors that resist other accounts transfer funds to the account.
	 * 
	 * @param accId
	 *            the account that received funds transferred from other accounts
	 * @return the factors that resist other accounts transfer funds to the
	 *         accounts. the factors is separated by <tt>;</tt>
	 */
	String buildResistFactors(int accId);

	/**
	 * send alarm information to the front desk by web socket, especially, front web
	 * page .
	 */
	void sendAlarmToFrontDesk();

	/**
	 * account's ID that has matching flows transferred to other account ,either has
	 * been in Risk Status by monitoring the real balance transformation.
	 * 
	 * @see this#buildAcc4FlowOutMatching()
	 * @see TransMonitorServiceImpl#buildAcc4MonitorRisk()
	 */
	Set<Integer> buildAcc4Alarm();

	Set<Integer> buildValidAcc();

	/**
	 *
	 * @param fromId
	 * @param toId
	 * @param operator
	 * @param transInt
	 * @throws Exception
	 */
	void lockTrans(Object fromId, Integer toId, Integer operator, Integer transInt, Integer expTM) throws Exception;

	Double enScore4Fr(int type, int zone, int l, int handi, int bal);

	Integer[] deScore4Fr(Double score);

	boolean llockUpdStatus(Object fromId, Integer toId, int status);

	TransLock buildLock(boolean ignore, int fromId);

	/**
	 * 激活账号
	 */
	TransferEntity activeAccByTest(int fromId, boolean create);

	/**
	 * 测试转账，测试转账和激活账号调同一个接口，只是不过滤银行，所以新增一个接口
	 */
	TransferEntity getTestTrans(int fromId, boolean create);

	/**
	 * 激活转账任务上报
	 *
	 * @param entity
	 */
	void ackTrans(TransferEntity entity);

	/**
	 * 申请转账任务
	 *
	 * @param fromId
	 * @param create
	 * @param amount
	 * @return
	 */
	TransferEntity applyTrans(int fromId, boolean create, Float amount, boolean isFilterBank);

	/**
	 * 给指定账户分配金额区间的任务
	 *
	 * @param baseInfo
	 * @param min
	 * @param max
	 */
	boolean allocByAccAndBal(AccountBaseInfo baseInfo, Integer min, Integer max);

	boolean checkBlack(int accountId);

	/**
	 * 第三方入款卡 下发到出款卡 锁定
	 *
	 * @param fromId
	 * @param toId
	 * @param operator
	 * @param transInt
	 * @param expireTimeSeconds
	 * @return
	 */
	boolean lockForThirdDrawToOutCard(Object fromId, Integer toId, Integer operator, Integer transInt,
			Integer expireTimeSeconds);

	/***
	 * 下发任务 锁定 生成锁
	 * 
	 * @param fromId
	 * @param toId
	 * @param operator
	 * @param transInt
	 * @param expireTimeSeconds
	 * @return
	 */
	boolean lockForDrawTaskToOutCard(Object fromId, Integer toId, Integer operator, Integer transInt,
			Integer expireTimeSeconds);

	/**
	 * 第三方入款卡 下发到出款卡 解锁
	 * 
	 * @param fromId
	 * @param toId
	 * @return
	 */
	boolean unLockForThirdDrawToOutCard(Object fromId, Integer toId);

	/**
	 * 下发任务 解锁 或者匹配解锁 打回解锁 超时解锁
	 * 
	 * @param fromId
	 * @param toId
	 * @return
	 */
	boolean unLockForDrawTaskToOutCard(Object fromId, Integer toId);

	/**
	 * 第三方下发完成之后 移除出款卡
	 * 
	 * @param toId
	 * @return
	 */
	void removeNeedAmountOutCard(Integer toId);

	/**
	 * 获取账户最新余额
	 *
	 * @param target
	 * @return
	 */
	BigDecimal getCurrBalance(Integer target);

	Map<Integer, BigDecimal> allCurrBalance(@Nullable List<Integer> accountIds);

	boolean hasTrans(int accid);

	/**
	 * 当前余额 + 当日出款 是否大于当日出款限额
	 *
	 * @param accId
	 * @return
	 */
	boolean exceedAmountSumDailyOutward(int accId);

	boolean exceedAmountSumDailyOutward2(Integer accId);

	/**
	 * 当前余额 + 当日出款 是否大于当日出款限额
	 *
	 * @param accId
	 * @return
	 */
	boolean exceedAmountSumDailyOutwardNew(int accId);

	/**
	 * 用于账号收款时，判断账号是否在线
	 *
	 * @param accId
	 * @return
	 */
	boolean isOnline(int accId);

	TransLock buildLockToId(boolean ignore, int toId);

	Map<Integer, TransLock> allToIdsTransLocks(List<Integer> accountIds);

	void applyRelBalNew(int id, BigDecimal bal, Long repTm, boolean updApiInvokeTm);

	boolean checkDailyIn(AccountBaseInfo base);

	boolean checkDailyIn2(Map<Integer, BigDecimal> dailyIn, AccountBaseInfo base);

	List<String> onLineAcc(List<Integer> accountIds);

	void inOutModelCheck(String accountId, String inoutModel);

	/**
	 * 连续转账失败的数据
	 *
	 * @return
	 */
	Set<Integer> buildFailureTrans();

	/**
	 * 校验账户是否连续转账失败
	 *
	 * @param failure
	 * @param accId
	 * @return
	 */
	boolean checkFailureTrans(Set<Integer> failure, Integer accId);

}
