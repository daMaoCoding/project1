package com.xinbo.fundstransfer.restful;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.enums.CurrentSystemLevel;
import com.xinbo.fundstransfer.runtime.MemCacheUtils;
import com.xinbo.fundstransfer.service.AccountExtraService;
import com.xinbo.fundstransfer.service.AccountMoreService;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.AccountSyncService;
import com.xinbo.fundstransfer.service.AllocateIncomeAccountService;
import com.xinbo.fundstransfer.service.AllocateTransferService;
import com.xinbo.fundstransfer.service.CabanaService;
import com.xinbo.fundstransfer.service.FinLessStatService;
import com.xinbo.fundstransfer.service.HostMonitorService;
import com.xinbo.fundstransfer.service.LevelService;
import com.xinbo.fundstransfer.service.QuickPayService;
import com.xinbo.fundstransfer.service.SysUserService;

/**
 * 账号管理（新）
 */
@RestController
@RequestMapping("/r/accountBase")
public class AccountBaseController extends BaseController {
	private static final Logger logger = LoggerFactory.getLogger(AccountBaseController.class);
	@Autowired
	public HttpServletRequest request;
	@Autowired
	private LevelService levelService;
	@Autowired
	private AccountService accountService;
	@Autowired
	private AccountExtraService accountExtraService;
	@Autowired
	private AccountSyncService accountSyncService;
	@Autowired
	private QuickPayService quickPayService;
	@Autowired
	private AllocateIncomeAccountService incomeAccountAllocateService;
	@Autowired
	private CabanaService cabanaService;
	@Autowired
	private AccountMoreService accMoreSer;
	@Autowired
	private HostMonitorService hostMonitorService;
	@Autowired
	private AllocateTransferService allocateTransferService;
	@Autowired
	private FinLessStatService finLessStatService;

	private ObjectMapper mapper = new ObjectMapper();

	/**
	 * PC/返利网 账号修改 不可以修改盘口和类型 IncomeAuditComp:UpdateCompBank 入款 修改银行卡
	 * IncomeAccountIssue:Update 下发 修改 AccountOutComp:Update 出款 修改全部
	 * AccountAudit:Update 返利网管理 汇总-修改
	 * 
	 * @param vo
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequiresPermissions(value = { "IncomeAuditComp:UpdateCompBank", "IncomeAccountIssue:Update",
			"AccountOutComp:Update", "AccountAudit:Update" }, logical = Logical.OR)
	@RequestMapping("/update")
	public String update(@Valid BizAccount vo) throws JsonProcessingException {
		String params = buildParams().toString();
		try {
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (operator == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请登录!"));
			}
			logger.info(String.format("%s，操作人员：%s，参数：%s", "账号更新newControllerAccountUpdate", operator.getUid(), params));
			if (vo.getId() == null) {
				log.error("修改账号信息 id不能为空");
				throw new Exception("id不能为空.");
			}
			BizAccount accountInfo = accountService.getById(vo.getId());
			BizAccount oldAccount = new BizAccount();// 旧account
			BizAccount db = new BizAccount();// 对象复制出来，以免每个set都去修改数据库自动保存导致程序变慢
			BeanUtils.copyProperties(oldAccount, accountInfo);
			BeanUtils.copyProperties(db, accountInfo);
			// 无层级修改为有层级 或者修改未传入层级（会以之前的层级为准）都不需要校验
			if (null != vo.getCurrSysLevel() && null != db.getCurrSysLevel()
					&& vo.getCurrSysLevel().intValue() != db.getCurrSysLevel().intValue()) {
				if (db.getCurrSysLevel().equals(CurrentSystemLevel.Outter.getValue())
						|| db.getCurrSysLevel().equals(CurrentSystemLevel.Designated.getValue())) {
					log.error("外层或者指定层的账号不允许修改为内层和中层");
					throw new Exception("外层或者指定层的账号不允许修改为内层和中层");
				}
			}
			if (null != vo.getLimitOutOne() && null != vo.getLimitOutOneLow()
					&& (vo.getLimitOutOne() < vo.getLimitOutOneLow())) {
				log.error("“最高出款限额”必须大于“最低出款限额”");
				throw new Exception("“最高出款限额”必须大于“最低出款限额”");
			}
			Map<String, Object> result = new HashMap<String, Object>();
			Date date = new Date();
			// 顺序勿更换
			db.setStatus(vo.getStatus() == null ? db.getStatus() : vo.getStatus());
			if (db.getFlag() == null || (!db.getFlag().equals(2))) {
				// PC卡才可以改基本信息
				db.setBankType(StringUtils.isBlank(vo.getBankType()) ? db.getBankType() : vo.getBankType());
				db.setBankName(StringUtils.isBlank(vo.getBankName()) ? db.getBankName() : vo.getBankName());
				db.setOwner(StringUtils.isBlank(vo.getOwner()) ? db.getOwner() : vo.getOwner());
				db.setPeakBalance(vo.getPeakBalance() == null ? db.getPeakBalance() : vo.getPeakBalance());// 余额峰值/保证金
			}
			db.setProvince(ObjectUtils.isEmpty(vo.getProvince()) ? db.getProvince() : vo.getProvince());
			db.setCity(ObjectUtils.isEmpty(vo.getCity()) ? db.getCity() : vo.getCity());
			db.setHandicapId(vo.getHandicapId() == null ? db.getHandicapId() : vo.getHandicapId());
			db.setType(vo.getType() == null ? db.getType() : vo.getType());
			// 层级
			db.setCurrSysLevel(vo.getCurrSysLevel() == null ? db.getCurrSysLevel() : vo.getCurrSysLevel());
			// 限额
			db.setMinInAmount(vo.getMinInAmount() == null ? db.getMinInAmount() : vo.getMinInAmount());// 最小入款金额
			db.setLimitBalance(vo.getLimitBalance() == null ? db.getLimitBalance() : vo.getLimitBalance());// 余额告警
			db.setLimitIn(vo.getLimitIn() == null ? db.getLimitIn() : vo.getLimitIn());// 当日入款限额
			db.setLimitOut(vo.getLimitOut() == null ? db.getLimitOut() : vo.getLimitOut());// 当日出款限额
			db.setLimitOutOne(vo.getLimitOutOne() == null ? db.getLimitOutOne() : vo.getLimitOutOne());// 单笔最高出款
			if (db.getLimitOutOne() == null) {
				Map<String, String> systemSetting = new HashMap<>();
				systemSetting = MemCacheUtils.getInstance().getSystemProfile();
				db.setLimitOutOne(Integer.parseInt(systemSetting.get("OUTDRAW_LIMIT_OUT_ONE")));
			}
			db.setLimitOutOneLow(vo.getLimitOutOneLow() == null ? db.getLimitOutOneLow() : vo.getLimitOutOneLow());// 单笔最低出款
			db.setLowestOut(vo.getLowestOut() == null ? db.getLowestOut() : vo.getLowestOut());// 最低余额限制
			db.setLimitOutCount(vo.getLimitOutCount() == null ? db.getLimitOutCount() : vo.getLimitOutCount());// 当日出款笔数

			// 无编号时，自动生成编号
			if (StringUtils.isEmpty(db.getAlias())) {
				// 编号六位数，跳过为4的数字 从100000开始递增
				String maxAlias = accountService.getMaxAlias();
				if (StringUtils.isEmpty(maxAlias) || maxAlias.equals("0")) {
					db.setAlias("100000");
				} else {
					int alias = Integer.parseInt(maxAlias) + 1;
					db.setAlias(Integer.toString(alias).replace("4", "5"));
				}
			}

			// 状态 顺序勿更换（修改部分资料也可能导致状态停用）
			if (oldAccount.getStatus() != null && db.getStatus() != null
					&& !oldAccount.getStatus().equals(db.getStatus())) {
				if (!db.getType().equals(AccountType.OutBank.getTypeId())) {
					// 只要不是出款卡 清空持卡人
					db.setHolder(null);
				} else {
					db.setHolder(operator != null ? operator.getId() : null);
				}
				if (db.getStatus().equals(AccountStatus.Normal.getStatus())) {// 在用
					// 账号从下发黑名单剔除
					allocateTransferService.rmFrBlackList(db.getId());
				} else if (db.getStatus().equals(AccountStatus.Freeze.getStatus())) {// 冻结
					// 只有冻结 才保存冻结备注
					db.setRemark(CommonUtils.genRemark(db.getRemark(),
							"【" + oldAccount.getStatusStr() + "转冻结】" + (vo.getRemark() != null ? vo.getRemark() : ""),
							date, operator.getUid()));
					// 冻结的时候添加到待处理业务表、如果存在没有处理完的则不添加
					int count = finLessStatService.findCountsById(db.getId(), "portion");
					if (count <= 0) {
						finLessStatService.addTrace(db.getId(), db.getBankBalance());
					}
				}
				if (StringUtils.isNotBlank(vo.getRemark())) {
					// 如果输入了备注 所有状态变更原因保存到操作日志
					db.setRemark4Extra(db.getRemark4Extra() != null ? db.getRemark4Extra()
							: "" + "【状态变更原因】" + vo.getRemark() + ";\r\n");
				}
			}

			// 用途 入款返利网
			boolean isUpdateYT=false;
			if(db.getType().equals(AccountType.InBank.getTypeId()) && db.getFlag().equals(2)) {//入款 返利网
				if(null != db.getSubType() && null != vo.getSubType() ){//非空判断
					if(!db.getSubType().equals(vo.getSubType())){//用途类型有变化
						isUpdateYT=true;
					}else if(vo.getSubType()!=3) {
						//先入后出
						if(null==db.getLimitPercentage()||!db.getLimitPercentage().equals(vo.getLimitPercentage())
								||null==db.getMinBalance()||!db.getMinBalance().equals(vo.getMinBalance())){//参数有变更
							isUpdateYT=true;
						}
					}
				}
			}
			if (isUpdateYT) {
				// 用途 用途相关参数有任何修改
				String remark4extra = "";
				db.setSubType(vo.getSubType());
				// c:边入边出和先入后出 outEnable修改为1
				db.setOutEnable((byte)1);
				if (vo.getSubType() == 0) {
					db.setLimitPercentage(vo.getLimitPercentage());
					db.setMinBalance(vo.getMinBalance());
					remark4extra = "【用途】变更为先入后出(返利网)：当余额大于或等于信用额度的" + vo.getLimitPercentage() + "时，转为出款卡，当余额小于"
							+ vo.getMinBalance() + "元，再转为入款卡;\r\n";
				} else if (vo.getSubType() == 3) {// 改为云闪付需要校验
					int counts = quickPayService.getBindAccountIdNum(db.getId());
					if (counts > 0) {
						// 成功 保存到数据库以及操作记录
						remark4extra = "【用途】变更为边入边出(返利网);\r\n";
					} else {
						// 失败 因为是单行，所以如果失败里面有数据，说明用途更新失败
						remark4extra = "【用途更新失败】变更为边入边出;\r\n";
						result.put("updateFailed_outEnable", true);
					}
				}
				db.setRemark4Extra(
						StringUtils.isNotBlank(db.getRemark4Extra()) ? db.getRemark4Extra() + remark4extra
								: remark4extra);
			}
			db.setSubType(vo.getSubType() == null ? db.getSubType() : vo.getSubType());//顺序勿更换
			db.setModifier(operator.getId());// 操作人
			db.setUpdateTime(date);// 最后更新时间
			result.put("newAccount", db);
			accountService.save(db);
			// 0元信用额度 顺序勿更换 否则保证金无法修改
			if (buildParams().containsKey("zerocredits")) {
				String remark4extra = "勾选了0元信用额度;\r\n";
				db.setRemark4Extra(StringUtils.isNotBlank(db.getRemark4Extra()) ? db.getRemark4Extra() + remark4extra
						: remark4extra);
				accMoreSer.setToZeroCredit(operator, db.getMobile());
			}
			// 操作记录表 顺序勿更换
			accountExtraService.saveAccountExtraLog(oldAccount, db, operator.getUid());
			hostMonitorService.update(db);
			logger.debug("update>> id {}",db.getId());
			accountService.broadCast(db);// 广播
			cabanaService.updAcc(db.getId());
			cabanaService.refreshAcc(db.getId());
			// 状态有变更，更新Redis
			if (Objects.nonNull(db) && Objects.nonNull(oldAccount)
					&& !Objects.equals(db.getStatus(), oldAccount.getStatus())) {
				incomeAccountAllocateService.update(db.getId(), db.getType(), db.getStatus());
			}
			GeneralResponseData<Map<String, Object>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			responseData.setData(result);
			return mapper.writeValueAsString(responseData);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error(String.format("%s，参数：%s，结果：%s", "账号更新", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getMessage()));
		}
	}

	/**
	 * 返利网账号启用 可以改盘口和类型 不可以改基本类型 保证金
	 * 
	 * @param vo
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/activated2enable")
	public String activated2enable(@Valid BizAccount vo) throws JsonProcessingException {
		String params = buildParams().toString();
		try {
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (operator == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请登录!"));
			}
			logger.info(String.format("%s，操作人员：%s，参数：%s", "账号更新newControllerAccountUpdate", operator.getUid(), params));
			if (vo.getId() == null) {
				log.error("修改账号信息 id不能为空");
				throw new Exception("id不能为空.");
			}
			BizAccount accountInfo = accountService.getById(vo.getId());
			BizAccount oldAccount = new BizAccount();// 旧account
			BizAccount db = new BizAccount();// 对象复制出来，以免每个set都去修改数据库自动保存导致程序变慢
			BeanUtils.copyProperties(oldAccount, accountInfo);
			BeanUtils.copyProperties(db, accountInfo);
			// 云闪付
			if (!ObjectUtils.isEmpty(vo.getType()) && !ObjectUtils.isEmpty(vo.getSubType())
					&& vo.getType().equals(AccountType.InBank.getTypeId()) && vo.getSubType() == 3) {
				// 新旧类型对比是否做出更新
				if (ObjectUtils.isEmpty(oldAccount.getType()) || ObjectUtils.isEmpty(oldAccount.getSubType())
						|| !oldAccount.getType().equals(vo.getType())
						|| !oldAccount.getSubType().equals(vo.getSubType())) {
					log.error("不允许修改为云闪付入款卡");
					throw new Exception("不允许修改为云闪付入款卡");
				}
			}
			// 无层级修改为有层级 或者修改未传入层级（会以之前的层级为准）都不需要校验
			if (null != vo.getCurrSysLevel() && null != db.getCurrSysLevel()
					&& vo.getCurrSysLevel().intValue() != db.getCurrSysLevel().intValue()) {
				if (db.getCurrSysLevel().equals(CurrentSystemLevel.Outter.getValue())
						|| db.getCurrSysLevel().equals(CurrentSystemLevel.Designated.getValue())) {
					log.error("外层或者指定层的账号不允许修改为内层和中层");
					throw new Exception("外层或者指定层的账号不允许修改为内层和中层");
				}
			}
			if (null != vo.getLimitOutOne() && null != vo.getLimitOutOneLow()
					&& (vo.getLimitOutOne() < vo.getLimitOutOneLow())) {
				log.error("“最高出款限额”必须大于“最低出款限额”");
				throw new Exception("“最高出款限额”必须大于“最低出款限额”");
			}
			Date date = new Date();
			// 顺序勿更换
			db.setStatus(vo.getStatus() == null ? db.getStatus() : vo.getStatus());
			db.setProvince(ObjectUtils.isEmpty(vo.getProvince()) ? db.getProvince() : vo.getProvince());
			db.setCity(ObjectUtils.isEmpty(vo.getCity()) ? db.getCity() : vo.getCity());
			db.setHandicapId(vo.getHandicapId() == null ? db.getHandicapId() : vo.getHandicapId());
			db.setType(vo.getType() == null ? db.getType() : vo.getType());
			db.setSubType(vo.getSubType() == null ? db.getSubType() : vo.getSubType());
			// 层级
			db.setCurrSysLevel(vo.getCurrSysLevel() == null ? db.getCurrSysLevel() : vo.getCurrSysLevel());
			// 限额
			db.setMinInAmount(vo.getMinInAmount() == null ? db.getMinInAmount() : vo.getMinInAmount());// 最小入款金额
			db.setLimitBalance(vo.getLimitBalance() == null ? db.getLimitBalance() : vo.getLimitBalance());// 余额告警
			db.setLimitIn(vo.getLimitIn() == null ? db.getLimitIn() : vo.getLimitIn());// 当日入款限额
			db.setLimitOut(vo.getLimitOut() == null ? db.getLimitOut() : vo.getLimitOut());// 当日出款限额
			db.setLimitOutOne(vo.getLimitOutOne() == null ? db.getLimitOutOne() : vo.getLimitOutOne());// 单笔最高出款
			db.setLimitOutOneLow(vo.getLimitOutOneLow() == null ? db.getLimitOutOneLow() : vo.getLimitOutOneLow());// 单笔最低出款
			db.setLowestOut(vo.getLowestOut() == null ? db.getLowestOut() : vo.getLowestOut());// 最低余额限制
			db.setLimitOutCount(vo.getLimitOutCount() == null ? db.getLimitOutCount() : vo.getLimitOutCount());// 当日出款笔数

			// 无编号时，自动生成编号
			if (StringUtils.isEmpty(db.getAlias())) {
				// 编号六位数，跳过为4的数字 从100000开始递增
				String maxAlias = accountService.getMaxAlias();
				if (StringUtils.isEmpty(maxAlias) || maxAlias.equals("0")) {
					db.setAlias("100000");
				} else {
					int alias = Integer.parseInt(maxAlias) + 1;
					db.setAlias(Integer.toString(alias).replace("4", "5"));
				}
			}

			// 状态 顺序勿更换
			if (oldAccount.getStatus() != null && db.getStatus() != null
					&& !oldAccount.getStatus().equals(db.getStatus())) {
				//后续需要考虑到卡类型切换的问题
				if (!db.getType().equals(AccountType.OutBank.getTypeId())) {
					// 只要不是出款卡 清空持卡人
					db.setHolder(null);
				} else {
					db.setHolder(operator != null ? operator.getId() : null);
				}
				if (db.getStatus().equals(AccountStatus.Normal.getStatus())) {// 在用
					// 账号从下发黑名单剔除
					allocateTransferService.rmFrBlackList(db.getId());
				} else if (db.getStatus().equals(AccountStatus.StopTemp.getStatus())) {// 停用
				} else if (db.getStatus().equals(AccountStatus.Freeze.getStatus())) {// 冻结
					// 只有冻结 才保存冻结备注
					db.setRemark(CommonUtils.genRemark(db.getRemark(),
							"【" + oldAccount.getStatusStr() + "转冻结】" + (vo.getRemark() != null ? vo.getRemark() : ""),
							date, operator.getUid()));
					// 冻结的时候添加到待处理业务表、如果存在没有处理完的则不添加
					int count = finLessStatService.findCountsById(db.getId(), "portion");
					if (count <= 0) {
						finLessStatService.addTrace(db.getId(), db.getBankBalance());
					}
				}
				if (StringUtils.isNotBlank(vo.getRemark())) {
					// 如果输入了备注 所有状态变更原因保存到操作日志
					db.setRemark4Extra(db.getRemark4Extra() != null ? db.getRemark4Extra()
							: "" + "【状态变更原因】" + vo.getRemark4Extra() + ";\r\n");
				}
			}
			// 类型变更 out_enable holder本来就没值不用管（已激活去启用，所以不用管状态）
			if (!oldAccount.getType().equals(db.getType())) {
				// 如果新类型不是入款卡，清空sub_type和
				if (!db.getType().equals(AccountType.InBank.getTypeId())) {
					db.setSubType(null);
				}
			}
			db.setModifier(operator.getId());// 操作人
			db.setUpdateTime(date);// 最后更新时间
			accountService.save(db);
			accountExtraService.saveAccountExtraLog(oldAccount, db, operator.getUid());// 操作记录表
			hostMonitorService.update(db);
			log.debug("activated2enable>> id {}",db.getId());
			accountService.broadCast(db);// 广播 账号本地缓存刷新接口
			cabanaService.updAcc(db.getId());
			cabanaService.refreshAcc(db.getId());
			// 0元信用额度
			if (buildParams().containsKey("zerocredits")) {
				accMoreSer.setToZeroCredit(operator, db.getMobile());
				String remark4extra = "勾选了0元信用额度;\r\n";
				db.setRemark4Extra(StringUtils.isNotBlank(db.getRemark4Extra()) ? db.getRemark4Extra() + remark4extra
						: remark4extra);
			}
			// 账号类型或者状态有变更，更新Redis
			if (Objects.nonNull(db) && Objects.nonNull(oldAccount)
					&& (!Objects.equals(db.getType(), oldAccount.getType())
							|| !Objects.equals(db.getStatus(), oldAccount.getStatus()))) {
				incomeAccountAllocateService.update(db.getId(), db.getType(), db.getStatus());
			}
			// 是否更新了盘口信息 或者类型
			if (oldAccount.getHandicapId() == null || !oldAccount.getHandicapId().equals(db.getHandicapId())
					|| oldAccount.getType() == null || !oldAccount.getType().equals(db.getType())) {
				// 新盘口全部层级赋给此账号（会先清空原有层级）
				List<Object[]> levels = levelService.findByHandicapIdsArray(db.getHandicapId());
				if (null != levels && levels.size() > 0) {
					List<Integer> levelIdList = new ArrayList<Integer>();
					for (Object[] level : levels) {
						if (null != level && level.length > 0 && null != level[0]) {
							levelIdList.add(Integer.parseInt(level[0].toString()));
						}
					}
					if (levelIdList.size() > 0) {
						accountSyncService.saveAccountLevelAndFlush(levelIdList, db.getId(), db.getType());
					}
				} else {
					log.info(db.getHandicapId() + "盘口下的层级为空：" + levels.toString());
				}
			}
			GeneralResponseData<BizAccount> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			responseData.setData(db);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(String.format("%s，参数：%s，结果：%s", "账号更新", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getMessage()));
		}
	}
}
