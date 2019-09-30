package com.xinbo.fundstransfer.component.net.socket;

/**
 * 事件定义：
 * <p>
 * <ul>
 * <li>{@code 0 }开始-START</li>
 * <li>{@code 1 }停止-STOP</li>
 * <li>{@code 2 }新增银行卡-ADD</li>
 * <li>{@code 3 }删除银行卡-DEL</li>
 * <li>{@code 4 }设置-SETUP</li>
 * <li>{@code 5 }上报状态-REPORT</li>
 * <li>{@code 6 }暂无用到-SIGNHOOK</li>
 * <li>{@code 7 }连接建立确认-ACKNOWLEDGED</li>
 * <li>{@code 8 }删除主机-SHUTDOWN</li>
 * <li>{@code 9 }暂停-PAUSE</li>
 * <li>{@code 10 }恢复-RESUME</li>
 * <li>{@code 11 }工具升级-UPGRADE</li>
 * <li>{@code 12 }更新账号基本信息-UPDATE</li>
 * <li>{@code 13 }取消转账-CANCEL</li>
 * <li>{@code 14 }抓取流水-CAPTURE</li>
 * <li>{@code 15 }下发转帐-TRANSFER</li>
 * <li>{@code 16 }上报转帐信息-TRANSFERINFO</li>
 * <li>{@code 17 }上传回执-RECEIPT</li>
 * <li>{@code 18 }正常模式-NORMALMODE</li>
 * <li>{@code 19 }抓取流水模式-CAPTUREMODE</li>
 * <li>{@code 20 }转账模式-TRANSMODE</li>
 * <li>{@code 21} 流水确认</li>
 * <li>{@code 22} 上传日志</li>
 * <li>{@code 23} 补发流水</li>
 * </ul>
 * 
 */
public enum ActionEventEnum {
	START, STOP, ADD, DEL, SETUP, REPORT, SIGNHOOK, ACKNOWLEDGED, SHUTDOWN, PAUSE, RESUME, UPGRADE, UPDATE, CANCEL, CAPTURE, TRANSFER, TRANSFERINFO, RECEIPT, NORMALMODE, CAPTUREMODE, TRANSMODE, ACKLOG, UPLOADLOG, RESENDLOG;
}