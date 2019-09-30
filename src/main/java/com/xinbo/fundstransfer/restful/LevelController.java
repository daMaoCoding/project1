package com.xinbo.fundstransfer.restful;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.BizLevel;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.enums.CurrentSystemLevel;
import com.xinbo.fundstransfer.service.HandicapService;
import com.xinbo.fundstransfer.service.LevelService;
import com.xinbo.fundstransfer.service.SysDataPermissionService;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/r/level")
public class LevelController extends BaseController {

	private static final Logger logger = LoggerFactory.getLogger(LevelController.class);

	@Autowired
	private LevelService levelService;
	@Autowired
	private HandicapService handicapService;
	@Autowired
	private SysDataPermissionService dataPermissionService;

	private ObjectMapper mapper = new ObjectMapper();

	/** 解除层级的内外层绑定 */
	@RequestMapping("/unbindInnerOrOuterLevel")
	public String unbindInnerOrOuterLevel(@RequestParam(value = "type") Integer type,
			@RequestParam(value = "levelIds") Integer[] levelIds) throws JsonProcessingException {
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆!"));
		}
		if (type == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数丢失,请联系技术!"));
		}
		if (levelIds == null || levelIds.length == 0) {
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请选择需要解绑的层级!"));
		}
		List<BizLevel> updateLevelList = new ArrayList<>();
		for (Integer levelId : levelIds) {
			BizLevel level = levelService.findFromCache(levelId);
			if (level != null) {
				level.setCurrSysLevel(null);
				updateLevelList.add(level);
			}
		}
		try {
			if (updateLevelList != null && updateLevelList.size() > 0) {
				levelService.save(updateLevelList);
				for (BizLevel level : updateLevelList) {
					redisService.convertAndSend(RedisTopics.REFRESH_LEVEL, mapper.writeValueAsString(level));
				}
			}
		} catch (JsonProcessingException e) {
			logger.info("解绑内外层失败:{},参数:type:{},levelIds:{}", e, type, levelIds);
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "解绑内外层失败!"));
		}
		return mapper.writeValueAsString(
				new GeneralResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "解绑成功!"));
	}

	@RequestMapping("/saveBindRelation")
	public String saveBindRelation(@RequestParam(value = "bindRelation") String[] bindRelation)
			throws JsonProcessingException {
		try {
			GeneralResponseData<List<BizLevel>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			List<BizLevel> list = new ArrayList<>();
			for (String relation : bindRelation) {
				String[] array = relation.split(":");
				BizLevel level = levelService.findFromCache(Integer.valueOf(array[0]));
				if (level != null) {
					Integer current = array.length > 1 ? Integer.valueOf(array[1]) : null;
					if ((current != null && !current.equals(level.getCurrSysLevel()))
							|| (level.getCurrSysLevel() != null && !level.getCurrSysLevel().equals(current))) {
						level.setCurrSysLevel(current);
						list.add(level);

					}
				}
			}
			if (!CollectionUtils.isEmpty(list)) {
				levelService.save(list);
				for (BizLevel level : list) {
					redisService.convertAndSend(RedisTopics.REFRESH_LEVEL, mapper.writeValueAsString(level));
				}
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	/** 查询层级绑定信息 type=1 外层 2 内层 */
	@RequestMapping("/findPage")
	public String findPage(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "handicapId", required = false) Integer handicapId,
			@RequestParam(value = "type", required = false) Integer type) throws JsonProcessingException {
		try {
			GeneralResponseData<List<BizLevel>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			List<BizHandicap> dataToList = dataPermissionService.getOnlyHandicapByUserId(sysUser);
			if (CollectionUtils.isEmpty(dataToList)) {
				throw new Exception("当前用户未配置盘口权限，请配置后再进行查询!");
			}
			Specification<BizLevel> specification = (root, criteriaQuery, criteriaBuilder) -> {
				Predicate predicate = null;
				List<Integer> handicapIdToList = new ArrayList<Integer>();
				for (int i = 0; i < dataToList.size(); i++) {
					if (null != dataToList.get(i) && null != dataToList.get(i).getId()) {
						handicapIdToList.add(dataToList.get(i).getId());
					}
				}
				predicate = addAndPredicate(criteriaBuilder, predicate,
						root.get("handicapId").in(handicapIdToList.toArray()));
				if (handicapId != null) {
					predicate = addAndPredicate(criteriaBuilder, predicate,
							criteriaBuilder.equal(root.get("handicapId"), handicapId));
				}
				if (type != null) {
					predicate = addAndPredicate(criteriaBuilder, predicate,
							criteriaBuilder.or(criteriaBuilder.equal(root.get("currSysLevel"), type),
									criteriaBuilder.or(root.get("currSysLevel").isNull())));
				}
				predicate = addAndPredicate(criteriaBuilder, predicate,
						criteriaBuilder.notEqual(root.get("status"), 2));
				return predicate;
			};
			// 已绑定显示在前 未绑定显示在后面
			PageRequest pageRequest = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					Sort.Direction.DESC, "id");
			Page<BizLevel> page = levelService.findPage(specification, pageRequest);
			responseData.setData(page.getContent());
			responseData.setPage(new Paging(page));
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	private Predicate addAndPredicate(final CriteriaBuilder criteriaBuilder, final Predicate oldPredicate,
			final Predicate newPredicate) {
		return oldPredicate != null ? criteriaBuilder.and(oldPredicate, newPredicate) : newPredicate;
	}

	/**
	 * 查询层级信息
	 * 
	 * @param handicapCode
	 *            盘口编码
	 * @param enabled
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/list")
	public String list(@RequestParam(value = "handicapCode", required = false) String handicapCode,
			@RequestParam(value = "enabled", required = false) Integer enabled) throws JsonProcessingException {
		try {
			logger.debug("参数：{}", handicapCode);
			GeneralResponseData<List<BizLevel>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			List<BizLevel> dataToList = null;
			if (StringUtils.isNotBlank(handicapCode)) {
				BizHandicap handicap = handicapService.findFromCacheByCode(handicapCode);
				if (handicap != null) {
					dataToList = levelService.findByHandicapId(handicap.getId());
					if (!CollectionUtils.isEmpty(dataToList) && enabled != null) {
						dataToList = dataToList.stream().filter((p) -> enabled.equals(p.getStatus()))
								.collect(Collectors.toList());
					}
				}
			} else {
				dataToList = levelService.findAll();
			}
			responseData.setData(dataToList);
			return mapper.writeValueAsString(responseData);
		} catch (JsonProcessingException e) {
			logger.error("查询失败：{}", e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/listCurrentSystemLevel")
	public String listCurrentSystemLevel() throws JsonProcessingException {
		try {
			GeneralResponseData<List<Map<String, Object>>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			List<Map<String, Object>> data = new ArrayList<>();
			for (CurrentSystemLevel level : CurrentSystemLevel.values()) {
				Map<String, Object> item = new HashMap<>();
				item.put("value", level.getValue());
				item.put("name", level.getName());
				data.add(item);
			}
			responseData.setData(data);
			return mapper.writeValueAsString(responseData);
		} catch (JsonProcessingException e) {
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/findByPerm")
	public String findByPerm(@RequestParam(value = "handicapId") Integer handicapId) throws JsonProcessingException {
		try {
			logger.debug("参数：{}", handicapId);
			List<BizLevel> dataList = new ArrayList<>();
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (operator.getId() == AppConstants.USER_ID_4_ADMIN) {
				dataList = levelService.findByHandicapId(handicapId);
			} else {
				List<Integer> levelIdList = dataPermissionService.findLevelIdList(operator.getId());
				if (!CollectionUtils.isEmpty(levelIdList)) {
					for (Integer p : levelIdList) {
						BizLevel l = levelService.findFromCache(p);
						if (l != null) {
							if (handicapId.equals(l.getHandicapId())) {
								dataList.add(l);
							}
						}
					}
				}
			}
			GeneralResponseData<List<BizLevel>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			responseData.setData(dataList);
			return mapper.writeValueAsString(responseData);
		} catch (JsonProcessingException e) {
			logger.error("查询失败：{}", e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/listForIncomeAudit")
	public String listForIncomeAudit() throws JsonProcessingException {
		try {
			GeneralResponseData<List<BizLevel>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			List<BizLevel> dataToList = levelService.findByHandicapId(1);
			if (!CollectionUtils.isEmpty(dataToList)) {
				dataToList = dataToList.stream().filter((p) -> p.getStatus() == 1).collect(Collectors.toList());
			}
			responseData.setData(dataToList);
			return mapper.writeValueAsString(responseData);
		} catch (JsonProcessingException e) {
			logger.error("查询失败：{}", e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 根据盘口ID 查询层级 盘口名称 为了组装信息
	 */
	@RequestMapping("/getLevels")
	public String find(@RequestParam(value = "handicapIdsStr") String handicapIdsStr,
			@RequestParam(value = "handicapNamesStr") String handicapNamesStr) throws JsonProcessingException {
		try {
			logger.debug("参数：{},{}", handicapIdsStr, handicapNamesStr);
			GeneralResponseData<List<Map<String, Object>>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			String handicapId[] = handicapIdsStr.split(",");
			String handicapName[] = handicapNamesStr.split(",");
			List<Map<String, Object>> retList = new LinkedList<>();
			for (int i = 0, L = handicapId.length; i < L; i++) {
				List<BizLevel> dataToList = levelService.findByHandicapId(Integer.parseInt(handicapId[i]));
				if (dataToList != null && dataToList.size() > 0) {
					for (BizLevel data : dataToList) {
						Map<String, Object> map = new HashMap<>();
						map.put("id", data.getId());
						map.put("levelName", data.getName());
						map.put("handicapName", handicapName[i]);
						retList.add(map);
					}
				}
			}

			responseData.setData(retList.size() > 0 ? retList : null);

			return mapper.writeValueAsString(responseData);
		} catch (JsonProcessingException e) {
			logger.error("查询失败：{}", e);
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 通过盘口名称 查询层级名称<br>
	 * 2019-09-18：页面传入的是盘口id 所以需要根据id查询
	 */
	@RequestMapping("/getByHandicap")
	public String getByHandicap(@RequestParam(value = "handicap", required = false) String handicap)
			throws JsonProcessingException {
		GeneralResponseData<List<Object>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		try {
			logger.debug("参数：{}", handicap);
			if (StringUtils.isBlank(handicap)) {
				return null;
			}
			// 2019-09-18 盘口options改便选择层级查询异常修复
			List<Object> bizLevelList = levelService.findLevelNameByHandicapName(handicap);
			responseData.setData(bizLevelList);
		} catch (Exception e) {
			logger.error("通过盘口名称查询层级名称失败:{}", e);
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 通过盘口id 查询层级信息
	 */
	@RequestMapping("/getByHandicapId")
	public String getLevelListByHandicapId(@RequestParam(value = "handicapId") Integer handicapId)
			throws JsonProcessingException {
		GeneralResponseData<List<BizLevel>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		try {
			logger.debug("参数：{}", handicapId);
			List<BizLevel> bizLevelList = levelService.findByHandicapId(handicapId);
			if (bizLevelList != null && bizLevelList.size() > 0) {
				responseData.setData(bizLevelList);
			}
		} catch (Exception e) {
			logger.error("通过盘口id查询层级信息失败:" + e);
		}
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * 根据当前用户查询层级数据权限
	 * 
	 */
	@RequestMapping("/getLevelByCurrentUser")
	public String getLevelByCurrentUser() throws JsonProcessingException {
		GeneralResponseData<List<Map<String, Object>>> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		List<Integer> levelIdSList = dataPermissionService.findLevelIdList(sysUser.getId());
		if (!CollectionUtils.isEmpty(levelIdSList)) {
			List<Map<String, Object>> list = new ArrayList<>();
			levelIdSList.forEach((p) -> {
				BizLevel bizLevel = levelService.findFromCache(p);
				if (bizLevel != null) {
					Map<String, Object> map = new HashMap<>();
					BizHandicap bizHandicap = handicapService.findFromCacheById(bizLevel.getHandicapId());
					if (bizHandicap != null) {
						map.put("handicapName",
								StringUtils.isNotBlank(bizHandicap.getName()) ? bizHandicap.getName() : "");
					}
					map.put("id", bizLevel.getId());
					map.put("levelName", StringUtils.isNotBlank(bizLevel.getName()) ? bizLevel.getName() : "");
					list.add(map);
				}
			});
			responseData.setData(list);
		}
		return mapper.writeValueAsString(responseData);
	}
}
