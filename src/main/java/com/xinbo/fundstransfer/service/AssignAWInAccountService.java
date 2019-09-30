package com.xinbo.fundstransfer.service;

import java.util.Map;
import java.util.Set;

/**
 * Created by Administrator on 2018/10/18. 入款审核人员 点击接单的时候 分配支付宝微信入款账号
 */
public interface AssignAWInAccountService {
	// 获取所有支付宝 微信 账号 状态包括:1 3 4
	Map<Integer, Map<Integer, Set<Object[]>>> getAWAccountsAll(int userId);

	// 获取支付宝 微信 账号
	Map<Integer, Set<Object[]>> getAWAccountsByType(int type);

	// 根据用户id获取手中接单的账号信息 id account owner
	Map<Integer, Set<Object[]>> getAccountIdsByUser(int userId, int type);

	// 获取redis上在线接单用户
	Set<String> getUsersOnLine(Integer zone);

	// 获取已经被分配的用户
	Set<String> getAssignedUsers(Integer zone);

	// socket打开开始接单 保存接单用户
	long saveUserToRedis(int userId, String zone);

	// socket关闭结束接单 删除用户缓存
	long deleteUserOnRedis(int userId);

	// 平台有支付宝 微信账号推送发消息
	void sendMessageOnPushAccount(int type, int accountId);

	// 平台账号更新的时候
	int dealOnAccountUpdate(int accountId, int type);

	// 支付宝 微信账号 有推送或者变更的时候分配
	int assignAccountToLeastUser(int accountId, int type, int status);

	// socket 打开 接单 分配账号
	boolean assignOnStartByUser(int userId);

	// socket 关闭 结束接单 分配账号
	boolean assignOnStopByUser(int userId);

	// 删除分配的某个支付宝 微信
	int deleteAssignedAccountByUser(int userId, int accountId, int type);

	// 用户接单或者结束接单的时候 删除事件队列里多余事件
	void popMessageOnStartOrStop(int userId, int type);

	// 删除已分配的记录
	void deleteAssignedRecord(int zone);

	// 用户结束接单的时候,删除该用户的缓存
	void deleteAssignedUser(int userId);

	// 开始接单的时候 异常退出的情况下 删除不在线接单的用户缓存
	void deleteCacheUserOffline(int zone);

	// 获取分配锁
	int lock4AssignAW();

	// 释放分配锁
	void unlock4AssignAW();
}
