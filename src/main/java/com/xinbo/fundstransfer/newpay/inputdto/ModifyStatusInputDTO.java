package com.xinbo.fundstransfer.newpay.inputdto;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

/**
 * Created by Administrator on 2018/7/12.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ModifyStatusInputDTO implements Serializable {
    @NotNull
    private Integer oid;// 盘口编码
    @NotNull
    private Long accountId;/// ownerNewpayConfig/findByCondition返回的bankAccountId或wechatAccountId或alipayAccountId
    @NotNull
    private Byte type;// 0.wx 1.zfb 2.银行卡 3.云闪付
    @NotNull
    private Byte status;// 0.停用 1.启用
}
