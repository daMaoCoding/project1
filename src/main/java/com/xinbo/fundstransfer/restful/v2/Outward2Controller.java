package com.xinbo.fundstransfer.restful.v2;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.BizLevel;
import com.xinbo.fundstransfer.domain.entity.BizOutwardRequest;
import com.xinbo.fundstransfer.restful.v2.pojo.RequestOutward;
import com.xinbo.fundstransfer.service.HandicapService;
import com.xinbo.fundstransfer.service.LevelService;
import com.xinbo.fundstransfer.service.OutwardRequestService;
import com.xinbo.fundstransfer.service.RedisService;

import java.util.Objects;

/**
 * 新平台出款同步
 */
@RestController
@RequestMapping("/api/v2/outward")
public class Outward2Controller extends TokenValidation {
	private static final Logger log = LoggerFactory.getLogger(Outward2Controller.class);
	@Autowired
	LevelService levelService;
	@Autowired
	private RedisService redisService;
	@Autowired
	private OutwardRequestService outwardRequestService;
	@Autowired
	HandicapService handicapService;
	private ObjectMapper mapper = new ObjectMapper();
	SimpleResponseData error400 = new SimpleResponseData(400, "Error");
	SimpleResponseData error401 = new SimpleResponseData(401, "Error");
	SimpleResponseData error500 = new SimpleResponseData(500, "Error");
	SimpleResponseData success = new SimpleResponseData(1, "OK");

	/**
	 * 新平台出款订单 同步
	 * 
	 * @throws JsonProcessingException
	 */
	@RequestMapping(value = "/put", method = RequestMethod.POST, consumes = "application/json")
	public @ResponseBody SimpleResponseData put(@Valid @RequestBody RequestOutward requestBody, BindingResult result)
			throws JsonProcessingException {
		log.info("Outward >> RequestBody:{}", mapper.writeValueAsString(requestBody));
		if (result.hasErrors()) {
			log.error("{} 参数校验错误", requestBody.getOrderNo());
			return error400;
		}
		if (!checkToken(requestBody.getToken(), requestBody.getHandicap(), requestBody.getOrderNo())) {
			log.error("{} token错误", requestBody.getOrderNo());
			return error401;
		}
		BizHandicap bizHandicap = handicapService.findFromCacheByCode(requestBody.getHandicap());
		if (null == bizHandicap || !Objects.equals(bizHandicap.getStatus(), 1)) {
			log.error("{} 盘口不存在, orderNo：{}", requestBody.getHandicap(), requestBody.getOrderNo());
			return error400;
		}
		try {
			BizOutwardRequest o = new BizOutwardRequest();
			o.setAmount(requestBody.getAmount());
			o.setCreateTime(DateUtils.parseDate(requestBody.getCreateTime(), "yyyy-MM-dd HH:mm:ss"));
			o.setHandicap(bizHandicap.getId());
			if (StringUtils.isNotEmpty(requestBody.getLevel())) {
				BizLevel bizLevel = levelService.findFromCache(bizHandicap.getId(), requestBody.getLevel());
				o.setLevel(bizLevel != null ? bizLevel.getId() : null);
			}
			o.setOrderNo(requestBody.getOrderNo());
			o.setRemark(requestBody.getRemark());
			o.setToAccount(requestBody.getAccount());
			o.setToAccountBank(requestBody.getBankType());
			o.setToAccountName(requestBody.getBankName());
			o.setToAccountOwner(requestBody.getOwner());
			o.setMember(requestBody.getUsername());
			o.setOutIp(requestBody.getOutIp());
			o.setBankProvince(requestBody.getBankProvince());
			o.setBankCity(requestBody.getBankCity());
			String json = mapper.writeValueAsString(o);
			try {
				redisService.rightPush(RedisTopics.OUTWARD_REQUEST, json);
				log.info("Outward[redis],orderNo:{}", requestBody.getOrderNo());
			} catch (Exception e) {
				log.error("redis保存新平台出款提单错误.", e);
				if (null == outwardRequestService.findByHandicapAndOrderNo(o.getHandicap(), o.getOrderNo())) {
					outwardRequestService.save(o);
					log.info("Outward[DB] direct put in db. orderNo: {}", o.getOrderNo());
				} else {
					log.info("Outward error, orderNo: {} already exist.", o.getOrderNo());
				}
			}
			return success;
		} catch (Exception e) {
			log.error("Outward error. orderNo:" + requestBody.getOrderNo() + "," + e.getLocalizedMessage(), e);
			return error500;
		}
	}
}
