package com.xinbo.fundstransfer.restful.api;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.*;

import com.xinbo.fundstransfer.domain.ResponseData;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.TransferEntity;
import com.xinbo.fundstransfer.service.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.domain.GeneralResponseData.ResponseStatus;

/**
 * 云端请求接口
 *
 * @author
 */
@RestController("apiCloudController")
@RequestMapping("/api/cloud")
public class CloudController {
	private static Logger log = LoggerFactory.getLogger(CloudController.class);
	@Value("${funds.transfer.apicloudkey}")
	String keyscloud;
	@Autowired
	AllocateTransferService allocateTransferService;
	@Autowired
	AccountService accountService;
	@Autowired
	private ObjectMapper mapper = new ObjectMapper();
	@Autowired
	Environment environment;

	@RequestMapping("/trans/apply")
	public String applyTrans(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.info("Cloud (ApplyTrans) >> RequestBody:{}", bodyJson);
			Map arg0 = mapper.readValue(bodyJson, HashMap.class);
			String account = (String) arg0.get("account");
			String handicap = (String) arg0.get("handicap");
			String lStr = (String) arg0.get("l");
			Integer l = StringUtils.isNumeric(lStr) ? Integer.valueOf(lStr) : null;
			String balStr = (String) arg0.get("balance");
			BigDecimal balance = StringUtils.isNotBlank(balStr) ? new BigDecimal(balStr) : null;
			String token = (String) arg0.get("token");
			Map<String, Object> params = new HashMap<String, Object>() {
				{
					put("account", account);
					put("handicap", handicap);
					put("l", l);
					put("balance", balance);
				}
			};
			if (!Objects.equals(token, digest(params))) {
				log.error("Cloud(ApplyTrans) token error. {}", bodyJson);
				return mapper.writeValueAsString(
						new SimpleResponseData(ResponseStatus.FAIL.getValue(), "Cloud(ApplyTrans) token error."));
			}
			TransferEntity data = allocateTransferService.applyByFrom(account, handicap, l, balance);
			ResponseData<TransferEntity> responseData = new ResponseData<>(ResponseStatus.SUCCESS.getValue(),
					"success");
			responseData.setData(data);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			log.error("Cloud(ApplyTrans) error.", e);
			return mapper.writeValueAsString(new SimpleResponseData(ResponseStatus.FAIL.getValue(),
					"Cloud(ApplyTrans) error." + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/trans/cancel")
	public String canclTrans(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.info("Cloud (canclTrans) >> RequestBody:{}", bodyJson);
			Map arg0 = mapper.readValue(bodyJson, HashMap.class);
			String fromAccount = (String) arg0.get("fromAccount");
			String toAccount = (String) arg0.get("toAccount");
			Integer toId = findAccId(toAccount);
			String token = (String) arg0.get("token");
			Map<String, Object> params = new HashMap<String, Object>() {
				{
					put("fromAccount", fromAccount);
					put("toAccount", toAccount);
				}
			};
			if (Objects.isNull(toId)) {
				log.error("Cloud(canclTrans) toAccount doesn't exist. {}", bodyJson);
				return mapper.writeValueAsString(
						new SimpleResponseData(ResponseStatus.FAIL.getValue(), "toAccount doesn't exist."));
			}
			if (!Objects.equals(token, digest(params))) {
				log.error("Cloud(canclTrans) token error. {}", bodyJson);
				return mapper.writeValueAsString(
						new SimpleResponseData(ResponseStatus.FAIL.getValue(), "Cloud(canclTrans) token error."));
			}
			allocateTransferService.cancelByCloud(fromAccount, toId);
			return mapper.writeValueAsString(new SimpleResponseData(ResponseStatus.SUCCESS.getValue(), "success"));
		} catch (Exception e) {
			log.error("Cloud (canclTrans) error.", e);
			return mapper.writeValueAsString(new SimpleResponseData(ResponseStatus.FAIL.getValue(),
					"Cloud (canclTrans) error." + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/trans/ack")
	public String ackTrans(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.info("Cloud (transAck) >> RequestBody:{}", bodyJson);
			Map arg0 = mapper.readValue(bodyJson, HashMap.class);
			String fromAccount = (String) arg0.get("fromAccount");
			String toAccount = (String) arg0.get("toAccount");
			Integer toId = findAccId(toAccount);
			String token = (String) arg0.get("token");
			Map<String, Object> params = new HashMap<String, Object>() {
				{
					put("fromAccount", fromAccount);
					put("toAccount", toAccount);
				}
			};
			if (Objects.isNull(toId)) {
				log.error("Cloud(transAck) toAccount doesn't exist. {}", bodyJson);
				return mapper.writeValueAsString(
						new SimpleResponseData(ResponseStatus.FAIL.getValue(), " toAccount doesn't exist."));
			}
			if (!Objects.equals(token, digest(params))) {
				log.error("Cloud(transAck) token error. {}", bodyJson);
				return mapper
						.writeValueAsString(new SimpleResponseData(ResponseStatus.FAIL.getValue(), "token error."));
			}
			allocateTransferService.ackByCloud(fromAccount, toId);
			return mapper.writeValueAsString(new SimpleResponseData(ResponseStatus.SUCCESS.getValue(), "success"));
		} catch (Exception e) {
			log.error("Cloud (transAck) error.", e);
			return mapper.writeValueAsString(
					new SimpleResponseData(ResponseStatus.FAIL.getValue(), "error." + e.getLocalizedMessage()));
		}
	}

	private String digest(Map<String, Object> arg0) throws Exception {
		Map<String, Object> params = new TreeMap<String, Object>(Comparator.naturalOrder()) {
			{
				putAll(arg0);
			}
		};
		StringBuilder target = new StringBuilder();
		for (Object val : params.values()) {
			if (Objects.nonNull(val)) {
				target.append(val.toString().trim());
			}
		}
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		md5.update((target + keyscloud).getBytes());
		byte[] btResult = md5.digest();
		StringBuilder md5Token = new StringBuilder();
		for (byte b : btResult) {
			int bt = b & 0xff;
			if (bt < 16) {
				md5Token.append(0);
			}
			md5Token.append(Integer.toHexString(bt));
		}
		return md5Token.toString().toUpperCase();
	}

	private Integer findAccId(String account) {
		account = StringUtils.trimToEmpty(account);
		AccountBaseInfo acc = accountService.getFromCacheByTypeAndAccount(AccountType.OutBank.getTypeId(), account);
		if (Objects.nonNull(acc)) {
			return acc.getId();
		}
		acc = accountService.getFromCacheByTypeAndAccount(AccountType.ReserveBank.getTypeId(), account);
		return Objects.nonNull(acc) ? acc.getId() : null;
	}
}
