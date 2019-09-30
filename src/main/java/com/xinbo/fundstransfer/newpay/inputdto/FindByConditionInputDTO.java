package com.xinbo.fundstransfer.newpay.inputdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Created by Administrator on 2018/7/11.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class FindByConditionInputDTO extends PageInputDTO implements Serializable {
	@NotNull
	private Integer oid;// 盘口编码，不填表示全部
	private Byte level; // 0：外层，1：中层，2：内层，不填表示全部
	private String bankAccount;// 银行卡账号
	private String wechatAccount; // 微信账号
	private String alipayAccount;// 支付宝账号
	private String tel;// 联系电话
	private String statuses;// 0.停用 1.启用，多选以“,”隔开
	private String types;// 0：客户，1：自用，多选以“,”隔开
	private Byte isEpAlipay;// 否 是否是企业支付宝 0:否 1:是
}
