package com.xinbo.fundstransfer.restful;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.service.CloudService;

/**
 * 返点管理
 */
@RestController
@RequestMapping("/r/bonus/cloud")
public class BonusController extends BaseController {
	private static final Logger logger = LoggerFactory.getLogger(BonusController.class);
	@Autowired
	private CloudService cloudService;

	@RequestMapping("/totalForEach")
	public String totalForEach(@RequestParam(value = "mobileList") List<String> mobileList)
			throws JsonProcessingException {
		try {
			return mapper.writeValueAsString(cloudService.rBonusTotalForEach(mobileList));
		} catch (Exception e) {
			logger.error("bounus totalForEach Error,", e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/findIncomeLogByAliAcc")
	public String findIncomeLogByAliAcc(@RequestParam(value = "pageNo") Integer pageNo,
			@RequestParam(value = "mobile") String mobile,
			@RequestParam(value = "startAmt", required = false) BigDecimal startAmt,
			@RequestParam(value = "endAmt", required = false) BigDecimal endAmt,
			@RequestParam(value = "startTime", required = false) Long startTime,
			@RequestParam(value = "endTime", required = false) Long endTime,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			return mapper.writeValueAsString(cloudService.rBonusFindIncomeLogByAliAcc(mobile, startAmt, endAmt,
					startTime, endTime, pageSize, pageNo));
		} catch (Exception e) {
			logger.error("bounus findIncomeLogByAliAcc Error,", e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/findIncomeLogByWecAcc")
	public String findIncomeLogByWecAcc(@RequestParam(value = "pageNo") Integer pageNo,
			@RequestParam(value = "mobile") String mobile,
			@RequestParam(value = "startAmt", required = false) BigDecimal startAmt,
			@RequestParam(value = "endAmt", required = false) BigDecimal endAmt,
			@RequestParam(value = "startTime", required = false) Long startTime,
			@RequestParam(value = "endTime", required = false) Long endTime,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			return mapper.writeValueAsString(cloudService.rBonusFindIncomeLogByWecAcc(mobile, startAmt, endAmt,
					startTime, endTime, pageSize, pageNo));
		} catch (Exception e) {
			logger.error("bounus findIncomeLogByWecAcc Error,", e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/bonus")
	public String bonus(@RequestParam(value = "pageNo") Integer pageNo, @RequestParam(value = "mobile") String mobile,
			@RequestParam(value = "startAmt", required = false) BigDecimal startAmt,
			@RequestParam(value = "endAmt", required = false) BigDecimal endAmt,
			@RequestParam(value = "startTime", required = false) Long startTime,
			@RequestParam(value = "endTime", required = false) Long endTime,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			return mapper.writeValueAsString(
					cloudService.rBonusBonus(mobile, startAmt, endAmt, startTime, endTime, pageSize, pageNo));
		} catch (Exception e) {
			logger.error("bounus bonus Error,", e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getLocalizedMessage()));
		}
	}
}
