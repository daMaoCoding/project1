package com.xinbo.fundstransfer.chatPay.valtools.reqVo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;

/**
 * ************************
 * 二维码工具上报验证结果。
 * @author tony
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReqBackValQrJob {


    /**
     * token : 12312123abc
     * deviceId : 888-8899-955
     * qrId : FxIZt1Ud1goU1000
     * status : 1
     */
    @NotNull(message = "token不能空")
    @Length(min = 4,message = "token错误")
    private String token;

    @NotNull(message = "设备号不能空")
    private String deviceId;

    @NotNull(message = "二维码ID不能空")
    private String qrId;

    @NotNull(message = "验证结果不能空")
    private int status;


}
