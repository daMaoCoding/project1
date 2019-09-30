package com.xinbo.fundstransfer.domain.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class FindForOfbOutputDTO implements Serializable {

    /**
     * id : 1
     * content :
     * handelName :
     * type : 0
     * createTime : 2017-10-16 11:11:11
     * status : 0
     * statusDesc : 未处理
     * fileCol : []
     */

    private int id;
    private String content;
    private String handelName;
    private int type;
    private String createTime;
    private int status;
    private String statusDesc;
    private List<FileDetailOutputDTO> fileCol;
}
