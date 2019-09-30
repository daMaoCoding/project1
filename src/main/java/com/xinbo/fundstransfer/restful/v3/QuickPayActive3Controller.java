package com.xinbo.fundstransfer.restful.v3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppProperties;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.entity.BizAccountMore;
import com.xinbo.fundstransfer.domain.entity.BizOtherAccount;
import com.xinbo.fundstransfer.domain.entity.BizRebateUser;
import com.xinbo.fundstransfer.restful.v3.pojo.ReqV3Active;
import com.xinbo.fundstransfer.service.AccountMoreService;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.AllocateTransService;
import com.xinbo.fundstransfer.service.CabanaService;
import com.xinbo.fundstransfer.service.QuickPayService;
import com.xinbo.fundstransfer.service.RebateApiService;
import com.xinbo.fundstransfer.service.RebateUserService;
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
public class QuickPayActive3Controller extends Base3Common {
	private static final Logger log = LoggerFactory.getLogger(QuickPayActive3Controller.class);
	private ObjectMapper mapper = new ObjectMapper();
	@Autowired
	private AppProperties appProperties;
	@Autowired
	private QuickPayService quickPayService;
	@Autowired
	private CabanaService cabanaService;
	@Autowired
	private RebateUserService rebateUserService;
	@Autowired
	private AccountMoreService accMoreSer;

	/**
	 * active acc.
	 */
	@RequestMapping(value = "/qpactive", method = RequestMethod.POST, consumes = "application/json")
	@ResponseBody
	public Object qpactive(@Valid @RequestBody ReqV3Active requestBody, BindingResult result)
			throws JsonProcessingException {
		String paramBody = mapper.writeValueAsString(requestBody);
		log.info("QuickPayActiveV3 >> RequestBody:{}", paramBody);
		if (result.hasErrors()) {
			log.debug("QuickPayActiveV3 >> invalid params. RequestBody:{}", paramBody);
			return ERROR_PARAM_INVALID;
		}
		if (!checkToken(requestBody))
			return ERROR_TOKEN_INVALID;
		BizOtherAccount otheracc = quickPayService.getFromCacheByAccountNo(requestBody.getAcc());
		if (null == otheracc) {
			log.info("QuickPayActiveV3 >> 云闪付账号不存在！  RequestBody:{}", paramBody);
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "云闪付账号不存在。"));
		}
		BizRebateUser rebateUser = rebateUserService.getFromCacheByUid(otheracc.getUid() + "");
		if (null == rebateUser) {
			log.info("QuickPayActiveV3 >> 兼职不存在！  RequestBody:{}", paramBody);
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "兼职不存在。"));
		}
		BizAccountMore more = accMoreSer.getFromCacheByUid(rebateUser.getUid());
		if (null == more || null == more.getMoible()) {
			log.info("QuickPayActiveV3 >> 兼职不存在！  RequestBody:{}", paramBody);
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "兼职不存在。"));
		}
		cabanaService.activeQuickPay(StringUtils.lowerCase(rebateUser.getUserName()), otheracc.getAccountNo(),
				otheracc.getLoginPwd(), more.getMoible());
		return SUCCESS;
	}

	private boolean checkToken(ReqV3Active arg0) {
		if (StringUtils.isBlank(arg0.getToken())) {
			log.info("QuickPayActiveV3 ( invalid token ) >> token param is null. ");
			return false;
		}
		String oriContent = StringUtils.trimToEmpty(arg0.getAcc());
		String calToken = CommonUtils.md5digest(oriContent + appProperties.getRebatesalt());
		if (StringUtils.equals(calToken, arg0.getToken()))
			return true;
		log.info("QuickPayActiveV3 ( invalid token ) >>  oriContent: {}  oriToken: {} calToekn: {}", oriContent,
				arg0.getToken(), calToken);
		return false;
	}
}
