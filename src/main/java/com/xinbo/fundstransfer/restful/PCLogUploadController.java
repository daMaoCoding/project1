package com.xinbo.fundstransfer.restful;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.component.net.socket.ActionEventEnum;
import com.xinbo.fundstransfer.component.net.socket.MessageEntity;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.service.RedisService;

import lombok.extern.slf4j.Slf4j;

/**
 * 描述:系统请求上传PC虚拟机日志 业务管理-PC监控-某台虚拟机日志上传
 */
@RestController
@Slf4j
public class PCLogUploadController {
	@Autowired
	private RedisService redisService;
	@Autowired
	private ObjectMapper mapper;

	/**
	 * 描述:PC监控--上传日志
	 * 
	 * @param map
	 *            key-v1:"time": "2019-02-15", key-v2:"virtualIp": 1
	 * @return
	 */
	@PostMapping(value = "/r/tool/uploadLog", consumes = "application/json")
	public Map uploadLog(@RequestBody Map map) {
		Map ret = new HashMap() {
			{
				put("status", "1");
				put("msg", "请求成功!");
			}
		};
		try {
			if (Objects.isNull(map) || Objects.isNull(map.get("time")) || Objects.isNull(map.get("virtualIp"))) {
				return new HashMap() {
					{
						put("-1", "参数不能为空!");
					}
				};
			}
			MessageEntity<Map> msg = new MessageEntity<>();
			Map<String, String> params = new HashMap<>();
			params.put("reqTime", map.get("time").toString());
			params.put("virtualIp", map.get("virtualIp").toString());
			msg.setAction(ActionEventEnum.UPLOADLOG.ordinal());
			msg.setData(params);
			msg.setIp(null);
			redisService.convertAndSend(RedisTopics.PUSH_MESSAGE_TOOLS, mapper.writeValueAsString(msg));
			return ret;
		} catch (Exception e) {
			log.error("请求PC上传日志失败:", e);
			ret = new HashMap() {
				{
					put("status", "-1");
					put("msg", "请求失败!");
				}
			};
		}
		return ret;
	}

	/**
	 * 描述:PC监控--补发流水
	 *
	 * @param map
	 *            key-v1:"time": "2019-02-15", key-v2:"virtualIp": 1
	 * @return
	 */
	@PostMapping(value = "/r/tool/cacheFlow", consumes = "application/json")
	public Map cacheFlow(@RequestBody Map map) {
		Map ret = new HashMap() {
			{
				put("status", "1");
				put("msg", "请求成功!");
			}
		};
		try {
			if (Objects.isNull(map) || Objects.isNull(map.get("time")) || Objects.isNull(map.get("virtualIp"))) {
				return new HashMap() {
					{
						put("-1", "参数不能为空!");
					}
				};
			}
			MessageEntity<Map> msg = new MessageEntity<>();
			Map<String, String> params = new HashMap<>();
			params.put("reqTime", map.get("time").toString());
			params.put("virtualIp", map.get("virtualIp").toString());
			msg.setAction(ActionEventEnum.RESENDLOG.ordinal());
			msg.setData(params);
			msg.setIp(null);
			redisService.convertAndSend(RedisTopics.PUSH_MESSAGE_TOOLS, mapper.writeValueAsString(msg));
			return ret;
		} catch (Exception e) {
			log.error("请求PC补发流水失败:", e);
			ret = new HashMap() {
				{
					put("status", "-1");
					put("msg", "请求失败!");
				}
			};
		}
		return ret;
	}
}
