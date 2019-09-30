package com.xinbo.fundstransfer.ali4enterprise.outputdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class PageOutPutDTO<T> implements Serializable {
	private Integer pageNo;
	private Integer dbIndex;
	private Integer pageSize;
	private Integer totalRecordNumber;
	private Integer totalPageNumber;
	private String orderField;
	private String orderSort;
	private Integer maxId;
	private List<T> resultList;

}
