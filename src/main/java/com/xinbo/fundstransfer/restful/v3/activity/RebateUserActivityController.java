package com.xinbo.fundstransfer.restful.v3.activity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppProperties;
import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.domain.entity.activity.BizAccountFlwActivity;
import com.xinbo.fundstransfer.domain.repository.AccountMoreRepository;
import com.xinbo.fundstransfer.domain.repository.activity.RebateUserActivityRepository;
import com.xinbo.fundstransfer.restful.v3.Base3Common;
import com.xinbo.fundstransfer.service.RebateApiService;
import com.xinbo.fundstransfer.service.RebateUserActivityService;
import com.xinbo.fundstransfer.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ************************
 * 返利网用户活动接口
 * @author tony
 */
@RestController
@RequestMapping("/api/v3")
@Slf4j
public class RebateUserActivityController extends Base3Common {
    @Autowired  private RebateUserActivityService rebateUserActivityService;
    private static final  String TAG="[返利网用户参加/退出活动]";

    /**
     * 返利网兼职参与活动
     */
    @RequestMapping(value = "/userJoinActivity", method = RequestMethod.POST, consumes = "application/json")
    @ResponseBody
    public SimpleResponseData userJoinActivity(@RequestBody(required = false) String reqJsonStr) {
        log.info("{}->joinActive,参数：{}",TAG,reqJsonStr);
        BizAccountFlwActivity bizAccountFlwActivity = null;
        SimpleResponseData simpleResponseData = null;
        //JSON参数验证
        try {
             bizAccountFlwActivity = JSON.parseObject(reqJsonStr, BizAccountFlwActivity.class, Feature.AllowUnQuotedFieldNames);
        }catch (Exception e){
            return ERROR_JSON_PARAM;
        }

        try {
            //基本验证参数
            SimpleResponseData checkResultSimple = rebateUserActivityService.reqParamCheck(bizAccountFlwActivity);
            if(null!=checkResultSimple) return checkResultSimple;
            //逻辑参数验证
            SimpleResponseData checkResultLogic = rebateUserActivityService.reqLogicCheck(bizAccountFlwActivity);
            if(null!=checkResultLogic) return checkResultLogic;
            //兼职参与活动/退出活动&同步更新参加活动uid的账号
             simpleResponseData = rebateUserActivityService.joinActive(bizAccountFlwActivity);
            if(simpleResponseData.getStatus()==1)   rebateUserActivityService.updateReidsWithAccountsInActivity();
        } catch (Exception e) {
            log.error("{},错误：",TAG,e.getMessage(),e);
            return  new SimpleResponseData(0,  StringUtils.isBlank(e.getMessage())?ERROR_INNER_EXCEPTION.getMessage():e.getMessage());
        }
        return simpleResponseData;
    }



}
