package com.xinbo.fundstransfer.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.xinbo.fundstransfer.domain.pojo.TroubleShootDTO;

/**
 * Created by Administrator on 2018/9/19.
 */
public interface AsignFailedTaskService {

	// 判断用户是否在线
	boolean isOnlineUsers(int userId, int zone);

	// socket打开连接,用户保存在redis,分配排查任务
	boolean asignTaskOnSocketOpen(int userId);

	// 暂停接单 type=2 删除已分配的未锁定的任务,锁定的如有ip对应关系也删除
	// 结束接单 type=3 删除所有已分配给该用户任务
	void removeUserAndTasksInRedis(int userId, int type);

	// 转排查的时候分配排查任务
	boolean asignReviewTaskOnTurnToFail(long taskId);

	// 转排查的时候分配排查任务
	void asignOnTurnToFail(Long taskId);

	// 开始任务排查接单删除已缓存的订单
	void deleteIpAccountTasksOnStart(int userId);

	// 多于一个人接单的时候 根据taskid删除已分配未锁定的单子
	void deleteByTaskIdsAndUsers(List<String> taskIds, List<String> users);

	// 暂停任务排查接单
	void pauseReviewTask(int userId);

	// 暂停任务排查接单
	boolean dealOnpauseReviewTask(int userId);

	// 结束任务排查接单
	boolean dealOnstopReviewTask(int userId);

	// 结束任务排查接单
	void stopReviewTask(int userId);

	// 分配排查任务
	void asignReviewTask(int userId);

	// 处理重复分配
	void updateDuplicate();

	void popMessageOnStartOrStop(int userId, int type);

	// 分配还没分配的待排查的任务
	void asignFailureTasksFirstTime(int userId, int zone);

	void releaseLock4AsignTask();

	// 在redis上缓存的用户id
	void startTaskReview(int userId);

	int getLock4AsignTask();

	// 排查锁定任务
	int lockTaskForCheck(int taskId, int userId);

	// 处理排查
	int updateReviewTask(int taskId, String remark, int userId);

	// 添加备注
	int updateRemark(int userId, int taskId, String remark);

	double troubleShootSum(TroubleShootDTO troubleShootDTO, Integer[] operator, Integer fromAccount[],
			List<Integer> shooterList, String[] handicapCodes, List<String> lockedTaskIds);

	long troubleShootCount(TroubleShootDTO troubleShootDTO, Integer[] operator, Integer fromAccount[],
			List<Integer> shooterList, String[] handicapCodes, List<String> lockedTaskIds);

	// 查询taskid在某用户手上
	Object getTaskReviewByTaskId(int taskId);

	Map<String, Set<Integer>> getRealIpAndAccountIds(Integer[] accountIds);

	// 获取锁定的正在排查的记录
	List<String> getLockedTaskIdByUserId(Integer userId, String type);

	List<Object[]> troubleShootList(TroubleShootDTO troubleShootDTO, Integer[] operator, Integer fromAccount[],
			List<Integer> shooterList, String[] handicapCodes, List<String> lockedTaskIds);

	// 查询分配重复的记录
	List<Object[]> findDuplicaRecords();

	// 查询待排查任务
	List<Object[]> findFailureStatusTask(Integer userId);

	// 查询在线接单人员正在处理的单子数量 obj[0] operator obj[1] count 升序
	List<Object[]> getOperatorAndDealingCount(List<String> users);

	// 根据zone获取在线处理排查的人员
	List<String> getReviewingUserInRedis(int zone);

	// 获取正在排查的人和手中的排查任务
	List<Object[]> getReviewingUserAndReviewTask(List<String> users, int status);

	// 获取正在排查的人和手中的排查任务 但是已经暂停的
	List<Object[]> getUserAndReviewTaskNotInRedis(List<String> users);

	// 获取用户已分配的排查任务
	List<Object[]> getAssignedReviwTaskByUserId(List<String> users);
}
