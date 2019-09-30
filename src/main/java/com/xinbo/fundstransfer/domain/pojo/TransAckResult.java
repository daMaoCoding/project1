package com.xinbo.fundstransfer.domain.pojo;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Created by Owner on 2018/7/31.
 */
public class TransAckResult {
	private int oppId;
	private String oppAcc;
	private String oppOwner;
	private String oppAlias;
	private String oppBankType;
	private BigDecimal tdAmt;
	private Long tdTm;
	private int sort = 0;
	private int confirm;
	private long order = 0;

	public TransAckResult() {
	}

	public TransAckResult(AccountBaseInfo opp, BigDecimal tdAmt, Long tdTm, Integer sort, Integer confirm, Long order) {
		if (Objects.nonNull(opp)) {
			this.oppId = opp.getId();
			this.oppAcc = opp.getAccount();
			this.oppOwner = opp.getOwner();
			this.oppAlias = opp.getAlias();
			this.oppBankType = opp.getBankType();
		}
		this.tdAmt = tdAmt;
		this.tdTm = tdTm;
		this.sort = sort;
		this.confirm = confirm;
		this.order = order;
	}

	public int getOppId() {
		return oppId;
	}

	public void setOppId(int oppId) {
		this.oppId = oppId;
	}

	public String getOppAcc() {
		return oppAcc;
	}

	public void setOppAcc(String oppAcc) {
		this.oppAcc = oppAcc;
	}

	public String getOppOwner() {
		return oppOwner;
	}

	public void setOppOwner(String oppOwner) {
		this.oppOwner = oppOwner;
	}

	public String getOppAlias() {
		return oppAlias;
	}

	public void setOppAlias(String oppAlias) {
		this.oppAlias = oppAlias;
	}

	public String getOppBankType() {
		return oppBankType;
	}

	public void setOppBankType(String oppBankType) {
		this.oppBankType = oppBankType;
	}

	public BigDecimal getTdAmt() {
		return tdAmt;
	}

	public void setTdAmt(BigDecimal tdAmt) {
		this.tdAmt = tdAmt;
	}

	public Long getTdTm() {
		return tdTm;
	}

	public void setTdTm(Long tdTm) {
		this.tdTm = tdTm;
	}

	public int getSort() {
		return sort;
	}

	public void setSort(int sort) {
		this.sort = sort;
	}

	public int getConfirm() {
		return confirm;
	}

	public void setConfirm(int confirm) {
		this.confirm = confirm;
	}

	public long getOrder() {
		return order;
	}

	public void setOrder(long order) {
		this.order = order;
	}
}
