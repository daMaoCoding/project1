package com.xinbo.fundstransfer.service;

import com.xinbo.fundstransfer.domain.entity.BizOtherAccountBindEntity;

import java.util.List;

public interface OtherAccountBindService {

	void deleteByAccountId(Integer accountId);

	BizOtherAccountBindEntity findByAccountIdAndBindId(Integer accountId, Integer bindedId);

	List<BizOtherAccountBindEntity> findByOtherAccountId(Integer otherAccountId);

}
