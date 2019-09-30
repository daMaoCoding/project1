package com.xinbo.fundstransfer.restful;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jndi.url.iiopname.iiopnameURLContextFactory;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.enums.OutwardTaskStatus;
import com.xinbo.fundstransfer.domain.pojo.TroubleShootDTO;
import com.xinbo.fundstransfer.service.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.*;

/**
 * Created by Administrator on 2018/9/19.
 */
@RestController
@RequestMapping("/r/taskReview")
@Slf4j
public class AsignFailedTaskController {
	@Autowired
	private ObjectMapper mapper;
	@Autowired
	private AsignFailedTaskService asignFailedTaskService;
	@Autowired
	private SysUserService sysUserService;
	@Autowired
	private SysDataPermissionService sysDataPermissionService;
	@Autowired
	private AccountService accountService;
	@Autowired
	private HandicapService handicapService;
	@Autowired
	private SysUserProfileService userProfileService;

	@RequestMapping("/lockTaskForCheck")
	public String lockTaskForCheck(@RequestParam(value = "taskId") Integer taskId) throws JsonProcessingException {
		GeneralResponseData<Map<String, List<Map<String, Object>>>> responseData;
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		if (taskId == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数丢失");
			return mapper.writeValueAsString(responseData);
		}
		try {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "排查锁定成功");
			int add = asignFailedTaskService.lockTaskForCheck(taskId, sysUser.getId());
			if (add <= 0) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "排查锁定失败");
			}
		} catch (Exception e) {
			log.error("AsignFailedTaskController.lockTaskForCheck fail,e :", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"排查锁定失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
		return mapper.writeValueAsString(responseData);
	}

	@RequestMapping("/getTaskReviewInfo")
	public String getTaskReviewInfo(@RequestParam(value = "status") int status) throws JsonProcessingException {
		GeneralResponseData<Map<String, List<Map<String, Object>>>> responseData;
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "接开始单成功");
		List<String> onlineUsers = asignFailedTaskService.getReviewingUserInRedis(999999);
		if (CollectionUtils.isEmpty(onlineUsers)) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "没有用户在线接单");
			return mapper.writeValueAsString(responseData);
		}
		// 统计在线接单人数 以及手中的任务数量 obj[0] operator obj[1] operatorCount obj[2] status obj[3]
		// statusCount
		List<Object[]> list = null;
		if (!CollectionUtils.isEmpty(onlineUsers)) {
			list = asignFailedTaskService.getReviewingUserAndReviewTask(onlineUsers, status);
		}
		// 统计暂停接单人数 以及手中的任务数量 obj[0] operator obj[1] operatorCount obj[2] status obj[3]
		// statusCount
		List<Object[]> list2 = asignFailedTaskService.getUserAndReviewTaskNotInRedis(onlineUsers);
		List<Map<String, Object>> ret6online = new ArrayList<>(), ret2onlie = new ArrayList<>(),
				ret6pause = new ArrayList<>(), ret2pause = new ArrayList<>();
		Map<String, List<Map<String, Object>>> retMap = new HashMap<>();
		if (!CollectionUtils.isEmpty(list)) {// 正在接单的
			list.forEach(p -> {
				Map<String, Object> map = new HashMap<>();
				String user = sysUserService.findFromCacheById(Integer.valueOf((String) p[0])).getUid();
				if (p[2] != null) {
					if (p[2].equals(OutwardTaskStatus.Failure.getStatus())) {
						map.put("user6", user);
						map.put("status6Count", p[3]);
						ret6online.add(map);
					} else if (p[2].equals(OutwardTaskStatus.ManagerDeal.getStatus())) {
						map.put("user2", user);
						map.put("status2Count", p[3]);
						ret2onlie.add(map);
					} else {
						map.put("user6", user);
						map.put("status6Count", p[3]);
						ret6online.add(map);
						map.put("user2", user);
						map.put("status2Count", p[3]);
						ret2onlie.add(map);
					}
				}
			});
			if (!CollectionUtils.isEmpty(ret2onlie)) {
				retMap.put("online2", ret2onlie);
			}
			if (!CollectionUtils.isEmpty(ret6online)) {
				retMap.put("online6", ret6online);
			}
		}
		if (!CollectionUtils.isEmpty(list2)) {// 暂停接单的
			list2.forEach(p -> {
				Map<String, Object> map = new HashMap<>();
				String user = sysUserService.findFromCacheById(Integer.valueOf(p[0].toString())).getUid();
				if (p[2] != null) {
					if (p[2].equals(OutwardTaskStatus.Failure.getStatus())) {
						map.put("pausedUser6", user);
						map.put("pausedStatus6Count", p[3]);
						ret6pause.add(map);
					} else if (p[2].equals(OutwardTaskStatus.ManagerDeal.getStatus())) {
						map.put("pausedUser2", user);
						map.put("pausedStatus2Count", p[3]);
						ret2pause.add(map);
					} else {
						map.put("pausedUser6", user);
						map.put("pausedStatus6Count", p[3]);
						ret6pause.add(map);
						map.put("pausedUser2", user);
						map.put("pausedStatus2Count", p[3]);
						ret2pause.add(map);
					}
				}
			});
			if (!CollectionUtils.isEmpty(ret2pause)) {
				retMap.put("paused2", ret2pause);
			}
			if (!CollectionUtils.isEmpty(ret6pause)) {
				retMap.put("paused6", ret6pause);
			}
		}
		responseData.setData(retMap);
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 接收或停止接收待排查单子
	 * 
	 * @param type
	 *            1 接单(socket处理) 2 暂停 3 结束 如果不传则只是查询在线用户数及手中的任务数
	 * @param userId
	 *            用户id
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/troubleShootAction")
	public String troubleShootAction(@RequestParam(value = "type", required = false) Integer type,
			@RequestParam(value = "userId") Integer userId) throws JsonProcessingException {
		log.debug("问题排查接单/暂停/结束 参数:{}", userId);
		String rest = returnNoDataPermissionOrNoLogin(null);
		if (StringUtils.isNotBlank(rest)) {
			return rest;
		}
		GeneralResponseData<String> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功");
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		int zone = userProfileService.getSysUserProfileZoneByUserId(userId);
		log.debug("根据用户id:{},获取区域结果:{}", userId, zone);
		if (zone == -1) {
			log.info("用户userId:{},uid:{},sysUser.getId:{},没有划分区域!", userId, sysUser.getUid(), sysUser.getId());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"用户:" + sysUser.getUid() + ",没有划分区域!");
			return mapper.writeValueAsString(responseData);
		}
		if (type != null) {
			try {
				if (type == 1) {
					log.debug("开始接单:userId:{},zone:{}", userId, zone);
					asignFailedTaskService.startTaskReview(userId);
					responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
							"开始接单成功");
				}
				if (type == 2) {
					log.debug("暂停接单:userId:{},zone:{}", userId, zone);
					asignFailedTaskService.pauseReviewTask(userId);
					responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
							"暂停接单成功");
				}
				if (type == 3) {
					log.debug("结束接单:userId:{},zone:{}", userId, zone);
					asignFailedTaskService.stopReviewTask(userId);
					responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
							"结束接单成功");
					return mapper.writeValueAsString(responseData);
				}
			} catch (Exception e) {
				log.info("任务排查操作失败: ", e);
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"操作失败:" + e.getStackTrace());
				responseData.setData(null);
				return mapper.writeValueAsString(responseData);
			}
		}
		return mapper.writeValueAsString(responseData);
	}

	private List<Integer> shooterList(List<Integer> shooterList, String pageType, String shooterInput, Integer userId) {
		if (StringUtils.isNotBlank(pageType)) {
			if ("1".equals(pageType)) {
				if (userId != null) {
					shooterList.add(userId);
				}
			} else {
				if (StringUtils.isNotBlank(shooterInput)) {
					List<SysUser> sysUser1 = sysUserService.findByUidLike(shooterInput);
					if (CollectionUtils.isEmpty(sysUser1)) {
						sysUser1 = sysUserService.findByNameLike(shooterInput);
					}
					if (!CollectionUtils.isEmpty(sysUser1)) {
						sysUser1.stream().forEach(p -> shooterList.add(p.getId()));
					}
				}
			}
		}
		return shooterList;
	}

	/**
	 * 任务排查页签 : queryType :正在排查3 正在接单1 已排查 2
	 *
	 * @param troubleShootDTO
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/troubleShoot")
	public String troubleShoot(@Valid TroubleShootDTO troubleShootDTO) throws JsonProcessingException {
		String res = returnNoDataPermissionOrNoLogin(troubleShootDTO);
		if (StringUtils.isNotBlank(res)) {
			return res;
		}
		boolean notPlatQuery = (troubleShootDTO == null || troubleShootDTO.getPlatFormQuery() == null
				|| troubleShootDTO.getPlatFormQuery().intValue() != 1);
		GeneralResponseData<List<Map<String, Object>>> responseData;
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		List<BizHandicap> bizHandicapList = notPlatQuery ? sysDataPermissionService.getHandicapByUserId(sysUser) : null;
		List<BizHandicap> bizHandicapList2 = notPlatQuery ? getHandicap(sysUser, bizHandicapList) : null;

		List<String> taskLocked = null;
		if ("1".equals(troubleShootDTO.getQueryType()) || "3".equals(troubleShootDTO.getQueryType())) {
			taskLocked = asignFailedTaskService.getLockedTaskIdByUserId(sysUser.getId(), "query");
		}
		String[] handicapCodes = CommonUtils.handicapCodes(bizHandicapList2, troubleShootDTO.getHandicap());
		List<Integer> shooterList = new ArrayList<>();
		shooterList = shooterList(shooterList, troubleShootDTO.getPageType(), troubleShootDTO.getShooter(),
				notPlatQuery ? sysUser.getId() : null);
		// 出款人
		Integer[] operatorIds = null;
		Integer[] fromAccount = null;
		if (StringUtils.isNotBlank(troubleShootDTO.getOutAccount())) {
			fromAccount = fromAccount(troubleShootDTO.getOutAccount());
		}
		if (StringUtils.isNotBlank(troubleShootDTO.getOperator())) {
			operatorIds = operatorIds(troubleShootDTO.getOperator());
		}
		// t.id
		// ,t.outward_request_id,t.amount,t.asign_time,t.time_consuming,t.operator,t.account_id,t.remark,t.screenshot,t.to_account,t.to_account_owner,t.handicap,t.level,t.member,t.order_no,t.status
		// ,r.operator as shooter ,
		// t.id
		// ,t.outward_request_id,t.amount,r.finish_time,t.time_consuming,t.operator,t.account_id,r.remark,t.screenshot,t.to_account,t.to_account_owner,t.handicap,t.level,t.member,t.order_no,t.status
		// ,r.operator as shooter ,r.finish_time - r.asign_time
		try {
			List<Object[]> list = asignFailedTaskService.troubleShootList(troubleShootDTO, operatorIds, fromAccount,
					shooterList, handicapCodes, taskLocked);
			List<Map<String, Object>> ret = new ArrayList<>();
			if (!CollectionUtils.isEmpty(list)) {
				list.forEach(p -> {
					Map<String, Object> map = new HashMap<>();
					map.put("handicap", p[11]);
					map.put("level", p[12]);
					map.put("member", p[13]);
					map.put("orderNo", p[14]);
					map.put("amount", p[2]);
					map.put("drawer", p[5] != null ? sysUserService.findFromCacheById((int) p[5]).getUid() : "机器");
					map.put("toAccount", p[9]);
					map.put("asignTime", p[3]);
					// map.put("timeConsume",
					// CommonUtils.convertTime2String((Integer) p[4]));
					map.put("remark",
							p[7] != null ? (p[7].toString()).replace("\r\n", "<br>").replace("\n", "<br>") : "");
					if ((p[7] != null && StringUtils.isNotBlank(p[7].toString()) && p[7].toString().contains("第三方")
							&& !p[7].toString().contains("第三方转")) || (p[5] != null && p[6] == null)) {
						map.put("thirdRemarkFlag", "yes");
					} else {
						map.put("thirdRemarkFlag", "no");
					}
					map.put("photo", p[8]);
					map.put("taskId", p[0]);
					map.put("reqId", p[1]);
					map.put("taskStatus", OutwardTaskStatus.findByStatus((int) p[15]).getMsg());
					if (p[16] != null) {
						SysUser sysUser1 = sysUserService.findFromCacheById(Integer.valueOf((String) p[16]));
						map.put("shooter", sysUser1 != null ? sysUser1.getUid() : "");
					} else {
						map.put("shooter", "");
					}
					if (Objects.nonNull(p[6])) {
						BizAccount bizAccount = accountService.getById((Integer) p[6]);
						if (Objects.nonNull(bizAccount)) {
							map.put("outAccountAlias", bizAccount.getAlias());
							map.put("outAccount", bizAccount.getAccount());
							// 开户人 姓 隐藏为*
							map.put("outAccountOwner", "*" + bizAccount.getOwner().substring(1));
							map.put("accountId", bizAccount.getId());
						}
					}
					if (StringUtils.isNotBlank(troubleShootDTO.getPageType())) {
						if ("1".equals(troubleShootDTO.getPageType())) {
							if ("1".equals(StringUtils.trim(troubleShootDTO.getQueryType()))
									|| "3".equals(StringUtils.trim(troubleShootDTO.getQueryType()))) {
								// 待排查 计算是否过了5分钟
								Long timeUsed = p[3] != null ? (System.currentTimeMillis() - ((Date) p[3]).getTime())
										: 0;
								boolean failedOutTime5 = p[3] != null
										? System.currentTimeMillis() - ((Date) p[3]).getTime() > 5 * 60 * 1000
										: false;
								map.put("failedOutTime5", failedOutTime5 == true && failedOutTime5);
								map.put("timeConsume", CommonUtils.convertTime2String(timeUsed));
							} else {
								map.put("failedOutTime5", false);
								map.put("timeConsume",
										CommonUtils.convertTime2String(Long.valueOf(p[17].toString()) * 1000));
							}
						} else {
							map.put("failedOutTime5", false);
							map.put("timeConsume",
									CommonUtils.convertTime2String(Long.valueOf(p[17].toString()) * 1000));
						}
					}
					ret.add(map);
				});
			}
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
			responseData.setData(CollectionUtils.isEmpty(ret) ? null : ret);
			responseData.setPage(new Paging());
			return mapper.writeValueAsString(responseData);
		} catch (JsonProcessingException e) {
			log.info("查询任务排查失败:{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "任务排查查询失败");
			responseData.setData(null);
			responseData.setPage(new Paging());
			return mapper.writeValueAsString(responseData);
		}
	}

	@RequestMapping("/troubleShootSum")
	public String troubleShootSum(@Valid TroubleShootDTO troubleShootDTO) throws JsonProcessingException {
		String res = returnNoDataPermissionOrNoLogin(troubleShootDTO);
		if (StringUtils.isNotBlank(res)) {
			return res;
		}
		boolean notPlatQuery = (troubleShootDTO == null || troubleShootDTO.getPlatFormQuery() == null
				|| troubleShootDTO.getPlatFormQuery().intValue() != 1);
		GeneralResponseData<String> responseData;
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		List<BizHandicap> bizHandicapList = notPlatQuery ? sysDataPermissionService.getHandicapByUserId(sysUser) : null;
		List<BizHandicap> bizHandicapList2 = notPlatQuery ? getHandicap(sysUser, bizHandicapList) : null;
		List<String> taskLocked = null;
		if ("1".equals(troubleShootDTO.getQueryType()) || "3".equals(troubleShootDTO.getQueryType())) {
			taskLocked = asignFailedTaskService.getLockedTaskIdByUserId(sysUser.getId(), "query");
		}
		String[] handicapCodes = CommonUtils.handicapCodes(bizHandicapList2, troubleShootDTO.getHandicap());
		List<Integer> shooterList = new ArrayList<>();
		shooterList = shooterList(shooterList, troubleShootDTO.getPageType(), troubleShootDTO.getShooter(),
				notPlatQuery ? sysUser.getId() : null);
		// 出款人
		Integer[] operatorIds = null;
		Integer[] fromAccount = null;
		if (StringUtils.isNotBlank(troubleShootDTO.getOutAccount())) {
			fromAccount = fromAccount(troubleShootDTO.getOutAccount());
		}
		if (StringUtils.isNotBlank(troubleShootDTO.getOperator())) {
			operatorIds = operatorIds(troubleShootDTO.getOperator());
		}
		double sum = 0;
		try {
			sum = asignFailedTaskService.troubleShootSum(troubleShootDTO, operatorIds, fromAccount, shooterList,
					handicapCodes, taskLocked);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询总金额成功");
			responseData.setData(String.valueOf(sum));
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			log.info("获取总金额失败:{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询总金额失败");
			responseData.setData(String.valueOf(sum));
			return mapper.writeValueAsString(responseData);
		}

	}

	// 对权限和参数等的校验
	private String returnNoDataPermissionOrNoLogin(TroubleShootDTO troubleShootDTO) throws JsonProcessingException {
		GeneralResponseData<String> responseData;
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		boolean notPlatQuery = (troubleShootDTO == null || troubleShootDTO.getPlatFormQuery() == null
				|| troubleShootDTO.getPlatFormQuery().intValue() != 1);
		if (sysUser == null && notPlatQuery) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆");
			responseData.setData(null);
			responseData.setPage(new Paging());
			return mapper.writeValueAsString(responseData);
		}
		List<BizHandicap> bizHandicapList = !notPlatQuery ? null
				: sysDataPermissionService.getHandicapByUserId(sysUser);
		if (notPlatQuery && CollectionUtils.isEmpty(bizHandicapList)) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "没有数据权限");
			responseData.setData(null);
			responseData.setPage(new Paging());
			return mapper.writeValueAsString(responseData);
		}
		if (troubleShootDTO != null) {
			List<String> taskLocked;
			// 正在排查的
			if ("1".equals(troubleShootDTO.getQueryType()) || "3".equals(troubleShootDTO.getQueryType())) {
				taskLocked = asignFailedTaskService.getLockedTaskIdByUserId(sysUser.getId(), "query");
				if ("3".equals(troubleShootDTO.getQueryType()) && CollectionUtils.isEmpty(taskLocked)) {
					responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
							"无数据");
					responseData.setData(null);
					responseData.setPage(new Paging());
					return mapper.writeValueAsString(responseData);
				}
			}
			List<Integer> shooterList = new ArrayList<>();
			shooterList = shooterList(shooterList, troubleShootDTO.getPageType(), troubleShootDTO.getShooter(),
					notPlatQuery ? sysUser.getId() : null);
			if (notPlatQuery && StringUtils.isNotBlank(troubleShootDTO.getShooter())) {
				if (CollectionUtils.isEmpty(shooterList)) {
					responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
							"查无数据");
					responseData.setData(null);
					responseData.setPage(new Paging());
					return mapper.writeValueAsString(responseData);
				}
			}
			Integer[] operatorIds;// 出款人
			Integer[] fromAccount;
			if (StringUtils.isNotBlank(troubleShootDTO.getOutAccount())) {
				fromAccount = fromAccount(troubleShootDTO.getOutAccount());
				if (fromAccount == null || fromAccount.length == 0) {
					responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
							"查询无数据");
					responseData.setData(null);
					responseData.setPage(new Paging());
					return mapper.writeValueAsString(responseData);
				}
			}
			if (StringUtils.isNotBlank(troubleShootDTO.getOperator())) {
				operatorIds = operatorIds(troubleShootDTO.getOperator());
				if (null == operatorIds || operatorIds.length == 0) {
					responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
							"查询无数据");
					responseData.setPage(new Paging());
					return mapper.writeValueAsString(responseData);
				}
			}
		}
		return null;
	}

	private List<BizHandicap> getHandicap(SysUser sysUser, List<BizHandicap> bizHandicapList) {
		List<BizHandicap> bizHandicapList2 = new ArrayList<>();
		int zone = userProfileService.getSysUserProfileZoneByUserId(sysUser.getId());
		if (zone != 0) {
			List<Object> handicapCodeList = handicapService.findHandicapCodesByZone(zone, sysUser.getId());
			if (!CollectionUtils.isEmpty(handicapCodeList)) {
				bizHandicapList.stream().forEach(p -> {
					if (handicapCodeList.contains(p.getCode())) {
						bizHandicapList2.add(p);
					}
				});
			}
		}
		return bizHandicapList2;
	}

	@RequestMapping("/troubleShootCount")
	public String troubleShootCount(@Valid TroubleShootDTO troubleShootDTO) throws JsonProcessingException {
		String res = returnNoDataPermissionOrNoLogin(troubleShootDTO);
		if (StringUtils.isNotBlank(res)) {
			return res;
		}
		boolean notPlatQuery = (troubleShootDTO == null || troubleShootDTO.getPlatFormQuery() == null
				|| troubleShootDTO.getPlatFormQuery().intValue() != 1);
		GeneralResponseData<String> responseData;
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		List<BizHandicap> bizHandicapList = notPlatQuery ? sysDataPermissionService.getHandicapByUserId(sysUser) : null;
		List<BizHandicap> bizHandicapList2 = notPlatQuery ? getHandicap(sysUser, bizHandicapList) : null;

		List<String> taskLocked = null;
		if ("1".equals(troubleShootDTO.getQueryType()) || "3".equals(troubleShootDTO.getQueryType())) {
			taskLocked = asignFailedTaskService.getLockedTaskIdByUserId(sysUser.getId(), "query");
		}
		String[] handicapCodes = CommonUtils.handicapCodes(bizHandicapList2, troubleShootDTO.getHandicap());
		List<Integer> shooterList = new ArrayList<>();
		shooterList = shooterList(shooterList, troubleShootDTO.getPageType(), troubleShootDTO.getShooter(),
				notPlatQuery ? sysUser.getId() : null);
		// 出款人
		Integer[] operatorIds = null;
		Integer[] fromAccount = null;
		if (StringUtils.isNotBlank(troubleShootDTO.getOutAccount())) {
			fromAccount = fromAccount(troubleShootDTO.getOutAccount());
		}
		if (StringUtils.isNotBlank(troubleShootDTO.getOperator())) {
			operatorIds = operatorIds(troubleShootDTO.getOperator());
		}
		long count;
		try {
			count = asignFailedTaskService.troubleShootCount(troubleShootDTO, operatorIds, fromAccount, shooterList,
					handicapCodes, taskLocked);
			Paging page;
			page = CommonUtils.getPage(troubleShootDTO.getPageNo() + 1, troubleShootDTO.getPageSize(),
					String.valueOf(count));
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "获取总记录成功");
			responseData.setPage(page);
		} catch (Exception e) {
			log.info("总记录失败：{}", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue());
		}
		return mapper.writeValueAsString(responseData);
	}

	private Integer[] fromAccount(String accountAlias) {
		Integer[] fromAccount = null;
		if (StringUtils.isNotBlank(accountAlias)) {
			List<Integer> accountIdList = accountService.queryAccountIdsByAlias(StringUtils.trim(accountAlias));
			if (accountIdList != null && accountIdList.size() > 0) {
				fromAccount = new Integer[accountIdList.size()];
				for (int i = 0, L = accountIdList.size(); i < L; i++) {
					fromAccount[i] = accountIdList.get(i);
				}
			}
		}
		return fromAccount;
	}

	private Integer[] operatorIds(String operatorName) {
		Integer[] operatorIds = null;
		if (StringUtils.isNotBlank(operatorName)) {
			List<SysUser> sysUserList = sysUserService.findByNameLike(StringUtils.trimToEmpty(operatorName));
			if (null != sysUserList && sysUserList.size() > 0) {
				operatorIds = new Integer[sysUserList.size()];
				for (int i = 0, L = sysUserList.size(); i < L; i++) {
					operatorIds[i] = sysUserList.get(i).getId();
				}
			}
		}
		return operatorIds;
	}

}
