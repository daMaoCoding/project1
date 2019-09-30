package com.xinbo.fundstransfer.restful.v3.settingSyn.reqVo;

import lombok.Data;

import javax.validation.constraints.*;


/**
 * ************************
 *
 * @author tony
 */
@Data
public class ReqSetMaxUpgradeQuantity {

    /**
     * token : "1231234asdb"
     * timestamp : 1567559934000
     * number : 10
     */

    @NotBlank(message = "token不能为空")
    @Size(min = 4, message = "token错误")
    private String token;

    @NotNull(message = "时间戳不能为空")
    private long timestamp;

    @Min(value = 1, message = "更新客户端数量不能小于1")
    @Max(value = 500, message = "更新客户端最大值不能超过500")
    private int number;

}
