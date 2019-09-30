package com.xinbo.fundstransfer.component.net.http.api;

import com.xinbo.fundstransfer.domain.SimpleResponseData;

import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;
import rx.Observable;

public interface ITestService {

	/**
	 * 一个用于测试的接口
	 * 
	 * @param username
	 * @return
	 */
	@POST("/r/test/greeting")
	Observable<SimpleResponseData> greeting(@Query("username") String username);

	/**
	 * 模拟测试入款请求 usercode 会员编码
	 */
	@POST("/api/income/put")
	Observable<SimpleResponseData> income(@Query("level") String level, @Query("handicap") String handicap,
			@Query("amount") float amount, @Query("remark") String remark, @Query("order_no") String order_no,
			@Query("type") int type, @Query("time") String time, @Query("from_account") String from_account,
			@Query("to_account") String to_account, @Query("usercode") int usercode, @Query("username") String username,
			@Query("realname") String realname, @Query("payment_code") String payment_code,
			@Query("token") String token);

	/**
	 * 模拟测试出款请求
	 */
	@POST("/api/outward/put")
	Observable<SimpleResponseData> outward(@Query("level") String level, @Query("handicap") String handicap,
			@Query("amount") float amount, @Query("remark") String remark, @Query("order_no") String order_no,
			@Query("time") String time, @Query("account_owner") String account_owner,
			@Query("account_name") String account_name, @Query("account") String account,
			@Query("username") String username, @Query("realname") String realname, @Query("token") String token,
			@Query("usercode") int usercode, @Query("account_bank") String account_bank);

	/**
	 * 模拟测试
	 */
	@POST("/api/outward/task/get")
	Observable<SimpleResponseData> task(@Query("amount") float amount, @Query("transaction_no") String transaction_no,
			@Query("payment_code") String payment_code, @Query("time") long time,
			@Query("from_account") String from_account, @Query("to_account") String to_account, @Query("type") int type,
			@Query("token") String token);

	/**
	 * 模拟测试
	 */
	@POST("/api/level/put")
	Observable<String> apilevel(@Body RequestBody body);

	/**
	 * 模拟测试
	 */
	@POST("/api/account/put")
	Observable<String> apiaccount(@Body RequestBody body);

	/**
	 * 模拟测试
	 */
	@POST("/api/income/put")
	Observable<String> apiincome(@Body RequestBody body);

	/**
	 * 模拟测试
	 */
	@POST("/api/outward/put")
	Observable<String> apioutward(@Body RequestBody body);

}
