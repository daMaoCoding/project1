/**
 * 
 */
package com.xinbo.fundstransfer.domain.repository;

import java.math.BigDecimal;
import java.util.List;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.xinbo.fundstransfer.daifucomponent.entity.DaifuConfigRequest;

/**
 * @author blake
 *
 */
public interface DaifuConfigRequestRepository
		extends BaseRepository<DaifuConfigRequest, Integer> {

	/**
	 * 第三方代付成功后更新代付通道的出款累计值
	 * @param handicap
	 * @param outMoneyId
	 * @param exactMoney
	 * @return
	 */
	@Modifying
	@Query(nativeQuery = true, value = "update biz_daifu_config "
			+ "set "
			+ " crk_out_money=crk_out_money+?3,"
			+ " crk_out_money_history=crk_out_money_history+?3,"
			+ " crk_out_times=crk_out_times+1,"
			+ " crk_out_times_history=crk_out_times_history+1"
			+ " where handicap=?1 and id=?2")
	int addStaticByDaifuSuccess(Integer handicap, Integer outMoneyId, BigDecimal exactMoney);

	/**
	 * 根据id查询统计值
	 * @param outConfigIdList
	 * @return
	 */
	@Query(nativeQuery = true, value = "select daifu_config_id,"
			+ "SUM(IF(plat_status=1,1,0)) countSuccess,"
			+ "SUM(IF(plat_status=2,1,0)) countError,"
			+ "SUM(IF(plat_status=0 or plat_status=3 ,1,0)) countPaying  "
			+ "from biz_daifu_info "
			+ "where daifu_config_id in (:outConfigIdList)"
			+ "GROUP BY daifu_config_id")
	List<Object> findStaticByIdList(@Param("outConfigIdList") List<Integer> outConfigIdList);

}
