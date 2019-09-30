package com.xinbo.fundstransfer.component.net.http.restTemplate;

import com.xinbo.fundstransfer.chatPay.flwsynzfbwx.reqVo.ReqFlwQrValBack;
import org.springframework.stereotype.Component;


/**
 * ************************
 * 与返利网http交互
 * @author tony
 */

@Component
public class RebateServiceApiStatic extends  BaseServiceApiStatic {

    private static final String notfiFlwQrValStatus_path ="/api/synwxzfb";



    /**
     * 通知返利网二维码验证状态
     */
    public int notfiFlwQrValStatus(ReqFlwQrValBack reqFlwQrValBack){
        return basePostJsonFlw(notfiFlwQrValStatus_path,reqFlwQrValBack,null).getIntValue("status"); //失败返回0
    }




}
