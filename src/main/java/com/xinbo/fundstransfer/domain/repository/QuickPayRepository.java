package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import com.xinbo.fundstransfer.domain.entity.BizOtherAccountBindEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.xinbo.fundstransfer.domain.entity.BizOtherAccount;
import com.xinbo.fundstransfer.domain.entity.BizRebateUser;

public interface QuickPayRepository
		extends BaseRepository<BizOtherAccount, Integer> {

	BizOtherAccount findById2(Integer Id);

	BizOtherAccount findByAccountNo(String AccountNo);

	BizOtherAccount findByUid(Integer uid);

	@Transactional
	@Modifying
	@Query(nativeQuery = true, value = "delete from biz_other_account where id=?1")
	void deleteById(Integer id);

	@Transactional
	@Modifying
	@Query(nativeQuery = true, value = "insert into biz_other_account_bind(other_account_id,account_id) values (?1,?2)")
	void saveBind(Integer otherAccountId, Integer accountid);

	@Transactional
	@Modifying
	@Query(nativeQuery = true, value = "delete from biz_other_account_bind where other_account_id=?1 and account_id=?2")
	void deleteBind(Integer otherAccountId, Integer accountid);

	@Query(nativeQuery = true, value = "select count(1) from biz_other_account_bind where other_account_id=?1 and account_id=?2 limit 1")
	int findCounts(Integer otherAccountId, Integer accountid);

	@Transactional
	@Modifying
	@Query(nativeQuery = true, value = "delete from biz_other_account_bind where other_account_id=?1")
	void deleteBindAll(Integer otherAccountId);

	@Query(nativeQuery = true, value = "select count(1) from biz_other_account_bind where account_id=?1 limit 1")
	int findByAccountId(Integer accountid);

}