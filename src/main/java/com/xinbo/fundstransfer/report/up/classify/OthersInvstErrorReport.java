package com.xinbo.fundstransfer.report.up.classify;

import com.fasterxml.jackson.core.type.TypeReference;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalPush;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import com.xinbo.fundstransfer.report.up.Report;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import com.xinbo.fundstransfer.report.up.ReportInvstError;
import com.xinbo.fundstransfer.report.up.ReportUp;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Objects;

@ReportUp(Report.REPORT_UP + Report.ACC_TYPE_OTHERS + SysBalPush.CLASSIFY_INVST_ERROR)
public class OthersInvstErrorReport extends Report {

	@Override
	protected void deal(StringRedisTemplate template, String rpushData, ReportCheck check) throws Exception {
		SysBalPush<ReportInvstError> data = ObjectMapperUtils.deserialize(rpushData,
				new TypeReference<SysBalPush<ReportInvstError>>() {
				});
		if (Objects.isNull(data) || Objects.isNull(data.getData()))
			return;
		ReportInvstError param = data.getData();
		AccountBaseInfo base = accSer.getFromCacheById(param.getAccId());
		SysUser operator = userSer.findFromCacheById(param.getOperator());
		storeHandler.invstError(base, operator, param);
		log.info(
				"SB{} [ OTHERS INVST ERROR REPORT  ] -> sysBal: {} bankBal: {} diff: {} operator: {} errorId: {} remark: {}",
				param.getAccId(), param.getSysBal(), param.getBankBal(), param.getDiff(), param.getOperator(),
				param.getErrorId(), param.getRemark());
	}
}
