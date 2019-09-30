package com.xinbo.fundstransfer.service.impl;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.AppProperties;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.chatPay.commons.enums.RedisKeyEnums;
import com.xinbo.fundstransfer.chatPay.commons.enums.RedisLockKeyEnums;
import com.xinbo.fundstransfer.component.common.TimeChangeCommon;
import com.xinbo.fundstransfer.component.net.http.v3.RebateHttpClient;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.DeductAmountResponseData;
import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.*;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.TransferEntity;
import com.xinbo.fundstransfer.domain.repository.*;
import com.xinbo.fundstransfer.restful.v3.pojo.ResV3DailyLogItem;
import com.xinbo.fundstransfer.runtime.task.ToolResponseData;
import com.xinbo.fundstransfer.restful.api.pojo.ApiDeductionAmount;
import com.xinbo.fundstransfer.restful.api.pojo.ApiWechatLog;
import com.xinbo.fundstransfer.restful.v3.pojo.ReqV3RateItem;
import com.xinbo.fundstransfer.service.*;
import okhttp3.MediaType;
import okhttp3.RequestBody;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.util.CollectionUtils;
import org.redisson.api.RLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class RebateApiServiceImpl implements RebateApiService {
	private static final Logger log = LoggerFactory.getLogger(RebateApiServiceImpl.class);
	@Autowired
	private AppProperties appPros;
	@Autowired
	private AllocateIncomeAccountService allocInAccSer;
	@Autowired
	private SysUserProfileService userProSer;
	@Autowired
	private AccountRebateDayRepository rebateDayDao;
	@Autowired
	private AccountReturnSummaryRepository returnSummaryDao;
	@Autowired
	private AccountRepository accDao;
	@Autowired
	private BankLogRepository bankLogDao;
	@Autowired
	@Lazy
	private AccountService accSer;
	@Autowired
	private AccountRebateRepository accRebateDao;
	@Autowired
	private RedisService redisSer;
	@Autowired
	TransactionLogService transLogService;
	@Autowired
	private BankLogRepository bankLogRepository;
	@Autowired
	private BankLogService bankLogService;
	@Autowired
	private TransactionLogService transactionLogService;
	@Autowired
	private AccountRebateDayService accRebateDaySer;
	@Autowired
	private AccountMoreService accMoreSer;
	@Autowired
	private RebateUserService rebateUserService;
	@Autowired
	AccountMoreRepository accountMoreDao;

	private ObjectMapper mapper = new ObjectMapper();
	private static final String CONSTANT_TOKEN_VARIABLE = "token";
	private static final String CONSTANT_HTTP_MEDIA_TYPE = "application/json";

	/**
	 * a timer to compute part-time duty personal's daily commission.
	 */
	// @Scheduled(fixedRate = 3600000)
	public void schedule4CommissionDaily() {
		if (!allocInAccSer.checkHostRunRight()) {
			log.trace(
					"CommissionDaily >>  this host {} doesn't have Right to compute part-time duty personal's daily commission.",
					CommonUtils.getInternalIp());
			return;
		}
		commissionDailyReturnsummary(valOfCommissionTime(), 0);
	}

	@Scheduled(fixedRate = 5000)
	public void commissionDaily() {
		if (!allocInAccSer.checkHostRunRight())
			return;
		List<String> failList = new ArrayList<>();
		try {
			while (true) {
				String entity = (String) redisSer.leftPop(RedisTopics.REBATE_DAY);
				if (Objects.isNull(entity))
					break;
				RebateHttpClient.getInstance().getPlatformServiceApi().rebateDaily(buildReqBody(entity))
						.subscribe(d -> {
							if (d.getStatus() == 1)
								log.info("CommissionDaily >> send [ SUCCESS ] data: {}", entity);
							else
								log.error("CommissionDaily >> send [FAIL ] {} data: {}", d.getMessage(), entity);
						}, e -> {
							if (StringUtils.isBlank(e.getMessage()) || !e.getMessage().contains("HTTP 500")) {
								log.error("CommissionDaily >> errorExp  data: {} e:", entity, e);
								failList.add(entity);
							} else {
								log.error("CommissionDaily >> [ HTTP500 ] data: {}", entity);
							}
						});
			}
		} catch (Exception e) {
			log.error("CommissionDaily >> finalExp", e);
		} finally {
			for (String fail : failList)
				redisSer.rightPush(RedisTopics.REBATE_DAY, fail);
		}
	}

	/**
	 * @param commissionTime
	 *            time format :'yyyy-MM-dd'
	 */
	@Override
	public void commissionDaily(String commissionTime) {
		List<ReqV3RateItem> rateList = buildRate();
		if (CollectionUtils.isEmpty(rateList)) {
			log.debug(
					"CommissionDaily >>  this host {} has no need to compute commission ,due to rate configuration is null|empty.",
					CommonUtils.getInternalIp());
			return;
		}
		if (StringUtils.isBlank(commissionTime)) {
			log.trace(
					"CommissionDaily >>  this host {} has no need to compute commission ,due to commissionTime is null|empty.",
					CommonUtils.getInternalIp());
			return;
		}
		try {
			log.info("CommissionDaily >>  Host: {} Time: {} . [ START ]", CommonUtils.getInternalIp(), commissionTime);
			Date[] startAndEnd = startAndEndTimeOfCommission(commissionTime);
			if (Objects.isNull(startAndEnd) || startAndEnd.length != 2) {
				log.trace(
						"CommissionDaily >>  this host {} time:{}  has no need to compute commission ,because start time and end time is null.",
						CommonUtils.getInternalIp(), commissionTime);
				return;
			}
			Date start = startAndEnd[0], end = startAndEnd[1];
			List<Integer> allList = accDao.findAccountId4Rebate();
			List<Integer> doneList = rebateDayDao.findAccountByCalcTime(commissionTime);
			// 查询这次返利的黑名单
			List<Integer> blackList = accRebateDao.findBlackList(commissionTime);
			boolean result = true;
			for (Integer accId : allList) {
				if (Objects.isNull(accId) || doneList.contains(accId)) {
					log.trace("CommissionDaily >> host: {} accId: {} time: {}  already compute.",
							CommonUtils.getInternalIp(), accId, commissionTime);
					continue;
				}
				if (Objects.isNull(accId) || blackList.contains(accId)) {
					log.info("当天不返利的兼职账号 >> host: {} accId: {} time: {}  already compute.", CommonUtils.getInternalIp(),
							accId, commissionTime);
					continue;
				}
				List<BizBankLog> logList = bankLogDao.findRebateRecord(accId, start, end);
				if (CollectionUtils.isEmpty(logList))
					continue;
				AccountBaseInfo base = accSer.getFromCacheById(accId);
				BizAccountMore more = accountMoreDao.findByMoible(base.getMobile());
				// 判断是否零信用额度，如果是零信用额度 判断是否存在多张卡，如果有多张卡 只能返50
				boolean flage = false;
				if (null != more.getMargin() && more.getMargin().compareTo(new BigDecimal(1000)) == 0) {
					String[] account = more.getAccounts().split(",");
					List<Integer> tmp = new ArrayList<Integer>();
					for (String str : account) {
						if (str != null && str.length() != 0) {
							tmp.add(Integer.parseInt(str));
						}
					}
					if (tmp.size() > 1) {
						doneList = rebateDayDao.findAccountByCalcTime(commissionTime);
						// 查询是否已经有其它卡已经返佣了，如果其它卡已经返佣50则不能继续返利
						for (Integer acId : tmp) {
							if (doneList.contains(acId)) {
								flage = true;
								log.info("零信用额度只能返佣50,uid:" + more.getUid());
								break;
							}
						}
					}
				}
				if (flage)
					continue;
				ReqV3RateItem rate = buildCommisson(rateList, more);
				if (Objects.isNull(rate)) {
					log.trace("CommissionDaily >> host: {} accId: {} time: {}   rate is null|empty.",
							CommonUtils.getInternalIp(), accId, commissionTime);
					continue;
				}
				List<ResV3DailyLogItem> tidList = new ArrayList<>();
				BigDecimal total = BigDecimal.ZERO, amount = BigDecimal.ZERO, reallyAmount = BigDecimal.ZERO;
				for (BizBankLog log : logList) {
					ResV3DailyLogItem daily = new ResV3DailyLogItem(log.getId(), log.getAmount(), log.getCommission());
					total = total.add(log.getAmount());
					amount = amount
							.add(null == daily.getCashbackAmount() ? new BigDecimal("0") : daily.getCashbackAmount());
					reallyAmount = amount;
					tidList.add(daily);
				}
				// 如果是第一档 多余50给50 不够50给50 如果超过第一档不够50给50
				if (Objects.nonNull(rate.getUplimit()) && rate.getUplimit() > 0) {
					amount = new BigDecimal(rate.getUplimit());
					// amount = amount.floatValue() > rate.getUplimit() ? new
					// BigDecimal(rate.getUplimit()) : amount;
				} else {
					amount = amount.floatValue() > 50 ? amount : new BigDecimal("50");
				}
				// 如果不是零信用额度的，当前账号返利的是50 则需要查看是否已经有其它账号返利50 如果有则只能返利一个50
				boolean fla = false;
				if (null != more.getMargin() && more.getMargin().compareTo(new BigDecimal(1000)) == 1
						&& amount.compareTo(new BigDecimal(50)) == 0) {
					String[] account = more.getAccounts().split(",");
					// 其它账号
					List<Integer> tmp = new ArrayList<Integer>();
					for (String str : account) {
						if (str != null && str.length() != 0 && Integer.parseInt(str) != accId) {
							tmp.add(Integer.parseInt(str));
						}
					}
					if (tmp.size() > 0) {
						// 查询是否已经有其它卡已经返佣了，如果其它卡已经返佣50则不能继续返利
						doneList = rebateDayDao.findAccountByCalcTime5(commissionTime, tmp);
						// 其它账号返利金
						BigDecimal rebateAmounts = bankLogDao.findRebateBanks(tmp, start, end);
						if (rebateAmounts.compareTo(new BigDecimal("50")) == 1)
							amount = reallyAmount;
						if (doneList.size() > 0) {
							fla = true;
							log.info("只能一个账号返佣50,uid:" + more.getUid());
						}
					}
				}
				if (fla)
					continue;
				// 返利时候 查询是否存在需要扣除的返佣，存在则需要扣除
				BizDeductAmount deductAmount = rebateUserService.deductAmountByUid(more.getUid());
				if (null != deductAmount && deductAmount.getAmount().compareTo(BigDecimal.ZERO) == 1) {
					// 如果返佣多余需要扣除的返佣
					if (amount.compareTo(deductAmount.getAmount()) == 1) {
						deductAmount(more.getUid(), deductAmount.getAmount(), "0", base.getAccount(), "返利时候扣除返佣");
						amount = amount.subtract(deductAmount.getAmount());
						String remarks = "从" + base.getAlias() + "返佣扣除佣金:" + deductAmount.getAmount();
						deductAmount
								.setRemark(CommonUtils.genRemark(deductAmount.getRemark(), remarks, new Date(), "系统"));
						deductAmount.setAmount(BigDecimal.ZERO);
						rebateUserService.saveDeductAmount(deductAmount);
					} else if (amount.compareTo(deductAmount.getAmount()) == 0) {// 返佣和扣除的相等
						deductAmount(more.getUid(), deductAmount.getAmount(), "0", base.getAccount(), "返利时候扣除返佣");
						String remarks = "从" + base.getAlias() + "返佣扣除佣金:" + deductAmount.getAmount();
						deductAmount
								.setRemark(CommonUtils.genRemark(deductAmount.getRemark(), remarks, new Date(), "系统"));
						deductAmount.setAmount(BigDecimal.ZERO);
						rebateUserService.saveDeductAmount(deductAmount);
						amount = BigDecimal.ZERO;
					} else {// 返佣小于扣除的佣金
						deductAmount(more.getUid(), amount, "0", base.getAccount(), "返利时候扣除返佣");
						deductAmount.setAmount(deductAmount.getAmount().subtract(amount));
						String remarks = "从" + base.getAlias() + "返佣扣除佣金:" + amount;
						deductAmount
								.setRemark(CommonUtils.genRemark(deductAmount.getRemark(), remarks, new Date(), "系统"));
						rebateUserService.saveDeductAmount(deductAmount);
						amount = BigDecimal.ZERO;
					}
				}
				total = total.abs();
				amount = amount.abs();
				boolean ret = commissionDaily(more, base.getAccount(), total, amount, commissionTime, tidList, accId);
				result = result && ret;
			}
			// if (result) {
			// valOfCommissionTime(commissionTime);
			// }
		} catch (Exception e) {
			log.info("CommissionDaily >>  Host: {} Time: {} . [ ERROR ] : {}", CommonUtils.getInternalIp(),
					commissionTime, e);
		}
	}

	@Override
	public void commissionDailyReturnsummary(String commissionTime, int type) {
		List<ReqV3RateItem> rateList = buildRate();
		if (CollectionUtils.isEmpty(rateList)) {
			log.debug(
					"commissionDailyReturnsummary >>  this host {} has no need to compute commission ,due to rate configuration is null|empty.",
					CommonUtils.getInternalIp());
			return;
		}
		if (StringUtils.isBlank(commissionTime)) {
			log.trace(
					"commissionDailyReturnsummary >>  this host {} has no need to compute commission ,due to commissionTime is null|empty.",
					CommonUtils.getInternalIp());
			return;
		}
		try {
			log.info("commissionDailyReturnsummary >>  Host: {} Time: {} . [ START ]", CommonUtils.getInternalIp(),
					commissionTime);
			Date[] startAndEnd = startAndEndTimeOfCommission(commissionTime);
			if (Objects.isNull(startAndEnd) || startAndEnd.length != 2) {
				log.trace(
						"commissionDailyReturnsummary >>  this host {} time:{}  has no need to compute commission ,because start time and end time is null.",
						CommonUtils.getInternalIp(), commissionTime);
				return;
			}
			Date start = startAndEnd[0], end = startAndEnd[1];
			List<Integer> allList = accDao.findAccountId4Rebate();
			List<Integer> doneList = returnSummaryDao.findReturnSummaryByCalcTime(commissionTime);
			boolean result = true;
			for (Integer accId : allList) {
				if (Objects.isNull(accId) || (doneList.contains(accId) && type == 0)) {
					log.trace("commissionDailyReturnsummary >> host: {} accId: {} time: {}  already compute.",
							CommonUtils.getInternalIp(), accId, commissionTime);
					continue;
				}
				List<BizBankLog> logList = bankLogDao.findRebateRecord(accId, start, end);
				if (CollectionUtils.isEmpty(logList))
					continue;
				AccountBaseInfo base = accSer.getFromCacheById(accId);
				BizAccountMore more = accountMoreDao.findByMoible(base.getMobile());
				// 判断是否零信用额度，如果是零信用额度 判断是否存在多张卡，如果有多张卡 只能返50
				boolean flage = false;
				if (null != more.getMargin() && more.getMargin().compareTo(new BigDecimal(1000)) == 0) {
					String[] account = more.getAccounts().split(",");
					List<Integer> tmp = new ArrayList<Integer>();
					for (String str : account) {
						if (str != null && str.length() != 0) {
							tmp.add(Integer.parseInt(str));
						}
					}
					if (tmp.size() > 1) {
						doneList = returnSummaryDao.findReturnSummaryByCalcTime(commissionTime);
						// 查询是否已经有其它卡已经返佣了，如果其它卡已经返佣50则不能继续返利
						for (Integer acId : tmp) {
							if (doneList.contains(acId)) {
								flage = true;
								log.info("零信用额度只能返佣50,uid:" + more.getUid());
								break;
							}
						}
					}
				}
				if (flage)
					continue;
				ReqV3RateItem rate = buildCommisson(rateList, more);
				if (Objects.isNull(rate)) {
					log.trace("commissionDailyReturnsummary >> host: {} accId: {} time: {}   rate is null|empty.",
							CommonUtils.getInternalIp(), accId, commissionTime);
					continue;
				}
				List<ResV3DailyLogItem> tidList = new ArrayList<>();
				BigDecimal total = BigDecimal.ZERO, amount = BigDecimal.ZERO, reallyAmount = BigDecimal.ZERO;
				for (BizBankLog log : logList) {
					ResV3DailyLogItem daily = new ResV3DailyLogItem(log.getId(), log.getAmount(), log.getCommission());
					total = total.add(log.getAmount());
					amount = amount
							.add(null == daily.getCashbackAmount() ? new BigDecimal("0") : daily.getCashbackAmount());
					reallyAmount = amount;
					tidList.add(daily);
				}
				// 如果是第一档 多余50给50 不够50给50 如果超过第一档不够50给50
				if (Objects.nonNull(rate.getUplimit()) && rate.getUplimit() > 0) {
					amount = new BigDecimal(rate.getUplimit());
					// amount = amount.floatValue() > rate.getUplimit() ? new
					// BigDecimal(rate.getUplimit()) : amount;
				} else {
					amount = amount.floatValue() > 50 ? amount : new BigDecimal("50");
				}
				// 如果不是零信用额度的，当前账号返利的是50 则需要查看其它账号是否返利超过50.如果超过了则当前账号只能返利对应的利率
				boolean fla = false;
				if (null != more.getMargin() && more.getMargin().compareTo(new BigDecimal(1000)) == 1
						&& amount.compareTo(new BigDecimal(50)) == 0) {
					String[] account = more.getAccounts().split(",");
					// 其它账号
					List<Integer> tmp = new ArrayList<Integer>();
					for (String str : account) {
						if (str != null && str.length() != 0 && Integer.parseInt(str) != accId) {
							tmp.add(Integer.parseInt(str));
						}
					}
					if (tmp.size() > 0) {
						// 查询是否已经有其它卡已经返佣超过50了 如果有则不能返利50
						doneList = returnSummaryDao.findReturnSummaryByCalcTime5(commissionTime, tmp);
						// 其它账号返利金
						BigDecimal rebateAmounts = bankLogDao.findRebateBanks(tmp, start, end);
						if (rebateAmounts.compareTo(new BigDecimal("50")) == 1)
							amount = reallyAmount;
						if (doneList.size() > 0) {
							fla = true;
							log.info("只能一个账号返佣50,uid:" + more.getUid());
						}
					}
				}
				if (fla)
					continue;
				// 返利时候 查询是否存在需要扣除的返佣，存在则需要扣除
				BizDeductAmount deductAmount = rebateUserService.deductAmountByUid(more.getUid());
				if (null != deductAmount && deductAmount.getAmount().compareTo(BigDecimal.ZERO) == 1) {
					// 如果返佣多余需要扣除的返佣
					if (amount.compareTo(deductAmount.getAmount()) == 1) {
						amount = amount.subtract(deductAmount.getAmount());
					} else if (amount.compareTo(deductAmount.getAmount()) == 0) {// 返佣和扣除的相等
						amount = BigDecimal.ZERO;
					} else {// 返佣小于扣除的佣金
						amount = BigDecimal.ZERO;
					}
				}
				total = total.abs();
				amount = amount.abs();
				boolean ret = commissionDailyReturnsummary(more, base.getAccount(), total, amount, commissionTime,
						tidList, accId, doneList, type);
				result = result && ret;
			}
			if (result) {
				valOfCommissionTime(commissionTime);
			}
		} catch (Exception e) {
			log.info("commissionDailyReturnsummary >>  Host: {} Time: {} . [ ERROR ] : {}", CommonUtils.getInternalIp(),
					commissionTime, e);
		}
	}

	/**
	 * part-time duty personal's bank account commission daily
	 *
	 * @param acc
	 *            part-time personal's bank account
	 * @param total
	 *            bank statement stroke amount
	 * @param amount
	 *            commission
	 * @param time
	 *            calculate time ,time format 'yyyy-MM-dd'
	 * @return {@code true} revoke successfully ,otherwise, {@code false}
	 */
	@Override
	public boolean commissionDaily(BizAccountMore more, String acc, BigDecimal total, BigDecimal amount, String time,
			List<ResV3DailyLogItem> tid, int accId) {
		String accTrim = StringUtils.trimToEmpty(acc);
		total = total.setScale(2, RoundingMode.HALF_UP);
		amount = amount.setScale(2, RoundingMode.HALF_UP);
		BigDecimal balance = Objects.isNull(more.getBalance()) ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
				: more.getBalance().setScale(2, RoundingMode.HALF_UP);
		BizAccount account = accDao.findById2(accId);
		BigDecimal bankBalance = account.getBankBalance();
		bankBalance = Objects.isNull(bankBalance) ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
				: bankBalance.setScale(2, RoundingMode.HALF_UP);
		/*
		 * encrypt content : acc+total+amount+balance+time+tid; encryption way
		 * :MD5
		 */
		String preCtn = accTrim + total + amount + balance + bankBalance + time;
		log.info(
				"Approve4VerifyCommission CommissionDaily >> pre-data  acc: {} total: {} amount: {} balance: {} realTimeBalance: {}  time:{} tid: {} preCtn: {} UID:{}",
				accTrim, total, amount, balance, bankBalance, time, tid, preCtn, more.getUid());
		Map<String, Object> param = new HashMap<>();
		param.put("acc", accTrim);
		param.put("total", total);
		param.put("amount", amount);
		param.put("balance", balance);
		param.put("realTimeBalance", bankBalance);
		param.put("time", time);
		param.put("items", tid);
		param.put("uid", more.getUid());
		param.put("token", CommonUtils.md5digest(preCtn + appPros.getRebatesalt()));
		try {
			redisSer.rightPush(RedisTopics.REBATE_DAY, mapper.writeValueAsString(param));
			more.setBalance(Objects.isNull(more.getBalance()) ? amount : more.getBalance().add(amount));
			accMoreSer.saveAndFlash(more);
			BizAccountRebateDay rebateDay = new BizAccountRebateDay();
			rebateDay.setAccount(accId);
			rebateDay.setTotalAmount(total);
			rebateDay.setAmount(amount);
			rebateDay.setActivityAmount(null);
			rebateDay.setCreateTime(new Date());
			rebateDay.setCalcTime(time);
			accRebateDaySer.saveAndFlash(rebateDay);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	@Override
	@Transactional
	public boolean commissionDailyReturnsummary(BizAccountMore more, String acc, BigDecimal total, BigDecimal amount,
			String time, List<ResV3DailyLogItem> tid, int accId, List<Integer> doneList, int type) {
		total = total.setScale(2, RoundingMode.HALF_UP);
		amount = amount.setScale(2, RoundingMode.HALF_UP);
		BigDecimal balance = Objects.isNull(more.getBalance()) ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
				: more.getBalance().setScale(2, RoundingMode.HALF_UP);
		try {
			// 如果存在 则是点了重新审核 需要保持备注一致
			if (doneList.contains(accId)) {
				accRebateDao.updateAmount(time, accId, total, amount);
			} else {
				BizAccountReturnSummary rebateDay = new BizAccountReturnSummary();
				rebateDay.setAccount(accId);
				rebateDay.setTotalAmount(total);
				rebateDay.setAmount(amount);
				rebateDay.setCreateTime(new Date());
				rebateDay.setCalcTime(time);
				String[] account = more.getAccounts().split(",");
				List<Integer> tmp = new ArrayList<Integer>();
				for (String str : account) {
					if (str != null && str.length() != 0) {
						tmp.add(Integer.parseInt(str));
					}
				}
				// 记录每天的可提现额度，只允许一个账号加上可提现额度，其它账号只加当前返利金额，统计的时候会sum，如果都加上 有问题。
				boolean flage = true;
				if (tmp.size() > 1) {
					doneList = returnSummaryDao.findReturnSummaryByCalcTime(time);
					for (Integer acId : tmp) {
						if (doneList.contains(acId)) {
							flage = false;
							break;
						}
					}
				}
				if (flage)
					rebateDay.setBalance(balance.add(amount));
				else
					rebateDay.setBalance(amount);
				String oldRemark = accRebateDao.findRemark(time);
				// 如果是重新计算 且以前没有统计到 需要保持备注一致
				if (null != oldRemark && !"".equals(oldRemark)) {
					String oldStatus = accRebateDao.findStatus(time);
					rebateDay.setRemark(oldRemark);
					rebateDay.setStatus(Integer.parseInt(oldStatus));
				} else {
					rebateDay.setStatus(0);
				}
				accRebateDaySer.saveAndFlash(rebateDay);
			}
		} catch (Exception e) {
			log.error("commissionDailyReturnsummary计算错误", e);
			return false;
		}
		return true;
	}

	@Scheduled(fixedRate = 10000)
	protected void logs() {
		if (!allocInAccSer.checkHostRunRight())
			return;
		List<String> failList = new ArrayList<>();
		try {
			while (true) {
				String entity = (String) redisSer.leftPop(RedisTopics.REBATE_BANK_LOGS);
				if (Objects.isNull(entity))
					break;
				RebateHttpClient.getInstance().getPlatformServiceApi().log(buildReqBody(entity)).subscribe(d -> {
					if (d.getStatus() == 1)
						log.info("LogsRemitTask >> send [ SUCCESS ] data: {}", entity);
					else
						log.error("LogsRemitTask >> send [FAIL ] {} data: {}", d.getMessage(), entity);
				}, e -> {
					if (StringUtils.isBlank(e.getMessage()) || !e.getMessage().contains("HTTP 500")) {
						log.error("LogsRemitTask >> errorExp  data: {}", entity, e);
						failList.add(entity);
					} else {
						log.error("LogsRemitTask >> [ HTTP500 ] data: {}", entity);
					}
				});
			}
		} catch (Exception e) {
			log.error("LogsRemitTask >> finalExp", e);
		} finally {
			for (String fail : failList)
				redisSer.rightPush(RedisTopics.REBATE_BANK_LOGS, fail);
		}
	}

	private static final int BS_Fee = BankLogStatus.Fee.getStatus();
	private static final int BS_Refunding = BankLogStatus.Refunding.getStatus();
	private static final int BS_Refunded = BankLogStatus.Refunded.getStatus();
	private static final int BS_Interest = BankLogStatus.Interest.getStatus();
	private static final String ZERO_00 = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP).toString();

	@Override
	public void logs(List<BizBankLog> logs) {
		if (CollectionUtils.isEmpty(logs))
			return;
		StringBuilder preContent = new StringBuilder();
		List<Map<String, Object>> dataList = new ArrayList<>();
		List<BizBankLog> chgList = new ArrayList<>();
		List<ReqV3RateItem> rateList = buildRate();
		List<ReqV3RateItem> agentRateList = buildAgentRate();
		logs.forEach(p -> {
			AccountBaseInfo base = accSer.getFromCacheById(p.getFromAccount());
			BizAccountMore more = accMoreSer.getFromCacheByMobile(base.getMobile());
			if (Objects.nonNull(more) && Objects.equals(base.getFlag(), AccountFlag.REFUND.getTypeId())) {
				Map<String, Object> item = new HashMap<>();
				if (Objects.nonNull(p.getId())) {
					item.put("id", p.getId());
					preContent.append(p.getId());
				}
				if (StringUtils.isNotBlank(base.getAccount())) {
					item.put("acc", base.getAccount().trim());
					preContent.append(base.getAccount().trim());
				}
				if (Objects.nonNull(p.getAmount())) {
					BigDecimal amount = p.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP);
					item.put("amount", amount);
					preContent.append(amount);
				}
				if (StringUtils.isNotBlank(p.getToAccount())) {
					item.put("toacc", p.getToAccount().trim());
					preContent.append(p.getToAccount().trim());
				}
				if (StringUtils.isNotBlank(p.getToAccountOwner())) {
					item.put("toholder", p.getToAccountOwner().trim());
					preContent.append(p.getToAccountOwner().trim());
				}
				if (Objects.nonNull(p.getTradingTime())) {
					String time = CommonUtils.getDateStr(p.getTradingTime());
					item.put("time", time);
					preContent.append(time);
				}
				if (Objects.nonNull(p.getCreateTime())) {
					String catchTime = CommonUtils.getDateStr(p.getCreateTime());
					item.put("catchTime", catchTime);
					preContent.append(catchTime);
				} else {
					String catchTime = CommonUtils.getDateStr(new Date());
					item.put("catchTime", catchTime);
					preContent.append(catchTime);
				}
				if (StringUtils.isNotBlank(p.getSummary())) {
					item.put("summary", p.getSummary().trim());
				}
				item.put("commission", "0");
				int st = p.getStatus();
				if (!Objects.equals(base.getStatus(), AccountStatus.Inactivated.getStatus()) && st != BS_Fee
						&& st != BS_Refunding && st != BS_Refunded && st != BS_Interest
						&& p.getAmount().compareTo(BigDecimal.ZERO) < 0) {
					ReqV3RateItem rateItem = null;
					if (null != more.getAgent() && more.getAgent() && null != more.getMargin()
							&& more.getMargin().compareTo(new BigDecimal("30000")) > -1) {
						rateItem = buildCommisson(agentRateList, more);
					} else {
						rateItem = buildCommisson(rateList, more);
					}
					if (Objects.nonNull(rateItem)) {
						ResV3DailyLogItem daily = new ResV3DailyLogItem(p.getId(), p.getAmount(), rateItem);
						// 如果是一千以下的押金 一天的流水返利不能超过设置的值目前是50
						if (Objects.nonNull(rateItem.getUplimit()) && rateItem.getUplimit() > 0) {
							String start = TimeChangeCommon.getToday5StartTime(),
									end = TimeChangeCommon.getToday5EndTime();
							BigDecimal totalAmount = bankLogDao.findTotalAmount(p.getFromAccount(), start, end);
							if (totalAmount.floatValue() > rateItem.getUplimit()) {
								daily.setCashbackAmount(new BigDecimal("0"));
							} else if ((daily.getCashbackAmount().floatValue() + totalAmount.floatValue()) > rateItem
									.getUplimit()) {
								daily.setCashbackAmount(new BigDecimal("50").subtract(totalAmount));
							}
						}
						if (daily.getCashbackAmount().compareTo(BigDecimal.ZERO) > 0) {
							p.setCommission(daily.getCashbackAmount());
							item.put("commission", daily.getCashbackAmount());
							preContent.append(daily.getCashbackAmount());
							chgList.add(p);
						} else {
							preContent.append(ZERO_00);
						}
					} else {
						preContent.append(ZERO_00);
					}
				} else {
					preContent.append(ZERO_00);
				}
				dataList.add(item);
			}
		});
		for (BizBankLog log : chgList)
			bankLogService.updateCsById(log.getId(), log.getCommission());
		if (dataList.size() == 0)
			return;
		String data = null;
		Map<String, Object> param = new HashMap<>();
		param.put("items", dataList);
		param.put("token", CommonUtils.md5digest(preContent.toString() + appPros.getRebatesalt()));
		try {
			data = mapper.writeValueAsString(param);
			log.info("LogsRemitTask >> send data: {} preCtn: {}", data, preContent.toString());
			redisSer.rightPush(RedisTopics.REBATE_BANK_LOGS, data);
		} catch (Exception e) {
			log.info("LogsRemit >> error: {}", data);
		}
	}

	/**
	 * confirm bank account credit limit for part-time duty personal.
	 *
	 * @param bankLog
	 *            bank statement
	 * @param req
	 *            upgrade limit request
	 */
	@Override
	public void ackCreditLimit(BizBankLog bankLog, BizIncomeRequest req) {
		if (Objects.isNull(bankLog) || Objects.isNull(req)) {
			log.info("数据为空！accountId {}", bankLog.getFromAccount());
			return;
		}
		BizAccount accout = accSer.getById(req.getToId());
		if (Objects.isNull(accout)) {
			log.info("账号为空！accountId {}", req.getToId());
			return;
		}
		BizAccountMore more = accMoreSer.findByMobile(accout.getMobile());
		if (Objects.isNull(more)) {
			log.info("根据手机号找不到数据！Mobile {}", accout.getMobile());
			return;
		}
		try {
			BigDecimal amount = bankLog.getAmount();
			BigDecimal lPeek = Objects.nonNull(more.getMargin()) ? more.getMargin() : BigDecimal.ZERO;
			BigDecimal rPeak = BigDecimal.ZERO;
			{
				BigDecimal tmpPeek = Objects.isNull(accout.getPeakBalance()) ? BigDecimal.ZERO
						: new BigDecimal(accout.getPeakBalance());
				tmpPeek = tmpPeek.add(bankLog.getAmount());
				accout.setPeakBalance(tmpPeek.intValue());
				accSer.updateBaseInfo(accout);
				accSer.broadCast(accout);
			}
			if (lPeek.compareTo(new BigDecimal(1000)) == 0) {
				BigDecimal init = BigDecimal.ZERO;
				for (String accId : StringUtils.trimToEmpty(more.getAccounts()).split(",")) {
					if (StringUtils.isBlank(accId) || !StringUtils.isNumeric(accId))
						continue;
					BizAccount acc = accSer.getById(Integer.valueOf(accId));
					if (Objects.isNull(acc.getPeakBalance()) || acc.getPeakBalance() < 0)
						acc.setPeakBalance(1);
					if (Objects.nonNull(acc))
						init = init.add(Objects.isNull(acc.getPeakBalance()) ? BigDecimal.ZERO
								: new BigDecimal(acc.getPeakBalance()));
				}
				if (init.compareTo(new BigDecimal(1000)) > 0) {
					rPeak = init;
				}
				rPeak = init.setScale(2, RoundingMode.HALF_UP);
			} else {
				rPeak = lPeek.add(amount).setScale(2, RoundingMode.HALF_UP);
			}
			String gen = String.format("%s 额度提升 %s [%s %s]", CommonUtils.transToStarString(accout.getAccount()),
					amount.toString(), lPeek.toString(), rPeak.toString());
			more.setMargin(rPeak);
			more.setLinelimit(
					(Objects.isNull(more.getLinelimit()) ? BigDecimal.ZERO : more.getLinelimit()).add(amount));
			more.setRemark(CommonUtils.genRemark(more.getRemark(), gen, new Date(), "REB"));
			accMoreSer.saveAndFlash(more);
			String tid = req.getOrderNo();
			/*
			 * encrypt content : acc +amount+ tid +logId+ salt; encryption way
			 * :MD5
			 */
			String preCtn = StringUtils.trimToEmpty(accout.getAccount()) + amount.setScale(2, RoundingMode.HALF_UP)
					+ rPeak + StringUtils.trimToEmpty(tid) + bankLog.getId();
			log.info("LimitAct >> pre-data  acc: {} amount: {} tid: {} preCtn: {}", accout.getAccount(), rPeak, tid,
					preCtn);
			HashMap<String, Object> params = new HashMap<>();
			params.put("acc", StringUtils.trimToEmpty(accout.getAccount()));
			params.put("amount", amount.setScale(2, RoundingMode.HALF_UP));
			params.put("balance", rPeak.setScale(2, RoundingMode.HALF_UP));
			params.put("tid", StringUtils.trimToEmpty(tid));
			params.put("logId", bankLog.getId());
			params.put(CONSTANT_TOKEN_VARIABLE, CommonUtils.md5digest(preCtn + appPros.getRebatesalt()));
			redisSer.rightPush(RedisTopics.REBATE_UP_LIMIT, mapper.writeValueAsString(params));
		} catch (Exception e) {
			log.error("LimitActError", e);
		}
	}

	/**
	 * confirm bank account credit limit for part-time duty personal.
	 *
	 * @param acc
	 *            part-time duty personal's bank account
	 * @param amount
	 *            bank account credit limit
	 * @return {@code true} confirm success ,otherwise, {@code false}
	 */
	@Override
	public boolean ackCreditLimit(String acc, BigDecimal amount, String tid) {
		if (StringUtils.isBlank(acc) || Objects.isNull(amount))
			return false;
		SimpleResponseData[] ret = new SimpleResponseData[1];
		BigDecimal limit = amount.setScale(2, RoundingMode.HALF_UP);
		/* encrypt content : acc +amount+ tid + salt; encryption way :MD5 */
		String preCtn = StringUtils.trimToEmpty(acc) + "0.00" + limit + StringUtils.trimToEmpty(tid);
		log.debug("LimitAct >> pre-data  acc: {} amount: {} tid: {} preCtn: {}", acc, limit, tid, preCtn);
		Map<String, Object> param = new HashMap<>();
		param.put("acc", StringUtils.trimToEmpty(acc));
		// 初始化信用额度 提额/降额为0
		param.put("amount", new BigDecimal("0.00"));
		param.put("balance", limit);
		param.put("tid", StringUtils.trimToEmpty(tid));
		param.put(CONSTANT_TOKEN_VARIABLE, CommonUtils.md5digest(preCtn + appPros.getRebatesalt()));
		RebateHttpClient.getInstance().getPlatformServiceApi().limitAck(buildReqBody(param)).subscribe(d -> {
			ret[0] = d;
			if (d.getStatus() != 1) {
				log.error("LimitAct >> revoke Rebate System fail. acc: {} amount: {} tid: {} error: {}", acc, limit,
						tid, d.getMessage());
			}
		}, e -> log.error("LimitAct >> revoke Rebate System error. acc: {} amount: {} tid: {}  e: {}", acc, limit, tid,
				e));
		boolean result = ret.length != 0 && Objects.nonNull(ret[0]) && ret[0].getStatus() == 1;
		if (result) {
			log.info("LimitAct >> revoke Rebate System successfully. acc: {} amount: {} tid: {}", acc, limit, tid);
		}
		return result;
	}

	@Scheduled(fixedRate = 1000)
	public void ackCreditLimit() {
		if (!allocInAccSer.checkHostRunRight())
			return;
		List<String> failList = new ArrayList<>();
		try {
			while (true) {
				String entity = (String) redisSer.leftPop(RedisTopics.REBATE_UP_LIMIT);
				if (Objects.isNull(entity))
					break;
				RebateHttpClient.getInstance().getPlatformServiceApi().limitAck(buildReqBody(entity)).subscribe(d -> {
					log.info("LimitAct >> send data: {}", entity);
					if (d.getStatus() == 1)
						log.info("LimitAct >> send [ SUCCESS ] data: {}", entity);
					else
						log.error("LimitAct >> send [FAIL ] {} data: {}", d.getMessage(), entity);
				}, e -> {
					if (StringUtils.isBlank(e.getMessage()) || !e.getMessage().contains("HTTP 500")) {
						log.error("LimitAct >> revoke Rebate System error. data:{}  e: {}", entity, e);
						failList.add(entity);
					} else {
						log.error("LimitAct >> [ HTTP500 ] data: {}", entity);
					}
				});
			}
		} catch (Exception e) {
			log.error("LimitAct >> finalExp", e);
		} finally {
			for (String item : failList)
				redisSer.rightPush(RedisTopics.REBATE_UP_LIMIT, item);
		}
	}

	/**
	 * confirm part-time duty personal commission remittance
	 *
	 * @param uid
	 *            part-time user identity
	 * @param amount
	 *            remittance amount
	 * @param balance
	 *            total commission
	 * @param msg
	 *            remark
	 * @param tid
	 *            Rebate System business id
	 * @return {@code true} confirm success ,otherwise, {@code false}
	 */
	@Override
	public boolean ackRemittance(String uid, BigDecimal amount, BigDecimal balance, String tid, String msg) {
		if (StringUtils.isBlank(uid) || StringUtils.isBlank(tid) || Objects.isNull(amount) || Objects.isNull(balance)) {
			log.error("AckRemit >> param is empty|null .  uid: {} amount: {} balance: {} tid: {} msg: {}", uid, amount,
					balance, tid, msg);
			return true;
		}
		if (amount.compareTo(BigDecimal.ZERO) <= 0) {
			log.error(
					"AckRemit >> param 'amount' can't be less than 0 .  uid: {} amount: {} balance: {} tid: {} msg: {}",
					uid, amount, balance, tid, msg);
			return true;
		}
		BigDecimal amountRadix2 = amount.setScale(2, RoundingMode.HALF_UP);
		BigDecimal balanceRadix2 = balance.setScale(2, RoundingMode.HALF_UP);
		SimpleResponseData[] ret = new SimpleResponseData[1];
		/*
		 * encrypt content : uid +amount+ result + msg+tid+salt; encryption
		 * way:MD5
		 */
		String preCtn = StringUtils.trimToEmpty(uid) + amountRadix2 + balanceRadix2 + StringUtils.trimToEmpty("已完成")
				+ StringUtils.trimToEmpty(tid);
		log.info("LimitAct >> pre-data  uid: {} amount: {} balance: {} tid: {} msg: {}  preCtn: {}", uid, amount,
				balance, tid, msg, preCtn);
		Map<String, Object> param = new HashMap<>();
		param.put("uid", uid);
		param.put("amount", amountRadix2.toString());
		param.put("balance", balanceRadix2.toString());
		param.put("msg", "已完成");
		param.put("tid", StringUtils.trimToEmpty(tid));
		param.put(CONSTANT_TOKEN_VARIABLE, CommonUtils.md5digest(preCtn + appPros.getRebatesalt()));
		RebateHttpClient.getInstance().getPlatformServiceApi().withdrawalAck(buildReqBody(param)).subscribe(d -> {
			ret[0] = d;
			if (d.getStatus() != 1) {
				log.error(
						"AckRemit >> revoke Rebate System fail.  uid: {} amount: {} balance: {} tid: {} msg: {} error: {}",
						uid, amountRadix2, balanceRadix2, tid, msg, d.getMessage());
			}
		}, e -> log.error(
				"AckRemit >> revoke Rebate System error.  uid: {} amount: {} balance: {} tid: {} msg: {} error: {}",
				uid, amountRadix2, balanceRadix2, tid, msg, e));
		boolean result = ret.length != 0 && Objects.nonNull(ret[0]) && ret[0].getStatus() == 1;
		if (result) {
			log.info("AckRemit >> revoke Rebate System successfully.  uid: {} amount: {} balance: {} tid: {} msg: {}",
					uid, amountRadix2, balanceRadix2, tid, msg);
		}
		return result;
	}

	/**
	 * cancel part-time duty personal commission remittance
	 *
	 * @param uid
	 *            part-time user identity
	 * @param amount
	 *            remittance amount
	 * @param balance
	 *            total commission
	 * @param tid
	 *            Rebate System business id
	 * @param msg
	 *            remark
	 * @return {@code true} cancel success ,otherwise, {@code false}
	 */
	@Override
	public boolean cancelRemittance(String uid, BigDecimal amount, BigDecimal balance, String tid, String msg) {
		if (StringUtils.isBlank(uid) || StringUtils.isBlank(tid) || Objects.isNull(amount) || Objects.isNull(balance)) {
			log.error("CancelRemit >> param is empty|null .  uid: {} amount: {} balance: {} tid: {} msg: {}", uid,
					amount, balance, tid, msg);
			return true;
		}
		if (amount.compareTo(BigDecimal.ZERO) <= 0) {
			log.error(
					"CancelRemit >> param 'amount' can't be less than 0 .  uid: {} amount: {} balance: {} tid: {} msg: {}",
					uid, amount, balance, tid, msg);
			return true;
		}
		BigDecimal amountRadix2 = amount.setScale(2, RoundingMode.HALF_UP);
		BigDecimal balanceRadix2 = balance.setScale(2, RoundingMode.HALF_UP);
		SimpleResponseData[] ret = new SimpleResponseData[1];
		/*
		 * encrypt content : uid +amount+ result + msg+tid+salt; encryption
		 * way:MD5
		 */
		String preCtn = StringUtils.trimToEmpty(uid) + amountRadix2 + balanceRadix2 + StringUtils.trimToEmpty(msg)
				+ StringUtils.trimToEmpty(tid);
		log.info("CancelRemit >> pre-data  uid: {} amount: {} balance: {} tid: {} msg: {} preCtn: {}", uid, amount,
				balance, tid, msg, preCtn);
		Map<String, Object> param = new HashMap<>();
		param.put("uid", uid);
		param.put("amount", amountRadix2);
		param.put("balance", balanceRadix2);
		param.put("msg", StringUtils.trimToEmpty(msg));
		param.put("tid", StringUtils.trimToEmpty(tid));
		param.put(CONSTANT_TOKEN_VARIABLE, CommonUtils.md5digest(preCtn + appPros.getRebatesalt()));
		RebateHttpClient.getInstance().getPlatformServiceApi().withdrawalCancel(buildReqBody(param)).subscribe(d -> {
			ret[0] = d;
			if (d.getStatus() != 1) {
				log.error(
						"CancelRemit >> revoke Rebate System fail.  uid: {} amount: {} balance: {} tid: {} msg: {} error: {}",
						uid, amountRadix2, balanceRadix2, tid, msg, d.getMessage());
			}
		}, e -> log.error(
				"CancelRemit >> revoke Rebate System error.  uid: {} amount: {} balance: {} tid: {} msg: {} error: {}",
				uid, amountRadix2, balanceRadix2, tid, msg, e));
		boolean result = ret.length != 0 && Objects.nonNull(ret[0]) && ret[0].getStatus() == 1;
		if (result) {
			log.info(
					"CancelRemitSuccess >> revoke Rebate System successfully.  uid: {} amount: {} balance: {} tid: {} msg: {}",
					uid, amountRadix2, balanceRadix2, tid, msg);
		}
		return result;
	}

	/**
	 * confirm part-time duty personal commission remittance
	 *
	 * @param rebateId
	 *            {@link com.xinbo.fundstransfer.domain.entity.BizAccountRebate}#id
	 * @param remark
	 *            description
	 * @param operator
	 *            who send the cancel directive.
	 */
	@Override
	public void confirm(Long rebateId, String remark, SysUser operator) {
		/* check whether params are null | empty */
		if (Objects.isNull(rebateId) || StringUtils.isBlank(remark) || Objects.isNull(operator)) {
			log.info("AckRemit (rebateId: {} remark: {} operator: {} ) >> param is null|empty", rebateId, remark,
					Objects.isNull(operator) ? null : operator.getUid());
			return;
		}
		BizAccountRebate rebate = accRebateDao.findOne(rebateId);
		/* check whether rebate exists. */
		if (Objects.isNull(rebate)) {
			log.info("AckRemit (rebateId: {} remark: {} operator: {} ) >> rebate doesn't exist.", rebateId, remark,
					operator.getUid());
			return;
		}
		int status = rebate.getStatus();
		int MATCHED = OutwardTaskStatus.Matched.getStatus(), DEPOSITED = OutwardTaskStatus.Deposited.getStatus(),
				MANAGECANCEL = OutwardTaskStatus.ManageCancel.getStatus();
		/* check whether rebate is in finished status [Matched , Deposited ] */
		if (status == MATCHED || status == DEPOSITED || status == MANAGECANCEL) {
			log.info(
					"AckRemit (rebateId: {} remark: {} operator: {} ) >> rebate can't be [ Matched , Deposited ,ManageCancel ] .",
					rebateId, remark, operator.getUid());
			return;
		}
		BizAccountMore more = accMoreSer.getFromCacheByUid(rebate.getUid());
		boolean result = false;
		if (null != rebate && null != rebate.getType() && 2 == rebate.getType()) {
			for (String accId : StringUtils.trimToEmpty(more.getAccounts()).split(",")) {
				if (StringUtils.isBlank(accId) || !StringUtils.isNumeric(accId))
					continue;
				BizAccount account = accSer.getById(Integer.valueOf(accId));
				if (null != account && account.getStatus() != AccountStatus.Freeze.getStatus()
						&& account.getStatus() != AccountStatus.Inactivated.getStatus()
						&& account.getStatus() != AccountStatus.Delete.getStatus()) {
					result = derate(account.getAccount(), -rebate.getAmount().floatValue(),
							more.getMargin().floatValue(), null, rebate.getTid());
					break;
				}
			}
		} else {
			result = ackRemittance(more.getUid(), rebate.getAmount(), more.getBalance(), rebate.getTid(), remark);
		}
		if (result) {
			remark = String.format("%s(转完成)", remark);
			remark = CommonUtils.genRemark(rebate.getRemark(), remark, new Date(), operator.getUid());
			// 点击完成时查看是否有流水，如果有流水则更新状态
			Long bankFlowId = findMatchingBankLogId(rebate.getAccountId(), rebate.getToAccount(), rebate.getToHolder(),
					rebate.getAmount());
			if (null != bankFlowId)
				bankLogService.updateBankLog(bankFlowId, BankLogStatus.Matched.getStatus());
			accRebateDao.updRemarkOrStatus(rebateId, remark, null == bankFlowId
					? OutwardTaskStatus.Deposited.getStatus() : OutwardTaskStatus.Matched.getStatus(),
					rebate.getStatus());
		}
	}

	/**
	 * robot confirm part-time duty personal commission remittance@param
	 * rebateId
	 * {@link com.xinbo.fundstransfer.domain.entity.BizAccountRebate}#id [not
	 * null]
	 */
	@Override
	public void confirmByRobot(TransferEntity entity) {
		/* check whether params are null | empty */
		if (Objects.isNull(entity) || Objects.isNull(entity.getTaskId())) {
			log.info("AckRemitRobot (rebateId: null ) >> param 'rebateId' is null|empty");
			return;
		}
		long rebateId = entity.getTaskId();
		String remark = entity.getRemark(), screenshot = entity.getScreenshot();
		BizAccountRebate rebate = accRebateDao.findOne(rebateId);
		/* check whether rebate exists. */
		if (Objects.isNull(rebate)) {
			log.info("AckRemitRobot (rebateId: {} remark: {} screenshot: {} ) >> rebate doesn't exist.", rebateId,
					remark, screenshot);
			return;
		}
		int status = rebate.getStatus();
		/* check whether rebate is in finished status [Matched , Deposited ] */
		if (status != OutwardTaskStatus.Undeposit.getStatus() || Objects.isNull(rebate.getAccountId())) {
			log.info(
					"AckRemitRobot (rebateId: {} remark: {} screenshot: {} ) >> is't in [ UNDEPOSIT ] | isn't allocated.",
					rebateId, remark, screenshot);
			return;
		}
		boolean result = false;
		BizAccountMore more = accMoreSer.getFromCacheByUid(rebate.getUid());
		if (null != rebate && null != rebate.getType() && 2 == rebate.getType()) {
			for (String accId : StringUtils.trimToEmpty(more.getAccounts()).split(",")) {
				if (StringUtils.isBlank(accId) || !StringUtils.isNumeric(accId))
					continue;
				BizAccount account = accSer.getById(Integer.valueOf(accId));
				if (null != account && account.getStatus() != AccountStatus.Freeze.getStatus()
						&& account.getStatus() != AccountStatus.Inactivated.getStatus()
						&& account.getStatus() != AccountStatus.Delete.getStatus()) {
					result = derate(account.getAccount(), -rebate.getAmount().floatValue(),
							more.getMargin().floatValue(), null, rebate.getTid());
					break;
				}
			}
		} else {
			result = ackRemittance(more.getUid(), rebate.getAmount(), more.getBalance(), rebate.getTid(), remark);
		}
		if (result) {
			remark = String.format("%s(转完成)", remark);
			remark = CommonUtils.genRemark(rebate.getRemark(), remark, new Date(), "系统");
			accRebateDao.updRemarkOrStatus(rebateId, remark, OutwardTaskStatus.Deposited.getStatus(),
					rebate.getStatus(), screenshot);
		} else {
			remark = String.format("%s(出款成功 通知返利网失败 转待排查)", remark);
			remark = CommonUtils.genRemark(rebate.getRemark(), remark, new Date(), "系统");
			accRebateDao.updRemarkOrStatus(rebateId, remark, OutwardTaskStatus.Failure.getStatus(), rebate.getStatus(),
					screenshot);
		}
	}

	/**
	 * cancel part-time duty personal commission remittance
	 *
	 * @param rebateId
	 *            {@link com.xinbo.fundstransfer.domain.entity.BizAccountRebate}#id
	 * @param remark
	 *            description
	 * @param operator
	 *            who send the cancel directive.
	 */
	@Override
	public void cancel(Long rebateId, String remark, SysUser operator, boolean forece) {
		/* check whether params are null | empty */
		if (Objects.isNull(rebateId) || StringUtils.isBlank(remark) || Objects.isNull(operator)) {
			log.error("CancelRemit (rebateId: {} remark: {} operator: {} ) >> param is null|empty", rebateId, remark,
					Objects.isNull(operator) ? null : operator.getUid());
			return;
		}
		BizAccountRebate rebate = accRebateDao.findOne(rebateId);
		/* check whether rebate exists. */
		if (Objects.isNull(rebate)) {
			log.error("CancelRemit (rebateId: {} remark: {} operator: {} ) >> rebate doesn't exist.", rebateId, remark,
					operator.getUid());
			return;
		}
		int status = rebate.getStatus();
		/* check whether rebate is in finished status [MANAGECANCEL] */
		if (Objects.equals(status, OutwardTaskStatus.ManageCancel.getStatus())) {
			log.error("CancelRemit (rebateId: {} remark: {} operator: {} ) >> rebate is already in [ MANAGECANCEL ].",
					rebateId, remark, operator.getUid());
			return;
		}
		if (!forece && !Objects.equals(status, OutwardTaskStatus.Failure.getStatus())) {
			log.error(
					"CancelRemit (rebateId: {} remark: {} operator: {} ) >> rebate is not in [ Failure ]. currentStatus: {}",
					rebateId, remark, operator.getUid(), status);
			return;
		}
		BizAccountMore more = accMoreSer.getFromByUid(rebate.getUid());
		BigDecimal balance = more.getBalance().add(rebate.getAmount()).setScale(2, RoundingMode.HALF_UP);
		boolean result = false;
		if (null != rebate.getType() && 2 == rebate.getType()) {
			result = limitCancel(more.getUid(), (more.getMargin().add(rebate.getAmount())), remark, rebate.getTid());
		} else {
			result = cancelRemittance(more.getUid(), rebate.getAmount(), balance, rebate.getTid(), remark);
		}
		if (result) {/* notice:transaction in db . */
			log.info("CancelRemit》现有金额：" + more.getBalance() + "任务金额：" + rebate.getAmount() + ",取消之后的金额：" + balance
					+ ",rebateId:" + rebateId);
			if (null != rebate.getType() && 2 == rebate.getType()) {
				String remarks = "当前额度：" + more.getMargin() + ",取消后额度：" + more.getMargin().add(rebate.getAmount())
						+ ",降额单号：" + rebate.getTid() + "单号金额：" + rebate.getAmount();
				more.setRemark(CommonUtils.genRemark(more.getRemark(), remarks, new Date(), "sys"));
				more.setMargin(more.getMargin().add(rebate.getAmount()));
				more.setLinelimit(more.getLinelimit().add(rebate.getAmount()));
			} else {
				more.setBalance(balance);
			}
			// accMoreSer.updateBalance(more.getId(), balance);
			accMoreSer.saveAndFlash(more);
			remark = String.format("%s(转取消)", remark);
			remark = CommonUtils.genRemark(rebate.getRemark(), remark, new Date(), operator.getUid());
			accRebateDao.updRemarkOrStatus(rebateId, remark, OutwardTaskStatus.ManageCancel.getStatus(),
					rebate.getStatus());
		}
		log.info("CancelRemit (rebateId: {} remark: {} operator: {} ) >> result: {} . ", rebateId, remark,
				operator.getUid(), result);
	}

	/**
	 * remark part-time duty personal commission remittance
	 *
	 * @param rebateId
	 *            {@link com.xinbo.fundstransfer.domain.entity.BizAccountRebate}#id
	 * @param remark
	 *            description
	 * @param operator
	 *            who send the cancel directive.
	 */
	@Override
	public void remark(Long rebateId, String remark, SysUser operator) {
		/* check whether params are null | empty */
		if (Objects.isNull(rebateId) || StringUtils.isBlank(remark) || Objects.isNull(operator)) {
			log.error("RemarkRemit (rebateId: {} remark: {} operator: {} ) >> param is null|empty", rebateId, remark,
					Objects.isNull(operator) ? null : operator.getUid());
			return;
		}
		BizAccountRebate rebate = accRebateDao.findOne(rebateId);
		/* check whether rebate exists. */
		if (Objects.isNull(rebate)) {
			log.error("RemarkRemit (rebateId: {} remark: {} operator: {} ) >> rebate doesn't exist.", rebateId, remark,
					operator.getUid());
			return;
		}
		remark = CommonUtils.genRemark(rebate.getRemark(), remark, new Date(), operator.getUid());
		accRebateDao.updRemarkOrStatus(rebateId, remark, rebate.getStatus(), rebate.getStatus());
		log.info("RemarkRemit (rebateId: {} remark: {} operator: {} ) >> successfully .", rebateId, remark,
				operator.getUid());
	}

	/**
	 * alter the duty {@code rebateId} to Pending investigation status.
	 *
	 * @param rebateId
	 *            {@link com.xinbo.fundstransfer.domain.entity.BizAccountRebate}#id
	 * @param remark
	 *            description
	 * @param operator
	 *            who send the failure directive.
	 */
	@Override
	public void fail(Long rebateId, String remark, SysUser operator) {
		/* check whether params are null | empty */
		if (Objects.isNull(rebateId) || StringUtils.isBlank(remark) || Objects.isNull(operator)) {
			log.error("FailRemit (rebateId: {} remark: {} operator: {} ) >> param is null|empty", rebateId, remark,
					Objects.isNull(operator) ? null : operator.getUid());
			return;
		}
		BizAccountRebate rebate = accRebateDao.findOne(rebateId);
		/* check whether rebate exists. */
		if (Objects.isNull(rebate)) {
			log.error("FailRemit (rebateId: {} remark: {} operator: {} ) >> rebate doesn't exist.", rebateId, remark,
					operator.getUid());
			return;
		}
		int status = rebate.getStatus();
		int Failure = OutwardTaskStatus.Failure.getStatus(), Undeposit = OutwardTaskStatus.Undeposit.getStatus();
		/* check whether rebate is in [Failure,ManageCancel] status */
		if (status == Failure || Objects.equals(status, OutwardTaskStatus.ManageCancel.getStatus())) {
			String statusMsg = Objects.equals(status, Failure) ? "Failure" : "ManageCancel";
			log.error("FailRemit (rebateId: {} remark: {} operator: {} ) >> rebate is in [ {} ] .", rebateId, remark,
					operator.getUid(), statusMsg);
			return;
		}
		/* check whether rebate status is legal */
		if (!Objects.equals(status, OutwardTaskStatus.Deposited.getStatus())
				&& !Objects.equals(status, OutwardTaskStatus.Matched.getStatus()) && status != Undeposit) {
			log.error(
					"FailRemit (rebateId: {} remark: {} operator: {} ) >> rebate is not in [ Deposited , Matched , Undeposit] status. current status: {}",
					rebateId, remark, operator.getUid(), status);
			return;
		}
		if (status == Undeposit && Objects.isNull(rebate.getAccountId())) {
			log.error(
					"FailRemit (rebateId: {} remark: {} operator: {} ) >> un-allocate duty can't be transfer into Failure status",
					rebateId, remark, operator.getUid());
			return;
		}
		remark = String.format("%s(转待排查)", remark);
		remark = CommonUtils.genRemark(rebate.getRemark(), remark, new Date(), operator.getUid());
		accRebateDao.updRemarkOrStatus(rebateId, remark, Failure, rebate.getStatus());
		log.info("FailRemit (rebateId: {} remark: {} operator: {} ) >> successfully .", rebateId, remark,
				operator.getUid());
	}

	@Override
	public void failByRobot(TransferEntity entity) {
		/* check whether params are null | empty */
		if (Objects.isNull(entity) || Objects.isNull(entity.getTaskId())) {
			log.info("FailRemitRobot (rebateId: null remark: {} screenshot: {} ) >> param 'rebateId' is null|empty");
			return;
		}
		long rebateId = entity.getTaskId();
		String remark = entity.getRemark(), screenshot = entity.getScreenshot();
		BizAccountRebate rebate = accRebateDao.findOne(rebateId);
		/* check whether rebate exists. */
		if (Objects.isNull(rebate)) {
			log.info("FailRemitRobot (rebateId: {} remark: {} screenshot: {} ) >> rebate doesn't exist.", rebateId,
					remark, screenshot);
			return;
		}
		int status = rebate.getStatus();
		/* check whether rebate is in finished status [Matched , Deposited ] */
		if (status != OutwardTaskStatus.Undeposit.getStatus() || Objects.isNull(rebate.getAccountId())) {
			log.info(
					"FailRemitRobot (rebateId: {} remark: {} screenshot: {} ) >> is't in [ UNDEPOSIT ] | isn't allocated.",
					rebateId, remark, screenshot);
			return;
		}
		remark = String.format("%s(出款失败 转待排查)", remark);
		remark = CommonUtils.genRemark(rebate.getRemark(), remark, new Date(), "系统");
		accRebateDao.updRemarkOrStatus(rebateId, remark, OutwardTaskStatus.Failure.getStatus(), rebate.getStatus(),
				screenshot);
	}

	@Override
	public void match(Long rebateId, Long bankLogId, String remark, SysUser operator) {
		if (Objects.isNull(rebateId) || Objects.isNull(bankLogId) || Objects.isNull(operator)
				|| StringUtils.isBlank(remark)) {
			log.info("MatchRemit >>  param is null|empty . rebateId:{} bankLogId:{} remark:{} operator:{} ", rebateId,
					bankLogId, remark, (Objects.isNull(operator) ? null : operator.getUid()));
			return;
		}
		BizAccountRebate rebate = accRebateDao.findById2(rebateId);
		if (rebate == null || !OutwardTaskStatus.Deposited.getStatus().equals(rebate.getStatus())) {
			log.info(
					"MatchRemit >>  rebate isn't in [ Deposited ] status . rebateId:{} bankLogId:{} remark:{} operator:{} ",
					rebateId, bankLogId, remark, operator.getUid());
			return;
		}
		BizBankLog bankLog = bankLogService.get(bankLogId);
		if (Objects.isNull(bankLog)) {
			log.info("MatchRemit >>  bankLog doesn't exist . rebateId:{} bankLogId:{} remark:{} operator:{} ", rebateId,
					bankLogId, remark, operator.getUid());
			return;
		}
		log.info("MatchRemit >>  rebateId:{} bankLogId:{} remark:{} operator:{} ", rebateId, bankLogId, remark,
				operator.getUid());
		BizTransactionLog o = new BizTransactionLog();
		o.setAmount(bankLog.getAmount());
		o.setOrderId(rebateId);
		o.setConfirmor(operator.getId());
		o.setOperator(operator.getId());
		o.setCreateTime(new Date());
		o.setType(IncomeRequestType.RebateCommission.getType());
		o.setToAccount(0);
		o.setFromAccount(bankLog.getFromAccount());
		o.setDifference(rebate.getAmount().subtract(bankLog.getAmount()));
		o.setRemark(CommonUtils.genRemark(o.getRemark(), remark, o.getCreateTime(), operator.getUid()));
		o.setFromBanklogId(bankLogId);
		transactionLogService.save(o);
		remark = CommonUtils.genRemark(rebate.getRemark(), remark, new Date(), operator.getUid());
		accRebateDao.updRemarkOrStatus(rebateId, remark, OutwardTaskStatus.Matched.getStatus(), rebate.getStatus());
	}

	@Override
	public boolean checkAckLimit(Integer accId) {
		if (Objects.isNull(accId)) {
			return false;
		}
		AccountBaseInfo base = accSer.getFromCacheById(accId);
		return Objects.nonNull(base) && Objects.equals(base.getFlag(), 2)
				&& Objects.equals(base.getStatus(), AccountStatus.Activated.getStatus())
				&& (Objects.isNull(base.getPeakBalance()) || base.getPeakBalance() <= 0);
	}

	/**
	 * report auditing account information to rebate system.
	 *
	 * @param audit
	 *            {@code true} pass audit, otherwise,{@code false}
	 */
	@Override
	public void auditAcc(boolean audit, String oriAcc, String oriOwner, String currAcc, String currOwner) {
		int status = audit ? 2 : 5;
		log.info("RebateAuditAcc >> apply status: {} oriAcc：{} oriOwner：{} currAcc: {} currOwner: {}", status, oriAcc,
				oriOwner, currAcc, currOwner);
		Map<String, Object> param = new HashMap<>();
		param.put("oriAcc", StringUtils.trimToEmpty(oriAcc));
		param.put("oriOwner", StringUtils.trimToEmpty(oriOwner));
		param.put("currAcc", StringUtils.trimToEmpty(currAcc));
		param.put("currOwner", StringUtils.trimToEmpty(currOwner));
		param.put("status", status);
		StringBuilder preContent = new StringBuilder();
		preContent.append(StringUtils.trimToEmpty(oriAcc)).append(StringUtils.trimToEmpty(oriOwner))
				.append(StringUtils.trimToEmpty(currAcc)).append(StringUtils.trimToEmpty(currOwner)).append(status);
		CommonUtils.md5digest(preContent.toString() + appPros.getRebatesalt());
		param.put("token", CommonUtils.md5digest(preContent.toString() + appPros.getRebatesalt()));
		String data = null;
		try {
			data = mapper.writeValueAsString(param);
			log.info("RebateAuditAcc >> send data: {} preCtn: {}", data, preContent.toString());
			redisSer.rightPush(RedisTopics.REBATE_AUDIT_ACCS, data);
		} catch (Exception e) {
			log.info("RebateAuditAcc >> error: {}", data);
		}
	}

	@Override
	public BizAccountRebate findById(Long rebateId) {
		return accRebateDao.findById2(rebateId);
	}

	@Scheduled(fixedRate = 10000)
	protected void auditAcc() {
		if (!allocInAccSer.checkHostRunRight()) {
			return;
		}
		List<String> data = new ArrayList<>();
		try {
			while (true) {
				String entity = (String) redisSer.leftPop(RedisTopics.REBATE_AUDIT_ACCS);
				if (Objects.isNull(entity))
					break;
				SimpleResponseData[] ret = new SimpleResponseData[1];
				RebateHttpClient.getInstance().getPlatformServiceApi().accFeedback(buildReqBody(entity))
						.subscribe(d -> ret[0] = d, e -> log.error("RebateAuditAcc >> errorExp  data: {}", entity, e));
				if (ret.length == 1 && Objects.nonNull(ret[0]))
					log.info("RebateAuditAcc >> Receive Msg: {}  data: {}", ret[0].getMessage(), entity);
				if (ret.length != 1 || (Objects.isNull(ret[0]) || ret[0].getStatus() != 1 && ret[0].getStatus() != 2))
					data.add(entity);
			}
		} catch (Exception e) {
			log.error("RebateAuditAcc >> finalExp", e);
		} finally {
			for (String item : data)
				redisSer.rightPush(RedisTopics.REBATE_AUDIT_ACCS, item);
			if (!CollectionUtils.isEmpty(data)) {
				try {
					Thread.sleep(20000L);
				} catch (InterruptedException e) {
					log.error("RebateAuditAcc >> InterruptExp", e);
				}

			}
		}
	}

	/**
	 * get this computing commission time
	 *
	 * @return time format 'yyyy-MM-dd'
	 * @see this#valOfCommissionTime(String)
	 */
	private String valOfCommissionTime() {
		String proKey = UserProfileKey.RECORD_TIME_4_DAILY_COMMISSION.getValue();
		List<SysUserProfile> proList = userProSer.findByPropertyKey(proKey).stream()
				.filter(p -> Objects.equals(p.getUserId(), AppConstants.USER_ID_4_ADMIN)).collect(Collectors.toList());
		SysUserProfile pro = CollectionUtils.isEmpty(proList) ? null : proList.get(0);
		String dayB2 = CommonUtils.getDateFormatyyyyMMdd2Str(new Date(System.currentTimeMillis() - 86400000 * 2));
		String dayB1 = CommonUtils.getDateFormatyyyyMMdd2Str(new Date(System.currentTimeMillis() - 86400000));
		if (Objects.isNull(pro)) {
			pro = new SysUserProfile();
			pro.setPropertyValue(dayB2);
			pro.setUserId(AppConstants.USER_ID_4_ADMIN);
			pro.setPropertyName("返利网每日佣金计算 日期");
			pro.setPropertyKey(proKey);
			userProSer.save(pro);
			return dayB1;
		} else if (StringUtils.isBlank(pro.getPropertyValue())) {
			pro.setPropertyValue(dayB2);
			userProSer.save(pro);
			return dayB1;
		} else {
			String[] yyyyMMdd = pro.getPropertyValue().split("-");
			Calendar calendar = Calendar.getInstance();
			calendar.set(Calendar.YEAR, Integer.valueOf(yyyyMMdd[0]));
			calendar.set(Calendar.MONTH, Integer.valueOf(yyyyMMdd[1]) - 1);
			calendar.set(Calendar.DAY_OF_MONTH, Integer.valueOf(yyyyMMdd[2]) + 1);
			String d1 = CommonUtils.getDateFormatyyyyMMdd2Str(calendar.getTime());
			String d2 = CommonUtils.getDateFormatyyyyMMdd2Str(new Date());
			return Objects.equals(d1, d2) ? null : d1;
		}
	}

	/**
	 * set this computing commission time
	 *
	 * @param time
	 *            format 'yyyy-MM-dd'
	 * @see this#valOfCommissionTime()
	 */
	private void valOfCommissionTime(String time) {
		if (StringUtils.isBlank(time)) {
			return;
		}
		String proKey = UserProfileKey.RECORD_TIME_4_DAILY_COMMISSION.getValue();
		List<SysUserProfile> proList = userProSer.findByPropertyKey(proKey).stream()
				.filter(p -> Objects.equals(p.getUserId(), AppConstants.USER_ID_4_ADMIN)).collect(Collectors.toList());
		SysUserProfile pro = CollectionUtils.isEmpty(proList) ? new SysUserProfile() : proList.get(0);
		pro.setPropertyValue(time);
		pro.setUserId(AppConstants.USER_ID_4_ADMIN);
		pro.setPropertyName("返利网每日佣金计算 日期");
		pro.setPropertyKey(proKey);
		userProSer.save(pro);
	}

	private List<ReqV3RateItem> buildRate() {
		List<SysUserProfile> proList = userProSer.findByPropertyKey(UserProfileKey.REBATE_SYS_RATE_SETTING.getValue())
				.stream().filter(p -> Objects.equals(p.getUserId(), AppConstants.USER_ID_4_ADMIN))
				.collect(Collectors.toList());
		if (CollectionUtils.isEmpty(proList) || StringUtils.isBlank(proList.get(0).getPropertyValue()))
			return null;
		try {
			return mapper.readValue(proList.get(0).getPropertyValue(), new TypeReference<List<ReqV3RateItem>>() {
			});
		} catch (Exception e) {
			return null;
		}
	}

	private ReqV3RateItem buildWxRate() {
		SysUserProfile proList = userProSer.findByUserIdAndPropertyKey(false, AppConstants.USER_ID_4_ADMIN,
				UserProfileKey.REBATE_WX_SYS_RATE_SETTING.getValue());
		if (Objects.isNull(proList))
			return null;
		try {
			return mapper.readValue(proList.getPropertyValue().replace("[", "").replace("]", ""), ReqV3RateItem.class);
		} catch (Exception e) {
			return null;
		}
	}

	private ReqV3RateItem buildZfbRate() {
		SysUserProfile proList = userProSer.findByUserIdAndPropertyKey(false, AppConstants.USER_ID_4_ADMIN,
				UserProfileKey.REBATE_ZFB_SYS_RATE_SETTING.getValue());
		if (Objects.isNull(proList))
			return null;
		try {
			return mapper.readValue(proList.getPropertyValue().replace("[", "").replace("]", ""), ReqV3RateItem.class);
		} catch (Exception e) {
			return null;
		}
	}

	private List<ReqV3RateItem> buildAgentRate() {
		List<SysUserProfile> proList = userProSer
				.findByPropertyKey(UserProfileKey.REBATE_AGENT_SYS_RATE_SETTING.getValue()).stream()
				.filter(p -> Objects.equals(p.getUserId(), AppConstants.USER_ID_4_ADMIN)).collect(Collectors.toList());
		if (CollectionUtils.isEmpty(proList) || StringUtils.isBlank(proList.get(0).getPropertyValue()))
			return null;
		try {
			return mapper.readValue(proList.get(0).getPropertyValue(), new TypeReference<List<ReqV3RateItem>>() {
			});
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * get start time and end time by commission time
	 *
	 * @param time
	 *            commission time , time format:yyyy-MM-dd
	 */
	private Date[] startAndEndTimeOfCommission(String time) {
		if (StringUtils.isBlank(time))
			return null;
		String[] yyyyMMdd = time.split("-");
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, Integer.valueOf(yyyyMMdd[0]));
		calendar.set(Calendar.MONTH, Integer.valueOf(yyyyMMdd[1]) - 1);
		calendar.set(Calendar.DAY_OF_MONTH, Integer.valueOf(yyyyMMdd[2]));
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return new Date[] { calendar.getTime(), new Date(calendar.getTime().getTime() + 86400000 - 500) };
	}

	private ReqV3RateItem buildCommisson(List<ReqV3RateItem> rateList, BizAccountMore base) {
		if (Objects.isNull(base) || Objects.isNull(base.getMargin())) {
			return null;
		}
		rateList.sort((o1, o2) -> {
			float o3 = o1.getAmount() - o2.getAmount();
			return (o3 > 0) ? 1 : (o3 < 0 ? -1 : 0);
		});
		float peek = base.getMargin().floatValue();
		if (peek == 0) {
			rateList = rateList.stream().filter(p -> p.getAmount() == 0).collect(Collectors.toList());
			return CollectionUtils.isEmpty(rateList) ? null : rateList.get(0);
		}
		ReqV3RateItem target = rateList.get(rateList.size() - 1);
		for (int i = 0; i < rateList.size(); i++) {
			ReqV3RateItem rate = rateList.get(i);
			if (peek < rate.getAmount()) {
				if (i >= 1) {
					target = rateList.get(i - 1);
				}
				break;
			} else if ((i + 1) == rateList.size()) {
				target = rateList.get(i);
			}
		}
		// for (ReqV3RateItem rate : rateList) {
		// if (peek < rate.getAmount()) {
		// target = rate;
		// break;
		// }
		// }
		return target;
	}

	/**
	 * build {@link RequestBody}
	 *
	 * @param params
	 *            to be encrypted and pack to {@link RequestBody}
	 */
	private RequestBody buildReqBody(Map<String, Object> params) {
		try {
			return buildReqBody(mapper.writeValueAsString(params));
		} catch (Exception e) {
			return null;
		}
	}

	private RequestBody buildReqBody(String params) {
		try {
			return RequestBody.create(MediaType.parse(CONSTANT_HTTP_MEDIA_TYPE), params);
		} catch (Exception e) {
			return null;
		}
	}

	private Long findMatchingBankLogId(Integer fromAccountId, String toAccount, String toOwner, BigDecimal amount) {
		if (fromAccountId == null)
			return null;
		toAccount = StringUtils.trimToEmpty(toAccount);
		toOwner = StringUtils.trimToEmpty(toOwner);
		Integer status = BankLogStatus.Matching.getStatus();
		List<BizBankLog> logList = bankLogRepository.findByFromAccountAndAmount(fromAccountId,
				amount.abs().multiply(new BigDecimal(-1)));
		logList = logList.stream().filter(p -> p.getStatus() == null || p.getStatus().equals(status))
				.collect(Collectors.toList());
		logList.sort((o1, o2) -> -o1.getTradingTime().compareTo(o2.getTradingTime()));
		for (BizBankLog log : logList) {
			if (StringUtils.equals(StringUtils.trimToEmpty(log.getToAccount()), toAccount)
					|| StringUtils.equals(StringUtils.trimToEmpty(log.getToAccountOwner()), toOwner)) {
				return log.getId();
			}
		}
		return null;
	}

	@Override
	public void ackRechargelimit(BizAccountMore more, float RechargeAmount, String tid) {
		if (Objects.isNull(more) || Objects.isNull(tid))
			return;
		BigDecimal lPeek = more.getMargin();
		BigDecimal amount = Objects.nonNull(RechargeAmount) ? new BigDecimal(RechargeAmount) : BigDecimal.ZERO;
		lPeek = lPeek.setScale(2, RoundingMode.HALF_UP);
		BigDecimal rPeak = BigDecimal.ZERO;
		BizAccount accout = null;
		String lastAccountId = "";
		// 针对兼职人员账号提额，找一个不是冻结、异常、删除的卡 提额
		for (String accId : StringUtils.trimToEmpty(more.getAccounts()).split(",")) {
			if (StringUtils.isBlank(accId) || !StringUtils.isNumeric(accId))
				continue;
			accout = accSer.getById(Integer.valueOf(accId));
			lastAccountId = accId;
			if (accout.getStatus() != AccountStatus.Freeze.getStatus()
					&& accout.getStatus() != AccountStatus.Excep.getStatus()
					&& accout.getStatus() != AccountStatus.Delete.getStatus()) {
				break;
			}
		}
		// 如果没有正常的卡 则取最后一个
		if (null == accout) {
			accout = accSer.getById(Integer.valueOf(lastAccountId));
		}
		BigDecimal tmpPeek = Objects.isNull(accout.getPeakBalance()) ? BigDecimal.ZERO
				: new BigDecimal(accout.getPeakBalance());
		tmpPeek = tmpPeek.add(new BigDecimal(RechargeAmount));
		accout.setPeakBalance(tmpPeek.intValue());
		accSer.updateBaseInfo(accout);
		accSer.broadCast(accout);
		if (lPeek.compareTo(new BigDecimal(1000)) == 0) {
			BigDecimal init = BigDecimal.ZERO;
			for (String accId : StringUtils.trimToEmpty(more.getAccounts()).split(",")) {
				if (StringUtils.isBlank(accId) || !StringUtils.isNumeric(accId))
					continue;
				BizAccount acc = accSer.getById(Integer.valueOf(accId));
				if (Objects.nonNull(acc))
					init = init.add(new BigDecimal(acc.getPeakBalance()));
			}
			if (init.compareTo(new BigDecimal(1000)) > 0) {
				rPeak = init;
			}
			rPeak = init.setScale(2, RoundingMode.HALF_UP);
		} else {
			rPeak = lPeek.add(amount).setScale(2, RoundingMode.HALF_UP);
		}
		String gen = String.format("%s 返利佣金充值信用额度提升 %s [%s %s]", tid, lPeek.toString(), lPeek.toString(),
				rPeak.toString());
		more.setMargin(rPeak);
		more.setBalance(more.getBalance().subtract(new BigDecimal(RechargeAmount)));
		more.setRemark(CommonUtils.genRemark(more.getRemark(), gen, new Date(), "REB"));
		accMoreSer.saveAndFlash(more);
		/*
		 * encrypt content : amount+ tid + salt; encryption way :MD5
		 */
		String preCtn = new BigDecimal(RechargeAmount).setScale(2, RoundingMode.HALF_UP) + rPeak.toString()
				+ StringUtils.trimToEmpty(tid);
		log.info("RechargelimitV3 >> pre-data  Margin: {} tid: {} pre:{} ", rPeak, tid, preCtn);
		HashMap<String, Object> params = new HashMap<>();
		params.put("amount", new BigDecimal(RechargeAmount).setScale(2, RoundingMode.HALF_UP));
		params.put("balance", rPeak.setScale(2, RoundingMode.HALF_UP));
		params.put("tid", StringUtils.trimToEmpty(tid));
		params.put(CONSTANT_TOKEN_VARIABLE, CommonUtils.md5digest(preCtn + appPros.getRebatesalt()));
		try {
			redisSer.rightPush(RedisTopics.REBATE_RECHARGE_LIMIT, mapper.writeValueAsString(params));
		} catch (Exception e) {
			log.error("RechargelimitV3 >> Margin: {} limit: {} tid: {}", more.getMargin(), rPeak, tid, e);
		}
	}

	@Scheduled(fixedRate = 1000)
	public void ackRechargelimit() {
		if (!allocInAccSer.checkHostRunRight())
			return;
		List<String> failList = new ArrayList<>();
		try {
			while (true) {
				String entity = (String) redisSer.leftPop(RedisTopics.REBATE_RECHARGE_LIMIT);
				if (Objects.isNull(entity))
					break;
				RebateHttpClient.getInstance().getPlatformServiceApi().limitAck(buildReqBody(entity)).subscribe(d -> {
					if (d.getStatus() == 1)
						log.info("RechargelimitV3 >> send [ SUCCESS ] data: {}", entity);
					else
						log.error("RechargelimitV3 >> send [FAIL ] {} data: {}", d.getMessage(), entity);
				}, e -> {
					if (StringUtils.isBlank(e.getMessage()) || !e.getMessage().contains("HTTP 500")) {
						log.error("RechargelimitV3 >> revoke Rebate System error. data:{}  e: {}", entity, e);
						failList.add(entity);
					} else {
						log.error("RechargelimitV3 >> [ HTTP500 ] data: {}", entity);
					}
				});
			}
		} catch (Exception e) {
			log.error("RechargelimitV3 >> finalExp", e);
		} finally {
			for (String item : failList)
				redisSer.rightPush(RedisTopics.REBATE_RECHARGE_LIMIT, item);
		}
	}

	@Override
	public String getUserByUid(String uid) {
		String token = CommonUtils.md5digest(uid + appPros.getRebatesalt());
		ThreadLocal<String> json = new ThreadLocal<>();
		RebateHttpClient.getInstance().getPlatformServiceApi().getUserByUid(("/api/users/" + uid), ("Bearer " + token))
				.subscribe(data -> {
					log.info("查询兼职人员联系方式结果:{}", data);
					if (data.getStatus() == 1) {
						ObjectMapper objectMapper = new ObjectMapper();
						try {
							json.set(objectMapper.writeValueAsString(data));
						} catch (JsonProcessingException e) {
							log.info("查询兼职人员联系方式失败:{}", e);
						}
					} else {
						log.info("查询失败:{}", data);
					}
				}, e -> {
					log.error("查询兼职人员联系方式失败", e);
					json.set("查询失败" + e.getLocalizedMessage());
				});
		return json.get();
	}

	@Override
	public void finish(Long rebateId, String remark, SysUser operator) {
		/* check whether params are null | empty */
		if (Objects.isNull(rebateId) || StringUtils.isBlank(remark) || Objects.isNull(operator)) {
			log.info("finish (rebateId: {} remark: {} operator: {} ) >> param is null|empty", rebateId, remark,
					Objects.isNull(operator) ? null : operator.getUid());
			return;
		}
		BizAccountRebate rebate = accRebateDao.findOne(rebateId);
		/* check whether rebate exists. */
		if (Objects.isNull(rebate)) {
			log.info("finish (rebateId: {} remark: {} operator: {} ) >> rebate doesn't exist.", rebateId, remark,
					operator.getUid());
			return;
		}
		int status = rebate.getStatus();
		int MATCHED = OutwardTaskStatus.Matched.getStatus(), DEPOSITED = OutwardTaskStatus.Deposited.getStatus(),
				MANAGECANCEL = OutwardTaskStatus.ManageCancel.getStatus();
		/* check whether rebate is in finished status [Matched , Deposited ] */
		if (status == MATCHED || status == DEPOSITED || status == MANAGECANCEL) {
			log.info(
					"finish (rebateId: {} remark: {} operator: {} ) >> rebate can't be [ Matched , Deposited ,ManageCancel ] .",
					rebateId, remark, operator.getUid());
			return;
		}
		remark = String.format("%s(转完成)", remark);
		remark = CommonUtils.genRemark(rebate.getRemark(), remark, new Date(), operator.getUid());
		// 点击完成时查看是否有流水，如果有流水则更新状态
		Long bankFlowId = findMatchingBankLogId(rebate.getAccountId(), rebate.getToAccount(), rebate.getToHolder(),
				rebate.getAmount());
		// 确认成功.
		BizAccountMore more = accMoreSer.getFromCacheByUid(rebate.getUid());
		if (null != rebate && null != rebate.getType() && 2 == rebate.getType()) {
			for (String accId : StringUtils.trimToEmpty(more.getAccounts()).split(",")) {
				if (StringUtils.isBlank(accId) || !StringUtils.isNumeric(accId))
					continue;
				BizAccount account = accSer.getById(Integer.valueOf(accId));
				if (null != account && account.getStatus() != AccountStatus.Freeze.getStatus()
						&& account.getStatus() != AccountStatus.Inactivated.getStatus()
						&& account.getStatus() != AccountStatus.Delete.getStatus()) {
					derate(account.getAccount(), -rebate.getAmount().floatValue(), more.getMargin().floatValue(), null,
							rebate.getTid());
					break;
				}
			}
		} else {
			ackRemittance(more.getUid(), rebate.getAmount(), more.getBalance(), rebate.getTid(), remark);
		}
		if (null != bankFlowId)
			bankLogService.updateBankLog(bankFlowId, BankLogStatus.Matched.getStatus());
		accRebateDao.updRemarkOrStatus(rebateId, remark,
				null == bankFlowId ? OutwardTaskStatus.Deposited.getStatus() : OutwardTaskStatus.Matched.getStatus(),
				rebate.getStatus());
	}

	public boolean timelyManner(boolean audit, String oriAcc, String oriOwner, String currAcc, String currOwner) {
		if (!allocInAccSer.checkHostRunRight()) {
			return false;
		}
		int status = audit ? 2 : 5;
		log.info("MobileChangeRebateRebateAuditAcc >> apply status: {} oriAcc：{} oriOwner：{} currAcc: {} currOwner: {}",
				status, oriAcc, oriOwner, currAcc, currOwner);
		Map<String, Object> param = new HashMap<>();
		param.put("oriAcc", StringUtils.trimToEmpty(oriAcc));
		param.put("oriOwner", StringUtils.trimToEmpty(oriOwner));
		param.put("currAcc", StringUtils.trimToEmpty(currAcc));
		param.put("currOwner", StringUtils.trimToEmpty(currOwner));
		param.put("status", status);
		StringBuilder preContent = new StringBuilder();
		preContent.append(StringUtils.trimToEmpty(oriAcc)).append(StringUtils.trimToEmpty(oriOwner))
				.append(StringUtils.trimToEmpty(currAcc)).append(StringUtils.trimToEmpty(currOwner)).append(status);
		CommonUtils.md5digest(preContent.toString() + appPros.getRebatesalt());
		param.put("token", CommonUtils.md5digest(preContent.toString() + appPros.getRebatesalt()));
		String data = "";
		try {
			data = mapper.writeValueAsString(param);
			SimpleResponseData[] ret = new SimpleResponseData[1];
			RebateHttpClient.getInstance().getPlatformServiceApi().accFeedback(buildReqBody(data)).subscribe(
					d -> ret[0] = d,
					e -> log.error("MobileChangeRebateRebateAuditAcc >> errorExp  data: {}", "手机转返利网错误！", e));
			if (ret.length == 1 && Objects.nonNull(ret[0])) {
				log.info("MobileChangeRebateRebateAuditAcc >> Receive Msg: {}  data: {}", ret[0].getMessage(), data);
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			log.error("MobileChangeRebateRebateAuditAcc >> finalExp", e);
			return false;
		}
	}

	@Override
	public boolean derate(String account, float amount, float balance, Long logid, String tid) {
		String data = "";
		try {
			HashMap<String, Object> params = new HashMap<>();
			params.put("acc", account);
			params.put("amount", new BigDecimal(amount).setScale(2, RoundingMode.HALF_UP));
			params.put("balance", new BigDecimal(balance).setScale(2, RoundingMode.HALF_UP));
			params.put("tid", tid);
			params.put("logId", logid);
			String preContent = StringUtils.trimToEmpty(account)
					+ new BigDecimal(amount).setScale(2, RoundingMode.HALF_UP)
					+ new BigDecimal(balance).setScale(2, RoundingMode.HALF_UP) + (null == tid ? "" : tid)
					+ (null == logid ? "" : logid);
			CommonUtils.md5digest(preContent.toString() + appPros.getRebatesalt());
			params.put("token", CommonUtils.md5digest(preContent.toString() + appPros.getRebatesalt()));
			data = mapper.writeValueAsString(params);
			SimpleResponseData[] ret = new SimpleResponseData[1];
			RebateHttpClient.getInstance().getPlatformServiceApi().limitAck(buildReqBody(data))
					.subscribe(d -> ret[0] = d, e -> log.error("deratelimitV3 >> errorExp  data: {}", "返利网降额错误！", e));
			if (Objects.nonNull(ret[0]) && ret[0].getStatus() == 1) {
				log.info("deratelimitV3Success >> Receive Msg: {}  status: {}  data: {}", ret[0].getMessage(),
						ret[0].getStatus(), data);
				return true;
			} else {
				log.info("deratelimitV3fail >> Receive Msg: {}  status: {}  data: {}", ret[0].getMessage(),
						ret[0].getStatus(), data);
				return false;
			}
		} catch (Exception e) {
			log.error("deratelimitV3 >> finalExp", e);
			return false;
		}

	}

	@Override
	public void activation(String rebateUserAcc, String uid, String status, String cardNo[], String message) {
		List<Map<String, String>> dataList = new ArrayList<>();
		Map<String, String> cardNos = new HashMap<>();
		String str = rebateUserAcc + uid + status;
		for (String acc : cardNo) {
			cardNos.put("cardNo", acc);
			dataList.add(cardNos);
		}
		String token = CommonUtils.md5digest(str + appPros.getRebatesalt());
		HashMap<String, Object> params = new HashMap<>();
		params.put("userId", uid);
		params.put("status", status);
		params.put("accounts", dataList);
		params.put("token", token);
		ThreadLocal<String> json = new ThreadLocal<>();
		try {
			log.info("同步绑定关系 data: {}  token{}", mapper.writeValueAsString(params), str);
		} catch (JsonProcessingException e1) {
			e1.printStackTrace();
		}
		RebateHttpClient.getInstance().getPlatformServiceApi()
				.activation(("/api/cqps/" + rebateUserAcc + "/accounts"), buildReqBody(params), ("Bearer " + token))
				.subscribe(data -> {
					log.info("同步绑定关系:{}", data);
					if (data.getStatus() == 1) {
						ObjectMapper objectMapper = new ObjectMapper();
						try {
							json.set(objectMapper.writeValueAsString(data));
						} catch (JsonProcessingException e) {
							log.info("同步绑定关系失败:{}", e);
						}
					} else {
						log.info("同步绑定关系失败:{}", data);
					}
				}, e -> {
					log.error("同步绑定关系失败", e);
					json.set("查询失败" + e.getLocalizedMessage());
				});
	}

	@Override
	public void usersDevice(String mobile, String ip, String deviceId) {
		BizAccountMore more = accMoreSer.getFromCacheByMobile(mobile);
		if (null != more) {
			List<Map<String, String>> dataList = new ArrayList<>();
			Map<String, String> usersDeviceMap = new HashMap<>();
			usersDeviceMap.put("userId", more.getUid());
			usersDeviceMap.put("ipAddress", ip.replace("\"", ""));
			usersDeviceMap.put("deviceId", deviceId);
			dataList.add(usersDeviceMap);
			String token = CommonUtils.md5digest(more.getUid() + ip + deviceId + appPros.getRebatesalt());
			HashMap<String, Object> params = new HashMap<>();
			params.put("device", dataList);
			params.put("token", token);
			RebateHttpClient.getInstance().getPlatformServiceApi().usersDevice(buildReqBody(params)).subscribe(d -> {
				if (d.getStatus() == 1)
					log.info("usersDevice >> send [ SUCCESS ] data: {}", params);
				else
					log.error("usersDevice >> send [FAIL ] {} data: {}", d.getMessage(), params);
			}, e -> {
				log.error("usersDevice >> [ HTTP500 ] data: {}", params);
			});

		}
	}

	@Override
	public BigDecimal deductAmount(String uid, BigDecimal amount, String type, String acc, String remark) {
		Date date = new Date();
		SimpleDateFormat abc = new SimpleDateFormat("yyyy-MM-dd");
		String dd = abc.format(date);
		String token = CommonUtils
				.md5digest(uid + amount.setScale(2, RoundingMode.HALF_UP) + type + acc + dd + appPros.getRebatesalt());
		HashMap<String, Object> params = new HashMap<>();
		params.put("userId", uid);
		params.put("amount", amount.setScale(2, RoundingMode.HALF_UP));
		params.put("type", type);
		params.put("acc", acc);
		params.put("time", dd);
		params.put("remark", remark);
		params.put("token", token);
		BigDecimal[] ret = new BigDecimal[] { null };
		RebateHttpClient.getInstance().getPlatformServiceApi().deductAmount(buildReqBody(params)).subscribe(d -> {
			if (d.getStatus() == 1) {
				log.info("deductAmount >> send [ SUCCESS ] data: {}, d: {}", params, d);
				ApiDeductionAmount entity = null;
				try {
					entity = mapper.readValue(mapper.writeValueAsString(d.getData()), ApiDeductionAmount.class);
				} catch (IOException e1) {
					log.info("deductAmount >> 解析失败:" + d.getData());
				}
				log.info("deductAmount >> send [ SUCCESS ] Balance: {}", entity.getBalance());
				ret[0] = new BigDecimal(entity.getBalance());
			} else {
				log.error("deductAmount >> send [FAIL ] {} data: {}", d.getMessage(), params);
			}
		}, e -> {
			log.error("deductAmount >> [ HTTP500 ] data: {}", params);
		});
		return ret[0];
	}

	@Override
	public void rebateUserStatus(String account) {
		BizAccount acc = accSer.findByAccount(account).get(0);
		if (acc.getFlag() != 2)
			return;
		String status = "";
		if (acc.getStatus() == AccountStatus.Normal.getStatus()
				|| acc.getStatus() == AccountStatus.Enabled.getStatus()) {
			status = "0";
		} else if (acc.getStatus() == AccountStatus.Freeze.getStatus()
				|| acc.getStatus() == AccountStatus.Delete.getStatus()) {
			status = "4";
		} else {
			status = "2";
		}
		String token = CommonUtils.md5digest(account + status + appPros.getRebatesalt());
		HashMap<String, Object> params = new HashMap<>();
		params.put("acc", account);
		params.put("useStatus", status);
		params.put("token", token);
		RebateHttpClient.getInstance().getPlatformServiceApi().usestatus(buildReqBody(params)).subscribe(d -> {
			if (d.getStatus() == 1)
				log.info("rebateUserStatus >> send [ SUCCESS ] data: {}", params);
			else
				log.error("rebateUserStatus >> send [FAIL ] {} data: {}", d.getMessage(), params);
		}, e -> {
			log.error("rebateUserStatus >> [ HTTP500 ] data: {}", params);
		});
	}

	@Override
	public void joinFlwActivity(String uid, String activityNumber) {
		String token = CommonUtils.md5digest(uid + activityNumber + appPros.getRebatesalt());
		HashMap<String, Object> params = new HashMap<>();
		params.put("userId", uid);
		params.put("activityNumber", activityNumber);
		params.put("token", token);
		RebateHttpClient.getInstance().getPlatformServiceApi().joinFlwEvent(buildReqBody(params)).subscribe(d -> {
			if (d.getStatus() == 1)
				log.info("rebateUserStatus >> send [ SUCCESS ] data: {}", params);
			else
				log.error("rebateUserStatus >> send [FAIL ] {} data: {}", d.getMessage(), params);
		}, e -> {
			log.error("rebateUserStatus >> [ HTTP500 ] data: {}", params);
		});
	}

	@Override
	public boolean limitCancel(String uid, BigDecimal balance, String msg, String tid) {
		if (StringUtils.isBlank(uid) || StringUtils.isBlank(tid) || Objects.isNull(balance)) {
			log.error("limitCancel >> param is empty|null .  uid: {}  balance: {} tid: {} msg: {}", uid, balance, tid,
					msg);
			return true;
		}
		BigDecimal balanceRadix2 = balance.setScale(2, RoundingMode.HALF_UP);
		SimpleResponseData[] ret = new SimpleResponseData[1];
		/*
		 * encrypt content : uid +balance + msg+tid+salt; encryption way:MD5
		 */
		String preCtn = StringUtils.trimToEmpty(uid) + balanceRadix2 + StringUtils.trimToEmpty(msg)
				+ StringUtils.trimToEmpty(tid);
		log.info("limitCancel >> pre-data  uid: {} balance: {} tid: {} msg: {} preCtn: {}", uid, balance, tid, msg,
				preCtn);
		Map<String, Object> param = new HashMap<>();
		param.put("uid", uid);
		param.put("balance", balanceRadix2);
		param.put("msg", StringUtils.trimToEmpty(msg));
		param.put("tid", StringUtils.trimToEmpty(tid));
		param.put(CONSTANT_TOKEN_VARIABLE, CommonUtils.md5digest(preCtn + appPros.getRebatesalt()));
		RebateHttpClient.getInstance().getPlatformServiceApi().limitCancel(buildReqBody(param)).subscribe(d -> {
			ret[0] = d;
			if (d.getStatus() != 1) {
				log.error("limitCancel >> revoke Rebate System fail.  uid: {} balance: {} tid: {} msg: {} error: {}",
						uid, balanceRadix2, tid, msg, d.getMessage());
			}
		}, e -> log.error("limitCancel >> revoke Rebate System error.  uid: {}  balance: {} tid: {} msg: {} error: {}",
				uid, balanceRadix2, tid, msg, e));
		boolean result = ret.length != 0 && Objects.nonNull(ret[0]) && ret[0].getStatus() == 1;
		if (result) {
			log.info("limitCancel >> revoke Rebate System successfully.  uid: {}  balance: {} tid: {} msg: {}", uid,
					balanceRadix2, tid, msg);
		}
		return result;
	}

	@Scheduled(fixedRate = 500)
	protected void wxZfblogs() {
		List<String> failList = new ArrayList<>();
		ReqV3RateItem wxRateList = buildWxRate();
		ReqV3RateItem zfbRateList = buildZfbRate();
		RLock lock = redisSer.getRedisLock(RedisLockKeyEnums.CHATPAY_SYN_REBATE_USER_ORDER_LOCK.getLockKey());
		boolean isLock = false;
		try {
			while (true) {
				isLock = lock.tryLock(3, 2, TimeUnit.SECONDS); // 尝试获取分布式锁
				if (isLock) {
					String entity = (String) redisSer.leftPop(RedisKeyEnums.CHATPAY_SYN_REBATE_USER_ORDER.getKey());
					if (Objects.isNull(entity))
						break;
					List<Map<String, Object>> dataList = new ArrayList<>();
					WxZfbOrder order = mapper.readValue(entity, WxZfbOrder.class);
					Map<String, Object> item = new HashMap<>();
					BigDecimal bonusAmount = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);
					item.put("uid", order.getUid());
					item.put("tradeType", order.getTradeType());
					item.put("tradeStatus", order.getTradeStatus());
					item.put("tradeNo", order.getTradeNo());
					item.put("roomNo", order.getRoomNo());
					item.put("creator", order.getCreator());
					item.put("receiver", order.getReceiver());
					item.put("chkCode", order.getChkCode());
					item.put("amount", order.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP));
					item.put("tradeTime", order.getTradeTime());
					item.put("catchTime", order.getCatchTime());
					if (order.getAmount().floatValue() > 0) {
						if (order.getTradeType().equals("1")) {
							bonusAmount = order.getAmount().multiply(new BigDecimal((zfbRateList.getRate() / 100)));
						} else if (order.getTradeType().equals("2")) {
							bonusAmount = order.getAmount().multiply(new BigDecimal((wxRateList.getRate() / 100)));
						}
						item.put("bonusAmount", bonusAmount.setScale(2, BigDecimal.ROUND_HALF_UP));
					} else {
						item.put("bonusAmount", bonusAmount);
					}
					String preContent = order.getUid() + order.getTradeType() + order.getTradeStatus()
							+ order.getTradeNo() + order.getRoomNo() + order.getCreator() + order.getReceiver()
							+ order.getChkCode() + order.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP) + bonusAmount
							+ order.getTradeTime() + order.getCatchTime();
					dataList.add(item);
					Map<String, Object> param = new HashMap<>();
					param.put("items", dataList);
					param.put("token", CommonUtils.md5digest(preContent + appPros.getRebatesalt()));
					String par = mapper.writeValueAsString(param);
					RebateHttpClient.getInstance().getPlatformServiceApi().wxZfbLogs(buildReqBody(par)).subscribe(d -> {
						if (d.getStatus() == 1)
							log.info("WxZfbLogs >> send [ SUCCESS ] data: {}", entity);
						else
							log.error("WxZfbLogs >> send [FAIL ] {} data: {}", d.getMessage(), entity);
					}, e -> {
						if (StringUtils.isBlank(e.getMessage()) || !e.getMessage().contains("HTTP 500")) {
							log.error("WxZfbLogs >> errorExp  data: {}", entity, e);
							failList.add(entity);
						} else {
							log.error("WxZfbLogs >> [ HTTP500 ] data: {}", entity);
						}
					});
				}
			}
		} catch (Exception e) {
			log.error("WxZfbLogs >> finalExp", e);
		} finally {
			if (isLock)
				lock.unlock();
			for (String fail : failList)
				redisSer.rightPush(RedisKeyEnums.CHATPAY_SYN_REBATE_USER_ORDER.getKey(), fail);
		}
	}
}
