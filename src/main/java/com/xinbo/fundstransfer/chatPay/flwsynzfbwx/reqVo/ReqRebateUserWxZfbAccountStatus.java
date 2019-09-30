package com.xinbo.fundstransfer.chatPay.flwsynzfbwx.reqVo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * ************************
 * 返利网 调用出入款 同步 兼职停用微信/支付宝 收款二维码 状态
 * @author tony
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReqRebateUserWxZfbAccountStatus {


    /**
     * 参数签名 md5(uid+qrId+timestamp)
     */
    @NotBlank(message = "token不能为空")
    @Size(min = 4, message = "token错误")
    private String token;


    /**
     * 兼职UID
     */
    @NotNull(message = "UID不能为空")
    private String uid;


    /**
     * 兼职二维码id
     */
    @NotNull(message = "UID不能为空")
    private String qrId;


    /**
     * 状态：	1正常(启用)，0停用
     */
    @NotNull(message = "使用状态不能为空")
    private int status;



    /**
     * 返利网时间戳
     */
    @NotNull(message = "时间戳不能为空")
    private long timestamp;




}
