package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import com.xinbo.fundstransfer.domain.entity.BizLevel;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LevelRepository extends BaseRepository<BizLevel, Integer> {
	List<BizLevel> findByHandicapIdAndCurrSysLevel(Integer handicapId, Integer currentLevel);

	List<BizLevel> findByHandicapIdAndCodeIn(Integer handicapId, List<String> codes);

	List<BizLevel> findByHandicapId(Integer handicapId);

	BizLevel findByHandicapIdAndCode(Integer handicapId, String code);

	BizLevel findById2(Integer id);

	@Query(value = "select l.name from BizLevel l , BizHandicap h where l.handicapId = h.id and h.id =:handicap")
	List<Object> findLevelNameByHandicapName(@Param(value = "handicap") Integer handicap);
}