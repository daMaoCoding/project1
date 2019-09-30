package com.xinbo.fundstransfer.newpay.inputdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.persistence.criteria.CriteriaBuilder;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Created by Administrator on 2018/7/12.
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class FindCommissionDetailInputDTO extends PageInputDTO implements Serializable {
	private Double moneyStart;// 检索金额开始值

	private Double moneyEnd;// 检索金额结束值

	private Long timeStart;// 检索日期开始值

	private Long timeEnd;// 检索日期结束值
	@NotNull
	private Integer oid;// 盘口编码
	@NotNull
	private Long mobileId;// 手机号id
}
