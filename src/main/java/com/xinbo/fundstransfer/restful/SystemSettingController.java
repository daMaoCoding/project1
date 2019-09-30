package com.xinbo.fundstransfer.restful;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.net.socket.MinaMonitorServer;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.DynamicSpecifications;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.GeneralResponseData.ResponseStatus;
import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.chatPay.commons.config.AliIncomeConfig;
import com.xinbo.fundstransfer.chatPay.commons.config.AliOutConfig;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.entity.SysUserProfile;
import com.xinbo.fundstransfer.domain.enums.UserProfileKey;
import com.xinbo.fundstransfer.domain.pojo.SystemWebSocketCategory;
import com.xinbo.fundstransfer.service.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/r/set")
public class SystemSettingController extends BaseController {

	@Autowired
	private ObjectMapper mapper;

	private static final Logger logger = LoggerFactory.getLogger(SystemSettingController.class);
	@Autowired
	private RedisService redisService;
	@Autowired
	private SystemSettingService systemSettingService;
	@Autowired
	private SysUserProfileService sysUserProfileService;
	@Autowired
	private HostMonitorService hostMonitorService;
	@Autowired
	private AllocateTransService allocateTransService;
	@Autowired
	private AllocateOutwardTaskService allocateOutwardTaskService;
	@Autowired
	private CabanaService cabanaService;
	@Autowired
	MinaMonitorServer minaMonitorServer;
	@Autowired
	AccountService accountService;

	@GetMapping("/third2OutSetting")
	public String enableThirdDrawToOutCard(@RequestParam(value = "action", required = false) String action)
			throws JsonProcessingException {
		GeneralResponseData ret = StringUtils.isNotBlank(action)
				? new GeneralResponseData<>(ResponseStatus.SUCCESS.getValue(), "保存成功")
				: new GeneralResponseData<>(ResponseStatus.SUCCESS.getValue(), "查询成功");
		String res = accountService.enableThirdDrawToOutCard(action);
		if (StringUtils.isNotBlank(res) && StringUtils.isNotBlank(action) && !res.equals(action)) {
			ret = new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "保存失败");
		}
		ret.setData(res);
		return mapper.writeValueAsString(ret);
	}

	@RequestMapping("/socketInfo")
	public String socketInfo() throws JsonProcessingException {
		Map<String, Object> socket = minaMonitorServer.socketInfo();
		GeneralResponseData ret = new GeneralResponseData<>(ResponseStatus.SUCCESS.getValue(), null);
		ret.setData(socket);
		return mapper.writeValueAsString(ret);
	}

	@RequestMapping("/reloadInAcc4Alloc")
	public String reloadInAcc4Alloc() throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		redisService.convertAndSend(RedisTopics.SYS_REBOOT, CommonUtils.getInternalIp());
		return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.SUCCESS.getValue(), "入款账号正在加载..."));
	}

	@RequestMapping("/flushCache")
	public String flushCache() throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		try {
			redisService.convertAndSend(RedisTopics.REFRESH_ALL_SYS_SETTING,
					org.apache.commons.lang3.StringUtils.EMPTY);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "Success:reload system setting"));
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "Fail: reload system setting"));
		}
	}

	/**
	 * 获取大额出款审核金额,优先从缓存获取，如果没有再查询数据库，更新的时候会同时更新缓存
	 */
	@RequestMapping("/findAutoApproveLimit")
	public String findAutoApproveLimit() throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		GeneralResponseData<String> responseData;
		try {
			SysUserProfile sysUserProfile = sysUserProfileService.findByUserIdAndPropertyKey(false,
					AppConstants.USER_ID_4_ADMIN, "OUTDRAW_LIMIT_APPROVE");
			if (sysUserProfile != null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue());
				responseData.setData(sysUserProfile.getPropertyValue());
			} else {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue());
				responseData.setData(null);
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			responseData.setData(null);
			return mapper.writeValueAsString(responseData);
		}
	}

	/**
	 * 初始化 系统管理--设置管理
	 */
	@RequestMapping("/list")
	public String findAll() throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		try {
			GeneralResponseData<Map<String, List<SysUserProfile>>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			List<SysUserProfile> list = systemSettingService.findAll(AppConstants.USER_ID_4_ADMIN);
			Map<String, List<SysUserProfile>> map = new LinkedHashMap<>();
			if (list != null && list.size() > 0) {
				List<SysUserProfile> incomeSettingList = new LinkedList<>();
				List<SysUserProfile> outDrawSettingList = new LinkedList<>();
				List<SysUserProfile> financialSettingList = new LinkedList<>();
				List<SysUserProfile> otherSettingList = new LinkedList<>();
				list.forEach(p -> {
					if (p.getPropertyKey().contains("INCOME_")) {
						incomeSettingList.add(p);
					}
					if (p.getPropertyKey().contains("OUTDRAW_")) {
						outDrawSettingList.add(p);
					}
					if (p.getPropertyKey().contains("FINANCE_")) {
						financialSettingList.add(p);
					}
					if (p.getPropertyKey().contains("OTHER_")) {
						otherSettingList.add(p);
					}
				});
				map.put("incomeSettingList", incomeSettingList);
				map.put("outDrawSettingList", outDrawSettingList);
				map.put("financialSettingList", financialSettingList);
				map.put("otherSettingList", otherSettingList);
			}
			responseData.setData(map);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error("查询系统设置失败:{}", e);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询失败"));
		}
	}

	@RequestMapping("/findAllToMap")
	public String findAllToMap() throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		try {
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			Map<String, Object> dataToMap = new HashMap<>();
			List<SysUserProfile> dataToList = systemSettingService.findAll(AppConstants.USER_ID_4_ADMIN);
			for (SysUserProfile profile : dataToList) {
				dataToMap.put(profile.getPropertyKey(), profile.getPropertyValue());
			}
			responseData.setData(dataToMap);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error("查询失败：{}", e);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询失败"));
		}
	}

	/**
	 *
	 */
	@RequestMapping("/saveOutUpLim")
	public String saveOutUpLim(@RequestParam(value = "zone") int zone,
			@RequestParam(value = "on1stop0") Integer on1stop0,
			@RequestParam(value = "uplimit", required = false) Integer uplimit,
			@RequestParam(value = "triglimit", required = false) Integer triglimit,
			@RequestParam(value = "lastTime", required = false) Integer lastTime) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		logger.info("SystemSetting >> saveOutUpLim (operator:{} on1stop0:{}  uplimit:{} triglimit:{} lastTime:{})",
				operator.getUid(), on1stop0, uplimit, triglimit, lastTime);
		try {
			if (zone == 0) {
				return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "请选择区域"));
			}
			if (on1stop0 == 1) {
				if (uplimit == null || uplimit < 5000 || uplimit > 40000) {
					return mapper.writeValueAsString(new GeneralResponseData<>(
							GeneralResponseData.ResponseStatus.FAIL.getValue(), "下发上限值应在[5000,45000]"));
				}
				if (triglimit == null || triglimit < 2000 || triglimit > 12000) {
					return mapper.writeValueAsString(new GeneralResponseData<>(
							GeneralResponseData.ResponseStatus.FAIL.getValue(), "下发触发值应在[2000,12000]"));
				}
				if (uplimit - triglimit < 3000) {
					return mapper.writeValueAsString(new GeneralResponseData<>(
							GeneralResponseData.ResponseStatus.FAIL.getValue(), "下发上限值至少大于下发触发值3000以上"));
				}
				if (lastTime == null || lastTime < 10 || lastTime > 180) {
					return mapper.writeValueAsString(new GeneralResponseData<>(
							GeneralResponseData.ResponseStatus.FAIL.getValue(), "下发持续时间应在[10,180]"));
				}
				allocateTransService.setUpLim4ONeed(zone, true, uplimit, triglimit, lastTime);
			} else {
				allocateTransService.setUpLim4ONeed(zone, false, 0, 0, 0);
			}
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "保存成功"));
		} catch (Exception e) {
			logger.error(
					"SystemSetting >> saveOutUpLim (operator:{} on1stop0:{}  uplimit:{} triglimit:{} lastTime:{}) exception:{}",
					operator.getUid(), on1stop0, uplimit, triglimit, lastTime, e);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "保存失败"));
		}
	}

	@RequestMapping("/findOutUpLim")
	public String findOutUpLim(@RequestParam(value = "zone") int zone) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		try {
			Map<String, Object> data = new HashMap<String, Object>() {
				{
					put("on1stop0", 0);
					put("uplimit", 0);
					put("triglimit", 0);
					put("lastTime", 0);
					put("expireTime", 0);
				}
			};
			long[] upLim4ONeed = allocateTransService.getUpLim4ONeed(zone);
			if (upLim4ONeed[0] == 1) {
				data.put("on1stop0", upLim4ONeed[0]);
				data.put("uplimit", upLim4ONeed[1]);
				data.put("triglimit", upLim4ONeed[2]);
				data.put("lastTime", upLim4ONeed[3]);
				data.put("expireTime", CommonUtils.getDateFormat2Str(new Date(upLim4ONeed[4])));
			}
			GeneralResponseData<Map<String, Object>> res = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			res.setData(data);
			return mapper.writeValueAsString(res);
		} catch (Exception e) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "获取成功"));
		}
	}

	/**
	 *
	 */
	@RequestMapping("/saveOutMergeLevel")
	public String saveOutMergeLevel(@RequestParam(value = "zone") Integer zone,
			@RequestParam(value = "on1stop0") Integer on1stop0,
			@RequestParam(value = "lastTime", required = false) Integer lastTime) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		logger.info("SystemSetting >> saveOutMergeLevel (operator:{} on1stop0:{} lastTime:{})", operator.getUid(),
				on1stop0, lastTime);
		try {
			if (on1stop0 == 1) {
				if (lastTime == null || lastTime < 1 && lastTime > 24) {
					return mapper.writeValueAsString(new GeneralResponseData<>(
							GeneralResponseData.ResponseStatus.FAIL.getValue(), "持续时间应在[1,24]之间"));
				}
				allocateOutwardTaskService.setMergeLevel(true, zone, true, lastTime);
			} else {
				allocateOutwardTaskService.setMergeLevel(true, zone, false, 0);
			}
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "保存成功"));
		} catch (Exception e) {
			logger.info("SystemSetting >> saveOutMergeLevel (operator:{} on1stop0:{} lastTime:{})", operator.getUid(),
					on1stop0, lastTime, e);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "保存失败"));
		}
	}

	@RequestMapping("/findOutMergeLevel")
	public String findOutMergeLevel(@RequestParam(value = "zone") Integer zone) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		try {
			Map<String, Object> data = new HashMap<String, Object>() {
				{
					put("zone", zone);
					put("on1stop0", 0);
					put("lastTime", 0);
					put("expireTime", 0);
				}
			};
			if (zone != 0) {
				long[] setting = allocateOutwardTaskService.getMergeLevel(zone);
				if (setting[0] == 1) {
					data.put("zone", zone);
					data.put("on1stop0", setting[0]);
					data.put("lastTime", setting[1]);
					data.put("expireTime", CommonUtils.getDateFormat2Str(new Date(setting[2])));
				}
			}
			GeneralResponseData<Map<String, Object>> res = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			res.setData(data);
			return mapper.writeValueAsString(res);
		} catch (Exception e) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "获取失败"));
		}
	}

	@RequestMapping("/save")
	public String save(@RequestParam(value = "idsArray") Integer[] ids,
			@RequestParam(value = "valsArray") String[] vals) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		logger.info("SystemSetting >> save (operator:{}  idsArray:{}  valsArray:{})", operator.getUid(), ids, vals);
		try {
			GeneralResponseData<List<SysUserProfile>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "保存成功");
			List<SysUserProfile> list = new ArrayList<>();
			if (null != ids && ids.length > 0) {
				for (int i = 0, L = ids.length; i < L; i++) {
					SysUserProfile sysUserProfile = systemSettingService.findById(ids[i]);
					if (!StringUtils.equals(sysUserProfile.getPropertyValue(), vals[i])) {
						sysUserProfile.setPropertyValue(vals[i]);
						sysUserProfile = systemSettingService.saveSetting(sysUserProfile);
						redisService.convertAndSend(RedisTopics.REFRESH_SYSTEM_PROFILE,
								mapper.writeValueAsString(sysUserProfile));
					}
					list.add(sysUserProfile);
				}
			}
			responseData.setData(list);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error("SystemSetting >> save (operator:{}  idsArray:{}  valsArray:{}) exception:{}",
					operator.getUid(), ids, vals, e);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "保存失败"));
		}
	}

	@RequestMapping("/delete")
	public String delete(@RequestParam(value = "idsArray") Integer[] ids) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		logger.info("SystemSetting >> delete (operator:{}  ids:{})", operator.getUid(), ids);
		try {
			GeneralResponseData<String> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "删除成功");
			for (Integer id : ids) {
				systemSettingService.deleteById(id);
			}
			responseData.setData(ids.length + "");
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.info("SystemSetting >> delete (operator:{}  ids:{}) exception:{}", operator.getUid(), ids, e);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "删除失败"));
		}
	}

	@RequestMapping("/deleteMaintainBank")
	public String deleteMaintainBank(@RequestParam(value = "bankType") String bankType) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		logger.info("SystemSetting >> deleteMaintainBank (operator:{}  bankType:{})", operator.getUid(), bankType);
		try {
			GeneralResponseData<String> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "删除成功");
			List<SysUserProfile> profiles = systemSettingService
					.findByPropertyKey(UserProfileKey.OUTDRAW_SYS_ALL_BANKTYPE.getValue());
			for (SysUserProfile profile : profiles) {
				if (profile.getUserId().equals(AppConstants.USER_ID_4_ADMIN)) {
					// 获取所有银行类型列表
					String[] bankTypes = profile.getPropertyValue().split(";");
					if (bankTypes.length > 0 && Arrays.asList(bankTypes).contains(bankType)) {
						// 检索所有银行类别中是否存在需要删除的银行
						List<SysUserProfile> maintainBanks = systemSettingService
								.findByPropertyKey(UserProfileKey.OUTDRAW_SYS_MAINTAIN_BANKTYPE.getValue());
						for (SysUserProfile maintainBank : maintainBanks) {
							if (maintainBank.getUserId().equals(AppConstants.USER_ID_4_ADMIN)) {
								String[] maintainBankTypes = maintainBank.getPropertyValue().split(",");
								// 检索正在维护的银行列表是否不包含需要删除的
								if (maintainBankTypes.length > 0
										&& Arrays.asList(maintainBankTypes).contains(bankType)) {
									responseData = new GeneralResponseData<>(
											GeneralResponseData.ResponseStatus.FAIL.getValue(),
											bankType + "删除失败，此银行正在维护中," + bankType);
								} else {
									String bankTypeStr = "";
									// 执行删除
									for (String temp : bankTypes) {
										if (null != temp && !temp.trim().equals("")) {
											if (!temp.trim().equals(bankType.trim())) {
												bankTypeStr += temp + ";";
											}
										}
									}
									if (bankTypeStr.substring(bankTypeStr.length() - 1).equals(";")) {
										bankTypeStr = bankTypeStr.substring(0, bankTypeStr.length() - 1);
									}
									profile.setPropertyValue(bankTypeStr);
									// 更新并推送广播
									profile = systemSettingService.saveSetting(profile);
									redisService.convertAndSend(RedisTopics.REFRESH_SYSTEM_PROFILE,
											mapper.writeValueAsString(profile));
								}
							}
						}
					} else {
						responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
								bankType + "删除失败，列表中无此银行");
					}
				}
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error("SystemSetting >> deleteMaintainBank (operator:{}  bankType:{}) exception:{}",
					operator.getUid(), bankType, e);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "删除失败"));
		}
	}

	/**
	 * 根据配置的Key去更新对应系统设置
	 */
	@RequiresPermissions(value = { "SystemSetting:Update:*", "SystemMaintainBank:*" }, logical = Logical.OR)
	@RequestMapping("/update")
	public String update(@RequestParam(value = "keysArray") String[] keys,
			@RequestParam(value = "valsArray") String[] vals) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		try {
			logger.info("SystemSetting >> update (operator:{}  keys:{}  vals:{})", operator.getUid(), keys, vals);
			GeneralResponseData<List<SysUserProfile>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "保存成功");
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			List<SysUserProfile> list = new ArrayList<>();
			if (null != keys && keys.length > 0) {
				for (int i = 0, L = keys.length; i < L; i++) {
					if (keys[i].equals(UserProfileKey.ENABLE_INBANK_ALLOCATE_OUTWARD_HANDICAP.getValue())) {
						if (vals[i] != null) {
							logger.info("入款卡当出款卡用  盘口 {},{}", sysUser.getUid(), vals[i]);
						}
					} else if (keys[i].equals(UserProfileKey.ALLOCATE_OUTWARD_TASK_ENABLE_INBANK.getValue())) {
						if (vals[i] != null) {
							logger.info("入款卡当出款卡用  开启/关闭 {},{}", sysUser.getUid(), vals[i]);
						}
					} else if (keys[i].equals(UserProfileKey.HANDICAP_MANILA_ZONE.getValue())) {
						if (vals[i] != null) {
							logger.info("保存马尼拉区域盘口 {},{}", sysUser.getUid(), vals[i]);
						}
					} else if (keys[i].equals(UserProfileKey.HANDICAP_TAIWAN_ZONE.getValue())) {
						if (vals[i] != null) {
							logger.info("保存台湾区域盘口 {},{}", sysUser.getUid(), vals[i]);
						}
					}
					// 拆单金额加强校验，取值范围3-5万
					if (keys[i].equals(UserProfileKey.OUTDRAW_SPLIT_AMOUNT_OUTSIDE.getValue())
							|| keys[i].equals(UserProfileKey.OUTDRAW_SPLIT_AMOUNT_INSIDE.getValue())) {
						if (vals[i] != null) {
							Integer value = Integer.parseInt(vals[i]);
							if (value < 20000 || value > 50000) {
								return mapper.writeValueAsString(
										new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
												"保存失败,拆单金额值应该为20,000 ~ 50,000，当前值：" + value));
							}
						}

					}
					// 停止派单时间 不能大于七点
					if (keys[i].equals(UserProfileKey.OUTDRA_HALT_ALLOC_START_TIME.getValue())) {
						if (vals[i] != null) {
							String value = vals[i];
							SimpleDateFormat sdFormatter = new SimpleDateFormat("HH:mm:ss");
							Date startTime = sdFormatter.parse(value);
							if (startTime.getTime() > sdFormatter.parse("07:00:00").getTime()) {
								return mapper.writeValueAsString(new GeneralResponseData<>(
										GeneralResponseData.ResponseStatus.FAIL.getValue(), "停止派单时间不能大于七点：" + value));
							}
						}

					}
					// 需求7876 云闪付（边入边出）卡每日入款限额（元）
					if (keys[i].equals(UserProfileKey.Income_YSF_OneDay_Limit.getValue())) {
						if (vals[i] != null) {
							Integer value = Integer.parseInt(vals[i]);
							if (value<0) {
								return mapper.writeValueAsString(
										new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
												"云闪付（边入边出）卡每日入款限额不能小于0，当前值：" + value));
							}
						}
					}
					
					List<SysUserProfile> profiles = systemSettingService.findByPropertyKey(keys[i]);
					if (null != profiles && profiles.size() >= 1) {
						if (profiles.size() == 1) {
							// 系统设置只检索出userid为1（管理员）的一条系统设置时，走此段代码
							SysUserProfile sysUserProfile = profiles.get(0);
							String key = "";
							if (sysUserProfile.getPropertyKey().equals("OUTDRAW_SYS_MAINTAIN_BANKTYPE")
									|| sysUserProfile.getPropertyKey().equals("OUTDRAW_SYS_PEER_TRANSFER")) {
								if (vals.length > 0) {
									for (String val : vals) {
										key += val + ",";
									}
								}
								sysUserProfile.setPropertyValue(key);
								sysUserProfile = systemSettingService.saveSetting(sysUserProfile);
								// 更新缓存
								sysUserProfileService.findByUserIdAndPropertyKey(true, AppConstants.USER_ID_4_ADMIN,
										sysUserProfile.getPropertyKey());
								logger.info("银行流水维护，操作人{}，{}", sysUser.getUid(), key);
								redisService.convertAndSend(RedisTopics.REFRESH_SYSTEM_PROFILE,
										mapper.writeValueAsString(sysUserProfile));
							} else {
								if (!StringUtils.equals(sysUserProfile.getPropertyValue(), vals[i])) {
									sysUserProfile.setPropertyValue(vals[i]);
									sysUserProfile = systemSettingService.saveSetting(sysUserProfile);
									redisService.convertAndSend(RedisTopics.REFRESH_SYSTEM_PROFILE,
											mapper.writeValueAsString(sysUserProfile));
								}
							}
							list.add(sysUserProfile);
						} else {
							// 检索出多个配置时，查询uiserid为1的 才是系统配置，其它的可能是对单个用户设定的配置
							for (SysUserProfile temp : profiles) {
								if (temp.getUserId().equals(AppConstants.USER_ID_4_ADMIN)) {
									if (!StringUtils.equals(temp.getPropertyValue(), vals[i])) {
										temp.setPropertyValue(vals[i]);
										temp = systemSettingService.saveSetting(temp);
										// 更新缓存
										sysUserProfileService.findByUserIdAndPropertyKey(true,
												AppConstants.USER_ID_4_ADMIN, temp.getPropertyKey());
										redisService.convertAndSend(RedisTopics.REFRESH_SYSTEM_PROFILE,
												mapper.writeValueAsString(temp));
									}
									list.add(temp);
								}
							}
						}
					}
				}
			}
			responseData.setData(list);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error("SystemSetting >> update (operator:{}  keys:{}  vals:{}) exception:{}", operator.getUid(),
					keys, vals, e);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "保存失败"));
		}
	}

	/**
	 * 【仅快捷访问配置用】根据用户uid和key搜索对应配置
	 */
	@RequestMapping("/savequicklink")
	public String saveQucikLink(SysUserProfile userProfile) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		logger.info("SystemSetting >> saveQucikLink (operator:{}  userProfile:{})", operator.getUid(), userProfile);
		GeneralResponseData<List<SysUserProfile>> responseData = new GeneralResponseData<>(
				ResponseStatus.SUCCESS.getValue());
		try {
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			userProfile.setUserId(sysUser.getId());
			String key = userProfile.getPropertyKey();
			if (null == key || org.apache.commons.lang3.StringUtils.isBlank(key)) {
				return mapper.writeValueAsString(new GeneralResponseData<>(
						GeneralResponseData.ResponseStatus.FAIL.getValue(), "Param is null,be failed."));
			}
			// 过滤查询条件
			List<SearchFilter> filterToList = new ArrayList<>();
			filterToList.add(new SearchFilter("userId", SearchFilter.Operator.EQ, sysUser.getId()));
			filterToList.add(new SearchFilter("propertyKey", SearchFilter.Operator.EQ, key));

			SearchFilter[] filterToArray = filterToList.toArray(new SearchFilter[filterToList.size()]);
			Specification<SysUserProfile> specif = DynamicSpecifications.build(request, SysUserProfile.class,
					filterToArray);
			List<SysUserProfile> profiles = sysUserProfileService.findAll(specif);
			// 系统是否已存在此快捷方式=
			if (profiles.size() > 0) {
				// 更新
				userProfile.setId(profiles.get(0).getId());
			}
			sysUserProfileService.save(userProfile);
		} catch (Exception e) {
			logger.error("SystemSetting >> saveQucikLink (operator:{}  userProfile:{}) exception:{}", operator.getUid(),
					userProfile, e);
			return mapper
					.writeValueAsString(new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue()));
		}

		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 查询当前用户已配置的快速链接
	 */
	@RequestMapping("/findQuickLinkByUserId")
	public String findQuickLinkByUserId() throws Exception {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		GeneralResponseData<Object> responseData = new GeneralResponseData<>(ResponseStatus.SUCCESS.getValue());
		try {
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (null != sysUser) {
				responseData.setData(sysUserProfileService.findQuickLinkList(sysUser.getId()));
			}
		} catch (Exception e) {
			logger.error("SystemSetting >> findQuickLinkByUserId (operator:{}) exception:{}", operator.getUid(), e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue());
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 系统什级
	 */
	@RequiresPermissions("SystemSetting:*")
	@RequestMapping("/tools/upgrade")
	public String upgrade() throws Exception {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		logger.info("SystemSetting >> upgrade (operator:{})", operator.getUid());
		try {
			hostMonitorService.upgradeByCommand();
		} catch (Exception e) {
			logger.error("SystemSetting >> upgrade (operator:{}) exception:{}", operator.getUid(), e);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "内部错误"));
		}
		return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.SUCCESS.getValue()));
	}

	/**
	 * 系统什级
	 */
	@RequestMapping("/tools/internal/upgrade")
	public String internalUpgrade() throws Exception {
		logger.info("SystemSetting >> upgrade");
		try {
			hostMonitorService.upgradeByCommand();
		} catch (Exception e) {
			logger.error("SystemSetting >> upgrade exception:{}", e);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "内部错误"));
		}
		return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.SUCCESS.getValue()));
	}

	/**
	 * 为全系统推送消息
	 */
	@RequestMapping("/sendmsgtoall")
	public String sendMsgToAll(@RequestParam(value = "message", required = false) String message)
			throws IOException, InterruptedException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		logger.info("SystemSetting >> sendmsgtoall (operator:{},message:{})", operator.getUid(), message);
		try {
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 设置日期格式
			message = df.format(new Date()) + "&nbsp;&nbsp;&nbsp;" + operator.getUsername() + "</br>" + message;
			String info = CommonUtils.genSysMsg4WS(null, SystemWebSocketCategory.MessageToAllUser, message);
			redisService.convertAndSend(RedisTopics.BROADCAST, info);
		} catch (Exception e) {
			logger.error("SystemSetting >> sendmsgtoall (operator:{},message:{}) exception:{}", operator.getUid(),
					message, e);
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "发送失败！"));
		}
		return mapper.writeValueAsString(
				new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "发送成功！"));
	}

	/**
	 * Cabana 版本升级
	 */
	@RequestMapping("/cabanaVersion")
	public String cabanaVersion(@RequestParam(value = "data", required = false) String data,
			@RequestParam(value = "APP_PRE_UPDATE_MOBILES", required = false) String mobiles,
			@RequestParam(value = "version", required = false) String version)
			throws IOException, InterruptedException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		if (org.apache.commons.lang3.StringUtils.isBlank(data)
				&& org.apache.commons.lang3.StringUtils.isBlank(version)) {
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "设置参数不正确"));
		}
		logger.info("SystemSetting >> cabanaVersion (operator:{})", operator.getUid());
		try {
			if (org.apache.commons.lang3.StringUtils.isNotBlank(mobiles) && mobiles.length() > 10) {
				SysUserProfile profile = sysUserProfileService.findByUserIdAndPropertyKey(AppConstants.USER_ID_4_ADMIN,
						"APP_PRE_UPDATE_MOBILES");
				if (profile != null) {
					profile.setPropertyValue(mobiles);
				} else {
					profile = new SysUserProfile(AppConstants.USER_ID_4_ADMIN, "APP_PRE_UPDATE_MOBILES", mobiles);
				}
				sysUserProfileService.saveAndFlush(profile);
			}
			Object ret = cabanaService.version(data, mobiles, version);
			return mapper.writeValueAsString(ret);
		} catch (Exception e) {
			logger.error("SystemSetting >> cabanaVersion (operator:{}) exception:{}", operator.getUid(), e);
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "发送失败！"));
		}
	}

	@RequestMapping("/cabanaVersionList")
	public String cabanaVersionList() throws IOException, InterruptedException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		logger.info("SystemSetting >> cabanaVersionList (operator:{})", operator.getUid());
		try {
			Object ret = cabanaService.versionList();
			return mapper.writeValueAsString(ret);
		} catch (Exception e) {
			logger.error("SystemSetting >> cabanaVersionList (operator:{}) exception:{}", operator.getUid(), e);
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "发送失败！"));
		}
	}

	/**
	 * 打补丁
	 */
	@RequiresPermissions("SystemSetting:*")
	@RequestMapping("/app/patch")
	public String appPatch() throws Exception {
		try {
			SysUserProfile profile = sysUserProfileService.findByUserIdAndPropertyKey(AppConstants.USER_ID_4_ADMIN,
					"APP_PATCH_VERSION");
			SysUserProfile uri = sysUserProfileService.findByUserIdAndPropertyKey(AppConstants.USER_ID_4_ADMIN,
					"APP_PATCH_URL");
			if (org.apache.commons.lang3.StringUtils.isBlank(profile.getPropertyValue())
					|| org.apache.commons.lang3.StringUtils.isBlank(uri.getPropertyValue())) {
				return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "参数非法"));
			}
			sysUserProfileService.saveAndFlush(uri);
			cabanaService.appPatch(profile.getPropertyValue(), uri.getPropertyValue());
		} catch (Exception e) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "内部错误"));
		}
		return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.SUCCESS.getValue()));
	}

	@RequestMapping("/autoPatch")
	public String autoPatch(@RequestParam(value = "appPatchVersion") String appPatchVersion,
			@RequestParam(value = "appPatchUrl") String appPatchUrl) throws Exception {
		if (StringUtils.isBlank(appPatchVersion) || StringUtils.isBlank(appPatchUrl)) {
			log.info("autoPatch>>appPatchVersion {} appPatchUrl {}", appPatchVersion, appPatchUrl);
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "参数不允许为空"));
		}
		try {
			SysUserProfile version = sysUserProfileService.findByUserIdAndPropertyKey(true,
					AppConstants.USER_ID_4_ADMIN, "APP_PATCH_VERSION");
			version.setPropertyValue(appPatchVersion);
			systemSettingService.saveSetting(version);
			redisService.convertAndSend(RedisTopics.REFRESH_SYSTEM_PROFILE, mapper.writeValueAsString(version));

			SysUserProfile url = sysUserProfileService.findByUserIdAndPropertyKey(true, AppConstants.USER_ID_4_ADMIN,
					"APP_PATCH_URL");
			url.setPropertyValue(appPatchUrl);
			systemSettingService.saveSetting(url);
			redisService.convertAndSend(RedisTopics.REFRESH_SYSTEM_PROFILE, mapper.writeValueAsString(url));
			cabanaService.appPatch(appPatchVersion, appPatchUrl);
		} catch (Exception e) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "内部错误"));
		}
		return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.SUCCESS.getValue()));
	}

	@RequestMapping("/findSettingByKeys")
	public String autoPatch(@RequestParam(value = "keysArray") String[] keys) throws Exception {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		try {
			// 封装查询条件 DynamicSpecifications.build(request);
			List<SearchFilter> filterToList = new ArrayList<>();
			filterToList.add(new SearchFilter("userId", SearchFilter.Operator.EQ, AppConstants.USER_ID_4_ADMIN));
			filterToList.add(new SearchFilter("propertyKey", SearchFilter.Operator.IN, keys));
			SearchFilter[] filterToArray = filterToList.toArray(new SearchFilter[filterToList.size()]);
			Specification<SysUserProfile> specif = DynamicSpecifications.build(request, SysUserProfile.class,
					filterToArray);
			List<SysUserProfile> result = sysUserProfileService.findAll(specif);
			GeneralResponseData<List<SysUserProfile>> responseData = new GeneralResponseData<>(
					ResponseStatus.SUCCESS.getValue());
			responseData.setData(result);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "内部错误"));
		}
	}

	/**
	 * 获取支付宝入款配置
	 * 
	 * @param keys
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/findAliInConfig")
	public String findAliInConfig() throws Exception {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		try {
			SysUserProfile sysUserProfile = sysUserProfileService.findByUserIdAndPropertyKey(true,
					AppConstants.USER_ID_4_ADMIN, UserProfileKey.INCOME_ALI_CONFIG.getValue());
			GeneralResponseData<AliIncomeConfig> responseData = new GeneralResponseData<>(
					ResponseStatus.SUCCESS.getValue());
			// json串转化为对象返回页面
			AliIncomeConfig aliInConfig = JSONObject.parseObject(sysUserProfile.getPropertyValue(),
					AliIncomeConfig.class);
			responseData.setData(aliInConfig);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "内部错误"));
		}
	}

	/**
	 * 获取支付宝出款配置
	 * 
	 * @param keys
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/findAliOutConfig")
	public String findAliOutConfig() throws Exception {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		try {
			SysUserProfile sysUserProfile = sysUserProfileService.findByUserIdAndPropertyKey(true,
					AppConstants.USER_ID_4_ADMIN, UserProfileKey.OUT_ALI_CONFIG.getValue());
			GeneralResponseData<AliOutConfig> responseData = new GeneralResponseData<>(
					ResponseStatus.SUCCESS.getValue());
			// json串转化为对象返回页面
			AliOutConfig aliInConfig = JSONObject.parseObject(sysUserProfile.getPropertyValue(), AliOutConfig.class);
			responseData.setData(aliInConfig);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "内部错误"));
		}
	}

	@RequestMapping("/updateByParams")
	public String updateByParams(@RequestParam(value = "param") String param) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		try {
			logger.info(String.format("%s，操作人员：%s，参数：%s", "批量更新配置信息", operator.getUid(), param));
			JSONArray data = new JSONArray(param);
			List<SysUserProfile> result = sysUserProfileService.updateSysUserProfile(data);
			redisService.convertAndSend(RedisTopics.REFRESH_ALL_SYS_SETTING,
					org.apache.commons.lang3.StringUtils.EMPTY);
			GeneralResponseData<List<SysUserProfile>> responseData = new GeneralResponseData<>(
					ResponseStatus.SUCCESS.getValue());
			responseData.setData(result);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error(String.format("%s，参数：%s，结果：%s", "批量更新出入款额度", param, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getMessage()));
		}
	}

	/**
	 * 根据key更新value
	 * 
	 * @param param
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/updateParamByKey")
	public String updateAliParamByKey(@RequestParam(value = "param") String param) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(new GeneralResponseData<>(ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		try {
			logger.info(String.format("%s，操作人员：%s，参数：%s", "更新支付宝或威信出入款配置", operator.getUid(), param));
			JSONArray data = new JSONArray(param);
			sysUserProfileService.updateSysUserProfile(data);
			redisService.convertAndSend(RedisTopics.REFRESH_ALL_SYS_SETTING,
					org.apache.commons.lang3.StringUtils.EMPTY);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功 "));
		} catch (Exception e) {
			logger.error(String.format("%s，参数：%s，结果：%s", "批量更新出入款额度", param, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getMessage()));
		}
	}
}
