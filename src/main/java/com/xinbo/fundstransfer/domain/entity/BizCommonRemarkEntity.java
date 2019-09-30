package com.xinbo.fundstransfer.domain.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.xinbo.fundstransfer.domain.pojo.BizCommonRemarkInputDTO;
import org.apache.commons.lang.ObjectUtils;
import org.omg.CORBA.PRIVATE_MEMBER;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

/**
 * 公共保存备注 表
 */
@Entity
@Table(name = "biz_common_remark")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BizCommonRemarkEntity implements Serializable {
	private Integer id;
	private Integer businessId;
	private Timestamp createTime;
	private Timestamp updateTime;
	private String createUid;
	private String updateUid;
	private String remark;
	private Byte status;
	private String type;

	public BizCommonRemarkEntity() {
		super();
	}

	public BizCommonRemarkEntity wrapFromInputDTO(BizCommonRemarkInputDTO inputDTO) {
		this.id = inputDTO.getId();
		this.businessId = inputDTO.getBusinessId();
		this.createTime = Timestamp.from(Instant.now());
		this.createUid = inputDTO.getSysUser() == null ? "sys" : inputDTO.getSysUser().getUid();
		this.remark = inputDTO.getRemark();
		this.status = inputDTO.getStatus() == null ? BizCommonRemarkInputDTO.RemarkStatus.NORMAL.getCode().byteValue()
				: inputDTO.getStatus().byteValue();
		this.type = inputDTO.getType();
		if (this.id != null) {
			this.updateTime = inputDTO.getUpdateTime() == null ? null : inputDTO.getUpdateTime();
			this.updateUid = inputDTO.getSysUser() == null ? null : inputDTO.getSysUser().getUid();
		}
		return this;
	}

	public BizCommonRemarkEntity(Integer id, Integer businessId, Timestamp createTime, Timestamp updateTime,
			String createUid, String updateUid, String remark, Byte status, String type) {
		this.id = id;
		this.businessId = businessId;
		this.createTime = createTime;
		this.updateTime = updateTime;
		this.createUid = createUid;
		this.updateUid = updateUid;
		this.remark = remark;
		this.status = status;
		this.type = type;
	}

	@Id
	@Column(name = "id", nullable = false)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Integer getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Basic
	@Column(name = "business_id", nullable = false)
	public Integer getBusinessId() {
		return businessId;
	}

	public void setBusinessId(int businessId) {
		this.businessId = businessId;
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
	@Column(name = "create_uid", nullable = false, length = 45)
	public String getCreateUid() {
		return createUid;
	}

	public void setCreateUid(String createUid) {
		this.createUid = createUid;
	}

	@Basic
	@Column(name = "update_uid", nullable = false, length = 45)
	public String getUpdateUid() {
		return updateUid;
	}

	public void setUpdateUid(String updateUid) {
		this.updateUid = updateUid;
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
	@Column(name = "remark", nullable = true, length = 500)
	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	@Basic
	@Column(name = "status", nullable = true)
	public Byte getStatus() {
		return status;
	}

	public void setStatus(Byte status) {
		this.status = status;
	}

	@Basic
	@Column(name = "type", nullable = true)
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		BizCommonRemarkEntity that = (BizCommonRemarkEntity) o;
		return id == that.id && businessId == that.businessId && Objects.equals(createTime, that.createTime)
				&& Objects.equals(createUid, that.createUid) && Objects.equals(remark, that.remark)
				&& Objects.equals(status, that.status) && Objects.equals(type, that.type);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, businessId, createTime, createUid, remark, status, type);
	}
}
