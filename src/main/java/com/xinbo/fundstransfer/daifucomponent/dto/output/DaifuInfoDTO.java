/**
 * 
 */
package com.xinbo.fundstransfer.daifucomponent.dto.output;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 代付订单信息DTO
 * @author blake
 *
 */
public class DaifuInfoDTO implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 250430053670012273L;

	private Timestamp createTime;
	
	/**
	 * 处理结果<br>
	 * 对于页面而言：0-未知 3-正在支付 都属于待处理。1-完成  2-取消
	 */
	private Byte platStatus;
	
	/**
	 * 金额
	 */
	private BigDecimal exactMoney;
	
	/**
	 * 出款订单号
	 */
	private String outwardRequestOrderNo;
	
	/**
	 * 代付订单号
	 */
	private String platPayCode;
	
	/**
	 * 处理结果
	 */
	private String resultDesc;
	
	/**
	 * 操作人id
	 */
	private Integer operator;
	
	/**
	 * 操作人Uid
	 */
	private String operatorUid;
	
	/**
	 * 最后操作时间
	 */
	private Timestamp uptime;
	
	/**
	 * 备注
	 */
	private String remark;
	
	/**
	 * 第三方返回的错误消息
	 */
	private String errorMsg;

	/**
	 * 出入款系统中对出款任务的状态
	 */
	private Byte status;
	
	/**
	 * @return the createTime
	 */
	public Timestamp getCreateTime() {
		return createTime;
	}

	/**
	 * @param createTime the createTime to set
	 */
	public void setCreateTime(Timestamp createTime) {
		this.createTime = createTime;
	}

	/**
	 * @return the resultDesc
	 */
	public String getResultDesc() {
		return resultDesc;
	}

	/**
	 * @param resultDesc the resultDesc to set
	 */
	public void setResultDesc(String resultDesc) {
		this.resultDesc = resultDesc;
	}

	public Integer getOperator() {
		return operator;
	}

	public void setOperator(Integer operator) {
		this.operator = operator;
	}

	/**
	 * @return the uptime
	 */
	public Timestamp getUptime() {
		return uptime;
	}

	/**
	 * @param uptime the uptime to set
	 */
	public void setUptime(Timestamp uptime) {
		this.uptime = uptime;
	}

	/**
	 * @return the remark
	 */
	public String getRemark() {
		return remark;
	}

	/**
	 * @param remark the remark to set
	 */
	public void setRemark(String remark) {
		this.remark = remark;
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
	 * @return the exactMoney
	 */
	public BigDecimal getExactMoney() {
		return exactMoney;
	}

	/**
	 * @param exactMoney the exactMoney to set
	 */
	public void setExactMoney(BigDecimal exactMoney) {
		this.exactMoney = exactMoney;
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
	 * @return the status
	 */
	public Byte getStatus() {
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(Byte status) {
		this.status = status;
	}

	/**
	 * @return the errorMsg
	 */
	public String getErrorMsg() {
		return errorMsg;
	}

	/**
	 * @param errorMsg the errorMsg to set
	 */
	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}

	public String getOperatorUid() {
		return operatorUid;
	}

	public void setOperatorUid(String operatorUid) {
		this.operatorUid = operatorUid;
	}
	
}
