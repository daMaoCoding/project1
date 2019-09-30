package com.xinbo.fundstransfer.report.up.classify;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.xinbo.fundstransfer.FeeUtil;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import org.apache.shiro.util.CollectionUtils;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalPush;
import com.xinbo.fundstransfer.domain.pojo.SysBalTrans;
import com.xinbo.fundstransfer.domain.pojo.TransferEntity;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import com.xinbo.fundstransfer.report.up.Report;
import com.xinbo.fundstransfer.report.up.ReportUp;

/**
 * 上报处理： 入款卡：转账结果上报 -- 仅作为计算出款流水、系统余额和银行余额；该系统流水挂起。
 */
@ReportUp(Report.REPORT_UP + Report.ACC_TYPE_INBANK + SysBalPush.CLASSIFY_TRANSFER)
public class InBankTransReport extends Report {

	@Override
	protected void deal(StringRedisTemplate template, String rpushData, ReportCheck check) throws Exception {
		SysBalPush<TransferEntity> data = ObjectMapperUtils.deserialize(rpushData,
				new TypeReference<SysBalPush<TransferEntity>>() {
				});
		if (Objects.isNull(data) || Objects.isNull(data.getData()))
			return;
		AccountBaseInfo base = accSer.getFromCacheById(data.getTarget());
		if (Objects.isNull(base))
			return;
		TransferEntity entity = data.getData();
		if (Objects.isNull(entity.getAmount()))
			return;
		// 判断 该转账实体是否有效，无效则表示，该转账失败，直接丢弃
		int FR_ID = SysBalUtils.frId(entity), TO_ID = SysBalUtils.toId(entity);
		String TO_ACC_3 = SysBalUtils.last3letters(entity.getAccount());
		BigDecimal TR_AMT = SysBalUtils.transAmt(entity);
		long TASK_ID = SysBalUtils.taskId(entity);
		String TO_OWN_2 = SysBalUtils.last2letters(entity.getOwner());
		reRegist(template, entity, TASK_ID, TR_AMT, FR_ID, TO_ID, TO_ACC_3, TO_OWN_2, check);
		if (failHandler.invalid(base, entity, check)) {
			log.info("SB{} SB{} [ TRANSFER ENTITY INVALID ] >> before: {}  after: {}  amount: {} 2Acc3: {}", FR_ID,
					TO_ID, SysBalUtils.beforeBal(entity), SysBalUtils.afterBal(entity), TR_AMT, TO_ACC_3);
			return;
		}
		if (!successHandler.inbankOutByEntity(template, base, entity, check)) {
			if (!successHandler.yunSFInOutByEntity(template, base, entity, check))
				successHandler.commonResultEq1(template, base, entity, check);
		}
		benchmark(template, base.getId(), SysBalUtils.afterBal(entity));
	}
}
