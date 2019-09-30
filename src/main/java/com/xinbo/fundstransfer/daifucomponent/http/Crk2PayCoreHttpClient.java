package com.xinbo.fundstransfer.daifucomponent.http;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.xinbo.fundstransfer.component.net.http.api.IPlatformService;
import com.xinbo.fundstransfer.component.spring.SpringContextUtils;
import com.xinbo.fundstransfer.service.HandicapService;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import retrofit2.CallAdapter;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

@Slf4j
public class Crk2PayCoreHttpClient {
	
	
	private OkHttpClient okHttpClient;
	private Converter.Factory converterFactory;
	private CallAdapter.Factory rxJavaCallAdapterFactory;
	/** key为盘口主键ID */
	private Map<String, Crk2PayCoreService> platformServiceMap = new HashMap<String, Crk2PayCoreService>();
	HandicapService handicapService;
	IPlatformService platformService;

	private Crk2PayCoreHttpClient() {
		okHttpClient = new OkHttpClient().newBuilder().connectTimeout(25, TimeUnit.SECONDS)
				.readTimeout(25, TimeUnit.SECONDS).writeTimeout(25, TimeUnit.SECONDS).build();
		converterFactory = JacksonConverterFactory.create();
		rxJavaCallAdapterFactory = RxJavaCallAdapterFactory.create();
		handicapService = SpringContextUtils.getBean(HandicapService.class);
	}

	private static volatile Crk2PayCoreHttpClient instance;

	public static Crk2PayCoreHttpClient getInstance() {
		if (instance == null) {
			synchronized (Crk2PayCoreHttpClient.class) {
				if (instance == null) {
					instance = new Crk2PayCoreHttpClient();
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
	public Crk2PayCoreService getCrk2PayCoreApi(String handicap,String baseUrl) {
		log.info("获取盘口{}访问payCode的接口api,baseUrl ={}",handicap,baseUrl);
		if (platformServiceMap.get(handicap) == null) {
			Retrofit retrofit = new Retrofit.Builder().client(okHttpClient)
					.baseUrl(baseUrl)
					.addConverterFactory(converterFactory)
					.addCallAdapterFactory(rxJavaCallAdapterFactory).build();
			platformServiceMap.put(handicap, retrofit.create(Crk2PayCoreService.class));
			log.info("盘口编码：{}，访问payCode服务调用的接口的baseUrl = {}。", handicap,baseUrl);
		}
		return platformServiceMap.get(handicap);
	}

}
