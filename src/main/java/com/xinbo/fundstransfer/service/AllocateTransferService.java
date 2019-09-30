package com.xinbo.fundstransfer.service;

import com.xinbo.fundstransfer.domain.pojo.TransferEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public interface AllocateTransferService {

	/**
	 * 常量：所有账号
	 */
	int WILD_CARD_ACCOUNT = 0;

	/**
	 * 最大误差 MAX_TOLERANCE(生产环境，测试环境：BigDecimal.TEN)</br>
	 */
	int MAX_TOLERANCE = 1100;

	/**
	 * 常量：账号预留余额设置(生产环境,测环境：BigDecimal.TEN)</br>
	 * TRANS_ROBOT_FEE:机器出款时预留金额(生产环境) </br>
	 * TRANS_MANUAL_FEE:人工出款时预留金额(生产环境) </br>
	 */
	BigDecimal TRANS_ROBOT_BAL = new BigDecimal(50), TRANS_MANUAL_BAL = new BigDecimal(20);

	/**
	 * 转账测试
	 */
	TransferEntity applyByTest(int fromId);

	/**
	 * 机器转账申请(from账号 发起)
	 */
	TransferEntity applyByFrom(int fromId, BigDecimal reBal);

	/**
	 * 云端转账申请
	 * 
	 * @param acc
	 *            账号
	 * @param handi
	 *            盘口编码
	 * @param l
	 *            内外层
	 * @param relBal
	 *            余额
	 * @return 转账实体
	 */
	TransferEntity applyByFrom(String acc, String handi, Integer l, BigDecimal relBal);

	/***
	 * 账号实时余额上报
	 * 
	 * @param id
	 *            账号ID
	 * @param relBal
	 *            账号实时余额
	 * @param f
	 *            是否强制更新真实余额
	 */
	void applyRelBal(int id, BigDecimal relBal, boolean f);

	/**
	 * 转账确认
	 */
	void ackByRobot(TransferEntity entity);

	/**
	 * 转账确认
	 */
	void ackByCloud(String frAcc, int toId);

	/**
	 * 取消转账
	 */
	void cancelByCloud(String frAcc, int toId) throws Exception;

	/**
	 * 获取转账金额
	 *
	 * @param from
	 *            汇款账号
	 * @param to
	 *            汇入账号
	 * @param operator
	 *            操作者 operator== ppConstants.USER_ID_4_ADMIN 代表机器
	 * @return BigDecimal[0] 汇款金额整数部分 BigDecimal[1] 汇款金额小数部分
	 */
	BigDecimal[] findTrans(int from, int to, int operator);

	/**
	 * 转账解锁
	 *
	 * @param toId
	 *            转入账号
	 */
	void unlockTrans(Integer fromId, Integer toId, Integer operator) throws Exception;

	/**
	 * 获取汇出账号ID
	 * 
	 * @param toId
	 *            汇入账号
	 */
	List<Integer> findFrIdList(Integer toId);

	Integer transToReqType(int fromAccountType);

	/**
	 * 把账号添加到下发黑名单
	 * 
	 * @param accId
	 *            账号ID
	 * @param canDel
	 *            是否可以被自动删除
	 */
	void addToBlackList(int accId, boolean canDel);

	/**
	 * fr 账号给指定任务出款加入黑名单
	 */
	void addToBlackList(int fr, long taskId, long expirSeconds);

	/**
	 * 把账号从下发黑名单中移除
	 * 
	 * @param accId
	 *            账号ID
	 */
	void rmFrBlackList(int accId);

	/**
	 * 该账号是否在下发黑名单中
	 *
	 * @param toId
	 *            账号ID
	 * @return true:不在下发黑名单中</br>
	 *         false:在下发黑名单中</br>
	 */
	boolean checkBlack(int toId);

	/**
	 * 下发黑名单中是否可以被自动删除
	 *
	 * @param toId
	 *            账号ID
	 * @return true:可以被自动删除</br>
	 *         false:不可以被自动删除</br>
	 */
	boolean blackCanDel(int toId);

	/**
	 * 判断frId与toId 之间是否存在转账关系
	 * 
	 * @param black
	 *            所有黑名单集
	 * @param frId
	 *            汇出账号
	 * @param toId
	 *            汇入账号
	 * @return true:存在转账关系；false:不存在转账关系
	 */
	boolean checkBlack(Set<String> black, int frId, int toId);

	boolean checkBlack(Set<String> black, String frAcc, int toId);

	boolean checkBlack(Set<String> black, int frAcc, long taskId);

	/**
	 * 查询所有黑名单集
	 * 
	 */
	Set<String> findBlackList();

	/**
	 * 第三方转账锁定判断是否已经锁定
	 */
	boolean isLockThirdTrans(String toIdOperatorUid, String operatorUid) throws Exception;

	/**
	 * 第三方转账锁定判断是否已经锁定 全部匹配
	 */
	boolean isLockThirdTransByoperator(String toIdOperatorUid, String operatorUid) throws Exception;
}
