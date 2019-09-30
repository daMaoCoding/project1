package com.xinbo.fundstransfer.newpay.inputdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Created by Administrator on 2018/7/13. 1.3.6 第一个tab取消
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class CancelInputDTO implements Serializable {
	@NotNull
	private Integer oid;// 盘口编码
	@NotNull
	private Long id;// /ownerNewpayLog/find10ByCondition返回的inId
	@NotNull
	private String code;// 订单号;
	@NotNull
	private String remark;// 备注
	private String oldRemark;// 旧的备注
	private Long operationAdminId;// 操作人id
	private String operationAdminName;// 操作人账号
}
