package com.xinbo.fundstransfer.report.init.classify;

import com.sun.org.apache.regexp.internal.RE;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.report.acc.ErrorAlarm;
import com.xinbo.fundstransfer.report.init.ActionInit;
import com.xinbo.fundstransfer.report.init.InitAnnotation;
import com.xinbo.fundstransfer.report.init.InitHandler;
import com.xinbo.fundstransfer.report.init.InitParam;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.util.CollectionUtils;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Objects;

/**
 * 根据异常ID 初始化账号信息
 */
@InitAnnotation(ActionInit.PREFIX_ACTION_INIT + ActionInit.ACTION_INIT_TYPE_InitError)
public class InitError extends ActionInit {

	@Override
	protected boolean deal(AccountBaseInfo base, InitHandler handler, InitParam param) {
		if (Objects.isNull(handler) || Objects.isNull(param))
			return false;
		if (Objects.isNull(base) && Objects.isNull(param.getErrorId()))
			return false;
		if (Objects.nonNull(base)) {// 根据账号ID初始化
			return dealByAccount(handler, base, param);
		} else {
			return dealByError(handler, param);// 根据ERROR_ID初始化
		}
	}

	private boolean dealByError(InitHandler handler, InitParam param) {
		Long errorId = Objects.requireNonNull(param.getErrorId(), "errorId is null");
		SysUser operator = Objects.requireNonNull(param.getOperator(), "操作者为空");
		String remark = Objects.requireNonNull(StringUtils.trimToNull(param.getRemark()), "备注不能为空");
		BizSysErr err = Objects.requireNonNull(sysErrDao.findOne(errorId), "该记录不存在");
		sysErrDao.delete(errorId);
		List<BizSysInvst> invstList = sysInvstDao.findByErrorId(errorId);
		if (!CollectionUtils.isEmpty(invstList))
			sysInvstDao.delete(invstList);
		if (Objects.isNull(err) || Objects.isNull(err.getTarget()))
			return true;
		accountingStringRedisTemplate.boundHashOps(RedisKeys.SYS_ACC_RUNNING).delete(String.valueOf(err.getTarget()));
		accountingStringRedisTemplate.boundHashOps(RedisKeys.ACC_SYS_INIT).delete(String.valueOf(err.getTarget()));
		BizAccount acc = Objects.requireNonNull(accDao.findById2(err.getTarget()), "账号不存在");
		storeHandler.init(acc, remark,operator);
		handler.recordToFail(acc);
		acc.setBalance(acc.getBankBalance());
		accDao.saveAndFlush(acc);
		return true;
	}

	private boolean dealByAccount(InitHandler handler, AccountBaseInfo base, InitParam param) {
		SysUser operator = Objects.requireNonNull(param.getOperator(), "操作者为空");
		base = Objects.requireNonNull(base, "该账号不存在");
		String target = String.valueOf(base.getId()), remark = "--";
		ErrorAlarm error = new ErrorAlarm(
				(String) accountingStringRedisTemplate.boundHashOps(RedisKeys.SYS_ACC_RUNNING).get(target));
		if (error.getErrorId() != 0) {
			param.setErrorId(error.getErrorId());
			param.setOperator(operator);
			param.setRemark(remark);
			dealByError(handler, param);
		} else {
			accountingStringRedisTemplate.boundHashOps(RedisKeys.SYS_ACC_RUNNING).delete(target);
			accountingStringRedisTemplate.boundHashOps(RedisKeys.ACC_SYS_INIT).delete(target);
			BizAccount acc = Objects.requireNonNull(accDao.findById2(base.getId()), "账号不存在");
			storeHandler.init(acc, remark,operator);
			handler.recordToFail(acc);
			acc.setBalance(acc.getBankBalance());
			accDao.saveAndFlush(acc);
		}
		return false;
	}
}
