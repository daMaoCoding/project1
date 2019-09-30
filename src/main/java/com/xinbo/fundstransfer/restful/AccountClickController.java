package com.xinbo.fundstransfer.restful;

import java.util.*;

import javax.servlet.http.HttpServletRequest;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.domain.*;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.service.*;

/**
 * 帐号点击操作记录表(下发卡用)
 */
@RestController
@RequestMapping("/r/accountClick")
public class AccountClickController extends BaseController {
	private static final Logger logger = LoggerFactory.getLogger(AccountClickController.class);
	@Autowired
	private AccountClickService accountClickService;
	@Autowired
	public HttpServletRequest request;

	@RequestMapping("/list")
	public String findAll(@RequestParam(value = "pageNo") Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@Valid BizAccountClick accountExtra) throws JsonProcessingException {
		String params = buildParams().toString();
		logger.trace(String.format("%s，参数：%s", "账号点击记录分页获取", params));
		try {
			GeneralResponseData<List<BizAccountClick>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser == null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆");
				return mapper.writeValueAsString(responseData);
			}
			List<SearchFilter> filterToList = DynamicSpecifications.build(request);
			if (accountExtra.getAccountId() != null) {
				filterToList.add(new SearchFilter("accountId", SearchFilter.Operator.EQ, accountExtra.getAccountId()));
			}
			if (StringUtils.isNotEmpty(accountExtra.getOperator())) {
				filterToList.add(new SearchFilter("operator", SearchFilter.Operator.EQ, accountExtra.getOperator()));
			}
			Date[] startAndEndTime = CommonUtils.parseStartAndEndTime(startAndEndTimeToArray);
			if (startAndEndTime[0] != null) {
				filterToList.add(new SearchFilter("time", SearchFilter.Operator.GTE, startAndEndTime[0]));
			}
			if (startAndEndTime[1] != null) {
				filterToList.add(new SearchFilter("time", SearchFilter.Operator.LTE, startAndEndTime[1]));
			}
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					 Sort.Direction.DESC, "time");
			SearchFilter[] filterToArray = filterToList.toArray(new SearchFilter[filterToList.size()]);
			Specification<BizAccountClick> specif = DynamicSpecifications.build(BizAccountClick.class, filterToArray);
			Page<BizAccountClick> page=accountClickService.findAll(specif,pageRequest);
			responseData.setData(page.getContent());
			responseData.setPage(new Paging(page));
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(String.format("%s，参数：%s，结果：%s", "账号操作记录分页获取", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询失败  " + e.getLocalizedMessage()));
		}
		
	}
	
	
	
			
}
