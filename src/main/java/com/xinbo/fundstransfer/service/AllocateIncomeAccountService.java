package com.xinbo.fundstransfer.service;

import java.util.List;

public interface AllocateIncomeAccountService {

	void reboot();

	boolean registOrCancel(int auditor, boolean regist);

	int[] findAllocatedCountAndAllCount();

	/**
	 * 分配入款审核账号
	 */
	void allocate(int inzone, boolean ifUnavailableBroadCast);

	/**
	 * 当账号分类或状态改变时，更新Redis存储值
	 */
	void update(int id, int type, int status);

	/**
	 * 判断该应用主机是否具有运行权限(update,allocate)
	 */
	boolean checkHostRunRight();

	/**
	 * 核对该用户是否可以停止接单（入款审核）
	 */
	String checkLogout(Integer userId);

	boolean checkHandicap(String handicapCode);

	List<Integer> findAccountIdList(List<Integer> auditorList);
}
