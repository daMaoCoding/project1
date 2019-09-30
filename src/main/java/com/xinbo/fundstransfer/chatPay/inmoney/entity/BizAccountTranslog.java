package com.xinbo.fundstransfer.chatPay.inmoney.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;


import lombok.Data;

/**
 * 	兼职交易记录 实体
 * @author ERIC
 *
 */
@Entity
@Table(name = "biz_account_translog")
@Data
public class BizAccountTranslog  implements Serializable  {
	private static final long serialVersionUID = 1894503230784821949L;

	/**
	   * 主键
	   */
	  @Id
	  @GeneratedValue(strategy = GenerationType.IDENTITY)
	  @Column(name = "id", insertable = false, nullable = false)
	  private Long id;

	  /**
	   * 	流水编号(自动生成有意义)
	   */
	  @Column(name = "code", nullable = false)
	  private String code;
	  
	  /**
	   * 	兼职  支付宝账号ID/微信账号表id
	   */
	  @Column(name = "account_id", nullable = false)
	  private Long accountId;
	  
	  /**
	   * 	类型，1 代收  2代付
	   */
	  @Column(name = "inout_type", nullable = false)
	  private Integer inoutType;		  
	  
	  /**
	   * 	金额
	   */
	  @Column(name = "money", nullable = false)
	  private BigDecimal money;		
	  
	  /**
	   * 	余额
	   */
	  @Column(name = "balance", nullable = false)
	  private BigDecimal balance;		
	  
	  /**
	   * 	创建时间
	   */
	  @Column(name = "create_time", nullable = false)
	  private Date createtime;	  
	  
	  /**
	   * 	备注
	   */
	  @Column(name = "remark", nullable = false)
	  private String remark;	  	 
	  
	  /**
	   * 	会员账号
	   */
	  @Column(name = "member", nullable = false)
	  private String member;	
	  
	  /**
	   * 	会员支付宝入款单号
	   */
	  @Column(name = "income_request_id", nullable = false)
	  private Long incomeRequestId;
	  
	  /**
	   * 	会员支付宝出款单号
	   */
	  @Column(name = "outward_request_id", nullable = false)
	  private Long outwardRequestId;	
	  
	  /**
	   * 	房间号
	   */
	  @Column(name = "room_num", nullable = false)
	  private String roomNum;	  	 
	  
	  /**
	   * 	加入房间Token
	   */
	  @Column(name = "room_token", nullable = false)
	  private String roomToken;		 

	  /**
	   * 	客服系统业务ID
	   */
	  @Column(name = "biz_id", nullable = false)
	  private String bizId;		
	  
	  /**
	   * 	状态: 0-未完成；1-完成
	   */
	  @Column(name = "status", nullable = false)
	  private Integer status;


	  /**
	   * 	兼职uid
	   */
	  @Column(name = "rebate_uid", nullable = false)
	  private String rebateUid;

	  /**
	   * 订单备注码
	   */
	  @Column(name = "chat_pay_bzm", nullable = false)
	  private String chatPayBzm;
	  
	  /**
	   * 交易确认完成时间（成功/失败）
	   */
	  @Column(name = "finsh_tran_time", nullable = false)
	  private Date finshTranTime;
	  
	  /**
	   * 第三方账单中记录的时间
	   */
	  @Column(name = "bill_tran_time", nullable = false)
	  private Date billTranTime;	  
	 
}
