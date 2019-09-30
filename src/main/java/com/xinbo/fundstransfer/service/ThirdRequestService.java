package com.xinbo.fundstransfer.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.xinbo.fundstransfer.domain.entity.BizThirdRequest;

public interface ThirdRequestService {
	/**
	 * 通过缓存的 订单号#盘口id 查询 订单
	 * 
	 * @param cacheStr
	 * @return
	 */
	BizThirdRequest findOneByCacheStr(String cacheStr);

	/**
	 * 通过id查询盘口信息
	 * 
	 * @param handicapId
	 * @param orderNo
	 * @return
	 */
	BizThirdRequest findByHandicapAndOrderNo(Integer handicapId, String orderNo);

	/**
	 * 保存
	 * 
	 * @param o
	 * @return
	 */
	BizThirdRequest save(BizThirdRequest o);

	/**
	 * 分页，条件查询
	 * 
	 * @param specification
	 * @param pageable
	 * @return
	 */
	Page<BizThirdRequest> findPage(Specification<BizThirdRequest> specification, Pageable pageable);
}
