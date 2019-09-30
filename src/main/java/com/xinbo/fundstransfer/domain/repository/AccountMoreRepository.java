package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import com.xinbo.fundstransfer.domain.entity.BizAccountMore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

public interface AccountMoreRepository extends BaseRepository<BizAccountMore, Integer> {

	BizAccountMore findByUid(String uid);

	List<BizAccountMore> findByIdIn(List<Integer> ids);

	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_account_more set balance=?2 where id=?1")
	void updateBalance(Integer id, BigDecimal balance);

	BizAccountMore findByMoible(String moible);

	@Query(nativeQuery = true, value = "select * from biz_account_more where accounts like concat('%',?1,'%')")
	List<BizAccountMore> getAccountMoreByaccountId(int accountId);

	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_account_more set moible=?2 where uid=?1")
	void updateMobileByUid(String uid, String mobile);

	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_account_more set margin=?2,linelimit=?2 where id=?1")
	void updateMargin(Integer id, BigDecimal margin);

	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_account_more set accounts=?2 where id=?1")
	void updateAccountsByid(Integer id, String accounts);

	@Query(nativeQuery = true, value = "select more.accounts from biz_account_more more where in_flw_activity=?1")
	List<String> findAccountsByInFlwActivity(int in_flw_activity);

	@Query(nativeQuery = true, value = "select * from biz_account_more more where 1=? ")
	Page<BizAccountMore> queryAccountMoreList(String queryString, Pageable pageable);

	@Query(nativeQuery = true, value = "select distinct more.moible from biz_account_more more where in_flw_activity=?1 and id not in (?2)")
	List<String> findMoibleListByConditions(int inFlwActivity, List<Integer> notInIdList);
}
