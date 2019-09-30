package com.xinbo.fundstransfer.component.net.http;

import com.xinbo.fundstransfer.component.net.http.api.IPlatformService;

import okhttp3.OkHttpClient;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;

public class TestHttpClient {
	private OkHttpClient okHttpClient;
	private CallAdapter.Factory rxJavaCallAdapterFactory;

	IPlatformService platformService;

	private TestHttpClient() {
		okHttpClient = new OkHttpClient();
		rxJavaCallAdapterFactory = RxJavaCallAdapterFactory.create();
	}

	private static volatile TestHttpClient instance;

	public static TestHttpClient getInstance() {
		if (instance == null) {
			synchronized (TestHttpClient.class) {
				if (instance == null) {
					instance = new TestHttpClient();
				}
			}
		}
		return instance;
	}

	/**
	 * 测试用，获取平台相关业务接口API
	 * 
	 * @param handicap
	 * @return
	 */
	public IPlatformService getIPlatformService() {
		if (platformService == null) {
			Retrofit retrofit = new Retrofit.Builder().client(okHttpClient).baseUrl("http://119.9.124.151:9998")
					.addConverterFactory(StringConverterFactory.create())
					.addCallAdapterFactory(rxJavaCallAdapterFactory).build();
			platformService = retrofit.create(IPlatformService.class);
		}
		return platformService;
	}

}
