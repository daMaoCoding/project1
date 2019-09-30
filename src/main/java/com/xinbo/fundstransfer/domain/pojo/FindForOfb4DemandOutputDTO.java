package com.xinbo.fundstransfer.domain.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class FindForOfb4DemandOutputDTO implements Serializable {

    /**
     * audit : 1
     * status : 0
     * noCancel : 1
     * noPause : 1
     * auditTime : 2018-07-23 14:27:29
     * planTime : 2018-07-28 23:59:59
     * actualTime : null
     * createTime : 2018-07-23 14:27:04
     * audit2Cause : null
     * srCol : []
     * codeStr : 192
     */

    private int audit;
    private int status;
    private int noCancel;
    private int noPause;
    private String auditTime;
    private String planTime;
    private Object actualTime;
    private String createTime;
    private Object audit2Cause;
    private String codeStr;
    private List<SRColDetail> srCol;


    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public class SRColDetail implements Serializable {

        /**
         * content : 产品需求
         * handelTime : 2018-07-23 14:27:29
         */

        private String content;
        private String handelTime;
    }
}