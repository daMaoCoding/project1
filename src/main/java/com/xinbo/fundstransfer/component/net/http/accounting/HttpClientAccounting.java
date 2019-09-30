package com.xinbo.fundstransfer.component.net.http.accounting;

import com.xinbo.fundstransfer.AppProperties;
import com.xinbo.fundstransfer.component.net.http.cabana.HttpClientCabana;
import com.xinbo.fundstransfer.component.spring.SpringContextUtils;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import retrofit2.CallAdapter;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.concurrent.TimeUnit;

public class HttpClientAccounting {
	public static final Logger logger = LoggerFactory.getLogger(HttpClientAccounting.class);
	private OkHttpClient okHttpClient;
	private Converter.Factory converterFactory;
	private CallAdapter.Factory rxJavaCallAdapterFactory;
	private IAccountingService accountingService;
	private Environment environment;
	private static volatile HttpClientAccounting instance;
	private AppProperties appProperties;

	private HttpClientAccounting() {
		okHttpClient = new OkHttpClient().newBuilder().connectTimeout(6, TimeUnit.SECONDS)
				.readTimeout(6, TimeUnit.SECONDS).writeTimeout(6, TimeUnit.SECONDS).build();
		converterFactory = JacksonConverterFactory.create();
		rxJavaCallAdapterFactory = RxJavaCallAdapterFactory.create();
		environment = SpringContextUtils.getBean(Environment.class);
		appProperties = SpringContextUtils.getBean(AppProperties.class);
	}

	public IAccountingService getAccountingService() {
		if (accountingService == null) {
			Retrofit retrofit = new Retrofit.Builder().client(okHttpClient)
					.baseUrl(environment.getProperty("funds.transfer.accounting.uri"))
					.addConverterFactory(converterFactory).addCallAdapterFactory(rxJavaCallAdapterFactory).build();
			accountingService = retrofit.create(IAccountingService.class);
		}
		return accountingService;
	}

	public static HttpClientAccounting getInstance() {
		if (instance == null) {
			synchronized (HttpClientCabana.class) {
				if (instance == null) {
					instance = new HttpClientAccounting();
				}
			}
		}
		return instance;
	}
}
