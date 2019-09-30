package com.xinbo.fundstransfer.daifucomponent.service;

import com.xinbo.fundstransfer.daifucomponent.dto.input.DaifuSurpportBankTypeInputDTO;
import com.xinbo.fundstransfer.daifucomponent.dto.input.DaifuSynSurpportBankTypeInputDTO;
import com.xinbo.fundstransfer.domain.entity.BizDaifuSurpportBanktypeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Map;

public interface DaifuSurpportBankTypeService {

	void freshBankTypeCache();

	/**
	 * 查询出入款系统支持的所有银行类型
	 * 
	 * @return
	 */
	String queryAllSurpportBankType();

	/**
	 * 查询 判断是否 包含某个银行卡类型
	 * 
	 * @param bankType
	 * @return
	 */
	boolean queryBankTypeIncluded(String bankType);

	/**
	 * 查询平台某供应商代付支持的银行类型
	 * 
	 * @param inputDTO
	 */
	String querSurpportBankType(DaifuSynSurpportBankTypeInputDTO inputDTO);

	/**
	 * 平台同步或者出入款查询 供应商支持的银行类型
	 * 
	 * @param map
	 * @return
	 */
	Map<String, String> saveAllBankType(Map<String, String> map);

	/**
	 * 查询某个供应商支持的银行类型
	 * 
	 * @param inputDTO
	 * @return
	 */
	Page<BizDaifuSurpportBanktypeEntity> list(DaifuSurpportBankTypeInputDTO inputDTO, PageRequest pageRequest);

	/**
	 * 出入款更新某个供应商选中支持某些银行类型
	 * 
	 * @param inputDTO
	 * @return
	 */
	BizDaifuSurpportBanktypeEntity updateSurpport(DaifuSynSurpportBankTypeInputDTO inputDTO);
}
