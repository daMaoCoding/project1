package com.xinbo.fundstransfer.domain.entity;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * 描述:新公司出款 - 用途类型 - Entity
 *
 * @author cobby
 * @create 2019年08月24日11:12
 */
@Entity
@Table(name = "biz_outusemanage_request")
@Data
public class NewOutWardEntity implements Serializable {

    /** ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long    id;

    /** 状态 0- 正常 1- 已删除 */
    @Column(name = "status")
    private Integer status;

    /** 用途类型名称 */
    @Column(name = "use_name")
    private String  useName;


    /** 创建人id */
    @Column(name = "create_id")
    private Long    createId;


    /** 创建人Name */
    @Column(name = "create_name")
    private String  createName;


    /** 创建时间 */
    @Column(name = "create_time")
    private Date    createTime;


    /** 操作人id */
    @Column(name = "handel_id")
    private Long    handelId;


    /** 操作人Name */
    @Column(name = "handel_name")
    private String  handelName;


    /** 操作时间 */
    @Column(name = "handel_time")
    private Date    handelTime;


}
