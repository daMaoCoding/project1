package com.xinbo.fundstransfer.newpay.inputdto;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

/**
 * Created by Administrator on 2018/7/13.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class MatchingInputDTO implements Serializable {
	@NotNull
	private Integer oid;// 盘口编码
	@NotNull
	private Long inId;// /ownerNewpayLog/find10ByCondition返回的inId
	@NotNull
	private Long uid;// 会员uid
	@NotNull
	private Long logId;// /ownerNewpayLog/find11ByCondition返回的id
	@NotNull
	private String tradingFlow;// 交易流水;
	private String remark;// 备注
	private String oldRemark;// 旧的备注
	private Long operationAdminId;// 操作人id
	private String operationAdminName;// 操作人账号
	private String chkRemark;// 否 收款理由
}
