package com.xinbo.fundstransfer.restful;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import com.xinbo.fundstransfer.sec.FundTransferEncrypter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.apache.shiro.web.util.WebUtils;
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
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.common.TimeChangeCommon;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.FinLessStatService;
import com.xinbo.fundstransfer.service.SysDataPermissionService;
import com.xinbo.fundstransfer.service.SysUserService;
import com.xinbo.fundstransfer.service.SystemSettingService;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.FinFrostLessStat;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.entity.SysUserProfile;
import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import com.xinbo.fundstransfer.domain.enums.AccountTraceType;

/**
 * 亏损请求接口
 * 
 * @author Steven
 *
 */
@Slf4j
@RestController
@RequestMapping("/r/finLessStat")
public class FinLessStatController extends BaseController {
	@Autowired
	private FinLessStatService finLessStatService;
	@Autowired
	private SysDataPermissionService sysDataPermissionService;
	@Autowired
	private SysUserService sysUserService;
	@Autowired
	private AccountService accountService;
	@Autowired
	private SystemSettingService systemSettingService;
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
	 * 入款亏损
	 * 
	 * @param pageNo
	 *            页码
	 * @param handicap
	 *            盘口
	 * @param level
	 *            层级
	 * @param accounttype
	 * @param account
	 *            账号
	 * @param startAndEndTimeToArray
	 *            时间控件数组值
	 * @param fieldval
	 *            时间单选按钮值
	 * @param type
	 *            哪个数据源（冻结卡亏损、入款亏损、中转亏损、出款亏损、盘口亏损）
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequiresPermissions("FinLessStat:*")
	@RequestMapping("/fininless")
	public String FininlessStatistics(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "handicap", required = false) int handicap,
			@RequestParam(value = "level", required = false) int level,
			@RequestParam(value = "accounttype", required = false) int accounttype,
			@RequestParam(value = "account", required = false) String account,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "fieldval", required = false) String fieldval,
			@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "cartype", required = false) String cartype,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					Sort.Direction.DESC, "bank_balance");
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
			String whereAccount = "";
			if (!"".equals(account) && !"".equals(account)) {
				whereAccount = account;
			} else {
				whereAccount = null;
			}
			if ("".equals(cartype)) {
				cartype = null;
			}
			// 参数存在map里面传过去 调用共同的service 根据type判断处理
			// 获取当前用户盘口 进行权限限制
			SysUser loginUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			List<Integer> handicapList = getHandicapIdByCurrentUser(loginUser.getId() == 1 ? 0 : handicap, loginUser);
			Map<String, Object> parmap = new HashMap<String, Object>();
			parmap.put("handicap", handicapList);
			parmap.put("level", level);
			parmap.put("Accounttype", accounttype);
			parmap.put("whereAccount", whereAccount);
			parmap.put("fristTime", TimeChangeCommon.TimeStamp2Date(fristTime, "yyyy-MM-dd HH:mm:ss"));
			parmap.put("lastTime", TimeChangeCommon.TimeStamp2Date(lastTime, "yyyy-MM-dd HH:mm:ss"));
			parmap.put("fieldval", TimeChangeCommon.TimeStamp2Date(fieldval, "yyyy-MM-dd HH:mm:ss"));
			parmap.put("whereTransactionValue",
					TimeChangeCommon.TimeStamp2Date(whereTransactionValue, "yyyy-MM-dd HH:mm:ss"));
			parmap.put("type", type);
			parmap.put("pageRequest", pageRequest);
			parmap.put("cartype", cartype);
			Map<String, Object> mapp = finLessStatService.findFinInLessStatis(parmap);
			Map<String, Object> map = new LinkedHashMap<>();
			// 冻结卡数据
			Page<Object> frostPage = (Page<Object>) mapp.get("frostPage");
			if (null != frostPage) {
				List<Object> FinFrostLessStatList = frostPage.getContent();
				List<FinFrostLessStat> arrlist = new ArrayList<FinFrostLessStat>();
				for (int i = 0; i < FinFrostLessStatList.size(); i++) {
					Object[] obj = (Object[]) FinFrostLessStatList.get(i);
					FinFrostLessStat finFrostLessStat = new FinFrostLessStat();
					finFrostLessStat.setAlias((String) obj[0]);
					finFrostLessStat.setHandicap((String) obj[2]);
					finFrostLessStat.setLevel((String) obj[3]);
					finFrostLessStat.setType((int) obj[4]);
					finFrostLessStat.setAccount((String) obj[5]);
					finFrostLessStat.setBankname((String) obj[6]);
					finFrostLessStat.setBankbalance((BigDecimal) obj[7]);
					finFrostLessStat.setId((int) obj[8]);
					finFrostLessStat.setOwner((String) obj[9]);
					finFrostLessStat.setBanktype((String) obj[10]);
					finFrostLessStat.setRemark(StringUtils.isNotBlank((String) obj[11])
							? ((String) obj[11]).replace("\r\n", "<br>").replace("\n", "<br>") : "");
					finFrostLessStat.setCurrSysLeval(null == obj[12] ? 0 : (int) obj[12]);
					finFrostLessStat.setBalance((BigDecimal) obj[13]);
					String newStr = obj[11] == null ? ""
							: obj[11].toString().substring(obj[11].toString().lastIndexOf("【转冻结】") + 5,
									obj[11].toString().length());
					finFrostLessStat.setCause(obj[11] == null ? (obj[4].toString().equals("1") ? "平台冻结" : "")
							: newStr.equals("") ? "系统检测卡片异常，自动转冻结。" : newStr);
					arrlist.add(finFrostLessStat);
				}
				map.put("frostarrlist", arrlist);
				map.put("frosttotal", mapp.get("frosttotal"));
				map.put("frostPage", new Paging(frostPage));
			}
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("亏损统计Controller查询失败" + e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/finpending")
	public String FininlessPending(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "handicap", required = false) int handicap,
			@RequestParam(value = "level", required = false) int level,
			@RequestParam(value = "accounttype", required = false) int accounttype,
			@RequestParam(value = "account", required = false) String account,
			@RequestParam(value = "alias", required = false) String alias,
			@RequestParam(value = "startAndEndTimeToArray", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "fieldval", required = false) String fieldval,
			@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "cartype", required = false) String cartype,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "disposeType", required = false) String disposeType,
			@RequestParam(value = "statusType", required = false) String statusType,
			@RequestParam(value = "jdType", required = false) String jdType,
			@RequestParam(value = "classification", required = false) String classification,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE);
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
			String whereAccount = "";
			if (!"".equals(account) && !"".equals(account)) {
				whereAccount = account;
			} else {
				whereAccount = null;
			}
			if ("".equals(cartype)) {
				cartype = null;
			}
			if ("".equals(statusType)) {
				statusType = null;
			}
			if ("".equals(jdType)) {
				jdType = null;
			}
			if ("".equals(classification)) {
				classification = null;
			}
			// 参数存在map里面传过去 调用共同的service 根据type判断处理
			// 获取当前用户盘口 进行权限限制
			SysUser loginUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			List<Integer> handicapList = getHandicapIdByCurrentUser(loginUser.getId() == 1 ? 0 : handicap, loginUser);
			Map<String, Object> parmap = new HashMap<String, Object>();
			parmap.put("handicap", handicapList);
			parmap.put("level", level);
			parmap.put("Accounttype", accounttype);
			parmap.put("whereAccount", whereAccount);
			parmap.put("alias", alias);
			parmap.put("fristTime", TimeChangeCommon.TimeStamp2Date(fristTime, "yyyy-MM-dd HH:mm:ss"));
			parmap.put("lastTime", TimeChangeCommon.TimeStamp2Date(lastTime, "yyyy-MM-dd HH:mm:ss"));
			parmap.put("fieldval", TimeChangeCommon.TimeStamp2Date(fieldval, "yyyy-MM-dd HH:mm:ss"));
			parmap.put("whereTransactionValue",
					TimeChangeCommon.TimeStamp2Date(whereTransactionValue, "yyyy-MM-dd HH:mm:ss"));
			parmap.put("type", type);
			parmap.put("pageRequest", pageRequest);
			parmap.put("cartype", cartype);
			parmap.put("classification", classification);
			parmap.put("status", status);
			parmap.put("disposeType", disposeType);
			parmap.put("statusType", statusType);
			parmap.put("jdType", jdType);
			Map<String, Object> mapp = finLessStatService.findFinPending(parmap);
			Map<String, Object> map = new LinkedHashMap<>();
			// 冻结卡数据
			Page<Object> frostPage = (Page<Object>) mapp.get("frostPage");
			if (null != frostPage) {
				List<Object> FinFrostLessStatList = frostPage.getContent();
				List<FinFrostLessStat> arrlist = new ArrayList<FinFrostLessStat>();
				for (int i = 0; i < FinFrostLessStatList.size(); i++) {
					Object[] obj = (Object[]) FinFrostLessStatList.get(i);
					SysUser sysUser = sysUserService
							.findFromCacheById(obj[15] == null ? 0 : Integer.parseInt(obj[15].toString()));
					FinFrostLessStat finFrostLessStat = new FinFrostLessStat();
					BizAccount acc = accountService.findById(null, Integer.parseInt(obj[8].toString()));
					if (null != acc)
						finFrostLessStat.setFlag(acc.getFlag().toString());
					finFrostLessStat.setAlias((String) obj[0]);
					finFrostLessStat.setHandicap((String) obj[2]);
					finFrostLessStat.setLevel((String) obj[3]);
					finFrostLessStat.setType((int) obj[4]);
					finFrostLessStat.setAccount((String) obj[5]);
					finFrostLessStat.setBankname((String) obj[6]);
					finFrostLessStat.setBankbalance((BigDecimal) obj[7]);
					finFrostLessStat.setId((int) obj[8]);
					finFrostLessStat.setOwner((String) obj[9]);
					finFrostLessStat.setBanktype((String) obj[10]);
					finFrostLessStat.setRemark(StringUtils.isNotBlank((String) obj[11])
							? ((String) obj[11]).replace("\r\n", "<br>").replace("\n", "<br>") : "");
					finFrostLessStat.setCurrSysLeval(null == obj[12] ? 0 : (int) obj[12]);
					finFrostLessStat.setBalance((BigDecimal) obj[13]);
					finFrostLessStat.setSbuType(null == obj[22] ? "" : obj[22].toString());
					String newStr = obj[11] == null ? ""
							: obj[11].toString().substring(obj[11].toString().lastIndexOf("【转冻结】") + 5,
									obj[11].toString().length());
					finFrostLessStat.setCause(obj[11] == null ? (obj[4].toString().equals("1") ? "平台冻结" : "")
							: newStr.equals("") ? "系统检测卡片异常，自动转冻结。" : newStr);
					finFrostLessStat.setCreateTime((String) obj[14]);
					finFrostLessStat.setOperator(sysUser == null ? "" : sysUser.getUid());
					finFrostLessStat.setPendingRemark(StringUtils.isNotBlank((String) obj[16])
							? ((String) obj[16]).replace("\r\n", "<br>").replace("\n", "<br>") : "");
					if (status.equals("3")) {
						int operEnd = obj[16].toString().indexOf("永久删除") == -1
								? (obj[16].toString().indexOf("解冻恢复使用") == -1 ? obj[16].toString().indexOf("持续冻结")
										: obj[16].toString().indexOf("解冻恢复使用"))
								: obj[16].toString().indexOf("永久删除");
						int operStart = obj[16].toString().indexOf("(账号处理)");
						if (operEnd != -1)
							finFrostLessStat.setConfirmor(obj[16].toString().substring(operStart + 6, operEnd));
					}
					finFrostLessStat.setPendingStatus(obj[17].toString());
					finFrostLessStat.setPendingId((int) obj[18]);
					finFrostLessStat.setStatus((int) obj[19]);
					finFrostLessStat.setAmount(new BigDecimal(obj[20] == null ? "0" : obj[20].toString()));
					finFrostLessStat.setDefrostType(obj[21] == null ? ""
							: AccountTraceType.findByTypeId(Integer.parseInt(obj[21].toString())).getMsg());
					if (obj[16] != null && obj[16].toString().lastIndexOf("(盘口驳回)") == -1)
						finFrostLessStat.setShowPassword(
								obj[16] == null ? "no" : (obj[16].toString().indexOf("显示密码：是") == -1 ? "no" : "yes"));
					else if (obj[16] != null) {
						String str = obj[16] == null ? null
								: obj[16].toString().substring(obj[16].toString().lastIndexOf("(盘口驳回)"),
										obj[16].toString().length());
						finFrostLessStat
								.setShowPassword(str == null ? "no" : (str.indexOf("显示密码：是") == -1 ? "no" : "yes"));
					}
					arrlist.add(finFrostLessStat);
				}
				map.put("frostarrlist", arrlist);
				map.put("frosttotal", mapp.get("frosttotal"));
				map.put("frostPage", new Paging(frostPage));
			}
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("亏损统计Controller查询失败" + e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequiresPermissions("FinLessStat:*")
	@RequestMapping("/findHistory")
	public String FindHistory(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "accountid", required = false) int accountid,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					Sort.Direction.ASC, "id");
			Map<String, Object> map = finLessStatService.findHistory(accountid, pageRequest);
			// 冻结卡数据
			Page<Object> frostPage = (Page<Object>) map.get("HistoryPage");
			if (null != frostPage) {
				List<Object> FinFrostLessStatList = frostPage.getContent();
				List<FinFrostLessStat> arrlist = new ArrayList<FinFrostLessStat>();
				for (int i = 0; i < FinFrostLessStatList.size(); i++) {
					Object[] obj = (Object[]) FinFrostLessStatList.get(i);
					FinFrostLessStat finFrostLessStat = new FinFrostLessStat();
					finFrostLessStat.setRemark(StringUtils.isNotBlank((String) obj[2])
							? ((String) obj[2]).replace("\r\n", "<br>").replace("\n", "<br>") : "");
					finFrostLessStat.setTime((String) obj[3]);
					finFrostLessStat.setOperator((String) obj[4]);
					arrlist.add(finFrostLessStat);
				}
				map.put("historylist", arrlist);
				map.put("historyPage", new Paging(frostPage));
			}
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("账号操作历史记录统计Controller查询失败" + e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/freezingProcess")
	public String freezingProcess(@RequestParam(value = "accountid") Long accountid,
			@RequestParam(value = "traceId") Long traceId,
			@RequestParam(value = "remark", required = false) String remark,
			@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "derating", required = false) String derating,
			@RequestParam(value = "jAmount", required = false) BigDecimal jAmount) throws JsonProcessingException {
		try {
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			// 查找旧的备注
			String oldRmark = finLessStatService.findOldRemark(traceId);
			// 查询状态
			String status = finLessStatService.findStatus(traceId);
			if ("3".equals(status))
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "已经是完成状态！"));
			finLessStatService.freezingProcess(accountid, traceId, operator.getUid(), remark, oldRmark, type, jAmount,
					derating);
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功"));
		} catch (Exception e) {
			log.error("操作失败：异常 ", e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getMessage()));
		}
	}

	@RequestMapping("/updateAccountStatus")
	public String updateAccountStatus(@RequestParam(value = "accountid") Long accountid)
			throws JsonProcessingException {
		try {
			BizAccount account = accountService.getById(Integer.parseInt(accountid.toString()));
			if (account.getStatus() != AccountStatus.Freeze.getStatus())
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "不是冻结状态，不能操作！"));
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			finLessStatService.updateAccountStatus(accountid, operator);
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功"));
		} catch (Exception e) {
			log.error("操作失败：异常 ", e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getMessage()));
		}
	}

	@RequestMapping("/jiedongmoney")
	public String jieDongMoney(@RequestParam(value = "id") Long id,
			@RequestParam(value = "amount", required = false) BigDecimal amount,
			@RequestParam(value = "remark", required = false) String remark,
			@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "particulars", required = false) String particulars) throws JsonProcessingException {
		try {
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			// 查找旧的备注
			String oldRmark = finLessStatService.findOldRemark(id);
			// 查询状态
			String status = finLessStatService.findStatus(id);
			if (!status.equals("1"))
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "已经处理,请刷新！ "));
			finLessStatService.jieDongMoney(
					operator.getId(), (oldRmark + "\r\n" + CommonUtils.getDateStr(new Date()) + " " + operator.getUid()
							+ "(解冻方式)\r\n" + (particulars + "\r\n" + "解冻金额：" + amount + " 备注：" + remark + "(处理)")),
					id, amount, type);
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功"));
		} catch (Exception e) {
			log.error("操作失败：异常 ", e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getMessage()));
		}
	}

	@RequestMapping("/accomplish")
	public String accomplish(@RequestParam(value = "id") Long id,
			@RequestParam(value = "remark", required = false) String remark,
			@RequestParam(value = "status", required = false) String status) throws JsonProcessingException {
		try {
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			// 查询状态
			String orderStatus = finLessStatService.findStatus(id);
			if (status.equals("1")) {
				if (!orderStatus.equals("2"))
					return mapper.writeValueAsString(
							new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "已经处理,请刷新！ "));
			} else if (status.equals("0")) {
				if (!orderStatus.equals("1"))
					return mapper.writeValueAsString(
							new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "已经处理,请刷新！ "));
			}
			// 查找旧的备注
			String oldRmark = finLessStatService.findOldRemark(id);
			finLessStatService.accomplish(CommonUtils.genRemark(oldRmark,
					remark + (status.equals("1") ? "(财务驳回)" : "(盘口驳回)"), new Date(), operator.getUid()), id, status);
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功"));
		} catch (Exception e) {
			log.error("操作失败：异常 ", e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getMessage()));
		}
	}

	@RequestMapping("/cashflow")
	public String cashflow(@RequestParam(value = "traceId") Long traceId,
			@RequestParam(value = "remark", required = false) String remark,
			@RequestParam(value = "type", required = false) String type) throws JsonProcessingException {
		try {
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			// 查询状态
			String status = finLessStatService.findStatus(traceId);
			if (!status.equals("0"))
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "已经处理,请刷新！ "));
			// 查找旧的备注
			String oldRmark = finLessStatService.findOldRemark(traceId);
			finLessStatService.cashflow(CommonUtils.genRemark(oldRmark,
					remark + "(金流处理)" + (type.equals("yes") ? " 显示密码：是" : ""), new Date(), operator.getUid()), traceId);
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功"));
		} catch (Exception e) {
			log.error("操作失败：异常 ", e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getMessage()));
		}
	}

	@RequestMapping("/findPassword")
	public String list(@RequestParam(value = "alias") String alias, @RequestParam(value = "pendingId") Long pendingId)
			throws JsonProcessingException {
		String params = buildParams().toString();
		log.trace(String.format("%s，参数：%s", "账号获取", params));
		try {
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (StringUtils.isAnyBlank(alias)) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请输入编号"));
			}
			if (operator == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请先登陆"));
			}
			// 查询状态
			String status = finLessStatService.findStatus(pendingId);
			if (checkIp(status)) {
				List<BizAccount> accounts = accountService.findByAlias(alias);
				if (accounts != null && accounts.size() > 0) {
					BizAccount account = accounts.get(0);
					if (account.getStatus() != AccountStatus.Freeze.getStatus()) {
						return mapper.writeValueAsString(new GeneralResponseData(
								GeneralResponseData.ResponseStatus.FAIL.getValue(), "此卡不是冻结状态！"));
					}

					if (!status.equals("0") && !status.equals("1")) {
						return mapper.writeValueAsString(new GeneralResponseData(
								GeneralResponseData.ResponseStatus.FAIL.getValue(), "此卡不在金流处理或者盘口处理状态！"));
					}
					String result = "账号：" + account.getAccount();
					if (StringUtils.isNotBlank(account.getSign_())) {
						result += "<br/>登录名：" + FundTransferEncrypter.decryptDb(account.getSign_());
					}
					if (StringUtils.isNotBlank(account.getHook_())) {
						result += "<br/>登录密码：" + FundTransferEncrypter.decryptDb(account.getHook_());
					}
					if (StringUtils.isNotBlank(account.getHub_())) {
						result += "<br/>支付密码：" + FundTransferEncrypter.decryptDb(account.getHub_());
					}
					if (StringUtils.isNotBlank(account.getBing_())) {
						result += "<br/>U盾密码：" + FundTransferEncrypter.decryptDb(account.getBing_());
					}
					return mapper.writeValueAsString(
							new GeneralResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), result));
				} else {
					return mapper.writeValueAsString(
							new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "账号不存在"));
				}
			} else {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "没有权限查看密码！"));
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error(String.format("%s，参数：%s，结果：%s", "账号获取", params, e.getMessage()));
			return ("操作失败  " + e.getLocalizedMessage());
		}
	}

	/**
	 * 用途为手机时候 检测当前主机IP是否在系统设置中 没有则无权限
	 * 
	 * @throws Exception
	 */
	private boolean checkIp(String status) throws Exception {
		if (Objects.isNull(status) || Objects.equals(status, "1")) {
			return true;
		}
		boolean CheckIP = false;
		List<SysUserProfile> list = systemSettingService.findByPropertyKey("SUPERVISOR_OPEN_ACCOUNT_PIN_IPS");
		String ipStr = null;
		if (null != list && list.size() > 0) {
			for (SysUserProfile sysProperty : list) {
				if (sysProperty.getPropertyKey().equals("SUPERVISOR_OPEN_ACCOUNT_PIN_IPS")) {
					ipStr = sysProperty.getPropertyValue();
				}
			}
		}
		log.info("ipStr ipStr ipStr :{},remoteIp:{}", ipStr, CommonUtils.getRemoteIp(request));
		if (StringUtils.isNotBlank(ipStr)) {
			String[] ipArray = ipStr.split(",");
			if (null != ipArray && ipArray.length > 0 && Arrays.asList(ipArray)
					.contains(new SimpleCookie("JIP").readValue(WebUtils.toHttp(request), null))) {
				CheckIP = true;
			}
		}
		if (!CheckIP) {
			log.error("未授权,ip:{}", CommonUtils.getRemoteIp(request));
		}
		return CheckIP;
	}

	@RequestMapping("/findCountsById")
	public String findCountsById(@RequestParam(value = "accountId", required = false) String accountId,
			@RequestParam(value = "accountIds", required = false) String[] accountIds) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			if (accountIds != null && accountIds.length > 0) {
				String message = "";
				for (int i = 0; i < accountIds.length; i++) {
					BizAccount account = accountService.getById(Integer.parseInt(accountIds[i].toString()));
					// 不能删除客户卡
					// if (account.getType() == 14) {
					// message += "账号：" + addAccount(account.getAccount()) + "
					// 编号：" + account.getAlias() + "</br>";
					//
					// }
					// 查询是否存在未处理的冻结数据(倒序查询最新时间)
					int count = finLessStatService.findCountsById(account.getId(), "portion");
					// 如果是银行卡，查看是否存在流水
					if (account.getType() == 1 || account.getType() == 5 || account.getType() == 6
							|| account.getType() == 7 || account.getType() == 8 || account.getType() == 9
							|| account.getType() == 10 || account.getType() == 11 || account.getType() == 12
							|| account.getType() == 13 || account.getType() == 14) {
						String createTime = finLessStatService.findCarCountsById(account.getId());
						if (null != createTime && !createTime.equals("")) {
							Date dtf1 = df.parse(createTime);
							Date dtf2 = df.parse(df.format((DateUtils.addDays(new Date(), -30))));
							if (dtf1.getTime() > dtf2.getTime()) {
								message += "账号：" + addAccount(account.getAccount()) + " 编号：" + account.getAlias()
										+ "</br>";
							}
						}
					} else {
						// 判断是否为空，时间是否在三十天之内
						String createTime = finLessStatService.findThirdCountsById(account.getId().toString());
						if (null != createTime && !createTime.equals("")) {
							Date dtf1 = df.parse(createTime);
							Date dtf2 = df.parse(df.format((DateUtils.addDays(new Date(), -30))));
							if (dtf1.getTime() > dtf2.getTime()) {
								message += "账号：" + addAccount(account.getAccount()) + " 编号：" + account.getAlias()
										+ "</br>";

							}
						}
					}
					if (count > 0) {
						message += "账号：" + addAccount(account.getAccount()) + " 编号：" + account.getAlias() + "</br>";
					}
				}
				if (message.equals("")) {
					return mapper.writeValueAsString(responseData);
				} else {
					return mapper.writeValueAsString(
							new GeneralResponseData(3, "存在未处理的冻结数据或者一个月内存在银行流水或者存在提单</br> " + message));
				}
			} else {
				String message = "";
				BizAccount account = accountService.getById(Integer.parseInt(accountId));
				// if (account.getType() == 14) {
				// message += "是客户卡不能删除！";
				//
				// }
				// 查询是否存在未处理的冻结数据
				int count = finLessStatService.findCountsById(Integer.parseInt(accountId), "portion");
				// 如果是银行卡，查看是否存在流水
				if (account.getType() == 1 || account.getType() == 5 || account.getType() == 6 || account.getType() == 7
						|| account.getType() == 8 || account.getType() == 9 || account.getType() == 10
						|| account.getType() == 11 || account.getType() == 12 || account.getType() == 13
						|| account.getType() == 14) {
					String createTime = finLessStatService.findCarCountsById(Integer.parseInt(accountId));
					if (null != createTime && !createTime.equals("")) {
						Date dtf1 = df.parse(createTime);
						Date dtf2 = df.parse(df.format((DateUtils.addDays(new Date(), -30))));
						if (dtf1.getTime() > dtf2.getTime()) {
							message += "一个月内存在银行流水，请排查。";
						}
					}
				} else {
					// 判断是否为空，时间是否在三十天之内
					String createTime = finLessStatService.findThirdCountsById(accountId);
					if (null != createTime && !createTime.equals("")) {
						Date dtf1 = df.parse(createTime);
						Date dtf2 = df.parse(df.format((DateUtils.addDays(new Date(), -30))));
						if (dtf1.getTime() > dtf2.getTime()) {
							message += "一个月内存在第三方入款单，请排查。";

						}
					}
				}
				if (count > 0) {
					message += "存在未处理的冻结数据";
				}
				if (!message.equals("")) {
					return mapper.writeValueAsString(new GeneralResponseData(3,
							message + "</br>账号：" + addAccount(account.getAccount()) + " 编号：" + account.getAlias()));
				}
				return mapper.writeValueAsString(responseData);
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("账号操作历史记录统计Controller查询失败" + e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	private static String addAccount(String account) {
		if (account.length() >= 8) {
			return account.toString().substring(0, 4) + "**"
					+ account.toString().substring(account.toString().length() - 4);
		} else if (account.toString().length() >= 4) {
			return account.toString().substring(0, 2) + "**"
					+ account.toString().substring(account.toString().length() - 2);
		} else if (account.toString().length() >= 2) {
			return account.toString().substring(0, 1) + "**"
					+ account.toString().substring(account.toString().length() - 1);
		} else {
			return "";
		}
	}

}
