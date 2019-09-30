package com.xinbo.fundstransfer.domain.pojo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.xinbo.fundstransfer.domain.Paging;

/**
 * Created by 000 on 2017/7/14
 */
public class PageOutwardTaskCheck implements Serializable {
	private Paging page;

	private List<OutwardTaskCheckContent> content = new ArrayList<>();

	public PageOutwardTaskCheck() {
	}

	public PageOutwardTaskCheck(Paging page) {
		this.page = page;
	}

	public Paging getPage() {
		return page;
	}

	public List<OutwardTaskCheckContent> getContent() {
		return content;
	}

	public void push(OutwardTaskCheckContent item) {
		this.content.add(item);
	}

	public class OutwardTaskCheckContent {

		private Long outwardTaskId;

		private Integer outwardRequestId;

		private Integer fromAccountId;

		private BigDecimal amount;

		private Date asignTime;

		private Integer handicapId;

		private Integer levelId;

		private String memberUserName;

		private String toAccountBankName;

		private String toAccount;

		private String toAccountOwner;

		private String handicapName;

		private String levelName;

		private String asignTimeStr;

		private String fromAccount;

		private String taskOperatorUid;

		private String taskRemark;

		private Integer taskStatus;

		private BigDecimal fee;

		private String screenshot;

		private String orderNo;

		private String fromAlias;

		private String fromOwner;

		private String fromBankType;

		public OutwardTaskCheckContent() {
		}

		public OutwardTaskCheckContent(Long outwardTaskId, Integer outwardRequestId, Integer fromAccountId,
				BigDecimal amount, Date asignTime, Integer handicapId, Integer levelId, String memberUserName,
				String toAccountBankName, String toAccount, String toAccountOwner, String taskOperatorUid,
				String taskRemark, Integer taskStatus, String screenshot, String orderNo) {
			this.outwardTaskId = outwardTaskId;
			this.outwardRequestId = outwardRequestId;
			this.fromAccountId = fromAccountId;
			this.amount = amount;
			this.asignTime = asignTime;
			this.handicapId = handicapId;
			this.levelId = levelId;
			this.memberUserName = memberUserName;
			this.toAccountBankName = toAccountBankName;
			this.toAccount = toAccount;
			this.toAccountOwner = toAccountOwner;
			this.taskOperatorUid = taskOperatorUid;
			this.taskRemark = taskRemark;
			this.taskStatus = taskStatus;
			this.screenshot = screenshot;
			this.orderNo = orderNo;
		}

		public Long getOutwardTaskId() {
			return outwardTaskId;
		}

		public void setOutwardTaskId(Long outwardTaskId) {
			this.outwardTaskId = outwardTaskId;
		}

		public Integer getOutwardRequestId() {
			return outwardRequestId;
		}

		public void setOutwardRequestId(Integer outwardRequestId) {
			this.outwardRequestId = outwardRequestId;
		}

		public Integer getFromAccountId() {
			return fromAccountId;
		}

		public void setFromAccountId(Integer fromAccountId) {
			this.fromAccountId = fromAccountId;
		}

		public BigDecimal getAmount() {
			return amount;
		}

		public void setAmount(BigDecimal amount) {
			this.amount = amount;
		}

		public Date getAsignTime() {
			return asignTime;
		}

		public void setAsignTime(Date asignTime) {
			this.asignTime = asignTime;
		}

		public Integer getHandicapId() {
			return handicapId;
		}

		public void setHandicapId(Integer handicapId) {
			this.handicapId = handicapId;
		}

		public Integer getLevelId() {
			return levelId;
		}

		public void setLevelId(Integer levelId) {
			this.levelId = levelId;
		}

		public String getMemberUserName() {
			return memberUserName;
		}

		public void setMemberUserName(String memberUserName) {
			this.memberUserName = memberUserName;
		}

		public String getToAccountBankName() {
			return toAccountBankName;
		}

		public void setToAccountBankName(String toAccountBankName) {
			this.toAccountBankName = toAccountBankName;
		}

		public String getToAccount() {
			return toAccount;
		}

		public void setToAccount(String toAccount) {
			this.toAccount = toAccount;
		}

		public String getToAccountOwner() {
			return toAccountOwner;
		}

		public void setToAccountOwner(String toAccountOwner) {
			this.toAccountOwner = toAccountOwner;
		}

		public String getHandicapName() {
			return handicapName;
		}

		public void setHandicapName(String handicapName) {
			this.handicapName = handicapName;
		}

		public String getLevelName() {
			return levelName;
		}

		public void setLevelName(String levelName) {
			this.levelName = levelName;
		}

		public String getAsignTimeStr() {
			return asignTimeStr;
		}

		public void setAsignTimeStr(String asignTimeStr) {
			this.asignTimeStr = asignTimeStr;
		}

		public String getFromAccount() {
			return fromAccount;
		}

		public void setFromAccount(String fromAccount) {
			this.fromAccount = fromAccount;
		}

		public String getTaskOperatorUid() {
			return taskOperatorUid;
		}

		public void setTaskOperatorUid(String taskOperatorUid) {
			this.taskOperatorUid = taskOperatorUid;
		}

		public String getTaskRemark() {
			return taskRemark;
		}

		public void setTaskRemark(String taskRemark) {
			this.taskRemark = taskRemark;
		}

		public BigDecimal getFee() {
			return fee;
		}

		public void setFee(BigDecimal fee) {
			this.fee = fee;
		}

		public Integer getTaskStatus() {
			return taskStatus;
		}

		public void setTaskStatus(Integer taskStatus) {
			this.taskStatus = taskStatus;
		}

		public String getScreenshot() {
			return screenshot;
		}

		public void setScreenshot(String screenshot) {
			this.screenshot = screenshot;
		}

		public String getOrderNo() {
			return orderNo;
		}

		public void setOrderNo(String orderNo) {
			this.orderNo = orderNo;
		}

		public String getFromAlias() {
			return fromAlias;
		}

		public void setFromAlias(String fromAlias) {
			this.fromAlias = fromAlias;
		}

		public String getFromOwner() {
			return fromOwner;
		}

		public void setFromOwner(String fromOwner) {
			this.fromOwner = fromOwner;
		}

		public String getFromBankType() {
			return fromBankType;
		}

		public void setFromBankType(String fromBankType) {
			this.fromBankType = fromBankType;
		}
	}

}
