package com.xinbo.fundstransfer.restful.api;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.report.SystemAccountConfiguration;
import com.xinbo.fundstransfer.service.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.net.socket.AccountEntity;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.GeneralResponseData.ResponseStatus;
import com.xinbo.fundstransfer.domain.ResponseData;
import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.domain.enums.OutwardTaskStatus;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalPush;
import com.xinbo.fundstransfer.domain.pojo.TransLock;
import com.xinbo.fundstransfer.domain.pojo.TransferEntity;
import com.xinbo.fundstransfer.report.SystemAccountManager;
import com.xinbo.fundstransfer.restful.BaseController;
import com.xinbo.fundstransfer.sec.FundTransferEncrypter;

/**
 * 出款接口
 *
 * 
 *
 */
@RestController("apiOutwardTaskController")
@RequestMapping("/api/transfer")
public class OutwardTaskController extends BaseController {
	private static final Logger log = LoggerFactory.getLogger(OutwardTaskController.class);
	@Autowired
	AllocateOutwardTaskService outwardTaskAllocateService;
	@Autowired
	AllocateTransferService allocateTransferService;
	@Autowired
	AllocateTransService allocateTransService;
	@Autowired
	OutwardTaskService outwardTaskService;
	@Autowired
	Environment environment;
	@Autowired
	AccountService accountService;
	@Autowired
	SysUserService userService;
	@Autowired
	com.xinbo.fundstransfer.restful.AccountController accountController;
	@Autowired
	private TransMonitorService transMonitorService;
	@Autowired
	OutwardTaskService oTaskSer;
	@Autowired
	OutwardRequestService oReqSer;
	@Autowired
	AccountExpOprService accountExpOprService;
	@Autowired
	AccountRebateService accountRebateService;
	@Autowired
	private RebateApiService rebateApiService;
	@Autowired
	AllocateTransService allocTransSer;
	@Autowired
	SystemAccountManager systemAccountManager;
	@Autowired
	HandicapService handicapService;

	private ObjectMapper mapper = new ObjectMapper();

	/**
	 * 获取下发任务
	 */
	@RequestMapping("/in/get")
	public String in(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.info("Transfer(InGet) >> RequestBody:{}", bodyJson);
			AccountEntity entity = mapper.readValue(bodyJson, AccountEntity.class);
			// 1-验证参数
			if (!md5(entity.getAccount()).equals(entity.getToken())) {
				log.info("Failure, invalid Token.");
				return mapper.writeValueAsString(
						new ResponseData(ResponseStatus.FAIL.getValue(), "Failure, invalid Token."));
			}
			AccountBaseInfo base = accountService.getFromCacheById(entity.getId());
			// 2-返回转帐信息
			TransferEntity data = AppConstants.NEW_TRANSFER
					? CommonUtils.checkDistHandicapNewVersion(base.getHandicapId())
							? allocateTransService.applyByFromNew(entity.getId(), entity.getBankBalance())
							: allocateTransService.applyByFrom(entity.getId(), entity.getBankBalance())
					: allocateTransferService.applyByFrom(entity.getId(), entity.getBankBalance());
			// 3-账号余额实时上报
			if (AppConstants.NEW_TRANSFER) {
				allocateTransService.applyRelBal(entity.getId(), entity.getBankBalance());
			} else {
				allocateTransferService.applyRelBal(entity.getId(), entity.getBankBalance(), false);
			}
			if (data != null) {
				log.info("Transfer(InGet) >> Response  fromId:{} toId:{} toAccount:{} bankBalance:{} amount:{}",
						data.getFromAccountId(), data.getToAccountId(), data.getAccount(), entity.getBankBalance(),
						data.getAmount());
			} else {
				log.info("Transfer(InGet) >> Response  fromId:{}  to:null bankBalance:{} ", entity.getId(),
						entity.getBankBalance());
			}
			ResponseData<TransferEntity> responseData = new ResponseData<>(ResponseStatus.SUCCESS.getValue(),
					"success");
			responseData.setData(data);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			log.error("Transfer(InGet) error.", e);
			return mapper.writeValueAsString(new ResponseData(ResponseStatus.FAIL.getValue(), e.getLocalizedMessage()));
		}
	}

	/**
	 * 下发完成，向服务端响应结果
	 */
	@RequestMapping("/in/ack")
	public String inAck(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.info("Transfer(inAck) >> RequestBody:{}", bodyJson);
			TransferEntity entity = mapper.readValue(bodyJson, TransferEntity.class);
			// 1-验证参数
			if (!md5(entity.getAccount()).equals(entity.getToken())) {
				log.info("Failure, invalid Token.");
				return mapper.writeValueAsString(
						new SimpleResponseData(ResponseStatus.FAIL.getValue(), "Failure, invalid Token."));
			}
			if (Objects.equals(entity.getResult(), 3)) {
				redisService.increment(RedisKeys.COUNT_FAILURE_TRANS, String.valueOf(entity.getFromAccountId()), 1);
			} else if (Objects.equals(entity.getResult(), 1)) {
				redisService.getFloatRedisTemplate().opsForHash().delete(RedisKeys.COUNT_FAILURE_TRANS,
						String.valueOf(entity.getFromAccountId()));
			}
			systemAccountManager.rpush(new SysBalPush(entity.getFromAccountId(), SysBalPush.CLASSIFY_TRANSFER, entity));
			if (Objects.nonNull(entity.getTaskId())) {
				BizOutwardTask task = oTaskSer.findById(entity.getTaskId());
				if (Objects.nonNull(task) && !Objects.equals(entity.getFromAccountId(), task.getAccountId())) {
					log.error(
							"Transfer(inAck) >> task has been allocated to another acc, ori acc: {} cur acc: {} taskid: {} ",
							entity.getFromAccountId(), task.getAccountId(), entity.getTaskId());
					return mapper.writeValueAsString(
							new SimpleResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "success"));
				}
			}
			// 2-转账确认
			if (AppConstants.NEW_TRANSFER) {
				allocateTransService.ackByRobot(entity);
				transMonitorService.reportTransResult(entity);
			} else {
				allocateTransferService.ackByRobot(entity);
			}
			// 3-账号余额实时上报
			if (entity.getFromAccountId() != null) {
				if (AppConstants.NEW_TRANSFER) {
					allocateTransService.applyRelBal(entity.getFromAccountId(),
							entity.getBalance() == null ? null : new BigDecimal(entity.getBalance()));
				} else if (Objects.nonNull(entity.getBalance())) {
					allocateTransferService.applyRelBal(entity.getFromAccountId(), new BigDecimal(entity.getBalance()),
							false);
				}
			} else {
				log.error("Transfer(inAck) error. fromAccountId is Empty. RequestBody:{}", bodyJson);
			}
			return mapper.writeValueAsString(new SimpleResponseData(ResponseStatus.SUCCESS.getValue(), "success"));
		} catch (Exception e) {
			log.error("Transfer(inAck) error.", e);
			return mapper.writeValueAsString(new SimpleResponseData(ResponseStatus.FAIL.getValue(),
					"Transfer ack error." + e.getLocalizedMessage()));
		}
	}

	/**
	 * 获取出款任务
	 */
	@RequestMapping("/out/get")
	public String out(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.info("Transfer(OutGet) >> RequestBody:{}", bodyJson);
			AccountEntity entity = mapper.readValue(bodyJson, AccountEntity.class);
			// 1-验证参数
			if (!md5(entity.getAccount()).equals(entity.getToken())) {
				log.info("Failure, invalid Token.");
				return mapper.writeValueAsString(
						new ResponseData(ResponseStatus.FAIL.getValue(), "Failure, invalid Token."));
			}
			AccountBaseInfo base = accountService.getFromCacheById(entity.getId());
			TransferEntity data = null;
			// 2-返回转帐信息
			if (CommonUtils.checkDistHandicapNewVersion(base.getHandicapId())) {
				data = outwardTaskAllocateService.applyTask4RobotNew(entity.getId(), entity.getBankBalance());
			} else {
				data = outwardTaskAllocateService.applyTask4Robot(entity.getId(), entity.getBankBalance());
			}
			// 3-账号余额实时上报
			if (AppConstants.NEW_TRANSFER) {
				allocateTransService.applyRelBal(entity.getId(), entity.getBankBalance());
			} else {
				allocateTransferService.applyRelBal(entity.getId(), entity.getBankBalance(), Objects.nonNull(data));
			}
			if (data != null) {
				log.info("Transfer(OutGet) >> Response  accountId:{} account:{} bankBalance:{} taskId:{}  amount:{}",
						data.getFromAccountId(), data.getAccount(), entity.getBankBalance(), data.getTaskId(),
						data.getAmount());
			}
			ResponseData<TransferEntity> responseData = new ResponseData<>(ResponseStatus.SUCCESS.getValue(),
					"success");
			responseData.setData(data);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			log.error("Transfer(OutGet) error.", e);
			return mapper.writeValueAsString(new ResponseData(ResponseStatus.FAIL.getValue(), e.getLocalizedMessage()));
		}
	}

	/**
	 * 出款完成，向服务端响应结果
	 */
	@RequestMapping("/out/ack")
	public String outAck(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.info("Transfer(outAck) >> RequestBody:{}", bodyJson);
			TransferEntity entity = mapper.readValue(bodyJson, TransferEntity.class);
			// 1-验证参数
			if (!md5(entity.getAccount()).equals(entity.getToken())) {
				log.info("Failure, invalid Token.");
				return mapper.writeValueAsString(
						new SimpleResponseData(ResponseStatus.FAIL.getValue(), "Failure, invalid Token."));
			}
			if (Objects.equals(entity.getResult(), 3)) {
				redisService.increment(RedisKeys.COUNT_FAILURE_TRANS, String.valueOf(entity.getFromAccountId()), 1);
			} else if (Objects.equals(entity.getResult(), 1)) {
				redisService.getFloatRedisTemplate().opsForHash().delete(RedisKeys.COUNT_FAILURE_TRANS,
						String.valueOf(entity.getFromAccountId()));
			}
			systemAccountManager.rpush(new SysBalPush(entity.getFromAccountId(), SysBalPush.CLASSIFY_TRANSFER, entity));
			boolean memberTask = true;
			// 出款完成确认
			BizOutwardTask task = null;
			if (Objects.nonNull(entity.getTaskId())) {
				allocTransSer.llockUpdStatus(entity.getFromAccountId(), entity.getTaskId().intValue(),
						TransLock.STATUS_DEL);
				task = oTaskSer.findById(entity.getTaskId());
				BizAccountRebate rebate = null;
				if (Objects.isNull(task)) {
					rebate = accountRebateService.findById(entity.getTaskId());
				}
				outwardTaskAllocateService.sreenshot(task, entity.getScreenshot());
				if (Objects.isNull(task) && Objects.isNull(rebate)) {
					return mapper.writeValueAsString(
							new SimpleResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "success"));
				} else if ((Objects.nonNull(task) && !Objects.equals(entity.getFromAccountId(), task.getAccountId()))
						|| (Objects.nonNull(rebate)
								&& !Objects.equals(entity.getFromAccountId(), rebate.getAccountId()))) {
					log.error(
							"Transfer(outAck) >> task has been allocated to another acc, ori acc: {} cur acc: {} taskid: {} ",
							entity.getFromAccountId(),
							Objects.nonNull(task) ? task.getAccountId() : rebate.getAccountId(), entity.getTaskId());
					return mapper.writeValueAsString(
							new SimpleResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "success"));
				}
				if (Objects.nonNull(task) && Objects.equals(task.getStatus(), OutwardTaskStatus.Matched.getStatus())) {
					BizOutwardRequest req = oReqSer.get(task.getOutwardRequestId());
					if (Objects.nonNull(req)) {
						outwardTaskAllocateService.noticePlatIfFinished(AppConstants.USER_ID_4_ADMIN, req);
					}
					outwardTaskAllocateService.updateBizOutwardTask(task, entity);
					return mapper.writeValueAsString(
							new SimpleResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "success"));
				}
				if (Objects.nonNull(rebate)) {
					memberTask = false;
				}
			}
			if (memberTask) {
				// 一键转出、测试转账上报
				if (Objects.isNull(entity.getTaskId()) && Objects.nonNull(entity.getRemark())
						&& entity.getRemark().contains("一键转出"))
					allocateTransService.ackTrans(entity);
				else {
					// result为空或3，表示转账失败，可以重新分配
					SysUser operator = userService.findFromCacheById(AppConstants.USER_ID_4_ADMIN);
					if (Objects.isNull(entity.getResult()) || Objects.equals(entity.getResult(), 3)) {
						if (Objects.nonNull(entity.getTaskId())) {
							outwardTaskAllocateService.remark4Mgr(entity.getTaskId(), false, false, operator, null,
									StringUtils.trimToEmpty(entity.getRemark()) + "机器转出机器出款" + entity.getResult());
						} else {
							outwardTaskAllocateService.remark4Mgr(entity.getFromAccountId(), false, false, operator,
									"机器转出机器出款");
						}
					} else if (entity.getResult() == 1) {// 出款成功
						outwardTaskAllocateService.ack4Robot(entity);
					} else {// 出款失败 转主管 // result为4表示未知，等待排查
						String remark = StringUtils.isBlank(entity.getRemark()) ? "机器转主管" : entity.getRemark();
						remark = remark + "-P" + entity.getResult();
						if (entity.getTaskId() != null) {
							remark = StringUtils.trimToEmpty(entity.getRemark()) + "-P" + entity.getResult();
							BizHandicap handicap = Objects.isNull(task) ? null
									: handicapService.findFromCacheByCode(task.getHandicap());
							if (SystemAccountConfiguration
									.checkHandicapByNeedOpenService4NeedAutoSurvey4UnknownTask(handicap)) {
								outwardTaskAllocateService.alterStatusToUnknown(entity.getTaskId(), operator, remark,
										StringUtils.trimToNull(entity.getScreenshot()));
							} else {
								outwardTaskAllocateService.alterStatusToMgr(entity.getTaskId(), operator, remark,
										StringUtils.trimToNull(entity.getScreenshot()));
							}
						} else {
							outwardTaskAllocateService.alterStatusToMgr(entity.getFromAccountId(), remark);
						}
					}
				}
			} else {
				if (entity.getResult() != null && entity.getResult() == 1) {
					rebateApiService.confirmByRobot(entity);
				} else {
					accountRebateService.unknownByRobot(entity);
				}
			}
			outwardTaskAllocateService.recordLog(entity.getFromAccountId(), entity.getTaskId());
			// 账号回收
			if (entity.getBalance() != null && entity.getBalance() <= 50
					&& !allocateTransferService.checkBlack(entity.getFromAccountId())) {
				try {
					accountController.recycle4OutwardAccount(entity.getFromAccountId(), AppConstants.USER_ID_4_ADMIN);
				} catch (Exception e) {
					log.error("账号回收>> {}", e);
				}
			}
			return mapper.writeValueAsString(new SimpleResponseData(ResponseStatus.SUCCESS.getValue(), "success"));
		} catch (Exception e) {
			log.error("Transfer(outAck) error.", e);
			return mapper.writeValueAsString(new SimpleResponseData(ResponseStatus.FAIL.getValue(),
					"Transfer(outAck) error." + e.getLocalizedMessage()));
		}
	}

	/**
	 * 出款任务转主管
	 */
	@RequestMapping("/out/turn")
	public String turn(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.info("Transfer(outTurn) >> RequestBody:{}", bodyJson);
			TransferEntity entity = mapper.readValue(bodyJson, TransferEntity.class);
			// 1-验证参数
			if (!md5(entity.getAccount()).equals(entity.getToken())) {
				log.info("Failure, invalid Token.");
				return mapper.writeValueAsString(
						new SimpleResponseData(ResponseStatus.FAIL.getValue(), "Failure, invalid Token."));
			}
			if (Objects.equals(entity.getResult(), 3)) {
				redisService.increment(RedisKeys.COUNT_FAILURE_TRANS, String.valueOf(entity.getFromAccountId()), 1);
			} else if (Objects.equals(entity.getResult(), 1)) {
				redisService.getFloatRedisTemplate().opsForHash().delete(RedisKeys.COUNT_FAILURE_TRANS,
						String.valueOf(entity.getFromAccountId()));
			}
			systemAccountManager.rpush(new SysBalPush(entity.getFromAccountId(), SysBalPush.CLASSIFY_TRANSFER, entity));
			BizOutwardTask task = null;
			if (Objects.nonNull(entity.getTaskId())) {
				allocTransSer.llockUpdStatus(entity.getFromAccountId(), entity.getTaskId().intValue(),
						TransLock.STATUS_DEL);
				// 防止入款卡转主管后，对应的translock中的数据没有删除，影响后续出款、下发任务，这里进行解锁
				allocateTransService.llockUpdStatus(entity.getFromAccountId(), entity.getTaskId().intValue(),
						TransLock.STATUS_DEL);
				task = oTaskSer.findById(entity.getTaskId());
				outwardTaskAllocateService.sreenshot(task, entity.getScreenshot());
				if (Objects.isNull(task)) {
					BizAccountRebate rebate = rebateApiService.findById(entity.getTaskId());
					if (Objects.isNull(rebate)) {
						return mapper.writeValueAsString(new SimpleResponseData(
								GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "success"));
					} else {
						rebateApiService.failByRobot(entity);
					}
				} else if (!Objects.equals(entity.getFromAccountId(), task.getAccountId())) {
					log.error(
							"OutwardTaskController >> task has been allocated to another acc, ori acc: {} cur acc: {} ",
							entity.getFromAccountId(), task.getAccountId());
					return mapper.writeValueAsString(
							new SimpleResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "success"));
				}
				if (Objects.equals(task.getStatus(), OutwardTaskStatus.Matched.getStatus())) {
					BizOutwardRequest req = oReqSer.get(task.getOutwardRequestId());
					if (Objects.nonNull(req))
						outwardTaskAllocateService.noticePlatIfFinished(AppConstants.USER_ID_4_ADMIN, req);
					return mapper.writeValueAsString(
							new SimpleResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "success"));
				}
			}
			if (Objects.nonNull(entity.getToAccountId())) {
				allocateTransService.ackByRobot(entity);
			}
			// 一键转出余额、账户激活、测试转账上报
			// Objects.nonNull(entity.getAmount())
			// 过滤掉工具端只提供entity.getFromAccountId()的数据
			else if (Objects.nonNull(entity.getAmount()) && Objects.isNull(entity.getTaskId())
					&& Objects.nonNull(entity.getRemark()) && entity.getRemark().contains("一键转出")) {
				allocateTransService.ackTrans(entity);
			} else {
				SysUser operator = userService.findFromCacheById(AppConstants.USER_ID_4_ADMIN);
				if (Objects.isNull(entity.getResult()) || Objects.equals(entity.getResult(), 3)) {
					if (Objects.nonNull(entity.getTaskId())) {
						outwardTaskAllocateService.remark4Mgr(entity.getTaskId(), false, false, operator, null,
								"机器转出机器出款" + entity.getResult());
					} else {
						outwardTaskAllocateService.remark4Mgr(entity.getFromAccountId(), false, false, operator,
								"机器转出机器出款");
					}
				} else if (entity.getResult() == 1) {// 工具上报出款成功
					outwardTaskAllocateService.ack4Robot(entity);
				} else {// 工具上报出款未知
					String remark = StringUtils.isBlank(entity.getRemark()) ? "机器转主管" : entity.getRemark();
					remark = remark + "-P" + entity.getResult();
					if (entity.getTaskId() != null) {
						String screenshot = StringUtils.trimToNull(entity.getScreenshot());
						remark = StringUtils.trimToEmpty(entity.getRemark()) + "-P" + entity.getResult();
						BizHandicap handicap = Objects.isNull(task) ? null
								: handicapService.findFromCacheByCode(task.getHandicap());
						if (SystemAccountConfiguration
								.checkHandicapByNeedOpenService4NeedAutoSurvey4UnknownTask(handicap)) {
							outwardTaskAllocateService.alterStatusToUnknown(entity.getTaskId(), null, remark,
									screenshot);
						} else {
							outwardTaskAllocateService.alterStatusToMgr(entity.getTaskId(), operator, remark,
									screenshot);
						}
					} else {
						outwardTaskAllocateService.alterStatusToMgr(entity.getFromAccountId(), remark);
					}
				}
				outwardTaskAllocateService.recordLog(entity.getFromAccountId(), entity.getTaskId());
			}
			return mapper.writeValueAsString(new SimpleResponseData(ResponseStatus.SUCCESS.getValue(), "success"));
		} catch (Exception e) {
			log.error("Transfer(outTurn) error.", e);
			return mapper.writeValueAsString(new SimpleResponseData(ResponseStatus.FAIL.getValue(),
					"Transfer(outTurn) error." + e.getLocalizedMessage()));
		}
	}

	/**
	 * 出款卡请求下发，出款卡没钱了
	 */
	@RequestMapping("/pay")
	public String pay(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.info("Transfer(Pay) >> RequestBody:{}", bodyJson);
			AccountEntity entity = mapper.readValue(bodyJson, AccountEntity.class);
			if (!md5(entity.getAccount()).equals(entity.getToken())) {
				log.info("Failure, invalid Token.");
				return "Failure, invalid Token.";
			}
			systemAccountManager
					.rpush(new SysBalPush(entity.getId(), SysBalPush.CLASSIFY_BANK_BAL, entity.getBankBalance()));
			if (AppConstants.NEW_TRANSFER) {
				allocateTransService.applyRelBal(entity.getId(), entity.getBankBalance());
			} else {
				allocateTransferService.applyRelBal(entity.getId(), entity.getBankBalance(), false);
			}
			return mapper.writeValueAsString(new SimpleResponseData(ResponseStatus.SUCCESS.getValue(), "success"));
		} catch (Exception e) {
			log.error("Transfer(Pay) error.", e);
			return mapper.writeValueAsString(new SimpleResponseData(ResponseStatus.FAIL.getValue(),
					"Transfer(Pay) error." + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/logTm")
	public String logTm(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.info("Transfer(logTm) >> RequestBody:{}", bodyJson);
			AccountEntity entity = mapper.readValue(bodyJson, AccountEntity.class);
			if (!md5(entity.getAccount()).equals(entity.getToken())) {
				log.info("Failure, invalid Token.");
				return mapper.writeValueAsString(
						new SimpleResponseData(ResponseStatus.FAIL.getValue(), "Failure, invalid Token."));
			}
			if (Objects.nonNull(entity.getBankBalance()) && entity.getBankBalance().compareTo(BigDecimal.ZERO) > 0) {
				if (AppConstants.NEW_TRANSFER) {
					allocateTransService.applyRelBal(entity.getId(), entity.getBankBalance());
				} else {
					allocateTransferService.applyRelBal(entity.getId(), entity.getBankBalance(), false);
				}
			}
			systemAccountManager.rpush(
					new SysBalPush(entity.getId(), SysBalPush.CLASSIFY_BANK_LOGS_TIME, System.currentTimeMillis()));
			return mapper.writeValueAsString(new SimpleResponseData(ResponseStatus.SUCCESS.getValue(), "success"));
		} catch (Exception e) {
			log.error("Transfer(logTm) error.", e);
			return mapper.writeValueAsString(new SimpleResponseData(ResponseStatus.FAIL.getValue(),
					"Transfer(logTm) error." + e.getLocalizedMessage()));
		}
	}

	/**
	 * 更新回执
	 * 
	 * @param taskId
	 *            任务ID
	 * @param receiptImg
	 *            回执路径
	 */
	@RequestMapping("/receipt")
	public @ResponseBody SimpleResponseData receipt(@RequestParam(value = "taskId") Long taskId,
			@RequestParam(value = "receiptImg") String receiptImg) {
		try {
			log.info("receipt >> taskId:{}, receiptImg: {}", taskId, receiptImg);
			BizOutwardTask task = outwardTaskService.findById(taskId);
			if (null != task) {
				task.setScreenshot(receiptImg);
				outwardTaskService.update(task);
				log.info("Update receipt success.");
				return new SimpleResponseData(1, "OK");
			} else {
				log.info("Outward task does not exist.");
				return new SimpleResponseData(500, "Outward task does not exist");
			}
		} catch (Exception e) {
			log.error("Update receipt error.", e);
			return new SimpleResponseData(500, e.getLocalizedMessage());
		}
	}

	/**
	 * 获取出款任务
	 */
	@RequestMapping("/out/test")
	public String test(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.info("Transfer(OutTest) >> RequestBody:{}", bodyJson);
			AccountEntity entity = mapper.readValue(bodyJson, AccountEntity.class);
			// 1-验证参数
			if (!md5(entity.getAccount()).equals(entity.getToken())) {
				log.info("Failure, invalid Token.");
				return mapper.writeValueAsString(
						new ResponseData(ResponseStatus.FAIL.getValue(), "Failure, invalid Token."));
			}
			// 2-返回转帐信息
			TransferEntity data = allocateTransService.applyTrans(entity.getId(), false, null, false);// allocateTransferService.applyByTest(entity.getId());
			if (data != null) {
				log.info("转账测试>> id:{} account:{} toAcc:{} toOwner:{} toAmt:{}", data.getFromAccountId(),
						entity.getAccount(), data.getAccount(), data.getOwner(), data.getAmount());
			} else {
				data = allocateTransService.getTestTrans(entity.getId(), true);
				if (data != null)
					log.info("转账测试>> id:{} account:{} toAcc:{} toOwner:{} toAmt:{}", data.getFromAccountId(),
							entity.getAccount(), data.getAccount(), data.getOwner(), data.getAmount());
			}
			ResponseData<TransferEntity> responseData = new ResponseData<>(ResponseStatus.SUCCESS.getValue(),
					"success");
			responseData.setData(data);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			log.error("Transfer(OutGet) error.", e);
			return mapper.writeValueAsString(new ResponseData(ResponseStatus.FAIL.getValue(), e.getLocalizedMessage()));
		}
	}

	/**
	 * 一键转出余额
	 */
	@RequestMapping("/out/onetime")
	public String oneTime(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.info("Transfer(OneTime) >> RequestBody:{}", bodyJson);
			AccountEntity entity = mapper.readValue(bodyJson, AccountEntity.class);
			// 1-验证参数
			if (!md5(entity.getAccount()).equals(entity.getToken())) {
				log.info("Failure, invalid Token.");
				return mapper.writeValueAsString(
						new ResponseData(ResponseStatus.FAIL.getValue(), "Failure, invalid Token."));
			}
			BigDecimal balace = entity.getBankBalance();
			if (Objects.nonNull(balace)) {
				float amount = balace.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue() - 50;
				if (amount > 0) {
					AccountBaseInfo base = accountService.getFromCacheById(entity.getId());
					if(base.getBankName().contains("南京")) {
						amount = Math.min(amount, 39950.00f);
					}else {
						amount = Math.min(amount, 49000.00f);
					}
					// 2-返回转帐信息
					TransferEntity data = allocateTransService.applyTrans(entity.getId(), true, amount, false);
					if (data != null) {
						log.info("一键转出余额>> id:{} account:{} toAcc:{} toOwner:{} toAmt:{}", data.getFromAccountId(),
								entity.getAccount(), data.getAccount(), data.getOwner(), data.getAmount());
					}
					ResponseData<TransferEntity> responseData = new ResponseData<>(ResponseStatus.SUCCESS.getValue(),
							"success");
					responseData.setData(data);
					return mapper.writeValueAsString(responseData);
				}
			}
			log.info("Failure, Balance is null or balance less then 50");
			return mapper.writeValueAsString(new ResponseData(ResponseStatus.FAIL.getValue(),
					"Failure, balance is null or balance less then 50"));
		} catch (Exception e) {
			log.error("Transfer(OneTime) error.", e);
			return mapper.writeValueAsString(new ResponseData(ResponseStatus.FAIL.getValue(), e.getLocalizedMessage()));
		}
	}

	/**
	 * 账号异常操作
	 */
	@RequestMapping("/expOpr")
	public String expOpr(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.info("Transfer(ExpOpr) >> RequestBody:{}", bodyJson);
			HashMap<String, Object> entity = mapper.readValue(bodyJson, HashMap.class);
			String content = (String) entity.get("content");
			Integer accountId = (Integer) entity.get("accountId");
			Long clientTime = (Long) entity.get("clientTime");
			if (Objects.isNull(entity) || StringUtils.isBlank(content) || Objects.isNull(accountId)
					|| Objects.isNull(clientTime)) {
				return mapper.writeValueAsString(new ResponseData<>(ResponseStatus.FAIL.getValue(), "信息不全"));
			}
			BizAccountExpOpr opr = new BizAccountExpOpr();
			opr.setAccountId(accountId);
			opr.setClientTime(new Date(clientTime));
			opr.setContent(content);
			accountExpOprService.record(opr);
			return mapper.writeValueAsString(new ResponseData<>(ResponseStatus.SUCCESS.getValue(), "success"));
		} catch (Exception e) {
			log.error("Transfer(ExpOpr) error.", e);
			return mapper.writeValueAsString(new ResponseData(ResponseStatus.FAIL.getValue(), e.getLocalizedMessage()));
		}
	}

	/**
	 * 账号异常操作
	 */
	@RequestMapping("/publicKey")
	public String publicKey(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.info("Transfer(publicKey) >> RequestBody:{}", bodyJson);
			HashMap<String, Object> entity = mapper.readValue(bodyJson, HashMap.class);
			String ip = (String) entity.get("ip");
			String mac = (String) entity.get("mac");
			String token = (String) entity.get("token");
			if (StringUtils.isBlank(ip) || Objects.isNull(mac)) {
				return StringUtils.EMPTY;
			}
			if (!md5(StringUtils.trimToEmpty(ip) + StringUtils.trimToEmpty(mac))
					.equals(StringUtils.trimToEmpty(token))) {
				log.info("Failure, invalid Token.");
				return StringUtils.EMPTY;
			}
			log.info("PCPublicKey=> ip:{} mac:{}", ip, mac);
			return new String(
					org.apache.mina.util.Base64.encodeBase64(FundTransferEncrypter.publicKeyPc.getBytes("UTF-8")),
					"UTF-8");
		} catch (Exception e) {
			log.error("Transfer(publicKey) error. {} ", FundTransferEncrypter.publicKeyPc, e);
		}
		return StringUtils.EMPTY;
	}

	/**
	 * 生成MD5
	 * 
	 */
	private String md5(String raw) {
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			md5.update((raw + environment.getProperty("funds.transfer.apikey")).getBytes());
			// 进行哈希计算并返回结果
			byte[] btResult = md5.digest();
			// 进行哈希计算后得到的数据的长度
			StringBuilder md5Token = new StringBuilder();
			for (byte b : btResult) {
				int bt = b & 0xff;
				if (16 > bt) {
					md5Token.append(0);
				}
				md5Token.append(Integer.toHexString(bt));
			}
			return md5Token.toString();
		} catch (NoSuchAlgorithmException e) {
			log.error("", e);
			return "";
		}
	}
}