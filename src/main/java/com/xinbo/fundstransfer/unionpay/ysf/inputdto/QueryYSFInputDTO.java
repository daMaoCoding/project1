package com.xinbo.fundstransfer.unionpay.ysf.inputdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.Timestamp;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class QueryYSFInputDTO implements Serializable {
	private Integer id;
	private Integer handicapId;
	private String accountNo;
	private String owner;
	private Byte[] ownType;// 自有 兼职 或者其他
	private Byte[] status;// 云闪付状态
	private Timestamp updateTime;
	@NotNull
	private Integer pageSize;// 必填 每页记录条数
	@NotNull
	private Integer pageNo;// 必填 查询页码
	private String orderField;// 非必填 排序字段
	private String orderSort;// 非必填 asc、desc
}
