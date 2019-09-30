package com.xinbo.fundstransfer.restful;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.component.common.TimeChangeCommon;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.service.FinMoreStatStatService;
import com.xinbo.fundstransfer.service.HandicapService;
import com.xinbo.fundstransfer.service.SysDataPermissionService;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.ClearDateReport;
import com.xinbo.fundstransfer.domain.entity.FinMoreStat;
import com.xinbo.fundstransfer.domain.entity.SysUser;

import java.util.Map;
import java.util.TimeZone;

/**
 * 出入财务汇总请求接口
 * 
 * @author Steven
 *
 */
@Slf4j
@RestController
@RequestMapping("/r/finmorestat")
public class FinMoreStatController extends BaseController {
	@Autowired
	private FinMoreStatStatService finMoreStatStatService;
	@Autowired
	private ObjectMapper mapper = new ObjectMapper();
	@Autowired
	private HandicapService handicapService;
	@Autowired
	private SysDataPermissionService sysDataPermissionService;
	String params = "";

	/** 获取当前用户拥有的盘口id */
	private List<Integer> getHandicapIdByCurrentUser(Integer handicap, SysUser sysUser) {
		List<Integer> handicapList = new ArrayList<>();
		SysUser loginUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			return handicapList;
		}
		if (loginUser.getId() != 0 && handicap == 0) {
			List<BizHandicap> list = sysDataPermissionService.getHandicapByUserId(sysUser);
			if (list != null && list.size() > 0) {
				list.stream().forEach(p -> handicapList.add(p.getId()));
			}
		} else {
			handicapList.add(handicap);
		}
		return handicapList;
	}

	/**
	 * 
	 * @param pageNo
	 *            页码
	 * @param handicap
	 *            盘口
	 * @param startAndEndTimeToArray
	 *            时间控件数组值
	 * @param fieldval
	 *            时间单选按钮值
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequiresPermissions("FinMoreStat:*")
	@RequestMapping("/finmorestat")
	public String AccountStatistics(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "handicap", required = false) int handicap,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "fieldval", required = false) String fieldval,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, 100);
			// 拼接根据时间查询的条件(今日、本周、本月、上月)
			String whereTransactionValue = "";
			if ("today".equals(fieldval)) {
				// 今天数据
				Date nowTime = new Date(System.currentTimeMillis());
				SimpleDateFormat sdFormatter = new SimpleDateFormat("yyyy-MM-dd");
				String retStrFormatNowDate = sdFormatter.format(nowTime);
				String startTime = retStrFormatNowDate + " 07:00:00";
				Calendar c = Calendar.getInstance();
				c.setTime(nowTime);
				c.add(Calendar.DAY_OF_MONTH, 1);// 今天+1天
				Date tomorrow = c.getTime();
				String endTime = sdFormatter.format(tomorrow) + " 06:59:59";
				fieldval = startTime;
				whereTransactionValue = endTime;
			} else if ("thisWeek".equals(fieldval)) {
				// 本周的数据
				// fieldval = getTimesWeekmorning().toString();
				// whereTransactionValue = getTimesWeeknight().toString();
				// 昨日数据
				fieldval = TimeChangeCommon.getYesterdayStartTime();
				whereTransactionValue = TimeChangeCommon.getYesterdayEndTime();
			} else if ("thisMonth".equals(fieldval)) {
				// 本月的数据
				fieldval = TimeChangeCommon.getTimesMonthmorning();
				whereTransactionValue = TimeChangeCommon.getTimesMonthnight();
			} else if ("lastMonth".equals(fieldval)) {
				// 上月的数据
				fieldval = TimeChangeCommon.getLastMonthStartMorning();
				whereTransactionValue = TimeChangeCommon.getLastMonthEndMorning();
			}
			// 拼接时间戳查询数据条件
			String fristTime = null;
			String lastTime = null;
			if (0 != startAndEndTimeToArray.length && null != startAndEndTimeToArray) {
				fristTime = startAndEndTimeToArray[0];
				lastTime = startAndEndTimeToArray[1];
				fieldval = null;
				whereTransactionValue = null;
			}
			if (0 != handicap) {
			} else {
				handicap = 0;
			}
			Map<String, Object> mapp = finMoreStatStatService.findMoreStat(handicap,
					TimeChangeCommon.TimeStamp2Date(fristTime, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(lastTime, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(fieldval, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(whereTransactionValue, "yyyy-MM-dd HH:mm:ss"), pageRequest);
			Map<String, Object> map = new LinkedHashMap<>();
			List<FinMoreStat> arrlist = new ArrayList<FinMoreStat>();
			Page<Object> Page = (Page<Object>) mapp.get("Page");
			if (null != Page) {
				// List<Object> newlist=Page.getContent();
				// 用新构造的集合，里面有入款人数、出款人数信息
				List<Object> newlist = (List<Object>) mapp.get("data");
				for (int i = 0; i < newlist.size(); i++) {
					Object[] obj = (Object[]) newlist.get(i);
					FinMoreStat FinMoreStat = new FinMoreStat();
					FinMoreStat.setId((int) obj[0]);
					FinMoreStat.setHandicapname((String) obj[1]);
					// 入款数据
					FinMoreStat.setCountinps(new Integer(String.valueOf(obj[10])));
					FinMoreStat.setCountin(new Integer(String.valueOf(obj[2])));
					FinMoreStat.setAmountinbalance((BigDecimal) obj[3]);
					FinMoreStat.setAmountinactualamount((BigDecimal) obj[4]);
					// 出款数据
					FinMoreStat.setCountoutps(new Integer(String.valueOf(obj[11])));
					FinMoreStat.setCountout(new Integer(String.valueOf(obj[5])));
					FinMoreStat.setCountoutfee((BigDecimal) obj[6]);
					FinMoreStat.setAmountoutbalance((BigDecimal) obj[7]);
					FinMoreStat.setAmountoutactualamount((BigDecimal) obj[8]);
					// 盈利金额
					FinMoreStat.setProfit((BigDecimal) obj[9]);
					arrlist.add(FinMoreStat);
				}

			}
			// 暂时取小计，盘口没有超过十个
			// map.put("total", mapp.get("total"));
			map.put("page", new Paging(Page));
			map.put("arrlist", arrlist);
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("出入财务汇总Controller查询失败" + e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequiresPermissions("FinMoreStat:*")
	@RequestMapping("/finmorestatFromClearDate")
	public String AccountStatisticsFromClearDate(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "handicap", required = false) int handicap,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "fieldval", required = false) String fieldval,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, 100);
			BizHandicap hd = handicapService.findFromCacheById(handicap);
			// 拼接根据时间查询的条件(今日、本周、本月、上月)
			String whereTransactionValue = "";
			String fristTime = null;
			String lastTime = null;
			if ("today".equals(fieldval)) {
				// 今天数据
				Date nowTime = new Date(System.currentTimeMillis());
				SimpleDateFormat sdFormatter = new SimpleDateFormat("yyyy-MM-dd");
				String retStrFormatNowDate = sdFormatter.format(nowTime);
				String startTime = retStrFormatNowDate + " 07:00:00";
				Calendar c = Calendar.getInstance();
				c.setTime(nowTime);
				c.add(Calendar.DAY_OF_MONTH, 1);// 今天+1天
				Date tomorrow = c.getTime();
				String endTime = sdFormatter.format(tomorrow) + " 06:59:59";
				fieldval = startTime;
				whereTransactionValue = endTime;
				fristTime = startTime;
				lastTime = endTime;
			} else if ("thisWeek".equals(fieldval)) {
				// 本周的数据
				// fieldval = getTimesWeekmorning().toString();
				// whereTransactionValue = getTimesWeeknight().toString();
				// 昨日数据
				fieldval = TimeChangeCommon.getYesterdayStartTime();
				whereTransactionValue = TimeChangeCommon.getYesterdayEndTime();
				fristTime = fieldval;
				lastTime = whereTransactionValue;
			} else if ("thisMonth".equals(fieldval)) {
				// 本月的数据
				fieldval = TimeChangeCommon.getTimesMonthmorning();
				whereTransactionValue = TimeChangeCommon.getTimesMonthnight();
				fristTime = fieldval;
				lastTime = whereTransactionValue;
			} else if ("lastMonth".equals(fieldval)) {
				// 上月的数据
				fieldval = TimeChangeCommon.getLastMonthStartMorning();
				whereTransactionValue = TimeChangeCommon.getLastMonthEndMorning();
				fristTime = fieldval;
				lastTime = whereTransactionValue;
			} else if ("week".equals(fieldval)) {
				fieldval = TimeChangeCommon.getTimesWeekmorning();
				whereTransactionValue = TimeChangeCommon.getTimesWeeknight();
				fristTime = fieldval;
				lastTime = whereTransactionValue;
			} else if ("lastweek".equals(fieldval)) {
				fieldval = TimeChangeCommon.getPreviousWeekday();
				whereTransactionValue = TimeChangeCommon.getTimesWeekmorningAt6();
				fristTime = fieldval;
				lastTime = whereTransactionValue;
			}
			// 拼接时间戳查询数据条件
			if (0 != startAndEndTimeToArray.length && null != startAndEndTimeToArray) {
				fristTime = startAndEndTimeToArray[0];
				lastTime = startAndEndTimeToArray[1];
				SimpleDateFormat sdFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				Date retStrFormatNowDate = sdFormatter.parse(lastTime);
				Calendar c = Calendar.getInstance();
				c.setTime(retStrFormatNowDate);
				c.add(Calendar.DAY_OF_MONTH, 1);// 今天+1天
				Date tomorrow = c.getTime();
				lastTime = sdFormatter.format(tomorrow);
				fieldval = null;
				whereTransactionValue = null;
			}
			if (0 != handicap) {
			} else {
				handicap = 0;
			}
			SysUser loginUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			// 获取当前用户盘口 进行权限限制
			List<Integer> handicapList = getHandicapIdByCurrentUser(loginUser.getId() == 1 ? 0 : handicap, loginUser);
			Map<String, Object> mapp = finMoreStatStatService.findMoreStatFromClearDate(handicapList,
					TimeChangeCommon.TimeStamp2Date(fristTime, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(lastTime, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(fieldval, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(whereTransactionValue, "yyyy-MM-dd HH:mm:ss"), pageRequest);
			// 查询根据盘口统计的出款单信息
			Map<String, Object> thirdMapp = finMoreStatStatService.findOutThird(
					TimeChangeCommon.TimeStamp2Date(fristTime, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(lastTime, "yyyy-MM-dd HH:mm:ss"), pageRequest);

			// 查询根据盘口统计的入款单信息
			Map<String, Object> incomeMap = finMoreStatStatService.findIncomRequest(
					TimeChangeCommon.TimeStamp2Date(fristTime, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(lastTime, "yyyy-MM-dd HH:mm:ss"), pageRequest);

			// 根据盘口统计当天的冻结卡张数以及金额。
			Map<String, Object> freezeCardMapp = finMoreStatStatService.findfreezeCard(
					TimeChangeCommon.TimeStamp2Date(fristTime, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(lastTime, "yyyy-MM-dd HH:mm:ss"), pageRequest);

			// 根据盘口统计每天的第三方入款笔数
			// Map<String, Object> thirdIncomCounts =
			// finMoreStatStatService.findThirdIncomCounts(
			// TimeChangeCommon.TimeStamp2Date(fristTime, "yyyy-MM-dd
			// HH:mm:ss"),
			// TimeChangeCommon.TimeStamp2Date(lastTime, "yyyy-MM-dd HH:mm:ss"),
			// pageRequest);

			// Map<String, Object> outMap =
			// finMoreStatStatService.findOutPerson(
			// TimeChangeCommon.TimeStamp2Date(fristTime, "yyyy-MM-dd
			// HH:mm:ss"),
			// TimeChangeCommon.TimeStamp2Date(lastTime, "yyyy-MM-dd HH:mm:ss"),
			// pageRequest);
			Map<String, Object> map = new LinkedHashMap<>();
			List<ClearDateReport> arrlist = new ArrayList<ClearDateReport>();
			Page<Object> Page = (Page<Object>) mapp.get("Page");
			if (null != Page) {
				// List<Object> newlist=Page.getContent();
				// 用新构造的集合，里面有入款人数、出款人数信息
				List<Object> newlist = (List<Object>) mapp.get("data");
				List<Object> thirdList = (List<Object>) thirdMapp.get("data");
				List<Object> incomeList = (List<Object>) incomeMap.get("data");
				List<Object> freezeCardList = (List<Object>) freezeCardMapp.get("data");
				// List<Object> thirdIncomCountsList = (List<Object>)
				// thirdIncomCounts.get("data");
				// 查询人数
				// List<Object> outPerson = (List<Object>) outMap.get("data");
				for (int i = 0; i < newlist.size(); i++) {
					Object[] obj = (Object[]) newlist.get(i);
					if (null == obj[0])
						continue;
					ClearDateReport clearDateReport = new ClearDateReport();
					BizHandicap bizHandicap = handicapService.findFromCacheById(null == obj[0] ? 0 : (int) obj[0]);
					if (bizHandicap.getStatus() != 1)
						continue;
					clearDateReport.setHandicapId(null == obj[0] ? 0 : (int) obj[0]);
					clearDateReport.setHandicapName(null == bizHandicap ? null : bizHandicap.getName());
					clearDateReport.setIncomePerson(null == obj[1] ? 0 : new Integer(String.valueOf(obj[1])));
					clearDateReport.setIncomeCount(null == obj[2] ? 0 : new Integer(String.valueOf(obj[2])));
					clearDateReport.setIncome(null == obj[3] ? BigDecimal.ZERO : new BigDecimal(obj[3].toString()));
					clearDateReport
							.setCompanyIncome(null == obj[3] ? BigDecimal.ZERO : new BigDecimal(obj[3].toString()));
					// 取income表入款数据
					if (incomeList.size() > 0) {
						for (int k = 0; k < incomeList.size(); k++) {
							Object[] incomeObj = (Object[]) incomeList.get(k);
							if (incomeObj[0] == bizHandicap.getId()) {
								clearDateReport.setIncome(null == incomeObj[1] ? BigDecimal.ZERO
										: new BigDecimal(incomeObj[1].toString()));
								clearDateReport.setCompanyIncome(null == incomeObj[1] ? BigDecimal.ZERO
										: new BigDecimal(incomeObj[1].toString()));
							}
						}
					}
					// clearDateReport.setIncome(new
					// BigDecimal(obj[3].toString()));
					// clearDateReport.setCompanyIncome(new
					// BigDecimal(obj[3].toString()));
					// for (int j = 0; j < outPerson.size(); j++) {
					// Object[] outPersonobj = (Object[]) outPerson.get(j);
					// if (outPersonobj[0] == bizHandicap.getId()) {
					// clearDateReport.setOutwardPerson(
					// new Integer((outPersonobj[1] == null ? "0" :
					// outPersonobj[1].toString())));
					// }
					// }

					for (int j = 0; j < freezeCardList.size(); j++) {
						Object[] freezeCard = (Object[]) freezeCardList.get(j);
						if (freezeCard[0] == bizHandicap.getId()) {
							clearDateReport.setFreezeCardCount(
									null == freezeCard[1] ? 0 : new Integer(String.valueOf(freezeCard[1])));
							clearDateReport.setFreezeAmounts(
									null == freezeCard[2] ? BigDecimal.ZERO : new BigDecimal(freezeCard[2].toString()));
						}
					}

					// for (int j = 0; j < thirdIncomCountsList.size(); j++) {
					// Object[] thirdIncomCount = (Object[])
					// thirdIncomCountsList.get(j);
					// if (thirdIncomCount[0] == bizHandicap.getId()) {
					// clearDateReport.setThirdIncomeCount(new
					// Integer(String.valueOf(thirdIncomCount[1])));
					// }
					// }

					clearDateReport.setOutwardCount(null == obj[5] ? 0 : new Integer(String.valueOf(obj[5])));
					clearDateReport.setFee(null == obj[6] ? BigDecimal.ZERO : new BigDecimal(obj[6].toString()));
					clearDateReport.setOutwardSys(null == obj[7] ? BigDecimal.ZERO : new BigDecimal(obj[7].toString()));
					clearDateReport.setIncomeFee(new BigDecimal("0"));
					for (int j = 0; j < thirdList.size(); j++) {
						Object[] Thirdobj = (Object[]) thirdList.get(j);
						if (Thirdobj[1] == bizHandicap.getId()) {
							clearDateReport.setOutwardSys(
									new BigDecimal((Thirdobj[0] == null ? "0" : Thirdobj[0].toString())));
							clearDateReport.setOutwardSysCount(
									new Integer((Thirdobj[2] == null ? "0" : Thirdobj[2].toString())));
							clearDateReport.setIncome(clearDateReport.getIncome()
									.add(new BigDecimal((Thirdobj[3] == null ? "0" : Thirdobj[3].toString()))));
							clearDateReport.setThirdIncome(
									new BigDecimal((Thirdobj[3] == null ? "0" : Thirdobj[3].toString())));
							clearDateReport.setIncomeCount(clearDateReport.getIncomeCount()
									+ new Integer((Thirdobj[4] == null ? "0" : Thirdobj[4].toString())));
							clearDateReport.setIncomePerson(clearDateReport.getIncomePerson()
									+ new Integer((Thirdobj[5] == null ? "0" : Thirdobj[5].toString())));
							clearDateReport
									.setIncomeFee(new BigDecimal((null == Thirdobj[6] ? "0" : Thirdobj[6].toString())));
							clearDateReport.setThirdIncomeCount(
									null == Thirdobj[4] ? 0 : new Integer(String.valueOf(Thirdobj[4])));
							clearDateReport.setOutwardPerson(
									null == Thirdobj[7] ? 0 : new Integer(String.valueOf(Thirdobj[7])));
						}

					}
					clearDateReport.setLosCount(null == obj[8] ? 0 : new Integer(String.valueOf(obj[8])));
					clearDateReport.setLos(null == obj[9] ? BigDecimal.ZERO : new BigDecimal(obj[9].toString()));
					arrlist.add(clearDateReport);
				}

			}
			// 暂时取小计，盘口没有超过十个
			// map.put("total", mapp.get("total"));
			map.put("page", new Paging(Page));
			map.put("arrlist", arrlist);
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			log.error("出入财务汇总Controller查询失败", e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequiresPermissions("FinMoreStat:*")
	@RequestMapping("/finmorestatFromClearDateRealTime")
	public String finmorestatFromClearDateRealTime(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "handicap", required = false) int handicap,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, 100);
			BizHandicap hd = handicapService.findFromCacheById(handicap);
			// 拼接根据时间查询的条件(今日、本周、本月、上月)
			String fristTime = null;
			String lastTime = null;
			// 今天数据
			Date nowTime = new Date(System.currentTimeMillis());
			SimpleDateFormat sdFormatter = new SimpleDateFormat("yyyy-MM-dd");
			String retStrFormatNowDate = sdFormatter.format(nowTime);
			String startTime = retStrFormatNowDate + " 07:00:00";
			Calendar c = Calendar.getInstance();
			c.setTime(nowTime);
			c.add(Calendar.DAY_OF_MONTH, 1);// 今天+1天
			Date tomorrow = c.getTime();
			String endTime = sdFormatter.format(tomorrow) + " 06:59:59";
			fristTime = startTime;
			lastTime = endTime;
			if (0 != handicap) {
			} else {
				handicap = 0;
			}
			SysUser loginUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			// 获取当前用户盘口 进行权限限制
			List<Integer> handicapList = getHandicapIdByCurrentUser(loginUser.getId() == 1 ? 0 : handicap, loginUser);
			Map<String, Object> mapp = finMoreStatStatService.finmorestatFromClearDateRealTime(handicapList,
					TimeChangeCommon.TimeStamp2Date(fristTime, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(lastTime, "yyyy-MM-dd HH:mm:ss"), pageRequest);
			// 查询第三方入款数据
			Map<String, Object> thridIncomeMap = finMoreStatStatService.findThridIncom(
					TimeChangeCommon.TimeStamp2Date(fristTime, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(lastTime, "yyyy-MM-dd HH:mm:ss"), pageRequest);

			// 查询亏损流水
			Map<String, Object> lossAmountsMap = finMoreStatStatService.findLossAmounts(
					TimeChangeCommon.TimeStamp2Date(fristTime, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(lastTime, "yyyy-MM-dd HH:mm:ss"), pageRequest);

			Map<String, Object> map = new LinkedHashMap<>();
			List<ClearDateReport> arrlist = new ArrayList<ClearDateReport>();
			Page<Object> Page = (Page<Object>) mapp.get("Page");
			if (null != Page) {
				List<Object> newlist = (List<Object>) mapp.get("data");
				List<Object> thirdIncomelist = (List<Object>) thridIncomeMap.get("data");
				List<Object> lossAmountslist = (List<Object>) lossAmountsMap.get("data");
				for (int i = 0; i < newlist.size(); i++) {
					Object[] obj = (Object[]) newlist.get(i);
					if (null == obj[0])
						continue;
					ClearDateReport clearDateReport = new ClearDateReport();
					BizHandicap bizHandicap = handicapService.findFromCacheById(null == obj[0] ? 0 : (int) obj[0]);
					clearDateReport.setHandicapId(null == obj[0] ? 0 : (int) obj[0]);
					clearDateReport.setHandicapName(null == bizHandicap ? null : bizHandicap.getName());
					// 入款人数
					clearDateReport.setIncomePerson(new Integer(String.valueOf(obj[1])));
					// 入款笔数
					clearDateReport.setIncomeCount(new Integer(String.valueOf(obj[2])));
					// 入款金额
					clearDateReport.setIncome(new BigDecimal(obj[3].toString()));
					// 公司入款金额
					clearDateReport.setCompanyIncome(new BigDecimal(obj[3].toString()));
					// 出款人数
					clearDateReport.setOutwardPerson(null == obj[5] ? 0 : new Integer(String.valueOf(obj[5])));
					// 出款笔数
					clearDateReport.setOutwardSysCount(null == obj[6] ? 0 : new Integer(String.valueOf(obj[6])));
					// 出款金额
					clearDateReport.setOutwardSys(null == obj[7] ? BigDecimal.ZERO : new BigDecimal(obj[7].toString()));
					// 出款手续费
					clearDateReport.setFee(null == obj[8] ? BigDecimal.ZERO : new BigDecimal(String.valueOf(obj[8])));
					// 冻结卡张数
					clearDateReport.setFreezeCardCount(null == obj[11] ? 0 : new Integer(String.valueOf(obj[11])));
					// 冻结卡金额
					clearDateReport
							.setFreezeAmounts(null == obj[12] ? BigDecimal.ZERO : new BigDecimal(obj[12].toString()));
					clearDateReport.setIncomeFee(new BigDecimal("0"));

					clearDateReport.setLosCount(0);
					clearDateReport.setLos(BigDecimal.ZERO);
					for (int m = 0; m < lossAmountslist.size(); m++) {
						Object[] Thirdobj = (Object[]) thirdIncomelist.get(m);
						if (Thirdobj[0] == bizHandicap.getId()) {
							clearDateReport.setLosCount(new Integer(String.valueOf(obj[2])));
							clearDateReport.setLos(new BigDecimal(obj[1].toString()));
						}
					}

					clearDateReport.setThirdIncome(BigDecimal.ZERO);
					clearDateReport.setThirdIncomeCount(0);
					for (int j = 0; j < thirdIncomelist.size(); j++) {
						Object[] Thirdobj = (Object[]) thirdIncomelist.get(j);
						if (Thirdobj[0] == bizHandicap.getId()) {
							clearDateReport.setIncome(clearDateReport.getIncome()
									.add(new BigDecimal((null == Thirdobj[1] ? "0" : Thirdobj[1].toString()))));
							clearDateReport.setThirdIncome(
									new BigDecimal((null == Thirdobj[1] ? "0" : Thirdobj[1].toString())));
							clearDateReport.setIncomeCount(clearDateReport.getIncomeCount()
									+ new Integer((null == Thirdobj[2] ? "0" : Thirdobj[2].toString())));
							clearDateReport.setIncomePerson(clearDateReport.getIncomePerson()
									+ new Integer((null == Thirdobj[3] ? "0" : Thirdobj[3].toString())));
							clearDateReport
									.setIncomeFee(new BigDecimal((null == Thirdobj[4] ? "0" : Thirdobj[4].toString())));
							clearDateReport.setThirdIncomeCount(
									null == Thirdobj[2] ? 0 : new Integer(String.valueOf(Thirdobj[2])));
						}

					}
					arrlist.add(clearDateReport);
				}

			}
			map.put("page", new Paging(Page));
			map.put("arrlist", arrlist);
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("出入财务汇总Controller查询失败" + e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 出入财务汇总>明细
	 * 
	 * @param pageNo
	 *            页码
	 * @param handicap
	 *            盘口
	 * @param level
	 *            层级
	 * @param startAndEndTimeToArray
	 *            时间控件数组值
	 * @param fieldval
	 *            时间单选按钮值
	 * @param parentstartAndEndTimeToArray
	 *            父页面时间控件数组值
	 * @param parentfieldval
	 *            父页面时间单选按钮值
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequiresPermissions("FinMoreStat:*")
	@RequestMapping("/finmorelevelstat")
	public String FinMoreLevelStat(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "handicap", required = false) int handicap,
			@RequestParam(value = "level", required = false) int level,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "fieldval", required = false) String fieldval,
			@RequestParam(value = "parentstartAndEndTimeToArray", required = false) String[] parentstartAndEndTimeToArray,
			@RequestParam(value = "parentfieldval", required = false) String parentfieldval,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			params = buildParams().toString();
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, 100);

			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			// 拼接父页面的时间
			String ptfristTime = null;
			String ptlastTime = null;
			if (parentstartAndEndTimeToArray.length != 0 && parentstartAndEndTimeToArray != null) {
				ptfristTime = parentstartAndEndTimeToArray[0];
				ptlastTime = parentstartAndEndTimeToArray[1];
			} else {
				if ("today".equals(parentfieldval)) {
					// 今天数据
					Date nowTime = new Date(System.currentTimeMillis());
					SimpleDateFormat sdFormatter = new SimpleDateFormat("yyyy-MM-dd");
					String retStrFormatNowDate = sdFormatter.format(nowTime);
					String startTime = retStrFormatNowDate + " 07:00:00";
					Calendar c = Calendar.getInstance();
					c.setTime(nowTime);
					c.add(Calendar.DAY_OF_MONTH, 1);// 今天+1天
					Date tomorrow = c.getTime();
					String endTime = sdFormatter.format(tomorrow) + " 06:59:59";
					ptfristTime = startTime;
					ptlastTime = endTime;
				} else if ("thisWeek".equals(parentfieldval)) {
					// 本周的数据
					// ptfristTime = getTimesWeekmorning().toString();
					// ptlastTime = getTimesWeeknight().toString();
					// 昨日数据
					ptfristTime = TimeChangeCommon.getYesterdayStartTime();
					ptlastTime = TimeChangeCommon.getYesterdayEndTime();
				} else if ("thisMonth".equals(parentfieldval)) {
					// 本月的数据
					ptfristTime = TimeChangeCommon.getTimesMonthmorning();
					ptlastTime = TimeChangeCommon.getTimesMonthnight();
				} else if ("lastMonth".equals(parentfieldval)) {
					// 上月的数据
					ptfristTime = TimeChangeCommon.getLastMonthStartMorning();
					ptlastTime = TimeChangeCommon.getLastMonthEndMorning();
				} else if ("lastweek".equals(parentfieldval)) {
					ptfristTime = TimeChangeCommon.getPreviousWeekday();
					ptlastTime = TimeChangeCommon.getTimesWeekmorningAt6();
				} else if ("week".equals(parentfieldval)) {
					ptfristTime = TimeChangeCommon.getTimesWeekmorning();
					ptlastTime = TimeChangeCommon.getTimesWeeknight();
				}
			} // 拼接根据时间查询的条件(今日、本周、本月、上月)
			String whereTransactionValue = "";
			if ("today".equals(fieldval)) {
				// 今天数据
				Date nowTime = new Date(System.currentTimeMillis());
				SimpleDateFormat sdFormatter = new SimpleDateFormat("yyyy-MM-dd");
				String retStrFormatNowDate = sdFormatter.format(nowTime);
				String startTime = retStrFormatNowDate + " 00:00:00";
				String endTime = retStrFormatNowDate + " 23:59:59";
				fieldval = startTime;
				whereTransactionValue = endTime;
			} else if ("thisWeek".equals(fieldval)) {
				// 本周的数据
				// fieldval = getTimesWeekmorning().toString();
				// whereTransactionValue = getTimesWeeknight().toString();
				// 昨日数据
				fieldval = TimeChangeCommon.getYesterdayStartTime();
				whereTransactionValue = TimeChangeCommon.getYesterdayEndTime();
			} else if ("thisMonth".equals(fieldval)) {
				// 本月的数据
				fieldval = TimeChangeCommon.getTimesMonthmorning();
				whereTransactionValue = TimeChangeCommon.getTimesMonthnight();
			} else if ("lastMonth".equals(fieldval)) {
				// 上月的数据
				fieldval = TimeChangeCommon.getLastMonthStartMorning();
				whereTransactionValue = TimeChangeCommon.getLastMonthEndMorning();
			}
			// 拼接时间戳查询数据条件
			String fristTime = null;
			String lastTime = null;
			if (0 != startAndEndTimeToArray.length && null != startAndEndTimeToArray) {
				fristTime = startAndEndTimeToArray[0];
				lastTime = startAndEndTimeToArray[1];
				Date dtf1 = df.parse(fristTime);
				Date dtf2 = df.parse(ptfristTime);
				Date dtl1 = df.parse(lastTime);
				Date dtl2 = df.parse(ptlastTime);
				// 当两个时间都不为空 比较两个时间的大小 进行赋值
				if (dtf1.getTime() < dtf2.getTime()) {
					fristTime = ptfristTime;
				}
				if (dtl1.getTime() > dtl2.getTime()) {
					lastTime = ptlastTime;
				}
				fieldval = null;
				whereTransactionValue = null;
			} else {
				if (fieldval == null) {
					fieldval = ptfristTime;
					whereTransactionValue = ptlastTime;
				}
				Date dtf1 = df.parse(fieldval);
				Date dtf2 = df.parse(ptfristTime);
				Date dtl1 = df.parse(whereTransactionValue);
				Date dtl2 = df.parse(ptlastTime);
				// 当两个时间都不为空 比较两个时间的大小 进行赋值
				if (dtf1.getTime() < dtf2.getTime()) {
					fieldval = ptfristTime;
				}
				if (dtl1.getTime() > dtl2.getTime()) {
					whereTransactionValue = ptlastTime;
				}
			}
			level = 0 != level ? level : 0;
			Map<String, Object> mapp = finMoreStatStatService.findMoreLevelStat(handicap, level,
					TimeChangeCommon.TimeStamp2Date(fristTime, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(lastTime, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(fieldval, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(whereTransactionValue, "yyyy-MM-dd HH:mm:ss"), pageRequest);
			Map<String, Object> map = new LinkedHashMap<>();
			List<FinMoreStat> arrlist = new ArrayList<FinMoreStat>();
			Page<Object> Page = (Page<Object>) mapp.get("Page");
			if (null != Page) {
				// List<Object> AccountStatisticsList=Page.getContent();
				// 用新构造的集合，里面有入款人数、出款人数信息
				List<Object> newlist = (List<Object>) mapp.get("data");
				for (int i = 0; i < newlist.size(); i++) {
					Object[] obj = (Object[]) newlist.get(i);
					FinMoreStat FinMoreStat = new FinMoreStat();
					FinMoreStat.setId((int) obj[0]);
					FinMoreStat.setHandicapname((String) obj[1]);
					FinMoreStat.setLevelname((String) obj[2]);
					// 入款数据
					FinMoreStat.setCountinps(new Integer(String.valueOf(obj[11])));
					FinMoreStat.setCountin(new Integer(String.valueOf(obj[3])));
					FinMoreStat.setAmountinbalance((BigDecimal) obj[4]);
					FinMoreStat.setAmountinactualamount((BigDecimal) obj[5]);
					// 出款数据
					FinMoreStat.setCountoutps(new Integer(String.valueOf(obj[12])));
					FinMoreStat.setCountout(new Integer(String.valueOf(obj[6])));
					FinMoreStat.setCountoutfee((BigDecimal) obj[7]);
					FinMoreStat.setAmountoutbalance((BigDecimal) obj[8]);
					FinMoreStat.setAmountoutactualamount((BigDecimal) obj[9]);
					// 盈利金额
					FinMoreStat.setProfit((BigDecimal) obj[10]);
					arrlist.add(FinMoreStat);
				}

			}
			// map.put("total", mapp.get("total"));
			map.put("page", new Paging(Page));
			map.put("arrlist", arrlist);
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			log.info("出入财务汇总》明细Controller查询失败" + e);
			log.info("参数" + params);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

}
