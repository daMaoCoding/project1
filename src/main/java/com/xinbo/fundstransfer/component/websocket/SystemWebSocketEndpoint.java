
package com.xinbo.fundstransfer.component.websocket;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

import com.xinbo.fundstransfer.component.net.socket.AccountEntity;
import com.xinbo.fundstransfer.component.net.socket.RunningStatusEnum;
import com.xinbo.fundstransfer.domain.ResponseData;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.CabanaStatus;
import com.xinbo.fundstransfer.service.*;
import com.xinbo.fundstransfer.utils.ServiceDomain;
import org.apache.commons.lang3.StringUtils;
import org.apache.mina.util.ConcurrentHashSet;
import org.apache.shiro.util.CollectionUtils;
import org.apache.tomcat.websocket.WsSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.net.socket.ActionEventEnum;
import com.xinbo.fundstransfer.component.net.socket.MessageEntity;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.component.spring.SpringContextUtils;

@Component
@ServerEndpoint(value = "/ws/system", configurator = GetHttpSessionConfigurator.class)
public class SystemWebSocketEndpoint {
    private static Timer timer = new Timer();
    private static Set<WsSession> sessionSet = new ConcurrentHashSet<>();
    private static Logger LOG = LoggerFactory.getLogger(SystemWebSocketEndpoint.class);
    private static ObjectMapper mapper = new ObjectMapper();
    private static boolean BROAD_CAST_REPORT_STATUS = true;
    private static boolean BROAD_CAST_APP_STATUS = true;
    private static final String HANDLE_MESSAGE_IPS = "IPS";
    private static final String HANDLE_MESSAGE_APP = "IDS,";
    private static final ConcurrentHashSet<Integer> APPIDS = new ConcurrentHashSet<>();
    private static RateLimiter RATE_LIMITER = RateLimiter.create(0.5);
    private static RedisService redisService;
    private static CabanaService cabanaService;
    private static AccountService accountService;

    private static String serviceTag;

    @PostConstruct
    public void init() {
        LOG.error(String.format("服务启动类别 %s  %s", ServiceDomain.valueOf(serviceTag), (ServiceDomain.valueOf(serviceTag)== ServiceDomain.WEB)));
        if (ServiceDomain.valueOf(serviceTag)== ServiceDomain.WEB||ServiceDomain.valueOf(serviceTag)== ServiceDomain.TASK) {
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            timer.schedule(new TimerTask() {
                public void run() {
                    try {
                        if (BROAD_CAST_REPORT_STATUS | (BROAD_CAST_REPORT_STATUS = false)) {
                            LOG.debug("handleMessage：{}", HANDLE_MESSAGE_IPS);
                            HostMonitorService hostMonitorService = SpringContextUtils.getBean(HostMonitorService.class);
                            MessageEntity messageEntity = new MessageEntity();
                            messageEntity.setAction(ActionEventEnum.REPORT.ordinal());
                            hostMonitorService.messageBroadCast(messageEntity);
                        }
                    } catch (Exception e) {
                        LOG.error("" + e);
                    }
                }
            }, 3000, 3000);
        }
    }

    @OnOpen
    public void open(Session ses, EndpointConfig conf) {
        RATE_LIMITER.acquire();
        WsSession session = (WsSession)ses;
        String jSessionId = (String) conf.getUserProperties().get(AppConstants.JSESSIONID);
        LOG.debug("Opened new system session. jSessionId: {} ", jSessionId);
        closeOtherWSByJSessionId(true, AppConstants.HISTORY_WS, jSessionId);
        sessionSet.add(session);
    }

    @OnMessage
    public void handleMessage(Session ses, String message) throws IOException {
        if (StringUtils.isBlank(message)) {
            return;
        }
        if (HANDLE_MESSAGE_IPS.equals(message)) {
            BROAD_CAST_REPORT_STATUS = true;
        }
        if (message.startsWith(HANDLE_MESSAGE_APP)) {
            message = message.replace(HANDLE_MESSAGE_APP, StringUtils.EMPTY);
            if (StringUtils.isBlank(message)) {
                return;
            }
            for (String d : message.split(",")) {
                if (StringUtils.isNumeric(d)) {
                    APPIDS.add(Integer.valueOf(d));
                }
            }
            BROAD_CAST_APP_STATUS = true;
        }
    }

    @OnError
    public void error(Session session, Throwable error) {
        try {
            session.close();
        } catch (Exception e) {
            LOG.error("", error);
        }
    }

    @OnClose
    public void close(Session ses, CloseReason reason) {
        if (reason != null && StringUtils.equals(reason.getReasonPhrase(), AppConstants.HISTORY_WS)) {
            return;
        }
        if (reason != null && StringUtils.equals(reason.getReasonPhrase(), AppConstants.LOGOUT_WS)) {
            SpringContextUtils.getBean(SysUserService.class).broadCastCategoryInfo();// 广播：在线用户统计信息
            return;
        }
        WsSession session = (WsSession)ses;
        String closeCode = reason == null ? StringUtils.EMPTY : reason.getCloseCode().toString();
        String jSessionId = session.getHttpSessionId();
        LOG.debug("close websocket session. jSessionId: {} ,closeCode:{}", jSessionId, closeCode);
        sessionSet.remove(session);
        if (jSessionId != null) {
            final Integer userId = CommonUtils.genFactorByJSessionId(jSessionId);
            redisService = SpringContextUtils.getBean(RedisService.class);
            redisService.getStringRedisTemplate().boundValueOps(RedisKeys.gen4ShiroSession(userId)).expire(600,
                    TimeUnit.MILLISECONDS);
            timer.schedule(new TimerTask() {
                public void run() {
                    try {
                        SysUserService userService = SpringContextUtils.getBean(SysUserService.class);
                        if (!userService.online(userId)) {
                            String closeMsg = CommonUtils.genCloseMsg4WS(userId, null, null, AppConstants.LOGOUT_WS,
                                    null);
                            SpringContextUtils.getBean(RedisService.class).convertAndSend(RedisTopics.CLOSE_WEBSOCKET,
                                    closeMsg);
                            userService.broadCastCategoryInfo();// 广播：在线用户统计信息
                        }
                    } catch (Exception e) {
                        LOG.error("" + e);
                    }
                }
            }, 650);
        }
    }

    public void closeBySessionId(String wsType, String sessionId) {
        closeOtherWSByJSessionId(false, wsType, sessionId);
    }

    public void logout(Integer userId) {
        List<WsSession> sessions =sessionSet.stream().filter(p->
             Objects.nonNull(p.getHttpSessionId())&&Objects.equals(CommonUtils.genFactorByJSessionId(p.getHttpSessionId()),userId)
        ).collect(Collectors.toList());
        sessions.forEach((p -> {
            try {
                sessionSet.remove(p);
                p.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, AppConstants.LOGOUT_WS));
            } catch (Exception e) {
                LOG.error("关闭Session失败." + e);
            }
        }));
    }

    /**
     * @param message key:userId,version,message
     */
    public void sendMessage(String message) {
        try {
            Map params = mapper.readValue(message, HashMap.class);
            Integer userId = (Integer) params.get("userId");
            params.remove("userId");
            if (userId != null) {
                List<WsSession> sessions =sessionSet.stream().filter(p->
                     Objects.nonNull(p.getHttpSessionId())&&Objects.equals(CommonUtils.genFactorByJSessionId(p.getHttpSessionId()),userId)
                ).collect(Collectors.toList());
                sessions.forEach((o) -> {
                    if (null != o) {
                        synchronized (o) {
                            try {
                                LOG.trace("Send message : {}, sessionid : {}", message, o.getId());
                                o.getBasicRemote().sendText(message);
                            } catch (IOException e) {
                                LOG.error("发送消息", e);
                            }
                        }
                    }
                });
            } else {
                LOG.trace("Send message : {}", message);
                sessionSet.forEach(( o) -> {
                    synchronized (o) {
                        try {
                            if(o.isOpen()) {
                                o.getBasicRemote().sendText(message);
                            }
                        } catch (IOException e) {
                            LOG.error("发送消息", e);
                        }
                    }
                });
            }
        } catch (Exception e) {
            LOG.error("系统WebSocket发送信息失败..." + e.getMessage());
        }
    }

    private void closeOtherWSByJSessionId(boolean broadCast, String wsType, String sessionId) {
        Integer userId = CommonUtils.genFactorByJSessionId(sessionId);
        if (broadCast) {
            String closeMsg = CommonUtils.genCloseMsg4WS(userId, CommonUtils.getInternalIp(),
                    SystemWebSocketEndpoint.class, wsType, sessionId);
            SpringContextUtils.getBean(RedisService.class).convertAndSend(RedisTopics.CLOSE_WEBSOCKET, closeMsg);
        }
        List<Session> sessions =sessionSet.stream().filter(p->
             Objects.nonNull(p.getHttpSessionId())&&Objects.equals(CommonUtils.genFactorByJSessionId(p.getHttpSessionId()),userId)
                    &&!Objects.equals(p.getHttpSessionId(),sessionId)
        ).collect(Collectors.toList());

        sessions.forEach((p -> {
            try {
                sessionSet.remove(p);
                p.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, AppConstants.HISTORY_WS));
            } catch (Exception e) {
                LOG.error("关闭Session失败." + e);
            }
        }));
    }


    /**
     * 区分服务类型
     *
     * @param serviceTag
     */
    @Value("${service.tag}")
    public void setServiceTag(String serviceTag) {
        this.serviceTag = serviceTag;
    }
}
