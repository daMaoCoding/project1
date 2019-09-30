package com.xinbo.fundstransfer.service.impl;

import com.xinbo.fundstransfer.domain.repository.AccountStatisticsRepository;
import com.xinbo.fundstransfer.service.AccountStatisticsService;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

@Service
public class AccountStatisticsServiceImpl implements AccountStatisticsService {
	static final Logger log = LoggerFactory.getLogger(AccountStatisticsServiceImpl.class);
	@Autowired
	private AccountStatisticsRepository accountStatisticsRepository;
	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public Map<String, Object> findAccountStatistics(String bankStartTime, String bankEndTime, String Account,
			String sysTemFristTime, String sysTemEndTime, String accountOwner, String bankType, int handicap,
			PageRequest pageRequest) throws Exception {
		/*
		 * // 查询统计系统出款记录 Page<Object> sysTemPage =
		 * accountStatisticsRepository.queyAccountSystem(Account,
		 * sysTemFristTime, sysTemEndTime, accountOwner, bankType, pageRequest);
		 * // 查询总计进行返回 java.lang.Object[] stsTemTotal =
		 * accountStatisticsRepository.totalAccountSystem(Account,
		 * sysTemFristTime, sysTemEndTime, accountOwner, bankType); // 查询统计
		 * 银行卡出款记录 Page<Object> bankPage =
		 * accountStatisticsRepository.queyAccountBank(Account, sysTemFristTime,
		 * sysTemEndTime, accountOwner, bankType, pageRequest); // 查询总计进行返回
		 * java.lang.Object[] bankTotal =
		 * accountStatisticsRepository.totalAccountBank(Account,
		 * sysTemFristTime, sysTemEndTime, accountOwner, bankType); //
		 * 把两个结果集拼在一起 List<Object> newlist = new ArrayList<Object>();
		 * List<Object> sysTemList = sysTemPage.getContent(); List<Object>
		 * bankList = bankPage.getContent(); for (int i = 0; i <
		 * sysTemList.size(); i++) { Object[] newobj = new Object[10]; Object[]
		 * objSys = (Object[]) sysTemList.get(i); if (i > (bankList.size() - 1))
		 * { newobj[0] = objSys[0]; newobj[1] = objSys[1]; newobj[2] =
		 * objSys[2]; newobj[3] = objSys[3]; newobj[4] = objSys[4]; newobj[5] =
		 * objSys[5]; newobj[6] = objSys[6]; newobj[7] = 0; newobj[8] = 0;
		 * newobj[9] = 0; } else { Object[] objBank = (Object[])
		 * bankList.get(i); newobj[0] = objSys[0]; newobj[1] = objSys[1];
		 * newobj[2] = objSys[2]; newobj[3] = objSys[3]; newobj[4] = objSys[4];
		 * newobj[5] = objSys[5]; newobj[6] = objSys[6]; newobj[7] = objBank[3];
		 * newobj[8] = objBank[4]; newobj[9] = objBank[5]; }
		 * newlist.add(newobj); }
		 * 
		 * Map<String, Object> map = new HashMap<>(); map.put("Page",
		 * sysTemPage); map.put("list", newlist); Object[] objTotal = (Object[])
		 * bankTotal[0]; map.put("total", stsTemTotal[0] + "," + objTotal[0] +
		 * "," + objTotal[1]);
		 */
		Page<Object> dataToPage = accountStatisticsRepository.queyAccountStatistics(Account, sysTemFristTime,
				sysTemEndTime, accountOwner, bankType, handicap, pageRequest);
		// 查询总计进行返回
		java.lang.Object[] total = accountStatisticsRepository.totalAccountStatistics(Account, sysTemFristTime,
				sysTemEndTime, accountOwner, bankType, handicap);
		Map<String, Object> map = new HashMap<>();
		map.put("Page", dataToPage);
		map.put("total", total);
		log.debug(
				"调用出款明细Service whereBankValue：{},whereTransactionValue：{},whereAccount：{},fristTime：{},lastTime：{},pageRequest：{}：",
				bankStartTime, bankEndTime, Account, sysTemFristTime, sysTemEndTime, pageRequest);
		log.debug("返回的dataToPage{}，total{}", dataToPage.getContent(), total);
		return map;
	}

	@Transactional
	@Override
	public Map<String, Object> AccountStatisticsFromClearDate(String bankStartTime, String bankEndTime, String Account,
			String sysTemFristTime, String sysTemEndTime, String accountOwner, String bankType,
			List<Integer> handicapList, String cartype, PageRequest pageRequest) throws Exception {
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
		// if (startTime.equals(sysTemFristTime) &&
		// endTime.equals(sysTemEndTime)) {
		// Query query = entityManager.createNativeQuery("{call
		// ClearingDataProc(?)}");
		// query.setParameter(1, df.format(new Date()));
		// query.executeUpdate();
		// }
		Page<Object> dataToPage = accountStatisticsRepository.queyAccountStatisticsFromClearDate(Account,
				sysTemFristTime, sysTemEndTime, accountOwner, bankType, handicapList, cartype, pageRequest);
		// 查询总计进行返回
		java.lang.Object[] total = accountStatisticsRepository.totalAccountStatisticsFromClearDate(Account,
				sysTemFristTime, sysTemEndTime, accountOwner, bankType, handicapList, cartype);
		Map<String, Object> map = new HashMap<>();
		map.put("Page", dataToPage);
		map.put("total", total);
		log.debug(
				"调用出款明细Service whereBankValue：{},whereTransactionValue：{},whereAccount：{},fristTime：{},lastTime：{},pageRequest：{}：",
				bankStartTime, bankEndTime, Account, sysTemFristTime, sysTemEndTime, pageRequest);
		log.debug("返回的dataToPage{}，total{}", dataToPage.getContent(), total);
		return map;
	}

	@Override
	public Map<String, Object> findFinOutStatSys(int accountid, String accountname, String ptfristTime,
			String ptlastTime, BigDecimal startamount, BigDecimal endamount, int restatus, int tastatus,
			PageRequest pageRequest) throws Exception {
		log.debug("调用出款明细>查询系统明细 参数 accountid：{},accountname：{},ptfristTime：{},ptlastTime：{},pageRequest：{}：",
				accountid, accountname, ptfristTime, ptlastTime, pageRequest);
		Page<Object> dataToPage = accountStatisticsRepository.queyfindFinOutStatSys(accountid, accountname, ptfristTime,
				ptlastTime, startamount, endamount, restatus, tastatus, pageRequest);
		// 查询总计进行返回
		java.lang.Object[] total = accountStatisticsRepository.totalqueyfindFinOutStatSys(accountid, accountname,
				ptfristTime, ptlastTime, startamount, endamount, restatus, tastatus);
		Map<String, Object> map = new HashMap<>();
		map.put("Page", dataToPage);
		map.put("total", total);
		return map;
	}

	@Override
	public Map<String, Object> findFinOutStatFlow(int accountid, String toaccountowner, String toaccount,
			String ptfristTime, String ptlastTime, BigDecimal startamount, BigDecimal endamount, int bkstatus,
			int typestatus, PageRequest pageRequest) throws Exception {
		log.debug(
				"调用出款明细>查询银行明细 参数 accountid：{},toaccountowner：{},toaccount：{},ptfristTime：{},ptlastTime：{} ,pageRequest：{}：",
				accountid, toaccountowner, toaccount, ptfristTime, ptlastTime, pageRequest);
		Page<Object> dataToPage = accountStatisticsRepository.queyfindFinOutStatFlow(accountid, toaccountowner,
				toaccount, ptfristTime, ptlastTime, startamount, endamount, bkstatus, typestatus, pageRequest);
		// 查询总计进行返回
		java.lang.Object[] total = accountStatisticsRepository.totalqueyfindFinOutStatFlow(accountid, toaccountowner,
				toaccount, ptfristTime, ptlastTime, startamount, endamount, bkstatus, typestatus);
		Map<String, Object> map = new HashMap<>();
		map.put("Page", dataToPage);
		map.put("total", total);
		return map;
	}

	@Override
	public Page<Object> findFinOutStatFlowDetails(int id, PageRequest pageRequest) throws Exception {
		log.debug("调用出款明细>查询银行明细>详情信息 参数 id：{} ,pageRequest：{}：", id, pageRequest);
		return accountStatisticsRepository.queyfindFinOutStatFlowDetails(id, pageRequest);
	}

	@Override
	public Map<String, Object> findAccountStatisticsHandicap(int whereHandicap, int whereLevel, String fristTime,
			String lastTime, String fieldvalHandicap, String whereTransactionValue, PageRequest pageRequest)
			throws Exception {
		log.debug(
				"出款明细>按盘口统计参数  whereHandicap：{},whereLevel：{},fristTime：{},lastTime：{},fieldvalHandicap：{},whereTransactionValue：{},pageRequest：{}：",
				whereHandicap, whereLevel, fristTime, lastTime, fieldvalHandicap, whereTransactionValue, pageRequest);
		// 按盘口统计 如果盘口和层级为空则根据时间统计查询 、如果不为空则不统计查询，只查询相关的盘口、层级信息
		Page<Object> dataToPage;
		if (0 == whereHandicap) {
			dataToPage = accountStatisticsRepository.queyAccountStatisticsHandicap(fristTime, lastTime,
					fieldvalHandicap, whereTransactionValue, pageRequest);
			// 查询总计进行返回
			java.lang.Object[] total = accountStatisticsRepository.totalqueyAccountStatisticsHandicap(fristTime,
					lastTime, fieldvalHandicap, whereTransactionValue);
			Map<String, Object> map = new HashMap<>();
			map.put("Page", dataToPage);
			map.put("total", total);
			return map;
		} else {
			dataToPage = accountStatisticsRepository.queyAccountStatisticsByHandicapAndLevel(whereHandicap, whereLevel,
					fristTime, lastTime, fieldvalHandicap, whereTransactionValue, pageRequest);
			// 查询总计进行返回
			java.lang.Object[] total = accountStatisticsRepository.totalqueyAccountStatisticsByHandicapAndLevel(
					whereHandicap, whereLevel, fristTime, lastTime, fieldvalHandicap, whereTransactionValue);
			Map<String, Object> map = new HashMap<>();
			map.put("Page", dataToPage);
			map.put("total", total);
			return map;
		}
	}

	@Override
	public Map<String, Object> findAccountStatisticsHandicapFromClearDate(List<Integer> handicapList, int whereLevel,
			String fristTime, String lastTime, String fieldvalHandicap, String whereTransactionValue,
			PageRequest pageRequest) throws Exception {
		log.debug(
				"出款明细>按盘口统计参数  whereLevel：{},fristTime：{},lastTime：{},fieldvalHandicap：{},whereTransactionValue：{},pageRequest：{}：",
				whereLevel, fristTime, lastTime, fieldvalHandicap, whereTransactionValue, pageRequest);
		// 按盘口统计 如果盘口和层级为空则根据时间统计查询 、如果不为空则不统计查询，只查询相关的盘口、层级信息
		Page<Object> dataToPage;
		dataToPage = accountStatisticsRepository.queyAccountStatisticsHandicapFromClearDate(handicapList,
				fieldvalHandicap, whereTransactionValue, pageRequest);
		// // 查询总计进行返回
		// java.lang.Object[] total = accountStatisticsRepository
		// .totalqueyAccountStatisticsHandicapFromClearDate(handicapList,
		// fieldvalHandicap, whereTransactionValue);
		Map<String, Object> map = new HashMap<>();
		map.put("Page", dataToPage);
		// map.put("total", total);
		return map;
	}

	@Override
	public Map<String, Object> findFinOutStatMatch(int handicap, int level, String member, String fristTime,
			String lastTime, BigDecimal startamount, BigDecimal endamount, String type, int rqhandicap, int id,
			int restatus, int tastatus, List<Integer> handicaps, PageRequest pageRequest) throws Exception {
		log.debug(
				"出款明细>按盘口统计>明细 参数  handicap：{},level：{},member：{},fristTime：{},lastTime：{},startamount：{},endamount：{},type：{},rqhandicap：{},id：{},pageRequest：{}：",
				handicap, level, member, fristTime, lastTime, startamount, endamount, type, rqhandicap, id,
				pageRequest);
		// 按盘口统计 如果盘口和层级为空则根据时间统计查询 、如果不为空则不统计查询，只查询相关的盘口、层级信息
		Page<Object> dataToPage = accountStatisticsRepository.queyAccountMatchByhandicap(handicap, level, member,
				fristTime, lastTime, startamount, endamount, rqhandicap, id, restatus, tastatus, handicaps,
				pageRequest);
		// 查询去重的总计进行返回 因为存在拆单的现象 可能父表存在多条同样的记录
		java.lang.Object[] total = accountStatisticsRepository.totalqueyAccountMatchByhandicap(handicap, level, member,
				fristTime, lastTime, startamount, endamount, rqhandicap, id, restatus, tastatus);
		// 查询子表总计进行返回
		java.lang.Object[] total1 = accountStatisticsRepository.totalqueyAccountMatchByhandicap1(handicap, level,
				member, fristTime, lastTime, startamount, endamount, rqhandicap, id, restatus, tastatus);
		Map<String, Object> map = new HashMap<>();
		map.put("Page", dataToPage);
		map.put("total", total);
		map.put("total1", total1);
		return map;
	}
}
