package com.xinbo.fundstransfer.utils.randomUtil;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * md5生成
 */
public class Md5ConvertUtil implements ConvertUtil {
    private final static ConvertUtil CONVERT_UTIL = new Md5ConvertUtil();

    private Md5ConvertUtil() {
    }

    public static ConvertUtil getInstance() {
        return CONVERT_UTIL;
    }

    @Override
    public String convert(String str) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(str.getBytes());
            String hex;
            StringBuilder md5Str = new StringBuilder();
            for (byte aByte : bytes) {
                hex = Integer.toHexString(aByte & 0xff);
                if (hex.length() < 2) {
                    md5Str.append('0');
                }
                md5Str.append(hex);
            }
            return md5Str.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
}
