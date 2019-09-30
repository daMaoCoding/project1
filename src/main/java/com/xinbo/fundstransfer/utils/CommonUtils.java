package com.xinbo.fundstransfer.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;

/**
 * @author Administrator
 */
@Slf4j
public class CommonUtils {

    /**
     * 算法:md5加密 - 出入款
     */
    public static String md5digest(String content) {
        try {
            // 将字符串返序列化成byte数组
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(content);
            oos.close();
            byte[] bToEnc = baos.toByteArray();
            // 将byte数组进行“消息摘要”计算，得到加密串(result)
            MessageDigest e = MessageDigest.getInstance("MD5");
            e.update(bToEnc);
            return new BigInteger(1, e.digest()).toString(16);
        } catch (Exception e) {
            return "" + e.getLocalizedMessage();
        }
    }



    /**
     * 算法:md5加密 - 通用
     */
    public static String getMD5UpperCase(String str)  {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");// 拿到一个MD5转换器（如果想要SHA1参数换成”SHA1”）
            messageDigest.update(str.getBytes());

            byte[] resultByteArray = messageDigest.digest();
            return byteArrayToHex(resultByteArray); // 字符数组转换成字符串返回

            // return new BigInteger(1, messageDigest.digest()).toString(16).toUpperCase();;
        } catch (Exception e) {
            log.error("[公用]-对字符串md5加密,并转成大写出错{}", e.getMessage());

        }
        return "";
    }

    /**
     * 将字节数组换成成16进制的字符串
     */
    public static String byteArrayToHex(byte[] byteArray) {
        // 初始化一个字符数组，用来存放每个16进制字符
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        // new一个字符数组，这个就是用来组成结果字符串的（解释一下：一个byte是八位二进制，也就是2位十六进制字符（2的8次方等于16的2次方））
        char[] resultCharArray = new char[byteArray.length * 2];
        // 遍历字节数组，通过位运算（位运算效率高），转换成字符放到字符数组中去
        int index = 0;
        for (byte b : byteArray) {
            resultCharArray[index++] = hexDigits[b >>> 4 & 0xf];
            resultCharArray[index++] = hexDigits[b & 0xf];
        }
        // 字符数组组合成字符串返回
        return new String(resultCharArray);
    }



}
