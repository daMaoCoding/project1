package com.xinbo.fundstransfer.utils;

/**
 * 服务领域区分
 * ALL, 避免使用
 * WEB　站点服务
 * APP　Cabana接口服务
 * PC　桌面工具服务
 * ACCOUNTING　账户服务
 * CACHFLOW   流水服务
 * REPORT  报表服务
 * INNER   内部服务器
 */
public enum  ServiceDomain {
    ALL,
    WEB,
    APP,
    PC,
    TASK,
    ACCOUNTING,
    CACHFLOW,
    REPORT,
    INNER
}
