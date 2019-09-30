package com.xinbo.fundstransfer.service;

import com.xinbo.fundstransfer.domain.pojo.BizCommonRemarkInputDTO;
import com.xinbo.fundstransfer.domain.pojo.BizCommonRemarkOutputDTO;
import org.springframework.data.domain.PageRequest;

import java.util.Map;

public interface CommonRemarkService {
	/**
	 * 新增备注
	 * 
	 * @param inputDTO
	 * @return
	 */
	BizCommonRemarkOutputDTO add(BizCommonRemarkInputDTO inputDTO);

	/**
	 * 删除备注 暂时逻辑删除 status=2
	 * 
	 * @param inputDTO
	 * @return
	 */
	BizCommonRemarkOutputDTO delete(BizCommonRemarkInputDTO inputDTO);

	/**
	 * 
	 * 列表 查询 或者根据某个三方账户id查询
	 * 
	 * @param inputDTO
	 * @return
	 */
	Map<String, Object> list(BizCommonRemarkInputDTO inputDTO, PageRequest pageRequest);

	/**
	 * 根据业务id获取最新的一条备注
	 * 
	 * @param businessId
	 * @return
	 */
	String latestRemark(Integer businessId, String type);
}
