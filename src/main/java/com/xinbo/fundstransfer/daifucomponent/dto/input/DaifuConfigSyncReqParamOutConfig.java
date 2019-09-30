/**
 * 
 */
package com.xinbo.fundstransfer.daifucomponent.dto.input;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author blake
 *
 */
public class DaifuConfigSyncReqParamOutConfig implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3235834056301749645L;

	/**
	 * （会员每日）时间间隔，单位小时
	 */
	private Integer timeLimit;
	/**
	 * （会员每日）免手续费次数
	 */
	private Integer nofeeTimes;
	/**
	 * （会员每日）最大出款次数
	 */
	private Integer outMax;
	/**
	 * （会员每日）最大累计出款金额
	 */
	private BigDecimal dayMax;
	/**
	 * （会员每次）最大出款金额
	 */
	private BigDecimal onceMax;
	/**
	 * （会员每次）最小出款金额
	 */
	private BigDecimal onceMin;

	public void setTimeLimit(Integer timeLimit) {
		this.timeLimit = timeLimit;
	}

	public Integer getTimeLimit() {
		return timeLimit;
	}

	public void setNofeeTimes(Integer nofeeTimes) {
		this.nofeeTimes = nofeeTimes;
	}

	public Integer getNofeeTimes() {
		return nofeeTimes;
	}

	public void setOutMax(Integer outMax) {
		this.outMax = outMax;
	}

	public Integer getOutMax() {
		return outMax;
	}

	public void setDayMax(BigDecimal dayMax) {
		this.dayMax = dayMax;
	}

	public BigDecimal getDayMax() {
		return dayMax;
	}

	public void setOnceMax(BigDecimal onceMax) {
		this.onceMax = onceMax;
	}

	public BigDecimal getOnceMax() {
		return onceMax;
	}

	public void setOnceMin(BigDecimal onceMin) {
		this.onceMin = onceMin;
	}

	public BigDecimal getOnceMin() {
		return onceMin;
	}

	@Override
	public String toString() {
		StringBuffer str= new StringBuffer("{") ;
		if(this.timeLimit!=null) {
			str.append("\"timeLimit\":").append(this.timeLimit).append(",");
		}
		if(this.nofeeTimes!=null) {
			str.append("\"nofeeTimes\":").append(this.nofeeTimes).append(",");
		}
		if(this.outMax!=null) {
			str.append("\"outMax\":").append(this.outMax).append(",");
		}
		if(this.dayMax!=null) {
			str.append("\"dayMax\":").append(this.dayMax).append(",");
		}
		if(this.onceMax!=null) {
			str.append("\"onceMax\":").append(this.onceMax).append(",");
		}
		if(this.onceMin!=null) {
			str.append("\"onceMin\":").append(this.onceMin).append(",");
		}
		if(str.toString().endsWith(",")) {
			str = new StringBuffer(str.substring(0, str.length()-1));
		}
		str.append("}");
		return str.toString();
	}
}
