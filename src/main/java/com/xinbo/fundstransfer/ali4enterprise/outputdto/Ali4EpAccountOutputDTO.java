package com.xinbo.fundstransfer.ali4enterprise.outputdto;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

/**
 * 企业支付宝账号信息展示查询结果返回 {@link com.xinbo.fundstransfer.domain.entity.BizAccount}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Ali4EpAccountOutputDTO implements Serializable {
	private Integer id;// 账号id
	private Integer handicapId;// 盘口id
	private String account;// 账号 只支持右模糊 'xxx%'
	private String bankName;// 名称
	/**
	 * {@link com.xinbo.fundstransfer.domain.enums.AccountStatus}
	 */
	private Integer status;// 状态
	private Date createTime;// 同步时间
	private String level;// 层级
	private String remark;// 备注,摘要
}
