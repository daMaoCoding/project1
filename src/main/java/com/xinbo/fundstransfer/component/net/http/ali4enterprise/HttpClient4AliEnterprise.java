package com.xinbo.fundstransfer.component.net.http.ali4enterprise;

import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class HttpClient4AliEnterprise {
	private static OkHttpClient mOkHttpClient = new OkHttpClient();
	static {
		mOkHttpClient.newBuilder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS);
	}

	// 同步调用
	public static Response execute(Request request) throws IOException {
		if (mOkHttpClient == null) {
			mOkHttpClient = new OkHttpClient();
		}
		return mOkHttpClient.newCall(request).execute();
	}

	// 异步调用
	public static void enqueue(Request request, Callback responseCallback) {
		mOkHttpClient.newCall(request).enqueue(responseCallback);
	}

	// 异步调用 void 不返回结果;
	public static void enqueue(Request request) {
		mOkHttpClient.newCall(request).enqueue(new Callback() {

			@Override
			public void onFailure(Call call, IOException e) {

			}

			@Override
			public void onResponse(Call call, Response response) throws IOException {

			}
		});
	}

}
