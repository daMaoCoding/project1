package com.xinbo.fundstransfer.service.impl.activity;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.xinbo.fundstransfer.AppProperties;
import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.domain.entity.activity.BizFlwActivitySyn;
import com.xinbo.fundstransfer.domain.repository.activity.RebateActivitySynRepository;
import com.xinbo.fundstransfer.restful.v3.Base3Common;
import com.xinbo.fundstransfer.restful.v3.pojo.ReqV3RateItem;
import com.xinbo.fundstransfer.service.RebateActivitySynService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.xinbo.fundstransfer.domain.enums.ActivityEnums;
import com.xinbo.fundstransfer.domain.enums.ActivityEnums.ActivityStatus;
import com.xinbo.fundstransfer.domain.enums.ActivityEnums.ActivityAllowWithdrawal;
import com.xinbo.fundstransfer.domain.enums.ActivityEnums.ActivityAllowUseCommissions;


import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * ************************
 * 返利网活动同步
 * @author tony
 */
@Service
@Slf4j
public class RebateActivitySynServiceImpl extends Base3Common implements RebateActivitySynService {
    private static Cache<Object, Double> cache = CacheBuilder.newBuilder().maximumSize(20000) .expireAfterWrite(1, TimeUnit.HOURS).build();

    @Autowired  private RebateActivitySynRepository repository;
    @Autowired  private AppProperties appProperties;
    private String tokenCheckFormatStr="%s%s%s%s" ;
    private String TAG = "[返利网活动同步]";

     private static final SimpleResponseData  ERROR_ACTIVITY_NAME=  new SimpleResponseData(0, "参数错误，活动名称不正确");
     private static final SimpleResponseData  ERROR_ACTIVITY_NUMBER=  new SimpleResponseData(0, "参数错误，活动编号不正确");
     private static final SimpleResponseData  ERROR_ACTIVITY_START_TIME=   new SimpleResponseData(0, "参数错误，活动开始时间不正确");
     private static final SimpleResponseData  ERROR_ACTIVITY_END_TIME=   new SimpleResponseData(0, "参数错误，活动结束时间不正确");
     private static final SimpleResponseData  ERROR_ACTIVITY_CONDITIONACCOUNTS=   new SimpleResponseData(0, "参数错误，参与活动绑定银行卡数量限制不正确。");
     private static final SimpleResponseData  ERROR_ACTIVITY_CONDITIONMARGIN=   new SimpleResponseData(0, "参数错误，参与活动最低额度限制不正确。");
     private static final SimpleResponseData  ERROR_ACTIVITY_CONDITIONYSF=   new SimpleResponseData(0, "参数错误，参与活动是否需要开通云闪付限制不正确。");
     private static final SimpleResponseData  ERROR_ACTIVITY_TOPMARGIN=   new SimpleResponseData(0, "参数错误，活动任务最终额度不正确");
     private static final SimpleResponseData  ERROR_ACTIVITY_UNKNOW_STATUS=   new SimpleResponseData(0, "参数错误，未知活动状态。");
     private static final SimpleResponseData  ERROR_ACTIVITY_UNKNOW_ALLOWWITHDRAWAL=   new SimpleResponseData(0, "参数错误，未知是否可提现。");
     private static final SimpleResponseData  ERROR_ACTIVITY_UNKNOW_ALLOWUSECOMMISSIONS=   new SimpleResponseData(0, "参数错误，未知是否可以见佣金当额度使用。");
     private static final SimpleResponseData  ERROR_CAN_NOT_EDIT_START_TIME=  new SimpleResponseData(0, "参数错误，进行中活动,不允许修改开始时间。");
     private static final SimpleResponseData  ERROR_CAN_NOT_STOP_ACTIVITY_WITHOUT_ACTIVITY=  new SimpleResponseData(0, "参数错误，活动不存在,无法停止。");
     private static final SimpleResponseData  ERROR_ACTIVITY_DURING_TIME=  new SimpleResponseData(0, "参数错误，活动时间最少1天。");
     private static final SimpleResponseData  ERROR_STOP_ACTIVITY_END_TIME=  new SimpleResponseData(0, "参数错误，活动停止，停止时间不能是未来时间");




    //基本参数验证
    @Override
    public SimpleResponseData reqParamCheck(BizFlwActivitySyn obj) {
        if(obj==null || StringUtils.isBlank(obj.getActivityName())) return ERROR_ACTIVITY_NAME ;
        if(StringUtils.isBlank(obj.getActivityNumber())) return  ERROR_ACTIVITY_NUMBER;
        if(null==obj.getActivityStartTime()) return ERROR_ACTIVITY_START_TIME;
        if(null==obj.getActivityEndTime()) return  ERROR_ACTIVITY_END_TIME;
        if(null==obj.getConditionAccounts() || obj.getConditionAccounts() < -1) return  ERROR_ACTIVITY_CONDITIONACCOUNTS;
        if(null==obj.getConditionMargin() || obj.getConditionMargin().longValue() < -1) return  ERROR_ACTIVITY_CONDITIONMARGIN;
        if(null==obj.getConditionYsf() || obj.getConditionYsf().longValue() < -1) return  ERROR_ACTIVITY_CONDITIONYSF;
        if( obj.getActivityEndTime().getTime()<System.currentTimeMillis() || obj.getActivityEndTime().getTime()<=obj.getActivityStartTime().getTime()) return  ERROR_ACTIVITY_END_TIME;
        if(null==obj.getTopMargin() ||obj.getTopMargin().longValue()<0L || obj.getTmpMargin().longValue()<0L) return  ERROR_ACTIVITY_TOPMARGIN;
        if( obj.getActivityStatusEnum() !=ActivityStatus.CANCEL &&  obj.getActivityStatusEnum()!=ActivityEnums.ActivityStatus.OK) return ERROR_ACTIVITY_UNKNOW_STATUS ;
        if(obj.getAllowWithdrawalEnum() !=ActivityAllowWithdrawal.YES &&  obj.getAllowWithdrawalEnum() !=ActivityAllowWithdrawal.NO) return  ERROR_ACTIVITY_UNKNOW_ALLOWWITHDRAWAL;
       //if( obj.getActivityStatusEnum() ==ActivityStatus.CANCEL && obj.getActivityEndTime().getTime()>System.currentTimeMillis()) return   ERROR_STOP_ACTIVITY_END_TIME;
        if(obj.getAllowUseCommissionsEnum() != ActivityAllowUseCommissions.YES &&  obj.getAllowUseCommissionsEnum() !=ActivityAllowUseCommissions.NO) return  ERROR_ACTIVITY_UNKNOW_ALLOWUSECOMMISSIONS;



        if (null==obj.getGiftMarginRuleObj() || obj.getGiftMarginRuleObj().stream().filter(p -> p.getAmount() < 0 || p.getRate() < 0 || p.getRate() > 10).count() > 0) return  ERROR_RATE_AMOUNT_RATE_ERROR;
        Set<Float> amountSet = new HashSet<>();
        for (ReqV3RateItem item : obj.getGiftMarginRuleObj()) {
            if (amountSet.contains(item.getAmount()))    return ERROR_RATE_SAME_AMOUNT;
            amountSet.add(item.getAmount());
        }
        if(!reqTokenCheck(obj)) return  ERROR_TOKEN_INVALID;
        return null;
    }


    //逻辑参数验证，无需考虑编号重复，已有则更新。
    @Override
    public SimpleResponseData reqLogicCheck(BizFlwActivitySyn obj) {
        List<BizFlwActivitySyn> activitys = repository.findByActivityNumber(obj.getActivityNumber());
        if(activitys!=null && activitys.size()==1){//修改活动
            BizFlwActivitySyn bizFlwActivitySynDB = activitys.get(0);
            if(System.currentTimeMillis()>=bizFlwActivitySynDB.getActivityStartTime().getTime() && bizFlwActivitySynDB.getActivityStartTime().getTime()!=obj.getActivityStartTime().getTime())
                return  ERROR_CAN_NOT_EDIT_START_TIME;
        }else{//新增活动验证
            if(obj.getActivityStatusEnum()==ActivityEnums.ActivityStatus.CANCEL)
                return  ERROR_CAN_NOT_STOP_ACTIVITY_WITHOUT_ACTIVITY;
            if(null==obj.getActivityStartTime()  ||obj.getActivityStartTime().getTime() <= System.currentTimeMillis() )
                return  ERROR_ACTIVITY_START_TIME;
        }
        if(obj.getActivityEndTime().getTime()-obj.getActivityStartTime().getTime()<86400000) return  ERROR_ACTIVITY_DURING_TIME;
        return null;
    }



    //返利网活动入库
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BizFlwActivitySyn synActive(BizFlwActivitySyn bizFlwActivitySyn, Date now) {
        List<BizFlwActivitySyn> activitys = repository.findByActivityNumber(bizFlwActivitySyn.getActivityNumber());
        //新增正常活动
        if(CollectionUtils.isEmpty(activitys) && bizFlwActivitySyn.getActivityStatusEnum()==ActivityStatus.OK){
            bizFlwActivitySyn.setCreateTime(now);
            bizFlwActivitySyn.setUpdateTime(now);
            bizFlwActivitySyn.setActivityNumber(bizFlwActivitySyn.getActivityNumber().trim());
            return repository.saveAndFlush(bizFlwActivitySyn);
        }
        //修改正常活动
        if(CollectionUtils.isNotEmpty(activitys) && activitys.size()==1 && bizFlwActivitySyn.getActivityStatusEnum()==ActivityStatus.OK){
            bizFlwActivitySyn.setId(activitys.get(0).getId());
            bizFlwActivitySyn.setCreateTime(activitys.get(0).getCreateTime());
            bizFlwActivitySyn.setUpdateTime(now);
            bizFlwActivitySyn.setActivityNumber(bizFlwActivitySyn.getActivityNumber().trim());
            return repository.saveAndFlush(bizFlwActivitySyn);
        }
        //取消活动
        if(CollectionUtils.isNotEmpty(activitys)  && activitys.size()==1 && bizFlwActivitySyn.getActivityStatusEnum()==ActivityStatus.CANCEL){
            //修改活动表
            bizFlwActivitySyn.setId(activitys.get(0).getId());
            bizFlwActivitySyn.setCreateTime(activitys.get(0).getCreateTime());
            bizFlwActivitySyn.setUpdateTime(now);
            bizFlwActivitySyn.setActivityNumber(bizFlwActivitySyn.getActivityNumber().trim());
            bizFlwActivitySyn.setActivityStatus(ActivityEnums.ActivityStatus.CANCEL.getNum());
            bizFlwActivitySyn.setActivityEndTime(new Date());

            //修改more表，此操作由统计佣金做，统计时如果是系统取消活动，当日佣金正常结算给兼职，如果兼职自己退出活动，则不给今日活动佣金。
            return repository.saveAndFlush(bizFlwActivitySyn);
        }
        return null;
    }



    /**
     * 查找目前进行中的活动
     */
    @Override
    public  List<BizFlwActivitySyn> findAvailableActivity(){
        return repository.findAvailableActivity();
    }




    /**
     * Token字符串验证
     * activityName+activityStartTime+activityEndTime+密钥
     */
    private boolean reqTokenCheck(BizFlwActivitySyn obj) {
        try {
            if(obj!=null && StringUtils.isNotBlank(obj.getToken())){
                return ActivityUtil.reqTokenCheck(tokenCheckFormatStr,obj.getToken(),obj.getActivityName(),obj.getActivityStartTime().getTime(),obj.getActivityEndTime().getTime(),appProperties.getRebatesalt());
            }
        }catch (Exception e){
            log.error("{},token生成失败:{}",TAG,e.getMessage(),e);
            return false;
        }
        return false;
    }


}
