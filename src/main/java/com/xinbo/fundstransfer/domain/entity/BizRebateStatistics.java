package com.xinbo.fundstransfer.domain.entity;

import java.util.Date;

import javax.persistence.*;

@Entity
@Table(name = "biz_rebate_statistics")
public class BizRebateStatistics {
	private Long id;
	private Date statisticsDate;
	private Integer newAccTotal;
	private Integer quitAccTotal;
	private Integer accTotal;
	private Integer newAccNewCard;
	private Integer nowAccNewCard;
	private Integer quitCardTotal;
	private Integer cardTotal;
	private Integer enableCardTotal;
	private Integer disableCardTotal;
	private Integer freezeCardTotal;
	private Integer newAccUpgradeCredits;
	private Integer nowAccUpgradeCredits;
	private Integer reduceCredits;
	private Integer creditsTotal;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "statistics_date")
	public Date getStatisticsDate() {
		return statisticsDate;
	}

	public void setStatisticsDate(Date statisticsDate) {
		this.statisticsDate = statisticsDate;
	}

	@Column(name = "new_acc_total")
	public Integer getNewAccTotal() {
		return newAccTotal;
	}

	public void setNewAccTotal(Integer newAccTotal) {
		this.newAccTotal = newAccTotal;
	}

	@Column(name = "quit_acc_total")
	public Integer getQuitAccTotal() {
		return quitAccTotal;
	}

	public void setQuitAccTotal(Integer quitAccTotal) {
		this.quitAccTotal = quitAccTotal;
	}

	@Column(name = "acc_total")
	public Integer getAccTotal() {
		return accTotal;
	}

	public void setAccTotal(Integer accTotal) {
		this.accTotal = accTotal;
	}

	@Column(name = "new_acc_new_card")
	public Integer getNewAccNewCard() {
		return newAccNewCard;
	}

	public void setNewAccNewCard(Integer newAccNewCard) {
		this.newAccNewCard = newAccNewCard;
	}

	@Column(name = "now_acc_new_card")
	public Integer getNowAccNewCard() {
		return nowAccNewCard;
	}

	public void setNowAccNewCard(Integer nowAccNewCard) {
		this.nowAccNewCard = nowAccNewCard;
	}

	@Column(name = "quit_card_total")
	public Integer getQuitCardTotal() {
		return quitCardTotal;
	}

	public void setQuitCardTotal(Integer quitCardTotal) {
		this.quitCardTotal = quitCardTotal;
	}

	@Column(name = "card_total")
	public Integer getCardTotal() {
		return cardTotal;
	}

	public void setCardTotal(Integer cardTotal) {
		this.cardTotal = cardTotal;
	}

	@Column(name = "enable_card_total")
	public Integer getEnableCardTotal() {
		return enableCardTotal;
	}

	public void setEnableCardTotal(Integer enableCardTotal) {
		this.enableCardTotal = enableCardTotal;
	}

	@Column(name = "disable_card_total")
	public Integer getDisableCardTotal() {
		return disableCardTotal;
	}

	public void setDisableCardTotal(Integer disableCardTotal) {
		this.disableCardTotal = disableCardTotal;
	}

	@Column(name = "freeze_card_total")
	public Integer getFreezeCardTotal() {
		return freezeCardTotal;
	}

	public void setFreezeCardTotal(Integer freezeCardTotal) {
		this.freezeCardTotal = freezeCardTotal;
	}

	@Column(name = "new_acc_upgrade_credits")
	public Integer getNewAccUpgradeCredits() {
		return newAccUpgradeCredits;
	}

	public void setNewAccUpgradeCredits(Integer newAccUpgradeCredits) {
		this.newAccUpgradeCredits = newAccUpgradeCredits;
	}

	@Column(name = "now_acc_upgrade_credits")
	public Integer getNowAccUpgradeCredits() {
		return nowAccUpgradeCredits;
	}

	public void setNowAccUpgradeCredits(Integer nowAccUpgradeCredits) {
		this.nowAccUpgradeCredits = nowAccUpgradeCredits;
	}

	@Column(name = "reduce_credits")
	public Integer getReduceCredits() {
		return reduceCredits;
	}

	public void setReduceCredits(Integer reduceCredits) {
		this.reduceCredits = reduceCredits;
	}

	@Column(name = "credits_total")
	public Integer getCreditsTotal() {
		return creditsTotal;
	}

	public void setCreditsTotal(Integer creditsTotal) {
		this.creditsTotal = creditsTotal;
	}
	
	public BizRebateStatistics() {
		super();
	}

	public BizRebateStatistics(Long id, Date statisticsDate, Integer newAccTotal, Integer quitAccTotal,
			Integer accTotal, Integer newAccNewCard, Integer nowAccNewCard, Integer quitCardTotal, Integer cardTotal,
			Integer enableCardTotal, Integer disableCardTotal, Integer freezeCardTotal, Integer newAccUpgradeCredits,
			Integer nowAccUpgradeCredits, Integer reduceCredits, Integer creditsTotal) {
		super();
		this.id = id;
		this.statisticsDate = statisticsDate;
		this.newAccTotal = newAccTotal;
		this.quitAccTotal = quitAccTotal;
		this.accTotal = accTotal;
		this.newAccNewCard = newAccNewCard;
		this.nowAccNewCard = nowAccNewCard;
		this.quitCardTotal = quitCardTotal;
		this.cardTotal = cardTotal;
		this.enableCardTotal = enableCardTotal;
		this.disableCardTotal = disableCardTotal;
		this.freezeCardTotal = freezeCardTotal;
		this.newAccUpgradeCredits = newAccUpgradeCredits;
		this.nowAccUpgradeCredits = nowAccUpgradeCredits;
		this.reduceCredits = reduceCredits;
		this.creditsTotal = creditsTotal;
	}

}
