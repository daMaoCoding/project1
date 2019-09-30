package com.xinbo.fundstransfer.ali4enterprise.outputdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class FindEpAisleOutputDTO implements Serializable {
	private String id;// 企业支付宝通道编号
	private String name;// 企业支付宝通道名称

}
