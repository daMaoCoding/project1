package com.xinbo.fundstransfer.newpay.outdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by Administrator on 2018/7/13.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Find4ByConditionOutputDTO implements Serializable {
	private long id; // id
	private Integer oid;// 业主oid
	private String ownerName; // 业主名称
	private String inAccount; // 收款账号
	private Byte status; // 状态，0：停用 ，1：启用
	private String reporttime; // 匹配时间（确认时间）
	private Integer mnt;// 待处理流水数
	private Byte type; // 0.wx 1.zfb 2.银行卡
	private String device;// 设备号
}
