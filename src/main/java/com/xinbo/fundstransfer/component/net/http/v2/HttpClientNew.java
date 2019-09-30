package com.xinbo.fundstransfer.component.net.http.v2;

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
 * 新系统接口
 *
 */
public class HttpClientNew {
	public static final Logger log = LoggerFactory.getLogger(HttpClientNew.class);
	private OkHttpClient okHttpClient;
	private Converter.Factory converterFactory;
	private CallAdapter.Factory rxJavaCallAdapterFactory;
	private PlatformServiceApi platformServiceApi;
	private Environment environment;// 当前环境的application.properties的 配置
	private static volatile HttpClientNew instance;

	private HttpClientNew() {
		okHttpClient = new OkHttpClient().newBuilder().connectTimeout(6, TimeUnit.SECONDS)
				.readTimeout(6, TimeUnit.SECONDS).writeTimeout(6, TimeUnit.SECONDS).build();
		converterFactory = JacksonConverterFactory.create();
		rxJavaCallAdapterFactory = RxJavaCallAdapterFactory.create();
		environment = SpringContextUtils.getBean(Environment.class);
	}

	/**
	 * 获取平台相关业务接口API
	 * 
	 * @return
	 */
	public PlatformServiceApi getPlatformServiceApi() {
		if (platformServiceApi == null) {
			Retrofit retrofit = new Retrofit.Builder().client(okHttpClient)
					.baseUrl(environment.getProperty("funds.transfer.uri")).addConverterFactory(converterFactory)
					.addCallAdapterFactory(rxJavaCallAdapterFactory).build();
			platformServiceApi = retrofit.create(PlatformServiceApi.class);
		}
		return platformServiceApi;
	}

	public static HttpClientNew getInstance() {
		if (instance == null) {
			synchronized (HttpClientNew.class) {
				if (instance == null) {
					instance = new HttpClientNew();
				}
			}
		}
		return instance;
	}

}
