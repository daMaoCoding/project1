package com.xinbo.fundstransfer.restful.v3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.AppProperties;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.domain.entity.SysUserProfile;
import com.xinbo.fundstransfer.domain.enums.UserProfileKey;
import com.xinbo.fundstransfer.restful.v3.pojo.ReqV3Rate;
import com.xinbo.fundstransfer.restful.v3.pojo.ReqV3RateItem;
import com.xinbo.fundstransfer.service.RedisService;
import com.xinbo.fundstransfer.service.SystemSettingService;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v3")
@SuppressWarnings("WeakAccess unused")
public class AgentRate3Controller extends Base3Common {
	private static final Logger log = LoggerFactory.getLogger(Limit3Controller.class);
	private ObjectMapper mapper = new ObjectMapper();
	@Autowired
	private AppProperties appProperties;
	@Autowired
	private SystemSettingService systemSettingService;
	@Autowired
	private RedisService redisService;

	@RequestMapping(value = "/agentRate", method = RequestMethod.POST, consumes = "application/json")
	@ResponseBody
	public SimpleResponseData rate(@Valid @RequestBody ReqV3Rate requestBody, BindingResult result)
			throws JsonProcessingException {
		String paramBody = mapper.writeValueAsString(requestBody);
		log.info("RateV3 >> RequestBody:{}", paramBody);
		if (result.hasErrors()) {
			log.debug("RateV3 >> param invalid. RequestBody:{}", paramBody);
			return ERROR_PARAM_INVALID;
		}
		if (!checkToken(requestBody, paramBody)) {
			return ERROR_TOKEN_INVALID;
		}
		if (requestBody.getItems().stream().filter(p -> p.getAmount() < 0 || p.getRate() < 0 || p.getRate() > 10
				|| p.getUplimit() != null && p.getUplimit() < 0).count() > 0) {
			log.debug(
					"RateV3 >> amount is less than 0 , rate is less than 0 or greater than 10 ,uplimit is less than 0. RequestBody:{}",
					paramBody);
			return ERROR_RATE_AMOUNT_RATE_ERROR;
		}
		Set<Float> amountSet = new HashSet<>();
		for (ReqV3RateItem item : requestBody.getItems()) {
			if (amountSet.contains(item.getAmount())) {
				log.debug("RateV3 >> collection contains same amount . same amount: {}  RequestBody:{}",
						item.getAmount(), paramBody);
				return ERROR_RATE_SAME_AMOUNT;
			}
			amountSet.add(item.getAmount());
		}
		requestBody.getItems().sort((o1, o2) -> {
			float o3 = o1.getAmount() - o2.getAmount();
			return (o3 > 0) ? 1 : (o3 < 0 ? -1 : 0);
		});
		String value = mapper.writeValueAsString(requestBody.getItems());
		List<SysUserProfile> settingList = systemSettingService
				.findByPropertyKey(UserProfileKey.REBATE_AGENT_SYS_RATE_SETTING.getValue()).stream()
				.filter(p -> Objects.equals(p.getUserId(), 1)).collect(Collectors.toList());
		SysUserProfile profile = CollectionUtils.isEmpty(settingList) ? null : settingList.get(0);
		if (Objects.isNull(profile)) {
			profile = new SysUserProfile();
			profile.setUserId(AppConstants.USER_ID_4_ADMIN);
			profile.setPropertyKey(UserProfileKey.REBATE_AGENT_SYS_RATE_SETTING.getValue());
			profile.setPropertyName("返利网代理返利设置");
		}
		profile.setPropertyValue(value);
		profile = systemSettingService.saveSetting(profile);
		redisService.convertAndSend(RedisTopics.REFRESH_SYSTEM_PROFILE, mapper.writeValueAsString(profile));
		return SUCCESS;
	}

	/**
	 * check whether token is valid.
	 * <p>
	 * calculate target : amount+acc+logid+tid+salt.
	 *
	 * @return {@code true} pass checkpoint {@code false } non-pass checkpoint
	 */
	private boolean checkToken(ReqV3Rate arg0, String paramBody) {
		if (StringUtils.isBlank(arg0.getToken())) {
			log.debug("RateV3 >> param 'token' is empty|null. RequestBody:{}", paramBody);
			return false;
		}
		if (CollectionUtils.isEmpty(arg0.getItems())) {
			log.debug("RateV3 >> param 'items' is empty. RequestBody:{}", paramBody);
			return false;
		}
		List<ReqV3RateItem> items = arg0.getItems().stream().sorted((o1, o2) -> {
			float o3 = o1.getAmount() - o2.getAmount();
			return (o3 > 0) ? 1 : (o3 < 0 ? -1 : 0);
		}).collect(Collectors.toList());
		StringBuilder sb = new StringBuilder();
		items.forEach(p -> sb.append(trans2Radix(p.getAmount()).toString()).append(trans2Radix(p.getRate()))
				.append(Objects.isNull(p.getUplimit()) || p.getUplimit() == 0 ? StringUtils.EMPTY
						: trans2Radix(p.getUplimit())));
		String oriContent = sb.toString();
		String calToken = CommonUtils.md5digest(oriContent + appProperties.getRebatesalt());
		if (StringUtils.equals(calToken, arg0.getToken()))
			return true;
		log.debug("RateV3 >> invalid token. oriCtn: {}  oriTkn: {} calTkn: {}", oriContent, arg0.getToken(), calToken);
		return false;
	}
}
