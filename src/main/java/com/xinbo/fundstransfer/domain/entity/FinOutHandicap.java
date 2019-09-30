package com.xinbo.fundstransfer.domain.entity;

import java.math.BigDecimal;

public class FinOutHandicap implements java.io.Serializable {

	private String handicapname;
	private int handicapno;
	private BigDecimal amount;
	private BigDecimal fee;
	private int conuts;
	private int level;
	private String levelname;
	private int id;

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
	 * @return the handicapno
	 */
	public int getHandicapno() {
		return handicapno;
	}

	/**
	 * @param handicapno
	 *            the handicapno to set
	 */
	public void setHandicapno(int handicapno) {
		this.handicapno = handicapno;
	}

	/**
	 * @return the amount
	 */
	public BigDecimal getAmount() {
		return amount;
	}

	/**
	 * @param amount
	 *            the amount to set
	 */
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	/**
	 * @return the fee
	 */
	public BigDecimal getFee() {
		return fee;
	}

	/**
	 * @param fee
	 *            the fee to set
	 */
	public void setFee(BigDecimal fee) {
		this.fee = fee;
	}

	/**
	 * @return the conuts
	 */
	public int getConuts() {
		return conuts;
	}

	/**
	 * @param conuts
	 *            the conuts to set
	 */
	public void setConuts(int conuts) {
		this.conuts = conuts;
	}

	/**
	 * @return the level
	 */
	public int getLevel() {
		return level;
	}

	/**
	 * @param level
	 *            the level to set
	 */
	public void setLevel(int level) {
		this.level = level;
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

	public FinOutHandicap(String handicapname, int handicapno, BigDecimal amount, BigDecimal fee, int conuts, int level,
			String levelname, int id) {
		super();
		this.handicapname = handicapname;
		this.handicapno = handicapno;
		this.amount = amount;
		this.fee = fee;
		this.conuts = conuts;
		this.level = level;
		this.levelname = levelname;
		this.id = id;
	}

	public FinOutHandicap() {
		super();
	}

}
