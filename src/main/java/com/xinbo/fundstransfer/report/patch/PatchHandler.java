package com.xinbo.fundstransfer.report.patch;

import com.xinbo.fundstransfer.report.up.ReportCheck;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class PatchHandler extends ApplicationObjectSupport {
	private static final Map<String, PatchAction> dealMap = new LinkedHashMap<>();

	@PostConstruct
	private void init() {
		Map<String, Object> map = super.getApplicationContext().getBeansWithAnnotation(PatchAnnotation.class);
		map.forEach((k, v) -> dealMap.put(k, (PatchAction) v));
	}

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
	public boolean init0(StringRedisTemplate template, ReportCheck reportCheck) {
		return dealMap.get(PatchAction.PREFIX_PATCH_ACTION + PatchAction.SORT_ACTION_TYPE_Init0).deal(template, null,
				reportCheck);
	}

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
	public boolean init1(StringRedisTemplate template, ReportCheck reportCheck) {
		return dealMap.get(PatchAction.PREFIX_PATCH_ACTION + PatchAction.SORT_ACTION_TYPE_Init1).deal(template, null,
				reportCheck);
	}

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
	public boolean sort0(StringRedisTemplate template, ReportCheck reportCheck) {
		return dealMap.get(PatchAction.PREFIX_PATCH_ACTION + PatchAction.SORT_ACTION_TYPE_SORT0).deal(template, null,
				reportCheck);
	}

	/**
	 * 银行有季度结息，但是无结息流水，处理</Br>
	 * 季度末月的20日为结息日，次日付息。 结息日分别为: 3月20日 6月20日 9月20日 12月20日,入账日期为21日
	 */
	public boolean quarterInterest(StringRedisTemplate template, ReportCheck reportCheck) {
		return dealMap.get(PatchAction.PREFIX_PATCH_ACTION + PatchAction.SORT_ACTION_TYPE_QuarterInterest)
				.deal(template, null, reportCheck);
	}

	/**
	 * 收款方 确认收款相同金额 （1.按照余额确认，2.按照流水确认），确认后，银行余额与系统余额之间存在差额，差额等与该确认金额。
	 * 则：按照余额确认的订单，确认失败。
	 */
	public boolean depositSameAmount(StringRedisTemplate template, ReportCheck reportCheck) {
		return dealMap.get(PatchAction.PREFIX_PATCH_ACTION + PatchAction.SORT_ACTION_TYPE_DepositSameAmount)
				.deal(template, null, reportCheck);
	}

	public boolean yunSFAbsentFlow(StringRedisTemplate template, ReportCheck reportCheck) {
		return dealMap.get(PatchAction.PREFIX_PATCH_ACTION + PatchAction.SORT_ACTION_TYPE_YunSFAbsentFlow)
				.deal(template, null, reportCheck);
	}

	public boolean yunSFInomeSameAmount(StringRedisTemplate template, ReportCheck reportCheck) {
		return dealMap.get(PatchAction.PREFIX_PATCH_ACTION + PatchAction.SORT_ACTION_TYPE_YunSFInomeSameAmount)
				.deal(template, null, reportCheck);
	}
}
