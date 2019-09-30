package com.xinbo.fundstransfer.chatPay.valtools.reqVo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;

/**
 * ************************
 * 二维码工具，获取验证二维码任务
 * @author tony
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReqGetValQrJob {

    /**
     * token : abcd
     * deviceId : devId
     */

    @NotNull(message = "token不能空")
    @Length(min = 4,message = "token错误")
    private String token;

    @NotNull(message = "设备号不能空")
    private String deviceId;


}
