package com.xinbo.fundstransfer.service;

import com.xinbo.fundstransfer.daifucomponent.dto.output.DaifuResult;
import com.xinbo.fundstransfer.domain.entity.BizOutwardRequest;
import com.xinbo.fundstransfer.domain.entity.BizOutwardTask;

public interface DaifuTaskService {
	/** 检查 会员出款银行卡类型是否被支持 */
	boolean daifuBankTypeCondition(String memberBankType);

	/**
	 * 判断是否满足第三方代付条件
	 * 
	 * @param outwardRequest
	 * @return
	 */
	boolean daifuCondition(BizOutwardRequest outwardRequest);

	/**
	 * 调用代付
	 * 
	 * @param outwardTask
	 * @return
	 */
	DaifuResult callDaifu(BizOutwardTask outwardTask);

	/**
	 * 调用第三方代付立即返回结果处理
	 * 
	 * @param daifuResult
	 * @return
	 */
	boolean daifuResultDeal(DaifuResult daifuResult, BizOutwardTask outwardTask);

	/**
	 * 通知平台出款订单使用哪一个第三方出款
	 * 
	 * @param daifuResult
	 * @param outwardTask
	 */
	void selectDaifu(DaifuResult daifuResult, BizOutwardTask outwardTask);

	/**
	 * 查询第三方代付得到结果 之后处理
	 * 
	 * @param daifuResult
	 * @return
	 */
	boolean daifuQueryStatusDeal(DaifuResult daifuResult);

	BizOutwardTask dealMapStatus0(BizOutwardTask task);

	void dealMapStatus1(BizOutwardTask task, DaifuResult daifuResult);

	void dealMapStatus2(BizOutwardTask task, DaifuResult daifuResult);

	void dealMapStatus3(BizOutwardTask task, DaifuResult daifuResult);

	void dealMapStatus4(BizOutwardTask task, DaifuResult daifuResult);

	void intervene(BizOutwardTask outward);
}
