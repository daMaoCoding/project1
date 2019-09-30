package com.xinbo.fundstransfer.service;

import java.util.List;

import com.xinbo.fundstransfer.domain.entity.BizAccountLevel;
import com.xinbo.fundstransfer.domain.entity.BizAccountSync;

/**
 * 手机号管理
 * 
 * @author 
 *
 */
public interface AccountSyncService {

	BizAccountSync saveAndFlush(BizAccountSync syncVo);

	/** 存储账号层级信息 */
	void saveAccountLevelAndFlush(List<String> levelCodes, Integer accountId, String handicapCode);
	
	/** 存储账号层级信息 */
	void saveAccountLevelAndFlush(List<Integer> levelIds, Integer accountId,Integer newType);

	BizAccountSync findByAccountId(Integer accountId);
}
