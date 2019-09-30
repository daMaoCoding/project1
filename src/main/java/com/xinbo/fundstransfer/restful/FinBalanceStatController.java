package com.xinbo.fundstransfer.restful;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
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
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.FinBalanceStatService;
import com.xinbo.fundstransfer.domain.entity.FinInStat;
import com.xinbo.fundstransfer.domain.entity.FinTransStatMatch;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.AccountReturnSummary;

import java.util.Map;

import com.xinbo.fundstransfer.domain.entity.AccountStatistics;
import com.xinbo.fundstransfer.domain.entity.FinBalanceStatCard;

/**
 * 余额明细请求接口
 * 
 * @author Steven
 *
 */
@Slf4j
@RestController
@RequestMapping("/r/finbalancestat")
public class FinBalanceStatController extends BaseController {
	@Autowired
	private FinBalanceStatService finBalanceStatService;
	@Autowired
	private AccountService accountService;
	@Autowired
	private ObjectMapper mapper = new ObjectMapper();

	/**
	 * 余额明细
	 * 
	 * @param pageNo
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequiresPermissions("FinBalanceStat:*")
	@RequestMapping("/finbalancestat")
	public String FinMoreLevelStat(@RequestParam(value = "pageNo") int pageNo) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, AppConstants.PAGE_SIZE_MAX, Sort.Direction.ASC, "id");
			Map<String, Object> mapp = finBalanceStatService.finBalanceStat(pageRequest);
			Map<String, Object> map = new LinkedHashMap<>();
			Page<Object> Page = (Page<Object>) mapp.get("Page");
			List<FinInStat> arrlist = new ArrayList<FinInStat>();
			if (null != Page) {
				List<Object> AccountStatisticsList = Page.getContent();
				for (int i = 0; i < AccountStatisticsList.size(); i++) {
					Object[] obj = (Object[]) AccountStatisticsList.get(i);
					FinInStat FinInStat = new FinInStat();
					FinInStat.setId(new Integer(String.valueOf(obj[0])));
					FinInStat.setHandicapname((String) obj[1]);
					FinInStat.setBankUse(new BigDecimal(obj[2].toString()));
					FinInStat.setBankStop(new BigDecimal(obj[3].toString()));
					FinInStat.setBankCanUse(new BigDecimal(obj[4].toString()));
					FinInStat.setSysUse(new BigDecimal(obj[5].toString()));
					FinInStat.setSysStop(new BigDecimal(obj[6].toString()));
					FinInStat.setSysCanUse(new BigDecimal(obj[7].toString()));
					arrlist.add(FinInStat);
				}
			}
			map.put("arrlist", arrlist);
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("余额明细Controlle查询失败" + e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/finbalanceEveryDay")
	public String finbalanceEveryDay(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "handicapId") int handicapId,
			@RequestParam(value = "startAndEndTime", required = false) String[] startAndEndTime)
			throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, AppConstants.PAGE_SIZE_MAX, Sort.Direction.ASC, "id");
			String fristTime = null;
			String lastTime = null;
			if (0 != startAndEndTime.length && null != startAndEndTime) {
				fristTime = startAndEndTime[0];
				lastTime = startAndEndTime[1];
			}
			Map<String, Object> mapp = finBalanceStatService.finbalanceEveryDay(handicapId, fristTime, lastTime,
					pageRequest);
			Map<String, Object> map = new LinkedHashMap<>();
			Page<Object> Page = (Page<Object>) mapp.get("Page");
			List<FinInStat> arrlist = new ArrayList<FinInStat>();
			if (null != Page) {
				List<Object> AccountStatisticsList = Page.getContent();
				for (int i = 0; i < AccountStatisticsList.size(); i++) {
					Object[] obj = (Object[]) AccountStatisticsList.get(i);
					FinInStat FinInStat = new FinInStat();
					FinInStat.setId(new Integer(String.valueOf(obj[0])));
					FinInStat.setHandicapname((String) obj[1]);
					FinInStat.setBankUse(new BigDecimal(obj[3].toString()));
					FinInStat.setBankStop(new BigDecimal(obj[4].toString()));
					FinInStat.setBankCanUse(new BigDecimal(obj[5].toString()));
					FinInStat.setSysUse(new BigDecimal(obj[6].toString()));
					FinInStat.setSysStop(new BigDecimal(obj[7].toString()));
					FinInStat.setSysCanUse(new BigDecimal(obj[8].toString()));
					FinInStat.setBanktype((String) obj[2]);
					arrlist.add(FinInStat);
				}
			}
			map.put("arrlist", arrlist);
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("余额明细Controlle查询失败" + e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/findBalanceDetail")
	public String findBalanceDetail(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "account") String account, @RequestParam(value = "bankType") String bankType,
			@RequestParam(value = "handicap") int handicap, @RequestParam(value = "time") String time,
			@RequestParam(value = "type") int type, @RequestParam(value = "status") int status,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE);
			if ("".equals(account)) {
				account = null;
			}
			if ("".equals(bankType)) {
				bankType = null;
			}
			Map<String, Object> mapp = finBalanceStatService.findBalanceDetail(account, bankType, handicap, time, type,
					status, pageRequest);
			Map<String, Object> map = new LinkedHashMap<>();
			Page<Object> rebatePage = (Page<Object>) mapp.get("rebatePage");
			if (null != rebatePage) {
				List<Object> rebateList = rebatePage.getContent();
				List<FinInStat> arrlist = new ArrayList<FinInStat>();
				for (int i = 0; i < rebateList.size(); i++) {
					Object[] obj = (Object[]) rebateList.get(i);
					FinInStat FinInStat = new FinInStat();
					FinInStat.setTime(obj[4].toString());
					AccountBaseInfo bizAccount = accountService.getFromCacheById(Integer.parseInt(obj[0].toString()));
					FinInStat.setId(bizAccount.getId());
					FinInStat.setAccount(bizAccount.getAccount());
					FinInStat.setAlias(null == bizAccount.getAlias() ? "" : bizAccount.getAlias());
					FinInStat.setBanktype(null == bizAccount.getBankType() ? "" : bizAccount.getBankType());
					FinInStat.setBankBalance(new BigDecimal(obj[1].toString()));
					FinInStat.setAmount(new BigDecimal(obj[2].toString()));
					arrlist.add(FinInStat);
				}
				map.put("rebatelist", arrlist);
				map.put("rebateTotal", mapp.get("rebateTotal"));
				map.put("rebatePage", new Paging(rebatePage));
			}
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("余额明细Controlle查询失败" + e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 余额明细>明细
	 * 
	 * @param pageNo
	 *            页码
	 * @param id
	 *            标识查询不同的数据源
	 * @param account
	 *            账号
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequiresPermissions("FinBalanceStat:*")
	@RequestMapping("/finbalancestatcard")
	public String FinBalanceStatCard(@RequestParam(value = "pageNo") int pageNo, @RequestParam(value = "id") int id,
			@RequestParam(value = "account") String account, @RequestParam(value = "status") int status,
			@RequestParam(value = "bankType") String bankType,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					Sort.Direction.DESC, "bank_balance");
			if ("".equals(account) || "" == account) {
				account = null;
			}
			if ("".equals(bankType) || "" == bankType) {
				bankType = null;
			}
			Map<String, Object> mapp = finBalanceStatService.finBalanceStatCard(id, account, status, bankType,
					pageRequest);
			Map<String, Object> map = new LinkedHashMap<>();
			Page<Object> Page = (Page<Object>) mapp.get("Page");
			List<FinBalanceStatCard> arrlist = new ArrayList<FinBalanceStatCard>();
			if (null != Page) {
				List<Object> AccountStatisticsList = Page.getContent();
				for (int i = 0; i < AccountStatisticsList.size(); i++) {
					Object[] obj = (Object[]) AccountStatisticsList.get(i);
					FinBalanceStatCard finBalanceStatCard = new FinBalanceStatCard();
					finBalanceStatCard.setId((int) obj[0]);
					finBalanceStatCard.setAccount((String) obj[1]);
					finBalanceStatCard.setType((int) obj[2]);
					finBalanceStatCard.setBankname(obj[3] == null ? "" : (String) obj[3]);
					finBalanceStatCard.setBalance((BigDecimal) obj[4]);
					finBalanceStatCard.setBankbalance((BigDecimal) obj[5]);
					if (7 != id) {
						finBalanceStatCard.setAlias(null == obj[6] ? "" : (String) obj[6]);
						finBalanceStatCard.setOwner((String) obj[7]);
						finBalanceStatCard.setBanktype((String) obj[8]);
					}
					arrlist.add(finBalanceStatCard);
				}
			}
			map.put("total", mapp.get("total"));
			map.put("page", new Paging(Page));
			map.put("arrlist", arrlist);
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("余额明细》明细Controller查询失败" + e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 余额明细>明细>系统明细(银行明细 根据type判断 查询哪个数据源)
	 * 
	 * @param pageNo
	 *            页码
	 * @param to_account
	 *            汇出账号
	 * @param from_account
	 *            汇入账号
	 * @param startAndEndTimeToArray
	 *            时间控件数组值
	 * @param startamount
	 *            开始金额
	 * @param endamount
	 *            结束金额
	 * @param accountid
	 *            账号id
	 * @param id
	 *            查询不同数据源的标识（入款账号、出款账号、备用金、现金卡..）
	 * @param type
	 *            标识不同的数据源（系统流水 or 银行流水）
	 * @param accounttype
	 *            因为入款账号里面有第三方的入款账号，第三方的入款账号没有汇出账号，所以把第三方的账号拿出来查询第三方的数据
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequiresPermissions("FinBalanceStat:*")
	@RequestMapping("/finTransBalanceSys")
	public String FinTransBalanceSys(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "to_account", required = false) String to_account,
			@RequestParam(value = "from_account", required = false) String from_account,
			@RequestParam(value = "startAndEndTime", required = false) String[] startAndEndTimeToArray,
			@RequestParam(value = "startamount", required = false) BigDecimal startamount,
			@RequestParam(value = "endamount", required = false) BigDecimal endamount,
			@RequestParam(value = "accountid", required = false) int accountid,
			@RequestParam(value = "id", required = false) int id,
			@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "accounttype", required = false) String accounttype,
			@RequestParam(value = "accountname", required = false) String accountname,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					Sort.Direction.ASC, "A.amount");
			if ("".equals(to_account) || "" == to_account) {
				to_account = null;
			}
			if ("".equals(from_account) || "" == from_account) {
				from_account = null;
			}
			if (new BigDecimal(0) != startamount) {

			} else {
				startamount = new BigDecimal(0);
				endamount = new BigDecimal(0);
			}
			// 拼接时间戳查询数据条件
			String fristTime = null;
			String lastTime = null;
			if (0 != startAndEndTimeToArray.length && null != startAndEndTimeToArray) {
				fristTime = startAndEndTimeToArray[0];
				lastTime = startAndEndTimeToArray[1];
			}
			Map<String, Object> mapp = finBalanceStatService.finTransBalanceSys(to_account, from_account,
					TimeChangeCommon.TimeStamp2Date(fristTime, "yyyy-MM-dd HH:mm:ss"),
					TimeChangeCommon.TimeStamp2Date(lastTime, "yyyy-MM-dd HH:mm:ss"), startamount, endamount, accountid,
					id, type, accounttype, accountname, pageRequest);
			Page<Object> page = (Page<Object>) mapp.get("Page");
			List<FinTransStatMatch> arrlist = new ArrayList<FinTransStatMatch>();
			if (null != page) {
				List<Object> FinTransStatMatch = page.getContent();
				for (int i = 0; i < FinTransStatMatch.size(); i++) {
					Object[] obj = (Object[]) FinTransStatMatch.get(i);
					FinTransStatMatch FinTrans = new FinTransStatMatch();
					// 系统流水
					if ("sys".equals(type)) {
						FinTrans.setFromaccountname((String) obj[2]);
						FinTrans.setToaccountname((String) obj[4]);
						FinTrans.setAmount((BigDecimal) obj[5]);
						FinTrans.setFee((BigDecimal) obj[6]);
						FinTrans.setCreatetime((String) obj[7]);
						FinTrans.setRemark(
								null == obj[8] ? "" : ((String) obj[8]).replace("\r\n", "<br>").replace("\n", "<br>"));
						FinTrans.setOperator((String) obj[10]);
						// 处理入款账号里面有第三方的入款账号
						// if("2".equals(accounttype) && 1==id){
						// FinTrans.setMemberusername((String) obj[11]);
						// }
						arrlist.add(FinTrans);
					} else if ("bank".equals(type)) {// 银行流水
						FinTrans.setFromaccountname((String) obj[2]);
						FinTrans.setToaccountname((String) obj[3]);
						FinTrans.setAmount((BigDecimal) obj[4]);
						FinTrans.setFee((BigDecimal) obj[5]);
						FinTrans.setCreatetime((String) obj[6]);
						arrlist.add(FinTrans);
					}
				}
			}
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("arrlist", arrlist);
			map.put("page", new Paging(page));
			map.put("total", mapp.get("total"));
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("余额明细>明细>系统明细Controller查询失败" + e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 清算数据，查询是否还存在没有匹配的数据
	 * 
	 * @param startAndEndTime
	 *            时间
	 * @param pageNo
	 *            页码
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequiresPermissions("FinBalanceStat:*")
	@RequestMapping("/clearaccountdate")
	public String ClearAccountDate(@RequestParam(value = "startAndEndTime", required = false) String startAndEndTime,
			@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					Sort.Direction.ASC, "D.counts");

			// 拼接时间戳查询数据条件
			String fristTime = null;
			if (!"".equals(startAndEndTime) && null != startAndEndTime) {
				fristTime = startAndEndTime + ":59";
			}
			Map<String, Object> mapp = finBalanceStatService
					.ClearAccountDate(TimeChangeCommon.TimeStamp2Date(fristTime, "yyyy-MM-dd HH:mm:ss"), pageRequest);
			Map<String, Object> map = new LinkedHashMap<>();
			Page<Object> Page = (Page<Object>) mapp.get("Page");
			List<AccountStatistics> arrlist = new ArrayList<AccountStatistics>();
			if (null != Page) {
				List<Object> AccountStatisticsList = Page.getContent();
				for (int i = 0; i < AccountStatisticsList.size(); i++) {
					Object[] obj = (Object[]) AccountStatisticsList.get(i);
					AccountStatistics AccountStatistics = new AccountStatistics();
					AccountStatistics.setAccount((String) obj[0]);
					AccountStatistics.setType((int) obj[1]);
					AccountStatistics.setCounts(new Integer(String.valueOf(obj[2])));
					// 用bankcount 当状态
					AccountStatistics.setBankcount((int) obj[3]);
					arrlist.add(AccountStatistics);
				}
			}
			map.put("arrlist", arrlist);
			map.put("page", new Paging(Page));
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("清算数据，查询是否还存在没有匹配的数据Controller失败" + e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 删除已经匹配的数据
	 * 
	 * @param startAndEndTime
	 *            时间
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequiresPermissions("FinBalanceStat:*")
	@RequestMapping("/deleteaccountdate")
	public String DeleteAccountDate(@RequestParam(value = "startAndEndTime", required = false) String startAndEndTime)
			throws JsonProcessingException {
		GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		Map<String, Object> map = new LinkedHashMap<>();
		try {
			// 拼接时间戳查询数据条件
			String fristTime = null;
			if (!"".equals(startAndEndTime) && null != startAndEndTime) {
				fristTime = startAndEndTime;
			}
			SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = sDateFormat.parse(startAndEndTime);
			// 校验时间是否在十天之外
			if ((int) Math.abs(((date.getTime() - new Date().getTime()) / (1000 * 3600 * 24))) <= 10) {
				map.put("message", "所选时间在十天之内，不能清算！");
			} else {
				Map<String, Object> mapp = finBalanceStatService
						.DeleteAccountDate(TimeChangeCommon.TimeStamp2Date(fristTime, "yyyy-MM-dd HH:mm:ss"));
				map.put("message", mapp.get("message"));
				redisService.convertAndSend(RedisTopics.DELETED_SCREENSHOTS, startAndEndTime);
			}
		} catch (Exception e) {
			e.printStackTrace();
			map.put("message", "清算失败！" + e.getLocalizedMessage());
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		}
		responseData.setData(map);
		return mapper.writeValueAsString(responseData);
	}

}