/**
 *
 */
package com.xinbo.fundstransfer.daifucomponent.service.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.xinbo.fundstransfer.daifucomponent.dto.input.DaifuConfigSyncReqParamConfig;
import com.xinbo.fundstransfer.daifucomponent.dto.input.DaifuConfigSyncReqParamFeeConfig;
import com.xinbo.fundstransfer.daifucomponent.dto.input.DaifuConfigSyncReqParamLevelConfig;
import com.xinbo.fundstransfer.daifucomponent.dto.input.DaifuConfigSyncReqParamOutConfig;
import com.xinbo.fundstransfer.daifucomponent.dto.input.DaifuConfirmReqParamTo;
import com.xinbo.fundstransfer.daifucomponent.dto.output.DaifuConfigParamTo;
import com.xinbo.fundstransfer.daifucomponent.dto.output.DaifuResult;
import com.xinbo.fundstransfer.daifucomponent.dto.output.DaifuResult.ResultEnum;
import com.xinbo.fundstransfer.daifucomponent.entity.DaifuConfigRequest;
import com.xinbo.fundstransfer.daifucomponent.entity.DaifuInfo;
import com.xinbo.fundstransfer.daifucomponent.exception.PayPlatNotCallAgainException;
import com.xinbo.fundstransfer.daifucomponent.http.Crk2PayCoreQueryResponse;
import com.xinbo.fundstransfer.daifucomponent.http.Crk2PayCoreSendResponse;
import com.xinbo.fundstransfer.daifucomponent.util.DaifuCacheUtil;
import com.xinbo.fundstransfer.daifucomponent.util.OutConfigHolder;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.BizLevel;
import com.xinbo.fundstransfer.domain.entity.BizOutwardRequest;
import com.xinbo.fundstransfer.domain.repository.DaifuConfigRequestRepository;
import com.xinbo.fundstransfer.domain.repository.DaifuInfoRepository;
import com.xinbo.fundstransfer.domain.repository.LevelRepository;
import com.xinbo.fundstransfer.domain.repository.OutwardRequestRepository;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import com.xinbo.fundstransfer.runtime.task.HandicapNotifyUrlTask;
import com.xinbo.fundstransfer.service.HandicapService;
import com.xinbo.fundstransfer.service.RedisService;
import com.xinbo.fundstransfer.utils.randutil.JedisLock;

import lombok.extern.slf4j.Slf4j;

/**
 * @author blake
 *
 */
@Slf4j
@Service
public class DaifuServiceImpl {

	private static final ObjectMapper mapper = new ObjectMapper();
	static {
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
	}

	@Autowired
	private DaifuInfoRepository daifuInfoDao;

	@Autowired
	private DaifuConfigRequestRepository daifuConfigDao;

	@Autowired
	private OutwardRequestRepository outwardRequestDao;

	@Autowired
	private DaifuExtService daifuExtService;
	@Autowired
	private DaifuSubServiceImpl daifuSubServiceImpl;
	@Autowired
	private HandicapService handicapService;
	@Autowired
	private RedisService redisService;
	@Autowired
	private DaifuCacheUtil daifuCacheUtil;
	@Autowired
	private LevelRepository levelDao;

	private static final String daifuOrderHandicapSplit = "D";
	private static final String daifuOrderFormat = "%s%s%s%s";

	protected static final BigDecimal bigDecimal100 = new BigDecimal("100");
	// 支付系统返回的程序处理状态（不是真正订单的处理状态）
	protected static final String RESPONSE_DAIFU_CODE_SUCCESS = "SUCCESS";
	private static final String RESPONSE_DAIFU_CODE_ERROR = "ERROR";
	// 支付系统返回的订单状态
	/**
	 * payCore返回出款单的代付结果-成功<br>
	 * SUCCESS
	 */
	protected static final String ORDER_STATE_SUCCESS ="SUCCESS";
	/**
	 * payCore返回出款单的代付结果-正在支付<br>
	 * PAYING
	 */
	protected static final String ORDER_STATE_PAYING ="PAYING";
	/**
	 * payCore返回出款单的代付结果-取消<br>
	 * ERROR
	 */
	protected static final String ORDER_STATE_ERROR ="ERROR";
	/**
	 * payCore返回出款单的代付结果-未知<br>
	 * UNKOWN<br>
	 * 可能造成的原因：请求超时-网络中断等
	 */
	protected static final String ORDER_STATE_UNKOWN ="UNKNOW";


	private static final String DAIFU_OUT_ID_DISTRIBUTED_LOCK = "DAIFU_OUT_ID_DISTRICT_LOCK_%s";

	private static final String DAIFU_INFO_SENDED_KEY="DAIFU:INFO_%s:send_%s";
	private static final String DAIFU_INFO2OUTREQUEST_SENDED_KEY="DAIFU:INFO2OUTREQUEST_%s:send_%s";

	/**
	 * 支付平台约定某些服务提供商支持全部银行，不再单独标记某一个银行<br>
	 * 支持全部银行
	 */
	private static final String SUPPORT_ALL_BANK = "支持全部银行";

	private static final Byte ONE = new Byte("1");
	private static final Byte ZERO = new Byte("0");

	/**
	 * 代付金额要求单位为 分，所以格式化<br>
	 * 格式化方式：实际金额 * 100.不保留小数
	 *
	 * @param amount
	 * @return
	 */
	private static final String daifuAmountFormat(BigDecimal amount) {
		return String.format("%s", amount.multiply(bigDecimal100).setScale(0, RoundingMode.HALF_UP));
	}

	/**
	 * 构建代付订单
	 *
	 * @param handicap
	 * @param orderNo
	 * @return
	 */
	private String genDaifuOrderNo(Integer handicap, String orderNo) {
		return String.format(daifuOrderFormat, orderNo, String.valueOf(System.currentTimeMillis()).substring(7, 13),
				daifuOrderHandicapSplit, handicap);
	}

	/**
	 * 获取回调地址
	 *
	 * @param handicapCode
	 * @return
	 */
	private String getNotifyUrl(String handicapCode) {
		Object notifyUrl = this.redisService.getStringRedisTemplate()
				.boundHashOps(HandicapNotifyUrlTask.HANDICAP_NOTIFY_URL).get(handicapCode);
		return ObjectUtils.isEmpty(notifyUrl) ? null : notifyUrl.toString();
	}

	/**
	 * 从代付订单中获取 handicap
	 *
	 * @param orderNo
	 * @return
	 */
	private Integer getHandicapByDaifuOrderNo(String orderNo) {
		if (StringUtils.isEmpty(orderNo) || !orderNo.contains(daifuOrderHandicapSplit)) {
			return null;
		} else {
			String[] tmp = orderNo.split(daifuOrderHandicapSplit);
			if (tmp.length > 1) {
				return Integer.parseInt(tmp[1]);
			} else {
				return Integer.parseInt(tmp[0]);
			}
		}
	}

	public Object getConfigParamByOrderId(String orderId)
			throws PayPlatNotCallAgainException, JsonParseException, JsonMappingException, IOException {
		log.info("支付平台查询代付订单及商号密钥信息，订单号 {}", orderId);
		Integer handicap = getHandicapByDaifuOrderNo(orderId);
		BizHandicap bizHandicap = handicapService.findFromCacheById(handicap);
		if (null == bizHandicap) {
			log.error("根据代付订单{}获取的盘口id{}没有查询到对应的盘口信息", orderId, handicap);
			throw new PayPlatNotCallAgainException(String.format("未知的盘口id%s", handicap));
		}
		DaifuInfo daifuInfo = getDaifuInfoFromCache(bizHandicap.getCode(),orderId);
		if(ObjectUtils.isEmpty(daifuInfo)) {
			Optional<DaifuInfo> daifuInfo1 = daifuInfoDao.findOne((root, criteriaQuery, criteriaBuilder) -> {
				List<Predicate> predicateList = new ArrayList<>();
				Path<Integer> handicapPath = root.get("handicapId");
				Path<String> channelNamePath = root.get("platPayCode");
				Predicate p1 = criteriaBuilder.equal(handicapPath, bizHandicap.getId());
				predicateList.add(p1);
				Predicate p2 = criteriaBuilder.equal(channelNamePath, orderId);
				predicateList.add(p2);
				criteriaQuery.where(predicateList.toArray(new Predicate[predicateList.size()]));
				return null;
			});
			if(daifuInfo1.isPresent()) {
				daifuInfo = daifuInfo1.get();
			}
		}
		if (ObjectUtils.isEmpty(daifuInfo)) {
			log.error("支付平台查询代付订单及商号密钥信息，订单号 {}数据不存在", orderId);
			throw new PayPlatNotCallAgainException(String.format("未知的代付订单%s", orderId));
		}

		// 平台出款单
		BizOutwardRequest outwardRequest =getOutwardRequestFromCache(bizHandicap.getCode(), daifuInfo.getOutwardRequestId());
		if(ObjectUtils.isEmpty(outwardRequest)) {
			log.info("根据代付订单{}对应的出款单id={}未能在缓存中获取数据，将查表获取",orderId,daifuInfo.getOutwardRequestId());
			Optional<BizOutwardRequest> outwardRequestOptional = outwardRequestDao.findById(daifuInfo.getOutwardRequestId());
			if(outwardRequestOptional.isPresent()) {
				outwardRequest = outwardRequestOptional.get();
			}
			log.info("根据代付订单{}对应的出款单id={}反查出款单，所得数据为：{}",orderId,daifuInfo.getOutwardRequestId(),mapper.writeValueAsString(outwardRequest));
		}
		log.info("根据代付订单{}对应的出款单id={}对应数据为:{}",orderId,daifuInfo.getOutwardRequestId(),mapper.writeValueAsString(outwardRequest));

		DaifuConfigParamTo result = new DaifuConfigParamTo();

		if (outwardRequest != null) {
			// 会员申请出款时的ip
			result.setaPI_Client_IP(outwardRequest.getOutIp());
			// 出款会员名称
			result.setaPI_CUSTOMER_ACCOUNT(outwardRequest.getMember());
			//mark 2019-05-23 需求5897 字段意义修改
			// 原分行名称
			//result.setaPI_CUSTOMER_BANK_BRANCH(outwardRequest.getToAccountName());
			//API_CUSTOMER_BANK_BRANCH 分行所在省份
			result.setaPI_CUSTOMER_BANK_BRANCH(outwardRequest.getBankProvince());
			//API_CUSTOMER_BANK_SUB_BRANCH 支行所在城市
			result.setaPI_CUSTOMER_BANK_SUB_BRANCH(outwardRequest.getBankCity());
			
			// 银行名称
			result.setaPI_CUSTOMER_BANK_NAME(outwardRequest.getToAccountBank());
			// 银行卡号
			result.setaPI_CUSTOMER_BANK_NUMBER(outwardRequest.getToAccount());
			// 收款人姓名
			result.setaPI_CUSTOMER_NAME(outwardRequest.getToAccountOwner());
		}

		result.setaPI_AMOUNT(daifuAmountFormat(daifuInfo.getExactMoney()));
		result.setaPI_OID(bizHandicap.getCode());
		result.setaPI_ORDER_ID(daifuInfo.getPlatPayCode());
		result.setaPI_ORDER_STATE(String.valueOf(daifuInfo.getPlatStatus()));
		result.setaPI_OrDER_TIME(String.valueOf(daifuInfo.getCreateTime().getTime()));
		result.setaPI_CHANNEL_BANK_NAME(daifuInfo.getChannelBankName());

		// 出款通道配置
		Optional<DaifuConfigRequest> daifuConfigOptional = daifuConfigDao.findById(daifuInfo.getDaifuConfigId());
		if (daifuConfigOptional.isPresent()) {
			DaifuConfigRequest daifuConfig = daifuConfigOptional.get();
			result.setaPI_MEMBERID(daifuConfig.getMemberId());
			if (!StringUtils.isEmpty(daifuConfig.getConfig())) {
				DaifuConfigSyncReqParamConfig config = mapper.readValue(daifuConfig.getConfig(),
						DaifuConfigSyncReqParamConfig.class);
				if (config != null) {
					result.setaPI_KEY(config.getPrivateKey());
					if (!StringUtils.isEmpty(config.getNotifyUrl())) {
						result.setaPI_NOTIFY_URL_PREFIX(config.getNotifyUrl());
					} else {
						result.setaPI_NOTIFY_URL_PREFIX(getNotifyUrl(bizHandicap.getCode()));
					}
					if(!StringUtils.isEmpty(result.getaPI_NOTIFY_URL_PREFIX()) && !result.getaPI_NOTIFY_URL_PREFIX().toLowerCase().startsWith("http://")) {
						result.setaPI_NOTIFY_URL_PREFIX("http://".concat(result.getaPI_NOTIFY_URL_PREFIX()));
					}
					result.setaPI_PUBLIC_KEY(config.getPublicKey());
				}
			}
		}
		// FIXME 需要其他参数写在这里
		// result.setaPI_OTHER_PARAM(aPI_OTHER_PARAM);
		return result;
	}

	/**
	 * 从缓存中获取代付订单信息
	 * @param handicapCode
	 * @param orderId
	 * @return
	 */
	private DaifuInfo getDaifuInfoFromCache(String handicapCode,String orderId) {
		DaifuInfo result = null;
		String key = String.format(DAIFU_INFO_SENDED_KEY, handicapCode,orderId);
		String redisStrValue = redisService.getStringRedisTemplate().boundValueOps(key).get();
		if(!StringUtils.isEmpty(redisStrValue)) {
			try {
				result = mapper.readValue(redisStrValue,DaifuInfo.class);
			} catch (IOException e) {
				log.error("从缓存中获取代付订单{}信息,将内容{}转换成对象时异常",orderId,redisStrValue,e);
			}
		}
		return result;
	}

	/**
	 * 从缓存中获取代付订单对应的出款单信息
	 * @param handicapCode
	 * @param orderId
	 * @return
	 */
	private BizOutwardRequest getOutwardRequestFromCache(String handicapCode,Long outRequestId) {
		BizOutwardRequest result = null;
		String key = String.format(DAIFU_INFO2OUTREQUEST_SENDED_KEY, handicapCode,outRequestId);
		String redisStrValue = redisService.getStringRedisTemplate().boundValueOps(key).get();
		log.info("从缓存中获取{}，所得内容为：{}",key,redisStrValue);
		if(!StringUtils.isEmpty(redisStrValue)) {
			try {
				result = mapper.readValue(redisStrValue,BizOutwardRequest.class);
			} catch (IOException e) {
				log.error("从缓存中获出款单id={}的信息,将内容{}转换成对象时异常",outRequestId,redisStrValue,e);
			}
		}
		return result;
	}

	/**
	 * 支付平台通知支付结果
	 *
	 * @param param
	 * @return
	 * @throws Exception
	 */
	public Map<String, Object> doOperationByPlatParam(DaifuConfirmReqParamTo param) throws Exception {
		Assert.isTrue(RESPONSE_DAIFU_CODE_SUCCESS.equals(param.getResponseDaifuCode())
				|| RESPONSE_DAIFU_CODE_ERROR.equals(param.getResponseDaifuCode()), "未知的responseDaifuCode");
		Assert.isTrue(!StringUtils.isEmpty(param.getResponseOrderID()), "responseOrderID不能为空");
		Assert.isTrue(ORDER_STATE_SUCCESS.equals(param.getResponseOrderState())
				|| ORDER_STATE_PAYING.equals(param.getResponseOrderState())
				|| ORDER_STATE_ERROR.equals(param.getResponseOrderState())
				|| ORDER_STATE_UNKOWN.equals(param.getResponseOrderState()), "未知的responseOrderState值");

		Integer handicap = getHandicapByDaifuOrderNo(param.getResponseOrderID());
		BizHandicap bizHandicap = handicapService.findFromCacheById(handicap);
		if (null == bizHandicap) {
			log.error("代付回调，代付订单{}获取的盘口id{}没有查询到对应的盘口信息", param.getResponseOrderID(), handicap);
			throw new PayPlatNotCallAgainException(String.format("未知的盘口id%s", handicap));
		}

		DaifuInfo daifuInfo = getDaifuInfoFromCache(bizHandicap.getCode(),param.getResponseOrderID());
		if(ObjectUtils.isEmpty(daifuInfo)) {
			log.info("doOperationByPlatParam从缓存中未能获取结果，改成读库获取");
			Optional<DaifuInfo> daifuInfoOptional = daifuInfoDao.findOne((root, criteriaQuery, criteriaBuilder) -> {
				List<Predicate> predicateList = new ArrayList<>();
				Path<Integer> handicapPath = root.get("handicapId");
				Path<String> channelNamePath = root.get("platPayCode");
				Predicate p1 = criteriaBuilder.equal(handicapPath, bizHandicap.getId());
				predicateList.add(p1);
				Predicate p2 = criteriaBuilder.equal(channelNamePath, param.getResponseOrderID());
				predicateList.add(p2);
				criteriaQuery.where(predicateList.toArray(new Predicate[predicateList.size()]));
				return null;
			});
			if(daifuInfoOptional.isPresent()) {
				daifuInfo = daifuInfoOptional.get();
			}
		}
		if (ObjectUtils.isEmpty(daifuInfo)) {
			log.error("支付平台通知订单支付结果，订单号 {}数据不存在", param.getResponseOrderID());
			throw new PayPlatNotCallAgainException(String.format("未知的代付订单%s", param.getResponseOrderID()));
		}

		// 记录每一次的请求参数
		daifuInfoDao.apendPayResponse(handicap, daifuInfo.getId(), mapper.writeValueAsString(param));

		// 支付平台传递 未知状态时，不再进行处理
		if (ORDER_STATE_UNKOWN.equals(param.getResponseOrderState())) {
			log.info("根据支付平台处理第三方出款，支付平台返回responseOrderState = {},不进行处理", param.getResponseOrderState());
			return null;
		}

		// `plat_status` tinyint(1) NOT NULL COMMENT '第三方处理状态 0-未知 1-处理完成 2-取消 3-正在支付',
		if (DaifuResult.ResultEnum.SUCCESS.getValue().equals(daifuInfo.getPlatStatus())) {
			daifuSubServiceImpl.toNotify(daifuInfo);
			if (!ORDER_STATE_SUCCESS.equals(param.getResponseOrderState())) {
				throw new PayPlatNotCallAgainException(
						String.format("出入款系统已经成功了，又传了一个非成功的状态[%s]过来?", param.getResponseOrderState()));
			} else {
				throw new PayPlatNotCallAgainException("出入款系统已经处理完成");
			}
		}

		if (DaifuResult.ResultEnum.ERROR.getValue().equals(daifuInfo.getPlatStatus())) {
			daifuSubServiceImpl.toNotify(daifuInfo);
			if (!ORDER_STATE_ERROR.equals(param.getResponseOrderState())) {
				throw new PayPlatNotCallAgainException(
						String.format("出入款系统已经取消了，又传了一个非取消的状态[%s]过来?", param.getResponseOrderState()));
			} else {
				throw new PayPlatNotCallAgainException("出入款系统已经取消");
			}
		}
		// 加锁进行限定
		StringRedisTemplate jedis = redisService.getStringRedisTemplate();
		if (jedis != null) {
			JedisLock lock = new JedisLock(jedis,
					String.format(DAIFU_OUT_ID_DISTRIBUTED_LOCK, daifuInfo.getOutwardRequestId()), 10000, 5000);
			try {
				if (lock.acquire()) {
					if (ORDER_STATE_ERROR.equals(param.getResponseOrderState())) {
						log.info("根据支付平台处理第三方出款，支付平台返回responseOrderState = {},需要将第三方出款单取消，使得可以重新选择第三方",
								param.getResponseOrderState());
						log.info("将第三方出款单状态改为2（取消）");
						daifuSubServiceImpl.doCancel(param, daifuInfo);
						daifuInfo2Cache(bizHandicap.getCode(), daifuInfo);
						Map<String, Object> result = new HashMap<>();
						result.put("result", 1);
						return result;
					}

					if (ORDER_STATE_PAYING.equals(param.getResponseOrderState())) {
						log.info("根据支付平台处理第三方出款，支付平台返回responseOrderState = {},第三方正在进行操作",
								param.getResponseOrderState());
						log.info("将第三方出款单状态从0（未知）改为3（第三方处理中）");
						daifuSubServiceImpl.doPaying(daifuInfo);
						daifuInfo2Cache(bizHandicap.getCode(), daifuInfo);
					}

					if (ORDER_STATE_SUCCESS.equals(param.getResponseOrderState())) {
						log.info("根据支付平台处理第三方出款，支付平台返回responseOrderState = {},第三方处理成功",
								param.getResponseOrderState());
						log.info("将第三方出款单状态改为1（处理完成）");
						daifuSubServiceImpl.doSuccess(param, daifuInfo);
						daifuInfo2Cache(bizHandicap.getCode(), daifuInfo);
						Map<String, Object> result = new HashMap<>();
						result.put("result", 1);
						return result;
					}
					return null;
				} else {
					Map<String, Object> result = new HashMap<>();
					result.put("result", 0);
					result.put("msg", "获取redis锁失败");
					return result;
				}
			} finally {
				lock.release();
			}
		} else {
			Map<String, Object> result = new HashMap<>();
			result.put("result", 0);
			result.put("msg", "获取redis锁失败");
			return result;
		}
	}

	/**
	 * 判断出款任务是否可以使用第三方代付
	 *
	 * @param handicapId
	 * @param levelId
	 * @param outwardRequestId
	 * @param orderNo
	 * @param amount
	 * @return
	 */
	public boolean isReady(BizHandicap bizHandicap, Long taskId, Long outwardRequestId, BigDecimal amount) {
		boolean result = false;
		BizOutwardRequest outward = outwardRequestDao.findOne(outwardRequestId);
		try {
			Optional<BizOutwardRequest> outwardRequestOptional = outwardRequestDao.findById(outwardRequestId);
			if(outwardRequestOptional.isPresent()) {
				result = isReady2(bizHandicap, taskId, outwardRequestOptional.get(), amount);
			}
		}catch (Exception e) {
			log.error("判断出款单是否可以使用第三方代付时异常，将返回false",e);
			result = false;
		} finally {
			OutConfigHolder.clearReadyDaifuConfigList();
		}
		return result;
	}

	/**
	 * <pre>
	 * 判断是否可用使用第三方代付:
	 * 出款单的状态
	 * 是否存在正在处理或者已经处理完成的代付订单√
	 *
	 * 层级&通道。层级单笔最小、单笔最大、单日累计
	 * 通道支持的银行
	 * 出款规则（每日，次数，重复出款，累计；免手续费次数）
	 * 通道上限下限停用金额
	 * </pre>
	 *
	 * @param bizHandicap
	 * @param outward
	 * @param amount
	 * @return
	 */
	private boolean isReady2(BizHandicap bizHandicap, Long taskId, BizOutwardRequest outward, BigDecimal amount) {
		OutConfigHolder.clearReadyDaifuConfigList();
		log.debug("线程{}判断盘口号{}的出款单Id{}是否可以使用第三方代付", Thread.currentThread().getName(), bizHandicap.getCode(),
				outward == null ? null : outward.getId());
		if (ObjectUtils.isEmpty(outward)) {
			log.debug("线程{}判断盘口号{}的出款单Id{}是否可以使用第三方代付，未能查询到对应入款单，将返回false", Thread.currentThread().getName(),
					bizHandicap.getCode(), outward.getId());
			return false;
		}

		// FIXME BLAKE 2019-04-24
		// 出款任务状态？？？

		// 层级编码
		BizLevel level = levelDao.findById2(outward.getLevel());
		if(ObjectUtils.isEmpty(level)) {
			log.debug("线程{}判断盘口号{}的出款单Id{}是否可以使用第三方代付，未知的层级id={}，将返回false",
					Thread.currentThread().getName(), bizHandicap.getCode(), outward.getId(),outward.getLevel());
			return false;
		}
		// 判断是否有可用出款通道
		List<DaifuConfigRequest> availableOutConfig = getAvailableOutConfig(bizHandicap, outward, level,
				amount);
		if (CollectionUtils.isEmpty(availableOutConfig)) {
			log.debug("线程{}判断盘口号{}的出款单Id{}是否可以使用第三方代付，无满足规则的出款通道配置，将返回false",
					Thread.currentThread().getName(), bizHandicap.getCode(), outward.getId());
			return false;
		}
		return true;
	}

	/**
	 * 根据平台出款单id获取未取消的代付订单
	 *
	 * @param handicapId
	 * @param outwardRequestId
	 * @return
	 */
	private List<DaifuInfo> getUnErrorDaifuInfoByOutRequestId(Integer handicapId, Long outwardRequestId) {
		List<DaifuInfo> result = daifuInfoDao.findAll((root, criteriaQuery, criteriaBuilder) -> {
			List<Predicate> predicateList = new ArrayList<>();
			Path<Integer> handicapPath = root.get("handicapId");
			Path<Long> outwardRequestIdPath = root.get("outwardRequestId");
			Path<Byte> platStatusPath = root.get("platStatus");
			Predicate p1 = criteriaBuilder.equal(handicapPath, handicapId);
			predicateList.add(p1);
			Predicate p2 = criteriaBuilder.equal(outwardRequestIdPath, outwardRequestId);
			predicateList.add(p2);
			Predicate p3 = criteriaBuilder.notEqual(platStatusPath, ResultEnum.ERROR.getValue());
			predicateList.add(p3);
			criteriaQuery.where(predicateList.toArray(new Predicate[predicateList.size()]));
			return null;
		});
		return result;
	}

	/**
	 * 获取可用出款通道配置
	 *
	 * <pre>
	 * 1、通道上限下限停用金额
	 * 2、通道支持的银行
	 * 3、层级&通道。层级单笔最小、单笔最大、单日累计
	 * 4、出款规则（每次出款下限/每次出款上限/每日出款上限/重复出款间隔小时数/每日出款次数/每日免收手续费次数）
	 * </pre>
	 *
	 * @param bizHandicap
	 * @param outward
	 * @param amount
	 * @return
	 */
	private List<DaifuConfigRequest> getAvailableOutConfig(BizHandicap bizHandicap, BizOutwardRequest outward,
														   BizLevel level, BigDecimal amount) {
		String handicapNotifyUrl = getNotifyUrl(bizHandicap.getCode());
		List<DaifuConfigRequest> result = new ArrayList<>();
		List<DaifuConfigRequest> activeOutConfigList = getOutConfigByActiveStatus(bizHandicap.getId());
		log.debug("线程{}获取到当前状态为启用的出款通道数量为:{}", Thread.currentThread().getName(), activeOutConfigList.size());
		if (CollectionUtils.isEmpty(activeOutConfigList)) {
			return result;
		}
		List<DaifuConfigRequest> channelBankSuitableList = getChannelBankSuitableListFromActiveOutConfigList(activeOutConfigList,outward.getToAccountBank());
		log.debug("线程{}经过{出入款系统配置该提供商是否支持用户的银行={}}过滤后的剩余出款通道数量为:{}", Thread.currentThread().getName(),
				outward.getToAccountBank(),channelBankSuitableList.size());
		if (CollectionUtils.isEmpty(channelBankSuitableList)) {
			return result;
		}
		List<DaifuConfigRequest> outConfigSuitableList = channelBankSuitableList.stream()
				// 1、通道上限下限停用金额和支付回调
				.filter(t -> isOutConfigSuitable(t.getConfig(), handicapNotifyUrl, t.getCrkOutMoney(), amount))
				.collect(Collectors.toList());
		log.debug("线程{}经过{通道上限下限停用金额和支付回调}过滤后的剩余出款通道数量为:{}", Thread.currentThread().getName(),
				outConfigSuitableList.size());
		if (CollectionUtils.isEmpty(outConfigSuitableList)) {
			return result;
		}
		List<DaifuConfigRequest> userBankNameSuitableList = outConfigSuitableList.stream()
				// 2、通道支持的银行
				.filter(t -> isUserBankSuitable(t.getBankConfig(), outward.getToAccountBank()))
				.collect(Collectors.toList());
		log.debug("线程{}经过{通道支持的银行-用户银行{}}过滤后的剩余出款通道数量为:{}", Thread.currentThread().getName(),
				outward.getToAccountBank(), userBankNameSuitableList.size());
		if (CollectionUtils.isEmpty(userBankNameSuitableList)) {
			return result;
		}
		List<Integer> userBankNameSuitableIdList = userBankNameSuitableList.stream().map(DaifuConfigRequest::getId)
				.collect(Collectors.toList());
		// 获取通道层级今日累计
		final Map<Integer, BigDecimal> levelTodayOutTotal = new HashMap<>();

		// 会员今日累计
		// 会员今日使用出款通道次数
		final Map<Integer, Integer> userTodayOutCount = new HashMap<>();
		// 会员今日最近一次使用出款通道的时间
		final Map<Integer, Timestamp> userTodayOutLastOutTime = new HashMap<>();
		// 会员今日使用出款通道累计出款金额
		final Map<Integer, BigDecimal> userTodayOutTotal = new HashMap<>();

		List<DaifuInfo> todayLevelAllOut = getTodayLevelAllOutByOutConfigIdList(bizHandicap.getId(), level.getId(),
				userBankNameSuitableIdList);
		if (!CollectionUtils.isEmpty(todayLevelAllOut)) {
			todayLevelAllOut.stream().forEach(t -> {
				BigDecimal tmp = levelTodayOutTotal.get(t.getDaifuConfigId());
				if (ObjectUtils.isEmpty(tmp)) {
					tmp = BigDecimal.ZERO;
				}
				levelTodayOutTotal.put(t.getDaifuConfigId(), tmp.add(t.getExactMoney()));
				if (!StringUtils.isEmpty(t.getUserName()) && t.getUserName().equals(outward.getMember())) {
					// 会员今日使用通道次数
					Integer tmp2 = userTodayOutCount.get(t.getDaifuConfigId());
					if (ObjectUtils.isEmpty(tmp2)) {
						tmp2 = 0;
					}
					userTodayOutCount.put(t.getDaifuConfigId(), tmp2 + 1);

					// 会员今日使用通道累计出款金额
					BigDecimal tmp3 = userTodayOutTotal.get(t.getDaifuConfigId());
					if (ObjectUtils.isEmpty(tmp3)) {
						tmp3 = BigDecimal.ZERO;
					}
					userTodayOutTotal.put(t.getDaifuConfigId(), tmp3.add(t.getExactMoney()));

					// 会员最近一次使用该出款的时间
					Timestamp lastUseTime = userTodayOutLastOutTime.get(t.getDaifuConfigId());
					if (!ObjectUtils.isEmpty(t.getCreateTime())
							&& (ObjectUtils.isEmpty(lastUseTime) || lastUseTime.before(t.getCreateTime()))) {
						userTodayOutLastOutTime.put(t.getDaifuConfigId(), t.getCreateTime());
					}
				}
			});
		}

		List<DaifuConfigRequest> levelSuitableList = userBankNameSuitableList.stream()
				// 3、层级&通道。层级单笔最小、单笔最大、单日累计
				.filter(t -> isLevelSuitable(t.getLevelConfig(), level.getCode(), amount,
						levelTodayOutTotal.get(t.getId())))
				.collect(Collectors.toList());
		log.debug("线程{}经过{层级&通道。层级单笔最小、单笔最大、单日累计}过滤后的剩余出款通道数量为:{}", Thread.currentThread().getName(),
				levelSuitableList.size());
		if (CollectionUtils.isEmpty(levelSuitableList)) {
			return result;
		}

		// 4、出款规则（每次出款下限/每次出款上限/每日出款上限/重复出款间隔小时数/每日出款次数/每日免收手续费次数）
		List<DaifuConfigRequest> outConfigSetSuitableList = levelSuitableList.stream()
				.filter(t -> isOutSetSuitable(t.getOutConfigSet(), amount, userTodayOutCount.get(t.getId()),
						userTodayOutTotal.get(t.getId()), userTodayOutLastOutTime.get(t.getId())))
				.collect(Collectors.toList());
		log.debug("线程{}经过{出款规则（每次出款下限/每次出款上限/每日出款上限/重复出款间隔小时数/每日出款次数}过滤后的剩余出款通道数量为:{}",
				Thread.currentThread().getName(), outConfigSetSuitableList.size());
		if (CollectionUtils.isEmpty(outConfigSetSuitableList)) {
			return result;
		}
		result.addAll(outConfigSetSuitableList);
		Map<Integer, Integer> userTodayOutCount2 =new HashMap<>();
		result.stream().forEach(t->{
			Integer tmp = userTodayOutCount.get(t.getId());
			if(ObjectUtils.isEmpty(tmp)) {
				tmp = 0;
			}
			userTodayOutCount2.put(t.getId(), tmp);
		});
		OutConfigHolder.setReadyDaifuConfigList(result,userTodayOutCount2);
		return result;
	}

	/**
	 * 出款类型，约定：0-微信 1-支付宝 2-银行<br>
	 * 目前第三方都仅支付银行卡出款，所以这里先取 2
	 */
	private static final String OUT_TYPE_BANK ="2";
	/**
	 * 判断出款规则是否适用
	 *
	 * @param outSetConfigStr
	 * @param amount
	 * @param useCount
	 * @param total
	 * @param lastUseTime
	 * @return
	 */
	private boolean isOutSetSuitable(String outSetConfigStr, BigDecimal amount, Integer useCount, BigDecimal total,
									 Timestamp lastUseTime) {
		Map<String, DaifuConfigSyncReqParamOutConfig> outSetConfigMap = null;
		if (!StringUtils.isEmpty(outSetConfigStr)) {
			outSetConfigMap = getOutSetConfigMap(outSetConfigStr);
		}
		if (ObjectUtils.isEmpty(outSetConfigMap) || ObjectUtils.isEmpty(outSetConfigMap.get(OUT_TYPE_BANK))) {
			return false;
		}

		DaifuConfigSyncReqParamOutConfig outConfig = outSetConfigMap.get(OUT_TYPE_BANK);
		// 会员每日累计出款金额
		if (!ObjectUtils.isEmpty(total) && !ObjectUtils.isEmpty(outConfig.getDayMax())
				&& outConfig.getDayMax().compareTo(BigDecimal.ZERO) > 0 && total.compareTo(outConfig.getDayMax()) > 0) {
			return false;
		}
		// 会员每日累计出款次数
		if (!ObjectUtils.isEmpty(useCount) && !ObjectUtils.isEmpty(outConfig.getOutMax())
				&& outConfig.getOutMax().compareTo(0) > 0 && useCount.compareTo(outConfig.getOutMax()) > 0) {
			return false;
		}

		// 会员每次最大
		outConfig.getOnceMax();
		if (!ObjectUtils.isEmpty(outConfig.getOnceMax()) && outConfig.getOnceMax().compareTo(BigDecimal.ZERO) > 0
				&& amount.compareTo(outConfig.getOnceMax()) > 0) {
			return false;
		}
		// 会员每次出款最小
		if (!ObjectUtils.isEmpty(outConfig.getOnceMin()) && outConfig.getOnceMin().compareTo(BigDecimal.ZERO) > 0
				&& amount.compareTo(outConfig.getOnceMin()) > 0) {
			return false;
		}

		// 时间间隔
		if (!ObjectUtils.isEmpty(lastUseTime) && !ObjectUtils.isEmpty(outConfig.getTimeLimit())
				&& outConfig.getTimeLimit().compareTo(0) > 0) {
			Calendar now = Calendar.getInstance();
			now.add(Calendar.HOUR_OF_DAY, -outConfig.getTimeLimit());
			if (lastUseTime.after(now.getTime())) {
				return false;
			}
		}
		return true;
	}

	/**
	 *
	 * @param outSetConfigStr
	 * @return
	 */
	private Map<String, DaifuConfigSyncReqParamOutConfig> getOutSetConfigMap(String outSetConfigStr) {
		Map<String, DaifuConfigSyncReqParamOutConfig> outSetConfigMap = null;
		try {
			outSetConfigMap = mapper.readValue(outSetConfigStr, mapper.getTypeFactory().constructMapType(HashMap.class,
					String.class, DaifuConfigSyncReqParamOutConfig.class));
		} catch (Exception e) {
			log.error("线程{}将通道 outSetConfigStr={}转换为Map<String,DaifuConfigSyncReqParamOutConfig>对象时异常",
					Thread.currentThread().getName(), outSetConfigStr, e);
		}
		if(outSetConfigMap ==null) {
			outSetConfigMap = new HashMap<>();
		}
		return outSetConfigMap;
	}

	/**
	 * 根据出款通道id查询层级今日出款数据
	 *
	 * @param handicapId
	 * @param levelId
	 * @param outConfigIdList
	 * @return
	 */
	private List<DaifuInfo> getTodayLevelAllOutByOutConfigIdList(Integer handicapId, Integer levelId,
																 List<Integer> outConfigIdList) {
		List<DaifuInfo> result = daifuInfoDao.findAll((root, criteriaQuery, criteriaBuilder) -> {
			List<Predicate> predicateList = new ArrayList<>();
			Path<Integer> handicapPath = root.get("handicapId");
			Path<Integer> daifuConfigIdPath = root.get("daifuConfigId");
			Path<Integer> levelPath = root.get("level");
			Path<Byte> platStatusPath = root.get("platStatus");
			Path<Timestamp> createTimePath = root.get("createTime");
			Predicate p1 = criteriaBuilder.equal(handicapPath, handicapId);
			predicateList.add(p1);
			Predicate p2 = criteriaBuilder.equal(platStatusPath, ONE);
			predicateList.add(p2);
			Predicate p3 = criteriaBuilder.equal(levelPath, levelId);
			predicateList.add(p3);
			Predicate p4 = criteriaBuilder.equal(platStatusPath, ResultEnum.SUCCESS.getValue());
			predicateList.add(p4);
			if (!CollectionUtils.isEmpty(outConfigIdList)) {
				// 通道id
				Predicate[] restrictions = new Predicate[outConfigIdList.size()];
				for (int i = 0; i < outConfigIdList.size(); i++) {
					Integer outConfigId = outConfigIdList.get(i);
					restrictions[i] = criteriaBuilder.equal(daifuConfigIdPath, outConfigId);
				}
				predicateList.add(criteriaBuilder.or(restrictions));
			}
			// 创建时间,限制今天
			Calendar now = Calendar.getInstance();
			predicateList.add(criteriaBuilder.lessThanOrEqualTo(createTimePath, new Timestamp(now.getTimeInMillis())));
			now.set(Calendar.HOUR_OF_DAY, 0);
			now.set(Calendar.MINUTE, 0);
			now.set(Calendar.SECOND, 0);
			now.set(Calendar.MILLISECOND, 0);
			predicateList
					.add(criteriaBuilder.greaterThanOrEqualTo(createTimePath, new Timestamp(now.getTimeInMillis())));
			criteriaQuery.where(predicateList.toArray(new Predicate[predicateList.size()]));
			return null;
		});
		return result;
	}

	/**
	 * 判断层级是否适用<br>
	 *
	 * @param levelConfig
	 * @param levelCode
	 * @param amount
	 * @param todayTotal  该通道该层级今日累计出款金额
	 * @return
	 */
	private boolean isLevelSuitable(String levelConfigStr, String levelCode, BigDecimal amount, BigDecimal todayTotal) {
		if (todayTotal == null) {
			todayTotal = BigDecimal.ZERO;
		}
		Map<String, DaifuConfigSyncReqParamLevelConfig> levelConfigMap = null;
		try {
			levelConfigMap = mapper.readValue(levelConfigStr, mapper.getTypeFactory().constructMapType(HashMap.class,
					String.class, DaifuConfigSyncReqParamLevelConfig.class));
		} catch (Exception e) {
			log.error("线程{}将通道 levelConfigStr={}转换为Map<String,DaifuConfigSyncReqParamLevelConfig>对象时异常",
					Thread.currentThread().getName(), levelConfigStr, e);
		}
		DaifuConfigSyncReqParamLevelConfig levelConfig = null;
		if (!ObjectUtils.isEmpty(levelConfigMap)) {
			levelConfig = levelConfigMap.get(levelCode);
		}
		boolean result = !ObjectUtils.isEmpty(levelConfig)
				&& (levelConfig.getOutDayMax() == null || BigDecimal.ZERO.equals(levelConfig.getOutDayMax())
				|| levelConfig.getOutDayMax().compareTo(amount.add(todayTotal)) >= 0)
				&& (levelConfig.getOutOnceMin() == null || amount.compareTo(levelConfig.getOutOnceMin()) >= 0)
				&& (levelConfig.getOutOnceMax() == null || BigDecimal.ZERO.equals(levelConfig.getOutOnceMax())
				|| amount.compareTo(levelConfig.getOutOnceMax()) <= 0);
		return result;
	}

	/**
	 * 判断通道配置是否适用<br>
	 * 包括最大最小停用金额和回调地址
	 *
	 * @param configStr
	 * @return
	 */
	private boolean isOutConfigSuitable(String configStr, String handicapNotifyUrl, BigDecimal outMoney,
										BigDecimal amount) {
		if (ObjectUtils.isEmpty(outMoney)) {
			outMoney = BigDecimal.ZERO;
		}
		DaifuConfigSyncReqParamConfig config = null;
		try {
			config = mapper.readValue(configStr, DaifuConfigSyncReqParamConfig.class);
		} catch (Exception e) {
			log.error("线程{}将通道 configStr={}转换为DaifuConfigSyncReqParamConfig对象时异常", Thread.currentThread().getName(),
					configStr, e);
		}
		if (!ObjectUtils.isEmpty(config)) {
			if (config.getStoreStopMoney() != null && config.getStoreStopMoney().compareTo(BigDecimal.ZERO) > 0
					&& config.getStoreStopMoney().compareTo(outMoney) <= 0) {
				return false;
			}
			if (config.getStoreMinMoney() != null && config.getStoreMinMoney().compareTo(BigDecimal.ZERO) > 0
					&& config.getStoreMinMoney().compareTo(amount) > 0) {
				return false;
			}
			if (config.getStoreMaxMoney() != null && config.getStoreMaxMoney().compareTo(BigDecimal.ZERO) > 0
					&& config.getStoreMaxMoney().compareTo(amount) < 0) {
				return false;
			}
			return !StringUtils.isEmpty(handicapNotifyUrl) || !StringUtils.isEmpty(config.getNotifyUrl());
		} else {
			return !StringUtils.isEmpty(handicapNotifyUrl);
		}
	}

	/**
	 * 判断通道是否支持用户的出款银行
	 *
	 * @param bankConfig
	 * @param userBank
	 * @return
	 */
	private boolean isUserBankSuitable(String bankConfig, String userBank) {
		if (StringUtils.isEmpty(bankConfig)) {
			return false;
		}
		return !StringUtils.isEmpty(getOutConfigChannelName4BankName(bankConfig, userBank));
	}

	/**
	 * 出入款系统配置该提供商是否支持用户的银行
	 * @return
	 */
	private List<DaifuConfigRequest> getChannelBankSuitableListFromActiveOutConfigList(List<DaifuConfigRequest> activeOutConfigList,String userBank) {
		List<DaifuConfigRequest> result = new ArrayList<DaifuConfigRequest>();
		if(CollectionUtils.isEmpty(activeOutConfigList)||StringUtils.isEmpty(userBank)) {
			return result;
		}
		Set<String> channelNameSet = activeOutConfigList.stream().map(DaifuConfigRequest::getChannelName)
				.collect(Collectors.toSet());
		Set<String> supportUserBankChannelNameSet = new HashSet<String>();
		for(String cn:channelNameSet) {
			if(supportUserBankChannelNameSet.contains(cn)) {
				continue;
			}
			Set<String> cnSupportBankSet = daifuCacheUtil.getChannelSupportBankFromCache(cn);
			if(!ObjectUtils.isEmpty(cnSupportBankSet) && cnSupportBankSet.contains(userBank)) {
				supportUserBankChannelNameSet.add(cn);
			}
		}
		result.addAll(activeOutConfigList.stream().filter(t->supportUserBankChannelNameSet.contains(t.getChannelName())).collect(Collectors.toList()));
		return result;
	}

	private HashMap<String, String> getOutConfigSupportBank(String bankConfig) {
		HashMap<String, String> bankConfigMap = null;
		try {
			bankConfigMap = mapper.readValue(bankConfig,
					mapper.getTypeFactory().constructMapType(HashMap.class, String.class, String.class));
		} catch (IOException e) {
			log.error("线程{}将通道 bankConfig={}转换为HashMap对象时异常", Thread.currentThread().getName(), bankConfig, e);
		}
		if (bankConfigMap == null) {
			bankConfigMap = new HashMap<>();
		}
		return bankConfigMap;
	}

	/**
	 * 获取通道对用户出款银行的 payCoreChannelName
	 *
	 * @param bankConfig
	 * @param userBank
	 * @return
	 */
	private String getOutConfigChannelName4BankName(String bankConfig, String userBank) {
		Map<String, String> bankConfigMap = this.getOutConfigSupportBank(bankConfig);
		String result = bankConfigMap.get(SUPPORT_ALL_BANK);
		if (StringUtils.isEmpty(result)) {
			result = bankConfigMap.get(userBank);
		}
		return result;
	}

	private DaifuConfigSyncReqParamFeeConfig getOutConfigFeeConfig(String feeConfig) {
		DaifuConfigSyncReqParamFeeConfig result = null;
		if (!StringUtils.isEmpty(feeConfig)) {
			try {
				result = mapper.readValue(feeConfig, DaifuConfigSyncReqParamFeeConfig.class);
			} catch (IOException e) {
				log.error("线程{}获取出款通道手续费配置时异常", Thread.currentThread().getName(), e);
			}
		}
		return result;
	}

	/**
	 * 获取启用的出款通道
	 *
	 * @param bizHandicap
	 * @return
	 */
	private List<DaifuConfigRequest> getOutConfigByActiveStatus(Integer bizHandicapId) {
		List<DaifuConfigRequest> result = daifuConfigDao.findAll((root, criteriaQuery, criteriaBuilder) -> {
			List<Predicate> predicateList = new ArrayList<>();
			Path<Integer> handicapPath = root.get("handicapId");
			Path<Byte> platformStatusPath = root.get("platformStatus");
			Path<Byte> platformDeleteFlagPath = root.get("platformDeleteFlag");
			Path<Byte> crkStatusPath = root.get("crkStatus");
			Predicate p1 = criteriaBuilder.equal(handicapPath, bizHandicapId);
			predicateList.add(p1);
			Predicate p2 = criteriaBuilder.equal(platformStatusPath, ONE);
			predicateList.add(p2);
			Predicate p3 = criteriaBuilder.equal(platformDeleteFlagPath, ZERO);
			predicateList.add(p3);
			Predicate p4 = criteriaBuilder.equal(crkStatusPath, ONE);
			predicateList.add(p4);
			criteriaQuery.where(predicateList.toArray(new Predicate[predicateList.size()]));
			return null;
		});
		return result;
	}

	/**
	 * 干预第三方代付结果，通常用于第三方完成处理（包括取消或者成功）后无法正常回调<br>
	 * 当第三方取消时，将代付订单状态设置为 2-取消<br>
	 * 当第三方正在处理时，将代付订单状态设置为 3-正在支付<br>
	 * 当第三方成功支付出款时，将代付订单状态设置为 1-处理完成，并且增加出款通道的<br>
	 *
	 * @param handicapId       盘口号
	 * @param operatorId       操作人id
	 * @param outwardRequestId 出款订单号id
	 * @param daifuResult      第三方代付结果
	 * @return
	 */
	@Transactional(rollbackOn = Exception.class, value = TxType.REQUIRED)
	public DaifuResult interveneByResultEnum(BizHandicap bizHandicap, Integer operatorId, Long outwardRequestId,
											 ResultEnum daifuResult) {
		DaifuResult result = null;
		StringRedisTemplate jedis = redisService.getStringRedisTemplate();
		if (jedis == null) {
			log.debug("线程{}判断盘口号{}的出款单Id{}是否可以使用第三方代付，没有获取到jedis", Thread.currentThread().getName(),
					bizHandicap.getCode(), outwardRequestId);
			result = new DaifuResult();
			result.setHandicapId(bizHandicap.getId());
			result.setHandicapCode(bizHandicap.getCode());
			result.setResult(ResultEnum.PROC_EXCEPTION);
			return result;
		}
		JedisLock lock = new JedisLock(jedis, String.format(DAIFU_OUT_ID_DISTRIBUTED_LOCK, outwardRequestId), 10000,
				5000);
		try {
			if (lock.acquire()) {
				BizOutwardRequest outward = null;
				Optional<BizOutwardRequest> outwardOptional = outwardRequestDao.findById(outwardRequestId);
				if (!outwardOptional.isPresent()) {
					log.debug("线程{}干预盘口{}出款单{}的代付结果，未知的出款单，返回出款单异常", Thread.currentThread().getName(),
							bizHandicap.getCode(), outwardRequestId);
					result = new DaifuResult();
					result.setHandicapId(bizHandicap.getId());
					result.setResult(ResultEnum.NO_OUT_REQUEST);
					result.setErrorMsg("未知的出款单id");
					return result;
				}
				outward= outwardOptional.get();
				// 查询该出款单最近的代付订单信息
				List<DaifuInfo> daifuInfoList = daifuInfoDao.findAll((root, criteriaQuery, criteriaBuilder) -> {
					List<Predicate> predicateList = new ArrayList<>();
					Path<Integer> handicapPath = root.get("handicapId");
					Path<Long> outwardRequestIdPath = root.get("outwardRequestId");
					Predicate p1 = criteriaBuilder.equal(handicapPath, bizHandicap.getId());
					predicateList.add(p1);
					Predicate p2 = criteriaBuilder.equal(outwardRequestIdPath, outwardRequestId);
					predicateList.add(p2);
					criteriaQuery.where(predicateList.toArray(new Predicate[predicateList.size()]));
					return null;
				}, new Sort(Direction.DESC, "createTime"));

				if (CollectionUtils.isEmpty(daifuInfoList)) {
					log.debug("线程{}干预盘口{}出款单{}的代付结果，根据出款单id没有查询到代付信息，返回代付订单异常", Thread.currentThread().getName(),
							bizHandicap.getCode(), outwardRequestId);
					result = new DaifuResult();
					result.setHandicapId(bizHandicap.getId());
					result.setOutwardTaskOrderNo(outward.getOrderNo());
					result.setResult(ResultEnum.NO_DAIFU_INFO);
					result.setErrorMsg("该出款单未构建代付订单");
					return result;
				} else {
					DaifuInfo daifuInfo = daifuInfoList.get(0);
					result = daifuInfo.toResult4Outward();
					// 如果表中已经记录为支付完成或者已经取消，不能再进行干预
					if (ResultEnum.SUCCESS.getValue().equals(daifuInfo.getPlatStatus())) {
						result.setResult(ResultEnum.SUCCESS);
					} else if (ResultEnum.ERROR.getValue().equals(daifuInfo.getPlatStatus())) {
						result.setResult(ResultEnum.ERROR);
					} else {
						daifuInfo.setPlatStatus(daifuResult.getValue());
						daifuInfo.setRemark(
								String.format("operator=%s 干预处理结果，将结果设置为%s", operatorId, daifuResult.getDesc()));
						daifuInfo.setUptime(new Timestamp(System.currentTimeMillis()));
						daifuInfo.setInterveneAdminId(operatorId);
						daifuInfo.setCreateTime(new Timestamp(System.currentTimeMillis()));
						daifuInfoDao.save(daifuInfo);
						daifuSubServiceImpl.toNotify(daifuInfo);
						// 如果干预结果为第三方处理完成，增加出款通道累计值
						if (ResultEnum.SUCCESS.equals(daifuResult)) {
							daifuConfigDao.addStaticByDaifuSuccess(bizHandicap.getId(),
									daifuInfo.getDaifuConfigId(), daifuInfo.getExactMoney());
						}
					}
				}
			} else {
				log.error("线程{}干预盘口{}出款单{}的代付结果，未能获取到jedisLock，需要重试", Thread.currentThread().getName(),
						bizHandicap.getCode(), outwardRequestId);
				result = new DaifuResult();
				result.setHandicapId(bizHandicap.getId());
				result.setHandicapCode(bizHandicap.getCode());
				result.setResult(ResultEnum.PROC_EXCEPTION);
				return result;
			}
		} catch (Exception e) {
			log.error("线程{}干预盘口{}出款单{}的代付结果，产生异常，需要重试", Thread.currentThread().getName(), bizHandicap.getCode(),
					outwardRequestId);
			result = new DaifuResult();
			result.setHandicapId(bizHandicap.getId());
			result.setHandicapCode(bizHandicap.getCode());
			result.setResult(ResultEnum.PROC_EXCEPTION);
			return result;
		}
		return result;
	}

	/**
	 * 查找出款代付结果
	 *
	 * @param id
	 * @param outwardRequestId
	 * @return
	 */
	public DaifuResult getDaifuResult(BizHandicap bizHandicap, Long outwardRequestId) {
		log.debug("线程{}获取盘口{}出款单{}的代付结果", Thread.currentThread().getName(), bizHandicap.getCode(), outwardRequestId);
		DaifuResult result = null;
		BizOutwardRequest outward = null;
		Optional<BizOutwardRequest> outwardOptional = outwardRequestDao.findById(outwardRequestId);
		if (!outwardOptional.isPresent()) {
			log.debug("线程{}获取盘口{}出款单{}的代付结果，未知的出款单，返回出款单异常", Thread.currentThread().getName(), bizHandicap.getCode(),
					outwardRequestId);
			result = new DaifuResult();
			result.setHandicapId(bizHandicap.getId());
			result.setResult(ResultEnum.NO_OUT_REQUEST);
			return result;
		}else {
			outward = outwardOptional.get();
		}
		// 查询该出款单最近的代付订单信息
		List<DaifuInfo> daifuInfoList = daifuInfoDao.findAll((root, criteriaQuery, criteriaBuilder) -> {
			List<Predicate> predicateList = new ArrayList<>();
			Path<Integer> handicapPath = root.get("handicapId");
			Path<Long> outwardRequestIdPath = root.get("outwardRequestId");
			Predicate p1 = criteriaBuilder.equal(handicapPath, bizHandicap.getId());
			predicateList.add(p1);
			Predicate p2 = criteriaBuilder.equal(outwardRequestIdPath, outwardRequestId);
			predicateList.add(p2);
			criteriaQuery.where(predicateList.toArray(new Predicate[predicateList.size()]));
			return null;
		}, new Sort(Direction.DESC, "createTime"));

		if (CollectionUtils.isEmpty(daifuInfoList)) {
			log.debug("线程{}获取盘口{}出款单{}的代付结果，根据出款单id没有查询到代付信息，返回代付订单异常", Thread.currentThread().getName(),
					bizHandicap.getCode(), outwardRequestId);
			result = new DaifuResult();
			result.setHandicapId(bizHandicap.getId());
			result.setOutwardTaskOrderNo(outward.getOrderNo());
			result.setResult(ResultEnum.NO_DAIFU_INFO);
			result.setErrorMsg("该出款单未构建代付订单");
			return result;
		} else {
			DaifuInfo daifuInfo = daifuInfoList.get(0);
			result = daifuInfo.toResult4Outward();
			// 如果表中已经记录为第三方处理完成（包括支付完成和取消），则直接返回表中的状态
			if (ResultEnum.SUCCESS.getValue().equals(daifuInfo.getPlatStatus()) || ResultEnum.ERROR.getValue().equals(daifuInfo.getPlatStatus())) {
				return result;
			}else {
				daifuInfo2Cache(bizHandicap.getCode(), daifuInfo);
				// 如果表中状态不是支付完成。发送请求到 payCore 查询
				Crk2PayCoreQueryResponse payCoreResult = daifuExtService.queryDaifuResult(bizHandicap.getCode(),
						daifuInfo.getPlatPayCode(), new HashMap<>());
				ResultEnum resultEnum = payCoreOrderState2ResultEnum(payCoreResult.getResponseDaifuCode(),
						payCoreResult.getResponseOrderState());
				if(!ResultEnum.UNKOWN.equals(resultEnum)) {
					result.setResult(resultEnum);
				}
				if((ResultEnum.UNKOWN.equals(result.getResult()) || ResultEnum.PAYING.equals(result.getResult()))
						&& daifuSubServiceImpl.isToIntervene(result, daifuInfo)) {
					log.info("盘口{}代付订单{}满足转排查条件",bizHandicap.getCode(),daifuInfo.getPlatPayCode());
					result.setResult(ResultEnum.TO_INTERVENE);
				}
			}
		}
		return result;
	}

	/**
	 * 根据请求代付结果调用 doOperationByPlatParam 进行处理
	 * @param sendResponse
	 */
	private void asyncDoOperationByPlatParam(Crk2PayCoreSendResponse sendResponse) {
		//如果是未知状态的，则不进行异步处理
		if(StringUtils.isEmpty(sendResponse.getRequestDaifuOrderState())
				||ORDER_STATE_UNKOWN.equals(sendResponse.getRequestDaifuOrderState())) {
			return;
		}
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					DaifuConfirmReqParamTo paramTo = crk2PayCoreSendResponse2ReqParamTo(sendResponse);
					log.debug("线程：{}根据请求代付结果异步处理，请求代付结果：{}",Thread.currentThread().getName(),ObjectMapperUtils.serialize(sendResponse));
					doOperationByPlatParam(paramTo);
					log.debug("线程：{}根据请求代付结果异步处理，完成处理",Thread.currentThread().getName());
				} catch (Exception e) {
					log.error("线程：{}根据请求代付结果异步处理时异常",Thread.currentThread().getName(),e);
				}
			}
		}).start();
	}

	private DaifuConfirmReqParamTo crk2PayCoreSendResponse2ReqParamTo(Crk2PayCoreSendResponse payCoreQueryResult) {
		DaifuConfirmReqParamTo result = new DaifuConfirmReqParamTo();
		result.setResponseDaifuAmount(payCoreQueryResult.getRequestDaifuAmount());
		result.setResponseDaifuChannel(payCoreQueryResult.getRequestDaifuChannelBankName());
		result.setResponseDaifuCode(payCoreQueryResult.getRequestDaifuCode());
		result.setResponseDaifuErrorMsg(payCoreQueryResult.getRequestDaifuErrorMsg());
		//result.setResponseDaifuMemberId(payCoreQueryResult.get);
		//result.setResponseDaifuMsg(payCoreQueryResult.getResponseDaifuMsg());
		result.setResponseDaifuOid(payCoreQueryResult.getRequestDaifuOid());
		result.setResponseDaifuOrderCreateTime(payCoreQueryResult.getRequestDaifuOrderCreateTime());
		result.setResponseDaifuOtherParam(payCoreQueryResult.getRequestDaifuOtherParam());
		//result.setResponseDaifuSign(payCoreQueryResult.getResponseDaifuSign());
		result.setResponseDaifuTotalTime(payCoreQueryResult.getRequestDaifuTotalTime());
		result.setResponseOrderID(payCoreQueryResult.getRequestDaifuOrderId());
		result.setResponseOrderState(payCoreQueryResult.getRequestDaifuOrderState());
		return result;
	}

	/**
	 * 根据查询代付结果调用 doOperationByPlatParam 进行处理
	 * @param payCoreResult
	 */
	private void asyncDoOperationByPlatParam(Crk2PayCoreQueryResponse payCoreQueryResult) {
		//如果是未知状态的，则不进行异步处理
		if(StringUtils.isEmpty(payCoreQueryResult.getResponseOrderState())
				||ORDER_STATE_UNKOWN.equals(payCoreQueryResult.getResponseOrderState())) {
			return;
		}
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					DaifuConfirmReqParamTo paramTo = crk2PayCoreQueryResponse2ReqParamTo(payCoreQueryResult);
					log.debug("线程：{}根据查询结果异步处理，查询结果：{}", Thread.currentThread().getName(),
							ObjectMapperUtils.serialize(payCoreQueryResult));
					doOperationByPlatParam(paramTo);
					log.debug("线程：{}根据查询结果异步处理完成", Thread.currentThread().getName());
				} catch (Exception e) {
					log.error("线程：{}根据查询结果异步处理时异常", Thread.currentThread().getName(), e);
				}
			}
		}).start();
	}

	private DaifuConfirmReqParamTo crk2PayCoreQueryResponse2ReqParamTo(Crk2PayCoreQueryResponse payCoreQueryResult) {
		DaifuConfirmReqParamTo result = new DaifuConfirmReqParamTo();
		result.setResponseDaifuAmount(payCoreQueryResult.getResponseDaifuAmount());
		result.setResponseDaifuChannel(payCoreQueryResult.getResponseDaifuChannel());
		result.setResponseDaifuCode(payCoreQueryResult.getResponseDaifuCode());
		result.setResponseDaifuErrorMsg(payCoreQueryResult.getResponseDaifuErrorMsg());
		result.setResponseDaifuMemberId(payCoreQueryResult.getResponseDaifuMemberId());
		result.setResponseDaifuMsg(payCoreQueryResult.getResponseDaifuMsg());
		result.setResponseDaifuOid(payCoreQueryResult.getResponseDaifuOid());
		result.setResponseDaifuOrderCreateTime(payCoreQueryResult.getResponseDaifuOrderCreateTime());
		result.setResponseDaifuOtherParam(payCoreQueryResult.getResponseDaifuOtherParam());
		result.setResponseDaifuSign(payCoreQueryResult.getResponseDaifuSign());
		result.setResponseDaifuTotalTime(payCoreQueryResult.getResponseDaifuTotalTime());
		result.setResponseOrderID(payCoreQueryResult.getResponseOrderID());
		result.setResponseOrderState(payCoreQueryResult.getResponseOrderState());
		return result;
	}

	/**
	 * 支付平台返回代付订单状态转成 ResultEnum
	 *
	 * @param payCoreOrderState
	 * @return
	 */
	private ResultEnum payCoreOrderState2ResultEnum(String payCoreDaifuCode, String payCoreOrderState) {
		ResultEnum resultEnum = null;
		if (RESPONSE_DAIFU_CODE_SUCCESS.equals(payCoreDaifuCode)) {
			if (ORDER_STATE_SUCCESS.equals(payCoreOrderState)) {
				resultEnum = ResultEnum.SUCCESS;
			}
			if (ORDER_STATE_PAYING.equals(payCoreOrderState)) {
				resultEnum = ResultEnum.PAYING;
			}
			if (ORDER_STATE_ERROR.equals(payCoreOrderState)) {
				resultEnum = ResultEnum.ERROR;
			}
			if (ORDER_STATE_UNKOWN.equals(payCoreOrderState)) {
				resultEnum = ResultEnum.UNKOWN;
			}
		} else {
			resultEnum = ResultEnum.ERROR;
		}

		return resultEnum;
	}

	/**
	 *
	 * @param bizHandicap
	 * @param operator
	 * @param outwardRequestId
	 * @param amount
	 * @return
	 */
	public DaifuResult genDaifuInfoAndSend2PayCore(BizHandicap bizHandicap, Integer operator, Long outwardRequestId,
												   Long outwardTaskId, BigDecimal amount) {
		BizOutwardRequest outward = null;
		Optional<BizOutwardRequest> outwardOptional = outwardRequestDao.findById(outwardRequestId);
		DaifuResult result = null;
		log.debug("线程{}盘口号{}的出款单Id{}尝试使用第三方代付", Thread.currentThread().getName(), bizHandicap.getCode(),
				outwardRequestId);
		if (!outwardOptional.isPresent()) {
			log.debug("线程{}盘口号{}的出款单Id{}尝试使用第三方代付，未能查询到对应入款单，将返回出款单异常", Thread.currentThread().getName(),
					bizHandicap.getCode(), outwardRequestId);
			result = new DaifuResult();
			result.setResult(ResultEnum.NO_OUT_REQUEST);
			result.setErrorMsg("未知的出款单id");
			result.setHandicapId(bizHandicap.getId());
			result.setHandicapCode(bizHandicap.getCode());
			return result;
		}else {
			outward = outwardOptional.get();
		}
		StringRedisTemplate jedis = redisService.getStringRedisTemplate();
		if (jedis == null) {
			log.debug("线程{}盘口号{}的出款单Id{}尝试使用第三方代付，没有获取到jedis，将返回程序处理异常", Thread.currentThread().getName(),
					bizHandicap.getCode(), outwardRequestId);
			result = new DaifuResult();
			result.setResult(ResultEnum.PROC_EXCEPTION);
			result.setErrorMsg("获取redis失败");
			result.setHandicapId(bizHandicap.getId());
			result.setHandicapCode(bizHandicap.getCode());
			return result;
		}

		try {
			JedisLock lock = new JedisLock(jedis, String.format(DAIFU_OUT_ID_DISTRIBUTED_LOCK, outwardRequestId), 10000,
					5000);
			try {
				if (lock.acquire()) {
					boolean readyFlag = isReady2(bizHandicap, outwardTaskId, outward, amount);
					if (!readyFlag) {
						log.debug("线程{}盘口号{}的出款单Id{}尝试使用第三方代付，不满足适用第三方出款的条件，将返回不满足代付条件",
								Thread.currentThread().getName(), bizHandicap.getCode(), outwardRequestId);
						result = new DaifuResult();
						result.setResult(ResultEnum.NO_READ);
						result.setErrorMsg("不满足适用第三方出款的条件");
						result.setHandicapId(bizHandicap.getId());
						result.setHandicapCode(bizHandicap.getCode());
						return result;
					}
					// 判断该出款单是否有正在处理 或者 已经完成了 的代付订单
					List<DaifuInfo> unErrorDaifuInfoList = getUnErrorDaifuInfoByOutRequestId(bizHandicap.getId(),
							outward.getId());
					if (!CollectionUtils.isEmpty(unErrorDaifuInfoList)) {
						log.debug("线程{}盘口号{}的出款单Id{}尝试使用第三方代付，存在正在处理或者已经完成出款的代付订单，将直接返回现有代付订单的结果",
								Thread.currentThread().getName(), bizHandicap.getCode(), outward.getId());
						return unErrorDaifuInfoList.get(0).toResult4Outward();
					}

					List<DaifuConfigRequest> availableOutConfig = OutConfigHolder.getReadyDaifuConfigList();
					List<DaifuConfigRequest> sortedAvailableOutConfig = sortOutConfig(bizHandicap.getCode(),
							availableOutConfig);
					DaifuConfigRequest daifuConfig = sortedAvailableOutConfig.get(0);
					// 设置通道最近使用时间
					setOutConfigUseTime(bizHandicap.getCode(), daifuConfig);
					// 构建代付信息并存库
					DaifuInfo daifuInfo = new DaifuInfo();
					daifuInfo.setChannelBankName(
							getOutConfigChannelName4BankName(daifuConfig.getBankConfig(), outward.getToAccountBank()));
					daifuInfo.setCreateAdminId(operator);
					daifuInfo.setCreateTime(new Timestamp(System.currentTimeMillis()));
					daifuInfo.setDaifuConfigId(daifuConfig.getId());
					daifuInfo.setChannelName(daifuConfig.getChannelName());
					daifuInfo.setMemberId(daifuConfig.getMemberId());
					daifuInfo.setHandicapId(bizHandicap.getId());
					daifuInfo.setLevel(outward.getLevel());
					daifuInfo.setOutwardRequestId(outwardRequestId);
					daifuInfo.setOutwardRequestOrderNo(outward.getOrderNo());
					daifuInfo.setOutwardTaskId(outwardTaskId);
					daifuInfo.setPlatPayCode(genDaifuOrderNo(bizHandicap.getId(), outward.getOrderNo()));
					daifuInfo.setPlatStatus(ResultEnum.UNKOWN.getValue());
					daifuInfo.setUserName(outward.getMember());
					BigDecimal ruleOutFee = BigDecimal.ZERO;

					//该通道的出款规则
					DaifuConfigSyncReqParamOutConfig outConfigSet = getOutSetConfigMap(daifuConfig.getOutConfigSet()).get(OUT_TYPE_BANK);
					if(!ObjectUtils.isEmpty(outConfigSet)
							&& !ObjectUtils.isEmpty(outConfigSet.getNofeeTimes())
							&& outConfigSet.getNofeeTimes()>0
					) {
						//会员今日对该通道已使用次数
						Integer useCount = OutConfigHolder.getReadyUserUseCount().get(daifuConfig.getId());
						if(!ObjectUtils.isEmpty(useCount) && useCount.compareTo(outConfigSet.getNofeeTimes())>=0) {
							DaifuConfigSyncReqParamFeeConfig feeConfig = getOutConfigFeeConfig(daifuConfig.getFeeConfig());
							if (feeConfig != null) {
								if (feeConfig.getFeePercent() == null || BigDecimal.ZERO.equals(feeConfig.getFeePercent())
										|| (feeConfig.getFreeMoney() != null
										&& feeConfig.getFreeMoney().compareTo(amount) > 0)) {
									log.info("本次不收取手续费");
								} else {
									ruleOutFee = amount.multiply(feeConfig.getFeePercent());
									log.info("通过手续费规则计算所得手续费为：{}", ruleOutFee);
									if (feeConfig.getLimitMoney() != null
											&& feeConfig.getLimitMoney().compareTo(BigDecimal.ZERO) > 0
											&& ruleOutFee.compareTo(feeConfig.getLimitMoney()) > 0) {
										log.info("通过手续费规则计算所得手续费{}超过手续费上限{},取手续费上限为本次出款手续费", ruleOutFee);
										ruleOutFee = feeConfig.getLimitMoney();
									}
								}
							}
						}
					}

					BigDecimal exactMoney = amount.subtract(ruleOutFee);
					daifuInfo.setExactMoney(exactMoney);
					daifuInfo.setRuleOutFee(ruleOutFee);

					daifuSubServiceImpl.saveDaifuInfoBeforeSend2PayCore(daifuInfo);
					if (daifuInfo.getId() != null) {
						//FIXME 是否需要其他参数？
						Map<String, Object> otherParam = new HashMap<>();
						outwardRequest2Cache(bizHandicap.getCode(), outward);
						daifuInfo2Cache(bizHandicap.getCode(), daifuInfo);
						Crk2PayCoreSendResponse sendResponse = daifuExtService.sendOrder2PayCore(bizHandicap.getCode(),
								daifuInfo.getPlatPayCode(), otherParam);
						asyncDoOperationByPlatParam(sendResponse);
						result = daifuInfo.toResult4Outward();
						result.setResult(payCoreOrderState2ResultEnum(sendResponse.getRequestDaifuCode(),
								sendResponse.getRequestDaifuOrderState()));
						if(ResultEnum.ERROR.equals(result.getResult())) {
							daifuInfo.setPlatStatus(ResultEnum.ERROR.getValue());
							daifuInfo.setErrorMsg(daifuSubServiceImpl.getErrorMsgFromResMsg(sendResponse.getRequestDaifuErrorMsg()));
						}
						return result;
					} else {
						result = new DaifuResult();
						result.setResult(ResultEnum.PROC_EXCEPTION);
						result.setErrorMsg("保存代付订单信息时异常");
						result.setHandicapId(bizHandicap.getId());
						result.setHandicapCode(bizHandicap.getCode());
						result.setDaifuConfigId(daifuInfo.getDaifuConfigId());
						return result;
					}
				} else {
					log.error("线程{}盘口{}的出款订单ID={}请求使用第三方出款时未能获取到jedisLock", Thread.currentThread().getName(),
							bizHandicap.getCode(), outwardRequestId);
					result = new DaifuResult();
					result.setResult(ResultEnum.PROC_EXCEPTION);
					result.setErrorMsg("请求使用第三方出款时未能获取到jedisLock");
					result.setHandicapId(bizHandicap.getId());
					result.setHandicapCode(bizHandicap.getCode());
					return result;
				}
			} catch (Exception e) {
				log.error("线程{}盘口{}的出款订单ID={}请求使用第三方出款时异常", Thread.currentThread().getName(), bizHandicap.getCode(),
						outwardRequestId, e);
				result = new DaifuResult();
				result.setResult(ResultEnum.PROC_EXCEPTION);
				result.setErrorMsg(String.format("盘口%s的出款订单ID=%s请求使用第三方出款时异常", bizHandicap.getCode(),
						outwardRequestId));
				result.setHandicapId(bizHandicap.getId());
				result.setHandicapCode(bizHandicap.getCode());
				return result;
			}
		} finally {
			OutConfigHolder.clearReadyDaifuConfigList();
		}
	}

	/**
	 * 代付通道最近使用时间 zset外键<br>
	 * daifu:outconfig:use_盘口号
	 */
	private static final String DAIFU_OUTCONFIG_USE_TIME_KEY = "DAIFU:OUTCONFIG:USE_%s";
	/**
	 * 代付通道最近使用时间 zset内健<br>
	 * 提供商_商号
	 */
	private static final String DAIFU_OUTCONFIG_USE_TIME_ZSETKEY = "%s_%s";

	private static final Long SEVEN_DAYS_TIMEMILLIS = 7 * 24 * 60 * 60 * 1000L;

	/**
	 * 出款通道最近使用时间升序排列
	 *
	 * @param handicapCode
	 * @param availableOutConfig
	 * @return
	 */
	private List<DaifuConfigRequest> sortOutConfig(final String handicapCode,
												   final List<DaifuConfigRequest> availableOutConfig) {
		Long minScore = System.currentTimeMillis() - SEVEN_DAYS_TIMEMILLIS;
		String useZsetKey = String.format(DAIFU_OUTCONFIG_USE_TIME_KEY, handicapCode);
		List<DaifuConfigRequest> result = new ArrayList<>();
		Map<String, DaifuConfigRequest> outConfigMap = new HashMap<>();
		availableOutConfig.forEach(t -> {
			outConfigMap.put(String.format(DAIFU_OUTCONFIG_USE_TIME_ZSETKEY, t.getChannelName(), t.getMemberId()), t);
		});

		// 移除当前时间7天之前的数据
		redisService.getStringRedisTemplate().boundZSetOps(useZsetKey).removeRangeByScore(new Double(0), new Double(minScore));
		Set<String> use = redisService.getStringRedisTemplate().boundZSetOps(useZsetKey).rangeByScore(minScore,
				System.currentTimeMillis());
		if (CollectionUtils.isEmpty(use)) {
			result.addAll(availableOutConfig);
		} else {
			/**
			 * 最近使用时间倒序排列 如果通道没有在redis中记录过，默认排在前面 如果通道在redis中记录过，使用 szet顺序的排序
			 */

			List<DaifuConfigRequest> tmp = new ArrayList<>();
			for (DaifuConfigRequest daifuConfig : availableOutConfig) {
				if (!use.contains(String.format(DAIFU_OUTCONFIG_USE_TIME_ZSETKEY, daifuConfig.getChannelName(),
						daifuConfig.getMemberId()))) {
					result.add(daifuConfig);
				} else {
					tmp.add(daifuConfig);
				}
			}
			if (tmp.size() > 0) {
				for (String str : use) {
					DaifuConfigRequest config = outConfigMap.get(str);
					if (!ObjectUtils.isEmpty(config)) {
						result.add(config);
					}
				}
			}
		}

		return result;
	}

	/**
	 * 设置出款通道最近使用时间
	 *
	 * @param handicapCode
	 * @param availableOutConfig
	 */
	private void setOutConfigUseTime(String handicapCode, DaifuConfigRequest availableOutConfig) {
		String useZsetKey = String.format(DAIFU_OUTCONFIG_USE_TIME_KEY, handicapCode);
		redisService.getStringRedisTemplate().boundZSetOps(useZsetKey)
				.add(String.format(DAIFU_OUTCONFIG_USE_TIME_ZSETKEY, availableOutConfig.getChannelName(),
						availableOutConfig.getMemberId()), System.currentTimeMillis());
	}

	private static final String TASK_GET_PAYCORE_RESULT_DISTRIBUTE_LOKC = "TASK_GET_PAYCORE_RESULT_DISTRIBUTE_LOKC_%s";

	/**
	 * 定时任务取代付订单中状态为未知的订单去payCore获取代付结果
	 */
	public void getPayCoreResult4Task() {
		log.debug("定时任务：{}往payCore获取代付订单支付结果", new Timestamp(System.currentTimeMillis()));
		StringRedisTemplate jedis = redisService.getStringRedisTemplate();
		if (jedis != null) {
			List<BizHandicap> handicapList = handicapService.findByStatusEqual(1);
			for (BizHandicap handicap : handicapList) {
				log.debug("尝试将盘口{}的代付订单发送到payCore", handicap.getCode());
				JedisLock lock = new JedisLock(jedis,
						String.format(TASK_GET_PAYCORE_RESULT_DISTRIBUTE_LOKC, handicap.getId()), 1000,
						5 * 60 * 1000);
				try {
					if (lock.acquire()) {
						List<DaifuInfo> unCompleteDaifuList = getDaifuInfoByPlatStatusUnComplete(handicap.getId());
						if (CollectionUtils.isEmpty(unCompleteDaifuList)) {
							log.debug("盘口{}没有未知状态的代付订单", handicap.getCode());
						} else {
							queryDaifuResult4Task(handicap.getCode(), unCompleteDaifuList);
						}
					}
				} catch (InterruptedException e) {
					log.error("定时任务：盘口{}往payCore获取代付订单结果时异常", handicap.getCode(), e);
				} finally {
					lock.release();
				}
			}
		}
	}

	private void queryDaifuResult4Task(String handicapCode, List<DaifuInfo> unCompleteDaifuList) {
		HashMap<String, Object> otherParam = new HashMap<>();
		//定时任务自动查询时加入该参数。减少paycore日志记录
		//因为定时任务非常频繁。payCore在识别该参数后，不记录日志。
		otherParam.put("queryDaifuFrom", "自动查询");
		for (DaifuInfo daifu : unCompleteDaifuList) {
			try {
				daifuInfo2Cache(handicapCode, daifu);
				Crk2PayCoreQueryResponse queryResult = daifuExtService.queryDaifuResult(handicapCode, daifu.getPlatPayCode(), otherParam);
				//asyncDoOperationByPlatParam(queryResult);
			}catch (Exception e) {
				log.error("queryDaifuResult4Task时异常,异常信息:",e);
			}
		}
	}

	private List<DaifuInfo> getDaifuInfoByPlatStatusUnComplete(Integer handicapId) {
		Timestamp createTimeEnd = daifuCacheUtil.getPayCoreTimeEnd();
		Timestamp createTimeStart = daifuCacheUtil.getPayCoreTimeStart();

		List<DaifuInfo> unCompleteDaifuList = daifuInfoDao.findAll((root, criteriaQuery, criteriaBuilder) -> {
			List<Predicate> predicateList = new ArrayList<>();
			Path<Integer> handicapPath = root.get("handicapId");
			Path<Byte> platStatusPath = root.get("platStatus");
			Path<Timestamp> createTimePath = root.get("createTime");
			Predicate p1 = criteriaBuilder.equal(handicapPath, handicapId);
			predicateList.add(p1);
			Predicate p2 = criteriaBuilder.lessThan(createTimePath, createTimeEnd);
			predicateList.add(p2);
			Predicate p3 = criteriaBuilder.greaterThan(createTimePath, createTimeStart);
			predicateList.add(p3);
			Predicate platStatusUnkown = criteriaBuilder.equal(platStatusPath, ResultEnum.UNKOWN.getValue());
			Predicate platStatusPaying = criteriaBuilder.equal(platStatusPath, ResultEnum.PAYING.getValue());
			Predicate p4 = criteriaBuilder.or(platStatusUnkown, platStatusPaying);
			predicateList.add(p4);
			criteriaQuery.where(predicateList.toArray(new Predicate[predicateList.size()]));
			return null;
		});
		return unCompleteDaifuList;
	}

	private static final Long THIRTY_MINUTE_MILLIS= 30*60*1000L;
	/**
	 * 代付订单信息存入redis,并设置有效时间 30分钟
	 * @param handicapCode
	 * @param daifuInfo
	 */
	private void outwardRequest2Cache(String handicapCode,BizOutwardRequest outward) {
		try {
			String key = String.format(DAIFU_INFO2OUTREQUEST_SENDED_KEY, handicapCode,outward.getId());
			String value = mapper.writeValueAsString(outward);
			redisService.getStringRedisTemplate().boundValueOps(key).set(value);
			redisService.getStringRedisTemplate().boundValueOps(key).expire(THIRTY_MINUTE_MILLIS, TimeUnit.MILLISECONDS);
			log.info("将代付订单对应的出款单放入到缓存中,key={}，value={},当前时间:{}",key,value,System.currentTimeMillis());
		}catch (Exception e) {
			log.error("outwardRequest2Cache时异常",e);
		}
	}
	/**
	 * 代付订单信息存入redis,并设置有效时间 30分钟
	 * @param handicapCode
	 * @param daifuInfo
	 */
	private void daifuInfo2Cache(String handicapCode,DaifuInfo daifuInfo) {
		try {
			String key = String.format(DAIFU_INFO_SENDED_KEY, handicapCode,daifuInfo.getPlatPayCode());
			redisService.getStringRedisTemplate().boundValueOps(key).set(mapper.writeValueAsString(daifuInfo));
			redisService.getStringRedisTemplate().boundValueOps(key).expire(THIRTY_MINUTE_MILLIS, TimeUnit.MILLISECONDS);
		}catch (Exception e) {
			log.error("daifuInfo2Cache时异常",e);
		}
	}

	public boolean isSupportedBankType(Integer handicapId, String bankType) {
		List<DaifuConfigRequest> activeOutConfigList = getOutConfigByActiveStatus(handicapId);
		log.debug("线程{} isSupportedBankType获取盘口id={}银行={} 可用的出款通道数量：{}", Thread.currentThread().getName(),
				handicapId,bankType, activeOutConfigList.size());
		if(CollectionUtils.isEmpty(activeOutConfigList)) {
			return false;
		}
		List<DaifuConfigRequest> channelBankSuitableList = getChannelBankSuitableListFromActiveOutConfigList(activeOutConfigList, bankType);
		log.debug("线程{} getChannelBankSuitableListFromActiveOutConfigList获取盘口id={}银行={} 过滤掉配置的提供商不可用银行后，剩余数量：{}", Thread.currentThread().getName(),
				handicapId,bankType, channelBankSuitableList.size());
		if(CollectionUtils.isEmpty(channelBankSuitableList)) {
			return false;
		}
		List<DaifuConfigRequest> userBankNameSuitableList = channelBankSuitableList.stream()
				//通道支持的银行
				.filter(t -> isUserBankSuitable(t.getBankConfig(), bankType))
				.collect(Collectors.toList());
		log.debug("线程{}经过{通道支持的银行-用户银行{}}过滤后的剩余出款通道数量为:{}", Thread.currentThread().getName(),
				bankType, userBankNameSuitableList.size());
		return (!CollectionUtils.isEmpty(userBankNameSuitableList));
	}

}