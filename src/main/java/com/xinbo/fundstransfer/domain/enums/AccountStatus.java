package com.xinbo.fundstransfer.domain.enums;

/**
 * Created by 000 on 2017/6/27.
 */
public enum AccountStatus {
    Normal(1, "在用"),//正常,在用
    Freeze(3, "冻结"),
    StopTemp(4, "停用"),
    Enabled(5,"可用"),//可用，新卡(这里的新卡针对入款账号而言，新卡启用状态变为正常,新卡转启用后不能恢复成新卡，平台需把状态同步过来)
    Inactivated(6,"未激活"),//未激活，账号新建时，默认类型为未激活，需要做转账测试并匹配成功才可以完成激活
    Activated(7,"已激活"),//未激活，账号新建时，默认类型为未激活，需要做转账测试并匹配成功才可以完成激活
    Excep(-1, "异常"),
    Delete(-2, "删除");


    private Integer status = null;
    private String msg = null;

    AccountStatus(Integer status, String msg) {
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

    public static AccountStatus findByStatus(Integer status) {
        if (status == null) {
            return null;
        }
        for (AccountStatus accountStatus : AccountStatus.values()) {
            if (status.equals(accountStatus.status)) {
                return accountStatus;
            }
        }
        return null;
    }
}
