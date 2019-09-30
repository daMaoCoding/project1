package com.xinbo.fundstransfer.service.impl.activity;

import com.alibaba.fastjson.JSON;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.xinbo.fundstransfer.AppProperties;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.DynamicSpecifications;
import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizRebateUser;
import com.xinbo.fundstransfer.domain.entity.activity.BizAccountFlwActivity;
import com.xinbo.fundstransfer.domain.entity.BizAccountMore;
import com.xinbo.fundstransfer.domain.entity.activity.BizFlwActivitySyn;
import com.xinbo.fundstransfer.domain.enums.*;
import com.xinbo.fundstransfer.domain.repository.AccountMoreRepository;
import com.xinbo.fundstransfer.domain.repository.RebateUserRepository;
import com.xinbo.fundstransfer.domain.repository.activity.RebateActivitySynRepository;
import com.xinbo.fundstransfer.domain.repository.activity.RebateUserActivityRepository;
import com.xinbo.fundstransfer.restful.v3.Base3Common;
import com.xinbo.fundstransfer.restful.v3.activity.result.UserJoinActivityDataResult;
import com.xinbo.fundstransfer.service.AccountMoreService;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.RebateUserActivityService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

import  com.xinbo.fundstransfer.domain.enums.ActivityEnums.UserActivityStatus;
import  com.xinbo.fundstransfer.domain.enums.ActivityEnums.ActivityStatus;
import  com.xinbo.fundstransfer.domain.enums.ActivityEnums.AccountMoreActivityInStatus;
import  com.xinbo.fundstransfer.domain.enums.ActivityEnums.QuitStatus;
import  com.xinbo.fundstransfer.domain.enums.ActivityEnums.ActivityAllowWithdrawal;
import org.springframework.util.CollectionUtils;


/**
 * ************************
 *  返利网兼职参加活动
 * @author tony
 */
@Service
@Slf4j
public class RebateUserActivityServiceImpl extends Base3Common implements RebateUserActivityService  {
    private static  Cache<String,Set<String>> cache = CacheBuilder.newBuilder().maximumSize(200000) .expireAfterWrite(1, TimeUnit.DAYS).build();
    private static  final String TAG ="[返利网兼职参加活动]";
    private static  final String tokenCheckFormatStr ="%s%s%s%s%s";
    private static  final String AccountsInActivitys_Redis_Key ="AccountsInActivitys";

    @Autowired  private RebateUserActivityRepository repository;
    @Autowired  private AppProperties appProperties;
    @Autowired  private AccountMoreService accountMoreService;
    @Autowired  private AccountService accountService;
    @Autowired  private RebateActivitySynRepository activitySynRepository;
    @Autowired  private RebateUserRepository rebateUserRepository;
    @Autowired  private StringRedisTemplate stringRedisTemplate;
    @Autowired  private AccountMoreRepository accountMoreRepository;

    public static final SimpleResponseData ERROR_ACTIVITY_USERSTATUS = new SimpleResponseData(0, "参数错误，参与活动状态错误，只支持参与/退出");
    public static final SimpleResponseData ERROR_ACTIVITY_USERID = new SimpleResponseData(0, "参数错误，参数错误，兼职id不正确");
    public static final SimpleResponseData ERROR_ACTIVITY_NUMBER = new SimpleResponseData(0, "参数错误，活动编号不正确。");
    public static final SimpleResponseData ERROR_ACTIVITY_USER_STARTTIME= new SimpleResponseData(0, "参数错误，参与活动时间不正确。");
    public static final SimpleResponseData ERROR_ACTIVITY_USER_ENDTIME = new SimpleResponseData(0, "参数错误，退出时间不正确。");
    public static final SimpleResponseData ERROR_ACTIVITY_NOT_EXIST = new SimpleResponseData(0, "验证错误，活动不在。");
    public static final SimpleResponseData ERROR_ACTIVITY_NOT_START = new SimpleResponseData(0, "验证错误，活动未启用。");
    public static final SimpleResponseData ERROR_USER_NOT_EXIST = new SimpleResponseData(0, "验证错误，用户状态不正常(不存在或停用冻结未审核...)。");
    public static final SimpleResponseData ERROR_ACCOUNT_MORE_NOT_EXIST = new SimpleResponseData(0, "验证错误，兼职详细信息不存在。");
    public static final SimpleResponseData ERROR_USER_JOINTIME_NOT_IN_ACTIVITY = new SimpleResponseData(0, "验证错误，兼职参与活动时间，不在活动期间内。");
    public static final SimpleResponseData ERROR_USER_JOIN_CURRENT_TIME_NOT_IN_ACTIVITY = new SimpleResponseData(0, "验证错误，当前时间不在活动时间内，无法参与活动");
    public static final SimpleResponseData ERROR_USER_IS_IN_BUSY = new SimpleResponseData(0, "验证错误，兼职已有[参与其他进行中活动],或[活动已停止佣金未结算]完成，目前无法加入活动。");
    public static final SimpleResponseData ERROR_USER_IS_IN_THIS_ACTIVITY = new SimpleResponseData(0, "验证错误，兼职已有参加本次活动，无法重复加入。");
    public static final SimpleResponseData ERROR_USER_MARGIN_LOW = new SimpleResponseData(0, "验证错误，兼职额度不足，无法加入活动。");
    public static final SimpleResponseData ERROR_USER_MARGIN_HIGH = new SimpleResponseData(0, "验证错误，兼职额度大于等于活动最高额度，无法加入活动。");
    public static final SimpleResponseData ERROR_USER_ACCOUNTS_LOW = new SimpleResponseData(0, "验证错误，兼职绑定银行卡数量不足，无法加入活动。");
    public static final SimpleResponseData ERROR_USER_ACCOUNTS_USEABLE_LOW =  new SimpleResponseData(0, "验证错误，兼职绑定[可用]银行卡数量不足，无法加入活动。");
    public static final SimpleResponseData ERROR_USER_NO_YSF =  new SimpleResponseData(0, "验证错误，兼职没有开通[云闪付]，无法加入活动。");
    public static final SimpleResponseData ERROR_USER_QUIT_TIME_NOT_IN_ACTIVITY =  new SimpleResponseData(0, "验证错误，兼职参退出动时间，不在活动期间内。");
    public static final SimpleResponseData ERROR_USER_QUIT_CURRENT_TIME_NOT_IN_ACTIVITY =  new SimpleResponseData(0, "验证错误，当前时间不在活动时间内，无法退出活动");
    public static final SimpleResponseData ERROR_USER_QUIT_NOT_IN_ACTIVITY =  new SimpleResponseData(0, "验证错误，兼职没有参加该活动，无法退出");
    public static final Exception ERROR_AMOUNT =  new Exception("[严重错误]： 计算错误，用户金额数据存在负数。");




    @Override
    public  List<BizAccountFlwActivity> findByUid(String uid) {
        return repository.findByUid(uid);
    }

    @Override
    @Transactional
    public BizAccountFlwActivity save(BizAccountFlwActivity bizAccountFlwActivity) {
        return repository.saveAndFlush(bizAccountFlwActivity);
    }


    //参数,基本验证
    @Override
    public SimpleResponseData reqParamCheck(BizAccountFlwActivity bizAccountFlwActivity) {
        if(Objects.isNull(bizAccountFlwActivity)) return  ERROR_JSON_PARAM;
        if(StringUtils.isBlank(bizAccountFlwActivity.getUid())) return  ERROR_ACTIVITY_USERID;
        if(StringUtils.isBlank(bizAccountFlwActivity.getActivityNumber())) return  ERROR_ACTIVITY_NUMBER;
        if(bizAccountFlwActivity.getUserStatusEnum()!=UserActivityStatus.InActivity && bizAccountFlwActivity.getUserStatusEnum()!=UserActivityStatus.UserQuit)
            return  ERROR_ACTIVITY_USERSTATUS;
        if(bizAccountFlwActivity.getUserStatusEnum()==UserActivityStatus.InActivity){
            if(null==bizAccountFlwActivity.getUserStartTime()) return  ERROR_ACTIVITY_USER_STARTTIME;
            if(bizAccountFlwActivity.getUserStartTime().getTime()<new Date().getTime()-60000L)  return ERROR_ACTIVITY_USER_STARTTIME ;
        }
        if(bizAccountFlwActivity.getUserStatusEnum()==UserActivityStatus.UserQuit){
            if(null==bizAccountFlwActivity.getUserEndTime()) return ERROR_ACTIVITY_USER_ENDTIME;
            if(bizAccountFlwActivity.getUserEndTime().getTime()<new Date().getTime()-60000L)  return ERROR_ACTIVITY_USER_ENDTIME;
        }
        if(!reqTokenCheck(bizAccountFlwActivity)) return  ERROR_TOKEN_INVALID;
        return null;
    }


    //参数,逻辑验证
    @Override
    @Transactional
    public SimpleResponseData reqLogicCheck(BizAccountFlwActivity bizAccountFlwActivityReq) {
        boolean checkRebateUserStatus = false;
        BizFlwActivitySyn bizFlwActivitySynInDb = null;

        //验证活动状态
        List<BizFlwActivitySyn> bizFlwActivitySynsInDb = activitySynRepository.findByActivityNumber(bizAccountFlwActivityReq.getActivityNumber());
        if(null==bizFlwActivitySynsInDb || bizFlwActivitySynsInDb.size()!=1)  return ERROR_ACTIVITY_NOT_EXIST;
        if(null!=bizFlwActivitySynsInDb && bizFlwActivitySynsInDb.size()==1 )    bizFlwActivitySynInDb = bizFlwActivitySynsInDb.get(0); //活动编号唯一
        if( bizFlwActivitySynInDb.getActivityStatusEnum()!=ActivityStatus.OK) return ERROR_ACTIVITY_NOT_START;

        //验证兼职用户状态
        BizRebateUser bizRebateUserInDb = rebateUserRepository.findByUid(bizAccountFlwActivityReq.getUid());
        if(bizRebateUserInDb!=null && bizRebateUserInDb.getStatus()==ActivityEnums.RebateUserStatus.RUNNING.getNum()) checkRebateUserStatus=true;
        if(!checkRebateUserStatus) return ERROR_USER_NOT_EXIST;

        //验证account_more关联信息
        BizAccountMore accMoreInDb = accountMoreService.getFromByUid(bizAccountFlwActivityReq.getUid());
        if(accMoreInDb==null)  return ERROR_ACCOUNT_MORE_NOT_EXIST;

        //金额等null->0
        bizAccountMoreBigDecimalNullToZero(accMoreInDb,bizFlwActivitySynInDb);

        //加入活动/退出活动验证
        switch (bizAccountFlwActivityReq.getUserStatusEnum()) {
            case InActivity:
                return reqLogicCheck_joinActivity(bizAccountFlwActivityReq,bizFlwActivitySynInDb,accMoreInDb);
            case UserQuit:
                return reqLogicCheck_quitActivity(bizAccountFlwActivityReq,bizFlwActivitySynInDb);
            default:
                return ERROR_ACTIVITY_USERSTATUS;
        }

    }


    /**
     *  加入活动逻辑验证
     * @param bizAccountFlwActivityReq 返利网请求参数-用户加入/退出活动
     * @param bizFlwActivitySynInDb     待加入活动的活动信息
     * @param accMoreInDb               兼职更多信息
     * @return  返利网返回结果
     */
    private SimpleResponseData reqLogicCheck_joinActivity(BizAccountFlwActivity bizAccountFlwActivityReq, BizFlwActivitySyn bizFlwActivitySynInDb,BizAccountMore accMoreInDb){
        if(bizAccountFlwActivityReq.getUserStatusEnum()== UserActivityStatus.InActivity){

            //当前时间必须在活动期间内才可以[参与]
            if(!ActivityUtil.isBetween(bizFlwActivitySynInDb.getActivityStartTime(),bizFlwActivitySynInDb.getActivityEndTime(),bizAccountFlwActivityReq.getUserStartTime()))
                return ERROR_USER_JOINTIME_NOT_IN_ACTIVITY;
            if(!ActivityUtil.isBetween(bizFlwActivitySynInDb.getActivityStartTime(),bizFlwActivitySynInDb.getActivityEndTime(),new Date()))
                return ERROR_USER_JOIN_CURRENT_TIME_NOT_IN_ACTIVITY;

            //验证兼职是否有参与其他活动，目前禁止1人同时参与2个活动
            List<BizAccountFlwActivity> inAndAwaitActivitys = findInAndAwaitActivitysByUid(bizAccountFlwActivityReq.getUid());
            if(null!=inAndAwaitActivitys && inAndAwaitActivitys.size()>=1)
                return ERROR_USER_IS_IN_BUSY;

            //验证兼职是否已经加入该活动
            if(null!=inAndAwaitActivitys && inAndAwaitActivitys.size()==1 && inAndAwaitActivitys.get(0).getActivityNumber().trim().equalsIgnoreCase(bizFlwActivitySynInDb.getActivityNumber().trim()))
                return ERROR_USER_IS_IN_THIS_ACTIVITY;

            //参与活动最低额度限制（-1不限制,3k）
            if(bizFlwActivitySynInDb.getConditionMargin()!=null && bizFlwActivitySynInDb.getConditionMargin().longValue()>0){
                if(null==accMoreInDb.getMargin() ||accMoreInDb.getMargin().compareTo(bizFlwActivitySynInDb.getConditionMargin())<0)
                    return ERROR_USER_MARGIN_LOW;
            }

            //参与活动最高额度限制(2万)
            if(bizFlwActivitySynInDb.getTopMargin()!=null){
                if(accMoreInDb.getMargin().compareTo(bizFlwActivitySynInDb.getTopMargin())>=0)
                    return ERROR_USER_MARGIN_HIGH;
            }

            //参与活动卡数限制(2张)
            if(bizFlwActivitySynInDb.getConditionAccounts()!=null && bizFlwActivitySynInDb.getConditionAccounts().intValue()>0){
                Integer conditionAccounts = bizFlwActivitySynInDb.getConditionAccounts();
                ArrayList<Integer> moreAccountIds =ActivityUtil.findAccountIds(accMoreInDb.getAccounts());
                if(StringUtils.isBlank(accMoreInDb.getAccounts()) || moreAccountIds.size()<conditionAccounts)
                    return ERROR_USER_ACCOUNTS_LOW;
                //检查绑定可用银行卡状态是否满足2张   AccountStatus in (1,4,5)
                List<BizAccount> bizAccountsInDb = accountService.findByIds(moreAccountIds);
                if(bizAccountsInDb==null || bizAccountsInDb.size()<conditionAccounts)
                    return ERROR_USER_ACCOUNTS_USEABLE_LOW;
                if(bizAccountsInDb.stream().filter(p -> p.getStatus()!= AccountStatus.Normal.getStatus() || p.getStatus()!= AccountStatus.StopTemp.getStatus() || p.getStatus()!= AccountStatus.Enabled.getStatus() ).count()<conditionAccounts)
                    return ERROR_USER_ACCOUNTS_USEABLE_LOW;
            }

            //参与活动限制：是否开通云闪付  biz_account 表里面 Type=1 Flag=2  SubType=3 (代理需要开通云闪付,活动不需要)，-1不限制
            if(bizFlwActivitySynInDb.getConditionYsf()!=null && bizFlwActivitySynInDb.getConditionYsf()!=-1){
                ArrayList<Integer> accountIds =ActivityUtil.findAccountIds(accMoreInDb.getAccounts());
                List<BizAccount> bizAccountsInDb = accountService.findByIds(accountIds);
                if(bizAccountsInDb.stream().filter(p -> p.getType()== AccountType.InBank.getTypeId() &&p.getFlag()==AccountFlag.REFUND.getTypeId() && p.getSubType()== InBankSubType.IN_BANK_YSF.getSubType() ).count()<1)
                    return ERROR_USER_NO_YSF;
            }
            return null;
        }
        return ERROR_ACTIVITY_USERSTATUS;
    }


    /**
     *  退出活动逻辑验证
      * @param bizAccountFlwActivityReq  返利网请求参数-用户加入/退出活动
     * @param bizFlwActivitySynInDb   待加入活动的活动信息
     * @return  返利网返回结果
     */
    private SimpleResponseData reqLogicCheck_quitActivity(BizAccountFlwActivity bizAccountFlwActivityReq, BizFlwActivitySyn bizFlwActivitySynInDb){
        if(bizAccountFlwActivityReq.getUserStatusEnum() ==  UserActivityStatus.UserQuit){
            //当前时间必须在活动期间内才可以[退出]
            final BizFlwActivitySyn bizFlwActivitySynDb = bizFlwActivitySynInDb;
            if(!ActivityUtil.isBetween(bizFlwActivitySynDb.getActivityStartTime(),bizFlwActivitySynDb.getActivityEndTime(),bizAccountFlwActivityReq.getUserEndTime()))
                return ERROR_USER_QUIT_TIME_NOT_IN_ACTIVITY;
            if(!ActivityUtil.isBetween(bizFlwActivitySynDb.getActivityStartTime(),bizFlwActivitySynDb.getActivityEndTime(),new Date()))
                return ERROR_USER_QUIT_CURRENT_TIME_NOT_IN_ACTIVITY;
            //兼职是否参与该活动
            List<BizAccountFlwActivity> inAndAwaitActivitys = findInAndAwaitActivitysByUid(bizAccountFlwActivityReq.getUid());
            if(null!=inAndAwaitActivitys && inAndAwaitActivitys.stream().filter(p -> p.getActivityNumber().trim().equals(bizFlwActivitySynDb.getActivityNumber().trim()) ).count()==1)
                return null;
            return ERROR_USER_QUIT_NOT_IN_ACTIVITY;
        }
        return ERROR_ACTIVITY_USERSTATUS;
    }


    /**
     *  兼职参与/退出活动(调用之前需要先执行参数验证)
      * @param bizAccountFlwActivityReq 返利网兼职参与活动参数
     * @return
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor=Exception.class)
    public synchronized SimpleResponseData joinActive(BizAccountFlwActivity bizAccountFlwActivityReq) throws Exception {
        if(null!=bizAccountFlwActivityReq){
            //加入活动/退出活动验证
            switch (bizAccountFlwActivityReq.getUserStatusEnum()) {
                case InActivity:
                    return joinActivity(bizAccountFlwActivityReq);
                case UserQuit:
                    return quitActivity(bizAccountFlwActivityReq);
                default:
                    return ERROR_ACTIVITY_USERSTATUS;
            }
        }
        return ERROR_JSON_PARAM;
    }


    /**
     * 加入活动
     */
    private SimpleResponseData joinActivity(BizAccountFlwActivity bizAccountFlwActivityReq){
        if(null!=bizAccountFlwActivityReq && bizAccountFlwActivityReq.getUserStatusEnum()==UserActivityStatus.InActivity){
            //更改more表，临时额度+=活动赠送临时额度，活动进行中1，保存用户活动关联
            BizAccountMore accMoreInDb = accountMoreService.getFromByUid(bizAccountFlwActivityReq.getUid()); //防止缓存有问题(手动改库缓存)
            BizFlwActivitySyn bizFlwActivitySynInDb = activitySynRepository.findByActivityNumberAndActivityStatus(bizAccountFlwActivityReq.getActivityNumber(), ActivityEnums.ActivityStatus.OK.getNum());
            if(null!=accMoreInDb && null!=bizFlwActivitySynInDb){
                bizAccountMoreBigDecimalNullToZero(accMoreInDb,bizFlwActivitySynInDb);
                bizAccountFlwActivityReq.setActivityNumber(bizAccountFlwActivityReq.getActivityNumber().trim());
                bizAccountFlwActivityReq.setUserEndTime(null);
                bizAccountFlwActivityReq.setActivityId(bizFlwActivitySynInDb.getId()); //关联活动id
                bizAccountFlwActivityReq.setActivityTmpMargin(bizFlwActivitySynInDb.getTmpMargin()); //活动赠送临时额度
                bizAccountFlwActivityReq.setActivityAmount(BigDecimal.ZERO); //初始化活动佣金0
                accMoreInDb.setTmpMargin(accMoreInDb.getTmpMargin().add( bizFlwActivitySynInDb.getTmpMargin() )); //增加活动临时额度
                accMoreInDb.setInFlwActivity(AccountMoreActivityInStatus.YES.getNum()); //标记用户参与返利网活动
                accMoreInDb.setRemark(CommonUtils.genRemark(accMoreInDb.getRemark(), "加入活动编号："+bizAccountFlwActivityReq.getActivityNumber(), new Date(), "[系统-活动]"));
                accountMoreService.saveAndFlash(accMoreInDb);
                repository.saveAndFlush(bizAccountFlwActivityReq);
                return SUCCESS;
            }
            return ERROR_INNER_EXCEPTION;
        }
        return ERROR_ACTIVITY_USERSTATUS;
    }


    /**
     * 退出活动
     */
    private SimpleResponseData quitActivity(BizAccountFlwActivity bizAccountFlwActivityReq) throws Exception {
        if(null!=bizAccountFlwActivityReq && bizAccountFlwActivityReq.getUserStatusEnum()==UserActivityStatus.UserQuit){

            BizAccountMore accMoreInDb = accountMoreService.getFromByUid(bizAccountFlwActivityReq.getUid()); //防止缓存有问题(手动改库缓存)
            BizAccountFlwActivity bizAccountFlwActivityInDb = repository.findByUidAndActivityNumberAndUserStatus(bizAccountFlwActivityReq.getUid(), bizAccountFlwActivityReq.getActivityNumber().trim(),ActivityEnums.UserActivityStatus.InActivity.getNum());
            BizFlwActivitySyn bizFlwActivitySynInDb = activitySynRepository.findByActivityNumberAndActivityStatus(bizAccountFlwActivityReq.getActivityNumber(), ActivityEnums.ActivityStatus.OK.getNum());

            //返回flw结果
            UserJoinActivityDataResult userJoinActivityDataResult=new UserJoinActivityDataResult();
            userJoinActivityDataResult.setQuitStatus(ActivityEnums.QuitStatus.NotFinish.getNum());//兼职活动完成状态
            userJoinActivityDataResult.setActivityTotalMargin(bizAccountFlwActivityInDb.getActivityAmount());//本次活动获得的活动佣金
            userJoinActivityDataResult.setTmpMargin(bizFlwActivitySynInDb.getTmpMargin()); //本次活动赠送的临时额度

            if(null!=accMoreInDb && null!=bizAccountFlwActivityInDb){
                bizAccountMoreBigDecimalNullToZero(accMoreInDb,bizFlwActivitySynInDb); //设置相关数字null值->0
                bizAccountFlwActivityInDb.setUserStatus(UserActivityStatus.UserQuit.getNum()); //设置用户活动自己退出(如达到活动额度，将改为完成任务)
                bizAccountFlwActivityInDb.setUserEndTime(bizAccountFlwActivityReq.getUserEndTime()); //设置用户活动关联用户结束时间
                accMoreInDb.setTmpMargin(accMoreInDb.getTmpMargin().subtract(bizAccountFlwActivityInDb.getActivityTmpMargin())); //扣除活动赠送临时额度。
                accMoreInDb.setInFlwActivity(AccountMoreActivityInStatus.NO.getNum()); //标记用户未参与返利网活动

                //退出时是否完成返利网活动任务
                if(accMoreInDb.getMargin().compareTo(bizFlwActivitySynInDb.getTopMargin())>=0){ //已到达活动额度，不扣除活动赠送额度，+到可提现金额中
                   // accMoreInDb.setBalance(accMoreInDb.getBalance().add(bizAccountFlwActivityInDb.getActivityAmount())); //达到活动额度，活动额度+到可提现金额。->改为额外返利金自动提额上
                    bizAccountFlwActivityInDb.setUserStatus(UserActivityStatus.FinishActivity.getNum());
                    userJoinActivityDataResult.setQuitStatus(QuitStatus.Finish.getNum());
                    accMoreInDb.setRemark(CommonUtils.genRemark(accMoreInDb.getRemark(), "完成活动退出"+bizAccountFlwActivityInDb.getActivityNumber()+"，本活动累计佣金:"+bizAccountFlwActivityInDb.getActivityAmount().toString(), new Date(), "[系统-活动]"));
                    //todo:如果活动不是自动佣金转额度，要将活动额外的佣金转提现
                }else{
                    accMoreInDb.setActivityTotalAmount(accMoreInDb.getActivityTotalAmount().subtract(bizAccountFlwActivityInDb.getActivityAmount())); //more活动累计总表扣除活动多赠送的额度，
                    //todo:如果活动不是佣金转额度，不改变额度
                    accMoreInDb.setMargin(accMoreInDb.getMargin().subtract(bizAccountFlwActivityInDb.getActivityAmount())); //未完成任务，将累计已经自动提额的活动佣金扣除。
                    accMoreInDb.setLinelimit(accMoreInDb.getLinelimit().subtract(bizAccountFlwActivityInDb.getActivityAmount())); //更改可下发金额
                    accMoreInDb.setRemark(CommonUtils.genRemark(accMoreInDb.getRemark(), "未完成活动退出"+bizAccountFlwActivityInDb.getActivityNumber()+"，本活动累计佣金:"+bizAccountFlwActivityInDb.getActivityAmount().toString(), new Date(), "[系统-活动]"));
                }

                if(!checkBizAccMore(accMoreInDb)){
                    log.error("[严重] {}:{}{}",TAG,"AccountMore,检查不通过，金额为负数。", JSON.toJSONString(accMoreInDb));
                    throw ERROR_AMOUNT;
                }
                accountMoreService.saveAndFlash(accMoreInDb);
                repository.saveAndFlush(bizAccountFlwActivityInDb);
                userJoinActivityDataResult.setMargin(accMoreInDb.getMargin()); //返回flw结果，当前额度
                return  new SimpleResponseData(1, "OK",userJoinActivityDataResult);
            }
            return ERROR_INNER_EXCEPTION;
        }
        return ERROR_ACTIVITY_USERSTATUS;
    }





    /**
     * 检查金额是否不正常
     */
    private boolean checkBizAccMore(BizAccountMore accountMore) {
        boolean result = false;
        if(null!=accountMore){
            bizAccountMoreBigDecimalNullToZero(accountMore,null);
            return  //accountMore.getBalance().compareTo(BigDecimal.ZERO)>=0 &&
                    accountMore.getTmpMargin().compareTo(BigDecimal.ZERO)>=0 &&
                    accountMore.getActivityTotalAmount().compareTo(BigDecimal.ZERO)>=0 ;
        }
        return result;
    }


    /**
     * 数字空-设置0
     */
    private void bizAccountMoreBigDecimalNullToZero(BizAccountMore accountMore, BizFlwActivitySyn bizFlwActivitySyn){
        if(accountMore!=null){
            if(accountMore.getMargin()==null)                accountMore.setMargin(BigDecimal.ZERO);
            if(accountMore.getBalance()==null)               accountMore.setBalance(BigDecimal.ZERO);
            if(accountMore.getTmpMargin()==null)             accountMore.setTmpMargin(BigDecimal.ZERO);
            if(accountMore.getActivityTotalAmount()==null)   accountMore.setActivityTotalAmount(BigDecimal.ZERO);
        }
        if(bizFlwActivitySyn!=null){
            if(bizFlwActivitySyn.getTopMargin()==null)   bizFlwActivitySyn.setTopMargin(BigDecimal.ZERO);
            if(bizFlwActivitySyn.getTmpMargin()==null)   bizFlwActivitySyn.setTmpMargin(BigDecimal.ZERO);
        }
    }



    /**
     * 查找兼职用户-进行中的活动
     */
     private List<BizAccountFlwActivity>  findInAndAwaitActivitysByUid(String uid){
        if(StringUtils.isBlank(uid)) return null;
         Specification<BizAccountFlwActivity> specif = DynamicSpecifications.build(BizAccountFlwActivity.class,
                 new SearchFilter("uid", SearchFilter.Operator.EQ, uid ),
                 new SearchFilter("userStatus", SearchFilter.Operator.IN, Arrays.asList(ActivityEnums.UserActivityStatus.InActivity.getNum()).toArray() )
                 );
         return repository.findAll(specif);
     }



    /**
     * Token字符串验证
     * uid+activityNumber+userEndTime+userStartTime+userStatus+密钥
     */
    private boolean reqTokenCheck(BizAccountFlwActivity bizAccountFlwActivity) {
        try {
            if(bizAccountFlwActivity!=null && StringUtils.isNotBlank(bizAccountFlwActivity.getToken())){
                return ActivityUtil.reqTokenCheck(tokenCheckFormatStr, bizAccountFlwActivity.getToken(), bizAccountFlwActivity.getUid(), bizAccountFlwActivity.getActivityNumber(), bizAccountFlwActivity.getUserStartTime().getTime(), bizAccountFlwActivity.getUserStatus(), appProperties.getRebatesalt());
            }
        }catch (Exception e){
            log.error("{},token生成失败:{}",TAG,e.getMessage(),e);
            return false;
        }
        return false;
    }



    //从本地缓存或redis-获取全部参与活动的返利网兼职账号绑定的卡-id,
    @Override
    public Set<String> getAllAccountsInActivity() {
        if (cache != null) {
            Set<String> locaAccountsInActivitys = cache.getIfPresent(AccountsInActivitys_Redis_Key);
            if(null==locaAccountsInActivitys || locaAccountsInActivitys.size()==0){
                return getAllAccountsInActivityInRedis();
            }
            return  locaAccountsInActivitys;
        }
        return Sets.newHashSet();
    }


    //从redis-获取全部参与活动的返利网兼职账号绑定的卡-id,并更新本地缓存
    private Set<String> getAllAccountsInActivityInRedis() {
         if(stringRedisTemplate.hasKey(AccountsInActivitys_Redis_Key) && stringRedisTemplate.opsForSet().size(AccountsInActivitys_Redis_Key)>0){
             Set<String> members = stringRedisTemplate.opsForSet().members(AccountsInActivitys_Redis_Key);
             updateLocalCacheWithAccountsInActivity(members);
             return members;
         }
         return  Sets.newHashSet();
    }



    //返利网用户活动Topic消息-redisMq
    @Override
    public void onMessage(String topic,String msg){
        if(topic.equalsIgnoreCase(RedisTopics.REBAT_USER_ACTIVITY_TOPIC)){
            if(msg.equalsIgnoreCase(AccountsInActivitys_Redis_Key)){ //从redis获取-参加活动账号，更新本地缓存
                getAllAccountsInActivityInRedis();
            }
        }
    }


    //数据库更新redis和本地缓存并通知其他设备-全部参与活动的返利网兼职账号绑定的卡-id
    @Override
    public void updateReidsWithAccountsInActivity(){
        try {
            stringRedisTemplate.delete(AccountsInActivitys_Redis_Key);
            List<String> membersDb = accountMoreRepository.findAccountsByInFlwActivity(ActivityEnums.AccountMoreActivityInStatus.YES.getNum());
            if(membersDb!=null && membersDb.size()>0){
                ArrayList<String> members = Lists.newArrayList();
                membersDb.stream().forEach(m->{
                    ActivityUtil.findAccountIds(m).forEach(id-> members.add(id.toString()) );
                });
                stringRedisTemplate.opsForSet().add(AccountsInActivitys_Redis_Key,members.toArray(new String[members.size()]));
                stringRedisTemplate.convertAndSend(RedisTopics.REBAT_USER_ACTIVITY_TOPIC,AccountsInActivitys_Redis_Key);//发布更新消息
            }
        }catch (Exception e){
            log.error("{},{}，{}",TAG,"兼职参与/退出活动更新缓存失败：",e.getMessage(),e);
        }
    }



    //更新本地缓存
    private void updateLocalCacheWithAccountsInActivity(Set<String> members){
       if(cache!=null){
           cache.invalidate(AccountsInActivitys_Redis_Key);
           cache.put(AccountsInActivitys_Redis_Key,members);
       }
    }



    @Override
    public boolean allowWithdrawal(String uid) {
        List<BizFlwActivitySyn> inActivitysWithUid = activitySynRepository.findInActivityWithUid(uid);
        if(inActivitysWithUid.stream().filter(p->p.getAllowWithdrawalEnum()==ActivityAllowWithdrawal.NO).count()>0) return false;
        return true;
    }


    @Override
    public Map<String, BigDecimal> getSumActivityAmountByUserActivityStatusIsIn() {
        List<Map<String, String>> resDb = repository.getSumActivityAmountByUserActivityStatus(UserActivityStatus.InActivity.getNum());
        if(!CollectionUtils.isEmpty(resDb)){
            HashMap<String, BigDecimal> resultMap = Maps.newHashMap();
            resDb.stream().forEach(map-> resultMap.put(map.get("uid"),new BigDecimal(map.get("amount"))));
            return  resultMap;
        }
        return Collections.emptyMap();
    }



}
