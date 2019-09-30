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
public class StaticstiOutputDTO implements Serializable {
	Number mnt1;// 银行卡数量
	Number mnt2; // 已绑定数量
	Number mnt3;// 未绑定数量
}
