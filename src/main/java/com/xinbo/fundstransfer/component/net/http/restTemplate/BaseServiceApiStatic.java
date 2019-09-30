package com.xinbo.fundstransfer.component.net.http.restTemplate;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xinbo.fundstransfer.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * ************************
 *  RestTemplate 简单 Http 工具
 * @author tony
 */
@Slf4j
public class BaseServiceApiStatic {

    @Autowired  AppProperties appProperties;



    /**
     * 出入款调用平台CRK通用 http头信息
     */
    protected static final  Map<String,String> platformCrkBaseHeader = new HashMap<String,String>(){
        {
            put("CRK_KEY","CRK_CRK");
        }
    };




    /**
     * 出入款调用 聊天室  通用 http头信息 （暂时对方无限制）
     */
    protected static final  Map<String,String> callCenterBaseHeader = new HashMap<String,String>(){
        {
            put("FROM_KEY","FUNDS");
        }
    };



    /**
     * 平台
     */
    protected JSONObject basePostJsonPt(String path, Object obj, Map<String,String> header){
        try {
            String url = appProperties.getUri().concat(path);
            String respStr = RestTemplateUtil.postJson(url, obj, header);
            log.info("[发送平台http],地址:{},参数：{}，平台返回：{}",url,JSON.toJSONString(obj),respStr);
            if(StringUtils.isNotBlank(respStr)) return JSONObject.parseObject(respStr);
        }catch (Exception e){}
        return new JSONObject();
    }



    /**
     * 返利网
     */
    protected JSONObject basePostJsonFlw(String path, Object obj, Map<String,String> header){
        try {
            String url = appProperties.getRebateuri().concat(path);
            String respStr = RestTemplateUtil.postJson(url, obj, header);
            log.info("[发送返利网http],地址:{},参数：{}，返利网返回：{}",url,JSON.toJSONString(obj),respStr);
            if(StringUtils.isNotBlank(respStr)) return JSONObject.parseObject(respStr);
        }catch (Exception e){}
        return new JSONObject();
    }





    /**
     * 聊天室
     */
    protected JSONObject basePostJsonCallCenter(String path, Object obj, Map<String,String> header){
        try {
            String url = appProperties.getCallCenterUrl().concat(path);
            String respStr = RestTemplateUtil.postJson(url, obj, header);
            log.info("[发送聊天室http],地址:{},参数：{}，聊天室返回：{}",url,JSON.toJSONString(obj),respStr);
            if(StringUtils.isNotBlank(respStr)) return JSONObject.parseObject(respStr);
        }catch (Exception e){}
        return new JSONObject();
    }
}
