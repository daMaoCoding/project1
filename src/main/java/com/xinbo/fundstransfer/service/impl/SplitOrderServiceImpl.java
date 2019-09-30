package com.xinbo.fundstransfer.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.xinbo.fundstransfer.accountfee.exception.NoSuiteAccountFeeRuleException;
import com.xinbo.fundstransfer.accountfee.pojo.AccountFeeCalResult;
import com.xinbo.fundstransfer.accountfee.pojo.AccountFeeConfig;
import com.xinbo.fundstransfer.accountfee.service.AccountFeeService;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.HandicapService;
import com.xinbo.fundstransfer.service.RedisService;
import com.xinbo.fundstransfer.service.SplitOrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.persistence.EntityManager;
import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service(value = "SplitOrderServiceImpl")
@Slf4j

public class SplitOrderServiceImpl implements SplitOrderService {
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private AccountFeeService accountFeeService;
	@Autowired
	private RedisService redisService;
	@Autowired
	private AccountService accountService;
	@Autowired
	private HandicapService handicapService;
	private static final String SUCCESS_OK = "OK";

	/**
	 * 如果没有设定第三方账号 不能拆单
	 * 
	 * @param operatorId
	 * @return
	 */
	@Override
	public boolean accessibleSplit(Integer operatorId) {
		String key = RedisKeys.SETUP_USER_THIRDACCOUNT + ":" + operatorId;
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		if (template.hasKey(key)) {
			return true;
		}
		return false;
	}

	@Override
	public BigDecimal getHistoryFee(String orderNo, String subOrder) {
		Assert.notNull(orderNo, "订单为空");
		Assert.notNull(subOrder, "子订单为空");
		String key = RedisKeys.SPLIT_SUBORDER_HISTORY_THIRD_AMOUNT_FEE + orderNo;
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		if (template.hasKey(key)) {
			HashOperations operations = template.opsForHash();
			Object obj = operations.get(key, subOrder);
			if (null != obj) {
				return new BigDecimal(obj.toString());
			}
		}
		return BigDecimal.ZERO;
	}

	@Override
	public void saveHistoryFee(String orderNo, String subOrder, BigDecimal fee, boolean save) {
		Assert.notNull(orderNo, "订单为空");
		// SPLIT_SUBORDER_HISTORY_THIRD_AMOUNT_FEE
		String key = RedisKeys.SPLIT_SUBORDER_HISTORY_THIRD_AMOUNT_FEE + orderNo;
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		HashOperations operations = template.opsForHash();
		if (save) {
			Assert.notNull(subOrder, "子订单为空");
			Assert.notNull(fee, "手续费为空");
			operations.put(key, subOrder, (fee == null ? 0 : fee) + "");
		} else {
			if (StringUtils.isNotBlank(subOrder)) {
				if (operations.hasKey(key, subOrder)) {
					operations.delete(key, subOrder);
				}
			} else {
				template.delete(key);
			}
		}
	}

	/**
	 * 重新出款操作 重置
	 *
	 * @param subOrder
	 * @param operatorId
	 * @return
	 */
	@Override
	public String resetFinished(String orderNo, String subOrder, Integer operatorId) {
		Assert.notNull(orderNo, "订单号为空");
		Assert.notNull(subOrder, "子单号为空");
		Assert.notNull(operatorId, "操作人为空");
		String key = RedisKeys.SPLIT_USER_ORDER + operatorId + ":" + orderNo;
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		BigDecimal amount = BigDecimal.ZERO;
		BigDecimal fee = BigDecimal.ZERO;
		Integer thirdId = null;
		HashOperations operations = template.opsForHash();
		if (template.hasKey(key)) {
			Object hval = operations.get(key, subOrder);
			if (null != hval) {
				// amount:status
				String[] havlArr = hval.toString().split(":");
				String newHval = havlArr[0] + ":" + SubOrderStatus.SPLIT.getStatus();
				operations.put(key, subOrder, newHval);
			}
		}
		String keyFinished = RedisKeys.SPLIT_SUBORDER_FINISH_THIRD_AMOUNT_FEE + operatorId + ":" + orderNo;
		if (template.hasKey(keyFinished)) {

			if (operations.hasKey(keyFinished, subOrder)) {
				// thirdId:amount:fee
				Object hval = operations.get(keyFinished, subOrder);
				if (null != hval) {
					// thirdId:amount:fee
					String[] hvalArr = hval.toString().split(":");
					thirdId = Integer.valueOf(hvalArr[0]);
					amount = new BigDecimal(hvalArr[1]);
					fee = new BigDecimal(hvalArr[2]);
					operations.delete(keyFinished, subOrder);
				}
			}
		}
		saveHistoryFee(orderNo, subOrder, fee, true);
		return SUCCESS_OK;
	}

	/**
	 * 根据三方账号手续费规则<br>
	 * 更新 三方账号的系统余额<br>
	 * 
	 * 用于给会员出款的三方账号，手续费规则只能是 从商户扣 0 <br>
	 * 如果是从出款金额里面扣 1 ,那么实际出款金额要加上手续费<br>
	 * {@link AccountFeeConfig#getFeeType()}
	 *
	 * @param amount
	 * @param fee
	 * @param thirdId
	 * @param add
	 *            true 加系统余额 false 减系统余额
	 */
	@Override
	public String updateThirdAccountBalance(BigDecimal amount, BigDecimal fee, Integer thirdId, boolean add) {
		Assert.notNull(amount, "金额为空");
		Assert.notNull(fee, "手续费为空");
		Assert.notNull(thirdId, "三方账号id为空");
		BizAccount account = accountService.getById(thirdId);
		if (account == null) {
			log.info("三方账号信息为空!");
			return "三方账号信息为空!";
		}

		AccountFeeConfig config = accountFeeService.findByAccountBaseInfo(account);
		if (config == null) {
			log.info("编号:{} 没有设置手续费规则!", account.getAlias());
			return "编号:" + account.getAlias() + "没有设置手续费规则!";
		}
		// TODO 是否跟三方账号手续费规则相关?
		// 从金额扣 在三方官网输入的金额=拆单金额+手续费
		// 从商户里扣 在三方官网输入的金额为拆单金额
		if (add) {
			// 重新出款
			account.setBalance(account.getBalance().add(amount.add(fee)));
		} else {
			// 完成出款
			account.setBalance(account.getBalance().subtract(amount.add(fee)));
		}
		return SUCCESS_OK;
	}

	@Override
	public BigDecimal getOrderAmountByOrderNo(String orderNo) {
		String sql = "select amount from biz_outward_request where order_no='" + StringUtils.trim(orderNo) + "'";
		Object obj = entityManager.createNativeQuery(sql).getSingleResult();
		return null != obj ? new BigDecimal(obj.toString()) : null;
	}

	@Override
	public List<Map<String, String>> splitList(String orderNo, Integer userId) {
		List<Map<String, String>> list = Lists.newLinkedList();
		String key = RedisKeys.SPLIT_USER_ORDER + userId + ":" + orderNo;
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		// 查询完成出款 使用的盘口 三方账号 三方商号
		Map<String, Map<String, String>> sub = getUsedThirdId(orderNo, userId);
		if (template.hasKey(key)) {
			HashOperations operations = template.opsForHash();
			Map<String, String> map = operations.entries(key);
			for (Map.Entry<String, String> entry : map.entrySet()) {
				Map<String, String> map1 = new HashMap<>(3);
				String val = entry.getValue();
				String[] valArr = val.split(":");
				String subOrder = entry.getKey();
				map1.put("subOrder", subOrder);
				if (!CollectionUtils.isEmpty(sub)) {
					Map<String, String> sub1 = sub.get(subOrder);
					if (!CollectionUtils.isEmpty(sub1)) {
						// handicapName bankName account thirdId amount fee
						map1.put("statusDesc", SubOrderStatus.SUCCESS.getDesc());
						map1.put("handicapName", sub1.get("handicapName"));
						map1.put("bankName", sub1.get("bankName"));
						map1.put("account", sub1.get("account"));
						map1.put("thirdId", sub1.get("thirdId"));
						map1.put("amount", sub1.get("amount"));
						map1.put("fee", sub1.get("fee"));
					} else {
						map1.put("amount", valArr[0]);
						// 重新打回之后的手续费
						map1.put("fee", getHistoryFee(orderNo, subOrder).toString());
						map1.put("statusDesc", SubOrderStatus.getStatusDesc(Integer.valueOf(valArr[1])));
					}
				} else {
					map1.put("amount", valArr[0]);
					// 拆分完成 或者 重新出款
					map1.put("statusDesc", SubOrderStatus.getStatusDesc(Integer.valueOf(valArr[1])));
					// 重新打回之后的手续费
					map1.put("fee", getHistoryFee(orderNo, subOrder).toString());
				}
				list.add(map1);
			}
		}
		return list;
	}

	/**
	 * 拆单:根据输入的 {@see expectedSplitNum} 拆单 <br>
	 * 并缓存在redis 生成子订单时间戳加拆分数量均值 <br>
	 * 如果输入的订单号已经拆分则直接删除原来的拆分 重新拆分<br>
	 * hkey 当前时间+拆单数量n的某个值 <br>
	 * hval 金额+":"+ 状态{@link SubOrderStatus}
	 *
	 * @param orderNo
	 * @param expectedSplitNum
	 * @param operatorId
	 * @return
	 */
	@Override
	public String splitOrder(@NotBlank String orderNo, Integer expectedSplitNum, Integer operatorId) {
		Assert.notNull(orderNo, "订单号不能为空!");
		Assert.notNull(expectedSplitNum, "拆单数量不能为空!");
		// Supplier<String> supplier = () -> "拆单必须大于1";
		// Assert.isTrue(expectedSplitNum > 2, supplier);
		Assert.notNull(operatorId, "操作人不能为空!");
		BigDecimal amount = getOrderAmountByOrderNo(orderNo);
		if (null == amount)
			return "订单金额为空";

		String key = RedisKeys.SPLIT_USER_ORDER + operatorId + ":" + orderNo;
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		HashOperations operations = template.opsForHash();
		return split(orderNo, operatorId, amount, expectedSplitNum, operations, key);
	}

	/**
	 * 取消拆单
	 *
	 * @param orderNo
	 * @param operatorId
	 * @return
	 */
	@Override
	public String cancelSplit(String orderNo, Integer operatorId) {
		Assert.notNull(orderNo, "订单号不能为空!");
		Assert.notNull(operatorId, "用户Id不能为空!");
		if (isPartialFinished(orderNo, operatorId)) {
			return "已经有部分完成出款,先打回再取消!";
		}
		String key = RedisKeys.SPLIT_USER_ORDER + operatorId + ":" + orderNo;
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		if (template.hasKey(key)) {
			template.delete(key);
			saveHistoryFee(orderNo, null, null, false);
		}
		return SUCCESS_OK;
	}

	private String split(String orderNo, Integer operatorId, BigDecimal amount, Integer expectedSplitNum,
			HashOperations operations, String key) {
		// 订单总金额
		String amountStr = amount.toString();
		long now = LocalDateTime.now().toInstant(ZoneOffset.of("+8")).toEpochMilli();
		// 已经完成的金额
		BigDecimal amountAlreadyFinished = amountAlreadyFinished(orderNo, operatorId);
		if (new BigDecimal(amountStr).compareTo(amountAlreadyFinished) <= 0) {
			return "OK";
		}
		BigDecimal amountRemainderBD = new BigDecimal(amountStr).subtract(amountAlreadyFinished);
		String amountRemainderStr = amountRemainderBD.toString();
		String[] amountRemainderStrArr = new String[] { amountRemainderStr };
		if (amountRemainderStr.indexOf(".") >= 0) {
			amountRemainderStrArr = amountRemainderStr.split("\\.");
		}

		Integer amountRemainder = Integer.valueOf(amountRemainderStrArr[0]);
		if (expectedSplitNum > amountRemainder / 10 - 2) {// 拆单数不能大于金额的十分之一减2 例：140最多分14-2=12
			return "拆单数据太大,重新输入";
		}
		// 余数
		int remainder = amountRemainder % expectedSplitNum;
		int average = amountRemainder / expectedSplitNum;
		deleteUnfinishSubOrder(orderNo, operatorId);
		// 算法使每单金额都不一样
		average = average - 1; // 每个单金额减1给波动值
		int floatCount = remainder + expectedSplitNum;// 波动值=余数+单数
		for (int i = 0; i < expectedSplitNum; i++) {
			if (i < expectedSplitNum - 1) {
				if (i % 2 == 0) {
					floatCount = floatCount - i;
					operations.put(key, now + "" + i, average + i + ":" + SubOrderStatus.SPLIT.getStatus());
				} else {
					floatCount = floatCount + i;
					operations.put(key, now + "" + i, average - i + ":" + SubOrderStatus.SPLIT.getStatus());
				}

			} else {
				// 最后一个单加上小数点后的金额
				if (amountRemainderStrArr.length > 1 && !"00".equals(amountRemainderStrArr[1])) {
					operations.put(key, now + "" + i, (average + floatCount) + "." + amountRemainderStrArr[1] + ":"
							+ SubOrderStatus.SPLIT.getStatus());
				} else {
					operations.put(key, now + "" + i, (average + floatCount) + ":" + SubOrderStatus.SPLIT.getStatus());
				}
			}

		}
		return SUCCESS_OK;

	}

	/**
	 * 新增或者删除 某个子单
	 *
	 * @param subOrder
	 * @param orderNo
	 * @param operatorId
	 * @param type
	 * @return
	 */
	@Override
	public String updateSplit(String subOrder, String orderNo, Integer operatorId, Byte type) {
		Assert.notNull(subOrder, "子订单为空");
		Assert.notNull(orderNo, "订单为空");
		Assert.notNull(operatorId, "操作人为空");
		Assert.notNull(type, "类型为空");

		String key = RedisKeys.SPLIT_USER_ORDER + operatorId + ":" + orderNo;
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		if (template.hasKey(key)) {
			HashOperations operations = template.opsForHash();
			Long splitNum = operations.size(key);
			if (isPartialFinished(orderNo, operatorId)) {
				splitNum = splitNum - finishedNums(orderNo, operatorId);
			}
			if (type.compareTo((byte) 1) == 0) {
				// 减
				if (splitNum == 1) {
					operations.delete(key, subOrder);
					saveHistoryFee(orderNo, subOrder, null, false);
					return "OK";
				}
				saveHistoryFee(orderNo, subOrder, null, false);
				return splitOrder(orderNo, splitNum.intValue() - 1, operatorId);
			}
			if (type.compareTo((byte) 2) == 0) {
				// 加
				return splitOrder(orderNo, splitNum.intValue() + 1, operatorId);
			}
		}
		return SUCCESS_OK;
	}

	/**
	 * <p>
	 * 更新拆分的子订单状态
	 * </p>
	 * <li>如果完成 更新子订单为完成状态</li>
	 * <li>如果重新出款(原第三方不可用 或者其他异常) 则删除原来的出款 重新出款</li>
	 * 
	 *
	 * @param orderNo
	 *            真正的会员提单订单号
	 * @param subOrderNo
	 *            拆分的订单号 拆分时间+拆分单数升序 <br>
	 *            （如1565856604378+1/2/3/4/5/....）
	 */
	@Override
	public String updateSubOrderFinish(Integer thirdId, String orderNo, Integer operatorId, String subOrderNo,
			BigDecimal amount, BigDecimal fee) {
		Assert.notNull(thirdId, "三方账号id为空");
		Assert.notNull(orderNo, "订单号为空");
		Assert.notNull(operatorId, "操作人为空");
		Assert.notNull(subOrderNo, "子单号为空");
		Assert.notNull(amount, "金额为空");
		Assert.notNull(fee, "手续费为空");
		// 已经完成出款的子订单总金额
		BigDecimal amountAlreadyFinished = amountAlreadyFinished(orderNo, operatorId);
		AccountBaseInfo baseInfo = accountService.getFromCacheById(thirdId);
		// 手续费规则
		AccountFeeConfig th3FeeCfg = accountFeeService.findTh3FeeCfg(baseInfo);
		AccountFeeCalResult feeCalResult = null;
		// 计算出的手续费
		try {
			feeCalResult = accountFeeService.calAccountFee2(baseInfo, amount);
			log.debug("三方账号id:{} 出款金额:{} 手续费结果:{}", thirdId, amount, ObjectMapperUtils.serialize(feeCalResult));
		} catch (NoSuiteAccountFeeRuleException e) {
			log.error("获取手续费异常:", e);

		}
		// 商家扣 false 从金额扣 true(页面传入的金额比实际的拆单金额大，总的实际金额比订单实际金额大)
		boolean isMinusFeeFromAmount = false;
		// 拆单生成的子订单金额
		BigDecimal subOrderAmount = getSplitAmountBySubOrder(orderNo, subOrderNo, operatorId);
		if (Objects.nonNull(th3FeeCfg)) {
			// 商家扣0 从金额扣1
			if (Objects.equals(th3FeeCfg.getFeeType(), (byte) 1)) {
				log.info("子订单:{}, 页面传入金额:{}, 拆单金额:{}, 使用三方账号id:{} ,手续费规则为:{} 从金额里扣除!", subOrderNo, amount,
						subOrderAmount, thirdId, th3FeeCfg.getFeeType());
				isMinusFeeFromAmount = true;
			}
		} else {
			log.info("账号id:{} 没有设置手续费规则!", thirdId);
		}
		// 实际出款总额减去订单实际金额大于0
		BigDecimal subtractAmount = amount.subtract(subOrderAmount);
		// 出款金额是否在使用的三方账号的手续费规则里
		boolean isInFeeRule = Objects.nonNull(feeCalResult) && Objects.nonNull(feeCalResult.getFee());

		if (isMinusFeeFromAmount && subtractAmount.compareTo(BigDecimal.ZERO) > 0 && isInFeeRule) {
			log.info("子订单:{} 页面传入实际金额比拆单金额大:{},页面传入手续费:{}", subOrderNo, subtractAmount, fee);
			if (subtractAmount.compareTo(fee) != 0) {
				// 如果差值跟页面传入的手续费不等则以差值为手续费
				log.info("以差值:{} 作为手续费", subtractAmount);
				fee = subtractAmount;
			}
			// 如果使用的三方账号手续费规则是从金额里扣除的，页面传入的金额会比拆单金额大
			// 实际应该给会员出的金额为拆单的子订单金额
			amount = subOrderAmount;
		}

		// 将要完成的金额+已经完成的金额
		BigDecimal amountToFinishAll = amountAlreadyFinished != null ? amount.add(amountAlreadyFinished) : amount;
		// 订单实际金额
		BigDecimal amountReal = getOrderAmountByOrderNo(orderNo);
		// 如果是从金额里面扣手续费 但是根据规则计算的手续费为空或者为0 那么出款的金额不能大于总金额
		if (isMinusFeeFromAmount && !isInFeeRule) {
			if (amountToFinishAll.compareTo(amountReal) >= 0) {
				return "出款金额太大,请重新输入金额!";
			}
		}
		if (isExceededAmount(amountReal, amountToFinishAll)) {
			log.info("订单号:{} 拆单后要出款金额总数:{}  订单实际金额:{}  ", orderNo, amountToFinishAll, amountReal);
			// return "提现总金额:" + amountToFinishAll + ",超出了订单金额:" + amountReal;
		}
		String key = RedisKeys.SPLIT_USER_ORDER + operatorId + ":" + orderNo;
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		HashOperations operations = template.opsForHash();
		String hkey = subOrderNo;
		Object obj = operations.get(key, hkey);
		if (null == obj) {
			log.info("不存在拆单:{}", hkey);
			return "提现的拆单不存在";
		}

		// 真正完成的金额
		String newVal = amount + ":" + SubOrderStatus.SUCCESS.getStatus();
		// 更新 拆单子订单状态
		operations.put(key, hkey, newVal);
		// 保存使用的第三方账号使用的金额
		saveThirdIdAndAmount(orderNo, operatorId, subOrderNo, thirdId, amount, fee, true);
		// 保存历史手续费
		saveHistoryFee(orderNo, subOrderNo, null, false);
		// updateThirdAccountBalance(amount, fee, thirdId, false);
		// 完成之后根据实际完成金额对未完成的拆单重新拆单
		return "OK";// splitSubFinishPostProccess(orderNo, operatorId);
	}

	@Override
	public String checkFeeAndOrderAmount(Integer thirdId, String orderNo, Integer operatorId, String subOrderNo,
			BigDecimal amount, BigDecimal fee) {
		Assert.notNull(thirdId, "三方账号id为空");
		Assert.notNull(orderNo, "订单号为空");
		Assert.notNull(operatorId, "操作人为空");
		Assert.notNull(subOrderNo, "子单号为空");
		Assert.notNull(amount, "金额为空");
		Assert.notNull(fee, "手续费为空");

		AccountBaseInfo baseInfo = accountService.getFromCacheById(thirdId);
		AccountFeeConfig th3FeeCfg = accountFeeService.findTh3FeeCfg(baseInfo);
		// 商家扣 false 从金额扣 true
		boolean isMinusFeeFromAmount = false;
		if (Objects.nonNull(th3FeeCfg)) {
			// 商家扣0 从金额扣1
			if (Objects.equals(th3FeeCfg.getFeeType(), (byte) 1)) {
				isMinusFeeFromAmount = true;
			}
		} else {
			log.info("账号id:{} 没有设置手续费规则!", thirdId);
		}
		if (isMinusFeeFromAmount) {
			// 拆单子单号的金额
			BigDecimal subOrderAmount = getSplitAmountBySubOrder(orderNo, subOrderNo, operatorId);
			// fee 前端传入的手续费金额
			if (BigDecimal.ZERO.compareTo(subOrderAmount) > 0 && subOrderAmount.add(fee).compareTo(amount) != 0) {
				log.info("本拆单金额:{} 手续费:{} 输入的实际出款金额:{}", subOrderAmount, fee, amount);
				return "本拆单金额:" + subOrderAmount + " 加上手续费:" + fee + " ,不等于输入的实际出款金额:" + amount;
			}
		}
		return "OK";
	}

	@Override
	public String splitSubFinishPostProccess(String orderNo, Integer operatorId) {
		Assert.notNull(orderNo, "订单号为空");
		Assert.notNull(operatorId, "用户id为空");

		BigDecimal amountAlreadyFinished = amountAlreadyFinished(orderNo, operatorId);
		BigDecimal amountReal = getOrderAmountByOrderNo(orderNo);
		BigDecimal toFinishAmount = amountReal.subtract(amountAlreadyFinished);
		if (BigDecimal.ZERO.compareTo(toFinishAmount) == 0) {
			log.info("全部完成 无需重新拆单1");
			return "OK";
		}
		// 未完成的数量
		Long unfinishedNums = unfinishedNums(operatorId, orderNo);
		if (unfinishedNums == 0L) {
			log.info("全部完成 无需重新拆单2");
			return "OK";
		}
		String res = splitOrder(orderNo, unfinishedNums.intValue(), operatorId);
		if (!SUCCESS_OK.equals("OK")) {
			log.info("完成子订单之后重新拆单异常!");
			res = "完成子订单之后重新拆单异常:" + res;
		}
		return res;
	}

	/**
	 * 保存 子订单 使用的第三方账号 金额
	 * 
	 * @param orderNo
	 * @param operatorId
	 * @param subOrder
	 * @param thirdId
	 * @param amount
	 * @param save
	 */
	@Override
	public void saveThirdIdAndAmount(String orderNo, Integer operatorId, String subOrder, Integer thirdId,
			BigDecimal amount, BigDecimal fee, boolean save) {
		String key = RedisKeys.SPLIT_SUBORDER_FINISH_THIRD_AMOUNT_FEE + operatorId + ":" + orderNo;
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		HashOperations operations = template.opsForHash();
		if (save) {
			if (operations.hasKey(key, subOrder)) {
				operations.delete(key, subOrder);
			}
			operations.put(key, subOrder, thirdId + ":" + amount + ":" + fee);
		} else {
			operations.delete(key, subOrder);
		}
	}

	/**
	 * 查询 某个订单号拆单之后子订单使用的三方账号id和金额
	 *
	 * @param orderNo
	 * @param operatorId
	 * @return
	 */
	@Override
	public Map<String, Map<String, String>> getUsedThirdId(String orderNo, Integer operatorId) {
		String key = RedisKeys.SPLIT_SUBORDER_FINISH_THIRD_AMOUNT_FEE + operatorId + ":" + orderNo;
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		// 如果原有 则直接覆盖
		HashOperations operations = template.opsForHash();
		if (template.hasKey(key)) {
			Map<String, String> map = operations.entries(key);
			Map<String, Map<String, String>> res = Maps.newLinkedHashMap();
			if (!CollectionUtils.isEmpty(map)) {
				for (Map.Entry<String, String> entry : map.entrySet()) {
					String hval = entry.getValue();
					String[] hvalArr = hval.split(":");
					if (null != hvalArr && hvalArr.length > 0) {
						String thirdId = hvalArr[0];
						String amount = hvalArr[1];
						String fee = hvalArr[2];
						AccountBaseInfo info = accountService.getFromCacheById(Integer.valueOf(thirdId));
						if (info != null) {
							// handicapName bankName account thirdId amount fee
							BizHandicap handicap = handicapService.findFromCacheById(info.getHandicapId());
							Map<String, String> data = new LinkedHashMap() {
								{
									put("handicapName", handicap == null ? "无" : handicap.getName());
									put("bankName", info.getBankName());
									put("account", info.getAccount());
									put("thirdId", thirdId);
									put("amount", amount);
									put("fee", fee);
								}
							};
							res.put(entry.getKey(), data);
						}
					}
				}
			}
			return res;
		}
		return Maps.newHashMap();
	}

	@Override
	public boolean isPartialFinished(String orderNo, Integer userId) {
		String key2 = RedisKeys.SPLIT_SUBORDER_FINISH_THIRD_AMOUNT_FEE + userId + ":" + orderNo;
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		return template.hasKey(key2);
	}

	/**
	 * 未完成的子单子数量
	 * 
	 * @param operatorId
	 * @param orderNo
	 * @return
	 */
	@Override
	public Long unfinishedNums(Integer operatorId, String orderNo) {
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		HashOperations operations = template.opsForHash();
		String key = RedisKeys.SPLIT_USER_ORDER + operatorId + ":" + orderNo;
		// 所有子单子数量 - 已经完成出款的子单数量
		Long splitNum = operations.size(key);

		return splitNum - finishedNums(orderNo, operatorId);
	}

	@Override
	public void deleteUnfinishSubOrder(String orderNo, Integer operatorId) {
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		HashOperations operations = template.opsForHash();
		String key = RedisKeys.SPLIT_USER_ORDER + operatorId + ":" + orderNo;
		String[] subOrders = unfinishedSubOrderHkeys(orderNo, operatorId);
		if (null != subOrders && subOrders.length > 0) {
			operations.delete(key, subOrders);
		}
	}

	@Override
	public String[] unfinishedSubOrderHkeys(String orderNo, Integer operatorId) {
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		HashOperations operations = template.opsForHash();
		String key = RedisKeys.SPLIT_USER_ORDER + operatorId + ":" + orderNo;
		Map<String, String> map = operations.entries(key);
		List<String> hkeys = Lists.newLinkedList();
		for (Map.Entry<String, String> entry : map.entrySet()) {
			String[] vals = entry.getValue().split(":");
			if (SubOrderStatus.SPLIT.getStatus().equals(Integer.valueOf(vals[1]))) {
				hkeys.add(entry.getKey());
			}
		}
		int size = hkeys.size();
		String[] res = new String[size];
		for (int i = 0; i < size; i++) {
			res[i] = hkeys.get(i);
		}
		return res;
	}

	/**
	 * 子订单完成出款的数量
	 * 
	 * @param orderNo
	 * @return
	 */
	@Override
	public Long finishedNums(String orderNo, Integer userId) {
		String key2 = RedisKeys.SPLIT_SUBORDER_FINISH_THIRD_AMOUNT_FEE + userId + ":" + orderNo;
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		HashOperations hashOperations = template.opsForHash();
		if (template.hasKey(key2)) {
			return hashOperations.size(key2);
		}
		return 0L;
	}

	@Override
	public boolean isExceededAmount(BigDecimal amountReal, BigDecimal amountAlreadyFinished) {
		if (amountReal.compareTo(amountAlreadyFinished) < 0) {
			return true;
		}
		return false;
	}

	/**
	 * 使用的三方信息:盘口|商号|金额,...<br>
	 * 盘口:金龙彩票,商号:亿汇付,金额:10000,id:248702|盘口:金龙彩票,商号:汇隆,金额:10000,id:248720"<br>
	 *
	 * @param orderNo
	 * @param operatorId
	 * @return
	 */
	@Override
	public String usedThirdAccount(String orderNo, Integer operatorId, String fee) {
		Assert.notNull(operatorId, "用户id为空");
		Assert.notNull(orderNo, "订单号为空");
		StringBuilder stringBuilder = new StringBuilder();
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		String key2 = RedisKeys.SPLIT_SUBORDER_FINISH_THIRD_AMOUNT_FEE + operatorId + ":" + orderNo;
		if (template.hasKey(key2)) {
			HashOperations hashOperations = template.opsForHash();
			Map<String, String> entries = hashOperations.entries(key2);
			for (Map.Entry<String, String> entry : entries.entrySet()) {
				// thirdId:amount:fee
				String hval = entry.getValue();
				if (StringUtils.isNotBlank(hval)) {
					String[] hvalArr = hval.split(":");
					Integer thirdId = Integer.valueOf(hvalArr[0]);
					BigDecimal feeVal = new BigDecimal(hvalArr[2]);
					String amount = hvalArr[1];
					AccountBaseInfo baseInfo = accountService.getFromCacheById(thirdId);
					if (baseInfo != null && null != baseInfo.getHandicapId()) {
						BizHandicap handicap = handicapService.findFromCacheById(baseInfo.getHandicapId());
						if (null != handicap && StringUtils.isNotBlank(handicap.getName())) {
							stringBuilder.append("盘口:").append(handicap.getName()).append(",商号:")
									.append(baseInfo.getBankName()).append(",金额:").append(amount).append(",id:")
									.append(thirdId).append(",手续费:").append(feeVal).append("|");
						} else {
							log.info("三方账号id:{} 盘口信息:{}", thirdId, handicap.toString());
							return "NO";
						}
					} else {
						log.info("三方账号Id：{} 信息:{}", thirdId, baseInfo.toString());
						return "NO";
					}

				}
			}
		}
		int lastIndex = stringBuilder.toString().lastIndexOf("|");
		return lastIndex >= 0 ? stringBuilder.toString().substring(0, lastIndex) : StringUtils.EMPTY;
	}

	@Override
	public BigDecimal amountAlreadyFinished(String orderNo, Integer operatorId) {
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		BigDecimal amountFinished = BigDecimal.ZERO;
		String key2 = RedisKeys.SPLIT_SUBORDER_FINISH_THIRD_AMOUNT_FEE + operatorId + ":" + orderNo;
		if (template.hasKey(key2)) {
			HashOperations hashOperations = template.opsForHash();
			Map<String, String> entries = hashOperations.entries(key2);
			for (Map.Entry<String, String> entry : entries.entrySet()) {
				// thirdId:amount:fee
				String hval = entry.getValue();
				if (StringUtils.isNotBlank(hval)) {
					String[] amountArr = hval.split(":");
					amountFinished = amountFinished.add(new BigDecimal(amountArr[1]));
				}
			}
		}
		return amountFinished;
	}

	/**
	 * 全部出款完成的时候 调用 删除所有拆单缓存
	 *
	 * @param orderNo
	 */
	@Override
	public void updateOrderFinal(String orderNo, Integer operatorId) {
		Assert.notNull(orderNo, "订单号为空");
		Assert.notNull(operatorId, "操作人为空");
		// 删除完成金额 记录
		String key = RedisKeys.SPLIT_SUBORDER_FINISH_THIRD_AMOUNT_FEE + operatorId + ":" + orderNo;
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		if (template.hasKey(key)) {
			template.delete(key);
		}
		// 删除拆单记录
		String key2 = RedisKeys.SPLIT_USER_ORDER + operatorId + ":" + orderNo;
		if (template.hasKey(key2)) {
			template.delete(key2);
		}
	}

	/**
	 * 是否可以 点击完成 <br>
	 * 真正完成出款<br>
	 * 在点击完成出款的时候加校验 调用
	 *
	 * @param orderNo
	 * @return
	 */
	@Override
	public boolean finalFinishCapable(String orderNo, Integer userId) {
		BigDecimal unFinishedAmount = finalUnFinishedAmount(orderNo, userId);
		return BigDecimal.ZERO.compareTo(unFinishedAmount) == 0;
	}

	/**
	 * 最后点击完成 还有尚未完成出款的金额
	 *
	 * @param orderNo
	 * @return
	 */
	@Override
	public BigDecimal finalUnFinishedAmount(String orderNo, Integer userId) {
		BigDecimal amountAlreadyFinished = amountAlreadyFinished(orderNo, userId);
		BigDecimal amountReal = getOrderAmountByOrderNo(orderNo);
		return amountReal.subtract(amountAlreadyFinished);
	}

	@Override
	public BigDecimal getSplitAmountBySubOrder(String orderNo, String subOrder, Integer userId) {
		Assert.notNull(orderNo, "订单号为空");
		Assert.notNull(subOrder, "子订单号为空");
		Assert.notNull(userId, "操作人为空");
		String key = RedisKeys.SPLIT_USER_ORDER + userId + ":" + orderNo;
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		HashOperations operations = template.opsForHash();
		BigDecimal amount = BigDecimal.ZERO;
		if (template.hasKey(key)) {
			Object obj = operations.get(key, subOrder);
			if (obj != null) {
				String subAmount = obj.toString().split(":")[0];
				log.info("订单号:{} 子单号:{} 金额：{}", orderNo, subOrder, subAmount);
				amount = new BigDecimal(subAmount);
			}
		}
		return amount;
	}
}
