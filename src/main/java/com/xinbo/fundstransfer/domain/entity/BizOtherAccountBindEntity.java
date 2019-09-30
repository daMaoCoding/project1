package com.xinbo.fundstransfer.domain.entity;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "biz_other_account_bind", schema = "fundsTransfer")
public class BizOtherAccountBindEntity {
	private Integer id;
	private Integer otherAccountId;
	private Integer accountId;

	@Id
	@Column(name = "id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Basic
	@Column(name = "other_account_id")
	public Integer getOtherAccountId() {
		return otherAccountId;
	}

	public void setOtherAccountId(Integer otherAccountId) {
		this.otherAccountId = otherAccountId;
	}

	@Basic
	@Column(name = "account_id")
	public Integer getAccountId() {
		return accountId;
	}

	public void setAccountId(Integer accountId) {
		this.accountId = accountId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		BizOtherAccountBindEntity that = (BizOtherAccountBindEntity) o;
		return id == that.id && Objects.equals(otherAccountId, that.otherAccountId)
				&& Objects.equals(accountId, that.accountId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, otherAccountId, accountId);
	}
}
