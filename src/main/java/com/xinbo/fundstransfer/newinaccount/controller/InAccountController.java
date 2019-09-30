package com.xinbo.fundstransfer.newinaccount.controller;

import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.enums.InBankSubType;
import com.xinbo.fundstransfer.newinaccount.dto.PageDTO;
import com.xinbo.fundstransfer.newinaccount.dto.input.*;
import com.xinbo.fundstransfer.newinaccount.dto.output.CardForPayOutputDTO;
import com.xinbo.fundstransfer.newinaccount.dto.output.CardForPayOutputDTO2;
import com.xinbo.fundstransfer.newinaccount.dto.output.FindColOutputDTO;
import com.xinbo.fundstransfer.newinaccount.dto.output.FindPageOutputDTO;
import com.xinbo.fundstransfer.newinaccount.dto.output.StaticstiOutputDTO;
import com.xinbo.fundstransfer.newinaccount.service.InAccountMixService;
import com.xinbo.fundstransfer.newinaccount.service.InAccountService;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import com.xinbo.fundstransfer.restful.v2.TokenValidation;
import com.xinbo.fundstransfer.service.AccountChangeService;
import com.xinbo.fundstransfer.unionpay.ysf.service.YSFService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Administrator
 */
@RestController
@RequestMapping("/bank")
@Slf4j
public class InAccountController extends TokenValidation {
	@Autowired
	private InAccountService service;
	@Autowired
	private YSFService ysfService;
	@Autowired
	private InAccountMixService minService;
	@Autowired
	private AccountChangeService accountChangeService;

	/**
	 * 1.5.3 通道管理 – 新增入款通道/修改通道资料 – 查询银行卡列表
	 */
	@PostMapping("/findCol")
	public GeneralResponseData findCol(@Validated @RequestBody FindColInputDTO inputDTO) {
		log.info(" 接口参数:{}", inputDTO.toString());
		BizHandicap handicap = service.getHandicap(inputDTO.getOid().intValue());
		if (ObjectUtils.isEmpty(handicap)) {
			return new GeneralResponseData(-1, "盘口不存在!");
		}
		GeneralResponseData res = new GeneralResponseData(1, "操作成功");
		List<FindColOutputDTO> data = null;
		try {
			data = service.findCol(inputDTO, handicap);
		} catch (Exception e) {
			e.printStackTrace();
			res = new GeneralResponseData(-1, "操作失败!" + e.getMessage());
		}
		res.setData(data);
		return res;
	}

	/**
	 * 1.5.4 银行卡管理查询分页列表
	 *
	 * @param inputDTO
	 * @return
	 */
	@PostMapping("/findPage")
	public GeneralResponseData findPage(@Validated @RequestBody FindPageInputDTO inputDTO) {
		log.info(" 接口参数:{}", inputDTO.toString());
		BizHandicap handicap = service.getHandicap(inputDTO.getOid());
		if (ObjectUtils.isEmpty(handicap)) {
			return new GeneralResponseData(-1, "盘口不存在!");
		}
		GeneralResponseData res = new GeneralResponseData(1, "操作成功");
		PageDTO<FindPageOutputDTO> data = null;
		try {
			data = service.findPage(inputDTO, handicap);
		} catch (Exception e) {
			e.printStackTrace();
			res = new GeneralResponseData(-1, "操作失败!" + e.getMessage());
		}
		res.setData(data);
		return res;
	}

	/**
	 * 1.5.5 银行卡管理统计
	 *
	 * @param inputDTO
	 * @return
	 */
	@PostMapping("/statistics")
	public GeneralResponseData statistics(@Validated @RequestBody StaticsticInputDTO inputDTO) {
		log.info(" 接口参数:{}", inputDTO.toString());
		BizHandicap handicap = service.getHandicap(inputDTO.getOid());
		if (ObjectUtils.isEmpty(handicap)) {
			return new GeneralResponseData(-1, "盘口不存在!");
		}
		GeneralResponseData res = new GeneralResponseData(1, "操作成功");
		StaticstiOutputDTO data = null;
		try {
			data = service.statisctic(inputDTO, handicap);
		} catch (Exception e) {
			e.printStackTrace();
			res = new GeneralResponseData(-1, "操作失败!" + e.getMessage());
		}
		res.setData(data);
		return res;
	}

	/**
	 * 1.5.6 银行卡管理取消绑定 绑定
	 *
	 * @param inputDTO
	 * @return
	 */
	@PostMapping("/modifyBind")
	public GeneralResponseData modifyBind(@Validated @RequestBody ModifyBindInputDTO inputDTO) {
		log.info(" 接口参数:{}", inputDTO.toString());
		if (inputDTO.getCancelStatus().equals(0) && ObjectUtils.isEmpty(inputDTO.getPocId())) {
			return new GeneralResponseData(-1, String.format("参数:%s校验不通过!", ObjectMapperUtils.serialize(inputDTO)));
		}
		BizHandicap handicap = service.getHandicap(inputDTO.getOid());
		if (ObjectUtils.isEmpty(handicap)) {
			return new GeneralResponseData(-1, "盘口不存在!");
		}
		GeneralResponseData res = new GeneralResponseData(1, "取消绑定成功");
		try {
			String ret = service.modifyBind(inputDTO, handicap);
			final String expectRet = "OK";
			if (ObjectUtils.isEmpty(ret) || !expectRet.equals(ret)) {
				res = new GeneralResponseData(-1, "取消绑定失败!" + ret);
			}
		} catch (Exception e) {
			e.printStackTrace();
			res = new GeneralResponseData(-1, "取消绑定失败!" + e.getMessage());
		}
		res.setData(GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		return res;
	}

	/**
	 * 1.5.7 通道管理--删除通道/更新(取消,新增)银行卡绑定/绑定层级
	 *
	 * @param inputDTO
	 * @return
	 */
	@PostMapping("/updateBind")
	public GeneralResponseData updateBind(@Validated @RequestBody UpdateBindInputDTO inputDTO) {
		log.info(" info 接口参数:{}", inputDTO.toString());
		BizHandicap handicap = service.getHandicap(inputDTO.getOid());
		if (ObjectUtils.isEmpty(handicap)) {
			return new GeneralResponseData(-1, "盘口不存在!");
		}
		final String expectRet = "OK";
		try {
			String ret = service.updateBind(inputDTO, handicap);
			if (expectRet.equals(ret)) {
				log.debug("保存时间:{}", System.currentTimeMillis());
				service.noticeFreshCache(inputDTO.getPocId());
				return new GeneralResponseData(1, "更新成功!");
			}
			return new GeneralResponseData(-1, "更新失败!" + ret);
		} catch (Exception e) {
			e.printStackTrace();
			return new GeneralResponseData(-1, "更新失败!" + e.getMessage());
		}
	}

	/**
	 * 6437 2.0.4 入款请求获取银行卡信息
	 *
	 * @param map
	 * @return
	 */
	@PostMapping("/cardForPayByBankType")
	public GeneralResponseData cardForPayByBankType(@RequestBody Map map) {
		log.info("6437 param :{}", map);
		if (ObjectUtils.isEmpty(map)) {
			return new GeneralResponseData(-1, "参数必传!");
		}
		final String amount = "amount", oid = "oid", type = "type", userName = "userName",
				// 需求 6437
				levelCode = "levelCode", ocId = "ocId", bankTypeId = "bankTypeId";
		if (ObjectUtils.isEmpty(map.get(amount))) {
			return new GeneralResponseData(-1, "参数amount必传!");
		}
		if (ObjectUtils.isEmpty(map.get(oid))) {
			return new GeneralResponseData(-1, "参数oid必传!");
		}
		if (ObjectUtils.isEmpty(map.get(type))) {
			return new GeneralResponseData(-1, "参数type必传!");
		}
		if (ObjectUtils.isEmpty(map.get(userName))) {
			return new GeneralResponseData(-1, "参数userName必传!");
		}
		if (ObjectUtils.isEmpty(map.get(levelCode))) {
			return new GeneralResponseData(-1, "参数levelCode必传!");
		}
		if (ObjectUtils.isEmpty(map.get(ocId))) {
			return new GeneralResponseData(-1, "参数ocId必传!");
		}
		// 需求 5109 云闪付单次入款金额不能超过 4999 元
		if (InBankSubType.IN_BANK_YSF.getSubType().equals(Integer.parseInt(map.get(type).toString()))
				|| InBankSubType.IN_BANK_YSF_MIX.getSubType().equals(Integer.parseInt(map.get(type).toString()))) {
			if (Double.valueOf(map.get(amount).toString()).compareTo(4999D) > 0) {
				return new GeneralResponseData(-1, "云闪付单次最大入款金额不能超过4999!");
			}
		}

		CardForPayInputDTO inputDTO = new CardForPayInputDTO();
		inputDTO.setAmount(Double.valueOf(map.get(amount).toString()));
		inputDTO.setOid(Integer.valueOf(map.get(oid).toString()));
		inputDTO.setType(Byte.valueOf(map.get(type).toString()));
		inputDTO.setUserName(map.get(userName).toString());
		// 需求 6437
		inputDTO.setLevelCode(map.get(levelCode).toString());
		inputDTO.setOcId(Long.parseLong(map.get(ocId).toString()));
		if (!ObjectUtils.isEmpty(map.get(bankTypeId))) {
			inputDTO.setBankTypeId(Long.parseLong(map.get(bankTypeId).toString()));
		}

		BizHandicap handicap = service.getHandicap(inputDTO.getOid());
		if (ObjectUtils.isEmpty(handicap)) {
			return new GeneralResponseData(-1, "盘口不存在!");
		}
		CardForPayOutputDTO outputDTO;
		GeneralResponseData res;
		try {
			if (InBankSubType.IN_BANK_YSF.getSubType().equals(inputDTO.getType().intValue())) {
				outputDTO = ysfService.cardForPayYsfByLevelCode(inputDTO, handicap);
				if (!ObjectUtils.isEmpty(outputDTO)) {
					res = new GeneralResponseData(1, "获取成功!");
				} else {
					res = new GeneralResponseData(0, "获取失败!");
				}
				res.setData(outputDTO);
			} else {
				outputDTO = service.cardForPayByLevelCode(inputDTO, handicap);
				if (!ObjectUtils.isEmpty(outputDTO)) {
					res = new GeneralResponseData(1, "获取成功!");
				} else {
					res = new GeneralResponseData(0, "获取失败!");
				}
				res.setData(outputDTO);
			}
			if (res.getData() != null) {
				CardForPayOutputDTO outDTO = (CardForPayOutputDTO) res.getData();
				List<CardForPayOutputDTO.BankInfo> bankInfoList = outDTO.getAccountList();
				if (!CollectionUtils.isEmpty(bankInfoList)) {
					List<String> accounts = bankInfoList.stream().map(p -> p.getCardNo().toString())
							.collect(Collectors.toList());
					// 分配成功，占用卡的信用额度
					accountChangeService.addToOccuCredits(inputDTO.getOid(), accounts, outDTO.getFinalAmount());
				}
			}
			// 需求 6437 .传递进来的ocId 原来返回
			if (!ObjectUtils.isEmpty(outputDTO) && !ObjectUtils.isEmpty(inputDTO.getOcId())) {
				outputDTO.setPocId(inputDTO.getOcId());
			}
			if (res.getData() != null) {
				CardForPayOutputDTO2 data = new CardForPayOutputDTO2();
				CardForPayOutputDTO outDTO = (CardForPayOutputDTO) res.getData();
				BeanUtils.copyProperties(outputDTO, data);
				List<CardForPayOutputDTO.BankInfo> bankInfoList = outDTO.getAccountList();
				if (!CollectionUtils.isEmpty(bankInfoList)) {
					data.setAccount(bankInfoList.get(0));
				}
				res.setData(data);
			}
		} catch (Exception e) {
			log.error("bank/cardForPayByBankType产生异常", e);
			res = new GeneralResponseData(-1, "获取失败!" + e.getMessage());
		}
		return res;
	}

	/**
	 * 1.5.8 会员支付通道上报选择银行卡
	 *
	 * @param map
	 * @return
	 */
	@PostMapping("/cardForPay")
	public GeneralResponseData cardForPay(@RequestBody Map map) {
		log.info("param :{}", map);
		if (ObjectUtils.isEmpty(map)) {
			return new GeneralResponseData(-1, "参数必传!");
		}
		final String amount = "amount", pocIdMapCardNosCol = "pocIdMapCardNosCol", oid = "oid", type = "type",
				userName = "userName", pocIdTypeCol = "pocIdTypeCol";
		if (ObjectUtils.isEmpty(map.get(amount))) {
			return new GeneralResponseData(-1, "参数amount必传!");
		}
		if (ObjectUtils.isEmpty(map.get(pocIdMapCardNosCol))) {
			return new GeneralResponseData(-1, "参数pocIdMapCardNosCol必传!");
		}
		if (ObjectUtils.isEmpty(map.get(oid))) {
			return new GeneralResponseData(-1, "参数oid必传!");
		}
		if (ObjectUtils.isEmpty(map.get(type))) {
			return new GeneralResponseData(-1, "参数type必传!");
		}
		if ((InBankSubType.IN_BANK_YSF.getSubType().equals(Integer.parseInt(map.get(type).toString()))
				|| InBankSubType.IN_BANK_YSF_MIX.getSubType().equals(Integer.parseInt(map.get(type).toString())))
				&& ObjectUtils.isEmpty(map.get(userName))) {
			return new GeneralResponseData(-1, "云闪付支付参数userName必传!");
		}
		// 需求 5109 云闪付单次入款金额不能超过 4999 元
		if (InBankSubType.IN_BANK_YSF.getSubType().equals(Integer.parseInt(map.get(type).toString()))
				|| InBankSubType.IN_BANK_YSF_MIX.getSubType().equals(Integer.parseInt(map.get(type).toString()))) {
			if (Double.valueOf(map.get(amount).toString()).compareTo(4999D) > 0) {
				return new GeneralResponseData(-1, "云闪付单次最大入款金额不能超过4999!");
			}
		}

		CardForPayInputDTO inputDTO = new CardForPayInputDTO();
		inputDTO.setAmount(Double.valueOf(map.get(amount).toString()));
		inputDTO.setPocIdMapCardNosCol((Map<String, String>) map.get(pocIdMapCardNosCol));
		inputDTO.setPocIdTypeCol((Map<String, List<Long>>) map.get(pocIdTypeCol));
		inputDTO.setOid(Integer.valueOf(map.get(oid).toString()));
		inputDTO.setType(Byte.valueOf(map.get(type).toString()));
		inputDTO.setUserName(map.get(userName).toString());

		BizHandicap handicap = service.getHandicap(inputDTO.getOid());
		if (ObjectUtils.isEmpty(handicap)) {
			return new GeneralResponseData(-1, "盘口不存在!");
		}
		CardForPayOutputDTO outputDTO;
		GeneralResponseData res;
		try {
			if (InBankSubType.IN_BANK_YSF_MIX.getSubType().equals(inputDTO.getType().intValue())) {
				outputDTO = minService.cardForPayMix(inputDTO, handicap);
				if (!ObjectUtils.isEmpty(outputDTO)) {
					res = new GeneralResponseData(1, "获取成功!");
				} else {
					res = new GeneralResponseData(0, "获取失败!");
				}
				res.setData(outputDTO);
			} else if (InBankSubType.IN_BANK_YSF.getSubType().equals(inputDTO.getType().intValue())) {
				outputDTO = ysfService.cardForPayYSF(inputDTO, handicap);
				if (!ObjectUtils.isEmpty(outputDTO)) {
					res = new GeneralResponseData(1, "获取成功!");
				} else {
					res = new GeneralResponseData(0, "获取失败!");
				}
				res.setData(outputDTO);
			} else {
				// outputDTO = service.cardForPay(inputDTO, handicap);
				outputDTO = service.cardForPay1(inputDTO, handicap);
				if (!ObjectUtils.isEmpty(outputDTO)) {
					res = new GeneralResponseData(1, "获取成功!");
				} else {
					res = new GeneralResponseData(0, "获取失败!");
				}
				res.setData(outputDTO);
			}
			if (res.getData() != null) {
				CardForPayOutputDTO outDTO = (CardForPayOutputDTO) res.getData();
				List<CardForPayOutputDTO.BankInfo> bankInfoList = outDTO.getAccountList();
				if (!CollectionUtils.isEmpty(bankInfoList)) {
					List<String> accounts = bankInfoList.stream().map(p -> p.getCardNo().toString())
							.collect(Collectors.toList());
					// 分配成功，占用卡的信用额度
					accountChangeService.addToOccuCredits((Integer) map.get(oid), accounts, outDTO.getFinalAmount());
				}
			}
		} catch (Exception e) {
			log.error("bank/carForPay产生异常", e);
			res = new GeneralResponseData(-1, "获取失败!" + e.getMessage());
		}
		return res;
	}

	/**
	 * 1.5.11 统计通道可用卡数和不可用卡数
	 *
	 * @param map
	 * @return
	 */
	@PostMapping("/usedOrNon")
	public GeneralResponseData userOrNon(@RequestBody Map<Integer, List<Long>> map) {
		log.info("查询参数 :{}", ObjectMapperUtils.serialize(map));
		if (ObjectUtils.isEmpty(map)) {
			return new GeneralResponseData(-1, "参数必传!");
		}
		GeneralResponseData ret = new GeneralResponseData(1, "获取成功!");
		List<CardsStatisticOutputDTO> res = null;
		try {
			res = service.userOrNon(map);
		} catch (Exception e) {
			e.printStackTrace();
			ret = new GeneralResponseData(-1, "获取失败!" + e.getMessage());
		}
		ret.setData(res);
		return ret;
	}

	/**
	 * 2.0.1 查询通道告警信息(需求 4807)
	 *
	 * @param inputDTO
	 * @return
	 */
	@PostMapping("/findAlarm")
	public GeneralResponseData findAlarmPage(@Validated @RequestBody FindAlarmInputDTO inputDTO) {
		log.info(" 查询通道告警信息接口参数:{}", inputDTO.toString());
		BizHandicap handicap = service.getHandicap(inputDTO.getOid().intValue());
		if (ObjectUtils.isEmpty(handicap)) {
			return new GeneralResponseData(-1, "盘口不存在!");
		}
		GeneralResponseData res = new GeneralResponseData(1, "操作成功");
		PageDTO<FindPageOutputDTO> data = null;
		try {
			data = service.findAlarmPage(inputDTO, handicap);
		} catch (Exception e) {
			e.printStackTrace();
			res = new GeneralResponseData(-1, "操作失败!" + e.getMessage());
		}
		res.setData(data);
		return res;
	}

	/**
	 * 2.0.2 查询某盘口下的银行卡号状态是否冻结或者删除（需求4707）
	 *
	 * @param inputDTO
	 * @return
	 */
	@PostMapping("/checkCardStatus")
	public GeneralResponseData checkCardStatus(@Validated @RequestBody CheckCardStatusInputDTO inputDTO) {
		log.info("查询某盘口下的银行卡号状态是否冻结或者删除接口参数:{}", inputDTO.toString());
		BizHandicap handicap = service.getHandicap(inputDTO.getOid().intValue());
		GeneralResponseData res = new GeneralResponseData(1, "操作成功");
		if (ObjectUtils.isEmpty(handicap)) {
			log.info("盘口不存在, 参数 :{}", ObjectMapperUtils.serialize(inputDTO));
			res = new GeneralResponseData(1, "盘口不存在");
			res.setData("1");
			return res;
		}
		inputDTO.setHandicapId(handicap.getId());
		String data = null;
		try {
			data = service.checkCardStatus(inputDTO);
		} catch (Exception e) {
			e.printStackTrace();
			res = new GeneralResponseData(-1, "操作失败!" + e.getMessage());
		}
		res.setData(data);
		return res;
	}

	@PostMapping("/findAccountNosByIds")
	public GeneralResponseData findAccountNosByIds(@RequestBody List<Integer> accountIds) {
		log.info("param :{}", accountIds);
		if (ObjectUtils.isEmpty(accountIds)) {
			return new GeneralResponseData(-1, "参数必传!");
		}
		GeneralResponseData ret = new GeneralResponseData(1, "获取成功!");
		List<String> res = null;
		try {
			res = service.findAccountNosByIds(accountIds);
		} catch (Exception e) {
			e.printStackTrace();
			ret = new GeneralResponseData(-1, "获取失败!" + e.getMessage());
		}
		ret.setData(res);
		return ret;
	}

	/**
	 * 2.0.3 查询可用于入款的银行卡类别
	 */
	@PostMapping("/bankList")
	public GeneralResponseData bankList() {
		GeneralResponseData res = new GeneralResponseData(1, "操作成功");
		List<Map<String, Object>> data = null;
		try {
			data = service.bankList();
		} catch (Exception e) {
			e.printStackTrace();
			res = new GeneralResponseData(-1, "操作失败!" + e.getMessage());
		}
		res.setData(data);
		return res;
	}
}