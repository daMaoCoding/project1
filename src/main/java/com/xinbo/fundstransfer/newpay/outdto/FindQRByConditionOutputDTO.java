package com.xinbo.fundstransfer.newpay.outdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by Administrator on 2018/7/11.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class FindQRByConditionOutputDTO implements Serializable {
    private long id;// id
    private Integer oid;// 业主oid
    private Byte type; // 类型，0：微信，1：支付宝 3.云闪付
    private String url; // 收款二维码链接
    private Double money; // 收款二维码金额
    private String chkRemark; // 收款理由
    private Byte mcmStatus;// 是否有成功被点击过 0.否 1.是
}
