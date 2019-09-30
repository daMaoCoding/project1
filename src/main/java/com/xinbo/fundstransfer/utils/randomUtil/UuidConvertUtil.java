package com.xinbo.fundstransfer.utils.randomUtil;

import java.util.UUID;

/**
 * UUID方式
 */
public class UuidConvertUtil implements ConvertUtil {
    private final static ConvertUtil CONVERT_UTIL = new UuidConvertUtil();

    private UuidConvertUtil() {
    }

    public static ConvertUtil getInstance() {
        return CONVERT_UTIL;
    }

    @Override
    public String convert(String str) {
        String uuid = UUID.randomUUID().toString();
        return uuid.substring(0, 8) + uuid.substring(9, 13)
                + uuid.substring(14, 18) + uuid.substring(19, 23)
                + uuid.substring(24);
    }
}
