package com.xinbo.fundstransfer.restful.v2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.BizLevel;
import com.xinbo.fundstransfer.domain.pojo.SystemWebSocketCategory;
import com.xinbo.fundstransfer.restful.v2.pojo.RequestLevel;
import com.xinbo.fundstransfer.service.HandicapService;
import com.xinbo.fundstransfer.service.LevelService;
import com.xinbo.fundstransfer.service.RedisService;

/**
 * 层级信息
 */
@RestController
@RequestMapping("/api/v2/level")
public class Level2Controller extends TokenValidation {
	private static final Logger log = LoggerFactory.getLogger(Level2Controller.class);
	@Autowired
	private LevelService levelService;
	@Autowired
	private HandicapService handicapService;
	@Autowired
	private RedisService redisService;
	private ObjectMapper mapper = new ObjectMapper();
	SimpleResponseData error400 = new SimpleResponseData(400, "Error");
	SimpleResponseData error401 = new SimpleResponseData(401, "Error");
	SimpleResponseData error500 = new SimpleResponseData(500, "Error");
	SimpleResponseData success = new SimpleResponseData(1, "OK");
	/**
	 * 层级新增或修改
	 * 
	 * @throws JsonProcessingException
	 */
	@RequestMapping(value = "/put", method = RequestMethod.POST, consumes = "application/json")
	public @ResponseBody SimpleResponseData put(@Valid @RequestBody RequestLevel requestBody, BindingResult result)
			throws JsonProcessingException {
		log.info("Level >> RequestBody:{}", mapper.writeValueAsString(requestBody));
		if (result.hasErrors()) {
			return error400;
		}
		if (!checkToken(requestBody.getToken(), requestBody.getHandicap())) {
			return error401;
		}
		BizHandicap bizHandicap = handicapService.findFromCacheByCode(requestBody.getHandicap());
		if (null == bizHandicap || !Objects.equals(bizHandicap.getStatus(), 1)) {
			log.error("{} 盘口不存在, 层级code：{}", requestBody.getHandicap(), requestBody.getCode());
			return error400;
		}
		BizLevel level = levelService.findFromCache(bizHandicap.getId(), requestBody.getCode());
		if (level == null) {
			level = new BizLevel();
			level.setStatus(1);
			level.setCode(requestBody.getCode());
			level.setHandicapId(bizHandicap.getId());
			level.setName(requestBody.getName());
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
		} else if (requestBody.getStatus() != null || StringUtils.isNotEmpty(requestBody.getName())) {
			if (null != requestBody.getStatus()) {
				if (requestBody.getStatus() != 0 && requestBody.getStatus() != 1 && requestBody.getStatus() != 2) {
					// 层级状态 停用 启用 删除 可以同步 其他值不同步
					return error400;
				}
				level.setStatus(requestBody.getStatus());
			}
			if (StringUtils.isNotEmpty(requestBody.getName())) {
				level.setName(requestBody.getName());
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
		return success;
	}
}
