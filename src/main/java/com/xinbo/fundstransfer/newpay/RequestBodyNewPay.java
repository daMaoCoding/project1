package com.xinbo.fundstransfer.newpay;

import com.xinbo.fundstransfer.ali4enterprise.inputdto.CommonInputDTO;
import com.xinbo.fundstransfer.domain.pojo.AddForCrkInputDTO;
import com.xinbo.fundstransfer.domain.pojo.FindForOfb4DemandInputDTO;
import com.xinbo.fundstransfer.domain.pojo.FindForOfbInputDTO;
import com.xinbo.fundstransfer.domain.pojo.FindForOidInputDTO;
import com.xinbo.fundstransfer.newpay.inputdto.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by Administrator on 2018/7/11.
 */
@Slf4j
@Component
public class RequestBodyNewPay {
    @Autowired
    private Token4NewPay token4NewPay;

    // 1.1.1 新增客户资料 body
    public RequestBody addRequestBody(AddNewPayInputDTO inputDTO) {
        String json = "{\"oid\":" + inputDTO.getOid() + ",\"contactName\":\"" + inputDTO.getContactName()
                + "\",\"tel\":\"" + inputDTO.getTel() + "\",\"credits\":" + inputDTO.getCredits() + ",\"type\":"
                + inputDTO.getType() + ",\"level\":" + inputDTO.getLevel();
        if (StringUtils.isNotBlank(inputDTO.getCommissionBankNum())) {
            json += ",\"commissionBankNum\":\"" + inputDTO.getCommissionBankNum() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getCommissionOpenMan())) {
            json += ",\"commissionOpenMan\":\"" + inputDTO.getCommissionOpenMan() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getCommissionBankName())) {
            json += ",\"commissionBankName\":\"" + inputDTO.getCommissionBankName() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getWechatAccount())) {
            json += ",\"wechatAccount\":\"" + inputDTO.getWechatAccount() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getWechatName())) {
            json += ",\"wechatName\":\"" + inputDTO.getWechatName() + "\"";
        }
        if (inputDTO.getWechatInLimit() != null) {
            json += ",\"wechatInLimit\":" + inputDTO.getWechatInLimit();
        }
        if (inputDTO.getWechatBalanceAlarm() != null) {
            json += ",\"wechatBalanceAlarm\":" + inputDTO.getWechatBalanceAlarm();
        }
        if (StringUtils.isNotBlank(inputDTO.getWechatLoginPassword())) {
            json += ",\"wechatLoginPassword\":\"" + inputDTO.getWechatLoginPassword() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getWechatPaymentPassword())) {
            json += ",\"wechatPaymentPassword\":\"" + inputDTO.getWechatPaymentPassword() + "\"";
        }
        if (inputDTO.getWechatQrDrawalMethod() != null) {
            json += ",\"wechatQrDrawalMethod\":" + inputDTO.getWechatQrDrawalMethod();
        }
        if (inputDTO.getWechatBankDrawalMethod() != null) {
            json += ",\"wechatBankDrawalMethod\":" + inputDTO.getWechatBankDrawalMethod();
        }
        if (StringUtils.isNotBlank(inputDTO.getAlipayAccount())) {
            json += ",\"alipayAccount\":\"" + inputDTO.getAlipayAccount() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getAlipayName())) {
            json += ",\"alipayName\":\"" + inputDTO.getAlipayName() + "\"";
        }
        if (inputDTO.getAlipayInLimit() != null) {
            json += ",\"alipayInLimit\":" + inputDTO.getAlipayInLimit();
        }
        if (inputDTO.getAlipayBalanceAlarm() != null) {
            json += ",\"alipayBalanceAlarm\":" + inputDTO.getAlipayBalanceAlarm();
        }
        if (StringUtils.isNotBlank(inputDTO.getAlipayLoginPassword())) {
            json += ",\"alipayLoginPassword\":\"" + inputDTO.getAlipayLoginPassword() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getAlipayPaymentPassword())) {
            json += ",\"alipayPaymentPassword\":\"" + inputDTO.getAlipayPaymentPassword() + "\"";
        }
        if (inputDTO.getAlipayQrDrawalMethod() != null) {
            json += ",\"alipayQrDrawalMethod\":" + inputDTO.getAlipayQrDrawalMethod();
        }
        if (inputDTO.getAlipayBankDrawalMethod() != null) {
            json += ",\"alipayBankDrawalMethod\":" + inputDTO.getAlipayBankDrawalMethod();
        }
        if (inputDTO.getBankId() != null) {
            json += ",\"bankId\":" + inputDTO.getBankId();
        }
        if (StringUtils.isNotBlank(inputDTO.getBankAccount())) {
            json += ",\"bankAccount\":\"" + inputDTO.getBankAccount() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getOpenMan())) {
            json += ",\"openMan\":\"" + inputDTO.getOpenMan() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getBankName())) {
            json += ",\"bankName\":\"" + inputDTO.getBankName() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getBankOpen())) {
            json += ",\"bankOpen\":\"" + inputDTO.getBankOpen() + "\"";
        }
        if (inputDTO.getBankBalanceAlarm() != null) {
            json += ",\"bankBalanceAlarm\":" + inputDTO.getBankBalanceAlarm();
        }
        if (StringUtils.isNotBlank(inputDTO.getBankLoginPassword())) {
            json += ",\"bankLoginPassword\":\"" + inputDTO.getBankLoginPassword() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getBankPaymentPassword())) {
            json += ",\"bankPaymentPassword\":\"" + inputDTO.getBankPaymentPassword() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getBankPassword())) {
            json += ",\"bankPassword\":\"" + inputDTO.getBankPassword() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getUshieldPassword())) {
            json += ",\"uShieldPassword\":\"" + inputDTO.getUshieldPassword() + "\"";
        }
        if (inputDTO.getYlbThreshold() != null) {
            json += ",\"ylbThreshold\":\"" + inputDTO.getYlbThreshold() + "\"";
        }
        if (inputDTO.getYlbInterval() != null) {
            json += ",\"ylbInterval\":\"" + inputDTO.getYlbInterval() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getZfbAlias1())) {
            json += ",\"zfbAlias1\":\"" + inputDTO.getZfbAlias1() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getZfbAlias2())) {
            json += ",\"zfbAlias2\":\"" + inputDTO.getZfbAlias2() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getWxAlias1())) {
            json += ",\"wxAlias1\":\"" + inputDTO.getWxAlias1() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getWxAlias1())) {
            json += ",\"wxAlias2\":\"" + inputDTO.getWxAlias2() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getWxAlias1())) {
            json += ",\"wxAlias3\":\"" + inputDTO.getWxAlias3() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getUid())) {
            json += ",\"uid\":\"" + inputDTO.getUid() + "\"";
        }
        if (inputDTO.getIsEpAlipay() != null) {
            json += ",\"isEpAlipay\":" + inputDTO.getIsEpAlipay();
        }
        if (inputDTO.getYsfBankDrawalMethod() != null) {
            json += ",\"ysfBankDrawalMethod\":" + inputDTO.getYsfBankDrawalMethod();
        }
        if (inputDTO.getYsfQrDrawalMethod() != null) {
            json += ",\"ysfQrDrawalMethod\":" + inputDTO.getYsfQrDrawalMethod();
        }
        if (inputDTO.getYsfBalanceAlarm() != null) {
            json += ",\"ysfBalanceAlarm\":" + inputDTO.getYsfBalanceAlarm();
        }
        if (null != inputDTO.getYsfInLimit()) {
            json += ",\"ysfInLimit\":" + inputDTO.getYsfInLimit();
        }
        if (StringUtils.isNotBlank(inputDTO.getYsfTradepass())) {
            json += ",\"ysfTradepass\":\"" + inputDTO.getYsfTradepass() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getYsfLoginpass())) {
            json += ",\"ysfLoginpass\":\"" + inputDTO.getYsfLoginpass() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getYsfAccount())) {
            json += ",\"ysfAccount\":\"" + inputDTO.getYsfAccount() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getYsfName())) {
            json += ",\"ysfName\":\"" + inputDTO.getYsfName() + "\"";
        }
        json += "}";
        log.info("addRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.1.2 修改基本信息
    public RequestBody modifyInfoRequestBody(ModifyInfoInputDTO inputDTO) {
        String json = "{\"oid\":" + inputDTO.getOid() + ",\"id\":" + inputDTO.getId() + ",\"contactName\":\""
                + inputDTO.getContactName() + "\",\"tel\":\"" + inputDTO.getTel() + "\",\"type\":\""
                + inputDTO.getType() + "\",\"status\":" + inputDTO.getStatus() + ",\"credits\":" + inputDTO.getCredits()
                + ",\"level\":" + inputDTO.getLevel();
        if (StringUtils.isNotBlank(inputDTO.getCommissionBankName())) {
            json += ",\"commissionBankName\":\"" + inputDTO.getCommissionBankName() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getCommissionBankNum())) {
            json += ",\"commissionBankNum\":\"" + inputDTO.getCommissionBankNum() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getCommissionOpenMan())) {
            json += ",\"commissionOpenMan\":\"" + inputDTO.getCommissionOpenMan() + "\"";
        }
        json += "}";
        log.info("modifyInfoRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.1.3 修改账号资料
    public RequestBody modifyAccountRequestBody(ModifyAccountInputDTO inputDTO) {
        String json = "{\"oid\":" + inputDTO.getOid() + ",\"mobileId\":" + inputDTO.getMobileId() + ",\"type\":"
                + inputDTO.getType() + ",\"account\":\"" + inputDTO.getAccount() + "\",\"name\":\"" + inputDTO.getName()
                + "\"";

        if (inputDTO.getAccountId() != null) {
            json += ",\"accountId\":" + inputDTO.getAccountId();
        }
        if (inputDTO.getInLimit() != null) {
            json += ",\"inLimit\":" + inputDTO.getInLimit();
        }
        if (inputDTO.getBalanceAlarm() != null) {
            json += ",\"balanceAlarm\":" + inputDTO.getBalanceAlarm();
        }
        if (inputDTO.getQrDrawalMethod() != null) {
            json += ",\"qrDrawalMethod\":" + inputDTO.getQrDrawalMethod();
        }
        if (inputDTO.getBankDrawalMethod() != null) {
            json += ",\"bankDrawalMethod\":" + inputDTO.getBankDrawalMethod();
        }
        if (inputDTO.getBankId() != null) {
            json += ",\"bankId\":" + inputDTO.getBankId();
        }
        if (StringUtils.isNotBlank(inputDTO.getOpenMan())) {
            json += ",\"openMan\":\"" + inputDTO.getOpenMan() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getBankName())) {
            json += ",\"bankName\":\"" + inputDTO.getBankName() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getBankOpen())) {
            json += ",\"bankOpen\":\"" + inputDTO.getBankOpen() + "\"";
        }
        if (inputDTO.getYlbThreshold() != null) {
            json += ",\"ylbThreshold\":\"" + inputDTO.getYlbThreshold() + "\"";
        }
        if (inputDTO.getYlbInterval() != null) {
            json += ",\"ylbInterval\":\"" + inputDTO.getYlbInterval() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getZfbAlias1())) {
            json += ",\"zfbAlias1\":\"" + inputDTO.getZfbAlias1() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getZfbAlias2())) {
            json += ",\"zfbAlias2\":\"" + inputDTO.getZfbAlias2() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getWxAlias1())) {
            json += ",\"wxAlias1\":\"" + inputDTO.getWxAlias1() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getWxAlias1())) {
            json += ",\"wxAlias2\":\"" + inputDTO.getWxAlias2() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getWxAlias1())) {
            json += ",\"wxAlias3\":\"" + inputDTO.getWxAlias3() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getUid())) {
            json += ",\"uid\":\"" + inputDTO.getUid() + "\"";
        }
        if (inputDTO.getIsEpAlipay() != null) {
            json += ",\"isEpAlipay\":" + inputDTO.getIsEpAlipay();
        }
        if (inputDTO.getYsfBalanceAlarm() != null) {
            json += ",\"ysfBalanceAlarm\":" + inputDTO.getYsfBalanceAlarm();
        }
        if (null != inputDTO.getYsfInLimit()) {
            json += ",\"ysfInLimit\":" + inputDTO.getYsfInLimit();
        }
        if (StringUtils.isNotBlank(inputDTO.getYsfTradepass())) {
            json += ",\"ysfTradepass\":\"" + inputDTO.getYsfTradepass() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getYsfLoginpass())) {
            json += ",\"ysfLoginpass\":\"" + inputDTO.getYsfLoginpass() + "\"";
        }
        json += "}";
        log.info("modifyAccountRequestBody json value:{}", json);

        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.1.4 修改状态
    public RequestBody modifyStatusRequestBody(ModifyStatusInputDTO inputDTO) {
        String json = "{\"oid\":" + inputDTO.getOid() + ",\"accountId\":" + inputDTO.getAccountId() + ",\"type\":"
                + inputDTO.getType() + ",\"status\":" + inputDTO.getStatus() + "}";
        json += "}";
        log.info("modifyStatusRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.1.5 修改密码
    public RequestBody modifyPwdRequestBody(ModifyPWDInputDTO inputDTO) {
        String json = "{\"oid\":" + inputDTO.getOid() + ",\"id\":" + inputDTO.getId();
        if (StringUtils.isNotBlank(inputDTO.getWechatLoginPassword())) {
            json += ",\"wechatLoginPassword\":\"" + inputDTO.getWechatLoginPassword() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getWechatPaymentPassword())) {
            json += ",\"wechatPaymentPassword\":\"" + inputDTO.getWechatPaymentPassword() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getAlipayLoginPassword())) {
            json += ",\"alipayLoginPassword\":\"" + inputDTO.getAlipayLoginPassword() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getAlipayPaymentPassword())) {
            json += ",\"alipayPaymentPassword\":\"" + inputDTO.getAlipayPaymentPassword() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getBankLoginPassword())) {
            json += ",\"bankLoginPassword\":\"" + inputDTO.getBankLoginPassword() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getBankPaymentPassword())) {
            json += ",\"bankPaymentPassword\":\"" + inputDTO.getBankPaymentPassword() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getBankPassword())) {
            json += ",\"bankPassword\":\"" + inputDTO.getBankPassword() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getUshieldPassword())) {
            json += ",\"uShieldPassword\":\"" + inputDTO.getUshieldPassword() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getYsfLoginPassword())) {
            json += ",\"ysfLoginPassword\":\"" + inputDTO.getYsfLoginPassword() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getYsfPaymentPassword())) {
            json += ",\"ysfPaymentPassword\":\"" + inputDTO.getYsfPaymentPassword() + "\"";
        }
        json += "}";
        log.info("modifyPwdRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.1.6 删除
    public RequestBody removeRequestBody(RemoveInputDTO inputDTO) {
        String json = "{\"oid\":" + inputDTO.getOid() + ",\"id\":" + inputDTO.getId() + "}";
        log.info("removeRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.1.7 条件查询分页列表
    public RequestBody findByConditionRequestBody(FindByConditionInputDTO inputDTO) {
        String json = "{\"pageNo\":" + inputDTO.getPageNo() + ",\"pageSize\":" + inputDTO.getPageSize() + "";
        if (inputDTO.getOid() != null) {
            json += ",\"oid\":\"" + inputDTO.getOid() + "\"";
        }
        if (inputDTO.getLevel() != null) {
            json += ",\"level\":" + inputDTO.getLevel();
        }
        if (StringUtils.isNotBlank(inputDTO.getBankAccount())) {
            json += ",\"bankAccount\":\"" + inputDTO.getBankAccount() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getWechatAccount())) {
            json += ",\"wechatAccount\":\"" + inputDTO.getWechatAccount() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getAlipayAccount())) {
            json += ",\"alipayAccount\":\"" + inputDTO.getAlipayAccount() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getTel())) {
            json += ",\"tel\":\"" + inputDTO.getTel() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getStatuses())) {
            json += ",\"statuses\":\"" + inputDTO.getStatuses() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getTypes())) {
            json += ",\"types\":\"" + inputDTO.getTypes() + "\"";
        }
        if (inputDTO.getIsEpAlipay() != null) {
            json += ",\"isEpAlipay\":" + inputDTO.getIsEpAlipay();
        }
        json += "}";
        log.info("findByConditionRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.1.8 查询微信、支付宝绑定的银行卡
    public RequestBody findBankRequestBody(FindBankInputDTO inputDTO) {
        String json = "{\"oid\":" + inputDTO.getOid() + ",\"id\":" + inputDTO.getId() + "}";
        log.info("findBankRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.1.9 查询兼职人员微信、支付宝、银行卡的余额、转入转出金额
    public RequestBody findBalanceInfoRequestBody(FindBalanceInfoInputDTO inputDTO) {
        String json = "{\"oid\":" + inputDTO.getOid() + ",\"id\":" + inputDTO.getId() + "}";
        log.info("findBalanceInfoRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.1.10 查询账号信息，account带*
    public RequestBody findAccountInfoRequestBody(FindAccountInfoInputDTO inputDTO) {
        String json = "{\"oid\":" + inputDTO.getOid() + ",\"id\":" + inputDTO.getId() + ",\"type\":"
                + inputDTO.getType() + "}";
        log.info("findAccountInfoRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.1.11 查询账号信息，account没有带*
    public RequestBody findAccountInfo2RequestBody(FindAccountInfoInputDTO inputDTO) {
        String json = "{\"oid\":" + inputDTO.getOid() + ",\"id\":" + inputDTO.getId() + ",\"type\":"
                + inputDTO.getType();
        if (inputDTO.getIsEpAlipay() != null) {
            json += ",\"isEpAlipay\":" + inputDTO.getIsEpAlipay();
        }
        json += "}";
        log.info("findAccountInfo2RequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.1.12 查询手机信息
    public RequestBody findTelInfoRequestBody(FindTelInfoInputDTO inputDTO) {
        String json = "{\"oid\":" + inputDTO.getOid() + ",\"id\":" + inputDTO.getId() + "}";
        log.info("findTelInfoRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.1.13 点击今日收款/佣金列
    public RequestBody find8ByConditionRequestBody(Find8ByConditionInputDTO inputDTO) {
        String json = "{\"oid\":" + inputDTO.getOid() + ",\"inoutType\":" + inputDTO.getInoutType() + ",\"type\":"
                + inputDTO.getType() + ",\"mobileId\":" + inputDTO.getMobileId() + ",\"timeStart\":"
                + inputDTO.getTimeStart() + ",\"timeEnd\":" + inputDTO.getTimeEnd() + ",\"pageNo\":"
                + inputDTO.getPageNo() + ",\"pageSize\":" + inputDTO.getPageSize();
        if (inputDTO.getMoneyStart() != null) {
            json += ",\"moneyStart\":" + inputDTO.getMoneyStart();
        }
        if (inputDTO.getMoneyEnd() != null) {
            json += ",\"moneyEnd\":" + inputDTO.getMoneyEnd();
        }
        json += "}";
        log.info("find8ByConditionRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.1.14 点击今日收款/佣金列，返佣记录
    public RequestBody findCommissionDetailByConditionRequestBody(FindCommissionDetailInputDTO inputDTO) {
        String json = "{\"pageNo\":" + inputDTO.getPageNo() + ",\"pageSize\":" + inputDTO.getPageSize() + ",\"oid\":"
                + inputDTO.getOid() + ",\"mobileId\":" + inputDTO.getMobileId();
        if (inputDTO.getMoneyStart() != null) {
            json += ",\"moneyStart\":" + inputDTO.getMoneyStart();
        }
        if (inputDTO.getMoneyEnd() != null) {
            json += ",\"moneyEnd\":" + inputDTO.getMoneyEnd();
        }
        if (inputDTO.getTimeStart() != null) {
            json += ",\"timeStart\":" + inputDTO.getTimeStart();
        }
        if (inputDTO.getTimeEnd() != null) {
            json += ",\"timeEnd\":" + inputDTO.getTimeEnd();
        }
        json += "}";
        log.info("findCommissionDetailByConditionRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.1.15 查询二维码分页列表
    public RequestBody findQRByConditionRequestBody(FindQRByConditionInputDTO inputDTO) {
        String json = "{\"oid\":" + inputDTO.getOid() + ",\"type\":" + inputDTO.getType() + ",\"mobileId\":"
                + inputDTO.getMobileId() + ",\"accountId\":" + inputDTO.getAccountId() + ",\"pageNo\":"
                + inputDTO.getPageNo() + ",\"pageSize\":" + inputDTO.getPageSize();
        if (inputDTO.getMoneyStart() != null) {
            json += ",\"moneyStart\":" + inputDTO.getMoneyStart();
        }
        if (inputDTO.getMoneyEnd() != null) {
            json += ",\"moneyEnd\":" + inputDTO.getMoneyEnd();
        }
        json += "}";
        log.info("findQRByConditionRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.1.16 批量生成二维码地址
    public RequestBody batchAddQRRequestBody(BatchAddQRInputDTO inputDTO) {
        String json = "{\"oid\":" + inputDTO.getOid() + ",\"type\":" + inputDTO.getType() + ",\"accountId\":"
                + inputDTO.getAccountId() + ",\"moneySet\":[";
        if (inputDTO.getMoneySet().size() > 0) {
            List<Double> list = new ArrayList<>(inputDTO.getMoneySet());
            for (int i = 0; i < list.size(); i++) {
                if (i < list.size() - 1) {
                    json += list.get(i) + ",";
                } else {
                    json += list.get(i) + "]";
                }
            }
        }
        if (inputDTO.getQrCodeCount() != null) {
            json += ",\"qrCodeCount\":" + inputDTO.getQrCodeCount();
        }
        json += "}";
        log.info("batchAddQRRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.1.17 批量删除二维码
    public RequestBody batchRemoveAnymoreRequestBody(BatchDeleteQRInputDTO inputDTO) {
        String json = "[";
        if (!CollectionUtils.isEmpty(inputDTO.getList())) {
            for (int i = 0; i < inputDTO.getList().size(); i++) {
                BatchDeleteQRInputDTOInner dto = inputDTO.getList().get(i);
                if (i < inputDTO.getList().size() - 1) {
                    json += "{\"id\":" + dto.getId() + ",\"oid\":" + dto.getOid() + "},";
                } else {
                    json += "{\"id\":" + dto.getId() + ",\"oid\":" + dto.getOid() + "}";
                }
            }
        }
        json += "]";
        log.info("batchRemoveAnymoreRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }
    // 1.1.18 查询银行下拉列表 无

    // 1.1.19 查询密码是否已被设置
    public RequestBody findPwdExistsRequestBody(FindPwdExistsInputDTO inputDTO) {
        String json = "{\"oid\":" + inputDTO.getOid() + ",\"id\":" + inputDTO.getId() + "}";
        log.info("findPwdExistsRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.1.20 修改收款理由前缀后缀
    public RequestBody modifyInfoRequestBody(ModifyFixInputDTO inputDTO) {
        String json = "{\"oid\":" + inputDTO.getOid() + ",\"mobileId\":" + inputDTO.getMobileId() + ",\"prefix\":\""
                + inputDTO.getPrefix() + "\",\"suffix\":\"" + inputDTO.getSuffix() + "\"";
        json += ",\"chkType\":" + inputDTO.getChkType();
        json += "}";
        log.info("modifyInfoRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.1.21 查询业主未删除的新支付通道
    public RequestBody findPOCForCrkRequestBody(FindPOCForCrkInputDTO inputDTO) {
        String json = "{";
        json += "\"oid\":" + inputDTO.getOid();
        json += ",\"type\":" + inputDTO.getType();
        if (inputDTO.getIsEpAlipay() != null) {
            json += ",\"isEpAlipay\":" + inputDTO.getIsEpAlipay();
        }
        json += "}";
        log.info("findPOCForCrkRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.1.22 绑定支付通道和客户资料
    public RequestBody newpayAisleConfigBindRequestBody(NewpayAisleConfigBindInputDTO inputDTO) {
        String json = "{\"oid\":" + inputDTO.getOid() + ",\"mobileId\":" + inputDTO.getMobileId() + ",\"type\":"
                + inputDTO.getType() + ",\"ocIdCol\":" + inputDTO.getOcIdCol().toString() + ",\"operationAdminId\":"
                + inputDTO.getOperationAdminId() + ",\"operationAdminName\":\"" + inputDTO.getOperationAdminName()
                + "\"}";
        log.info("newpayAisleConfigBindRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.1.23 查询客户资料已绑定的支付通道
    public RequestBody newpayAisleConfigFindBindRequestBody(NewpayAisleConfigFindBindInputDTO inputDTO) {
        String json = "{ \"oid\":" + inputDTO.getOid() + ",\"mobileId\":" + inputDTO.getMobileId() + ",\"type\":"
                + inputDTO.getType() + "}";
        log.info("newpayAisleConfigFindBindRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.1.24 银行的余额同步
    public RequestBody syncBankBalanceRequestBody(SyncBankBalanceInputDTO inputDTO) {
        String token = token4NewPay.getToken4NewPay(
                new Object[]{inputDTO.getAccount(), inputDTO.getBalance(), inputDTO.getSysBalance()});
        log.info("RequestBodyNewPay.syncBankBalanceRequestBody token is :{}.", token);
        if (StringUtils.isBlank(token)) {
            return null;
        }
        String json = "{\"oid\":" + inputDTO.getOid() + ",\"account\":\"" + inputDTO.getAccount() + "\""
                + ",\"balance\":" + inputDTO.getBalance() + ",\"sysBalance\":" + inputDTO.getSysBalance()
                + ",\"token\":\"" + token + "\"}";
        log.info("syncBankBalanceRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);

    }

    // 1.1.25 形容词名词 - 新增
    public RequestBody contentAdd(AddContentInputDTO inputDTO) {
        String json = "{";
        json += "\"oid\":" + inputDTO.getOid();
        json += ",\"type\":" + inputDTO.getType();
        if (null != inputDTO.getTypeId()) {
            json += ",\"typeId\":" + inputDTO.getTypeId();
        }
        json += ",\"content\":\"" + inputDTO.getContent() + "\"";
        json += ",\"operationAdminId\":" + inputDTO.getOperationAdminId();
        json += ",\"operationAdminName\":\"" + inputDTO.getOperationAdminName() + "\"";
        json += "}";
        log.info("contentAdd json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.1.26 形容词名词 – 修改
    public RequestBody contentModify(ModifyContentInputDTO inputDTO) {
        String json = "{";
        json += "\"oid\":" + inputDTO.getOid();
        json += ",\"id\":" + inputDTO.getId();
        json += ",\"content\":\"" + inputDTO.getContent() + "\"";
        json += ",\"operationAdminId\":" + inputDTO.getOperationAdminId();
        json += ",\"operationAdminName\":\"" + inputDTO.getOperationAdminName() + "\"";
        json += "}";
        log.info("contentModify json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.1.27 形容词名词 – 启用、停用
    public RequestBody contentEnable(EnableContentInputDTO inputDTO) {
        String json = "{";
        json += "\"oid\":" + inputDTO.getOid();
        json += ",\"id\":" + inputDTO.getId();
        json += ",\"status\":" + inputDTO.getStatus();
        json += ",\"operationAdminId\":" + inputDTO.getOperationAdminId();
        json += ",\"operationAdminName\":\"" + inputDTO.getOperationAdminName() + "\"";
        json += "}";
        log.info("contentEnable json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.1.28 形容词名词 – 删除
    public RequestBody contentRemove(RemoveContentInputDTO inputDTO) {
        String json = "{";
        json += "\"oid\":" + inputDTO.getOid();
        json += ",\"id\":" + inputDTO.getId();
        json += ",\"operationAdminId\":" + inputDTO.getOperationAdminId();
        json += ",\"operationAdminName\":\"" + inputDTO.getOperationAdminName() + "\"";
        json += "}";
        log.info("contentRemove json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.1.29 形容词名词 – 分页查询
    public RequestBody findContentByCondition(FindContentByConditionInputDTO inputDTO) {
        String json = "{";
        json += "\"oid\":" + inputDTO.getOid();
        json += ",\"type\":" + inputDTO.getType();
        if (null != inputDTO.getStatus()) {
            json += ",\"status\":" + inputDTO.getStatus();
        }
        if (StringUtils.isNotEmpty(inputDTO.getContent())) {
            json += ",\"content\":\"" + inputDTO.getContent() + "\"";
        }
        if (StringUtils.isNotEmpty(inputDTO.getAdminName())) {
            json += ",\"adminName\":\"" + inputDTO.getAdminName() + "\"";
        }
        if (StringUtils.isNotEmpty(inputDTO.getTypeName())) {
            json += ",\"typeName\":\"" + inputDTO.getTypeName() + "\"";
        }
        if (null != inputDTO.getUptimeStart()) {
            json += ",\"uptimeStart\":" + inputDTO.getUptimeStart();
        }
        if (null != inputDTO.getUptimeEnd()) {
            json += ",\"uptimeEnd\":" + inputDTO.getUptimeEnd();
        }
        json += ",\"pageNo\":" + inputDTO.getPageNo();
        json += ",\"pageSize\":" + inputDTO.getPageSize();
        json += ",\"orderField\":\"uptime\"";
        json += ",\"orderSort\":\"desc\"";
        json += "}";
        log.info("findContentByCondition json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.1.30 修改未确认出款金额开关
    public RequestBody modifyUoFlag(ModifyUoFlagInputDTO inputDTO) {
        String json = "{";
        json += "\"oid\":" + inputDTO.getOid();
        json += ",\"mobileId\":" + inputDTO.getMobileId();
        json += ",\"uoFlag\":" + inputDTO.getUoFlag();
        json += ",\"operationAdminId\":" + inputDTO.getOperationAdminId();
        json += ",\"operationAdminName\":\"" + inputDTO.getOperationAdminName() + "\"";
        json += "}";
        log.info("modifyUoFlag json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.1.31 生成常用金额/非常用金额二维码
    public RequestBody genANMultQr(GenANMultQrInputDTO inputDTO) {
        String json = "{";
        json += "\"oid\":" + inputDTO.getOid();
        json += ",\"type\":" + inputDTO.getType();
        json += ",\"device\":\"" + inputDTO.getDevice() + "\"";
        json += ",\"commonFlag\":" + inputDTO.getCommonFlag();
        json += "}";
        log.info("genANMultQr json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.1.32 统计常用金额、非常用金额已生成二维码个数和总个数
    public RequestBody statisticsMWR(StatisticsMWRInputDTO inputDTO) {
        String json = "{";
        json += "\"oid\":" + inputDTO.getOid();
        json += ",\"mobileId\":" + inputDTO.getMobileId();
        json += ",\"type\":" + inputDTO.getType();
        json += "}";
        log.info("statisticsMWR json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.1.33 形容词类型 – 新增
    public RequestBody addWordType(AddWordTypeInputDTO inputDTO) {
        String json = "{";
        json += "\"oid\":" + inputDTO.getOid();
        json += ",\"typeName\":\"" + inputDTO.getTypeName() + "\"";
        json += ",\"type\":" + inputDTO.getType();
        json += ",\"operationAdminId\":" + inputDTO.getOperationAdminId();
        json += ",\"operationAdminName\":\"" + inputDTO.getOperationAdminName() + "\"";
        json += "}";
        log.info("addWordType json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.1.34 形容词类型 – 查询列表
    public RequestBody findWordType(FindWordTypeInputDTO inputDTO) {
        String json = "{";
        json += "\"oid\":" + inputDTO.getOid();
        json += ",\"type\":" + inputDTO.getType();
        json += "}";
        log.info("findWordType json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.1.35 形容词类型 – 删除
    public RequestBody removeWordType(RemoveWordTypeInputDTO inputDTO) {
        String json = "{";
        json += "\"oid\":" + inputDTO.getOid();
        json += ",\"id\":" + inputDTO.getId();
        json += ",\"operationAdminId\":" + inputDTO.getOperationAdminId();
        json += ",\"operationAdminName\":\"" + inputDTO.getOperationAdminName() + "\"";
        json += "}";
        log.info("removeWordType json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.1.36 兼职绑定词语
    public RequestBody bindingWordType(BindingWordTypeInputDTO inputDTO) {
        String json = "{";
        json += "\"oid\":" + inputDTO.getOid();
        json += ",\"type\":" + inputDTO.getType();
        json += ",\"mobileId\":" + inputDTO.getMobileId();
        json += ",\"wordTypeId\":" + inputDTO.getWordTypeId();
        json += ",\"operationAdminId\":" + inputDTO.getOperationAdminId();
        json += ",\"operationAdminName\":\"" + inputDTO.getOperationAdminName() + "\"";
        json += "}";
        log.info("bindingWordType json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.1.37 查询词库绑定分页列表
    public RequestBody findForBind(FindForBindInputDTO inputDTO) {
        String json = "{";
        json += "\"oid\":" + inputDTO.getOid();
        if (StringUtils.isNotEmpty(inputDTO.getTel())) {
            json += ",\"tel\":\"" + inputDTO.getTel() + "\"";
        }
        json += ",\"pageNo\":" + inputDTO.getPageNo();
        json += ",\"pageSize\":" + inputDTO.getPageSize();
        json += "}";
        log.info("findForBind json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.2.1 查询银行卡分页列表
    public RequestBody findBankCardByConditionRequestBody(FindBankCardByConditionInputDTO inputDTO) {
        String json = "{\"timeStart\":" + inputDTO.getTimeStart() + ",\"timeEnd\":" + inputDTO.getTimeEnd()
                + ",\"pageNo\":" + inputDTO.getPageNo() + ",\"pageSize\":" + inputDTO.getPageSize() + ",\"oid\":\""
                + inputDTO.getOid() + "\"";
        if (StringUtils.isNotBlank(inputDTO.getStatuses())) {
            json += ",\"statuses\":\"" + inputDTO.getStatuses() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getLevels())) {
            json += ",\"levels\":\"" + inputDTO.getLevels() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getConfigTypes())) {
            json += ",\"configTypes\":\"" + inputDTO.getConfigTypes() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getAccount())) {
            json += ",\"account\":\"" + inputDTO.getAccount() + "\"";
        }
        json += "}";
        log.info("findBankCardByConditionRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.2.2 询微信、支付宝分页列表
    public RequestBody findAWByConditionRequestBody(FindAWByConditionInputDTO inputDTO) {
        String json = "{\"timeStart\":" + inputDTO.getTimeStart() + ",\"timeEnd\":" + inputDTO.getTimeEnd()
                + ",\"pageNo\":" + inputDTO.getPageNo() + ",\"pageSize\":" + inputDTO.getPageSize() + ",\"type\":"
                + inputDTO.getType() + ",\"oid\":\"" + inputDTO.getOid() + "\"";
        if (StringUtils.isNotBlank(inputDTO.getStatuses())) {
            json += ",\"statuses\":\"" + inputDTO.getStatuses() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getLevels())) {
            json += ",\"levels\":\"" + inputDTO.getLevels() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getConfigTypes())) {
            json += ",\"configTypes\":\"" + inputDTO.getConfigTypes() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getAccount())) {
            json += ",\"account\":\"" + inputDTO.getAccount() + "\"";
        }
        json += "}";
        log.info("findAWByConditionRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.2.3 银行卡转入记录点击数字
    public RequestBody find2ByConditionRequestBody(Find2ByConditioninputDTO inputDTO) {
        String json = "{\"oid\":" + inputDTO.getOid() + ",\"type\":" + inputDTO.getType() + ",\"accountId\":"
                + inputDTO.getAccountId() + ",\"inoutType\":" + inputDTO.getInoutType() + ",\"status\":"
                + inputDTO.getStatus() + ",\"timeStart\":" + inputDTO.getTimeStart() + ",\"timeEnd\":"
                + inputDTO.getTimeEnd() + ",\"pageNo\":" + inputDTO.getPageNo() + ",\"pageSize\":"
                + inputDTO.getPageSize();

        if (StringUtils.isNotBlank(inputDTO.getAccount())) {
            json += ",\"account\":" + inputDTO.getAccount();
        }
        if (StringUtils.isNotBlank(inputDTO.getCode())) {
            json += ",\"code\":" + inputDTO.getCode();
        }
        if (StringUtils.isNotBlank(inputDTO.getUserName())) {
            json += ",\"userName\":" + inputDTO.getUserName();
        }
        if (inputDTO.getLevel() != null) {
            json += ",\"level\":" + inputDTO.getLevel();
        }
        if (inputDTO.getMoneyStart() != null) {
            json += ",\"moneyStart\":" + inputDTO.getMoneyStart();
        }
        if (inputDTO.getMoneyEnd() != null) {
            json += ",\"moneyEnd\":" + inputDTO.getMoneyEnd();
        }
        json += "}";
        log.info("find2ByConditionRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.2.4 微信、支付宝转入记录点击数字
    public RequestBody findAWIN2ByConditionRequestBody(FindAWIn2ByConditionInputDTO inputDTO) {
        String json = "{\"oid\":" + inputDTO.getOid() + ",\"type\":" + inputDTO.getType() + ",\"accountId\":"
                + inputDTO.getAccountId() + ",\"inoutType\":" + inputDTO.getInoutType() + ",\"status\":"
                + inputDTO.getStatus() + ",\"timeStart\":" + inputDTO.getTimeStart() + ",\"timeEnd\":"
                + inputDTO.getTimeEnd() + ",\"pageNo\":" + inputDTO.getPageNo() + ",\"pageSize\":"
                + inputDTO.getPageSize() + ",\"mobileId\":" + inputDTO.getMobileId();

        if (StringUtils.isNotBlank(inputDTO.getInAccount())) {
            json += ",\"inAccount\":\"" + inputDTO.getInAccount() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getCode())) {
            json += ",\"code\":\"" + inputDTO.getCode() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getUserName())) {
            json += ",\"userName\":\"" + inputDTO.getUserName() + "\"";
        }
        if (inputDTO.getLevel() != null) {
            json += ",\"level\":" + inputDTO.getLevel();
        }
        if (inputDTO.getMoneyStart() != null) {
            json += ",\"moneyStart\":" + inputDTO.getMoneyStart();
        }
        if (inputDTO.getMoneyEnd() != null) {
            json += ",\"moneyEnd\":" + inputDTO.getMoneyEnd();
        }
        json += "}";
        log.info("findAWIN2ByConditionRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.2.5 微信、支付宝、银行卡转出记录点击数字
    public RequestBody find3ByConditionRequestBody(FindAWB3OutByConditionInputDTO inputDTO) {
        String json = "{\"oid\":" + inputDTO.getOid() + ",\"type\":" + inputDTO.getType() + ",\"accountId\":"
                + inputDTO.getAccountId() + ",\"inoutType\":" + inputDTO.getInoutType() + ",\"status\":"
                + inputDTO.getStatus() + ",\"timeStart\":" + inputDTO.getTimeStart() + ",\"timeEnd\":"
                + inputDTO.getTimeEnd() + ",\"pageNo\":" + inputDTO.getPageNo() + ",\"pageSize\":"
                + inputDTO.getPageSize();

        if (StringUtils.isNotBlank(inputDTO.getInAccount())) {
            json += ",\"inAccount\":" + inputDTO.getInAccount();
        }
        if (StringUtils.isNotBlank(inputDTO.getCode())) {
            json += ",\"code\":" + inputDTO.getCode();
        }
        if (StringUtils.isNotBlank(inputDTO.getUserName())) {
            json += ",\"userName\":" + inputDTO.getUserName();
        }
        if (inputDTO.getLevel() != null) {
            json += ",\"level\":" + inputDTO.getLevel();
        }
        if (inputDTO.getMoneyStart() != null) {
            json += ",\"moneyStart\":" + inputDTO.getMoneyStart();
        }
        if (inputDTO.getMoneyEnd() != null) {
            json += ",\"moneyEnd\":" + inputDTO.getMoneyEnd();
        }
        json += "}";
        log.info("find3ByConditionRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.2.6 微信、支付宝流水
    public RequestBody findAWLog3ByConditionRequestBody(FindAWLOG3ByConditionInputDTO inputDTO) {
        String json = "{\"oid\":" + inputDTO.getOid() + ",\"type\":" + inputDTO.getType() + ",\"accountId\":"
                + inputDTO.getAccountId() + ",\"statuses\":\"" + inputDTO.getStatuses() + "\",\"timeStart\":"
                + inputDTO.getTimeStart() + ",\"timeEnd\":" + inputDTO.getTimeEnd() + ",\"pageNo\":"
                + inputDTO.getPageNo() + ",\"pageSize\":" + inputDTO.getPageSize();
        if (inputDTO.getMoneyStart() != null) {
            json += ",\"moneyStart\":" + inputDTO.getMoneyStart();
        }
        if (inputDTO.getMoneyEnd() != null) {
            json += ",\"moneyEnd\":" + inputDTO.getMoneyEnd();
        }
        json += "}";
        log.info("findAWLog3ByConditionRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.2.7 银行卡流水
    public RequestBody find9ByConditionRequestBody(FindBLog9ByConditionInputDTO inputDTO) {
        String json = "{\"oid\":" + inputDTO.getOid() + ",\"mobileId\":" + inputDTO.getMobileId() + ",\"accountId\":"
                + inputDTO.getAccountId() + ",\"statuses\":\"" + inputDTO.getStatuses() + "\",\"timeStart\":"
                + inputDTO.getTimeStart() + ",\"timeEnd\":" + inputDTO.getTimeEnd() + ",\"pageNo\":"
                + inputDTO.getPageNo() + ",\"pageSize\":" + inputDTO.getPageSize();
        if (inputDTO.getMoneyStart() != null) {
            json += ",\"moneyStart\":" + inputDTO.getMoneyStart();
        }
        if (inputDTO.getMoneyEnd() != null) {
            json += ",\"moneyEnd\":" + inputDTO.getMoneyEnd();
        }
        json += "}";
        log.info("find9ByConditionRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.2.8 对账
    public RequestBody verifyAccountRequestBody(VerifyAccountInputDTO inputDTO) {
        String json = "{\"oid\":" + inputDTO.getOid() + ",\"accountId\":" + inputDTO.getAccountId() + ",\"type\":"
                + inputDTO.getType() + "}";
        log.info("verifyAccountRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.3.1 微信、支付宝正在匹配查询分页列表
    public RequestBody find4ByConditionRequestBody(Find4ByConditionInputDTO inputDTO) {
        String json = "{\"type\":" + inputDTO.getType() + ",\"timeStart\":" + inputDTO.getTimeStart() + ",\"timeEnd\":"
                + inputDTO.getTimeEnd() + ",\"pageNo\":" + inputDTO.getPageNo() + ",\"pageSize\":"
                + inputDTO.getPageSize() + ",\"oid\":\"" + inputDTO.getOid() + "\"";
        if (StringUtils.isNotBlank(inputDTO.getCode())) {
            json += ",\"code\":\"" + inputDTO.getCode() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getUserName())) {
            json += ",\"userName\":\"" + inputDTO.getUserName() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getInAccount())) {
            json += ",\"inAccount\":\"" + inputDTO.getInAccount() + "\"";
        }
        if (!CollectionUtils.isEmpty(inputDTO.getInAccountCol())) {
            json += ",\"inAccountCol\":{\"";
            if (inputDTO.getInAccountCol().size() == 1) {
                json += inputDTO.getInAccountCol().get(0) + "\"}";
            } else {
                for (int i = 0, size = inputDTO.getInAccountCol().size(); i < size; i++) {
                    if (i < size - 1) {
                        json += inputDTO.getInAccountCol().get(i) + "\",";
                    } else {
                        json += inputDTO.getInAccountCol().get(i) + "\"}";
                    }
                }
            }
        }
        if (inputDTO.getMoneyStart() != null) {
            json += ",\"moneyStart\":" + inputDTO.getMoneyStart();
        }
        if (inputDTO.getMoneyEnd() != null) {
            json += ",\"moneyEnd\":" + inputDTO.getMoneyEnd();
        }
        json += "}";
        log.info("find4ByConditionRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.3.2 微信、支付宝未匹配、已匹配查询分页列表
    public RequestBody find5ByConditionRequestBody(Find5ByConditionInputDTO inputDTO) {
        String json = "{\"type\":" + inputDTO.getType() + ",\"status\":" + inputDTO.getStatus() + ",\"timeStart\":"
                + inputDTO.getTimeStart() + ",\"timeEnd\":" + inputDTO.getTimeEnd() + ",\"pageNo\":"
                + inputDTO.getPageNo() + ",\"pageSize\":" + inputDTO.getPageSize() + ",\"oid\":\"" + inputDTO.getOid()
                + "\"";
        if (StringUtils.isNotBlank(inputDTO.getCode())) {
            json += ",\"code\":\"" + inputDTO.getCode() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getUserName())) {
            json += ",\"userName\":\"" + inputDTO.getUserName() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getInAccount())) {
            json += ",\"inAccount\":\"" + inputDTO.getInAccount() + "\"";
        }
        if (inputDTO.getMoneyStart() != null
                && StringUtils.isNotBlank(StringUtils.trimToEmpty(inputDTO.getMoneyStart().toString()))) {
            json += ",\"moneyStart\":" + inputDTO.getMoneyStart();
        }
        if (inputDTO.getMoneyEnd() != null
                && StringUtils.isNotBlank(StringUtils.trimToEmpty(inputDTO.getMoneyEnd().toString()))) {
            json += ",\"moneyEnd\":" + inputDTO.getMoneyEnd();
        }
        if (StringUtils.isNotBlank(StringUtils.trimToEmpty(inputDTO.getChkRemark()))) {
            json += ",\"chkRemark\":\"" + inputDTO.getChkRemark() + "\"";
        }
        json += "}";
        log.info("find5ByConditionRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.3.3 微信、支付宝未认领查询分页列表
    public RequestBody find6ByConditionRequestBody(Find6ByConditionInputDTO inputDTO) {
        String json = "{\"type\":" + inputDTO.getType() + ",\"timeStart\":" + inputDTO.getTimeStart() + ",\"timeEnd\":"
                + inputDTO.getTimeEnd() + ",\"pageNo\":" + inputDTO.getPageNo() + ",\"pageSize\":"
                + inputDTO.getPageSize() + ",\"oid\":\"" + inputDTO.getOid() + "\"";
        if (StringUtils.isNotBlank(inputDTO.getInAccount())) {
            json += ",\"inAccount\":\"" + inputDTO.getInAccount() + "\"";
        }
        if (inputDTO.getMoneyStart() != null) {
            json += ",\"moneyStart\":" + inputDTO.getMoneyStart();
        }
        if (inputDTO.getMoneyEnd() != null) {
            json += ",\"moneyEnd\":" + inputDTO.getMoneyEnd();
        }
        json += "}";
        log.info("find6ByConditionRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.3.4 微信、支付宝已取消查询分页列表
    public RequestBody find7ByConditionRequestBody(Find7ByConditionInputDTO inputDTO1) {
        String json = "{\"type\":" + inputDTO1.getType() + ",\"timeStart\":" + inputDTO1.getTimeStart()
                + ",\"timeEnd\":" + inputDTO1.getTimeEnd() + ",\"pageNo\":" + inputDTO1.getPageNo() + ",\"pageSize\":"
                + inputDTO1.getPageSize() + ",\"oid\":\"" + inputDTO1.getOid() + "\"";
        if (StringUtils.isNotBlank(inputDTO1.getInAccount())) {
            json += ",\"inAccount\":\"" + inputDTO1.getInAccount() + "\"";
        }
        if (inputDTO1.getMoneyStart() != null) {
            json += ",\"moneyStart\":" + inputDTO1.getMoneyStart();
        }
        if (inputDTO1.getMoneyEnd() != null) {
            json += ",\"moneyEnd\":" + inputDTO1.getMoneyEnd();
        }
        if (StringUtils.isNotBlank(inputDTO1.getCode())) {
            json += ",\"code\":\"" + inputDTO1.getCode() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO1.getUserName())) {
            json += ",\"userName\":\"" + inputDTO1.getUserName() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO1.getChkRemark())) {
            json += ",\"chkRemark\":\"" + inputDTO1.getChkRemark() + "\"";
        }
        json += "}";
        log.info("find7ByConditionRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.3.5 点击“待处理流水”列，第一个tab
    public RequestBody find10ByConditionRequestBody(Find10ByConditionInputDTO inputDTO) {
        String json = "{\"oid\":" + inputDTO.getOid() + ",\"type\":" + inputDTO.getType() + ",\"inAccount\":\""
                + inputDTO.getInAccount() + "\",\"timeStart\":" + inputDTO.getTimeStart() + ",\"timeEnd\":"
                + inputDTO.getTimeEnd() + ",\"pageNo\":" + inputDTO.getPageNo() + ",\"pageSize\":"
                + inputDTO.getPageSize() + ",\"device\":\"" + inputDTO.getDevice() + "\"";
        if (StringUtils.isNotBlank(inputDTO.getCode())) {
            json += ",\"code\":\"" + inputDTO.getCode() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getUserName())) {
            json += ",\"userName\":\"" + inputDTO.getUserName() + "\"";
        }

        if (inputDTO.getMoneyStart() != null) {
            json += ",\"moneyStart\":" + inputDTO.getMoneyStart();
        }
        if (inputDTO.getMoneyEnd() != null) {
            json += ",\"moneyEnd\":" + inputDTO.getMoneyEnd();
        }
        json += "}";
        log.info("find10ByConditionRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.3.6 第一个tab取消
    public RequestBody cancelRequestBody(CancelInputDTO inputDTO) {
        String json = "{\"oid\":" + inputDTO.getOid() + ",\"id\":" + inputDTO.getId() + ",\"remark\":\""
                + inputDTO.getRemark() + "\"";
        if (inputDTO.getOperationAdminId() != null) {
            json += ",\"operationAdminId\":" + inputDTO.getOperationAdminId();
        }
        if (StringUtils.isNotBlank(inputDTO.getOperationAdminName())) {
            json += ",\"operationAdminName\":\"" + inputDTO.getOperationAdminName() + "\"";
        }
        json += "}";
        log.info("cancelRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.3.7 第一个tab新增备注
    public RequestBody modifyRemarkRequestBody(ModifyRemarkInputDTO inputDTO) {
        String json = "{\"oid\":" + inputDTO.getOid() + ",\"id\":" + inputDTO.getId() + ",\"remark\":\""
                + inputDTO.getRemark() + "\"";
        if (inputDTO.getOperationAdminId() != null) {
            json += ",\"operationAdminId\":" + inputDTO.getOperationAdminId();
        }
        if (StringUtils.isNotBlank(inputDTO.getOperationAdminName())) {
            json += ",\"operationAdminName\":\"" + inputDTO.getOperationAdminName() + "\"";
        }
        json += "}";
        log.info("modifyRemarkRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.3.8 点击“待处理流水”列，第二个tab
    public RequestBody find11ByConditionRequestBody(Find11ByConditionInputDTO inputDTO) {
        String json = "{\"oid\":" + inputDTO.getOid() + ",\"type\":" + inputDTO.getType() + ",\"inAccount\":\""
                + inputDTO.getInAccount() + "\",\"timeStart\":" + inputDTO.getTimeStart() + ",\"timeEnd\":"
                + inputDTO.getTimeEnd() + ",\"pageNo\":" + inputDTO.getPageNo() + ",\"pageSize\":"
                + inputDTO.getPageSize() + ",\"device\":\"" + inputDTO.getDevice() + "\"";
        if (inputDTO.getMoneyStart() != null) {
            json += ",\"moneyStart\":" + inputDTO.getMoneyStart();
        }
        if (inputDTO.getMoneyEnd() != null) {
            json += ",\"moneyEnd\":" + inputDTO.getMoneyEnd();
        }
        if (StringUtils.isNotBlank(inputDTO.getFlowNo())) {
            json += ",\"flowNo\":\"" + inputDTO.getFlowNo() + "\"";
        }
        json += "}";
        log.info("find11ByConditionRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.3.9 第二个tab新增备注
    public RequestBody addRemarkRequestBody(AddRemarkInputDTO inputDTO) {
        String json = "{\"oid\":" + inputDTO.getOid() + ",\"id\":" + inputDTO.getId() + ",\"remark\":\""
                + inputDTO.getRemark() + "\"";
        if (inputDTO.getOperationAdminId() != null) {
            json += ",\"operationAdminId\":" + inputDTO.getOperationAdminId();
        }
        if (StringUtils.isNotBlank(inputDTO.getOperationAdminName())) {
            json += ",\"operationAdminName\":\"" + inputDTO.getOperationAdminName() + "\"";
        }
        json += "}";
        log.info("addRemarkRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.3.10 第二个tab补提单
    public RequestBody putPlusRequestBody(PutPlusInputDTO inputDTO) {
        String json = "{\"oid\":" + inputDTO.getOid() + ",\"type\":" + inputDTO.getType() + ",\"userName\":\""
                + inputDTO.getUserName() + "\",\"account\":\"" + inputDTO.getAccount() + "\",\"money\":"
                + inputDTO.getMoney() + ",\"createtime\":" + inputDTO.getCreateTime();
        if (StringUtils.isNotBlank(inputDTO.getRemark())) {
            json += ",\"remark\":\"" + inputDTO.getRemark() + "\"";
        }
        if (inputDTO.getOperationAdminId() != null) {
            json += ",\"operationAdminId\":" + inputDTO.getOperationAdminId();
        }
        if (StringUtils.isNotBlank(inputDTO.getOperationAdminName())) {
            json += ",\"operationAdminName\":\"" + inputDTO.getOperationAdminName() + "\"";
        }
        json += "}";
        log.info("putPlusRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.3.11 第一个tab和第二个tab匹配
    public RequestBody matchingRequestBody(MatchingInputDTO inputDTO) {
        String json = "{\"oid\":" + inputDTO.getOid() + ",\"inId\":" + inputDTO.getInId() + ",\"uid\":"
                + inputDTO.getUid() + ",\"logId\":" + inputDTO.getLogId() + ",\"tradingFlow\":\""
                + inputDTO.getTradingFlow() + "\"";
        if (inputDTO.getOperationAdminId() != null) {
            json += ",\"operationAdminId\":" + inputDTO.getOperationAdminId();
        }
        if (StringUtils.isNotBlank(inputDTO.getOperationAdminName())) {
            json += ",\"operationAdminName\":\"" + inputDTO.getOperationAdminName() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getRemark())) {
            json += ",\"remark\":\"" + inputDTO.getRemark() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getChkRemark())) {
            json += ",\"chkRemark\":\"" + inputDTO.getChkRemark() + "\"";
        }
        json += "}";
        log.info("matchingRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.3.12 统计指定device的正在匹配总数
    public RequestBody statisticsRequestBody(StatisticsInputDTO inputDTO) {
        String json = "{\"oid\":" + inputDTO.getOid() + ",\"timeStart\":" + inputDTO.getTimeStart() + ",\"timeEnd\":"
                + inputDTO.getTimeEnd();
        if (inputDTO.getDeviceCol() != null && inputDTO.getDeviceCol().length > 0) {
            json += ",\"deviceCol\":[\"";
            String[] deviceCol = inputDTO.getDeviceCol();
            int len = deviceCol.length;
            if (len == 1) {
                json += deviceCol[0] + "\"]";
            } else {
                for (int i = 0; i < len; i++) {
                    if (i < len - 1) {
                        json += inputDTO.getDeviceCol()[i] + "\",\"";
                    } else {
                        json += inputDTO.getDeviceCol()[i] + "\"]";
                    }
                }
            }
        }
        json += "}";
        log.info("statisticsRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.4.1 新支付下发记录
    public RequestBody find4WByConditionRequestBody(Find4WByConditionInputDTO inputDTO) {
        String json = "{\"status\":" + inputDTO.getStatus() + ",\"timeStart\":" + inputDTO.getTimeStart()
                + ",\"timeEnd\":" + inputDTO.getTimeEnd() + ",\"pageNo\":" + inputDTO.getPageNo() + ",\"pageSize\":"
                + inputDTO.getPageSize();
        if (inputDTO.getOid() != null) {
            json += ",\"oid\":" + inputDTO.getOid();
        }
        if (StringUtils.isNotBlank(inputDTO.getDrawBankName())) {
            json += ",\"drawBankName\":\"" + inputDTO.getDrawBankName() + "\"";
        }
        if (StringUtils.isNotBlank(inputDTO.getPayBankName())) {
            json += ",\"payBankName\":\"" + inputDTO.getPayBankName() + "\"";
        }
        if (inputDTO.getMoneyStart() != null) {
            json += ",\"moneyStart\":" + inputDTO.getMoneyStart();
        }
        if (inputDTO.getMoneyEnd() != null) {
            json += ",\"moneyEnd\":" + inputDTO.getMoneyEnd();
        }
        if (inputDTO.getAdmintimeStart() != null) {
            json += ",\"admintimeStart\":" + inputDTO.getAdmintimeStart();
        }
        if (inputDTO.getAdmintimeEnd() != null) {
            json += ",\"admintimeEnd\":" + inputDTO.getAdmintimeEnd();
        }
        json += "}";
        log.info("find4WByConditionRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.4.2 返佣规则 – 查询分页列表
    public RequestBody findCRByConditionRequestBody(FindCRByConditionInputDTO inputDTO) {
        String json = "{ \"pageNo\":" + inputDTO.getPageNo() + ",\"pageSize\":" + inputDTO.getPageSize();
        if (inputDTO.getOid() != null) {
            json += ",\"oid\":" + inputDTO.getOid();
        }
        if (inputDTO.getInType() != null) {
            json += ",\"inType\":" + inputDTO.getInType();
        }
        if (StringUtils.isNotBlank(inputDTO.getAdminName())) {
            json += ",\"adminName\":\"" + inputDTO.getAdminName() + "\"";
        }
        if (inputDTO.getCommissionPercentStart() != null) {
            json += ",\"commissionPercentStart\":" + inputDTO.getCommissionPercentStart();
        }
        if (inputDTO.getCommissionPercentEnd() != null) {
            json += ",\"commissionPercentEnd\":" + inputDTO.getCommissionPercentEnd();
        }
        if (inputDTO.getCommissionMaxStart() != null) {
            json += ",\"commissionMaxStart\":" + inputDTO.getCommissionMaxStart();
        }
        if (inputDTO.getCommissionMaxEnd() != null) {
            json += ",\"commissionMaxEnd\":" + inputDTO.getCommissionMaxEnd();
        }
        json += "}";
        log.info("findCRByConditionRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.4.3 返佣规则 – 新增
    public RequestBody addCRRequestBody(AddCRInputDTO inputDTO) {
        String json = "{\"oid\":" + inputDTO.getOid() + ",\"inType\":" + inputDTO.getInType() + ",\"startMoney\":"
                + inputDTO.getStartMoney() + ",\"endMoney\":" + inputDTO.getEndMoney() + ",\"commissionPercent\":"
                + inputDTO.getCommissionPercent() + ",\"commissionMax\":" + inputDTO.getCommissionMax();
        if (inputDTO.getOperationAdminId() != null) {
            json += ",\"operationAdminId\":" + inputDTO.getOperationAdminId();
        }
        if (StringUtils.isNotBlank(inputDTO.getOperationAdminName())) {
            json += ",\"operationAdminName\":\"" + inputDTO.getOperationAdminName() + "\"";
        }
        json += "}";
        log.info("addCRRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.4.4 返佣规则 – 修改
    public RequestBody modifyCRRequestBody(ModifyCRInputDTO inputDTO) {
        String json = "{\"oid\":" + inputDTO.getOid() + ",\"id\":" + inputDTO.getId() + ",\"inType\":"
                + inputDTO.getInType() + ",\"startMoney\":" + inputDTO.getStartMoney() + ",\"endMoney\":"
                + inputDTO.getEndMoney() + ",\"commissionPercent\":" + inputDTO.getCommissionPercent()
                + ",\"commissionMax\":" + inputDTO.getCommissionMax();
        if (inputDTO.getOperationAdminId() != null) {
            json += ",\"operationAdminId\":" + inputDTO.getOperationAdminId();
        }
        if (StringUtils.isNotBlank(inputDTO.getOperationAdminName())) {
            json += ",\"operationAdminName\":\"" + inputDTO.getOperationAdminName() + "\"";
        }
        json += "}";
        log.info("modifyCRRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.4.5 返佣规则 – 删除
    public RequestBody removeCRRequestBody(RemoveCRInputDTO inputDTO) {
        String json = "{\"oid\":" + inputDTO.getOid() + ",\"id\":" + inputDTO.getId();

        if (inputDTO.getOperationAdminId() != null) {
            json += ",\"operationAdminId\":" + inputDTO.getOperationAdminId();
        }
        if (StringUtils.isNotBlank(inputDTO.getOperationAdminName())) {
            json += ",\"operationAdminName\":\"" + inputDTO.getOperationAdminName() + "\"";
        }
        json += "}";
        log.info("removeCRRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.6.1 确认结果
    public RequestBody confirmRequestBody(ConfirmInputDTO inputDTO) {
        String json = "{\"oid\":" + inputDTO.getOid() + ",\"code\":\"" + inputDTO.getCode() + "\"}";
        log.info("confirmRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.6.2 重置信用额度
    public RequestBody resetRequestBody(ResetInputDTO inputDTO) {
        String json = "{\"oid\":" + inputDTO.getOid() + ",\"device\":\"" + inputDTO.getDevice() + "\",\"type\":"
                + inputDTO.getType() + ",\"money\":" + inputDTO.getMoney() + "}";
        log.info("resetRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.6.3 自动重置信用额度
    public RequestBody autoResetRequestBody(AutoResetInputDTO inputDTO) {
        String token = token4NewPay.getToken4NewPay(new Object[]{inputDTO.getAccount(), inputDTO.getMoney()});
        log.info("autoResetRequestBody token :{}", token);
        if (StringUtils.isBlank(token)) {
            log.info("autoResetRequestBody token is empty , please check the inputDTO");
            return null;
        }
        String json = "{\"oid\":" + inputDTO.getOid() + ",\"openMan\":\"" + inputDTO.getOpenMan() + "\",\"account\":\""
                + inputDTO.getAccount() + "\",\"inTime\":" + inputDTO.getInTime() + ",\"money\":" + inputDTO.getMoney()
                + ",\"token\":\"" + token + "\"}";
        log.info("autoResetRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.6.4 银行卡-修改状态
    public RequestBody modifyBindCardStatus2RequestBody(ModifyBindCardStatus2InputDTO inputDTO) {
        String token = token4NewPay.getToken4NewPay(
                new Object[]{inputDTO.getOid(), inputDTO.getCardPayeeCol().size(), inputDTO.getStatus()});
        log.info("modifyBindCardStatus2RequestBody token :{}", token);
        if (StringUtils.isBlank(token)) {
            log.info("modifyBindCardStatus2RequestBody token is empty , please check the inputDTO");
            return null;
        }
        String json = "{\"oid\":\"" + inputDTO.getOid() + "\",\"status\":" + inputDTO.getStatus()
                + ",\"operationAdminName\":\"" + inputDTO.getOperationAdminName() + "\",\"token\":\"" + token
                + "\",\"cardPayeeCol\":[";
        if (!CollectionUtils.isEmpty(inputDTO.getCardPayeeCol())) {
            if (inputDTO.getCardPayeeCol().size() == 1) {
                ModifyBindCardStatus2InputDTO.CardPayee cardPayee = inputDTO.getCardPayeeCol().get(0);
                json += "{\"cardNo\":\"" + cardPayee.getCardNo() + "\",\"payeeName\":\"" + cardPayee.getPayeeName()
                        + "\"}]";
            } else {
                for (int i = 0, size = inputDTO.getCardPayeeCol().size(); i < size; i++) {
                    ModifyBindCardStatus2InputDTO.CardPayee cardPayee = inputDTO.getCardPayeeCol().get(i);
                    if (i < size - 1) {
                        json += "{\"cardNo\":\"" + cardPayee.getCardNo() + "\",\"payeeName\":\""
                                + cardPayee.getPayeeName() + "\"},";
                    } else {
                        json += "{\"cardNo\":\"" + cardPayee.getCardNo() + "\",\"payeeName\":\""
                                + cardPayee.getPayeeName() + "\"}]";
                    }
                }
            }
        }
        json += "}";
        log.info("modifyBindCardStatus2RequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    // 1.6.5 新增反馈
    public RequestBody addForCrkRequestBody(AddForCrkInputDTO inputDTO) {
        String json = "{\"oid\":" + inputDTO.getOid() + ",\"adminId\":\"" + inputDTO.getAdminId() + "\",\"userName\":\""
                + inputDTO.getUserName() + "\",\"type2\":" + inputDTO.getType2() + ",\"important\":"
                + inputDTO.getImportant() + ",\"title\":\"" + inputDTO.getTitle() + "\",\"content\":\""
                + inputDTO.getContent() + "\"";
        if (Objects.nonNull(inputDTO.getZipCol()) && inputDTO.getZipCol().length > 0) {
            json += ",\"zipCol\":[";
            if (inputDTO.getZipCol().length == 1) {
                json += "\"" + inputDTO.getZipCol()[0] + "\"]";
            } else {
                for (int i = 0, len = inputDTO.getZipCol().length; i < len; i++) {
                    if (i < len - 1) {
                        json += "\"" + inputDTO.getZipCol()[i] + "\",";
                    } else {
                        json += "\"" + inputDTO.getZipCol()[i] + "\"]";
                    }
                }
            }
        }
        if (Objects.nonNull(inputDTO.getImgCol()) && inputDTO.getImgCol().length > 0) {
            json += ",\"imgCol\":[";
            if (inputDTO.getImgCol().length == 1) {
                json += "\"" + inputDTO.getImgCol()[0] + "\"]";
            } else {
                for (int i = 0, len = inputDTO.getImgCol().length; i < len; i++) {
                    if (i < len - 1) {
                        json += "\"" + inputDTO.getImgCol()[i] + "\",";
                    } else {
                        json += "\"" + inputDTO.getImgCol()[i] + "\"]";
                    }
                }
            }
        }
        json += "}";
        log.info("addForCrkRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    public RequestBody findForOidRequestBody(FindForOidInputDTO inputDTO) {
        String json = "{\"pageSize\":" + inputDTO.getPageSize() +
                ",\"pageNo\":" + inputDTO.getPageNo();
        if (inputDTO.getCrk() != null) {
            json += ",\"crk\":" + inputDTO.getCrk();
        }
        if (inputDTO.getIsMain() != null) {
            json += ",\"isMain\":" + inputDTO.getIsMain();
        }
        if (inputDTO.getResult() != null) {
            json += ",\"result\":" + inputDTO.getResult();
        }
        if (inputDTO.getStatus() != null) {
            json += ",\"status\":" + inputDTO.getStatus();
        }
        if (inputDTO.getType() != null) {
            json += ",\"type\":" + inputDTO.getType();
        }
        if (inputDTO.getType2() != null) {
            json += ",\"type2\":" + inputDTO.getType2();
        }
        if (inputDTO.getNewReply2() != null) {
            json += ",\"newReply2\":" + inputDTO.getNewReply2();
        }
        if (inputDTO.getTypeId() != null) {
            json += ",\"typeId\":" + inputDTO.getTypeId();
        }
        if (inputDTO.getImportant() != null) {
            json += ",\"important\":" + inputDTO.getImportant();
        }
        if (StringUtils.isNotBlank(inputDTO.getUserName())) {
            json += ",\"userName\":" + inputDTO.getUserName();
        }
        if (StringUtils.isNotBlank(inputDTO.getCreateTimeStart())) {
            json += ",\"createTimeStart\":" + inputDTO.getCreateTimeStart();
        }
        if (StringUtils.isNotBlank(inputDTO.getCreateTimeEnd())) {
            json += ",\"createTimeEnd\":" + inputDTO.getCreateTimeEnd();
        }
        if (StringUtils.isNotBlank(inputDTO.getHandelTimeStart())) {
            json += ",\"handelTimeStart\":" + inputDTO.getHandelTimeStart();
        }
        if (StringUtils.isNotBlank(inputDTO.getHandelTimeEnd())) {
            json += ",\"handelTimeEnd\":" + inputDTO.getHandelTimeEnd();
        }

        if (StringUtils.isNotBlank(inputDTO.getOrderField())) {
            json += ",\"orderField\":" + inputDTO.getOrderField();
        }
        if (StringUtils.isNotBlank(inputDTO.getOrderSort())) {
            json += ",\"orderSort\":" + inputDTO.getOrderSort();
        }
        json += "}";
        log.info("findForOidRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    public RequestBody findContentRequestBody(CommonInputDTO inputDTO) {
        String json = "{\"oid\":" + inputDTO.getOid() +
                ",\"id\":" + inputDTO.getId() + "}";
        log.info("findContentRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    public RequestBody findForOfbRequestBody(FindForOfbInputDTO inputDTO) {
        String json = "{\"ofbId\":" + inputDTO.getOfbId();
        if (StringUtils.isNotBlank(inputDTO.getCreateTimeStart())) {
            json += ",\"createTimeStart\":" + inputDTO.getCreateTimeStart();
        }
        if (StringUtils.isNotBlank(inputDTO.getCreateTimeEnd())) {
            json += ",\"createTimeEnd\":" + inputDTO.getCreateTimeEnd();
        }
        json += "}";
        log.info("findContentRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    public RequestBody findForOfb2RequestBody(FindForOfb4DemandInputDTO inputDTO) {
        String json = "{\"ofbId\":" + inputDTO.getOfbId() + ",\"oid\":" + inputDTO.getOid() + "}";
        log.info("findForOfb2RequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }

    public RequestBody solveRequestBody(CommonInputDTO inputDTO) {
        String json = "{\"oid\":" + inputDTO.getOid() +
                ",\"id\":" + inputDTO.getId() + "}";
        log.info("solveRequestBody json value:{}", json);
        return RequestBody.create(MediaType.parse("application/json"), json);
    }
}
