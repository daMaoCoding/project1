package com.xinbo.fundstransfer.restful;

import java.util.*;

import com.xinbo.fundstransfer.component.redis.RedisTopics;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.service.HandicapService;
import com.xinbo.fundstransfer.service.SysDataPermissionService;

/**
 * 盘口类
 */
@RestController
@RequestMapping("/r/handicap")
public class HandicapController extends BaseController {
	private static final Logger logger = LoggerFactory.getLogger(HandicapController.class);
	@Autowired
	private HandicapService handicapService;
	@Autowired
	private SysDataPermissionService dataPermissionService;
	private ObjectMapper mapper = new ObjectMapper();

	/**
	 * 返回list<Map<handicapId,levelList>> 以盘口id为key的所有层级信息
	 * 
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/handicap2LevelListAll")
	public String handicap2LevelListMapAll() throws JsonProcessingException {
		GeneralResponseData<List<Map<Integer, List<Object[]>>>> responseData;
		try {
			List<Map<Integer, List<Object[]>>> retList = null;
			List<Object[]> list = handicapService.handicap2LevelListAll();
			retList = retList(list, retList);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功!");
			responseData.setData(retList);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.info("查询盘口层级关联列表失败:{}", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询失败!");
			return mapper.writeValueAsString(responseData);
		}
	}

	@RequestMapping("/flushCache")
	public String flushCache() throws JsonProcessingException {
		try {
			redisService.convertAndSend(RedisTopics.REFRESH_ALL_HANDICAP, StringUtils.EMPTY);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "Success:reload handicap"));
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "Fail: reload handicap"));
		}
	}

	/**
	 * 返回 当前人的数据权限下的 list<Map<handicapId,levelList>> 以盘口id为key的所有层级信息
	 *
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/handicap2LevelList4User")
	public String handicap2LevelListMap() throws JsonProcessingException {
		GeneralResponseData<List<Map<Integer, List<Object[]>>>> responseData;
		try {
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser == null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆!");
				return mapper.writeValueAsString(responseData);
			}
			List<Map<Integer, List<Object[]>>> retList = null;
			List<Object[]> list = handicapService.handicap2LevelList4User(sysUser.getId());
			retList = retList(list, retList);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功!");
			responseData.setData(retList);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.info("查询盘口层级关联列表失败:{}", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询失败!");
			return mapper.writeValueAsString(responseData);
		}
	}

	/** 组装 []->list->map->list */
	private List<Map<Integer, List<Object[]>>> retList(List<Object[]> list,
			List<Map<Integer, List<Object[]>>> retList) {
		if (list != null && list.size() > 0) {
			retList = new ArrayList<>();
			for (int i = 0; i < list.size(); i++) {
				Object[] p = list.get(i);
				if (retList.size() > 0) {
					Integer listOldLength = retList.size();
					for (int j = 0; j < retList.size();) {
						Map<Integer, List<Object[]>> map1 = retList.get(j);
						if (map1.containsKey(p[0])) {
							List<Object[]> list1 = map1.get(p[0]);
							list1.add(p);
							map1.put((Integer) p[0], list1);
							retList.remove(j);
							retList.add(map1);
							break;
						} else {
							j++;
						}
						if (j >= listOldLength) {
							wrap(p, retList);
						}
					}
				} else {
					wrap(p, retList);
				}
			}
		}
		return retList;
	}

	/** 组装 map->list */
	private void wrap(Object[] p, List<Map<Integer, List<Object[]>>> list) {
		Map<Integer, List<Object[]>> map = new HashMap();
		List<Object[]> list1 = new ArrayList<>();
		list1.add(p);
		map.put((Integer) p[0], list1);
		list.add(map);
	}

	@RequestMapping("/list")
	public String list() throws JsonProcessingException {
		try {
			GeneralResponseData<List<BizHandicap>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			List<BizHandicap> dataToList = handicapService.findAllToList();
			responseData.setData(dataToList);
			return mapper.writeValueAsString(responseData);
		} catch (JsonProcessingException e) {
			logger.error("查询所有盘口失败:{}", e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/listZone")
	public String listZone() throws JsonProcessingException {
		try {
			GeneralResponseData<List<BizHandicap>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			responseData.setData(handicapService.findAllZoneToList());
			return mapper.writeValueAsString(responseData);
		} catch (JsonProcessingException e) {
			logger.error("查询所有区域失败:{}", e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 根据用户查询该用户拥有的盘口
	 * 
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/findByPerm")
	public String findByPerm() throws JsonProcessingException {
		try {
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (operator == null) {
				GeneralResponseData<List<BizHandicap>> responseData = new GeneralResponseData<>(
						GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆");
				return mapper.writeValueAsString(responseData);
			}
			List<BizHandicap> dataToList = dataPermissionService.getHandicapByUserId(operator);
			GeneralResponseData<List<BizHandicap>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			responseData.setData(dataToList);
			return mapper.writeValueAsString(responseData);
		} catch (JsonProcessingException e) {
			logger.error("根据当前用户查询所有盘口失败:{}", e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 根据用户查询该用户拥有的盘口,如果没有则返回新盘口或者没有绑定权限的盘口
	 *
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/findByPermFirstThenNoPerm")
	public String findByPermFirstThenNoPerm() throws JsonProcessingException {
		try {
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (operator == null) {
				GeneralResponseData<List<BizHandicap>> responseData = new GeneralResponseData<>(
						GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆");
				return mapper.writeValueAsString(responseData);
			}
			List<BizHandicap> dataToList = dataPermissionService.findByPermFirstThenNoPerm(operator);
			GeneralResponseData<List<BizHandicap>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			responseData.setData(dataToList);
			return mapper.writeValueAsString(responseData);
		} catch (JsonProcessingException e) {
			logger.error("查询盘口失败:{}", e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 获取盘口信息 默认根据当前用户id查询 使用场景: 只获取当前人的数据权限
	 * 
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/getHandicap")
	public String getCurrentUserHandicap() throws JsonProcessingException {
		GeneralResponseData<List<BizHandicap>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆!");
			return mapper.writeValueAsString(responseData);
		}
		try {
			logger.debug("根据用户id查询盘口:{}", sysUser.getId());
			List<BizHandicap> bizHandicapList = handicapService.findByUserId(sysUser.getId());
			responseData.setData(bizHandicapList);
		} catch (Exception e) {
			logger.error("根据用户id查询盘口失败：" + e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"根据用户id查询盘口失败");
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 更新盘口所属区域
	 *
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/updateZone")
	public String updateZone(@RequestParam(value = "keysArray") String[] keys) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		try {
			logger.info("Handicap >> updateZone (operator:{}  keys:{})", operator.getUid(), keys);
			GeneralResponseData responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "保存成功");
			if (null != keys && keys.length > 0) {
				for (int i = 0, L = keys.length; i < L; i++) {
					String[] key = keys[i].split(";");
					Integer handicapId = Integer.parseInt(key[0]);
					Integer zone = Integer.parseInt(key[1]);
					handicapService.updateZoneByHandicapId(handicapId, zone);
				}
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error("Handicap >> updateZone (operator:{}  keys:{}) exception:{}", operator.getUid(), keys, e);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "保存失败"));
		}
	}
}
