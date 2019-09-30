package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import com.xinbo.fundstransfer.domain.entity.BizThirdAccountEntity;

public interface BizThirdAccountEntityRepository extends BaseRepository<BizThirdAccountEntity, Integer> {

	BizThirdAccountEntity findDistinctByAccountId(Integer accountId);
}
