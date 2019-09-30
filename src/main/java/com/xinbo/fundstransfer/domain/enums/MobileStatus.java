package com.xinbo.fundstransfer.domain.enums;

/**
 * Created by 000 on 2017/6/27.
 */
public enum MobileStatus {
    Normal(1, "启用"),
    Freeze(3, "冻结"),
    StopTemp(4, "停用");


    private Integer status = null;
    private String msg = null;

    MobileStatus(Integer status, String msg) {
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

    public static MobileStatus findByStatus(Integer status) {
        if (status == null) {
            return null;
        }
        for (MobileStatus mobileStatus : MobileStatus.values()) {
            if (status.equals(mobileStatus.status)) {
                return mobileStatus;
            }
        }
        return null;
    }
}
