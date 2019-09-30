package com.xinbo.fundstransfer.domain.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.xinbo.fundstransfer.domain.enums.CurrentSystemLevel;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 *
 * @author Administrator
 * @date 2018/7/20
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class OutwardTaskTotalInputDTO implements Serializable {
	private String bankType;
	/**
	 * 第三方("third") 代付("daifu")
	 */
	private String drawType;
	private String handicap;
	private String level;
	private String orderNo;
	private String member;
	private BigDecimal fromMoney;
	private BigDecimal toMoney;
	private String accountAlias;
	private String operatorName;
	private String flag;
	private Integer maintain;
	private Integer robot;
	private Integer manual;
	private Integer phone;
	/**
	 * 代付
	 */
	private Integer thirdInsteadPay;
	private String startTime;
	private String endTime;
	private Integer pageNo;
	private Integer pageSize;
	private Integer sortFlag;
	private Integer type;
	/**
	 * pc 手机 人工 机器 (待排查 包含 代付)
	 */
	private String operatorType;
	/**
	 * 如果 值为 1 表示平台查询 其他情况为系统内查询
	 */
	private Byte platFormQueryFlag;
	/**
	 * @see CurrentSystemLevel 9 表示人工
	 */
	private String sysLevel;
}
