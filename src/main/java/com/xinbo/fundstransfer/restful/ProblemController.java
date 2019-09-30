package com.xinbo.fundstransfer.restful;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.domain.*;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.*;
import com.xinbo.fundstransfer.domain.pojo.*;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.report.SystemAccountManager;
import com.xinbo.fundstransfer.report.up.ReportInitParam;
import com.xinbo.fundstransfer.service.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.util.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/r/problem")
public class ProblemController extends BaseController {
	@Autowired
	private CabanaService cabanaService;
	@Autowired
	private AccountService accSer;
	@Autowired
	private HandicapService handiSer;
	@Autowired
	private ProblemService problemService;
	@Autowired
	private RedisService redisService;
	@Autowired
	private HandicapService handicapSer;
	@Autowired
	private SysUserService userSer;
	@Autowired
	private SysErrService sysErrSer;
	@Autowired
	private SysInvstService sysInvstSer;
	@Autowired
	private SystemAccountManager systemAccountManager;

	/**
	 * 所有账号动态初始化
	 */
	@RequestMapping("/dynamicInitAll")
	public String dynamicInitAll() throws JsonProcessingException {
		SysUser loginUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(loginUser)) {
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		new Thread(() -> {
			List<SearchFilter> filterToList = new ArrayList<>();
			// 需要时时对账的账号分类：入款卡，出款卡，备用卡，下发卡；状态：非冻结，删除状态
			List<Integer> typeInList = new ArrayList<>();
			typeInList.add(AccountType.InBank.getTypeId());
			typeInList.add(AccountType.OutBank.getTypeId());
			typeInList.add(AccountType.ReserveBank.getTypeId());
			typeInList.add(AccountType.BindAli.getTypeId());
			typeInList.add(AccountType.BindWechat.getTypeId());
			typeInList.add(AccountType.ThirdCommon.getTypeId());
			typeInList.add(AccountType.BindCommon.getTypeId());
			filterToList.add(new SearchFilter("type", SearchFilter.Operator.IN, typeInList.toArray()));
			filterToList.add(new SearchFilter("status", SearchFilter.Operator.NOTEQ, AccountStatus.Freeze.getStatus()));
			filterToList.add(new SearchFilter("status", SearchFilter.Operator.NOTEQ, AccountStatus.Delete.getStatus()));
			SearchFilter[] filterToArray = filterToList.toArray(new SearchFilter[filterToList.size()]);
			List<Integer> idList = accSer.findAccountIdList(filterToArray);
			for (Integer accId : idList)
				systemAccountManager.rpush(new SysBalPush(accId, SysBalPush.CLASSIFY_INIT,
						new ReportInitParam(accId, loginUser.getId(), null)));
		}).start();
		return mapper.writeValueAsString(
				new ResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "success"));
	}

	/**
	 * 单个账号动态初始化
	 */
	@RequestMapping("/dynamicInitIndv")
	public String dynamicInitIndv(@RequestParam(value = "id") int id) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.nonNull(operator)) {
			systemAccountManager.rpush(
					new SysBalPush(id, SysBalPush.CLASSIFY_INIT, new ReportInitParam(id, operator.getId(), null)));
		} else {
			return mapper
					.writeValueAsString(new ResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请登陆"));
		}
		return mapper.writeValueAsString(
				new ResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "success"));
	}

	private String lastletters(String target, int size) {
		target = StringUtils.trimToEmpty(target);
		int l = target.length();
		return l < size ? StringUtils.EMPTY : "**" + target.substring(l - size, l);
	}

	/**
	 * 问题排查汇总
	 *
	 * @param tarHandicap
	 *            盘口ID
	 * @param tarBankType
	 *            银行类别 （中国银行，农业银行，建设银行）
	 * @param tarAlias
	 *            别名
	 * @param tarFlag
	 *            PC|手机|返利网
	 * @param tarLevel
	 *            外层|中层|内层|指定层
	 * @param stOcrTm
	 *            发生的开始时间
	 * @param edOcrTm
	 *            发生的结束时间
	 */
	@RequestMapping("/accInvTotal")
	public String accInvTotal(@RequestParam(value = "pageNo") Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "tarHandicap", required = false) Integer tarHandicap,
			@RequestParam(value = "tarBankType", required = false) String tarBankType,
			@RequestParam(value = "tarAlias", required = false) String tarAlias,
			@RequestParam(value = "tarFlag", required = false) Integer tarFlag,
			@RequestParam(value = "tarLevel", required = false) Integer tarLevel,
			@RequestParam(value = "stOcrTm", required = false) String stOcrTm,
			@RequestParam(value = "edOcrTm", required = false) String edOcrTm,
			@RequestParam(value = "stBal", required = false) BigDecimal stBal,
			@RequestParam(value = "edBal", required = false) BigDecimal edBal,
			@RequestParam(value = "status", required = false) Integer status,
			@RequestParam(value = "handicapId", required = false) Integer[] handicapId) throws JsonProcessingException {
		if (Objects.isNull(SecurityUtils.getSubject().getPrincipal())) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		if (StringUtils.isBlank(stOcrTm) || StringUtils.isBlank(edOcrTm)) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "时间不能为空"));
		}
		List<SearchFilter> filterToList = DynamicSpecifications.build(request);
		// 查询条件：盘口
		if (Objects.nonNull(tarHandicap) && tarHandicap != 0) {
			filterToList.add(new SearchFilter("targetHandicap", SearchFilter.Operator.EQ, tarHandicap));
		}
		// 查询条件：银行类别
		if (StringUtils.isNotBlank(tarBankType)) {
			filterToList.add(new SearchFilter("targetBankType", SearchFilter.Operator.EQ, tarBankType.trim()));
		}
		// 查询条件：账号别名
		if (StringUtils.isNotBlank(tarAlias)) {
			filterToList.add(new SearchFilter("targetAlias", SearchFilter.Operator.EQ, tarAlias.trim()));
		}
		// 查询条件：账号用途(PC,手机，返利网)
		if (Objects.nonNull(tarFlag)) {
			filterToList.add(new SearchFilter("targetFlag", SearchFilter.Operator.EQ, tarFlag));
		}
		// 查询条件：层级（外层，中层，内层，指定层）
		if (Objects.nonNull(tarLevel)) {
			filterToList.add(new SearchFilter("targetLevel", SearchFilter.Operator.EQ, tarLevel));
		}
		// 查询条件：异常发生时间
		if (StringUtils.isNotBlank(stOcrTm)) {
			filterToList
					.add(new SearchFilter("occurTime", SearchFilter.Operator.GTE, CommonUtils.string2Date(stOcrTm)));
		}
		// 查询条件：异常结束时间
		if (StringUtils.isNotBlank(edOcrTm)) {
			filterToList
					.add(new SearchFilter("occurTime", SearchFilter.Operator.LTE, CommonUtils.string2Date(edOcrTm)));
		}
		// 查询条件：排查结果
		if (Objects.nonNull(status)) {
			filterToList.add(new SearchFilter("status", SearchFilter.Operator.EQ, status));
		}
		// 查询条件：异常差额 开始值
		if (Objects.nonNull(stBal)) {
			filterToList.add(new SearchFilter("margin", SearchFilter.Operator.GTE, stBal));
		}
		// 查询条件：异常差额 结束值
		if (Objects.nonNull(edBal)) {
			filterToList.add(new SearchFilter("margin", SearchFilter.Operator.LTE, edBal));
		}
		// 查询条件：盘口
		if (handicapId != null && handicapId.length > 0) {
			if (handicapId.length > 1) {
				filterToList.add(new SearchFilter("targetHandicap", SearchFilter.Operator.IN, handicapId));
			} else {
				filterToList.add(new SearchFilter("targetHandicap", SearchFilter.Operator.EQ, handicapId[0]));
			}
		}
		PageRequest pageable = new PageRequest(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
				Sort.Direction.DESC, "status", "id");
		SearchFilter[] filterToArray = filterToList.toArray(new SearchFilter[filterToList.size()]);
		Specification<BizSysErr> specif = DynamicSpecifications.build(BizSysErr.class, filterToArray);
		Page<BizSysErr> data = sysErrSer.findPage(specif, pageable);
		if (!CollectionUtils.isEmpty(data.getContent())) {
			for (BizSysErr err : data.getContent()) {
				if (Objects.nonNull(err.getTargetHandicap())) {
					BizHandicap handicap = handicapSer.findFromCacheById(err.getTargetHandicap());
					if (Objects.nonNull(handicap)) {
						err.setHandicapName(handicap.getName());
					}
				}
				if (Objects.nonNull(err.getTargetLevel())) {
					CurrentSystemLevel currLevel = CurrentSystemLevel.valueOf(err.getTargetLevel());
					if (Objects.nonNull(currLevel)) {
						err.setLevelName(currLevel.getName());
					}
				}
				AccountBaseInfo base = accSer.getFromCacheById(err.getTarget());
				if (Objects.nonNull(base)) {
					String account = base.getAccount();
					int l = account.length();
					String st = l <= 4 ? account : account.substring(0, 4);
					String ed = l <= 4 ? account : account.substring(l - 4, l);
					int ln = StringUtils.trimToEmpty(base.getOwner()).length();
					String ownerSimp = "*" + SysBalUtils.last1letters(base.getOwner());
					if (ln > 2) {
						String tmp = StringUtils.trimToEmpty(base.getOwner());
						ownerSimp = tmp.substring(0, 1) + ownerSimp;
					}
					String simName = StringUtils.trimToEmpty(base.getBankType()) + "|" + ownerSimp + "</br>" + st + "*"
							+ ed;
					err.setSimpName(simName);
				}
				// 待排查任务状态
				if (Objects.nonNull(err.getStatus())) {
					SysErrStatus errStatus = SysErrStatus.findByStatus(err.getStatus());
					if (Objects.nonNull(errStatus)) {
						if (Objects.equals(errStatus.getStatus(), SysErrStatus.Locking.getStatus())) {
							err.setStatusName("<span style='color: #666666'>" + errStatus.getMsg() + "</span>");
						} else if (Objects.equals(errStatus.getStatus(), SysErrStatus.Locked.getStatus())) {
							err.setStatusName("<span style='color: #0066CC'>" + errStatus.getMsg() + "</span>");
						} else if (Objects.equals(errStatus.getStatus(), SysErrStatus.FinishedNormarl.getStatus())) {
							err.setStatusName("<span style='color: #00CC00'>" + errStatus.getMsg() + "</span>");
						} else if (Objects.equals(errStatus.getStatus(), SysErrStatus.FinishedFreeze.getStatus())) {
							err.setStatusName("<span style='color: #FF0000'>" + errStatus.getMsg() + "</span>");
						} else {
							err.setStatusName("---");
						}
					} else {
						err.setStatusName("---");
					}
				}
				AccountFlag accFlag = AccountFlag.findByTypeId(err.getTargetFlag());
				if (Objects.nonNull(accFlag)) {
					err.setFlagName(accFlag.getMsg());
				}
				err.setCollectorName(StringUtils.EMPTY);
				if (Objects.nonNull(err.getCollector())) {
					SysUser opr = userSer.findFromCacheById(err.getCollector());
					if (Objects.nonNull(opr)) {
						err.setCollectorName(opr.getUid());
					}
				}
				String timeSimp = "发生时间:" + CommonUtils.getDateStr(err.getOccurTime());
				if (Objects.nonNull(err.getCollectTime())) {
					timeSimp = timeSimp + "</br>锁定时间:" + CommonUtils.getDateStr(err.getCollectTime());
				}
				if (Objects.isNull(err.getCollectTime())) {
					timeSimp = timeSimp + "</br>排队耗时:"
							+ CommonUtils.convertTime2String(System.currentTimeMillis() - err.getOccurTime().getTime());
				} else {
					if (Objects.isNull(err.getConsumeTime()) || err.getConsumeTime() == 0) {
						timeSimp = timeSimp + "</br>处理耗时:" + CommonUtils
								.convertTime2String(System.currentTimeMillis() - err.getCollectTime().getTime());
					} else {
						timeSimp = timeSimp + "</br>处理耗时:"
								+ CommonUtils.convertTime2String(err.getConsumeTime() * 1000);
					}
				}
				// 银行卡状态
				if (Objects.nonNull(base)) {
					err.setAccStatus(base.getStatus());
					if (Objects.equals(base.getStatus(), AccountStatus.Normal.getStatus())) {
						err.setAccStatusName("<span style='color: #00CC00'>在用</span>");
					} else if (Objects.equals(base.getStatus(), AccountStatus.Enabled.getStatus())) {
						err.setAccStatusName("<span style='color: #00CC00'>可用</span>");
					} else if (Objects.equals(base.getStatus(), AccountStatus.StopTemp.getStatus())) {
						err.setAccStatusName("<span style='color: #FF0000;'>停用<span>");
					} else if (Objects.equals(base.getStatus(), AccountStatus.Freeze.getStatus())) {
						err.setAccStatusName("<span style='color: #000000'>冻结</span>");
					} else {
						AccountStatus st = AccountStatus.findByStatus(base.getStatus());
						String stName = Objects.isNull(st) ? StringUtils.EMPTY : st.getMsg();
						err.setAccStatusName("<span style='color: #00CC00'>" + stName + "</span>");
					}

				}
				err.setTimeSimp(timeSimp);
			}
		}
		GeneralResponseData<List<BizSysErr>> res = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		res.setData(data.getContent());
		res.setPage(new Paging(data));
		return mapper.writeValueAsString(res);
	}

	@RequestMapping("/accInvLock")
	public String accInvLock(@RequestParam(value = "errId") Long errId) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		try {
			sysErrSer.lock(errId, operator);
		} catch (Exception e) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getMessage()));
		}
		return mapper
				.writeValueAsString(new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
	}

	@RequestMapping("/accInvUnLock")
	public String accInvUnLock(@RequestParam(value = "errId") Long errId) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		try {
			sysErrSer.unlock(errId, operator);
		} catch (Exception e) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getMessage()));
		}
		return mapper
				.writeValueAsString(new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
	}

	@RequestMapping("/initErrorByErrorId")
	public String initErrorByErrorId(@RequestParam(value = "errorId") Long errorId,
			@RequestParam(value = "remark", required = false) String remark) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		try {
			systemAccountManager.initByErrorId(errorId, operator, remark);
		} catch (Exception e) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getMessage()));
		}
		return mapper
				.writeValueAsString(new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
	}

	@RequestMapping("/initErrorByAccountId")
	public String initErrorByAccountId(@RequestParam(value = "accountId") Integer accountId)
			throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		try {
			systemAccountManager.initByAccountId(accountId, operator);
		} catch (Exception e) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getMessage()));
		}
		return mapper
				.writeValueAsString(new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
	}

	@RequestMapping("/invstRemark")
	public String invstRemark(@RequestParam(value = "errorId") Long errorId,
			@RequestParam(value = "remark", required = false) String remark) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		try {
			systemAccountManager.invstRemark(errorId, operator, remark);
		} catch (Exception e) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getMessage()));
		}
		return mapper
				.writeValueAsString(new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
	}

	@RequestMapping("/accInvDoing")
	public String accInvDoing(@RequestParam(value = "errorId") Long errorId,
			@RequestParam(value = "errorLogs", required = false) String[] errorLogs,
			@RequestParam(value = "remark", required = false) String remark,
			@RequestParam(value = "invstResult", required = false) Integer invstResult,
			@RequestParam(value = "transferToOther", required = false) String transferToOther)
			throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		transferToOther = StringUtils.trimToNull(transferToOther);
		try {
			if (transferToOther != null) {
				systemAccountManager.transErrorToOther(errorId, operator, transferToOther, remark);
			} else {
				List<AccInvstDoing> doingList = new ArrayList<>();
				for (String item : errorLogs) {
					String[] d = item.split("#");
					Long bankLogId = Long.valueOf(d[0]);
					Integer invstType = Integer.valueOf(d[1]);
					String orderNo = d.length > 2 ? d[2] : null;
					doingList.add(new AccInvstDoing(bankLogId, invstType, orderNo));
				}
				systemAccountManager.invstError(errorId, doingList, operator, remark,
						SysErrStatus.findByStatus(invstResult));
			}
		} catch (Exception e) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getMessage()));
		}
		return mapper
				.writeValueAsString(new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
	}

	@RequestMapping("/findErrorByErrorId")
	public String findErrorByErrorId(@RequestParam(value = "errorId") Long errorId) throws JsonProcessingException {
		BizSysErr err = sysErrSer.findById(errorId);
		SysUser collector = userSer.findFromCacheById(err.getCollector());
		if (Objects.nonNull(err)) {
			if (collector.getId() == AppConstants.USER_ID_4_ADMIN) {
				err.setCollectorName("系统");
			} else {
				err.setCollectorName(collector.getUid());
			}
		} else {
			err.setCollectorName("无");
		}
		if (err.getConsumeTime() != null && err.getConsumeTime() != 0) {
			if (err.getCollectTime() == null) {
				err.setFinishTime(new Date(err.getOccurTime().getTime() + err.getConsumeTime() * 1000));
			} else {
				err.setFinishTime(new Date(err.getCollectTime().getTime() + err.getConsumeTime() * 1000));
			}
		} else {
			err.setFinishTime(new Date());
		}
		if (StringUtils.isNoneEmpty(err.getRemark())) {
			String[] ifd = err.getRemark().split("\r\n");
			err.setLastRemark(ifd.length == 1 ? err.getRemark() : ifd[ifd.length - 1]);
		}
		GeneralResponseData<BizSysErr> data = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		data.setData(err);
		return mapper.writeValueAsString(data);
	}

	@RequestMapping("/findInvstByErrorId")
	public String findInvstByErrorId(@RequestParam(value = "errorId") Long errorId) throws JsonProcessingException {
		List<BizSysInvst> dataList = sysInvstSer.findByErrorId(errorId);
		GeneralResponseData<List<BizSysInvst>> data = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		data.setData(dataList);
		return mapper.writeValueAsString(data);
	}

	@RequestMapping("/taskInv")
	public String taskInv(@RequestParam(value = "accId") Integer accId) throws JsonProcessingException {
		AccountBaseInfo base = accSer.getFromCacheById(accId);
		if (Objects.nonNull(base)) {
			String pattern = SysBalTrans.genPatternSus(accId, SysBalUtils.last3letters(base.getAccount()));
			List<SysBalTrans> dataList = redisService.getStringRedisTemplate().keys(pattern).stream()
					.map(SysBalTrans::new).collect(Collectors.toList());
			List<Map<String, Object>> result = new ArrayList<>();
			String frAccSim = StringUtils.EMPTY;
			/* from account base information */
			{
				if (StringUtils.isNotBlank(base.getAlias()))// alias
					frAccSim = frAccSim + base.getAlias();
				String owner = lastletters(base.getOwner(), 1);
				if (StringUtils.isNotBlank(owner))// owner
					frAccSim = frAccSim + (StringUtils.isNotBlank(frAccSim) ? "|" + owner : owner);
				String account = lastletters(base.getAccount(), 3);
				if (StringUtils.isNotBlank(account))// account
					frAccSim = frAccSim + (StringUtils.isNotBlank(frAccSim) ? "|" + account : account);
			}
			String frSim = frAccSim;
			dataList.stream().forEach(p -> {
				Map<String, Object> obj = new HashMap<>();
				obj.put("frSim", frSim);
				obj.put("amt", p.getAmt());
				String toSim = StringUtils.EMPTY;
				if (p.getToId() != 0) {
					AccountBaseInfo to = accSer.getFromCacheById(p.getToId());
					toSim = Objects.isNull(to) ? toSim : to.getAlias();
				}
				String owner = lastletters(p.getToOwn2Last(), 1);
				if (StringUtils.isNotBlank(owner))// owner
					toSim = toSim + (StringUtils.isNotBlank(toSim) ? "|" + owner : owner);
				String account = lastletters(p.getToAcc3Last(), 3);
				if (StringUtils.isNotBlank(account))// account
					toSim = toSim + (StringUtils.isNotBlank(toSim) ? "|" + account : account);
				obj.put("toSim", toSim);
				obj.put("time", CommonUtils.getDateStr(new Date(p.getAckTm())));
				obj.put("taskType", p.getTaskType());
				if (Objects.equals(p.getTaskType(), SysBalTrans.TASK_TYPE_INNER)) {
					obj.put("taskTypeName", "下发");
				} else if (Objects.equals(p.getTaskType(), SysBalTrans.TASK_TYPE_OUTREBATE)) {
					obj.put("taskTypeName", "返利");
				} else {
					obj.put("taskTypeName", "出款");
				}
				obj.put("msg", p.getMsg());
				result.add(obj);
			});
			GeneralResponseData<List<Map<String, Object>>> res = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			res.setData(result);
			return mapper.writeValueAsString(res);
		}
		return mapper.writeValueAsString(new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue()));
	}

	@RequestMapping("/accInv4Outward")
	public String accInv4Outward() throws JsonProcessingException {
		if (Objects.isNull(SecurityUtils.getSubject().getPrincipal()))
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		try {
			ResponseData<List<CabanaStatus>> data = cabanaService.status4Error();
			if (CollectionUtils.isEmpty(data.getData()) || !Objects.equals(data.getStatus(), 1))
				return mapper.writeValueAsString(data);
			List<Map<String, Object>> ret = new ArrayList<>();
			Set<Integer> ids = new HashSet<>();
			data.getData().forEach(p -> {
				ids.add(p.getId());
				if (StringUtils.isNotBlank(p.getError()))
					ret.add(pack(p.getId(), p));
			});
			ids.add(1);
			if (ids.size() > 0) {
				accSer.findAccountIdList(new SearchFilter("id", SearchFilter.Operator.NOTIN, ids),
						new SearchFilter("status", SearchFilter.Operator.EQ, AccountStatus.Normal.getStatus()),
						new SearchFilter("flag", SearchFilter.Operator.NOTEQ, AccountFlag.PC.getTypeId()))
						.forEach(p -> ret.add(pack(p, null)));
			}
			GeneralResponseData<List<Map<String, Object>>> re = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			re.setData(ret);
			re.setPage(new Paging(0, 1, ids.size()));
			return mapper.writeValueAsString(re);
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/eqpInv4Mobile")
	public String eqpInv4Mobile(@RequestParam(value = "pageNo", required = false) Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "status", required = false) Integer status,
			@RequestParam(value = "batteryStatus", required = false) Integer batteryStatus,
			@RequestParam(value = "dealStatus", required = false) String dealStatus,
			@RequestParam(value = "lockStatus", required = false) Integer lockStatus,
			@RequestParam(value = "rebate_deal", required = false) Integer rebate_deal,
			@RequestParam(value = "alias", required = false) String alias,
			@RequestParam(value = "offLineStatus", required = false) String offLineStatus,
			@RequestParam(value = "appVersion", required = false) String appVersion) throws JsonProcessingException {
		try {
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (Objects.isNull(operator)) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			GeneralResponseData<List> response = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			pageNo = pageNo == null ? 1 : pageNo;
			pageSize = pageSize == null ? AppConstants.PAGE_SIZE : pageSize;
			PageRequest pageRequest = new PageRequest(pageNo, pageSize);
			List<Predicate<DeviceStatus>> rules = new ArrayList<>();
			if (Objects.nonNull(status)) {
				Predicate<DeviceStatus> rule = device -> Objects.equals(device.getStatus(), status);
				rules.add(rule);
			}
			if (Objects.nonNull(batteryStatus)) {
				Predicate<DeviceStatus> rule = device -> Objects.equals(device.getBatteryStatus(), batteryStatus);
				rules.add(rule);
			}
			if (Objects.nonNull(offLineStatus)) {
				Predicate<DeviceStatus> rule = device -> Objects.equals(device.getOffLineStatus(), offLineStatus);
				rules.add(rule);
			}
			if (StringUtils.isNotBlank(dealStatus)) {
				Predicate<DeviceStatus> rule = device -> dealStatus.contains("," + device.getDealStatus() + ",");
				rules.add(rule);
			}
			if (Objects.nonNull(lockStatus)) {
				Predicate<DeviceStatus> rule = device -> Objects.equals(device.getLockStatus(), lockStatus);
				rules.add(rule);
			}
			if (StringUtils.isNotBlank(appVersion)) {
				Predicate<DeviceStatus> rule = device -> Objects.equals(device.getAppVersion(), appVersion);
				rules.add(rule);
			}
			if (StringUtils.isNotBlank(alias)) {
				List<BizAccount> accounts = accSer.findByAlias(alias);
				if (!CollectionUtils.isEmpty(accounts)) {
					Predicate<DeviceStatus> rule = device -> Objects.equals(device.getId(), accounts.get(0).getId());
					rules.add(rule);
				} else {
					List<DeviceStatus> res = new ArrayList<>();
					response.setData(res);
					return mapper.writeValueAsString(response);
				}
			}
			if (Objects.nonNull(rebate_deal)) {
				if (rebate_deal == 1) {
					Predicate<DeviceStatus> rule = device -> !StringUtils.equals(device.getOperator(), "ADMIN");
					rules.add(rule);
				} else {
					Predicate<DeviceStatus> rule = device -> StringUtils.equals(device.getOperator(), "ADMIN");
					rules.add(rule);
				}
			}
			Page<List> data = problemService.getProblemInfoList(operator, pageRequest, rules);
			response.setData(data.getContent());
			response.setPage(new Paging(data));
			return mapper.writeValueAsString(response);
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getLocalizedMessage()));
		}
	}

	private Map<String, Object> pack(Integer id, CabanaStatus st) {
		Map<String, Object> ret = new HashMap<>();
		AccountBaseInfo base = accSer.getFromCacheById(id);
		ret.put("id", base.getId());
		ret.put("account", CommonUtils.transToStarString(base.getAccount()));
		ret.put("bankName", base.getBankName());
		ret.put("bankType", base.getBankType());
		ret.put("owner", CommonUtils.transToStarString(base.getOwner()));
		ret.put("alias", base.getAlias());
		ret.put("type", base.getType());
		AccountType accType = AccountType.findByTypeId(base.getType());
		if (Objects.nonNull(accType)) {
			ret.put("typeStr", accType.getMsg());
		}
		ret.put("status", base.getStatus());
		ret.put("statusName", "在用");
		ret.put("online", false);
		BizHandicap handi = handiSer.findFromCacheById(base.getHandicapId());
		if (Objects.nonNull(handi))
			ret.put("handicapName", handi.getName());
		ret.put("mobile", CommonUtils.transToStarString(base.getMobile()));
		ret.put("peakBalance", base.getPeakBalance());
		if (Objects.nonNull(st)) {
			ret.put("id", st.getId());
			ret.put("time", st.getTime());
			ret.put("status", st.getStatus());
			ret.put("mode", st.getMode());
			ret.put("logtime", st.getLogtime());
			ret.put("balance", st.getBalance());
			ret.put("error", st.getError());
			ret.put("online", true);
		}
		return ret;
	}

	@RequestMapping("/devInvLock")
	public String devInvLock(@RequestParam(value = "mobile", required = true) String mobile,
			@RequestParam(value = "operator", required = false) String operator,
			@RequestParam(value = "id", required = false) String id) throws JsonProcessingException {
		SysUser currOperator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(currOperator)) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		try {
			boolean result = problemService.lock(mobile, currOperator, operator, id);
			if (!result) {
				return mapper.writeValueAsString(new GeneralResponseData<>(
						GeneralResponseData.ResponseStatus.FAIL.getValue(), "已被其他账号锁定，请刷新页面"));
			}
		} catch (Exception e) {
			mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getMessage()));
		}
		return mapper
				.writeValueAsString(new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
	}

	@RequestMapping("/devInvDeal")
	public String devInvDeal(@RequestParam(value = "mobile", required = true) String mobile,
			@RequestParam(value = "remark", required = true) String remark) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		try {
			problemService.deal(mobile, operator, remark);
		} catch (Exception e) {
			mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getMessage()));
		}
		return mapper
				.writeValueAsString(new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
	}

	@RequestMapping("/devInvUnLock")
	public String devInvUnLock(@RequestParam(value = "mobile") String mobile) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		try {
			problemService.unlock(mobile, operator);
		} catch (Exception e) {
			mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getMessage()));
		}
		return mapper
				.writeValueAsString(new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
	}

	@RequestMapping("/getDeviceStatus")
	public String getDeviceStatus(@RequestParam(value = "mobile") String mobile) throws JsonProcessingException {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		try {
			GeneralResponseData<Map<String, String>> response = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			Map<String, String> ds = problemService.getDeviceByMobile(mobile);
			if (ds != null) {
				response.setData(ds);
			}
			return mapper.writeValueAsString(response);
		} catch (Exception e) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getMessage()));
		}
	}

	@RequestMapping("/getContractInfo")
	public String getContractInfo(@RequestParam(value = "mobile") String mobile) throws JsonProcessingException {
		log.debug("getContractInfo>> mobile {}", mobile);
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		try {
			GeneralResponseData<Map<String, String>> response = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			Map<String, String> result = problemService.getContractInfo(mobile);
			response.setData(result);
			return mapper.writeValueAsString(response);
		} catch (Exception e) {
			log.error("getContractInfo>> mobile {},message {}", mobile, e.getLocalizedMessage());
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getMessage()));
		}
	}

	@RequestMapping("/getSysErr")
	public String getSysErr(@RequestParam(value = "accId") Long accId) throws JsonProcessingException {
		log.debug("getSysErr>> accId {}", accId);
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (Objects.isNull(operator)) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
		}
		try {
			BizSysErr bizSysErr = sysErrSer.findNotFinishedByAccId(accId);
			if (bizSysErr != null) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "存在待排查数据"));
			}
			GeneralResponseData<Map<String, String>> response = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "不存在待排查数据");
			return mapper.writeValueAsString(response);
		} catch (Exception e) {
			log.debug("getSysErr>> mobile {},message {}", accId, e.getLocalizedMessage());
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getMessage()));
		}
	}
}
