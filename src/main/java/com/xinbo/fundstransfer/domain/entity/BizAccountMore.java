package com.xinbo.fundstransfer.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "biz_account_more")
@JsonIgnoreProperties(ignoreUnknown = true)
public class BizAccountMore {

	private Integer id;
	private String uid;
	private BigDecimal balance;
	private Date updateTime;
	private String accounts;
	private String remark;
	private BigDecimal margin;
	private Integer classify;
	private String moible;
	private Integer handicap;
	private BigDecimal linelimit;

    private BigDecimal tmpMargin;	 	       //返利网活动相关-赠送临时额度，如+1500，+1千
    private BigDecimal activityTotalAmount;	 	//返利网活动累计佣金(包括多活动)
    private Integer inFlwActivity;	 	           //是否有在参与返利网的活动(0无，1有)

	private BigDecimal totalOutFlow;          //累计出款流水
	private BigDecimal totalRebate;           //累计返佣
	private String username;                   //返利网用户
	private int joinDays;                     //加入返利网天数
	private String cardUsedOrStop;            //启用停用卡
	private String isDisplay;                 //是否显示
	private Date displayAfterDate;            //指定日期及以后显示

	private Boolean isAgent;                  //是否是代理(满足条件：1，非代理不满足条件0)


	@Column(name = "is_agent")
	public Boolean getAgent() {
		return isAgent;
	}

	public void setAgent(Boolean agent) {
		isAgent = agent;
	}

	@Column(name = "tmp_margin")
    public BigDecimal getTmpMargin() {
        return tmpMargin;
    }

    public void setTmpMargin(BigDecimal tmpMargin) {
        this.tmpMargin = tmpMargin;
    }


	@Column(name = "activity_total_amount")
	public BigDecimal getActivityTotalAmount() {
		return activityTotalAmount;
	}

	public void setActivityTotalAmount(BigDecimal activityTotalAmount) {
		this.activityTotalAmount = activityTotalAmount;
	}




    @Column(name = "in_flw_activity")
    public Integer getInFlwActivity() {
        return inFlwActivity;
    }

    public void setInFlwActivity(Integer inFlwActivity) {
        this.inFlwActivity = inFlwActivity;
    }







	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column(name = "uid")
	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	@Column(name = "balance")
	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	@Column(name = "update_time")
	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	@Column(name = "accounts")
	public String getAccounts() {
		return accounts;
	}

	public void setAccounts(String accounts) {
		this.accounts = accounts;
	}

	@Column(name = "remark")
	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	@Column(name = "margin")
	public BigDecimal getMargin() {
		return margin;
	}

	public void setMargin(BigDecimal margin) {
		this.margin = margin;
	}

	@Column(name = "classify")
	public Integer getClassify() {
		return classify;
	}

	public void setClassify(Integer classify) {
		this.classify = classify;
	}

	@Column(name = "moible")
	public String getMoible() {
		return moible;
	}

	public void setMoible(String moible) {
		this.moible = moible;
	}

	@Column(name = "handicap")
	public Integer getHandicap() {
		return handicap;
	}

	public void setHandicap(Integer handicap) {
		this.handicap = handicap;
	}

	@Column(name = "linelimit")
	public BigDecimal getLinelimit() {
		return linelimit;
	}

	public void setLinelimit(BigDecimal linelimit) {
		this.linelimit = linelimit;
	}

	@Column(name = "total_out_flow")
	public BigDecimal getTotalOutFlow() {
		return totalOutFlow;
	}

	public void setTotalOutFlow(BigDecimal totalOutFlow) {
		this.totalOutFlow = totalOutFlow;
	}

	@Column(name = "total_rebate")
	public BigDecimal getTotalRebate() {
		return totalRebate;
	}

	public void setTotalRebate(BigDecimal totalRebate) {
		this.totalRebate = totalRebate;
	}

	@Column(name = "is_display")
	public String getIsDisplay() {
		return isDisplay;
	}

	@Column(name = "display_after_date")
	public Date getDisplayAfterDate() {
		return displayAfterDate;
	}

	public void setDisplayAfterDate(Date displayAfterDate) {
		this.displayAfterDate = displayAfterDate;
	}

	public void setIsDisplay(String isDisplay) {
		this.isDisplay = isDisplay;
	}

	@Transient
	public String getUsername() {
		return username;
	}

	public void setUsername(String username){
    	this.username = username;
	}

	public void setJoinDays(int joinDays){
		this.joinDays = joinDays;
	}

	@Transient
	public int getJoinDays(){
		return joinDays;
	}

	@Transient
	public String getCardUsedOrStop() {
		return cardUsedOrStop;
	}

	public void setCardUsedOrStop(String cardUsedOrStop) {
		this.cardUsedOrStop = cardUsedOrStop;
	}
}
