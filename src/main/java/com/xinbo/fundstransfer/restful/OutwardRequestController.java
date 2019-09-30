package com.xinbo.fundstransfer.restful;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.common.TimeChangeCommon;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.enums.OutwardRequestStatus;
import com.xinbo.fundstransfer.domain.enums.OutwardTaskStatus;
import com.xinbo.fundstransfer.domain.enums.SysDataPermissionENUM;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.repository.OutwardTaskRepository;
import com.xinbo.fundstransfer.service.*;
import com.xinbo.fundstransfer.service.impl.RedisDistributedLockUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.shiro.SecurityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping(value = "/r/out")
public class OutwardRequestController extends BaseController {
	private static final Logger logger = LoggerFactory.getLogger(OutwardRequestController.class);
	private OutwardRequestService outwardRequestService;
	private SysUserService sysUserService;
	private AccountService accountService;
	private SysDataPermissionService sysDataPermissionService;
	private HandicapService handicapService;
	private LevelService levelService;
	private AllocateOutwardTaskService outwardTaskAllocateService;
	private ObjectMapper mapper;
	private FinTransStatService finTransStatService;
	private AccountStatisticsService accountStatisticsService;
	private OutwardTaskRepository outwardTaskRepository;
	private OutwardTaskService outwardTaskService;
	@Autowired
	private SysUserProfileService sysUserProfileService;

	@Autowired
	public OutwardRequestController(OutwardTaskService outwardTaskService, OutwardTaskRepository outwardTaskRepository,
			AccountStatisticsService accountStatisticsService, OutwardRequestService outwardRequestService,
			SysUserService sysUserService, SysDataPermissionService sysDataPermissionService,
			HandicapService handicapService, AccountService accountService, FinTransStatService finTransStatService,
			LevelService levelService, AllocateOutwardTaskService outwardTaskAllocateService, ObjectMapper mapper) {
		this.outwardTaskService = outwardTaskService;
		this.outwardTaskRepository = outwardTaskRepository;
		this.accountStatisticsService = accountStatisticsService;
		this.outwardRequestService = outwardRequestService;
		this.sysUserService = sysUserService;
		this.sysDataPermissionService = sysDataPermissionService;
		this.handicapService = handicapService;
		this.levelService = levelService;
		this.outwardTaskAllocateService = outwardTaskAllocateService;
		this.accountService = accountService;
		this.finTransStatService = finTransStatService;
		this.mapper = mapper;

	}

	/**
	 * 获取IP归属地
	 */
	@RequestMapping("/getIpAttribution/{ip}")
	public String getIpAttribution(@PathVariable("ip") String ip) throws JsonProcessingException {
		// "http://192.168.100.39/index.php?ip=116.50.140.37";
		GeneralResponseData<String> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
		String ret = null;
		if (StringUtils.isNotBlank(ip)) {
			try {
				ret = CommonUtils.getInfoByInternetUrl("http://172.25.38.90/index.php?ip=" + ip);
				responseData.setData(ret);

			} catch (Exception e) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询失败");
				responseData.setData(ret);
			}
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 统计公司用款
	 */
	@RequestMapping("/statisticsCompany")
	public String statisticsCompanyExpenditure(@RequestParam(value = "handicap", required = false) String handicap,
			@RequestParam(value = "startTime", required = false) String startTime,
			@RequestParam(value = "endTime", required = false) String endTime) throws JsonProcessingException {
		GeneralResponseData<List<Map<String, List<Object[]>>>> responseData;
		List<Map<String, List<Object[]>>> retList;
		try {
			List<Integer> handicapIdList = null;
			if (StringUtils.isNotBlank(handicap)) {
				handicapIdList = handicapService.findByNameLikeOrCodeLikeOrIdLike(handicap);
			}
			if (StringUtils.isNotBlank(handicap) && (handicapIdList == null || handicapIdList.size() == 0)) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"暂无数据,请重新输入");
				return mapper.writeValueAsString(responseData);
			}
			Date startTime1 = null;
			Date endTime1 = null;
			if (StringUtils.isNotBlank(startTime)) {
				startTime1 = CommonUtils.string2Date(startTime);
			}
			if (StringUtils.isNotBlank(endTime)) {
				endTime1 = CommonUtils.string2Date(endTime);
			}
			List<Object[]> list = outwardRequestService.statisticsCompanyExpenditure(handicapIdList, startTime1,
					endTime1);
			if (list != null && list.size() > 0) {
				retList = new ArrayList<>();
				Map<String, List<Object[]>> dataMap = new HashMap<>();
				// 用途 总计 盘口id 盘口名称 总金额
				for (int i = 0, L = list.size(); i < L; i++) {
					if (list.get(i)[2] != null && list.get(i)[3] != null) {
						String key = StringUtils.trim(new StringBuilder(list.get(i)[2].toString()).append("-")
								.append(list.get(i)[3]).toString());
						if (dataMap != null && dataMap.size() > 0) {
							if (dataMap.containsKey(key)) {
								dataMap.get(key).add(list.get(i));
							} else {
								List<Object[]> newList = new ArrayList<>();
								newList.add(list.get(i));
								dataMap.put(key, newList);
							}
						} else {
							List<Object[]> newList = new ArrayList<>();
							newList.add(list.get(i));
							dataMap.put(key, newList);
						}
					}
				}
				retList.add(dataMap);
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
				responseData.setData(retList);
				return mapper.writeValueAsString(responseData);
			} else {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "暂无数据");
				responseData.setData(null);
				return mapper.writeValueAsString(responseData);
			}
		} catch (Exception e) {
			logger.info("查询统计公司用款异常:{}", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询异常，请稍后!");
			return mapper.writeValueAsString(responseData);
		}
	}

	/**
	 * 获取审核主管信息
	 */
	@RequestMapping("/findUsersForCompanyAuditor")
	public String findUsersForCompanyAuditor(@RequestParam(value = "handicapId", required = false) Integer handicapId)
			throws JsonProcessingException {
		GeneralResponseData<List<SysUser>> responseData;
		try {
			List<SysUser> list = sysUserService.findUsersForCompanyAuditor(handicapId);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
					"获取审核主管信息成功");
			responseData.setData(list);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.info("获取公司用款审核人信息失败:{}", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "获取审核主管信息失败");
			return mapper.writeValueAsString(responseData);
		}
	}

	/**
	 * 公司用款 最终人工确认 :只需要加备注 无需改状态
	 */
	@RequestMapping("/confirmCompanyExpendResult")
	public String confirmCompanyExpendResult(@RequestParam(value = "taskId") Long taskId,
			@RequestParam(value = "remark", required = false) String remark) throws JsonProcessingException {
		GeneralResponseData<List<Map<String, Object>>> responseData;
		if (taskId == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "网络异常，稍后重试");
			return mapper.writeValueAsString(responseData);
		}
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		BizOutwardTask bizOutwardTask = outwardTaskService.findById(taskId);
		if (StringUtils.isNotBlank(bizOutwardTask.getRemark()) && bizOutwardTask.getRemark().contains("财务确认")) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "该用款已被执行确认！");
			return mapper.writeValueAsString(responseData);
		}
		if (bizOutwardTask != null && bizOutwardTask.getStatus().equals(OutwardTaskStatus.Matched.getStatus())) {
			try {
				outwardTaskAllocateService.remark4Ack(taskId, sysUser, remark);
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "确认成功");
				return mapper.writeValueAsString(responseData);
			} catch (Exception e) {
				logger.info("确认公司用款异常:{}", e);
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"确认公司用款异常。");
				return mapper.writeValueAsString(responseData);
			}
		} else {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "该用款暂时不能确认。");
			return mapper.writeValueAsString(responseData);
		}
	}

	/**
	 * 加备注 任务和请求都加备注
	 */
	@RequestMapping("/addRemarkCompanyExpend")
	public String addRemarkCompanyExpend(@RequestParam(value = "remark") String remark,
			@RequestParam(value = "taskId", required = false) Long taskId,
			@RequestParam(value = "reqId", required = false) Long reqId) throws JsonProcessingException {
		GeneralResponseData<List<Map<String, Object>>> responseData;
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (StringUtils.isBlank(remark)) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请填写备注");
			return mapper.writeValueAsString(responseData);
		}
		BizOutwardTask bizOutwardTask = null;
		if (taskId != null) {
			bizOutwardTask = outwardTaskService.findById(taskId);
		}
		if (bizOutwardTask != null) {
			outwardTaskAllocateService.remark4Custom(taskId, sysUser, StringUtils.trim(remark));
		}
		BizOutwardRequest bizOutwardRequest = outwardRequestService.get(reqId);
		if (bizOutwardTask == null && reqId != null) {
			bizOutwardRequest.setRemark(CommonUtils.genRemark(bizOutwardRequest.getRemark(), StringUtils.trim(remark),
					new Date(), new StringBuilder(sysUser.getUid()).append("备注").toString()));
			outwardRequestService.save(bizOutwardRequest);
		}
		responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "添加备注成功");
		return mapper.writeValueAsString(responseData);
	}

	private String purpose(String purpose) {
		if (StringUtils.isNotBlank(purpose)) {
			purpose = new StringBuilder("%").append(StringUtils.trim(purpose)).append("%").toString();
		} else {
			purpose = null;
		}
		return purpose;
	}

	private List<Integer> accountIdList(String outAccount) {
		List<Integer> accountIdList = null;
		if (StringUtils.isNotBlank(outAccount)) {
			accountIdList = accountService.queryAccountIdsByAlias(StringUtils.trim(outAccount));
		}
		return accountIdList;
	}

	private Integer operator(String type) {
		Integer operator = null;
		if (StringUtils.isNotBlank(type)) {
			if (type.equals("机器")) {
				operator = 1;
			} else {
				operator = 2;
			}
		}
		return operator;
	}

	/**
	 * 查询公司用款 区分公司用款订单和其他订单用member(null) statu=1 全部 2 待审核 3 未出款 4 已完成
	 */
	@RequestMapping("/queryCompanyExpend")
	public String queryCompanyExpend(@RequestParam(value = "handicapId", required = false) Integer handicapId,
			@RequestParam(value = "startTime", required = false) String startTime,
			@RequestParam(value = "endTime", required = false) String endTime,
			@RequestParam(value = "amountStart", required = false) BigDecimal amountStart,
			@RequestParam(value = "amountEnd", required = false) BigDecimal amountEnd,
			@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "outAccount", required = false) String outAccount,
			@RequestParam(value = "purpose", required = false) String purpose,
			@RequestParam(value = "pageNo", required = false) Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		GeneralResponseData<List<Map<String, Object>>> responseData;
		PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE);
		try {
			List<Map<String, Object>> list = new ArrayList<>();
			if (StringUtils.isBlank(type)) {
				type = null;
			}
			purpose = purpose(purpose);
			List<Integer> accountIdList = accountIdList(outAccount);
			if (StringUtils.isNotBlank(outAccount)) {
				if (accountIdList == null || accountIdList.size() == 0) {
					responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
							"无记录");
					responseData.setData(null);
					responseData.setPage(new Paging());
					return mapper.writeValueAsString(responseData);
				}
			}
			Integer operator = operator(type);
			List<Object[]> listData = outwardRequestService.queryCompanyExpend(pageRequest, handicapId, operator,
					amountStart, amountEnd, accountIdList, purpose, CommonUtils.string2Date(startTime),
					CommonUtils.string2Date(endTime));
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
			if (listData != null && listData.size() > 0) {
				for (Object[] p : listData) {
					Map<String, Object> map = new LinkedHashMap<>();
					map.put("reqId", p[0] != null ? p[0] : "");
					BizHandicap bizHandicap = null;
					if (p[1] != null) {
						bizHandicap = handicapService.findFromCacheById((int) p[1]);
					}
					if (bizHandicap != null) {
						map.put("handicap", StringUtils.isNotBlank(bizHandicap.getCode()) ? bizHandicap.getCode() : "");
						map.put("handicapId", bizHandicap.getId());
					} else {
						map.put("handicap", null);
						map.put("handicapId", null);
					}
					if (p[2] != null) {
						map.put("purpose", p[2]);
					} else {
						map.put("purpose", "");
					}
					map.put("orderNo", p[3] != null ? p[3] : "");
					map.put("amount", p[4] != null ? p[4] : "0");
					map.put("taskAmount", p[5] != null ? p[5] : 0);
					SysUser sysUser = null;
					if (p[6] != null) {
						sysUser = sysUserService.findFromCacheById((int) p[6]);
					}
					if (sysUser != null) {
						map.put("reviewer", StringUtils.isNotBlank(sysUser.getUid()) ? sysUser.getUid() : "");
						map.put("reviewerId", sysUser.getId());
					} else {
						map.put("reviewer", null);
						map.put("reviewerId", null);
					}
					if (p[7] != null) {
						sysUser = sysUserService.findFromCacheById((int) p[7]);
						map.put("taskOperator",
								sysUser != null && StringUtils.isNotBlank(sysUser.getUid()) ? sysUser.getUid() : "");
					} else {
						map.put("taskOperator", "");
					}
					BizAccount bizAccount = null;
					if (p[8] != null) {
						bizAccount = accountService.getById((Integer) p[8]);
					}
					if (bizAccount != null) {
						map.put("outAccountId", p[8]);
						map.put("outAccount",
								StringUtils.isNotBlank(bizAccount.getAccount()) ? bizAccount.getAccount() : "");
					} else {
						map.put("outAccountId", null);
						map.put("outAccount", null);
					}
					map.put("toAccount", p[9] != null ? p[9] : "");
					if (p[10] != null) {
						OutwardTaskStatus status = OutwardTaskStatus.findByStatus((Integer) p[10]);
						map.put("taskStatus", status != null ? status.getMsg() : "");
					} else {
						map.put("taskStatus", "");
					}
					if (null != p[18]) {
						OutwardRequestStatus status = OutwardRequestStatus.findByStatus((Integer) p[18]);
						map.put("reqStatus", status != null ? status.getMsg() : "");
					} else {
						map.put("reqStatus", "");
					}
					map.put("createTime", p[11] != null ? p[11] : "");
					map.put("asignTime", p[12] != null ? p[12] : "");
					map.put("timeComsuming", p[20] != null ? p[20] : "");
					map.put("reqRemark",
							p[13] != null
									? StringUtils.trim(p[13].toString()).replace("\r\n", "<br>").replace("\n", "<br>")
									: "");
					map.put("screenshot", p[14] != null ? p[14] : "");
					map.put("toAccountOwner", p[15] != null ? p[15] : "");
					map.put("toAccountBank", p[16] != null ? p[16] : "");
					map.put("taskId", p[17] != null ? p[17] : "");
					if (p[19] != null) {
						map.put("taskRemark", p[19] != null
								? StringUtils.trim(p[19].toString()).replace("\r\n", "<br>").replace("\n", "<br>")
								: "");
					} else {
						map.put("taskRemark", null);
					}
					list.add(map);
				}
			}
			responseData.setData(list);
			Paging page;
			if (listData != null && listData.size() > 0) {
				page = CommonUtils.getPage(pageNo + 1, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
						String.valueOf(listData.size()));
			} else {
				page = CommonUtils.getPage(0, pageSize != null ? pageSize : AppConstants.PAGE_SIZE, "0");
			}
			responseData.setPage(page);
		} catch (Exception e) {
			logger.info("查询公司用款失败:{}", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询失败");

		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 公司用款总记录数
	 */
	@RequestMapping("/countCompanyExpend")
	public String countCompanyExpend(@RequestParam(value = "handicapId", required = false) Integer handicapId,
			@RequestParam(value = "startTime", required = false) String startTime,
			@RequestParam(value = "endTime", required = false) String endTime,
			@RequestParam(value = "amountStart", required = false) BigDecimal amountStart,
			@RequestParam(value = "amountEnd", required = false) BigDecimal amountEnd,
			@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "outAccount", required = false) String outAccount,
			@RequestParam(value = "purpose", required = false) String purpose,
			@RequestParam(value = "pageNo", required = false) Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		GeneralResponseData<Long> responseData;
		try {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询总记录数");
			if (StringUtils.isBlank(type)) {
				type = null;
			}
			purpose = purpose(purpose);
			List<Integer> accountIdList = accountIdList(outAccount);
			if (StringUtils.isNotBlank(outAccount)) {
				if (accountIdList == null || accountIdList.size() == 0) {
					responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
							"无记录");
					responseData.setData(null);
					responseData.setPage(new Paging());
					return mapper.writeValueAsString(responseData);
				}
			}
			Integer operator = operator(type);
			Long count = outwardRequestService.countCompanyExpend(handicapId, operator, amountStart, amountEnd,
					accountIdList, purpose, CommonUtils.string2Date(startTime), CommonUtils.string2Date(endTime));
			Paging page;
			if (count != null) {
				page = CommonUtils.getPage(pageNo + 1, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
						String.valueOf(count));
			} else {
				page = CommonUtils.getPage(0, pageSize != null ? pageSize : AppConstants.PAGE_SIZE, "0");
			}
			responseData.setData(count);
			responseData.setPage(page);

		} catch (Exception e) {
			logger.info("查询公司用款总记录数错误：{}", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询总记录数失败");
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 公司用款总金额
	 */
	@RequestMapping("/sumCompanyExpend")
	public String sumCompanyExpend(@RequestParam(value = "handicapId", required = false) Integer handicapId,
			@RequestParam(value = "startTime", required = false) String startTime,
			@RequestParam(value = "endTime", required = false) String endTime,
			@RequestParam(value = "amountStart", required = false) BigDecimal amountStart,
			@RequestParam(value = "amountEnd", required = false) BigDecimal amountEnd,
			@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "outAccount", required = false) String outAccount,
			@RequestParam(value = "purpose", required = false) String purpose) throws JsonProcessingException {
		GeneralResponseData<BigDecimal> responseData;
		try {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询总金额成功");
			if (StringUtils.isBlank(type)) {
				type = null;
			}
			purpose = purpose(purpose);
			List<Integer> accountIdList = accountIdList(outAccount);
			if (StringUtils.isNotBlank(outAccount)) {
				if (accountIdList == null || accountIdList.size() == 0) {
					responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
							"无记录");
					responseData.setData(null);
					responseData.setPage(new Paging());
					return mapper.writeValueAsString(responseData);
				}
			}
			Integer operator = operator(type);
			BigDecimal sum = outwardRequestService.sumCompanyExpend(handicapId, operator, amountStart, amountEnd,
					accountIdList, purpose, CommonUtils.string2Date(startTime), CommonUtils.string2Date(endTime));
			if (sum != null) {
				responseData.setData(sum);
			} else {
				responseData.setData(new BigDecimal(0));
			}

		} catch (Exception e) {
			logger.info("查询公司用款金额错误：{}", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询总金额失败");
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 删除 公司用款
	 */
	@RequestMapping("/delete")
	public String delete(@RequestParam(value = "remark", required = false) String remark,
			@RequestParam(value = "reqId", required = false) Long reqId,
			@RequestParam(value = "localHostIp", required = false) String localHostIp) throws JsonProcessingException {
		GeneralResponseData<String> responseData;
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		if (StringUtils.isBlank(remark)) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请填写备注");
			return mapper.writeValueAsString(responseData);
		}
		BizOutwardRequest bizOutwardRequest = outwardRequestService.get(reqId);
		if (bizOutwardRequest != null
				&& bizOutwardRequest.getStatus().equals(OutwardRequestStatus.Processing.getStatus())) {
			RedisDistributedLockUtils redisDistributedLockUtils = new RedisDistributedLockUtils("delete" + reqId,
					3 * 1000, 3 * 1000);
			try {
				if (redisDistributedLockUtils != null && redisDistributedLockUtils.acquireLock()
						&& bizOutwardRequest.getStatus().equals(OutwardRequestStatus.Processing.getStatus())) {
					bizOutwardRequest.setRemark(CommonUtils.genRemark(bizOutwardRequest.getRemark(), remark, new Date(),
							new StringBuilder(sysUser.getUid()).append("删除(").append(localHostIp).append(")")
									.toString()));
					bizOutwardRequest.setUpdateTime(new Date());
					bizOutwardRequest.setTimeConsuming((int) (bizOutwardRequest.getUpdateTime().getTime()
							- bizOutwardRequest.getCreateTime().getTime()) / 1000);
					bizOutwardRequest.setStatus(OutwardRequestStatus.Canceled.getStatus());
					outwardRequestService.update(bizOutwardRequest);
				}
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						"删除成功!");
				return mapper.writeValueAsString(responseData);
			} catch (Exception e) {
				logger.info("删除失败:{}", e);
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "删除失败!");
				return mapper.writeValueAsString(responseData);
			} finally {
				if (redisDistributedLockUtils != null) {
					redisDistributedLockUtils.releaseLock();
				}
			}
		} else {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "状态已变更请刷新!");
			return mapper.writeValueAsString(responseData);
		}
	}

	/**
	 * 新增 编辑 公司用款
	 */
	@RequestMapping("/addCompanyExpend")
	public String addCompanyExpend(@RequestParam(value = "amount", required = false) BigDecimal amount,
			@RequestParam(value = "localHostIp", required = false) String localHostIp,
			@RequestParam(value = "remark", required = false) String remark, @RequestParam(value = "reqId") Long reqId,
			@RequestParam(value = "auditorId", required = false) Integer auditorId,
			@RequestParam(value = "toAccount", required = false) String toAccount,
			@RequestParam(value = "toBank", required = false) String toBank,
			@RequestParam(value = "toName", required = false) String toName,
			@RequestParam(value = "handicap", required = false) Integer handicap,
			@RequestParam(value = "purpose", required = false) String purpose,
			@RequestParam(value = "timeFlag", required = false) String timeFlag)
			throws JsonProcessingException, UnknownHostException {
		GeneralResponseData<String> responseData;
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		if (amount == null || StringUtils.isBlank(toAccount) || StringUtils.isBlank(toAccount)
				|| StringUtils.isBlank(toBank) || StringUtils.isBlank(toName) || StringUtils.isBlank(purpose)) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请填写参数");
			return mapper.writeValueAsString(responseData);
		}
		if (reqId != null) {
			// 编辑
			if (StringUtils.isBlank(remark)) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请填写备注");
				return mapper.writeValueAsString(responseData);
			}
			BizOutwardRequest bizOutwardRequest = outwardRequestService.get(reqId);
			if (bizOutwardRequest == null
					|| !bizOutwardRequest.getStatus().equals(OutwardRequestStatus.Processing.getStatus())) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"该条记录不能编辑");
				return mapper.writeValueAsString(responseData);
			}
			String remarkNew = CommonUtils.genRemark(bizOutwardRequest.getRemark(), remark, new Date(),
					new StringBuilder(sysUser.getUid()).append("编辑(").append(localHostIp).append(")").toString());
			logger.debug("客户端Ip:{}", localHostIp);
			// 编辑保存公司用款请求
			RedisDistributedLockUtils lockUtils = new RedisDistributedLockUtils("editCompanyExpend_" + reqId, 3 * 1000,
					3 * 1000);
			try {
				if (lockUtils != null && lockUtils.acquireLock()) {
					generateOutwardRequest(bizOutwardRequest, amount, purpose, remarkNew, toAccount, toName, toBank,
							handicap, auditorId);
				}
			} catch (Exception e) {
				logger.info("编辑公司用款参数，{}，失败:{}", reqId, e);
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "编辑失败!");
				return mapper.writeValueAsString(responseData);
			} finally {
				if (lockUtils != null) {
					lockUtils.releaseLock();
				}
			}
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "编辑成功");
			return mapper.writeValueAsString(responseData);
		} else {
			// 新增
			if (StringUtils.isBlank(timeFlag)) {
				return null;
			}
			// 如果已经新增则不再新增,防止一次新增操作重复提交
			BizOutwardRequest bizOutwardRequest = outwardRequestService
					.findByCreateTimeAndMember(new Date(Long.valueOf(timeFlag)));
			if (bizOutwardRequest != null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "添加成功");
				return mapper.writeValueAsString(responseData);
			}
			BizHandicap bizHandicap = handicapService.findFromCacheById(handicap);
			StringBuilder remarks = new StringBuilder();
			remarks.append("新增盘口").append(bizHandicap.getCode()).append("用款-").append(purpose);
			logger.debug("客户端Ip:{}", localHostIp);
			StringBuilder sb = new StringBuilder();
			sb.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append(" ")
					.append(sysUser.getUid()).append("(").append(localHostIp).append(")").append("\r\n")
					.append(remarks);
			// 新增保存公司用款请求
			generateOutwardRequest(null, amount, purpose, sb.toString(), toAccount, toName, toBank, handicap,
					auditorId);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "添加成功");
			return mapper.writeValueAsString(responseData);
		}
	}

	/**
	 * 保存公司用款请求 待审核
	 */
	private void generateOutwardRequest(BizOutwardRequest bizOutwardRequest, BigDecimal amount, String purpose,
			String remarks, String toAccount, String toName, String toBank, Integer handicap, Integer auditorId) {
		if (bizOutwardRequest == null) {
			// 新增
			bizOutwardRequest = new BizOutwardRequest();
			String str = ((Long) System.currentTimeMillis()).toString();
			str = str.substring(str.length() - 8, str.length());
			Calendar c = Calendar.getInstance();
			int year = c.get(Calendar.YEAR); // 获取年
			int month = c.get(Calendar.MONTH) + 1; // 获取月份，0表示1月份
			int day = c.get(Calendar.DAY_OF_MONTH); // 获取当前天数
			StringBuilder orderNo = new StringBuilder();
			orderNo.append("3").append(year).append(month > 9 ? month : "0" + month).append(day > 9 ? day : "0" + day)
					.append(str.substring(str.length() - 8, str.length()));
			bizOutwardRequest.setOrderNo(orderNo.toString());
		}
		bizOutwardRequest.setRemark(remarks);
		bizOutwardRequest.setStatus(OutwardRequestStatus.Processing.getStatus());
		bizOutwardRequest.setReview(purpose);// 大类-小类
		bizOutwardRequest.setReviewer(auditorId);
		bizOutwardRequest.setAmount(amount);
		bizOutwardRequest.setCreateTime(new Date());
		bizOutwardRequest.setHandicap(handicap);
		// 去除账号所有空格
		bizOutwardRequest.setToAccount(toAccount.replaceAll("\\s*", ""));
		bizOutwardRequest.setToAccountBank(toBank.replaceAll("\\s*", ""));
		bizOutwardRequest.setToAccountOwner(toName.replaceAll("\\s*", ""));
		bizOutwardRequest.setMember(null);// member为null 标识公司用款
		outwardRequestService.save(bizOutwardRequest);

	}

	/**
	 * 主管业主 审批自己盘口下的公司用款
	 */
	@RequestMapping("/approveHandicapExpend")
	public String approveHandicapExpend(@RequestParam(value = "reqId") Long reqId,
			@RequestParam(value = "localHostIp") String localHostIp,
			@RequestParam(value = "remark", required = false) String remark) throws JsonProcessingException {
		GeneralResponseData<String> responseData;
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请注册或重新登陆!");
			return mapper.writeValueAsString(responseData);
		}
		BizOutwardRequest bizOutwardRequest = outwardRequestService.get(reqId);
		if (bizOutwardRequest == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "审批信息不存在!");
			return mapper.writeValueAsString(responseData);
		}
		if (bizOutwardRequest.getReviewer() != null && !bizOutwardRequest.getReviewer().equals(sysUser.getId())) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "您不能审批!");
			return mapper.writeValueAsString(responseData);
		}
		if (bizOutwardRequest.getStatus().equals(OutwardRequestStatus.Processing.getStatus())) {
			String remarks;
			if (StringUtils.isNotBlank(remark)) {
				remarks = CommonUtils.genRemark(bizOutwardRequest.getRemark(), StringUtils.trim(remark), new Date(),
						sysUser.getUid() + "(" + localHostIp + ")" + "(审批)");
			} else {
				remarks = CommonUtils.genRemark(
						bizOutwardRequest.getRemark(), sysUser.getUid() + " 于时间："
								+ new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + " 审批!",
						new Date(), sysUser.getUid());
			}
			RedisDistributedLockUtils redisDistributedLockUtils = new RedisDistributedLockUtils(
					"approveCompanyExpend:" + reqId, 3 * 1000, 3 * 1000);
			try {
				if (redisDistributedLockUtils != null && redisDistributedLockUtils.acquireLock()) {
					// int updateRes =
					// outwardRequestService.updateForCompanyExpend(bizOutwardRequest.getId(),
					// remarks,
					// sysUser.getId());
					if (bizOutwardRequest.getStatus().equals(OutwardRequestStatus.Processing.getStatus())) {
						// 检查是否有出款任务
						Sort sort = new Sort(Sort.Direction.DESC, "asignTime");
						List<BizOutwardTask> tasks = outwardTaskRepository.findByOutwardRequestId(reqId, sort);
						if (tasks == null || tasks.size() == 0) {
							// 更新返回1但是bizOutwardRequest并没有更新
							bizOutwardRequest.setTimeConsuming(
									(int) (new Date().getTime() - bizOutwardRequest.getCreateTime().getTime()) / 1000);
							bizOutwardRequest.setStatus(OutwardRequestStatus.Approved.getStatus());
							bizOutwardRequest.setReviewer(sysUser.getId());
							bizOutwardRequest.setRemark(remarks);
							bizOutwardRequest = outwardRequestService.update(bizOutwardRequest);
							outwardRequestService.splitReqAndGenerateTask(bizOutwardRequest);
						} else {
							logger.info("已有盘口用款任务存在，任务条数：{}，订单号：{}", tasks.size(), tasks.get(0).getOrderNo());
						}
					}

				}

			} catch (Exception e) {
				logger.info("审批失败,参数:{}:{}", reqId, e);
			} finally {
				if (redisDistributedLockUtils != null) {
					redisDistributedLockUtils.releaseLock();
				}
			}
		}
		responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "审批操作成功!");
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 快捷查询
	 */
	@RequestMapping("/quickQuery")
	public String quickQueryForOut(@RequestParam(value = "member") String member,
			@RequestParam(value = "startTime") String startTime, @RequestParam(value = "endTime") String endTime,
			@RequestParam(value = "orderNo") String orderNo, @RequestParam(value = "pageNo") Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "handicapCode", required = false) String handicapCode,
			@RequestParam(value = "platFormQuery", required = false) Byte platFormQuery)
			throws JsonProcessingException {
		GeneralResponseData<Map<String, Object>> responseData;
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		boolean isNoPlatFormQuery = platFormQuery == null || platFormQuery.intValue() != 1;
		if (sysUser == null && isNoPlatFormQuery) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		BizHandicap handicap = null;
		if (!isNoPlatFormQuery) {
			if (StringUtils.isBlank(handicapCode)) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "盘口必传");
				return mapper.writeValueAsString(responseData);
			}
			handicap = handicapService.findFromCacheByCode(StringUtils.trim(handicapCode));
			if (handicap == null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "盘口不存在");
				return mapper.writeValueAsString(responseData);
			}
		}
		if (StringUtils.isBlank(member) && StringUtils.isBlank(orderNo)) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"请输入会员名或者订单号！");
			responseData.setPage(new Paging());
			return mapper.writeValueAsString(responseData);
		}
		if (StringUtils.isBlank(member)) {
			member = null;
		} else {
			member = StringUtils.trim(member);
		}
		if (StringUtils.isBlank(orderNo)) {
			orderNo = null;
		} else {
			orderNo = StringUtils.trim(orderNo);
		}
		try {
			List<SysDataPermission> sysDataPermissionList = isNoPlatFormQuery
					? sysDataPermissionService.findSysDataPermission(sysUser.getId())
					: null;
			if (isNoPlatFormQuery && CollectionUtils.isEmpty(sysDataPermissionList)) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "没有数据权限");
				return mapper.writeValueAsString(responseData);
			}
			List<Integer> handicapList = isNoPlatFormQuery ? new ArrayList<>() : Arrays.asList(handicap.getId());
			if (isNoPlatFormQuery) {
				sysDataPermissionList.forEach(p -> {
					if (SysDataPermissionENUM.HANDICAPCODE.getValue().equals(p.getFieldName())
							&& StringUtils.isNotBlank(p.getFieldValue())) {
						handicapList.add(Integer.valueOf(p.getFieldValue()));
					}
				});
			}
			List<Map<String, Object>> retList = new LinkedList<>();
			Map<String, Object> resMap = new HashMap<>();
			Sort sort = new Sort(Sort.Direction.DESC, "create_time");
			List<Object[]> list = outwardRequestService.quickQueryForOut(startTime, endTime, member, orderNo,
					handicapList);
			if (list != null && list.size() > 0) {
				for (Object[] p : list) {
					Map<String, Object> map = new HashMap<>();
					map.put("reqId", p[0]);
					if (p[1] != null) {
						map.put("taskId", p[1]);
					} else {
						map.put("taskId", null);
					}
					BizHandicap bizHandicap = null;
					if (p[2] != null) {
						bizHandicap = handicapService.findFromCacheById((Integer) p[2]);
					}
					if (bizHandicap != null && StringUtils.isNotBlank(bizHandicap.getCode())) {
						map.put("handicap", bizHandicap.getName());// 显示盘口名称
					} else {
						map.put("handicap", null);
					}

					BizLevel bizLevel = null;
					if (p[3] != null) {
						bizLevel = levelService.findFromCache((Integer) p[3]);
					}
					if (bizLevel != null && StringUtils.isNotBlank(bizLevel.getName())) {
						map.put("level", bizLevel.getName());
					} else {
						map.put("level", null);
					}
					map.put("orderNo", p[4]);
					map.put("reqAmount", p[5]);
					if (p[6] != null) {
						map.put("taskAmount", p[6]);
					} else {
						map.put("taskAmount", null);
					}
					map.put("reqStatus", p[7]);
					if (p[8] != null) {
						map.put("taskStatus",
								((int) p[8] == OutwardTaskStatus.Undeposit.getStatus() && p[17] != null) ? "ZERO"
										: p[8]);
					} else {
						map.put("taskStatus", null);
					}
					if (p[10] != null) {
						map.put("reqTimeConsuming", p[10]);
						map.put("reqCreateTime", ((Date) p[9]).getTime() + Long.parseLong(p[10].toString()) * 1000);
					} else {
						map.put("reqTimeConsuming", null);
						map.put("reqCreateTime", p[9]);
					}
					if (p[11] != null) {
						map.put("taskAsignTime", p[11]);
					} else {
						map.put("taskAsignTime", null);
					}
					if (p[12] != null) {
						map.put("taskTimeConsuming", (Integer) p[12] * 1000);
					} else {
						map.put("taskTimeConsuming", null);
					}
					map.put("reqMember", p[13]);
					if (p[14] != null) {
						map.put("reqRemark", ((String) p[14]).replace("\r\n", "<br>").replace("\n", "<br>"));
					} else {
						map.put("reqRemark", null);
					}
					if (p[15] != null) {
						map.put("taskRemark", ((String) p[15]).replace("\r\n", "<br>").replace("\n", "<br>"));
					} else {
						map.put("taskRemark", null);
					}
					if (p[16] != null) {
						map.put("successPhotoUrl", p[16]);
					} else {
						map.put("successPhotoUrl", null);
					}
					if (p[17] != null) {
						map.put("accountId", p[17]);
					} else {
						map.put("accountId", null);
					}
					retList.add(map);
				}
				Paging page;
				if (retList != null && retList.size() > 0) {
					page = CommonUtils.getPage(pageNo + 1, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
							String.valueOf(retList.size()));
				} else {
					page = CommonUtils.getPage(0, pageSize != null ? pageSize : AppConstants.PAGE_SIZE, "0");
				}
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
				responseData.setPage(page);
				resMap.put("retList", retList);
				responseData.setData(resMap);
			} else {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "无记录");
				responseData.setPage(new Paging());
				responseData.setData(null);
			}
		} catch (Exception e) {
			logger.error("快捷查询参数：{}，{},失败：{}", member, orderNo, e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 快捷查询 总记录数
	 */
	@RequestMapping("/countQuickQueryForOut")
	public String countQuickQueryForOut(@RequestParam(value = "member") String member,
			@RequestParam(value = "orderNo") String orderNo, @RequestParam(value = "pageNo") Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		GeneralResponseData<String> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "获取快捷查询总记录数成功");
		if (StringUtils.isBlank(member) && StringUtils.isBlank(orderNo)) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"请输入会员名或者订单号！");
			responseData.setPage(new Paging());
			return mapper.writeValueAsString(responseData);
		}
		if (StringUtils.isBlank(member)) {
			member = null;
		} else {
			member = "%" + member + "%";
		}
		if (StringUtils.isBlank(orderNo)) {
			orderNo = null;
		} else {
			orderNo = "%" + orderNo + "%";
		}

		try {
			Long count = outwardRequestService.quickQueryCountForOut(member, orderNo);
			Paging page;
			if (count != null) {
				page = CommonUtils.getPage(pageNo + 1, pageSize != null ? 100 : 100, count.toString());
			} else {
				page = CommonUtils.getPage(0, pageSize != null ? 100 : 100, "0");
			}
			responseData.setPage(page);
			responseData.setData(null);
		} catch (Exception e) {
			logger.info("获取快捷查询总记录数失败：{}", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"获取快捷查询总记录数失败");
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 快捷查询 总金额
	 */
	@RequestMapping("/sumQuickQueryForOut")
	public String sumQuickQueryForOut(@RequestParam(value = "member") String member,
			@RequestParam(value = "orderNo") String orderNo) throws JsonProcessingException {
		GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "获取快捷查询总金额成功");
		if (StringUtils.isBlank(member) && StringUtils.isBlank(orderNo)) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"请输入会员名或者订单号！");
			responseData.setPage(new Paging());
			return mapper.writeValueAsString(responseData);
		}
		if (StringUtils.isBlank(member)) {
			member = null;
		} else {
			member = "%" + member + "%";
		}
		if (StringUtils.isBlank(orderNo)) {
			orderNo = null;
		} else {
			orderNo = "%" + orderNo + "%";
		}
		try {
			BigDecimal[] sum = outwardRequestService.quickQuerySumForOut(member, orderNo);
			if (sum != null && sum.length > 0) {
				Map map = new HashMap();
				map.put("reqSum", sum[0]);
				if (sum.length > 1 && sum[1] != null) {
					map.put("taskSum", sum[1]);
				} else {
					map.put("taskSum", null);
				}
				responseData.setData(map);
			} else {
				responseData.setData(null);
			}
		} catch (Exception e) {
			logger.info("获取快捷查询总金额失败：{}", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "获取快捷查询总金额失败");
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 通过出款请求id查询
	 */
	@RequestMapping("/getById")
	public String getByRequestId(@RequestParam(value = "id") Long id) throws JsonProcessingException {
		GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		try {
			BizOutwardRequest bizOutwardRequest = outwardRequestService.get(id);
			Map<String, Object> map = null;
			final int len4 = 4, len2 = 2;
			if (bizOutwardRequest != null) {
				map = new LinkedHashMap<>();
				map.put("orderNo", bizOutwardRequest.getOrderNo());
				map.put("member", bizOutwardRequest.getMember());
				map.put("memberCode", bizOutwardRequest.getMemberCode());
				map.put("approveReason", bizOutwardRequest.getReview());
				map.put("amount", bizOutwardRequest.getAmount());
				String toAccount = bizOutwardRequest.getToAccount();
				if (StringUtils.isNotBlank(toAccount)) {
					int len = toAccount.length();
					if (len > len4) {
						toAccount = toAccount.substring(0, len4) + "********" + toAccount.substring(len - len4);
					} else {
						toAccount = toAccount + "********" + toAccount;
					}
				}
				map.put("toAccount", toAccount);
				map.put("createTime", bizOutwardRequest.getCreateTime());
				String toAccountOwner = StringUtils.isNotBlank(bizOutwardRequest.getToAccountOwner())
						? bizOutwardRequest.getToAccountOwner()
						: "";
				// int length = bizOutwardRequest.getToAccountOwner().length();
				// toAccountOwner = getToAccountOwner(len4, len2, toAccountOwner, length);
				map.put("toAccountOwner", toAccountOwner);
				// 开户地址
				map.put("toAccountName", bizOutwardRequest.getToAccountName());
				// 开户行
				map.put("toAccountBank", bizOutwardRequest.getToAccountBank());
				if (bizOutwardRequest.getHandicap() != null) {
					BizHandicap bizHandicap = handicapService.findFromCacheById(bizOutwardRequest.getHandicap());
					map.put("handicapName",
							bizHandicap != null && StringUtils.isNotBlank(bizHandicap.getName()) ? bizHandicap.getName()
									: "");
				}
				if (bizOutwardRequest.getLevel() != null) {
					BizLevel bizLevel = levelService.findFromCache(bizOutwardRequest.getLevel());
					map.put("level", bizLevel.getName());
				}
			}
			responseData.setData(map);
		} catch (Exception e) {
			logger.error("通过出款请求id查询参数：{}，失败：{}", id, e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询失败");
		}
		return mapper.writeValueAsString(responseData);
	}

	private String getToAccountOwner(int len4, int len2, String toAccountOwner, int length) {
		if (length > 1) {
			String asterisk = length >= len4 ? "**" : "*";
			if (length == len2) {
				toAccountOwner = asterisk + toAccountOwner.substring(1);
			} else {
				toAccountOwner = toAccountOwner.substring(0, 1) + asterisk + toAccountOwner.substring(length - 1);
			}
		}
		return toAccountOwner;
	}

	/**
	 * 通过出款审核id 会员出款相关信息
	 */
	@RequestMapping("/getRelatedInfo/{id}/{orderNo}")
	public String getRelatedInfo(@PathVariable("id") Long id) throws JsonProcessingException {
		GeneralResponseData<String> responseData;
		try {
			String jsonStr = outwardRequestService.getOutwardDetails(id);
			if (StringUtils.isNotBlank(jsonStr) && new JSONObject(jsonStr).get("Result").equals(1)) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						"获取信息成功");
			} else {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						new JSONObject(jsonStr).get("Desc") + "");
			}
			responseData.setData(jsonStr);
		} catch (Exception e) {
			logger.error("获取出款审核信息失败：", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"获取信息失败,请联系技术");
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 出款审核页签--正在审核 获取出款审核
	 */
	@RequestMapping("/getTask")
	public String getApproveTask() throws JsonProcessingException {
		GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "获取审核任务成功");
		try {
			// 获取用户信息
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser == null || sysUser.getId() == null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "用户信息不存在");
				return mapper.writeValueAsString(responseData);
			}
			final int len2 = 2, len4 = 4, statusSpec = 99999;
			BizOutwardRequest bizOutwardRequest = outwardRequestService.getApproveTask(sysUser.getId());
			if (null != bizOutwardRequest && !bizOutwardRequest.getStatus().equals(statusSpec)) {
				Map<String, Object> map = new HashMap<>(32);
				String handicapName = "";
				String level = "";
				BizHandicap bizHandicap;
				if (bizOutwardRequest.getHandicap() != null) {
					bizHandicap = handicapService.findFromCacheById(bizOutwardRequest.getHandicap());
					handicapName = (null != bizHandicap && StringUtils.isNotBlank(bizHandicap.getName()))
							? bizHandicap.getName()
							: handicapName;
				}
				map.put("id", bizOutwardRequest.getId() != null ? bizOutwardRequest.getId() : "");
				String toAccount = StringUtils.isNotBlank(bizOutwardRequest.getToAccount())
						? bizOutwardRequest.getToAccount()
						: "";
				int len = toAccount.length();
				if (len > 0) {
					if (len > len4) {
						toAccount = toAccount.substring(0, len4) + "********" + toAccount.substring(len - len4);
					} else {
						toAccount = toAccount + "********" + toAccount;
					}
				}
				map.put("toAccount", toAccount);
				if (StringUtils.isNotBlank(bizOutwardRequest.getMember())) {
					map.put("approveReason",
							StringUtils.isNotBlank(bizOutwardRequest.getReview()) ? bizOutwardRequest.getReview() : "");
				} else {
					String approveReason = "";
					if (StringUtils.isNotBlank(bizOutwardRequest.getReview())) {
						if (StringUtils.isNotBlank(handicapName)) {
							approveReason = new StringBuilder(handicapName).append("-")
									.append(bizOutwardRequest.getReview()).toString();
						} else {
							approveReason = bizOutwardRequest.getReview();
						}
					}
					map.put("approveReason", approveReason);
				}
				map.put("orderNo",
						StringUtils.isNotBlank(bizOutwardRequest.getOrderNo()) ? bizOutwardRequest.getOrderNo() : "");
				map.put("member",
						StringUtils.isNotBlank(bizOutwardRequest.getMember()) ? bizOutwardRequest.getMember() : "");
				map.put("memberCode",
						new Integer(bizOutwardRequest.getMemberCode()) != null ? bizOutwardRequest.getMemberCode()
								: "");
				map.put("amount", bizOutwardRequest.getAmount() != null ? bizOutwardRequest.getAmount() : "");
				String toAccountOwner = StringUtils.isNotBlank(bizOutwardRequest.getToAccountOwner())
						? bizOutwardRequest.getToAccountOwner()
						: "";
				// int length = toAccountOwner.length();
				// toAccountOwner = getToAccountOwner(len4, len2, toAccountOwner, length);
				map.put("toAccountOwner", toAccountOwner);
				map.put("toAccountName",
						StringUtils.isNotBlank(bizOutwardRequest.getToAccountName())
								? bizOutwardRequest.getToAccountName()
								: "");// 开户地址
				map.put("toAccountBank",
						StringUtils.isNotBlank(bizOutwardRequest.getToAccountBank())
								? bizOutwardRequest.getToAccountBank()
								: "");// 开户行
				map.put("createTime",
						bizOutwardRequest.getCreateTime() != null ? bizOutwardRequest.getCreateTime() : "");

				if (bizOutwardRequest.getLevel() != null) {
					BizLevel bizLevel = levelService.findFromCache(bizOutwardRequest.getLevel());
					level = (null != bizLevel && StringUtils.isNotBlank(bizLevel.getName())) ? bizLevel.getName()
							: level;
				}
				map.put("handicapName", handicapName);
				map.put("level", level);
				responseData.setData(map);
				return mapper.writeValueAsString(responseData);
			} else {
				if (bizOutwardRequest == null) {
					responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
							"无审核数据");
				} else if (bizOutwardRequest.getStatus().equals(99999)) {
					responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
							"无数据权限或没有设置审核金额");
				}
				responseData.setData(null);
				return mapper.writeValueAsString(responseData);
			}
		} catch (Exception e) {
			logger.error("获取出款审核失败,error:", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "获取失败");
			return mapper.writeValueAsString(responseData);
		}
	}

	/**
	 * 出款审核--查询flag 已审核2 已拒绝 3 已取消 5 转主管 4 只查当前人的记录
	 */
	@RequestMapping(value = "/get")
	public String getOutWardRequest(@RequestParam(value = "handicap") Integer handicap,
			@RequestParam(value = "level") Integer level, @RequestParam(value = "member") String member,
			@RequestParam(value = "orderNo") String orderNo, @RequestParam(value = "startTime") String startTime,
			@RequestParam(value = "endTime") String endTime, @RequestParam(value = "fromMoney") BigDecimal fromMoney,
			@RequestParam(value = "toMoney") BigDecimal toMoney, @RequestParam(value = "flag") int flag,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "pageNo") Integer pageNo) throws JsonProcessingException {
		GeneralResponseData<List<Map<String, Object>>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		SysUser sysUser1 = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser1 == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		List<Integer> handicapList = getHandicapIdByCurrentUser(handicap, sysUser1);
		if (handicapList == null || handicapList.size() == 0) {
			responseData.setData(null);
			responseData.setPage(new Paging());
			return mapper.writeValueAsString(responseData);
		}
		Integer[] status = null;
		switch (flag) {
		case 2:// 只查询已审核
			status = new Integer[] { OutwardRequestStatus.Approved.getStatus(),
					OutwardRequestStatus.Acknowledged.getStatus(), OutwardRequestStatus.Failure.getStatus() };
			break;
		case 3:// 只查询已拒绝
			status = new Integer[] { OutwardRequestStatus.Reject.getStatus() };
			break;
		case 4:// 只查询主管审核
			status = new Integer[] { OutwardRequestStatus.ManagerProcessing.getStatus() };
			break;
		case 5:// 只查询已取消
			status = new Integer[] { OutwardRequestStatus.Canceled.getStatus() };
			break;
		}
		try {
			List<Map<String, Object>> list = new ArrayList<>();
			Map<String, Object> map = new LinkedHashMap<>();
			Sort sort2 = new Sort(Sort.Direction.DESC, "updateTime");
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					sort2);
			Page<BizOutwardRequest> page = outwardRequestService.findOutwardRequestPageNoCount(handicapList, level,
					status, member, orderNo, new Integer[] { sysUser1.getId() }, null, startTime, endTime, fromMoney,
					toMoney, pageRequest);
			map.put("page", new Paging(page));
			map.put("dataList", getReturnList(page.getContent(), flag, sysUser1));
			list.add(map);
			responseData.setData(list);
		} catch (Exception e) {
			logger.error("查询失败：{}", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "获取失败");
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 获取出款审核 出款审核汇总 总金额
	 */
	@RequestMapping("/getOutWardRequestSum")
	public String getOutWardRequestSum(@RequestParam(value = "handicap") Integer handicap,
			@RequestParam(value = "level") Integer level, @RequestParam(value = "member") String member,
			@RequestParam(value = "orderNo") String orderNo, @RequestParam(value = "startTime") String startTime,
			@RequestParam(value = "endTime") String endTime, @RequestParam(value = "fromMoney") BigDecimal fromMoney,
			@RequestParam(value = "toMoney") BigDecimal toMoney, @RequestParam(value = "flag") int flag)
			throws JsonProcessingException {
		GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		SysUser sysUser1 = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser1 == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		List<Integer> handicapList = getHandicapIdByCurrentUser(handicap, sysUser1);
		if (handicapList == null || handicapList.size() == 0) {
			responseData.setData(null);
			responseData.setPage(new Paging());
			return mapper.writeValueAsString(responseData);
		}
		Map<String, Object> map = new HashMap<>();
		Integer[] status = new Integer[0];
		if (flag == 2) {
			status = new Integer[] { OutwardRequestStatus.Approved.getStatus(),
					OutwardRequestStatus.Acknowledged.getStatus(), OutwardRequestStatus.Failure.getStatus() };
		}
		if (flag == 3) {
			status = new Integer[] { OutwardRequestStatus.Reject.getStatus() };
		}
		if (flag == 4) {
			status = new Integer[] { OutwardRequestStatus.ManagerProcessing.getStatus() };
		}
		if (flag == 5) {
			status = new Integer[] { OutwardRequestStatus.Canceled.getStatus() };
		}
		String sum = outwardRequestService.getOutwardRequestSumAmount(handicapList, level, status, member, orderNo,
				new Integer[] { sysUser1.getId() }, null, startTime, endTime, fromMoney, toMoney);
		map.put("sumAmount", sum);
		responseData.setData(map);
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 获取出款审核 总记录
	 */
	@RequestMapping("/getOutWardRequestCount")
	public String getOutWardRequestCount(@RequestParam(value = "handicap") Integer handicap,
			@RequestParam(value = "level") Integer level, @RequestParam(value = "member") String member,
			@RequestParam(value = "orderNo") String orderNo, @RequestParam(value = "startTime") String startTime,
			@RequestParam(value = "endTime") String endTime, @RequestParam(value = "fromMoney") BigDecimal fromMoney,
			@RequestParam(value = "toMoney") BigDecimal toMoney, @RequestParam(value = "flag") int flag,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "pageNo") Integer pageNo) throws JsonProcessingException {
		GeneralResponseData<String> responseData;
		SysUser sysUser1 = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser1 == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		List<Integer> handicapList = getHandicapIdByCurrentUser(handicap, sysUser1);
		if (handicapList == null || handicapList.size() == 0) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "无记录");
			responseData.setData(null);
			responseData.setPage(new Paging());
			return mapper.writeValueAsString(responseData);
		}
		Integer[] status = new Integer[0];
		if (flag == 2) {
			status = new Integer[] { OutwardRequestStatus.Approved.getStatus(),
					OutwardRequestStatus.Acknowledged.getStatus(), OutwardRequestStatus.Failure.getStatus() };
		}
		if (flag == 3) {
			status = new Integer[] { OutwardRequestStatus.Reject.getStatus() };
		}
		if (flag == 4) {
			status = new Integer[] { OutwardRequestStatus.ManagerProcessing.getStatus() };
		}
		if (flag == 5) {
			status = new Integer[] { OutwardRequestStatus.Canceled.getStatus() };
		}
		try {
			Long count = outwardRequestService.getOutwardRequestCount(handicapList, level, status, member, orderNo,
					new Integer[] { sysUser1.getId() }, null, startTime, endTime, fromMoney, toMoney);
			Paging page;
			if (count != null) {
				page = CommonUtils.getPage(pageNo + 1, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
						String.valueOf(count));
			} else {
				page = CommonUtils.getPage(0, pageSize != null ? pageSize : AppConstants.PAGE_SIZE, "0");
			}
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "获取总记录成功");
			responseData.setPage(page);
		} catch (Exception e) {
			e.printStackTrace();
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "获取总记录数失败");
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * @desc 出款审核 --通过 1，取消2, 拒绝3，转主管 4; 通过 转主管 不更新update_time 字段 只有 取消 拒绝 通知平台
	 *       才会更新,但是每一次操作都更新耗时;如果已经生成了出款任务再取消拒绝任务,不更新出款请求耗时,只更新出款任务耗时。
	 */
	@RequestMapping("/save")
	public String save(@RequestParam(value = "id") Long id, @RequestParam(value = "remark") String remark,
			@RequestParam(value = "type") Integer type, @RequestParam(value = "memberCode") String memberCode,
			@RequestParam(value = "orderNo") String orderNo) throws JsonProcessingException {
		GeneralResponseData<BizOutwardRequest> responseData = null;
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		BizOutwardRequest bizOutwardRequest = outwardRequestService.get(id);
		if (bizOutwardRequest == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "记录不存在,请刷新!");
			return mapper.writeValueAsString(responseData);
		}
		if (!(bizOutwardRequest.getStatus().equals(OutwardRequestStatus.Processing.getStatus())
				|| bizOutwardRequest.getStatus().equals(OutwardRequestStatus.ManagerProcessing.getStatus()))) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "数据以变更,请刷新!");
			return mapper.writeValueAsString(responseData);
		}
		if (OutwardRequestStatus.Approved.getStatus().equals(type)) {
			// 调用生成出款任务
			try {
				if (id == null || memberCode == null || StringUtils.isBlank(orderNo)) {
					responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
							OutwardRequestStatus.Approved.getMsg() + "参数丢失，操作失败");
					return mapper.writeValueAsString(responseData);
				}
				outwardRequestService.approve(outwardRequestService.get(id), sysUser.getId(), remark, memberCode,
						orderNo);
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						OutwardRequestStatus.Approved.getMsg() + "操作成功");
				return mapper.writeValueAsString(responseData);
			} catch (Exception e) {
				logger.error("调用生成出款任务失败：{},出款审核操作，参数：{},{}", e, id, orderNo);
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败");
				return mapper.writeValueAsString(responseData);
			}
		}
		if (OutwardRequestStatus.Reject.getStatus().equals(type)
				|| OutwardRequestStatus.Canceled.getStatus().equals(type)
				|| OutwardRequestStatus.ManagerProcessing.getStatus().equals(type)) {
			if (id == null || memberCode == null || StringUtils.isBlank(remark) || StringUtils.isBlank(orderNo)) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						OutwardRequestStatus.Approved.getMsg() + "操作失败");
				return mapper.writeValueAsString(responseData);
			}
			// type 2 取消 3 拒绝 4 转主管
			// 状态 0-正在审核，1-审核通过，2-拒绝，3-主管处理，4-已取消',
			int status = type == 2 ? OutwardRequestStatus.Canceled.getStatus()
					: type == 3 ? OutwardRequestStatus.Reject.getStatus()
							: type == 4 ? OutwardRequestStatus.ManagerProcessing.getStatus() : null;
			// 调用通知平台
			try {
				outwardRequestService.reportStatus2Platform(id, status, remark, memberCode, orderNo, sysUser);
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功");
				return mapper.writeValueAsString(responseData);
			} catch (Exception e) {
				logger.error("通知失败：{}", e);
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败");
				return mapper.writeValueAsString(responseData);
			}
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 获取出款审核汇总 记录 操作类型 status = 3,0,1,2,4,6 待处理 待审核 已审核 已拒绝 已取消 通知失败
	 * 出款审核汇总---条件查询出款审核汇总信息
	 */
	@RequestMapping("/total")
	public String searchTotal(@RequestParam(value = "handicap") Integer handicap,
			@RequestParam(value = "level") Integer level, @RequestParam(value = "member") String member,
			@RequestParam(value = "orderNo") String orderNo, @RequestParam(value = "auditor") String auditor,
			@RequestParam(value = "startTime") String startTime, @RequestParam(value = "endTime") String endTime,
			@RequestParam(value = "famount") BigDecimal famount, @RequestParam(value = "tamount") BigDecimal tmount,
			@RequestParam(value = "reviwerType") String reviwerType, @RequestParam(value = "status") Integer status,
			@RequestParam(value = "pageNo") Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		SysUser sysUser1 = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser1 == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		List<Integer> handicapList = getHandicapIdByCurrentUser(handicap, sysUser1);
		if (handicapList == null || handicapList.size() == 0) {
			GeneralResponseData<List<Map<String, Object>>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "无数据");
			responseData.setData(null);
			responseData.setPage(new Paging());
			return mapper.writeValueAsString(responseData);
		}
		Integer[] statusArr = status(status);
		Integer[] auditorsId = auditor(auditor);
		// 根据审核人名称模糊查询审核人ID
		if (StringUtils.isNotBlank(auditor) && (auditorsId == null || auditorsId.length == 0)) {
			return mapper
					.writeValueAsString(new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue()));
		}
		PageRequest pageRequest;
		if (status == 1) {
			pageRequest = PageRequest.of(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					Sort.Direction.DESC, "status", "createTime");
		} else if (status == 0 || status == 3) {
			pageRequest = PageRequest.of(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					Sort.Direction.DESC, "createTime");
		} else {
			pageRequest = PageRequest.of(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					Sort.Direction.DESC, "updateTime", "createTime");

		}
		GeneralResponseData<List<Map<String, Object>>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
		try {
			Page<BizOutwardRequest> pageForTotal = outwardRequestService.findOutwardRequestPageNoCount(handicapList,
					level, statusArr, member, orderNo, auditorsId, reviwerType, startTime, endTime, famount, tmount,
					pageRequest);

			if (pageForTotal != null && null != pageForTotal.getContent() && pageForTotal.getContent().size() > 0) {
				List<BizOutwardRequest> bizOutwardRequestList = pageForTotal.getContent();
				List<Map<String, Object>> list = new LinkedList<>();
				// 组装审核人信息 组装盘口信息
				for (BizOutwardRequest req : bizOutwardRequestList) {
					Map<String, Object> map = new LinkedHashMap<>();
					map.put("id", req.getId() != null ? req.getId() : "");
					map.put("orderNo", StringUtils.isNotBlank(req.getOrderNo()) ? req.getOrderNo() : "");
					map.put("member", StringUtils.isNotBlank(req.getMember()) ? req.getMember() : "公司用款");
					String handicapName = "";
					if (req.getHandicap() != null) {
						BizHandicap bizHandicap = handicapService.findFromCacheById(req.getHandicap());
						handicapName = (null != bizHandicap && StringUtils.isNotBlank(bizHandicap.getName()))
								? bizHandicap.getName()
								: "";
					}
					map.put("handicapName", handicapName);
					String levelName = "";
					if (req.getLevel() != null) {
						BizLevel bizLevel = levelService.findFromCache(req.getLevel());
						levelName = (null != bizLevel && StringUtils.isNotBlank(bizLevel.getName()))
								? bizLevel.getName()
								: "";
					}
					map.put("levelName", levelName);
					map.put("amount", req.getAmount() != null ? req.getAmount() : "");
					String reviewer = status != 0 && status != 3 ? "机器" : "";
					String reviewerType1 = "机器";
					if (req.getReviewer() != null) {
						SysUser sysUser = sysUserService.findFromCacheById(req.getReviewer());
						reviewer = (null != sysUser && StringUtils.isNotBlank(sysUser.getUsername()))
								? sysUser.getUsername()
								: "";
					}
					reviewerType1 = req.getReviewer() == null ? reviewerType1 : "人工";
					map.put("reviewer", reviewer);
					map.put("reviewerType", reviewerType1);
					Integer timesCous = req.getTimeConsuming();
					// 时间显示 用不到 updateTime
					if (timesCous != null) {
						map.put("approveTime", req.getCreateTime().getTime() + timesCous * 1000);
						map.put("timeConsuming", CommonUtils.convertTime2String(timesCous * 1000L));
					} else {
						map.put("timeConsuming", "0 秒");
						map.put("approveTime", "");
					}
					String remark = null;
					if (status == 1) {
						List<BizOutwardTask> bizOutwardTask = outwardTaskService.findByRequestId(req.getId());
						if (!org.springframework.util.CollectionUtils.isEmpty(bizOutwardTask)) {
							remark = StringUtils.isNotBlank(bizOutwardTask.get(0).getRemark())
									? StringUtils.trim(bizOutwardTask.get(0).getRemark()).replace("\r\n", "<br>")
											.replace("\n", "<br>")
									: "";
						}
					} else {
						remark = StringUtils.isNotBlank(req.getRemark())
								? StringUtils.trim(req.getRemark()).replace("\r\n", "<br>").replace("\n", "<br>")
								: "";
					}
					map.put("remark", StringUtils.trim(remark));
					map.put("createTime", req.getCreateTime() != null ? req.getCreateTime().getTime() : "");
					if (status == 1) {
						if (req.getStatus().equals(OutwardRequestStatus.Approved.getStatus())) {
							map.put("status", OutwardRequestStatus.Approved.getMsg());
						}
						if (req.getStatus().equals(OutwardRequestStatus.Acknowledged.getStatus())) {
							map.put("status", OutwardRequestStatus.Acknowledged.getMsg());
						}
						if (req.getStatus().equals(OutwardRequestStatus.Failure.getStatus())) {
							map.put("status", OutwardRequestStatus.Failure.getMsg());
						}
					}
					list.add(map);
				}
				responseData.setData(list);
				responseData.setPage(new Paging(pageForTotal));
			}
		} catch (Exception e) {
			logger.error("查询出款审核汇总信息，方法searchTotal调用失败:{}", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询失败");
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 获取当前用户拥有的盘口id
	 */
	private List<Integer> getHandicapIdByCurrentUser(Integer handicap, SysUser sysUser) {
		List<Integer> handicapList = new ArrayList<>();
		if (sysUser == null) {
			return handicapList;
		}
		if (handicap == null) {
			List<BizHandicap> list = sysDataPermissionService.getHandicapByUserId(sysUser);
			if (list != null && list.size() > 0) {
				list.stream().forEach(p -> handicapList.add(p.getId()));
			}
		} else {
			handicapList.add(handicap);
		}
		return handicapList;
	}

	private Integer[] auditor(String auditor) {
		Integer[] auditorsId = null;
		// 根据审核人名称模糊查询审核人ID
		if (StringUtils.isNotBlank(auditor)) {
			List<SysUser> sysUserList = sysUserService.findByNameLike(auditor);
			if (null != sysUserList && sysUserList.size() > 0) {
				auditorsId = new Integer[sysUserList.size()];
				for (int i = 0, L = sysUserList.size(); i < L; i++) {
					auditorsId[i] = sysUserList.get(i).getId();
				}
			}
		}
		return auditorsId;
	}

	private Integer[] status(Integer status) {
		Integer[] statusArr = new Integer[0];
		if (status != null) {
			switch (status) {
			case 0:
				statusArr = new Integer[] { OutwardRequestStatus.Processing.getStatus() };
				break;
			case 1:
				statusArr = new Integer[] { OutwardRequestStatus.Approved.getStatus(),
						OutwardRequestStatus.Acknowledged.getStatus(), OutwardRequestStatus.Failure.getStatus() };
				break;
			case 2:
				statusArr = new Integer[] { OutwardRequestStatus.Reject.getStatus() };
				break;
			case 3:
				statusArr = new Integer[] { OutwardRequestStatus.ManagerProcessing.getStatus() };
				break;
			case 4:
				statusArr = new Integer[] { OutwardRequestStatus.Canceled.getStatus() };
				break;
			}
		}
		return statusArr;
	}

	/**
	 * 获取出款审核汇总 总金额
	 */
	@RequestMapping("/getOutwardRequestTotalSumAmount")
	public String getOutwardRequestTotalSumAmount(@RequestParam(value = "handicap") Integer handicap,
			@RequestParam(value = "level") Integer level, @RequestParam(value = "member") String member,
			@RequestParam(value = "orderNo") String orderNo, @RequestParam(value = "auditor") String auditor,
			@RequestParam(value = "startTime") String startTime, @RequestParam(value = "endTime") String endTime,
			@RequestParam(value = "famount") BigDecimal famount, @RequestParam(value = "tamount") BigDecimal tmount,
			@RequestParam(value = "reviwerType") String reviwerType, @RequestParam(value = "status") Integer status)
			throws JsonProcessingException {
		SysUser sysUser1 = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser1 == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		List<Integer> handicapList = getHandicapIdByCurrentUser(handicap, sysUser1);
		if (handicapList == null || handicapList.size() == 0) {
			GeneralResponseData<List<Map<String, Object>>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "无数据");
			responseData.setData(null);
			responseData.setPage(new Paging());
			return mapper.writeValueAsString(responseData);
		}
		Integer[] statusArr = status(status);
		Integer[] auditorsId = auditor(auditor);
		// 根据审核人名称模糊查询审核人ID
		if (StringUtils.isNotBlank(auditor) && (auditorsId == null || auditorsId.length == 0)) {
			return mapper
					.writeValueAsString(new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue()));
		}
		GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
		String sum = outwardRequestService.getOutwardRequestSumAmount(handicapList, level, statusArr, member, orderNo,
				auditorsId, reviwerType, startTime, endTime, famount, tmount);
		Map<String, Object> map = new HashMap<>();
		map.put("sumAmount", sum);
		responseData.setData(map);
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 获取出款审核汇总总记录
	 */
	@RequestMapping("/getOutwardRequestTotalCount")
	public String getOutwardRequestTotalCount(@RequestParam(value = "handicap") Integer handicap,
			@RequestParam(value = "level") Integer level, @RequestParam(value = "member") String member,
			@RequestParam(value = "orderNo") String orderNo, @RequestParam(value = "auditor") String auditor,
			@RequestParam(value = "startTime") String startTime, @RequestParam(value = "endTime") String endTime,
			@RequestParam(value = "famount") BigDecimal famount, @RequestParam(value = "tamount") BigDecimal tmount,
			@RequestParam(value = "reviwerType") String reviwerType, @RequestParam(value = "status") Integer status,
			@RequestParam(value = "pageNo") Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		SysUser sysUser1 = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser1 == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		List<Integer> handicapList = getHandicapIdByCurrentUser(handicap, sysUser1);
		if (handicapList == null || handicapList.size() == 0) {
			GeneralResponseData<List<Map<String, Object>>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "无数据");
			responseData.setData(null);
			responseData.setPage(new Paging());
			return mapper.writeValueAsString(responseData);
		}
		Integer[] statusArr = status(status);
		Integer[] auditorsId = auditor(auditor);
		// 根据审核人名称模糊查询审核人ID
		if (StringUtils.isNotBlank(auditor) && (auditorsId == null || auditorsId.length == 0)) {
			return mapper
					.writeValueAsString(new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue()));
		}
		GeneralResponseData<String> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
		try {
			Long count = outwardRequestService.getOutwardRequestCount(handicapList, level, statusArr, member, orderNo,
					auditorsId, reviwerType, startTime, endTime, famount, tmount);
			Paging page;
			if (count != null) {
				page = CommonUtils.getPage(pageNo + 1, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
						String.valueOf(count));
			} else {
				page = CommonUtils.getPage(0, pageSize != null ? pageSize : AppConstants.PAGE_SIZE, "0");
			}
			responseData.setPage(page);
		} catch (Exception e) {
			logger.error("查询出款请求汇总总记录数失败：{}", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询失败");
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 通知平台
	 */
	@RequestMapping("/noticePlatForm")
	public String noticePlatForm(@RequestParam(value = "requestId") Long requestId) throws JsonProcessingException {
		GeneralResponseData<String> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功");
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		BizOutwardRequest req = outwardRequestService.get(requestId);
		if (req == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "出款请求不存在");
			return mapper.writeValueAsString(responseData);
		}
		try {
			outwardTaskAllocateService.noticePlatIfFinished(sysUser.getId(), req);
		} catch (Exception e) {
			logger.error("通知平台失败 :{}", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "通知平台失败");
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 由当前用户获取盘口信息
	 */
	@RequestMapping(value = "/handicap")
	public String initialHandicap() throws JsonProcessingException {
		GeneralResponseData<List<BizHandicap>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		List<BizHandicap> list = sysDataPermissionService.getHandicapByUserId(sysUser);
		responseData.setData(list);
		return mapper.writeValueAsString(responseData);
	}

	public Integer[] getHandicap(Integer handicap) {
		Integer[] handicapId = new Integer[0];
		if (null == handicap) {
			// 查询该用户数据权限下的所有盘口信息
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			List<BizHandicap> bizHandicapList = sysDataPermissionService.getHandicapByUserId(sysUser);
			if (bizHandicapList != null && bizHandicapList.size() > 0) {
				handicapId = new Integer[bizHandicapList.size()];
				for (int i = 0, L = bizHandicapList.size(); i < L; i++) {
					handicapId[i] = bizHandicapList.get(i).getId();
				}
			}
		} else {
			handicapId = new Integer[1];
			handicapId[0] = handicap;
		}
		return handicapId;
	}

	private List<Map<String, Object>> getReturnList(List<BizOutwardRequest> list2, Integer status, SysUser sysUser) {
		List<Map<String, Object>> list = new LinkedList<>();
		if (list2 != null && list2.size() > 0) {
			for (BizOutwardRequest req : list2) {
				Map<String, Object> mapIner = new LinkedHashMap<>();
				mapIner.put("id", req.getId());
				mapIner.put("orderNo", req.getOrderNo());
				mapIner.put("member", StringUtils.isNotBlank(req.getMember()) ? req.getMember() : "公司用款");
				mapIner.put("handicap", req.getHandicap() != null ? req.getHandicap() : "");
				mapIner.put("level", req.getLevel() != null ? req.getLevel() : "");
				mapIner.put("amount", req.getAmount());
				mapIner.put("reviewer", sysUser.getUid());// 当前人只能查看自己的操作记录,所以操作人必定是当前人
				mapIner.put("reviewerType", "人工");
				// flag 已审核2 已拒绝 3 已取消 5 转主管 4
				Integer timesCous = req.getTimeConsuming();
				// 时间显示 用不到 updateTime
				if (timesCous != null) {
					mapIner.put("approveTime", req.getCreateTime().getTime() + timesCous * 1000);
					mapIner.put("timeConsuming", CommonUtils.convertTime2String(timesCous * 1000L));
				} else {
					mapIner.put("timeConsuming", "0 秒");
					mapIner.put("approveTime", "");
				}
				String remark;
				if (status == 2 && (req.getStatus().equals(OutwardRequestStatus.Acknowledged.getStatus())
						|| req.getStatus().equals(OutwardRequestStatus.Failure.getStatus()))) {
					List<BizOutwardTask> bizOutwardTask = outwardTaskService.findByRequestId(req.getId());
					remark = StringUtils.isNotBlank(bizOutwardTask.get(0).getRemark()) ? StringUtils
							.trim(bizOutwardTask.get(0).getRemark()).replace("\r\n", "<br>").replace("\n", "<br>") : "";
				} else {
					remark = StringUtils.isNotBlank(req.getRemark())
							? StringUtils.trim(req.getRemark()).replace("\r\n", "<br>").replace("\n", "<br>")
							: "";
				}
				mapIner.put("remark", remark);
				mapIner.put("review", StringUtils.isNotBlank(req.getReview()) ? req.getReview() : "");
				mapIner.put("applyTime", req.getCreateTime());
				list.add(mapIner);
			}
		}
		return list;
	}

	@RequestMapping("/exportout/{startAndEndTimeToArray}/{handicap}/{accountid}/{handicapid}")
	public void exportOutwardSys(@PathVariable String[] startAndEndTimeToArray,
			@PathVariable(value = "handicap") Integer handicap, @PathVariable(value = "accountid") Integer accountid,
			@PathVariable(value = "handicapid") int handicapid, HttpServletRequest request,
			HttpServletResponse response) throws JsonProcessingException, ParseException {

		// 拼接时间戳查询数据条件
		String startTime = null;
		String endTime = null;
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		if (0 != startAndEndTimeToArray.length && null != startAndEndTimeToArray) {
			startTime = startAndEndTimeToArray[0];
			endTime = startAndEndTimeToArray[1];
		}
		Date dtf1 = df.parse(endTime);
		List<Integer> handicaps = new ArrayList<>();
		// 只能查询自己区域的账号
		List<SysUserProfile> manilaUserPro = sysUserProfileService.findByPropertyKey("HANDICAP_MANILA_ZONE");
		String manilaHandicap = manilaUserPro.get(0).getPropertyValue();
		List<SysUserProfile> taiWanuserPro = sysUserProfileService.findByPropertyKey("HANDICAP_TAIWAN_ZONE");
		String taiwanHandicap = taiWanuserPro.get(0).getPropertyValue();
		BizHandicap bizHandicapPro = handicapService.findFromCacheById(operator.getHandicap());
		List<Integer> handicapIds = handicapService
				.findByZone(bizHandicapPro == null ? "0" : bizHandicapPro.getId().toString());
		String handicapId = CollectionUtils.isEmpty(handicapIds) ? "0" : handicapIds.get(0).toString();
		if (ArrayUtils.contains(manilaHandicap.substring(1, manilaHandicap.length()).split(";"),
				(operator.getCategory() > 400 ? (operator.getCategory() - 400) + "" : handicapId))) {
			String[] handicapStr = manilaHandicap.substring(1, manilaHandicap.length()).split(";");
			for (String hd : handicapStr) {
				handicaps.add(Integer.valueOf(hd));
			}
		}
		if (ArrayUtils.contains(taiwanHandicap.substring(1, taiwanHandicap.length()).split(";"),
				(operator.getCategory() > 400 ? (operator.getCategory() - 400) + "" : handicapId))) {
			String[] handicapStr = taiwanHandicap.substring(1, taiwanHandicap.length()).split(";");
			for (String hd : handicapStr) {
				handicaps.add(Integer.valueOf(hd));
			}
		}
		try {
			PageRequest pageRequest = new PageRequest(0, 999999999);
			if (accountid != 0 && accountid != 8888 && handicapid != 8888) {
				// 中转明细 下发银行卡导出全部下发卡的系统明细
				if (accountid == 99999999 || accountid == 77777777) {
					Map<String, Object> mapp = finTransStatService.finTransStatMatch(null,
							TimeChangeCommon.TimeStamp2Date(startTime, "yyyy-MM-dd HH:mm:ss"),
							TimeChangeCommon.TimeStamp2Date(endTime, "yyyy-MM-dd HH:mm:ss"), new BigDecimal(0),
							new BigDecimal(0), 0, accountid == 99999999 ? 107 : 110, "sys", 9999, handicap, handicaps,
							pageRequest);
					Page<Object> page = (Page<Object>) mapp.get("Page");
					List<Object> list = page.getContent();

					Map<String, String> excelInfo = new HashMap<String, String>();
					excelInfo.put("excelName",
							startTime + "-" + endTime + (accountid == 99999999 ? "下发银行卡中转数据" : "备用银行卡中转数据"));
					excelInfo.put("titleName",
							startTime + "-" + endTime + (accountid == 99999999 ? "下发银行卡中转数据" : "备用银行卡中转数据"));
					response = setResponse(response, excelInfo.get("excelName"));
					OutputStream output = response.getOutputStream();
					BufferedOutputStream bufferedOutPut = new BufferedOutputStream(output);
					HSSFWorkbook wb = new HSSFWorkbook();
					// 样式
					HSSFCellStyle cellStyleTitle = getCellStyleTitle(wb);
					HSSFCellStyle cellStyle = getCellStyle(wb);
					HSSFSheet sheet = wb.createSheet();
					String[] titleName = new String[] { "盘口", "汇出账号", "订单号", "状态", "汇入账号", "金额", "手续费", "时间" };
					// 合并列 开始行，结束行，开始列，结束列F
					HSSFRow rowTitle1 = sheet.createRow(0);
					sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, titleName.length - 1));
					loadRowContents(rowTitle1, cellStyleTitle, new String[] { excelInfo.get("titleName") });
					HSSFRow rowTitle2 = sheet.createRow(1);
					loadRowContents(rowTitle2, cellStyleTitle, titleName);
					for (int i = 0; i < list.size(); i++) {
						Object[] p = (Object[]) list.get(i);
						HSSFRow row = sheet.createRow(i + 2);
						BizHandicap bizHandicap = handicapService.findFromCacheById((Integer) p[10]);
						AccountBaseInfo bizAccountFrom = accountService.getFromCacheById((Integer) p[1]);
						AccountBaseInfo bizAccountTo = accountService.getFromCacheById((Integer) p[8]);
						String status = "";
						switch (p[9].toString()) {
						case "0":
							status = "审核通过";
							break;
						case "1":
							status = "已匹配";
							break;
						case "2":
							status = "无法匹配";
							break;
						case "3":
							status = "已取消";
							break;
						default:
							status = "";
							break;
						}
						loadRowContentss(row, cellStyle,
								new String[] { null == bizHandicap ? "" : bizHandicap.getName(),
										(null == bizAccountFrom ? "" : bizAccountFrom.getOwner()) + "|"
												+ (null == bizAccountFrom ? "" : bizAccountFrom.getBankType()),
										p[0].toString(), status,
										(null == bizAccountTo ? "" : bizAccountTo.getOwner()) + "|"
												+ (null == bizAccountTo ? "" : bizAccountTo.getBankType()),
										p[4].toString(), p[5].toString(), p[6].toString() },
								false);
					}
					HSSFRow rowFoot = sheet.createRow((list == null ? 0 : list.size()) + 2);
					loadRowContents(rowFoot, cellStyleTitle, new String[] { "总条数：" + list.size() });
					// 列宽自适应 不可移动代码位置
					for (int i = 0; i < titleName.length; i++) {
						sheet.autoSizeColumn(i);
					}
					try {
						bufferedOutPut.flush();
						wb.write(bufferedOutPut);
						bufferedOutPut.close();
						wb.close();
					} catch (IOException e) {
						e.printStackTrace();
						logger.trace("Output   is   closed");
					}
				} else {
					// 查询中转明细，第三方入款中转数据 进行导出
					List<Object[]> list = finTransStatService.finThirdPartyTransit(startTime, endTime, accountid,
							handicap, handicaps);
					if (0 != startAndEndTimeToArray.length) {
						Map<String, String> excelInfo = new HashMap<String, String>();
						excelInfo.put("excelName", startTime + "-" + endTime + "中转数据");
						excelInfo.put("titleName", startTime + "-" + endTime + "中转数据");
						response = setResponse(response, excelInfo.get("excelName"));
						OutputStream output = response.getOutputStream();
						BufferedOutputStream bufferedOutPut = new BufferedOutputStream(output);
						HSSFWorkbook wb = new HSSFWorkbook();
						// 样式
						HSSFCellStyle cellStyleTitle = getCellStyleTitle(wb);
						HSSFCellStyle cellStyle = getCellStyle(wb);
						HSSFSheet sheet = wb.createSheet();
						String[] titleName = new String[] { "盘口", "层级", "汇出账号", "汇出账号名称", "类别", "金额", "手续费", "汇入账号",
								"编号", "订单号", "状态", "创建时间", "更新时间" };
						// 合并列 开始行，结束行，开始列，结束列
						HSSFRow rowTitle1 = sheet.createRow(0);
						sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, titleName.length - 1));
						loadRowContents(rowTitle1, cellStyleTitle, new String[] { excelInfo.get("titleName") });
						HSSFRow rowTitle2 = sheet.createRow(1);
						loadRowContents(rowTitle2, cellStyleTitle, titleName);
						for (int i = 0; i < list.size(); i++) {
							Object[] p = list.get(i);
							HSSFRow row = sheet.createRow(i + 2);
							BizHandicap bizHandicap = handicapService.findFromCacheById((Integer) p[0]);
							BizLevel bizLevel = levelService.findFromCache((Integer) p[1]);
							AccountBaseInfo bizAccount = accountService.getFromCacheById((Integer) p[11]);
							String reStatus = "";
							switch (p[8].toString()) {
							case "0":
								reStatus = "匹配中";
								break;
							case "1":
								reStatus = "已匹配";
								break;
							case "2":
								reStatus = "无法匹配";
								break;
							case "3":
								reStatus = "已取消";
								break;
							default:
								reStatus = "";
								break;
							}
							loadRowContentss(row, cellStyle,
									new String[] { null == bizHandicap ? "" : bizHandicap.getName(),
											null == bizLevel ? "" : bizLevel.getName(), (String) p[2], p[3].toString(),
											null == p[4] ? "" : p[4].toString(), p[6].toString(),
											null == p[7] ? "0" : p[7].toString(),
											bizAccount.getOwner() + "|" + bizAccount.getBankType(),
											bizAccount.getAlias(), p[5].toString(), reStatus, p[9].toString(),
											null == p[10] ? "" : p[10].toString() },
									false);
						}
						HSSFRow rowFoot = sheet.createRow((list == null ? 0 : list.size()) + 2);
						loadRowContents(rowFoot, cellStyleTitle, new String[] { "总条数：" + list.size() });
						// 列宽自适应 不可移动代码位置
						for (int i = 0; i < titleName.length; i++) {
							sheet.autoSizeColumn(i);
						}
						try {
							bufferedOutPut.flush();
							wb.write(bufferedOutPut);
							bufferedOutPut.close();
							wb.close();
						} catch (IOException e) {
							e.printStackTrace();
							logger.trace("Output   is   closed");
						}
					}

				}

			} else if (handicapid != 0 && accountid != 8888 && handicapid != 8888) {
				// 导出按照盘口统计的数据
				Map<String, Object> mapp = accountStatisticsService.findFinOutStatMatch(handicapid, 0, null,
						TimeChangeCommon.TimeStamp2Date(startTime, "yyyy-MM-dd HH:mm:ss"),
						TimeChangeCommon.TimeStamp2Date(endTime, "yyyy-MM-dd HH:mm:ss"), new BigDecimal(0),
						new BigDecimal(0), "handicapid", handicapid, 0, 9999, 9999, handicaps, pageRequest);
				Page<Object> page = (Page<Object>) mapp.get("Page");
				List<Object> list = page.getContent();

				Map<String, String> excelInfo = new HashMap<String, String>();
				excelInfo.put("excelName", startTime + "-" + endTime + "出款数据");
				excelInfo.put("titleName", startTime + "-" + endTime + "出款数据");
				response = setResponse(response, excelInfo.get("excelName"));
				OutputStream output = response.getOutputStream();
				BufferedOutputStream bufferedOutPut = new BufferedOutputStream(output);
				HSSFWorkbook wb = new HSSFWorkbook();
				// 样式
				HSSFCellStyle cellStyleTitle = getCellStyleTitle(wb);
				HSSFCellStyle cellStyle = getCellStyle(wb);
				HSSFSheet sheet = wb.createSheet();
				String[] titleName = new String[] { "盘口", "会员", "订单号", "审核状态", "出款状态", "申请金额", "出款金额", "申请时间", "更新时间",
						"汇出账号", "出款人", "会员姓名", "金额", "匹配时间" };
				// 合并列 开始行，结束行，开始列，结束列
				HSSFRow rowTitle1 = sheet.createRow(0);
				sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, titleName.length - 1));
				loadRowContents(rowTitle1, cellStyleTitle, new String[] { excelInfo.get("titleName") });
				HSSFRow rowTitle2 = sheet.createRow(1);
				loadRowContents(rowTitle2, cellStyleTitle, titleName);
				for (int i = 0; i < list.size(); i++) {
					Object[] p = (Object[]) list.get(i);
					HSSFRow row = sheet.createRow(i + 2);
					BizHandicap bizHandicap = handicapService.findFromCacheById((Integer) p[0]);
					SysUser sysUser = sysUserService.findFromCacheById((Integer) p[7]);
					AccountBaseInfo bizAccount = accountService.getFromCacheById((Integer) p[13]);
					String reStatus = "";
					switch (p[10].toString()) {
					case "1":
						reStatus = "审核通过";
						break;
					case "2":
						reStatus = "拒绝";
						break;
					case "3":
						reStatus = "主管处理";
						break;
					case "4":
						reStatus = "已取消";
						break;
					case "5":
						reStatus = "出款成功，平台已确认";
						break;
					case "6":
						reStatus = "出款成功，与平台确认失败";
						break;
					default:
						reStatus = "";
						break;
					}
					String taStatus = "";
					switch (p[11].toString().toString()) {
					case "1":
						taStatus = "已出款";
						break;
					case "2":
						taStatus = "主管处理";
						break;
					case "3":
						taStatus = "主管取消";
						break;
					case "4":
						taStatus = "主管拒绝";
						break;
					case "5":
						taStatus = "流水匹配";
						break;
					case "6":
						taStatus = "出款失败";
						break;
					case "7":
						taStatus = "无效记录，已重新出款";
						break;
					case "8":
						taStatus = "银行维护";
						break;
					default:
						taStatus = "";
						break;
					}
					String remarks = p[14].toString();
					String handicapAndTradeName = "";
					// 判断是否第三方出款。
					if (remarks.indexOf("{") != -1 && p[13] == null) {
						// 取最后一个 出款数据
						String newStr = remarks.substring(remarks.lastIndexOf("{") + 1, remarks.lastIndexOf("}"));
						String[] remark = newStr.split("\\|");
						// 判断是否存在多个第三方账号出款
						if (remark.length > 1) {
							for (int j = 0; j < remark.length; j++) {
								handicapAndTradeName += remark[j].split(",")[0] + "," + remark[j].split(",")[1] + ";";
							}
						} else {
							// 异常数据处理(存在不规范格式)
							String[] SpecificationRemark = newStr.split(",");
							if (SpecificationRemark.length > 1) {
								handicapAndTradeName += newStr.split(",")[0] + "," + newStr.split(",")[1];
							}
						}
					}
					Date dtf2 = null == p[15] ? null : df.parse(p[16].toString());
					boolean isFlag = null != dtf2 && dtf1.getTime() < dtf2.getTime();
					loadRowContentss(row, cellStyle, new String[] { null == bizHandicap ? "" : bizHandicap.getName(),
							(String) p[1], (String) p[2], reStatus, taStatus, p[5].toString(), p[6].toString(),
							(String) p[3], (String) p[4],
							!"".equals(handicapAndTradeName) ? handicapAndTradeName
									: ((null == bizAccount ? "" : bizAccount.getOwner()) + "|"
											+ (null == bizAccount ? "" : bizAccount.getBankType())),
							null == sysUser ? "机器" : sysUser.getUsername(), null == p[15] ? "" : p[9].toString(),
							null == p[15] ? "" : p[6].toString(), null == p[15] ? "" : p[16].toString() }, isFlag);
				}
				HSSFRow rowFoot = sheet.createRow((list == null ? 0 : list.size()) + 2);
				loadRowContents(rowFoot, cellStyleTitle, new String[] { "总条数：" + list.size() });
				// 列宽自适应 不可移动代码位置
				for (int i = 0; i < titleName.length; i++) {
					sheet.autoSizeColumn(i);
				}
				try {
					bufferedOutPut.flush();
					wb.write(bufferedOutPut);
					bufferedOutPut.close();
					wb.close();
				} catch (IOException e) {
					e.printStackTrace();
					logger.trace("Output   is   closed");
				}

			} else if (accountid == 8888 && handicapid == 8888) {// 导出全部盘口的出款单
				// 导出按照盘口统计的数据
				Map<String, Object> mapp = accountStatisticsService.findFinOutStatMatch(0, 0, null,
						TimeChangeCommon.TimeStamp2Date(startTime, "yyyy-MM-dd HH:mm:ss"),
						TimeChangeCommon.TimeStamp2Date(endTime, "yyyy-MM-dd HH:mm:ss"), new BigDecimal(0),
						new BigDecimal(0), "handicapid", 0, 0, 9999, 9999, handicaps, pageRequest);
				Page<Object> page = (Page<Object>) mapp.get("Page");
				List<Object> list = page.getContent();

				Map<String, String> excelInfo = new HashMap<String, String>();
				excelInfo.put("excelName", startTime + "-" + endTime + "出款数据");
				excelInfo.put("titleName", startTime + "-" + endTime + "出款数据");
				response = setResponse(response, excelInfo.get("excelName"));
				OutputStream output = response.getOutputStream();
				BufferedOutputStream bufferedOutPut = new BufferedOutputStream(output);
				HSSFWorkbook wb = new HSSFWorkbook();
				// 样式
				HSSFCellStyle cellStyleTitle = getCellStyleTitle(wb);
				HSSFCellStyle cellStyle = getCellStyle(wb);
				int totle = list.size();// 获取List集合的size
				int mus = 50000;// 每个工作表格最多存储50000条数据（注：excel表格一个工作表可以存储65536条）
				int avg = totle / mus;
				for (int k = 0; k < avg + 1; k++) {
					HSSFSheet sheet = wb.createSheet();
					// 合并列 开始行，结束行，开始列，结束列
					HSSFRow rowTitle1 = sheet.createRow(0);
					String[] titleName = new String[] { "盘口", "会员", "订单号", "审核状态", "出款状态", "申请金额", "出款金额", "申请时间",
							"更新时间", "汇出账号", "类型", "出款人", "会员姓名", "金额", "匹配时间" };
					sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, titleName.length - 1));
					loadRowContents(rowTitle1, cellStyleTitle, new String[] { excelInfo.get("titleName") });
					HSSFRow rowTitle2 = sheet.createRow(1);
					loadRowContents(rowTitle2, cellStyleTitle, titleName);

					int num = k * mus;
					int index = 0;
					for (int i = num; i < list.size(); i++) {
						if (index == mus) {// 判断index == mus的时候跳出当前for循环
							break;
						}
						Object[] p = (Object[]) list.get(i);
						HSSFRow row = sheet.createRow(index + 2);
						BizHandicap bizHandicap = handicapService.findFromCacheById((Integer) p[0]);
						SysUser sysUser = sysUserService.findFromCacheById((Integer) p[7]);
						AccountBaseInfo bizAccount = accountService.getFromCacheById((Integer) p[13]);
						String reStatus = "";
						switch (p[10].toString()) {
						case "1":
							reStatus = "审核通过";
							break;
						case "2":
							reStatus = "拒绝";
							break;
						case "3":
							reStatus = "主管处理";
							break;
						case "4":
							reStatus = "已取消";
							break;
						case "5":
							reStatus = "出款成功，平台已确认";
							break;
						case "6":
							reStatus = "出款成功，与平台确认失败";
							break;
						default:
							reStatus = "";
							break;
						}
						String taStatus = "";
						switch (p[11].toString().toString()) {
						case "1":
							taStatus = "已出款";
							break;
						case "2":
							taStatus = "主管处理";
							break;
						case "3":
							taStatus = "主管取消";
							break;
						case "4":
							taStatus = "主管拒绝";
							break;
						case "5":
							taStatus = "流水匹配";
							break;
						case "6":
							taStatus = "出款失败";
							break;
						case "7":
							taStatus = "无效记录，已重新出款";
							break;
						case "8":
							taStatus = "银行维护";
							break;
						default:
							taStatus = "";
							break;
						}
						String remarks = p[14].toString();
						String handicapAndTradeName = "";
						// 判断是否第三方出款。
						if (remarks.indexOf("{") != -1 && p[13] == null) {
							// 取最后一个 出款数据
							String newStr = remarks.substring(remarks.lastIndexOf("{") + 1, remarks.lastIndexOf("}"));
							String[] remark = newStr.split("\\|");
							// 判断是否存在多个第三方账号出款
							if (remark.length > 1) {
								for (int j = 0; j < remark.length; j++) {
									handicapAndTradeName += remark[j].split(",")[0] + "," + remark[j].split(",")[1]
											+ ";";
								}
							} else {
								// 异常数据处理(存在不规范格式)
								String[] SpecificationRemark = newStr.split(",");
								if (SpecificationRemark.length > 1) {
									handicapAndTradeName += newStr.split(",")[0] + "," + newStr.split(",")[1];
								}
							}
						}
						Date dtf2 = null == p[15] ? null : df.parse(p[16].toString());
						boolean isFlag = null != dtf2 && dtf1.getTime() < dtf2.getTime();
						loadRowContentss(row, cellStyle, new String[] {
								null == bizHandicap ? "" : bizHandicap.getName(), (String) p[1], (String) p[2],
								reStatus, taStatus, p[5].toString(), p[6].toString(), (String) p[3], (String) p[4],
								!"".equals(handicapAndTradeName) ? handicapAndTradeName
										: ((null == bizAccount ? "" : bizAccount.getOwner()) + "|"
												+ (null == bizAccount ? "" : bizAccount.getBankType())),
								null == (AccountType.findByTypeId(null == bizAccount ? 0 : bizAccount.getType())) ? ""
										: AccountType.findByTypeId(null == bizAccount ? 0 : bizAccount.getType())
												.getMsg(),
								null == sysUser ? "机器" : sysUser.getUsername(), null == p[15] ? "" : p[9].toString(),
								null == p[15] ? "" : p[6].toString(), null == p[15] ? "" : p[16].toString() }, isFlag);
						index++;
					}
					HSSFRow rowFoot = sheet.createRow((list == null ? 0 : index) + 2);
					loadRowContents(rowFoot, cellStyleTitle, new String[] { "总条数：" + index });
					// 列宽自适应 不可移动代码位置
					for (int i = 0; i < titleName.length; i++) {
						sheet.autoSizeColumn(i);
					}
				}

				try {
					bufferedOutPut.flush();
					wb.write(bufferedOutPut);
					bufferedOutPut.close();
					wb.close();
				} catch (IOException e) {
					e.printStackTrace();
					logger.trace("Output   is   closed");
				}

			} else {
				Page<Object[]> page = outwardRequestService.quickQuery(null, null, startTime, endTime, handicap,
						pageRequest);
				// 查询存在回冲的单，切创建时间和更新时间不在一天的数据， 导出时 标记。
				// 快捷查询 导出
				List<Object> backToRushList = outwardRequestService.quickBackToRush(startTime, endTime);
				List<Object[]> list = page.getContent();
				if (list != null && list.size() > 0) {
					if (0 != startAndEndTimeToArray.length) {
						Map<String, String> excelInfo = new HashMap<String, String>();
						excelInfo.put("excelName", startTime + "-" + endTime + "出款数据");
						excelInfo.put("titleName", startTime + "-" + endTime + "出款数据");
						response = setResponse(response, excelInfo.get("excelName"));
						OutputStream output = response.getOutputStream();
						BufferedOutputStream bufferedOutPut = new BufferedOutputStream(output);
						HSSFWorkbook wb = new HSSFWorkbook();
						// 样式
						HSSFCellStyle cellStyleTitle = getCellStyleTitle(wb);
						HSSFCellStyle cellStyle = getCellStyle(wb);
						HSSFSheet sheet = wb.createSheet();
						String[] titleName = new String[] { "盘口", "层级", "会员名", "真实名", "订单号", "订单金额", "任务金额", "审核状态",
								"出款状态", "审核时间", "出款时间" };
						// 合并列 开始行，结束行，开始列，结束列
						HSSFRow rowTitle1 = sheet.createRow(0);
						sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, titleName.length - 1));
						loadRowContents(rowTitle1, cellStyleTitle, new String[] { excelInfo.get("titleName") });
						HSSFRow rowTitle2 = sheet.createRow(1);
						loadRowContents(rowTitle2, cellStyleTitle, titleName);
						for (int i = 0; i < list.size(); i++) {
							Object[] p = list.get(i);
							HSSFRow row = sheet.createRow(i + 2);
							BizHandicap bizHandicap = handicapService.findFromCacheById((Integer) p[2]);
							BizLevel bizLevel = levelService.findFromCache((Integer) p[3]);
							String reStatus = "";
							switch (p[5].toString()) {
							case "1":
								reStatus = "审核通过";
								break;
							case "2":
								reStatus = "拒绝";
								break;
							case "3":
								reStatus = "主管处理";
								break;
							case "4":
								reStatus = "已取消";
								break;
							case "5":
								reStatus = "出款成功，平台已确认";
								break;
							case "6":
								reStatus = "出款成功，与平台确认失败";
								break;
							default:
								reStatus = "";
								break;
							}
							String taStatus = "";
							switch (p[9].toString().toString()) {
							case "1":
								taStatus = "已出款";
								break;
							case "2":
								taStatus = "主管处理";
								break;
							case "3":
								taStatus = "主管取消";
								break;
							case "4":
								taStatus = "主管拒绝";
								break;
							case "5":
								taStatus = "流水匹配";
								break;
							case "6":
								taStatus = "出款失败";
								break;
							case "7":
								taStatus = "无效记录，已重新出款";
								break;
							case "8":
								taStatus = "银行维护";
								break;
							default:
								taStatus = "";
								break;
							}
							boolean isFlag = backToRushList.stream().filter(orders -> orders.equals(p[1])).count() > 0;
							loadRowContentss(row, cellStyle,
									new String[] { bizHandicap.getName(), bizLevel.getName(), (String) p[7],
											p[14].toString(), (String) p[1], p[4].toString(), p[8].toString(), reStatus,
											taStatus, p[6].toString(), p[13] == null ? "空" : p[13].toString() },
									isFlag);
						}
						HSSFRow rowFoot = sheet.createRow((list == null ? 0 : list.size()) + 2);
						loadRowContents(rowFoot, cellStyleTitle, new String[] { "总条数：" + list.size() });
						// 列宽自适应 不可移动代码位置
						for (int i = 0; i < titleName.length; i++) {
							sheet.autoSizeColumn(i);
						}
						try {
							bufferedOutPut.flush();
							wb.write(bufferedOutPut);
							bufferedOutPut.close();
							wb.close();
						} catch (IOException e) {
							e.printStackTrace();
							logger.trace("Output   is   closed");
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("导出出款信息失败：{}", e);
		}
	}

	/**
	 * Excel title样式
	 *
	 * @param wb
	 * @return
	 */
	@SuppressWarnings("deprecation")
	private HSSFCellStyle getCellStyleTitle(HSSFWorkbook wb) {
		HSSFCellStyle cellStyleTitle = wb.createCellStyle();
		cellStyleTitle.setAlignment(HSSFCellStyle.ALIGN_CENTER);
		cellStyleTitle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
		HSSFFont font = wb.createFont();
		font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
		font.setFontName("宋体");
		font.setFontHeight((short) 200);
		cellStyleTitle.setFont(font);
		// 表头第一行信息必须强制换行 切勿删除！
		cellStyleTitle.setWrapText(true);
		return cellStyleTitle;
	}

	/**
	 * Excel 内容样式
	 *
	 * @param wb
	 * @return
	 */
	@SuppressWarnings("deprecation")
	private HSSFCellStyle getCellStyle(HSSFWorkbook wb) {
		HSSFCellStyle cellStyle = wb.createCellStyle();
		cellStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER);
		cellStyle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
		HSSFFont font = wb.createFont();
		font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
		font.setFontName("宋体");
		font.setFontHeight((short) 200);
		cellStyle.setWrapText(true);// 先设置为自动换行
		return cellStyle;
	}

	/**
	 * 加载单行数据内容
	 *
	 * @param row
	 * @param cellStyleTitle
	 * @param contents
	 * @return
	 */
	private HSSFRow loadRowContents(HSSFRow row, HSSFCellStyle cellStyleTitle, Object[] contents) {
		for (int i = 0; i < contents.length; i++) {
			HSSFCell cell = row.createCell(i);
			cell.setCellStyle(cellStyleTitle);
			cell.setCellValue(new HSSFRichTextString(null != contents[i] ? contents[i].toString() : ""));
			cell = row.createCell(i + 1);
		}
		return row;
	}

	/**
	 * 加载单行数据内容
	 *
	 * @param row
	 * @param cellStyleTitle
	 * @param contents
	 * @return
	 */
	private HSSFRow loadRowContentss(HSSFRow row, HSSFCellStyle cellStyleTitle, Object[] contents, Boolean isFlag) {
		for (int i = 0; i < contents.length; i++) {
			HSSFCell cell = row.createCell(i);
			if (isFlag) {
				cellStyleTitle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);// 设置前景填充样式
				cellStyleTitle.setFillForegroundColor(HSSFColor.RED.index);// 前景填充色
				cell.setCellStyle(cellStyleTitle);
			}
			if (i == 5 || i == 6) {
				cell.setCellValue(Double.parseDouble((String) contents[i]));
			} else {
				cell.setCellValue(new HSSFRichTextString(null != contents[i] ? contents[i].toString() : ""));
			}
			cell = row.createCell(i + 1);
		}
		return row;
	}

	public HttpServletResponse setResponse(HttpServletResponse response, String fileName) {
		try {
			fileName = new String(fileName.getBytes("GBK"), "iso8859-1");
			response.reset();
			response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xls");// 指定下载的文件名
			response.setContentType("application/msexcel");
			response.setHeader("Pragma", "no-cache");
			response.setHeader("Cache-Control", "no-cache");
			response.setDateHeader("Expires", 0);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("setResponse异常：" + e.getMessage());
		}
		return response;
	}
}
