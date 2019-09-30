package com.xinbo.fundstransfer.restful;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.domain.DynamicSpecifications;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.entity.BizAccountExpOpr;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.service.AccountExpOprService;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.HandicapService;
import com.xinbo.fundstransfer.service.SysUserService;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@SuppressWarnings("WeakAccess unused")
@RequestMapping("/r/accountExpOpr")
public class AccountExpOprController extends BaseController {
	@Autowired
	private HandicapService handicapService;
	@Autowired
	private AccountExpOprService accountAuditService;
	@Autowired
	private AccountService accountService;
	@Autowired
	private SysUserService userService;
	private ObjectMapper mapper = new ObjectMapper();

	@RequestMapping("/list")
	public String list(@RequestParam(value = "pageNo") Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "oprLike") String oprLike, @RequestParam(value = "alias") String alias,
			@RequestParam(value = "accLike") String accLike, @RequestParam(value = "bankType") String bankType,
			@RequestParam(value = "handiArray") Integer[] handiArray) throws JsonProcessingException {
		try {
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			Date[] startAndEnd = CommonUtils.parseStartAndEndTime(startAndEndTimeToArray);
			pageSize = Objects.isNull(pageSize) ? AppConstants.PAGE_SIZE : pageSize;
			List<SearchFilter> filterToList = DynamicSpecifications.build(request);
			if (startAndEnd != null && startAndEnd[0] != null && startAndEnd[1] != null) {
				filterToList.add(new SearchFilter("clientTime", SearchFilter.Operator.GTE, startAndEnd[0]));
				filterToList.add(new SearchFilter("clientTime", SearchFilter.Operator.LTE, startAndEnd[1]));
			}
			Date stTm = startAndEnd != null && startAndEnd.length == 2 ? startAndEnd[0] : null;
			Date edTm = startAndEnd != null && startAndEnd.length == 2 ? startAndEnd[1] : null;
			alias = StringUtils.trimToNull(alias);
			accLike = StringUtils.trimToNull(accLike);
			String bank = StringUtils.trimToNull(bankType);
			String opr = StringUtils.trimToNull(oprLike);
			SearchFilter[] filterToArray = filterToList.toArray(new SearchFilter[filterToList.size()]);
			Page<BizAccountExpOpr> page = accountAuditService.findPage(stTm, edTm, handiArray, alias, accLike, bank,
					opr, new PageRequest(pageNo, pageSize, Sort.Direction.DESC, "create_time"));
			GeneralResponseData<List<Map<String, Object>>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			List<Map<String, Object>> data = new ArrayList<>();
			if (!CollectionUtils.isEmpty(page.getContent())) {
				for (BizAccountExpOpr audit : page.getContent()) {
					Map<String, Object> obj = packAccAuditToStar(audit);
					if (Objects.nonNull(obj))
						data.add(obj);
				}
			}
			responseData.setData(data);
			responseData.setPage(new Paging(page));
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/addOperator")
	public String addOperator(@RequestParam(value = "id") Integer id, @RequestParam(value = "operator") String operator)
			throws JsonProcessingException {
		try {
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			if (StringUtils.isBlank(operator)) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作者为空"));
			}
			if (userService.findByUid(StringUtils.trim(operator)) == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作者不存在"));
			}
			accountAuditService.addOperator(id, operator, sysUser);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/addRemark")
	public String addRemark(@RequestParam(value = "id") Integer id, @RequestParam(value = "remark") String remark)
			throws JsonProcessingException {
		try {
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			if (StringUtils.isBlank(remark)) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "备注为空"));
			}
			accountAuditService.addRemark(id, remark, sysUser);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * re-package account audit information. middle part of properties
	 * account,mobile,owner ,sign,hook,hub,bing replaced with star flag.
	 */
	private Map<String, Object> packAccAuditToStar(BizAccountExpOpr audit) {
		AccountBaseInfo baseInfo = accountService.getFromCacheById(audit.getAccountId());
		BizHandicap handicap = handicapService.findFromCacheById(baseInfo.getHandicapId());
		String account = baseInfo.getBankType() + "|" + baseInfo.getOwner() + "|"
				+ startStar(baseInfo.getAccount(), 4);
		return new HashMap<String, Object>() {
			{
				put("alias", Objects.isNull(baseInfo) ? "" : baseInfo.getAlias());
				put("handicap", Objects.isNull(handicap) ? "" : handicap.getName());
				put("id", audit.getId());
				put("accountId", audit.getAccountId());
				put("clientIp", audit.getClientIp());
				put("clientPosition", audit.getClientPosition());
				put("clientTime", audit.getClientTime());
				put("operator", audit.getOperator());
				put("content", audit.getContent());
				put("type", audit.getType());
				put("remark", audit.getRemark());
				put("createTime", audit.getCreateTime());
				put("account", account);
			}
		};
	}

	private String startStar(String content, int length) {
		if (StringUtils.isBlank(content))
			return StringUtils.EMPTY;
		int cl = content.length();
		if (length > cl)
			return "..." + content;
		return "..." + content.substring(cl - length, cl);

	}
}
