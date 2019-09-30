package com.xinbo.fundstransfer.restful.v3.settingSyn;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.AppProperties;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.domain.entity.SysUserProfile;
import com.xinbo.fundstransfer.domain.enums.UserProfileKey;
import com.xinbo.fundstransfer.restful.v2.TokenValidation;
import com.xinbo.fundstransfer.restful.v3.settingSyn.reqVo.ReqSetMaxUpgradeQuantity;
import com.xinbo.fundstransfer.runtime.MemCacheUtils;
import com.xinbo.fundstransfer.service.RedisService;
import com.xinbo.fundstransfer.service.SysUserProfileService;
import com.xinbo.fundstransfer.utils.TokenCheckUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * ************************
 * 返利网同步 设置
 * @author tony
 */
@Slf4j
@RestController
public class FlwSettingController  extends TokenValidation {

    private static String TAG = "返利网同步 设置";
    private static Feature[] baseFeatures = {Feature.AllowUnQuotedFieldNames,Feature.IgnoreNotMatch};


    @Autowired     private SysUserProfileService sysUserProfileService;
//  @Autowired     private SystemSettingService systemSettingService;
    @Autowired     private RedisService redisService;
    private static  final String tokenCheckFormatStr ="%s%s%s";
    private static ObjectMapper mapper = new ObjectMapper();
    @Autowired AppProperties appProperties;

    /**
     * 2.15   返利网-设置同时升级工具客户端个数
     */
    @RequestMapping(value = "/api/v3/setMaxUpgradeQuantity",method = RequestMethod.POST, consumes = "application/json")
    public Map<String,Object> setMaxUpgradeQuantity(@Validated @RequestBody ReqSetMaxUpgradeQuantity reqSetMaxUpgradeQuantity, Errors errors){
        log.info("{}->setMaxUpgradeQuantity,参数：{}",TAG, JSON.toJSONString(reqSetMaxUpgradeQuantity));
        if (errors.hasErrors()) {
            return  SimpleResponseData.getErrorMapResult( errors.getAllErrors().get(0).getDefaultMessage());
        }
        try {
            if(!TokenCheckUtil.reqTokenCheck(tokenCheckFormatStr,reqSetMaxUpgradeQuantity.getToken(),reqSetMaxUpgradeQuantity.getTimestamp(),reqSetMaxUpgradeQuantity.getNumber(),appProperties.getRebatesalt())){
                return SimpleResponseData.getErrorMapResult("token验证错误。");
            }
            SysUserProfile sysUserProfile = sysUserProfileService.findByUserIdAndPropertyKey(true, AppConstants.USER_ID_4_ADMIN, UserProfileKey.APP_TOOLS_MAX_UPGRADE_QUANTITY.getValue());
            sysUserProfile.setPropertyValue(String.valueOf(reqSetMaxUpgradeQuantity.getNumber()));
            sysUserProfileService.saveAndFlush(sysUserProfile);
            log.info("更新最大app更新每分钟缓存，操作人{}，{}", AppConstants.USER_ID_4_ADMIN, UserProfileKey.APP_TOOLS_MAX_UPGRADE_QUANTITY.getValue());
            redisService.convertAndSend(RedisTopics.REFRESH_SYSTEM_PROFILE,mapper.writeValueAsString(sysUserProfile));
        }catch (Exception e){
            log.error("{},msg:{}",TAG,e.getMessage(),e);
           return SimpleResponseData.getErrorMapResult("系统错误。"+e.getMessage());
        }
        return SimpleResponseData.getSuccessMapResult();
    }



    /**
     * cabana 获取 返利网设置同时升级工具客户端个数
     */
    @RequestMapping(value = "/api/v2/funds/getMaxUpgradeQuantity",method = RequestMethod.POST, consumes = "application/json")
    public Map<String,Object> getMaxUpgradeQuantity(@RequestBody String bodyJson){

        Map<String, Object> successMapResult = SimpleResponseData.getSuccessMapResult();
        int defaultNumber=30;
        try {
            TreeMap<String, String> params = mapper.readValue(bodyJson, TreeMap.class);
            String token = params.get("token");
            params.remove("token");
            if (!Objects.equals(md5digest(params, appProperties.getCabanasalt()), token)) {
                return   SimpleResponseData.getErrorMapResult("token错误。");
            }
            String  numberStr = MemCacheUtils.getInstance().getSystemProfile().getOrDefault(UserProfileKey.APP_TOOLS_MAX_UPGRADE_QUANTITY.getValue(),"30");
            if(StringUtils.isNotBlank(numberStr)&& Integer.valueOf(numberStr)>0)
                defaultNumber = Integer.valueOf(numberStr);
        }catch (Exception e){
            log.error("msg:{}",e.getMessage(),e);
            return   SimpleResponseData.getErrorMapResult("内部错误。");
        }
        successMapResult.put("maxUpgradeQuantity",defaultNumber);
        return successMapResult;
    }



}
