package com.xinbo.fundstransfer.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.pojo.TransferEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import com.xinbo.fundstransfer.domain.entity.BizAccountMore;
import com.xinbo.fundstransfer.domain.entity.BizAccountRebate;

public interface AccountRebateService {
	Page<BizAccountRebate> findPage(Specification<BizAccountRebate> specification, Pageable pageable);

	BizAccountRebate saveAndFlush(BizAccountRebate rebate);

	void create(BizAccountMore more, BizAccountRebate rebate);

	List<BizAccountRebate> findAll(Specification<BizAccountRebate> specification);

	Map<String, Object> findRebate(int status, String type, String orderNo, String fromAmount, String toMoney,
			String fristTime, String lastTime, int handicap, List<Integer> handicapList, String uName,
			String rebateType, PageRequest pageRequest) throws Exception;

	Map<String, Object> findAuditCommission(String fristTime, String lastTime, String results, PageRequest pageRequest)
			throws Exception;

	Map<String, Object> findDetail(String rebateUser, String bankType, String caclTime, BigDecimal startAmount,
			BigDecimal endAmount, PageRequest pageRequest) throws Exception;

	Map<String, Object> findComplete(String fristTime, String lastTime, PageRequest pageRequest) throws Exception;

	Map<String, Object> findCompleteDetail(String rebateUser, String caclTime, PageRequest pageRequest)
			throws Exception;

	BizAccountRebate findById(Long id);

	BizAccountRebate findLatestByTid(String orderNo);

	BizAccountRebate findRebateByBankLog(int fromAccountId, Float amount, String toAccount, int type, int cardType,
			int orderType);

	void updateStatusById(Long id, Integer status);

	void reAssignDrawing(SysUser operator, Long id, String remark);

	void unknownByRobot(TransferEntity entity);

	String lock(String caclTime, SysUser user);

	void unlock(String caclTime, SysUser user);

	void saveAudit(String caclTime, int status, String remark, SysUser user, String rebateUser) throws Exception;

	void recalculate(String caclTime, String remark, SysUser user) throws Exception;;

	Map<String, Object> findDerating(String orderNo, String fromAmount, String toMoney, int handicap,
			List<Integer> handicapList, String uname, String status, PageRequest pageRequest) throws Exception;

	String saveDeratingAudit(Long id, int status, String remark, SysUser user) throws Exception;
}
