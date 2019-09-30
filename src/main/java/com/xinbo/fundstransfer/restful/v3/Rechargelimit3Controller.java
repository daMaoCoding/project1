package com.xinbo.fundstransfer.restful.v3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppProperties;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.restful.v3.pojo.ReqV3Rechargelimit;
import com.xinbo.fundstransfer.service.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping("/api/v3")
@SuppressWarnings("WeakAccess unused")
public class Rechargelimit3Controller extends Base3Common {
	private static final Logger log = LoggerFactory.getLogger(Rechargelimit3Controller.class);
	private ObjectMapper mapper = new ObjectMapper();
	@Autowired
	private AppProperties appProperties;
	@Autowired
	private AccountMoreService accountMoreService;
	@Autowired
	private RebateApiService rebateApiService;

	@RequestMapping(value = "/rechargelimit", method = RequestMethod.POST, consumes = "application/json")
	@ResponseBody
	public SimpleResponseData rechargelimit(@Valid @RequestBody ReqV3Rechargelimit requestBody, BindingResult result)
			throws JsonProcessingException {
		String paramBody = mapper.writeValueAsString(requestBody);
		log.info("RechargelimitV3 >> RequestBody:{}", paramBody);
		if (result.hasErrors()) {
			log.info("RechargelimitV3 >> invalid params. RequestBody:{}", paramBody);
			return ERROR_PARAM_INVALID;
		}
		if (!checkToken(requestBody)) {
			return ERROR_TOKEN_INVALID;
		}
		BizAccountMore accMore = accountMoreService.getFromByUid(requestBody.getUid());
		if (null == accMore) {
			log.info("RechargelimitV3 >> 用户不存在 . uid: {} RequestBody:{}", requestBody.getUid(), paramBody);
			return ERROR_WITHDRAWAL_USER_DOESNT_EXIST;
		}
		// 确保之前的信用额度一致
		if (accMore.getMargin()
				.compareTo((new BigDecimal(requestBody.getBalance()).setScale(2, RoundingMode.HALF_UP))) != 0) {
			log.info("RechargelimitV3 >> 信用额度不相同 . uid: {} RequestBody:{}", requestBody.getUid(), paramBody);
			return ERROR_RATE_NOTSAME_MARGIN;
		}
		rebateApiService.ackRechargelimit(accMore, requestBody.getAmount(), requestBody.getTid());
		return SUCCESS;
	}

	/**
	 * check whether token is valid.
	 * <p>
	 * calculate target : amount+uid+balance+tid+salt.
	 *
	 * @return {@code true} pass checkpoint {@code false } non-pass checkpoint
	 */
	private boolean checkToken(ReqV3Rechargelimit arg0) {
		if (StringUtils.isBlank(arg0.getToken())) {
			log.debug("RechargelimitV3 >> param token is empty|null. amount: {} uid: {} balance: {} tid: {}",
					arg0.getAmount(), arg0.getUid(), arg0.getBalance(), arg0.getTid());
			return false;
		}
		String oriContent = trans2Radix(arg0.getAmount()).toString() + arg0.getUid()
				+ trans2Radix(arg0.getBalance()).toString() + arg0.getTid();
		String calToken = CommonUtils.md5digest(oriContent + appProperties.getRebatesalt());
		if (StringUtils.equals(calToken, arg0.getToken()))
			return true;
		log.debug("LimitV3 >> invalid token. amount: {} uid: {} balance: {} tid: {} oriCtn: {}  oriTkn: {} calTkn: {}",
				arg0.getAmount(), arg0.getUid(), arg0.getBalance(), arg0.getTid(), oriContent, arg0.getToken(),
				calToken);
		return false;
	}
}
