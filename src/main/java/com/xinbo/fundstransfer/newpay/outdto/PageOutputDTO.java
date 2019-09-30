package com.xinbo.fundstransfer.newpay.outdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.apache.poi.ss.formula.functions.T;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Created by Administrator on 2018/7/11.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class PageOutputDTO<T> implements Serializable {
	private Integer pageNo; // 分页页码
	private Integer pageSize; // 每页条数
	private Integer totalRecordNumber; // 总共的记录条数
	private Integer totalPageNumber;// 总共的页数
	private BigDecimal other;// 总金额
	private List<T> resultList;// 返回的具体DTO数据list
}
