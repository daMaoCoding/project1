package com.xinbo.fundstransfer.restful.v3.agent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.domain.entity.agent.BizRebateAgentSyn;
import com.xinbo.fundstransfer.domain.repository.AccountMoreRepository;
import com.xinbo.fundstransfer.domain.repository.agent.BizRebateAgentSynRepository;
import com.xinbo.fundstransfer.service.BizRebateAgentSynService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.text.MessageFormat;

/**
 * ************************
 *
 * @author tony
 */
@Slf4j
@RestController
@RequestMapping("/api/v3")
public class AgentController {

    @PersistenceContext private EntityManager entityManager;
    @Autowired AccountMoreRepository bizAccountMoreRepository;
    @Autowired BizRebateAgentSynRepository bizRebateAgentSynRepository;
    @Autowired  BizRebateAgentSynService bizRebateAgentSynService;


    private static String TAG = "返利网代理同步";


    @RequestMapping(value = "/synAgentRelation", method = RequestMethod.POST, consumes = "application/json")
    public SimpleResponseData synAgentRelation(@RequestBody(required = false) String reqJsonStr){
        log.info("{}->synAgentRelation,参数：{}",TAG,reqJsonStr);

        BizRebateAgentSyn bizRebateAgentReq = null;
        try {
            bizRebateAgentReq = JSON.parseObject(reqJsonStr, BizRebateAgentSyn.class, Feature.AllowUnQuotedFieldNames);
        }catch (Exception e){
            log.error("{},出错，解析参数JSON出错:{}。",TAG,e.getMessage(),e);
            return  new SimpleResponseData(0,TAG+",出错，解析参数JSON出错。");
        }

        //参数检查
        SimpleResponseData checkResult =  bizRebateAgentSynService.checkReqParam(bizRebateAgentReq);
        if(checkResult!=null) return checkResult;

        //保存或更新
        try {
            bizRebateAgentSynService.syn(bizRebateAgentReq);
        }catch(Exception e){
            String msg = MessageFormat.format("{0},出错:{1}。",TAG,e.getMessage());
            log.error(msg,e);
            return new SimpleResponseData(0,msg);
        }

        return new SimpleResponseData(1,"OK");

    }

}























