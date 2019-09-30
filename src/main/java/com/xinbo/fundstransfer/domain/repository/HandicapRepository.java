package com.xinbo.fundstransfer.domain.repository;

import java.util.List;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import org.springframework.transaction.annotation.Transactional;

public interface HandicapRepository extends BaseRepository<BizHandicap, Integer> {

	BizHandicap findByCode(String code);

	BizHandicap findById2(Integer id);

	@Query(nativeQuery = true, value = "select DISTINCT id from biz_handicap where 1=1 and (:nameOrCode is null or (name like:nameOrCode) or (code like :nameOrCode ) or (id like :nameOrCode ))")
	List<Integer> findByNameLikeOrCodeLikeOrIdLike(@Param("nameOrCode") String nameOrCode);

	@Query(nativeQuery = true, value = "SELECT * FROM biz_handicap  h where 1=1 and not exists(select id from biz_level  where  handicap_id = h.id )")
	List<BizHandicap> findNewHandicap();

	/** 获取盘口层级关联关系 */
	@Query(nativeQuery = true, value = " select distinct h.id ,h.name,h.code,h.status,l.id as levelId,l.name as levelName,l.code as levelCode,l.status as levelStatus,l.curr_sys_level FROM fundsTransfer.biz_handicap h left JOIN fundsTransfer.biz_level l on  \n"
			+ " h.id = l.handicap_id  ORDER BY h.id ")
	List<Object[]> handicap2LevelListAll();

	/** 根据当前用户的数据权限获取盘口层级关联关系 */
	@Query(nativeQuery = true, value = " select distinct h.id,h.name,h.code,h.status,l.id as levelId,l.name as levelName,l.code as levelCode,l.handicap_id,l.status as levelStatus,l.curr_sys_level from  biz_handicap h, biz_level l \n"
			+ "where h.id = l.handicap_id and l.id in  (select distinct d.field_value from  sys_data_permission d where d.user_id=:userId and d.field_name='LEVELCODE') ORDER BY h.id "
			+ " UNION select DISTINCT h.id,h.name,h.code,h.status,null,null,null,null,null,null from  biz_handicap h ,sys_data_permission d where h.id=d.field_value and  d.user_id=:userId and d.field_name='HANDICAPCODE'")
	List<Object[]> handicap2LevelList4User(@Param("userId") Integer userId);

	/** 获取正常状态的盘口信息 status =1 */
	List<BizHandicap> findByStatusEquals(Integer status);

	@Query(nativeQuery = true, value = "select DISTINCT id from biz_handicap where 1=1 and status=1 and zone=:zone")
	List<Integer> findByZone(@Param("zone") String zone);

	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_handicap set zone=?2 where id=?1")
	int updateZoneByHandicapId(int handicapId, int zone);

	@Query(nativeQuery = true, value = "select DISTINCT code from biz_handicap where  status=1 and zone=:zone")
	List<Integer> findHandicapCodesByZone(@Param("zone") Integer zone);
}