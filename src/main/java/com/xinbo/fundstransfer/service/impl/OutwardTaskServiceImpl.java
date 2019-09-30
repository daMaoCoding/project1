package com.xinbo.fundstransfer.service.impl;

import com.google.common.base.Preconditions;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.domain.DynamicSpecifications;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.*;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.OutwardTaskTotalInputDTO;
import com.xinbo.fundstransfer.domain.pojo.PageOutwardTaskCheck;
import com.xinbo.fundstransfer.domain.repository.OutwardTaskRepository;
import com.xinbo.fundstransfer.domain.repository.QueryNoCountDao;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import com.xinbo.fundstransfer.service.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class OutwardTaskServiceImpl implements OutwardTaskService {
	private static final Logger log = LoggerFactory.getLogger(OutwardTaskServiceImpl.class);
	private OutwardTaskRepository outwardTaskRepository;
	private HandicapService handicapService;
	private AccountService accountService;
	private SysUserService userService;
	private LevelService levelService;
	@Autowired
	private RedisService redisService;
	@PersistenceContext
	private EntityManager entityManager;
	private QueryNoCountDao queryNoCountDao;

	@Autowired
	@Lazy
	public OutwardTaskServiceImpl(OutwardTaskRepository outwardTaskRepository, HandicapService handicapService,
			AccountService accountService, SysUserService userService, LevelService levelService,
			QueryNoCountDao queryNoCountDao) {
		this.outwardTaskRepository = outwardTaskRepository;
		this.handicapService = handicapService;
		this.accountService = accountService;
		this.userService = userService;
		this.levelService = levelService;
		this.queryNoCountDao = queryNoCountDao;
	}

	public EntityManager getEntityManager() {
		return entityManager;
	}

	public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	@Autowired
	@Lazy
	private AllocateOutwardTaskService outwardTaskAllocateService;
	@Autowired
	@Lazy
	private OutwardRequestService outwardRequestService;

	@Override
	public BizOutwardTask findByIdAndStatusIn(Long id) {
		BizOutwardTask task = outwardTaskRepository.findByIdAndStatus(id);
		return task;
	}

	/**
	 * 根据taskid集合查询 id status remark 返回map taskid是为key status remark数组为值
	 * object[]--obj0 status obj1 remark
	 * 
	 * @param taskIds
	 * @return map : k-v: taskId- object[]--obj0 status obj1 remark
	 */
	@Override
	public Map<Long, Object[]> findStatusRemarkByTaskId(List<Long> taskIds) {
		log.debug("代付账号页面根据任务id集合查询 状态备注 参数:{}", taskIds);
		Map<Long, Object[]> res = new HashMap<>(16);
		try {
			Optional.ofNullable(taskIds).orElseThrow(() -> new Exception("taskId不能为空!"));
			List<Object[]> list = outwardTaskRepository.findIdAndStatusAndRemark(taskIds);
			log.debug("代付账号页面根据任务id集合查询 状态备注 查询结果:{}", list);
			if (!ObjectUtils.isEmpty(list)) {
				for (int i = 0, size = list.size(); i < size; i++) {
					// select id,status ,remark
					Object[] arr = new Object[3];
					Object[] indx = list.get(i);
					arr[0] = indx[1];
					arr[1] = indx[2];
					arr[2] = indx[3];
					res.put(Long.parseLong(indx[0].toString()), arr);
				}
			}
			log.debug("代付账号页面根据任务id集合查询 状态备注 返回结果:{}", res);
			return res;
		} catch (Exception e) {
			log.error("代付账号页面根据任务id集合查询 状态备注异常:", e);
		}
		return res;
	}

	@Override
	public BizOutwardTask findByOrderNoAndHandicapCode(String orderNo, String handicapCode) {
		BizOutwardTask bizOutwardTask = outwardTaskRepository.findDistinctByOrderNoAndHandicap(orderNo, handicapCode);
		return bizOutwardTask;
	}

	@Transactional(rollbackFor = { NumberFormatException.class, Exception.class })
	@Override
	public void thirdOutwardTaskDeal(BizOutwardTask bizOutwardTask, SysUser operator, Long taskId, Integer userId,
			String remark, Integer fromAccountId, String platPayCode) {
		Preconditions.checkNotNull(bizOutwardTask);
		final String daifuRemark = "调用代付成功";
		try {
			log.debug("参数:task:{},remark:{},platPayCode:{},", ObjectMapperUtils.serialize(bizOutwardTask), remark,
					platPayCode);
			outwardTaskAllocateService.ack4User(taskId, userId, remark, fromAccountId, true, platPayCode);
			BizOutwardRequest req = outwardRequestService.get(bizOutwardTask.getOutwardRequestId());
			log.debug("单号：" + req.getOrderNo() + "是第三方出款。");
			if (StringUtils.isNotBlank(remark)) {
				if (remark.contains(daifuRemark)) {
					outwardRequestService.saveThirdOut(fromAccountId, req.getAmount(), req, null, remark);
				} else {
					// 如果第三方备注不为空，且不是选择其它的第三方出款
					if (remark.indexOf("id") > 0) {
						log.debug("单号：" + req.getOrderNo() + "是第三方出款且备注不为空。");
						String[] remarkValue = remark.split("\\|");
						// 判断是否存在多个第三方账号出款
						if (remarkValue.length > 1) {
							log.debug("单号：" + req.getOrderNo() + "是第三方出款且备注不为空且是多个第三方账号出款。");
							for (int j = 0; j < remarkValue.length; j++) {
								BigDecimal thirdOutAmount = new BigDecimal(remarkValue[j].split(",")[2].split(":")[1]);
								int thirdId = Integer
										.valueOf(remarkValue[j].split(",")[3].split(":")[1].replace("}", ""));
								log.info("单号：" + req.getOrderNo() + "保存第三方出款信息到记录表。");
								outwardRequestService.saveThirdOut(thirdId, thirdOutAmount, req, operator, "出款操作！");
							}
						} else {
							log.debug("单号：" + req.getOrderNo() + "是第三方出款且备注不为空是一个第三方账号出款。");
							// 异常数据处理(存在不规范格式)
							remarkValue = remark.split(",");
							if (remarkValue.length > 1) {
								BigDecimal thirdOutAmount = new BigDecimal(remarkValue[2].split(":")[1]);
								int thirdId = Integer.valueOf(remarkValue[3].split(":")[1].replace("}", ""));
								log.info("单号：" + req.getOrderNo() + "保存第三方出款信息到记录表。");
								outwardRequestService.saveThirdOut(thirdId, thirdOutAmount, req, operator, "出款操作！");
							}
						}
					}
				}
			}

		} catch (NumberFormatException e) {
			log.error("处理第三方出款NumberFormatException异常:", e);
		} catch (Exception e) {
			log.error("处理第三方出款 Exception异常:", e);
		}
	}

	@Override
	public Double sumPhoneOut(OutwardTaskTotalInputDTO inputDTO, Integer[] fromAccount, Integer[] operatorIds) {
		String sql = " select sum(t.amount)  FROM biz_outward_task  t where  t.status in(1,5) and  exists (select a.id from  fundsTransfer.biz_account a  where t.account_id=a.id and a.flag=1 ) ";
		sql = wrapSqlForPhoneOut(sql, inputDTO, fromAccount, operatorIds);
		BigDecimal sum = (BigDecimal) entityManager.createNativeQuery(sql).getSingleResult();
		return sum == null ? 0 : sum.doubleValue();
	}

	@Override
	public Long countPhoneOut(OutwardTaskTotalInputDTO inputDTO, Integer[] fromAccount, Integer[] operatorIds) {
		String sql = " select count(t.id)  FROM biz_outward_task t where  t.status in(1,5) and  exists (select a.id from  fundsTransfer.biz_account a  where t.account_id= a.id and  a.flag=1 ) ";
		sql = wrapSqlForPhoneOut(sql, inputDTO, fromAccount, operatorIds);
		BigInteger count = (BigInteger) entityManager.createNativeQuery(sql).getSingleResult();
		return count.longValue();
	}

	@Override
	public List<Object[]> findPhoneOut(OutwardTaskTotalInputDTO inputDTO, Integer[] fromAccount,
			Integer[] operatorIds) {
		String sql = "SELECT t.id,t.outward_request_id,t.amount,t.asign_time,t.time_consuming,t.operator,t.status,t.account_id,t.remark,t.screenshot,t.to_account,t.handicap,t.level,t.member,t.order_no FROM biz_outward_task t where t.status in(1,5) and  exists (select a.id from  fundsTransfer.biz_account a  where a.id=t.account_id  and a.flag=1 )  ";
		sql = wrapSqlForPhoneOut(sql, inputDTO, fromAccount, operatorIds);
		List<Object[]> list = entityManager.createNativeQuery(sql)
				.setFirstResult(inputDTO.getPageNo() * inputDTO.getPageSize()).setMaxResults(inputDTO.getPageSize())
				.getResultList();
		return list;
	}

	private String wrapSqlForPhoneOut(String sql, OutwardTaskTotalInputDTO inputDTO, Integer[] fromAccount,
			Integer[] operatorIds) {
		if (StringUtils.isNotBlank(inputDTO.getHandicap())) {
			sql += " and t.handicap=" + inputDTO.getHandicap();
		}
		if (StringUtils.isNotBlank(inputDTO.getLevel())) {
			sql += " and t.level =\'" + inputDTO.getLevel() + "\'";
		}
		if (StringUtils.isNotBlank(inputDTO.getStartTime())) {
			sql += " and t.asign_time >=\'" + inputDTO.getStartTime() + "\'";
		}
		if (StringUtils.isNotBlank(inputDTO.getEndTime())) {
			sql += " and t.asign_time <=\'" + inputDTO.getEndTime() + "\'";
		}
		if (inputDTO.getFromMoney() != null) {
			sql += " and t.amount >=" + inputDTO.getFromMoney();
		}
		if (inputDTO.getToMoney() != null) {
			sql += " and t.amount <=" + inputDTO.getToMoney();
		}
		if (StringUtils.isNotBlank(inputDTO.getMember())) {
			sql += " and t.member =\'" + inputDTO.getMember() + "\'";
		}
		if (fromAccount != null && fromAccount.length == 0) {
			if (fromAccount.length == 1) {
				sql += " and t.account_id =" + fromAccount[0];
			} else {
				sql += " and t.account_id in (";
				for (int i = 0, len = fromAccount.length; i < len; i++) {
					if (i < len - 1) {
						sql += fromAccount[i] + ",";
					} else {
						sql += fromAccount[i] + ")";
					}
				}
			}
		}
		if (operatorIds != null && operatorIds.length > 0) {
			if (operatorIds.length == 1) {
				sql += " and t.operator =" + operatorIds[0];
			} else {
				sql += " and t.operator in(";
				for (int i = 0, len = operatorIds.length; i < len; i++) {
					if (i < len - 1) {
						sql += operatorIds[i] + ",";
					} else {
						sql += operatorIds[i] + ")";
					}
				}
			}
		}
		if (StringUtils.isNotBlank(inputDTO.getDrawType())) {
			// 只适用完成出款查询的时候
			if ("third".equals(inputDTO.getDrawType())) {
				sql += "and t.account_id is null ";
			}
			if ("bank".equals(inputDTO.getDrawType())) {
				sql += "and t.account_id is not  null ";
			}
		}
		if (inputDTO.getRobot() != null) {
			sql += " and t.operator is   null";
		}
		if (inputDTO.getManual() != null) {
			sql += " and t.operator is not null";
		}
		sql += " order by t.asign_time desc ";
		return sql;
	}

	@Override
	public List<BizOutwardTask> findByRequestId(Long outwardRequestId) {
		return outwardTaskRepository.findByOutwardRequestId(outwardRequestId);
	}

	@Override
	public List<Map<String, Object>> queryInfoByIdList(List<Long> idList) {
		List<Map<String, Object>> result = new ArrayList<>();
		if (CollectionUtils.isEmpty(idList)) {
			return result;
		}
		List<String> idListI = new ArrayList<>();
		idList.forEach(p -> idListI.add(p.toString()));
		StringBuilder sbSql = new StringBuilder();
		sbSql.append(
				"select task.id,task.amount,task.asign_time confirmTime,task.account_id fromAccountId,biz_account.account fromAccount,req.to_account toAccount,sys_user.uid confirmUid,task.remark taskRemark,task.screenshot screenshot from biz_outward_task task ");
		sbSql.append("left join biz_outward_request req on task.outward_request_id=req.id ");
		sbSql.append("left join biz_account on task.account_id=biz_account.id ");
		sbSql.append("left join sys_user on sys_user.id=task.operator ");
		sbSql.append("where true and task.id in (").append(String.join(",", idListI)).append(")");
		List<Object> dataList = entityManager.createNativeQuery(sbSql.toString()).getResultList();
		dataList.forEach(p -> result.add(new HashMap<String, Object>() {
			{
				Object[] countArr = (Object[]) p;
				put("id", countArr[0]);
				put("amount", countArr[1]);
				put("confirmTime", countArr[2]);
				put("fromAccountId", countArr[3]);
				put("fromAccount", countArr[4]);
				put("toAccount", countArr[5]);
				put("confirmUid", countArr[6]);
				put("taskRemark", countArr[7]);
				put("screenshot", countArr[8]);
			}
		}));
		return result;
	}

	@Override
	@Transactional
	public BizOutwardTask save(BizOutwardTask entity) {
		return outwardTaskRepository.save(entity);
	}

	@Override
	@Transactional
	public BizOutwardTask update(BizOutwardTask entity) {
		return outwardTaskRepository.saveAndFlush(entity);
	}

	@Override
	public List<?> findListJoinUser() {
		return outwardTaskRepository.findListJoinUser();
	}

	@Override
	public BizOutwardTask findById(Long id) {
		return outwardTaskRepository.findById2(id);
	}

	@Override
	public List<BizOutwardTask> findList(Specification<BizOutwardTask> specification, Sort sort) {
		return outwardTaskRepository.findAll(specification, sort);
	}

	@Override
	public PageOutwardTaskCheck findPage4Check(String operator, Long outwardTaskId, Integer fromAccountId,
			Integer handicapId, Integer levelId, Integer status, Date startTime, Date endTime, BigDecimal minAmount,
			BigDecimal maxAmount, String toAccount, Pageable pageable) {
		Page<Object> dataToPage = outwardTaskRepository.findAllForCheck(outwardTaskId, fromAccountId, handicapId,
				levelId, status, startTime, endTime, minAmount, maxAmount, toAccount, pageable);
		PageOutwardTaskCheck result = new PageOutwardTaskCheck(new Paging(dataToPage));
		PageOutwardTaskCheck.OutwardTaskCheckContent item;
		for (Object data : dataToPage.getContent()) {
			Object[] valueToArray = (Object[]) data;
			Long _outwardTaskId = Long.valueOf((Integer) valueToArray[0]);
			Integer _outwardRequestId = (Integer) valueToArray[1];
			Integer _fromAccountId = (Integer) valueToArray[2];
			BigDecimal _amount = (BigDecimal) valueToArray[3];
			Date _asignTime = (Date) valueToArray[4];
			Integer _handicapId = (Integer) valueToArray[5];
			Integer _levelId = (Integer) valueToArray[6];
			String _memberUserName = (String) valueToArray[7];
			String _toAccountBankName = (String) valueToArray[8];
			String _toAccount = (String) valueToArray[9];
			String _toAccountOwner = (String) valueToArray[10];
			SysUser _taskOperator = valueToArray[11] == null ? null
					: userService.findFromCacheById((Integer) valueToArray[11]);
			String _taskOperatorUid = _taskOperator == null ? StringUtils.EMPTY : _taskOperator.getUid();
			String _taskRemark = StringUtils.trimToEmpty((String) valueToArray[12]);
			Integer _status = (Integer) valueToArray[13];
			String _screenshot = (String) valueToArray[14];
			String _orderNo = (String) valueToArray[15];
			item = result.new OutwardTaskCheckContent(_outwardTaskId, _outwardRequestId, _fromAccountId, _amount,
					_asignTime, _handicapId, _levelId, _memberUserName, _toAccountBankName, _toAccount, _toAccountOwner,
					_taskOperatorUid, _taskRemark, _status, _screenshot, _orderNo);
			AccountBaseInfo base = accountService.getFromCacheById(_fromAccountId);
			if (!Objects.isNull(base)) {
				item.setFromAlias(base.getAlias());
				item.setFromBankType(base.getBankType());
				item.setFromOwner(base.getOwner());
			}
			result.push(item);
		}
		SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		result.getContent().forEach((p) -> {
			if (p.getHandicapId() != null) {
				BizHandicap handicap = handicapService.findFromCacheById(p.getHandicapId());
				p.setHandicapName(handicap == null ? StringUtils.EMPTY : handicap.getName());
			}
			if (p.getLevelId() != null) {
				BizLevel level = levelService.findFromCache(p.getLevelId());
				p.setLevelName(level == null ? StringUtils.EMPTY : level.getName());
			}
			if (p.getAsignTime() != null) {
				p.setAsignTimeStr(SDF.format(p.getAsignTime()));
			}
			if (p.getFromAccountId() != null) {
				BizAccount fromAccount = null;
				try {
					fromAccount = accountService.getById(p.getFromAccountId());
					p.setFromAccount(fromAccount == null ? StringUtils.EMPTY : fromAccount.getAccount());
				} catch (Exception e) {
					log.error("出款记录对账分页获取", e);
				}
			}
		});
		return result;
	}

	@Override
	public BigDecimal[] findTotal4Check(Long outwardTaskId, Integer fromAccountId, Integer handicapId, Integer levelId,
			Integer status, Date startTime, Date endTime, BigDecimal minAmount, BigDecimal maxAmount,
			String toAccount) {
		Object[] dataList = outwardTaskRepository.queryAmountAndFeeForCheckByTotal(outwardTaskId, fromAccountId,
				handicapId, levelId, status, startTime, endTime, minAmount, maxAmount, toAccount);
		Object[] result = (Object[]) dataList[0];
		return new BigDecimal[] { (BigDecimal) result[0], (BigDecimal) result[1] };
	}

	@Override
	public Page<BizOutwardTask> findOutwardTaskPageNoCount(List<String> handicapCodeList, String level, String orderNo,
			String member, String drawType, Integer[] operatorIds, Integer[] accountId, String startTime,
			String endTime, BigDecimal fromMoney, BigDecimal toMoney, String operatorType, Integer[] status,
			PageRequest pageRequest, String sysLevel) {
		Specification<BizOutwardTask> specification = wrapSpecification(handicapCodeList, level, orderNo, member,
				drawType, operatorIds, accountId, startTime, endTime, fromMoney, toMoney, operatorType, status,
				sysLevel);
		return queryNoCountDao.findAll(specification, pageRequest, BizOutwardTask.class);
	}

	@Override
	public String getOutwardTaskSum(List<String> handicapCodeList, String level, String orderNo, String member,
			String drawType, Integer[] operatorIds, Integer[] accountId, String startTime, String endTime,
			BigDecimal fromMoney, BigDecimal toMoney, String operatorType, Integer[] status, String sysLevel) {
		return getOutwardTaskSumAmount(handicapCodeList, level, orderNo, member, drawType, operatorIds, accountId,
				startTime, endTime, fromMoney, toMoney, operatorType, status, sysLevel);
	}

	private void commonExpressions(Root<BizOutwardTask> root, CriteriaBuilder criteriaBuilder,
			List<Expression<Boolean>> expressions, List<String> handicapCodeList, String level, String orderNo,
			String member, String drawType, Integer[] operators, Integer[] accountId, String startTime, String endTime,
			BigDecimal fromMoney, BigDecimal toMoney, String operatorType, Integer[] status, String sysLevel) {

		final Path<Integer> operatorR = root.get("operator");
		final Path<Integer> statusR = root.get("status");
		final Path<Date> asignTime = root.get("asignTime");
		final Path<BigDecimal> amountR = root.get("amount");
		final Path<Integer> accountIdR = root.get("accountId");
		final Path<String> handicapR = root.get("handicap");
		final Path<String> levelR = root.get("level");
		final Path<String> memberR = root.get("member");
		final Path<String> orderNoR = root.get("orderNo");
		final Path<Integer> thirdInsteadPayR = root.get("thirdInsteadPay");
		final Path<Integer> currSysLevelR = root.get("currSysLevel");
		final Path<Integer> outwardPayTypeR = root.get("outwardPayType");

		final String mra = "mra";
		final String mr = "mr";
		final String third = "third";
		final String bank = "bank";
		final String daifu = "daifu";
		final String robot = "robot";
		final String manual = "manual";
		final String phone = "phone";
		final boolean check = !(status.length == 1 && (status[0].equals(OutwardTaskStatus.Undeposit.getStatus())
				|| status[0].equals(OutwardTaskStatus.DuringMaintain.getStatus())));
		if (check) {
			if (StringUtils.isNotBlank(startTime)) {
				Date startTime1 = CommonUtils.string2Date(startTime);
				if (status[0].equals(OutwardTaskStatus.ManageRefuse.getStatus())
						|| status[0].equals(OutwardTaskStatus.ManageCancel.getStatus())) {
					expressions.add(criteriaBuilder.or(criteriaBuilder.greaterThanOrEqualTo(asignTime, startTime1),
							asignTime.isNull()));
				} else {
					expressions.add(criteriaBuilder.greaterThanOrEqualTo(asignTime, startTime1));
				}
			}
			if (StringUtils.isNotBlank(endTime)) {
				Date endTime1 = CommonUtils.string2Date(endTime);
				if (status[0].equals(OutwardTaskStatus.ManageRefuse.getStatus())
						|| status[0].equals(OutwardTaskStatus.ManageCancel.getStatus())) {
					expressions.add(criteriaBuilder.or(criteriaBuilder.lessThanOrEqualTo(asignTime, endTime1),
							asignTime.isNull()));
				} else {
					expressions.add(criteriaBuilder.lessThanOrEqualTo(asignTime, endTime1));
				}
			}
		}
		if (StringUtils.isNotBlank(sysLevel)) {
			// 9为人工，其他的过滤层级
			if ("9".equals(sysLevel)) {
				expressions.add(outwardPayTypeR
						.in(new Integer[] { OutWardPayType.MANUAL.getType(), OutWardPayType.ThirdPay.getType() }));
			} else {
				expressions.add(criteriaBuilder.equal(currSysLevelR, Integer.parseInt(sysLevel)));
			}
		}
		if (status != null && status.length > 0) {
			if (status.length == 1) {
				if (status[0].equals(OutwardTaskStatus.ManagerDeal.getStatus())
						|| status[0].equals(OutwardTaskStatus.ManageRefuse.getStatus())
						|| status[0].equals(OutwardTaskStatus.ManageCancel.getStatus())
						|| status[0].equals(OutwardTaskStatus.Failure.getStatus())) {
					expressions.add(criteriaBuilder.equal(statusR, status[0]));
				} else {
					expressions.add(criteriaBuilder.equal(statusR, status[0]));
					if (status[0].equals(OutwardTaskStatus.Undeposit.getStatus())
							|| status[0].equals(OutwardTaskStatus.DuringMaintain.getStatus())) {
						// 未出款(包括银行维护期间的出款记录) 没有出款账号
						expressions.add(accountIdR.isNull());
						expressions.add(operatorR.isNull());
					}
				}
			} else {
				if (status.length == 2 && status[1] == 99999) {
					expressions.add(criteriaBuilder.equal(statusR, status[0]));
					expressions.add(criteriaBuilder.or(accountIdR.isNotNull(), operatorR.isNotNull()));
				} else {
					expressions.add(statusR.in(status));
				}
			}
		}
		if (StringUtils.isNotBlank(operatorType)) {
			if (daifu.equals(operatorType)) {
				// 代付--待排查里查询条件
				expressions.add(criteriaBuilder.equal(thirdInsteadPayR, 1));
				// 出款账号id
				if (!ObjectUtils.isEmpty(accountId)) {
					if (accountId.length == 1) {
						expressions.add(criteriaBuilder.or(criteriaBuilder.equal(accountIdR, accountId[0]),
								operatorR.isNotNull()));
					} else {
						expressions.add(criteriaBuilder.or(accountIdR.in(accountId), operatorR.isNotNull()));
					}
				}
			} else {
				// if (mra.equals(operatorType)) {
				// // 人工和手机或人工和PC
				// if (accountId != null && accountId.length > 0) {
				// if (accountId.length == 1) {
				// expressions.add(criteriaBuilder.or(
				// criteriaBuilder.and(operatorR.isNotNull(),
				// criteriaBuilder.equal(accountIdR, accountId[0])),
				// criteriaBuilder.equal(accountIdR, accountId[0])));
				// } else {
				// expressions.add(criteriaBuilder.or(
				// criteriaBuilder.and(operatorR.isNotNull(),
				// accountIdR.in(accountId)),
				// accountIdR.in(accountId)));
				// }
				// }
				// }
				// if (mr.equals(operatorType)) {
				// // 人工和手机或人工和PC
				// if (accountId != null && accountId.length > 0) {
				// if (accountId.length == 1) {
				// expressions.add(criteriaBuilder.or(operatorR.isNotNull(),
				// criteriaBuilder.equal(accountIdR, accountId[0])));
				// } else {
				// expressions.add(criteriaBuilder.or(operatorR.isNotNull(),
				// accountIdR.in(accountId)));
				// }
				// }
				// }
				if (robot.equals(operatorType)) {
					// PC或者手机
					if (accountId != null && accountId.length > 0) {
						if (accountId.length == 1) {
							expressions.add(criteriaBuilder.and(operatorR.isNull(),
									criteriaBuilder.equal(accountIdR, accountId[0])));
						} else {
							expressions.add(criteriaBuilder.and(operatorR.isNull(), accountIdR.in(accountId)));
						}
					} else {
						expressions.add(operatorR.isNull());
					}
				}
				if (phone.equals(operatorType)) {
					expressions.add(operatorR.isNull());
					// 返利网
					if (accountId != null && accountId.length > 0) {
						if (accountId.length == 1) {
							expressions.add(criteriaBuilder.equal(accountIdR, accountId[0]));
						} else {
							expressions.add(accountIdR.in(accountId));
						}
					}
				}
				if (manual.equals(operatorType)) {
					// 人工
					expressions.add(operatorR.isNotNull());
					if (accountId != null && accountId.length > 0) {
						if (accountId.length == 1) {
							expressions.add(criteriaBuilder.equal(accountIdR, accountId[0]));
						} else {
							expressions.add(accountIdR.in(accountId));
						}
					} else {
						expressions.add(accountIdR.isNotNull());
						// expressions.add(operatorR.isNotNull());
						// expressions.add(criteriaBuilder.notEqual(thirdInsteadPayR,
						// 1));
					}
				}
			}

		} else {
			if (!(StringUtils.isNotBlank(drawType) && (daifu.equals(drawType) || third.equals(drawType)))) {
				if (accountId != null && accountId.length > 0) {
					if (accountId.length == 1) {
						expressions.add(criteriaBuilder.equal(accountIdR, accountId[0]));
					} else {
						expressions.add(accountIdR.in(accountId));
					}
				}
			}
		}
		if (handicapCodeList != null && handicapCodeList.size() > 0) {
			if (handicapCodeList.size() == 1) {
				expressions.add(criteriaBuilder.equal(handicapR, StringUtils.trim(handicapCodeList.get(0))));
			} else {
				expressions.add(handicapR.in(handicapCodeList));
			}
		}
		if (StringUtils.isNotBlank(level)) {
			expressions.add(criteriaBuilder.equal(levelR, StringUtils.trim(level)));
		}
		if (StringUtils.isNotBlank(orderNo)) {
			expressions.add(criteriaBuilder.like(orderNoR, "%" + StringUtils.trim(orderNo) + "%"));
		}
		if (StringUtils.isNotBlank(member)) {
			expressions.add(criteriaBuilder.like(memberR, "%" + StringUtils.trim(member) + "%"));
		}
		if (fromMoney != null) {
			expressions.add(criteriaBuilder.greaterThanOrEqualTo(amountR, fromMoney));
		}
		if (toMoney != null) {
			expressions.add(criteriaBuilder.lessThanOrEqualTo(amountR, toMoney));
		}
		if (operators != null && operators.length > 0) {
			if (operators.length == 1) {
				expressions.add(criteriaBuilder.equal(operatorR, operators[0]));
			} else {
				expressions.add(operatorR.in(operators));
			}
		}
		if (StringUtils.isNotBlank(drawType)) {
			if (third.equals(drawType)) {
				expressions.add(accountIdR.isNull());
				expressions.add(
						criteriaBuilder.or(criteriaBuilder.notEqual(thirdInsteadPayR, 1), thirdInsteadPayR.isNull()));
			}
			// if (bank.equals(drawType)) {
			// expressions.add(accountIdR.isNotNull());
			// expressions.add(criteriaBuilder.notEqual(thirdInsteadPayR, 1));
			// }
			if (daifu.equals(drawType)) {
				expressions.add(criteriaBuilder.equal(thirdInsteadPayR, 1));
			}
		}
	}

	/**
	 * 获取总金额--出款任务 出款任务汇总 使用
	 */
	private String getOutwardTaskSumAmount(List<String> handicapCodeList, String level, String orderNo, String member,
			String drawType, Integer[] operators, Integer[] accountId, String startTime, String endTime,
			BigDecimal fromMoney, BigDecimal toMoney, String operatorType, Integer[] status, String sysLevel) {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<BigDecimal> query = criteriaBuilder.createQuery(BigDecimal.class);
		Root<BizOutwardTask> root = query.from(BizOutwardTask.class);
		Predicate predicate = criteriaBuilder.conjunction();
		List<Expression<Boolean>> expressions = predicate.getExpressions();

		final Path<BigDecimal> amountR = root.get("amount");
		Expression<BigDecimal> sumTrxAmt = criteriaBuilder.sum(amountR);
		commonExpressions(root, criteriaBuilder, expressions, handicapCodeList, level, orderNo, member, drawType,
				operators, accountId, startTime, endTime, fromMoney, toMoney, operatorType, status, sysLevel);
		query.where(predicate);
		query.select(sumTrxAmt);
		BigDecimal result = entityManager.createQuery(query).getSingleResult();
		result = (null == result ? BigDecimal.ZERO : result);
		return result.toString();
	}

	/**
	 * 获取总记录数--出款任务 出款任务汇总 使用
	 */
	@Override
	public Long getOutwardTaskCount(List<String> handicapCodeList, String level, String orderNo, String member,
			String drawType, Integer[] operators, Integer[] accountId, String startTime, String endTime,
			BigDecimal fromMoney, BigDecimal toMoney, String operatorType, Integer[] status, String sysLevel) {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
		Root<BizOutwardTask> root = query.from(BizOutwardTask.class);
		Predicate predicate = criteriaBuilder.conjunction();
		List<Expression<Boolean>> expressions = predicate.getExpressions();
		final Path<Long> idR = root.get("id");
		Expression<Long> count = criteriaBuilder.count(idR);
		commonExpressions(root, criteriaBuilder, expressions, handicapCodeList, level, orderNo, member, drawType,
				operators, accountId, startTime, endTime, fromMoney, toMoney, operatorType, status, sysLevel);
		query.where(predicate);
		query.select(count);
		Long result = entityManager.createQuery(query).getSingleResult();
		result = (null == result ? 0L : result);
		return result;
	}

	/**
	 * 出款任务 出款任务汇总页签使用
	 */
	private Specification<BizOutwardTask> wrapSpecification(List<String> handicapCodeList, String level, String orderNo,
			String member, String drawType, Integer[] operators, Integer[] accountId, String startTime, String endTime,
			BigDecimal fromMoney, BigDecimal toMoney, String operatorType, Integer[] status, String sysLevel) {
		Specification<BizOutwardTask> specification = (root, criteriaQuery, criteriaBuilder) -> {
			Predicate predicate = null;
			final Path<Integer> operatorR = root.get("operator");
			final Path<Integer> statusR = root.get("status");
			final Path<Date> asignTime = root.get("asignTime");
			final Path<BigDecimal> amountR = root.get("amount");
			final Path<Integer> accountIdR = root.get("accountId");
			final Path<String> handicapR = root.get("handicap");
			final Path<String> levelR = root.get("level");
			final Path<String> memberR = root.get("member");
			final Path<String> orderNoR = root.get("orderNo");
			final Path<Integer> thirdInsteadPayR = root.get("thirdInsteadPay");
			final Path<Integer> sysLevelR = root.get("currSysLevel");
			final Path<Integer> outwardPayTypeR = root.get("outwardPayType");
			final boolean check = !(status.length == 1 && (status[0].equals(OutwardTaskStatus.Undeposit.getStatus())
					|| status[0].equals(OutwardTaskStatus.DuringMaintain.getStatus())));

			final String mra = "mra";
			final String mr = "mr";
			final String third = "third";
			final String bank = "bank";
			final String daifu = "daifu";
			final String robot = "robot";
			final String manual = "manual";
			final String phone = "phone";
			if (check) {
				// 处理时间 范围开始
				if (StringUtils.isNotBlank(startTime)) {
					Date startTime1 = CommonUtils.string2Date(startTime);
					if (status[0].equals(OutwardTaskStatus.ManageRefuse.getStatus())
							|| status[0].equals(OutwardTaskStatus.ManageCancel.getStatus())) {
						predicate = addAndPredicate(criteriaBuilder, predicate, criteriaBuilder
								.or(criteriaBuilder.greaterThanOrEqualTo(asignTime, startTime1), asignTime.isNull()));
					} else {
						predicate = addAndPredicate(criteriaBuilder, predicate,
								criteriaBuilder.greaterThanOrEqualTo(asignTime, startTime1));
					}

				}
				if (StringUtils.isNotBlank(endTime)) {
					Date endTime1 = CommonUtils.string2Date(endTime);
					if (status[0].equals(OutwardTaskStatus.ManageRefuse.getStatus())
							|| status[0].equals(OutwardTaskStatus.ManageCancel.getStatus())) {
						predicate = addAndPredicate(criteriaBuilder, predicate, criteriaBuilder
								.or(criteriaBuilder.lessThanOrEqualTo(asignTime, endTime1), asignTime.isNull()));
					} else {
						predicate = addAndPredicate(criteriaBuilder, predicate,
								criteriaBuilder.lessThanOrEqualTo(asignTime, endTime1));
					}
				}

			}
			if (StringUtils.isNotBlank(sysLevel)) {
				// 9为人工，其他的过滤层级
				if ("9".equals(sysLevel)) {
					predicate = addAndPredicate(criteriaBuilder, predicate, outwardPayTypeR
							.in(new Integer[] { OutWardPayType.MANUAL.getType(), OutWardPayType.ThirdPay.getType() }));
				} else {
					predicate = addAndPredicate(criteriaBuilder, predicate,
							criteriaBuilder.equal(sysLevelR, Integer.parseInt(sysLevel)));
				}
			}
			if (status != null && status.length > 0) {
				if (status.length == 1) {
					// 除了正在匹配 和 完成匹配的status length=2 其他是 1
					if (status[0].equals(OutwardTaskStatus.ManagerDeal.getStatus())
							|| status[0].equals(OutwardTaskStatus.ManageRefuse.getStatus())
							|| status[0].equals(OutwardTaskStatus.ManageCancel.getStatus())
							|| status[0].equals(OutwardTaskStatus.Failure.getStatus())) {
						// 主管处理 拒绝 取消 待排查
						predicate = addAndPredicate(criteriaBuilder, predicate,
								criteriaBuilder.equal(statusR, status[0]));
					} else {
						// 未出款
						predicate = addAndPredicate(criteriaBuilder, predicate,
								criteriaBuilder.equal(statusR, status[0]));
						if (status[0].equals(OutwardTaskStatus.Undeposit.getStatus())
								|| status[0].equals(OutwardTaskStatus.DuringMaintain.getStatus())) {
							// 未出款(包括银行维护 DuringMaintain 期间的出款记录) 没有出款账号
							predicate = addAndPredicate(criteriaBuilder, predicate,
									criteriaBuilder.and(accountIdR.isNull(), operatorR.isNull()));
						}
					}
				} else {
					// 正在出款 完成出款
					if (status.length == 2 && status[1] == 99999) {
						// 99999 区分未出款和正在出款(包括重新分配给第三方的)
						predicate = addAndPredicate(criteriaBuilder, predicate,
								criteriaBuilder.equal(statusR, status[0]));
						predicate = addAndPredicate(criteriaBuilder, predicate,
								criteriaBuilder.or(accountIdR.isNotNull(), operatorR.isNotNull()));
					} else {
						predicate = addAndPredicate(criteriaBuilder, predicate, statusR.in(status));
					}
				}
			}
			if (StringUtils.isNotBlank(operatorType)) {
				if (daifu.equals(operatorType)) {
					// 代付--待排查里查询条件
					predicate = addAndPredicate(criteriaBuilder, predicate, criteriaBuilder.equal(thirdInsteadPayR, 1));
					// 出款账号id
					if (!ObjectUtils.isEmpty(accountId)) {
						if (accountId.length == 1) {
							predicate = addAndPredicate(criteriaBuilder, predicate, criteriaBuilder
									.or(criteriaBuilder.equal(accountIdR, accountId[0]), operatorR.isNotNull()));
						} else {
							predicate = addAndPredicate(criteriaBuilder, predicate,
									criteriaBuilder.or(accountIdR.in(accountId), operatorR.isNotNull()));
						}
					}
				} else {
					// if (mra.equals(operatorType)) {
					// // 一定有输入出款账号 pc/手机 人工
					// if (accountId != null && accountId.length > 0) {
					// // 出款账号id
					// if (accountId.length == 1) {
					// predicate = addAndPredicate(criteriaBuilder, predicate,
					// criteriaBuilder.or(
					// criteriaBuilder.and(criteriaBuilder.equal(accountIdR,
					// accountId[0]),
					// operatorR.isNotNull()),
					// criteriaBuilder.equal(accountIdR, accountId[0])));
					// } else {
					// predicate = addAndPredicate(criteriaBuilder, predicate,
					// criteriaBuilder.or(
					// criteriaBuilder.and(accountIdR.in(accountId),
					// operatorR.isNotNull()),
					// accountIdR.in(accountId)));
					// }
					// }
					// }
					// if (mr.equals(operatorType)) {
					// // 人工或手机,人工或PC
					// if (accountId != null && accountId.length > 0) {
					// // 出款账号id
					// if (accountId.length == 1) {
					// predicate = addAndPredicate(criteriaBuilder, predicate,
					// criteriaBuilder
					// .or(criteriaBuilder.equal(accountIdR, accountId[0]),
					// operatorR.isNotNull()));
					// } else {
					// predicate = addAndPredicate(criteriaBuilder, predicate,
					// criteriaBuilder.or(accountIdR.in(accountId),
					// operatorR.isNotNull()));
					// }
					// }
					// }
					if (robot.equals(operatorType)) {
						// PC或者手机
						// predicate = addAndPredicate(criteriaBuilder,
						// predicate, operatorR.isNull());
						if (accountId != null && accountId.length > 0) {
							// 出款账号id
							if (accountId.length == 1) {
								predicate = addAndPredicate(criteriaBuilder, predicate, criteriaBuilder
										.and(criteriaBuilder.equal(accountIdR, accountId[0]), operatorR.isNull()));
							} else {
								predicate = addAndPredicate(criteriaBuilder, predicate,
										criteriaBuilder.and(operatorR.isNull(), accountIdR.in(accountId)));
							}
						} else {
							predicate = addAndPredicate(criteriaBuilder, predicate, operatorR.isNull());
						}
					} else if (phone.equals(operatorType)) {
						predicate = addAndPredicate(criteriaBuilder, predicate, operatorR.isNull());
						// 手机 --返利网
						if (accountId != null && accountId.length > 0) {
							// 出款账号id
							if (accountId.length == 1) {
								predicate = addAndPredicate(criteriaBuilder, predicate,
										criteriaBuilder.equal(accountIdR, accountId[0]));
							} else {
								predicate = addAndPredicate(criteriaBuilder, predicate, accountIdR.in(accountId));
							}
						}
					} else if (manual.equals(operatorType)) {
						// 人工
						predicate = addAndPredicate(criteriaBuilder, predicate, operatorR.isNotNull());
						if (accountId != null && accountId.length > 0) {
							// 出款账号id
							if (accountId.length == 1) {
								predicate = addAndPredicate(criteriaBuilder, predicate,
										criteriaBuilder.equal(accountIdR, accountId[0]));
							} else {
								predicate = addAndPredicate(criteriaBuilder, predicate, accountIdR.in(accountId));
							}
						} else {
							predicate = addAndPredicate(criteriaBuilder, predicate, accountIdR.isNotNull());
							// predicate = addAndPredicate(criteriaBuilder,
							// predicate,
							// operatorR.isNotNull());
							// predicate = addAndPredicate(criteriaBuilder,
							// predicate,
							// criteriaBuilder.notEqual(thirdInsteadPayR, 1));
						}
					}
				}
			} else {
				// 出款账号id
				if (!(StringUtils.isNotBlank(drawType) && (daifu.equals(drawType) || third.equals(drawType)))) {
					if (accountId != null && accountId.length > 0) {
						if (accountId.length == 1) {
							predicate = addAndPredicate(criteriaBuilder, predicate,
									criteriaBuilder.equal(accountIdR, accountId[0]));
						} else {
							predicate = addAndPredicate(criteriaBuilder, predicate, accountIdR.in(accountId));
						}
					}
				}

			}
			if (handicapCodeList != null && handicapCodeList.size() > 0) {
				if (handicapCodeList.size() == 1) {
					predicate = addAndPredicate(criteriaBuilder, predicate,
							criteriaBuilder.equal(handicapR, StringUtils.trim(handicapCodeList.get(0))));
				} else {
					predicate = addAndPredicate(criteriaBuilder, predicate, handicapR.in(handicapCodeList));
				}
			}
			if (StringUtils.isNotBlank(level)) {
				predicate = addAndPredicate(criteriaBuilder, predicate,
						criteriaBuilder.equal(levelR, StringUtils.trim(level)));
			}
			if (StringUtils.isNotBlank(member)) {
				predicate = addAndPredicate(criteriaBuilder, predicate,
						criteriaBuilder.like(memberR, "%" + StringUtils.trim(member) + "%"));
			}
			if (StringUtils.isNotBlank(orderNo)) {
				predicate = addAndPredicate(criteriaBuilder, predicate,
						criteriaBuilder.like(orderNoR, "%" + StringUtils.trim(orderNo) + "%"));
			}
			if (fromMoney != null) {
				// 出款金额
				predicate = addAndPredicate(criteriaBuilder, predicate,
						criteriaBuilder.greaterThanOrEqualTo(amountR, fromMoney));
			}
			if (toMoney != null) {
				// 出款金额
				predicate = addAndPredicate(criteriaBuilder, predicate,
						criteriaBuilder.lessThanOrEqualTo(amountR, toMoney));
			}
			if (operators != null && operators.length > 0) {
				// 处理人id
				if (operators.length == 1) {
					predicate = addAndPredicate(criteriaBuilder, predicate,
							criteriaBuilder.equal(operatorR, operators[0]));
				} else {
					predicate = addAndPredicate(criteriaBuilder, predicate, operatorR.in(operators));
				}
			}
			if (StringUtils.isNotBlank(drawType)) {
				// 只适用完成出款查询的时候
				if (third.equals(drawType)) {
					predicate = addAndPredicate(criteriaBuilder, predicate, accountIdR.isNull());
					predicate = addAndPredicate(criteriaBuilder, predicate, criteriaBuilder.or(
							criteriaBuilder.notEqual(thirdInsteadPayR, 1), criteriaBuilder.isNull(thirdInsteadPayR)));
				}
				// if (bank.equals(drawType)) {
				// predicate = addAndPredicate(criteriaBuilder, predicate,
				// accountIdR.isNotNull());
				// predicate = addAndPredicate(criteriaBuilder, predicate,
				// criteriaBuilder.notEqual(thirdInsteadPayR, 1));
				// }
				if (daifu.equals(drawType)) {
					predicate = addAndPredicate(criteriaBuilder, predicate, criteriaBuilder.equal(thirdInsteadPayR, 1));
				}
			}
			return predicate;
		};
		return specification;
	}

	private Predicate addAndPredicate(final CriteriaBuilder criteriaBuilder, final Predicate oldPredicate,
			final Predicate newPredicate) {
		return oldPredicate != null ? criteriaBuilder.and(oldPredicate, newPredicate) : newPredicate;
	}

	private Predicate addOrPredicate(final CriteriaBuilder criteriaBuilder, final Predicate oldPredicate,
			final Predicate newPredicate) {
		return oldPredicate != null ? criteriaBuilder.or(oldPredicate, newPredicate) : newPredicate;
	}

	@Override
	public BizOutwardTask findOutwardTask(int fromAccountId, Float amount, String toAccountOrtoAccountOwner, int type,
			int cardType, int useLike) {
		log.debug("出款》 流水匹配  fromAccountId:{} amount:{} toAccountOrtoAccountOwner{} type{}", fromAccountId, amount,
				toAccountOrtoAccountOwner, type);
		if (useLike == 2) {
			return outwardTaskRepository.findOutwardTaskUseLike(fromAccountId, amount, toAccountOrtoAccountOwner, type,
					cardType);
		} else {
			return outwardTaskRepository.findOutwardTask(fromAccountId, amount, toAccountOrtoAccountOwner, type,
					cardType);
		}
		/*
		 * Query query = entityManager.createNativeQuery(
		 * "select * from  biz_outward_task where status=1 and account_id=?1 and amount=?2 "
		 * +
		 * " and (?4=2 or to_account=?3 ) and (?4=1 or to_account_owner=?3 ) order by asign_time desc limit 1"
		 * , BizOutwardTask.class); query.setParameter(1, fromAccountId);
		 * query.setParameter(2, amount); query.setParameter(3,
		 * toAccountOrtoAccountOwner); query.setParameter(4, type); return
		 * (BizOutwardTask) query.getSingleResult();
		 */
	}

	@Override
	public void manualOutMoney(String toAccountNo) {
		redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ARTIFICIAL_CARD).add(toAccountNo,
				BigDecimal.TEN.doubleValue());

	}

	@Override
	public void cancelArtificial(String toAccountNo) {
		redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ARTIFICIAL_CARD).remove(toAccountNo);

	}

	@Override
	public boolean checkManualOut4Member(String toAccountNo) {
		return redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ARTIFICIAL_CARD).score(toAccountNo) != null;
	}

	@Override
	public BizOutwardTask findReuseTask(int fromAccountId, Float amount, String toAccountOrtoAccountOwner, int type,
			int status) {
		log.info("ReuseTask 冲正重新生成任务  fromAccountId:{} amount:{} toAccountOrtoAccountOwner{} type{}", fromAccountId,
				amount, toAccountOrtoAccountOwner, type);
		return outwardTaskRepository.findReuseTask(fromAccountId, amount, toAccountOrtoAccountOwner, type, status);
	}

	@Override
	@Transactional
	public void updateStatusById(Long id, int status) {
		outwardTaskRepository.updateStatusById(id, status);
	}

	@Override
	@Transactional
	public void transToOther(Long taskId, SysUser operator, Integer transferToOther, String remark) {
		Objects.requireNonNull(taskId, "参数为空");
		Objects.requireNonNull(operator, "参数为空");
		Objects.requireNonNull(transferToOther, "参数为空");
		Objects.requireNonNull((remark = StringUtils.trimToNull(remark)), "备注为空");
		SysUser opp = Objects.requireNonNull(userService.findFromCacheById(transferToOther), "对方不存在");
		BizOutwardTask task = Objects.requireNonNull(outwardTaskRepository.findById2(taskId), "任务不存在");
		if (!Objects.equals(task.getOperator(), operator.getId()))
			Objects.requireNonNull(null, "非接单本人操作");
		remark = CommonUtils.genRemark(task.getRemark(), remark + "|至-" + opp.getUid(), new Date(), operator.getUid());
		task.setRemark(remark);
		task.setOperator(transferToOther);
		task.setAsignTime(new Date());
		if (Objects.nonNull(task.getAccountId())) {
			Specification<BizAccount> specif = DynamicSpecifications.build(BizAccount.class,
					new SearchFilter("type", SearchFilter.Operator.EQ, AccountType.OutBank.getTypeId()),
					new SearchFilter("status", SearchFilter.Operator.EQ, AccountStatus.Normal.getStatus()),
					new SearchFilter("holder", SearchFilter.Operator.EQ, transferToOther));
			List<BizAccount> accList = accountService.getAccountList(specif, null);
			if (Objects.isNull(accList))
				throw new RuntimeException("对方无绑定银行卡");
			if (accList.size() != 1)
				throw new RuntimeException("对方绑定多张银行卡");
			task.setAccountId(accList.get(0).getId());// 对方账号设置
		}
		outwardTaskRepository.saveAndFlush(task);
		log.info(
				"OUTWARD [ TRANSFER TASK TO OTHER PERSON ] >>  operator: {} operatorId: {} other: {} taskId: {} orderNo: {} amt: {} oppOwner: {} oppAccount: {} remark: {}",
				operator.getUid(), operator.getId(), transferToOther, task.getId(), task.getOrderNo(), task.getAmount(),
				task.getToAccountOwner(), task.getToAccount(), remark);
	}

	@Override
	public List<SysUser> outwardUserList(SysUser operator) {
		List<SysUser> ret = new ArrayList<>();
		Query outwardQative = entityManager.createNativeQuery(
				"select user_id from sys_user_role r,sys_role_menu_permission rm,sys_menu_permission m where r.role_id=rm.role_id and m.id=rm.menu_permission_id and m.permission_key='OutwardTask:*'");
		List<Object> objList = outwardQative.getResultList();
		List<Integer> data = new ArrayList<>();
		objList.stream().forEach(p -> data.add((Integer) p));
		for (Integer userId : data) {
			SysUser u = userService.findFromCacheById(userId);
			if (Objects.nonNull(u) && Objects.equals(u.getCategory(), operator.getCategory())
					&& Objects.equals(u.getHandicap(), operator.getHandicap())
					&& Objects.equals(u.getStatus(), UserStatus.ENABLED.getValue())) {
				ret.add(u);
			}
		}
		return ret;
	}

}
