package com.xinbo.fundstransfer.domain.entity;

import com.fasterxml.jackson.annotation.JsonInclude;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

/**
 * Created by Administrator on 2018/7/3.
 */
@Entity
@Table(name = "biz_task_review")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BizTaskReviewEntity implements Serializable {
    private int id;
    private String operator;
    private String handicap;
    private Date asignTime;
    private Date finishTime;
    private String remark;
    private Integer taskid;


    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    @Column(name = "operator")
    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }


    @Column(name = "handicap")
    public String getHandicap() {
        return handicap;
    }

    public void setHandicap(String handicap) {
        this.handicap = handicap;
    }


    @Column(name = "asign_time")
    public Date getAsignTime() {
        return asignTime;
    }

    public void setAsignTime(Date asignTime) {
        this.asignTime = asignTime;
    }


    @Column(name = "finish_time")
    public Date getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Date finishTime) {
        this.finishTime = finishTime;
    }


    @Column(name = "remark")
    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BizTaskReviewEntity that = (BizTaskReviewEntity) o;

        if (id != that.id) return false;
        if (operator != null ? !operator.equals(that.operator) : that.operator != null) return false;
        if (handicap != null ? !handicap.equals(that.handicap) : that.handicap != null) return false;
        if (asignTime != null ? !asignTime.equals(that.asignTime) : that.asignTime != null) return false;
        if (finishTime != null ? !finishTime.equals(that.finishTime) : that.finishTime != null) return false;
        if (remark != null ? !remark.equals(that.remark) : that.remark != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (operator != null ? operator.hashCode() : 0);
        result = 31 * result + (handicap != null ? handicap.hashCode() : 0);
        result = 31 * result + (asignTime != null ? asignTime.hashCode() : 0);
        result = 31 * result + (finishTime != null ? finishTime.hashCode() : 0);
        result = 31 * result + (remark != null ? remark.hashCode() : 0);
        return result;
    }

    @Column(name = "taskid")
    public Integer getTaskid() {
        return taskid;
    }

    public void setTaskid(Integer taskid) {
        this.taskid = taskid;
    }
}
