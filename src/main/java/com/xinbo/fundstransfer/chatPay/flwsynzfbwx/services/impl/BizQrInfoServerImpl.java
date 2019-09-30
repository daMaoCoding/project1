package com.xinbo.fundstransfer.chatPay.flwsynzfbwx.services.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.chatPay.flwsynzfbwx.entity.BizQrInfo;
import com.xinbo.fundstransfer.chatPay.flwsynzfbwx.repository.BizQrInfoRepository;
import com.xinbo.fundstransfer.chatPay.flwsynzfbwx.reqVo.ReqFlwQrValBack;
import com.xinbo.fundstransfer.chatPay.flwsynzfbwx.reqVo.ReqRebateUserWxZfbAccount;
import com.xinbo.fundstransfer.chatPay.flwsynzfbwx.services.BizQrInfoServer;
import com.xinbo.fundstransfer.chatPay.ptqrval.reqVo.ReqPtQrVal;
import com.xinbo.fundstransfer.chatPay.ptqrval.reqVo.ReqPtQrValBack;
import com.xinbo.fundstransfer.component.net.http.restTemplate.PlatformServiceApiStatic;
import com.xinbo.fundstransfer.component.net.http.restTemplate.RebateServiceApiStatic;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizAccountMore;
import com.xinbo.fundstransfer.domain.enums.AccountFlag;
import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.sec.FundTransferEncrypter;
import com.xinbo.fundstransfer.service.AccountMoreService;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.utils.TokenCheckUtil;
import com.xinbo.fundstransfer.utils.randomUtil.RandomUtil;
import com.xinbo.fundstransfer.chatPay.valtools.reqVo.ReqBackValQrJob;
import com.xinbo.fundstransfer.chatPay.valtools.reqVo.ReqGetValQrJob;
import com.xinbo.fundstransfer.AppProperties;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * ************************
 * 二维码验证
 * @author tony
 */
@Slf4j
@Service
public class BizQrInfoServerImpl implements BizQrInfoServer {

    @Autowired  AppProperties appProperties;
    @Autowired  RebateServiceApiStatic rebateServiceApiStatic;
    @Autowired  PlatformServiceApiStatic platformServiceApiStatic;
    @Autowired  BizQrInfoRepository bizQrInfoRepository;
    @Autowired  RandomUtil randomUtil;

    @Autowired private AccountService accountService;
    @Autowired  private AccountMoreService accountMoreSer;


    private static Cache<String, JSONObject> cache = CacheBuilder.newBuilder().maximumSize(20000) .expireAfterWrite(15, TimeUnit.DAYS).build();


    /**
     * 返利网需要验证的二维码
     */
    @Override
    public BizQrInfo convertAndCheck(ReqRebateUserWxZfbAccount reqRebateUserWxZfbAccount) {
        String qrid = randomUtil.genQrId(RandomUtil.PrexQRID.F,reqRebateUserWxZfbAccount.getQrContent(),reqRebateUserWxZfbAccount.getUid());
        BizQrInfo bizQrInfo = new BizQrInfo();
        bizQrInfo.setQrId(qrid);
        bizQrInfo.setQrFrom(0); //二维码来源(0返利网，1平台)
        bizQrInfo.setUid(reqRebateUserWxZfbAccount.getUid());
        bizQrInfo.setUname(reqRebateUserWxZfbAccount.getUname());
        bizQrInfo.setHandicapCode(null);
        bizQrInfo.setAccount(reqRebateUserWxZfbAccount.getAccount());
        bizQrInfo.setQrType(reqRebateUserWxZfbAccount.getQrType());
        bizQrInfo.setQrStatus(1);  //二维码状态(1正常，0停用 。 默认1)
        bizQrInfo.setQrContent(reqRebateUserWxZfbAccount.getQrContent());
        bizQrInfo.setName(reqRebateUserWxZfbAccount.getName());
        bizQrInfo.setCreateTime(new Date());
        bizQrInfo.setValQrStatus(0); //0新生成，1任务取走验证中，2验证成功 ，3验证失败
        return  bizQrInfo;
    }

    /**
     * 平台需要验证的二维码
     */
    @Override
    public BizQrInfo convertAndCheck(ReqPtQrVal reqPtQrVal) {
        String qrid = randomUtil.genQrId(RandomUtil.PrexQRID.P,reqPtQrVal.getQrContent(),reqPtQrVal.getUid());
        BizQrInfo bizQrInfo = new BizQrInfo();
        bizQrInfo.setQrId(qrid);
        bizQrInfo.setQrFrom(1); //二维码来源(0返利网，1平台)
        bizQrInfo.setUid(reqPtQrVal.getUid());
        bizQrInfo.setUname(reqPtQrVal.getUname());
        bizQrInfo.setHandicapCode(reqPtQrVal.getOid());
        bizQrInfo.setAccount(reqPtQrVal.getAccount());
        bizQrInfo.setQrType(reqPtQrVal.getQrType());
        bizQrInfo.setQrStatus(1);  //二维码状态(1正常，0停用 。 默认1)
        bizQrInfo.setQrContent(reqPtQrVal.getQrContent());
        bizQrInfo.setName(reqPtQrVal.getName());
        bizQrInfo.setCreateTime(new Date());
        bizQrInfo.setValQrStatus(0); //0新生成，1任务取走验证中，2验证成功 ，3验证失败
        bizQrInfo.setProvinceId(null); //会员省
        bizQrInfo.setCityId(null); //会员市
        return  bizQrInfo;
    }




    @Override
    @Modifying
    @Transactional
    public BizQrInfo saveAndFlush(BizQrInfo bizQrInfo) {
       return  bizQrInfoRepository.saveAndFlush(bizQrInfo);
    }


    /**
     * 二维码验证工具获取要验证任务
     */
    @Override
    public BizQrInfo findValQrJob() {
       //1.返利网新账号
       BizQrInfo bizQrInfo = bizQrInfoRepository.findFlwNewQr();
       if(null!= bizQrInfo) return bizQrInfo;
       //2.返利网验证超时账号
        bizQrInfo = bizQrInfoRepository.findFlwJobQr();
        if(null!= bizQrInfo) return bizQrInfo;
        //3.平台新账号
        bizQrInfo = bizQrInfoRepository.findPtNewQr();
        if(null!= bizQrInfo) return bizQrInfo;
        //4.平台验证超时账号
        bizQrInfo = bizQrInfoRepository.findPtJobQr();
        if(null!= bizQrInfo) return bizQrInfo;
        return null;
    }


    /**
     * 验证工具接收任务,保存任务信息
     */
    @Override
    @Transactional
    public BizQrInfo valQrJobAccept(ReqGetValQrJob reqGetValQrJob, BizQrInfo bizQrInfo) {
        if(reqGetValQrJob!=null && bizQrInfo!=null){
            bizQrInfo.setValQrJobTime(new Date());
            bizQrInfo.setValDevicesId(reqGetValQrJob.getDeviceId());
            bizQrInfo.setValQrStatus(1);  //任务取走，验证中
           return saveAndFlush(bizQrInfo);
        }
        return bizQrInfo;
    }


    /**
     * 通过二维码id获取二维码信息(无缓存)
     */
    @Override
    public BizQrInfo findQrInfoByQrId(String qrId) {
        try {
            return bizQrInfoRepository.findByQrId(qrId);
        }catch (Exception e ){
            log.error("[二维码id获取二维码错误]",e);
        }
        return null;
    }


    /**
     * 更新工具验证结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void backValQrJobAccept(ReqBackValQrJob reqBackValQrJob, BizQrInfo bizQrInfo) {
        if(reqBackValQrJob!=null && bizQrInfo!=null){
            bizQrInfo.setValQrBackTime(new Date());
            bizQrInfo.setValDevicesId(reqBackValQrJob.getDeviceId());
            bizQrInfo.setValQrStatus(reqBackValQrJob.getStatus()==1?2:3 );  //二维码验证结果1成功
/*            if(bizQrInfo.getValQrStatus()==2 && bizQrInfo.getQrFrom()==0){//保存返利网兼职验证通过的qr信息
                //保存至兼职表二维码表
                List<BizRebateUserQr> exist = bizRebateUserQrRepository.findByUidAndQrInfoId(bizQrInfo.getUid(), bizQrInfo.getId());
                if(exist.isEmpty()){
                    BizRebateUserQr bizRebateUserQr = new BizRebateUserQr();
                    bizRebateUserQr.setUid(bizQrInfo.getUid());
                    bizRebateUserQr.setQrInfoId(bizQrInfo.getId());
                    bizRebateUserQrRepository.saveAndFlush(bizRebateUserQr);
                }
            }*/
            bizQrInfo.setValQrNotifStatus(notifyFlwAndPt(bizQrInfo)); //回调通知返利网&平台相关状态
            saveAndFlush(bizQrInfo); //保存通知成功/失败状态
        }
    }






    /**
     * 通知返利网和平台二维码验证状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int notifyFlwAndPt(BizQrInfo bizQrInfo) {
        try {
            if(null!=bizQrInfo) {
                switch (bizQrInfo.getQrFrom()) {
                    //返利网
                    case 0:
                        String tokenflw = TokenCheckUtil.reqTokenGenerate(bizQrInfo.getQrId(),bizQrInfo.getValQrStatus()==2?1:0,appProperties.getRebatesalt());
                        ReqFlwQrValBack  reqFlwQrValBack = new ReqFlwQrValBack();
                        reqFlwQrValBack.setQrId(bizQrInfo.getQrId());
                        reqFlwQrValBack.setStatus(bizQrInfo.getValQrStatus()==2?1:0);
                        reqFlwQrValBack.setToken(tokenflw);
                        return  rebateServiceApiStatic.notfiFlwQrValStatus(reqFlwQrValBack);
                    //平台
                    case 1:
                         ReqPtQrValBack reqPtQrValBack = new ReqPtQrValBack();
                         reqPtQrValBack.setOid(bizQrInfo.getHandicapCode());
                         reqPtQrValBack.setUid(Long.parseLong(bizQrInfo.getUid()));
                         reqPtQrValBack.setQr_id(bizQrInfo.getQrId());
                         reqPtQrValBack.setStatus(bizQrInfo.getValQrStatus()==2?1:0);
                         reqPtQrValBack.setOpt_time(System.currentTimeMillis());
                       //  String tokenpt = TokenCheckUtil.reqTokenGenerate(reqPtQrValBack.getOid(),reqPtQrValBack.getUid(),reqPtQrValBack.getQr_id(),reqPtQrValBack.getStatus(),reqPtQrValBack.getOpt_time(), appProperties.getKeystore());
                        // reqPtQrValBack.setToken(tokenpt);
                         return  platformServiceApiStatic.notifyQrCheckResult(reqPtQrValBack);
                    default:
                        return 0;
                }
            }
        }catch (Exception e){
            log.error("[通知返利网和平台二维码验证状态] 错误：{}",e.getMessage(),e);
        }
        return 0;
    }


    /**
     * 请求验证时保存兼职二维码信息到account & accountMore
     * 0.1个兼职只能绑定有1个支付宝或微信账号,微信/支付宝账号可能同名。
     * 1.兼职微信支付宝账号，默认绑定盘口more表的盘口
     * @return
     */
    @Override
    public BizAccount saveOrUpdateAccountAndAccountMore(ReqRebateUserWxZfbAccount reqRebateUserWxZfbAccount) throws Exception {
       BizAccount  bizAccountDb = null;
       int accountType =  reqRebateUserWxZfbAccount.getQrType()==0? AccountType.InAccountFlwWx.getTypeId() :reqRebateUserWxZfbAccount.getQrType()==1?AccountType.InAccountFlwZfb.getTypeId():AccountType.UnKnow.getTypeId();
       String bankName = reqRebateUserWxZfbAccount.getQrType()==0?"微信收款码":reqRebateUserWxZfbAccount.getQrType()==1?"支付宝收款码":"未知";

        //同步微信支付宝，必须先绑定银行卡。既more会存在数据。
        BizAccountMore moreDb = accountMoreSer.getFromByUid(reqRebateUserWxZfbAccount.getUid());
        if(moreDb==null || StringUtils.isBlank(moreDb.getAccounts())){
            log.error("保存返利网同步微信支付宝账号错误，more空。参数：{}",JSON.toJSONString(reqRebateUserWxZfbAccount));
            throw new RuntimeException("无法查到账号已有信息。");
        }

        List<Integer> accounts = Stream.of(StringUtils.trimToEmpty(moreDb.getAccounts()).split(",")).filter(StringUtils::isNumeric).map(Integer::valueOf).collect(Collectors.toList());
        List<BizAccount> moreAccounts = accountService.findByIds(new ArrayList<>(accounts));
        List<BizAccount> moreAccountsWithFilterAccount = moreAccounts==null?null:moreAccounts.stream().filter(x ->
            x.getAccount().equalsIgnoreCase(reqRebateUserWxZfbAccount.getAccount()) && x.getType()==accountType
         ).collect(Collectors.toList());
        if(null!=moreAccountsWithFilterAccount && moreAccountsWithFilterAccount.size()>1){
            log.error("保存返利网同步微信支付宝账号错误,more表关联的account存在多个相同账号名称的account。参数：{}",JSON.toJSONString(reqRebateUserWxZfbAccount));
            throw new RuntimeException("严重：存在多个账号信息。"+reqRebateUserWxZfbAccount.getAccount());
        }

        if(null==moreAccountsWithFilterAccount || CollectionUtils.isEmpty(moreAccountsWithFilterAccount))   bizAccountDb = new BizAccount();
        if(null!=moreAccountsWithFilterAccount && moreAccountsWithFilterAccount.size()==1) bizAccountDb=moreAccountsWithFilterAccount.get(0);


        //设置新的account信息
        String  accountRemark = StringUtils.isNotBlank(bizAccountDb.getRemark())?bizAccountDb.getRemark():"";
        if(Objects.isNull(bizAccountDb.getId())){
            bizAccountDb.setStatus(AccountStatus.Activated.getStatus()); //已激活，（返利网支付宝/微信不需要激活）
            bizAccountDb.setMobile(moreDb.getMoible());  //关联手机号
            bizAccountDb.setBankBalance(BigDecimal.ZERO); //银行余额0
            bizAccountDb.setBalance(BigDecimal.ZERO); //系统余额0
            bizAccountDb.setCreateTime(new Date()); //创建时间
            bizAccountDb.setAlias(getNewAlias());  //设置别名
            bizAccountDb.setHandicapId(moreDb.getHandicap()); //默认more.uid的盘口
            bizAccountDb.setBankName(bankName); //银行名称
            bizAccountDb.setType(accountType); //返利网同步微信账号/支付宝账号
            bizAccountDb.setFlag(AccountFlag.REFUND.getTypeId());  //返利网
            accountRemark=CommonUtils.genRemark(accountRemark, "【账号创建】" , new Date(),"[返利网同步]");
        }else{
            accountRemark=CommonUtils.genRemark(accountRemark, "【更新账号】" , new Date(),"[返利网同步]");
        }
        bizAccountDb.setUpdateTime(new Date());  //设置更新时间
        bizAccountDb.setAccount(reqRebateUserWxZfbAccount.getAccount()); //设置账号/微信支付宝
        bizAccountDb.setQrContent(reqRebateUserWxZfbAccount.getQrContent().trim()); //设置二维码信息
        bizAccountDb.setOwner(reqRebateUserWxZfbAccount.getName()); //真实姓名
        bizAccountDb.setHub_(FundTransferEncrypter.encryptDb(StringUtils.trimToEmpty(deTpwd(reqRebateUserWxZfbAccount.getTpwd())))); //设置交易密码
        bizAccountDb.setRemark(accountRemark);  //设置备注

        BizAccount bizAccount = accountService.saveRebateAcc(bizAccountDb, moreDb, reqRebateUserWxZfbAccount.getUid(), AccountFlag.REFUND); //保存
        return  bizAccount;
    }


    /**
     * 获取别名
     */
    public synchronized String  getNewAlias(){
        String maxAlias = accountService.getMaxAlias();
        if (StringUtils.isEmpty(maxAlias) || maxAlias.equals("0")) {
            return "100000";
        } else {
            return Integer.toString(Integer.parseInt(maxAlias) + 1).replace("4", "5");
        }
    }


    /**
     * 二维码查询
     */
    public  List<BizQrInfo> findAllByUidAndQrStatusAndValQrStatusAndQrContentIn(String uid,int qrStatus,int valQrStatus,String[] qrContent){
        return bizQrInfoRepository.findAllByUidAndQrStatusAndValQrStatusAndQrContentIn(uid,qrStatus,valQrStatus,qrContent);
    }







    /*
     * loginPwd 在第5位插入3个随机0-9的数字 然后用base64加密
     */
    private String deLpwd(String lped) {
        if (StringUtils.isBlank(lped))
            return null;
        lped = new String(org.apache.mina.util.Base64.decodeBase64(lped.getBytes()));
        return lped.substring(0, 5) + lped.substring(8);
    }

    /*
     * payPwd 在第2位插入一个随机0-9的数字 然后用base64加密
     */
    private String deTpwd(String tpwd) {
        if (StringUtils.isBlank(tpwd))
            return null;
        tpwd = new String(org.apache.mina.util.Base64.decodeBase64(tpwd.getBytes()));
        return tpwd.substring(0, 2) + tpwd.substring(3);
    }

}
