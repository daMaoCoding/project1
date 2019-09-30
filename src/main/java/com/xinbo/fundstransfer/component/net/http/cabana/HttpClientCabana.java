package com.xinbo.fundstransfer.component.net.http.cabana;

import com.xinbo.fundstransfer.AppProperties;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class HttpClientCabana {
	public static final Logger log = LoggerFactory.getLogger(HttpClientCabana.class);
	private OkHttpClient okHttpClient;
	private Converter.Factory converterFactory;
	private CallAdapter.Factory rxJavaCallAdapterFactory;
	private ICabanaService cabanaService;
	private Environment environment;
	private static volatile HttpClientCabana instance;
	private AppProperties appProperties;
	private List<ICabanaService> cabanaSerList = new ArrayList<ICabanaService>();

	private HttpClientCabana() {
		okHttpClient = new OkHttpClient().newBuilder().connectTimeout(6, TimeUnit.SECONDS)
				.readTimeout(6, TimeUnit.SECONDS).writeTimeout(6, TimeUnit.SECONDS).build();
		converterFactory = JacksonConverterFactory.create();
		rxJavaCallAdapterFactory = RxJavaCallAdapterFactory.create();
		environment = SpringContextUtils.getBean(Environment.class);
		appProperties = SpringContextUtils.getBean(AppProperties.class);
	}

	public ICabanaService getCabanaService() {
		if (cabanaService == null) {
			// 本地测试使用 ----"http://localhost:8081"
			// environment.getProperty("funds.transfer.cabanauri")
			Retrofit retrofit = new Retrofit.Builder().client(okHttpClient)
					.baseUrl(environment.getProperty("funds.transfer.cabanauri")).addConverterFactory(converterFactory)
					.addCallAdapterFactory(rxJavaCallAdapterFactory).build();
			cabanaService = retrofit.create(ICabanaService.class);
		}
		return cabanaService;
	}
	
	public List<ICabanaService> getCabanaServiceNew() {
		if (cabanaSerList == null || cabanaSerList.size() == 0) {
			List<String> cabanaUri = appProperties.getCabanauris();
			log.info("cabanaUris:{}", cabanaUri);
			for (int i = 0; i < cabanaUri.size(); i++) {
				log.info("cabanaUri:{}", cabanaUri.get(i));
				// 本地测试使用 ----"http://localhost:8081"
				// environment.getProperty("funds.transfer.cabanauri")
				Retrofit retrofit = new Retrofit.Builder().client(okHttpClient).baseUrl(cabanaUri.get(i))
						.addConverterFactory(converterFactory).addCallAdapterFactory(rxJavaCallAdapterFactory).build();
				cabanaSerList.add(retrofit.create(ICabanaService.class));
			}
		}
		return cabanaSerList;
	}

	public static HttpClientCabana getInstance() {
		if (instance == null) {
			synchronized (HttpClientCabana.class) {
				if (instance == null) {
					instance = new HttpClientCabana();
				}
			}
		}
		return instance;
	}
}
