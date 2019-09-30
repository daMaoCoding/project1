package com.xinbo.fundstransfer.restful.api;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.GeneralResponseData.ResponseStatus;
import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.BizLevel;
import com.xinbo.fundstransfer.domain.entity.BizOutwardRequest;
import com.xinbo.fundstransfer.restful.BaseController;
import com.xinbo.fundstransfer.restful.api.pojo.ApiOutward;
import com.xinbo.fundstransfer.service.HandicapService;
import com.xinbo.fundstransfer.service.LevelService;
import com.xinbo.fundstransfer.service.OutwardRequestService;
import com.xinbo.fundstransfer.service.RedisService;

/**
 * 出款请求接口
 * 
 * @author Owner
 *
 */
@RestController("apiOutwardRequestController")
@RequestMapping("/api/outward")
public class OutwardRequestController extends BaseController {
	Logger log = LoggerFactory.getLogger(this.getClass());
	@Autowired
	public HttpServletRequest request;
	@Autowired
	LevelService levelService;
	@Autowired
	private RedisService redisService;
	@Autowired
	private OutwardRequestService outwardRequestService;
	@Autowired
	HandicapService handicapService;
	@Autowired
	Environment environment;
	private ObjectMapper mapper = new ObjectMapper();

	@RequestMapping("/put")
	public String put(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.info("Outward >> RequestBody:{}", bodyJson);
			ApiOutward entity = mapper.readValue(bodyJson, ApiOutward.class);
			BizHandicap bizHandicap = handicapService.findFromCacheByCode(entity.getHandicap());
			if (null == bizHandicap) {
				log.info("{} 盘口不存在,订单号：{}", entity.getHandicap(), entity.getOrderNo());
				return mapper.writeValueAsString(
						new SimpleResponseData(ResponseStatus.FAIL.getValue(), "Failure, handicap does not exist."));
			}
			// 计算token
			Map<String, String> parameters = new TreeMap<String, String>(new Comparator<String>() {
				@Override
				public int compare(String obj1, String obj2) {
					return obj1.compareTo(obj2);
				}
			});
			parameters.put("handicap", entity.getHandicap());
			parameters.put("amount", entity.getAmount());
			parameters.put("remark", entity.getRemark());
			parameters.put("orderno", entity.getOrderNo());
			parameters.put("createtime", entity.getCreateTime());
			parameters.put("account", entity.getAccount());
			parameters.put("banktype", entity.getBankType());
			parameters.put("bankname", entity.getBankName());
			parameters.put("owner", entity.getOwner());
			parameters.put("username", entity.getUsername());
			parameters.put("usercode", entity.getUsercode() + "");
			if (StringUtils.isNotEmpty(entity.getRealname())) {
				parameters.put("realname", entity.getRealname());
			}
			if (StringUtils.isNotEmpty(entity.getLevel())) {
				parameters.put("level", entity.getLevel());
			}
			StringBuilder sb = new StringBuilder();
			Set<Map.Entry<String, String>> entrySet = parameters.entrySet();
			for (Map.Entry<String, String> entry : entrySet) {
				sb.append(entry.getValue());
			}
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			md5.update(sb.append(environment.getProperty("funds.transfer.apikey")).toString().getBytes());
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
			if (!md5Token.toString().equals(entity.getToken())) {
				log.info("Token error. ReqToken:{}, md5:{}", entity.getToken(), sb.toString());
				return mapper.writeValueAsString(new GeneralResponseData(
						GeneralResponseData.ResponseStatus.FAIL.getValue(), "Failure, invalid Token."));
			}

			BizOutwardRequest o = new BizOutwardRequest();
			o.setAmount(new BigDecimal(entity.getAmount()));
			o.setCreateTime(DateUtils.parseDate(entity.getCreateTime(), "yyyy-MM-dd HH:mm:ss"));
			o.setHandicap(bizHandicap.getId());
			if (StringUtils.isNotEmpty(entity.getLevel())) {
				BizLevel bizLevel = levelService.findFromCache(bizHandicap.getId(), entity.getLevel());
				o.setLevel(bizLevel.getId());
			}
			o.setOrderNo(entity.getOrderNo());
			o.setRemark(entity.getRemark());
			o.setToAccount(entity.getAccount());
			o.setToAccountBank(entity.getBankType());
			o.setToAccountName(entity.getBankName());
			o.setToAccountOwner(entity.getOwner());
			o.setMember(entity.getUsername());
			o.setMemberCode(entity.getUsercode());
			String json = mapper.writeValueAsString(o);

			try {
				redisService.rightPush(RedisTopics.OUTWARD_REQUEST, json);
			} catch (Exception e) {
				log.error("", e);
				if (null == outwardRequestService.findByHandicapAndOrderNo(o.getHandicap(), o.getOrderNo())) {
					outwardRequestService.save(o);
					log.info("Outward[DB] direct put in db. orderNo: {}", o.getOrderNo());
				} else {
					log.info("Outward error, orderNo: {} already exist.", o.getOrderNo());
				}
			}
			SimpleResponseData responseData = new SimpleResponseData(ResponseStatus.SUCCESS.getValue(), "Success.");
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			log.error("Outward error", e);
			return mapper.writeValueAsString(
					new SimpleResponseData(ResponseStatus.FAIL.getValue(), "Failure, " + e.getLocalizedMessage()));
		}
	}

}
