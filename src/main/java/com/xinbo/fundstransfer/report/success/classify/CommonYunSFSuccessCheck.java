package com.xinbo.fundstransfer.report.success.classify;

import com.xinbo.fundstransfer.domain.entity.BizSysLog;
import com.xinbo.fundstransfer.domain.enums.InBankSubType;
import com.xinbo.fundstransfer.domain.enums.SysLogType;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.success.SuccessAnnotation;
import com.xinbo.fundstransfer.report.success.SuccessCheck;
import com.xinbo.fundstransfer.report.success.SuccessHandler;
import com.xinbo.fundstransfer.report.success.SuccessParam;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 云闪付对账特殊处理 <br>
 * 处理以下场景：</br>
 * 2019-05-28 10:21:22 转账1 sysBal:300 bankBal:300 对账TRUE</br>
 * 2019-05-28 10:24:24 转账2 sysBal:500 bankBal:300 对账FAIL</br>
 * 2019-05-28 10:27:27 转账3 sysBal:100 bankBal:500 对账FAIL</br>
 * 2019-05-28 10:30:30 转账4 sysBal:120 bankBal:120 对账TRUE</br>
 * 则：</br>
 * 转账2，转账3 成功</br>
 */
@SuccessAnnotation(SuccessCheck.PREFIX_SUCCESS_CHECK + SuccessCheck.SUCCESS_CHECK_TYPE_COMMON_YUNSF)
public class CommonYunSFSuccessCheck extends SuccessCheck {

	protected boolean deal(StringRedisTemplate template, AccountBaseInfo base, SuccessHandler handler,
			SuccessParam param, ReportCheck check) {
		if (true) {
			return false;
		}
		// 校验:参数空
		if (Objects.isNull(template) || Objects.isNull(base) || Objects.isNull(handler) || Objects.isNull(param)
				|| Objects.isNull(check))
			return false;
		// 校验:卡类型 1.入款卡 2.云闪付
		if (!Objects.equals(base.getType(), InBank)
				&& !(Objects.equals(base.getSubType(), InBankSubType.IN_BANK_YSF.getSubType())
						|| Objects.equals(base.getSubType(), InBankSubType.IN_BANK_YSF_MIX.getSubType())))
			return false;
		// 校验:是否对账成功过
		if (!check.getCheckAny())
			return false;
		List<BizSysLog> checkHis = storeHandler.findSysLogFromCache(base.getId());
		// 校验：数据库记录是否为空
		if (CollectionUtils.isEmpty(checkHis))
			return true;
		compute(checkHis);
		return true;
	}

	/**
	 * @param checkHis
	 *            系统账目历史数据
	 */
	private void compute(List<BizSysLog> checkHis) {
		checkHis = checkHis.stream().sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o2.getId() - o1.getId()))
				.collect(Collectors.toList());
		List<BizSysLog> needList = new ArrayList<>();
		boolean beginCal = false;
		BigDecimal calNextBal = null;
		for (BizSysLog his : checkHis) {
			if (Objects.isNull(his.getBalance()) || Objects.isNull(his.getBankBalance()))
				continue;
			BigDecimal sysBal = SysBalUtils.radix2(his.getBalance());
			BigDecimal bankBal = SysBalUtils.radix2(his.getBankBalance());
			BigDecimal amt = SysBalUtils.radix2(his.getAmount());
			boolean check = bankBal.compareTo(sysBal) == 0;
			if (check) {
				calNextBal = Objects.isNull(calNextBal) ? sysBal.subtract(amt) : calNextBal.subtract(amt);
			}
			beginCal = beginCal || check;
			// 数据有效性校验
			if (check || !beginCal)
				continue;
			// 退出循环条件：
			// 1. 遇到初始化的系统账目
			// 2. 遇到转出记录(转出记录非云闪付工具上报，而是有APP工具抓取，能抓取到流水)
			if (Objects.equals(his.getType(), SysLogType.Init.getTypeId()))
				break;
			if (calNextBal.compareTo(sysBal) == 0) {
				calNextBal = calNextBal.subtract(his.getAmount());
				if (his.getAmount().compareTo(BigDecimal.ZERO) > 0)
					needList.add(his);
			} else {
				break;
			}
			needList.add(his);
		}
		// 操作对账不成功的数据:系统余额赋值给银行余额
		for (BizSysLog his : needList) {
			his.setBankBalance(his.getBalance());
			storeHandler.saveAndFlush(his);
			logger.info("SB{} [ YUNSF ] -> amt: {} oppAccount: {} oppOwner: {} sysLogId: {} ", his.getAccountId(),
					his.getAmount(), his.getOppAccount(), his.getOppOwner(), his.getId());
		}
	}
}
