package com.xinbo.fundstransfer.newpay.inputdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Created by Administrator on 2018/8/28.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ResetInputDTO implements Serializable {
    @NotNull
    private String device;// 设备号
    @NotNull
    private Integer type;// 类型：0：微信，1：支付宝 2-银行  3.云闪付
    @NotNull
    private Number money;// 金额，小于兼职人员的信用额度
    @NotNull
    private Integer oid;// 盘口编码

}
