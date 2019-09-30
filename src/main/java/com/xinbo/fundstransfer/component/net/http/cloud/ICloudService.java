package com.xinbo.fundstransfer.component.net.http.cloud;

import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.POST;
import rx.Observable;

/**
 * 云端相关接口在此提供
 *
 * @author Eden
 */
public interface ICloudService {

	@POST("/r/mobile/put")
	Observable<Object> rMobilePut(@Body RequestBody body);

	@POST("/r/mobile/get")
	Observable<Object> rMobileGet(@Body RequestBody body);

	@POST("/r/mobile/updBase")
	Observable<Object> rMobileUpdBase(@Body RequestBody body);

	@POST("/r/mobile/updAccStatus")
	Observable<Object> rMobileUpdAccStatus(@Body RequestBody body);

	@POST("/r/mobile/bonusAccList")
	Observable<Object> rMobileBonusAccList(@Body RequestBody body);

	@POST("/r/mobile/getBal")
	Observable<Object> rMobileGetBal(@Body RequestBody body);

	@POST("/r/mobile/pageForQr")
	Observable<Object> rMobilePageForQr(@Body RequestBody body);

	@POST("/r/mobile/genQR")
	Observable<Object> rMobileGenQR(@Body RequestBody body);

	@POST("/r/mobile/genExactQR")
	Observable<Object> rMobileGenExactQR(@Body RequestBody body);

	@POST("/r/mobile/genZeroQR")
	Observable<Object> rMobileGenZeroQR(@Body RequestBody body);

	@POST("/r/mobile/delQR")
	Observable<Object> rMobileDelQR(@Body RequestBody body);

	@POST("/r/mobile/delQRList")
	Observable<Object> rMobileDelQRList(@Body RequestBody body);

	@POST("/r/mobile/del")
	Observable<Object> rMobileDel(@Body RequestBody body);

	@POST("/r/bank/transfer")
	Observable<Object> rBankTransfer(@Body RequestBody body);

	@POST("/r/mobile/pwd")
	Observable<Object> rMobilePwd(@Body RequestBody body);

	@POST("/r/mobile/updAcc")
	Observable<Object> rMobileUpdAcc(@Body RequestBody body);

	@POST("/r/bonus/totalForEach")
	Observable<Object> rBonusTotalForEach(@Body RequestBody body);

	@POST("/r/bonus/findIncomeLogByAliAcc")
	Observable<Object> rBonusFindIncomeLogByAliAcc(@Body RequestBody body);

	@POST("/r/bonus/findIncomeLogByWecAcc")
	Observable<Object> rBonusFindIncomeLogByWecAcc(@Body RequestBody body);

	@POST("/r/bonus/bonus")
	Observable<Object> rBonusBonus(@Body RequestBody body);

	@POST("/r/mobile/page")
	Observable<Object> rMobilePage(@Body RequestBody body);

	@POST("/api/wechat/findWechatLogByWechar")
	Observable<Object> findWechatLogByWechar(@Body RequestBody body);

	@POST("/api/wechat/findWechatMatched")
	Observable<Object> findWechatMatched(@Body RequestBody body);

	@POST("/api/wechat/findMBAndInvoice")
	Observable<Object> findMBAndInvoice(@Body RequestBody body);

	@POST("/api/wechat/findWechatUnClaim")
	Observable<Object> findWechatUnClaim(@Body RequestBody body);

	@POST("/api/wechat/findWechatCanceled")
	Observable<Object> findWechatCanceled(@Body RequestBody body);

	@POST("/api/wechat/wechatAck")
	Observable<Object> wechatAck(@Body RequestBody body);

	@POST("/api/wechat/wecahtCancel")
	Observable<Object> wechatdepositCancel(@Body RequestBody body);

	@POST("/api/wechat/UpdateDepositTime")
	Observable<Object> UpdateWechatDepositTime(@Body RequestBody body);

	@POST("/api/wechat/customerAddRemark")
	Observable<Object> customerAddRemarkWecaht(@Body RequestBody body);

	@POST("/fund2mas/remedyOrder")
	Observable<Object> generateRequestOrder(@Body RequestBody body);

	@POST("/api/alipay/statisticalAliLog")
	Observable<Object> statisticalAliLog(@Body RequestBody body);

	@POST("/api/alipay/findAliMBAndInvoice")
	Observable<Object> findAliMBAndInvoice(@Body RequestBody body);

	@POST("/api/alipay/findAliMatched")
	Observable<Object> findAliMatched(@Body RequestBody body);

	@POST("/api/alipay/findAliCanceled")
	Observable<Object> findAliCanceled(@Body RequestBody body);

	@POST("/api/alipay/findAliUnClaim")
	Observable<Object> findAliUnClaim(@Body RequestBody body);

	@POST("/api/alipay/aliPayAck")
	Observable<Object> aliPayAck(@Body RequestBody body);

	@POST("/api/alipay/aliPayCancel")
	Observable<Object> aliPaydepositCancel(@Body RequestBody body);

	@POST("/api/alipay/UpdateDepositTime")
	Observable<Object> UpdateAliPayDepositTime(@Body RequestBody body);

	@POST("/api/alipay/customerAddRemark")
	Observable<Object> customerAddRemarkAliPay(@Body RequestBody body);

	@POST("/fund2mas/account/search")
	Observable<Object> search(@Body RequestBody body);

	@POST("/fund2mas/account/launchCheck")
	Observable<Object> launchCheck(@Body RequestBody body);

	@POST("/fund2mas/account/searchCount")
	Observable<Object> searchCount(@Body RequestBody body);

	@POST("/fund2mas/trade/searchDetail")
	Observable<Object> searchDetail(@Body RequestBody body);

	@POST("/fund2mas/log/logDetail")
	Observable<Object> logDetail(@Body RequestBody body);

	@POST("/fund2mas/log/logDetailCountAndSum")
	Observable<Object> logDetailCountAndSum(@Body RequestBody body);

	@POST("/api/countReceipts/countReceipts")
	Observable<Object> countReceipts(@Body RequestBody body);

	@POST("/api/countReceipts/sysdetail")
	Observable<Object> sysDetail(@Body RequestBody body);

	@POST("/r/sys/pro/find")
	Observable<Object> rSysFindPro(@Body RequestBody body);

	@POST("/r/sys/pro/save")
	Observable<Object> rSysSavePro(@Body RequestBody body);
}
