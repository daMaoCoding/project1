package com.xinbo.fundstransfer.restful.v3;

import java.util.Objects;

import javax.validation.Valid;

import com.xinbo.fundstransfer.domain.entity.BizAccountMore;
import com.xinbo.fundstransfer.domain.entity.BizOtherAccount;
import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import com.xinbo.fundstransfer.restful.v3.pojo.ReqV3Mobile;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppProperties;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.service.*;

@RestController
@RequestMapping("/api/v3")
@SuppressWarnings("WeakAccess unused")
public class Mobile3Controller extends Base3Common {
	private static final Logger log = LoggerFactory.getLogger(Mobile3Controller.class);
	private ObjectMapper mapper = new ObjectMapper();
	@Autowired
	private AppProperties appProperties;
	@Autowired
	private BankLogService bankLogService;
	@Autowired
	private AccountService accountService;
	@Autowired
	private AccountMoreService accountMoreSer;
	@Autowired
	private RebateApiService rebateApiService;
	@Autowired
	IncomeRequestService incomeRequestService;
	@Autowired
	private QuickPayService quickPayService;

	@RequestMapping(value = "/mobile", method = RequestMethod.POST, consumes = "application/json")
	@ResponseBody
	public SimpleResponseData mobile(@Valid @RequestBody ReqV3Mobile requestBody, BindingResult result)
			throws JsonProcessingException {
		String paramBody = mapper.writeValueAsString(requestBody);
		log.info("MobileV3 >> RequestBody:{}", paramBody);
		if (result.hasErrors()) {
			log.debug("MobileV3 >> invalid params. RequestBody:{}", paramBody);
			return ERROR_PARAM_INVALID;
		}
		if (!checkToken(requestBody)) {
			return ERROR_TOKEN_INVALID;
		}
		BizAccountMore more = accountMoreSer.getFromCacheByUid(requestBody.getUid());
		if (Objects.isNull(more)) {
			log.info("MobileV3Error >> message:{} RequestBody:{}", ERROR_MOBILE_MORE_NONEXIST, paramBody);
			return ERROR_MOBILE_MORE_NONEXIST;
		}
		if (!Objects.equals(more.getMoible(), requestBody.getHistory())) {
			log.info("MobileV3Error >> message:{} RequestBody:{}", ERROR_MOBILE_DIFF_HISTORY, paramBody);
			return ERROR_MOBILE_DIFF_HISTORY;
		}
		// 如果银行卡激活了，或者云闪付激活了 则不允许修改手机号
		BizOtherAccount ysfAcc = quickPayService.getByUid(Integer.parseInt(more.getUid()));
		if (null == ysfAcc) {
			// 没有绑定云闪付 则只校验银行卡
			for (String accId : StringUtils.trimToEmpty(more.getAccounts()).split(",")) {
				if (StringUtils.isBlank(accId) || !StringUtils.isNumeric(accId))
					continue;
				BizAccount acc = accountService.getById(Integer.valueOf(accId));
				// 如果有账号激活了 则不能修改
				if (acc.getStatus() != AccountStatus.Inactivated.getStatus()) {
					log.info("MobileV3Error >> 卡号已经激活不能修改. acc:{}", acc.getAccount());
					return new SimpleResponseData(-1, "卡号已经激活不能修改" + acc.getAccount());
				}
			}
		} else {
			for (String accId : StringUtils.trimToEmpty(more.getAccounts()).split(",")) {
				if (StringUtils.isBlank(accId) || !StringUtils.isNumeric(accId))
					continue;
				BizAccount acc = accountService.getById(Integer.valueOf(accId));
				// 如果有账号激活了 则不能修改
				if (acc.getStatus() != AccountStatus.Inactivated.getStatus()) {
					log.info("MobileV3Error >> 卡号已经激活不能修改. acc:{}", acc.getAccount());
					return new SimpleResponseData(-1, "卡号已经激活不能修改" + acc.getAccount());
				}
			}
			if (ysfAcc.getStatus() != 3) {
				log.info("MobileV3Error >> 云闪付账号已激活不能修改. acc:{}", ysfAcc.getAccountNo());
				return new SimpleResponseData(-1, "云闪付账号已激活不能修改" + ysfAcc.getAccountNo());
			}
		}
		if (null != ysfAcc) {
			ysfAcc.setAccountNo(requestBody.getMobile());
			BizOtherAccount quickAcc = quickPayService.save(ysfAcc);
			quickPayService.flushCache(quickAcc);
		}
		accountMoreSer.updateMobileByUid(more.getUid(), requestBody.getMobile());
		for (String accId : StringUtils.trimToEmpty(more.getAccounts()).split(",")) {
			if (StringUtils.isBlank(accId) || !StringUtils.isNumeric(accId))
				continue;
			BizAccount acc = accountService.getById(Integer.valueOf(accId));
			if (Objects.isNull(acc) || Objects.equals(acc.getMobile(), requestBody.getMobile())
					|| !Objects.equals(acc.getFlag(), 2))
				continue;
			acc.setMobile(requestBody.getMobile());
			accountService.updateBaseInfo(acc);
			accountService.broadCast(acc);
		}
		return SUCCESS;
	}

	/**
	 * check whether token is valid.
	 * <p>
	 * calculate target : history+mobile+uid+salt.
	 *
	 * @return {@code true} pass checkpoint {@code false } non-pass checkpoint
	 */
	private boolean checkToken(ReqV3Mobile arg0) {
		if (StringUtils.isBlank(arg0.getToken())) {
			log.debug("MobileV3 >> param token is empty|null. history: {} mobile: {} uid: {}", arg0.getHistory(),
					arg0.getMobile(), arg0.getUid());
			return false;
		}
		String oriContent = StringUtils.trimToEmpty(arg0.getHistory()) + StringUtils.trimToEmpty(arg0.getMobile())
				+ StringUtils.trimToEmpty(arg0.getUid());
		String calToken = CommonUtils.md5digest(oriContent + appProperties.getRebatesalt());
		if (StringUtils.equals(calToken, arg0.getToken()))
			return true;
		log.debug("MobileV3 >> invalid token. history: {} mobile: {} uid: {} oriCtn: {}  oriTkn: {} calTkn: {}",
				arg0.getHistory(), arg0.getMobile(), arg0.getUid(), oriContent, arg0.getToken(), calToken);
		return false;
	}
}
