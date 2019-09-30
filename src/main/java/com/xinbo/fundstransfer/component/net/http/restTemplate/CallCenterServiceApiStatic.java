package com.xinbo.fundstransfer.component.net.http.restTemplate;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xinbo.fundstransfer.chatPay.callCenter.reqVo.ReqCreateRoom;
import com.xinbo.fundstransfer.chatPay.callCenter.reqVo.ReqCreateToken;
import com.xinbo.fundstransfer.chatPay.callCenter.reqVo.ReqGetBizIdWithIncomeInfo;
import com.xinbo.fundstransfer.chatPay.callCenter.reqVo.ReqGetBizIdWithOutInfo;
import com.xinbo.fundstransfer.chatPay.inmoney.params.InmoneyNotifyInfoDto;
import com.xinbo.fundstransfer.domain.entity.BizOutwardRequest;

import lombok.extern.slf4j.Slf4j;


/**
 * ************************
 * 与 客服系统(聊天室支付) http交互
 * @author tony
 */

@Component
@Slf4j
public class CallCenterServiceApiStatic extends  BaseServiceApiStatic {

    /**
     * 出入款 调用客服系统，客服系统返回响应标志
     */
    private  static final String returnStatusKey = "code";
    private  static final String returnDataKey = "data";


    private static final String createRoom_path ="/roomt/createRoom";
    private static final String saveMemberIn_path ="/membert/saveIn";
    private static final String saveMemberOut_path ="/membert/saveOut";
    private static final String createToken_path ="/createToken";
    private static final String addToRoom_path ="/membert/addToRoom";
    private static final String inUserPaySuccessMatchingWater_path ="/inUserPaySuccessMatchingWater";
    private static final String payComplete_path ="/payComplete";




    /**
     * 出入款调用客服系统，判断是否调用成功
     */
    private boolean isReqSuccess(JSONObject resp){
        if(null!=resp && resp.containsKey(returnStatusKey) && resp.getIntValue(returnStatusKey)==200 ) return true;
        return false;
    }


    /**
     * 提交请求，过滤正确返回
     * @param path  请求路径
     * @param reqParam 请求参数
     * @param errMsg  错误消息
     * @return
     */
    protected JSONObject postJsonFilterSuccess(String path,Object reqParam,String errMsg){
        JSONObject resp = basePostJsonCallCenter(path, reqParam, callCenterBaseHeader);
        if(isReqSuccess(resp) && resp.containsKey(returnDataKey) && StringUtils.isNotBlank(resp.getString(returnDataKey))){
            return resp;
        }
        log.error("{}，uri:{},参数：{}，响应:{}",errMsg,path, JSON.toJSONString(reqParam),resp.toJSONString());
        throw new RuntimeException(errMsg);
    }



    /**
     * 请求生成房间号
     */
    public String createRoom(ReqCreateRoom reqCreateRoom){
        return postJsonFilterSuccess(createRoom_path, reqCreateRoom,"创建房间出错").getString(returnDataKey);
    }



    /**
     * 保存入款单信息，获取业务编号
     */
    public String getBizIdWithIncomeInfo(ReqGetBizIdWithIncomeInfo reqGetBizIdWithIncomeInfo){
        return postJsonFilterSuccess(saveMemberIn_path,reqGetBizIdWithIncomeInfo ,"保存入款单信息，获取业务编号出错").getString(returnDataKey);
    }



    /**
     * 保存出款单信息，获取业务编号
     */
    public String getBizIdWithOutInfo(ReqGetBizIdWithOutInfo reqGetBizIdWithOutInfo){
        return postJsonFilterSuccess(saveMemberOut_path,reqGetBizIdWithOutInfo ,"保存出款单信息，获取业务编号出错").getString(returnDataKey);
    }


    /**
     * 创建聊天室token
     * @param reqCreateToken 请求参数
     * @return 数组[0]：token
     *         数组[1]: 完整url,如： http://abc.com/?token=200
     */
    public String[] createTokenAndRoomUrl(ReqCreateToken reqCreateToken){
        String errMsg = "创建聊天室token出错";
        JSONObject resp = postJsonFilterSuccess(createToken_path, reqCreateToken, errMsg);
        String token = resp.getJSONObject(returnDataKey).getString("token");
        String domain = resp.getJSONObject(returnDataKey).getString("h5Url");
        if(StringUtils.isBlank(token) || StringUtils.isBlank(domain)) {
            log.error("{}，uri:{},参数：{}，响应:{}",errMsg,createToken_path, JSON.toJSONString(reqCreateToken),resp.toJSONString());
            throw new RuntimeException(errMsg);
        }
        return new String[]{token,domain+"/?token="+token};
    }




    /**
     * 使用token将用户拉入房间
     */
    public boolean addToRoom(String token){
        JSONObject reqParam = new JSONObject();
        reqParam.put("token",token);
        if(1==postJsonFilterSuccess(addToRoom_path, reqParam, "使用token将用户拉入房间出错").getIntValue(returnDataKey))
            return true;
        return false;
    }

    /**
     * 	会员入款成功，流水匹配成功
     */
    public boolean inUserPaySuccessMatchingWater(InmoneyNotifyInfoDto inmoney, BizOutwardRequest outward) {
    	JSONObject reqParam = new JSONObject();
        reqParam.put("inUserId",inmoney.getInUserId());
        reqParam.put("inCode",inmoney.getInCode());
        reqParam.put("inRoomId",inmoney.getInRoomId());
        reqParam.put("inToken",inmoney.getInToken());
        reqParam.put("outUserId",outward.getChatPayBizId());
        reqParam.put("outCode",outward.getOrderNo());
        reqParam.put("outRoomId",outward.getChatPayRoomNum());
        reqParam.put("outToken",outward.getChatPayRoomToken());       
        if(1==postJsonFilterSuccess(inUserPaySuccessMatchingWater_path, reqParam, "流水匹配成功，通知客服系统出错").getIntValue(returnDataKey))
            return true;
        return false;
    }
    
    /**
     * 	当本次出款金额=累计收款时，出入款调用本接口
     */
    public boolean payComplete(String userId, String code, String roomId, String token) {
    	JSONObject reqParam = new JSONObject();
        reqParam.put("userId",userId);
        reqParam.put("code",code);
        reqParam.put("roomId",roomId);
        reqParam.put("token",token);
        if(1==postJsonFilterSuccess(payComplete_path, reqParam, "出款成功，通知客服系统出错").getIntValue(returnDataKey))
            return true;
        return false;
    }

}
