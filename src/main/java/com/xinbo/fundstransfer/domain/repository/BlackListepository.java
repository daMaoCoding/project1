package com.xinbo.fundstransfer.domain.repository;

import java.util.List;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.xinbo.fundstransfer.domain.entity.BizBlackList;

public interface BlackListepository extends BaseRepository<BizBlackList, Integer> {

	List<BizBlackList> findByName(String name);
	List<BizBlackList> findByAccount(String account);
	BizBlackList findByNameAndAccount(String name, String account);
	
}