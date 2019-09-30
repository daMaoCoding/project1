package com.xinbo.fundstransfer.domain.entity;

import com.fasterxml.jackson.annotation.JsonInclude;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "biz_alipay_request")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BizAlipayRequest {
	private Integer id;
	private Integer alipayid;
	private Integer level;
	private Integer handicap;
	private Integer status;
	private BigDecimal amount;
	private Date createTime;
	private String remark;
	private String orderNo;
	private Integer operator;
	private String memberId;
	private String memberName;
	private Date updateTime;
	private Integer alipayLogId;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column(name = "alipayid")
	public Integer getAlipayid() {
		return alipayid;
	}

	public void setAlipayid(Integer alipayid) {
		this.alipayid = alipayid;
	}

	@Column(name = "level")
	public Integer getLevel() {
		return level;
	}

	public void setLevel(Integer level) {
		this.level = level;
	}

	@Column(name = "handicap")
	public Integer getHandicap() {
		return handicap;
	}

	public void setHandicap(Integer handicap) {
		this.handicap = handicap;
	}

	@Column(name = "status")
	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	@Column(name = "amount")
	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	@Column(name = "create_time")
	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	@Column(name = "remark")
	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	@Column(name = "order_no")
	public String getOrderNo() {
		return orderNo;
	}

	public void setOrderNo(String orderNo) {
		this.orderNo = orderNo;
	}

	@Column(name = "operator")
	public Integer getOperator() {
		return operator;
	}

	public void setOperator(Integer operator) {
		this.operator = operator;
	}

	@Column(name = "member_id")
	public String getMemberId() {
		return memberId;
	}

	public void setMemberId(String memberId) {
		this.memberId = memberId;
	}

	@Column(name = "member_name")
	public String getMemberName() {
		return memberName;
	}

	public void setMemberName(String memberName) {
		this.memberName = memberName;
	}

	@Column(name = "update_time")
	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	@Column(name = "alipay_log_id")
	public Integer getAlipayLogId() {
		return alipayLogId;
	}

	public void setAlipayLogId(Integer alipayLogId) {
		this.alipayLogId = alipayLogId;
	}
}
