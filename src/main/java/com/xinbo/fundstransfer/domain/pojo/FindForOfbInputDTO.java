package com.xinbo.fundstransfer.domain.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class FindForOfbInputDTO implements Serializable {

    /**
     * ofbId : 1  （必传）反馈id
     * createTimeStart : 2017-10-16 11:11:11  日期检索开始值
     * createTimeEnd : 2018-10-16 11:11:11   日期检索结束值
     */
    @NotNull
    private Long ofbId;
    private String createTimeStart;
    private String createTimeEnd;
}
