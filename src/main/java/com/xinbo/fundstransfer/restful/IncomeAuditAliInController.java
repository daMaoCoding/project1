package com.xinbo.fundstransfer.restful;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.service.IncomeAuditAliInService;
import com.xinbo.fundstransfer.service.SysDataPermissionService;
import com.xinbo.fundstransfer.domain.entity.BizAliRequest;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.Random;

/**
 * 支付宝入款请求接口
 * 
 * @author 007
 *
 */
@RestController
@RequestMapping("/r/IncomeAuditAliIn")
public class IncomeAuditAliInController extends BaseController {
	@Autowired
	private IncomeAuditAliInService incomeAuditAliInService;
	@Autowired
	private SysDataPermissionService sysDataPermissionService;
	@Autowired
	private ObjectMapper mapper = new ObjectMapper();
	private static final Logger logger = LoggerFactory.getLogger(IncomeAuditAliInController.class);

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
	 * 根据参数统计入款支付宝未匹配的流水
	 * 
	 * @param pageNo
	 * @param handicap
	 * @param AliNumber
	 * @param startAndEndTimeToArray
	 * @param pageSize
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequiresPermissions("IncomeAuditAliIn:*")
	@RequestMapping("/findAliLogByWechar")
	public String findAliLogByWechar(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "handicap", required = false) String handicap,
			@RequestParam(value = "AliNumber", required = false) String AliNumber,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			SysUser loginUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			String pageSise = pageSize != null ? pageSize.toString() : Integer.toString(AppConstants.PAGE_SIZE);
			String startTime = startAndEndTimeToArray[0];
			String endTime = startAndEndTimeToArray[1];
			handicap = handicap.equals("0") ? null : handicap;
			List<String> handicapList = getHandicapIdByCurrentUser(handicap, loginUser);
			Object object = incomeAuditAliInService.statisticalAliLog(pageNo, handicapList, AliNumber, startTime,
					endTime, pageSise);
			return mapper.writeValueAsString(object);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("支付宝入款请求接口Controller 查询发生错误" + e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 根据入款账号查询流水和入款单
	 * 
	 * @param invoicePageNo
	 * @param banklogPageNo
	 * @param AliId
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
	@RequiresPermissions("IncomeAuditAliIn:*")
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
			Object object = incomeAuditAliInService.findMBAndInvoice(pageSise, account, startTime, endTime, member,
					orderNo, fromAmount, toAmount, payer, invoicePageNo, banklogPageNo);
			return mapper.writeValueAsString(object);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("查询支付宝流水和入款单接口Controller 查询发生错误" + e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 支付宝入款已经匹配的数据
	 * 
	 * @param pageNo
	 * @param handicap
	 * @param startAndEndTimeToArray
	 * @param member
	 * @param orderNo
	 * @param fromAmount
	 * @param toAmount
	 * @param payer
	 * @param AliNumber
	 * @param pageSize
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequiresPermissions("IncomeAuditAliIn:*")
	@RequestMapping("/findAliMatched")
	public String findAliMatched(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "handicap", required = false) String handicap,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "member", required = false) String member,
			@RequestParam(value = "orderNo", required = false) String orderNo,
			@RequestParam(value = "fromAmount", required = false) BigDecimal fromAmount,
			@RequestParam(value = "toAmount", required = false) BigDecimal toAmount,
			@RequestParam(value = "payer", required = false) String payer,
			@RequestParam(value = "AliNumber", required = false) String AliNumber,
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
			Object object = incomeAuditAliInService.findAliMatched(pageNo, handicapList, startTime, endTime, member,
					orderNo, fromAmount, toAmount, payer, AliNumber, status, pageSise);
			return mapper.writeValueAsString(object);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("支付宝入款已经匹配的数据接口Controller 查询发生错误" + e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 查询支付宝入款取消的单
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
	@RequiresPermissions("IncomeAuditAliIn:*")
	@RequestMapping("/findAliCanceled")
	public String findAliCanceled(@RequestParam(value = "pageNo") int pageNo,
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
			Object object = incomeAuditAliInService.findAliCanceled(pageNo, handicapList, startTime, endTime, member,
					orderNo, fromAmount, toAmount, pageSise);
			return mapper.writeValueAsString(object);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("支付宝入款已经取消的数据接口Controller 查询发生错误" + e);
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
	 * @param AliNo
	 * @param fromAmount
	 * @param toAmount
	 * @param pageSize
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequiresPermissions("IncomeAuditAliIn:*")
	@RequestMapping("/findAliUnClaim")
	public String findAliUnClaim(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "handicap", required = false) String handicap,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "member", required = false) String member,
			@RequestParam(value = "AliNo", required = false) String AliNo,
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
			Object object = incomeAuditAliInService.findAliUnClaim(pageNo, handicapList, startTime, endTime, member,
					AliNo, fromAmount, toAmount, pageSise);
			return mapper.writeValueAsString(object);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("支付宝入款已经取消的数据接口Controller 查询发生错误" + e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 支付宝入款手动匹配操作
	 * 
	 * @param sysRequestId
	 * @param bankFlowId
	 * @param matchRemark
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequiresPermissions("IncomeAuditAliIn:*")
	@RequestMapping("/AliInMatch")
	public String AliInMatch(@RequestParam(value = "sysRequestId", required = false) int sysRequestId,
			@RequestParam(value = "bankFlowId", required = false) int bankFlowId,
			@RequestParam(value = "matchRemark", required = false) String matchRemark) throws JsonProcessingException {
		try {
			SysUser loginUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			Object object = incomeAuditAliInService.AliInMatch(sysRequestId, bankFlowId, matchRemark,
					loginUser.getUid());
			return mapper.writeValueAsString(object);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("AliPay 支付宝入款手动匹配操作接口Controller 查询发生错误" + e);
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
				Object object = incomeAuditAliInService.updateRemarkById(id, remark, type, sysUser.getUid());
				return mapper.writeValueAsString(object);
			} else if ("alilog".equals(type)) {
				Object object = incomeAuditAliInService.updateAliPayLogRemarkById(id, remark, type, sysUser.getUid());
				return mapper.writeValueAsString(object);

			}
		} catch (Exception e) {
			logger.error("AliPay 添加备注失败:{}", e);
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
			@RequestParam(value = "handicapId", required = false) String handicap) throws JsonProcessingException {
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
				Object object = incomeAuditAliInService.cancelAndCallFlatform(incomeRequestId, handicap, orderNo,
						remark, sysUser.getUid());
				return mapper.writeValueAsString(object);
			} else {
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(new Date());
				calendar.set(Calendar.HOUR_OF_DAY, 0);
				calendar.set(Calendar.MINUTE, new Random().nextInt(10));
				calendar.set(Calendar.SECOND, new Random().nextInt(59));
				Date zero = calendar.getTime();
				Object object = incomeAuditAliInService.updateTimeById(incomeRequestId, zero);
				return mapper.writeValueAsString(object);
			}
		} catch (Exception e) {
			logger.error("AliPay操作失败，异常:{},调用取消/隐藏操作结束,参数:order {}", e, orderNo);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败");
			return mapper.writeValueAsString(responseData);
		}
	}

	/**
	 * 支付宝补提单
	 */
	@RequestMapping("/generateAliPayRequestOrder")
	public String generateAliPayRequestOrder(
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
			Object object = incomeAuditAliInService.generateAliPayRequestOrder(memberAccount, amount, account, remark,
					createTime, bankLogId, handicap, sysUser.getUid());
			return mapper.writeValueAsString(object);
		} catch (Exception e) {
			logger.error("Wecaht操作失败，异常:{},补提单失败,参数:memberAccount {}", e, memberAccount);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败");
			return mapper.writeValueAsString(responseData);
		}
	}

	/**
	 * 获取正在匹配的支付宝入款单
	 * @throws ParseException 
	 */
	@RequestMapping("/aliIncomeToMatch")
	public String aliIncomeToMatch(@RequestParam(value = "handicap", required = false) Integer handicap,
			@RequestParam(value = "level", required = false) Integer level,
			@RequestParam(value = "incomeMember", required = false) String incomeMember,
			@RequestParam(value = "incomeOrder", required = false) String incomeOrder,
			@RequestParam(value = "timeStart", required = false) Long timeStart,
			@RequestParam(value = "timeEnd", required = false) Long timeEnd,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "pageNo", required = false) Integer pageNo) throws JsonProcessingException, ParseException {
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆！"));
		}
		Sort sort = new Sort(Sort.Direction.DESC, "id");
		PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,sort);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		 Map<String,Object> map =  incomeAuditAliInService.aliIncomeToMatch(pageRequest, handicap, level, incomeMember,
				incomeOrder,sdf.format(new Date(timeStart)),sdf.format(new Date(timeEnd)));
		 Page<Object> page = (Page<Object>) map.get("page");
		double totalAmount =  map.get("totalAmount")==null?0d:((BigDecimal) map.get("totalAmount")).doubleValue();
		List<Object> AliLogList = (List<Object>) page.getContent();
		List<Map<String, Object>> arrlist = new ArrayList<Map<String, Object>>();
		Map<String, Object> AliRequest = null;
		 long nowTime = System.currentTimeMillis();
		for (int i = 0; i < AliLogList.size(); i++) {
			Object[] obj = (Object[]) AliLogList.get(i);
			AliRequest = new HashMap<String, Object>();
			AliRequest.put("id", obj[0]);
			AliRequest.put("handicapName", obj[1]);
			AliRequest.put("levelName", obj[2]);
			AliRequest.put("member_user_name", obj[3]);
			AliRequest.put("order_no", obj[4]);
			AliRequest.put("create_time", obj[5]);
			AliRequest.put("amount", obj[6]);
			AliRequest.put("waitLongTime", nowTime-sdf.parse(obj[5].toString()).getTime());
			arrlist.add(AliRequest);
		}
	GeneralResponseData<List<Map<String, Object>>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		responseData.setData(arrlist);
		responseData.setPage(new Paging(page));
		responseData.setMessage(totalAmount+"");
		return mapper.writeValueAsString(responseData);
		
	}

	/**
	 * 获取失败的支付宝入款单
	 * @throws ParseException 
	 */
	@RequestMapping("/aliIncomeFail")
	public String aliIncomeFail(@RequestParam(value = "handicap", required = false) Integer handicap,
			@RequestParam(value = "level", required = false) Integer level,
			@RequestParam(value = "incomeMember", required = false) String incomeMember,
			@RequestParam(value = "incomeOrder", required = false) String incomeOrder,
			@RequestParam(value = "timeStart", required = false) Long timeStart,
			@RequestParam(value = "timeEnd", required = false) Long timeEnd,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "pageNo", required = false) Integer pageNo) throws JsonProcessingException, ParseException {
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆！"));
		}
		Sort sort = new Sort(Sort.Direction.DESC, "id");
		PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,sort);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		 Map<String,Object> map =  incomeAuditAliInService.aliIncomeFail(pageRequest, handicap, level, incomeMember,
				incomeOrder,sdf.format(new Date(timeStart)),sdf.format(new Date(timeEnd)));
		 Page<Object> page = (Page<Object>) map.get("page");
		double totalAmount =  map.get("totalAmount")==null?0d:((BigDecimal) map.get("totalAmount")).doubleValue();
		List<Object> AliLogList = (List<Object>) page.getContent();
		List<Map<String, Object>> arrlist = new ArrayList<Map<String, Object>>();
		Map<String, Object> AliRequest = null;
		for (int i = 0; i < AliLogList.size(); i++) {
			Object[] obj = (Object[]) AliLogList.get(i);
			AliRequest = new HashMap<String, Object>();
			AliRequest.put("id", obj[0]);
			AliRequest.put("handicapName", obj[1]);
			AliRequest.put("levelName", obj[2]);
			AliRequest.put("member_user_name", obj[3]);
			AliRequest.put("order_no", obj[4]);
			AliRequest.put("create_time", obj[5]);
			AliRequest.put("amount", obj[6]);
			AliRequest.put("waitLongTime", sdf.parse(obj[7].toString()).getTime()-sdf.parse(obj[5].toString()).getTime());
			arrlist.add(AliRequest);
		}
	GeneralResponseData<List<Map<String, Object>>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		responseData.setData(arrlist);
		responseData.setPage(new Paging(page));
		responseData.setMessage(totalAmount+"");
		return mapper.writeValueAsString(responseData);
		
	}
	
	/**
	 * 获取进行中支付宝入款单
	 * @throws ParseException 
	 */
	@RequestMapping("/aliIncomeMatched")
	public String aliIncomeMatched(@RequestParam(value = "handicap", required = false) Integer handicap,
			@RequestParam(value = "level", required = false) Integer level,
			@RequestParam(value = "member", required = false) String member,
			@RequestParam(value = "toMember", required = false) String toMember,
			@RequestParam(value = "inOrderNo", required = false) String inOrderNo,
			@RequestParam(value = "outOrderNo", required = false) String outOrderNo,
			@RequestParam(value = "toHandicapRadio", required = false) Integer toHandicapRadio,
			@RequestParam(value = "timeStart", required = false) Long timeStart,
			@RequestParam(value = "timeEnd", required = false) Long timeEnd,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "pageNo", required = false) Integer pageNo) throws JsonProcessingException, ParseException {
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆！"));
		}
		PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
				Sort.Direction.DESC, "orderAll.create_time");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		 Map<String,Object> map =  incomeAuditAliInService.aliIncomeMatched(pageRequest, handicap, level, member,
				 toMember,inOrderNo,outOrderNo,toHandicapRadio,sdf.format(new Date(timeStart)),sdf.format(new Date(timeEnd)));
		 Page<Object> page = (Page<Object>) map.get("page");
		double totalAmount =  map.get("totalAmount")==null?0d:((BigDecimal) map.get("totalAmount")).doubleValue();
		double totalToAmount =  map.get("totalToAmount")==null?0d:((BigDecimal) map.get("totalToAmount")).doubleValue();
		List<Object> AliLogList = (List<Object>) page.getContent();
		List<Map<String, Object>> arrlist = new ArrayList<Map<String, Object>>();
		Map<String, Object> AliRequest = null;
		 long nowTime = System.currentTimeMillis();
		for (int i = 0; i < AliLogList.size(); i++) {
			Object[] obj = (Object[]) AliLogList.get(i);
			AliRequest = new HashMap<String, Object>();
			AliRequest.put("id", obj[0]);
			AliRequest.put("handicapName", obj[1]);
			AliRequest.put("levelName", obj[2]);
			AliRequest.put("member", obj[3]);
			AliRequest.put("inOrderNo", obj[4]);
			AliRequest.put("createTime", obj[6]);
			AliRequest.put("amount", obj[8]);
			AliRequest.put("toMember", obj[9]);
			AliRequest.put("toOrderNo", obj[10]);
			AliRequest.put("toHandicapName", obj[11]);
			AliRequest.put("toLevelName", obj[12]);
			AliRequest.put("toAmount", obj[13]);
			AliRequest.put("waitLongTime", nowTime-sdf.parse(obj[7].toString()).getTime());
			arrlist.add(AliRequest);
		}
	GeneralResponseData<List<Map<String, Object>>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		responseData.setData(arrlist);
		responseData.setPage(new Paging(page));
		responseData.setMessage(totalAmount+":"+totalToAmount);
		return mapper.writeValueAsString(responseData);
		
	}
	
	/**
	 * 获取成功的支付宝入款单
	 * @throws ParseException 
	 */
	@RequestMapping("/aliIncomeSuccess")
	public String aliIncomeSuccess(@RequestParam(value = "handicap", required = false) Integer handicap,
			@RequestParam(value = "level", required = false) Integer level,
			@RequestParam(value = "member", required = false) String member,
			@RequestParam(value = "toMember", required = false) String toMember,
			@RequestParam(value = "inOrderNo", required = false) String inOrderNo,
			@RequestParam(value = "outOrderNo", required = false) String outOrderNo,
			@RequestParam(value = "toHandicapRadio", required = false) Integer toHandicapRadio,
			@RequestParam(value = "timeStart", required = false) Long timeStart,
			@RequestParam(value = "timeEnd", required = false) Long timeEnd,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "pageNo", required = false) Integer pageNo) throws JsonProcessingException, ParseException {
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆！"));
		}
		PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
				Sort.Direction.DESC, "orderAll.create_time");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		 Map<String,Object> map =  incomeAuditAliInService.aliIncomeSuccess(pageRequest, handicap, level,  member,
				 toMember,inOrderNo,outOrderNo,toHandicapRadio,sdf.format(new Date(timeStart)),sdf.format(new Date(timeEnd)));
		 Page<Object> page = (Page<Object>) map.get("page");
		double totalAmount =  map.get("totalAmount")==null?0d:((BigDecimal) map.get("totalAmount")).doubleValue();
		double totalToAmount =  map.get("totalToAmount")==null?0d:((BigDecimal) map.get("totalToAmount")).doubleValue();
		List<Object> AliLogList = (List<Object>) page.getContent();
		List<Map<String, Object>> arrlist = new ArrayList<Map<String, Object>>();
		Map<String, Object> AliRequest = null;
		for (int i = 0; i < AliLogList.size(); i++) {
			Object[] obj = (Object[]) AliLogList.get(i);
			AliRequest = new HashMap<String, Object>();
			AliRequest.put("id", obj[0]);
			AliRequest.put("handicapName", obj[1]);
			AliRequest.put("levelName", obj[2]);
			AliRequest.put("member", obj[3]);
			AliRequest.put("inOrderNo", obj[4]);
			AliRequest.put("createTime", obj[6]);
			AliRequest.put("finishTime", obj[7]);
			AliRequest.put("amount", obj[8]);
			AliRequest.put("toMember", obj[9]);
			AliRequest.put("toOrderNo", obj[10]);
			AliRequest.put("toHandicapName", obj[11]);
			AliRequest.put("toLevelName", obj[12]);
			AliRequest.put("toAmount", obj[13]);
			AliRequest.put("waitLongTime", sdf.parse(obj[7].toString()).getTime()-sdf.parse(obj[6].toString()).getTime());
			arrlist.add(AliRequest);
		}
	GeneralResponseData<List<Map<String, Object>>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		responseData.setData(arrlist);
		responseData.setPage(new Paging(page));
		responseData.setMessage(totalAmount+":"+totalToAmount);
		return mapper.writeValueAsString(responseData);
		
	}
}
