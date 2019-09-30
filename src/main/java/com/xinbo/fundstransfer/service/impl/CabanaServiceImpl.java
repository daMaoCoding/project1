package com.xinbo.fundstransfer.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.component.net.http.cabana.HttpClientCabana;
import com.xinbo.fundstransfer.component.net.http.cabana.ICabanaService;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.ResponseData;
import com.xinbo.fundstransfer.domain.pojo.Account;
import com.xinbo.fundstransfer.domain.pojo.CabanaStatus;
import com.xinbo.fundstransfer.restful.v2.TokenValidation;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.CabanaService;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Component
public class CabanaServiceImpl extends TokenValidation implements CabanaService {
	private static final Logger log = LoggerFactory.getLogger(CabanaServiceImpl.class);
	private ObjectMapper mapper = new ObjectMapper();
	private static Timer timer = new Timer();
	private static final String CONSTANT_TOKEN_VARIABLE = "token";
	private static final String CONSTANT_HTTP_MEDIA_TYPE = "application/json";
	@Value("${funds.transfer.cabanasalt}")
	private String cabanasalt;
	@Autowired @Lazy
	private AccountService accSer;
	private ExecutorService executor = Executors.newFixedThreadPool(40);

	@Override
	@SuppressWarnings("unchecked")
	public ResponseData<List<CabanaStatus>> statusByAcc(List<String> accList) {
		log.debug(">> CabanaServiceImpl statusByAcc param:{}", accList);
		if (CollectionUtils.isEmpty(accList)) {
			log.trace("Cabana statusByAcc( {} ) >> the param (accountList) is empty. ");
			return new ResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数为空.");
		}
		try {
			ResponseData<List<CabanaStatus>>[] ret = new ResponseData[1];
			List<ICabanaService> cabanaList = HttpClientCabana.getInstance().getCabanaServiceNew();
			CountDownLatch latch = new CountDownLatch(cabanaList.size());
			for (ICabanaService cabanaService : cabanaList) {
				executor.submit(() -> cabanaService
						.statusByAcc(buildReqBody(cabanasalt, new TreeMap<String, String>(Comparator.naturalOrder()) {
							{
								put("accountList", String.join(",",
										accList.stream().map(String::valueOf).collect(Collectors.toList())));
							}
						})).subscribe(d -> {
							if (d.getStatus() != GeneralResponseData.ResponseStatus.SUCCESS.getValue())
								log.info("Cabana statusByAcc( {} ) >> return error. {}",
										String.join(",",
												accList.stream().map(String::valueOf).collect(Collectors.toList())),
										d.getMessage());
							log.debug(">> CabanaServiceImpl statusByAcc result:{}", d);
							ret[0] = d;
							latch.countDown();
						}, e -> {
							log.error("Cabana statusByAcc( {} ) >> process error. ", String.join(",",
									accList.stream().map(String::valueOf).collect(Collectors.toList())), e);
							ret[0] = new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请求异常");
							latch.countDown();
						}));
			}
			latch.await();
			return ret[0];
		} catch (Exception e) {
			log.error("Cabana statusByAcc( {} ) >> finally error. ",
					String.join(",", accList.stream().map(String::valueOf).collect(Collectors.toList())), e);
			return new ResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "处理异常");
		}
	}

	@Override
	@SuppressWarnings("all")
	public ResponseData<List<CabanaStatus>> status(List<Integer> accountList) {
		log.debug(">> CabanaServiceImpl status param:{}", accountList);
		if (CollectionUtils.isEmpty(accountList)) {
			log.trace("Cabana Status( {} ) >> the param (accountList) is empty. ");
			return new ResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数为空.");
		}
		try {
			List<ICabanaService> cabanaList = HttpClientCabana.getInstance().getCabanaServiceNew();
			ResponseData<List<CabanaStatus>>[] ret = new ResponseData[cabanaList.size()];
			CountDownLatch latch = new CountDownLatch(cabanaList.size());
			for (int i = 0; i < cabanaList.size(); i++) {
				ICabanaService cabanaService = cabanaList.get(i);
				executor.submit(() -> cabanaService
						.status(buildReqBody(cabanasalt, new TreeMap<String, String>(Comparator.naturalOrder()) {
							{
								put("accountList", String.join(",",
										accountList.stream().map(String::valueOf).collect(Collectors.toList())));
							}
						})).subscribe(d -> {
							if (d.getStatus() != GeneralResponseData.ResponseStatus.SUCCESS.getValue())
								log.info("Cabana Status( {} ) >> return error. {}",
										String.join(",",
												accountList.stream().map(String::valueOf).collect(Collectors.toList())),
										d.getMessage());
							if(ret[0] == null || ret[0].getData() == null) {
								ret[0] = d;
							}else {
								if(d.getData() != null) {
									ret[0].getData().addAll(d.getData());
									log.debug(">> cabanaStatus status:{},status:{}", d.getData());
								}
							}
							latch.countDown();
						}, e -> {
							log.error("Cabana Status( {} ) >> process error. ", String.join(",",
									accountList.stream().map(String::valueOf).collect(Collectors.toList())), e);
							ret[0] = new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请求异常");
							latch.countDown();
						}));
			}
			latch.await();
			return ret[0];
		} catch (Exception e) {
			log.error("Cabana Status( {} ) >> finally error. ",
					String.join(",", accountList.stream().map(String::valueOf).collect(Collectors.toList())), e);
			return new ResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "处理异常");
		}
	}

	@Override
	public ResponseData<?> login(Integer accId) {
		log.debug(">> CabanaServiceImpl login param:{}", accId);
		if (Objects.isNull(accId)) {
			log.info("Cabana login( {} ) >> the param (accId) is empty. ");
			return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数为空.");
		}
		try {
			ResponseData<?>[] ret = new ResponseData<?>[1];
			List<ICabanaService> cabanaList = HttpClientCabana.getInstance().getCabanaServiceNew();
			CountDownLatch latch = new CountDownLatch(cabanaList.size());
			for (ICabanaService cabanaService : cabanaList) {
				executor.submit(() -> cabanaService
						.login(buildReqBody(cabanasalt, new TreeMap<String, String>(Comparator.naturalOrder()) {
							{
								put("accId", accId.toString());
							}
						})).subscribe(d -> {
							if (d.getStatus() != GeneralResponseData.ResponseStatus.SUCCESS.getValue())
								log.info("Cabana login( {} ) >> return error. ", accId, d.getMessage());
							log.debug(">> CabanaServiceImpl login result:{}", d);
							ret[0] = d;
							latch.countDown();
						}, e -> {
							log.error("Cabana login( {} ) >> process error. ", accId, e);
							ret[0] = new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请求异常");
							latch.countDown();
						}));
			}
			latch.await();
			return ret[0];
		} catch (Exception e) {
			log.error("Cabana Status( {} ) >> finally error. ", accId, e);
			return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "处理异常");
		}
	}

	@Override
	public ResponseData<?> conciliate(Integer accId, String date) {
		log.debug(">> CabanaServiceImpl conciliate param1:{},param2:{}", accId, date);
		if (Objects.isNull(accId)) {
			log.info("Cabana conciliate( {} ) >> the param (accId) is empty. ");
			return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数为空.");
		}
		try {
			ResponseData<?>[] ret = new ResponseData<?>[1];
			List<ICabanaService> cabanaList = HttpClientCabana.getInstance().getCabanaServiceNew();
			CountDownLatch latch = new CountDownLatch(cabanaList.size());
			for (ICabanaService cabanaService : cabanaList) {
				executor.submit(() -> cabanaService
						.conciliate(buildReqBody(cabanasalt, new TreeMap<String, String>(Comparator.naturalOrder()) {
							{
								put("accId", accId.toString());
								put("date", date.toString());
							}
						})).subscribe(d -> {
							if (d.getStatus() != GeneralResponseData.ResponseStatus.SUCCESS.getValue())
								log.info("Cabana conciliate( {} ) >> return error. {}", accId, d.getMessage());
							log.debug(">> CabanaServiceImpl conciliate result:{}", d);
							ret[0] = d;
							latch.countDown();
						}, e -> {
							log.error("Cabana conciliate( {} ) >> process error. ", accId, e);
							ret[0] = new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请求异常");
							latch.countDown();
						}));
			}
			latch.await();
			return ret[0];
		} catch (Exception e) {
			log.error("Cabana conciliate( {} ) >> finally error. ", accId, e);
			return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "处理异常");
		}
	}
	
	@Override
	public ResponseData<?> getCacheFlow(Integer accId, String date) {
		log.debug(">> CabanaServiceImpl getCacheFlow param1:{},param2:{}", accId, date);
		if (Objects.isNull(accId)) {
			log.info("Cabana getCacheFlow( {} ) >> the param (accId) is empty. ");
			return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数为空.");
		}
		try {
			ResponseData<?>[] ret = new ResponseData<?>[1];
			List<ICabanaService> cabanaList = HttpClientCabana.getInstance().getCabanaServiceNew();
			CountDownLatch latch = new CountDownLatch(cabanaList.size());
			for (ICabanaService cabanaService : cabanaList) {
				executor.submit(() -> cabanaService
						.getCacheFlow(buildReqBody(cabanasalt, new TreeMap<String, String>(Comparator.naturalOrder()) {
							{
								put("accId", accId.toString());
								put("date", date.toString());
							}
						})).subscribe(d -> {
							if (d.getStatus() != GeneralResponseData.ResponseStatus.SUCCESS.getValue())
								log.info("Cabana getCacheFlow( {} ) >> return error. {}", accId, d.getMessage());
							log.debug(">> CabanaServiceImpl getCacheFlow result:{}", d);
							ret[0] = d;
							latch.countDown();
						}, e -> {
							log.error("Cabana getCacheFlow( {} ) >> process error. ", accId, e);
							ret[0] = new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请求异常");
							latch.countDown();
						}));
			}
			latch.await();
			return ret[0];
		} catch (Exception e) {
			log.error("Cabana getCacheFlow( {} ) >> finally error. ", accId, e);
			return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "处理异常");
		}
	}

	@Override
	public ResponseData<?> conciliateINCR(Integer accId) {
		log.debug(">> CabanaServiceImpl conciliateINCR param1:{}", accId);
		if (Objects.isNull(accId)) {
			log.info("Cabana conciliateINCR( {} ) >> the param (accId) is empty. ");
			return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数为空.");
		}
		try {
			ResponseData<?>[] ret = new ResponseData<?>[1];
			List<ICabanaService> cabanaList = HttpClientCabana.getInstance().getCabanaServiceNew();
			CountDownLatch latch = new CountDownLatch(cabanaList.size());
			for (ICabanaService cabanaService : cabanaList) {
				executor.submit(() -> cabanaService.conciliateINCR(
						buildReqBody(cabanasalt, new TreeMap<String, String>(Comparator.naturalOrder()) {
							{
								put("accId", accId.toString());
							}
						})).subscribe(d -> {
							if (d.getStatus() != GeneralResponseData.ResponseStatus.SUCCESS.getValue())
								log.info("Cabana conciliateINCR( {} ) >> return error. {}", accId, d.getMessage());
							log.debug(">> CabanaServiceImpl conciliateINCR result:{}", d);
							ret[0] = d;
							latch.countDown();
						}, e -> {
							log.error("Cabana conciliateINCR( {} ) >> process error. ", accId, e);
							ret[0] = new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请求异常");
							latch.countDown();
						}));
			}
			latch.await();
			return ret[0];
		} catch (Exception e) {
			log.error("Cabana conciliateINCR( {} ) >> finally error. ", accId, e);
			return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "处理异常");
		}
	}

	@Override
	public ResponseData<?> reAck(Integer accId) {
		log.debug(">> CabanaServiceImpl reAck param1:{}", accId);
		if (Objects.isNull(accId)) {
			log.info("Cabana reAck( {} ) >> the param (accId) is empty. ");
			return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数为空.");
		}
		try {
			ResponseData<?>[] ret = new ResponseData<?>[1];
			List<ICabanaService> cabanaList = HttpClientCabana.getInstance().getCabanaServiceNew();
			CountDownLatch latch = new CountDownLatch(cabanaList.size());
			for (ICabanaService cabanaService : cabanaList) {
				executor.submit(() -> cabanaService
						.reAck(buildReqBody(cabanasalt, new TreeMap<String, String>(Comparator.naturalOrder()) {
							{
								put("accId", accId.toString());
							}
						})).subscribe(d -> {
							if (d.getStatus() != GeneralResponseData.ResponseStatus.SUCCESS.getValue())
								log.info("Cabana reAck( {} ) >> return error. {}", accId, d.getMessage());
							log.debug(">> CabanaServiceImpl reAck result:{}", d);
							ret[0] = d;
							latch.countDown();
						}, e -> {
							log.error("Cabana reAck( {} ) >> process error. ", accId, e);
							ret[0] = new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请求异常");
							latch.countDown();
						}));
			}
			latch.await();
			return ret[0];
		} catch (Exception e) {
			log.error("Cabana reAck( {} ) >> finally error. ", accId, e);
			return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "处理异常");
		}
	}

	@Override
	public ResponseData<?> logs(Integer accId, String date) {
		log.debug(">> CabanaServiceImpl logs param1:{},param2:{}", accId, date);
		if (Objects.isNull(accId)) {
			log.info("Cabana logs( {} ) >> the param (accId) is empty. ");
			return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数为空.");
		}
		try {
			ResponseData<?>[] ret = new ResponseData<?>[1];
			List<ICabanaService> cabanaList = HttpClientCabana.getInstance().getCabanaServiceNew();
			CountDownLatch latch = new CountDownLatch(cabanaList.size());
			for (ICabanaService cabanaService : cabanaList) {
				executor.submit(() -> cabanaService
					.logs(buildReqBody(cabanasalt, new TreeMap<String, String>(Comparator.naturalOrder()) {
						{
							put("accId", accId.toString());
							put("date", date);
						}
					})).subscribe(d -> {
						if (d.getStatus() != GeneralResponseData.ResponseStatus.SUCCESS.getValue())
							log.info("Cabana logs( {} ) >> return error. {}", accId, d.getMessage());
						log.debug(">> CabanaServiceImpl logs result:{}", d);
						if(d.getMessage().equals("操作成功")) {
							ret[0] = d;
						}
						if(ret[0] == null) {
							ret[0] = d;
						}
						latch.countDown();
					}, e -> {
						log.error("Cabana logs( {} ) >> process error. ", accId, e);
						ret[0] = new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请求异常");
						latch.countDown();
					}));
			}
			latch.await();
			return ret[0];
		} catch (Exception e) {
			log.error("Cabana logs( {} ) >> finally error. ", accId, e);
			return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "处理异常");
		}
	}

	@Override
	public ResponseData<?> updAcc(Integer accId) {
		timer.schedule(new TimerTask() {
			public void run() {
				try {
					refreshAcc(accId);
				} catch (Exception e) {
				}
			}
		}, 1000);
		return new ResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功");
	}

	@Override
	public ResponseData<?> setModel(Integer accId, String model) {
		log.debug(">> CabanaServiceImpl setModel param1:{},param2:{}", accId, model);
		if (Objects.isNull(accId) || Objects.isNull(model)) {
			log.info("Cabana setModel( {} ) >> the param (accId | model) is empty. ");
			return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数为空.");
		}
		try {
			ResponseData<?>[] ret = new ResponseData<?>[1];
			List<ICabanaService> cabanaList = HttpClientCabana.getInstance().getCabanaServiceNew();
			CountDownLatch latch = new CountDownLatch(cabanaList.size());
			for (ICabanaService cabanaService : cabanaList) {
				executor.submit(() -> cabanaService
						.model(buildReqBody(cabanasalt, new TreeMap<String, String>(Comparator.naturalOrder()) {
							{
								put("accId", accId.toString());
								put("model", model);
							}
						})).subscribe(d -> {
							if (d.getStatus() != GeneralResponseData.ResponseStatus.SUCCESS.getValue())
								log.info("Cabana setModel( {},{} ) >> return error. {}", accId, model, d.getMessage());
							log.debug(">> CabanaServiceImpl setModel result:{}", d);
							ret[0] = d;
							latch.countDown();
						}, e -> {
							log.error("Cabana setModel( {},{} ) >> process error. ", accId, model, e);
							ret[0] = new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请求异常");
							latch.countDown();
						}));
			}
			latch.await();
			return ret[0];
		} catch (Exception e) {
			log.error("Cabana setModel( {} ,{}) >> finally error. ", accId, model, e);
			return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "处理异常");
		}
	}

	@Override
	public ResponseData<?> inOutModel(Integer accId, String model) {
		log.debug(">> CabanaServiceImpl inOutModel param1:{},param2:{}", accId, model);
		if (Objects.isNull(accId) || Objects.isNull(model)) {
			log.info("Cabana setModel( {} ) >> the param (accId | model) is empty. ");
			return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数为空.");
		}
		try {
			ResponseData<?>[] ret = new ResponseData<?>[1];
			List<ICabanaService> cabanaList = HttpClientCabana.getInstance().getCabanaServiceNew();
			CountDownLatch latch = new CountDownLatch(cabanaList.size());
			for (ICabanaService cabanaService : cabanaList) {
				executor.submit(() -> cabanaService
						.inOutModel(buildReqBody(cabanasalt, new TreeMap<String, String>(Comparator.naturalOrder()) {
							{
								put("accId", accId.toString());
								put("model", model);
							}
						})).subscribe(d -> {
							if (d.getStatus() != GeneralResponseData.ResponseStatus.SUCCESS.getValue())
								log.info("Cabana inOutModel( {},{} ) >> return error. {}", accId, model, d.getMessage());
							log.debug(">> CabanaServiceImpl inOutModel result:{}", d);
							ret[0] = d;
							latch.countDown();
						}, e -> {
							log.error("Cabana setModel( {},{} ) >> process error. ", accId, model, e);
							ret[0] = new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请求异常");
							latch.countDown();
						}));
			}
			latch.await();
			return ret[0];
		} catch (Exception e) {
			log.error("Cabana setModel( {} ,{}) >> finally error. ", accId, model, e);
			return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "处理异常");
		}
	}

	@Override
	public ResponseData<?> screen(Integer accId) {
		log.debug(">> CabanaServiceImpl screen param1:{}", accId);
		if (Objects.isNull(accId)) {
			log.info("Cabana screen( {} ) >> the param (accId) is empty. ");
			return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数为空.");
		}
		try {
			ResponseData<?>[] ret = new ResponseData<?>[1];
			List<ICabanaService> cabanaList = HttpClientCabana.getInstance().getCabanaServiceNew();
			CountDownLatch latch = new CountDownLatch(cabanaList.size());
			for (ICabanaService cabanaService : cabanaList) {
				executor.submit(() -> cabanaService
						.screen(buildReqBody(cabanasalt, new TreeMap<String, String>(Comparator.naturalOrder()) {
							{
								put("accId", accId.toString());
							}
						})).subscribe(d -> {
							if (d.getStatus() != GeneralResponseData.ResponseStatus.SUCCESS.getValue())
								log.info("Cabana screen( {} ) >> return error. {}", accId, d.getMessage());
							log.debug(">> CabanaServiceImpl screen result:{}", d);
							if(d.getMessage().equals("操作成功")) {
								ret[0] = d;
							}
							if(ret[0] == null) {
								ret[0] = d;
							}
							latch.countDown();
						}, e -> {
							log.error("Cabana screen( {} ) >> process error. ", accId, e);
							ret[0] = new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请求异常");
							latch.countDown();
						}));
			}
			latch.await();
			return ret[0];
		} catch (Exception e) {
			log.error("Cabana screen( {} ) >> finally error. ", accId, e);
			return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "处理异常");
		}
	}

	@Override
	public ResponseData<?> error(Integer accId) {
		log.debug(">> CabanaServiceImpl error param1:{}", accId);
		if (Objects.isNull(accId)) {
			log.info("Cabana error( {} ) >> the param (accId) is empty. ");
			return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数为空.");
		}
		try {
			ResponseData<?>[] ret = new ResponseData<?>[1];
			List<ICabanaService> cabanaList = HttpClientCabana.getInstance().getCabanaServiceNew();
			CountDownLatch latch = new CountDownLatch(cabanaList.size());
			for (ICabanaService cabanaService : cabanaList) {
				executor.submit(() -> cabanaService
						.error(buildReqBody(cabanasalt, new TreeMap<String, String>(Comparator.naturalOrder()) {
							{
								put("accId", accId.toString());
							}
						})).subscribe(d -> {
							if (d.getStatus() != GeneralResponseData.ResponseStatus.SUCCESS.getValue())
								log.info("Cabana error( {} ) >> return error. {}", accId, d.getMessage());
							log.debug(">> CabanaServiceImpl error result:{}", d);
							ret[0] = d;
							latch.countDown();
						}, e -> {
							log.error("Cabana error( {} ) >> process error. ", accId, e);
							ret[0] = new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请求异常");
							latch.countDown();
						}));
			}
			latch.await();
			return ret[0];
		} catch (Exception e) {
			log.error("Cabana error( {} ) >> finally error. ", accId, e);
			return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "处理异常");
		}
	}

	@Override
	public ResponseData<?> version(String data,String mobiles,String version) {
		log.debug(">> CabanaServiceImpl version param1:{},param2:{},param3:{}", data, mobiles, version);
		try {
			ResponseData<?>[] ret = new ResponseData<?>[1];
			List<ICabanaService> cabanaList = HttpClientCabana.getInstance().getCabanaServiceNew();
			CountDownLatch latch = new CountDownLatch(cabanaList.size());
			for (ICabanaService cabanaService : cabanaList) {
				executor.submit(() -> cabanaService
						.version(buildReqBody(cabanasalt, new TreeMap<String, String>(Comparator.naturalOrder()) {
							{
								put("data", data);
								put("mobiles", mobiles);
								put("version", version);
							}
						})).subscribe(d -> {
							if (d.getStatus() != GeneralResponseData.ResponseStatus.SUCCESS.getValue())
								log.info("Cabana version() >> return error. {}", d.getMessage());
							log.debug(">> CabanaServiceImpl version result:{}", d);
							ret[0] = d;
							latch.countDown();
						}, e -> {
							log.error("Cabana version() >> process error. ", e);
							ret[0] = new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请求异常");
							latch.countDown();
						}));
			}
			latch.await();
			return ret[0];
		} catch (Exception e) {
			log.error("Cabana version() >> finally error. ", e);
			return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "处理异常");
		}
	}

	@Override
	public ResponseData<?> versionList() {
		try {
			ResponseData<?>[] ret = new ResponseData<?>[1];
			List<ICabanaService> cabanaList = HttpClientCabana.getInstance().getCabanaServiceNew();
			CountDownLatch latch = new CountDownLatch(cabanaList.size());
			for (ICabanaService cabanaService : cabanaList) {
				executor.submit(() -> cabanaService
						.versionList(buildReqBody(cabanasalt, new TreeMap<>(Comparator.naturalOrder())))
						.subscribe(d -> {
							if (d.getStatus() != GeneralResponseData.ResponseStatus.SUCCESS.getValue())
								log.info("Cabana versionList() >> return error. {}", d.getMessage());
							log.debug(">> CabanaServiceImpl versionList result:{}", d);
							ret[0] = d;
							latch.countDown();
						}, e -> {
							log.error("Cabana versionList() >> process error. ", e);
							ret[0] = new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请求异常");
							latch.countDown();
						}));
			}
			latch.await();
			return ret[0];
		} catch (Exception e) {
			log.error("Cabana versionList() >> finally error. ", e);
			return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "处理异常");
		}
	}

	@Override
	public ResponseData<?> hisSMS(String mobile) {
		log.debug(">> CabanaServiceImpl hisSMS param1:{}", mobile);
		if (Objects.isNull(mobile)) {
			log.info("Cabana hisSMS( {} ) >> the param (mobile) is empty. ");
			return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数为空.");
		}
		try {
			ResponseData<?>[] ret = new ResponseData<?>[1];
			List<ICabanaService> cabanaList = HttpClientCabana.getInstance().getCabanaServiceNew();
			CountDownLatch latch = new CountDownLatch(cabanaList.size());
			for (ICabanaService cabanaService : cabanaList) {
				executor.submit(() -> cabanaService
						.hisSMS(buildReqBody(cabanasalt, new TreeMap<String, String>(Comparator.naturalOrder()) {
							{
								put("mobile", mobile);
							}
						})).subscribe(d -> {
							if (d.getStatus() != GeneralResponseData.ResponseStatus.SUCCESS.getValue())
								log.info("Cabana hisSMS( {} ) >> return error. {}", mobile, d.getMessage());
							log.debug(">> CabanaServiceImpl hisSMS result:{}", d);
							ret[0] = d;
							latch.countDown();
						}, e -> {
							log.error("Cabana hisSMS( {} ) >> process error. ", mobile, e);
							ret[0] = new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请求异常");
							latch.countDown();
						}));
			}
			latch.await();
			return ret[0];
		} catch (Exception e) {
			log.error("Cabana hisSMS( {} ) >> finally error. ", mobile, e);
			return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "处理异常");
		}
	}

	@Override
	public ResponseData<?> logLevel(String level) throws Exception {
		log.debug(">> CabanaServiceImpl logLevel param1:{}", level);
		Exception[] exp = new Exception[1];
		if (Objects.nonNull(level) && "TRACE,DEBUG,INFO,WARN,ERROR".contains(level)) {
			HttpClientCabana.getInstance().getCabanaService()
					.logLevel(RequestBody.create(MediaType.parse("application/json"),
							"{\"configuredLevel\": \"" + level + "\"}"))
					.subscribe(d -> log.info("Log level setting is successful."), e -> {
						if (!(e instanceof JsonMappingException))
							exp[0] = (Exception) e;
					});
		}
		if (Objects.nonNull(exp[0]))
			throw exp[0];
		HashMap[] ret = new HashMap[1];
		HttpClientCabana.getInstance().getCabanaService().logLevel().subscribe(d -> ret[0] = d,
				e -> exp[0] = (Exception) e);
		if (Objects.nonNull(exp[0]))
			throw exp[0];
		ResponseData res = new ResponseData<HashMap>(GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		res.setData(ret[0]);
		return res;
	}

	@Override
	public ResponseData<?> change(Integer fromId, Integer toId) {
		log.debug(">> CabanaServiceImpl change param1:{},param2:{}", fromId, toId);
		if (Objects.isNull(fromId) || Objects.isNull(toId)) {
			log.info("Cabana change( {} {} ) >> the param (accId or toId ) is empty. ");
			return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数为空.");
		}
		try {
			ResponseData<?>[] ret = new ResponseData<?>[1];
			List<ICabanaService> cabanaList = HttpClientCabana.getInstance().getCabanaServiceNew();
			CountDownLatch latch = new CountDownLatch(cabanaList.size());
			for (ICabanaService cabanaService : cabanaList) {
				executor.submit(() -> cabanaService
						.change(buildReqBody(cabanasalt, new TreeMap<String, String>(Comparator.naturalOrder()) {
							{
								put("fromId", fromId.toString());
								put("toId", toId.toString());
							}
						})).subscribe(d -> {
							if (d.getStatus() != GeneralResponseData.ResponseStatus.SUCCESS.getValue())
								log.info("Cabana change( {} {} ) >> return error. {}", fromId, toId, d.getMessage());
							log.debug(">> CabanaServiceImpl change result:{}", d);
							ret[0] = d;
							latch.countDown();
						}, e -> {
							log.error("Cabana change( {} {} ) >> process error. ", fromId, toId, e);
							ret[0] = new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请求异常");
							latch.countDown();
						}));
			}
			latch.await();
			return ret[0];
		} catch (Exception e) {
			log.error("Cabana change( {} {} ) >> finally error. ", fromId, toId, e);
			return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "处理异常");
		}
	}

	@Override
	public ResponseData<List<CabanaStatus>> status4Error() {
		log.debug(">> CabanaServiceImpl status4Error param1 not");
		try {
			ResponseData<List<CabanaStatus>>[] ret = new ResponseData[1];
			List<ICabanaService> cabanaList = HttpClientCabana.getInstance().getCabanaServiceNew();
			CountDownLatch latch = new CountDownLatch(cabanaList.size());
			for (ICabanaService cabanaService : cabanaList) {
				executor.submit(() -> cabanaService
						.status4Error(buildReqBody(cabanasalt, new TreeMap<>(Comparator.naturalOrder())))
						.subscribe(d -> {
							if (d.getStatus() != GeneralResponseData.ResponseStatus.SUCCESS.getValue())
								log.info("Cabana status4Error() >> return error. {}", d.getMessage());
							log.debug(">> CabanaServiceImpl status4Error result:{}", d);
							ret[0] = d;
							latch.countDown();
						}, e -> {
							log.error("Cabana status4Error() >> process error. ", e);
							ret[0] = new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请求异常");
							latch.countDown();
						}));
			}
			latch.await();
			return ret[0];
		} catch (Exception e) {
			log.error("Cabana status4Error() >> finally error. ", e);
			return new ResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "处理异常");
		}
	}

	/**
	 * build {@link RequestBody}
	 * <p>
	 * encrypt the param {@code params} with the param {@code salt}
	 *
	 * @param salt
	 *            used to encrypt the param
	 * @param params
	 *            to be encrypted and pack to {@link RequestBody}
	 */
	@Override
	public RequestBody buildReqBody(String salt, TreeMap<String, String> params) throws JsonProcessingException {
		params.put(CONSTANT_TOKEN_VARIABLE, md5digest(params, salt));
		return RequestBody.create(MediaType.parse(CONSTANT_HTTP_MEDIA_TYPE), mapper.writeValueAsString(params));
	}

	@Override
	public ResponseData<?> activeQuickPay(String rebateUser, String quickAcc, String quickPass, String mobile) {
		log.debug(">> CabanaServiceImpl activeQuickPay param1:{},param2:{},param3:{},param4:{}", rebateUser, quickAcc,
				quickPass, mobile);
		if (StringUtils.isBlank(mobile) || StringUtils.isBlank(quickAcc) || StringUtils.isBlank(quickPass)) {
			log.info("Cabana activeQuickPay({} {}) >> the param (mobile or quickAcc or quickPass ) is empty. ", mobile,
					quickAcc);
			return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数为空.");
		}
		try {
			ResponseData<?>[] ret = new ResponseData<?>[1];
			List<ICabanaService> cabanaList = HttpClientCabana.getInstance().getCabanaServiceNew();
			CountDownLatch latch = new CountDownLatch(cabanaList.size());
			for (ICabanaService cabanaService : cabanaList) {
				executor.submit(() -> cabanaService.activeQuickPay(
						buildReqBody(cabanasalt, new TreeMap<String, String>(Comparator.naturalOrder()) {
							{
								put("rebateUser", rebateUser);
								put("quickAcc", quickAcc);
								put("quickPass", quickPass);
								put("mobile", mobile);
							}
						})).subscribe(d -> {
							if (d.getStatus() != GeneralResponseData.ResponseStatus.SUCCESS.getValue())
								log.info("Cabana activeQuickPay( {} {} ) >> return error. {}", mobile, quickAcc,
										d.getMessage());
							log.debug(">> CabanaServiceImpl activeQuickPay result:{}", d);
							ret[0] = d;
							latch.countDown();
						}, e -> {
							log.error("Cabana activeQuickPay( {} {} ) >> process error. ", mobile, quickAcc, e);
							ret[0] = new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请求异常");
							latch.countDown();
						}));
			}
			latch.await();
			return ret[0];
		} catch (Exception e) {
			log.error("Cabana activeQuickPay( {} {} ) >> finally error. ", mobile, quickAcc, e);
			return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "处理异常");
		}
	}

	@Override
	public ResponseData<?> refreshAcc(Integer accId) {
		log.debug(">> CabanaServiceImpl refreshAcc param1:{}", accId);
		log.debug("refreshAcc>> accId {}", accId);
		List<Account> accList = accSer.getRebateAccListById(accId);
		if (accList == null) {
			return null;
		}
		try {
			String accListStr = mapper.writeValueAsString(accList);
			ResponseData<?>[] ret = new ResponseData<?>[1];
			List<ICabanaService> cabanaList = HttpClientCabana.getInstance().getCabanaServiceNew();
			CountDownLatch latch = new CountDownLatch(cabanaList.size());
			for (ICabanaService cabanaService : cabanaList) {
				executor.submit(() -> cabanaService
						.refreshAcc(buildReqBody(cabanasalt, new TreeMap<String, String>(Comparator.naturalOrder()) {
							{
								put("accListStr", accListStr);
							}
						})).subscribe(d -> {
							if (d.getStatus() != GeneralResponseData.ResponseStatus.SUCCESS.getValue())
								log.info("Cabana refreshAcc( {} ) >> return error. {}", accId, d.getMessage());
							log.debug(">> CabanaServiceImpl refreshAcc result:{}", d);
							ret[0] = d;
							latch.countDown();
						}, e -> {
							log.error("Cabana refreshAcc( {} ) >> process error. ", accId, e);
							ret[0] = new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请求异常");
							latch.countDown();
						}));
			}
			latch.await();
			return ret[0];
		} catch (Exception e) {
			log.error("Cabana refreshAcc( {} ) >> finally error. ", accId, e);
			return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "处理异常");
		}
	}

	@Override
	public ResponseData<?> getLastVersion() {
		log.debug(">> CabanaServiceImpl getLastVersion param1 not");
		try {
			ResponseData<?>[] ret = new ResponseData<?>[1];
			List<ICabanaService> cabanaList = HttpClientCabana.getInstance().getCabanaServiceNew();
			CountDownLatch latch = new CountDownLatch(cabanaList.size());
			for (ICabanaService cabanaService : cabanaList) {
				executor.submit(() -> cabanaService.getLastVersion(
						buildReqBody(cabanasalt, new TreeMap<String, String>(Comparator.naturalOrder()) {
							{
								put("useToCreateTokken", "abc");
							}
						})).subscribe(d -> {
							if (d.getStatus() != GeneralResponseData.ResponseStatus.SUCCESS.getValue())
								log.info("Cabana getLastVersion>> return error. {}", d.getMessage());
							log.debug(">> CabanaServiceImpl getLastVersion result:{}", d);
							ret[0] = d;
							latch.countDown();
						}, e -> {
							log.error("Cabana getLastVersion>> process error. ", e);
							ret[0] = new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请求异常");
							latch.countDown();
						}));
			}
			latch.await();
			return ret[0];
		} catch (Exception e) {
			log.error("Cabana getLastVersion>> finally error. ", e);
			return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "处理异常");
		}
	}

	@Override
	public ResponseData<?> appPatch(String appPatchVersion,String url) {
		log.debug(">> CabanaServiceImpl appPatch param1:{}", appPatchVersion);
		try {
			ResponseData<?>[] ret = new ResponseData<?>[1];
			List<ICabanaService> cabanaList = HttpClientCabana.getInstance().getCabanaServiceNew();
			CountDownLatch latch = new CountDownLatch(cabanaList.size());
			for (ICabanaService cabanaService : cabanaList) {
				executor.submit(() -> cabanaService
						.doPatch4App(buildReqBody(cabanasalt, new TreeMap<String, String>(Comparator.naturalOrder()) {
							{
								put("appPatchVersion", appPatchVersion);
								put("patchUrl", url);
							}
						})).subscribe(d -> {
							if (d.getStatus() != GeneralResponseData.ResponseStatus.SUCCESS.getValue())
								log.info("Cabana appPatch>> return error. {}", d.getMessage());
							log.debug(">> CabanaServiceImpl appPatch result:{}", d);
							ret[0] = d;
							latch.countDown();
						}, e -> {
							log.error("Cabana appPatch>> process error. ", e);
							ret[0] = new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请求异常");
							latch.countDown();
						}));
			}
			latch.await();
			return ret[0];
		} catch (Exception e) {
			log.error("Cabana appPatch>> finally error. ", e);
			return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "处理异常");
		}
	}
	
	@Override
    public ResponseData<?> initQuickPay(Integer accId) {
		log.debug(">> CabanaServiceImpl initQuickPay param1:{}", accId);
        try {
            ResponseData<?>[] ret = new ResponseData<?>[1];
            List<ICabanaService> cabanaList = HttpClientCabana.getInstance().getCabanaServiceNew();
			CountDownLatch latch = new CountDownLatch(cabanaList.size());
			for (ICabanaService cabanaService : cabanaList) {
				executor.submit(() -> cabanaService
						.initQuickPay(buildReqBody(cabanasalt, new TreeMap<String, String>(Comparator.naturalOrder()) {
							{
								put("accId", accId.toString());
							}
						})).subscribe(d -> {
							if (d.getStatus() != GeneralResponseData.ResponseStatus.SUCCESS.getValue())
								log.debug("Cabana initQuickPay>> return error. {}", d.getMessage());
							log.debug(">> CabanaServiceImpl initQuickPay result:{}", d);
							ret[0] = d;
							latch.countDown();
						}, e -> {
							log.error("Cabana initQuickPay>> process error. ", e);
							ret[0] = new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请求异常");
							latch.countDown();
						}));
			}
			latch.await();
			return ret[0];
        } catch (Exception e) {
            log.error("Cabana initQuickPay>> finally error. ", e);
            return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "处理异常");
        }
    }

	@Override
	public ResponseData<?> forcedExit(String mobile) {
		log.debug(">> CabanaServiceImpl forcedExit param1:{}", mobile);
		try {
			ResponseData<?>[] ret = new ResponseData<?>[1];
			List<ICabanaService> cabanaList = HttpClientCabana.getInstance().getCabanaServiceNew();
			CountDownLatch latch = new CountDownLatch(cabanaList.size());
			for (ICabanaService cabanaService : cabanaList) {
				executor.submit(() -> cabanaService
						.forcedExit(buildReqBody(cabanasalt, new TreeMap<String, String>(Comparator.naturalOrder()) {
							{
								put("mobile", mobile);
							}
						})).subscribe(d -> {
							if (d.getStatus() != GeneralResponseData.ResponseStatus.SUCCESS.getValue())
								log.debug("Cabana forcedExit>> return error. {}", d.getMessage());
							log.debug(">> CabanaServiceImpl forcedExit result:{}", d);
							ret[0] = d;
							latch.countDown();
						}, e -> {
							log.error("Cabana forcedExit>> process error. ", e);
							ret[0] = new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请求异常");
							latch.countDown();
						}));
			}
			latch.await();
			return ret[0];
		} catch (Exception e) {
			log.error("Cabana forcedExit>> finally error. ", e);
			return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "处理异常");
		}
	}
}
