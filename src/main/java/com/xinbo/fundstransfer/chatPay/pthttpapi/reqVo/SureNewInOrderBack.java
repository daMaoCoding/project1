package com.xinbo.fundstransfer.chatPay.pthttpapi.reqVo;

import lombok.Data;

/**
 * ************************
 * 出入款调用平台，会员入款单部分确认(入款补提单) 
 * @author wira
 */
@Data
public class SureNewInOrderBack {

    /**
     * token : 参数签名
     * oid : 盘口编码
     * uid : 会员UID
     * user_name : 会员账号
     * in_money : 入款金额，必须大于0(单位元，例：1，1.2，1.21)
     * opt_time : 操作时间(毫秒时间戳)（新订单创建时间和确认时间）（如果有取消订单也表示取消订单时间）
     * in_type : 入款类型[0:微信，1:支付宝]
     * payee_qr_id : 收款人QrId （非必填）
     * payee_name : 收款人真实姓名
     * payee_account : 收款人账号
     * crk_order_id : 出入款入款补提单ID
     * crk_order_reason : 出入款补单原因 （非必填）
     * cancel_code : 要取消的入款订单编码
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
     * 会员UID
     */
    private Long uid;

    /**
     * 会员账号
     */
    private String user_name;

    /**
     * 入款金额，必须大于0(单位元，例：1，1.2，1.21)
     */
    private double in_money;


    /**
     * 操作时间(毫秒时间戳)（新订单创建时间和确认时间）（如果有取消订单也表示取消订单时间）
     */
    private Long opt_time;
    
    /**
     * 入款类型[0:微信，1:支付宝]
     */
    private int in_type;
    
    /**
     * 收款人QrId
     */
    private String payee_qr_id;
    
    /**
     * 收款人真实姓名
     */
    private String payee_name;
    
    /**
     * 收款人账号
     */
    private String payee_account;
    
    /**
     * 出入款入款补提单ID
     */
    private String crk_order_id;
    
    /**
     * 出入款补单原因
     */
    private String crk_order_reason;
    
    /**
     * 要取消的入款订单编码
     */
    private String cancel_code;



}
