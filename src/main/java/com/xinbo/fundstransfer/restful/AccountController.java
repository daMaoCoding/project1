package com.xinbo.fundstransfer.restful;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Functions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.accountfee.exception.NoSuiteAccountFeeRuleException;
import com.xinbo.fundstransfer.accountfee.pojo.AccountFeeCalResult;
import com.xinbo.fundstransfer.accountfee.service.AccountFeeService;
import com.xinbo.fundstransfer.assign.AvailableCardCache;
import com.xinbo.fundstransfer.component.net.socket.ActionEventEnum;
import com.xinbo.fundstransfer.component.net.socket.MessageEntity;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.*;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.*;
import com.xinbo.fundstransfer.domain.enums.UserCategory;
import com.xinbo.fundstransfer.domain.pojo.*;
import com.xinbo.fundstransfer.domain.repository.AccountLevelRepository;
import com.xinbo.fundstransfer.domain.repository.OutwardTaskRepository;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import com.xinbo.fundstransfer.report.SystemAccountManager;
import com.xinbo.fundstransfer.report.up.ReportInitParam;
import com.xinbo.fundstransfer.runtime.MemCacheUtils;
import com.xinbo.fundstransfer.sec.FundTransferEncrypter;
import com.xinbo.fundstransfer.service.*;
import com.xinbo.fundstransfer.service.impl.AccountServiceImpl;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.apache.shiro.web.util.WebUtils;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 账号管理
 */
@RestController
@RequestMapping("/r/account")
public class AccountController extends BaseController {
	private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

	@Autowired
	public HttpServletRequest request;
	@Autowired
	private SysUserService userService;
	@Autowired
	private AccountService accountService;
	@Autowired
	private AccountExtraService accountExtraService;
	@Autowired
	private AccountBindingService accountBindingService;
	@Autowired
	private BankLogService bankLogService;
	@Autowired
	private AccountLevelRepository accountLevelRepository;
	@Autowired
	private SysDataPermissionService dataPermissionService;
	@Autowired
	private AllocateIncomeAccountService incomeAccountAllocateService;
	@Autowired
	private HostMonitorService hostMonitorService;
	@Autowired
	private AllocateTransferService allocateTransferService;
	@Autowired
	private HandicapService handicapService;
	@Autowired
	private IncomeRequestService incomeRequestService;
	@Autowired
	private AccountSyncService accountSyncService;
	@Autowired
	private FinBalanceStatService finBalanceStatService;
	@Autowired
	private LevelService levelService;
	@Autowired
	private OutwardTaskRepository outwardTaskDao;

	@Autowired
	private SystemSettingService systemSettingService;
	@Autowired
	private CabanaService cabanaService;
	@Autowired
	private SysUserProfileService sysUserProfileService;
	@Autowired
	private FinLessStatService finLessStatService;
	private ObjectMapper mapper = new ObjectMapper();
	@Autowired
	private AllocateTransService allocateTransService;
	@Autowired
	private AccountMoreService accMoreSer;
	@Autowired
	private RebateApiService rebateApiService;
	@Autowired
	private SysDataPermissionService sysDataPermissionService;
	@Autowired
	private RebateUserService rebateUserService;
	@Autowired
	private AccountChangeService accountChangeService;
	@Autowired
	private SystemAccountManager systemAccountManager;
	@Autowired
	private AccountFeeService accountFeeService;
	@Autowired
	private RebateStatisticsService rebateStatisticsService;
	@Autowired
	private AccountClickService accountClickService;
	@Autowired
	private ThirdAccountService thirdAccountService;
	private final LoadingCache<Integer, List<Integer>> ONLINEIDLIST = CacheBuilder.newBuilder().maximumSize(10000)
			.refreshAfterWrite(30, TimeUnit.SECONDS).build(new CacheLoader<Integer, List<Integer>>() {
				@Override
				public List<Integer> load(Integer type) {
					List<Integer> onlineList = accountService.onlineAccountIdsList(type);
					logger.debug("加载 在线 账号id :{},type:{}", onlineList.toString(), type);
					return onlineList;
				}
			});
	private final LoadingCache<Integer, List<Integer>> PAUSEDIDLIST = CacheBuilder.newBuilder().maximumSize(10000)
			.refreshAfterWrite(30, TimeUnit.SECONDS).build(new CacheLoader<Integer, List<Integer>>() {
				@Override
				public List<Integer> load(Integer type) {
					List<Integer> pasuedList = accountService.pausedMobileAccountIds(type);
					logger.debug("加载 暂停 账号id :{},type:{}", pasuedList.toString(), type);
					return pasuedList;
				}
			});

	/**
	 * 获取当前用户拥有的盘口id
	 */
	private List<Integer> getHandicapIdByCurrentUser(Integer handicap, SysUser sysUser) {
		List<Integer> handicapList = new ArrayList<>();
		SysUser loginUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			return handicapList;
		}
		if (loginUser.getId() != 0 && handicap == 0) {
			List<BizHandicap> list = sysDataPermissionService.getHandicapByUserId(sysUser);
			if (list != null && list.size() > 0) {
				list.stream().forEach(p -> handicapList.add(p.getId()));
			}
		} else {
			handicapList.add(handicap);
		}
		return handicapList;
	}

	/**
	 * 描述:新增公司入款银行卡
	 *
	 * @param inputDTO
	 * @param errors
	 * @return
	 */
	@PostMapping("/addInBankAccount")
	public GeneralResponseData addInBankAccount(@Validated @RequestBody AddInBankAccountInputDTO inputDTO,
			Errors errors) {
		if (errors.hasErrors()) {
			return new GeneralResponseData(-1, "参数校验不通过!");
		}
		BizHandicap handicap = handicapService.findFromCacheById(inputDTO.getHandicapId());
		if (ObjectUtils.isEmpty(handicap)) {
			return new GeneralResponseData(-1, String.format("盘口%s不存在!", handicap.getName()));
		}
		if (!handicap.getStatus().equals(1)) {
			return new GeneralResponseData(-1, String.format("盘口%s已停用!", handicap.getName()));
		}
		BizAccount old = accountService.findByAccountNo(inputDTO.getAccount(), inputDTO.getSubType());
		if (!ObjectUtils.isEmpty(old)) {
			return new GeneralResponseData(-1, String.format("账号%s已存在!", old.getAccount()));
		}
		BizAccount ret = accountService.addInBankAccount(inputDTO, old);
		GeneralResponseData responseData = new GeneralResponseData(1, "新增成功!");
		responseData.setData(ret);
		return responseData;
	}

	/**
	 * 查询所有出款账号
	 */
	@RequestMapping("/getAllOutAccount")
	public String getAllOutAccount() throws JsonProcessingException {
		try {
			Integer[] outTypeArray = { AccountType.OutBank.getTypeId(), AccountType.OutThird.getTypeId() };
			List<BizAccount> data = accountService.getAllOutAccount(outTypeArray);
			GeneralResponseData<List<BizAccount>> response = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "获取所有出款账号成功");
			response.setData(data);
			return mapper.writeValueAsString(response);
		} catch (Exception e) {
			logger.error("获取所有出款账号成功：{}", e);
			return mapper
					.writeValueAsString(new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue()));
		}
	}

	/**
	 * 根据账号id查询同一个层级下的其他账号id
	 *
	 * @param accoutId
	 *            账号ID
	 */
	@RequestMapping("/getOtherAccountIds")
	public String getOtherAccountsInSameLevel(@RequestParam("accountId") Integer accoutId)
			throws JsonProcessingException {
		try {
			List<Integer> data = accountService.getBizAccountLevelList(accoutId);
			GeneralResponseData<List<Integer>> response = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			response.setData(data);
			return mapper.writeValueAsString(response);
		} catch (Exception e) {
			logger.error("根据账号查询同一层级下的其他账号失败：参数：{},原因：{}", accoutId, e);
			return mapper
					.writeValueAsString(new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue()));
		}
	}

	/**
	 * 检测是否可以停止审核（入款审核）
	 */
	@RequestMapping("/validStopAudit4Income")
	public String validStopAudit4Income() throws JsonProcessingException {
		GeneralResponseData<Integer> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		try {
			boolean emptyUser = Objects.isNull(operator);
			int data = 0;
			if (emptyUser) {
				data = 1;
				responseData.setData(data);
				return mapper.writeValueAsString(responseData);
			}
			StringBuilder stringBuilder = new StringBuilder();
			boolean checkLogOut = false;
			final String checkYes = "YES";
			String check = incomeAccountAllocateService.checkLogout(operator.getId());
			if (checkYes.equals(check)) {
				checkLogOut = true;
			} else {
				stringBuilder.append(check);
			}
			if (checkLogOut) {
				data = 1;
			}
			logger.trace("检测是否可以停止审核（入款审核） uid:" + operator.getUid() + ";result:" + (emptyUser || checkLogOut));
			// boolean valid = Objects.isNull(operator) ||
			// incomeAccountAllocateService.checkLogout(operator.getId());
			// responseData.setData(valid ? 1 : 0);
			responseData.setData(data);
			responseData.setMessage(stringBuilder.toString());
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error("查询失败：{}", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue());
			return mapper.writeValueAsString(responseData);
		}
	}

	/**
	 * 通过账号id 查询 盘口 层级信息
	 */
	@RequestMapping("/getLevelsOrHandicapByAccountIdArray")
	public String getLevelsByAccountId(@RequestParam(value = "accountIdArr") Integer[] accountIdArr)
			throws JsonProcessingException {

		try {
			List<Collection<Object>> data = accountService.findHandicapAndLevel(accountIdArr);
			GeneralResponseData<List<Collection<Object>>> response = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			if (CollectionUtils.isEmpty(data)) {
				response.setData(new ArrayList<>());
			} else {
				response.setData(data);
			}
			return mapper.writeValueAsString(response);
		} catch (Exception e) {
			logger.error("查询失败：{}", e);
			return mapper
					.writeValueAsString(new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue()));
		}
	}

	/**
	 * 根据当前用户获取账号信息(只查入款银行卡)
	 */
	// @RequiresPermissions({ "IncomeAuditComp:Retrieve" })
	@RequestMapping("/getAccountsByCurrentUser")
	public String getAccountsByCurrentUser() throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		String params = buildParams().toString();
		try {
			logger.trace(String.format("%s，参数：%s", "根据当前用户获取账号信息(查入款卡)", params));
			List<Integer> levelIdSList = dataPermissionService.findLevelIdList(operator.getId());
			GeneralResponseData<List<BizAccount>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			if (!CollectionUtils.isEmpty(levelIdSList)) {
				responseData.setData(accountService.getIncomeAccountList(null, levelIdSList,
						AccountType.InBank.getTypeId(), AccountType.InAli.getTypeId()));
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error(String.format("%s，参数：%s，结果：%s", "根据当前用户获取账号信息(查入款卡)", params, e.getMessage()));
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询失败"));
		}
	}

	/**
	 * 根据盘口与层级获取账号信息
	 *
	 * @param handicapId
	 *            盘口ID
	 * @param levelId
	 *            层级ID
	 * @param incomeTypeArray
	 *            入款账号分类
	 */
	@RequestMapping("/findByHandicapAndLevel")
	public String findByHandicapAndLevel(@RequestParam(value = "handicapId", required = false) Integer handicapId,
			@RequestParam(value = "levelId", required = false) Integer levelId,
			@RequestParam(value = "incomeTypeArray", required = false) Integer[] incomeTypeArray)
			throws JsonProcessingException {
		List<Integer> levelList = levelId == null ? null : new ArrayList<Integer>() {
			{
				add(levelId);
			}
		};
		List<BizAccount> accountList = accountService.getIncomeAccountList(handicapId, levelList, incomeTypeArray);
		GeneralResponseData<List<BizAccount>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		responseData.setData(accountList);
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 根据层级id 查询账号
	 */
	@RequiresPermissions(value = { "IncomeAuditComp:Retrieve", "OutwardTask:Retrieve",
			"OutwardTaskTotal:Retrieve" }, logical = Logical.OR)
	@RequestMapping("/getByLevelId")
	public String getByLevelId(@RequestParam(value = "levelId") Integer levelId) throws JsonProcessingException {
		String params = buildParams().toString();
		logger.trace(String.format("%s，参数：%s", "根据层级id 查询账号", params));
		GeneralResponseData<List<BizAccount>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		List<BizAccountLevel> bizAccountLevelList = accountLevelRepository.findByLevelId(levelId);
		if (bizAccountLevelList != null && bizAccountLevelList.size() > 0) {
			List<Integer> integerList = new ArrayList<>();
			bizAccountLevelList.forEach((p) -> integerList.add(p.getAccountId()));
			Specification<BizAccount> specif = DynamicSpecifications.build(BizAccount.class,
					new SearchFilter("id", SearchFilter.Operator.IN, integerList.toArray()));
			List<BizAccount> bizAccountList = accountService.getAccountList(specif, null);
			responseData.setData(bizAccountList);
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 通过账号查询 当前人的拥有的出款账号 出款使用 以判断出款账号和网银账号是否一致
	 */
	@RequiresPermissions(value = { "OutwardTask:Retrieve", "OutwardTaskTotal:Retrieve" }, logical = Logical.OR)
	@RequestMapping("/getByAccount")
	public String findByAccout(@RequestParam(value = "account") String account) throws JsonProcessingException {
		String params = buildParams().toString();
		logger.trace(String.format("%s，参数：%s", "通过账号查询 出款使用 以判断出款账号和网银账号是否一致", params));
		GeneralResponseData<List<BizAccount>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		List<BizAccount> accountList = null;
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		try {
			List<SearchFilter> filterToList = new ArrayList<>();
			if (sysUser != null && sysUser.getId() != null) {
				filterToList.add(new SearchFilter("holder", SearchFilter.Operator.EQ, sysUser.getId()));
			}
			filterToList.add(new SearchFilter("account", SearchFilter.Operator.LIKE, account));
			filterToList.add(new SearchFilter("type", SearchFilter.Operator.EQ, AccountType.OutBank.getTypeId()));
			filterToList.add(new SearchFilter("status", SearchFilter.Operator.EQ, AccountStatus.Normal.getStatus()));
			SearchFilter[] filterToArray = filterToList.toArray(new SearchFilter[filterToList.size()]);
			Specification<BizAccount> specif = DynamicSpecifications.build(BizAccount.class, filterToArray);
			accountList = accountService.getAccountList(specif, sysUser);
		} catch (Exception e) {
			logger.error(String.format("%s，参数：%s，结果：%s", "通过账号查询 出款使用 以判断出款账号和网银账号是否一致 ", params, e.getMessage()));
		}
		responseData.setData(accountList);
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 单个账号信息查询
	 */
	@RequestMapping("/findById")
	public String findById(@RequestParam(value = "id") Integer id) throws JsonProcessingException {
		String params = buildParams().toString();
		logger.trace(String.format("%s，参数：%s", "单个账号信息查询", params));
		try {
			GeneralResponseData<BizAccount> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			BizAccount account = accountService.findById(operator, id);
			responseData.setData(account);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error(String.format("%s，参数：%s，结果：%s", "单个账号信息查询", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 查询账号统计信息
	 */
	@RequestMapping("/findmorebyid")
	public String findMoreById(@RequestParam(value = "id") Integer id) throws JsonProcessingException {
		String params = buildParams().toString();
		logger.trace(String.format("%s，参数：%s", "查询账号统计信息", params));
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			BizAccount account = accountService.findById(operator, id);
			String sumAmountCondition = bankLogService.querySumAmountCondition(id, 24);
			if (null == sumAmountCondition) {
				sumAmountCondition = "0";
			}
			String queryIncomeTotal = bankLogService.queryIncomeTotal(id);
			if (null == queryIncomeTotal) {
				queryIncomeTotal = "0";
			}
			String queryOutTotal = bankLogService.queryOutTotal(id);
			if (null == queryOutTotal) {
				queryOutTotal = "0";
			}
			BigDecimal incomeDailyTotal = accountService.findAmountDailyByTotal(0, id);
			BigDecimal outcomeDailyTotal = accountService.findAmountDailyByTotal(1, id);
			Map<String, Object> data = new HashMap<>();
			data.put("account", account);
			data.put("sumAmountCondition", sumAmountCondition);
			data.put("queryIncomeTotal", queryIncomeTotal);
			data.put("queryOutTotal", queryOutTotal);
			data.put("incomeDailyTotal", incomeDailyTotal);
			data.put("outDailyTotal", outcomeDailyTotal);
			responseData.setData(data);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error(String.format("%s，参数：%s，结果：%s", "查询账号统计信息", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 查询待提额数据
	 */
	@RequestMapping("/toberaised")
	public String toBeRaised(@RequestParam(value = "pageNo") Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "typeToArray", required = false) Integer[] typeToArray,
			@RequestParam(value = "statusToArray", required = false) Integer[] statusToArray,
			@RequestParam(value = "handicapId", required = false) Integer[] handicapId,
			@RequestParam(value = "currSysLevel", required = false) Integer[] currSysLevel)
			throws JsonProcessingException {
		Map<String, Object> param = buildParams();
		String params = param.toString();
		logger.trace(String.format("%s，参数：%s", "查询待提额信息", params));
		try {
			GeneralResponseData<List<BizAccount>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser == null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆");
				return mapper.writeValueAsString(responseData);
			}
			List<SearchFilter> filterToList = DynamicSpecifications.build(request);
			if (typeToArray == null || typeToArray.length == 0 || statusToArray == null || statusToArray.length == 0) {
				return mapper.writeValueAsString(responseData);
			}

			List<BizHandicap> dataToList = dataPermissionService.getOnlyHandicapByUserId(sysUser);
			List<Integer> handicapIdToList = null;
			if (CollectionUtils.isEmpty(dataToList)) {
				return mapper.writeValueAsString(new GeneralResponseData<>(
						GeneralResponseData.ResponseStatus.FAIL.getValue(), "当前用户未配置盘口权限，请配置后再进行查询！"));
			} else {
				handicapIdToList = new ArrayList<Integer>();
				for (int i = 0; i < dataToList.size(); i++) {
					if (null != dataToList.get(i) && null != dataToList.get(i).getId()) {
						handicapIdToList.add(dataToList.get(i).getId());
					}
				}
				if (CollectionUtils.isEmpty(handicapIdToList)) {
					return mapper.writeValueAsString(responseData);
				}
				if (handicapId != null && handicapId.length > 0) {
					handicapIdToList = Arrays.asList(handicapId);
				}
			}
			if (currSysLevel == null || currSysLevel.length == 0) {
				currSysLevel = null;
			}
			String bankType = null;
			if (param.get("search_LIKE_bankType") != null
					&& StringUtils.isNotBlank(param.get("search_LIKE_bankType").toString())) {
				bankType = param.get("search_LIKE_bankType").toString();
			}
			String account = null;
			if (param.get("search_LIKE_account") != null
					&& StringUtils.isNotBlank(param.get("search_LIKE_account").toString())) {
				account = param.get("search_LIKE_account").toString();
			}
			String owner = null;
			if (param.get("search_LIKE_owner") != null
					&& StringUtils.isNotBlank(param.get("search_LIKE_owner").toString())) {
				owner = param.get("search_LIKE_owner").toString();
			}
			String alias = null;
			if (param.get("search_EQ_alias") != null
					&& StringUtils.isNotBlank(param.get("search_EQ_alias").toString())) {
				alias = param.get("search_EQ_alias").toString();
			}
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE);
			Page<BizAccount> page;
			List<Integer> typeList = null;
			List<Integer> statusList = null;
			List<Integer> levelList = null;
			if (typeToArray != null && typeToArray.length > 0) {
				typeList = Arrays.asList(typeToArray);
			}
			if (statusToArray != null && statusToArray.length > 0) {
				statusList = Arrays.asList(statusToArray);
			}
			if (currSysLevel != null && currSysLevel.length > 0) {
				levelList = Arrays.asList(currSysLevel);
			}
			page = accountService.findToBeRaisePage(sysUser, typeList, statusList, handicapIdToList, levelList,
					bankType, account, owner, alias, pageRequest);
			BigDecimal totalBankBalance = accountService.getTotalBankBalance(sysUser, typeList, statusList,
					handicapIdToList, levelList, bankType, account, owner, alias);
			Map<String, Object> header = new HashMap<>();
			header.put("totalAmountBankBalance", totalBankBalance);
			responseData.setData(page.getContent());
			responseData.setPage(new Paging(page, header));
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error(String.format("%s，参数：%s，结果：%s", "查询待提额信息", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 监控银行流水，为主机分配账号时，账号列表获取
	 */
	@RequestMapping("/list4Host")
	public String list4Host(@RequestParam(value = "pageNo", required = false) int pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "type", required = false) Integer[] type,
			@RequestParam(value = "currSysLevel", required = false) Integer currSysLevel,
			@RequestParam(value = "statusToArray", required = false) Integer[] statusToArray)
			throws JsonProcessingException {
		String params = buildParams().toString();
		logger.trace(String.format("%s，参数：%s", "监控银行流水，为主机分配账号时，账号列表获取", params));
		try {
			GeneralResponseData<List<BizAccount>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (operator == null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆");
				return mapper.writeValueAsString(responseData);
			}

			List<SearchFilter> filterToList = DynamicSpecifications.build(request);
			if (type == null || statusToArray == null || statusToArray.length == 0) {
				return mapper.writeValueAsString(responseData);
			}
			filterToList.add(new SearchFilter("type", SearchFilter.Operator.IN, type));
			filterToList.add(new SearchFilter("status", SearchFilter.Operator.IN, statusToArray));
			List<Integer> hostAccountIdList = hostMonitorService.findAllAccountIdList();
			if (!CollectionUtils.isEmpty(hostAccountIdList)) {
				filterToList.add(new SearchFilter("id", SearchFilter.Operator.NOTIN, hostAccountIdList.toArray()));
			}
			if (null != currSysLevel) {
				filterToList.add(new SearchFilter("currSysLevel", SearchFilter.Operator.EQ, currSysLevel));
			}
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					Sort.Direction.ASC, "id");
			SearchFilter[] filterToArray = filterToList.toArray(new SearchFilter[filterToList.size()]);
			Specification<BizAccount> specif = DynamicSpecifications.build(BizAccount.class, filterToArray);
			Page<BizAccount> page = accountService.findPage(operator, specif, pageRequest);
			page.getContent().stream().forEach((p) -> p.setOwner(CommonUtils.hideAccountAll(p.getOwner(), "name")));
			responseData.setData(page.getContent());
			List<Integer> accountIdList = accountService.findAccountIdList(filterToArray);
			responseData.setPage(new Paging(page, buildHeader(accountIdList, filterToArray, null)));
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error(String.format("%s，参数：%s，结果：%s", "监控银行流水，为主机分配账号时，账号列表获取", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 银行账号信息获取
	 *
	 * @param account
	 *            账号
	 */
	@RequestMapping("/find4Bank")
	public String find4Bank(@RequestParam(value = "account") String account) throws JsonProcessingException {
		try {
			List<BizAccount> accList = accountService.findList(null, DynamicSpecifications.build(BizAccount.class,
					new SearchFilter("account", SearchFilter.Operator.EQ, account)));
			accList = accList.stream()
					.filter(p -> !Objects.equals(p.getType(), AccountType.InAli.getTypeId())
							|| !Objects.equals(p.getType(), AccountType.InWechat.getTypeId())
							|| !Objects.equals(p.getType(), AccountType.InThird.getTypeId()))
					.collect(Collectors.toList());
			if (CollectionUtils.isEmpty(accList)) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "无数据记录"));
			}
			GeneralResponseData<BizAccount> response = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			response.setData(accList.get(0));
			return mapper.writeValueAsString(response);
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 支付宝账号信息获取
	 *
	 * @param account
	 *            账号
	 */
	@RequestMapping("/find4Alipay")
	public String find4Alipay(@RequestParam(value = "account") String account) throws JsonProcessingException {
		try {
			List<BizAccount> accList = accountService.findList(null,
					DynamicSpecifications.build(BizAccount.class,
							new SearchFilter("account", SearchFilter.Operator.EQ, account),
							new SearchFilter("type", SearchFilter.Operator.EQ, AccountType.InAli.getTypeId())));
			if (CollectionUtils.isEmpty(accList)) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "无数据记录"));
			}
			GeneralResponseData<BizAccount> response = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			response.setData(accList.get(0));
			return mapper.writeValueAsString(response);
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 微信账号信息获取
	 *
	 * @param account
	 *            账号
	 */
	@RequestMapping("/find4Wechat")
	public String find4Wechat(@RequestParam(value = "account") String account) throws JsonProcessingException {
		try {
			List<BizAccount> accList = accountService.findList(null,
					DynamicSpecifications.build(BizAccount.class,
							new SearchFilter("account", SearchFilter.Operator.EQ, account),
							new SearchFilter("type", SearchFilter.Operator.EQ, AccountType.InWechat.getTypeId())));
			if (CollectionUtils.isEmpty(accList)) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "无数据记录"));
			}
			GeneralResponseData<BizAccount> response = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			response.setData(accList.get(0));
			return mapper.writeValueAsString(response);
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 入款汇总 匹配中账号列表 根据银行流水条倒序
	 *
	 * @param pageNo
	 * @param statusToArray
	 * @param handicapId
	 * @param account
	 * @param bankType
	 * @param owner
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/findIncomeAccountOrderByBankLog")
	public String findIncomeAccountOrderByBankLog(@RequestParam(value = "pageNo") Integer pageNo,
			@RequestParam(value = "statusToArray", required = false) Integer[] statusToArray,
			@RequestParam(value = "search_IN_flag", required = false) Integer[] search_IN_flag,
			@RequestParam(value = "handicapId", required = false) Integer[] handicapId,
			@RequestParam(value = "account", required = false) String account,
			@RequestParam(value = "alias", required = false) String alias,
			@RequestParam(value = "bankType", required = false) String bankType,
			@RequestParam(value = "owner", required = false) String owner) throws JsonProcessingException {
		String params = buildParams().toString();
		logger.trace(String.format("%s，参数：%s", "账号分页获取", params));
		try {
			GeneralResponseData<List<Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, 10);
			if (null == handicapId || handicapId.length < 1) {
				handicapId = new Integer[100];
				SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
				List<BizHandicap> dataToList = dataPermissionService.getOnlyHandicapByUserId(sysUser);
				if (CollectionUtils.isEmpty(dataToList)) {
					return mapper.writeValueAsString(new GeneralResponseData<>(
							GeneralResponseData.ResponseStatus.FAIL.getValue(), "当前用户未配置盘口权限，请配置后再进行查询！"));
				} else {
					for (int i = 0; i < dataToList.size(); i++) {
						if (null != dataToList.get(i) && null != dataToList.get(i).getId()) {
							handicapId[i] = dataToList.get(i).getId();
						}
					}
				}
			}
			Page<Object> page = accountService.findIncomeAccountOrderByBankLog(handicapId, account, alias, bankType,
					owner, search_IN_flag, statusToArray, pageRequest);
			responseData.setData(page.getContent());
			responseData.setPage(new Paging(page));
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(String.format("%s，参数：%s，结果：%s", "账号分页获取", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/findByAlias")
	public String findByAlias(@RequestParam(value = "alias") String alias) throws JsonProcessingException {
		String params = buildParams().toString();
		logger.trace(String.format("%s，参数：%s", "查询", params));
		try {
			GeneralResponseData<List<BizAccount>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			if (StringUtils.isAnyBlank(alias)) {
				return mapper.writeValueAsString("请输入编号");
			}
			responseData.setData(accountService.findByAlias(alias));
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(String.format("%s，参数：%s，结果：%s", "查询失败", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询失败  " + e.getLocalizedMessage()));
		}
	}

	@Autowired
	private AvailableCardCache availableCardCache;

	@RequestMapping("/statusDesc")
	public String statusDesc(@RequestParam(value = "id") Integer accountId) throws JsonProcessingException {
		if (accountId == null) {
			return "参数必传";
		}
		String desc = availableCardCache.statusReason(accountId);
		GeneralResponseData<String> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
		responseData.setData(desc);
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 根据查询条件读取账号
	 *
	 * @param pageNo
	 * @param pageSize
	 * @param typeToArray
	 * @param statusToArray
	 * @param operator
	 * @param holderType
	 * @param handicapId
	 * @param currSysLevel
	 * @param bankType
	 * @param sortProperty
	 * @param sortDirection
	 * @param auditor
	 * @param deviceStatus
	 * @param transBlackTo
	 * @param isRetrieve
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/list2")
	public String list2(@RequestParam(value = "pageNo") Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "typeToArray", required = false) Integer[] typeToArray,
			@RequestParam(value = "statusToArray", required = false) Integer[] statusToArray,
			@RequestParam(value = "operator", required = false) String operator,
			@RequestParam(value = "holderType", required = false) String holderType,
			@RequestParam(value = "search_EQ_handicapId", required = false) Integer handicapIdEq,
			@RequestParam(value = "search_IN_handicapId", required = false) Integer[] handicapId,
			@RequestParam(value = "currSysLevel", required = false) Integer[] currSysLevel,
			@RequestParam(value = "bankType", required = false) String bankType,
			@RequestParam(value = "needTotal", required = false) Integer needTotal,

			@RequestParam(value = "sortProperty", required = false) String sortProperty,
			@RequestParam(value = "sortDirection", required = false) Integer sortDirection,
			@RequestParam(value = "auditor", required = false) String auditor,
			@RequestParam(value = "deviceStatus", required = false) String deviceStatus,
			@RequestParam(value = "transBlackTo", required = false) Integer transBlackTo,
			@RequestParam(value = "isRetrieve", required = false) String isRetrieve) throws JsonProcessingException {
		String params = buildParams().toString();
		logger.trace(String.format("%s，参数：%s", "账号分页获取", params));
		try {
			long start = System.currentTimeMillis();
			long threadId = Thread.currentThread().getId();
			logger.debug("当前线程ID:{} , 查询开始时间:{}", threadId, start);
			GeneralResponseData<List<BizAccount>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			responseData.setData(new ArrayList<>());
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser == null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆");
				return mapper.writeValueAsString(responseData);
			}
			String handicaps = request.getParameter("search_IN_handicapId");
			if (StringUtils.isBlank(handicaps)
					|| (null == handicapId || handicapId.length == 0) && null == handicapIdEq) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "盘口必传");
				return mapper.writeValueAsString(responseData);
			}
			if (typeToArray == null || typeToArray.length == 0 || statusToArray == null || statusToArray.length == 0) {
				return mapper.writeValueAsString(responseData);
			}
			// 封装查询条件 DynamicSpecifications.build(request);
			List<SearchFilter> filterToList = new ArrayList<>();
			// DynamicSpecifications.build(request);
			// 忽略状态，查询所有银行卡张数 顺序勿调换 全部 在线 暂停+离线 停用 冻结
			int totalSize = 0, onlineSize = 0, offlineSize = 0, stoppedSzie = 0, freezedSize = 0, otherStatusSize = 0;
			Map<String, Object> header = new HashMap<>(16);
			Integer[] idSizes = { totalSize, onlineSize, offlineSize, stoppedSzie, freezedSize };
			header.put("IdSize", idSizes);
			header.put("totalAmountBalance", 0);
			header.put("totalAmountBankBalance", 0);
			header.put("totalAmountIncomeDaily", 0);
			header.put("totalAmountOutwardDaily", 0);
			Paging page = new Paging();
			page.setHeader(header);
			responseData.setPage(page);

			List<Integer> statusList = (statusToArray == null || statusToArray.length == 0) ? new ArrayList<>()
					: Arrays.asList(statusToArray);
			boolean status1And5 = (statusList.size() == 2 && statusList.contains(AccountStatus.Normal.getStatus())
					&& statusList.contains(AccountStatus.Enabled.getStatus()))
					|| (statusList.size() == 1 && AccountStatus.Normal.getStatus().equals(statusList.get(0))
							|| AccountStatus.Enabled.getStatus().equals(statusList.get(0)));
			boolean queryOnline = !ObjectUtils.isEmpty(deviceStatus) && deviceStatus.equals("online") && status1And5;
			boolean queryOffline = !ObjectUtils.isEmpty(deviceStatus) && deviceStatus.equals("offline") && status1And5;
			boolean queryStopped = null != statusToArray && statusToArray.length == 1
					&& AccountStatus.StopTemp.getStatus().equals(statusToArray[0]);
			boolean queryFreezed = null != statusToArray && statusToArray.length == 1
					&& AccountStatus.Freeze.getStatus().equals(statusToArray[0]);
			boolean queryOtherStatus = false;

			// 审核人
			List<Integer> accountIdListByAuditor = new ArrayList<>();
			boolean auditorQuery = StringUtils.isNotBlank(auditor);
			if (auditorQuery) {
				List<Integer> auditorIdList = new ArrayList<>();
				List<SysUser> users = userService.findByUidLike(auditor);
				if (!CollectionUtils.isEmpty(users)) {
					users.forEach((p) -> auditorIdList.add(p.getId()));
				}
				if (!CollectionUtils.isEmpty(auditorIdList)) {
					accountIdListByAuditor = incomeAccountAllocateService.findAccountIdList(auditorIdList);
				}
			}
			// 持卡人 都查 但是只过滤 全部 在线 离线数据 不过滤 停用和冻结的
			boolean userHolderFilter = StringUtils.isNotBlank(operator);
			List<Integer> holderIdList = new ArrayList<>();
			List<SysUser> operatorList;
			if (userHolderFilter) {
				operatorList = userService.findByUidLike(operator);
				if (!CollectionUtils.isEmpty(operatorList)) {
					holderIdList = operatorList.stream().map(p -> p.getId()).collect(Collectors.toList());
				}
			}

			// 回收 未回收
			boolean queryIsRetrieve = Objects.equals(isRetrieve, "1");
			boolean queryNotRetrieve = Objects.equals(isRetrieve, "2");
			Set<String> recycle1 = accountService.getRecycleBindComm();
			Set<String> recycle = CollectionUtils.isEmpty(recycle1) ? new HashSet<>() : recycle1;

			// 固定条件是 类型 如果查询页签确定了 status 也确定了 其他都是针对某个页签的具体条件 可以抽出来
			if (typeToArray.length == 1) {
				filterToList.add(new SearchFilter("type", SearchFilter.Operator.EQ, typeToArray[0]));
			} else {
				filterToList.add(new SearchFilter("type", SearchFilter.Operator.IN, typeToArray));
			}
			// needTotal 不为空并且是1 表示账号统计 其他查询按照传入的状态查询
			boolean queryNeedTotal = null != needTotal && needTotal == 1;
			Integer[] status = queryNeedTotal
					? new Integer[] { AccountStatus.Normal.getStatus(), AccountStatus.StopTemp.getStatus(),
							AccountStatus.Enabled.getStatus() }
					: statusToArray;
			if (status.length == 1) {
				filterToList.add(new SearchFilter("status", SearchFilter.Operator.EQ, status[0]));
			} else {
				filterToList.add(new SearchFilter("status", SearchFilter.Operator.IN, status));
			}
			if (null != handicapIdEq) {
				filterToList.add(new SearchFilter("handicapId", SearchFilter.Operator.EQ, handicapIdEq));
			}
			if (!handicaps.contains(",")) {
				filterToList.add(new SearchFilter("handicapId", SearchFilter.Operator.EQ, handicaps));
			} else {
				filterToList.add(new SearchFilter("handicapId", SearchFilter.Operator.IN, handicaps.split(",")));

			}
			String flagStr = request.getParameter("search_IN_flag");
			if (StringUtils.isNotBlank(flagStr)) {
				if (!flagStr.contains(",")) {
					filterToList.add(new SearchFilter("flag", SearchFilter.Operator.EQ, flagStr));
				} else {
					filterToList.add(new SearchFilter("flag", SearchFilter.Operator.IN, flagStr.split(",")));
				}
			}

			// 降序 升序 条件
			Sort.Direction direction = sortDirection != null && Sort.Direction.ASC.ordinal() == sortDirection
					? Sort.Direction.ASC
					: Sort.Direction.DESC;

			// 第三方查询 标识
			boolean flag = (statusToArray.length == 1 && statusToArray[0] == 3);

			Sort sort = Sort.by(
					ArrayUtils.contains(typeToArray, AccountType.InThird.getTypeId()) ? Sort.Direction.DESC : direction,
					flag ? "updateTime"
							: ArrayUtils.contains(typeToArray, AccountType.InThird.getTypeId()) ? "balance"
									: "bankBalance",
					"currSysLevel", "handicapId", "status");
			if (!StringUtils.isBlank(sortProperty) && !"status".equals(sortProperty)) {
				if ("statusImportant".equals(sortProperty)) {
					sort = Sort.by(direction, "status");
				} else {
					sort = Sort.by(direction, sortProperty);
				}
			}

			SearchFilter[] filterToArray = filterToList.toArray(new SearchFilter[filterToList.size()]);
			Specification<BizAccount> specif = DynamicSpecifications.build(BizAccount.class, filterToArray);
			// 查询不用分页 直接查询
			long sqlStart = System.currentTimeMillis();
			logger.debug("当前线程ID:{} ,查询sql 开始时间:{}", threadId, sqlStart);
			List<BizAccount> data = accountService.findBySpecificAndSort(sysUser, specif, sort);
			long sqlEnd = System.currentTimeMillis();
			logger.debug("当前线程ID:{} ,查询sql 结束时间:{} ,耗时:{} ", threadId, sqlEnd, sqlEnd - sqlStart);
			data = CollectionUtils.isEmpty(data) ? Lists.newArrayList() : data;
			totalSize = data.size();
			idSizes[0] = totalSize;

			if (dealEmptyResult(header, responseData, page, idSizes, data)) {
				return mapper.writeValueAsString(responseData);
			}

			// 停用 冻结

			List<BizAccount> stoppedList = data.stream()
					.filter(p -> AccountStatus.StopTemp.getStatus().equals(p.getStatus())).collect(Collectors.toList());
			List<BizAccount> freezedList = data.stream()
					.filter(p -> AccountStatus.Freeze.getStatus().equals(p.getStatus())).collect(Collectors.toList());

			stoppedSzie = stoppedList.size();

			freezedSize = freezedList.size();
			long onlineStart = System.currentTimeMillis();
			logger.debug("当前线程ID:{} ,统计在线 开始时间:{}", threadId, onlineStart);
			// 在线
			List<Integer> online_idList = ONLINEIDLIST.get(typeToArray[0]);// accountService.onlineAccountIdsList(typeToArray[0]);
			logger.debug("当前线程ID:{} , 查询的在线id:{}", threadId, online_idList);

			// 不在线 且不是暂停的 都归为离线 只要是状态是在用 可用
			ArrayList status1Or5 = new ArrayList() {
				{
					add(AccountStatus.Enabled.getStatus());
					add(AccountStatus.Normal.getStatus());
				}
			};

			// 在线
			List<BizAccount> onlineList = new ArrayList<>();
			if (!CollectionUtils.isEmpty(online_idList)) {
				onlineList = data.stream()
						.filter(p -> status1Or5.contains(p.getStatus()) && online_idList.contains(p.getId()))
						.collect(Collectors.toList());
			}
			onlineSize = onlineList.size();
			long onlineEnd = System.currentTimeMillis();
			logger.debug("当前线程ID:{} ,统计在线 结束时间:{},耗时:{}  ", threadId, onlineEnd, onlineEnd - onlineStart);

			long offLineStart = System.currentTimeMillis();
			logger.debug("当前线程ID:{} ,统计离线 开始时间:{}", threadId, offLineStart);

			// 离线 = 不在在线里 和 暂停
			Set<BizAccount> offLineList = new HashSet<>();
			// 暂停
			List<Integer> stop_idList = PAUSEDIDLIST.get(typeToArray[0]);// accountService.pausedMobileAccountIds(typeToArray[0]);
			logger.debug("当前线程ID:{} ,查询的暂停id:{}", threadId, stop_idList);
			List<BizAccount> pausedList = new ArrayList<>();

			// 在暂停里
			if (!CollectionUtils.isEmpty(stop_idList)) {
				pausedList = data.stream()
						.filter(p -> status1Or5.contains(p.getStatus()) && stop_idList.contains(p.getId()))
						.collect(Collectors.toList());

			}
			offLineList.addAll(pausedList);

			// 既不在在线里 也不在暂停里
			List<BizAccount> notOnlineAndNotPaused = new ArrayList<>();
			if (!CollectionUtils.isEmpty(onlineList)) {
				List<Integer> online = online_idList;
				// 不在线的
				List<BizAccount> notOnline = data.stream()
						.filter(p -> status1Or5.contains(p.getStatus()) && !online.contains(p.getId()))
						.collect(Collectors.toList());
				notOnlineAndNotPaused.addAll(notOnline);

			}
			if (!CollectionUtils.isEmpty(pausedList)) {
				// 不在暂停里的
				List<Integer> paused = stop_idList;
				List<BizAccount> dataToFilter = notOnlineAndNotPaused.size() == 0 ? data : notOnlineAndNotPaused;
				List<BizAccount> nontPaused = dataToFilter.stream()
						.filter(p -> status1Or5.contains(p.getStatus()) && !paused.contains(p.getId()))
						.collect(Collectors.toList());
				notOnlineAndNotPaused.addAll(nontPaused);
			}
			if (CollectionUtils.isEmpty(onlineList) && CollectionUtils.isEmpty(pausedList)) {
				// 都没有在线和暂停的记录 那么离线就是 status= 1 5 的记录
				notOnlineAndNotPaused = data.stream().filter(p -> status1Or5.contains(p.getStatus()))
						.collect(Collectors.toList());
			}
			offLineList.addAll(notOnlineAndNotPaused);
			offlineSize = offLineList.size();

			long offLineEnd = System.currentTimeMillis();
			logger.debug("当前线程ID:{} ,统计离线 结束时间:{} 耗时:{}", threadId, offLineEnd, offLineEnd - offLineStart);

			idSizes[0] = totalSize;
			idSizes[1] = onlineSize;
			idSizes[2] = offlineSize;
			idSizes[3] = stoppedSzie;
			idSizes[4] = freezedSize;

			// 以下为特定的页签的过滤条件
			long filterStart = System.currentTimeMillis();
			logger.debug("当前线程ID:{} ,条件过滤 开始时间:{}", threadId, filterStart);
			String mobile = request.getParameter("search_LIKE_mobile");
			if (StringUtils.isNotBlank(mobile)) {
				data = data.stream().filter(p -> mobile.equals(p.getMobile())).collect(Collectors.toList());
				onlineList = onlineList.stream().filter(p -> mobile.equals(p.getMobile())).collect(Collectors.toList());
				offLineList = offLineList.stream().filter(p -> mobile.equals(p.getMobile()))
						.collect(Collectors.toSet());
				stoppedList = stoppedList.stream().filter(p -> mobile.equals(p.getMobile()))
						.collect(Collectors.toList());
				freezedList = freezedList.stream().filter(p -> mobile.equals(p.getMobile()))
						.collect(Collectors.toList());
			}
			String bankName = request.getParameter("search_LIKE_bankName");
			if (StringUtils.isNotBlank(bankName)) {
				data = data.stream().filter(p -> bankName.equals(p.getBankName())).collect(Collectors.toList());
				onlineList = onlineList.stream().filter(p -> bankName.equals(p.getBankName()))
						.collect(Collectors.toList());
				offLineList = offLineList.stream().filter(p -> bankName.equals(p.getBankName()))
						.collect(Collectors.toSet());
				stoppedList = stoppedList.stream().filter(p -> bankName.equals(p.getBankName()))
						.collect(Collectors.toList());
				freezedList = freezedList.stream().filter(p -> bankName.equals(p.getBankName()))
						.collect(Collectors.toList());
			}
			// 状态条件
			boolean statusQuery = null != statusToArray && statusToArray.length > 0 && (queryOnline || queryOffline);
			if (statusQuery) {
				if (statusToArray.length == 1) {
					List<BizAccount> data1 = data;
					if (!CollectionUtils.isEmpty(onlineList)) {
						onlineList = onlineList.stream().filter(p -> {
							boolean statusEQ = statusToArray[0].equals(p.getStatus());
							if (!statusEQ) {
								data1.remove(p);
							}
							return statusEQ;
						}).collect(Collectors.toList());
					}
					if (!CollectionUtils.isEmpty(offLineList)) {
						offLineList = offLineList.stream().filter(p -> {
							boolean statusEQ = statusToArray[0].equals(p.getStatus());
							if (!statusEQ) {
								data1.remove(p);
							}
							return statusEQ;
						}).collect(Collectors.toSet());
					}
					data = data1;
				} else {
					List<Integer> statusIn = Arrays.asList(statusToArray);
					List<BizAccount> data1 = data;
					if (!CollectionUtils.isEmpty(onlineList)) {
						onlineList = onlineList.stream().filter(p -> {
							boolean statusContain = statusIn.contains(p.getStatus());
							if (!statusContain) {
								data1.remove(p);
							}
							return statusContain;
						}).collect(Collectors.toList());
					}
					if (!CollectionUtils.isEmpty(offLineList)) {
						offLineList = offLineList.stream().filter(p -> {
							boolean statusContain = statusIn.contains(p.getStatus());
							if (!statusContain) {
								data1.remove(p);
							}
							return statusContain;
						}).collect(Collectors.toSet());
					}
					data = data1;
				}
			}

			// 持卡人
			if (userHolderFilter) {
				List<Integer> holderIdLists = holderIdList;
				onlineList = onlineList.stream().filter(p -> holderIdLists.contains(p.getHolder()))
						.collect(Collectors.toList());
				offLineList = offLineList.stream().filter(p -> holderIdLists.contains(p.getHolder()))
						.collect(Collectors.toSet());
				stoppedList = stoppedList.stream().filter(p -> holderIdLists.contains(p.getHolder()))
						.collect(Collectors.toList());
				freezedList = freezedList.stream().filter(p -> holderIdLists.contains(p.getHolder()))
						.collect(Collectors.toList());
				data = data.stream().filter(p -> holderIdLists.contains(p.getHolder())).collect(Collectors.toList());
			}
			// 盘口条件
			if (handicapIdEq != null) {
				data = data.stream().filter(p -> handicapIdEq.equals(p.getHandicapId())).collect(Collectors.toList());
				onlineList = onlineList.stream().filter(p -> handicapIdEq.equals(p.getHandicapId()))
						.collect(Collectors.toList());
				offLineList = offLineList.stream().filter(p -> handicapIdEq.equals(p.getHandicapId()))
						.collect(Collectors.toSet());
				stoppedList = stoppedList.stream().filter(p -> handicapIdEq.equals(p.getHandicapId()))
						.collect(Collectors.toList());
				freezedList = freezedList.stream().filter(p -> handicapIdEq.equals(p.getHandicapId()))
						.collect(Collectors.toList());
			}
			if (null != handicapId && handicapId.length > 0) {
				List<Integer> handicapIds = Arrays.asList(handicapId);
				data = data.stream().filter(p -> handicapIds.contains(p.getHandicapId())).collect(Collectors.toList());
				onlineList = onlineList.stream().filter(p -> handicapIds.contains(p.getHandicapId()))
						.collect(Collectors.toList());
				offLineList = offLineList.stream().filter(p -> handicapIds.contains(p.getHandicapId()))
						.collect(Collectors.toSet());
				stoppedList = stoppedList.stream().filter(p -> handicapIds.contains(p.getHandicapId()))
						.collect(Collectors.toList());
				freezedList = freezedList.stream().filter(p -> handicapIds.contains(p.getHandicapId()))
						.collect(Collectors.toList());
			}
			// 编号过滤
			String alias = request.getParameter("search_EQ_alias");
			String aliasLike = request.getParameter("search_LIKE_alias");
			if (StringUtils.isNotBlank(alias)) {
				data = data.stream().filter(p -> alias.equals(p.getAlias())).collect(Collectors.toList());
				onlineList = onlineList.stream().filter(p -> alias.equals(p.getAlias())).collect(Collectors.toList());
				offLineList = offLineList.stream().filter(p -> alias.equals(p.getAlias())).collect(Collectors.toSet());
				stoppedList = stoppedList.stream().filter(p -> alias.equals(p.getAlias())).collect(Collectors.toList());
				freezedList = freezedList.stream().filter(p -> alias.equals(p.getAlias())).collect(Collectors.toList());
			}
			if (StringUtils.isNotBlank(aliasLike)) {
				data = data.stream().filter(p -> aliasLike.equals(p.getAlias())).collect(Collectors.toList());
				onlineList = onlineList.stream().filter(p -> aliasLike.equals(p.getAlias()))
						.collect(Collectors.toList());
				offLineList = offLineList.stream().filter(p -> aliasLike.equals(p.getAlias()))
						.collect(Collectors.toSet());
				stoppedList = stoppedList.stream().filter(p -> aliasLike.equals(p.getAlias()))
						.collect(Collectors.toList());
				freezedList = freezedList.stream().filter(p -> aliasLike.equals(p.getAlias()))
						.collect(Collectors.toList());
			}
			// 开户人
			String owner = request.getParameter("search_LIKE_owner");
			if (StringUtils.isNotBlank(owner)) {
				data = data.stream()
						.filter(p -> StringUtils.isNotBlank(p.getOwner()) && p.getOwner().indexOf(owner) >= 0)
						.collect(Collectors.toList());
				onlineList = onlineList.stream()
						.filter(p -> StringUtils.isNotBlank(p.getOwner()) && p.getOwner().indexOf(owner) >= 0)
						.collect(Collectors.toList());
				offLineList = offLineList.stream()
						.filter(p -> StringUtils.isNotBlank(p.getOwner()) && p.getOwner().indexOf(owner) >= 0)
						.collect(Collectors.toSet());
				stoppedList = stoppedList.stream()
						.filter(p -> StringUtils.isNotBlank(p.getOwner()) && p.getOwner().indexOf(owner) >= 0)
						.collect(Collectors.toList());
				freezedList = freezedList.stream()
						.filter(p -> StringUtils.isNotBlank(p.getOwner()) && p.getOwner().indexOf(owner) >= 0)
						.collect(Collectors.toList());
			}
			// 账号条件过滤
			String account = request.getParameter("search_LIKE_account");
			if (StringUtils.isNotBlank(account)) {
				data = data.stream()
						.filter(p -> StringUtils.isNotBlank(p.getAccount()) && p.getAccount().indexOf(account) >= 0)
						.collect(Collectors.toList());
				onlineList = onlineList.stream()
						.filter(p -> StringUtils.isNotBlank(p.getAccount()) && p.getAccount().indexOf(account) >= 0)
						.collect(Collectors.toList());
				offLineList = offLineList.stream()
						.filter(p -> StringUtils.isNotBlank(p.getAccount()) && p.getAccount().indexOf(account) >= 0)
						.collect(Collectors.toSet());
				stoppedList = stoppedList.stream()
						.filter(p -> StringUtils.isNotBlank(p.getAccount()) && p.getAccount().indexOf(account) >= 0)
						.collect(Collectors.toList());
				freezedList = freezedList.stream()
						.filter(p -> StringUtils.isNotBlank(p.getAccount()) && p.getAccount().indexOf(account) >= 0)
						.collect(Collectors.toList());
			}
			// 人工 manual 过滤条件
			String holderTypeFilter = StringUtils.isNotBlank(holderType) ? holderType : "";
			if ("manual".equals(holderTypeFilter)) {
				data = data.stream().filter(p -> null != p.getHolder()).collect(Collectors.toList());
				onlineList = onlineList.stream().filter(p -> null != p.getHolder()).collect(Collectors.toList());
				offLineList = offLineList.stream().filter(p -> null != p.getHolder()).collect(Collectors.toSet());
				stoppedList = stoppedList.stream().filter(p -> null != p.getHolder()).collect(Collectors.toList());
				freezedList = freezedList.stream().filter(p -> null != p.getHolder()).collect(Collectors.toList());
			}
			// 机器 robot 过滤条件
			if ("robot".equals(holderTypeFilter)) {
				data = data.stream().filter(p -> null == p.getHolder()).collect(Collectors.toList());
				onlineList = onlineList.stream().filter(p -> null == p.getHolder()).collect(Collectors.toList());
				offLineList = offLineList.stream().filter(p -> null == p.getHolder()).collect(Collectors.toSet());
				stoppedList = stoppedList.stream().filter(p -> null == p.getHolder()).collect(Collectors.toList());
				freezedList = freezedList.stream().filter(p -> null == p.getHolder()).collect(Collectors.toList());
			}
			// 内外中层级 过滤条件
			if (null != currSysLevel && currSysLevel.length > 0) {
				List<Integer> currSysLevelList = Arrays.asList(currSysLevel);
				data = data.stream().filter(p -> currSysLevelList.contains(p.getCurrSysLevel()))
						.collect(Collectors.toList());
				onlineList = onlineList.stream().filter(p -> currSysLevelList.contains(p.getCurrSysLevel()))
						.collect(Collectors.toList());
				offLineList = offLineList.stream().filter(p -> currSysLevelList.contains(p.getCurrSysLevel()))
						.collect(Collectors.toSet());
				stoppedList = stoppedList.stream().filter(p -> currSysLevelList.contains(p.getCurrSysLevel()))
						.collect(Collectors.toList());
				freezedList = freezedList.stream().filter(p -> currSysLevelList.contains(p.getCurrSysLevel()))
						.collect(Collectors.toList());
			}

			// 银行类型 过滤条件
			String bankTypeReq = request.getParameter("search_LIKE_bankType");
			if (StringUtils.isNotBlank(bankTypeReq)) {
				data = data.stream().filter(p -> bankTypeReq.equals(p.getBankType())).collect(Collectors.toList());
				onlineList = onlineList.stream().filter(p -> bankTypeReq.equals(p.getBankType()))
						.collect(Collectors.toList());
				offLineList = offLineList.stream().filter(p -> bankTypeReq.equals(p.getBankType()))
						.collect(Collectors.toSet());
				stoppedList = stoppedList.stream().filter(p -> bankTypeReq.equals(p.getBankType()))
						.collect(Collectors.toList());
				freezedList = freezedList.stream().filter(p -> bankTypeReq.equals(p.getBankType()))
						.collect(Collectors.toList());
			}
			if (StringUtils.isNotBlank(bankType)) {
				data = data.stream().filter(p -> bankType.equals(p.getBankType())).collect(Collectors.toList());
				onlineList = onlineList.stream().filter(p -> bankType.equals(p.getBankType()))
						.collect(Collectors.toList());
				offLineList = offLineList.stream().filter(p -> bankType.equals(p.getBankType()))
						.collect(Collectors.toSet());
				stoppedList = stoppedList.stream().filter(p -> bankType.equals(p.getBankType()))
						.collect(Collectors.toList());
				freezedList = freezedList.stream().filter(p -> bankType.equals(p.getBankType()))
						.collect(Collectors.toList());
			}
			// 审核人条件过滤
			if (auditorQuery) {
				List<Integer> accountIdListByAuditorFinal = accountIdListByAuditor;
				data = data.stream().filter(p -> accountIdListByAuditorFinal.contains(p.getId()))
						.collect(Collectors.toList());
				onlineList = onlineList.stream().filter(p -> accountIdListByAuditorFinal.contains(p.getId()))
						.collect(Collectors.toList());
				offLineList = offLineList.stream().filter(p -> accountIdListByAuditorFinal.contains(p.getId()))
						.collect(Collectors.toSet());
				stoppedList = stoppedList.stream().filter(p -> accountIdListByAuditorFinal.contains(p.getId()))
						.collect(Collectors.toList());
				freezedList = freezedList.stream().filter(p -> accountIdListByAuditorFinal.contains(p.getId()))
						.collect(Collectors.toList());
			}
			// 回收条件过滤
			if (queryIsRetrieve) {
				data = data.stream().filter(p -> recycle.contains(p.getId().toString())).collect(Collectors.toList());
				onlineList = onlineList.stream().filter(p -> recycle.contains(p.getId().toString()))
						.collect(Collectors.toList());
				offLineList = offLineList.stream().filter(p -> recycle.contains(p.getId().toString()))
						.collect(Collectors.toSet());
				stoppedList = stoppedList.stream().filter(p -> recycle.contains(p.getId().toString()))
						.collect(Collectors.toList());
				freezedList = freezedList.stream().filter(p -> recycle.contains(p.getId().toString()))
						.collect(Collectors.toList());
			}
			// 未回收条件过滤
			if (queryNotRetrieve) {
				data = data.stream().filter(p -> !recycle.contains(p.getId().toString())).collect(Collectors.toList());
				onlineList = onlineList.stream().filter(p -> !recycle.contains(p.getId().toString()))
						.collect(Collectors.toList());
				offLineList = offLineList.stream().filter(p -> !recycle.contains(p.getId().toString()))
						.collect(Collectors.toSet());
				stoppedList = stoppedList.stream().filter(p -> !recycle.contains(p.getId().toString()))
						.collect(Collectors.toList());
				freezedList = freezedList.stream().filter(p -> !recycle.contains(p.getId().toString()))
						.collect(Collectors.toList());
			}
			// 如果是按照 当日收款/当日出款 排序
			// 账号id
			List<Integer> accountIdList = data.stream().map(p -> p.getId()).collect(Collectors.toList());
			List<Map.Entry<Integer, BigDecimal>> amountDailyList;
			if ("incomeAmountDaily".equals(sortProperty) || "outwardAmountDaily".equals(sortProperty)) {
				List<Object> idStringList = new ArrayList<>();
				accountIdList.forEach((p) -> idStringList.add(String.valueOf(p)));
				amountDailyList = new ArrayList(accountService
						.findAmountDaily(("incomeAmountDaily".equals(sortProperty) ? 0 : 1), idStringList).entrySet());

				if (CollectionUtils.isEmpty(amountDailyList)) {
					wrapAccountResult(otherStatusSize, idSizes, queryOnline, queryOffline, queryStopped, queryFreezed,
							queryOtherStatus);
					header.put("IdSize", idSizes);
					return mapper.writeValueAsString(responseData);
				}
				int positive = Sort.Direction.ASC.ordinal() == direction.ordinal() ? 1 : (-1);
				Collections.sort(amountDailyList, (o1, o2) -> positive * o1.getValue().compareTo(o2.getValue()));
				List<Integer> accountIds = amountDailyList.stream().map(p -> p.getKey()).collect(Collectors.toList());
				data = data.stream().filter(p -> accountIds.contains(p.getId())).collect(Collectors.toList());

				if (CollectionUtils.isEmpty(data)) {
					wrapAccountResult(otherStatusSize, idSizes, queryOnline, queryOffline, queryStopped, queryFreezed,
							queryOtherStatus);
					header.put("IdSize", idSizes);
					return mapper.writeValueAsString(responseData);
				}
				onlineList = onlineList.stream().filter(p -> accountIds.contains(p.getId()))
						.collect(Collectors.toList());
				offLineList = offLineList.stream().filter(p -> accountIds.contains(p.getId()))
						.collect(Collectors.toSet());
				stoppedList = stoppedList.stream().filter(p -> accountIds.contains(p.getId()))
						.collect(Collectors.toList());
				freezedList = freezedList.stream().filter(p -> accountIds.contains(p.getId()))
						.collect(Collectors.toList());
			}
			totalSize = data.size();
			idSizes[0] = totalSize;

			if (queryOnline) {
				data = onlineList;
			} else if (queryOffline) {
				data = new ArrayList<>(offLineList);
			} else if (queryStopped) {
				data = stoppedList;
			} else if (queryFreezed) {
				data = freezedList;
			}

			int finalSize = data.size();

			onlineSize = onlineList.size();
			offlineSize = offLineList.size();
			stoppedSzie = stoppedList.size();
			freezedSize = freezedList.size();

			int counts = finalSize;

			idSizes[1] = onlineSize;
			idSizes[2] = offlineSize;
			idSizes[3] = stoppedSzie;
			idSizes[4] = freezedSize;

			long filterEnd = System.currentTimeMillis();
			logger.debug("当前线程ID:{} ,条件过滤 结束时间:{} 耗时:{}", threadId, filterEnd, filterEnd - filterStart);

			long balanceStart = System.currentTimeMillis();
			logger.debug("当前线程ID:{} ,统计银行金额 开始时间:{}", threadId, balanceStart);
			BigDecimal sumBalance = CollectionUtils.isEmpty(data) ? BigDecimal.ZERO : data.stream().map(p -> {
				if (null == p.getBalance()) {
					p.setBalance(BigDecimal.ZERO);
				}
				BigDecimal bal = p.getBalance();
				return bal;
			}).filter(p -> null != p).reduce(BigDecimal::add).get();
			BigDecimal sumBankBalance = CollectionUtils.isEmpty(data) ? BigDecimal.ZERO : data.stream().map(p -> {
				if (null == p.getBankBalance()) {
					p.setBankBalance(BigDecimal.ZERO);
				}
				BigDecimal bal = p.getBankBalance();
				return bal;
			}).reduce(BigDecimal::add).get();
			long balanceEnd = System.currentTimeMillis();
			logger.debug("当前线程ID:{} ,统计银行金额 结束时间:{} 耗时:{}", threadId, balanceEnd, balanceEnd - balanceStart);

			header.put("totalAmountBalance", sumBalance);
			header.put("totalAmountBankBalance", sumBankBalance);

			pageSize = null == pageSize ? AppConstants.PAGE_SIZE : pageSize;
			// 如果查询的结果比页码还少
			// if (counts <= pageNo) {
			// pageNo = 0;
			// }
			// int pageIndexMax = (counts / pageSize);
			// int indexEnd = pageNo < pageIndexMax ? (pageNo + 1) * pageSize :
			// counts;
			// int indexStart = pageIndexMax == 0 ? pageIndexMax : pageNo *
			// pageSize;
			// if (pageIndexMax == 0) {
			// pageNo = 0;
			// }
			// List<BizAccount> data2 = data.subList(indexStart, indexEnd);
			List<BizAccount> data2 = data;
			int average = counts / pageSize;
			int remainder = counts % pageSize;
			if (average == 0) {
				// 总记录数 小于pageSize的情况
				pageNo = 0;
				data2 = data.subList(0, counts);
			}
			if (average > 0) {
				if (remainder == 0) {
					if (pageNo < average) {
						data2 = data.subList(pageNo * pageSize, (pageNo + 1) * pageSize);
					}
				}
				if (remainder > 0) {
					if (pageNo >= average) {
						if (counts < pageNo * pageSize) {
							pageNo = 0;
							data2 = data.subList(pageNo * pageSize, (pageNo + 1) * pageSize);
						} else {
							data2 = data.subList(pageNo * pageSize, counts);
						}

					} else {
						data2 = data.subList(pageNo * pageSize, (pageNo + 1) * pageSize);
					}
				}
			}
			long wrapDataStart = System.currentTimeMillis();
			logger.debug("当前线程ID:{} ,封装数据packAllForForAccount 开始时间:{}", threadId, wrapDataStart);
			List<BizAccount> ret = accountService.packAllForForAccount(sysUser, data2);
			long wrapDataEnd = System.currentTimeMillis();
			logger.debug("当前线程ID:{} ,封装数据packAllForForAccount 结束时间:{} 耗时:{}", threadId, wrapDataEnd,
					wrapDataEnd - wrapDataStart);

			// 黑名单
			long blackStart = System.currentTimeMillis();
			logger.debug("当前线程ID:{} ,黑名单校验 开始时间:{}", threadId, blackStart);
			if (transBlackTo != null) {
				Set<String> black = allocateTransferService.findBlackList();
				if (!CollectionUtils.isEmpty(black)) {
					for (BizAccount acc : ret) {
						String key = RedisKeys.gen4TransBlack(AllocateTransferService.WILD_CARD_ACCOUNT, acc.getId(),
								0);
						if (black.stream().filter(p -> p.startsWith(key)).count() > 0) {
							acc.setTransBlackTo(1);
						}
					}
				}
			}
			long blackEnd = System.currentTimeMillis();
			logger.debug("当前线程ID:{} ,黑名单校验 结束时间:{}", threadId, blackEnd, blackEnd - blackStart);

			ret.stream().forEach(p -> {
				if (recycle.contains(p.getId().toString())) {
					p.setIsRetrieve("1");
				} else {
					p.setIsRetrieve("2");
				}
				p.setOwner(CommonUtils.hideAccountAll(p.getOwner(), "name"));
			});

			responseData.setData(ret);
			if (counts != 0) {
				page = CommonUtils.getPage(pageNo + 1, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
						String.valueOf(counts));
			} else {
				page = CommonUtils.getPage(0, pageSize != null ? pageSize : AppConstants.PAGE_SIZE, "0");
			}

			List<Integer> accountIdLists = ret.stream().map(p -> p.getId()).collect(Collectors.toList());
			Integer[] accountIdArray = new Integer[accountIdList.size()];
			logger.debug("当前线程ID:{} ,封装总金额 在线 离线 数据 参数:{} ", threadId, accountIdArray);
			long findAmountDailyByTotalStart = System.currentTimeMillis();
			logger.debug("当前线程ID:{} ,查询总入款金额findAmountDailyByTotal 开始时间:{}", threadId, findAmountDailyByTotalStart);
			BigDecimal totalIncomeDaily = accountService.findAmountDailyByTotal(0,
					accountIdLists.toArray(accountIdArray));
			long findAmountDailyByTotalEnd = System.currentTimeMillis();
			logger.debug("当前线程ID:{} ,查询总入款金额 findAmountDailyByTotal 结果时间:{},耗时:{}", threadId, findAmountDailyByTotalEnd,
					findAmountDailyByTotalEnd - findAmountDailyByTotalStart);
			logger.debug("当前线程ID:{} ,查询总入款金额 结果:{}", threadId, totalIncomeDaily);
			long totalOutwardDailyStart = System.currentTimeMillis();
			logger.debug("当前线程ID:{} ,查询总出款金额 findAmountDailyByTotal 开始时间:{}", threadId, totalOutwardDailyStart);
			BigDecimal totalOutwardDaily = accountService.findAmountDailyByTotal(1,
					accountIdLists.toArray(accountIdArray));
			long totalOutwardDailyEnd = System.currentTimeMillis();
			logger.debug("当前线程ID:{} ,查询总出款金额 findAmountDailyByTotal 结果时间:{} 耗时:{}", threadId, totalOutwardDailyEnd,
					totalOutwardDailyEnd - totalOutwardDailyStart);
			logger.debug("当前线程ID:{} ,查询总出款金额 结果:{}", threadId, totalIncomeDaily);
			header.put("IdSize", idSizes);
			header.put("totalAmountIncomeDaily", totalIncomeDaily);
			header.put("totalAmountOutwardDaily", totalOutwardDaily);

			page.setHeader(header);
			responseData.setPage(page);

			logger.debug("当前线程ID:{} ,封装数据 结束时间:{}", threadId, System.currentTimeMillis());

			long end = System.currentTimeMillis();
			logger.debug("当前线程ID:{} ,查询结束时间:{},时间差:{} ", threadId, end, end - start);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(String.format("%s，参数：%s，结果：%s", "账号分页获取", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	private void wrapAccountResult(int otherStatusSize, Integer[] idSizes, boolean queryOnline, boolean queryOffline,
			boolean queryStopped, boolean queryFreezed, boolean queryOtherStatus) {
		if (queryOnline) {
			idSizes[0] = idSizes[0] - idSizes[1];
			idSizes[1] = 0;
		} else if (queryOffline) {
			idSizes[0] = idSizes[0] - idSizes[2];
			idSizes[2] = 0;
		} else if (queryFreezed) {
			idSizes[0] = idSizes[0] - idSizes[4];
			idSizes[4] = 0;
		} else if (queryStopped) {
			idSizes[0] = idSizes[0] - idSizes[3];
			idSizes[3] = 0;
		} else if (queryOtherStatus) {
			idSizes[0] = idSizes[0] - otherStatusSize;
			if (idSizes[0] == 0) {
				idSizes[1] = 0;
				idSizes[2] = 0;
				idSizes[3] = 0;
				idSizes[4] = 0;
			}
		}
	}

	private boolean dealEmptyResult(Map<String, Object> header, GeneralResponseData<List<BizAccount>> responseData,
			Paging page, Integer[] idSizes, List<BizAccount> extractList) {
		if (CollectionUtils.isEmpty(extractList)) {
			header.put("IdSize", idSizes);
			page.setHeader(header);
			responseData.setPage(page);
			return true;
		}
		return false;
	}

	/**
	 * 根据查询条件读取账号
	 */
	@RequestMapping("/list")
	public String list(@RequestParam(value = "pageNo") Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "typeToArray", required = false) Integer[] typeToArray,
			@RequestParam(value = "statusToArray", required = false) Integer[] statusToArray,
			@RequestParam(value = "search_IN_handicapId", required = false) Integer[] search_IN_handicapId,
			@RequestParam(value = "operator", required = false) String operator,
			@RequestParam(value = "holderType", required = false) String holderType,
			@RequestParam(value = "currSysLevel", required = false) Integer[] currSysLevel,
			@RequestParam(value = "bankType", required = false) String bankType,
			@RequestParam(value = "bankTypeToArray", required = false) String[] bankTypeToArray,
			@RequestParam(value = "sortProperty", required = false) String sortProperty,
			@RequestParam(value = "sortDirection", required = false) Integer sortDirection,
			@RequestParam(value = "auditor", required = false) String auditor,
			@RequestParam(value = "transBlackTo", required = false) Integer transBlackTo,
			@RequestParam(value = "mySetup", required = false) Byte mySetup,
			@RequestParam(value = "isRetrieve", required = false) String isRetrieve) throws JsonProcessingException {
		String params = buildParams().toString();
		logger.trace(String.format("%s，参数：%s", "账号分页获取", params));
		try {
			GeneralResponseData<List<BizAccount>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser == null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆");
				return mapper.writeValueAsString(responseData);
			}
			if (search_IN_handicapId == null || search_IN_handicapId.length < 1) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "用户未配置盘口");
				return mapper.writeValueAsString(responseData);
			}
			List<SearchFilter> filterToList = DynamicSpecifications.build(request);
			List<SearchFilter> filterToList2 = DynamicSpecifications.build(request);
			if (StringUtils.isNotBlank(auditor)) {
				List<Integer> auditorIdList = new ArrayList<>();
				userService.findByUidLike(auditor).forEach((p) -> auditorIdList.add(p.getId()));
				List<Integer> accountIdList = incomeAccountAllocateService.findAccountIdList(auditorIdList);
				if (CollectionUtils.isEmpty(accountIdList)) {
					return mapper.writeValueAsString(responseData);
				}
				filterToList.add(new SearchFilter("id", SearchFilter.Operator.IN, accountIdList.toArray()));
			}
			// 如果是 下发任务页签 mySetup 1 全部 2 我的设定
			List<Integer> allSetUpList = Lists.newArrayList();
			List<Integer> mySetUped = null;
			boolean isMysetUp = null != mySetup;
			Map<String, Object> setUpMap = Maps.newHashMap();
			Map<Integer, String> thirdLockOper = isMysetUp && mySetup == 1 ? accountService.getAllSetUpThirdAccount()
					: new HashMap<>();
			if (isMysetUp) {
				// allSetUpList = accountService.getAllSetUpThirdAccount();
				mySetUped = accountService.getMySetUpThirdAccount(sysUser.getId());
				setUpMap.put("allSetUpNumber", 0);
				setUpMap.put("mySetUpedNumber", mySetUped.size());

				if (2 == mySetup.intValue()) {
					if (CollectionUtils.isEmpty(mySetUped)) {
						setUpMap.put("mySetUpedNumber", 0);
						Paging paging = new Paging();
						paging.setHeader(setUpMap);
						responseData.setPage(paging);
						responseData.setData(Lists.newArrayList());
						// return mapper.writeValueAsString(responseData);
					} else {
						// filterToList.add(new SearchFilter("id",
						// SearchFilter.Operator.IN,
						// mySetUped.toArray()));
					}
				}

			}

			if (typeToArray == null || typeToArray.length == 0 || statusToArray == null || statusToArray.length == 0) {
				return mapper.writeValueAsString(responseData);
			}
			if (typeToArray.length == 1) {
				filterToList.add(new SearchFilter("type", SearchFilter.Operator.EQ, typeToArray[0]));
			} else {
				filterToList.add(new SearchFilter("type", SearchFilter.Operator.IN, typeToArray));
			}
			SearchFilter filterStatus;
			if (statusToArray.length == 1) {
				filterStatus = new SearchFilter("status", SearchFilter.Operator.EQ, statusToArray[0]);
				filterToList.add(filterStatus);
			} else {
				filterStatus = new SearchFilter("status", SearchFilter.Operator.IN, statusToArray);
				filterToList.add(filterStatus);
			}
			if (StringUtils.isNotBlank(operator)) {
				List<SysUser> operatorList = userService.findByUidLike(operator);
				if (CollectionUtils.isEmpty(operatorList)) {
					return mapper.writeValueAsString(responseData);
				} else {
					List<Integer> holderIdList = new ArrayList<>();
					operatorList.forEach((p) -> holderIdList.add(p.getId()));
					filterToList.add(new SearchFilter("holder", SearchFilter.Operator.IN, holderIdList.toArray()));
				}
			}
			if (null != holderType) {
				// 人工 机器
				if (holderType.equals("manual")) {
					filterToList.add(new SearchFilter("holder", SearchFilter.Operator.ISNOTNULL, null));
					filterToList.add(new SearchFilter("flag", SearchFilter.Operator.NOTEQ, 1));
				} else if (holderType.equals("robot")) {
					filterToList.add(new SearchFilter("holder", SearchFilter.Operator.ISNULL, null));
					filterToList.add(new SearchFilter("flag", SearchFilter.Operator.NOTEQ, 1));
				} else if (holderType.equals("phone")) {
					filterToList.add(new SearchFilter("flag", SearchFilter.Operator.EQ, 1));
				}
			}
			if (null != currSysLevel) {
				if (currSysLevel.length > 1) {
					filterToList.add(new SearchFilter("currSysLevel", SearchFilter.Operator.IN, currSysLevel));
				}
				if (currSysLevel.length == 1) {
					filterToList.add(new SearchFilter("currSysLevel", SearchFilter.Operator.EQ, currSysLevel[0]));
				}
			}
			if (bankTypeToArray != null && bankTypeToArray.length != 0) {
				filterToList.add(new SearchFilter("bankType", SearchFilter.Operator.IN, bankTypeToArray));
			}
			if (bankType != null && StringUtils.isNoneEmpty(bankType)) {
				filterToList.add(new SearchFilter("bankType", SearchFilter.Operator.LIKE, bankType));
			}
			// 忽略状态，查询所有银行卡张数 顺序勿调换
			int totalSize = 0, onlineSize = 0, stopSize = 0, offlineSize = 0;
			// 全部
			if (Objects.nonNull(filterStatus))
				filterToList.remove(filterStatus);
			List<Integer> sts = new ArrayList<>(Arrays.asList(statusToArray));
			sts.remove(AccountStatus.Delete.getStatus());
			SearchFilter filterTmp = null;
			if (sts.size() == 1) {
				filterTmp = new SearchFilter("status", SearchFilter.Operator.EQ, sts.get(0));
				filterToList.add(filterTmp);
			} else if (sts.size() > 1) {
				filterTmp = new SearchFilter("status", SearchFilter.Operator.IN, sts.toArray());
				filterToList.add(filterTmp);
			}
			SearchFilter[] total_filterToArray = filterToList.toArray(new SearchFilter[filterToList.size()]);
			List<Integer> total_IdList = accountService.findAccountIdList(total_filterToArray);
			if (Objects.nonNull(filterTmp))
				filterToList.remove(filterTmp);
			if (Objects.nonNull(filterStatus))
				filterToList.add(filterStatus);
			if (null != total_IdList && total_IdList.size() > 0) {
				totalSize = total_IdList.size();
				setUpMap.put("allSetUpNumber", totalSize);
				if (isMysetUp) {
					// 我的设定
					if (2 == mySetup.intValue()) {
						if (CollectionUtils.isEmpty(mySetUped)) {
							return mapper.writeValueAsString(responseData);
						} else {
							filterToList.add(new SearchFilter("id", SearchFilter.Operator.IN, mySetUped.toArray()));
						}
					}
				}
			}
			// 在线
			List<SearchFilter> online_filterToList = new ArrayList<SearchFilter>();
			online_filterToList.addAll(filterToList);
			List<Integer> online_idList = accountService.onlineAccountIdsList(typeToArray[0]);
			logger.debug("查询的在线id:{}", online_idList);
			if (online_idList == null || online_idList.size() == 0) {
				online_filterToList.add(new SearchFilter("id", SearchFilter.Operator.EQ, 0));
			} else {
				online_filterToList.add(new SearchFilter("id", SearchFilter.Operator.IN, online_idList.toArray()));
			}
			SearchFilter[] online_filterToArray = online_filterToList
					.toArray(new SearchFilter[online_filterToList.size()]);
			List<Integer> online_IdList = accountService.findAccountIdList(online_filterToArray);
			if (null != online_IdList && online_IdList.size() > 0) {
				onlineSize = online_IdList.size();
			}
			// 暂停
			List<SearchFilter> stop_filterToList = new ArrayList<SearchFilter>();
			stop_filterToList.addAll(filterToList);
			List<Integer> stop_idList = accountService.pausedMobileAccountIds(typeToArray[0]);
			logger.debug("查询的暂停id:{}", stop_idList);
			if (stop_idList == null || stop_idList.size() == 0) {
				stop_filterToList.add(new SearchFilter("id", SearchFilter.Operator.EQ, 0));
			} else {
				stop_filterToList.add(new SearchFilter("id", SearchFilter.Operator.IN, stop_idList.toArray()));
			}
			SearchFilter[] stop_filterToArray = stop_filterToList.toArray(new SearchFilter[stop_filterToList.size()]);
			List<Integer> stop_IdList = accountService.findAccountIdList(stop_filterToArray);
			if (null != stop_IdList && stop_IdList.size() > 0) {
				stopSize = stop_IdList.size();
			}
			// 离线
			if ((totalSize - onlineSize - stopSize) > 0) {
				offlineSize = totalSize - onlineSize - stopSize;
			}
			Sort.Direction direction = sortDirection != null && Sort.Direction.ASC.ordinal() == sortDirection
					? Sort.Direction.ASC
					: Sort.Direction.DESC;
			boolean flag = (statusToArray.length == 1 && statusToArray[0] == 3);
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					ArrayUtils.contains(typeToArray, AccountType.InThird.getTypeId()) ? Sort.Direction.DESC : direction,
					flag ? "updateTime"
							: ArrayUtils.contains(typeToArray, AccountType.InThird.getTypeId()) ? "balance"
									: "bankBalance",
					"currSysLevel", "handicapId", "status");
			if (!StringUtils.isBlank(sortProperty) && !sortProperty.equals("status")) {
				if (sortProperty.equals("statusImportant")) {
					pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
							direction, "status");
				} else {
					pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
							direction, sortProperty);
				}
			}
			if (Objects.equals(isRetrieve, "1") || Objects.equals(isRetrieve, "2")) {
				Set<String> recycle = accountService.getRecycleBindComm();
				List<Integer> recyList = recycle.stream().map(p -> Integer.parseInt(p)).collect(Collectors.toList());
				if ("1".equals(isRetrieve)) {
					filterToList.add(new SearchFilter("id", SearchFilter.Operator.IN, recyList.toArray()));
				} else {
					filterToList.add(new SearchFilter("id", SearchFilter.Operator.NOTIN, recyList.toArray()));
				}
			}

			SearchFilter[] filterToArray = filterToList.toArray(new SearchFilter[filterToList.size()]);
			Specification<BizAccount> specif = DynamicSpecifications.build(BizAccount.class, filterToArray);

			Page<BizAccount> page;
			List<Integer> accountIdList = accountService.findAccountIdList(filterToArray);
			if ("incomeAmountDaily".equals(sortProperty) || "outwardAmountDaily".equals(sortProperty)) {
				page = accountService.findPageOrderByAmountDaily(sysUser, accountIdList, pageRequest);
			} else {
				page = accountService.findPage(sysUser, specif, pageRequest);
			}
			if (transBlackTo != null && !CollectionUtils.isEmpty(page.getContent())) {
				Set<String> black = allocateTransferService.findBlackList();
				if (!CollectionUtils.isEmpty(black)) {
					for (BizAccount acc : page.getContent()) {
						String key = RedisKeys.gen4TransBlack(AllocateTransferService.WILD_CARD_ACCOUNT, acc.getId(),
								0);
						boolean ret = black.stream().filter(p -> p.startsWith(key)).count() == 0;
						if (!ret) {
							acc.setTransBlackTo(1);
						}
					}
				}
			}
			if (!CollectionUtils.isEmpty(page.getContent())) {
				Set<String> recycle = accountService.getRecycleBindComm();
				if (!CollectionUtils.isEmpty(recycle)) {
					for (BizAccount acc : page.getContent()) {
						if (recycle.contains(acc.getId().toString())) {
							acc.setIsRetrieve("1"); // 已回收
						} else {
							acc.setIsRetrieve("2"); // 未回收
						}
					}
				}
			}
			if (null != page && !CollectionUtils.isEmpty(page.getContent())) {
				Iterator it = page.getContent().iterator();
				for (; it.hasNext();) {
					BizAccount p = (BizAccount) it.next();
					p.setOwner(CommonUtils.hideAccountAll(p.getOwner(), "name"));
					if (allSetUpList.contains(p.getId())) {
						// 全部页签里 表示是否被设定 1 设定 其他没设定
						p.setIsSetUpFlag((byte) 1);
					}
				}
			}
			List<BizAccount> data2 = page.getContent();
			if (isMysetUp && !CollectionUtils.isEmpty(data2)) {
				List<BizAccount> usedLastTimeRecord = Lists.newLinkedList(), normalData = Lists.newLinkedList(),
						abNormalData = Lists.newLinkedList();
				Integer idUsedLastTime = incomeRequestService.getThirdIdLastUsedByUserId(sysUser.getId());
				// 判断是否已经设定 需求 7456
				List<Integer> settedLists = accountService.getAllSetUpThirdAccount(null);
				List<Integer> mysetUpLists = mySetUped;
				Predicate<BizAccount> predicate = p -> !CollectionUtils.isEmpty(settedLists)
						&& settedLists.contains(p.getId());
				Predicate<BizAccount> predicateMy = p -> !CollectionUtils.isEmpty(mysetUpLists)
						&& mysetUpLists.contains(p.getId());
				List<BizAccount> dataSetted = Lists.newLinkedList();
				List<BizAccount> dataNotSetted = Lists.newLinkedList();
				data2.stream().forEach(p -> {
					p.setThirdLockOper(StringUtils.trimToEmpty(thirdLockOper.get(p.getId())));
					if (predicate.test(p)) {
						dataSetted.add(p);
						if (predicateMy.test(p)) {
							p.setIsSetUpFlag((byte) 2);
							// 我设定的则返回url 用于拆单出款
							BizThirdAccountOutputDTO thirdAccountOutputDTO = thirdAccountService
									.findByAccountId(p.getId());
							if (thirdAccountOutputDTO != null) {
								String url = thirdAccountOutputDTO.getThirdNameUrl();
								if (StringUtils.isNotBlank(url)) {
									url = url.startsWith("http://") ? url : "http://" + url;
									p.setBankNameUrl(url);
								}
							}
						} else {
							p.setIsSetUpFlag((byte) 1);
						}
					} else {
						dataNotSetted.add(p);
					}
				});
				data2 = Lists.newLinkedList();
				data2.addAll(dataNotSetted);
				// 已设定的数据放在后面
				data2.addAll(dataSetted);

				data2.stream().forEach(p -> {
					if (p.getId().equals(idUsedLastTime)) {
						usedLastTimeRecord.add(p);
					} else if (AccountStatus.Normal.getStatus().equals(p.getStatus())) {
						normalData.add(p);
					} else {
						abNormalData.add(p);
					}
				});
				data2 = Lists.newLinkedList();
				if (!CollectionUtils.isEmpty(usedLastTimeRecord))
					data2.addAll(usedLastTimeRecord);
				if (!CollectionUtils.isEmpty(normalData))
					data2.addAll(normalData);
				if (!CollectionUtils.isEmpty(abNormalData))
					data2.addAll(abNormalData);
			}

			responseData.setData(data2);
			int totalIdSize = 0;
			if (null != total_IdList && total_IdList.size() > 0) {
				totalIdSize = total_IdList.size();
			}
			int[] idSizes = { totalSize, onlineSize, stopSize, offlineSize };
			responseData.setPage(new Paging(page, buildHeader(accountIdList, filterToArray, idSizes)));
			if (isMysetUp) {
				responseData.getPage().getHeader().putAll(setUpMap);
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(String.format("%s，参数：%s，结果：%s", "账号分页获取", params, e.getMessage()), e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/showRebateUser")
	public String showRebateUser(@RequestParam(value = "pageNo") Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "handicapId", required = false) String handicapId,
			@RequestParam(value = "bankType", required = false) String bankType,
			@RequestParam(value = "typeToArray", required = false) Integer[] typeToArray,
			@RequestParam(value = "alias", required = false) String alias,
			@RequestParam(value = "account", required = false) String account,
			@RequestParam(value = "owner", required = false) String owner,
			@RequestParam(value = "subType", required = false) String subType,
			@RequestParam(value = "currSysLevel", required = false) String currSysLevel,
			@RequestParam(value = "statusToArray", required = false) Integer[] statusToArray,
			@RequestParam(value = "rebateUser", required = false) String rebateUser,
			@RequestParam(value = "startAmount", required = false) BigDecimal startamount,
			@RequestParam(value = "endAmount", required = false) BigDecimal endamount) throws JsonProcessingException {
		String params = buildParams().toString();
		logger.trace(String.format("%s，参数：%s", "账号分页获取", params));
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE);
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser == null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆");
				return mapper.writeValueAsString(responseData);
			}
			handicapId = handicapId.equals("") ? null : handicapId;
			bankType = bankType.equals("") ? null : bankType;
			alias = alias.equals("") ? null : alias;
			account = account.equals("") ? null : account;
			owner = owner.equals("") ? null : owner;
			subType = subType.equals("") ? null : subType;
			currSysLevel = currSysLevel.equals("") ? null : currSysLevel;
			rebateUser = rebateUser.equals("") ? null : rebateUser;
			Map<String, Object> mapp = accountService.showRebateUser(handicapId, bankType, typeToArray, alias, account,
					owner, currSysLevel, statusToArray, rebateUser, startamount, endamount, subType, pageRequest);
			Page<Object> page = (Page<Object>) mapp.get("Page");
			List<Object> AccountStatisticsList = (List<Object>) page.getContent();
			List<BizAccount> arrlist = new ArrayList<BizAccount>();
			for (int i = 0; i < AccountStatisticsList.size(); i++) {
				Object[] obj = (Object[]) AccountStatisticsList.get(i);
				BizHandicap bizHandicap = handicapService.findFromCacheById(null == obj[14] ? 0 : (int) obj[14]);
				BizAccount accountUser = new BizAccount();
				accountUser.setLimitPercentage(obj[0] == null ? BigDecimal.ZERO : new BigDecimal(obj[0].toString()));
				accountUser.setUid(obj[1] == null ? "" : (String) obj[1]);
				accountUser.setUserName(obj[2] == null ? "" : (String) obj[2]);
				accountUser.setMargin(obj[4] == null ? new BigDecimal(0) : new BigDecimal(obj[4].toString()));
				accountUser.setMobile(obj[3] == null ? "" : (String) obj[3]);
				accountUser.setBankBalance(obj[13] == null ? new BigDecimal(0) : new BigDecimal(obj[13].toString()));
				accountUser.setId((Integer) obj[6]);
				accountUser.setAccount((String) obj[7]);
				accountUser.setBankType((String) obj[22]);
				accountUser.setOwner((String) obj[11]);
				accountUser.setStatus((Integer) obj[9]);
				accountUser.setHandicapName(null == bizHandicap ? "" : bizHandicap.getName());
				accountUser.setAlias((String) obj[17]);
				accountUser.setCreateTimeStr(AccountStatus.findByStatus((Integer) obj[9]).getMsg());
				accountUser.setCurrSysLevelName(
						null == obj[26] ? "" : CurrentSystemLevel.valueOf((int) obj[26]).getName());
				accountUser.setRemark(StringUtils.isNotBlank((String) obj[5])
						? ((String) obj[5]).replace("\r\n", "<br>").replace("\n", "<br>")
						: "");
				// 查看是否存在扣佣金的数据
				BizDeductAmount deductAmount = rebateUserService.deductAmountByUid(accountUser.getUid());
				if (null != deductAmount) {
					accountUser.setMinInAmount(deductAmount.getAmount());
					accountUser.setCity(null != deductAmount.getRemark()
							? (deductAmount.getRemark()).replace("\r\n", "<br>").replace("\n", "<br>")
							: "");
				} else {
					accountUser.setMinInAmount(BigDecimal.ZERO);
					accountUser.setCity("");
				}

				arrlist.add(accountUser);
			}
			Map<String, Object> map = new LinkedHashMap<>();
			Object[] totalObj = (Object[]) mapp.get("Obj");
			map.put("arrlist", arrlist);
			map.put("page", new Paging(page));
			map.put("bankBalanceTotal", totalObj == null ? 0 : ((Object[]) totalObj[0])[0]);
			map.put("marginTotal", totalObj == null ? 0 : ((Object[]) totalObj[0])[1]);
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(String.format("%s，参数：%s，结果：%s", "账号分页获取", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 特殊权限修改用查询
	 *
	 * @param pageNo
	 * @param pageSize
	 * @param typeToArray
	 * @param statusToArray
	 * @param handicapId
	 * @param bankType
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/list4edit")
	public String list4edit(@RequestParam(value = "pageNo") Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "typeToArray", required = false) Integer[] typeToArray,
			@RequestParam(value = "statusToArray", required = false) Integer[] statusToArray,
			@RequestParam(value = "handicapId", required = false) Integer handicapId,
			@RequestParam(value = "bankType", required = false) String bankType) throws JsonProcessingException {
		String params = buildParams().toString();
		logger.trace(String.format("%s，参数：%s", "账号分页获取", params));
		try {
			GeneralResponseData<List<BizAccount>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser == null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆");
				return mapper.writeValueAsString(responseData);
			}
			List<SearchFilter> filterToList = DynamicSpecifications.build(request);
			if (typeToArray == null || typeToArray.length == 0 || statusToArray == null || statusToArray.length == 0) {
				return mapper.writeValueAsString(responseData);
			}
			if (typeToArray.length == 1) {
				filterToList.add(new SearchFilter("type", SearchFilter.Operator.EQ, typeToArray[0]));
			} else {
				filterToList.add(new SearchFilter("type", SearchFilter.Operator.IN, typeToArray));
			}
			if (statusToArray.length == 1) {
				filterToList.add(new SearchFilter("status", SearchFilter.Operator.EQ, statusToArray[0]));
			} else {
				filterToList.add(new SearchFilter("status", SearchFilter.Operator.IN, statusToArray));
			}
			// 只能查询到已拥有盘口下的信息
			List<BizHandicap> dataToList = dataPermissionService.getOnlyHandicapByUserId(sysUser);
			if (CollectionUtils.isEmpty(dataToList)) {
				return mapper.writeValueAsString(new GeneralResponseData<>(
						GeneralResponseData.ResponseStatus.FAIL.getValue(), "当前用户未配置盘口权限，请配置后再进行查询！"));
			} else {
				List<Integer> handicapIdToList = new ArrayList<Integer>();
				for (int i = 0; i < dataToList.size(); i++) {
					if (null != dataToList.get(i) && null != dataToList.get(i).getId()) {
						handicapIdToList.add(dataToList.get(i).getId());
					}
				}
				if (CollectionUtils.isEmpty(handicapIdToList)) {
					return mapper.writeValueAsString(responseData);
				} else {
					filterToList
							.add(new SearchFilter("handicapId", SearchFilter.Operator.IN, handicapIdToList.toArray()));
				}
			}
			if (handicapId != null) {
				filterToList.add(new SearchFilter("handicapId", SearchFilter.Operator.EQ, handicapId));
			}
			PageRequest pageRequest;
			if (ArrayUtils.contains(statusToArray, AccountStatus.Delete.getStatus())) {
				pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
						Sort.Direction.DESC, "updateTime");
			} else {
				pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
						Sort.Direction.ASC, "handicapId", "status", "type");
			}
			SearchFilter[] filterToArray = filterToList.toArray(new SearchFilter[filterToList.size()]);
			Specification<BizAccount> specif = DynamicSpecifications.build(BizAccount.class, filterToArray);
			Page<BizAccount> page;
			List<Integer> accountIdList = accountService.findAccountIdList(filterToArray);
			page = accountService.findPage(sysUser, specif, pageRequest);
			responseData.setData(page.getContent());
			responseData.setPage(new Paging(page, buildHeader(accountIdList, filterToArray, null)));
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(String.format("%s，参数：%s，结果：%s", "账号分页获取", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 获取转账账号列表
	 */
	@RequestMapping("/list4Trans")
	public String list4Trans(@RequestParam(value = "pageNo") int pageNo, @RequestParam(value = "type") Integer type,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "fromId") Integer fromId, @RequestParam(value = "bankType") String bankType,
			@RequestParam(value = "statusToArray", required = false) Integer[] statusToArray,
			@RequestParam(value = "lockToArray", required = false) Integer[] lockToArray,
			@RequestParam(value = "bankTypeToArray", required = false) String[] bankTypeToArray)
			throws JsonProcessingException {
		String params = buildParams().toString();
		logger.trace(String.format("%s，参数：%s", "账号分页获取", params));
		try {
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			GeneralResponseData<List<BizAccount>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			// 封装查询参数
			List<SearchFilter> filterToList = DynamicSpecifications.build(request);
			filterToList.add(new SearchFilter("type", SearchFilter.Operator.EQ, type));
			if (statusToArray.length == 1) {
				filterToList.add(new SearchFilter("status", SearchFilter.Operator.EQ, statusToArray[0]));
			} else {
				filterToList.add(new SearchFilter("status", SearchFilter.Operator.IN, statusToArray));
			}
			if (bankTypeToArray != null && bankTypeToArray.length != 0) {
				filterToList.add(new SearchFilter("bankType", SearchFilter.Operator.IN, bankTypeToArray));
			}
			if (bankType != null && StringUtils.isNoneBlank(bankType)) {
				filterToList.add(new SearchFilter("bankType", SearchFilter.Operator.LIKE, bankType));
			}
			// 只能查询自己区域的账号
			if (!operator.getCategory().equals(-1)) {
				List<SysUserProfile> manilaUserPro = sysUserProfileService.findByPropertyKey("HANDICAP_MANILA_ZONE");
				String manilaHandicap = manilaUserPro.get(0).getPropertyValue();
				List<SysUserProfile> taiWanuserPro = sysUserProfileService.findByPropertyKey("HANDICAP_TAIWAN_ZONE");
				String taiwanHandicap = taiWanuserPro.get(0).getPropertyValue();
				BizHandicap bizHandicap = handicapService.findFromCacheById(operator.getHandicap());
				List<Integer> handicapIds = handicapService
						.findByZone(bizHandicap == null ? "0" : bizHandicap.getId().toString());
				String handicapId = CollectionUtils.isEmpty(handicapIds) ? "0" : handicapIds.get(0).toString();
				if (ArrayUtils.contains(manilaHandicap.substring(1, manilaHandicap.length()).split(";"),
						(operator.getCategory() > 400 ? (operator.getCategory() - 400) + "" : handicapId))) {
					String[] handicaps = manilaHandicap.substring(1, manilaHandicap.length()).split(";");
					filterToList.add(new SearchFilter("handicapId", SearchFilter.Operator.IN, handicaps));
				}
				if (ArrayUtils.contains(taiwanHandicap.substring(1, taiwanHandicap.length()).split(";"),
						(operator.getCategory() > 400 ? (operator.getCategory() - 400) + "" : handicapId))) {
					String[] handicaps = taiwanHandicap.substring(1, taiwanHandicap.length()).split(";");
					filterToList.add(new SearchFilter("handicapId", SearchFilter.Operator.IN, handicaps));
				}
			}

			if (lockToArray != null && lockToArray.length == 1) {
				int lock = lockToArray[0];
				List<Integer> accountToList = accountService.findLockAccountIdList(true, operator.getId());
				if (lock == 0 && !CollectionUtils.isEmpty(accountToList)) {
					filterToList.add(new SearchFilter("id", SearchFilter.Operator.NOTIN, accountToList.toArray()));
				} else if (lock == 1 && !CollectionUtils.isEmpty(accountToList)) {
					filterToList.add(new SearchFilter("id", SearchFilter.Operator.IN, accountToList.toArray()));
				} else if (lock == 1 && CollectionUtils.isEmpty(accountToList)) {
					return mapper.writeValueAsString(responseData);
				}
			}
			// 封装分页参数
			PageRequest pageable = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					Sort.Direction.ASC, "balance");
			SearchFilter[] filterToArray = filterToList.toArray(new SearchFilter[filterToList.size()]);
			Specification<BizAccount> specif = DynamicSpecifications.build(BizAccount.class, filterToArray);
			// 获取数据
			List<Integer> accountIdList = accountService.findAccountIdList(filterToArray);
			Page<BizAccount> page = accountService.findPage(operator, specif, pageable);
			List<BizAccount> content = buildTransAmount(fromId, page.getContent(), operator);
			for (int i = 0; i < content.size(); i++) {
				BizAccount account = content.get(i);
				if (allocateTransferService.isLockThirdTrans(account.getId().toString(), operator.getUid())) {
					if (allocateTransferService.isLockThirdTransByoperator(account.getId().toString(),
							operator.getUid())) {
						account.setLockByOperator(1);
					} else {
						continue;
					}
				}
			}
			responseData.setData(content);
			responseData.setPage(new Paging(page, buildHeader(accountIdList, filterToArray, null)));
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error(String.format("%s，参数：%s，结果：%s", "账号分页获取", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 微信下发，支付宝下发 ：绑定账号获取
	 */
	@RequiresPermissions(value = { "IncomeAsignAlipay:Retrieve", "IncomeAsignWechat:Retrieve" }, logical = Logical.OR)
	@RequestMapping("/list4BindAliAndBindWechat")
	public String list4BindAliAndBindWechat(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "type") int type,
			@RequestParam(value = "statusToArray", required = false) Integer[] statusToArray,
			@RequestParam(value = "handicapId", required = false) Integer[] handicapId,
			@RequestParam(value = "levelId", required = false) Integer levelId) throws JsonProcessingException {
		try {
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			GeneralResponseData<List<Map<String, Object>>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			List<SearchFilter> filterToList = DynamicSpecifications.build(request);
			if (AccountType.BindAli.getTypeId() != type && AccountType.BindWechat.getTypeId() != type) {
				return mapper.writeValueAsString(responseData);
			}
			if (handicapId != null) {
				int incomeType = type == AccountType.BindAli.getTypeId() ? AccountType.InAli.getTypeId()
						: AccountType.InWechat.getTypeId();
				List<Integer> bindingIdList = build4BindingIdList(handicapId, levelId, incomeType);
				if (CollectionUtils.isEmpty(bindingIdList)) {
					return mapper.writeValueAsString(responseData);
				}
				filterToList.add(new SearchFilter("id", SearchFilter.Operator.IN, bindingIdList.toArray()));
			}
			filterToList.add(new SearchFilter("type", SearchFilter.Operator.EQ, type));
			filterToList.add(new SearchFilter("status", SearchFilter.Operator.IN, statusToArray));
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					Sort.Direction.ASC, "status");
			SearchFilter[] filterToArray = filterToList.toArray(new SearchFilter[filterToList.size()]);
			Specification<BizAccount> specif = DynamicSpecifications.build(BizAccount.class, filterToArray);
			Page<BizAccount> p = accountService.findPage(sysUser, specif, pageRequest);
			@SuppressWarnings("unchecked")
			PageImpl<Map<String, Object>> page = new PageImpl(build4AliAndWechat(p.getContent()), pageRequest,
					p.getTotalPages());
			responseData.setData(page.getContent());
			List<Integer> accountIdList = accountService.findAccountIdList(filterToArray);
			responseData.setPage(new Paging(page, buildHeader(accountIdList, filterToArray, null)));
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			String params = buildParams().toString();
			logger.error(String.format("%s，参数：%s，结果：%s", "微信下发，支付宝下发 ：绑定账号获取", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 创建账号
	 */
	@RequiresPermissions(value = { "IncomeAccountComp:addIncomeAccount", "IncomeAccountThird:Create",
			"IncomeAccountIssue:Create", "OutwardAccountBankEnabled:Create", "OutwardAccountThirdEnabled:Create",
			"OutwardAccountStorageReserved:Create", "OutwardAccountStorageCash:Create", "OutwardAccountAll:Create",
			"AccountOutComp:Create" }, logical = Logical.OR)
	@RequestMapping("/create")
	public String create(@Valid BizAccount vo,
			@RequestParam(value = "handicapCode", required = false) String handicapCode)
			throws JsonProcessingException {
		String params = buildParams().toString();
		try {
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (operator == null) {
				throw new Exception("创建账号失败,重新登陆!");
			}
			BizHandicap bizHandicap;
			if (StringUtils.isNotBlank(handicapCode)) {
				bizHandicap = handicapService.findFromCacheByCode(handicapCode);
				if (bizHandicap == null) {
					throw new Exception("创建账号失败,盘口不存在!");
				}
				vo.setHandicapId(bizHandicap.getId());
			}
			logger.info(String.format("%s，操作人员：%s，参数：%s", "账号更新", operator.getUid(), params));
			if (StringUtils.isBlank(vo.getAccount()) || vo.getType() == null) {
				logger.error("创建账号失败, 账号，或Type为空.");
				throw new Exception("创建账号失败, 账号，或Type为空");
			}
			// 任何状态的账号不可重复
			List<Integer> hisList = accountService.findAccountIdList(
					new SearchFilter("account", SearchFilter.Operator.EQ, StringUtils.trimToEmpty(vo.getAccount())));
			if (!CollectionUtils.isEmpty(hisList)) {
				logger.error("创建账号失败，该账号已存在");
				throw new Exception("该账号已存在.");
			}
			if (vo.getLimitOutOne() == null) {
				Map<String, String> systemSetting = new HashMap<>();
				systemSetting = MemCacheUtils.getInstance().getSystemProfile();
				vo.setLimitOutOne(Integer.parseInt(systemSetting.get("OUTDRAW_LIMIT_OUT_ONE")));
			}
			if (null != vo.getLimitOutOne() && null != vo.getLimitOutOneLow()
					&& (vo.getLimitOutOne() < vo.getLimitOutOneLow())) {
				logger.error("“最高出款限额”必须大于“最低出款限额”");
				throw new Exception("“最高出款限额”必须大于“最低出款限额”");
			}
			Date date = new Date();
			vo.setCreateTime(date);
			vo.setFlag(AccountFlag.PC.getTypeId());
			vo.setOutEnable((byte) 0);
			vo.setStatus(AccountStatus.Inactivated.getStatus());
			vo.setUpdateTime(date);
			vo.setBankBalance(vo.getBalance());
			vo.setCreator(operator.getId());
			vo.setModifier(operator.getId());
			vo.setRemark(CommonUtils.genRemark(null, "【账号创建】" + (vo.getRemark() != null ? vo.getRemark() : ""), date,
					operator.getUid()));
			// 是银行账号时 自动生成编号
			if (!Arrays
					.asList(new Integer[] { AccountType.InThird.getTypeId(), AccountType.InAli.getTypeId(),
							AccountType.InWechat.getTypeId(), AccountType.OutThird.getTypeId() })
					.contains(vo.getType())) {
				// 编号六位数，跳过为4的数字 从100000开始递增
				String maxAlias = accountService.getMaxAlias();
				if (StringUtils.isEmpty(maxAlias) || maxAlias.equals("0")) {
					vo.setAlias("100000");
				} else {
					int alias = Integer.parseInt(maxAlias) + 1;
					vo.setAlias(Integer.toString(alias).replace("4", "5"));
				}
			} else {
				// 防止接口传入alias被保存
				vo.setAlias(null);
			}
			BizAccount data = accountService.create(null, vo);
			accountExtraService.addAccountExtraLog(vo.getId(), operator.getUid());
			logger.debug("create>> id {}", data.getId());
			accountService.broadCast(data);
			GeneralResponseData<BizAccount> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			responseData.setData(data);
			if (Objects.nonNull(data) && Objects.equals(data.getFlag(), 1)) {
				accountService.setModel(data.getId(), 1, 1);
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error(String.format("%s，参数：%s，结果：%s", "创建账号", params, e.getMessage()));
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getMessage()));
		}
	}

	/**
	 * 创建账号
	 */
	@RequestMapping("/createAccount14")
	public String createAccount14(@Valid BizAccount vo, @RequestParam(value = "handicapCode") String handicapCode)
			throws JsonProcessingException {
		String params = buildParams().toString();
		String message = "入款账号客户绑定卡,新增后同步到本系统失败,";
		try {
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (operator == null) {
				logger.info(message + "请重新登陆");
				throw new Exception(message + "请重新登陆");
			}
			BizHandicap bizHandicap = handicapService.findFromCacheByCode(handicapCode);
			if (bizHandicap == null) {
				logger.info(message + "盘口不存在");
				throw new Exception(message + "盘口不存在");
			}
			vo.setHandicapId(bizHandicap.getId());
			logger.info(String.format("%s，操作人员：%s，参数：%s", "入款账号客户绑定卡存储", operator.getUid(), params));
			List<Integer> hisList = accountService.findAccountIdList(
					new SearchFilter("account", SearchFilter.Operator.EQ, StringUtils.trimToEmpty(vo.getAccount())));
			if (!CollectionUtils.isEmpty(hisList)) {
				logger.info(message + "账号已存在");
				throw new Exception(message + "账号已存在");
			}
			// 单笔出款限额
			if (vo.getLimitOutOne() == null) {
				Map<String, String> systemSetting = new HashMap<>();
				systemSetting = MemCacheUtils.getInstance().getSystemProfile();
				vo.setLimitOutOne(Integer.parseInt(systemSetting.get("OUTDRAW_LIMIT_OUT_ONE")));
			}
			if (StringUtils.isNotEmpty(vo.getHook())) {
				vo.setHook_(FundTransferEncrypter.encryptDb(StringUtils.trimToEmpty(vo.getHook())));
			}
			if (StringUtils.isNotEmpty(vo.getHub())) {
				vo.setHub_(FundTransferEncrypter.encryptDb(StringUtils.trimToEmpty(vo.getHub())));
			}
			if (StringUtils.isNotEmpty(vo.getBing())) {
				vo.setBing_(FundTransferEncrypter.encryptDb(StringUtils.trimToEmpty(vo.getBing())));
			}
			int flag = 1;
			vo.setFlag(flag);
			if (null == vo.getStatus()) {
				vo.setStatus(AccountStatus.StopTemp.getStatus());
			}
			vo.setType(AccountType.BindCustomer.getTypeId());
			Date date = new Date();
			vo.setBalance(new BigDecimal("0.0"));
			vo.setBankBalance(new BigDecimal("0.0"));
			vo.setCreateTime(date);
			vo.setUpdateTime(date);
			vo.setCreator(operator.getId());
			vo.setModifier(operator.getId());
			vo.setRemark(CommonUtils.genRemark(null,
					"【新增账号同步到出入款系统】<br/>" + (vo.getRemark() != null ? vo.getRemark() : ""), date, operator.getUid()));
			// 编号六位数，跳过为4的数字 从100000开始递增
			String maxAlias = accountService.getMaxAlias();
			if (StringUtils.isEmpty(maxAlias) || maxAlias.equals("0")) {
				vo.setAlias("100000");
			} else {
				int alias = Integer.parseInt(maxAlias) + 1;
				vo.setAlias(Integer.toString(alias).replace("4", "5"));
			}
			BizAccount data = accountService.create(null, vo);
			accountExtraService.addAccountExtraLog(vo.getId(), operator.getUid());
			logger.debug("createAccount14>> id {}", data.getId());
			accountService.broadCast(data);
			// 新增时:模式设置
			if (flag == 1) {
				accountService.setModel(data.getId(), 1, 1);
			}
			GeneralResponseData<BizAccount> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			responseData.setData(data);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error(String.format("%s，参数：%s，结果：%s", message, params, e.getMessage()));
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getMessage()));
		}
	}

	/**
	 * 客户资料管理 银行账号更新
	 */
	@RequestMapping("/updateAccount14Info")
	public String updateAccount14Info(@Valid BizAccount vo) throws JsonProcessingException {
		String params = buildParams().toString();
		String message = "入款账号客户绑定卡，修改后同步到本系统失败,";
		try {
			GeneralResponseData<BizAccount> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			BizAccount db = accountService.getById(vo.getId());
			if (null == db) {
				logger.info(message + "账号不存在");
				responseData.setMessage(message + "账号不存在");
				return mapper.writeValueAsString(responseData);
			}
			logger.info(String.format("%s，操作人员：%s，参数：%s", "款账号客户绑定卡执行更新", operator.getUid(), db));
			if (!db.getType().equals(AccountType.BindCustomer.getTypeId())) {
				logger.info(message + "账号类型错误，不允许修改");
				throw new Exception(message + "账号类型错误，不允许修改");
			}
			// 新旧账号不一致 查看新账号是否已存在
			if (!db.getAccount().equals(vo.getAccount())) {
				List<Integer> hisList = accountService.findAccountIdList(new SearchFilter("account",
						SearchFilter.Operator.EQ, StringUtils.trimToEmpty(vo.getAccount())));
				if (!CollectionUtils.isEmpty(hisList)) {
					logger.info(message + "账号已存在");
					throw new Exception(message + "账号已存在");
				}
			}
			BizAccount oldAccount = new BizAccount();
			BeanUtils.copyProperties(oldAccount, db);
			db.setAccount(StringUtils.isBlank(vo.getAccount()) ? db.getAccount() : vo.getAccount());
			db.setBankName(StringUtils.isBlank(vo.getBankName()) ? db.getBankName() : vo.getBankName());
			db.setOwner(StringUtils.isBlank(vo.getOwner()) ? db.getOwner() : vo.getOwner());
			db.setBankType(StringUtils.isBlank(vo.getBankType()) ? db.getBankType() : vo.getBankType());
			db.setMobile(StringUtils.isBlank(vo.getMobile()) ? db.getMobile() : vo.getMobile());
			db.setLimitBalance(vo.getLimitBalance() == null ? db.getLimitBalance() : vo.getLimitBalance());
			vo.setRemark(CommonUtils.genRemark(null, (vo.getRemark() != null ? vo.getRemark() : ""), new Date(),
					operator.getUid()));
			db.setUpdateTime(new Date());
			if (StringUtils.isEmpty(db.getAlias())) {
				// 编号六位数，跳过为4的数字 从100000开始递增
				String maxAlias = accountService.getMaxAlias();
				if (StringUtils.isEmpty(maxAlias) || maxAlias.equals("0")) {
					db.setAlias("100000");
				} else {
					int alias = Integer.parseInt(maxAlias) + 1;
					db.setAlias(Integer.toString(alias).replace("4", "5"));
				}
			}
			accountExtraService.saveAccountExtraLog(oldAccount, db, operator.getUid());
			hostMonitorService.update(db);
			logger.debug("updateAccount14Info>> id {}", db.getId());
			accountService.broadCast(db);
			cabanaService.updAcc(db.getId());
			responseData.setData(db);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(String.format("%s，参数：%s，结果：%s", "账号更新", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getMessage()));
		}
	}

	/**
	 * 入款账号客户绑定卡 更新 状态 密码 内外层
	 *
	 * @param vo
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/updateAccount14Other")
	public String updateAccount14Other(@Valid BizAccount vo) throws JsonProcessingException {
		String params = buildParams().toString();
		String message = "入款账号客户绑定卡，修改后同步到本系统失败,";
		try {
			GeneralResponseData<BizAccount> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			logger.info(String.format("%s，操作人员：%s，参数：%s", "款账号客户绑定卡修改 状态/密码/内外层级", operator.getUid(), params));
			List<BizAccount> list = accountService.findByAccount(vo.getAccount());
			if (CollectionUtils.isEmpty(list)) {
				// 账号不存在
				logger.info(message + "账号不存在");
				throw new Exception(message + "账号不存在");
			}
			BizAccount db = list.get(0);
			logger.info(String.format("%s，操作人员：%s，参数：%s", "款账号客户绑定卡执行更新", operator.getUid(), db));
			if (!db.getType().equals(AccountType.BindCustomer.getTypeId())) {
				logger.info(message + "账号类型错误，不允许修改");
				throw new Exception(message + "账号类型错误，不允许修改");
			}
			BizAccount oldAccount = new BizAccount();
			BeanUtils.copyProperties(oldAccount, db);
			db.setStatus(vo.getStatus() == null ? db.getStatus() : vo.getStatus());
			db.setCurrSysLevel(vo.getCurrSysLevel() == null ? db.getCurrSysLevel() : vo.getCurrSysLevel());
			db.setMobile(StringUtils.isBlank(vo.getMobile()) ? db.getMobile() : vo.getMobile());
			db.setUpdateTime(new Date());
			if (StringUtils.isNotEmpty(vo.getHook())) {
				db.setHook_(FundTransferEncrypter.encryptDb(StringUtils.trimToEmpty(vo.getHook())));
			}
			if (StringUtils.isNotEmpty(vo.getHub())) {
				db.setHub_(FundTransferEncrypter.encryptDb(StringUtils.trimToEmpty(vo.getHub())));
			}
			if (StringUtils.isNotEmpty(vo.getBing())) {
				db.setBing_(FundTransferEncrypter.encryptDb(StringUtils.trimToEmpty(vo.getBing())));
			}
			accountExtraService.saveAccountExtraLog(oldAccount, db, operator.getUid());
			hostMonitorService.update(db);
			logger.debug("updateAccount14Other>> id {}", db.getId());
			accountService.broadCast(db);
			cabanaService.updAcc(db.getId());
			responseData.setData(db);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(String.format("%s，参数：%s，结果：%s", "账号更新", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getMessage()));
		}
	}

	/**
	 * 账号更新
	 */
	@RequiresPermissions(value = { "IncomeAccountComp:Update", "IncomeAccountThird:Update", "IncomeAccountIssue:Update",
			"OutwardAccountBankEnabled:Update", "OutwardAccountThirdEnabled:Update",
			"OutwardAccountStorageReserved:Update", "OutwardAccountStorageCash:Update", "OutwardAccountAll:Update",
			"IncomeAuditComp:UpdateCompBank", "IncomeAuditComp:UpdateCompAlipay", "IncomeAuditComp:UpdateCompWechat",
			"IncomeAccountIssue:UpdateStatus", "AccountOutComp:Update" }, logical = Logical.OR)
	@RequestMapping("/update")
	public String update(@Valid BizAccount vo) throws JsonProcessingException {
		String params = buildParams().toString();
		try {
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			logger.info(String.format("%s，操作人员：%s，参数：%s", "账号更新", operator.getUid(), params));
			if (vo.getId() == null) {
				logger.error("修改账号信息 id不能为空");
				throw new Exception("id不能为空.");
			}
			if (vo.getMobile() != null && vo.getMobile().length() > 11) {
				logger.error("修改账号信息 手机长度不能超过11位");
				throw new Exception("手机长度不能超过11位");
			}
			BizAccount db = accountService.getById(vo.getId());
			Integer dbStatus = Objects.isNull(db) ? null : db.getStatus();
			if (Objects.nonNull(db) && Objects.equals(db.getStatus(), AccountStatus.Activated.getStatus())
					&& !Objects.equals(db.getStatus(), vo.getStatus())) {
				/**
				 * if (db.getBankBalance() == null ||
				 * db.getBankBalance().compareTo(BigDecimal.ZERO) == 0) { throw new
				 * Exception("银行余额为0，不能启用"); }
				 **/
				db.setBalance(db.getBankBalance());
				vo.setBankBalance(db.getBankBalance());
				vo.setBalance(db.getBankBalance());
			}
			// 如果是返利网账号 且是已激活 且未初始化额度 不允许转其它状态
			// if (db.getFlag() == 2 && db.getStatus().intValue() ==
			// AccountStatus.Activated.getStatus().intValue()
			// && (Objects.isNull(db.getPeakBalance()) || db.getPeakBalance() <=
			// 0)) {
			// throw new Exception("未初始化信用额度不能转其它状态！");
			// }
			BizAccount oldAccount = new BizAccount();
			org.springframework.beans.BeanUtils.copyProperties(db, oldAccount);
			if (db == null) {
				logger.error("修改账号信息 所修改的数据不存在");
				throw new Exception("所修改的数据不存在");
			}
			// 云闪付
			if (!ObjectUtils.isEmpty(vo.getType()) && !ObjectUtils.isEmpty(vo.getSubType())
					&& vo.getType().equals(AccountType.InBank.getTypeId()) && vo.getSubType() == 3) {
				// 新旧类型对比是否做出更新
				if (ObjectUtils.isEmpty(oldAccount.getType()) || ObjectUtils.isEmpty(oldAccount.getSubType())
						|| !oldAccount.getType().equals(vo.getType())
						|| !oldAccount.getSubType().equals(vo.getSubType())) {
					logger.error("不允许修改为云闪付入款卡");
					throw new Exception("不允许修改为云闪付入款卡");
				}
			}
			// 无层级修改为有层级 或者修改未传入层级（会以之前的层级为准）都不需要校验
			if (null != vo.getCurrSysLevel() && null != db.getCurrSysLevel()
					&& vo.getCurrSysLevel().intValue() != db.getCurrSysLevel().intValue()) {
				if (db.getCurrSysLevel().equals(CurrentSystemLevel.Outter.getValue())
						|| db.getCurrSysLevel().equals(CurrentSystemLevel.Designated.getValue())) {
					logger.error("外层或者指定层的账号不允许修改为内层和中层");
					throw new Exception("外层或者指定层的账号不允许修改为内层和中层");
				}
			}
			if (null != vo.getLimitOutOne() && null != vo.getLimitOutOneLow()
					&& (vo.getLimitOutOne() < vo.getLimitOutOneLow())) {
				logger.error("“最高出款限额”必须大于“最低出款限额”");
				throw new Exception("“最高出款限额”必须大于“最低出款限额”");
			}
			// if (vo.getFlag() != null && vo.getFlag().equals(1)) {
			// checkIp();
			// }
			// 顺序勿更换
			db.setStatus(vo.getStatus() == null ? db.getStatus() : vo.getStatus());
			// 返利网不允许修改基本信息
			if (db.getFlag() == null || (!db.getFlag().equals(2))) {
				db.setAccount(StringUtils.isBlank(vo.getAccount()) ? db.getAccount() : vo.getAccount());
				db.setBankName(StringUtils.isBlank(vo.getBankName()) ? db.getBankName() : vo.getBankName());
				db.setOwner(StringUtils.isBlank(vo.getOwner()) ? db.getOwner() : vo.getOwner());
				db.setBankType(StringUtils.isBlank(vo.getBankType()) ? db.getBankType() : vo.getBankType());
				db.setBalance(vo.getBalance() == null ? db.getBalance() : vo.getBalance());
				db.setPeakBalance(vo.getPeakBalance() == null ? db.getPeakBalance() : vo.getPeakBalance());
				// db.setFlag(vo.getFlag() == null ? db.getFlag() :
				// vo.getFlag());
				db.setMobile(vo.getMobile() == null ? db.getMobile() : vo.getMobile());
				// 账号类型变更时
				if (!oldAccount.getType().equals(db.getType())) {
					// 清空持卡人字段并同步更新redis（否则会影响工具端报警功能）
					db.setHolder(null);
					// 如果原来是在用 状态变为停用
					if (oldAccount.getStatus().equals(AccountStatus.Normal.getStatus())) {
						db.setStatus(AccountStatus.StopTemp.getStatus());
					}
				}
			}
			db.setProvince(ObjectUtils.isEmpty(vo.getProvince()) ? db.getProvince() : vo.getProvince());
			db.setCity(ObjectUtils.isEmpty(vo.getCity()) ? db.getCity() : vo.getCity());
			db.setHandicapId(vo.getHandicapId() == null ? db.getHandicapId() : vo.getHandicapId());
			db.setType(vo.getType() == null ? db.getType() : vo.getType());
			db.setSubType(vo.getSubType() == null ? db.getSubType() : vo.getSubType());
			db.setCurrSysLevel(vo.getCurrSysLevel() == null ? db.getCurrSysLevel() : vo.getCurrSysLevel());
			db.setLimitIn(vo.getLimitIn() == null ? db.getLimitIn() : vo.getLimitIn());
			db.setLimitOut(vo.getLimitOut() == null ? db.getLimitOut() : vo.getLimitOut());
			db.setLimitOutOne(vo.getLimitOutOne() == null ? db.getLimitOutOne() : vo.getLimitOutOne());
			db.setLimitOutOneLow(vo.getLimitOutOneLow() == null ? db.getLimitOutOneLow() : vo.getLimitOutOneLow());
			db.setLimitOutCount(vo.getLimitOutCount() == null ? db.getLimitOutCount() : vo.getLimitOutCount());
			db.setMinInAmount(vo.getMinInAmount() == null ? db.getMinInAmount() : vo.getMinInAmount());
			db.setOutEnable(vo.getOutEnable() == null ? db.getOutEnable() : vo.getOutEnable());
			if (db.getLimitOutOne() == null) {
				Map<String, String> systemSetting = new HashMap<>();
				systemSetting = MemCacheUtils.getInstance().getSystemProfile();
				db.setLimitOutOne(Integer.parseInt(systemSetting.get("OUTDRAW_LIMIT_OUT_ONE")));
			}
			db.setLowestOut(vo.getLowestOut() == null ? db.getLowestOut() : vo.getLowestOut());
			// 是银行账号 且查出的数据无编号时，自动生成编号
			if (StringUtils.isEmpty(db.getAlias()) && !Arrays
					.asList(new Integer[] { AccountType.InThird.getTypeId(), AccountType.InAli.getTypeId(),
							AccountType.InWechat.getTypeId(), AccountType.OutThird.getTypeId() })
					.contains(db.getType())) {
				// 编号六位数，跳过为4的数字 从100000开始递增
				String maxAlias = accountService.getMaxAlias();
				if (StringUtils.isEmpty(maxAlias) || maxAlias.equals("0")) {
					db.setAlias("100000");
				} else {
					int alias = Integer.parseInt(maxAlias) + 1;
					db.setAlias(Integer.toString(alias).replace("4", "5"));
				}
			}
			// 如果是第三方账号可以修改余额
			if (AccountType.InThird.getTypeId().equals(db.getType())) {
				db.setBankBalance(vo.getBankBalance() == null ? db.getBankBalance() : vo.getBankBalance());
			}
			db.setModifier(operator.getId());
			db.setLimitBalance(vo.getLimitBalance() == null ? db.getLimitBalance() : vo.getLimitBalance());
			db.setUpdateTime(new Date());
			accountExtraService.saveAccountExtraLog(oldAccount, db, operator.getUid());
			hostMonitorService.update(db);
			// 如果是第三方账号 去保存汇率
			if (AccountType.InThird.getTypeId().equals(db.getType())) {
				if (vo.getRate() != null || vo.getRateValue() != null) {
					db.setRate(vo.getRate());
					db.setRateType(vo.getRateType());
					db.setRateValue(vo.getRateValue());
					accountExtraService.saveAccountRate(db);
				}
				// 冻结的时候添加到待处理业务表、如果存在没有处理完的则不添加
				if (vo.getFreezeTe() == 1) {
					BizHandicap handicap = handicapService.findFromCacheById(db.getHandicapId());
					if (ObjectUtils.isEmpty(handicap)) {
						return mapper.writeValueAsString(
								new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "盘口不存在"));
					}
					boolean flag = accountService.syncThirdStatus(Integer.parseInt(handicap.getCode()), 2,
							db.getAccount(), db.getBankType());
					if (flag) {
						db.setStatus(AccountStatus.Freeze.getStatus());
						String remark = CommonUtils.genRemark(
								StringUtils.isNotBlank(db.getRemark()) ? db.getRemark() : "", vo.getRemark(),
								new Date(), operator.getUid());
						db.setRemark(remark);
						vo.setAccountFeeConfig(null);
						int count = finLessStatService.findCountsById(vo.getId(), "portion");
						if (count <= 0)
							finLessStatService.addTrace(vo.getId(), db.getBankBalance());
					}
				}
			}
			logger.debug("update>> id {}", db.getId());
			accountService.broadCast(db);
			cabanaService.refreshAcc(db.getId());
			boolean updAllocate = Objects.nonNull(db) && Objects.nonNull(oldAccount)
					&& (!Objects.equals(db.getType(), oldAccount.getType())
							|| !Objects.equals(db.getStatus(), oldAccount.getStatus()));
			if (updAllocate)
				incomeAccountAllocateService.update(db.getId(), db.getType(), db.getStatus());
			GeneralResponseData<BizAccount> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			if (buildParams().containsKey("zerocredits")) {
				accMoreSer.setToZeroCredit(operator, db.getMobile());
			}
			// 6720 (6739) 更新第三方下发手续费规则
			if (!ObjectUtils.isEmpty(vo.getAccountFeeConfig())) {
				AccountBaseInfo accountBaseInfo = accountService.getFromCacheById(vo.getId());
				if (!ObjectUtils.isEmpty(accountBaseInfo)) {
					BizAccount vot = new BizAccount();
					vot.setHandicapId(accountBaseInfo.getHandicapId());
					vot.setId(accountBaseInfo.getId());
					vot.setStatus(accountBaseInfo.getStatus());
					vot.setBankType(accountBaseInfo.getBankType());
					vot.setAccount(accountBaseInfo.getAccount());
					accountFeeService.update(vot, operator.getUsername(), vo.getAccountFeeConfig().getFeeType(),
							vo.getAccountFeeConfig().getCalFeeType(),
							ObjectUtils.isEmpty(vo.getAccountFeeConfig().getCalFeePercent()) ? null
									: new BigDecimal(vo.getAccountFeeConfig().getCalFeePercent()),
							vo.getAccountFeeConfig().getCalFeeLevelType());
				}
			}
			if (Objects.equals(dbStatus, AccountStatus.Activated.getStatus())
					&& !Objects.equals(vo.getStatus(), AccountStatus.Activated.getStatus())) {
				systemAccountManager.rpush(new SysBalPush(db.getId(), SysBalPush.CLASSIFY_INIT,
						new ReportInitParam(db.getId(), operator.getId(), "启用已激活账号")));
				logger.info("SB{}  系统账目初始化 >> 启用已激活账号", db.getId());
			}
			responseData.setData(db);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(String.format("%s，参数：%s，结果：%s", "账号更新", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getMessage()));
		}
	}

	/**
	 * 账号特殊信息更新
	 */
	@RequiresPermissions(value = "UpdateSpecialAccount:UpdateSpecialAccount")
	@RequestMapping("/updateSpecialAccount")
	public String updateSpecialAccount(@Valid BizAccount vo) throws JsonProcessingException {
		String params = buildParams().toString();
		try {
			String resultMessage = "";
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			logger.info(String.format("%s，操作人员：%s，参数：%s", "账号特殊信息更新", operator.getUid(), params));
			if (vo.getId() == null) {
				logger.error("修改账号信息 id不能为空");
				throw new Exception("id不能为空.");
			}
			if (vo.getHandicapId() == null) {
				logger.error("修改账号信息 盘口不能为空");
				throw new Exception("盘口不能为空.");
			}
			if (vo.getType() == null) {
				logger.error("修改账号信息 类型不能为空");
				throw new Exception("类型不能为空.");
			}
			BizAccount db = accountService.getById(vo.getId());
			boolean updAllocate = Objects.nonNull(db) && Objects.nonNull(vo)
					&& (Objects.nonNull(vo.getType()) && !Objects.equals(db.getType(), vo.getType())
							|| Objects.nonNull(vo.getStatus()) && !Objects.equals(db.getStatus(), vo.getStatus()));
			BizAccount oldAccount = new BizAccount();
			BeanUtils.copyProperties(oldAccount, db);
			if (db == null) {
				logger.error("修改账号信息 所修改的数据不存在");
				throw new Exception("所修改的数据不存在.");
			}
			db.setHandicapId(vo.getHandicapId() == null ? db.getHandicapId() : vo.getHandicapId());
			db.setAccount(StringUtils.isBlank(vo.getAccount()) ? db.getAccount() : vo.getAccount());
			db.setType(vo.getType() == null ? db.getType() : vo.getType());
			db.setSubType(vo.getSubType() == null ? db.getSubType() : vo.getSubType());
			db.setCurrSysLevel(vo.getCurrSysLevel() == null ? db.getCurrSysLevel() : vo.getCurrSysLevel());
			db.setModifier(operator.getId());
			db.setUpdateTime(new Date());
			// 是否更新了账号信息
			if (!oldAccount.getAccount().equals(db.getAccount())) {
				// 新旧账号不一致时，直接查询新账号是否存在数据库，存在则抛出异常
				List<Integer> hisList = accountService.findAccountIdList(new SearchFilter("account",
						SearchFilter.Operator.EQ, StringUtils.trimToEmpty(vo.getAccount())));
				if (!CollectionUtils.isEmpty(hisList)) {
					logger.error("创建账号失败，该账号已存在");
					throw new Exception("该账号已存在.");
				}
				db.setStatus(AccountStatus.Inactivated.getStatus());
				resultMessage = "当前状态为未激活，请做一笔转账测试完成激活";
				// 刷新入款表冗余数据
				incomeRequestService.updateAccount(db.getId(), db.getAccount());
			} else {
				// 未修改账号时 如果修改了盘口 层级 类型 且原来原来是在用 状态变停用
				if (oldAccount.getHandicapId() == null || !oldAccount.getHandicapId().equals(db.getHandicapId())
						|| oldAccount.getCurrSysLevel() == null
						|| !oldAccount.getCurrSysLevel().equals(db.getCurrSysLevel()) || oldAccount.getType() == null
						|| !oldAccount.getType().equals(db.getType())) {
					if (oldAccount.getStatus().equals(AccountStatus.Normal.getStatus())) {
						db.setStatus(AccountStatus.StopTemp.getStatus());
					}
				}
			}
			// 是否更新了盘口信息 或者类型
			if (oldAccount.getHandicapId() == null || !oldAccount.getHandicapId().equals(db.getHandicapId())
					|| oldAccount.getType() == null || !oldAccount.getType().equals(db.getType())) {
				// 新盘口全部层级赋给此账号（会先清空原有层级）
				List<Object[]> levels = levelService.findByHandicapIdsArray(db.getHandicapId());
				if (null != levels && levels.size() > 0) {
					List<Integer> levelIdList = new ArrayList<Integer>();
					for (Object[] level : levels) {
						if (null != level && level.length > 0 && null != level[0]) {
							levelIdList.add(Integer.parseInt(level[0].toString()));
						}
					}
					if (levelIdList.size() > 0) {
						accountSyncService.saveAccountLevelAndFlush(levelIdList, db.getId(), db.getType());
					}
				} else {
					logger.info(db.getHandicapId() + "盘口下的层级为空：" + levels.toString());
				}
				// 任何卡类型 转换后 清空持卡人字段并同步更新redis（入款卡不能有持卡人，仅修改子类型不影响）
				if (!oldAccount.getType().equals(db.getType())) {
					db.setHolder(null);
				}
			}
			// 如果新类型不是入款卡，清空sub_type和out_enable
			if (!db.getType().equals(AccountType.InBank.getTypeId())) {
				db.setSubType(null);
				db.setOutEnable(null);
			}
			if (Objects.equals(db.getType(), AccountType.InBank.getTypeId())
					&& !Objects.equals(db.getSubType(), oldAccount.getSubType())) {
				boolean res = accountService.modifyInBankStatus(oldAccount, oldAccount.getStatus(), null, true);
				if (res) {
					db.setPassageId(null);
				}
			}
			accountExtraService.saveAccountExtraLog(oldAccount, db, operator.getUid());
			logger.debug("updateSpecialAccount>> id {}", db.getId());
			accountService.broadCast(db);
			hostMonitorService.update(db);
			cabanaService.refreshAcc(db.getId());
			if (updAllocate)
				incomeAccountAllocateService.update(db.getId(), db.getType(), db.getStatus());
			GeneralResponseData<BizAccount> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue(), resultMessage);
			responseData.setData(db);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error(String.format("%s，参数：%s，结果：%s", "账号更新", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getMessage()));
		}
	}

	/**
	 * 批量更新出入款额度
	 */
	@RequiresPermissions(value = "IncomeAccountComp:ButtonOutSet")
	@RequestMapping("/batchUpdateLimit")
	public String batchUpdateLimit(@RequestParam(value = "param") String param) throws JsonProcessingException {
		// String params = buildParams().toString();
		try {
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			logger.info(String.format("%s，操作人员：%s，参数：%s", "批量更新出入款额度", operator.getUid(), param));
			JSONArray data = new JSONArray(param);
			Map<String, List<Integer>> result = accountService.batchUpdateLimit(data);
			// accountService.broadCast();// 批量刷新缓存数据
			GeneralResponseData<Map<String, List<Integer>>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "");
			responseData.setData(result);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error(String.format("%s，参数：%s，结果：%s", "批量更新出入款额度", param, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getMessage()));
		}
	}

	@RequestMapping("/checkTypeChangeAlarm")
	public String checkTypeChangeAlarm(@RequestParam(value = "param", required = false) String param,
			@RequestParam(value = "id", required = false) Integer id,
			@RequestParam(value = "handicap", required = false) String handicap,
			@RequestParam(value = "account", required = false) String account,
			@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "subType", required = false) String subType) throws JsonProcessingException {
		logger.debug("checkTypeChangeAlarm>>校验账号是否可以修改类型，参数 {}", param);
		GeneralResponseData<List<String>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		try {
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser == null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆");
				return mapper.writeValueAsString(responseData);
			}
			if (StringUtils.isBlank(param) && (Objects.isNull(id) || StringUtils.isBlank(handicap)
					|| StringUtils.isBlank(account) || StringUtils.isBlank(type) || StringUtils.isBlank(subType))) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"提供的参数不正确");
				return mapper.writeValueAsString(responseData);
			}
			List<String> result = null;
			/**
			 * if (StringUtils.isNotBlank(param)) { JSONArray data = new JSONArray(param);
			 * result = accountService.checkTypeChangeAlarm(data); } else { result =
			 * accountService.checkTypeChangeAlarm(id, handicap, account, type, subType); }
			 **/
			responseData.setData(result);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.info("账号切换告警信息失败 {}", e);
			if (StringUtils.isNotBlank(e.getMessage())) {
				responseData.setMessage(e.getMessage());
			} else {
				responseData.setMessage("账号切换告警信息失败");
			}
			responseData.setStatus(GeneralResponseData.ResponseStatus.FAIL.getValue());
			return mapper.writeValueAsString(responseData);
		}
	}

	/**
	 * 已删除账号恢复
	 *
	 * @param accountId
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequiresPermissions(value = "UpdateSpecialAccount:RestoreDelAccount")
	@RequestMapping("/restoreDelAccount")
	public String restoreDelAccount(@RequestParam(value = "accountId") Integer accountId)
			throws JsonProcessingException {
		String params = buildParams().toString();
		try {
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			logger.info(String.format("%s，操作人员：%s，参数：%s", "已删除账号恢复", operator.getUid(), params));
			BizAccount db = accountService.getById(accountId);
			BizAccount oldAccount = accountService.getById(accountId);
			BeanUtils.copyProperties(oldAccount, db);
			if (db == null) {
				logger.error("已删除账号恢复 所恢复的数据不存在");
				throw new Exception("所恢复的数据不存在.");
			}
			if (db.getStatus() == AccountStatus.Delete.getStatus()) {
				db.setStatus(AccountStatus.StopTemp.getStatus());
			} else {
				logger.error("已删除账号恢复，账号" + db.getAccount() + "并非删除状态：" + db.getStatus());
				throw new Exception("已删除账号恢复，账号");
			}
			db.setModifier(operator.getId());
			db.setUpdateTime(new Date());
			accountExtraService.saveAccountExtraLog(oldAccount, db, operator.getUid());
			logger.debug("restoreDelAccount>> id {}", db.getId());
			accountService.broadCast(db);
			hostMonitorService.update(db);
			cabanaService.updAcc(db.getId());
			GeneralResponseData<BizAccount> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			responseData.setData(db);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error(String.format("%s，参数：%s，结果：%s", "账号更新", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getMessage()));
		}
	}

	/**
	 * 获取出款人员的出款账号 账号类型 {@link AccountType} 没有出款第三方,如果是财务人员则查入款第三方账号
	 */
	@RequestMapping("/findAllocateAccount4OutwardAsign")
	public String findAllocateAccount4OutwardAsign(@RequestParam(value = "pageNo", required = false) Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "accountNo", required = false) String accountNo) throws JsonProcessingException {
		String params = buildParams().toString();
		logger.trace(String.format("%s，参数：%s", "获取出款人员的出款账号", params));
		try {
			GeneralResponseData<Object> responseData;
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (operator == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆!"));
			}
			SysUser sysUser = userService.findFromCacheById(operator.getId());
			Integer type = AccountType.OutBank.getTypeId();
			if (sysUser != null && sysUser.getCategory() >= 300) {
				type = AccountType.InThird.getTypeId();
			}
			if (StringUtils.isNotBlank(accountNo)) {
				accountNo = "%" + accountNo + "%";
			}
			PageRequest pageRequest = new PageRequest(pageNo, pageSize == null ? AppConstants.PAGE_SIZE : pageSize);
			Page<BizAccount> page = accountService.find4OutwardAsign(operator.getId(), type, accountNo, pageRequest);
			if (page == null || page.getContent() == null || page.getContent().size() == 0) {
				String msg;
				if (operator.getCategory() >= 300) {
					msg = "没有在用的第三方入款卡";
				} else {
					msg = "请先分配可用的出款卡,再接单!";
				}
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), msg);
				return mapper.writeValueAsString(responseData);
			} else {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue());
				Paging page2 = CommonUtils.getPage(pageNo + 1, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
						String.valueOf(page.getTotalElements()));
				responseData.setData(page.getContent());
				responseData.setPage(page2);
				return mapper.writeValueAsString(responseData);
			}
		} catch (Exception e) {
			logger.info(String.format("%s，参数：%s，结果：%s", "获取出款人员的出款账号", params, e));
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败" + e.getLocalizedMessage()));
		}
	}

	/**
	 * 获取出款人员的出款账号 账号类型 {@link AccountType} 没有出款第三方,如果是财务人员则查入款第三方账号 总记录数
	 */
	@RequestMapping("/findAllocateAccount4OutwardAsignCount")
	public String findAllocateAccount4OutwardAsignCount(
			@RequestParam(value = "pageNo", required = false) Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "accountNo", required = false) String accountNo) throws JsonProcessingException {
		String params = buildParams().toString();
		logger.trace(String.format("%s，参数：%s", "获取出款人员的出款账号", params));
		try {
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (operator == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆!"));
			}
			Integer type = AccountType.OutBank.getTypeId();
			if (operator.getCategory() >= 300) {
				type = AccountType.InThird.getTypeId();
			}
			if (StringUtils.isNotBlank(accountNo)) {
				accountNo = "%" + accountNo + "%";
			}
			Long count = accountService.find4OutwardAsignCount(operator.getId(), type, accountNo);
			Paging page2;
			if (count != null) {
				page2 = CommonUtils.getPage(pageNo + 1, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
						String.valueOf(count));
			} else {
				page2 = CommonUtils.getPage(0, pageSize != null ? pageSize : AppConstants.PAGE_SIZE, "0");
			}
			GeneralResponseData<Object> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			responseData.setPage(page2);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.info(String.format("%s，参数：%s，结果：%s", "获取出款人员的出款账号", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败" + e.getLocalizedMessage()));
		}
	}

	/**
	 * 根据层级id查找账号 入款审核使用
	 */
	@RequiresPermissions({ "IncomeAuditComp:Retrieve" })
	@RequestMapping("/getAccounts")
	public String getAccounts(@RequestParam(value = "levelIds") String levelIds) throws JsonProcessingException {
		String params = buildParams().toString();
		logger.trace(String.format("%s，参数：%s", "根据层级id查找账号 入款审核使用", params));
		GeneralResponseData<List<BizAccount>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		String[] levelIdsArr = levelIds.split(",");
		List<Integer> levelIdsList = new ArrayList<>();
		for (String id : levelIdsArr) {
			levelIdsList.add(Integer.valueOf(id));
		}
		if (!CollectionUtils.isEmpty(levelIdsList)) {
			try {
				logger.info("获取入款审核账号");
				List<BizAccount> bizAccountList = accountService.getIncomeAccountList(null, levelIdsList,
						AccountType.InBank.getTypeId());
				bizAccountList = bizAccountList.stream()
						.filter((p) -> p.getType().equals(AccountType.InBank.getTypeId())).collect(Collectors.toList());
				responseData.setData(distinct(bizAccountList));
			} catch (Exception e) {
				logger.error("获取入款审核账号选取失败");
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "初始化失败");
			}
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 根据锁定的账号 获取 选定的第三方账号id
	 *
	 * @param
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/getSelectedThirdIdByLockedId")
	public String getSelectedThirdIdByLockedId(@RequestParam(value = "accountId") Integer accountId)
			throws JsonProcessingException {
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请登录!"));
		}
		Integer val = accountService.getSelectedThirdIdByLockedId(accountId);
		GeneralResponseData<Integer> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "获取成功!");
		responseData.setData(val);
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 选定某个第三方账号 作为提现账号 保存操作记录
	 *
	 * @param thirdId
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/saveSelectThirdRecord")
	public String saveSelectThirdRecord(@RequestParam(value = "thirdId") Integer thirdId,
			@RequestParam(value = "accountIds") List<Integer> accountIds) throws JsonProcessingException {
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请登录!"));
		}
		accountService.saveSelectThirdRecord(thirdId, accountIds);
		// 保存使用的最新的第三方账号
		incomeRequestService.saveThirdIdUsedLatest(thirdId, sysUser.getId());
		return mapper.writeValueAsString(
				new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "保存操作成功!"));
	}

	/***
	 * 需求 7594
	 *
	 * @param thirdId
	 * @return
	 * @throws JsonProcessingException
	 */
	@GetMapping("/releaseOtherSetup")
	@RequiresPermissions(value = { "ThirdDrawTask:ReleaseOtherSetup:*" })
	public String releaseOtherSetup(@RequestParam(value = "id") Integer thirdId) throws JsonProcessingException {
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请登录!"));
		}
		logger.info("用户：{} 解除账号id:{}", sysUser.getUid(), thirdId);
		accountService.releaseOtherSetup(thirdId);
		return mapper.writeValueAsString(
				new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "解除成功!"));
	}

	/**
	 * 选择第三方 添加我的设定 解除我的设定
	 *
	 * @param thirdAccounts
	 * @param type
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/setThirdAccount")
	public String setThirdAccount(@RequestParam(value = "thirdAccount") List<Integer> thirdAccounts,
			@RequestParam(value = "type") Byte type) throws JsonProcessingException {
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请登录!"));
		}
		// 需求 7459
		int size = thirdAccounts.size();
		if (size == 0)
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功!"));
		String mes = "操作成功!";
		if (type == 1) {
			thirdAccounts = accountService.filterAlreadySetted(thirdAccounts);
			int size2 = thirdAccounts.size();
			if (CollectionUtils.isEmpty(thirdAccounts)) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "账号已被设定!"));
			}
			mes = size == size2 ? "操作成功!" : "操作成功,部分账号已被其他人设定!";
			//需求 7994 判断第三方账号是否设置了有效的下发手续费规则
			thirdAccounts = accountFeeService.filterNoEffectFeeConfig(thirdAccounts);
			if(thirdAccounts.size()==0) {
				log.debug("设置锁定第三方账号时，过滤未设置有效下发规则的账号id后，最后所得id数量为0");
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "选择的账号未设置有效的下发规则!"));
			}
			if(thirdAccounts.size()!=size2) {
				mes = "操作成功,部分账号为设置下发手续费规则!";
			}
		}
		accountService.setThirdAccount(thirdAccounts, sysUser.getId(), type.intValue() == 1 ? true : false);
		return mapper.writeValueAsString(
				new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), mes));
	}

	/**
	 * 下发任务 锁定 解锁
	 *
	 * @param accountIds
	 * @param type
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/lockOrUnlockInDrawTaskPage")
	public String lockOrUnlockInDrawTaskPage(@RequestParam(value = "accountId") List<Integer> accountIds,
			@RequestParam(value = "type") Byte type) throws JsonProcessingException {
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请登录!"));
		}
		if (CollectionUtils.isEmpty(accountIds)) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请选择数据"));
		}
		if (type.intValue() == 1) {
			Iterator it = accountIds.iterator();
			for (; it.hasNext();) {
				Integer accountId = Integer.valueOf(it.next().toString());
				AccountBaseInfo baseInfo = accountService.getFromCacheById(accountId);
				if (null == baseInfo) {
					logger.debug("账号id:{} 信息为空");
					return mapper.writeValueAsString(new GeneralResponseData<>(
							GeneralResponseData.ResponseStatus.FAIL.getValue(), "账号id:" + accountId + " 信息为空!"));
				}
				if (accountService.judgeLocked(accountId)) {
					logger.info("账号id:{} 已被锁定");
					return mapper.writeValueAsString(new GeneralResponseData<>(
							GeneralResponseData.ResponseStatus.FAIL.getValue(), "账号id:" + accountId + " 已被锁定!"));
				}
			}
		}
		boolean res = accountService.lockedOrUnlockByDrawTask(sysUser.getId(), accountIds, type.intValue() == 1);
		if (!res) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败!"));
		} else {
			// for (Integer accId : accountIds) {
			// accountService.saveDrawTime(accId, type.intValue() == 1);
			// }
			refreshCache(sysUser.getId());
			accountClickService.addClickLogList(accountIds, type.intValue() == 1 ? "锁定" : "解锁");
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功!"));
		}
	}

	/**
	 * 下发失败统计 明细
	 *
	 * @param inputDTO
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping(value = "/findThirdDrawFailDetail", method = RequestMethod.POST)
	public String findThirdDrawFailDetail(@RequestBody ThirdDrawFailStatisticInputDTO inputDTO)
			throws JsonProcessingException {
		boolean test = inputDTO.getTestFlag() != null && inputDTO.getTestFlag().intValue() == 1;
		SysUser sysUser = test ? userService.findFromCacheById(5) : (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (!test && null != inputDTO.getQueryFlag() && inputDTO.getQueryFlag().intValue() == 2) {
			if (sysUser == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请登录!"));
			}
			inputDTO.setOperator(sysUser.getId());
		}
		String[] handicaps = null;
		// 只能查询自己区域的账号
		if (!sysUser.getCategory().equals(-1)) {
			List<SysUserProfile> manilaUserPro = sysUserProfileService.findByPropertyKey("HANDICAP_MANILA_ZONE");
			String manilaHandicap = manilaUserPro.get(0).getPropertyValue();
			List<SysUserProfile> taiWanuserPro = sysUserProfileService.findByPropertyKey("HANDICAP_TAIWAN_ZONE");
			String taiwanHandicap = taiWanuserPro.get(0).getPropertyValue();
			BizHandicap bizHandicap = handicapService.findFromCacheById(sysUser.getHandicap());
			List<Integer> handicapIds = handicapService
					.findByZone(bizHandicap == null ? "0" : bizHandicap.getId().toString());
			String handicapId = CollectionUtils.isEmpty(handicapIds) ? "0" : handicapIds.get(0).toString();
			if (ArrayUtils.contains(manilaHandicap.substring(1).split(";"),
					(sysUser.getCategory() > 400 ? (sysUser.getCategory() - 400) + "" : handicapId))) {
				handicaps = manilaHandicap.substring(1).split(";");

			} else if (ArrayUtils.contains(taiwanHandicap.substring(1).split(";"),
					(sysUser.getCategory() > 400 ? (sysUser.getCategory() - 400) + "" : handicapId))) {
				handicaps = taiwanHandicap.substring(1).split(";");

			}
			if (ObjectUtils.isEmpty(handicaps)) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "用户所属区域没有盘口权限!"));
			}
		}
		if (StringUtils.isNotBlank(inputDTO.getThirdAccount()) || StringUtils.isNotBlank(inputDTO.getDrawToAccount())
				|| StringUtils.isNotBlank(inputDTO.getThirdName())
				|| StringUtils.isNotBlank(StringUtils.trim(inputDTO.getStartTime()))
				|| StringUtils.isNotBlank(StringUtils.trim(inputDTO.getEndTime()))) {
			inputDTO.setPageNo(0);
		}
		if (inputDTO.getPageSize() == null) {
			inputDTO.setPageSize(AppConstants.PAGE_SIZE);
		}
		inputDTO.setHandicaps(handicaps);
		if (StringUtils.isNotBlank(inputDTO.getOperatorUid())) {
			SysUser sysUser1 = userService.findByUid(inputDTO.getOperatorUid());
			if (sysUser1 == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "无数据!"));
			}
			inputDTO.setOperator(sysUser1.getId());
		}
		List<ThirdDrawFailStatisticOutputDTO> list = incomeRequestService.findThirdDrawFailDetail(inputDTO);
		int counts = list.size();
		GeneralResponseData<List<ThirdDrawFailStatisticOutputDTO>> res = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功!");
		Paging page;
		int pageNo = inputDTO.getPageNo();
		int pageSize = inputDTO.getPageSize();

		if (counts != 0) {
			page = CommonUtils.getPage(pageNo + 1, pageSize, String.valueOf(counts));
			BigDecimal sumAmount = list.stream().map(p -> p.getAmount()).reduce(BigDecimal::add).get();
			page.setHeader(new HashMap(1) {
				{
					put("sumAmount", sumAmount);
				}
			});
		} else {
			page = CommonUtils.getPage(0, pageSize, "0");
			page.setHeader(new HashMap(1) {
				{
					put("sumAmount", 0);
				}
			});
		}
		res.setPage(page);

		int start = pageNo * pageSize;
		int end = counts - start > page.getPageSize() ? start + page.getPageSize() : counts;
		List<ThirdDrawFailStatisticOutputDTO> dataRes = list.subList(start, end);
		res.setData(dataRes);
		return mapper.writeValueAsString(res);
	}

	/**
	 * 下发失败统计
	 *
	 * @param inputDTO
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping(value = "/findThirdDrawFail", method = RequestMethod.POST)
	public String findThirdDrawFail(@RequestBody ThirdDrawFailStatisticInputDTO inputDTO)
			throws JsonProcessingException {
		boolean test = inputDTO.getTestFlag() != null && inputDTO.getTestFlag().intValue() == 1;
		SysUser sysUser = test ? userService.findFromCacheById(5) : (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (!test && null != inputDTO.getQueryFlag() && inputDTO.getQueryFlag().intValue() == 2) {
			if (sysUser == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请登录!"));
			}
			inputDTO.setOperator(sysUser.getId());
		}
		String[] handicaps = null;
		// 只能查询自己区域的账号
		if (!sysUser.getCategory().equals(-1)) {
			List<SysUserProfile> manilaUserPro = sysUserProfileService.findByPropertyKey("HANDICAP_MANILA_ZONE");
			String manilaHandicap = manilaUserPro.get(0).getPropertyValue();
			List<SysUserProfile> taiWanuserPro = sysUserProfileService.findByPropertyKey("HANDICAP_TAIWAN_ZONE");
			String taiwanHandicap = taiWanuserPro.get(0).getPropertyValue();
			BizHandicap bizHandicap = handicapService.findFromCacheById(sysUser.getHandicap());
			List<Integer> handicapIds = handicapService
					.findByZone(bizHandicap == null ? "0" : bizHandicap.getId().toString());
			String handicapId = CollectionUtils.isEmpty(handicapIds) ? "0" : handicapIds.get(0).toString();
			if (ArrayUtils.contains(manilaHandicap.substring(1).split(";"),
					(sysUser.getCategory() > 400 ? (sysUser.getCategory() - 400) + "" : handicapId))) {
				handicaps = manilaHandicap.substring(1).split(";");

			} else if (ArrayUtils.contains(taiwanHandicap.substring(1).split(";"),
					(sysUser.getCategory() > 400 ? (sysUser.getCategory() - 400) + "" : handicapId))) {
				handicaps = taiwanHandicap.substring(1).split(";");

			}
			if (ObjectUtils.isEmpty(handicaps)) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "用户所属区域没有盘口权限!"));
			}
		}
		if (StringUtils.isNotBlank(inputDTO.getThirdAccount()) || StringUtils.isNotBlank(inputDTO.getDrawToAccount())
				|| StringUtils.isNotBlank(inputDTO.getThirdName())
				|| StringUtils.isNotBlank(StringUtils.trim(inputDTO.getStartTime()))
				|| StringUtils.isNotBlank(StringUtils.trim(inputDTO.getEndTime()))) {
			inputDTO.setPageNo(0);
		}
		inputDTO.setHandicaps(handicaps);
		List<ThirdDrawFailStatisticOutputDTO> list = incomeRequestService.findThirdDrawFail(inputDTO);
		int counts = list.size();
		GeneralResponseData<List<ThirdDrawFailStatisticOutputDTO>> res = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功!");
		BinaryOperator<Integer> add = (x, y) -> x + y;
		int countAllFail = counts == 0 ? 0 : list.stream().map(p -> p.getFailCounts()).reduce(add).get();
		Paging page;
		int pageNo = inputDTO.getPageNo();
		int pageSize = inputDTO.getPageSize();

		if (counts != 0) {
			page = CommonUtils.getPage(pageNo + 1, pageSize, String.valueOf(counts));
		} else {
			page = CommonUtils.getPage(0, pageSize, "0");
		}
		page.setHeader(new HashMap(1) {
			{
				put("countAllFail", countAllFail);
			}
		});
		res.setPage(page);
		int start = pageNo * pageSize;
		int end = counts - start > page.getPageSize() ? start + page.getPageSize() : counts;
		List<ThirdDrawFailStatisticOutputDTO> dataRes = list.subList(start, end);
		res.setData(dataRes);
		return mapper.writeValueAsString(res);
	}

	private LoadingCache<String, Map<String, Object>> cacheCounts = CacheBuilder.newBuilder()
			.expireAfterAccess(1, TimeUnit.HOURS).refreshAfterWrite(10, TimeUnit.SECONDS)
			.build(new CacheLoader<String, Map<String, Object>>() {
				@Override
				public Map<String, Object> load(String key) throws Exception {
					Map<String, Object> map = new HashMap(4) {
						{
							put("queryStatus1InAllCount", 0);
							put("queryStatus2InAllCount", 0);
							put("queryStatus1ByOneCount", 0);
							put("queryStatus2ByOneCount", 0);
						}
					};
					if (StringUtils.isNotBlank(key)) {
						Integer userId = Integer.valueOf(key);
						Integer queryStatus1InAllCount = accountService.findNewDrawTask().size();
						Integer queryStatus2InAllCount = accountService.findLockedOrUnfinishedDrawTask().size();

						Integer queryStatus1ByOneCount = accountService.findLockedByOneDrawTask(userId).size();
						Integer queryStatus2ByOneCount = accountService.findUnfinishedByOneDrawTask(userId).size();
						map.put("queryStatus1InAllCount", queryStatus1InAllCount);
						map.put("queryStatus2InAllCount", queryStatus2InAllCount);
						map.put("queryStatus1ByOneCount", queryStatus1ByOneCount);
						map.put("queryStatus2ByOneCount", queryStatus2ByOneCount);

					}
					return map;
				}
			});

	/**
	 * 查询 完成下发 下发失败
	 *
	 * @param inputDTO
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping(value = "/findDrawRecord", method = RequestMethod.POST)
	public String findDrawRecord(@RequestBody FindDrawTaskInputDTO inputDTO) throws JsonProcessingException {

		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请登录!"));
		}
		if (inputDTO.getDrawRecordStatus() == null) {
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "DrawRecordStatus参数必传!"));
		}
		String[] handicaps = null;
		if (Objects.nonNull(inputDTO.getHandicap()))
			handicaps = new String[] { String.valueOf(inputDTO.getHandicap()) };
		if (Objects.isNull(handicaps)) {
			List<String> tmp = handicapService.handicapIdList(sysUser).stream().filter(Objects::nonNull)
					.map(String::valueOf).collect(Collectors.toList());
			if (CollectionUtils.isEmpty(tmp))
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "用户无盘口权限!"));
			handicaps = tmp.toArray(new String[tmp.size()]);
		}
		int start = inputDTO.getPageNo()
				* (inputDTO.getPageSize() != null ? inputDTO.getPageSize() : AppConstants.PAGE_SIZE);
		// 查询条件
		if (inputDTO.getStatus() != null || inputDTO.getLocked() != null
				|| StringUtils.isNotBlank(inputDTO.getAccount()) || StringUtils.isNotBlank(inputDTO.getOwner())
				|| StringUtils.isNotBlank(inputDTO.getBankName()) || StringUtils.isNotBlank(inputDTO.getBankType())
				|| inputDTO.getLocked() != null) {
			inputDTO.setPageNo(0);
		}
		if (inputDTO.getPageSize() == null) {
			inputDTO.setPageSize(AppConstants.PAGE_SIZE);
		}
		GeneralResponseData<List<FindDrawTaskOutputDTO>> res = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功!");
		inputDTO.setSysUser(sysUser);
		logger.debug("查询下发完成 下发失败记录 开始:{}", System.currentTimeMillis());
		List<FindDrawTaskOutputDTO> data = accountService.findDrawTaskRecord(inputDTO, handicaps);
		logger.debug("查询下发完成 下发失败记录 结束:{}", System.currentTimeMillis());
		int counts = data.size();
		Paging page;

		if (CollectionUtils.isEmpty(data)) {
			page = CommonUtils.getPage(0, inputDTO.getPageSize(), "0");
			Map<String, Object> map = new HashMap<>(3);
			map.put("sumAmount", 0);
			map.put("sumFee", 0);
            if ( inputDTO.getDrawRecordStatus() == 1 ){
                map.put("inType0", 0);
                map.put("inType1", 0);
                map.put("inType2", 0);
                map.put("inType3", 0);
            }
			res.setPage(page);
			res.setData(Lists.newArrayList());
			res.getPage().setHeader(map);
			try {
				res.getPage().getHeader().putAll(cacheCounts.get(sysUser.getId().toString()));
			} catch (ExecutionException e) {
				logger.error("获取缓存 异常 ：", e);
			}
			return mapper.writeValueAsString(res);
		} else {
			page = CommonUtils.getPage(inputDTO.getPageNo() + 1, inputDTO.getPageSize(), String.valueOf(counts));
			Map<String, Object> map = new HashMap<>(3);
			ThreadLocal<BigDecimal> sumAmount = new ThreadLocal<BigDecimal>() {
				{
					set(new BigDecimal(0));
				}
			}, sumFee = new ThreadLocal<BigDecimal>() {
				{
					set(new BigDecimal(0));
				}
			};
			data.stream().forEach(p -> {

				if (null != p.getAmount()) {
					sumAmount.set(sumAmount.get().add(p.getAmount()));
				}
				if (null != p.getFee()) {
					sumFee.set(sumFee.get().add(p.getFee()));
				}
			});

			map.put("sumAmount", sumAmount.get());
			map.put("sumFee", sumFee.get());
			if ( inputDTO.getDrawRecordStatus() == 11 ){
                Double inType0 = data.stream()
                        .filter(w -> w.getPayee() ==0)
                        .mapToDouble(w -> w.getAmount().doubleValue())
                        .sum();
                Double inType1 = data.stream()
                        .filter(w -> w.getPayee() ==1)
                        .mapToDouble(w -> w.getAmount().doubleValue())
                        .sum();
                Double inType2 = data.stream()
                        .filter(w -> w.getPayee() ==2)
                        .mapToDouble(w -> w.getAmount().doubleValue())
                        .sum();
                Double inType3 = data.stream()
                        .filter(w -> w.getPayee() ==3)
                        .mapToDouble(w -> w.getAmount().doubleValue())
                        .sum();
                map.put("inType0", inType0);
                map.put("inType1", inType1);
                map.put("inType2", inType2);
                map.put("inType3", inType3);

				Double balanceTotal = data.stream()
						.filter(w -> w.getBalance() != null)
						.mapToDouble(w -> w.getBalance().doubleValue())
						.sum();
				Double bankBalanceTotal = data.stream()
						.filter(w -> w.getBankBalance()!=null)
						.mapToDouble(w -> w.getBankBalance().doubleValue())
						.sum();
                map.put("bankBalanceTotal", bankBalanceTotal);
                map.put("balanceTotal", balanceTotal);
            }

			sumAmount.remove();
			sumFee.remove();

			int end = counts > inputDTO.getPageSize()
					? (counts - start > inputDTO.getPageSize() ? start + inputDTO.getPageSize() : counts)
					: counts;
			List<FindDrawTaskOutputDTO> dataRes = CollectionUtils.isEmpty(data) ? Lists.newLinkedList()
					: data.subList(start, end);

			res.setData(dataRes);
			res.setPage(page);
			res.getPage().setHeader(map);
			try {
				res.getPage().getHeader().putAll(cacheCounts.get(sysUser.getId().toString()));
			} catch (ExecutionException e) {
				logger.error("获取缓存 异常 ：", e);
			}
			return mapper.writeValueAsString(res);
		}
	}

	private LoadingCache<String, List<Integer>> newTaskCache = CacheBuilder.newBuilder()
			.refreshAfterWrite(5, TimeUnit.SECONDS).build(new CacheLoader<String, List<Integer>>() {
				@Override
				public List<Integer> load(String key) {
					List<Integer> res = accountService.findNewDrawTask();
					return res;
				}
			});
	private LoadingCache<String, List<Integer>> lockedAndUnfinishCache = CacheBuilder.newBuilder()
			.refreshAfterWrite(6, TimeUnit.SECONDS).build(new CacheLoader<String, List<Integer>>() {
				@Override
				public List<Integer> load(String key) throws Exception {
					List<Integer> res = accountService.findLockedOrUnfinishedDrawTask();
					logger.debug("缓存自动更新 下发中的 结果:{}", res.toString());
					return res;
				}
			});

	private LoadingCache<String, List<Integer>> lockedByOneCache = CacheBuilder.newBuilder()
			.refreshAfterWrite(7, TimeUnit.SECONDS).build(new CacheLoader<String, List<Integer>>() {
				@Override
				public List<Integer> load(String key) throws Exception {
					List<Integer> res = accountService.findLockedByOneDrawTask(Integer.valueOf(key));
					logger.debug("缓存自动更新 用户id{} 锁定的 结果:{}", key, res.toString());
					return res;
				}
			});
	private LoadingCache<String, List<Integer>> unfinishedByOneCache = CacheBuilder.newBuilder()
			.refreshAfterWrite(8, TimeUnit.SECONDS).build(new CacheLoader<String, List<Integer>>() {
				@Override
				public List<Integer> load(String key) throws Exception {
					List<Integer> res = accountService.findUnfinishedByOneDrawTask(Integer.valueOf(key));
					logger.debug("缓存自动更新 用户id{} 锁定的 结果:{}", key, res.toString());
					return res;
				}
			});

	/**
	 * 刷新缓存
	 *
	 * @param userId
	 */
	public void refreshCache(Integer userId) {
		newTaskCache.invalidate("ALL");
		lockedAndUnfinishCache.invalidate("ALL");
		lockedByOneCache.invalidate(userId.toString());
		unfinishedByOneCache.invalidate(userId.toString());

		newTaskCache.refresh("ALL");
		lockedAndUnfinishCache.refresh("ALL");
		lockedByOneCache.refresh(userId.toString());
		unfinishedByOneCache.refresh(userId.toString());
	}

	/**
	 * 需求 7595 <br>
	 * 根据输入的金额获取手续费
	 *
	 * @param id
	 * @param amount
	 * @return
	 * @throws JsonProcessingException
	 */
	@GetMapping("/dynamicFee")
	public String dynamicFee(@RequestParam(value = "id") Integer id, @RequestParam(value = "amount") String amount)
			throws JsonProcessingException {
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请登录!"));
		}
		logger.debug("动态获取手续费参数:id:{} 金额:{}", id, amount);
		String fee = "0";
		Integer thirdId = accountService.getSelectedThirdIdByLockedId(id);
		if (thirdId == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "没有选定第三方账号!"));
		}
		logger.debug("根据账号id:{} 获取选定的三方账号id:{}", id, thirdId);
		AccountBaseInfo baseInfoThird = accountService.getFromCacheById(thirdId);
		if (null == baseInfoThird) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "设定三方账号为空!"));
		}
		logger.debug("动态获取手续费结 输入的金额:{},第三方账号信息:{}", amount, baseInfoThird.toString());
		BizAccount account = accountService.getById(baseInfoThird.getId());
		if (StringUtils.isNotBlank(amount) && null != account) {
			try {
				// 输入的金额
				BigDecimal amount2 = new BigDecimal(amount);
				// 需求 7595
				AccountFeeCalResult result = accountFeeService.calAccountFee(account, amount2);
				logger.debug("calAccountFee动态获取手续费结果:{}", result);
				if (null != result) {
					fee = result.getFee().toString();
				}
			} catch (NoSuiteAccountFeeRuleException e) {
				logger.error("获取手续费异常NoSuiteAccountFeeRuleException:", e);
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getMessage()));
			} catch (Exception e) {
				logger.error("获取手续费异常:", e);
			}
		}
		GeneralResponseData<String> res = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "计算手续费成功!");
		res.setData(fee);
		return mapper.writeValueAsString(res);
	}

	/**
	 * 获取下发银行卡列表 1 入款微信账号，入款支付宝账号，所绑定的 2 入款微信账号，入款支付宝账号 可绑定，但是未绑定的
	 * incomeAccountId：此id下绑定的银行卡 statusOfIssueToArray：状态 正常 暂停 异常
	 * typeOfIssueToArray：类型 支付宝专用 微信专用 第三方专用 公用绑定 binding0binded1：查询已绑定的输入1
	 * 查询可绑定但是未绑定的 输入0
	 */
	@RequestMapping("/findbindissue")
	public String findBindIssue(FindBindIssueInputDTO inputDTO, @Valid BizAccount bizAccount)
			throws JsonProcessingException {
		String params = buildParams().toString();
		logger.trace(String.format("%s，参数：%s", "获取下发银行卡列表", params));
		try {
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请登录!"));
			}
			GeneralResponseData<List<BizAccount>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			List<SearchFilter> filterToList = new ArrayList<>();
			List<SearchFilter> filterListOr = new ArrayList<>();
			int pageNo = inputDTO.getPageNo() == null ? 0 : inputDTO.getPageNo();
			int pageSize = inputDTO.getPageSize() != null ? inputDTO.getPageSize() : AppConstants.PAGE_SIZE;
			PageRequest pageRequest = PageRequest.of(pageNo, pageSize, Sort.Direction.ASC, "bankBalance");
			// 此账号绑定的全部银行卡集合
			List<Integer> issueToList = accountBindingService.findBindAccountId(inputDTO.getIncomeAccountId());
			// 第三方入款 提现 弹出框的 全部:出款卡 其他卡
			boolean queryAll = inputDTO.getQueryType() == null;
			boolean queryOutcard = inputDTO.getQueryType() != null && inputDTO.getQueryType().intValue() == 1;
			boolean queryOthercard = inputDTO.getQueryType() != null && inputDTO.getQueryType().intValue() == 2;

			// 所有锁定的 当前人锁定的 下发卡
			List<String> locked = accountService.getTargetAccountLockedInRedis(null, null),
					lockedAccByUserId = accountService.getLockedAccByUserId(sysUser.getId());

			// 所有锁定的出款卡 当前人锁定的出款卡
			List<String> outCardLocked = accountService.allOutCardIdsLocked(),
					outCardLockedByUserId = accountService.outCardIdsLockedByUserId(sysUser.getId());
			logger.debug("所有锁定的出款卡:{}", outCardLocked.toString());
			logger.debug("当前人锁定的出款卡:{}", outCardLockedByUserId.toString());

			if (!CollectionUtils.isEmpty(outCardLocked)) {
				if (queryAll) {
					if (CollectionUtils.isEmpty(locked)) {
						locked = Lists.newArrayList();
					}
					locked.addAll(outCardLocked);
				}
				if (queryOutcard) {
					locked = Lists.newArrayList();
					locked.addAll(outCardLocked);
				}

			} else {
				if (queryOutcard && inputDTO.getBinding0binded1() == 2) {
					responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
							"无记录");
					responseData.setData(null);
					responseData.setPage(null);
					return mapper.writeValueAsString(responseData);
				}
			}
			if (!CollectionUtils.isEmpty(outCardLockedByUserId)) {
				if (queryAll) {
					if (CollectionUtils.isEmpty(lockedAccByUserId)) {
						lockedAccByUserId = Lists.newArrayList();
					}
					lockedAccByUserId.addAll(outCardLockedByUserId);
				}
				if (queryOutcard) {
					lockedAccByUserId = Lists.newArrayList();
					lockedAccByUserId.addAll(outCardLockedByUserId);
				}

			} else {
				if (queryOutcard && inputDTO.getBinding0binded1() == 2) {
					responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
							"无记录");
					responseData.setData(null);
					responseData.setPage(null);
					return mapper.writeValueAsString(responseData);
				}
			}

			// 所有需要下发钱但未锁定的也未绑定的出款卡
			// List<String> unBindedOutCardIds = null;
			// 所有需要下发钱但未锁定的卡
			List<Integer> allOutCardIds = accountService.needThirdDrawToOutCardIds();
			// 所有需要下发钱但未锁定的卡
			List<String> unLockedOutCardIds = CollectionUtils.isEmpty(allOutCardIds) ? Lists.newArrayList()
					: allOutCardIds.stream().map(Object::toString).collect(Collectors.toList());

			logger.debug("所有需要下发钱但未锁定的卡：{}", unLockedOutCardIds.toString());

			Set<String> excluded = accountService.getRecycleBindComm();
			logger.debug("不能下发不出现在未锁定和锁定的页面中的记录:{}", excluded.toString());
			List<Integer> lockedInDrawTaskPage = accountService.allLockedInDrawTask();
			logger.debug("在下发任务页签 被锁定的:{}", lockedInDrawTaskPage.toString());
			if (!CollectionUtils.isEmpty(lockedInDrawTaskPage)) {
				excluded.addAll(Lists.transform(lockedInDrawTaskPage, Functions.toStringFunction()));
			}
			if (inputDTO.getBinding0binded1() == 1) {
				// 未锁定的 包括 已绑定和未绑定的 下发卡 第三方专用卡 需要下发钱的出款卡 小于设定百分比的大额出款卡
				if (!CollectionUtils.isEmpty(excluded)) {
					if (CollectionUtils.isEmpty(locked)) {
						locked = Lists.newArrayList();
					}
					locked.addAll(excluded);
				}
				if (queryAll) {
					if (!CollectionUtils.isEmpty(locked)) {
						filterToList.add(new SearchFilter("id", SearchFilter.Operator.NOTIN, locked.toArray()));
					}
					inputDTO.setTypeOfIssueToArray(
							new Integer[] { AccountType.ThirdCommon.getTypeId(), AccountType.BindCommon.getTypeId() });
					if (!CollectionUtils.isEmpty(unLockedOutCardIds)) {
						filterListOr
								.add(new SearchFilter("id", SearchFilter.Operator.IN, unLockedOutCardIds.toArray()));
					}
				}
				if (queryOthercard) {
					if (!CollectionUtils.isEmpty(locked)) {
						filterToList.add(new SearchFilter("id", SearchFilter.Operator.NOTIN, locked.toArray()));
					}
					inputDTO.setTypeOfIssueToArray(
							new Integer[] { AccountType.ThirdCommon.getTypeId(), AccountType.BindCommon.getTypeId() });
				}
				if (queryOutcard) {
					if (!CollectionUtils.isEmpty(unLockedOutCardIds)) {
						filterToList
								.add(new SearchFilter("id", SearchFilter.Operator.IN, unLockedOutCardIds.toArray()));
						inputDTO.setTypeOfIssueToArray(new Integer[] { AccountType.OutBank.getTypeId() });
					} else {
						if (queryOutcard) {
							responseData = new GeneralResponseData<>(
									GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "无记录");
							responseData.setData(null);
							responseData.setPage(null);
							return mapper.writeValueAsString(responseData);
						}
					}
				}
			}

			if (inputDTO.getBinding0binded1() == 2) {
				// 已锁定页签
				if (CollectionUtils.isEmpty(lockedAccByUserId)) {
					return mapper.writeValueAsString(responseData);
				}
				filterToList.add(new SearchFilter("id", SearchFilter.Operator.IN, lockedAccByUserId.toArray()));
				if (!CollectionUtils.isEmpty(excluded)) {
					filterToList.add(new SearchFilter("id", SearchFilter.Operator.NOTIN, excluded.toArray()));
				}
				if (queryAll) {
					inputDTO.setTypeOfIssueToArray(new Integer[] { AccountType.ThirdCommon.getTypeId(),
							AccountType.BindCommon.getTypeId(), AccountType.OutBank.getTypeId() });
				} else if (queryOthercard) {
					inputDTO.setTypeOfIssueToArray(
							new Integer[] { AccountType.ThirdCommon.getTypeId(), AccountType.BindCommon.getTypeId() });
				} else if (queryOutcard) {
					inputDTO.setTypeOfIssueToArray(new Integer[] { AccountType.OutBank.getTypeId() });
				}

			}

			if (inputDTO.getStatusOfIssueToArray() != null && inputDTO.getStatusOfIssueToArray().length > 0) {
				filterToList
						.add(new SearchFilter("status", SearchFilter.Operator.IN, inputDTO.getStatusOfIssueToArray()));
				if (inputDTO.getBinding0binded1() == 1 && queryAll && !CollectionUtils.isEmpty(unLockedOutCardIds)) {
					filterListOr.add(
							new SearchFilter("status", SearchFilter.Operator.IN, inputDTO.getStatusOfIssueToArray()));

				}
			}
			if (inputDTO.getBankBrandList() != null && inputDTO.getBankBrandList().length > 0) {
				filterToList.add(new SearchFilter("bankType", SearchFilter.Operator.IN, inputDTO.getBankBrandList()));
				if (inputDTO.getBinding0binded1() == 1 && queryAll && !CollectionUtils.isEmpty(unLockedOutCardIds)) {
					filterListOr
							.add(new SearchFilter("bankType", SearchFilter.Operator.IN, inputDTO.getBankBrandList()));
				}
			}
			if (StringUtils.isNotBlank(inputDTO.getBankType())) {
				filterToList.add(new SearchFilter("bankType", SearchFilter.Operator.LIKE, inputDTO.getBankType()));
				if (inputDTO.getBinding0binded1() == 1 && queryAll && !CollectionUtils.isEmpty(unLockedOutCardIds)) {
					filterListOr.add(new SearchFilter("bankType", SearchFilter.Operator.LIKE, inputDTO.getBankType()));
				}
			}
			if (StringUtils.isNotBlank(inputDTO.getFlag())) {
				filterToList.add(new SearchFilter("flag", SearchFilter.Operator.EQ, inputDTO.getFlag()));
				if (inputDTO.getBinding0binded1() == 1 && queryAll && !CollectionUtils.isEmpty(unLockedOutCardIds)) {
					filterListOr.add(new SearchFilter("flag", SearchFilter.Operator.EQ, inputDTO.getFlag()));
				}
			}
			if (inputDTO.getCurrSysLevelToArray() != null && inputDTO.getCurrSysLevelToArray().length > 0) {
				filterToList.add(
						new SearchFilter("currSysLevel", SearchFilter.Operator.IN, inputDTO.getCurrSysLevelToArray()));
				if (inputDTO.getBinding0binded1() == 1 && queryAll && !CollectionUtils.isEmpty(unLockedOutCardIds)) {
					filterListOr.add(new SearchFilter("currSysLevel", SearchFilter.Operator.IN,
							inputDTO.getCurrSysLevelToArray()));
				}
			}
			if (inputDTO.getTypeOfIssueToArray() != null && inputDTO.getTypeOfIssueToArray().length > 0) {
				filterToList.add(new SearchFilter("type", SearchFilter.Operator.IN, inputDTO.getTypeOfIssueToArray()));
				if (inputDTO.getBinding0binded1() == 1 && queryAll && !CollectionUtils.isEmpty(unLockedOutCardIds)) {
					filterListOr
							.add(new SearchFilter("type", SearchFilter.Operator.EQ, AccountType.OutBank.getTypeId()));
				}
			}
			if (bizAccount.getAccount() != null && !bizAccount.getAccount().equals("")) {
				filterToList.add(new SearchFilter("account", SearchFilter.Operator.LIKE, bizAccount.getAccount()));
				if (inputDTO.getBinding0binded1() == 1 && queryAll && !CollectionUtils.isEmpty(unLockedOutCardIds)) {
					filterListOr.add(new SearchFilter("account", SearchFilter.Operator.LIKE, bizAccount.getAccount()));
				}
			}
			if (StringUtils.isNotBlank(bizAccount.getAlias())) {
				filterToList.add(new SearchFilter("alias", SearchFilter.Operator.EQ, bizAccount.getAlias()));
				if (inputDTO.getBinding0binded1() == 1 && queryAll && !CollectionUtils.isEmpty(unLockedOutCardIds)) {
					filterListOr.add(new SearchFilter("alias", SearchFilter.Operator.EQ, bizAccount.getAlias()));
				}
			}
			if (bizAccount.getOwner() != null && !bizAccount.getOwner().equals("")) {
				filterToList.add(new SearchFilter("owner", SearchFilter.Operator.LIKE, bizAccount.getOwner()));
				if (inputDTO.getBinding0binded1() == 1 && queryAll && !CollectionUtils.isEmpty(unLockedOutCardIds)) {
					filterListOr.add(new SearchFilter("owner", SearchFilter.Operator.LIKE, bizAccount.getOwner()));
				}
			}
			// 只能查询自己区域的账号
			if (!sysUser.getCategory().equals(-1)) {
				List<SysUserProfile> manilaUserPro = sysUserProfileService.findByPropertyKey("HANDICAP_MANILA_ZONE");
				String manilaHandicap = manilaUserPro.get(0).getPropertyValue();
				List<SysUserProfile> taiWanuserPro = sysUserProfileService.findByPropertyKey("HANDICAP_TAIWAN_ZONE");
				String taiwanHandicap = taiWanuserPro.get(0).getPropertyValue();
				BizHandicap bizHandicap = handicapService.findFromCacheById(sysUser.getHandicap());
				List<Integer> handicapIds = handicapService
						.findByZone(bizHandicap == null ? "0" : bizHandicap.getId().toString());
				String handicapId = CollectionUtils.isEmpty(handicapIds) ? "0" : handicapIds.get(0).toString();
				getHandicapThirdDraw(inputDTO, sysUser, filterToList, filterListOr, queryAll, unLockedOutCardIds,
						manilaHandicap, handicapId);
				getHandicapThirdDraw(inputDTO, sysUser, filterToList, filterListOr, queryAll, unLockedOutCardIds,
						taiwanHandicap, handicapId);

			}

			SearchFilter[] filterToArray = filterToList.toArray(new SearchFilter[filterToList.size()]);
			SearchFilter[] filterToArrayOr = CollectionUtils.isEmpty(filterListOr) ? null
					: filterListOr.toArray(new SearchFilter[filterListOr.size()]);
			Specification<BizAccount> specif = (null == filterToArrayOr || filterToArrayOr.length == 0)
					? DynamicSpecifications.build(BizAccount.class, filterToArray)
					: DynamicSpecifications.buildAndOr(filterToArray, filterToArrayOr);
			Page<BizAccount> page = accountService.findPage(sysUser, specif, pageRequest);
			List<BizAccount> BizAccountList = accountService.statisticsInto(page.getContent(), sysUser,
					inputDTO.getIncomeAccountId());
			try {
				AccountServiceImpl.thirdDrawSortData(BizAccountList);
			} catch (Exception e) {
				if (e.getLocalizedMessage().equals("Comparison method violates its general contract!")) {
					responseData.setData(BizAccountList);
					responseData.setPage(new Paging(page));
					return mapper.writeValueAsString(responseData);
				}
			}
			// 如果是未锁定 绑定的放在前面 且是出款卡的放在最前面 然后是未绑定的
			// 返回前端的记录集合
			List<BizAccount> resAccountList = new LinkedList<>();
			// 出款卡绑定
			List<BizAccount> outCardBindedLinkedList = new LinkedList<>();
			// 出款卡未绑定
			List<BizAccount> notOutBindedCardList = new LinkedList<>();
			// 其他绑定
			List<BizAccount> otherBindedAccountList = new LinkedList<>();
			// 其他未绑定
			List<BizAccount> otherUnBindAccountList = new LinkedList<>();
			if (inputDTO.getBinding0binded1() == 1) {
				// 已绑定的
				if (!CollectionUtils.isEmpty(issueToList)) {
					// 对查询出的记录遍历
					for (BizAccount account : BizAccountList) {
						// limitOut存储 单次可下发余额
						AccountBaseInfo baseInfo = accountService.getFromCacheById(account.getId());
						Integer singleTimeAvailableAmount = accountChangeService.currCredits(baseInfo);
						logger.debug("单次可以下发的金额:{},账号信息:{}", singleTimeAvailableAmount,
								ObjectMapperUtils.serialize(baseInfo));
						account.setLimitOut(singleTimeAvailableAmount > 0 ? singleTimeAvailableAmount : 0);
						if (issueToList.contains(account.getId())) {
							if (unLockedOutCardIds.contains(account.getId().toString())) {
								// 绑定标识
								account.setBindId(5);
								// 绑定的出款卡
								outCardBindedLinkedList.add(account);
							} else {
								// 绑定标识
								account.setBindId(1);
								// 绑定的其他卡
								otherBindedAccountList.add(account);
							}
						} else {
							if (unLockedOutCardIds.contains(account.getId().toString())) {
								// 绑定标识
								account.setBindId(5);
								// 出款卡未绑定
								notOutBindedCardList.add(account);
							} else {
								// 未绑定的其他卡
								otherUnBindAccountList.add(account);
							}
						}

					}
				} else {
					// 没有绑定的记录
					// 对查询出的记录遍历
					for (BizAccount account : BizAccountList) {
						// limitOut存储 单次可下发余额
						AccountBaseInfo baseInfo = accountService.getFromCacheById(account.getId());
						Integer singleTimeAvailableAmount = accountChangeService.currCredits(baseInfo);
						logger.debug("单次可以下发的金额:{},账号信息:{}", singleTimeAvailableAmount,
								ObjectMapperUtils.serialize(baseInfo));
						account.setLimitOut(singleTimeAvailableAmount > 0 ? singleTimeAvailableAmount : 0);
						if (unLockedOutCardIds.contains(account.getId().toString())) {
							// 绑定标识
							account.setBindId(5);
							// 出款卡 未绑定的
							notOutBindedCardList.add(account);
						} else {
							// 未绑定的其他卡
							otherUnBindAccountList.add(account);
						}
					}
				}
				if (!CollectionUtils.isEmpty(outCardBindedLinkedList)) {
					resAccountList.addAll(outCardBindedLinkedList);
				}
				if (!CollectionUtils.isEmpty(notOutBindedCardList)) {
					resAccountList.addAll(notOutBindedCardList);
				}
				if (!CollectionUtils.isEmpty(otherBindedAccountList)) {
					resAccountList.addAll(otherBindedAccountList);
				}
				if (!CollectionUtils.isEmpty(otherUnBindAccountList)) {
					resAccountList.addAll(otherUnBindAccountList);
				}
			} else {
				resAccountList = BizAccountList;
				List<BizAccount> lockedOutCard = new LinkedList<>(), lockedOther = new LinkedList<>();
				for (BizAccount account : resAccountList) {
					AccountBaseInfo baseInfo = accountService.getFromCacheById(account.getId());
					Integer singleTimeAvailableAmount = accountChangeService.currCredits(baseInfo);
					logger.debug("单次可以下发的金额:{},账号信息:{}", singleTimeAvailableAmount,
							ObjectMapperUtils.serialize(baseInfo));
					account.setLimitOut(singleTimeAvailableAmount > 0 ? singleTimeAvailableAmount : 0);
					if (AccountType.OutBank.getTypeId().equals(baseInfo.getType())) {
						// 绑定标识
						account.setBindId(5);
						lockedOutCard.add(account);
					} else {
						lockedOther.add(account);
					}
				}
				resAccountList.clear();
				if (!CollectionUtils.isEmpty(lockedOutCard)) {
					resAccountList.addAll(lockedOutCard);
				}
				if (!CollectionUtils.isEmpty(lockedOther)) {
					resAccountList.addAll(lockedOther);
				}
			}
			responseData.setData(resAccountList);
			responseData.setPage(new Paging(page));
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(String.format("%s，参数：%s，结果：%s", "获取下发银行卡列表", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 下发任务 全部 我的锁定<br>
	 * 查询逻辑:先获取四种状态的卡id，然后根据id以及页面输入的各个条件查询<br>
	 * 然后对查询结果封装各种数据信息；对各个状态的数据按照查询结果过滤返回页面显示数量<br>
	 *
	 * @param inputDTO
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping(value = "/findDrawTask", method = RequestMethod.POST)
	public String findDrawTask(@RequestBody FindDrawTaskInputDTO inputDTO) throws JsonProcessingException {
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请登录!"));
		}
		// 只能查询自己区域的账号
		String[] handicaps = null;
		if (Objects.nonNull(inputDTO.getHandicap()))
			handicaps = new String[] { String.valueOf(inputDTO.getHandicap()) };
		if (Objects.isNull(handicaps)) {
			List<String> tmp = handicapService.handicapIdList(sysUser).stream().filter(Objects::nonNull)
					.map(String::valueOf).collect(Collectors.toList());
			if (CollectionUtils.isEmpty(tmp))
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "用户无盘口权限!"));
			handicaps = tmp.toArray(new String[tmp.size()]);
		}
		// 查询条件
		if (inputDTO.getStatus() != null || inputDTO.getLocked() != null
				|| StringUtils.isNotBlank(inputDTO.getAccount()) || StringUtils.isNotBlank(inputDTO.getOwner())
				|| StringUtils.isNotBlank(inputDTO.getBankName()) || StringUtils.isNotBlank(inputDTO.getBankType())
				|| inputDTO.getLocked() != null) {
			inputDTO.setPageNo(0);
		}
		inputDTO.setPageSize(inputDTO.getPageSize() == null ? AppConstants.PAGE_SIZE : inputDTO.getPageSize());
		GeneralResponseData<List<FindDrawTaskOutputDTO>> res = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功!");
		inputDTO.setSysUser(sysUser);

		// 全部(下发任务:新下发任务 正在下发)
		boolean queryAll = null != inputDTO.getPageFlag() && inputDTO.getPageFlag().intValue() == 1;
		// 已锁定的(我已锁定:正在下发 等待到账)
		boolean queryMyLocked = null != inputDTO.getPageFlag() && inputDTO.getPageFlag().intValue() == 2;

		// 新的下发任务
		boolean queryNewTaskInAll = queryAll && null != inputDTO.getQueryStatus()
				&& inputDTO.getQueryStatus().intValue() == 1;
		// 锁定和下发中的
		boolean queryLockedAndDrawingInAll = queryAll && null != inputDTO.getQueryStatus()
				&& inputDTO.getQueryStatus().intValue() == 2;
		// 已锁定页签 正在下发(锁定的)
		boolean queryLockedByOne = queryMyLocked && null != inputDTO.getQueryStatus()
				&& inputDTO.getQueryStatus().intValue() == 1;
		// 已锁定页签 等待到账
		boolean queryUnfinishedByOne = queryMyLocked && null != inputDTO.getQueryStatus()
				&& inputDTO.getQueryStatus().intValue() == 2;

		inputDTO.setQueryNewTaskInAll(queryNewTaskInAll);
		inputDTO.setQueryLockedAndDrawingInAll(queryLockedAndDrawingInAll);
		inputDTO.setQueryLockedByOne(queryLockedByOne);
		inputDTO.setQueryUnfinishedByOne(queryUnfinishedByOne);

		// 全部 新下发任务
		logger.debug("从redis查询新任务 开始:{}", System.currentTimeMillis());
		List<Integer> newDrawTask = null;
		try {
			newDrawTask = newTaskCache.get("ALL");
			if (!CollectionUtils.isEmpty(newDrawTask)) {
				// 测试中发现 如果某个账号在黑名单 redis：RecycleBindCommSet里 会查出该账号id 要过滤掉
				List<String> excluded = accountService.excludedIds();
				newDrawTask = newDrawTask.stream().filter(p -> !excluded.contains(p.toString()))
						.collect(Collectors.toList());
			}
			logger.info("获取的账号id:{}", newDrawTask.toString());
		} catch (ExecutionException e) {
			logger.error("获取新任务缓存 异常:", e);
		}
		logger.debug("从redis查询新任务 结束:{},结果:{} ", System.currentTimeMillis(), newDrawTask.toString());
		logger.debug("从redis查询下发中的任务 开始:{}", System.currentTimeMillis());
		// 全部 正在下发
		List<Integer> lockedOrUnfinishedDrawTask = null;
		try {
			lockedOrUnfinishedDrawTask = lockedAndUnfinishCache.get("ALL");
		} catch (ExecutionException e) {
			logger.error("获取下发中缓存 异常:", e);
		}
		logger.debug("从redis查询下发中的任务 结束:{},结果:{} ", System.currentTimeMillis(), lockedOrUnfinishedDrawTask.toString());
		// 我的锁定 正在下发
		logger.debug("从redis查询已锁定正在下发的任务 开始:{}", System.currentTimeMillis());
		List<Integer> lockedByOneDrawTask = null;
		try {
			lockedByOneDrawTask = lockedByOneCache.get(sysUser.getId().toString());
		} catch (ExecutionException e) {
			logger.error("获取用户id:{} 锁定的缓存 异常:", sysUser.getId(), e);
		}
		logger.debug("从redis查询已锁定正在下发的任务 结束:{},结果:{}", System.currentTimeMillis(), lockedByOneDrawTask.toString());
		logger.debug("从redis查询已锁定等待到账的任务 开始:{}", System.currentTimeMillis());
		// 我的锁定 等待到账
		List<Integer> unfinishedByOneDrawTask = null;
		try {
			unfinishedByOneDrawTask = unfinishedByOneCache.get(sysUser.getId().toString());
			logger.debug("用户:{}已下发，未到账的记录:{}", sysUser.getUid(), unfinishedByOneDrawTask.toString());
		} catch (ExecutionException e) {
			logger.error("获取用户id:{} 已下发未匹配的缓存 异常:", sysUser.getId(), e);
		}
		logger.debug("从redis查询已锁定等待到账的任务 结束:{},结果:{} ", System.currentTimeMillis(), unfinishedByOneDrawTask.toString());

		inputDTO.setNewDrawTask(newDrawTask);
		inputDTO.setLockedOrUnfinishedDrawTask(lockedOrUnfinishedDrawTask);
		inputDTO.setLockedByOneDrawTask(lockedByOneDrawTask);
		inputDTO.setUnfinishedByOneDrawTask(unfinishedByOneDrawTask);

		logger.debug("从数据库查询 开始:{}", System.currentTimeMillis());
		List<Integer> allCardIds = new ArrayList();
		if (!CollectionUtils.isEmpty(newDrawTask)) {
			allCardIds.addAll(newDrawTask);
		}
		if (!CollectionUtils.isEmpty(lockedOrUnfinishedDrawTask)) {
			allCardIds.addAll(lockedOrUnfinishedDrawTask);
		}
		// 去重
		Set<Integer> allCardIdsSet = new HashSet<>();
//		allCardIdsSet.addAll(allCardIds);

		// 出款卡  已回收的卡 过滤
		if (!CollectionUtils.isEmpty(allCardIds)) {
			Set<String> black = allocateTransferService.findBlackList();
			if (!CollectionUtils.isEmpty(black)) {
				for (String acc : black) {
					for (Integer card :allCardIds){
						if (!acc.contains(card + "")){
							allCardIdsSet.add(card);
						}
					}
				}
				inputDTO.setAllCardIds(Lists.newArrayList(allCardIdsSet));
			}else {
				inputDTO.setAllCardIds(Lists.newArrayList(allCardIds));
			}
		}
		logger.info("查询 参数:{}", inputDTO.toString());
		FindDrawTaskResult drawTaskResult = accountService.findDrawTask(inputDTO, handicaps);
		List<FindDrawTaskOutputDTO> data = drawTaskResult.getList();
		logger.debug("从数据库查询 结束:{}   ", System.currentTimeMillis());
		logger.debug("查询结果:{}", data.toString());

		int pageSize = null == inputDTO.getPageSize() ? AppConstants.PAGE_SIZE : inputDTO.getPageSize();
		Paging page;
		if (CollectionUtils.isEmpty(data)) {
			page = CommonUtils.getPage(0, pageSize, "0");
			res.setData(null);
		} else {
			page = CommonUtils.getPage(inputDTO.getPageNo() + 1, pageSize, String.valueOf(data.size()));
			List<FindDrawTaskOutputDTO> data2 = data;
			int counts = data.size();
			int pageNo = inputDTO.getPageNo();
			int average = counts / pageSize;
			int remainder = counts % pageSize;
			if (average == 0) {
				// 总记录数 小于pageSize的情况
				pageNo = 0;
				data2 = data.subList(0, counts);
			}
			if (average > 0) {
				if (remainder == 0) {
					if (pageNo < average) {
						data2 = data.subList(pageNo * pageSize, (pageNo + 1) * pageSize);
					}
				}
				if (remainder > 0) {
					if (pageNo >= average) {
						if (counts < pageNo * pageSize) {
							pageNo = 0;
							data2 = data.subList(pageNo * pageSize, (pageNo + 1) * pageSize);
						} else {
							data2 = data.subList(pageNo * pageSize, counts);
						}

					} else {
						data2 = data.subList(pageNo * pageSize, (pageNo + 1) * pageSize);
					}
				}
			}
			res.setData(data2);
		}
		res.setPage(page);
		res.getPage().setHeader(drawTaskResult.getCountsMap());
		res.getPage().getHeader().putAll(drawTaskResult.getSumAmountMap());

		cacheCounts.invalidate(sysUser.getId().toString());
		cacheCounts.put(sysUser.getId().toString(), drawTaskResult.getCountsMap());
		refreshCache(sysUser.getId());
		return mapper.writeValueAsString(res);

	}

	private void getHandicapThirdDraw(FindBindIssueInputDTO inputDTO, SysUser sysUser, List<SearchFilter> filterToList,
			List<SearchFilter> filterListOr, boolean queryAll, List<String> unLockedOutCardIds, String manilaHandicap,
			String handicapId) {
		if (ArrayUtils.contains(manilaHandicap.substring(1).split(";"),
				(sysUser.getCategory() > 400 ? (sysUser.getCategory() - 400) + "" : handicapId))) {
			String[] handicaps = manilaHandicap.substring(1).split(";");
			filterToList.add(new SearchFilter("handicapId", SearchFilter.Operator.IN, handicaps));
			if (inputDTO.getBinding0binded1() == 1 && queryAll && !CollectionUtils.isEmpty(unLockedOutCardIds)) {
				filterListOr.add(new SearchFilter("handicapId", SearchFilter.Operator.IN, handicaps));
			}
		}
	}

	/**
	 * 获取第三方下发银行卡列表
	 */
	@RequestMapping("/findthirdissue")
	public String findThirdIssue(@RequestParam(value = "count") int count,
			@RequestParam(value = "typeList") Integer[] typeList,
			@RequestParam(value = "bankBrandList", required = false) String[] bankBrandList)
			throws JsonProcessingException {
		String params = buildParams().toString();
		logger.trace(String.format("%s，参数：%s", "获取第三方下发银行卡列表", params));
		try {
			GeneralResponseData<List<BizAccount>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			List<SearchFilter> filterToList = new ArrayList<>();
			if (bankBrandList != null && bankBrandList.length > 0) {
				filterToList.add(new SearchFilter("bankType", SearchFilter.Operator.IN, bankBrandList));
			}
			// 查询公用银行卡和第三方用
			filterToList.add(new SearchFilter("type", SearchFilter.Operator.IN, typeList));
			filterToList.add(new SearchFilter("status", SearchFilter.Operator.EQ, AccountStatus.Normal.getStatus()));
			SearchFilter[] filterToArray = filterToList.toArray(new SearchFilter[filterToList.size()]);
			PageRequest pageable = new PageRequest(0, count, Sort.Direction.ASC, "incomeAmountDaily");
			SysUser user = (SysUser) SecurityUtils.getSubject().getPrincipal();
			List<Integer> accountIdList = accountService.findAccountIdList(filterToArray);
			Page<BizAccount> accounts = accountService.findPageOrderByAmountDaily(user, accountIdList, pageable);
			if (accounts != null) {
				responseData.setData(accounts.getContent());
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error(String.format("%s，参数：%s，结果：%s", "获取第三方下发银行卡列表", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 获取入款账号列表（根据下发卡账号）
	 */
	@RequestMapping("/findIncomeByIssueAccountId")
	public String findIncomeByIssueAccountId(@RequestParam(value = "issueAccountId") Integer issueAccountId,
			@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "statusOfIncomeToArray", required = false) Integer[] statusOfIncomeToArray,
			@RequestParam(value = "search_LIKE_account", required = false) String search_LIKE_account,
			@RequestParam(value = "search_LIKE_owner", required = false) String search_LIKE_owner)
			throws JsonProcessingException {
		String params = buildParams().toString();
		logger.debug(String.format("%s，参数：%s", "获取入款账号列表（根据下发卡账号）", params));
		try {
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			GeneralResponseData<List<BizAccount>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			List<SearchFilter> filterToList = new ArrayList<>();
			Integer[] incomeTypeToArray = { AccountType.InWechat.getTypeId(), AccountType.InThird.getTypeId(),
					AccountType.InAli.getTypeId() };
			List<Integer> incomeToList = accountBindingService.findAccountId(issueAccountId);
			filterToList.add(new SearchFilter("type", SearchFilter.Operator.IN, incomeTypeToArray));
			if (incomeToList.size() > 0) {
				filterToList.add(new SearchFilter("id", SearchFilter.Operator.IN, incomeToList.toArray()));
			} else {
				filterToList.add(new SearchFilter("id", SearchFilter.Operator.EQ, 0));
			}
			if (null != search_LIKE_account && StringUtils.isNotBlank(search_LIKE_account)) {
				filterToList.add(new SearchFilter("account", SearchFilter.Operator.LIKE, search_LIKE_account));
			}
			if (null != search_LIKE_owner && StringUtils.isNotBlank(search_LIKE_owner)) {
				filterToList.add(new SearchFilter("owner", SearchFilter.Operator.LIKE, search_LIKE_owner));
			}
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					Sort.Direction.ASC, "id");
			if (statusOfIncomeToArray != null && statusOfIncomeToArray.length > 0) {
				filterToList.add(new SearchFilter("status", SearchFilter.Operator.IN, statusOfIncomeToArray));
			}
			SearchFilter[] filterToArray = filterToList.toArray(new SearchFilter[filterToList.size()]);
			Specification<BizAccount> specif = DynamicSpecifications.build(BizAccount.class, filterToArray);
			Page<BizAccount> page = accountService.findPage(sysUser, specif, pageRequest);
			responseData.setData(page.getContent());
			responseData.setPage(new Paging(page));
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error(String.format("%s，参数：%s，结果：%s", "获取入款账号列表（根据下发卡账号）", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 下发卡绑定与解绑
	 */
	@PostMapping(value = "/bindOrUnbindForIssue", consumes = "application/json")
	public String bindOrUnbindForIssue(@Validated @RequestBody BindOrUnBindInputDTO inputDTO, Errors errors)
			throws JsonProcessingException {
		if (errors.hasErrors()) {
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数有空"));
		}
		String params = buildParams().toString();
		try {
			GeneralResponseData<List<BizAccount>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			logger.debug(String.format("%s，操作人员：%s，参数：%s", "下发卡绑定与解绑", operator.getUid(), params));
			accountBindingService.bindOrUnbind(inputDTO.getIssueAccountId(), inputDTO.getIncomeAccountId(),
					inputDTO.getBinding0binded1().intValue());
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error(String.format("%s，参数：%s，结果：%s", "下发卡绑定与解绑", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 第三方提现 锁定之后 输入金额 扣除第三方账号系统余额 解锁之后要 加回
	 *
	 * @param map
	 *            {amount:10,id:第三方账号id,toId:出款账号id}
	 */
	@RequestMapping(value = "/dealBalance", method = RequestMethod.POST)
	public void dealThirdAccountBalance(@RequestBody Map map) {
		logger.debug("参数:{}", ObjectMapperUtils.serialize(map));
		if (CollectionUtils.isEmpty(map)) {
			return;
		}
		if (map.get("id") == null && map.get("toId") == null && map.get("amount") == null) {
			return;
		}
		Integer id = Integer.valueOf(map.get("id").toString());
		Integer toId = map.get("toId") != null ? Integer.valueOf(map.get("toId").toString()) : null;
		// 包含实际出款金额和手续费
		BigDecimal amount = map.get("amount") != null ? new BigDecimal(map.get("amount").toString()) : null;
		if (toId == null || amount == null) {
			accountService.removeAmountInputStored(id, toId);
			return;
		}
		accountService.dealSysBalanceLockedOrUnlocked(id, toId, amount);
	}

	/**
	 * 描述：第三方下发到出款卡或者下发卡或者备用卡,需要锁定目标账号锁或者解除锁定 锁定：一个目标账号只能一个人锁定,不能同时锁定 解锁：解锁之后可以锁定
	 *
	 * @param fromId
	 *            第三方账号
	 * @param toId
	 *            目标账号
	 * @param lock1OrUnlock0
	 *            锁定标识：1 锁定 0 解锁 10是其他卡锁定第三方卡 20是其他卡解锁第三方卡
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/lockOrUnlock")
	public String lockOrUnlock(@RequestParam(value = "toId") Integer toId,
			@RequestParam(value = "fromId") Integer fromId,
			@RequestParam(value = "lock1OrUnlock0") Integer lock1OrUnlock0) throws JsonProcessingException {
		String params = buildParams().toString();
		try {
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (operator == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆 "));
			}
			AccountBaseInfo baseInfoFrom = accountService.getFromCacheById(fromId);
			if (baseInfoFrom == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "需要下发的第三方账号不存在!"));
			}
			AccountBaseInfo baseInfoTo = accountService.getFromCacheById(toId);
			if (baseInfoTo == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "需要锁定的目标账号不存在!"));
			}
			if (accountService.judgeLocked(toId)) {
				logger.info("账号id:{} 已被锁定");
				return mapper.writeValueAsString(new GeneralResponseData<>(
						GeneralResponseData.ResponseStatus.FAIL.getValue(), "账号id:" + toId + " 已被锁定!"));
			}
			GeneralResponseData<BizAccount> responseData = null;
			logger.info(String.format("%s，操作人员：%s，参数：%s", "账号锁定与解锁", operator.getUid(), params));
			long ret = 1L;
			boolean thirdToOutCard = null != baseInfoTo.getType()
					&& baseInfoTo.getType().equals(AccountType.OutBank.getTypeId());
			if (lock1OrUnlock0 == 1) {
				// 是否在 下发任务页签里 已经被锁定了
				boolean isLockedInDrawTaskPage = accountService.isLockedInDrawTaskPage(toId);
				if (!isLockedInDrawTaskPage) {
					if (thirdToOutCard) {
						ret = accountService.lockOutCardNeedAmount(operator.getId(), toId, fromId);
					} else {
						ret = accountService.lockThirdInAccount4Draw(operator.getId(), toId, fromId);
					}
				}
			} else {
				if (thirdToOutCard) {
					ret = accountService.unlockedThirdToDrawList(operator.getId(), toId, fromId);
				} else {
					ret = accountService.unlockThirdInAccount4Draw(operator.getId(), toId, null);
				}
			}
			if (thirdToOutCard) {
				if (ret == 1) {
					List<Integer> toDelete = new ArrayList() {
						{
							add(toId);
						}
					};
					if (lock1OrUnlock0 == 1) {

						// accountService.saveLocked(operator.getId(), toDelete,
						// true);
						responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
								"锁定成功");
					} else {
						// accountService.saveLocked(operator.getId(), toDelete,
						// false);
						responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
								"解锁成功");
						accountService.removeAmountInputStored(fromId, toId);
					}
				} else if (ret == -1) {
					if (lock1OrUnlock0 == 1) {
						responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
								"不能重复锁定!");
					} else {
						responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
								"解锁无法获取jedis实例!");
					}
				} else if (ret == -2) {
					if (lock1OrUnlock0 == 1) {
						responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
								"获取不到锁,请稍后!");
					} else {
						responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
								"锁不存在无法解锁!");
					}
				} else if (ret == -3) {
					if (lock1OrUnlock0 == 1) {
						responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
								"锁定异常!");
					} else {
						responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
								"解锁异常!");
					}
				} else if (ret == -4) {
					if (lock1OrUnlock0 == 1) {
						responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
								"锁定异常!");
					} else {
						responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
								"llockstatus解锁异常,请重试!");
					}
				} else if (ret == -5) {
					if (lock1OrUnlock0 == 1) {
						responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
								"上一笔下发还未完成!");
					}
				} else if (ret == -6) {
					if (lock1OrUnlock0 == 1) {
						responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
								"已被其他方式锁定!");
					} else {
						responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
								"锁定记录不存在!");
					}

				} else if (ret == -7) {
					if (lock1OrUnlock0 == 1) {
						responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
								"下发超额不能锁定!");
					}
				} else if (ret == -8) {
					if (lock1OrUnlock0 == 1) {
						responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
								"已被其他人锁定不能锁定!");
					}
				} else if (ret == -9) {
					if (lock1OrUnlock0 == 1) {
						responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
								"未绑定不能锁定!");
					}
				} else if (ret == -10) {
					if (lock1OrUnlock0 == 1) {
						responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
								"添加到llcok失败,不能锁定!");
					}
				} else if (ret == 0) {
					if (lock1OrUnlock0 == 1) {
						responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
								"其他异常不能锁定!");
					}
				}

			} else {
				if (ret == 1) {
					List<Integer> toDelete = new ArrayList() {
						{
							add(toId);
						}
					};
					if (lock1OrUnlock0 == 1) {
						// accountService.saveLocked(operator.getId(), toDelete,
						// true);
						responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
								"锁定成功");
					} else {
						// accountService.saveLocked(operator.getId(), toDelete,
						// false);
						responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
								"解锁成功");
					}
				} else if (ret == 0) {
					if (lock1OrUnlock0 == 1) {
						responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
								"该账号已被锁定");
					}
				} else {
					if (lock1OrUnlock0 == 1) {
						responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
								"锁定失败");
					}
				}
			}
			return mapper.writeValueAsString(responseData);
		} catch (

		Exception e) {
			logger.error(String.format("%s，参数：%s，结果：%s", "账号锁定与解锁", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"锁定失败 : " + e.getLocalizedMessage()));
		}
	}

	/***
	 * 转永久冻结（所有账号）
	 */
	@RequestMapping("/toFreezeForver")
	public String toFreezeForver(@RequestParam(value = "accountId") Integer accountId,
			@RequestParam(value = "remark", required = false) String remark) throws JsonProcessingException {
		String params = buildParams().toString();
		try {
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			SysUser sysUser = userService.findFromCacheById(1);// 管理员 以后需要改动
			// 根据操作人获取所属主管的信息
			logger.info(String.format("%s，操作人员：%s，参数：%s", "转永久冻结（所有账号）", operator.getUid(), params));
			Date d = new Date();
			BizAccount account = accountService.getById(accountId);
			BizAccount oldAccount = new BizAccount();
			BeanUtils.copyProperties(oldAccount, account);
			if (oldAccount.getType().equals(AccountType.InBank.getTypeId())) {
				// 入款卡不能有持卡人
				account.setHolder(null);
			} else {
				account.setHolder(operator.getId());
			}
			account.setUpdateTime(d);
			account.setRemark(CommonUtils.genRemark(account.getRemark(),
					"【转" + AccountStatus.Freeze.getMsg() + "】" + (remark != null ? remark : ""), d, operator.getUid()));
			account.setStatus(AccountStatus.Freeze.getStatus());
			account.setUpdateTime(d);
			accountService.updateBaseInfo(account);
			accountExtraService.saveAccountExtraLog(oldAccount, account, operator.getUid());
			logger.debug("toFreezeForver>> id {}", account.getId());
			accountService.broadCast(account);
			hostMonitorService.update(account);
			// 冻结的时候添加到待处理业务表、如果存在没有处理完的则不添加
			int count = finLessStatService.findCountsById(accountId, "portion");
			if (count <= 0)
				finLessStatService.addTrace(accountId, account.getBankBalance());
			String map = mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "冻结成功！"));
			// 有备注参数 则通知主管 冻结账号使用
			redisService.convertAndSend(RedisTopics.REFRESH_USER, sysUser.getId().toString());
			cabanaService.updAcc(account.getId());
			if (Objects.equals(AccountType.InBank.getTypeId(), account.getType()) && account.getPassageId() != null) {
				accountService.modifyInBankStatus(account, AccountStatus.Freeze.getStatus(), null, false);
			}
			return map;
		} catch (Exception e) {
			logger.error("转永久冻结出错 ", e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 转可用
	 */
	@RequestMapping("/toEnabled")
	public String toEnabled(@RequestParam(value = "accountId") int accountId) throws JsonProcessingException {
		String params = buildParams().toString();
		try {
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (Objects.isNull(operator)) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			logger.info(String.format("%s，操作人员：%s，参数：%s", "转可用", operator.getUid(), params));
			BizAccount account = accountService.getById(accountId);
			BizAccount oldAccount = new BizAccount();
			BeanUtils.copyProperties(oldAccount, account);
			account.setStatus(AccountStatus.Enabled.getStatus());
			if (oldAccount.getType().equals(AccountType.InBank.getTypeId())) {
				// 入款卡不能有持卡人
				account.setHolder(null);
			} else {
				account.setHolder(operator.getId());
			}
			Date date = new Date();
			account.setUpdateTime(date);
			accountService.updateBaseInfo(account);
			accountExtraService.saveAccountExtraLog(oldAccount, account, operator.getUid());
			logger.debug("toEnabled>> id {}", account.getId());
			accountService.broadCast(account);
			hostMonitorService.update(account);
			cabanaService.updAcc(account.getId());
			// systemAccountManager.rpush(new SysBalPush(accountId,
			// SysBalPush.CLASSIFY_INIT,
			// new ReportInitParam(accountId, operator.getId(), "停用卡转在用")));
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			logger.error(String.format("%s，参数：%s，结果：%s", "转可用", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	/***
	 * 转临时停用
	 */
	@RequestMapping("/toStopTemp")
	public String toStopTemp(@RequestParam(value = "accountId") int accountId,
			@RequestParam(value = "remark", required = false) String remark) throws JsonProcessingException {
		String params = buildParams().toString();
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			BizAccount db = accountService.getById(accountId);
			BizAccount oldAccount = new BizAccount();
			BeanUtils.copyProperties(oldAccount, db);
			logger.info(String.format("%s，操作人员：%s，参数：%s", "转临时停用", operator.getUid(), params));
			accountService.toStopTemp(accountId, remark, operator.getId());
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error(String.format("%s，参数：%s，结果：%s", "转临时停用", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 分配（出款账号）/在用（入款卡）
	 */
	@RequestMapping("/asin4OutwardAccount")
	public String asin4OutwardAccount(@RequestParam(value = "accountId") Integer accountId,
			@RequestParam(value = "operatorId", required = false) Integer operatorId,
			@RequestParam(value = "remark", required = false) String remark) throws JsonProcessingException {
		SysUser loginUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(loginUser)) {
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		String params = buildParams().toString();
		try {
			SysUser operator = operatorId != null ? userService.findFromCacheById(operatorId) : null;
			String uid = operator == null ? "机器" : operator.getUid();
			logger.info(String.format("操作人员: %s 出款人员：%s，参数：%s", "分配（出款账号）/或入款卡在用", loginUser.getUid(), uid, params));
			BizAccount account = accountService.getById(accountId);
			BizAccount oldAccount = new BizAccount();
			BeanUtils.copyProperties(oldAccount, account);
			account.setStatus(AccountStatus.Normal.getStatus());
			if (account.getType().equals(AccountType.InBank.getTypeId())) {
				// 入款卡清空持卡人
				account.setHolder(null);
			} else {
				account.setHolder(operator != null ? operator.getId() : null);
			}
			account.setModifier(((SysUser) SecurityUtils.getSubject().getPrincipal()).getId());
			Date date = new Date();
			// 如果是从冻结转在用的，则加上备注
			if (account.getStatus().equals(AccountStatus.Freeze.getStatus())) {
				account.setRemark(CommonUtils.genRemark(account.getRemark(),
						"【" + AccountStatus.Freeze.getMsg() + "转" + AccountStatus.Normal.getMsg() + "】"
								+ (remark != null ? remark : ""),
						date, operator != null ? operator.getUid() : StringUtils.EMPTY));
			}
			account.setUpdateTime(date);
			accountService.updateBaseInfo(account);
			// opera为分配的持卡人 切换到保存方法中查询uid
			accountExtraService.saveAccountExtraLog(oldAccount, account, null);
			logger.debug("asin4OutwardAccount>> id {}", account.getId());
			accountService.broadCast(account);
			hostMonitorService.update(account);
			cabanaService.updAcc(account.getId());
			allocateTransferService.rmFrBlackList(accountId);
			boolean updAllocate = Objects.nonNull(oldAccount) && Objects.nonNull(account)
					&& (!Objects.equals(oldAccount.getType(), account.getType())
							|| !Objects.equals(oldAccount.getStatus(), account.getStatus()));
			if (updAllocate)
				incomeAccountAllocateService.update(account.getId(), account.getType(), account.getStatus());
			// systemAccountManager.rpush(new SysBalPush(accountId,
			// SysBalPush.CLASSIFY_INIT,
			// new ReportInitParam(accountId, loginUser.getId(), "使用该卡,转在用")));
			String map = mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));

			return map;
		} catch (Exception e) {
			logger.error(String.format("%s，参数：%s，结果：%s", "分配（出款账号）", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getMessage()));
		}
	}

	/**
	 * 批量分配（出款账号）
	 */
	@RequestMapping("/asin4OutwardAccountByBatch")
	public String asin4OutwardAccountByBatch(@RequestParam(value = "accArray") Integer[] accArray)
			throws JsonProcessingException {
		SysUser loginUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(loginUser))
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		if (Objects.isNull(accArray) || accArray.length == 0)
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "账号不能为空"));
		Date date = new Date();
		StringBuilder accIdSb = new StringBuilder();
		for (Integer accountId : accArray) {
			try {
				accIdSb.append(",").append(accountId.toString());
				BizAccount account = accountService.getById(accountId);
				BizAccount oldAccount = new BizAccount();
				BeanUtils.copyProperties(oldAccount, account);
				account.setStatus(AccountStatus.Normal.getStatus());
				account.setHolder(null);
				account.setModifier(loginUser.getId());
				account.setUpdateTime(date);
				accountService.updateBaseInfo(account);
				accountExtraService.saveAccountExtraLog(oldAccount, account, loginUser.getUid());
				logger.debug("asin4OutwardAccountByBatch>> id {}", account.getId());
				accountService.broadCast(account);
				hostMonitorService.update(account);
				cabanaService.updAcc(account.getId());
				allocateTransferService.rmFrBlackList(accountId);
			} catch (Exception e) {
				logger.error("批量分配（出款账号），accountId：{} ", accountId, e);
			}
		}
		logger.info("Account ( asin4OutwardAccountByBatch ) >> uid: {} accArray:{}", loginUser.getUid(),
				accIdSb.toString());
		return mapper
				.writeValueAsString(new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
	}

	/**
	 * 回收（出款账号）
	 */
	@RequestMapping("/recycle4OutwardAccount")
	public String recycle4OutwardAccount(@RequestParam(value = "accountId") Integer accountId)
			throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (operator == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		BizAccount acc = accountService.getById(accountId);
		AccountBaseInfo base = accountService.getFromCacheById(accountId);
		if (Objects.isNull(base) || Objects.isNull(acc)) {
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "账号不存在"));
		}
		BizOutwardTask allocTask = outwardTaskDao.applyTask4Robot(accountId, OutwardTaskStatus.Undeposit.getStatus());
		if (Objects.nonNull(allocTask) && Objects.nonNull(acc.getBankBalance())
				&& acc.getBankBalance().compareTo(allocTask.getAmount()) <= 0) {
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"该账号已分配一笔需补钱的出款任务，现需等待出完，或人工转出该任务后方可回收"));
		}
		String params = buildParams().toString();
		try {
			if (base != null && Objects.equals(base.getStatus(), AccountStatus.Normal.getStatus())) {
				boolean checkBlack = allocateTransferService.checkBlack(accountId);
				if (!checkBlack) {
					if (base.checkMobile() && Objects.nonNull(allocTask)) {
						return mapper.writeValueAsString(new GeneralResponseData(
								GeneralResponseData.ResponseStatus.FAIL.getValue(), "手机银行账户有笔款，不能进行回收"));
					}
					// 在下发黑名单队列中：回收该账号到可用状态
					logger.info(String.format("%s，操作人员：%s，参数：%s", "回收", operator.getUid(), params));
					recycle4OutwardAccount(accountId, operator.getId());
				} else {
					allocateTransferService.addToBlackList(accountId, false);// 不在下发黑名单中：则加入下发黑名单列表
					logger.info(String.format("%s，操作人员：%s，参数：%s", "人工回收，加入黑名单，一天后自动过期，", operator.getUid(), params));
				}
			} else {
				allocateTransferService.addToBlackList(accountId, false);// 不在下发黑名单中：则加入下发黑名单列表
				logger.info(String.format("%s，操作人员：%s，参数：%s", "人工回收，加入黑名单，一天后自动过期，", operator.getUid(), params));
			}
			// 发送取消转账命令
			List<Integer> frIdList = allocateTransferService.findFrIdList(accountId);
			if (!CollectionUtils.isEmpty(frIdList)) {
				for (Integer frId : frIdList) {
					MessageEntity<Integer> entity = new MessageEntity<>();
					entity.setAction(ActionEventEnum.CANCEL.ordinal());
					entity.setData(frId);
					entity.setIp(hostMonitorService.findHostByAcc(frId));
					hostMonitorService.messageBroadCast(entity);
					logger.info("Cancel Transfer  frId:{} toId:{}", frId, accountId);
				}
			}
			cabanaService.updAcc(accountId);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			logger.error(String.format("%s，参数：%s，结果：%s", "回收", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getMessage()));
		}
	}

	/**
	 * 批量回收（出款账号）
	 */
	@RequestMapping("/recycle4OutwardAccountByBatch")
	public String recycle4OutwardAccountByBatch(@RequestParam(value = "accArray") Integer[] accArray)
			throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator))
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		if (Objects.isNull(accArray) || accArray.length == 0)
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "批量回收账号不能为空"));
		StringBuilder accIdSb = new StringBuilder();
		for (Integer accountId : accArray) {
			accIdSb.append(",").append(accountId.toString());
			BizAccount acc = accountService.getById(accountId);
			BizOutwardTask allocTask = outwardTaskDao.applyTask4Robot(accountId,
					OutwardTaskStatus.Undeposit.getStatus());
			if (Objects.nonNull(acc) && Objects.nonNull(allocTask) && Objects.nonNull(acc.getBankBalance())
					&& acc.getBankBalance().compareTo(allocTask.getAmount()) <= 0) {
				continue;
			}
			try {
				AccountBaseInfo base = accountService.getFromCacheById(accountId);
				if (base != null && Objects.equals(base.getStatus(), AccountStatus.Normal.getStatus())) {
					if (!allocateTransferService.checkBlack(accountId)) {
						recycle4OutwardAccount(accountId, operator.getId());
					} else {
						allocateTransferService.addToBlackList(accountId, false);// 不在下发黑名单中：则加入下发黑名单列表
					}
				} else {
					allocateTransferService.addToBlackList(accountId, false);// 不在下发黑名单中：则加入下发黑名单列表
				}
			} catch (Exception e) {
				logger.error(String.format("回收 accId：%s 结果：%s", accountId, e.getMessage()));
			}
			cabanaService.updAcc(accountId);
		}
		logger.info("Account ( recycle4OutwardAccountByBatch ) >> uid: {} accArray:{}", operator.getUid(),
				accIdSb.toString());
		return mapper
				.writeValueAsString(new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
	}

	/**
	 * 回收（下发账号）
	 */
	@RequestMapping("/recycle4BindComm")
	public String recycle4BindComm(@RequestParam(value = "accountId") Integer accountId)
			throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (operator == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		accountService.recycle4BindComm(operator, accountId, true);
		return mapper
				.writeValueAsString(new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
	}

	/**
	 * 取消回收（下发账号）
	 */
	@RequestMapping("/cancelRecycle4BindComm")
	public String cancelRecycle4BindComm(@RequestParam(value = "accountId") Integer accountId)
			throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (operator == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		accountService.recycle4BindComm(operator, accountId, false);
		return mapper
				.writeValueAsString(new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
	}

	/**
	 * 账号删除
	 */
	@RequiresPermissions({ "IncomeAuditComp:deleteIncomeAccount" })
	@RequestMapping("/delete")
	public String delete(@RequestParam(value = "id") Integer id,
			@RequestParam(value = "remark", required = false) String remark) throws JsonProcessingException {
		String params = buildParams().toString();
		try {
			GeneralResponseData<BizAccount> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			BizAccount account = accountService.getById(id);
			if (null == account) {
				throw new Exception("ID为:" + id + "的账号不存在，请输入正确的参数");
			}
			if (StringUtils.isNotBlank(account.getGps())) {
				throw new Exception("账号还挂在IP" + account.getGps() + "，请移除再进行删除");
			}
			account.setStatus(AccountStatus.Delete.getStatus());
			account.setModifier(((SysUser) SecurityUtils.getSubject().getPrincipal()).getId());
			Date date = new Date();
			account.setUpdateTime(date);
			accountService.updateBaseInfo(account);
			DelAccountExtraLog(id, remark);
			logger.debug("delete>> id {}", account.getId());
			accountService.broadCast(account);
			hostMonitorService.update(account);
			accountService.updateBaseInfo(account);
			cabanaService.updAcc(account.getId());
			String result = mapper.writeValueAsString(responseData);
			logger.debug(String.format("%s，参数：%s，结果：%s", "账号删除", params, result));
			return result;
		} catch (Exception e) {
			logger.error(String.format("%s，参数：%s，结果：%s", "账号删除", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getMessage()));
		}
	}

	/**
	 * 账号彻底删除 不可回收
	 */
	@RequiresPermissions({ "UpdateSpecialAccount:DeleteAndClear" })
	@RequestMapping("/deleteAndClear")
	public String deleteAndClear(@RequestParam(value = "id") Integer id) throws JsonProcessingException {
		String params = buildParams().toString();
		try {
			GeneralResponseData<BizAccount> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			BizAccount account = accountService.getById(id);
			if (null == account) {
				throw new Exception("ID为:" + id + "的账号不存在，请输入正确的参数");
			}
			if (StringUtils.isNotBlank(account.getGps())) {
				throw new Exception("账号还挂在IP" + account.getGps() + "，请移除再进行删除");
			}
			// 查询账号是否已清算
			PageRequest pageRequest = new PageRequest(0, AppConstants.PAGE_SIZE_MAX);
			Map<String, Object> mapp = finBalanceStatService.ClearAccountDate(null, pageRequest);
			Page<Object> Page = (Page<Object>) mapp.get("Page");
			if (null != Page) {
				for (Object temp : Page.getContent()) {
					Object[] obj = (Object[]) temp;
					if (account.getAccount().equals((String) obj[0]) && account.getType().equals((Integer) obj[1])) {
						throw new Exception("ID为:" + id + "的账号" + account.getAccount() + "还有数据未清算，请先清算后再操作！");
					}
				}
			}
			// 已清算，彻底删除 1清除账号与账号绑定关系 2账号与层级的绑定关系 3操作记录 4平台同步信息 5出款操作日志
			// 6帐号每日清算汇总报表 7账号表
			accountService.deleteAndClear(id);
			String result = mapper.writeValueAsString(responseData);
			logger.debug(String.format("%s，参数：%s，结果：%s", "账号删除", params, result));
			return result;
		} catch (Exception e) {
			logger.error(String.format("%s，参数：%s，结果：%s", "账号删除", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getMessage()));
		}
	}

	/**
	 * 根据条件搜索冻结卡
	 */
	@RequestMapping("/searchFreezed")
	public String searchFreezed(@Valid BizAccount bizAccount,
			@RequestParam(value = "currSysLevelList", required = false) String[] currSysLevelList)
			throws JsonProcessingException {
		String params = buildParams().toString();
		logger.trace(String.format("%s，参数：%s", "账号分页获取", params));
		try {
			GeneralResponseData<List<BizAccount>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			List<SearchFilter> filterToList = DynamicSpecifications.build(request);
			if (null != bizAccount.getCurrSysLevel()) {
				filterToList
						.add(new SearchFilter("currSysLevel", SearchFilter.Operator.EQ, bizAccount.getCurrSysLevel()));
			}
			if (null != currSysLevelList && currSysLevelList.length > 0) {
				filterToList.add(new SearchFilter("currSysLevel", SearchFilter.Operator.IN, currSysLevelList));
			}
			// 冻结卡
			filterToList.add(new SearchFilter("status", SearchFilter.Operator.EQ, AccountStatus.Freeze.getStatus()));
			SearchFilter[] filterToArray = filterToList.toArray(new SearchFilter[filterToList.size()]);
			Specification<BizAccount> specif = DynamicSpecifications.build(BizAccount.class, filterToArray);
			// 根据搜索条件查找全部账号信息
			List<BizAccount> accountIdList = accountService.findList(null, specif);
			responseData.setData(accountIdList);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(String.format("%s，参数：%s，结果：%s", "账号分页获取", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * @param categoryArray
	 *            统计分类
	 * @param accountIdArray
	 *            统计账号集合
	 * @param fromToArray
	 *            fromId:toId
	 */
	@RequestMapping("/findStatInOut")
	public String findStatInOut(@RequestParam(value = "categoryArray", required = false) Integer[] categoryArray,
			@RequestParam(value = "accountIdArray", required = false) Integer[] accountIdArray,
			@RequestParam(value = "fromToArray", required = false) String[] fromToArray)
			throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			Map<String, Object> data = new HashMap<>();
			if (categoryArray != null && categoryArray.length > 0 && accountIdArray != null
					&& accountIdArray.length > 0) {
				List<Integer> accountIdList = Arrays.asList(accountIdArray);
				for (Integer cat : categoryArray) {
					AccountStatInOut.Category category = AccountStatInOut.Category.findCategory(cat);
					if (category != null) {
						Map<Integer, AccountStatInOut> dataMap = new HashMap<>();
						List<AccountStatInOut> dataList = accountService.findStatInOut(category, accountIdList);
						dataList.forEach(p -> dataMap.put(p.getId(), p));
						for (Integer id : accountIdArray) {
							if (!dataMap.containsKey(id)) {
								dataList.add(new AccountStatInOut(id, cat));
							}
						}
						data.put(String.valueOf(category.getValue()), dataList);
					}
				}
				data.put("matchingAmt", accountService.findStat(accountIdList));
				// data.put("sysBalAndAlarm",
				// transMonitorService.findSysBalAndAlarm(accountIdList));
			}
			if (fromToArray != null && fromToArray.length > 0) {
				data.put("fromTo", accountService.findStatFromTo(fromToArray));
			}
			responseData.setData(data);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			String params = buildParams().toString();
			logger.error(String.format("%s，参数：%s，结果：%s", "统计 findStatInOut", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getMessage()));
		}
	}

	/**
	 * 获取客户绑定卡的转入转出统计信息
	 *
	 * @param accountIdArray
	 *            客户绑定卡ID集合
	 */
	@RequestMapping("/findStat4BindCustomer")
	public String findStat4BindCustomer(
			@RequestParam(value = "accountIdArray", required = false) Integer[] accountIdArray)
			throws JsonProcessingException {
		try {
			GeneralResponseData<Map<Integer, BigDecimal[]>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			responseData.setData(accountService.findStat4BindCustomer(Arrays.asList(accountIdArray)));
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			String params = buildParams().toString();
			logger.error(String.format("%s，参数：%s，结果：%s", "统计 findStat4BindCustomer", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getMessage()));
		}
	}

	/**
	 * 获取账号表当前最大的编号
	 */
	@RequestMapping("/getMaxAlias")
	public String getMaxAlias() throws JsonProcessingException {
		String params = buildParams().toString();
		logger.trace(String.format("%s，参数：%s", "账号分页获取", params));
		try {
			GeneralResponseData<String> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			String maxAlias = accountService.getMaxAlias();
			responseData.setData(maxAlias);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error("获取账号表当前最大编号，", e.getMessage());
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * set model
	 *
	 * @param accId
	 *            account's identity
	 * @param trans
	 *            1:MOBILE 2:PC
	 * @param crawl
	 *            1:MOBILE 2:PC
	 */
	@RequestMapping("/setModel")
	public String setModel(@RequestParam(value = "accId") Integer accId, @RequestParam(value = "trans") Integer trans,
			@RequestParam(value = "crawl") Integer crawl) throws JsonProcessingException {
		try {
			accountService.setModel(accId, trans, crawl);
		} catch (Exception e) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getMessage()));
		}
		return mapper
				.writeValueAsString(new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
	}

	/**
	 * get model
	 *
	 * @param accId
	 *            account's identity
	 */
	@RequestMapping("/getModel")
	public String getModel(@RequestParam(value = "accId") Integer accId) throws JsonProcessingException {
		try {
			GeneralResponseData<String> res = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			res.setData(accountService.getModel(accId));
			return mapper.writeValueAsString(res);
		} catch (Exception e) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getMessage()));
		}

	}

	/**
	 * 删除账号信息
	 */
	private void DelAccountExtraLog(Integer accountId, String remark) {
		if (null == accountId) {
			return;
		}
		BizAccountExtra accountExtra = new BizAccountExtra();
		accountExtra.setTime(new Date());
		accountExtra.setAccountId(accountId);
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		accountExtra.setOperator(operator.getUid());
		accountExtra.setRemark("【删除账号】" + remark);
		accountExtraService.insertRow(accountExtra);
	}

	private List<Integer> build4BindingIdList(Integer[] handicapId, Integer levelId, int type) throws Exception {
		List<Integer> result = new ArrayList<>();
		if (handicapId == null) {
			return result;
		}
		List<SearchFilter> filterToList = new ArrayList<>();
		filterToList.add(new SearchFilter("type", SearchFilter.Operator.EQ, type));
		filterToList.add(new SearchFilter("handicapId", SearchFilter.Operator.IN, handicapId));
		if (levelId != null) {
			List<Integer> accountIdToList;
			if (CollectionUtils.isEmpty(accountIdToList = accountService.findAccountIdList(levelId))) {
				return result;
			}
			filterToList.add(new SearchFilter("id", SearchFilter.Operator.IN, accountIdToList.toArray()));
		}
		PageRequest pageRequest = new PageRequest(0, 100000, Sort.Direction.ASC, "id");
		SearchFilter[] filterToArray = filterToList.toArray(new SearchFilter[filterToList.size()]);
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		Specification<BizAccount> specif = DynamicSpecifications.build(BizAccount.class, filterToArray);
		List<BizAccount> incomeList = accountService.findPage(sysUser, specif, pageRequest).getContent();
		if (CollectionUtils.isEmpty(incomeList)) {
			return result;
		}
		List<Integer> incomeAccountIdList = new ArrayList<>();
		incomeList.forEach((p) -> incomeAccountIdList.add(p.getId()));
		accountBindingService.findByAccountIdList(incomeAccountIdList).forEach((p) -> result.add(p.getBindAccountId()));
		return result;
	}

	private List<Map<String, Object>> build4AliAndWechat(List<BizAccount> accountToList) {
		List<Map<String, Object>> result = new ArrayList<>();
		if (CollectionUtils.isEmpty(accountToList)) {
			return result;
		}
		List<Integer> incomeIdList = new ArrayList<>();
		Map<Integer, List<Integer>> bindingIdToIncomeIdMap = new HashMap<>();
		Map<Integer, BizAccount> incomeIdToAccountMap = new HashMap<>();
		List<Integer> issueAccountIdList = new ArrayList<>();
		// List<SearchFilter> filterToList = new ArrayList<>();
		accountToList.forEach((p) -> issueAccountIdList.add(p.getId()));
		// filterToList.add(new SearchFilter("id", SearchFilter.Operator.IN,
		// issueAccountIdList.toArray()));
		accountBindingService.findByBindAccountIdList(issueAccountIdList).forEach((p) -> {
			incomeIdList.add(p.getAccountId());
			List<Integer> idList = bindingIdToIncomeIdMap.get(p.getBindAccountId());
			if (idList == null) {
				idList = new ArrayList<>();
				bindingIdToIncomeIdMap.put(p.getBindAccountId(), idList);
			}
			idList.add(p.getAccountId());
		});
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (!CollectionUtils.isEmpty(incomeIdList)) {
			Specification<BizAccount> specif = DynamicSpecifications.build(BizAccount.class,
					new SearchFilter("id", SearchFilter.Operator.IN, incomeIdList.toArray()));
			accountService.findList(operator, specif).forEach((p) -> incomeIdToAccountMap.put(p.getId(), p));
		}
		accountToList.forEach((p) -> {
			Map<String, Object> data = new HashMap<>();
			List<Integer> tList = bindingIdToIncomeIdMap.get(p.getId());
			if (!CollectionUtils.isEmpty(tList)) {
				tList.forEach(p1 -> {
					BizAccount in = incomeIdToAccountMap.get(p1);
					if (in != null) {
						String incomeAccountIdGroup = (String) data.get("incomeAccountIdGroup");
						incomeAccountIdGroup = StringUtils.isBlank(incomeAccountIdGroup) ? in.getId().toString()
								: incomeAccountIdGroup + "<br/>" + in.getId();
						data.put("incomeAccountIdGroup", incomeAccountIdGroup);
						String incomeAccountGroup = (String) data.get("incomeAccountGroup");
						incomeAccountGroup = StringUtils.isBlank(incomeAccountGroup) ? in.getAccount()
								: incomeAccountGroup + "<br/>" + in.getAccount();
						data.put("incomeAccountGroup", incomeAccountGroup);
						String incomeHandicapNameGroup = (String) data.get("incomeHandicapNameGroup");
						incomeHandicapNameGroup = StringUtils.isBlank(incomeHandicapNameGroup) ? in.getHandicapName()
								: incomeHandicapNameGroup + "<br/>" + in.getHandicapName();
						data.put("incomeHandicapNameGroup", incomeHandicapNameGroup);
						String incomeLevelNameGroup = (String) data.get("incomeLevelNameGroup");
						incomeLevelNameGroup = StringUtils.isBlank(incomeLevelNameGroup) ? in.getLevelNameToGroup()
								: incomeLevelNameGroup + "<br/>" + in.getLevelNameToGroup();
						data.put("incomeLevelNameGroup", incomeLevelNameGroup);
						String bindToMappingGroup = (String) data.get("bindToMappingGroup");
						data.put("bindToMappingGroup",
								bindToMappingGroup == null ? "0" : bindToMappingGroup + "<br/>0");
						String bindToMappedGroup = (String) data.get("bindToMappedGroup");
						data.put("bindToMappedGroup", bindToMappedGroup == null ? "0" : bindToMappedGroup + "<br/>0");
						String bindToCancelGroup = (String) data.get("bindToCancelGroup");
						data.put("bindToCancelGroup", bindToCancelGroup == null ? "0" : bindToCancelGroup + "<br/>0");
					}
				});
			} else {
				data.put("incomeAccountIdGroup", "");
				data.put("incomeAccountGroup", "");
				data.put("incomeHandicapNameGroup", "");
				data.put("incomeLevelNameGroup", "");
				String bindToMappingGroup = (String) data.get("bindToMappingGroup");
				data.put("bindToMappingGroup", bindToMappingGroup == null ? "0" : bindToMappingGroup + "<br/>0");
				String bindToMappedGroup = (String) data.get("bindToMappedGroup");
				data.put("bindToMappedGroup", bindToMappedGroup == null ? "0" : bindToMappedGroup + "<br/>0");
				String bindToCancelGroup = (String) data.get("bindToCancelGroup");
				data.put("bindToCancelGroup", bindToCancelGroup == null ? "0" : bindToCancelGroup + "<br/>0");
			}
			data.put("bindId", p.getId());
			data.put("bindAlias", p.getAlias());
			data.put("bindCurrSysLevelName", p.getCurrSysLevelName());
			data.put("bindAccount", p.getAccount());
			data.put("bindOwner", p.getOwner());
			data.put("bindStatusName", p.getStatusName());
			data.put("bindType", p.getType());
			data.put("bindStatus", p.getStatus());
			data.put("bindInAmountDaily", p.getIncomeAmountDaily());
			data.put("bindLimitIn", p.getLimitIn());
			data.put("bindOutAmountDaily", p.getOutwardAmountDaily());
			data.put("bindLimitOut", p.getLimitOut());
			data.put("bindSysAmount", p.getBalance());
			data.put("bindBankAmount", p.getBankBalance());
			data.put("bindToMapping", "0");
			data.put("bindToMapped", "0");
			data.put("bindToCancel", "0");
			data.put("bindFromMapping", "0");
			data.put("bindFromMapped", "0");
			data.put("bindFromCancel", "0");
			result.add(data);
		});
		return result;
	}

	private Map<String, Object> buildHeader(List<Integer> accountIdList, SearchFilter[] filterToArray, int[] IdSize) {
		Map<String, Object> result = new HashMap<>();
		Integer[] accountIdArray = accountIdList.toArray(new Integer[accountIdList.size()]);

		BigDecimal totalIncomeDaily = accountService.findAmountDailyByTotal(0, accountIdArray);// income0outward1
		logger.debug("查询总入款金额 参数:{} 结果:{}", accountIdArray.toString(), totalIncomeDaily);
		BigDecimal totalOutwardDaily = accountService.findAmountDailyByTotal(1, accountIdArray);// income0outward1
		logger.debug("查询总出款金额 参数:{} 结果:{}", accountIdArray.toString(), totalOutwardDaily);
		BigDecimal[] balanceAndBankBalance = accountService.findBalanceAndBankBalanceByTotal(filterToArray);
		logger.debug("查询balanceAndBankBalance 参数:{} 结果:{}", filterToArray.toString(), balanceAndBankBalance);
		result.put("IdSize", IdSize);
		result.put("totalAmountIncomeDaily", totalIncomeDaily);
		result.put("totalAmountOutwardDaily", totalOutwardDaily);
		result.put("totalAmountBalance", balanceAndBankBalance[0]);
		result.put("totalAmountBankBalance", balanceAndBankBalance[1]);
		return result;
	}

	private List<BizAccount> buildTransAmount(int fromAccountId, List<BizAccount> accountList, SysUser oprator) {
		for (BizAccount to : accountList) {
			try {
				BigDecimal[] trans = allocateTransferService.findTrans(fromAccountId, to.getId(), oprator.getId());
				to.setTransInt(trans[0]);
				to.setTransRadix(trans[1]);
			} catch (Exception e) {
				logger.error("" + e);
			}
		}
		return accountList;
	}

	/**
	 * 去重
	 */
	private List<BizAccount> distinct(List<BizAccount> bizAccountList) {
		Set<BizAccount> set = new TreeSet<>((o1, o2) -> {
			// 字符串,则按照asicc码升序排列
			return o2.getId().compareTo(o1.getId());
		});
		set.addAll(bizAccountList);
		return new ArrayList<>(set);
	}

	public void recycle4OutwardAccount(int accountId, int oprId) throws Exception {
		BizAccount account = accountService.getById(accountId);
		BizAccount oldAccount = new BizAccount();
		SysUser opr = userService.findFromCacheById(oprId);
		BeanUtils.copyProperties(oldAccount, account);
		account.setStatus(AccountStatus.Enabled.getStatus());
		account.setHolder(oprId);
		account.setModifier(oprId);
		Date date = new Date();
		account.setUpdateTime(date);
		accountService.updateBaseInfo(account);
		accountExtraService.saveAccountExtraLog(oldAccount, account, opr.getUid());
		logger.debug("derating>> id {}", account.getId());
		accountService.broadCast(account);
		hostMonitorService.update(account);
		allocateTransferService.rmFrBlackList(accountId);
	}

	@RequestMapping("/findIssuedThird")
	public String findIssuedThird(@RequestParam(value = "fromId", required = false) int fromId,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "pageNo") int pageNo) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					Sort.Direction.DESC, "create_time");
			Map<String, Object> mapp = accountService.findIssuedThird(fromId, startAndEndTimeToArray[0],
					startAndEndTimeToArray[1], pageRequest);
			Page<Object> page = (Page<Object>) mapp.get("Page");
			List<Object> thirdIssuedList = (List<Object>) page.getContent();
			List<BizIncomeRequest> arrlist = new ArrayList<BizIncomeRequest>();
			for (int i = 0; i < thirdIssuedList.size(); i++) {
				Object[] obj = (Object[]) thirdIssuedList.get(i);
				BizIncomeRequest incomeRequest = new BizIncomeRequest();
				incomeRequest.setFromId((int) obj[0]);
				incomeRequest.setFromAccount((String) obj[1]);
				incomeRequest.setToId(obj[2] == null ? 0 : (int) obj[2]);
				incomeRequest.setToAccount((String) obj[3]);
				incomeRequest.setOrderNo((String) obj[4]);
				incomeRequest.setAmount(new BigDecimal(obj[5].toString()));
				incomeRequest.setFee(obj[6] == null ? new BigDecimal("0") : new BigDecimal(obj[6].toString()));
				incomeRequest.setStatus((int) obj[7]);
				incomeRequest.setCreateStr((String) obj[8]);
				arrlist.add(incomeRequest);
			}
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("arrlist", arrlist);
			map.put("page", new Paging(page));
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("调用第三方系统余额Controller查询失败：" + e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/findEncashThird")
	public String findEncashThird(@RequestParam(value = "fromId", required = false) int fromId,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "pageNo") int pageNo) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					Sort.Direction.DESC, "create_time");
			Map<String, Object> mapp = accountService.findEncashThird(fromId, startAndEndTimeToArray[0],
					startAndEndTimeToArray[1], pageRequest);
			Page<Object> page = (Page<Object>) mapp.get("Page");
			List<Object> thirdEncashList = (List<Object>) page.getContent();
			List<BizThirdOut> arrlist = new ArrayList<BizThirdOut>();
			for (int i = 0; i < thirdEncashList.size(); i++) {
				Object[] obj = (Object[]) thirdEncashList.get(i);
				AccountBaseInfo fromAccount = accountService.getFromCacheById((Integer) obj[0]);
				BizThirdOut bizThirdOut = new BizThirdOut();
				bizThirdOut.setFromid((int) obj[0]);
				bizThirdOut.setFromAccount(fromAccount.getAccount());
				bizThirdOut.setToAccount((String) obj[1]);
				bizThirdOut.setToAccountOwner((String) obj[2]);
				bizThirdOut.setToAccountBank((String) obj[3]);
				bizThirdOut.setAmount(new BigDecimal(obj[4].toString()));
				bizThirdOut.setFee(new BigDecimal(obj[5].toString()));
				bizThirdOut.setRemark((String) obj[6]);
				bizThirdOut.setCreateTime((String) obj[7]);
				bizThirdOut.setOperator((String) obj[8]);
				arrlist.add(bizThirdOut);
			}
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("arrlist", arrlist);
			map.put("page", new Paging(page));
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("调用第三方系统余额Controller查询失败：" + e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/findMembersThird")
	public String findMembersThird(@RequestParam(value = "fromId", required = false) int fromId,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "handicapCode", required = false) String handicapCode,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "pageNo") int pageNo) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					Sort.Direction.DESC, "create_time");
			Map<String, Object> mapp = accountService.findMembersThird(fromId, startAndEndTimeToArray[0],
					startAndEndTimeToArray[1], handicapCode.equals("null") ? null : handicapCode, pageRequest);
			Page<Object> page = (Page<Object>) mapp.get("Page");
			List<Object> thirdEncashList = (List<Object>) page.getContent();
			List<BizThirdOut> arrlist = new ArrayList<BizThirdOut>();
			for (int i = 0; i < thirdEncashList.size(); i++) {
				Object[] obj = (Object[]) thirdEncashList.get(i);
				AccountBaseInfo fromAccount = accountService.getFromCacheById((Integer) obj[0]);
				BizHandicap bizHandicap = handicapService.findFromCacheByCode((String) obj[9]);
				BizThirdOut bizThirdOut = new BizThirdOut();
				bizThirdOut.setFromid((int) obj[0]);
				bizThirdOut.setFromAccount(fromAccount.getAccount());
				bizThirdOut.setToAccount((String) obj[1]);
				bizThirdOut.setToAccountOwner((String) obj[2]);
				bizThirdOut.setToAccountBank((String) obj[3]);
				bizThirdOut.setAmount(new BigDecimal(obj[4].toString()));
				bizThirdOut.setFee(obj[5] == null ? new BigDecimal("0") : new BigDecimal(obj[5].toString()));
				bizThirdOut.setRemark((String) obj[6]);
				bizThirdOut.setCreateTime((String) obj[7]);
				bizThirdOut.setOperator((String) obj[8]);
				bizThirdOut.setHandicap(bizHandicap.getName());
				bizThirdOut.setOrderNo((String) obj[10]);
				bizThirdOut.setMember((String) obj[11]);
				bizThirdOut.setLevel((String) obj[12]);
				arrlist.add(bizThirdOut);
			}
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("arrlist", arrlist);
			map.put("page", new Paging(page));
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("调用第三方系统余额Controller查询失败：" + e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/updateOuterLimit")
	public String updateOuterLimit(@RequestParam(value = "outerLimit", required = false) Integer outerLimit,
			@RequestParam(value = "middleLimit", required = false) Integer middleLimit,
			@RequestParam(value = "innerLimit", required = false) Integer innerLimit,
			@RequestParam(value = "specifyLimit", required = false) Integer specifyLimit,
			@RequestParam(value = "handicapId", required = false) Integer handicapId,
			@RequestParam(value = "bankType", required = false) String bankType,
			@RequestParam(value = "flag", required = false) Integer flag) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (operator == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆!"));
			}
			if (Objects.isNull(handicapId) && Objects.equals(UserCategory.ADMIN.getValue(), operator.getCategory())) {
				return mapper.writeValueAsString(new GeneralResponseData(
						GeneralResponseData.ResponseStatus.FAIL.getValue(), "超级管理员不能更改所有盘口限额!"));
			}
			Set<Integer> dataRight = dataPermissionService.getHandicapByUserId(operator).stream()
					.mapToInt(p -> p.getId()).mapToObj(p -> p).collect(Collectors.toSet());
			List<Integer> handicapIdList = new ArrayList<>();
			if (Objects.equals(UserCategory.ADMIN.getValue(), operator.getCategory())) {
				handicapIdList.add(handicapId);
			} else {
				int zone = handicapService.findZoneByHandiId(operator.getHandicap());
				handicapService.findAllToList().stream()
						.filter(p -> Objects.equals(zone, p.getZone())
								&& (Objects.isNull(handicapId) || Objects.equals(p.getId(), handicapId))
								&& dataRight.contains(p.getId()))
						.forEach(p -> handicapIdList.add(p.getId()));
			}
			flag = 0;
			bankType = StringUtils.trimToNull(bankType);
			if (!CollectionUtils.isEmpty(handicapIdList)) {
				for (Integer handicap : handicapIdList) {
					Integer type = AccountType.OutBank.getTypeId();
					Integer status = AccountStatus.Normal.getStatus();
					accountService.updateOuterLimit(handicap, flag, outerLimit, middleLimit, innerLimit, specifyLimit,
							type, status, bankType);
				}
				accountService.broadCast();// 批量刷新缓存数据
			} else if (Objects.nonNull(handicapId)) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "你没有权限修改该盘口限额!"));
			} else {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "没有盘口数据可修改!"));
			}
			logger.info(operator.getUid() + "设置出款卡当日出款限额:outerLimit:" + outerLimit + "middleLimit" + middleLimit
					+ "innerLimit:" + innerLimit + "specifyLimit:" + specifyLimit);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("调用出款卡当日出款限额设置Controller失败：" + e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败" + e.getLocalizedMessage()));
		}
	}

	/**
	 * 用途为手机时候 检测当前主机IP是否在系统设置中 没有则无权限
	 *
	 * @throws Exception
	 */
	private void checkIp() throws Exception {
		boolean CheckIP = false;
		List<SysUserProfile> list = systemSettingService.findByPropertyKey("ACCOUNT_CREATE_FLAG_PHONE_IPS");
		String ipStr = null;
		if (null != list && list.size() > 0) {
			for (SysUserProfile sysProperty : list) {
				if (sysProperty.getPropertyKey().equals("ACCOUNT_CREATE_FLAG_PHONE_IPS")) {
					ipStr = sysProperty.getPropertyValue();
				}
			}
		}
		String NativeIP = new SimpleCookie("JIP").readValue(WebUtils.toHttp(request), null);
		logger.info("ipStr ipStr ipStr :{},remoteIp:{}", ipStr, NativeIP);
		if (StringUtils.isNotBlank(ipStr)) {
			String[] ipArray = ipStr.split(",");
			if (null != ipArray && ipArray.length > 0 && Arrays.asList(ipArray).contains(NativeIP)) {
				CheckIP = true;
			}
		}
		if (!CheckIP) {
			logger.error("未授权,ip:{}", NativeIP);
			throw new Exception("未授权");
		}
	}

	/**
	 * 根据账号ID获取激活任务
	 *
	 * @param accountId
	 *            账号ID
	 */
	@RequestMapping("/getActiveTrans")
	public String getTestTrans(@RequestParam(value = "accountId") int accountId) throws JsonProcessingException {
		GeneralResponseData<TransferEntity> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		try {
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser == null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆");
				return mapper.writeValueAsString(responseData);
			}
			// 2-返回转帐信息
			TransferEntity data = allocateTransService.activeAccByTest(accountId, true);
			if (data != null) {
				logger.info("转账测试>> id:{} toAcc:{} toOwner:{} toAmt:{}", data.getFromAccountId(), data.getAccount(),
						data.getOwner(), data.getAmount());
			}
			responseData.setData(data);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.info("获取测试转账任务失败 acc {} {}", accountId, e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "获取转账任务失败！");
			return mapper.writeValueAsString(responseData);
		}
	}

	@RequestMapping("/findDeleteAccount")
	public String findRebate(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "handicap", required = false) int handicap,
			@RequestParam(value = "alias", required = false) String alias,
			@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "flag", required = false) String flag,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE);
			alias = alias.equals("") ? null : alias;
			flag = flag.equals("") ? null : flag;
			status = status.equals("") ? null : status;
			Map<String, Object> map = accountService.findDeleteAccount(handicap, alias, type, flag, status,
					pageRequest);
			Page<Object> Page = (Page<Object>) map.get("Page");
			if (null != Page) {
				List<Object> List = Page.getContent();
				List<BizAccount> arrlist = new ArrayList<BizAccount>();
				for (int i = 0; i < List.size(); i++) {
					Object[] obj = (Object[]) List.get(i);
					BizHandicap bizHandicap = handicapService.findFromCacheById(null == obj[8] ? 0 : (Integer) obj[8]);
					BizAccount account = new BizAccount();
					account.setId((Integer) obj[0]);
					account.setHandicapName(null == bizHandicap ? "" : bizHandicap.getName());
					account.setAlias(obj[11] == null ? "" : obj[11].toString());
					account.setType((Integer) obj[4]);
					account.setAccount(obj[1] == null ? "" : obj[1].toString());
					account.setBankBalance((BigDecimal) obj[7]);
					account.setUpdateTimeStr(obj[14].toString());
					account.setStatus((Integer) obj[3]);
					account.setFlag((Integer) obj[31]);
					arrlist.add(account);
				}
				map.put("list", arrlist);
				map.put("Total", map.get("Total"));
				map.put("Page", new Paging(Page));
			}
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("账号删除Controller查询失败" + e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/deleteAccount")
	public String deleteAccount(@RequestParam(value = "accountId", required = false) Integer accountId,
			@RequestParam(value = "accountIds", required = false) Integer[] accountIds,
			@RequestParam(value = "type", required = false) String type) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "删除成功");
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			if ("fin".equals(type)) {
				List<Integer> inAccountIds = new ArrayList<>();
				if (accountIds != null && accountIds.length > 0) {
					for (int i = 0, len = accountIds.length; i < len; i++) {
						BizAccount account = accountService.getById(accountIds[i]);
						if (Objects.isNull(account)) {
							throw new Exception("ID为:" + accountIds[i] + "的账号不存在，请输入正确的参数");
						}
						if (account.getStatus() != -2) {
							throw new Exception("账号：" + addAccount(account.getAccount()) + " 编号：" + account.getAlias()
									+ "的账号不是待财务主管删除状态！");
						}
						if (account.getType() != null && account.getType().equals(AccountType.InBank.getTypeId())) {
							inAccountIds.add(accountIds[i]);
						} else {
							accountService.deleteAndClear(accountIds[i]);
							// 插入操作记录表
							BizAccountExtra accountExtra = new BizAccountExtra();
							accountExtra.setTime(new Date());
							accountExtra.setAccountId(accountIds[i]);
							accountExtra.setOperator(operator.getUid());
							accountExtra.setRemark("【财务主管删除账号】");
							accountExtraService.insertRow(accountExtra);
							logger.info(String.format("%s，AAccountId：%s，盘口：%s，编号：%s，操作人：%s", "财务主管删除账号", accountIds[i],
									account.getHandicapName(), account.getAlias(), operator.getUid()));
							logger.debug("deleteAccount>> id {}", account.getId());
							accountService.broadCast(account);
						}
					}

				} else {
					BizAccount account = accountService.getById(accountId);
					if (null == account) {
						throw new Exception("ID为:" + accountId + "的账号不存在，请输入正确的参数");
					}
					if (account.getStatus() != -2) {
						throw new Exception("账号：" + addAccount(account.getAccount()) + " 编号：" + account.getAlias()
								+ "的账号不是待财务主管删除状态！");
					}
					if (!account.getType().equals(AccountType.InBank.getTypeId())) {
						accountService.deleteAndClear(accountId);
						// 插入操作记录表
						BizAccountExtra accountExtra = new BizAccountExtra();
						accountExtra.setTime(new Date());
						accountExtra.setAccountId(accountId);
						accountExtra.setOperator(operator.getUid());
						accountExtra.setRemark("【财务主管删除账号】");
						accountExtraService.insertRow(accountExtra);
						logger.info(String.format("%s，AAccountId：%s，盘口：%s，编号：%s，操作人：%s", "财务主管删除账号", accountId,
								account.getHandicapName(), account.getAlias(), operator.getUid()));
						accountService.broadCast(account);
					} else {
						inAccountIds.add(accountId);
					}
				}
				if (!CollectionUtils.isEmpty(inAccountIds)) {
					for (int i = 0, size = inAccountIds.size(); i < size; i++) {
						BizAccount account = accountService.getById(inAccountIds.get(i));
						accountService.deleteAndClear(inAccountIds.get(i));
						// 插入操作记录表
						BizAccountExtra accountExtra = new BizAccountExtra();
						accountExtra.setTime(new Date());
						accountExtra.setAccountId(inAccountIds.get(i));
						accountExtra.setOperator(operator.getUid());
						accountExtra.setRemark("【财务主管删除账号】");
						accountExtraService.insertRow(accountExtra);
						logger.info(String.format("%s，AAccountId：%s，盘口：%s，编号：%s，操作人：%s", "财务主管删除账号",
								inAccountIds.get(i), account.getHandicapName(), account.getAlias(), operator.getUid()));
						accountService.broadCast(account);
					}
				}
			} else {
				if (accountIds != null && accountIds.length > 0) {
					for (int i = 0; i < accountIds.length; i++) {
						BizAccount account = accountService.getById(Integer.parseInt(accountIds[i].toString()));
						if (null == account) {
							throw new Exception("ID为:" + accountIds[i] + "的账号不存在，请输入正确的参数");
						}
						Date dtf1 = account.getUpdateTime();
						Date dtf2 = df.parse(df.format((DateUtils.addDays(new Date(), -30))));
						if (dtf1.getTime() > dtf2.getTime()) {
							throw new Exception("账号：" + addAccount(account.getAccount()) + " 编号：" + account.getAlias()
									+ "的账号更新时间在一个月之内，请刷新页面！");
						}
						if (account.getStatus() != 3 && account.getStatus() != 4) {
							throw new Exception("账号：" + addAccount(account.getAccount()) + " 编号：" + account.getAlias()
									+ "的账号不是冻结或者停用状态！");
						}
						// 插入操作记录表
						BizAccountExtra accountExtra = new BizAccountExtra();
						accountExtra.setTime(new Date());
						accountExtra.setAccountId(accountIds[i]);
						accountExtra.setOperator(operator.getUid());
						accountExtra.setRemark("【金流主管删除账号】");
						accountExtraService.insertRow(accountExtra);
						accountService.broadCast(account);
						logger.info(String.format("%s，AAccountId：%s，盘口：%s，编号：%s，操作人：%s", "金流主管删除账号", accountIds[i],
								account.getHandicapName(), account.getAlias(), operator.getUid()));
					}
					List<String> ids = Arrays.stream(accountIds).map(p -> String.valueOf(p))
							.collect(Collectors.toList());
					accountService.updateAcountById(ids, AccountStatus.Delete.getStatus());
				} else {
					BizAccount account = accountService.getById(accountId);
					if (null == account) {
						throw new Exception("ID为:" + accountId + "的账号不存在，请输入正确的参数");
					}
					Date dtf1 = account.getUpdateTime();
					Date dtf2 = df.parse(df.format((DateUtils.addDays(new Date(), -30))));
					if (dtf1.getTime() > dtf2.getTime()) {
						throw new Exception("账号：" + addAccount(account.getAccount()) + " 编号：" + account.getAlias()
								+ "的账号更新时间在一个月之内，请刷新页面！");
					}
					if (account.getStatus() != 3 && account.getStatus() != 4) {
						throw new Exception("账号：" + addAccount(account.getAccount()) + " 编号：" + account.getAlias()
								+ "的账号不是冻结或者停用状态！");
					}
					account.setStatus(AccountStatus.Delete.getStatus());
					accountService.updateBaseInfo(account);
					// 插入操作记录表
					BizAccountExtra accountExtra = new BizAccountExtra();
					accountExtra.setTime(new Date());
					accountExtra.setAccountId(accountId);
					accountExtra.setOperator(operator.getUid());
					accountExtra.setRemark("【金流主管删除账号】");
					accountExtraService.insertRow(accountExtra);
					accountService.broadCast(account);
					logger.info(String.format("%s，AAccountId：%s，盘口：%s，编号：%s，操作人：%s", "金流主管删除账号", accountId,
							account.getHandicapName(), account.getAlias(), operator.getUid()));
				}
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error("删除账号失败:", e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
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

	@RequestMapping("/getUserByMobile")
	public String getUserByMobile(@RequestParam(value = "mobile") String mobile) throws JsonProcessingException {
		GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		try {
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser == null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆");
				return mapper.writeValueAsString(responseData);
			}
			BizAccountMore more = accMoreSer.getFromCacheByMobile(mobile);
			String message = rebateApiService.getUserByUid(more == null ? mobile : more.getUid());
			if (null == message) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询失败");
				return mapper.writeValueAsString(responseData);
			}
			JsonNode rootNode = mapper.readTree(message);
			JsonNode content = rootNode.path("data");
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("contactor", content.get("contactor"));
			map.put("contactText", content.get("contactText"));
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.info("获取兼职人员联系方式失败 {}", mobile, e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"获取兼职人员联系方式失败！");
			return mapper.writeValueAsString(responseData);
		}
	}

	@RequestMapping("/setToZeroCredits")
	public String setToZeroCredits(@RequestParam(value = "accountId", required = true) Integer accountId)
			throws JsonProcessingException {
		GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		try {
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser == null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆");
				return mapper.writeValueAsString(responseData);
			}
			AccountBaseInfo base = accountService.getFromCacheById(accountId);
			if (Objects.isNull(base)) {
				logger.info("setToZeroCredit id>>> {} account doesn't exists ", accountId);
				return mapper.writeValueAsString(
						new ResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "success"));
			}
			BizAccountMore more = accMoreSer.getFromCacheByMobile(base.getMobile());
			if (Objects.isNull(more) || more.getMargin() != null && more.getMargin().intValue() >= 1000) {
				if (Objects.isNull(more)) {
					logger.info("setToZeroCredit acc id {} Mobile {} >>> BizAccountMore doesn't exists ", accountId,
							base.getMobile());
				} else {
					logger.info(
							"setToZeroCredit acc id {} Mobile {} >>> margin {} is more then 1000,can't set to zero credit",
							accountId, base.getMobile(), more.getMargin().intValue());
				}
			}
			accMoreSer.setToZeroCredit(sysUser, more.getMoible());
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.info("设置0信用额度失败 {}", accountId, e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "设置0信用额度失败！");
			return mapper.writeValueAsString(responseData);
		}
	}

	@RequestMapping("/checkBlack")
	public String checkBlack(@RequestParam(value = "accountId", required = true) Integer accountId)
			throws JsonProcessingException {
		GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		try {
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser == null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆");
				return mapper.writeValueAsString(responseData);
			}
			boolean checked = allocateTransService.checkBlack(accountId);
			if (checked)
				responseData.setStatus(GeneralResponseData.ResponseStatus.FAIL.getValue());
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.info("校验黑名单失败 {}", accountId, e);
			responseData.setMessage("查询黑名单失败");
			return mapper.writeValueAsString(responseData);
		}
	}

	@RequestMapping("/derating")
	public String derating(@RequestParam(value = "id", required = false) Integer id,
			@RequestParam(value = "amount", required = false) BigDecimal amount,
			@RequestParam(value = "remark", required = false) String remark)
			throws JsonProcessingException, IllegalAccessException, InvocationTargetException {
		GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		try {
			BizAccount base = accountService.getById(id);
			if (null != base && base.getFlag() == 2) {
				BizAccountMore more = accMoreSer.getFromCacheByMobile(base.getMobile());
				if (null != more && more.getMargin().floatValue() >= amount.floatValue()) {
					// 向返利网请求降额
					boolean flag = rebateApiService.derate(base.getAccount(), -amount.floatValue(),
							(more.getMargin().subtract(amount)).floatValue(), null, null);
					if (flag) {
						// 如果当前的卡 够扣除
						if ((Objects.isNull(base.getPeakBalance()) ? BigDecimal.ZERO
								: new BigDecimal(base.getPeakBalance()))
										.floatValue() >= Math.abs(amount.floatValue())) {
							base.setPeakBalance((int) (null == base.getPeakBalance() ? 0
									: base.getPeakBalance().floatValue() - amount.floatValue()));
							logger.debug("derating>> id {}", id);
							accountService.broadCast(base);
							hostMonitorService.update(base);
							cabanaService.updAcc(base.getId());
						} else {
							// 当前卡不够扣
							float reduceAmount = Math.abs(amount.floatValue())
									- (Objects.isNull(base.getPeakBalance()) ? BigDecimal.ZERO
											: new BigDecimal(base.getPeakBalance())).floatValue();
							base.setPeakBalance(0);
							logger.debug("derating>> id {}", id);
							accountService.broadCast(base);
							hostMonitorService.update(base);
							cabanaService.updAcc(base.getId());
							for (String accId : StringUtils.trimToEmpty(more.getAccounts()).split(",")) {
								if (StringUtils.isBlank(accId) || !StringUtils.isNumeric(accId)
										|| accId.equals(base.getId().toString()))
									continue;
								BizAccount account = accountService.getById(Integer.valueOf(accId));
								if (Objects.nonNull(base)) {
									if (reduceAmount <= 0)
										break;
									if ((Objects.isNull(account.getPeakBalance()) ? BigDecimal.ZERO
											: new BigDecimal(account.getPeakBalance())).floatValue() >= reduceAmount) {
										// 当前卡够扣
										Integer pb = (int) ((Objects.isNull(account.getPeakBalance()) ? BigDecimal.ZERO
												: new BigDecimal(account.getPeakBalance())).floatValue()
												- reduceAmount);
										account.setPeakBalance(pb);
										logger.debug("derating>> id {}", id);
										accountService.broadCast(account);
										hostMonitorService.update(account);
										cabanaService.updAcc(account.getId());
									} else {
										// 当前卡不够扣
										reduceAmount = reduceAmount
												- (Objects.isNull(account.getPeakBalance()) ? BigDecimal.ZERO
														: new BigDecimal(account.getPeakBalance())).floatValue();
										account.setPeakBalance(0);
										logger.debug("derating>> id {}", id);
										accountService.broadCast(account);
										hostMonitorService.update(account);
										cabanaService.updAcc(account.getId());
									}
								}
							}
						}
						String remarks = "降低信用额度:" + base.getId() + ";" + more.getMargin() + ">"
								+ (more.getMargin().subtract(amount.setScale(2, RoundingMode.HALF_UP))).setScale(2,
										RoundingMode.HALF_UP)
								+ ";备注：" + remark;
						more.setLinelimit(more.getLinelimit().subtract(amount));
						more.setMargin(more.getMargin().subtract(amount));
						more.setRemark(CommonUtils.genRemark(more.getRemark(), remarks, new Date(), sysUser.getUid()));
						accMoreSer.saveAndFlash(more);
					} else {
						responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
								"返利网降额失败！");
						return mapper.writeValueAsString(responseData);
					}
				} else {
					responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
							"降额的金额不能大于额度！");
					return mapper.writeValueAsString(responseData);
				}
			}
		} catch (Exception e) {
			logger.info("降低信用额度失败 {}", id, e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "降低信用额度失败！");
			return mapper.writeValueAsString(responseData);
		}
		return mapper.writeValueAsString(
				new GeneralResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功  "));
	}

	@RequestMapping("/deductamount")
	public String deductamount(@RequestParam(value = "uid", required = false) String uid,
			@RequestParam(value = "amount", required = false) BigDecimal amount,
			@RequestParam(value = "remark", required = false) String remark)
			throws JsonProcessingException, IllegalAccessException, InvocationTargetException {
		GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		try {
			// 查看是否存在数据，如果存在 就累加，如果不存在则添加
			BizDeductAmount deductAmount = rebateUserService.deductAmountByUid(uid);
			String remarks = "扣除佣金:" + amount + ";备注：" + remark;
			if (null != deductAmount) {
				deductAmount.setRemark(
						CommonUtils.genRemark(deductAmount.getRemark(), remarks, new Date(), sysUser.getUid()));
				deductAmount.setAmount(deductAmount.getAmount().add(amount));
				rebateUserService.saveDeductAmount(deductAmount);
			} else {
				BizDeductAmount deduct = new BizDeductAmount();
				deduct.setAmount(amount);
				deduct.setRemark(CommonUtils.genRemark("", remarks, new Date(), sysUser.getUid()));
				deduct.setUid(uid);
				rebateUserService.saveDeductAmount(deduct);
			}
			// 扣佣金的信息传给返利网
			BigDecimal balance = rebateApiService.deductAmount(uid, amount, "1", "", remark);
			BizDeductAmount deductMsg = rebateUserService.deductAmountByUid(uid);
			// 如返回（可提现余额-扣除金額）>=0则表示 可提现余额够扣 <0则不够扣 第二天返利的时候需要接着扣
			if (null != balance && balance.floatValue() >= 0) {
				logger.info("返回balance :" + balance);
				String re = "从可提现余额扣除佣金:" + amount;
				deductMsg.setRemark(CommonUtils.genRemark(deductMsg.getRemark(), re, new Date(), "系统"));
				deductMsg.setAmount(BigDecimal.ZERO);
				rebateUserService.saveDeductAmount(deductMsg);
				BizAccountMore more = accMoreSer.getFromByUid(uid);
				more.setBalance(more.getBalance().subtract(amount));
				accMoreSer.saveAndFlash(more);
			} else if (null != balance && balance.floatValue() < 0) {
				logger.info("返回balance :" + balance);
				// 如果可提现余额有被减少过
				if (amount.compareTo(balance.abs()) == 1) {
					String re = "从可提现余额扣除佣金:" + (amount.subtract(balance.abs()));
					deductMsg.setRemark(CommonUtils.genRemark(deductMsg.getRemark(), re, new Date(), "系统"));
					deductMsg.setAmount(deductMsg.getAmount().subtract((amount.subtract(balance.abs()))));
					rebateUserService.saveDeductAmount(deductMsg);
					BizAccountMore more = accMoreSer.getFromByUid(uid);
					more.setBalance(more.getBalance().subtract((amount.subtract(balance.abs()))));
					accMoreSer.saveAndFlash(more);
				}
			}
		} catch (Exception e) {
			logger.info("扣除佣金失败 {}" + e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "扣除佣金失败！");
			return mapper.writeValueAsString(responseData);
		}
		return mapper.writeValueAsString(
				new GeneralResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功  "));
	}

	@RequestMapping("/resetbankAmount")
	public String resetbankAmount(@RequestParam(value = "id", required = false) int id) throws JsonProcessingException {
		GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		try {
			BizAccount db = accountService.getById(id);
			if (null != db && db.getBankBalance().compareTo(new BigDecimal("0.1")) != 0) {
				// 清零的时候 冻结流程如果存在没有处理完的则不添加
				int count = finLessStatService.findCountsById(db.getId(), "portion");
				if (count > 0)
					return mapper.writeValueAsString(
							new GeneralResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
									"编号为：" + db.getAlias() + "的卡还在冻结流程中，请处理完毕后在清零！"));
				BizAccountMore mo = accMoreSer.findByMobile(db.getMobile());
				if (null != db) {
					mo.setLinelimit(mo.getLinelimit().add(db.getBankBalance()));
					accMoreSer.saveAndFlash(mo);
				}
				String remarks = "余额清零";
				db.setBankBalance(new BigDecimal("0.1"));
				db.setRemark(CommonUtils.genRemark(db.getRemark(), remarks, new Date(), sysUser.getUid()));
				logger.debug("resetbankAmount>> id {}", db.getId());
				accountService.broadCast(db);
				hostMonitorService.update(db);
				cabanaService.updAcc(db.getId());
			}
		} catch (Exception e) {
			logger.info("余额清零失败 {}" + e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "余额清零失败！");
			return mapper.writeValueAsString(responseData);
		}
		return mapper.writeValueAsString(
				new GeneralResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功  "));
	}

	/**
	 * 获取最新版本信息
	 */
	@RequestMapping("/getLastVersion")
	public String getLastVersion() throws JsonProcessingException {
		try {
			return mapper.writeValueAsString(cabanaService.getLastVersion());
		} catch (Exception e) {
			logger.error("Transfer(ackActiveQuickPay) error. {} ", e);
			return mapper.writeValueAsString(
					new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/recalculate")
	public String recalculate(@RequestParam(value = "uid", required = false) String uid)
			throws JsonProcessingException, IllegalAccessException, InvocationTargetException {
		GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		try {
			boolean flag = accountService.recalculate(uid);
			if (!flag) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"请确保兼职已经把工具拉起来了！");
				return mapper.writeValueAsString(responseData);
			}
		} catch (Exception e) {
			logger.info("计算额度失败： ", e);
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "计算额度失败  "));
		}
		return mapper.writeValueAsString(
				new GeneralResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功  "));
	}

	@RequestMapping("/showRebateStatistics")
	public String showRebateStatistics(@RequestParam(value = "pageNo") Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "startDate", required = false) String startDate,
			@RequestParam(value = "endDate", required = false) String endDate) throws JsonProcessingException {
		String params = buildParams().toString();
		logger.trace(String.format("%s，参数：%s", "账号分页获取", params));
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE);
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser == null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆");
				return mapper.writeValueAsString(responseData);
			}
			Map<String, Object> mapp = rebateStatisticsService.showRebateStatistics(startDate, endDate, pageRequest);
			Page<Object> page = (Page<Object>) mapp.get("rebatePage");
			List<Object> rabeteStatisticsList = (List<Object>) page.getContent();
			List<BizRebateStatistics> rebateList = new ArrayList<BizRebateStatistics>();
			for (int i = 0; i < rabeteStatisticsList.size(); i++) {
				Object[] obj = (Object[]) rabeteStatisticsList.get(i);
				BizRebateStatistics rebateStatistics = new BizRebateStatistics();
				rebateStatistics.setId(Long.valueOf(obj[0].toString()));
				rebateStatistics.setStatisticsDate(new SimpleDateFormat("yyyy-MM-dd").parse(obj[1].toString()));
				rebateStatistics.setNewAccTotal(Integer.valueOf(obj[2].toString()));
				rebateStatistics.setQuitAccTotal(Integer.valueOf(obj[3].toString()));
				rebateStatistics.setAccTotal(Integer.valueOf(obj[4].toString()));
				rebateStatistics.setNewAccNewCard(Integer.valueOf(obj[5].toString()));
				rebateStatistics.setNowAccNewCard(Integer.valueOf(obj[6].toString()));
				rebateStatistics.setQuitCardTotal(Integer.valueOf(obj[7].toString()));
				rebateStatistics.setCardTotal(Integer.valueOf(obj[8].toString()));
				rebateStatistics.setEnableCardTotal(Integer.valueOf(obj[9].toString()));
				rebateStatistics.setDisableCardTotal(Integer.valueOf(obj[10].toString()));
				rebateStatistics.setFreezeCardTotal(Integer.valueOf(obj[11].toString()));
				rebateStatistics.setNewAccUpgradeCredits(Integer.valueOf(obj[12].toString()));
				rebateStatistics.setNowAccUpgradeCredits(Integer.valueOf(obj[13].toString()));
				rebateStatistics.setCreditsTotal(Integer.valueOf(obj[15].toString()));
				rebateList.add(rebateStatistics);
			}
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("rebateList", rebateList);
			map.put("page", new Paging(page));
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(String.format("%s，参数：%s，结果：%s", "账号分页获取", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/showRebateUserNew")
	public String showRebateUserNew(@RequestParam(value = "pageNo") Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "statisticsDate", required = false) String statisticsDate,
			@RequestParam(value = "statisticsType", required = false) String statisticsType,
			@RequestParam(value = "rebateUser", required = false) String rebateUser,
			@RequestParam(value = "bankType", required = false) String bankType,
			@RequestParam(value = "account", required = false) String account,
			@RequestParam(value = "owner", required = false) String owner,
			@RequestParam(value = "startDate", required = false) String startDate,
			@RequestParam(value = "queryType", required = false) String queryType,
			@RequestParam(value = "statusToArray", required = false) Integer[] statusToArray,
			@RequestParam(value = "alias", required = false) String alias) throws JsonProcessingException {
		String params = buildParams().toString();
		logger.trace(String.format("%s，参数：%s", "账号分页获取", params));
		try {
			GeneralResponseData<List<BizAccount>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE);
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser == null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆");
				return mapper.writeValueAsString(responseData);
			}
			bankType = bankType.equals("") ? null : bankType;
			alias = alias.equals("") ? null : alias;
			account = account.equals("") ? null : account;
			owner = owner.equals("") ? null : owner;
			rebateUser = rebateUser.equals("") ? null : rebateUser;
			Map<String, Object> mapp = rebateStatisticsService.showRebateUserByType(startDate, queryType, rebateUser,
					bankType, account, owner, statusToArray, alias, pageRequest);
			Page<BizAccount> page = (Page<BizAccount>) mapp.get("page");
			Paging paging = new Paging(page);
			responseData.setData(page.getContent());
			responseData.setPage(paging);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(String.format("%s，参数：%s，结果：%s", "账号分页获取", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}
}