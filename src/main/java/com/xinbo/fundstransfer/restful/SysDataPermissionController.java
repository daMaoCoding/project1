package com.xinbo.fundstransfer.restful;

import java.util.*;

import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.BizLevel;
import com.xinbo.fundstransfer.domain.entity.SysDataPermission;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.enums.SysDataPermissionENUM;
import com.xinbo.fundstransfer.domain.pojo.UserCategory;
import com.xinbo.fundstransfer.service.*;

@RestController
@RequestMapping("/r/permission")
public class SysDataPermissionController extends BaseController {
	private static final Logger logger = LoggerFactory.getLogger(SysDataPermissionController.class);
	@Autowired
	private SysDataPermissionService sysDataPermissionService;
	@Autowired
	private HandicapService handicapService;
	@Autowired
	private LevelService levelService;
	@Autowired
	private SysUserService userService;
	@Autowired
	private AccountService accountService;
	private ObjectMapper mapper = new ObjectMapper();

	/** 根据用户分配的盘口权限查询分配的层级权限 */
	@RequestMapping("/findLevel4UserByHandicapId")
	public String findLevel4UserByHandicapId(@RequestParam(value = "handicapId") Integer handicapId)
			throws JsonProcessingException {
		logger.debug("根据盘口权限查询层级权限：handicapId{}", handicapId);
		SysUser user = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (user == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		List<BizLevel> ret = new ArrayList<>();
		BizHandicap bizHandicap = handicapService.findFromCacheById(handicapId);
		if (bizHandicap != null && bizHandicap.getStatus() == 1) {
			List<BizLevel> bizLevelList = levelService.findByHandicapId(bizHandicap.getId());
			List<SysDataPermission> permList = sysDataPermissionService.findSysDataPermission(user.getId());
			if (!CollectionUtils.isEmpty(permList) && !CollectionUtils.isEmpty(bizLevelList)) {
				bizLevelList.forEach(p -> permList.forEach(q -> {
					if (SysDataPermissionENUM.LEVELCODE.getValue().equals(q.getFieldName())) {
						if (Integer.valueOf(q.getFieldValue()) == p.getId()) {
							ret.add(p);
						}
					}
				}));
			}
		}
		GeneralResponseData<List<BizLevel>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		responseData.setData(ret);
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 查询用户数据权限 盘口 层级 该接口只适用于用户管理 权限 handicapIdArray 传值 则表示根据盘口查询层级信息 包括用户已分配的和未分配的
	 * type "handicapOwn" 表示属于某个盘口下的
	 */
	@RequestMapping("/find4User")
	public String find4User(@RequestParam(value = "userId") Integer userId,
			@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "handicapIdArray", required = false) Integer[] handicapIdArray)
			throws JsonProcessingException {
		GeneralResponseData<Map<String, List<?>>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		Map<String, List<?>> result = new HashMap<>();
		try {
			logger.debug("查询用户权限参数：userId{}", userId);
			SysUser user = null;
			if (userId != null) {
				user = userService.findFromCacheById(userId);
			}
			if (user == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			// Integer userHandicapId = UserCategory.getHandicapId(user.getCategory());
			// handicapIdArray = userHandicapId != null ? new Integer[] { userHandicapId } :
			// handicapIdArray;
			List<SysDataPermission> permList = sysDataPermissionService.findSysDataPermission(userId);

			List<BizHandicap> bizHandicapList = new ArrayList<>();
			List<Map<String, Object>> levelMapList = new ArrayList<>(), bizLevelMapList = new LinkedList<>();
			Set<Integer> permHandicapIdSet = new HashSet<>(), tmp = new HashSet<>(), permLevelIdSet = new HashSet<>();

			// 盘口change事件 显示所有层级 已分配的层级选中
			if (handicapIdArray != null && handicapIdArray.length > 0) {
				// if (org.apache.commons.lang3.StringUtils.isNotBlank(type) &&
				// "handicapOwn".equals(type)) {
				// // 盘口下的所有层级信息
				// List<BizLevel> levelList = levelService.findByHandicapId(handicapIdArray[0]);
				// BizHandicap bizHandicap =
				// handicapService.findFromCacheById(handicapIdArray[0]);
				// List<Integer> permLevelIdList = new ArrayList<>();
				// if (!CollectionUtils.isEmpty(permList)) {
				// permList.forEach(p -> {
				// if (SysDataPermissionENUM.LEVELCODE.getValue().equals(p.getFieldName())) {
				// permLevelIdList.add(Integer.valueOf(p.getFieldValue()));
				// }
				// });
				// }
				// if (bizHandicap != null && !CollectionUtils.isEmpty(levelList)) {
				// levelList.forEach(p -> {
				// Map<String, Object> levelMap = new HashMap<>();
				// levelMap.put("id", p.getId());
				// levelMap.put("name", p.getName());
				// levelMap.put("code", p.getCode());
				// if (bizHandicap != null) {
				// levelMap.put("parent", bizHandicap.getName());
				// } else {
				// levelMap.put("parent", null);
				// }
				// if (permLevelIdList.toString().indexOf(p.getId().toString()) > -1) {
				// levelMap.put("selected", "yes");
				// bizLevelMapList.add(levelMap);
				// } else {
				// levelMap.put("selected", "no");
				// levelMapList.add(levelMap);
				// }
				// });
				// bizHandicapList.add(bizHandicap);
				// result.put("bizLevelMapList", bizLevelMapList);// 用户层级权限
				// result.put("bizHandicapList", bizHandicapList);// 用户盘口权限
				// result.put("handicapList", null);// 未分配的盘口数据
				// result.put("levelMapList", levelMapList);// 未分配给用户的层级权限
				// } else {
				// result.put("bizLevelMapList", null);// 用户层级权限
				// result.put("bizHandicapList", null);// 用户盘口权限
				// result.put("handicapList", null);// 未分配的盘口数据
				// result.put("levelMapList", null);// 未分配给用户的层级权限
				// }
				// responseData.setData(result);
				// return mapper.writeValueAsString(responseData);
				// } else {
				Set<Integer> handicapIdArraySet = new HashSet<>(Arrays.asList(handicapIdArray));
				if (!CollectionUtils.isEmpty(permList)) {
					permList.forEach(p -> {
						if (SysDataPermissionENUM.HANDICAPCODE.getValue().equals(p.getFieldName())) {
							if (handicapIdArraySet.contains(Integer.valueOf(p.getFieldValue()))) {
								permHandicapIdSet.add(Integer.valueOf(p.getFieldValue()));
								handicapIdArraySet.remove(Integer.valueOf(p.getFieldValue()));
							}
						}
					});
				}
				List<Object[]> levelListPerm = new ArrayList<>();
				List<Object[]> levelList = new ArrayList<>();

				levelListPerm = getLevelListByHandicapArray(levelListPerm, permHandicapIdSet);
				levelList = getLevelListByHandicapArray(levelList, handicapIdArraySet);

				List<Map> ret = new LinkedList<>();
				List<Map> selectedYes = new LinkedList<>();
				List<Map> selectedNo = new LinkedList<>();
				Map map;
				if (!CollectionUtils.isEmpty(levelListPerm)) {
					for (int i = 0; i < levelListPerm.size(); i++) {
						BizHandicap bizHandicap = handicapService.findFromCacheById((Integer) levelListPerm.get(i)[3]);
						map = new HashMap();
						map.put("id", levelListPerm.get(i)[0]);
						map.put("name", levelListPerm.get(i)[1]);
						map.put("code", levelListPerm.get(i)[2]);
						map.put("parent", bizHandicap.getName());
						map.put("selected", "yes");
						selectedYes.add(map);
					}
					ret.addAll(selectedYes);
				}
				if (!CollectionUtils.isEmpty(levelList)) {
					for (int i = 0; i < levelList.size(); i++) {
						BizHandicap bizHandicap = handicapService.findFromCacheById((Integer) levelList.get(i)[3]);
						map = new HashMap();
						map.put("id", levelList.get(i)[0]);
						map.put("name", levelList.get(i)[1]);
						map.put("code", levelList.get(i)[2]);
						map.put("parent", bizHandicap.getName());
						map.put("selected", "yes");
						map.put("selected", "no");
						selectedNo.add(map);
					}
					ret.addAll(selectedNo);
				}
				result.put("result", ret);
				responseData.setData(result);
				return mapper.writeValueAsString(responseData);
				// }

			} else {
				if (!CollectionUtils.isEmpty(permList)) {
					for (int i = 0, l = permList.size(); i < l; i++) {
						BizLevel bizLevel;
						BizHandicap bizHandicap = null;
						if (SysDataPermissionENUM.LEVELCODE.getValue().equals(permList.get(i).getFieldName())) {
							bizLevel = levelService.findFromCache(Integer.valueOf(permList.get(i).getFieldValue()));
							if (bizLevel != null) {
								String handicapName = null;
								if (bizLevel.getHandicapId() != null) {
									bizHandicap = handicapService.findFromCacheById(bizLevel.getHandicapId());
									handicapName = bizHandicap.getName();
								}
								permLevelIdSet.add(bizLevel.getId());
								map2List(bizLevelMapList, bizLevel, handicapName);
								// bizLevelMapList.add(levelMap);
							}
						} else if (SysDataPermissionENUM.HANDICAPCODE.getValue()
								.equals(permList.get(i).getFieldName())) {
							bizHandicap = handicapService
									.findFromCacheById(Integer.valueOf(permList.get(i).getFieldValue()));
						}
						if (bizHandicap == null || !Objects.equals(bizHandicap.getStatus(), 1)) {
							continue;
						}
						if (permHandicapIdSet.size() > 0 && permHandicapIdSet.contains(bizHandicap.getId())) {
							continue;
						}
						tmp.add(bizHandicap.getId());
					}
					permHandicapIdSet.addAll(tmp);// 用户分配的盘口权限
					result.put("bizLevelMapList", bizLevelMapList);// 用户层级权限
					for (Integer handicapId : permHandicapIdSet) {
						BizHandicap bizHandicap = handicapService.findFromCacheById(handicapId);
						if (bizHandicap == null || !Objects.equals(bizHandicap.getStatus(), 1)) {
							continue;
						}
						bizHandicapList.add(bizHandicap);
						levelService.findByHandicapId(handicapId).forEach((p -> {
							if (!permLevelIdSet.contains(p.getId()) && Objects.equals(1, p.getStatus())) {
								// permLevelIdSet 用户分配的层级id
								// 添加未分配的层级
								map2List(levelMapList, p, bizHandicap.getName());
							}
						}));
					}
					// 未分配的盘口数据
					List<BizHandicap> list = new ArrayList<>();
					handicapService.findAllToList().forEach(p -> {
						if (!permHandicapIdSet.contains(p.getId())) {
							list.add(p);
							// levelService.findByHandicapId(p.getId()).forEach((q -> {
							// map2List(levelMapList, q, p.getName());
							// }));
						}
					});
					result.put("handicapList", list);// 未分配的盘口数据
					result.put("levelMapList", levelMapList);// 未分配给用户的层级权限
					result.put("bizHandicapList", bizHandicapList);// 用户盘口权限

				} else {
					result.put("handicapList", handicapService.findAllToList());
					handicapService.findAllToList().forEach(p -> levelService.findByHandicapId(p.getId())
							.forEach((q -> map2List(levelMapList, q, p.getName()))));
					result.put("levelMapList", levelMapList);// 未分配给用户的层级权限
				}
			}
			responseData.setData(result);

		} catch (Exception e) {
			logger.error("查询用户权限失败：{}", e);
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询用户数据权限失败"));
		}
		return mapper.writeValueAsString(responseData);
	}

	private List<Object[]> getLevelListByHandicapArray(List<Object[]> levelList, Set<Integer> handicapIdArraySet) {
		if (handicapIdArraySet.size() > 0) {
			Integer[] handicapIds = new Integer[handicapIdArraySet.size()];
			List<Integer> list = new ArrayList(handicapIdArraySet);
			for (int i = 0; i < list.size(); i++) {
				handicapIds[i] = list.get(i);
			}
			levelList = levelService.findByHandicapIdsArray(handicapIds);
		}
		return levelList;
	}

	private void map2List(List list, BizLevel q, String handicapName) {
		Map<String, Object> levelMap = new HashMap<>();
		levelMap.put("id", q.getId());
		levelMap.put("name", q.getName());
		levelMap.put("code", q.getCode());
		levelMap.put("parent", handicapName);
		list.add(levelMap);
	}

	/**
	 * 用户点击权限--初始数据权限-层级/盘口
	 *
	 * @author
	 * @param userId
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/getPermission")
	public String getPermission(@RequestParam(value = "userId") Integer userId) throws JsonProcessingException {
		GeneralResponseData<Map<String, List<?>>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		Map<String, List<?>> resMap = new HashMap<>();
		try {// 先查询用户数据权限 如果没有数据权限 则查找系统全部数据权限
			logger.debug("获取用户数据权限：userId{}", userId);
			List<SysDataPermission> list1 = sysDataPermissionService.findSysDataPermission(userId);
			if (null != list1 && list1.size() > 0) {
				// 根据list1的层级value查询对应的盘口，组装盘口，层级信息返回页面展示
				List<BizHandicap> bizHandicapList = new ArrayList<>();
				List<Map<String, Object>> levelMapList = new LinkedList<>();

				for (int i = 0, L = list1.size(); i < L; i++) {
					BizHandicap bizHandicap = null;
					if (SysDataPermissionENUM.LEVELCODE.getValue().equals(list1.get(i).getFieldName())) {
						Map<String, Object> map = new HashMap<>();
						// 保存的时候保存 层级表主键（k:v） levelId：id值
						BizLevel bizLevel = levelService.findFromCache(Integer.valueOf(list1.get(i).getFieldValue()));
						// 根据盘口id查询盘口信息
						if (bizLevel != null && bizLevel.getHandicapId() != null) {
							bizHandicap = handicapService.findFromCacheById(bizLevel.getHandicapId());
						}
						if (bizHandicap == null || !Objects.equals(bizHandicap.getStatus(), 1)) {
							continue;
						}

						map.put("id", bizLevel.getId());
						map.put("name", bizLevel.getName());
						map.put("parent", bizHandicap.getName());
						levelMapList.add(map);// 组装(六合彩)层级1
					} else if (SysDataPermissionENUM.HANDICAPCODE.getValue().equals(list1.get(i).getFieldName())) {
						bizHandicap = handicapService.findFromCacheById(Integer.valueOf(list1.get(i).getFieldValue()));
					}
					if (bizHandicap != null)
						bizHandicapList.add(bizHandicap);
				}
				if (levelMapList != null && levelMapList.size() > 0) {
					resMap.put("bizLevelList", levelMapList);
				}

				if (null != bizHandicapList && bizHandicapList.size() > 0) {
					// 根据盘口id去重
					List<BizHandicap> newList = distinct(bizHandicapList);
					if (null != newList && newList.size() > 0) {
						resMap.put("bizHandicapList", newList);
					}
				}
			} else {
				// 获取所有盘口 所有层级
				List<BizHandicap> handicapList = handicapService.findAllToList();
				if (null != handicapList && handicapList.size() > 0) {
					resMap.put("handicapList", handicapList);
					List<Map<String, Object>> mapList = new ArrayList<>();
					for (int i = 0, L = handicapList.size(); i < L; i++) {
						if (handicapList.get(i) == null || !Objects.equals(handicapList.get(i).getStatus(), 1)) {
							continue;
						}
						List<BizLevel> levelList = levelService.findByHandicapId(handicapList.get(i).getId());
						String handicapName = handicapList.get(i).getName();
						if (levelList != null && levelList.size() > 0) {
							for (int j = 0, K = levelList.size(); j < K; j++) {
								Map<String, Object> map = new HashMap<>();
								map.put("parent", handicapName);
								map.put("name", levelList.get(j).getName());
								map.put("id", levelList.get(j).getId());
								mapList.add(map);
							}
						}
					}
					if (mapList != null && mapList.size() > 0) {
						resMap.put("levelListByHandicap", mapList);
					}
				}

			}
			responseData.setData(resMap);

		} catch (Exception e) {
			logger.error("获取权限失败：{}", e);
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询用户数据权限失败"));
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 保存
	 *
	 * @author
	 * @param id
	 * @param levelIds
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping(value = "/savePermission")
	public String savePermission(@RequestParam(value = "userId") Integer id,
			@RequestParam(value = "levelId") String levelIds, @RequestParam(value = "handicapId") String handicapIds)
			throws JsonProcessingException {
		GeneralResponseData<List<SysDataPermission>> responseData = new GeneralResponseData<List<SysDataPermission>>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "保存成功！");
		try {
			logger.debug("保存用户权限：参数 userid{},levelId{}", id, levelIds);
			// 根据用户id和层级id保存
			List<SysDataPermission> list = sysDataPermissionService.savePermission(levelIds, handicapIds, id);
			responseData.setData(list);
			// 同时查询用户的入款账号信息
			accountService.findIncomeAccountIdList4User(true, id);
		} catch (Exception e) {
			logger.error("保存用户权限失败：{}", e);
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "保存失败"));
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 初始化
	 *
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/initial")
	public String initial() throws JsonProcessingException {
		GeneralResponseData<List<BizHandicap>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		try {
			List<BizHandicap> handicapList = handicapService.findAllToList();
			if (null != handicapList && handicapList.size() > 0) {
				responseData.setData(handicapList);
			}

		} catch (Exception e) {
			logger.error("初始化失败：{}", e);
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 去重
	 *
	 * @param bizHandicapList
	 * @return
	 */
	public List<BizHandicap> distinct(List<BizHandicap> bizHandicapList) {
		if (bizHandicapList == null || bizHandicapList.size() == 0) {
			return Collections.emptyList();
		}
		Set<BizHandicap> set = new TreeSet<>((o1, o2) -> {
			// 字符串,则按照asicc码升序排列
			return o2.getId().compareTo(o1.getId());
		});
		set.addAll(bizHandicapList);
		return new ArrayList<>(set);
	}
}
