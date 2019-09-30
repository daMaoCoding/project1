package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import com.xinbo.fundstransfer.domain.entity.BizOtherAccountBindEntity;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.util.List;

public interface OtherAccountBindRepository
		extends BaseRepository<BizOtherAccountBindEntity, Integer> {

	List<BizOtherAccountBindEntity> findByOtherAccountId(Integer accountId);

	List<BizOtherAccountBindEntity> findByAccountId(Integer bindAccountId);

	BizOtherAccountBindEntity findByOtherAccountIdAndAccountId(Integer accountId, Integer bindAccountId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query(value = "select oab from BizOtherAccountBindEntity oab where oab.otherAccountId=:otherAccountId")
	List<BizOtherAccountBindEntity> findByOtherAccountId4Delete(@Param("otherAccountId") Integer otherAccountId);

	void deleteByOtherAccountId(Integer accountId);
}
