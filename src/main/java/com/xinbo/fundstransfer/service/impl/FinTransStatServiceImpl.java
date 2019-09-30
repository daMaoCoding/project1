package com.xinbo.fundstransfer.service.impl;

import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.enums.IncomeRequestType;
import com.xinbo.fundstransfer.domain.repository.FinTransStatRepository;
import com.xinbo.fundstransfer.service.FinTransStatService;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinTransStatServiceImpl implements FinTransStatService {
	@Autowired
	private FinTransStatRepository finTransStatRepository;
	@PersistenceContext
	private EntityManager entityManager;
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(FinTransStatServiceImpl.class);

	@Override
	public Map<String, Object> findFinTransStat(String account, String fristTime, String lastTime, String fieldval,
			String whereTransactionValue, String type, String accountOwner, String bankType, int handicap,
			PageRequest pageRequest) throws Exception {
		logger.debug(
				"调用中转明细 参数 whereAccount：{},fristTime：{},lastTime：{},fieldval：{},whereTransactionValue：{},type:{},pageRequest：{}：",
				account, fristTime, lastTime, fieldval, whereTransactionValue, type, pageRequest);
		Page<Object> dataToPage;
		java.lang.Object[] total;
		Map<String, Object> map = new HashMap<String, Object>();
		List<Integer> accountTypes = new ArrayList<>();
		List<Integer> incomeType = new ArrayList<>();
		// 入款银行卡中转
		if ("Bankcard".equals(type)) {
			accountTypes.add(AccountType.InBank.getTypeId());
			incomeType.add(IncomeRequestType.IssueCompBank.getType());
			incomeType.add(IncomeRequestType.IssueComnBank.getType());
			dataToPage = finTransStatRepository.queyfindFinTransStat(account, fristTime, lastTime, accountTypes,
					incomeType, accountOwner, bankType, handicap, pageRequest);
			// 查询总计进行返回
			total = finTransStatRepository.totalfindFinTransStat(account, fristTime, lastTime, accountTypes, incomeType,
					accountOwner, bankType, handicap);
			map.put("BankcardPage", dataToPage);
			map.put("Bankcardtotal", total);
		} else if ("Paytreasure".equals(type)) {// 入款支付宝中转
			accountTypes.clear();
			incomeType.clear();
			accountTypes.add(AccountType.InAli.getTypeId());
			incomeType.add(IncomeRequestType.IssueAli.getType());
			dataToPage = finTransStatRepository.queyfindFinTransStat(account, fristTime, lastTime, accountTypes,
					incomeType, accountOwner, bankType, handicap, pageRequest);
			// 查询总计进行返回
			total = finTransStatRepository.totalfindFinTransStat(account, fristTime, lastTime, accountTypes, incomeType,
					accountOwner, bankType, handicap);
			map.put("PaytreasurePage", dataToPage);
			map.put("Paytreasuretotal", total);
		} else if ("WeChat".equals(type)) {// 入款微信中转
			accountTypes.clear();
			incomeType.clear();
			accountTypes.add(AccountType.InWechat.getTypeId());
			incomeType.add(IncomeRequestType.IssueWechat.getType());
			dataToPage = finTransStatRepository.queyfindFinTransStat(account, fristTime, lastTime, accountTypes,
					incomeType, accountOwner, bankType, handicap, pageRequest);
			// 查询总计进行返回
			total = finTransStatRepository.totalfindFinTransStat(account, fristTime, lastTime, accountTypes, incomeType,
					accountOwner, bankType, handicap);
			map.put("WeChatPage", dataToPage);
			map.put("WeChattotal", total);
		} else if ("Thethirdparty".equals(type)) {// 下发银行卡中转
			accountTypes.clear();
			incomeType.clear();
			accountTypes.add(AccountType.ThirdCommon.getTypeId());
			accountTypes.add(AccountType.BindCommon.getTypeId());
			incomeType.add(IncomeRequestType.IssueComnBank.getType());
			dataToPage = finTransStatRepository.queyfindFinTransStat(account, fristTime, lastTime, accountTypes,
					incomeType, accountOwner, bankType, handicap, pageRequest);
			// 查询总计进行返回
			total = finTransStatRepository.totalfindFinTransStat(account, fristTime, lastTime, accountTypes, incomeType,
					accountOwner, bankType, handicap);
			map.put("thirdpartyPage", dataToPage);
			map.put("thirdpartytotal", total);
		} else if ("third".equals(type)) {// 入款第三方中转
			accountTypes.clear();
			incomeType.clear();
			accountTypes.add(AccountType.InThird.getTypeId());
			incomeType.add(IncomeRequestType.WithdrawThird.getType());
			dataToPage = finTransStatRepository.queyfindthird(account, fristTime, lastTime, accountTypes, incomeType,
					accountOwner, bankType, handicap, pageRequest);
			// 查询总计进行返回
			total = finTransStatRepository.totalfindthird(account, fristTime, lastTime, accountTypes, incomeType,
					accountOwner, bankType, handicap);
			map.put("thirdPage", dataToPage);
			map.put("thirdtotal", total);
		} else if ("Screening".equals(type)) {// 中转隔天排查
			dataToPage = finTransStatRepository.queyScreening(fristTime, lastTime, pageRequest);
			// 查询总计进行返回
			total = finTransStatRepository.totalScreening(fristTime, lastTime);
			map.put("screeningPage", dataToPage);
			map.put("screeningtotal", total);
		} else if ("Thesender".equals(type)) {// 下发卡中转
			// dataToPage =
			// finTransStatRepository.queyfindFinTransStat(whereAccount,
			// fristTime, lastTime, fieldval,
			// whereTransactionValue, 104, pageRequest);
			// // 查询总计进行返回
			// total =
			// finTransStatRepository.totalfindFinTransStat(whereAccount,
			// fristTime, lastTime, fieldval,
			// whereTransactionValue, 104);
			// map.put("ThesenderPage", dataToPage);
			// map.put("Thesendertotal", total);
		}

		return map;
	}

	@Transactional
	@Override
	public Map<String, Object> findFinTransStatFromClearDate(String account, String fristTime, String lastTime,
			String fieldval, String whereTransactionValue, String type, String accountOwner, String bankType,
			List<Integer> handicapList, PageRequest pageRequest) throws Exception {
		logger.debug(
				"调用中转明细 参数 whereAccount：{},fristTime：{},lastTime：{},fieldval：{},whereTransactionValue：{},type:{},pageRequest：{}：",
				account, fristTime, lastTime, fieldval, whereTransactionValue, type, pageRequest);
		// 今天数据
		Date nowTime = new Date(System.currentTimeMillis());
		SimpleDateFormat sdFormatter = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 设置日期格式
		String retStrFormatNowDate = sdFormatter.format(nowTime);
		String startTime = retStrFormatNowDate + " 07:00:00";
		Calendar c = Calendar.getInstance();
		c.setTime(nowTime);
		c.add(Calendar.DAY_OF_MONTH, 1);// 今天+1天
		Date tomorrow = c.getTime();
		String endTime = sdFormatter.format(tomorrow) + " 06:59:59";
		// 如果点击的是今天 则先跑存储。
		// if (startTime.equals(fristTime) && endTime.equals(lastTime)) {
		// Query query = entityManager.createNativeQuery("{call
		// ClearingDataProc(?)}");
		// query.setParameter(1, df.format(new Date()));
		// query.executeUpdate();
		// }
		Page<Object> dataToPage;
		java.lang.Object[] total;
		Map<String, Object> map = new HashMap<String, Object>();
		List<Integer> accountTypes = new ArrayList<>();
		List<Integer> incomeType = new ArrayList<>();
		// 入款银行卡中转
		if ("Bankcard".equals(type)) {
			accountTypes.add(AccountType.InBank.getTypeId());
			incomeType.add(IncomeRequestType.IssueCompBank.getType());
			incomeType.add(IncomeRequestType.IssueComnBank.getType());
			dataToPage = finTransStatRepository.queyfindFinTransStatFromClearDate(account, fristTime, lastTime,
					accountTypes, accountOwner, bankType, handicapList, type, pageRequest);
			// 查询总计进行返回
			total = finTransStatRepository.totalfindFinTransStatFromClearDate(account, fristTime, lastTime,
					accountTypes, accountOwner, bankType, handicapList, type);
			map.put("BankcardPage", dataToPage);
			map.put("Bankcardtotal", total);
		} else if ("Thethirdparty".equals(type)) {// 下发银行卡中转
			accountTypes.clear();
			incomeType.clear();
			accountTypes.add(AccountType.ThirdCommon.getTypeId());
			accountTypes.add(AccountType.BindCommon.getTypeId());
			accountTypes.add(AccountType.BindWechat.getTypeId());
			accountTypes.add(AccountType.BindAli.getTypeId());
			incomeType.add(IncomeRequestType.IssueComnBank.getType());
			incomeType.add(IncomeRequestType.IssueAli.getType());
			incomeType.add(IncomeRequestType.IssueWechat.getType());
			dataToPage = finTransStatRepository.queyfindFinTransStatFromClearDate(account, fristTime, lastTime,
					accountTypes, accountOwner, bankType, handicapList, type, pageRequest);
			// 查询总计进行返回
			total = finTransStatRepository.totalfindFinTransStatFromClearDate(account, fristTime, lastTime,
					accountTypes, accountOwner, bankType, handicapList, type);
			map.put("ThesenderPage", dataToPage);
			map.put("Thesendertotal", total);
		} else if ("standbyCard".equals(type)) {// 备用银行卡中转
			accountTypes.clear();
			incomeType.clear();
			accountTypes.add(AccountType.ReserveBank.getTypeId());
			dataToPage = finTransStatRepository.queyfindFinTransStatFromClearDate(account, fristTime, lastTime,
					accountTypes, accountOwner, bankType, handicapList, type, pageRequest);
			// 查询总计进行返回
			total = finTransStatRepository.totalfindFinTransStatFromClearDate(account, fristTime, lastTime,
					accountTypes, accountOwner, bankType, handicapList, type);
			map.put("StandbyPage", dataToPage);
			map.put("Standbytotal", total);
		}

		return map;
	}

	/**
	 * 中转明细>明细
	 */
	@Override
	public Map<String, Object> finTransStatMatch(String orderno, String fristTime, String lastTime,
			BigDecimal startamount, BigDecimal endamount, int accountid, int type, String serytype, int status,
			int handicap, List<Integer> handicapIds, PageRequest pageRequest) throws Exception {
		logger.debug(
				"调用中转明细》明细  参数 orderno：{},fristTime：{},lastTime：{},startamount：{},endamount：{},accountid:{},type:{},serytype:{},pageRequest：{}：",
				orderno, fristTime, lastTime, startamount, endamount, accountid, type, serytype, pageRequest);
		Page<Object> dataToPage = null;
		java.lang.Object[] total = null;
		if ("bank".equals(serytype)) {
			dataToPage = finTransStatRepository.finTransStatMatchBank(orderno, fristTime, lastTime, startamount,
					endamount, accountid, pageRequest);
			// 查询总计进行返回
			total = finTransStatRepository.totalfinTransStatMatchBank(orderno, fristTime, lastTime, startamount,
					endamount, accountid);
		} else if ("sys".equals(serytype)) {
			List<Integer> types = new ArrayList<>();
			// 下发的数据包括 支付宝下发、微信下发
			if (type == 107) {
				types.add(IncomeRequestType.IssueComnBank.getType());
				types.add(IncomeRequestType.IssueAli.getType());
				types.add(IncomeRequestType.IssueWechat.getType());
			} else {
				types.add(type);
			}
			dataToPage = finTransStatRepository.finTransStatMatchSys(orderno, fristTime, lastTime, startamount,
					endamount, accountid, type, status, handicap, handicapIds, types, pageRequest);
			// 查询总计进行返回
			total = finTransStatRepository.totalfinTransStatMatchSys(orderno, fristTime, lastTime, startamount,
					endamount, accountid, type, status, handicap);
		}
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("Page", dataToPage);
		map.put("total", total);
		return map;
	}

	@Override
	public List<Object[]> finThirdPartyTransit(String fristTime, String lastTime, int accountid, int handicap,
			List<Integer> handicaps) throws Exception {
		return finTransStatRepository.finThirdPartyTransit(fristTime, lastTime, accountid, handicap, handicaps);
	}

	/**
	 * 出入卡清算
	 */
	@Transactional
	@Override
	public Map<String, Object> FinCardLiquidation(String account, String fristTime, String lastTime, String fieldval,
			String whereTransactionValue, String type, String accountOwner, String bankType, List<Integer> handicapList,
			String cartype, String status, PageRequest pageRequest) throws Exception {
		logger.debug(
				"调用出入卡清算 参数 whereAccount：{},fristTime：{},lastTime：{},fieldval：{},whereTransactionValue：{},type:{},pageRequest：{}：",
				account, fristTime, lastTime, fieldval, whereTransactionValue, type, pageRequest);
		// 今天数据
		Date nowTime = new Date(System.currentTimeMillis());
		SimpleDateFormat sdFormatter = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 设置日期格式
		String retStrFormatNowDate = sdFormatter.format(nowTime);
		String startTime = retStrFormatNowDate + " 07:00:00";
		Calendar c = Calendar.getInstance();
		c.setTime(nowTime);
		c.add(Calendar.DAY_OF_MONTH, 1);// 今天+1天
		Date tomorrow = c.getTime();
		String endTime = sdFormatter.format(tomorrow) + " 06:59:59";
		// 如果点击的是今天 则先跑存储。
		// if (startTime.equals(fristTime) && endTime.equals(lastTime)) {
		// Query query = entityManager.createNativeQuery("{call
		// ClearingDataProc(?)}");
		// query.setParameter(1, df.format(new Date()));
		// query.executeUpdate();
		// }
		Page<Object> dataToPage;
		java.lang.Object[] total;
		java.lang.Object[] minusDate;
		Map<String, Object> map = new HashMap<String, Object>();
		List<Integer> types = new ArrayList<>();
		if ("income".equals(cartype)) {
			types.add(AccountType.InBank.getTypeId());
		} else if ("outward".equals(cartype)) {
			types.add(AccountType.OutBank.getTypeId());
		} else if ("issue".equals(cartype)) {
			types.add(AccountType.ThirdCommon.getTypeId());
			types.add(AccountType.BindCommon.getTypeId());
		} else if ("other".equals(cartype)) {
			types.add(AccountType.ReserveBank.getTypeId());
			types.add(AccountType.CashBank.getTypeId());
			types.add(AccountType.BindWechat.getTypeId());
			types.add(AccountType.BindAli.getTypeId());
		} else {
			types.add(AccountType.InBank.getTypeId());
			types.add(AccountType.OutBank.getTypeId());
			types.add(AccountType.ThirdCommon.getTypeId());
			types.add(AccountType.BindCommon.getTypeId());
			types.add(AccountType.ReserveBank.getTypeId());
			types.add(AccountType.CashBank.getTypeId());
			types.add(AccountType.BindWechat.getTypeId());
			types.add(AccountType.BindAli.getTypeId());
		}
		int statuss = "".equals(status) ? 0 : Integer.parseInt(status);
		// 查询今天冻结卡的id
		List<Integer> frozenCardId = finTransStatRepository.findFrozenCardId(fristTime, lastTime);
		// 入款银行卡中转
		dataToPage = finTransStatRepository.queyFinCardLiquidation(account, fristTime, lastTime, accountOwner, bankType,
				handicapList, types, statuss, frozenCardId, pageRequest);
		// 查询总计进行返回
		total = finTransStatRepository.totalFinCardLiquidation(account, fristTime, lastTime, accountOwner, bankType,
				handicapList, types, statuss, frozenCardId);
		// 备用卡入款数据存在双倍，查询数据除以一半
		minusDate = finTransStatRepository.minusDate(account, fristTime, lastTime, accountOwner, bankType, handicapList,
				types, statuss);
		map.put("CardLiquidationPage", dataToPage);
		map.put("CardLiquidationtotal", total);
		map.put("minusDate", minusDate);
		return map;
	}

}
