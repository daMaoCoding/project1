package com.xinbo.fundstransfer.restful;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

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
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.domain.DynamicSpecifications;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.entity.BizNotice;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.service.NoticeService;

@RestController
@RequestMapping("/r/notice")
public class NoticeController extends BaseController {
	private static final Logger logger = LoggerFactory.getLogger(NoticeController.class);
	@Autowired
	public HttpServletRequest request;
	@Autowired
	NoticeService noticeService;
	private ObjectMapper mapper = new ObjectMapper();

	/**
	 * 根据查询条件筛选
	 * 
	 * @param seachStr
	 *            完整匹配：编号/IP 模糊匹配：主机名/账号
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/list")
	public String list(@RequestParam(value = "pageNo") Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize)
			throws JsonProcessingException {
		try {
			GeneralResponseData<List<BizNotice>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					Sort.Direction.DESC, "publishTime");
			List<SearchFilter> filterToList = DynamicSpecifications.build(request);
			SearchFilter[] filterToArray = filterToList.toArray(new SearchFilter[filterToList.size()]);
			Specification<BizNotice> specif = DynamicSpecifications.build(BizNotice.class, filterToArray);
			Page<BizNotice> page = noticeService.findPage(specif, pageRequest);
			responseData.setData(page.getContent());
			responseData.setPage(new Paging(page));
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/findbyid")
	public String findById(@RequestParam(value = "id") Integer id) throws JsonProcessingException {
		try {
			GeneralResponseData<BizNotice> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			BizNotice bizNotice = noticeService.findById(id);
			responseData.setData(bizNotice);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询失败  " + e.getLocalizedMessage()));
		}
	}


	@RequestMapping("/create")
	public String create(@Valid BizNotice notice) throws JsonProcessingException {
		String params = buildParams().toString();
		String errorInfo = "添加公告失败：";
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		logger.info(String.format("%s，操作人员：%s，参数：%s", "新增公告", operator.getUid(), params));
		try {
			if(ObjectUtils.isEmpty(notice.getType())) {
				log.error(errorInfo+"类型不能为空");
				throw new Exception(errorInfo+"类型不能为空");
			}
			if(ObjectUtils.isEmpty(notice.getPublishNo())) {
				log.error(errorInfo+"版本号不能为空");
				throw new Exception(errorInfo+"版本号不能为空");
			}
			if(ObjectUtils.isEmpty(notice.getPublishTime())) {
				log.error(errorInfo+"上线时间不能为空");
				throw new Exception(errorInfo+"上线时间不能为空");
			}
			if(ObjectUtils.isEmpty(notice.getTitle())) {
				log.error(errorInfo+"标题不能为空");
				throw new Exception(errorInfo+"标题不能为空");
			}
			if(ObjectUtils.isEmpty(notice.getContant())) {
				log.error(errorInfo+"内容不能为空");
				throw new Exception(errorInfo+"内容不能为空");
			}
			notice.setStatus(1);
			notice.setUpdateTime(new Date());
			notice.setOperator(operator.getUid());
			noticeService.saveAndFlush(notice);
			GeneralResponseData<BizNotice> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error(String.format("%s，参数：%s，结果：%s", errorInfo, params, e.getMessage()));
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getMessage()));
		}

	}

	@RequestMapping("/update")
	public String update(@Valid BizNotice vo) throws JsonProcessingException {
		String params = buildParams().toString();
		String errorInfo = "修改公告失败：";
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		logger.info(String.format("%s，操作人员：%s，参数：%s", "修改公告", operator.getUid(), params));
		try {
			if(ObjectUtils.isEmpty(vo.getId())) {
				log.error(errorInfo+"id不能为空");
				throw new Exception(errorInfo+"id不能为空");
			}
			BizNotice notice=noticeService.findById(vo.getId());
			if(null==notice) {
				log.error(errorInfo+"数据不存在");
				throw new Exception(errorInfo+"数据不存在");
			}
			if(!ObjectUtils.isEmpty(vo.getTitle())) {notice.setTitle(vo.getTitle());}
			if(!ObjectUtils.isEmpty(vo.getContant())) {notice.setContant(vo.getContant());}
			if(!ObjectUtils.isEmpty(vo.getType())) {notice.setType(vo.getType());}
			if(!ObjectUtils.isEmpty(vo.getPublishNo())) {notice.setPublishNo(vo.getPublishNo());}
			if(!ObjectUtils.isEmpty(vo.getPublishTime())) {notice.setPublishTime(vo.getPublishTime());}
			notice.setUpdateTime(new Date());
			notice.setOperator(operator.getUid());
			noticeService.saveAndFlush(notice);
			GeneralResponseData<BizNotice> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error(String.format("%s，参数：%s，结果：%s", errorInfo, params, e.getMessage()));
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getMessage()));
		}

	}


	@RequestMapping("/delete")
	public String delete(@RequestParam(value = "id") Integer id) throws JsonProcessingException {
		String params = buildParams().toString();
		String errorInfo = "删除公告失败：";
		try {
			BizNotice notice = noticeService.findById(id);
			if (null == notice) {
				logger.error(errorInfo + "公告不存在，请刷新页面");
				throw new Exception(errorInfo + "公告不存在，请刷新页面");
			}
			notice.setStatus(-1);
			noticeService.saveAndFlush(notice);
			GeneralResponseData<BizNotice> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			String result = mapper.writeValueAsString(responseData);
			logger.debug(String.format("%s，参数：%s，结果：%s", "公告删除", params, result));
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error(String.format("%s，参数：%s，结果：%s", "操作失败", params, e.getMessage()));
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getMessage()));
		}

	}

}
