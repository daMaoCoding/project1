/**
 * 
 */
package com.xinbo.fundstransfer.accountfee.service;

import java.math.BigDecimal;
import java.util.List;

import com.xinbo.fundstransfer.accountfee.exception.NoSuiteAccountFeeRuleException;
import com.xinbo.fundstransfer.accountfee.pojo.AccountFeeCalResult;
import com.xinbo.fundstransfer.accountfee.pojo.AccountFeeConfig;
import com.xinbo.fundstransfer.component.redis.msgqueue.HandleException;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;

/**
 * 第三方下发手续费规则接口
 * @author Blake
 *
 */
public interface AccountFeeService {

	/**
	 * 根据账号id查询第三方下发手续费规则
	 * @param accountBaseInfo
	 * @return
	 */
	AccountFeeConfig findByAccountBaseInfo(BizAccount accountBaseInfo);

	AccountFeeConfig findByAccountBaseInfo2(AccountBaseInfo accountBaseInfo);

	/**
	 * 根据账号id查询第三方下发手续费规则
	 * 
	 * @param base
	 * @return
	 */
	AccountFeeConfig findTh3FeeCfg(AccountBaseInfo base);

	/**
	 * 按账号id修改第三方下发手续费规则
	 * @param accountId 账号
	 * @param operationAdminName 操作人用户名，必填
	 * @param feeType 收费方式：0-从商户余额扣取手续费 1-从到账金额扣取手续费，必填
	 * @param calFeeType 手续费计算规则：0-按百分比计费 1-按阶梯式计费，必填
	 * @param calFeePercent 统一收取费率，大于等于0小于1的小数。当calFeeType = 0 时必填。
	 * @param calFeeLevelType 阶梯计费类型 0-按百分比计费， 1-按金额计费。当carFeeType = 1 时必填。
	 */
	void update(BizAccount accountBaseInfo,String operationAdminName,Byte feeType,Byte calFeeType,BigDecimal calFeePercent,Byte calFeeLevelType) throws HandleException;

	/**
	 * 为账号的第三方下发手续费规则添加计费阶梯
	 * @param accountId 账号
	 * @param operationAdminName 操作人用户名，必填
	 * @param calFeeLevelType 阶梯计费类型 0-按百分比计费，1-按金额计费 。必填。
	 * @param moneyBegin 开始金额，必填。大于等于0。必填。
	 * @param moneyEnd 结束金额，大于开始金额。必填。
	 * @param feeMoney 当 calFeeLevelType=1-按金额计费 时必填。大于等于 0。
	 * @param feePercent 当 calFeeLevelType=0-按金额计费 时必填。大于等于 0 小于 1。
	 */
	void calFeeLevelAdd(BizAccount accountBaseInfo,String operationAdminName,Byte calFeeLevelType,Double moneyBegin,Double moneyEnd,BigDecimal feeMoney,BigDecimal feePercent) throws HandleException;

	/**
	 * 为账号的第三方下发手续费规则删除计费阶梯
	 * @param accountId 账号
	 * @param operationAdminName 操作人用户名，必填
	 * @param calFeeLevelType 阶梯计费类型 0-按百分比计费 1-按金额计费。必填。
	 * @param indexId 查询时返回的计费阶梯 index。必填
	 */
	void calFeeLevelDel(BizAccount accountBaseInfo,String operationAdminName,Byte calFeeLevelType,Long indexId) throws HandleException;

	/**
	 * 第三方下发计算每次下发的金额和手续费
	 * @param accountBaseInfo 第三方账号信息
	 * @param inMoney 需要计算手续费的金额，单次可下余额
	 * @return 返回计算结果
	 * @throws NoSuiteAccountFeeRuleException 无适用计费规则时抛出该异常
	 */
	AccountFeeCalResult calAccountFee(BizAccount accountBaseInfo,BigDecimal inMoney) throws NoSuiteAccountFeeRuleException;

	AccountFeeCalResult calAccountFee2(AccountBaseInfo accountBaseInfo, BigDecimal inMoney)
			throws NoSuiteAccountFeeRuleException;

	/**
	 * 过滤未设置有效的下发规则的第三方账号
	 * @param thirdAccounts
	 * @return 存在有效下发规则账号的列表，非 null ，有可能size = 0
	 */
	List<Integer> filterNoEffectFeeConfig(List<Integer> thirdAccounts);

}
