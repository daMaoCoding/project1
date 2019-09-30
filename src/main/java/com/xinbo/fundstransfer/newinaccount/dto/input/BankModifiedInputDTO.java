package com.xinbo.fundstransfer.newinaccount.dto.input;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author Administrator
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@ToString(callSuper = true)
public class BankModifiedInputDTO implements Serializable {
	/** （必传）盘口编码 */
	private Integer oid;
	/** （必传）通道id */
	private Long pocId;
	/** （必传）银行卡号 */
	private String cardNo;
	/** （必传）0.停用 1.删除 2.修改银行卡号 4.冻结 5.启用 */
	private Byte status;
	/** 新银行卡号，status=2时必传 */
	private String cardNo2;
}
