package com.xinbo.fundstransfer.component.net.http.api;

import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.POST;
import rx.Observable;

/**
 * 平台相关接口在此提供
 *
 *
 */
public interface IPlatformService {
	/**
	 * 公司入款补提单
	 * 
	 * @param body
	 * @return
	 */
	@POST("/Pay/Deposit/")
	Observable<String> Deposit(@Body RequestBody body);

	/**
	 * 获取出款稽核信息 ,参数： iUserKey会员编号, sOrderId订单号
	 *
	 * @param body
	 * @return
	 */
	@POST("/WithdrawAuditInfo/")
	Observable<String> WithdrawAuditInfo(@Body RequestBody body);

	/**
	 * 出款锁定 ,参数： iUserKey会员编号, sOrderId订单号
	 *
	 * @param body
	 * @return
	 */
	@POST("/WithdrawMoneyLock/")
	Observable<String> WithdrawMoneyLock(@Body RequestBody body);

	/**
	 * 出款解除锁定 ,参数： iUserKey会员编号, sOrderId订单号
	 *
	 * @param body
	 * @return
	 */
	@POST("/WithdrawMoneyUnLock/")
	Observable<String> WithdrawMoneyUnLock(@Body RequestBody body);

	/**
	 * 出款确定，完成出款 ,参数： iUserKey会员编号, sOrderId订单号
	 *
	 * @param body
	 * @return
	 */
	@POST("/WithdrawMoneyOK/")
	Observable<String> WithdrawMoneyOK(@Body RequestBody body);

	/**
	 * 出款取消 ,参数： iUserKey会员编号, sOrderId订单号
	 *
	 * @param body
	 * @return
	 */
	@POST("/WithdrawMoneyCancel/")
	Observable<String> WithdrawMoneyCancel(@Body RequestBody body);

	/**
	 * 出款拒绝,参数： iUserKey会员编号, sOrderId订单号
	 *
	 * @param body
	 * @return
	 */
	@POST("/WithdrawRefuse/")
	Observable<String> WithdrawRefuse(@Body RequestBody body);

	/**
	 * 更新出款备注 ,参数： iUserKey会员编号, sOrderId订单号, sRemark备注
	 *
	 * @param body
	 * @return
	 */
	@POST("/WithdrawMoneyRemarkUpdate/")
	Observable<String> WithdrawMoneyRemarkUpdate(@Body RequestBody body);

	/**
	 * 为入款添加备注信息 ,参数： iUserKey会员编号, sOrderId订单号, sRemark备注
	 *
	 * @param body
	 * @return
	 */
	@POST("/DepositRemarkUpdate/")
	Observable<String> DepositRemarkUpdate(@Body RequestBody body);

	/**
	 * 锁定记录 ,参数： iUserKey会员编号, sOrderId订单号
	 *
	 * @param body
	 * @return
	 */
	@POST("/DepositMoneyLock/")
	Observable<String> DepositMoneyLock(@Body RequestBody body);

	/**
	 * 解除锁定,参数： iUserKey会员编号, sOrderId订单号
	 *
	 * @param body
	 * @return
	 */
	@POST("/DepositMoneyUnLock/")
	Observable<String> DepositMoneyUnLock(@Body RequestBody body);

	/**
	 * 取消记录 ,参数： iUserKey会员编号, sOrderId订单号, sRemark备注
	 *
	 * @param body
	 * @return
	 */
	@POST("/DepositMoneyCancel/")
	Observable<String> DepositMoneyCancel(@Body RequestBody body);

	/**
	 * 批量取消记录 ,参数： sOrderIdList订单号多个以英文逗号隔开, sRemark备注可选
	 *
	 * @param body
	 * @return
	 */
	@POST("/DepositMoneyBatchCancel/")
	Observable<String> DepositMoneyBatchCancel(@Body RequestBody body);

	/**
	 * 确认操作，先锁定，再确认 ,参数： iUserKey会员编号, sOrderId订单号
	 *
	 * @param body
	 * @return
	 */
	@POST("/DepositMoneyAudit/")
	Observable<String> DepositMoneyAudit(@Body RequestBody body);

}
