/**
 * 
 */
package com.xinbo.fundstransfer.accountfee.pojo;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * @author Blake
 *
 */
public class AccountFeeCalFeeLevelBase implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1917908843643766938L;
	
	private Long index;
	private Double moneyBegin;
	private Double moneyEnd;
	private String createName;
	private Long createTime;
	
	public Long getIndex() {
		return index;
	}
	public void setIndex(Long index) {
		this.index = index;
	}
	public String getMoneyRangeStr() {
		if(this.moneyBegin ==null || this.moneyEnd==null) {
			return null;
		}
		return String.format("%s%s", this.moneyBegin,Double.isInfinite(this.moneyEnd)?"以上":(String.format("~%s",this.moneyEnd)));
	}
	public String getCreateName() {
		return createName;
	}
	public void setCreateName(String createName) {
		this.createName = createName;
	}
	public Long getCreateTime() {
		return createTime;
	}
	public void setCreateTime(Long createTime) {
		this.createTime = createTime;
	}
	public Double getMoneyBegin() {
		return moneyBegin;
	}
	public void setMoneyBegin(Double moneyBegin) {
		this.moneyBegin = moneyBegin;
	}
	public Double getMoneyEnd() {
		return moneyEnd;
	}
	public void setMoneyEnd(Double moneyEnd) {
		this.moneyEnd = moneyEnd;
	}

}
