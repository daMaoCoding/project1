package com.xinbo.fundstransfer.domain.pojo;

import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.spring.SpringContextUtils;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.service.AccountService;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

@Data
public class FindDrawTaskOutputDTO implements Serializable {
	/**
	 * 实际下发 金额
	 */
	private BigDecimal drawedAmount;
	/**
	 * 实际下发 手续费
	 */
	private BigDecimal drawedFee;

	/**
	 * 下发使用的第三方账号id
	 */
	private Integer thirdAccountId;
	/**
	 * 选中的第三方名称
	 */
	private String thirdName;
	/**
	 * 提现时间 (用于计算下发耗时)
	 */
	private Long drawTime;
	/**
	 * 锁定时间 解锁 其他人锁定把锁定时间置空
	 */
	private Long lockTime;
	/**
	 * 出款卡 添加时间 计算总耗时
	 */
	private Long addTime;
	/** 锁定人 */
	private String lockerName;
	/**
	 * 标识 未下发 1 已锁定 2 待到账 3
	 */
	private Byte isDrawing;
	/**
	 * 账号此刻之前的入款流水
	 */
	private BigDecimal allInFlowAmount;

	private Integer id;

	private String account;
	private String bankType;
	private String bankName;
	private Integer status;
	private Integer type;
	private String alias;
	private String owner;
	private BigDecimal balance;
	private BigDecimal bankBalance;

	private Integer limitOut;
	private Integer currSysLevel;
	private Integer flag;

	private String typeStr;
	private String statusStr;
	private String creatorStr;
	private String modifierStr;
	private String createTimeStr;
	private String updateTimeStr;

	private BigDecimal amount;
	private BigDecimal fee;
	private Long timeConsuming;
	private Date createTime;
	private Date updateTime;
	/**
	 * 三方账号
	 */
	private String thirdAccount;

	private String handicap;

	private BigDecimal thirdBalance;

	private BigDecimal thirdBankBalance;
	private Integer payee;
	private Integer inType;
	private String memberRealName;

	public FindDrawTaskOutputDTO() {

	}

	public FindDrawTaskOutputDTO(BizAccount account) {
		this.id = account.getId();
		this.currSysLevel = account.getCurrSysLevel();
		this.alias = account.getAlias();
		this.flag = account.getFlag();
		this.account = account.getAccount();
		this.bankType = account.getBankType();
		this.bankName = account.getBankName();
		this.status = account.getStatus();
		this.type = account.getType();

		this.owner = account.getOwner();
		this.balance = account.getBalance();
		this.bankBalance = account.getBankBalance();

		this.limitOut = account.getLimitOut();

		this.typeStr = account.getTypeStr();
		this.statusStr = account.getStatusStr();

	}

	public FindDrawTaskOutputDTO getOutPut(Map obj, FindDrawTaskOutputDTO outputDTO) {
		if (obj == null ) {
			return outputDTO;
		}

		AccountService accountService = SpringContextUtils.getBean(AccountService.class);
		String         id             = obj.get("id").toString();
		Integer        integer        = Integer.valueOf(id);
		BizAccount account = accountService.getById(integer);
		outputDTO = new FindDrawTaskOutputDTO(account);

		// 封装第三方信息
		outputDTO.setThirdAccountId( obj.get("from_id") != null ?
				Integer.valueOf(obj.get("from_id").toString()) : Integer.valueOf(obj.get("to_id").toString()) );
		// a.id ,a.alias, a.curr_sys_level,a.flag ,
		// a.status,a.type,a.account,a.bank_type,a.bank_name,a.owner,a.bank_balance,a.balance,a.limit_out,i.amount,i.fee,i.time_consuming

		// a.id ,i.amount,i.fee,i.time_consuming
		if (obj.get("amount") != null) {
			outputDTO.setAmount(new BigDecimal(obj.get("amount").toString()));
		}
		if (obj.get("fee") != null) {
			outputDTO.setFee(new BigDecimal(obj.get("fee").toString()));
		}
		if (null != obj.get("createTime") && !"0".equalsIgnoreCase(obj.get("createTime").toString())) {
			Date date = CommonUtils.string2Date(obj.get("createTime").toString());
			outputDTO.setCreateTime(date);
		}
		if (null != obj.get("updateTime") && !"0".equalsIgnoreCase(obj.get("updateTime").toString())) {
			Date date = CommonUtils.string2Date(obj.get("updateTime").toString());
			outputDTO.setUpdateTime(date);
		}
		if (obj.get("timeConsuming") != null) {
			outputDTO.setTimeConsuming(Long.valueOf(obj.get("timeConsuming").toString()));
		} else {
			if (outputDTO.getUpdateTime() != null && outputDTO.getCreateTime() != null) {
				outputDTO.setTimeConsuming(
						(outputDTO.getUpdateTime().getTime() - outputDTO.getCreateTime().getTime()) / 1000);
			}
		}
		outputDTO.setThirdBalance(BigDecimal.ZERO);
		outputDTO.setThirdBankBalance(BigDecimal.ZERO);
		if (null != obj.get("thirdBalance") && !"0".equalsIgnoreCase(obj.get("thirdBalance").toString())) {
			outputDTO.setThirdBalance(new BigDecimal(obj.get("thirdBalance").toString()));
		}
		if (null != obj.get("thirdBankBalance") && !"0".equalsIgnoreCase(obj.get("thirdBankBalance").toString())) {
			outputDTO.setThirdBankBalance(new BigDecimal(obj.get("thirdBankBalance").toString()));
		}
		if (null != obj.get("memberRealName") && !"".equalsIgnoreCase(obj.get("memberRealName").toString())) {
			outputDTO.setMemberRealName(obj.get("memberRealName").toString());
		}

		return outputDTO;
	}
/*
	public FindDrawTaskOutputDTO getOutPut(Object[] obj, FindDrawTaskOutputDTO outputDTO) {
		if (obj == null || obj.length == 0) {
			return outputDTO;
		}

		AccountService accountService = SpringContextUtils.getBean(AccountService.class);
		BizAccount account = accountService.getById(Integer.valueOf(obj[0].toString()));
		outputDTO = new FindDrawTaskOutputDTO(account);
		// a.id ,a.alias, a.curr_sys_level,a.flag ,
		// a.status,a.type,a.account,a.bank_type,a.bank_name,a.owner,a.bank_balance,a.balance,a.limit_out,i.amount,i.fee,i.time_consuming

		// a.id ,i.amount,i.fee,i.time_consuming
		if (obj[1] != null) {
			outputDTO.setAmount(new BigDecimal(obj[1].toString()));
		}
		if (obj[2] != null) {
			outputDTO.setFee(new BigDecimal(obj[2].toString()));
		}
		if (null != obj[6] && !"0".equalsIgnoreCase(obj[6].toString())) {
			Date date = CommonUtils.string2Date(obj[6].toString());
			outputDTO.setCreateTime(date);
		}
		if (null != obj[7] && !"0".equalsIgnoreCase(obj[7].toString())) {
			Date date = CommonUtils.string2Date(obj[7].toString());
			outputDTO.setUpdateTime(date);
		}
		if (obj[3] != null) {
			outputDTO.setTimeConsuming(Long.valueOf(obj[3].toString()));
		} else {
			if (outputDTO.getUpdateTime() != null && outputDTO.getCreateTime() != null) {
				outputDTO.setTimeConsuming(
						(outputDTO.getUpdateTime().getTime() - outputDTO.getCreateTime().getTime()) / 1000);
			}
		}
		if (null != obj[5]) {
			outputDTO.setThirdAccountId(Integer.valueOf(obj[5].toString()));
		}
		outputDTO.setThirdBalance(BigDecimal.ZERO);
		outputDTO.setThirdBankBalance(BigDecimal.ZERO);
		if (null != obj[8] && !"0".equalsIgnoreCase(obj[8].toString())) {
			outputDTO.setThirdBalance(new BigDecimal(obj[8].toString()));
		}
		if (null != obj[9] && !"0".equalsIgnoreCase(obj[9].toString())) {
			outputDTO.setThirdBankBalance(new BigDecimal(obj[9].toString()));
		}
		return outputDTO;
	}*/

	@Override
	public String toString() {
		return "FindDrawTaskOutputDTO{" + "drawedAmount=" + drawedAmount + ", drawedFee=" + drawedFee
				+ ", thirdAccountId=" + thirdAccountId + ", thirdName='" + thirdName + '\'' + ", drawTime=" + drawTime
				+ ", lockTime=" + lockTime + ", addTime=" + addTime + ", lockerName='" + lockerName + '\''
				+ ", isDrawing=" + isDrawing + ", allInFlowAmount=" + allInFlowAmount + ", id=" + id + ", account='"
				+ account + '\'' + ", bankType='" + bankType + '\'' + ", bankName='" + bankName + '\'' + ", status="
				+ status + ", type=" + type + ", alias='" + alias + '\'' + ", owner='" + owner + '\'' + ", balance="
				+ balance + ", bankBalance=" + bankBalance + ", limitOut=" + limitOut + ", currSysLevel=" + currSysLevel
				+ ", flag=" + flag + ", typeStr='" + typeStr + '\'' + ", statusStr='" + statusStr + '\''
				+ ", creatorStr='" + creatorStr + '\'' + ", modifierStr='" + modifierStr + '\'' + ", createTimeStr='"
				+ createTimeStr + '\'' + ", updateTimeStr='" + updateTimeStr + '\'' + ", amount=" + amount + ", fee="
				+ fee + ", timeConsuming=" + timeConsuming + ", createTime=" + createTime + ", updateTime=" + updateTime
				+ ", thirdAccount='" + thirdAccount + '\'' + ", handicap='" + handicap + '\'' + ", thirdBalance="
				+ thirdBalance + ", thirdBankBalance=" + thirdBankBalance +", payee=" + payee +", inType=" + inType+", memberRealName=" + memberRealName + '}';
	}
}
