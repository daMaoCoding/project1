package com.xinbo.fundstransfer.report.patch.classify;

import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizSysLog;
import com.xinbo.fundstransfer.domain.enums.SysLogType;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.patch.PatchAction;
import com.xinbo.fundstransfer.report.patch.PatchAnnotation;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 初始化导致系统对账不正确，进行修正 </br>
 * 类型-------交易金额--------系统余额------银行余额------对账结果</br>
 * 下发----- -5519.58 ---- 50.36 ------ 50.36 ------成功</br>
 * 初始化--- 40000.11 ---- 40050.47 --- 40050.47----成功</br>
 * 下发---- -40000.59 --- 49.88 ------- 49.88 ------失败</br>
 * 入款---- 40000.11 ---- 40049.99 ---- 40050.47 ---失败</br>
 * 根据数据可以看出</br>
 * 初始化数据[40000.11]与 入款数据[40000.11] 一模一样，</br>
 * 初始化有问题，此类主要针对此场景进行修正</br>
 * 1.把此初始化数据[40000.11] 更改为入款数据[40000.11]</br>
 * 2.把入款数据[40000.11] 修改为一条初始化数据，初始化金额变为0
 * 
 */
@PatchAnnotation(PatchAction.PREFIX_PATCH_ACTION + PatchAction.SORT_ACTION_TYPE_Init0)
public class Init0PatchAction extends PatchAction {
	protected static final Logger logger = LoggerFactory.getLogger(Sort0PatchAction.class);

	/**
	 * @return {@code false} 没有修正过 或 修正过且修正不成功 </br>
	 *         {@code true } 修正过且修正不成功
	 */
	public boolean deal(StringRedisTemplate template, BizAccount account, ReportCheck check) {
		if (Objects.isNull(check) || Objects.isNull(check.getTarget()) || Objects.isNull(check.getBase())
				|| check.getCount() == 0)
			return false;
		List<BizSysLog> sysList = storeHandler.findSysLogFromCache(check.getTarget());
		if (CollectionUtils.isEmpty(sysList))
			return false;
		Integer[] indexArray = computeIndex(sysList);
		if (indexArray[0] < 0 || indexArray[1] < 0)
			return false;
		int probIndex = indexArray[0], initIndex = indexArray[1];
		BigDecimal diff = sysList.get(probIndex).getAmount().negate();
		List<BizSysLog> cloned = clone(diff, probIndex, initIndex, sysList);
		List<BizSysLog> changed = sort(diff, probIndex, initIndex, sysList);
		storeHandler.updateByBatch(check.getBase(), diff, changed, cloned);
		logger.info("SB{} [  PATCH INIT0  ] >>  msg:{}", check.getTarget(), ObjectMapperUtils.serialize(changed));
		return true;
	}

	/**
	 * 复制
	 */
	private List<BizSysLog> clone(BigDecimal diff, int probIndex, int initIndex, List<BizSysLog> targetList) {
		List<BizSysLog> ret = new ArrayList<>();
		for (int index = 0; index <= initIndex; index++) {
			BizSysLog item = targetList.get(index);
			if (Objects.isNull(item))
				continue;
			BigDecimal sysBal = SysBalUtils.radix2(item.getBalance());
			BigDecimal bankBal = SysBalUtils.radix2(item.getBankBalance());
			if (index == probIndex || index == initIndex || sysBal.add(diff).compareTo(bankBal) == 0) {
				BizSysLog cloned = item.clone();
				if (Objects.nonNull(cloned))
					ret.add(cloned);
			}
		}
		return ret;
	}

	/**
	 * 数据处理
	 */
	private List<BizSysLog> sort(BigDecimal diff, int probIndex, int initIndex, List<BizSysLog> targetList) {
		List<BizSysLog> result = new ArrayList<>();
		BizSysLog prob = targetList.get(probIndex);
		BizSysLog init = targetList.get(initIndex);
		{// 系统余额交换
			BigDecimal tmp = prob.getBalance();
			prob.setBalance(init.getBalance());
			init.setBalance(tmp);
		}
		{// 创建时间交换
			Date tmp = prob.getCreateTime();
			prob.setCreateTime(init.getCreateTime());
			init.setCreateTime(tmp);
		}
		{// 更新时间交换
			Date tmp = prob.getUpdateTime();
			prob.setUpdateTime(init.getUpdateTime());
			init.setUpdateTime(tmp);
		}
		{// 确认时间交换
			Date tmp = prob.getSuccessTime();
			prob.setSuccessTime(init.getSuccessTime());
			init.setSuccessTime(tmp);
		}
		{// ID交换
			Long tmp = prob.getId();
			prob.setId(init.getId());
			init.setId(tmp);
		}
		init.setBalance(init.getBankBalance());
		init.setAmount(BigDecimal.ZERO);
		init.setSummary("[修正0:" + diff + "]" + StringUtils.trimToEmpty(init.getSummary()));
		result.add(prob);
		result.add(init);
		for (int index = 0; index <= initIndex; index++) {
			if (index == probIndex || index == initIndex) {
				continue;
			}
			BizSysLog item = targetList.get(index);
			if (Objects.isNull(item))
				continue;
			BigDecimal sysBal = SysBalUtils.radix2(item.getBalance());
			BigDecimal bankBal = SysBalUtils.radix2(item.getBankBalance());
			if (sysBal.add(diff).compareTo(bankBal) == 0) {
				item.setBalance(item.getBalance().add(diff));
				result.add(item);
			}
		}
		return result;
	}

	/**
	 * @return [0] 有问题数据，[1] 初始化位置
	 */
	private Integer[] computeIndex(List<BizSysLog> lgList) {
		int probIndex = -1, initIndex = -1, l = lgList.size();
		// 找出最近初始化的Index
		for (int index = 0; index < l; index++) {
			// 只考虑最近12条记录
			if (index > 12)
				break;
			BizSysLog lg = lgList.get(index);
			// 找出最近一条初始化记录
			if (Objects.equals(lg.getType(), SysLogType.Init.getTypeId())) {
				initIndex = index;
				break;
			}
		}
		// 没有找到：直接返回
		if (initIndex < 0)
			return new Integer[] { -1, -1 };
		// 找出有问题的Index
		BigDecimal initAmt = SysBalUtils.radix2(lgList.get(initIndex).getAmount());
		BigDecimal initBankBal = SysBalUtils.radix2(lgList.get(initIndex).getBankBalance());
		for (int index = 0; index < initIndex; index++) {
			BigDecimal amt = SysBalUtils.radix2(lgList.get(index).getAmount());
			BigDecimal bankBal = SysBalUtils.radix2(lgList.get(index).getBankBalance());
			if (initAmt.compareTo(amt) == 0 && initBankBal.compareTo(bankBal) == 0) {
				probIndex = index;
				break;
			}
		}
		// 没有找到有问题的记录：直接返回
		if (probIndex < 0)
			return new Integer[] { -1, -1 };
		// 有问题的记录 与 初始化记录 相距过大 {@code 7}:直接返回
		if (initIndex - probIndex > 7)
			return new Integer[] { -1, -1 };
		return new Integer[] { probIndex, initIndex };
	}
}
