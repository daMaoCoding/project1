package com.xinbo.fundstransfer.utils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.Maps;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SimpleResponseData implements Serializable {

    public static final int SUCCESS = 1;
    public static final int ERROR = 0;


    private int status;
    private String message;
    private Object data;


    public SimpleResponseData(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public SimpleResponseData(int status, String message, Object data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }



    /**
     * 使用【code】+【MSG】,产生错误响应
     */
    public static SimpleResponseData error(int status, String message){
        return new SimpleResponseData(status,message);
    }


    /**
     * 使用【MSG】+【data】,产生错误响应
     */
    public static SimpleResponseData error(String message, Object t){
        return new SimpleResponseData(ERROR,message,t);
    }

    /**
     * 使用【MSG】,产生错误响应
     */
    public static SimpleResponseData error(String message){
        return new SimpleResponseData(ERROR,message);
    }


    /**
     * 使用【MSG】+【data】,产生正确响应
     */
    public static SimpleResponseData success(String message, Object t){
        return new SimpleResponseData(SUCCESS,message,t);
    }

    /**
     * 成功-Map响应(需要添加字段再加入此map)
     */
    public static Map<String,Object> getSuccessMapResult(){
        HashMap<String, Object> map = Maps.newHashMap();
        map.put("status",SUCCESS);
        map.put("message","OK");
        return map;
    }


    /**
     * 失败-Map响应(需要添加字段再加入此map)
     */
    public static Map<String,Object> getErrorMapResult(String errMsg){
        HashMap<String, Object> map = Maps.newHashMap();
        map.put("status",ERROR);
        map.put("message",errMsg);
        return map;
    }


}
