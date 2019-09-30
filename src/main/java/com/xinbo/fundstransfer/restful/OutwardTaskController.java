package com.xinbo.fundstransfer.restful;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.net.socket.ActionEventEnum;
import com.xinbo.fundstransfer.component.net.socket.MessageEntity;
import com.xinbo.fundstransfer.component.net.socket.MinaMonitorServer;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.daifucomponent.dto.output.DaifuResult;
import com.xinbo.fundstransfer.daifucomponent.entity.DaifuConfigRequest;
import com.xinbo.fundstransfer.daifucomponent.service.Daifu4OutwardService;
import com.xinbo.fundstransfer.domain.*;
import com.xinbo.fundstransfer.domain.GeneralResponseData.ResponseStatus;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.*;
import com.xinbo.fundstransfer.domain.enums.UserCategory;
import com.xinbo.fundstransfer.domain.pojo.*;
import com.xinbo.fundstransfer.domain.repository.DaifuConfigRequestRepository;
import com.xinbo.fundstransfer.report.SystemAccountManager;
import com.xinbo.fundstransfer.restful.api.pojo.ApiIncome;
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
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/r/outtask")
public class OutwardTaskController extends BaseController {
	private static final Logger logger = LoggerFactory.getLogger(OutwardTaskController.class);
	private OutwardTaskService outwardTaskService;
	private AllocateOutwardTaskService outwardTaskAllocateService;
	private SysUserProfileService sysUserProfileService;
	private OutwardRequestService outwardRequestService;
	private HandicapService handicapService;
	private LevelService levelService;
	private SysUserService sysUserService;
	private AccountService accountService;
	private SystemSettingService systemSettingService;
	private static ObjectMapper mapper;
	private BankLogService bankLogService;
	private TransactionLogService transactionLogService;
	private TransMonitorService transMonitorService;
	private static SysDataPermissionService sysDataPermissionService;
	private AsignFailedTaskService asignFailedTaskService;
	@Autowired
	MinaMonitorServer minaMonitorServer;
	@Autowired
	AllocateTransService allocateTransService;
	@Autowired
	SystemAccountManager systemAccountManager;
	@Autowired
	private Daifu4OutwardService daifu4OutwardService;
	@Autowired
	private DaifuConfigRequestRepository daifuConfigRequestRepository;
	@Autowired
	private SplitOrderService splitOrderService;

	@Autowired
	private IncomeRequestService incomeRequestService;

	@Autowired
	public OutwardTaskController(SysDataPermissionService sysDataPermissionService,
			TransactionLogService transactionLogService, OutwardTaskService outwardTaskService,
			AllocateOutwardTaskService outwardTaskAllocateService, SysUserProfileService sysUserProfileService,
			OutwardRequestService outwardRequestService, HandicapService handicapService, LevelService levelService,
			SysUserService sysUserService, AccountService accountService, SystemSettingService systemSettingService,
			ObjectMapper mapper, BankLogService bankLogService, TransMonitorService transMonitorService,
			AsignFailedTaskService asignFailedTaskService) {
		this.sysDataPermissionService = sysDataPermissionService;
		this.transactionLogService = transactionLogService;
		this.outwardTaskService = outwardTaskService;
		this.outwardTaskAllocateService = outwardTaskAllocateService;
		this.sysUserProfileService = sysUserProfileService;
		this.outwardRequestService = outwardRequestService;
		this.handicapService = handicapService;
		this.levelService = levelService;
		this.sysUserService = sysUserService;
		this.accountService = accountService;
		this.systemSettingService = systemSettingService;
		this.mapper = mapper;
		this.bankLogService = bankLogService;
		this.transMonitorService = transMonitorService;
		this.asignFailedTaskService = asignFailedTaskService;
	}

	/**
	 * 上传回执单 参数 任务id 交易流水id
	 */
	@RequestMapping("/uploadReceiptForTask")
	public String uploadReceiptForTask(@RequestParam(value = "taskId") Long taskId) throws JsonProcessingException {
		GeneralResponseData<String> responseData;
		try {
			if (taskId == null) {
				responseData = new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "网络异常，请稍后");
				return mapper.writeValueAsString(responseData);
			}
			BizTransactionLog bizTransactionLog = transactionLogService.findByOrderIdAndType(taskId,
					TransactionLogType.OUTWARD.getType());
			if (bizTransactionLog != null && bizTransactionLog.getFromBanklogId() != null) {
				try {
					// 调用上传回执service
					BizBankLog o = bankLogService.get(bizTransactionLog.getFromBanklogId());
					// 特殊处下，将出款任务ID放在流水的status字段存储，后续要根据任务ID更新回执截图
					o.setStatus(taskId.intValue());
					MessageEntity<BizBankLog> msg = new MessageEntity<BizBankLog>();
					msg.setAction(ActionEventEnum.RECEIPT.ordinal());
					msg.setData(o);
					String ip = minaMonitorServer.getIPByAccountId(o.getFromAccount());
					msg.setIp(ip);
					logger.info("------------->上传回执 IP:" + ip);
					if (StringUtils.isEmpty(ip)) {
						logger.warn(">>>>>>>>>>>>>>> 警告：获取不到指定IP，将全量推送消息");
					}
					redisService.convertAndSend(RedisTopics.PUSH_MESSAGE_TOOLS, mapper.writeValueAsString(msg));
					responseData = new GeneralResponseData<>(ResponseStatus.SUCCESS.getValue(),
							"已通知工具人工上传回执单，请稍候刷新页面查看回执");
					return mapper.writeValueAsString(responseData);
				} catch (Exception e) {
					logger.error("上传回执失败：{}", e);
					responseData = new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "系统错误");
					return mapper.writeValueAsString(responseData);
				}
			} else {
				responseData = new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "无相应流水记录，无法上传回执:"
						+ (bizTransactionLog == null || bizTransactionLog.getFromBanklogId() == null));
				return mapper.writeValueAsString(responseData);
			}
		} catch (JsonProcessingException e) {
			logger.error(" OutwardTaskController.uploadReceiptForTask error: ", e);
			responseData = new GeneralResponseData<>(ResponseStatus.FAIL.getValue(),
					" 上传回执异常:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	/**
	 * 备注
	 *
	 * @param taskId
	 * @param remark
	 * @param type
	 *            type==review 时 表示任务排查添加备注
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/remark")
	public String remark(@RequestParam(value = "taskId") Long taskId, @RequestParam(value = "remark") String remark,
			@RequestParam(value = "type", required = false) String type) throws JsonProcessingException {
		try {
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (operator == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆"));
			}
			if (taskId == null || StringUtils.isBlank(remark)) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请填写备注或者其他参数错误"));
			}
			if (StringUtils.isNotBlank(type) && "req".equals(type)) {
				BizOutwardRequest request = outwardRequestService.get(taskId);
				if (request == null) {
					return mapper.writeValueAsString(
							new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "订单不存在!"));
				}
				remark = CommonUtils.genRemark(request.getRemark(), remark, new Date(), operator.getUid());
				request.setRemark(remark);
				outwardRequestService.update(request);
				return mapper.writeValueAsString(new GeneralResponseData(ResponseStatus.SUCCESS.getValue(), "添加成功!"));
			}
			BizOutwardTask bizOutwardTask = outwardTaskService.findById(taskId);
			if (bizOutwardTask == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "任务不存在"));
			}
			if (StringUtils.isNotBlank(type) && "review".equals(type)) {
				if (bizOutwardTask.getStatus().equals(OutwardTaskStatus.Failure.getStatus())
						|| bizOutwardTask.getStatus().equals(OutwardTaskStatus.ManagerDeal.getStatus())) {
					asignFailedTaskService.updateRemark(operator.getId(), taskId.intValue(), CommonUtils.genRemark(
							bizOutwardTask.getRemark(), StringUtils.trim(remark), new Date(), operator.getUid()));
				}
			}
			outwardTaskAllocateService.remark4Custom(taskId, operator, StringUtils.trim(remark));
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功"));
		} catch (Exception e) {
			logger.error("操作失败：异常 ", e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getMessage()));
		}
	}

	// 新增会员到永久出款队列
	@RequestMapping("/artificialoutmoney")
	public String artificialOutMoney(@RequestParam(value = "toaccount") String toAccount)
			throws JsonProcessingException {
		try {
			logger.info("新增会员到永久出款队列 toAccount:{} ", toAccount);
			outwardTaskService.manualOutMoney(toAccount);
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功"));
		} catch (Exception e) {
			logger.error("操作失败：异常 ", e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getMessage()));
		}
	}

	// 删除会员从永久出款队列
	@RequestMapping("/cancelartificial")
	public String cancelArtificial(@RequestParam(value = "toaccount") String toAccount) throws JsonProcessingException {
		try {
			logger.info("删除会员从永久出款队列 toAccount:{}", toAccount);
			outwardTaskService.cancelArtificial(toAccount);
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功"));
		} catch (Exception e) {
			logger.error("操作失败：异常 ", e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getMessage()));
		}
	}

	/**
	 * 待排查 重新生成 更新任务状态 生成新的任务
	 *
	 * @param taskId
	 *            old task id
	 * @param remark
	 *            remark added by user for the new task to be created
	 * @param type
	 *            new task will be dealed by the manner : robot manual third
	 * @param bankType
	 *            new task will be drawn by the out card bank type
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/recreate")
	public String recreateTask(@RequestParam(value = "taskId") Long taskId,
			@RequestParam(value = "remark") String remark, @RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "bankType", required = false) String[] bankType) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (operator == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆"));
		}
		if (StringUtils.isEmpty(remark)) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请添加备注"));
		}
		BizOutwardTask bizOutwardTask = outwardTaskService.findById(taskId);
		BizOutwardRequest req = outwardRequestService.get(bizOutwardTask.getOutwardRequestId());
		if (bizOutwardTask == null || req == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "该务不存在"));
		}
		if (bizOutwardTask.getStatus() != null
				&& !bizOutwardTask.getStatus().equals(OutwardTaskStatus.Failure.getStatus())) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "任务状态已变更,请刷新!"));
		}
		// 代付订单 如果是正在支付 或 未知 则不能重新生成
		if (bizOutwardTask.getThirdInsteadPay() != null && bizOutwardTask.getThirdInsteadPay() == 1) {
			DaifuResult result = daifu4OutwardService.query(bizOutwardTask);
			boolean flag = !ObjectUtils.isEmpty(result)
					&& (result.getResult().getValue().equals(DaifuResult.ResultEnum.PAYING.getValue())
							|| result.getResult().getValue().equals(DaifuResult.ResultEnum.UNKOWN.getValue()));
			if (flag) {
				return mapper.writeValueAsString(new GeneralResponseData<>(
						GeneralResponseData.ResponseStatus.FAIL.getValue(), "第三方代付中不能重新生成任务!"));
			}
		}

		try {
			outwardTaskAllocateService.alterStatusToInvalid(taskId, operator, remark, bankType, type);
			asignFailedTaskService.updateReviewTask(taskId.intValue(),
					CommonUtils.genRemark(bizOutwardTask.getRemark(), remark, new Date(), operator.getUid()),
					operator.getId());
			// 判断是否第三方出款。
			String thirdOutRemark = StringUtils.isNotBlank(bizOutwardTask.getRemark()) ? bizOutwardTask.getRemark()
					: "";
			if (thirdOutRemark.indexOf("{") > -1) {
				// 取最后一个 出款数据
				String newStr = thirdOutRemark.substring(thirdOutRemark.lastIndexOf("{") + 1,
						thirdOutRemark.lastIndexOf("}"));
				String[] remarkValue = newStr.split("\\|");
				// 判断是否存在多个第三方账号出款
				if (remarkValue.length > 1) {
					for (int j = 0; j < remarkValue.length; j++) {
						BigDecimal thirdOutAmount = new BigDecimal("-" + remarkValue[j].split(",")[2].split(":")[1]);
						int thirdId = Integer.valueOf(remarkValue[j].split(",")[3].split(":")[1].replace("\r\n", ""));
						outwardRequestService.saveThirdOut(thirdId, thirdOutAmount, req, operator, remark);
					}
				} else {
					remarkValue = newStr.split(",");
					if (remarkValue.length > 1) {
						BigDecimal thirdOutAmount = new BigDecimal("-" + remarkValue[2].split(":")[1]);
						int thirdId = Integer.valueOf(remarkValue[3].split(":")[1].replace("\r\n", ""));
						outwardRequestService.saveThirdOut(thirdId, thirdOutAmount, req, operator, remark);
					}
				}
			}
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功"));
		} catch (Exception e) {
			logger.error("重新生成新任务失败：taskId：{}，operator：{},e:{}", taskId, operator.getId(), e.getLocalizedMessage());
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败:" + e.getLocalizedMessage()));
		}

	}

	/**
	 * 转待排查
	 */
	@RequestMapping("/turnToFail")
	public String turnToFail(@RequestParam(value = "taskId") Long taskId, @RequestParam(value = "remark") String remark,
			@RequestParam(value = "platFormQuery", required = false) Byte platFormQuery)
			throws JsonProcessingException {
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		boolean isNoPlatFormQuery = platFormQuery == null || platFormQuery.intValue() != 1;
		if (sysUser == null && isNoPlatFormQuery) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		if (!StringUtils.isNotBlank(remark)) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请填写备注"));
		}
		BizOutwardTask bizOutwardTask = outwardTaskService.findById(taskId);
		if (bizOutwardTask == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "任务不存在"));
		}
		if (!bizOutwardTask.getStatus().equals(OutwardTaskStatus.Failure.getStatus())) {

			outwardTaskAllocateService.alterStatusToFail(taskId, sysUser, remark);
			if (bizOutwardTask.getOperator() == null) {
				// 非人工出款才会分配任务排查
				asignFailedTaskService.asignOnTurnToFail(bizOutwardTask.getId());
			}
		}
		return mapper.writeValueAsString(
				new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功"));
	}

	@RequestMapping("/noticePlatForm")
	public String noticePlatForm(@RequestParam(value = "taskId") Long taskId) throws JsonProcessingException {
		GeneralResponseData<String> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "通知平台成功");
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		BizOutwardTask task = outwardTaskService.findById(taskId);
		if (task == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "任务不存在");
			return mapper.writeValueAsString(responseData);
		}
		BizOutwardRequest req = outwardRequestService.get(task.getOutwardRequestId());
		outwardTaskAllocateService.noticePlatIfFinished(sysUser.getId(), req);
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 通过id 获取出款任务
	 */
	@RequestMapping("/findById")
	public String findById(@RequestParam(value = "id") Long id) throws JsonProcessingException {
		GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "获取成功");
		try {
			BizOutwardTask bizOutwardTask = outwardTaskService.findById(id);
			if (bizOutwardTask != null && bizOutwardTask.getOutwardRequestId() != null) {
				BizOutwardRequest bizOutwardRequest = outwardRequestService.get(bizOutwardTask.getOutwardRequestId());
				if (bizOutwardRequest != null) {
					Map<String, Object> map = new HashMap<>();
					map.put("orderNo", bizOutwardRequest.getOrderNo());
					map.put("memberCode", bizOutwardRequest.getMemberCode());
					responseData.setData(map);
				}
			}
		} catch (Exception e) {
			logger.debug("通过id获取出款任务失败：{},参数：{}", e, id);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "获取失败");
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 查询机器人出款信息
	 */
	@RequestMapping("/machine")
	public String getMachineTask() throws JsonProcessingException {
		GeneralResponseData<List<Map<String, Object>>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		try {
			// 查询机器人处理的记录 返回 机器人id 处理总数
			List<?> list = outwardTaskService.findListJoinUser();
			if (list != null && list.size() > 0) {
				List<Map<String, Object>> resList = new ArrayList<>();
				for (Object obj : list) {
					Map<String, Object> map = new LinkedHashMap<>();
					Object[] arr = (Object[]) obj;
					SysUser sysUser = sysUserService.findFromCacheById((Integer) arr[0]);
					// 根据机器人id查询出款信息
					List<BizOutwardTask> bizOutwardTaskList = null;
					if (sysUser != null && sysUser.getId() != null) {
						Specification<BizOutwardTask> specification = DynamicSpecifications.build(BizOutwardTask.class,
								new SearchFilter("operator", SearchFilter.Operator.EQ, sysUser.getId()));
						Sort sort = new Sort(Sort.Direction.DESC, "asignTime");
						bizOutwardTaskList = outwardTaskService.findList(specification, sort);
					}
					if (bizOutwardTaskList != null && bizOutwardTaskList.size() > 0) {
						BizOutwardTask bizOutwardTask = bizOutwardTaskList.get(0);
						map.put("robotNo", sysUser.getUid() != null ? sysUser.getUid() : "");
						// 查询分配给机器人的 出款卡信息
						BizAccount bizAccount = accountService.getById(bizOutwardTask.getAccountId());
						map.put("bankName",
								bizAccount != null && StringUtils.isNotBlank(bizAccount.getBankName())
										? bizAccount.getBankName()
										: "");
						// 查询当日出款额度
						SysUserProfile sysUserProfile = sysUserProfileService
								.findByUserIdAndPropertyKey(sysUser.getId(), "LIMIT_OUT_DAILY");
						BigDecimal limit = sysUserProfile != null
								&& StringUtils.isNotBlank(sysUserProfile.getPropertyValue())
										? new BigDecimal(sysUserProfile.getPropertyValue())
										: new BigDecimal(0);
						// 当日出款额度-出款额度 = 额度余额
						map.put("balance", limit.subtract(new BigDecimal(
								String.valueOf(bizOutwardTask.getAmount() != null ? bizOutwardTask.getAmount() : 0))));
						map.put("outCount", arr[1]);
						String status = "正在出款";
						map.put("status", status);
						// 查询出款请求信息
						BizOutwardRequest bizOutwardRequest = outwardRequestService
								.get(bizOutwardTask.getOutwardRequestId());
						if (bizOutwardRequest != null) {
							map.put("orderNo", bizOutwardRequest.getOrderNo());
							BizHandicap bizHandicap = handicapService
									.findFromCacheById(bizOutwardRequest.getHandicap());
							map.put("handicap", bizHandicap.getName());
							map.put("member", bizOutwardRequest.getMember());
						}
					}
					resList.add(map);
				}
				responseData.setData(resList);
			}
		} catch (Exception e) {
			logger.error("查询机器人出款失败:{}", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询失败");
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 通过出款请求id 获取出款任务 出款请求信息 用于显示 取消按钮 ,拒绝按钮 界面
	 */
	@RequestMapping("/getById")
	public String getTaskById(@RequestParam(value = "id") Long taskId,
			@RequestParam(value = "outRequestId") Long outRequestId) throws JsonProcessingException {
		GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		try {
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			Map<String, Object> map = new LinkedHashMap<>();
			BizOutwardRequest bizOutwardRequest = outwardRequestService.get(outRequestId);
			// Sort sort = new Sort(Sort.Direction.ASC, "asignTime");
			BizOutwardTask bizOutwardTask = outwardTaskService.findById(taskId);
			if (bizOutwardRequest != null) {
				Double splitBoundary = systemSettingService.getOutDrawLimitApproveInCache("OUTDRAW_LIMIT_APPROVE");
				map.put("splitBoundary", splitBoundary);
				// 订单信息
				map.put("orderNo",
						StringUtils.isNotBlank(bizOutwardRequest.getOrderNo()) ? bizOutwardRequest.getOrderNo() : "");
				BizHandicap bizHandicap = handicapService.findFromCacheById(bizOutwardRequest.getHandicap());
				map.put("handicap",
						null != bizHandicap && StringUtils.isNotBlank(bizHandicap.getName()) ? bizHandicap.getName()
								: "");
				BizLevel bizLevel = levelService.findFromCache(bizOutwardRequest.getLevel());
				map.put("level",
						bizLevel != null && StringUtils.isNotBlank(bizLevel.getName()) ? bizLevel.getName() : "");
				map.put("member", bizOutwardRequest.getMember());
				// 出款信息
				map.put("toAccountOwner", bizOutwardRequest.getToAccountOwner());
				map.put("toAccountBank", bizOutwardRequest.getToAccountBank());
				map.put("toAccountName", bizOutwardRequest.getToAccountName());
				map.put("toAccountNo", bizOutwardRequest.getToAccount());
				if (bizOutwardTask != null) {
					map.put("type", bizOutwardTask.getOperator() == null ? "机器出款" : "人工出款");
					map.put("asignTime", bizOutwardRequest.getCreateTime());
					map.put("amount", bizOutwardTask.getAmount());
					map.put("taskId", bizOutwardTask.getId());
				}
				map.put("userId", sysUser.getId());
				map.put("orderNo", bizOutwardRequest.getOrderNo());
				map.put("memberCode", bizOutwardRequest.getMemberCode());
				responseData.setData(map);
			}
		} catch (Exception e) {
			logger.error("获取失败：{}", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue());
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 查询无记录的时候 返回
	 *
	 * @return
	 * @throws JsonProcessingException
	 */
	private String retEmptyResults() throws JsonProcessingException {
		GeneralResponseData<?> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "无数据");
		responseData.setPage(new Paging());
		responseData.setData(null);
		return mapper.writeValueAsString(responseData);
	}

	private String returnCommon(OutwardTaskTotalInputDTO inputDTO) throws JsonProcessingException {
		String ret = "";
		Integer[] fromAccount;
		if ("outDrawing".equals(inputDTO.getFlag()) || "masterOut".equals(inputDTO.getFlag())
				|| "failedOut".equals(inputDTO.getFlag())) {
			if (StringUtils.isNotBlank(inputDTO.getAccountAlias()) || StringUtils.isNotBlank(inputDTO.getBankType())
					|| StringUtils.isNotBlank(inputDTO.getLevel())) {
				fromAccount = fromAccount4OutDrawing(inputDTO.getAccountAlias(), inputDTO.getBankType(),
						inputDTO.getLevel());
				if (fromAccount == null || fromAccount.length == 0) {
					ret = retEmptyResults();
				}
			}
		} else {
			if (inputDTO.getRobot() != null || inputDTO.getPhone() != null
					|| StringUtils.isNotBlank(inputDTO.getAccountAlias())) {// 如果有选人工
				// 则不能直接返回无数据
				fromAccount = fromAccount4Total(inputDTO);
				if (fromAccount == null || fromAccount.length == 0) {
					if (inputDTO.getManual() != null && StringUtils.isNotBlank(inputDTO.getAccountAlias())) {
						fromAccount = fromAccount(inputDTO.getAccountAlias());
						if ((fromAccount != null && fromAccount.length > 0)
								&& (inputDTO.getRobot() != null || inputDTO.getPhone() != null)) {
							inputDTO.setOperatorType("manual");
						}
					}
				}
				if (fromAccount == null || fromAccount.length == 0) {
					ret = retEmptyResults();
				}
			}
		}
		return ret;
	}

	private Integer[] getFromAccount(OutwardTaskTotalInputDTO inputDTO) {
		Integer[] fromAccount = null;
		if ("outDrawing".equals(inputDTO.getFlag()) || "masterOut".equals(inputDTO.getFlag())
				|| "failedOut".equals(inputDTO.getFlag())) {
			if (StringUtils.isNotBlank(inputDTO.getAccountAlias()) || StringUtils.isNotBlank(inputDTO.getBankType())
					|| StringUtils.isNotBlank(inputDTO.getLevel())) {
				fromAccount = fromAccount4OutDrawing(inputDTO.getAccountAlias(), inputDTO.getBankType(),
						inputDTO.getLevel());
			}
		} else {
			if (inputDTO.getRobot() != null || inputDTO.getPhone() != null
					|| StringUtils.isNotBlank(inputDTO.getAccountAlias())) {// 如果有选人工
				// 则不能直接返回无数据
				fromAccount = fromAccount4Total(inputDTO);
				if (fromAccount == null || fromAccount.length == 0) {
					if (inputDTO.getManual() != null && StringUtils.isNotBlank(inputDTO.getAccountAlias())) {
						fromAccount = fromAccount(inputDTO.getAccountAlias());
						if ((fromAccount != null && fromAccount.length > 0)
								&& (inputDTO.getRobot() != null || inputDTO.getPhone() != null)) {
							inputDTO.setOperatorType("manual");
						}
					}
				}
			}
		}
		return fromAccount;
	}

	private Map<String, Object> wrapMap(Map<String, Object> map, AccountBaseInfo bizAccount) {
		if (bizAccount != null) {
			map.put("outAccount", StringUtils.isNotBlank(bizAccount.getAccount()) ? bizAccount.getAccount() : "");
			map.put("outAccountAlias", StringUtils.isNotBlank(bizAccount.getAlias()) ? bizAccount.getAlias() : "");
			map.put("outAccountOwner",
					StringUtils.isNotBlank(bizAccount.getOwner()) ? StringUtils.trim(bizAccount.getOwner()) : "");
			map.put("outAccountType", StringUtils.isNotBlank(bizAccount.getBankType()) ? bizAccount.getBankType() : "");
			map.put("accountId", bizAccount.getId());
			if (bizAccount.getType().equals(AccountType.InBank.getTypeId())) {
				map.put("operator", "入款卡");
			} else if (bizAccount.getType().equals(AccountType.ReserveBank.getTypeId())) {
				map.put("operator", "备用卡");
			} else if (bizAccount.getType().equals(AccountType.BindCustomer.getTypeId())) {
				map.put("operator", "客户卡");
			} else {
				if (bizAccount.getFlag() != null && bizAccount.getFlag() == 1) {
					map.put("operator", "返利网");
				}
			}
		} else {
			map.put("outAccount", "");
			map.put("outAccountAlias", "");
			map.put("outAccountOwner", "");
			map.put("outAccountType", "");
			map.put("account", "");
		}
		return map;
	}

	private Map<String, Object> wrapMap2(Map<String, Object> map, boolean time3To15, boolean timeOver15) {
		if (time3To15) {
			map.put("successOutTime3To15", true);
		} else if (timeOver15) {
			map.put("successOutTime15", true);
		} else {
			map.put("successOutTimeOther", true);
		}
		return map;
	}

	private String getOperatorType(OutwardTaskTotalInputDTO inputDTO) {
		if (StringUtils.isNotBlank(inputDTO.getOperatorType())) {
			return inputDTO.getOperatorType();
		}
		final String successOut = "successOut";
		final String failedOut = "failedOut";
		if (successOut.equals(inputDTO.getFlag())) {
			boolean check = (null != inputDTO.getRobot() || null != inputDTO.getPhone())
					&& null != inputDTO.getManual();
			if (check) {
				if (StringUtils.isNotBlank(inputDTO.getAccountAlias())) {
					// 有输入出款账号,人工,手机/PC
					inputDTO.setOperatorType("mra");
				} else {
					// 没有输入出款账号
					inputDTO.setOperatorType("mr");
				}
			} else {
				if (null != inputDTO.getRobot()) {
					// 完成出款页面 包含手机 和 PC
					inputDTO.setOperatorType("robot");
				} else if (null != inputDTO.getPhone()) {
					inputDTO.setOperatorType("phone");
				} else if (null != inputDTO.getManual()) {
					inputDTO.setOperatorType("manual");
				}
			}
		} else {
			if (null != inputDTO.getRobot()) {
				inputDTO.setOperatorType("robot");
			}
			if (null != inputDTO.getManual()) {
				inputDTO.setOperatorType("manual");
			}
			if (failedOut.equals(inputDTO.getFlag())) {
				if (null != inputDTO.getThirdInsteadPay() && inputDTO.getThirdInsteadPay() == 2) {
					inputDTO.setOperatorType("daifu");
				}
			}
		}
		return inputDTO.getOperatorType();
	}

	final String toOutDraw = "toOutDraw";
	final String successOut = "successOut";
	final String outDrawing = "outDrawing";
	final String failedOut = "failedOut";
	final String masterOut = "masterOut";

	/**
	 * @desc 出款任务汇总页签 (不论出款成功与失败，这笔出款任务都是已经审核了的) 正在出款 未出款 完成出款(1 5 6) 主管处理( 2 ) 出款失败
	 *       主管取消 3 主管拒绝 4 asignTime timeConsume 伴随着状态转变贯穿整个出款任务的始终 最新的asignTime
	 *       就是当前操作的时间，timeConsume就是当前的asignTime减去上一个操作的asignTime
	 */
	@RequestMapping("/total")
	public String total(OutwardTaskTotalInputDTO inputDTO) throws JsonProcessingException {
		GeneralResponseData<List<Map<String, Object>>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		SysUser sysUser1 = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser1 == null
				&& (inputDTO.getPlatFormQueryFlag() == null || inputDTO.getPlatFormQueryFlag().intValue() != 1)) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		List<String> handicapCodeList = sysDataPermissionService.handicapCodeList(inputDTO.getHandicap(), sysUser1);
		if (handicapCodeList == null || handicapCodeList.size() == 0) {
			return retEmptyResults();
		}

		try {
			Integer[] operatorIds = null;
			if (StringUtils.isNotBlank(inputDTO.getOperatorName())) {
				operatorIds = operatorIds(inputDTO.getOperatorName());
				if (operatorIds == null || operatorIds.length == 0) {
					return retEmptyResults();
				}

			}
			String ret = returnCommon(inputDTO);
			if (StringUtils.isNotBlank(ret)) {
				return ret;
			}
			Integer[] fromAccount = getFromAccount(inputDTO);
			// 人工 机器 手机(完成出款页签) 代付(待排查)
			String operatorType = getOperatorType(inputDTO);
			String level = !outDrawing.equals(inputDTO.getFlag()) && !masterOut.equals(inputDTO.getFlag())
					&& !failedOut.equals(inputDTO.getFlag()) ? inputDTO.getLevel() : null;
			Integer[] status = status(inputDTO.getFlag(), inputDTO.getMaintain());
			String sysLevel = inputDTO.getSysLevel();

			// 金额 出款时间 耗时
			String sortStr1 = "amount", sortStr2 = "asignTime", sortStr3 = "timeConsuming";
			Sort sort2;
			if (toOutDraw.equals(inputDTO.getFlag())) {
				sortStr2 = "id";
				sort2 = Sort.by(Sort.Direction.ASC, sortStr2);
			} else {
				sort2 = Sort.by(Sort.Direction.DESC, sortStr2);
			}
			PageRequest pageRequest;
			String drawingType = "";
			if (successOut.equals(inputDTO.getFlag())) {
				// 出款类型 银行卡(bank) 第三方(third) 代付(daifu)
				drawingType = inputDTO.getDrawType();
				Sort sort = sort2;
				if (inputDTO.getSortFlag() != null) {
					// sortFlag 1 2 3 降序 4 5 6 升序
					if (inputDTO.getSortFlag() == 1) {
						sort = Sort.by(Sort.Direction.DESC, sortStr1);
					} else if (inputDTO.getSortFlag() == 2) {
						sort = Sort.by(Sort.Direction.DESC, sortStr2);
					} else if (inputDTO.getSortFlag() == 3) {
						sort = Sort.by(Sort.Direction.DESC, sortStr3);
					} else if (inputDTO.getSortFlag() == 4) {
						sort = Sort.by(Sort.Direction.ASC, sortStr1);
					} else if (inputDTO.getSortFlag() == 5) {
						sort = Sort.by(Sort.Direction.ASC, sortStr2);
					} else if (inputDTO.getSortFlag() == 6) {
						sort = Sort.by(Sort.Direction.ASC, sortStr3);
					}
				}
				pageRequest = PageRequest.of(inputDTO.getPageNo(),
						inputDTO.getPageSize() == null ? AppConstants.PAGE_SIZE : inputDTO.getPageSize(), sort);
			} else {
				if (outDrawing.equals(inputDTO.getFlag())) {
					// 为了把超时单子放在前面 asignTime 升序
					// 默认按照 时间 降序
					Sort sort1 = Sort.by(Sort.Direction.ASC, sortStr2);
					pageRequest = PageRequest.of(inputDTO.getPageNo(),
							inputDTO.getPageSize() == null ? AppConstants.PAGE_SIZE : inputDTO.getPageSize(), sort1);
				} else {
					pageRequest = PageRequest.of(inputDTO.getPageNo(),
							inputDTO.getPageSize() == null ? AppConstants.PAGE_SIZE : inputDTO.getPageSize(), sort2);
				}
			}
			Page<BizOutwardTask> page = outwardTaskService.findOutwardTaskPageNoCount(handicapCodeList, level,
					inputDTO.getOrderNo(), inputDTO.getMember(), drawingType, operatorIds, fromAccount,
					inputDTO.getStartTime(), inputDTO.getEndTime(), inputDTO.getFromMoney(), inputDTO.getToMoney(),
					operatorType, status, pageRequest, sysLevel);
			List<Map<String, Object>> list = new LinkedList<>(), list2 = null, list3 = null;
			if (page != null && page.getContent() != null && page.getContent().size() > 0) {
				List<BizOutwardTask> outwardTaskList = page.getContent();
				if (successOut.equals(inputDTO.getFlag())) {
					list2 = new ArrayList<>();
					list3 = new ArrayList<>();
				}
				long now = System.currentTimeMillis();
				for (BizOutwardTask task : outwardTaskList) {
					Map<String, Object> map = new HashMap<>();
					BizOutwardRequest bizOutwardRequest = outwardRequestService.get(task.getOutwardRequestId());
					if (bizOutwardRequest != null) {
						map.put("requestStatus", bizOutwardRequest.getStatus());
					}
					boolean thirdOut = !"successOut".equals(inputDTO.getFlag())
							&& ((StringUtils.isNotBlank(task.getRemark()) && task.getRemark().contains("第三方")
									&& !task.getRemark().contains("第三方转"))
									|| (task.getOperator() != null && task.getAccountId() == null));
					if (thirdOut) {
						if (!masterOut.equals(inputDTO.getFlag())) {
							map.put("thirdRemarkFlag", "yes");
						} else {
							map.put("thirdRemarkFlag", "no");
						}
					} else {
						// 由于现在有第三方转出人工或者机器出款,所以在主管处理里面完成出款不分第三方出款
						map.put("thirdRemarkFlag", "no");
					}
					map.put("toAccountNo",
							bizOutwardRequest != null && StringUtils.isNotBlank(bizOutwardRequest.getToAccount())
									? bizOutwardRequest.getToAccount()
									: "");
					// 判断会员出款账号是否在永久人工出款里
					if (bizOutwardRequest != null && StringUtils.isNotBlank(bizOutwardRequest.getToAccount())
							&& outwardTaskService.checkManualOut4Member(bizOutwardRequest.getToAccount())) {
						map.put("isArtificialOutMoney", "true");
					} else {
						map.put("isArtificialOutMoney", "false");
					}
					AccountBaseInfo bizAccount = null;
					if (task.getAccountId() != null) {
						if (null != task.getThirdInsteadPay() && task.getThirdInsteadPay() == 1) {
							DaifuConfigRequest daifuConfigRequest = daifuConfigRequestRepository
									.findOne(task.getAccountId());

							if (!ObjectUtils.isEmpty(daifuConfigRequest)) {
								logger.debug("代付订单 账号信息:{}", daifuConfigRequest);
								map.put("account",
										StringUtils.isNotBlank(daifuConfigRequest.getMemberId())
												? daifuConfigRequest.getMemberId()
												: "");

								map.put("accountId", daifuConfigRequest.getId());
								map.put("outAccount",
										StringUtils.isNotBlank(daifuConfigRequest.getMemberId())
												? daifuConfigRequest.getMemberId()
												: "");
								map.put("outAccountAlias", "");
								map.put("outAccountOwner", "");
								map.put("outAccountType",
										StringUtils.isNotBlank(daifuConfigRequest.getChannelName())
												? daifuConfigRequest.getChannelName()
												: "");
							}
						} else {
							bizAccount = accountService.getFromCacheById(task.getAccountId());
						}
					}
					if (bizAccount != null) {
						map = wrapMap(map, bizAccount);
					}
					if (!toOutDraw.equals(inputDTO.getFlag())) {
						if (task.getOperator() != null) {
							if (task.getOperator() == AppConstants.USER_ID_4_ADMIN && null != task.getThirdInsteadPay()
									&& task.getThirdInsteadPay() == 1) {
								map.put("operator", "代付");
								map.put("operatorType", "代付");
							} else {
								SysUser sysUser = sysUserService.findFromCacheById(task.getOperator());
								String uid = sysUser == null || StringUtils.isBlank(sysUser.getUid()) ? ""
										: sysUser.getUid();
								boolean thirdManual = OutWardPayType.ThirdPay.equals(task.getThirdInsteadPay())
										|| task.getAccountId() == null;
								if (StringUtils.isNotBlank(uid)) {
									if (thirdManual) {
										map.put("operator", "(" + uid + ")三方");
										map.put("operatorType", "三方");
									} else {
										map.put("operator", "(" + uid + ")人工");
										map.put("operatorType", "人工");
									}
								} else {
									map.put("operator", "");
									map.put("operatorType", "");
								}
							}

						} else {
							if (successOut.equals(inputDTO.getFlag())) {
								if (inputDTO.getPhone() != null && inputDTO.getRobot() == null && bizAccount != null
										&& bizAccount.getFlag() != null && bizAccount.getFlag() == 2
										&& (task.getThirdInsteadPay() == null || task.getThirdInsteadPay() != 1)) {
									map.put("operator", "手机");// 返利网
									map.put("operatorType", "机器");
								}
								if (inputDTO.getPhone() == null && inputDTO.getRobot() != null && bizAccount != null
										&& bizAccount.getFlag() != null && bizAccount.getFlag() != 2
										&& (task.getThirdInsteadPay() == null || task.getThirdInsteadPay() != 1)) {

									map.put("operator", "PC");
									map.put("operatorType", "机器");
								}
								if (inputDTO.getPhone() == null && inputDTO.getRobot() == null) {
									if (task.getThirdInsteadPay() != null
											&& task.getThirdInsteadPay().intValue() == 1) {
										map.put("operator", "代付");
										map.put("operatorType", "代付");
									} else {
										if (bizAccount != null && bizAccount.getFlag() != null
												&& bizAccount.getFlag() == 2) {
											map.put("operator", "手机");// 返利网
											map.put("operatorType", "机器");
										} else {
											map.put("operator", "PC");
											map.put("operatorType", "机器");
										}
									}
								}
							} else {
								if (task.getThirdInsteadPay() != null && task.getThirdInsteadPay().intValue() == 1) {
									map.put("operator", "代付");
									map.put("operatorType", "代付");
								} else {
									if (bizAccount != null && bizAccount.getFlag() != null
											&& bizAccount.getFlag() == 2) {
										map.put("operator", "手机");// 返利网
										map.put("operatorType", "机器");
									} else {
										map.put("operator", "PC");
										map.put("operatorType", "机器");
									}
								}
								// if (outDrawing.equals(inputDTO.getFlag())) {
								// if (null != bizAccount && bizAccount.getFlag() == 2) {
								// map.put("operator", "手机");// 返利网
								// map.put("operatorType", "机器");
								// } else {
								// map.put("operator", "PC");// 机器
								// map.put("operatorType", "机器");
								// }
								// } else {
								// map.put("operator", "PC");// 机器
								// map.put("operatorType", "机器");
								// }
								// map.put("operator", "PC");// 机器
								// map.put("operatorType", "机器");
							}
						}

						// if (successOut.equals(inputDTO.getFlag())) {
						// if (task.getOperator() == null) {
						// if (inputDTO.getPhone() != null && inputDTO.getRobot() == null && bizAccount
						// != null
						// && bizAccount.getFlag() != null && bizAccount.getFlag() == 2
						// && (task.getThirdInsteadPay() == null || task.getThirdInsteadPay() != 1)) {
						// map.put("operatorType", "机器");//返利网
						// }
						// if (inputDTO.getPhone() == null && inputDTO.getRobot() != null && bizAccount
						// != null
						// && bizAccount.getFlag() != null && bizAccount.getFlag() == 2
						// && (task.getThirdInsteadPay() == null || task.getThirdInsteadPay() != 1)) {
						// map.put("operatorType", "机器");//PC
						// }
						// if (inputDTO.getPhone() == null && inputDTO.getRobot() == null) {
						// if (task.getThirdInsteadPay() != null
						// && task.getThirdInsteadPay().intValue() == 1) {
						// map.put("operatorType", "代付");
						// } else {
						// if (bizAccount != null && bizAccount.getFlag() != null
						// && bizAccount.getFlag() == 2) {
						// map.put("operatorType", "机器");//返利网
						// } else {
						// map.put("operatorType", "机器");//PC
						// }
						// }
						// }
						// } else {
						// map.put("operatorType", "人工");
						// }
						//
						// } else {
						// map.put("operatorType", task.getOperator() == null ? "机器" : "人工");
						// }
					}
					if (null != task.getTimeConsuming() && !"toOutDraw".equals(inputDTO.getFlag())) {
						map.put("timeConsume", CommonUtils.convertTime2String(task.getTimeConsuming() * 1000L));
					}
					if (task.getAsignTime() != null) {
						map.put("asignTime", CommonUtils.millionSeconds2DateStr(task.getAsignTime().getTime()));
					} else {
						map.put("asignTime", "");
					}
					if (outDrawing.equals(inputDTO.getFlag())) {
						if (task.getAsignTime() != null) {
							// 时间差 是否已经过了3分钟 Instant.now().toEpochMilli()
							long timeUsed = now - task.getAsignTime().getTime();
							// 3*60*1000-10*60*1000
							map.put("timeGap3to10", timeUsed > 180000 && timeUsed < 600000);
							// 10*60*1000
							map.put("timeGapMore10", timeUsed >= 600000);
							map.put("timeUsed", CommonUtils.convertTime2String(timeUsed));
							map.put("timeUsedMillis", timeUsed);
						} else {
							// 时间差 是否已经过了3分钟
							map.put("timeGap", false);
							map.put("timeUsed", "0");
						}
					}
					if (failedOut.equals(inputDTO.getFlag())) {
						if (task.getAsignTime() != null) {
							// 待排查 计算是否过了5分钟
							long timeUsed = now - task.getAsignTime().getTime();
							boolean failedOutTime5 = timeUsed > 5 * 60 * 1000;
							map.put("failedOutTime5", failedOutTime5 == true && failedOutTime5);
							map.put("timeConsume", CommonUtils.convertTime2String(timeUsed));
						} else {
							map.put("failedOutTime5", false);
						}
					}
					if (masterOut.equals(inputDTO.getFlag()) || failedOut.equals(inputDTO.getFlag())) {
						// 正在排查人
						Object obj = asignFailedTaskService.getTaskReviewByTaskId(task.getId().intValue());
						if (obj != null) {
							Object[] obj1 = (Object[]) obj;
							if (obj1.length >= 2 && !"NULL".equals(obj1[1].toString())) {
								SysUser sysUser = sysUserService.findFromCacheById(Integer.valueOf(obj1[1].toString()));
								if (sysUser != null) {
									map.put("taskHolder", sysUser.getUid());
								}
							} else {
								map.put("taskHolder", "");
							}
						}
					}
					if (masterOut.equals(inputDTO.getFlag())) {
						if (task.getAsignTime() != null) {
							// 主管处理 计算是否过了10分钟
							long timeConsume = now - task.getAsignTime().getTime();
							boolean masterOutTime10 = timeConsume > 10 * 60 * 1000;
							map.put("masterOutTime10", masterOutTime10 == true && masterOutTime10);
							map.put("timeConsume", CommonUtils.convertTime2String(timeConsume));
						} else {
							map.put("masterOutTime10", false);
						}
						if (task.getAccountId() == null) {
							map.put("account", null);
							map.put("accountId", null);
						}
					}
					if (successOut.equals(inputDTO.getFlag())) {
						boolean timeOver15;
						boolean time3To15;
						long timeUsedAll;// 累计耗时
						// 审核耗时(提单到审核)
						long audtiTime = bizOutwardRequest != null && bizOutwardRequest.getTimeConsuming() != null
								&& bizOutwardRequest.getTimeConsuming() > 0
										? bizOutwardRequest.getTimeConsuming() * 1000
										: 0;
						// 等待耗时(审核之后到分配出去)
						long waitTimes = (task.getAsignTime().getTime()
								- (bizOutwardRequest != null ? bizOutwardRequest.getCreateTime().getTime() : 0))
								- audtiTime;
						// 出款耗时 加 等待耗时 加 审核耗时
						timeUsedAll = (task.getTimeConsuming() != null && task.getTimeConsuming() > 0
								? task.getTimeConsuming() * 1000
								: 0) + (waitTimes > 0 ? waitTimes : 0) + audtiTime;
						if (task.getAsignTime() != null) {
							// 完成出款 计算是否大于15分钟
							timeOver15 = timeUsedAll >= 15 * 60 * 1000;
							time3To15 = timeUsedAll >= 3 * 60 * 1000 && timeUsedAll <= 15 * 60 * 1000;
							map = wrapMap2(map, time3To15, timeOver15);
						} else {
							map.put("successOutTimeOther", true);
						}
						map.put("orderTime",
								bizOutwardRequest != null ? bizOutwardRequest.getCreateTime().getTime() : "");

						String audtiTimeUsed = CommonUtils.convertTime2String(audtiTime);
						map.put("auditTime", StringUtils.isNotBlank(audtiTimeUsed) ? audtiTimeUsed : "0");
						// 出款耗时
						String successOutTimeUsed = CommonUtils.convertTime2String(
								task.getTimeConsuming() == null ? 0 : task.getTimeConsuming() * 1000L);
						String waitTime = CommonUtils.convertTime2String(waitTimes > 0 ? waitTimes : 0);
						map.put("waitTime", StringUtils.isNotBlank(waitTime) ? waitTime : "0");

						map.put("successOutTime",
								StringUtils.isNotBlank(successOutTimeUsed) ? successOutTimeUsed : "0");
						String timeConsumingAll = CommonUtils.convertTime2String(timeUsedAll > 0 ? timeUsedAll : 0);
						map.put("timeConsumingAll", StringUtils.isNotBlank(timeConsumingAll) ? timeConsumingAll : "0");
					}
					map.put("outwardRequestId", task.getOutwardRequestId());
					map.put("handicap", StringUtils.isNotBlank(task.getHandicap()) ? task.getHandicap() : "");
					map.put("level", StringUtils.isNotBlank(task.getLevel()) ? task.getLevel() : "");
					map.put("orderNo", StringUtils.isNotBlank(task.getOrderNo()) ? task.getOrderNo() : "");
					map.put("member", StringUtils.isNotBlank(task.getMember()) ? task.getMember() : "公司用款");
					map.put("amount", task.getAmount());
					map.put("taskStatus", task.getStatus());
					map.put("taskStatusStr", OutwardTaskStatus.findByStatus(task.getStatus()).getMsg());
					map.put("remark",
							StringUtils.isNotBlank(task.getRemark())
									? task.getRemark().replace("\r\n", "<br>").replace("\n", "<br>")
									: "");
					map.put("successPhotoUrl",
							StringUtils.isNotBlank(task.getScreenshot()) ? task.getScreenshot() : "");
					map.put("id", task.getId());
					// 第三方代付 标识
					map.put("thirdInsteadPay", task.getThirdInsteadPay());
					// map.put("outRequestId",
					// task.getOutwardRequestId());
					if (successOut.equals(inputDTO.getFlag())) {
						// 可以再通知平台的
						if (bizOutwardRequest != null && (!bizOutwardRequest.getStatus()
								.equals(OutwardRequestStatus.Acknowledged.getStatus())
								|| (bizOutwardRequest.getStatus().equals(OutwardRequestStatus.Failure.getStatus())
										&& !task.getStatus().equals(OutwardTaskStatus.Matched.getStatus())))) {
							list2.add(map);
						} else {
							// 已经通知平台的
							list3.add(map);
						}
					} else {
						list.add(map);
					}
				}
				if (successOut.equals(inputDTO.getFlag())) {
					list.addAll(list2);
					list.addAll(list3);
				}
				responseData.setData(list);
				responseData.setPage(new Paging(page));
				return mapper.writeValueAsString(responseData);
			} else {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "无数据");
				responseData.setData(null);
				responseData.setPage(new Paging());
				return mapper.writeValueAsString(responseData);
			}
		} catch (Exception e) {
			logger.error("查询汇总错误：{}", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	private Integer[] fromAccount4Total(OutwardTaskTotalInputDTO inputDTO) {
		Integer[] fromAccount = new Integer[0];
		List<Integer> accountIdList = accountService.queryAccountIdsByAliasOrPhoneRobot(inputDTO);
		if (CollectionUtils.isEmpty(accountIdList)) {
			return fromAccount;
		}

		int size = accountIdList.size();
		if (accountIdList != null && size > 0) {
			fromAccount = new Integer[size];
			accountIdList.toArray(fromAccount);
		}
		return fromAccount;
	}

	private Integer[] fromAccount(String accountAlias) {
		if (StringUtils.isBlank(accountAlias)) {
			return null;
		}
		Integer[] fromAccount = new Integer[0];
		List<Integer> accountIdList = accountService.queryAccountIdsByAlias(accountAlias);
		if (CollectionUtils.isEmpty(accountIdList)) {
			return fromAccount;
		}
		int size = accountIdList.size();
		if (accountIdList != null && size > 0) {
			fromAccount = new Integer[size];
			accountIdList.toArray(fromAccount);
		}
		return fromAccount;
	}

	private Integer[] fromAccount4OutDrawing(String accountAlias, String bankType, String currentLevel) {
		Integer[] fromAccount = null;
		if (StringUtils.isNotBlank(accountAlias) || StringUtils.isNotBlank(bankType)
				|| StringUtils.isNotBlank(currentLevel)) {
			List<Object> accountIdList = accountService.queryAccountForOutDrawing(accountAlias, bankType, currentLevel);
			if (accountIdList != null && accountIdList.size() > 0) {
				fromAccount = new Integer[accountIdList.size()];
				for (int i = 0, L = accountIdList.size(); i < L; i++) {
					fromAccount[i] = (Integer) accountIdList.get(i);
				}
			}
		}
		return fromAccount;
	}

	private Integer[] operatorIds(String operatorName) {
		Integer[] operatorIds = null;
		if (StringUtils.isNotBlank(operatorName)) {
			List<SysUser> sysUserList = sysUserService.findByNameLike(StringUtils.trimToEmpty(operatorName));
			if (null != sysUserList && sysUserList.size() > 0) {
				operatorIds = new Integer[sysUserList.size()];
				for (int i = 0, L = sysUserList.size(); i < L; i++) {
					operatorIds[i] = sysUserList.get(i).getId();
				}
			}
		}
		return operatorIds;
	}

	private final String refused = "refused";
	private final String canceled = "canceled";
	private final String all = "all";

	private Integer[] status(String flag, Integer maintain) {
		Integer[] status = null;
		if (StringUtils.isNotBlank(flag)) {
			if (flag.equals(toOutDraw)) {
				if (maintain == null) {
					status = new Integer[1];
					status[0] = OutwardTaskStatus.Undeposit.getStatus();
				} else {
					status = new Integer[1];
					status[0] = OutwardTaskStatus.DuringMaintain.getStatus();
				}
			}
			if (flag.equals(outDrawing)) {
				status = new Integer[2];
				status[0] = OutwardTaskStatus.Undeposit.getStatus();
				status[1] = 99999;
			}
			if (flag.equals(masterOut)) {
				status = new Integer[1];
				status[0] = OutwardTaskStatus.ManagerDeal.getStatus();
			}
			if (flag.equals(refused)) {
				status = new Integer[1];
				status[0] = OutwardTaskStatus.ManageRefuse.getStatus();
			}
			if (flag.equals(canceled)) {
				status = new Integer[1];
				status[0] = OutwardTaskStatus.ManageCancel.getStatus();
			}
			if (flag.equals(successOut)) {
				status = new Integer[2];
				status[0] = OutwardTaskStatus.Deposited.getStatus();
				status[1] = OutwardTaskStatus.Matched.getStatus();

			}
			if (flag.equals(failedOut)) {
				status = new Integer[1];
				status[0] = OutwardTaskStatus.Failure.getStatus();
			}
			if (flag.equals(all)) {
				status = new Integer[] { OutwardTaskStatus.Undeposit.getStatus(),
						OutwardTaskStatus.DuringMaintain.getStatus(), OutwardTaskStatus.ManagerDeal.getStatus(),
						OutwardTaskStatus.ManageRefuse.getStatus(), OutwardTaskStatus.ManageCancel.getStatus(),
						OutwardTaskStatus.Deposited.getStatus(), OutwardTaskStatus.Matched.getStatus(),
						OutwardTaskStatus.Failure.getStatus() };
			}
		}
		return status;
	}

	/**
	 * 获取出款任务汇总 总记录数
	 */
	@RequestMapping("/getOutwardTaskTotalCount")
	public String getOutwardTaskTotalCount(OutwardTaskTotalInputDTO inputDTO) throws JsonProcessingException {
		GeneralResponseData<List<Map<String, Object>>> responseData;
		SysUser sysUser1 = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser1 == null
				&& (inputDTO.getPlatFormQueryFlag() == null || inputDTO.getPlatFormQueryFlag().intValue() != 1)) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		List<String> handicapCodeList = sysDataPermissionService.handicapCodeList(inputDTO.getHandicap(), sysUser1);
		if (handicapCodeList == null || handicapCodeList.size() == 0) {
			return retEmptyResults();
		}
		Integer[] operatorIds = operatorIds(inputDTO.getOperatorName());
		if (StringUtils.isNotBlank(inputDTO.getOperatorName())) {
			if (operatorIds == null || operatorIds.length == 0) {
				return retEmptyResults();
			}

		}
		String ret = returnCommon(inputDTO);
		if (StringUtils.isNotBlank(ret)) {
			return ret;
		}
		String sysLevel = inputDTO.getSysLevel();
		Integer[] fromAccount = getFromAccount(inputDTO);
		String operatorType = getOperatorType(inputDTO);
		String level = (!outDrawing.equals(inputDTO.getFlag()) && !masterOut.equals(inputDTO.getFlag())
				&& !failedOut.equals(inputDTO.getFlag())) ? inputDTO.getLevel() : null;
		Integer[] status = status(inputDTO.getFlag(), inputDTO.getMaintain());

		String drawingType = "";
		if (successOut.equals(inputDTO.getFlag())) {
			drawingType = inputDTO.getDrawType();
		}
		try {
			Long count = outwardTaskService.getOutwardTaskCount(handicapCodeList, level, inputDTO.getOrderNo(),
					inputDTO.getMember(), drawingType, operatorIds, fromAccount, inputDTO.getStartTime(),
					inputDTO.getEndTime(), inputDTO.getFromMoney(), inputDTO.getToMoney(), operatorType, status,
					sysLevel);

			Paging page;
			if (count != null) {
				page = CommonUtils.getPage(inputDTO.getPageNo() + 1,
						inputDTO.getPageSize() != null ? inputDTO.getPageSize() : AppConstants.PAGE_SIZE,
						String.valueOf(count));
			} else {
				page = CommonUtils.getPage(0,
						inputDTO.getPageSize() != null ? inputDTO.getPageSize() : AppConstants.PAGE_SIZE, "0");
			}
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "获取总记录成功");
			responseData.setPage(page);
		} catch (Exception e) {
			logger.error("获取出款任务汇总总记录数失败：{}", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue());
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 获取出款任务汇总 总金额
	 */
	@RequestMapping("/getOutwardTaskTotalSum")
	public String getOutwardTaskTotalSum(OutwardTaskTotalInputDTO inputDTO) throws JsonProcessingException {
		GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		SysUser sysUser1 = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser1 == null
				&& (inputDTO.getPlatFormQueryFlag() == null || inputDTO.getPlatFormQueryFlag().intValue() != 1)) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		List<String> handicapCodeList = sysDataPermissionService.handicapCodeList(inputDTO.getHandicap(), sysUser1);
		if (handicapCodeList == null || handicapCodeList.size() == 0) {
			return retEmptyResults();
		}
		Integer[] operatorIds = operatorIds(inputDTO.getOperatorName());
		if (StringUtils.isNotBlank(inputDTO.getOperatorName())) {
			if (operatorIds == null || operatorIds.length == 0) {
				return retEmptyResults();
			}

		}
		String ret = returnCommon(inputDTO);
		if (StringUtils.isNotBlank(ret)) {
			return ret;
		}
		Integer[] fromAccount = getFromAccount(inputDTO);
		String operatorType = getOperatorType(inputDTO);
		String level = (!outDrawing.equals(inputDTO.getFlag()) && !masterOut.equals(inputDTO.getFlag())
				&& !failedOut.equals(inputDTO.getFlag())) ? inputDTO.getLevel() : null;
		Integer[] status = status(inputDTO.getFlag(), inputDTO.getMaintain());
		String drawingType = "";
		if (successOut.equals(inputDTO.getFlag())) {
			drawingType = inputDTO.getDrawType();
		}
		String sysLevel = inputDTO.getSysLevel();
		try {
			String sumAmount = outwardTaskService.getOutwardTaskSum(handicapCodeList, level, inputDTO.getOrderNo(),
					inputDTO.getMember(), drawingType, operatorIds, fromAccount, inputDTO.getStartTime(),
					inputDTO.getEndTime(), inputDTO.getFromMoney(), inputDTO.getToMoney(), operatorType, status,
					sysLevel);

			Map<String, Object> map = new HashMap();
			map.put("sumAmount", sumAmount);
			responseData.setData(map);
		} catch (Exception e) {
			logger.error("获取出款任务汇总总金额失败：{}", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue());
		}
		return mapper.writeValueAsString(responseData);
	}

	public String status(int[] status) {
		String statusF = "";
		if (status.length > 0) {
			for (int i = 0, L = status.length; i < L; i++) {
				for (int j = i; j < L; j++) {
					if (status[i] == status[j] && status[i] == OutwardTaskStatus.Undeposit.getStatus()) {
						statusF = "0";// 都未出款
					} else if (status[i] == status[j] && status[i] == OutwardTaskStatus.Deposited.getStatus()) {
						statusF = "1";// 都已出款
					} else if (status[i] == status[j] && status[i] == OutwardTaskStatus.Matched.getStatus()) {
						statusF = "2";// 都已经和流水匹配成功
					} else {
						statusF = "4";// 正在出款
					}
				}
			}
		}
		return statusF;
	}

	public List<BizOutwardTask> distinct(List<BizOutwardTask> list) {
		if (list == null || list.size() == 0) {
			return Collections.emptyList();
		}
		Set<BizOutwardTask> set = new TreeSet<>((o1, o2) -> {
			// 字符串,则按照asicc码升序排列
			return o2.getOutwardRequestId().compareTo(o1.getOutwardRequestId());
		});
		set.addAll(list);
		return new ArrayList<>(set);
	}

	/***
	 * 主管分配:出款汇总 主管处理页签 未出款页签 包含第三方出款
	 */
	@RequestMapping("/reallocateTask")
	public String reAllocateTask(@RequestParam(value = "bankType") String[] bankType,
			@RequestParam(value = "type") String type,
			@RequestParam(value = "actionPage", required = false) String actionPage,
			@RequestParam(value = "remark") String remark, @RequestParam(value = "taskId") Long taskId)
			throws JsonProcessingException {
		GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "分配成功");
		try {
			if (taskId == null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"参数丢失分配失败");
				return mapper.writeValueAsString(responseData);
			}
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser == null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆!");
				return mapper.writeValueAsString(responseData);
			}
			BizOutwardTask bizOutwardTask = outwardTaskService.findById(taskId);
			if (bizOutwardTask == null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "任务不存在!");
				return mapper.writeValueAsString(responseData);
			}
			// 如果是第三方代付的 分配的时候先查询daifu_info 表状态
			if (null != bizOutwardTask.getOperator()
					&& bizOutwardTask.getOperator().equals(AppConstants.USER_ID_4_ADMIN)
					&& null != bizOutwardTask.getThirdInsteadPay() && bizOutwardTask.getThirdInsteadPay() == 1) {
				DaifuResult daifuResult = daifu4OutwardService.query(bizOutwardTask);
				if (!ObjectUtils.isEmpty(daifuResult)
						&& daifuResult.getResult().getValue().equals(DaifuResult.ResultEnum.PAYING.getValue())) {
					responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
							"任务正在使用第三方代付,等待自动转排查!");
					return mapper.writeValueAsString(responseData);
				}
			}
			if (StringUtils.isBlank(actionPage)) {
				if (bizOutwardTask.getStatus().equals(OutwardTaskStatus.ManagerDeal.getStatus())
						|| bizOutwardTask.getAccountId() == null
						|| (bizOutwardTask.getAccountId() != null && bizOutwardTask.getAsignTime() != null)) {
					if (bizOutwardTask.getAccountId() != null && bizOutwardTask.getAsignTime() != null) {
						long timeMinus = System.currentTimeMillis() - bizOutwardTask.getAsignTime().getTime();
						if (timeMinus > 0 && timeMinus <= 60000) {
							// 60秒之后才能分配
							responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
									"任务60秒内不能重复分配!");
							return mapper.writeValueAsString(responseData);
						}
					}
					boolean isManual = StringUtils.isNotBlank(type) && "manual".equals(type);
					if (StringUtils.isNotBlank(type) && "thirdOut".equals(type)) {
						outwardTaskAllocateService.remark4Mgr(taskId, isManual, true, sysUser, bankType, remark);
					} else {
						outwardTaskAllocateService.remark4Mgr(taskId, isManual, false, sysUser, bankType, remark);
					}
					asignFailedTaskService.updateReviewTask(taskId.intValue(),
							CommonUtils.genRemark(bizOutwardTask.getRemark(), remark, new Date(),
									sysUser.getUid() + "(排查:由主管处理状态分配)"),
							sysUser.getId());
					return mapper.writeValueAsString(responseData);
				}
				return mapper.writeValueAsString(responseData);
			} else {
				// 时间过长,正在出款已分配的订单再分配
				if (bizOutwardTask.getStatus().equals(OutwardTaskStatus.Undeposit.getStatus())
						&& bizOutwardTask.getAccountId() != null) {
					// AccountBaseInfo base =
					// accountService.getFromCacheById(bizOutwardTask.getAccountId());
					// if (!(Objects.nonNull(base) && base.checkMobile())) {
					// return mapper.writeValueAsString(new
					// GeneralResponseData<>(
					// GeneralResponseData.ResponseStatus.FAIL.getValue(),
					// "该功能只针对手机出款"));
					// }
					if (bizOutwardTask.getAsignTime() != null) {
						long timeMinus = System.currentTimeMillis() - bizOutwardTask.getAsignTime().getTime();
						if (timeMinus > 0 && timeMinus <= 60000) {
							// 60秒之后才能分配
							responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
									"任务60秒内不能重复分配!");
							return mapper.writeValueAsString(responseData);
						}
					}
					boolean isManual = StringUtils.isNotBlank(type) && "manual".equals(type);
					outwardTaskAllocateService.reAssignDrawingTask(bizOutwardTask, isManual, bankType, sysUser,
							StringUtils.trim(remark));
				} else {
					responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
							"该任务已分配，无法继续分配，请刷新!");
				}
				return mapper.writeValueAsString(responseData);
			}
		} catch (Exception e) {
			logger.error("分配失败：{}", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "分配失败");
			return mapper.writeValueAsString(responseData);
		}
	}

	/**
	 * 出款任务页签--正在出款 获取出款任务 提单信息 汇入信息
	 */
	@RequestMapping("/get")
	public String getTask(@RequestParam(value = "taskIdLastTime", required = false) String taskIdLastTime)
			throws JsonProcessingException {
		GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "出款任务获取成功");
		try {
			Map<String, Object> map = new HashMap<>();
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser == null || sysUser.getHandicap() == null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆");
				return mapper.writeValueAsString(responseData);
			}
			BizOutwardTask bizOutwardTask = outwardTaskAllocateService.applyTask4User(sysUser.getId());
			if (bizOutwardTask != null && bizOutwardTask.getOperator().equals(sysUser.getId())) {
				logger.info("本次获取任务id:{},上次获取任务id:{}", bizOutwardTask.getId(), taskIdLastTime);
				Double splitBoundary = systemSettingService
						.getOutDrawLimitApproveInCache(UserProfileKey.OUTDRAW_LIMIT_APPROVE.getValue());
				BizOutwardRequest bizOutwardRequest = outwardRequestService.get(bizOutwardTask.getOutwardRequestId());
				String review = bizOutwardRequest.getReview() == null ? "" : bizOutwardRequest.getReview();
				SysUser sysUser1 = sysUserService.findFromCacheById(bizOutwardTask.getOperator());
				if (sysUser1 != null && sysUser1.getCategory() >= 300) {
					// 第三方出款 目前只有财务人员才能出第三方
					map.put("review", new StringBuilder(review).append("(第三方出款)").toString());
					// 默认拆单1
					// if (!splitOrderService.isSplitAlready(bizOutwardRequest.getOrderNo(),
					// sysUser1.getId())) {
					// splitOrderService.splitOrder(bizOutwardRequest.getOrderNo(), 1,
					// sysUser1.getId());
					// }
				} else {
					if (StringUtils.isNotBlank(bizOutwardTask.getRemark())
							&& bizOutwardTask.getRemark().contains("系统调用代付异常")) {
						map.put("review", new StringBuilder(review).append("(三方代付出款异常!)").toString());
					} else {
						map.put("review", review);
					}
				}
				map.put("splitBoundary", splitBoundary);
				BizHandicap bizHandicap = handicapService.findFromCacheById(bizOutwardRequest.getHandicap());
				BizLevel bizLevel = levelService.findFromCache(bizOutwardRequest.getLevel());
				map.put("orderNo", bizOutwardRequest.getOrderNo());
				map.put("handicap",
						bizHandicap != null && StringUtils.isNotBlank(bizHandicap.getName()) ? bizHandicap.getName()
								: "");
				map.put("level",
						bizLevel != null && StringUtils.isNotBlank(bizLevel.getName()) ? bizLevel.getName() : "");
				map.put("member", bizOutwardRequest.getMember());
				map.put("memberCode", bizOutwardRequest.getMemberCode());
				map.put("asignTime", bizOutwardTask.getAsignTime());
				map.put("amount", bizOutwardTask.getAmount());
				map.put("manualForever", bizOutwardTask.getManualForever());// 限制人工出款标志

				map.put("toAccountOwner", bizOutwardRequest.getToAccountOwner());
				map.put("toAccountBank", bizOutwardRequest.getToAccountBank());
				map.put("toAccountName", bizOutwardRequest.getToAccountName());
				map.put("toAccountNo", bizOutwardRequest.getToAccount());
				// 分配任务的时候分配出款账号一起
				map.put("accountId", bizOutwardTask.getAccountId());
				map.put("userId", sysUser.getId());
				map.put("taskId", bizOutwardTask.getId());
				map.put("taskRemarks",
						StringUtils.isNotBlank(bizOutwardTask.getRemark())
								? bizOutwardTask.getRemark().replace("\r\n", "<br>").replace("\n", "<br>")
								: "");
				responseData.setData(map);
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error("调用出款任务失败：{}", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "出款任务获取失败");
			return mapper.writeValueAsString(responseData);
		}
	}

	/**
	 * 完成出款 third 为true表示第三方出款<br>
	 * 如果是三方出款则备注thirdRemark格式:<br>
	 * 盘口:金龙彩票,商号:亿汇付,金额:10000,id:248702|盘口:金龙彩票,商号:汇隆,金额:10000,id:248720"<br>
	 *
	 * 如果是拆单出款 也要拼接thirdRemark格式 如上<br>
	 * 2019-09-12 <br>
	 * 如果是拆单 且使用的三方账号手续费规则是从金额里扣除的允许拆单金额总和大于订单金额且为输入的手续费。
	 */
	@RequestMapping("/transfer")
	public String transfer(@RequestParam(value = "taskId", required = false) Long taskId,
			@RequestParam(value = "userId", required = false) Integer userId,
			@RequestParam(value = "remark", required = false) String remark,
			@RequestParam(value = "fee", required = false) String fee,
			@RequestParam(value = "thirdRemark", required = false) String thirdRemark,
			@RequestParam(value = "third", required = false) Boolean third,
			@RequestParam(value = "fromAccountId", required = false) Integer fromAccountId)
			throws JsonProcessingException {
		GeneralResponseData<String> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		log.info("人工完成出款 参数:taskId:{},userId:{},remark:{},fee:{},thirdRemark:{},third:{},fromAccountId:{}", taskId,
				userId, remark, fee, thirdRemark, third, fromAccountId);
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (operator == null || !operator.getId().equals(userId)) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		if (third != null && third) {
			if (StringUtils.isBlank(thirdRemark)) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"请选择第三方出款账号!");
				return mapper.writeValueAsString(responseData);
			}
		}
		try {
			BizOutwardTask bizOutwardTask = outwardTaskService.findById(taskId);
			if (bizOutwardTask == null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"操作失败,记录不存在");
				return mapper.writeValueAsString(responseData);
			}
			if (bizOutwardTask.getStatus().equals(OutwardTaskStatus.Deposited.getStatus())) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "已出款，请刷新");
				return mapper.writeValueAsString(responseData);
			}
			if (bizOutwardTask.getStatus().equals(OutwardTaskStatus.Matched.getStatus())) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"已匹配流水，请刷新");
				return mapper.writeValueAsString(responseData);
			}
			String orderNo = bizOutwardTask.getOrderNo();
			boolean splitFinish = thirdRemark.equals("拆单完成出款")
					|| splitOrderService.isPartialFinished(orderNo, operator.getId());
			if (third) {
				// 如果是三方出款拆单
				if (splitFinish) {
					if (StringUtils.isEmpty(fee)) {
						responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
								"手续费参数有误,联系技术!");
						return mapper.writeValueAsString(responseData);
					}
					logger.info("三方出款拆单 点击完成,orderNo: {},taskId :{} ", orderNo, taskId);
					if (splitOrderService.finalFinishCapable(orderNo, operator.getId())) {
						// 使用的第三方账号信息
						thirdRemark = splitOrderService.usedThirdAccount(orderNo, operator.getId(), fee);
						logger.info("三方拆单出款完成，获取三方信息拼接结果:{}", thirdRemark);
						if ("NO".equals(thirdRemark)) {
							responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
									"拆单出款账号或盘口信息有误,联系技术!");
							return mapper.writeValueAsString(responseData);
						}
					} else {
						responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
								"还有未完成的拆单!");
						return mapper.writeValueAsString(responseData);
					}
				}

				// 第三方出款把盘口商号金额放在备注里:{盘口|商号|金额,...}
				if (StringUtils.isNotBlank(remark)) {
					remark = new StringBuilder().append("{").append(thirdRemark).append(",")
							.append(StringUtils.trimToEmpty(remark)).append("}").toString();
				} else {
					remark = new StringBuilder().append("{").append(thirdRemark).append("}").toString();
				}
			}
			outwardTaskAllocateService.ack4User(taskId, userId, remark, fromAccountId, third, null);
			if (!third) {
				transMonitorService.reportTransResult(taskId);
			}
			BizOutwardRequest req = outwardRequestService.get(bizOutwardTask.getOutwardRequestId());
			// 如果是第三方出款则保存第三方账号的出款信息到记录表
			if (third) {
				Date now = new Date();
				Long timeconsume = now.getTime() - req.getCreateTime().getTime();
				// 保存出款记录到下发完成记录里
				List<BizIncomeRequest> drawFinishRecord = new LinkedList<>();
				logger.debug("单号：" + req.getOrderNo() + "是第三方出款。");
				// 如果第三方备注不为空，且不是选择其它的第三方出款
				if (StringUtils.isNotBlank(remark) && remark.indexOf("id") > 0) {
					logger.debug("单号：" + req.getOrderNo() + "是第三方出款且备注不为空。");
					String[] remarkValue = remark.split("\\|");
					// 判断是否存在多个第三方账号出款
					BizAccount thirdAccount;
					if (remarkValue.length > 1) {
						logger.debug("单号：" + req.getOrderNo() + "是第三方出款且备注不为空且是多个第三方账号出款。");
						for (int j = 0; j < remarkValue.length; j++) {
							BigDecimal thirdOutAmount = new BigDecimal(remarkValue[j].split(",")[2].split(":")[1]);
							int thirdId = Integer.valueOf(remarkValue[j].split(",")[3].split(":")[1].replace("}", ""));
							logger.info("单号：" + req.getOrderNo() + "保存第三方出款信息到记录表。");
							// 保存到biz_third_out 然后触发器[update_third_balance]更新三方系统余额
							BigDecimal feeVal = BigDecimal.ZERO;
							thirdAccount = accountService.getById(thirdId);
							if (splitFinish) {
								feeVal = new BigDecimal(remarkValue[j].split(",")[4].split(":")[1]);
								outwardRequestService.saveThirdOutWithFee(thirdId, thirdOutAmount, feeVal, req,
										operator, "拆单出款操作！");
							} else {
								outwardRequestService.saveThirdOut(thirdId, thirdOutAmount, req, operator, "出款操作！");
							}
							drawFinishRecord.add(new BizIncomeRequest(req, thirdId, thirdOutAmount, feeVal,
									operator.getId(), thirdAccount, timeconsume));

						}
					} else {
						logger.debug("单号：" + req.getOrderNo() + "是第三方出款且备注不为空是一个第三方账号出款。");
						// 异常数据处理(存在不规范格式)
						remarkValue = remark.split(",");
						if (remarkValue.length > 1) {
							BigDecimal thirdOutAmount = new BigDecimal(remarkValue[2].split(":")[1]);
							int thirdId = Integer.valueOf(remarkValue[3].split(":")[1].replace("}", ""));
							thirdAccount = accountService.getById(thirdId);
							logger.info("单号：" + req.getOrderNo() + "保存第三方出款信息到记录表。");
							// 保存到biz_third_out 然后触发器[update_third_balance]更新三方系统余额
							BigDecimal feeVal = BigDecimal.ZERO;
							if (splitFinish) {
								feeVal = new BigDecimal(remarkValue[4].split(":")[1]);
								outwardRequestService.saveThirdOutWithFee(thirdId, thirdOutAmount, feeVal, req,
										operator, "拆单出款操作！");
							} else {
								outwardRequestService.saveThirdOut(thirdId, thirdOutAmount, req, operator, "出款操作！");
							}
							drawFinishRecord.add(new BizIncomeRequest(req, thirdId, thirdOutAmount, feeVal,
									operator.getId(), thirdAccount, timeconsume));
						}
					}
				}
				// 三方拆单出款 完成 删除缓存
				if (splitFinish) {
					splitOrderService.updateOrderFinal(bizOutwardTask.getOrderNo(), operator.getId());
				}
				if (!CollectionUtils.isEmpty(drawFinishRecord)) {
					List<BizIncomeRequest> res = incomeRequestService.saveCollection(drawFinishRecord);
					logger.debug("保存到下发完成记录结果:{}", res.size() == drawFinishRecord.size());
				}
			}
		} catch (Exception e) {
			logger.error("转账完成调用接口失败：{},参数：taskId:{},userId:{},fromAccountId:{},第三方出款third:{}", e, taskId, userId,
					fromAccountId, third);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败");
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * @param status
	 *            {@link OutwardTaskStatus} 转主管 2 , 取消 3 ,拒绝 4
	 */
	@RequestMapping("/status")
	public String dealOther(@RequestParam(value = "taskId") Long taskId, @RequestParam(value = "status") Integer status,
			@RequestParam(value = "remark") String remark,
			@RequestParam(value = "artificialOutAccount", required = false) String artificialOutAccount,
			@RequestParam(value = "accountError", required = false) String accountError,
			@RequestParam(value = "thirdOutFlag", required = false) Boolean thirdOutFlag,
			@RequestParam(value = "orderNo", required = false) String orderNo) throws JsonProcessingException {
		GeneralResponseData<String> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功");
		try {
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (operator == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			if (StringUtils.isBlank(remark)) {
				new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请填写原因");
				return mapper.writeValueAsString(responseData);
			}
			BizOutwardTask bizOutwardTask = outwardTaskService.findById(taskId);
			if (bizOutwardTask == null) {
				new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "任务不存在!");
				return mapper.writeValueAsString(responseData);
			}
			// 如果是代付的订单 正在支付的订单是取消和拒绝的
			boolean daifu = (OutwardTaskStatus.ManageRefuse.getStatus().equals(status)
					|| OutwardTaskStatus.ManageCancel.getStatus().equals(status))
					&& bizOutwardTask.getThirdInsteadPay() != null && bizOutwardTask.getThirdInsteadPay() == 1;
			if (daifu) {
				DaifuResult result = daifu4OutwardService.query(bizOutwardTask);
				boolean flag = !ObjectUtils.isEmpty(result)
						&& (result.getResult().getValue().equals(DaifuResult.ResultEnum.PAYING.getValue())
								|| result.getResult().getValue().equals(DaifuResult.ResultEnum.UNKOWN.getValue()));
				if (flag) {
					String msg = OutwardTaskStatus.ManageCancel.getStatus().equals(status) ? "第三方代付中不能取消!"
							: "第三方代付中不能拒绝!";
					return mapper.writeValueAsString(
							new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), msg));
				}
			}
			String desc;
			if (OutwardTaskStatus.ManageCancel.getStatus().equals(status)) {
				if (bizOutwardTask.getStatus().equals(OutwardTaskStatus.ManageCancel.getStatus())) {
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
							"任务状态已变更:" + OutwardTaskStatus.ManageCancel.getMsg());
					return mapper.writeValueAsString(responseData);
				}
				desc = OutwardTaskStatus.ManageCancel.getMsg();
				if (thirdOutFlag != null && thirdOutFlag) {
					remark = new StringBuilder("第三方出款{取消}").append(remark).toString();
				}
				if (StringUtils.isNotBlank(accountError) && "ACCOUNT_ERROR".equals(StringUtils.trim(accountError))) {
					remark = new StringBuilder(remark).append("#ERROR#").toString();
				}
				outwardTaskAllocateService.alterStatusToCancel(null, taskId, operator, remark);
				if (StringUtils.isNotBlank(artificialOutAccount)) {
					logger.info("新增会员到永久出款队列 toAccount:{} ", artificialOutAccount);
					outwardTaskService.manualOutMoney(artificialOutAccount);
				}
				asignFailedTaskService.updateReviewTask(taskId.intValue(),
						CommonUtils.genRemark(bizOutwardTask.getRemark(), remark, new Date(),
								operator.getUid() + "(" + desc + ")"),
						operator.getId());
			}
			if (OutwardTaskStatus.ManageRefuse.getStatus().equals(status)) {
				if (bizOutwardTask.getStatus().equals(OutwardTaskStatus.ManageRefuse.getStatus())) {
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
							"任务状态已变更:" + OutwardTaskStatus.ManageRefuse.getMsg());
					return mapper.writeValueAsString(responseData);
				}
				desc = OutwardTaskStatus.ManageRefuse.getMsg();
				if (thirdOutFlag != null && thirdOutFlag) {
					remark = new StringBuilder("第三方出款{拒绝}").append(remark).toString();
				}
				outwardTaskAllocateService.alterStatusToRefuse(null, taskId, operator, remark);
				asignFailedTaskService.updateReviewTask(taskId.intValue(),
						CommonUtils.genRemark(bizOutwardTask.getRemark(), remark, new Date(),
								operator.getUid() + "(" + desc + ")"),
						operator.getId());
			}
			if (OutwardTaskStatus.ManagerDeal.getStatus().equals(status)) {
				if (bizOutwardTask.getStatus().equals(OutwardTaskStatus.ManagerDeal.getStatus())) {
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
							"任务状态已变更:" + OutwardTaskStatus.ManagerDeal.getMsg());
					return mapper.writeValueAsString(responseData);
				}
				if (thirdOutFlag != null && thirdOutFlag) {
					remark = new StringBuilder("第三方出款{转主管}").append(remark).toString();
				}
				if (Objects.nonNull(bizOutwardTask) && Objects.nonNull(bizOutwardTask.getAccountId())) {
					systemAccountManager.rpush(new SysBalPush<>(bizOutwardTask.getAccountId(),
							SysBalPush.CLASSIFY_BANK_MAN_MGR, bizOutwardTask));
				}
				outwardTaskAllocateService.alterStatusToMgr(taskId, operator, remark, null);
			}

		} catch (Exception e) {
			logger.error("操作失败：{},操作参数：taskId:{},orderNo:{}", e, taskId, orderNo);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败");
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 出款任务页签 type 完成出款1 出款失败0（status==6）(不论出款成功与失败，这笔出款任务都是已经审核了的) 但是：2017-11-08
	 * ：为了统计需要更新状态包括 1 5 6
	 */
	@RequestMapping("/task")
	public String getOutwardTaskList(@RequestParam(value = "handicapId") String handicap,
			@RequestParam(value = "levelId") String level, @RequestParam(value = "orderNo") String orderNo,
			@RequestParam(value = "member") String member, @RequestParam(value = "fromMoney") BigDecimal fromMoney,
			@RequestParam(value = "toMoney") BigDecimal toMoney,
			@RequestParam(value = "accountAlias") String accountAlias,
			@RequestParam(value = "startTime") String startTime, @RequestParam(value = "endTime") String endTime,
			@RequestParam(value = "pageNo") Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "type") Integer type, @RequestParam(value = "sysLevel") String sysLevel)
			throws JsonProcessingException {
		GeneralResponseData<List<Map<String, Object>>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		SysUser sysUser1 = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser1 == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		List<String> handicapCodeList = sysDataPermissionService.handicapCodeList(handicap, sysUser1);
		if (handicapCodeList == null || handicapCodeList.size() == 0) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "无记录");
			responseData.setPage(new Paging());
			responseData.setData(null);
			return mapper.writeValueAsString(responseData);
		}
		try {
			List<Map<String, Object>> list = new LinkedList<>();
			List<Map<String, Object>> list2 = null;
			List<Map<String, Object>> list3 = null;
			Integer[] status = null;
			Integer[] accountId = fromAccount(accountAlias);
			if (StringUtils.isNotBlank(accountAlias)) {
				if (accountId == null || accountId.length == 0) {
					responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
							"无记录");
					responseData.setData(null);
					responseData.setPage(new Paging());
					return mapper.writeValueAsString(responseData);
				}
			}
			if (type == 1) {
				// 已出款
				status = new Integer[2];
				status[0] = OutwardTaskStatus.Deposited.getStatus();
				status[1] = OutwardTaskStatus.Matched.getStatus();
			}
			if (type == 0) {
				status = new Integer[1];
				status[0] = OutwardTaskStatus.Failure.getStatus();
			}
			PageRequest pageRequest = new PageRequest(pageNo, pageSize == null ? AppConstants.PAGE_SIZE : pageSize,
					new Sort(Sort.Direction.DESC, "asignTime"));
			Page<BizOutwardTask> page = outwardTaskService.findOutwardTaskPageNoCount(handicapCodeList, level, orderNo,
					null, member, new Integer[] { sysUser1.getId() }, accountId, startTime, endTime, fromMoney, toMoney,
					null, status, pageRequest, sysLevel);
			List<BizOutwardTask> bizOutwardTaskList = page != null && page.getContent() != null ? page.getContent()
					: null;
			if (bizOutwardTaskList != null && bizOutwardTaskList.size() > 0) {
				if (type == 1) {
					list2 = new ArrayList<>();
					list3 = new ArrayList<>();
				}
				for (BizOutwardTask task : bizOutwardTaskList) {
					Map<String, Object> map = new LinkedHashMap<>();
					map.put("id", task.getId());
					map.put("outwardRequestId", task.getOutwardRequestId());
					BizOutwardRequest bizOutwardRequest = outwardRequestService.get(task.getOutwardRequestId());
					if (bizOutwardRequest != null) {
						map.put("requestStatus", bizOutwardRequest.getStatus());
					}
					map.put("handicap", task.getHandicap());
					map.put("level", task.getLevel());
					map.put("orderNo", task.getOrderNo());
					map.put("member", StringUtils.isNotBlank(task.getMember()) ? task.getMember() : "公司用款");
					if (task.getAccountId() != null) {
						if (null != task.getThirdInsteadPay() && task.getThirdInsteadPay() == 1) {
							DaifuConfigRequest daifuConfigRequest = daifuConfigRequestRepository
									.findOne(task.getAccountId());
							if (!ObjectUtils.isEmpty(daifuConfigRequest)) {
								logger.debug("代付订单 信息:{}", daifuConfigRequest);
								map.put("account",
										StringUtils.isNotBlank(daifuConfigRequest.getMemberId())
												? daifuConfigRequest.getMemberId()
												: "");
								map.put("accountId", daifuConfigRequest.getId());
								map.put("bankName",
										StringUtils.isNotBlank(daifuConfigRequest.getChannelName())
												? daifuConfigRequest.getChannelName()
												: "");
							}
						} else {
							AccountBaseInfo bizAccount = accountService.getFromCacheById(task.getAccountId());
							if (bizAccount != null) {
								map.put("account",
										StringUtils.isNotBlank(bizAccount.getAccount()) ? bizAccount.getAccount() : "");
								map.put("accountId", bizAccount.getId());
								map.put("bankName",
										StringUtils.isNotBlank(bizAccount.getBankName()) ? bizAccount.getBankName()
												: "");
							}
						}

					}
					map.put("amount", task.getAmount());
					Integer timesCous = task.getTimeConsuming();
					if (timesCous != null) {
						map.put("timeconsuming", CommonUtils.convertTime2String(timesCous * 1000L));// 保存的是秒所以先转为毫秒再格式化
						map.put("asignTime",
								task.getAsignTime() != null
										? CommonUtils.millionSeconds2DateStr(
												task.getAsignTime().getTime() + timesCous * 1000)
										: "");
					} else {
						map.put("asignTime",
								task.getAsignTime() != null
										? CommonUtils.millionSeconds2DateStr(task.getAsignTime().getTime())
										: "");
					}
					map.put("operator", sysUser1.getUid());
					map.put("taskStatus", task.getStatus());
					map.put("remark",
							StringUtils.isNotBlank(task.getRemark())
									? task.getRemark().replace("\r\n", "<br>").replace("\n", "<br>")
									: "");
					map.put("successPhotoUrl", task.getScreenshot());
					// 第三方代付 标识
					map.put("thirdInsteadPay", task.getThirdInsteadPay());
					if (type == 1) {
						if (!OutwardRequestStatus.Acknowledged.getStatus().equals(bizOutwardRequest.getStatus())
								|| (OutwardRequestStatus.Failure.getStatus().equals(bizOutwardRequest.getStatus())
										&& !OutwardTaskStatus.Matched.getStatus().equals(task.getStatus()))) {
							list2.add(map);// 可以再通知平台的
						} else {
							list3.add(map);// 已经通知平台的
						}
					} else {
						list.add(map);
					}
				}
				if (type == 1) {
					list.addAll(list2);
					list.addAll(list3);
				}
				responseData.setData(list);
				responseData.setPage(new Paging(page));
			} else {
				responseData.setData(null);
			}
		} catch (Exception e) {
			logger.error("查询失败：{}", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue());
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 获取出款任务页签 完成 失败 总金额
	 */
	@RequestMapping("/getOutwardTaskSum")
	public String getOutwardTaskSum(@RequestParam(value = "handicapId") String handicap,
			@RequestParam(value = "levelId") String level, @RequestParam(value = "orderNo") String orderNo,
			@RequestParam(value = "member") String member, @RequestParam(value = "fromMoney") BigDecimal fromMoney,
			@RequestParam(value = "toMoney") BigDecimal toMoney,
			@RequestParam(value = "accountAlias") String accountAlias,
			@RequestParam(value = "startTime") String startTime, @RequestParam(value = "endTime") String endTime,
			@RequestParam(value = "type") Integer type, @RequestParam(value = "sysLevel") String sysLevel)
			throws JsonProcessingException {
		GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		SysUser sysUser1 = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser1 == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		List<String> handicapCodeList = sysDataPermissionService.handicapCodeList(handicap, sysUser1);
		if (handicapCodeList == null || handicapCodeList.size() == 0) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "无记录");
			responseData.setPage(new Paging());
			responseData.setData(null);
			return mapper.writeValueAsString(responseData);
		}
		try {
			Integer[] status = null;
			Integer[] accountId = fromAccount(accountAlias);
			if (StringUtils.isNotBlank(accountAlias)) {
				if (accountId == null || accountId.length == 0) {
					responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
							"无记录");
					responseData.setData(null);
					responseData.setPage(new Paging());
					return mapper.writeValueAsString(responseData);
				}
			}
			if (type == 1) {
				// 已出款
				status = new Integer[2];
				status[0] = OutwardTaskStatus.Deposited.getStatus();
				status[1] = OutwardTaskStatus.Matched.getStatus();
			}
			if (type == 0) {
				status = new Integer[1];
				status[0] = OutwardTaskStatus.Failure.getStatus();
			}
			String sum = outwardTaskService.getOutwardTaskSum(handicapCodeList, level, orderNo, member, null,
					new Integer[] { sysUser1.getId() }, accountId, startTime, endTime, fromMoney, toMoney, null, status,
					sysLevel);
			Map<String, Object> map = new HashMap<>();
			map.put("sumAmount", sum);
			responseData.setData(map);
		} catch (Exception e) {
			logger.error("获取出款任务金额失败：{}", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue());
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 获取出款任务页签 完成 失败 总记录数
	 */
	@RequestMapping("/getOutwardTaskCount")
	public String getOutwardTaskCount(@RequestParam(value = "handicapId") String handicap,
			@RequestParam(value = "levelId") String level, @RequestParam(value = "orderNo") String orderNo,
			@RequestParam(value = "member") String member, @RequestParam(value = "fromMoney") BigDecimal fromMoney,
			@RequestParam(value = "toMoney") BigDecimal toMoney,
			@RequestParam(value = "accountAlias") String accountAlias,
			@RequestParam(value = "startTime") String startTime, @RequestParam(value = "endTime") String endTime,
			@RequestParam(value = "pageNo") Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "type") Integer type, @RequestParam(value = "sysLevel") String sysLevel)
			throws JsonProcessingException {
		GeneralResponseData<List<Map<String, Object>>> responseData;
		SysUser sysUser1 = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser1 == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		List<String> handicapCodeList = sysDataPermissionService.handicapCodeList(handicap, sysUser1);
		if (handicapCodeList == null || handicapCodeList.size() == 0) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "无记录");
			responseData.setPage(new Paging());
			responseData.setData(null);
			return mapper.writeValueAsString(responseData);
		}
		try {
			Integer[] status = null;
			Integer[] accountId = fromAccount(accountAlias);
			if (StringUtils.isNotBlank(accountAlias)) {
				if (accountId == null || accountId.length == 0) {
					responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
							"无记录");
					responseData.setData(null);
					responseData.setPage(new Paging());
					return mapper.writeValueAsString(responseData);
				}
			}
			if (type == 1) {
				status = new Integer[2];
				status[0] = OutwardTaskStatus.Deposited.getStatus();
				status[1] = OutwardTaskStatus.Matched.getStatus();
			}
			if (type == 0) {
				status = new Integer[1];
				status[0] = OutwardTaskStatus.Failure.getStatus();
			}
			Long count = outwardTaskService.getOutwardTaskCount(handicapCodeList, level, orderNo, member, null,
					new Integer[] { sysUser1.getId() }, accountId, startTime, endTime, fromMoney, toMoney, null, status,
					sysLevel);
			Paging page;
			if (count != null) {
				page = CommonUtils.getPage(pageNo + 1, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
						String.valueOf(count));
			} else {
				page = CommonUtils.getPage(0, pageSize != null ? pageSize : AppConstants.PAGE_SIZE, "0");
			}
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "获取总记录成功");
			responseData.setPage(page);
		} catch (Exception e) {
			logger.error("获取出款任务总记录数失败：{}", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue());
		}
		return mapper.writeValueAsString(responseData);
	}

	@RequestMapping("/reject")
	public String reject(@RequestParam(value = "outwardTaskId") Long outwardTaskId,
			@RequestParam(value = "remark") String remark) throws JsonProcessingException {
		try {
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (operator == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆!"));
			}
			BizOutwardTask task = outwardTaskService.findById(outwardTaskId);
			if (task == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "任务不存在!"));
			}
			outwardTaskAllocateService.alterStatusToFail(outwardTaskId, operator, remark);
			if (task.getOperator() == null) {
				// 非人工出款才会分配任务排查
				asignFailedTaskService.asignOnTurnToFail(task.getId());
			}
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "转待排查成功"));
		} catch (Exception e) {
			logger.error("/reject 转待排查失败：" + e.getLocalizedMessage());
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "转待排查失败" + e.getLocalizedMessage()));
		}
	}

	/**
	 * 转已匹配<br/>
	 * 在找不到流水的情况下，手工把该出款任务转已匹配
	 */
	@RequestMapping("/matchWithoutBankLog")
	public String matchWithoutBankLog(@RequestParam(value = "outwardTaskId") Long outwardTaskId,
			@RequestParam(value = "remark") String remark) throws JsonProcessingException {
		try {
			SysUser opr = (SysUser) SecurityUtils.getSubject().getPrincipal();
			outwardTaskAllocateService.alterStatusToMatched(outwardTaskId, null, opr, remark);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功"));
		} catch (Exception e) {
			logger.error("操作失败,匹配操作参数：taskId:{},remark:{} e:{}", outwardTaskId, outwardTaskId, e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "匹配失败" + e.getLocalizedMessage()));
		}
	}

	/**
	 * 匹配操作
	 */
	@RequestMapping("/match")
	public String match(@RequestParam(value = "bankFlowId") Long bankFlowId,
			@RequestParam(value = "outwardTaskId") Long outwardTaskId, @RequestParam(value = "remark") String remark)
			throws JsonProcessingException {
		try {
			SysUser loginUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			BizOutwardTask outwardInfo = outwardTaskService.findById(outwardTaskId);
			BizBankLog bankLog = bankLogService.get(bankFlowId);
			if (outwardInfo == null || bankLog == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "信息异常，匹配失败"));
			} else {
				if (null == outwardInfo.getStatus()
						|| !outwardInfo.getStatus().equals(OutwardTaskStatus.Deposited.getStatus())) {
					return mapper.writeValueAsString(new GeneralResponseData<>(
							GeneralResponseData.ResponseStatus.FAIL.getValue(), "出款记录无法匹配，只有已出款且未匹配的数据才允许匹配，当前状态："
									+ OutwardTaskStatus.findByStatus(outwardInfo.getStatus()).getMsg()));
				}
				// 未认领不存在数据库中，数据库只有已匹配与匹配中
				if (null == bankLog.getStatus() || !bankLog.getStatus().equals(BankLogStatus.Matching.getStatus())) {
					return mapper.writeValueAsString(
							new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
									"银行流水无法匹配，当前状态：" + BankLogStatus.findByStatus(bankLog.getStatus()).getMsg()));
				}
			}
			outwardTaskAllocateService.alterStatusToMatched(outwardTaskId, bankFlowId, loginUser, remark);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "匹配成功"));
		} catch (Exception e) {
			logger.error("/match 匹配失败：{},匹配操作参数：taskId{}，flowId:{}", e, outwardTaskId, bankFlowId);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "匹配失败" + e.getLocalizedMessage()));
		}
	}

	@RequestMapping(value = "/findInfoById")
	public String findInfoById(@RequestParam(value = "id") Long id) throws JsonProcessingException {
		try {
			GeneralResponseData<Object> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			SysUser user = (SysUser) SecurityUtils.getSubject().getPrincipal();
			PageRequest pageRequest = new PageRequest(0, AppConstants.PAGE_SIZE, Sort.Direction.ASC, "id");
			PageOutwardTaskCheck pageOutwardTaskCheck = outwardTaskService.findPage4Check(user.getUid(), id, null, null,
					null, null, null, null, null, null, null, pageRequest);
			Object data = CollectionUtils.isEmpty(pageOutwardTaskCheck.getContent()) ? null
					: pageOutwardTaskCheck.getContent().get(0);
			responseData.setData(data);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error("/findInfoById 查询失败：{},id{}", e, id);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping(value = "/list")
	public String list(@RequestParam(value = "status", required = false) Integer status,
			@RequestParam(value = "pageNo") Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "id", required = false) Long outwardTaskId,
			@RequestParam(value = "fromAccountId", required = false) Integer fromAccountId,
			@RequestParam(value = "handicapId", required = false) Integer handicapId,
			@RequestParam(value = "levelId", required = false) Integer levelId,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "minAmount", required = false) BigDecimal minAmount,
			@RequestParam(value = "maxAmount", required = false) BigDecimal maxAmount,
			@RequestParam(value = "toAccount", required = false) String toAccount) throws JsonProcessingException {
		try {
			GeneralResponseData<List<PageOutwardTaskCheck.OutwardTaskCheckContent>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			SysUser user = (SysUser) SecurityUtils.getSubject().getPrincipal();
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					Sort.Direction.DESC, "asign_time");
			Date[] startAndEndTime = CommonUtils.parseStartAndEndTime(startAndEndTimeToArray);
			PageOutwardTaskCheck pageOutwardTaskCheck = outwardTaskService.findPage4Check(user.getUid(), outwardTaskId,
					fromAccountId, handicapId, levelId, status, startAndEndTime[0], startAndEndTime[1], minAmount,
					maxAmount, StringUtils.trimToNull(toAccount), pageRequest);
			BigDecimal[] amountAndFeeTotal = outwardTaskService.findTotal4Check(outwardTaskId, fromAccountId,
					handicapId, levelId, status, startAndEndTime[0], startAndEndTime[1], minAmount, maxAmount,
					StringUtils.trimToNull(toAccount));
			Map<String, Object> header = new HashMap<>();
			header.put("totalAmount", amountAndFeeTotal[0]);
			header.put("totalFee", amountAndFeeTotal[1]);
			Paging paging = pageOutwardTaskCheck.getPage();
			paging.setHeader(header);
			responseData.setData(pageOutwardTaskCheck.getContent());
			responseData.setPage(paging);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 由处理人 人工 机器人 查找处理人id
	 */
	public Integer[] getOperator(String operatorName, Integer robot, Integer manual) {
		Integer[] operatorIds = new Integer[0];// 出款人id-操作人
		Integer[] category = new Integer[0];
		if (robot != null) {
			category = new Integer[1];
			category[0] = UserCategory.Robot.getValue();
		}
		if (manual != null) {
			category = new Integer[3];
			category[0] = -1;
			category[1] = com.xinbo.fundstransfer.domain.pojo.UserCategory.Outward.getCode();
			category[2] = com.xinbo.fundstransfer.domain.pojo.UserCategory.IncomeAudit.getCode();
		}
		if (manual != null && robot != null) {
			category = new Integer[4];
			category[0] = com.xinbo.fundstransfer.domain.pojo.UserCategory.Outward.getCode();
			category[1] = com.xinbo.fundstransfer.domain.pojo.UserCategory.IncomeAudit.getCode();
			category[2] = UserCategory.Robot.getValue();
			category[3] = -1;// 管理员
		}
		List<SysUser> sysUserList = sysUserService.findByNameAndCategory(operatorName, category);// 查询用户信息
		// 获取id
		if (sysUserList != null && sysUserList.size() > 0) {
			operatorIds = new Integer[sysUserList.size()];
			for (int i = 0, L = operatorIds.length; i < L; i++) {
				operatorIds[i] = sysUserList.get(i).getId();
			}
		}
		return operatorIds;
	}

	@RequestMapping("/customersendmessage")
	public String customerSendMessage(@RequestParam(value = "message", required = false) String message)
			throws IOException, InterruptedException {
		GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 设置日期格式
		message = df.format(new Date()) + "&nbsp;&nbsp;&nbsp;" + operator.getUsername() + "</br>" + message;
		try {
			String info = CommonUtils.genSysMsg4WS(null, SystemWebSocketCategory.CustomerService, message);
			redisService.convertAndSend(RedisTopics.BROADCAST, info);
		} catch (Exception e) {
			logger.error("方法：{}，操作失败：异常{}", request.getMethod(), e);
			e.printStackTrace();
			responseData.setMessage("发送失败！");
			return mapper.writeValueAsString(responseData);
		}
		responseData.setMessage("发送成功！");
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 完成按钮事件 待排查转完成(出款任务 出款任务汇总) 主管处理转完成 第三方出款 主管只能分配 不能点完成
	 */
	@RequestMapping("/finsh")
	public String finsh(@RequestParam(value = "type") String type, @RequestParam(value = "taskId") Long taskId,
			@RequestParam(value = "remark") String remark) throws JsonProcessingException {
		GeneralResponseData<String> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "转完成成功");
		try {
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser == null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆");
				return mapper.writeValueAsString(responseData);
			}
			BizOutwardTask bizOutwardTask = outwardTaskService.findById(taskId);
			if (bizOutwardTask == null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "任务不存在");
				return mapper.writeValueAsString(responseData);
			}
			// 代付订单 如果是正在支付 或 未知 则不能重新生成
			if (bizOutwardTask.getThirdInsteadPay() != null && bizOutwardTask.getThirdInsteadPay() == 1) {
				DaifuResult result = daifu4OutwardService.query(bizOutwardTask);
				boolean flag = !ObjectUtils.isEmpty(result)
						&& (result.getResult().getValue().equals(DaifuResult.ResultEnum.PAYING.getValue())
								|| result.getResult().getValue().equals(DaifuResult.ResultEnum.UNKOWN.getValue()));
				if (flag) {
					return mapper.writeValueAsString(new GeneralResponseData<>(
							GeneralResponseData.ResponseStatus.FAIL.getValue(), "第三方代付中不能重新生成任务!"));
				}
			}
			if (!bizOutwardTask.getStatus().equals(OutwardTaskStatus.Failure.getStatus())
					&& !bizOutwardTask.getStatus().equals(OutwardTaskStatus.ManagerDeal.getStatus())) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"任务状态已变更!");
				return mapper.writeValueAsString(responseData);
			}
			String desc = "排查任务";
			if (bizOutwardTask.getStatus().equals(OutwardTaskStatus.Failure.getStatus())) {
				desc = OutwardTaskStatus.Failure.getMsg();
			}
			if (bizOutwardTask.getStatus().equals(OutwardTaskStatus.ManagerDeal.getStatus())) {
				desc = OutwardTaskStatus.ManagerDeal.getMsg();
			}
			if (StringUtils.isNotBlank(type) && type.equals("failedOutToMatched")) {
				outwardTaskAllocateService.ToFinishFromFailOrManagerDeal(taskId, sysUser, remark);
			} else {
				outwardTaskAllocateService.ack4User(taskId, sysUser.getId(), remark, bizOutwardTask.getAccountId(),
						false, null);
				// 从主管点击完成 找流水匹配
				outwardTaskAllocateService.ToFinishFromFailOrManagerDeal(taskId, sysUser, remark);
			}
			asignFailedTaskService.updateReviewTask(taskId.intValue(), CommonUtils.genRemark(bizOutwardTask.getRemark(),
					remark, new Date(), sysUser.getUid() + "(排查:" + desc + "转完成)"), sysUser.getId());
		} catch (Exception e) {
			logger.error("转完成操作失败：{},参数：{}", e, taskId);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败:" + e.getLocalizedMessage());
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 重新生成任务时 检查是否存在流水，如果存在流水则给提示信息，避免重复出款
	 *
	 * @param taskId
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/checkBankLog")
	public String checkBankLog(@RequestParam(value = "taskId") int taskId) throws JsonProcessingException {
		GeneralResponseData<String> responseData;
		try {
			Long bankId = outwardTaskAllocateService.checkBankLog(taskId);
			if (bankId != null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						"检查流水成功");
				// 如果有记录输出日志
				logger.info("本次检查流水任务id:{}", taskId);
			} else {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "检查流水成功");
			}
		} catch (Exception e) {
			logger.error("检查流水操作失败：{},参数：{}", e, taskId);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败");
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 保存第三方账号出现金信息
	 *
	 * @param bodyJson
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/saveThirdCashOut")
	public String saveThirdCashOut(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			logger.info("Income >> RequestBody:{}", bodyJson);
			ApiIncome entity = mapper.readValue(bodyJson, ApiIncome.class);
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			outwardRequestService.saveThirdCashOut(entity.getFromId(), entity.getToAccount(), entity.getUsername(),
					entity.getToAccountBank(), entity.getAmount().equals("") ? "0" : entity.getAmount(),
					entity.getFee().equals("") ? "0" : entity.getFee(), entity.getRemark(), operator);
			return mapper.writeValueAsString(new SimpleResponseData(ResponseStatus.SUCCESS.getValue(), "Success."));
		} catch (Exception e) {
			logger.error("Income error.", e);
			return mapper.writeValueAsString(new SimpleResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"Failure, " + e.getMessage()));
		}
	}

	/**
	 * 获取测试任务
	 */
	@RequestMapping("/out/test")
	public String test(@RequestParam(value = "accId") int accId) throws JsonProcessingException {
		try {
			SysUser sysUser1 = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser1 == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			// 2-返回转帐信息
			TransferEntity data = allocateTransService.applyTrans(accId, true, null, false);
			ResponseData responseData = new ResponseData<>(ResponseStatus.SUCCESS.getValue(), "success");
			if (data != null) {
				logger.info("转账测试>> id:{} account:{} toAcc:{} toOwner:{} toAmt:{}", data.getFromAccountId(),
						data.getAccount(), data.getAccount(), data.getOwner(), data.getAmount());
				responseData
						.setMessage("将向（" + data.getBankType() + "：" + data.getOwner() + "）转" + data.getAmount() + "元");
				return mapper.writeValueAsString(responseData);
			}
			responseData.setMessage("未能分配测试转账任务，请稍后再试！");
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error("Transfer(OutGet) error.", e);
			return mapper.writeValueAsString(new ResponseData(ResponseStatus.FAIL.getValue(), e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/transToOther")
	public String transToOther(@RequestParam(value = "taskId") Long taskId,
			@RequestParam(value = "transferToOther") Integer transferToOther,
			@RequestParam(value = "remark") String remark) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		try {
			outwardTaskService.transToOther(taskId, operator, transferToOther, remark);
		} catch (Exception e) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getMessage()));
		}
		String msg = CommonUtils.genSysMsg4WS(transferToOther, SystemWebSocketCategory.System, "你有新的出款任务需要处理");
		redisService.convertAndSend(RedisTopics.BROADCAST, msg);
		return mapper.writeValueAsString(new ResponseData(ResponseStatus.SUCCESS.getValue()));
	}

	@RequestMapping("/outwardUserList")
	public String outwardUserList(@RequestParam(value = "taskId") Long taskId) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		BizOutwardTask task = outwardTaskService.findById(taskId);
		List<Map<String, Object>> data = new ArrayList<>();
		if (Objects.isNull(task.getAccountId()) || task.getAccountId() == 0) {
			List<SysUser> userList = outwardTaskService.outwardUserList(operator);
			Set<Integer> ids = new HashSet<>();
			for (SysUser u : userList) {
				if (Objects.isNull(u) || Objects.isNull(u.getId()) || ids.contains(u.getId()))
					continue;
                if(!sysUserService.online(u.getId())){
                    continue;
                }
				ids.add(u.getId());
				Map<String, Object> em = new HashMap<>();
				em.put("id", u.getId());
				em.put("uid", u.getUid());
				data.add(em);
			}
		} else {
			Specification<BizAccount> specif = DynamicSpecifications.build(BizAccount.class,
					new SearchFilter("type", SearchFilter.Operator.EQ, AccountType.OutBank.getTypeId()),
					new SearchFilter("status", SearchFilter.Operator.EQ, AccountStatus.Normal.getStatus()),
					new SearchFilter("holder", SearchFilter.Operator.ISNOTNULL, StringUtils.EMPTY));
			List<BizAccount> accList = accountService.getAccountList(specif, null);
			List<Integer> oprList = accList.stream().filter(p -> Objects.nonNull(p.getHolder()))
					.map(BizAccount::getHolder).collect(Collectors.toList());
			Set<Integer> ids = new HashSet<>();
			for (Integer opr : oprList) {
				SysUser u = sysUserService.findFromCacheById(opr);
				if (Objects.nonNull(u) && !Objects.equals(u.getId(), operator.getId())
						&& Objects.equals(u.getHandicap(), operator.getHandicap()) && !ids.contains(u.getId())) {
                    if(!sysUserService.online(u.getId())){
                        continue;
                    }
					ids.add(u.getId());
					Map<String, Object> em = new HashMap<>();
					em.put("id", u.getId());
					em.put("uid", u.getUid());
					data.add(em);
				}
			}
		}
		GeneralResponseData<List<Map<String, Object>>> res = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		res.setData(data);
		return mapper.writeValueAsString(res);
	}
}
