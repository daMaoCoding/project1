package com.xinbo.fundstransfer.chatPay.pthttpapi.reqVo;

import lombok.Data;

/**
 * ************************ 
 * 出入款调用平台，会员入款单确认
 * @author wira
 */
@Data
public class SureInOrderBack {
	/**
	 * token : 参数签名 
	 * oid : 盘口编码 
	 * code : 入款订单号 
	 * opt_time : 确认时间(毫秒时间戳)
	 */

	/**
	 * 参数签名
	 */
	private String token;

	/**
	 * 盘口编码
	 */
	private int oid;

	/**
	 * 入款订单号
	 */
	private String code;

	/**
	 * 确认时间(毫秒时间戳)
	 */
	private Long in_time;

}
