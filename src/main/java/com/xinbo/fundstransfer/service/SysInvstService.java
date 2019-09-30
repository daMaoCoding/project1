package com.xinbo.fundstransfer.service;

import com.xinbo.fundstransfer.domain.entity.BizSysInvst;
import com.xinbo.fundstransfer.domain.pojo.SysBalTrans;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public interface SysInvstService {

	List<BizSysInvst> findByErrorId(Long errorId);

	void delete(List<BizSysInvst> dataList);

	BizSysInvst saveAndFlush(Integer accountId, Long bankLogId, BigDecimal amount, BigDecimal balance,
			BigDecimal bankBalance, Integer oppId, Integer oppHandicap, String oppAccount, String oppOwner,
			Long errorId, String batchNo, Long orderId, String orderNo, Integer type, String summary, String remark,
			Integer confirmer, Date createTime, Date occurTime, Long consumeMillis);

	List<BizSysInvst> findByAccountIdAndBankLogId(Integer accountId, Long bankLogId);
}
