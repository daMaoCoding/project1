package com.xinbo.fundstransfer.chatPay.callCenter.services.impl;

import com.xinbo.fundstransfer.chatPay.callCenter.redis.ChatPayRebateUserRedisStatus;
import com.xinbo.fundstransfer.chatPay.callCenter.reqVo.ReqRebateUserOut;
import com.xinbo.fundstransfer.chatPay.callCenter.reqVo.ReqRebateUserReady;
import com.xinbo.fundstransfer.chatPay.callCenter.services.RebateUserStatusServices;
import com.xinbo.fundstransfer.chatPay.commons.enums.ChatPayRebateUserJobTypeEnum;
import com.xinbo.fundstransfer.chatPay.commons.enums.ChatPayRebateUserStatusEnum;
import com.xinbo.fundstransfer.chatPay.pthttpapi.reqVo.ChangeUserChatPayBlacklistBack;
import com.xinbo.fundstransfer.component.net.http.restTemplate.PlatformServiceApiStatic;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizAccountMore;
import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import com.xinbo.fundstransfer.service.AccountMoreService;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.RedisService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.jws.Oneway;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ************************
 * 聊天室 支付兼职状态
 * @author tony
 */
@Service
public class RebateUserStatusServicesImpl implements RebateUserStatusServices {

    @Autowired    AccountService accountService;
    @Autowired    AccountMoreService accountMoreService;
    @Autowired    ChatPayBizAccountMoreServices bizAccountMoreUtil;
    @Autowired    PlatformServiceApiStatic platformServiceApiStatic;
    @Autowired    RedisService redisService;

    /**
     * 缓存兼职任务状态
     */
    private static final String ChatPayRebateUserRedisStatus = "ChatPayRebateUserRedisStatus";


    /**
     * 返利网兼职准备接任务
     */
    @Override
    public boolean p2pRebateUserReady(ReqRebateUserReady reqRebateUserReady) {
        //0.兼职处理任务类型
        ChatPayRebateUserJobTypeEnum jobType = ChatPayRebateUserJobTypeEnum.getByNumber(reqRebateUserReady.getJobType());
        if(jobType==ChatPayRebateUserJobTypeEnum.UNKNOWENUM) throw new RuntimeException("未知预处理任务类型");

        //1.找到关联的微信/支付宝收款二维码
        BizAccountMore bizAccountMore = accountMoreService.getFromByUid(reqRebateUserReady.getUid());
        if(bizAccountMore==null || StringUtils.isBlank(bizAccountMore.getAccounts())) throw new RuntimeException("无绑定账号");

        //2.查找验证通过，并没停用的微信/支付宝 账号信息
        List<BizAccount> accounts  =  bizAccountMoreUtil.findUseAbleAccountsByQrType(bizAccountMore, null);
        if(CollectionUtils.isEmpty(accounts))  throw new RuntimeException("无绑定验证通过并可用的收款码信息");

        //3.过滤account账号状态为 在用/可用
        accounts = accounts.stream().filter(x-> x.getStatus()== AccountStatus.Normal.getStatus() ||x.getStatus()== AccountStatus.Enabled.getStatus() ).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(accounts))  throw new RuntimeException("无绑定在用/可用的收款码信息");


        //4.todo://如果老板要求兼职不能同时处理 支付宝和微信，再此处验证，目前兼职绑定账号全部上线。

        //5.缓存账户状态
        HashOperations<String, String, ChatPayRebateUserRedisStatus> hashOperations = redisService.getHashOperations();

        hashOperations.put(ChatPayRebateUserRedisStatus,reqRebateUserReady.getUid(), new ChatPayRebateUserRedisStatus(accounts, ChatPayRebateUserStatusEnum.ON_LINE, jobType,System.currentTimeMillis()) );
        //ChatPayRebateUserRedisStatus chatPayRebateUserRedisStatus = hashOperations.get(ChatPayRebateUserRedisStatus, "000");
        //System.out.println(chatPayRebateUserRedisStatus);
        return true;
    }


    /**
     * 兼职退出聊天室支付
     */
    @Override
    public boolean p2pRebateUserOut(ReqRebateUserOut reqRebateUserOut) {
        HashOperations<String, String, ChatPayRebateUserRedisStatus> hashOperations = redisService.getHashOperations();
        if(hashOperations.hasKey(ChatPayRebateUserRedisStatus,reqRebateUserOut.getUid())){
            hashOperations.delete(ChatPayRebateUserRedisStatus,reqRebateUserOut.getUid());
            //todo:如果要求，如果被客服踢出去，或者不点按钮，记录违规次数在此记录，如果违规次数达到，发送平台拉黑。目前兼职没有限制。
            return true;
        }
        return true;
    }


    /**
     * 修改会员聊天室通道黑名单状态[拉黑/解除拉黑]
     */
    @Override
    public boolean changeUserChatPayBlacklist(ChangeUserChatPayBlacklistBack changeUserChatPayBlacklistBack) {
        int i = platformServiceApiStatic.changeUserChatPayBlacklist(changeUserChatPayBlacklistBack);
        if(i==1) return true;
        return false;
    }
}
