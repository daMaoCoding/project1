package com.xinbo.fundstransfer.chatPay.commons.services;

import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.BizLevel;
import com.xinbo.fundstransfer.service.HandicapService;
import com.xinbo.fundstransfer.service.LevelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * ************************
 * 盘口，区域，层级
 * @author tony
 */
@Service
public class OidAndLevelServices {

    @Autowired   private HandicapService handicapService;
    @Autowired   private LevelService levelService;

    /**
     * 通过oid号获取,盘口id,区域
     *
     */
    public BizHandicap getBizHandicapByOid(String oid){
       return  handicapService.findFromCacheByCode(oid);
    }


    /**
     * 通过盘口id获取
     */
    public BizHandicap getBizHandicapByid(int id){
        return  handicapService.findFromCacheById(id);
    }


    /**
     * 通过盘口id,层级编码，获取层级（内/外层）
     */
    public BizLevel getBizLevelByOidAndLevelCode(String oid,String levelCode){
        BizHandicap bizHandicapByOid = getBizHandicapByOid(oid);
        if(bizHandicapByOid==null) return null;
        return levelService.findFromCache(bizHandicapByOid.getId(), levelCode);
    }


}
