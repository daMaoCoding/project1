package com.xinbo.fundstransfer.component.net.http.v3;

import com.xinbo.fundstransfer.domain.DeductAmountResponseData;
import com.xinbo.fundstransfer.domain.ResponseData;
import com.xinbo.fundstransfer.domain.SimpleResponseData;
import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Url;
import rx.Observable;

public interface RebateServiceApi {

	@POST("/api/daily/log")
	Observable<SimpleResponseData> rebateDaily(@Body RequestBody body);

	@POST("/api/log")
	Observable<SimpleResponseData> log(@Body RequestBody body);

	@POST("/api/withdrawal/ack")
	Observable<SimpleResponseData> withdrawalAck(@Body RequestBody body);

	@POST("/api/withdrawal/cancel")
	Observable<SimpleResponseData> withdrawalCancel(@Body RequestBody body);

	@POST("/api/limit/ack")
	Observable<SimpleResponseData> limitAck(@Body RequestBody body);

	@PATCH("/api/acc/feedback")
	Observable<SimpleResponseData> accFeedback(@Body RequestBody body);

	@GET
	Observable<ResponseData> getUserByUid(@Url String url, @Header("Authorization") String header);

	@PATCH
	Observable<SimpleResponseData> activation(@Url String url, @Body RequestBody body,
			@Header("Authorization") String header);

	@POST("/api/users/device")
	Observable<SimpleResponseData> usersDevice(@Body RequestBody body);

	@POST("/api/deduct")
	Observable<SimpleResponseData> deductAmount(@Body RequestBody body);

	@POST("/api/usestatus")
	Observable<SimpleResponseData> usestatus(@Body RequestBody body);

	@POST("/api/joinFlwEvent")
	Observable<SimpleResponseData> joinFlwEvent(@Body RequestBody body);

	@POST("/api/syncDoubleCreditEvent")
	Observable<SimpleResponseData> syncDoubleCreditEvent(@Body RequestBody body);

	@POST("/api/limit/cancel")
	Observable<SimpleResponseData> limitCancel(@Body RequestBody body);

	@POST("/api/wxzfb/records")
	Observable<SimpleResponseData> wxZfbLogs(@Body RequestBody body);

}
