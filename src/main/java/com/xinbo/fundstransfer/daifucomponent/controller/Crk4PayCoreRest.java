/**
 * 
 */
package com.xinbo.fundstransfer.daifucomponent.controller;

import java.sql.Timestamp;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.daifucomponent.dto.input.DaifuConfirmReqParamTo;
import com.xinbo.fundstransfer.daifucomponent.exception.PayPlatNotCallAgainException;
import com.xinbo.fundstransfer.daifucomponent.service.impl.DaifuServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 出入款为payCore提供的服务接口
 * @author blake
 *
 */
@Slf4j
@RestController
@RequestMapping("/userMoney")
public class Crk4PayCoreRest {
	
	@Autowired
	private DaifuServiceImpl daifuService;
	
	/**
	 * 查询订单支付参数<br>
	 * 这里查询写库，因为 AdminDbi -> AdminRest ->PayServer -> AdminDbi 就几毫秒，写库的数据有可能尚未同步到读库 
	 * @param orderId t_plat_outmoney_info.plat_pay_code
	 * @return <pre>
	 * {
		    "aPI_AMOUNT": "5000",        
		    "aPI_CHANNEL_BANK_NAME": "DUOFU_BANK_WEB_DF_ZGJSYH",
		    "aPI_CUSTOMER_ACCOUNT": "wangxiaojun",
		    "aPI_CUSTOMER_BANK_BRANCH": "陕西省分行",
		    "aPI_CUSTOMER_BANK_NAME": "中国建设银行",
		    "aPI_CUSTOMER_BANK_NUMBER": "6217004160022335741",
		    "aPI_CUSTOMER_BANK_SUB_BRANCH": "永寿县支行",
		    "aPI_CUSTOMER_NAME": "王小军",
		    "aPI_Client_IP": "123.123.123.123",
		    "aPI_KEY": "12919E9D90647E63040F17E67578234......3",  //加密的
		    "aPI_MEMBERID": "MD63309719",
		    "aPI_NOTIFY_URL_PREFIX": "http://66p.nsqmz6812.com:30000",
		    "aPI_OID": "100",
		    "aPI_ORDER_ID": "20190124115526959909",
		    "aPI_ORDER_STATE": "0",   //DB内部订单状态，pay-core记录，无其他用处
		    "aPI_OTHER_PARAM": "这是其他参数将会原样返回",  //这是db要传给fore的参数
		    "aPI_OrDER_TIME": "1548054754000",
		    "aPI_PUBLIC_KEY": ""
	}
	 * 
	 * </pre>
	 * @throws ResultException 
	 */
	@ResponseBody
	@RequestMapping(value = "/getPlatformConfigDaifu/{orderId}")
	public Object getPlatformConfigDaifu(@PathVariable("orderId") String orderId) throws Exception {
		Object result = null;
		try {
			result = daifuService.getConfigParamByOrderId(orderId);
		}catch (Exception e) {
			log.error("payCore访问getPlatformConfigDaifu获取代付信息时异常",e);
			HashMap<String,Object> result2 = new HashMap<>();
			if(e instanceof PayPlatNotCallAgainException) {
				result2.put("msg", e.getMessage());
				result2.put("result",1);
				result = result2;
			}
		}
		return result;
	}
	
	/**
	 * 根据pay的第三方出款处理状态，对平台数据进行不同的处理
	 * @param param <pre>{
	    "responseOrderID": "20190122113634826105"   //代付订单号
	    "responseDaifuCode": "SUCCESS",  //默认只会发送成功的消息，如果有收到ERROR的无需任何处理
	    "responseOrderState": "ERROR",     //这个重要：代付订单状态，PAYING-支付中，SUCCESS-代付成功，ERROR-代付失败或者是取消的，UNKNOW-未知状态(会很少出现长见于第三方回调的数据状态有改变无法正确获取，db无需处理就好)
	    "responseDaifuAmount": "5000",  //代付金额分，会与发送代付请求时一致。 
	    "responseDaifuChannel": "DUOFU_BANK_WEB_DF_ZGJSYH",  //通道名称
	    "responseDaifuErrorMsg": "第三方回调确定转账取消或失败。{\"order_no\":\"20181008165128\",\"notify_type\":\"back_notify\",\"merchant_code\":\"3018687\",\"return_params\":\"\",\"trade_time\":\"1538989431\",\"order_amount\":\"50.00\",\"trade_status\":\"success\",\"paid_amount\":\"50.00\",\"sign\":\"1a980312c256b10274f04e5020df3ff3\",\"trade_no\":\"118100816512990655\",\"order_time\":\"1538988689\"}",
	    "responseDaifuMemberId": "3018687",  //商户号
	    "responseDaifuMsg": "success",  //消息，没什么用
	    "responseDaifuOid": "100",   //业主OID
	    "responseDaifuOrderCreateTime": "1548055923000",  //代付订单创建时间
	    "responseDaifuOtherParam": "这是其他参数将会原样返回", //传递的其他参数
	    "responseDaifuSign": "0B64049055A550A5770206F1C3204321",  //pay-core 与业务系统确认身份的签名，目前没用
	    "responseDaifuTotalTime": 0     //超时时间 
	}</pre>
	 * 
	 * @return
	 * @throws Exception 
	 */
	@RequestMapping(value = "/platformInConfirmDaifu", method = RequestMethod.POST)
	@ResponseBody
	public Map<String,Object> platformInConfirmDaifu(@RequestBody DaifuConfirmReqParamTo param,HttpServletRequest request) {
		this.logComfirmRequest(param, request);
		//记录接收请求的时间
		param.setRequestTimeStr(String.format("%s", new Timestamp(System.currentTimeMillis())));
		Map<String,Object> result = new HashMap<>();
		try{
			result = daifuService.doOperationByPlatParam(param);
		}catch (Exception e) {
			String msg = e.getMessage();
			result.put("msg",msg);
			if(e instanceof PayPlatNotCallAgainException) {
				result.put("result", 1);
			}else {
				result.put("result", 0);
			}
		}
		return result;
	}
	
	private void logComfirmRequest(DaifuConfirmReqParamTo param,HttpServletRequest request){
		try{
			ObjectMapper mapper = new ObjectMapper();
			log.error(String.format("回调参数：%s", mapper.writeValueAsString(param)));
			log.error(String.format("回调请求地址：%s", request.getServletPath()));
			log.error(String.format("远程主机remoteAddr：%s", request.getRemoteAddr()));
			log.error(String.format("远程主机host：%s", request.getRemoteHost()));
			log.error(String.format("远程主机端口：%s", request.getRemotePort()));
			log.error(String.format("远程主机用户：%s", request.getRemoteUser()));
			Enumeration<String> headerNames = request.getHeaderNames();
			while(headerNames.hasMoreElements()){
				String nextHeaderKey = headerNames.nextElement();
				log.error(String.format("请求头：%s : %s", nextHeaderKey,request.getHeader(nextHeaderKey)));
			}
			log.error(String.format("Referer:  %s",request.getHeader("Referer")));
		}catch (Exception e) {
			log.error("记录第三方出款回调确认请求参数时发生错误！！！！",e);
		}
	}
}
