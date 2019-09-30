package com.xinbo.fundstransfer.service;

import com.xinbo.fundstransfer.domain.entity.BizAccountLevel;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.BizLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public interface LevelService {
	List<BizLevel> findByHandicapIdAndCodes(Integer handicapId, List<String> codes);

	List<BizAccountLevel> wrapAccountLevel(String[] levelArray, BizHandicap bizHandicap);

	List<Object> findLevelNameByHandicapName(String handicap);

	/**
	 * 单个保存或更新层级信息
	 * 
	 * @param data
	 *            待更新的层级信息
	 * @return 操作后的层级信息
	 */
	BizLevel save(BizLevel data);

	/**
	 * 批量保存或更新层级信息
	 * 
	 * @param dataList
	 *            层级集
	 */
	void save(List<BizLevel> dataList);

	/**
	 * 刷新内存中层级数据
	 * 
	 * @param level
	 *            层级
	 */
	void flushCache(BizLevel level);

	/**
	 * 分页获取层级信息
	 * <p>
	 * 层级中携带 盘口名，内/外层名
	 * </p>
	 */
	Page<BizLevel> findPage(Specification<BizLevel> specification, Pageable pageable);

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
	List<BizLevel> findByHandicapId(int handicapId);

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
	BizLevel findFromCache(int handicapId, String levelCode);

	/**
	 * 从内存中查询层级信息（根据层级ID）
	 * <p>
	 * 内存中，则直接从内存中直接取出，并直接返回；</br>
	 * 内存中没有，则从DB中获取，加载到内存中，并返回
	 * </p>
	 * 
	 * @param levelId
	 *            层级ID
	 * @return 层级
	 */
	BizLevel findFromCache(Integer levelId);

	List<BizLevel> findAll();

	/** 通过盘口id数组查询层级信息 */
	List<Object[]> findByHandicapIdsArray(Integer[] handicapIds);

	/** 通过盘口id查询层级信息 */
	List<Object[]> findByHandicapIdsArray(Integer handicapId);
}
