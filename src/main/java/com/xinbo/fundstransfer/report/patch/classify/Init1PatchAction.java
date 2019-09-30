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
import java.util.*;

/**
 * 初始化导致系统对账不正确，进行修正 </br>
 * 类型-------交易金额--------系统余额------银行余额------差额-----对账结果</br>
 * 下发----- -5519.58 ---- 0900.36 ------ 1000.36 --- 100.00 ---失败</br>
 * 初始化---- -0500.00 --- 0400.36 ------ 0400.36 ---- 000.00 ---成功</br>
 * 下发------ 0500.43 ---- 0900.79 ------ 0800.79 --- -100.00 ---失败</br>
 * 入款----- -0080.00 ---- 0820.79 ------ 0720.79 --- -100.00 ---失败</br>
 * 根据数据可以看出</br>
 * 初始化数据[-0500.00]后的系统账目差额一致一直未 -100.00</br>
 * 初始化有问题，此类主要针对此场景进行修正</br>
 * 1.修改此初始化数据</br>
 * 2.把以后的系统账目的数据的系统余额以此减少100.00
 */
@PatchAnnotation(PatchAction.PREFIX_PATCH_ACTION + PatchAction.SORT_ACTION_TYPE_Init1)
public class Init1PatchAction extends PatchAction {
	protected static final Logger logger = LoggerFactory.getLogger(Init1PatchAction.class);

	/**
	 * @return {@code false} 没有修正过 或 修正过且修正不成功 </br>
	 *         {@code true } 修正过且修正不成功
	 */
	public boolean deal(StringRedisTemplate template, BizAccount account, ReportCheck check) {
		if (Objects.isNull(check) || Objects.isNull(check.getTarget()) || Objects.isNull(check.getBase())
				|| check.getCheckLast() || check.getCount() == 0)
			return false;
		List<BizSysLog> sysList = storeHandler.findSysLogFromCache(check.getTarget());
		if (CollectionUtils.isEmpty(sysList))
			return false;
		int initIndex = computeIndex(sysList);
		if (initIndex < 0)
			return false;
		List<BizSysLog> cloned = clone(initIndex, sysList);
		BizSysLog first = cloned.get(0);
		BigDecimal diff = SysBalUtils.radix2(first.getBankBalance()).subtract(first.getBalance());
		List<BizSysLog> changed = sort(diff, initIndex, sysList);
		storeHandler.updateByBatch(check.getBase(), diff, changed, cloned);
		logger.info("SB{} [  PATCH INIT1  ] >>  msg:{}", check.getTarget(), ObjectMapperUtils.serialize(changed));
		return true;
	}

	/**
	 * 计算并返回修改后的数据
	 */
	private List<BizSysLog> sort(BigDecimal diff, int initIndex, List<BizSysLog> targetList) {
		List<BizSysLog> result = new ArrayList<>();
		for (int index = 0; index < initIndex; index++) {
			BizSysLog item = targetList.get(index);
			if (Objects.isNull(item))
				continue;
			item.setBalance(item.getBalance().add(diff));
			result.add(item);
		}
		diff = SysBalUtils.radix2(diff);
		BizSysLog init = targetList.get(initIndex);
		init.setBalance(init.getBalance().add(diff));
		init.setBankBalance(init.getBalance());
		init.setSummary("[修正1:" + diff + "]" + StringUtils.trimToEmpty(init.getSummary()));
		result.add(init);
		return result;
	}

	/**
	 * 复制需要修改的数据
	 */
	private List<BizSysLog> clone(int initIndex, List<BizSysLog> targetList) {
		List<BizSysLog> result = new ArrayList<>();
		for (int index = 0; index <= initIndex; index++) {
			BizSysLog item = targetList.get(index);
			if (Objects.isNull(item))
				continue;
			BizSysLog cloned = item.clone();
			if (Objects.nonNull(cloned))
				result.add(cloned);
		}
		return result;
	}

	/**
	 * @return [0] 有问题数据，[1] 初始化位置
	 */
	private int computeIndex(List<BizSysLog> lgList) {
		int initIndex = -1, l = lgList.size();
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
			return -1;
		// 如果前一条不存在：直接返回
		if (initIndex + 1 >= l)
			return -1;
		// 检查：初始化数据的前一条数据是否账目已经对上，如果对上则不满足此场景，直接返回
		BizSysLog beforOne = lgList.get(initIndex + 1);
		if (SysBalUtils.radix2(beforOne.getBalance()).compareTo(SysBalUtils.radix2(beforOne.getBankBalance())) == 0)
			return -1;
		// 检查：初始化后的系统账目数据的差额（不能为0.00）是否一致，如果不一致，不满足此场景，直接返回
		int count = 0;
		Set<String> diff = new HashSet<>();
		for (int index = 0; index < initIndex; index++) {
			count = count + 1;
			BigDecimal sysBal = SysBalUtils.radix2(lgList.get(index).getBalance());
			BigDecimal bankBal = SysBalUtils.radix2(lgList.get(index).getBankBalance());
			if (sysBal.compareTo(bankBal) == 0) {
				diff.clear();
				break;
			}
			diff.add(SysBalUtils.radix2(bankBal.subtract(sysBal)).toString());
		}
		// 检查：初始化后的系统账目的数据量，如果只有一条数据，不满足此场景，直接返回
		if (count < 2)
			return -1;
		// 检查：不相同的差额的个数，如果个数不等于1，不满足此场景，直接返回
		if (diff.size() != 1)
			return -1;
		return initIndex;
	}
}
