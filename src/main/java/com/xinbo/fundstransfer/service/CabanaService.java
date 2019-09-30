package com.xinbo.fundstransfer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.xinbo.fundstransfer.domain.ResponseData;
import com.xinbo.fundstransfer.domain.pojo.CabanaStatus;
import okhttp3.RequestBody;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public interface CabanaService {

	ResponseData<List<CabanaStatus>> statusByAcc(List<String> accList);

	ResponseData<List<CabanaStatus>> status(List<Integer> idList);

	ResponseData<?> login(Integer accId);

	ResponseData<?> conciliate(Integer accId, String date);
	
	ResponseData<?> getCacheFlow(Integer accId, String date);

	ResponseData<?> conciliateINCR(Integer accId);

	ResponseData<?> reAck(Integer accId);

	ResponseData<?> logs(Integer accId, String date);

	ResponseData<?> updAcc(Integer accId);

	ResponseData<?> setModel(Integer accId, String model);

	ResponseData<?> inOutModel(Integer accId, String model);

	ResponseData<?> screen(Integer accId);

	ResponseData<?> error(Integer accId);

	ResponseData<?> version(String data,String mobiles,String version);

	ResponseData<?> versionList();

	ResponseData<?> hisSMS(String mobile);

	ResponseData<?> logLevel(String level) throws Exception;

	ResponseData<?> change(Integer fromId, Integer toId);

	ResponseData<List<CabanaStatus>> status4Error();

	RequestBody buildReqBody(String salt, TreeMap<String, String> params) throws JsonProcessingException;

	ResponseData<?> activeQuickPay(String rebateUser, String quickAcc, String quickPass,String mobile);

	ResponseData<?> refreshAcc(Integer accId);

	ResponseData<?> getLastVersion();

	ResponseData<?> appPatch(String patchVersion,String patchUrl);

	ResponseData<?> initQuickPay(Integer accId);

	ResponseData<?> forcedExit(String mobile);

}
