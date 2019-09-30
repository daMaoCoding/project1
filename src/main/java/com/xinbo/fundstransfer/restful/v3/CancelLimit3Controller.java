package com.xinbo.fundstransfer.restful.v3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppProperties;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.restful.v3.pojo.ReqV3CancelLimit;
import com.xinbo.fundstransfer.restful.v3.pojo.ReqV3Limit;
import com.xinbo.fundstransfer.service.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/v3")
@SuppressWarnings("WeakAccess unused")
public class CancelLimit3Controller extends Base3Common {
	private static final Logger log = LoggerFactory.getLogger(CancelLimit3Controller.class);
	private ObjectMapper mapper = new ObjectMapper();
	@Autowired
	private AppProperties appProperties;
	@Autowired
	IncomeRequestService incomeRequestService;
	@Autowired
	private AccountRebateService accountRebateService;

	@RequestMapping(value = "/cancelLimit", method = RequestMethod.POST, consumes = "application/json")
	@ResponseBody
	public SimpleResponseData limit(@Valid @RequestBody ReqV3CancelLimit requestBody, BindingResult result)
			throws JsonProcessingException {
		String paramBody = mapper.writeValueAsString(requestBody);
		log.info("cancelLimit >> RequestBody:{}", paramBody);
		if (result.hasErrors()) {
			log.debug("cancelLimit >> invalid params. RequestBody:{}", paramBody);
			return ERROR_PARAM_INVALID;
		}
		if (requestBody.getType() == 1) {
			// 提额单取消
			BizIncomeRequest incom = incomeRequestService.findUnmatchedListByOrderNo(requestBody.getTid()).get(0);
			if (null != incom && incom.getStatus() == 0) {
				incom.setStatus(3);
				incomeRequestService.saveAndFlush(incom);
			} else {
				log.info("cancelLimit >> 取消提额单不存在. RequestBody:{}", paramBody);
				return new SimpleResponseData(0, "任务不存在或者状态不是匹配中");
			}
		} else if (requestBody.getType() == 2) {
			// 降额单取消
			BizAccountRebate rebate = accountRebateService.findLatestByTid(requestBody.getTid());
			if (null != rebate && rebate.getStatus() == 888) {
				rebate.setStatus(999);
				accountRebateService.saveAndFlush(rebate);
			} else {
				log.info("cancelLimit >> 取消降额单不存在. RequestBody:{}", paramBody);
				return new SimpleResponseData(0, "任务不存在或者状态不是匹配中");
			}
		}
		return SUCCESS;
	}

	/**
	 * check whether token is valid.
	 * <p>
	 * calculate target : amount+acc+logid+tid+salt.
	 *
	 * @return {@code true} pass checkpoint {@code false } non-pass checkpoint
	 */
	private boolean checkToken(ReqV3Limit arg0) {
		if (StringUtils.isBlank(arg0.getToken())) {
			log.debug("LimitV3 >> param token is empty|null. amount: {} acc: {} tid: {}", arg0.getAmount(),
					arg0.getAcc(), arg0.getTid());
			return false;
		}
		String oriContent = trans2Radix(arg0.getAmount()).toString() + StringUtils.trimToEmpty(arg0.getAcc())
				+ arg0.getTid();
		String calToken = CommonUtils.md5digest(oriContent + appProperties.getRebatesalt());
		if (StringUtils.equals(calToken, arg0.getToken()))
			return true;
		log.debug("LimitV3 >> invalid token. amount: {} acc: {} tid: {} oriCtn: {}  oriTkn: {} calTkn: {}",
				arg0.getAmount(), arg0.getAcc(), arg0.getTid(), oriContent, arg0.getToken(), calToken);
		return false;
	}
}
