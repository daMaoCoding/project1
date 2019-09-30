package com.xinbo.fundstransfer.service.impl;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.xinbo.fundstransfer.domain.pojo.LoginMsg;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.entity.BizAccountMore;
import com.xinbo.fundstransfer.domain.entity.BizDeductAmount;
import com.xinbo.fundstransfer.domain.entity.BizRebateUser;
import com.xinbo.fundstransfer.domain.repository.DeductAmountRepository;
import com.xinbo.fundstransfer.domain.repository.RebateUserRepository;
import com.xinbo.fundstransfer.service.RebateUserService;
import com.xinbo.fundstransfer.service.RedisService;

@Service
public class RebateUserServiceImpl implements RebateUserService {
	private static final Logger log = LoggerFactory.getLogger(RebateUserServiceImpl.class);
	@Autowired
	private RebateUserRepository rebateUserRepository;

	@Autowired
	private DeductAmountRepository deductAmountRepository;
	@Autowired
	private RedisService redisSer;

	private static final Cache<Object, BizRebateUser> RebateUserCacheBuilder = CacheBuilder.newBuilder()
			.maximumSize(20000).expireAfterWrite(4, TimeUnit.DAYS).build();
	private static final Cache<Object, BizRebateUser> RebateUserNameCacheBuilder = CacheBuilder.newBuilder()
			.maximumSize(20000).expireAfterWrite(4, TimeUnit.DAYS).build();

	private static ThreadLocal<PasswordEncoder> ThreadLocal_PasswordEncode = new ThreadLocal<>();
	private static ObjectMapper mapper = new ObjectMapper();

	@Override
	@Transactional
	public BizRebateUser save(BizRebateUser data) {
		if (Objects.isNull(data)) {
			return null;
		}
		BizRebateUser user = rebateUserRepository.saveAndFlush(data);
		broadCast(user);
		return user;
	}

	@Override
	public void flushCache(BizRebateUser data) {
		if (Objects.nonNull(data)) {
			RebateUserNameCacheBuilder.put(data.getUserName(), data);
			RebateUserCacheBuilder.put(data.getUid(), data);
		}
	}

	@Override
	public BizRebateUser getFromCacheByUid(String uid) {
		BizRebateUser user = RebateUserCacheBuilder.getIfPresent(uid);
		if (null == user) {
			user = rebateUserRepository.findByUid(uid);
			if (null == user)
				return null;
			else {
				RebateUserCacheBuilder.put(user.getUid(), user);
				RebateUserNameCacheBuilder.put(user.getUserName(), user);
			}
		}
		return user;
	}

	public String deLpwd(String lped) {
		if (StringUtils.isBlank(lped))
			return null;
		lped = new String(org.apache.mina.util.Base64.decodeBase64(lped.getBytes()));
		return lped.substring(0, 5) + lped.substring(8);
	}

	@Override
	public BizRebateUser getFromCacheByUserName(String username) {
		BizRebateUser user = RebateUserNameCacheBuilder.getIfPresent(username);
		if (null == user) {
			user = rebateUserRepository.findByUserName(username);
			if (user != null) {
				RebateUserCacheBuilder.put(user.getUid(), user);
				RebateUserNameCacheBuilder.put(user.getUserName(), user);
			}
		}
		return user;
	}

	@Override
	public String checkUserAndPass(String username, String password) {
		if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
			log.info("checkUserAndPass>> username or password is null ,username {}", username);
			return LoginMsg.UserOrPassNull.getMsg();
		}
		BizRebateUser user = getFromCacheByUserName(username);
		if (null == user || user.getStatus() != 0) {
			log.info("checkUserAndPass>>user doesn't exists or status is wrong ,user {}", username);
			return LoginMsg.UserNotExtOrStatusErr.getMsg();
		}
		if (StringUtils.isBlank(user.getPassword()) || !getPasswordEncode().matches(password, user.getPassword())) {
			log.info("checkUserAndPass>>password check fail,username {}", username);
			return LoginMsg.UserOrPassErr.getMsg();
		}
		log.info("checkUserAndPass>>username and password check passed ! username {}", username);
		return LoginMsg.Success.getMsg();
	}

	private static PasswordEncoder getPasswordEncode() {
		PasswordEncoder passEncode = ThreadLocal_PasswordEncode.get();
		if (passEncode == null) {
			PasswordEncoder encoder = new BCryptPasswordEncoder(4);
			ThreadLocal_PasswordEncode.set(encoder);
			return encoder;
		}
		return passEncode;
	}

	@Override
	public void cleanCache() {
		RebateUserCacheBuilder.invalidateAll();
		RebateUserNameCacheBuilder.invalidateAll();
	}

	@Override
	public BizDeductAmount deductAmountByUid(String uid) {
		return deductAmountRepository.findByUid(uid);
	}

	@Override
	@Transactional
	public BizDeductAmount saveDeductAmount(BizDeductAmount deductAmount) {
		return deductAmountRepository.saveAndFlush(deductAmount);
	}

	private void broadCast(BizRebateUser rebateUser) {
		if (null == rebateUser || null == rebateUser.getUserName() || null == rebateUser.getUid())
			return;
		try {
			redisSer.convertAndSend(RedisTopics.REFRESH_REBATE_USER, mapper.writeValueAsString(rebateUser));
		} catch (Exception e) {
			log.error("AccMore >> broadCast id: {} ", e);
		}
	}
}
