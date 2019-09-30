package com.xinbo.fundstransfer.report.acc;

import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.enums.SysErrStatus;
import com.xinbo.fundstransfer.domain.enums.SysInvstType;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

public class SysAccPush<T> {

	/**
	 * 流水异常：重复流水
	 */
	public static final int ActionDuplicateStatement = -1;// SysInvstType.DuplicateStatement.getType()

	/**
	 * 系统订单：无效
	 */
	public static final int ActionInvalidTransfer = -2;// SysInvstType.InvalidTransfer.getType();

	/**
	 * 流水异常：额外收入
	 */
	public static final int ActionUnknowIncome = 1;// SysInvstType.UnknowIncome.getType();

	/**
	 * 流水异常：重复出款
	 */
	public static final int ActionDuplicateOutward = 2;// SysInvstType.DuplicateOutward.getType();

	/**
	 * 费用流水
	 */
	public static final int ActionFee = 3;// SysInvstType.Fee.getType();

	/**
	 * 流水处理：盗刷-兼职所为
	 */
	public static final int ActionUnkownOutwardPartTime = 4;// SysInvstType.UnkownOutwardByPartTime.getType();
	/**
	 * 流水异常：回冲
	 */
	public static final int ActionRefund = 5;// SysInvstType.Refund.getType();
	/**
	 * 流水处理：盗刷-非兼职所为
	 */
	public static final int ActionUnkownOutwardNonePartTime = 6;// SysInvstType.UnkownOutwardByNonePartTime.getType();

	/**
	 * 流水处理：盗刷-卡商
	 */
	public static final int ActionUnkownOutwardPC = 7;// SysInvstType.UnkownOutwardByPC.getType();
	/**
	 * 人工内部转出
	 */
	public static final int ActionManualTransOut = 8;// SysInvstType.ManualTransOut.getType();

	/**
	 * 人工内部转入
	 */
	public static final int ActionManualTransIn = 9;// SysInvstType.ManualTransIn.getType();

	public SysAccPush() {
	}

	public SysAccPush(int target, int classify, T data, String remark) {
		this.target = target;
		this.classify = classify;
		this.data = data;
		this.remark = remark;
	}

	public SysAccPush(Long errorId, int target, int classify, T data, SysUser operator, String remark, String orderNo,
			String orderType, SysErrStatus errStatus) {
		this.errorId = errorId;
		this.target = target;
		this.classify = classify;
		this.data = data;
		this.operator = operator;
		this.remark = remark;
		this.orderNo = StringUtils.trimToNull(orderNo);
		this.orderType = StringUtils.trimToNull(orderType);
		this.errSt = Objects.isNull(errStatus) ? null : String.valueOf(errStatus.getStatus());
	}

	private Long errorId;

	/**
	 * 账号ID
	 */
	private int target;
	private T data;
	/**
	 * 分类
	 */
	private int classify;

	/**
	 * 操作者
	 */
	private SysUser operator;

	/**
	 * 备注
	 */
	private String remark;

	private String orderNo = null;

	private String orderType = null;

	private String errSt = null;

	public Long getErrorId() {
		return errorId;
	}

	public void setErrorId(Long errorId) {
		this.errorId = errorId;
	}

	public int getTarget() {
		return target;
	}

	public void setTarget(int target) {
		this.target = target;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}

	public int getClassify() {
		return classify;
	}

	public void setClassify(int classify) {
		this.classify = classify;
	}

	public SysUser getOperator() {
		return operator;
	}

	public void setOperator(SysUser operator) {
		this.operator = operator;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public String getOrderNo() {
		return orderNo;
	}

	public void setOrderNo(String orderNo) {
		this.orderNo = orderNo;
	}

	public String getOrderType() {
		return orderType;
	}

	public void setOrderType(String orderType) {
		this.orderType = orderType;
	}

	public String getErrSt() {
		return errSt;
	}

	public void setErrSt(String errSt) {
		this.errSt = errSt;
	}
}
