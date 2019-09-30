package com.xinbo.fundstransfer.domain.entity;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 描述:新公司用款 - 成功失败记录
 *
 * @author cobby
 * @create 2019年09月14日13:18
 */
@Entity
@Table(name = "biz_usemoneytake_request")
@Data
public class BizUseMoneyTakeEntity {

    /** ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 盘口 */
    @Column(name = "handicap")
    private Integer handicap;

    /** 第三方编号 */
    @Column(name = "third_code")
    private String thirdCode;

    /** 公司用款表编码 */
    @Column(name = "code")
    private String code;

    /** 状态，0：已确认，1：已取消，2：确认失败 */
    @Column(name = "status")
    private Integer status;

    /** 金额 */
    @Column(name = "amount")
    private BigDecimal amount;

    /** 备注 */
    @Column(name = "remark")
    private String remark;

    /** 收款银行类型，像中国银行，招商银行 */
    @Column(name = "toAccountBank")
    private String toAccountBank;

    /** 收款卡的帐号 */
    @Column(name = "to_account")
    private String toAccount;

    /** 收款卡的开户人 */
    @Column(name = "to_account_owner")
    private String toAccountOwner;

    /** 下发耗时开始时间 */
    @Column(name = "create_time")
    private Date createTime;

    /** 总耗时开始时间 */
    @Column(name = "create_time_total")
    private Date createTimeTotal;

    /** 下发耗时/总耗时的结束时间 */
    @Column(name = "update_time")
    private Date updateTime;

    /** 总耗时，单位秒 */
    @Column(name = "consuming_time")
    private Long consumingTime;

    /** 下发耗时，单位秒 */
    @Transient
    private Long timeConsuming;

    /** 第三方费率计算 */
    @Column(name = "fee")
    private BigDecimal fee;

    /** 第三方系统余额，二位小数 */
    @Column(name = "balance")
    private BigDecimal balance;

    /** 第三方后台余额 */
    @Column(name = "bank_balance")
    private BigDecimal bankBalance;

    /** 盘口名称 */
    @Transient
    private String handicapName;

    /** 盘口名称 */
    @Transient
    private String thirdName;

    /** 锁定人Name */
    @Column(name = "locker_name")
    private String lockerName;
}
