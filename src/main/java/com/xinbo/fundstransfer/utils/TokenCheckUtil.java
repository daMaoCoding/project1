package com.xinbo.fundstransfer.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.stream.Stream;

/**
 * ************************
 *
 * @author tony
 */
@Slf4j
public class TokenCheckUtil {

    /**
     * 验证请求token
     */
    public static boolean reqTokenCheck(String formater, String token, Object... args) {
        return reqTokenCheckBase(token, formater, args);
    }

    /**
     * 验证请求token
     */
    public static boolean reqTokenCheck( String token, Object... args) {
        final String  join[] = {""};
        Stream.of(args).forEach(x->join[0]+="%s");
        String formater = join[0];
        return reqTokenCheckBase(token, formater, args);
    }

    private static boolean reqTokenCheckBase(String token, String formater, Object[] args) {
        if (StringUtils.isNotBlank(formater) && StringUtils.isNotBlank(token) && null != args && args.length > 0) {
            String oriContent = String.format(formater, args);
            String rightToken = CommonUtils.md5digest(oriContent);
            log.info("计算正确-token:{}", rightToken);
            return StringUtils.equalsIgnoreCase(rightToken, token);
        }
        return false;
    }



    /**
     * 生成请求token
     */
    public static String reqTokenGenerate(String formater,Object... args){
        String rightToken = reqTokenGenerateBase(formater, args);
        if (rightToken != null) return rightToken;
        return "reqTokenGenerate-Error";
    }


    /**
     * 生成请求token
     */
    public static String reqTokenGenerate(Object... args){
        final String  join[] = {""};
        Stream.of(args).forEach(x->join[0]+="%s");
        String formater = join[0];

        String rightToken = reqTokenGenerateBase(formater, args);
        if (rightToken != null) return rightToken;
        return "reqTokenGenerate-Error";
    }

    private static String reqTokenGenerateBase(String formater, Object[] args) {
        if (StringUtils.isNotBlank(formater) && null != args && args.length > 0) {
            String oriContent = String.format(formater, args);
            String rightToken = CommonUtils.md5digest(oriContent);
            log.info("计算正确-token:{}", rightToken);
            return rightToken;
        }
        return null;
    }

}
