package com.xinbo.fundstransfer.domain.pojo;

import lombok.Data;

import java.util.Date;


/**
 * 描述:新公司入款 - 用途类型管理
 *
 * @author cobby
 * @create 2019年08月24日10:15
 */
@Data
public class NewOutWardTypeManageRequest implements java.io.Serializable{

    /** ID */
    private Long   id;

    /** 状态 0- 正常 1- 已删除 */
    private Integer status;

    /** 用途类型名称 */
    private String useName;

    /** 创建人id */
    private Long createId;

    /** 创建人Name */
    private String createName;

    /** 创建时间 */
    private Date   createTime;

    /** 操作人id */
    private Long handelId;

    /** 操作人Name */
    private String handelName;

    /** 操作时间 */
    private Date   handelTime;

}
