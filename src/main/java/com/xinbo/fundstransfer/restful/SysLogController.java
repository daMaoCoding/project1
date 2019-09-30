package com.xinbo.fundstransfer.restful;

import java.math.BigDecimal;
import java.util.*;

import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.net.http.accounting.HttpClientAccounting;
import com.xinbo.fundstransfer.domain.*;
import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.entity.BizSysInvst;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.repository.BankLogRepository;
import com.xinbo.fundstransfer.report.SystemAccountManager;
import com.xinbo.fundstransfer.service.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.domain.entity.BizSysLog;
import com.xinbo.fundstransfer.domain.entity.SysUser;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/r/syslog")
public class SysLogController extends BaseController {
	private static final Logger logger = LoggerFactory.getLogger(SysLogController.class);
	@Autowired
	private SysLogService sysLogService;
	@Autowired
	private AccountService accountService;
	@Autowired
	private SysUserService useSer;
	@Autowired
	private BankLogRepository bankLogDao;
	@Autowired
	private SystemAccountManager systemAccountManager;
	@Value("${funds.transfer.accounting.salt:c2ee0d77ad9a312c677ab001d2b88fdd}")
	private String ACCOUNTING_SALT;

	@RequestMapping("/list")
	public String list(@RequestParam(value = "isProblem", required = false) Integer isProblem,
			@RequestParam(value = "pageNo") Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		String params = buildParams().toString();
		logger.trace(String.format("%s，参数：%s", "分页获取", params));
		try {
			GeneralResponseData<Object> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser == null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆");
				return mapper.writeValueAsString(responseData);
			}
			String search_EQ_accountId = StringUtils.trimToEmpty(request.getParameter("search_EQ_accountId"));
			if (!StringUtils.isNumeric(search_EQ_accountId)) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作有误");
				return mapper.writeValueAsString(responseData);
			}
			String search_GTE_createTime = StringUtils.trimToEmpty(request.getParameter("search_GTE_createTime"));
			String search_LTE_createTime = StringUtils.trimToEmpty(request.getParameter("search_LTE_createTime"));
			if (StringUtils.isBlank(search_GTE_createTime) || StringUtils.isBlank(search_LTE_createTime)) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "时间不能为空");
				return mapper.writeValueAsString(responseData);
			}
			Map<String, Object> param = param(request);
			param.put("token", CommonUtils.md5digest(search_EQ_accountId + search_GTE_createTime + search_LTE_createTime
					+ StringUtils.trimToEmpty(ACCOUNTING_SALT)));
			Object[] rets = new Object[1];
			HttpClientAccounting.getInstance().getAccountingService().status(param).subscribe(d -> {
				rets[0] = d;
			}, e -> log.error("SysList 参数：%s >> process error. ", params, e));
			if (Objects.nonNull(rets[0])) {
				return mapper.writeValueAsString(rets[0]);
			}
			List<SearchFilter> filterToList = DynamicSpecifications.build(request);
			// 问题排查的数据 操作人有值（type如果是结息(-1) 费用(-4) 系统自动排查的operator也会为空）
			if (null != isProblem && isProblem.equals(1)) {
				filterToList.add(new SearchFilter("operator", SearchFilter.Operator.ISNOTNULL, null));
			}
			SearchFilter[] filterToArray = filterToList.toArray(new SearchFilter[filterToList.size()]);
			Specification<BizSysLog> specif = DynamicSpecifications.build(BizSysLog.class, filterToArray);
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					Sort.Direction.DESC, "status", "successTime", "id");
			Page<BizSysLog> page = sysLogService.findPage(specif, pageRequest);
			List<BizSysLog> dataList = new ArrayList<>();
			if (StringUtils.isNotBlank(search_EQ_accountId)) {
				Integer accountId = Integer.valueOf(search_EQ_accountId);
				AccountBaseInfo base = accountService.getFromCacheById(accountId);
				if (Objects.nonNull(base)) {
					List<BizSysLog> logs = systemAccountManager.suspend(base);
					if (!CollectionUtils.isEmpty(logs))
						dataList.addAll(logs);
				}
			}
			if (!CollectionUtils.isEmpty(page.getContent())) {
				List<BizSysLog> cloned = new ArrayList<>();
				for (BizSysLog sys : page.getContent()) {
					if (Objects.isNull(sys))
						continue;
					BizSysLog em = sys.clone();
					if (Objects.isNull(em))
						continue;
					if (Objects.nonNull(em.getBalance()) && Objects.nonNull(em.getBankBalance())
							&& em.getBalance().compareTo(em.getBankBalance()) != 0) {
						BigDecimal diff = em.getBankBalance().subtract(em.getBalance());
						String remark = StringUtils.trimToNull(em.getRemark());
						if (Objects.isNull(remark))
							em.setRemark(diff.toString());
						else
							em.setRemark(diff + "-" + remark);
					}
					cloned.add(em);
				}
				dataList.addAll(cloned);
			}
			responseData.setData(dataList);
			// 转入和转出金额
			List<SearchFilter> filterToList2 = new ArrayList<SearchFilter>();// 转入
			List<SearchFilter> filterToList3 = new ArrayList<SearchFilter>();// 转出
			if (filterToList.size() > 0) {
				for (SearchFilter temp : filterToList) {
					if (null != temp) {
						filterToList2.add(temp);
						filterToList3.add(temp);
					}
				}
			}
			filterToList2.add(new SearchFilter("amount", SearchFilter.Operator.GT, 0));
			filterToList3.add(new SearchFilter("amount", SearchFilter.Operator.LT, 0));
			SearchFilter[] filterToArray2 = filterToList2.toArray(new SearchFilter[filterToList2.size()]);
			SearchFilter[] filterToArray3 = filterToList3.toArray(new SearchFilter[filterToList3.size()]);
			BigDecimal[] amounts = sysLogService.findTotal(filterToArray);
			BigDecimal[] AmountPlus = sysLogService.findAmountTotal(filterToArray2);
			BigDecimal[] AmountNagetive = sysLogService.findAmountTotal(filterToArray3);
			Map<String, Object> total = new HashMap<>();
			total.put("feeTotal", amounts[0]);
			total.put("balanceTotal", amounts[1]);
			total.put("amountPlus", AmountPlus[0]);
			total.put("amountNagetive", AmountNagetive[0]);
			responseData.setPage(new Paging(page, total));
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error(String.format("%s，参数：%s，结果：%s", "分页获取", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/list4Invst")
	public String list4Invst(@RequestParam(value = "pageNo") Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		String params = buildParams().toString();
		logger.trace(String.format("%s，参数：%s", "分页获取", params));
		try {
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			List<SearchFilter> filterToList = DynamicSpecifications.build(request);
			SearchFilter[] filterToArray = filterToList.toArray(new SearchFilter[filterToList.size()]);
			Specification<BizSysInvst> specif = DynamicSpecifications.build(BizSysInvst.class, filterToArray);
			int pz = pageSize != null ? pageSize : AppConstants.PAGE_SIZE;
			PageRequest pageRequest = new PageRequest(pageNo, pz, Sort.Direction.DESC, "id");
			Page<BizSysInvst> page = sysLogService.findPage4Invst(specif, pageRequest);
			GeneralResponseData<List<BizSysInvst>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			responseData.setData(page.getContent());
			if (!CollectionUtils.isEmpty(page.getContent())) {
				for (BizSysInvst invst : page.getContent()) {
					SysUser confirmer = useSer.findFromCacheById(invst.getConfirmer());
					if (Objects.nonNull(confirmer)) {
						invst.setConfirmerName(confirmer.getUid());
					} else {
						invst.setConfirmerName("系统");
					}
					if (Objects.nonNull(invst.getConsumeMillis()) && invst.getConsumeMillis() != 0) {
						invst.setConsumeStr(CommonUtils.convertTime2String(invst.getConsumeMillis()));
					} else {
						invst.setConsumeStr(StringUtils.EMPTY);
					}
				}
			}
			// 转入和转出金额
			List<SearchFilter> filterToList2 = new ArrayList<>();// 转入
			List<SearchFilter> filterToList3 = new ArrayList<>();// 转出
			for (SearchFilter temp : filterToList) {
				if (null != temp) {
					filterToList3.add(temp);
					filterToList2.add(temp);
				}
			}
			filterToList2.add(new SearchFilter("amount", SearchFilter.Operator.GT, 0));
			filterToList3.add(new SearchFilter("amount", SearchFilter.Operator.LT, 0));
			SearchFilter[] filterToArray2 = filterToList2.toArray(new SearchFilter[filterToList2.size()]);
			SearchFilter[] filterToArray3 = filterToList3.toArray(new SearchFilter[filterToList3.size()]);
			BigDecimal[] AmountPlus = sysLogService.findAmountTotal4Invst(filterToArray2);
			BigDecimal[] AmountNagetive = sysLogService.findAmountTotal4Invst(filterToArray3);
			Map<String, Object> total = new HashMap<>();
			total.put("amountPlus", AmountPlus[0]);
			total.put("amountNagetive", AmountNagetive[0]);
			responseData.setPage(new Paging(page, total));
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error(String.format("%s，参数：%s，结果：%s", "分页获取", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/list4BankLogs")
	public String list4BankLogs(@RequestParam(value = "accId") Integer accId,
			@RequestParam(value = "logIds") Long[] logIds) throws JsonProcessingException {
		try {
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			List<SearchFilter> filterList = new ArrayList<>();
			filterList.add(new SearchFilter("fromAccount", SearchFilter.Operator.EQ, accId));
			filterList.add(new SearchFilter("id", SearchFilter.Operator.IN, logIds));
			SearchFilter[] filterToArray = filterList.toArray(new SearchFilter[filterList.size()]);
			Specification<BizBankLog> specification = DynamicSpecifications.build(BizBankLog.class, filterToArray);
			List<BizBankLog> list = bankLogDao.findAll(specification);
			GeneralResponseData<List<BizBankLog>> res = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			res.setData(list);
			return mapper.writeValueAsString(res);
		} catch (Exception e) {
			e.printStackTrace();
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	private Map<String, Object> param(HttpServletRequest request) {
		Map<String, Object> ret = new HashMap<>();
		Enumeration<String> em = request.getParameterNames();
		while (em.hasMoreElements()) {
			String k = em.nextElement();
			ret.put(k, request.getParameter(k));
		}
		return ret;
	}
}
