package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import com.xinbo.fundstransfer.domain.entity.BizCommonRemarkEntity;

public interface BizCommonRemarkEntityRepository extends BaseRepository<BizCommonRemarkEntity, Integer> {

	BizCommonRemarkEntity findFirstByBusinessIdAndTypeAndStatusOrderByCreateTimeDesc(Integer businessId, String type,
			Byte status);
}
