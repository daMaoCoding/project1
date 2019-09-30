package com.xinbo.fundstransfer.domain.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UnBindThirdAccountOutputDTO implements Serializable {

	/**
	 * 三方账号id
	 */
	private Integer Id;
	private String bankName;
	private String account;
	private String owner;
	private Integer handicapId;
	private String handicapName;

	public UnBindThirdAccountOutputDTO() {
		super();
	}

	// id,handicap_id,account,bank_name ,owner
	public UnBindThirdAccountOutputDTO wrapFromObj(Object[] obj) {
		if (obj == null || obj.length == 0)
			return null;
		this.Id = obj[0] == null ? null : Integer.valueOf(obj[0].toString());
		this.handicapId = obj[1] == null ? null : Integer.valueOf(obj[1].toString());
		this.account = obj[2] == null ? null : obj[2].toString();
		this.bankName = obj[3] == null ? null : obj[3].toString();
		this.owner = obj[4] == null ? null : obj[4].toString();
		this.handicapName = obj[5] == null ? null : obj[5].toString();
		return this;
	}

	public UnBindThirdAccountOutputDTO(Integer id, String bankName, String account, String owner, Integer handicapId,
			String handicapName) {
		Id = id;
		this.bankName = bankName;
		this.account = account;
		this.owner = owner;
		this.handicapId = handicapId;
		this.handicapName = handicapName;
	}

	@Override
	public String toString() {
		return "UnBindThirdAccountOutputDTO{" + "Id=" + Id + ", bankName='" + bankName + '\'' + ", account='" + account
				+ '\'' + ", owner='" + owner + '\'' + ", handicapId=" + handicapId + ", handicapName='" + handicapName
				+ '\'' + '}';
	}
}
