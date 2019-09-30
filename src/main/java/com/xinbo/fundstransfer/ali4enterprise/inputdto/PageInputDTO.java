package com.xinbo.fundstransfer.ali4enterprise.inputdto;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class PageInputDTO implements Serializable {
	@NotNull
	private Integer pageSize;// 必填 每页记录条数
	@NotNull
	private Integer pageNo;// 必填 查询页码
	private String orderField;// 非必填 排序字段
	private String orderSort;// 非必填 asc、desc

	public Integer getPageNo() {
		return pageNo;
	}

	public void setPageNo(Integer pageNo) {
		if (pageNo == null)
			pageNo = 1;
		else {
			pageNo += 1;
		}
		this.pageNo = pageNo;
	}
}
