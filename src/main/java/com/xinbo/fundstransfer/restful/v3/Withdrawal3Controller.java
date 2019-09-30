package com.xinbo.fundstransfer.restful.v3;

import java.math.BigDecimal;
import java.util.Date;
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
import com.xinbo.fundstransfer.AppProperties;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizAccountMore;
import com.xinbo.fundstransfer.domain.entity.BizAccountRebate;
import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import com.xinbo.fundstransfer.domain.enums.BankEnums;
import com.xinbo.fundstransfer.domain.enums.OutwardTaskStatus;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.restful.v3.pojo.ReqV3Withdrawal;
import com.xinbo.fundstransfer.service.AccountMoreService;
import com.xinbo.fundstransfer.service.AccountRebateService;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.RebateUserActivityService;

@RestController
@RequestMapping("/api/v3")
@SuppressWarnings("WeakAccess unused")
public class Withdrawal3Controller extends Base3Common {
	private static final Logger log = LoggerFactory.getLogger(Limit3Controller.class);
	private ObjectMapper mapper = new ObjectMapper();
	@Autowired
	private AccountMoreService accountMoreService;
	@Autowired
	private AccountRebateService accountRebateService;
	@Autowired
	private AccountService accSer;
	@Autowired
	private AppProperties appProperties;
	@Autowired
	private RebateUserActivityService rebateUserActivityService;

	@RequestMapping(value = "/withdrawal", method = RequestMethod.POST, consumes = "application/json")
	@ResponseBody
	public SimpleResponseData withdrawal(@Valid @RequestBody ReqV3Withdrawal requestBody, BindingResult result)
			throws JsonProcessingException {
		String paramBody = mapper.writeValueAsString(requestBody);
		log.info("WithdrawalV3 >> RequestBody:{}", paramBody);
		if (result.hasErrors()) {
			log.info("WithdrawalV3 >>  invalid params. RequestBody:{} err{}", paramBody, ERROR_PARAM_INVALID);
			return ERROR_PARAM_INVALID;
		}
		if (!checkToken(requestBody)) {
			log.info("WithdrawalV3 >> invalid token. RequestBody:{} err{}", paramBody, ERROR_TOKEN_INVALID);
			return ERROR_TOKEN_INVALID;
		}
		if (!rebateUserActivityService.allowWithdrawal(requestBody.getUid())) {
			log.info("WithdrawalV3 >> 兼职在活动中 不能进行提现！. RequestBody:{} err{}", paramBody, ERROR_TOKEN_INVALID);
			return new SimpleResponseData(0, " 兼职在活动中 不能进行提现");
		}
		// 处理不同步上线，没有这个参数过来
		if (Objects.isNull(requestBody.getType()) || requestBody.getType() == 0)
			requestBody.setType(1);
		BizAccountMore more = accountMoreService.getFromByUid(requestBody.getUid());
		if (Objects.isNull(more)) {
			log.info("WithdrawalV3 >> the user 'uid' {} doesn't exist. RequestBody:{}  err{}", requestBody.getUid(),
					paramBody, ERROR_WITHDRAWAL_USER_DOESNT_EXIST);
			return ERROR_WITHDRAWAL_USER_DOESNT_EXIST;
		}
		// 如果是降额 退的钱不能超过所有卡的押金金额
		if (requestBody.getType() == 2) {
			if (new BigDecimal(requestBody.getBalance()).compareTo(more.getMargin()) != 0) {
				log.info("WithdrawalV3 >> 额度不一致降额失败. RequestBody:{} err{}", paramBody, "额度不一致降额失败");
				return new SimpleResponseData(601, "额度不一致降额失败");
			}
			if (requestBody.getAmount() > more.getLinelimit().floatValue()) {
				log.info("WithdrawalV3 >> 退额超过可用额度. RequestBody:{} err{}", paramBody, "退额超过可用额度");
				return new SimpleResponseData(601, "退额超过可用额度");
			}
			if (more.getMargin().floatValue() - requestBody.getAmount() < 1000) {
				log.info("WithdrawalV3 >> 降额后 额度不能低于1000. RequestBody:{} err{}", paramBody, "降额后 额度不能低于1000");
				return new SimpleResponseData(601, "降额后 额度不能低于1000");
			}
		}

		BigDecimal fdBalance = more.getBalance();
		if (requestBody.getType() == 1 && Objects.isNull(fdBalance)) {
			log.info("WithdrawalV3 >> the user 'uid' {} 's balance is null. RequestBody:{}  err{}",
					requestBody.getUid(), paramBody, ERROR_WITHDRAWAL_BALANCE_IS_NULL);
			return ERROR_WITHDRAWAL_BALANCE_IS_NULL;
		}
		BigDecimal reBalance = trans2Radix(requestBody.getBalance());
		if (requestBody.getType() == 1 && fdBalance.compareTo(reBalance) != 0) {
			log.info(
					"WithdrawalV3 >> the user 'uid' {} 's balance is invalid . fdBalance:{} reBalance:{}  RequestBody:{} err{}",
					requestBody.getUid(), fdBalance, reBalance, paramBody,
					ERROR_WITHDRAWAL_BALANCE_WRONG + ">>>>同步可提现余额！");
			// log.info(
			// "WithdrawalV3 >> the user 'uid' {} 's balance is invalid .
			// fdBalance:{} reBalance:{} RequestBody:{} err{}",
			// requestBody.getUid(), fdBalance, reBalance, paramBody,
			// ERROR_WITHDRAWAL_BALANCE_WRONG);
			// return ERROR_WITHDRAWAL_BALANCE_WRONG;
		}
		if (requestBody.getType() == 1 && fdBalance.compareTo(reBalance) < 0) {
			log.info(
					"WithdrawalV3 >> amount can't be greater than balance . amount:{} balance:{}  RequestBody:{} err{}",
					reBalance, reBalance, paramBody, ERROR_WITHDRAWAL_AMOUNT_GREATER_THAN_BALANCE);
			return ERROR_WITHDRAWAL_AMOUNT_GREATER_THAN_BALANCE;
		}
		BigDecimal reAmount = trans2Radix(requestBody.getAmount());
		if (reAmount.intValue() > 10000 && requestBody.getType() == 1) {
			log.info("WithdrawalV3 >> amount can't exceed 10000 every time . amount:{}  RequestBody:{} err{}", reAmount,
					paramBody, ERROR_WITHDRAWAL_AMOUNT_EXCEED_10000);
			return ERROR_WITHDRAWAL_AMOUNT_EXCEED_10000;
		}
		if (reBalance.compareTo(reAmount) < 0 && requestBody.getType() == 1) {
			log.info(
					"WithdrawalV3 >> amount can't be greater than balance . amount:{} balance:{}  RequestBody:{} err{}",
					reAmount, reBalance, paramBody, ERROR_WITHDRAWAL_AMOUNT_GREATER_THAN_BALANCE);
			return ERROR_WITHDRAWAL_AMOUNT_GREATER_THAN_BALANCE;
		}
		BizAccountRebate rebate = new BizAccountRebate();
		if (StringUtils.isNotBlank(more.getAccounts())) {
			for (String idStr : more.getAccounts().split(",")) {
				if (StringUtils.isNotBlank(idStr) && StringUtils.isNumeric(idStr)) {
					AccountBaseInfo base = accSer.getFromCacheById(Integer.valueOf(idStr));
					if (Objects.isNull(base) || Objects.isNull(base.getHandicapId())
							|| base.getStatus() == AccountStatus.Inactivated.getStatus()) {
						continue;
					}
					rebate.setHandicap(base.getHandicapId());
					break;
				}
			}
		}
		if (Objects.isNull(rebate.getHandicap())) {
			log.info("WithdrawalV3 >> can't find a handicap  . amount:{} balance:{}  RequestBody:{} err{}", reAmount,
					reBalance, paramBody, ERROR_WITHDRAWAL_CAL_HANDICAP_ERROR);
			return ERROR_WITHDRAWAL_CAL_HANDICAP_ERROR;
		}
		BankEnums bank = BankEnums.findByLog(requestBody.getAcctype());
		rebate.setUid(more.getUid());
		rebate.setTid(StringUtils.trim(requestBody.getTid()));
		rebate.setAccountId(null);
		rebate.setToAccount(StringUtils.trimToEmpty(requestBody.getToacc()));
		rebate.setToHolder(StringUtils.trimToEmpty(requestBody.getHolder()));
		rebate.setToAccountType(
				Objects.isNull(bank) ? StringUtils.trimToEmpty(requestBody.getAcctype()) : bank.getDesc());
		rebate.setToAccountInfo(StringUtils.trimToEmpty(requestBody.getAccinfo()));
		rebate.setBalance(reBalance);
		rebate.setAmount(reAmount);
		if (requestBody.getType() == 1) {
			rebate.setStatus(OutwardTaskStatus.Undeposit.getStatus());
		} else {
			rebate.setStatus(888);
		}
		rebate.setCreateTime(new Date());
		rebate.setUpdateTime(rebate.getCreateTime());
		rebate.setRemark(null);
		if (Objects.nonNull(requestBody.getType())) {
			rebate.setType(requestBody.getType());
		}
		accountRebateService.create(more, rebate);
		return SUCCESS;
	}

	/**
	 * check whether token is valid.
	 * <p>
	 * calculate target : uid+toacc+amount+balance+tid+salt.
	 *
	 * @return {@code true} pass checkpoint {@code false } non-pass checkpoint
	 */
	private boolean checkToken(ReqV3Withdrawal arg0) {
		if (StringUtils.isBlank(arg0.getToken())) {
			log.debug("WithdrawalV3 >> param 'token' is empty|null. uid: {} toacc: {} amount: {} balance: {} tid: {}",
					arg0.getUid(), arg0.getToacc(), arg0.getAmount(), arg0.getBalance(), arg0.getTid());
			return false;
		}
		String oriContent = "";
		if (Objects.isNull(arg0.getType()) || arg0.getType() == 0) {
			oriContent = StringUtils.trimToEmpty(arg0.getUid()) + StringUtils.trimToEmpty(arg0.getToacc())
					+ trans2Radix(arg0.getAmount()) + trans2Radix(arg0.getBalance())
					+ StringUtils.trimToEmpty(arg0.getTid());
		} else {
			oriContent = StringUtils.trimToEmpty(arg0.getUid()) + StringUtils.trimToEmpty(arg0.getToacc())
					+ trans2Radix(arg0.getAmount()) + trans2Radix(arg0.getBalance())
					+ StringUtils.trimToEmpty(arg0.getTid()) + arg0.getType();
		}
		String calToken = CommonUtils.md5digest(oriContent + appProperties.getRebatesalt());
		if (StringUtils.equals(calToken, arg0.getToken()))
			return true;
		log.debug(
				"WithdrawalV3 >> invalid token. uid: {} toacc: {} amount: {} balance: {} tid: {} oriCtn: {}  oriTkn: {} calTkn: {}",
				arg0.getUid(), arg0.getToacc(), arg0.getAmount(), arg0.getBalance(), arg0.getTid(), oriContent,
				arg0.getToken(), calToken);
		return false;
	}
}
