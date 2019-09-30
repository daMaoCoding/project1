package com.xinbo.fundstransfer.chatPay.commons.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.AppProperties;
import com.xinbo.fundstransfer.component.net.http.restTemplate.CallCenterServiceApiStatic;
import com.xinbo.fundstransfer.chatPay.commons.config.AliIncomeConfig;
import com.xinbo.fundstransfer.chatPay.commons.config.AliOutConfig;
import com.xinbo.fundstransfer.domain.entity.SysUserProfile;
import com.xinbo.fundstransfer.domain.enums.UserProfileKey;
import com.xinbo.fundstransfer.service.SysUserProfileService;
import com.xinbo.fundstransfer.service.SystemSettingService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * ************************
 * 聊天室支付 系统配置相关服务
 * 0.账号验证通过
 * 1.入款/出款配置是否要匹配给会员/兼职
 * 2.区域匹配/层级匹配
 * 3.
 * @author tony
 */

@Slf4j
@Component
public class ChatPayConfigService {

    @Autowired  SysUserProfileService sysUserProfileService;
    @Autowired  CallCenterServiceApiStatic callCenterServiceApiStatic;
    @Autowired  SystemSettingService systemSettingService;
    @Autowired  AppProperties appProperties;


    /**
     * 聊天室支付 获取支付宝入款配置
     * 配置页面路径：入款管理/支付宝入款/设置
     */
    public AliIncomeConfig getAliIncomeConfig (){
        SysUserProfile sysUserProfile = sysUserProfileService.findByUserIdAndPropertyKey(false, AppConstants.USER_ID_4_ADMIN, UserProfileKey.INCOME_ALI_CONFIG.getValue());
        if(null!=sysUserProfile && StringUtils.isNotBlank(sysUserProfile.getPropertyValue())){
            try {
                return JSON.parseObject(sysUserProfile.getPropertyValue(), AliIncomeConfig.class, Feature.AllowUnQuotedFieldNames, Feature.IgnoreNotMatch);
            }catch (Exception e){
                log.error("获取聊天室支付，支付宝入款配置失败，错误:{},配置：{}",e.getMessage(), JSON.toJSONString(sysUserProfile),e);
                throw new RuntimeException("获取支付宝出款配置失败");
            }
        }
        throw new RuntimeException("获取支付宝入款配置失败");
    }



    /**
     * 聊天室支付  获取支付宝出款配置
     * 配置页面路径：出款管理/支付宝出款/设置
     */
    public AliOutConfig getAliOutConfig(){
        SysUserProfile sysUserProfile = sysUserProfileService.findByUserIdAndPropertyKey(false, AppConstants.USER_ID_4_ADMIN, UserProfileKey.OUT_ALI_CONFIG.getValue());
        if(null!=sysUserProfile && StringUtils.isNotBlank(sysUserProfile.getPropertyValue())){
            try {
               return JSON.parseObject(sysUserProfile.getPropertyValue(), AliOutConfig.class, Feature.AllowUnQuotedFieldNames, Feature.IgnoreNotMatch);
            }catch (Exception e){
                log.error("获取聊天室支付，支付宝出款配置失败，错误:{},配置：{}",e.getMessage(), JSON.toJSONString(sysUserProfile),e);
                throw new RuntimeException("获取支付宝出款配置失败");
            }
        }
        throw new RuntimeException("获取支付宝出款配置失败");
    }




    /**
     * 获取数据字典配置-入款/出款 账号分配配置
     * 字典：sys_user_profile.* ;
     */
    public  AssignAccountConfig getAssignAccountConfig(){
        SysUserProfile s1 = sysUserProfileService.findByUserIdAndPropertyKey(false, AppConstants.USER_ID_4_ADMIN, UserProfileKey.CHAT_PAY_INMONEY_ASSIGN_PLANTUSER.getValue());
        SysUserProfile s2 = sysUserProfileService.findByUserIdAndPropertyKey(false, AppConstants.USER_ID_4_ADMIN, UserProfileKey.CHAT_PAY_INMONEY_ASSIGN_REBATEUSER.getValue());
        SysUserProfile s3 = sysUserProfileService.findByUserIdAndPropertyKey(false, AppConstants.USER_ID_4_ADMIN, UserProfileKey.CHAT_PAY_OUTMONEY_ASSIGN_PLANTUSER.getValue());
        SysUserProfile s4 = sysUserProfileService.findByUserIdAndPropertyKey(false, AppConstants.USER_ID_4_ADMIN, UserProfileKey.CHAT_PAY_OUTMONEY_ASSIGN_REBATEUSER.getValue());
        SysUserProfile s5 = sysUserProfileService.findByUserIdAndPropertyKey(false, AppConstants.USER_ID_4_ADMIN, UserProfileKey.CHAT_PAY_INMONEY_MATCHING_WITH_ZONE.getValue());
        SysUserProfile s6 = sysUserProfileService.findByUserIdAndPropertyKey(false, AppConstants.USER_ID_4_ADMIN, UserProfileKey.CHAT_PAY_INMONEY_MATCHING_WITH_LEVEL.getValue());
        SysUserProfile s7 = sysUserProfileService.findByUserIdAndPropertyKey(false, AppConstants.USER_ID_4_ADMIN, UserProfileKey.CHAT_PAY_OUTMONEY_MATCHING_WITH_ZONE.getValue());
        SysUserProfile s8 = sysUserProfileService.findByUserIdAndPropertyKey(false, AppConstants.USER_ID_4_ADMIN, UserProfileKey.CHAT_PAY_OUTMONEY_MATCHING_WITH_LEVEL.getValue());
        SysUserProfile s9 = sysUserProfileService.findByUserIdAndPropertyKey(false, AppConstants.USER_ID_4_ADMIN, UserProfileKey.CHAT_PAY_NOT_LOGIN_ROOM_TIME_OUT.getValue());
        if(null!=s1 && StringUtils.isNotBlank(s1.getPropertyValue()) &&
           null!=s2 && StringUtils.isNotBlank(s2.getPropertyValue()) &&
           null!=s3 && StringUtils.isNotBlank(s3.getPropertyValue()) &&
           null!=s4 && StringUtils.isNotBlank(s4.getPropertyValue()) &&
           null!=s5 && StringUtils.isNotBlank(s5.getPropertyValue()) &&
           null!=s6 && StringUtils.isNotBlank(s6.getPropertyValue()) &&
           null!=s7 && StringUtils.isNotBlank(s7.getPropertyValue()) &&
           null!=s8 && StringUtils.isNotBlank(s8.getPropertyValue()) &&
           null!=s9 && StringUtils.isNotBlank(s9.getPropertyValue())  ){
            try {
                boolean inmoneyAssignToPlantUser =   BooleanUtils.toBoolean(Integer.parseInt(s1.getPropertyValue()));
                boolean inmoneyAssignToRebateUser =  BooleanUtils.toBoolean(Integer.parseInt(s2.getPropertyValue()));
                boolean outmoneyAssignToPlantUser =  BooleanUtils.toBoolean(Integer.parseInt(s3.getPropertyValue()));
                boolean outmoneyAssignToRebateUser = BooleanUtils.toBoolean(Integer.parseInt(s4.getPropertyValue()));
                boolean inmoneyMatchingWithZone =    BooleanUtils.toBoolean(Integer.parseInt(s5.getPropertyValue()));
                boolean inmoneyMatchingWithLevel =   BooleanUtils.toBoolean(Integer.parseInt(s6.getPropertyValue()));
                boolean outmoneyMatchingWithZone =   BooleanUtils.toBoolean(Integer.parseInt(s7.getPropertyValue()));
                boolean outmoneyMatchingWithLevel =  BooleanUtils.toBoolean(Integer.parseInt(s8.getPropertyValue()));
                boolean notLoginRoomTimeout     =  BooleanUtils.toBoolean(Integer.parseInt(s9.getPropertyValue()));

                return new AssignAccountConfig(inmoneyAssignToPlantUser,inmoneyAssignToRebateUser,outmoneyAssignToPlantUser,outmoneyAssignToRebateUser,
                                               inmoneyMatchingWithZone,inmoneyMatchingWithLevel,outmoneyMatchingWithZone,outmoneyMatchingWithLevel,
                                               notLoginRoomTimeout);
            }catch (Exception e){
                log.error("获取数据字典配置-入款/出款 账号分配配置 失败，错误:{}",e.getMessage(),e);
                throw new RuntimeException("获取支付宝出款配置失败");
            }
        }
        throw new RuntimeException("获取账号分配配置失败");
    }





}
