package com.xinbo.fundstransfer.domain.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * 1.1.1   查询反馈列表
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class FindForOidOutputDTO implements Serializable {

    /**
     * id : 25
     * ownerName : 彩票999
     * title : // 公告标题
     * important : 0  // 反馈重要性类型 0.一般 1.重要 2.非常重要 3.紧急
     * importantDesc : 一般
     * typeName :  业务类型名
     * type : 1   // 反馈类型 0.普通 1.私密
     * typeDesc : 私密   type的描述
     * status : 0   0.未处理 1.处理中 2.处理中超时 3.处理完成 4.超时完成
     * statusDesc : 未处理   status描述
     * result : 0  0.未解决 1.已解决
     * resultDesc : 未解决    result描述
     * adminId : 1   创建人id
     * userName :    创建人
     * newReply : 1   是否有新的回复 0.否 1.是
     * newFb : 0   是否是新的反馈信息 0.否 1.是
     * isTimeout : 是   是否超时 0.否 1.是
     * outTime : 2017-05-12 15:56:26    超时时间
     * handelTime : 2017-05-12 15:56:26    最后操作时间
     * createTime : 2017-05-12 15:56:26     反馈时间
     */

    private Long id;
    private String ownerName;
    private String title;
    private Byte important;
    private String importantDesc;
    private String typeName;
    private Byte type;
    private String typeDesc;
    private Byte status;
    private String statusDesc;
    private Byte result;
    private String resultDesc;
    private Long adminId;
    private String userName;
    private Byte newReply;
    private Byte newFb;
    private String isTimeout;
    private String outTime;
    private String handelTime;
    private String createTime;

}
