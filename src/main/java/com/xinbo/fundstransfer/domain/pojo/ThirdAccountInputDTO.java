package com.xinbo.fundstransfer.domain.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThirdAccountInputDTO implements Serializable {
	/**
	 * 盘口id 不是编码
	 */
	private List<Integer> handicapId;

	private String thirdName;

	private Integer pageNo;
	private Integer pageSize;
	/**
	 * 最新备注的业务类型
	 */
	private String type;

	private Integer id;
	/**
	 * 全部三方资料 1 我的三方资料 2
	 */
	private Byte queryPage;

	private SysUser sysUser;
	/**
	 * {@link com.xinbo.fundstransfer.domain.enums.AccountStatus}
	 */
	private Byte status;

	@Override
	public String toString() {
		return "ThirdAccountInputDTO{" + "handicapId=" + handicapId + ", thirdName='" + thirdName + '\'' + ", pageNo="
				+ pageNo + ", pageSize=" + pageSize + ", type='" + type + '\'' + ", id=" + id + ", queryPage="
				+ queryPage + ", sysUser=" + sysUser + ", status=" + status + '}';
	}
}
