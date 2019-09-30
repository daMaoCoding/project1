package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import com.xinbo.fundstransfer.domain.entity.BizOtherAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;

public interface BizOtherAccountRepository
		extends BaseRepository<BizOtherAccountEntity, Integer> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query(value = " select a from BizOtherAccountEntity a where a.id=:id")
	BizOtherAccountEntity getByIdForLock(@Param("id") Integer id);

	BizOtherAccountEntity findByAccountNo(String accountNo);

	BizOtherAccountEntity findById2(Integer id);
}
