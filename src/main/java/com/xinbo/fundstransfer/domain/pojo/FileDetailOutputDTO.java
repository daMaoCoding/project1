package com.xinbo.fundstransfer.domain.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class FileDetailOutputDTO implements Serializable {

    /**
     * id : 1000000000000002   文件id
     * path :   文件路径
     * type : 1    0.图片 1.音频
     * times : 20  音频长度（秒）
     * beRead : 1  音频是否已播 0.否 1.是
     */

    private long id;
    private String path;
    private int type;
    private int times;
    private int beRead;
}
