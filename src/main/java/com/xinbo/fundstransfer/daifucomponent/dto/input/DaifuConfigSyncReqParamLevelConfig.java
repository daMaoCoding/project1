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
public class DaifuConfigSyncReqParamLevelConfig implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6515245780555027742L;
	private String levelName;
	private BigDecimal outOnceMin;
	private BigDecimal outOnceMax;
	private BigDecimal outDayMax;

	public void setOutOnceMin(BigDecimal outOnceMin) {
		this.outOnceMin = outOnceMin;
	}

	public BigDecimal getOutOnceMin() {
		return outOnceMin;
	}

	public void setOutOnceMax(BigDecimal outOnceMax) {
		this.outOnceMax = outOnceMax;
	}

	public BigDecimal getOutOnceMax() {
		return outOnceMax;
	}

	public void setOutDayMax(BigDecimal outDayMax) {
		this.outDayMax = outDayMax;
	}

	public BigDecimal getOutDayMax() {
		return outDayMax;
	}
	
	/**
	 * @return the levelName
	 */
	public String getLevelName() {
		return levelName;
	}

	/**
	 * @param levelName the levelName to set
	 */
	public void setLevelName(String levelName) {
		this.levelName = levelName;
	}

	@Override
	public String toString() {
		StringBuffer str= new StringBuffer("{") ;
		if(this.outOnceMin!=null) {
			str.append("\"outOnceMin\":").append(this.outOnceMin).append(",");
		}
		if(this.outOnceMax!=null) {
			str.append("\"outOnceMax\":").append(this.outOnceMax).append(",");
		}
		if(this.outDayMax!=null) {
			str.append("\"outDayMax\":").append(this.outDayMax).append(",");
		}
		if(this.levelName!=null) {
			str.append("\"levelName\":").append(this.levelName).append(",");
		}
		if(str.toString().endsWith(",")) {
			str = new StringBuffer(str.substring(0, str.length()-1));
		}
		str.append("}");
		return str.toString();
	}

}
