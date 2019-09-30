package com.xinbo.fundstransfer.component.net.http.cloud;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import com.xinbo.fundstransfer.component.spring.SpringContextUtils;

import okhttp3.OkHttpClient;
import retrofit2.CallAdapter;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 * 云端接口
 *
 */
public class HttpClientCloud {
	public static final Logger log = LoggerFactory.getLogger(HttpClientCloud.class);
	private OkHttpClient okHttpClient;
	private Converter.Factory converterFactory;
	private CallAdapter.Factory rxJavaCallAdapterFactory;
	private ICloudService cloudService;
	private Environment environment;
	private static volatile HttpClientCloud instance;

	private HttpClientCloud() {
		okHttpClient = new OkHttpClient().newBuilder().connectTimeout(6, TimeUnit.SECONDS)
				.readTimeout(6, TimeUnit.SECONDS).writeTimeout(6, TimeUnit.SECONDS).build();
		converterFactory = JacksonConverterFactory.create();
		rxJavaCallAdapterFactory = RxJavaCallAdapterFactory.create();
		environment = SpringContextUtils.getBean(Environment.class);
	}

	/**
	 * 获取平台相关业务接口API
	 * 
	 */
	public ICloudService getCloudService() {
		if (cloudService == null) {
			Retrofit retrofit = new Retrofit.Builder().client(okHttpClient)
					.baseUrl(environment.getProperty("funds.transfer.cloud")).addConverterFactory(converterFactory)
					.addCallAdapterFactory(rxJavaCallAdapterFactory).build();
			cloudService = retrofit.create(ICloudService.class);
		}
		return cloudService;
	}

	public static HttpClientCloud getInstance() {
		if (instance == null) {
			synchronized (HttpClientCloud.class) {
				if (instance == null) {
					instance = new HttpClientCloud();
				}
			}
		}
		return instance;
	}
	
}
