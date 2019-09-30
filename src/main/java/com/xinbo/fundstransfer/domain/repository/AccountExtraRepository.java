package com.xinbo.fundstransfer.domain.repository;

import java.math.BigDecimal;
import java.util.List;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import com.xinbo.fundstransfer.domain.entity.BizAccountExtra;

public interface AccountExtraRepository
		extends BaseRepository<BizAccountExtra, Integer> {
	// 查找是否设置第三方汇率
	@Query(nativeQuery = true, value = "select account_id,rate,type,rate_rule from biz_account_rate where account_id=?1 limit 1")
	String[] findByid(int id);

	// 修改第三方账号费率
	@Modifying
	@Query(nativeQuery = true, value = "update biz_account_rate set type=?2,rate=?3,rate_rule=?4 where account_id=?1")
	void updateAccountRate(int id, int rateType, Float rate, String rateValue);

	// 保存第三方账号费率
	@Modifying
	@Query(nativeQuery = true, value = "insert into biz_account_rate(account_id,rate,type,rate_rule) values (?1,?3,?2,?4)")
	void saveAccountRate(int id, int rateType, Float rate, String rateValue);

	// 更改系统余额
	@Modifying
	@Query(nativeQuery = true, value = "update biz_account set balance=IF(balance is null,0,balance)+?2 where id=?1")
	void updateThirdAccountBalance(int id, BigDecimal balance);

	// 查找当日第三方下发已匹配、未匹配的金额
	@Query(nativeQuery = true, value = "select from_id,ifnull(sum(case when status=0 then amount end),0)mapping,ifnull(sum(case when status=1 then amount end),0)mapped "
			+ "from biz_income_request where from_id in (?1) and create_time between ?2 and ?3 group by from_id ")
	List<Object[]> findIssuedAmounts(List<Object> fromIds, String startTime, String endTime);

}