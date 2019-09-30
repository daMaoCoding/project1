package com.xinbo.fundstransfer.service.impl;

import com.xinbo.fundstransfer.domain.entity.BizOtherAccountBindEntity;
import com.xinbo.fundstransfer.domain.repository.OtherAccountBindRepository;
import com.xinbo.fundstransfer.service.OtherAccountBindService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.List;

@Service
public class OtherAccountBindServiceImpl implements OtherAccountBindService {
	@Autowired
	private OtherAccountBindRepository repository;

	@Override
	@Transactional
	public void deleteByAccountId(Integer accountId) {
		List<BizOtherAccountBindEntity> bindEntityList = repository.findByOtherAccountId4Delete(accountId);
		if (!ObjectUtils.isEmpty(bindEntityList)) {
			repository.deleteByOtherAccountId(accountId);
		}
	}

	@Override
	public BizOtherAccountBindEntity findByAccountIdAndBindId(Integer accountId, Integer bindedId) {
		return repository.findByOtherAccountIdAndAccountId(accountId, bindedId);
	}

	@Override
	public List<BizOtherAccountBindEntity> findByOtherAccountId(Integer otherAccountId) {
		return repository.findByOtherAccountId(otherAccountId);
	}
}
