package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.xinbo.fundstransfer.domain.entity.BizDeductAmount;
import com.xinbo.fundstransfer.domain.entity.BizRebateUser;

import java.util.List;

public interface DeductAmountRepository extends BaseRepository<BizDeductAmount, Integer> {
	BizDeductAmount findByUid(String uid);
}