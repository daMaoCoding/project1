package com.xinbo.fundstransfer.service.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xinbo.fundstransfer.domain.entity.BizAccountLevel;
import com.xinbo.fundstransfer.domain.entity.BizAccountSync;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.repository.AccountLevelRepository;
import com.xinbo.fundstransfer.domain.repository.AccountSyncRepository;
import com.xinbo.fundstransfer.service.*;

/**
 * 账号管理 *
 *
 * @author 
 */
@Service
public class AccountSyncServiceImpl implements AccountSyncService {
	private static final Logger log = LoggerFactory.getLogger(AccountSyncServiceImpl.class);

	@Autowired
	private AccountSyncRepository accountSyncRepository;
	@Autowired
	private AccountLevelRepository accountLevelRepository;

	@Transactional
	@Override
	public BizAccountSync saveAndFlush(BizAccountSync syncVo) {
		return accountSyncRepository.saveAndFlush(syncVo);
	}

	@Transactional
	@Override
	public void saveAccountLevelAndFlush(List<String> levelCodes, Integer accountId, String handicapCode) {
		if (null != accountId && null != levelCodes && levelCodes.size() > 0) {
			// 先清空Account下的层级
			List<BizAccountLevel> readyDeleteVos = accountLevelRepository.findByAccountId(accountId);
			if (null != readyDeleteVos && readyDeleteVos.size() > 0) {
				accountLevelRepository.delete(readyDeleteVos);
			}
			// 存储新层级关系
			for (String levelCode : levelCodes) {
				accountLevelRepository.insertByLevelCode(accountId, levelCode, handicapCode);
			}
		}
	}
	
	@Transactional
	@Override
	public void saveAccountLevelAndFlush(List<Integer> levelIds, Integer accountId,Integer newType) {
		if (null != accountId && null != levelIds && levelIds.size() > 0) {
			// 先清空Account下的层级
			List<BizAccountLevel> readyDeleteVos = accountLevelRepository.findByAccountId(accountId);
			if (null != readyDeleteVos && readyDeleteVos.size() > 0) {
				accountLevelRepository.delete(readyDeleteVos);
			}
			// 入款卡 存储新层级关系
			if(newType.equals(AccountType.InBank.getTypeId())) {
				for (Integer levelId : levelIds) {
					accountLevelRepository.insertByLevelCode(accountId, levelId);
				}
			}
		}
	}

	@Override
	public BizAccountSync findByAccountId(Integer accountId) {
		return accountSyncRepository.findByAccountId(accountId);
	}

}
