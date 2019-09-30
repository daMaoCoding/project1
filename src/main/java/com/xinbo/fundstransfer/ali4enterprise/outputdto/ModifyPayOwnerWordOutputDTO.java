package com.xinbo.fundstransfer.ali4enterprise.outputdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ModifyPayOwnerWordOutputDTO implements Serializable {
	private Long id;
	private Integer oid;
	private Long typeId;// 类型编号
	private String content;// 商品说明文字
	private String adminName;// 最后操作人
	private String adminTime;// 最后操作时间
}
