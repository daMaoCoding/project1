package com.xinbo.fundstransfer.service.impl;

import com.xinbo.fundstransfer.domain.repository.FinMoreStatRepository;
import com.xinbo.fundstransfer.service.FinMoreStatStatService;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinMoreStatServiceImpl implements FinMoreStatStatService {
	@Autowired
	private FinMoreStatRepository finMoreStatRepository;
	@PersistenceContext
	private EntityManager entityManager;
	private static final Logger logger = LoggerFactory.getLogger(FinMoreStatServiceImpl.class);

	/**
	 * 出入财务汇总
	 */
	@Override
	public Map<String, Object> findMoreStat(int handicap, String fristTime, String lastTime, String fieldval,
			String whereTransactionValue, PageRequest pageRequest) throws Exception {
		logger.debug(
				"调用出入 财务汇总  参数 handicap：{},fristTime：{},lastTime：{},fieldval：{},whereTransactionValue：{},pageRequest：{}：",
				handicap, fristTime, lastTime, fieldval, whereTransactionValue, pageRequest);
		Map<String, Object> map = new HashMap<String, Object>();
		Page<Object> dataToPage = finMoreStatRepository.queyfindMoreStat(handicap, fristTime, lastTime, fieldval,
				whereTransactionValue, pageRequest);
		// List<Object> newlist = new ArrayList<>();
		// // 循环查询入款人数、出款人数
		// List<Object> AccountStatisticsList = dataToPage.getContent();
		// SimpleDateFormat startTimeee = new SimpleDateFormat("yyyy-MM-dd
		// HH:mm:ss:SSS");
		// System.out.println(startTimeee.format(new Date()));
		// for (int i = 0; i < AccountStatisticsList.size(); i++) {
		// Object[] obj = (Object[]) AccountStatisticsList.get(i);
		// // 入款总人数
		// int countinps = finMoreStatRepository.queyfindMoreStatCountinps((int)
		// obj[0], fristTime, lastTime, fieldval,
		// whereTransactionValue);
		// // 出款总人数
		// int countoutps =
		// finMoreStatRepository.queyfindMoreStatCountoutps((int) obj[0],
		// fristTime, lastTime,
		// fieldval, whereTransactionValue);
		// Object[] newobj =
		// {obj[0],obj[1],obj[2],obj[3],obj[4],obj[5],obj[6],obj[7],obj[8],obj[9],
		// countinps,countoutps };
		// newlist.add(newobj);
		// }
		// SimpleDateFormat startTimeeee = new SimpleDateFormat("yyyy-MM-dd
		// HH:mm:ss:SSS");
		// System.out.println(startTimeeee.format(new Date()));
		// 查询总计进行返回
		// 暂时取小计的金额，盘口没有超过十个
		// java.lang.Object[] total =
		// finMoreStatRepository.totalfindMoreStat(handicap, fristTime,
		// lastTime, fieldval,
		// whereTransactionValue);
		map.put("Page", dataToPage);
		// map.put("total", total);
		map.put("data", dataToPage.getContent());
		return map;
	}

	@Transactional
	@Override
	public Map<String, Object> findMoreStatFromClearDate(List<Integer> handicapList, String fristTime, String lastTime,
			String fieldval, String whereTransactionValue, PageRequest pageRequest) throws Exception {
		logger.debug("调用出入 财务汇总  参数 fristTime：{},lastTime：{},fieldval：{},whereTransactionValue：{},pageRequest：{}：",
				fristTime, lastTime, fieldval, whereTransactionValue, pageRequest);
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
		Map<String, Object> map = new HashMap<String, Object>();
		Page<Object> dataToPage = finMoreStatRepository.queyfindMoreStatFromClearDate(handicapList, fristTime, lastTime,
				pageRequest);
		map.put("Page", dataToPage);
		// map.put("total", total);
		map.put("data", dataToPage.getContent());
		return map;
	}

	/**
	 * 出入财务汇总>明细
	 */
	@Override
	public Map<String, Object> findMoreLevelStat(int handicap, int level, String fristTime, String lastTime,
			String fieldval, String whereTransactionValue, PageRequest pageRequest) throws Exception {
		logger.debug(
				"调用 出入财务汇总》明细  参数 handicap：{},level：{},fristTime：{},lastTime：{},fieldval：{},whereTransactionValue:{},pageRequest：{}：",
				handicap, level, fristTime, lastTime, fieldval, whereTransactionValue, pageRequest);
		Map<String, Object> map = new HashMap<String, Object>();
		Page<Object> dataToPage = finMoreStatRepository.queyfindMoreLevelStat(handicap, level, fristTime, lastTime,
				fieldval, whereTransactionValue, pageRequest);
		// List<Object> newlist = new ArrayList<>();
		// // 循环查询入款人数、出款人数
		// List<Object> AccountStatisticsList = dataToPage.getContent();
		// for (int i = 0; i < AccountStatisticsList.size(); i++) {
		// Object[] obj = (Object[]) AccountStatisticsList.get(i);
		// // 入款总人数
		// int countinps =
		// finMoreStatRepository.queyfindMoreLevelStatCountinps((int) obj[2],
		// handicap, fristTime,
		// lastTime, fieldval, whereTransactionValue);
		// // 出款总人数
		// int countoutps =
		// finMoreStatRepository.queyfindMoreLevelStatCountoutps((int) obj[2],
		// handicap, fristTime,
		// lastTime, fieldval, whereTransactionValue);
		// Object[] newobj = { obj[1], obj[2], obj[3], obj[4], obj[5], obj[6],
		// obj[7], obj[8], obj[9], obj[10], obj[11],
		// countinps, countoutps };
		// newlist.add(newobj);
		// }
		// // 查询总计进行返回
		// 从页面直接取 小计的值 不分页显示
		// java.lang.Object[] total =
		// finMoreStatRepository.totalfindMoreLevelStat(handicap, level,
		// fristTime, lastTime,
		// fieldval, whereTransactionValue);
		map.put("Page", dataToPage);
		// map.put("total", total);
		map.put("data", dataToPage.getContent());
		return map;
	}

	@Override
	public Map<String, Object> findOutThird(String fristTime, String lastTime, PageRequest pageRequest)
			throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		Page<Object> dataToPage = finMoreStatRepository.findOutThird(fristTime, lastTime, pageRequest);
		map.put("Page", dataToPage);
		map.put("data", dataToPage.getContent());
		return map;
	}

	@Override
	public Map<String, Object> findIncomRequest(String fristTime, String lastTime, PageRequest pageRequest)
			throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		Page<Object> dataToPage = finMoreStatRepository.findIncomRequest(fristTime, lastTime, pageRequest);
		map.put("Page", dataToPage);
		map.put("data", dataToPage.getContent());
		return map;
	}

	@Override
	public Map<String, Object> findfreezeCard(String fristTime, String lastTime, PageRequest pageRequest)
			throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		Page<Object> dataToPage = finMoreStatRepository.findfreezeCard(fristTime, lastTime, pageRequest);
		map.put("Page", dataToPage);
		map.put("data", dataToPage.getContent());
		return map;
	}

	@Override
	public Map<String, Object> findThirdIncomCounts(String fristTime, String lastTime, PageRequest pageRequest)
			throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		Page<Object> dataToPage = finMoreStatRepository.findThirdIncomCounts(fristTime, lastTime, pageRequest);
		map.put("Page", dataToPage);
		map.put("data", dataToPage.getContent());
		return map;
	}

	@Override
	public Map<String, Object> findOutPerson(String startTime, String endTime, PageRequest pageRequest)
			throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		Page<Object> dataToPage = finMoreStatRepository.findOutPerson(startTime, endTime, pageRequest);
		map.put("Page", dataToPage);
		map.put("data", dataToPage.getContent());
		return map;
	}

	@Override
	public Map<String, Object> finmorestatFromClearDateRealTime(List<Integer> handicapList, String fristTime,
			String lastTime, PageRequest pageRequest) throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		Page<Object> dataToPage = finMoreStatRepository.finmorestatFromClearDateRealTime(handicapList, fristTime,
				lastTime, pageRequest);
		map.put("Page", dataToPage);
		map.put("data", dataToPage.getContent());
		return map;
	}

	@Override
	public Map<String, Object> findThridIncom(String fristTime, String lastTime, PageRequest pageRequest)
			throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		Page<Object> dataToPage = finMoreStatRepository.findThridIncom(fristTime, lastTime, pageRequest);
		map.put("Page", dataToPage);
		map.put("data", dataToPage.getContent());
		return map;
	}

	@Override
	public Map<String, Object> findLossAmounts(String fristTime, String lastTime, PageRequest pageRequest)
			throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		Page<Object> dataToPage = finMoreStatRepository.findLossAmounts(fristTime, lastTime, pageRequest);
		map.put("Page", dataToPage);
		map.put("data", dataToPage.getContent());
		return map;
	}

}
