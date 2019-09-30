package com.xinbo.fundstransfer.report.up.classify;

import com.xinbo.fundstransfer.domain.entity.BizOutwardTask;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.xinbo.fundstransfer.domain.pojo.SysBalPush;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import com.xinbo.fundstransfer.report.up.Report;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import com.xinbo.fundstransfer.report.up.ReportUp;

import java.util.Objects;

/**
 * 上报处理： 入款卡：人工出款转主管处理
 */
@ReportUp(Report.REPORT_UP + Report.ACC_TYPE_INBANK + SysBalPush.CLASSIFY_BANK_MAN_MGR)
public class InBankManMgrReport extends Report {

	@Override
	protected void deal(StringRedisTemplate template, String rpushData, ReportCheck check) throws Exception {
		SysBalPush<BizOutwardTask> data = ObjectMapperUtils.deserialize(rpushData,
				new TypeReference<SysBalPush<BizOutwardTask>>() {
				});
		if (Objects.isNull(data) || Objects.isNull(data.getData()) || Objects.isNull(data.getData().getAccountId())
				|| Objects.isNull(check))
			return;
		outward2Mgr4Man(template, data.getData(), check);
	}
}