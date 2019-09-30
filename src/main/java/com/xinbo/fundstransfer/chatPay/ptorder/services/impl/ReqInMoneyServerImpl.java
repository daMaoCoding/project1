package com.xinbo.fundstransfer.chatPay.ptorder.services.impl;

import com.alibaba.fastjson.JSON;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.chatPay.commons.enums.ReqChannelType;
import com.xinbo.fundstransfer.chatPay.commons.services.AbstractChatPayBaseServer;
import com.xinbo.fundstransfer.chatPay.ptorder.reqVo.ReqInMoney;
import com.xinbo.fundstransfer.chatPay.ptorder.services.ReqInMoneyServer;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.BizIncomeRequest;
import com.xinbo.fundstransfer.domain.entity.BizLevel;
import com.xinbo.fundstransfer.domain.enums.IncomeRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.*;

/**
 * ************************
 *  聊天室支付 入款
 * @author tony
 */
@Slf4j
@Service
public class ReqInMoneyServerImpl extends AbstractChatPayBaseServer implements ReqInMoneyServer {

    /**
     * 聊天室支付-入款
     */
    @Override
    public Map<String, String> reqInMoney(ReqInMoney reqInMoney,long startTimeStamp) {
        final  ExecutorService executor = Executors.newSingleThreadExecutor();
        long totalTimeOut = geAliIncomeConfig().getTotalMatchSecondForAliIn() * 1000L - 500L;   //预留500毫秒平台通信，入款配置的最大超时时间-500毫秒（包含找会员出款单+找兼职拉房间）
        if(totalTimeOut<0) totalTimeOut=Long.MAX_VALUE;  //无意义配置，永不超时
        FutureTask<Map<String, String>> future = new FutureTask(new ReqInMoneyCallable(reqInMoney,totalTimeOut,startTimeStamp));
        executor.submit(future);
        try {
            return  future.get(totalTimeOut, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("[聊天室支付]-[平台入款请求-总控室]，无法找到合适通道，超时：{}毫秒， 异常类型：{}，异常：{}，请求入款信息：{}",totalTimeOut,e.getClass(),e.getMessage(),JSON.toJSONString(reqInMoney));
            throw  new RuntimeException("超时,未找到合适通道,请稍后再试");
        }finally {
            future.cancel(true);
            executor.shutdownNow();
        }
    }


    /**
     * 会员入款处理（总-外部控制超时）
     */
    @Data
    @AllArgsConstructor
    class ReqInMoneyCallable implements Callable<Map<String,String>> {
        private ReqInMoney reqInMoney;
        private long totalTimeOut ;
        private long startTimeStamp ;
        @Override
        public Map<String, String> call() throws Exception {
            //1.根据配置验证入款单信息 todo：//金额等，日出款总额等
            chatPayValidate.valReqInMoney(reqInMoney,geAliIncomeConfig());

            //2.生成入款单&验证
            String remarkNum                  = genRemarkNum(reqInMoney.getOrderNo()); //备注码
            BizHandicap handicap              = getBizHandicapByOid(reqInMoney.getHandicap()); //盘口
            BizLevel level                    = getBizLevelByOidAndLevelCode(reqInMoney.getHandicap(), reqInMoney.getuLeavel()); //层级
            BizIncomeRequest bizIncomeRequest =  converReqInMoneyToBizIncomeRequest(reqInMoney,remarkNum,level,handicap);  //构造入款单
            chatPayValidate.valReqInMoney(remarkNum,handicap,level,bizIncomeRequest);  //验证构造后参数

            //3. 保存入款单
           return  saveMemberInMoneyOrder(bizIncomeRequest,handicap,level,remarkNum,totalTimeOut,startTimeStamp);
        }
    }



    /**
     *  获取房间(oid,单号，通道类型),线程执行，1找房间，2给兼职创建代收
     */
    private Map<String, RLock > findRoomNumber(BizIncomeRequest bizIncomeRequest,long totalTimeOut,long startTimeStamp){
        Map<String, RLock > roomNumberAndLock = null;
        roomNumberAndLock = findMemberOutMoneyRoom(bizIncomeRequest); //查找会员出款单房间&锁住
        if(MapUtils.isEmpty(roomNumberAndLock) || roomNumberAndLock.size()!=1)
            roomNumberAndLock = createRebateUserDaiShouRoom(bizIncomeRequest,totalTimeOut,startTimeStamp);  //创建兼职代收房间
        if(MapUtils.isEmpty(roomNumberAndLock) || roomNumberAndLock.size()!=1){
            throw cannotFindRoom;
        }
        return roomNumberAndLock;
    }




    /**
     * 会员入款处理-匹配会员出款单
     */
    @Data
    @AllArgsConstructor
    class FindMemberOutMoneyRoomCallable implements Callable<Map<String, RLock >> {
        BizIncomeRequest bizIncomeRequest = null;
        long findMemberOutMoneyRoomTimeOut;
        @Override
        public Map<String, RLock > call() throws Exception {
            return roomHandlerAdapter.findMemberOutMoneyRoom(bizIncomeRequest,geAliIncomeConfig(),findMemberOutMoneyRoomTimeOut);
        }
    }




    /**
     * 会员入款处理-拉兼职，给兼职创建房间(兼职代收)
     */
    @Data
    @AllArgsConstructor
    class FindRebateUserDaiShouRoomCallable implements Callable< Map<String, RLock >> {
        BizIncomeRequest bizIncomeRequest = null;
        long rebateUserRoomTimeOut;
        @Override
        public  Map<String, RLock > call() throws Exception {
            return roomHandlerAdapter.findCreateRebateUserDaiShouRoom(bizIncomeRequest,geAliIncomeConfig(),rebateUserRoomTimeOut);
        }
    }




    /**
     * 处理入款单-找会员出款单-房间 (有入款配置时间超时参数) &锁住
     */
    public Map<String, RLock> findMemberOutMoneyRoom(BizIncomeRequest bizIncomeRequest) {
        long findMemberOutMoneyRoomTimeOut = geAliIncomeConfig().getMatchSecondForAliIn() * 1000L;
        if(findMemberOutMoneyRoomTimeOut<0) findMemberOutMoneyRoomTimeOut=Long.MAX_VALUE; //无意义配置，永不超时(匹配兼职永不超时)
        final  ExecutorService executor = Executors.newSingleThreadExecutor();
        FutureTask<Map<String, RLock >> future = new FutureTask(new FindMemberOutMoneyRoomCallable(bizIncomeRequest,findMemberOutMoneyRoomTimeOut));
        executor.submit(future);
        try{
            return  future.get(findMemberOutMoneyRoomTimeOut, TimeUnit.MILLISECONDS);  //会员提交一笔入款，等待【n】秒内提交的会员出款单匹配聊天室
        } catch (Exception e) {
            log.error("[聊天室支付]-[平台入款请求-01-findMemberOutMoneyRoom]，无法找到入款会员对应的[出款会员]订单房间，超时时间：{}毫秒，异常类型：{}，异常：{}，请求入款信息：{}",findMemberOutMoneyRoomTimeOut,e.getClass(),e.getMessage(),JSON.toJSONString(bizIncomeRequest));
        }finally {
            future.cancel(true);
            executor.shutdownNow();
        }
        return null;
    }





    /**
     * 处理入款单-创建兼职代收-(有入款配置时间超时参数)
     */
    private  Map<String, RLock > createRebateUserDaiShouRoom(BizIncomeRequest bizIncomeRequest, long totalTimeOut,long startTimeStamp ) {
        long rebateUserRoomTimeOut = totalTimeOut-(System.currentTimeMillis()-startTimeStamp)-callCenterReservedTimeMillis; //推测超时=总耗时-已耗时(当前时间-开始时间)-预留耗时
        final  ExecutorService executor = Executors.newSingleThreadExecutor();
        FutureTask< Map<String, RLock >> future = null;
        future = new FutureTask( new FindRebateUserDaiShouRoomCallable(bizIncomeRequest,rebateUserRoomTimeOut));
        executor.submit(future);
        try {
            return  future.get(rebateUserRoomTimeOut, TimeUnit.MILLISECONDS);//处理兼职代收(包含-找兼职，锁兼职，创建代收房间）
        } catch (Exception e) {
            log.error("[聊天室支付]-[平台入款请求-02-createRebateUserDaiShouRoom]，无法为入款会员创建[兼职代收]订单房间，超时时间：{}毫秒，异常类型：{}，异常信息：{}，请求入款信息：{}",rebateUserRoomTimeOut,e.getClass(),e.getMessage(),JSON.toJSONString(bizIncomeRequest));
        }finally {
            future.cancel(true);
            executor.shutdownNow();
        }
        return null;
    }



    /**
     * 转换入款请求->入款实体
     */
    private BizIncomeRequest converReqInMoneyToBizIncomeRequest(ReqInMoney reqInMoney,String remarkNum,BizLevel level,BizHandicap handicap) {
        BizIncomeRequest bizIncomeRequest = new BizIncomeRequest();
        bizIncomeRequest.setLevel(level==null?null:level.getCurrSysLevel());  //层级
        bizIncomeRequest.setHandicap(handicap.getId());   //盘口
        bizIncomeRequest.setStatus(IncomeRequestStatus.Matching.getStatus());  //匹配中
        bizIncomeRequest.setAmount(reqInMoney.getAmount());  //金额
        bizIncomeRequest.setCreateTime(new Date(reqInMoney.getOrderCreateTime()));  //创建时间-平台订单创建时间
        bizIncomeRequest.setRemark(CommonUtils.genRemark("","平台请求入款",new Date(),sysOperater)); //备注
        bizIncomeRequest.setType(ReqChannelType.getIncomeType(reqInMoney.getChannelType()).getType()); //入款类型
        bizIncomeRequest.setToAccountBank( ReqChannelType.getAccountType(reqInMoney.getChannelType()).getMsg() ); //收款账号类型
        bizIncomeRequest.setOrderNo(reqInMoney.getOrderNo());  //订单号
        bizIncomeRequest.setMemberUserName(reqInMoney.getuName()); //会员账号
        bizIncomeRequest.setMemberRealName(reqInMoney.getuRealName()); //会员姓名
        bizIncomeRequest.setMemberCode(reqInMoney.getuId()); //会员id,更改int->string保存
        bizIncomeRequest.setChatPayBzm(remarkNum); //备注码
        return bizIncomeRequest;
    }


    /**
     * 锁住房间 & 保存入款单
     */
    protected Map<String, String> saveMemberInMoneyOrder(BizIncomeRequest bizIncomeRequest, BizHandicap handicap, BizLevel level, String remarkNum, long totalTimeOut, long startTimeStamp) {
        boolean isLock = true;  //找到房间既上锁

        //3.1 获取房间(oid,单号，通道类型),线程执行，1找房间，2给兼职创建代收
         Map<String, RLock > roomAndLock = findRoomNumber(bizIncomeRequest,totalTimeOut,startTimeStamp);
         String  roomNum = roomAndLock.keySet().iterator().next();
         RLock   lock    = roomAndLock.get(roomNum);
        try {
            if (isLock) {
                //3.2.获取业务id (订单信息)
                String bizId = callCenterServiceApiStatic.getBizIdWithIncomeInfo(genMemberReqGetBizIdWithIncomeInfoWithOutInfoByIncomeRequest(bizIncomeRequest,handicap,remarkNum));
                //3.3.获取token（房间号+Uid +bizId)
                String[] tokenAndRoomUrl = callCenterServiceApiStatic.createTokenAndRoomUrl(genCreateToken(roomNum,bizIncomeRequest, bizId));
                chatPayValidate.valCallCenterRoomInfoResult(roomNum,bizId,tokenAndRoomUrl);
                //4.保存数据
                roomHandlerAdapter.handleInMoneyOrder(bizIncomeRequest, handicap,level,remarkNum,geAliIncomeConfig());
                //5.返回平台
                return genPtLoginRoomInfoResult(tokenAndRoomUrl,roomNum,  bizId);
            }
        }finally {
            if(isLock)
                lock.unlock();
        }
        throw cannotLookRoom;
    }



}
