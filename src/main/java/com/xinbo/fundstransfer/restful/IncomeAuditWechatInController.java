package com.xinbo.fundstransfer.restful;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.service.IncomeAuditWechatInService;
import com.xinbo.fundstransfer.service.SysDataPermissionService;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import java.util.Map;
import java.util.Random;

/**
 * 微信入款请求接口
 * 
 * @author 007
 *
 */
@RestController
@RequestMapping("/r/IncomeAuditWechatIn")
public class IncomeAuditWechatInController extends BaseController {
	@Autowired
	private IncomeAuditWechatInService incomeAuditWechatInService;
	@Autowired
	private SysDataPermissionService sysDataPermissionService;
	@Autowired
	private ObjectMapper mapper = new ObjectMapper();
	private static final Logger logger = LoggerFactory.getLogger(IncomeAuditWechatInController.class);

	/** 获取当前用户拥有的盘口code */
	private List<String> getHandicapIdByCurrentUser(String handicap, SysUser sysUser) {
		List<String> handicapList = new ArrayList<>();
		if (sysUser == null) {
			return handicapList;
		}
		if (handicap == null) {
			List<BizHandicap> list = sysDataPermissionService.getHandicapByUserId(sysUser);
			if (list != null && list.size() > 0) {
				list.stream().forEach(p -> handicapList.add(p.getCode()));
			}
		} else {
			handicapList.add(handicap);
		}
		return handicapList;
	}

	/**
	 * 根据参数统计入款微信未匹配的流水
	 * 
	 * @param pageNo
	 * @param handicap
	 * @param wechatNumber
	 * @param startAndEndTimeToArray
	 * @param pageSize
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequiresPermissions("IncomeAuditWechatIn:*")
	@RequestMapping("/findWechatLogByWechar")
	public String findWechatLogByWechar(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "handicap", required = false) String handicap,
			@RequestParam(value = "wechatNumber", required = false) String wechatNumber,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			String startTime = startAndEndTimeToArray[0];
			String endTime = startAndEndTimeToArray[1];
			String pageSise = pageSize != null ? pageSize.toString() : Integer.toString(AppConstants.PAGE_SIZE);
			SysUser loginUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			handicap = handicap.equals("0") ? null : handicap;
			List<String> handicapList = getHandicapIdByCurrentUser(handicap, loginUser);
			Object object = incomeAuditWechatInService.statisticalWechatLog(Integer.toString(pageNo), handicapList,
					wechatNumber, startTime, endTime, pageSise);
			return mapper.writeValueAsString(object);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("微信入款请求接口Controller 查询发生错误" + e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 根据入款账号查询流水和入款单
	 * 
	 * @param invoicePageNo
	 * @param banklogPageNo
	 * @param wechatId
	 * @param startAndEndTimeToArray
	 * @param member
	 * @param orderNo
	 * @param fromAmount
	 * @param toAmount
	 * @param payer
	 * @param pageSize
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequiresPermissions("IncomeAuditWechatIn:*")
	@RequestMapping("/findMBAndInvoice")
	public String findMBAndInvoice(@RequestParam(value = "invoicePageNo") int invoicePageNo,
			@RequestParam(value = "banklogPageNo") int banklogPageNo,
			@RequestParam(value = "account", required = false) String account,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "member", required = false) String member,
			@RequestParam(value = "orderNo", required = false) String orderNo,
			@RequestParam(value = "fromAmount", required = false) BigDecimal fromAmount,
			@RequestParam(value = "toAmount", required = false) BigDecimal toAmount,
			@RequestParam(value = "payer", required = false) String payer,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			String pageSise = pageSize != null ? pageSize.toString() : Integer.toString(AppConstants.PAGE_SIZE);
			String startTime = startAndEndTimeToArray[0];
			String endTime = startAndEndTimeToArray[1];
			fromAmount = null == fromAmount ? new BigDecimal(0) : fromAmount;
			toAmount = null == toAmount ? new BigDecimal(0) : toAmount;
			Object object = incomeAuditWechatInService.findMBAndInvoice(account, startTime, endTime, member, orderNo,
					fromAmount, toAmount, payer, Integer.toString(invoicePageNo), Integer.toString(banklogPageNo),
					pageSise);
			return mapper.writeValueAsString(object);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("查询微信流水和入款单接口Controller 查询发生错误" + e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 微信入款已经匹配的数据
	 * 
	 * @param pageNo
	 * @param handicap
	 * @param startAndEndTimeToArray
	 * @param member
	 * @param orderNo
	 * @param fromAmount
	 * @param toAmount
	 * @param payer
	 * @param wechatNumber
	 * @param pageSize
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequiresPermissions("IncomeAuditWechatIn:*")
	@RequestMapping("/findWechatMatched")
	public String findWechatMatched(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "handicap", required = false) String handicap,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "member", required = false) String member,
			@RequestParam(value = "orderNo", required = false) String orderNo,
			@RequestParam(value = "fromAmount", required = false) BigDecimal fromAmount,
			@RequestParam(value = "toAmount", required = false) BigDecimal toAmount,
			@RequestParam(value = "payer", required = false) String payer,
			@RequestParam(value = "wechatNumber", required = false) String wechatNumber,
			@RequestParam(value = "status", required = false) int status,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			String pageSise = pageSize != null ? pageSize.toString() : Integer.toString(AppConstants.PAGE_SIZE);
			String startTime = startAndEndTimeToArray[0];
			String endTime = startAndEndTimeToArray[1];
			fromAmount = null == fromAmount ? new BigDecimal(0) : fromAmount;
			toAmount = null == toAmount ? new BigDecimal(0) : toAmount;
			SysUser loginUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			handicap = handicap.equals("0") ? null : handicap;
			List<String> handicapList = getHandicapIdByCurrentUser(handicap, loginUser);
			Object object = incomeAuditWechatInService.findWechatMatched(pageNo, handicapList, startTime, endTime,
					member, orderNo, fromAmount, toAmount, wechatNumber, status, pageSise);
			return mapper.writeValueAsString(object);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("微信入款已经匹配的数据接口Controller 查询发生错误" + e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 查询微信入款取消的单
	 * 
	 * @param pageNo
	 * @param handicap
	 * @param startAndEndTimeToArray
	 * @param member
	 * @param orderNo
	 * @param fromAmount
	 * @param toAmount
	 * @param pageSize
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequiresPermissions("IncomeAuditWechatIn:*")
	@RequestMapping("/findWechatCanceled")
	public String findWechatCanceled(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "handicap", required = false) String handicap,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "member", required = false) String member,
			@RequestParam(value = "orderNo", required = false) String orderNo,
			@RequestParam(value = "fromAmount", required = false) BigDecimal fromAmount,
			@RequestParam(value = "toAmount", required = false) BigDecimal toAmount,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			String pageSise = pageSize != null ? pageSize.toString() : Integer.toString(AppConstants.PAGE_SIZE);
			String startTime = startAndEndTimeToArray[0];
			String endTime = startAndEndTimeToArray[1];
			fromAmount = null == fromAmount ? new BigDecimal(0) : fromAmount;
			toAmount = null == toAmount ? new BigDecimal(0) : toAmount;
			SysUser loginUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			handicap = handicap.equals("0") ? null : handicap;
			List<String> handicapList = getHandicapIdByCurrentUser(handicap, loginUser);
			Object object = incomeAuditWechatInService.findWechatCanceled(pageNo, handicapList, startTime, endTime,
					member, orderNo, fromAmount, toAmount, pageSise);
			return mapper.writeValueAsString(object);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("微信入款已经取消的数据接口Controller 查询发生错误" + e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 查询未认领的流水
	 * 
	 * @param pageNo
	 * @param handicap
	 * @param startAndEndTimeToArray
	 * @param member
	 * @param wechatNo
	 * @param fromAmount
	 * @param toAmount
	 * @param pageSize
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequiresPermissions("IncomeAuditWechatIn:*")
	@RequestMapping("/findWechatUnClaim")
	public String findWechatUnClaim(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "handicap", required = false) String handicap,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "member", required = false) String member,
			@RequestParam(value = "wechatNo", required = false) String wechatNo,
			@RequestParam(value = "fromAmount", required = false) BigDecimal fromAmount,
			@RequestParam(value = "toAmount", required = false) BigDecimal toAmount,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			String pageSise = pageSize != null ? pageSize.toString() : Integer.toString(AppConstants.PAGE_SIZE);
			String startTime = startAndEndTimeToArray[0];
			String endTime = startAndEndTimeToArray[1];
			fromAmount = null == fromAmount ? new BigDecimal(0) : fromAmount;
			toAmount = null == toAmount ? new BigDecimal(0) : toAmount;
			SysUser loginUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			handicap = handicap.equals("0") ? null : handicap;
			List<String> handicapList = getHandicapIdByCurrentUser(handicap, loginUser);
			Object object = incomeAuditWechatInService.findWechatUnClaim(pageNo, handicapList, startTime, endTime,
					wechatNo, fromAmount, toAmount, pageSise);
			return mapper.writeValueAsString(object);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("微信入款已经取消的数据接口Controller 查询发生错误" + e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 微信入款手动匹配操作
	 * 
	 * @param sysRequestId
	 * @param bankFlowId
	 * @param matchRemark
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequiresPermissions("IncomeAuditWechatIn:*")
	@RequestMapping("/wechatInMatch")
	public String wechatInMatch(@RequestParam(value = "sysRequestId", required = false) int sysRequestId,
			@RequestParam(value = "bankFlowId", required = false) int bankFlowId,
			@RequestParam(value = "matchRemark", required = false) String matchRemark) throws JsonProcessingException {
		try {
			SysUser loginUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			Object object = incomeAuditWechatInService.wechatAck(sysRequestId, bankFlowId, matchRemark,
					loginUser.getUsername());
			return mapper.writeValueAsString(object);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Wecaht 入款手动匹配操作接口Controller 查询发生错误" + e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 客服添加备注
	 */
	@RequestMapping("/customerAddRemark")
	public String customerAddRemark(@RequestParam(value = "id") Long id, @RequestParam(value = "remark") String remark,
			@RequestParam(value = "type") String type) throws JsonProcessingException {
		GeneralResponseData<List<Map<String, Object>>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "添加备注成功");
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆！"));
		}
		try {
			if (id == null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"参数异常，请联系技术");
				return mapper.writeValueAsString(responseData);
			}
			if (remark == null || "".equals(remark)) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请填写备注信息");
				return mapper.writeValueAsString(responseData);
			}
			if ("invoice".equals(type)) {
				Object object = incomeAuditWechatInService.updateRemarkById(id, remark, type, sysUser.getUid());
				return mapper.writeValueAsString(object);
			} else if ("wechatlog".equals(type)) {
				Object object = incomeAuditWechatInService.updateWecahtLogRemarkById(id, remark, type,
						sysUser.getUid());
				return mapper.writeValueAsString(object);
			}
		} catch (Exception e) {
			logger.error("添加备注失败:{}", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "添加备注失败");
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 取消操作 隐藏操作
	 */
	@RequestMapping("/reject2Platform")
	public String reject2Platform(@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "incomeRequestId") int incomeRequestId,
			@RequestParam(value = "remark", required = false) String remark,
			@RequestParam(value = "orderNo", required = false) String orderNo,
			@RequestParam(value = "handicap", required = false) String handicap) throws JsonProcessingException {
		GeneralResponseData<String> responseData;
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		try {
			if (StringUtils.isNotBlank(type) && "cancel".equals(type)) {
				if (StringUtils.isBlank(remark)) {
					responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
							"请填写备注!");
					return mapper.writeValueAsString(responseData);
				}
				Object object = incomeAuditWechatInService.cancelAndCallFlatform(incomeRequestId, handicap, orderNo,
						remark, sysUser.getUid());
				return mapper.writeValueAsString(object);
			} else {

				Calendar calendar = Calendar.getInstance();
				calendar.setTime(new Date());
				calendar.set(Calendar.HOUR_OF_DAY, 0);
				calendar.set(Calendar.MINUTE, new Random().nextInt(10));
				calendar.set(Calendar.SECOND, new Random().nextInt(59));
				Date zero = calendar.getTime();
				Object object = incomeAuditWechatInService.updateTimeById(incomeRequestId, zero);
				return mapper.writeValueAsString(object);
			}
		} catch (Exception e) {
			logger.error("Wecaht操作失败，异常:{},调用取消/隐藏操作结束,参数:order {}", e, orderNo);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败");
			return mapper.writeValueAsString(responseData);
		}
	}

	/**
	 * 微信补提单
	 */
	@RequestMapping("/generateWecahtRequestOrder")
	public String generateWecahtRequestOrder(
			@RequestParam(value = "memberAccount", required = false) String memberAccount,
			@RequestParam(value = "amount") BigDecimal amount,
			@RequestParam(value = "account", required = false) String account,
			@RequestParam(value = "remark", required = false) String remark,
			@RequestParam(value = "createTime", required = false) String createTime,
			@RequestParam(value = "handicap", required = false) String handicap,
			@RequestParam(value = "bankLogId", required = false) int bankLogId) throws JsonProcessingException {
		GeneralResponseData<String> responseData;
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		try {
			if (StringUtils.isBlank(memberAccount)) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"请填写会员账号!");
				return mapper.writeValueAsString(responseData);
			}
			Object object = incomeAuditWechatInService.generateWecahtRequestOrder(memberAccount, amount, account,
					remark, createTime, bankLogId, handicap, sysUser.getUid());
			return mapper.writeValueAsString(object);
		} catch (Exception e) {
			logger.error("Wecaht操作失败，异常:{},补提单失败,参数:memberAccount {}", e, memberAccount);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败");
			return mapper.writeValueAsString(responseData);
		}
	}

}
