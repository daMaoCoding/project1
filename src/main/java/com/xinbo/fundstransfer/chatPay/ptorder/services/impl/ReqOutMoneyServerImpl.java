package com.xinbo.fundstransfer.chatPay.ptorder.services.impl;

import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.chatPay.commons.enums.ReqChannelType;
import com.xinbo.fundstransfer.chatPay.commons.services.AbstractChatPayBaseServer;
import com.xinbo.fundstransfer.chatPay.flwsynzfbwx.entity.BizQrInfo;
import com.xinbo.fundstransfer.chatPay.ptorder.reqVo.ReqOutMoney;
import com.xinbo.fundstransfer.chatPay.ptorder.services.ReqOutMoneyServer;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.BizLevel;
import com.xinbo.fundstransfer.domain.entity.BizOutwardRequest;
import com.xinbo.fundstransfer.domain.enums.OutwardRequestStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

/**
 * ************************
 * 聊天室支付 出款
 * @author tony
 */
@Slf4j
@Service
public class ReqOutMoneyServerImpl extends AbstractChatPayBaseServer implements ReqOutMoneyServer {

    /**
     * 聊天室支付 出款
     */
    @Override
    public Map<String, String> reqOutMoney(ReqOutMoney reqOutMoney) {

        //0.验证收款二维码
        BizQrInfo bizQrInfo = chatPayValidate.valQrInfo(reqOutMoney.getQrId());

        //1.根据配置验证出款单信息 todo：//金额等，日出款总额等
        chatPayValidate.valReqOutMoney(reqOutMoney,getAliOutConfig(),bizQrInfo);

        //2.生成出款单&验证
        String remarkNum                    =  genRemarkNum(reqOutMoney.getOrderNo()); //备注码
        BizHandicap handicap                =  getBizHandicapByOid(reqOutMoney.getHandicap()); //盘口
        BizLevel level                      =  getBizLevelByOidAndLevelCode(reqOutMoney.getHandicap(), reqOutMoney.getuLeavel()); //层级
        BizOutwardRequest bizOutwardRequest =  converReqOutMoneyToBizOutwardRequest(reqOutMoney,remarkNum,level,handicap,bizQrInfo);  //构造出款单
        chatPayValidate.valReqOutMoney(remarkNum,handicap,level,bizOutwardRequest);  //验证构造后参数

        //3.1.创建房间（oid,单号，通道类型）
        String roomNum = callCenterServiceApiStatic.createRoom(genRoomByBizOutwardRequest(bizOutwardRequest,handicap));

        //3.2.获取业务id (订单信息)
        String bizId = callCenterServiceApiStatic.getBizIdWithOutInfo(genMemberReqGetBizIdWithOutInfoByOutwardRequest(bizOutwardRequest,handicap));

        //3.3.获取token（房间号+Uid +bizId)
        String[] tokenAndRoomUrl = callCenterServiceApiStatic.createTokenAndRoomUrl(genCreateToken(roomNum,bizOutwardRequest, bizId));
        chatPayValidate.valCallCenterRoomInfoResult(roomNum,bizId,tokenAndRoomUrl);

        //4.保存数据
        roomHandlerAdapter.handleOutMoneyOrder(bizOutwardRequest, handicap,level,remarkNum,getAliOutConfig());

        //5.返回平台
        return genPtLoginRoomInfoResult(tokenAndRoomUrl,roomNum,  bizId);
    }





    /**
     * 转换出款请求->出款实体
     */
    private BizOutwardRequest converReqOutMoneyToBizOutwardRequest(ReqOutMoney reqOutMoney, String remarkNum, BizLevel level, BizHandicap handicap, BizQrInfo bizQrInfo) {
        BizOutwardRequest bizOutwardRequest = new BizOutwardRequest();
        bizOutwardRequest.setOrderNo(orderNumberAppendOid(reqOutMoney.getOrderNo(),reqOutMoney.getHandicap()));  //单号（注意：会在平台订单号后面增加"O"+盘口号，如果：）
        bizOutwardRequest.setHandicap(handicap.getId());         //盘口
        bizOutwardRequest.setMember(reqOutMoney.getuName());     //会员账号
        bizOutwardRequest.setAmount(reqOutMoney.getAmount());
        bizOutwardRequest.setCreateTime(new Date(reqOutMoney.getOrderCreateTime()));  //订单时间
        bizOutwardRequest.setStatus(OutwardRequestStatus.ChatPayWaitMatch.getStatus()); //默认审核通过，不需要审核
        bizOutwardRequest.setRemark(CommonUtils.genRemark("","平台请求出款",new Date(),sysOperater)); //备注
        bizOutwardRequest.setToAccount(bizQrInfo.getAccount());  //收款账户
        bizOutwardRequest.setToAccountOwner(bizQrInfo.getName()); //收款人
        bizOutwardRequest.setLevel(level==null?null:level.getCurrSysLevel()); //层级
        bizOutwardRequest.setMemberCode(reqOutMoney.getuId()); //会员id
        bizOutwardRequest.setToAccountBank(ReqChannelType.getAccountType(reqOutMoney.getChannelType()).getMsg()); //出款银行类型
        bizOutwardRequest.setOutIp(reqOutMoney.getuIp()); //出款ip
        bizOutwardRequest.setChatPayBzm(remarkNum);  //备注码
        bizOutwardRequest.setChatPayQrContent(bizQrInfo.getQrContent()); //二维码内容
        bizOutwardRequest.setType(ReqChannelType.getOutwardType(reqOutMoney.getChannelType()).getNum()); //出款类型，银行卡，微信，支付宝...
        return bizOutwardRequest;
    }
}
