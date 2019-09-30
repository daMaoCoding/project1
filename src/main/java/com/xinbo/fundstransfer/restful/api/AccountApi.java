package com.xinbo.fundstransfer.restful.api;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.domain.DynamicSpecifications;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.BizIncomeRequest;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.enums.IncomeRequestType;
import com.xinbo.fundstransfer.domain.enums.UserCategory;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.restful.BaseController;
import com.xinbo.fundstransfer.service.AccountExtraService;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.HandicapService;
import com.xinbo.fundstransfer.service.IncomeRequestService;



/**
 * 提供给平台的接口
 * @author Administrator
 *
 */
@RestController("AccountApi")
@RequestMapping("/api/account")
public class AccountApi extends BaseController {
	private static Logger logger = LoggerFactory.getLogger(AccountApi.class);
	@Autowired
	private HandicapService handicapService;
	@Autowired
	private AccountService accountService;
	@Autowired
	private IncomeRequestService incomeRequestService;
	
	/**
	 * 公司入款汇总 - 正在匹配 - 查询账号（/r/account/findIncomeAccountOrderByBankLog）
	 * @param pageNo
	 * @param statusToArray
	 * @param search_IN_flag
	 * @param oidList
	 * @param account
	 * @param bankType
	 * @param owner
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/findIncomeAccountOrderByBankLog")
	public String findIncomeAccountOrderByBankLog(@RequestParam(value = "pageNo") Integer pageNo,
			@RequestParam(value = "statusToArray") Integer[] statusToArray,
			@RequestParam(value = "search_IN_flag", required = false) Integer[] search_IN_flag,
			@RequestParam(value = "oidList") List<String> oidList,
			@RequestParam(value = "account", required = false) String account,
			@RequestParam(value = "bankType", required = false) String bankType,
			@RequestParam(value = "owner", required = false) String owner) throws JsonProcessingException {
		String params = buildParams().toString();
		logger.trace(String.format("%s，参数：%s", "平台接口调用，账号分页获取", params));
		try {
			GeneralResponseData<List<Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, 10);
			List<Integer> handicapId=new ArrayList<Integer>();
			oidList.stream().forEach((p) -> handicapId.add(handicapService.findFromCacheByCode(p).getId()));
			Page<Object> page = accountService.findIncomeAccountOrderByBankLog((Integer[])handicapId.toArray(new Integer[handicapId.size()]), account, null, bankType, owner,
					search_IN_flag, statusToArray, pageRequest);
			responseData.setData(page.getContent());
			responseData.setPage(new Paging(page));
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(String.format("%s，参数：%s，结果：%s", "平台接口调用，账号分页获取", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}
	
	/**
	 * 公司入款汇总 - 正在匹配 - 查询账号（/r/income/findbyvo）
	 * @param pageNo
	 * @param pageSize
	 * @param bizIncomeRequest
	 * @param oidList
	 * @param minAmount
	 * @param maxAmount
	 * @param startAndEndTimeToArray
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/findIncomeMatchingOrder")
	public String findIncomeMatchingOrder(@RequestParam(value = "pageNo") Integer pageNo, 
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@Valid BizIncomeRequest bizIncomeRequest,
			@RequestParam(value = "oidList") List<String> oidList,
			@RequestParam(value = "minAmount", required = false) BigDecimal minAmount,
			@RequestParam(value = "maxAmount", required = false) BigDecimal maxAmount,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray)
			throws JsonProcessingException {
		try {
			GeneralResponseData<List<BizIncomeRequest>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			List<SearchFilter> filterToList = DynamicSpecifications.build(request);
			// 盘口
			List<Integer> handicapId=new ArrayList<Integer>();
			oidList.stream().forEach((p) -> handicapId.add(handicapService.findFromCacheByCode(p).getId()));
			filterToList.add(new SearchFilter("handicap", SearchFilter.Operator.IN, handicapId.toArray()));
			// 只查入款单
			filterToList.add(new SearchFilter("type", SearchFilter.Operator.EQ, IncomeRequestType.PlatFromBank.getType()));
			// 金额区间值用
			if (null != minAmount) {
				filterToList.add(new SearchFilter("amount", SearchFilter.Operator.GTE, minAmount));
			}
			if (null != maxAmount) {
				filterToList.add(new SearchFilter("amount", SearchFilter.Operator.LTE, maxAmount));
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
			//header封装
			Map<String, Object> header = new HashMap<>();
			BigDecimal[] amountAndFee = incomeRequestService.findAmountAndFeeByTotal(filterToArray);
			header.put("totalAmount", amountAndFee[0]);
			header.put("totalFee", amountAndFee[1]);
			responseData.setData(page.getContent());
			responseData.setPage(new Paging(page, header));
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error("平台接口调用，方法：{}，操作失败：异常{}", request.getMethod(), e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "平台接口调用，操作失败  " + e.getLocalizedMessage()));
		}
	}
	
}
