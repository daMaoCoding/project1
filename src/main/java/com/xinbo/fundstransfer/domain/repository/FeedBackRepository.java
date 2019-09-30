package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.xinbo.fundstransfer.SqlConstants;
import com.xinbo.fundstransfer.domain.entity.AccountStatistics;

public interface FeedBackRepository extends BaseRepository<AccountStatistics, Long> {

	// 查询意见反馈
	@Query(nativeQuery = true, value = "select id,date_format(create_time,'%Y-%m-%d %H:%i:%s') create_time,date_format(update_time,'%Y-%m-%d %H:%i:%s') update_time,level,status,issue,creator,acceptor,imgs"
			+ " from biz_feedback where status in (?4) and (?1 is null or issue like concat('%',?1,'%')) and (?2 is null or (?6='untreatedFind' or update_time between ?2 and ?3))"
			+ " and (?2 is null or (?6='processedFind' or create_time between ?2 and ?3)) and (?5=0 or level=?5) "
			, countQuery = SqlConstants.SEARCH_FEEDBACK_COUNTQUERY)
	Page<Object> findFeedBack(String untreatedFind, String fristTime, String lastTime,List<Integer> status,String level,String type,Pageable pageable);
	
	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "insert into biz_feedback(create_time,level,status,issue,creator,imgs) values(?1,?2,1,?3,?4,?5)")
	void saveFeedBack(String nowTime,String level, String describe, String userId, String imgs);
	
	// 查询意见反馈
	@Query(nativeQuery = true, value = "select issue from biz_feedback where id=?1")
	String findOldRemark(String id);
	
	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_feedback set issue=?2 where id=?1")
	void saveRemark(String id,String remark);
	
	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_feedback set issue=?3,acceptor=?2,status=2 where id=?1")
	void dealWith(String id,String userId,String remark);
	
	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_feedback set status=3,update_time=now() where id=?1")
	void finish(String id);
	
	@Query(nativeQuery = true, value = "select id,date_format(create_time,'%Y-%m-%d %H:%i:%s') create_time,date_format(update_time,'%Y-%m-%d %H:%i:%s') update_time,level,status,issue,creator,acceptor,imgs"
			+ " from biz_feedback where id=?1")
	List<Object> showFeedBackDetails(String id);
	
	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "delete from biz_feedback where id=?1")
	void deleteFeedBack(String id);
	
}