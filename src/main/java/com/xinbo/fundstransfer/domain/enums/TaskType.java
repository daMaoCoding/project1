package com.xinbo.fundstransfer.domain.enums;

public enum TaskType {
    MemberOutTask(0, "会员出款任务"), RebateTask(1, "返利任务");
    private Integer typeId = null;
    private String msg = null;

    TaskType(Integer typeId, String msg) {
        this.typeId = typeId;
        this.msg = msg;
    }

    public Integer getTypeId() {
        return typeId;
    }

    public void setTypeId(Integer typeId) {
        this.typeId = typeId;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public static TaskType findByTypeId(Integer typeId) {
        if (typeId == null) {
            return null;
        }
        for (TaskType type : TaskType.values()) {
            if (typeId.equals(type.typeId)) {
                return type;
            }
        }
        return null;
    }

}
