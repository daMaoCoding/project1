package com.xinbo.fundstransfer.restful;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.accountfee.exception.NoSuiteAccountFeeRuleException;
import com.xinbo.fundstransfer.accountfee.pojo.AccountFeeCalResult;
import com.xinbo.fundstransfer.accountfee.service.AccountFeeService;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.SplitOrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@Slf4j
@RequestMapping("/r/out")
public class SplitOrderController {

	@Autowired
	ObjectMapper objectMapper;
	@Autowired
	SplitOrderService service;
	@Autowired
	AccountService accountService;

	@Autowired
	private AccountFeeService accountFeeService;

	private static final String SUCCESS_OK = "OK";

	/** 计算手续费 */
	@GetMapping("/dynamicFee")
	public String dynamicFee(@RequestParam(value = "thirdId") Integer thirdId,
			@RequestParam(value = "amount") BigDecimal amount) throws JsonProcessingException {
		SysUser user = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(user)) {
			objectMapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请登录"));
		}
		AccountBaseInfo baseInfoThird = accountService.getFromCacheById(thirdId);
		if (null == baseInfoThird) {
			return objectMapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "三方账号信息不存在!"));
		}
		String fee = null;
		try {
			BizAccount account = accountService.getById(baseInfoThird.getId());
			AccountFeeCalResult result = accountFeeService.calAccountFee(account, amount);
			log.debug("calAccountFee动态获取手续费结果:{}", result);
			if (null != result) {
				fee = result.getFee().toString();
			}
		} catch (NoSuiteAccountFeeRuleException e) {
			log.error("获取手续费异常NoSuiteAccountFeeRuleException:", e);
			return objectMapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getMessage()));
		} catch (Exception e) {
			log.error("获取手续费异常:", e);
		}
		GeneralResponseData<String> res = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "计算手续费成功!");
		res.setData(fee);
		return objectMapper.writeValueAsString(res);
	}

	@GetMapping("/splitList")
	public String list(@RequestParam(value = "orderNo") String orderNo) throws JsonProcessingException {
		SysUser user = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(user)) {
			objectMapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请登录"));
		}
		List<Map<String, String>> list = service.splitList(orderNo, user.getId());
		GeneralResponseData<List<Map<String, String>>> res = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
		res.setData(list);
		return objectMapper.writeValueAsString(res);

	}

	/**
	 * 拆单 输入数字拆单
	 * 
	 * @param splitNum
	 * @param orderNo
	 * @return
	 * @throws JsonProcessingException
	 */
	@GetMapping("/split")
	public String split(@RequestParam(value = "num") Integer splitNum, @RequestParam(value = "orderNo") String orderNo)
			throws JsonProcessingException {
		SysUser user = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(user)) {
			objectMapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请登录"));
		}
		if (!service.accessibleSplit(user.getId())) {
			return objectMapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "没有设定第三方账号"));
		}
		String res = splitNum > 0 ? service.splitOrder(orderNo, splitNum, user.getId())
				: service.cancelSplit(orderNo, user.getId());
		service.saveHistoryFee(orderNo, null, null, false);
		if (!SUCCESS_OK.equals(res)) {
			return objectMapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), res));
		} else {
			return objectMapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "拆分成功!"));
		}
	}

	@GetMapping("/resetFinished")
	public String resetFinished(@RequestParam(value = "orderNo") String orderNo,
			@RequestParam(value = "subOrder") String subOrder) throws JsonProcessingException {
		SysUser user = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(user)) {
			return objectMapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请登录"));
		}
		String res = service.resetFinished(orderNo, subOrder, user.getId());
		if (SUCCESS_OK.equals(res))
			return objectMapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功!"));
		else
			return objectMapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), res));
	}

	@GetMapping("/checkFinishSub")
	public String checkFinishSub(@RequestParam(value = "thirdId") Integer thirdId,
			@RequestParam(value = "orderNo") String orderNo, @RequestParam(value = "subOrderNo") String subOrderNo,
			@RequestParam(value = "amount") BigDecimal amount, @RequestParam(value = "fee") BigDecimal fee)
			throws JsonProcessingException {
		SysUser user = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(user)) {
			return objectMapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请登录"));
		}
		log.debug("用户:{} 点击完成校验金额 参数:thirdId {},orderNo {} ,subOrderNo {},amount {},fee {}", user.getUid(), thirdId,
				orderNo, subOrderNo, amount, fee);
		String res = service.checkFeeAndOrderAmount(thirdId, orderNo, user.getId(), subOrderNo, amount, fee);
		if (!SUCCESS_OK.equals(res)) {
			return objectMapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), res));
		} else {
			return objectMapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功!"));
		}
	}

	/**
	 * 完成出款 或者 重新出款 子订单
	 * 
	 * @param thirdId
	 * @param orderNo
	 * @param subOrderNo
	 * @param amount
	 * @return
	 * @throws JsonProcessingException
	 */
	@GetMapping("/finishSub")
	public String finishSub(@RequestParam(value = "thirdId") Integer thirdId,
			@RequestParam(value = "orderNo") String orderNo, @RequestParam(value = "subOrderNo") String subOrderNo,
			@RequestParam(value = "amount") BigDecimal amount, @RequestParam(value = "fee") BigDecimal fee)
			throws JsonProcessingException {
		SysUser user = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(user)) {
			return objectMapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请登录"));
		}
		log.debug("用户:{} 点击完成 参数:thirdId {},orderNo {} ,subOrderNo {},amount {},fee {}", user.getUid(), thirdId,
				orderNo, subOrderNo, amount, fee);
		String res = service.updateSubOrderFinish(thirdId, orderNo, user.getId(), subOrderNo, amount, fee);
		if (!SUCCESS_OK.equals(res)) {
			return objectMapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), res));
		} else {
			return objectMapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功!"));
		}
	}

	/**
	 * 拆单 删除 或者增加
	 * 
	 * @param subOrder
	 * @param orderNo
	 * @return
	 * @throws JsonProcessingException
	 */
	@GetMapping("/updateSplit")
	public String updateSplit(@RequestParam(value = "subOrder", required = false) String subOrder,
			@RequestParam(value = "orderNo") String orderNo, @RequestParam(value = "type") Byte type)
			throws JsonProcessingException {
		SysUser user = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(user)) {
			return objectMapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请登录"));
		}
		if (!service.accessibleSplit(user.getId())) {
			return objectMapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "没有设定第三方账号"));
		}
		String res = service.updateSplit(subOrder, orderNo, user.getId(), type);
		if (!SUCCESS_OK.equals(res)) {
			return objectMapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), res));
		} else {
			return objectMapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功!"));
		}
	}
}
