package com.xinbo.fundstransfer.chatPay.inmoney.entity;

import java.io.Serializable;
import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.xinbo.fundstransfer.domain.entity.BizIncomeRequest;
import com.xinbo.fundstransfer.domain.entity.BizOutwardRequest;

import lombok.Data;

/**
 * 	聊天支付记录（出款和入款的配对）实体
 * @author ERIC
 *
 */
@Entity
@Table(name = "biz_chatpay_log")
@Data
public class BizChatpayLog  implements Serializable  {
	private static final long serialVersionUID = 1894503230784821949L;

	/**
	   * 主键
	   */
	  @Id
	  @GeneratedValue(strategy = GenerationType.IDENTITY)
	  @Column(name = "id", insertable = false, nullable = false)
	  private Long id;

	  /**
	   * 	房间号
	   */
	  @Column(name = "room_num", nullable = false)
	  private String room_num;
	  
	  /**
	   * 	入款类型，1.会员入款  2.兼职代付
	   */
	  @Column(name = "income_type", nullable = false)
	  private Integer incomeType;
	  
	  /**
	   * 	入款单号， 会员入款是入款单号；兼职代付是兼职交易记录的代付ID
	   */
	  @Column(name = "income_order_no", nullable = false)
	  private String incomeOrderNo;		  
	  
	  /**
	   * 	出款类型，1会员出款   2.兼职代收
	   */
	  @Column(name = "outward_type", nullable = false)
	  private Integer outwardType;		
	  
	  /**
	   * 	出款单号，会员出款是出款单号；兼职代收是兼职交易记录的代收ID
	   */
	  @Column(name = "outward_order_no", nullable = false)
	  private String outwardOrderNo;		
	  
	  /**
	   * 	交易状态：0.进行中,1成功，2失败
	   */
	  @Column(name = "status", nullable = false)
	  private Integer status;	  
	  
	  /**
	   * 	确认方式： 流水确认流水id，   人工确认（出款方确认||客服确认）用户名
	   */
	  @Column(name = "status_reason", nullable = false)
	  private String statusReason;	  	 
	  
	  /**
	   * 	金额
	   */
	  @Column(name = "amount", nullable = false)
	  private BigDecimal amount;	 
	  
	  /**
	   * 	备注信息
	   */
	  @Column(name = "remark", nullable = false)
	  private String remark;	
	  
	  /**
	   * 入款单
	   */
	  @Transient
	  private BizIncomeRequest incomeRequest;
	  
	  /**
	   * 出款单
	   */
	  @Transient
	  private BizOutwardRequest outwardRequest;
	  
	  /**
	   * 兼职交易单
	   */
	  private BizAccountTranslog translog;
	  
	  public void appendRemark(String remark) {
		  if(this.remark == null) {
			  this.remark = remark;
		  } else {
			  this.remark = this.remark + "\n\r" + remark;
		  }
	  }
	  
	  /**
	   * 入款人员是否会员
	   * @return
	   */
	  public boolean incomeIsMember() {
		  if(incomeType != null && incomeType.intValue() == 1) {
			  return true;
		  }
		  return false;
	  }
	  
	  public boolean outwardIsMember() {
		  if(outwardType != null && outwardType.intValue() == 1) {
			  return true;
		  }
		  return false;
	  }
}
