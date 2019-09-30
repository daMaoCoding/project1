package com.xinbo.fundstransfer.restful;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.component.net.socket.AccountEntity;
import com.xinbo.fundstransfer.component.net.socket.ActionEventEnum;
import com.xinbo.fundstransfer.component.net.socket.MessageEntity;
import com.xinbo.fundstransfer.component.net.socket.RunningStatusEnum;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.ResponseData;
import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.*;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalPush;
import com.xinbo.fundstransfer.domain.pojo.SysBalTrans;
import com.xinbo.fundstransfer.domain.pojo.TransferEntity;
import com.xinbo.fundstransfer.report.SystemAccountManager;
import com.xinbo.fundstransfer.report.acc.ErrorAlarm;
import com.xinbo.fundstransfer.report.acc.ErrorHandler;
import com.xinbo.fundstransfer.report.up.ReportInitParam;
import com.xinbo.fundstransfer.restful.v2.Income2Controller;
import com.xinbo.fundstransfer.restful.v3.pojo.RequestTestIncomeInputDTO;
import com.xinbo.fundstransfer.runtime.MemCacheUtils;
import com.xinbo.fundstransfer.service.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/auto/test")
@Slf4j
public class AutoTestController {
	@Autowired
	private AllocateTransService allocateTransService;
	@Autowired
	private AccountService accountService;
	@Autowired
	private AccountRebateService accountRebateService;
	@Autowired
	private IncomeRequestService incomeRequestService;
	@Autowired
	HandicapService handicapService;
	@Autowired
	LevelService levelService;
	@Autowired
	private RedisService redisService;
	@Autowired
	ThirdRequestService thirdRequestService;
	@Autowired
	AllocateOutwardTaskService oTaskSer;
	@Autowired
	ErrorHandler errorHandler;
	@Autowired
	private QuickPayService quickPayService;
	@Autowired
	private RebateUserService rebateUserService;
	@Autowired
	private CabanaService cabanaService;
	@Autowired
	private AccountMoreService accMoreSer;
	@Autowired
	AllocateTransferService allocateTransferService;
	@Autowired
	SysErrService errSer;
	@Autowired
	SystemAccountManager systemAccountManager;
	@Autowired
	AllocateOutwardTaskService allocOTaskSer;
	private ObjectMapper mapper = new ObjectMapper();

	@RequestMapping("/clearProblem")
	public String clearProblem(@RequestParam(value = "id") Long id) throws JsonProcessingException {
		errSer.clean(id);
		return mapper.writeValueAsString(
				new ResponseData<String>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "success"));
	}

	@RequestMapping("/invstId")
	public String invstId(@RequestParam(value = "id") Integer id) throws JsonProcessingException {
		String v = (String) redisService.getStringRedisTemplate().boundHashOps(RedisKeys.SYS_ACC_RUNNING)
				.get(String.valueOf(id));
		ResponseData ret = new ResponseData<ErrorAlarm>(GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		ret.setData(new ErrorAlarm(v));
		return mapper.writeValueAsString(ret);
	}

	/**
	 * 所有账号动态初始化
	 */
	@RequestMapping("/dynamicInitAll")
	public String dynamicInitAll() throws JsonProcessingException {
		SysUser loginUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(loginUser)) {
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		new Thread(()->{
			List<SearchFilter> filterToList = new ArrayList<>();
			// 需要时时对账的账号分类：入款卡，出款卡，备用卡，下发卡；状态：非冻结，删除状态
			List<Integer> typeInList = new ArrayList<>();
			typeInList.add(AccountType.InBank.getTypeId());
			typeInList.add(AccountType.OutBank.getTypeId());
			typeInList.add(AccountType.ReserveBank.getTypeId());
			typeInList.add(AccountType.BindAli.getTypeId());
			typeInList.add(AccountType.BindWechat.getTypeId());
			typeInList.add(AccountType.ThirdCommon.getTypeId());
			typeInList.add(AccountType.BindCommon.getTypeId());
			filterToList.add(new SearchFilter("type", SearchFilter.Operator.IN, typeInList.toArray()));
			filterToList.add(new SearchFilter("status", SearchFilter.Operator.NOTEQ, AccountStatus.Freeze.getStatus()));
			filterToList.add(new SearchFilter("status", SearchFilter.Operator.NOTEQ, AccountStatus.Delete.getStatus()));
			SearchFilter[] filterToArray = filterToList.toArray(new SearchFilter[filterToList.size()]);
			List<Integer> idList = accountService.findAccountIdList(filterToArray);
			for(Integer accId:idList)
				systemAccountManager.rpush(new SysBalPush(accId, SysBalPush.CLASSIFY_INIT, new ReportInitParam(accId, loginUser.getId(), null)));
		}).start();
		return mapper.writeValueAsString(
				new ResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "success"));
	}

	/**
	 * 单个账号动态初始化
	 */
	@RequestMapping("/dynamicInitIndv")
	public String dynamicInitIndv(@RequestParam(value = "id") int id) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.nonNull(operator)) {
			systemAccountManager.rpush(new SysBalPush(id, SysBalPush.CLASSIFY_INIT, new ReportInitParam(id,operator.getId(),null)));
		} else {
			return mapper
					.writeValueAsString(new ResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请登陆"));
		}
		return mapper.writeValueAsString(
				new ResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "success"));
	}

	/**
	 * 单个账号初始化过程：查看
	 */
	@RequestMapping("/dynamicInitWatch")
	public String dynamicInitWatch(@RequestParam(value = "id") int id) throws JsonProcessingException {
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		String data = (String) template.boundHashOps(RedisKeys.ACC_SYS_INIT).get(String.valueOf(id));
		ResponseData res = new ResponseData<String>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "success");
		res.setData(data);
		return mapper.writeValueAsString(res);
	}

	@RequestMapping("/testIncome")
	public SimpleResponseData testIncome(@RequestBody RequestTestIncomeInputDTO requestBody)
			throws JsonProcessingException {
		log.info("TEST Income >> RequestBody:{}", mapper.writeValueAsString(requestBody));
		try {
			BizIncomeRequest o;
			// 要判断是否平台入款，若为中转类型不作盘口与层级校验
			if (IncomeRequestType.isPlatform(requestBody.getType())) {
				BizHandicap bizHandicap = handicapService.findFromCacheByCode(requestBody.getHandicap());
				if (null == bizHandicap || !Objects.equals(bizHandicap.getStatus(), 1)) {
					log.info("{} 盘口不存在,订单号：{}", requestBody.getHandicap(), requestBody.getOrderNo());
					return new SimpleResponseData(400, "盘口不存在");
				}
				if (requestBody.getType() == IncomeRequestType.PlatFromThird.getType()) {
					if (StringUtils.isBlank(requestBody.getFromAccount())) {
						log.info("盘口:{},第三方订单号：{},商家:{}", requestBody.getHandicap(), requestBody.getOrderNo(),
								requestBody.getFromAccount());
						return new SimpleResponseData(400, "第三方订单商家名称必传");
					}
				}
				AccountBaseInfo account = null;
				if (requestBody.getType() == IncomeRequestType.PlatFromThird.getType()) {
					account = accountService.getFromCacheByHandicapIdAndAccountAndBankName(bizHandicap.getId(),
							requestBody.getToAccount(), requestBody.getFromAccount());
					if (null == account) {
						log.info("{} 入款帐号不存在,订单号：{}", requestBody.getToAccount(), requestBody.getOrderNo());
						return new SimpleResponseData(400, "入款帐号不存在");
					}
				} else {
					if (requestBody.getToAccount().contains("#")) {
						// 支付宝转银行卡的入款订单可能对应多个收款账号，并且这些收款账号必须都是支付宝银行卡类型
						String[] toAccounts = requestBody.getToAccount().split("#");
						for (String toAcc : toAccounts) {
							AccountBaseInfo account1 = accountService
									.getFromCacheByHandicapIdAndAccount(bizHandicap.getId(), toAcc);
							if (Objects.isNull(account1)) {
								return new SimpleResponseData(400, " 收款账号为空,请核实!");

							}
							if (Objects.isNull(account1.getSubType())
									|| !InBankSubType.IN_BANK_ALIIN.getSubType().equals(account1.getSubType())) {
								return new SimpleResponseData(400, "收款账号:" + account1.getAccount() + "不属于支付宝入款卡,请核实!");
							}
						}
					} else {
						account = accountService.getFromCacheByHandicapIdAndAccount(bizHandicap.getId(),
								requestBody.getToAccount());
						if (null == account) {
							log.info("{} 入款帐号不存在,订单号：{}", requestBody.getToAccount(), requestBody.getOrderNo());
							return new SimpleResponseData(400, "入款帐号不存在");
						}
					}
				}
				// 分开处理，公司入款与第三方
				if (requestBody.getType() == IncomeRequestType.PlatFromThird.getType()) {
					BizThirdRequest third = new BizThirdRequest();
					third.setHandicap(bizHandicap.getId());
					if (StringUtils.isNotEmpty(requestBody.getLevel())) {
						BizLevel bizLevel = levelService.findFromCache(bizHandicap.getId(), requestBody.getLevel());
						if (null != bizLevel) {
							third.setLevel(bizLevel.getId());
						}
					}
					third.setToAccount(account.getAccount());
					third.setAmount(requestBody.getAmount());
					third.setCreateTime(DateUtils.parseDate(requestBody.getCreateTime(), "yyyy-MM-dd HH:mm:ss"));
					if (StringUtils.isNotEmpty(requestBody.getAckTime())) {
						third.setAckTime(DateUtils.parseDate(requestBody.getAckTime(), "yyyy-MM-dd HH:mm:ss"));
					}
					third.setOrderNo(requestBody.getOrderNo());
					third.setRemark(requestBody.getRemark());
					third.setFromAccount(requestBody.getFromAccount());// 保存商家
					third.setMemberUserName(requestBody.getUsername());
					third.setMemberRealName(requestBody.getRealname());
					if (StringUtils.isNotBlank(requestBody.getFee())) {
						third.setFee(new BigDecimal(requestBody.getFee()));
					} else {
						// 平台传空 则设置0.0
						third.setFee(BigDecimal.ZERO);
					}
                    Income2Controller.saveThirdOrder(third, log, redisService, thirdRequestService);
                    return new SimpleResponseData(1, "OK");
				} else {
					o = new BizIncomeRequest();
					if (!requestBody.getToAccount().contains("#")) {
						o.setToId(account.getId());// 如果订单对应多个收款账号,先不保存toId
						o.setToAccount(account.getAccount());
					} else {
						o.setToAccount(requestBody.getToAccount());
					}
					o.setHandicap(bizHandicap.getId());
					if (StringUtils.isNotBlank(requestBody.getLevel())) {
						BizLevel bizLevel = levelService.findFromCache(bizHandicap.getId(), requestBody.getLevel());
						if (null != bizLevel) {
							o.setLevel(bizLevel.getId());
						}
					}
					// o.setToAccount(account.getAccount());
					o.setAmount(requestBody.getAmount());
					o.setCreateTime(DateUtils.parseDate(requestBody.getCreateTime(), "yyyy-MM-dd HH:mm:ss"));
					if (StringUtils.isNotBlank(requestBody.getAckTime())) {
						o.setUpdateTime(DateUtils.parseDate(requestBody.getAckTime(), "yyyy-MM-dd HH:mm:ss"));
					}
					o.setOrderNo(requestBody.getOrderNo());
					o.setRemark(requestBody.getRemark());
					o.setType(requestBody.getType());
					o.setFromAccount(requestBody.getFromAccount());
					// o.setMemberCode(requestBody.getUsercode());
					o.setMemberUserName(requestBody.getUsername());
					o.setMemberRealName(requestBody.getRealname());
					if (o.getType() != null && (o.getType().equals(IncomeRequestType.PlatFromAli.getType())
							|| o.getType().equals(IncomeRequestType.PlatFromWechat.getType()))) {
						o.setStatus(IncomeRequestStatus.Matched.getStatus());
					} else {
						o.setStatus(IncomeRequestStatus.Matching.getStatus());
					}
					if (StringUtils.isNotBlank(requestBody.getFee())) {
						o.setFee(new BigDecimal(requestBody.getFee()));
					}
				}
			} else {
				SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
				if (operator == null) {
					throw new Exception("会话超时，请重新登陆.");
				}
				BizAccount fromAccount = accountService.getById(requestBody.getFromId());
				if (fromAccount == null) {
					throw new Exception("账号不存在.");
				}
				// 转账总金额=转账金额+转账手续费
				float tranfer = requestBody.getAmount().floatValue()
						+ (StringUtils.isBlank(requestBody.getFee()) ? 0F : Float.parseFloat(requestBody.getFee()));
				// 系统余额
				float balance = fromAccount.getBalance() == null ? 0F : fromAccount.getBalance().floatValue();
				// 银行余额
				float bankBalance = fromAccount.getBankBalance() == null ? 0F
						: fromAccount.getBankBalance().floatValue();
				if ((!fromAccount.getType().equals(AccountType.InThird.getTypeId()))
						&& ((bankBalance < tranfer & balance < balance) || (balance - tranfer) * (-1) > bankBalance)) {
					// 系统余额与银行余额都小于转账总金额或（系统余额-转账总金额）的相反数大于银行余额
					throw new Exception("系统余额或银行余额不足.");
				}
				AccountBaseInfo to = accountService.getFromCacheById(requestBody.getToId());
				o = new BizIncomeRequest();
				o.setToId(requestBody.getToId());
				o.setFromId(requestBody.getFromId());
				o.setHandicap(0);
				o.setLevel(0);
				o.setToAccount(requestBody.getToAccount());
				o.setOperator(requestBody.getOperator());
				o.setAmount(requestBody.getAmount());
				o.setCreateTime(new Date());
				o.setOrderNo(requestBody.getOrderNo());
				o.setRemark(requestBody.getRemark());
				o.setType(requestBody.getType());
				o.setFromAccount(requestBody.getFromAccount());
				o.setMemberUserName(requestBody.getUsername());
				o.setMemberRealName(to != null ? to.getOwner() : requestBody.getRealname());
				if (o.getType() != null && (o.getType().equals(IncomeRequestType.PlatFromAli.getType())
						|| o.getType().equals(IncomeRequestType.PlatFromWechat.getType()))) {
					o.setStatus(IncomeRequestStatus.Matched.getStatus());
				} else {
					o.setStatus(IncomeRequestStatus.Matching.getStatus());
				}
				if (null != requestBody.getFee() && StringUtils.isNotEmpty(requestBody.getFee())) {
					o.setFee(new BigDecimal(requestBody.getFee()));
				}
				if (IncomeRequestType.WithdrawThirdToCustomer.getType() != requestBody.getType()) {
					allocateTransferService.unlockTrans(requestBody.getFromId(), o.getToId(), operator.getId());// 提现到客户卡无需解锁
				} else {
					o.setToAccountBank(requestBody.getToAccountBank());// 客户卡
				}
			}

			String json = mapper.writeValueAsString(o);
			try {
				log.info("Income[redis],rightPush before json:{}", json);
				redisService.rightPush(RedisTopics.INCOME_REQUEST, json);
				log.debug("Income[redis],orderNo:{}", requestBody.getOrderNo());
			} catch (Exception e) {
				log.error("", e);
				// 特殊处理，若redis
				// sentinel作主从切换，或当前master出现异常挂掉，切换时会有几秒钟的延迟连接不可用，不重复后直接入库
				if (null == incomeRequestService.findByHandicapAndOrderNo(o.getHandicap(), o.getOrderNo())) {
					log.info("Income[DB],orderNo: {}", o.getOrderNo());
					incomeRequestService.save(o, false);
				} else {
					log.info("Income error, orderNo: {} already exist.", o.getOrderNo());
				}
			}
			return new SimpleResponseData(1, "OK");
		} catch (Exception e) {
			log.error("Income  orderNo:" + requestBody.getOrderNo() + ",error.", e);
			return new SimpleResponseData(500, "Error");
		}

	}

	/**
	 * 余额上报</br>
	 * {</br>
	 * ####id，</br>
	 * ####bankBalance</br>
	 * }
	 */
	@RequestMapping("/pay")
	public String pay(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.info("Transfer( pay ) >> RequestBody:{}", bodyJson);
			HashMap<String, String> params = mapper.readValue(bodyJson, HashMap.class);
			String id = params.get("id");
			String bankBalance = params.get("bankBalance");
			String rptTm = params.get("rptTm");
			String logTm = params.get("logTm");
			String inoutModel = params.get("inout");
			String applyTask = params.get("applyTask");
			if (StringUtils.isBlank(id) || StringUtils.isBlank(bankBalance) || StringUtils.isBlank(rptTm)) {
				log.error("Transfer( pay ) >> params are empty. RequestBody:{}", bodyJson);
				return mapper.writeValueAsString(
						new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "Id is empty."));
			}
			if (StringUtils.isNotBlank(bankBalance)) {
				systemAccountManager.rpush(new SysBalPush(Integer.parseInt(id), SysBalPush.CLASSIFY_BANK_BAL,
						new BigDecimal(bankBalance)));
			}
			redisService.convertAndSend(RedisTopics.ACCOUNT_CHANGE_BROADCAST, bodyJson);
			AccountBaseInfo base = accountService.getFromCacheById(Integer.valueOf(id));
			if (Objects.nonNull(base) && StringUtils.isNotBlank(logTm) && StringUtils.isNumeric(logTm)) {
				MessageEntity<List<AccountEntity>> entity = new MessageEntity<>();
				entity.setAction(ActionEventEnum.NORMALMODE.ordinal());
				entity.setIp("无IP");
				List<AccountEntity> list = new ArrayList<>();
				AccountEntity d = new AccountEntity();
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
				accountService.savePauseOrResumeOrOnlineForMobile(Integer.valueOf(id), 22);
			} else {
				// 保存在线账号id到缓存
				accountService.saveOnlineAccontIds(Integer.valueOf(id), true);
				// 删除 暂停状态
				accountService.savePauseOrResumeAccountId(Integer.valueOf(id), 22);
			}
			if (StringUtils.isNumeric(logTm)) {
				// 流水上报时间
				redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_LOG_TM).put(id, logTm);
			}
			allocateTransService.applyRelBal(Integer.valueOf(id), new BigDecimal(bankBalance), Long.valueOf(rptTm), true);
			if("1".equals(applyTask)){
				allocOTaskSer.applyTask4MobileNew(Integer.valueOf(id), new BigDecimal(bankBalance));
			}
			allocateTransService.inOutModelCheck(id, inoutModel);
			return mapper.writeValueAsString(
					new SimpleResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "success"));
		} catch (Exception e) {
			return mapper.writeValueAsString(new SimpleResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"Transfer(Pay) error." + e.getLocalizedMessage()));
		}
	}

	/**
	 * 银行流水上报</br>
	 * { </br>
	 * ####balance：</br>
	 * ####banklogs：</br>
	 * ################{</br>
	 * ##################fromAccount，</br>
	 * ##################tradingTime，</br>
	 * ##################createTime，</br>
	 * ##################summary，</br>
	 * ##################amount，</br>
	 * ##################balance，</br>
	 * ##################remark，</br>
	 * ##################toAccount，</br>
	 * ##################toAccountOwner，</br>
	 * ##################bankType，</br>
	 * ##################status</br>
	 * ################}</br>
	 * }</br>
	 */
	@RequestMapping("/logs")
	public String logs(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			TreeMap<String, String> params = mapper.readValue(bodyJson, TreeMap.class);
			// MemCacheUtils.getInstance().getBanklogs().add(params.get("logs"));
			redisService.rightPush(RedisTopics.BANK_STATEMENT, params.get("logs"));
		} catch (Exception e) {
			return mapper.writeValueAsString(new SimpleResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"Failure, " + e.getMessage()));
		}
		return mapper.writeValueAsString(
				new SimpleResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "Success."));
	}

	/**
	 * 出款任务上报
	 * <p>
	 * {</br>
	 * #### fromAccountId</br>
	 * #### toAccountId</br>
	 * #### account</br>
	 * #### owner</br>
	 * #### bankType</br>
	 * #### bankAddr</br>
	 * #### amount</br>
	 * #### taskId</br>
	 * #### result</br>
	 * #### screenshot</br>
	 * #### remark</br>
	 * #### balance</br>
	 * }
	 */
	@RequestMapping("/turn")
	public String turn(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			TransferEntity entity = mapper.readValue(bodyJson, TransferEntity.class);
			if (Objects.isNull(entity) || Objects.isNull(entity.getFromAccountId())) {
				return mapper.writeValueAsString(
						new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "frId is empty."));
			}
			if (entity.getResult() != null && Objects.equals(entity.getResult(), 4))
				allocOTaskSer.alterStatusToUnknown(entity.getTaskId(), null, entity.getRemark(),
						StringUtils.trimToNull(entity.getScreenshot()));
			if (Objects.equals(entity.getResult(), 3)) {
				redisService.increment(RedisKeys.COUNT_FAILURE_TRANS, String.valueOf(entity.getFromAccountId()), 1);
			} else if (Objects.equals(entity.getResult(), 1)) {
				redisService.getFloatRedisTemplate().opsForHash().delete(RedisKeys.COUNT_FAILURE_TRANS,
						String.valueOf(entity.getFromAccountId()));
			}
			systemAccountManager.rpush(new SysBalPush(entity.getFromAccountId(), SysBalPush.CLASSIFY_TRANSFER, entity));
			return mapper.writeValueAsString(
					new SimpleResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "success"));
		} catch (Exception e) {
			return mapper.writeValueAsString(new SimpleResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"Transfer(outTurn) error." + e.getLocalizedMessage()));
		}
	}

	/**
	 * 生产转账任务并取走</br>
	 * {</br>
	 * ####spareout,返利提现1，0：内部中专</br>
	 * ####frId 转账方账号ID</br>
	 * ###toId 对方账号ID</br>
	 * ###amt 下发金额</br>
	 * }</br>
	 */
	@RequestMapping("/genTask")
	public String genTask(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			HashMap params = mapper.readValue(bodyJson, HashMap.class);
			Object frIdObj = params.get("frId");
			Object toIdObj = params.get("toId");
			Object amtObj = params.get("amt");
			Object toAccObj = params.get("toAccount");
			Object toOwnerObj = params.get("toOwner");
			Object toBankObj = params.get("toBankType");
			Object toBankNObj = params.get("toBankName");
			Object spareoutObj = params.get("spareout");
			if (Objects.isNull(frIdObj) || Objects.isNull(toIdObj) || Objects.isNull(amtObj))
				return mapper.writeValueAsString(
						new SimpleResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数为空"));
			if (!StringUtils.isNumeric(frIdObj.toString()))
				return mapper.writeValueAsString(
						new SimpleResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "frId应为数字"));
			if (!StringUtils.isNumeric(toIdObj.toString()))
				return mapper.writeValueAsString(
						new SimpleResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "toId应为数字"));
			int frId = Integer.valueOf(frIdObj.toString()), toId = Integer.valueOf(toIdObj.toString());
			BigDecimal amt = new BigDecimal(amtObj.toString()).setScale(2, BigDecimal.ROUND_HALF_UP);
			Integer spareout = StringUtils.isNumeric(String.valueOf(spareoutObj))
					? Integer.valueOf(String.valueOf(spareoutObj))
					: 0;
			AccountBaseInfo frBase = accountService.getFromCacheById(frId);
			AccountBaseInfo toBase = accountService.getFromCacheById(toId);
			if (amt.compareTo(BigDecimal.ZERO) <= 0)
				return mapper.writeValueAsString(
						new SimpleResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "转账金额不能小于0"));
			if (Objects.equals(spareout, 1)) {// 返利提现
				BizAccountRebate rebate = new BizAccountRebate();
				rebate.setUid(String.valueOf((100 + System.currentTimeMillis() % 100)));
				rebate.setTid(String.valueOf("R" + System.currentTimeMillis()));
				rebate.setAccountId(frId);
				// rebate.setToAccount(toAccObj.toString());
				// rebate.setToHolder(toOwnerObj.toString());
				// rebate.setToAccountType(toBankObj.toString());
				// rebate.setToAccountInfo(toBankNObj.toString());
				rebate.setToAccount(toBase.getAccount());
				rebate.setToHolder(toBase.getOwner());
				rebate.setToAccountType(toBase.getBankType());
				rebate.setToAccountInfo(toBase.getBankName());
				rebate.setAmount(amt);
				rebate.setBalance(amt.multiply(new BigDecimal(3.2)));
				rebate.setStatus(0);
				rebate.setCreateTime(new Date());
				rebate.setHandicap(toBase.getHandicapId());
				rebate.setAsignTime(new Date());
				rebate = accountRebateService.saveAndFlush(rebate);
				TransferEntity ret = new TransferEntity();
				ret.setFromAccountId(rebate.getAccountId());
				ret.setAccount(StringUtils.trimToNull(rebate.getToAccount()));
				ret.setTaskId(rebate.getId());
				ret.setAmount(rebate.getAmount().floatValue());
				ret.setOwner(StringUtils.trimToNull(rebate.getToHolder()));
				ret.setBankType(StringUtils.trimToNull(rebate.getToAccountType()));// Type对应的是ToAccountBank、BankAddr对应的是ToAccountName
				ret.setBankAddr(StringUtils.trimToNull(rebate.getToAccountInfo()));
				ret.setAcquireTime(System.currentTimeMillis());
				ret.setAcquireTime(System.currentTimeMillis());
				ret.setRemark("返利任务");
				systemAccountManager.regist(ret, BigDecimal.ZERO);
				return mapper.writeValueAsString(new SimpleResponseData(
						GeneralResponseData.ResponseStatus.SUCCESS.getValue(), mapper.writeValueAsString(ret)));
			} else if (Objects.equals(spareout, 2)) {// 会员出款
				TransferEntity ret = oTaskSer.applyTask4Robot(frId, new BigDecimal("12380.00"));
				return mapper.writeValueAsString(new SimpleResponseData(
						GeneralResponseData.ResponseStatus.SUCCESS.getValue(), mapper.writeValueAsString(ret)));
			} else {
				if (!(amt.floatValue() > amt.intValue()))
					return mapper.writeValueAsString(
							new SimpleResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "中转金额应该为小数"));
				BizIncomeRequest income = new BizIncomeRequest();
				income.setLevel(0);
				income.setHandicap(frBase.getHandicapId());
				income.setStatus(0);
				income.setAmount(amt);
				income.setType(107);
				income.setOrderNo("T" + String.valueOf(System.currentTimeMillis()));
				income.setOperator(null);
				income.setFromAccount(frBase.getAccount());
				income.setToAccount(toBase.getAccount());
				income.setMemberRealName(toBase.getOwner());
				income.setToAccountBank(toBase.getBankType());
				income.setFromId(frId);
				income.setToId(toId);
				income.setCreateTime(new Date());
				income = incomeRequestService.update(income);
				TransferEntity ret = new TransferEntity();
				ret.setFromAccountId(income.getFromId());
				ret.setToAccountId(income.getToId());
				ret.setAccount(toBase.getAccount());
				ret.setTaskId(null);
				ret.setAmount(income.getAmount().floatValue());
				ret.setOwner(toBase.getOwner());
				ret.setBankType(toBase.getBankType());// Type对应的是ToAccountBank、BankAddr对应的是ToAccountName
				ret.setBankAddr(toBase.getBankName());
				ret.setAcquireTime(System.currentTimeMillis());
				systemAccountManager.regist(ret, BigDecimal.ZERO);
				return mapper.writeValueAsString(new SimpleResponseData(
						GeneralResponseData.ResponseStatus.SUCCESS.getValue(), mapper.writeValueAsString(ret)));
			}
		} catch (Exception e) {
			return mapper.writeValueAsString(new SimpleResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"Transfer(outTurn) error." + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/activeQuickPay")
	private String flushByid(@RequestParam(value = "acc", required = false) String acc) throws JsonProcessingException {
		BizOtherAccount otheracc = quickPayService.getFromCacheByAccountNo(acc);
		if (null == otheracc) {
			log.info("QuickPayActiveV3 >> 云闪付账号不存在！  RequestBody:{}", acc);
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "云闪付账号不存在。"));
		}
		BizRebateUser rebateUser = rebateUserService.getFromCacheByUid(otheracc.getUid() + "");
		if (null == rebateUser) {
			log.info("QuickPayActiveV3 >> 兼职不存在！  RequestBody:{}", acc);
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "兼职不存在。"));
		}
		BizAccountMore more = accMoreSer.getFromCacheByUid(rebateUser.getUid());
		if (null == more || null == more.getMoible()) {
			log.info("QuickPayActiveV3 >> 兼职不存在！  RequestBody:{}", acc);
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "兼职不存在。"));
		}
		cabanaService.activeQuickPay(StringUtils.lowerCase(rebateUser.getUserName()), otheracc.getAccountNo(),
				otheracc.getLoginPwd(), more.getMoible());
		return mapper.writeValueAsString(
				new GeneralResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "激活流程发送成功"));
	}
}
