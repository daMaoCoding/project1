package com.xinbo.fundstransfer.component.mvc;

/**
 * ************************
 *  header限制常量，@RequireHeader()使用
 * @author tony
 */
public  final class HeaderCondition {

    /**
     * 调用者的请求头 key,固定。
     */
    public static final String requireHeaderKey="FROM_KEY";


    /**
     * 平台调用出入款需要携带Header  (平台的crk),[PT_CRK,此处必须大写]，调用者的参数可以是pt_crk
     */
    public static final String  ptCrkHeader = "PT_CRK";


    /**
     * 聊天室调用出入款携带Header
     */
    public static final String  chatPayHeader = "CHAT_PAY";
}
