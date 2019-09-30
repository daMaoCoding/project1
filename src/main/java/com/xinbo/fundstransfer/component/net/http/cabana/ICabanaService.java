package com.xinbo.fundstransfer.component.net.http.cabana;

import com.xinbo.fundstransfer.domain.ResponseData;
import com.xinbo.fundstransfer.domain.pojo.CabanaStatus;
import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import rx.Observable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface ICabanaService {

	@POST("/r/api/status")
	Observable<ResponseData<List<CabanaStatus>>> status(@Body RequestBody body);

	@POST("/r/api/status4Error")
	Observable<ResponseData<List<CabanaStatus>>> status4Error(@Body RequestBody body);

	@POST("/r/api/statusByAcc")
	Observable<ResponseData<List<CabanaStatus>>> statusByAcc(@Body RequestBody body);

	@POST("/r/api/login")
	Observable<ResponseData<String>> login(@Body RequestBody body);

	@POST("/r/api/conciliate")
	Observable<ResponseData<String>> conciliate(@Body RequestBody body);
	
	@POST("/r/api/getCacheFlow")
	Observable<ResponseData<String>> getCacheFlow(@Body RequestBody body);

	@POST("/r/api/conciliateINCR")
	Observable<ResponseData<String>> conciliateINCR(@Body RequestBody body);

	@POST("/r/api/reAck")
	Observable<ResponseData<String>> reAck(@Body RequestBody body);

	@POST("/r/api/logs")
	Observable<ResponseData<String>> logs(@Body RequestBody body);

	@POST("/r/api/updAcc")
	Observable<ResponseData<String>> updAcc(@Body RequestBody body);

	@POST("/r/api/model")
	Observable<ResponseData<String>> model(@Body RequestBody body);

	@POST("/r/api/inOutModel")
	Observable<ResponseData<String>> inOutModel(@Body RequestBody body);

	@POST("/r/api/screen")
	Observable<ResponseData<String>> screen(@Body RequestBody body);

	@POST("/r/api/error")
	Observable<ResponseData<String>> error(@Body RequestBody body);

	@POST("/r/api/version")
	Observable<ResponseData<String>> version(@Body RequestBody body);

	@POST("/r/api/versionList")
	Observable<ResponseData<Object>> versionList(@Body RequestBody body);

	@POST("/r/api/hisSMS")
	Observable<ResponseData<List<String>>> hisSMS(@Body RequestBody body);

	@POST("/loggers/com.example.cabana")
	Observable<String> logLevel(@Body RequestBody body);

	@GET("/loggers/com.example.cabana")
	Observable<HashMap> logLevel();

	@POST("/r/api/change")
	Observable<ResponseData<String>> change(@Body RequestBody body);

	/**
	 * 描述:出入款系统调用cabana再调用 云闪付APP生成收款二维码
	 * 
	 * @param body
	 * @return
	 */
	@POST("/cabana/call4GenerateQRs")
	Observable<ResponseData<Map>> call4GenerateQRs(@Body RequestBody body);

	@POST("/r/api/activeQuickPay")
	Observable<ResponseData<String>> activeQuickPay(@Body RequestBody body);

	@POST("/r/api/refreshAcc")
	Observable<ResponseData<String>> refreshAcc(@Body RequestBody body);

	@POST("/r/api/getVersionForFundTrans")
	Observable<ResponseData<String>> getLastVersion(@Body RequestBody body);

	@POST("/r/api/doPatch4App")
	Observable<ResponseData<String>> doPatch4App(@Body RequestBody body);
	
	@POST("/r/api/initQuickPay")
    Observable<ResponseData<String>> initQuickPay(@Body RequestBody body);

	@POST("/r/api/forcedExit")
	Observable<ResponseData<String>> forcedExit(@Body RequestBody body);

	@POST("/r/api/connectFromFundTrans")
	Observable<ResponseData<String>> testConnect(@Body RequestBody body);
}
