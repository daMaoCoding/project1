package com.xinbo.fundstransfer.restful.v3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import com.xinbo.fundstransfer.domain.entity.BizAccountMore;
import com.xinbo.fundstransfer.domain.entity.BizRebateUser;
import com.xinbo.fundstransfer.restful.v3.pojo.ReqV3RebateUser;
import com.xinbo.fundstransfer.service.AccountMoreService;
import com.xinbo.fundstransfer.service.CabanaService;
import com.xinbo.fundstransfer.service.RebateUserService;
import com.xinbo.fundstransfer.service.RedisService;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppProperties;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.domain.GeneralResponseData.ResponseStatus;

@RestController
@RequestMapping("/api/v3")
@SuppressWarnings("WeakAccess unused")
public class RebateUser3Controller {
	private static final Logger log = LoggerFactory.getLogger(RebateUser3Controller.class);
	private ObjectMapper mapper = new ObjectMapper();
	@Autowired
	private AppProperties appProperties;
	@Autowired
	private RebateUserService rebateUserService;
	@Autowired
	private CabanaService cabanaService;
	@Autowired
	private RedisService redisSer;
	@Autowired
	private AccountMoreService accMoreSer;

	@RequestMapping("/flushRebateUser")
	public String flush() throws JsonProcessingException {
		try {
			redisSer.convertAndSend(RedisTopics.REBATE_USER_CLEAN, StringUtils.EMPTY);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			return mapper
					.writeValueAsString(new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue()));
		}
	}

	@RequestMapping("/put")
	public String put(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.info("RebateUserPut >> RequestBody:{}", bodyJson);
			ReqV3RebateUser entity = null;
			BizRebateUser o = null;
			String sb = "";
			List<ReqV3RebateUser> entityList = new ArrayList<ReqV3RebateUser>();
			JsonNode rootNode = mapper.readTree(bodyJson);
			JsonNode content = rootNode.path("data");
			JsonNode toKen = rootNode.path("token");
			// 计算token
			if ("".equals(content.toString())) {
				entity = mapper.readValue(bodyJson.toString(), ReqV3RebateUser.class);
				if ("".equals(toKen.toString()) || null == entity.getUid() || null == entity.getUserName()
						|| null == entity.getPassword() || null == entity.getContactor()
						|| null == entity.getContactText() || null == entity.getSalesld() || null == entity.getFlag()
						|| null == entity.getStatus() || null == entity.getSalesName()) {
					log.info("参数校验失败. RequestBody:{}", bodyJson);
					return mapper.writeValueAsString(
							new SimpleResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验失败"));
				}
				entityList.add(entity);
				sb += entity.getUid() + entity.getUserName() + entity.getPassword() + entity.getContactor()
						+ entity.getContactText() + entity.getSalesld() + entity.getFlag().toString()
						+ entity.getStatus().toString();
			} else {
				JSONArray jsonArray = new JSONArray(content.toString());
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject jsonObj = jsonArray.getJSONObject(i);
					entity = mapper.readValue(jsonObj.toString(), ReqV3RebateUser.class);
					if ("".equals(toKen.toString()) || null == entity.getUid() || null == entity.getUserName()
							|| null == entity.getPassword() || null == entity.getContactor()
							|| null == entity.getContactText() || null == entity.getSalesld()
							|| null == entity.getFlag() || null == entity.getStatus()
							|| null == entity.getSalesName()) {
						log.info("参数校验失败. RequestBody:{}", bodyJson);
						return mapper.writeValueAsString(
								new SimpleResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验失败"));
					}
					entityList.add(entity);
					sb += entity.getUid() + entity.getUserName() + entity.getPassword() + entity.getContactor()
							+ entity.getContactText() + entity.getSalesld() + entity.getFlag().toString()
							+ entity.getStatus().toString();
				}
			}
			if (!checkToken(sb.toString(), toKen.toString().substring(1, toKen.toString().length() - 1))) {
				log.info("Token error. RebateUserPut:{}, token:{}", bodyJson, entity.getToken());
				return mapper.writeValueAsString(
						new SimpleResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "Token校验失败"));
			}
			for (int j = 0; j < entityList.size(); j++) {
				ReqV3RebateUser rebateUser = entityList.get(j);
				o = new BizRebateUser();
				o.setUid(rebateUser.getUid());
				o.setUserName(rebateUser.getUserName());
				o.setPassword(rebateUser.getPassword());
				o.setContactor(rebateUser.getContactor());
				o.setContactText(rebateUser.getContactText());
				o.setSalesld(rebateUser.getSalesld());
				o.setStatus(rebateUser.getStatus());
				o.setSalesName(rebateUser.getSalesName());
				o.setCreateTime(new Date());
				if (rebateUser.getFlag() == 0) {
					// 审核中的不入 库
					if (rebateUser.getStatus() == 1)
						continue;
					BizRebateUser user = rebateUserService.getFromCacheByUid(o.getUid());
					if (null != user) {
						user.setUserName(rebateUser.getUserName());
						user.setPassword(rebateUser.getPassword());
						user.setContactor(rebateUser.getContactor());
						user.setContactText(rebateUser.getContactText());
						user.setSalesld(rebateUser.getSalesld());
						user.setSalesName(rebateUser.getSalesName());
						user.setStatus(rebateUser.getStatus());
						user = rebateUserService.save(user);
					} else {
						user = rebateUserService.save(o);
					}
				} else if (rebateUser.getFlag() == 1) {
					BizRebateUser user = rebateUserService.getFromCacheByUid(o.getUid());
					if (null == user) {
						return mapper.writeValueAsString(new SimpleResponseData(
								GeneralResponseData.ResponseStatus.FAIL.getValue(), "不存在uid为：" + o.getUid() + " 的兼职"));
					}
					user.setUserName(rebateUser.getUserName());
					user.setPassword(rebateUser.getPassword());
					user.setContactor(rebateUser.getContactor());
					user.setContactText(rebateUser.getContactText());
					user.setSalesld(rebateUser.getSalesld());
					user.setStatus(rebateUser.getStatus());
					user.setSalesName(rebateUser.getSalesName());
					user = rebateUserService.save(user);
					// 修改时候 假如状态是停用状态 则强制退出
					BizAccountMore more = accMoreSer.getFromCacheByUid(o.getUid());
					if (null != more && null != more.getMoible() && rebateUser.getStatus().equals(2)) {
						cabanaService.forcedExit(more.getMoible());
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("RebateUserPut error{}", e.getMessage());
			return mapper.writeValueAsString(
					new SimpleResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "服务器内部错误"));
		}
		return mapper.writeValueAsString(new SimpleResponseData(ResponseStatus.SUCCESS.getValue(), "Success."));
	}

	private boolean checkToken(String Content, String token) {
		String calToken = CommonUtils.md5digest(Content + appProperties.getRebatesalt());
		if (StringUtils.equals(calToken, token))
			return true;
		return false;
	}
}
