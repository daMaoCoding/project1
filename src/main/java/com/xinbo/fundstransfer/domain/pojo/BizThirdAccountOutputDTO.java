package com.xinbo.fundstransfer.domain.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.xinbo.fundstransfer.domain.entity.BizThirdAccountEntity;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BizThirdAccountOutputDTO implements Serializable {
	private Integer id;
	private String thirdName;
	private String thirdNumber;
	/**
	 * 账号状态 1 3 4 {@link com.xinbo.fundstransfer.domain.enums.AccountStatus}
	 */
	private Byte status;
	private String loginAccount;
	private String loginPass;
	private String payPass;
	private Integer accountId;
	private String thirdNameUrl;
	/**
	 * 盘口id
	 */
	private Integer handicapId;
	/**
	 * 盘口名称
	 */
	private String handicapName;
	private String createTime;
	private String createUid;
	private String updateTime;
	private String updateUid;
	private String latestRemark;
	private BigDecimal sysBalance;

	public BizThirdAccountOutputDTO() {
		super();
	}

	public BizThirdAccountOutputDTO wrapFromObj(Object[] obj) {
		// t.id,a.bank_name,a.account,a.status,t.login_account,t.login_pass,t.pay_pass,t.account_id,t.third_name_url,a.handicap_id,t.create_time,t.create_uid,t.update_time,t.update_uid
		// , a.balance as balance
		if (obj == null || obj.length == 0)
			return null;
		this.id = obj[0] == null ? null : Integer.valueOf(obj[0].toString());
		this.thirdName = obj[1] == null ? null : obj[1].toString();
		this.thirdNumber = obj[2] == null ? null : obj[2].toString();
		this.status = obj[3] == null ? null : Integer.valueOf(obj[3].toString()).byteValue();
		this.loginAccount = obj[4] == null ? null : obj[4].toString();
		this.loginPass = obj[5] == null ? null : obj[5].toString();
		this.payPass = obj[6] == null ? null : obj[6].toString();
		this.accountId = obj[7] == null ? null : Integer.valueOf(obj[7].toString());
		this.thirdNameUrl = obj[8] == null ? null : obj[8].toString();
		this.handicapId = obj[9] == null ? null : Integer.valueOf(obj[9].toString());
		this.createTime = obj[10] == null ? null : obj[10].toString();
		this.createUid = obj[11] == null ? null : obj[11].toString();
		this.updateTime = obj[12] == null ? null : obj[12].toString();
		this.updateUid = obj[13] == null ? null : obj[13].toString();
		this.sysBalance = obj[14] == null ? BigDecimal.ZERO : new BigDecimal(obj[14].toString());
		return this;
	}

	public BizThirdAccountOutputDTO wrapFromEntity(BizThirdAccountEntity entity) {
		this.id = entity.getId();
		this.accountId = entity.getAccountId();
		this.createTime = entity.getCreateTime().toString();
		this.createUid = entity.getCreateUid();
		this.loginAccount = entity.getLoginAccount();
		this.loginPass = entity.getLoginPass();
		this.payPass = entity.getPayPass();
		this.thirdNameUrl = entity.getThirdNameUrl();
		this.updateTime = entity.getUpdateTime() == null ? null : entity.getUpdateTime().toString();
		this.updateUid = entity.getUpdateUid() == null ? null : entity.getUpdateUid();
		return this;
	}

	public BizThirdAccountOutputDTO(Integer id, String thirdName, String thirdNumber, Byte status, String loginAccount,
			String loginPass, String payPass, Integer accountId, String thirdNameUrl, Integer handicapId,
			String createTime, String createUid, String updateTime, String updateUid, String latestRemark) {
		this.id = id;
		this.thirdName = thirdName;
		this.thirdNumber = thirdNumber;
		this.status = status;
		this.loginAccount = loginAccount;
		this.loginPass = loginPass;
		this.payPass = payPass;
		this.accountId = accountId;
		this.thirdNameUrl = thirdNameUrl;
		this.handicapId = handicapId;
		this.createTime = createTime;
		this.createUid = createUid;
		this.updateTime = updateTime;
		this.updateUid = updateUid;
		this.latestRemark = latestRemark;
	}
}
