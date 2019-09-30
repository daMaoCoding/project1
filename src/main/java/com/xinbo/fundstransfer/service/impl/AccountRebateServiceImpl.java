package com.xinbo.fundstransfer.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import com.xinbo.fundstransfer.domain.enums.ActivityEnums;
import com.xinbo.fundstransfer.domain.enums.OutwardTaskStatus;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.TransferEntity;
import com.xinbo.fundstransfer.domain.repository.AccountMoreRepository;
import com.xinbo.fundstransfer.domain.repository.AccountRebateRepository;
import com.xinbo.fundstransfer.domain.repository.AccountReturnSummaryRepository;
import com.xinbo.fundstransfer.domain.repository.BanklogCommissionRespository;
import com.xinbo.fundstransfer.report.SystemAccountConfiguration;
import com.xinbo.fundstransfer.runtime.task.ToolResponseData;
import com.xinbo.fundstransfer.service.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Service
public class AccountRebateServiceImpl implements AccountRebateService {
	private static final Logger log = LoggerFactory.getLogger(AccountRebateServiceImpl.class);
	@Autowired
	private AccountRebateRepository accountRebateDao;
	@Autowired
	private AccountMoreService accountMoreService;
	@Autowired
	@Lazy
	private AllocateOutwardTaskService allocOutwardTaskSer;
	@Autowired
	private AccountRebateRepository accRebateDao;
	@Autowired
	private SysUserService sysUserService;
	@Autowired
	private RedisService redisService;
	@Autowired
	private AccountReturnSummaryRepository returnSummaryDao;
	@Autowired
	private CommissionHandler commissionHandler;
	@Autowired
	private BanklogCommissionRespository banklogCommissionRespository;
	@Autowired
	private RebateApiService rebateApiService;
	@Autowired
	@Lazy
	private AccountService accountService;
	@Autowired
	private AllocateIncomeAccountService allocInAccSer;
	@Autowired
	private RebateUserService rebateUserService;
	@Autowired
	private AccountMoreRepository accountMoreDao;
	@Autowired
	private HostMonitorService hostMonitorService;
	@Autowired
	private CabanaService cabanaService;
	@Autowired
	private HandicapService handicapService;
	private ObjectMapper mapper = new ObjectMapper();

	public Page<BizAccountRebate> findPage(Specification<BizAccountRebate> specification, Pageable pageable) {
		return accountRebateDao.findAll(specification, pageable);
	}

	@Override
	@Transactional
	public BizAccountRebate saveAndFlush(BizAccountRebate rebate) {
		return accountRebateDao.saveAndFlush(rebate);
	}

	@Override
	@Transactional
	public void create(BizAccountMore more, BizAccountRebate rebate) {
		BizAccountRebate his = accountRebateDao.findByTid(rebate.getTid());
		if (Objects.nonNull(his)) {
			log.info(
					"RebateV3 >>  the order already exist in db. id: {}  tid: {} uid: {} balance: {} amount: {} toacc: {} toholder toinfo: {} toBank: {}",
					rebate.getId(), rebate.getTid(), rebate.getUid(), rebate.getBalance(), rebate.getAmount(),
					rebate.getToAccount(), rebate.getToHolder(), rebate.getToAccountInfo(), rebate.getToAccountType());
			return;
		}
		accountRebateDao.saveAndFlush(rebate);
		if (rebate.getType() == 1) {
			more.setBalance(rebate.getBalance().subtract(rebate.getAmount()));
			accountMoreService.saveAndFlash(more);
			allocOutwardTaskSer.rpush(rebate, false);
		}
		log.info(
				"RebateV3 >>  user rebate . id: {}  tid: {} uid: {} balance: {} amount: {} toacc: {} toholder toinfo: {} toBank: {}",
				rebate.getId(), rebate.getTid(), rebate.getUid(), rebate.getBalance(), rebate.getAmount(),
				rebate.getToAccount(), rebate.getToHolder(), rebate.getToAccountInfo(), rebate.getToAccountType());
	}

	@Override
	public List<BizAccountRebate> findAll(Specification<BizAccountRebate> specification) {
		return accountRebateDao.findAll(specification);
	}

	@Override
	public Map<String, Object> findRebate(int status, String type, String orderNo, String fromAmount, String toMoney,
			String fristTime, String lastTime, int handicap, List<Integer> handicapList, String uName,
			String rebateType, PageRequest pageRequest) throws Exception {
		Page<Object> dataToPage;
		java.lang.Object[] total;
		rebateType = "".equals(rebateType) ? null : rebateType;
		List<Integer> statusList = new ArrayList<>();
		if (1 == status) {
			statusList.add(OutwardTaskStatus.Deposited.getStatus());
			statusList.add(OutwardTaskStatus.Matched.getStatus());
		} else
			statusList.add(status);
		Map<String, Object> map = new HashMap<String, Object>();
		dataToPage = accountRebateDao.findRebate(statusList, type, ("".equals(orderNo) ? null : orderNo),
				("".equals(fromAmount) ? null : fromAmount), ("".equals(toMoney) ? null : toMoney), fristTime, lastTime,
				handicap, handicapList, uName, rebateType, pageRequest);
		// 查询总计进行返回
		total = accountRebateDao.totalFindRebate(statusList, type, ("".equals(orderNo) ? null : orderNo),
				("".equals(fromAmount) ? null : fromAmount), ("".equals(toMoney) ? null : toMoney), fristTime, lastTime,
				handicap, handicapList, uName, rebateType);
		map.put("rebatePage", dataToPage);
		map.put("rebateTotal", total);
		return map;
	}

	@Override
	public Map<String, Object> findAuditCommission(String fristTime, String lastTime, String results,
			PageRequest pageRequest) throws Exception {
		Page<Object> dataToPage;
		java.lang.Object[] total;
		Map<String, Object> map = new HashMap<String, Object>();
		dataToPage = accountRebateDao.findAuditCommission(fristTime, lastTime, results, pageRequest);
		// 查询总计进行返回
		total = accountRebateDao.totalFindAuditCommission(fristTime, lastTime, results);
		map.put("rebatePage", dataToPage);
		map.put("rebateTotal", total);
		return map;
	}

	@Override
	public BizAccountRebate findById(Long id) {
		return accountRebateDao.findById2(id);
	}

	@Override
	public BizAccountRebate findLatestByTid(String orderNo) {
		return accountRebateDao.findLatestByTid(orderNo);
	}

	@Override
	public BizAccountRebate findRebateByBankLog(int fromAccountId, Float amount, String toAccountOrtoAccountOwner,
			int type, int cardType, int orderType) {
		return accountRebateDao.findRebateByBankLog(fromAccountId, amount, toAccountOrtoAccountOwner, type, cardType,
				orderType);
	}

	@Transactional
	@Override
	public void updateStatusById(Long id, Integer status) {
		accountRebateDao.updateStatusById(id, status);
	}

	@Transactional
	@Override
	public void reAssignDrawing(SysUser operator, Long id, String remark) {
		BizAccountRebate rebate = findById(id);
		if (Objects.nonNull(rebate) && rebate.getStatus() == OutwardTaskStatus.Failure.getStatus()
				|| Objects.nonNull(rebate) && rebate.getStatus() == OutwardTaskStatus.Undeposit.getStatus()) {
			String remarks = CommonUtils.genRemark(rebate.getRemark(), remark, new Date(), operator.getUid());
			accountRebateDao.reAssignDrawing(id, remarks);
			allocOutwardTaskSer.rpush(rebate, false);
		}
	}

	@Override
	@Transactional
	public void unknownByRobot(TransferEntity entity) {
		if (Objects.isNull(entity) || Objects.isNull(entity.getTaskId())) {
			log.info("UnknownRemitRobot (rebateId: null remark: {} screenshot: {} ) >> param 'rebateId' is null|empty");
			return;
		}
		long rebateId = entity.getTaskId();
		String remark = entity.getRemark(), screenshot = entity.getScreenshot();
		BizAccountRebate rebate = accRebateDao.findOne(rebateId);
		if (Objects.isNull(rebate)) {
			log.info("UnknownRemitRobot (rebateId: {} remark: {} screenshot: {} ) >> rebate doesn't exist.", rebateId,
					remark, screenshot);
			return;
		}
		int status = rebate.getStatus();
		if (status != OutwardTaskStatus.Undeposit.getStatus() || Objects.isNull(rebate.getAccountId())) {
			log.info(
					"UnknownRemitRobot (rebateId: {} remark: {} screenshot: {} ) >> is't in [ UNDEPOSIT ] | isn't allocated.",
					rebateId, remark, screenshot);
			return;
		}
		// 转账结果上报为3表示失败，可以重新分配出去
		if (Objects.isNull(entity.getResult()) || Objects.equals(entity.getResult(), 3)) {
			SysUser operator = sysUserService.findFromCacheById(AppConstants.USER_ID_4_ADMIN);
			String errMsg = entity.getRemark();
			reAssignDrawing(operator, rebateId, StringUtils.isNotBlank(errMsg) ? errMsg : "转账失败重新分配");
		} else {
			remark = String.format("%s(出款结果未知)", remark);
			remark = CommonUtils.genRemark(rebate.getRemark(), remark, new Date(), "系统");
			// 根据是否开启自动对账的参数决定是转待排查还是转未知
			BizHandicap handicap = handicapService.findFromCacheById(rebate.getHandicap());
			if (SystemAccountConfiguration.checkHandicapByNeedOpenService4NeedAutoSurvey4UnknownTask(handicap)) {
				accRebateDao.updRemarkOrStatus(rebateId, remark, OutwardTaskStatus.Unknown.getStatus(),
						rebate.getStatus(), screenshot);
			} else {
				accRebateDao.updRemarkOrStatus(rebateId, remark, OutwardTaskStatus.Failure.getStatus(), status,
						screenshot);
			}

		}
	}

	@Override
	public String lock(String caclTime, SysUser user) {
		Map trans = redisService.getStringRedisTemplate().opsForHash().entries(RedisKeys.REBATE_AMOUTS_KEYS);
		Object o = trans.get(caclTime);
		if (Objects.nonNull(o)) {
			return "当前所选日期返利数据已被锁定！";
		}
		redisService.getStringRedisTemplate().opsForHash().put(RedisKeys.REBATE_AMOUTS_KEYS, caclTime,
				caclTime + ":" + user.getUid());
		return "锁定成功";
	}

	@Override
	public void unlock(String caclTime, SysUser user) {
		redisService.getStringRedisTemplate().boundHashOps(RedisKeys.REBATE_AMOUTS_KEYS).delete(caclTime);
	}

	@Transactional
	@Override
	public void saveAudit(String caclTime, int status, String remark, SysUser user, String rebateUser)
			throws ParseException, JsonProcessingException {
		log.info("Approve4VerifyCommission >> [ SAVE AUDIT ] caclTime：{} status：{} remark：{} rebateUser： {}", caclTime,
				status, remark, rebateUser);
		String oldRemark = accountRebateDao.findRemark(caclTime);
		remark = CommonUtils.genRemark(oldRemark, remark, new Date(), user.getUid());
		accountRebateDao.saveAudit(caclTime, status, remark);
		redisService.getStringRedisTemplate().boundHashOps(RedisKeys.REBATE_AMOUTS_KEYS).delete(caclTime);
		if (!"".equals(rebateUser) && null != rebateUser) {
			// 保存当天黑名单信息
			String[] rebateu = rebateUser.split(",");
			for (String str : rebateu) {
				if (str != null && str.length() != 0) {
					BizRebateUser us = rebateUserService.getFromCacheByUserName(str);
					if (null != us) {
						BizAccountMore more = accountMoreService.getFromCacheByUid(us.getUid());
						if (null != more && null != more.getAccounts()) {
							String[] account = more.getAccounts().split(",");
							for (String st : account) {
								if (st != null && st.length() != 0) {
									accountRebateDao.addBlackList(st, caclTime, remark);
								}
							}
						}
					}
				}
			}
		}
		if (status == 1) {
			// 查询出要审核的数据
			List<BizAccountReturnSummary> returnSummary = returnSummaryDao.findByCalcTime(caclTime);
			List<BizBankLogCommission> cankLogCommission = banklogCommissionRespository.findByCalcTime(caclTime);
			if (cankLogCommission.size() <= 0) {
				rebateApiService.commissionDaily(caclTime);
				log.info(
						"Approve4VerifyCommission >> [ HISTORY LOGIC PASS ] caclTime：{} status：{} remark：{} rebateUser： {}",
						caclTime, status, remark, rebateUser);
			} else {
				Map<String, List<BizAccountReturnSummary>> map = new HashMap<>();
				map.put("returnSummary", returnSummary);
				String jsonfromArr = mapper.writeValueAsString(map);
				new Thread(() -> approve4VerifyCommission(jsonfromArr)).start();
				log.info(
						"Approve4VerifyCommission >> [ NEWEST LOGIC PASS ] caclTime：{} status：{} remark：{} rebateUser： {}",
						caclTime, status, remark, rebateUser);
				// redisService.rightPush(RedisTopics.REBATE_DATA, jsonfromArr);
			}
		}
	}

	@Override
	public Map<String, Object> findDetail(String rebateUser, String bankType, String caclTime, BigDecimal startAmount,
			BigDecimal endAmount, PageRequest pageRequest) throws Exception {
		Page<Object> dataToPage;
		java.lang.Object[] total;
		Map<String, Object> map = new HashMap<String, Object>();
		dataToPage = accountRebateDao.findDetail(rebateUser, bankType, caclTime, startAmount, endAmount, pageRequest);
		// 查询总计进行返回
		total = accountRebateDao.totalFindDetail(rebateUser, bankType, caclTime, startAmount, endAmount);
		map.put("rebatePage", dataToPage);
		map.put("rebateTotal", total);
		return map;
	}

	@Override
	public Map<String, Object> findComplete(String fristTime, String lastTime, PageRequest pageRequest)
			throws Exception {
		Page<Object> dataToPage;
		java.lang.Object[] total;
		Map<String, Object> map = new HashMap<String, Object>();
		dataToPage = accountRebateDao.findComplete(fristTime, lastTime, pageRequest);
		// 查询总计进行返回
		total = accountRebateDao.totalFindComplete(fristTime, lastTime);
		map.put("rebatePage", dataToPage);
		map.put("rebateTotal", total);
		return map;
	}

	@Override
	public Map<String, Object> findCompleteDetail(String rebateUser, String caclTime, PageRequest pageRequest)
			throws Exception {
		Page<Object> dataToPage;
		java.lang.Object[] total;
		Map<String, Object> map = new HashMap<String, Object>();
		dataToPage = accountRebateDao.findCompleteDetail(rebateUser, caclTime, pageRequest);
		// 查询总计进行返回
		total = accountRebateDao.totalFindCompleteDetail(rebateUser, caclTime);
		map.put("rebatePage", dataToPage);
		map.put("rebateTotal", total);
		return map;
	}

	@Transactional
	@Override
	public void recalculate(String caclTime, String remark, SysUser user) throws Exception {
		log.info("重新计算当天返利数！" + caclTime);
		String oldRemark = accountRebateDao.findRemark(caclTime);
		remark = CommonUtils.genRemark(oldRemark, remark, new Date(), user.getUid());
		accountRebateDao.updateAudit(caclTime, remark);
		// rebateApiService.commissionDailyReturnsummary(caclTime, 1);

	}

	// @Scheduled(fixedRate = 5000)
	public void approve4VerifyCommission(String json) {
		// if (!allocInAccSer.checkHostRunRight()) {
		// log.trace(
		// "rebateData >> this host {} doesn't have Right to
		// computepart-timeduty personal's daily commission.",
		// CommonUtils.getInternalIp());
		// return;
		// }
		try {
			// Object json = null;
			// json = redisService.leftPop(RedisTopics.REBATE_DATA);
			if (null == json || "" == json || "null" == json) {
				return;
			}
			log.info("Approve4VerifyCommission >> {}", json);
			BizAccountReturnSummary returnSummarys = mapper.readValue(json.toString(), BizAccountReturnSummary.class);
			List<BizAccountReturnSummary> returnSummary = returnSummarys.getReturnSummary();
			if (returnSummary.size() <= 0) {
				log.info("Approve4VerifyCommission >> [ No Summary Data ] {}", json);
				return;
			}
			Map<String, BizAccountMore> uidMore = new HashMap<>();
			List<Integer> moreIds = new ArrayList<>();
			moreIds.add(0);
			String time = returnSummary.get(0).getCalcTime();
			returnSummary = returnSummary.stream()
					.filter(p -> Objects.nonNull(p.getAmount()) && p.getAmount().compareTo(BigDecimal.ZERO) > 0
							|| Objects.nonNull(p.getAgentAmount()) && p.getAgentAmount().compareTo(BigDecimal.ZERO) > 0)
					.collect(Collectors.toList());
			returnSummary.forEach(p -> {
				AccountBaseInfo base = accountService.getFromCacheById(p.getAccount());
				if (null == base)
					return;
				BizAccountMore more = accountMoreService.findByMobile(base.getMobile());
				if (null == more)
					return;
				p.setUid(more.getUid());
				uidMore.put(more.getUid(), more);
				moreIds.add(more.getId());
			});
			Map<String, List<BizAccountReturnSummary>> uidSummaries = returnSummary.stream()
					.collect(Collectors.groupingBy(BizAccountReturnSummary::getUid));
			// 找出参加活动，但没有流水的用户（防止用户已经参加的活动已经停止）
			List<String> moibleList4JoinActivityNoFlow = accountMoreDao
					.findMoibleListByConditions(ActivityEnums.AccountMoreActivityInStatus.YES.getNum(), moreIds);
			moibleList4JoinActivityNoFlow.forEach(p -> {
				if (StringUtils.isNotBlank(p)) {
					BizAccountMore more = accountMoreService.findByMobile(p);
					if (Objects.nonNull(more)) {
						String uid = StringUtils.trimToEmpty(more.getUid());
						uidMore.put(uid, more);
						uidSummaries.put(uid, Collections.EMPTY_LIST);
					}
				}
			});
			uidSummaries.forEach((k, v) -> {
				BizAccountMore more = uidMore.get(k);
				if (null == more)
					return;
				commissionHandler.approve4VerifyCommission(more, time, v);
			});
		} catch (Exception e) {
			log.info("审核数据失败", e);
		}
	}

	@Override
	public Map<String, Object> findDerating(String orderNo, String fromAmount, String toMoney, int handicap,
			List<Integer> handicapList, String uname, String status, PageRequest pageRequest) throws Exception {
		Page<Object> dataToPage;
		java.lang.Object[] total;
		List<Integer> statusList = new ArrayList<>();
		if ("".equals(status)) {
			statusList.add(0);
			statusList.add(1);
			statusList.add(2);
			statusList.add(3);
			statusList.add(4);
			statusList.add(5);
			statusList.add(6);
			statusList.add(7);
			statusList.add(8);
			statusList.add(9);
			statusList.add(10);
			statusList.add(11);
			statusList.add(888);
			statusList.add(999);
		} else if ("2".equals(status)) {
			statusList.add(999);
		} else if ("1".equals(status)) {
			statusList.add(0);
			statusList.add(1);
			statusList.add(2);
			statusList.add(3);
			statusList.add(4);
			statusList.add(5);
			statusList.add(6);
			statusList.add(7);
			statusList.add(8);
			statusList.add(9);
			statusList.add(10);
			statusList.add(11);
		} else if ("0".equals(status)) {
			statusList.add(888);
		}
		Map<String, Object> map = new HashMap<String, Object>();
		dataToPage = accountRebateDao.findDerating(statusList, ("".equals(orderNo) ? null : orderNo),
				("".equals(fromAmount) ? null : fromAmount), ("".equals(toMoney) ? null : toMoney), handicap,
				handicapList, uname, pageRequest);
		// 查询总计进行返回
		total = accountRebateDao.totalFindDerating(statusList, ("".equals(orderNo) ? null : orderNo),
				("".equals(fromAmount) ? null : fromAmount), ("".equals(toMoney) ? null : toMoney), handicap,
				handicapList, uname);
		map.put("rebatePage", dataToPage);
		map.put("rebateTotal", total);
		return map;
	}

	@Transactional
	@Override
	public String saveDeratingAudit(Long id, int status, String remark, SysUser user)
			throws ParseException, JsonProcessingException {
		log.info("审核降额参数 >>  id：{} status：{} remark：{} ", id, status, remark);
		String message = "审核成功";
		try {
			BizAccountRebate rebate = accountRebateDao.findById2(id);
			if (null == rebate) {
				message = "该笔任务不存在！";
				return message;
			}
			BizAccountMore mo = accountMoreService.getFromByUid(rebate.getUid());
			if (mo.getMargin().compareTo(rebate.getBalance()) != 0 && status == 1) {
				message = "该兼职目前信用额度和申请前不一致！";
				return message;
			}
			if (rebate.getStatus() != 888) {
				message = "该笔任务不是审核状态！";
				return message;
			}

			if (1 == status) {
				// boolean flag = false;
				// for (String accId :
				// StringUtils.trimToEmpty(mo.getAccounts()).split(",")) {
				// if (StringUtils.isBlank(accId) ||
				// !StringUtils.isNumeric(accId))
				// continue;
				// BizAccount account =
				// accountService.getById(Integer.valueOf(accId));
				// if (null != account && account.getStatus() !=
				// AccountStatus.Freeze.getStatus()
				// && account.getStatus() !=
				// AccountStatus.Inactivated.getStatus()
				// && account.getStatus() != AccountStatus.Delete.getStatus()) {
				// flag = rebateApiService.derate(account.getAccount(),
				// -rebate.getAmount().floatValue(),
				// (mo.getMargin().subtract(rebate.getAmount())).floatValue(),
				// null, rebate.getTid());
				// break;
				// }
				// }
				// if (flag) {
				remark = CommonUtils.genRemark(rebate.getRemark(), remark, new Date(), user.getUid());
				accountRebateDao.updateStatusAndRemark(id, 888, 0, remark);
				allocOutwardTaskSer.rpush(rebate, false);
				String remarks = "当前额度：" + mo.getMargin() + ",降额后额度：" + mo.getMargin().subtract(rebate.getAmount())
						+ ",降额单号：" + rebate.getTid() + "单号金额：" + rebate.getAmount();
				mo.setRemark(CommonUtils.genRemark(mo.getRemark(), remarks, new Date(), "sys"));
				mo.setMargin(mo.getMargin().subtract(rebate.getAmount()));
				mo.setLinelimit(
						(null == mo.getLinelimit() ? BigDecimal.ZERO : mo.getLinelimit()).subtract(rebate.getAmount()));
				accountMoreService.saveAndFlash(mo);
				float reduceAmount = rebate.getAmount().floatValue();
				for (String accId : StringUtils.trimToEmpty(mo.getAccounts()).split(",")) {
					if (StringUtils.isBlank(accId) || !StringUtils.isNumeric(accId))
						continue;
					BizAccount account = accountService.getById(Integer.valueOf(accId));
					if (reduceAmount <= 0)
						break;
					if ((Objects.isNull(account.getPeakBalance()) ? BigDecimal.ZERO
							: new BigDecimal(account.getPeakBalance())).floatValue() >= reduceAmount) {
						// 当前卡够扣
						Integer pb = (int) ((Objects.isNull(account.getPeakBalance()) ? BigDecimal.ZERO
								: new BigDecimal(account.getPeakBalance())).floatValue() - reduceAmount);
						account.setPeakBalance(pb);
						log.debug("derating>> id {}", id);
						accountService.broadCast(account);
						hostMonitorService.update(account);
						cabanaService.updAcc(account.getId());
					} else {
						// 当前卡不够扣
						reduceAmount = reduceAmount - (Objects.isNull(account.getPeakBalance()) ? BigDecimal.ZERO
								: new BigDecimal(account.getPeakBalance())).floatValue();
						account.setPeakBalance(0);
						log.debug("derating>> id {}", id);
						accountService.broadCast(account);
						hostMonitorService.update(account);
						cabanaService.updAcc(account.getId());
					}
				}
				// } else {
				// message = "返利网信用额度确认失败";
				// return message;
				// }
			} else {
				boolean flag = rebateApiService.limitCancel(mo.getUid(), mo.getMargin(), remark, rebate.getTid());
				if (flag) {
					remark = CommonUtils.genRemark(rebate.getRemark(), remark, new Date(), user.getUid());
					accountRebateDao.updateStatusAndRemark(id, 888, 999, remark);
				} else {
					message = "返利网取消失败";
					return message;
				}
			}
		} catch (Exception e) {
			log.error("审核降额失败", e);
			message = "审核降额失败";
		}
		return message;

	}
}
