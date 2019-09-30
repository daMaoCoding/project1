/**
 * 
 */
package com.xinbo.fundstransfer.daifucomponent.dto.output;

import java.util.Date;

/**
 * 请求代付的结果
 *
 * @author blake
 *
 */
public class DaifuResult {

	public enum ResultEnum {
		UNKOWN((byte) 0, "未知"), SUCCESS((byte) 1, "完成（支付成功）"), ERROR((byte) 2, "取消"), PAYING((byte) 3,
				"正在支付"), TO_INTERVENE((byte) 4, "待排查"), NO_READ((byte) -124, "不满足使用第三方代付条件"), NO_DAIFU_INFO((byte) -125,
						"代付订单异常（没有构建代付订单）"), NO_OUT_REQUEST((byte) -126, "出款单异常（无出款单信息）"),

		PROC_EXCEPTION((byte) -127, "程序处理异常");

		private Byte value;
		private String desc;

		private ResultEnum(Byte b, String desc) {
			this.value = b;
			this.desc = desc;
		}

		public Byte getValue() {
			return this.value;
		}

		public String getDesc() {
			return this.desc;
		}
	}

	/**
	 * 盘口id
	 */
	private Integer handicapId;

	/**
	 * 盘口Code
	 */
	private String handicapCode;

	/**
	 * biz_outward_request.order_no
	 */
	private String outwardTaskOrderNo;
	
	/**
	 * biz_outward_task.id
	 */
	private Long outwardTaskId;

	/**
	 * 出款通道id
	 */
	private Integer daifuConfigId;

	/**
	 * 提供商名称
	 */
	private String channelName;

	/**
	 * 商号
	 */
	private String memberId;

	/**
	 * biz_daifu_info.plat_pay_code
	 */
	private String platPayCode;

	/**
	 * <pre>
	 * 支付结果:
	 * UNKOWN 可能由于网络原因，不能获取到准确的支付结果。此类状态的出款单需要查询第三方支付结果后才能进行后续处理
	 * SUCCESS 第三方已经成功出款，第三方已经支付完成，走出款成功逻辑
	 * ERROR 第三方取消，此时可以重新分配出款（包括第三方、自动、人工）
	 * PAYING 正在支付，此时不允许分配给其他第三方出款、自动出款或者进行人工出款
	 * DATA_EXCEPTION 数据不存在/没有构建代付订单等
	 * </pre>
	 */
	private ResultEnum result;

	/**
	 * 获取错误消息 <br>
	 * 仅当状态为取消时才有可能返回
	 */
	private String errorMsg;
	/**
	 * 代付订单创建时间
	 */
	private Date createTime;

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	/**
	 * @return the handicapId
	 */
	public Integer getHandicapId() {
		return handicapId;
	}

	/**
	 * @param handicapId
	 *            the handicapId to set
	 */
	public void setHandicapId(Integer handicapId) {
		this.handicapId = handicapId;
	}

	/**
	 * @return the platPayCode
	 */
	public String getPlatPayCode() {
		return platPayCode;
	}

	/**
	 * @param platPayCode
	 *            the platPayCode to set
	 */
	public void setPlatPayCode(String platPayCode) {
		this.platPayCode = platPayCode;
	}

	/**
	 * @return the result
	 */
	public ResultEnum getResult() {
		return result;
	}

	/**
	 * @param result
	 *            the result to set
	 */
	public void setResult(ResultEnum result) {
		this.result = result;
	}

	/**
	 * 获取错误消息 <br>
	 * 仅当状态为取消时才有可能返回
	 *
	 * @return the errorMsg
	 */
	public String getErrorMsg() {
		return errorMsg;
	}

	/**
	 * @param errorMsg
	 *            the errorMsg to set
	 */
	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}

	/**
	 * @return the outwardTaskOrderNo
	 */
	public String getOutwardTaskOrderNo() {
		return outwardTaskOrderNo;
	}

	/**
	 * @param outwardTaskOrderNo
	 *            the outwardTaskOrderNo to set
	 */
	public void setOutwardTaskOrderNo(String outwardTaskOrderNo) {
		this.outwardTaskOrderNo = outwardTaskOrderNo;
	}

	/**
	 * @return the channelName
	 */
	public String getChannelName() {
		return channelName;
	}

	/**
	 * @param channelName
	 *            the channelName to set
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
	 * @param memberId
	 *            the memberId to set
	 */
	public void setMemberId(String memberId) {
		this.memberId = memberId;
	}

	/**
	 * @return the handicapCode
	 */
	public String getHandicapCode() {
		return handicapCode;
	}

	/**
	 * @param handicapCode
	 *            the handicapCode to set
	 */
	public void setHandicapCode(String handicapCode) {
		this.handicapCode = handicapCode;
	}

	/**
	 * @return the daifuConfigId
	 */
	public Integer getDaifuConfigId() {
		return daifuConfigId;
	}

	/**
	 * @param daifuConfigId
	 *            the daifuConfigId to set
	 */
	public void setDaifuConfigId(Integer daifuConfigId) {
		this.daifuConfigId = daifuConfigId;
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

}
