package com.xinbo.fundstransfer.newpay.inputdto;

import java.io.Serializable;
import java.util.Date;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

/**
 * Created by Administrator on 2018/7/13.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class PutPlusInputDTO implements Serializable {
	@NotNull
	private Integer oid;// 盘口编码
	@NotNull
	private Byte type;// 0：微信，1：支付宝
	@NotBlank
	private String userName;// 会员名
	@NotBlank
	private String account;// 微信、支付宝账号
	@NotNull
	private Double money;// 金额
	private String remark;// 备注
	private String oldRemark;// 旧的备注
	@NotNull
	private Long createTime;// 存款时间
	private Long operationAdminId;// 操作人id
	private String operationAdminName;// 操作人账号
}
