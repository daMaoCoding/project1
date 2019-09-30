package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import com.xinbo.fundstransfer.domain.entity.BizAccountExpOpr;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface AccountExpOprRepository
		extends BaseRepository<BizAccountExpOpr, Integer> {

	@Query(nativeQuery = true, value = "select e.* from biz_account_expopr e left join biz_account a on e.account_id=a.id where 1=1 and (:stTm is null OR e.client_time>=:stTm) and (:edTm IS NULL OR e.client_time<=:edTm) and (a.handicap_id in (:handiList)) and (:alias IS NULL OR a.alias=:alias) and (:accLike IS NULL OR a.account like concat('%',:accLike,'%')) and (:bank IS NULL OR a.bank_type=:bank) and (:opr IS NULL OR e.operator=:opr) ",
			countQuery = "select count(e.id) from biz_account_expopr e left join biz_account a on e.account_id=a.id where 1=1 and (:stTm is null OR e.client_time>=:stTm) and (:edTm IS NULL OR e.client_time<=:edTm) and (a.handicap_id in (:handiList)) and (:alias IS NULL OR a.alias=:alias) and (:accLike IS NULL OR a.account like concat('%',:accLike,'%')) and (:bank IS NULL OR a.bank_type=:bank) and (:opr IS NULL OR e.operator=:opr)")
	Page<BizAccountExpOpr> findByPage(@Param("stTm") Date stTm, @Param("edTm") Date edTm,
									  @Param("handiList") List<Integer> handiList, @Param("alias") String alias, @Param("accLike") String accLike,
									  @Param("bank") String bank, @Param("opr") String opr, Pageable pageable);
}