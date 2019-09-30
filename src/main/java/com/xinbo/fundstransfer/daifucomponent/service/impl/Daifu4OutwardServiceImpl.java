/**
 * 
 */
package com.xinbo.fundstransfer.daifucomponent.service.impl;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.xinbo.fundstransfer.daifucomponent.dto.output.DaifuResult;
import com.xinbo.fundstransfer.daifucomponent.dto.output.DaifuResult.ResultEnum;
import com.xinbo.fundstransfer.daifucomponent.service.Daifu4OutwardService;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.BizOutwardTask;
import com.xinbo.fundstransfer.service.HandicapService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author blake
 *
 */
@Slf4j
@Service
public class Daifu4OutwardServiceImpl implements Daifu4OutwardService {
	private static final ObjectMapper mapper = new ObjectMapper();
	static {
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
	}
	
	@Autowired
	private DaifuServiceImpl daifuServiceImpl;
	
	@Autowired
	private HandicapService handicapService;
	
	private String paramToJson(Object param) {
		try {
			return mapper.writeValueAsString(param);
		} catch (JsonProcessingException e) {
			log.error("线程：{}日志输出序列化参数时异常。", Thread.currentThread().getName(),e);
			return null;
		}
	}
	/**
	 * 判断某个盘口 会员提单的收款银行卡是否在支持的银行卡类型中
	 * @param handicapId 订单所属盘口id
	 * @param bankType 订单收款银行卡类型
	 * @return true 表示支持 false 表示不支持
	 */
	@Override
	public boolean isSupportedBankType(Integer handicapId, String bankType) {
		return daifuServiceImpl.isSupportedBankType(handicapId, bankType);
	}

	@Override
	public boolean isRead(BizOutwardTask outward) {
		log.info("线程：{}判断出款任务是否可以使用第三方代付，参数{}", Thread.currentThread().getName(),paramToJson(outward));
		Assert.isTrue(!StringUtils.isEmpty(outward.getHandicap()),"盘口号不能为空");
		Assert.isTrue(!ObjectUtils.isEmpty(outward.getId()), "id不能为空");
		Assert.isTrue(!ObjectUtils.isEmpty(outward.getOutwardRequestId()), "outwardRequestId不能为空");
		Assert.isTrue(!ObjectUtils.isEmpty(outward.getAmount()) && outward.getAmount().compareTo(BigDecimal.ZERO)>0, "amount不能为空,并且amount必须大于0");
		BizHandicap bizHandicap = handicapService.findFromCacheByCode(outward.getHandicap());
		Assert.isTrue(!ObjectUtils.isEmpty(bizHandicap), "未知的盘口号");
		boolean result = daifuServiceImpl.isReady(bizHandicap,outward.getId(),
						outward.getOutwardRequestId(),outward.getAmount());
		log.info("线程：{}判断盘口{}出款任务订单号{}是否可以使用第三方代付，结果：{}", Thread.currentThread().getName(), outward.getHandicap(),
				outward.getOrderNo(), result);
		return result;
	}

	@Override
	public DaifuResult daifu(BizOutwardTask outward) {
		log.info("线程：{}出款任务请求使用第三方代付，参数{}", Thread.currentThread().getName(),paramToJson(outward));
		Assert.isTrue(!StringUtils.isEmpty(outward.getHandicap()),"盘口号不能为空");
		Assert.isTrue(!StringUtils.isEmpty(outward.getOperator()),"operator不能为空");
		Assert.isTrue(!ObjectUtils.isEmpty(outward.getId()), "id不能为空");
		Assert.isTrue(!ObjectUtils.isEmpty(outward.getOutwardRequestId()), "outwardRequestId不能为空");
		Assert.isTrue(!ObjectUtils.isEmpty(outward.getAmount()) && outward.getAmount().compareTo(BigDecimal.ZERO)>0, "amount不能为空,并且amount必须大于0");
		BizHandicap bizHandicap = handicapService.findFromCacheByCode(outward.getHandicap());
		Assert.isTrue(!ObjectUtils.isEmpty(bizHandicap), "未知的盘口号");
			//生成代付信息并且请求代付
		DaifuResult result = daifuServiceImpl.genDaifuInfoAndSend2PayCore(bizHandicap,outward.getOperator(),
						outward.getOutwardRequestId(),outward.getId(), outward.getAmount());
		log.info("线程：{}盘口{}出款任务订单号{}请求使用第三方代付，结果：{}", Thread.currentThread().getName(), outward.getHandicap(),
				outward.getOrderNo(), result.getResult().getDesc());
		return result;
	}

	@Override
	public DaifuResult query(BizOutwardTask outward) {
		log.info("线程：{}查询出款任务第三方代付结果，参数：{}",Thread.currentThread().getName(),paramToJson(outward));
		Assert.isTrue(!StringUtils.isEmpty(outward.getHandicap()),"盘口号不能为空");
		Assert.isTrue(!ObjectUtils.isEmpty(outward.getId()), "id不能为空");
		Assert.isTrue(!ObjectUtils.isEmpty(outward.getOutwardRequestId()), "outwardRequestId不能为空");
		BizHandicap bizHandicap = handicapService.findFromCacheByCode(outward.getHandicap());
		Assert.isTrue(!ObjectUtils.isEmpty(bizHandicap), "未知的盘口号");
		DaifuResult result = daifuServiceImpl.getDaifuResult(bizHandicap,
					outward.getOutwardRequestId());
		log.info("线程：{}查询盘口{}出款任务订单号{}第三方代付结果，结果：{}", Thread.currentThread().getName(), outward.getHandicap(),
				outward.getOrderNo(), result.getResult().getDesc());
		return result;
	}

	@Override
	public DaifuResult intervene(BizOutwardTask outward, ResultEnum daifuResult) {
		log.info("线程：{}干预出款任务第三方代付结果，参数：{}|{}",Thread.currentThread().getName(),paramToJson(outward),paramToJson(daifuResult));
		Assert.isTrue(!StringUtils.isEmpty(outward.getHandicap()),"盘口号不能为空");
		Assert.isTrue(!StringUtils.isEmpty(outward.getOperator()),"operator不能为空");
		Assert.isTrue(!ObjectUtils.isEmpty(outward.getOutwardRequestId()), "outwardRequestId不能为空");
		Assert.isTrue(!ObjectUtils.isEmpty(daifuResult), "代付结果不能为空");
		Assert.isTrue(ResultEnum.SUCCESS.equals(daifuResult)||ResultEnum.PAYING.equals(daifuResult)||ResultEnum.ERROR.equals(daifuResult), "daifuResult值超出范围");
		BizHandicap bizHandicap = handicapService.findFromCacheByCode(outward.getHandicap());
		Assert.isTrue(!ObjectUtils.isEmpty(bizHandicap), "未知的盘口号");
		DaifuResult result = daifuServiceImpl.interveneByResultEnum(bizHandicap,outward.getOperator(),outward.getOutwardRequestId(),daifuResult);
		log.info("线程：{}干预盘口{}出款任务订单号{}第三方代付结果，结果：{}", Thread.currentThread().getName(), outward.getHandicap(),
				outward.getOrderNo(), result);
		return result;
	}

}
