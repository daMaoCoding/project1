package com.xinbo.fundstransfer.restful.v3.activity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.domain.entity.activity.BizFlwActivitySyn;
import com.xinbo.fundstransfer.restful.v3.Base3Common;
import com.xinbo.fundstransfer.service.RebateActivitySynService;
import com.xinbo.fundstransfer.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * ************************
 * 返利网活动同步接口
 * @author tony
 */
@RestController
@RequestMapping("/api/v3")
@Slf4j
public class RebateActivitySynController extends Base3Common {
     private ObjectMapper mapper = new ObjectMapper();
     private static final  String TAG="[返利网活动同步]";

    @Autowired  RebateActivitySynService rebateActivitySynService;
    @Autowired  private RedisService redisService;


    /**
     * 返利网兼职参与活动
     */
    @RequestMapping(value = "/synActivity", method = RequestMethod.POST, consumes = "application/json")
    @ResponseBody
    public SimpleResponseData synActivity(@RequestBody String reqJsonStr, BindingResult result) {
        log.info("{}->synActivity,参数：{}",TAG,reqJsonStr);

        BizFlwActivitySyn bizFlwActivitySyn = null;
        Date now = new Date();

        //JSON参数验证
        try {
            bizFlwActivitySyn = JSON.parseObject(reqJsonStr, BizFlwActivitySyn.class, Feature.AllowUnQuotedFieldNames);
        }catch (Exception e){
            return ERROR_JSON_PARAM;
        }
        //基本验证参数
        SimpleResponseData checkResultSimple = rebateActivitySynService.reqParamCheck(bizFlwActivitySyn);
        if(null!=checkResultSimple) return checkResultSimple;

        //逻辑参数验证
        SimpleResponseData checkResultLogic = rebateActivitySynService.reqLogicCheck(bizFlwActivitySyn);
        if(null!=checkResultLogic) return checkResultLogic;

        //添加/修改活动
        BizFlwActivitySyn bizFlwActivitySynDb = rebateActivitySynService.synActive(bizFlwActivitySyn,now);
        if(null!=bizFlwActivitySynDb) return SUCCESS;

        return ERROR_INNER_EXCEPTION;
    }



}
