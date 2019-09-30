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
public class FindAccountInfoInputDTO implements Serializable {
    @NotNull
    private Integer oid;// 盘口编码
    @NotNull
    private Long id;
    @NotNull
    private Byte type;// 0.wx 1.zfb 2.银行卡 3.云闪付
    private Byte isEpAlipay;// 否 是否是企业支付宝 0:否 1:是
}
