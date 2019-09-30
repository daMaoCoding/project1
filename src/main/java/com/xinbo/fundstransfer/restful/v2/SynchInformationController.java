package com.xinbo.fundstransfer.restful.v2;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.component.net.http.RequestBodyParser;
import com.xinbo.fundstransfer.component.net.http.v2.HttpClientNew;
import com.xinbo.fundstransfer.component.net.http.v2.PlatformServiceApi;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.BizOutwardRequest;
import com.xinbo.fundstransfer.service.HandicapService;
import com.xinbo.fundstransfer.service.OutwardRequestService;
import com.xinbo.fundstransfer.service.RedisService;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by Owner on 2018/5/4. 调用新平台用于批量同步入款，出款，账号，层级等信息
 */
@RestController
@RequestMapping("/r/synch")
@Slf4j
public class SynchInformationController {
	@Autowired
	RequestBodyParser requestBodyParser;
	@Autowired
	ObjectMapper mapper;
	@Autowired
	OutwardRequestService outwardRequestService;
	@Autowired
	HandicapService handicapService;
	@Autowired
	RedisService redisService;
	// 同步出款任务开关key
	private static final String SYNCH_OUTREQORDERS_SWITCH_KEY = "synchOrdersSwitchKey";
	// 同步入款任务开关key
	private static final String SYNCH_INCOMEORDERS_SWITCH_KEY = "synchIncomeOrdersSwitchKey";

	private enum SYNCHTYPE {
		INCOME(1, "入款"), OUTREQ(2, "出款");
		private int code;
		private String desc;

		SYNCHTYPE(int code, String desc) {
			this.code = code;
			this.desc = desc;
		}

		public int getCode() {
			return code;
		}

		public void setCode(int code) {
			this.code = code;
		}

		public String getDesc() {
			return desc;
		}

		public void setDesc(String desc) {
			this.desc = desc;
		}
	}

	private enum ACTION {
		OPEN(1, "1"), CLOSE(2, "2"), LOOKUP(3, "查询");
		private int code;
		private String desc;

		ACTION(int code, String desc) {
			this.code = code;
			this.desc = desc;
		}

		public int getCode() {
			return code;
		}

		public void setCode(int code) {
			this.code = code;
		}

		public String getDesc() {
			return desc;
		}

		public void setDesc(String desc) {
			this.desc = desc;
		}

		@Override
		public String toString() {
			return "ACTION{" + "code=" + code + ", desc='" + desc + '\'' + '}';
		}

	}

	/**
	 * 
	 * @param action
	 *            1 开启(则执行出入款同步) 2 关闭(不执行出入款同步任务) 3 查询
	 * @param type
	 *            1入款 2出款
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/controlSynchSwitch")
	public GeneralResponseData<Map<String, String>> controlSynchSwitch(
			@RequestParam(value = "action", required = false) Integer action,
			@RequestParam(value = "type", required = false) Integer type) {
		GeneralResponseData<Map<String, String>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功!");
		StringRedisTemplate redisTemplate = redisService.getStringRedisTemplate();
		String key = null;
		if (action != null && action != ACTION.LOOKUP.code && type != null) {
			try {
				key = type == SYNCHTYPE.INCOME.code ? SYNCH_INCOMEORDERS_SWITCH_KEY
						: type == SYNCHTYPE.OUTREQ.code ? SYNCH_OUTREQORDERS_SWITCH_KEY : "";
				String val = action == ACTION.OPEN.code ? ACTION.OPEN.getDesc()
						: action == ACTION.CLOSE.code ? ACTION.CLOSE.getDesc() : "";
				if (StringUtils.isBlank(key) || StringUtils.isBlank(val)) {
					responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
							"操作失败!");
					return responseData;
				}
				redisTemplate.opsForValue().set(key, val);
			} catch (Exception e) {
				log.error("操作定时任务开关失败: ", e);
				return new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败!");

			}
		}
		if (StringUtils.isBlank(key)) {
			Map map = new HashMap();
			map.put("income", redisTemplate.opsForValue().get(SYNCH_INCOMEORDERS_SWITCH_KEY));
			map.put("outreq", redisTemplate.opsForValue().get(SYNCH_OUTREQORDERS_SWITCH_KEY));
			responseData.setData(map);
			return responseData;
		} else {
			String val = redisTemplate.opsForValue().get(key);
			String key2 = type == SYNCHTYPE.INCOME.code ? "income" : "outreq";
			Map map = new HashMap();
			map.put(key2, val);
			responseData.setData(map);
			return responseData;
		}
	}

	/**
	 * @param type
	 *            同步信息类型 1-入款,2-出款,3-帐号信息,4-层级信息 5-出款通道信息
	 * @param handicap
	 *            盘口编码 调用平台的key测试 2018bc5ff11b4d418d7a9b7c394c78a8 预生产2019开头
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/synchInfo")
	public String synch(@RequestParam(value = "type") Integer type, @RequestParam(value = "handicap") String handicap)
			throws JsonProcessingException {
		GeneralResponseData<String> responseData;
		try {
			// type:1-入款，2-出款，3-帐号信息，4-层级信息
			if (StringUtils.isBlank(handicap)) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请选择盘口!");
				return mapper.writeValueAsString(responseData);
			}
			if (type == null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"请选择同步类型!");
				return mapper.writeValueAsString(responseData);
			}
			log.info("同步信息参数:handicap:{},type:{}", handicap, type);
			HttpClientNew.getInstance().getPlatformServiceApi()
					.sync(requestBodyParser.buildSyncRequestBody(handicap, type))
					.subscribe(data -> log.info("同步信息结果:{}", data), e -> log.error("sync error.", e));
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "同步信息成功!");
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			log.info("同步信息失败:{}", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "同步信息失败!");
			return mapper.writeValueAsString(responseData);
		}
	}

	/**
	 * type :1 本次入款信息查询 2 大额中奖
	 *
	 * @param orderNo
	 * @param type
	 *            1,2
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/getInfo")
	public String getInfo(@RequestParam(value = "orderNo") String orderNo, @RequestParam(value = "type") Integer type)
			throws JsonProcessingException {
		GeneralResponseData<String> responseData;
		if (StringUtils.isBlank(orderNo)) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数丢失!");
			return mapper.writeValueAsString(responseData);
		}
		try {
			log.info("查询信息参数:orderNo:{}", orderNo);
			BizOutwardRequest bizOutwardRequest = outwardRequestService.findByOrderNo(orderNo);
			if (bizOutwardRequest == null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"订单信息不存在!");
				return mapper.writeValueAsString(responseData);
			}
			BizHandicap bizHandicap = handicapService.findFromCacheById(bizOutwardRequest.getHandicap());
			ThreadLocal<String> json = new ThreadLocal<>();
			if (type != null) {
				PlatformServiceApi platformServiceApi = HttpClientNew.getInstance().getPlatformServiceApi();
				if (type == 1) {
					platformServiceApi.findBigWin(requestBodyParser.buildRequestBody(bizHandicap.getCode(),
							bizOutwardRequest.getOrderNo(), "")).subscribe(data -> {
								log.info("查询会员大额中奖信息结果:{}", data);
								if (data.getStatus() == 1) {
									ObjectMapper objectMapper = new ObjectMapper();
									try {
										json.set(objectMapper.writeValueAsString(data));
									} catch (JsonProcessingException e) {
										log.info("查询会员大额中奖信息格式转换失败:{}", e);
									}
								} else {
									log.info("查询失败:{}", data);
								}
							}, e -> {
								log.error("查询会员大额中奖信息失败", e);
								json.set("查询失败" + e.getLocalizedMessage());
							});
				}
				if (type == 2) {
					platformServiceApi.findThis(requestBodyParser.buildRequestBody(bizHandicap.getCode(),
							bizOutwardRequest.getOrderNo(), "")).subscribe(data -> {
								log.info("查询最近入款信息结果:{}", data);
								if (data.getStatus() == 1) {
									ObjectMapper objectMapper = new ObjectMapper();
									try {
										json.set(objectMapper.writeValueAsString(data));
									} catch (JsonProcessingException e) {
										log.info("查询最近入款信息格式转换失败:{}", e);
									}
								} else {
									log.info("查询失败:{}", data);
									json.set("查询失败:" + data.getStatus());
								}
							}, e -> {
								log.error("查询失败", e);
								json.set("查询失败" + e.getLocalizedMessage());
							});
				}
				if (type == 3) {
					platformServiceApi.findThisLoginIp(requestBodyParser.buildRequestBody(bizHandicap.getCode(),
							bizOutwardRequest.getOrderNo(), "")).subscribe(data -> {
								log.info("本次账号登陆IP查询结果:{}", data);
								if (data.getStatus() == 1) {
									ObjectMapper objectMapper = new ObjectMapper();
									try {
										json.set(objectMapper.writeValueAsString(data));
									} catch (JsonProcessingException e) {
										log.info("本次账号登陆IP查询信息格式转换失败:{}", e);
									}
								} else {
									log.info("查询失败:{}", data);
									json.set("查询失败:" + data.getStatus());
								}
							}, e -> {
								log.error("查询失败", e);
								json.set("查询失败" + e.getLocalizedMessage());
							});
				}
				if (type == 4) {
					platformServiceApi.findThisSameIp(requestBodyParser.buildRequestBody(bizHandicap.getCode(),
							bizOutwardRequest.getOrderNo(), "")).subscribe(data -> {
								log.info("本次相同IP登陆账号查询结果:{}", data);
								if (data.getStatus() == 1) {
									ObjectMapper objectMapper = new ObjectMapper();
									try {
										json.set(objectMapper.writeValueAsString(data));
									} catch (JsonProcessingException e) {
										log.info("本次相同IP登陆账号查询信息格式转换失败:{}", e);
									}
								} else {
									log.info("查询失败:{}", data);
									json.set("查询失败:" + data.getStatus());
								}
							}, e -> {
								log.error("查询失败", e);
								json.set("查询失败" + e.getLocalizedMessage());
							});
				}
			}
			String res = json.get();
			if (StringUtils.isNotBlank(res)) {
				if (res.contains("查询失败")) {
					responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
							"获取信息失败!");
				} else {
					responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
							"获取信息成功!");
				}

			} else {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						"获取信息失败!");
			}
			responseData.setData(res);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			log.error("获取信息失败", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "获取信息失败!");
			return mapper.writeValueAsString(responseData);
		}
	}
}
