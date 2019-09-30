package com.xinbo.fundstransfer.restful;

import java.math.BigDecimal;
import java.util.*;

import javax.validation.Valid;

import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.domain.enums.OutwardTaskStatus;
import com.xinbo.fundstransfer.domain.enums.TransactionLogType;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.service.*;
import org.apache.shiro.SecurityUtils;
import org.springframework.util.CollectionUtils;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.domain.DynamicSpecifications;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.entity.BizIncomeRequest;
import com.xinbo.fundstransfer.domain.entity.BizOutwardRequest;
import com.xinbo.fundstransfer.domain.entity.BizOutwardTask;
import com.xinbo.fundstransfer.domain.entity.BizTransactionLog;
import com.xinbo.fundstransfer.domain.entity.SysUser;

/**
 * 匹配数据
 *
 * @author Eden
 *
 */
@RestController
@RequestMapping("/r/match")
public class TransactionLogController extends BaseController {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(AccountController.class);
	@Autowired
	private TransactionLogService transactionService;
	@Autowired
	private AccountService accountService;
	@Autowired
	private IncomeRequestService incomeService;
	@Autowired
	private SysUserService userService;
	@Autowired
	private OutwardTaskService outwardTaskService;
	@Autowired
	private OutwardRequestService outwardRequestService;
	private ObjectMapper mapper = new ObjectMapper();

	@RequestMapping("/findInOutByLogId")
	public String findInOutByLogId(@RequestParam(value = "logId") Long logId) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			Map<String, Object> data = new HashMap<>();
			BizTransactionLog tInfo = transactionService.findByFromBanklogId(logId);
			if (tInfo == null) {
				tInfo = transactionService.findByToBanklogId(logId);
			}
			if (null != tInfo && null != tInfo.getOrderId()) {
				// 匹配数据只有fromAccountId有值时，说明是出款匹配，只有toAccountId有值
				// 是入款匹配，都有时，是系统中转匹配
				if (tInfo.getFromAccount() != 0 && (tInfo.getToAccount() == null || tInfo.getToAccount() == 0)) {
					// 出款卡
					BizOutwardTask bizOutwardTask = outwardTaskService.findById(tInfo.getOrderId());
					if (null != bizOutwardTask.getStatus()) {
						bizOutwardTask
								.setStatusStr(OutwardTaskStatus.findByStatus(bizOutwardTask.getStatus()).getMsg());
					}
					if (null != bizOutwardTask.getOperator()) {
						SysUser operator = userService.findFromCacheById(bizOutwardTask.getOperator());
						if (null != operator) {
							bizOutwardTask.setOperatorUid(operator.getUid());
						}
					}
					data.put("outwardTask", bizOutwardTask);
					if (null != bizOutwardTask.getOutwardRequestId()) {
						BizOutwardRequest outwardRequest = outwardRequestService
								.get(bizOutwardTask.getOutwardRequestId());
						data.put("outwardRequest", outwardRequest);
						if (null != outwardRequest.getReviewer()) {
							SysUser reviewer = userService.findFromCacheById(outwardRequest.getReviewer());
							if (null != reviewer) {
								outwardRequest.setReviewerUid(reviewer.getUid());
							}
						}
					}
				} else {
					// 入款卡 系统中转
					BizIncomeRequest incomeInfo = incomeService.get(tInfo.getOrderId());
					data.put("incomeInfo", incomeInfo);
				}
			}
			responseData.setData(data);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/findbyfrom")
	public String findByFrom(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize, @Valid BizTransactionLog transLog,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "minAmount", required = false) BigDecimal minAmount,
			@RequestParam(value = "maxAmount", required = false) BigDecimal maxAmount) throws JsonProcessingException {
		try {
			pageSize = pageSize == null ? AppConstants.PAGE_SIZE : pageSize;
			Date[] startEndTime = CommonUtils.parseStartAndEndTime(startAndEndTimeToArray);
			List<SearchFilter> filterList = DynamicSpecifications.build(request);
			if (transLog.getId() != null) {
				filterList.add(new SearchFilter("id", SearchFilter.Operator.EQ, transLog.getId()));
			}
			if (transLog.getFromAccount() != 0) {
				filterList.add(new SearchFilter("fromAccount", SearchFilter.Operator.EQ, transLog.getFromAccount()));
			}
			if (transLog.getToAccount() != null && transLog.getToAccount() != 0) {
				filterList.add(new SearchFilter("toAccount", SearchFilter.Operator.EQ, transLog.getToAccount()));
			}
			if (transLog.getType() != null) {
				filterList.add(new SearchFilter("type", SearchFilter.Operator.EQ, transLog.getType()));
			}
			if (startEndTime[0] != null) {
				filterList.add(new SearchFilter("createTime", SearchFilter.Operator.GTE, startEndTime[0]));
			}
			if (startEndTime[1] != null) {
				filterList.add(new SearchFilter("createTime", SearchFilter.Operator.LTE, startEndTime[1]));
			}
			if (minAmount != null) {
				filterList.add(new SearchFilter("amount", SearchFilter.Operator.GTE, minAmount));
			}
			if (maxAmount != null) {
				filterList.add(new SearchFilter("amount", SearchFilter.Operator.LTE, maxAmount));
			}
			PageRequest pageRequest = new PageRequest(pageNo, pageSize, Sort.Direction.DESC, "createTime");
			SearchFilter[] filterArray = filterList.toArray(new SearchFilter[filterList.size()]);
			Specification<BizTransactionLog> specif = DynamicSpecifications.build(request, BizTransactionLog.class,
					filterArray);
			Page<BizTransactionLog> page = transactionService.findAll(specif, pageRequest);
			page = buildOrderVo(page);
			for (BizTransactionLog temp : page.getContent()) {
				if (temp.getConfirmor() != null) {
					temp.setConfirmorUid(userService.findFromCacheById(temp.getConfirmor()).getUid());
				}
				if (temp.getFromAccount() != 0) {
					AccountBaseInfo base = accountService.getFromCacheById(temp.getFromAccount());
					if (Objects.nonNull(base)) {
						temp.setFromAccountNO(base.getAccount());
						temp.setFromAlias(base.getAlias());
						temp.setFromBankType(base.getBankType());
						temp.setFromOwner(base.getOwner());
					}
				}
			}
			GeneralResponseData<List<BizTransactionLog>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			responseData.setData(page.getContent());
			responseData.setPage(new Paging(page, buildHeader(filterArray)));
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 人工平账
	 */
	@RequestMapping("/flatBalance")
	public String flatBalance(@Valid BizTransactionLog log) throws JsonProcessingException {
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		logger.info("人工平账=>fromAccount:{} toAccount:{} amount:{} operator:{}", log.getFromAccount(), log.getToAccount(),
				log.getAmount(), sysUser.getUid());
		log.setConfirmor(sysUser.getId());
		log.setOperator(sysUser.getId());
		log.setCreateTime(new Date());
		BizTransactionLog data = transactionService.flatBalance(log);
		GeneralResponseData<BizTransactionLog> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		responseData.setData(data);
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 根据type,orderIdList 获取
	 * 
	 * @param typeNot
	 *            sql where type!=typeNot
	 * @param orderIdIn
	 *            sql where orderId in (orderIdIn);
	 */
	@RequestMapping("/findByTypeNotAndOrderIdIn")
	public String findByTypeNotAndOrderIdIn(@RequestParam("typeNot") Integer typeNot,
			@RequestParam(value = "orderIdIn") Long[] orderIdIn) throws JsonProcessingException {
		List<BizTransactionLog> dataList = transactionService.findByTypeNotAndOrderIdIn(typeNot,
				Arrays.asList(orderIdIn));
		GeneralResponseData<List<BizTransactionLog>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		responseData.setData(dataList);
		return mapper.writeValueAsString(responseData);
	}

	private Map<String, Object> buildHeader(SearchFilter[] filterToArray) {
		Map<String, Object> result = new HashMap<>();
		BigDecimal[] amountAndFeeTotal = transactionService.findAmountAndFeeByTotal(filterToArray);
		result.put("totalFee", amountAndFeeTotal[1]);
		result.put("totalAmount", amountAndFeeTotal[0]);
		return result;
	}

	private Page<BizTransactionLog> buildOrderVo(Page<BizTransactionLog> page) {
		if (page == null || CollectionUtils.isEmpty(page.getContent())) {
			return page;
		}
		List<Long> reqIdList = new ArrayList<>(), outIdList = new ArrayList<>();
		page.getContent().forEach(log -> {
			if (log.getOrderId() != null) {
				if (TransactionLogType.OUTWARD.getType().equals(log.getType())) {
					outIdList.add(log.getOrderId());
				} else {
					reqIdList.add(log.getOrderId());
				}
			}
		});
		Map<Long, BizIncomeRequest> incomeMap = new HashMap<>();
		if (!CollectionUtils.isEmpty(reqIdList)) {
			List<BizIncomeRequest> incomeList = incomeService.findByIdList(reqIdList);
			incomeList.forEach(p -> incomeMap.put(p.getId(), p));
		}
		Map<Long, Map<String, Object>> outMap = new HashMap<>();
		if (!CollectionUtils.isEmpty(outIdList)) {
			List<Map<String, Object>> outList = outwardTaskService.queryInfoByIdList(outIdList);
			outList.forEach(p -> outMap.put(Long.valueOf(p.get("id").toString()), p));
		}
		page.getContent().forEach(log -> {
			if (log.getOrderId() != null) {
				if (TransactionLogType.OUTWARD.getType().equals(log.getType())) {
					log.setOutwardTaskInfo(outMap.get(log.getOrderId()));
				} else {
					log.setIncomeVO(incomeMap.get(log.getOrderId()));
				}
			}
		});
		return page;
	}
}
