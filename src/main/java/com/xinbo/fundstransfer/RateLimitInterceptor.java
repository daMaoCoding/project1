package com.xinbo.fundstransfer;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.RateLimiter;
import com.google.common.util.concurrent.Striped;
import com.xinbo.fundstransfer.domain.GeneralResponseData;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("rateLimitInterceptor")
public class RateLimitInterceptor extends HandlerInterceptorAdapter {
	@Autowired
	private AppProperties appProperties;
	
	private ObjectMapper mapper = new ObjectMapper();
	private static final Cache<String, RateLimiter> ACCOUNT_RATELIMITER = CacheBuilder.newBuilder().maximumSize(60000).expireAfterWrite(60, TimeUnit.SECONDS).build();
	private Striped<Lock> lockStripes = Striped.lock(1024);

	protected boolean tryAcquire(String tk) {
		if (StringUtils.isEmpty(tk)) {
			return false;
		}
		if (ACCOUNT_RATELIMITER.getIfPresent(tk) == null) {
			Lock lock = lockStripes.get(tk);
			try {
				lock.lock();
				if (ACCOUNT_RATELIMITER.getIfPresent(tk) == null) {
					Double permits = appProperties.getRatelimitPermitsPerSecond();
					if(permits == null) {
						permits = 10.0;
					}
					ACCOUNT_RATELIMITER.put(tk, RateLimiter.create(permits));
				}
			} finally {
				lock.unlock();
			}
		}
		if (ACCOUNT_RATELIMITER.getIfPresent(tk) == null) {
			return false;
		}
		return ACCOUNT_RATELIMITER.getIfPresent(tk).tryAcquire();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		boolean needValid = false;
		if(CollectionUtils.isEmpty(appProperties.getRatelimitURL())) {
			return true;
		} 
		for(String url : appProperties.getRatelimitURL()) {
			if(request.getRequestURI().startsWith(url)) {
				needValid = true;
				break;
			}
		}
		if(!needValid) {
			return true;
		}
		String tk = request.getHeader("tk");
		if (!tryAcquire(tk)) {
			log.error("URL {} tk {} 请求限流", request.getRequestURI(), tk);
			String rs = mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请求限流."));
			PrintWriter printWriter = null;
			try {
				printWriter = response.getWriter();
				printWriter.print(rs);
				printWriter.flush();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (printWriter != null) {
					printWriter.close();
				}
			}
			return false;
		}
		return true;
	}
}
