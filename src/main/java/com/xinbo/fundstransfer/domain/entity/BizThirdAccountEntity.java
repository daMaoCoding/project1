package com.xinbo.fundstransfer.domain.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.xinbo.fundstransfer.domain.pojo.BizThirdAccountInputDTO;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.*;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

/**
 * 第三方资料表
 */
@Entity
@Table(name = "biz_third_account")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BizThirdAccountEntity implements Serializable {
	private Integer id;

	private String loginAccount;
	private String loginPass;
	private String payPass;
	private Integer accountId;
	private String thirdNameUrl;

	private Timestamp createTime;
	private String createUid;
	private Timestamp updateTime;
	private String updateUid;

	public BizThirdAccountEntity() {
		super();
	}

	public BizThirdAccountEntity wrapFromInputDTO(BizThirdAccountInputDTO inputDTO, BizThirdAccountEntity oldEntity)
			throws InvocationTargetException, IllegalAccessException {
		if (null == inputDTO || null == oldEntity)
			return null;
		BizThirdAccountEntity newEntity = new BizThirdAccountEntity();
		BeanUtils.copyProperties(newEntity, oldEntity);
		newEntity.setUpdateTime(Timestamp.from(Instant.now()));
		newEntity.setUpdateUid(inputDTO.getSysUser() == null ? "sys" : inputDTO.getSysUser().getUid());

		if (!ObjectUtils.equals(inputDTO.getAccountId(), newEntity.getAccountId())) {
			newEntity.setAccountId(inputDTO.getAccountId());
		}
		if (!StringUtils.equals(inputDTO.getLoginAccount(), newEntity.getLoginAccount())) {
			newEntity.setAccountId(inputDTO.getAccountId());
		}

		if (!StringUtils.equals(inputDTO.getThirdNameUrl(), newEntity.getThirdNameUrl())) {
			newEntity.setThirdNameUrl(inputDTO.getThirdNameUrl());
		}

		if (!StringUtils.equals(inputDTO.getLoginAccount(), newEntity.getLoginAccount())) {
			newEntity.setLoginAccount(inputDTO.getLoginAccount());
		}
		if (!StringUtils.equals(inputDTO.getLoginPass(), newEntity.getLoginPass())) {
			newEntity.setLoginPass(inputDTO.getLoginPass());
		}
		if (!StringUtils.equals(inputDTO.getPayPass(), newEntity.getPayPass())) {
			newEntity.setPayPass(inputDTO.getPayPass());
		}

		return newEntity;
	}

	public BizThirdAccountEntity wrapFromInputDTO(BizThirdAccountInputDTO inputDTO) {
		this.id = inputDTO.getId();
		this.accountId = inputDTO.getAccountId();
		if (this.id == null) {
			this.createTime = Timestamp.from(Instant.now());
			this.createUid = inputDTO.getSysUser() == null ? "sys" : inputDTO.getSysUser().getUid();
		} else {
			this.updateUid = inputDTO.getSysUser() == null ? "sys" : inputDTO.getSysUser().getUid();
			this.updateTime = Timestamp.from(Instant.now());
		}
		this.loginAccount = inputDTO.getLoginAccount();
		this.loginPass = inputDTO.getLoginPass();
		this.payPass = inputDTO.getPayPass();
		this.thirdNameUrl = inputDTO.getThirdNameUrl();
		return this;
	}

	public BizThirdAccountEntity(int id, String thirdName, String thirdNumber, byte status, String loginAccount,
			String loginPass, String payPass, int accountId, String thirdNameUrl, int handicapId, Timestamp createTime,
			String createUid, Timestamp updateTime, String updateUid) {
		this.id = id;

		this.loginAccount = loginAccount;
		this.loginPass = loginPass;
		this.payPass = payPass;
		this.accountId = accountId;
		this.thirdNameUrl = thirdNameUrl;

		this.createTime = createTime;
		this.createUid = createUid;
		this.updateTime = updateTime;
		this.updateUid = updateUid;
	}

	@Id
	@Column(name = "id", nullable = false)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Basic
	@Column(name = "login_account", nullable = false, length = 500)
	public String getLoginAccount() {
		return loginAccount;
	}

	public void setLoginAccount(String loginAccount) {
		this.loginAccount = loginAccount;
	}

	@Basic
	@Column(name = "login_pass", nullable = false, length = 500)
	public String getLoginPass() {
		return loginPass;
	}

	public void setLoginPass(String loginPass) {
		this.loginPass = loginPass;
	}

	@Basic
	@Column(name = "pay_pass", nullable = false, length = 500)
	public String getPayPass() {
		return payPass;
	}

	public void setPayPass(String payPass) {
		this.payPass = payPass;
	}

	@Basic
	@Column(name = "account_id", nullable = false)
	public Integer getAccountId() {
		return accountId;
	}

	public void setAccountId(Integer accountId) {
		this.accountId = accountId;
	}

	@Basic
	@Column(name = "third_name_url", nullable = false, length = 500)
	public String getThirdNameUrl() {
		return thirdNameUrl;
	}

	public void setThirdNameUrl(String thirdNameUrl) {
		this.thirdNameUrl = thirdNameUrl;
	}

	@Basic
	@Column(name = "create_time", nullable = true)
	public Timestamp getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Timestamp createTime) {
		this.createTime = createTime;
	}

	@Basic
	@Column(name = "create_uid", nullable = true, length = 45)
	public String getCreateUid() {
		return createUid;
	}

	public void setCreateUid(String createUid) {
		this.createUid = createUid;
	}

	@Basic
	@Column(name = "update_time", nullable = true)
	public Timestamp getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Timestamp updateTime) {
		this.updateTime = updateTime;
	}

	@Basic
	@Column(name = "update_uid", nullable = true, length = 45)
	public String getUpdateUid() {
		return updateUid;
	}

	public void setUpdateUid(String updateUid) {
		this.updateUid = updateUid;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		BizThirdAccountEntity that = (BizThirdAccountEntity) o;
		return id == that.id && accountId == that.accountId && Objects.equals(loginAccount, that.loginAccount)
				&& Objects.equals(loginPass, that.loginPass) && Objects.equals(payPass, that.payPass)
				&& Objects.equals(thirdNameUrl, that.thirdNameUrl) && Objects.equals(createTime, that.createTime)
				&& Objects.equals(createUid, that.createUid) && Objects.equals(updateTime, that.updateTime)
				&& Objects.equals(updateUid, that.updateUid);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, loginAccount, loginPass, payPass, accountId, thirdNameUrl, createTime, createUid,
				updateTime, updateUid);
	}
}
