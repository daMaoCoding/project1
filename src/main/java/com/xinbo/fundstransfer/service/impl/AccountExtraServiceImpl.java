package com.xinbo.fundstransfer.service.impl;

import com.xinbo.fundstransfer.domain.enums.AccountFlag;
import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.repository.AccountExtraRepository;
import com.xinbo.fundstransfer.service.*;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 账号操作记录
 *
 * @author
 */
@Service
public class AccountExtraServiceImpl implements AccountExtraService {
	@Autowired
	private AccountExtraRepository accountExtraRespository;
	@Autowired
	@Lazy
	private AccountService accountService;
	@Autowired
	private HandicapService handicapService;

	/**
	 * 存储账号操作记录
	 */
	@Override
	@Transactional
	public void insertRow(BizAccountExtra accountExtra) {
		accountExtraRespository.save(accountExtra);
	}

	@Override
	@Transactional
	public void addAccountExtraLog(Integer accountId, String Uid) {
		BizAccount account = accountService.getById(accountId);
		if (null == account) {
			return;
		}
		BizAccountExtra accountExtra = new BizAccountExtra();
		accountExtra.setTime(new Date());
		accountExtra.setAccountId(account.getId());
		if (StringUtils.isEmpty(Uid)) {
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			Uid = operator.getUid();
		}
		accountExtra.setOperator(Uid);
		String remark = "";
		if (null != account.getHandicapId()) {
			BizHandicap handicap = handicapService.findFromCacheById(account.getHandicapId());
			if (null != handicap && StringUtils.isNotEmpty(handicap.getName()))
				remark += "【盘口】" + handicap.getName() + "\r\n";
		}
		if (StringUtils.isNotEmpty(account.getLevelNameToGroup())) {
			remark += "【层级】" + account.getLevelNameToGroup() + "\r\n";
		}
		remark += "【账号信息】";
		if (StringUtils.isNotEmpty(account.getBankType())) {
			remark += account.getBankType();
		}
		if (StringUtils.isNotEmpty(account.getOwner())) {
			remark += "|" + account.getOwner();
		}
		if (StringUtils.isNotEmpty(account.getAccount())) {
			remark += "|" + CommonUtils.hideAccountAll(account.getAccount(), null);
		}
		remark += "\r\n";
		if (StringUtils.isNotEmpty(account.getBankName())) {
			remark += "【支行】" + account.getBankName() + "\r\n";
		}
		if (StringUtils.isNotEmpty(account.getAlias())) {
			remark += "【编号】" + account.getAlias()
					+ (StringUtils.isNotEmpty(account.getCurrSysLevelName()) ? "-" + account.getCurrSysLevelName() : "")
					+ "\r\n";
		}
		if (null != account.getCurrSysLevel()) {
			remark += "【内外层】" + getStrByCurrLevelId(account.getCurrSysLevel()) + "\r\n";
		}
		if (StringUtils.isNotEmpty(account.getStatusStr())) {
			remark += "【状态】" + account.getStatusStr() + "\r\n";
		}
		if (StringUtils.isNotEmpty(account.getTypeStr())) {
			remark += "【类型】" + account.getTypeStr() + "\r\n";
		}
		if (null != account.getSubType()) {
			remark += "【子类型】" + getStrBySubType(account.getSubType()) + "\r\n";
		}
		if (null != account.getLimitIn()) {
			remark += "【入款限额】" + account.getLimitIn() + "\r\n";
		}
		if (null != account.getLimitOut()) {
			remark += "【出款限额】" + account.getLimitOut() + "\r\n";
		}
		if (null != account.getLowestOut()) {
			remark += "【最低余额限制】" + account.getLowestOut() + "\r\n";
		}
		if (null != account.getPeakBalance()) {
			remark += "【余额峰值】" + account.getPeakBalance() + "\r\n";
		}
		if (null != account.getLimitBalance()) {
			remark += "【余额告警】" + account.getLimitBalance() + "\r\n";
		}
		if (null != account.getBankBalance()) {
			remark += "【银行余额】" + account.getBankBalance() + "\r\n";
		}
		if (StringUtils.isNotEmpty(account.getRemark())) {
			remark += "【备注】" + account.getRemark() + "\r\n";
		}
		if (null != account.getFlag()) {
			remark += "【用途】" + account.getFlag() + "\r\n";
			if (account.getFlag().equals(1) && StringUtils.isNotEmpty(account.getMobile())) {
				remark += "【手机号】" + CommonUtils.hideAccountAll(account.getMobile(), "phone") + "\r\n";
			}
		}
		if (StringUtils.isEmpty(remark)) {
			return;
		}
		accountExtra.setRemark(remark);
		insertRow(accountExtra);
	}

	@Override
	public Page<BizAccountExtra> findAll(Specification<BizAccountExtra> specification, PageRequest pageRequest) {
		return accountExtraRespository.findAll(specification, pageRequest);
	}

	/**
	 * 保存账号操作信息
	 *
	 * @param oldAccount
	 * @param newAccount
	 * @param Uid
	 */
	@Transactional
	public void saveAccountExtraLog(BizAccount oldAccount, BizAccount newAccount, String Uid) {
		if (null == oldAccount || null == newAccount) {
			return;
		}
		BizAccountExtra accountExtra = new BizAccountExtra();
		accountExtra.setTime(new Date());
		accountExtra.setAccountId(newAccount.getId());
		if (StringUtils.isEmpty(Uid)) {
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			Uid = operator.getUid();
		}
		accountExtra.setOperator(Uid);
		String remark = "";
		if (StringUtils.isNotBlank(newAccount.getRemark4Extra())) {
			//储存操作记录说明用，想储存一段自定义文字却
			remark += newAccount.getRemark4Extra();
		}
		if (accountInfoIsUpdate(oldAccount.getStatus(), newAccount.getStatus())) {
			remark += "状态 " + AccountStatus.findByStatus(oldAccount.getStatus()).getMsg() + " 改为 "
					+ AccountStatus.findByStatus(newAccount.getStatus()).getMsg() + ";\r\n";
		}
		if (accountInfoIsUpdate(oldAccount.getAccount(), newAccount.getAccount())) {
			remark += "账号  " + CommonUtils.hideAccountAll(oldAccount.getAccount(), null) + " 改为 "
					+ CommonUtils.hideAccountAll(newAccount.getAccount(), null) + ";\r\n";
		}
		if (accountInfoIsUpdate(oldAccount.getHandicapId(), newAccount.getHandicapId())) {
			remark += "盘口  " + oldAccount.getHandicapId() + " 改为 " + newAccount.getHandicapId() + ";\r\n";
		}
		if (accountInfoIsUpdate(oldAccount.getType(), newAccount.getType())) {
			remark += "类型 " + AccountType.findByTypeId(oldAccount.getType()).getMsg() + " 改为 "
					+ AccountType.findByTypeId(newAccount.getType()).getMsg() + ";\r\n";
		}
		if (accountInfoIsUpdate(oldAccount.getSubType(), newAccount.getSubType())) {
			remark += "类型 " + getStrBySubType(oldAccount.getSubType()) + " 改为 "
					+ getStrBySubType(newAccount.getSubType()) + ";\r\n";
		}
		if (accountInfoIsUpdate(oldAccount.getBankType(), newAccount.getBankType())) {
			remark += "银行类型  " + oldAccount.getBankType() + " 改为 " + newAccount.getBankType() + ";\r\n";
		}
		if (accountInfoIsUpdate(oldAccount.getBankName(), newAccount.getBankName())) {
			remark += "开户行 " + oldAccount.getBankName() + " 改为 " + newAccount.getBankName() + ";\r\n";
		}
		if (accountInfoIsUpdate(oldAccount.getOwner(), newAccount.getOwner())) {
			remark += "开户人 " + oldAccount.getOwner() + " 改为 " + newAccount.getOwner() + ";\r\n";
		}
		if (accountInfoIsUpdate(oldAccount.getFlag(), newAccount.getFlag())) {
			remark += "来源 " + getStrByFlagId(oldAccount.getFlag()) + " 改为 " + getStrByFlagId(newAccount.getFlag())
					+ ";\r\n";
		}
		if (accountInfoIsUpdate(oldAccount.getMobile(), newAccount.getMobile())) {
			remark += "手机号 " + CommonUtils.hideAccountAll(oldAccount.getMobile(), "phone") + " 改为 "
					+ CommonUtils.hideAccountAll(newAccount.getMobile(), "phone") + ";\r\n";
		}
		if (accountInfoIsUpdate(oldAccount.getCurrSysLevel(), newAccount.getCurrSysLevel())) {
			remark += "内外层 " + getStrByCurrLevelId(oldAccount.getCurrSysLevel()) + " 改为 "
					+ getStrByCurrLevelId(newAccount.getCurrSysLevel()) + ";\r\n";
		}
		if (accountInfoIsUpdate(oldAccount.getProvince(), newAccount.getProvince())) {
			remark += "省 " + oldAccount.getProvince() + " 改为 " + newAccount.getProvince() + ";\r\n";
		}
		if (accountInfoIsUpdate(oldAccount.getCity(), newAccount.getCity())) {
			remark += "市 " + oldAccount.getCity() + " 改为 " + newAccount.getCity() + ";\r\n";
		}
		if (accountInfoIsUpdate(oldAccount.getMinInAmount(), newAccount.getMinInAmount())) {
			remark += "最小入款金额  " + oldAccount.getMinInAmount() + " 改为 " + newAccount.getMinInAmount() + ";\r\n";
		}
		if (accountInfoIsUpdate(oldAccount.getPeakBalance(), newAccount.getPeakBalance())) {
			remark += "余额峰值 " + oldAccount.getPeakBalance() + " 改为 " + newAccount.getPeakBalance() + ";\r\n";
		}
		if (accountInfoIsUpdate(oldAccount.getLimitBalance(), newAccount.getLimitBalance())) {
			remark += "余额告警 " + oldAccount.getLimitBalance() + " 改为 " + newAccount.getLimitBalance() + ";\r\n";
		}
		if (accountInfoIsUpdate(oldAccount.getLimitIn(), newAccount.getLimitIn())) {
			remark += "当日入款限额 " + oldAccount.getLimitIn() + " 改为 " + newAccount.getLimitIn() + ";\r\n";
		}
		if (accountInfoIsUpdate(oldAccount.getLimitOut(), newAccount.getLimitOut())) {
			remark += "当日出款限额 " + oldAccount.getLimitOut() + " 改为 " + newAccount.getLimitOut() + ";\r\n";
		}
		if (accountInfoIsUpdate(oldAccount.getLimitOutOne(), newAccount.getLimitOutOne())) {
			remark += "单笔最高出款  " + oldAccount.getLimitOutOne() + " 改为 " + newAccount.getLimitOutOne() + ";\r\n";
		}
		if (accountInfoIsUpdate(oldAccount.getLimitOutOneLow(), newAccount.getLimitOutOneLow())) {
			remark += "单笔最低出款  " + oldAccount.getLimitOutOneLow() + " 改为 " + newAccount.getLimitOutOneLow() + ";\r\n";
		}
		if (accountInfoIsUpdate(oldAccount.getLowestOut(), newAccount.getLowestOut())) {
			remark += "最低余额限制 " + oldAccount.getLowestOut() + " 改为 " + newAccount.getLowestOut() + ";\r\n";
		}
		if (accountInfoIsUpdate(oldAccount.getLimitOutCount(), newAccount.getLimitOutCount())) {
			remark += "当日出款笔数  " + oldAccount.getLimitOutCount() + " 改为 " + newAccount.getLimitOutCount() + ";\r\n";
		}
		if (StringUtils.isEmpty(remark)) {
			return;
		}
		accountExtra.setRemark(remark);
		accountExtraRespository.save(accountExtra);
	}

	private String getStrByCurrLevelId(Integer id) {
		String result = "";
		if (null != id) {
			if (id.equals(1)) {
				result = "外层";
			} else if (id.equals(2)) {
				result = "内层";
			} else if (id.equals(4)) {
				result = "中层";
			} else if (id.equals(5)) {
				result = "指定层";
			}
		}
		return result;
	}

	private String getStrByFlagId(Integer id) {
		String result = "PC";
		if (null != id && id.equals(2)) {
			result = "返利网";
		}
		return result;
	}

	private String getStrBySubType(Integer id) {
		String result = "银行入款卡";
		if (null != id) {
			if (id.equals(1)) {
				result = "支付宝入款卡";
			}
		}
		return result;
	}

	/**
	 * 对比两个字符串是否有更新
	 *
	 * @param oldStr
	 * @param newStr
	 * @return true:有更新 false:无更新
	 */
	private boolean accountInfoIsUpdate(String oldStr, String newStr) {
		if (StringUtils.isEmpty(oldStr) && StringUtils.isEmpty(newStr)) {
			// 都为空
			return false;
		} else if (StringUtils.isEmpty(oldStr) || StringUtils.isEmpty(newStr)) {
			// 只有一个为空
			return true;
		} else if (!oldStr.equals(newStr)) {
			// 都不为空
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 对比两个Integer是否有更新
	 *
	 * @param oldStr
	 * @param newStr
	 * @return true:有更新 false:无更新
	 */
	private boolean accountInfoIsUpdate(Integer oldStr, Integer newStr) {
		if (null == oldStr && null == newStr) {
			// 都为空
			return false;
		} else if (null == oldStr || null == newStr) {
			// 只有一个为空
			return true;
		} else if (!oldStr.equals(newStr)) {
			// 都不为空
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 对比两个BigDecimal是否有更新
	 *
	 * @param oldStr
	 * @param newStr
	 * @return true:有更新 false:无更新
	 */
	private boolean accountInfoIsUpdate(BigDecimal oldStr, BigDecimal newStr) {
		if (null == oldStr && null == newStr) {
			// 都为空
			return false;
		} else if (null == oldStr || null == newStr) {
			// 只有一个为空
			return true;
		} else if (oldStr.compareTo(newStr) != 0) {
			// 都不为空
			return true;
		} else {
			return false;
		}
	}

	@Transactional
	public void saveAccountRate(BizAccount account) {
		String[] values = accountExtraRespository.findByid(account.getId());
		if (values.length <= 0) {
			accountExtraRespository.saveAccountRate(account.getId(), account.getRateType(), account.getRate(),
					account.getRateValue());
		} else {
			accountExtraRespository.updateAccountRate(account.getId(), account.getRateType(), account.getRate(),
					account.getRateValue());
		}
	}

	@Transactional
	public void updateThirdAccountBl(int id, BigDecimal amount) {
		accountExtraRespository.updateThirdAccountBalance(id, amount);
	}
}
