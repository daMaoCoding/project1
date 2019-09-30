package com.xinbo.fundstransfer.domain.entity;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 描述:新公司出款 - entity
 *
 * @author cobby
 * @create 2019年08月30日15:28
 */
@Entity
@Table(name = "biz_usemoney_request")
@Data
public class BizUsemoneyRequestEntity {

    /** ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 编号 */
    @Column(name = "code")
    private String code;

//    /** 订单号 */
//    @Column(name = "order_no")
//    private Long orderNo;

    /** 第三方编码 */
    @Column(name = "third_code")
    private String thirdCode;

    /** 第三方名称 */
    @Transient
    private String thirdName;

    /** 用款人(新增公司用款创建人) */
    @Column(name = "member")
    private String  member;

    /** 用款人(新增公司用款创建人)- 编码 */
    @Column(name = "member_code")
    private Integer memberCode;

    /** 用途类型ID */
    @Column(name = "usetype")
    private Integer usetype;

    /** 用途类型名称 */
    @Transient
    private String useName;

    /** 盘口ID */
    @Column(name = "handicap")
    private Integer handicap;

    /** 盘口名称 */
    @Transient
    private String handicapName;

    /** 出款金额 */
    @Column(name = "amount")
    private BigDecimal amount;

    /** 收款方式 0-银行卡 1-第三方 */
    @Column(name = "receipt_type")
    private Integer receiptType;

    /** 0-待审核,1-财务审核通过,2-下发审核通过,3-财务审核不通过,4-下发审核不通过,5-等待到账 ,6-完成出款,7-确认对账 */
    @Column(name = "status")
    private Integer status;

    /** 收款银行类型，像中国银行，招商银行 */
    @Column(name = "to_account_bank")
    private String toAccountBank;

    /** 收款卡的帐号 */
    @Column(name = "to_account")
    private String toAccount;

    /** 收款卡的开户人 */
    @Column(name = "to_account_owner")
    private String toAccountOwner;

    /** 创建时间 */
    @Column(name = "create_time")
    private Date createTime;

    /** 财务审核人Name */
    @Column(name = "finance_reviewer_name")
    private String financeReviewerName;

    /** 财务审核时间 */
    @Column(name = "finance_reviewer_time")
    private Date financeReviewerTime;

    /** 下发审核人Name */
    @Column(name = "task_reviewer_name")
    private String taskReviewerName;

    /** '下发审核时间' */
    @Column(name = "task_reviewer_time")
    private Date taskReviewerTime;

    /** 审核信息 */
    @Column(name = "review")
    private String review;

    /** 审核耗时，单位秒 */
    @Column(name = "time_consuming")
    private Long timeConsuming;

    /** 最后更新时间 */
    @Column(name = "update_time")
    private Date updateTime;

    /** 备注 */
    @Column(name = "remark")
    private String remark;

    /** 锁定状态 */
    @Column(name = "lock_status")
    private Integer lockStatus;

    /** 锁定人Name */
    @Column(name = "lock_id")
    private String lockId;

    /** 锁定人Name */
    @Column(name = "lock_name")
    private String lockName;

    /** 锁定时间 */
    @Column(name = "lock_time")
    private Date lockTime;

    /** 第三方费率计算 */
    @Column(name = "fee")
    private BigDecimal fee;

    /** 第三方系统余额，二位小数 */
    @Transient
    private BigDecimal balance;

    /** 第三方后台余额 */
    @Transient
    private BigDecimal bankBalance;

    /** 入款审核表ID */
    @Column(name = "biz_income_id")
    private Long bizIncomeId;

    /** 下发到账时间 */
    @Column(name = "sent_time")
    private Date sentTime;

    /** 下发耗时 */
    @Transient
    private Long sentConsumingTime;

    /** 总耗时 */
    @Transient
    private Long consumingTime;

    /** 每次点击提现时间 */
    @Column(name = "cash_time")
    private Date cashTime;

}
