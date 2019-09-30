package com.xinbo.fundstransfer.domain.pojo;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class AddForCrkInputDTO implements Serializable {
	@NotNull
	private Integer oid;// 99 （必传）盘口编码
	private Long adminId; // （必传）反馈人id
	private String userName;// （必传）反馈人账号
	@NotNull
	private Byte type2;// 1, （必传）0.问题 1.需求
	@NotNull
	private int bussinessType;// （必传）业务类型 1.自动出入款系统 2.返利网 3.返利网工具
	@NotNull
	private Byte important;// 1, （必传）重要性类型 0.一般 1.重要 2.非常重要 3.紧急
	@NotNull
	private String title; // （必传）标题
	@NotNull
	private String content; // （必传）内容
	private String[] imgCol; // 图片 路径 1.6.6返回的结果
	private String[] zipCol;// 压缩文件 路径 1.6.6返回的结果

}
