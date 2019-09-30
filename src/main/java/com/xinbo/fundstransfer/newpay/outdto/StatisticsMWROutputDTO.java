package com.xinbo.fundstransfer.newpay.outdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.apache.poi.ss.formula.functions.T;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class StatisticsMWROutputDTO<T> implements Serializable {
	private Integer mnt1; // 常用金额 - 总个数
	private Integer mnt2; // 常用金额 - 已生成个数
	private Integer mnt3; // 非常用金额 - 总个数
	private Integer mnt4;// 非常用金额 - 已生成个数
	private Byte qrAccomplishFlag;// 账号常用金额二维码是否生成完成：0-未完成 ，1-完成
}
