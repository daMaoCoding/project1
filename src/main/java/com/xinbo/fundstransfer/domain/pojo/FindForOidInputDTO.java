package com.xinbo.fundstransfer.domain.pojo;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.xinbo.fundstransfer.ali4enterprise.inputdto.PageInputDTO;
import lombok.Data;

/**
 * 1.1.1   查询反馈列表
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class FindForOidInputDTO extends PageInputDTO {

    /**
     * crk : 1  如果传1  则只查询出入款四种反馈类型
     * isMain : 1   我的反馈 0.否  1.是
     * result : 0   0.未解决 1.已解决（为空或不传，表示全部）
     * status: 0,   0.未处理 1.处理中 2.处理中超时 3.处理完成 4.超时完成（为空或不
     * type : 0   0.未处理 1.处理中 2.处理中超时 3.处理完成 4.超时完成（为空或不传，表示全部状态）
     * type2 : 0    0.普通 1.私密（不传或为空，表示全部类型）
     * newReply2 : 1   0.问题 1.需求（不传或为空，表示全部类型）
     * typeId : 10000000     业务类型id（为空或不传，表示全部）
     * important : 0    反馈重要性类型 0.一般 1.重要 2.非常重要 3.紧急（为空或不传，表示全部）
     * userName :   创建人
     * createTimeStart : 2017/05/12 11:11:11    反馈时间开始
     * createTimeEnd : 2018/05/12 11:11:11      反馈时间结束
     * handelTimeStart : 2017/05/12 11:11:11    最后操作时间开始
     * handelTimeEnd : 2018/05/12 11:11:11    最后操作时间结束
     */

    private Byte crk;
    private Byte isMain;
    private Byte result;
    private Byte status;
    private Byte type;
    private Byte type2;
    private Byte newReply2;
    private Byte typeId;
    private Byte important;
    private String userName;
    private String createTimeStart;
    private String createTimeEnd;
    private String handelTimeStart;
    private String handelTimeEnd;

}
