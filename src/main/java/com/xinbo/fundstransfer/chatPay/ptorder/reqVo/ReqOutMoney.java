package com.xinbo.fundstransfer.chatPay.ptorder.reqVo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Objects;

import java.math.BigDecimal;

/**
 * ************************
 * 聊天室支付  平台请求出入款  会员出款单
 * @author tony
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReqOutMoney {

    /**
     * 盘口
     */
    private String   handicap;

    /**
     * 出款金额
     */
    private BigDecimal amount;


    /**
     * 出款订单号
     */
    private String   orderNo;


    /**
     * 订单生成时间戳
     */
    private Long orderCreateTime;


    /**
     * 会员UID
     */
    private String   uId;


    /**
     * 会员账号
     */
    private String  uName;


    /**
     * 会员真实姓名
     */
    private String   uRealName;


    /**
     * 会员层级(平台层级)
     */
    private String   uLeavel;


    /**
     * 会员出款时ip
     */
    private String    uIp;

    /**
     * 会员出款客户端，3 APP-Android，4 APP-IOS，5 APP-Other，6 WEB，7 Windows，8 Mac,9 WAP
     */
    private Integer uClient;


    /**
     * 出款类型(0微信出款，1支付宝出款)
     */
    private Integer channelType;


    /**
     * 出款会员收款二维码id
     */
    private String  qrId;


    public String getHandicap() {
        return handicap;
    }

    public void setHandicap(String handicap) {
        this.handicap = handicap;
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

    public String getQrId() {
        return qrId;
    }

    public void setQrId(String qrId) {
        this.qrId = qrId;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReqOutMoney that = (ReqOutMoney) o;
        return Objects.equal(handicap, that.handicap) &&
                Objects.equal(amount, that.amount) &&
                Objects.equal(orderNo, that.orderNo) &&
                Objects.equal(orderCreateTime, that.orderCreateTime) &&
                Objects.equal(uId, that.uId) &&
                Objects.equal(uName, that.uName) &&
                Objects.equal(uRealName, that.uRealName) &&
                Objects.equal(uLeavel, that.uLeavel) &&
                Objects.equal(uIp, that.uIp) &&
                Objects.equal(uClient, that.uClient) &&
                Objects.equal(channelType, that.channelType) &&
                Objects.equal(qrId, that.qrId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(handicap, amount, orderNo, orderCreateTime, uId, uName, uRealName, uLeavel, uIp, uClient, channelType, qrId);
    }


    @Override
    public String toString() {
        return "ReqOutward{" +
                "handicap='" + handicap + '\'' +
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
                ", qrId='" + qrId + '\'' +
                '}';
    }
}
