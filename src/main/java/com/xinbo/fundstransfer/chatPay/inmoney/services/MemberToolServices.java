package com.xinbo.fundstransfer.chatPay.inmoney.services;

import com.xinbo.fundstransfer.chatPay.inmoney.params.BillLogDto;

/**
 * 	支付宝和微信工具上报流水 然后进行匹配的服务
 * @author ERIC
 *
 */
public interface MemberToolServices {
	
	/**
	 * 接收工具上报流水-(支付宝流水)
	 * @param billLogDto
	 */
	void repotZfbBillsLog(BillLogDto billLogDto);
	
	/**
	 * 接收工具上报流水-(微信流水)
	 * @param billLogDto
	 */
	void repotWxBillsLog(BillLogDto billLogDto);
}
