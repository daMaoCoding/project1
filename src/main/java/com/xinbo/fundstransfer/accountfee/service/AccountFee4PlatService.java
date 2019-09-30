/**
 * 
 */
package com.xinbo.fundstransfer.accountfee.service;

import javax.validation.Valid;

import com.xinbo.fundstransfer.component.redis.msgqueue.HandleException;
import com.xinbo.fundstransfer.accountfee.pojo.AccountFeeConfig;
import com.xinbo.fundstransfer.accountfee.pojo.AccountFee4PlatLevelAddReq;
import com.xinbo.fundstransfer.accountfee.pojo.AccountFee4PlatLevelDelReq;
import com.xinbo.fundstransfer.accountfee.pojo.AccountFee4PlatUpdateReq;

/**
 * 第三方下发手续费规则接口
 * @author Blake
 *
 */
public interface AccountFee4PlatService {

	/**
	 * 提供平台使用<br>
	 * 平台查询第三方下发手续费规则
	 * @param handicap
	 * @param bankType
	 * @param account
	 * @return
	 * @throws HandleException
	 */
	AccountFeeConfig findByPlat(String handicap, String bankType, String account)  throws HandleException;
	
	/**
	 * 提供平台使用<br>
	 * @param requestBody
	 * @throws HandleException
	 */
	void updateByPlat(@Valid AccountFee4PlatUpdateReq requestBody)  throws HandleException;

	/**
	 * 提供平台使用<br>
	 * @param requestBody
	 * @throws HandleException
	 */
	void calFeeLevelAddByPlat(@Valid AccountFee4PlatLevelAddReq requestBody)  throws HandleException;

	/**
	 * 提供平台使用<br>
	 * @param requestBody
	 * @throws HandleException
	 */
	void calFeeLevelDelByPlat(@Valid AccountFee4PlatLevelDelReq requestBody)  throws HandleException;

}
