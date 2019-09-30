package com.xinbo.fundstransfer.ali4enterprise.outputdto;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class FindPayOwnerWordTypeListOutputDTO implements Serializable {
	private Long id;
	private Integer oid;
	private String typeName;// 类型名称
	private String adminName;// 最后操作人
	private String adminTime;// 最后操作时间
}
