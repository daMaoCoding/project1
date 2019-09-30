package com.xinbo.fundstransfer.service;

import com.xinbo.fundstransfer.domain.entity.BizAccountBinding;

import java.util.List;

/**
 * @desc 下法卡绑定 Created by Eden on 2017/7/6.
 */
public interface AccountBindingService {

	/**
	 * 入款微信，入款支付宝，入款第三方 与下发卡绑定
	 * 
	 * @param incomeAccountId
	 *            入款长啊后
	 * @param issueAccountId
	 *            下发卡账号ID
	 * @param bind1Unbind0
	 *            1:绑定；0：解除绑定
	 */
	void bindOrUnbind(List<Integer> issueAccountId, Integer incomeAccountId, Integer bind1Unbind0) throws Exception;

	/**
	 * 根据入款账号查询所绑定的下发卡账号
	 * 
	 * @param accountId
	 *            入款账号
	 */
	List<Integer> findBindAccountId(Integer accountId);

	/**
	 * 根据下发卡账号查询所绑定的入款账号
	 *
	 * @param bindAccountId
	 *            下发卡账号
	 */
	List<Integer> findAccountId(Integer bindAccountId);

	List<BizAccountBinding> findByBindAccountIdList(List<Integer> bindAccountIdList);

	List<BizAccountBinding> findByAccountIdList(List<Integer> accountIdList);

	/**
	 * 根据 第三方入款账号id 和 绑定的账号id 查询是否已经绑定
	 * 
	 * @param accountId
	 * @param bindedId
	 * @return
	 */
	BizAccountBinding findByAccountIdAndBindedId(Integer accountId, Integer bindedId);
}
