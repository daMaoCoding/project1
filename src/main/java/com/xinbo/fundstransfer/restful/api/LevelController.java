package com.xinbo.fundstransfer.restful.api;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.xinbo.fundstransfer.CommonUtils;
import org.apache.commons.lang3.StringUtils;
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
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.BizLevel;
import com.xinbo.fundstransfer.domain.pojo.SystemWebSocketCategory;
import com.xinbo.fundstransfer.restful.api.pojo.ApiLevel;
import com.xinbo.fundstransfer.service.HandicapService;
import com.xinbo.fundstransfer.service.LevelService;
import com.xinbo.fundstransfer.service.RedisService;

/**
 * 层级请求接口
 *
 * @author Eden
 */
@RestController("apiLevelController")
@RequestMapping("/api/level")
public class LevelController {
	private Logger log = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private LevelService levelService;
	@Autowired
	private RedisService redisService;
	@Autowired
	private HandicapService handicapService;
	private ObjectMapper mapper = new ObjectMapper();
	@Autowired
	Environment environment;

	/**
	 * 层级新增或修改
	 */
	@RequestMapping(value = "/put")
	public String put(@RequestBody String bodyJson) throws JsonProcessingException {
		log.trace("Level >> RequestBody:{}", bodyJson);
		try {
			ApiLevel entity = mapper.readValue(bodyJson, ApiLevel.class);
			if (StringUtils.isEmpty(entity.getHandicap())) {
				return mapper.writeValueAsString(new GeneralResponseData(
						GeneralResponseData.ResponseStatus.FAIL.getValue(), "Failure, The handicap is empty."));
			}
			if (StringUtils.isEmpty(entity.getCode())) {
				return mapper.writeValueAsString(new GeneralResponseData(
						GeneralResponseData.ResponseStatus.FAIL.getValue(), "Failure, The code is empty."));
			}
			if (StringUtils.isEmpty(entity.getToken())) {
				return mapper.writeValueAsString(new GeneralResponseData(
						GeneralResponseData.ResponseStatus.FAIL.getValue(), "Failure, The token is empty."));
			}
			// 计算token
			Map<String, String> parameters = new TreeMap<>((obj1, obj2) -> obj1.compareTo(obj2));
			parameters.put("handicap", entity.getHandicap());
			parameters.put("code", entity.getCode());
			if (StringUtils.isNotEmpty(entity.getName())) {
				parameters.put("name", entity.getName());
			}
			if (null != entity.getStatus()) {
				parameters.put("status", String.valueOf(entity.getStatus()));
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
				log.info("Token error. Request token:{}, encrypt string:{}", entity.getToken(), sb.toString());
				return mapper.writeValueAsString(new GeneralResponseData(
						GeneralResponseData.ResponseStatus.FAIL.getValue(), "Failure, invalid Token."));
			}
			BizHandicap bizHandicap = handicapService.findFromCacheByCode(entity.getHandicap());
			if (null == bizHandicap) {
				log.info("{} 盘口不存在", entity.getHandicap());
				return mapper.writeValueAsString(new GeneralResponseData(
						GeneralResponseData.ResponseStatus.FAIL.getValue(), "Failure, handicap does not exist."));
			}
			BizLevel level = levelService.findFromCache(bizHandicap.getId(), entity.getCode());
			if (level == null) {
				level = new BizLevel();
				level.setStatus(1);
				level.setCode(entity.getCode());
				level.setHandicapId(bizHandicap.getId());
				level.setName(entity.getName());
				level = levelService.save(level);
				redisService.convertAndSend(RedisTopics.REFRESH_LEVEL, mapper.writeValueAsString(level));
				// 层级的变化广播的前端
				Map<String, Object> levelInfo = new HashMap<>();
				List<BizLevel> levelList = levelService.findByHandicapId(bizHandicap.getId());
				levelInfo.put("levelList", levelList);
				levelInfo.put("handicapId", bizHandicap.getId());
				String info = CommonUtils.genSysMsg4WS(null, SystemWebSocketCategory.LevelList,
						mapper.writeValueAsString(levelInfo));
				redisService.convertAndSend(RedisTopics.BROADCAST, info);
			} else if (entity.getStatus() != null || StringUtils.isNotEmpty(entity.getName())) {
				if (null != entity.getStatus()) {
					level.setStatus(entity.getStatus());
				}
				if (StringUtils.isNotEmpty(entity.getName())) {
					level.setName(entity.getName());
				}
				levelService.save(level);
				redisService.convertAndSend(RedisTopics.REFRESH_LEVEL, mapper.writeValueAsString(level));
				// 层级的变化广播的前端
				Map<String, Object> levelInfo = new HashMap<>();
				List<BizLevel> levelList = levelService.findByHandicapId(bizHandicap.getId());
				levelInfo.put("levelList", levelList);
				levelInfo.put("handicapId", bizHandicap.getId());
				String info = CommonUtils.genSysMsg4WS(null, SystemWebSocketCategory.LevelList,
						mapper.writeValueAsString(levelInfo));
				redisService.convertAndSend(RedisTopics.BROADCAST, info);

			}
			GeneralResponseData<BizLevel> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "Success.");
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			log.error("Level error. >>", e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"Failure, " + e.getLocalizedMessage()));
		}
	}
}
