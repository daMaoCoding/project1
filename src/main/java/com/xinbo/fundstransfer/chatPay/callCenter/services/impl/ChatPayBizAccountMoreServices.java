package com.xinbo.fundstransfer.chatPay.callCenter.services.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.xinbo.fundstransfer.chatPay.callCenter.repository.ChatPayAccountRepository;
import com.xinbo.fundstransfer.chatPay.commons.enums.BizQrInfoEnums;
import com.xinbo.fundstransfer.chatPay.flwsynzfbwx.entity.BizQrInfo;
import com.xinbo.fundstransfer.chatPay.flwsynzfbwx.services.BizQrInfoServer;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizAccountMore;
import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.service.AccountService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ************************
 *
 * @author tony
 */
@Component
public class ChatPayBizAccountMoreServices {

    @Autowired  AccountService accountService;
    @Autowired  ChatPayAccountRepository chatPayAccountRepository;
    @Autowired  BizQrInfoServer bizQrInfoServer;

    /**
     * 解析 bizAccountMore 获取绑定Account ID
     */
    public Integer []  parseAccountIds(final BizAccountMore bizAccountMore){
        if(null!=bizAccountMore && StringUtils.isNotBlank(bizAccountMore.getAccounts())){
            List<Integer> accountIds = Stream.of(StringUtils.trimToEmpty(bizAccountMore.getAccounts()).split(",")).filter(StringUtils::isNumeric).map(Integer::valueOf).collect(Collectors.toList());
            if(!CollectionUtils.isEmpty(accountIds)){
                return accountIds.toArray(new Integer[accountIds.size()]);
            }
            return  new Integer[0];
        }
        return  new Integer[0];
    }


    /**
     * qrinfoType转 accountType
     */
    private Integer []  convertQrTypeToAccountType(BizQrInfoEnums.QrType qrType){
        Integer [] accountType = new Integer[0];
        if(null==qrType){
            accountType = new Integer[]{AccountType.InAccountFlwZfb.getTypeId(),AccountType.InAccountFlwWx.getTypeId()};
        }
        if(qrType==BizQrInfoEnums.QrType.WX){
            accountType = new Integer[]{AccountType.InAccountFlwWx.getTypeId()};
        }
        if(qrType==BizQrInfoEnums.QrType.ZFB){
            accountType = new Integer[]{AccountType.InAccountFlwZfb.getTypeId()};
        }
        return accountType;
    }




    /**
     * 通过二维码类型查询Account信息
     * BizQrInfoEnums.QrType  为空查询所有
     */
    public List<BizAccount> findUseAbleAccountsByQrType(final BizAccountMore bizAccountMore,BizQrInfoEnums.QrType qrType){
        List<BizAccount> useAbleAccounts = Lists.newArrayList();
        Integer [] accountIds = parseAccountIds(bizAccountMore);
        Integer [] accountTypes = convertQrTypeToAccountType(qrType);

        List<BizAccount> accounts = chatPayAccountRepository.findAllByIdInAndTypeIn(accountIds,accountTypes);
        if(!CollectionUtils.isEmpty(accounts)){
            HashSet<String> qrContentsSet = Sets.newHashSet();
            accounts.stream().forEach(x->qrContentsSet.add(x.getQrContent()));
            String[] qrContents = qrContentsSet.toArray(new String[qrContentsSet.size()]);
            List<BizQrInfo> useAbleQrInfo = bizQrInfoServer.findAllByUidAndQrStatusAndValQrStatusAndQrContentIn(bizAccountMore.getUid(), BizQrInfoEnums.QrStatus.RUNNING.getNum(), BizQrInfoEnums.ValQrStatus.SUCCESS.getNum(), qrContents);
            useAbleQrInfo.forEach(x->
                accounts.forEach(y->{
                    if(y.getQrContent().equalsIgnoreCase(x.getQrContent())){
                        useAbleAccounts.add(y);
                    }
                }));
        }
        return useAbleAccounts;
    }


}
