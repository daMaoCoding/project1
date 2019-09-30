package com.xinbo.fundstransfer.service.impl;

import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.repository.SysInvstRepository;
import com.xinbo.fundstransfer.service.SysInvstService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class SysInvstServiceImpl implements SysInvstService {
	@Autowired
	private SysInvstRepository logInvstDao;

	@Override
	public List<BizSysInvst> findByErrorId(Long errorId) {
		return logInvstDao.findByErrorId(errorId);
	}

	@Override
	@Transactional
	public void delete(List<BizSysInvst> dataList) {
		logInvstDao.delete(dataList);
	}

	@Override
	@Transactional
	public BizSysInvst saveAndFlush(Integer accountId, Long bankLogId, BigDecimal amount, BigDecimal balance,
			BigDecimal bankBalance, Integer oppId, Integer oppHandicap, String oppAccount, String oppOwner,
			Long errorId, String batchNo, Long orderId, String orderNo, Integer type, String summary, String remark,
			Integer confirmer, Date createTime, Date occurTime, Long consumeMillis) {
		consumeMillis = Objects.isNull(consumeMillis) ? 0 : consumeMillis;
		BizSysInvst entity = new BizSysInvst();
		entity.setId(null);
		entity.setAccountId(accountId);
		entity.setBankLogId(bankLogId);
		entity.setAmount(amount);
		entity.setBalance(balance);
		entity.setBankBalance(bankBalance);
		entity.setOppId(oppId);
		entity.setOppHandicap(oppHandicap);
		entity.setOppAccount(oppAccount);
		entity.setOppOwner(oppOwner);
		entity.setErrorId(errorId);
		entity.setBatchNo(batchNo);
		entity.setOrderId(orderId);
		entity.setOrderNo(orderNo);
		entity.setType(type);
		entity.setSummary(summary);
		entity.setRemark(remark);
		entity.setConfirmer(confirmer);
		entity.setCreateTime(createTime);
		entity.setOccurTime(occurTime);
		entity.setConsumeMillis(consumeMillis / 1000);
		entity = logInvstDao.saveAndFlush(entity);
		if (StringUtils.isBlank(entity.getBatchNo()) || StringUtils.isBlank(entity.getOrderNo())) {
			if (StringUtils.isBlank(entity.getBatchNo())) {
				entity.setBatchNo(genBatchNo(entity.getId()));
			}
			if (StringUtils.isBlank(entity.getOrderNo())) {
				entity.setOrderNo(genOrderNo(entity.getId()));
			}
			entity = logInvstDao.saveAndFlush(entity);
		}
		return entity;
	}

	public List<BizSysInvst> findByAccountIdAndBankLogId(Integer accountId, Long bankLogId) {
		return logInvstDao.findByAccountIdAndBankLogId(accountId, bankLogId);
	}

	/**
	 * 问题排查：批次号
	 */
	private String genBatchNo(long invstId) {
		String d = String.valueOf(System.currentTimeMillis());
		return String.format("B%s%s%d", d.substring(0, d.length() - String.valueOf(invstId).length()), "S", invstId);
	}

	/**
	 * 该笔交易：订单号
	 */
	private String genOrderNo(long invstId) {
		String d = String.valueOf(System.currentTimeMillis());
		return String.format("S%s%s%d", d.substring(0, d.length() - String.valueOf(invstId).length()), "V", invstId);
	}
}
