package com.xinbo.fundstransfer.domain.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.xinbo.fundstransfer.domain.entity.BizCommonRemarkEntity;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BizCommonRemarkOutputDTO {
	private Integer id;
	private Integer businessId;
	private String createTime;
	private String createUid;
	private String remark;
	private Byte status;
	private String updateTime;
	private String updateUid;

	public BizCommonRemarkOutputDTO() {
		super();
	}

	public BizCommonRemarkOutputDTO wrapFromEntity(BizCommonRemarkEntity entity) {
		this.id = entity.getId();
		this.businessId = entity.getBusinessId();
		this.createTime = entity.getCreateTime().toString();
		this.createUid = entity.getCreateUid();
		this.id = entity.getId();
		this.remark = entity.getRemark();
		this.status = entity.getStatus();
		this.updateTime = entity.getUpdateTime() == null ? null : entity.getUpdateTime().toString();
		this.updateUid = entity.getUpdateUid() == null ? null : entity.getUpdateUid();
		return this;
	}

	public BizCommonRemarkOutputDTO(Integer id, Integer businessId, String createTime, String createUid, String remark,
			Byte status, String updateTime, String updateUid) {
		this.id = id;
		this.businessId = businessId;
		this.createTime = createTime;
		this.createUid = createUid;
		this.remark = remark;
		this.status = status;
		this.updateTime = updateTime;
		this.updateUid = updateUid;
	}
}
