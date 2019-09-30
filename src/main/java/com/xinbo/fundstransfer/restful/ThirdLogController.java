package com.xinbo.fundstransfer.restful;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
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
import com.xinbo.fundstransfer.domain.entity.BizThirdLog;
import com.xinbo.fundstransfer.service.ThirdLogService;

@RestController
@RequestMapping("/r/thirdlog")
public class ThirdLogController extends BaseController {

	private static final Logger logger = LoggerFactory.getLogger(ThirdLogController.class);

	@Autowired
	ThirdLogService thirdLogService;

	@RequestMapping("/findbyfrom")
	public String findByFrom(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "searchTypeIn0Out1", required = false) Integer searchTypeIn0Out1,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "minAmount", required = false) BigDecimal minAmount,
			@RequestParam(value = "maxAmount", required = false) BigDecimal maxAmount, @Valid BizThirdLog bizThirdLog)
			throws JsonProcessingException {
		try {
			GeneralResponseData<List<BizThirdLog>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					Sort.Direction.DESC, "id", "tradingTime");
			List<SearchFilter> filterToList = DynamicSpecifications.build(request);
			logger.debug("开始封装查询条件{}", bizThirdLog);
			if (searchTypeIn0Out1 != null && searchTypeIn0Out1 == 0) {
				filterToList.add(new SearchFilter("amount", SearchFilter.Operator.GT, 0));
			} else if (searchTypeIn0Out1 != null && searchTypeIn0Out1 == 1) {
				filterToList.add(new SearchFilter("amount", SearchFilter.Operator.LT, 0));
			}
			if (null != minAmount) {
				filterToList.add(new SearchFilter("amount", SearchFilter.Operator.GTE, minAmount));
			}
			if (null != maxAmount) {
				filterToList.add(new SearchFilter("amount", SearchFilter.Operator.LTE, maxAmount));
			}
			Date[] startAndEndTime = CommonUtils.parseStartAndEndTime(null, startAndEndTimeToArray);
			if (startAndEndTime[0] != null) {
				filterToList.add(new SearchFilter("tradingTime", SearchFilter.Operator.GTE, startAndEndTime[0]));
			}
			if (startAndEndTime[1] != null) {
				filterToList.add(new SearchFilter("tradingTime", SearchFilter.Operator.LTE, startAndEndTime[1]));
			}
			if (null != bizThirdLog.getId()) {
				filterToList.add(new SearchFilter("id", SearchFilter.Operator.EQ, bizThirdLog.getId()));
			}
			if (0 != bizThirdLog.getFromAccount()) {
				filterToList
						.add(new SearchFilter("fromAccount", SearchFilter.Operator.EQ, bizThirdLog.getFromAccount()));
			}
			if (StringUtils.isNotBlank(bizThirdLog.getOrderNo())) {
				filterToList.add(new SearchFilter("orderNo", SearchFilter.Operator.EQ, bizThirdLog.getOrderNo()));
			}
			SearchFilter[] filterToArray = filterToList.toArray(new SearchFilter[filterToList.size()]);
			Specification<BizThirdLog> specification = DynamicSpecifications.build(BizThirdLog.class, filterToArray);
			logger.debug("执行第三方流水查找findAll {}", specification, pageRequest);
			Page<BizThirdLog> page = thirdLogService.findAll(specification, pageRequest);
			Map<String, Object> header = buildHeader(filterToArray);
			responseData.setData(page.getContent());
			responseData.setPage(new Paging(page, header));
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error("第三方流水查找异常" + e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	private Map<String, Object> buildHeader(SearchFilter[] filterToArray) {
		Map<String, Object> result = new HashMap<>();
		logger.debug("流水总计");
		String amount = thirdLogService.findAmountTotal(filterToArray);
		String fee = thirdLogService.findFeeTotal(filterToArray);
		result.put("totalAmount", new BigDecimal(amount));
		result.put("totalFee", new BigDecimal(fee));
		return result;
	}
}
