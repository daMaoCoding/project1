package com.xinbo.fundstransfer.domain.entity;

import javax.persistence.*;

@Entity
@Table(name = "biz_account_binding")
public class BizAccountBinding implements java.io.Serializable {
	private Integer id;
	private Integer accountId;
	private Integer bindAccountId;

	public BizAccountBinding() {
	}

	public BizAccountBinding(Integer id, Integer accountId, Integer bindAccountId) {
		this.id = id;
		this.accountId = accountId;
		this.bindAccountId = bindAccountId;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column(name = "account_id")
	public Integer getAccountId() {
		return accountId;
	}

	public void setAccountId(Integer accountId) {
		this.accountId = accountId;
	}

	@Column(name = "bind_account_id")
	public Integer getBindAccountId() {
		return bindAccountId;
	}

	public void setBindAccountId(Integer bindAccountId) {
		this.bindAccountId = bindAccountId;
	}
}
