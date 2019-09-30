package com.xinbo.fundstransfer.component.net.http;

import com.xinbo.fundstransfer.component.net.http.api.IPlatformService;
import com.xinbo.fundstransfer.component.net.http.api.ITestService;
import com.xinbo.fundstransfer.component.spring.SpringContextUtils;
import com.xinbo.fundstransfer.service.HandicapService;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import retrofit2.CallAdapter;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * HTTP工具类
 * 
 *
 *
 */
public class HttpClient {
	Logger log = LoggerFactory.getLogger(this.getClass());
	private OkHttpClient okHttpClient;
	private ITestService testService;
	private Converter.Factory converterFactory;
	private CallAdapter.Factory rxJavaCallAdapterFactory;
	private Environment environment;// 当前环境的application.properties的 配置
	/** key为盘口主键ID */
	private Map<Integer, IPlatformService> platformServiceMap = new HashMap<Integer, IPlatformService>();
	HandicapService handicapService;
	IPlatformService platformService;

	private HttpClient() {
		okHttpClient = new OkHttpClient().newBuilder().connectTimeout(25, TimeUnit.SECONDS)
				.readTimeout(25, TimeUnit.SECONDS).writeTimeout(25, TimeUnit.SECONDS).build();
		converterFactory = JacksonConverterFactory.create();
		rxJavaCallAdapterFactory = RxJavaCallAdapterFactory.create();
		environment = SpringContextUtils.getBean(Environment.class);
		handicapService = SpringContextUtils.getBean(HandicapService.class);
	}

	private static volatile HttpClient instance;

	public static HttpClient getInstance() {
		if (instance == null) {
			synchronized (HttpClient.class) {
				if (instance == null) {
					instance = new HttpClient();
				}
			}
		}
		return instance;
	}

	/**
	 * 获取平台相关业务接口API
	 * 
	 * @param handicap
	 * @return
	 */
	public IPlatformService getIPlatformService(Integer handicap) {
		if (platformServiceMap.get(handicap) == null) {
			String handicapCode = handicapService.findFromCacheById(handicap).getCode();
			Retrofit retrofit = new Retrofit.Builder().client(okHttpClient)
					.baseUrl(environment.getProperty("funds.transfer.url." + handicapCode))
					.addConverterFactory(StringConverterFactory.create())
					.addCallAdapterFactory(rxJavaCallAdapterFactory).build();
			platformServiceMap.put(handicap, retrofit.create(IPlatformService.class));
			log.info("根据盘口编码：{}，创建新的业务接口实例。", handicapCode);
		}
		return platformServiceMap.get(handicap);
	}

	/**
	 * 测试用，获取平台相关业务接口API
	 * 
	 * @param handicap
	 * @return
	 */
	public IPlatformService getIPlatformService() {
		if (platformService == null) {
			Retrofit retrofit = new Retrofit.Builder().client(okHttpClient).baseUrl("http://35.201.136.132:9998")
					.addConverterFactory(StringConverterFactory.create())
					.addCallAdapterFactory(rxJavaCallAdapterFactory).build();
			platformService = retrofit.create(IPlatformService.class);
		}
		return platformService;
	}

	/**
	 * 测试接口API
	 * 
	 * @return
	 */
	public ITestService getITestService() {
		if (testService == null) {
			Retrofit retrofit = new Retrofit.Builder().client(okHttpClient).baseUrl("http://localhost:8080")
					.addConverterFactory(StringConverterFactory.create())
					.addCallAdapterFactory(rxJavaCallAdapterFactory).build();
			testService = retrofit.create(ITestService.class);
		}
		return testService;
	}

}
