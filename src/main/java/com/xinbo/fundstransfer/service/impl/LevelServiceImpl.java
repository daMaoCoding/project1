package com.xinbo.fundstransfer.service.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.xinbo.fundstransfer.domain.entity.BizAccountLevel;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.BizLevel;
import com.xinbo.fundstransfer.domain.enums.CurrentSystemLevel;
import com.xinbo.fundstransfer.domain.repository.LevelRepository;
import com.xinbo.fundstransfer.service.HandicapService;
import com.xinbo.fundstransfer.service.LevelService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
public class LevelServiceImpl implements LevelService {
	@Autowired
	private LevelRepository levelRepository;
	@Autowired
	private HandicapService handicapService;
	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public List<BizLevel> findByHandicapIdAndCodes(Integer handicapId, List<String> codes) {
		return levelRepository.findByHandicapIdAndCodeIn(handicapId, codes);
	}

	@Override
	public List<BizAccountLevel> wrapAccountLevel(String[] levelArray, BizHandicap bizHandicap) {
		List<BizAccountLevel> accountLevelToList = null;
		if (levelArray != null && levelArray.length > 1) {
			accountLevelToList = new ArrayList<>();
			for (String levelCode : levelArray) {
				BizLevel level = findFromCache(bizHandicap.getId(), levelCode);
				if (level != null) {
					accountLevelToList.add(new BizAccountLevel(null, level.getId()));
				}
			}
		}
		return accountLevelToList;
	}

	/**
	 * 层级缓存</br>
	 * key#levelId 层级ID</br>
	 * key#handicapId:levelCode 盘口ID 与层级编码组合</br>
	 */
	private static final Cache<Object, BizLevel> levelCacheBuilder = CacheBuilder.newBuilder().maximumSize(20000)
			.expireAfterWrite(4, TimeUnit.DAYS).build();

	@Override
	public List<Object> findLevelNameByHandicapName(String handicap) {
		return levelRepository.findLevelNameByHandicapName(Integer.valueOf(handicap));
	}

	/**
	 * 单个保存或更新层级信息
	 *
	 * @param data
	 *            待更新的层级信息
	 * @return 操作后的层级信息
	 */
	@Override
	@Transactional
	public BizLevel save(BizLevel data) {
		if (Objects.isNull(data)) {
			return null;
		}
		return levelRepository.save(data);
	}

	/**
	 * 批量保存或更新层级信息
	 *
	 * @param dataList
	 *            层级集
	 */
	@Override
	@Transactional
	public void save(List<BizLevel> dataList) {
		if (CollectionUtils.isEmpty(dataList)) {
			return;
		}
		levelRepository.save(dataList);
	}

	/**
	 * 刷新内存中层级数据
	 *
	 * @param level
	 *            层级
	 */
	@Override
	public void flushCache(BizLevel level) {
		if (Objects.nonNull(level)) {
			levelCacheBuilder.put(level.getId(), level);
			levelCacheBuilder.put(String.format("%d:%s", level.getHandicapId(), level.getCode()), level);
		}
	}

	/**
	 * 分页获取层级信息
	 * <p>
	 * 层级中携带 盘口名，内/外层名
	 * </p>
	 */
	@Override
	public Page<BizLevel> findPage(Specification<BizLevel> specification, Pageable pageable) {
		Page<BizLevel> page = levelRepository.findAll(specification, pageable);
		for (BizLevel level : page.getContent()) {
			if (Objects.isNull(level.getCurrSysLevel())) {
				level.setCurrSysLevelName(StringUtils.EMPTY);
			} else {
				CurrentSystemLevel curr = CurrentSystemLevel.valueOf(level.getCurrSysLevel());
				if (Objects.isNull(curr)) {
					level.setCurrSysLevelName(StringUtils.EMPTY);
				} else {
					level.setCurrSysLevelName(curr.getName());
				}
			}
			level.setHandicapName(handicapService.findFromCacheById(level.getHandicapId()).getName());
		}
		return page;
	}

	/**
	 * 获取某盘口下的所有层级
	 * <p>
	 * 包含非可用层级
	 * </p>
	 *
	 * @param handicapId
	 *            盘口ID
	 * @return 层级集合
	 */
	@Override
	public List<BizLevel> findByHandicapId(int handicapId) {
		return levelRepository.findByHandicapId(handicapId);
	}

	/**
	 * 从内存中查询层级（根据盘口ID与层级编码）
	 * <p>
	 * 内存中，则直接从内存中直接取出，并直接返回；</br>
	 * 内存中没有，则从DB中获取，加载到内存中，并返回
	 * </p>
	 *
	 * @param handicapId
	 *            盘口ID
	 * @param levelCode
	 *            层级编码
	 * @return 层级
	 */
	@Override
	public BizLevel findFromCache(int handicapId, String levelCode) {
		if (StringUtils.isBlank(levelCode)) {
			return null;
		}
		BizLevel level = levelCacheBuilder.getIfPresent(handicapId + ":" + levelCode);
		if (Objects.isNull(level)) {
			level = levelRepository.findByHandicapIdAndCode(handicapId, levelCode);
			flushCache(level);
		}
		return level;
	}

	/**
	 * 从内存中查询层级信息
	 * <p>
	 * 内存中，则直接从内存中直接取出，并直接返回；</br>
	 * 内存中没有，则从DB中获取，加载到内存中，并返回
	 * </p>
	 *
	 * @param levelId
	 *            层级ID
	 * @return 层级
	 */
	@Override
	public BizLevel findFromCache(Integer levelId) {
		if (Objects.isNull(levelId)) {
			return null;
		}
		BizLevel level = levelCacheBuilder.getIfPresent(levelId);
		if (Objects.isNull(level)) {
			level = levelRepository.findById2(levelId);
			flushCache(level);
		}
		return level;
	}

	/** 查询所有层级信息 */
	@Override
	public List<BizLevel> findAll() {
		return levelRepository.findAll();
	}

	@Override
	public List<Object[]> findByHandicapIdsArray(Integer[] handicapIds) {
		String sql = "select * from biz_level where 1=1 and ";
		if (handicapIds.length == 1) {
			sql += " handicap_id=" + handicapIds[0];
		} else {
			sql += " handicap_id in (";
			for (int i = 0; i < handicapIds.length; i++) {
				if (i < handicapIds.length - 1) {
					sql += handicapIds[i] + ",";
				} else {
					sql += handicapIds[i] + ")";
				}
			}
		}
		List<Object[]> levelList = entityManager.createNativeQuery(sql).getResultList();
		return levelList;
	}

	@Override
	public List<Object[]> findByHandicapIdsArray(Integer handicapId) {
		String sql = "select * from biz_level where 1=1 and  handicap_id=" + handicapId;
		List<Object[]> levelList = entityManager.createNativeQuery(sql).getResultList();
		return levelList;
	}
}
