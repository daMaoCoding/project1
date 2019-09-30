package com.xinbo.fundstransfer.chatPay.config.reqVo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.*;

/**
 * ************************
 *  平台查询会员微信支付宝入款时超时时间
 * @author tony
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReqWxZfbIncomeTimeOut {

    /**
     * 盘口
     */
    @NotNull(message = "Oid不能空。")
    private int oid;


    /**
     * 	类型(0微信收款码，1支付宝收款码)
     */
    @Min(value = 0,message = "查询类型错误")
    @Max(value = 1,message = "查询类型错误")
    @NotNull(message = "qrType不能空")
    private int qrType;

}
