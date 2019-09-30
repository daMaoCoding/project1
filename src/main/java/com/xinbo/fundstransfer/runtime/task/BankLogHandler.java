package com.xinbo.fundstransfer.runtime.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.BankLogOrderUtils;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.RefundUtil;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.DynamicSpecifications;
import com.xinbo.fundstransfer.domain.ResponseDataNewPay;
import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.*;
import com.xinbo.fundstransfer.domain.pojo.*;
import com.xinbo.fundstransfer.domain.repository.BankLogRepository;
import com.xinbo.fundstransfer.newpay.inputdto.AutoResetInputDTO;
import com.xinbo.fundstransfer.newpay.inputdto.ConfirmInputDTO;
import com.xinbo.fundstransfer.newpay.service.NewPayService;
import com.xinbo.fundstransfer.report.SystemAccountManager;
import com.xinbo.fundstransfer.report.up.ReportInitParam;
import com.xinbo.fundstransfer.runtime.MemCacheUtils;
import com.xinbo.fundstransfer.service.*;
import com.xinbo.fundstransfer.unionpay.ysf.service.YSFService;
import com.xinbo.fundstransfer.utils.ServiceDomain;
import com.xinbo.fundstransfer.utils.randutil.JedisLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 银行流水处理
 */
@Slf4j
@Component
public class BankLogHandler {
	private ObjectMapper mapper = new ObjectMapper();
	@Autowired
	private BankLogService bankLogService;
	@Autowired
	private AccountService accountService;
	@Autowired
	private RedisService redisService;
	@Autowired
	private IncomeRequestService incomeRequestService;
	@Autowired
	private OutwardTaskService outwardTaskService;
	@Autowired
	private TransactionLogService transactionLogService;
	@Autowired
	private AllocateTransferService allocateTransferService;
	@Autowired
	private AllocateTransService allocateTransService;
	@Autowired
	private AccountExtraService accountExtraService;
	@Autowired
	private NewPayService newPayService;
	@Autowired
	private HandicapService handicapService;
	@Autowired
	private TransMonitorService transMonitorService;
	@Autowired
	private RebateApiService rebateApiService;
	@Autowired
	private AccountRebateService accountRebateService;
	@Autowired
	private AllocateOutwardTaskService outwardTaskAllocateService;
	@Autowired
	private BankLogRepository bankLogRepository;
	private boolean isRuning = true;
	@Autowired
	private AccountMoreService accountMoreSer;
	@Autowired
	private HostMonitorService hostMonitorService;
	@Autowired
	private CabanaService cabanaService;
	@Autowired
	private YSFService ysfService;
	@Autowired
	private SystemAccountManager systemAccountManager;
	@Autowired
	private AllocateIncomeAccountService allocInAccSer;
	/**
	 * 传递给前端的帐号信息，前端根据自己选择的盘口与帐号来确定是否要刷新页面数据 ，只刷该事件类型的数据
	 */
	private IncomeAuditWs noticeEntity = new IncomeAuditWs();
	private static boolean checkHostRunRight = false;

	@Value("${service.tag}")
	public void setServiceTag(String serviceTag) {
		if (Objects.nonNull(serviceTag)) {
			checkHostRunRight = ServiceDomain.valueOf(serviceTag) == ServiceDomain.CACHFLOW;
		}
	}

	// public BankLogHandler() {
	// try {
	// ysfService = SpringContextUtils.getBean(YSFService.class);
	// bankLogService = SpringContextUtils.getBean(BankLogService.class);
	// accountService = SpringContextUtils.getBean(AccountService.class);
	// incomeRequestService =
	// SpringContextUtils.getBean(IncomeRequestService.class);
	// outwardTaskService =
	// SpringContextUtils.getBean(OutwardTaskService.class);
	// redisService = SpringContextUtils.getBean(RedisService.class);
	// allocateTransferService =
	// SpringContextUtils.getBean(AllocateTransferService.class);
	// allocateTransService =
	// SpringContextUtils.getBean(AllocateTransService.class);
	// transactionLogService =
	// SpringContextUtils.getBean(TransactionLogService.class);
	// accountExtraService =
	// SpringContextUtils.getBean(AccountExtraService.class);
	// // appProperties = SpringContextUtils.getBean(AppProperties.class);
	// newPayService = SpringContextUtils.getBean(NewPayService.class);
	// handicapService = SpringContextUtils.getBean(HandicapService.class);
	// transMonitorService =
	// SpringContextUtils.getBean(TransMonitorService.class);
	// rebateApiService = SpringContextUtils.getBean(RebateApiService.class);
	// accountRebateService =
	// SpringContextUtils.getBean(AccountRebateService.class);
	// outwardTaskAllocateService =
	// SpringContextUtils.getBean(AllocateOutwardTaskService.class);
	// sysBalService = SpringContextUtils.getBean(SysBalService.class);
	// bankLogRepository = SpringContextUtils.getBean(BankLogRepository.class);
	// accountMoreSer = SpringContextUtils.getBean(AccountMoreService.class);
	// hostMonitorService =
	// SpringContextUtils.getBean(HostMonitorService.class);
	// cabanaService = SpringContextUtils.getBean(CabanaService.class);
	// systemAccountManager =
	// SpringContextUtils.getBean(SystemAccountManager.class);
	// noticeEntity = new IncomeAuditWs();
	// } catch (Exception e) {
	// log.error("", e);
	// }
	// }
	private int counts = 0;

	@Scheduled(fixedRate = 100)
	public void saveBankLog() {
		if (!checkHostRunRight)
			return; 
		log.debug("begin handler banklog ...");
		Object json = null;
		while (true) {
			json = redisService.leftPop(RedisTopics.BANK_STATEMENT);
			if (null == json || "" == json || "null" == json) {
				break;
			}
			counts++;
			try {
				// json = MemCacheUtils.getInstance().getBanklogs().poll();
				if (StringUtils.isNotBlank(json.toString())) {
					List<BizBankLog> logs = new ArrayList<>();
					List<BizBankLog> accountsLogs = new ArrayList<>();
					log.debug("处理流水:{}", json);
					ToolResponseData data = mapper.readValue(json.toString(), ToolResponseData.class);
					int fromId = 0;
					if (!CollectionUtils.isEmpty(data.getBanklogs())) {
						fromId = data.getBanklogs().get(0).getFromAccount();
						AccountBaseInfo base = accountService.getFromCacheById(fromId);
						if (Objects.nonNull(base)) {
							if (BankLogOrderUtils.reverse(base, base.getBankType()))
								Collections.reverse(data.getBanklogs());
						}
					}
					for (BizBankLog o : data.getBanklogs()) {
						fromId = o.getFromAccount();
						// 如果交易时间超过十三位则不处理 最小的14位是2286年
						if (null == o.getTradingTime() || o.getTradingTime().getTime() == 0
								|| Long.toString(o.getTradingTime().getTime()).length() > 13)
							continue;
						// 加锁进行限定
						StringRedisTemplate jedis = redisService.getStringRedisTemplate();
						JedisLock lock = new JedisLock(jedis,
								String.format("FLOWING_LOCK_%s:%s", fromId, o.getAmount()), 3000, 2000);
						try {
							lock.acquire();
							AccountBaseInfo baseInfo = accountService.getFromCacheById(fromId);
							if (null == baseInfo)
								continue;
							List<SearchFilter> filters = new ArrayList<>();
							// 如果是云闪付的入款卡 转入的流水存在手机和云闪付同时抓的情况，对比数据不能重复入库
							if (null != baseInfo && null != baseInfo.getSubType() && baseInfo.getFlag() == 2
									&& baseInfo.getSubType() == 3 && baseInfo.getType() == 1
									&& o.getAmount().floatValue() > 0) {
								// 如果是云闪付抓的流水则把serial设置为1
								if (Objects.isNull(o.getBalance()) || o.getBalance().compareTo(BigDecimal.ZERO) == 0)
									o.setSerial(1);
								filters.add(
										new SearchFilter("fromAccount", SearchFilter.Operator.EQ, o.getFromAccount()));
								filters.add(new SearchFilter("amount", SearchFilter.Operator.EQ,
										o.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP)));
								filters.add(new SearchFilter("tradingTime", SearchFilter.Operator.GT,
										DateUtils.addHours(o.getTradingTime(), -1)));
								filters.add(new SearchFilter("tradingTime", SearchFilter.Operator.LT,
										DateUtils.addHours(o.getTradingTime(), 1)));
							} else {
								// 如果是app的入款卡入款，只对比时间、金额、入款卡
								if (null != baseInfo.getType() && null != baseInfo.getFlag() && baseInfo.getType() == 1
										&& baseInfo.getFlag() == 2 && o.getAmount().floatValue() > 0) {
									filters.add(new SearchFilter("fromAccount", SearchFilter.Operator.EQ,
											o.getFromAccount()));
									filters.add(new SearchFilter("amount", SearchFilter.Operator.EQ,
											o.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP)));
									filters.add(new SearchFilter("tradingTime", SearchFilter.Operator.GT,
											DateUtils.addHours(o.getTradingTime(), -1)));
									filters.add(new SearchFilter("tradingTime", SearchFilter.Operator.LT,
											DateUtils.addHours(o.getTradingTime(), 1)));
								} else {
									// 平安银行 入款卡比较 金额，对方姓名，交易时间有可能减一秒
									filters.add(new SearchFilter("fromAccount", SearchFilter.Operator.EQ,
											o.getFromAccount()));
									filters.add(new SearchFilter("amount", SearchFilter.Operator.EQ,
											o.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP)));
									if (baseInfo.getBankType().equals("平安银行")) {
										filters.add(new SearchFilter("tradingTime", SearchFilter.Operator.LTE,
												o.getTradingTime()));
										filters.add(new SearchFilter("tradingTime", SearchFilter.Operator.GTE,
												DateUtils.addSeconds(o.getTradingTime(), -1)));
									} else {
										filters.add(
												new SearchFilter("balance", SearchFilter.Operator.EQ, o.getBalance()));
										filters.add(new SearchFilter("tradingTime", SearchFilter.Operator.EQ,
												o.getTradingTime()));
									}
									if (StringUtils.isNotBlank(o.getToAccount())) {
										filters.add(new SearchFilter("toAccount", SearchFilter.Operator.EQ,
												o.getToAccount().trim()));
										o.setToAccount(o.getToAccount().trim());
									}
									if (StringUtils.isNotBlank(o.getToAccountOwner())) {
										filters.add(new SearchFilter("toAccountOwner", SearchFilter.Operator.EQ,
												o.getToAccountOwner()));
									}
								}
							}

							Specification<BizBankLog> specification = DynamicSpecifications.build(BizBankLog.class,
									filters.toArray(new SearchFilter[filters.size()]));
							List<BizBankLog> finds = bankLogService.findAll(specification);
							// 如果是云闪付的入款卡，且云闪付流水先抓到，app的流水后抓到，需要把对应的云闪付的余额覆盖掉。
							boolean ysf = null != baseInfo
									&& Objects.equals(baseInfo.getFlag(), AccountFlag.REFUND.getTypeId())
									&& (Objects.equals(baseInfo.getSubType(), InBankSubType.IN_BANK_YSF.getSubType())
											|| Objects.equals(baseInfo.getSubType(),
													InBankSubType.IN_BANK_YSF_MIX.getSubType()))
									&& Objects.equals(baseInfo.getType(), AccountType.InBank.getTypeId());
							boolean amountGtZero = Objects.nonNull(o.getAmount())
									&& o.getAmount().compareTo(BigDecimal.ZERO) > 0;
							if (ysf && amountGtZero && !CollectionUtils.isEmpty(finds)) {
								BizBankLog his = finds.get(0);
								// String oppAccount =
								// StringUtils.trimToEmpty(his.getToAccount());
								boolean eqZeroHisBalance = Objects.isNull(his.getBalance())
										|| his.getBalance().compareTo(BigDecimal.ZERO) == 0;
								/*
								 * boolean eqFourHisAccount =
								 * StringUtils.isNumeric(oppAccount) &&
								 * oppAccount.length() == 4;
								 */
								boolean checkNewFromApp = Objects.nonNull(o.getBalance())
										&& o.getBalance().compareTo(BigDecimal.ZERO) != 0;
								// 确保云闪付抓的流水 余额没有被改过(存在重复上报的情况，不能重复传给仙人)
								// 如果是云闪付的入款卡且app的流水后抓到 o.getBalance()>0
								if (eqZeroHisBalance && checkNewFromApp) {
									bankLogService.updateBalanceByid(his.getId(), o.getBalance());
									o.setId(his.getId());
									o.setStatus(his.getStatus());
									o.setCreateTime(his.getCreateTime());
									accountsLogs.add(o);
								}
							}
							// 如果是入款卡 且是app抓的流水，且一个小时内存在相同的数据 对比serial
							// 是否一样，不一样则入库
							if (null != baseInfo && baseInfo.getFlag() == 2 && baseInfo.getType() == 1
									&& o.getAmount().floatValue() > 0 && o.getBalance().compareTo(BigDecimal.ZERO) == 1
									&& !CollectionUtils.isEmpty(finds)) {
								boolean flag = true;
								for (int i = 0; i < finds.size(); i++) {
									BizBankLog lo = finds.get(i);
									// 历史数据为空当1处理
									if (null == lo.getSerial())
										lo.setSerial(1);
									if (null == o.getSerial() || lo.getSerial() == o.getSerial()
											|| o.getSerial() == 0) {
										flag = false;
										break;
									}
								}
								if (flag)
									finds = null;
							}

							if (null == finds || finds.size() <= 0) {
								AccountBaseInfo base = accountService.getFromCacheById(o.getFromAccount());
								String bankType = Objects.isNull(base) ? StringUtils.EMPTY : base.getBankType();
								// 金额小于50，并且摘要中包含：手续费、汇费、收费（建行）字眼的都不参与匹配
								if (Math.abs(o.getAmount().floatValue()) < 50 && StringUtils.isNotBlank(o.getSummary())
										&& (o.getSummary().contains("手续费") || o.getSummary().contains("汇费")
												|| o.getSummary().contains("收费"))) {
									o.setStatus(BankLogStatus.Fee.getStatus());
								} else if (RefundUtil.refund(bankType, o.getSummary())) {
									o.setStatus(BankLogStatus.Refunding.getStatus());
								}
								// 如果是冲正的流水 则找对应的任务重新生成
								// if (o.getStatus() ==
								// BankLogStatus.Refunding.getStatus().intValue())
								// {
								// if (reuseTask(o))
								// o.setStatus(BankLogStatus.Refunded.getStatus());
								// }
								o = bankLogService.save(o);
								logs.add(o);
								accountsLogs.add(o);
								log.info(
										"Banklog saved[DB]: fromAccount:{},amount:{},summary:{},balance:{},tradingTime:{},createTime:{}",
										o.getFromAccount(), o.getAmount(), o.getSummary(), o.getBalance(),
										o.getTradingTime(), o.getCreateTime());
								if (null != o.getStatus()
										&& o.getStatus().intValue() == BankLogStatus.Matching.getStatus().intValue()) {
									// 自动匹配
									match(o);
								}
								if (!Objects.equals(o.getStatus(), BankLogStatus.Fee.getStatus())
										&& !Objects.equals(o.getStatus(), BankLogStatus.Refunding.getStatus())
										&& Objects.nonNull(o.getAmount())
										&& o.getAmount().compareTo(BigDecimal.ZERO) < 0
										&& Objects.nonNull(o.getTradingTime())
										&& o.getTradingTime().getTime() >= CommonUtils.getStartMillisOfAmountDaily()) {
									if (Objects.nonNull(base)) {
										String idStr = String.valueOf(base.getId());
										RedisTemplate<String, String> template = redisService.getFloatRedisTemplate();
										Long nextExpire = CommonUtils.getExpireTime4AmountDaily()
												- System.currentTimeMillis();
										// 当日出款限额
										// 手机银行&PC
										redisService.increment(RedisKeys.AMOUNT_SUM_BY_DAILY_OUTWARD, idStr,
												o.getAmount().abs().floatValue());
										Long hisExpire = template.boundHashOps(RedisKeys.AMOUNT_SUM_BY_DAILY_OUTWARD)
												.getExpire();
										if (hisExpire != null && hisExpire < 0) {// 当日清零
											template.expire(RedisKeys.AMOUNT_SUM_BY_DAILY_OUTWARD, nextExpire,
													TimeUnit.MILLISECONDS);
										}

										{// 当日出款笔数
											redisService.increment(RedisKeys.COUNT_SUM_BY_DAILY_OUTWARD, idStr, 1);
											Long hisExpireCount = template
													.boundHashOps(RedisKeys.COUNT_SUM_BY_DAILY_OUTWARD).getExpire();
											if (hisExpireCount != null && hisExpireCount < 0) {// 当日清零
												template.expire(RedisKeys.COUNT_SUM_BY_DAILY_OUTWARD, nextExpire,
														TimeUnit.MILLISECONDS);
											}
										}
									}
								}
							} else {
								log.debug("Banklog already exists. {}", o);
							}
						} finally {
							lock.release();
						}
					}
					// 传流水到接口
					try {
						rebateApiService.logs(logs);
					} catch (Exception e) {
						log.error("BankLogToRebate 失败!", e.getMessage());
					}
					systemAccountManager.rpush(new SysBalPush(fromId, SysBalPush.CLASSIFY_BANK_LOGS, accountsLogs));
					// 一批流水数据处理完毕，看卡类型是否为客户绑定卡，如果是客户绑定卡，看是否要恢复额度
					List<BizBankLog> banklogs = data.getBanklogs();
					if (!CollectionUtils.isEmpty(banklogs)) {
						AccountBaseInfo base = accountService.getFromCacheById(banklogs.get(0).getFromAccount());
						if (null == base)
							continue;
						if (Objects.equals(base.getType(), AccountType.BindCustomer.getTypeId())) {
							Object credit = redisService.getFloatRedisTemplate().opsForHash()
									.get(RedisKeys.ACCOUNT_CREDIT_RESTORE, String.valueOf(base.getId()));
							if (Objects.nonNull(credit)) {
								DecimalFormat decimalFormat = new DecimalFormat(".00");
								String creditStr = decimalFormat.format(credit);
								try {
									float bal = Float.parseFloat(creditStr);
									if (bal > 0) {
										String code = handicapService.findFromCacheById(base.getHandicapId()).getCode();
										if (StringUtils.isNotBlank(code)) {
											AutoResetInputDTO inputDTO = new AutoResetInputDTO();
											inputDTO.setAccount(base.getAccount());
											inputDTO.setInTime(System.currentTimeMillis());
											inputDTO.setMoney(Float.parseFloat(creditStr));
											inputDTO.setOpenMan(base.getOwner());
											inputDTO.setOid(Integer.parseInt(code));
											ResponseDataNewPay responseDataNewPay = newPayService.autoReset(inputDTO);
											if (responseDataNewPay.getCode() == 200) {
												redisService.getFloatRedisTemplate().opsForHash().put(
														RedisKeys.ACCOUNT_CREDIT_RESTORE, String.valueOf(base.getId()),
														"0");
												log.info("账户: {}，恢复额度: {} 成功！", base.getId(), creditStr);
											} else {
												log.error("账户: {}，恢复额度: {} 失败，失败原因: {} ！", base.getId(), creditStr,
														responseDataNewPay.getMsg());
											}
										}
									}
								} catch (Exception e) {
									log.error("账户: {}，恢复额度: {} 异常，异常原因: {} ！", base.getId(), creditStr, e.getMessage());
								}
							}
						}
					}
					if (logs.size() > 0) {
						BizBankLog log = logs.get(0);
						int frId = log.getFromAccount();
						AccountBaseInfo baseInfo = accountService.getFromCacheById(frId);
						if (baseInfo.getStatus() == AccountStatus.Normal.getStatus()
								&& baseInfo.getBankType().equals("民生银行")) {
							Long total = bankLogRepository.getTotalBankLog(baseInfo.getId(),
									CommonUtils.getDateFormat2Str(CommonUtils.getZeroDate(log.getCreateTime())),
									CommonUtils.getNowDate());
							if (Objects.nonNull(total) && total > 450) {
								// 入款卡超过450条流水，提示停用
								if (baseInfo.getType() == AccountType.InBank.getTypeId()) {
									String info = CommonUtils.genSysMsg4WS(null,
											SystemWebSocketCategory.MessageToAllUser,
											"民生银行最多只能抓取500条流水数据" + "</br>" + "编号：" + baseInfo.getAlias() + "，开户人："
													+ baseInfo.getOwner() + "</br>目前流水记录数为：" + total + "。建议停用此卡！！");
									redisService.convertAndSend(RedisTopics.BROADCAST, info);
								} else {
									accountService.toStopTemp(baseInfo.getId(), null, AppConstants.USER_ID_4_ADMIN);
								}
							}
						}
					}
				} else {
					TimeUnit.MILLISECONDS.sleep(3000);
					log.trace("No data, sleep 3000 ms");
				}
			} catch (Exception e) {
				redisService.rightPush(RedisTopics.BANK_STATEMENT, String.valueOf(json));
				log.error("处理流水异常  队列流水参数: {} ", json);
				log.error("处理流水异常  error:", e);
				try {
					TimeUnit.MILLISECONDS.sleep(3000);
				} catch (InterruptedException e1) {
					log.error("", e1);
				}
			}
			if (counts > 15) {
				try {
					counts = 0;
					TimeUnit.MILLISECONDS.sleep(1000);
					log.info("Counts>15 sleep 1000 ms");
				} catch (InterruptedException e) {
					log.error("", e);
				}
			}
		}
	}

	private void match(BizBankLog bankLog) {
		try {
			boolean matched = false;
			BizIncomeRequest incomeRequest = null;
			BizOutwardTask outwardTask = null;
			AccountBaseInfo bizAccount = accountService.getFromCacheById(bankLog.getFromAccount());
			// 转入, 银行流水有二种可能：1、+入款（可能是系统中转则会有二条银行流水，from,to帐号同时产生流水），2、-出款
			if (bankLog.getAmount().floatValue() > 0) {
				/** 入款与流水记录匹配最大时间间隔（小时） */
				Integer validIntervalTimeHour = Integer.parseInt(
						MemCacheUtils.getInstance().getSystemProfile().getOrDefault("INCOME_MATCH_HOURS", "2"));
				// 有的流水没有具体的时间 只有当天的日期，无法匹配，这里做转换。
				if (bankLog.getTradingTime().toString().indexOf("00:00:00") > -1) {
					bankLog.setTradingTime(bankLog.getCreateTime());
				}
				List<BizIncomeRequest> incomes;
				// 匹配规则，带小数的直接去匹配
				if (bankLog.getAmount().intValue() != bankLog.getAmount().floatValue()) {
					// 匹配规则：入款帐号相同--》入款金额相等--》入款时间在设置范围-->状态是未匹配和已经匹配
					// 根据卡是什么类型 决定要不要限制时间查询，如果不是入款卡就不限制时间。
					Object[] parms = { bankLog.getFromAccount(), bankLog.getTradingTime(),
							AccountType.InBank.getTypeId() == bizAccount.getType() ? validIntervalTimeHour : null,
							IncomeRequestStatus.Matching.getStatus(), IncomeRequestStatus.Matched.getStatus(),
							bankLog.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP), 1, bizAccount.getType() };
					incomes = incomeRequestService.findAll(parms);
					log.info("Matching! FromAccount:{},TradingTime:{},Type:{},Amount:{},isNull:{}",
							bankLog.getFromAccount(), bankLog.getTradingTime(), bizAccount.getType(),
							Math.abs(bankLog.getAmount().floatValue()), (null == incomes || incomes.size() <= 0));
					SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					// 如果存在两个同样金额的正在匹配的单则不进行匹配
					// 查找的订单时间上限为流水创建时间或者交易时间(DateUtils.addHours(bankLog.getTradingTime(),
					// 2))
					Date start = AccountType.InBank.getTypeId() == bizAccount.getType()
							? DateUtils.addHours(bankLog.getTradingTime(), -2)
							: (DateUtils.addHours(bankLog.getTradingTime(), -12));
					Date end = AccountType.InBank.getTypeId() == bizAccount.getType() ? bankLog.getTradingTime()
							: (DateUtils.addHours(bankLog.getTradingTime(), 13));
					int matchingCounts = incomeRequestService.findIncomeCounts(bankLog.getFromAccount(),
							sd.format(start), sd.format(end), IncomeRequestStatus.Matching.getStatus(),
							bankLog.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP));
					if (!CollectionUtils.isEmpty(incomes) && matchingCounts == 1) {
						matched = true;
						incomeRequest = incomes.get(0);
						// 如果不是入款卡 找到单据了 则是下发的单
						if (!incomeRequest.getType().equals(IncomeRequestType.PlatFromBank.getType())) {
							bankLog.setTaskId(incomeRequest.getId());
							bankLog.setTaskType(SysBalTrans.TASK_TYPE_INNER);
							bankLog.setOrderNo(incomeRequest.getOrderNo());
						} else {
							bankLog.setTaskId(incomeRequest.getId());
							bankLog.setTaskType(SysBalTrans.TASK_TYPE_INCOME);
							bankLog.setOrderNo(incomeRequest.getOrderNo());
						}
						// 入款存在一个单对应两个相同的流水，三个小时内第二条流水过来时也会找到已经匹配的单，这里做处理，如果是入款单，且状态是1(已匹配)
						// 不参与匹配
						if (incomeRequest.getType().equals(IncomeRequestType.PlatFromBank.getType())
								&& incomeRequest.getStatus().equals(IncomeRequestStatus.Matched.getStatus())) {
							matched = false;
							incomeRequest = null;
							// 无法自动匹配的 都会显示在前端
							noticeEntity.setIncomeAuditWsFrom(IncomeAuditWsEnum.FromInBankLog.ordinal());
							noticeEntity.setAccount(bankLog.getFromAccountNO());
							noticeEntity.setAccountId(bankLog.getFromAccount());
							noticeEntity.setMessage("有新的流水");
							// 广播消息通知前端有新的记录，browser通过websocket事件刷新内容
							try {
								redisService.convertAndSend(RedisTopics.INCOME_REQUEST,
										mapper.writeValueAsString(noticeEntity));
								log.info("redis广播发消息给前端，incomes 查询不到记录");
							} catch (JsonProcessingException e) {
								log.error("", e);
							}
						}
						if (!matched) {
							BizHandicap handicap = handicapService.findFromCacheById(bizAccount.getHandicapId());
							if (Objects.nonNull(handicap) && Objects.nonNull(handicap.getCode())
									&& Objects.nonNull(bizAccount) && Objects.nonNull(bizAccount.getAccount())
									&& Objects.nonNull(bankLog) && Objects.nonNull(bankLog.getAmount())) {
								// 上报平台此流水金额不能使用于新提单
								incomeRequestService.moneyBeUsed(Integer.valueOf(handicap.getCode()),
										bizAccount.getAccount(), bankLog.getAmount());
							}
						}
					} else {
						if (AccountType.InBank.getTypeId() == bizAccount.getType()) {
							// 入款卡则上报未匹配流水金额锁定
							BizHandicap handicap = handicapService.findFromCacheById(bizAccount.getHandicapId());
							if (Objects.nonNull(handicap) && Objects.nonNull(handicap.getCode())
									&& Objects.nonNull(bizAccount) && Objects.nonNull(bizAccount.getAccount())
									&& Objects.nonNull(bankLog) && Objects.nonNull(bankLog.getAmount())) {
								// 上报平台此流水金额不能使用于新提单
								incomeRequestService.moneyBeUsed(Integer.valueOf(handicap.getCode()),
										bizAccount.getAccount(), bankLog.getAmount());
								// 新公司入款 流水未匹配 锁定金额
								if (CommonUtils.checkNewInComeEnabled(bizAccount.getHandicapId())) {
									ysfService.lockRandNum(bizAccount.getAccount(), bankLog.getAmount());
								}
							}
						}
						log.info("Matching! FromAccount:{},orders count:{}", bankLog.getFromAccount(), matchingCounts);
						// 无法自动匹配的 都会显示在前端
						noticeEntity.setIncomeAuditWsFrom(IncomeAuditWsEnum.FromInBankLog.ordinal());
						noticeEntity.setAccount(bankLog.getFromAccountNO());
						noticeEntity.setAccountId(bankLog.getFromAccount());
						noticeEntity.setMessage("有新的流水");
						// 广播消息通知前端有新的记录，browser通过websocket事件刷新内容
						try {
							redisService.convertAndSend(RedisTopics.INCOME_REQUEST,
									mapper.writeValueAsString(noticeEntity));
							log.info("redis广播发消息给前端，incomes 查询不到记录");
						} catch (JsonProcessingException e) {
							log.error("", e);
						}
					}
					// 匹配完成以后，处理入款卡占用的临时额度
					if (matched) {
						incomeRequestService.dealOccuTempLimit(bankLog);
					}
				} else {
					// 非小数的，判断这流水是否是下发卡，如果是（大部分第三方下发无法输入小数），去自动匹配
					if (null != bizAccount && (AccountType.isInternal(bizAccount.getType())
							|| bizAccount.getType().equals(AccountType.OutBank.getTypeId()))) {
						// 匹配规则：to_id相同、金额相同、流水时间在一定范围内、入款类型是（101支付宝提现、102微信提现、103第三方提现、113平台过来的流水匹配）
						// 下发数据不用限制时间查询
						Object[] parms = { bankLog.getFromAccount(), bankLog.getTradingTime(), null,
								IncomeRequestStatus.Matching.getStatus(), IncomeRequestStatus.Matched.getStatus(),
								bankLog.getAmount(), 3, bizAccount.getType() };
						incomes = incomeRequestService.findAll(parms);
						log.debug("Matching! FromAccount:{},TradingTime:{},Amount:{},isNull:{}",
								bankLog.getFromAccount(), bankLog.getTradingTime(), bankLog.getAmount().floatValue(),
								(null == incomes || incomes.size() <= 0));
						if (!CollectionUtils.isEmpty(incomes)) {
							matched = true;
							incomeRequest = incomes.get(0);
							bankLog.setTaskId(incomeRequest.getId());
							bankLog.setTaskType(SysBalTrans.TASK_TYPE_INNER);
							bankLog.setOrderNo(incomeRequest.getOrderNo());
							// 如果是出款卡
						}
					} else if ((StringUtils.isNotBlank(bankLog.getToAccount()) && null != bizAccount
							&& AccountType.InBank.getTypeId() == bizAccount.getType()
							&& bankLog.getToAccountOwner().length() <= 3) || bizAccount.getBankType().equals("工商银行")) {
						// 如果入款卡工商存款人名字是：(支付宝（中国）网络技术有限公司) 则截取流水摘要 支付宝前面的的信息当名字
						if (bizAccount.getType() == AccountType.InBank.getTypeId()
								&& bizAccount.getBankType().equals("工商银行")
								&& bankLog.getToAccountOwner().equals("支付宝（中国）网络技术有限公司")) {
							String owner = bankLog.getSummary().substring(0, bankLog.getSummary().indexOf("支付宝"));
							bankLog.setToAccountOwner("".equals(owner) ? bankLog.getToAccountOwner() : owner);
						}
						// 如果是入款卡、且有对方名字、且是非小数的、去查找是否有会员提单。如果有则把小数的单取消，生成整数的单进行匹配
						generateIncomeRequestOrder(bankLog);
					} else {
						// 无法自动匹配的 都会显示在前端
						noticeEntity.setIncomeAuditWsFrom(IncomeAuditWsEnum.FromInBankLog.ordinal());
						noticeEntity.setAccount(bankLog.getFromAccountNO());
						noticeEntity.setAccountId(bankLog.getFromAccount());
						noticeEntity.setMessage("有新的流水");
						// 广播消息通知前端有新的记录，browser通过websocket事件刷新内容
						try {
							redisService.convertAndSend(RedisTopics.INCOME_REQUEST,
									mapper.writeValueAsString(noticeEntity));
							log.info("redis广播发消息给前端，流水金额：int {},float {}", bankLog.getAmount().intValue(),
									bankLog.getAmount().floatValue());
						} catch (JsonProcessingException e) {
							log.error("", e);
						}
					}
				}
				// 支付宝 微信 专用入款 无订单流水无法匹配调用平台回复额度接口 且对方账号不为空
				// 周四版本不包含该功能 暂时屏蔽
				// if (!matched && bizAccount != null && bizAccount.getType() !=
				// null
				// && (bizAccount.getType().equals(AccountType.BindAli)
				// || bizAccount.getType().equals(AccountType.BindWechat))) {
				// Integer code =
				// Integer.valueOf(handicapService.findFromCacheById(bizAccount.getHandicapId()).getCode());
				// log.info("支付宝微信专用入款调用平台恢复信用额度:
				// 收款账号类型:{},对方账号:{},对方姓名:{},金额:{},盘口编码:{},交易时间:{}",
				// AccountType.findByTypeId(bizAccount.getType()).getMsg(),
				// bankLog.getToAccount(),
				// bankLog.getToAccountOwner(), bankLog.getAmount(), code,
				// bankLog.getTradingTime());
				// AutoResetInputDTO inputDTO = new AutoResetInputDTO();
				// inputDTO.setAccount(StringUtils.isNotBlank(bankLog.getToAccount())
				// ?
				// bankLog.getToAccount() : "无");
				// inputDTO.setInTime(bankLog.getTradingTime() != null ?
				// bankLog.getTradingTime().getTime()
				// : bankLog.getCreateTime().getTime());
				// inputDTO.setMoney(bankLog.getAmount());
				// inputDTO.setOpenMan(
				// StringUtils.isNotBlank(bankLog.getToAccountOwner()) ?
				// bankLog.getToAccountOwner() : "无");
				// inputDTO.setOid(code);
				// newPayService.autoReset(inputDTO);
				// }
			}
			// 转出
			else {
				// 如果对方帐号不为空(云农的没有抓取对方账号信息)且是纯数字，则是某些银行的流水屏蔽了帐号信息或拼接的，改用按对方姓名来找
				if (StringUtils.isNotBlank(bankLog.getToAccount()) && CommonUtils.isNumeric(bankLog.getToAccount())) {
					// 查任务表，出款卡、金额、to_account相同
					outwardTask = outwardTaskService.findOutwardTask(bankLog.getFromAccount(),
							Math.abs(bankLog.getAmount().floatValue()), bankLog.getToAccount(), 1, 1, 1);
					log.debug("Matching! FromAccount:{},Amount:{},ToAccount:{},isNull:{}", bankLog.getFromAccount(),
							bankLog.getAmount().floatValue(), bankLog.getToAccount(), null == outwardTask);
				} else {
					// 查任务表，出款卡、金额、to_account_owner相同
					// 如果是信业卡转出对方账号过长 存在抓取不全的问题
					if (bizAccount.getBankType().equals("兴业银行") && bankLog.getToAccountOwner().length() > 4) {
						outwardTask = outwardTaskService.findOutwardTask(bankLog.getFromAccount(),
								Math.abs(bankLog.getAmount().floatValue()), bankLog.getToAccountOwner().substring(0, 3),
								2, 1, 2);
					} else {
						outwardTask = outwardTaskService.findOutwardTask(bankLog.getFromAccount(),
								Math.abs(bankLog.getAmount().floatValue()), bankLog.getToAccountOwner(), 2, 1, 1);
					}
					log.debug("Matching! FromAccount:{},Amount:{},ToAccount:{},isNull:{}", bankLog.getFromAccount(),
							bankLog.getAmount().floatValue(), bankLog.getToAccount(), null == outwardTask);
				}
				if (null != outwardTask) {
					matched = true;
					bankLog.setTaskId(outwardTask.getId());
					bankLog.setTaskType(SysBalTrans.TASK_TYPE_OUTMEMEBER);
					bankLog.setOrderNo(outwardTask.getOrderNo());
				}

				// 若没匹配上，是系统内部转帐，再向系统中转匹配：向入款表查，from帐号与流水from帐号一样--金额相同--时间区间相同
				if (!matched) {
					// 如果是入款银行卡且金额是整数没有匹配上
					// 查找正在出款的单(入款银行卡也进行出款操作。边出款边抓流水，存在流水先到的情况)
					if (AccountType.InBank.getTypeId() == bizAccount.getType()
							&& Math.abs(bankLog.getAmount().intValue()) == Math.abs(bankLog.getAmount().floatValue())) {
						log.info("Matching 入款银行卡出款没有找到已出款状态的单。FromAccount:{},Amount:{}", bankLog.getFromAccount(),
								bankLog.getAmount().floatValue());
						// 如果对方帐号不为空(云农的没有抓取对方账号信息)且是纯数字，则是某些银行的流水屏蔽了帐号信息或拼接的，改用按对方姓名来找
						if (StringUtils.isNotBlank(bankLog.getToAccount())
								&& CommonUtils.isNumeric(bankLog.getToAccount())) {
							// 查任务表，出款卡、金额、to_account相同
							outwardTask = outwardTaskService.findOutwardTask(bankLog.getFromAccount(),
									Math.abs(bankLog.getAmount().floatValue()), bankLog.getToAccount(), 1, 2, 1);
						} else {
							// 查任务表，出款卡、金额、to_account_owner相同
							outwardTask = outwardTaskService.findOutwardTask(bankLog.getFromAccount(),
									Math.abs(bankLog.getAmount().floatValue()), bankLog.getToAccountOwner(), 2, 2, 1);
						}
						if (null != outwardTask) {
							matched = true;
							bankLog.setTaskId(outwardTask.getId());
							bankLog.setTaskType(SysBalTrans.TASK_TYPE_OUTMEMEBER);
							bankLog.setOrderNo(outwardTask.getOrderNo());
						}
					} else {
						// 如果是测试转账的流水则不走匹配
						// 检查是否有激活的任务，激活的任务不走匹配
						String key = bankLog.getFromAccount() + ":"
								+ bankLog.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP).toString();
						Object o = redisService.getStringRedisTemplate().opsForHash()
								.entries(RedisKeys.ACTIVE_ACCOUNT_KEYS).get(key);
						if (bankLog.getAmount().floatValue() < -10.99 || Objects.isNull(o)) {
							/** 入款与流水记录匹配最大时间间隔（小时） */
							int validIntervalTimeHour = Integer.parseInt(MemCacheUtils.getInstance().getSystemProfile()
									.getOrDefault("INCOME_MATCH_HOURS", "2"));
							// 有的流水没有具体的时间 只有当天的日期，无法匹配，这里做转换。
							if (bankLog.getTradingTime().toString().indexOf("00:00:00") > -1) {
								bankLog.setTradingTime(bankLog.getCreateTime());
							}
							// 如果是内部转账 则不限制查询时间（根据转出的账号 去查询数据，跟会员账号关联不上）
							Object[] parms = { bankLog.getFromAccount(), bankLog.getTradingTime(), null,
									IncomeRequestStatus.Matching.getStatus(), IncomeRequestStatus.Matched.getStatus(),
									Math.abs(bankLog.getAmount().floatValue()), 2, bizAccount.getType() };
							List<BizIncomeRequest> incomes = incomeRequestService.findAll(parms);
							log.debug("Matching! FromAccount:{},TradingTime:{},Amount:{},isNull:{}",
									bankLog.getFromAccount(), bankLog.getTradingTime(),
									bankLog.getAmount().floatValue(), (null == incomes || incomes.size() <= 0));
							if (!CollectionUtils.isEmpty(incomes)) {
								incomeRequest = incomes.get(0);
								matched = true;
							}
						}
					}
				}
				// 未匹配任何提单记录，需要发出告警，对于转出的流水一定严管，涉及资金流失，可能是转错，或故意转到其它卡
				if (!matched) {
					// TODO
					log.warn("警告：转出流水未匹配任何提单记录, {}", bankLog);
				}
				if (matched && Objects.equals(bizAccount.getType(), AccountType.BindCustomer.getTypeId())) {
					redisService.increment(RedisKeys.ACCOUNT_CREDIT_RESTORE, String.valueOf(bizAccount.getId()),
							Math.abs(bankLog.getAmount().floatValue()));
				}
			}
			if (matched) {
				// 设置通知类型为已匹配，广播前端
				noticeEntity.setIncomeAuditWsFrom(IncomeAuditWsEnum.FromMatched.ordinal());
				// 若是平台入款，向平台反馈--> 确认
				if (null != incomeRequest && IncomeRequestType.isPlatform(incomeRequest.getType())) {
					// 向平台确认操作成功后，会插入系统交易流水
					log.info("banklog match incomeRequest orderNo:{}", incomeRequest.getOrderNo());
					incomeRequestService.ack(bankLog, incomeRequest, IncomeRequestStatus.Matched.getMsg(),
							IncomeRequestStatus.Matched.getMsg(), null);
					incomeRequestService.match4FlowsByAliInBankAccount(incomeRequestService.get(incomeRequest.getId()),
							bankLogService.get(bankLog.getId()));
				} else {
					// 金额负数，转出流水匹配，outwardTask不为null即给会员的
					if (null != outwardTask) {
						BizTransactionLog o = new BizTransactionLog();
						o.setOrderId(outwardTask.getId());
						o.setOperator(outwardTask.getOperator());
						// o.setToAccount(0);// 会员帐号在系统中不存在，置0
						o.setFromAccount(outwardTask.getAccountId());// 来自哪个帐号
						o.setType(TransactionLogType.OUTWARD.getType());
						o.setAmount(bankLog.getAmount());
						o.setFromBanklogId(bankLog.getId());
						// o.setConfirmor(1);// 系统管理员,用户表第一个记录
						o.setCreateTime(new Date());
						transactionLogService.save(o);
						log.info("Matched! OrderId:{}, Bank log id:{}",
								outwardTask == null ? incomeRequest.getOrderNo() : outwardTask.getId() + "(taskId)",
								bankLog.getId());
					} else {
						// 先查找是否已有相关交易记录，系统内部中转，产生from,to二条流水
						// 任何一个帐号的流水先到，若匹配成功则会插入交易明细，二条流水分别匹配from,to流水
						BizTransactionLog o = transactionLogService.findByReqId(incomeRequest.getId());
						log.debug("系统内部中转匹配  TransactionLogIsNull : {}  amount:{}", (null == o),
								bankLog.getAmount().floatValue());
						if (null == o) {
							o = new BizTransactionLog();
							o.setFromAccount(incomeRequest.getFromId());
							o.setToAccount(incomeRequest.getToId());
							o.setOrderId(incomeRequest.getId());
							o.setType(incomeRequest.getType());
							o.setOperator(incomeRequest.getOperator());
							o.setAmount(incomeRequest.getAmount());
							o.setCreateTime(new Date());
						}
						if (bankLog.getAmount().floatValue() > 0) {
							// 金额正数，收入，更新to帐号流水，from往to转帐，是to的流水,
							// 同时更新to收款帐号当日累计入款
							increment(incomeRequest.getToId(), incomeRequest.getAmount().floatValue());
							o.setToBanklogId(bankLog.getId());
							log.debug("Reset IncomeDailyTotal income_toID : {}", incomeRequest.getToId());
						} else {
							// 金额负数，支出，from流水
							o.setFromBanklogId(bankLog.getId());
						}
						transactionLogService.save(o);
						log.info("Matched!(系统中转) OrderId:{}, Bank log id:{}",
								outwardTask == null ? incomeRequest.getOrderNo() : outwardTask.getId() + "(taskId)",
								bankLog.getId());
						boolean isActiveFlow = null != bankLog && bankLog.getAmount() != null
								&& bankLog.getAmount().compareTo(BigDecimal.ZERO) > 0;
						if (isActiveFlow) {
							// 正的流水 才是第三方下发到下发卡 才会走匹配后删除锁的逻辑
							// 移除待确认的队列
							AccountBaseInfo baseInfo = accountService.getFromCacheById(incomeRequest.getToId());
							if (baseInfo != null && baseInfo.getType() != null
									&& baseInfo.getType().equals(AccountType.OutBank.getTypeId())) {
								log.info("第三方下发到出款卡 系统上报流水自动匹配 fromId :{} , toId :{}", incomeRequest.getFromId(),
										incomeRequest.getToId());
								accountService.removeLockedRecordThirdDrawToOutCard(incomeRequest.getFromId(),
										incomeRequest.getToId());
							}
							// 匹配保存 秒 添加更新时间
							// incomeRequest =
							// incomeRequestService.get(incomeRequest.getId());
							Date updateTime = new Date();
							Long timeConSuming = (updateTime.getTime() - incomeRequest.getCreateTime().getTime())
									/ 1000;
							log.info("完成耗时:更新时间{} 创建时间:{} 耗时:{}", updateTime.getTime(),
									incomeRequest.getCreateTime().getTime(), timeConSuming);
							// incomeRequestService.saveAndFlush(incomeRequest);
							incomeRequestService.updateTimeconsumingAndUpdateTime(incomeRequest.getId(), updateTime,
									timeConSuming);

							accountService.dealMatch(incomeRequest.getToId());

							// 匹配之后删除 未确认的缓存
							accountService.deleteUnfinishedDrawInThirdByMatched(incomeRequest.getToId());
							// 删除 提现时间
							accountService.saveDrawTime(incomeRequest.getToId(), false);
						}

					}
				}
			}
			// PC 测试转账流水匹配成功，激活账户，手机和返利网激活走另外的逻辑
			if (Objects.equals(bizAccount.getStatus(), AccountStatus.Inactivated.getStatus())
					&& (bizAccount.getFlag() == null || bizAccount.getFlag() == 0)) {
				if (bankLog.getAmount().floatValue() >= -10.99 && bankLog.getAmount().floatValue() <= -10.01) {
					String[] activeAcc = getActiveAccTransInfo(bankLog);
					if (Objects.nonNull(activeAcc)) {
						BizAccount account = accountService.getById(bankLog.getFromAccount());
						BizAccount oldAccount = new BizAccount();
						try {
							BeanUtils.copyProperties(oldAccount, account);
							Date date = new Date();
							account.setRemark(
									CommonUtils.genRemark(account.getRemark(), "【" + AccountStatus.Inactivated.getMsg()
											+ "转" + AccountStatus.Enabled.getMsg() + "】" + "测试流水匹配，", date, "系统管理员"));
							account.setStatus(AccountStatus.Enabled.getStatus());
							account.setUpdateTime(date);
							accountService.updateBaseInfo(account);
							accountService.broadCast(account);
							accountExtraService.saveAccountExtraLog(oldAccount, account, "系统管理员");
							systemAccountManager.rpush(new SysBalPush(account.getId(), SysBalPush.CLASSIFY_INIT,
									new ReportInitParam(account.getId(), AppConstants.USER_ID_4_ADMIN, "PC账号激活")));
							log.info("SB{}  系统账目初始化 >> PC账号激活成功 {}", bankLog.getFromAccount(), bankLog.getAmount());
							if (account.getType() == AccountType.InBank.getTypeId()) {
								newPayService.updateStatus_sync(account.getId(), AccountStatus.Enabled.getStatus());
							}
							log.info("Account {} actived", bankLog.getFromAccount());
						} catch (Exception e) {
							log.error("Account active exception, FromAccount:{}", bankLog.getFromAccount(), e);
						}
					}
				}
			}
			if (matched && incomeRequest != null && incomeRequest.getType() != null
					&& incomeRequest.getHandicap() != null && incomeRequest.getOrderNo() != null
					&& incomeRequest.getType() == IncomeRequestType.PlatFormToFundsTransfer.getType()) {
				try {
					BizHandicap handicap = handicapService.findFromCacheById(incomeRequest.getHandicap());
					if (Objects.nonNull(handicap)) {
						ConfirmInputDTO dto = new ConfirmInputDTO();
						dto.setOid(Integer.valueOf(handicap.getCode()));
						dto.setCode(incomeRequest.getOrderNo().substring(1));
						newPayService.confirm(dto);
						log.info("ConfirmIncomeFromCloud => oid:{} code:{}", dto.getOid(), dto.getCode());
					}
				} catch (Exception l) {
					log.error("Confirm Income From Cloud Transfer .", l);
				}
			}
			// 如果没有匹配到单且金额是10.5或者-10.5元自动转入已处理状态。
			if (!matched && (bankLog.getAmount().compareTo(new BigDecimal("10.5")) == 0
					|| bankLog.getAmount().compareTo(new BigDecimal("-10.5")) == 0)) {
				log.info("NoMatched 10.5=> fromAccount:{}", bankLog.getFromAccount());
				bankLogService.updateBankLog(bankLog.getId(), BankLogStatus.Disposed.getStatus());
			}
			// 如果没有匹配到单且是负数，则看是否兼职提现
			if (!matched && bankLog.getAmount().floatValue() < 0) {
				BizAccountRebate rebate = null;
				if (StringUtils.isNotBlank(bankLog.getToAccount()) && CommonUtils.isNumeric(bankLog.getToAccount())) {
					// 查询兼职人员提现表，出款卡、金额、to_account相同
					rebate = accountRebateService.findRebateByBankLog(bankLog.getFromAccount(),
							Math.abs(bankLog.getAmount().floatValue()), bankLog.getToAccount(), 1, 1, 1);
				} else {
					// 查询兼职人员提现表，出款卡、金额、to_holder相同
					rebate = accountRebateService.findRebateByBankLog(bankLog.getFromAccount(),
							Math.abs(bankLog.getAmount().floatValue()), bankLog.getToAccountOwner(), 2, 1, 1);
				}
				if (null != rebate) {
					matched = true;
					bankLog.setTaskId(rebate.getId());
					bankLog.setTaskType(SysBalTrans.TASK_TYPE_OUTREBATE);
					bankLog.setOrderNo(rebate.getTid());
					accountRebateService.updateStatusById(rebate.getId(), OutwardTaskStatus.Matched.getStatus());
					bankLogService.updateStatusRm(bankLog.getId(), BankLogStatus.Matched.getStatus(), "兼职提现流水");
				}
			}
			// 如果是转入，并且金额在10.01到10.99之前，看是否为激活账户的测试转账数据，如果是，将对应账户激活
			if (bankLog.getAmount().floatValue() >= 10.01 && bankLog.getAmount().floatValue() <= 10.99) {
				String[] activeAcc = getActiveAccTransInfo(bankLog);
				if (Objects.nonNull(activeAcc)) {
					Integer accId = Integer.parseInt(activeAcc[0]);
					BizAccount account = accountService.getById(accId);
					AccountBaseInfo base = accountService.getFromCacheById(accId);
					if (Objects.nonNull(account)
							&& Objects.equals(account.getStatus(), AccountStatus.Inactivated.getStatus())
							&& Objects.nonNull(base) && base.checkMobile()) {
						BizAccount oldAccount = new BizAccount();
						bankLogService.updateBankLog(bankLog.getId(), BankLogStatus.Disposed.getStatus());
						try {
							BeanUtils.copyProperties(oldAccount, account);
							Date date = new Date();
							String remark = "";
							boolean flag = false;
							if (!activeAcc[1].equals(activeAcc[3]) && activeAcc[3].indexOf("*") < 0) {
								account.setAccount(activeAcc[3]);
								flag = true;
								remark = remark + "更新账号信息（" + addAccount(activeAcc[1]) + " 为 "
										+ addAccount(activeAcc[3]) + "）";
							}
							if (!activeAcc[2].equals(activeAcc[4])) {
								account.setOwner(activeAcc[4]);
								remark = remark + "更新开户人信息（" + "*" + activeAcc[2].substring(1, activeAcc[2].length())
										+ " 为 " + "*" + activeAcc[4].substring(1, activeAcc[4].length()) + "）";
							}
							List<BizAccount> accList = accountService.findByAccount(activeAcc[3]);
							// 如果正确的卡号存在了则不去激活（存在兼职添加错了卡号，又添加正确的卡号，然后激活错误的卡号）
							if (flag && accList.size() > 0) {
								log.info("激活更新后的卡号已存在不允许激活！ Account {}", activeAcc[3]);
							} else {
								remark = remark + "账号状态更新为停用状态";
								account.setRemark(CommonUtils.genRemark(account.getRemark(), remark, date, "系统管理员"));
								account.setStatus(AccountStatus.Activated.getStatus());
								account.setUpdateTime(date);
								accountService.updateBaseInfo(account);
								accountService.broadCast(account);
								accountExtraService.saveAccountExtraLog(oldAccount, account, "系统管理员");
								if (account.getFlag() != null && account.getFlag() == 2) {
									rebateApiService.auditAcc(true, activeAcc[1], activeAcc[2], activeAcc[3],
											activeAcc[4]);
								}
								updateBanklogToDisposed(accId);
								log.info("Account {} actived", bankLog.getFromAccount());
							}
						} catch (Exception e) {
							log.error("Account active exception, FromAccount:{}", bankLog.getFromAccount(), e);
						}
					}
				}
			}
			// 如果没有匹配且金额>0且为整数 且为兼职银行卡流水 去查找是否为提额流水
			if (!matched && bankLog.getAmount().floatValue() > 0
					&& bankLog.getAmount().intValue() == bankLog.getAmount().floatValue()
					&& Objects.equals(2, bizAccount.getFlag())) {
				if (bankLog.getTradingTime().toString().indexOf("00:00:00") > -1) {
					bankLog.setTradingTime(bankLog.getCreateTime());
				}
				SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				BizIncomeRequest incomeRequestLimit = incomeRequestService.findRebateLimit(bankLog.getAmount(),
						bankLog.getFromAccount(), sd.format((DateUtils.addHours(bankLog.getCreateTime(), -13))),
						sd.format((DateUtils.addHours(bankLog.getCreateTime(), 13))));
				// 查询是否有一笔相同金额的转出流水，如果存在则不匹配，存在兼职提额时转入 转出 再转入的操作，会重复提额
				int bankLogCounts = bankLogService.finCounts(bankLog.getFromAccount(),
						bankLog.getAmount().abs().multiply(new BigDecimal("-1")),
						sd.format((DateUtils.addHours(bankLog.getTradingTime(), -24))),
						sd.format((DateUtils.addHours(bankLog.getTradingTime(), 24))));
				if (null != incomeRequestLimit && bankLogCounts <= 0) {
					log.info("找到提额单进行提额！accountId {}", bankLog.getFromAccount());
					matched = true;
					rebateApiService.ackCreditLimit(bankLog, incomeRequestLimit);
					// 更改为匹配状态、调用提额的接口
					bankLog.setTaskId(incomeRequestLimit.getId());
					bankLog.setTaskType(SysBalTrans.TASK_TYPE_INNER);
					bankLog.setOrderNo(incomeRequestLimit.getOrderNo());
					bankLogService.updateStatusRm(bankLog.getId(), BankLogStatus.Matched.getStatus(), "兼职提额流水");
					incomeRequestService.updateStatusById(incomeRequestLimit.getId(),
							OutwardTaskStatus.Matched.getStatus());
				}
			}

			// 如果没有匹配且金额<0 且为兼职银行卡流水 去查找是否为降额流水
			if (!matched && bankLog.getAmount().floatValue() < 0 && Objects.equals(2, bizAccount.getFlag())) {
				BizAccountRebate rebate = null;
				if (StringUtils.isNotBlank(bankLog.getToAccount()) && CommonUtils.isNumeric(bankLog.getToAccount())) {
					// 查询兼职人员提现表，出款卡、金额、to_account相同
					rebate = accountRebateService.findRebateByBankLog(bankLog.getFromAccount(),
							Math.abs(bankLog.getAmount().floatValue()), bankLog.getToAccount(), 1, 1, 2);
				} else {
					// 查询兼职人员提现表，出款卡、金额、to_holder相同
					rebate = accountRebateService.findRebateByBankLog(bankLog.getFromAccount(),
							Math.abs(bankLog.getAmount().floatValue()), bankLog.getToAccountOwner(), 2, 1, 2);
				}
				BizAccount acc = accountService.getById(bankLog.getFromAccount());
				if (null != rebate && null != acc) {
					matched = true;
					bankLog.setTaskId(rebate.getId());
					bankLog.setTaskType(SysBalTrans.TASK_TYPE_OUTREBATE);
					bankLog.setOrderNo(rebate.getTid());
					accountRebateService.updateStatusById(rebate.getId(), OutwardTaskStatus.Matched.getStatus());
					bankLogService.updateStatusRm(bankLog.getId(), BankLogStatus.Matched.getStatus(), "兼职降额流水");
					BizAccountMore mo = accountMoreSer.findByMobile(acc.getMobile());
					float margin = mo.getMargin().floatValue() - Math.abs(bankLog.getAmount().floatValue());
					if (null != mo) {
						// 向返利网降额请求
						boolean flag = rebateApiService.derate(acc.getAccount(), bankLog.getAmount().floatValue(),
								margin, bankLog.getId(), rebate.getTid());
						if (flag) {
							mo.setMargin(BigDecimal.valueOf(margin));
							accountMoreSer.saveAndFlash(mo);
							// 如果一张卡 不够扣 则需要扣除多张卡
							Integer peakBalance = (int) (acc.getPeakBalance().floatValue()
									- Math.abs(bankLog.getAmount().floatValue()));
							// 如果当前的卡 够扣除
							if (acc.getPeakBalance().floatValue() >= Math.abs(bankLog.getAmount().floatValue())) {
								acc.setPeakBalance(peakBalance);
								accountService.broadCast(acc);
								hostMonitorService.update(acc);
								cabanaService.updAcc(acc.getId());
							} else {
								// 当前卡不够扣
								float reduceAmount = Math.abs(bankLog.getAmount().floatValue()) - acc.getPeakBalance();
								acc.setPeakBalance(0);
								accountService.broadCast(acc);
								hostMonitorService.update(acc);
								cabanaService.updAcc(acc.getId());
								for (String accId : StringUtils.trimToEmpty(mo.getAccounts()).split(",")) {
									if (StringUtils.isBlank(accId) || !StringUtils.isNumeric(accId)
											|| accId.equals(acc.getId().toString()))
										continue;
									BizAccount account = accountService.getById(Integer.valueOf(accId));
									if (Objects.nonNull(acc)) {
										if (reduceAmount <= 0)
											break;
										if (account.getPeakBalance() >= reduceAmount) {
											// 当前卡够扣
											Integer pb = (int) (account.getPeakBalance() - reduceAmount);
											account.setPeakBalance(pb);
											accountService.broadCast(account);
											hostMonitorService.update(account);
											cabanaService.updAcc(account.getId());
										} else {
											// 当前卡不够扣
											reduceAmount = reduceAmount - account.getPeakBalance();
											account.setPeakBalance(0);
											accountService.broadCast(account);
											hostMonitorService.update(account);
											cabanaService.updAcc(account.getId());
										}
									}
								}
							}
						}
					}
				}
			}
			if (!matched) {
				String key = bankLog.getFromAccount() + ":"
						+ bankLog.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP).toString();
				// ActiveAccountTestTransKey 数据格式
				// key frId:amount
				// val
				// ActiveAccountTestTrans:frId:toId:frAccount:toAccount:toOwner:toBankType:amount
				Object o = redisService.getStringRedisTemplate().opsForHash()
						.entries(RedisKeys.ACTIVE_ACCOUNT_TEST_KEYS).get(key);
				if (o == null) {
					o = redisService.getStringRedisTemplate().opsForHash().entries(RedisKeys.ACTIVE_ACCOUNT_KEYS)
							.get(key);
				}
				if (Objects.nonNull(o)) {
					AccountBaseInfo baseInfo = accountService.getFromCacheById(bankLog.getFromAccount());
					bankLog.setStatus(BankLogStatus.Disposed.getStatus());
					// 把对方信息写到备注里面
					String str[] = o.toString().split(":");
					AccountBaseInfo toAccount = accountService.getFromCacheById(Integer.parseInt(str[2].toString()));
					if (null != toAccount) {
						String handicap = handicapService.findFromCacheById(toAccount.getHandicapId()).getName();
						bankLog.setRemark("一键转出、自动切换，流水自动处理,对方类型:" + toAccount.getBankName() + "；盘口:" + handicap);
					} else {
						bankLog.setRemark("一键转出、自动切换，流水自动处理");
					}
					bankLogService.updateStatusRm(bankLog.getId(), bankLog.getStatus(), bankLog.getRemark());
					redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACTIVE_ACCOUNT_TEST_KEYS).delete(key);
					redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACTIVE_ACCOUNT_KEYS).delete(key);
					// 如果是返利网的则初始化信用额度
					if (baseInfo.getFlag() == 2) {
						BizAccountMore more = accountMoreSer.findByMobile(baseInfo.getMobile());
						BigDecimal bankBalanceInt = new BigDecimal(bankLog.getBalance().intValue());
						if (Objects.nonNull(more) && Objects.isNull(baseInfo.getPeakBalance())) {
							log.info("开始初始化信用额度！accountId {}", baseInfo.getId());
							if (Objects.nonNull(more.getMargin())
									&& more.getMargin().compareTo(new BigDecimal(1000)) == 0) {
								BigDecimal init = bankBalanceInt;
								BizAccount target = accountService.getById(baseInfo.getId());
								for (String accId : StringUtils.trimToEmpty(more.getAccounts()).split(",")) {
									if (StringUtils.isBlank(accId) || !StringUtils.isNumeric(accId))
										continue;
									BizAccount acc = accountService.getById(Integer.valueOf(accId));
									if (Objects.nonNull(acc) && !Objects.equals(acc.getId(), target.getId())) {
										init = init.add(Objects.isNull(acc.getPeakBalance()) ? BigDecimal.ZERO
												: new BigDecimal(acc.getPeakBalance()));
									}
								}
								if (rebateApiService.ackCreditLimit(baseInfo.getAccount(), init, null)) {
									log.info("初始化信用额度，返利网成功！accountId {}", baseInfo.getId());
									if (Objects.nonNull(target)) {
										target.setPeakBalance(bankBalanceInt.intValue());
										accountService.updateBaseInfo(target);
										accountService.broadCast(target);
									}
									if (init.compareTo(new BigDecimal(1000)) > 0) {
										more.setMargin(init);
										accountMoreSer.saveAndFlash(more);
									}
									List<SearchFilter> filters = new ArrayList<>();
									filters.add(new SearchFilter("fromAccount", SearchFilter.Operator.EQ,
											baseInfo.getId()));
									filters.add(new SearchFilter("status", SearchFilter.Operator.EQ,
											BankLogStatus.Matching.getStatus()));
									Specification<BizBankLog> specification = DynamicSpecifications
											.build(BizBankLog.class, filters.toArray(new SearchFilter[filters.size()]));
									List<BizBankLog> finds = bankLogService.findAll(specification);
									for (BizBankLog k : finds) {
										bankLogService.updateBankLog(k.getId(), BankLogStatus.Disposed.getStatus());
									}
								}
							} else {
								BigDecimal bal = Objects.isNull(more.getMargin()) ? BigDecimal.ZERO : more.getMargin();
								bal = bal.add(bankBalanceInt);
								if (rebateApiService.ackCreditLimit(baseInfo.getAccount(), bal, null)) {
									log.info("初始化信用额度，返利网成功！accountId {}", baseInfo.getId());
									List<SearchFilter> filters = new ArrayList<>();
									filters.add(new SearchFilter("fromAccount", SearchFilter.Operator.EQ,
											baseInfo.getId()));
									filters.add(new SearchFilter("status", SearchFilter.Operator.EQ,
											BankLogStatus.Matching.getStatus()));
									Specification<BizBankLog> specification = DynamicSpecifications
											.build(BizBankLog.class, filters.toArray(new SearchFilter[filters.size()]));
									List<BizBankLog> finds = bankLogService.findAll(specification);
									for (BizBankLog k : finds) {
										bankLogService.updateBankLog(k.getId(), BankLogStatus.Disposed.getStatus());
									}
									more.setMargin(bal);
									// 如果只有一张卡则把可用额度也同时初始化
									String[] account = more.getAccounts().split(",");
									List<Integer> tmp = new ArrayList<Integer>();
									for (String sr : account) {
										if (sr != null && sr.length() != 0) {
											tmp.add(Integer.parseInt(sr));
										}
									}
									if (tmp.size() == 1)
										more.setLinelimit(bal);
									accountMoreSer.saveAndFlash(more);
									BizAccount acc = accountService.getById(baseInfo.getId());
									if (Objects.nonNull(acc)) {
										acc.setPeakBalance(bankBalanceInt.intValue());
										accountService.updateBaseInfo(acc);
										accountService.broadCast(acc);
									}
								}
							}
						} else {
							log.info("初始化信用额度，数据有误！accountId {}", baseInfo.getId());
						}
					}
				}
			}
		} catch (Exception e) {
			log.error("流水自动匹配订单操作失败:", e);
		}
	}

	/**
	 * 为指定帐号递增每日入款
	 *
	 * @param accountId
	 * @param amount
	 */
	private void increment(int accountId, float amount) {
		// 更新当日入款数
		redisService.increment(RedisKeys.AMOUNT_SUM_BY_DAILY_INCOME, String.valueOf(accountId), amount);
		Long expire = redisService.getFloatRedisTemplate().boundHashOps(RedisKeys.AMOUNT_SUM_BY_DAILY_INCOME)
				.getExpire();
		if (null != expire && expire.longValue() < 0) {
			// 当日清零
			long currentTimeMillis = System.currentTimeMillis();
			long expireTime = CommonUtils.getExpireTime4AmountDaily() - currentTimeMillis;
			redisService.getFloatRedisTemplate().expire(RedisKeys.AMOUNT_SUM_BY_DAILY_INCOME, expireTime,
					TimeUnit.MILLISECONDS);
			log.debug("Reset IncomeDailyTotal expire : {}", expireTime);
		}
	}

	public void stop() {
		isRuning = false;
	}

	private String[] getActiveAccTransInfo(BizBankLog bankLog) {
		String key = bankLog.getFromAccount() + ":"
				+ bankLog.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP).toString();
		// ActiveAccountTestTransKey 数据格式
		// key toId:amount
		// val
		// ActiveAccountTestTrans:frId:toId:frAccount:toAccount:toOwner:toBankType:amount
		Map trans;
		if (bankLog.getAmount().intValue() < 0) {
			trans = redisService.getStringRedisTemplate().opsForHash().entries(RedisKeys.ACTIVE_ACCOUNT_TEST_KEYS);
		} else {
			trans = redisService.getStringRedisTemplate().opsForHash().entries(RedisKeys.ACTIVE_ACCOUNT_KEYS);
		}
		Object o = trans.get(key);
		if (Objects.nonNull(o)) {
			String[] transInfo = o.toString().split(":");
			String frId = transInfo[1];
			AccountBaseInfo base = accountService.getFromCacheById(Integer.parseInt(frId));
			if (Objects.nonNull(base) && base.getStatus() == AccountStatus.Inactivated.getStatus()) {
				String account = base.getAccount();
				String owner = base.getOwner();
				String toAcc = bankLog.getToAccount();
				String toOwner = bankLog.getToAccountOwner();
				// amount小于0表示转出账号，不用校验账号信息
				if (bankLog.getAmount().floatValue() < 0 || toAcc.startsWith(account.substring(0, 4))
						|| toAcc.endsWith(account.substring(account.length() - 4)) || toOwner.equals(owner)) {
					// 删除用于激活的key
					if (bankLog.getAmount().floatValue() < 0) {
						redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACTIVE_ACCOUNT_TEST_KEYS)
								.delete(key);
					} else {
						redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACTIVE_ACCOUNT_KEYS).delete(key);
					}
					return new String[] { frId, account, owner, toAcc, toOwner };
				}
			}
		}
		return null;
	}

	private void updateBanklogToDisposed(Integer accId) {
		List<SearchFilter> filters = new ArrayList<>();
		filters.add(new SearchFilter("fromAccount", SearchFilter.Operator.EQ, accId));
		Specification<BizBankLog> specification = DynamicSpecifications.build(BizBankLog.class,
				filters.toArray(new SearchFilter[filters.size()]));
		List<BizBankLog> finds = bankLogService.findAll(specification);
		for (BizBankLog o : finds) {
			bankLogService.updateBankLog(o.getId(), BankLogStatus.Disposed.getStatus());
		}
	}

	// 冲正流水重新生成任务
	private boolean reuseTask(BizBankLog o) {
		BizAccount account = accountService.getById(o.getFromAccount());
		// 只有出款卡、入款卡、备用卡才出款
		if (null == account || (account.getType() != AccountType.InBank.getTypeId()
				&& account.getType() != AccountType.OutBank.getTypeId()
				&& account.getType() != AccountType.ReserveBank.getTypeId()))
			return false;
		BizOutwardTask outwardTask = null;
		if (StringUtils.isNotBlank(o.getToAccount()) && CommonUtils.isNumeric(o.getToAccount())) {
			// 先查询是否已经存在同样金额的正在出款的单、如果存在则不重新分配（可能人工已经分配出去）
			outwardTask = outwardTaskService.findReuseTask(o.getFromAccount(), Math.abs(o.getAmount().floatValue()),
					o.getToAccount(), 1, OutwardTaskStatus.Undeposit.getStatus());
			if (null != outwardTask)
				return false;
			// 查任务表，出款卡、金额、to_account相同 先处理待排查的单、 如果没有 则查询已出款、然后是已出款的单
			outwardTask = outwardTaskService.findReuseTask(o.getFromAccount(), Math.abs(o.getAmount().floatValue()),
					o.getToAccount(), 1, OutwardTaskStatus.Failure.getStatus());
			if (null == outwardTask)
				outwardTask = outwardTaskService.findReuseTask(o.getFromAccount(), Math.abs(o.getAmount().floatValue()),
						o.getToAccount(), 1, OutwardTaskStatus.Deposited.getStatus());
			if (null == outwardTask)
				outwardTask = outwardTaskService.findReuseTask(o.getFromAccount(), Math.abs(o.getAmount().floatValue()),
						o.getToAccount(), 1, OutwardTaskStatus.Matched.getStatus());
			log.info("reuseTask! FromAccount:{},Amount:{},ToAccount:{},isNull:{}", o.getFromAccount(),
					o.getAmount().floatValue(), o.getToAccount(), null == outwardTask);
		} else {
			// 先查询是否已经存在同样金额的正在出款的单、如果存在则不重新分配（可能人工已经分配出去）
			outwardTask = outwardTaskService.findReuseTask(o.getFromAccount(), Math.abs(o.getAmount().floatValue()),
					o.getToAccountOwner(), 2, OutwardTaskStatus.Undeposit.getStatus());
			if (null != outwardTask)
				return false;
			// 查任务表，出款卡、金额、to_account_owner相同
			outwardTask = outwardTaskService.findReuseTask(o.getFromAccount(), Math.abs(o.getAmount().floatValue()),
					o.getToAccountOwner(), 2, OutwardTaskStatus.Failure.getStatus());
			if (null == outwardTask)
				outwardTask = outwardTaskService.findReuseTask(o.getFromAccount(), Math.abs(o.getAmount().floatValue()),
						o.getToAccountOwner(), 2, OutwardTaskStatus.Deposited.getStatus());
			if (null == outwardTask)
				outwardTask = outwardTaskService.findReuseTask(o.getFromAccount(), Math.abs(o.getAmount().floatValue()),
						o.getToAccountOwner(), 2, OutwardTaskStatus.Matched.getStatus());
			log.info("reuseTask! FromAccount:{},Amount:{},ToAccount:{},isNull:{}", o.getFromAccount(),
					o.getAmount().floatValue(), o.getToAccount(), null == outwardTask);
		}
		if (null != outwardTask) {
			try {
				// 先把单修改为待排查的状态
				outwardTaskService.updateStatusById(outwardTask.getId(), OutwardTaskStatus.Failure.getStatus());
				// 重新生成任务
				outwardTaskAllocateService.alterStatusToInvalid(outwardTask.getId(), null, "冲正流水从新生成任务", null,
						"manual");
				log.info("reuseTask Success!FromAccount:{},Amount:{},ToAccount:{}", o.getFromAccount(),
						o.getAmount().floatValue(), o.getToAccount());
			} catch (Exception e) {
				log.error("重新生成新任务失败：taskId：{}，operator：{},e:{}", outwardTask.getId(), "系统", e.getLocalizedMessage());
				return false;
			}
		} else {
			return false;
		}
		return true;
	}

	private void generateIncomeRequestOrder(BizBankLog bankLog) {
		try {
			log.info("generateIncomeRequestOrder 入款整数补提单FromAccount:{},Amount:{}", bankLog.getFromAccount(),
					bankLog.getAmount().floatValue());
			// 查找规则：入款帐号相同--》对方名字--》入款金额相等--》入款时间在设置范围-->状态是未匹配
			DateTimeFormatter sd = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			if (bankLog.getTradingTime().toString().indexOf("00:00:00") > -1) {
				bankLog.setTradingTime(bankLog.getCreateTime());
			}
			if (StringUtils.isBlank(bankLog.getToAccountOwner())) {
				log.info("auto generate order for integer banklog amount toAccountOwner is empty");
				return;
			}
			BizIncomeRequest incom = incomeRequestService.findIncome(bankLog.getFromAccount(),
					bankLog.getToAccountOwner(), bankLog.getAmount(),
					sd.format(LocalDateTime.ofInstant((DateUtils.addHours(bankLog.getTradingTime(), -2)).toInstant(),
							ZoneId.systemDefault())),
					sd.format(LocalDateTime.ofInstant((DateUtils.addHours(bankLog.getTradingTime(), 2)).toInstant(),
							ZoneId.systemDefault())),
					IncomeRequestStatus.Matching.getStatus());
			// 如果找到了则取消掉
			if (null != incom) {
				ThreadLocal<Boolean> flag = new ThreadLocal<>();
				boolean flag1 = incomeRequestService.cancelAndCallFlatform(IncomeRequestStatus.Canceled.getStatus(),
						incom.getId(), "系统取消", incom.getOrderNo(), incom.getHandicap(), bankLog.getFromAccount(),
						incom.getMemberCode(), null);
				flag.set(flag1);
				if (flag.get()) {
					incomeRequestService.generateOrderForIntegerBankAmount(bankLog, incom);
				} else {
					log.info("autoCancel 自动取消失败！单号：orderNo {}", incom.getOrderNo());
				}
			}
		} catch (Exception e) {
			log.error("auto cancel and generate order fail", e);
		}

	}

	private static String addAccount(String account) {
		if (account.length() >= 8) {
			return account.toString().substring(0, 4) + "**"
					+ account.toString().substring(account.toString().length() - 4);
		} else if (account.toString().length() >= 4) {
			return account.toString().substring(0, 2) + "**"
					+ account.toString().substring(account.toString().length() - 2);
		} else if (account.toString().length() >= 2) {
			return account.toString().substring(0, 1) + "**"
					+ account.toString().substring(account.toString().length() - 1);
		} else {
			return "";
		}
	}
}
