package com.xinbo.fundstransfer.newpay.outdto;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * Created by Administrator on 2018/7/11.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class FindAWByConditionOutputDTO implements Serializable {
	private long id;
	private Long mobileId;
	private Integer oid; // 业主oid
	private String ownerName; // 业主名称
	private Byte type;// 类型，0：客户，1：自用
	private Byte level; // 0：外层，1：中层，2：内层
	private String tel; // 联系电话
	private String name; // 微信、支付宝名称
	private Byte status; // 0.停用 1.启用
	private Double balance; // 微信、支付宝余额
	private String account; // 微信、支付宝账号
	private Integer inMnt1;// 入款 - 已匹配
	private Integer inMnt2; // 入款 - 未认领（无法匹配）
	private Integer inMnt3;// 入款 - 已取消（已驳回）
	private Integer inMnt4; // 入款 - 匹配中
	private Integer outMnt1;// 出款 - 已匹配
	private Integer outMnt2; // 出款 - 未认领（无法匹配）
	private Integer outMnt3; // 出款 - 已取消（已驳回）
	private Integer outMnt4;// 出款 - 匹配中
	private String device;// 设备号
	private Byte deviceStatus;// 设备状态 0 ： 可用 1：繁忙 2：离线
}
