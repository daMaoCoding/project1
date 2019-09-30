package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import com.xinbo.fundstransfer.domain.entity.BizAccountReturnSummary;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Set;

public interface AccountReturnSummaryRepository extends BaseRepository<BizAccountReturnSummary, Integer> {
	@Query(value = "select account from BizAccountReturnSummary d where d.calcTime=?1")
	List<Integer> findReturnSummaryByCalcTime(String calcTime);

	@Query(value = "select account from BizAccountReturnSummary d where d.calcTime=?1 and d.amount>0 and d.account in (?2)")
	List<Integer> findReturnSummaryByCalcTime(String calcTime, List<Integer> accounts);

	@Query(value = "select account from BizAccountReturnSummary d where d.calcTime=?1 and d.amount=50 and d.account in (?2)")
	List<Integer> findReturnSummaryByCalcTime5(String calcTime, List<Integer> accounts);

	List<BizAccountReturnSummary> findByCalcTimeAndAccount(String calcTime, Integer account);

	List<BizAccountReturnSummary> findByCalcTime(String calcTime);

	List<BizAccountReturnSummary> findByCalcTimeAndUidIn(String calcTime, Set<String> uidList);

	List<BizAccountReturnSummary> findByCalcTimeAndUid(String calcTime, String uid);
}
