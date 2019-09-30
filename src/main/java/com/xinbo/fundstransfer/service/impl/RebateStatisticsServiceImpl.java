package com.xinbo.fundstransfer.service.impl;

import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizAccountMore;
import com.xinbo.fundstransfer.domain.entity.BizRebateUser;
import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import com.xinbo.fundstransfer.domain.enums.CurrentSystemLevel;
import com.xinbo.fundstransfer.domain.repository.RebateStatisticsRepository;
import com.xinbo.fundstransfer.service.*;

import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class RebateStatisticsServiceImpl implements RebateStatisticsService {
	private static final Logger log = LoggerFactory.getLogger(RebateStatisticsServiceImpl.class);

	@Autowired
	private RebateStatisticsRepository rebateStatisticsDao;
	@PersistenceContext
	private EntityManager entityManager;
	@Autowired
	private CommonRemarkServiceImpl commonRemarkServiceImpl;
	@Autowired
	private AccountMoreService accMoreSer;
	@Autowired
	private RebateUserServiceImpl rebateUserSer;
	@Autowired
	private AllocateIncomeAccountService alloIAcntSer;

	@Override
	public Map<String, Object> showRebateStatistics(String startDate, String endDate, PageRequest pageRequest)
			throws Exception {
		Page<Object> dataToPage;
		Map<String, Object> map = new HashMap<String, Object>();
		dataToPage = rebateStatisticsDao.showRebateStatistics(startDate, endDate, pageRequest);
		// 查询总计进行返回
		map.put("rebatePage", dataToPage);
		return map;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Map<String, Object> showRebateUserByType(String startDate, String queryType, String rebateUser, String bankType,
			 String account, String owner, Integer[] status, String alias, PageRequest pageRequest) throws Exception{
		StringBuilder countBuilder = new StringBuilder("select count(1) from ( ");
		StringBuilder strBuilder = new StringBuilder(
				"SELECT t2.* FROM biz_rebate_user t LEFT JOIN biz_account_more t1 ON t.uid = t1.uid");
		strBuilder.append(" LEFT JOIN biz_account t2 ON t1.moible = t2.mobile ");
		if(!queryType.equals("3")) {
			strBuilder.append(" and t2.type in (1, 5, 8, 13) ");
		}
		if(queryType.equals("8") || queryType.equals("9")) {
			strBuilder.append(
					" LEFT JOIN ( SELECT * FROM biz_income_request tin WHERE tin.type = '401' AND tin.update_time >= '")
					.append(startDate + " 07:00:00 '").append(" AND tin.update_time <= '").append(getDateAdd(startDate) + " 06:59:59'")
					.append(" AND tin.STATUS = '5') t3 ON t2.account = t3.to_account ");
		}
		strBuilder.append(" where flag = 2 ");
		if(queryType.equals("8") || queryType.equals("9")) {
			strBuilder.append(" AND t3.to_account is not null ");
		}
		if (queryType.equals("1") || queryType.equals("4") || queryType.equals("8")) {
			strBuilder.append(" AND t.create_time >= '").append(startDate + " 07:00:00")
					.append("' AND t.create_time <= '").append(getDateAdd(startDate) + " 06:59:59'");
		}
		if (queryType.equals("4") || queryType.equals("5")) {
			strBuilder.append(" AND t2.create_time >= '").append(startDate + " 07:00:00")
					.append("' AND t2.create_time <= '").append(getDateAdd(startDate) + " 06:59:59'");
		}
		if (queryType.equals("5") || queryType.equals("9")) {
			strBuilder.append(" AND t.create_time <= '").append(startDate + " 07:00:00'");
		}
		if(queryType.equals("6")) {
			strBuilder.append(" AND t2.account is not null ");
		}
		if(Objects.nonNull(rebateUser)) {
			strBuilder.append(" AND ( '"+rebateUser).append("' IS NULL OR t.user_name LIKE concat( '%"+rebateUser).append("%' ) ) ");
		}
		if(Objects.nonNull(bankType)) {
			strBuilder.append(" AND ( '"+bankType).append("' IS NULL OR t2.bank_type LIKE concat( '%"+bankType).append("%' ) ) ");
		}
		if(Objects.nonNull(account)) {
			strBuilder.append(" AND ( '"+account).append("' IS NULL OR t2.account LIKE concat( '%"+account).append("%' ) ) ");
		}
		if(Objects.nonNull(owner)) {
			strBuilder.append(" AND ( '"+owner).append("' IS NULL OR t2.OWNER LIKE concat( '%"+owner).append("%' ) ) ");
		}
		if(Objects.nonNull(alias)) {
			strBuilder.append(" AND ( '"+alias).append("' IS NULL OR t2.alias LIKE concat( '%"+alias).append("%' ) ) ");
		}
		if(Objects.nonNull(status) && status.length > 0) {
			String str = "";
			for (Integer s : status) {
				str = str + s + ",";
			}
			str = str.substring(0, str.length()-1);
			strBuilder.append(" AND t2.status in ( "+str).append(" ) ");
		}
		countBuilder.append(strBuilder).append(") total ");
		strBuilder.append(" order by t1.margin desc, t1.id desc");
		strBuilder.append(" limit ");
		strBuilder.append(pageRequest.getOffset()).append(",").append(pageRequest.getPageSize());
		// 查询列表信息
		List<BizAccount> accountList = entityManager.createNativeQuery(strBuilder.toString(), BizAccount.class)
				.getResultList();
		BigInteger totalCount = (BigInteger)entityManager.createNativeQuery(countBuilder.toString()).getSingleResult();
		for (BizAccount bizAccount : accountList) {
			if (!Objects.nonNull(bizAccount.getId())) {
				bizAccount.setUserName(bizAccount.getMobile());
			} else {
				BizAccountMore more = accMoreSer.getFromCacheByMobile(bizAccount.getMobile());
				if(more == null) {
					log.debug("bizAccount ---- :{}", bizAccount.getMobile());
					continue;
				}
				BizRebateUser rebate = rebateUserSer.getFromCacheByUid(more.getUid());
				String remark = commonRemarkServiceImpl.latestRemark(Integer.valueOf(more.getUid()), "rebateUser");
				bizAccount.setRemark4Extra(remark);
				bizAccount.setUserName(rebate.getUserName());
				bizAccount.setMargin(more.getMargin());
				bizAccount.setCurrSysLevelName(CurrentSystemLevel
						.valueOf((int) (bizAccount.getCurrSysLevel() == null ? 1 : bizAccount.getCurrSysLevel()))
						.getName());
				bizAccount.setRemark(StringUtils.isNotBlank((String) bizAccount.getRemark())
						? ((String) bizAccount.getRemark()).replace("\r\n", "<br>").replace("\n", "<br>")
						: "");
				bizAccount.setCreateTimeStr(AccountStatus.findByStatus((Integer) bizAccount.getStatus()).getMsg());
			}
		}
		Map<String, Object> result = new HashMap<>();
		result.put("page", new PageImpl(accountList, pageRequest, totalCount.longValue()));
		return result;
	}
	
	@Override
	public void scheduleStatisticsReabte() throws Exception{
		scheduleStatisticsReabteData();
	}
	
	
	@Scheduled(fixedRate = 180000)
	public void scheduleStatisticsReabteData() {
		log.info("scheduleStatisticsReabteData start");
		if (!alloIAcntSer.checkHostRunRight()) {
			String nativeHost = CommonUtils.getInternalIp();
			log.debug("the host {} have no right to execute the allocation transfer schedule at present.",
					nativeHost);
			return;
		}
		if (!checkDate()) {
			log.info("当前非返利网业务数据分析执行时间！");
			return;
		}
		try {
			// 获取当前日期，计算统计数据区间
			String[] currentDate = getCurrentDate();
			
			// 新增业务数据统计
			rebateStatisticsDao.insertRebateStatistics(currentDate[0], currentDate[1]);
		} catch (Exception e) {
			log.error("返利网页数数据分析异常：{}", e.getMessage());
		}
		return;
	}

	private boolean checkDate() {
		long curr = System.currentTimeMillis();
		Calendar cal = Calendar.getInstance();
		// 设置任务开始时间
		cal.set(Calendar.HOUR_OF_DAY, 6);
		cal.set(Calendar.MINUTE, 50);
		cal.set(Calendar.SECOND, 0);
		Long REBATE_STATISTICS_START_TIME = cal.getTime().getTime();
		// 设置任务结束时间
		cal.set(Calendar.HOUR_OF_DAY, 12);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		Long REBATE_STATISTICS_END_TIME = cal.getTime().getTime();
		if (REBATE_STATISTICS_START_TIME <= curr && curr <= REBATE_STATISTICS_END_TIME) {
			int isDate = rebateStatisticsDao.findRebateStatisticsByDate(getDateAdd("")+" 00:00:00");
			if(isDate > 0) {
				return false;
			}
			return true;
		}
		return false;
	}
	
	// 获取当前日期，计算统计数据区间
	private String[] getCurrentDate() {
		String[] starAndEndDate = new String[2];
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");// 设置日期格式
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DATE, -1);
		Date time = calendar.getTime();
		starAndEndDate[0] = df.format(time) + " 07:00:00";
		starAndEndDate[1] = df.format(new Date()) + " 06:59:59";
		return starAndEndDate;
	}
	
	private String getDateAdd(String date) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();  
        try {
        	if(date != "") {
				c.setTime(sdf.parse(date));
				c.add(Calendar.DAY_OF_MONTH, 1); 
        	}else {
        		c.setTime(new Date());
				c.add(Calendar.DAY_OF_MONTH, -1);
        	}
			Date sDate = c.getTime();
			date = sdf.format(sDate);
		} catch (ParseException e) {
			e.printStackTrace();
		}  
        return date;
	}
}
