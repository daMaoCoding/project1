package com.xinbo.fundstransfer.restful.api;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.GeneralResponseData.ResponseStatus;
import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.restful.BaseController;
import com.xinbo.fundstransfer.restful.api.pojo.ApiALiPayLog;
import com.xinbo.fundstransfer.runtime.MemCacheUtils;
import com.xinbo.fundstransfer.service.*;

/**
 * 支付宝流水请求借口
 * 
 * @author 007
 *
 */
@RestController("apiALiPayLogRequestController")
@RequestMapping("/api/alipayLog")
public class ALiPayLogRequestController extends BaseController {
	Logger log = LoggerFactory.getLogger(this.getClass());
	@Autowired
	AccountService accountService;
	@Autowired
	Environment environment;
	private ObjectMapper mapper = new ObjectMapper();

	@RequestMapping("/put")
	public String put(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.info("ALiPayLog >> RequestBody:{}", bodyJson);
			ApiALiPayLog entity = null;
			BizAliLog o = null;
			StringBuilder sb = new StringBuilder();
			List<ApiALiPayLog> entityList = new ArrayList<ApiALiPayLog>();
			JsonNode rootNode = mapper.readTree(bodyJson);
			JsonNode content = rootNode.path("data");
			JsonNode toKen = rootNode.path("token");
			// 计算token
			JSONArray jsonArray = new JSONArray(content.toString());
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObj = jsonArray.getJSONObject(i);
				entity = mapper.readValue(jsonObj.toString(), ApiALiPayLog.class);
				entityList.add(entity);
				Map<String, String> parameters = new TreeMap<String, String>(Comparator.naturalOrder());
				parameters.put("account", entity.getAccount());
				parameters.put("amount", entity.getAmount());
				parameters.put("balance", entity.getBalance());
				parameters.put("depositor", entity.getDepositor());
				parameters.put("summary", entity.getSummary());
				parameters.put("tradingTime", entity.getTradingTime());
				for (String val : parameters.values()) {
					if (Objects.nonNull(val)) {
						sb.append(val.trim());
					}
				}
			}
			if (!checkToken(sb.toString(), toKen.toString().substring(1, toKen.toString().length() - 1))) {
				log.info("Token error. ALiPayLogToken:{}, md5:{}", entity.getToken(), sb.toString());
				return mapper.writeValueAsString(new SimpleResponseData(
						GeneralResponseData.ResponseStatus.FAIL.getValue(), "Failure, invalid Token."));
			}
			for (int j = 0; j < entityList.size(); j++) {
				entity = entityList.get(j);
				AccountBaseInfo bizAccount = accountService.getFromCacheByTypeAndAccount(AccountType.InAli.getTypeId(),
						entity.getAccount());
				if (null == bizAccount)
					continue;
				SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				o = new BizAliLog();
				o.setFromAccount(bizAccount.getId());
				o.setTradingTime(sd.parse(entity.getTradingTime()));
				o.setAmount(new BigDecimal(entity.getAmount()));
				o.setBalance(new BigDecimal(entity.getBalance()));
				o.setSummary(entity.getSummary());
				o.setDepositor(entity.getDepositor());
				String json = mapper.writeValueAsString(o);
				log.debug("alipay保存到队列,start:{}", json);
				MemCacheUtils.getInstance().getAlipaylogs().offer(json);
				log.debug("alipay保存到队列,finish:{}", json);
			}
		} catch (Exception e) {
			log.error("ALiPayLog error.", e);
			return mapper.writeValueAsString(new SimpleResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"Failure, " + e.getMessage()));
		}
		return mapper.writeValueAsString(new SimpleResponseData(ResponseStatus.SUCCESS.getValue(), "Success."));
	}

	private boolean checkToken(String content, String token) throws NoSuchAlgorithmException {
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		md5.update((content + environment.getProperty("funds.transfer.apicloudkey")).getBytes());
		// 进行哈希计算并返回结果
		byte[] btResult = md5.digest();
		// 进行哈希计算后得到的数据的长度
		StringBuffer md5Token = new StringBuffer();
		for (byte b : btResult) {
			int bt = b & 0xff;
			if (bt < 16) {
				md5Token.append(0);
			}
			md5Token.append(Integer.toHexString(bt));
		}
		if (md5Token.toString().toUpperCase().equals(token)) {
			return true;
		}
		return false;
	}

}
