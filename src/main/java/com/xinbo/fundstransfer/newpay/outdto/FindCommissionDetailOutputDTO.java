package com.xinbo.fundstransfer.newpay.outdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by Administrator on 2018/7/12.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class FindCommissionDetailOutputDTO implements Serializable {
	private String commissionBankNum; // 返佣账号
	private String commissionOpenMan; // 返佣开户人
	private String commissionBankName; // 返佣开户行
	private Double money; // 金额
	private String createtime; // 交易时间
	private Byte status; // 状态：0：待处理，1：确认，2，取消
	private String remark; // 备注
}
