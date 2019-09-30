package com.xinbo.fundstransfer.domain.enums;

import java.util.Objects;

public enum OutWardPayType {
    PC(0,"PC"),
    ThirdInsteadPay(1, "三方代付"),
    REFUND(2, "返利网"),
    ThirdPay(3, "三方人工"),
    MANUAL(4,"人工");
    private Integer type;
    private String typeDesc;

    OutWardPayType(Integer type, String typeDesc) {
        this.type = type;
        this.typeDesc = typeDesc;
    }

    OutWardPayType(Integer type) {
        this.type = type;
    }

    public static OutWardPayType getType(Integer type) {
        if (Objects.isNull(type))
            return null;
        for (OutWardPayType payType : OutWardPayType.values()) {
            if (type.equals(payType.getType()))
                return payType;
        }
        return null;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getTypeDesc() {
        return typeDesc;
    }

    public void setTypeDesc(String typeDesc) {
        this.typeDesc = typeDesc;
    }
}
