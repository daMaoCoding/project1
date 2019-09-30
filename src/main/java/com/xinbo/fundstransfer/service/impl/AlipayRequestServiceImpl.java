package com.xinbo.fundstransfer.service.impl;

import com.xinbo.fundstransfer.domain.entity.BizAlipayRequest;
import com.xinbo.fundstransfer.domain.repository.AlipayRequestRepository;
import com.xinbo.fundstransfer.service.AlipayRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlipayRequestServiceImpl implements AlipayRequestService {
	@Autowired
	private AlipayRequestRepository alipayRequestDao;

    @Override
    @Transactional
	public BizAlipayRequest save(BizAlipayRequest req) {
		return alipayRequestDao.saveAndFlush(req);
	}
}
