package com.xinbo.fundstransfer.restful;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.component.common.TimeChangeCommon;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.service.AccountMoreService;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.AccountStatisticsService;
import com.xinbo.fundstransfer.service.CabanaService;
import com.xinbo.fundstransfer.service.FinMoreStatStatService;
import com.xinbo.fundstransfer.service.HandicapService;
import com.xinbo.fundstransfer.service.HostMonitorService;
import com.xinbo.fundstransfer.service.LevelService;
import com.xinbo.fundstransfer.service.RebateApiService;
import com.xinbo.fundstransfer.service.RedisService;
import com.xinbo.fundstransfer.service.SysDataPermissionService;
import com.xinbo.fundstransfer.service.SysUserProfileService;
import com.xinbo.fundstransfer.service.SysUserService;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.util.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 出款明细请求接口
 * 
 * @author Steven
 *
 */
@Slf4j
@RestController
@RequestMapping("/r/finoutstat")
public class AccountStatisticsController extends BaseController {
	@Autowired
	private AccountStatisticsService accountStatisticsService;
	@Autowired
	private LevelService levelService;
	@Autowired
	private HandicapService handicapService;
	@Autowired
	private SysUserService sysUserService;
	@Autowired
	private AccountService accountService;
	@Autowired
	private SysDataPermissionService sysDataPermissionService;
	@Autowired
	private FinMoreStatStatService finMoreStatStatService;
	@Autowired
	private SysUserProfileService sysUserProfileService;
	@Autowired
	private AccountMoreService accountMoreService;
	@Autowired
	private RedisService redisSer;
	@Autowired
	private RebateApiService rebateApiService;
	@Autowired
	private HostMonitorService hostMonitorService;
	@Autowired
	private CabanaService cabanaService;
	private ObjectMapper mapper = new ObjectMapper();

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
	 *            当前页码
	 * @param account
	 *            账号
	 * @param startAndEndTimeToArray
	 *            时间控件的时间搓
	 * @param fieldval
	 *            时间单选按钮的值
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequiresPermissions("FinanceOutward:*")
	@RequestMapping("/accountstatistics")
	public String AccountStatistics(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "account", required = false) String account,
			@RequestParam(value = "accountOwner", required = false) String accountOwner,
			@RequestParam(value = "bankType", required = false) String bankType,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "fieldval", required = false) String fieldval,
			@RequestParam(value = "handicap", required = false) int handicap,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE);
			// 拼接根据时间查询的条件(今日、本周、本月、上月)
			String whereTransactionValue = "";
			// 拼接时间戳查询数据条件
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
				fristTime = TimeChangeCommon.getYesterdayStartTime();
				lastTime = TimeChangeCommon.getYesterdayEndTime();
			} else if ("thisMonth".equals(fieldval)) {
				// 本月的数据
				fieldval = TimeChangeCommon.getTimesMonthmorning();
				whereTransactionValue = TimeChangeCommon.getTimesMonthnight();
				fristTime = TimeChangeCommon.getTimesMonthmorning();
				lastTime = TimeChangeCommon.getTimesMonthnight();
			} else if ("lastMonth".equals(fieldval)) {
				// 上月的数据
				fieldval = TimeChangeCommon.getLastMonthStartMorning();
				whereTransactionValue = TimeChangeCommon.getLastMonthEndMorning();
				fristTime = TimeChangeCommon.getLastMonthStartMorning();
				lastTime = TimeChangeCommon.getLastMonthEndMorning();
				// fieldval = "2017-7-14 00:00:00";
				// whereTransactionValue = "2017-7-27 00:00:00";
			} else if ("week".equals(fieldval)) {
				fieldval = TimeChangeCommon.getTimesWeekmorning();
				whereTransactionValue = TimeChangeCommon.getTimesWeeknight();
				fristTime = TimeChangeCommon.getTimesWeekmorning();
				lastTime = TimeChangeCommon.getTimesWeeknight();
			}
			if (0 != startAndEndTimeToArray.length && null != startAndEndTimeToArray) {
				fristTime = startAndEndTimeToArray[0];
				lastTime = startAndEndTimeToArray[1];
			}
			account = !"".equals(account) ? account : null;
			accountOwner = !"".equals(accountOwner) ? accountOwner : null;
			bankType = !"".equals(bankType) ? bankType : null;
			Map<String, Object> mapp = accountStatisticsService.findAccountStatistics(
					TimeChangeCommon.TimeStamp2Date(fieldval, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(whereTransactionValue, "yyyy-MM-dd HH:mm:ss"), account,
					TimeChangeCommon.TimeStamp2Date(fristTime, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(lastTime, "yyyy-MM-dd HH:mm:ss"), accountOwner, bankType, handicap,
					pageRequest);
			Page<Object> page = (Page<Object>) mapp.get("Page");
			List<Object> AccountStatisticsList = (List<Object>) page.getContent();
			List<AccountStatistics> arrlist = new ArrayList<AccountStatistics>();
			for (int i = 0; i < AccountStatisticsList.size(); i++) {
				Object[] obj = (Object[]) AccountStatisticsList.get(i);
				BizHandicap bizHandicap = handicapService.findFromCacheById(null == obj[10] ? 0 : (int) obj[10]);
				AccountStatistics AccountStatistics = new AccountStatistics();
				AccountStatistics.setId((int) obj[0]);
				AccountStatistics.setAccount((String) obj[1]);
				AccountStatistics.setBankamount(new BigDecimal(obj[7].toString()));
				AccountStatistics.setBankfee(new BigDecimal(obj[8].toString()));
				AccountStatistics.setBankcount(new Integer(String.valueOf(obj[9])));
				AccountStatistics.setTradingamount((BigDecimal) obj[5]);
				AccountStatistics.setTradingcount(new Integer(String.valueOf(obj[6])));
				AccountStatistics.setAlias((String) obj[2]);
				AccountStatistics.setOwner((String) obj[3]);
				AccountStatistics.setBanktype((String) obj[4]);
				AccountStatistics.setCounts((int) page.getTotalElements());
				AccountStatistics.setHandicapname(null == bizHandicap ? "" : bizHandicap.getName());
				arrlist.add(AccountStatistics);
			}
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("arrlist", arrlist);
			map.put("total", mapp.get("total"));
			map.put("page", new Paging(page));
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("调用出款明细账号统计Controller查询失败：" + e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 
	 * @param pageNo
	 *            当前页码
	 * @param account
	 *            账号
	 * @param startAndEndTimeToArray
	 *            时间控件的时间搓
	 * @param fieldval
	 *            时间单选按钮的值
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequiresPermissions("FinanceOutward:*")
	@RequestMapping("/accountStatisticsFromClearDate")
	public String AccountStatisticsFromClearDate(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "account", required = false) String account,
			@RequestParam(value = "accountOwner", required = false) String accountOwner,
			@RequestParam(value = "bankType", required = false) String bankType,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "fieldval", required = false) String fieldval,
			@RequestParam(value = "handicap", required = false) int handicap,
			@RequestParam(value = "cartype", required = false) String cartype,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE);
			// 拼接根据时间查询的条件(今日、本周、本月、上月)
			String whereTransactionValue = "";
			// 拼接时间戳查询数据条件
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
				fristTime = TimeChangeCommon.getYesterdayStartTime();
				lastTime = TimeChangeCommon.getYesterdayEndTime();
			} else if ("thisMonth".equals(fieldval)) {
				// 本月的数据
				fieldval = TimeChangeCommon.getTimesMonthmorning();
				whereTransactionValue = TimeChangeCommon.getTimesMonthnight();
				fristTime = TimeChangeCommon.getTimesMonthmorning();
				lastTime = TimeChangeCommon.getTimesMonthnight();
			} else if ("lastMonth".equals(fieldval)) {
				// 上月的数据
				fieldval = TimeChangeCommon.getLastMonthStartMorning();
				whereTransactionValue = TimeChangeCommon.getLastMonthEndMorning();
				fristTime = TimeChangeCommon.getLastMonthStartMorning();
				lastTime = TimeChangeCommon.getLastMonthEndMorning();
				// fieldval = "2017-7-14 00:00:00";
				// whereTransactionValue = "2017-7-27 00:00:00";
			} else if ("week".equals(fieldval)) {
				fieldval = TimeChangeCommon.getTimesWeekmorning();
				whereTransactionValue = TimeChangeCommon.getTimesWeeknight();
				fristTime = TimeChangeCommon.getTimesWeekmorning();
				lastTime = TimeChangeCommon.getTimesWeeknight();
			} else if ("lastweek".equals(fieldval)) {
				fieldval = TimeChangeCommon.getPreviousWeekday();
				whereTransactionValue = TimeChangeCommon.getTimesWeekmorningAt6();
				fristTime = TimeChangeCommon.getPreviousWeekday();
				lastTime = TimeChangeCommon.getTimesWeekmorningAt6();
			}
			if (0 != startAndEndTimeToArray.length && null != startAndEndTimeToArray) {
				fristTime = startAndEndTimeToArray[0];
				lastTime = startAndEndTimeToArray[1];
			}
			account = !"".equals(account) ? account : null;
			accountOwner = !"".equals(accountOwner) ? accountOwner : null;
			cartype = !"".equals(cartype) ? cartype : null;
			bankType = !"".equals(bankType) ? bankType : null;
			SysUser loginUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			// 获取当前用户盘口 进行权限限制
			List<Integer> handicapList = getHandicapIdByCurrentUser(loginUser.getId() == 1 ? 0 : handicap, loginUser);
			Map<String, Object> mapp = accountStatisticsService.AccountStatisticsFromClearDate(
					TimeChangeCommon.TimeStamp2Date(fieldval, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(whereTransactionValue, "yyyy-MM-dd HH:mm:ss"), account,
					TimeChangeCommon.TimeStamp2Date(fristTime, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(lastTime, "yyyy-MM-dd HH:mm:ss"), accountOwner, bankType,
					handicapList, cartype, pageRequest);
			Page<Object> page = (Page<Object>) mapp.get("Page");
			List<Object> AccountStatisticsList = (List<Object>) page.getContent();
			List<ClearDateReport> arrlist = new ArrayList<ClearDateReport>();
			for (int i = 0; i < AccountStatisticsList.size(); i++) {
				Object[] obj = (Object[]) AccountStatisticsList.get(i);
				BizHandicap bizHandicap = handicapService.findFromCacheById(null == obj[3] ? 0 : (int) obj[3]);
				ClearDateReport clearDateReport = new ClearDateReport();
				clearDateReport.setAccount((String) obj[0]);
				clearDateReport.setBankName((String) obj[1]);
				clearDateReport.setOwner((String) obj[2]);
				clearDateReport.setHandicapName(null == bizHandicap ? "" : bizHandicap.getName());
				clearDateReport.setAlias((String) obj[4]);
				clearDateReport.setBankType((String) obj[5]);
				clearDateReport.setAccountId((int) obj[6]);
				clearDateReport.setOutward(new BigDecimal(obj[8].toString()));
				clearDateReport.setFee(new BigDecimal(obj[9].toString()));
				clearDateReport.setBalance(new BigDecimal(obj[11].toString()));
				clearDateReport.setOutwardCount((int) obj[12]);
				clearDateReport.setOutwardPerson((int) obj[13]);
				clearDateReport.setFeeCount((int) obj[16]);
				clearDateReport.setLos(new BigDecimal(obj[17].toString()));
				clearDateReport.setLosCount((int) obj[18]);
				clearDateReport.setOutwardSys(new BigDecimal(obj[21].toString()));
				clearDateReport.setOutwardSysCount((int) obj[22]);
				arrlist.add(clearDateReport);
			}
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("arrlist", arrlist);
			map.put("total", mapp.get("total"));
			map.put("page", new Paging(page));
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("调用出款明细账号统计Controller查询失败：" + e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 按盘口统计
	 * 
	 * @param pageNo
	 *            当前页码
	 * @param handicap
	 *            盘口
	 * @param level
	 *            层级
	 * @param startAndEndTimeToArray
	 *            时间控件的时间搓
	 * @param fieldvalHandicap
	 *            时间单选按钮的值
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequiresPermissions("FinanceOutward:*")
	@RequestMapping("/accountstatisticshandicap")
	public String AccountStatisticsHandicap(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "handicap", required = false) int handicap,
			@RequestParam(value = "level", required = false) int level,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "fieldvalHandicap", required = false) String fieldvalHandicap,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					Sort.Direction.ASC, "handicap");
			// 拼接根据时间查询的条件(今日、本周、本月、上月)
			String whereTransactionValue = "";
			if ("today".equals(fieldvalHandicap)) {
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
				fieldvalHandicap = startTime;
				whereTransactionValue = endTime;
			} else if ("thisWeek".equals(fieldvalHandicap)) {
				// 本周的数据
				// fieldvalHandicap = getTimesWeekmorning().toString();
				// whereTransactionValue = getTimesWeeknight().toString();
				// 昨日数据
				fieldvalHandicap = TimeChangeCommon.getYesterdayStartTime();
				whereTransactionValue = TimeChangeCommon.getYesterdayEndTime();
			} else if ("thisMonth".equals(fieldvalHandicap)) {
				// 本月的数据
				fieldvalHandicap = TimeChangeCommon.getTimesMonthmorning().toString();
				whereTransactionValue = TimeChangeCommon.getTimesMonthnight().toString();
			} else if ("lastMonth".equals(fieldvalHandicap)) {
				// 上月的数据
				fieldvalHandicap = TimeChangeCommon.getLastMonthStartMorning().toString();
				whereTransactionValue = TimeChangeCommon.getLastMonthEndMorning().toString();
			}
			// 拼接时间戳查询数据条件
			String fristTime = null;
			String lastTime = null;
			if (startAndEndTimeToArray.length != 0 && startAndEndTimeToArray != null) {
				fristTime = startAndEndTimeToArray[0];
				lastTime = startAndEndTimeToArray[1];
				fieldvalHandicap = null;
				whereTransactionValue = null;
			}
			// 拼接根据盘口查询的条件
			int whereHandicap;
			if (0 != handicap) {
				whereHandicap = handicap;
			} else {
				whereHandicap = 0;
			}

			// 拼接根据层级查询的条件
			int whereLevel;
			if (0 != level) {
				whereLevel = level;
			} else {
				whereLevel = 0;
			}
			Map<String, Object> mapp = accountStatisticsService.findAccountStatisticsHandicap(whereHandicap, whereLevel,
					TimeChangeCommon.TimeStamp2Date(fristTime, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(lastTime, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(fieldvalHandicap, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(whereTransactionValue, "yyyy-MM-dd HH:mm:ss"), pageRequest);
			Page<Object> page = (Page<Object>) mapp.get("Page");
			Map<String, Object> map = new LinkedHashMap<>();
			if (null != page) {
				List<Object> FinOutHandicapList = page.getContent();
				List<FinOutHandicap> arrlist = new ArrayList<FinOutHandicap>();
				for (int i = 0; i < FinOutHandicapList.size(); i++) {
					Object[] obj = (Object[]) FinOutHandicapList.get(i);
					FinOutHandicap FinOutHandicap = new FinOutHandicap();
					FinOutHandicap.setHandicapname((String) obj[0]);
					FinOutHandicap.setHandicapno((int) obj[1]);
					if (whereHandicap == 0) {
						FinOutHandicap.setAmount((BigDecimal) obj[2]);
						FinOutHandicap.setFee((BigDecimal) obj[3]);
						FinOutHandicap.setConuts(new Integer(String.valueOf(obj[4])));
						FinOutHandicap.setLevel(0);
					} else {
						FinOutHandicap.setLevelname((String) obj[2]);
						FinOutHandicap.setLevel((int) obj[3]);
						FinOutHandicap.setAmount((BigDecimal) obj[4]);
						FinOutHandicap.setFee((BigDecimal) obj[5]);
						FinOutHandicap.setId((int) obj[6]);
						FinOutHandicap.setConuts(1);
					}
					arrlist.add(FinOutHandicap);
				}
				map.put("arrlist", arrlist);
				map.put("page", new Paging(page));
				map.put("total", mapp.get("total"));
			}
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("调用出款明细盘口统计Controller查询失败：" + e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequiresPermissions("FinanceOutward:*")
	@RequestMapping("/accountstatisticshandicapFromClearDate")
	public String AccountStatisticsHandicapFromClearDate(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "handicap", required = false) int handicap,
			@RequestParam(value = "level", required = false) int level,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "fieldvalHandicap", required = false) String fieldvalHandicap,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE_MAX);
			// 拼接根据时间查询的条件(今日、本周、本月、上月)
			String whereTransactionValue = "";
			if ("today".equals(fieldvalHandicap)) {
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
				fieldvalHandicap = startTime;
				whereTransactionValue = endTime;
			} else if ("thisWeek".equals(fieldvalHandicap)) {
				// 本周的数据
				// fieldvalHandicap = getTimesWeekmorning().toString();
				// whereTransactionValue = getTimesWeeknight().toString();
				// 昨日数据
				fieldvalHandicap = TimeChangeCommon.getYesterdayStartTime();
				whereTransactionValue = TimeChangeCommon.getYesterdayEndTime();
			} else if ("thisMonth".equals(fieldvalHandicap)) {
				// 本月的数据
				fieldvalHandicap = TimeChangeCommon.getTimesMonthmorning().toString();
				whereTransactionValue = TimeChangeCommon.getTimesMonthnight().toString();
			} else if ("lastMonth".equals(fieldvalHandicap)) {
				// 上月的数据
				fieldvalHandicap = TimeChangeCommon.getLastMonthStartMorning().toString();
				whereTransactionValue = TimeChangeCommon.getLastMonthEndMorning().toString();
			} else if ("week".equals(fieldvalHandicap)) {
				fieldvalHandicap = TimeChangeCommon.getTimesWeekmorning();
				whereTransactionValue = TimeChangeCommon.getTimesWeeknight();
			} else if ("lastweek".equals(fieldvalHandicap)) {
				fieldvalHandicap = TimeChangeCommon.getPreviousWeekday();
				whereTransactionValue = TimeChangeCommon.getTimesWeekmorningAt6();
			}
			// 拼接时间戳查询数据条件
			String fristTime = null;
			String lastTime = null;
			if (startAndEndTimeToArray.length != 0 && startAndEndTimeToArray != null) {
				fristTime = startAndEndTimeToArray[0];
				lastTime = startAndEndTimeToArray[1];
				fieldvalHandicap = fristTime;
				whereTransactionValue = lastTime;
			}
			// 拼接根据盘口查询的条件
			int whereHandicap;
			if (0 != handicap) {
				whereHandicap = handicap;
			} else {
				whereHandicap = 0;
			}

			// 拼接根据层级查询的条件
			int whereLevel;
			if (0 != level) {
				whereLevel = level;
			} else {
				whereLevel = 0;
			}
			SysUser loginUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			// 获取当前用户盘口 进行权限限制
			List<Integer> handicapList = getHandicapIdByCurrentUser(loginUser.getId() == 1 ? 0 : handicap, loginUser);
			Map<String, Object> mapp = accountStatisticsService.findAccountStatisticsHandicapFromClearDate(handicapList,
					whereLevel, TimeChangeCommon.TimeStamp2Date(fristTime, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(lastTime, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(fieldvalHandicap, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(whereTransactionValue, "yyyy-MM-dd HH:mm:ss"), pageRequest);
			// 查询根据盘口统计的出款单信息
			Map<String, Object> thirdMapp = finMoreStatStatService.findOutThird(
					TimeChangeCommon.TimeStamp2Date(fieldvalHandicap, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(whereTransactionValue, "yyyy-MM-dd HH:mm:ss"), pageRequest);
			Page<Object> page = (Page<Object>) mapp.get("Page");
			Map<String, Object> map = new LinkedHashMap<>();
			if (null != page) {
				List<Object> FinOutHandicapList = page.getContent();
				List<ClearDateReport> arrlist = new ArrayList<ClearDateReport>();
				List<Object> thirdList = (List<Object>) thirdMapp.get("data");
				for (int i = 0; i < FinOutHandicapList.size(); i++) {
					Object[] obj = (Object[]) FinOutHandicapList.get(i);
					BizHandicap bizHandicap = handicapService
							.findFromCacheById(null == obj[0] ? 0 : Integer.parseInt(obj[0].toString()));
					ClearDateReport clearDateReport = new ClearDateReport();
					clearDateReport.setHandicapId(null == obj[0] ? 0 : (int) obj[0]);
					clearDateReport.setOutwardSys(new BigDecimal((double) obj[1]));
					clearDateReport.setFee(new BigDecimal((double) obj[2]));
					clearDateReport.setOutwardSysCount(Integer.parseInt(obj[3].toString()));
					clearDateReport.setHandicapName(null == bizHandicap ? "" : bizHandicap.getName());
					for (int j = 0; j < thirdList.size(); j++) {
						Object[] Thirdobj = (Object[]) thirdList.get(j);
						if (Thirdobj[1] == bizHandicap.getId()) {
							clearDateReport.setOutwardSys(
									new BigDecimal((Thirdobj[0] == null ? "0" : Thirdobj[0].toString())));
							clearDateReport.setOutwardSysCount(
									new Integer((Thirdobj[2] == null ? "0" : Thirdobj[2].toString())));
						}

					}
					arrlist.add(clearDateReport);
				}
				map.put("arrlist", arrlist);
				map.put("page", new Paging(page));
				// map.put("total", mapp.get("total"));
			}
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("调用出款明细盘口统计Controller查询失败：" + e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 查询盘口统计>明细
	 * 
	 * @param pageNo
	 *            当前页码
	 * @param handicap
	 *            盘口
	 * @param level
	 *            层级
	 * @param member
	 *            会员名称
	 * @param startAndEndTimeToArray
	 *            时间控件数组值
	 * @param startamount
	 *            金额范围开始值
	 * @param endamount
	 *            金额范围结束值
	 * @param type
	 * @param rqhandicap
	 *            父页面传过来的盘口值
	 * @param id
	 *            盘口id
	 * @param parentstartAndEndTimeToArray
	 *            父页面传过来的时间控件数组值
	 * @param parentfieldval
	 *            父页面传过来的时间单选按钮值
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequiresPermissions("FinanceOutward:*")
	@RequestMapping("/finoutmacth")
	public String FinOutStatMacth(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "handicap", required = false) int handicap,
			@RequestParam(value = "level", required = false) int level,
			@RequestParam(value = "member", required = false) String member,
			@RequestParam(value = "startAndEndTime", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "startamount", required = false) BigDecimal startamount,
			@RequestParam(value = "endamount", required = false) BigDecimal endamount,
			@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "rqhandicap", required = false) int rqhandicap,
			@RequestParam(value = "id", required = false) int id,
			@RequestParam(value = "parentstartAndEndTimeToArray", required = false) String[] parentstartAndEndTimeToArray,
			@RequestParam(value = "parentfieldval", required = false) String parentfieldval,
			@RequestParam(value = "restatus", required = false) int restatus,
			@RequestParam(value = "tastatus", required = false) int tastatus,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					Sort.Direction.ASC, "id");
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if ("".equals(member) || "" == member) {
				member = null;
			}
			if (new BigDecimal(0) != startamount) {

			} else {
				startamount = new BigDecimal(0);
				endamount = new BigDecimal(0);
			}
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
					ptfristTime = TimeChangeCommon.getYesterdayStartTime();
					ptlastTime = TimeChangeCommon.getYesterdayEndTime();
				} else if ("thisMonth".equals(parentfieldval)) {
					// 本月的数据
					ptfristTime = TimeChangeCommon.getTimesMonthmorning().toString();
					ptlastTime = TimeChangeCommon.getTimesMonthnight().toString();
				} else if ("lastMonth".equals(parentfieldval)) {
					// 上月的数据
					ptfristTime = TimeChangeCommon.getLastMonthStartMorning().toString();
					ptlastTime = TimeChangeCommon.getLastMonthEndMorning().toString();
				}
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
			} else {
				fristTime = ptfristTime;
				lastTime = ptlastTime;
			}
			List<Integer> handicaps = new ArrayList<>();
			// 只能查询自己区域的账号
			List<SysUserProfile> manilaUserPro = sysUserProfileService.findByPropertyKey("HANDICAP_MANILA_ZONE");
			String manilaHandicap = manilaUserPro.get(0).getPropertyValue();
			List<SysUserProfile> taiWanuserPro = sysUserProfileService.findByPropertyKey("HANDICAP_TAIWAN_ZONE");
			String taiwanHandicap = taiWanuserPro.get(0).getPropertyValue();
			BizHandicap bizHandicapPro = handicapService.findFromCacheById(operator.getHandicap());
			List<Integer> handicapIds = handicapService
					.findByZone(bizHandicapPro == null ? "0" : bizHandicapPro.getId().toString());
			String handicapId = CollectionUtils.isEmpty(handicapIds) ? "0" : handicapIds.get(0).toString();
			if (ArrayUtils.contains(manilaHandicap.substring(1, manilaHandicap.length()).split(";"),
					(operator.getCategory() > 400 ? (operator.getCategory() - 400) + "" : handicapId))) {
				String[] handicapStr = manilaHandicap.substring(1, manilaHandicap.length()).split(";");
				for (String hd : handicapStr) {
					handicaps.add(Integer.valueOf(hd));
				}
			}
			if (ArrayUtils.contains(taiwanHandicap.substring(1, taiwanHandicap.length()).split(";"),
					(operator.getCategory() > 400 ? (operator.getCategory() - 400) + "" : handicapId))) {
				String[] handicapStr = taiwanHandicap.substring(1, taiwanHandicap.length()).split(";");
				for (String hd : handicapStr) {
					handicaps.add(Integer.valueOf(hd));
				}
			}
			Map<String, Object> mapp = accountStatisticsService.findFinOutStatMatch(handicap, level, member,
					TimeChangeCommon.TimeStamp2Date(fristTime, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(lastTime, "yyyy-MM-dd HH:mm:ss"), startamount, endamount, type,
					rqhandicap, id, restatus, tastatus, handicaps, pageRequest);
			Page<Object> page = (Page<Object>) mapp.get("Page");
			if (null != page) {
				List<Object> FinOutMatchList = page.getContent();
				List<FinOutMatch> arrlist = new ArrayList<FinOutMatch>();
				BigDecimal bankAmounts = BigDecimal.ZERO;
				for (int i = 0; i < FinOutMatchList.size(); i++) {
					Object[] obj = (Object[]) FinOutMatchList.get(i);
					BizHandicap bizHandicap = handicapService.findFromCacheById((Integer) obj[0]);
					SysUser sysUser = sysUserService.findFromCacheById((Integer) obj[7]);
					AccountBaseInfo bizAccount = accountService.getFromCacheById((Integer) obj[13]);
					FinOutMatch FinOutMatch = new FinOutMatch();
					FinOutMatch.setHandicapname(bizHandicap.getName());
					FinOutMatch.setHandicappno((int) obj[0]);
					FinOutMatch.setMember((String) obj[1]);
					FinOutMatch.setOrderno((String) obj[2]);
					FinOutMatch.setCreatetime((String) obj[3]);
					FinOutMatch.setAmounts((BigDecimal) obj[5]);
					FinOutMatch.setAmount((BigDecimal) obj[6]);
					FinOutMatch.setOperator(null == sysUser ? null : sysUser.getUsername());
					if (null != obj[15]) {
						FinOutMatch.setToaccount((String) obj[8]);
						FinOutMatch.setToaccountowner((String) obj[9]);
						FinOutMatch.setBankamount((BigDecimal) obj[6]);
						FinOutMatch.setBankcreatime((String) obj[16]);
						bankAmounts = bankAmounts.add((BigDecimal) obj[6]);
					} else {
						FinOutMatch.setToaccount("");
						FinOutMatch.setToaccountowner("");
					}
					FinOutMatch.setFromaccount(null == bizAccount ? "" : bizAccount.getAccount());
					FinOutMatch.setFromaccountid(null == obj[13] ? 0 : (Integer) obj[13]);
					FinOutMatch.setRestatus((int) obj[10]);
					FinOutMatch.setTastatus((int) obj[11]);
					FinOutMatch.setUpdatetime((String) obj[4]);
					FinOutMatch.setOwner(null == bizAccount ? "" : bizAccount.getOwner());
					FinOutMatch.setBanktype(null == bizAccount ? "" : bizAccount.getBankType());
					FinOutMatch.setAlias(null == bizAccount ? "" : bizAccount.getAlias());
					arrlist.add(FinOutMatch);
				}
				Map<String, Object> map = new LinkedHashMap<>();
				map.put("arrlist", arrlist);
				map.put("page", new Paging(page));
				map.put("total", mapp.get("total"));
				map.put("total1", mapp.get("total1"));
				map.put("bankAmounts", bankAmounts);
				responseData.setData(map);
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("调用出款明细盘口统计>明细Controller查询失败：" + e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 查询系统明细
	 * 
	 * @param pageNo
	 *            页码
	 * @param accountid
	 *            账号id
	 * @param account
	 *            账号
	 * @param parentstartAndEndTimeToArray
	 *            父页面时间控件数组值
	 * @param parentfieldval
	 *            父页面时间单选按钮值
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequiresPermissions("FinanceOutward:*")
	@RequestMapping("/finoutstatsys")
	public String FinOutStatSys(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "accountid", required = false) int accountid,
			@RequestParam(value = "account", required = false) String account,
			@RequestParam(value = "parentstartAndEndTimeToArray", required = false) String[] parentstartAndEndTimeToArray,
			@RequestParam(value = "parentfieldval", required = false) String parentfieldval,
			@RequestParam(value = "startamount", required = false) BigDecimal startamount,
			@RequestParam(value = "endamount", required = false) BigDecimal endamount,
			@RequestParam(value = "restatus", required = false) int restatus,
			@RequestParam(value = "tastatus", required = false) int tastatus,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					Sort.Direction.ASC, "amount");
			if ("".equals(account) || "" == account) {
				account = null;
			}
			// 根据父页面的时间去查询对应的数据
			// 拼接父页面的时间
			String ptfristTime = null;
			String ptlastTime = null;
			if (0 != parentstartAndEndTimeToArray.length && null != parentstartAndEndTimeToArray) {
				ptfristTime = parentstartAndEndTimeToArray[0];
				ptlastTime = parentstartAndEndTimeToArray[1];
			} else {
				if ("today".equals(parentfieldval)) {
					// 今天数据
					Date nowTime = new Date(System.currentTimeMillis());
					SimpleDateFormat sdFormatter = new SimpleDateFormat("yyyy-MM-dd");
					String retStrFormatNowDate = sdFormatter.format(nowTime);
					String startTime = retStrFormatNowDate + " 00:00:00";
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
					ptfristTime = TimeChangeCommon.getYesterdayStartTime();
					ptlastTime = TimeChangeCommon.getYesterdayEndTime();
				} else if ("thisMonth".equals(parentfieldval)) {
					// 本月的数据
					ptfristTime = TimeChangeCommon.getTimesMonthmorning().toString();
					ptlastTime = TimeChangeCommon.getTimesMonthnight().toString();
				} else if ("lastMonth".equals(parentfieldval)) {
					// 上月的数据
					ptfristTime = TimeChangeCommon.getLastMonthStartMorning().toString();
					ptlastTime = TimeChangeCommon.getLastMonthEndMorning().toString();
				}
			}
			if (new BigDecimal(0) == startamount) {
				startamount = new BigDecimal(0);
				endamount = new BigDecimal(0);
			}
			Map<String, Object> mapp = accountStatisticsService.findFinOutStatSys(accountid, account,
					TimeChangeCommon.TimeStamp2Date(ptfristTime, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(ptlastTime, "yyyy-MM-dd HH:mm:ss"), startamount, endamount,
					restatus, tastatus, pageRequest);
			Page<Object> page = (Page<Object>) mapp.get("Page");
			if (null != page) {
				List<Object> FinOutStatSysList = page.getContent();
				List<FinOutStatSys> arrlist = new ArrayList<FinOutStatSys>();
				for (int i = 0; i < FinOutStatSysList.size(); i++) {
					Object[] obj = (Object[]) FinOutStatSysList.get(i);
					FinOutStatSys FinOutStatSys = new FinOutStatSys();
					FinOutStatSys.setHandicapname((String) obj[0]);
					FinOutStatSys.setLevelname((String) obj[1]);
					FinOutStatSys.setMember((String) obj[2]);
					FinOutStatSys.setAccountname((String) obj[3]);
					FinOutStatSys.setToaccount((String) obj[4]);
					FinOutStatSys.setAmount((BigDecimal) obj[5]);
					FinOutStatSys.setFee((BigDecimal) obj[6]);
					FinOutStatSys.setOperatorname((String) obj[7]);
					FinOutStatSys.setAsigntime((String) obj[8]);
					FinOutStatSys.setOrderno((String) obj[9]);
					FinOutStatSys.setAccountowner((String) obj[10]);
					FinOutStatSys.setRestatus((int) obj[11]);
					FinOutStatSys.setTastatus((int) obj[12]);
					arrlist.add(FinOutStatSys);
				}
				Map<String, Object> map = new LinkedHashMap<>();
				map.put("arrlist", arrlist);
				map.put("page", new Paging(page));
				map.put("total", mapp.get("total"));
				responseData.setData(map);
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("调用出款明细账号统计>系统明细Controller查询失败：" + e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 查询银行明细
	 * 
	 * @param pageNo
	 *            页码
	 * @param accountid
	 *            账号id
	 * @param toaccountowner
	 *            开户人
	 * @param toaccount
	 *            收款账号
	 * @param parentstartAndEndTimeToArray
	 *            父页面时间控件数组值
	 * @param parentfieldval
	 *            父页面时间单选按钮值
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequiresPermissions("FinanceOutward:*")
	@RequestMapping("/finoutstatflow")
	public String FinOutStatFlow(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "accountid", required = false) int accountid,
			@RequestParam(value = "toaccountowner", required = false) String toaccountowner,
			@RequestParam(value = "toaccount", required = false) String toaccount,
			@RequestParam(value = "parentstartAndEndTimeToArray", required = false) String[] parentstartAndEndTimeToArray,
			@RequestParam(value = "parentfieldval", required = false) String parentfieldval,
			@RequestParam(value = "startamount", required = false) BigDecimal startamount,
			@RequestParam(value = "endamount", required = false) BigDecimal endamount,
			@RequestParam(value = "bkstatus", required = false) int bkstatus,
			@RequestParam(value = "typestatus", required = false) int typestatus,
			@RequestParam(value = "descOrAsc", required = false) String descOrAsc,
			@RequestParam(value = "orderBy", required = false) String orderBy,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					descOrAsc.equals("desc") ? Sort.Direction.DESC : Sort.Direction.ASC,
					orderBy.equals("tradingtime") ? "trading_time" : "create_time");
			if ("".equals(toaccountowner) || "" == toaccountowner) {
				toaccountowner = null;
			}
			if ("".equals(toaccount) || "" == toaccount) {
				toaccount = null;
			}
			// 根据父页面的时间去查询对应的数据
			// 拼接父页面的时间
			String ptfristTime = null;
			String ptlastTime = null;
			if (0 != parentstartAndEndTimeToArray.length && null != parentstartAndEndTimeToArray) {
				ptfristTime = parentstartAndEndTimeToArray[0];
				ptlastTime = parentstartAndEndTimeToArray[1];
			} else {
				if ("today".equals(parentfieldval)) {
					// 今天数据
					Date nowTime = new Date(System.currentTimeMillis());
					SimpleDateFormat sdFormatter = new SimpleDateFormat("yyyy-MM-dd");
					String retStrFormatNowDate = sdFormatter.format(nowTime);
					String startTime = retStrFormatNowDate + " 00:00:00";
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
					ptfristTime = TimeChangeCommon.getYesterdayStartTime();
					ptlastTime = TimeChangeCommon.getYesterdayEndTime();
				} else if ("thisMonth".equals(parentfieldval)) {
					// 本月的数据
					ptfristTime = TimeChangeCommon.getTimesMonthmorning().toString();
					ptlastTime = TimeChangeCommon.getTimesMonthnight().toString();
				} else if ("lastMonth".equals(parentfieldval)) {
					// 上月的数据
					ptfristTime = TimeChangeCommon.getLastMonthStartMorning().toString();
					ptlastTime = TimeChangeCommon.getLastMonthEndMorning().toString();
				}
			}
			Map<String, Object> mapp = accountStatisticsService.findFinOutStatFlow(accountid, toaccountowner, toaccount,
					TimeChangeCommon.TimeStamp2Date(ptfristTime, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(ptlastTime, "yyyy-MM-dd HH:mm:ss"), startamount, endamount,
					bkstatus, typestatus, pageRequest);
			Page<Object> page = (Page<Object>) mapp.get("Page");
			if (null != page) {
				List<Object> FinOutStatFlowList = page.getContent();
				List<FinOutStatFlow> arrlist = new ArrayList<FinOutStatFlow>();
				for (int i = 0; i < FinOutStatFlowList.size(); i++) {
					Object[] obj = (Object[]) FinOutStatFlowList.get(i);
					FinOutStatFlow FinOutStatFlow = new FinOutStatFlow();
					FinOutStatFlow.setAccount((String) obj[0]);
					FinOutStatFlow.setToaccount((String) obj[1]);
					FinOutStatFlow.setAmount((BigDecimal) obj[2]);
					FinOutStatFlow.setFee((BigDecimal) obj[3]);
					// FinOutStatFlow.setPaycode((String) obj[4]);
					FinOutStatFlow.setType(3);
					FinOutStatFlow.setTransactionno((String) obj[6]);
					FinOutStatFlow.setToaccountowner((String) obj[7]);
					FinOutStatFlow.setTradingtime((String) obj[8]);
					FinOutStatFlow.setId((int) obj[9]);
					FinOutStatFlow.setStatus((int) obj[4]);
					FinOutStatFlow.setBalance((BigDecimal) obj[11]);
					FinOutStatFlow.setRemark((String) obj[6]);
					FinOutStatFlow.setSummary((String) obj[12]);
					FinOutStatFlow.setCreatetime((String) obj[10]);
					arrlist.add(FinOutStatFlow);
				}
				Map<String, Object> map = new LinkedHashMap<>();
				map.put("arrlist", arrlist);
				map.put("page", new Paging(page));
				map.put("total", mapp.get("total"));
				responseData.setData(map);
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("调用出款明细账号统计>银行明细Controller查询失败：" + e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 查询银行明细>详情
	 * 
	 * @param id
	 *            收款账号id
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequiresPermissions("FinanceOutward:*")
	@RequestMapping("/finoutstatflowdetails")
	public String FinOutStatFlowDetails(@RequestParam(value = "id", required = false) int id)
			throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(0, AppConstants.PAGE_SIZE, Sort.Direction.ASC, "id");
			Page<Object> page = accountStatisticsService.findFinOutStatFlowDetails(id, pageRequest);
			List<Object> FinOutStatFlowDetailsList = page.getContent();
			List<FinOutStatFlowDeTaiIs> arrlist = new ArrayList<FinOutStatFlowDeTaiIs>();
			// 判断是否有数据
			if (FinOutStatFlowDetailsList.size() > 0) {
				Object[] obj = (Object[]) FinOutStatFlowDetailsList.get(0);
				FinOutStatFlowDeTaiIs details = new FinOutStatFlowDeTaiIs();
				details.setHandicapname((String) obj[0]);
				details.setLevelname((String) obj[1]);
				details.setMember((String) obj[2]);
				details.setToaccount((String) obj[3]);
				details.setToaccountname((String) obj[4]);
				details.setToaccountowner((String) obj[5]);
				details.setAtoaccount((String) obj[6]);
				details.setAmount((BigDecimal) obj[7]);
				details.setFee((BigDecimal) obj[8]);
				details.setTradingtime((String) obj[9]);
				details.setType((int) obj[10]);
				details.setDamount((BigDecimal) obj[11]);
				details.setBfee((BigDecimal) obj[12]);
				details.setAsigntime((String) obj[13]);
				details.setOperatorname((String) obj[14]);
				details.setComfirmor((String) obj[15]);
				details.setAccountname((String) obj[16]);
				details.setBankname((String) obj[17]);
				details.setOwner((String) obj[18]);
				arrlist.add(details);
			}
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("arrlist", arrlist);
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("调用出款明细账号统计>银行明细>详情Controller查询失败：" + e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/findbyhandicapid")
	public String listForIncomeAudit(@RequestParam(value = "handicap", required = false) int handicap)
			throws JsonProcessingException {
		try {
			GeneralResponseData<List<BizLevel>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			List<BizLevel> dataToList = levelService.findByHandicapId(handicap);
			if (!CollectionUtils.isEmpty(dataToList)) {
				dataToList = dataToList.stream().filter((p) -> p.getStatus() == 1).collect(Collectors.toList());
			}
			responseData.setData(dataToList);
			return mapper.writeValueAsString(responseData);
		} catch (JsonProcessingException e) {
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/getUserByUid")
	public String getUserByUid(@RequestParam(value = "uid", required = false) int uid) throws JsonProcessingException {
		try {
			BizAccountMore accMore = accountMoreService.getFromByUid(String.valueOf(uid));
			String json = mapper.writeValueAsString(accMore);
			return json;
		} catch (JsonProcessingException e) {
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/getLimitAckToRedis")
	public String getLimitAckToRedis(@RequestParam(value = "id", required = false) String id)
			throws JsonProcessingException {
		return redisService.getString("limitAck" + id);
	}

	@RequestMapping("/setLimitAckToRedis")
	public String setLimitAckToRedis(@RequestParam(value = "id", required = false) String id)
			throws JsonProcessingException {
		redisSer.setString("limitAck" + id, id);
		return "成功！";
	}

	// 向返利网确认额度
	@RequestMapping("/limitAck")
	public String limitAck(@RequestParam(value = "account", required = false) String account,
			@RequestParam(value = "amount", required = false) BigDecimal amount) throws JsonProcessingException {
		boolean flag = rebateApiService.ackCreditLimit(account, amount, null);
		if (flag)
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功  "));
		else
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  "));
	}

	// 修改more表可提现金额
	@RequestMapping("/addBalance")
	public String addBalance(@RequestParam(value = "id", required = false) Integer id,
			@RequestParam(value = "amount", required = false) BigDecimal amount) throws JsonProcessingException {
		accountMoreService.updateBalance(id, amount);
		return mapper.writeValueAsString(
				new GeneralResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功  "));
	}

	// 修改more表margin
	@RequestMapping("/updateMargin")
	public String updateMargin(@RequestParam(value = "id", required = false) Integer id,
			@RequestParam(value = "margin", required = false) BigDecimal margin) throws JsonProcessingException {
		accountMoreService.updateMargin(id, margin);
		return mapper.writeValueAsString(
				new GeneralResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功  "));
	}

	// 根据手机号查询more表信息
	@RequestMapping("/findUidByphone")
	public String findUidByaccountId(@RequestParam(value = "phone", required = false) String phone)
			throws JsonProcessingException {
		BizAccountMore more = accountMoreService.findByMobile(phone);
		return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
				"操作成功  " + more.getUid()));
	}

	// 修改more表accounts
	@RequestMapping("/updateAccountsByid")
	public String updateAccountsByid(@RequestParam(value = "id", required = false) Integer id,
			@RequestParam(value = "accounts", required = false) String accounts) throws JsonProcessingException {
		accountMoreService.updateAccountsByid(id, accounts);
		return mapper.writeValueAsString(
				new GeneralResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功  "));
	}

	// 修改账号表peakBalance
	@RequestMapping("/updatePeakBalanceByid")
	public String updatePeakBalanceByid(@RequestParam(value = "id", required = false) Integer id,
			@RequestParam(value = "amount", required = false) Integer amount)
			throws JsonProcessingException, IllegalAccessException, InvocationTargetException {
		BizAccount db = accountService.getById(id);
		db.setPeakBalance(amount);
		log.debug("updatePeakBalanceByid>> id {}",db.getId());
		accountService.broadCast(db);
		hostMonitorService.update(db);
		cabanaService.updAcc(db.getId());
		return mapper.writeValueAsString(
				new GeneralResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功  "));
	}

}
