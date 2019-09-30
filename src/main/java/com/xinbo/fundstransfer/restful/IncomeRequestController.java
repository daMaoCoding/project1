package com.xinbo.fundstransfer.restful;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.accountfee.pojo.AccountFeeConfig;
import com.xinbo.fundstransfer.accountfee.service.AccountFeeService;
import com.xinbo.fundstransfer.component.net.socket.ActionEventEnum;
import com.xinbo.fundstransfer.component.net.socket.MessageEntity;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.DynamicSpecifications;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.*;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.IncomeMatchingInputDTO;
import com.xinbo.fundstransfer.domain.pojo.SaveThirdTransInputDTO;
import com.xinbo.fundstransfer.domain.pojo.SysBalTrans;
import com.xinbo.fundstransfer.domain.repository.AccountLevelRepository;
import com.xinbo.fundstransfer.report.SystemAccountManager;
import com.xinbo.fundstransfer.service.*;
import com.xinbo.fundstransfer.service.impl.RedisDistributedLockUtils;
import com.xinbo.fundstransfer.unionpay.ysf.service.YSFService;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;

@RestController
@RequestMapping("/r/income")
public class IncomeRequestController extends BaseController {
	private static final Logger logger = LoggerFactory.getLogger(IncomeRequestController.class);
	private static final String CUSTOMER_SENDMESSAGE_SUCCEED = "succeed";
	private AccountService accountService;
	private IncomeRequestService incomeRequestService;
	private HandicapService handicapService;
	private LevelService levelService;
	private BankLogService bankLogService;
	TransactionLogService transactionLogService;
	SysUserService sysUserService;
	BizThirdLogService bizThirdLogService;
	AccountLevelRepository accountLevelRepository;
	private ObjectMapper mapper;
	@Autowired
	private SysDataPermissionService dataPermissionService;
	@Autowired
	private AssignAWInAccountService awInAccountService;
	@Autowired
	private TransMonitorService transMonitorService;
	@Autowired
	private YSFService ysfService;
	@Autowired
	private RebateApiService rebateApiService;
	@Autowired
	private SystemAccountManager systemAccountManager;
	private static String GENERATE_NEW_ORDER_KEY = "addIncomeReqOrderForBankLog:";
	@Autowired
	private AllocateTransService allocateTransService;
	@Autowired
	private AccountChangeService accountChangeService;
	@Autowired
	private AccountController accountController;

	@Autowired
	private AccountClickService accountClickService;

	@Autowired
	public IncomeRequestController(AccountService accountService, IncomeRequestService incomeRequestService,
			HandicapService handicapService, LevelService levelService, BankLogService bankLogService,
			TransactionLogService transactionLogService, SysUserService sysUserService,
			BizThirdLogService bizThirdLogService, AccountLevelRepository accountLevelRepository, ObjectMapper mapper) {
		this.accountService = accountService;
		this.incomeRequestService = incomeRequestService;
		this.handicapService = handicapService;
		this.levelService = levelService;
		this.bankLogService = bankLogService;
		this.transactionLogService = transactionLogService;
		this.sysUserService = sysUserService;
		this.bizThirdLogService = bizThirdLogService;
		this.accountLevelRepository = accountLevelRepository;
		this.sysUserService = sysUserService;
		this.mapper = mapper;
	}

	/**
	 * 查询 已分配的支付宝 微信账号
	 *
	 * @param type
	 *            {@link AccountType}
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/getAssignedAWINAccounts")
	public String getAssignedAWINAccounts(@RequestParam(value = "type") Integer type) throws JsonProcessingException {
		GeneralResponseData<Map<Integer, Set<Object[]>>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功！");
		try {
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆！"));

			}
			Map<Integer, Set<Object[]>> ret = awInAccountService.getAccountIdsByUser(sysUser.getId(), type);
			responseData.setData(ret);
			return mapper.writeValueAsString(responseData);
		} catch (JsonProcessingException e) {
			logger.error("IncomeRequestController.getAssignedAWINAccounts error:", e);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询失败！"));
		}
	}

	/**
	 * 获取正在匹配的微信账号
	 */
	@RequestMapping("/alipayAccountToMatch")
	public String alipayAccountToMatch(@RequestParam(value = "handicap", required = false) Integer handicap,
			@RequestParam(value = "level", required = false) Integer level,
			@RequestParam(value = "alipayAccount", required = false) String alipayAccount,
			@RequestParam(value = "orderOnly", required = false) Integer orderOnly,
			@RequestParam(value = "flowOnly", required = false) Integer flowOnly,
			@RequestParam(value = "status", required = false) List<Integer> status,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "pageNo", required = false) Integer pageNo) throws JsonProcessingException {
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆！"));

		}
		Integer type = 1;// 默认只查有流水的微信账号
		if (orderOnly != null && flowOnly != null) {
			type = 3;
		} else if (orderOnly == null && flowOnly == null) {
			type = 1;
		} else {
			type = orderOnly != null ? orderOnly : flowOnly;
		}
		PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE);
		List<Object> list = incomeRequestService.findAlipayAccount(pageRequest, handicap, level, alipayAccount, status,
				type);
		GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功！");
		return null;
	}

	/**
	 * 入款已匹配 已取消 订单号模态框悬浮事件
	 */
	@RequestMapping("/matchedInfo")
	public String getMatchedInfo(@RequestParam(value = "reqId") Long incomeRequestId) throws JsonProcessingException {
		GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功！");
		try {
			Object[] obj = (Object[]) incomeRequestService.getMatchedInfo(incomeRequestId);
			Map<String, Object> map = new HashMap<>();
			if (obj != null) {
				map.put("handicapNameHover", obj[0]);
				map.put("levelNameHover", obj[1]);
				map.put("memberAccountHover", obj[2]);
				map.put("toAccountHover", obj[3]);
				map.put("amountHover", obj[4]);
				map.put("bankAmountHover", obj[5]);
				map.put("amountMinusHover", obj[6]);
				map.put("orderNoHover", obj[7]);
				map.put("receiverHover", obj[8]);
				map.put("receivedBankHover", obj[9]);
				map.put("bankFlowRemarkHover", obj[10]);
				map.put("creatTimeHover", obj[11]);
				map.put("tradingTimeHover", obj[12]);
				map.put("matchedTimeHover", obj[13]);
				Long time = ((Timestamp) obj[13]).getTime() - ((Timestamp) obj[11]).getTime();
				map.put("consumeTimeHover", CommonUtils.convertTime2String(time));
				map.put("payerHover", obj[14]);
			}
			responseData.setData(map);
		} catch (Exception e) {
			logger.error("根据入款请求id查询已匹配信息失败：{}", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询失败！");
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 激活工具抓取流水
	 */
	@RequestMapping("/activateTools")
	public String activateTools(@RequestParam(value = "accountId") Integer accountId) throws JsonProcessingException {
		GeneralResponseData<List<Map<String, Object>>> responseData;
		if (accountId == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数异常，请联系技术！");
			return mapper.writeValueAsString(responseData);
		}
		SysUser loginUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(loginUser))
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		MessageEntity<Integer> msg = new MessageEntity<>();
		msg.setAction(ActionEventEnum.CAPTURE.ordinal());
		msg.setData(accountId);
		msg.setIp(accountId.toString());
		redisService.convertAndSend(RedisTopics.PUSH_MESSAGE_TOOLS, mapper.writeValueAsString(msg));
		logger.info("activateTools >> 立即抓取流水 uid: {}  accountId: {}", loginUser.getUid(), accountId);
		responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "激活工具成功！");
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 转账记录获取
	 */
	@RequestMapping("/getExchangeLog")
	public String getExchangeLog(@RequestParam(value = "accountId") Integer accountId) throws JsonProcessingException {
		GeneralResponseData<List<Map<String, Object>>> responseData;
		if (accountId == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数异常，请联系技术！");
			return mapper.writeValueAsString(responseData);
		}
		MessageEntity<Integer> msg = new MessageEntity<>();
		msg.setAction(ActionEventEnum.TRANSFERINFO.ordinal());
		msg.setData(accountId);
		redisService.convertAndSend(RedisTopics.PUSH_MESSAGE_TOOLS, mapper.writeValueAsString(msg));
		responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 下发
	 */
	@RequestMapping("/doIssued")
	public String doIssued(@RequestParam(value = "accountId") Integer accountId) throws JsonProcessingException {
		GeneralResponseData<List<Map<String, Object>>> responseData;
		if (accountId == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数异常，请联系技术！");
			return mapper.writeValueAsString(responseData);
		}
		// 下发广播功能去掉
		// MessageEntity<Integer> msg = new MessageEntity<>();
		// msg.setAction(ActionEventEnum.TRANSFER.ordinal());
		// msg.setData(accountId);
		// redisService.convertAndSend(RedisTopics.PUSH_MESSAGE_TOOLS,
		// mapper.writeValueAsString(msg));
		responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 客服发消息 银行卡提单发消息调用
	 *
	 * @param accountId
	 *            公司入款当前页签的账号id 以便发送到对应审核人
	 */
	@RequiresPermissions(value = { "IncomeAuditCompTotal:CustomerSendMessage:*",
			"IncomeAuditComp:CustomerSendMessage:*" }, logical = Logical.OR)
	@RequestMapping("/customerSendMsg")
	public String customerSendMsg(@RequestParam(value = "id") Long requestId,
			@RequestParam(value = "accountId") Long accountId, @RequestParam(value = "message") String message)
			throws JsonProcessingException {
		GeneralResponseData<List<Map<String, Object>>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "发送消息成功");
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆！"));

		}
		if (accountId == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"参数异常，请联系技术人员");
			return mapper.writeValueAsString(responseData);
		}
		if (message == null || "".equals(message)) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请填写备注信息");
			return mapper.writeValueAsString(responseData);
		}
		try {
			String flag = incomeRequestService.customerSendMsg(requestId, accountId, message, sysUser.getUsername());
			if (!flag.equals(CUSTOMER_SENDMESSAGE_SUCCEED)) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "发送消息失败");
				return mapper.writeValueAsString(responseData);
			}
		} catch (Exception e) {
			logger.error("发送消息失败：{}", e);
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 客服添加备注
	 */
	@RequiresPermissions(value = { "IncomeAuditCompTotal:CustomerAddRemark:*",
			"IncomeAuditComp:CustomerAddRemark:*" }, logical = Logical.OR)
	@RequestMapping("/customerAddRemark")
	public String customerAddRemark(@RequestParam(value = "id") Long requestId,
			@RequestParam(value = "remark") String remark) throws JsonProcessingException {
		GeneralResponseData<List<Map<String, Object>>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "添加备注成功");
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆！"));
		}
		try {
			if (requestId == null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"参数异常，请联系技术");
				return mapper.writeValueAsString(responseData);
			}
			if (remark == null || "".equals(remark)) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请填写备注信息");
				return mapper.writeValueAsString(responseData);
			}
			BizIncomeRequest bizIncomeRequest = incomeRequestService.getBizIncomeRequestByIdForUpdate(requestId);
			StringBuilder addRemark = new StringBuilder();
			String addRemarkTime = CommonUtils.getNowDate();
			if (StringUtils.isNotEmpty(bizIncomeRequest.getRemark())) {
				addRemark.append(bizIncomeRequest.getRemark());
				addRemark.append("\r\n" + addRemarkTime + "  " + sysUser.getUsername() + "\r\n" + remark);
			} else
				addRemark.append(addRemarkTime + "  " + sysUser.getUsername() + "\r\n" + remark);
			bizIncomeRequest.setRemark(addRemark.toString());
			bizIncomeRequest.setUpdateTime(new Date());
			incomeRequestService.update(bizIncomeRequest);
		} catch (Exception e) {
			logger.error("添加备注失败:{}", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "添加备注失败");
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 公司入款补提单，生成请求body <br/>
	 * handicap，盘口编码， sUserID，会员帐号 fAmount，充值金额 sDepositName，存款人姓名 sBankAccount，收款卡号
	 * sBankType，收款银行 sReceiptsHuman，收款人姓名 sBankAddr，收款人开户行 sRemark，备注
	 * iSaveType，存款方式 ,1网银转账，2ATM自动柜员机，3ATM现金入款，4银行柜台，5手机银行，10其他
	 * sSaveTime，存款时间，格式必须是：YYYY-MM-DD HH:MM:SS
	 */
	@RequiresPermissions(value = { "IncomeAuditComp:AuditorAddOrder:*",
			"IncomeAuditCompTotal:AuditorAddOrder:*" }, logical = Logical.OR)
	@RequestMapping("/generateIncomeRequestOrder")
	public String generateIncomeRequestOrder(@RequestParam(value = "amount") String amount,
			@RequestParam(value = "type") Integer depositType, @RequestParam(value = "name") String name,
			@RequestParam(value = "memberAccount") String memberAccount,
			@RequestParam(value = "bankLogId") Long bankLogId, @RequestParam(value = "pfTypeSub") Integer pfTypeSub,
			@RequestParam(value = "localHostIp", required = false) String localHostIp,
			@RequestParam(value = "accountId", required = false) Integer accountId,
			@RequestParam(value = "accountNo", required = false) String accountNo,
			@RequestParam(value = "remark") String remark) throws JsonProcessingException {
		GeneralResponseData<List<Map<String, Object>>> responseData;
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆！"));

		}
		logger.info(
				"补提单参数,补提单人:{},流水id:{},补提单主机Ip:{},时间:{}, name:{},memberAccount:{},accountNo:{},accountId:{},type:{},remark:{},amount: {},pfTypeSub:{}",
				sysUser.getUid(), bankLogId, localHostIp, CommonUtils.getNowDate(), name, memberAccount, accountNo,
				accountId, depositType, remark, amount, pfTypeSub);
		if (StringUtils.isBlank(accountNo) || StringUtils.isBlank(memberAccount) || amount == null
				|| depositType == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请填写相关信息再提交！");
			return mapper.writeValueAsString(responseData);
		}
		if (bankLogId == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "重复点击！");
			return mapper.writeValueAsString(responseData);
		}
		if (new BigDecimal(amount).equals(0)) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "存款金额必须大于0！");
			return mapper.writeValueAsString(responseData);
		}
		BizBankLog bizBankLog = bankLogService.get(bankLogId);
		if (bizBankLog == null || !bizBankLog.getStatus().equals(BankLogStatus.Matching.getStatus())) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"流水状态已改变不能补提单！");
			return mapper.writeValueAsString(responseData);
		}
		if (new BigDecimal(amount).compareTo(bizBankLog.getAmount()) != 0) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"与流水金额不一致,不能补提单！");
			return mapper.writeValueAsString(responseData);
		}
		if (accountId == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "账号Id丢失！"));
		}
		AccountBaseInfo accountBaseInfo = accountService
				.getFromCacheById(accountId == bizBankLog.getFromAccount() ? accountId : bizBankLog.getFromAccount());
		if (accountBaseInfo == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "公司入款账号不存在！");
			return mapper.writeValueAsString(responseData);
		}
		BizHandicap bizHandicap = handicapService.findFromCacheById(accountBaseInfo.getHandicapId());
		if (bizHandicap == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"入款账号对应盘口信息不存在！");
			return mapper.writeValueAsString(responseData);
		}
		if (StringUtils.isNotBlank(StringUtils.trim(remark))) {
			if (StringUtils.isBlank(name)) {
				name = "ATM存款补单";
				if (StringUtils.isNotBlank(localHostIp)) {
					remark = new StringBuilder("补提单:").append(CommonUtils.getNowDate()).append(" ")
							.append(sysUser.getUid()).append("(").append(localHostIp).append(")").append(remark)
							.append(name).toString();
				} else {
					remark = new StringBuilder("补提单:").append(CommonUtils.getNowDate()).append(" ")
							.append(sysUser.getUid()).append(remark).append(name).toString();
				}
			} else {
				if (StringUtils.isNotBlank(localHostIp)) {
					remark = new StringBuilder("补提单:").append(CommonUtils.getNowDate()).append(" ")
							.append(sysUser.getUid()).append("(").append(localHostIp).append(")").append(remark)
							.append(name).toString();
				} else {
					remark = new StringBuilder("补提单:").append(CommonUtils.getNowDate()).append(" ")
							.append(sysUser.getUid()).append(remark).append(name).toString();
				}
			}
		} else {
			if (StringUtils.isBlank(name)) {
				name = "ATM存款补单";
				if (StringUtils.isNotBlank(localHostIp)) {
					remark = new StringBuilder("补提单").append(CommonUtils.getNowDate()).append(" ")
							.append(sysUser.getUid()).append("(").append(localHostIp).append(")").append(name)
							.toString();
				} else {
					remark = new StringBuilder("补提单").append(CommonUtils.getNowDate()).append(" ")
							.append(sysUser.getUid()).append(name).toString();
				}
			} else {
				if (StringUtils.isNotBlank(localHostIp)) {
					remark = new StringBuilder("补提单").append(CommonUtils.getNowDate()).append(" ")
							.append(sysUser.getUid()).append("(").append(localHostIp).append(")").append(name)
							.toString();
				} else {
					remark = new StringBuilder("补提单").append(CommonUtils.getNowDate()).append(" ")
							.append(sysUser.getUid()).append(name).toString();
				}
			}
		}
		String ret;
		RedisDistributedLockUtils lockUtils = new RedisDistributedLockUtils(GENERATE_NEW_ORDER_KEY + bankLogId,
				1 * 1000, 20 * 1000);
		try {
			if (lockUtils != null && lockUtils.acquireLock()) {
				ret = incomeRequestService.generateIncomeRequestOrder(pfTypeSub, bizHandicap, name, memberAccount,
						amount, accountBaseInfo, depositType, remark, sysUser.getUid());
			} else {
				// 10秒内重复点击
				ret = lockUtils != null && !lockUtils.acquireLock() ? "repeat" : "getLockFail";
			}
			logger.info("补提单 返回结果:{}", ret);
			if (StringUtils.isBlank(ret)) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"补提单失败,无返回结果!");
			} else {

				// ret ="success"
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						"补提单成功！");
				if (!"success".equals(ret)) {
					if ("repeat".equals(ret)) {
						logger.info("重复补提单:流水id:{},金额:{}", bankLogId, amount);
						responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
								"补提单20秒内重复点击！");
					} else if ("getLockFail".equals(ret)) {
						logger.info("本机拿不到锁:{},流水id:{},金额:{}", CommonUtils.getInternalIp(), bankLogId, amount);
						responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
								"补提单无法获取锁！");
					} else {
						responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
								"补提单失败" + ret);
					}
				}
			}

		} catch (Exception e) {
			logger.info("补提单失败:{}", e);
			lockUtils.releaseLock();// 补单失败释放锁,防止不能继续补单
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"补提单操作失败:" + e.getLocalizedMessage());
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 查询三方流水 第三方入款
	 */
	@RequestMapping("/thirdLogNoCount")
	public String thirdLogNoCount(@RequestParam(value = "handicap") Integer handicap,
			@RequestParam(value = "level") Integer level, @RequestParam(value = "account") Integer accountId,
			@RequestParam(value = "orderNo") String orderNo, @RequestParam(value = "channel") String channel,
			@RequestParam(value = "startTime") String startTime, @RequestParam(value = "endTime") String endTime,
			@RequestParam(value = "startMoney") String startMoney, @RequestParam(value = "endMoney") String endMoney,
			@RequestParam(value = "pageNo") Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		GeneralResponseData<List<Map<String, Object>>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询三方流水成功");
		try {
			Set<Integer> accountIdSets = new HashSet<>();
			if (accountId != null) {
				accountIdSets = new HashSet<>();
				accountIdSets.add(accountId);
				level = null;
			}
			if (level != null) {
				accountIdSets = new HashSet<>();
				List<BizAccountLevel> bizAccountLevelList = accountLevelRepository.findByLevelId(level);
				if (bizAccountLevelList != null && bizAccountLevelList.size() > 0) {
					Set<Integer> accountIdSet = new HashSet<>();
					bizAccountLevelList.forEach(p -> accountIdSet.add(p.getAccountId()));
					accountIdSets.addAll(accountIdSet);
				}
				if (accountIdSets == null || accountIdSets.size() == 0) {
					responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
							"查不到记录");
					responseData.setData(null);
					return mapper.writeValueAsString(responseData);
				}
				handicap = null;
			}
			if (handicap != null) {
				List<BizLevel> bizLevelList = levelService.findByHandicapId(handicap);
				if (bizLevelList != null && bizLevelList.size() > 0) {
					Set<Integer> levelIdSet = new HashSet<>();
					bizLevelList.forEach((p) -> {
						levelIdSet.add(p.getId());
					});
					List<BizAccountLevel> bizAccountLevelList = accountLevelRepository
							.findByLevelIdIn(new ArrayList<>(levelIdSet));
					if (bizAccountLevelList != null && bizAccountLevelList.size() > 0) {
						Set<Integer> accountIdset = new HashSet<>();
						bizAccountLevelList.forEach(p -> accountIdset.add(p.getAccountId()));
						accountIdSets.addAll(accountIdset);
					}
				}
				if (accountIdSets == null || accountIdSets.size() == 0) {
					responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
							"查不到记录");
					responseData.setData(null);
					return mapper.writeValueAsString(responseData);
				}
			}
			Sort sort = new Sort(Sort.Direction.DESC, "id");
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					sort);
			Page<BizThirdLog> page = bizThirdLogService.pageNoCount(orderNo, channel, startTime, endTime, startMoney,
					endMoney, new ArrayList<>(accountIdSets), pageRequest);
			List<Map<String, Object>> mapList = new ArrayList<>();
			Integer levelParam = level;
			Integer handicapParam = handicap;
			if (page != null && page.getContent() != null && page.getContent().size() > 0) {
				List<BizThirdLog> list = page.getContent();
				list.forEach((p) -> {
					Map map = new HashMap();
					StringBuilder levelName = new StringBuilder();
					StringBuilder handicapName = new StringBuilder();
					if (levelParam != null) {
						BizLevel bizLevel = levelService.findFromCache(levelParam);
						levelName.append(StringUtils.isNotBlank(bizLevel.getName()) ? bizLevel.getName() : "");
					} else {
						List<BizAccountLevel> bizAccountLevelList = accountLevelRepository
								.findByAccountId(p.getFromAccount());
						Set<Integer> levelIdSet = new TreeSet<>();
						List<BizLevel> bizLevelList = new ArrayList<>();
						if (bizAccountLevelList != null && bizAccountLevelList.size() > 0) {
							bizAccountLevelList.forEach((q) -> levelIdSet.add(q.getLevelId()));
							if (levelIdSet != null && levelIdSet.size() > 0) {
								new ArrayList<>(levelIdSet)
										.forEach((g) -> bizLevelList.add(levelService.findFromCache(g)));
							}
							if (bizLevelList != null && bizLevelList.size() > 0) {
								bizLevelList.forEach((k) -> levelName.append(k.getName()).append("|"));
							}
						}
					}
					if (handicapParam != null) {
						BizHandicap bizHandicap = handicapService.findFromCacheById(handicapParam);
						handicapName.append(StringUtils.isNotBlank(bizHandicap.getName()) ? bizHandicap.getName() : "");
					} else {
						List<BizAccountLevel> bizAccountLevelList = accountLevelRepository
								.findByAccountId(p.getFromAccount());
						Set<Integer> levelIdSet = new TreeSet<>();
						List<BizLevel> bizLevelList = new ArrayList<>();
						Set<BizHandicap> bizHandicapList = new HashSet<>();
						if (bizAccountLevelList != null && bizAccountLevelList.size() > 0) {
							bizAccountLevelList.forEach((q) -> levelIdSet.add(q.getLevelId()));
							if (levelIdSet != null && levelIdSet.size() > 0) {
								new ArrayList<>(levelIdSet)
										.forEach((g) -> bizLevelList.add(levelService.findFromCache(g)));
							}
							if (bizLevelList != null && bizLevelList.size() > 0) {
								bizLevelList.forEach((k) -> bizHandicapList
										.add(handicapService.findFromCacheById(k.getHandicapId())));
							}
							if (bizHandicapList != null && bizHandicapList.size() > 0) {
								bizHandicapList.forEach((h) -> handicapName.append(h.getName()).append("|"));
							}
						}
					}
					map.put("handicap",
							StringUtils.isNotEmpty(handicapName) ? handicapName.substring(0, handicapName.length() - 1)
									: "");
					map.put("level",
							StringUtils.isNotEmpty(levelName) ? levelName.substring(0, levelName.length() - 1) : "");
					AccountBaseInfo account = accountService.getFromCacheById(p.getFromAccount());
					if (account != null) {
						map.put("fromAccountId", account.getId());
						map.put("thirdBankName", account.getBankName());
						map.put("thirdAccount", account.getAccount());
					}
					map.put("thirdAccontBalance", p.getBalance());
					map.put("amount", p.getAmount());
					map.put("fee", p.getFee());
					map.put("orderNo", p.getOrderNo());
					map.put("tradingTime", p.getTradingTime());
					map.put("remark", p.getRemark());
					map.put("channel", p.getChannel());
					mapList.add(map);
				});
			}
			Paging paging = new Paging(page);
			responseData.setPage(paging);
			responseData.setData(mapList);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error("查询第三方流水失败：{}", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询失败");
			return mapper.writeValueAsString(responseData);
		}
	}

	/**
	 * 查询三方流水总金额 第三方入款
	 */
	@RequestMapping("/thirdLogSum")
	public String thirdLogSum(@RequestParam(value = "handicap") Integer handicap,
			@RequestParam(value = "level") Integer level, @RequestParam(value = "account") Integer accountId,
			@RequestParam(value = "orderNo") String orderNo, @RequestParam(value = "channel") String channel,
			@RequestParam(value = "startTime") String startTime, @RequestParam(value = "endTime") String endTime,
			@RequestParam(value = "startMoney") String startMoney, @RequestParam(value = "endMoney") String endMoney,
			@RequestParam(value = "pageNo") Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {

		GeneralResponseData<List> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询三方流水总金额成功");
		try {
			Set<Integer> accountIdSets = new HashSet<>();
			if (accountId != null) {
				accountIdSets = new HashSet<>();
				accountIdSets.add(accountId);
				level = null;
			}
			if (level != null) {
				accountIdSets = new HashSet<>();
				List<BizAccountLevel> bizAccountLevelList = accountLevelRepository.findByLevelId(level);
				if (bizAccountLevelList != null && bizAccountLevelList.size() > 0) {
					Set<Integer> accountIdSet = new HashSet<>();
					bizAccountLevelList.forEach(p -> accountIdSet.add(p.getAccountId()));
					accountIdSets.addAll(accountIdSet);
				}
				if (accountIdSets == null || accountIdSets.size() == 0) {
					responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
							"查不到记录");
					responseData.setData(null);
					return mapper.writeValueAsString(responseData);
				}
				handicap = null;
			}
			if (handicap != null) {
				List<BizLevel> bizLevelList = levelService.findByHandicapId(handicap);
				if (bizLevelList != null && bizLevelList.size() > 0) {
					Set<Integer> levelIdSet = new HashSet<>();
					bizLevelList.forEach((p) -> levelIdSet.add(p.getId()));
					List<BizAccountLevel> bizAccountLevelList = accountLevelRepository
							.findByLevelIdIn(new ArrayList<>(levelIdSet));
					if (bizAccountLevelList != null && bizAccountLevelList.size() > 0) {
						Set<Integer> accountIdset = new HashSet<>();
						bizAccountLevelList.forEach(p -> accountIdset.add(p.getAccountId()));
						accountIdSets.addAll(accountIdset);
					}
				}
				if (accountIdSets == null || accountIdSets.size() == 0) {
					responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
							"查不到记录");
					responseData.setData(null);
					return mapper.writeValueAsString(responseData);
				}
			}
			Sort sort = new Sort(Sort.Direction.DESC, "id");
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					sort);
			List sum = bizThirdLogService.sum(orderNo, channel, startTime, endTime, startMoney, endMoney,
					new ArrayList<>(accountIdSets), pageRequest);
			responseData.setData(sum);// 金额 手续费 余额
		} catch (Exception e) {
			logger.error("查询三方流水总金额失败：{}", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
					"查询三方流水总金额失败");
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 查询三方流水总记录 第三方入款
	 */
	@RequestMapping("/thirdLogCount")
	public String thirdLogCount(@RequestParam(value = "handicap") Integer handicap,
			@RequestParam(value = "level") Integer level, @RequestParam(value = "account") Integer accountId,
			@RequestParam(value = "orderNo") String orderNo, @RequestParam(value = "channel") String channel,
			@RequestParam(value = "startTime") String startTime, @RequestParam(value = "endTime") String endTime,
			@RequestParam(value = "startMoney") String startMoney, @RequestParam(value = "endMoney") String endMoney,
			@RequestParam(value = "pageNo") Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		GeneralResponseData<String> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询三方流水总记录数成功");
		try {
			Set<Integer> accountIdSets = new HashSet<>();
			if (accountId != null) {
				accountIdSets = new HashSet<>();
				accountIdSets.add(accountId);
				level = null;
			}
			if (level != null) {
				accountIdSets = new HashSet<>();
				List<BizAccountLevel> bizAccountLevelList = accountLevelRepository.findByLevelId(level);
				if (bizAccountLevelList != null && bizAccountLevelList.size() > 0) {
					Set<Integer> accountIdSet = new HashSet<>();
					bizAccountLevelList.forEach(p -> accountIdSet.add(p.getAccountId()));
					accountIdSets.addAll(accountIdSet);
				}
				if (accountIdSets == null || accountIdSets.size() == 0) {
					responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
							"查不到记录");
					responseData.setData(null);
					return mapper.writeValueAsString(responseData);
				}
				handicap = null;
			}
			if (handicap != null) {
				List<BizLevel> bizLevelList = levelService.findByHandicapId(handicap);
				if (bizLevelList != null && bizLevelList.size() > 0) {
					Set<Integer> levelIdSet = new HashSet<>();
					bizLevelList.forEach((p) -> {
						levelIdSet.add(p.getId());
					});
					List<BizAccountLevel> bizAccountLevelList = accountLevelRepository
							.findByLevelIdIn(new ArrayList<>(levelIdSet));
					if (bizAccountLevelList != null && bizAccountLevelList.size() > 0) {
						Set<Integer> accountIdset = new HashSet<>();
						bizAccountLevelList.forEach(p -> accountIdset.add(p.getAccountId()));
						accountIdSets.addAll(accountIdset);
					}
				}
				if (accountIdSets == null || accountIdSets.size() == 0) {
					responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
							"查不到记录");
					responseData.setData(null);
					return mapper.writeValueAsString(responseData);
				}
			}
			Sort sort = new Sort(Sort.Direction.DESC, "id");
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					sort);
			Page<BizThirdLog> bizThirdLogPage = bizThirdLogService.page(orderNo, channel, startTime, endTime,
					startMoney, endMoney, new ArrayList<>(accountIdSets), pageRequest);
			Paging page;
			if (bizThirdLogPage != null && bizThirdLogPage.getContent() != null
					&& bizThirdLogPage.getContent().size() > 0) {
				page = CommonUtils.getPage(bizThirdLogPage.getNumber() + 1,
						pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
						String.valueOf(bizThirdLogPage.getTotalElements()));
			} else {
				page = CommonUtils.getPage(0, pageSize != null ? pageSize : AppConstants.PAGE_SIZE, "0");
			}
			responseData.setPage(page);
		} catch (Exception e) {
			logger.error("查询三方流水总记录数失败,{}", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
					"查询三方流水总记录数失败");
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 通过主键id查询系统提单
	 */
	@RequestMapping(value = "/getById")
	public String findSysRequestById(@RequestParam(value = "id") Long id) throws JsonProcessingException {
		GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
		try {
			BizIncomeRequest bizIncomeRequest = incomeRequestService.findById(id);
			if (bizIncomeRequest != null) {
				Map<String, Object> map = new HashMap<>();
				BizHandicap bizHandicap = handicapService.findFromCacheById(bizIncomeRequest.getHandicap());
				if (bizHandicap != null) {
					map.put("handicap", StringUtils.isNotBlank(bizHandicap.getName()) ? bizHandicap.getName() : "");
				}
				BizLevel bizLevel = levelService.findFromCache(bizIncomeRequest.getLevel());
				if (bizLevel != null) {
					map.put("level", StringUtils.isNotBlank(bizLevel.getName()) ? bizLevel.getName() : "");
				}
				map.put("member", bizIncomeRequest.getMemberUserName());
				map.put("memberRealName", bizIncomeRequest.getMemberRealName());
				map.put("amount", bizIncomeRequest.getAmount());
				map.put("orderNo", bizIncomeRequest.getOrderNo());
				map.put("creatTime", bizIncomeRequest.getCreateTime());
				responseData.setData(map);
			}
		} catch (Exception e) {
			logger.error("查询失败:{},参数：{}", e, id);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询失败");
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * * 查询入款审核 第三方入款 toAccount 是商号 不是id
	 *
	 * @param status
	 *            1已对账 ackTime不为空 0未对账 ackTime 为空
	 */
	@RequestMapping("/third")
	public String third(@RequestParam(value = "status") Integer status,
			@RequestParam(value = "handicap") Integer[] handicap, @RequestParam(value = "level") Integer level,
			@RequestParam(value = "member") String member, @RequestParam(value = "toAccount") String toAccount,
			@RequestParam(value = "thirdOrderNo") String thirdOrderNo,
			@RequestParam(value = "fromMoney") BigDecimal fromMoney,
			@RequestParam(value = "toMoney") BigDecimal toMoney, @RequestParam(value = "startTime") String startTime,
			@RequestParam(value = "endTime") String endTime, @RequestParam(value = "pageNo") Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		GeneralResponseData<List<Map<String, Object>>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		List<Map<String, Object>> list = new ArrayList<>();
		try {
			Sort sort;
			if (status == 1)
				sort = new Sort(Sort.Direction.DESC, "ackTime");
			else
				sort = new Sort(Sort.Direction.DESC, "createTime");

			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					sort);
			Page<BizThirdRequest> pageSysMatchedThird = incomeRequestService.findThirdMatchedOrUnMatchNoCount(status,
					handicap, level, member, thirdOrderNo, toAccount, fromMoney, toMoney, startTime, endTime,
					pageRequest);
			if (pageSysMatchedThird.getContent() != null && pageSysMatchedThird.getContent().size() > 0) {
				List<BizThirdRequest> contentList = pageSysMatchedThird.getContent();
				contentList.stream().forEach((p) -> {
					Map<String, Object> map1 = new HashMap<>();
					map1.put("handicapId", p.getHandicap());
					map1.put("level", p.getLevel());
					map1.put("member", StringUtils.isNotBlank(p.getMemberUserName()) ? p.getMemberUserName() : "");
					map1.put("memberCode", p.getMemberCode() != null ? p.getMemberCode() : "");
					map1.put("orderNo", StringUtils.isNotBlank(p.getOrderNo()) ? p.getOrderNo() : "");
					AccountBaseInfo bizAccount = accountService.getFromCacheByHandicapIdAndAccountAndBankName(
							p.getHandicap(), p.getToAccount(), p.getFromAccount());
					if (bizAccount != null) {
						map1.put("toId", bizAccount.getId());
						map1.put("toAccount",
								StringUtils.isNotBlank(bizAccount.getAccount()) ? bizAccount.getAccount() : "");
						map1.put("bankName",
								StringUtils.isNotBlank(bizAccount.getBankName()) ? bizAccount.getBankName() : "");
						map1.put("owner", StringUtils.isNotBlank(bizAccount.getOwner()) ? bizAccount.getOwner() : "");
					}
					map1.put("amount", p.getAmount() != null ? p.getAmount() : 0);
					map1.put("fee", p.getFee() != null ? p.getFee() : 0);
					map1.put("createTime", p.getCreateTime());
					map1.put("updateTime", p.getAckTime());
					map1.put("remark", StringUtils.isNotBlank(p.getRemark()) ? p.getRemark() : "");
					map1.put("id", p.getId());

					list.add(map1);
				});
				responseData.setData(list);
				Paging page = new Paging(pageSysMatchedThird);
				responseData.setPage(page);
			}
		} catch (Exception e) {
			logger.error("查询失败：", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "服务异常，请稍后...");
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 获取第三方 总额 toAccount 商号 不是id
	 * <p>
	 * status 1已对账 updateTime不为空 0未对账 updateTime 为空
	 */
	@RequestMapping("/thirdSumAmount")
	public String getThirdSum(@RequestParam(value = "status") Integer status,
			@RequestParam(value = "handicap") Integer[] handicap, @RequestParam(value = "level") Integer level,
			@RequestParam(value = "member") String member, @RequestParam(value = "toAccount") String toAccount,
			@RequestParam(value = "thirdOrderNo") String thirdOrderNo,
			@RequestParam(value = "fromMoney") BigDecimal fromMoney,
			@RequestParam(value = "toMoney") BigDecimal toMoney, @RequestParam(value = "startTime") String startTime,
			@RequestParam(value = "endTime") String endTime) throws JsonProcessingException {
		GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		BigDecimal[] sum = incomeRequestService.findThirdSumAmountByConditions(status, handicap, level, member,
				thirdOrderNo, toAccount, fromMoney, toMoney, startTime, endTime);
		Map<String, Object> map = new HashMap<>();
		if (sum == null || sum.length == 0) {
			map.put("amountAll", 0.00);
			map.put("feeAll", 0.00);
		} else {
			map.put("amountAll", Objects.isNull(sum[0]) ? 0 : sum[0]);
			map.put("feeAll", Objects.isNull(sum[1]) ? 0 : sum[1]);
		}
		responseData.setData(map);
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 获取第三方总记录数 toAccount 商号 不是 id status 1已对账 updateTime不为空 0未对账 updateTime 为空
	 */
	@RequestMapping("/thirdCount")
	public String getThirdPageInfo(@RequestParam(value = "status") Integer status,
			@RequestParam(value = "handicap") Integer[] handicap, @RequestParam(value = "level") Integer level,
			@RequestParam(value = "member") String member, @RequestParam(value = "toAccount") String toAccount,
			@RequestParam(value = "thirdOrderNo") String thirdOrderNo,
			@RequestParam(value = "fromMoney") BigDecimal fromMoney,
			@RequestParam(value = "toMoney") BigDecimal toMoney, @RequestParam(value = "startTime") String startTime,
			@RequestParam(value = "endTime") String endTime, @RequestParam(value = "pageNo") Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		GeneralResponseData<String> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		try {
			Sort sort;
			if (status == 1)
				sort = new Sort(Sort.Direction.DESC, "ackTime");
			else
				sort = new Sort(Sort.Direction.DESC, "createTime");
			Integer realPageSize = pageSize != null ? pageSize : AppConstants.PAGE_SIZE;
			PageRequest pageRequest1 = new PageRequest(pageNo, realPageSize, sort);
			Long count = incomeRequestService.findThirdMatchedOrUnMatchCount(status, handicap, level, member,
					thirdOrderNo, toAccount, fromMoney, toMoney, startTime, endTime, pageRequest1);
			Paging page;
			if (count > 0) {
				page = CommonUtils.getPage(pageNo + 1, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
						count.toString());
			} else {
				page = CommonUtils.getPage(0, pageSize != null ? pageSize : AppConstants.PAGE_SIZE, "0");
			}
			responseData.setPage(page);
		} catch (Exception e) {
			logger.error("获取总条数失败：{}", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue());
		}
		return mapper.writeValueAsString(responseData);
	}

	private Integer getStatus(String type) {
		Integer status = null;
		if (StringUtils.isNotBlank(type)) {
			if ("matched".equals(type)) {
				status = IncomeRequestStatus.Matched.getStatus();
			}
			if ("canceled".equals(type)) {
				status = IncomeRequestStatus.Canceled.getStatus();
			}
		}
		return status;
	}

	/**
	 * 查询已匹配或者已取消 公司入款记录
	 */
	@RequestMapping("/matchedOrCanceled")
	public String searchMatchedOrCanceled(@RequestParam(value = "type") String type,
			@RequestParam(value = "accountIds") Integer[] accountIdsArray,
			@RequestParam(value = "handicap") Integer[] handicap, @RequestParam(value = "level") Integer level,
			@RequestParam(value = "account") Integer accountId, @RequestParam(value = "member") String member,
			@RequestParam(value = "fromMoney") BigDecimal fromMoney,
			@RequestParam(value = "toMoney") BigDecimal toMoney, @RequestParam(value = "startTime") String startTime,
			@RequestParam(value = "endTime") String endTime, @RequestParam(value = "orderNo") String orderNo,
			@RequestParam(value = "pageNo") Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		GeneralResponseData<Map<String, Object>> responseData;
		SysUser sysUser1 = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser1 == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		Map<String, Object> map = new LinkedHashMap<>();
		Integer status = getStatus(type);
		Integer[] accoutIdArray = accountIdsArray;
		if (accountId != null) {
			accoutIdArray = new Integer[] { accountId };
		}
		PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
				Sort.Direction.DESC, "createTime", "id");
		try {
			// 查状态 accountIds传null
			Page<BizIncomeRequest> page = incomeRequestService.findMatchedOrCanceledCompanyInPageNocount(status,
					handicap, level, accoutIdArray, member, fromMoney, toMoney, startTime, endTime, orderNo,
					sysUser1.getId(), pageRequest);
			if (page != null && page.getContent() != null && page.getContent().size() > 0) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue());
				List<Map<String, Object>> list = new LinkedList<>();
				for (BizIncomeRequest bizIncomeRequest : page.getContent()) {
					if (ArrayUtils.contains(accoutIdArray, bizIncomeRequest.getToId())) {
						Map<String, Object> map1 = new HashMap<>();
						map1.put("id", bizIncomeRequest.getId());
						BizAccount bizAccount = accountService.getById(bizIncomeRequest.getToId());
						if (bizAccount != null) {
							map1.put("receiver",
									StringUtils.isNotBlank(bizAccount.getOwner()) ? bizAccount.getOwner() : "");
							map1.put("receiverBank",
									StringUtils.isNotBlank(bizAccount.getBankName()) ? bizAccount.getBankName() : "");
							map1.put("toAccountNo",
									StringUtils.isNotBlank(bizAccount.getAccount()) ? bizAccount.getAccount() : "");
							map1.put("toAccountId", bizAccount.getId());
						}
						map1.put("level", bizIncomeRequest.getLevel());
						map1.put("handicap", bizIncomeRequest.getHandicap());
						map1.put("operator", sysUser1.getUid());// 当前人只能看自己的操作记录,所以操作人必定是当前人
						Long timeConsume = 0L;
						if ("matched".equals(type)) {
							BizTransactionLog bizTransactionLog = transactionLogService
									.findByReqId(bizIncomeRequest.getId());
							map1.put("remark",
									bizTransactionLog != null && StringUtils.isNotBlank(bizTransactionLog.getRemark())
											? bizTransactionLog.getRemark().replace("\r\n", "<br>").replace("\n",
													"<br>")
											: "");
							if (bizTransactionLog != null && bizTransactionLog.getCreateTime() != null) {
								timeConsume = bizTransactionLog.getCreateTime().getTime()
										- bizIncomeRequest.getCreateTime().getTime();
							}
						}
						if ("canceled".equals(type)) {
							map1.put("remark",
									StringUtils.isNotBlank(bizIncomeRequest.getRemark())
											? bizIncomeRequest.getRemark().replace("\r\n", "<br>").replace("\n", "<br>")
											: "");
						}

						if (timeConsume != null && timeConsume.intValue() >= 0) {
							map1.put("timeConsume", CommonUtils.convertTime2String(timeConsume));
						} else {
							map1.put("timeConsume", "");// 匹配时间-提单时间
						}
						map1.put("matchedTime",
								bizIncomeRequest.getUpdateTime() != null ? bizIncomeRequest.getUpdateTime().getTime()
										: "");

						map1.put("member",
								StringUtils.isNotBlank(bizIncomeRequest.getMemberUserName())
										? bizIncomeRequest.getMemberUserName()
										: "");
						map1.put("payer",
								StringUtils.isNotBlank(bizIncomeRequest.getMemberRealName())
										? bizIncomeRequest.getMemberRealName()
										: "");
						map1.put("amount", bizIncomeRequest.getAmount() != null ? bizIncomeRequest.getAmount() : "");
						map1.put("orderTime",
								bizIncomeRequest.getCreateTime() != null ? bizIncomeRequest.getCreateTime() : "");
						map1.put("tradeTime", "");
						map1.put("orderNo",
								StringUtils.isNotBlank(bizIncomeRequest.getOrderNo()) ? bizIncomeRequest.getOrderNo()
										: "");

						map1.put("balanceGap", "");
						map1.put("bankAmount", "");

						if (bizIncomeRequest.getStatus() != null) {
							String statuss = bizIncomeRequest.getStatus()
									.equals(IncomeRequestStatus.Matched.getStatus())
											? IncomeRequestStatus.Matched.getMsg()
											: bizIncomeRequest.getStatus()
													.equals(IncomeRequestStatus.Canceled.getStatus())
															? IncomeRequestStatus.Canceled.getMsg()
															: "";
							map1.put("status", statuss);
						}
						list.add(map1);
					}
				}
				map.put("list", list);
				responseData.setData(map);
				responseData.setPage(new Paging());
			} else {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "无记录");
			}
		} catch (Exception e) {
			logger.error("查询已匹配记录失败：" + e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "服务异常，请稍后...");
		}
		return mapper.writeValueAsString(responseData);
	}

	/***
	 * 查询已匹配或者已取消 总金额
	 */
	@RequestMapping("/getCompanyInSum")
	public String getCompanyInSum(@RequestParam(value = "type") String type,
			@RequestParam(value = "accountIds") Integer[] accountIdsArray,
			@RequestParam(value = "handicap") Integer[] handicap, @RequestParam(value = "level") Integer level,
			@RequestParam(value = "account") Integer accountId, @RequestParam(value = "member") String member,
			@RequestParam(value = "fromMoney") BigDecimal fromMoney,
			@RequestParam(value = "toMoney") BigDecimal toMoney, @RequestParam(value = "startTime") String startTime,
			@RequestParam(value = "endTime") String endTime, @RequestParam(value = "orderNo") String orderNo)
			throws JsonProcessingException {
		SysUser sysUser1 = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser1 == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "获取总金额成功");
		Map<String, Object> map = new LinkedHashMap<>();
		Integer status = getStatus(type);
		Integer[] accoutIdArray = accountIdsArray;
		if (accountId != null) {
			accoutIdArray = new Integer[] { accountId };
		}
		try {
			String sum = incomeRequestService.findMatchedOrCanceledCompanyInSum(status, handicap, level, accoutIdArray,
					member, fromMoney, toMoney, startTime, endTime, orderNo, sysUser1.getId());
			map.put("sumAmount", sum);
			responseData.setData(map);
		} catch (Exception e) {
			logger.error("查询已匹配/已取消总金额失败:{}", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "获取总金额失败");
		}

		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 查询已匹配或者已取消 总记录数 分页
	 */
	@RequestMapping("/getCompanyInCount")
	public String getCompanyInCount(@RequestParam(value = "type") String type,
			@RequestParam(value = "accountIds") Integer[] accountIdsArray,
			@RequestParam(value = "handicap") Integer[] handicap, @RequestParam(value = "level") Integer level,
			@RequestParam(value = "account") Integer accountId, @RequestParam(value = "member") String member,
			@RequestParam(value = "fromMoney") BigDecimal fromMoney,
			@RequestParam(value = "toMoney") BigDecimal toMoney, @RequestParam(value = "startTime") String startTime,
			@RequestParam(value = "endTime") String endTime, @RequestParam(value = "orderNo") String orderNo,
			@RequestParam(value = "pageNo") Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		GeneralResponseData<Map<String, Object>> responseData;
		SysUser sysUser1 = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser1 == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		Integer status = getStatus(type);
		Integer[] accoutIdArray = accountIdsArray;
		if (accountId != null) {
			accoutIdArray = new Integer[] { accountId };
		}
		try {
			String count = incomeRequestService.findMatchedOrCanceledCompanyInCount(status, handicap, level,
					accoutIdArray, member, fromMoney, toMoney, startTime, endTime, orderNo, sysUser1.getId());
			Paging page2;
			if (StringUtils.isNotBlank(count) && Integer.parseInt(count) > 0) {
				page2 = CommonUtils.getPage(pageNo + 1, pageSize != null ? pageSize : AppConstants.PAGE_SIZE, count);
			} else {
				page2 = CommonUtils.getPage(0, pageSize != null ? pageSize : AppConstants.PAGE_SIZE, "0");
			}
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			responseData.setPage(page2);
		} catch (Exception e) {
			logger.error("获取总记录失败：{}", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "获取总记录失败");
		}
		return mapper.writeValueAsString(responseData);
	}

	private Integer[] accoutIdArray(List<Integer> list, Integer[] accoutIdArray, Integer[] accountId) {
		if (list != null && list.size() > 0) {
			accoutIdArray = new Integer[list.size() + 1];
			accoutIdArray[0] = accountId[0];
			for (int i = 0, L = list.size(); i < L; i++) {
				accoutIdArray[i + 1] = list.get(i);
			}
		}
		return accoutIdArray;
	}

	/**
	 * 查询公司入款 系统提单 未匹配 accountId 数组 如果长度为1 则是根据当前页签的账号查询 如果长度大于1 则是根据同一个层级的账号来查询记录
	 */
	@RequestMapping(value = "/search")
	public String search(IncomeMatchingInputDTO inputDTO) throws JsonProcessingException {
		GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		Map<String, Object> map = new LinkedHashMap<>();
		Sort sort = new Sort(Sort.Direction.DESC, "createTime");
		PageRequest pageRequest = new PageRequest(inputDTO.getPageNo(), AppConstants.PAGE_SIZE / 2, sort);
		try {
			Integer[] accoutIdArray = inputDTO.getAccountId();
			// 前端去除了选中某一行流水取查询同一层级下的账号提单 现在默认查询同一层级下该账号同一层级下暂停 冻结的账号提单
			List<Integer> list = null;// 同一层级的冻结 暂停的 账号
			if (accoutIdArray != null && accoutIdArray.length == 1) {
				BizAccount bizAccount = accountService.getById(accoutIdArray[0]);
				if (bizAccount.getStatus().equals(AccountStatus.Normal.getStatus())) {
					// 正常的账号才扩展查询同层级的其他异常账号
					list = accountService.findAccountIdInSameLevel(accoutIdArray[0]);
				}
			}
			accoutIdArray = accoutIdArray(list, accoutIdArray, accoutIdArray);
			Page<BizIncomeRequest> pageSysOrder = incomeRequestService.findCompanyInNoCount(inputDTO.getPayMan(),
					IncomeRequestStatus.Matching.getStatus(), accoutIdArray, inputDTO.getLevel(), inputDTO.getOrderNo(),
					inputDTO.getMember(),
					StringUtils.isNotBlank(inputDTO.getFromMoney())
							? new BigDecimal(StringUtils.trim(inputDTO.getFromMoney()))
							: null,
					StringUtils.isNotBlank(inputDTO.getToMoney())
							? new BigDecimal(StringUtils.trim(inputDTO.getToMoney()))
							: null,
					StringUtils.trim(inputDTO.getStartTime()), StringUtils.trim(inputDTO.getEndTime()), pageRequest);
			if (pageSysOrder != null && pageSysOrder.getContent() != null && pageSysOrder.getContent().size() > 0) {
				List<BizIncomeRequest> bizIncomeRequestList = pageSysOrder.getContent();
				if (bizIncomeRequestList != null && bizIncomeRequestList.size() > 0) {
					List<Map<String, Object>> mapList = new LinkedList<>();
					for (int i = 0, L = bizIncomeRequestList.size(); i < L; i++) {
						if (Arrays.asList(accoutIdArray).contains(bizIncomeRequestList.get(i).getToId())) {
							Map<String, Object> map1 = new LinkedHashMap<>();
							map1.put("id", bizIncomeRequestList.get(i).getId());
							map1.put("toId", bizIncomeRequestList.get(i).getToId());
							map1.put("handicap",
									bizIncomeRequestList.get(i).getHandicap() != null
											? bizIncomeRequestList.get(i).getHandicap()
											: "");// 盘口id
							map1.put("level", bizIncomeRequestList.get(i).getLevel());// 层级id
							map1.put("member", bizIncomeRequestList.get(i).getMemberUserName());
							map1.put("memberCode", bizIncomeRequestList.get(i).getMemberCode());
							map1.put("realName", bizIncomeRequestList.get(i).getMemberRealName());
							map1.put("amount", bizIncomeRequestList.get(i).getAmount());
							map1.put("orderNo", bizIncomeRequestList.get(i).getOrderNo());
							map1.put("createTime", bizIncomeRequestList.get(i).getCreateTime());
							map1.put("updateTime", bizIncomeRequestList.get(i).getUpdateTime());
							if (!inputDTO.getAccountId()[0].equals(bizIncomeRequestList.get(i).getToId())) {
								BizAccount bizAccount = accountService.getById(bizIncomeRequestList.get(i).getToId());
								String account = new StringBuilder(bizAccount.getAccount().substring(0, 5)).append("**")
										.append(bizAccount.getAccount().substring(bizAccount.getAccount().length() - 4))
										.toString();
								map1.put("remark",
										"同层级账号：'" + account + "|" + bizAccount.getOwner() + "'  的提单<br>"
												+ (StringUtils.isNotBlank(bizIncomeRequestList.get(i).getRemark())
														? bizIncomeRequestList.get(i).getRemark()
																.replace("\r\n", "<br>").replace("\n", "<br>")
														: ""));
							} else {
								map1.put("remark", StringUtils.isNotBlank(bizIncomeRequestList.get(i).getRemark())
										? bizIncomeRequestList.get(i).getRemark().replace("\r\n", "<br>").replace("\n",
												"<br>")
										: "");
							}
							mapList.add(map1);
						}
					}
					map.put("sysOrderList", mapList);
				}
				responseData.setData(map);
				responseData.setPage(new Paging(pageSysOrder));
			} else {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "无记录");
				responseData.setData(null);
			}
		} catch (Exception e) {
			logger.error("查询系统提单失败：", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"服务异常，请稍后..." + e.getStackTrace());
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 获取正在匹配公司入款总金额
	 */
	@RequestMapping(value = "/getMatchingCompanyInSum")
	public String getCompanyInSum(IncomeMatchingInputDTO inputDTO) throws JsonProcessingException {
		GeneralResponseData<String> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		Integer[] accoutIdArray = inputDTO.getAccountId();
		// 前端去除了选中某一行流水取查询同一层级下的账号提单 现在默认查询同一层级下该账号同一层级下暂停 冻结的账号提单
		List<Integer> list = null;// 同一层级的冻结 暂停的 账号
		if (accoutIdArray != null && accoutIdArray.length == 1) {
			BizAccount bizAccount = accountService.getById(accoutIdArray[0]);
			if (bizAccount.getStatus().equals(AccountStatus.Normal.getStatus())) {
				// 正常的账号才扩展查询同层级的其他异常账号
				list = accountService.findAccountIdInSameLevel(accoutIdArray[0]);
			}
		}
		accoutIdArray = accoutIdArray(list, accoutIdArray, accoutIdArray);
		String sum = null;
		try {
			sum = incomeRequestService.findMatchingInComeRequestSum(inputDTO.getPayMan(),
					IncomeRequestStatus.Matching.getStatus(), accoutIdArray, inputDTO.getLevel(), inputDTO.getOrderNo(),
					inputDTO.getMember(), inputDTO.getFromMoney(), inputDTO.getToMoney(), inputDTO.getStartTime(),
					inputDTO.getEndTime());
		} catch (Exception e) {
			logger.error("查询提单总金额 失败: ", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询提单总金额失败:" + e.getStackTrace());
		}
		responseData.setData(sum);
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 获取公司入款正在匹配的总记录数
	 */
	@RequestMapping(value = "/getCompanyInMatchingCount")
	public String getCompanyInCount(IncomeMatchingInputDTO inputDTO) throws JsonProcessingException {
		GeneralResponseData<String> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		Sort sort = new Sort(Sort.Direction.DESC, "id");// createTime
		PageRequest pageRequest = new PageRequest(inputDTO.getPageNo(), AppConstants.PAGE_SIZE / 2, sort);
		Integer[] accoutIdArray = inputDTO.getAccountId();
		// 前端去除了选中某一行流水取查询同一层级下的账号提单 现在默认查询同一层级下该账号同一层级下暂停 冻结的账号提单
		List<Integer> list = null;// 同一层级的冻结 暂停的 账号
		if (accoutIdArray != null && accoutIdArray.length == 1) {
			BizAccount bizAccount = accountService.getById(accoutIdArray[0]);
			if (bizAccount.getStatus().equals(AccountStatus.Normal.getStatus())) {
				// 正常的账号才扩展查询同层级的其他异常账号
				list = accountService.findAccountIdInSameLevel(accoutIdArray[0]);
			}
		}
		accoutIdArray = accoutIdArray(list, accoutIdArray, accoutIdArray);
		try {
			String count = incomeRequestService.findMatchingRequestCount(inputDTO.getPayMan(),
					IncomeRequestStatus.Matching.getStatus(), accoutIdArray, inputDTO.getLevel(), inputDTO.getOrderNo(),
					inputDTO.getMember(), inputDTO.getFromMoney(), inputDTO.getToMoney(), inputDTO.getStartTime(),
					inputDTO.getEndTime(), pageRequest);
			Paging page;
			if (StringUtils.isNotBlank(count) && Integer.valueOf(count) > 0) {
				page = CommonUtils.getPage(inputDTO.getPageNo() + 1, AppConstants.PAGE_SIZE / 2, count);
			} else {
				page = CommonUtils.getPage(0, AppConstants.PAGE_SIZE / 2, "0");
			}
			responseData.setPage(page);
		} catch (Exception e) {
			logger.error("查询公司入款总数失败：", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询提单总记录失败:" + e.getStackTrace());
		}
		return mapper.writeValueAsString(responseData);
	}

	private String remark(String remark, String name, String localHostIp, SysUser sysUser) {
		if (StringUtils.isNotBlank(StringUtils.trim(remark))) {
			if (StringUtils.isBlank(name)) {
				name = "ATM存款补单";
				if (StringUtils.isNotBlank(localHostIp)) {
					remark = new StringBuilder().append(CommonUtils.getNowDate()).append(" ").append(sysUser.getUid())
							.append("(").append(localHostIp).append(")").append("金额不一致匹配:").append(remark).append(name)
							.toString();
				} else {
					remark = new StringBuilder().append(CommonUtils.getNowDate()).append(" ").append(sysUser.getUid())
							.append("金额不一致匹配:").append(remark).append(name).toString();
				}
			} else {
				if (StringUtils.isNotBlank(localHostIp)) {
					remark = new StringBuilder().append(CommonUtils.getNowDate()).append(" ").append(sysUser.getUid())
							.append("(").append(localHostIp).append(")").append("金额不一致匹配:").append(remark).append(name)
							.toString();
				} else {
					remark = new StringBuilder().append(CommonUtils.getNowDate()).append(" ").append(sysUser.getUid())
							.append("金额不一致匹配:").append(remark).append(name).toString();
				}
			}
		} else {
			if (StringUtils.isBlank(name)) {
				name = "ATM存款补单";
				if (StringUtils.isNotBlank(localHostIp)) {
					remark = new StringBuilder().append(CommonUtils.getNowDate()).append(" ").append(sysUser.getUid())
							.append("(").append(localHostIp).append(")").append("金额不一致匹配").append(name).toString();
				} else {
					remark = new StringBuilder().append(CommonUtils.getNowDate()).append(" ").append(sysUser.getUid())
							.append("金额不一致匹配").append(name).toString();
				}
			} else {
				if (StringUtils.isNotBlank(localHostIp)) {
					remark = new StringBuilder().append(CommonUtils.getNowDate()).append(" ").append(sysUser.getUid())
							.append("(").append(localHostIp).append(")").append("金额不一致匹配:").append(remark).append(name)
							.toString();
				} else {
					remark = new StringBuilder().append(CommonUtils.getNowDate()).append(" ").append(sysUser.getUid())
							.append("金额不一致匹配").append(name).toString();
				}
			}
		}
		return remark;
	}

	/**
	 * 金额不一致执行匹配 deposiType:存款方式 ,1网银转账，2ATM自动柜员机，3ATM现金入款，4银行柜台，5手机银行，10其他
	 */
	@RequiresPermissions(value = "IncomeAuditComp:MatchIncomeReq:*")
	@RequestMapping("/matchForInconsistentAmount")
	public String match(@RequestParam(value = "amount", required = false) String amount,
			@RequestParam(value = "type", required = false) Integer depositType,
			@RequestParam(value = "name", required = false) String name,
			@RequestParam(value = "memberAccount", required = false) String memberAccount,
			@RequestParam(value = "bankLogId", required = false) Long bankLogId,
			@RequestParam(value = "incomeReqId", required = false) Long incomeReqId,
			@RequestParam(value = "localHostIp", required = false) String localHostIp,
			@RequestParam(value = "accountNo", required = false) String accountNo,
			@RequestParam(value = "accountId", required = false) Integer accountId,
			@RequestParam(value = "remark", required = false) String remark) throws JsonProcessingException {
		GeneralResponseData<List<Map<String, Object>>> responseData;
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆！");
			return mapper.writeValueAsString(responseData);
		}
		logger.info(
				"金额不一致匹配参数,操作人:{},主机Ip:{},时间:{}, name:{},memberAccount:{},accountNo:{},accountId:{},type:{},remark:{},amount: {}",
				sysUser.getUid(), localHostIp, CommonUtils.getNowDate(), name, memberAccount, accountNo, accountId,
				depositType, remark, amount);
		if (StringUtils.isBlank(accountNo) || StringUtils.isBlank(memberAccount) || StringUtils.isBlank(amount)
				|| depositType == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请填写相关信息再提交！");
			return mapper.writeValueAsString(responseData);
		}
		if (new BigDecimal(amount).equals(BigDecimal.ZERO)) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "存款金额必须大于0！");
			return mapper.writeValueAsString(responseData);
		}
		// AccountBaseInfo accountBaseInfo =
		// accountService.getFromCacheById(accountId);
		BizBankLog bankLog = bankLogService.get(bankLogId);
		BizIncomeRequest incomeRequest = incomeRequestService.get(incomeReqId);
		if (bankLog == null || incomeRequest == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "记录不存在,无法匹配！");
			return mapper.writeValueAsString(responseData);
		}

		String remarkWrap = remark(remark, name, localHostIp, sysUser);
		// 金额不一致的以流水金额为准，先补提单获取新单子之后，自动匹配，然后取消原订单。
		// 锁住该条订单和流水,其他人不能做匹配操作
		if (bankLog != null && incomeRequest != null) {
			if (!Objects.equals(bankLog.getStatus(), BankLogStatus.Matching.getStatus())) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "流水已处理"));
			}
			// 以流水的入款账号为准,防止前端传入的accountId与流水的入款账号id不一致，导致盘口不一致
			AccountBaseInfo base = accountService.getFromCacheById(bankLog.getFromAccount());
			if (Objects.nonNull(base) && Objects.nonNull(base.getHandicapId())
					&& Objects.equals(base.getType(), AccountType.InBank.getTypeId())
					&& !Objects.equals(base.getHandicapId(), incomeRequest.getHandicap())) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "提单盘口与流水盘口不一致"));
			}
			BizHandicap bizHandicap = handicapService.findFromCacheById(base.getHandicapId());
			if (bizHandicap == null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"入款账号对应盘口信息不存在！");
				return mapper.writeValueAsString(responseData);
			}
			RedisDistributedLockUtils lockUtils = new RedisDistributedLockUtils(
					new StringBuilder("matchForInconsistentAmount_").append(String.valueOf(bankLog.getId())).append("_")
							.append(incomeRequest.getId()).toString(),
					1000, 5000);
			try {
				if (lockUtils != null && lockUtils.acquireLock()) {
					if (!incomeRequest.getStatus().equals(IncomeRequestStatus.Matching.getStatus())) {
						return mapper.writeValueAsString(new GeneralResponseData<>(
								GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "匹配成功!"));
					}
					boolean flag = incomeRequestService.matchForInconsistentAmount(bizHandicap, name, memberAccount,
							amount, base, depositType, remark, remarkWrap, bankLog, incomeRequest, sysUser);
					if (flag) {
						return mapper.writeValueAsString(new GeneralResponseData<>(
								GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "匹配成功!"));
					} else {
						return mapper.writeValueAsString(
								new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "匹配失败!"));
					}
				} else {
					return mapper.writeValueAsString(new GeneralResponseData<>(
							GeneralResponseData.ResponseStatus.FAIL.getValue(), "此单正在匹配，您无法操作"));
				}
			} catch (Exception e) {
				logger.info("金额不一致匹配失败:{},参数:orderNo:{},amount:{},flowId:{},incomeReqId:{}", e,
						incomeRequest.getOrderNo(), bankLog.getAmount(), bankLogId, incomeReqId);
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "匹配失败"));
			} finally {
				if (lockUtils != null) {
					lockUtils.releaseLock();
				}
			}
		} else {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "匹配数据不存在"));
		}
	}

	/**
	 * 金额一致执行匹配操作
	 *
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/match")
	public String match(@RequestParam(value = "flowId", required = false) Long flowId,
			@RequestParam(value = "incomeReqId", required = false) Long incomeReqId,
			@RequestParam(value = "remark", required = false) String remark,
			@RequestParam(value = "orderNo", required = false) String orderNo) throws JsonProcessingException {
		try {
			SysUser loginUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (loginUser == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆！"));

			}
			BizIncomeRequest incomeRequest = incomeRequestService.findById(incomeReqId);
			BizBankLog bankLog = bankLogService.get(flowId);
			if (incomeRequest.getType().equals(IncomeRequestType.RebateLimit.getType())) {
				long nd = 1000 * 24 * 60 * 60;
				long nh = 1000 * 60 * 60;
				// 提额单手动匹配，如果提额单超过13个小时则不允许匹配，存在返利网13个小时后 自动失败，就算匹配返利网也加不上去
				long hour = (System.currentTimeMillis() - incomeRequest.getCreateTime().getTime()) % nd / nh;
				if (hour >= 13) {
					return mapper.writeValueAsString(new GeneralResponseData<>(
							GeneralResponseData.ResponseStatus.FAIL.getValue(), "13小时后不能匹配!"));
				} else {
					rebateApiService.ackCreditLimit(bankLog, incomeRequest);
					// 更改为匹配状态、调用提额的接口
					bankLog.setTaskId(incomeRequest.getId());
					bankLog.setTaskType(SysBalTrans.TASK_TYPE_INNER);
					bankLog.setOrderNo(incomeRequest.getOrderNo());
					bankLogService.updateStatusRm(bankLog.getId(), BankLogStatus.Matched.getStatus(),
							loginUser.getUid() + ":兼职提额流水");
					incomeRequestService.updateStatusById(incomeRequest.getId(), OutwardTaskStatus.Matched.getStatus());
					return mapper.writeValueAsString(
							new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "匹配成功"));
				}

			} else {
				if (incomeRequest == null || bankLog == null) {
					return mapper.writeValueAsString(
							new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "信息异常，匹配失败"));
				} else {
					if (Objects.nonNull(incomeRequest.getHandicap())) {
						AccountBaseInfo base = accountService.getFromCacheById(bankLog.getFromAccount());
						if (Objects.nonNull(base) && Objects.nonNull(base.getHandicapId())
								&& Objects.equals(base.getType(), AccountType.InBank.getTypeId())
								&& !Objects.equals(base.getHandicapId(), incomeRequest.getHandicap())) {
							return mapper.writeValueAsString(new GeneralResponseData<>(
									GeneralResponseData.ResponseStatus.FAIL.getValue(), "提单盘口与流水盘口不一致"));
						}
					}
					if (null == incomeRequest.getStatus()
							|| incomeRequest.getStatus() != IncomeRequestStatus.Matching.getStatus()) {
						return mapper.writeValueAsString(new GeneralResponseData<>(
								GeneralResponseData.ResponseStatus.FAIL.getValue(), "入款记录无法匹配，当前状态："
										+ IncomeRequestStatus.findByStatus(incomeRequest.getStatus()).getMsg()));
					}
					// 未认领不存在数据库中，数据库只有已匹配与匹配中
					if (null == bankLog.getStatus() || bankLog.getStatus() != BankLogStatus.Matching.getStatus()) {
						return mapper.writeValueAsString(
								new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
										"银行流水无法匹配，当前状态：" + BankLogStatus.findByStatus(bankLog.getStatus()).getMsg()));
					}
				}
				String remarkWrap;
				if (bankLog.getFromAccount() != incomeRequest.getToId()) {
					// 同层级账号匹配
					StringBuilder sb = new StringBuilder();
					sb.append(CommonUtils.getNowDate()).append("  ").append(loginUser.getUsername()).append("\r\n");
					sb.append("同层级账号匹配:提单号:").append(incomeRequest.getOrderNo()).append(",流水金额:")
							.append(bankLog.getAmount()).append(",流水抓取时间:")
							.append(CommonUtils.getDateFormat2Str(bankLog.getCreateTime())).append("!\r\n")
							.append(remark);
					remarkWrap = sb.toString();
				} else {
					StringBuilder sb = new StringBuilder();
					sb.append(CommonUtils.getNowDate()).append("  ").append(loginUser.getUsername()).append("\r\n")
							.append(remark);
					remarkWrap = sb.toString();
				}
				incomeRequestService.match(bankLog, incomeRequest, remark, remarkWrap, loginUser);
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "匹配成功"));
			}
		} catch (Exception e) {
			logger.error("匹配失败：{},匹配操作，参数：订单号：orderNo:{} ,流水ID:flowId:{},请求id：incomeReqId:{}", e, orderNo, flowId,
					incomeReqId);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "匹配失败" + e + e.getLocalizedMessage()));
		}
	}

	/**
	 * 取消操作 隐藏操作 强制入款（ 针对 第三方入款的） 开启取消操作
	 */
	@RequiresPermissions(value = { "IncomeAuditComp:AuditorConceal:*",
			"IncomeAuditComp:CancelIncomeReq:*" }, logical = Logical.OR)
	@RequestMapping("/reject2Platform")
	public String reject2Platform(@RequestParam(value = "accountId", required = false) Integer accountId,
			@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "incomeRequestId") Long incomeRequestId,
			@RequestParam(value = "remark", required = false) String remark,
			@RequestParam(value = "orderNo", required = false) String orderNo,
			@RequestParam(value = "handicapId", required = false) Integer handicap,
			@RequestParam(value = "memberCode", required = false) String memberCode) throws JsonProcessingException {
		GeneralResponseData<String> responseData;
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		try {
			if (incomeRequestId == null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败");
				return mapper.writeValueAsString(responseData);
			}
			BizIncomeRequest bizIncomeRequest = incomeRequestService.findById(incomeRequestId);
			if (bizIncomeRequest == null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"订单不存在,无法操作");
				return mapper.writeValueAsString(responseData);
			}
			if (StringUtils.isNotBlank(type) && type.equals("compelIn")) {
				// 强制入款--第三方入款的
				bizIncomeRequest.setStatus(IncomeRequestStatus.Matched.getStatus());
				bizIncomeRequest.setUpdateTime(new Date());
				incomeRequestService.save(bizIncomeRequest, true);
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功");
				return mapper.writeValueAsString(responseData);
			} else if (StringUtils.isNotBlank(type) && "cancel".equals(type)) {
				if (StringUtils.isBlank(remark)) {
					responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
							"请填写备注!");
					return mapper.writeValueAsString(responseData);
				}
				// 取消操作订单号必定存在
				if (!bizIncomeRequest.getStatus().equals(IncomeRequestStatus.Matching.getStatus())) {
					responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
							"订单已取消,请刷新!");
					return mapper.writeValueAsString(responseData);
				}
				if (handicap == null) {
					handicap = bizIncomeRequest.getHandicap();
				}
				boolean flag = incomeRequestService.cancelAndCallFlatform(IncomeRequestStatus.Canceled.getStatus(),
						incomeRequestId, remark, orderNo, handicap, accountId, memberCode, sysUser);
				if (flag) {
					// 新公司入款取消订单之后释放金额
					AccountBaseInfo accountBaseInfo = accountService.getFromCacheById(bizIncomeRequest.getToId());
					if (CommonUtils.checkNewInComeEnabled(accountBaseInfo.getHandicapId())) {
						ysfService.recycleRandNum(accountBaseInfo.getAccount(), bizIncomeRequest.getAmount());
					}
					responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
							"操作成功!");
				} else {
					responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
							"取消失败!");
				}
				return mapper.writeValueAsString(responseData);
			} else {
				// 隐藏操作 只需要accoutid requestId
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(new Date());
				calendar.set(Calendar.HOUR_OF_DAY, 0);
				calendar.set(Calendar.MINUTE, new Random().nextInt(10));
				calendar.set(Calendar.SECOND, new Random().nextInt(59));
				Date zero = calendar.getTime();
				bizIncomeRequest.setCreateTime(zero);// 是否操作updateTime该字段
				incomeRequestService.update(bizIncomeRequest);
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功");
				return mapper.writeValueAsString(responseData);
			}
		} catch (Exception e) {
			logger.error("操作失败，异常:{},调用取消/隐藏操作结束,参数:order {},incomeReqId:{}", e, orderNo, incomeRequestId);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败");
			return mapper.writeValueAsString(responseData);
		}
	}

	private void dealReject(BizIncomeRequest incomeRequest, Integer accountId, String remark, SysUser loginUser) {
		incomeRequest = incomeRequestService.reject2CurrSys(incomeRequest.getId(), remark, loginUser);
		systemAccountManager.cancel(incomeRequest);
		accountService.deleteDrawAmountAndFee(incomeRequest.getToId());
		// 打回 添加系统余额
		BizAccount account = accountService.getById(incomeRequest.getFromId());
		if (null != account && AccountType.InThird.getTypeId().equals(account.getType())) {
			BigDecimal fee = Objects.nonNull(incomeRequest.getFee()) ? incomeRequest.getFee().abs() : BigDecimal.ZERO;
			BigDecimal amt = Objects.nonNull(incomeRequest.getAmount()) ? incomeRequest.getAmount().abs()
					: BigDecimal.ZERO;
			logger.info("1 打回 第三方账号 id: {}  frId:{} toId: {} amt: {} fee: {} ", incomeRequest.getId(),
					incomeRequest.getFromId(), incomeRequest.getToId(), amt, fee);
		} else {
			logger.info("第三方账号不存在 id:{}", accountId);
		}
		accountClickService.addClickLog(accountId, "点击【下发失败】");
		Date updateTime = new Date();
		Long timeConSuming = (updateTime.getTime() - incomeRequest.getCreateTime().getTime()) / 1000;
		incomeRequestService.updateTimeconsumingAndUpdateTime(incomeRequest.getId(), updateTime, timeConSuming);
		// 刷新缓存
		accountController.refreshCache(loginUser.getId());
		// 删除 提现时间 2019-09-12 放开
		accountService.saveDrawTime(accountId, false);
	}

	/**
	 * 转平台通知 <br>
	 * newManner =1 表示 下发任务里 点击下发失败 <br>
	 * (thirdAccountId 下发的第三方账号id accountId 出款卡或者下发卡的id)
	 */
	@RequestMapping("/reject2CurrSys")
	public String reject2CurrSys(@RequestParam(value = "incomeRequestId", required = false) Long incomeRequestId,
			@RequestParam(value = "remark", required = false) String remark,
			@RequestParam(value = "thirdAccountId", required = false) Integer thirdAccountId,
			@RequestParam(value = "accountId", required = false) Integer accountId,
			@RequestParam(value = "amount", required = false) Integer amount,
			@RequestParam(value = "newManner", required = false) Byte newManner) throws JsonProcessingException {
		try {
			SysUser loginUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (loginUser == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆！"));

			}
			boolean isNewManner = (null != newManner && newManner.intValue() == 1);
			if (isNewManner) {
				logger.info("点击下发失败 参数: thirdAccountID:{} accountID:{},amount:{} ", thirdAccountId, accountId, amount);
				if (null == accountId) {
					return mapper.writeValueAsString(new GeneralResponseData<>(
							GeneralResponseData.ResponseStatus.FAIL.getValue(), " 下发账号id必传！"));
				}
				BizIncomeRequest incomeRequest = incomeRequestService.findOneThirdDrawRecord(accountId, amount);
				logger.info("查询的下发记录:{}", incomeRequest);
				if (null == incomeRequest) {
					return mapper.writeValueAsString(
							new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "没有下发记录！"));
				}
				dealReject(incomeRequest, accountId, remark, loginUser);
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功!"));
			} else {
				BizIncomeRequest incomeRequest = incomeRequestService.findById(incomeRequestId);
				// 下发任务里 点击下发失败 =打回 或者 在下发卡里的小圆圈打回
				// 所有正在下发未到账的Id
				List<Integer> allDrawingIds = accountService.allDrawingIds();
				boolean isDrawByNewManner = incomeRequest != null && allDrawingIds.contains(incomeRequest.getToId());
				if (isDrawByNewManner) {
					logger.info("小圆圈里打回! 参数: incomeRequestId {} ,accountId {} ", incomeRequestId, accountId);
					dealReject(incomeRequest, incomeRequest.getToId(), remark, loginUser);
					return mapper.writeValueAsString(
							new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功!"));
				}

				// 第三方入款 提现里 打回
				BizIncomeRequest inReq = incomeRequestService.reject2CurrSys(incomeRequestId, remark, loginUser);

				systemAccountManager.cancel(inReq);
				// 打回 则删除 待确认hash里的记录
				// 把原来的锁定目标 解锁
				String thirdAccountNano = accountService.getFromIdWithNanoFromLockedHash(inReq.getToId());
				logger.info("打回 删除  锁定的第三方记录:{}", thirdAccountNano);
				if (StringUtils.isNotBlank(thirdAccountNano)) {
					boolean unlocked = allocateTransService.unLockForThirdDrawToOutCard(thirdAccountNano,
							inReq.getToId());
					logger.info("打回 删除 llockUpdStatus 参数:toId:{} fromId加时间:{}, 结果:{}", inReq.getToId(),
							thirdAccountNano, unlocked);
					accountService.removeLockedHash(inReq.getToId());
					// accountService.deleteDrawAmountAndFee(inReq.getToId());
				}
				accountService.removeBySystem(inReq.getToId());
				BizAccount account = accountService.getById(inReq.getFromId());
				// 只有fromId是第三方账号 才能加回系统余额
				if (null != account && AccountType.InThird.getTypeId().equals(account.getType())) {
					BigDecimal fee = Objects.nonNull(inReq.getFee()) ? inReq.getFee().abs() : BigDecimal.ZERO;
					BigDecimal amt = Objects.nonNull(inReq.getAmount()) ? inReq.getAmount().abs() : BigDecimal.ZERO;
					logger.info("打回 第三方账号 id: {}  frId:{} toId: {} amt: {} fee: {} ", inReq.getId(), inReq.getFromId(),
							inReq.getToId(), amt, fee);
				} else {
					logger.debug("第三方账号不存在 id:{}", accountId);
				}
				// 删除 未匹配的缓存
				accountService.deleteUnfinishedDrawInThirdByMatched(inReq.getToId());
				// 打回保存 耗时
				if (inReq.getTimeconsume() == null) {
					Date updateTime = new Date();
					Long timeConSuming = (updateTime.getTime() - inReq.getCreateTime().getTime()) / 1000;
					incomeRequestService.updateTimeconsumingAndUpdateTime(inReq.getId(), updateTime, timeConSuming);
				}
			}
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			logger.error("转平台失败：{}，参数：incomeRequestId：{}", e, incomeRequestId);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 通过id 查询入款请求
	 */
	@RequestMapping("/findbyid")
	public String findById(@RequestParam(value = "id") Long id) throws JsonProcessingException {
		try {
			GeneralResponseData<BizIncomeRequest> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			BizIncomeRequest incomeInfo = incomeRequestService.get(id);
			// 提额单不存在fromid
			BizAccount acc = accountService.getById(null == incomeInfo.getFromId() ? 0 : incomeInfo.getFromId());
			incomeInfo.setFromAccount(null == acc ? "" : acc.getAccount());
			responseData.setData(incomeInfo);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error("操作失败：{},方法：{}，参数：{}", e, request.getMethod(), id);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 统一查询接口 "from",则from为eq "to"则to为eq
	 */
	@RequestMapping("/findbyvo")
	public String findByVO(@RequestParam(value = "pageNo") int pageNo, @Valid BizIncomeRequest bizIncomeRequest,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "minAmount", required = false) BigDecimal minAmount,
			@RequestParam(value = "maxAmount", required = false) BigDecimal maxAmount,
			@RequestParam(value = "levelId", required = false) Integer levelId,
			@RequestParam(value = "isCancel", required = false) Integer isCancel,
			@RequestParam(value = "statusArray", required = false) Integer[] statusArray,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray)
			throws JsonProcessingException {
		try {
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (Objects.isNull(sysUser)) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			GeneralResponseData<List<BizIncomeRequest>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			List<SearchFilter> filterToList = DynamicSpecifications.build(request);
			if (null != bizIncomeRequest.getId() && hasValue(bizIncomeRequest.getId().toString())) {
				filterToList.add(new SearchFilter("id", SearchFilter.Operator.EQ, bizIncomeRequest.getId()));
			}
			if (bizIncomeRequest.getFromId() != null) {
				filterToList.add(new SearchFilter("fromId", SearchFilter.Operator.EQ, bizIncomeRequest.getFromId()));
			}
			if (bizIncomeRequest.getToId() != null) {
				filterToList.add(new SearchFilter("toId", SearchFilter.Operator.EQ, bizIncomeRequest.getToId()));
			}
			if (StringUtils.isNotEmpty(bizIncomeRequest.getFromAccount())) {
				filterToList.add(
						new SearchFilter("fromAccount", SearchFilter.Operator.LIKE, bizIncomeRequest.getFromAccount()));
			}
			if (StringUtils.isNotEmpty(bizIncomeRequest.getToAccount())) {
				filterToList.add(
						new SearchFilter("toAccount", SearchFilter.Operator.LIKE, bizIncomeRequest.getToAccount()));
			}
			AccountBaseInfo toBase = accountService.getFromCacheById(bizIncomeRequest.getToId());
			if ((null != isCancel && isCancel == 1)
					|| (!Objects.equals(sysUser.getCategory(), UserCategory.ADMIN.getValue()) && Objects.nonNull(toBase)
							&& (Objects.equals(toBase.getType(), AccountType.InBank.getTypeId())
									|| Objects.equals(toBase.getType(), AccountType.InThird.getTypeId())
									|| Objects.equals(toBase.getType(), AccountType.InWechat.getTypeId())
									|| Objects.equals(toBase.getType(), AccountType.InAli.getTypeId())))) {
				List<BizHandicap> dataToList = dataPermissionService.getOnlyHandicapByUserId(sysUser);
				if (CollectionUtils.isEmpty(dataToList)) {
					throw new Exception("当前用户未配置盘口权限，请配置后再进行查询！");
				} else {
					if (Objects.isNull(bizIncomeRequest.getHandicap())) {
						List<Integer> handicapIdToList = new ArrayList<>();
						for (int i = 0; i < dataToList.size(); i++) {
							if (null != dataToList.get(i) && null != dataToList.get(i).getId()) {
								handicapIdToList.add(dataToList.get(i).getId());
							}
						}
						filterToList.add(
								new SearchFilter("handicap", SearchFilter.Operator.IN, handicapIdToList.toArray()));
					} else {
						filterToList.add(
								new SearchFilter("handicap", SearchFilter.Operator.EQ, bizIncomeRequest.getHandicap()));
					}
				}
			}
			if (null != bizIncomeRequest.getLevel()) {
				filterToList.add(new SearchFilter("level", SearchFilter.Operator.EQ, bizIncomeRequest.getLevel()));
			}
			if (null != statusArray && statusArray.length > 1) {
				filterToList.add(new SearchFilter("status", SearchFilter.Operator.IN, statusArray));
			} else if (null != statusArray && statusArray.length == 1) {
				filterToList.add(new SearchFilter("status", SearchFilter.Operator.EQ, statusArray[0]));
			} else if (null != bizIncomeRequest.getStatus()) {
				filterToList.add(new SearchFilter("status", SearchFilter.Operator.EQ, bizIncomeRequest.getStatus()));
			}
			// 金额区间值用
			if (null != minAmount) {
				filterToList.add(new SearchFilter("amount", SearchFilter.Operator.GTE, minAmount));
			}
			if (null != maxAmount) {
				filterToList.add(new SearchFilter("amount", SearchFilter.Operator.LTE, maxAmount));
			}
			if (null != bizIncomeRequest.getType()) {
				filterToList.add(new SearchFilter("type", SearchFilter.Operator.EQ, bizIncomeRequest.getType()));
			}
			if (hasValue(bizIncomeRequest.getOrderNo())) {
				filterToList.add(new SearchFilter("orderNo", SearchFilter.Operator.EQ, bizIncomeRequest.getOrderNo()));
			}
			if (hasValue(bizIncomeRequest.getMemberUserName())) {
				filterToList.add(new SearchFilter("memberUserName", SearchFilter.Operator.LIKE,
						bizIncomeRequest.getMemberUserName()));
			}
			Date[] startAndEndTime = CommonUtils.parseStartAndEndTime(startAndEndTimeToArray);
			if (startAndEndTime[0] != null) {
				filterToList.add(new SearchFilter("createTime", SearchFilter.Operator.GTE, startAndEndTime[0]));
			}
			if (startAndEndTime[1] != null) {
				filterToList.add(new SearchFilter("createTime", SearchFilter.Operator.LTE, startAndEndTime[1]));
			}
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					Sort.Direction.DESC, "createTime", "id");
			SearchFilter[] filterToArray = filterToList.toArray(new SearchFilter[filterToList.size()]);
			Specification<BizIncomeRequest> specif = DynamicSpecifications.build(BizIncomeRequest.class, filterToArray);
			Page<BizIncomeRequest> page = incomeRequestService.findAll(specif, pageRequest);
			Map<String, Object> header = buildHeader(filterToArray);
			responseData.setData(page.getContent());
			responseData.setPage(new Paging(page, header));
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error("方法：{}，操作失败：异常{}", request.getMethod(), e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 已匹配 入款系统单
	 *
	 * @param bizIncomeRequest
	 * @param pageNo
	 * @param pageSize
	 * @param minAmount
	 * @param maxAmount
	 * @param startAndEndTimeToArray
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/findmatchedbyvo")
	public String findMatchedByVO(@Valid BizIncomeRequest bizIncomeRequest, @RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "minAmount", required = false) BigDecimal minAmount,
			@RequestParam(value = "maxAmount", required = false) BigDecimal maxAmount,
			@RequestParam(value = "operatorType", required = false) String operatorType,
			@RequestParam(value = "handicapList", required = false) List<Integer> handicapList,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray)
			throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					Sort.Direction.DESC, "create_time");
			// 修改空字符串为null
			bizIncomeRequest.setOrderNo(changeNullFromEmpty(bizIncomeRequest.getOrderNo()));
			bizIncomeRequest.setToAccount(changeNullFromEmpty(bizIncomeRequest.getToAccount()));
			bizIncomeRequest.setMemberUserName(changeNullFromEmpty(bizIncomeRequest.getMemberUserName()));
			bizIncomeRequest.setOperatorUid(changeNullFromEmpty(bizIncomeRequest.getOperatorUid()));
			String manual = null, robot = null;
			if (null != operatorType) {
				if (operatorType.equals("manual")) {
					// 人工
					manual = "manual";
				} else if (operatorType.equals("robot")) {
					// 机器
					robot = "robot";
				}
			}
			if (startAndEndTimeToArray == null || startAndEndTimeToArray.length <= 0) {
				startAndEndTimeToArray = new String[] { null, null };
			}
			// 调用SQL
			Map<String, Object> map = incomeRequestService.findMatchedBySQL(bizIncomeRequest.getMemberUserName(),
					startAndEndTimeToArray[0], startAndEndTimeToArray[1],
					minAmount == null ? new BigDecimal("0") : minAmount,
					maxAmount == null ? new BigDecimal("0") : maxAmount, bizIncomeRequest.getOrderNo(),
					bizIncomeRequest.getToAccount(), bizIncomeRequest.getOperatorUid(), manual, robot, handicapList,
					pageRequest);
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error("方法：{}，查询已匹配数据失败：异常{}", request.getMethod(), e.getLocalizedMessage());
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询已匹配数据失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 查询已匹配的入款银行卡的匹配信息
	 *
	 * @param bizIncomeRequest
	 * @param pageNo
	 * @param pageSize
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/findfastsearch")
	public String findFastSearch(@Valid BizIncomeRequest bizIncomeRequest, @RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray)
			throws JsonProcessingException {
		try {
			GeneralResponseData<List<BizIncomeRequest>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			List<SearchFilter> filterToList = DynamicSpecifications.build(request);
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			List<BizHandicap> dataToList = dataPermissionService.getOnlyHandicapByUserId(sysUser);
			List<Integer> handicapIdToList = new ArrayList<Integer>();
			if (CollectionUtils.isEmpty(dataToList)) {
				throw new Exception("当前用户未配置盘口权限，请配置后再进行查询！");
			} else {
				for (int i = 0; i < dataToList.size(); i++) {
					if (null != dataToList.get(i) && null != dataToList.get(i).getId()) {
						handicapIdToList.add(dataToList.get(i).getId());
					}
				}
				filterToList.add(new SearchFilter("handicap", SearchFilter.Operator.IN, handicapIdToList.toArray()));
			}
			if (hasValue(bizIncomeRequest.getOrderNo())) {
				filterToList.add(new SearchFilter("orderNo", SearchFilter.Operator.EQ, bizIncomeRequest.getOrderNo()));
			}
			if (hasValue(bizIncomeRequest.getMemberUserName())) {
				filterToList.add(new SearchFilter("memberUserName", SearchFilter.Operator.EQ,
						bizIncomeRequest.getMemberUserName()));
			}
			if (hasValue(bizIncomeRequest.getMemberRealName())) {
				filterToList.add(new SearchFilter("memberRealName", SearchFilter.Operator.LIKE,
						bizIncomeRequest.getMemberRealName()));
			}
			Date[] startAndEndTime = CommonUtils.parseStartAndEndTime(startAndEndTimeToArray);
			if (startAndEndTime[0] != null) {
				filterToList.add(new SearchFilter("createTime", SearchFilter.Operator.GTE, startAndEndTime[0]));
			}
			if (startAndEndTime[1] != null) {
				filterToList.add(new SearchFilter("createTime", SearchFilter.Operator.LTE, startAndEndTime[1]));
			}
			filterToList
					.add(new SearchFilter("type", SearchFilter.Operator.EQ, IncomeRequestType.PlatFromBank.getType()));
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					Sort.Direction.DESC, "createTime", "id");
			SearchFilter[] filterToArray = filterToList.toArray(new SearchFilter[filterToList.size()]);
			Specification<BizIncomeRequest> specif = DynamicSpecifications.build(BizIncomeRequest.class, filterToArray);
			Page<BizIncomeRequest> page = incomeRequestService.findAll(specif, pageRequest);
			Map<String, Object> header = buildHeader(filterToArray);
			responseData.setData(page.getContent());
			responseData.setPage(new Paging(page, header));
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error("方法：{}，操作失败：异常{}", request.getMethod(), e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	private Boolean hasValue(String temp) {
		return !(null == temp || StringUtils.isBlank(temp));
	}

	private Map<String, Object> buildHeader(SearchFilter[] filterToArray) {
		Map<String, Object> result = new HashMap<>();
		BigDecimal[] amountAndFee = incomeRequestService.findAmountAndFeeByTotal(filterToArray);
		result.put("totalAmount", amountAndFee[0]);
		result.put("totalFee", amountAndFee[1]);
		return result;
	}

	/**
	 * 保存停止接单原因
	 *
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/stoporder")
	public String SaveStoporder(@RequestParam(value = "remark", required = false) String remark,
			@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "localHostIp", required = false) String localHostIp) throws JsonProcessingException {
		GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		Map<String, Object> map = new LinkedHashMap<>();
		if (StringUtils.isBlank(type)) {
			return "";
		}
		try {
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (operator == null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆");
				return mapper.writeValueAsString(responseData);
			}
			logger.info("点击接单停单操作人:{},ip:{},type:{}", operator.getUid(), localHostIp, type);
			Map<String, Object> mapp = incomeRequestService.SaveStopOrder(remark, operator.getUid(), type);
			map.put("message", mapp.get("message"));
		} catch (Exception e) {
			logger.error("方法：{}，操作失败：异常{}", request.getMethod(), e);
			e.printStackTrace();
			map.put("message", "停止接单原因保存失败！");
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		}
		responseData.setData(map);
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 查询停止接单原因
	 *
	 * @param pageNo
	 * @param username
	 * @param type
	 * @param startAndEndTimeToArray
	 * @param pageSize
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/searstoporder")
	public String SearStopOrder(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "username", required = false) String username,
			@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
				Sort.Direction.DESC, "id");
		Map<String, Object> map = new LinkedHashMap<>();
		try {
			// 拼接时间戳查询数据条件
			String fristTime = null;
			String lastTime = null;
			if (0 != startAndEndTimeToArray.length && null != startAndEndTimeToArray) {
				fristTime = startAndEndTimeToArray[0];
				lastTime = startAndEndTimeToArray[1];
			}
			if ("".equals(username))
				username = null;
			if ("".equals(type))
				type = null;
			Map<String, Object> mapp = incomeRequestService.SearStopOrder(username, type, fristTime, lastTime,
					pageRequest);
			Page<Object> page = (Page<Object>) mapp.get("Page");
			List<Object> stopOrderList = page.getContent();
			List<SysStopOrder> arrlist = new ArrayList<SysStopOrder>();
			for (int i = 0; i < stopOrderList.size(); i++) {
				Object[] obj = (Object[]) stopOrderList.get(i);
				SysStopOrder SysStopOrder = new SysStopOrder();
				SysStopOrder.setId((int) obj[0]);
				SysStopOrder.setUsername((String) obj[1]);
				SysStopOrder.setCreatetime((String) obj[2]);
				SysStopOrder.setRemark((String) obj[3]);
				SysStopOrder.setType((String) obj[4]);
				arrlist.add(SysStopOrder);
			}
			map.put("arrlist", arrlist);
			map.put("page", new Paging(page));
		} catch (Exception e) {
			logger.error("方法：{}，操作失败：异常{}", request.getMethod(), e);
			e.printStackTrace();
		}
		responseData.setData(map);
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 第三方提现到客户卡 状态修改专用接口
	 *
	 * @param incomeRequestId
	 * @param confirm1Reject3
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/third2CustomConfirmOrReject")
	public String Third2CustomConfirmOrReject(@RequestParam(value = "incomeRequestId") Long incomeRequestId,
			@RequestParam(value = "confirm1Reject3") Integer confirm1Reject3) throws JsonProcessingException {
		GeneralResponseData<String> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功");
		try {
			BizIncomeRequest bizIncomeRequest = incomeRequestService.findById(incomeRequestId);
			if (null != bizIncomeRequest && null != bizIncomeRequest.getType()
					&& bizIncomeRequest.getType().equals(IncomeRequestType.WithdrawThirdToCustomer.getType())
					&& null != bizIncomeRequest.getStatus()
					&& bizIncomeRequest.getStatus().equals(IncomeRequestStatus.Matching.getStatus())) {
				// 只有第三方提现到客户卡可以操作 直接确认或拒绝 且必须在待确认状态
				if (confirm1Reject3.equals(IncomeRequestStatus.Matched.getStatus())) {
					// 确认操作
					bizIncomeRequest.setStatus(IncomeRequestStatus.Matched.getStatus());
				} else if (confirm1Reject3.equals(IncomeRequestStatus.Canceled.getStatus())) {
					// 取消操作
					bizIncomeRequest.setStatus(IncomeRequestStatus.Canceled.getStatus());
				} else {
					responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
							"操作失败，confirm1Reject3参数错误");
				}
				incomeRequestService.save(bizIncomeRequest, true);
			} else {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败");
			}
		} catch (Exception e) {
			logger.error("第三方提现到客户卡 状态修改失败，{},入款id：" + incomeRequestId + "，操作：" + confirm1Reject3, e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败");
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 修改空字符串为null
	 *
	 * @param temp
	 * @return
	 */
	private String changeNullFromEmpty(String temp) {
		if (temp == null || StringUtils.trim(temp).equals("")) {
			return null;
		} else {
			return temp;
		}
	}

	@Autowired
	private AccountFeeService accountFeeService;

	/**
	 * 下发任务 提现
	 *
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping(value = "/saveThirdTransInDrawTask")
	public String saveThirdTransInDrawTask(@RequestParam(value = "accountId") Integer accountId,
			@RequestParam(value = "amount") BigDecimal amount,
			@RequestParam(value = "fee", required = false) String fee,
			@RequestParam(value = "thirdBalance") BigDecimal thirdBal,
			@RequestParam(value = "thirdBankBalance") BigDecimal thirdBankBal) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator))
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		Integer th3Id = accountService.getSelectedThirdIdByLockedId(accountId);
		if (Objects.isNull(th3Id))
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "未选定第三方"));
		if (BigDecimal.ZERO.compareTo(amount) >= 0)
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "无法处理非正提现金额"));
		AccountBaseInfo from = accountService.getFromCacheById(th3Id);
		if (Objects.isNull(from))
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "三方账号不存在"));
		AccountBaseInfo to = accountService.getFromCacheById(accountId);
		if (Objects.isNull(to))
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "下发的账号不存在!"));
		logger.info(
				"NewThirdTrans{}  [ INIT PARAMS ] >> frId: {} toId: {}  amt:  {} fee: {} thirdBal: {} thirdBankBal: {}",
				th3Id, th3Id, accountId, amount, fee, thirdBal, thirdBankBal);
		BigDecimal realAmount = amount;
		BigDecimal realFee = StringUtils.isNotBlank(fee) ? new BigDecimal(fee).abs() : BigDecimal.ZERO;
		if (realFee.compareTo(BigDecimal.ZERO) > 0) {
			AccountFeeConfig th3FeeCfg = accountFeeService.findTh3FeeCfg(from);
			if (Objects.nonNull(th3FeeCfg) && Objects.equals(th3FeeCfg.getFeeType(), (byte) 1)) {// 商家扣0从金额扣1
				realAmount = realAmount.subtract(realFee);
				logger.info(
						"NewThirdTrans{}  [ FEE DEDUCT FROM TRANSFER AMOUNT ] >> frId: {} toId: {} realAmt：{} amt:  {} fee: {} thirdBal: {} thirdBankBal: {}",
						th3Id, th3Id, accountId, realAmount.add(realFee), amount, fee, thirdBal, thirdBankBal);
			}
		}
		BizIncomeRequest o = new BizIncomeRequest();
		o.setToId(accountId);
		o.setFromId(th3Id);
		o.setHandicap(from.getHandicapId());
		o.setLevel(0);
		o.setToAccount(to.getAccount());
		o.setOperator(operator.getId());
		o.setAmount(realAmount);// 手续费计算之后的真实金额
		o.setFee(realFee);// 手续费
		o.setCreateTime(new Date());
		o.setOrderNo(System.currentTimeMillis() + StringUtils.EMPTY + ((int) (Math.random() * 100 + 1)));
		o.setRemark(StringUtils.EMPTY);
		o.setType(IncomeRequestType.WithdrawThird.getType());
		o.setFromAccount(from.getAccount());
		o.setMemberUserName(StringUtils.EMPTY);
		o.setMemberRealName(to != null ? to.getOwner() : StringUtils.EMPTY);
		o.setStatus(IncomeRequestStatus.Matching.getStatus());
		Long lockedTime = accountService.getOutcardLockedTime(accountId);
		if (Objects.nonNull(lockedTime))
			o.setTimeconsume((o.getCreateTime().getTime() - lockedTime) / 1000);// 秒
		o.setThirdBalance(thirdBal);
		o.setThirdBankBalance(thirdBankBal);
		BizIncomeRequest ret = incomeRequestService.saveThirdDraw(o);
		BizAccount th3Acc = accountService.getById(o.getFromId());
		if (Objects.nonNull(th3Acc)) {
			th3Acc.setBankBalance(thirdBankBal);
			accountService.save(th3Acc);
			systemAccountManager.registTh3(th3Acc, ret);
		}
		// 提现时间
		accountService.saveDrawTime(o.getToId(), true);
		transMonitorService.reportTransResult(ret);
		accountService.saveDrawAmountAndFee(o.getFromId(), o.getToId(), o.getAmount(), o.getFee());// 保存下发金额直到完成匹配了删除
		accountController.refreshCache(operator.getId());// 刷新缓存
		logger.info(
				"NewThirdTrans{}  [ WITHDRAW FROM TH3 ACCOUNT SUCCESSFULLY ] >> frId: {} toId: {}  amt:  {} fee: {} thirdBal: {} thirdBankBal: {}",
				th3Id, th3Id, accountId, amount, fee, thirdBal, thirdBankBal);
		return mapper.writeValueAsString(
				new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "提现成功!"));
	}

	/**
	 * 第三方账号提现
	 *
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping(value = "/saveThirdTrans", method = RequestMethod.POST)
	public String saveThirdTrans(@Valid @RequestBody SaveThirdTransInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		Map<String, Object> map = new LinkedHashMap<>();
		if (result.hasErrors())
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过"));
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator))
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		try {
			BizAccount fromAccountTrans = accountService.getById(inputDTO.getFromId());
			if (fromAccountTrans == null)
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "三方账号不存在!"));
			if (fromAccountTrans.getBalance() == null || BigDecimal.ZERO.equals(fromAccountTrans.getBalance())) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"三方系统余额为空或者为0!");
				return mapper.writeValueAsString(responseData);
			}
			if (!accountService.checkThirdInAccount4DrawLocked(operator.getId(), inputDTO.getToIdArray(),
					inputDTO.getFromId())) {
				return mapper.writeValueAsString(new GeneralResponseData<>(
						GeneralResponseData.ResponseStatus.FAIL.getValue(), "你锁定的账号已失效或未锁定,请点击查询刷新页面！"));
			}
			logger.info("第三方提现:fromAccount:{},frId:{},toIdArray:{},toAccountToArray:{}", inputDTO.getFromAccount(),
					inputDTO.getFromId(), inputDTO.getToIdArray(), inputDTO.getToAccountToArray());
			BigDecimal[] amountToArray = inputDTO.getAmountToArray();
			for (int i = 0; i < inputDTO.getToIdArray().length; i++) {
				if (amountToArray[i] == null || amountToArray[i].compareTo(BigDecimal.valueOf(0)) <= 0) {
					return mapper.writeValueAsString(
							new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "金额有误！"));
				}
				AccountBaseInfo to = accountService.getFromCacheById(inputDTO.getToIdArray()[i]);
				if (to == null) {
					return mapper.writeValueAsString(new GeneralResponseData<>(
							GeneralResponseData.ResponseStatus.FAIL.getValue(), "账号Id:" + to.getId() + "信息不存在!"));
				}
				if (!AccountStatus.Normal.getStatus().equals(to.getStatus())) {
					return mapper.writeValueAsString(new GeneralResponseData<>(
							GeneralResponseData.ResponseStatus.FAIL.getValue(), "账号:" + to.getAccount() + "不是在用状态!"));
				}
				if (allocateTransService.exceedAmountSumDailyOutward(to.getId())) {
					return mapper.writeValueAsString(
							new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
									"账号:" + to.getAccount() + "已达到当日入款限额!"));
				}
				BizIncomeRequest o = new BizIncomeRequest();
				o.setToId(inputDTO.getToIdArray()[i]);
				o.setFromId(inputDTO.getFromId());
				o.setHandicap(fromAccountTrans.getHandicapId());
				o.setLevel(0);
				o.setToAccount(inputDTO.getToAccountToArray()[i]);
				o.setOperator(operator.getId());
				o.setAmount(amountToArray[i]);
				o.setCreateTime(new Date());
				o.setOrderNo(inputDTO.getOrderNoArray()[i]);
				o.setRemark(StringUtils.EMPTY);
				o.setType(inputDTO.getType());
				o.setFromAccount(inputDTO.getFromAccount());
				o.setMemberUserName(StringUtils.EMPTY);
				o.setMemberRealName(to != null ? to.getOwner() : StringUtils.EMPTY);
				o.setStatus(IncomeRequestStatus.Matching.getStatus());
				if (inputDTO.getFeeToArray().length > 0 && StringUtils.isNotEmpty(inputDTO.getFeeToArray()[i])) {
					o.setFee(new BigDecimal(inputDTO.getFeeToArray()[i]));
				}
				if (null == incomeRequestService.findByHandicapAndOrderNo(o.getHandicap(), o.getOrderNo())) {
					// 如果是下发任务里提现的
					if (null != inputDTO.getDrawTask() && inputDTO.getDrawTask().intValue() == 1) {
					} else {
						boolean outCard = null != to.getType() && AccountType.OutBank.getTypeId().equals(to.getType());
						if (outCard) {
							Integer currcredit = accountChangeService.currCredits(to);
							boolean currcreditConditon = currcredit == null
									|| currcredit <= amountToArray[i].intValue();
							Set<String> excluded = accountService.getRecycleBindComm();
							boolean isBlack = !CollectionUtils.isEmpty(excluded)
									&& excluded.contains(to.getId().toString());
							boolean below = accountService.belowPercentageNeedThirdDraw(to.getId());
							boolean isFlowBalaceLargerThanDailyOut = accountService
									.isFlowBalaceLargerThanDailyOut(to.getId());
							if (isFlowBalaceLargerThanDailyOut) {
								logger.debug("出款卡id:{} 出款流水加余额大于当日出款限额 ：{}", to.getId(),
										isFlowBalaceLargerThanDailyOut);
								accountService.unlockedThirdToDrawList(operator.getId(), to.getId(),
										inputDTO.getFromId());
								String thirdAccountNano = accountService.getFromIdWithNanoFromLockedHash(to.getId());
								allocateTransService.unLockForThirdDrawToOutCard(thirdAccountNano, to.getId());
								accountService.removeLockedHash(to.getId());
								accountService.removeAmountInputStored(inputDTO.getFromId(), to.getId());
								responseData = new GeneralResponseData<>(
										GeneralResponseData.ResponseStatus.FAIL.getValue(),
										"账号" + to.getAccount() + "出款流水加余额大于当日出款限额,不能下发！");
								return mapper.writeValueAsString(responseData);
							}
							if (currcreditConditon) {
								logger.debug("当前信用额度为空:{} ,或者 信用额度小于 提现金额:{}", currcredit, amountToArray[i].intValue());
								accountService.unlockedThirdToDrawList(operator.getId(), to.getId(),
										inputDTO.getFromId());
								String thirdAccountNano = accountService.getFromIdWithNanoFromLockedHash(to.getId());
								allocateTransService.unLockForThirdDrawToOutCard(thirdAccountNano, to.getId());
								accountService.removeLockedHash(to.getId());
								accountService.removeAmountInputStored(inputDTO.getFromId(), to.getId());
								responseData = new GeneralResponseData<>(
										GeneralResponseData.ResponseStatus.FAIL.getValue(),
										"账号" + to.getAccount() + "当前信用额度为0或者小于提现金额,不能下发！");
								return mapper.writeValueAsString(responseData);
							}
							if (isBlack) {
								logger.debug("账号:{} 已被冻结,不能继续下发！", to.getAccount());
								accountService.unlockedThirdToDrawList(operator.getId(), to.getId(),
										inputDTO.getFromId());
								String thirdAccountNano = accountService.getFromIdWithNanoFromLockedHash(to.getId());
								allocateTransService.unLockForThirdDrawToOutCard(thirdAccountNano, to.getId());
								accountService.removeLockedHash(to.getId());
								accountService.removeAmountInputStored(inputDTO.getFromId(), to.getId());
								responseData = new GeneralResponseData<>(
										GeneralResponseData.ResponseStatus.FAIL.getValue(),
										"账号" + to.getAccount() + "已被冻结,不能继续下发！");
								return mapper.writeValueAsString(responseData);
							}
							if (!below) {
								logger.debug("账号 :{} 余额已达上限,不能继续下发！", to.getAccount());
								accountService.unlockedThirdToDrawList(operator.getId(), to.getId(),
										inputDTO.getFromId());
								String thirdAccountNano = accountService.getFromIdWithNanoFromLockedHash(to.getId());
								allocateTransService.unLockForThirdDrawToOutCard(thirdAccountNano, to.getId());
								accountService.removeLockedHash(to.getId());
								accountService.removeAmountInputStored(inputDTO.getFromId(), to.getId());
								responseData = new GeneralResponseData<>(
										GeneralResponseData.ResponseStatus.FAIL.getValue(),
										"账号" + to.getAccount() + " 余额已达上限,不能继续下发！");
								return mapper.writeValueAsString(responseData);
							}
							accountService.unlockedAndAddUnfinished(operator.getId(), to.getId(), inputDTO.getFromId());
							// accountService.removeAmountInputStored(inputDTO.getFromId(),
							// to.getId());
						}
						// incomeRequestService.save(o, false);
						BizIncomeRequest ret = incomeRequestService.saveThirdDraw(o);
						systemAccountManager.registTh3(fromAccountTrans, ret);
						transMonitorService.reportTransResult(ret);
						// 第三方下发 提现后 系统余额要扣除 不论是下发到下发卡还是下发到出款卡
						accountService.removeAmountInputStored(inputDTO.getFromId(), to.getId());

						// accountService.saveDrawAmountAndFee(o.getFromId(),
						// o.getToId(),
						// o.getAmount(),
						// null == o.getFee() ? BigDecimal.ZERO : o.getFee());
						if (!outCard) {
							accountService.unlockThirdInAccount4Draw(operator.getId(), to.getId(),
									inputDTO.getFromId());
						}
						// 保存到尚未匹配的缓存里 以判断不能在下发任务里再次出现
						accountService.saveToUnfinishedDrawInThird(to.getId());
						logger.info("Income 第三方下发已入数据库，order:{},toId:{},fromId:{}", o.getOrderNo(), to.getId(),
								inputDTO.getFromId());
					}
				} else {
					logger.debug("orderNo: {} ,already exist.", o.getOrderNo());
				}
			}
			map.put("message", "提现成功！");
		} catch (Exception e) {
			logger.error("提现失败：", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"提现失败！" + e.getStackTrace());
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		}
		responseData.setData(map);
		return mapper.writeValueAsString(responseData);
	}
}
