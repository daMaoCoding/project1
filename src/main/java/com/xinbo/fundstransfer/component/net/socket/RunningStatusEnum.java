package com.xinbo.fundstransfer.component.net.socket;

/**
 * 帐号状态定义： 离线，未抓取-OFFLINE，正常，抓取中-NORMAL，异常，超时未上报-ERROR, 暂停-PAUSE，告警-WARN
 * 
 * 
 *
 */
public enum RunningStatusEnum {
	OFFLINE, NORMAL, ERROR, PAUSE, WARN;
}