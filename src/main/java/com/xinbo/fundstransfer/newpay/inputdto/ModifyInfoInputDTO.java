package com.xinbo.fundstransfer.newpay.inputdto;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

/**
 * Created by Administrator on 2018/7/11.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ModifyInfoInputDTO implements Serializable {
	@NotNull
	private Integer oid;// 盘口编码
	@NotNull
	private Long id;// id
	@NotNull
	private String contactName;// 联系人名
	@NotNull
	private String tel;// 联系电话
	@NotNull
	private Byte status;// 0.停用 1.启用
	@NotNull
	private Byte type;// 类型，0：客户，1：自用
	private Double credits;// 是 信用额度
	@NotNull
	private Byte level;// 内外层，0：外层，1：中层，2：内层
	private String commissionOpenMan;// 返佣开户人
	private String commissionBankName;// 返佣开户行
	private String commissionBankNum;// 返佣账号
	private Double todayOutCount;// 今日出款
	private Double ylbBalance;// 余利宝余额
}
