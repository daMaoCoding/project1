package com.xinbo.fundstransfer.restful.api;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.net.socket.AccountEntity;
import com.xinbo.fundstransfer.component.net.socket.ActionEventEnum;
import com.xinbo.fundstransfer.component.net.socket.MessageEntity;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.GeneralResponseData.ResponseStatus;
import com.xinbo.fundstransfer.domain.ResponseData;
import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.enums.CurrentSystemLevel;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.restful.api.pojo.ApiAccount;
import com.xinbo.fundstransfer.service.*;

/**
 * 入款请求接口
 *
 * @author Eden
 */
@RestController("apiAccountController")
@RequestMapping("/api/account")
public class AccountController {
	private static Logger log = LoggerFactory.getLogger(AccountController.class);
	@Autowired
	private SysUserService userService;
	@Autowired
	private HostMonitorService monitorService;
	@Autowired
	private AccountService accountService;
	@Autowired
	private LevelService levelService;
	@Autowired
	private HandicapService handicapService;
	@Autowired
	private AllocateIncomeAccountService incomeAccountAllocateService;
	@Autowired
	private AllocateTransferService allocateTransferService;
	@Autowired
	private AccountExtraService accountExtraService;
	private ObjectMapper mapper = new ObjectMapper();
	@Autowired
	Environment environment;

	/***
	 * 工具：发现卡异常 把卡转到异常处理状态
	 */
	@RequestMapping("/freeze")
	public String freeze(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.info("account(freeze) >> RequestBody:{}", bodyJson);
			AccountEntity entity = mapper.readValue(bodyJson, AccountEntity.class);
			// 1-验证参数
			if (!md5(entity.getAccount()).equals(entity.getToken())) {
				log.info("Failure, invalid Token.");
				return mapper.writeValueAsString(
						new ResponseData(ResponseStatus.FAIL.getValue(), "Failure, invalid Token."));
			}
			AccountBaseInfo base = accountService.getFromCacheById(entity.getId());
			if (base == null || Objects.equals(base.getStatus(), AccountStatus.Freeze.getStatus())) {
				return mapper.writeValueAsString(new SimpleResponseData(ResponseStatus.SUCCESS.getValue(), "success"));
			}
			SysUser sysUser = userService.findFromCacheById(AppConstants.USER_ID_4_ADMIN);// 管理员
			Date d = new Date();
			BizAccount account = accountService.getById(entity.getId());
			BizAccount oldAccount = new BizAccount();
			BeanUtils.copyProperties(oldAccount, account);
			account.setHolder(sysUser.getId());
			account.setUpdateTime(d);
			account.setRemark(CommonUtils.genRemark(account.getRemark(),
					"【转" + AccountStatus.Excep.getMsg() + "】" + StringUtils.EMPTY, d, sysUser.getUid()));
			account.setStatus(AccountStatus.Excep.getStatus());
			account.setUpdateTime(d);
			accountService.updateBaseInfo(account);
			accountExtraService.saveAccountExtraLog(oldAccount, account, sysUser.getUid());
			accountService.broadCast(account);
			monitorService.update(account);
			// 发送取消转账命令
			List<Integer> frIdList = allocateTransferService.findFrIdList(entity.getId());
			if (!CollectionUtils.isEmpty(frIdList)) {
				for (Integer frId : frIdList) {
					MessageEntity<Integer> messageEntity = new MessageEntity<>();
					messageEntity.setData(frId);
					messageEntity.setIp(monitorService.findHostByAcc(frId));
					messageEntity.setAction(ActionEventEnum.CANCEL.ordinal());
					monitorService.messageBroadCast(messageEntity);
					log.info("Cancel Transfer  frId:{} toId:{}", frId, entity.getId());
				}
			}
			return mapper.writeValueAsString(new SimpleResponseData(ResponseStatus.SUCCESS.getValue(), "success"));
		} catch (Exception e) {
			log.error("Transfer(inAck) error.", e);
			return mapper.writeValueAsString(new SimpleResponseData(ResponseStatus.FAIL.getValue(),
					"Transfer ack error." + e.getLocalizedMessage()));
		}
	}

	/**
	 * 入款账号新增或修改
	 */
	@RequestMapping(value = "/put")
	public String put(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.trace("Account >> RequestBody:{}", bodyJson);
			ApiAccount entity = mapper.readValue(bodyJson, ApiAccount.class);
			if (null == entity.getType()) {
				return mapper.writeValueAsString(new SimpleResponseData(
						GeneralResponseData.ResponseStatus.FAIL.getValue(), "Failure, The type is empty."));
			}
			if (StringUtils.isEmpty(entity.getHandicap())) {
				return mapper.writeValueAsString(
						new SimpleResponseData(ResponseStatus.FAIL.getValue(), "Failure, The handicap is empty."));
			}
			if (StringUtils.isEmpty(entity.getAccount())) {
				return mapper.writeValueAsString(new SimpleResponseData(
						GeneralResponseData.ResponseStatus.FAIL.getValue(), "Failure, The account is empty."));
			}
			if (StringUtils.isEmpty(entity.getToken())) {
				return mapper.writeValueAsString(new SimpleResponseData(
						GeneralResponseData.ResponseStatus.FAIL.getValue(), "Failure, The token is empty."));
			}
			if (StringUtils.isEmpty(entity.getBankType())) {
				return mapper.writeValueAsString(new SimpleResponseData(
						GeneralResponseData.ResponseStatus.FAIL.getValue(), "Failure, The bankType is empty."));
			}
			if (StringUtils.isEmpty(entity.getLevelType())) {
				return mapper.writeValueAsString(new SimpleResponseData(
						GeneralResponseData.ResponseStatus.FAIL.getValue(), "Failure, The levelType is empty."));
			}
			if (entity.getType().intValue() == 1 && StringUtils.isEmpty(entity.getBankName())) {
				return mapper.writeValueAsString(new SimpleResponseData(
						GeneralResponseData.ResponseStatus.FAIL.getValue(), "Failure, The bankName is empty."));
			}
			if (entity.getType().intValue() == 1 && StringUtils.isEmpty(entity.getOwner())) {
				return mapper.writeValueAsString(new SimpleResponseData(
						GeneralResponseData.ResponseStatus.FAIL.getValue(), "Failure, The owner is empty."));
			}
			Integer currentSystemLevel = CurrentSystemLevel.findByName(entity.getLevelType());
			if (currentSystemLevel == null) {
				log.info("{} 内层，外层 传输有误,LevelType：{}", entity.getHandicap(), entity.getLevelType());
				return mapper.writeValueAsString(
						new SimpleResponseData(ResponseStatus.FAIL.getValue(), "Failure, LevelType does not exist."));
			}
			// 计算token
			Map<String, String> parameters = new TreeMap<>(Comparator.naturalOrder());
			parameters.put("type", entity.getType() + "");
			parameters.put("handicap", entity.getHandicap());
			parameters.put("account", entity.getAccount());
			parameters.put("banktype", entity.getBankType());
			parameters.put("leveltype", entity.getLevelType());
			if (StringUtils.isNotEmpty(entity.getLevels())) {
				parameters.put("levels", entity.getLevels());
			}
			if (StringUtils.isNotEmpty(entity.getBankName())) {
				parameters.put("bankname", entity.getBankName());
			}
			if (StringUtils.isNotEmpty(entity.getOwner())) {
				parameters.put("owner", entity.getOwner());
			}
			if (entity.getStatus() != null) {
				parameters.put("status", entity.getStatus() + "");
			}
			StringBuilder sb = new StringBuilder();
			Set<Map.Entry<String, String>> entrySet = parameters.entrySet();
			for (Map.Entry<String, String> entry : entrySet) {
				sb.append(entry.getValue());
			}
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			md5.update(sb.append(environment.getProperty("funds.transfer.apikey")).toString().getBytes());
			// 进行哈希计算并返回结果
			byte[] btResult = md5.digest();
			// 进行哈希计算后得到的数据的长度
			StringBuffer md5Token = new StringBuffer();
			for (byte b : btResult) {
				int bt = b & 0xff;
				if (bt < 16) {
					md5Token.append(0);
				}
				md5Token.append(Integer.toHexString(bt));
			}
			if (!md5Token.toString().equals(entity.getToken())) {
				log.info("Token error. RequestToken:{}, encrypt string:{}", entity.getToken(), sb.toString());
				return mapper.writeValueAsString(
						new SimpleResponseData(ResponseStatus.FAIL.getValue(), "Failure, invalid Token."));
			}
			BizHandicap bizHandicap = handicapService.findFromCacheByCode(entity.getHandicap());
			if (null == bizHandicap) {
				log.info("{} 盘口不存在,帐号：{}", entity.getHandicap(), entity.getAccount());
				return mapper.writeValueAsString(
						new SimpleResponseData(ResponseStatus.FAIL.getValue(), "Failure, handicap does not exist."));
			}
			AccountBaseInfo baseInfo = accountService.getFromCacheByHandicapIdAndAccount(bizHandicap.getId(),
					entity.getAccount());
			String[] levelArray = { "unknown" };
			if (StringUtils.isNotEmpty(entity.getLevels())) {
				List<String> levelList = new LinkedList<>();
				String[] tempArray = entity.getLevels().split(",");
				for (String level : tempArray) {
					BizLevel find = levelService.findFromCache(bizHandicap.getId(), level);
					if (null != find) {
						levelList.add(level);
					}
				}
				if (levelList.size() > 0) {
					levelArray = levelList.toArray(levelArray);
				}
			}
			if (null != entity.getStatus()) {
				Integer status;
				// 0-停用，1-启用,2-冻结，3-删除，4-新卡
				if (entity.getStatus().intValue() == 0) {// 停用
					status = AccountStatus.StopTemp.getStatus();
				} else if (entity.getStatus().intValue() == 1) {// 启用
					status = AccountStatus.Normal.getStatus();
				} else if (entity.getStatus().intValue() == 2) {// 冻结
					status = AccountStatus.Freeze.getStatus();
				} else if (entity.getStatus().intValue() == 3) {// 删除
					status = AccountStatus.Delete.getStatus();
				} else if (entity.getStatus().intValue() == 4) {// 新卡（可用）
					status = AccountStatus.Enabled.getStatus();
				} else {
					log.error("Account 状态非法输入 >>{} bodyJson:{}", entity.getHandicap(), bodyJson);
					return mapper.writeValueAsString(new SimpleResponseData(ResponseStatus.FAIL.getValue(), "状态非法输入"));
				}
				entity.setStatus(status);
			}
			if (baseInfo == null) {
				if (entity.getStatus().equals(AccountStatus.Normal.getStatus())
						|| entity.getStatus().equals(AccountStatus.Enabled.getStatus())) {
					// 新增时只同步 在用 和 新卡
					BizAccount o = new BizAccount();
					o.setHandicapId(bizHandicap.getId());
					o.setStatus(entity.getStatus());
					o.setAccount(entity.getAccount());
					o.setBankName(entity.getBankName());
					o.setOwner(entity.getOwner());
					o.setType(entity.getType());
					o.setCurrSysLevel(currentSystemLevel);
					o.setFlag(0);
					List<BizAccountLevel> accountLevelToList = new ArrayList<>();
					if (levelArray != null && levelArray.length > 0) {
						for (String levelCode : levelArray) {
							BizLevel level = levelService.findFromCache(bizHandicap.getId(), levelCode);
							if (level != null) {
								accountLevelToList.add(new BizAccountLevel(null, level.getId()));
							}
						}
					}
					Date date = new Date();
					o.setCreateTime(date);
					o.setUpdateTime(date);
					o.setRemark(CommonUtils.genRemark(null, "平台同步账号", date, "系统"));
					// 是银行账号 且查出的数据无编号时，自动生成编号
					o = accountService.setAccountAlias(o);
					BizAccount newAccount = accountService.create(accountLevelToList, o);
					accountService.broadCast(newAccount);
					if (newAccount.getType().equals(AccountType.InBank.getTypeId())
							&& incomeAccountAllocateService.checkHandicap(entity.getHandicap())) {
						incomeAccountAllocateService.update(newAccount.getId(), newAccount.getType(),
								newAccount.getStatus());
					}
				}
			} else {
				// 检测 新卡状态
				int enabled = AccountStatus.Enabled.getStatus().intValue();
				if (baseInfo.getStatus() != enabled && entity.getStatus() == enabled) {
					log.error(
							"Account 新卡转启用后不能恢复成新卡  >> handicap:{} account:{} new.status:{}，old.status:{},bodyJson:{}",
							entity.getHandicap(), entity.getAccount(), entity.getStatus(), baseInfo.getStatus(),
							bodyJson);
					return mapper.writeValueAsString(
							new SimpleResponseData(ResponseStatus.FAIL.getValue(), "Failure, 新卡转启用后不能恢复成新卡"));
				}
				BizAccount latestAccount = null;
				// 入款账号分配 标识
				boolean updAllocate = false;
				// 修改账号 基本信息
				boolean updateAccount = checkUpdateAccount(bizHandicap, baseInfo, entity);
				if (updateAccount) {
					BizAccount db = accountService.getById(baseInfo.getId());
					updAllocate = incomeAccountAllocateService.checkHandicap(entity.getHandicap())
							&& (!db.getStatus().equals(entity.getStatus()) || !db.getType().equals(entity.getType()))
							&& (db.getType().equals(AccountType.InBank.getTypeId())
									|| baseInfo.getType().equals(AccountType.InBank.getTypeId()));
					db.setStatus(entity.getStatus());
					db.setType(entity.getType());
					db.setUpdateTime(new Date());
					accountService.updateBaseInfo(db);
					latestAccount = db;
				}
				// 广播
				if (updateAccount) {
					if (AccountStatus.Delete.getStatus().equals(latestAccount.getStatus())) {
						log.error("Account Delete  >> handicap:{} account:{} status:{}， bodyJson:{}",
								entity.getHandicap(), entity.getAccount(), entity.getStatus(), bodyJson);
					} else {
						log.debug("Account Update  >> handicap:{} account:{} status:{}， bodyJson:{}",
								entity.getHandicap(), entity.getAccount(), entity.getStatus(), bodyJson);
					}
					accountService.broadCast(latestAccount);
				}
				// 入款账号 分配
				if (updAllocate) {
					log.info("Account >> UpdateAlloc:{}", entity.getAccount());
					incomeAccountAllocateService.update(latestAccount.getId(), latestAccount.getType(),
							latestAccount.getStatus());
				}
			}
			return mapper.writeValueAsString(new SimpleResponseData(ResponseStatus.SUCCESS.getValue(), "Success."));
		} catch (Exception e) {
			log.error("Account error. >>", e);
			return mapper.writeValueAsString(
					new SimpleResponseData(ResponseStatus.FAIL.getValue(), "Failure, " + e.getLocalizedMessage()));
		}
	}

	private boolean checkUpdateAccount(BizHandicap handicap, AccountBaseInfo b, ApiAccount o) {
		if (b.getType() != null && !b.getType().equals(o.getType())) {
			log.trace("need update:handicap={},account={},to.type={},from.type={}", handicap.getCode(), b.getAccount(),
					o.getType(), b.getType());
			return true;
		}
		if (b.getStatus() != null && !b.getStatus().equals(o.getStatus())) {
			log.trace("need update:handicap={},account={},to.status={},from.status={}", handicap.getCode(),
					b.getAccount(), o.getStatus(), b.getStatus());
			return true;
		}
		return false;
	}

	/**
	 * 生成MD5
	 *
	 */
	private String md5(String raw) {
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			md5.update((raw + environment.getProperty("funds.transfer.apikey")).getBytes());
			// 进行哈希计算并返回结果
			byte[] btResult = md5.digest();
			// 进行哈希计算后得到的数据的长度
			StringBuilder md5Token = new StringBuilder();
			for (byte b : btResult) {
				int bt = b & 0xff;
				if (16 > bt) {
					md5Token.append(0);
				}
				md5Token.append(Integer.toHexString(bt));
			}
			return md5Token.toString();
		} catch (NoSuchAlgorithmException e) {
			log.error("", e);
			return "";
		}
	}
}
