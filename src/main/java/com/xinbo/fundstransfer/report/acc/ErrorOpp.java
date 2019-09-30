package com.xinbo.fundstransfer.report.acc;

public class ErrorOpp {

	private Integer oppId;
	private Integer oppHandicap;
	private String oppAccount;
	private String oppOwner;
	private Long orderId;
	private String orderNo;

	public ErrorOpp() {
	}

	public ErrorOpp(Integer oppId, Integer oppHandicap, String oppAccount, String oppOwner, Long orderId,
			String orderNo) {
		this.oppId = oppId;
		this.oppHandicap = oppHandicap;
		this.oppAccount = oppAccount;
		this.oppOwner = oppOwner;
		this.orderId = orderId;
		this.orderNo = orderNo;
	}

	public Integer getOppId() {
		return oppId;
	}

	public void setOppId(Integer oppId) {
		this.oppId = oppId;
	}

	public Integer getOppHandicap() {
		return oppHandicap;
	}

	public void setOppHandicap(Integer oppHandicap) {
		this.oppHandicap = oppHandicap;
	}

	public String getOppAccount() {
		return oppAccount;
	}

	public void setOppAccount(String oppAccount) {
		this.oppAccount = oppAccount;
	}

	public String getOppOwner() {
		return oppOwner;
	}

	public void setOppOwner(String oppOwner) {
		this.oppOwner = oppOwner;
	}

	public Long getOrderId() {
		return orderId;
	}

	public void setOrderId(Long orderId) {
		this.orderId = orderId;
	}

	public String getOrderNo() {
		return orderNo;
	}

	public void setOrderNo(String orderNo) {
		this.orderNo = orderNo;
	}
}
