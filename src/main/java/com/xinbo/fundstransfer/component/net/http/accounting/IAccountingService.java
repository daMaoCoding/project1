package com.xinbo.fundstransfer.component.net.http.accounting;

import retrofit2.http.GET;
import retrofit2.http.QueryMap;
import rx.Observable;

import java.util.Map;

public interface IAccountingService {

	@GET("/accounting/sysLog/list")
	Observable<Object> status(@QueryMap Map<String, Object> params);
}
