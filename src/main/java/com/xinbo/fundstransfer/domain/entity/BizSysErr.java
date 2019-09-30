package com.xinbo.fundstransfer.domain.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "biz_sys_err")
public class BizSysErr {
	private Long id;
	private int target;
	private int targetType;
	private int targetHandicap;
	private String targetBankType;
	private String targetAlias;
	private int targetFlag;
	private int targetLevel;
	private String batchNo;
	private BigDecimal balance;
	private BigDecimal bankBalance;
	private BigDecimal margin;
	private Date occurTime;
	private Integer collector;
	private Date collectTime;
	private Integer status;
	private Long consumeTime;
	private String remark;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "target")
	public int getTarget() {
		return target;
	}

	public void setTarget(int target) {
		this.target = target;
	}

	@Column(name = "target_type")
	public int getTargetType() {
		return targetType;
	}

	public void setTargetType(int targetType) {
		this.targetType = targetType;
	}

	@Column(name = "target_handicap")
	public int getTargetHandicap() {
		return targetHandicap;
	}

	public void setTargetHandicap(int targetHandicap) {
		this.targetHandicap = targetHandicap;
	}

	@Column(name = "target_bank_type")
	public String getTargetBankType() {
		return targetBankType;
	}

	public void setTargetBankType(String targetBankType) {
		this.targetBankType = targetBankType;
	}

	@Column(name = "target_alias")
	public String getTargetAlias() {
		return targetAlias;
	}

	public void setTargetAlias(String targetAlias) {
		this.targetAlias = targetAlias;
	}

	@Column(name = "target_flag")
	public int getTargetFlag() {
		return targetFlag;
	}

	public void setTargetFlag(int targetFlag) {
		this.targetFlag = targetFlag;
	}

	@Column(name = "target_level")
	public int getTargetLevel() {
		return targetLevel;
	}

	public void setTargetLevel(int targetLevel) {
		this.targetLevel = targetLevel;
	}

	@Column(name = "batch_no")
	public String getBatchNo() {
		return batchNo;
	}

	public void setBatchNo(String batchNo) {
		this.batchNo = batchNo;
	}

	@Column(name = "balance")
	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	@Column(name = "bank_balance")
	public BigDecimal getBankBalance() {
		return bankBalance;
	}

	public void setBankBalance(BigDecimal bankBalance) {
		this.bankBalance = bankBalance;
	}

	@Column(name = "margin")
	public BigDecimal getMargin() {
		return margin;
	}

	public void setMargin(BigDecimal margin) {
		this.margin = margin;
	}

	@Column(name = "occur_time")
	public Date getOccurTime() {
		return occurTime;
	}

	public void setOccurTime(Date occurTime) {
		this.occurTime = occurTime;
	}

	@Column(name = "collector")
	public Integer getCollector() {
		return collector;
	}

	public void setCollector(Integer collector) {
		this.collector = collector;
	}

	@Column(name = "collect_time")
	public Date getCollectTime() {
		return collectTime;
	}

	public void setCollectTime(Date collectTime) {
		this.collectTime = collectTime;
	}

	@Column(name = "status")
	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	@Column(name = "consume_time")
	public Long getConsumeTime() {
		return consumeTime;
	}

	public void setConsumeTime(Long consumeTime) {
		this.consumeTime = consumeTime;
	}

	@Column(name = "remark")
	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	/**
	 * 临时数据填充
	 */
	private String handicapName;

	private String levelName;

	private String simpName;

	private String statusName;

	private String flagName;

	private String collectorName;

	private String timeSimp;

	private Integer accStatus;

	private String accStatusName;

	private Date finishTime;

	private String lastRemark;

	@Transient
	public String getHandicapName() {
		return handicapName;
	}

	public void setHandicapName(String handicapName) {
		this.handicapName = handicapName;
	}

	@Transient
	public String getLevelName() {
		return levelName;
	}

	public void setLevelName(String levelName) {
		this.levelName = levelName;
	}

	@Transient
	public String getSimpName() {
		return simpName;
	}

	public void setSimpName(String simpName) {
		this.simpName = simpName;
	}

	@Transient
	public String getStatusName() {
		return statusName;
	}

	public void setStatusName(String statusName) {
		this.statusName = statusName;
	}

	@Transient
	public String getFlagName() {
		return flagName;
	}

	public void setFlagName(String flagName) {
		this.flagName = flagName;
	}

	@Transient
	public String getCollectorName() {
		return collectorName;
	}

	public void setCollectorName(String collectorName) {
		this.collectorName = collectorName;
	}

	@Transient
	public String getTimeSimp() {
		return timeSimp;
	}

	public void setTimeSimp(String timeSimp) {
		this.timeSimp = timeSimp;
	}

	@Transient
	public Integer getAccStatus() {
		return accStatus;
	}

	public void setAccStatus(Integer accStatus) {
		this.accStatus = accStatus;
	}

	@Transient
	public String getAccStatusName() {
		return accStatusName;
	}

	public void setAccStatusName(String accStatusName) {
		this.accStatusName = accStatusName;
	}

	@Transient
	public Date getFinishTime() {
		return finishTime;
	}

	public void setFinishTime(Date finishTime) {
		this.finishTime = finishTime;
	}

	@Transient
	public String getLastRemark() {
		return lastRemark;
	}

	public void setLastRemark(String lastRemark) {
		this.lastRemark = lastRemark;
	}
}
