package com.xinbo.fundstransfer.component.shiro;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.service.RedisService;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.eis.AbstractSessionDAO;
import org.apache.shiro.session.mgt.eis.JavaUuidSessionIdGenerator;
import org.apache.shiro.session.mgt.eis.SessionIdGenerator;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.apache.shiro.web.util.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.RedisTemplate;

import com.xinbo.fundstransfer.component.spring.SpringContextUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.ServletRequest;

public class ShiroSessionDAO extends AbstractSessionDAO {
	private SessionIdGenerator sessionIdGenerator = new JavaUuidSessionIdGenerator();
	private Logger logger = LoggerFactory.getLogger(ShiroSessionDAO.class);
	private RedisTemplate<String, Object> redisTemplate;
	private long EXPIRE_MILLISECONDS = 86400000;
	/**
	 * session对象的缓存
	 */
	private static final Cache<String, Session> sessionCache = CacheBuilder.newBuilder()
			.expireAfterWrite(1, TimeUnit.MINUTES).build();

	public void setSessionIdGenerator(SessionIdGenerator sessionIdGenerator) {
		this.sessionIdGenerator = sessionIdGenerator;
	}

	protected Serializable generateSessionId(Session session) {
		ServletRequest req = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		String USERID = new SimpleCookie("JUSERID").readValue(WebUtils.toHttp(req), null);
		Integer userId = AppConstants.THREADLOCAL_USER_ID.get();
		userId = userId != null ? userId : Integer.valueOf(USERID);
		if (this.sessionIdGenerator == null) {
			String msg = "sessionIdGenerator attribute has not been configured.";
			throw new IllegalStateException(msg);
		} else {
			return CommonUtils.genJSessionIdByFactor(userId);
		}
	}

	@Override
	public void update(Session session) throws UnknownSessionException {
		saveSession(session);
	}

	@Override
	public void delete(Session session) {
		if (session == null || session.getId() == null) {
			return;
		}
		Integer userId = Integer.valueOf(CommonUtils.genFactorByJSessionId(session.getId()));
		getRedisTemplate().delete(RedisKeys.gen4ShiroSession(userId));
		sessionCache.invalidate(RedisKeys.gen4ShiroSession(userId));
		String closeMsg = CommonUtils.genCloseMsg4WS(userId, null, null, AppConstants.LOGOUT_WS, null);
		SpringContextUtils.getBean(RedisService.class).convertAndSend(RedisTopics.CLOSE_WEBSOCKET, closeMsg);
		logger.trace("delete session, host :{}", session.getHost());
	}

	@Override
	public Collection<Session> getActiveSessions() {
		Set<Session> sessions = new HashSet<>();
		Set<String> keys = getRedisTemplate().keys(RedisKeys.genPattern4ShiroSession());
		if (keys != null && keys.size() > 0) {
			for (String key : keys) {
				Session o = (Session) getRedisTemplate().boundValueOps(key).get();
				sessions.add(o);
			}
		}
		return sessions;
	}

	@Override
	protected Serializable doCreate(Session session) {
		Serializable serializable = this.generateSessionId(session);
		this.assignSessionId(session, serializable);
		this.saveSession(session);
		return serializable;
	}

	@Override
	protected Session doReadSession(Serializable sessionId) {
		if (sessionId == null) {
			return null;
		}
		Integer userId = CommonUtils.genFactorByJSessionId(sessionId);
		try {
			boolean cache = CommonUtils.getSessionFromCache();
			Session result = null;
			if (cache) {
				result = sessionCache.getIfPresent(RedisKeys.gen4ShiroSession(userId));
				if (result != null) {
					return result;
				}
			}
			BoundValueOperations<String, Object> boundValueOps = getRedisTemplate()
					.boundValueOps(RedisKeys.gen4ShiroSession(userId));
			result = (Session) boundValueOps.get();
			if (result == null) {
				String closeMsg = CommonUtils.genCloseMsg4WS(userId, null, null, AppConstants.LOGOUT_WS, null);
				SpringContextUtils.getBean(RedisService.class).convertAndSend(RedisTopics.CLOSE_WEBSOCKET, closeMsg);
			} else {
				if (cache) {
					sessionCache.put(RedisKeys.gen4ShiroSession(userId), result);
				}
				boundValueOps.expire(EXPIRE_MILLISECONDS, TimeUnit.MILLISECONDS);
			}
			return result;
		} catch (Exception e) {
			sessionCache.invalidate(RedisKeys.gen4ShiroSession(userId));
			return null;
		}
	}

	private void saveSession(Session session) {
		if (session == null || session.getId() == null) {
			return;
		}
		session.setTimeout(EXPIRE_MILLISECONDS);
		Integer userId = CommonUtils.genFactorByJSessionId(session.getId());
		getRedisTemplate().boundValueOps(RedisKeys.gen4ShiroSession(userId)).set(session, EXPIRE_MILLISECONDS,
				TimeUnit.MILLISECONDS);
		sessionCache.put(RedisKeys.gen4ShiroSession(userId),session);
	}

	private RedisTemplate<String, Object> getRedisTemplate() {
		if (null == redisTemplate) {
			redisTemplate = SpringContextUtils.getBean("redisTemplate", RedisTemplate.class);
		}
		return redisTemplate;
	}

}
