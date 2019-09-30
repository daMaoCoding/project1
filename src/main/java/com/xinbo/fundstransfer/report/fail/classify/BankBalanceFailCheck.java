package com.xinbo.fundstransfer.report.fail.classify;

import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalTrans;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.fail.*;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 根据余额上报确认 系统订单失败原理 </br>
 * 场景一 </br>
 * 假设存在账号A，转账前余额 8000.00， 转账 2000.00 </br>
 * 1.2019-04-22 08:57:20 工具(账号A)从服务端取走转账任务 2000.00 </br>
 * 2.2019-04-22 08:59:10 工具(账号A)完成转账任务并上报 转账前 8000.00，转账后 00.00</br>
 * 3.2019-04-22 09:00:10 工具(账号A) 余额上报 8000.00 </br>
 * 4.2019-04-22 09:01:11 工具(账号A) 余额上报 8000.00 </br>
 * 5.2019-04-22 09:02:01 工具(账号A) 余额上报 8000.00 </br>
 * 如果出现此现象，上报余额始终等于转账前余额 则认为 工具(账号A)转账失败</br>
 * 场景二（不考虑）</br>
 * 假设存在账号A, 转账前系统余额 8000.00， 银行余额 8000.00 转账任务 2000.00</br>
 * 1.2019-04-22 08:57:20 工具(账号A)从服务端取走转账任务 2000.00 </br>
 * 2.2019-04-22 08:59:10 工具(账号A)完成转账任务并上报 转账前 8000.00，转账后 00.00</br>
 * 3.2019-04-22 09:00:10 工具(账号A) 余额上报 8000.00 </br>
 * 4.2019-04-22 09:01:11 工具(账号A) 余额上报 8000.00 </br>
 * 5.2019-04-22 09:02:01 工具(账号A) 余额上报 8000.00 </br>
 * 如果出现此场景, 上报余额时，系统余额始终等于银行余额，则任务 工具(账号A)转账失败 </br>
 */
@FailAnnotation(FailCheck.PREFIX_FAIL_CHECK + Common.WATCHER_4_BANK_BALANCE)
public class BankBalanceFailCheck extends FailCheck {
	protected static final Logger logger = LoggerFactory.getLogger(BankBalanceFailCheck.class);
	private static final long TOLERANCE_MILLIS = 40000;
	/**
	 * 银行余额上报最小时间间隔
	 */
	private static final long ADJOIN_INTERVAL_MIN = 15000;
	/**
	 * 银行余额连续等于转账前余额前的次数是
	 */
	private static final int COUNT_SERIES = 2;

	@Override
	protected boolean deal(StringRedisTemplate template, AccountBaseInfo base, FailHandler handler, EntityNotify param,
			ReportCheck check) {
		if (Objects.isNull(param.getTarId()) || Objects.isNull(base) || Objects.isNull(check))
			return false;
		String msg = EntityRecord.genMsg(Common.WATCHER_4_BANK_BALANCE, param.getBankBal(), param.getSysBal(),
				param.getOccurTime());
		push(base.getId(), msg, param.getOccurTime());
		// 余额确认失败，排除以下场景
		// 1.人工出款
		// 2.非在用卡
		if (Objects.isNull(base)
				|| Objects.equals(AccountType.OutBank.getTypeId(), base.getType()) && Objects.nonNull(base.getHolder())
						&& !Objects.equals(AppConstants.USER_ID_4_ADMIN, base.getHolder())
				|| !Objects.equals(AccountStatus.Normal.getStatus(), base.getStatus()))
			return true;
		long now = System.currentTimeMillis();
		// 获取待排查的转账任务
		// 1.没有被回冲
		// 2.汇出方|汇入方都没被确认
		// 3.转账实体工具端已经上报
		// 4.转账实体已经上报了 {@code TOLERANCE_MILLIS} 毫秒
		List<SysBalTrans> orderAxis = check.getTransOutAll().stream()
				.filter(p -> SysBalTrans.SYS_REFUND != p.getSys() && !p.ackTo() && !p.ackFr() && p.getAckTm() > 0
						&& now - p.getAckTm() > TOLERANCE_MILLIS)
				.sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o1.getAckTm() - o2.getAckTm()))
				.collect(Collectors.toList());
		// 没有待排查的转账任务：直接返回
		if (CollectionUtils.isEmpty(orderAxis))
			return true;
		List<EntityRecord> eventAxis = template.boundZSetOps(RedisKeys.genKey4SysBalLogs(base.getId()))
				.rangeByScoreWithScores(0, now).stream().map(EntityRecord::new)
				.sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o1.getOccurMillis() - o2.getOccurMillis()))
				.collect(Collectors.toList());
		// 该账号没有上报事件：直接返回
		if (CollectionUtils.isEmpty(eventAxis))
			return true;
		analyse(template, handler, eventAxis, orderAxis);
		return true;
	}

	private void analyse(StringRedisTemplate template, FailHandler failHandler, List<EntityRecord> eventAxis,
			List<SysBalTrans> targetList) {
		for (SysBalTrans ts : targetList) {
			try {
				// 余额确认（{@code Common.MILLIS_CHECK_POINT} 毫秒之内）
				List<EntityRecord> dataList = eventAxis.stream()
						.filter(p -> p.getOccurMillis() < (ts.getAckTm() + Common.MILLIS_CHECK_POINT)
								&& p.getOccurMillis() > ts.getAckTm())
						.sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o1.getOccurMillis() - o2.getOccurMillis()))
						.collect(Collectors.toList());
				List<EntityRecord> infSeries = new ArrayList<>();
				for (EntityRecord data : dataList) {
					// 1.在该笔交易后，是否进行过人工初始化系统余额
					// 2.银行余额已连续等于转账前余额前的次数是否大于{@code COUNT_SERIES}
					if (Objects.equals(Common.WATCHER_4_INIT_BAL, data.getType()) || infSeries.size() >= COUNT_SERIES)
						break;
					// 不考虑
					// 1.银行流水上报信息，这里只考虑 银行余额上报
					// 2.发生在 {@code TOLERANCE_MILLIS } 毫秒前的，余额上报信息，防止 工具上报不准确
					if (Objects.equals(Common.WATCHER_4_BANK_LOGS, data.getType())
							|| (ts.getAckTm() + TOLERANCE_MILLIS) >= data.getOccurMillis())
						continue;
					// 考虑 银行余额上报时，银行余额连续等于转账前余额，观察次数
					// 如果次数>={@code COUNT_SERIES},则：该任务失败
					if (ts.getBefore().compareTo(data.getBankBal()) == 0)
						infSeries.add(data);
					else
						break;
				}
				if (infSeries.size() >= COUNT_SERIES && ADJOIN_INTERVAL_MIN < min4AdjoinInterval(infSeries)) {
					List<String> his = infSeries.stream().map(p -> p.toString()).collect(Collectors.toList());
					failHandler.fail(template, ts, null, SysBalUtils.autoRemark("转账结果失败BAL"));
					logger.info("FAIL{} [ BANK BALANCE CONFIRM ] >> msg: {} reason-series: {} ", ts.getFrId(),
							ts.getMsg(), String.join(StringUtils.EMPTY, his));
				}
			} catch (Exception e) {
				logger.info("FAIL{} [ BANK BALANCE EXCEPTION ] >> msg: {} e: {} ", ts.getFrId(), ts.getMsg(),
						e.getMessage(), e);
			}
		}
	}

	/**
	 * 求工具上报最小时间间隔
	 */
	private Long min4AdjoinInterval(List<EntityRecord> infSeries) {
		if (CollectionUtils.isEmpty(infSeries))
			return 0L;
		Long result = infSeries.get(0).getOccurMillis();
		int l = infSeries.size();
		for (int index = 0; index < l - 1; index++)
			result = Math.min(result,
					(infSeries.get(index + 1).getOccurMillis() - infSeries.get(index).getOccurMillis()));
		return result;
	}
}
