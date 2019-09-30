package com.xinbo.fundstransfer.restful;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
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
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.component.common.TimeChangeCommon;
import com.xinbo.fundstransfer.domain.DynamicSpecifications;
import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.BizIncomeRequest;
import com.xinbo.fundstransfer.domain.entity.BizOutwardRequest;
import com.xinbo.fundstransfer.domain.entity.BizOutwardTask;
import com.xinbo.fundstransfer.domain.entity.BizTransactionLog;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.entity.SysUserProfile;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.enums.BankLogStatus;
import com.xinbo.fundstransfer.domain.enums.CurrentSystemLevel;
import com.xinbo.fundstransfer.domain.enums.IncomeRequestStatus;
import com.xinbo.fundstransfer.domain.enums.IncomeRequestType;
import com.xinbo.fundstransfer.domain.enums.OutwardRequestStatus;
import com.xinbo.fundstransfer.domain.enums.OutwardTaskStatus;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.BankLogService;
import com.xinbo.fundstransfer.service.ExportAccountService;
import com.xinbo.fundstransfer.service.FinInStatisticsService;
import com.xinbo.fundstransfer.service.HandicapService;
import com.xinbo.fundstransfer.service.IncomeRequestService;
import com.xinbo.fundstransfer.service.OutwardRequestService;
import com.xinbo.fundstransfer.service.OutwardTaskService;
import com.xinbo.fundstransfer.service.SysUserProfileService;
import com.xinbo.fundstransfer.service.SysUserService;
import com.xinbo.fundstransfer.service.TransactionLogService;

/**
 * 导出
 * 
 * @author Administrator
 *
 */
@RestController
@RequestMapping("/r/exportaccount")
public class ExportAccountController {
	private static final Logger logger = LoggerFactory.getLogger(ExportAccountController.class);

	@Autowired
	private AccountService accountService;
	@Autowired
	private IncomeRequestService incomeRequestService;
	@Autowired
	BankLogService bankLogService;
	@Autowired
	private OutwardTaskService outwardTaskService;
	@Autowired
	private OutwardRequestService outwardRequestService;
	@Autowired
	TransactionLogService transactionLogService;
	@Autowired
	private SysUserService userService;
	@Autowired
	FinInStatisticsService finInStatisticsService;
	@Autowired
	ExportAccountService exportAccountService;
	@Autowired
	private HandicapService handicapService;
	@Autowired
	private SysUserProfileService sysUserProfileService;

	/**
	 * 出款明细 系统 空值0
	 * 
	 * @param accountId
	 * @param startAndEndTimeToArray
	 * @param fieldval
	 * @param request
	 * @param response
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/outwardSys/{accountId}/{startAndEndTimeToArray}/{fieldval}")
	public void outwardSys(@PathVariable Integer accountId, @PathVariable String[] startAndEndTimeToArray,
			@PathVariable String fieldval, HttpServletRequest request, HttpServletResponse response)
			throws JsonProcessingException {
		try {
			Map<String, String> excelInfo = new HashMap<String, String>();
			excelInfo = getFileNameAndAccountInfo(accountId, "出款明细");
			response = setResponse(response, excelInfo.get("excelName"));
			OutputStream output = response.getOutputStream();
			BufferedOutputStream bufferedOutPut = new BufferedOutputStream(output);
			HSSFWorkbook wb = new HSSFWorkbook();
			// 样式
			HSSFCellStyle cellStyleTitle = getCellStyleTitle(wb);
			HSSFCellStyle cellStyle = getCellStyle(wb);
			HSSFSheet sheet = wb.createSheet();
			// 查询条件
			List<SearchFilter> filterToList = DynamicSpecifications.build(request);
			filterToList.add(new SearchFilter("accountId", SearchFilter.Operator.EQ, accountId));
			// 日期控制
			Map<String, String> startAndEndTime = TimeChangeCommon.setTimeSizeByFieldval(startAndEndTimeToArray,
					fieldval);
			if (startAndEndTime.get("startTime") != null) {
				filterToList.add(
						new SearchFilter("asignTime", SearchFilter.Operator.GTE, startAndEndTime.get("startTime")));
			}
			if (startAndEndTime.get("endTime") != null) {
				filterToList
						.add(new SearchFilter("asignTime", SearchFilter.Operator.LTE, startAndEndTime.get("endTime")));
			}
			SearchFilter[] filterToArray = filterToList.toArray(new SearchFilter[filterToList.size()]);
			Specification<BizOutwardTask> specif = DynamicSpecifications.build(BizOutwardTask.class, filterToArray);
			Sort sort = new Sort(Sort.Direction.DESC, "asignTime");
			List<BizOutwardTask> otwardTaskList = outwardTaskService.findList(specif, sort);
			// 标题订单号 会员账号 姓名（真实姓名） 提出金额 出款金额 状态（请求的状态 比如已确认） 出款人 备注 时间
			// （任务处理最后更新时间）
			String[] titleName = new String[] { "订单号", "会员账号", "姓名", "提出金额", "出款金额", "请求状态", "任务状态", "出款人", "备注",
					"时间", };
			sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, titleName.length - 1));
			// 合并列 开始行，结束行，开始列，结束列
			HSSFRow rowTitle1 = sheet.createRow(0);
			loadRowContents(rowTitle1, cellStyleTitle, new String[] { excelInfo.get("titleName") });
			HSSFRow rowTitle2 = sheet.createRow(1);
			loadRowContents(rowTitle2, cellStyleTitle, titleName);
			// 数据行
			if (null != otwardTaskList && otwardTaskList.size() > 0) {
				for (int i = 0; i < otwardTaskList.size(); i++) {
					BizOutwardTask record = otwardTaskList.get(i);
					HSSFRow row = sheet.createRow(i + 2);
					if (null != record) {
						BizOutwardRequest outrequest = outwardRequestService.get(record.getOutwardRequestId());
						if (null == outrequest) {
							continue;
						}
						String operatorUid = "机器";
						if (null != record.getOperator()) {
							SysUser operator = userService.findFromCacheById(record.getOperator());
							if (null != operator) {
								operatorUid = operator.getUid();
							}
						}
						loadRowContents(row, cellStyle,
								new String[] { outrequest.getOrderNo(), outrequest.getMember(),
										outrequest.getToAccountOwner(), outrequest.getAmount().toString(),
										record.getAmount().toString(),
										OutwardRequestStatus.findByStatus(outrequest.getStatus()).getMsg(),
										OutwardTaskStatus.findByStatus(record.getStatus()).getMsg(), operatorUid,
										outrequest.getRemark() + record.getRemark(), TimeChangeCommon.TimeStamp2Date(
												outrequest.getUpdateTime().toString(), "yyyy-MM-dd HH:mm:ss") });
					}
				}
			}
			HSSFRow rowFoot = sheet.createRow((otwardTaskList == null ? 0 : otwardTaskList.size()) + 2);
			loadRowContents(rowFoot, cellStyleTitle, new String[] { "总条数：" + otwardTaskList.size() });
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
			logger.error("入款明细导出异常", e);
		}

	}

	@RequestMapping("/incomeIssued/{accountId}/{startAndEndTimeToArray}/{fieldval}")
	public void incomeIssued(@PathVariable Integer accountId, @PathVariable String[] startAndEndTimeToArray,
			@PathVariable String fieldval, HttpServletRequest request, HttpServletResponse response)
			throws JsonProcessingException {
		try {
			PageRequest pageRequest = new PageRequest(0, AppConstants.PAGE_SIZE_MAX, Sort.Direction.ASC, "id");
			Map<String, String> excelInfo = new HashMap<String, String>();
			excelInfo = getFileNameAndAccountInfo(accountId, "下发银行卡明细");
			response = setResponse(response, excelInfo.get("excelName"));
			OutputStream output = response.getOutputStream();
			BufferedOutputStream bufferedOutPut = new BufferedOutputStream(output);
			HSSFWorkbook wb = new HSSFWorkbook();
			// 样式
			HSSFCellStyle cellStyleTitle = getCellStyleTitle(wb);
			HSSFCellStyle cellStyle = getCellStyle(wb);
			HSSFSheet sheet = wb.createSheet();
			// 日期控制
			Map<String, String> startAndEndTime = TimeChangeCommon.setTimeSizeByFieldval(startAndEndTimeToArray,
					fieldval);
			// 查询条件
			Map<String, Object> mapp = finInStatisticsService.findFinInStatMatch(null, startAndEndTime.get("startTime"),
					startAndEndTime.get("endTime"), new BigDecimal("0"), new BigDecimal("0"), accountId, "sendcard", 0,
					pageRequest);
			// 标题
			String[] titleName = new String[] { "账号", "账号名称", "订单号", "金额", "手续费", "时间", "备注" };
			sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, titleName.length - 1));
			// 合并列 开始行，结束行，开始列，结束列
			HSSFRow rowTitle1 = sheet.createRow(0);
			loadRowContents(rowTitle1, cellStyleTitle, new String[] { excelInfo.get("titleName") });
			HSSFRow rowTitle2 = sheet.createRow(1);
			loadRowContents(rowTitle2, cellStyleTitle, titleName);
			// 数据行
			if (null != mapp) {
				Page<Object[]> page = (Page<Object[]>) mapp.get("sendCardPage");
				List<Object[]> incomeList = page.getContent();
				if (null != incomeList && incomeList.size() > 0) {
					for (int i = 0; i < incomeList.size(); i++) {
						Object[] record = incomeList.get(i);
						HSSFRow row = sheet.createRow(i + 2);
						if (null != record) {
							loadRowContents(row, cellStyle,
									new String[] { record[0].toString(), record[1].toString(), record[3].toString(),
											record[4].toString(), record[5].toString(), record[6].toString(),
											record[7].toString() });
						}
					}
				}
				HSSFRow rowFoot = sheet.createRow((incomeList == null ? 0 : incomeList.size()) + 2);
				loadRowContents(rowFoot, cellStyleTitle, new String[] { "总条数：" + incomeList.size() });
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
			logger.error("入款明细导出异常", e);
		}

	}

	/**
	 * 入款明细 系统 空值0
	 * 
	 * @param accountId
	 * @param startAndEndTimeToArray
	 * @param fieldval
	 * @param type
	 * @param request
	 * @param response
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/incomeSys/{accountId}/{startAndEndTimeToArray}/{fieldval}/{operaType}")
	public void incomeSys(@PathVariable String accountId, @PathVariable String[] startAndEndTimeToArray,
			@PathVariable String fieldval, @PathVariable String operaType, HttpServletRequest request,
			HttpServletResponse response) throws JsonProcessingException {
		try {
			Map<String, String> excelInfo = new HashMap<String, String>();
			if (operaType.equals("trans")) {
				excelInfo = getFileNameAndAccountInfo(Integer.parseInt(accountId), "中转明细");
			} else {
				excelInfo = getFileNameAndAccountInfo((accountId.indexOf("8888") == -1 ? Integer.parseInt(accountId)
						: Integer.parseInt(accountId.split(",")[0])), "入款明细");
			}
			response = setResponse(response, excelInfo.get("excelName"));
			OutputStream output = response.getOutputStream();
			BufferedOutputStream bufferedOutPut = new BufferedOutputStream(output);
			HSSFWorkbook wb = new HSSFWorkbook();
			// 样式
			HSSFCellStyle cellStyleTitle = getCellStyleTitle(wb);
			HSSFCellStyle cellStyle = getCellStyle(wb);
			HSSFSheet sheet = wb.createSheet();
			// 查询条件
			List<SearchFilter> filterToList = DynamicSpecifications.build(request);
			if (accountId.indexOf("8888") == -1) {
				filterToList.add(new SearchFilter("toId", SearchFilter.Operator.EQ, accountId));
			} else {
				filterToList.add(new SearchFilter("type", SearchFilter.Operator.EQ, 1));
				filterToList.add(new SearchFilter("handicap", SearchFilter.Operator.EQ, accountId.split(",")[1]));
			}
			// 无需查询已取消的
			List<Integer> status = new ArrayList<Integer>();
			status.add(IncomeRequestStatus.Matching.getStatus());
			status.add(IncomeRequestStatus.Matched.getStatus());
			filterToList.add(new SearchFilter("status", SearchFilter.Operator.IN, status.toArray()));
			// 日期控制
			Map<String, String> startAndEndTime = TimeChangeCommon.setTimeSizeByFieldval(startAndEndTimeToArray,
					fieldval);
			if (startAndEndTime.get("startTime") != null) {
				filterToList.add(
						new SearchFilter("createTime", SearchFilter.Operator.GTE, startAndEndTime.get("startTime")));
			}
			if (startAndEndTime.get("endTime") != null) {
				filterToList
						.add(new SearchFilter("createTime", SearchFilter.Operator.LTE, startAndEndTime.get("endTime")));
			}
			PageRequest pageRequest = new PageRequest(0, AppConstants.PAGE_SIZE_MAX, Sort.Direction.DESC, "createTime",
					"id");
			SearchFilter[] filterToArray = filterToList.toArray(new SearchFilter[filterToList.size()]);
			Specification<BizIncomeRequest> specif = DynamicSpecifications.build(BizIncomeRequest.class, filterToArray);
			Page<BizIncomeRequest> page = incomeRequestService.findAll(specif, pageRequest);
			// // 总计 金额：amountAndFee[0] 手续费：amountAndFee[1]
			// BigDecimal[] amountAndFee =
			// incomeRequestService.findAmountAndFeeByTotal(filterToArray);
			// 标题
			String[] titleName = new String[] { "订单号", operaType.equals("senderCard") ? "账号" : "会员账号",
					operaType.equals("senderCard") ? "账号名称" : "存款人", "存入金额", "盘口", "收款卡银行", "收款卡姓名", "状态", "操作人", "备注",
					"最后更新时间" };
			sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, titleName.length - 1));
			// 合并列 开始行，结束行，开始列，结束列
			HSSFRow rowTitle1 = sheet.createRow(0);
			loadRowContents(rowTitle1, cellStyleTitle, new String[] { excelInfo.get("titleName") });
			HSSFRow rowTitle2 = sheet.createRow(1);
			loadRowContents(rowTitle2, cellStyleTitle, titleName);
			// 数据行
			if (null != page) {
				List<BizIncomeRequest> incomeList = page.getContent();
				if (null != incomeList && incomeList.size() > 0) {
					for (int i = 0; i < incomeList.size(); i++) {
						BizIncomeRequest record = incomeList.get(i);
						HSSFRow row = sheet.createRow(i + 2);
						if (null != record) {
							// 状态名与平台保持一致
							String statusStr = "";
							if (record.getStatus().equals(IncomeRequestStatus.Matching.getStatus())) {
								statusStr = "匹配中";
							} else if (record.getStatus().equals(IncomeRequestStatus.Matched.getStatus())) {
								statusStr = "已匹配";
							} else if (record.getStatus().equals(IncomeRequestStatus.Canceled.getStatus())) {
								statusStr = "已取消";
							}
							// 查找对应匹配信息
							BizTransactionLog tlInfo = transactionLogService.findByOrderIdAndType(record.getId(),
									IncomeRequestType.PlatFromBank.getType());
							String confirmUid = "机器";
							if (null != tlInfo) {
								SysUser confirmor = userService.findFromCacheById(tlInfo.getConfirmor());
								if (null != confirmor) {
									confirmUid = confirmor.getUid();
								}
							}
							BizHandicap bizHandicap = handicapService.findFromCacheById(record.getHandicap());
							loadRowContents(row, cellStyle,
									new String[] { record.getOrderNo(),
											operaType.equals("senderCard") ? record.getFromAccount()
													: record.getMemberUserName(),
											operaType.equals("senderCard") ? record.getFromBankType()
													: record.getMemberRealName(),
											record.getAmount().toString(),
											bizHandicap == null ? "" : bizHandicap.getName(), record.getToBankType(),
											record.getToOwner(), statusStr, confirmUid, record.getRemark(),
											record.getUpdateTime() == null ? ""
													: TimeChangeCommon.TimeStamp2Date(record.getUpdateTime().toString(),
															"yyyy-MM-dd HH:mm:ss") });
						}
					}
				}
				// // 底部总计信息
				// String amountTotal = "0";
				// if (amountAndFee != null && amountAndFee[0] != null) {
				// amountTotal = amountAndFee[0].toString();
				// }
				HSSFRow rowFoot = sheet.createRow((incomeList == null ? 0 : incomeList.size()) + 2);
				// loadRowContents(rowFoot, cellStyleTitle, new String[] {
				// "总条数：" + incomeList.size(), "", "", "", "", amountTotal });
				loadRowContents(rowFoot, cellStyleTitle, new String[] { "总条数：" + incomeList.size() });
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
			logger.error("入款明细导出异常", e);
		}

	}

	/**
	 * 中转明细 系统 空值0
	 * 
	 * @param accountId
	 * @param startAndEndTimeToArray
	 * @param fieldval
	 * @param type
	 * @param request
	 * @param response
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/transSys/{accountId}/{startAndEndTimeToArray}/{fieldval}/{operaType}")
	public void transSys(@PathVariable Integer accountId, @PathVariable String[] startAndEndTimeToArray,
			@PathVariable String fieldval, @PathVariable String operaType, HttpServletRequest request,
			HttpServletResponse response) throws JsonProcessingException {
		try {
			Map<String, String> excelInfo = new HashMap<String, String>();
			if (operaType.equals("trans")) {
				excelInfo = getFileNameAndAccountInfo(accountId, "中转明细");
			} else {
				excelInfo = getFileNameAndAccountInfo(accountId, "入款明细");
			}
			response = setResponse(response, excelInfo.get("excelName"));
			OutputStream output = response.getOutputStream();
			BufferedOutputStream bufferedOutPut = new BufferedOutputStream(output);
			HSSFWorkbook wb = new HSSFWorkbook();
			// 样式
			HSSFCellStyle cellStyleTitle = getCellStyleTitle(wb);
			HSSFCellStyle cellStyle = getCellStyle(wb);
			HSSFSheet sheet = wb.createSheet();
			// 查询条件
			List<SearchFilter> filterToList = DynamicSpecifications.build(request);
			filterToList.add(new SearchFilter("toId", SearchFilter.Operator.EQ, accountId));
			// 无需查询已取消的
			List<Integer> status = new ArrayList<Integer>();
			status.add(IncomeRequestStatus.Matching.getStatus());
			status.add(IncomeRequestStatus.Matched.getStatus());
			filterToList.add(new SearchFilter("status", SearchFilter.Operator.IN, status.toArray()));
			// 日期控制
			Map<String, String> startAndEndTime = TimeChangeCommon.setTimeSizeByFieldval(startAndEndTimeToArray,
					fieldval);
			if (startAndEndTime.get("startTime") != null) {
				filterToList.add(
						new SearchFilter("createTime", SearchFilter.Operator.GTE, startAndEndTime.get("startTime")));
			}
			if (startAndEndTime.get("endTime") != null) {
				filterToList
						.add(new SearchFilter("createTime", SearchFilter.Operator.LTE, startAndEndTime.get("endTime")));
			}
			PageRequest pageRequest = new PageRequest(0, AppConstants.PAGE_SIZE_MAX, Sort.Direction.DESC, "createTime",
					"id");
			SearchFilter[] filterToArray = filterToList.toArray(new SearchFilter[filterToList.size()]);
			Specification<BizIncomeRequest> specif = DynamicSpecifications.build(BizIncomeRequest.class, filterToArray);
			Page<BizIncomeRequest> page = incomeRequestService.findAll(specif, pageRequest);
			// // 总计 金额：amountAndFee[0] 手续费：amountAndFee[1]
			// BigDecimal[] amountAndFee =
			// incomeRequestService.findAmountAndFeeByTotal(filterToArray);
			// 标题
			String[] titleName = new String[] { "盘口", "层级", "存款账号", "会员姓名", "存款人", "金额", "订单号", "创建时间", "更新时间", "状态",
					"备注" };
			sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, titleName.length - 1));
			// 合并列 开始行，结束行，开始列，结束列
			HSSFRow rowTitle1 = sheet.createRow(0);
			loadRowContents(rowTitle1, cellStyleTitle, new String[] { excelInfo.get("titleName") });
			HSSFRow rowTitle2 = sheet.createRow(1);
			loadRowContents(rowTitle2, cellStyleTitle, titleName);
			// 数据行
			if (null != page) {
				List<BizIncomeRequest> incomeList = page.getContent();
				if (null != incomeList && incomeList.size() > 0) {
					for (int i = 0; i < incomeList.size(); i++) {
						BizIncomeRequest record = incomeList.get(i);
						HSSFRow row = sheet.createRow(i + 2);
						if (null != record) {
							loadRowContents(row, cellStyle, new String[] { record.getHandicapName(),
									record.getLevelName(), record.getFromAccount(), record.getMemberUserName(),
									record.getMemberRealName(), record.getAmount().toString(), record.getOrderNo(),
									TimeChangeCommon.TimeStamp2Date(record.getCreateTime().toString(),
											"yyyy-MM-dd HH:mm:ss"),
									record.getUpdateTime() == null ? ""
											: TimeChangeCommon.TimeStamp2Date(record.getUpdateTime().toString(),
													"yyyy-MM-dd HH:mm:ss"),
									IncomeRequestStatus.findByStatus(record.getStatus()).getMsg(),
									record.getRemark() });
						}
					}
				}
				// // 底部总计信息
				// String amountTotal = "0";
				// if (amountAndFee != null && amountAndFee[0] != null) {
				// amountTotal = amountAndFee[0].toString();
				// }
				HSSFRow rowFoot = sheet.createRow((incomeList == null ? 0 : incomeList.size()) + 2);
				// loadRowContents(rowFoot, cellStyleTitle, new String[] {
				// "总条数：" + incomeList.size(), "", "", "", "", amountTotal });
				loadRowContents(rowFoot, cellStyleTitle, new String[] { "总条数：" + incomeList.size() });
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
			logger.error("入款明细导出异常", e);
		}

	}

	// 出款卡银行流水导出
	@RequestMapping("/outBankLog/{accountId}/{startAndEndTimeToArray}/{fieldval}/{bankType}")
	public void outBankLog(@PathVariable Integer accountId, @PathVariable String[] startAndEndTimeToArray,
			@PathVariable String fieldval, @PathVariable String bankType, HttpServletRequest request,
			HttpServletResponse response) throws JsonProcessingException {
		try {
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			Map<String, String> excelInfo = new HashMap<String, String>();
			excelInfo = getFileNameAndAccountInfo(accountId, "银行流水明细");
			response = setResponse(response, excelInfo.get("excelName"));
			OutputStream output = response.getOutputStream();
			BufferedOutputStream bufferedOutPut = new BufferedOutputStream(output);
			HSSFWorkbook wb = new HSSFWorkbook();
			// 样式
			HSSFCellStyle cellStyleTitle = getCellStyleTitle(wb);
			HSSFCellStyle cellStyle = getCellStyle(wb);

			PageRequest pageRequest = new PageRequest(0, AppConstants.PAGE_SIZE_MAX);
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

			Map<String, Object> mapp = accountService.findOutBankLog(startAndEndTimeToArray[0],
					startAndEndTimeToArray[1], bankType.equals("0") ? null : bankType, handicaps, pageRequest);

			// 导出所有银行七点的余额
			if (accountId == -8) {
				mapp = accountService.find7TimeBalance(startAndEndTimeToArray[0], handicaps, pageRequest);
			}
			Page<Object> page = (Page<Object>) mapp.get("Page");
			// 数据行
			if (null != page) {
				List<Object> bankLogList = (List<Object>) page.getContent();
				if (null != bankLogList && bankLogList.size() > 0) {
					int totle = bankLogList.size();// 获取List集合的size
					int mus = 50000;// 每个工作表格最多存储50000条数据（注：excel表格一个工作表可以存储65536条）
					int avg = totle / mus;
					for (int j = 0; j < avg + 1; j++) {
						HSSFSheet sheet = wb.createSheet();
						// 标题与列头
						String[] titleName = null;
						// -2是导出出款卡银行单信息
						// -3是入款卡银行单信息
						if (accountId == -8) {
							titleName = new String[] { "盘口", "银行类别", "名称", "余额", "时间", "编码" };
						} else {
							titleName = new String[] { (accountId == -2 ? "转出账号" : "转入账号"), "盘口", "金额", "余额", "状态",
									"对方账号", "对方姓名", "交易时间", "抓取时间", "摘要", "备注", "层级" };
						}
						sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, titleName.length - 1));
						// 合并列 开始行，结束行，开始列，结束列
						HSSFRow rowTitle1 = sheet.createRow(0);
						HSSFRow rowTitle2 = sheet.createRow(1);
						loadRowContentsForTheThirdParty(rowTitle1, cellStyleTitle,
								new String[] { excelInfo.get("titleName") });
						loadRowContentsForTheThirdParty(rowTitle2, cellStyleTitle, titleName);

						int num = j * mus;
						int index = 0;
						for (int i = num; i < bankLogList.size(); i++) {
							if (index == mus) {// 判断index == mus的时候跳出当前for循环
								break;
							}
							Object[] record = (Object[]) bankLogList.get(i);
							HSSFRow row = sheet.createRow(index + 2);
							if (null != record) {
								if (accountId == -8) {
									AccountBaseInfo bizAccountFrom = accountService
											.getFromCacheById((Integer) record[5]);
									BizHandicap bizHandicap = handicapService.findFromCacheById(
											Integer.valueOf(record[3] == null ? "0" : record[3].toString()));
									loadRowContentsForTheThirdParty(row, cellStyle,
											new String[] { bizHandicap == null ? "" : bizHandicap.getName(),
													AccountType.findByTypeId(Integer.valueOf(record[4].toString()))
															.getMsg(),
													(record[0] + "|" + record[1]), record[2].toString(),
													startAndEndTimeToArray[0], bizAccountFrom.getAlias() });
								} else {
									BankLogStatus statusStr = BankLogStatus.findByStatus((Integer) record[4]);
									AccountBaseInfo bizAccountFrom = accountService
											.getFromCacheById((Integer) record[1]);
									BizHandicap bizHandicap = handicapService
											.findFromCacheById(bizAccountFrom.getHandicapId());
									loadRowContentsForTheThirdParty(row, cellStyle, new String[] {
											(bizAccountFrom.getOwner() + "|" + bizAccountFrom.getBankType()),
											bizHandicap.getName(), record[3] == null ? "" : record[3].toString(),
											record[8] == null ? "" : record[8].toString(), statusStr.getMsg(),
											record[6] == null ? "" : record[6].toString(),
											record[7] == null ? "" : record[7].toString(),
											record[2] == null ? "" : record[2].toString(),
											TimeChangeCommon.TimeStamp2Date(record[9].toString(),
													"yyyy-MM-dd HH:mm:ss"),
											record[10] == null ? "" : record[10].toString(),
											record[5] == null ? "" : record[5].toString(),
											CurrentSystemLevel.valueOf(bizAccountFrom.getCurrSysLevel() == null ? 1
													: bizAccountFrom.getCurrSysLevel()).getName() });
								}
							}
							index++;
						}
						HSSFRow rowFoot = sheet.createRow((bankLogList == null ? 0 : index) + 2);
						loadRowContents(rowFoot, cellStyleTitle, new String[] { "总条数：" + index });
						// 列宽自适应 不可移动代码位置
						for (int i = 0; i < titleName.length; i++) {
							sheet.autoSizeColumn(i);
						}
					}
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
		} catch (Exception e) {
			logger.error("入款明细导出异常", e);
		}

	}

	/**
	 * 银行流水导出 空值0
	 * 
	 * @param accountId
	 * @param startAndEndTimeToArray
	 * @param fieldval
	 * @param request
	 * @param response
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/bankLog/{accountId}/{startAndEndTimeToArray}/{fieldval}")
	public void bankLog(@PathVariable Integer accountId, @PathVariable String[] startAndEndTimeToArray,
			@PathVariable String fieldval, HttpServletRequest request, HttpServletResponse response)
			throws JsonProcessingException {
		try {
			Map<String, String> excelInfo = new HashMap<String, String>();
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			excelInfo = getFileNameAndAccountInfo(accountId, "银行流水明细");
			response = setResponse(response, excelInfo.get("excelName"));
			OutputStream output = response.getOutputStream();
			BufferedOutputStream bufferedOutPut = new BufferedOutputStream(output);
			HSSFWorkbook wb = new HSSFWorkbook();
			// 样式
			HSSFCellStyle cellStyleTitle = getCellStyleTitle(wb);
			HSSFCellStyle cellStyle = getCellStyle(wb);
			HSSFSheet sheet = wb.createSheet();
			// 查询条件
			List<SearchFilter> filterToList = DynamicSpecifications.build(request);
			// 查询下发卡id 0是下发卡导出 -1是备用卡导出 -2是导出出款卡银行单信息-3是入款卡银行单信息-5客户绑定卡银行单信息
			List<Integer> accountType = new ArrayList<>();
			if (accountId == -2) {
				accountType.add(AccountType.OutBank.getTypeId());
			} else if (accountId == -1) {
				accountType.add(AccountType.ReserveBank.getTypeId());
			} else if (accountId == -3) {
				accountType.add(AccountType.InBank.getTypeId());
			} else if (accountId == -5) {
				accountType.add(AccountType.BindCustomer.getTypeId());
			} else {
				accountType.add(AccountType.ThirdCommon.getTypeId());
				accountType.add(AccountType.BindCommon.getTypeId());
			}
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
			List<Integer> accountIdList = accountService.getAccountList(accountType, handicaps);
			Integer[] accountIds = (Integer[]) accountIdList.toArray(new Integer[0]);
			// 日期控制
			Map<String, String> startAndEndTime = TimeChangeCommon.setTimeSizeByFieldval(startAndEndTimeToArray,
					fieldval);
			if (startAndEndTime.get("startTime") != null) {
				filterToList.add(
						new SearchFilter("createTime", SearchFilter.Operator.GTE, startAndEndTime.get("startTime")));
			}
			if (startAndEndTime.get("endTime") != null) {
				filterToList
						.add(new SearchFilter("createTime", SearchFilter.Operator.LTE, startAndEndTime.get("endTime")));
			}
			if (accountId != 0 && accountId != -1 && accountId != -2 && accountId != -3)
				filterToList.add(new SearchFilter("fromAccount", SearchFilter.Operator.EQ, accountId));
			else
				filterToList.add(new SearchFilter("fromAccount", SearchFilter.Operator.IN, accountIds));
			PageRequest pageRequest = new PageRequest(0, AppConstants.PAGE_SIZE_MAX, Sort.Direction.DESC, "createTime",
					"id");
			SearchFilter[] filterToArray = filterToList.toArray(new SearchFilter[filterToList.size()]);
			Specification<BizBankLog> specif = DynamicSpecifications.build(BizBankLog.class, filterToArray);
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			Page<BizBankLog> page = null;
			if (fieldval.equals("0")) {
				page = bankLogService.findAll(sysUser.getUid(), specif, accountId, fieldval, pageRequest);
			}
			// 下发银行卡数据
			List<Object> sendCardList = new ArrayList<>();
			if (accountId.equals(0) && !fieldval.equals("0")) {
				sendCardList = bankLogService.findSenderCard(startAndEndTime.get("startTime"),
						startAndEndTime.get("endTime"), handicaps);
			}
			// // 总计
			// BigDecimal[] amount =
			// bankLogService.findAmountTotal(filterToArray);
			// 标题与列头
			String[] titleName = null;
			// -2是导出出款卡银行单信息
			// -3是入款卡银行单信息
			if (accountId == -2 || accountId == -3) {
				titleName = new String[] { (accountId == -2 ? "转出账号" : "转入账号"), "盘口", "金额", "余额", "状态", "对方账号", "对方姓名",
						"交易时间", "抓取时间", "摘要", "备注" };
			} else {
				titleName = (accountId != 0 && accountId != -1 && accountId != -3)
						? new String[] { "对方账号", "对方姓名", "金额", "余额", "交易时间", "抓取时间", "状态", "摘要", "备注" }
						: new String[] { "转出账号", "状态", "金额", "余额", "盘口", "对方账号", "对方姓名", "交易时间", "抓取时间", "摘要", "备注",
								"盘口", "名称", "对方账号类型", "对方账号盘口" };
			}

			sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, titleName.length - 1));
			// 合并列 开始行，结束行，开始列，结束列
			HSSFRow rowTitle1 = sheet.createRow(0);
			HSSFRow rowTitle2 = sheet.createRow(1);
			if (accountId != 0 && accountId != -1 && accountId != -3) {
				loadRowContents(rowTitle1, cellStyleTitle, new String[] { excelInfo.get("titleName") });
				loadRowContents(rowTitle2, cellStyleTitle, titleName);
			} else {
				loadRowContentsForTheThirdParty(rowTitle1, cellStyleTitle, new String[] { excelInfo.get("titleName") });
				loadRowContentsForTheThirdParty(rowTitle2, cellStyleTitle, titleName);
			}
			// 数据行
			if (null != page) {
				List<BizBankLog> bankLogList = page.getContent();
				if (null != bankLogList && bankLogList.size() > 0) {
					Date dateNow = new Date();
					for (int i = 0; i < bankLogList.size(); i++) {
						BizBankLog record = bankLogList.get(i);
						HSSFRow row = sheet.createRow(i + 2);
						if (null != record) {
							String statusStr = record.getStatusStr();
							if (record.getStatus() == BankLogStatus.Matching.getStatus()
									&& ((dateNow.getTime() - record.getTradingTime().getTime()) / 1000 / 60
											/ 60) > 24) {
								statusStr = BankLogStatus.NoOwner.getMsg();
							}
							if (accountId == -2 || accountId == -3) {
								AccountBaseInfo bizAccountFrom = accountService
										.getFromCacheById(record.getFromAccount());
								loadRowContentsForTheThirdParty(row, cellStyle,
										new String[] { (bizAccountFrom.getOwner() + "|" + bizAccountFrom.getBankType()),
												record.getHandicapName(), record.getAmount().toString(),
												record.getBalance().toString(), statusStr, record.getToAccount(),
												record.getToAccountOwner(), record.getTradingTimeStr(),
												TimeChangeCommon.TimeStamp2Date(record.getCreateTime().toString(),
														"yyyy-MM-dd HH:mm:ss"),
												record.getSummary(), record.getRemark() });
							} else {
								if (accountId != 0 && accountId != -1 && accountId != -3) {
									loadRowContents(row, cellStyle,
											new String[] { record.getToAccount(), record.getToAccountOwner(),
													record.getAmount().toString(), record.getBalance().toString(),
													record.getTradingTimeStr(),
													TimeChangeCommon.TimeStamp2Date(record.getCreateTime().toString(),
															"yyyy-MM-dd HH:mm:ss"),
													statusStr, record.getSummary(), record.getRemark() });
								} else {
									AccountBaseInfo bizAccountFrom = accountService
											.getFromCacheById(record.getFromAccount());
									BizHandicap bizHandicap = handicapService
											.findFromCacheById(bizAccountFrom.getHandicapId());
									loadRowContentsForTheThirdParty(row, cellStyle, new String[] {
											(bizAccountFrom.getOwner() + "|" + bizAccountFrom.getBankType()), statusStr,
											record.getAmount().toString(), record.getBalance().toString(),
											bizHandicap.getName(), record.getToAccount(), record.getToAccountOwner(),
											record.getTradingTimeStr(),
											TimeChangeCommon.TimeStamp2Date(record.getCreateTime().toString(),
													"yyyy-MM-dd HH:mm:ss"),
											record.getSummary(), record.getRemark(), record.getTransFerHandicap(),
											record.getTransFerBankName(),
											(null == record.getFromAccountTypeName() ? ""
													: record.getFromAccountTypeName()),
											(null == record.getTransFerHandicap() ? ""
													: record.getTransFerHandicap()) });
								}
							}
						}
					}
				}
				// // 底部总计信息
				// String amountTotal = "0";
				// if (amount != null && amount[0] != null) {
				// amountTotal = amount[0].toString();
				// }
				HSSFRow rowFoot = sheet.createRow((bankLogList == null ? 0 : bankLogList.size()) + 2);
				// loadRowContents(rowFoot, cellStyleTitle, new String[] {
				// "总条数：" + bankLogList.size(), "", amountTotal });
				loadRowContents(rowFoot, cellStyleTitle, new String[] { "总条数：" + bankLogList.size() });

			} else {
				if (sendCardList.size() > 0) {
					for (int i = 0; i < sendCardList.size(); i++) {
						HSSFRow row = sheet.createRow(i + 2);
						Object[] object = (Object[]) sendCardList.get(i);
						AccountBaseInfo bizAccountFrom = accountService
								.getFromCacheById(Integer.valueOf(object[1].toString()));
						BizHandicap bizHandicap = handicapService
								.findFromCacheById(bizAccountFrom == null ? 0 : bizAccountFrom.getHandicapId());

						AccountBaseInfo transFerBizAccountFrom = null;
						BizHandicap transFerBizHandicap = null;
						if (new BigDecimal(object[3].toString()).compareTo(new BigDecimal("0")) > 0) {
							transFerBizAccountFrom = accountService
									.getFromCacheById(object[12] == null ? 0 : Integer.valueOf(object[12].toString()));
							transFerBizHandicap = handicapService.findFromCacheById(
									transFerBizAccountFrom == null ? 0 : transFerBizAccountFrom.getHandicapId());
						}
						loadRowContentsForTheThirdParty(row, cellStyle, new String[] {
								(bizAccountFrom.getOwner() + "|" + bizAccountFrom.getBankType()),
								BankLogStatus.findByStatus(Integer.valueOf(object[4].toString())).getMsg(),
								object[3].toString(), object[8].toString(),
								bizHandicap == null ? "" : bizHandicap.getName(),
								object[6] == null ? "" : object[6].toString(),
								object[7] == null ? "" : object[7].toString(),
								TimeChangeCommon.TimeStamp2Date(
										null == object[2] ? object[9].toString() : object[2].toString(),
										"yyyy-MM-dd HH:mm:ss"),
								TimeChangeCommon.TimeStamp2Date(object[9].toString(), "yyyy-MM-dd HH:mm:ss"),
								object[10] == null ? "" : object[10].toString(),
								object[5] == null ? "" : object[5].toString(),
								new BigDecimal(object[3].toString()).compareTo(new BigDecimal("0")) > 0
										? (transFerBizHandicap == null ? "" : transFerBizHandicap.getName()) : "",
								new BigDecimal(object[3].toString()).compareTo(new BigDecimal("0")) > 0
										? (transFerBizAccountFrom == null ? "" : transFerBizAccountFrom.getBankName())
										: "" });
					}
				}
				HSSFRow rowFoot = sheet.createRow((sendCardList == null ? 0 : sendCardList.size()) + 2);
				loadRowContents(rowFoot, cellStyleTitle, new String[] { "总条数：" + sendCardList.size() });
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
			logger.info("ExporAccount导出异常", e);
		}

	}

	/**
	 * 出款卡 冻结卡 冻结筛选 导出每个账号出款银行
	 * 
	 * @param accountIdToArray
	 * @param startAndEndTimeToArray
	 * @param request
	 * @param response
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/freezedOutwardLog/{accountIdToArray}/{startAndEndTimeToArray}")
	public void freezedOutwardLog(@PathVariable Integer[] accountIdToArray,
			@PathVariable String[] startAndEndTimeToArray, HttpServletRequest request, HttpServletResponse response)
			throws JsonProcessingException {
		try {
			// 文件名
			SimpleDateFormat sdFormatter = new SimpleDateFormat("yyyyMMddHHmmss");
			String excelName = sdFormatter.format(new Date(System.currentTimeMillis()));
			excelName += "冻结卡筛选明细.xls";
			response = setResponse(response, excelName);
			OutputStream output = response.getOutputStream();
			BufferedOutputStream bufferedOutPut = new BufferedOutputStream(output);
			HSSFWorkbook wb = new HSSFWorkbook();
			// 样式
			HSSFCellStyle cellStyleTitle = getCellStyleTitle(wb);
			HSSFCellStyle cellStyle = getCellStyle(wb);
			HSSFSheet sheet = wb.createSheet();
			// 标题与列头
			String[] titleName = new String[] { "编号", "银行类别", "开始时间", "结束时间", "收款卡号", "统计" };
			// 合并列 开始行，结束行，开始列，结束列
			HSSFRow rowTitle = sheet.createRow(0);
			loadRowContents(rowTitle, cellStyleTitle, titleName);
			// 数据行
			int rowTotal = 1;
			SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Object[] accountArray = exportAccountService.searchAccountByIds(Arrays.asList(accountIdToArray));
			if (null != accountArray && accountArray.length > 0) {
				for (int i = 0; i < accountArray.length; i++) {
					// 循环获取每一行的账号信息和日期查询条件
					Object[] accountInfo = (Object[]) accountArray[i];
					String date[] = startAndEndTimeToArray[i].split(" - ");
					Object[] bizOutwardLogArray = exportAccountService.freezedOutwardLog(
							Integer.parseInt(accountInfo[0].toString()), sf.parse(date[0]), sf.parse(date[1]));
					if (null != bizOutwardLogArray && bizOutwardLogArray.length > 0) {
						for (int j = 0; j < bizOutwardLogArray.length; j++) {
							Object[] bizOutwardLog = (Object[]) bizOutwardLogArray[j];
							if (null != bizOutwardLog) {
								HSSFRow row = sheet.createRow(rowTotal);
								loadRowContents(row, cellStyle, new String[] { accountInfo[2].toString(),
										accountInfo[3].toString(), date[0], date[1], bizOutwardLog[1].toString() });
								// 最后一列加公式
								HSSFCell rosFormula = row.createCell(5);
								rosFormula.setCellFormula("COUNTIF(E:E,E" + (rowTotal + 1) + ")");
							}
							rowTotal++;
						}
					}
				}
				HSSFRow rowFoot = sheet.createRow(rowTotal);
				loadRowContents(rowFoot, cellStyleTitle, new String[] { "总条数：" + (rowTotal - 1) });

			}
			// 列宽自适应 不可移动代码位置
			for (int i = 0; i < titleName.length; i++) {
				sheet.autoSizeColumn(i);
			}
			// 强制执行公式
			sheet.setForceFormulaRecalculation(true);
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
			logger.error("入款明细导出异常", e);
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

	private HSSFRow loadRowContentsForTheThirdParty(HSSFRow row, HSSFCellStyle cellStyleTitle, Object[] contents) {
		for (int i = 0; i < contents.length; i++) {
			HSSFCell cell = row.createCell(i);
			cell.setCellStyle(cellStyleTitle);
			if (contents.length >= 10 && !contents[0].equals("转出账号") && !contents[0].equals("转入账号")) {
				if (i == 2 || i == 3) {
					cell.setCellValue(Double.parseDouble((String) contents[i]));
				} else {
					cell.setCellValue(new HSSFRichTextString(null != contents[i] ? contents[i].toString() : ""));
				}
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

	/**
	 * 生成Excel名和表头账号信息
	 * 
	 * @param accountId
	 * @param typeName
	 * @return
	 */
	private Map<String, String> getFileNameAndAccountInfo(Integer accountId, String typeName) {
		Map<String, String> result = new HashMap<String, String>();
		String excelName = "", titleName = "";
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		BizAccount account = accountService.findById(sysUser, accountId);
		// 文件名
		SimpleDateFormat sdFormatter = new SimpleDateFormat("yyyyMMdd");
		excelName += sdFormatter.format(new Date(System.currentTimeMillis()));
		excelName += typeName;
		excelName += account != null ? account.getAlias() : "";
		excelName += ".xls";
		result.put("excelName", excelName);
		// 表头信息
		if (account != null && account.getType() == 3) {
			titleName += account.getAccount();
		} else {
			titleName += account != null ? ((account != null ? account.getAlias() : "无") + " " + account.getOwner()
					+ " " + (account != null ? account.getBankType() : "无")) : "银行流水数据";
		}
		// titleName += "\r\n" + account.getAccount();
		// titleName += "\r\n开户行：" + (account.getBankName() != null ?
		// account.getBankName() : "无");
		// titleName += "\r\n系统余额：" + (account.getBalance() != null ?
		// account.getBalance() : "0") + "，银行余额："
		// + (account.getBankBalance() != null ? account.getBankBalance() :
		// "0");
		result.put("titleName", titleName);
		return result;
	}

}
