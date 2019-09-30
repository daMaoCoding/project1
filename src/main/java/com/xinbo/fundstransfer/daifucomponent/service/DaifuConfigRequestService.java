/**
 * 
 */
package com.xinbo.fundstransfer.daifucomponent.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.xinbo.fundstransfer.daifucomponent.dto.output.OutConfigDTO;
import com.xinbo.fundstransfer.daifucomponent.entity.DaifuConfigRequest;

/**
 * 平台代付配置同步接口
 *
 * @author blake
 *
 */
public interface DaifuConfigRequestService {

	/**
	 * 新增或者更新现有数据
	 *
	 * @param req
	 * @return
	 */
	void saveOrUpdate(DaifuConfigRequest req);

	Page<DaifuConfigRequest> findPage(Specification<DaifuConfigRequest> specification, Pageable pageable) throws Exception;


	/**
	 * 获取出款通道第三方余额
	 * @param handicapId 盘口id，必填
	 * @param outConfigId 代付通道id，必填
	 * @return
	 */
	BigDecimal getBalaceFromPayCore(Integer handicapId,Integer outConfigId);

	List<OutConfigDTO> findStaticByIdList(List<Integer> outConfigIdList);

}
