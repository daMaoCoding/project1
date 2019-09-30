package com.xinbo.fundstransfer.ali4enterprise.outputdto;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class FindEpStopAlarmOutputDTO implements Serializable {
	private String rate;// 入款大于或等于停用金额的rate开始告警
	private Double money;// 入款大于或等于money元，开始告警*

}
