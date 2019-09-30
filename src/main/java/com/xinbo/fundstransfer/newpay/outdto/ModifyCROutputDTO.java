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
public class ModifyCROutputDTO implements Serializable {
	private long id;
	private Integer oid;// 业主oid
	private Byte inType;// 0.wx 1.zfb 2.银行卡，不传或为空表示全部
	private String adminName;// 操作人
	private Double startMoney;// 该兼职当天所收金额起始值
	private Double endMoney;// 该兼职当天所收金额结束值
	private Float commissionPercent;// 佣金比列
	private Float commissionMax;// 最高限额
	private String uptime;// 匹配时间（确认时间）
}
