package com.xinbo.fundstransfer.chatPay.pthttpapi.reqVo;

import java.util.List;

import lombok.Data;

/**
 * ************************
 * 会员出款单确认或取消 
 * @author wira
 */
@Data
public class SureOrCancelOutOrderBack {

    /**
     * token : 参数签名
     * oid : 盘口编码
     * type : 操作类型[0：取消出款，1：确认出款]
     * code : 出款订单号
     * opt_time : 确认或取消时间(毫秒时间戳)
     * detailList : 明细订单数据（非必填）
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
     * 操作类型[0：取消出款，1：确认出款]
     */
    private byte type;

    /**
     * 出款订单号
     */
    private String code;

    /**
     * 确认或取消时间(毫秒时间戳)
     */
    private Long opt_time;
    
    /**
     * 明细订单数据
     */
    private List<OrderDetail> detailList;


}
