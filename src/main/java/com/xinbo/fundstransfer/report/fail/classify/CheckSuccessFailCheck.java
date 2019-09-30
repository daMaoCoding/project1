package com.xinbo.fundstransfer.report.fail.classify;

import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalTrans;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.fail.*;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import org.apache.shiro.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 失败订单确认（后一笔转账成功，确认前一笔是否成功）
 */
@FailAnnotation(FailCheck.PREFIX_FAIL_CHECK + Common.WATCHER_4_CHECK_SUCCESS)
public class CheckSuccessFailCheck extends FailCheck {
	protected static final Logger logger = LoggerFactory.getLogger(CheckSuccessFailCheck.class);

	@Override
	protected boolean deal(StringRedisTemplate template, AccountBaseInfo base, FailHandler handler, EntityNotify param,
			ReportCheck check) {
		if (Objects.isNull(template) || Objects.isNull(handler) || Objects.isNull(check))
			return false;
		if (CollectionUtils.isEmpty(check.getTrans()))
			return true;
		// 找出该账号未确认的转账记录
		List<SysBalTrans> dataList = check.getTransOutAll().stream()
				.filter(p -> SysBalTrans.SYS_REFUND != p.getSys() && !p.ackFr() && !p.ackTo())
				.sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o2.getGetTm() - o1.getGetTm()))
				.collect(Collectors.toList());
		if (CollectionUtils.isEmpty(dataList))
			return true;
		// 1.计算该账号最近确认时间
		// 2.通过最近确认时间，推断失败交易订单
		check.getTrans().stream().mapToLong(SysBalTrans::getGetTm).max().ifPresent(lastTm -> {
			String checkTm = Common.yyyyMMddHHmmss.get().format(new Date(lastTm));
			dataList.stream().filter(p -> p.getGetTm() > 0 && p.getGetTm() < lastTm).forEach(p -> {
				String getTm = Common.yyyyMMddHHmmss.get().format(new Date(p.getGetTm()));
				String remark = String.format("失败,认领:%s,对账:%s", getTm, checkTm);
				handler.fail(template, p, null, SysBalUtils.autoRemark(remark));
			});
		});
		return true;
	}
}
