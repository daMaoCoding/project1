package com.xinbo.fundstransfer.component.net.http.newpay;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;

import com.xinbo.fundstransfer.component.spring.SpringContextUtils;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import retrofit2.CallAdapter;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 * Created by Administrator on 2018/7/11. 新支付http客户端
 */
@Slf4j
public class HttpClient4NewPay {
	private OkHttpClient okHttpClient;
	private Converter.Factory converterFactory;
	private CallAdapter.Factory rxJavaCallAdapterFactory;
	private PlatformNewPayService platformNewPayService;
	private Environment environment;// 当前环境的application.properties的 配置
	private static volatile HttpClient4NewPay instance;

	private HttpClient4NewPay() {
		okHttpClient = new OkHttpClient().newBuilder().connectTimeout(30, TimeUnit.SECONDS)
				.readTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS).build();
		converterFactory = JacksonConverterFactory.create();
		rxJavaCallAdapterFactory = RxJavaCallAdapterFactory.create();
		environment = SpringContextUtils.getBean(Environment.class);
	}

	/**
	 * 获取平台相关业务接口API
	 *
	 * @return
	 */
	public PlatformNewPayService getPlatformNewPayServiceApi(final boolean feedback) {
		final String url = feedback ? environment.getProperty("feedback.platform.uri")
				: environment.getProperty("funds.transfer.uri");
		if (StringUtils.isBlank(url)) {
			log.info("env url is null please check ");
			return null;
		}
		Retrofit retrofit = new Retrofit.Builder().client(okHttpClient).baseUrl(url)
				.addConverterFactory(converterFactory).addCallAdapterFactory(rxJavaCallAdapterFactory).build();
		platformNewPayService = retrofit.create(PlatformNewPayService.class);
		return platformNewPayService;
	}

	public static HttpClient4NewPay getInstance() {
		if (instance == null) {
			synchronized (HttpClient4NewPay.class) {
				if (instance == null) {
					instance = new HttpClient4NewPay();
				}
			}
		}
		return instance;
	}

}
