/**
 * 
 */
package com.xinbo.fundstransfer.daifucomponent.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Preconditions;
import com.xinbo.fundstransfer.daifucomponent.dto.output.DaifuResult;
import com.xinbo.fundstransfer.daifucomponent.service.Outward4DaifuService;
import com.xinbo.fundstransfer.daifucomponent.util.DaiFuStatusMapTaskStatus;
import com.xinbo.fundstransfer.domain.entity.BizOutwardTask;
import com.xinbo.fundstransfer.service.DaifuTaskService;
import com.xinbo.fundstransfer.service.HandicapService;
import com.xinbo.fundstransfer.service.OutwardTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * @author blake
 *
 */
@Slf4j
@Service
public class Outward4DaifuServiceImpl implements Outward4DaifuService {
	@Autowired
	private OutwardTaskService outwardTaskService;
	@Autowired @Lazy
	private DaifuTaskService daifuTaskService;

	@Autowired
	private HandicapService handicapService;

	private static final ObjectMapper mapper = new ObjectMapper();
	static {
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
	}

	private String toJson(Object o) {
		try {
			return mapper.writeValueAsString(o);
		} catch (JsonProcessingException e) {
			return null;
		}
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see com.xinbo.fundstransfer.daifucomponent.service.Outward4DaifuService#
	 *      doAsDaifuResult(com.xinbo.fundstransfer.daifucomponent.dto.output.
	 *      DaifuResult)
	 * 
	 *      第三方代付订单状态变化 出入款更新出款任务状态
	 * 
	 */
	@Override
	public void doAsDaifuResult(DaifuResult result) {
		log.debug("第三方代付订单状态变化 调用出入款更新出款任务状态！参数:{}", toJson(result));
		Preconditions.checkNotNull(result);
		Preconditions.checkNotNull(result.getResult());
		Preconditions.checkNotNull(result.getResult().getValue());
		Preconditions.checkNotNull(result.getOutwardTaskOrderNo());
		Byte status = result.getResult().getValue();
		Integer statusMap = DaiFuStatusMapTaskStatus.getTaskStatusFromDaifuStatus(status);
		log.debug("第三方代付订单状态变化 状态映射结果:{}", statusMap);
		if (null != statusMap) {
			try {
				BizOutwardTask task = outwardTaskService.findByIdAndStatusIn(result.getOutwardTaskId());
				Preconditions.checkNotNull(task);
				boolean res = daifuTaskService.daifuResultDeal(result, task);
				log.debug("代付结果状态变更处理结果:{},参数:result:{},task:{}", res, toJson(result), toJson(task));
			} catch (Exception e) {
				log.debug("第三方代付订单状态变化 处理结果异常:", e);
			}
		}
	}

}
