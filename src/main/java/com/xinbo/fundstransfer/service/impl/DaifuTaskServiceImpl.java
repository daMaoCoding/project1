package com.xinbo.fundstransfer.service.impl;

import com.google.common.base.Preconditions;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.net.http.RequestBodyParser;
import com.xinbo.fundstransfer.component.net.http.v2.HttpClientNew;
import com.xinbo.fundstransfer.daifucomponent.dto.output.DaifuResult;
import com.xinbo.fundstransfer.daifucomponent.service.Daifu4OutwardService;
import com.xinbo.fundstransfer.daifucomponent.service.DaifuSurpportBankTypeService;
import com.xinbo.fundstransfer.daifucomponent.util.DaiFuStatusMapTaskStatus;
import com.xinbo.fundstransfer.domain.entity.BizOutwardRequest;
import com.xinbo.fundstransfer.domain.entity.BizOutwardTask;
import com.xinbo.fundstransfer.domain.enums.OutWardPayType;
import com.xinbo.fundstransfer.domain.enums.OutwardTaskStatus;
import com.xinbo.fundstransfer.domain.repository.OutwardTaskRepository;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import com.xinbo.fundstransfer.runtime.MemCacheUtils;
import com.xinbo.fundstransfer.service.AllocateOutwardTaskService;
import com.xinbo.fundstransfer.service.DaifuTaskService;
import com.xinbo.fundstransfer.service.OutwardTaskService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.RequestBody;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.Date;

/**
 * @author Administrator
 */
@Slf4j
@Service
public class DaifuTaskServiceImpl implements DaifuTaskService {

	private AllocateOutwardTaskService allocateOutwardTaskService;
	private OutwardTaskRepository outwardTaskRepository;
	private OutwardTaskService outwardTaskService;
	private AllocateOutwardTaskService outwardTaskAllocateService;
	@Autowired
	private Daifu4OutwardService daifu4OutwardService;
	@Autowired
	private DaifuSurpportBankTypeService daifuSurpportBankTypeService;

	@Autowired @Lazy
	public DaifuTaskServiceImpl(AllocateOutwardTaskService outwardTaskAllocateService,
			AllocateOutwardTaskService allocateOutwardTaskService, OutwardTaskRepository outwardTaskRepository,
			OutwardTaskService outwardTaskService) {
		this.allocateOutwardTaskService = allocateOutwardTaskService;
		this.outwardTaskRepository = outwardTaskRepository;
		this.outwardTaskService = outwardTaskService;
		this.outwardTaskAllocateService = outwardTaskAllocateService;
	}

	/** 检查 会员出款银行卡类型是否被支持 */
	@Override
	public boolean daifuBankTypeCondition(String memberBankType) {
		boolean exist = daifuSurpportBankTypeService.queryBankTypeIncluded(memberBankType);
		log.debug("判断会员的出款银行类型是否支持:memberBankType  {},exist  {}", memberBankType, exist);
		return exist;
	}

	@Override
	public boolean daifuCondition(BizOutwardRequest outwardRequest) {
		log.debug("是否满足调用代付3个条件,参数:状态:{},单号:{},备注:{},审核人:{}", outwardRequest.getStatus(), outwardRequest.getOrderNo(),
				outwardRequest.getRemark(), outwardRequest.getReviewer());
		boolean flag = false;
		try {
			Integer limitAmount = Integer.valueOf(
					MemCacheUtils.getInstance().getSystemProfile().getOrDefault("DAIFU_AMOUNT_UPLIMIT", "500"));
			log.debug("代付最大金额配置项:{}", limitAmount);
			boolean condition1 = outwardRequest.getAmount().compareTo(new BigDecimal(limitAmount)) <= 0;
			log.debug("代付条件1 金额小于配置项或者小于500:{}", condition1);
			if (!condition1) {
				log.debug("审核之后校验是否满足金额:{}小于配置值:{},结果:{}", outwardRequest.getAmount(), limitAmount, condition1);
				return flag;
			}
			boolean condition2 = allocateOutwardTaskService.checkFirst(outwardRequest.getReview());
			log.debug("代付条件2 非首次出:{}", condition2);
			if (condition2) {
				log.debug("审核之后校验是否满足非首次出款:{},结果:{}", outwardRequest.getReview(), condition2);
				return flag;
			}
			String toAccountBank = outwardRequest.getToAccountBank();
			String toAccountName = outwardRequest.getToAccountName();
			log.debug("对方账号银行类型toAccountBank:{},银行支行名称toAccountName:{}", toAccountBank, toAccountName);
			if (StringUtils.isBlank(toAccountBank) && StringUtils.isBlank(toAccountName)) {
				log.debug("审核之后校验是否满足银行类型,参数:toAccountBank:{},toAccountName:{} 都是空!", toAccountBank, toAccountName);
				return flag;
			}
			String toBankType = ObjectUtils.isEmpty(toAccountBank) ? toAccountName : toAccountBank;
			boolean condition3;
			try {
				log.debug("调用isSupportedBankType ");
				condition3 = daifu4OutwardService.isSupportedBankType(outwardRequest.getHandicap(), toBankType);
			} catch (Exception e) {
				e.printStackTrace();
				log.error("调用isSupportedBankType 异常:", e);
				return flag;
			}
			log.debug("代付条件3 是否支持银行isSupportedBankType结果:{}", condition3);
			if (!condition3) {
				log.debug("审核之后校验是否满足银行类型,参数:toAccountBank:{},toAccountName:{},结果：{}", toAccountBank, toAccountName,
						condition3);
				return flag;
			}
			if (condition1 && !condition2 && condition3) {
				flag = true;
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
			log.error("代付检查3个条件异常NumberFormatException:", e);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("代付检查3个条件异常Exception:", e);
		}
		log.debug("代付检查3个条件结果:", flag);
		return flag;
	}

	/**
	 * 调用代付 outward对象以下参数不能为空:handicap,operator,id,outwardRequestId,orderNo,amount
	 * 
	 * @param outwardTask
	 *            代付的出款任务
	 * @return
	 */
	@Override
	@Transactional(rollbackOn = Exception.class)
	public DaifuResult callDaifu(BizOutwardTask outwardTask) {
		log.debug("调用代付,参数:{}", outwardTask);
		Preconditions.checkNotNull(outwardTask);
		Preconditions.checkNotNull(outwardTask.getHandicap());
		Preconditions.checkNotNull(outwardTask.getId());
		Preconditions.checkNotNull(outwardTask.getOutwardRequestId());
		Preconditions.checkNotNull(outwardTask.getOrderNo());
		Preconditions.checkNotNull(outwardTask.getAmount());
		// 1表示系统操作
		outwardTask.setOperator(1);
		DaifuResult result = daifu4OutwardService.daifu(outwardTask);
		return result;
	}

	/**
	 * 代付取消或其他情况 按原流程出款 0
	 */

	private static final int DAIFU_TASK_STATUS0 = 0;
	/**
	 * 代付初始化状态 未知
	 */
	private static final int DAIFU_TASK_STATUS1 = 1;
	/**
	 * 正在代付转正在出款
	 */
	private static final int DAIFU_TASK_STATUS2 = 2;
	/**
	 * 代付成功转待完成
	 */
	private static final int DAIFU_TASK_STATUS3 = 3;
	/**
	 * 转待排查
	 */
	private static final int DAIFU_TASK_STATUS4 = 4;
	/**
	 * 订单不存在
	 */
	private static final int DAIFU_TASK_STATUS5 = 5;

	/**
	 * 处理代付调用立即返回的结果
	 * 
	 * @param daifuResult
	 *            调用代付立即返回的结果
	 * @param task
	 *            代付的出款任务
	 * @return
	 */
	@Override
	@Transactional(rollbackOn = Exception.class)
	public boolean daifuResultDeal(DaifuResult daifuResult, BizOutwardTask task) {
		Preconditions.checkNotNull(daifuResult);
		Preconditions.checkNotNull(task);
		Integer status = DaiFuStatusMapTaskStatus.getTaskStatusFromDaifuStatus(daifuResult.getResult().getValue());
		log.debug("处理调用代付返回的结果,daifuResult:{},task:{},将处理的状态:{}", ObjectMapperUtils.serialize(daifuResult),
				ObjectMapperUtils.serialize(task), DaiFuStatusMapTaskStatus.getDaifuStausMapDesc(status));
		if (null == status) {
			log.debug("处理代付结果 状态映射结果没有值:{}", status);
			return true;
		}
		if (status == DAIFU_TASK_STATUS5) {
			log.debug("处理代付结果 状态映射结果值:{}", DAIFU_TASK_STATUS5);
			BizOutwardTask task1 = outwardTaskService.findById(task.getId());
			if (task1 == null) {
				log.debug("订单不存在,不处理。任务id:{}", task.getId());
				return true;
			} else {
				log.debug("代付结果无订单:{},但是系统查询存在订单:{},转待排查!", ObjectMapperUtils.serialize(task), task1);
				dealMapStatus4(task, daifuResult);
				return true;
			}
		}
		boolean common1234 = status == DAIFU_TASK_STATUS4 || status == DAIFU_TASK_STATUS3
				|| status == DAIFU_TASK_STATUS2 || status == DAIFU_TASK_STATUS1;
		boolean common234 = status == DAIFU_TASK_STATUS4 || status == DAIFU_TASK_STATUS3
				|| status == DAIFU_TASK_STATUS2;
		if (common1234) {
			task.setThirdInsteadPay(1);
			task.setOperator(1);
			task.setOutwardPayType(OutWardPayType.ThirdInsteadPay.getType());
		}
		if (common234) {
			task.setAccountId(daifuResult == null ? null : daifuResult.getDaifuConfigId());
		}

		try {
			if (status == DAIFU_TASK_STATUS4) {
				log.debug("转待排查, 状态:{}", task, status);
				dealMapStatus4(task, daifuResult);
				return true;
			}
			if (status == DAIFU_TASK_STATUS3) {
				// task.setTimeConsuming(getDaifuTakenTime(daifuResult));
				log.debug("代付成功转待完成, 状态:{}", task, status);
				dealMapStatus3(task, daifuResult);
				return true;
			}
			if (status == DAIFU_TASK_STATUS2) {
				task.setAsignTime(new Date());
				// task.setTimeConsuming(getDaifuTakenTime(daifuResult));
				log.debug("代付正在支付转正在出款,状态:{}", status);
				dealMapStatus2(task, daifuResult);
			}
			if (status == DAIFU_TASK_STATUS1) {
				log.debug("代付未出款,状态:{}", status);
				dealMapStatus1(task, daifuResult);
				return true;
			}
			if (status == DAIFU_TASK_STATUS0) {
				log.debug("处理调用代付返回的结果,状态:{},任务：{},按原流程放入队列!", status, task);
				task = dealMapStatus0(task);
				// 待出款任务发送Redis队列，待出款人员认领
				// BizOutwardRequest outwardRequest =
				// outwardRequestService.findByOrderNo(task.getOrderNo());
				// outwardTaskAllocateService.rpush(outwardRequest, task, false);
				// 转人工出款
				// "系统调用代付异常转人工出款!";
				StringBuilder remark = new StringBuilder("系统调用代付");
				remark = wrapRemark(remark, daifuResult.getChannelName(), daifuResult.getMemberId());
				remark.append("转人工出款!");
				outwardTaskAllocateService.remark4Mgr(task.getId(), true, false, null, null, remark.toString());
				return false;
			}
			return false;
		} catch (Exception e) {
			log.error("处理第三方代付结果 异常:", e);
			return false;
		}
	}

	@Autowired
	RequestBodyParser requestBodyParser;

	/**
	 * 通知平台出款订单使用哪一个第三方出款
	 *
	 * @param daifuResult
	 * @param outwardTask
	 */
	@Override
	@Transactional(rollbackOn = Exception.class)
	public void selectDaifu(DaifuResult daifuResult, BizOutwardTask outwardTask) {
		RequestBody requestBody = requestBodyParser.buildRequestBodyForSelectDaifu(outwardTask.getHandicap(),
				outwardTask.getOrderNo(), daifuResult.getPlatPayCode(), daifuResult.getChannelName(),
				daifuResult.getMemberId());
		HttpClientNew.getInstance().getPlatformServiceApi().selectDaifu(requestBody).subscribe(
				simpleResponseData -> log.debug("调用返回结果:{}", simpleResponseData), e -> log.error("调用失败:", e));
	}

	private final String getMemberIdAlias(String memberId) {
		String res = "";
		if (StringUtils.isNotBlank(memberId)) {
			res = memberId.substring(0, 3) + "****" + memberId.substring(memberId.length() - 3);
		}
		return res;
	}

	/**
	 * 处理 按原流程处理的情况
	 * 
	 * @param task
	 */
	@Transactional(rollbackOn = Exception.class)
	@Override
	public BizOutwardTask dealMapStatus0(BizOutwardTask task) {
		Preconditions.checkNotNull(task);
		try {
			task.setThirdInsteadPay(0);
			task.setOperator(null);
			task.setAccountId(null);
			task.setAsignTime(null);
			task.setStatus(OutwardTaskStatus.Undeposit.getStatus());
			task.setRemark(CommonUtils.genRemark(task.getRemark(), "调用代付返回结果为空转正常流程", new Date(), "系统"));
			task.setOutwardPayType(OutWardPayType.MANUAL.getType());
			task = outwardTaskRepository.saveAndFlush(task);
			return task;
		} catch (Exception e) {
			log.error("代付结果处理按原流程处理的情况异常:", e);
			return null;
		}
	}

	private final StringBuilder wrapRemark(StringBuilder remark, String channelName, String memberId) {
		if (StringUtils.isNotBlank(channelName)) {
			remark.append(":提供商-" + channelName);
		}
		if (StringUtils.isNotBlank(memberId)) {
			remark.append(",商号-" + getMemberIdAlias(memberId));
		}
		return remark;
	}

	/**
	 * 处理 代付未出款
	 * 
	 * @param task
	 */
	@Transactional(rollbackOn = Exception.class)
	@Override
	public void dealMapStatus1(BizOutwardTask task, DaifuResult daifuResult) {
		try {
			task.setAccountId(null);
			// task.setAsignTime(daifuResult.getCreateTime());
			task.setStatus(OutwardTaskStatus.Undeposit.getStatus());
			// 调用代付转未出款
			StringBuilder remark = new StringBuilder("调用代付");
			remark = wrapRemark(remark, daifuResult.getChannelName(), daifuResult.getMemberId());
			remark.append("转未出款!");
			task.setRemark(CommonUtils.genRemark(task.getRemark(), remark.toString(), new Date(), "系统"));
			outwardTaskRepository.saveAndFlush(task);
		} catch (Exception e) {
			log.error("代付结果处理转未出款 异常:", e);
		}
	}

	private int getDaifuTakenTime(DaifuResult daifuResult) {
		log.debug("获取创建时间:{}", daifuResult);
		Preconditions.checkNotNull(daifuResult);
		Preconditions.checkNotNull(daifuResult.getCreateTime());
		long time = System.currentTimeMillis() - daifuResult.getCreateTime().getTime() / 1000L;
		return (int) time;
	}

	/**
	 * 处理 代付正在支付转正在出款
	 * 
	 * @param task
	 */
	@Transactional(rollbackOn = Exception.class)
	@Override
	public void dealMapStatus2(BizOutwardTask task, DaifuResult daifuResult) {
		try {
			task.setAsignTime(daifuResult.getCreateTime());
			task.setStatus(OutwardTaskStatus.Undeposit.getStatus());
			StringBuilder remark = new StringBuilder("调用代付");
			remark = wrapRemark(remark, daifuResult.getChannelName(), daifuResult.getMemberId());
			remark.append("转正在出款");
			task.setRemark(CommonUtils.genRemark(task.getRemark(), remark.toString(), new Date(), "系统"));
			outwardTaskRepository.saveAndFlush(task);
		} catch (Exception e) {
			log.error("代付正在支付转正在出款 异常：", e);
		}
	}

	/**
	 * 处理 代付成功转完成
	 * 
	 * @param task
	 */
	@Transactional(rollbackOn = Exception.class)
	@Override
	public void dealMapStatus3(BizOutwardTask task, DaifuResult daifuResult) {
		Preconditions.checkNotNull(task);
		Preconditions.checkNotNull(daifuResult.getDaifuConfigId());
		log.debug("代付成功转完成,参数：task:{}，daifuResult:{}", ObjectMapperUtils.serialize(task),
				ObjectMapperUtils.serialize(daifuResult));
		try {
			if (ObjectUtils.isEmpty(task.getAsignTime())) {
				task.setAsignTime(daifuResult.getCreateTime());
			}
			task.setAccountId(daifuResult.getDaifuConfigId());
			// task.setTimeConsuming(getDaifuTakenTime(daifuResult));
			// task.setStatus(OutwardTaskStatus.Deposited.getStatus());
			// task.setRemark(CommonUtils.genRemark(task.getRemark(), "调用代付成功转完成", new
			// Date(), "系统"));
			// outwardTaskRepository.saveAndFlush(task);
			StringBuilder remark = new StringBuilder("调用代付成功");
			remark = wrapRemark(remark, daifuResult.getChannelName(), daifuResult.getMemberId());
			remark.append("转完成");
			outwardTaskService.thirdOutwardTaskDeal(task, null, task.getId(), 1, remark.toString(),
					daifuResult.getDaifuConfigId(), daifuResult.getPlatPayCode());

		} catch (Exception e) {
			log.error("代付成功转完成 异常：", e);
		}
	}

	/**
	 * 处理 转待排查
	 * 
	 * @param task
	 */
	@Transactional(rollbackOn = Exception.class)
	@Override
	public void dealMapStatus4(BizOutwardTask task, DaifuResult daifuResult) {
		try {
			task.setAccountId(daifuResult.getDaifuConfigId());
			// task.setTimeConsuming(getDaifuTakenTime(daifuResult));
			task.setStatus(OutwardTaskStatus.Failure.getStatus());
			StringBuilder remark = new StringBuilder("调用代付");
			remark = wrapRemark(remark, daifuResult.getChannelName(), daifuResult.getMemberId());
			remark.append("转待排查");
			task.setRemark(CommonUtils.genRemark(task.getRemark(), remark.toString(), new Date(), "系统"));
			outwardTaskRepository.saveAndFlush(task);
		} catch (Exception e) {
			log.error("代付转待排查 异常:", e);
		}
	}

	@Override
	public void intervene(BizOutwardTask outward) {
		try {
			log.debug("人工取消/拒绝代付:{}", ObjectMapperUtils.serialize(outward));
			daifu4OutwardService.intervene(outward, DaifuResult.ResultEnum.ERROR);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean daifuQueryStatusDeal(DaifuResult daifuResult) {
		return false;
	}
}
