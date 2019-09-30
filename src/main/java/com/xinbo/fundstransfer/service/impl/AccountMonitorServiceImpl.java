package com.xinbo.fundstransfer.service.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.xinbo.fundstransfer.domain.pojo.SysBalPush;
import com.xinbo.fundstransfer.domain.pojo.SysBalTrans;
import com.xinbo.fundstransfer.report.SystemAccountManager;
import com.xinbo.fundstransfer.report.up.ReportParamStreamAlarm;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.common.TimeChangeCommon;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.*;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.TransRec;
import com.xinbo.fundstransfer.domain.repository.BankLogRepository;
import com.xinbo.fundstransfer.domain.repository.OutwardTaskRepository;
import com.xinbo.fundstransfer.service.*;

/**
 * 账号监控
 *
 * @author Eden
 * @since 1.8
 */
@Service
public class AccountMonitorServiceImpl implements AccountMonitorService {
	private static final Logger logger = LoggerFactory.getLogger(AccountMonitorServiceImpl.class);
	@Autowired
	private HandicapService handicapService;
	@Autowired
	private SysUserService userService;
	@Autowired
	private AccountService accService;
	@Autowired
	private BankLogRepository bankLogDao;
	@Autowired
	private OutwardTaskRepository oTaskDao;
	@Autowired
	private AllocateOutwardTaskService allocateOutwardTaskService;
	@Autowired
	private TransactionLogService transactionLogService;
	@Autowired
	private IncomeRequestService incomeRequestService;
	@Autowired
	private AllocateTransferService allocateTransferService;
	@Autowired
	private OutwardRequestService oReqSeir;
	@PersistenceContext
	private EntityManager entityManager;
	@Autowired
	private AllocateTransService transSer;
	@Autowired
	private SystemAccountManager systemAccountManager;

	/**
	 * 标识：操作记录分类</br>
	 * ACTION_REC_OTASK:查询出款记录</br>
	 * ACTION_REC_REQ :查询下发记录</br>
	 */
	private static final int ACTION_REC_OTASK = 1, ACTION_REC_REQ = 0;

	/**
	 * 标识：系统
	 */
	private static final String SYS = "系统";

	/**
	 * 下发账号类型</br>
	 * 用逗号分割
	 */
	private static final String FROM_ACC_TYPES_4_ISSUE = String.format("%d,%d,%d,%d,%d,%d",
			AccountType.InBank.getTypeId(), AccountType.BindAli.getTypeId(), AccountType.BindWechat.getTypeId(),
			AccountType.BindCommon.getTypeId(), AccountType.ThirdCommon.getTypeId(),
			AccountType.ReserveBank.getTypeId());

	/**
	 * 两天所对应的毫秒数
	 */
	private static final long TWO_DAYS_MILIS = 2 * 24 * 60 * 60 * 1000;

	/**
	 * 分页获取未匹配的流水
	 * <p>
	 * 流水包含：转入，转出流水
	 * </p>
	 *
	 * @param handicapId
	 *            盘口ID
	 * @param accType
	 *            账号分类
	 * @param statArr
	 *            流水状态
	 * @param bankType
	 *            银行类型
	 * @param aliasLike
	 *            编号 </br>
	 *            inclusive null
	 * @param amtBtw
	 *            汇款金额[amountMin,amountMax]</br>
	 *            amtBtw,amountMin,amountMax inclusive null
	 * @param timeBtw
	 *            交易时间[transTimeStart,transTimeEnd]</br>
	 *            timeBtw,transTimeStart,transTimeEnd inclusive null </br>
	 *            format:yyyy-MM-dd HH:mm:ss
	 * @param transIn0Out1
	 *            0 入账流水 1 出账流水
	 * @param doing0OrDone1
	 *            0 待处理;1:已处理
	 * @param pageable
	 *            分页信息
	 */
	@Override
	public Page<BizBankLog> findMatchingFlowPage4Acc(Integer handicapId, Integer accType, Integer[] statArr,
			String bankType, String aliasLike, BigDecimal[] amtBtw, String[] timeBtw, Integer transIn0Out1,
			int doing0OrDone1, Pageable pageable) {
		String where = buildWhere4FlowSql(handicapId, accType, statArr, bankType, aliasLike, amtBtw, timeBtw,
				transIn0Out1, doing0OrDone1);
		// 获取：总条数
		String totalSQL = String.format("select count(log.id) %s", where);
		BigInteger total = (BigInteger) entityManager.createNativeQuery(totalSQL).getSingleResult();
		// 获取:数据
		long off = pageable.getOffset();
		int size = pageable.getPageSize();
		String sql4Data = String.format("select log.id %s order by log.create_time desc limit %d,%d", where, off, size);
		@SuppressWarnings("unchecked")
		List<Object> flowIdList = entityManager.createNativeQuery(sql4Data).getResultList();
		List<BizBankLog> dataList;
		if (!CollectionUtils.isEmpty(flowIdList)) {
			dataList = bankLogDao.findAll(flowIdList.stream().mapToLong(p -> Long.valueOf((Integer) p)).mapToObj(p -> p)
					.collect(Collectors.toList()));
		} else {
			dataList = Collections.emptyList();
		}
		// 绑定:附加信息
		dataList.forEach(this::buildExtra);
		dataList = dataList.stream().sorted(Comparator.comparing(BizBankLog::getCreateTime).reversed())
				.collect(Collectors.toList());
		return new PageImpl<>(dataList, pageable, total.intValue());
	}

	/**
	 * 根据流水获取操作记录
	 * <p>
	 * 一周之内
	 * </p>
	 *
	 * @param flowId
	 *            流水ID
	 * @return 操作记录集
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<TransRec> findRecList4Acc(long flowId) {
		BizBankLog bl = bankLogDao.findOne(flowId);
		if (Objects.isNull(bl) || Objects.equals(bl.getStatus(), BankLogStatus.Fee.getStatus())) {
			logger.debug("根据流水获取操作记录 >> flowId：{} status:{}", flowId, bl == null ? "no" : bl.getStatus());
			return Collections.emptyList();
		}
		// 基本信息
		String st = CommonUtils.getDateFormat2Str(new Date(bl.getTradingTime().getTime() - TWO_DAYS_MILIS));
		String ct = CommonUtils.getDateFormat2Str(new Date(bl.getCreateTime().getTime() + TWO_DAYS_MILIS / 16));
		boolean negative = bl.getAmount().compareTo(BigDecimal.ZERO) < 0;
		int fromAccId = bl.getFromAccount();
		BigDecimal amtAbs = bl.getAmount().abs();
		logger.debug("根据流水获取操作记录 >> flowId:{} fromAccId:{} amtAbs:{} ct:{} st:{}", flowId, fromAccId, amtAbs, ct, st);
		AccountBaseInfo base = accService.getFromCacheById(fromAccId);
		// 获取操作记录
		List<TransRec> ret = new ArrayList<>();
		if (negative && Objects.equals(AccountType.OutBank.getTypeId(), base.getType())) {
			String nativeSql = String.format(
					"select task.* from biz_outward_task task left join biz_transaction_log log on task.id=log.order_id and log.type=%d where task.account_id=%d and task.amount=%s and log.from_banklog_id is null and task.asign_time<='%s' and task.asign_time>='%s' order by id desc",
					TransactionLogType.OUTWARD.getType(), fromAccId, amtAbs, ct, st);
			logger.debug("根据流水获取操作记录 出款>>flowId:{} sql:{}", flowId, nativeSql);
			List<BizOutwardTask> dataList = entityManager.createNativeQuery(nativeSql, BizOutwardTask.class)
					.getResultList();
			logger.debug("根据流水获取操作记录 出款>>flowId:{} size of result:{}", flowId, dataList.size());
			dataList.forEach(p -> ret.add(buildTransRec(p)));// 会员出款
		} else {
			if (negative) {// 下发from
				String nativeSql = String.format(
						"select req.* from biz_income_request req left join biz_transaction_log log on req.id=log.order_id and log.type>100 where req.from_id=%d and req.amount=%s and log.from_banklog_id is null and req.create_time<='%s' and req.create_time>='%s' order by id desc",
						fromAccId, amtAbs, ct, st);
				logger.debug("根据流水获取操作记录 下发from>>flowId:%d sql:%s", flowId, nativeSql);
				List<BizIncomeRequest> dataList = entityManager.createNativeQuery(nativeSql, BizIncomeRequest.class)
						.getResultList();
				logger.debug("根据流水获取操作记录 下发from>>flowId:{} size of result:{}", flowId, dataList.size());
				dataList.forEach(p -> ret.add(buildTransRec(p)));
			} else {// 下发 to
				String nativeSql = String.format(
						"select req.* from biz_income_request req left join biz_transaction_log log on req.id=log.order_id and log.type>100 where req.to_id=%d and req.amount=%s and log.to_banklog_id is null and req.create_time<='%s' and req.create_time>='%s' order by id desc",
						fromAccId, amtAbs, ct, st);
				logger.debug("根据流水获取操作记录 下发to>>flowId:{} sql:{}", flowId, nativeSql);
				List<BizIncomeRequest> dataList = entityManager.createNativeQuery(nativeSql, BizIncomeRequest.class)
						.getResultList();
				logger.debug("根据流水获取操作记录 下发to>>flowId:{} size of result:{}", flowId, dataList.size());
				dataList.forEach(p -> ret.add(buildTransRec(p)));
			}
		}
		return ret;
	}

	/**
	 * 根据收款流水，获取汇款流水
	 *
	 * @param toFlowId
	 *            收款流水ID
	 * @return 汇款流水
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<BizBankLog> findFrFlowList4ToFlow(long toFlowId) {
		BizBankLog flow = bankLogDao.findOne(toFlowId);
		logger.debug("根据收款流水,获取汇款流水 >>toFId:{} 存在:{}", toFlowId, Objects.nonNull(flow));
		if (Objects.isNull(flow) || flow.getAmount().compareTo(BigDecimal.ZERO) < 0) {
			logger.debug("根据收款流水,获取汇款流水 >>toFId:{} amt:{}", toFlowId, flow == null ? "No" : flow.getAmount());
			return Collections.emptyList();
		}
		AccountBaseInfo base = accService.getFromCacheById(flow.getFromAccount());
		if (Objects.isNull(base) || !Objects.equals(base.getType(), AccountType.OutBank.getTypeId())
				&& !Objects.equals(base.getType(), AccountType.ReserveBank.getTypeId())) {
			logger.debug("根据收款流水,获取汇款流水 >>toFId:{} frAccType:{}", toFlowId, base == null ? "No" : base.getType());
			return Collections.emptyList();
		}
		BigDecimal amt = flow.getAmount().abs().multiply(new BigDecimal(-1));
		String time = CommonUtils.getDateFormat2Str(new Date(flow.getCreateTime().getTime() - TWO_DAYS_MILIS));
		logger.debug("根据收款流水,获取汇款流水 >>amt:{} time:{} typs:{},toFlowId:{}", amt, time, FROM_ACC_TYPES_4_ISSUE, toFlowId);
		String nativeSql = String.format(
				"select bank.* from biz_bank_log bank,biz_account acc where bank.id!=%d and bank.from_account=acc.id and bank.status=0 and bank.amount=%s and acc.type in (%s) and trading_time>='%s'",
				toFlowId, amt, FROM_ACC_TYPES_4_ISSUE, time);
		logger.debug("根据收款流水,获取汇款流水 >>toFId:{} sql:{}", toFlowId, nativeSql);
		List<BizBankLog> result = entityManager.createNativeQuery(nativeSql, BizBankLog.class).getResultList();
		logger.debug("根据收款流水,获取汇款流水 >> toFId:{} size of result:{}", toFlowId, result.size());
		result.forEach(this::buildExtra);
		return result;
	}

	public List<BizBankLog> findToFlowList4FrFlow(long frFlowId) {
		BizBankLog flow = bankLogDao.findOne(frFlowId);
		logger.debug("根据汇款流水,获取收款流水 >>frFlowId:{} 存在:{}", frFlowId, Objects.nonNull(flow));
		if (Objects.isNull(flow) || flow.getAmount().compareTo(BigDecimal.ZERO) > 0) {
			logger.debug("根据汇款流水,获取收款流水 >>frFlowId:{} amt:{}", frFlowId, flow == null ? "No" : flow.getAmount());
			return Collections.emptyList();
		}
		AccountBaseInfo base = accService.getFromCacheById(flow.getFromAccount());
		if (Objects.isNull(base) || Objects.equals(base.getType(), AccountType.OutBank.getTypeId())) {
			logger.debug("根据汇款流水,获取收款流水 >>frFlowId:{} frAccType:{}", frFlowId, base == null ? "No" : base.getType());
			return Collections.emptyList();
		}
		BigDecimal amt = flow.getAmount().abs();
		String time = CommonUtils.getDateFormat2Str(new Date(flow.getCreateTime().getTime() - TWO_DAYS_MILIS));
		logger.debug("根据汇款流水,获取收款流水 >>amt:{} time:{} typs:{},frFlowId:{}", amt, time, FROM_ACC_TYPES_4_ISSUE, frFlowId);
		String TO_ACC_TYPES_4_ISSUE = String.format("%d,%d", AccountType.OutBank.getTypeId(),
				AccountType.ReserveBank.getTypeId());
		String nativeSql = String.format(
				"select bank.* from biz_bank_log bank,biz_account acc where bank.id!=%d and bank.from_account=acc.id and bank.status=0 and bank.amount=%s and acc.type in (%s) and trading_time>='%s'",
				frFlowId, amt, TO_ACC_TYPES_4_ISSUE, time);
		logger.debug("根据汇款流水,获取收款流水 >>frFlowId:{} sql:{}", frFlowId, nativeSql);
		List<BizBankLog> result = entityManager.createNativeQuery(nativeSql, BizBankLog.class).getResultList();
		logger.debug("根据汇款流水,获取收款流水 >> frFlowId:{} size of result:{}", frFlowId, result.size());
		result.forEach(this::buildExtra);
		return result;
	}

	/**
	 * 组装出款账号转账记录 盘口
	 * 
	 * @param inReq
	 *            下发记录
	 */
	private TransRec buildTransRec(BizIncomeRequest inReq) {
		AccountBaseInfo fromBase = accService.getFromCacheById(inReq.getFromId());
		AccountBaseInfo toBase = accService.getFromCacheById(inReq.getToId());
		SysUser user = userService.findFromCacheById(inReq.getOperator());
		String opr = Objects.isNull(user) ? null : user.getUid();
		TransRec rec = new TransRec(inReq.getId(), ACTION_REC_REQ, null, StringUtils.EMPTY, null,
				inReq.getAmount().abs(), inReq.getOrderNo(), inReq.getRemark(), inReq.getCreateTime(), inReq.getToId(),
				toBase.getBankType(), toBase.getAccount(), toBase.getOwner(), toBase.getAlias(), inReq.getFromId(),
				fromBase.getBankType(), fromBase.getAccount(), fromBase.getOwner(), fromBase.getAlias(), null, opr);
		rec.setFromType(toBase.getType());
		AccountType type = AccountType.findByTypeId(toBase.getType());
		rec.setFromTypeName(Objects.isNull(type) ? StringUtils.EMPTY : type.getMsg());
		Integer l = toBase.getCurrSysLevel();
		l = Objects.isNull(l) ? CurrentSystemLevel.Outter.getValue() : l;
		CurrentSystemLevel cl = CurrentSystemLevel.valueOf(l);
		cl = Objects.isNull(cl) ? CurrentSystemLevel.Outter : cl;
		rec.setFromCurrSysLevel(l);
		rec.setFromCurrSysLevelName(cl.getName());
		return rec;
	}

	/**
	 * 组装出款账号转账记录
	 *
	 * @param oTask
	 *            出款记录
	 */
	private TransRec buildTransRec(BizOutwardTask oTask) {
		Integer handId = null;
		String handCd = null;
		if (StringUtils.isNotBlank(oTask.getHandicap())) {
			BizHandicap hand = handicapService.findFromCacheByCode(oTask.getHandicap().trim());
			if (Objects.nonNull(hand)) {
				handId = hand.getId();
				handCd = hand.getCode();
			}
		}
		AccountBaseInfo base = accService.getFromCacheById(oTask.getAccountId());
		SysUser user = userService.findFromCacheById(oTask.getOperator());
		String opr = Objects.isNull(user) ? null : user.getUid();
		TransRec rec = new TransRec(oTask.getId(), ACTION_REC_OTASK, handId, handCd, oTask.getScreenshot(),
				oTask.getAmount().multiply(new BigDecimal(-1)), oTask.getOrderNo(), oTask.getRemark(),
				oTask.getAsignTime(), base.getId(), base.getBankType(), base.getAccount(), base.getOwner(),
				base.getAlias(), null, null, oTask.getToAccount(), oTask.getToAccountOwner(), null, oTask.getMember(),
				opr);
		rec.setFromType(base.getType());
		AccountType type = AccountType.findByTypeId(base.getType());
		rec.setFromTypeName(Objects.isNull(type) ? StringUtils.EMPTY : type.getMsg());
		Integer l = base.getCurrSysLevel();
		l = Objects.isNull(l) ? CurrentSystemLevel.Outter.getValue() : l;
		CurrentSystemLevel cl = CurrentSystemLevel.valueOf(l);
		cl = Objects.isNull(cl) ? CurrentSystemLevel.Outter : cl;
		rec.setFromCurrSysLevel(l);
		rec.setFromCurrSysLevelName(cl.getName());
		return rec;
	}

	/**
	 * 修改流水为回冲待审
	 *
	 * @param flowId
	 *            银行流水ID
	 * @param operator
	 *            操作人
	 * @param remark
	 *            备注
	 * @throws Exception
	 *             流水不存在 或 已处理
	 */
	@Override
	@Transactional
	public void alterFlowToRefunding(long flowId, SysUser operator, String remark) throws Exception {
		BizBankLog bak = Objects.requireNonNull(bankLogDao.findOne(flowId), "流水不存在");
		if (!Objects.equals(bak.getStatus(), BankLogStatus.Matching.getStatus())) {
			throw new Exception("该流水已处理.");
		}
		Date d = new Date();
		String opr = Objects.isNull(operator) ? SYS : operator.getUid();
		logger.info("账号监控->修改流水为回冲待审 operator:{} flowId:{} amount:{} remark:{}", opr, flowId, bak.getAmount(), remark);
		bak.setStatus(BankLogStatus.Refunding.getStatus());
		remark = remarkAction(BankLogStatus.Refunding, remark);
		bak.setRemark(CommonUtils.genRemark(bak.getRemark(), remark, d, opr));
		bak.setUpdateTime(d);
		bankLogDao.saveAndFlush(bak);
		transSer.buildFlowMatching(bak.getFromAccount());
	}

	/**
	 * 修改流水为费用流水
	 *
	 * @param flowId
	 *            银行流水ID
	 * @param operator
	 *            操作人
	 * @param remark
	 *            备注
	 * @throws Exception
	 *             流水不存在 或 已处理 或 金额超出50元时
	 */
	@Override
	@Transactional
	public void alterFlowToFee(long flowId, SysUser operator, String remark) throws Exception {
		BizBankLog bak = Objects.requireNonNull(bankLogDao.findOne(flowId), "流水不存在");
		// 检测：非空；待匹配
		if (!Objects.equals(BankLogStatus.Matching.getStatus(), bak.getStatus())) {
			throw new Exception("该流水已处理.");
		}
		String opr = Objects.isNull(operator) ? SYS : operator.getUid();
		logger.info("账号监控->修改流水为费用 operator:{} flowId:{} amount:{} remark:{}", opr, flowId, bak.getAmount(), remark);
		// 检测：费用不能超过50元
		if (bak.getAmount().abs().floatValue() >= 350) {
			throw new Exception("费用不能超过350元.");
		}
		Date d = new Date();
		remark = remarkAction(BankLogStatus.Fee, remark);
		bak.setStatus(BankLogStatus.Fee.getStatus());
		bak.setRemark(CommonUtils.genRemark(bak.getRemark(), remark, d, opr));
		bak.setUpdateTime(d);
		bankLogDao.saveAndFlush(bak);
		transSer.buildFlowMatching(bak.getFromAccount());
	}

	/**
	 * 修改流水为已匹配
	 *
	 * @param flowId
	 *            流水ID
	 * @param recId
	 *            操作记录ID
	 * @param operator
	 *            操作人
	 * @param remark
	 *            备注
	 * @throws Exception
	 *             流水不存在；操作者不存在;流水已处理;下发记录不存在;下发提单金额与流水金额不符
	 */
	@Override
	@Transactional
	public void alterFlowToMatched(long flowId, long recId, SysUser operator, String remark) throws Exception {
		BizBankLog bl = Objects.requireNonNull(bankLogDao.findOne(flowId), "流水不存在");
		operator = Objects.requireNonNull(operator, "操作者不存在");
		if (!Objects.equals(bl.getStatus(), BankLogStatus.Matching.getStatus())) {
			throw new Exception("该流水已处理");
		}
		// 基本信息
		Date dNow = new Date();
		// 流水备注信息
		String rem4Flow = "(匹配 flow:" + flowId + ",recId:" + recId + ")" + StringUtils.trimToEmpty(remark);
		rem4Flow = remarkAction(BankLogStatus.Matched, rem4Flow);
		// 为from账号流水添加备注信息
		bl.setUpdateTime(dNow);
		bl.setRemark(CommonUtils.genRemark(bl.getRemark(), rem4Flow, dNow, operator.getUid()));
		bankLogDao.saveAndFlush(bl);
		// 匹配处理
		boolean negative = bl.getAmount().compareTo(BigDecimal.ZERO) < 0;
		AccountBaseInfo base = accService.getFromCacheById(bl.getFromAccount());
		if (negative && Objects.equals(AccountType.OutBank.getTypeId(), base.getType())) {
			// 出款匹配
			allocateOutwardTaskService.remark4Custom(recId, operator, remark);
			BizOutwardTask task = oTaskDao.findById2(recId);
			BizTransactionLog o = transactionLogService.findByOrderIdAndType(task.getId(),
					TransactionLogType.OUTWARD.getType());
			if (Objects.isNull(o)) {
				o = new BizTransactionLog();
				o.setOrderId(task.getId());
				o.setType(TransactionLogType.OUTWARD.getType());
				o.setCreateTime(new Date());
			}
			o.setAmount(bl.getAmount().abs().multiply(new BigDecimal(-1)));
			o.setConfirmor(operator.getId());
			o.setOperator(task.getOperator());
			o.setToAccount(0);
			o.setFromAccount(bl.getFromAccount());
			o.setDifference(BigDecimal.ZERO);
			o.setRemark(CommonUtils.genRemark(o.getRemark(), remark, o.getCreateTime(), operator.getUid()));
			o.setFromBanklogId(flowId);
			transactionLogService.save(o);
			// noticePlatIfFinished
			allocateOutwardTaskService.noticePlatIfFinished(operator.getId(), oReqSeir.get(task.getOutwardRequestId()));
			if (Objects.nonNull(task.getAccountId())) {
				transSer.buildFlowMatching(task.getAccountId());
			}
			List<BizBankLog> logs = new ArrayList<>();
			bl.setTaskId(task.getId());
			bl.setTaskType(SysBalTrans.TASK_TYPE_OUTMEMEBER);
			bl.setOrderNo(task.getOrderNo());
			logs.add(bl);
			systemAccountManager.rpush(new SysBalPush(bl.getFromAccount(), SysBalPush.CLASSIFY_BANK_LOGS, logs));
		} else {
			// 下发匹配
			BizIncomeRequest inReq = Objects.requireNonNull(incomeRequestService.get(recId), "下发记录不存在");
			if (!Objects.equals(inReq.getAmount().abs(), bl.getAmount().abs())) {
				throw new Exception("下发提单金额与流水金额不符");
			}
			BizTransactionLog o = transactionLogService.findByReqId(recId);
			if (Objects.isNull(o)) {
				o = new BizTransactionLog();
				o.setFromAccount(inReq.getFromId());
				o.setToAccount(inReq.getToId());
				o.setOrderId(inReq.getId());
				o.setType(inReq.getType());
				o.setOperator(inReq.getOperator());
				o.setAmount(inReq.getAmount());
				o.setCreateTime(dNow);
			}
			if (negative) {
				o.setFromBanklogId(flowId);
			} else {
				o.setToBanklogId(flowId);
			}
			transactionLogService.save(o);
			if (inReq.getFromId() != null) {
				transSer.buildFlowMatching(inReq.getFromId());
			}
			if (inReq.getToId() != null) {
				transSer.buildFlowMatching(inReq.getToId());
			}
			List<BizBankLog> logs = new ArrayList<>();
			bl.setTaskId(inReq.getId());
			bl.setTaskType(SysBalTrans.TASK_TYPE_INNER);
			bl.setOrderNo(inReq.getOrderNo());
			logs.add(bl);
			systemAccountManager.rpush(new SysBalPush(bl.getFromAccount(), SysBalPush.CLASSIFY_BANK_LOGS, logs));
		}
	}

	/**
	 * 修改流水为利息/结息
	 * <p>
	 * 修改流水状态;</br>
	 * 对应账号: 系统余额=系统余额+额外收入</br>
	 * </p>
	 *
	 * @param flowId
	 *            银行流水ID
	 * @param operator
	 *            操作人
	 * @param remark
	 *            备注
	 * @throws Exception
	 *             流水不存在；该流水已处理；金额为负；金额大于50元
	 */
	@Override
	@Transactional
	public void alterFlowToInterest(long flowId, SysUser operator, String remark) throws Exception {
		BizBankLog bak = Objects.requireNonNull(bankLogDao.findOne(flowId), "流水不存在");
		// 检测：待匹配
		if (!Objects.equals(bak.getStatus(), BankLogStatus.Matching.getStatus())) {
			throw new Exception("该流水已处理");
		}
		String opr = Objects.isNull(operator) ? SYS : operator.getUid();
		logger.info("账号监控->修改流水为利息/结息 operator:{} flowId:{} amount:{} remark:{}", opr, flowId, bak.getAmount(), remark);
		// 检测：利息不能大于50元 且 小于等于0
		if (bak.getAmount().abs().floatValue() > 50 || bak.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
			throw new Exception("结息不能小于零 且金额不能超过50元");
		}
		Date d = new Date();
		remark = remarkAction(BankLogStatus.Interest, remark);
		bak.setStatus(BankLogStatus.Interest.getStatus());
		bak.setRemark(CommonUtils.genRemark(bak.getRemark(), remark, d, opr));
		bak.setUpdateTime(d);
		bak = bankLogDao.saveAndFlush(bak);
		transSer.buildFlowMatching(bak.getFromAccount());
		ReportParamStreamAlarm alarm = new ReportParamStreamAlarm(bak.getFromAccount(), bak.getId(), bak.getAmount(),
				BankLogStatus.Interest.getStatus(), operator.getId(), remark);
		systemAccountManager.rpush(new SysBalPush(bak.getFromAccount(), SysBalPush.CLASSIFY_STREAM_ALARM, alarm));
	}

	/**
	 * 修改流水为亏损
	 *
	 * @param flowId
	 *            银行流水ID
	 * @param reasonCode
	 *            原因编码
	 * @param operator
	 *            操作人
	 * @param remark
	 *            备注
	 * @throws Exception
	 *             流水不存在 或 已处理 或 金额为正
	 */
	@Override
	@Transactional
	public void alterFlowToDeficit(long flowId, int reasonCode, SysUser operator, String remark) throws Exception {
		BizBankLog bak = Objects.requireNonNull(bankLogDao.findOne(flowId), "流水不存在");
		// 检测：待匹配；
		if (!Objects.equals(bak.getStatus(), BankLogStatus.Matching.getStatus())) {
			throw new Exception("该流水已处理");
		}
		String opr = Objects.isNull(operator) ? SYS : operator.getUid();
		logger.info("账号监控->修改流水为亏损 operator:{} flowId:{} amount:{} remark:{}", opr, flowId, bak.getAmount(), remark);
		// 检测：金额大于0
		if (bak.getAmount().compareTo(BigDecimal.ZERO) >= 0) {
			throw new Exception("金额为正");
		}
		if (!Objects.equals(reasonCode, BankLogStatus.DeficitManual.getStatus())
				&& !Objects.equals(reasonCode, BankLogStatus.DeficitSysBug.getStatus())
				&& !Objects.equals(reasonCode, BankLogStatus.DeficitOther.getStatus())) {
			throw new Exception("系统原因不正确");
		}
		Date d = new Date();
		remark = remarkAction(BankLogStatus.findByStatus(reasonCode), remark);
		bak.setStatus(reasonCode);
		bak.setRemark(CommonUtils.genRemark(bak.getRemark(), remark, d, opr));
		bak.setUpdateTime(d);
		bankLogDao.saveAndFlush(bak);
		transSer.buildFlowMatching(bak.getFromAccount());
	}

	/**
	 * 把该流水标记为外部资金
	 *
	 * @param flowId
	 *            汇款流水
	 * @param operator
	 *            操作者
	 * @param remark
	 *            备注
	 * @throws Exception
	 *             流水不存在</br>
	 *             流水已处理</br>
	 *             金额为负</br>
	 */
	@Override
	@Transactional
	public void alterFlowToExtFunds(long flowId, SysUser operator, String remark) throws Exception {
		BizBankLog bak = Objects.requireNonNull(bankLogDao.findOne(flowId), "流水不存在");
		// 检测：待匹配；
		if (!Objects.equals(bak.getStatus(), BankLogStatus.Matching.getStatus())) {
			throw new Exception("该流水已处理");
		}
		String opr = Objects.isNull(operator) ? SYS : operator.getUid();
		logger.info("账号监控->修改流水为亏损 operator:{} flowId:{} amount:{} remark:{}", opr, flowId, bak.getAmount(), remark);
		// 检测：金额大于0
		if (bak.getAmount().compareTo(BigDecimal.ZERO) < 0) {
			throw new Exception("金额必须为正");
		}
		Date d = new Date();
		remark = remarkAction(BankLogStatus.ExtFunds, remark);
		bak.setStatus(BankLogStatus.ExtFunds.getStatus());
		bak.setRemark(CommonUtils.genRemark(bak.getRemark(), remark, d, opr));
		bak.setUpdateTime(d);
		bak = bankLogDao.saveAndFlush(bak);
		transSer.buildFlowMatching(bak.getFromAccount());
		ReportParamStreamAlarm alarm = new ReportParamStreamAlarm(bak.getFromAccount(), bak.getId(), bak.getAmount(),
				BankLogStatus.ExtFunds.getStatus(), operator.getId(), remark);
		systemAccountManager.rpush(new SysBalPush(bak.getFromAccount(), SysBalPush.CLASSIFY_STREAM_ALARM, alarm));
	}

	@Override
	@Transactional
	public void alterFlowToDisposed(long flowId, SysUser operator, String remark) throws Exception {
		BizBankLog bak = Objects.requireNonNull(bankLogDao.findOne(flowId), "流水不存在");
		// 检测：待匹配；
		if (!Objects.equals(bak.getStatus(), BankLogStatus.Matching.getStatus())) {
			throw new Exception("该流水已处理");
		}
		String opr = Objects.isNull(operator) ? SYS : operator.getUid();
		logger.info("账号监控->修改流水为已处理 operator:{} flowId:{} amount:{} remark:{}", opr, flowId, bak.getAmount(), remark);
		// 检测：金额大于0
		Date d = new Date();
		remark = remarkAction(BankLogStatus.Disposed, remark);
		bak.setStatus(BankLogStatus.Disposed.getStatus());
		bak.setRemark(CommonUtils.genRemark(bak.getRemark(), remark, d, opr));
		bak.setUpdateTime(d);
		bak = bankLogDao.saveAndFlush(bak);
		transSer.buildFlowMatching(bak.getFromAccount());
		ReportParamStreamAlarm alarm = new ReportParamStreamAlarm(bak.getFromAccount(), bak.getId(), bak.getAmount(),
				BankLogStatus.Disposed.getStatus(), operator.getId(), remark);
		systemAccountManager.rpush(new SysBalPush(bak.getFromAccount(), SysBalPush.CLASSIFY_STREAM_ALARM, alarm));
	}

	@Override
	@Transactional
	public Integer alterFlowToInvalid(long flowId, SysUser operator) {
		BizBankLog bak = bankLogDao.findOne(flowId);
		if (Objects.isNull(bak)) {
			return null;
		}
		// 检测：待匹配；
		if (!Objects.equals(bak.getStatus(), BankLogStatus.Matching.getStatus())) {
			return null;
		}
		String opr = Objects.isNull(operator) ? SYS : operator.getUid();
		logger.info("账号监控->修改流水为已处理 operator:{} flowId:{} amount:{} remark:{}", opr, flowId, bak.getAmount(), "置无效记录");
		Date d = new Date();
		String remark = remarkAction(BankLogStatus.Disposed, "置无效记录");
		bak.setStatus(BankLogStatus.Disposed.getStatus());
		bak.setRemark(CommonUtils.genRemark(bak.getRemark(), remark, d, opr));
		bak.setUpdateTime(d);
		bankLogDao.saveAndFlush(bak);
		int ret = bak.getFromAccount();
		ReportParamStreamAlarm alarm = new ReportParamStreamAlarm(bak.getFromAccount(), bak.getId(), bak.getAmount(),
				BankLogStatus.Disposed.getStatus(), operator.getId(), "无效流水或重复流水");
		systemAccountManager.rpush(new SysBalPush(bak.getFromAccount(), SysBalPush.CLASSIFY_STREAM_ALARM, alarm));
		return ret;
	}

	/**
	 * 补下发提单
	 *
	 * @param fromFlowId
	 *            汇款流水
	 * @param toFlowId
	 *            收款流水
	 * @param operator
	 *            操作者
	 * @param remark
	 *            备注
	 * @throws Exception
	 *             流水不存在；</br>
	 *             账号不存在；</br>
	 *             汇款账号类型不正确；</br>
	 *             收款账号类型不正确;</br>
	 *             汇款流水金额不能为正;</br>
	 *             收款流水金额不能为负;</br>
	 *             汇款金额与收款金额必须要不相等；</br>
	 *             收款账号与流水不匹配</br>
	 *             汇款流水已处理</br>
	 *             收款流水已处理</br>
	 */
	@Override
	@Transactional
	public void makeUpRec4Issue(long fromFlowId, long toFlowId, SysUser operator, String remark) throws Exception {
		BizBankLog frBak = Objects.requireNonNull(bankLogDao.findOne(fromFlowId), "流水存在");
		AccountBaseInfo frAcc = Objects.requireNonNull(accService.getFromCacheById(frBak.getFromAccount()));
		if (!String.format(",%s,", FROM_ACC_TYPES_4_ISSUE).contains(String.format(",%d,", frAcc.getType()))) {
			throw new Exception("汇款账号类型不正确");
		}
		BizBankLog toBak = Objects.requireNonNull(bankLogDao.findOne(toFlowId), "流水不存在");
		AccountBaseInfo toAcc = Objects.requireNonNull(accService.getFromCacheById(toBak.getFromAccount()));
		if (!Objects.equals(toAcc.getType(), AccountType.OutBank.getTypeId())
				&& !Objects.equals(toAcc.getType(), AccountType.ReserveBank.getTypeId())) {
			throw new Exception("收款账号类型不正确");
		}
		if (frBak.getAmount().compareTo(BigDecimal.ZERO) >= 0 || toBak.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
			throw new Exception("汇款流水金额不能为正;收款流水金额不能为负");
		}
		if (!Objects.equals(frBak.getAmount().abs(), toBak.getAmount().abs())) {
			throw new Exception("汇款金额与收款金额必须要不相等");
		}
		String toAccount = StringUtils.trimToEmpty(frBak.getToAccount());
		String toOwner = StringUtils.trimToEmpty(frBak.getToAccountOwner());
		if (StringUtils.isNotBlank(toAccount) && !toAccount.contains("*")
				&& !Objects.equals(toAccount, StringUtils.trimToEmpty(toAcc.getAccount()))) {
			throw new Exception("收款账号与流水不匹配");
		}
		if (StringUtils.isNotBlank(toOwner) && !toOwner.contains("*")
				&& !Objects.equals(toOwner, StringUtils.trimToEmpty(toAcc.getOwner()))) {
			throw new Exception("收款账号与流水不匹配");
		}
		if (!Objects.equals(frBak.getStatus(), BankLogStatus.Matching.getStatus())) {
			throw new Exception("汇款流水已处理");
		}
		if (!Objects.equals(toBak.getStatus(), BankLogStatus.Matching.getStatus())) {
			throw new Exception("收款流水已处理");
		}
		String opr = Objects.isNull(operator) ? SYS : operator.getUid();
		logger.info("账号监控->补下发提单 opr:{} frFlowId:{} toFlowId:{} frAcc:{} toAcc:{} amt:{} remark:{}", opr, fromFlowId,
				toFlowId, frAcc.getAccount(), toAcc.getAccount(), toBak.getAmount(), remark);
		// 下发提单备注
		String remark4Req = "(补下发提单 frFlow:" + fromFlowId + ",toFlow:" + toFlowId + ")"
				+ StringUtils.trimToEmpty(remark);
		Date dNow = new Date();
		// 补下发提单
		Date d = frBak.getTradingTime();
		BizIncomeRequest o = new BizIncomeRequest();
		o.setFromId(frAcc.getId());
		o.setToId(toAcc.getId());
		o.setHandicap(0);
		o.setLevel(0);
		o.setToAccount(toAcc.getAccount());
		o.setOperator(null);
		o.setAmount(toBak.getAmount());
		o.setCreateTime(d);
		o.setOrderNo(String.valueOf(System.currentTimeMillis()));
		o.setRemark(CommonUtils.genRemark(null, remark4Req, d, opr));
		o.setType(allocateTransferService.transToReqType(frAcc.getType()));
		o.setFromAccount(frAcc.getAccount());
		o.setMemberUserName(StringUtils.EMPTY);
		o.setMemberRealName(toAcc.getOwner());
		o.setStatus(IncomeRequestStatus.Matching.getStatus());
		o.setToAccountBank(toAcc.getBankName());
		BizIncomeRequest inReq = incomeRequestService.save(o, true);
		// 流水备注信息
		String rem4Flow = "(补下发提单 frFlow:" + fromFlowId + ",toFlow:" + toFlowId + ",inReq:" + inReq.getId() + ")"
				+ StringUtils.trimToEmpty(remark);
		// 为from账号流水添加备注信息
		frBak.setUpdateTime(dNow);
		frBak.setRemark(CommonUtils.genRemark(frBak.getRemark(), rem4Flow, dNow, opr));
		bankLogDao.saveAndFlush(frBak);
		// 为to账号流水添加备注信息
		toBak.setUpdateTime(dNow);
		toBak.setRemark(CommonUtils.genRemark(toBak.getRemark(), rem4Flow, dNow, opr));
		bankLogDao.saveAndFlush(toBak);
		// 补交易记录
		BizTransactionLog tx = new BizTransactionLog();
		tx.setFromAccount(inReq.getFromId());
		tx.setToAccount(inReq.getToId());
		tx.setOrderId(inReq.getId());
		tx.setType(inReq.getType());
		tx.setOperator(inReq.getOperator());
		tx.setAmount(inReq.getAmount());
		tx.setCreateTime(new Date());
		tx.setFromBanklogId(frBak.getId());
		tx.setToBanklogId(toBak.getId());
		transactionLogService.save(tx);
		if (Objects.nonNull(inReq.getFromId())) {
			transSer.buildFlowMatching(inReq.getFromId());
		}
		if (Objects.nonNull(inReq.getToId())) {
			transSer.buildFlowMatching(inReq.getToId());
		}
	}

	/**
	 * 银行流水添加备注
	 *
	 * @param flowId
	 *            流水ID
	 * @param operator
	 *            开户人
	 * @param remark
	 *            备注
	 * @throws Exception
	 *             流水不存在</br>
	 *             操着者不存在</br>
	 *             备注不能未空</br>
	 */
	@Override
	@Transactional
	public void remark4Flow(long flowId, SysUser operator, String remark) throws Exception {
		BizBankLog bak = Objects.requireNonNull(bankLogDao.findOne(flowId), "流水不存在");
		operator = Objects.requireNonNull(operator, "请重新登陆");
		remark = Objects.requireNonNull(StringUtils.trimToNull(remark), "备注不能位空");
		logger.info("账号监控->给流水添加备注 flowId:{} opr:{} remark:{}", flowId, operator.getUid(), remark);
		bak.setRemark(CommonUtils.genRemark(bak.getRemark(), remark, new Date(), operator.getUid()));
		// bak.setUpdateTime(new Date()); 银行流水添加备注，不用修改更新时间
		bankLogDao.saveAndFlush(bak);
	}

	@Override
	public Map<String, Object> listBizIncomeRequest(Integer[] status, Integer[] level, Integer accType, String bankType,
			String startTime, String endTime, SysUser sysUser, PageRequest pageRequest, String accRef,
			Integer[] fromType) throws Exception {
		Map<String, Object> result = new HashMap<>();
		String where = buildWhere4IssueSql(status, level, accType, bankType, startTime, endTime, sysUser, accRef,
				fromType);
		// 获取：总条数
		String totalSQL = String.format("select count(req.id) %s", where);
		String sumAmtSQL = String.format("select sum(req.amount) %s", where);
		BigInteger total = (BigInteger) entityManager.createNativeQuery(totalSQL).getSingleResult();
		BigDecimal sumAmt = (BigDecimal) entityManager.createNativeQuery(sumAmtSQL).getSingleResult();
		if (Objects.nonNull(sumAmt)) {
			sumAmt.setScale(2);
		} else {
			sumAmt = new BigDecimal("0.00");
			sumAmt.setScale(2);
		}
		Map<String, Object> res;
		// 获取:数据
		long off = pageRequest.getOffset();
		int size = pageRequest.getPageSize();
		String sql4Data = String.format(
				"select afr.curr_sys_level,req.handicap,req.order_no,afr.type,afr.alias fal,req.from_account,afr.owner fow,afr.bank_type ft,ato.alias tal,req.to_account,ato.owner tow,ato.bank_type tt,req.amount,req.create_time,req.update_time,cap.name,req.remark,req.time_consuming,req.id,req.from_id,req.to_id %s order by req.create_time desc limit %d,%d",
				where, off, size);
		@SuppressWarnings("unchecked")
		List<Object> reqList = entityManager.createNativeQuery(sql4Data).getResultList();
		List<Map<String, Object>> dataList = new ArrayList<>();
		for (int i = 0; i < reqList.size(); i++) {
			res = new HashMap();
			Object[] obj = (Object[]) reqList.get(i);
			res.put("level", obj[0]);
			res.put("handicap", obj[1]);
			res.put("order_no", obj[2]);
			res.put("frtype", obj[3]);
			if (Integer.parseInt(obj[3].toString()) == 2) {
				res.put("frtypename", "第三方");
			} else {
				res.put("frtypename", AccountType.findByTypeId(Integer.parseInt(obj[3].toString())).getMsg());
			}
			res.put("fralias", obj[4]);
			res.put("fraccount", obj[5]);
			res.put("frowner",
					(Objects.equals(obj[6], null)) ? "" : CommonUtils.hideAccountAll(obj[6].toString(), "name"));
			res.put("frbanktype", obj[7]);
			res.put("toalias", obj[8]);
			res.put("toaccount", obj[9]);
			res.put("toowner",
					(Objects.equals(obj[10], null)) ? "" : CommonUtils.hideAccountAll(obj[10].toString(), "name"));
			res.put("tobanktype", obj[11]);
			res.put("amount", obj[12]);
			res.put("createtime", obj[13]);
			res.put("updatetime", obj[14]);
			res.put("handicapName", obj[15]);
			res.put("remark",
					StringUtils.trimToEmpty(obj[16] == null ? "" : obj[16].toString()).replace("\r\n", "<br>"));
			res.put("timeconsumingFmt", obj[17] == null ? StringUtils.EMPTY
					: CommonUtils.convertTime2String(Long.valueOf(obj[17].toString()) * 1000));
			res.put("timeconsuming", obj[17] == null ? 0 : Long.valueOf(obj[17].toString()));
			res.put("id", obj[18]);
			res.put("frId", obj[19]);
			res.put("toId", obj[20]);
			dataList.add(res);
		}
		Map totalinfo = new HashMap();
		totalinfo.put("totalTransCount", total);
		totalinfo.put("totalTransAmount", sumAmt.floatValue());
		result.put("total", totalinfo);
		result.put("page", new PageImpl(dataList, pageRequest, total.intValue()));
		return result;
	}

	/**
	 * 备注添加操作说明
	 * <p>
	 * 格式：</br>
	 * (操作说明)remark
	 * </p>
	 * 
	 * @param status
	 *            状态
	 * @param remark
	 *            备注
	 */
	private String remarkAction(BankLogStatus status, String remark) {
		if (Objects.isNull(status)) {
			return StringUtils.trimToEmpty(remark);
		}
		return "(" + status.getMsg() + ")" + StringUtils.trimToEmpty(remark);
	}

	private String buildWhere4FlowSql(Integer handicapId, Integer accType, Integer[] statArr, String bankType,
			String aliasLike, BigDecimal[] amtBtw, String[] timeBtw, Integer transIn0Out1, Integer doing0OrDone1) {
		StringBuilder result = new StringBuilder();
		result.append(" from biz_bank_log log,biz_account acc where log.from_account=acc.id");
		int inBak = AccountType.InBank.getTypeId();
		doing0OrDone1 = Objects.isNull(doing0OrDone1) ? 0 : doing0OrDone1;
		if (doing0OrDone1 == 0) {
			result.append(" and log.status=").append(BankLogStatus.Matching.getStatus());
		} else {
			result.append(" and log.update_time is not null");
		}
		if (Objects.nonNull(transIn0Out1)) {
			if (Objects.equals(transIn0Out1, 0)) {
				if (Objects.nonNull(accType)) {
					if (Objects.equals(accType, inBak)) {
						result.append(" and false ");
					} else {
						result.append(" and acc.type=").append(accType).append(" and log.amount>0");
					}
				} else {
					result.append(" and acc.type!=").append(inBak).append(" and log.amount>0");
				}
			}
			if (Objects.equals(transIn0Out1, 1)) {
				result.append(" and log.amount<0");
				if (Objects.nonNull(accType)) {
					result.append(" and acc.type=").append(accType);
				}
			}
		} else {
			if (Objects.nonNull(accType)) {
				if (Objects.equals(accType, inBak)) {
					result.append(" and acc.type=").append(inBak).append(" and log.amount<0");
				} else {
					result.append(" and acc.type=").append(accType);
				}
			} else {
				result.append(" and (acc.type=").append(inBak).append(" and log.amount<0").append(" or acc.type!=")
						.append(inBak).append(")");
			}
		}
		if (statArr == null) {
			result.append(" and log.status=").append(BankLogStatus.Matching.getStatus());
		} else if (statArr.length == 1) {
			result.append(" and log.status=").append(statArr[0]);
		} else if (statArr.length > 1) {
			result.append(" and log.status in (").append(BankLogStatus.Refunded.getStatus()).append(",");
			result.append(BankLogStatus.Refunding.getStatus()).append(")");
		}
		if (Objects.nonNull(handicapId)) {
			result.append(" and acc.handicap_id=").append(handicapId);
		}
		if (StringUtils.isNotBlank(bankType)) {
			result.append(" and acc.bank_type='").append(bankType.trim()).append("'");
		}
		if (StringUtils.isNotBlank(aliasLike)) {
			result.append(" and acc.alias ='").append(aliasLike.trim()).append("'");
		}
		if (Objects.nonNull(amtBtw) && amtBtw.length == 2) {
			BigDecimal minusOne = new BigDecimal(-1);
			if (Objects.nonNull(transIn0Out1)) {
				if (Objects.nonNull(amtBtw[0]) && Objects.nonNull(amtBtw[1])) {
					if (Objects.equals(transIn0Out1, 0)) {
						result.append(" and log.amount>=").append(amtBtw[0].abs()).append(" and log.amount<=")
								.append(amtBtw[1].abs());
					} else if (Objects.equals(transIn0Out1, 1)) {
						result.append(" and log.amount>=").append(amtBtw[1].abs().multiply(minusOne))
								.append(" and log.amount<=").append(amtBtw[0].abs().multiply(minusOne));
					}
				} else if (Objects.nonNull(amtBtw[0])) {
					if (Objects.equals(transIn0Out1, 0)) {
						result.append(" and log.amount>=").append(amtBtw[0].abs());
					} else if (Objects.equals(transIn0Out1, 1)) {
						result.append(" and log.amount<=").append(amtBtw[0].abs().multiply(minusOne));
					}
				} else if (Objects.nonNull(amtBtw[1])) {
					if (Objects.equals(transIn0Out1, 0)) {
						result.append(" and log.amount<=").append(amtBtw[1].abs());
					} else if (Objects.equals(transIn0Out1, 1)) {
						result.append(" and log.amount>=").append(amtBtw[1].abs().multiply(minusOne));
					}
				}
			} else {
				if (Objects.nonNull(amtBtw[0]) && Objects.nonNull(amtBtw[1])) {
					result.append(" and (log.amount>=").append(amtBtw[0].abs()).append(" and log.amount<=")
							.append(amtBtw[1].abs());
					result.append(" or log.amount>=").append(amtBtw[1].abs().multiply(minusOne))
							.append(" and log.amount<=").append(amtBtw[0].abs().multiply(minusOne)).append(")");
				} else if (Objects.nonNull(amtBtw[0])) {
					result.append(" and (log.amount>=").append(amtBtw[0].abs()).append(" or log.amount<=")
							.append(amtBtw[0].abs().multiply(minusOne)).append(")");
				} else if (Objects.nonNull(amtBtw[1])) {
					result.append(" and log.amount<=").append(amtBtw[1].abs());
					result.append(" and log.amount>=").append(amtBtw[1].abs().multiply(minusOne));
				}
			}
		}
		if (Objects.nonNull(timeBtw) && timeBtw.length == 2) {
			if (Objects.nonNull(timeBtw[0])) {
				result.append(" and log.create_time>='").append(timeBtw[0]).append("'");
			} else {
				result.append(" and log.create_time>='").append(TimeChangeCommon.getPrevious7days()).append("'");
			}
			if (Objects.nonNull(timeBtw[1])) {
				result.append(" and log.create_time<='").append(timeBtw[1]).append("'");
			}
		} else {
			result.append(" and log.create_time>='").append(CommonUtils.getStartTimeOfCurrDay()).append("'");
		}
		return result.toString();
	}

	private BizBankLog buildExtra(BizBankLog bankLog) {
		if (Objects.nonNull(bankLog)) {
			AccountBaseInfo base = accService.getFromCacheById(bankLog.getFromAccount());
			bankLog.setFromAccountType(base.getType());
			AccountType type = AccountType.findByTypeId(base.getType());
			bankLog.setFromAccountTypeName(Objects.isNull(type) ? StringUtils.EMPTY : type.getMsg());
			bankLog.setFromAccountNO(base.getAccount());
			bankLog.setFromAlias(base.getAlias());
			bankLog.setFromOwner(base.getOwner());
			bankLog.setFromBankType(base.getBankType());
			Integer l = base.getCurrSysLevel();
			l = Objects.isNull(l) ? CurrentSystemLevel.Outter.getValue() : l;
			CurrentSystemLevel cl = CurrentSystemLevel.valueOf(l);
			cl = Objects.isNull(cl) ? CurrentSystemLevel.Outter : cl;
			bankLog.setFromCurrSysLevel(l);
			bankLog.setFromCurrSysLevelName(cl.getName());
			bankLog.setFlag(base.getFlag());
		}
		return bankLog;
	}

	private String buildWhere4IssueSql(Integer[] status, Integer[] level, Integer accType, String bankType,
			String startTime, String endTime, SysUser sysUser, String accRef, Integer[] fromType) {
		StringBuilder result = new StringBuilder();
		Integer category = sysUser.getCategory();
		Integer zone = handicapService.findZoneByHandiId(sysUser.getHandicap());
		result.append(
				" from biz_income_request req,biz_account afr,biz_handicap cap,biz_account ato where req.type in ('103','106','107','110','112') and req.from_id = afr.id and ato.handicap_id = cap.id and req.to_id = ato.id and ato.type != 13 ");
		if (StringUtils.isNotBlank(accRef)) {
			result.append(" and ( ").append("ato.id = ").append(accRef).append(" or ato.alias = ").append(accRef)
					.append(" or ato.account like '%").append(accRef).append("' or afr.id = ").append(accRef)
					.append(" or afr.alias = ").append(accRef).append(" or afr.account like '%").append(accRef)
					.append("')");
		}
		if (StringUtils.isNotBlank(startTime)) {
			result.append(" and req.create_time >='").append(startTime).append("'");
		}
		if (StringUtils.isNotBlank(endTime)) {
			result.append(" and req.create_time <='").append(endTime).append("'");
		}
		if (status.length == 1) {
			result.append(" and req.status=").append(status[0]);
		} else if (status.length > 1) {
			result.append(" and req.status in (").append(IncomeRequestStatus.Matching.getStatus()).append(",");
			result.append(IncomeRequestStatus.Matched.getStatus()).append(",");
			result.append(IncomeRequestStatus.Unmatching.getStatus()).append(")");
		}
		if (fromType != null && fromType.length == 1) {
			result.append(" and afr.type = ").append(fromType[0]);
		} else if (fromType != null && fromType.length > 1) {
			for (int i = 0; i < fromType.length; i++) {
				if (i == 0) {
					result.append(" and afr.type in (").append(fromType[i]).append(",");
				} else if (i == fromType.length - 1) {
					result.append(fromType[i]).append(")");
				} else {
					result.append(fromType[i]).append(",");
				}
			}
		}
		if (level != null && level.length == 1) {
			result.append(" and ato.curr_sys_level=").append(level[0]);
		} else if (level != null && level.length > 1) {
			for (int i = 0; i < level.length; i++) {
				if (i == 0) {
					result.append(" and ato.curr_sys_level in (").append(level[i]).append(",");
				} else if (i == level.length - 1) {
					result.append(level[i]).append(")");
				} else {
					result.append(level[i]).append(",");
				}
			}
		}
		if (Objects.nonNull(accType)) {
			result.append(" and afr.type='").append(accType).append("'");
		}
		if (StringUtils.isNotBlank(bankType)) {
			result.append(" and ato.bank_type='").append(bankType).append("'");
		}
		// 非超级管理员只看自己所在地区的数据
		if (category != UserCategory.ADMIN.getValue()) {
			result.append(" and cap.zone = '").append(zone).append("'");
		}
		return result.toString();
	}
}
