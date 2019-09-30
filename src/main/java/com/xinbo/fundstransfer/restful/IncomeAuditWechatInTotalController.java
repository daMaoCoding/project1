package com.xinbo.fundstransfer.restful;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.xinbo.fundstransfer.service.IncomeAuditWechatTotalInService;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.BizWechatLog;
import com.xinbo.fundstransfer.domain.entity.BizWechatRequest;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import java.util.Map;

/**
 * 微信入款请求接口
 * 
 * @author 007
 *
 */
@RestController
@RequestMapping("/r/IncomeAuditWechatTotalIn")
public class IncomeAuditWechatInTotalController extends BaseController {
	@Autowired
	private IncomeAuditWechatTotalInService incomeAuditWechatTotalInService;
	@Autowired
	private HandicapService handicapService;
	@Autowired
	private AccountService accountService;
	@Autowired
	private ObjectMapper mapper = new ObjectMapper();
	private static final Logger logger = LoggerFactory.getLogger(IncomeAuditWechatInTotalController.class);

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
	@RequestMapping("/findWechatTotalLogByWechar")
	public String findWechatLogByWechar(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "handicap", required = false) int handicap,
			@RequestParam(value = "WechatTotalNumber", required = false) String wechatNumber,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE);
			String startTime = startAndEndTimeToArray[0];
			String endTime = startAndEndTimeToArray[1];
			Map<String, Object> mapp = incomeAuditWechatTotalInService.statisticalWechatLog(handicap, wechatNumber,
					startTime, endTime, pageRequest);
			Page<Object> page = (Page<Object>) mapp.get("Page");
			List<Object> wechatLogList = (List<Object>) page.getContent();
			List<BizWechatLog> arrlist = new ArrayList<BizWechatLog>();
			for (int i = 0; i < wechatLogList.size(); i++) {
				Object[] obj = (Object[]) wechatLogList.get(i);
				BizHandicap bizHandicap = handicapService.findFromCacheById(null == obj[2] ? 0 : (int) obj[2]);
				AccountBaseInfo bizAccount = accountService.getFromCacheById((int) obj[0]);
				BizWechatLog wechatLog = new BizWechatLog();
				wechatLog.setFromAccount((int) obj[0]);
				wechatLog.setCounts(new Integer(String.valueOf(obj[1])));
				wechatLog.setHandicapId((int) obj[2]);
				wechatLog.setHandicapName(bizHandicap.getName());
				wechatLog.setAccount((String) obj[3]);
				wechatLog.setOwner(bizAccount.getOwner());
				wechatLog.setAccountStatus(bizAccount.getStatus());
				arrlist.add(wechatLog);
			}
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("arrlist", arrlist);
			map.put("page", new Paging(page));
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("微信入款请求接口Controller 查询发生错误" + e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
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
			@RequestParam(value = "WechatTotalId", required = false) int wechatId,
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
			Map<String, Object> mapp = incomeAuditWechatTotalInService.findMBAndInvoice(wechatId, startTime, endTime,
					member, orderNo, fromAmount, toAmount, payer, invoicePageRequest, banklogPageRequest);
			Page<Object> invoiceDataToPage = (Page<Object>) mapp.get("invoiceDataToPage");
			Map<String, Object> map = new LinkedHashMap<>();
			if (null != invoiceDataToPage) {
				List<Object> invoiceList = (List<Object>) invoiceDataToPage.getContent();
				List<BizWechatRequest> arrlist = new ArrayList<BizWechatRequest>();
				for (int i = 0; i < invoiceList.size(); i++) {
					Object[] obj = (Object[]) invoiceList.get(i);
					BizHandicap bizHandicap = handicapService.findFromCacheById(null == obj[3] ? 0 : (int) obj[3]);
					AccountBaseInfo bizAccount = accountService.getFromCacheById((Integer) obj[1]);
					BizWechatRequest wechatRequest = new BizWechatRequest();
					wechatRequest.setId((int) obj[0]);
					wechatRequest.setHandicap((int) obj[3]);
					wechatRequest.setHandicapName(bizHandicap.getName());
					wechatRequest.setMemberName((String) obj[10]);
					wechatRequest.setMemberId((String) obj[11]);
					wechatRequest.setOrderNo((String) obj[8]);
					wechatRequest.setAmount((BigDecimal) obj[5]);
					wechatRequest.setWechatid((int) obj[1]);
					wechatRequest.setAccount(bizAccount.getAccount());
					wechatRequest.setCreateTime((String) obj[6]);
					wechatRequest.setRemark(StringUtils.trim(null == (String) obj[7] ? (String) obj[7]
							: StringUtils.trim((String) obj[7]).replace("\r\n", "<br>").replace("\n", "<br>")));
					arrlist.add(wechatRequest);
				}
				map.put("invoiceArrlist", arrlist);
				map.put("invoiceDataToPage", new Paging(invoiceDataToPage));
				map.put("invoiceTotal", mapp.get("invoiceTotal"));
			}
			Page<Object> BankLogDataToPage = (Page<Object>) mapp.get("BankLogDataToPage");
			if (null != BankLogDataToPage) {
				SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				List<Object> bankLogList = (List<Object>) BankLogDataToPage.getContent();
				List<BizWechatLog> bankLogArrlist = new ArrayList<BizWechatLog>();
				for (int i = 0; i < bankLogList.size(); i++) {
					Object[] obj = (Object[]) bankLogList.get(i);
					BizWechatLog wechatLog = new BizWechatLog();
					wechatLog.setId((int) obj[0]);
					wechatLog.setTrTime((String) obj[3]);
					wechatLog.setAmount((BigDecimal) obj[4]);
					wechatLog.setFromAccount((int) obj[1]);
					wechatLog.setRemark(null == (String) obj[6] ? (String) obj[6]
							: StringUtils.trim((String) obj[6]).replace("\r\n", "<br>").replace("\n", "<br>"));
					wechatLog.setSummary((String) obj[7]);
					wechatLog.setDepositor((String) obj[8]);
					wechatLog.setCrTime((String) obj[9]);
					bankLogArrlist.add(wechatLog);
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
	@RequestMapping("/findWechatTotalMatched")
	public String findWechatMatched(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "handicap", required = false) int handicap,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "member", required = false) String member,
			@RequestParam(value = "orderNo", required = false) String orderNo,
			@RequestParam(value = "fromAmount", required = false) BigDecimal fromAmount,
			@RequestParam(value = "toAmount", required = false) BigDecimal toAmount,
			@RequestParam(value = "payer", required = false) String payer,
			@RequestParam(value = "WechatTotalNumber", required = false) String wechatNumber,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE);
			String startTime = startAndEndTimeToArray[0];
			String endTime = startAndEndTimeToArray[1];
			fromAmount = null == fromAmount ? new BigDecimal(0) : fromAmount;
			toAmount = null == toAmount ? new BigDecimal(0) : toAmount;
			Map<String, Object> mapp = incomeAuditWechatTotalInService.findWechatMatched(handicap, startTime, endTime,
					member, orderNo, fromAmount, toAmount, payer, wechatNumber, pageRequest);
			Page<Object> page = (Page<Object>) mapp.get("Page");
			List<Object> wechatLogList = (List<Object>) page.getContent();
			List<BizWechatRequest> arrlist = new ArrayList<BizWechatRequest>();
			for (int i = 0; i < wechatLogList.size(); i++) {
				Object[] obj = (Object[]) wechatLogList.get(i);
				BizHandicap bizHandicap = handicapService.findFromCacheById(null == obj[1] ? 0 : (int) obj[1]);
				AccountBaseInfo bizAccount = accountService.getFromCacheById((Integer) obj[6]);
				BizWechatRequest wechatRequest = new BizWechatRequest();
				wechatRequest.setId((int) obj[0]);
				wechatRequest.setHandicap((int) obj[1]);
				wechatRequest.setHandicapName(bizHandicap.getName());
				wechatRequest.setMemberName((String) obj[2]);
				wechatRequest.setOrderNo((String) obj[3]);
				wechatRequest.setAmount((BigDecimal) obj[4]);
				wechatRequest.setDepositor((String) obj[5]);
				wechatRequest.setWechatid((int) obj[6]);
				wechatRequest.setAccount(bizAccount.getAccount());
				wechatRequest.setUpdateTime((String) obj[7]);
				wechatRequest.setRemark(null == (String) obj[8] ? (String) obj[8]
						: StringUtils.trim((String) obj[8]).replace("\r\n", "<br>").replace("\n", "<br>"));
				wechatRequest.setCreateTime((String) obj[9]);
				arrlist.add(wechatRequest);
			}
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("arrlist", arrlist);
			map.put("page", new Paging(page));
			map.put("total", mapp.get("total"));
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("微信入款已经匹配的数据接口Controller 查询发生错误" + e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
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
	@RequestMapping("/findWechatTotalCanceled")
	public String findWechatCanceled(@RequestParam(value = "pageNo") int pageNo,
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
			Map<String, Object> mapp = incomeAuditWechatTotalInService.findWechatCanceled(handicap, startTime, endTime,
					member, orderNo, fromAmount, toAmount, pageRequest);
			Page<Object> page = (Page<Object>) mapp.get("Page");
			List<Object> wechatLogList = (List<Object>) page.getContent();
			List<BizWechatRequest> arrlist = new ArrayList<BizWechatRequest>();
			for (int i = 0; i < wechatLogList.size(); i++) {
				Object[] obj = (Object[]) wechatLogList.get(i);
				BizHandicap bizHandicap = handicapService.findFromCacheById(null == obj[1] ? 0 : (int) obj[1]);
				AccountBaseInfo bizAccount = accountService.getFromCacheById((Integer) obj[5]);
				BizWechatRequest wechatRequest = new BizWechatRequest();
				wechatRequest.setId((int) obj[0]);
				wechatRequest.setHandicap((int) obj[1]);
				wechatRequest.setHandicapName(bizHandicap.getName());
				wechatRequest.setMemberName((String) obj[2]);
				wechatRequest.setOrderNo((String) obj[3]);
				wechatRequest.setAmount((BigDecimal) obj[4]);
				wechatRequest.setWechatid((int) obj[5]);
				wechatRequest.setAccount(bizAccount.getAccount());
				wechatRequest.setUpdateTime((String) obj[6]);
				wechatRequest.setRemark(null == (String) obj[7] ? (String) obj[7]
						: StringUtils.trim((String) obj[7]).replace("\r\n", "<br>").replace("\n", "<br>"));
				arrlist.add(wechatRequest);
			}
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("arrlist", arrlist);
			map.put("page", new Paging(page));
			map.put("total", mapp.get("total"));
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("微信入款已经取消的数据接口Controller 查询发生错误" + e);
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
	 * @param wechatNo
	 * @param fromAmount
	 * @param toAmount
	 * @param pageSize
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequiresPermissions("IncomeAuditWechatIn:*")
	@RequestMapping("/findWechatTotalUnClaim")
	public String findWechatUnClaim(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "handicap", required = false) int handicap,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "member", required = false) String member,
			@RequestParam(value = "WechatTotalNo", required = false) String wechatNo,
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
			Map<String, Object> mapp = incomeAuditWechatTotalInService.findWechatUnClaim(handicap, startTime, endTime,
					member, wechatNo, fromAmount, toAmount, pageRequest);
			Page<Object> page = (Page<Object>) mapp.get("Page");
			List<Object> wechatLogList = (List<Object>) page.getContent();
			List<BizWechatLog> arrlist = new ArrayList<BizWechatLog>();
			SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			for (int i = 0; i < wechatLogList.size(); i++) {
				Object[] obj = (Object[]) wechatLogList.get(i);
				BizHandicap bizHandicap = handicapService.findFromCacheById(null == obj[10] ? 0 : (int) obj[10]);
				AccountBaseInfo bizAccount = accountService.getFromCacheById((Integer) obj[1]);
				BizWechatLog wechatLog = new BizWechatLog();
				wechatLog.setId((int) obj[0]);
				wechatLog.setTrTime((String) obj[3]);
				wechatLog.setAmount((BigDecimal) obj[4]);
				wechatLog.setFromAccount((int) obj[1]);
				wechatLog.setRemark(StringUtils.trim(null == (String) obj[6] ? (String) obj[6]
						: StringUtils.trim((String) obj[6]).replace("\r\n", "<br>").replace("\n", "<br>")));
				wechatLog.setDepositor((String) obj[8]);
				wechatLog.setCrTime((String) obj[9]);
				wechatLog.setHandicapName(bizHandicap.getName());
				wechatLog.setAccount(bizAccount.getAccount());
				arrlist.add(wechatLog);
			}
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("arrlist", arrlist);
			map.put("page", new Paging(page));
			map.put("total", mapp.get("total"));
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("微信入款已经取消的数据接口Controller 查询发生错误" + e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

}
