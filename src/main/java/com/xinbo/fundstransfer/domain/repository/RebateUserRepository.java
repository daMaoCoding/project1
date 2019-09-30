package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import com.xinbo.fundstransfer.domain.entity.BizRebateUser;

import java.util.List;

public interface RebateUserRepository
		extends BaseRepository<BizRebateUser, Integer> {

	BizRebateUser findById2(Integer Id);

	BizRebateUser findByUid(String Uid);

	BizRebateUser findByUserName(String userName);
}