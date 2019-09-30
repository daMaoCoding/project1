package com.xinbo.fundstransfer.chatPay.ptorder.reqVo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Objects;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * ************************
 * 聊天室支付  平台请求出入款  会员入款单
 * @author tony
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReqInMoney {

    /**
     * 平台的 盘口，oid
     */
    @NotBlank(message = "盘口不能为空")
    private String  handicap;


    /**
     * 匹配超时时间，毫秒。(包括请求聊天室获取url时间)
     */
    @NotNull(message = "超时时间不能空")
    private Long timeout;


    /**
     * 入款金额
     */
    @NotNull(message = "金额不能空")
    private BigDecimal  amount;


    /**
     * 入款订单号
     */
    @NotBlank(message = "订单号不能空")
    private String      orderNo;

    /**
     * 订单生成时间戳
     */
    @NotNull(message = "订单生成时间不能空")
    private Long        orderCreateTime;

    /**
     * 会员UID
     */
    @NotBlank(message = "会员id不能空")
    private String      uId;

    /**
     * 会员账号
     */
    @NotBlank(message = "会员账号不能空")
    private String      uName;


    /**
     * 会员真实姓名
     */
    @NotBlank(message = "会员真实姓名不能空")
    private String      uRealName;


    /**
     * 会员层级(平台层级)
     */
    @NotBlank(message = "会员层级不能空")
    private String      uLeavel;

    /**
     * 会员入款时ip
     */
    @NotBlank(message = "会员入款ip不能空")
    private String      uIp;

    /**
     * 会员入款客户端，3 APP-Android，4 APP-IOS，5 APP-Other，6 WEB，7 Windows，8 Mac,9 WAP
     */
    @NotNull(message = "会员客户端不能空")
    private Integer     uClient;

    /**
     * 入款类型(0微信入款，1支付宝入款)
     */
    @NotNull(message = "入款类型不能空")
    @Min(value = 0,message = "入款类型错误")
    @Max(value = 1,message = "入款类型错误")
    private Integer     channelType;




    public String getHandicap() {
        return handicap;
    }

    public void setHandicap(String handicap) {
        this.handicap = handicap;
    }

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getOrderCreateTime() {
        return orderCreateTime;
    }

    public void setOrderCreateTime(Long orderCreateTime) {
        this.orderCreateTime = orderCreateTime;
    }

    public String getuId() {
        return uId;
    }

    public void setuId(String uId) {
        this.uId = uId;
    }

    public String getuName() {
        return uName;
    }

    public void setuName(String uName) {
        this.uName = uName;
    }

    public String getuRealName() {
        return uRealName;
    }

    public void setuRealName(String uRealName) {
        this.uRealName = uRealName;
    }

    public String getuLeavel() {
        return uLeavel;
    }

    public void setuLeavel(String uLeavel) {
        this.uLeavel = uLeavel;
    }

    public String getuIp() {
        return uIp;
    }

    public void setuIp(String uIp) {
        this.uIp = uIp;
    }

    public Integer getuClient() {
        return uClient;
    }

    public void setuClient(Integer uClient) {
        this.uClient = uClient;
    }

    public Integer getChannelType() {
        return channelType;
    }

    public void setChannelType(Integer channelType) {
        this.channelType = channelType;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReqInMoney reqIncome = (ReqInMoney) o;
        return Objects.equal(handicap, reqIncome.handicap) &&
                Objects.equal(timeout, reqIncome.timeout) &&
                Objects.equal(amount, reqIncome.amount) &&
                Objects.equal(orderNo, reqIncome.orderNo) &&
                Objects.equal(orderCreateTime, reqIncome.orderCreateTime) &&
                Objects.equal(uId, reqIncome.uId) &&
                Objects.equal(uName, reqIncome.uName) &&
                Objects.equal(uRealName, reqIncome.uRealName) &&
                Objects.equal(uLeavel, reqIncome.uLeavel) &&
                Objects.equal(uIp, reqIncome.uIp) &&
                Objects.equal(uClient, reqIncome.uClient) &&
                Objects.equal(channelType, reqIncome.channelType);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(handicap, timeout, amount, orderNo, orderCreateTime, uId, uName, uRealName, uLeavel, uIp, uClient, channelType);
    }

    @Override
    public String toString() {
        return "ReqIncome{" +
                "handicap='" + handicap + '\'' +
                ", timeout=" + timeout +
                ", amount=" + amount +
                ", orderNo='" + orderNo + '\'' +
                ", orderCreateTime=" + orderCreateTime +
                ", uId='" + uId + '\'' +
                ", uName='" + uName + '\'' +
                ", uRealName='" + uRealName + '\'' +
                ", uLeavel='" + uLeavel + '\'' +
                ", uIp='" + uIp + '\'' +
                ", uClient=" + uClient +
                ", channelType=" + channelType +
                '}';
    }
}
