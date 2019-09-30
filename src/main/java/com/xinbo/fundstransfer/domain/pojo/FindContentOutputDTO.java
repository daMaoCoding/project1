package com.xinbo.fundstransfer.domain.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 1.1.2   业主反馈 查看反馈内容
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class FindContentOutputDTO implements Serializable {

    /**
     * id : 1000000000000001
     * userName :
     * title :
     * content :
     * demandType : 1  需求审核值 0.审核不通过 1.审核通过（为null，表示反馈不是需求类型反馈）
     * businessReason :  需求类需求的业务原因
     * fileCol : []
     */

    private long id;
    private String userName;
    private String title;
    private String content;
    private int demandType;
    private String businessReason;
    private List<FileDetailOutputDTO> fileCol;
}
