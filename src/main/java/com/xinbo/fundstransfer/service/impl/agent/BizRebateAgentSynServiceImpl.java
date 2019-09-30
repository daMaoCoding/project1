package com.xinbo.fundstransfer.service.impl.agent;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.xinbo.fundstransfer.AppProperties;
import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.domain.entity.BizAccountMore;
import com.xinbo.fundstransfer.domain.entity.agent.BizRebateAgentSyn;
import com.xinbo.fundstransfer.domain.repository.AccountMoreRepository;
import com.xinbo.fundstransfer.domain.repository.agent.BizRebateAgentSynRepository;
import com.xinbo.fundstransfer.service.BizRebateAgentSynService;
import com.xinbo.fundstransfer.service.impl.activity.ActivityUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * ************************
 *
 * @author tony
 */
@Service
@Slf4j
public class BizRebateAgentSynServiceImpl implements BizRebateAgentSynService {
    private static Cache<String, BizRebateAgentSyn.AgentSubUsers > cache = CacheBuilder.newBuilder().maximumSize(20000) .expireAfterWrite(15, TimeUnit.DAYS).build();
    private static final String TAG = "[代理同步]" ;
    private static final String tokenCheckFormatStr="%s%s%s%s" ;
    @Autowired  private  AccountMoreRepository bizAccountMoreRepository;
    @Autowired  private  BizRebateAgentSynRepository bizRebateAgentSynRepository;
    @Autowired  private AppProperties appProperties;



    /**
     * 检查请求参数
     */
    @Override
    public SimpleResponseData checkReqParam(BizRebateAgentSyn bizRebateAgentReq) {
        try {
            if(null==bizRebateAgentReq || StringUtils.isBlank(bizRebateAgentReq.getUid())) throw new RuntimeException("请检查母账号");
            if(!reqTokenCheck(bizRebateAgentReq)) throw new RuntimeException("请检查Token");
            Set<String> checkReqUids = getCheckReqUids(bizRebateAgentReq);
            Set<String> checkDbUids = checkUidsExistence(checkReqUids);
            if(bizRebateAgentReq.getAgentSubUserIds()!=null && !CollectionUtils.isEmpty(bizRebateAgentReq.getAgentSubUserIds()) && bizRebateAgentReq.getAgentSubUserIds().stream().filter(x->{
                if(null!=x && StringUtils.isNotBlank(x.getUid()) &&  x.getUid().equalsIgnoreCase(bizRebateAgentReq.getUid())) return true;
                return false;
            }).count()!=0)
                throw new RuntimeException(MessageFormat.format("子账号中不应该包含母账号,UID:{0}",bizRebateAgentReq.getUid()));
            //查找差集
            Sets.SetView<String> difference = Sets.difference(checkReqUids, checkDbUids);
            if(CollectionUtils.isEmpty(difference) ) return null;
            if(difference.contains(bizRebateAgentReq.getUid())) return new SimpleResponseData(2,MessageFormat.format("{0},出错:母账号-UID:{1},不存在，请同步。",TAG,bizRebateAgentReq.getUid()));
            return new SimpleResponseData(3,MessageFormat.format("{0},出错:子账号-UID:{1},不存在，请同步。",TAG,Sets.difference(difference,Collections.singleton(bizRebateAgentReq.getUid())) ));
        }catch (Exception e){
            String msg = MessageFormat.format("{0},出错:{1}。",TAG,e.getMessage());
            log.error(msg,e);
            return new SimpleResponseData(0,msg);
        }
    }


    /**
     * 同步代理信息-需检查参数后保存/更新
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syn(BizRebateAgentSyn bizRebateAgentReq) {
        //saveOrUpdate
        BizRebateAgentSyn bizRebateAgentDB =  getByUid(bizRebateAgentReq);
        synDb(bizRebateAgentDB,bizRebateAgentReq);
    }


    /**
     * 同步代理信息入库
     */
    private void synDb( BizRebateAgentSyn bizRebateAgentDB,BizRebateAgentSyn bizRebateAgentReq){
        if(null==bizRebateAgentDB){
            bizRebateAgentReq.setCreatTime(new Date());
        }else{
            bizRebateAgentReq.setId(bizRebateAgentDB.getId());
            bizRebateAgentReq.setCreatTime(bizRebateAgentDB.getCreatTime());
        }
        bizRebateAgentReq.setUpdateTime(new Date());
        bizRebateAgentSynRepository.saveAndFlush(bizRebateAgentReq);
        BizAccountMore accountMore =  bizAccountMoreRepository.findByUid(bizRebateAgentReq.getUid());
        accountMore.setAgent(bizRebateAgentReq.getIsAgent());
        bizAccountMoreRepository.saveAndFlush(accountMore);
    }



    /**
     * 查找数据库已存在的uids
     */
    private Set<String> checkUidsExistence(Set<String> uidsReq) {
        Set<String> exisUids = Sets.newHashSet();
        if(!CollectionUtils.isEmpty(uidsReq)){
            exisUids = bizRebateAgentSynRepository.checkUidsExistence(uidsReq);
        }
        return exisUids;
    }




    /**
     * 获取请求的全部uid
     */
    private Set<String> getCheckReqUids(BizRebateAgentSyn bizRebateAgentReq) {
        if(bizRebateAgentReq!=null){
            Set<String> reqUids = Sets.newHashSet(StringUtils.trimToNull(bizRebateAgentReq.getUid()));
            Set<String> subUids =  bizRebateAgentReq.getAgentSubUserIds().stream().map(BizRebateAgentSyn.AgentSubUsers::getUid).collect(Collectors.toSet());
            reqUids.addAll(subUids); Iterables.removeIf(reqUids, StringUtils::isBlank);  //uidsReq.removeIf(Predicates.isNull());
            return reqUids;
        }
        return Sets.newHashSet();
    }




    /**
     * 通过uid查找数据库代理关联信息
     */
    private BizRebateAgentSyn getByUid(BizRebateAgentSyn bizRebateAgentReq) {
        List<BizRebateAgentSyn> bizRebateAgentSynList = bizRebateAgentSynRepository.findByUid(bizRebateAgentReq.getUid());
        if(CollectionUtils.isEmpty(bizRebateAgentSynList)) return null;
        if(bizRebateAgentSynList.size()==1) return bizRebateAgentSynList.get(0);
        String msg =  MessageFormat.format("同步代理信息查询返回不正确，存在多条,UID：{0}",bizRebateAgentReq.getUid());
        log.error(msg);
        throw new RuntimeException(msg);
    }





    /**
     * Token字符串验证
     */
    private boolean reqTokenCheck(BizRebateAgentSyn reqObj) {
        try {
            if(reqObj==null || StringUtils.isBlank(reqObj.getToken())) return false;
            if(reqObj!=null && StringUtils.isNotBlank(reqObj.getToken())){
                return ActivityUtil.reqTokenCheck(tokenCheckFormatStr,reqObj.getToken(),reqObj.getUid(),reqObj.getIsAgent()?SimpleResponseData.SUCCESS:SimpleResponseData.ERROR,reqObj.getAgentType(),appProperties.getRebatesalt());
                //return true;
            }
        }catch (Exception e){
            log.error("{},token生成失败:{}",TAG,e.getMessage(),e);
            return false;
        }
        return false;
    }




}
