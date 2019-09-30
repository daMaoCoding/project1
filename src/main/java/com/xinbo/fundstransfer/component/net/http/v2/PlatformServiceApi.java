package com.xinbo.fundstransfer.component.net.http.v2;

import com.xinbo.fundstransfer.domain.ResponseDataNewPay;
import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.domain.pojo.WithdrawAuditInfo;
import okhttp3.RequestBody;
import retrofit2.http.*;
import rx.Observable;

/**
 * 平台调用新系统相关接口在此提供
 */
public interface PlatformServiceApi {
	/**
	 * 入款补提单
	 *
	 * @param body
	 * @return
	 */
	@POST("/deposit/put")
	@Headers({ "CRK_KEY:CRK_CRK" })
	Observable<SimpleResponseData> deposit(@Body RequestBody body);

	/**
	 * 获取出款稽核信息
	 *
	 * @param body
	 * @return
	 */
	@POST("/withdraw/audit")
	@Headers({ "CRK_KEY:CRK_CRK" })
	Observable<WithdrawAuditInfo> withdrawalAudit(@Body RequestBody body);

	/**
	 * 出款确定
	 *
	 * @param body
	 * @return
	 */
	@POST("/withdraw/ack")
	@Headers({ "CRK_KEY:CRK_CRK" })
	Observable<SimpleResponseData> WithdrawalAck(@Body RequestBody body);

	/**
	 * 出款取消
	 *
	 * @param body
	 * @return
	 */
	@POST("/withdraw/cancel")
	@Headers({ "CRK_KEY:CRK_CRK" })
	Observable<SimpleResponseData> withdrawalCancel(@Body RequestBody body);

	/**
	 * 出款拒绝
	 *
	 * @param body
	 * @return
	 */
	@POST("/withdraw/reject")
	@Headers({ "CRK_KEY:CRK_CRK" })
	Observable<SimpleResponseData> withdrawalReject(@Body RequestBody body);

	/**
	 * 取消入款
	 *
	 * @param body
	 * @return
	 */
	@POST("/deposit/cancel")
	@Headers({ "CRK_KEY:CRK_CRK" })
	Observable<SimpleResponseData> depositCancel(@Body RequestBody body);

	/**
	 * 入款确认
	 *
	 * @param body
	 * @return
	 */
	@POST("/deposit/ack")
	@Headers({ "CRK_KEY:CRK_CRK" })
	Observable<SimpleResponseData> depositAck(@Body RequestBody body);

	/**
	 * 1.1.38 锁定money在12小时内不能再次使用
	 *
	 * @param body
	 * @return
	 */
	@POST("/crk/moneyBeUsed")
	@Headers({ "CRK_KEY:CRK_CRK" })
	Observable<ResponseDataNewPay<Boolean>> moneyBeUsed(@Body RequestBody body);

	/**
	 * 同步:入款 出款 层级 账号
	 *
	 * @param body
	 * @return
	 */
	@POST("/sync")
	@Headers({ "CRK_KEY:CRK_CRK" })
	Observable<SimpleResponseData> sync(@Body RequestBody body);

	/**
	 * 查询大额出款 参数:handicap orderNo token
	 *
	 * @param body
	 * @return
	 */
	@POST("/crk/findBigWin")
	@Headers({ "CRK_KEY:CRK_CRK" })
	Observable<SimpleResponseData> findBigWin(@Body RequestBody body);

	/**
	 * 本次入款信息查询，包括注册优惠、公司入款、第三方入款、人工存入、转点转入 参数:handicap orderNo token
	 *
	 * @param body
	 * @return
	 */
	@POST("/crk/findThis")
	@Headers({ "CRK_KEY:CRK_CRK" })
	Observable<SimpleResponseData> findThis(@Body RequestBody body);

	/**
	 * 本次账号登陆IP查询 参数:handicap orderNo token
	 *
	 * @param body
	 * @return
	 */
	@POST("/crk/findThisLoginIp")
	@Headers({ "CRK_KEY:CRK_CRK" })
	Observable<SimpleResponseData> findThisLoginIp(@Body RequestBody body);

	/**
	 * 本次相同IP登陆账号查询 参数:handicap orderNo token
	 *
	 * @param body
	 * @return
	 */
	@POST("/crk/findThisSameIp")
	@Headers({ "CRK_KEY:CRK_CRK" })
	Observable<SimpleResponseData> findThisSameIp(@Body RequestBody body);

	/**
	 * 获取盘口支付回调地址
	 *
	 * @param handicap
	 *            盘口号
	 * @return
	 */
	@GET("/ownerUrl4Crk/sync/{handicap}")
	@Headers({ "CRK_KEY:CRK_CRK" })
	Observable<SimpleResponseData> getNotifyUrlByhandicap(@Path("handicap") String handicap);

	/**
	 * 查询平台 代付供应商支持的银行类型
	 *
	 * @param body
	 * @return
	 */
	@POST("/daifu/bankTypeList")
	@Headers({ "CRK_KEY:CRK_CRK" })
	Observable<SimpleResponseData> querySurpportBankType(@Body RequestBody body);

	/**
	 * 6.2.14 通知平台出款订单使用哪一个第三方出款
	 *
	 * @param body
	 * @return
	 */
	@POST("/withdraw/selectDaifu")
	@Headers({ "CRK_KEY:CRK_CRK" })
	Observable<SimpleResponseData> selectDaifu(@Body RequestBody body);

	/**
	 * 查询入款卡切换类型时是否有告警信息
	 *
	 * @param body
	 * @return
	 */
	@POST("/withdraw/sync")
	@Headers({ "CRK_KEY:CRK_CRK" })
	Observable<SimpleResponseData> WithdrawSync(@Body RequestBody body);

	/**
	 * 第三方账号冻结的时候 同步给平台
	 *
	 * @param body
	 * @return
	 */
	@POST("/crk/syncThirdStatus")
	@Headers({ "CRK_KEY:CRK_CRK" })
	Observable<SimpleResponseData> syncThirdStatus(@Body RequestBody body);
}
