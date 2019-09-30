package com.xinbo.fundstransfer.restful.v3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppProperties;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.enums.BankLogStatus;
import com.xinbo.fundstransfer.domain.enums.IncomeRequestStatus;
import com.xinbo.fundstransfer.domain.enums.IncomeRequestType;
import com.xinbo.fundstransfer.domain.enums.OutwardTaskStatus;
import com.xinbo.fundstransfer.restful.v3.pojo.ReqV3Limit;
import com.xinbo.fundstransfer.service.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v3")
@SuppressWarnings("WeakAccess unused")
public class Limit3Controller extends Base3Common {
	private static final Logger log = LoggerFactory.getLogger(Limit3Controller.class);
	private ObjectMapper mapper = new ObjectMapper();
	@Autowired
	private AppProperties appProperties;
	@Autowired
	private BankLogService bankLogService;
	@Autowired
	private AccountService accountService;
	@Autowired
	private AccountMoreService accountMoreService;
	@Autowired
	private RebateApiService rebateApiService;
	@Autowired
	IncomeRequestService incomeRequestService;

	@RequestMapping(value = "/limit", method = RequestMethod.POST, consumes = "application/json")
	@ResponseBody
	public SimpleResponseData limit(@Valid @RequestBody ReqV3Limit requestBody, BindingResult result)
			throws JsonProcessingException {
		String paramBody = mapper.writeValueAsString(requestBody);
		log.info("LimitV3 >> RequestBody:{}", paramBody);
		if (result.hasErrors()) {
			log.debug("LimitV3 >> invalid params. RequestBody:{}", paramBody);
			return ERROR_PARAM_INVALID;
		}
		if (!checkToken(requestBody)) {
			return ERROR_TOKEN_INVALID;
		}
		BigDecimal calLimit = BigDecimal.ZERO;

		List<BizAccount> accList = accountService.findByAccount(StringUtils.trimToEmpty(requestBody.getAcc()));
		accList = accList.stream().filter(p -> AccountType.isBank(p.getType())).collect(Collectors.toList());
		if (CollectionUtils.isEmpty(accList)) {
			log.info("LimitV3 >> account doesn't exist . acc: {} RequestBody:{}", requestBody.getAcc(), paramBody);
			return ERROR_LIMIT_NO_ACCOUNT;
		}
		if (accList.size() > 1) {
			log.info("LimitV3 >> account isn't unique . acc: {} RequestBody:{}", requestBody.getAcc(), paramBody);
			return ERROR_LIMIT_DUPLICATE_ACCOUNT;
		}
		// proccess
		BizAccount acc = accList.get(0);
		BizIncomeRequest o = new BizIncomeRequest();
		o.setToId(acc.getId());
		o.setHandicap(acc.getHandicapId());
		o.setLevel(null);
		o.setToAccount(acc.getAccount());
		BigDecimal amount = new BigDecimal(requestBody.getAmount()).setScale(2, RoundingMode.HALF_UP).abs();
		o.setAmount(amount);
		o.setCreateTime(new Date());
		o.setUpdateTime(o.getCreateTime());
		o.setOrderNo(requestBody.getTid());
		o.setRemark(null);
		o.setType(IncomeRequestType.RebateLimit.getType());
		o.setFromAccount(null);
		o.setMemberCode(null);
		o.setMemberUserName(null);
		o.setMemberRealName(null);
		o.setStatus(IncomeRequestStatus.Matching.getStatus());
		o.setFee(BigDecimal.ZERO);
		BizIncomeRequest request = incomeRequestService.update(o);
		// 如果没有匹配且金额>0且为整数 且为兼职提额单 去查找是否有提额流水
		if (request.getAmount().intValue() == request.getAmount().floatValue()
				&& Objects.equals(request.getType(), IncomeRequestType.RebateLimit.getType())) {
			SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			// 根据金额，时间，状态，from_account查询流水。
			BizBankLog bankLog = bankLogService.findRebateLimitBankLog(request.getToId(), request.getAmount(),
					sd.format((DateUtils.addHours(request.getCreateTime(), -2))),
					sd.format((DateUtils.addHours(request.getCreateTime(), 2))));
			// 查询是否有转出同样金额 没有匹配的流水 如果有则不匹配，因为存在 兼职恶意转入 转出 在转入的情况 进行重复提额
			BizBankLog bl = bankLogService.findRebateLimitBankLog(request.getToId(),
					request.getAmount().multiply(new BigDecimal("-1")),
					sd.format((DateUtils.addHours(request.getCreateTime(), -24))),
					sd.format((DateUtils.addHours(request.getCreateTime(), 24))));

			if (Objects.nonNull(bankLog) && Objects.isNull(bl)) {
				// 更改为匹配状态、调用提额的接口
				rebateApiService.ackCreditLimit(bankLog, request);
				// 更改为匹配状态、调用提额的接口
				bankLogService.updateStatusRm(bankLog.getId(), BankLogStatus.Matched.getStatus(), "兼职提额流水");
				incomeRequestService.updateStatusById(request.getId(), OutwardTaskStatus.Matched.getStatus());
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
