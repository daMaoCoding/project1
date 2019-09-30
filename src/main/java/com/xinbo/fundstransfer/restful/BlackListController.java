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
import com.xinbo.fundstransfer.domain.*;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.service.*;

/**
 * 账号管理
 */
@RestController
@RequestMapping("/r/blacklist")
public class BlackListController extends BaseController {
	private static final Logger logger = LoggerFactory.getLogger(BlackListController.class);
	@Autowired
	private BlackListService blackListService;
	@Autowired
	public HttpServletRequest request;

	@RequestMapping("/list")
	public String list(@RequestParam(value = "pageNo") Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		String params = buildParams().toString();
		logger.trace(String.format("%s，参数：%s", "分页获取", params));
		try {
			GeneralResponseData<List<BizBlackList>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser == null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆");
				return mapper.writeValueAsString(responseData);
			}
			List<SearchFilter> filterToList = DynamicSpecifications.build(request);
			SearchFilter[] filterToArray = filterToList.toArray(new SearchFilter[filterToList.size()]);
			Specification<BizBlackList> specif = DynamicSpecifications.build(BizBlackList.class, filterToArray);
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					Sort.Direction.DESC, "createTime");
			Page<BizBlackList> page = blackListService.findPage(specif, pageRequest);
			responseData.setData(page.getContent());
			responseData.setPage(new Paging(page));
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error("查询失败：{}", e);
			return mapper
					.writeValueAsString(new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue()));
		}
	}
	
	
	@RequestMapping("/create")
	public String create(@Valid BizBlackList vo)
			throws JsonProcessingException {
		String params = buildParams().toString();
		try {
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (operator == null) {
				throw new Exception("新增黑名单,请重新登陆!");
			}
			logger.info(String.format("%s，操作人员：%s，参数：%s", "addBlackList_start", operator.getUid(), params));
			if (StringUtils.isBlank(vo.getAccount()) ) {
				log.error("创建失败, 账号为空.");
				throw new Exception("创建失败, 账号为空.");
			}
			//账号不可以重复
			List<BizBlackList>list=blackListService.findByAccount(vo.getAccount());
			if(null!=list&&list.size()>0) {
				log.error("创建失败, 黑名单已存在.");
				throw new Exception("创建失败, 黑名单已存在.");
			}
			Date date = new Date();
			vo.setCreateTime(date);
			vo.setOperator(operator.getUid());
			BizBlackList data = blackListService.saveAndFlush( vo);
			GeneralResponseData<BizBlackList> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			responseData.setData(data);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error(String.format("%s，参数：%s，结果：%s", "addBlackList_failed", params, e.getMessage()));
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getMessage()));
		}
	}
	
	@RequestMapping("/delete")
	public String delete(@RequestParam(value = "id", required = false) Integer id)
			throws JsonProcessingException {
		String params = buildParams().toString();
		try {
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (operator == null) {
				throw new Exception("删除黑名单失败,请重新登陆!");
			}
			logger.info(String.format("%s，操作人员：%s，参数：%s", "deleteBlackList_start", operator.getUid(), params));
			blackListService.delete(id);
			GeneralResponseData<BizBlackList> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error(String.format("%s，参数：%s，结果：%s", "deleteBlackList_failed", params, e.getMessage()));
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getMessage()));
		}
	}
			
}
