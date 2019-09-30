package com.xinbo.fundstransfer.restful;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.HandicapService;
import com.xinbo.fundstransfer.service.IncomeAuditAliTotalInService;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.BizAliLog;
import com.xinbo.fundstransfer.domain.entity.BizAliRequest;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

/**
 * 支付宝入款请求接口
 * 
 * @author 007
 *
 */
@RestController
@RequestMapping("/r/IncomeAuditAliInTotal")
public class IncomeAuditAliTotalInController extends BaseController {
	@Autowired
	private IncomeAuditAliTotalInService incomeAuditAliTotalInService;
	@Autowired
	private HandicapService handicapService;
	@Autowired
	private AccountService accountService;
	@Autowired
	private ObjectMapper mapper = new ObjectMapper();
	private static final Logger logger = LoggerFactory.getLogger(IncomeAuditAliTotalInController.class);

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
			@RequestParam(value = "handicap", required = false) int handicap,
			@RequestParam(value = "AliNumber", required = false) String AliNumber,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE);
			String startTime = startAndEndTimeToArray[0];
			String endTime = startAndEndTimeToArray[1];
			Map<String, Object> mapp = incomeAuditAliTotalInService.statisticalAliLog(handicap, AliNumber, startTime,
					endTime, pageRequest);
			Page<Object> page = (Page<Object>) mapp.get("Page");
			List<Object> AliLogList = (List<Object>) page.getContent();
			List<BizAliLog> arrlist = new ArrayList<BizAliLog>();
			for (int i = 0; i < AliLogList.size(); i++) {
				Object[] obj = (Object[]) AliLogList.get(i);
				BizHandicap bizHandicap = handicapService.findFromCacheById(null == obj[2] ? 0 : (int) obj[2]);
				AccountBaseInfo bizAccount = accountService.getFromCacheById((int) obj[0]);
				BizAliLog AliLog = new BizAliLog();
				AliLog.setFromAccount((int) obj[0]);
				AliLog.setCounts(new Integer(String.valueOf(obj[1])));
				AliLog.setHandicapId((int) obj[2]);
				AliLog.setHandicapName(bizHandicap.getName());
				AliLog.setAccount((String) obj[3]);
				AliLog.setOwner(bizAccount.getOwner());
				AliLog.setAccountStatus(bizAccount.getStatus());
				arrlist.add(AliLog);
			}
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("arrlist", arrlist);
			map.put("page", new Paging(page));
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("支付宝入款请求接口Controller 查询发生错误" + e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
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
			@RequestParam(value = "AliId", required = false) int AliId,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "member", required = false) String member,
			@RequestParam(value = "orderNo", required = false) String orderNo,
			@RequestParam(value = "fromAmount", required = false) BigDecimal fromAmount,
			@RequestParam(value = "toAmount", required = false) BigDecimal toAmount,
			@RequestParam(value = "payer", required = false) String payer,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest invoicePageRequest = new PageRequest(invoicePageNo,
					pageSize != null ? pageSize : AppConstants.PAGE_SIZE);
			PageRequest banklogPageRequest = new PageRequest(invoicePageNo,
					pageSize != null ? pageSize : AppConstants.PAGE_SIZE);
			String startTime = startAndEndTimeToArray[0];
			String endTime = startAndEndTimeToArray[1];
			fromAmount = null == fromAmount ? new BigDecimal(0) : fromAmount;
			toAmount = null == toAmount ? new BigDecimal(0) : toAmount;
			Map<String, Object> mapp = incomeAuditAliTotalInService.findMBAndInvoice(AliId, startTime, endTime, member,
					orderNo, fromAmount, toAmount, payer, invoicePageRequest, banklogPageRequest);
			Page<Object> invoiceDataToPage = (Page<Object>) mapp.get("invoiceDataToPage");
			Map<String, Object> map = new LinkedHashMap<>();
			if (null != invoiceDataToPage) {
				List<Object> invoiceList = (List<Object>) invoiceDataToPage.getContent();
				List<BizAliRequest> arrlist = new ArrayList<BizAliRequest>();
				for (int i = 0; i < invoiceList.size(); i++) {
					Object[] obj = (Object[]) invoiceList.get(i);
					BizHandicap bizHandicap = handicapService.findFromCacheById(null == obj[3] ? 0 : (int) obj[3]);
					AccountBaseInfo bizAccount = accountService.getFromCacheById((Integer) obj[1]);
					BizAliRequest AliRequest = new BizAliRequest();
					AliRequest.setId((int) obj[0]);
					AliRequest.setHandicap((int) obj[3]);
					AliRequest.setHandicapName(bizHandicap.getName());
					AliRequest.setMemberName((String) obj[10]);
					AliRequest.setMemberId((String) obj[11]);
					AliRequest.setOrderNo((String) obj[8]);
					AliRequest.setAmount((BigDecimal) obj[5]);
					AliRequest.setAliPayid((int) obj[1]);
					AliRequest.setAccount(bizAccount.getAccount());
					AliRequest.setCreateTime((String) obj[6]);
					AliRequest.setRemark(StringUtils.trim(null == (String) obj[7] ? (String) obj[7]
							: StringUtils.trim((String) obj[7]).replace("\r\n", "<br>").replace("\n", "<br>")));
					arrlist.add(AliRequest);
				}
				map.put("invoiceArrlist", arrlist);
				map.put("invoiceDataToPage", new Paging(invoiceDataToPage));
				map.put("invoiceTotal", mapp.get("invoiceTotal"));
			}
			Page<Object> BankLogDataToPage = (Page<Object>) mapp.get("BankLogDataToPage");
			if (null != BankLogDataToPage) {
				SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				List<Object> bankLogList = (List<Object>) BankLogDataToPage.getContent();
				List<BizAliLog> bankLogArrlist = new ArrayList<BizAliLog>();
				for (int i = 0; i < bankLogList.size(); i++) {
					Object[] obj = (Object[]) bankLogList.get(i);
					BizAliLog AliLog = new BizAliLog();
					AliLog.setId((int) obj[0]);
					AliLog.setTrTime((String) obj[3]);
					AliLog.setAmount((BigDecimal) obj[4]);
					AliLog.setFromAccount((int) obj[1]);
					AliLog.setRemark(StringUtils.trim(null == (String) obj[6] ? (String) obj[6]
							: StringUtils.trim((String) obj[6]).replace("\r\n", "<br>").replace("\n", "<br>")));
					AliLog.setSummary((String) obj[7]);
					AliLog.setDepositor((String) obj[8]);
					AliLog.setCrTime((String) obj[9]);
					bankLogArrlist.add(AliLog);
				}
				map.put("bankLogArrlist", bankLogArrlist);
				map.put("bankLogDataToPage", new Paging(BankLogDataToPage));
				map.put("bankLogTotal", mapp.get("BankLogTotal"));
			}
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("查询流水和入款单接口Controller 查询发生错误" + e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
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
			@RequestParam(value = "handicap", required = false) int handicap,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "member", required = false) String member,
			@RequestParam(value = "orderNo", required = false) String orderNo,
			@RequestParam(value = "fromAmount", required = false) BigDecimal fromAmount,
			@RequestParam(value = "toAmount", required = false) BigDecimal toAmount,
			@RequestParam(value = "payer", required = false) String payer,
			@RequestParam(value = "AliNumber", required = false) String AliNumber,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE);
			String startTime = startAndEndTimeToArray[0];
			String endTime = startAndEndTimeToArray[1];
			fromAmount = null == fromAmount ? new BigDecimal(0) : fromAmount;
			toAmount = null == toAmount ? new BigDecimal(0) : toAmount;
			Map<String, Object> mapp = incomeAuditAliTotalInService.findAliMatched(handicap, startTime, endTime, member,
					orderNo, fromAmount, toAmount, payer, AliNumber, pageRequest);
			Page<Object> page = (Page<Object>) mapp.get("Page");
			List<Object> AliLogList = (List<Object>) page.getContent();
			List<BizAliRequest> arrlist = new ArrayList<BizAliRequest>();
			for (int i = 0; i < AliLogList.size(); i++) {
				Object[] obj = (Object[]) AliLogList.get(i);
				BizHandicap bizHandicap = handicapService.findFromCacheById(null == obj[1] ? 0 : (int) obj[1]);
				AccountBaseInfo bizAccount = accountService.getFromCacheById((Integer) obj[6]);
				BizAliRequest AliRequest = new BizAliRequest();
				AliRequest.setId((int) obj[0]);
				AliRequest.setHandicap((int) obj[1]);
				AliRequest.setHandicapName(bizHandicap.getName());
				AliRequest.setMemberName((String) obj[2]);
				AliRequest.setOrderNo((String) obj[3]);
				AliRequest.setAmount((BigDecimal) obj[4]);
				AliRequest.setDepositor((String) obj[5]);
				AliRequest.setAliPayid((int) obj[6]);
				AliRequest.setAccount(bizAccount.getAccount());
				AliRequest.setUpdateTime((String) obj[7]);
				AliRequest.setRemark(StringUtils.trim(null == (String) obj[8] ? (String) obj[8]
						: StringUtils.trim((String) obj[8]).replace("\r\n", "<br>").replace("\n", "<br>")));
				AliRequest.setCreateTime((String) obj[9]);
				arrlist.add(AliRequest);
			}
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("arrlist", arrlist);
			map.put("page", new Paging(page));
			map.put("total", mapp.get("total"));
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("支付宝入款已经匹配的数据接口Controller 查询发生错误" + e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
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
			@RequestParam(value = "handicap", required = false) int handicap,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "member", required = false) String member,
			@RequestParam(value = "orderNo", required = false) String orderNo,
			@RequestParam(value = "fromAmount", required = false) BigDecimal fromAmount,
			@RequestParam(value = "toAmount", required = false) BigDecimal toAmount,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE);
			String startTime = startAndEndTimeToArray[0];
			String endTime = startAndEndTimeToArray[1];
			fromAmount = null == fromAmount ? new BigDecimal(0) : fromAmount;
			toAmount = null == toAmount ? new BigDecimal(0) : toAmount;
			Map<String, Object> mapp = incomeAuditAliTotalInService.findAliCanceled(handicap, startTime, endTime,
					member, orderNo, fromAmount, toAmount, pageRequest);
			Page<Object> page = (Page<Object>) mapp.get("Page");
			List<Object> AliLogList = (List<Object>) page.getContent();
			List<BizAliRequest> arrlist = new ArrayList<BizAliRequest>();
			for (int i = 0; i < AliLogList.size(); i++) {
				Object[] obj = (Object[]) AliLogList.get(i);
				BizHandicap bizHandicap = handicapService.findFromCacheById(null == obj[1] ? 0 : (int) obj[1]);
				AccountBaseInfo bizAccount = accountService.getFromCacheById((Integer) obj[5]);
				BizAliRequest AliRequest = new BizAliRequest();
				AliRequest.setId((int) obj[0]);
				AliRequest.setHandicap((int) obj[1]);
				AliRequest.setHandicapName(bizHandicap.getName());
				AliRequest.setMemberName((String) obj[2]);
				AliRequest.setOrderNo((String) obj[3]);
				AliRequest.setAmount((BigDecimal) obj[4]);
				AliRequest.setAliPayid((int) obj[5]);
				AliRequest.setAccount(bizAccount.getAccount());
				AliRequest.setUpdateTime((String) obj[6]);
				AliRequest.setRemark(StringUtils.trim(null == (String) obj[7] ? (String) obj[7]
						: StringUtils.trim((String) obj[7]).replace("\r\n", "<br>").replace("\n", "<br>")));
				arrlist.add(AliRequest);
			}
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("arrlist", arrlist);
			map.put("page", new Paging(page));
			map.put("total", mapp.get("total"));
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("支付宝入款已经取消的数据接口Controller 查询发生错误" + e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
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
			@RequestParam(value = "handicap", required = false) int handicap,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "member", required = false) String member,
			@RequestParam(value = "AliNo", required = false) String AliNo,
			@RequestParam(value = "fromAmount", required = false) BigDecimal fromAmount,
			@RequestParam(value = "toAmount", required = false) BigDecimal toAmount,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE);
			String startTime = null;
			String endTime = null;
			if (startAndEndTimeToArray.length > 0) {
				startTime = startAndEndTimeToArray[0];
				endTime = startAndEndTimeToArray[1];
			}
			fromAmount = null == fromAmount ? new BigDecimal(0) : fromAmount;
			toAmount = null == toAmount ? new BigDecimal(0) : toAmount;
			Map<String, Object> mapp = incomeAuditAliTotalInService.findAliUnClaim(handicap, startTime, endTime, member,
					AliNo, fromAmount, toAmount, pageRequest);
			Page<Object> page = (Page<Object>) mapp.get("Page");
			List<Object> AliLogList = (List<Object>) page.getContent();
			List<BizAliLog> arrlist = new ArrayList<BizAliLog>();
			SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			for (int i = 0; i < AliLogList.size(); i++) {
				Object[] obj = (Object[]) AliLogList.get(i);
				BizHandicap bizHandicap = handicapService.findFromCacheById(null == obj[10] ? 0 : (int) obj[10]);
				AccountBaseInfo bizAccount = accountService.getFromCacheById((Integer) obj[1]);
				BizAliLog AliLog = new BizAliLog();
				AliLog.setId((int) obj[0]);
				AliLog.setTrTime((String) obj[3]);
				AliLog.setAmount((BigDecimal) obj[4]);
				AliLog.setFromAccount((int) obj[1]);
				AliLog.setRemark(StringUtils.trim(null == (String) obj[6] ? (String) obj[6]
						: StringUtils.trim((String) obj[6]).replace("\r\n", "<br>").replace("\n", "<br>")));
				AliLog.setDepositor((String) obj[8]);
				AliLog.setCrTime((String) obj[9]);
				AliLog.setHandicapName(bizHandicap.getName());
				AliLog.setAccount(bizAccount.getAccount());
				arrlist.add(AliLog);
			}
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("arrlist", arrlist);
			map.put("page", new Paging(page));
			map.put("total", mapp.get("total"));
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("支付宝入款已经取消的数据接口Controller 查询发生错误" + e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}
}
