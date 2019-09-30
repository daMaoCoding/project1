package com.xinbo.fundstransfer.restful.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.AppProperties;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.assign.AssignBalance;
import com.xinbo.fundstransfer.component.net.http.HttpClient;
import com.xinbo.fundstransfer.component.net.socket.ActionEventEnum;
import com.xinbo.fundstransfer.component.net.socket.MessageEntity;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.*;
import com.xinbo.fundstransfer.domain.GeneralResponseData.ResponseStatus;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.*;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalPush;
import com.xinbo.fundstransfer.domain.pojo.TransferEntity;
import com.xinbo.fundstransfer.domain.repository.BankLogRepository;
import com.xinbo.fundstransfer.report.SystemAccountManager;
import com.xinbo.fundstransfer.restful.BaseController;
import com.xinbo.fundstransfer.runtime.MemCacheUtils;
import com.xinbo.fundstransfer.runtime.task.ToolResponseData;
import com.xinbo.fundstransfer.service.*;
import com.xinbo.fundstransfer.service.impl.CommissionHandler;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Test
 */
@RestController
@RequestMapping("/r/test")
public class TestController extends BaseController {
	Logger log = LoggerFactory.getLogger(TestController.class);
	@Autowired
	Environment environment;
	@Autowired
	AllocateTransService allocateTransService;
	@Autowired
	TransMonitorService transMonitorService;
	@Autowired
	AllocateOutwardTaskService outwardTaskAllocateService;
	@Autowired
	AllocateOutwardTaskService allocOTaskSer;
	@Autowired
	private SysUserService userService;
	@Autowired
	private AccountService accountService;
	@Autowired
	private AccountExtraService accountExtraService;
	@Autowired
	private HostMonitorService monitorService;
	@Autowired
	private AllocateTransferService allocateTransferService;
	@Autowired
	private BankLogService bankLogService;
	@Autowired
	private RebateApiService rebateApiService;
	@Autowired
	private IncomeRequestService incomeRequestService;
	@Autowired
	private CabanaService cabanaService;
	@Autowired
	private AccountMoreService moreSer;
	@Autowired
	private BankLogRepository bankLogDao;
	@Autowired
	private AppProperties appProperties;
	@Autowired
	private QuickPayService quickPayService;
	@Autowired
	private RebateUserService rebateUserService;
	@Autowired
	private HostMonitorService hostMonitorService;
	@Autowired
	private SystemAccountManager systemAccountManager;
	@Autowired
	private RedisService redisService;
	@Autowired
	private CommissionHandler commissionHandler;
	@Autowired
	private RebateStatisticsService rebateStatisticsSer;

	@RequestMapping("/freeze")
	public String freeze(@RequestParam("accountId") Integer accountId) throws JsonProcessingException {
		try {
			log.info("account(freeze) >> RequestBody:{}", accountId);
			AccountBaseInfo base = accountService.getFromCacheById(accountId);
			if (base == null || Objects.equals(base.getStatus(), AccountStatus.Freeze.getStatus())) {
				return mapper.writeValueAsString(new SimpleResponseData(ResponseStatus.SUCCESS.getValue(), "success"));
			}
			SysUser sysUser = userService.findFromCacheById(AppConstants.USER_ID_4_ADMIN);// 管理员
			Date d = new Date();
			BizAccount account = accountService.getById(accountId);
			BizAccount oldAccount = new BizAccount();
			BeanUtils.copyProperties(oldAccount, account);
			account.setHolder(sysUser.getId());
			account.setUpdateTime(d);
			account.setRemark(CommonUtils.genRemark(account.getRemark(),
					"【转" + AccountStatus.Excep.getMsg() + "】" + StringUtils.EMPTY, d, sysUser.getUid()));
			account.setStatus(AccountStatus.Excep.getStatus());
			account.setUpdateTime(d);
			accountService.updateBaseInfo(account);
			accountExtraService.saveAccountExtraLog(oldAccount, account, sysUser.getUid());
			accountService.broadCast(account);
			monitorService.update(account);
			// 发送取消转账命令
			List<Integer> frIdList = allocateTransferService.findFrIdList(accountId);
			if (!CollectionUtils.isEmpty(frIdList)) {
				for (Integer frId : frIdList) {
					MessageEntity<Integer> messageEntity = new MessageEntity<>();
					messageEntity.setData(frId);
					messageEntity.setIp(monitorService.findHostByAcc(frId));
					messageEntity.setAction(ActionEventEnum.CANCEL.ordinal());
					monitorService.messageBroadCast(messageEntity);
					log.info("Cancel Transfer  frId:{} toId:{}", frId, accountId);
				}
			}
			return mapper.writeValueAsString(new SimpleResponseData(ResponseStatus.SUCCESS.getValue(), "success"));
		} catch (Exception e) {
			log.error("Transfer(inAck) error.", e);
			return mapper.writeValueAsString(new SimpleResponseData(ResponseStatus.FAIL.getValue(),
					"Transfer ack error." + e.getLocalizedMessage()));
		}
	}

	// @RequestMapping(value = "/greeting", method = { RequestMethod.GET,
	// RequestMethod.POST })
	public String greeting(@RequestParam(value = "username", required = true) String username,
			@RequestParam(value = "message", required = false) String message) throws JsonProcessingException {
		redisService.convertAndSend(RedisTopics.BROADCAST, username);
		log.info("Welcome {}", username);
		SimpleResponseData responseData = new SimpleResponseData(ResponseStatus.SUCCESS.getValue());
		responseData.setMessage("Welcome , " + username + (StringUtils.isEmpty(message) ? "" : message));
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 请求内容是一个json串,spring会自动把他和我们的参数bean对应起来,不过要加@RequestBody注解
	 *
	 * @param json
	 * @return
	 */
	// @RequestMapping(value = "/json", method = { RequestMethod.POST,
	// RequestMethod.GET })
	public String loginByPost2(@RequestBody BizIncomeRequest json) {
		return json.toString();
	}

	// @RequestMapping(value = "/level", method = RequestMethod.GET)
	public String testLogLevel() {
		log.trace("Logger Level ：TRACE");
		log.debug("Logger Level ：DEBUG");
		log.info("Logger Level ：INFO");
		log.warn("Logger Level ：WARN");
		log.error("Logger Level ：ERROR");
		return "testLogLevel";
	}

	// @RequestMapping(value = "/pushMessage", method = { RequestMethod.GET,
	// RequestMethod.POST })
	public String pushMessage(@RequestParam(value = "message", required = true) String message)
			throws JsonProcessingException {
		redisService.convertAndSend(RedisTopics.BROADCAST, message);
		return "";
	}

	// @RequestMapping(value = "/income", method = { RequestMethod.GET,
	// RequestMethod.POST })
	public String income(@RequestParam(value = "amount", required = false) Float amount)
			throws JsonProcessingException {
		try {
			String handicap = "ysc";
			String level = "N0001";
			String remark = "";
			String from_account = "6215123422227890";
			String to_account = "95555014785236";
			// 会员编码
			int member_code = 1234;
			String time = DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss");
			String username = "Dom";
			int type = IncomeRequestType.PlatFromAli.getType();
			String order_no = String.valueOf(System.currentTimeMillis());
			String realname = "Franco";
			String payment_code = String.valueOf(System.currentTimeMillis());
			// 计算token
			Map<String, String> parameters = new TreeMap<String, String>(new Comparator<String>() {
				@Override
				public int compare(String obj1, String obj2) {
					return obj1.compareTo(obj2);
				}
			});
			parameters.put("handicap", handicap);
			parameters.put("level", level);
			parameters.put("amount", String.valueOf(amount == null ? 200.00f : amount.floatValue()));
			parameters.put("remark", remark);
			parameters.put("order_no", order_no);
			parameters.put("type", String.valueOf(type));
			parameters.put("time", time);
			parameters.put("from_account", from_account);
			parameters.put("to_account", to_account);
			parameters.put("username", username);
			parameters.put("realname", realname);
			parameters.put("payment_code", payment_code);
			StringBuilder sb = new StringBuilder();
			Set<Map.Entry<String, String>> entrySet = parameters.entrySet();
			for (Map.Entry<String, String> entry : entrySet) {
				sb.append(entry.getValue());
			}
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			md5.update(sb.append(environment.getProperty("funds.transfer.token." + handicap)).toString().getBytes());
			String md5Token = new BigInteger(1, md5.digest()).toString(16);
			HttpClient.getInstance().getITestService()
					.income(level, handicap, amount == null ? 200.00f : amount.floatValue(), remark, order_no, type,
							time, from_account, to_account, member_code, username, realname, payment_code, md5Token)
					.subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
					.subscribe(new Action1<SimpleResponseData>() {
						@Override
						public void call(SimpleResponseData data) {
							log.info(data.getMessage());
							// redisService.convertAndSend(PatternTopicEnum.BROADCAST.toString(),
							// SystemWebSocketEndpoint.class.getName() +
							// data.getMessage());
						}
					}, new Action1<Throwable>() {
						@Override
						public void call(Throwable e) {
							log.error("IncomeRequest error.", e);
							redisService.convertAndSend(RedisTopics.BROADCAST, "Error. " + e.getLocalizedMessage());
						}
					});

			SimpleResponseData responseData = new SimpleResponseData(ResponseStatus.SUCCESS.getValue(),
					getMessage("response.success"));
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			log.error("Income error", e);
			return mapper.writeValueAsString(new SimpleResponseData(ResponseStatus.FAIL.getValue(),
					getMessage("response.error") + e.getLocalizedMessage()));
		}
	}

	// @RequestMapping(value = "/outward", method = { RequestMethod.GET,
	// RequestMethod.POST })
	public String outward(@RequestParam(value = "orderNo", required = false) String orderNo)
			throws JsonProcessingException {
		try {
			String handicap = "ysc";
			String level = "N0001";
			String remark = "test";
			String account = "6215123422227890";
			String account_owner = "Owner";
			String account_name = "Lili";
			String account_bank = "中国银行";
			int usercode = 397419;
			String time = DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss");
			String username = "Dom";
			int type = IncomeRequestType.PlatFromAli.getType();
			String order_no = StringUtils.isEmpty(orderNo) ? String.valueOf(System.currentTimeMillis()) : orderNo;
			String realname = "Franco";
			String payment_code = String.valueOf(System.currentTimeMillis());
			// 计算token
			Map<String, String> parameters = new TreeMap<String, String>(new Comparator<String>() {
				@Override
				public int compare(String obj1, String obj2) {
					return obj1.compareTo(obj2);
				}
			});
			parameters.put("handicap", handicap);
			parameters.put("level", level);
			parameters.put("amount", String.valueOf(200.00f));
			parameters.put("remark", remark);
			parameters.put("order_no", order_no);
			parameters.put("time", time);
			parameters.put("account", account);
			parameters.put("account_name", account_name);
			parameters.put("account_owner", account_owner);
			parameters.put("username", username);
			parameters.put("order_no", order_no);
			parameters.put("realname", realname);
			parameters.put("usercode", String.valueOf(usercode));
			parameters.put("account_bank", account_bank);
			StringBuilder sb = new StringBuilder();
			Set<Map.Entry<String, String>> entrySet = parameters.entrySet();
			for (Map.Entry<String, String> entry : entrySet) {
				sb.append(entry.getValue());
			}
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			md5.update(sb.append(environment.getProperty("funds.transfer.token." + handicap)).toString().getBytes());
			String md5Token = new BigInteger(1, md5.digest()).toString(16);
			HttpClient.getInstance().getITestService()
					.outward(level, handicap, 200.00f, remark, order_no, time, account_owner, account_name, account,
							username, realname, md5Token, usercode, account_bank)
					.observeOn(Schedulers.io()).subscribe(new Action1<SimpleResponseData>() {
						@Override
						public void call(SimpleResponseData data) {
							log.info(data.getMessage());
							// redisService.convertAndSend(PatternTopicEnum.BROADCAST.toString(),
							// SystemWebSocketEndpoint.class.getName() +
							// data.getMessage());
						}
					}, new Action1<Throwable>() {
						@Override
						public void call(Throwable e) {
							log.error("OutwardRequest error.", e);
							redisService.convertAndSend(RedisTopics.BROADCAST, "Error. " + e.getLocalizedMessage());
						}
					});
			SimpleResponseData responseData = new SimpleResponseData(ResponseStatus.SUCCESS.getValue(),
					getMessage("response.success"));
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			log.error("Outward error", e);
			return mapper.writeValueAsString(new SimpleResponseData(ResponseStatus.FAIL.getValue(),
					getMessage("response.error") + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/realBal")
	public String realBal(@RequestParam(value = "id", required = false) Integer id,
			@RequestParam(value = "realBal", required = false) BigDecimal realBal) throws JsonProcessingException {
		try {
			allocateTransService.applyRelBal(id, realBal);
			AccountBaseInfo base = accountService.getFromCacheById(id);
			if (CommonUtils.checkDistHandicapNewVersion(base.getHandicapId())) {
				if (Objects.equals(base.getFlag(), AccountFlag.REFUND.getTypeId())) {
					allocOTaskSer.applyTask4MobileNew(id, realBal);
				} else {
					allocOTaskSer.applyTask4RobotNew(id, realBal);
				}
			} else {
				allocOTaskSer.applyTask4Mobile(id, realBal);
				allocOTaskSer.applyTask4Robot(id, realBal);
			}
			systemAccountManager.rpush(new SysBalPush(id, SysBalPush.CLASSIFY_BANK_BAL, realBal));
			GeneralResponseData<TransferEntity> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue()));
		}
	}

	@RequestMapping("/applyByCloud")
	public String applyByCloud(@RequestParam(value = "bindType", required = false) Integer bindType,
			@RequestParam(value = "acc", required = false) String acc,
			@RequestParam(value = "handi", required = false) String handi,
			@RequestParam(value = "l", required = false) Integer l,
			@RequestParam(value = "relBal", required = false) BigDecimal relBal) throws JsonProcessingException {
		try {
			TransferEntity entity = allocateTransService.applyByCloud(bindType, acc, handi, l, relBal);
			GeneralResponseData<TransferEntity> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			responseData.setData(entity);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			log.error("Outward error", e);
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue()));
		}
	}

	@RequestMapping("/ackByCloud")
	public String ackByCloud(@RequestParam(value = "orderNo", required = false) String orderNo,
			@RequestParam(value = "handicapCode", required = false) String handicapCode,
			@RequestParam(value = "frAcc", required = false) String frAcc,
			@RequestParam(value = "toId", required = false) Integer toId,
			@RequestParam(value = "amt", required = false) BigDecimal amt,
			@RequestParam(value = "acquireTime", required = false) Long acquireTime,
			@RequestParam(value = "ret", required = false) Integer ret) throws JsonProcessingException {
		try {
			allocateTransService.ackByCloud(orderNo, handicapCode, frAcc, toId, amt, acquireTime, ret == 1);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue()));
		}
	}

	@RequestMapping("/banklog")
	public String banklog(@RequestParam(value = "from_account", required = false) int from_account,
			@RequestParam(value = "to_account_owner", required = false) String to_account_owner,
			@RequestParam(value = "to_account", required = false) String to_account,
			@RequestParam(value = "transaction_no", required = false) String transaction_no,
			@RequestParam(value = "payment_code", required = false) String payment_code,
			@RequestParam(value = "type", required = false) Integer type,
			@RequestParam(value = "time", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date time,
			@RequestParam(value = "amount", required = false) Float amount,
			@RequestParam(value = "balance", required = false) Float balance,
			@RequestParam(value = "serial", required = false) Integer serial) throws JsonProcessingException {
		try {
			StringBuffer url = request.getRequestURL();
			if (url.substring(0, 15).equals("http://iopaymgr") || url.substring(0, 15).equals("http://hqiopayt")
					|| url.substring(0, 15).equals("http://localhos")) {
				ToolResponseData data = new ToolResponseData();
				BizBankLog bankLog = new BizBankLog();
				bankLog.setFromAccount(from_account);
				bankLog.setToAccountOwner(to_account_owner);
				bankLog.setToAccount(to_account);
				bankLog.setTradingTimeStr(transaction_no);
				bankLog.setTradingTime(time);
				bankLog.setCreateTime(time);
				bankLog.setAmount(BigDecimal.valueOf(amount));
				bankLog.setBalance(BigDecimal.valueOf(balance));
				bankLog.setSerial(serial);
				// TODO 给实体赋 值
				data.setBalance(balance);
				ArrayList<BizBankLog> logs = new ArrayList<BizBankLog>();
				logs.add(bankLog);
				data.setBanklogs(logs);
				redisService.rightPush(RedisTopics.BANK_STATEMENT, mapper.writeValueAsString(data));
				return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.SUCCESS.getValue()));
			} else {
				return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue()));
			}
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue()));
		}
	}

	@RequestMapping("/trans")
	public String trans(@RequestParam(value = "fromAccountId", required = false) Integer fromAccountId,
			@RequestParam(value = "toAccountId", required = false) Integer toAccountId,
			@RequestParam(value = "amount", required = false) Float amount,
			@RequestParam(value = "result", required = false) Integer result) throws JsonProcessingException {
		try {
			TransferEntity entity = new TransferEntity();
			entity.setFromAccountId(fromAccountId);
			entity.setToAccountId(toAccountId);
			entity.setAmount(amount);
			entity.setResult(result);
			transMonitorService.reportTransResult(entity);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue()));
		}
	}

	// @RequestMapping(value = "/task", method = { RequestMethod.GET,
	// RequestMethod.POST })
	public String task(@RequestParam(value = "paymentCode", required = false) String paymentCode)
			throws JsonProcessingException {
		try {
			HttpClient.getInstance().getITestService()
					.task(500f, String.valueOf(System.currentTimeMillis()), paymentCode, System.currentTimeMillis(),
							"6215123422227891", "6215123422227892", 1, "token")
					.observeOn(Schedulers.io()).subscribe(new Action1<SimpleResponseData>() {
						@Override
						public void call(SimpleResponseData data) {
							log.info(data.getMessage());
						}
					}, new Action1<Throwable>() {
						@Override
						public void call(Throwable e) {
							log.error("OutwardTask error.", e);
						}
					});
			SimpleResponseData responseData = new SimpleResponseData(ResponseStatus.SUCCESS.getValue(),
					getMessage("response.success"));
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			log.error("OutwardTask error", e);
			return mapper.writeValueAsString(new SimpleResponseData(ResponseStatus.FAIL.getValue(),
					getMessage("response.error") + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/margininit")
	public String margininit(@RequestParam(value = "id") Integer id) throws JsonProcessingException {
		BizAccount account = accountService.getById(id);
		if (Objects.isNull(account) && Objects.equals(account.getFlag(), AccountFlag.REFUND.getTypeId())
				&& Objects.nonNull(account.getMobile())) {
			BizAccountMore more = moreSer.getFromCacheByMobile(account.getMobile());
			BigDecimal init = BigDecimal.ZERO;
			for (String accId : StringUtils.trimToEmpty(more.getAccounts()).split(",")) {
				if (StringUtils.isBlank(accId) || !StringUtils.isNumeric(accId))
					continue;
				BizAccount acc = accountService.getById(Integer.valueOf(accId));
				if (Objects.nonNull(acc)) {
					init = init.add(new BigDecimal(acc.getPeakBalance()));
				}
			}
			boolean ret = rebateApiService.ackCreditLimit(account.getAccount(), init, null);
			log.info("id>>> {} ret: {}", id, ret);
		}
		return mapper.writeValueAsString(
				new ResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "success"));
	}

	@RequestMapping("/findTrans")
	public String findTrans(@RequestParam(value = "id") Integer id) throws JsonProcessingException {
		TransferEntity entity = allocOTaskSer.applyTask4Mobile(id, null);
		ResponseData<TransferEntity> responseData = new ResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "success");
		responseData.setData(entity);
		return mapper.writeValueAsString(responseData);
	}

	@RequestMapping("/logs")
	public String logs(@RequestParam(value = "fromAccount") Integer fromAccount) throws JsonProcessingException {
		List<BizBankLog> logList = new ArrayList<>();
		List<SearchFilter> filterToList = DynamicSpecifications.build(request);
		filterToList.add(new SearchFilter("fromAccount", SearchFilter.Operator.EQ, fromAccount));
		SearchFilter[] filterToArray = filterToList.toArray(new SearchFilter[filterToList.size()]);
		Specification<BizBankLog> specif = DynamicSpecifications.build(BizBankLog.class, filterToArray);
		List<BizBankLog> list = bankLogDao.findAll(specif);
		if (!CollectionUtils.isEmpty(list)) {
			systemAccountManager.rpush(new SysBalPush(fromAccount, SysBalPush.CLASSIFY_BANK_LOGS, list));
		}
		if (CollectionUtils.isEmpty(logList)) {
			return mapper.writeValueAsString(new ResponseData<>(ResponseStatus.FAIL.getValue(), "no data."));
		}
		return mapper.writeValueAsString(
				new ResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "success"));
	}

	@RequestMapping("/limitAct")
	public String limitAct(@RequestParam(value = "reqId") long reqId, @RequestParam(value = "logId") long logId)
			throws JsonProcessingException {
		BizBankLog log = bankLogService.findBankFlowById(logId);
		if (Objects.isNull(log)) {
			return mapper.writeValueAsString(new ResponseData<>(ResponseStatus.FAIL.getValue(), "流水不存在"));
		}
		BizIncomeRequest req = incomeRequestService.findById(reqId);
		if (Objects.isNull(req)) {
			return mapper.writeValueAsString(new ResponseData<>(ResponseStatus.FAIL.getValue(), "提额任务不存在"));
		}
		if (log.getAmount().abs().intValue() != req.getAmount().abs().intValue()) {
			return mapper.writeValueAsString(new ResponseData<>(ResponseStatus.FAIL.getValue(), "提额任务金额与流水金额不符"));
		}
		rebateApiService.ackCreditLimit(log, req);
		bankLogService.updateBankLog(log.getId(), BankLogStatus.Matched.getStatus());
		incomeRequestService.updateStatusById(req.getId(), OutwardTaskStatus.Matched.getStatus());
		return mapper.writeValueAsString(
				new ResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "success"));
	}

	@RequestMapping("/rebateCommissionDaily")
	public String rebateCommissionDaily() throws JsonProcessingException {
		new Thread(() -> {
			String day = CommonUtils.getDateFormatyyyyMMdd2Str(new Date(System.currentTimeMillis() - 86400000));
			commissionHandler.commissionSummarymanually(day);
		}).start();
		return mapper.writeValueAsString(
				new ResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "success"));
	}

	@RequestMapping("/rebateCommissionDailyByDate")
	public String rebateCommissionDailyByData(String date) throws JsonProcessingException {
		new Thread(() -> {
			rebateApiService.commissionDailyReturnsummary(date, 0);
		}).start();
		return mapper.writeValueAsString(
				new ResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "success"));
	}

	@RequestMapping("/rebateAccfeedback")
	public String rebateAccfeedback(@RequestParam(value = "audit") Integer audit,
			@RequestParam(value = "oriAcc") String oriAcc, @RequestParam(value = "oriOwner") String oriOwner,
			@RequestParam(value = "currAcc") String currAcc, @RequestParam(value = "currOwner") String currOwner)
			throws JsonProcessingException {
		List<BizAccount> hisList = accountService.findByAccount(StringUtils.trimToEmpty(oriAcc));
		hisList = hisList.stream().filter(p -> Objects.equals(p.getFlag(), 2)).collect(Collectors.toList());
		BizAccount account = org.apache.shiro.util.CollectionUtils.isEmpty(hisList) ? null : hisList.get(0);
		if (Objects.isNull(account)) {
			return mapper.writeValueAsString(new ResponseData<>(ResponseStatus.FAIL.getValue(), "账号不存在"));
		}
		account.setAccount(currAcc);
		account.setOwner(currOwner);
		account.setStatus(AccountStatus.StopTemp.getStatus());
		accountService.updateBaseInfo(account);
		accountService.broadCast(account);
		cabanaService.updAcc(account.getId());
		rebateApiService.auditAcc(Objects.equals(audit, 1), oriAcc, oriOwner, currAcc, currOwner);
		return mapper.writeValueAsString(
				new ResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "success"));
	}

	@RequestMapping("/sysballogs")
	public String sysballogs(@RequestParam(value = "logid") long logid) throws JsonProcessingException {
		BizBankLog log = bankLogService.get(logid);
		List<BizBankLog> logs = new ArrayList<>();
		logs.add(log);
		systemAccountManager.rpush(new SysBalPush(log.getFromAccount(), SysBalPush.CLASSIFY_BANK_LOGS, logs));
		return mapper.writeValueAsString(new GeneralResponseData(1, "success"));
	}

	@RequestMapping("/sysbalbal")
	public String sysbalbal(@RequestParam(value = "id") Integer id, @RequestParam(value = "bal") BigDecimal bal)
			throws JsonProcessingException {
		allocateTransService.applyRelBal(id, bal);
		systemAccountManager.rpush(new SysBalPush(id, SysBalPush.CLASSIFY_BANK_BAL, bal));
		return mapper.writeValueAsString(new GeneralResponseData(1, "success"));
	}

	@RequestMapping("/test00")
	public String test00(@RequestParam(value = "id") long id) throws JsonProcessingException {
		return mapper.writeValueAsString(new GeneralResponseData(1, "success"));
	}

	/**
	 * @RequestMapping("/in/get") public String inGet(@RequestParam(value =
	 * "accId", required = false) Integer accId,
	 *
	 * @RequestParam(value = "bankbalance", required = false) Float bankbalance)
	 *                     throws JsonProcessingException{ try {
	 *                     allocateTransService.applyByFrom(accId, new
	 *                     BigDecimal(bankbalance));
	 *                     allocateTransService.applyRelBal(accId, new
	 *                     BigDecimal(bankbalance)); return
	 *                     mapper.writeValueAsString( new
	 *                     GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
	 *                     } catch (Exception e) { return
	 *                     mapper.writeValueAsString(new
	 *                     GeneralResponseData<>(ResponseStatus.FAIL.getValue()));
	 *                     } }
	 */
	@RequestMapping("/in/ack")
	public String inAck(@RequestParam(value = "accId", required = false) Integer accId,
			@RequestParam(value = "amount", required = false) Float amount,
			@RequestParam(value = "taskId", required = false) Long taskId,
			@RequestParam(value = "toId", required = false) Integer toId,
			@RequestParam(value = "result", required = false) Integer result,
			@RequestParam(value = "remark", required = false) String remark,
			@RequestParam(value = "bankbalance", required = false) Float bankbalance) throws JsonProcessingException {
		try {
			TransferEntity entity = new TransferEntity();
			entity.setFromAccountId(accId);
			entity.setAmount(amount);
			entity.setResult(result);
			entity.setBalance(bankbalance);
			if (taskId != null) {
				entity.setTaskId(taskId);
			}
			if (toId != null) {
				entity.setToAccountId(toId);
			}
			entity.setRemark(remark);
			allocateTransService.ackByRobot(entity);
			transMonitorService.reportTransResult(entity);
			allocateTransService.applyRelBal(entity.getFromAccountId(), new BigDecimal(entity.getBalance()));
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue()));
		}
	}

	/*
	 * @RequestMapping("/out/get") public String outGet(@RequestParam(value =
	 * "accId", required = false) Integer accId,
	 *
	 * @RequestParam(value = "bankbalance", required = false) Float bankbalance,
	 *
	 * @RequestParam(value = "result", required = false) Integer result) throws
	 * JsonProcessingException{ try { TransferEntity entity = new
	 * TransferEntity(); entity.setFromAccountId(accId);
	 * entity.setBalance(bankbalance);
	 * outwardTaskAllocateService.applyTask4Robot(entity.getF romAccountId(),
	 * new BigDecimal(entity.getBalance())); return mapper.writeValueAsString(
	 * new GeneralResponseData<>(GeneralResponseData.ResponseStatus.
	 * SUCCESS.getValue ())); } catch (Exception e) { return
	 * mapper.writeValueAsStri ng(new
	 * GeneralResponseData<>(ResponseStatus.FAIL.getValue())); } }
	 */
	@RequestMapping("/out/ack")
	public String outAck(@RequestParam(value = "accId", required = false) Integer accId,
			@RequestParam(value = "amount", required = false) Float amount,
			@RequestParam(value = "taskId", required = false) Long taskId,
			@RequestParam(value = "result", required = false) Integer result,
			@RequestParam(value = "remark", required = false) String remark,
			@RequestParam(value = "bankbalance", required = false) Float bankbalance) throws JsonProcessingException {
		try {
			TransferEntity entity = new TransferEntity();
			entity.setFromAccountId(accId);
			entity.setTaskId(taskId);
			entity.setAmount(amount);
			entity.setResult(result);
			entity.setRemark(remark);
			entity.setBalance(bankbalance);
			outwardTaskAllocateService.ack4Robot(entity);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue()));
		}
	}

	@RequestMapping("/token")
	private String returnToken(@RequestParam(value = "content", required = false) String content) {
		String calToken = CommonUtils.md5digest(content + appProperties.getRebatesalt());
		return calToken;
	}

	@RequestMapping("/flushByid")
	private String flushByid(@RequestParam(value = "id", required = false) Integer id) throws JsonProcessingException {
		BizAccount acc = accountService.getById(id);
		accountService.broadCast(acc);
		hostMonitorService.update(acc);
		cabanaService.updAcc(acc.getId());
		return mapper.writeValueAsString(
				new GeneralResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "刷新成功。"));
	}

	@RequestMapping("/invalidate")
	private String invalidate(@RequestParam(value = "accId", required = false) Integer accId)
			throws JsonProcessingException {
		try {
			AssignBalance assignBalance = new AssignBalance();
			assignBalance.validateDate(accId);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue()));
		}
	}

	@RequestMapping("/conciliate")
	private String conciliate(@RequestParam(value = "accId", required = false) Integer accId)
			throws JsonProcessingException {
		try {
			cabanaService.conciliate(accId, "2018-07-10");
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue()));
		}
	}

	@RequestMapping("/testSchedule")
	private String testSchedule() throws JsonProcessingException {
		try {
			rebateStatisticsSer.scheduleStatisticsReabte();
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue()));
		}
	}

	@RequestMapping("/deleteActiveAccountTransKey")
	private String deleteActiveAccountTransKey() throws JsonProcessingException {
		try {
			String activeKeys = redisService.getStringRedisTemplate().opsForHash()
					.entries(RedisKeys.ACTIVE_ACCOUNT_KEYS).toString().replace("{", "").replace("}", "");
			String[] keys = activeKeys.split(",");
			for (String str : keys) {
				if (str != null && str.length() != 0) {
					String key = str.split("=")[0];
					String frId = str.split(":")[2];
					AccountBaseInfo toAccount = accountService.getFromCacheById(Integer.parseInt(frId));
					if (null != toAccount && null != toAccount.getStatus()
							&& toAccount.getStatus() == AccountStatus.Inactivated.getStatus()) {
						redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACTIVE_ACCOUNT_KEYS).delete(key);
					}
				}
			}
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue()));
		}
	}
}
