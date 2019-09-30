/**
 * 
 */
package com.xinbo.fundstransfer.accountfee.pojo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * @author Blake
 *
 */
public class AccountFeeConfig implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8559566631960643375L;
	
	@NotNull
	private String handicap;
	
	@NotNull
	private String bankType;
	
	@NotNull
	private String account;

	/**
	 *收费方式：0-从商户余额扣取手续费 1-从到账金额扣取手续费
	 */
	private Byte feeType;
	
	/**
	 * 手续费计算规则 0-按百分比计算  1-按阶梯式计算
	 */
	private Byte calFeeType;
	
	/**
	 * 统一收取费率
	 */
	private Float calFeePercent;
	
	/**
	 * 阶梯计费类型 0-按百分比计费，1-按金额计费 
	 */
	private Byte calFeeLevelType;
	
	/**
	 * 
	 */
	private List<AccountFeeCalFeeLevelMoney> calFeeLevelMoneyList;

	/**
	 * 
	 */
	private List<AccountFeeCalFeeLevelPercent> calFeeLevelPercentList;

	public Byte getFeeType() {
		return feeType;
	}

	public void setFeeType(Byte feeType) {
		this.feeType = feeType;
	}

	public Byte getCalFeeType() {
		return calFeeType;
	}

	public void setCalFeeType(Byte calFeeType) {
		this.calFeeType = calFeeType;
	}

	public Float getCalFeePercent() {
		return calFeePercent;
	}

	public void setCalFeePercent(Float calFeePercent) {
		this.calFeePercent = calFeePercent;
	}

	public Byte getCalFeeLevelType() {
		return calFeeLevelType;
	}

	public void setCalFeeLevelType(Byte calFeeLevelType) {
		this.calFeeLevelType = calFeeLevelType;
	}

	public List<AccountFeeCalFeeLevelMoney> getCalFeeLevelMoneyList() {
		return calFeeLevelMoneyList;
	}

	public void setCalFeeLevelMoneyList(List<AccountFeeCalFeeLevelMoney> calFeeLevelMoneyList) {
		this.calFeeLevelMoneyList = calFeeLevelMoneyList;
	}

	public List<AccountFeeCalFeeLevelPercent> getCalFeeLevelPercentList() {
		return calFeeLevelPercentList;
	}

	public void setCalFeeLevelPercentList(List<AccountFeeCalFeeLevelPercent> calFeeLevelPercentList) {
		this.calFeeLevelPercentList = calFeeLevelPercentList;
	}
	
	public String getHandicap() {
		return handicap;
	}

	public void setHandicap(String handicap) {
		this.handicap = handicap;
	}

	public String getBankType() {
		return bankType;
	}

	public void setBankType(String bankType) {
		this.bankType = bankType;
	}

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	/**
	 * 需求 7994 判断当前手续费规则是否有效规则
	 * <br>有效规则：存在一个规则使计算所得手续费>0
	 * <br>1、如果按固定百分比，则百分比必须大于0.
	 * <br>2、如果按阶梯计费：
	 * <br>2.1当使用按金额计费时，必须存在一个阶梯规则费用大于0
	 * <br>2.2当使用按百分比计费时，必须存在一个阶梯规则计费百分比大于 0
	 * @return true-有效 false-无效
	 */
	public boolean isEffect() {
		if(ObjectUtils.isEmpty(this.getFeeType())
				||ObjectUtils.isEmpty(this.getCalFeeType())
				) {
			return false;
		}
		if(this.getCalFeeType().intValue()==0) {
			return (!ObjectUtils.isEmpty(this.getCalFeePercent()))&&Float.compare(this.getCalFeePercent(), 0F)>0;
		}else if(!ObjectUtils.isEmpty(this.getCalFeeLevelType())) {
			if(this.getCalFeeLevelType().intValue()==0) {
				List<AccountFeeCalFeeLevelPercent> accountFeeCalFeeLevelPercentList= this.getCalFeeLevelPercentList(); 
				return CollectionUtils.isEmpty(accountFeeCalFeeLevelPercentList) && accountFeeCalFeeLevelPercentList
						.stream().anyMatch(t -> t.getFeePercent()!=null && t.getFeePercent().compareTo(BigDecimal.ZERO) > 0);
			}else {
				List<AccountFeeCalFeeLevelMoney> accountFeeCalFeeLevelMoneyList= this.getCalFeeLevelMoneyList(); 
				return CollectionUtils.isEmpty(accountFeeCalFeeLevelMoneyList) && accountFeeCalFeeLevelMoneyList
						.stream().anyMatch(t -> t.getFeeMoney()!=null && t.getFeeMoney().compareTo(BigDecimal.ZERO) > 0);
			}
		}
		return false;
	}
}
