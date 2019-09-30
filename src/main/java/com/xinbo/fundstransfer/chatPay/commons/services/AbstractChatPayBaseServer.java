package com.xinbo.fundstransfer.chatPay.commons.services;

import com.xinbo.fundstransfer.chatPay.callCenter.reqVo.ReqCreateRoom;
import com.xinbo.fundstransfer.chatPay.callCenter.reqVo.ReqCreateToken;
import com.xinbo.fundstransfer.chatPay.callCenter.reqVo.ReqGetBizIdWithIncomeInfo;
import com.xinbo.fundstransfer.chatPay.callCenter.reqVo.ReqGetBizIdWithOutInfo;
import com.xinbo.fundstransfer.chatPay.commons.config.AliIncomeConfig;
import com.xinbo.fundstransfer.chatPay.commons.config.AliOutConfig;
import com.xinbo.fundstransfer.chatPay.flwsynzfbwx.services.BizQrInfoServer;
import com.xinbo.fundstransfer.chatPay.ptorder.repository.ChatPayAccountTranslogRepository;
import com.xinbo.fundstransfer.chatPay.ptorder.repository.ChatPayLogRepository;
import com.xinbo.fundstransfer.chatPay.ptorder.repository.ChatPayReqInMoneyRepository;
import com.xinbo.fundstransfer.chatPay.ptorder.repository.ChatPayReqOutMoneyRepository;
import com.xinbo.fundstransfer.chatPay.ptorder.services.impl.ChatPayValidateServer;
import com.xinbo.fundstransfer.chatPay.ptorder.services.impl.RoomHandlerAdapter;
import com.xinbo.fundstransfer.component.net.http.restTemplate.CallCenterServiceApiStatic;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.BizIncomeRequest;
import com.xinbo.fundstransfer.domain.entity.BizLevel;
import com.xinbo.fundstransfer.domain.entity.BizOutwardRequest;
import com.xinbo.fundstransfer.domain.repository.IncomeRequestRepository;
import com.xinbo.fundstransfer.domain.repository.OutwardRequestRepository;
import com.xinbo.fundstransfer.service.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * ************************
 *  聊天室支付 服务 常量
 * @author tony
 */
@Slf4j
public abstract class AbstractChatPayBaseServer extends ChatPayBaseServer{

    /**
     * 聊天室支付-入款/出款 返平台字段
     */
    protected static final String sysOperater = "系统管理员";
    protected static final String outMoneyOrderNumberFormater = "%sO%s";
    protected RuntimeException unSupportException =  new RuntimeException("暂不支持的入款类型，目前只支持支付宝。");
    protected RuntimeException cannotLookRoom =  new RuntimeException("无法锁定入款房间");
    protected RuntimeException cannotFindRoom =  new RuntimeException("未找到出款会员房间号，也未找到合适兼职创建代收房间。");


    protected long callCenterReservedTimeMillis = 1000L;  //与聊天室通讯+聊天室处理预留时间,如需要可放入sysUserProfile/ChatPayConfigService

    protected static final String roomNumKey = "roomNumKey";
    protected static final String roomNumLockKey = "roomNumLockKey";


    /**
     * 聊天室支付-入款/出款 服务 依赖
     */

    @Autowired   protected   BizQrInfoServer bizQrInfoServer;
    @Autowired   protected   CallCenterServiceApiStatic callCenterServiceApiStatic;
    @Autowired   protected   ChatPayReqOutMoneyRepository chatPayReqOutMoneyRepository;
    @Autowired   protected   OutwardRequestRepository outwardRequestRepository;
    @Autowired   protected   ChatPayLogRepository chatPayLogRepository;
    @Autowired   protected   ChatPayAccountTranslogRepository chatPayAccountTranslogRepository;
    @Autowired   protected   ChatPayReqInMoneyRepository chatPayReqInMoneyRepository;
    @Autowired   protected   IncomeRequestRepository incomeRequestRepository;
    @Autowired   protected   BlackListService blackListService;
    @Autowired   protected   AccountService accountService;
    @Autowired   protected   ChatPayValidateServer chatPayValidate;
    @Autowired   protected   RoomHandlerAdapter roomHandlerAdapter;



    /**
     *  生成收款备注码， 格式：6位随机，2天内不重复,或使用后删除重复使用
     */
    protected String genRemarkNum(String orderNumber){
        return  randomUtil.genRemarkNum(orderNumber);
    }



    /**
     * 通过oid号获取,盘口id,区域
     */
    protected BizHandicap getBizHandicapByOid(String oid){
        return   oidAndLevelServices.getBizHandicapByOid(oid);
    }

    /**
     * 通过盘口id,层级编码，获取层级（内/外层）
     */
    public BizLevel getBizLevelByOidAndLevelCode(String oid, String levelCode){
        return oidAndLevelServices.getBizLevelByOidAndLevelCode(oid, levelCode);
    }


    /**
     * 出款单号加上OID盘口号
     */
    public static String orderNumberAppendOid(String ptOutMoneyOrderNumber,String oid){
        if(StringUtils.isNotBlank(ptOutMoneyOrderNumber) ){
           return String.format(outMoneyOrderNumberFormater,ptOutMoneyOrderNumber,oid);
        }
        log.error("[聊天室支付]-平台出款单号错误，无法添加OID盘口。订单号：{}",ptOutMoneyOrderNumber);
        throw new RuntimeException("出款单号错误");
    }


    /**
     * 还原出款订单号-去掉盘口号再通知平台
     */
    public static   String restoreOutMoneyOrderNumber(String crkOutMoneyOrderNumber){
        if(StringUtils.isNotBlank(crkOutMoneyOrderNumber) && crkOutMoneyOrderNumber.contains("O")){
              return   StringUtils.substringBeforeLast(crkOutMoneyOrderNumber,"O");
        }
        log.error("[聊天室支付]-无法还原平台订单号，出入款订单号：{}",crkOutMoneyOrderNumber);
        throw new RuntimeException("出款单号错误");
    }


    /**
     * 聊天室支付 获取支付宝出款配置
     */
    protected AliOutConfig getAliOutConfig(){
      return  chatPayConfigService.getAliOutConfig();
    }


    /**
     * 聊天室支付 获取支付宝入款配置
     */
    protected AliIncomeConfig geAliIncomeConfig(){
        return  chatPayConfigService.getAliIncomeConfig();
    }


    /**
     * 生成创建房间参数-出款单信息+盘口
     */
    protected ReqCreateRoom genRoomByBizOutwardRequest(BizOutwardRequest bizOutwardRequest, BizHandicap handicap){
        return new ReqCreateRoom(bizOutwardRequest,handicap);
    }


    /**
     * 生成 获取业务ID参数-请求出款单[会员]+盘口
     * @return
     */
    protected ReqGetBizIdWithOutInfo genMemberReqGetBizIdWithOutInfoByOutwardRequest(BizOutwardRequest bizOutwardRequest,BizHandicap handicap){
      return   new ReqGetBizIdWithOutInfo(bizOutwardRequest, handicap, 0, getAliOutConfig());
    }


    /**
     * 生成 获取业务ID参数-请求入款单[会员]+盘口
     * @return
     */
    protected ReqGetBizIdWithIncomeInfo genMemberReqGetBizIdWithIncomeInfoWithOutInfoByIncomeRequest(BizIncomeRequest bizIncomeRequest, BizHandicap handicap,String remarkCode){
        return   new ReqGetBizIdWithIncomeInfo(bizIncomeRequest, handicap, 0, geAliIncomeConfig(),remarkCode);
    }





    /**
     * 生成 获取token参数
     */
    protected ReqCreateToken  genCreateToken(String roomNum,BizOutwardRequest bizOutwardRequest,String bizId){
        if(null!=bizOutwardRequest){
            return new ReqCreateToken(roomNum,bizOutwardRequest.getMemberCode(),bizId);
        }
        throw new RuntimeException("内部错误：生成 获取token参数");
    }


    /**
     * 生成 获取token参数
     */
    protected ReqCreateToken  genCreateToken(String roomNum, BizIncomeRequest bizIncomeRequest,String bizId){
        if(null!=bizIncomeRequest){
            return new ReqCreateToken(roomNum,bizIncomeRequest.getMemberCode(),bizId);
        }
        throw new RuntimeException("内部错误：生成 获取token参数");
    }




    /**
     * 生成 返回平台加入房间需要的信息
     */
    protected Map<String, String> genPtLoginRoomInfoResult(String[] tokenAndRoomUrl, String roomNum, String bizId) {
        Map<String, String> loginRoomInfo = new HashMap<String, String>() {{
            put("token", tokenAndRoomUrl[0]);
            put("roomUrl", tokenAndRoomUrl[1]);
            put("roomNum", roomNum);
            put("bizId", bizId);
        }};
        return loginRoomInfo;
    }




}
