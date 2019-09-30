package com.xinbo.fundstransfer.domain.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.bouncycastle.pqc.math.linearalgebra.BigEndianConversions;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ThirdDrawFailStatisticOutputDTO implements Serializable {
	private Date createTime;
	private String time;
	private String thirdName;
	private String thirdAccount;
	private Integer fromId;

	private Integer failCounts;

	private String drawToAccount;
	private String operator;
	private BigDecimal amount;
}
