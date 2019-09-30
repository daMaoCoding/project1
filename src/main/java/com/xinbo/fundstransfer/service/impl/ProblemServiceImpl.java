package com.xinbo.fundstransfer.service.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.assign.OtherCache;
import com.xinbo.fundstransfer.domain.entity.BizDeviceDeal;
import com.xinbo.fundstransfer.domain.entity.BizRebateUser;
import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import com.xinbo.fundstransfer.domain.pojo.Account;
import com.xinbo.fundstransfer.domain.repository.DeviceDealRepository;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.domain.entity.BizAccountMore;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.DeviceStatus;
import com.xinbo.fundstransfer.service.*;

@Service
public class ProblemServiceImpl implements ProblemService {
	private static final Logger log = LoggerFactory.getLogger(ProblemServiceImpl.class);
	@Autowired
	private RedisService redisService;
	@Autowired
	@Lazy
	private AccountService accountService;
	@Autowired
	private AccountMoreService accountMoreService;
	@Autowired
	private AccountMoreService accMoreSer;
	@Autowired
	private RebateUserService rebateUserService;
	@Autowired
	private DeviceDealRepository deviceDealRepository;
	@Autowired
	private AllocateTransService allocateTransService;
	@Autowired
	private OtherCache otherCache;
	@Autowired
	private CabanaService cabanaService;
	private ObjectMapper mapper = new ObjectMapper();

	private volatile static Thread THREAD_ACC_PROBLEM = null;

	private static final Cache<String, String> DEVICE_STATUS = CacheBuilder.newBuilder().maximumSize(5000)
			.expireAfterWrite(90, TimeUnit.SECONDS).build();
	private static final ConcurrentLinkedQueue<String> DEVICE_QUENE = new ConcurrentLinkedQueue<>();
	private static final int ABNORMAL = 0; // 异常、待处理
	private static final int NORMAL = 1; // 正常、处理完成
	private static final int DEALING = 2; // 正在处理

	/**
	 * 工具端上报设备状态处理
	 *
	 * @param dsStr
	 */
	@Override
	public void reportDeviceStatus(String dsStr) {
		if (StringUtils.isBlank(dsStr)) {
			return;
		}
		String mobile = "";
		try {
			DeviceStatus ds = mapper.readValue(dsStr, DeviceStatus.class);
			AccountBaseInfo base = accountService.getFromCacheById(ds.getId());
			if (base != null) {
				mobile = accountService.getFromCacheById(ds.getId()).getMobile();
				if (Objects.equals(base.getStatus(), AccountStatus.Normal.getStatus())
						|| Objects.equals(base.getStatus(), AccountStatus.Enabled.getStatus())) {
					DEVICE_STATUS.put(mobile, dsStr);
					DEVICE_QUENE.add(mobile);
				} else {
					redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_DEVICE_STATUS_KEYS).delete(mobile);
				}
			}
		} catch (Exception e) {
			log.error("Error,problem message. id:{} msg:{}", mobile, e);
		}
		initProblemThread();
	}

	/**
	 * 处理设备状态上报信息
	 *
	 * @param mobile
	 */
	private void dealDeviceStatus(String mobile) {
		String dsStr = DEVICE_STATUS.getIfPresent(mobile);
		if (StringUtils.isBlank(dsStr)) {
			return;
		}
		dealDeviceKeys(mobile, dsStr);
		dealAccAlarm(mobile, dsStr);
	}

	/**
	 * 处理设备状态信息
	 *
	 * @param mobile
	 * @param dsStr
	 */
	private void dealDeviceKeys(String mobile, String dsStr) {
		String oldStr = (String) redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_DEVICE_STATUS_KEYS)
				.get(mobile);
		DeviceStatus old_status = getDeviceStatus(oldStr);
		DeviceStatus status = getDeviceStatus(dsStr);
		if (!CollectionUtils.isEmpty(status.getErrMsgMap())) {
			status.setErrTime(System.currentTimeMillis());
			String errMsg = "";
			for (Map.Entry<String, String> ent : status.getErrMsgMap().entrySet()) {
				if (StringUtils.isBlank(errMsg)) {
					errMsg = ent.getValue();
				} else {
					errMsg = CommonUtils.genRemark(errMsg, ent.getValue(), null, null);
				}
			}
			status.setErrMsg(errMsg);
			status.setLockStatus(ABNORMAL); // 未锁定
			status.setDealStatus(ABNORMAL); // 未处理
			status.setStatus(ABNORMAL); // 状态异常
			if (status.getErrMsgMap().containsKey("电量")) {
				status.setBatteryStatus(ABNORMAL);
			} else {
				status.setBatteryStatus(NORMAL);
			}
		} else {
			if (old_status != null && Objects.equals(old_status.getLockStatus(), NORMAL)) {
				status.setOperator(old_status.getOperator());
				status.setDealStatus(DEALING);
				status.setLockStatus(old_status.getLockStatus());
				status.setLockTime(old_status.getLockTime());
				status.setStatus(NORMAL);
			}else {
				status.setStatus(NORMAL);
				status.setLockStatus(ABNORMAL); // 未锁定
				status.setDealStatus(NORMAL); // 处理完成
			}
		}
		if (old_status != null) {
			if (Objects.equals(status.getStatus(), NORMAL) && Objects.equals(old_status.getStatus(), ABNORMAL)) {
				status.setSolveTime(System.currentTimeMillis());
				if (StringUtils.isNotBlank(old_status.getOperator())
						&& Objects.equals(old_status.getLockStatus(), NORMAL)) {
					status.setOperator(old_status.getOperator());
					status.setLockStatus(ABNORMAL);
					status.setLockTime(old_status.getLockTime());
				} else {
					status.setOperator("ADMIN");
					status.setLockStatus(ABNORMAL);
				}
			}
			if(Objects.equals(status.getStatus(), ABNORMAL) && Objects.equals(old_status.getDealStatus(), DEALING)){
				status.setOperator(old_status.getOperator());
				status.setDealStatus(DEALING);
				status.setLockStatus(old_status.getLockStatus());
				status.setLockTime(old_status.getLockTime());
			}
		}
		if (old_status != null && !CollectionUtils.isEmpty(old_status.getErrMsgMap()))
			status.setErrTime(old_status.getErrTime());
		writeDeviceToRedis(mobile, status);
	}

	/**
	 * 处理账号告警信息
	 *
	 * @param mobile
	 * @param dsStr
	 */
	private void dealAccAlarm(String mobile, String dsStr) {
		DeviceStatus status = getDeviceStatus(dsStr);
		if (status == null || StringUtils.isBlank(mobile)) {
			return;
		}
		if (!CollectionUtils.isEmpty(status.getErrMsgMap())) {
			for(Entry<String, String> e : status.getErrMsgMap().entrySet()) {
				if(e.getValue().contains("获取收款记录列表失败，服务器返回的错误消息")) {
					return;
				}
			}
			redisService.getStringRedisTemplate().boundHashOps(RedisKeys.PROBLEM_ACC_ALARM)
					.put(status.getId().toString(), "");
			log.trace("reportDeviceStatus>>device status is normal,stop business,accId {},msg {}", status.getId(),
					status.getErrMsgMap());
		} else {
			BizAccountMore more = accountMoreService.getFromCacheByMobile(mobile);
			String accounts = more.getAccounts();
			String[] ids = accounts.split(",");
			for (String id : ids) {
				if (StringUtils.isNotBlank(id)) {
					redisService.getStringRedisTemplate().boundHashOps(RedisKeys.PROBLEM_ACC_ALARM).delete(id);
				}
			}
		}
	}

	@Override
	public Page<List> getProblemInfoList(SysUser user, PageRequest pageRequest, List<Predicate<DeviceStatus>> rules) {
		int failCounts = CommonUtils.getFailureTransCount();
		List<DeviceStatus> result = new ArrayList<>();
		List<Integer> normalList = accountService.getAllAccIdByStatus(AccountStatus.Normal.getStatus());
		Map<Object,Object> keyStatus = redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_DEVICE_STATUS_KEYS)
				.entries();
		Collection<Object> objs = keyStatus.values();
		Set<Integer> fail = allocateTransService.buildFailureTrans();
		for (Object o : objs) {
			String dsStr = (String) o;
			DeviceStatus ds = getDeviceStatus(dsStr);
			if (ds != null) {
				if (ds.getStatus() == 1 && fail != null && fail.contains(ds.getId())) {
					ds.setDealStatus(ABNORMAL); // 未处理
					ds.setStatus(0);
					ds.setErrMsg("连续转账失败超过 " + failCounts + "，不再分配出款和下发任务");
				}
				AccountBaseInfo base = accountService.getFromCacheById(ds.getId());
				if (base != null && Objects.equals(base.getStatus(), AccountStatus.Normal.getStatus())) {
					normalList.remove(ds.getId());
					if (Objects.nonNull(ds.getErrMsg()) && ds.getErrMsg().equals("账号已离线")) {
						ds.setOffLineStatus("1");
					} else {
						ds.setOffLineStatus("0");
					}
					result.add(ds);
				} else {
					deleteDeviceStatus(ds.getMobile());
				}
			}
		}

		for(Integer id:normalList){
			AccountBaseInfo base = accountService.getFromCacheById(id);
			if(base != null){
				if(base.checkMobile() && StringUtils.isNotBlank(base.getMobile()) && !keyStatus.containsKey(base.getMobile())){
					DeviceStatus status = getOffLineStatus(base);
					result.add(status);
				}
			}
		}

		List<DeviceStatus> showResult = new ArrayList<>();
		if (!CollectionUtils.isEmpty(rules)) {
			result = result.stream().filter(ele -> rules.stream().reduce(t -> true, Predicate::and).test(ele))
					.collect(Collectors.toList());
		}
		int total = result.size();
		long off = pageRequest.getOffset() - 1, size = pageRequest.getPageSize();
		result.sort((o1, o2) -> {
			if (!Objects.equals(o1.getStatus(), o2.getStatus())) {
				return o1.getStatus() - o2.getStatus();
			} else {
				return StringUtils.compare(o1.getMobile(), o2.getMobile());
			}
		});
		for (int i = 0; i < total; i++) {
			if (i > off && i <= off + size) {
				DeviceStatus status = result.get(i);
				status.setRemark(getRemark(status.getMobile()));
				if(status.getLockStatus() != null && status.getLockStatus() == 1) {
					Instant instNow = Instant.now();  //当前的时间
					Instant inst3 = Instant.ofEpochMilli(status.getLockTime());
					status.setDealTime(Duration.between(inst3, instNow).getSeconds());
				}
				showResult.add(status);
			}
			if (i > off + size) {
				break;
			}
		}
		return new PageImpl(showResult, pageRequest, total);
	}

	public boolean lock(String mobile, SysUser operator, String dealer,String id) {
		String dsStr = (String) redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_DEVICE_STATUS_KEYS)
				.get(mobile);
		DeviceStatus status = getDeviceStatus(dsStr);
		if(status == null && StringUtils.isNotBlank(id)){
			AccountBaseInfo base = accountService.getFromCacheById(Integer.parseInt(id));
			status = getOffLineStatus(base);
		}
		if (Objects.equals(status.getLockStatus(), 1) && StringUtils.isNotBlank(status.getOperator()) && !StringUtils.equals(status.getOperator(),operator.getUid())) {
			return false;
		}
		if (status != null) {
			if (StringUtils.isNotBlank(dealer)) {
				status.setOperator(dealer);
			} else {
				status.setOperator(operator.getUid());
			}
			status.setLockTime(System.currentTimeMillis());
			status.setLockStatus(NORMAL);
			status.setDealStatus(DEALING);
			writeDeviceToRedis(mobile, status);
		}
		return true;
	}

	public void unlock(String mobile, SysUser operator) {
		String dsStr = (String) redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_DEVICE_STATUS_KEYS)
				.get(mobile);
		DeviceStatus s = getDeviceStatus(dsStr);
		if (s != null) {
			s.setOperator(null);
			s.setLockTime(null);
			s.setLockStatus(ABNORMAL);
			s.setDealStatus(NORMAL);
			writeDeviceToRedis(mobile, s);
		}
	}

	@Override
	@Transactional
	public void deal(String mobile, SysUser operator, String remark) {
		log.debug("deal>>处理设备排查信息 mobile {},remark {}", mobile, remark);
		BizDeviceDeal deal = deviceDealRepository.findById2(mobile);
		Date update = new Date();
		if (deal == null) {
			deal = new BizDeviceDeal();
			deal.setId(mobile);
			deal.setUpdateTime(update);
			remark = CommonUtils.genRemark("", remark, update, operator.getUid());
			deal.setRemark(remark);
		} else {
			deal.setUpdateTime(update);
			String remarkOld = deal.getRemark();
			remark = CommonUtils.genRemark(remarkOld, remark, update, operator.getUid());
			deal.setRemark(remark);
		}
		deviceDealRepository.saveAndFlush(deal);
		DeviceStatus ds = getDeviceStatusByMobile(mobile);
		if (ds != null && ds.getId() != null) {
			redisService.getStringRedisTemplate().boundHashOps(RedisKeys.PROBLEM_ACC_ALARM)
					.delete(ds.getId().toString());
			if (redisService.getStringRedisTemplate().boundHashOps(RedisKeys.COUNT_FAILURE_TRANS)
					.hasKey(ds.getId().toString())) {
				redisService.getStringRedisTemplate().boundHashOps(RedisKeys.COUNT_FAILURE_TRANS)
						.delete(ds.getId().toString());
				otherCache.invalidCache("TRANSFAILURE");
			}
			cabanaService.error(ds.getId());
		}
		deleteDeviceStatus(mobile);
		log.debug("deal>>处理设备排查信息完成 mobile {},remark {}", mobile, remark);
	}

	public Map<String, String> getDeviceByMobile(String mobile) {
		DeviceStatus ds = getDeviceStatusByMobile(mobile);
		return getDeviceStringMap(ds);
	}

	public Map<String, String> getContractInfo(String mobile) {
		Map<String, String> result = new HashMap<>();
		if (StringUtils.isBlank(mobile)) {
			return result;
		}
		BizAccountMore more = accMoreSer.getFromCacheByMobile(mobile);
		BizRebateUser rebateUser = rebateUserService.getFromCacheByUid(more.getUid());
		if (more != null) {
			if (more.getMargin() != null) {
				result.put("credits", more.getMargin().setScale(2, BigDecimal.ROUND_HALF_UP).toString());
			}
			result.put("creditsTime", CommonUtils.getDateStr(more.getUpdateTime()));
		}
		if (rebateUser != null) {
			result.put("salesName", rebateUser.getSalesName());
			result.put("contactor", rebateUser.getContactor());
			result.put("contactorInfo", rebateUser.getContactText());
		}
		BizDeviceDeal device = deviceDealRepository.findById2(mobile);
		if (device != null) {
			result.put("remark", device.getRemark());
			result.put("lastUpdateTime", CommonUtils.getDateStr(device.getUpdateTime()));
		}
		DeviceStatus ds = getDeviceStatusByMobile(mobile);
		if (ds != null) {
			result.put("currLock", ds.getOperator());
		}
		return result;
	}

	public DeviceStatus getDeviceStatusByMobile(String mobile) {
		String dsStr = (String) redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_DEVICE_STATUS_KEYS)
				.get(mobile);
		return getDeviceStatus(dsStr);
	}

	public void deleteDeviceStatus(String mobile) {
		redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_DEVICE_STATUS_KEYS).delete(mobile);
	}

	public void reportErrorMsg(String id,String errMsg){
		AccountBaseInfo base = accountService.getFromCacheById(Integer.parseInt(id));
		if(base != null && Objects.equals(base.getStatus(),AccountStatus.Normal.getStatus()) && base.checkMobile() && StringUtils.isNotBlank(base.getMobile())){
			DeviceStatus status = getDeviceStatusByMobile(base.getMobile());
			Map<String,String> errMap = status.getErrMsgMap();
			if(errMap == null){
				errMap = new HashMap<>();
			}
			errMap.put("手机上报错误信息",errMsg);
			status.setErrMsgMap(errMap);
			try {
				String dsStr = mapper.writeValueAsString(status);
				DEVICE_STATUS.put(base.getMobile(), dsStr);
				DEVICE_QUENE.add(base.getMobile());
			} catch (JsonProcessingException e) {
				log.debug("手机上报错误信息处理失败:{}", e);
			}
		}
		initProblemThread();
	}

	private DeviceStatus getDeviceStatus(String dsStr) {
		try {
			DeviceStatus ds = mapper.readValue(dsStr, DeviceStatus.class);
			if (ds.getId() != null) {
				AccountBaseInfo base = accountService.getFromCacheById(ds.getId());
				if (base != null) {
					ds.setBankType(base.getBankType());
					ds.setOwner(base.getOwner());
					ds.setAccount(base.getAccount());
					ds.setAlias(base.getAlias());
					ds.setUserName(getRebateUserName(base));
				}
			}
			return ds;
		} catch (Exception e) {
			return null;
		}
	}

	private void writeDeviceToRedis(String mobile, DeviceStatus status) {
		if (status == null) {
			return;
		}
		status.setMobile(mobile);
		try {
			redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_DEVICE_STATUS_KEYS).put(mobile,
					mapper.writeValueAsString(status));
		} catch (Exception e) {
			log.debug("设备排查添加锁定信息失败:{}", e);
		}
	}

	private Map<String, String> getDeviceStringMap(DeviceStatus ds) {
		if (ds != null) {
			Date date2 = new Date();
			Map<String, String> res = new HashMap<>();
			if (ds.getErrTime() != null) {
				date2.setTime(ds.getErrTime());
				res.put("errTime", CommonUtils.getDateStr(date2));
				if (ds.getSolveTime() == null) {
					long total = System.currentTimeMillis() - ds.getErrTime();
					String totalStr = getSolveTime(total);
					res.put("totalTime", totalStr);
				} else {
					long total = ds.getSolveTime() - ds.getErrTime();
					String totalStr = getSolveTime(total);
					res.put("totalTime", totalStr);
				}
			}
			if (ds.getLockTime() != null) {
				date2.setTime(ds.getLockTime());
				res.put("lockTime", CommonUtils.getDateStr(date2));
			}
			if (ds.getSolveTime() != null) {
				date2.setTime(ds.getSolveTime());
				res.put("solveTime", CommonUtils.getDateStr(date2));
			}
			if (StringUtils.isNotBlank(ds.getOperator())) {
				res.put("operator", "ADMIN".equals(ds.getOperator()) ? "兼职自处理" : ds.getOperator());
			}
			return res;
		}
		return null;
	}

	private String getSolveTime(long total) {
		SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
		String hms = formatter.format(total);
		if (total > 60 * 60 * 1000) {
			String[] time = hms.split(":");
			return time[0] + "小时" + time[1] + "分" + time[2] + "秒";
		} else if (total > 60 * 1000) {
			String[] time = hms.split(":");
			return time[1] + "分" + time[2] + "秒";
		} else {
			String[] time = hms.split(":");
			return time[2] + "秒";
		}
	}

	private String getRemark(String mobile){
		BizDeviceDeal deal = deviceDealRepository.findById2(mobile);
		if(Objects.isNull(deal)){
			return "";
		}
		return deal.getRemark().replace("\r\n", "<br>").replace("\n", "<br>");
	}

	private DeviceStatus getOffLineStatus(AccountBaseInfo base){
		DeviceStatus status = new DeviceStatus();
		status.setMobile(base.getMobile());
		status.setId(base.getId());
		status.setStatus(ABNORMAL);
		status.setDealStatus(ABNORMAL);
		status.setErrMsg("账号已离线");
		status.setBankType(base.getBankType());
		status.setOwner(base.getOwner());
		status.setAccount(base.getAccount());
		status.setAlias(base.getAlias());
		status.setOffLineStatus("1");
		status.setUserName(getRebateUserName(base));
		return status;
	}

	private String getRebateUserName(AccountBaseInfo base){
		if(base == null)
			return "";
		BizAccountMore more = accMoreSer.getFromCacheByMobile(base.getMobile());
		if(more != null){
			BizRebateUser user = rebateUserService.getFromCacheByUid(more.getUid());
			if(user != null)
				return user.getUserName();
		}
		return "";
	}

	private void initProblemThread(){
		if (AllocateIncomeAccountServiceImpl.APP_STOP && Objects.nonNull(THREAD_ACC_PROBLEM)) {
			try {
				THREAD_ACC_PROBLEM.interrupt();
			} catch (Exception e) {
				log.error("Error,stop thread for account problem.");
			} finally {
				THREAD_ACC_PROBLEM = null;
			}
			return;
		} else if (Objects.nonNull(THREAD_ACC_PROBLEM) && THREAD_ACC_PROBLEM.isAlive()) {
			log.trace("the thread for account problem already exist.");
			return;
		}
		THREAD_ACC_PROBLEM = new Thread(() -> {
			for (;;) {
				try {
					String dsMobile = DEVICE_QUENE.poll();
					if (StringUtils.isBlank(dsMobile)) {
						if (!AllocateIncomeAccountServiceImpl.APP_STOP && Objects.nonNull(THREAD_ACC_PROBLEM)) {
							try {
								Thread.sleep(2000);
							} catch (Exception e) {
								log.error("Thread Account Problem Exception.");
							}
							continue;
						} else {
							break;
						}
					}
					dealDeviceStatus(dsMobile);
				} catch (Exception e) {
					log.error("Thread Account Problem Exception. ", e);
				}
			}
		});
		THREAD_ACC_PROBLEM.setName("THREAD_ACC_PROBLEM");
		THREAD_ACC_PROBLEM.start();
	}
}