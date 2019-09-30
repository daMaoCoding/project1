package com.xinbo.fundstransfer.service;

import java.util.List;

import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.SysUser;

public interface HandicapService {

	List<BizHandicap> findAllToList();

	List<BizHandicap> findAllZoneToList();

	BizHandicap findFromCacheById(Integer handicapId);

	BizHandicap findFromCacheByCode(String handicapCode);

	void flushCache();

	List<BizHandicap> findByUserId(Integer userId);

	BizHandicap save(BizHandicap o);

	List<Integer> findByNameLikeOrCodeLikeOrIdLike(String handicap);

	List<BizHandicap> findNewHandciap();

	/** 查询所有的盘口层级 */
	List<Object[]> handicap2LevelListAll();

	/** 根据当前用户的数据权限获取盘口层级关联关系 */
	List<Object[]> handicap2LevelList4User(Integer userId);

	/** 获取正常状态的盘口信息 status =1 */
	List<BizHandicap> findByStatusEqual(Integer status);

	boolean checkDistHandi(Integer zone);

	/**
	 * find the zone ID though the handicap ID
	 * <p>
	 * the handicap doesn't exist, the MANILA zone's ID as result.
	 * </p>
	 * 
	 * @param handiId
	 *            the handicap's ID
	 * @return the zone ID
	 */
	int findZoneByHandiId(Integer handiId);

	/**
	 * find zone ID though user ID
	 * <p>
	 * if user ID is null, {@code 0} as result
	 * </p>
	 * <p>
	 * if user belongs to Administration category , {@code 0} as result
	 * </p>
	 *
	 * @return zone ID
	 *
	 */
	int findZoneByUserId(Integer userId);

	/**
	 * find the handicap ID though the handicap ID
	 * <p>
	 * the handicap ID is null, MANILA zone ID as result
	 * </p>
	 *
	 * @param handiId
	 *            the handicap ID
	 * @return the handicap ID
	 * 
	 */
	int findHandiByHandiId(Integer handiId);

	List<Integer> findByZone(String zone);

	void updateZoneByHandicapId(Integer handicapId, Integer zone);

	// 根据区域id查询盘口编码
	List<Object> findHandicapCodesByZone(Integer zone, int userId);

	List<Integer> handicapIdList(SysUser operator);
}
