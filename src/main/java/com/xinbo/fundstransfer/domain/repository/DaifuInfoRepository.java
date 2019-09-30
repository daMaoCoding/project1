/**
 * 
 */
package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.xinbo.fundstransfer.daifucomponent.entity.DaifuInfo;

/**
 * @author blake
 *
 */
public interface DaifuInfoRepository
		extends BaseRepository<DaifuInfo, Integer> {

	/**
	 * 记录每一次支付平台通知订单的支付结果参数
	 * @param id 出款单id
	 * @param paramStr 参数的 json 字符串
	 * @return
	 */
	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_daifu_info set plat_req_param= case when plat_req_param is null then JSON_ARRAY(cast (?3 as json)) else JSON_ARRAY_APPEND(plat_req_param,'$',cast (?3 as json)) end where handicap=?1 and id=?2")
	int apendPayResponse(Integer handicap,Integer id, String paramStr);

	/**
	 * 设置代付订单状态为 2-取消<br> 
	 * 仅当 代付订单状态为 0-未知或者3-支付中时可以设置为取消
	 * @param handicap 盘口id
	 * @param id 代付订单id
	 * @param error 错误消息
	 * @return
	 */
	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_daifu_info set plat_status =2,error_msg =?3,uptime= CURRENT_TIMESTAMP where handicap=?1 and id =?2 and (plat_status = 0 or plat_status = 3) ")
	int setPlatStatusCancel(Integer handicap, Integer id, String error);
	
	/**
	 * 设置代付订单状态为 3-正在支付<br> 
	 * 仅当 代付订单状态为 0-未知或者3-支付中时可以设置为正在支付
	 * @param handicap 盘口id
	 * @param id 代付订单id
	 * @return
	 */
	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_daifu_info set plat_status =3,uptime= CURRENT_TIMESTAMP where handicap=?1 and id =?2 and (plat_status = 0 or plat_status = 3) ")
	int setPlatStatusPaying(Integer handicap, Integer id);

	/**
	 * 设置代付订单状态为 1-完成<br> 
	 * 仅当 代付订单状态为 0-未知或者3-支付中时可以设置为完成
	 * @param handicap 盘口id
	 * @param id 代付订单id
	 * @return
	 */
	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "update biz_daifu_info set plat_status =1,uptime= CURRENT_TIMESTAMP where handicap=?1 and id =?2 and (plat_status = 0 or plat_status = 3) ")
	int setPlatStatusDone(Integer handicap, Integer id);

}
