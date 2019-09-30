package com.xinbo.fundstransfer.domain.enums;

/**
 * Created by Eden on 2017/6/27.
 */
public enum IncomeRequestStatus {

    /**
     * 匹配中
     */
    Matching(0, "匹配中"),

    /**
     * 已匹配
     */
    Matched(1, "已匹配"),


    /**
     * 无法匹配
     */
    Unmatching(2, "无法匹配"),

    /**
     * 已取消
     */
    Canceled(3, "已取消"),

    /**
     * 支转银取消
     */
    CANCELED4SUBINBANKALI(5, "支转银取消"),

    /**
     * 支转银匹配
     */
    MATCHED4SUBINBANKALI(4, "支转银匹配");



    private Integer status;
    private String msg;

    IncomeRequestStatus(Integer status, String msg) {
        this.status = status;
        this.msg = msg;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public static IncomeRequestStatus findByStatus(Integer status) {
        if (status == null) {
            return null;
        }
        for (IncomeRequestStatus reqStatus : IncomeRequestStatus.values()) {
            if (status.equals(reqStatus.getStatus())) {
                return reqStatus;
            }
        }
        return null;
    }
}
