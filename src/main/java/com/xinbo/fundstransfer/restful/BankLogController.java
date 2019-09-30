package com.xinbo.fundstransfer.restful;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.validation.Valid;

import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.domain.DynamicSpecifications;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.enums.BankLogStatus;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.FlowStatMatching;
import com.xinbo.fundstransfer.domain.repository.AccountLevelRepository;
import com.xinbo.fundstransfer.service.*;
import com.xinbo.fundstransfer.service.impl.RedisDistributedLockUtils;

@RestController
@RequestMapping("/r/banklog")
public class BankLogController extends BaseController {
	private static final Logger logger = LoggerFactory.getLogger(BankLogController.class);
	@Autowired
	BankLogService bankLogService;
	@Autowired
	private AccountService accountService;
	@Autowired
	private HandicapService handicapService;
	@Autowired
	private LevelService levelService;
	@Autowired
	private AccountLevelRepository accountLevelDao;
	@Autowired
	private SysDataPermissionService dataPermissionService;
	private ObjectMapper mapper = new ObjectMapper();
	/**
	 * 24小时：毫秒数
	 */
	private final static Long ONE_DAY_TIMESTAMP = 86400000L;
	/**
	 * 48小时：毫秒数
	 */
	private final static Long TWO_DAY_TIMESTAMP = 172800000L;
	/**
	 * 标识：客服发送信息成功
	 */
	private static final String SUCCESS_FOR_CUSTOMER_SEND_MSG = "succeed";

	@RequestMapping("/getUnmatchCount")
	public GeneralResponseData getUnmatchCount(@RequestParam(value = "accountIds") List<String> accountIdsList) {
		GeneralResponseData<Map> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
		try {
			log.debug("查询入款卡未匹配流水数量 参数:{}", accountIdsList.toString());
			Map map = bankLogService.countUnmatchFlowsFromCache(accountIdsList);
			log.debug("查询结果:{}", ObjectMapperUtils.serialize(map));
			responseData.setData(map);
			return responseData;
		} catch (Exception e) {
			log.error("查询入款卡未匹配流水数量失败:", e);
			responseData.setData(null);
			return responseData;
		}
	}

	@RequestMapping("/countUnmatch")
	public String countUnmatch(@RequestParam(value = "accountIds") List<String> accountIdsList,
			@RequestParam(value = "startTime", required = false) String startTime,
			@RequestParam(value = "endTime", required = false) String endTime) throws JsonProcessingException {
		GeneralResponseData<List<Object[]>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
		try {
			responseData.setData(bankLogService.countUnmatchFlows(accountIdsList, StringUtils.trim(startTime),
					StringUtils.trim(endTime)));
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			log.error("BankLogController.countUnmatch error:", e);
			responseData.setData(null);
			return mapper.writeValueAsString(responseData);
		}
	}

	/**
	 * 处理冲正
	 */
	@RequestMapping("/dealBackwashBankLog")
	public String dealBackwashBankLog(@RequestParam(value = "bankLogId") Long bankLogId,
			@RequestParam(value = "remark", required = false) String remark,
			@RequestParam(value = "localHostIp", required = false) String localHostIp) throws JsonProcessingException {
		GeneralResponseData<List<Map<String, Object>>> responseData;
		if (StringUtils.isBlank(remark)) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请填写处理冲正对应的订单"));
		}
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		BizBankLog bizBankLog = bankLogService.get(bankLogId);
		if (bizBankLog == null || !BankLogStatus.Refunding.getStatus().equals(bizBankLog.getStatus())) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "流水不存或已处理"));
		}
		remark = CommonUtils.genRemark(bizBankLog.getRemark(), remark, new Date(),
				new StringBuilder(sysUser.getUid()).append("(冲正处理)").append(localHostIp).toString());
		RedisDistributedLockUtils lockUtils = new RedisDistributedLockUtils(
				new StringBuilder("deal_backwash_").append(String.valueOf(bankLogId)).toString(), 5 * 1000, 5 * 1000);
		try {
			if (lockUtils != null && lockUtils.acquireLock()) {
				if (!BankLogStatus.Refunding.getStatus().equals(bizBankLog.getStatus())) {
					return mapper.writeValueAsString(
							new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "流水已处理"));
				}
				bizBankLog.setRemark(remark);
				bizBankLog.setStatus(BankLogStatus.Refunded.getStatus());
				bankLogService.update(bizBankLog);
			}
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "冲正处理成功");
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.info("冲正处理失败:{}", e);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "冲正处理失败"));
		} finally {
			if (lockUtils != null) {
				lockUtils.releaseLock();
			}
		}
	}

	/**
	 * 查询冲正
	 */
	@RequestMapping("/queryBackWashBankLog")
	public String queryBackWashBankLog(@RequestParam(value = "handicap") List<String> handicap,
			@RequestParam(value = "fromAccount", required = false) String fromAccount,
			@RequestParam(value = "orderNo", required = false) String orderNo,
			@RequestParam(value = "operator", required = false) String operator,
			@RequestParam(value = "status", required = false) Integer[] status,
			@RequestParam(value = "amountStart", required = false) BigDecimal amountStart,
			@RequestParam(value = "amountEnd", required = false) BigDecimal amountEnd,
			@RequestParam(value = "startTime", required = false) String startTime,
			@RequestParam(value = "endTime", required = false) String endTime,
			@RequestParam(value = "pageNo", required = false) Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "platFormQuery", required = false) Byte platFormQuery)
			throws JsonProcessingException {

		GeneralResponseData<List<Map<String, Object>>> responseData;
		PageRequest pageRequest = PageRequest.of(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE);
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null && (platFormQuery == null || platFormQuery.intValue() != 1)) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		if (CollectionUtils.isEmpty(handicap)) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "您没有盘口数据权限，请分配"));
		}
		try {
			if (StringUtils.isNotBlank(orderNo)) {
				orderNo = new StringBuilder("%").append(orderNo).append("%").toString();
			} else {
				orderNo = null;
			}
			if (StringUtils.isNotBlank(operator)) {
				operator = new StringBuilder("%").append(operator).append("%").toString();
			} else {
				operator = null;
			}
			if (StringUtils.isNotBlank(fromAccount)) {
				fromAccount = new StringBuilder("%").append(fromAccount).append("%").toString();
			} else {
				fromAccount = null;
			}
			List<Integer> statusList = new ArrayList<>();
			if (status != null && status.length > 0) {
				for (int i = 0, L = status.length; i < L; i++) {
					statusList.add(status[i]);
				}
			}
			List<Object[]> list = bankLogService.queryBackWashBankLong(pageRequest, handicap, fromAccount, orderNo,
					operator, statusList, amountStart, amountEnd, CommonUtils.string2Date(startTime),
					CommonUtils.string2Date(endTime));
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询冲正流水成功");
			if (list != null && list.size() > 0) {
				List<Map<String, Object>> dataList = new LinkedList<>();
				for (Object[] p : list) {
					Map map = new LinkedHashMap();
					map.put("bankLogId", p[0]);
					map.put("fromAccountId", p[1]);
					map.put("alias", p[2]);
					map.put("accountNo", p[3]);
					map.put("tradingTime", p[4]);
					if (null != p[4]) {
						map.put("tradingTime", p[4]);
					} else {
						map.put("tradingTime", null);
					}
					map.put("createTime", p[5]);
					map.put("amount", p[6]);
					map.put("bankBalance", p[7]);
					if (null != p[8]) {
						map.put("toAccount", p[8]);
					} else {
						map.put("toAccount", null);
					}
					if (null != p[9]) {
						map.put("toAccountOwner", p[9]);
					} else {
						map.put("toAccountOwner", null);
					}
					if (null != p[10]) {
						map.put("summary", p[10]);
					} else {
						map.put("summary", null);
					}
					if (null != p[11]) {
						map.put("remark", ((String) p[11]).replace("\r\n", "<br>").replace("\n", "<br>"));
					} else {
						map.put("remark", null);
					}
					if (null != p[12]) {
						map.put("handicap", p[12]);
					}
					if (null != p[13]) {
						map.put("bankType", p[13]);
					}

					dataList.add(map);
				}
				responseData.setData(dataList);
				Paging page;
				if (dataList != null && dataList.size() > 0) {
					page = CommonUtils.getPage(pageNo + 1, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
							String.valueOf(dataList.size()));
				} else {
					page = CommonUtils.getPage(0, pageSize != null ? pageSize : AppConstants.PAGE_SIZE, "0");
				}
				responseData.setPage(page);
			} else {
				responseData.setData(null);
				responseData.setPage(new Paging());
			}
		} catch (Exception e) {
			logger.info("查询冲正流水失败:{}", e);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询冲正流水失败"));
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 查询冲正总记录数
	 */
	@RequestMapping("/countBackWashBankLog")
	public String countBackWashBankLog(@RequestParam(value = "handicap", required = false) List<String> handicap,
			@RequestParam(value = "fromAccount", required = false) String fromAccount,
			@RequestParam(value = "orderNo", required = false) String orderNo,
			@RequestParam(value = "operator", required = false) String operator,
			@RequestParam(value = "status", required = false) Integer[] status,
			@RequestParam(value = "amountStart", required = false) BigDecimal amountStart,
			@RequestParam(value = "amountEnd", required = false) BigDecimal amountEnd,
			@RequestParam(value = "startTime", required = false) String startTime,
			@RequestParam(value = "endTime", required = false) String endTime,
			@RequestParam(value = "pageNo", required = false) Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "platFormQuery", required = false) Byte platFormQuery)
			throws JsonProcessingException {
		GeneralResponseData<Long> responseData;
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null && (platFormQuery == null || platFormQuery.intValue() != 1)) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		if (handicap == null || handicap.size() == 0) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "您没有盘口数据权限，请分配"));
		}
		try {
			if (StringUtils.isNotBlank(orderNo)) {
				orderNo = new StringBuilder("%").append(orderNo).append("%").toString();
			} else {
				orderNo = null;
			}
			if (StringUtils.isNotBlank(operator)) {
				operator = new StringBuilder("%").append(operator).append("%").toString();
			} else {
				operator = null;
			}
			if (StringUtils.isNotBlank(fromAccount)) {
				fromAccount = new StringBuilder("%").append(fromAccount).append("%").toString();
			} else {
				fromAccount = null;
			}
			List<Integer> statusList = new ArrayList<>();
			if (status != null && status.length > 0) {
				for (int i = 0, L = status.length; i < L; i++) {
					statusList.add(status[i]);
				}
			}
			Long count = bankLogService.countBackWashBankLong(handicap, fromAccount, orderNo, operator, statusList,
					amountStart, amountEnd, CommonUtils.string2Date(startTime), CommonUtils.string2Date(endTime));
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
					"查询冲正流水总记录数成功");
			Paging page;
			if (count != null) {
				page = CommonUtils.getPage(pageNo + 1, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
						String.valueOf(count));
			} else {
				page = CommonUtils.getPage(0, pageSize != null ? pageSize : AppConstants.PAGE_SIZE, "0");
			}
			responseData.setData(count);
			responseData.setPage(page);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.info("查询冲正流水总记录数失败:{}", e);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询冲正流水总记录数失败"));
		}
	}

	/**
	 * 查询冲正总金额
	 */
	@RequestMapping("/sumBackWashBankLog")
	public String sumBackWashBankLog(@RequestParam(value = "handicap", required = false) List<String> handicap,
			@RequestParam(value = "fromAccount", required = false) String fromAccount,
			@RequestParam(value = "orderNo", required = false) String orderNo,
			@RequestParam(value = "operator", required = false) String operator,
			@RequestParam(value = "status", required = false) Integer[] status,
			@RequestParam(value = "amountStart", required = false) BigDecimal amountStart,
			@RequestParam(value = "amountEnd", required = false) BigDecimal amountEnd,
			@RequestParam(value = "startTime", required = false) String startTime,
			@RequestParam(value = "endTime", required = false) String endTime,
			@RequestParam(value = "platFormQuery", required = false) Byte platFormQuery)
			throws JsonProcessingException {
		GeneralResponseData<BigDecimal> responseData;
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null && (platFormQuery == null || platFormQuery.intValue() != 1)) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		if (handicap == null || handicap.size() == 0) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "您没有盘口数据权限，请分配"));
		}
		try {
			if (StringUtils.isNotBlank(orderNo)) {
				orderNo = new StringBuilder("%").append(orderNo).append("%").toString();
			} else {
				orderNo = null;
			}
			if (StringUtils.isNotBlank(operator)) {
				operator = new StringBuilder("%").append(operator).append("%").toString();
			} else {
				operator = null;
			}
			if (StringUtils.isNotBlank(fromAccount)) {
				fromAccount = new StringBuilder("%").append(fromAccount).append("%").toString();
			} else {
				fromAccount = null;
			}
			List<Integer> statusList = new ArrayList<>();
			if (status != null && status.length > 0) {
				for (int i = 0, L = status.length; i < L; i++) {
					statusList.add(status[i]);
				}
			}
			BigDecimal sum = bankLogService.sumBackWashBankLong(handicap, fromAccount, orderNo, operator, statusList,
					amountStart, amountEnd, CommonUtils.string2Date(startTime), CommonUtils.string2Date(endTime));
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
					"查询冲正流水总金额成功");
			if (sum != null) {
				responseData.setData(sum);
			} else {
				responseData.setData(new BigDecimal(0));
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.info("查询冲正流水总金额失败:{}", e);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询冲正流水总金额失败"));
		}
	}

	/**
	 * 客服发消息
	 *
	 * @param accountId
	 *            公司入款当前页签的账号id 以便发送到对应审核人
	 */
	@RequiresPermissions(value = { "IncomeAuditCompTotal:CustomerSendMessage:*",
			"IncomeAuditComp:CustomerSendMessage:*" }, logical = Logical.OR)
	@RequestMapping("/customerSendMsg")
	public String customerSendMsg(@RequestParam(value = "id") Long bankFlowId,
			@RequestParam(value = "accountId") Long accountId, @RequestParam(value = "message") String message)
			throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (accountId == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数异常，请联系技术人员"));
		}
		if (StringUtils.isBlank(message)) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请填写备注信息"));
		}
		try {
			String flag = bankLogService.customerSendMsg(bankFlowId, accountId, message, operator.getUsername());
			log.info("发送消息结果:{}", flag);
		} catch (Exception e) {
			logger.error("发送消息失败： ", e);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "发送消息失败"));
		}
		return mapper.writeValueAsString(
				new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "发送消息成功"));
	}

	/**
	 * 客服添加备注
	 */
	@RequiresPermissions(value = { "IncomeAuditCompTotal:CustomerAddRemark:*",
			"IncomeAuditComp:CustomerAddRemark:*" }, logical = Logical.OR)
	@RequestMapping("/customerAddRemark")
	public String customerAddRemark(@RequestParam(value = "id") Long bankFlowId,
			@RequestParam(value = "remark") String remark,
			@RequestParam(value = "localHostIp", required = false) String localHostIp) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (bankFlowId == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数异常，请联系技术"));
		}
		if (StringUtils.isBlank(remark)) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请填写备注"));
		}
		try {
			BizBankLog bizBankLog = bankLogService.getBizBankLogByIdForUpdate(bankFlowId);
			String uid = operator.getUid();
			if (StringUtils.isNotBlank(localHostIp)) {
				uid = new StringBuilder(uid).append("(").append(localHostIp).append(")").toString();
			}
			bizBankLog.setRemark(CommonUtils.genRemark(bizBankLog.getRemark(), remark, new Date(), uid));
			bankLogService.update(bizBankLog);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "添加备注成功"));
		} catch (Exception e) {
			logger.error("添加备注失败：{}", e);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "添加备注失败"));
		}
	}

	/**
	 * 转已处理或手续费
	 *
	 * @param status
	 *            4 已处理 /5 手续费
	 */
	@RequiresPermissions(value = { "IncomeAuditComp:bankFlowFinalDeal:*",
			"IncomeAuditCompTotal:bankFlowFinalDeal:*" }, logical = Logical.OR)
	@RequestMapping("/doDisposedFee")
	public String doDisposedFee(@RequestParam(value = "bankLogId") Long bankLogId,
			@RequestParam(value = "status") Integer status, @RequestParam(value = "remark") String remark)
			throws JsonProcessingException {
		GeneralResponseData<List<Map<String, Object>>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功");
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		BizBankLog bizBankLog = bankLogService.getBizBankLogByIdForUpdate(bankLogId);
		// 校验
		if (bizBankLog == null || !BankLogStatus.Matching.getStatus().equals(bizBankLog.getStatus())
				|| (!BankLogStatus.Disposed.getStatus().equals(status)
						&& !BankLogStatus.Fee.getStatus().equals(status))) {
			return mapper
					.writeValueAsString(new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
							"请检查 1。当前流水是否存在  2.当前状态是否是未匹配(未认领在数据库中也是未匹配)状态 3.传入status是否是已处理或手续费"));
		}
		remark = "修改银行流水状态为：" + BankLogStatus.findByStatus(status).getMsg() + " \r\n" + remark;
		remark = CommonUtils.genRemark(bizBankLog.getRemark(), remark, new Date(), sysUser.getUid());
		bizBankLog.setRemark(remark);
		bizBankLog.setStatus(status);
		bankLogService.update(bizBankLog);
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 未认领的流水 恢复正在匹配
	 */
	// @RequestMapping("/recover")
	// public String recover(@RequestParam(value = "id") Long id) throws
	// JsonProcessingException {
	// logger.debug("恢复匹配操作:参数：id:{}", id);
	// GeneralResponseData<BizBankLog> responseData = new GeneralResponseData<>(
	// GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "恢复匹配成功");
	// BizBankLog bizBankLog = bankLogService.findBankFlowById(id);
	// if (bizBankLog != null) {
	// Date date = new Date();
	// Long updateTime = bizBankLog.getTradingTime().getTime();
	// try {
	// if (date.getTime() - updateTime >= ONE_DAY_TIMESTAMP
	// && date.getTime() - updateTime <= TWO_DAY_TIMESTAMP) {
	// bizBankLog.setTradingTime(date);
	// //bizBankLog.setStatus(BankLogStatus.Matching.getStatus());
	// bankLogService.save(bizBankLog);
	// responseData.setData(bizBankLog);
	// }
	// if (date.getTime() - updateTime < ONE_DAY_TIMESTAMP
	// || date.getTime() - updateTime > TWO_DAY_TIMESTAMP) {
	// responseData = new
	// GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
	// "该流水已恢复");
	//
	// }
	// } catch (Exception e) {
	// logger.error("恢复匹配操作失败：" + e);
	// responseData = new
	// GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
	// "恢复失败");
	// }
	// return mapper.writeValueAsString(responseData);
	// } else {
	// responseData = new
	// GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
	// "该流水已恢复");
	// responseData.setData(null);
	// return mapper.writeValueAsString(responseData);
	// }
	// }

	/**
	 * 入款审核 公司入款-未认领 银行流水 accountIdsArayy 只查询用户接单的几个账号的未认领的流水 由于第三方流水 单独成表 所以去除 flag
	 * 'company' 公司
	 */
	@RequestMapping("/unmatch")
	public String unMatch(@RequestParam(value = "accountIds") Integer[] accountIdsArayy,
			@RequestParam(value = "payer") String payer, @RequestParam(value = "account") Integer accountId,
			@RequestParam(value = "fromMoney") BigDecimal fromMoney,
			@RequestParam(value = "toMoney") BigDecimal toMoney, @RequestParam(value = "startTime") String startTime,
			@RequestParam(value = "endTime") String endTime, @RequestParam(value = "pageNo") Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		GeneralResponseData<List<Map<String, Object>>> responseData;
		PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
				Sort.Direction.DESC, "tradingTime", "createTime", "id");
		// 盘口 层级(收款账号所属的盘口层级) 付款人toAccountOwner
		SysUser currentUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		// 根据盘口 层级 当前用户id 查询收款账号
		try {
			Integer accountIds[] = new Integer[0];
			if (currentUser.getId() == AppConstants.USER_ID_4_ADMIN) {
				List<Integer> accountIdsList = accountService.findIncomeAccountIdList(null, null, currentUser.getId());
				if (accountIdsList != null && accountIdsList.size() > 0) {
					accountIds = new Integer[accountIdsList.size()];
					for (int i = 0, L = accountIdsList.size(); i < L; i++) {
						accountIds[i] = accountIdsList.get(i);
					}
				}
			} else {
				accountIds = accountIdsArayy;
			}
			Page<BizBankLog> page = bankLogService.findUnmatchForCompanyPage(accountIds, payer, accountId, fromMoney,
					toMoney, startTime, endTime, pageRequest);
			if (page != null && page.getContent() != null && page.getContent().size() > 0) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue());
				List<Map<String, Object>> list = new ArrayList<>();
				List<BizBankLog> bankLogList = page.getContent();
				String sum = bankLogService.findUnmatchForCompany(accountIds, payer, accountId, fromMoney, toMoney,
						startTime, endTime);
				for (BizBankLog p : bankLogList) {
					Map<String, Object> map = new HashMap<>();
					List<BizAccountLevel> bizAccountLevelList = accountLevelDao.findByAccountId(p.getFromAccount());
					if (bizAccountLevelList != null && bizAccountLevelList.size() > 0) {
						BizLevel bizLevel = levelService.findFromCache(bizAccountLevelList.get(0).getLevelId());
						if (bizLevel != null) {
							BizHandicap bizHandicap = handicapService.findFromCacheById(bizLevel.getHandicapId());
							map.put("handicap", bizHandicap.getName());
							map.put("level", bizLevel.getName());
						}
					}
					BizAccount bizAccount = accountService.findById(currentUser, p.getFromAccount());
					map.put("fromAccount", bizAccount.getAccount());
					map.put("fromAccountId", bizAccount.getId());
					String toAcc = "";// 对方账号隐藏***显示
					if (StringUtils.isNotBlank(p.getToAccount())) {
						int len = p.getToAccount().length();
						toAcc = p.getToAccount().substring(0, 3) + "***"
								+ p.getToAccount().substring(len > 3 ? len - 3 : len);
					}
					map.put("toAcount", toAcc);
					map.put("fromAccountName", bizAccount.getBankName());
					map.put("payer", p.getToAccountOwner());
					map.put("amount", p.getAmount());
					map.put("tradeTime", p.getTradingTime());
					map.put("createTime", p.getCreateTime());
					map.put("sumAmount", sum);
					map.put("id", p.getId());
					map.put("summary", StringUtils.isNotBlank(p.getSummary()) ? p.getSummary() : "");
					map.put("remarK",
							StringUtils.isNotBlank(p.getRemark())
									? p.getRemark().replace("\r\n", "<br>").replace("\n", "<br>")
									: "");
					list.add(map);
				}
				responseData.setData(list);
				responseData.setPage(new Paging(page));
			} else {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "无记录");
			}
		} catch (Exception e) {
			logger.error("查询未认领银行流水失败:" + e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询失败");
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 通过主键id查询银行流水
	 */
	@RequestMapping("/getById")
	public String findBankFlowById(@RequestParam(value = "id") Long id) throws JsonProcessingException {
		GeneralResponseData<BizBankLog> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		BizBankLog bizBankLog = bankLogService.findBankFlowById(id);
		responseData.setData(bizBankLog);
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 条件查询银行流水 正在匹配
	 */
	@RequestMapping("/search")
	public String search(@RequestParam(value = "accountId") Integer accountId,
			@RequestParam(value = "payMan") String payMan, @RequestParam(value = "pageNo") Integer pageNo,
			@RequestParam(value = "member") String member, @RequestParam(value = "fromMoney") BigDecimal fromMoney,
			@RequestParam(value = "toMoney") BigDecimal toMoney, @RequestParam(value = "startTime") String startTime,
			@RequestParam(value = "endTime") String endTime) throws JsonProcessingException {
		GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		Map<String, Object> map = new LinkedHashMap<>();
		Sort sort2 = new Sort(Sort.Direction.DESC, "tradingTime"); // tradingTime
		PageRequest pageRequest2 = new PageRequest(pageNo, AppConstants.PAGE_SIZE / 2, sort2);
		Integer[] accoutIdArray = new Integer[] { accountId };
		// 前端去除了选中某一行流水取查询同一层级下的账号提单 现在默认查询同一层级下该账号同一层级下暂停 冻结的账号流水
		List<Integer> list = null;// 同一层级的冻结 暂停的 账号
		if (accountId != null) {
			BizAccount bizAccount = accountService.getById(accountId);
			if (bizAccount.getStatus().equals(AccountStatus.Normal.getStatus())) {
				// 正常的账号才扩展查询同层级的其他异常账号
				list = accountService.findAccountIdInSameLevel(accountId);
			}
		}
		if (list != null && list.size() > 0) {
			accoutIdArray = new Integer[list.size() + 1];
			accoutIdArray[0] = accountId;
			for (int i = 0, L = list.size(); i < L; i++) {
				accoutIdArray[i + 1] = list.get(i);
			}
		}
		try {
			Page<BizBankLog> pageBankFlow = bankLogService.findBankLogPageNoCount(
					StringUtils.isNotBlank(payMan) ? payMan : null, BankLogStatus.Matching.getStatus(), null, null,
					accoutIdArray, StringUtils.isNotBlank(member) ? member : null, fromMoney, toMoney, startTime,
					endTime, pageRequest2);
			if (pageBankFlow != null && pageBankFlow.getContent() != null && pageBankFlow.getContent().size() > 0) {
				List<BizBankLog> list1 = pageBankFlow.getContent();
				map.put("bankFlowList", wrapListForRemark(list1, accountId, Arrays.asList(accoutIdArray)));
				responseData.setData(map);
				responseData.setPage(new Paging(pageBankFlow));
			} else {
				responseData.setData(null);
			}
		} catch (Exception e) {
			logger.error("查询银行流水失败：{}", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue());
		}
		return mapper.writeValueAsString(responseData);
	}

	private List<BizBankLog> wrapListForRemark(List<BizBankLog> list, Integer accountId, List<Integer> accountIdList) {
		if (list == null || list.size() == 0) {
			return Collections.emptyList();
		}
		List<BizBankLog> bankLogList = new LinkedList<>();
		list.forEach((p) -> {
			if (accountIdList.contains(p.getFromAccount())) {
				if (!accountId.equals(p.getFromAccount())) {
					BizAccount bizAccount = accountService.getById(p.getFromAccount());
					String account = new StringBuilder(bizAccount.getAccount().substring(0, 5)).append("**")
							.append(bizAccount.getAccount().substring(bizAccount.getAccount().length() - 4)).toString();
					if (StringUtils.isNotBlank(p.getRemark())) {
						p.setRemark("同层级账号:" + account + "|" + bizAccount.getOwner() + "  的流水<br>"
								+ p.getRemark().replace("\r\n", "<br>").replace("\n", "<br>"));
					} else {
						p.setRemark("同层级账号:" + account + "|" + bizAccount.getOwner() + "  的流水<br>");
					}

				} else {
					if (StringUtils.isNotBlank(p.getRemark())) {
						p.setRemark(p.getRemark().replace("\r\n", "<br>").replace("\n", "<br>"));
					}
				}
			}
			String toAcc = "";
			if (StringUtils.isNotBlank(p.getToAccount())) {
				int len = p.getToAccount().length();
				toAcc = p.getToAccount().substring(0, 3) + "***" + p.getToAccount().substring(len > 3 ? len - 3 : len);
			}
			p.setToAccount(toAcc);
			bankLogList.add(p);
		});
		return bankLogList;
	}

	/**
	 * 获取正在匹配银行流水总金额
	 */
	@RequestMapping("/getBankFlowSum")
	public String getBankFlowSum(@RequestParam(value = "accountId") Integer accountId,
			@RequestParam(value = "payMan") String payMan, @RequestParam(value = "member") String member,
			@RequestParam(value = "fromMoney") BigDecimal fromMoney,
			@RequestParam(value = "toMoney") BigDecimal toMoney, @RequestParam(value = "startTime") String startTime,
			@RequestParam(value = "endTime") String endTime) throws JsonProcessingException {
		GeneralResponseData<String> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		Integer[] accoutIdArray = new Integer[] { accountId };
		// 前端去除了选中某一行流水取查询同一层级下的账号提单 现在默认查询同一层级下该账号同一层级下暂停 冻结的账号流水
		List<Integer> list = null;// 同一层级的冻结 暂停的 账号
		if (accountId != null) {
			BizAccount bizAccount = accountService.getById(accountId);
			if (bizAccount.getStatus().equals(AccountStatus.Normal.getStatus())) {
				// 正常的账号才扩展查询同层级的其他异常账号
				list = accountService.findAccountIdInSameLevel(accountId);
			}
		}
		if (list != null && list.size() > 0) {
			accoutIdArray = new Integer[list.size() + 1];
			accoutIdArray[0] = accountId;
			for (int i = 0, L = list.size(); i < L; i++) {
				accoutIdArray[i + 1] = list.get(i);
			}
		}
		String sum = null;
		try {
			sum = bankLogService.getSumAmount(StringUtils.isNotBlank(payMan) ? payMan : null,
					BankLogStatus.Matching.getStatus(), null, null, null, accoutIdArray,
					StringUtils.isNotBlank(member) ? member : null, fromMoney, toMoney, startTime, endTime);
		} catch (Exception e) {
			logger.error("查询正在匹配银行流水失败:{}", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue());
		}
		responseData.setData(sum);
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 获取正在匹配银行流水总记录数
	 */
	@RequestMapping("/getBankFlowCount")
	public String getBankFlowCount(@RequestParam(value = "accountId") Integer accountId,
			@RequestParam(value = "payMan") String payMan, @RequestParam(value = "pageNo") Integer pageNo,
			@RequestParam(value = "member") String member, @RequestParam(value = "fromMoney") BigDecimal fromMoney,
			@RequestParam(value = "toMoney") BigDecimal toMoney, @RequestParam(value = "startTime") String startTime,
			@RequestParam(value = "endTime") String endTime) throws JsonProcessingException {
		GeneralResponseData<String> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		Integer[] accoutIdArray = new Integer[] { accountId };
		// 前端去除了选中某一行流水取查询同一层级下的账号提单 现在默认查询同一层级下该账号同一层级下暂停 冻结的账号流水
		List<Integer> list = null;// 同一层级的冻结 暂停的 账号
		if (accountId != null) {
			BizAccount bizAccount = accountService.getById(accountId);
			if (bizAccount.getStatus().equals(AccountStatus.Normal.getStatus())) {
				// 正常的账号才扩展查询同层级的其他异常账号
				list = accountService.findAccountIdInSameLevel(accountId);
			}
		}
		if (list != null && list.size() > 0) {
			accoutIdArray = new Integer[list.size() + 1];
			accoutIdArray[0] = accountId;
			for (int i = 0, L = list.size(); i < L; i++) {
				accoutIdArray[i + 1] = list.get(i);
			}
		}
		try {
			String count = bankLogService.getCount(StringUtils.isNotBlank(payMan) ? payMan : null,
					BankLogStatus.Matching.getStatus(), null, null, null, accoutIdArray,
					StringUtils.isNotBlank(member) ? member : null, fromMoney, toMoney, startTime, endTime);
			Paging page;
			if (StringUtils.isNotBlank(count) && Integer.parseInt(count) > 0) {
				page = CommonUtils.getPage(pageNo + 1, AppConstants.PAGE_SIZE / 2, count);
			} else {
				page = CommonUtils.getPage(0, AppConstants.PAGE_SIZE / 2, "0");
			}
			responseData.setPage(page);
		} catch (Exception e) {
			logger.error("查询银行流水总记录数失败：" + e.getMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询失败");
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 根据流水ID获取流水
	 *
	 * @param id
	 *            流水ID
	 */
	@RequestMapping("/findbyid")
	public String findById(@RequestParam(value = "id") Long id) throws JsonProcessingException {
		try {
			BizBankLog data = bankLogService.get(id);
			GeneralResponseData<BizBankLog> response = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			response.setData(data);
			return mapper.writeValueAsString(response);
		} catch (Exception e) {
			logger.error("根据流水ID获取流水 " + e);
			e.printStackTrace();
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 根据入款请求ID获取入款流水
	 * <p>
	 * 该入款请求须处于匹配状态
	 * </p>
	 *
	 * @param id
	 *            入款请求ID
	 */
	@RequestMapping("/findByIncomeReqId")
	public String findByIncomeReqId(@RequestParam(value = "id") Long id) throws JsonProcessingException {
		try {
			BizBankLog data = bankLogService.findByIncomeReqId(id);
			GeneralResponseData<BizBankLog> response = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			response.setData(data);
			return mapper.writeValueAsString(response);
		} catch (Exception e) {
			logger.error("根据入款请求ID获取入款流水" + e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/bankLogList")
	public String bankLogList(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize, @Valid BizBankLog bankLog,
			@RequestParam(value = "orderBy", required = false) String orderBy,
			@RequestParam(value = "minAmount", required = false) String minAmount,
			@RequestParam(value = "maxAmount", required = false) String maxAmount,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "typeList", required = false) List<Integer> typeList) throws JsonProcessingException {
		try {
			GeneralResponseData<List<BizBankLog>> response = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			// 默认先抓取时间，然后交易时间
			PageRequest pageRequest;
			if (StringUtils.isNotBlank(orderBy) && orderBy.equals("trading_time")) {
				pageRequest = PageRequest.of(pageNo, pageSize == null ? AppConstants.PAGE_SIZE : pageSize,
						Sort.Direction.DESC, "trading_time", "create_time", "id");
			} else if (StringUtils.isNotBlank(orderBy) && orderBy.equals("create_time")) {
				pageRequest = PageRequest.of(pageNo, pageSize == null ? AppConstants.PAGE_SIZE : pageSize,
						Sort.Direction.DESC, "create_time", "trading_time", "id");
			} else {
				pageRequest = new PageRequest(pageNo, pageSize == null ? AppConstants.PAGE_SIZE : pageSize,
						Sort.Direction.DESC, "id");
			}
			SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date startTime = null, endTime = null;
			if (startAndEndTimeToArray != null) {
				if (startAndEndTimeToArray.length > 0 && startAndEndTimeToArray[0] != null) {
					startTime = sf.parse(startAndEndTimeToArray[0]);
				}
				if (startAndEndTimeToArray.length > 1 && startAndEndTimeToArray[1] != null) {
					endTime = sf.parse(startAndEndTimeToArray[1]);
				}
			}
			BigDecimal min = StringUtils.isNotBlank(minAmount) ? new BigDecimal(minAmount) : null;
			BigDecimal max = StringUtils.isNotBlank(maxAmount) ? new BigDecimal(maxAmount) : null;
			Page<Object> page = bankLogService.bankLogList(pageRequest, bankLog, min, max, startTime, endTime);
			List<BizBankLog> bankLogList = new ArrayList<>();
			BizBankLog newBankLog;
			for (Object temp : page.getContent()) {
				if (temp == null) {
					continue;
				}
				Object[] record = (Object[]) temp;
				newBankLog = new BizBankLog();
				if (null != record[0] && StringUtils.isNotEmpty(record[0].toString())) {
					newBankLog.setId(Long.valueOf(record[0].toString()));
				}
				if (null != record[1] && StringUtils.isNotEmpty(record[1].toString())) {
					newBankLog.setFromAccount(Integer.valueOf(record[1].toString()));
				}
				if (null != record[2] && StringUtils.isNotEmpty(record[2].toString())) {
					newBankLog.setTradingTimeStr(record[2].toString());
				}
				if (null != record[3] && StringUtils.isNotEmpty(record[3].toString())) {
					newBankLog.setCreateTimeStr(record[3].toString());
				}
				if (null != record[4] && StringUtils.isNotEmpty(record[4].toString())) {
					newBankLog.setAmount(new BigDecimal(record[4].toString()));
				}
				if (null != record[5] && StringUtils.isNotEmpty(record[5].toString())) {
					newBankLog.setBalance(new BigDecimal(record[5].toString()));
				}
				if (null != record[6] && StringUtils.isNotEmpty(record[6].toString())) {
					newBankLog.setToAccount(record[6].toString());
				}
				if (null != record[7] && StringUtils.isNotEmpty(record[7].toString())) {
					newBankLog.setToAccountOwner(record[7].toString());
				}
				if (null != record[8] && StringUtils.isNotEmpty(record[8].toString())) {
					newBankLog.setSummary(record[8].toString());
				}
				if (null != record[9] && StringUtils.isNotEmpty(record[9].toString())) {
					newBankLog.setRemark(record[9].toString());
				}
				if (null != record[10] && StringUtils.isNotEmpty(record[10].toString())) {
					newBankLog.setStatus(Integer.parseInt(record[10].toString()));
				}
				bankLogList.add(newBankLog);
			}
			Map<String, Object> header = new HashMap<>();
			header.put("totalAmount",
					bankLogService.bankLogList_sumAmount(pageRequest, bankLog, min, max, startTime, endTime));
			response.setData(bankLogList);
			response.setPage(new Paging(page, header));
			return mapper.writeValueAsString(response);
		} catch (Exception e) {
			logger.error("查询银行流水 " + e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败 " + e.getLocalizedMessage()));
		}

	}

	/**
	 * 入款未认领流水
	 *
	 * @param pageNo
	 * @param pageSize
	 * @param bankLog
	 * @param minAmount
	 * @param maxAmount
	 * @param startAndEndTimeToArray
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/noOwner4Income")
	public String noOwner4Income(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "handicapId", required = false) Integer handicapId, @Valid BizBankLog bankLog,
			@RequestParam(value = "minAmount", required = false) BigDecimal minAmount,
			@RequestParam(value = "maxAmount", required = false) BigDecimal maxAmount,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray)
			throws JsonProcessingException {
		try {
			GeneralResponseData<List<BizBankLog>> response = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, pageSize == null ? AppConstants.PAGE_SIZE : pageSize);
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			List<BizHandicap> dataToList = dataPermissionService.getOnlyHandicapByUserId(sysUser);
			List<Integer> handicapIdToList = new ArrayList<Integer>();
			if (CollectionUtils.isEmpty(dataToList)) {
				throw new Exception("当前用户未配置盘口权限，请配置后再进行查询！");
			} else {
				if (null == handicapId) {
					for (int i = 0; i < dataToList.size(); i++) {
						if (null != dataToList.get(i) && null != dataToList.get(i).getId()) {
							handicapIdToList.add(dataToList.get(i).getId());
						}
					}
				} else {
					handicapIdToList.add(handicapId);
				}
			}
			SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date startTime = null, endTime = null;
			if (startAndEndTimeToArray != null) {
				if (startAndEndTimeToArray.length > 0 && startAndEndTimeToArray[0] != null) {
					startTime = sf.parse(startAndEndTimeToArray[0]);
				}
				if (startAndEndTimeToArray.length > 1 && startAndEndTimeToArray[1] != null) {
					endTime = sf.parse(startAndEndTimeToArray[1]);
				}
			}
			Page<Object> page = bankLogService.noOwner4Income(pageRequest, bankLog, minAmount, maxAmount, startTime,
					endTime, handicapIdToList);
			List<BizBankLog> bankLogList = new ArrayList<BizBankLog>();
			BizBankLog newBankLog;
			for (Object temp : page.getContent()) {
				if (temp == null) {
					continue;
				}
				Object[] record = (Object[]) temp;
				newBankLog = new BizBankLog();
				if (null != record[0] && StringUtils.isNotEmpty(record[0].toString())) {
					newBankLog.setId(Long.valueOf(record[0].toString()));
				}
				if (null != record[1] && StringUtils.isNotEmpty(record[1].toString())) {
					newBankLog.setFromAccount(Integer.valueOf(record[1].toString()));
				}
				if (null != record[2] && StringUtils.isNotEmpty(record[2].toString())) {
					newBankLog.setTradingTimeStr(record[2].toString());
				}
				if (null != record[3] && StringUtils.isNotEmpty(record[3].toString())) {
					newBankLog.setCreateTimeStr(record[3].toString());
				}
				if (null != record[4] && StringUtils.isNotEmpty(record[4].toString())) {
					newBankLog.setAmount(new BigDecimal(record[4].toString()));
				}
				if (null != record[5] && StringUtils.isNotEmpty(record[5].toString())) {
					newBankLog.setBalance(new BigDecimal(record[5].toString()));
				}
				if (null != record[6] && StringUtils.isNotEmpty(record[6].toString())) {
					newBankLog.setToAccount(record[6].toString());
				}
				if (null != record[7] && StringUtils.isNotEmpty(record[7].toString())) {
					newBankLog.setToAccountOwner(record[7].toString());
				}
				if (null != record[8] && StringUtils.isNotEmpty(record[8].toString())) {
					newBankLog.setSummary(record[8].toString());
				}
				if (null != record[9] && StringUtils.isNotEmpty(record[9].toString())) {
					newBankLog.setRemark(record[9].toString());
				}
				if (null != record[10] && StringUtils.isNotEmpty(record[10].toString())) {
					newBankLog.setStatus(Integer.parseInt(record[10].toString()));
				}
				if (null != record[11] && StringUtils.isNotEmpty(record[11].toString())) {
					newBankLog.setFromAccountNO(record[11].toString());
				}
				bankLogList.add(newBankLog);
			}
			Map<String, Object> header = new HashMap<String, Object>();
			header.put("totalAmount", bankLogService.bankLogList_sumAmount4(pageRequest, bankLog, minAmount, maxAmount,
					startTime, endTime, handicapIdToList));
			response.setData(bankLogList);
			response.setPage(new Paging(page, header));
			return mapper.writeValueAsString(response);
		} catch (Exception e) {
			logger.error("查询银行流水 " + e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败 " + e.getLocalizedMessage()));
		}

	}

	/**
	 * 统计未匹配的银行流水，以前端提示客服未匹配数量
	 */
	@RequestMapping("/countUnmatchBankLogs")
	public String countUnmatchBankLogs(@RequestParam(value = "fromAccountIds") List<Integer> fromAccountIdArray,
			@RequestParam(value = "startTime", required = false) String startTime,
			@RequestParam(value = "endTime", required = false) String endTime) throws JsonProcessingException {
		if (fromAccountIdArray != null && fromAccountIdArray.size() > 0) {
			GeneralResponseData<List<Object[]>> response = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			List<Object[]> list = bankLogService.countUnmatchBankLogs(fromAccountIdArray,
					CommonUtils.string2Date(startTime), CommonUtils.string2Date(endTime));
			response.setData(list);
			return mapper.writeValueAsString(response);
		} else {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "无数据! "));
		}
	}

	/**
	 * 查询银行流水
	 *
	 * @param searchTypeIn0Out1
	 *            0:转入流水(金额为+)</br>
	 *            1 转出流水(金额为-)
	 */
	@RequestMapping("/findbyfrom")
	public String findByFrom(@RequestParam(value = "pageNo") int pageNo, @Valid BizBankLog bankLog,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "searchTypeIn0Out1", required = false) Integer searchTypeIn0Out1,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "minAmount", required = false) BigDecimal minAmount,
			@RequestParam(value = "maxAmount", required = false) BigDecimal maxAmount,
			@RequestParam(value = "statusToArray", required = false) Integer[] statusToArray)
			throws JsonProcessingException {
		try {
			pageSize = pageSize == null ? AppConstants.PAGE_SIZE : pageSize;
			PageRequest pageRequest = new PageRequest(pageNo, pageSize, Sort.Direction.DESC, "tradingTime",
					"createTime");
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			List<SearchFilter> filterList = DynamicSpecifications.build(request);
			if (searchTypeIn0Out1 != null) {
				if (searchTypeIn0Out1 == 0) {// 转入
					filterList.add(new SearchFilter("amount", SearchFilter.Operator.GT, 0));
				} else {// 转出
					filterList.add(new SearchFilter("amount", SearchFilter.Operator.LT, 0));
				}
			}
			if (minAmount != null) {
				filterList.add(new SearchFilter("amount", SearchFilter.Operator.GTE, minAmount));
			}
			if (maxAmount != null) {
				filterList.add(new SearchFilter("amount", SearchFilter.Operator.LTE, maxAmount));
			}
			Date[] startAndEndTime = CommonUtils.parseStartAndEndTime(statusToArray, startAndEndTimeToArray);
			if (startAndEndTime[0] != null) {
				filterList.add(new SearchFilter("createTime", SearchFilter.Operator.GTE, startAndEndTime[0]));
			}
			if (startAndEndTime[1] != null) {
				filterList.add(new SearchFilter("createTime", SearchFilter.Operator.LTE, startAndEndTime[1]));
			}
			if (bankLog.getId() != null) {
				filterList.add(new SearchFilter("id", SearchFilter.Operator.EQ, bankLog.getId()));
			}
			// 已匹配流水
			if (bankLog.getFromAccount() != 0) {
				filterList.add(new SearchFilter("fromAccount", SearchFilter.Operator.EQ, bankLog.getFromAccount()));
			}
			if (StringUtils.isNotBlank(bankLog.getToAccount())) {
				filterList.add(new SearchFilter("toAccount", SearchFilter.Operator.LIKE, bankLog.getToAccount()));
			}
			if (StringUtils.isNotBlank(bankLog.getRemark())) {
				filterList.add(new SearchFilter("remark", SearchFilter.Operator.LIKE, bankLog.getRemark()));
			}
			if (StringUtils.isNotBlank(bankLog.getToAccountOwner())) {
				filterList.add(
						new SearchFilter("toAccountOwner", SearchFilter.Operator.LIKE, bankLog.getToAccountOwner()));
			}
			statusToArray = BankLogStatus.transStatusToArray(statusToArray);
			if (statusToArray != null && statusToArray.length > 0) {
				if (statusToArray.length > 1) {
					filterList.add(new SearchFilter("status", SearchFilter.Operator.IN, statusToArray));
				} else if (!statusToArray[0].equals(BankLogStatus.NoOwner.getStatus())) {
					filterList.add(new SearchFilter("status", SearchFilter.Operator.EQ, statusToArray[0]));
				}
			}
			SearchFilter[] filterToArray = filterList.toArray(new SearchFilter[filterList.size()]);
			Specification<BizBankLog> specification = DynamicSpecifications.build(BizBankLog.class, filterToArray);
			Page<BizBankLog> page = bankLogService.findAll(operator.getUid(), specification, -1, "-1", pageRequest);
			Map<String, Object> header = buildHeader(filterToArray);
			GeneralResponseData<List<BizBankLog>> response = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			response.setData(page.getContent());
			response.setPage(new Paging(page, header));
			return mapper.writeValueAsString(response);
		} catch (Exception e) {
			logger.error("查询银行流水 " + e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败 " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 正在匹配流水统计
	 * <p>
	 * 默认统计时间段：7天
	 * </p>
	 *
	 * @param accountIdArray
	 *            账号ID数组
	 */
	@RequestMapping("/findStat4Matching")
	public String findStat4Matching(@RequestParam(value = "accountIdArray") Integer[] accountIdArray)
			throws JsonProcessingException {
		try {
			List<FlowStatMatching> data = bankLogService.findFlowStat4Matching(accountIdArray);
			GeneralResponseData<List<FlowStatMatching>> response = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			response.setData(data);
			return mapper.writeValueAsString(response);
		} catch (Exception e) {
			logger.error("正在匹配流水统计" + e);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作出错" + e));
		}
	}

	private Map<String, Object> buildHeader(SearchFilter[] filterToArray) {
		Map<String, Object> result = new HashMap<>();
		BigDecimal[] amount = bankLogService.findAmountTotal(filterToArray);
		result.put("totalAmount", amount[0]);
		return result;
	}
}
