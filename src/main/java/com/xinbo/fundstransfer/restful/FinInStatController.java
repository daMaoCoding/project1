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
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.FinInStatisticsService;
import com.xinbo.fundstransfer.service.HandicapService;
import com.xinbo.fundstransfer.service.SysDataPermissionService;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.BizIncomeRequest;
import com.xinbo.fundstransfer.domain.entity.ClearDateReport;
import com.xinbo.fundstransfer.domain.entity.FinInStat;
import com.xinbo.fundstransfer.domain.entity.FinInStatMatch;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import java.util.Map;
 
/**
 * 入款明细请求接口
 * 
 * @author Steven
 *
 */
@Slf4j
@RestController
@RequestMapping("/r/fininstat")
public class FinInStatController extends BaseController {
    @Autowired
    private FinInStatisticsService finInStatisticsService;
    @Autowired
    private HandicapService handicapService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private SysDataPermissionService sysDataPermissionService;
    @Autowired
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
     * 入款明细
     * 
     * @param pageNo
     *            页码
     * @param handicap
     *            盘口
     * @param level
     *            层级
     * @param account
     *            收款账号
     * @param startAndEndTimeToArray
     *            时间控件数组值
     * @param fieldval
     *            时间单选按钮
     * @param type
     *            标识查询哪个数据源（银行卡、微信、支付宝、第三方）
     * @param handicapname
     *            盘口name值
     * @return
     * @throws JsonProcessingException
     */
    @RequiresPermissions("FinInStat:*")
    @RequestMapping("/fininstatistical")
    public String AccountStatistics(@RequestParam(value = "pageNo") int pageNo,
            @RequestParam(value = "handicap", required = false) int handicap,
            @RequestParam(value = "level", required = false) int level,
            @RequestParam(value = "account", required = false) String account,
            @RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
            @RequestParam(value = "fieldval", required = false) String fieldval,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "handicapname", required = false) String handicapname,
            @RequestParam(value = "accountOwner", required = false) String accountOwner,
            @RequestParam(value = "bankType", required = false) String bankType,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
        try {
            GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
                    GeneralResponseData.ResponseStatus.SUCCESS.getValue());
            PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
                    Sort.Direction.DESC, "amounts");
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
            } else if ("week".equals(fieldval)) {
                fieldval = TimeChangeCommon.getTimesWeekmorning();
                whereTransactionValue = TimeChangeCommon.getTimesWeeknight();
            } else if ("lastweek".equals(fieldval)) {
                fieldval = TimeChangeCommon.getPreviousWeekday();
                whereTransactionValue = TimeChangeCommon.getTimesWeekmorningAt6();
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
            // 拼接根据账号查询的条件
            account = !"".equals(account) ? account : null;
            accountOwner = !"".equals(accountOwner) ? accountOwner : null;
            bankType = !"".equals(bankType) ? bankType : null;
            if ("".equals(handicapname) || "" == handicapname) {
                handicapname = null;
            }
            // 获取当前用户盘口 进行权限限制
            SysUser loginUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
            List<Integer> handicapList = getHandicapIdByCurrentUser(loginUser.getId() == 1 ? 0 : handicap, loginUser);
            Map<String, Object> mapp = finInStatisticsService.findFinInStatistics(handicapList, level, account,
                    TimeChangeCommon.TimeStamp2Date(fristTime, "yyyy-MM-dd HH:mm:ss"),
                    TimeChangeCommon.TimeStamp2Date(lastTime, "yyyy-MM-dd HH:mm:ss"),
                    TimeChangeCommon.TimeStamp2Date(fieldval, "yyyy-MM-dd HH:mm:ss"),
                    TimeChangeCommon.TimeStamp2Date(whereTransactionValue, "yyyy-MM-dd HH:mm:ss"), type, handicapname,
                    accountOwner, bankType, pageRequest);
            Map<String, Object> map = new LinkedHashMap<>();
            // 银行数据
            Page<Object> BankcardPage = (Page<Object>) mapp.get("BankcardPage");
            if (null != BankcardPage) {
                List<Object> AccountStatisticsList = BankcardPage.getContent();
                List<FinInStat> arrlist = new ArrayList<FinInStat>();
                for (int i = 0; i < AccountStatisticsList.size(); i++) {
                    Object[] obj = (Object[]) AccountStatisticsList.get(i);
                    FinInStat FinInStat = new FinInStat();
                    FinInStat.setAmount((BigDecimal) obj[0]);
                    FinInStat.setFee((BigDecimal) obj[1]);
                    FinInStat.setCounts(new Integer(String.valueOf(obj[2])));
                    FinInStat.setId((int) obj[3]);
                    FinInStat.setHandicapname((String) obj[4]);
                    FinInStat.setLevelname((String) obj[5]);
                    FinInStat.setAccount((String) obj[6]);
                    FinInStat.setBankBalance((BigDecimal) obj[7]);
                    FinInStat.setAlias((String) obj[8]);
                    FinInStat.setOwner((String) obj[9]);
                    FinInStat.setBanktype((String) obj[10]);
                    arrlist.add(FinInStat);
                }
                map.put("Bankcardarrlist", arrlist);
                map.put("Bankcardtotal", mapp.get("Bankcardtotal"));
                map.put("Bankcardpage", new Paging(BankcardPage));
            }
 
            // 下发卡银行数据
            Page<Object> sendCardPage = (Page<Object>) mapp.get("sendCardPage");
            if (null != sendCardPage) {
                List<Object> sendCardList = sendCardPage.getContent();
                List<FinInStat> arrlist = new ArrayList<FinInStat>();
                for (int i = 0; i < sendCardList.size(); i++) {
                    Object[] obj = (Object[]) sendCardList.get(i);
                    FinInStat FinInStat = new FinInStat();
                    FinInStat.setAmount((BigDecimal) obj[0]);
                    FinInStat.setFee((BigDecimal) obj[1]);
                    FinInStat.setCounts(new Integer(String.valueOf(obj[2])));
                    FinInStat.setId((int) obj[3]);
                    FinInStat.setAccount((String) obj[4]);
                    FinInStat.setBankBalance((BigDecimal) obj[5]);
                    FinInStat.setAlias((String) obj[6]);
                    FinInStat.setOwner((String) obj[7]);
                    FinInStat.setBanktype((String) obj[8]);
                    arrlist.add(FinInStat);
                }
                map.put("sendCardlist", arrlist);
                map.put("sendCardtotal", mapp.get("sendCardTotal"));
                map.put("sendCardpage", new Paging(sendCardPage));
            }
 
            // 微信数据
            Page<Object> WeChatPage = (Page<Object>) mapp.get("WeChatPage");
            if (null != WeChatPage) {
                List<Object> WeChatList = WeChatPage.getContent();
                List<FinInStat> WeChatarrlist = new ArrayList<FinInStat>();
                for (int i = 0; i < WeChatList.size(); i++) {
                    Object[] obj = (Object[]) WeChatList.get(i);
                    FinInStat FinInStat = new FinInStat();
                    FinInStat.setAmount((BigDecimal) obj[0]);
                    FinInStat.setFee((BigDecimal) obj[1]);
                    FinInStat.setCounts(new Integer(String.valueOf(obj[2])));
                    FinInStat.setId((int) obj[3]);
                    FinInStat.setHandicapname((String) obj[4]);
                    FinInStat.setLevelname((String) obj[5]);
                    FinInStat.setAccount((String) obj[6]);
                    FinInStat.setBankBalance((BigDecimal) obj[7]);
                    WeChatarrlist.add(FinInStat);
                }
                map.put("WeChatarrlist", WeChatarrlist);
                map.put("WeChattotal", mapp.get("WeChattotal"));
                map.put("WeChatpage", new Paging(WeChatPage));
            }
 
            // 支付宝数据
            Page<Object> PaytreasurePage = (Page<Object>) mapp.get("PaytreasurePage");
            if (null != PaytreasurePage) {
                List<Object> PaytreasureList = PaytreasurePage.getContent();
                List<FinInStat> Paytreasurearrlist = new ArrayList<FinInStat>();
                for (int i = 0; i < PaytreasureList.size(); i++) {
                    Object[] obj = (Object[]) PaytreasureList.get(i);
                    FinInStat FinInStat = new FinInStat();
                    FinInStat.setAmount((BigDecimal) obj[0]);
                    FinInStat.setFee((BigDecimal) obj[1]);
                    FinInStat.setCounts(new Integer(String.valueOf(obj[2])));
                    FinInStat.setId((int) obj[3]);
                    FinInStat.setHandicapname((String) obj[4]);
                    FinInStat.setLevelname((String) obj[5]);
                    FinInStat.setAccount((String) obj[6]);
                    FinInStat.setBankBalance((BigDecimal) obj[7]);
                    Paytreasurearrlist.add(FinInStat);
                }
                map.put("Paytreasurearrlist", Paytreasurearrlist);
                map.put("Paytreasuretotal", mapp.get("Paytreasuretotal"));
                map.put("PaytreasurePage", new Paging(PaytreasurePage));
            }
 
            // 第三方数据
            Page<Object> thirdpartyPage = (Page<Object>) mapp.get("thirdpartyPage");
            if (null != thirdpartyPage) {
                List<Object> thirdpartyList = thirdpartyPage.getContent();
                List<FinInStat> thirdpartyarrlist = new ArrayList<FinInStat>();
                for (int i = 0; i < thirdpartyList.size(); i++) {
                    Object[] obj = (Object[]) thirdpartyList.get(i);
                    AccountBaseInfo bizAccountFrom = accountService.getFromCacheById((Integer) obj[0]);
                    FinInStat FinInStat = new FinInStat();
                    FinInStat.setId((int) obj[0]);
                    FinInStat.setHandicapname((String) obj[1]);
                    FinInStat.setLevelname((String) obj[3]);
                    FinInStat.setAccount(bizAccountFrom.getBankName());
                    FinInStat.setBankBalance((BigDecimal) obj[5]);
                    FinInStat.setAmount((BigDecimal) obj[6]);
                    FinInStat.setFee((BigDecimal) obj[7]);
                    FinInStat.setCounts(new Integer(String.valueOf(obj[8])));
                    thirdpartyarrlist.add(FinInStat);
                }
                map.put("thirdpartyarrlist", thirdpartyarrlist);
                map.put("thirdpartytotal", mapp.get("thirdpartytotal"));
                map.put("thirdpartyPage", new Paging(thirdpartyPage));
            }
            responseData.setData(map);
            return mapper.writeValueAsString(responseData);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("入款明细Controller 查询发生错误" + e);
            return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
                    "操作失败  " + e.getLocalizedMessage()));
        }
    }
 
    @RequiresPermissions("FinInStat:*")
    @RequestMapping("/fininstatisticalFromClearDate")
    public String AccountStatisticsFromClearDate(@RequestParam(value = "pageNo") int pageNo,
            @RequestParam(value = "handicap", required = false) int handicap,
            @RequestParam(value = "level", required = false) int level,
            @RequestParam(value = "account", required = false) String account,
            @RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
            @RequestParam(value = "fieldval", required = false) String fieldval,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "handicapname", required = false) String handicapname,
            @RequestParam(value = "accountOwner", required = false) String accountOwner,
            @RequestParam(value = "bankType", required = false) String bankType,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
        try {
            GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
                    GeneralResponseData.ResponseStatus.SUCCESS.getValue());
            PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
                    Sort.Direction.DESC, "income");
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
            } else if ("week".equals(fieldval)) {
                fieldval = TimeChangeCommon.getTimesWeekmorning();
                whereTransactionValue = TimeChangeCommon.getTimesWeeknight();
            } else if ("lastweek".equals(fieldval)) {
                fieldval = TimeChangeCommon.getPreviousWeekday();
                whereTransactionValue = TimeChangeCommon.getTimesWeekmorningAt6();
            }
            // 拼接时间戳查询数据条件
            String fristTime = null;
            String lastTime = null;
            if (0 != startAndEndTimeToArray.length && null != startAndEndTimeToArray) {
                fristTime = startAndEndTimeToArray[0];
                lastTime = startAndEndTimeToArray[1];
                fieldval = fristTime;
                whereTransactionValue = lastTime;
            }
            // 拼接根据账号查询的条件
            account = !"".equals(account) ? account : null;
            accountOwner = !"".equals(accountOwner) ? accountOwner : null;
            bankType = !"".equals(bankType) ? bankType : null;
            if ("".equals(handicapname) || "" == handicapname) {
                handicapname = null;
            }
            // 获取当前用户盘口 进行权限限制
            SysUser loginUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
            List<Integer> handicapList = getHandicapIdByCurrentUser(loginUser.getId() == 1 ? 0 : handicap, loginUser);
            Map<String, Object> mapp = finInStatisticsService.findFinInStatisticsFromClearDate(handicapList, level,
                    account, TimeChangeCommon.TimeStamp2Date(fristTime, "yyyy-MM-dd HH:mm:ss"),
                    TimeChangeCommon.TimeStamp2Date(lastTime, "yyyy-MM-dd HH:mm:ss"),
                    TimeChangeCommon.TimeStamp2Date(fieldval, "yyyy-MM-dd HH:mm:ss"),
                    TimeChangeCommon.TimeStamp2Date(whereTransactionValue, "yyyy-MM-dd HH:mm:ss"), type, handicapname,
                    accountOwner, bankType, pageRequest);
            Map<String, Object> map = new LinkedHashMap<>();
            // 银行数据
            Page<Object> BankcardPage = (Page<Object>) mapp.get("BankcardPage");
            if (null != BankcardPage) {
                List<Object> AccountStatisticsList = BankcardPage.getContent();
                List<ClearDateReport> arrlist = new ArrayList<ClearDateReport>();
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
                    clearDateReport.setIncome(new BigDecimal(obj[7].toString()));
                    clearDateReport.setIncomeCount((int) obj[8]);
                    clearDateReport.setIncomePerson((int) obj[9]);
                    clearDateReport.setIncomeSys(new BigDecimal(obj[10].toString()));
                    clearDateReport.setIncomeSysCount((int) obj[11]);
                    clearDateReport.setHandicapName(null == bizHandicap ? "" : bizHandicap.getName());
                    clearDateReport.setLevels((String) obj[12]);
                    arrlist.add(clearDateReport);
                }
                map.put("Bankcardarrlist", arrlist);
                map.put("Bankcardtotal", mapp.get("Bankcardtotal"));
                map.put("Bankcardpage", new Paging(BankcardPage));
            }
 
            // 下发卡银行数据
            Page<Object> sendCardPage = (Page<Object>) mapp.get("sendCardPage");
            if (null != sendCardPage) {
                List<Object> sendCardList = sendCardPage.getContent();
                List<ClearDateReport> arrlist = new ArrayList<ClearDateReport>();
                for (int i = 0; i < sendCardList.size(); i++) {
                    Object[] obj = (Object[]) sendCardList.get(i);
                    BizHandicap bizHandicap = handicapService.findFromCacheById(null == obj[0] ? 0 : (int) obj[0]);
                    ClearDateReport clearDateReport = new ClearDateReport();
                    clearDateReport.setHandicapId(null == obj[0] ? 0 : (int) obj[0]);
                    clearDateReport.setAccount((String) obj[1]);
                    clearDateReport.setAccountId((int) obj[2]);
                    clearDateReport.setAlias((String) obj[3]);
                    clearDateReport.setOwner((String) obj[4]);
                    clearDateReport.setBankType((String) obj[5]);
                    clearDateReport.setBalance(new BigDecimal(obj[6].toString()));
                    clearDateReport.setIncome(new BigDecimal(obj[7].toString()));
                    clearDateReport.setIncomeCount((int) obj[8]);
                    clearDateReport.setIncomePerson((int) obj[9]);
                    clearDateReport.setIncomeSys(new BigDecimal(obj[10].toString()));
                    clearDateReport.setIncomeSysCount((int) obj[11]);
                    clearDateReport.setLevels((String) obj[12]);
                    clearDateReport.setHandicapName(null == bizHandicap ? "" : bizHandicap.getName());
                    arrlist.add(clearDateReport);
                }
                map.put("sendCardlist", arrlist);
                map.put("sendCardtotal", mapp.get("sendCardTotal"));
                map.put("sendCardpage", new Paging(sendCardPage));
            }
 
            // 备用银行数据
            Page<Object> standbyCardPage = (Page<Object>) mapp.get("StandbyCardPage");
            if (null != standbyCardPage) {
                List<Object> standbyCardList = standbyCardPage.getContent();
                List<ClearDateReport> arrlist = new ArrayList<ClearDateReport>();
                for (int i = 0; i < standbyCardList.size(); i++) {
                    Object[] obj = (Object[]) standbyCardList.get(i);
                    BizHandicap bizHandicap = handicapService.findFromCacheById(null == obj[0] ? 0 : (int) obj[0]);
                    ClearDateReport clearDateReport = new ClearDateReport();
                    clearDateReport.setHandicapId(null == obj[0] ? 0 : (int) obj[0]);
                    clearDateReport.setAccount((String) obj[1]);
                    clearDateReport.setAccountId((int) obj[2]);
                    clearDateReport.setAlias((String) obj[3]);
                    clearDateReport.setOwner((String) obj[4]);
                    clearDateReport.setBankType((String) obj[5]);
                    clearDateReport.setBalance(new BigDecimal(obj[6].toString()));
                    clearDateReport.setIncome(new BigDecimal(obj[7].toString()));
                    clearDateReport.setIncomeCount((int) obj[8]);
                    clearDateReport.setIncomePerson((int) obj[9]);
                    clearDateReport.setIncomeSys(new BigDecimal(obj[10].toString()));
                    clearDateReport.setIncomeSysCount((int) obj[11]);
                    clearDateReport.setLevels((String) obj[12]);
                    clearDateReport.setHandicapName(null == bizHandicap ? "" : bizHandicap.getName());
                    arrlist.add(clearDateReport);
                }
                map.put("standbyCardlist", arrlist);
                map.put("standbyCardtotal", mapp.get("StandbyCardTotal"));
                map.put("standbyCardpage", new Paging(standbyCardPage));
            }
 
            // 客户绑定卡数据
            Page<Object> clientCardPage = (Page<Object>) mapp.get("ClientCardPage");
            if (null != clientCardPage) {
                List<Object> clientCardList = clientCardPage.getContent();
                List<ClearDateReport> arrlist = new ArrayList<ClearDateReport>();
                for (int i = 0; i < clientCardList.size(); i++) {
                    Object[] obj = (Object[]) clientCardList.get(i);
                    BizHandicap bizHandicap = handicapService.findFromCacheById(null == obj[0] ? 0 : (int) obj[0]);
                    ClearDateReport clearDateReport = new ClearDateReport();
                    clearDateReport.setHandicapId(null == obj[0] ? 0 : (int) obj[0]);
                    clearDateReport.setAccount((String) obj[1]);
                    clearDateReport.setAccountId((int) obj[2]);
                    clearDateReport.setAlias((String) obj[3]);
                    clearDateReport.setOwner((String) obj[4]);
                    clearDateReport.setBankType((String) obj[5]);
                    clearDateReport.setBalance(new BigDecimal(obj[6].toString()));
                    clearDateReport.setIncome(new BigDecimal(obj[7].toString()));
                    clearDateReport.setIncomeCount((int) obj[8]);
                    clearDateReport.setIncomePerson((int) obj[9]);
                    clearDateReport.setIncomeSys(new BigDecimal(obj[10].toString()));
                    clearDateReport.setIncomeSysCount((int) obj[11]);
                    clearDateReport.setLevels((String) obj[12]);
                    clearDateReport.setHandicapName(null == bizHandicap ? "" : bizHandicap.getName());
                    arrlist.add(clearDateReport);
                }
                map.put("clientCardlist", arrlist);
                map.put("clientCardtotal", mapp.get("ClientCardTotal"));
                map.put("clientCardpage", new Paging(clientCardPage));
            }
 
            responseData.setData(map);
            return mapper.writeValueAsString(responseData);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("入款明细Controller 查询发生错误" + e);
            return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
                    "操作失败  " + e.getLocalizedMessage()));
        }
    }
 
    /**
     * 入款明细>明细
     * 
     * @param pageNo
     *            页码
     * @param memberrealname
     *            汇出人
     * @param startAndEndTimeToArray
     *            时间控件数组值
     * @param startamount
     *            开始金额
     * @param endamount
     *            结束金额
     * @param id
     *            账号id
     * @param type
     *            标识查询哪个数据源（银行卡、微信、支付宝、第三方）
     * @param parentstartAndEndTimeToArray
     *            父页面时间控件数组值
     * @param parentfieldval
     *            父页面时间单选按钮值
     * @return
     * @throws JsonProcessingException
     */
    @RequiresPermissions("FinInStat:*")
    @RequestMapping("/fininstatmatch")
    public String FinOutStatMacth(@RequestParam(value = "pageNo") int pageNo,
            @RequestParam(value = "memberrealname", required = false) String memberrealname,
            @RequestParam(value = "startAndEndTime", required = false) String[] startAndEndTimeToArray,
            @RequestParam(value = "startamount", required = false) BigDecimal startamount,
            @RequestParam(value = "endamount", required = false) BigDecimal endamount,
            @RequestParam(value = "id", required = false) int id,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "parentstartAndEndTime", required = false) String[] parentstartAndEndTimeToArray,
            @RequestParam(value = "parentfieldval", required = false) String parentfieldval,
            @RequestParam(value = "handicap", required = false) int handicap,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
        try {
            GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
                    GeneralResponseData.ResponseStatus.SUCCESS.getValue());
            PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
                    Sort.Direction.ASC, "id");
            if ("".equals(memberrealname) || "" == memberrealname) {
                memberrealname = null;
            }
            if (new BigDecimal(0) == startamount) {
                startamount = new BigDecimal(0);
                endamount = new BigDecimal(0);
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
            Map<String, Object> mapp = finInStatisticsService.findFinInStatMatch(memberrealname,
                    TimeChangeCommon.TimeStamp2Date(fristTime, "yyyy-MM-dd HH:mm:ss"),
                    TimeChangeCommon.TimeStamp2Date(lastTime, "yyyy-MM-dd HH:mm:ss"), startamount, endamount, id, type,
                    handicap, pageRequest);
            Page<Object> page = (Page<Object>) mapp.get("Page");
            if (null != page) {
                List<Object> FinOutMatchList = page.getContent();
                List<FinInStatMatch> arrlist = new ArrayList<FinInStatMatch>();
                for (int i = 0; i < FinOutMatchList.size(); i++) {
                    Object[] obj = (Object[]) FinOutMatchList.get(i);
                    FinInStatMatch FinInStatMatch = new FinInStatMatch();
                    FinInStatMatch.setHandicapname((String) obj[0]);
                    if ("thirdparty".equals(type))
                        FinInStatMatch.setType(4);
                    else
                        FinInStatMatch.setType((int) obj[1]);
                    FinInStatMatch.setMemberrealname((String) obj[2]);
                    FinInStatMatch.setOrderno((String) obj[3]);
                    FinInStatMatch.setAmount((BigDecimal) obj[4]);
                    FinInStatMatch.setFee((BigDecimal) obj[5]);
                    FinInStatMatch.setCreatetime((String) obj[6]);
                    FinInStatMatch.setRemark((String) obj[7]);
                    arrlist.add(FinInStatMatch);
                }
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("arrlist", arrlist);
                map.put("page", new Paging(page));
                map.put("total", mapp.get("total"));
                responseData.setData(map);
            }
 
            Page<Object> sendCardpage = (Page<Object>) mapp.get("sendCardPage");
            if (null != sendCardpage) {
                List<Object> FinOutMatchList = sendCardpage.getContent();
                List<FinInStatMatch> arrlist = new ArrayList<FinInStatMatch>();
                for (int i = 0; i < FinOutMatchList.size(); i++) {
                    Object[] obj = (Object[]) FinOutMatchList.get(i);
                    AccountBaseInfo bizAccount = accountService.getFromCacheById((int) obj[9]);
                    BizHandicap bizHandicap = handicapService.findFromCacheById(bizAccount.getHandicapId());
                    FinInStatMatch FinInStatMatch = new FinInStatMatch();
                    FinInStatMatch.setFromaccount((String) obj[0]);
                    FinInStatMatch.setBankname((String) obj[1]);
                    FinInStatMatch.setOrderno((String) obj[3]);
                    FinInStatMatch.setAmount((BigDecimal) obj[4]);
                    FinInStatMatch.setFee((BigDecimal) obj[5]);
                    FinInStatMatch.setCreatetime((String) obj[6]);
                    FinInStatMatch.setRemark((String) obj[7]);
                    FinInStatMatch.setHandicapname(bizHandicap == null ? "" : bizHandicap.getName());
                    arrlist.add(FinInStatMatch);
                }
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("sendCardArrlist", arrlist);
                map.put("sendCardPage", new Paging(sendCardpage));
                map.put("sendCardTotal", mapp.get("sendCardtotal"));
                responseData.setData(map);
            }
            return mapper.writeValueAsString(responseData);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("入款明细》明细查询出错" + e);
            return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
                    "操作失败  " + e.getLocalizedMessage()));
        }
    }
 
    /**
     * 银行明细
     * 
     * @param pageNo
     * @param startAndEndTimeToArray
     * @param startamount
     * @param endamount
     * @param accountid
     * @param parentstartAndEndTimeToArray
     * @param status
     * @param pageSize
     * @return
     * @throws JsonProcessingException
     */
    @RequiresPermissions("FinInStat:*")
    @RequestMapping("/fininstatmatchbank")
    public String FinOutStatMacthBank(@RequestParam(value = "pageNo") int pageNo,
            @RequestParam(value = "startAndEndTime", required = false) String[] startAndEndTimeToArray,
            @RequestParam(value = "startamount", required = false) BigDecimal startamount,
            @RequestParam(value = "endamount", required = false) BigDecimal endamount,
            @RequestParam(value = "accountid", required = false) int accountid,
            @RequestParam(value = "parentstartAndEndTimeToArray", required = false) String[] parentstartAndEndTimeToArray,
            @RequestParam(value = "status") int status,
            @RequestParam(value = "InStatOrTransStat") String InStatOrTransStat,
            @RequestParam(value = "typestatus", required = false) int typestatus,
            @RequestParam(value = "parentfieldval", required = false) String parentfieldval,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
        try {
            GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
                    GeneralResponseData.ResponseStatus.SUCCESS.getValue());
            PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
                    Sort.Direction.ASC, "id");
            if (new BigDecimal(0) == startamount) {
                startamount = new BigDecimal(0);
                endamount = new BigDecimal(0);
            }
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            // 拼接父页面的时间
            String ptfristTime = null;
            String ptlastTime = null;
            if (0 != parentstartAndEndTimeToArray.length && null != parentstartAndEndTimeToArray) {
                ptfristTime = parentstartAndEndTimeToArray[0];
                ptlastTime = parentstartAndEndTimeToArray[1];
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
                    fristTime = startTime;
                    lastTime = endTime;
                } else if ("thisWeek".equals(parentfieldval)) {
                    // 本周的数据
                    // ptfristTime = getTimesWeekmorning().toString();
                    // ptlastTime = getTimesWeeknight().toString();
                    // 昨日数据
                    fristTime = TimeChangeCommon.getYesterdayStartTime();
                    lastTime = TimeChangeCommon.getYesterdayEndTime();
                } else if ("thisMonth".equals(parentfieldval)) {
                    // 本月的数据
                    fristTime = TimeChangeCommon.getTimesMonthmorning();
                    lastTime = TimeChangeCommon.getTimesMonthnight();
                } else if ("lastMonth".equals(parentfieldval)) {
                    // 上月的数据
                    fristTime = TimeChangeCommon.getLastMonthStartMorning();
                    lastTime = TimeChangeCommon.getLastMonthEndMorning();
                }
            }
            Map<String, Object> mapp = finInStatisticsService.findFinInStatMatchBank(
                    TimeChangeCommon.TimeStamp2Date(fristTime, "yyyy-MM-dd HH:mm:ss"),
                    TimeChangeCommon.TimeStamp2Date(lastTime, "yyyy-MM-dd HH:mm:ss"), startamount, endamount, accountid,
                    status, InStatOrTransStat, typestatus, pageRequest);
            Page<Object> page = (Page<Object>) mapp.get("Page");
            if (null != page) {
                List<Object> FinOutMatchList = page.getContent();
                List<FinInStatMatch> arrlist = new ArrayList<FinInStatMatch>();
                for (int i = 0; i < FinOutMatchList.size(); i++) {
                    Object[] obj = (Object[]) FinOutMatchList.get(i);
                    FinInStatMatch FinInStatMatch = new FinInStatMatch();
                    FinInStatMatch.setToaccount((String) obj[0]);
                    FinInStatMatch.setToaccountowner((String) obj[1]);
                    FinInStatMatch.setAmount((BigDecimal) obj[2]);
                    FinInStatMatch.setRemark((String) obj[3]);
                    FinInStatMatch.setSummary((String) obj[4]);
                    FinInStatMatch.setTradingtime((String) obj[5]);
                    FinInStatMatch.setStatus((int) obj[6]);
                    FinInStatMatch.setCreatetime((String) obj[7]);
                    FinInStatMatch.setBalance((BigDecimal) obj[8]);
                    arrlist.add(FinInStatMatch);
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
            log.error("入款明细》明细查询出错" + e);
            return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
                    "操作失败  " + e.getLocalizedMessage()));
        }
    }
 
    @RequestMapping("/incomethird")
    public String IncomeThird(@RequestParam(value = "pageNo") int pageNo,
            @RequestParam(value = "handicap", required = false) int handicap,
            @RequestParam(value = "level", required = false) int level,
            @RequestParam(value = "account", required = false) String account,
            @RequestParam(value = "thirdaccount", required = false) String thirdaccount,
            @RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
            @RequestParam(value = "toaccount", required = false) String toaccount,
            @RequestParam(value = "startamount", required = false) BigDecimal startamount,
            @RequestParam(value = "endamount", required = false) BigDecimal endamount,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
        try {
            GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
                    GeneralResponseData.ResponseStatus.SUCCESS.getValue());
            PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
                    Sort.Direction.DESC, "create_time");
            // 拼接时间戳查询数据条件
            String fristTime = null;
            String lastTime = null;
            // 初始查询当天的数据
            if (0 != startAndEndTimeToArray.length && null != startAndEndTimeToArray) {
                fristTime = startAndEndTimeToArray[0];
                lastTime = startAndEndTimeToArray[1];
            } else {
                // 今天数据
                Date nowTime = new Date(System.currentTimeMillis());
                SimpleDateFormat sdFormatter = new SimpleDateFormat("yyyy-MM-dd");
                String retStrFormatNowDate = sdFormatter.format(nowTime);
                fristTime = retStrFormatNowDate + " 07:00:00";
                Calendar c = Calendar.getInstance();
                c.setTime(nowTime);
                c.add(Calendar.DAY_OF_MONTH, 1);// 今天+1天
                Date tomorrow = c.getTime();
                lastTime = sdFormatter.format(tomorrow) + " 06:59:59";
            }
            // 会员账号
            String accountt = !"".equals(account) ? account : null;
            // 会员账号
            String thirdaccountt = !"".equals(thirdaccount) ? thirdaccount : null;
            // 收款账号
            String toaccountt = !"".equals(toaccount) ? toaccount : null;
            Map<String, Object> mapp = finInStatisticsService.findIncomeThird(handicap, level, accountt, thirdaccountt,
                    TimeChangeCommon.TimeStamp2Date(fristTime, "yyyy-MM-dd HH:mm:ss"),
                    TimeChangeCommon.TimeStamp2Date(lastTime, "yyyy-MM-dd HH:mm:ss"), toaccountt, startamount,
                    endamount, type, pageRequest);
            Map<String, Object> map = new LinkedHashMap<>();
            // 第三方已经对账的数据
            Page<Object> IncomeThirdPage = (Page<Object>) mapp.get("IncomeThird");
            SimpleDateFormat Simple = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            if (null != IncomeThirdPage) {
                List<Object> AccountStatisticsList = (List<Object>) mapp.get("data");
                List<BizIncomeRequest> arrlist = new ArrayList<BizIncomeRequest>();
                for (int i = 0; i < AccountStatisticsList.size(); i++) {
                    Object[] obj = (Object[]) AccountStatisticsList.get(i);
                    BizIncomeRequest Income = new BizIncomeRequest();
                    Income.setHandicapName((String) obj[0]);
                    Income.setLevelName((String) obj[1]);
                    Income.setMemberUserName((String) obj[2]);
                    Income.setOrderNo((String) obj[3]);
                    Income.setToAccountBank((String) obj[4]);
                    Income.setToAccount((String) obj[5]);
                    Income.setAmount((BigDecimal) obj[6]);
                    Income.setRemark((String) obj[7]);
                    Income.setToId((int) obj[8]);
                    arrlist.add(Income);
                }
                map.put("BizIncomelist", arrlist);
                map.put("IncomeThirdtotal", mapp.get("IncomeThirdtotal"));
                map.put("IncomeThirdPage", new Paging(IncomeThirdPage));
            }
            responseData.setData(map);
            return mapper.writeValueAsString(responseData);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("第三方入款对账 查询发生错误" + e);
            return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
                    "操作失败  " + e.getLocalizedMessage()));
        }
    }
 
    @RequestMapping("/incomesearbyaccount")
    public String IncomeSearByAccount(@RequestParam(value = "pageNo") int pageNo,
            @RequestParam(value = "member", required = false) String member,
            @RequestParam(value = "toid", required = false) int toid,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
        try {
            GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
                    GeneralResponseData.ResponseStatus.SUCCESS.getValue());
            PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
                    Sort.Direction.DESC, "create_time");
            // 拼接时间戳查询数据条件
            String fristTime = null;
            String lastTime = null;
            Date nowTime = new Date(System.currentTimeMillis());
            SimpleDateFormat sdFormatter = new SimpleDateFormat("yyyy-MM-dd");
            String retStrFormatNowDate = sdFormatter.format(nowTime);
            fristTime = retStrFormatNowDate + " 00:00:00";
            lastTime = retStrFormatNowDate + " 23:59:59";
            Map<String, Object> mapp = finInStatisticsService.findIncomeByAccount(member, toid,
                    TimeChangeCommon.TimeStamp2Date(fristTime, "yyyy-MM-dd HH:mm:ss"),
                    TimeChangeCommon.TimeStamp2Date(lastTime, "yyyy-MM-dd HH:mm:ss"), pageRequest);
            Map<String, Object> map = new LinkedHashMap<>();
            // 会员最近一天入款状况
            Page<Object> IncomeByaccountPage = (Page<Object>) mapp.get("Page");
            if (null != IncomeByaccountPage) {
                List<Object> AccountStatisticsList = IncomeByaccountPage.getContent();
                List<BizIncomeRequest> arrlist = new ArrayList<BizIncomeRequest>();
                for (int i = 0; i < AccountStatisticsList.size(); i++) {
                    Object[] obj = (Object[]) AccountStatisticsList.get(i);
                    BizIncomeRequest Income = new BizIncomeRequest();
                    Income.setId(Long.parseLong(obj[0].toString()));
                    Income.setHandicap((Integer) obj[1]);
                    Income.setHandicapName((String) obj[2]);
                    Income.setLevel((Integer) obj[3]);
                    Income.setLevelName((String) obj[4]);
                    Income.setAmount((BigDecimal) obj[5]);
                    Income.setCreateTime((Date) obj[6]);
                    Income.setType((Integer) obj[7]);
                    Income.setOrderNo((String) obj[8]);
                    Income.setToAccount((String) obj[9]);
                    Income.setToAccountBank((String) obj[10]);
                    Income.setMemberRealName((String) obj[12]);
                    arrlist.add(Income);
                }
                map.put("IncomeByMemberlist", arrlist);
                map.put("IncomeByMembertotal", mapp.get("total"));
                map.put("IncomeByMemberPage", new Paging(IncomeByaccountPage));
            }
            responseData.setData(map);
            return mapper.writeValueAsString(responseData);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("根据账号查询会员最近一天入款信息 查询发生错误" + e);
            return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
                    "操作失败  " + e.getLocalizedMessage()));
        }
    }
 
    @RequiresPermissions("FinInStat:*")
    @RequestMapping("/countReceipts")
    public String CountReceipts(@RequestParam(value = "pageNo") int pageNo,
            @RequestParam(value = "handicapCode", required = false) String handicapCode,
            @RequestParam(value = "account", required = false) String account,
            @RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
            @RequestParam(value = "fieldval", required = false) String fieldval,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
        try {
            String pageSise = pageSize != null ? pageSize.toString() : Integer.toString(AppConstants.PAGE_SIZE);
            // 拼接时间戳查询数据条件
            String fristTime = null;
            String lastTime = null;
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
                fristTime = startTime;
                lastTime = endTime;
            } else if ("thisWeek".equals(fieldval)) {
                // 本周的数据
                // fieldval = getTimesWeekmorning().toString();
                // whereTransactionValue = getTimesWeeknight().toString();
                // 昨日数据
                fristTime = TimeChangeCommon.getYesterdayStartTime();
                lastTime = TimeChangeCommon.getYesterdayEndTime();
            } else if ("thisMonth".equals(fieldval)) {
                // 本月的数据
                fristTime = TimeChangeCommon.getTimesMonthmorning();
                lastTime = TimeChangeCommon.getTimesMonthnight();
            } else if ("lastMonth".equals(fieldval)) {
                // 上月的数据
                fristTime = TimeChangeCommon.getLastMonthStartMorning();
                lastTime = TimeChangeCommon.getLastMonthEndMorning();
            } else if ("week".equals(fieldval)) {
                fristTime = TimeChangeCommon.getTimesWeekmorning();
                lastTime = TimeChangeCommon.getTimesWeeknight();
            } else if ("lastweek".equals(fieldval)) {
                fristTime = TimeChangeCommon.getPreviousWeekday();
                lastTime = TimeChangeCommon.getTimesWeekmorningAt6();
            }
            if (0 != startAndEndTimeToArray.length && null != startAndEndTimeToArray) {
                fristTime = startAndEndTimeToArray[0];
                lastTime = startAndEndTimeToArray[1];
            }
            Object object = finInStatisticsService.CountReceipts(pageNo, handicapCode, account,
                    TimeChangeCommon.TimeStamp2Date(fristTime, "yyyy-MM-dd HH:mm:ss"),
                    TimeChangeCommon.TimeStamp2Date(lastTime, "yyyy-MM-dd HH:mm:ss"), type, pageSise);
            return mapper.writeValueAsString(object);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("统计微信入款 查询发生错误" + e);
            return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
                    "操作失败  " + e.getLocalizedMessage()));
        }
    }
 
    @RequiresPermissions("FinInStat:*")
    @RequestMapping("/sysDetail")
    public String sysDetail(@RequestParam(value = "pageNo") int pageNo,
            @RequestParam(value = "account", required = false) String account,
            @RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
            @RequestParam(value = "fromAmount", required = false) BigDecimal fromAmount,
            @RequestParam(value = "toAmount", required = false) BigDecimal toAmount,
            @RequestParam(value = "orderNo", required = false) String orderNo,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
        try {
            String pageSise = pageSize != null ? pageSize.toString() : Integer.toString(AppConstants.PAGE_SIZE);
            // 拼接时间戳查询数据条件
            String fristTime = null;
            String lastTime = null;
            if (0 != startAndEndTimeToArray.length && null != startAndEndTimeToArray) {
                fristTime = startAndEndTimeToArray[0];
                lastTime = startAndEndTimeToArray[1];
            }
            Object object = finInStatisticsService.sysDetail(pageNo, account,
                    TimeChangeCommon.TimeStamp2Date(fristTime, "yyyy-MM-dd HH:mm:ss"),
                    TimeChangeCommon.TimeStamp2Date(lastTime, "yyyy-MM-dd HH:mm:ss"),
                    null == fromAmount ? new BigDecimal(0) : fromAmount,
                    null == toAmount ? new BigDecimal(0) : toAmount, orderNo, type, pageSise);
            return mapper.writeValueAsString(object);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("统计微信入款 查询发生错误" + e);
            return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
                    "操作失败  " + e.getLocalizedMessage()));
        }
    }
 
}