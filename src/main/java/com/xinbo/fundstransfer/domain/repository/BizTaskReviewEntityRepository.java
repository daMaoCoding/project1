package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.xinbo.fundstransfer.domain.entity.BizTaskReviewEntity;

import java.util.List;

/**
 * Created by Administrator on 2018/7/3.
 */

public interface BizTaskReviewEntityRepository
		extends BaseRepository<BizTaskReviewEntity, Integer> {

	@Modifying
	@Query(nativeQuery = true, value = "update biz_task_review set  finish_time =now(),remark=?2 where asign_time is not null and finish_time is null and taskid=?1 and operator=?3")
	int updateByTaskId(int taskId, String remark, int userId);

	@Modifying
	@Query(nativeQuery = true, value = "update biz_task_review set   remark=?3 where asign_time is not null and finish_time is null and operator=?1 and taskid=?2 ")
	int updateByRemark(int operator, int taskId, String remark);

	@Modifying
	@Query(nativeQuery = true, value = "DELETE  from biz_task_review  where  operator=?1  and  taskid=?2 and asign_time is not null and finish_time is null ")
	int updateDuplicate(int operator, int taskId);

	@Query(nativeQuery = true, value = "select id,IFNULL(operator,\"NULL\"),taskid from biz_task_review where taskid=?1 and finish_time is null limit 1")
	Object findReviewTaskByTaskId(int taskId);

	@Query(nativeQuery = true, value = "select * from biz_task_review where taskid=?1 and finish_time is null")
	List<BizTaskReviewEntity> getByTaskidAndAndFinishTimeIsNull(int taskId);
}