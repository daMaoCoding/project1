package com.xinbo.fundstransfer.utils.randomUtil;


import com.xinbo.fundstransfer.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * ************************
 *
 * @author tony
 */
@Slf4j
@Component
public class RandomUtil {

    @Autowired    RedisService redisService;

    /** 二维码id拼接连接字符串 */
    public static String qr_sp = "U";


    /** 二维码id格式 P/F+随机数+U+uid */
    private static String qridFormater = "%s%s"+qr_sp+"%s";

    /** 收款备注码缓存Key  */
    private static String Remark_Num_Key="Remark_Num_Key";



    /**
     * 平台二维码id前缀 -P
     * 返利网二维码id前缀 -F
     */

   public  enum PrexQRID{
        /**平台*/
        P("P"),

        /** 返利网*/
        F("F");
        private String  name;
        PrexQRID(String name){
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }


    /**
     * 随机类型
     */
    public enum  RandomTag{
        /**二维码随机数*/
        QRID,

        /**收款备注码*/
        REMARKNUM
    }

     enum  RandomType{
        /**
         * 数字
         */
        NUMBER,

        /**
         * 小写字母
         */
        LOWERCASE,

        /**
         * 大写字母
         */
        UPPERCASE,

        /**
         * 大小写字母
         */
        LOWERCASEANDUPPERCASE,

        /**
         * 数字+大小写字母
         */
        ALL
    }

  private   enum Generator{
        /**
         * MD5生成方式
         */
        MD5GENERATOR,

        /**
         * UUID生成方式
         */
        UUIDGENERATOR
    }

    /**
     * 生成二维码id,每次都是新的,返回格式 P/F+随机数+U+uid
     * @param prexQRID 来源/平台/返利网
     * @param qrContext 二维码内容
     * @param uid  二维码所属用户id
     * @return
     */
    public String genQrId(PrexQRID prexQRID,String qrContext,String uid){
        String randomStr = randomStr(qrContext, RandomType.ALL, 10, Generator.UUIDGENERATOR, RandomTag.QRID);
        return String.format(qridFormater,prexQRID.name,randomStr,uid);
    }


    /**
     * 生成收款备注码， 格式：6位随机，2天内不重复。
     */
    public String genRemarkNum(String orderNumber){
        return randomStr(orderNumber, RandomType.ALL, 6, Generator.UUIDGENERATOR, RandomTag.REMARKNUM);
    }


    /**
     * 生成随机字符串
     */
    private  String randomStr(String sourcesStr, RandomType type, int length, Generator generator,RandomTag randomTag) {
        ConvertUtil convertUtil;
        switch (generator) {
            case MD5GENERATOR:
                convertUtil = Md5ConvertUtil.getInstance();
                break;
            case UUIDGENERATOR:
                convertUtil = UuidConvertUtil.getInstance();
                break;
            default:
                convertUtil = Md5ConvertUtil.getInstance();
        }
        String[] values = convertUtil.shortString(sourcesStr, type, length);
        for (String value : values) {
            if (noExist(randomTag,sourcesStr, value)) {
                return value;
            }
        }
        return null;
    }


    /**
     * 判断备注码是否存在，默认保存2天，可使用完毕后删除
     */
    private  boolean noExist(RandomTag randomTag, String sourcesStr,String value) {
        switch (randomTag){
            case QRID:
                return true;
            case REMARKNUM:
                return !existRemarkNumber(sourcesStr,value);
            default:
                return true;
        }
    }



    /**
     * 不存在的key,写入redis
     */
    private  boolean existRemarkNumber(String sourcesStr, String key){
        try {
            String fullKey =Remark_Num_Key+":"+key;
            if(redisService.existsKey(fullKey)) return true;
            redisService.getSetOperations().add(fullKey, sourcesStr);
            redisService.expireKey(fullKey,2,TimeUnit.DAYS);
            return false;
        }catch (Exception e){
            log.error("[生成备注码] 错误：{}",e.getMessage(),e);
            return true;
        }
    }


    /**
     * 删除用完的key
     */
   private void delRemarkNumber(String remarkNumber){
       try {
           String fullKey =Remark_Num_Key+":"+remarkNumber;
           redisService.deleteKey(fullKey);
       }catch (Exception e){
           log.error("[删除备注码] 错误：{}",e.getMessage(),e);
       }
   }

}
