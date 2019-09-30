package com.xinbo.fundstransfer.ali4enterprise.outputdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class FindPayOwnerWordListOutputDTO implements Serializable {
	private Long id;// 100,
	private Integer oid;// 100,
	private Long typeId;// 类型编号
	private String content;// 商品说明文字
	private String adminName;// 最后操作人
	private String adminTime;// 最后操作时间
	private String typeName;// 类型名称
	private String ownerName;// 业主名称

}
