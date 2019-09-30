package com.xinbo.fundstransfer.configuation;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.Filter;

import com.xinbo.fundstransfer.AppConstants;
import org.apache.shiro.spring.LifecycleBeanPostProcessor;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.xinbo.fundstransfer.component.shiro.RestfulUserFilter;
import com.xinbo.fundstransfer.component.shiro.ShiroAuthorizingRealm;
import com.xinbo.fundstransfer.component.shiro.ShiroSessionDAO;
import com.xinbo.fundstransfer.component.shiro.SimpleFormAuthenticationFilter;

@Configuration
public class ShiroConfiguration {
	private static Map<String, String> filterChainDefinitionMap = new LinkedHashMap<>();

	@Bean(name = "securityManager")
	public DefaultWebSecurityManager getDefaultWebSecurityManager() {
		DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
		securityManager.setRealm(getShiroRealm());
		securityManager.setCacheManager(new org.apache.shiro.cache.MemoryConstrainedCacheManager());
		securityManager.setSessionManager(sessionManager());
		return securityManager;
	}

	@Bean(name = "shiroAuthorizingRealm")
	public ShiroAuthorizingRealm getShiroRealm() {
		return new ShiroAuthorizingRealm();
	}

	@Bean(name = "lifecycleBeanPostProcessor")
	public LifecycleBeanPostProcessor getLifecycleBeanPostProcessor() {
		return new LifecycleBeanPostProcessor();
	}

	@Bean
	public DefaultAdvisorAutoProxyCreator getDefaultAdvisorAutoProxyCreator() {
		DefaultAdvisorAutoProxyCreator daap = new DefaultAdvisorAutoProxyCreator();
		daap.setProxyTargetClass(true);
		return daap;
	}

	@Bean
	public AuthorizationAttributeSourceAdvisor getAuthorizationAttributeSourceAdvisor() {
		AuthorizationAttributeSourceAdvisor aasa = new AuthorizationAttributeSourceAdvisor();
		aasa.setSecurityManager(getDefaultWebSecurityManager());
		return new AuthorizationAttributeSourceAdvisor();
	}

	@Bean(name = "shiroFilter")
	public ShiroFilterFactoryBean getShiroFilterFactoryBean() {
		ShiroFilterFactoryBean shiroFilterFactoryBean = new ShiroFilterFactoryBean();
		shiroFilterFactoryBean.setSecurityManager(getDefaultWebSecurityManager());
		shiroFilterFactoryBean.setLoginUrl("/auth/login");
		shiroFilterFactoryBean.setUnauthorizedUrl("/auth/unauthorized");
		filterChainDefinitionMap.put("/login", "authc");
		filterChainDefinitionMap.put("/r/**", "user");
		// filterChainDefinitionMap.put("/api/**", "perms");
		filterChainDefinitionMap.put("/**", "anon");
		shiroFilterFactoryBean.setFilterChainDefinitionMap(filterChainDefinitionMap);
		Map<String, Filter> filters = new LinkedHashMap<>();
		filters.put("authc", getSimpleFormAuthenticationFilter());
		filters.put("user", getRestfulUserFilter());
		shiroFilterFactoryBean.setFilters(filters);
		return shiroFilterFactoryBean;
	}

	@Bean(name = "simpleFormAuthenticationFilter")
	public SimpleFormAuthenticationFilter getSimpleFormAuthenticationFilter() {
		return new SimpleFormAuthenticationFilter();
	}

	@Bean(name = "restfulUserFilter")
	public RestfulUserFilter getRestfulUserFilter() {
		return new RestfulUserFilter();
	}

	@Bean
	public DefaultWebSessionManager sessionManager() {
		DefaultWebSessionManager sessionManager = new DefaultWebSessionManager();
		SimpleCookie cookie = new SimpleCookie(AppConstants.JSESSIONID);
		cookie.setHttpOnly(true);
		sessionManager.setSessionIdCookie(cookie);
		sessionManager.setSessionDAO(new ShiroSessionDAO());
		return sessionManager;
	}

}
