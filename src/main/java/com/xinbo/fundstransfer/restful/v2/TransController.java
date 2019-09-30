package com.xinbo.fundstransfer.restful.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.net.socket.AccountEntity;
import com.xinbo.fundstransfer.component.net.socket.ActionEventEnum;
import com.xinbo.fundstransfer.component.net.socket.MessageEntity;
import com.xinbo.fundstransfer.component.net.socket.RunningStatusEnum;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.*;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.enums.InBankSubType;
import com.xinbo.fundstransfer.domain.enums.OutwardTaskStatus;
import com.xinbo.fundstransfer.domain.pojo.*;
import com.xinbo.fundstransfer.report.SystemAccountConfiguration;
import com.xinbo.fundstransfer.report.SystemAccountManager;
import com.xinbo.fundstransfer.sec.FundTransferEncrypter;
import com.xinbo.fundstransfer.service.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Eden
 */
@RestController("transController")
@RequestMapping("/api/v2/trans")
public class TransController extends TokenValidation {
	private static final Logger log = LoggerFactory.getLogger(TransController.class);
	@Autowired
	Environment environment;
	@Autowired
	AccountService accSer;
	@Autowired
	SysUserService userSer;
	@Autowired
	AllocateTransService allocTransSer;
	@Autowired
	AllocateOutwardTaskService allocOTaskSer;
	@Autowired
	OutwardTaskService oTaskSer;
	@Autowired
	OutwardRequestService oReqSer;
	@Autowired
	RedisService redisService;
	@Autowired
	RebateApiService rebateApiService;
	@Autowired
	AccountRebateService accountRebateService;
	@Autowired
	BankLogService bankLogService;
	@Autowired
	AccountMoreService accMoreSer;
	@Autowired
	ProblemService problemService;
	@Autowired
	QuickPayService quickPayService;
	@Autowired
	CabanaService cabanaService;
	@Autowired
	SystemAccountManager systemAccountManager;
	@Autowired
	AccountChangeService accountChangeService;
	@Autowired
	HandicapService handicapService;

	@Value("${funds.transfer.cabanasalt}")
	private String cabanasalt;
	private ObjectMapper mapper = new ObjectMapper();

	private static final Cache<String, String> CACHE_LOG = CacheBuilder.newBuilder().maximumSize(80000)
			.expireAfterWrite(60, TimeUnit.SECONDS).build();

	@RequestMapping("/disconnectEvent")
	public @ResponseBody String disconnectEvent(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.info("断网参数 RequestBody:{}", bodyJson);
			TreeMap<String, String> params = mapper.readValue(bodyJson, TreeMap.class);
			Integer accId = Integer.valueOf(params.get("accountId"));
			String token = params.get("token");
			params.remove("token");
			if (!Objects.equals(md5digest(params, cabanasalt), token)) {
				log.error("断网参数校验失败 accId: {}  ", accId);
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "token错误."));
			}
			GeneralResponseData<Account> response = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "保存断网成功");
			accSer.disconnectEvent(accId);
			return mapper.writeValueAsString(response);
		} catch (Exception e) {
			log.error("保存断网失败:", e);
			return mapper
					.writeValueAsString(new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue()));
		}
	}

	@RequestMapping("/savePauseOrResumeAccountId")
	public @ResponseBody String savePauseOrResumeAccountId(@RequestBody String bodyJson)
			throws JsonProcessingException {
		try {
			log.info("保存暂停参数 RequestBody:{}", bodyJson);
			TreeMap<String, String> params = mapper.readValue(bodyJson, TreeMap.class);
			Integer accId = Integer.valueOf(params.get("accountId"));
			Integer saveCode = Integer.valueOf(params.get("save"));
			String token = params.get("token");
			params.remove("token");
			if (!Objects.equals(md5digest(params, cabanasalt), token)) {
				log.error("保存暂停token校验失败 accId: {},saveCode:{} ", accId, saveCode);
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "token error."));
			}
			GeneralResponseData<Account> response = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "保存暂停成功");
			AccountBaseInfo info = accSer.getFromCacheById(accId);
			log.debug("账号基本信息:{}", info);
			if (null != info.getFlag() && info.getFlag().intValue() == 2 && info.getSubType() != null
					&& info.getSubType().intValue() == InBankSubType.IN_BANK_YSF.getSubType().intValue()) {
				accSer.savePauseOrResumeOrOnlineForMobile(info.getId(), saveCode);
			} else {
				accSer.savePauseOrResumeAccountId(accId, saveCode);
				if (saveCode == 88) {
					// 在线转暂停 则删除在线
					accSer.saveOnlineAccontIds(accId, false);
				}
			}

			return mapper.writeValueAsString(response);
		} catch (Exception e) {
			log.error("保存暂停失败:", e);
			return mapper
					.writeValueAsString(new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue()));
		}
	}

	@RequestMapping("/accGet")
	public @ResponseBody String accGet(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.info("Transfer( accGet ) >> RequestBody:{}", bodyJson);
			TreeMap<String, String> params = mapper.readValue(bodyJson, TreeMap.class);
			Integer accId = Integer.valueOf(params.get("accId"));
			String token = params.get("token");
			params.remove("token");
			if (!Objects.equals(md5digest(params, cabanasalt), token)) {
				log.error("Transfer( accGet ) >> token error. id: {} ", accId);
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "token error."));
			}
			GeneralResponseData<Account> response = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "获取账号成功");
			BizAccount acc = accSer.getById(accId);
			if (Objects.nonNull(acc) && (Objects.equals(acc.getFlag(), 1) || Objects.equals(acc.getFlag(), 2))) {
				response.setData(new Account(acc));
			}
			return mapper.writeValueAsString(response);
		} catch (Exception e) {
			log.error("accGet >> RequestBody:{}", bodyJson, e);
			return mapper
					.writeValueAsString(new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue()));
		}
	}

	@RequestMapping("/accGetByAccAndMob")
	public @ResponseBody String accGetByAccAndMob(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.info("Transfer( accGetByAccAndMob ) >> RequestBody:{}", bodyJson);
			TreeMap<String, String> params = mapper.readValue(bodyJson, TreeMap.class);
			String accNo = StringUtils.trimToNull(params.get("accNo"));
			String mobile = StringUtils.trimToNull(params.get("mobile"));
			if (accNo == null || mobile == null)
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
			String token = params.get("token");
			params = new TreeMap<String, String>(Comparator.naturalOrder()) {
				{
					put("accNo", accNo);
					put("mobile", mobile);
				}
			};
			if (!Objects.equals(md5digest(params, cabanasalt), token)) {
				log.error("Transfer( accGetByAccAndMob ) >> token error. accNo: {}  mobile:{}", accNo, mobile);
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "token error."));
			}
			List<SearchFilter> filterToList = new ArrayList<>();
			if (accNo != null)
				filterToList.add(new SearchFilter("account", SearchFilter.Operator.EQ, accNo));
			if (mobile != null)
				filterToList.add(new SearchFilter("mobile", SearchFilter.Operator.EQ, mobile));
			Specification<BizAccount> specif = DynamicSpecifications.build(BizAccount.class,
					filterToList.toArray(new SearchFilter[filterToList.size()]));
			List<BizAccount> dataList = accSer.getAccountList(specif, null);
			if (CollectionUtils.isEmpty(dataList)) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
			}
			GeneralResponseData<Account> response = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "获取账号成功");
			if (Objects.equals(dataList.get(0).getFlag(), 1) || Objects.equals(dataList.get(0).getFlag(), 2))
				response.setData(new Account(dataList.get(0)));
			return mapper.writeValueAsString(response);
		} catch (Exception e) {
			log.error("accGetByAccAndMob >> RequestBody:{}", bodyJson, e);
			return mapper
					.writeValueAsString(new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue()));
		}
	}

	@RequestMapping("/get")
	public String in(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.trace("Transfer( mobileGet ) >> RequestBody:{}", bodyJson);
			AccountEntity entity = mapper.readValue(bodyJson, AccountEntity.class);
			if (Objects.isNull(entity) || Objects.isNull(entity.getId())) {
				log.error("Transfer( mobileGet ) >> Id is empty. RequestBody:{}", bodyJson);
				return mapper.writeValueAsString(
						new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "Id is empty."));
			}
			String object = CACHE_LOG.getIfPresent("TASKGET" + entity.getId());
			if (object == null) {
				CACHE_LOG.put("TASKGET" + entity.getId(), "TASKGET");
				log.info("Transfer( mobileGet ) TaskGet{} >> RequestBody:{}", entity.getId(), bodyJson);
			}
			TreeMap<String, String> params = new TreeMap<>(Comparator.naturalOrder());
			params.put("id", Objects.nonNull(entity.getId()) ? entity.getId().toString() : null);
			params.put("bankBalance",
					Objects.nonNull(entity.getBankBalance()) ? entity.getBankBalance().toString() : null);
			if (!Objects.equals(md5digest(params, cabanasalt), entity.getToken())) {
				log.error("Transfer( mobileGet ) >> token error. id: {}  bankBal : {} ", entity.getId(),
						entity.getBankBalance());
				return mapper.writeValueAsString(
						new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "token error."));
			}
			AccountBaseInfo base = accSer.getFromCacheById(entity.getId());
			if (Objects.isNull(base)) {
				log.error("Transfer( mobileGet ) >> account doesn't exist. id: {}  bankBal : {} ", entity.getId(),
						entity.getBankBalance());
				return mapper.writeValueAsString(
						new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "account doesn't exist."));
			}
			TransferEntity data;
			boolean checkOTask = Objects.equals(AccountType.OutBank.getTypeId(), base.getType());
			String desc = checkOTask ? "OTask" : "Trans";
			if (base.getStatus() == AccountStatus.Inactivated.getStatus()) {
				data = allocTransSer.activeAccByTest(entity.getId(), false);
			} else {
				if (checkOTask) {
					data = allocOTaskSer.applyTask4Mobile(entity.getId(), entity.getBankBalance());
				} else {
					boolean isEnableInBankHandicap = CommonUtils.checkEnableInBankHandicap(base.getHandicapId());
					boolean newVersion = CommonUtils.checkDistHandicapNewVersion(base.getHandicapId());
					if (isEnableInBankHandicap) {
						if (newVersion) {
							data = allocOTaskSer.applyTask4MobileNew(entity.getId(), entity.getBankBalance());
						} else {
							data = allocOTaskSer.applyTask4Mobile(entity.getId(), entity.getBankBalance());
						}
						if (Objects.nonNull(data)) {
							desc = "OTask";
						} else {
							if (newVersion) {
								data = allocTransSer.applyByFromNew(entity.getId(), entity.getBankBalance());
							} else {
								data = allocTransSer.applyByFrom(entity.getId(), entity.getBankBalance());
							}
						}
					} else {
						if (newVersion) {
							data = allocTransSer.applyByFromNew(entity.getId(), entity.getBankBalance());
						} else {
							data = allocTransSer.applyByFrom(entity.getId(), entity.getBankBalance());
						}
					}
				}
			}
			if (Objects.nonNull(data))
				log.info("Transfer( mobileGet ) >> ( {} ) Res frId: {} toAcc: {} realBal: {} amt:{}", desc,
						data.getFromAccountId(), data.getAccount(), entity.getBankBalance(), data.getAmount());
			ResponseData<TransferEntity> responseData = new ResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "success");
			responseData.setData(data);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			log.error("Transfer( mobileGet ) >> error.", e);
			return mapper.writeValueAsString(
					new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/ack")
	public String ack(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.info("Transfer( mobileAck ) >> RequestBody:{}", bodyJson);
			TransferEntity entity = mapper.readValue(bodyJson, TransferEntity.class);
			if (Objects.isNull(entity) || Objects.isNull(entity.getFromAccountId())) {
				log.error("Transfer( mobileAck ) >> frId is empty. RequestBody:{}", bodyJson);
				return mapper.writeValueAsString(
						new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "frId is empty."));
			}
			TreeMap<String, String> params = new TreeMap<String, String>(Comparator.naturalOrder()) {
				{
					if (Objects.nonNull(entity.getFromAccountId()))
						put("fromAccountId", entity.getFromAccountId().toString());
					if (Objects.nonNull(entity.getToAccountId()))
						put("toAccountId", entity.getToAccountId().toString());
					if (StringUtils.isNotBlank(entity.getAccount()))
						put("account", StringUtils.trimToEmpty(entity.getAccount()));
					if (StringUtils.isNotBlank(entity.getOwner()))
						put("owner", StringUtils.trimToEmpty(entity.getOwner()));
					if (StringUtils.isNotBlank(entity.getBankType()))
						put("bankType", StringUtils.trimToEmpty(entity.getBankType()));
					if (StringUtils.isNotBlank(entity.getBankAddr()))
						put("bankAddr", StringUtils.trimToEmpty(entity.getBankAddr()));
					if (Objects.nonNull(entity.getAmount()))
						put("amount", entity.getAmount().toString());
					if (Objects.nonNull(entity.getTaskId()))
						put("taskId", entity.getTaskId().toString());
					if (Objects.nonNull(entity.getResult()))
						put("result", entity.getResult().toString());
					if (StringUtils.isNotBlank(entity.getScreenshot()))
						put("screenshot", StringUtils.trimToEmpty(entity.getScreenshot()));
				}
			};
			if (!Objects.equals(md5digest(params, cabanasalt), entity.getToken())) {
				log.error("Transfer( mobileAck ) >> token error. fromId: {} toAccount: {} amt : {} ",
						entity.getFromAccountId(), entity.getAccount(), entity.getAmount());
				return mapper.writeValueAsString(
						new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "token error."));
			}
			AccountBaseInfo base = accSer.getFromCacheById(entity.getFromAccountId());
			if (Objects.isNull(base)) {
				log.error("Transfer( mobileAck ) >> account doesn't exist. fromId: {} toAccount: {} amt : {} ",
						entity.getFromAccountId(), entity.getAccount(), entity.getAmount());
				return mapper.writeValueAsString(
						new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "account doesn't exist."));
			}
			boolean checkOTask = Objects.equals(AccountType.OutBank.getTypeId(), base.getType());
			if (checkOTask)
				allocOTaskSer.ack4Robot(entity);
			else
				allocTransSer.ackByRobot(entity);
			return mapper.writeValueAsString(
					new ResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "success"));
		} catch (Exception e) {
			log.error("Transfer( mobileAck ) >> error.", e);
			return mapper.writeValueAsString(
					new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getLocalizedMessage()));
		}
	}

	/**
	 * 出款任务转主管
	 */
	@RequestMapping("/turn")
	public String turn(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.info("Transfer( turn ) >> RequestBody:{}", bodyJson);
			TransferEntity entity = mapper.readValue(bodyJson, TransferEntity.class);
			if (Objects.isNull(entity) || Objects.isNull(entity.getFromAccountId())) {
				log.error("Transfer( turn ) >> frId is empty. RequestBody:{}", bodyJson);
				return mapper.writeValueAsString(
						new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "frId is empty."));
			}
			if (Objects.equals(entity.getResult(), 3) || Objects.isNull(entity.getResult())) {
				redisService.increment(RedisKeys.COUNT_FAILURE_TRANS, String.valueOf(entity.getFromAccountId()), 1);
			} else if (Objects.equals(entity.getResult(), 1)) {
				redisService.getFloatRedisTemplate().opsForHash().delete(RedisKeys.COUNT_FAILURE_TRANS,
						String.valueOf(entity.getFromAccountId()));
			}
			systemAccountManager.rpush(new SysBalPush(entity.getFromAccountId(), SysBalPush.CLASSIFY_TRANSFER, entity));
			TreeMap<String, String> params = new TreeMap<String, String>(Comparator.naturalOrder()) {
				{
					if (Objects.nonNull(entity.getToAccountId()))
						put("toAccountId", entity.getToAccountId().toString());
					if (Objects.nonNull(entity.getFromAccountId()))
						put("fromAccountId", entity.getFromAccountId().toString());
					if (StringUtils.isNotBlank(entity.getAccount()))
						put("account", StringUtils.trimToEmpty(entity.getAccount()));
					if (StringUtils.isNotBlank(entity.getOwner()))
						put("owner", StringUtils.trimToEmpty(entity.getOwner()));
					if (StringUtils.isNotBlank(entity.getBankType()))
						put("bankType", StringUtils.trimToEmpty(entity.getBankType()));
					if (StringUtils.isNotBlank(entity.getBankAddr()))
						put("bankAddr", StringUtils.trimToEmpty(entity.getBankAddr()));
					if (Objects.nonNull(entity.getAmount()))
						put("amount", entity.getAmount().toString());
					if (Objects.nonNull(entity.getTaskId()))
						put("taskId", entity.getTaskId().toString());
					if (Objects.nonNull(entity.getResult()))
						put("result", entity.getResult().toString());
					if (StringUtils.isNotBlank(entity.getScreenshot()))
						put("screenshot", StringUtils.trimToEmpty(entity.getScreenshot()));
					if (StringUtils.isNotBlank(entity.getRemark()))
						put("remark", StringUtils.trimToEmpty(entity.getRemark()));
					if (Objects.nonNull(entity.getBalance()))
						put("balance",
								new BigDecimal(entity.getBalance()).setScale(2, BigDecimal.ROUND_HALF_UP).toString());
				}
			};
			if (!Objects.equals(md5digest(params, cabanasalt), entity.getToken())) {
				log.error("Transfer( turn ) >> token error. fromId: {} toAccount: {} amt : {} ",
						entity.getFromAccountId(), entity.getAccount(), entity.getAmount());
				return mapper.writeValueAsString(
						new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "token error."));
			}
			// 实体上报需要处理余额
			if (entity.getFromAccountId() != null && entity.getBalance() != null && entity.getBalance() > 0) {
				allocTransSer.applyRelBal(entity.getFromAccountId(), new BigDecimal(entity.getBalance()));
			}
			AccountBaseInfo base = accSer.getFromCacheById(entity.getFromAccountId());
			if (Objects.isNull(base)) {
				log.error("Transfer( turn ) >> account doesn't exist. id: {} acc: {} bankBal : {} ",
						entity.getFromAccountId(), entity.getAccount(), entity.getAmount());
				return mapper.writeValueAsString(
						new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "account doesn't exist."));
			}
			BizOutwardTask task = null;
			if (Objects.nonNull(entity.getTaskId())) {
				// 防止入款卡转主管后，对应的translock中的数据没有删除，影响后续出款、下发任务，这里进行解锁
				allocTransSer.llockUpdStatus(entity.getFromAccountId(), entity.getTaskId().intValue(),
						Objects.equals(entity.getResult(), 3) ? TransLock.STATUS_DEL : TransLock.STATUS_ACK);
				task = oTaskSer.findById(entity.getTaskId());
				if (Objects.isNull(task)) {
					BizAccountRebate accountRebate = rebateApiService.findById(entity.getTaskId());
					if (Objects.isNull(accountRebate)) {
						return mapper.writeValueAsString(new SimpleResponseData(
								GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "success"));
					} else {
						if (!Objects.equals(entity.getFromAccountId(), accountRebate.getAccountId())) {
							log.error(
									"Transfer( turn ) >> task has been allocated to another acc, ori acc: {} cur acc: {} ",
									entity.getFromAccountId(), accountRebate.getAccountId());
							return mapper.writeValueAsString(new SimpleResponseData(
									GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "success"));
						}
						if (entity.getResult() != null && entity.getResult() == 1) {
							rebateApiService.confirmByRobot(entity);
						} else {
							accountRebateService.unknownByRobot(entity);
						}
						return mapper.writeValueAsString(new SimpleResponseData(
								GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "success"));
					}
				} else {
					allocOTaskSer.sreenshot(task, entity.getScreenshot());
					if (!Objects.equals(entity.getFromAccountId(), task.getAccountId())) {
						log.error(
								"Transfer( turn ) >> task has been allocated to another acc, ori acc: {} cur acc: {} ",
								entity.getFromAccountId(), task.getAccountId());
						return mapper.writeValueAsString(new SimpleResponseData(
								GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "success"));
					}
				}
				if (Objects.equals(task.getStatus(), OutwardTaskStatus.Matched.getStatus())) {
					BizOutwardRequest req = oReqSer.get(task.getOutwardRequestId());
					if (Objects.isNull(req)) {
						return mapper.writeValueAsString(new SimpleResponseData(
								GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "success"));
					}
					allocOTaskSer.noticePlatIfFinished(AppConstants.USER_ID_4_ADMIN, req);
					return mapper.writeValueAsString(
							new SimpleResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "success"));
				}
			} else if (base.getStatus() == AccountStatus.Inactivated.getStatus() || (Objects.isNull(entity.getTaskId())
					&& Objects.nonNull(entity.getRemark()) && entity.getRemark().contains("一键转出"))) {
				allocTransSer.ackTrans(entity);
				return mapper.writeValueAsString(
						new SimpleResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "success"));
			} else if (Objects.nonNull(entity.getToAccountId())) {
				allocTransSer.ackByRobot(entity);
				return mapper.writeValueAsString(
						new SimpleResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "success"));
			}
			SysUser operator = userSer.findFromCacheById(AppConstants.USER_ID_4_ADMIN);
			if (Objects.isNull(entity.getResult()) || Objects.equals(entity.getResult(), 3)) {// CABANA
				if (Objects.nonNull(entity.getTaskId()))
					allocOTaskSer.remark4Mgr(entity.getTaskId(), false, false, operator, null,
							StringUtils.trimToEmpty(entity.getRemark()) + "机器转出机器出款" + entity.getResult());
				else
					allocOTaskSer.remark4Mgr(entity.getFromAccountId(), false, false, operator, "机器转出机器出款");
			} else if (entity.getResult() == 1) {// 工具上报出款成功
				allocOTaskSer.ack4Robot(entity);
			} else {// 工具上报出款未知
				String remark = StringUtils.isBlank(entity.getRemark()) ? "机器转主管" : entity.getRemark();
				remark = remark + "-P" + entity.getResult();
				if (entity.getTaskId() != null) {
					remark = StringUtils.trimToEmpty(entity.getRemark()) + "-P" + entity.getResult();
					String screenshot = StringUtils.trimToNull(entity.getScreenshot());
					BizHandicap handicap = Objects.isNull(task) ? null
							: handicapService.findFromCacheByCode(task.getHandicap());
					if (SystemAccountConfiguration
							.checkHandicapByNeedOpenService4NeedAutoSurvey4UnknownTask(handicap)) {
						allocOTaskSer.alterStatusToUnknown(entity.getTaskId(), null, remark, screenshot);
					} else {
						allocOTaskSer.alterStatusToMgr(entity.getTaskId(), operator, remark, screenshot);
					}
				} else {
					allocOTaskSer.alterStatusToMgr(entity.getFromAccountId(), remark);
				}
			}
			allocOTaskSer.recordLog(entity.getFromAccountId(), entity.getTaskId());
			return mapper.writeValueAsString(
					new SimpleResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "success"));
		} catch (Exception e) {
			log.error("Transfer(outTurn) error.", e);
			return mapper.writeValueAsString(new SimpleResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"Transfer(outTurn) error." + e.getLocalizedMessage()));
		}
	}

	/**
	 * 出款卡请求下发，出款卡没钱了
	 */
	@RequestMapping("/pay")
	public String pay(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.info("Transfer( pay ) >> RequestBody:{}", bodyJson);
			TreeMap<String, String> params = mapper.readValue(bodyJson, TreeMap.class);
			String id = params.get("id");
			String bankBalance = params.get("bankBalance");
			String rptTm = params.get("rptTm");
			String logTm = params.get("logTm");
			String error = params.get("error");
			String inoutModel = params.get("inout");
			if (StringUtils.isBlank(id) || StringUtils.isBlank(bankBalance) || StringUtils.isBlank(rptTm)) {
				log.error("Transfer( pay ) >> params are empty. RequestBody:{}", bodyJson);
				return mapper.writeValueAsString(
						new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "Id is empty."));
			}
			String token = params.get("token");
			params.remove("token");
			if (!Objects.equals(md5digest(params, cabanasalt), token)) {
				log.error("Transfer( pay ) >> token error. id: {}  bankBal : {} ", id, bankBalance);
				return mapper.writeValueAsString(
						new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "token error."));
			}
			if (StringUtils.isNotBlank(error)) {
				problemService.reportErrorMsg(id, error);
			}
			// if (StringUtils.isBlank(error)) {
			// redisService.convertAndSend(RedisTopics.ALLOC_OUT_TASK_SUSPEND,
			// id + ":0:1:0");
			// } else {
			// redisService.convertAndSend(RedisTopics.ALLOC_OUT_TASK_SUSPEND,
			// id + ":1:1:" + error);
			// }
			if (StringUtils.isNotBlank(bankBalance)) {
				systemAccountManager.rpush(new SysBalPush(Integer.parseInt(id), SysBalPush.CLASSIFY_BANK_BAL,
						new BigDecimal(bankBalance)));
			}
			redisService.convertAndSend(RedisTopics.ACCOUNT_CHANGE_BROADCAST, bodyJson);
			AccountBaseInfo base = accSer.getFromCacheById(Integer.valueOf(id));
			if (Objects.nonNull(base) && StringUtils.isNotBlank(logTm) && StringUtils.isNumeric(logTm)) {
				MessageEntity<List<AccountEntity>> entity = new MessageEntity<>();
				entity.setAction(ActionEventEnum.NORMALMODE.ordinal());
				entity.setIp("无IP");
				List<AccountEntity> list = new ArrayList<>();
				AccountEntity d = new AccountEntity();
				// {"id":35116,"bank":"中国银行","alias":"111090","runningStatus":1,"runningMode":18,"lastTime":1555059222925,"ip":"182.18.10.204"}
				d.setId(Integer.valueOf(id));
				d.setBank(StringUtils.trimToEmpty(base.getBankType()));
				d.setAlias(base.getAlias());
				d.setRunningStatus(RunningStatusEnum.NORMAL.ordinal());
				d.setLastTime(Long.parseLong(logTm));
				list.add(d);
				entity.setData(list);
				redisService.convertAndSend(RedisTopics.TOOLS_STATUS_REPORT, mapper.writeValueAsString(entity));
			}

			if (null != base.getFlag() && base.getFlag().intValue() == 2 && null != base.getSubType()
					&& base.getSubType().intValue() == InBankSubType.IN_BANK_YSF.getSubType().intValue()) {
				// 返利网手机上报流水 如果是暂停状态则不更新
				accSer.savePauseOrResumeOrOnlineForMobile(Integer.valueOf(id), 22);
			} else {
				// 保存在线账号id到缓存
				accSer.saveOnlineAccontIds(Integer.valueOf(id), true);
				// 删除 暂停状态
				accSer.savePauseOrResumeAccountId(Integer.valueOf(id), 22);
			}
			if (StringUtils.isNumeric(logTm)) {
				// 流水上报时间
				redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_LOG_TM).put(id, logTm);
			}
			allocTransSer.applyRelBal(Integer.valueOf(id), new BigDecimal(bankBalance), Long.valueOf(rptTm), true);
			allocTransSer.inOutModelCheck(id, inoutModel);
			return mapper.writeValueAsString(
					new SimpleResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "success"));
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Transfer( pay ) >> error.", e);
			return mapper
					.writeValueAsString(new SimpleResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue()));
		}
	}

	@RequestMapping("/logs")
	public String logs(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.info("Transfer( logs ) >> RequestBody:{}", bodyJson);
			TreeMap<String, String> params = mapper.readValue(bodyJson, TreeMap.class);
			String logs = params.get("logs");
			String token = params.get("token");
			params.remove("token");
			if (!Objects.equals(md5digest(params, cabanasalt), token)) {
				log.error("Transfer( logs ) >> token error. bodyJson : {} ", bodyJson);
				return mapper.writeValueAsString(
						new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "token error."));
			}
			// boolean add =
			// MemCacheUtils.getInstance().getBanklogs().add(logs);
			redisService.rightPush(RedisTopics.BANK_STATEMENT, logs);
			accountChangeService.ackReConciliate(logs);
			log.debug("流水：{},添加到并发队列中成功:{}", logs, true);
		} catch (Exception e) {
			return mapper.writeValueAsString(new SimpleResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"Failure, " + e.getMessage()));
		}
		return mapper.writeValueAsString(
				new SimpleResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "Success."));
	}

	@RequestMapping("/modelAll")
	public String modelAll() throws JsonProcessingException {
		try {
			GeneralResponseData<Map<Object, Object>> response = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			response.setData(accSer.getModel());
			return mapper.writeValueAsString(response);
		} catch (Exception e) {
			return mapper.writeValueAsString(new SimpleResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"Failure, " + e.getMessage()));
		}
	}

	@RequestMapping("/model")
	public String model(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.info("Transfer( model ) >> RequestBody:{}", bodyJson);
			TreeMap<String, String> params = mapper.readValue(bodyJson, TreeMap.class);
			String accId = params.get("accId");
			String token = params.get("token");
			params.remove("token");
			if (!Objects.equals(md5digest(params, cabanasalt), token)) {
				log.error("Transfer( model ) >> token error. bodyJson : {} ", bodyJson);
				return mapper.writeValueAsString(
						new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "token error."));
			}
			GeneralResponseData<String> response = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			response.setData(StringUtils.trimToNull(accSer.getModel(Integer.valueOf(accId))));
			return mapper.writeValueAsString(response);
		} catch (Exception e) {
			return mapper.writeValueAsString(new SimpleResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"Failure, " + e.getMessage()));
		}
	}

	@RequestMapping("/offcon")
	public String offcon(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.info("Transfer( offcon ) >> RequestBody:{}", bodyJson);
			TreeMap<String, String> params = mapper.readValue(bodyJson, TreeMap.class);
			String accId = params.get("accId");
			String token = params.get("token");
			params.remove("token");
			if (!Objects.equals(md5digest(params, cabanasalt), token)) {
				log.error("Transfer( offcon ) >> token error. bodyJson : {} ", bodyJson);
				return mapper.writeValueAsString(
						new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "token error."));
			}
			AccountBaseInfo base = accSer.getFromCacheById(Integer.valueOf(accId));
			if (Objects.nonNull(base)) {
				MessageEntity<List<AccountEntity>> entity = new MessageEntity<>();
				entity.setAction(ActionEventEnum.NORMALMODE.ordinal());
				entity.setIp("无IP");
				List<AccountEntity> list = new ArrayList<>();
				AccountEntity d = new AccountEntity();
				// {"id":35116,"bank":"中国银行","alias":"111090","runningStatus":1,"runningMode":18,"lastTime":1555059222925,"ip":"182.18.10.204"}
				d.setId(base.getId());
				d.setBank(StringUtils.trimToEmpty(base.getBankType()));
				d.setAlias(base.getAlias());
				d.setRunningStatus(RunningStatusEnum.OFFLINE.ordinal());
				d.setLastTime(System.currentTimeMillis());
				list.add(d);
				entity.setData(list);
				redisService.convertAndSend(RedisTopics.TOOLS_STATUS_REPORT, mapper.writeValueAsString(entity));
				if (null != base.getFlag() && base.getFlag().intValue() == 2 && null != base.getSubType()
						&& base.getSubType().intValue() == InBankSubType.IN_BANK_YSF.getSubType().intValue()) {
					accSer.savePauseOrResumeOrOnlineForMobile(d.getId(), 999);
				} else {
					// 删除缓存在线账号id
					accSer.saveOnlineAccontIds(d.getId(), false);
				}
				problemService.deleteDeviceStatus(base.getMobile());
			}
			redisService.convertAndSend(RedisTopics.ALLOC_OUT_TASK_SUSPEND, accId + ":1:1");
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			return mapper.writeValueAsString(new SimpleResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"Failure, " + e.getMessage()));
		}
	}

	@RequestMapping("/laststep")
	public String laststep(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.info("Transfer( laststep ) >> RequestBody:{}", bodyJson);
			TreeMap<String, String> params = mapper.readValue(bodyJson, TreeMap.class);
			String accIdStr = params.get("accId");
			String taskIdStr = params.get("taskId");
			String token = params.get("token");
			params.remove("token");
			if (!Objects.equals(md5digest(params, cabanasalt), token)) {
				log.error("Transfer( laststep ) >> token error. bodyJson : {} ", bodyJson);
				return mapper.writeValueAsString(
						new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "token error."));
			}
			Integer accId = Integer.valueOf(accIdStr);
			Long taskId = Long.valueOf(taskIdStr);
			AccountBaseInfo base = accSer.getFromCacheById(accId);
			if (Objects.nonNull(base)) {
				allocOTaskSer.remark4Custom(taskId, null, String.format("%s 已完成交易最后一步", base.getAlias()));
			}
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			return mapper.writeValueAsString(new SimpleResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"Failure, " + e.getMessage()));
		}
	}

	@RequestMapping("/lgtm")
	public String lgtm(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.info("Transfer( lgtm ) >> RequestBody:{}", bodyJson);
			TreeMap<String, String> params = mapper.readValue(bodyJson, TreeMap.class);
			String accId = params.get("accId");
			String token = params.get("token");
			params.remove("token");
			if (!Objects.equals(md5digest(params, cabanasalt), token)) {
				log.error("Transfer( lgtm ) >> token error. bodyJson : {} ", bodyJson);
				return mapper.writeValueAsString(
						new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "token error."));
			}
			if (StringUtils.isNumeric(accId)) {
				systemAccountManager.rpush(new SysBalPush(Integer.valueOf(accId), SysBalPush.CLASSIFY_BANK_LOGS_TIME,
						System.currentTimeMillis()));
			}
			return mapper.writeValueAsString(
					new SimpleResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "success"));
		} catch (Exception e) {
			log.error("Transfer( lgtm ) error.", e);
			return mapper.writeValueAsString(new SimpleResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"Transfer( lgtm ) error." + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/addByMobile")
	public String addByMobile(@RequestBody String bodyJson) throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		try {
			TreeMap<String, String> map = mapper.readValue(bodyJson, TreeMap.class);
			String account = map.get("card");
			if (StringUtils.isNotEmpty(account)) {
				log.info("不再使用工具新增手机银行卡 account {}", account);
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "禁用手机新增手机银行卡!"));
			}
			if (Objects.nonNull(account)) {
				List<BizAccount> accounts = accSer.findByAccount(account);
				if (Objects.nonNull(accounts) && accounts.size() > 0) {
					BizAccount audit = accounts.get(0);
					if (audit.getFlag() != null && audit.getFlag() != 1) {
						log.info("非手机银行，不能通过手机修改信息 id {} account {} flag {}", audit.getId(), audit.getAccount(),
								audit.getFlag());
						return mapper.writeValueAsString(new GeneralResponseData(
								GeneralResponseData.ResponseStatus.FAIL.getValue(), "非手机银行卡，不能通过手机修改信息!"));
					}
					String mobile = StringUtils.trimToNull(map.get("mobile"));
					if (StringUtils.isNotBlank(mobile) && mobile.length() != 11) {
						log.info("更新账号信息失败，手机号码必须11位 id {} account {} mobile {}", audit.getId(), audit.getAccount(),
								mobile);
						return mapper.writeValueAsString(new GeneralResponseData(
								GeneralResponseData.ResponseStatus.FAIL.getValue(), "更新账号信息失败，手机号码必须11位!"));
					}
					Date date = new Date();
					audit.setUpdateTime(new Date());
					audit.setMobile(mobile);
					if (audit.getType() == null) {
						audit.setType(AccountType.OutBank.getTypeId());
					}
					String sign = StringUtils.trimToNull(map.get("userName"));
					// if (sign != null) {
					// audit.setSign(DDG.getInstance().encryptSign(sign));
					// } else {
					// audit.setSign(DDG.getInstance().encryptSign(mobile));
					// }
					// audit.setHook(map.get("loginPass"));
					// audit.setHub(map.get("tradePass"));
					audit.setRemark(CommonUtils.genRemark(audit.getRemark(), "手机修改账号信息", date, "admin"));
					accSer.create(null, audit);
					log.info("更新账号信息成功，id {} account {} mobile {}", audit.getId(), audit.getAccount(),
							audit.getMobile());
					return mapper.writeValueAsString(new GeneralResponseData(
							GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "更新账号信息成功!"));
				}
			}
			BizAccount o = new BizAccount();
			o.setAccount(account);
			String mobile = StringUtils.trimToNull(map.get("mobile"));
			if (StringUtils.isNotBlank(mobile) && mobile.length() != 11) {
				return mapper.writeValueAsString(new GeneralResponseData(
						GeneralResponseData.ResponseStatus.FAIL.getValue(), "新增账号信息失败，手机号码必须11位!"));
			}
			o.setMobile(mobile);
			o.setBankType(map.get("bankType"));
			o.setOwner(map.get("owner"));
			// String sign = StringUtils.trimToNull(map.get("userName"));
			// if (sign != null) {
			// o.setSign(DDG.getInstance().encryptSign(sign));
			// } else {
			// o.setSign(DDG.getInstance().encryptSign(mobile));
			// }
			// o.setHook(map.get("loginPass"));
			// o.setHub(map.get("tradePass"));
			Date date = new Date();
			o.setCreateTime(date);
			o.setUpdateTime(date);
			o.setFlag(1);
			String maxAlias = accSer.getMaxAlias();
			if (StringUtils.isEmpty(maxAlias) || maxAlias.equals("0")) {
				o.setAlias("100000");
			} else {
				int alias = Integer.parseInt(maxAlias) + 1;
				o.setAlias(Integer.toString(alias).replace("4", "5"));
			}
			o.setType(AccountType.OutBank.getTypeId());
			o.setStatus(AccountStatus.Inactivated.getStatus());
			o.setHandicapId(Integer.parseInt(map.get("number")));
			o.setRemark(CommonUtils.genRemark("", "手机新增账号信息", date, "admin"));
			BizAccount data = accSer.create(null, o);
			accSer.broadCast(data);
			if (Objects.nonNull(data) && Objects.equals(data.getFlag(), 1)) {
				accSer.setModel(data.getId(), 1, 1);
			}
			log.info("新增账号信息完成，账号信息: id {} account {} mobile {}", data.getId(), data.getAccount(), data.getMobile());
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "新增账号信息成功!"));
		} catch (Exception e) {
			log.info("新增账号信息异常 {}", e.getMessage());
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "新增账号信息异常!"));
		}
	}

	@RequestMapping("/reportVersion")
	public String reportVersion(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.info("Transfer( reportVersion ) >> RequestBody:{}", bodyJson);
			TreeMap<String, String> params = mapper.readValue(bodyJson, TreeMap.class);
			String accId = params.get("accId");
			String curVer = params.get("curVer");
			String latestVer = params.get("latestVer");
			String token = params.get("token");
			params.remove("token");
			if (!Objects.equals(md5digest(params, cabanasalt), token)) {
				log.error("Transfer( reportVersion ) >> token error. bodyJson : {} ", bodyJson);
				return mapper.writeValueAsString(
						new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "token error."));
			}
			if (StringUtils.isNumeric(accId))
				accSer.reportVersion(Integer.parseInt(accId), curVer, latestVer);
			return mapper.writeValueAsString(
					new SimpleResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "success"));
		} catch (Exception e) {
			log.error("Transfer( lgtm ) error.", e);
			return mapper.writeValueAsString(new SimpleResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"Transfer( lgtm ) error." + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/appLogin")
	public String login(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			TreeMap<String, String> params = mapper.readValue(bodyJson, TreeMap.class);
			String loginStr = params.get("loginStr");
			String token = params.get("token");
			params.remove("token");
			if (!Objects.equals(md5digest(params, cabanasalt), token)) {
				return mapper.writeValueAsString(
						new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "token error."));
			}
			if (loginStr.length() < 10) {
				return mapper.writeValueAsString(
						new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "loginStr error."));
			}
			loginStr = loginStr.substring(6);
			// 0 username 1 password 2 equipment identity
			String[] login = loginStr.split("#");
			if (login.length < 2) {
				return mapper.writeValueAsString(
						new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "loginStr error."));
			}
			String pass = login.length >= 3 ? login[1].substring(0, login[1].length() - 5)
					: login[1].substring(0, login[1].length() - 6);
			GeneralResponseData<List<Account>> response = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			List<Account> accList = accSer.loginByUserPass(login[0], pass);
			response.setData(accList);
			if (login.length > 2) {
				Account acc = accList.get(0);
				if (Objects.nonNull(acc.getId())) {
					accSer.sendDeviceInfo(acc.getId().toString(), params.get("clientIP"), login[2]);
				}
			}
			return mapper.writeValueAsString(response);
		} catch (Exception e) {
			log.error("Transfer( login ) error.", e);
			return mapper.writeValueAsString(new SimpleResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"Transfer( login ) error." + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/getCurrAndList")
	public String getCurrAndList(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.info("Transfer( getCurrAndList ) >> RequestBody:{}", bodyJson);
			TreeMap<String, String> params = mapper.readValue(bodyJson, TreeMap.class);
			String sign = params.get("sign");
			String token = params.get("token");
			params.remove("token");
			if (!Objects.equals(md5digest(params, cabanasalt), token)) {
				log.error("Transfer( getCurrAndList ) >> token error. bodyJson : {} ", bodyJson);
				return mapper.writeValueAsString(
						new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "token error."));
			}
			GeneralResponseData<List<Account>> response = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			List<Account> accList = accSer.getCurrAndList(sign);
			response.setData(accList);
			Account acc = accList.get(0);
			if (Objects.nonNull(acc.getId())) {
				accSer.sendDeviceInfo(acc.getId().toString(), params.get("clientIP"), params.get("equIdent"));
			}
			return mapper.writeValueAsString(response);
		} catch (Exception e) {
			log.error("Transfer( login ) error.", e);
			return mapper.writeValueAsString(new SimpleResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"Transfer( login ) error." + e.getLocalizedMessage()));
		}
	}

	/**
	 * 账号异常操作
	 */
	@RequestMapping("/publicKey")
	public String publicKey(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.info("Transfer(publicKey) >> RequestBody:{}", bodyJson);
			HashMap<String, String> entity = mapper.readValue(bodyJson, HashMap.class);
			TreeMap<String, String> params = mapper.readValue(bodyJson, TreeMap.class);
			String ip = entity.get("ip");
			String mac = entity.get("mac");
			String token = entity.get("token");
			params.remove("token");
			if (!Objects.equals(md5digest(params, cabanasalt), token)) {
				log.error("Transfer( publicKey ) >> token error. bodyJson : {} ", bodyJson);
				return mapper.writeValueAsString(
						new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "token error."));
			}
			if (StringUtils.isBlank(ip) || Objects.isNull(mac)) {
				return mapper.writeValueAsString(
						new ResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "信息不全"));
			}
			log.info("Transfer(publicKey) >>  ip:{} mac:{}", ip, mac);
			GeneralResponseData<String> response = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			response.setData(new String(
					org.apache.mina.util.Base64.encodeBase64(FundTransferEncrypter.publicKeyCabana.getBytes("UTF-8")),
					"UTF-8"));
			return mapper.writeValueAsString(response);
		} catch (Exception e) {
			log.error("Transfer(publicKey) error. {} ", FundTransferEncrypter.publicKeyPc, e);
			return mapper.writeValueAsString(
					new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getLocalizedMessage()));
		}
	}

	/**
	 * 账号异常操作
	 */
	@RequestMapping("/reportDeviceStatus")
	public String reportDeviceStatus(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			// log.trace("Transfer(reportDeviceStatus) >> RequestBody:{}",
			// bodyJson);
			TreeMap<String, String> params = mapper.readValue(bodyJson, TreeMap.class);
			String token = params.get("token");
			params.remove("token");
			if (!Objects.equals(md5digest(params, cabanasalt), token)) {
				log.error("Transfer( reportDeviceStatus ) >> token error. bodyJson : {} ", bodyJson);
				return mapper.writeValueAsString(
						new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "token error."));
			}
			problemService.reportDeviceStatus(params.get("dsStr"));
			GeneralResponseData response = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			return mapper.writeValueAsString(response);
		} catch (Exception e) {
			log.error("Transfer(reportDeviceStatus) error. {} ", e);
			return mapper.writeValueAsString(
					new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getLocalizedMessage()));
		}
	}

	/**
	 * 设备状态上报
	 */
	@RequestMapping("/ackActiveQuickPay")
	public String ackActiveQuickPay(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.info("Transfer(ackActiveQuickPay) >> RequestBody:{}", bodyJson);
			TreeMap<String, String> params = mapper.readValue(bodyJson, TreeMap.class);
			String token = params.get("token");
			params.remove("token");
			if (!Objects.equals(md5digest(params, cabanasalt), token)) {
				log.error("Transfer( ackActiveQuickPay ) >> token error. bodyJson : {} ", bodyJson);
				return mapper.writeValueAsString(
						new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "token error."));
			}
			String rebateUser = params.get("rebateUser");
			String accListStr = params.get("accListStr");
			try {
				List<Account> accList = mapper.readValue(accListStr, new TypeReference<List<Account>>() {
				});
				quickPayService.bindingAndStatus(rebateUser, accList, null);
			} catch (Exception e) {
				quickPayService.bindingAndStatus(rebateUser, null, accListStr);
			}

			GeneralResponseData response = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			return mapper.writeValueAsString(response);
		} catch (Exception e) {
			log.error("Transfer(ackActiveQuickPay) error. {} ", e);
			return mapper.writeValueAsString(
					new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getLocalizedMessage()));
		}
	}

	/**
	 * 获取最新版本信息
	 */
	@RequestMapping("/getLastVersion")
	public String getLastVersion() throws JsonProcessingException {
		try {
			/**
			 * log.info("Transfer(getLastVersion) >> RequestBody:{}", bodyJson);
			 * TreeMap<String, String> params = mapper.readValue(bodyJson,
			 * TreeMap.class); String token = params.get("token");
			 * params.remove("token"); if (!Objects.equals(md5digest(params,
			 * cabanasalt), token)) { log.error("Transfer( getLastVersion ) >>
			 * token error. bodyJson : {} ", bodyJson); return
			 * mapper.writeValueAsString( new
			 * ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
			 * "token error.")); }
			 **/

			return mapper.writeValueAsString(cabanaService.getLastVersion());
		} catch (Exception e) {
			log.error("Transfer(ackActiveQuickPay) error. {} ", e);
			return mapper.writeValueAsString(
					new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getLocalizedMessage()));
		}
	}

	/**
	 * 禁用云闪付
	 */
	@RequestMapping("/disableQuickPay")
	public String disableQuickPay(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.info("Transfer(disableQuickPay) >> RequestBody:{}", bodyJson);
			TreeMap<String, String> params = mapper.readValue(bodyJson, TreeMap.class);
			String token = params.get("token");
			params.remove("token");
			if (!Objects.equals(md5digest(params, cabanasalt), token)) {
				log.error("Transfer( disableQuickPay ) >> token error. bodyJson : {} ", bodyJson);
				return mapper.writeValueAsString(
						new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "token error."));
			}
			String account = params.get("account");
			accSer.disableQuickPay(account);
			List<BizAccount> accountList = accSer.findByAccount(account);
			if (!CollectionUtils.isEmpty(accountList)) {
				BizAccount account1 = accountList.stream()
						.filter(p -> p.getStatus().equals(AccountStatus.Normal.getStatus()))
						.collect(Collectors.toList()).get(0);
				log.debug("账号信息:{}", account1);
				Integer accountId = account1.getId();
				if (null != account1.getFlag() && account1.getFlag().intValue() == 2 && null != account1.getSubType()
						&& account1.getSubType().intValue() == InBankSubType.IN_BANK_YSF.getSubType().intValue()) {
					accSer.savePauseOrResumeOrOnlineForMobile(accountId, 400);
				} else {
					accSer.saveOnlineAccontIds(accountId, false);
					accSer.savePauseOrResumeAccountId(accountId, 88);
				}
			}
			GeneralResponseData response = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			return mapper.writeValueAsString(response);
		} catch (Exception e) {
			log.error("Transfer(ackActiveQuickPay) error. {} ", e);
			return mapper.writeValueAsString(
					new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getLocalizedMessage()));
		}
	}

	/**
	 * 启用云闪付
	 */
	@RequestMapping("/enableQuickPay")
	public String enableQuickPay(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.debug("Transfer(enableQuickPay) >> RequestBody:{}", bodyJson);
			TreeMap<String, String> params = mapper.readValue(bodyJson, TreeMap.class);
			String token = params.get("token");
			params.remove("token");
			if (!Objects.equals(md5digest(params, cabanasalt), token)) {
				log.error("Transfer( enableQuickPay ) >> token error. bodyJson : {} ", bodyJson);
				return mapper.writeValueAsString(
						new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "token error."));
			}
			String account = params.get("account");
			accSer.enableQuickPay(account);
			Optional<List<BizAccount>> optional = Optional.of(accSer.findByAccount(account).stream()
					.filter(p -> p.getStatus().equals(AccountStatus.Normal.getStatus())).collect(Collectors.toList()));
			if (optional.isPresent() && optional.get().size() > 0) {
				BizAccount bizAccount = optional.get().get(0);
				Integer accountId = bizAccount.getId();
				if (null != bizAccount.getFlag() && bizAccount.getFlag().intValue() == 2
						&& null != bizAccount.getSubType()
						&& bizAccount.getSubType().intValue() == InBankSubType.IN_BANK_YSF.getSubType().intValue()) {
					accSer.savePauseOrResumeOrOnlineForMobile(accountId, 405);
				} else {
					accSer.savePauseOrResumeAccountId(accountId, 22);
					accSer.saveOnlineAccontIds(accountId, true);
				}
			}
			GeneralResponseData response = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			return mapper.writeValueAsString(response);
		} catch (Exception e) {
			log.error("Transfer(enableQuickPay) error. {} ", e);
			return mapper.writeValueAsString(
					new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getLocalizedMessage()));
		}
	}

	/**
	 * 云闪付初始化时间
	 */
	@RequestMapping("/initQuickPayTime")
	public String initQuickPayTime(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.info("Transfer(initQuickPayTime) >> RequestBody:{}", bodyJson);
			TreeMap<String, String> params = mapper.readValue(bodyJson, TreeMap.class);
			String token = params.get("token");
			params.remove("token");
			if (!Objects.equals(md5digest(params, cabanasalt), token)) {
				log.info("Transfer( initQuickPayTime ) >> token error. bodyJson : {} ", bodyJson);
				return mapper.writeValueAsString(
						new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "token error."));
			}
			String accountId = params.get("accountId");
			String initTime = params.get("initTime");
			accSer.initQuickPayTime(accountId, initTime);
			GeneralResponseData response = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			return mapper.writeValueAsString(response);
		} catch (Exception e) {
			log.error("Transfer(initQuickPayTime) error. {} ", e);
			return mapper.writeValueAsString(
					new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getLocalizedMessage()));
		}
	}
}