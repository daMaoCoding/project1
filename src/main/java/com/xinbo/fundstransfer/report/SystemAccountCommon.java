package com.xinbo.fundstransfer.report;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.*;
import com.xinbo.fundstransfer.domain.pojo.*;
import com.xinbo.fundstransfer.domain.repository.*;
import com.xinbo.fundstransfer.report.acc.ErrorAlarm;
import com.xinbo.fundstransfer.report.acc.ErrorHandler;
import com.xinbo.fundstransfer.report.acc.SysAccPush;
import com.xinbo.fundstransfer.report.store.StoreHandler;
import com.xinbo.fundstransfer.report.up.ReportInvstError;
import com.xinbo.fundstransfer.report.up.ReportYSF;
import com.xinbo.fundstransfer.service.*;
import com.xinbo.fundstransfer.utils.ServiceDomain;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xinbo.fundstransfer.component.redis.RedisKeys;

@Service
public class SystemAccountCommon {
	private static final Logger logger = LoggerFactory.getLogger(SystemAccountCommon.class);
	@Autowired
	private AccountRepository accDao;
	@Lazy
	@Autowired
	private AccountService accSer;
	@Autowired
	private OutwardTaskRepository outwardTaskDao;
	@Autowired
	protected OutwardRequestRepository oReqDao;
	@Lazy
	@Autowired
	protected AllocateOutwardTaskService allocOTaskSer;
	@Lazy
	@Autowired
	private AccountRebateRepository accountRebateDao;
	@Lazy
	@Autowired
	private SysUserService userSer;
	@Lazy
	@Autowired
	protected RebateApiService reApiSer;
	@Lazy
	@Autowired
	protected HandicapService handicapService;
	@Lazy
	@Autowired
	protected SysLogService sysLogService;
	@Lazy
	@Autowired
	private SysErrRepository sysErrDao;
	@Lazy
	@Autowired
	private BankLogRepository bankLogDao;
	@Lazy
	@Autowired
	private AllocateIncomeAccountService alloIncomeAccountSer;
	@Lazy
	@Autowired
	private FinLessStatService finLessStatSer;
	@Lazy
	@Autowired
	protected OutwardTaskService oTaskSer;
	@Autowired
	protected StoreHandler storeHandler;

	private static boolean checkHostRunRight = false;

	@Value("${service.tag}")
	public void setServiceTag(String serviceTag) {
		if (Objects.nonNull(serviceTag)) {
			checkHostRunRight = ServiceDomain.valueOf(serviceTag) == ServiceDomain.ACCOUNTING;
		}
		logger.info("SB ACCOUNTING HOST {} >> execute right : {}", CommonUtils.getInternalIp(), checkHostRunRight);
	}

	public static boolean checkHostRunRight() {
		return checkHostRunRight;
	}

	/**
	 * 储存银行流水抓取时间
	 */
	public void crawlTime4BankStatement(StringRedisTemplate template, int accountId, long crawlTime) {
		if (Objects.isNull(template) || accountId <= 0 || crawlTime <= 0) {
			return;
		}
		template.boundHashOps(RedisKeys.SYS_LAST_LOGS_TIME).put(String.valueOf(accountId), String.valueOf(crawlTime));
	}

	/**
	 * 获取最近银行流水爬取时间
	 */
	public long crawlTime4BankStatement(StringRedisTemplate template, int accountId) {
		String ret = (String) template.boundHashOps(RedisKeys.SYS_LAST_LOGS_TIME).get(String.valueOf(accountId));
		if (Objects.isNull(ret) || StringUtils.isBlank(ret) || !StringUtils.isNumeric(ret)) {
			return 0L;
		}
		return Long.parseLong(ret);
	}

	public void cancalRebate(StringRedisTemplate template, int frId, long taskId) {
		cancal(template, frId, taskId, SysBalTrans.TASK_TYPE_OUTREBATE);
	}

	public void cancalOutward(StringRedisTemplate template, int frId, long taskId) {
		cancal(template, frId, taskId, SysBalTrans.TASK_TYPE_OUTMEMEBER);
	}

	public List<BizSysLog> suspend(StringRedisTemplate template, AccountBaseInfo info) {
		List<BizSysLog> ret = new ArrayList<>();
		BizAccount acc = accSer.getById(info.getId());
		if (Objects.isNull(acc))
			return ret;
		// 首条记录(status=999)|待确认（status =888）|执行中 （status==777）
		// 首条记录(type=999)
		BizSysLog first = null;
		if (Objects.nonNull(acc.getBankBalance()) && acc.getBankBalance().compareTo(BigDecimal.ZERO) != 0) {
			first = new BizSysLog();
			first.setAccountId(info.getId());
			first.setBankLogId(null);
			first.setAmount(BigDecimal.ZERO);
			first.setFee(BigDecimal.ZERO);
			first.setBalance(SysBalUtils.radix2(acc.getBalance()));
			first.setBankBalance(SysBalUtils.radix2(acc.getBankBalance()));
			first.setOppHandicap(acc.getHandicapId());
			first.setOrderId(null);
			first.setOrderNo(null);
			first.setType(999);// 当前银行余额
			first.setStatus(999);
			first.setCreateTime(realBalLastTime(template, acc.getId()));
			first.setSuccessTime(first.getCreateTime());
			first.setSummary("最新系统|银行余额");
			if (Objects.nonNull(acc.getBalance()) && Objects.nonNull(acc.getBankBalance())) {
				BigDecimal margin = SysBalUtils.radix2(acc.getBankBalance())
						.subtract(SysBalUtils.radix2(acc.getBalance())).setScale(2, BigDecimal.ROUND_HALF_UP);
				String remark = margin.compareTo(BigDecimal.ZERO) > 0 ? ("+" + margin + "(差额)") : (margin + "(差额)");
				first.setRemark(remark);
			}
			ret.add(first);
		}
		// 转出记录
		{
			List<SysBalTrans> d = new ArrayList<>();
			List<SysBalTrans> oriList = template.keys(SysBalTrans.genPatternFrId(info.getId())).stream()
					.map(SysBalTrans::new)
					.filter(p -> !p.ackFr() && SysBalTrans.SYS_REFUND != p.getSys()
							&& SysBalPush.CLASSIFY_BANK_MAN_MGR != p.getRegistWay()
							&& System.currentTimeMillis() - p.getGetTm() <= 420000)
					.collect(Collectors.toList());
			// 转出未上报记录
			List<SysBalTrans> d0 = oriList.stream().filter(p -> p.getAckTm() == 0)
					.sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o2.getGetTm() - o1.getGetTm()))
					.collect(Collectors.toList());
			// 转出已上报且未确认记录
			List<SysBalTrans> d_ = oriList.stream().filter(p -> p.getAckTm() > 0)
					.sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o2.getAckTm() - o1.getAckTm()))
					.collect(Collectors.toList());
			if (!CollectionUtils.isEmpty(d0)) {
				d.addAll(d0);
			}
			if (!CollectionUtils.isEmpty(d_)) {
				d.addAll(d_);
			}
			d.forEach(p -> {
				BizSysLog sys = new BizSysLog();
				sys.setAccountId(p.getFrId());
				sys.setBankLogId(null);
				sys.setAmount(p.getAmt().negate());
				sys.setFee(BigDecimal.ZERO);
				sys.setBalance(null);
				sys.setType(SysLogType.Transfer.getTypeId());
				if (p.getToId() != 0) {
					AccountBaseInfo tobase = accSer.getFromCacheById(p.getToId());
					if (Objects.nonNull(tobase)) {
						sys.setOrderId(null);
						sys.setOrderNo(null);
						sys.setOppId(p.getToId());
						sys.setOppHandicap(tobase.getHandicapId());
						sys.setOppAccount(tobase.getAccount());
						sys.setOppOwner(tobase.getOwner());
					}
				} else if (p.getTaskType() == SysBalTrans.TASK_TYPE_OUTMEMEBER) {
					BizOutwardTask task = outwardTaskDao.findById2(p.getTaskId());
					if (Objects.nonNull(task)) {
						sys.setOrderId(task.getId());
						sys.setOrderNo(task.getOrderNo());
						sys.setOppAccount(task.getToAccount());
						sys.setOppOwner(task.getToAccountOwner());
						BizHandicap oppHandicap = handicapService.findFromCacheByCode(task.getHandicap());
						if (Objects.nonNull(oppHandicap)) {
							sys.setOppHandicap(oppHandicap.getId());
						}
						sys.setType(SysLogType.Outward.getTypeId());
					}
				} else if (p.getTaskType() == SysBalTrans.TASK_TYPE_OUTREBATE) {
					BizAccountRebate task = accountRebateDao.findById2(p.getTaskId());
					if (Objects.nonNull(task)) {
						sys.setOrderId(task.getId());
						sys.setOrderNo(task.getTid());
						sys.setOppAccount(task.getToAccount());
						sys.setOppOwner(task.getToHolder());
						sys.setOppHandicap(task.getHandicap());
						sys.setType(SysLogType.Rebate.getTypeId());
					}
				}
				if (p.ackFr()) {
					sys.setStatus(SysLogStatus.Valid.getStatusId());
					sys.setCreateTime(new Date(p.getGetTm()));
					sys.setBalance(p.getBefore());
					sys.setBankBalance(p.getAfter());
				} else {
					sys.setStatus(p.getAckTm() > 0 ? 888 : 777);
					sys.setCreateTime(new Date(p.getGetTm()));
					sys.setBalance(null);
					sys.setBankBalance(p.getAckTm() > 0 ? p.getAfter() : null);
				}
				ret.add(sys);
			});
		}
		// 转入记录（待确认：888）
		if (!Objects.equals(AccountType.InBank.getTypeId(), info.getType())) {
			template.keys(SysBalTrans.genPatternToId(info.getId())).stream().map(SysBalTrans::new)
					.filter(p -> p.ackFr() && !p.ackTo() && p.getRegistWay() != SysBalTrans.REGIST_WAY_RE)
					.sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o2.getAckTm() - o1.getAckTm())).forEach(p -> {
						BizSysLog log = new BizSysLog();
						log.setAccountId(info.getId());
						log.setBankLogId(null);
						log.setAmount(p.getAmt());
						log.setFee(BigDecimal.ZERO);
						log.setBalance(null);
						log.setBankBalance(null);
						if (p.getFrId() != 0) {
							AccountBaseInfo frbase = accSer.getFromCacheById(p.getFrId());
							log.setOppId(p.getFrId());
							if (Objects.nonNull(frbase)) {
								log.setOppHandicap(frbase.getHandicapId());
								log.setOppAccount(frbase.getAccount());
								log.setOppOwner(frbase.getOwner());
							}
						}
						log.setOrderId(null);
						log.setOrderNo(null);
						log.setType(SysLogType.Transfer.getTypeId());
						log.setStatus(888);// 待确认
						log.setCreateTime(new Date(p.getGetTm()));
						ret.add(log);
					});
		}
		// 云闪付(入款流水判定)
		if (SystemAccountUtils.ysf(acc)) {
			Map<Object, Object> dataMap = template.boundHashOps(RedisKeys.SYS_BAL_YSF).entries();
			if (dataMap.size() > 0) {
				List<ReportYSF> ysfList = new ArrayList<>();
				for (Object p : dataMap.values()) {
					if (Objects.nonNull(p)) {
						ReportYSF ysf = new ReportYSF(p.toString());
						if (Objects.equals(acc.getId(), ysf.getFromAccount()) && Objects.nonNull(ysf.getAmount()))
							ysfList.add(ysf);
					}
				}
				if (!CollectionUtils.isEmpty(ysfList)) {
					Date startTime = new Date(System.currentTimeMillis() - 3600000 * 6);
					List<Long> bankLogIdList = ysfList.stream().map(ReportYSF::getFlogId).collect(Collectors.toList());
					List<BizSysLog> sysList = sysLogService.findByAccountIdAndBankLogIdInAndCreateTimeGreaterThan(
							acc.getId(), bankLogIdList, startTime);
					List<Long> logIdList = sysList.stream().map(BizSysLog::getBankLogId).collect(Collectors.toList());
					ysfList.stream().filter(p -> !logIdList.contains(p.getFlogId())).forEach(p -> {
						BizSysLog item = new BizSysLog();
						item.setAccountId(p.getFromAccount());
						item.setBankLogId(p.getFlogId());
						item.setAmount(p.getAmount());
						item.setFee(BigDecimal.ZERO);
						item.setBalance(SysBalUtils.radix2(acc.getBalance()));
						item.setBankBalance(null);
						item.setOppHandicap(acc.getHandicapId());
						item.setOrderId(p.getTaskId());
						item.setOrderNo(p.getOrderNo());
						item.setType(SysLogType.Income.getTypeId());//
						item.setStatus(666);
						item.setCreateTime(new Date(p.getCrawlTm()));
						item.setSuccessTime(item.getCreateTime());
						item.setSummary(StringUtils.trimToEmpty(p.getSummary()));
						ret.add(item);
					});
				}
			}
		}
		if (Objects.isNull(first) || Objects.isNull(first.getBalance()) || Objects.isNull(first.getBankBalance())) {
			return ret;
		}
		BigDecimal temp = first.getBalance();
		ListIterator<BizSysLog> iterator = ret.listIterator(ret.size());
		while (iterator.hasPrevious()) {
			BizSysLog p = iterator.previous();
			if (Objects.equals(p.getStatus(), 666)) {
				p.setBalance((temp = temp.add(p.getAmount())));
				first.setBalance(first.getBalance().add(p.getAmount()));
			}
			if (Objects.equals(p.getStatus(), 777) || Objects.equals(p.getStatus(), 888)) {
				p.setBalance((temp = temp.add(p.getAmount())));
			}
		}
		return ret;
	}

	protected void regist(StringRedisTemplate template, TransferEntity transferEntity, BigDecimal before) {
		int frId = SysBalUtils.frId(transferEntity), toId = SysBalUtils.toId(transferEntity);
		String toAcc3Last = SysBalUtils.last3letters(transferEntity.getAccount());
		String toOwn2Last = SysBalUtils.last2letters(transferEntity.getOwner());
		String frAcc3Last = StringUtils.EMPTY;
		String frOwn2Last = StringUtils.EMPTY;
		if (frId > 0) {
			AccountBaseInfo base = accSer.getFromCacheById(frId);
			if (Objects.nonNull(base)) {
				frAcc3Last = SysBalUtils.last3letters(base.getAccount());
				frOwn2Last = SysBalUtils.last2letters(base.getOwner());
			}
		}
		BigDecimal amt = SysBalUtils.transAmt(transferEntity);
		long taskId = SysBalUtils.taskId(transferEntity);
		int taskType = SysBalUtils.taskType(transferEntity);
		String ptn = SysBalTrans.genPatternFrIdAndToIdAndAmtAndFrAccAndToAcc(frId, toId, amt, frAcc3Last, toAcc3Last);
		List<SysBalTrans> tsList = template.keys(ptn).stream().map(SysBalTrans::new)
				.filter(p -> p.getTaskId() == taskId && p.getTaskType() == taskType).collect(Collectors.toList());
		if (CollectionUtils.isEmpty(tsList)) {
			String k = SysBalTrans.genMsg(frId, toId, amt, frAcc3Last, toAcc3Last, taskId, taskType, BigDecimal.ZERO,
					before, frOwn2Last, toOwn2Last, SysBalTrans.REGIST_WAY_AUTO);
			template.boundValueOps(k).set(StringUtils.EMPTY, SysBalUtils.expireRegRobot(), TimeUnit.MINUTES);
			transInOut(template, frId, toId, k);
			logger.info("SB{} SB{} [ REGIST NORMAL ] >> f: {} t: {} a: {} {}", frId, toId, frId, toId, amt, k);
		}
	}

	public String reRegist(StringRedisTemplate template, TransferEntity transferEntity) {
		int frId = SysBalUtils.frId(transferEntity), toId = SysBalUtils.toId(transferEntity);
		String toAcc3Last = SysBalUtils.last3letters(transferEntity.getAccount());
		String toOwn2Last = SysBalUtils.last2letters(transferEntity.getOwner());
		String frAcc3Last = StringUtils.EMPTY, frOwn2Last = StringUtils.EMPTY;
		if (frId > 0) {
			AccountBaseInfo base = accSer.getFromCacheById(frId);
			if (Objects.nonNull(base)) {
				frOwn2Last = SysBalUtils.last2letters(base.getOwner());
				frAcc3Last = SysBalUtils.last3letters(base.getAccount());
			}
		}
		BigDecimal amt = SysBalUtils.transAmt(transferEntity);
		long taskId = SysBalUtils.taskId(transferEntity);
		int taskType = SysBalUtils.taskType(transferEntity);
		String k = SysBalTrans.genMsg(SysBalTrans.SYS_NONE, frId, toId, amt, BigDecimal.ZERO, BigDecimal.ZERO,
				frAcc3Last, toAcc3Last, (System.currentTimeMillis() - 60000 * 7), 0, SysBalTrans.ACK_NONE,
				SysBalTrans.ACK_NONE, SysBalTrans.ACK_NONE, SysBalTrans.ACK_NONE, SysBalTrans.ACK_NONE,
				SysBalTrans.ACK_NONE, taskId, taskType, BigDecimal.ZERO, frOwn2Last, toOwn2Last, 0, 0, 0, 0, 0, 0,
				SysBalTrans.REGIST_WAY_RE);
		template.boundValueOps(k).set(StringUtils.EMPTY, SysBalUtils.expireRegRobot(), TimeUnit.MINUTES);
		return k;
	}

	protected void registTh3(StringRedisTemplate template, BizAccount acc3th, BizIncomeRequest order) {
		if (Objects.isNull(acc3th) || Objects.isNull(order) || Objects.isNull(order.getId()))
			return;
		long taskId = order.getId();
		BigDecimal amt = SysBalUtils.radix2(order.getAmount()).abs();
		String frAcc3Last = StringUtils.EMPTY, frOwn2Last = StringUtils.EMPTY;
		String toAcc3Last = StringUtils.EMPTY, toOwn2Last = StringUtils.EMPTY;
		int frId = acc3th.getId(), toId = order.getToId();
		int taskType = SysBalTrans.TASK_TYPE_INNER;
		String k = SysBalTrans.genMsg(frId, toId, amt, frAcc3Last, toAcc3Last, taskId, taskType, BigDecimal.ZERO,
				BigDecimal.ZERO, frOwn2Last, toOwn2Last, SysBalTrans.REGIST_WAY_TH3);
		SysBalTrans ts = new SysBalTrans(k);
		ts.setAckByCurrTrans(SysBalTrans.ACK_ACK);
		ts.setGetTm(System.currentTimeMillis());
		ts.setAckTm(System.currentTimeMillis());
		k = SysBalTrans.genMsg(ts);
		template.boundValueOps(k).set(StringUtils.EMPTY, SysBalUtils.expireRegTh3(), TimeUnit.MINUTES);
		logger.info("SB{} SB{} [ REGIST THIRD_TRANSFER ] >> f: {} t: {} a: {} {}", frId, toId, frId, toId, amt, k);
	}

	public void registMan(StringRedisTemplate template, BizOutwardTask task) {
		if (Objects.isNull(task) || Objects.isNull(task.getAccountId()) || Objects.isNull(task.getOperator())
				|| task.getOperator() == AppConstants.USER_ID_4_ADMIN)
			return;
		BizAccount acc = accDao.findById2(task.getAccountId());
		if (Objects.isNull(acc) || !Objects.equals(AccountType.OutBank.getTypeId(), acc.getType())
				|| Objects.isNull(acc.getHolder()))
			return;
		long taskId = task.getId();
		BigDecimal amt = SysBalUtils.radix2(task.getAmount()).abs();
		String frAcc3Last = SysBalUtils.last3letters(acc.getAccount());
		String frOwn2Last = SysBalUtils.last2letters(acc.getOwner());
		String toAcc3Last = SysBalUtils.last3letters(task.getToAccount());
		String toOwn2Last = SysBalUtils.last2letters(task.getToAccountOwner());
		BigDecimal before = SysBalUtils.radix2(acc.getBankBalance());
		int frId = acc.getId(), toId = 0;
		List<SysBalTrans> dataList = template.keys(SysBalTrans.genPatternFrId(frId)).stream().map(SysBalTrans::new)
				.filter(p -> SysBalTrans.TASK_TYPE_OUTMEMEBER == p.getTaskType() && taskId == p.getTaskId())
				.collect(Collectors.toList());
		if (CollectionUtils.isEmpty(dataList)) {
			int taskType = SysBalTrans.TASK_TYPE_OUTMEMEBER;
			String k = SysBalTrans.genMsg(frId, toId, amt, frAcc3Last, toAcc3Last, taskId, taskType, BigDecimal.ZERO,
					before, frOwn2Last, toOwn2Last, SysBalTrans.REGIST_WAY_MAN);
			template.boundValueOps(k).set(StringUtils.EMPTY, SysBalUtils.expireRegMan(), TimeUnit.MINUTES);
			logger.info("SB{} SB{} [ REGIST MANUAL_OUTWARD ] >> f: {} t: {} a: {} {}", frId, toId, frId, toId, amt, k);
		}
	}

	@Transactional
	public void cancel(StringRedisTemplate template, BizIncomeRequest order) {
		if (Objects.isNull(order) || order.getToId() == null || Objects.isNull(template))
			return;
		List<SysBalTrans> dataList = template.keys(SysBalTrans.genPatternToId(order.getToId())).stream()
				.map(SysBalTrans::new)
				.filter(p -> !p.ackTo() && p.getTaskType() == SysBalTrans.TASK_TYPE_INNER
						&& p.getTaskId() == order.getId())
				.sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o2.getGetTm() - o1.getGetTm()))
				.collect(Collectors.toList());
		if (CollectionUtils.isEmpty(dataList))
			return;
		SysBalTrans tsin = dataList.get(0);
		boolean refundBal = false;
		if (SysBalTrans.SYS_SUB == tsin.getSys()) {
			BigDecimal[] bs = storeHandler.setSysBal(template, order.getToId(), order.getAmount(), null, true);
			storeHandler.refund(tsin, null, bs);
			refundBal = true;
		}
		tsin.setSys(SysBalTrans.SYS_REFUND);
		tsin.setAckTm(System.currentTimeMillis());
		tsin.setAckByOppBal(SysBalTrans.ACK_CANCEL);
		template.delete(tsin.getMsg());
		template.boundHashOps(RedisKeys.SYS_BAL_IN).delete(String.valueOf(order.getToId()));
		String k = SysBalTrans.genMsg(tsin);
		logger.info("SB{} SB{} [ CANCEL ] -> refundBalance: {} msg: {}", tsin.getFrId(), tsin.getToId(), refundBal, k);
	}

	public void confirm(SysBalTrans ts) {
		if (ts == null || ts.getTaskId() == 0)
			return;
		try {
			if (ts.getTaskType() == SysBalTrans.TASK_TYPE_OUTMEMEBER) {
				BizOutwardTask task = outwardTaskDao.findById2(ts.getTaskId());
				if (Objects.isNull(task) || !Objects.equals(task.getAccountId(), ts.getFrId())
						|| Objects.nonNull(task.getOperator()))
					return;
				String remark = CommonUtils.genRemark(task.getRemark(), "排查-转账成功", new Date(), "系统");
				int l = remark.length();
				remark = l > 1000 ? remark.substring(l - 1000) : remark;
				int oriSt = task.getStatus();
				int desSt = OutwardTaskStatus.Deposited.getStatus();
				if (oriSt == OutwardTaskStatus.Deposited.getStatus() || oriSt == OutwardTaskStatus.Matched.getStatus()
						|| oriSt == OutwardTaskStatus.ManageCancel.getStatus()
						|| oriSt == OutwardTaskStatus.ManageRefuse.getStatus()) {
					desSt = oriSt;
				}
				outwardTaskDao.updateStatusAndRemark(task.getId(), oriSt, desSt, remark, task.getScreenshot());
				BizOutwardRequest req = oReqDao.findOne(task.getOutwardRequestId());
				if (Objects.isNull(req) || Objects.equals(req.getStatus(), OutwardRequestStatus.Reject.getStatus())
						|| Objects.equals(req.getStatus(), OutwardRequestStatus.Canceled.getStatus())
						|| Objects.equals(req.getStatus(), OutwardRequestStatus.Acknowledged.getStatus())) {
					return;
				}
				allocOTaskSer.noticePlatIfFinished(AppConstants.USER_ID_4_ADMIN, req);
			}
			if (ts.getTaskType() == SysBalTrans.TASK_TYPE_OUTREBATE) {
				BizAccountRebate task = accountRebateDao.findById2(ts.getTaskId());
				if (Objects.isNull(task) || !Objects.equals(task.getAccountId(), ts.getFrId()))
					return;
				String remark = CommonUtils.genRemark(task.getRemark(), "排查-转账成功", new Date(), "系统");
				int l = remark.length();
				remark = l > 1000 ? remark.substring(l - 1000) : remark;
				SysUser opr = userSer.findFromCacheById(AppConstants.USER_ID_4_ADMIN);
				try {
					reApiSer.finish(task.getId(), remark, opr);
				} catch (Exception e) {
					logger.info(
							"SB{} >> revoke revate platform fail. taskId: {} toAccount: -{} toOwner: -{} amount: {}  e: ",
							ts.getFrId(), ts.getTaskId(), ts.getToAcc3Last(), ts.getToOwn2Last(), ts.getAmt(), e);
				}
			}
		} catch (Exception e) {
			logger.info("SB{} >> taskId: {} toAccount: -{} toOwner: -{} amount: {}  e: ", ts.getFrId(), ts.getTaskId(),
					ts.getToAcc3Last(), ts.getToOwn2Last(), ts.getAmt(), e);
		}
	}

	/**
	 * 工具端信息上报</br>
	 * 转账实体，银行余额，银行流水,银行流水抓取时间
	 */
	protected void rpush(RedisTemplate accountingRedisTemplate, int classify, int target, String msg) {
		StringJoiner joiner = new StringJoiner(SysBalUtils.SEPARATOR);
		joiner.add(String.valueOf(classify));
		joiner.add(String.valueOf(target));
		joiner.add(msg);
		accountingRedisTemplate.boundListOps(RedisTopics.SYS_BAL_RPUSH).rightPush(joiner.toString());
		if (classify == SysBalPush.CLASSIFY_BANK_LOGS || classify == SysBalPush.CLASSIFY_BANK_LOG_)
			logger.info("SB{}  [ RPUSH ] --> : msg: {}", target, msg);
	}

	@Transactional
	public Integer invstRemark(StringRedisTemplate template, Long errId, SysUser operator, String remark) {
		Integer ret = null;
		operator = Objects.requireNonNull(operator, "操作者为空");
		remark = Objects.requireNonNull(remark, "备注不能为空");
		BizSysErr err = Objects.requireNonNull(sysErrDao.findOne(errId), "该记录不存在");
		remark = StringUtils.trimToEmpty(remark) + "-" + operator.getUid();
		err.setRemark(remark);
		BizAccount acc = accSer.getById(err.getTarget());
		if (Objects.isNull(acc) || Objects.isNull(acc.getBalance()) || Objects.isNull(acc.getBankBalance())
				|| SysBalUtils.radix2(acc.getBalance()).compareTo(SysBalUtils.radix2(acc.getBankBalance())) == 0) {
			err.setCollector(operator.getId());
			err.setCollectTime(new Date());
			err.setConsumeTime(10L);
			err.setStatus(SysErrStatus.FinishedNormarl.getStatus());
			template.boundHashOps(RedisKeys.SYS_ACC_RUNNING).delete(String.valueOf(err.getTarget()));
			ret = Objects.isNull(acc) ? null : acc.getId();
		}
		sysErrDao.saveAndFlush(err);
		return ret;
	}

	@Transactional
	public ReportInvstError invstError(StringRedisTemplate template, ErrorHandler errorHandler, Long errId,
			List<AccInvstDoing> doingList, SysUser operator, String remark, SysErrStatus st) throws Exception {
		if (Objects.isNull(st) || !SysErrStatus.finish(st.getStatus()))
			return null;
		Integer accId = null;
		BigDecimal diff = BigDecimal.ZERO;
		List<ActionDeStruct> actionDeStructList = new ArrayList<>();
		for (AccInvstDoing doing : doingList) {
			BizBankLog bank = bankLogDao.findOne(doing.getBankLogId());
			if (Objects.isNull(accId)) {
				BizAccount acc = accSer.getById((accId = bank.getFromAccount()));
				if (Objects.isNull(acc))
					throw new RuntimeException("账号不存在");// 目的在于事务回滚
				diff = SysBalUtils.radix2(acc.getBankBalance()).subtract(SysBalUtils.radix2(acc.getBalance()));
			}
			String ret = errorHandler.handle(new SysAccPush(errId, accId, doing.getInvstType(), bank, operator, remark,
					doing.getOrderNo(), String.valueOf(doing.getType()), st), actionDeStructList);
			if (StringUtils.isNotBlank(ret)) {
				logger.info("INVST{} [ ROLLBACK ] >>  remark:{}  error: {}", accId, remark, ret);
				throw new RuntimeException(ret);// 目的在于事务回滚
			}
		}
		BizAccount acc = accSer.getById(accId);
		BigDecimal sysBal = SysBalUtils.radix2(acc.getBalance()), bankBal = SysBalUtils.radix2(acc.getBankBalance());
		if (Objects.nonNull(acc) && sysBal.compareTo(bankBal) != 0) {
			logger.info("INVST{} [ ROLLBACK ] >>  remark:{}  error: {}", accId, remark, "处理结果，系统余额与银行余额不符");
			throw new RuntimeException("处理结果，系统余额与银行余额不符");// 目的在于事务回滚
		}
		// 更改系统告警记录状态
		BizSysErr err = sysErrDao.findOne(errId);
		err.setConsumeTime((System.currentTimeMillis() - err.getOccurTime().getTime()) / 1000);
		String rem = StringUtils
				.trimToEmpty(CommonUtils.genRemark(err.getRemark(), remark, new Date(), operator.getUid()));
		int l = rem.length();
		rem = l > 500 ? rem.substring(l - 500) : rem;
		err.setRemark(rem);
		err.setStatus(st.getStatus());
		sysErrDao.saveAndFlush(err);
		String errMsg = (String) template.boundHashOps(RedisKeys.SYS_ACC_RUNNING).get(String.valueOf(accId));
		ErrorAlarm alarm = new ErrorAlarm(errMsg);
		if (alarm.getErrorId() == 0 || alarm.getErrorId() == errId) {
			template.boundHashOps(RedisKeys.SYS_ACC_RUNNING).delete(String.valueOf(accId));
		}
		// 更改账号状态
		if (Objects.equals(SysErrStatus.FinishedFreeze.getStatus(), st.getStatus())) {
			acc.setStatus(AccountStatus.Freeze.getStatus());
			accSer.updateBaseInfo(acc);
			// 冻结的时候添加到待处理业务表、如果存在没有处理完的则不添加
			int count = finLessStatSer.findCountsById(acc.getId(), "portion");
			if (count <= 0)
				finLessStatSer.addTrace(acc.getId(), acc.getBankBalance());
		}
		remark = StringUtils.trimToEmpty(remark);
		if (actionDeStructList.size() > 0) {
			ActionDeStruct actionDeStruct = actionDeStructList.get(0);
			if (Objects.nonNull(actionDeStruct)) {
				boolean ret = actionDeStruct.execute();
				if (!ret) {
					logger.info("INVST{} [ ROLLBACK ] >>  remark:{}  error: {}", accId, remark, "远程调用失败");
					throw new RuntimeException("处理结果，远程调用失败");// 目的在于事务回滚
				}
			}
		}
		alloIncomeAccountSer.update(acc.getId(), acc.getType(), acc.getStatus());
		accSer.broadCast(acc);
		return new ReportInvstError(accId, operator.getId(), errId, remark, diff, sysBal, bankBal);
	}

	/**
	 * 转交给其他人处理
	 */
	@Transactional
	public void transErrorToOther(Long errorId, SysUser operator, String otherUser, String remark) throws Exception {
		if (errorId == null)
			throw new Exception("参数不能为空");
		otherUser = StringUtils.trimToNull(otherUser);
		if (Objects.isNull(operator))
			throw new Exception("操作者不能为空");
		if (otherUser == null)
			throw new Exception("转交人不能为空");
		SysUser other = userSer.findByUid(otherUser);
		if (Objects.isNull(other))
			throw new Exception("转交人不存在");
		BizSysErr err = sysErrDao.findOne(errorId);
		if (Objects.isNull(err))
			throw new Exception("该记录不存在");
		if (err.getCollector() == null)
			throw new Exception("该记录未被锁定");
		if (err.getCollector() != null && Objects.equals(err.getCollector(), other.getId())) {
			throw new Exception("该记录已被" + other.getUid() + "锁定");
		}
		if (System.currentTimeMillis() - err.getCollectTime().getTime() <= 3600000
				&& !Objects.equals(err.getCollector(), operator.getId())) {
			throw new Exception("1小时内，只能由锁定人转出");
		}
		if (Objects.equals(UserStatus.DISABLED.getValue(), other.getStatus()))
			throw new Exception("转交人已停用");
		err.setStatus(SysErrStatus.Locked.getStatus());
		err.setCollector(other.getId());
		err.setCollectTime(new Date());
		remark = "(" + operator.getUid() + "转" + other.getUid() + "处理)" + StringUtils.trimToEmpty(remark);
		String rem = StringUtils
				.trimToEmpty(CommonUtils.genRemark(err.getRemark(), remark, new Date(), operator.getUid()));
		int l = rem.length();
		rem = l > 500 ? rem.substring(l - 500) : rem;
		err.setRemark(rem);
		sysErrDao.saveAndFlush(err);
	}

	@Transactional
	public void invalid(AccountBaseInfo base, BizOutwardTask task, String remark, boolean transout) {
		if (Objects.isNull(task)) {
			return;
		}
		if (Objects.nonNull(task.getOperator())) {
			logger.info("SB{} [ ALTER TASK TO INVALID ] >> account: {} owner: {} amount: {} orderId: {} orderNo: {}",
					base.getId(), task.getToAccount(), task.getToAccountOwner(), task.getAmount(), task.getId(),
					task.getOrderNo());
			return;
		}
		if (!Objects.equals(OutwardTaskStatus.Failure.getStatus(), task.getStatus())) {
			oTaskSer.updateStatusById(task.getId(), OutwardTaskStatus.Failure.getStatus());// 先把单修改为待排查的状态
		}
		if (transout) {
			allocOTaskSer.alterStatusToInvalid(task.getId(), null, remark, null, null);// 重新生成任务
		} else {
			logger.info("SB{} [ ALTER TASK TO FAILURE ] >> account: {} owner: {} amount: {} orderId: {} orderNo: {}",
					base.getId(), task.getToAccount(), task.getToAccountOwner(), task.getAmount(), task.getId(),
					task.getOrderNo());
		}
	}

	private void transInOut(StringRedisTemplate template, int frId, int toId, String msg) {
		if (frId != 0)
			template.boundHashOps(RedisKeys.SYS_BAL_OUT).put(String.valueOf(frId), msg);
		if (toId != 0)
			template.boundHashOps(RedisKeys.SYS_BAL_IN).put(String.valueOf(toId), msg);
	}

	private void cancal(StringRedisTemplate template, int frId, long taskId, int taskType) {
		template.keys(SysBalTrans.genPatternFrId(frId)).stream().map(SysBalTrans::new)
				.filter(p -> p.getSys() != SysBalTrans.SYS_REFUND && !p.ackFr() && !p.ackTo() && p.getAckTm() == 0
						&& p.getTaskId() == taskId && p.getTaskType() == taskType)
				.findFirst().ifPresent(p -> template.delete(p.getMsg()));
	}

	private Date realBalLastTime(StringRedisTemplate template, Integer id) {
		if (Objects.isNull(id)) {
			return null;
		}
		String lastTm = (String) template.boundHashOps(RedisKeys.REAL_BAL_LASTTIME).get(String.valueOf(id));
		if (StringUtils.isBlank(lastTm) || !StringUtils.isNumeric(lastTm)) {
			return null;
		}
		return new Date(Long.valueOf(lastTm));
	}
}
