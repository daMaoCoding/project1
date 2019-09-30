package com.xinbo.fundstransfer.component.net.socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.xinbo.fundstransfer.CommonUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * socket服务端，控制银行流水抓取
 * 
 *
 *
 */
@Service
@Scope("singleton")
public class MinaMonitorServer {
	private static final Logger log = LoggerFactory.getLogger(MinaMonitorServer.class);
	private MonitorServerHandler handler;
	private NioSocketAcceptor acceptor;
	private ObjectMapper mapper = new ObjectMapper();

	public MinaMonitorServer() {
		try {
			long beginTime = System.currentTimeMillis();
			// 创建一个非阻塞的server端的Socket
			acceptor = new NioSocketAcceptor();
			handler = new MonitorServerHandler();
			// 设置过滤器（使用Mina提供的文本换行符编解码器）
			TextLineCodecFactory lineCodec = new TextLineCodecFactory(Charset.forName("UTF-8"));
			lineCodec.setDecoderMaxLineLength(1024 * 1024); // 1M
			lineCodec.setEncoderMaxLineLength(1024 * 1024); // 1M
			acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(lineCodec));
			// 设置逻辑处理
			acceptor.setHandler(handler);
			// 读写通道10秒内无操作进入空闲状态
			acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 10);
			// 客户端不会使用read的方式读取数据，先将属性设置为true的代码注释掉
			// acceptor.getSessionConfig().setUseReadOperation(true);
			// 绑定端口
			acceptor.setReuseAddress(true);
			acceptor.bind(new InetSocketAddress(9123));
			log.info("Socket create success. listen on port 9123, Time-consuming: {} milliseconds.",
					(System.currentTimeMillis() - beginTime));
		} catch (IOException e) {
			log.error("Socket create fail.", e);
		}
	}

	public Map<String, Object> socketInfo() {
		Map<String, Object> ret = new HashMap<>();
		ret.put("IP", CommonUtils.getInternalIp());
		ret.put("COUNT", acceptor.getManagedSessions().size());
		return ret;
	}

	/**
	 * 给指定client IP的socket连接发送消息
	 *
	 */
	public void messageSend(String message) {
		log.debug("received push message :{}", message);
		if (StringUtils.isBlank(message)) {
			return;
		}
		try {
			MessageEntity entity = mapper.readValue(message, MessageEntity.class);
			String ip = StringUtils.trimToNull(entity.getIp());
			if (StringUtils.isNumeric(ip)) {
				messageSentByAccountId(message, Integer.valueOf(ip));
				return;
			}
			Map<Long, IoSession> sessions = acceptor.getManagedSessions();
			Iterator<Entry<Long, IoSession>> entries = sessions.entrySet().iterator();
			while (entries.hasNext()) {
				Map.Entry<Long, IoSession> entry = entries.next();
				if (null == ip) {
					entry.getValue().write(message);
				} else {
					// 给指定IP发消息，然后退出循环
					if (ip.equals(entry.getValue().getAttribute(MonitorServerHandler.SESSION_CLIENTIP_KEY))) {
						entry.getValue().write(message);
						log.debug("send msg, ip: {}", ip);
						// 如果action是新增2、删除帐号3，同步更新缓存，注意action值参考ActionEventEnum值
						if (message.contains("\"action\":2")) {
							MessageEntity<AccountEntity> o = mapper.readValue(message,
									new TypeReference<MessageEntity<AccountEntity>>() {
									});
							getHandler().getAccountSessionMap().put(o.getData().getId(), entry.getValue().getId());
							log.debug("添加缓存帐号, ip: {}，account:{}", ip, o.getData().getId());
						} else if (message.contains("\"action\":3")) {
							MessageEntity<Integer> o = mapper.readValue(message,
									new TypeReference<MessageEntity<Integer>>() {
									});
							getHandler().getAccountSessionMap().remove(o.getData());
							log.debug("删除缓存帐号, ip: {}，account:{}", ip, o.getData());
						}
						break;
					}
				}
			}
		} catch (Exception e) {
			log.error("", e);
		}
	}

	/**
	 * 给指定帐号发送消息
	 * 
	 */
	public void messageSentByAccountId(String message, Integer accountId) {
		log.debug("msg: {}, accountId： {}", message, accountId);
		Long sessionid = getHandler().getAccountSessionMap().get(accountId);
		if (null != sessionid) {
			IoSession session = acceptor.getManagedSessions().get(sessionid);
			if (session != null) {
				session.write(message);
				log.debug("Message sent, ip: {}", session.getAttribute(MonitorServerHandler.SESSION_CLIENTIP_KEY));
			}
		} else {
			log.debug("未找到{}帐号的连接会话信息", accountId);
		}
	}

	/**
	 * 根据帐号ID找到所在工具的IP
	 * 
	 * @param accountId
	 * @return
	 */
	public String getIPByAccountId(Integer accountId) {
		log.debug("accountId： {}", accountId);
		Long sessionid = getHandler().getAccountSessionMap().get(accountId);
		if (null != sessionid) {
			IoSession session = acceptor.getManagedSessions().get(sessionid);
			if (session != null) {
				log.debug("find ip: {}", session.getAttribute(MonitorServerHandler.SESSION_CLIENTIP_KEY));
				return String.valueOf(session.getAttribute(MonitorServerHandler.SESSION_CLIENTIP_KEY));
			}
		} else {
			log.debug("未找到{}帐号的连接会话信息", accountId);
		}
		return "";
	}

	public MonitorServerHandler getHandler() {
		return handler;
	}

	public void setHandler(MonitorServerHandler handler) {
		this.handler = handler;
	}

	public NioSocketAcceptor getAcceptor() {
		return acceptor;
	}

	public void setAcceptor(NioSocketAcceptor acceptor) {
		this.acceptor = acceptor;
	}

}
