package com.xinbo.fundstransfer.configuation;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.*;

import com.alibaba.fastjson.support.spring.GenericFastJsonRedisSerializer;
import com.xinbo.fundstransfer.component.redis.message.RedisTopicHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.redisson.Redisson;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import com.xinbo.fundstransfer.component.redis.*;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.ReflectionUtils;


/**
 * Redis配置 1.ysfStringRedisTemplate/ysfRedisTemplate
 * 为云闪付分库读写使用，库使用配置：redis.ysfDatabase = 2,默认不配为1. 2.redis 消息发送/接收 使用默认，不区分云闪付/其他
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({RedisProperties.class})
public class RedisConfiguration {

	@Autowired	   private RedisProperties redisProperties;
	private static LettuceConnectionFactory ysfConnectionFactory;
	private static LettuceConnectionFactory accountingConnectionFactory;

	/* 库 */
	@Value("${spring.redis.ysf-database:1}")	            private Integer ysfDatabase;         //云闪付随机金额
	@Value("${spring.redis.accounting-database:2}")	        private Integer accountingDatabase;  //系统账目

	/* 主机 */
	@Value("${spring.redis.host:127.0.0.1}")	            private String host;
	@Value("${spring.redis.port:6379}")	                    private Integer port;
	@Value("${spring.redis.password:}")	                    private String password;

	/* 通用配置 */
	@Value("${spring.redis.timeout:8000}")	                private long    timeout;
	@Value("${spring.redis.lettuce.pool.max-active:100}")	private Integer maxActive;
	@Value("${spring.redis.lettuce.pool.max-idle:10}")	    private Integer maxIdle;
	@Value("${spring.redis.lettuce.pool.max-wait:10000}")	private Long    maxWait;
	@Value("${spring.redis.lettuce.pool.min-idle:5}")	    private Integer minIdle;



	/**
	 * Topic处理
	 * 1.可以 继承 MessageDelegate<泛型> ，重写 handleMessage
	 * 2.类增加注解：@RedisTopicProcess(value = "HelloTopicName")
	 */
	@Bean
	@DependsOn("redisTopicHandlerMapping")
	RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory, RedisTopicHandler redisTopicHandler,
											GeneralListenerAdapter generalListenerAdapter, ToolStatusListenerAdapter toolStatusListenerAdapter,
											IncomeAuditAccountAllocatedMessageListenerAdapter incomeAuditAccountAllocatedMessageListenerAdapter,
											CloseWebSocketMessageListenerAdapter closeWebSocketMessageListenerAdapter,
											IncomeAuditAccountAllocatingMessageListenerAdapter incomeAuditAccountAllocatingMessageListenerAdapter,
											IncomeRequestListenerAdapter income, AssignAWAccountListenerAdapter assignAWAccountListenerAdapter,
											TaskReviewWSEndpointMsgListenerAdapter taskReviewWSEndpointMsgListenerAdapter,
											ThirdAccountDrawWSEndpointMsgListenerAdapter thirdAccountDrawWSEndpointMsgListenerAdapter,
											IncomeApproveAWMessageListener incomeApproveAWMessageListener,
											YSFBankAccountUseMessageListenerAdapter ySFBankAccountUseMessageListenerAdapter,
											YSFQrCodePublishMessageListenerAdapter ySFQrCodePublishMessageListenerAdapter) {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		Set<PatternTopic> topics = new HashSet<>();
		topics.add(new PatternTopic(RedisTopics.FRESH_INACCOUNT_YSFLOGIN_CACHE));
		topics.add(new PatternTopic(RedisTopics.FRESH_INACCOUNT_CACHE));
		topics.add(new PatternTopic(RedisTopics.BROADCAST));
		topics.add(new PatternTopic(RedisTopics.PUSH_MESSAGE_TOOLS));
		topics.add(new PatternTopic(RedisTopics.REFRESH_MENUPERMISSION));
		topics.add(new PatternTopic(RedisTopics.REFRESH_SYSTEM_PROFILE));
		topics.add(new PatternTopic(RedisTopics.REFRESH_LEVEL));
		topics.add(new PatternTopic(RedisTopics.REFRESH_ACCOUNT));
		topics.add(new PatternTopic(RedisTopics.REFRESH_ACCOUNT_LIST));
		topics.add(new PatternTopic(RedisTopics.REFRESH_USER));
		topics.add(new PatternTopic(RedisTopics.SYS_REBOOT));
		topics.add(new PatternTopic(RedisTopics.DELETED_SCREENSHOTS));
		topics.add(new PatternTopic(RedisTopics.ALLOC_OUT_TASK_SUSPEND));
		topics.add(new PatternTopic(RedisTopics.REFRESH_ALL_HANDICAP));
		topics.add(new PatternTopic(RedisTopics.REFRESH_ALL_SYS_SETTING));
		topics.add(new PatternTopic(RedisTopics.REFRESH_OTASK_MERGE_LEVEL));
		topics.add(new PatternTopic(RedisTopics.DELETED_FEEDBACK_SCREENSHOTS));
		topics.add(new PatternTopic(RedisTopics.ACCOUNT_CHANGE_BROADCAST));
		topics.add(new PatternTopic(RedisTopics.REFRESH_ACCOUNT_MORE));
		topics.add(new PatternTopic(RedisTopics.REFRESH_REBATE_USER));
		topics.add(new PatternTopic(RedisTopics.ACCOUNT_MORE_CLEAN));
		topics.add(new PatternTopic(RedisTopics.OTHER_ACCOUNT_CLEAN));
		topics.add(new PatternTopic(RedisTopics.REBATE_USER_CLEAN));

		container.addMessageListener(generalListenerAdapter, topics);
		container.addMessageListener(incomeAuditAccountAllocatedMessageListenerAdapter,
				new PatternTopic(RedisTopics.ACCOUNT_ALLOCATED));
		container.addMessageListener(closeWebSocketMessageListenerAdapter,
				new PatternTopic(RedisTopics.CLOSE_WEBSOCKET));
		container.addMessageListener(incomeAuditAccountAllocatingMessageListenerAdapter,
				new PatternTopic((RedisTopics.ACCOUNT_ALLOCATING)));
		container.addMessageListener(income, new PatternTopic(RedisTopics.INCOME_REQUEST));
		container.addMessageListener(toolStatusListenerAdapter, new PatternTopic(RedisTopics.TOOLS_STATUS_REPORT));
		container.addMessageListener(taskReviewWSEndpointMsgListenerAdapter,
				new PatternTopic(RedisTopics.ASIGN_REVIEWTASK_TOPIC));
		container.addMessageListener(thirdAccountDrawWSEndpointMsgListenerAdapter,
				new PatternTopic(RedisTopics.FRESH_ACCOUNT_THIRDDRAW));
		container.addMessageListener(assignAWAccountListenerAdapter,
				new PatternTopic(RedisTopics.ASSIGN_INCOMEAWACCOUNT_TOPIC));
		container.addMessageListener(incomeApproveAWMessageListener,
				new PatternTopic(RedisTopics.ASSIGNED_INCOMEAWACCOUNT_TOPIC));
		container.addMessageListener(ySFBankAccountUseMessageListenerAdapter,
				new PatternTopic(RedisTopics.YSF_BANK_ACCOUNT_USE_TIME));
		container.addMessageListener(ySFQrCodePublishMessageListenerAdapter,
				new PatternTopic(RedisTopics.YSF_QR_CODE_MSG));
		redisTopicHandler.initContainer(container);
		return container;
	}











	/**
	 * 默认库 stringRedisTemplate
	 */
	@Bean
	public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
		RedisTemplate<Object, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(redisConnectionFactory);
		template.afterPropertiesSet();
		return template;
	}

	/**
	 * 默认库 stringRedisTemplate
	 */
	@Bean
	public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
		StringRedisTemplate template = new StringRedisTemplate();
		template.setConnectionFactory(redisConnectionFactory);
		template.afterPropertiesSet();
		return template;
	}

	/**
	 * 默认库  floatRedisTemplate
	 */
	@Bean
	public RedisTemplate<String, String> floatRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
		   RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
			redisTemplate.setConnectionFactory(redisConnectionFactory);
			redisTemplate.setValueSerializer(new GenericToStringSerializer<Float>(Float.class));
			redisTemplate.setKeySerializer(new StringRedisSerializer());
			redisTemplate.setHashKeySerializer(new StringRedisSerializer());
			redisTemplate.setHashValueSerializer(new GenericToStringSerializer<Float>(Float.class));
			redisTemplate.afterPropertiesSet();
		return redisTemplate;
	}











	/**
	 * FastJson反序列化
	 */
	@Bean
	GenericFastJsonRedisSerializer fastJsonRedisSerializer(){
		 return  new GenericFastJsonRedisSerializer();
	}

	/**
	 * 使用fastJson,生成不同库的，redisTemplate
	 */
	public  <K, V> RedisTemplate<K, V>  generateFastJsonRedisTemplate(RedisConnectionFactory redisConnectionFactory){
		GenericFastJsonRedisSerializer fastJsonRedisSerializer =fastJsonRedisSerializer();
		RedisTemplate<K, V> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(redisConnectionFactory);
		redisTemplate.setDefaultSerializer(fastJsonRedisSerializer);
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(fastJsonRedisSerializer);
		redisTemplate.setHashKeySerializer(new StringRedisSerializer());
		redisTemplate.setHashValueSerializer(fastJsonRedisSerializer);
		redisTemplate.afterPropertiesSet();
		return redisTemplate;
	}

	/**
	 * 使用fastJson,生成不同库的，StringRedisTemplate
	 */
	public   StringRedisTemplate  generateFastJsonStringRedisTemplate(RedisConnectionFactory redisConnectionFactory){
		StringRedisTemplate template = new StringRedisTemplate();
		template.setConnectionFactory(redisConnectionFactory);
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new StringRedisSerializer());
		template.afterPropertiesSet();
		return template;
	}

	/**
	 * 创建不同库连接工厂
	 */
	private LettuceConnectionFactory createLettuceConnectionFactory(Integer ysfDatabase) {
		LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder builder = LettucePoolingClientConfiguration.builder();
		RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
		GenericObjectPoolConfig genericObjectPoolConfig = new GenericObjectPoolConfig();
		redisStandaloneConfiguration.setHostName(host);
		redisStandaloneConfiguration.setPort(port);
		redisStandaloneConfiguration.setPassword(RedisPassword.of(password));
		redisStandaloneConfiguration.setDatabase(ysfDatabase);
		genericObjectPoolConfig.setMaxTotal(maxActive);
		genericObjectPoolConfig.setMaxIdle(maxIdle);
		genericObjectPoolConfig.setMinIdle(minIdle);
		genericObjectPoolConfig.setMaxWaitMillis(maxWait);
		builder.commandTimeout(Duration.ofMillis(timeout));
		builder.poolConfig(genericObjectPoolConfig);
		LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(redisStandaloneConfiguration, builder.build());
		connectionFactory.afterPropertiesSet();
		return connectionFactory;
	}











	/**
	 * 默认库  jsonRedisTemplate
	 */
	@Bean
	@DependsOn({ "redisTemplate", "stringRedisTemplate" })
	public <K, V> RedisTemplate<K, V> jsonRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
		return generateFastJsonRedisTemplate(redisConnectionFactory);
	}

	/**
	 * 默认库-jsonStringRedisTemplate
	 */
	@Bean
	@DependsOn({ "redisTemplate", "stringRedisTemplate" })
	public StringRedisTemplate jsonStringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
		return generateFastJsonStringRedisTemplate(redisConnectionFactory);
	}











	/**
	 * 云闪付-RedisTemplate
	 */
	@Bean
	@DependsOn({ "redisTemplate", "stringRedisTemplate" })
	public <K, V> RedisTemplate<K, V> ysfRedisTemplate() {
		return generateFastJsonRedisTemplate(getYsfRedisConFactory());
	}


	/**
	 * 云闪付-StringRedisTemplate
	 */
	@Bean
	@DependsOn({ "redisTemplate", "stringRedisTemplate" })
	public StringRedisTemplate ysfStringRedisTemplate() {
		return generateFastJsonStringRedisTemplate(getYsfRedisConFactory());
	}

	/**
	 * 云闪付Redis链接工厂
	 */
	private synchronized final LettuceConnectionFactory getYsfRedisConFactory() {
		if (this.ysfConnectionFactory == null) {
			this.ysfConnectionFactory = createLettuceConnectionFactory(ysfDatabase);
		}
		return ysfConnectionFactory;
	}










	/**
	 * 对账-RedisTemplate
	 */
	@Bean
	@DependsOn({ "redisTemplate", "stringRedisTemplate" })
	public <K, V> RedisTemplate<K, V> accountingRedisTemplate() {
		return generateFastJsonRedisTemplate(getAccountingConnectionFactory());

	}

	/**
	 * 对账-StringRedisTemplate
	 */
	@Bean
	@DependsOn({ "redisTemplate", "stringRedisTemplate" })
	public StringRedisTemplate accountingStringRedisTemplate() {
		return generateFastJsonStringRedisTemplate(getAccountingConnectionFactory());
	}


	/**
	 * 对账 链接工厂
	 */
	private synchronized final LettuceConnectionFactory getAccountingConnectionFactory() {
		if (this.accountingConnectionFactory == null) {
			this.accountingConnectionFactory = createLettuceConnectionFactory(accountingDatabase);
		}
		return accountingConnectionFactory;
	}











	/**
	 * Redisson连接
	 */
	@Bean(destroyMethod = "shutdown")
	//@ConditionalOnMissingBean(Redisson.class)
	public Redisson redisson(){
		Config config = null;
		Method clusterMethod = ReflectionUtils.findMethod(RedisProperties.class, "getCluster");
		Method timeoutMethod = ReflectionUtils.findMethod(RedisProperties.class, "getTimeout");
		Object timeoutValue = ReflectionUtils.invokeMethod(timeoutMethod, redisProperties);
		int timeout;
		if(null == timeoutValue){
			timeout = 0;
		}else if (!(timeoutValue instanceof Integer)) {
			Method millisMethod = ReflectionUtils.findMethod(timeoutValue.getClass(), "toMillis");
			timeout = ((Long) ReflectionUtils.invokeMethod(millisMethod, timeoutValue)).intValue();
		} else {
			timeout = (Integer)timeoutValue;
		}

		if (redisProperties.getSentinel() != null) {
			Method nodesMethod = ReflectionUtils.findMethod(RedisProperties.Sentinel.class, "getNodes");
			Object nodesValue = ReflectionUtils.invokeMethod(nodesMethod, redisProperties.getSentinel());

			String[] nodes;
			if (nodesValue instanceof String) {
				nodes = convert(Arrays.asList(((String)nodesValue).split(",")));
			} else {
				nodes = convert((List<String>)nodesValue);
			}

			config = new Config();
			config.useSentinelServers()
					.setMasterName(redisProperties.getSentinel().getMaster())
					.addSentinelAddress(nodes)
					.setDatabase(redisProperties.getDatabase())
					.setConnectTimeout(timeout)
					.setPassword(redisProperties.getPassword());
		} else if (clusterMethod != null && ReflectionUtils.invokeMethod(clusterMethod, redisProperties) != null) {
			Object clusterObject = ReflectionUtils.invokeMethod(clusterMethod, redisProperties);
			Method nodesMethod = ReflectionUtils.findMethod(clusterObject.getClass(), "getNodes");
			List<String> nodesObject = (List) ReflectionUtils.invokeMethod(nodesMethod, clusterObject);

			String[] nodes = convert(nodesObject);

			config = new Config();
			config.useClusterServers()
					.addNodeAddress(nodes)
					.setConnectTimeout(timeout)
					.setPassword(redisProperties.getPassword());
		} else {
			config = new Config();
			String prefix = "redis://";
			Method method = ReflectionUtils.findMethod(RedisProperties.class, "isSsl");
			if (method != null && (Boolean)ReflectionUtils.invokeMethod(method, redisProperties)) {
				prefix = "rediss://";
			}

			config.useSingleServer()
					.setAddress(prefix + redisProperties.getHost() + ":" + redisProperties.getPort())
					.setConnectTimeout(timeout)
					.setDatabase(redisProperties.getDatabase())
					.setPassword(redisProperties.getPassword());
		}

		return (Redisson)Redisson.create(config);
	}




	private String[] convert(List<String> nodesObject) {
		List<String> nodes = new ArrayList<String>(nodesObject.size());
		for (String node : nodesObject) {
			if (!node.startsWith("redis://") && !node.startsWith("rediss://")) {
				nodes.add("redis://" + node);
			} else {
				nodes.add(node);
			}
		}
		return nodes.toArray(new String[nodes.size()]);
	}












}
