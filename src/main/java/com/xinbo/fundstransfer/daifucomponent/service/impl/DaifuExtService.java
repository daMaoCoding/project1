/**
 * 
 */
package com.xinbo.fundstransfer.daifucomponent.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.xinbo.fundstransfer.daifucomponent.http.Crk2PayCoreBalance;
import com.xinbo.fundstransfer.daifucomponent.http.Crk2PayCoreHttpClient;
import com.xinbo.fundstransfer.daifucomponent.http.Crk2PayCoreQueryResponse;
import com.xinbo.fundstransfer.daifucomponent.http.Crk2PayCoreSendResponse;
import com.xinbo.fundstransfer.daifucomponent.http.Crk2PayCoreService;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;

import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import rx.Observable;

/**
 * @author blake
 *
 */
@Slf4j
@Component("daifuExtService")
@ConfigurationProperties(prefix = "funds.transfer.transfer2paycode")
public class DaifuExtService implements InitializingBean {

	/**
	 * 各盘口访问payCore的baseurl<br>
	 * List<盘口号=baseUrl>
	 */
	private List<String> url;
	
	/**
	 * 各盘口访问payCore的baseUrl<br>
	 * <盘口号,baseUrl>
	 */
	private Map<String,String> baseUrlMap = new HashMap<>();
	
	/**
	 * @param url the url to set
	 */
	public void setUrl(List<String> url) {
		this.url = url;
	}
	
	/**
	* @return the url
	*/
	public List<String> getUrl() {
		return url;
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		if(!CollectionUtils.isEmpty(this.url)) {
			for(String urlStr:url) {
				String[] t = urlStr.split("=");
				if(t.length>=2) {
					baseUrlMap.put(t[0], t[1]);
				}
			}
		}
	}

	/**
	 * 查询代付结果
	 * @param handicap 盘口号，必填
	 * @param orderNo 出款订单号，必填
	 * @param otherParam 其他参数，备用
	 * @return
	 */
	public Crk2PayCoreQueryResponse queryDaifuResult(String handicap,String orderNo,Map<String,Object> otherParam) {
		ThreadLocal<Crk2PayCoreQueryResponse> threadLocal = new ThreadLocal<>();
		try {
			Crk2PayCoreService crk2PayCoreService = Crk2PayCoreHttpClient.getInstance()
					.getCrk2PayCoreApi(handicap,baseUrlMap.get(handicap));
			if(ObjectUtils.isEmpty(otherParam)) {
				otherParam = new HashMap<>();
			} 
			String json = ObjectMapperUtils.serialize(otherParam);
			if(StringUtils.isEmpty(json)) {
				json ="{}";
			}
			RequestBody body = RequestBody.create(MediaType.parse("application/json"), json); 
			Observable<Crk2PayCoreQueryResponse> res = crk2PayCoreService.getDaifuResult(orderNo,body);
			res.subscribe(ret -> {
				log.info("将盘口{}的出款订单号{}发送payCode时，请求正常，得到返回结果：{}",handicap,orderNo,ObjectMapperUtils.serialize(ret));
				threadLocal.set(ret);
			}, e -> {
				log.error("将盘口{}的出款订单号{}发送payCode时，请求异常 :{}",handicap,orderNo, e.getMessage());
				Crk2PayCoreQueryResponse errorRet = this.genCrk2PayCoreQueryResponseByReqError(handicap,orderNo);
				threadLocal.set(errorRet);
				//e.printStackTrace();
			});
		} catch (Exception e) {
			log.error("sendOrder2PayCore 时异常", e);
			Crk2PayCoreQueryResponse errorRet = this.genCrk2PayCoreQueryResponseByReqError(handicap,orderNo);
			threadLocal.set(errorRet);
		}
		return threadLocal.get();
	}
	
	/**
	 * 在发送出款单请求payCore代付时，产生异常。<br>
	 * 返回的 responseOrderState 为 UNKOWN
	 * @return
	 */
	private Crk2PayCoreQueryResponse genCrk2PayCoreQueryResponseByReqError(String handicap,String orderNo) {
		Crk2PayCoreQueryResponse res = new Crk2PayCoreQueryResponse();
		res.setResponseDaifuCode(DaifuServiceImpl.RESPONSE_DAIFU_CODE_SUCCESS);
		res.setResponseOrderState(DaifuServiceImpl.ORDER_STATE_UNKOWN);
		res.setResponseOrderID(orderNo);
		res.setResponseDaifuOid(handicap);
		return res;
	}


	/**
	 * 将订单号发送给 payCore,请求payCore代付
	 * @param handicap 盘口号，必填
	 * @param orderNo 出款订单号，必填
	 * @param otherParam 其他参数，备用
	 * @return
	 */
	public Crk2PayCoreSendResponse sendOrder2PayCore(String handicap,String orderNo,Map<String, Object> otherParam) {
		ThreadLocal<Crk2PayCoreSendResponse> threadLocal = new ThreadLocal<>();
		try {
			Crk2PayCoreService crk2PayCoreService = Crk2PayCoreHttpClient.getInstance()
					.getCrk2PayCoreApi(handicap,baseUrlMap.get(handicap));
			if(ObjectUtils.isEmpty(otherParam)) {
				otherParam = new HashMap<String, Object>();
			} 
			String json = ObjectMapperUtils.serialize(otherParam);
			RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
			Observable<Crk2PayCoreSendResponse> res = crk2PayCoreService.sendDaifuOrderNo(orderNo,body);
			res.subscribe(ret -> {
				log.info("将盘口{}的出款订单号{}发送payCode时，请求正常，得到返回结果：{}",handicap,orderNo,ObjectMapperUtils.serialize(ret));
				threadLocal.set(ret);
			}, e -> {
				log.error("将盘口{}的出款订单号{}发送payCode时，请求异常 :{}",handicap,orderNo, e.getMessage());
				Crk2PayCoreSendResponse errorRet = this.genCrk2PayCoreSendResponseByReqError(handicap,orderNo,e.getMessage());
				threadLocal.set(errorRet);
			});
		} catch (Exception e) {
			log.error("sendOrder2PayCore 时异常", e);
			Crk2PayCoreSendResponse errorRet = this.genCrk2PayCoreSendResponseByReqError(handicap,orderNo,e.getMessage());
			threadLocal.set(errorRet);
		}
		return threadLocal.get();
	}
	
	/**
	 * 在发送出款单请求payCore代付时，产生异常。<br>
	 * 返回的 requestDaifuOrderState 为 UNKOWN
	 * @return
	 */
	private Crk2PayCoreSendResponse genCrk2PayCoreSendResponseByReqError(String handicap,String orderNo,String errorMsg) {
		Crk2PayCoreSendResponse res = new Crk2PayCoreSendResponse();
		res.setRequestDaifuCode(DaifuServiceImpl.RESPONSE_DAIFU_CODE_SUCCESS);
		res.setRequestDaifuOrderState(DaifuServiceImpl.ORDER_STATE_UNKOWN);
		res.setRequestDaifuOrderId(orderNo);
		res.setRequestDaifuOid(handicap);
		res.setRequestDaifuErrorMsg(errorMsg);
		return res;
	}
	
	/**
	 * 获取代付商号在第三方的余额
	 * 
	 * @return
	 */
	public Crk2PayCoreBalance getBalance(String handicap, String channelName, String memberId, String thirdId,
			String privateKey, String publicKey) {

		ThreadLocal<Crk2PayCoreBalance> threadLocal = new ThreadLocal<>();
		try {
			Crk2PayCoreService crk2PayCoreService = Crk2PayCoreHttpClient.getInstance().getCrk2PayCoreApi(handicap,
					baseUrlMap.get(handicap));
			/**
			 * 参数： 
			 * { 
			 * "api_OID":"100", //业主OID 
			 * "api_CHANNEL_BANK_NAME": "DUOFU",
			 * "api_MEMBERID": "MD63309719", //商户号 
			 * "api_KEY":"12919E9D9.....", //私钥，加密的，如果公钥必填的，下面的公钥也要填写 
			 * "api_PUBLIC_KEY":"", //公钥，未加密的
			 * "api_OTHER_PARAM":"rest传递的其他参数,原样返回" //其他参数 
			 * }
			 */
			Map<String, Object> param = new HashMap<>();
			param.put("api_OID", handicap);
			param.put("api_CHANNEL_BANK_NAME", thirdId);
			param.put("api_MEMBERID", memberId);
			param.put("api_KEY", privateKey);
			param.put("api_PUBLIC_KEY", publicKey);
			String json = ObjectMapperUtils.serialize(param);
			RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
			Observable<Crk2PayCoreBalance> res = crk2PayCoreService.getBalance(body);
			res.subscribe(ret -> {
				log.info("盘口{}的代付通道{提供商{},商号{}}请求payCore查询余额，结果：{}", handicap, channelName, memberId,
						ObjectMapperUtils.serialize(ret));
				threadLocal.set(ret);
			}, e -> {
				log.error("盘口{}的代付通道{提供商{},商号{}}请求payCore查询余额，请求异常 :{}", handicap, channelName, memberId,
						e.getMessage());
				Crk2PayCoreBalance errorRet = this.genCrk2PayCoreBalanceResponseByReqError(channelName, memberId,
						thirdId, e.getMessage());
				threadLocal.set(errorRet);
			});
		} catch (Exception e) {
			log.error("sendOrder2PayCore 时异常", e);
			Crk2PayCoreBalance errorRet = this.genCrk2PayCoreBalanceResponseByReqError(channelName, memberId, thirdId,
					e.getMessage());
			threadLocal.set(errorRet);
		}
		return threadLocal.get();
	}
	
	private Crk2PayCoreBalance genCrk2PayCoreBalanceResponseByReqError(String channelName,String memberId,String thirdId,String errorMsg) {
		Crk2PayCoreBalance result = new Crk2PayCoreBalance();
		result.setRequestDaifuBalance(null);
		result.setRequestDaifuCode(DaifuServiceImpl.ORDER_STATE_ERROR);
		result.setRequestDaifuErrorMsg(errorMsg);
		result.setReqyestDaifuChannelMemberId(memberId);
		return result;
	}
}
