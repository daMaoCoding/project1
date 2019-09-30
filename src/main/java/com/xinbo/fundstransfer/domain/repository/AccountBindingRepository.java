package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import com.xinbo.fundstransfer.domain.entity.BizAccountBinding;

import java.util.List;

public interface AccountBindingRepository extends BaseRepository<BizAccountBinding, Integer> {

	List<BizAccountBinding> findByAccountId(Integer accountId);

	List<BizAccountBinding> findByBindAccountId(Integer bindAccountId);

	BizAccountBinding findDistinctFirstByAccountIdAndBindAccountId(Integer accountId, Integer bindAccountId);

}