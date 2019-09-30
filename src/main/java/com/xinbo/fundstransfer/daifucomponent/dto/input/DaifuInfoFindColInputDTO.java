/**
 * 
 */
package com.xinbo.fundstransfer.daifucomponent.dto.input;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

import javax.validation.constraints.NotNull;

/**
 * @author blake
 *
 */
public class DaifuInfoFindColInputDTO implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5587222332791587156L;
	
	/**
	 * 盘口id，必填
	 */
	@NotNull
	private Integer handicapId;
	/**
	 * 通道id，必填
	 */
	@NotNull
	private Long outConfigId;
	
	/**
	 * 没有记录数
	 */
	private Integer pageSize;
	
	/**
	 * 页码
	 */
	@NotNull
	private Integer pageNo;
	
	/**
	 * 开始时间
	 */
	private Timestamp createTimeBegin;
	
	/**
	 * 结束时间
	 */
	private Timestamp createTimeEnd;
	
	/**
	 * 状态
	 */
	private Byte platStatus;
	
	/**
	 * 订单金额
	 */
	private BigDecimal exactMoneyBegin;
	
	/**
	 * 订单金额
	 */
	private BigDecimal exactMoneyEnd;
	
	/**
	 * 出款订单号
	 */
	private String outwardRequestOrderNo;
	
	/**
	 * 代付订单号
	 */
	private String platPayCode;

	/**
	 * @return the createTimeBegin
	 */
	public Timestamp getCreateTimeBegin() {
		return createTimeBegin;
	}

	/**
	 * @param createTimeBegin the createTimeBegin to set
	 */
	public void setCreateTimeBegin(Timestamp createTimeBegin) {
		this.createTimeBegin = createTimeBegin;
	}

	/**
	 * @return the createTimeEnd
	 */
	public Timestamp getCreateTimeEnd() {
		return createTimeEnd;
	}

	/**
	 * @param createTimeEnd the createTimeEnd to set
	 */
	public void setCreateTimeEnd(Timestamp createTimeEnd) {
		this.createTimeEnd = createTimeEnd;
	}

	/**
	 * @return the handicapId
	 */
	public Integer getHandicapId() {
		return handicapId;
	}

	/**
	 * @param handicapId the handicapId to set
	 */
	public void setHandicapId(Integer handicapId) {
		this.handicapId = handicapId;
	}

	/**
	 * @return the outConfigId
	 */
	public Long getOutConfigId() {
		return outConfigId;
	}

	/**
	 * @param outConfigId the outConfigId to set
	 */
	public void setOutConfigId(Long outConfigId) {
		this.outConfigId = outConfigId;
	}

	/**
	 * @return the pageSize
	 */
	public Integer getPageSize() {
		return pageSize;
	}

	/**
	 * @param pageSize the pageSize to set
	 */
	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}

	/**
	 * @return the pageNo
	 */
	public Integer getPageNo() {
		return pageNo;
	}

	/**
	 * @param pageNo the pageNo to set
	 */
	public void setPageNo(Integer pageNo) {
		this.pageNo = pageNo;
	}

	/**
	 * @return the platStatus
	 */
	public Byte getPlatStatus() {
		return platStatus;
	}

	/**
	 * @param platStatus the platStatus to set
	 */
	public void setPlatStatus(Byte platStatus) {
		this.platStatus = platStatus;
	}

	/**
	 * @return the outwardRequestOrderNo
	 */
	public String getOutwardRequestOrderNo() {
		return outwardRequestOrderNo;
	}

	/**
	 * @param outwardRequestOrderNo the outwardRequestOrderNo to set
	 */
	public void setOutwardRequestOrderNo(String outwardRequestOrderNo) {
		this.outwardRequestOrderNo = outwardRequestOrderNo;
	}

	/**
	 * @return the platPayCode
	 */
	public String getPlatPayCode() {
		return platPayCode;
	}

	/**
	 * @param platPayCode the platPayCode to set
	 */
	public void setPlatPayCode(String platPayCode) {
		this.platPayCode = platPayCode;
	}

	/**
	 * @return the exactMoneyBegin
	 */
	public BigDecimal getExactMoneyBegin() {
		return exactMoneyBegin;
	}

	/**
	 * @param exactMoneyBegin the exactMoneyBegin to set
	 */
	public void setExactMoneyBegin(BigDecimal exactMoneyBegin) {
		this.exactMoneyBegin = exactMoneyBegin;
	}

	/**
	 * @return the exactMoneyEnd
	 */
	public BigDecimal getExactMoneyEnd() {
		return exactMoneyEnd;
	}

	/**
	 * @param exactMoneyEnd the exactMoneyEnd to set
	 */
	public void setExactMoneyEnd(BigDecimal exactMoneyEnd) {
		this.exactMoneyEnd = exactMoneyEnd;
	}
	
}
