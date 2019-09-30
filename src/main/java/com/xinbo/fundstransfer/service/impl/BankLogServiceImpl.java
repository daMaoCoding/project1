package com.xinbo.fundstransfer.service.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.criteria.*;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.xinbo.fundstransfer.component.spring.SpringContextUtils;
import com.xinbo.fundstransfer.utils.ServiceDomain;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.DynamicPredicate;
import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.enums.BankLogStatus;
import com.xinbo.fundstransfer.domain.enums.IncomeAuditWsEnum;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.FlowStatMatching;
import com.xinbo.fundstransfer.domain.pojo.IncomeAuditWs;
import com.xinbo.fundstransfer.domain.repository.BankLogRepository;
import com.xinbo.fundstransfer.domain.repository.QueryNoCountDao;
import com.xinbo.fundstransfer.domain.repository.TransactionLogRepository;
import com.xinbo.fundstransfer.service.*;

/**
 * @author Administrator
 */
@Service
public class BankLogServiceImpl implements BankLogService {
	private static final Logger log = LoggerFactory.getLogger(BankLogServiceImpl.class);
	@PersistenceContext
	private EntityManager em;
	@Autowired
	private BankLogRepository bankLogRepository;
	@Autowired
	private TransactionLogRepository transactionLogRepository;
	@Autowired
	@Lazy
	private AccountService accountService;
	@Autowired
	private RedisService redisService;
	@PersistenceContext
	private EntityManager entityManager;
	@Autowired
	private QueryNoCountDao queryNoCountDao;
	@Autowired
	private SysUserService sysUserService;
	@Autowired
	private HandicapService handicapService;
	private static ObjectMapper mapper = new ObjectMapper();
	private final static Integer CONSTANT_FOR_BANKLOG_UNCLAIMED_STATUS = 99;
	private static final String CUSTOMER_SENDMESSAGE_FAILED = "fail";
	private static final String CUSTOMER_SENDMESSAGE_SUCCEED = "succeed";
	private static boolean checkHostRunRight = false;

	@Value("${service.tag}")
	public void setServiceTag(String serviceTag) {
		if (Objects.nonNull(serviceTag)) {
			checkHostRunRight = ServiceDomain.valueOf(serviceTag) == ServiceDomain.WEB;
		}
	}

	@Override
	public BizBankLog findByIdLessThanEqual(Integer fromAccount, Long id) {
		return bankLogRepository.findByIdLessThanEqual(fromAccount, id);
	}

	private LoadingCache<String, HashMap<Integer, Integer>> cacheBuilder = CacheBuilder.newBuilder().maximumSize(1024)
			.refreshAfterWrite(10, TimeUnit.SECONDS).initialCapacity(512)
			.build(new CacheLoader<String, HashMap<Integer, Integer>>() {
				@Override
				public HashMap load(String key) {
					Date[] date = CommonUtils.getStart7AndEnd6();
					log.debug("入款卡未匹配流水数量 刷新缓存 时间:{},{}", date[0], date[1]);
					List<Object[]> count = countFlowToMatchAllInAccount(date[0], date[1]);
					HashMap<Integer, Integer> map = null;
					if (!CollectionUtils.isEmpty(count)) {
						map = new HashMap(512);
						for (Object[] obj : count) {
							map.put(Integer.valueOf(obj[0].toString()), Integer.valueOf(obj[1].toString()));
						}
					}
					return map;
				};
			});

	@Override
	public HashMap<Integer, Integer> countUnmatchFlowsFromCache(List<String> accountIds) {
		if (this.cacheBuilder == null || this.cacheBuilder.size() == 0) {
			this.cacheBuilder.refresh("income_account_bank_log_unmatched_count");
		}
		if (!CollectionUtils.isEmpty(accountIds)) {
			try {
				HashMap<Integer, Integer> cached = this.cacheBuilder.get("income_account_bank_log_unmatched_count");
				if (CollectionUtils.isEmpty(cached)) {
					return new HashMap<>(0);
				}
				HashMap<Integer, Integer> res = new HashMap<>(512);
				for (String accId : accountIds) {
					if (cached.containsKey(Integer.valueOf(accId))) {
						res.put(Integer.valueOf(accId), cached.get(Integer.valueOf(accId)));
					}
				}
				return res;
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * 统计未匹配的流水总数
	 */
	@Override
	public List<Object[]> countUnmatchFlows(List<String> accountIds, String startTime, String endTime) {
		String sql = "select  from_account,count(from_account) from biz_bank_log where status=0 and amount>=0 ";
		if (!CollectionUtils.isEmpty(accountIds)) {
			if (accountIds.size() == 1) {
				sql += " and from_account = " + accountIds.get(0);
			} else {
				sql += " and from_account in(";
				for (int i = 0, size = accountIds.size(); i < size; i++) {
					if (i < size - 1) {
						sql += accountIds.get(i) + ",";
					} else {
						sql += accountIds.get(i) + ")";
					}
				}
			}
		}
		if (StringUtils.isNotBlank(startTime)) {
			sql += "and create_time >= \'" + startTime + "\'";
		}
		if (StringUtils.isNotBlank(endTime)) {
			sql += "and create_time <= \'" + endTime + "\'";
		}
		sql += " group by from_account ";
		List<Object[]> list = (List<Object[]>) entityManager.createNativeQuery(sql).getResultList();
		return list;
	}

	/**
	 * 统计未匹配流水的数量
	 */
	@Override
	public List<Object[]> countUnmatchBankLogs(List<Integer> fromAccountIdList, Date startTime, Date endTime) {
		return bankLogRepository.countUnmatchBankLogs(fromAccountIdList, startTime, endTime);
	}

	/**
	 * 查询冲正
	 */
	@Override
	public List<Object[]> queryBackWashBankLong(Pageable pageable, List<String> handicap, String fromAccount,
			String orderNo, String operator, List<Integer> status, BigDecimal amountStart, BigDecimal amountEnd,
			Date startTime, Date endTime) {
		return bankLogRepository.queryBackWashBankLong(pageable, handicap, fromAccount, orderNo, operator, status,
				amountStart, amountEnd, startTime, endTime);
	}

	/**
	 * 查询冲正总记录数
	 */
	@Override
	public Long countBackWashBankLong(List<String> handicap, String fromAccount, String orderNo, String operator,
			List<Integer> status, BigDecimal amountStart, BigDecimal amountEnd, Date startTime, Date endTime) {
		return bankLogRepository.countBackWashBankLong(handicap, fromAccount, orderNo, operator, status, amountStart,
				amountEnd, startTime, endTime);
	}

	/**
	 * 查询冲正总金额
	 */
	@Override
	public BigDecimal sumBackWashBankLong(List<String> handicap, String fromAccount, String orderNo, String operator,
			List<Integer> status, BigDecimal amountStart, BigDecimal amountEnd, Date startTime, Date endTime) {
		return bankLogRepository.sumBackWashBankLong(handicap, fromAccount, orderNo, operator, status, amountStart,
				amountEnd, startTime, endTime);
	}

	/**
	 * 客服发送消息
	 *
	 * @param accountId
	 *            审核人审核的账号
	 * @param message
	 *            消息
	 */
	@Override
	public String customerSendMsg(Long bankFlowId, Long accountId, String message, String customerName) {
		BizBankLog bizBankLog = get(bankFlowId);
		BizAccount bizAccount = accountService.getById(accountId.intValue());
		if (bizBankLog == null || bizAccount == null) {
			return CUSTOMER_SENDMESSAGE_FAILED;
		}
		String flag = CUSTOMER_SENDMESSAGE_FAILED;
		int accountLen = 0;
		String account = bizAccount.getAccount();
		if (StringUtils.isNotBlank(bizAccount.getAccount())) {
			accountLen = account.length();
		}
		String account2 = "无";
		if (accountLen != 0) {
			account2 = account.substring(0, 3) + "***"
					+ account.substring(accountLen > 3 ? accountLen - 3 : accountLen);
		}
		String toAcc = "";// 对方账号隐藏***显示
		if (StringUtils.isNotBlank(bizBankLog.getToAccount())) {
			int len = bizBankLog.getToAccount().length();
			toAcc = bizBankLog.getToAccount().substring(0, 3) + "***"
					+ bizBankLog.getToAccount().substring(len > 3 ? len - 3 : len);
		}
		String toAccOwner = "";
		if (StringUtils.isNotBlank(bizBankLog.getToAccountOwner())) {
			toAccOwner = bizBankLog.getToAccountOwner();
			// int len = bizBankLog.getToAccountOwner().length();
			// toAccOwner = len > 10 ?
			// bizBankLog.getToAccountOwner().substring(0, 5) + "*"
			// + bizBankLog.getToAccountOwner().substring(6) : "*" +
			// bizBankLog.getToAccountOwner().substring(1);
		}
		String messages = "来自客服：" + (customerName == null ? "" : customerName) + "<br>关于银行流水的消息<br>消息内容："
				+ (message == null ? "" : message) + "<br>账号编号："
				+ (bizAccount.getAlias() == null ? "" : bizAccount.getAlias()) + "<br>收款账号：" + account2 + "<br>存款金额："
				+ (bizBankLog.getAmount() == null ? "0" : bizBankLog.getAmount()) + "<br>存款人：" + toAccOwner
				+ "<br>付款账号：" + toAcc + "";
		String keyPattern = RedisKeys.genPattern4IncomeAuditAccountAllocateByAccountId(accountId);
		Set<String> keys = redisService.getStringRedisTemplate().keys(keyPattern);
		if (keys == null || keys.size() == 0) {
			return flag;
		}
		SysUser incomeAuditor = sysUserService
				.findFromCacheById(Integer.valueOf(new ArrayList<>(keys).get(0).split(":")[1]));
		IncomeAuditWs noticeEntity = new IncomeAuditWs();
		noticeEntity.setIncomeAuditWsFrom(IncomeAuditWsEnum.CustomerMessage.ordinal());
		noticeEntity.setAccountId(accountId.intValue());
		noticeEntity.setMessage(messages);
		noticeEntity.setOwner(incomeAuditor.getUid());
		try {
			redisService.convertAndSend(RedisTopics.INCOME_REQUEST, mapper.writeValueAsString(noticeEntity));
			flag = CUSTOMER_SENDMESSAGE_SUCCEED;
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return flag;
	}

	@Override
	@Transactional
	public BizBankLog getBizBankLogByIdForUpdate(Long id) {
		return bankLogRepository.getBizBankLogByIdForUpdate(id);
	}

	/**
	 * 查询未认领公司银行流水 第三方流水 全部记录
	 */
	@Override
	public String findUnmatchForCompany(Integer[] accountIds, String member, Integer account, BigDecimal fromMoney,
			BigDecimal toMoney, String startTime, String endTime) {
		return getSumAmount(null, CONSTANT_FOR_BANKLOG_UNCLAIMED_STATUS, accountIds, account, null, fromMoney, toMoney,
				null, startTime, endTime);
	}

	/**
	 * 查询公司入款未认领 第三方流水 分页
	 */
	@Override
	public Page<BizBankLog> findUnmatchForCompanyPage(Integer[] accountIds, String payer, Integer accountId,
			BigDecimal fromMoney, BigDecimal toMoney, String startTime, String endTime, PageRequest pageRequest) {
		Specification<BizBankLog> specification = wrapSpecific(null, CONSTANT_FOR_BANKLOG_UNCLAIMED_STATUS, accountIds,
				accountId, payer, fromMoney, toMoney, null, startTime, endTime);
		return bankLogRepository.findAll(specification, pageRequest);
	}

	/**
	 * 通过id查询
	 */
	@Override
	public BizBankLog findBankFlowById(Long id) {
		return bankLogRepository.findOne(id);
	}

	/**
	 * 金额
	 */
	@Override
	public String getSumAmount(String payMan, Integer status, Integer handicap, Integer level, Integer fromAccount,
			Integer[] accountId, String member, BigDecimal fromMoney, BigDecimal toMoney, String startTime,
			String endTime) {
		return getSumAmount(payMan, status, accountId, fromAccount, member, fromMoney, toMoney, null, startTime,
				endTime);
	}

	/**
	 * 条件查询银行流水 未匹配的
	 */
	@Override
	public Page<BizBankLog> findBankLogPageNoCount(String payMan, Integer status, Integer handicap, Integer level,
			Integer[] fromAccount, String member, BigDecimal fromMoney, BigDecimal toMoney, String startTime,
			String endTime, PageRequest pageRequest) {
		Specification<BizBankLog> specification = wrapSpecific(payMan, status, fromAccount, null, member, fromMoney,
				toMoney, null, startTime, endTime);
		return queryNoCountDao.findAll(specification, pageRequest, BizBankLog.class);
	}

	/**
	 * 统计入款银行卡 未匹配流水总数量 默认是当日 如果传入时间 则根据实际时间查询
	 *
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	@Override
	public List<Object[]> countFlowToMatchAllInAccount(Date startTime, Date endTime) {
		List<Object[]> counts = bankLogRepository.countsUnmatchFlows(startTime, endTime);
		return counts;
	}

	/**
	 * 查总记录数
	 */
	@Override
	public String getCount(String payMan, Integer status, Integer handicap, Integer level, Integer fromAccount,
			Integer[] accountId, String member, BigDecimal fromMoney, BigDecimal toMoney, String startTime,
			String endTime) {
		return getCount(payMan, status, accountId, fromAccount, member, fromMoney, toMoney, null, startTime, endTime);
	}

	private Specification<BizBankLog> wrapSpecific(String payMan, Integer status, Integer[] accountIds,
			Integer accountId, String member, BigDecimal fromMoney, BigDecimal toMoney, String thirdName,
			String startTime, String endTime) {
		return (root, criteriaQuery, criteriaBuilder) -> {
			Predicate predicate = null;
			Path<Integer> fromAccountR = root.get("fromAccount");
			Path<Integer> statusR = root.get("status");
			Path<Date> createTimeR = root.get("createTime");
			Path<String> toAccountOwnerR = root.get("toAccountOwner");
			Path<BigDecimal> amountR = root.get("amount");
			// 索引列条件优先
			if (!status.equals(CONSTANT_FOR_BANKLOG_UNCLAIMED_STATUS) && StringUtils.isNotBlank(startTime)) {
				Date startTimeD = CommonUtils.string2Date(startTime);
				predicate = addAndPredicate(criteriaBuilder, predicate,
						criteriaBuilder.greaterThanOrEqualTo(createTimeR, startTimeD));// updateTimeR
			}
			if (!status.equals(CONSTANT_FOR_BANKLOG_UNCLAIMED_STATUS) && StringUtils.isNotBlank(endTime)) {
				Date endTimeD = CommonUtils.string2Date(endTime);
				predicate = addAndPredicate(criteriaBuilder, predicate,
						criteriaBuilder.lessThanOrEqualTo(createTimeR, endTimeD));
			}
			if (status != null) {
				if (!status.equals(CONSTANT_FOR_BANKLOG_UNCLAIMED_STATUS)) {
					if (BankLogStatus.Matching.getStatus().equals(status)) {
						// 24*3600*1000 正在匹配的流水未超过24小时
						// 这个条件不起作用 因为有了时间限制：当天07:00:00到第二天 06:59:59
						// Long unmatchTime1 = System.currentTimeMillis() -
						// 86400000L;
						// Date date1 = new Date(unmatchTime1);
						// predicate = addAndPredicate(criteriaBuilder, null,
						// criteriaBuilder.greaterThanOrEqualTo(createTimeR,
						// date1));
					}
					predicate = addAndPredicate(criteriaBuilder, predicate, criteriaBuilder.equal(statusR, status));
					// 只查询金额为正的
					predicate = addAndPredicate(criteriaBuilder, predicate,
							criteriaBuilder.greaterThan(amountR, new BigDecimal(0)));

				} else {
					// 未认领
					Date date2 = DateUtils.addDays(new Date(), -1);
					Date date1 = DateUtils.addDays(new Date(), -2);
					if (StringUtils.isNotBlank(startTime)) {
						date1 = CommonUtils.string2Date(startTime);
					}
					if (StringUtils.isNotBlank(endTime)) {
						date2 = CommonUtils.string2Date(endTime);
					}
					predicate = addAndPredicate(criteriaBuilder, null,
							criteriaBuilder.greaterThanOrEqualTo(createTimeR, date1));
					predicate = addAndPredicate(criteriaBuilder, predicate,
							criteriaBuilder.lessThanOrEqualTo(createTimeR, date2));
					Integer[] statsUnclaimed = { BankLogStatus.Matching.getStatus(),
							BankLogStatus.NoOwner.getStatus() };
					predicate = addAndPredicate(criteriaBuilder, predicate, statusR.in(statsUnclaimed));
				}
			}
			if (StringUtils.isNotBlank(payMan)) {
				predicate = addAndPredicate(criteriaBuilder, predicate,
						criteriaBuilder.like(toAccountOwnerR, "%" + payMan + "%"));
			}
			if (StringUtils.isNotBlank(member)) {
				predicate = addAndPredicate(criteriaBuilder, predicate,
						criteriaBuilder.like(toAccountOwnerR, "%" + member + "%"));
			}
			if (null != fromMoney) {
				predicate = addAndPredicate(criteriaBuilder, predicate,
						criteriaBuilder.greaterThanOrEqualTo(amountR, fromMoney));
			}
			if (null != toMoney) {
				predicate = addAndPredicate(criteriaBuilder, predicate,
						criteriaBuilder.lessThanOrEqualTo(amountR, toMoney));
			}
			if (accountId != null) {
				predicate = addAndPredicate(criteriaBuilder, predicate, criteriaBuilder.equal(fromAccountR, accountId));
			}
			if (accountIds != null && accountIds.length > 0) {
				if (accountIds.length > 1)
					predicate = addAndPredicate(criteriaBuilder, predicate, fromAccountR.in(accountIds));
				else
					predicate = addAndPredicate(criteriaBuilder, predicate,
							criteriaBuilder.equal(fromAccountR, accountIds[0]));
			}
			if (StringUtils.isNotBlank(thirdName)) {
				predicate = addAndPredicate(criteriaBuilder, predicate,
						criteriaBuilder.like(toAccountOwnerR, thirdName));
			}
			return predicate;
		};
	}

	// 获取金额总额 去除 String searType 只查银行流水
	public String getSumAmount(String payMan, Integer status, Integer[] accountIds, Integer accountId, String member,
			BigDecimal fromMoney, BigDecimal toMoney, String thirdName, String startTime, String endTime) {
		CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
		CriteriaQuery<BigDecimal> query = criteriaBuilder.createQuery(BigDecimal.class);
		Root<BizBankLog> root = query.from(BizBankLog.class);
		Predicate predicate = criteriaBuilder.conjunction();
		final Path<Integer> fromAccountR = root.get("fromAccount");
		final Path<Integer> statusR = root.get("status");
		final Path<Date> createTimeR = root.get("createTime");
		final Path<String> toAccountOwnerR = root.get("toAccountOwner");
		final Path<BigDecimal> amountR = root.get("amount");
		List<Expression<Boolean>> expressions = predicate.getExpressions();
		Expression<BigDecimal> sumTrxAmt = criteriaBuilder.sum(amountR);
		if (!status.equals(CONSTANT_FOR_BANKLOG_UNCLAIMED_STATUS) && StringUtils.isNotBlank(startTime)) {
			Date startTimeD = CommonUtils.string2Date(startTime);
			expressions.add(criteriaBuilder.greaterThanOrEqualTo(createTimeR, startTimeD));
		}
		if (!status.equals(CONSTANT_FOR_BANKLOG_UNCLAIMED_STATUS) && StringUtils.isNotBlank(endTime)) {
			Date endTimeD = CommonUtils.string2Date(endTime);
			expressions.add(criteriaBuilder.lessThanOrEqualTo(createTimeR, endTimeD));
		}
		expressionList(createTimeR, amountR, statusR, criteriaBuilder, expressions, startTime, endTime, status);
		if (StringUtils.isNotBlank(payMan)) {
			expressions.add(criteriaBuilder.like(toAccountOwnerR, "%" + payMan + "%"));
		}
		if (StringUtils.isNotBlank(member)) {
			expressions.add(criteriaBuilder.like(toAccountOwnerR, "%" + member + "%"));
		}
		if (null != fromMoney) {
			expressions.add(criteriaBuilder.greaterThanOrEqualTo(amountR, fromMoney));
		}
		if (null != toMoney) {

			expressions.add(criteriaBuilder.lessThanOrEqualTo(amountR, toMoney));
		}
		if (accountId != null) {
			expressions.add(criteriaBuilder.equal(fromAccountR, accountId));
		}
		if (accountIds != null && accountIds.length > 0) {
			if (accountIds.length > 1)
				expressions.add(fromAccountR.in(accountIds));
			else
				expressions.add(criteriaBuilder.equal(fromAccountR, accountIds[0]));
		}
		if (StringUtils.isNotBlank(thirdName)) {
			expressions.add(criteriaBuilder.like(toAccountOwnerR, thirdName));
		}

		query.where(predicate);
		query.select(sumTrxAmt);
		BigDecimal result = em.createQuery(query).getSingleResult();
		result = (null == result ? BigDecimal.ZERO : result);
		return result.toString();
	}

	private List<Expression<Boolean>> expressionList(final Path<Date> createTimeR, final Path<BigDecimal> amountR,
			final Path<Integer> statusR, CriteriaBuilder criteriaBuilder, List<Expression<Boolean>> expressions,
			String startTime, String endTime, Integer status) {
		if (status != null) {
			if (!status.equals(CONSTANT_FOR_BANKLOG_UNCLAIMED_STATUS)) {
				if (BankLogStatus.Matching.getStatus().equals(status)) {
					// Long unmatchTime1 = System.currentTimeMillis() -
					// 86400000;// 正在匹配的流水未超过24小时
					// Date date1 = new Date(unmatchTime1);
					// expressions.add(criteriaBuilder.greaterThanOrEqualTo(createTimeR,
					// date1));
				}
				// 只查询金额为正的
				expressions.add(criteriaBuilder.equal(statusR, status));
				expressions.add(criteriaBuilder.greaterThan(amountR, new BigDecimal(0)));

			} else {
				// 未认领
				Date date2 = DateUtils.addDays(new Date(), -1);
				Date date1 = DateUtils.addDays(new Date(), -2);
				if (StringUtils.isNotBlank(startTime)) {
					date1 = CommonUtils.string2Date(startTime);
				}
				if (StringUtils.isNotBlank(endTime)) {
					date2 = CommonUtils.string2Date(endTime);
				}
				expressions.add(criteriaBuilder.greaterThanOrEqualTo(createTimeR, date1));
				expressions.add(criteriaBuilder.lessThanOrEqualTo(createTimeR, date2));
				Integer[] statsUnclaimed = { BankLogStatus.Matching.getStatus(), BankLogStatus.NoOwner.getStatus() };
				expressions.add(statusR.in(statsUnclaimed));
			}
		}
		return expressions;
	}

	// 获取总记录数 银行流水
	public String getCount(String payMan, Integer status, Integer[] accountIds, Integer accountId, String member,
			BigDecimal fromMoney, BigDecimal toMoney, String thirdName, String startTime, String endTime) {
		CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
		CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
		Root<BizBankLog> root = query.from(BizBankLog.class);
		Predicate predicate = criteriaBuilder.conjunction();
		final Path<Integer> fromAccountR = root.get("fromAccount");
		final Path<Integer> statusR = root.get("status");
		final Path<Date> createTimeR = root.get("createTime");
		final Path<String> toAccountOwnerR = root.get("toAccountOwner");
		final Path<BigDecimal> amountR = root.get("amount");
		final Path<Long> idR = root.get("id");
		List<Expression<Boolean>> expressions = predicate.getExpressions();
		Expression<Long> count = criteriaBuilder.count(idR);
		if (!status.equals(CONSTANT_FOR_BANKLOG_UNCLAIMED_STATUS) && StringUtils.isNotBlank(startTime)) {
			Date startTimeD = CommonUtils.string2Date(startTime);
			expressions.add(criteriaBuilder.greaterThanOrEqualTo(createTimeR, startTimeD));
		}
		if (!status.equals(CONSTANT_FOR_BANKLOG_UNCLAIMED_STATUS) && StringUtils.isNotBlank(endTime)) {
			Date endTimeD = CommonUtils.string2Date(endTime);
			expressions.add(criteriaBuilder.lessThanOrEqualTo(createTimeR, endTimeD));
		}
		expressionList(createTimeR, amountR, statusR, criteriaBuilder, expressions, startTime, endTime, status);
		if (StringUtils.isNotBlank(payMan)) {
			expressions.add(criteriaBuilder.like(toAccountOwnerR, "%" + payMan + "%"));
		}
		if (StringUtils.isNotBlank(member)) {
			expressions.add(criteriaBuilder.like(toAccountOwnerR, "%" + member + "%"));
		}
		if (null != fromMoney) {
			expressions.add(criteriaBuilder.greaterThanOrEqualTo(amountR, fromMoney));
		}
		if (null != toMoney) {

			expressions.add(criteriaBuilder.lessThanOrEqualTo(amountR, toMoney));
		}
		if (accountId != null) {
			expressions.add(criteriaBuilder.equal(fromAccountR, accountId));
		}
		if (accountIds != null && accountIds.length > 0) {
			if (accountIds.length > 1)
				expressions.add(fromAccountR.in(accountIds));
			else
				expressions.add(criteriaBuilder.equal(fromAccountR, accountIds[0]));
		}
		if (StringUtils.isNotBlank(thirdName)) {
			expressions.add(criteriaBuilder.like(toAccountOwnerR, thirdName));
		}

		query.where(predicate);
		query.select(count);
		Long result = em.createQuery(query).getSingleResult();
		result = (null == result ? 0L : result);
		return result.toString();
	}

	private Predicate addAndPredicate(final CriteriaBuilder criteriaBuilder, final Predicate oldPredicate,
			final Predicate newPredicate) {
		return oldPredicate != null ? criteriaBuilder.and(oldPredicate, newPredicate) : newPredicate;
	}

	@Override
	public Page<BizBankLog> findAll(Pageable pageable) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Page<BizBankLog> findAll(String operaor, Specification<BizBankLog> specification, Integer accountId,
			String fieldval, Pageable pageable) throws Exception {
		Page<BizBankLog> dataToList = bankLogRepository.findAll(specification, pageable);
		for (BizBankLog bankLog : dataToList) {
			bindExtForBankLog(bankLog, accountId, fieldval);
		}
		return dataToList;
	}

	@Override
	public BizBankLog get(Long id) {
		BizBankLog bankLogVO = bankLogRepository.findOne(id);
		try {
			bindExtForBankLog(bankLogVO, -1, "-1");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return bankLogVO;
	}

	/**
	 * 入款请求ID获取流水信息，该入款请求必须处于匹配状态
	 */
	@Override
	public BizBankLog findByIncomeReqId(Long id) {
		String SQL_findByIncomeReqestId = "select bl.id,bl.from_account fromAccount,bl.trading_time tradingTime,bl.amount,bl.status,bl.remark,bl.to_account toAccount,bl.to_account_owner toAccountOwner,bl.balance from biz_bank_log bl,biz_transaction_log tl,biz_income_request ir where bl.id=tl.banklog_id and tl.type!=0 and tl.order_id = ir.id  and ir.id=%d";
		Query outwardQative = entityManager.createNativeQuery(String.format(SQL_findByIncomeReqestId, id));
		Object[] valArray = (Object[]) outwardQative.getSingleResult();
		if (valArray == null) {
			return null;
		}
		BizBankLog result = new BizBankLog();
		result.setId((Long.valueOf((Integer) valArray[0])));
		result.setFromAccount((Integer) valArray[1]);
		result.setTradingTime((Date) valArray[2]);
		result.setAmount((BigDecimal) valArray[3]);
		result.setStatus((Integer) valArray[4]);
		result.setRemark((String) valArray[5]);
		result.setToAccount((String) valArray[6]);
		result.setToAccountOwner((String) valArray[7]);
		result.setBalance((BigDecimal) valArray[8]);
		try {
			bindExtForBankLog(result, -1, "-1");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public Page<Object> bankLogList(Pageable pageable, BizBankLog bankLog, BigDecimal minAmount, BigDecimal maxAmount,
			Date startTime, Date endTime) {
		if (StringUtils.isEmpty(bankLog.getToAccount())) {
			// 传字符串时，会查不出toAccount空的结果集
			bankLog.setToAccount(null);
		}
		if (StringUtils.isEmpty(bankLog.getToAccountOwner())) {
			// 传字符串时，会查不出toAccount空的结果集
			bankLog.setToAccountOwner(null);
		}
		if (bankLog.getStatus() != null && bankLog.getStatus().equals(0)) {
			// 匹配中
			return bankLogRepository.bankLogListStatus0(pageable, bankLog.getToAccount(), bankLog.getToAccountOwner(),
					bankLog.getFromAccount(), minAmount, maxAmount, startTime, endTime);
		} else if (bankLog.getStatus() != null && bankLog.getStatus().equals(3)) {
			// 未认领
			return bankLogRepository.bankLogListStatus3(pageable, bankLog.getToAccount(), bankLog.getToAccountOwner(),
					bankLog.getFromAccount(), minAmount, maxAmount, startTime, endTime);
		} else {
			// 未指定状态或者其它状态
			return bankLogRepository.bankLogList(pageable, bankLog.getToAccount(), bankLog.getToAccountOwner(),
					bankLog.getFromAccount(), minAmount, maxAmount, startTime, endTime, bankLog.getStatus());
		}
	}

	@Override
	public Page<Object> noOwner4Income(Pageable pageable, BizBankLog bankLog, BigDecimal minAmount,
			BigDecimal maxAmount, Date startTime, Date endTime, List<Integer> handicapIdToList) {
		// 传字符串时，会查不出空的结果集
		if (StringUtils.isEmpty(bankLog.getToAccount())) {
			bankLog.setToAccount(null);
		}
		if (StringUtils.isEmpty(bankLog.getToAccountOwner())) {
			bankLog.setToAccountOwner(null);
		}
		if (StringUtils.isEmpty(bankLog.getFromAccountNO())) {
			bankLog.setFromAccountNO(null);
		}
		if (StringUtils.isEmpty(bankLog.getRemark())) {
			bankLog.setRemark(null);
		}
		return bankLogRepository.bankLogListStatus4(pageable, bankLog.getToAccount(), bankLog.getToAccountOwner(),
				bankLog.getFromAccountNO(), bankLog.getRemark(), minAmount, maxAmount, startTime, endTime,
				handicapIdToList);
	}

	/**
	 * 查询银行流水总金额
	 */
	@Override
	public List<Object> bankLogList_sumAmount(Pageable pageable, BizBankLog bankLog, BigDecimal minAmount,
			BigDecimal maxAmount, Date startTime, Date endTime) {
		if (StringUtils.isEmpty(bankLog.getToAccount())) {
			// 传字符串时，会查不出toAccount空的结果集
			bankLog.setToAccount(null);
		}
		if (StringUtils.isEmpty(bankLog.getToAccountOwner())) {
			// 传字符串时，会查不出toAccount空的结果集
			bankLog.setToAccountOwner(null);
		}
		if (bankLog.getStatus() != null && bankLog.getStatus().equals(0)) {
			// 匹配中
			return bankLogRepository.bankLogList_sumAmountStatus0(bankLog.getToAccount(), bankLog.getToAccountOwner(),
					bankLog.getFromAccount(), minAmount, maxAmount, startTime, endTime);
		} else if (bankLog.getStatus() != null && bankLog.getStatus().equals(3)) {
			// 未认领
			return bankLogRepository.bankLogList_sumAmountStatus3(bankLog.getToAccount(), bankLog.getToAccountOwner(),
					bankLog.getFromAccount(), minAmount, maxAmount, startTime, endTime);
		} else {
			// 未指定状态或者其它状态
			return bankLogRepository.bankLogList_sumAmount(bankLog.getToAccount(), bankLog.getToAccountOwner(),
					bankLog.getFromAccount(), minAmount, maxAmount, startTime, endTime, bankLog.getStatus());
		}
	}

	@Override
	public BigDecimal bankLogList_sumAmount4(Pageable pageable, BizBankLog bankLog, BigDecimal minAmount,
			BigDecimal maxAmount, Date startTime, Date endTime, List<Integer> handicapIdToList) {
		return bankLogRepository.bankLogList_sumAmount4(bankLog.getToAccount(), bankLog.getToAccountOwner(),
				bankLog.getFromAccountNO(), bankLog.getRemark(), minAmount, maxAmount, startTime, endTime,
				handicapIdToList);
	}

	@Transactional
	@Override
	public BizBankLog save(BizBankLog entity) {
		return bankLogRepository.saveAndFlush(entity);
	}

	@Transactional
	@Override
	public List<BizBankLog> save(List<BizBankLog> entities) {
		return bankLogRepository.save(entities);
	}

	@Transactional
	@Override
	public BizBankLog update(BizBankLog entity) {
		return bankLogRepository.saveAndFlush(entity);
	}

	@Override
	@Transactional
	public void delete(Long id) {
		bankLogRepository.delete(id);
	}

	@Transactional
	@Override
	public void updateBankLog(Long id, Integer status) {
		bankLogRepository.updateBankLog(id, status);
	}

	private BizBankLog bindExtForBankLog(BizBankLog bankLog, Integer accountId, String fieldval) throws Exception {
		if (bankLog == null) {
			return null;
		}
		AccountBaseInfo from = accountService.getFromCacheById(bankLog.getFromAccount());
		// BizAccount from = accountService.getById(bankLog.getFromAccount());
		BizHandicap bizHandicap = handicapService.findFromCacheById(from.getHandicapId());
		// 查询不为空、金额大于0（第三方下发数据）、且是备用卡。
		if (accountId.equals(0) && !fieldval.equals("0")) {
			if ((bankLog.getAmount().compareTo(new BigDecimal("0")) > 0)
					&& (from.getType().equals(12) || from.getType().equals(13))) {
				BizTransactionLog ransactionLog = transactionLogRepository.findByToBanklogId(bankLog.getId());
				AccountBaseInfo bizAccountFrom = accountService
						.getFromCacheById(ransactionLog == null ? 0 : ransactionLog.getFromAccount());
				BizHandicap handicap = handicapService
						.findFromCacheById(bizAccountFrom == null ? 0 : bizAccountFrom.getHandicapId());
				bankLog.setTransFerBankName(bizAccountFrom == null ? "" : bizAccountFrom.getBankName());
				bankLog.setTransFerHandicap(handicap == null ? "" : handicap.getName());
			}
		}
		// 查询下发到哪个盘口、以及卡的类型
		if (accountId.equals(0)) {
			if ((bankLog.getAmount().compareTo(new BigDecimal("0")) < 0)
					&& (from.getType().equals(12) || from.getType().equals(13))) {
				BizTransactionLog ransactionLog = transactionLogRepository.findByFromBanklogId(bankLog.getId());
				AccountBaseInfo bizAccountFrom = accountService
						.getFromCacheById(ransactionLog == null ? 0 : ransactionLog.getToAccount());
				BizHandicap handicap = handicapService
						.findFromCacheById(bizAccountFrom == null ? 0 : bizAccountFrom.getHandicapId());
				bankLog.setFromAccountTypeName(null == bizAccountFrom.getType() ? ""
						: AccountType.findByTypeId(bizAccountFrom.getType()).getMsg());
				bankLog.setTransFerHandicap(handicap == null ? "" : handicap.getName());
			}
		}
		if (!Objects.isNull(from)) {
			bankLog.setFromAccountNO(from.getAccount());
			bankLog.setFromAlias(from.getAlias());
			bankLog.setFromOwner(from.getOwner());
			bankLog.setFromBankType(from.getBankType());
			bankLog.setHandicapName(bizHandicap == null ? "" : bizHandicap.getName());
		}
		if (bankLog.getTradingTime() != null) {
			bankLog.setTradingTimeStr(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(bankLog.getTradingTime()));
		}
		return bankLog;
	}

	@Transactional
	@Override
	public List<BizBankLog> findAll(Specification<BizBankLog> specification) {
		return bankLogRepository.findAll(specification);
	}

	/**
	 * 根据账号id,当前时间与交易时间差查询匹配中金额大于零的数据的金额总和
	 */
	@Override
	public String querySumAmountCondition(Integer fromId, int hours) {
		return bankLogRepository.querySumAmountCondition(fromId, hours);
	}

	/**
	 * 查询所有转入的金额
	 */
	@Override
	public String queryIncomeTotal(Integer fromId) {
		return bankLogRepository.queryIncomeTotal(fromId);
	}

	/**
	 * 查询所有转出的金额
	 */
	@Override
	public String queryOutTotal(Integer fromId) {
		return bankLogRepository.queryOutTotal(fromId);
	}

	/**
	 * 流水总计
	 */
	@Override
	public BigDecimal[] findAmountTotal(SearchFilter[] filterToArray) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Tuple> query = cb.createTupleQuery();
		Root<BizBankLog> root = query.from(BizBankLog.class);
		Predicate[] predicateArray = DynamicPredicate.build(cb, query, root, BizBankLog.class, filterToArray);
		query.multiselect(cb.sum(root.<BigDecimal>get("amount")));
		query.where(predicateArray);
		Object[] objArray = entityManager.createQuery(query).getSingleResult().toArray();
		return new BigDecimal[] { (BigDecimal) objArray[0] };
	}

	/**
	 * 获取正在匹配的流水的统计数据
	 *
	 * @param accountIdArray
	 *            账号ID集合
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<FlowStatMatching> findFlowStat4Matching(Integer[] accountIdArray) {
		List<FlowStatMatching> result = new ArrayList<>();
		if (Objects.isNull(accountIdArray) || accountIdArray.length == 0) {
			return result;
		}
		// SQL WHERE 条件 in 值组装 idsIn
		List<String> idList = new ArrayList<>();
		Arrays.stream(accountIdArray).forEach(p -> idList.add(p.toString()));
		String idsIn = String.join(",", idList);
		// SQL WHERE 条件 日期值，最近1周 latestOneWeek
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, -7);
		String latestOneWeek = CommonUtils.getDateFormat2Str(cal.getTime());
		// SQL 组装
		String sqlFmt = "select from_account accountId,'+' type,count(1)num,sum(amount)amount from biz_bank_log where from_account in (%s) and trading_time>='%s' and status=0 and amount>0 group by from_account union select from_account accountId,'-' type,count(1)num,sum(amount)amount from biz_bank_log where from_account in (%s) and trading_time>='%s' and status=0 and amount<0 group by from_account";
		String sql = String.format(sqlFmt, idsIn, latestOneWeek, idsIn, latestOneWeek);
		log.debug("获取正在匹配的流水的统计数据 =>sql:{}", sql);
		// 数据获取
		List<Object> dataList = entityManager.createNativeQuery(sql).getResultList();
		Map<Integer, FlowStatMatching> flowMap = new HashMap<>();
		dataList.forEach(p -> {
			Object[] countArr = (Object[]) p;
			int id = (Integer) countArr[0];
			String type = (String) countArr[1];
			int num = ((BigInteger) countArr[2]).intValue();
			BigDecimal amount = ((BigDecimal) countArr[3]).abs();
			FlowStatMatching flow = flowMap.get(id);
			if (flow == null) {
				flow = new FlowStatMatching();
				flowMap.put(id, flow);
			}
			flow.setAccountId(id);
			if (StringUtils.equals("+", type)) {
				flow.setInAmount(amount);
				flow.setInNum(num);
			} else {
				flow.setOutAmount(amount);
				flow.setOutNum(num);
			}
		});
		flowMap.forEach((k, v) -> result.add(v));
		result.forEach(p -> {
			AccountBaseInfo base = accountService.getFromCacheById(p.getAccountId());
			if (Objects.nonNull(base)) {
				p.setAccInfo(base.getAlias() + "|" + base.getOwner() + "|" + base.getBankType());
			}
		});
		return result;
	}

	@Override
	public List<Object> findSenderCard(String startTime, String endTime, List<Integer> handicaps) {
		return bankLogRepository.findSenderCard(startTime, endTime, handicaps);
	}

	@Override
	public BizBankLog findRebateLimitBankLog(int accountId, BigDecimal amount, String startTime, String endTime) {
		return bankLogRepository.findRebateLimitBankLog(accountId, amount, startTime, endTime);
	}

	@Override
	public List<BizBankLog> finBanks(Date tradingStart, Date tradingEnd, int fromAccountId, int status,
			BigDecimal amount, String owner) {
		return bankLogRepository.finBanks(tradingStart, tradingEnd, fromAccountId, status, amount, owner);
	}

	@Transactional
	@Override
	public void updateCsById(Long id, BigDecimal commission) {
		bankLogRepository.updateCsById(id, commission);
	}

	@Transactional
	@Override
	public void updateStatusRm(Long id, Integer status, String remark) {
		bankLogRepository.updateStatusRm(id, status, remark);
	}

	@Override
	public int finCounts(int fromId, BigDecimal amount, String startTime, String endTime) {
		return bankLogRepository.finCounts(fromId, amount, startTime, endTime);
	}

	@Override
	public int getDateTotalBankLog(int accountId) {
		return bankLogRepository.getDateTotalBankLog(accountId);
	}

	@Transactional
	@Override
	public void updateBalanceByid(Long id, BigDecimal balance) {
		bankLogRepository.updateBalanceByid(id, balance);
	}
}
