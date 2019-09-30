package com.xinbo.fundstransfer;

import java.util.List;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("funds.transfer")
public class AppProperties {

	/**
	 * 系统版本
	 */
	private String version;
	/**
	 *   php老购彩平台key
	 */
	private String apikey;

	/**
	 *  java 新购彩平台key
	 */
	private String keystore;


	/**
	 *  java 新购彩平台 url.
	 */
	private String uri;

	/**
	 *  返利网 key
	 */
	private String rebatesalt;

	/**
	 * 返利网 uri
	 */
	private String rebateuri;


	/**
	 * 卡巴拉 key
	 */
	private String cabanasalt;

	/**
	 * 卡巴拉 uris
	 */
	private List<String> cabanauris;


	/**
	 * 卡巴拉请求出入款 限流ips
	 */
	private Double ratelimitPermitsPerSecond;


	/**
	 * 卡巴拉请求出入款  需限流的url（startsWith 匹配）
	 */
	private List<String> ratelimitURL;


	/**
	 * 二维码验证工具(android app) key
	 */
	private String qrtoolssalt;


	/**
	 * 平台调用出入款，配置平台crk的ip白名单
	 * 通过header 限制调用者ip,格式：FROM_KEY 的值(header中)+_IPS
	 * 例如，平台调用出入款时，请求Header: FROM_KEY=PT_CRK  ，出入款启动配置白名单，funds.transfer.PT_CRK_IPS=***,***,***,
	 */
	private List<String> PT_CRK_IPS;


	/**
	 * 聊天室调用出入款，配置聊天室的白名单
	 */
	private List<String> CHAT_PAY_IPS;


	/**
	 * 客服系统(聊天室支付) url
	 */
	private String callCenterUrl;

}