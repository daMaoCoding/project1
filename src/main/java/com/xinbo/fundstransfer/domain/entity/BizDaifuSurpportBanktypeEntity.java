package com.xinbo.fundstransfer.domain.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * 第三方代付支持的银行类型
 * 
 * @author Administrator
 */
@Entity
@Table(name = "biz_daifu_surpport_banktype")
public class BizDaifuSurpportBanktypeEntity implements Serializable {

	private static final long serialVersionUID = -4885875175612100345L;
	private int id;
	private String provider;
	private String supportBankType;
	private String allBankType;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Column(name = "provider")
	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	@Column(name = "support_bank_type")
	public String getSupportBankType() {
		return supportBankType;
	}

	public void setSupportBankType(String supportBankType) {
		this.supportBankType = supportBankType;
	}

	@Column(name = "all_bank_type")
	public String getAllBankType() {
		return allBankType;
	}

	public void setAllBankType(String allBankType) {
		this.allBankType = allBankType;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		BizDaifuSurpportBanktypeEntity that = (BizDaifuSurpportBanktypeEntity) o;
		return id == that.id && Objects.equals(provider, that.provider)
				&& Objects.equals(supportBankType, that.supportBankType)
				&& Objects.equals(allBankType, that.allBankType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, provider, supportBankType, allBankType);
	}
}
