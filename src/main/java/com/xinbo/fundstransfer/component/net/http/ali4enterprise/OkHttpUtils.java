package com.xinbo.fundstransfer.component.net.http.ali4enterprise;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Component
@Slf4j
public class OkHttpUtils {

	@Value(value = "${funds.transfer.uri}")
	private String baseUri;

	public OkHttpUtils() {

	}

	public String post(String url, String json) {
		try {
			RequestBody body = StringUtils.isNotBlank(json)
					? RequestBody.create(MediaType.parse("application/json"), json)
					: null;
			Request request = new Request.Builder().url(baseUri + url).post(body).build();
			Response response = HttpClient4AliEnterprise.execute(request);
			if (response != null && response.isSuccessful()) {
				return response.body().string();
			}
			return "";
		} catch (IOException e) {
			log.error("OkHttpUtils.post:", e);
			return e.getLocalizedMessage();
		}
	}
}
