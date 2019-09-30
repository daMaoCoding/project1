package com.xinbo.fundstransfer.domain.pojo;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Created by 000 on 2017/10/6.
 */
public class AccountStatInOut {

	private int id;

	private int mapping = 0;

	private int mapped = 0;

	private int cancel = 0;

	/**
	 * 1:转入统计 2：转出统计
	 */
	private int category = 0;

	private String fromTo;

	public AccountStatInOut() {
	}

	public AccountStatInOut(Category category0) {
		if (category0 != null) {
			this.category = category0.getValue();
		}
	}

	public AccountStatInOut(int id, int category0) {
		this.category = category0;
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getMapping() {
		return mapping;
	}

	public void setMapping(BigDecimal mapping) {
		this.mapping = mapping == null ? 0 : mapping.intValue();
	}

	public int getMapped() {
		return mapped;
	}

	public void setMapped(BigDecimal mapped) {
		this.mapped = mapped == null ? 0 : mapped.intValue();
	}

	public int getCancel() {
		return cancel;
	}

	public void setCancel(BigDecimal cancel) {
		this.cancel = cancel == null ? 0 : cancel.intValue();
	}

	public int getCategory() {
		return category;
	}

	public void setCategory(BigInteger category) {
		this.category = category == null ? null : category.intValue();
	}

	public String getFromTo() {
		return fromTo;
	}

	public void setFromTo(String fromTo) {
		this.fromTo = fromTo;
	}

	public enum Category {
		In(0), OutTranfer(1), OutMember(2);
		private int type;

		Category(int value) {
			type = value;
		}

		public int getValue() {
			return type;
		}

		public static Category findCategory(int value) {
			for (Category cat : Category.values()) {
				if (cat.getValue() == value) {
					return cat;
				}
			}
			return null;
		}
	}
}
