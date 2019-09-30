package com.xinbo.fundstransfer.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class ObjectMapperUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ObjectMapperUtils.class);
    private static final ObjectMapper objectMapper;
    /* 属性值为null的，不参与序列化 */
    private static final ObjectMapper objectMapperNonNull;

    static {
        objectMapper = new ObjectMapper();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        objectMapper.setDateFormat(dateFormat);
        // 该特性决定了当遇到未知属性（没有映射到属性，没有任何setter或者任何可以处理它的handler），是否应该抛出一个
        // JsonMappingException异常。这个特性一般式所有其他处理方法对未知属性处理都无效后才被尝试，属性保留未处理状态。
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        objectMapperNonNull = new ObjectMapper();
        // 属性值为null的，不参与序列化
        objectMapperNonNull.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    /**
     * 将java类型的对象转换为JSON格式的字符串
     * @param object
     * @param <T>
     * @return
     * @author tour
     */
    public static <T> String serialize(T object) {
        if(object == null){
            return null;
        }
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 将java类型的对象转换为JSON格式的字符串
     * @param object
     * @param <T>
     * @return
     * @author tour
     */
    public static <T> String serializeNonNull(T object) {
        if(object == null){
            return null;
        }
        try {
            return objectMapperNonNull.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 将JSON格式的字符串转换为复杂对象
     * @param json
     * @param clazz
     * @param <T>
     * @return
     * @author tour
     */
    public static <T> T deserialize(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 将JSON格式的字符串转换为复杂对象
     * @param json
     * @param clazz
     * @param <T>
     * @return
     * @author tour
     * @throws IOException
     */
    public static <T> T deserializeThrowable(String json, Class<T> clazz) throws IOException {

        return objectMapper.readValue(json, clazz);
    }

    /**
     * 将JSON格式的字符串转换为复杂对象
     * @param json
     * @param typeReference
     * @param <T>
     * @return
     * @author tour
     */
    public static <T> T deserialize(String json, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 将JSON格式的字符串转换为复杂对象
     * @param json
     * @param typeReference
     * @param <T>
     * @return
     * @author tour
     */
    public static <T> T deserializeNonNull(String json, TypeReference<T> typeReference) {
        try {
            return objectMapperNonNull.readValue(json, typeReference);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 将一个对象序列化成byte[]
     * @param value
     * @param <T>
     * @return
     * @author tour
     */
    public static <T> byte[] serializeBytes(T value) {
        ObjectOutputStream oos = null;
        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(value);
            return baos.toByteArray();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        } finally {
            try {
                if (baos != null) {
                    baos.close();
                }
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }
            try {
                if (oos != null) {
                    oos.close();
                }
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * 将一个byte[]序列化成对象
     * @param bytes
     * @return
     * @author tour
     */
    public static Object deserializeBytes(byte[] bytes) {
        if(ArrayUtils.isEmpty(bytes)){
            return null;
        }
        ByteArrayInputStream bais = null;
        ObjectInputStream ois = null;
        try {
            bais = new ByteArrayInputStream(bytes);
            ois = new ObjectInputStream(bais);
            return ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            LOG.error(e.getMessage(), e);
        } finally {
            try {
                if (ois != null) {
                    ois.close();
                }
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }
            try {
                if (bais != null) {
                    bais.close();
                }
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return null;
    }
}
