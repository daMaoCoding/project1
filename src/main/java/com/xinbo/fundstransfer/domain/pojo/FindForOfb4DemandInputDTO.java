package com.xinbo.fundstransfer.domain.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class FindForOfb4DemandInputDTO implements Serializable {

    /**
     * ofbId : 1153405357063168
     * oid : 66
     */
    @NotNull
    private long ofbId;
    @NotNull
    private int oid;

}
