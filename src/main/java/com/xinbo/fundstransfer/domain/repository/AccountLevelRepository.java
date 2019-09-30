package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import com.xinbo.fundstransfer.domain.entity.BizAccountLevel;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.util.List;

public interface AccountLevelRepository
		extends BaseRepository<BizAccountLevel, Integer> {

	List<BizAccountLevel> findByAccountIdIsIn(List<Integer> accountIds);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query(value = "select bzl from BizAccountLevel bzl  where bzl.accountId =:accountId")
	List<BizAccountLevel> findByAccountId4Delete(@Param("accountId") Integer accountIds);

	@Query(nativeQuery = true, value = "select bzl from biz_account_level bzl  where account_id =:accountId")
	List<BizAccountLevel> findByAccountIdIn(@Param("accountId") Integer accountId);

	List<BizAccountLevel> findByLevelId(Integer levelId);

	List<BizAccountLevel> findByAccountId(Integer accountId);

	List<BizAccountLevel> findByAccountIdAndLevelId(Integer accountId, Integer levelId);

	List<BizAccountLevel> findByLevelIdIn(List<Integer> levelIdList);

	/** 通过层级编码新增一条数据 */
	@Modifying
	@Query(nativeQuery = true, value = "insert into biz_account_level (account_id,level_id) values (?1,(select id from biz_level where code=?2 and handicap_id=(SELECT id FROM fundsTransfer.biz_handicap where code=?3) limit 1 ))")
	void insertByLevelCode(Integer accountId, String levelCode, String handicapCode);

	/** 通过层级ID新增一条数据 */
	@Modifying
	@Query(nativeQuery = true, value = "insert into biz_account_level (account_id,level_id) values (?1,?2 )")
	void insertByLevelCode(Integer accountId, Integer levelId);
}