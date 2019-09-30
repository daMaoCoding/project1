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
public class FindContentByConditionInputDTO extends PageInputDTO implements Serializable {
	@NotNull
	private Integer oid;// 盘口编码
	@NotNull
	private Byte type;// 类型，0：形容词，1：名词
	private Byte status;// 状态：1-在用 0-停用
	private String content;// 词语内容
	private String adminName;// 最后操作人
	private String typeName;// 词语类型名称
	private Long uptimeStart;// 最后操作时间开始值
	private Long uptimeEnd;// 最后操作时间结束值

}
