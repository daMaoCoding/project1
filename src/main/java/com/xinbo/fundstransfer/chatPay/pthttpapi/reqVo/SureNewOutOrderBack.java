package com.xinbo.fundstransfer.chatPay.pthttpapi.reqVo;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

/**
 * ************************
 * 出入款调用平台，会员出款部分确认(出款补单) 
 * @author wira
 */
@Data
public class SureNewOutOrderBack {

	 /**
     * token : 参数签名
     * oid : 盘口编码
     * type : 操作类型[0：取消出款，1：确认出款]
     * money : 新出款金额(补单金额)
     * uid : 会员UID
     * user_name : 会员账号
     * admin_name : 操作人账号(补单人(系统自动或者操作人用户名))
     * crk_order_id : 出入款出款补提单id
     * crk_order_reason : 出入款出款补提单原因 （非必填）
     * opt_time : 确认时间(毫秒时间戳)(新单的补单时间，原单的取消时间)
     * cancel_code : 原出款单订单编号(收款人信息可以从原单号获取)
     * detailList : 明细订单数据 （非必填）
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
     * 新出款金额(补单金额)
     */
    private BigDecimal money;

    /**
     * 会员UID
     */
    private Long uid;
    
    /**
     * 会员账号
     */
    private String user_name;

    /**
     * 操作人账号(补单人(系统自动或者操作人用户名))
     */
    private String admin_name;

    /**
     * 出入款出款补提单id
     */
    private String crk_order_id;
    
    /**
     * 出入款出款补提单原因 （非必填）
     */
    private String crk_order_reason;

    /**
     * 确认时间(毫秒时间戳)(新单的补单时间，原单的取消时间)
     */
    private Long opt_time;

    /**
     * 原出款单订单编号(收款人信息可以从原单号获取)
     */
    private String cancel_code;
    
    /**
     * 明细订单数据
     */
    private List<OrderDetail> detailList;


}
