package com.xinbo.fundstransfer.ali4enterprise.inputdto;

import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

import javax.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class FindPayOwnerWordListInputDTO extends PageInputDTO {
	@NotNull
	private Integer oid;// 必填 盘口编码
	private Long typeId;// 非必填 类型编号
	private String typeName;// 非必填 类型名称
	private String content;// 非必填 商品说明文字
	private String adminName;// 非必填 最后操作人
	private Timestamp adminTimeStart;// 非必填 最后操作时间开始
	private Timestamp adminTimeEnd;// 非必填 最后操作时间结束

}
