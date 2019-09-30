package com.xinbo.fundstransfer.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.xinbo.fundstransfer.SqlConstants;
import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import com.xinbo.fundstransfer.domain.entity.BizRebateStatistics;

public interface RebateStatisticsRepository extends BaseRepository<BizRebateStatistics, Long> {
	@Query(nativeQuery = true, value = "SELECT" + "	brs.id," + " brs.statistics_date," + "	brs.new_acc_total,"
			+ "	brs.quit_acc_total," + " brs.acc_total," + " brs.new_acc_new_card," + "	brs.now_acc_new_card,"
			+ "	brs.quit_card_total," + " brs.card_total," + " brs.enable_card_total," + " brs.disable_card_total,"
			+ "	brs.freeze_card_total," + "	brs.new_acc_upgrade_credits," + " brs.now_acc_upgrade_credits,"
			+ "	brs.reduce_credits," + " brs.credits_total " + "FROM" + "	biz_rebate_statistics brs " + "WHERE"
			+ "	brs.statistics_date >= ?1  AND brs.statistics_date <= ?2 "
			+ " order by brs.statistics_date desc ", countQuery = SqlConstants.SEARCH_SHOWREBATE_STATISTICS)
	Page<Object> showRebateStatistics(String startDate, String endDate, Pageable pageable);
	
	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "insert into biz_rebate_statistics (statistics_date, new_acc_total, " +
			" acc_total, new_acc_new_card, now_acc_new_card, card_total, enable_card_total, disable_card_total, "+
			" freeze_card_total, new_acc_upgrade_credits, now_acc_upgrade_credits, credits_total)" + 
			" select date_format(?1,'%Y-%m-%d') as statisticsDate, count(distinct t5.addUser) newAccTotal, count(distinct t5.uid) accTotal, "+
			" count(distinct t5.addUseraddcard) addUseraddcard, count(distinct t5.oldUserAddCard) oldUserAddCard, count(distinct t5.account) cardTotal, "+
			" count(distinct(normal)) normal, count(distinct(stopTemp)) stopTemp, count(distinct(freeze)) freeze, sum(IFNULL(newAccAmount,0)) newAccAmount, "+
			" sum(IFNULL(oldAccAmount,0)) oldAccAmount, sum(peakBalance) peakBalance from ( select t1.uid," + 
			" CASE WHEN t1.create_time >= ?1 AND t1.create_time <= ?2 THEN t1.uid else null END AS addUser," + 
			" CASE WHEN t1.create_time >= ?1 AND t1.create_time <= ?2 AND t31.create_time >= ?1" + 
			" AND t31.create_time <= ?2 THEN t31.account ELSE null END AS addUseraddcard," + 
			" CASE WHEN t1.create_time < ?1 AND t31.create_time >= ?1  AND t31.create_time <= ?2 " + 
			" THEN t31.account ELSE null  END AS oldUserAddCard, t31.account,t31.status," + 
			" CASE WHEN t1.create_time >= ?1 AND t1.create_time <= ?2 THEN amount ELSE 0 END AS newAccAmount," + 
			" CASE WHEN t1.create_time < ?1 THEN amount ELSE 0 END AS oldAccAmount," + 
			" CASE WHEN t31.STATUS IN ( 1, 5 ) THEN t31.account ELSE null END AS normal," + 
			" CASE WHEN t31.STATUS = 3 THEN t31.account ELSE null END AS freeze," + 
			" CASE WHEN t31.STATUS = 4 THEN t31.account ELSE null END AS stopTemp," + 
			" t31.peak_balance as peakBalance from biz_rebate_user t1 left join biz_account_more t2 on t1.uid = t2.uid" + 
			" left join (select * from biz_account t3 where t3.type in (1,5,8,13) and t3.flag = 2) t31 on t2.moible = t31.mobile" + 
			" left join (select t4.to_account,sum(amount) amount from biz_income_request t4 where t4.type = '401' and t4.status = '5'" + 
			" and t4.update_time >= ?1 AND t4.update_time <= ?2" + 
			" group by t4.to_account) t41 on t31.account = t41.to_account) t5")
	void insertRebateStatistics(String startDate, String endDate);
	
	@Query(nativeQuery = true, value = "select count(1) from biz_rebate_statistics where statistics_date = ?1")
	int findRebateStatisticsByDate(String startDate);
}
