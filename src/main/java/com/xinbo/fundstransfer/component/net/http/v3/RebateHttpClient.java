package com.xinbo.fundstransfer.component.net.http.v3;

import com.xinbo.fundstransfer.AppProperties;
import com.xinbo.fundstransfer.component.spring.SpringContextUtils;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.CallAdapter;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.concurrent.TimeUnit;

public class RebateHttpClient {
	public static final Logger log = LoggerFactory.getLogger(RebateHttpClient.class);
	private OkHttpClient okHttpClient;
	private Converter.Factory converterFactory;
	private CallAdapter.Factory rxJavaCallAdapterFactory;
	private RebateServiceApi pebateServiceApi;
	private static volatile RebateHttpClient instance;

	private RebateHttpClient() {
		okHttpClient = new OkHttpClient().newBuilder().connectTimeout(6, TimeUnit.SECONDS)
				.readTimeout(6, TimeUnit.SECONDS).writeTimeout(6, TimeUnit.SECONDS).build();
		converterFactory = JacksonConverterFactory.create();
		rxJavaCallAdapterFactory = RxJavaCallAdapterFactory.create();
	}

	public RebateServiceApi getPlatformServiceApi() {
		if (pebateServiceApi == null) {
			AppProperties appProperties = SpringContextUtils.getBean(AppProperties.class);
			Retrofit retrofit = new Retrofit.Builder().client(okHttpClient).baseUrl(appProperties.getRebateuri())
					.addConverterFactory(converterFactory).addCallAdapterFactory(rxJavaCallAdapterFactory).build();
			pebateServiceApi = retrofit.create(RebateServiceApi.class);
		}
		return pebateServiceApi;
	}

	public static RebateHttpClient getInstance() {
		if (instance == null) {
			synchronized (RebateHttpClient.class) {
				if (instance == null) {
					instance = new RebateHttpClient();
				}
			}
		}
		return instance;
	}
}
