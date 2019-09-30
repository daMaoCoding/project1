package com.xinbo.fundstransfer.domain.entity;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "biz_device_deal")
public class BizDeviceDeal implements java.io.Serializable{
    private String id;
    private String remark;
    private Date updateTime;

    @Id
    @Column(name = "mobile")
    public String getId() {
        return id;
    }

    public void setId(String mobile) {
        this.id = mobile;
    }

    @Column(name = "remark")
    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Column(name = "updateTime")
    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
