package com.xinbo.fundstransfer.restful;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.shiro.SecurityUtils;
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
import com.xinbo.fundstransfer.domain.DynamicSpecifications;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.entity.BizRebateContact;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.service.RebateContactService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/r/rebateContact")
public class RebateContactController extends BaseController {
	@Autowired
	private RebateContactService rebateContactService;

	@RequestMapping(value = "/list")
	public String list(@RequestParam(value = "status", required = false) Integer status,
			@RequestParam(value = "pageNo") Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "id", required = false) Long id,
			@RequestParam(value = "uid", required = false) String uid) throws JsonProcessingException {
		try {
			GeneralResponseData<List<BizRebateContact>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			SysUser user = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (Objects.isNull(user)) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			PageRequest pageRequest = PageRequest.of(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					Sort.Direction.DESC, "createTime");
			List<SearchFilter> filterToList = new ArrayList<>();
			filterToList.add(new SearchFilter("status", SearchFilter.Operator.NOTEQ, "2"));
			if (Objects.nonNull(id)) {
				filterToList.add(new SearchFilter("id", SearchFilter.Operator.EQ, id));
			}
			if (Objects.nonNull(uid)) {
				filterToList.add(new SearchFilter("uid", SearchFilter.Operator.EQ, uid));
			}
			Specification<BizRebateContact> specif = DynamicSpecifications.build(BizRebateContact.class,
					filterToList.toArray(new SearchFilter[filterToList.size()]));
			Page<BizRebateContact> page = rebateContactService.findPage(specif, pageRequest);
			responseData.setData(page.getContent());
			responseData.setPage(new Paging(page));
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping(value = "/delete")
	public String delete(@RequestParam(value = "id", required = false) Long id) throws JsonProcessingException {
		try {
			SysUser user = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (Objects.isNull(user)) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			rebateContactService.delete(user, id);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "删除成功"));
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping(value = "/add")
	public String add(@RequestParam(value = "contactType") String contactType,
			@RequestParam(value = "isCommon", required = false) String isCommon,
			@RequestParam(value = "contactNo", required = false) String contactNo,
			@RequestParam(value = "uid", required = false) String uid) throws JsonProcessingException {
		try {
			SysUser user = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (Objects.isNull(user)) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			BizRebateContact contact = new BizRebateContact();
			contact.setOperator(user.getUid());
			contact.setContactNo(contactNo);
			contact.setStatus(1);
			contact.setContactType(contactType);
			contact.setIsCommon(isCommon);
			contact.setUid(uid);
			rebateContactService.save(user, contact);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "新增成功"));
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}
}
