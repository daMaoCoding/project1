package com.xinbo.fundstransfer.service.impl;

import com.xinbo.fundstransfer.component.net.http.cloud.HttpClientCloud;
import com.xinbo.fundstransfer.component.net.http.cloud.ReqCoudBodyParser;
import com.xinbo.fundstransfer.component.spring.SpringContextUtils;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.enums.IncomeRequestType;
import com.xinbo.fundstransfer.domain.repository.FinInStatisticsRepository;
import com.xinbo.fundstransfer.service.FinInStatisticsService;
import rx.functions.Action1;
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
public class FinInStatisticsServiceImpl implements FinInStatisticsService {
	@Autowired
	private FinInStatisticsRepository finInStatisticsRepository;
	@PersistenceContext
	private EntityManager entityManager;
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(FinInStatisticsServiceImpl.class);

	@Override
	public Map<String, Object> findFinInStatistics(List<Integer> handicapList, int level, String whereAccount,
			String fristTime, String lastTime, String fieldval, String whereTransactionValue, String type,
			String handicapname, String accountOwner, String bankType, PageRequest pageRequest) throws Exception {
		logger.debug(
				"调用入款明细 参数level：{},whereAccount：{},fristTime：{},lastTime：{},fieldval:{},whereTransactionValue:{},type:{},handicapname:{},pageRequest：{}：",
				level, whereAccount, fristTime, lastTime, fieldval, whereTransactionValue, type, handicapname,
				pageRequest);
		Page<Object> dataToPage;
		java.lang.Object[] total;
		Map<String, Object> map = new HashMap<String, Object>();
		if ("Bankcard".equals(type)) {
			List<Integer> types = new ArrayList<>();
			types.add(AccountType.InBank.getTypeId());
			dataToPage = finInStatisticsRepository.queyFinInStatistics(handicapList, level, whereAccount, fristTime,
					lastTime, fieldval, whereTransactionValue, types, handicapname,
					IncomeRequestType.PlatFromBank.getType(), accountOwner, bankType, pageRequest);
			// 查询总计进行返回
			total = finInStatisticsRepository.totalFinInStatistics(handicapList, level, whereAccount, fristTime,
					lastTime, fieldval, whereTransactionValue, types, handicapname,
					IncomeRequestType.PlatFromBank.getType(), accountOwner, bankType);
			map.put("BankcardPage", dataToPage);
			map.put("Bankcardtotal", total);
		} else if ("SendCard".equals(type)) {
			List<Integer> types = new ArrayList<>();
			types.add(AccountType.ThirdCommon.getTypeId());
			types.add(AccountType.BindCommon.getTypeId());
			dataToPage = finInStatisticsRepository.queyFinInSendCardStatistics(whereAccount, fristTime, lastTime,
					fieldval, whereTransactionValue, types, IncomeRequestType.WithdrawThird.getType(), accountOwner,
					bankType, pageRequest);
			// 查询总计进行返回
			total = finInStatisticsRepository.totalFinInSendCardStatistics(whereAccount, fristTime, lastTime, fieldval,
					whereTransactionValue, types, IncomeRequestType.WithdrawThird.getType(), accountOwner, bankType);
			map.put("sendCardPage", dataToPage);
			map.put("sendCardTotal", total);
		} else if ("WeChat".equals(type)) {
			List<Integer> types = new ArrayList<>();
			types.add(AccountType.InWechat.getTypeId());
			dataToPage = finInStatisticsRepository.queyFinInStatistics(handicapList, level, whereAccount, fristTime,
					lastTime, fieldval, whereTransactionValue, types, handicapname,
					IncomeRequestType.PlatFromWechat.getType(), accountOwner, bankType, pageRequest);
			// 查询总计进行返回
			total = finInStatisticsRepository.totalFinInStatistics(handicapList, level, whereAccount, fristTime,
					lastTime, fieldval, whereTransactionValue, types, handicapname,
					IncomeRequestType.PlatFromWechat.getType(), accountOwner, bankType);
			map.put("WeChatPage", dataToPage);
			map.put("WeChattotal", total);
		} else if ("Paytreasure".equals(type)) {
			List<Integer> types = new ArrayList<>();
			types.add(AccountType.InAli.getTypeId());
			dataToPage = finInStatisticsRepository.queyFinInStatistics(handicapList, level, whereAccount, fristTime,
					lastTime, fieldval, whereTransactionValue, types, handicapname,
					IncomeRequestType.PlatFromAli.getType(), accountOwner, bankType, pageRequest);
			// 查询总计进行返回
			total = finInStatisticsRepository.totalFinInStatistics(handicapList, level, whereAccount, fristTime,
					lastTime, fieldval, whereTransactionValue, types, handicapname,
					IncomeRequestType.PlatFromAli.getType(), accountOwner, bankType);
			map.put("PaytreasurePage", dataToPage);
			map.put("Paytreasuretotal", total);
		} else if ("Thethirdparty".equals(type)) {
			dataToPage = finInStatisticsRepository.queyFinThirdInStatistics(handicapList, level, whereAccount,
					fristTime, lastTime, fieldval, whereTransactionValue, AccountType.InThird.getTypeId(), pageRequest);
			// 查询总计进行返回
			total = finInStatisticsRepository.totalFinThirdInStatistics(handicapList, level, whereAccount, fristTime,
					lastTime, fieldval, whereTransactionValue, AccountType.InThird.getTypeId());
			map.put("thirdpartyPage", dataToPage);
			map.put("thirdpartytotal", total);
		}

		return map;
	}

	@Transactional
	@Override
	public Map<String, Object> findFinInStatisticsFromClearDate(List<Integer> handicapList, int level,
			String whereAccount, String fristTime, String lastTime, String fieldval, String whereTransactionValue,
			String type, String handicapname, String accountOwner, String bankType, PageRequest pageRequest)
			throws Exception {
		logger.debug(
				"调用入款明细 参数 level：{},whereAccount：{},fristTime：{},lastTime：{},fieldval:{},whereTransactionValue:{},type:{},handicapname:{},pageRequest：{}：",
				level, whereAccount, fristTime, lastTime, fieldval, whereTransactionValue, type, handicapname,
				pageRequest);
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
		// if (startTime.equals(fieldval) &&
		// endTime.equals(whereTransactionValue)) {
		// Query query = entityManager.createNativeQuery("{call
		// ClearingDataProc(?)}");
		// query.setParameter(1, df.format(new Date()));
		// query.executeUpdate();
		// }
		Page<Object> dataToPage;
		java.lang.Object[] total;
		Map<String, Object> map = new HashMap<String, Object>();
		if ("Bankcard".equals(type)) {
			List<Integer> types = new ArrayList<>();
			types.add(AccountType.InBank.getTypeId());
			dataToPage = finInStatisticsRepository.queyFinInStatisticsFromClearDate(handicapList, whereAccount,
					fieldval, whereTransactionValue, types, accountOwner, bankType, type, pageRequest);
			// 查询总计进行返回
			total = finInStatisticsRepository.totalFinInStatisticsFromClearDate(handicapList, whereAccount, fieldval,
					whereTransactionValue, types, accountOwner, bankType, type);
			map.put("BankcardPage", dataToPage);
			map.put("Bankcardtotal", total);
		} else if ("SendCard".equals(type)) {
			List<Integer> types = new ArrayList<>();
			types.add(AccountType.ThirdCommon.getTypeId());
			types.add(AccountType.BindCommon.getTypeId());
			types.add(AccountType.BindAli.getTypeId());
			types.add(AccountType.BindWechat.getTypeId());
			dataToPage = finInStatisticsRepository.queyFinInStatisticsFromClearDate(handicapList, whereAccount,
					fieldval, whereTransactionValue, types, accountOwner, bankType, type, pageRequest);
			// 查询总计进行返回
			total = finInStatisticsRepository.totalFinInStatisticsFromClearDate(handicapList, whereAccount, fieldval,
					whereTransactionValue, types, accountOwner, bankType, type);
			map.put("sendCardPage", dataToPage);
			map.put("sendCardTotal", total);
		} else if ("StandbyCard".equals(type)) {
			List<Integer> types = new ArrayList<>();
			types.add(AccountType.ReserveBank.getTypeId());
			dataToPage = finInStatisticsRepository.queyFinInStatisticsFromClearDate(handicapList, whereAccount,
					fieldval, whereTransactionValue, types, accountOwner, bankType, type, pageRequest);
			// 查询总计进行返回
			total = finInStatisticsRepository.totalFinInStatisticsFromClearDate(handicapList, whereAccount, fieldval,
					whereTransactionValue, types, accountOwner, bankType, type);
			map.put("StandbyCardPage", dataToPage);
			map.put("StandbyCardTotal", total);
		} else if ("ClientCard".equals(type)) {
			List<Integer> types = new ArrayList<>();
			types.add(AccountType.BindCustomer.getTypeId());
			dataToPage = finInStatisticsRepository.queyFinInStatisticsFromClearDate(handicapList, whereAccount,
					fieldval, whereTransactionValue, types, accountOwner, bankType, type, pageRequest);
			// 查询总计进行返回
			total = finInStatisticsRepository.totalFinInStatisticsFromClearDate(handicapList, whereAccount, fieldval,
					whereTransactionValue, types, accountOwner, bankType, type);
			map.put("ClientCardPage", dataToPage);
			map.put("ClientCardTotal", total);
		}
		return map;
	}

	@Override
	public Map<String, Object> findFinInStatMatch(String memberrealnamet, String fristTime, String lastTime,
			BigDecimal startamount, BigDecimal endamount, int id, String type, int handicap, PageRequest pageRequest)
			throws Exception {
		logger.debug(
				"调用入款明细》明细 参数 memberrealnamet：{},fristTime：{},lastTime：{},startamount：{},endamount：{},id:{},type:{},pageRequest：{}：",
				memberrealnamet, fristTime, lastTime, startamount, endamount, id, type, pageRequest);
		Page<Object> dataToPage = null;
		java.lang.Object[] total = null;
		Map<String, Object> map = new HashMap<String, Object>();
		if ("bank".equals(type)) {
			dataToPage = finInStatisticsRepository.findFinInStatMatch(memberrealnamet, fristTime, lastTime, startamount,
					endamount, id, IncomeRequestType.PlatFromBank.getType(), pageRequest);
			// 查询总计进行返回
			total = finInStatisticsRepository.totalfindFinInStatMatch(memberrealnamet, fristTime, lastTime, startamount,
					endamount, id, IncomeRequestType.PlatFromBank.getType());
		} else if ("sendcard".equals(type) || "standbyCard".equals(type)) {
			dataToPage = finInStatisticsRepository.findSendCardMatch(memberrealnamet, fristTime,
					lastTime, startamount, endamount, id, "sendcard".equals(type)
							? IncomeRequestType.WithdrawThird.getType() : IncomeRequestType.IssueCompBank.getType(),
					handicap, pageRequest);
			// 查询总计进行返回
			total = finInStatisticsRepository.totalfindSendCardMatch(memberrealnamet, fristTime,
					lastTime, startamount, endamount, id, "sendcard".equals(type)
							? IncomeRequestType.WithdrawThird.getType() : IncomeRequestType.IssueCompBank.getType(),
					handicap);
			map.put("sendCardPage", dataToPage);
			map.put("sendCardtotal", total);
			return map;
		} else if ("weixin".equals(type)) {
			dataToPage = finInStatisticsRepository.findFinInStatMatch(memberrealnamet, fristTime, lastTime, startamount,
					endamount, id, IncomeRequestType.PlatFromWechat.getType(), pageRequest);
			// 查询总计进行返回
			total = finInStatisticsRepository.totalfindFinInStatMatch(memberrealnamet, fristTime, lastTime, startamount,
					endamount, id, IncomeRequestType.PlatFromWechat.getType());
		} else if ("zhifubao".equals(type)) {
			dataToPage = finInStatisticsRepository.findFinInStatMatch(memberrealnamet, fristTime, lastTime, startamount,
					endamount, id, IncomeRequestType.PlatFromAli.getType(), pageRequest);
			// 查询总计进行返回
			total = finInStatisticsRepository.totalfindFinInStatMatch(memberrealnamet, fristTime, lastTime, startamount,
					endamount, id, IncomeRequestType.PlatFromAli.getType());
		} else if ("thirdparty".equals(type)) {
			dataToPage = finInStatisticsRepository.findFinInThirdStatMatch(memberrealnamet, fristTime, lastTime,
					startamount, endamount, id, pageRequest);
			// 查询总计进行返回
			total = finInStatisticsRepository.totalfindFinInThirdStatMatch(memberrealnamet, fristTime, lastTime,
					startamount, endamount, id);
		}

		map.put("Page", dataToPage);
		map.put("total", total);
		return map;
	}

	@Override
	public Map<String, Object> findIncomeThird(int handicap, int level, String accountt, String thirdaccountt,
			String fristTime, String lastTime, String toaccountt, BigDecimal startamount, BigDecimal endamount,
			String type, PageRequest pageRequest) throws Exception {
		Page<Object> dataToPage;
		java.lang.Object[] total;
		Map<String, Object> map = new HashMap<String, Object>();
		if ("thirdin".equals(type)) {
			if (pageRequest.getPageSize() <= 300) {
				List<Object> newlist = new ArrayList<>();
				dataToPage = finInStatisticsRepository.queyIncomeThird(handicap, level, accountt, thirdaccountt,
						fristTime, lastTime, toaccountt, startamount, endamount, pageRequest);
				List<Object> AccountStatisticsList = dataToPage.getContent();
				for (int i = 0; i < AccountStatisticsList.size(); i++) {
					Object[] obj = (Object[]) AccountStatisticsList.get(i);
					// 盘口
					String handicapname = finInStatisticsRepository.queyhandicap((int) obj[0]);
					// 层级
					String levelname = finInStatisticsRepository.queylevel((int) obj[1]);
					// 第三方账号
					String thirdaccount = finInStatisticsRepository.queythirdaccount((int) obj[4]);
					Object[] newobj = { handicapname, levelname, obj[2], obj[3], thirdaccount, obj[5], obj[6], obj[7],
							obj[4] };
					newlist.add(newobj);
				}
				// 查询总计进行返回
				total = finInStatisticsRepository.totalqueyIncomeThird(handicap, level, accountt, thirdaccountt,
						fristTime, lastTime, toaccountt, startamount, endamount);
				map.put("IncomeThird", dataToPage);
				map.put("IncomeThirdtotal", total);
				map.put("data", newlist);
			} else {
				dataToPage = finInStatisticsRepository.queyIncomeThirdd(handicap, level, accountt, thirdaccountt,
						fristTime, lastTime, toaccountt, startamount, endamount, pageRequest);
				// 查询总计进行返回
				total = finInStatisticsRepository.totalqueyIncomeThird(handicap, level, accountt, thirdaccountt,
						fristTime, lastTime, toaccountt, startamount, endamount);
				map.put("IncomeThird", dataToPage);
				map.put("IncomeThirdtotal", total);
				map.put("data", dataToPage.getContent());
			}
		} else if ("Bankcard".equals(type)) {

		}
		return map;
	}

	@Override
	public Map<String, Object> findIncomeByAccount(String member, int toid, String fristTime, String lastTime,
			PageRequest pageRequest) throws Exception {
		Page<Object> dataToPage = finInStatisticsRepository.findIncomeByAccount(member, toid, fristTime, lastTime,
				pageRequest);
		// 查询总计进行返回
		java.lang.Object[] total = finInStatisticsRepository.totalfindIncomeByAccount(member, toid, fristTime,
				lastTime);
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("Page", dataToPage);
		map.put("total", total);
		return map;
	}

	@Override
	public Map<String, Object> findFinInStatMatchBank(String fristTime, String lastTime, BigDecimal startamount,
			BigDecimal endamount, int accountid, int status, String InStatOrTransStat, int typestatus,
			PageRequest pageRequest) throws Exception {
		Page<Object> dataToPage = finInStatisticsRepository.findMatchBankByAccountid(fristTime, lastTime, startamount,
				endamount, accountid, status, typestatus, pageRequest);
		// 查询总计进行返回
		java.lang.Object[] total = finInStatisticsRepository.totalfindMatchBankByAccountid(fristTime, lastTime,
				startamount, endamount, accountid, status, typestatus);
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("Page", dataToPage);
		map.put("total", total);
		return map;
	}

	@Override
	public Object CountReceipts(int pageNo, String handicapCode, String account, String fristTime, String lastTime,
			String type, String pageSize) throws Exception {
		Object[] ret = new Object[1];
		Action1<Object> post = response -> ret[0] = response;
		Action1<Throwable> throwable = e -> logger.error("统计微信支付宝入款数据失败.", e);
		ReqCoudBodyParser req = SpringContextUtils.getBean(ReqCoudBodyParser.class);
		HttpClientCloud.getInstance().getCloudService()
				.countReceipts(
						req.generalCountReceiptsGet(pageNo, handicapCode, account, fristTime, lastTime, type, pageSize))
				.subscribe(post, throwable);
		return ret[0];
	}

	@Override
	public Object sysDetail(int pageNo, String account, String fristTime, String lastTime, BigDecimal startamount,
			BigDecimal endamount, String orderNo, String type, String pageSize) throws Exception {
		Object[] ret = new Object[1];
		Action1<Object> post = response -> ret[0] = response;
		Action1<Throwable> throwable = e -> logger.error("查看微信支付宝入款系统数据失败.", e);
		ReqCoudBodyParser req = SpringContextUtils.getBean(ReqCoudBodyParser.class);
		HttpClientCloud.getInstance().getCloudService().sysDetail(req.generalSysDetail(pageNo, account, fristTime,
				lastTime, startamount, endamount, orderNo, type, pageSize)).subscribe(post, throwable);
		return ret[0];
	}

}
