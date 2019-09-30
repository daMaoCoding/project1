package com.xinbo.fundstransfer.newpay.inputdto;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

/**
 * Created by Administrator on 2018/7/11.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class FindQRByConditionInputDTO extends PageInputDTO implements Serializable {
    @NotNull
    private Integer oid;// 盘口编码
    @NotNull
    private Byte type;// 0.wx 1.zfb 3.云闪付
    @NotNull
    private Long mobileId;// /ownerNewpayConfig/findByCondition返回的id
    @NotNull
    private Long accountId;// /ownerNewpayConfig/findByCondition返回的wechatAccountId或者alipayAccountId
    private Double moneyStart;// 检索金额开始值

    private Double moneyEnd;// 检索金额结束值
}
