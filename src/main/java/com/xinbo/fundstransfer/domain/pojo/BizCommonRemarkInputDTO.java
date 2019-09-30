package com.xinbo.fundstransfer.domain.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BizCommonRemarkInputDTO implements Serializable {

	private Integer id;
	private Integer businessId;
	private SysUser sysUser;
	private String remark;
	private Byte status;
	private String type;
	private Integer pageNo;
	private Integer pageSize;
	private Timestamp updateTime;

	public enum RemarkStatus {
		NORMAL(1, "正常"), DELETE(2, "删除");
		private Integer code;
		private String codeDesc;

		RemarkStatus(Integer code, String codeDesc) {
			this.code = code;
			this.codeDesc = codeDesc;
		}

		public Integer getCode() {
			return code;
		}

		public void setCode(Integer code) {
			this.code = code;
		}

		public String getCodeDesc() {
			return codeDesc;
		}

		public void setCodeDesc(String codeDesc) {
			this.codeDesc = codeDesc;
		}
	}

}
