package com.xinbo.fundstransfer.utils.randomUtil;

import java.util.Arrays;

public interface ConvertUtil {
    /**
     * 可用字符集
     */
    char[] CHARS = new char[]{
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
            , 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h',
            'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
            'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
            'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
            'U', 'V', 'W', 'X', 'Y', 'Z'
    };

    /**
     * 转换为32位字符串
     */
    String convert(String str);

    /**
     * 字符串缩短
     */
    default String[] shortString(String str, RandomUtil.RandomType  type, int length) {
        // 限制长度范围
        if (length < 4) {
            length = 4;
        } else if (length > 16) {
            length = 16;
        }
        // 计算位移步长
        int step = 32 / length;

        // 判断字符集
        char[] code;
        switch (type) {
            case NUMBER:
                // 纯数字
                code = Arrays.copyOfRange(CHARS, 0, 10);
                break;
            case LOWERCASE:
                // 纯小写字母
                code = Arrays.copyOfRange(CHARS, 10, 36);
                break;
            case UPPERCASE:
                // 大写字母
                code = Arrays.copyOfRange(CHARS, 36, 62);
                break;
            case LOWERCASEANDUPPERCASE:
                // 大小写
                code = Arrays.copyOfRange(CHARS, 10, 62);
                break;
            default:
                // 默认字母数字大小写
                code = CHARS;
        }
        // 数组下标范围
        long subHex = code.length - 1;

        // 加密32位摘要
        String md5Str = convert(str);
        String[] resStr = new String[4];
        // 摘要划分4段做备用筛选
        for (int i = 0; i < 4; i++) {
            String subStr = md5Str.substring(i * 8, i * 8 + 8);
            long hexIndex = Long.parseLong(subStr, 16);
            StringBuilder outChars = new StringBuilder();
            for (int j = 0; j < length; j++) {
                long index = subHex & hexIndex;
                outChars.append(code[(int) index]);
                hexIndex = hexIndex >> step;
            }
            resStr[i] = outChars.toString();
        }
        return resStr;
    }
}
