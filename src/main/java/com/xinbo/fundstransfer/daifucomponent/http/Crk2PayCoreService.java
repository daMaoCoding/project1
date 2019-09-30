package com.xinbo.fundstransfer.daifucomponent.http;

import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;
import rx.Observable;

/**
 * 出入款系统请求支付平台接口
 * @author blake
 *
 */
public interface Crk2PayCoreService {
	
	/**
	 * 发送订单请求支付 <br>
	 * 仅发送支付订单，其他信息（如密钥）等待payCore过来查询 <br>
	 * 请求url->http://PAY-CORE/reqDaiFu/{代付订单号}
	 * @param body
	 * @return
	 */
	@POST("/reqDaiFu/{orderNo}")
	Observable<Crk2PayCoreSendResponse> sendDaifuOrderNo(@Path("orderNo") final String orderNo,@Body RequestBody body);

	/**
	 * 查询第三方代付结构 <br>
	 * 请求url->http://PAY-CORE/reqDaiFu/query/{代付的订单号}
	 * @param body
	 * @return
	 */
	@POST("/reqDaiFu/query/{orderNo}")
	Observable<Crk2PayCoreQueryResponse> getDaifuResult(@Path("orderNo") final String orderNo,@Body RequestBody body);
	
	/**
	 * 获取商号余额 <br>
	 * 请求url->http://PAY-CORE/reqDaiFu/query/balance
	 * @param body
	 * <pre>参数：
{
	"api_OID":"100",    //业主OID
	"api_CHANNEL_BANK_NAME": "DUOFU",   //第三方ID,唯一标识第三方的，见Excel
	"api_MEMBERID": "MD63309719",  //商户号
	"api_KEY": "12919E9D9.....",   //私钥，加密的，如果公钥必填的，下面的公钥也要填写
	"api_PUBLIC_KEY":"",    //公钥，未加密的
	"api_OTHER_PARAM":"rest传递的其他参数,原样返回"  //其他参数
}
	 * </pre>
	 * @return
	 */
	@POST("/reqDaiFu/query/balance")
	Observable<Crk2PayCoreBalance> getBalance(@Body RequestBody body);
	
	
}
