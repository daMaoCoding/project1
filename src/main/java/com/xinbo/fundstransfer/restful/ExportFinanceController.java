package com.xinbo.fundstransfer.restful;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.xinbo.fundstransfer.component.common.TimeChangeCommon;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.FinInStatMatch;
import com.xinbo.fundstransfer.domain.entity.FinOutStatFlow;
import com.xinbo.fundstransfer.domain.entity.FinOutStatSys;
import com.xinbo.fundstransfer.domain.entity.FinTransStatMatch;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.entity.SysUserProfile;
import com.xinbo.fundstransfer.domain.enums.BankLogStatus;
import com.xinbo.fundstransfer.domain.enums.BankLogType;
import com.xinbo.fundstransfer.service.AccountStatisticsService;
import com.xinbo.fundstransfer.service.FinInStatisticsService;
import com.xinbo.fundstransfer.service.FinTransStatService;
import com.xinbo.fundstransfer.service.HandicapService;
import com.xinbo.fundstransfer.service.SysUserProfileService;

/**
 * 导出
 * 
 * @author Administrator
 *
 */
@RestController
@RequestMapping("/r/export")
public class ExportFinanceController {
	private static final Logger logger = LoggerFactory.getLogger(ExportFinanceController.class);

	@Autowired
	private AccountStatisticsService accountStatisticsService;
	@Autowired
	private FinTransStatService finTransStatService;
	@Autowired
	private FinInStatisticsService finInStatisticsService;
	@Autowired
	private SysUserProfileService sysUserProfileService;
	@Autowired
	private HandicapService handicapService;

	/**
	 * 入款明细导出 系统 空值0
	 * 
	 * @param accountId
	 * @param startAndEndTimeToArray
	 * @param fieldval
	 * @param type
	 * @param request
	 * @param response
	 * @throws JsonProcessingException
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	@RequestMapping("/incomeSys/{accountId}/{startAndEndTimeToArray}/{fieldval}/{type}")
	public void incomeSys(@PathVariable Integer accountId, @PathVariable String[] startAndEndTimeToArray,
			@PathVariable String fieldval, @PathVariable String type, HttpServletRequest request,
			HttpServletResponse response) throws JsonProcessingException {
		try {
			String fileName = "testExcel.xls";
			response = setResponse(response, fileName);
			OutputStream output = response.getOutputStream();
			BufferedOutputStream bufferedOutPut = new BufferedOutputStream(output);
			HSSFWorkbook wb = new HSSFWorkbook();
			// 样式
			HSSFCellStyle cellStyleTitle = wb.createCellStyle();
			cellStyleTitle.setAlignment(HSSFCellStyle.ALIGN_CENTER);
			cellStyleTitle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
			HSSFCellStyle cellStyle = wb.createCellStyle();
			cellStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER);
			cellStyle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
			HSSFFont font = wb.createFont();
			font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
			font.setFontName("宋体");
			font.setFontHeight((short) 200);
			cellStyleTitle.setFont(font);

			HSSFSheet sheet = wb.createSheet();
			// 标题
			HSSFRow rowTitle = sheet.createRow(0);
			String[] titleName = new String[] { "盘口", "类型", "汇出人", "订单号", "金额", "手续费", "时间", "备注" };
			loadRowContents(rowTitle, cellStyleTitle, titleName);
			// 内容
			Map<String, Object> result = findIncomeMacth(accountId, startAndEndTimeToArray, fieldval, type);
			List<Object> arrlist = (List<Object>) result.get("arrlist");
			if (null != arrlist) {
				for (int i = 0; i < arrlist.size(); i++) {
					FinInStatMatch record = (FinInStatMatch) arrlist.get(i);
					HSSFRow row = sheet.createRow(i + 1);
					if (null != record) {
						loadRowContents(row, cellStyle, new String[] { record.getHandicapname(),
								BankLogType.findByType(record.getType()).getMsg(), record.getMemberrealname(),
								record.getOrderno(), record.getAmount() != null ? record.getAmount().toString() : "0.0",
								null != record.getFee() ? record.getFee().toString() : "0.0", record.getCreatetime(),
								record.getRemark() });
					}
				}
			}
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
		} catch (Exception e) {
			logger.error("导出异常", e);
		}

	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> findIncomeMacth(Integer id, String[] startAndEndTimeToArray, String fieldval,
			String type) throws JsonProcessingException {
		try {
			Map<String, String> timeSize = TimeChangeCommon.setTimeSizeByFieldval(startAndEndTimeToArray, fieldval);
			PageRequest pageRequest = new PageRequest(0, 9999, Sort.Direction.ASC, "id");
			Map<String, Object> mapp = finInStatisticsService.findFinInStatMatch(null, timeSize.get("startTime"),
					timeSize.get("endTime"), new BigDecimal("0"), new BigDecimal("0"), id, type, 0, pageRequest);
			Page<Object> page = (Page<Object>) mapp.get("Page");
			Map<String, Object> map = new LinkedHashMap<>();
			if (null != page) {
				List<Object> FinOutMatchList = page.getContent();
				List<FinInStatMatch> arrlist = new ArrayList<FinInStatMatch>();
				for (int i = 0; i < FinOutMatchList.size(); i++) {
					Object[] obj = (Object[]) FinOutMatchList.get(i);
					FinInStatMatch FinInStatMatch = new FinInStatMatch();
					FinInStatMatch.setHandicapname((String) obj[0]);
					if ("thirdparty".equals(type))
						FinInStatMatch.setType(4);
					else
						FinInStatMatch.setType((int) obj[1]);
					FinInStatMatch.setMemberrealname((String) obj[2]);
					FinInStatMatch.setOrderno((String) obj[3]);
					FinInStatMatch.setAmount((BigDecimal) obj[4]);
					FinInStatMatch.setFee((BigDecimal) obj[5]);
					FinInStatMatch.setCreatetime((String) obj[6]);
					FinInStatMatch.setRemark((String) obj[7]);
					arrlist.add(FinInStatMatch);
				}
				map.put("arrlist", arrlist);
				map.put("page", new Paging(page));
				map.put("total", mapp.get("total"));
			}
			return map;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("入款明细》明细查询出错" + e);
			return null;
		}
	}

	/**
	 * 出款明细 系统 空值传0
	 * 
	 * @param account
	 * @param startAndEndTimeToArray
	 * @param fieldval
	 * @param request
	 * @param response
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/exportoutwardSys/{accountId}/{startAndEndTimeToArray}/{fieldval}")
	@SuppressWarnings({ "unchecked", "deprecation" })
	public void exportOutwardSys(@PathVariable Integer accountId, @PathVariable String[] startAndEndTimeToArray,
			@PathVariable String fieldval, HttpServletRequest request, HttpServletResponse response)
			throws JsonProcessingException {
		try {
			String fileName = "testExcel.xls";
			response = setResponse(response, fileName);
			OutputStream output = response.getOutputStream();
			BufferedOutputStream bufferedOutPut = new BufferedOutputStream(output);
			HSSFWorkbook wb = new HSSFWorkbook();
			// 样式
			HSSFCellStyle cellStyleTitle = wb.createCellStyle();
			cellStyleTitle.setAlignment(HSSFCellStyle.ALIGN_CENTER);
			cellStyleTitle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
			HSSFCellStyle cellStyle = wb.createCellStyle();
			cellStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER);
			cellStyle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
			HSSFFont font = wb.createFont();
			font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
			font.setFontName("宋体");
			font.setFontHeight((short) 200);
			cellStyleTitle.setFont(font);

			HSSFSheet sheet = wb.createSheet();
			// 标题
			HSSFRow rowTitle = sheet.createRow(0);
			String[] titleName = new String[] { "盘口", "层级", "会员账号", "收款账号", "金额", "手续费", "出款人", "出款时间" };
			loadRowContents(rowTitle, cellStyleTitle, titleName);
			// 内容
			Map<String, Object> result = outStatSysExport(accountId, startAndEndTimeToArray, fieldval);
			if (null != result) {
				List<Object> arrlist = (List<Object>) result.get("arrlist");
				for (int i = 0; i < arrlist.size(); i++) {
					FinOutStatSys record = (com.xinbo.fundstransfer.domain.entity.FinOutStatSys) arrlist.get(i);
					HSSFRow row = sheet.createRow(i + 1);
					if (null != record) {
						loadRowContents(row, cellStyle,
								new String[] { record.getHandicapname(), record.getLevelname(), record.getMember(),
										record.getAccountname(),
										record.getAmount() != null ? record.getAmount().toString() : "0.0",
										null != record.getFee() ? record.getFee().toString() : "0.0",
										record.getOperatorname(), record.getAsigntime() });
					}
				}
			}
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
		} catch (Exception e) {
			logger.error("导出异常", e);
		}
	}

	/**
	 * 出款系统明细 空参数用0代替
	 * 
	 * @param accountid
	 * @param startAndEndTimeToArray
	 * @param fieldval
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> outStatSysExport(int accountid, String[] startAndEndTimeToArray, String fieldval)
			throws Exception {
		PageRequest pageRequest = new PageRequest(0, 1000000, Sort.Direction.ASC, "amount");
		Map<String, String> timeSize = TimeChangeCommon.setTimeSizeByFieldval(startAndEndTimeToArray, fieldval);
		Map<String, Object> map = new HashMap<String, Object>();
		Map<String, Object> result = new LinkedHashMap<>();
		map = accountStatisticsService.findFinOutStatSys(accountid, null, timeSize.get("startTime"),
				timeSize.get("endTime"), new BigDecimal(1), new BigDecimal(999999), 9999, 9999, pageRequest);
		Page<Object> page = (Page<Object>) map.get("Page");
		if (null != page) {
			List<Object> FinOutStatSysList = page.getContent();
			List<FinOutStatSys> arrlist = new ArrayList<FinOutStatSys>();
			for (int i = 0; i < FinOutStatSysList.size(); i++) {
				Object[] obj = (Object[]) FinOutStatSysList.get(i);
				FinOutStatSys FinOutStatSys = new FinOutStatSys();
				FinOutStatSys.setHandicapname((String) obj[0]);
				FinOutStatSys.setLevelname((String) obj[1]);
				FinOutStatSys.setMember((String) obj[2]);
				FinOutStatSys.setAccountname((String) obj[3]);
				FinOutStatSys.setToaccount((String) obj[4]);
				FinOutStatSys.setAmount((BigDecimal) obj[5]);
				FinOutStatSys.setFee((BigDecimal) obj[6]);
				FinOutStatSys.setOperatorname(obj[7] != null ? (String) obj[7] : "机器");
				FinOutStatSys.setAsigntime((String) obj[8]);
				arrlist.add(FinOutStatSys);
			}
			result.put("arrlist", arrlist);
			result.put("page", new Paging(page));
			result.put("total", map.get("total"));
		}
		return result;
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	@RequestMapping("/exportBankLog/{accountId}/{startAndEndTimeToArray}/{fieldval}")
	public void exportBankLog(@PathVariable Integer accountId, @PathVariable String[] startAndEndTimeToArray,
			@PathVariable String fieldval, HttpServletRequest request, HttpServletResponse response)
			throws JsonProcessingException {
		try {
			String fileName = "testExcel.xls";
			response = setResponse(response, fileName);
			OutputStream output = response.getOutputStream();
			BufferedOutputStream bufferedOutPut = new BufferedOutputStream(output);
			HSSFWorkbook wb = new HSSFWorkbook();
			// 样式
			HSSFCellStyle cellStyleTitle = wb.createCellStyle();
			cellStyleTitle.setAlignment(HSSFCellStyle.ALIGN_CENTER);
			cellStyleTitle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
			HSSFCellStyle cellStyle = wb.createCellStyle();
			cellStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER);
			cellStyle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
			HSSFFont font = wb.createFont();
			font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
			font.setFontName("宋体");
			font.setFontHeight((short) 200);
			cellStyleTitle.setFont(font);

			HSSFSheet sheet = wb.createSheet();
			// 标题
			HSSFRow rowTitle = sheet.createRow(0);
			String[] titleName = new String[] { "收款账号", "状态", "类型", "开户人", "金额", "手续费", "交易时间", "抓取时间" };
			loadRowContents(rowTitle, cellStyleTitle, titleName);
			// 内容
			Map<String, Object> result = FinOutwardBankLog(accountId, startAndEndTimeToArray, fieldval);
			if (null != result) {
				List<Object> arrlist = (List<Object>) result.get("arrlist");
				for (int i = 0; i < arrlist.size(); i++) {
					FinOutStatFlow finOutStatFlow = (com.xinbo.fundstransfer.domain.entity.FinOutStatFlow) arrlist
							.get(i);
					HSSFRow row = sheet.createRow(i + 1);
					if (null != finOutStatFlow) {
						loadRowContents(row, cellStyle,
								new String[] { finOutStatFlow.getToaccount(),
										BankLogStatus.findByStatus(finOutStatFlow.getStatus()).getMsg(),
										BankLogType.findByType(finOutStatFlow.getType()).getMsg(),
										finOutStatFlow.getToaccountowner(), finOutStatFlow.getAmount().toString(),
										finOutStatFlow.getFee().toString(), finOutStatFlow.getTradingtime(),
										finOutStatFlow.getCreatetime() });
					}
				}
			}
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
		} catch (Exception e) {
			logger.error("导出异常", e);
		}

	}

	/**
	 * 查询银行流水
	 * 
	 * @param accountid
	 * @param startAndEndTimeToArray
	 * @param fieldval
	 * @return
	 * @throws JsonProcessingException
	 */
	@SuppressWarnings("unchecked")
	public Map<String, Object> FinOutwardBankLog(Integer accountId, String[] startAndEndTimeToArray, String fieldval)
			throws JsonProcessingException {
		try {
			PageRequest pageRequest = new PageRequest(0, 999999, Sort.Direction.ASC, "id");
			Map<String, String> timeSize = TimeChangeCommon.setTimeSizeByFieldval(startAndEndTimeToArray, fieldval);
			Map<String, Object> mapp = accountStatisticsService.findFinOutStatFlow(accountId, null, null,
					timeSize.get("startTime"), timeSize.get("endTime"), null, null, 9999, -1, pageRequest);
			Map<String, Object> map = new LinkedHashMap<>();
			Page<Object> page = (Page<Object>) mapp.get("Page");
			if (null != page) {
				List<Object> FinOutStatFlowList = page.getContent();
				List<FinOutStatFlow> arrlist = new ArrayList<FinOutStatFlow>();
				for (int i = 0; i < FinOutStatFlowList.size(); i++) {
					Object[] obj = (Object[]) FinOutStatFlowList.get(i);
					FinOutStatFlow FinOutStatFlow = new FinOutStatFlow();
					FinOutStatFlow.setAccount((String) obj[0]);
					FinOutStatFlow.setToaccount((String) obj[1]);
					FinOutStatFlow.setAmount((BigDecimal) obj[2]);
					FinOutStatFlow.setFee((BigDecimal) obj[3]);
					if (obj[4] != null) {
						FinOutStatFlow.setStatus(Integer.parseInt(obj[4].toString()));
					}
					FinOutStatFlow.setType(3);
					FinOutStatFlow.setTransactionno((String) obj[6]);
					FinOutStatFlow.setToaccountowner((String) obj[7]);
					FinOutStatFlow.setTradingtime((String) obj[8]);
					FinOutStatFlow.setId((int) obj[9]);
					FinOutStatFlow.setCreatetime((String) obj[10]);
					arrlist.add(FinOutStatFlow);
				}
				map.put("arrlist", arrlist);
				map.put("page", new Paging(page));
				map.put("total", mapp.get("total"));
			}
			return map;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("调用出款明细账号统计>银行明细Controller查询失败：" + e);
			return null;
		}
	}

	/**
	 * 中转明细
	 * 
	 * @param accountId
	 * @param startAndEndTimeToArray
	 * @param fieldval
	 * @param type
	 * @param serytype
	 * @param request
	 * @param response
	 * @throws JsonProcessingException
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	@RequestMapping("/exportTransitMacth/{accountId}/{startAndEndTimeToArray}/{fieldval}/{type}/{serytype}")
	public void exportTransitMacth(@PathVariable Integer accountId, @PathVariable String[] startAndEndTimeToArray,
			@PathVariable String fieldval, @PathVariable Integer type, @PathVariable String serytype,
			HttpServletRequest request, HttpServletResponse response) throws JsonProcessingException {
		try {
			String fileName = "testExcel.xls";
			response = setResponse(response, fileName);
			OutputStream output = response.getOutputStream();
			BufferedOutputStream bufferedOutPut = new BufferedOutputStream(output);
			HSSFWorkbook wb = new HSSFWorkbook();
			// 样式
			HSSFCellStyle cellStyleTitle = wb.createCellStyle();
			cellStyleTitle.setAlignment(HSSFCellStyle.ALIGN_CENTER);
			cellStyleTitle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
			HSSFCellStyle cellStyle = wb.createCellStyle();
			cellStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER);
			cellStyle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
			HSSFFont font = wb.createFont();
			font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
			font.setFontName("宋体");
			font.setFontHeight((short) 200);
			cellStyleTitle.setFont(font);
			String[] titleName = new String[] {};
			HSSFSheet sheet = wb.createSheet();
			if (serytype.equals("sys")) {
				titleName = new String[] { "订单号", "汇出账号", "金额", "手续费", "时间", "备注" };
				// 标题
				HSSFRow rowTitle = sheet.createRow(0);
				loadRowContents(rowTitle, cellStyleTitle, titleName);
				// 内容
				Map<String, Object> result = FindTransitMacth(accountId, startAndEndTimeToArray, fieldval, type,
						serytype);
				List<Object> arrlist = (List<Object>) result.get("arrlist");
				if (null != arrlist) {
					for (int i = 0; i < arrlist.size(); i++) {
						FinTransStatMatch record = (FinTransStatMatch) arrlist.get(i);
						HSSFRow row = sheet.createRow(i + 1);
						if (null != record) {
							loadRowContents(row, cellStyle,
									new String[] { record.getOrderno(), record.getFromaccountname(),
											record.getAmount() != null ? record.getAmount().toString() : "0.0",
											null != record.getFee() ? record.getFee().toString() : "0.0",
											record.getCreatetime(), record.getRemark() });
						}
					}
				}
			} else if (serytype.equals("bank")) {
				titleName = new String[] { "订单号", "汇出账号", "金额", "手续费", "交易时间", "抓取时间", "备注" };
				// 标题
				HSSFRow rowTitle = sheet.createRow(0);
				loadRowContents(rowTitle, cellStyleTitle, titleName);
				// 内容
				Map<String, Object> result = FindTransitMacth(accountId, startAndEndTimeToArray, fieldval, type,
						serytype);
				List<Object> arrlist = (List<Object>) result.get("arrlist");
				if (null != arrlist) {
					for (int i = 0; i < arrlist.size(); i++) {
						FinTransStatMatch record = (FinTransStatMatch) arrlist.get(i);
						HSSFRow row = sheet.createRow(i + 1);
						if (null != record) {
							loadRowContents(row, cellStyle,
									new String[] { record.getOrderno(), record.getFromaccountname(),
											record.getAmount() != null ? record.getAmount().toString() : "0.0",
											null != record.getFee() ? record.getFee().toString() : "0.0",
											record.getTradingtime(), record.getCreatetime(), record.getRemark() });
						}
					}
				}
			}

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
				logger.error(e.getMessage());
			}
		} catch (Exception e) {
			logger.error("导出异常", e);
		}

	}

	/**
	 * 中转 系统明细和银行明细
	 * 
	 * @param accountid
	 * @param startAndEndTimeToArray
	 * @param fieldval
	 * @param type
	 * @param serytype
	 * @return
	 * @throws JsonProcessingException
	 */
	@SuppressWarnings("unchecked")
	public Map<String, Object> FindTransitMacth(int accountid, String[] startAndEndTimeToArray, String fieldval,
			int type, String serytype) throws JsonProcessingException {
		try {
			PageRequest pageRequest = new PageRequest(0, 99999, Sort.Direction.ASC, "id");
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			Map<String, Object> map = new LinkedHashMap<>();
			Map<String, String> timeSize = TimeChangeCommon.setTimeSizeByFieldval(startAndEndTimeToArray, fieldval);
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
			Map<String, Object> mapp = finTransStatService.finTransStatMatch(null, timeSize.get("startTime"),
					timeSize.get("endTime"), new BigDecimal("0"), new BigDecimal("0"), accountid, type, serytype, 9999,
					0, handicapIds, pageRequest);
			Page<Object> page = (Page<Object>) mapp.get("Page");
			if (null != page) {
				List<Object> FinTransStatMatch = page.getContent();
				List<FinTransStatMatch> arrlist = new ArrayList<FinTransStatMatch>();
				for (int i = 0; i < FinTransStatMatch.size(); i++) {
					Object[] obj = (Object[]) FinTransStatMatch.get(i);
					FinTransStatMatch FinTrans = new FinTransStatMatch();
					FinTrans.setOrderno((String) obj[0]);
					FinTrans.setFromaccountname((String) obj[2]);
					FinTrans.setToaccountname((String) obj[3]);
					FinTrans.setAmount((BigDecimal) obj[4]);
					FinTrans.setFee((BigDecimal) obj[5]);
					FinTrans.setRemark((String) obj[7]);
					if (serytype.equals("sys")) {
						FinTrans.setCreatetime((String) obj[6]);
					} else if (serytype.equals("bank")) {
						FinTrans.setTradingtime((String) obj[6]);
						FinTrans.setCreatetime((String) obj[8]);
					}
					arrlist.add(FinTrans);
				}
				map.put("arrlist", arrlist);
				map.put("page", new Paging(page));
				map.put("total", mapp.get("total"));
			}
			return map;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("中转明细》详情Controller查询失败" + e);
			return null;
		}
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
	 * 行赋值
	 * 
	 * @param row
	 * @param cell
	 * @param contents
	 * @return
	 */
	@SuppressWarnings("unused")
	private HSSFCell loadCellContents(HSSFRow row, HSSFCell cell, HSSFCellStyle cellStyleTitle, Object[] contents) {
		for (int i = 0; i < contents.length; i++) {
			cell.setCellStyle(cellStyleTitle);
			cell.setCellValue(new HSSFRichTextString(null != contents[i] ? contents[i].toString() : ""));
			cell = row.createCell(i + 1);
		}
		return cell;
	}

	public HttpServletResponse setResponse(HttpServletResponse response, String fileName) {
		try {
			fileName = new String(fileName.getBytes("GBK"), "iso8859-1");
			response.reset();
			response.setHeader("Content-Disposition", "attachment;filename=" + fileName);// 指定下载的文件名
			response.setContentType("application/vnd.ms-excel");
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
