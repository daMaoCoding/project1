package com.xinbo.fundstransfer.newinaccount.service;

import com.xinbo.fundstransfer.domain.ResponseDataNewPay;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizAccountLevel;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.newinaccount.dto.PageDTO;
import com.xinbo.fundstransfer.newinaccount.dto.input.*;
import com.xinbo.fundstransfer.newinaccount.dto.output.CardForPayOutputDTO;
import com.xinbo.fundstransfer.newinaccount.dto.output.FindColOutputDTO;
import com.xinbo.fundstransfer.newinaccount.dto.output.FindPageOutputDTO;
import com.xinbo.fundstransfer.newinaccount.dto.output.StaticstiOutputDTO;

import java.util.List;
import java.util.Map;

/**
 * @author Administrator
 */
public interface InAccountService {
	String checkCardStatus(CheckCardStatusInputDTO inputDTO);

	List<String> findAccountNosByIds(List<Integer> accountIds);

	/** 判断某通道 pocId下的可用卡数量是否小于3 */
	boolean accessibleInAccountLessThan3(Number pocId);

	void deleteUsedRecord(Long pocId, Integer accountId);

	/** 选择最近最少使用的卡返回给平台 */
	String selectAccountToUse(final List<Integer> accountIds, final Long pocId);

	/** 1.5.3 通道管理 – 新增入款通道/修改通道资料 – 查询银行卡列表 */
	List<FindColOutputDTO> findCol(FindColInputDTO inputDTO, BizHandicap handicap);

	/** 1.5.4 银行卡管理查询分页列表 */
	PageDTO<FindPageOutputDTO> findPage(FindPageInputDTO inputDTO, BizHandicap handicap);

	/** 1.5.5 银行卡管理统计 */
	StaticstiOutputDTO statisctic(StaticsticInputDTO inputDTO, BizHandicap handicap);

	/** 1.5.6 银行卡管理取消绑定(单个银行卡) */
	String modifyBind(ModifyBindInputDTO inputDTO, BizHandicap handicap);

	/** 1.5.7 通道管理--删除通道/更新(取消,新增)银行卡绑定/绑定层级 */
	String updateBind(UpdateBindInputDTO inputDTO, BizHandicap handicap);

	/** 1.5.8 会员支付通道上报选择银行卡 */
	CardForPayOutputDTO cardForPay(CardForPayInputDTO inputDTO, BizHandicap handicap);

	/** 1.5.8 会员支付通道上报选择银行卡 */
	CardForPayOutputDTO cardForPay1(CardForPayInputDTO inputDTO, BizHandicap handicap);

	/** 1.5.11 统计通道可用卡数和不可用卡数 */
	List<CardsStatisticOutputDTO> userOrNon(Map<Integer, List<Long>> map);

	/** 2.0.1 查询通道告警信息(需求 4807) */
	PageDTO<FindPageOutputDTO> findAlarmPage(FindAlarmInputDTO inputDTO, BizHandicap handicap);

	ResponseDataNewPay cardAvailAlarm(CardAvailAlarmInputDTO inputDTO);

	ResponseDataNewPay bankModified(BankModifiedInputDTO inputDTO);

	List<BizAccount> findBySubTypeAndPassageId(Long passageId);

	List<BizAccount> findAccountsByIdsForUpdate(List<Integer> accounts);

	List<BizAccount> findByAccounts(List<String> accounts);

	List<BizAccount> findByAccountsForPay(CardForPayInputDTO inputDTO, Integer handicapId);

	List<BizAccount> saveBindBatch(List<BizAccount> accounts);

	List<BizAccountLevel> findAccountLevelsByAccountIds(List<Integer> accountIds);

	BizHandicap getHandicap(Integer handicapCode);

	void noticeFreshCache(Number passageId);

	void freshCache(Number passageId);

	void increaseInAccountExceed(Integer accId);

	/** 查询可用于入款的银行类型列表 */
	List<Map<String,Object>> bankList();

	/** 2.0.4 根据银行类型和层级返回入款卡 */
	CardForPayOutputDTO cardForPayByLevelCode(CardForPayInputDTO inputDTO, BizHandicap handicap);

	void increaseInAccountExceedByLevel(Integer accId);
}
