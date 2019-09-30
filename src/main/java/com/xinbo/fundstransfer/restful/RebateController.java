package com.xinbo.fundstransfer.restful;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.util.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.domain.DynamicSpecifications;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.BankLogStatus;
import com.xinbo.fundstransfer.domain.enums.OutwardTaskStatus;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.AccountReturnSummary;
import com.xinbo.fundstransfer.domain.pojo.PageOutwardTaskCheck;
import com.xinbo.fundstransfer.service.*;
import com.xinbo.fundstransfer.service.impl.CommissionHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/r/rebate")
public class RebateController extends BaseController {
	@Autowired
	private AccountRebateService accountRebateService;
	@Autowired
	private AccountService accountService;
	@Autowired
	private RebateApiService rebateApiService;
	@Autowired
	private HandicapService handicapService;
	@Autowired
	private BankLogService bankLogService;
	@Autowired
	private SysDataPermissionService sysDataPermissionService;
	@Autowired
	private AccountMoreService accountMoreSer;
	@Autowired
	private HostMonitorService hostMonitorService;
	@Autowired
	private CabanaService cabanaService;
	@Autowired
	private RebateUserService rebateUserService;
	@Autowired
	private CommissionHandler commissionHandler;
	@Autowired
	private CommonRemarkService commonRemarkService;

	/**
	 * 匹配操作
	 */
	@RequestMapping("/match")
	public String match(@RequestParam(value = "bankFlowId") Long bankFlowId, @RequestParam(value = "rebateId") Long id,
			@RequestParam(value = "remark") String remark) throws JsonProcessingException {
		try {
			SysUser loginUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (Objects.isNull(loginUser)) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			rebateApiService.match(id, bankFlowId, remark, loginUser);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "匹配成功"));
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "匹配失败" + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/reject")
	public String reject(@RequestParam(value = "id") Long id, @RequestParam(value = "remark") String remark)
			throws JsonProcessingException {
		try {
			SysUser user = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (Objects.isNull(user)) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			rebateApiService.fail(id, remark, user);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功"));
		} catch (Exception e) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "获取失败"));
		}
	}

	@RequestMapping("/findById")
	public String findById(@RequestParam(value = "id") Long id) throws JsonProcessingException {
		try {
			BizAccountRebate rebate = accountRebateService.findById(id);
			GeneralResponseData<BizAccountRebate> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "获取成功");
			responseData.setData(rebate);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "获取失败"));
		}
	}

	@RequestMapping("/lock")
	public String lock(@RequestParam(value = "caclTime") String caclTime) throws JsonProcessingException {
		try {
			GeneralResponseData<BizAccountRebate> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "锁定成功");
			SysUser user = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (Objects.isNull(user)) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			String message = accountRebateService.lock(caclTime, user);
			responseData.setMessage(message);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			log.error("锁定佣金失败", e);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "锁定失败"));
		}
	}

	@RequestMapping("/saveAudit")
	public String saveAudit(@RequestParam(value = "caclTime") String caclTime,
			@RequestParam(value = "status") int status, @RequestParam(value = "remark") String remark,
			@RequestParam(value = "rebateUser") String rebateUser) throws JsonProcessingException {
		try {
			GeneralResponseData<BizAccountRebate> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "审核佣金成功");
			SysUser user = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (Objects.isNull(user)) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			accountRebateService.saveAudit(caclTime, status, remark, user, rebateUser);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			log.error("审核佣金失败", e);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "审核佣金失败"));
		}
	}

	@RequestMapping("/recalculate")
	public String recalculate(@RequestParam(value = "caclTime") String caclTime) throws JsonProcessingException {
		try {
			GeneralResponseData<BizAccountRebate> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "重新计算成功");
			SysUser user = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (Objects.isNull(user)) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			accountRebateService.recalculate(caclTime, "重新计算返利数据", user);
			commissionHandler.commissionSummarymanually(caclTime);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			log.error("重新计算失败", e);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新计算失败"));
		}
	}

	@RequestMapping("/unlock")
	public String unlock(@RequestParam(value = "caclTime") String caclTime) throws JsonProcessingException {
		try {
			GeneralResponseData<BizAccountRebate> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "锁定成功");
			SysUser user = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (Objects.isNull(user)) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			accountRebateService.unlock(caclTime, user);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			log.error("锁定佣金失败", e);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "锁定失败"));
		}
	}

	@RequestMapping(value = "/list")
	public String list(@RequestParam(value = "status", required = false) Integer status,
			@RequestParam(value = "pageNo") Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "id", required = false) Long id,
			@RequestParam(value = "fromAccountId", required = false) Integer fromAccountId,
			@RequestParam(value = "handicapId", required = false) Integer handicapId,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "minAmount", required = false) BigDecimal minAmount,
			@RequestParam(value = "maxAmount", required = false) BigDecimal maxAmount,
			@RequestParam(value = "toAccount", required = false) String toAccount) throws JsonProcessingException {
		try {
			GeneralResponseData<List<PageOutwardTaskCheck.OutwardTaskCheckContent>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			SysUser user = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (Objects.isNull(user)) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					Sort.Direction.DESC, "asignTime");
			List<SearchFilter> filterToList = new ArrayList<>();
			if (Objects.nonNull(status)) {
				filterToList.add(new SearchFilter("status", SearchFilter.Operator.EQ, status));
			}
			if (Objects.nonNull(id)) {
				filterToList.add(new SearchFilter("id", SearchFilter.Operator.EQ, id));
			}
			if (Objects.nonNull(fromAccountId)) {
				filterToList.add(new SearchFilter("accountId", SearchFilter.Operator.EQ, fromAccountId));
			}
			if (Objects.nonNull(handicapId)) {
				filterToList.add(new SearchFilter("handicap", SearchFilter.Operator.EQ, handicapId));
			}
			Date[] startAndEndTime = CommonUtils.parseStartAndEndTime(startAndEndTimeToArray);
			if (Objects.nonNull(startAndEndTime) && startAndEndTime.length == 2) {
				filterToList.add(new SearchFilter("asignTime", SearchFilter.Operator.GTE, startAndEndTime[0]));
				filterToList.add(new SearchFilter("asignTime", SearchFilter.Operator.LTE, startAndEndTime[1]));
			}
			if (Objects.nonNull(minAmount)) {
				filterToList.add(new SearchFilter("amount", SearchFilter.Operator.GTE, minAmount));
			}
			if (Objects.nonNull(maxAmount)) {
				filterToList.add(new SearchFilter("amount", SearchFilter.Operator.LTE, maxAmount));
			}
			if (StringUtils.isNotBlank(toAccount)) {
				filterToList.add(new SearchFilter("toAccount", SearchFilter.Operator.LIKE, toAccount.trim()));
			}
			Specification<BizAccountRebate> specif = DynamicSpecifications.build(BizAccountRebate.class,
					filterToList.toArray(new SearchFilter[filterToList.size()]));
			Page<BizAccountRebate> page = accountRebateService.findPage(specif, pageRequest);

			PageOutwardTaskCheck result = new PageOutwardTaskCheck(new Paging(page));
			PageOutwardTaskCheck.OutwardTaskCheckContent item;
			if (Objects.nonNull(page) && !CollectionUtils.isEmpty(page.getContent())) {
				for (BizAccountRebate rebate : page.getContent()) {
					item = result.new OutwardTaskCheckContent(rebate.getId(), null, rebate.getAccountId(),
							rebate.getAmount(), rebate.getAsignTime(), rebate.getHandicap(), null, "返利网",
							rebate.getToAccountType(), rebate.getToAccount(), rebate.getToHolder(), "系统",
							rebate.getRemark(), rebate.getStatus(), rebate.getScreenshot(), rebate.getTid());
					AccountBaseInfo base = accountService.getFromCacheById(rebate.getAccountId());
					if (!Objects.isNull(base)) {
						item.setFromAlias(base.getAlias());
						item.setFromBankType(base.getBankType());
						item.setFromOwner(base.getOwner());
					}
					result.push(item);
				}
				SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				result.getContent().forEach((p) -> {
					if (p.getHandicapId() != null) {
						BizHandicap handicap = handicapService.findFromCacheById(p.getHandicapId());
						p.setHandicapName(handicap == null ? StringUtils.EMPTY : handicap.getName());
					}
					if (p.getAsignTime() != null) {
						p.setAsignTimeStr(SDF.format(p.getAsignTime()));
					}
					if (p.getFromAccountId() != null) {
						AccountBaseInfo base = accountService.getFromCacheById(p.getFromAccountId());
						if (Objects.nonNull(base)) {
							p.setFromAccount(base.getAccount());
						}
					}
				});
			}
			Map<String, Object> header = new HashMap<>();
			header.put("totalAmount", 0);
			header.put("totalFee", 0);
			Paging paging = result.getPage();
			paging.setHeader(header);
			responseData.setData(result.getContent());
			responseData.setPage(paging);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/findRebate")
	public String findRebate(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "status", required = false) int status,
			@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "orderNo", required = false) String orderNo,
			@RequestParam(value = "rebateType", required = false) String rebateType,
			@RequestParam(value = "fromAmount", required = false) String fromAmount,
			@RequestParam(value = "toMoney", required = false) String toMoney,
			@RequestParam(value = "startAndEndTime", required = false) String[] startAndEndTime,
			@RequestParam(value = "handicap", required = false) int handicap,
			@RequestParam(value = "uName", required = false) String uName,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					type.equals("rebate") ? Sort.Direction.DESC : Sort.Direction.ASC,
					type.equals("rebate") ? "differenceMinutes"
							: (type.equals("rebated") || type.equals("check")) ? "create_time" : "update_time");
			String fristTime = null;
			String lastTime = null;
			if (0 != startAndEndTime.length && null != startAndEndTime) {
				fristTime = startAndEndTime[0];
				lastTime = startAndEndTime[1];
			}
			// 获取当前用户盘口 进行权限限制
			SysUser loginUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			List<Integer> handicapList = getHandicapIdByCurrentUser(loginUser.getId() == 1 ? 0 : handicap, loginUser);
			Map<String, Object> map = accountRebateService.findRebate(status, type, orderNo, fromAmount, toMoney,
					fristTime, lastTime, handicap, handicapList, uName, rebateType, pageRequest);
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
			// 返利数据
			Page<Object> rebatePage = (Page<Object>) map.get("rebatePage");
			if (null != rebatePage) {
				List<Object> rebateList = rebatePage.getContent();
				List<BizAccountRebate> arrlist = new ArrayList<BizAccountRebate>();
				for (int i = 0; i < rebateList.size(); i++) {
					Object[] obj = (Object[]) rebateList.get(i);
					BizHandicap bizHandicap = handicapService.findFromCacheById((Integer) obj[14]);
					AccountBaseInfo bizAccount = new AccountBaseInfo();
					if (obj[3] != null)
						bizAccount = accountService.getFromCacheById((Integer) obj[3]);
					BizAccountRebate bizAccountRebate = new BizAccountRebate();
					bizAccountRebate.setId(Long.valueOf(obj[0].toString()));
					bizAccountRebate.setUid((String) obj[1]);
					bizAccountRebate.setTid((String) obj[2]);
					bizAccountRebate.setOutAccount(bizAccount == null ? "" : bizAccount.getAccount());
					bizAccountRebate.setAccountAlias(bizAccount == null ? "" : bizAccount.getAlias());
					bizAccountRebate.setAccountId(obj[3] == null ? 0 : (Integer) obj[3]);
					bizAccountRebate.setOutPerson(bizAccount == null ? ""
							: (bizAccount.getFlag() == null ? "PC"
									: (bizAccount.getFlag() == 1 ? "手机" : (bizAccount.getFlag() == 2 ? "返利网" : "PC"))));
					if (obj[4].toString().length() >= 8) {
						bizAccountRebate.setToAccount(obj[4].toString().substring(0, 4) + "**"
								+ obj[4].toString().substring(obj[4].toString().length() - 4));
					} else if (obj[4].toString().length() >= 4) {
						bizAccountRebate.setToAccount(obj[4].toString().substring(0, 2) + "**"
								+ obj[4].toString().substring(obj[4].toString().length() - 2));
					} else if (obj[4].toString().length() >= 2) {
						bizAccountRebate.setToAccount(obj[4].toString().substring(0, 1) + "**"
								+ obj[4].toString().substring(obj[4].toString().length() - 1));
					}
					bizAccountRebate.setToHolder(obj[5].toString().substring(0, 1) + "**");
					bizAccountRebate.setToAccountType((String) obj[6]);
					bizAccountRebate.setToAccountInfo((String) obj[7]);
					bizAccountRebate.setAmount(new BigDecimal(obj[8].toString()));
					bizAccountRebate.setBalance(new BigDecimal(obj[9].toString()));
					bizAccountRebate.setStatus(Integer.parseInt(obj[10].toString()));
					bizAccountRebate.setCreateTimeStr(obj[11].toString());
					bizAccountRebate.setUpdateTimeStr(obj[12] == null ? "" : obj[12].toString());
					bizAccountRebate.setRemark(StringUtils.isNotBlank((String) obj[13])
							? ((String) obj[13]).replace("\r\n", "<br>").replace("\n", "<br>") : "");
					bizAccountRebate.setHandicapName(bizHandicap.getName());
					bizAccountRebate.setAsignTimeStr(obj[15] == null ? "" : obj[15].toString());
					bizAccountRebate.setScreenshot(obj[17] == null ? "" : obj[17].toString());
					bizAccountRebate.setDifferenceMinutes(obj[18] == null ? 0 : Integer.parseInt(obj[18].toString()));
					bizAccountRebate.setType(obj[19] == null ? 1 : Integer.parseInt(obj[19].toString()));
					bizAccountRebate.setuName(obj[20] == null ? "" : (String) obj[20]);
					arrlist.add(bizAccountRebate);
				}
				map.put("rebatelist", arrlist);
				map.put("rebateTotal", map.get("rebateTotal"));
				map.put("rebatePage", new Paging(rebatePage));
			}
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("返利任务Controller查询失败" + e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/findAuditCommission")
	public String findAuditCommission(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "startAndEndTime", required = false) String[] startAndEndTime,
			@RequestParam(value = "results", required = false) String results,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			// 获取当前用户
			SysUser user = (SysUser) SecurityUtils.getSubject().getPrincipal();
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE);
			String fristTime = null;
			String lastTime = null;
			if (0 != startAndEndTime.length && null != startAndEndTime) {
				fristTime = startAndEndTime[0];
				lastTime = startAndEndTime[1];
			}
			if ("".equals(results)) {
				results = null;
			}
			Map<String, Object> map = accountRebateService.findAuditCommission(fristTime, lastTime, results,
					pageRequest);
			// 返利数据
			Page<Object> rebatePage = (Page<Object>) map.get("rebatePage");
			if (null != rebatePage) {
				List<Object> rebateList = rebatePage.getContent();
				List<AccountReturnSummary> arrlist = new ArrayList<AccountReturnSummary>();
				Map trans = redisService.getStringRedisTemplate().opsForHash().entries(RedisKeys.REBATE_AMOUTS_KEYS);
				for (int i = 0; i < rebateList.size(); i++) {
					Object[] obj = (Object[]) rebateList.get(i);
					AccountReturnSummary returnSummary = new AccountReturnSummary();
					// 判断当天的数据是否锁定
					Object o = trans.get(obj[0].toString());
					if (Objects.nonNull(o)) {
						returnSummary.setIsLock(1);
						// 判断这条数据是否当前登录人锁定
						String[] transInfo = o.toString().split(":");
						if (transInfo[1].equals(user.getUid())) {
							returnSummary.setIsLockMyself(1);
						} else {
							returnSummary.setIsLockMyself(0);
						}
					} else {
						returnSummary.setIsLock(0);
					}

					returnSummary.setCalcTime(obj[0].toString());
					returnSummary.setBankAmounts(new BigDecimal(obj[1].toString()));
					returnSummary.setRebateAmount(new BigDecimal(obj[2].toString()));
					returnSummary.setCounts(Integer.parseInt(obj[3].toString()));
					returnSummary.setStatus(null == obj[4] ? 0 : (int) obj[4]);
					returnSummary.setRemark(StringUtils.isNotBlank((String) obj[5])
							? ((String) obj[5]).replace("\r\n", "<br>").replace("\n", "<br>") : "");
					arrlist.add(returnSummary);
				}
				map.put("rebatelist", arrlist);
				map.put("rebateTotal", map.get("rebateTotal"));
				map.put("rebatePage", new Paging(rebatePage));
			}
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			log.error("佣金审核统计Controller查询失败", e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/findDetail")
	public String findDetail(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "rebateUser", required = false) String rebateUser,
			@RequestParam(value = "bankType", required = false) String bankType,
			@RequestParam(value = "caclTime", required = false) String caclTime,
			@RequestParam(value = "startamount", required = false) BigDecimal startamount,
			@RequestParam(value = "endamount", required = false) BigDecimal endamount,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			// 获取当前用户
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE);
			if ("".equals(rebateUser)) {
				rebateUser = null;
			}
			if ("".equals(bankType)) {
				bankType = null;
			}
			if (null == startamount) {
				startamount = BigDecimal.ZERO;
			}
			if (null == endamount) {
				endamount = BigDecimal.ZERO;
			}
			Map<String, Object> map = accountRebateService.findDetail(rebateUser, bankType, caclTime, startamount,
					endamount, pageRequest);
			// 返利数据
			Page<Object> rebatePage = (Page<Object>) map.get("rebatePage");
			if (null != rebatePage) {
				List<Object> rebateList = rebatePage.getContent();
				List<AccountReturnSummary> arrlist = new ArrayList<AccountReturnSummary>();
				for (int i = 0; i < rebateList.size(); i++) {
					Object[] obj = (Object[]) rebateList.get(i);
					AccountReturnSummary returnSummary = new AccountReturnSummary();
					AccountBaseInfo base = accountService.getFromCacheById(Integer.parseInt(obj[0].toString()));
					if (null == base || null == base.getMobile())
						continue;
					BizAccountMore more = accountMoreSer.getFromCacheByMobile(base.getMobile());
					returnSummary.setAccount(base.getAccount());
					returnSummary.setBankType(base.getBankType());
					returnSummary.setOwner(base.getOwner());
					List<Integer> tmp = new ArrayList<Integer>();
					if (null != more && null != more.getAccounts()) {
						String[] account = more.getAccounts().split(",");
						for (String str : account) {
							if (str != null && str.length() != 0) {
								tmp.add(Integer.parseInt(str));
							}
						}
					}
					returnSummary.setAccountId(Integer.parseInt(obj[0].toString()));
					returnSummary.setBankAmounts(new BigDecimal(obj[1].toString()));
					returnSummary.setRebateAmount(new BigDecimal(obj[2].toString()));
					returnSummary.setRebateUser(null == obj[3] ? "" : obj[3].toString());
					returnSummary.setTotalCards(tmp.size());
					arrlist.add(returnSummary);
				}
				map.put("rebatelist", arrlist);
				map.put("rebateTotal", map.get("rebateTotal"));
				map.put("rebatePage", new Paging(rebatePage));
			}
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			log.error("佣金审核统计明细Controller查询失败", e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/findComplete")
	public String findComplete(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "startAndEndTime", required = false) String[] startAndEndTime,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE);
			String fristTime = null;
			String lastTime = null;
			if (0 != startAndEndTime.length && null != startAndEndTime) {
				fristTime = startAndEndTime[0];
				lastTime = startAndEndTime[1];
			}
			Map<String, Object> map = accountRebateService.findComplete(fristTime, lastTime, pageRequest);
			// 返利数据
			Page<Object> rebatePage = (Page<Object>) map.get("rebatePage");
			if (null != rebatePage) {
				List<Object> rebateList = rebatePage.getContent();
				List<AccountReturnSummary> arrlist = new ArrayList<AccountReturnSummary>();
				for (int i = 0; i < rebateList.size(); i++) {
					Object[] obj = (Object[]) rebateList.get(i);
					AccountReturnSummary returnSummary = new AccountReturnSummary();
					returnSummary.setCalcTime(obj[0].toString());
					returnSummary.setCounts(Integer.parseInt(obj[1].toString()));
					returnSummary.setBankAmounts(new BigDecimal(obj[2].toString()));
					returnSummary.setRebateAmount(new BigDecimal(obj[3].toString()).compareTo(BigDecimal.ZERO) == 1
							? new BigDecimal(obj[3].toString()) : new BigDecimal("0"));
					returnSummary.setRemark(StringUtils.isNotBlank((String) obj[4])
							? ((String) obj[4]).replace("\r\n", "<br>").replace("\n", "<br>") : "");
					arrlist.add(returnSummary);
				}
				map.put("rebatelist", arrlist);
				map.put("rebateTotal", map.get("rebateTotal"));
				map.put("rebatePage", new Paging(rebatePage));
			}
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			log.error("佣金出款完成Controller查询失败", e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/findCompleteDetail")
	public String findCompleteDetail(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "rebateUser", required = false) String rebateUser,
			@RequestParam(value = "caclTime", required = false) String caclTime,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE);
			if ("".equals(rebateUser)) {
				rebateUser = null;
			}
			Map<String, Object> map = accountRebateService.findCompleteDetail(rebateUser, caclTime, pageRequest);
			// 返利数据
			Page<Object> rebatePage = (Page<Object>) map.get("rebatePage");
			if (null != rebatePage) {
				List<Object> rebateList = rebatePage.getContent();
				List<AccountReturnSummary> arrlist = new ArrayList<AccountReturnSummary>();
				for (int i = 0; i < rebateList.size(); i++) {
					Object[] obj = (Object[]) rebateList.get(i);
					AccountReturnSummary returnSummary = new AccountReturnSummary();
					returnSummary.setRebateUser(null == obj[0] ? "" : obj[0].toString());
					returnSummary.setBankAmounts(new BigDecimal(obj[1].toString()));
					returnSummary.setRebateAmount(new BigDecimal(obj[2].toString()));
					returnSummary.setRebateBalanc(new BigDecimal(obj[3].toString()).compareTo(BigDecimal.ZERO) == 1
							? new BigDecimal(obj[3].toString()) : new BigDecimal("0"));
					arrlist.add(returnSummary);
				}
				map.put("rebatelist", arrlist);
				map.put("rebateTotal", map.get("rebateTotal"));
				map.put("rebatePage", new Paging(rebatePage));
			}
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			log.error("佣金出款完成Controller查询失败", e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/saveRemarkAndUpdataStatus")
	public String saveRemarkAndUpdataStatus(@RequestParam(value = "id", required = false) Long id,
			@RequestParam(value = "remark") String remark,
			@RequestParam(value = "status", required = false) Integer status) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (Objects.isNull(operator)) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			if (StringUtils.isBlank(remark)) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "备注不能为空。"));
			}
			if (Objects.isNull(status)) {
				rebateApiService.remark(id, remark, operator);
			} else if (status == OutwardTaskStatus.Failure.getStatus()) {
				rebateApiService.fail(id, remark, operator);
			} else if (status == OutwardTaskStatus.Deposited.getStatus()) {
				rebateApiService.finish(id, remark, operator);
			} else if (status == OutwardTaskStatus.ManageCancel.getStatus()) {
				rebateApiService.cancel(id, remark, operator, false);
			} else if (status == OutwardTaskStatus.Undeposit.getStatus()) {
				accountRebateService.reAssignDrawing(operator, id, "重新生成任务：" + remark);
			} else if (status == 8) {
				accountRebateService.reAssignDrawing(operator, id, "分配：" + remark);
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("返利任务Controller添加备注失败：" + e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	/** 获取当前用户拥有的盘口id */
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

	@RequestMapping("/derate")
	public String alterFlowToRefunding(@RequestParam(value = "flowId") long flowId,
			@RequestParam(value = "remark") String remark) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		try {
			if (operator == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			log.info("derate ( flowId：{}  remark：{} ) >> operator:{} 返利网降额流水", flowId, remark, operator.getUid());
			BizBankLog log = bankLogService.findBankFlowById(flowId);
			if (null == log)
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "流水信息不正确"));

			BizAccount acc = accountService.getById(log.getFromAccount());
			if (null == acc)
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "找不到对应的账号信息"));

			BizAccountMore mo = accountMoreSer.findByMobile(acc.getMobile());
			float margin = mo.getMargin().floatValue() - Math.abs(log.getAmount().floatValue());
			if (null == mo)
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "找不到兼职信息"));
			// 向返利网降额请求
			boolean flag = rebateApiService.derate(acc.getAccount(), log.getAmount().floatValue(), margin, log.getId(),
					null);
			if (flag) {
				mo.setMargin(BigDecimal.valueOf(margin));
				accountMoreSer.saveAndFlash(mo);
				bankLogService.updateStatusRm(log.getId(), BankLogStatus.Disposed.getStatus(),
						operator.getUid() + " 降低信用额度：" + remark);
				// 如果一张卡 不够扣 则需要扣除多张卡
				Integer peakBalance = (int) ((Objects.isNull(acc.getPeakBalance()) ? BigDecimal.ZERO
						: new BigDecimal(acc.getPeakBalance())).floatValue() - Math.abs(log.getAmount().floatValue()));
				// 如果当前的卡 够扣除
				if ((Objects.isNull(acc.getPeakBalance()) ? BigDecimal.ZERO : new BigDecimal(acc.getPeakBalance()))
						.floatValue() >= Math.abs(log.getAmount().floatValue())) {
					acc.setPeakBalance(peakBalance);
					accountService.broadCast(acc);
					hostMonitorService.update(acc);
					cabanaService.updAcc(acc.getId());
				} else {
					// 当前卡不够扣
					float reduceAmount = Math.abs(log.getAmount().floatValue())
							- (Objects.isNull(acc.getPeakBalance()) ? BigDecimal.ZERO : acc.getPeakBalance())
									.floatValue();
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
								Integer pb = (int) ((Objects.isNull(account.getPeakBalance()) ? BigDecimal.ZERO
										: account.getPeakBalance()).floatValue() - reduceAmount);
								account.setPeakBalance(pb);
								accountService.broadCast(account);
								hostMonitorService.update(account);
								cabanaService.updAcc(account.getId());
							} else {
								// 当前卡不够扣
								reduceAmount = reduceAmount - (Objects.isNull(account.getPeakBalance())
										? BigDecimal.ZERO : account.getPeakBalance()).floatValue();
								account.setPeakBalance(0);
								accountService.broadCast(account);
								hostMonitorService.update(account);
								cabanaService.updAcc(account.getId());
							}
						}
					}
				}
			}
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			log.error("修改流水为返利网降额流水失败 operator:{} flowId:{} remark:{} ,exception:{}",
					Objects.isNull(operator) ? StringUtils.EMPTY : operator.getUid(), flowId, remark, e.getMessage());
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getMessage()));
		}
	}

	@RequestMapping(value = "/listDevelopManage")
	public String listManage(@RequestParam(value = "rebateUsername", required = false) String rebateUsername,
			@RequestParam(value = "marginMin", required = false) BigDecimal marginMin,
			@RequestParam(value = "marginMax", required = false) BigDecimal marginMax,
			@RequestParam(value = "currMarginMin", required = false) BigDecimal currMarginMin,
			@RequestParam(value = "currMarginMax", required = false) BigDecimal currMarginMax,
			@RequestParam(value = "totalRebateMin", required = false) BigDecimal totalRebateMin,
			@RequestParam(value = "totalRebateMax", required = false) BigDecimal totalRebateMax,
			@RequestParam(value = "developType") String developType,
			@RequestParam(value = "sortProperty", required = false) String sortProperty,
			@RequestParam(value = "sortDirection", required = false) Integer sortDirection,
			@RequestParam(value = "pageNo") Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			GeneralResponseData<List<BizAccountMore>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			SysUser user = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (Objects.isNull(user)) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			PageRequest pageRequest = PageRequest.of(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE);
			Map<String, Object> result = accountMoreSer.findPage(rebateUsername, marginMin, marginMax, currMarginMin,
					currMarginMax, totalRebateMin, totalRebateMax, developType, sortProperty, sortDirection,
					pageRequest);
			Long currTime = System.currentTimeMillis();
			Page<BizAccountMore> page = (Page<BizAccountMore>) result.get("page");
			if (!CollectionUtils.isEmpty(page.getContent())) {
				for (BizAccountMore more : page.getContent()) {
					more.setRemark(commonRemarkService.latestRemark(Integer.parseInt(more.getUid()), "rebateUser"));
					BizRebateUser rebate = rebateUserService.getFromCacheByUid(more.getUid());
					if (rebate != null) {
						more.setUsername(rebate.getUserName());
						if (rebate.getCreateTime() != null) {
							Long diffTime = (currTime - rebate.getCreateTime().getTime()) / (24 * 60 * 60 * 1000);
							more.setJoinDays(diffTime.intValue());
						}
					}
					String accounts = more.getAccounts();
					if (StringUtils.isNotBlank(accounts)) {
						String[] strs = accounts.split(",");
						int used = 0;
						int stop = 0;
						for (String str : strs) {
							if (StringUtils.isNumeric(str)) {
								AccountBaseInfo base = accountService.getFromCacheById(Integer.parseInt(str));
								if (base != null) {
									if (Objects.equals(base.getStatus(), AccountStatus.Normal.getStatus())
											|| Objects.equals(base.getStatus(), AccountStatus.Enabled.getStatus())) {
										used++;
									} else if (Objects.equals(base.getStatus(), AccountStatus.StopTemp.getStatus())) {
										stop++;
									}
								}
							}
						}
						more.setCardUsedOrStop(used + " / " + stop);
					} else {
						more.setCardUsedOrStop(0 + " / " + 0);
					}
				}
			}
			Map<String, Object> total = (Map<String, Object>) result.get("total");
			Paging paging = new Paging(page, total);
			responseData.setData(page.getContent());
			responseData.setPage(paging);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/findDerating")
	public String findDerating(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "orderNo", required = false) String orderNo,
			@RequestParam(value = "uname", required = false) String uname,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "fromAmount", required = false) String fromAmount,
			@RequestParam(value = "toMoney", required = false) String toMoney,
			@RequestParam(value = "handicap", required = false) int handicap,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					Sort.Direction.ASC, "create_time");
			// 获取当前用户盘口 进行权限限制
			SysUser loginUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			List<Integer> handicapList = getHandicapIdByCurrentUser(loginUser.getId() == 1 ? 0 : handicap, loginUser);
			Map<String, Object> map = accountRebateService.findDerating(orderNo, fromAmount, toMoney, handicap,
					handicapList, uname, status, pageRequest);
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
			// 返利数据
			Page<Object> rebatePage = (Page<Object>) map.get("rebatePage");
			if (null != rebatePage) {
				List<Object> rebateList = rebatePage.getContent();
				List<BizAccountRebate> arrlist = new ArrayList<BizAccountRebate>();
				for (int i = 0; i < rebateList.size(); i++) {
					Object[] obj = (Object[]) rebateList.get(i);
					BizHandicap bizHandicap = handicapService.findFromCacheById((Integer) obj[14]);
					AccountBaseInfo bizAccount = new AccountBaseInfo();
					if (obj[3] != null)
						bizAccount = accountService.getFromCacheById((Integer) obj[3]);
					BizAccountRebate bizAccountRebate = new BizAccountRebate();
					bizAccountRebate.setId(Long.valueOf(obj[0].toString()));
					bizAccountRebate.setUid((String) obj[1]);
					bizAccountRebate.setTid((String) obj[2]);
					bizAccountRebate.setOutAccount(bizAccount == null ? "" : bizAccount.getAccount());
					bizAccountRebate.setAccountAlias(bizAccount == null ? "" : bizAccount.getAlias());
					bizAccountRebate.setAccountId(obj[3] == null ? 0 : (Integer) obj[3]);
					bizAccountRebate.setOutPerson(bizAccount == null ? ""
							: (bizAccount.getFlag() == null ? "PC"
									: (bizAccount.getFlag() == 1 ? "手机" : (bizAccount.getFlag() == 2 ? "返利网" : "PC"))));
					if (obj[4].toString().length() >= 8) {
						bizAccountRebate.setToAccount(obj[4].toString().substring(0, 4) + "**"
								+ obj[4].toString().substring(obj[4].toString().length() - 4));
					} else if (obj[4].toString().length() >= 4) {
						bizAccountRebate.setToAccount(obj[4].toString().substring(0, 2) + "**"
								+ obj[4].toString().substring(obj[4].toString().length() - 2));
					} else if (obj[4].toString().length() >= 2) {
						bizAccountRebate.setToAccount(obj[4].toString().substring(0, 1) + "**"
								+ obj[4].toString().substring(obj[4].toString().length() - 1));
					}
					bizAccountRebate.setToHolder(obj[5].toString().substring(0, 1) + "**");
					bizAccountRebate.setToAccountType((String) obj[6]);
					bizAccountRebate.setToAccountInfo((String) obj[7]);
					bizAccountRebate.setAmount(new BigDecimal(obj[8].toString()));
					bizAccountRebate.setBalance(new BigDecimal(obj[9].toString()));
					bizAccountRebate.setStatus(Integer.parseInt(obj[10].toString()));
					bizAccountRebate.setCreateTimeStr(obj[11].toString());
					bizAccountRebate.setUpdateTimeStr(obj[12] == null ? "" : obj[12].toString());
					bizAccountRebate.setRemark(StringUtils.isNotBlank((String) obj[13])
							? ((String) obj[13]).replace("\r\n", "<br>").replace("\n", "<br>") : "");
					bizAccountRebate.setHandicapName(bizHandicap.getName());
					bizAccountRebate.setAsignTimeStr(obj[15] == null ? "" : obj[15].toString());
					bizAccountRebate.setScreenshot(obj[17] == null ? "" : obj[17].toString());
					bizAccountRebate.setDifferenceMinutes(obj[18] == null ? 0 : Integer.parseInt(obj[18].toString()));
					bizAccountRebate.setType(obj[19] == null ? 1 : Integer.parseInt(obj[19].toString()));
					bizAccountRebate.setuName(obj[20] == null ? "" : (String) obj[20]);
					arrlist.add(bizAccountRebate);
				}
				map.put("rebatelist", arrlist);
				map.put("rebateTotal", map.get("rebateTotal"));
				map.put("rebatePage", new Paging(rebatePage));
			}
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("降额任务Controller查询失败" + e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/saveDeratingAudit")
	public String saveDeratingAudit(@RequestParam(value = "id") Long id, @RequestParam(value = "status") int status,
			@RequestParam(value = "remark") String remark) throws JsonProcessingException {
		try {
			SysUser user = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (Objects.isNull(user)) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			String message = accountRebateService.saveDeratingAudit(id, status, remark, user);
			GeneralResponseData<BizAccountRebate> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue(), message);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			log.error("审核降额失败", e);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "审核降额失败"));
		}
	}

}
