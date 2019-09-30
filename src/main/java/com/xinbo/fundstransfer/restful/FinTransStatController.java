package com.xinbo.fundstransfer.restful;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.component.common.TimeChangeCommon;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.FinTransStatService;
import com.xinbo.fundstransfer.service.HandicapService;
import com.xinbo.fundstransfer.service.SysDataPermissionService;
import com.xinbo.fundstransfer.service.SysUserProfileService;
import com.xinbo.fundstransfer.domain.entity.AccountStatistics;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.BizIncomeRequest;
import com.xinbo.fundstransfer.domain.entity.ClearDateReport;
import com.xinbo.fundstransfer.domain.entity.FinTransStatMatch;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.entity.SysUserProfile;
import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import java.util.Map;

/**
 * 中转明细请求接口
 * 
 * @author Steven
 *
 */
@Slf4j
@RestController
@RequestMapping("/r/fintransstat")
public class FinTransStatController extends BaseController {
	@Autowired
	private FinTransStatService finTransStatService;
	@Autowired
	private HandicapService handicapService;
	@Autowired
	private AccountService accountService;
	@Autowired
	private ObjectMapper mapper = new ObjectMapper();
	@Autowired
	private SysDataPermissionService sysDataPermissionService;
	@Autowired
	private SysUserProfileService sysUserProfileService;

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
	 * @param account
	 *            汇入账号
	 * @param startAndEndTimeToArray
	 *            时间控件数组值
	 * @param fieldval
	 *            时间单选按钮值
	 * @param type
	 *            标识查询哪 个数据类型（入款银行卡中转、支付宝、微信、第三方）
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequiresPermissions("FinTransStat:*")
	@RequestMapping("/fintransstatdeal")
	public String AccountStatistics(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "account", required = false) String account,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "fieldval", required = false) String fieldval,
			@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "accountOwner", required = false) String accountOwner,
			@RequestParam(value = "bankType", required = false) String bankType,
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
				String startTimee = retStrFormatNowDate + " 07:00:00";
				Calendar c = Calendar.getInstance();
				c.setTime(nowTime);
				c.add(Calendar.DAY_OF_MONTH, 1);// 今天+1天
				Date tomorrow = c.getTime();
				String endTimee = sdFormatter.format(tomorrow) + " 06:59:59";
				fieldval = startTimee;
				whereTransactionValue = endTimee;
				fristTime = startTimee;
				lastTime = endTimee;
			} else if ("thisWeek".equals(fieldval)) {
				// 本周的数据
				// fieldval = getTimesWeekmorning().toString();
				// whereTransactionValue = getTimesWeeknight().toString();
				// 昨日数据
				fieldval = TimeChangeCommon.getYesterdayStartTime();
				whereTransactionValue = TimeChangeCommon.getYesterdayEndTime();
				fristTime = TimeChangeCommon.getYesterdayStartTime();
				;
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
			if (0 != startAndEndTimeToArray.length && null != startAndEndTimeToArray) {
				fristTime = startAndEndTimeToArray[0];
				lastTime = startAndEndTimeToArray[1];
				fieldval = null;
				whereTransactionValue = null;
			}
			account = !"".equals(account) ? account : null;
			accountOwner = !"".equals(accountOwner) ? accountOwner : null;
			bankType = !"".equals(bankType) ? bankType : null;
			Map<String, Object> mapp = finTransStatService.findFinTransStat(account,
					TimeChangeCommon.TimeStamp2Date(fristTime, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(lastTime, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(fieldval, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(whereTransactionValue, "yyyy-MM-dd HH:mm:ss"), type, accountOwner,
					bankType, handicap, pageRequest);
			Map<String, Object> map = new LinkedHashMap<>();
			// 银行数据
			List<AccountStatistics> arrlist = new ArrayList<AccountStatistics>();
			Page<Object> BankcardPage = (Page<Object>) mapp.get("BankcardPage");
			if (null != BankcardPage) {
				List<Object> AccountStatisticsList = BankcardPage.getContent();
				for (int i = 0; i < AccountStatisticsList.size(); i++) {
					Object[] obj = (Object[]) AccountStatisticsList.get(i);
					AccountStatistics AccountStatistics = new AccountStatistics();
					AccountStatistics.setAccount((String) obj[0]);
					AccountStatistics.setType((int) obj[1]);
					AccountStatistics.setId((int) obj[2]);
					AccountStatistics.setBankamount((BigDecimal) obj[9]);
					AccountStatistics.setBankfee((BigDecimal) obj[10]);
					AccountStatistics.setBankcount(new Integer(String.valueOf(obj[11])));
					AccountStatistics.setTradingamount((BigDecimal) obj[6]);
					AccountStatistics.setTradingfee((BigDecimal) obj[7]);
					AccountStatistics.setTradingcount(new Integer(String.valueOf(obj[8])));
					AccountStatistics.setAlias(String.valueOf(obj[3]));
					AccountStatistics.setOwner(String.valueOf(obj[4]));
					AccountStatistics.setBanktype(String.valueOf(obj[5]));
					AccountStatistics.setHandicapname(
							(handicapService.findFromCacheById(new Integer(String.valueOf(obj[12])))) == null ? ""
									: handicapService.findFromCacheById(new Integer(String.valueOf(obj[12])))
											.getName());
					arrlist.add(AccountStatistics);
				}
				map.put("Bankcardtotal", mapp.get("Bankcardtotal"));
				map.put("Bankcardpage", new Paging(BankcardPage));
			}
			map.put("Bankcardarrlist", arrlist);

			// 微信数据
			List<AccountStatistics> WeChatarrlist = new ArrayList<AccountStatistics>();
			Page<Object> WeChatPage = (Page<Object>) mapp.get("WeChatPage");
			if (null != WeChatPage) {
				List<Object> AccountStatisticsList = WeChatPage.getContent();
				for (int i = 0; i < AccountStatisticsList.size(); i++) {
					Object[] obj = (Object[]) AccountStatisticsList.get(i);
					AccountStatistics AccountStatistics = new AccountStatistics();
					AccountStatistics.setAccount((String) obj[0]);
					AccountStatistics.setType((int) obj[1]);
					AccountStatistics.setId((int) obj[2]);
					AccountStatistics.setBankamount((BigDecimal) obj[9]);
					AccountStatistics.setBankfee((BigDecimal) obj[10]);
					AccountStatistics.setBankcount(new Integer(String.valueOf(obj[11])));
					AccountStatistics.setTradingamount((BigDecimal) obj[6]);
					AccountStatistics.setTradingfee((BigDecimal) obj[7]);
					AccountStatistics.setTradingcount(new Integer(String.valueOf(obj[8])));
					WeChatarrlist.add(AccountStatistics);
				}
				map.put("WeChatpage", new Paging(WeChatPage));
				map.put("WeChattotal", mapp.get("WeChattotal"));
			}
			map.put("WeChatarrlist", WeChatarrlist);

			// 支付宝数据
			List<AccountStatistics> Paytreasurearrlist = new ArrayList<AccountStatistics>();
			Page<Object> PaytreasurePage = (Page<Object>) mapp.get("PaytreasurePage");
			if (null != PaytreasurePage) {
				List<Object> AccountStatisticsList = PaytreasurePage.getContent();
				for (int i = 0; i < AccountStatisticsList.size(); i++) {
					Object[] obj = (Object[]) AccountStatisticsList.get(i);
					AccountStatistics AccountStatistics = new AccountStatistics();
					AccountStatistics.setAccount((String) obj[0]);
					AccountStatistics.setType((int) obj[1]);
					AccountStatistics.setId((int) obj[2]);
					AccountStatistics.setBankamount((BigDecimal) obj[9]);
					AccountStatistics.setBankfee((BigDecimal) obj[10]);
					AccountStatistics.setBankcount(new Integer(String.valueOf(obj[11])));
					AccountStatistics.setTradingamount((BigDecimal) obj[6]);
					AccountStatistics.setTradingfee((BigDecimal) obj[7]);
					AccountStatistics.setTradingcount(new Integer(String.valueOf(obj[8])));
					Paytreasurearrlist.add(AccountStatistics);
				}
				map.put("Paytreasuretotal", mapp.get("Paytreasuretotal"));
				map.put("PaytreasurePage", new Paging(PaytreasurePage));
			}
			map.put("Paytreasurearrlist", Paytreasurearrlist);

			// 第三方数据
			List<AccountStatistics> thirdpartyarrlist = new ArrayList<AccountStatistics>();
			Page<Object> thirdpartyPage = (Page<Object>) mapp.get("thirdpartyPage");
			if (null != thirdpartyPage) {
				List<Object> AccountStatisticsList = thirdpartyPage.getContent();
				for (int i = 0; i < AccountStatisticsList.size(); i++) {
					Object[] obj = (Object[]) AccountStatisticsList.get(i);
					AccountStatistics AccountStatistics = new AccountStatistics();
					AccountStatistics.setAccount((String) obj[0]);
					AccountStatistics.setType((int) obj[1]);
					AccountStatistics.setId((int) obj[2]);
					AccountStatistics.setBankamount((BigDecimal) obj[9]);
					AccountStatistics.setBankfee((BigDecimal) obj[10]);
					AccountStatistics.setBankcount(new Integer(String.valueOf(obj[11])));
					AccountStatistics.setTradingamount((BigDecimal) obj[6]);
					AccountStatistics.setTradingfee((BigDecimal) obj[7]);
					AccountStatistics.setTradingcount(new Integer(String.valueOf(obj[8])));
					AccountStatistics.setAlias(String.valueOf(obj[3]));
					AccountStatistics.setOwner(String.valueOf(obj[4]));
					AccountStatistics.setBanktype(String.valueOf(obj[5]));
					thirdpartyarrlist.add(AccountStatistics);
				}
				map.put("thirdpartytotal", mapp.get("thirdpartytotal"));
				map.put("thirdpartyPage", new Paging(thirdpartyPage));
			}
			map.put("thirdpartyarrlist", thirdpartyarrlist);

			// 下发卡数据
			List<AccountStatistics> Thesenderarrlist = new ArrayList<AccountStatistics>();
			Page<Object> ThesenderPage = (Page<Object>) mapp.get("ThesenderPage");
			if (null != ThesenderPage) {
				List<Object> AccountStatisticsList = ThesenderPage.getContent();
				for (int i = 0; i < AccountStatisticsList.size(); i++) {
					Object[] obj = (Object[]) AccountStatisticsList.get(i);
					AccountStatistics AccountStatistics = new AccountStatistics();
					AccountStatistics.setAccount((String) obj[0]);
					AccountStatistics.setType((int) obj[1]);
					AccountStatistics.setId((int) obj[2]);
					AccountStatistics.setBankamount((BigDecimal) obj[9]);
					AccountStatistics.setBankfee((BigDecimal) obj[10]);
					AccountStatistics.setBankcount(new Integer(String.valueOf(obj[11])));
					AccountStatistics.setTradingamount((BigDecimal) obj[6]);
					AccountStatistics.setTradingfee((BigDecimal) obj[7]);
					AccountStatistics.setTradingcount(new Integer(String.valueOf(obj[8])));
					Thesenderarrlist.add(AccountStatistics);
				}
				map.put("Thesendertotal", mapp.get("Thesendertotal"));
				map.put("ThesenderPage", new Paging(ThesenderPage));
			}
			map.put("Thesenderarrlist", Thesenderarrlist);

			// 第三方中转
			List<AccountStatistics> thirdlist = new ArrayList<AccountStatistics>();
			Page<Object> thirdPage = (Page<Object>) mapp.get("thirdPage");
			if (null != thirdPage) {
				List<Object> AccountStatisticsList = thirdPage.getContent();
				for (int i = 0; i < AccountStatisticsList.size(); i++) {
					Object[] obj = (Object[]) AccountStatisticsList.get(i);
					AccountStatistics AccountStatistics = new AccountStatistics();
					AccountStatistics.setHandicapname(
							handicapService.findFromCacheById(new Integer(String.valueOf(obj[9]))).getName());
					AccountStatistics.setAccount((String) obj[0]);
					AccountStatistics.setType((int) obj[1]);
					AccountStatistics.setId((int) obj[2]);
					AccountStatistics.setTradingamount((BigDecimal) obj[6]);
					AccountStatistics.setTradingfee((BigDecimal) obj[7]);
					AccountStatistics.setTradingcount(new Integer(String.valueOf(obj[8])));
					AccountStatistics.setAlias(String.valueOf(obj[3]));
					AccountStatistics.setOwner(String.valueOf(obj[4]));
					AccountStatistics.setBanktype(String.valueOf(obj[5]));
					AccountStatistics.setBankname(String.valueOf(obj[10]));
					thirdlist.add(AccountStatistics);
				}
				map.put("thirdtotal", mapp.get("thirdtotal"));
				map.put("thirdPage", new Paging(thirdPage));
			}
			map.put("thirdlist", thirdlist);

			// 中转隔天排查
			List<BizIncomeRequest> screeninglist = new ArrayList<BizIncomeRequest>();
			Page<Object> screeningPage = (Page<Object>) mapp.get("screeningPage");
			if (null != screeningPage) {
				List<Object> AccountStatisticsList = screeningPage.getContent();
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				for (int i = 0; i < AccountStatisticsList.size(); i++) {
					Object[] obj = (Object[]) AccountStatisticsList.get(i);
					BizIncomeRequest income = new BizIncomeRequest();
					AccountBaseInfo fromAccount = accountService.getFromCacheById((Integer) obj[1]);
					AccountBaseInfo toAccount = accountService.getFromCacheById((Integer) obj[2]);
					income.setId(Long.parseLong(obj[0].toString()));
					income.setFromId((int) obj[1]);
					income.setToId((int) obj[2]);
					income.setAmount((BigDecimal) obj[3]);
					income.setOrderNo((String) obj[4]);
					income.setStatus((int) obj[5]);
					income.setFromBankType(obj[6].toString());
					income.setFromOwner(obj[7] == null ? "" : (String) obj[7]);
					income.setFromAlias(fromAccount.getAlias());
					income.setToAlias(toAccount.getAlias());
					screeninglist.add(income);
				}
				map.put("screeningtotal", mapp.get("screeningtotal"));
				map.put("screeningPage", new Paging(screeningPage));
			}
			map.put("screeninglist", screeninglist);

			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("中转明细controller查询失败" + e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequiresPermissions("FinTransStat:*")
	@RequestMapping("/fintransstatdealFromClearDate")
	public String AccountStatisticsFromClearDate(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "account", required = false) String account,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "fieldval", required = false) String fieldval,
			@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "accountOwner", required = false) String accountOwner,
			@RequestParam(value = "bankType", required = false) String bankType,
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
				String startTimee = retStrFormatNowDate + " 07:00:00";
				Calendar c = Calendar.getInstance();
				c.setTime(nowTime);
				c.add(Calendar.DAY_OF_MONTH, 1);// 今天+1天
				Date tomorrow = c.getTime();
				String endTimee = sdFormatter.format(tomorrow) + " 06:59:59";
				fieldval = startTimee;
				whereTransactionValue = endTimee;
				fristTime = startTimee;
				lastTime = endTimee;
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
			if (0 != startAndEndTimeToArray.length && null != startAndEndTimeToArray) {
				fristTime = startAndEndTimeToArray[0];
				lastTime = startAndEndTimeToArray[1];
				fieldval = null;
				whereTransactionValue = null;
			}
			account = !"".equals(account) ? account : null;
			accountOwner = !"".equals(accountOwner) ? accountOwner : null;
			bankType = !"".equals(bankType) ? bankType : null;
			// 获取当前用户盘口 进行权限限制
			SysUser loginUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			List<Integer> handicapList = getHandicapIdByCurrentUser(loginUser.getId() == 1 ? 0 : handicap, loginUser);
			Map<String, Object> mapp = finTransStatService.findFinTransStatFromClearDate(account,
					TimeChangeCommon.TimeStamp2Date(fristTime, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(lastTime, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(fieldval, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(whereTransactionValue, "yyyy-MM-dd HH:mm:ss"), type, accountOwner,
					bankType, handicapList, pageRequest);
			Map<String, Object> map = new LinkedHashMap<>();
			// 银行数据
			List<ClearDateReport> arrlist = new ArrayList<ClearDateReport>();
			Page<Object> BankcardPage = (Page<Object>) mapp.get("BankcardPage");
			if (null != BankcardPage) {
				List<Object> AccountStatisticsList = BankcardPage.getContent();
				for (int i = 0; i < AccountStatisticsList.size(); i++) {
					Object[] obj = (Object[]) AccountStatisticsList.get(i);
					BizHandicap bizHandicap = handicapService.findFromCacheById(null == obj[0] ? 0 : (int) obj[0]);
					ClearDateReport clearDateReport = new ClearDateReport();
					clearDateReport.setHandicapId((int) obj[0]);
					clearDateReport.setAccount((String) obj[1]);
					clearDateReport.setAccountId((int) obj[2]);
					clearDateReport.setAlias((String) obj[3]);
					clearDateReport.setOwner((String) obj[4]);
					clearDateReport.setBankType((String) obj[5]);
					clearDateReport.setBalance(new BigDecimal(obj[6].toString()));
					clearDateReport.setOutward(new BigDecimal(obj[7].toString()));
					clearDateReport.setFee(new BigDecimal(obj[8].toString()));
					clearDateReport.setOutwardCount((int) obj[9]);
					clearDateReport.setOutwardSys(new BigDecimal(obj[10].toString()));
					clearDateReport.setOutwardSysCount((int) obj[11]);
					clearDateReport.setHandicapName(bizHandicap.getName());
					arrlist.add(clearDateReport);
				}
				map.put("Bankcardtotal", mapp.get("Bankcardtotal"));
				map.put("Bankcardpage", new Paging(BankcardPage));
			}
			map.put("Bankcardarrlist", arrlist);

			// 下发卡数据
			List<ClearDateReport> Thesenderarrlist = new ArrayList<ClearDateReport>();
			Page<Object> ThesenderPage = (Page<Object>) mapp.get("ThesenderPage");
			if (null != ThesenderPage) {
				List<Object> AccountStatisticsList = ThesenderPage.getContent();
				for (int i = 0; i < AccountStatisticsList.size(); i++) {
					Object[] obj = (Object[]) AccountStatisticsList.get(i);
					BizHandicap bizHandicap = handicapService.findFromCacheById(null == obj[0] ? 0 : (int) obj[0]);
					ClearDateReport clearDateReport = new ClearDateReport();
					clearDateReport.setHandicapId(null == obj[0] ? 0 : (int) obj[0]);
					clearDateReport.setAccount((String) obj[1]);
					clearDateReport.setAccountId((int) obj[2]);
					clearDateReport.setAlias((String) obj[3]);
					clearDateReport.setOwner((String) obj[4]);
					clearDateReport.setBankType((String) obj[5]);
					clearDateReport.setBalance(new BigDecimal(obj[6].toString()));
					clearDateReport.setOutward(new BigDecimal(obj[7].toString()));
					clearDateReport.setFee(new BigDecimal(obj[8].toString()));
					clearDateReport.setOutwardCount((int) obj[9]);
					clearDateReport.setOutwardSys(new BigDecimal(obj[10].toString()));
					clearDateReport.setOutwardSysCount((int) obj[11]);
					clearDateReport.setHandicapName(null == bizHandicap ? "" : bizHandicap.getName());
					Thesenderarrlist.add(clearDateReport);
				}
				map.put("thirdpartytotal", mapp.get("Thesendertotal"));
				map.put("thirdpartyPage", new Paging(ThesenderPage));
			}
			map.put("thirdpartyarrlist", Thesenderarrlist);

			// 备用卡数据
			List<ClearDateReport> standbyList = new ArrayList<ClearDateReport>();
			Page<Object> StandbyPage = (Page<Object>) mapp.get("StandbyPage");
			if (null != StandbyPage) {
				List<Object> AccountStatisticsList = StandbyPage.getContent();
				for (int i = 0; i < AccountStatisticsList.size(); i++) {
					Object[] obj = (Object[]) AccountStatisticsList.get(i);
					BizHandicap bizHandicap = handicapService.findFromCacheById(null == obj[0] ? 0 : (int) obj[0]);
					ClearDateReport clearDateReport = new ClearDateReport();
					clearDateReport.setHandicapId(null == obj[0] ? 0 : (int) obj[0]);
					clearDateReport.setAccount((String) obj[1]);
					clearDateReport.setAccountId((int) obj[2]);
					clearDateReport.setAlias((String) obj[3]);
					clearDateReport.setOwner((String) obj[4]);
					clearDateReport.setBankType((String) obj[5]);
					clearDateReport.setBalance(new BigDecimal(obj[6].toString()));
					clearDateReport.setOutward(new BigDecimal(obj[7].toString()));
					clearDateReport.setFee(new BigDecimal(obj[8].toString()));
					clearDateReport.setOutwardCount((int) obj[9]);
					clearDateReport.setOutwardSys(new BigDecimal(obj[10].toString()));
					clearDateReport.setOutwardSysCount((int) obj[11]);
					clearDateReport.setHandicapName(null == bizHandicap ? "" : bizHandicap.getName());
					standbyList.add(clearDateReport);
				}
				map.put("standbytotal", mapp.get("Standbytotal"));
				map.put("standbyPage", new Paging(StandbyPage));
			}
			map.put("standbyList", standbyList);
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("中转明细controller查询失败" + e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 中转明细>明细
	 * 
	 * @param pageNo
	 *            页码
	 * @param orderno
	 *            订单号
	 * @param startAndEndTimeToArray
	 *            时间控件数组值
	 * @param startamount
	 *            开始金额
	 * @param endamount
	 *            结束金额
	 * @param accountid
	 *            账号id
	 * @param type
	 *            标识查询哪个数据源
	 * @param serytype
	 *            标识查询哪个流水（系统流水、银行流水）
	 * @param parentstartAndEndTimeToArray
	 *            父页面时间控件数组值
	 * @param parentfieldval
	 *            父页面时间单选按钮值
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequiresPermissions("FinTransStat:*")
	@RequestMapping("/finTransStatMatch")
	public String FinOutStatMacth(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "orderno", required = false) String orderno,
			@RequestParam(value = "startAndEndTime", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "startamount", required = false) BigDecimal startamount,
			@RequestParam(value = "endamount", required = false) BigDecimal endamount,
			@RequestParam(value = "accountid", required = false) int accountid,
			@RequestParam(value = "type", required = false) int type,
			@RequestParam(value = "serytype", required = false) String serytype,
			@RequestParam(value = "parentstartAndEndTimeToArray", required = false) String[] parentstartAndEndTimeToArray,
			@RequestParam(value = "parentfieldval", required = false) String parentfieldval,
			@RequestParam(value = "status", required = false) int status,
			@RequestParam(value = "handicap", required = false) int handicap,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					Sort.Direction.ASC, "id");
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if ("".equals(orderno) || "" == orderno) {
				orderno = null;
			}
			if (new BigDecimal(0) != startamount) {

			} else {
				startamount = new BigDecimal(0);
				endamount = new BigDecimal(0);
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
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
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
				}
			}
			// 拼接时间戳查询数据条件
			String fristTime = null;
			String lastTime = null;
			if (0 != startAndEndTimeToArray.length && null != startAndEndTimeToArray) {
				fristTime = startAndEndTimeToArray[0];
				lastTime = startAndEndTimeToArray[1];
				// Date dtf1 = df.parse(fristTime);
				// Date dtf2 = df.parse(ptfristTime);
				// Date dtl1 = df.parse(lastTime);
				// Date dtl2 = df.parse(ptlastTime);
				// // 当两个时间都不为空 比较两个时间的大小 进行赋值
				// if (dtf1.getTime() < dtf2.getTime()) {
				// fristTime = ptfristTime;
				// }
				// if (dtl1.getTime() > dtl2.getTime()) {
				// lastTime = ptlastTime;
				// }
			} else {
				fristTime = ptfristTime;
				lastTime = ptlastTime;
			}
			Map<String, Object> mapp = finTransStatService.finTransStatMatch(orderno,
					TimeChangeCommon.TimeStamp2Date(fristTime, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(lastTime, "yyyy-MM-dd HH:mm:ss"), startamount, endamount, accountid,
					type, serytype, status, handicap, handicapIds, pageRequest);
			Page<Object> page = (Page<Object>) mapp.get("Page");
			if (null != page) {
				List<Object> FinTransStatMatch = page.getContent();
				List<FinTransStatMatch> arrlist = new ArrayList<FinTransStatMatch>();
				for (int i = 0; i < FinTransStatMatch.size(); i++) {
					Object[] obj = (Object[]) FinTransStatMatch.get(i);
					AccountBaseInfo bizAccount = accountService.getFromCacheById((int) obj[8]);
					BizHandicap bizHandicap = handicapService.findFromCacheById(obj[10] == null ? 0 : (int) obj[10]);
					FinTransStatMatch FinTrans = new FinTransStatMatch();
					FinTrans.setOrderno((String) obj[0]);
					FinTrans.setFromaccountname((String) obj[2]);
					FinTrans.setToaccountname((String) obj[3]);
					FinTrans.setAmount((BigDecimal) obj[4]);
					FinTrans.setFee((BigDecimal) obj[5]);
					FinTrans.setCreatetime((String) obj[6]);
					FinTrans.setRemark((String) obj[7]);
					FinTrans.setToid((int) obj[8]);
					FinTrans.setStatus((int) obj[9]);
					FinTrans.setAlias(bizAccount.getAlias());
					FinTrans.setHandicapname(bizHandicap == null ? "" : bizHandicap.getName());
					arrlist.add(FinTrans);
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
			log.error("中转明细》详情Controller查询失败" + e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 
	 * @param pageNo
	 *            页码
	 * @param account
	 *            汇入账号
	 * @param startAndEndTimeToArray
	 *            时间控件数组值
	 * @param fieldval
	 *            时间单选按钮值
	 * @param type
	 *            标识查询哪 个数据类型（入款银行卡中转、支付宝、微信、第三方）
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequiresPermissions("FinTransStat:*")
	@RequestMapping("/fincardliquidation")
	public String FinCardLiquidation(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "account", required = false) String account,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "fieldval", required = false) String fieldval,
			@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "accountOwner", required = false) String accountOwner,
			@RequestParam(value = "bankType", required = false) String bankType,
			@RequestParam(value = "handicap", required = false) int handicap,
			@RequestParam(value = "cartype", required = false) String cartype,
			@RequestParam(value = "status", required = false) String status,
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
				String startTimee = retStrFormatNowDate + " 07:00:00";
				Calendar c = Calendar.getInstance();
				c.setTime(nowTime);
				c.add(Calendar.DAY_OF_MONTH, 1);// 今天+1天
				Date tomorrow = c.getTime();
				String endTimee = sdFormatter.format(tomorrow) + " 06:59:59";
				fieldval = startTimee;
				whereTransactionValue = endTimee;
				fristTime = startTimee;
				lastTime = endTimee;
			} else if ("thisWeek".equals(fieldval)) {
				// 本周的数据
				// fieldval = getTimesWeekmorning().toString();
				// whereTransactionValue = getTimesWeeknight().toString();
				// 昨日数据
				fieldval = TimeChangeCommon.getYesterdayStartTime();
				whereTransactionValue = TimeChangeCommon.getYesterdayEndTime();
				fristTime = TimeChangeCommon.getYesterdayStartTime();
				;
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
			if (0 != startAndEndTimeToArray.length && null != startAndEndTimeToArray) {
				fristTime = startAndEndTimeToArray[0];
				lastTime = startAndEndTimeToArray[1];
				fieldval = null;
				whereTransactionValue = null;
			}
			account = !"".equals(account) ? account : null;
			accountOwner = !"".equals(accountOwner) ? accountOwner : null;
			bankType = !"".equals(bankType) ? bankType : null;
			cartype = !"".equals(cartype) ? cartype : null;
			SysUser loginUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			// 获取当前用户盘口 进行权限限制
			List<Integer> handicapList = getHandicapIdByCurrentUser(loginUser.getId() == 1 ? 0 : handicap, loginUser);
			Map<String, Object> mapp = finTransStatService.FinCardLiquidation(account,
					TimeChangeCommon.TimeStamp2Date(fristTime, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(lastTime, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(fieldval, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(whereTransactionValue, "yyyy-MM-dd HH:mm:ss"), type, accountOwner,
					bankType, handicapList, cartype, status, pageRequest);
			Map<String, Object> map = new LinkedHashMap<>();
			// 银行数据
			List<ClearDateReport> arrlist = new ArrayList<ClearDateReport>();
			Page<Object> CardLiquidationPage = (Page<Object>) mapp.get("CardLiquidationPage");
			if (null != CardLiquidationPage) {
				List<Object> AccountStatisticsList = CardLiquidationPage.getContent();
				for (int i = 0; i < AccountStatisticsList.size(); i++) {
					Object[] obj = (Object[]) AccountStatisticsList.get(i);
					BizHandicap bizHandicap = handicapService.findFromCacheById(null == obj[0] ? 0 : (int) obj[0]);
					ClearDateReport clearDateReport = new ClearDateReport();
					clearDateReport.setHandicapName(null == bizHandicap ? "" : bizHandicap.getName());
					clearDateReport.setHandicapId(null == obj[0] ? 0 : (int) obj[0]);
					clearDateReport.setAccount((String) obj[1]);
					clearDateReport.setAccountType((int) obj[2]);
					clearDateReport.setAccountId((int) obj[3]);
					clearDateReport.setAlias((String) obj[4]);
					clearDateReport.setOwner((String) obj[5]);
					clearDateReport.setBankType((String) obj[6]);
					clearDateReport.setIncome(new BigDecimal(obj[7].toString()));
					clearDateReport.setOutward(new BigDecimal(obj[8].toString()));
					clearDateReport.setFee(new BigDecimal(obj[9].toString()));
					clearDateReport.setIncomeCount(Integer.parseInt(obj[10].toString()));
					clearDateReport.setOutwardCount(Integer.parseInt(obj[11].toString()));
					clearDateReport.setOutwardPerson(Integer.parseInt(obj[12].toString()));
					clearDateReport.setIncomePerson(Integer.parseInt(obj[13].toString()));
					clearDateReport.setFeeCount(Integer.parseInt(obj[14].toString()));
					clearDateReport.setLos(new BigDecimal(obj[15].toString()));
					clearDateReport.setLosCount(Integer.parseInt(obj[16].toString()));
					clearDateReport.setIncomeSys(new BigDecimal(obj[17].toString()));
					clearDateReport.setIncomeSysCount(Integer.parseInt(obj[18].toString()));
					clearDateReport.setOutwardSys(new BigDecimal(obj[19].toString()));
					clearDateReport.setOutwardSysCount(Integer.parseInt(obj[20].toString()));
					clearDateReport.setStatus(AccountStatus.findByStatus((int) obj[22]).getMsg());
					clearDateReport.setBalance(new BigDecimal(obj[23] == null ? "0" : obj[23].toString()));
					arrlist.add(clearDateReport);
				}
				map.put("CardLiquidationtotal", mapp.get("CardLiquidationtotal"));
				map.put("CardLiquidationpage", new Paging(CardLiquidationPage));
				map.put("minusDate", mapp.get("minusDate"));
			}
			map.put("CardLiquidationarrlist", arrlist);
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			log.error("出入卡清算controller查询失败", e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

}
