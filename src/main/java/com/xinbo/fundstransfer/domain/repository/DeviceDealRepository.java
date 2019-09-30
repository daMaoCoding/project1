package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;

import com.xinbo.fundstransfer.domain.entity.BizDeviceDeal;

public interface DeviceDealRepository extends BaseRepository<BizDeviceDeal, String> {
	BizDeviceDeal findById2(String mobile);

}