package com.xinbo.fundstransfer.ali4enterprise.outputdto;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class FindPayOwnerWordBindListOutputDTO implements Serializable {
	private Long typeId;// 类型编号
	private Integer oid;// 业主
	private String typeName;// 类型
	// private String content;// 商品说明文字
	private Byte isBind;// 是否已经绑定 0:未绑定 1:已绑定
	private Byte isHaveDesc;//
}
