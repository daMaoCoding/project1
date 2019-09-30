/**
 * 
 */
package com.xinbo.fundstransfer.daifucomponent.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.xinbo.fundstransfer.daifucomponent.dto.output.DaifuResult;

/**
 * @author blake
 *
 */
@Entity
@Table(name = "biz_daifu_info")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DaifuInfo implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3348936546849254055L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Integer id;
	@Column(name = "handicap")
	private Integer handicapId;
	@Column(name = "outward_task_id")
	private Long outwardTaskId;
	@Column(name = "outward_request_id")
	private Long outwardRequestId;
	@Column(name = "outward_request_order_no")
	private String outwardRequestOrderNo;
	@Column(name = "daifu_config_id")
	private Integer daifuConfigId;
	
	@Column(name = "channel_name")
	private String channelName;
	
	@Column(name = "member_id")
	private String memberId;
	
	@Column(name = "channel_bank_name")
	private String channelBankName;
	@Column(name = "level_id")
	private Integer level;
	@Column(name = "user_name")
	private String userName;
	@Column(name = "plat_pay_code")
	private String platPayCode;
	@Column(name = "rule_out_fee")
	private BigDecimal ruleOutFee;
	@Column(name = "exact_money")
	private BigDecimal exactMoney;
	@Column(name = "plat_status")
	private Byte platStatus;
	@Column(name = "plat_req_param")
	private String platReqParam;
	@Column(name = "error_msg")
	private String errorMsg;
	@Column(name = "create_time")
	private Timestamp createTime;
	@Column(name = "create_admin_id")
	private Integer createAdminId;
	@Column(name = "intervene_admin_id")
	private Integer interveneAdminId;
	@Column(name = "uptime")
	private Timestamp uptime;
	@Column(name = "remark")
	private String remark;
	
	public DaifuResult toResult4Outward() {
		DaifuResult result = new DaifuResult();
		result.setHandicapId(this.handicapId);
		result.setOutwardTaskOrderNo(this.outwardRequestOrderNo);
		result.setPlatPayCode(this.platPayCode);
		result.setDaifuConfigId(this.daifuConfigId);
		result.setOutwardTaskId(this.outwardTaskId);
		result.setChannelName(this.channelName);
		result.setMemberId(this.memberId);
		if(DaifuResult.ResultEnum.UNKOWN.getValue().equals(this.platStatus)) {
			result.setResult(DaifuResult.ResultEnum.UNKOWN);
		}
		if(DaifuResult.ResultEnum.SUCCESS.getValue().equals(this.platStatus)) {
			result.setResult(DaifuResult.ResultEnum.SUCCESS);
		}
		if(DaifuResult.ResultEnum.ERROR.getValue().equals(this.platStatus)) {
			result.setResult(DaifuResult.ResultEnum.ERROR);
			result.setErrorMsg(this.errorMsg);
		}
		if(DaifuResult.ResultEnum.PAYING.getValue().equals(this.platStatus)) {
			result.setResult(DaifuResult.ResultEnum.PAYING);
		}
		result.setCreateTime(this.createTime);
		return result;
	}
	
	
	/**
	 * @return the id
	 */
	public Integer getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(Integer id) {
		this.id = id;
	}
	/**
	 * @return the handicap
	 */
	public Integer getHandicapId() {
		return handicapId;
	}
	/**
	 * @param handicap the handicapId to set
	 */
	public void setHandicapId(Integer handicap) {
		this.handicapId = handicap;
	}
	/**
	 * @return the outConfigId
	 */
	public Integer getDaifuConfigId() {
		return daifuConfigId;
	}
	/**
	 * @param outConfigId the outConfigId to set
	 */
	public void setDaifuConfigId(Integer outConfigId) {
		this.daifuConfigId = outConfigId;
	}
	/**
	 * @return the level
	 */
	public Integer getLevel() {
		return level;
	}
	/**
	 * @param level the level to set
	 */
	public void setLevel(Integer level) {
		this.level = level;
	}
	/**
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}
	/**
	 * @param userName the userName to set
	 */
	public void setUserName(String userName) {
		this.userName = userName;
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
	 * @return the ruleOutFee
	 */
	public BigDecimal getRuleOutFee() {
		return ruleOutFee;
	}
	/**
	 * @param ruleOutFee the ruleOutFee to set
	 */
	public void setRuleOutFee(BigDecimal ruleOutFee) {
		this.ruleOutFee = ruleOutFee;
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
	 * @return the platReqParam
	 */
	public String getPlatReqParam() {
		return platReqParam;
	}
	/**
	 * @param platReqParam the platReqParam to set
	 */
	public void setPlatReqParam(String platReqParam) {
		this.platReqParam = platReqParam;
	}
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
	 * @return the createAdminId
	 */
	public Integer getCreateAdminId() {
		return createAdminId;
	}
	/**
	 * @param createAdminId the createAdminId to set
	 */
	public void setCreateAdminId(Integer createAdminId) {
		this.createAdminId = createAdminId;
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
	 * @return the channelBankName
	 */
	public String getChannelBankName() {
		return channelBankName;
	}
	/**
	 * @param channelBankName the channelBankName to set
	 */
	public void setChannelBankName(String channelBankName) {
		this.channelBankName = channelBankName;
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

	/**
	 * @return the outwardTaskId
	 */
	public Long getOutwardTaskId() {
		return outwardTaskId;
	}

	/**
	 * @param outwardTaskId the outwardTaskId to set
	 */
	public void setOutwardTaskId(Long outwardTaskId) {
		this.outwardTaskId = outwardTaskId;
	}

	/**
	 * @return the outwardRequestId
	 */
	public Long getOutwardRequestId() {
		return outwardRequestId;
	}

	/**
	 * @param outwardRequestId the outwardRequestId to set
	 */
	public void setOutwardRequestId(Long outwardRequestId) {
		this.outwardRequestId = outwardRequestId;
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
	 * @return the interveneAdminId
	 */
	public Integer getInterveneAdminId() {
		return interveneAdminId;
	}

	/**
	 * @param interveneAdminId the interveneAdminId to set
	 */
	public void setInterveneAdminId(Integer interveneAdminId) {
		this.interveneAdminId = interveneAdminId;
	}

	/**
	 * @return the channelName
	 */
	public String getChannelName() {
		return channelName;
	}

	/**
	 * @param channelName the channelName to set
	 */
	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}

	/**
	 * @return the memberId
	 */
	public String getMemberId() {
		return memberId;
	}

	/**
	 * @param memberId the memberId to set
	 */
	public void setMemberId(String memberId) {
		this.memberId = memberId;
	}
	
}
