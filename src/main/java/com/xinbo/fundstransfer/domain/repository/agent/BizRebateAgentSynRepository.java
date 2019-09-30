package com.xinbo.fundstransfer.domain.repository.agent;

import com.xinbo.fundstransfer.domain.entity.agent.BizRebateAgentSyn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface BizRebateAgentSynRepository
		extends JpaRepository<BizRebateAgentSyn, Long>, JpaSpecificationExecutor<BizRebateAgentSyn> {

	/**
	 * 通过uid查找代理信息
	 */
	List<BizRebateAgentSyn> findByUid(String uid);

	/**
	 * 检查请求参数的uid是否存在
	 */
	@Query(nativeQuery = true, value = "SELECT uid FROM biz_account_more WHERE uid in (?1)")
	Set<String> checkUidsExistence(Set<String> uids);

	List<BizRebateAgentSyn> findByIsAgentAndAgentType(boolean isAgent, int agentType);
}