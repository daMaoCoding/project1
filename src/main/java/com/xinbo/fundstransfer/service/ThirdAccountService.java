package com.xinbo.fundstransfer.service;

import com.xinbo.fundstransfer.domain.pojo.BizThirdAccountInputDTO;
import com.xinbo.fundstransfer.domain.pojo.BizThirdAccountOutputDTO;
import com.xinbo.fundstransfer.domain.pojo.ThirdAccountInputDTO;
import com.xinbo.fundstransfer.domain.pojo.UnBindThirdAccountOutputDTO;
import org.springframework.data.domain.PageRequest;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

public interface ThirdAccountService {
	List<UnBindThirdAccountOutputDTO> unbindThirdAccount(ThirdAccountInputDTO inputDTO);

	List<Integer> findBindedAccountIds();

	BizThirdAccountOutputDTO findByAccountId(Integer accountId);

	Map<String, Object> findByIdAndUnbind(ThirdAccountInputDTO inputDTO);

	BizThirdAccountOutputDTO findById(ThirdAccountInputDTO inputDTO);

	/**
	 * 新增 修改
	 * 
	 * @param inputDTO
	 * @return
	 */
	BizThirdAccountOutputDTO edit(BizThirdAccountInputDTO inputDTO)
			throws InvocationTargetException, IllegalAccessException;

	/**
	 * 查询列表
	 * 
	 * @param inputDTO
	 * @return
	 */
	List<BizThirdAccountOutputDTO> list(BizThirdAccountInputDTO inputDTO);

	Map<String, Object> page(ThirdAccountInputDTO inputDTO, PageRequest pageRequest);

	Map<String, Object> pageBySql(ThirdAccountInputDTO inputDTO, PageRequest pageRequest);

}
