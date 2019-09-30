package com.xinbo.fundstransfer.restful;

import java.math.BigDecimal;
import java.util.*;

import com.xinbo.fundstransfer.domain.enums.UserCategory;
import com.xinbo.fundstransfer.domain.pojo.TransAckResult;
import com.xinbo.fundstransfer.domain.pojo.TransMonitorResult;
import com.xinbo.fundstransfer.service.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
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
import com.xinbo.fundstransfer.domain.DynamicSpecifications;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.entity.MonitorStat;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import com.xinbo.fundstransfer.domain.pojo.TransRec;
import com.xinbo.fundstransfer.domain.pojo.TransTo;

/**
 * 账号监控
 */
@RestController
@RequestMapping("/r/accountMonitor")
@SuppressWarnings("WeakerAccess unused")
public class AccountMonitorController extends BaseController {
	private static final Logger logger = LoggerFactory.getLogger(AccountMonitorController.class);
	@Autowired
	private AccountService accountService;
	@Autowired
	private AccountMonitorService accountMonitorService;
	@Autowired
	private AllocateTransService allocateTransService;
	@Autowired
	private TransMonitorService transMonitorService;
	@Autowired
	private HandicapService handicapService;
	private ObjectMapper mapper = new ObjectMapper();

	/**
	 * 分页获取未匹配的流水
	 */
	@RequestMapping("/findFlowList")
	public String findFlowList(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "statusArray", required = false) Integer[] statArr,
			@RequestParam(value = "handicapId", required = false) Integer handicapId,
			@RequestParam(value = "transIn0Out1", required = false) Integer transIn0Out1,
			@RequestParam(value = "accType", required = false) Integer accType,
			@RequestParam(value = "bankType", required = false) String bankType,
			@RequestParam(value = "aliasLike", required = false) String aliasLike,
			@RequestParam(value = "amtBtw", required = false) BigDecimal[] amtBtw,
			@RequestParam(value = "timeBtw", required = false) String[] timeBtw,
			@RequestParam(value = "doing0OrDone1", required = false, defaultValue = "0") Integer doing0OrDone1)
			throws JsonProcessingException {
		try {
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (Objects.isNull(operator)) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			String accountIdStr = request.getParameter("accountId");
			pageSize = pageSize == null ? AppConstants.PAGE_SIZE : pageSize;
			PageRequest pageable = new PageRequest(pageNo, pageSize, Sort.Direction.DESC, "createTime", "id");
			Page<BizBankLog> page = accountMonitorService.findMatchingFlowPage4Acc(handicapId, accType, statArr,
					bankType, aliasLike, amtBtw, timeBtw, transIn0Out1, doing0OrDone1, pageable);
			GeneralResponseData<List<BizBankLog>> response = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			response.setData(page.getContent());
			response.setPage(new Paging(page));
			if (CollectionUtils.isEmpty(page.getContent()) && StringUtils.isNotBlank(accountIdStr)
					&& StringUtils.isNumeric(accountIdStr)) {
				allocateTransService.buildFlowMatching(Integer.valueOf(accountIdStr));
			}
			return mapper.writeValueAsString(response);
		} catch (Exception e) {
			logger.error("分页获取出款账号流水 " + e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败 " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 根据收款流水，获取汇款流水
	 */
	@RequestMapping("/findFrFlowList4ToFlow")
	public String findFrFlowList4ToFlow(@RequestParam(value = "toFlowId") long toFlowId)
			throws JsonProcessingException {
		try {
			GeneralResponseData<List<BizBankLog>> response = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			List<BizBankLog> dataList = accountMonitorService.findFrFlowList4ToFlow(toFlowId);
			if (CollectionUtils.isEmpty(dataList))
				dataList = accountMonitorService.findToFlowList4FrFlow(toFlowId);
			response.setData(dataList);
			return mapper.writeValueAsString(response);
		} catch (Exception e) {
			logger.error("根据收款流水，获取汇款流水 " + e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败 " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 根据流水获取操作记录
	 */
	@RequestMapping("/findRecList4OutAcc")
	public String findRecList4OutAcc(@RequestParam(value = "flowId") long flowId) throws JsonProcessingException {
		try {
			GeneralResponseData<List<TransRec>> response = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			response.setData(accountMonitorService.findRecList4Acc(flowId));
			return mapper.writeValueAsString(response);
		} catch (Exception e) {
			logger.error("分页获取出款账号流水 " + e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败 " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 修改流水为回冲流水
	 */
	@RequestMapping("/alterFlowToRefunding")
	public String alterFlowToRefunding(@RequestParam(value = "flowId") long flowId,
			@RequestParam(value = "remark") String remark) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		try {
			if (operator == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			log.info("alterFlowToRefunding ( flowId：{}  remark：{} ) >> operator:{} 修改流水为回冲流水", flowId, remark,
					operator.getUid());
			accountMonitorService.alterFlowToRefunding(flowId, operator, remark);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			log.error("修改流水为回冲流水 operator:{} flowId:{} remark:{} ,exception:{}",
					Objects.isNull(operator) ? StringUtils.EMPTY : operator.getUid(), flowId, remark, e.getMessage());
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getMessage()));
		}
	}

	/**
	 * 补下发提单
	 */
	@RequestMapping("/makeUpRec4Issue")
	public String makeUpRec4Issue(@RequestParam(value = "fromFlowId") long fromFlowId,
			@RequestParam(value = "toFlowId") long toFlowId, @RequestParam(value = "remark") String remark)
			throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		try {
			if (operator == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			log.info("makeUpRec4Issue ( fromFlowId: {} toFlowId: {}  remark: {} ) >> operator:{} 补下发提单", fromFlowId,
					toFlowId, remark, operator.getUid());
			accountMonitorService.makeUpRec4Issue(fromFlowId, toFlowId, operator, remark);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			try {
				accountMonitorService.makeUpRec4Issue(toFlowId, fromFlowId, operator, remark);
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
			} catch (Exception e1) {
				String opr = Objects.isNull(operator) ? StringUtils.EMPTY : operator.getUid();
				log.error("补下发提单 opr:{} frFlowId:{} toFlowId:{} remark:{} ,exception:{}", opr, fromFlowId, toFlowId,
						remark, e.getMessage());
				return mapper.writeValueAsString(new GeneralResponseData(
						GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e1.getMessage()));
			}
		}
	}

	/**
	 * 修改流水为费用流水
	 */
	@RequestMapping("/alterFlowToFee")
	public String alterFlowToFee(@RequestParam(value = "flowId") long flowId,
			@RequestParam(value = "remark") String remark) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		try {
			if (operator == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			log.info("alterFlowToFee ( flowId: {} remark : {} ) >> operator:{} 修改流水为费用流水", flowId, remark,
					operator.getUid());
			accountMonitorService.alterFlowToFee(flowId, operator, remark);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			log.error("修改流水为费用流水 operator:{} flowId:{} remark:{} ,exception:{}",
					Objects.isNull(operator) ? StringUtils.EMPTY : operator.getUid(), flowId, remark, e.getMessage());
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getMessage()));
		}
	}

	/**
	 * 修改流水为已匹配
	 */
	@RequestMapping("/alterFlowToMatched")
	public String alterFlowToMatched(@RequestParam(value = "flowId") long flowId,
			@RequestParam(value = "recId") long recId, @RequestParam(value = "remark") String remark)
			throws JsonProcessingException {
		SysUser opr = (SysUser) SecurityUtils.getSubject().getPrincipal();
		try {
			if (opr == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			log.info("alterFlowToMatched ( flowId: {} recId: {}  remark: {} ) >> operator:{} 修改流水为已匹配", flowId, recId,
					remark, opr.getUid());
			accountMonitorService.alterFlowToMatched(flowId, recId, opr, remark);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			log.error("修改流水为费用流水 operator:{} flowId:{} remark:{} ,exception:{}",
					Objects.isNull(opr) ? StringUtils.EMPTY : opr.getUid(), flowId, remark, e.getMessage());
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getMessage()));
		}
	}

	/**
	 * 修改流水为额外收入流水
	 */
	@RequestMapping("/alterFlowToInterest")
	public String alterFlowToInterest(@RequestParam(value = "flowId") long flowId,
			@RequestParam(value = "remark") String remark) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		try {
			if (operator == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			log.info("alterFlowToInterest ( flowId: {}  remark: {} ) >> operator:{} 修改流水为额外收入流水", flowId, remark,
					operator.getUid());
			accountMonitorService.alterFlowToInterest(flowId, operator, remark);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			log.error("修改流水为额外收入流水 operator:{} flowId:{} remark:{} ,exception:{}",
					Objects.isNull(operator) ? StringUtils.EMPTY : operator.getUid(), flowId, remark, e.getMessage());
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getMessage()));
		}
	}

	/**
	 * 修改流水为亏损
	 */
	@RequestMapping("/alterFlowToDeficit")
	public String alterFlowToDeficit(@RequestParam(value = "flowId") long flowId,
			@RequestParam(value = "reasonCode") int reasonCode, @RequestParam(value = "remark") String remark)
			throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		try {
			if (operator == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			log.info("alterFlowToDeficit ( flowId: {} reasonCode：{}  remark: {} ) >> operator:{} 修改流水为亏损", flowId,
					reasonCode, remark, operator.getUid());
			accountMonitorService.alterFlowToDeficit(flowId, reasonCode, operator, remark);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			log.error("修改流水为额外收入流水 operator:{} flowId:{} remark:{} ,exception:{}",
					Objects.isNull(operator) ? StringUtils.EMPTY : operator.getUid(), flowId, remark, e.getMessage());
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getMessage()));
		}
	}

	/**
	 * 把该流水标记为外部资金
	 */
	@RequestMapping("/alterFlowToExtFunds")
	public String alterFlowToExtFunds(@RequestParam(value = "flowId") long flowId,
			@RequestParam(value = "remark") String remark) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		try {
			if (operator == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			log.info("alterFlowToExtFunds ( flowId: {}   remark: {} ) >> operator:{} 把该流水标记为外部资金", flowId, remark,
					operator.getUid());
			accountMonitorService.alterFlowToExtFunds(flowId, operator, remark);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			log.error("把该流水标记为外部资金 operator:{} flowId:{} remark:{} ,exception:{}",
					Objects.isNull(operator) ? StringUtils.EMPTY : operator.getUid(), flowId, remark, e.getMessage());
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getMessage()));
		}
	}

	@RequestMapping("/alterFlowToDisposed")
	public String alterFlowToDisposed(@RequestParam(value = "flowId") long flowId,
			@RequestParam(value = "remark") String remark) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		try {
			if (operator == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			log.info("alterFlowToDisposed ( flowId: {}   remark: {} ) >> operator:{} ", flowId, remark,
					operator.getUid());
			accountMonitorService.alterFlowToDisposed(flowId, operator, remark);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			log.error("把该流水标记为已处理 operator:{} flowId:{} remark:{} ,exception:{}",
					Objects.isNull(operator) ? StringUtils.EMPTY : operator.getUid(), flowId, remark, e.getMessage());
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getMessage()));
		}
	}

	/**
	 * 流水添加备注
	 */
	@RequestMapping("/remark4Flow")
	public String remark4Flow(@RequestParam(value = "flowId") long flowId,
			@RequestParam(value = "remark") String remark) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		try {
			if (operator == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			log.info("remark4Flow ( flowId: {}   remark: {} ) >> operator:{} 流水添加备注", flowId, remark,
					operator.getUid());
			accountMonitorService.remark4Flow(flowId, operator, remark);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			log.error("流水添加备注 operator:{} flowId:{} remark:{} ,exception:{}",
					Objects.isNull(operator) ? StringUtils.EMPTY : operator.getUid(), flowId, remark, e.getMessage());
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getMessage()));
		}
	}

	@RequestMapping("/buildTransTo")
	public String buildTransTo(@RequestParam(value = "toId") Integer toId) throws JsonProcessingException {
		try {
			GeneralResponseData<List<TransTo>> res = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			res.setData(allocateTransService.buildTransTo(toId));
			res.setMessage(allocateTransService.buildResistFactors(toId));
			return mapper.writeValueAsString(res);
		} catch (Exception e) {
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  "));
		}
	}

	/**
	 * 为锁定记录添加备注
	 */
	@RequestMapping("/remark4TransLock")
	public String remark4TransLock(@RequestParam(value = "orderNo") String orderNo,
			@RequestParam(value = "remark") String remark) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		try {
			if (Objects.isNull(operator)) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			log.info("remark4TransLock ( orderNo: {}   remark: {} ) >> operator:{} 为锁定记录添加备注", orderNo, remark,
					operator.getUid());
			allocateTransService.remark4TransLock(operator, orderNo, remark);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getMessage()));
		}
	}

	@RequestMapping("/buildTrans")
	public String IssueMonitorStat() throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		try {
			List<MonitorStat> stat = allocateTransService.buildTransStat(operator);
			List<TransTo> detail = allocateTransService.buildTransTo4Transing(operator);
			List<TransTo> need = allocateTransService.buildTransTo4Needing(operator);
			GeneralResponseData<Map<String, Object>> response = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			Map<String, Object> result = new HashMap<>();
			result.put("stat", stat);
			result.put("detail", detail);
			result.put("need", need);
			response.setData(result);
			return mapper.writeValueAsString(response);
		} catch (Exception e) {
			log.error("获取下发监控统计信息失败 operator:{},exception:{}", operator.getUid(), e.getMessage());
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败  " + e.getMessage()));
		}
	}

	/**
	 * 下发监控列表页面
	 */
	@RequestMapping("/IssueList")
	@SuppressWarnings("unchecked")
	public String issueList(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "status") Integer[] statArr,
			@RequestParam(value = "level", required = false) Integer[] level,
			@RequestParam(value = "accType", required = false) Integer accType,
			@RequestParam(value = "bankType", required = false) String bankType,
			@RequestParam(value = "startTime", required = false) String startTime,
			@RequestParam(value = "endTime", required = false) String endTime,
			@RequestParam(value = "accRef", required = false) String accRef,
			@RequestParam(value = "fromType", required = false) Integer[] fromType) throws JsonProcessingException {
		GeneralResponseData<List> response = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		try {
			pageSize = pageSize == null ? AppConstants.PAGE_SIZE : pageSize;
			PageRequest pageRequest = new PageRequest(pageNo, pageSize);
			Map result = accountMonitorService.listBizIncomeRequest(statArr, level, accType, bankType, startTime,
					endTime, sysUser, pageRequest, accRef, fromType);
			Page<Map> page = (Page<Map>) result.get("page");
			Map<String, Object> total = (Map<String, Object>) result.get("total");
			response.setData(page.getContent());
			response.setPage(new Paging(page, total));
			return mapper.writeValueAsString(response);
		} catch (Exception e) {
			logger.error("查询汇总错误：{}", e);
			response = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "服务器异常,请联系运维...");
			return mapper.writeValueAsString(response);
		}
	}

	@RequestMapping("/remark4TransReq")
	public String remark4TransReq(@RequestParam(value = "reqId") Long reqId,
			@RequestParam(value = "remark") String remark) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		try {
			if (Objects.isNull(operator)) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			log.info("remark4TransReq ( reqId: {}   remark: {} ) >> operator:{} ", reqId, remark, operator.getUid());
			allocateTransService.remark4TransReq(operator, reqId, remark);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getMessage()));
		}
	}

	@RequestMapping("/cancelTransAck")
	public String cancelTransAck(@RequestParam(value = "reqId") Long reqId) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		try {
			log.info("cancelTransAck ( reqId: {}  ) >> operator:{} ", reqId, operator.getUid());
			allocateTransService.cancelTransAck(operator, reqId);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getMessage()));
		}
	}

	@RequestMapping("/listAccForAlarm")
	public String listAccForAlarm() throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		Map<Integer, BigDecimal[]> alarm = new HashMap<>();
		allocateTransService.buildFlowOutMatching().forEach((k, v) -> alarm.put(k, v));
		if (CollectionUtils.isEmpty(alarm)) {
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "暂无数据"));
		}
		try {
			Specification<BizAccount> specif = DynamicSpecifications.build(BizAccount.class,
					new SearchFilter("id", SearchFilter.Operator.IN, alarm.keySet().toArray()),
					new SearchFilter("status", SearchFilter.Operator.EQ, AccountStatus.Normal.getStatus()));
			List<BizAccount> list = accountService.findList(null, specif);
			Iterator<BizAccount> iterator = list.iterator();
			while (iterator.hasNext()) {
				BizAccount acc = iterator.next();
				try {// 权限控制
					if (acc.getHandicapId() == null || operator.getHandicap() == null
							|| Objects.equals(operator.getCategory(), UserCategory.ADMIN.getValue())
							|| Objects.equals(handicapService.findZoneByHandiId(acc.getHandicapId()),
									handicapService.findZoneByHandiId(operator.getHandicap()))) {
						BigDecimal[] hv = alarm.get(acc.getId());
						if (Objects.nonNull(hv) && hv.length > 1 && Objects.nonNull(hv[1])
								&& hv[1].compareTo(BigDecimal.ZERO) > 0) {
							acc.setMappedAmount(hv[1]);// mappedAmount==>cnt;
							acc.setMappingAmount(hv[0]);// mappingAmount==> amt;
						} else {
							allocateTransService.buildFlowMatching(acc.getId());
							iterator.remove();
						}
					} else {
						iterator.remove();
					}
				} catch (Exception e) {
					BigDecimal[] hv = alarm.get(acc.getId());
					if (Objects.nonNull(hv) && hv.length > 1 && Objects.nonNull(hv[1])
							&& hv[1].compareTo(BigDecimal.ZERO) > 0) {
						acc.setMappingAmount(hv[0]);// mappingAmount==> amt;
						acc.setMappedAmount(hv[1]);// mappedAmount==>cnt;
					} else {
						allocateTransService.buildFlowMatching(acc.getId());
						iterator.remove();
					}
				}
			}
			GeneralResponseData<List<BizAccount>> res = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue(), null);
			res.setData(list);
			return mapper.writeValueAsString(res);
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getMessage()));
		}
	}

	@RequestMapping("/buildTransAckRiskList")
	public String buildTransAckRiskList(@RequestParam(value = "id") Integer id) throws JsonProcessingException {
		try {
			GeneralResponseData<TransMonitorResult<TransAckResult>> res = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			res.setData(transMonitorService.getMonitorRiskResult(id));
			return mapper.writeValueAsString(res);
		} catch (Exception e) {
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  "));
		}
	}

	@RequestMapping("/clearAccAlarm4Risk")
	public String clearAccAlarm4Risk(@RequestParam(value = "id") Integer id) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		log.info("clearAccAlarm4Risk{} >> uid:{}", id, operator.getUid());
		try {
			transMonitorService.resetMonitorRisk(id);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  "));
		}
	}

	@RequestMapping("/doInvalidByBatch")
	public String doInvalidByBatch(@RequestParam(value = "ids") long[] ids) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		log.info("doInvalidByBatch {} >> uid: {}", ids, operator.getUid());
		try {
			Set<Integer> accIds = new HashSet<>();
			for (long flowId : ids) {
				Integer accId = accountMonitorService.alterFlowToInvalid(flowId, operator);
				if (Objects.nonNull(accId))
					accIds.add(accId);
			}
			for (Integer accId : accIds) {
				allocateTransService.buildFlowMatching(accId);
			}
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  "));
		}
	}
}
