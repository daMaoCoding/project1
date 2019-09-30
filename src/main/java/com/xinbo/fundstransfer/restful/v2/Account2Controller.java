package com.xinbo.fundstransfer.restful.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizAccountLevel;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.BizLevel;
import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.enums.CurrentSystemLevel;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.restful.v2.pojo.RequestAccount;
import com.xinbo.fundstransfer.service.*;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * 新平台账号同步信息
 */
@RestController
@RequestMapping("/api/v2/account")
public class Account2Controller extends TokenValidation {
	private static final Logger log = LoggerFactory.getLogger(Account2Controller.class);
	private final AccountService accountService;
	private final LevelService levelService;
	private final HandicapService handicapService;
	private final AccountExtraService accountExtraService;
	private final AllocateIncomeAccountService incomeAccountAllocateService;
	private final CabanaService cabanaService;
	private final AssignAWInAccountService assignAWInAccountService;
	private final FinLessStatService finLessStatService;
	private ObjectMapper mapper = new ObjectMapper();
	SimpleResponseData error400 = new SimpleResponseData(400, "Error");
	SimpleResponseData error401 = new SimpleResponseData(401, "Error");
	SimpleResponseData error500 = new SimpleResponseData(500, "Error");
	SimpleResponseData success = new SimpleResponseData(1, "OK");

	@Autowired
	public Account2Controller(AccountService accountService, LevelService levelService, HandicapService handicapService,
			AccountExtraService accountExtraService, AllocateIncomeAccountService incomeAccountAllocateService,
			CabanaService cabanaService, AssignAWInAccountService assignAWInAccountService,
			FinLessStatService finLessStatService) {
		this.accountService = accountService;
		this.levelService = levelService;
		this.handicapService = handicapService;
		this.accountExtraService = accountExtraService;
		this.incomeAccountAllocateService = incomeAccountAllocateService;
		this.cabanaService = cabanaService;
		this.assignAWInAccountService = assignAWInAccountService;
		this.finLessStatService = finLessStatService;
	}

	/**
	 * 账号同步
	 * 
	 * @param requestBody
	 *            账号信息
	 * @param result
	 *            调用结果
	 * @return 返回SimpleResponseData 结果
	 * @throws JsonProcessingException
	 *             转换异常
	 */
	@RequestMapping(value = "/put", method = RequestMethod.POST, consumes = "application/json")
	public @ResponseBody SimpleResponseData put(@Valid @RequestBody RequestAccount requestBody, BindingResult result)
			throws JsonProcessingException {
		log.info("Account >> RequestBody:{}", mapper.writeValueAsString(requestBody));
		if (result.hasErrors()) {
			return error400;
		}
		if (!checkToken(requestBody.getToken(), requestBody.getHandicap())) {
			return error401;
		}
		if (requestBody.getType() == null) {
			log.info("{} 账号类型必传,帐号：{}", requestBody.getBankType(), requestBody.getAccount());
			return error400;
		}
		if (requestBody.getType() == AccountType.InBank.getTypeId() && StringUtils.isBlank(requestBody.getBankType())) {
			log.info("{} 银行类型必传,帐号：{}", requestBody.getBankType(), requestBody.getAccount());
			return error400;
		}
		if (requestBody.getType() == AccountType.InBank.getTypeId() && StringUtils.isBlank(requestBody.getOwner())) {
			log.info("{} 开户人必传,帐号：{}", requestBody.getOwner(), requestBody.getAccount());
			return error400;
		}
		// 微信支付宝账号可能为空,所以在此校验其他情况不能为空
		if (!AccountType.InAli.getTypeId().equals(requestBody.getType())
				&& !AccountType.InWechat.getTypeId().equals(requestBody.getType())
				&& !AccountType.AliEnterPrise.getTypeId().equals(requestBody.getType())) {
			if (StringUtils.isBlank(requestBody.getAccount())) {
				log.info("{} 非入款支付宝微信,账号必传,帐号：{}", requestBody.getOwner(), requestBody.getAccount());
				return error400;
			}
		}
		if (AccountType.InAli.getTypeId().equals(requestBody.getType())
				|| AccountType.InWechat.getTypeId().equals(requestBody.getType())) {
			if (StringUtils.isBlank(requestBody.getBankName())) {
				log.info("入款支付宝微信账号：{},设备号: device 必传  ", requestBody.getAccount());
				return new SimpleResponseData(400, "设备号device必传");
			}
		}
		if (Objects.equals(requestBody.getType(), AccountType.InThird.getTypeId())
				|| Objects.equals(requestBody.getType(), AccountType.InAli.getTypeId())
				|| Objects.equals(requestBody.getType(), AccountType.InWechat.getTypeId())) {
			if (StringUtils.isBlank(requestBody.getBankName()) || StringUtils.isBlank(requestBody.getBankType())) {
				log.info("账号类型:{}, bankType:{},bankName：{}", AccountType.findByTypeId(requestBody.getType()).getMsg(),
						requestBody.getBankType(), requestBody.getBankName());
				return new SimpleResponseData(400, "第三方/微信/支付宝账号bankType和bankName必传");
			}
		}
		try {
			BizHandicap bizHandicap = handicapService.findFromCacheByCode(requestBody.getHandicap());
			if (null == bizHandicap || !Objects.equals(bizHandicap.getStatus(), 1)) {
				log.error("{} 盘口不存在,帐号：{}", requestBody.getHandicap(), requestBody.getAccount());
				return error400;
			}
			AccountBaseInfo baseInfo;
			// 第三方账号存在场景:盘口相同，账号相同，商号不同的情况
			if (Objects.equals(requestBody.getType(), AccountType.InThird.getTypeId())
					&& StringUtils.isNotBlank(requestBody.getBankName())) {
				baseInfo = accountService.getFromCacheByHandicapIdAndAccountAndBankName(bizHandicap.getId(),
						requestBody.getAccount(), requestBody.getBankName());
			} else {
				baseInfo = accountService.getFromCacheByHandicapIdAndAccount(bizHandicap.getId(),
						requestBody.getAccount());
			}
			if (baseInfo != null && !Objects.equals(baseInfo.getType(), AccountType.InBank.getTypeId())
					&& !Objects.equals(baseInfo.getType(), AccountType.InAli.getTypeId())
					&& !Objects.equals(baseInfo.getType(), AccountType.InWechat.getTypeId())
					&& !Objects.equals(baseInfo.getType(), AccountType.InThird.getTypeId())
					&& !Objects.equals(baseInfo.getType(), AccountType.AliEnterPrise.getTypeId())) {
				return success;
			}
			if (null != requestBody.getStatus()) {
				log.info("requestBody的账号:{},状态:{}", requestBody.getAccount(), requestBody.getStatus());
				Integer status;
				// 0-停用，1-启用,2-冻结，3-删除，4-新卡
				if (requestBody.getStatus() == 0) {// 停用
					status = AccountStatus.StopTemp.getStatus();
				} else if (requestBody.getStatus() == 1) {// 启用
					status = AccountStatus.Normal.getStatus();
				} else if (requestBody.getStatus() == 2) {// 冻结
					status = AccountStatus.Freeze.getStatus();
				} else if (requestBody.getStatus() == 3) {// 删除
					status = AccountStatus.Delete.getStatus();
				} else if (requestBody.getStatus() == 4) {// 新卡（可用）
					status = AccountStatus.Enabled.getStatus();
				} else {
					log.info("{} 帐号的状态不在范围内, status：{}", requestBody.getAccount(), requestBody.getStatus());
					return error400;
				}
				requestBody.setStatus(status);
			}
			String[] levelArray = { "unknown" };
			List<String> levelList;
			List<Integer> levelIdList = null;
			Integer accountId;
			log.info("TheLevelListAccount2Controller:{}", requestBody.getLevels());
			// 当前层级，拥有多个时，指定层>外层>中层>内层
			boolean Designated = false, Outter = false, Middle = false, Inner = false;
			int currentSystemLevel = 0;
			if (StringUtils.isNotBlank(requestBody.getLevels())) {
				// levels 传的就是层级编码
				levelList = new LinkedList<>();
				levelIdList = new LinkedList<>();
				String[] tempArray = requestBody.getLevels().split(",");
				for (String level : tempArray) {
					BizLevel find = levelService.findFromCache(bizHandicap.getId(), level);
					if (null != find) {
						levelList.add(level);
						levelIdList.add(find.getId());
						// 根据层级自动适配当前层级
						if (null != find.getCurrSysLevel()) {// 此层级绑定的内中外层不为空
							if (CurrentSystemLevel.Designated.getValue() == find.getCurrSysLevel()) {
								Designated = true;
							} else if (CurrentSystemLevel.Outter.getValue() == find.getCurrSysLevel()) {
								Outter = true;
							} else if (CurrentSystemLevel.Middle.getValue() == find.getCurrSysLevel()) {
								Middle = true;
							} else if (CurrentSystemLevel.Inner.getValue() == find.getCurrSysLevel()) {
								Inner = true;
							}
						}
					}
				}
				if (levelList.size() > 0) {
					levelArray = levelList.toArray(levelArray);
				}
				// 指定层>外层>中层>内层
				if (Designated) {
					currentSystemLevel = CurrentSystemLevel.Designated.getValue();
				} else if (Outter) {
					currentSystemLevel = CurrentSystemLevel.Outter.getValue();
				} else if (Middle) {
					currentSystemLevel = CurrentSystemLevel.Middle.getValue();
				} else if (Inner) {
					currentSystemLevel = CurrentSystemLevel.Inner.getValue();
				}
			}
			log.info("TheLevelListMatchToCurrentSystemLevelAccount2Controller:{}", currentSystemLevel);
			if (baseInfo == null) {
				// 如果是第三方账号 允许停用的账号同步过来
				boolean isThirdAccount = Objects.equals(requestBody.getType(), AccountType.InThird.getTypeId());
				if (isThirdAccount || (requestBody.getStatus().equals(AccountStatus.Normal.getStatus())
						|| requestBody.getStatus().equals(AccountStatus.Enabled.getStatus()))) {
					BizAccount o = new BizAccount();
					// o.setCurrSysLevel(currentSystemLevel);
					o.setHandicapId(bizHandicap.getId());
					o.setStatus(requestBody.getStatus());
					o.setAccount(requestBody.getAccount());
					o.setBankName(requestBody.getBankName());// 支付宝 微信 存设备号
					o.setBankType(requestBody.getBankType());// 做唯一性键,新增之后对于银行入款卡系统会设置类型,不以平台同步的为准
					o.setOwner(requestBody.getOwner());
					o.setFlag(0);
					o.setType(requestBody.getType());
					if (requestBody.getType() == AccountType.InBank.getTypeId()) {
						o.setSubType(requestBody.getSubType() == null ? 0 : requestBody.getSubType());
					}
					Date date = new Date();
					o.setCreateTime(date);
					o.setUpdateTime(date);
					List<BizAccountLevel> accountLevelToList = levelService.wrapAccountLevel(levelArray, bizHandicap);
					o = accountService.setAccountAlias(o);
					BizAccount newAccount = accountService.create(accountLevelToList, o);
					accountService.broadCast(newAccount);
					boolean type = newAccount.getType().equals(AccountType.InBank.getTypeId())
							|| newAccount.getType().equals(AccountType.InAli.getTypeId())
							|| newAccount.getType().equals(AccountType.InWechat.getTypeId())
							|| newAccount.getType().equals(AccountType.AliEnterPrise.getTypeId());
					if (type && incomeAccountAllocateService.checkHandicap(requestBody.getHandicap())) {
						incomeAccountAllocateService.update(newAccount.getId(), newAccount.getType(),
								newAccount.getStatus());
					}
					accountId = newAccount.getId();
					cabanaService.updAcc(newAccount.getId());
					accountExtraService.addAccountExtraLog(accountId, "平台同步");
					if (requestBody.getType() != null
							&& (AccountType.InWechat.getTypeId().equals(requestBody.getType())
									|| AccountType.InAli.getTypeId().equals(requestBody.getType()))
							&& accountId != null) {
						assignAWInAccountService.sendMessageOnPushAccount(requestBody.getType(), accountId);
					}
				}
			} else {
				// 检测 新卡状态 Enabled 是可用 新卡
				int enabled = AccountStatus.Enabled.getStatus();
				if (baseInfo.getStatus() != enabled && requestBody.getStatus() == enabled) {
					// log.info("Account 已启用后不能恢复成新卡 >> handicap:{} account:{}
					// new.status:{}，old.status:{}",
					// requestBody.getHandicap(), requestBody.getAccount(),
					// requestBody.getStatus(),
					// baseInfo.getStatus());
					// return error400;
				}
				BizAccount latestAccount;
				BizAccount db = accountService.getById(baseInfo.getId());
				BizAccount oldAccount = new BizAccount();
				BeanUtils.copyProperties(oldAccount, db);
				if (Objects.isNull(db)) {
					BizAccount account = new BizAccount().baseToBizAccount(baseInfo);
					log.info("account not in db :accountId:{},account:{},base->account:{}", baseInfo.getId(),
							baseInfo.getAccount(), account);
					accountService.broadCast(account);
					return success;
				}
				if (requestBody.getType().equals(AccountType.InAli) || requestBody.getType().equals(AccountType.InBank)
						|| requestBody.getType().equals(AccountType.InThird)
						|| requestBody.getType().equals(AccountType.InWechat)
						|| requestBody.getType().equals(AccountType.AliEnterPrise)) {
					if (db == null && requestBody.getStatus().equals(AccountStatus.Delete.getStatus())) {
						return success;
					}
					if (db.getStatus().equals(AccountStatus.Delete.getStatus())
							&& requestBody.getStatus().equals(AccountStatus.Delete.getStatus())) {
						return success;
					}
				}

				List<BizAccountLevel> bizAccountLevelList = accountService.findByAccountId(db.getId());
				if (!CollectionUtils.isEmpty(levelIdList)) {
					if (!CollectionUtils.isEmpty(bizAccountLevelList)) {
						if (bizAccountLevelList.size() == levelIdList.size()) {
							int count = 0;
							for (Integer aLevelIdList : levelIdList) {
								for (BizAccountLevel aBizAccountLevelList : bizAccountLevelList) {
									if (aLevelIdList == aBizAccountLevelList.getLevelId()) {
										count++;
									}
								}
							}
							if (count == levelIdList.size()) {
								log.info("层级信息不变:count:{},levelIdList.size():{}", count, levelIdList.size(),
										count == levelIdList.size());
							} else {
								// 先删除再新增
								log.info("先删除:{}再新增:{}", bizAccountLevelList.size(), levelIdList.size());
								accountService.deleteInBatch(bizAccountLevelList);
								accountService.bindLevels(levelIdList, db, levelArray);
							}
						} else {
							// 先删除再新增
							log.info("先删除:{}再新增:{}", bizAccountLevelList.size(), levelIdList.size());
							accountService.deleteInBatch(bizAccountLevelList);
							accountService.bindLevels(levelIdList, db, levelArray);
						}
					} else {
						// 新绑定层级
						log.info("新绑定层级:{}", levelIdList.size());
						accountService.bindLevels(levelIdList, db, levelArray);
					}
				} else {
					if (bizAccountLevelList != null && bizAccountLevelList.size() > 0) {
						// 直接删除
						log.info("先删除:{}", bizAccountLevelList.size());
						accountService.deleteInBatch(bizAccountLevelList);
					}
				}
				// 入款卡冻结时候拉倒亏损流水里面并且以前冻结的账号不拉倒冻结里面
				if (requestBody.getStatus().intValue() == AccountStatus.Freeze.getStatus()
						&& db.getStatus() != AccountStatus.Freeze.getStatus()) {
					// 查询是否存在未处理的冻结数据
					int count = finLessStatService.findCountsById(db.getId(), "portion");
					if (count <= 0) {
						finLessStatService.addTrace(db.getId(), db.getBankBalance());
					}
				}
				// 入款账号分配 标识
				boolean updAllocate = false, assignAWFlag = false;
				// 修改账号 基本信息
				boolean updateAccount = accountService.checkUpdateAccount(bizHandicap, baseInfo, requestBody);
				log.info("是否更新类型和状态:{}", updateAccount);
				if (updateAccount) {
					updAllocate = incomeAccountAllocateService.checkHandicap(requestBody.getHandicap())
							&& (!db.getStatus().equals(requestBody.getStatus())
									|| !db.getType().equals(requestBody.getType()))
							&& (db.getType().equals(AccountType.InBank.getTypeId())
									|| baseInfo.getType().equals(AccountType.InBank.getTypeId()));

					assignAWFlag = incomeAccountAllocateService.checkHandicap(requestBody.getHandicap())
							&& (!db.getStatus().equals(requestBody.getStatus())
									|| !db.getType().equals(requestBody.getType()))
							&& (db.getType().equals(AccountType.InAli.getTypeId())
									|| db.getType().equals(AccountType.InWechat.getTypeId()));
				}
				db.setStatus(requestBody.getStatus());
				db.setType(requestBody.getType());
				db.setSubType(requestBody.getSubType());
				db.setOwner(requestBody.getOwner());
				db.setBankName(requestBody.getBankName());
				if (!requestBody.getType().equals(AccountType.InBank.getTypeId())) {
					// 入款银行卡银行类型在出入款系统设置
					db.setBankType(requestBody.getBankType());
				}
				db.setUpdateTime(new Date());
				db.setHandicapId(bizHandicap.getId());
				// db.setCurrSysLevel(currentSystemLevel);
				db.setAccount(requestBody.getAccount());
				accountService.updateBaseInfo(db);
				if (AccountType.InBank.getTypeId().equals(requestBody.getType())) {
					latestAccount = db;
					// 广播同步账号信息
					accountService.broadCast(latestAccount);
					cabanaService.updAcc(db.getId());
					// 入款账号 分配
					if (updAllocate) {
						log.info("Account >> UpdateAlloc:{}", requestBody.getAccount());
						incomeAccountAllocateService.update(latestAccount.getId(), latestAccount.getType(),
								latestAccount.getStatus());
					}
				} else {
					accountId = db.getId();
					if (requestBody.getType() != null
							&& (AccountType.InWechat.getTypeId().equals(requestBody.getType())
									|| AccountType.InAli.getTypeId().equals(requestBody.getType()))
							&& accountId != null && assignAWFlag) {
						log.info("call assignAWInAccountService.sendMessageOnPushAccount");
						assignAWInAccountService.sendMessageOnPushAccount(requestBody.getType(), accountId);
					}
				}
				accountExtraService.saveAccountExtraLog(oldAccount, db, "平台同步");
			}
			log.info("accountService.put end :{}", requestBody.getBankName());
			return success;
		} catch (Exception e) {
			log.error("Account2Controller.put error :", e);
			return error500;
		}
	}

}
