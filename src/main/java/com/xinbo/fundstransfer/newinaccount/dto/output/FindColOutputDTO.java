package com.xinbo.fundstransfer.newinaccount.dto.output;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@ToString(callSuper = true)
public class FindColOutputDTO implements Serializable {
	/** 盘口编码 */
	Number oid;
	/** 银行卡 */
	String cardNo;
	/** 银行名称 */
	String bankName;
	/** 开户行 */
	String bankOpen;
	/** 开户人 */
	String openName;
	/** 最小入款金额 */
	Number minInMoney;
	/** 该银行卡绑定的支付通道的id，若没绑定，则为null */
	Long pocId;
	String province;
	String city;
	/** 银行卡是否可用 0.否 1.是 */
	Byte status2;
}
