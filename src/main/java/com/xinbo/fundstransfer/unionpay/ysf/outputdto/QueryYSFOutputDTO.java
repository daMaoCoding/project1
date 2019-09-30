package com.xinbo.fundstransfer.unionpay.ysf.outputdto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class QueryYSFOutputDTO implements Serializable {
	private Integer id;
	private String accountNo;
	private String owner;
	private String loginPWD;
	private String payPWD;
	private String handicap;
	private String ownTypeDesc;
	private Byte ownType;
	private String statusDesc;
	private Byte status;
	private Timestamp createTime;
	private Timestamp updateTime;
	private String remark;
	List<Integer> bindAccountIds;
	List<BizAccount> bindAccountList;
}
