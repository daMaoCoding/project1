package com.xinbo.fundstransfer.report.fail.classify;

import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalTrans;
import com.xinbo.fundstransfer.domain.pojo.TransferEntity;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.acc.SysAccPush;
import com.xinbo.fundstransfer.report.fail.*;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import org.apache.shiro.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 根据工具端实体上报中，含有失败关键字，确认转账失败
 */
@FailAnnotation(FailCheck.PREFIX_FAIL_CHECK + Common.WATCHER_4_KEY_WORDS)
public class KeyWordFailCheck extends FailCheck {
	protected static final Logger logger = LoggerFactory.getLogger(KeyWordFailCheck.class);

	@Override
	protected boolean deal(StringRedisTemplate template, AccountBaseInfo base, FailHandler handler, EntityNotify param,
			ReportCheck check) {
		if (Objects.isNull(template) || Objects.isNull(handler) || Objects.isNull(param) || Objects.isNull(check)
				|| Objects.isNull(param.getEntity()))
			return false;
		TransferEntity entity = param.getEntity();
		if (Objects.nonNull(entity.getRemark()) && entity.getRemark().contains("等待")
				&& entity.getRemark().contains("无结果转出") && Objects.nonNull(entity.getTaskId())) {
			long TASK_ID = SysBalUtils.taskId(entity);
			BigDecimal TR_AMT = SysBalUtils.transAmt(entity);
			int TO_ID = SysBalUtils.toId(entity);
			String TO_ACC_3 = SysBalUtils.last3letters(entity.getAccount());
			List<SysBalTrans> dataList = check.getTransOutAll()
					.stream().filter(p -> Objects.equals(TO_ACC_3, p.getToAcc3Last())
							&& TR_AMT.compareTo(p.getAmt()) == 0 && TASK_ID == p.getTaskId() && TO_ID == p.getToId())
					.collect(Collectors.toList());
			if (!CollectionUtils.isEmpty(dataList)) {
				handler.fail(template, dataList.get(0), null, SysBalUtils.autoRemark("等待无结果转出"));
			}
			return true;
		}
		return false;
	}
}
