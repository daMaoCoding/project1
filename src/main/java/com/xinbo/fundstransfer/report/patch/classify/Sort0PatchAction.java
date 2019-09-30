package com.xinbo.fundstransfer.report.patch.classify;

import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizSysLog;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.patch.PatchAction;
import com.xinbo.fundstransfer.report.patch.PatchAnnotation;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import org.apache.shiro.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.*;

/**
 * 排序场景：</br>
 * 银行交易过程：</br>
 * 交易金额-----银行余额</br>
 * 1.-50 -----258.46 </br>
 * 2.664.83---923.29 </br>
 * 3.-50 -----873.29</br>
 * 4.-407.56--465.73</br>
 * 系统账目顺序：</br>
 * 交易金额------银行余额-----系统余额------差额</br>
 * 1.-50 ----- 258.46 ---- 258.46 ----- 0</br>
 * 2.-50 ----- 208.46 ---- 873.29 ----- 664.83</br>
 * 3.-407.56-- -191.1 ---- 465.73 ----- 664.83</br>
 * 4.664.83--- 465.73 ---- 923.29 ----- 457.56</br>
 * 满足条件：</br>
 * 1.一笔或多笔转出 </br>
 * 2.只有一笔转出
 */
@PatchAnnotation(PatchAction.PREFIX_PATCH_ACTION + PatchAction.SORT_ACTION_TYPE_SORT0)
public class Sort0PatchAction extends PatchAction {
	protected static final Logger logger = LoggerFactory.getLogger(Sort0PatchAction.class);
	private static int PERIOD_MAX_INDEX = 50;

	/**
	 * @return {@code false} 没有排序 或 排过序且排序不成功 </br>
	 *         {@code true } 排过序且排序成功
	 */
	public boolean deal(StringRedisTemplate template, BizAccount account, ReportCheck check) {
		if (Objects.isNull(check) || Objects.isNull(check.getTarget()) || check.getCount() == 0)
			return false;
		List<BizSysLog> sysList = storeHandler.findSysLogFromCache(check.getTarget());
		if (CollectionUtils.isEmpty(sysList))
			return false;
		boolean result = false;
		int ref = 0, l = sysList.size();
		while (ref < PERIOD_MAX_INDEX && ref < l) {
			Integer[] indexArray = computeIndex(ref, sysList);
			if (indexArray[0] >= 0 && indexArray[1] >= 0) {
				List<BizSysLog> cloned = clone(indexArray[0], indexArray[1], sysList);
				List<BizSysLog> sorted = sort(indexArray[0], indexArray[1], sysList);
				if (cloned.size() != sorted.size()) {
					ref = indexArray[2] + 1;
					continue;
				}
				storeHandler.updateByBatch(check.getBase(), null, sorted, cloned);
				logger.info("SB{} [ PATCH SORT0 ] >> msg:{}", check.getTarget(), ObjectMapperUtils.serialize(sorted));
				result = true;
			}
			ref = indexArray[2] + 1;
		}
		return result;
	}

	/**
	 * 复制
	 */
	private List<BizSysLog> clone(int stIndex, int edIndex, List<BizSysLog> targetList) {
		List<BizSysLog> ret = new ArrayList<>();
		for (int index = stIndex; index < edIndex; index++) {
			BizSysLog item = targetList.get(index);
			if (Objects.isNull(item))
				continue;
			BizSysLog clone = item.clone();
			if (Objects.nonNull(clone))
				ret.add(clone);
		}
		return ret;
	}

	/**
	 * 排序
	 * 
	 * @param stIndex
	 *            排序开始位置
	 * @param edIndex
	 *            排序结束位置
	 * @param targetList
	 *            排序对象
	 * @return 排序后变动的BizSysLog结果集
	 */
	private List<BizSysLog> sort(int stIndex, int edIndex, List<BizSysLog> targetList) {
		BizSysLog head = targetList.get(stIndex);
		long refId = head.getId();
		Date refTm = head.getSuccessTime();
		List<BizSysLog> chgList = new ArrayList<>();
		for (int index = stIndex + 1; index < edIndex; index++) {
			BizSysLog item = targetList.get(index);
			{// ID交换
				long tmp = item.getId();
				item.setId(refId);
				refId = tmp;
			}
			refTm = item.getSuccessTime();
			chgList.add(item);
		}
		head.setId(refId);
		head.setSuccessTime(refTm);
		chgList.add(head);
		BizSysLog end = targetList.get(edIndex);
		{// 重新推算系统余额
			ListIterator<BizSysLog> iterator = chgList.listIterator(chgList.size());
			while (iterator.hasPrevious()) {
				BizSysLog p = iterator.previous();
				p.setBalance(end.getBalance().add(p.getAmount()));
				end = p;
			}
		}
		return chgList;
	}

	/**
	 * @return [0] 交换开始位置，[1] 交换结束位置，[2] 运行位置
	 */
	private Integer[] computeIndex(int refIndex, List<BizSysLog> lgList) {
		BigDecimal chgAmt = BigDecimal.ZERO;
		int size = lgList.size(), stIndex = -1, edIndex_1 = -1, calIndex = 0;
		for (int index = refIndex; index < size; index++) {
			calIndex = index;
			if (index > PERIOD_MAX_INDEX || index >= size)
				break;
			BizSysLog lg = lgList.get(index);
			BigDecimal sysBal = SysBalUtils.radix2(lg.getBalance());
			BigDecimal bankBal = SysBalUtils.radix2(lg.getBankBalance());
			// 过滤掉：从开头持续对账成功的系统账目
			if (stIndex == -1 && sysBal.compareTo(bankBal) == 0) {
				continue;
			}
			// 对账不成功的最近一笔 是转出记录：在不满足此场景
			BigDecimal transAmount = SysBalUtils.radix2(lg.getAmount());
			if (stIndex == -1 && transAmount.compareTo(BigDecimal.ZERO) < 0) {
				break;
			}
			// 对账不成功的最近一笔 是转入记录：交换开始位置
			if (stIndex == -1 && transAmount.compareTo(BigDecimal.ZERO) > 0) {
				stIndex = index;
				chgAmt = SysBalUtils.radix2(lg.getAmount());
				continue;
			}
			// 交换位置的前一笔：银行余额与系统余额的差额 不等于 交换位置的 转入金额 ，不满足此排序场景
			if (edIndex_1 == -1 && bankBal.subtract(sysBal).compareTo(chgAmt) != 0) {
				break;
			}
			// 交换位置的前一笔：银行余额与系统余额的差额 等于 换位置的 转入金额 ，则：end_1 = index;
			if (edIndex_1 == -1 && bankBal.subtract(sysBal).compareTo(chgAmt) == 0) {
				edIndex_1 = index;
				continue;
			}
			// 交换位置的前第二笔起：银行余额与系统余额的差额 不等于 换位置的 转入金额 且 银行余额不等于系统余额，则：不满足此排序场景
			if (edIndex_1 > 0 && bankBal.subtract(sysBal).compareTo(chgAmt) != 0 && bankBal.compareTo(sysBal) != 0) {
				break;
			}
			edIndex_1 = index;
			if (bankBal.compareTo(sysBal) == 0) {
				break;
			}
		}
		if (stIndex == -1 || edIndex_1 == -1) {
			return new Integer[] { -1, -1, calIndex };
		}
		BizSysLog lg = lgList.get(edIndex_1);
		BigDecimal sysBal = SysBalUtils.radix2(lg.getBalance()), bankBal = SysBalUtils.radix2(lg.getBankBalance());
		if (sysBal.compareTo(bankBal) == 0) {
			if (edIndex_1 - 1 < 0) {
				return new Integer[] { -1, -1, calIndex };
			}
			lg = lgList.get(edIndex_1 - 1);
			sysBal = SysBalUtils.radix2(lg.getBalance());
			bankBal = SysBalUtils.radix2(lg.getBankBalance());
			if (bankBal.subtract(sysBal).compareTo(chgAmt) == 0) {
				return new Integer[] { stIndex, edIndex_1, calIndex };
			} else {
				return new Integer[] { -1, -1, calIndex };
			}
		} else if (bankBal.subtract(sysBal).compareTo(chgAmt) == 0) {
			return new Integer[] { stIndex, edIndex_1, calIndex };
		} else {
			return new Integer[] { -1, -1, calIndex };
		}
	}
}
