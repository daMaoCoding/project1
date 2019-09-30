package com.xinbo.fundstransfer.restful;

import java.util.Date;
import java.util.List;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.StringUtil;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
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
import com.xinbo.fundstransfer.domain.DynamicSpecifications;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizAccountExtra;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.service.AccountExtraService;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.SysUserService;



@RestController
@RequestMapping("/r/accountExtra")
public class AccountExtraController extends BaseController {
	private static final Logger logger = LoggerFactory.getLogger(AccountExtraController.class);
	@Autowired
	private SysUserService userService;
	@Autowired
	private AccountService accountService;
	@Autowired
	private AccountExtraService accountExtraService;

	@RequiresPermissions("OtherPermission:OperatorLog" )
	@RequestMapping("/findAll")
	public String findAll(@RequestParam(value = "pageNo") Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@Valid BizAccountExtra accountExtra) throws JsonProcessingException {
		String params = buildParams().toString();
		logger.trace(String.format("%s，参数：%s", "账号操作记录分页获取", params));
		try {
			GeneralResponseData<List<BizAccountExtra>> responseData = new GeneralResponseData<>(
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
			Specification<BizAccountExtra> specif = DynamicSpecifications.build(BizAccountExtra.class, filterToArray);
			Page<BizAccountExtra> page=accountExtraService.findAll(specif,pageRequest);
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
