/**
 * 
 */
package com.xinbo.fundstransfer.daifucomponent.dto.input;

import java.io.Serializable;

/**
 * @author blake
 *
 */
public class DaifuConfigSyncReqParamWarnConfig implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2731291637463817L;
	private Byte type;
	private Double percent;
	private Double money;

	public void setType(Byte type) {
		this.type = type;
	}

	public int getType() {
		return type;
	}

	public void setPercent(double percent) {
		this.percent = percent;
	}

	public double getPercent() {
		return percent;
	}

	public void setMoney(Double money) {
		this.money = money;
	}

	public Double getMoney() {
		return money;
	}
	
	@Override
	public String toString() {
		StringBuffer str= new StringBuffer("{") ;
		if(this.type!=null) {
			str.append("\"type\":").append(this.type).append(",");
		}
		if(this.percent!=null) {
			str.append("\"percent\":").append(this.percent).append(",");
		}
		if(this.money!=null) {
			str.append("\"money\":").append(this.money).append(",");
		}
		if(str.toString().endsWith(",")) {
			str = new StringBuffer(str.substring(0, str.length()-1));
		}
		str.append("}");
		return str.toString();
	}
}
