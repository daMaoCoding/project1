package com.xinbo.fundstransfer.report;

import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.*;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalPush;
import com.xinbo.fundstransfer.domain.pojo.SysBalTrans;
import com.xinbo.fundstransfer.domain.pojo.TransferEntity;
import com.xinbo.fundstransfer.domain.repository.AccountRebateRepository;
import com.xinbo.fundstransfer.domain.repository.OutwardTaskRepository;
import com.xinbo.fundstransfer.domain.repository.SysErrRepository;
import com.xinbo.fundstransfer.report.acc.ErrorAlarm;
import com.xinbo.fundstransfer.report.up.ReportYSF;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.HandicapService;
import com.xinbo.fundstransfer.service.SysLogService;
import com.xinbo.fundstransfer.service.SysUserService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class SystemAccountOutterAdapter {
	private static final Logger logger = LoggerFactory.getLogger(SystemAccountCommon.class);
	@Lazy
	@Autowired
	private AccountService accSer;
	@Autowired
	private SysUserService userSer;
	@Autowired
	private SysErrRepository sysErrDao;
	@Autowired
	private OutwardTaskRepository outwardTaskDao;
	@Autowired
	private AccountRebateRepository accountRebateDao;
	@Autowired
	private HandicapService handicapService;
	@Autowired
	private SysLogService sysLogService;

	protected Map<Integer, Balancing> balanceing(StringRedisTemplate template, List<Integer> idList) {
		Map<Integer, Balancing> result = new HashMap<>();
		if (CollectionUtils.isEmpty(idList))
			return result;
		List<Object> ids = idList.stream().filter(Objects::nonNull).map(String::valueOf).collect(Collectors.toList());
		List<Object> vals = template.boundHashOps(RedisKeys.SYS_BAL_BALANCING).multiGet(ids);
		if (CollectionUtils.isEmpty(vals))
			return result;
		vals.forEach(hv -> {
			if (Objects.nonNull(hv)) {
				Balancing balancing = new Balancing(hv.toString());
				result.put(balancing.getTarget(), balancing);
			}
		});
		return result;
	}

	protected Set<Integer> alarm4AccountingInOut(StringRedisTemplate template, boolean isOutward) {
		Set<Integer> result = new HashSet<>();
		if (isOutward) {
			template.boundHashOps(RedisKeys.SYS_BAL_OUT).entries().forEach((k, v) -> {
				Integer id = Integer.valueOf((String) k);
				AccountBaseInfo base = accSer.getFromCacheById(id);
				if (Objects.nonNull(base) && (!Objects.equals(base.getType(), AccountType.InBank.getTypeId())
						&& !Objects.equals(base.getType(), AccountType.ReserveBank.getTypeId())
						&& !(Objects.equals(base.getType(), AccountType.OutBank.getTypeId())
								&& Objects.nonNull(base.getHolder())))) {
					result.add(Integer.valueOf((String) k));
				}
			});
		} else {
			template.boundHashOps(RedisKeys.SYS_BAL_IN).entries().forEach((k, v) -> {
				Integer id = Integer.valueOf((String) k);
				AccountBaseInfo base = accSer.getFromCacheById(id);
				if (Objects.nonNull(base) && (!Objects.equals(base.getType(), AccountType.InBank.getTypeId())
						&& !Objects.equals(base.getType(), AccountType.ReserveBank.getTypeId())
						&& !(Objects.equals(base.getType(), AccountType.OutBank.getTypeId())
								&& Objects.nonNull(base.getHolder())))) {
					result.add(Integer.valueOf((String) k));
				}
			});
		}
		return result;
	}

	protected Set<Integer> accountingSuspend(StringRedisTemplate template) {
		Set<Object> ks = template.boundHashOps(RedisKeys.SYS_ACC_RUUNING_AUTO).keys();
		if (CollectionUtils.isEmpty(ks))
			return Collections.EMPTY_SET;
		return ks.stream().filter(Objects::nonNull).map(String::valueOf).filter(StringUtils::isNumeric)
				.map(Integer::valueOf).collect(Collectors.toSet());
	}

	public Set<Integer> accountingExceedInCountLimit(StringRedisTemplate template) {
		Set<Integer> ids = new HashSet<>();
		Map<Object, Object> kv = template.boundHashOps(RedisKeys.SYS_ACC_RUUNING_AUTO).entries();
		if (CollectionUtils.isEmpty(kv))
			return ids;
		kv.forEach((k, v) -> {
			if (k != null && v != null) {
				AccountSystemSuspendingAlarm alarm = new AccountSystemSuspendingAlarm((String) v);
				if (alarm.isExceedInCount()) {
					if ((k instanceof String) && StringUtils.isNumeric((String) k)) {
						ids.add(Integer.valueOf((String) k));
					}
				}
			}
		});
		return ids;
	}

	protected Set<Integer> accountingException(StringRedisTemplate template, Set<Integer> inclusiveHandicaps) {
		Set<Integer> ids = new HashSet<>();
		if (CollectionUtils.isEmpty(inclusiveHandicaps))
			return ids;
		Map<Object, Object> kv = template.boundHashOps(RedisKeys.SYS_ACC_RUNNING).entries();
		if (CollectionUtils.isEmpty(kv))
			return ids;
		kv.forEach((k, v) -> {
			if (k != null && v != null) {
				ErrorAlarm alarm = new ErrorAlarm((String) v);
				if (alarm.getErrorId() != 0) {
					Integer accId = Integer.valueOf((String) k);
					if (Objects.nonNull(accId) && accId > 0) {
						AccountBaseInfo base = accSer.getFromCacheById(accId);
						if (Objects.nonNull(base) && Objects.nonNull(base.getHandicapId())
								&& inclusiveHandicaps.contains(base.getHandicapId())) {
							ids.add(Integer.valueOf((String) k));
						}
					}
				}
			}
		});
		return ids;
	}

	@Transactional
	protected void transErrorToOther(Long errorId, SysUser operator, String otherUser, String remark) throws Exception {
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

	protected void rpush(RedisTemplate accountingRedisTemplate, int classify, int target, String msg) {
		StringJoiner joiner = new StringJoiner(SysBalUtils.SEPARATOR);
		joiner.add(String.valueOf(classify));
		joiner.add(String.valueOf(target));
		joiner.add(msg);
		accountingRedisTemplate.boundListOps(RedisTopics.SYS_BAL_RPUSH).rightPush(joiner.toString());
		if (classify == SysBalPush.CLASSIFY_BANK_LOGS || classify == SysBalPush.CLASSIFY_BANK_LOG_)
			logger.info("SB{}  [ RPUSH ] --> : msg: {}", target, msg);
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
							&& SysBalTrans.REGIST_WAY_MAN_MGR != p.getRegistWay()
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
			if (!org.apache.shiro.util.CollectionUtils.isEmpty(d0)) {
				d.addAll(d0);
			}
			if (!org.apache.shiro.util.CollectionUtils.isEmpty(d_)) {
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
				if (!org.apache.shiro.util.CollectionUtils.isEmpty(ysfList)) {
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
		if (org.apache.shiro.util.CollectionUtils.isEmpty(tsList)) {
			String k = SysBalTrans.genMsg(frId, toId, amt, frAcc3Last, toAcc3Last, taskId, taskType, BigDecimal.ZERO,
					before, frOwn2Last, toOwn2Last, SysBalTrans.REGIST_WAY_AUTO);
			template.boundValueOps(k).set(StringUtils.EMPTY, SysBalUtils.expireRegRobot(), TimeUnit.MINUTES);
			logger.info("SB{} SB{} [ REGIST NORMAL ] >> f: {} t: {} a: {} {}", frId, toId, frId, toId, amt, k);
		}
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
		BizAccount acc = accSer.getById(task.getAccountId());
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
		if (org.apache.shiro.util.CollectionUtils.isEmpty(dataList)) {
			int taskType = SysBalTrans.TASK_TYPE_OUTMEMEBER;
			String k = SysBalTrans.genMsg(frId, toId, amt, frAcc3Last, toAcc3Last, taskId, taskType, BigDecimal.ZERO,
					before, frOwn2Last, toOwn2Last, SysBalTrans.REGIST_WAY_MAN);
			template.boundValueOps(k).set(StringUtils.EMPTY, SysBalUtils.expireRegMan(), TimeUnit.MINUTES);
			logger.info("SB{} SB{} [ REGIST MANUAL_OUTWARD ] >> f: {} t: {} a: {} {}", frId, toId, frId, toId, amt, k);
		}
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
