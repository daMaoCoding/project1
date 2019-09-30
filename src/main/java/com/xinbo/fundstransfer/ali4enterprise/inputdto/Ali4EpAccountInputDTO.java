package com.xinbo.fundstransfer.ali4enterprise.inputdto;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;

import lombok.Data;

/**
 * 企业支付宝账号信息展示入参 {@link com.xinbo.fundstransfer.domain.entity.BizAccount}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Ali4EpAccountInputDTO extends PageInputDTO {
	private Integer handicapId;// 盘口id
	private String account;// 账号 只支持右模糊 'xxx%'
	private String bankName;// 名称
	/**
	 * {@link com.xinbo.fundstransfer.domain.enums.AccountStatus}
	 */
	private Integer[] status;// 状态
	private Date createTimeEnd;// 同步时间
	private Date createTimeStart;// 同步时间
	private List<BizHandicap> handicaps;// 数据权限
}
