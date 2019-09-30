package com.xinbo.fundstransfer.newpay.inputdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Created by Administrator on 2018/7/11.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class PageInputDTO implements Serializable {
	@NotNull
	private Integer pageNo;// 分页页码
	@NotNull
	private Integer pageSize;// 每页条数

	public void setPageNo(Integer pageNo) {
		if (pageNo == null)
			pageNo = 1;
		else {
			pageNo += 1;
		}
		this.pageNo = pageNo;
	}
}
