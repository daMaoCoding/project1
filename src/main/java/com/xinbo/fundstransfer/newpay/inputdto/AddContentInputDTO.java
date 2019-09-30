package com.xinbo.fundstransfer.newpay.inputdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Created by Administrator on 2018/8/28.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class AddContentInputDTO implements Serializable {
	@NotNull
	private Integer oid;// 盘口编码
	@NotNull
	private Byte type;// 类型，0：形容词，1：名词
//	@NotNull 先注释掉，以免打包生产时旧版本未传入参数导致异常
	private Long typeId;// 词语类型id
	@NotNull
	private String content;// 词语内容
	private Long operationAdminId;// 操作人id
	private String operationAdminName;// 操作人账号

}
