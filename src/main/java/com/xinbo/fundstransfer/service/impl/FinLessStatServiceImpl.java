package com.xinbo.fundstransfer.service.impl;

import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizAccountMore;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import com.xinbo.fundstransfer.domain.enums.AccountTraceStatus;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.repository.AccountRebateRepository;
import com.xinbo.fundstransfer.domain.repository.FinLessStatisticsRepository;
import com.xinbo.fundstransfer.service.AccountMoreService;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.CabanaService;
import com.xinbo.fundstransfer.service.FinLessStatService;
import com.xinbo.fundstransfer.service.HostMonitorService;
import com.xinbo.fundstransfer.service.RebateApiService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinLessStatServiceImpl implements FinLessStatService {
	@Autowired
	private FinLessStatisticsRepository finLessStatisticsRepository;
	@Autowired
	@Lazy
	private AccountService accountService;
	@Autowired
	private AccountMoreService accountMoreSer;
	@Autowired
	private HostMonitorService hostMonitorService;
	@Autowired
	private CabanaService cabanaService;
	@Autowired
	private RebateApiService rebateApiService;
	@Autowired
	private AccountRebateRepository accRebateDao;
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(FinInStatisticsServiceImpl.class);

	@Override
	public Map<String, Object> findFinInLessStatis(Map<String, Object> parmap) throws Exception {
		logger.debug("调用入款亏损 参数 Map,pageRequest：{}：", parmap);
		Page<Object> dataToPage;
		java.lang.Object[] total;
		Map<String, Object> map = new HashMap<String, Object>();
		if (parmap.get("type").equals("Bankcard")) {// 入款亏损
			// dataToPage =
			// finInStatisticsRepository.queyFinInStatistics(handicap,level,whereAccount,fristTime,lastTime,fieldval,whereTransactionValue,1,handicapname,pageRequest);
			// 查询总计进行返回
			// total =
			// finInStatisticsRepository.totalFinInStatistics(handicap,level,whereAccount,fristTime,lastTime,fieldval,whereTransactionValue,1,handicapname);
			// map.put("BankcardPage", dataToPage);
			// map.put("Bankcardtotal", total);
		} else if (parmap.get("type").equals("Bankcard")) {// 中转亏损
			// dataToPage =
			// finInStatisticsRepository.queyFinInStatistics(handicap,level,whereAccount,fristTime,lastTime,fieldval,whereTransactionValue,4,handicapname,pageRequest);
			// 查询总计进行返回
			// total =
			// finInStatisticsRepository.totalFinInStatistics(handicap,level,whereAccount,fristTime,lastTime,fieldval,whereTransactionValue,4,handicapname);
			// map.put("WeChatPage", dataToPage);
			// map.put("WeChattotal", total);
		} else if (parmap.get("type").equals("Paytreasure")) {// 出款亏损
			// dataToPage =
			// finInStatisticsRepository.queyFinInStatistics(handicap,level,whereAccount,fristTime,lastTime,fieldval,whereTransactionValue,3,handicapname,pageRequest);
			// 查询总计进行返回
			// total =
			// finInStatisticsRepository.totalFinInStatistics(handicap,level,whereAccount,fristTime,lastTime,fieldval,whereTransactionValue,3,handicapname);
			// map.put("PaytreasurePage", dataToPage);
			// map.put("Paytreasuretotal", total);
		} else if (parmap.get("type").equals("Thethirdparty")) {// 盘口亏损
			// dataToPage =
			// finInStatisticsRepository.queyFinInStatistics(handicap,level,whereAccount,fristTime,lastTime,fieldval,whereTransactionValue,2,handicapname,pageRequest);
			// 查询总计进行返回
			// total =
			// finInStatisticsRepository.totalFinInStatistics(handicap,level,whereAccount,fristTime,lastTime,fieldval,whereTransactionValue,2,handicapname);
			// map.put("thirdpartyPage", dataToPage);
			// map.put("thirdpartytotal", total);
		} else if ("frostless".equals(parmap.get("type"))) {
			List<Integer> handicap = (List<Integer>) parmap.get("handicap");
			// int level=(int) parmap.get("level");
			String account = (String) parmap.get("whereAccount");
			String fristTime = (String) parmap.get("fristTime");
			String lastTime = (String) parmap.get("lastTime");
			String fieldval = (String) parmap.get("fieldval");
			String whereTransactionValue = (String) parmap.get("whereTransactionValue");
			String cartype = (String) parmap.get("cartype");
			PageRequest pageRequest = (PageRequest) parmap.get("pageRequest");
			List<Integer> types = new ArrayList<>();
			if ("income".equals(cartype)) {
				types.add(AccountType.InBank.getTypeId());
			} else if ("outward".equals(cartype)) {
				types.add(AccountType.OutBank.getTypeId());
			} else if ("issue".equals(cartype)) {
				types.add(AccountType.ThirdCommon.getTypeId());
				types.add(AccountType.BindCommon.getTypeId());
			} else if ("reserve".equals(cartype)) {
				types.add(AccountType.ReserveBank.getTypeId());
			} else {
				types.add(AccountType.CashBank.getTypeId());
				types.add(AccountType.BindWechat.getTypeId());
				types.add(AccountType.BindAli.getTypeId());
			}
			dataToPage = finLessStatisticsRepository.queyFinFrostlessStatistics(handicap, account, fristTime, lastTime,
					fieldval, whereTransactionValue, types, cartype, pageRequest);
			// 查询总计进行返回
			total = finLessStatisticsRepository.totalFinFrostlessStatistics(handicap, account, fristTime, lastTime,
					fieldval, whereTransactionValue, types, cartype);
			map.put("frostPage", dataToPage);
			map.put("frosttotal", total);
		}

		return map;
	}

	@Override
	public Map<String, Object> findFinPending(Map<String, Object> parmap) throws Exception {
		logger.debug("调用入款亏损 参数 Map,pageRequest：{}：", parmap);
		Page<Object> dataToPage;
		java.lang.Object[] total;
		Map<String, Object> map = new HashMap<String, Object>();
		if ("frostless".equals(parmap.get("type"))) {
			List<Integer> handicap = (List<Integer>) parmap.get("handicap");
			// int level=(int) parmap.get("level");
			String account = (String) parmap.get("whereAccount");
			String alias = (String) parmap.get("alias");
			String fristTime = (String) parmap.get("fristTime");
			String lastTime = (String) parmap.get("lastTime");
			String fieldval = (String) parmap.get("fieldval");
			String whereTransactionValue = (String) parmap.get("whereTransactionValue");
			String cartype = (String) parmap.get("cartype");
			String status = (String) parmap.get("status");
			String statusType = (String) parmap.get("statusType");
			String jdType = (String) parmap.get("jdType");
			String classification = (String) parmap.get("classification");
			PageRequest pageRequest = (PageRequest) parmap.get("pageRequest");
			List<Integer> types = new ArrayList<>();
			List<Integer> freezeCardStatus = new ArrayList<>();
			if (status.equals("6")) {
				freezeCardStatus.add(AccountTraceStatus.CashflowDispose.getStatusId());
				freezeCardStatus.add(AccountTraceStatus.HandicapDispose.getStatusId());
				freezeCardStatus.add(AccountTraceStatus.FinanceDispose.getStatusId());
				freezeCardStatus.add(AccountTraceStatus.ContinueToFreeze.getStatusId());
				freezeCardStatus.add(AccountTraceStatus.ThawRecovery.getStatusId());
				freezeCardStatus.add(AccountTraceStatus.shiftDelete.getStatusId());
			} else if (status.equals("3")) {
				freezeCardStatus.add(AccountTraceStatus.ContinueToFreeze.getStatusId());
				freezeCardStatus.add(AccountTraceStatus.ThawRecovery.getStatusId());
				freezeCardStatus.add(AccountTraceStatus.shiftDelete.getStatusId());
			} else {
				freezeCardStatus.add(Integer.parseInt(status));
			}
			if ("income".equals(cartype)) {
				types.add(AccountType.InBank.getTypeId());
			} else if ("outward".equals(cartype)) {
				types.add(AccountType.OutBank.getTypeId());
			} else if ("issue".equals(cartype)) {
				types.add(AccountType.ThirdCommon.getTypeId());
				types.add(AccountType.BindCommon.getTypeId());
			} else if ("reserve".equals(cartype)) {
				types.add(AccountType.ReserveBank.getTypeId());
			} else if ("third".equals(cartype)) {
				types.add(AccountType.InThird.getTypeId());
			} else {
				types.add(AccountType.CashBank.getTypeId());
				types.add(AccountType.BindWechat.getTypeId());
				types.add(AccountType.BindAli.getTypeId());
				types.add(AccountType.BindCustomer.getTypeId());
			}
			alias = alias.equals("") ? null : alias;
			dataToPage = finLessStatisticsRepository.queyFinFrostlessPending(handicap, account, fristTime, lastTime,
					fieldval, whereTransactionValue, types, cartype, freezeCardStatus, statusType, jdType, alias,
					classification, pageRequest);
			// 查询总计进行返回
			total = finLessStatisticsRepository.totalFinFrostlessPending(handicap, account, fristTime, lastTime,
					fieldval, whereTransactionValue, types, cartype, freezeCardStatus, statusType, jdType, alias,
					classification);
			map.put("frostPage", dataToPage);
			map.put("frosttotal", total);
		}

		return map;
	}

	@Transactional
	@Override
	public void freezingProcess(Long id, Long traceId, String uid, String remark, String oldRemark, String type,
			BigDecimal jAmount, String derating) throws Exception {
		BigDecimal dAmount = finLessStatisticsRepository.findBal(traceId);
		String event = "";
		String status = "";
		if (type.equals("MoneyHandling")) {
			event = "资金处理：";
		} else if (type.equals("PermanentlyFrozen")) {
			event = "永久删除";
			status = "5";
			finLessStatisticsRepository.UpdateAccountStatusByid(id, AccountStatus.Delete.getStatus());
		} else if (type.equals("RecoveryUSES")) {
			event = "解冻恢复使用";
			status = "4";
			finLessStatisticsRepository.UpdateAccountStatusByid(id, AccountStatus.StopTemp.getStatus());
		} else if (type.equals("freeze")) {
			event = "持续冻结";
			status = "3";
			finLessStatisticsRepository.UpdateAccountStatusByid(id, AccountStatus.Freeze.getStatus());
		}
		BizAccount account = accountService.getById(Integer.parseInt(id.toString()));
		// 如果是返利网的卡 解冻的时候需要把账号余额调整下，如果只有一张卡
		// 则需要把more表的信用额度清理，lineLimit清理，如果有多张卡则需要把lineLimit加上去
		// 如果解冻的金额 低于银行的余额 则把剩下的钱进行降额处理
		if (null != account && null != account.getFlag() && account.getFlag() == 2) {
			BizAccountMore mo = accountMoreSer.findByMobile(account.getMobile());
			BigDecimal mg = (null == mo.getMargin() ? BigDecimal.ZERO : mo.getMargin());
			String[] accId = mo.getAccounts().substring(1).split(",");
			boolean flag = false;
			String remarks = "";
			BigDecimal balance = dAmount.subtract(jAmount);
			if ("y".equals(derating)) {
				remarks += "当前银行余额：" + account.getBankBalance() + "冻结时余额：" + dAmount + ",解冻金额：" + jAmount + "是否降低额度"
						+ derating + ";当前额度：" + mg + ",解冻后额度：" + mg.subtract(balance).subtract(jAmount);
			} else {
				remarks += "当前银行余额：" + account.getBankBalance() + "冻结时余额：" + dAmount + ",解冻金额：" + jAmount + "是否降低额度"
						+ derating + ";当前额度：" + mg + ",解冻后额度：" + mg.subtract((dAmount.subtract(jAmount)));
			}
			BigDecimal margin = mg.subtract(balance);
			margin = margin.compareTo(BigDecimal.ZERO) == 1 ? margin : BigDecimal.ZERO;
			BigDecimal tmpPeek = Objects.isNull(account.getPeakBalance()) ? BigDecimal.ZERO
					: new BigDecimal(account.getPeakBalance());
			if (accId.length == 1 && mo != null) {
				// 查询是否有待审核的降额单
				BigDecimal deratingAmount = accRebateDao.deratingAmounts(mo.getUid());
				deratingAmount = null == deratingAmount ? BigDecimal.ZERO : deratingAmount;
				// 向返利网降额请 求
				if ("y".equals(derating)) {
					flag = rebateApiService.derate(account.getAccount(), -(balance.add(jAmount).floatValue()),
							margin.subtract(jAmount).subtract(deratingAmount).floatValue(), null, null);
				} else {
					flag = rebateApiService.derate(account.getAccount(), -(balance.floatValue()), margin.floatValue(),
							null, null);
				}
				if (flag) {
					// 真实余额大于解冻金额则进行相减 如果不大于则置为0.1 因为存在当时冻结的时候是1500 后来机器上报为56
					// 导致银行余额为负数
					if (account.getBankBalance().compareTo(jAmount) > -1) {
						account.setBankBalance(account.getBankBalance().subtract(jAmount));
					} else {
						account.setBankBalance(new BigDecimal("0.1"));
					}
					if ("y".equals(derating)) {
						account.setPeakBalance(
								(int) (tmpPeek.floatValue() - jAmount.floatValue() - balance.floatValue()));
					} else {
						account.setPeakBalance((int) (tmpPeek.floatValue() - jAmount.floatValue()));
					}
					accountService.broadCast(account);
					hostMonitorService.update(account);
					cabanaService.updAcc(account.getId());
					if ("y".equals(derating)) {
						mo.setMargin(margin.subtract(jAmount));
						mo.setLinelimit((null == mo.getLinelimit() ? BigDecimal.ZERO : mo.getLinelimit())
								.subtract(jAmount).subtract(balance));
					} else {
						mo.setMargin(margin);
						mo.setLinelimit(
								(null == mo.getLinelimit() ? BigDecimal.ZERO : mo.getLinelimit()).subtract(jAmount));
					}
					if (mo.getLinelimit().compareTo(BigDecimal.ZERO) == 0)
						mo.setLinelimit(BigDecimal.ZERO);
					mo.setRemark(CommonUtils.genRemark(mo.getRemark(), remarks, new Date(), "sys"));
					accountMoreSer.saveAndFlash(mo);
				}
			} else if (accId.length > 1) {
				// 查询是否有待审核的降额单
				BigDecimal deratingAmount = accRebateDao.deratingAmounts(mo.getUid());
				deratingAmount = null == deratingAmount ? BigDecimal.ZERO : deratingAmount;
				if ("y".equals(derating)) {
					// 向返利网降额请求
					flag = rebateApiService.derate(account.getAccount(), -(balance.add(jAmount).floatValue()),
							(null == mo.getMargin() ? BigDecimal.ZERO : mo.getMargin()).subtract(balance)
									.subtract(jAmount).subtract(deratingAmount).floatValue(),
							null, null);
				} else {
					flag = rebateApiService.derate(account.getAccount(), -balance.floatValue(),
							margin.subtract(deratingAmount).floatValue(), null, null);
				}
				if (flag) {
					if ("y".equals(derating)) {
						account.setPeakBalance(
								(int) (tmpPeek.floatValue() - balance.floatValue() - jAmount.floatValue()));
					} else {
						account.setPeakBalance((int) (tmpPeek.floatValue() - balance.floatValue()));
					}
					// 真实余额大于解冻金额则进行相减 如果不大于则置为0.1 因为存在当时冻结的时候是1500 后来机器上报为56
					// 导致银行余额为负数
					if (account.getBankBalance().compareTo(jAmount) > -1) {
						account.setBankBalance(account.getBankBalance().subtract(jAmount));
					} else {
						account.setBankBalance(new BigDecimal("0.1"));
					}
					accountService.broadCast(account);
					hostMonitorService.update(account);
					cabanaService.updAcc(account.getId());
					if ("y".equals(derating)) {
						mo.setMargin((null == mo.getMargin() ? BigDecimal.ZERO : mo.getMargin()).subtract(balance)
								.subtract(jAmount));
					} else {
						mo.setMargin((null == mo.getMargin() ? BigDecimal.ZERO : mo.getMargin()).subtract(balance));
					}
					if (mo.getMargin().compareTo(BigDecimal.ZERO) == 0)
						mo.setMargin(BigDecimal.ZERO);
					if ("y".equals(derating)) {
						mo.setLinelimit((null == mo.getLinelimit() ? BigDecimal.ZERO : mo.getLinelimit())
								.subtract(balance).subtract(jAmount));
					} else {
						mo.setLinelimit((null == mo.getLinelimit() ? BigDecimal.ZERO : mo.getLinelimit()).add(jAmount));
					}
					mo.setRemark(CommonUtils.genRemark(mo.getRemark(), remarks, new Date(), "sys"));
					accountMoreSer.saveAndFlash(mo);
				}
			}
		} else {
			if (account.getBankBalance().compareTo(jAmount) > -1) {
				account.setBankBalance(account.getBankBalance().subtract(jAmount));
			} else {
				account.setBankBalance(new BigDecimal("0.1"));
			}
		}
		accountService.broadCast(account);
		String remarks = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())) + " " + uid + "\r\n" + event
				+ remark;
		finLessStatisticsRepository.accomplish(
				CommonUtils.genRemark(oldRemark, event + ":" + remark, new Date(), ("(账号处理)" + uid)), traceId, status);
		finLessStatisticsRepository.saveAccountExtra(id, remarks, uid);
	}

	@Override
	public Map<String, Object> findHistory(int accountId, PageRequest pageRequest) throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		Page<Object> dataToPage = finLessStatisticsRepository.findHistory(accountId, pageRequest);
		map.put("HistoryPage", dataToPage);
		return map;
	}

	@Override
	public int findCountsById(int accountId, String type) throws Exception {
		return finLessStatisticsRepository.findCountsById(accountId, type);
	}

	@Override
	public String findCarCountsById(int accountId) throws Exception {
		return finLessStatisticsRepository.findCarCountsById(accountId);
	}

	@Override
	public String findThirdCountsById(String account) throws Exception {
		return finLessStatisticsRepository.findThirdCountsById(account);
	}

	@Override
	public String findOldRemark(Long id) throws Exception {
		return finLessStatisticsRepository.findOldRemark(id);
	}

	@Transactional
	@Override
	public void jieDongMoney(Integer uid, String remark, Long id, BigDecimal amount, String type) throws Exception {
		finLessStatisticsRepository.jieDongMoney(uid, remark, id, amount, type);
	}

	@Transactional
	@Override
	public void accomplish(String remark, Long id, String status) {
		finLessStatisticsRepository.accomplishAmount(remark, id, status);
	}

	@Transactional
	@Override
	public void addTrace(Integer id, BigDecimal bankBalance) throws Exception {
		finLessStatisticsRepository.addTrace(id, bankBalance);
	}

	@Transactional
	@Override
	public void updateAccountStatus(Long id, SysUser user) throws Exception {
		String remarks = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())) + " "
				+ user.getUid().toString() + "\r\n" + "转停用";
		finLessStatisticsRepository.saveAccountExtra(id, remarks, user.getUid());
		finLessStatisticsRepository.UpdateAccountStatusByid(id, AccountStatus.StopTemp.getStatus());
		BizAccount acc = accountService.getById(Integer.parseInt(id.toString()));
		accountService.broadCast(acc);
	}

	@Override
	public String findStatus(Long id) throws Exception {
		return finLessStatisticsRepository.findStatus(id);
	}

	@Transactional
	@Override
	public void cashflow(String remark, Long id) throws Exception {
		finLessStatisticsRepository.cashflow(remark, id);
	}

	@Transactional
	@Override
	public boolean derating(Integer accountId, BigDecimal derateAmount, String uid, String remark) throws Exception {
		try {
			BizAccount account = accountService.getById(accountId);
			if (null != account && account.getFlag() == 2) {
				boolean flag = false;
				BizAccountMore mo = accountMoreSer.findByMobile(account.getMobile());
				// 向返利网请求降额 如果是一千以下的 则用peakBalance当信用额度 去减
				if (Objects.nonNull(mo.getMargin()) && mo.getMargin().compareTo(new BigDecimal(1000)) <= 0) {
					BigDecimal init = BigDecimal.ZERO;
					for (String accId : StringUtils.trimToEmpty(mo.getAccounts()).split(",")) {
						if (StringUtils.isBlank(accId) || !StringUtils.isNumeric(accId))
							continue;
						BizAccount acc = accountService.getById(Integer.valueOf(accId));
						if (Objects.nonNull(acc))
							init = init.add(new BigDecimal(acc.getPeakBalance()));
					}
					flag = rebateApiService.derate(account.getAccount(), -derateAmount.floatValue(),
							(init.subtract(derateAmount)).floatValue(), null, null);
				} else {
					flag = rebateApiService.derate(account.getAccount(), -derateAmount.floatValue(),
							(mo.getMargin().subtract(derateAmount)).floatValue(), null, null);
				}
				if (flag) {
					if (Objects.nonNull(mo.getMargin()) && mo.getMargin().compareTo(new BigDecimal(1000)) <= 0) {
						BigDecimal init = BigDecimal.ZERO;
						for (String accId : StringUtils.trimToEmpty(mo.getAccounts()).split(",")) {
							if (StringUtils.isBlank(accId) || !StringUtils.isNumeric(accId))
								continue;
							BizAccount acc = accountService.getById(Integer.valueOf(accId));
							if (Objects.nonNull(acc))
								init = init.add(new BigDecimal(acc.getPeakBalance()));
						}
						mo.setMargin(init.subtract(derateAmount));
					} else {
						mo.setMargin(mo.getMargin().subtract(derateAmount));
					}
					// 如果一张卡 不够扣 则需要扣除多张卡
					Integer peakBalance = (int) (account.getPeakBalance().floatValue() - derateAmount.floatValue());
					// 如果当前的卡 够扣除
					if (account.getPeakBalance().floatValue() >= Math.abs(derateAmount.floatValue())) {
						account.setPeakBalance(peakBalance);
						accountService.broadCast(account);
						hostMonitorService.update(account);
						cabanaService.updAcc(account.getId());
					} else {
						// 当前卡不够扣
						float reduceAmount = Math.abs(derateAmount.floatValue()) - account.getPeakBalance();
						account.setPeakBalance(0);
						accountService.broadCast(account);
						hostMonitorService.update(account);
						cabanaService.updAcc(account.getId());
						for (String accId : StringUtils.trimToEmpty(mo.getAccounts()).split(",")) {
							if (StringUtils.isBlank(accId) || !StringUtils.isNumeric(accId)
									|| accId.equals(account.getId().toString()))
								continue;
							BizAccount accout = accountService.getById(Integer.valueOf(accId));
							if (Objects.nonNull(accout)) {
								if (reduceAmount <= 0)
									break;
								if (accout.getPeakBalance() >= reduceAmount) {
									// 当前卡够扣
									Integer pb = (int) (accout.getPeakBalance() - reduceAmount);
									accout.setPeakBalance(pb);
									accountService.broadCast(accout);
									hostMonitorService.update(accout);
									cabanaService.updAcc(accout.getId());
								} else {
									// 当前卡不够扣
									reduceAmount = reduceAmount - accout.getPeakBalance();
									accout.setPeakBalance(0);
									accountService.broadCast(accout);
									hostMonitorService.update(accout);
									cabanaService.updAcc(accout.getId());
								}
							}
						}
					}
					String remarks = "降低信用额度:" + mo.getMargin() + ">"
							+ (mo.getMargin().subtract(derateAmount.setScale(2, RoundingMode.HALF_UP))).setScale(2,
									RoundingMode.HALF_UP)
							+ ";备注：" + remark;
					mo.setRemark(CommonUtils.genRemark(mo.getRemark(), remarks, new Date(), uid));
					accountMoreSer.saveAndFlash(mo);
				} else {
					logger.info("返利网降额失败：" + account.getAccount());
					return false;
				}
			} else {
				logger.info("不是返利网账号：" + account.getAccount());
				return false;
			}
		} catch (Exception e) {
			logger.error("降额失败！", e);
			return false;
		}
		return true;
	}

}
