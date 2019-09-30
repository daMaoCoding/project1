package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import com.xinbo.fundstransfer.domain.entity.BizRebateContact;

public interface RebateContactRepository extends BaseRepository<BizRebateContact, Long> {

	BizRebateContact findById2(Long Id);

	BizRebateContact findByUid(String Uid);
}