package com.xinbo.fundstransfer.domain.entity;

import java.math.BigDecimal;

public class FinMoreStat implements java.io.Serializable {

	private int id;
	private String handicapname;
	private String levelname;
	private int countinps;
	private int countin;
	private BigDecimal amountinbalance;
	private BigDecimal amountinactualamount;
	private int countoutps;
	private int countout;
	private BigDecimal countoutfee;
	private BigDecimal amountoutbalance;
	private BigDecimal amountoutactualamount;
	private BigDecimal profit;

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @return the handicapname
	 */
	public String getHandicapname() {
		return handicapname;
	}

	/**
	 * @param handicapname
	 *            the handicapname to set
	 */
	public void setHandicapname(String handicapname) {
		this.handicapname = handicapname;
	}

	/**
	 * @return the countinps
	 */
	public int getCountinps() {
		return countinps;
	}

	/**
	 * @param countinps
	 *            the countinps to set
	 */
	public void setCountinps(int countinps) {
		this.countinps = countinps;
	}

	/**
	 * @return the countin
	 */
	public int getCountin() {
		return countin;
	}

	/**
	 * @param countin
	 *            the countin to set
	 */
	public void setCountin(int countin) {
		this.countin = countin;
	}

	/**
	 * @return the amountinbalance
	 */
	public BigDecimal getAmountinbalance() {
		return amountinbalance;
	}

	/**
	 * @param amountinbalance
	 *            the amountinbalance to set
	 */
	public void setAmountinbalance(BigDecimal amountinbalance) {
		this.amountinbalance = amountinbalance;
	}

	/**
	 * @return the amountinactualamount
	 */
	public BigDecimal getAmountinactualamount() {
		return amountinactualamount;
	}

	/**
	 * @param amountinactualamount
	 *            the amountinactualamount to set
	 */
	public void setAmountinactualamount(BigDecimal amountinactualamount) {
		this.amountinactualamount = amountinactualamount;
	}

	/**
	 * @return the countoutps
	 */
	public int getCountoutps() {
		return countoutps;
	}

	/**
	 * @param countoutps
	 *            the countoutps to set
	 */
	public void setCountoutps(int countoutps) {
		this.countoutps = countoutps;
	}

	/**
	 * @return the countout
	 */
	public int getCountout() {
		return countout;
	}

	/**
	 * @param countout
	 *            the countout to set
	 */
	public void setCountout(int countout) {
		this.countout = countout;
	}

	/**
	 * @return the countoutfee
	 */
	public BigDecimal getCountoutfee() {
		return countoutfee;
	}

	/**
	 * @param countoutfee
	 *            the countoutfee to set
	 */
	public void setCountoutfee(BigDecimal countoutfee) {
		this.countoutfee = countoutfee;
	}

	/**
	 * @return the amountoutbalance
	 */
	public BigDecimal getAmountoutbalance() {
		return amountoutbalance;
	}

	/**
	 * @param amountoutbalance
	 *            the amountoutbalance to set
	 */
	public void setAmountoutbalance(BigDecimal amountoutbalance) {
		this.amountoutbalance = amountoutbalance;
	}

	/**
	 * @return the amountoutactualamount
	 */
	public BigDecimal getAmountoutactualamount() {
		return amountoutactualamount;
	}

	/**
	 * @param amountoutactualamount
	 *            the amountoutactualamount to set
	 */
	public void setAmountoutactualamount(BigDecimal amountoutactualamount) {
		this.amountoutactualamount = amountoutactualamount;
	}

	/**
	 * @return the profit
	 */
	public BigDecimal getProfit() {
		return profit;
	}

	/**
	 * @param profit
	 *            the profit to set
	 */
	public void setProfit(BigDecimal profit) {
		this.profit = profit;
	}

	/**
	 * @return the levelname
	 */
	public String getLevelname() {
		return levelname;
	}

	/**
	 * @param levelname
	 *            the levelname to set
	 */
	public void setLevelname(String levelname) {
		this.levelname = levelname;
	}

	public FinMoreStat(int id, String handicapname, String levelname, int countinps, int countin,
			BigDecimal amountinbalance, BigDecimal amountinactualamount, int countoutps, int countout,
			BigDecimal countoutfee, BigDecimal amountoutbalance, BigDecimal amountoutactualamount, BigDecimal profit) {
		super();
		this.id = id;
		this.handicapname = handicapname;
		this.levelname = levelname;
		this.countinps = countinps;
		this.countin = countin;
		this.amountinbalance = amountinbalance;
		this.amountinactualamount = amountinactualamount;
		this.countoutps = countoutps;
		this.countout = countout;
		this.countoutfee = countoutfee;
		this.amountoutbalance = amountoutbalance;
		this.amountoutactualamount = amountoutactualamount;
		this.profit = profit;
	}

	public FinMoreStat() {
		super();
	}

}
