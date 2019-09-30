package com.xinbo.fundstransfer.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.net.http.RequestBodyParser;
import com.xinbo.fundstransfer.component.net.http.v2.HttpClientNew;
import com.xinbo.fundstransfer.daifucomponent.dto.output.DaifuResult;
import com.xinbo.fundstransfer.daifucomponent.service.Daifu4OutwardService;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.*;
import com.xinbo.fundstransfer.domain.pojo.Deposit;
import com.xinbo.fundstransfer.domain.pojo.WithdrawAuditInfo;
import com.xinbo.fundstransfer.domain.repository.OutwardRequestRepository;
import com.xinbo.fundstransfer.domain.repository.OutwardTaskRepository;
import com.xinbo.fundstransfer.domain.repository.QueryNoCountDao;
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
import rx.Observable;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class OutwardRequestServiceImpl implements OutwardRequestService {
	private static final Logger log = LoggerFactory.getLogger(OutwardRequestServiceImpl.class);
	@Autowired
	private OutwardTaskRepository outwardTaskRepository;
	@Autowired
	private OutwardRequestRepository outwardRequestRepository;
	@Autowired
	private RequestBodyParser requestBodyParser;
	@Autowired
	@Lazy
	private OutwardRequestService outwardRequestService;
	@Autowired
	private SysDataPermissionService sysDataPermissionService;
	@Autowired
	private HandicapService handicapService;
	@Autowired
	private LevelService levelService;
	@Autowired
	private SysUserService sysUserService;
	@PersistenceContext
	private EntityManager entityManager;
	@Autowired
	private QueryNoCountDao queryNoCountDao;
	@Autowired
	@Lazy
	private AllocateOutwardTaskService outwardTaskAllocateService;
	@Autowired
	private SysUserProfileService sysUserProfileService;
	@Autowired
	private DaifuTaskService daifuTaskService;
	@Autowired
	private Daifu4OutwardService daifu4OutwardService;
	private ObjectMapper mapper = new ObjectMapper();

	/**
	 * 根据创建时间和是否有会员名查找公司用款订单
	 */
	@Override
	public BizOutwardRequest findByCreateTimeAndMember(Date createTime) {
		return outwardRequestRepository.findByCreateTimeEqualsAndMemberIsNull(createTime);
	}

	/**
	 * 查找通知平台失败的且更新时间创建时间是48小时之内的订单再次发起通知
	 */
	@Override
	public List<BizOutwardRequest> findOrdersForNotify() {
		return outwardRequestRepository.findByStatusEquals(OutwardRequestStatus.Failure.getStatus());
	}

	@Override
	public BizOutwardRequest findByOrderNo(String orderNo) {
		return outwardRequestRepository.findByOrderNo(orderNo);
	}

	@Override
	public List<Object[]> statisticsCompanyExpenditure(List<Integer> handicap, Date startTime, Date endTime) {
		return outwardRequestRepository.statisticsCompanyExpenditure(handicap, startTime, endTime);
	}

	@Override
	public BigDecimal sumCompanyExpend(Integer handicap, Integer operator, BigDecimal amountStart, BigDecimal amountEnd,
			List<Integer> outAccountId, String purpose, Date startTime, Date endTime) {
		if (outAccountId != null && outAccountId.size() > 0) {
			return outwardRequestRepository.sumCompanyExpendAllWithAccount(handicap, operator, amountStart, amountEnd,
					outAccountId, purpose, startTime, endTime);
		} else {
			return outwardRequestRepository.sumCompanyExpendAll(handicap, operator, amountStart, amountEnd, purpose,
					startTime, endTime);
		}
	}

	@Override
	public Long countCompanyExpend(Integer handicap, Integer operator, BigDecimal amountStart, BigDecimal amountEnd,
			List<Integer> outAccountId, String purpose, Date startTime, Date endTime) {
		if (outAccountId != null && outAccountId.size() > 0) {
			return outwardRequestRepository.countCompanyExpendAllWithAccount(handicap, operator, amountStart, amountEnd,
					outAccountId, purpose, startTime, endTime);
		} else {
			return outwardRequestRepository.countCompanyExpendAll(handicap, operator, amountStart, amountEnd, purpose,
					startTime, endTime);
		}
	}

	@Override
	public List<Object[]> queryCompanyExpend(Pageable pageable, Integer handicap, Integer operator,
			BigDecimal amountStart, BigDecimal amountEnd, List<Integer> outAccountId, String purpose, Date startTime,
			Date endTime) {
		if (outAccountId != null && outAccountId.size() > 0) {
			return outwardRequestRepository.queryCompanyExpendALLWithAccount(pageable, handicap, operator, amountStart,
					amountEnd, outAccountId, purpose, startTime, endTime);
		} else {
			return outwardRequestRepository.queryCompanyExpendALL(pageable, handicap, operator, amountStart, amountEnd,
					purpose, startTime, endTime);
		}
	}

	@Override
	public List<BizOutwardRequest> findAllByStatus(Integer status) {
		return outwardRequestRepository.findAllByStatus(status);
	}

	@Override
	public Page<Object[]> quickQuery(String member, String orderNo, String startTime, String endTime, Integer handicap,
			Pageable pageable) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			return outwardRequestRepository.quickQuery(pageable, member, orderNo,
					startTime == null ? null : sdf.parse(startTime), endTime == null ? null : sdf.parse(endTime),
					handicap);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public List<Object[]> quickQueryForOut(String startTime, String endTime, String member, String orderNo,
			List<Integer> handicapList) {
		return outwardRequestRepository.quickQueryForOut(member, orderNo, handicapList,
				CommonUtils.string2Date(startTime), CommonUtils.string2Date(endTime));
	}

	@Override
	public Long quickQueryCountForOut(String member, String orderNo) {
		return outwardRequestRepository.quickQueryCountForOut(member, orderNo);
	}

	@Override
	public BigDecimal[] quickQuerySumForOut(String member, String orderNo) {
		BigDecimal[] amount = new BigDecimal[2];

		BigDecimal reqAmount = outwardRequestRepository.quickQueryReqSumForOut(member, orderNo);
		BigDecimal taskAmount = outwardRequestRepository.quickQueryTaskSumForOut(member, orderNo);
		if (reqAmount != null) {
			amount[0] = reqAmount;
		} else {
			amount[0] = new BigDecimal(0);
		}
		if (taskAmount != null) {
			amount[1] = taskAmount;
		} else {
			amount[1] = new BigDecimal(0);
		}
		return amount;
	}

	@Override
	public BizOutwardRequest get(Long id) {
		return outwardRequestRepository.findOne(id);
	}

	@Override
	public BizOutwardRequest findByHandicapAndOrderNo(int handicap, String orderNo) {
		return outwardRequestRepository.findByHandicapAndOrderNo(handicap, orderNo);
	}

	@Override
	@Transactional(rollbackFor=Exception.class)
	public BizOutwardRequest save(BizOutwardRequest entity) {
		return outwardRequestRepository.saveAndFlush(entity);
	}

	@Override
	@Transactional(rollbackFor=Exception.class)
	public BizOutwardRequest update(BizOutwardRequest entity) {
		return outwardRequestRepository.saveAndFlush(entity);
	}

	@Override
	public int addCompanyExpend(String orderNo, Integer handicap, BigDecimal amount, String remark, Integer reviewer,
			String toAccount, String toAccountOwner, String toAccountBank, String review) {
		return outwardRequestRepository.addCompanyExpend(orderNo, handicap, amount, remark, reviewer, toAccount,
				toAccountOwner, toAccountBank, review);
	}

	@Override
	public int updateForCompanyExpend(Long reqId, String remarks, Integer userId) {
		return outwardRequestRepository.updateForCompanyExpend(reqId, remarks, userId);
	}

	@Transactional
	@Override
	public int updateById(Long reqId, Integer userId) {
		return outwardRequestRepository.updateById(reqId, userId);
	}

	/**
	 * 主管审核通过，根据指定reqId和状态为3(主管处理)且不是1(审核通过)的记录
	 */
	@Override
	public int updateByIdAndStatus(Long reqId, Integer userId) {
		return outwardRequestRepository.updateByIdAndStatus(reqId, userId);
	}

	@Transactional
	@Override
	public void delete(Long id) {
		outwardRequestRepository.delete(id);
	}

	/**
	 * 出款审核页签 出款审核汇总 不带总记录数
	 */
	@Override
	public Page<BizOutwardRequest> findOutwardRequestPageNoCount(List<Integer> handicapList, Integer levelId,
			Integer[] status, String member, String orderNo, Integer[] reviwer, String reviewerType, String startTime,
			String endTime, BigDecimal fromMoney, BigDecimal tomoney, PageRequest pageRequest) {
		Specification<BizOutwardRequest> specification = wrapSpecification(handicapList, levelId, status, member,
				orderNo, reviwer, reviewerType, startTime, endTime, fromMoney, tomoney);
		return queryNoCountDao.findAll(specification, pageRequest, BizOutwardRequest.class);
	}

	/**
	 * 出款审核页签 出款审核汇总 总记录数
	 */
	@Override
	public Long getOutwardRequestCount(List<Integer> handicapList, Integer levelId, Integer[] status, String member,
			String orderNo, Integer reviwer[], String reviewerType, String startTime, String endTime,
			BigDecimal fmonery, BigDecimal tomoney) {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
		Root<BizOutwardRequest> root = query.from(BizOutwardRequest.class);
		Predicate predicate = criteriaBuilder.conjunction();
		final Path<Integer> handicapR = root.get("handicap");
		final Path<String> orderNoR = root.get("orderNo");
		final Path<Integer> levelR = root.get("level");
		final Path<Integer> statusR = root.get("status");
		final Path<String> memberR = root.get("member");
		final Path<Integer> reviewerR = root.get("reviewer");
		final Path<Date> createTimeR = root.get("createTime");
		final Path<Date> updateTimeR = root.get("updateTime");
		final Path<BigDecimal> amountR = root.get("amount");
		List<Expression<Boolean>> expressions = predicate.getExpressions();
		final Path<Long> idR = root.get("id");
		Expression<Long> count = criteriaBuilder.count(idR);
		// 3 0 2 主管处理 待审核 已拒绝 只查询 member!=null 即平台来的单子
		if (status.length == 1 && (status[0].equals(OutwardRequestStatus.Processing.getStatus())
				|| status[0].equals(OutwardRequestStatus.ManagerProcessing.getStatus())
				|| status[0].equals(OutwardRequestStatus.Reject.getStatus()))) {
			expressions.add(memberR.isNotNull());
		}
		if ((status.length == 1 && (status[0].equals(OutwardRequestStatus.Processing.getStatus())
				|| status[0].equals(OutwardRequestStatus.ManagerProcessing.getStatus()))) || status.length == 3) {
			if (StringUtils.isNotBlank(startTime)) {
				Date startTime1 = CommonUtils.string2Date(startTime);
				expressions.add(criteriaBuilder.greaterThanOrEqualTo(createTimeR, startTime1));
			}
			if (StringUtils.isNotBlank(endTime)) {
				Date endTime1 = CommonUtils.string2Date(endTime);
				expressions.add(criteriaBuilder.lessThanOrEqualTo(createTimeR, endTime1));

			}
		} else {
			if (StringUtils.isNotBlank(startTime)) {
				Date startTime1 = CommonUtils.string2Date(startTime);
				expressions.add(criteriaBuilder.greaterThanOrEqualTo(updateTimeR, startTime1));
			}
			if (StringUtils.isNotBlank(endTime)) {
				Date endTime1 = CommonUtils.string2Date(endTime);
				expressions.add(criteriaBuilder.lessThanOrEqualTo(updateTimeR, endTime1));

			}
		}
		if (status != null && status.length > 0) {
			if (status.length == 1) {
				expressions.add(criteriaBuilder.equal(statusR, status[0]));
			} else {
				expressions.add(statusR.in(status));
			}
		}
		if (handicapList != null && handicapList.size() > 0) {
			if (handicapList.size() == 1)
				expressions.add(criteriaBuilder.equal(handicapR, handicapList.get(0)));
			else
				expressions.add(handicapR.in(handicapList));
		}
		if (levelId != null) {
			expressions.add(criteriaBuilder.equal(levelR, levelId));
		}
		if (StringUtils.isNotBlank(member)) {
			expressions.add(criteriaBuilder.equal(memberR, StringUtils.trim(member)));
		}
		if (StringUtils.isNotBlank(orderNo)) {
			expressions.add(criteriaBuilder.equal(orderNoR, StringUtils.trim(orderNo)));
		}
		if (reviwer != null && reviwer.length > 0) {
			if (reviwer.length == 1)
				expressions.add(criteriaBuilder.equal(reviewerR, reviwer[0]));
			else
				expressions.add(reviewerR.in(reviwer));
		}
		if (StringUtils.isNotBlank(reviewerType)) {
			if ("robot".equals(reviewerType)) {
				predicate = addAndPredicate(criteriaBuilder, predicate, reviewerR.isNull());
			}
			if ("manual".equals(reviewerType)) {
				predicate = addAndPredicate(criteriaBuilder, predicate, reviewerR.isNotNull());
			}
		}

		if (fmonery != null) {
			expressions.add(criteriaBuilder.greaterThanOrEqualTo(amountR, fmonery));
		}
		if (tomoney != null) {
			expressions.add(criteriaBuilder.lessThanOrEqualTo(amountR, tomoney));
		}
		query.where(predicate);
		query.select(count);
		Long result = entityManager.createQuery(query).getSingleResult();
		result = (null == result ? 0L : result);
		return result;
	}

	/**
	 * 出款审核页签 出款审核汇总 总金额
	 */
	@Override
	public String getOutwardRequestSumAmount(List<Integer> handicapList, Integer levelId, Integer[] status,
			String member, String orderNo, Integer reviwer[], String reviewerType, String startTime, String endTime,
			BigDecimal fmonery, BigDecimal tomoney) {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<BigDecimal> query = criteriaBuilder.createQuery(BigDecimal.class);
		Root<BizOutwardRequest> root = query.from(BizOutwardRequest.class);
		Predicate predicate = criteriaBuilder.conjunction();
		final Path<Integer> handicapR = root.get("handicap");
		final Path<String> orderNoR = root.get("orderNo");
		final Path<Integer> levelR = root.get("level");
		final Path<Integer> statusR = root.get("status");
		final Path<String> memberR = root.get("member");
		final Path<Integer> reviewerR = root.get("reviewer");
		final Path<Date> createTimeR = root.get("createTime");
		final Path<Date> updateTimeR = root.get("updateTime");
		final Path<BigDecimal> amountR = root.get("amount");
		List<Expression<Boolean>> expressions = predicate.getExpressions();
		Expression<BigDecimal> sumTrxAmt = criteriaBuilder.sum(amountR);
		// 3 0 2 主管处理 待审核 已拒绝 只查询 member!=null 即平台来的单子
		if (status.length == 1 && (status[0].equals(OutwardRequestStatus.Processing.getStatus())
				|| status[0].equals(OutwardRequestStatus.ManagerProcessing.getStatus())
				|| status[0].equals(OutwardRequestStatus.Reject.getStatus()))) {
			expressions.add(memberR.isNotNull());
		}
		if ((status.length == 1 && (status[0].equals(OutwardRequestStatus.Processing.getStatus())
				|| status[0].equals(OutwardRequestStatus.ManagerProcessing.getStatus()))) || status.length == 3) {
			if (StringUtils.isNotBlank(startTime)) {
				Date startTime1 = CommonUtils.string2Date(startTime);
				expressions.add(criteriaBuilder.greaterThanOrEqualTo(createTimeR, startTime1));
			}
			if (StringUtils.isNotBlank(endTime)) {
				Date endTime1 = CommonUtils.string2Date(endTime);
				expressions.add(criteriaBuilder.lessThanOrEqualTo(createTimeR, endTime1));

			}
		} else {
			if (StringUtils.isNotBlank(startTime)) {
				Date startTime1 = CommonUtils.string2Date(startTime);
				expressions.add(criteriaBuilder.greaterThanOrEqualTo(updateTimeR, startTime1));
			}
			if (StringUtils.isNotBlank(endTime)) {
				Date endTime1 = CommonUtils.string2Date(endTime);
				expressions.add(criteriaBuilder.lessThanOrEqualTo(updateTimeR, endTime1));

			}
		}
		if (status != null && status.length > 0) {
			if (status.length == 1) {
				expressions.add(criteriaBuilder.equal(statusR, status[0]));
			} else {
				expressions.add(statusR.in(status));
			}
		}
		if (handicapList != null && handicapList.size() > 0) {
			if (handicapList.size() == 1)
				expressions.add(criteriaBuilder.equal(handicapR, handicapList.get(0)));
			else
				expressions.add(handicapR.in(handicapList));
		}
		if (levelId != null) {
			expressions.add(criteriaBuilder.equal(levelR, levelId));
		}
		if (StringUtils.isNotBlank(member)) {
			expressions.add(criteriaBuilder.equal(memberR, StringUtils.trim(member)));
		}
		if (StringUtils.isNotBlank(orderNo)) {
			expressions.add(criteriaBuilder.equal(orderNoR, StringUtils.trim(orderNo)));
		}
		if (reviwer != null && reviwer.length > 0) {
			if (reviwer.length == 1)
				expressions.add(criteriaBuilder.equal(reviewerR, reviwer[0]));
			else
				expressions.add(reviewerR.in(reviwer));
		}
		if (StringUtils.isNotBlank(reviewerType)) {
			if ("robot".equals(reviewerType)) {
				predicate = addAndPredicate(criteriaBuilder, predicate, reviewerR.isNull());
			}
			if ("manual".equals(reviewerType)) {
				predicate = addAndPredicate(criteriaBuilder, predicate, reviewerR.isNotNull());
			}
		}

		if (fmonery != null) {
			expressions.add(criteriaBuilder.greaterThanOrEqualTo(amountR, fmonery));
		}
		if (tomoney != null) {
			expressions.add(criteriaBuilder.lessThanOrEqualTo(amountR, tomoney));
		}
		query.where(predicate);
		query.select(sumTrxAmt);
		BigDecimal result = entityManager.createQuery(query).getSingleResult();
		result = (null == result ? BigDecimal.ZERO : result);
		return result.toString();
	}

	/**
	 * 出款审核 出款审核汇总页签 封装条件(查询记录 查询总记录数调用)
	 */
	private Specification<BizOutwardRequest> wrapSpecification(List<Integer> handicapList, Integer levelId,
			Integer[] status, String member, String orderNo, Integer[] reviwer, String reviewerType, String startTime,
			String endTime, BigDecimal fmonery, BigDecimal tomoney) {
		Specification<BizOutwardRequest> specification = (root, criteriaQuery, criteriaBuilder) -> {
			Predicate predicate = null;
			final Path<Integer> handicapR = root.get("handicap");
			final Path<String> orderNoR = root.get("orderNo");
			final Path<Integer> levelR = root.get("level");
			final Path<Integer> statusR = root.get("status");
			final Path<String> memberR = root.get("member");
			final Path<Integer> reviewerR = root.get("reviewer");
			final Path<Date> createTimeR = root.get("createTime");
			final Path<Date> updateTimeR = root.get("updateTime");
			final Path<BigDecimal> amount = root.get("amount");
			// 3 0 2 主管处理 待审核 已拒绝 只查询 member!=null 即平台来的单子
			if (status.length == 1 && (status[0].equals(OutwardRequestStatus.Processing.getStatus())
					|| status[0].equals(OutwardRequestStatus.ManagerProcessing.getStatus())
					|| status[0].equals(OutwardRequestStatus.Reject.getStatus()))) {
				predicate = addAndPredicate(criteriaBuilder, predicate, memberR.isNotNull());
			}
			// 待审核 主管处理 已处理 根据createTime 查询
			if ((status.length == 1 && (status[0].equals(OutwardRequestStatus.Processing.getStatus())
					|| status[0].equals(OutwardRequestStatus.ManagerProcessing.getStatus()))) || status.length == 3) {
				predicate = wrapTimeCondition(createTimeR, startTime, endTime, criteriaBuilder, predicate);
			} else {
				predicate = wrapTimeCondition(updateTimeR, startTime, endTime, criteriaBuilder, predicate);
			}
			if (status != null && status.length > 0) {
				if (status.length == 1)
					predicate = addAndPredicate(criteriaBuilder, predicate, criteriaBuilder.equal(statusR, status[0]));
				else
					predicate = addAndPredicate(criteriaBuilder, predicate, statusR.in(status));
			}
			if (handicapList != null && handicapList.size() > 0) {
				if (handicapList.size() == 1)
					predicate = addAndPredicate(criteriaBuilder, predicate,
							criteriaBuilder.equal(handicapR, handicapList.get(0)));
				else
					predicate = addAndPredicate(criteriaBuilder, predicate, handicapR.in(handicapList));

			}
			if (levelId != null) {
				predicate = addAndPredicate(criteriaBuilder, predicate, criteriaBuilder.equal(levelR, levelId));
			}
			if (StringUtils.isNotBlank(member)) {
				predicate = addAndPredicate(criteriaBuilder, predicate,
						criteriaBuilder.equal(memberR, StringUtils.trim(member)));
			}
			if (StringUtils.isNotBlank(orderNo)) {
				predicate = addAndPredicate(criteriaBuilder, predicate,
						criteriaBuilder.equal(orderNoR, StringUtils.trim(orderNo)));
			}
			if (reviwer != null && reviwer.length > 0) {
				if (reviwer.length == 1)
					predicate = addAndPredicate(criteriaBuilder, predicate,
							criteriaBuilder.equal(reviewerR, reviwer[0]));
				else
					predicate = addAndPredicate(criteriaBuilder, predicate, reviewerR.in(reviwer));
			}
			if (StringUtils.isNotBlank(reviewerType)) {
				if ("robot".equals(reviewerType)) {
					predicate = addAndPredicate(criteriaBuilder, predicate, reviewerR.isNull());
				}
				if ("manual".equals(reviewerType)) {
					predicate = addAndPredicate(criteriaBuilder, predicate, reviewerR.isNotNull());
				}
			}
			if (fmonery != null) {
				predicate = addAndPredicate(criteriaBuilder, predicate,
						criteriaBuilder.greaterThanOrEqualTo(amount, fmonery));
			}
			if (tomoney != null) {
				predicate = addAndPredicate(criteriaBuilder, predicate,
						criteriaBuilder.lessThanOrEqualTo(amount, tomoney));
			}

			return predicate;
		};
		return specification;
	}

	/**
	 * 抽取时间条件，复用
	 */
	private Predicate wrapTimeCondition(Path<Date> updateTime, String startTime, String endTime,
			CriteriaBuilder criteriaBuilder, Predicate predicate) {
		if (StringUtils.isNotEmpty(startTime)) {
			Date startTime1 = CommonUtils.string2Date(startTime);
			predicate = addAndPredicate(criteriaBuilder, predicate,
					criteriaBuilder.and(criteriaBuilder.greaterThanOrEqualTo(updateTime, startTime1)));
		}
		if (StringUtils.isNotEmpty(endTime)) {
			Date endTime1 = CommonUtils.string2Date(endTime);
			predicate = addAndPredicate(criteriaBuilder, predicate,
					criteriaBuilder.and(criteriaBuilder.lessThanOrEqualTo(updateTime, endTime1)));
		}
		return predicate;
	}

	private Predicate addAndPredicate(final CriteriaBuilder criteriaBuilder, final Predicate oldPredicate,
			final Predicate newPredicate) {
		return oldPredicate != null ? criteriaBuilder.and(oldPredicate, newPredicate) : newPredicate;
	}

	/**
	 * 审核通过，生成出款任务:人工审核 userid不为null 机器审核userid 为null
	 */
	@Transactional
	@Override
	public void approve(BizOutwardRequest o, Integer userid, String remark, String memberCode, String orderNo) {
		// 检查是否有出款任务
		Sort sort = new Sort(Sort.Direction.DESC, "asignTime");
		List<BizOutwardTask> tasks = outwardTaskRepository.findByOutwardRequestId(o.getId(), sort);
		if (null == tasks || tasks.size() == 0) {
			String uid;
			if (userid == null) {
				uid = "系统(审核)";
				if (StringUtils.isBlank(remark)) {
					remark = "系统自动审核通过";
				}
				o.setRemark(CommonUtils.genRemark(o.getRemark(), remark, new Date(), uid));
			} else {
				SysUser sysUser = sysUserService.findFromCacheById(userid);
				if (sysUser != null) {
					uid = sysUser.getUid();
					if (StringUtils.isNotBlank(remark)) {
						o.setRemark(CommonUtils.genRemark(o.getRemark(), remark + "(审核)", new Date(), uid));
					} else {
						o.setRemark(CommonUtils.genRemark(o.getRemark(), o.getReview() + "(人工审核)", new Date(), uid));
					}
				}
			}
			Long l = System.currentTimeMillis() - o.getCreateTime().getTime();
			o.setTimeConsuming(l.intValue() / 1000);// 保存审核耗时 单位：秒
			// 系统审核 o.getId() = null ,userId =null 和 人工非主管审核 o.getId()!=null
			// userid!=null
			if (o.getStatus().equals(OutwardRequestStatus.Processing.getStatus())) {
				log.info("非主管审核通过:userId:{},reqId:{},orderNo:{},状态{}", userid, o.getId(), orderNo, o.getStatus());
				try {
					o.setStatus(OutwardRequestStatus.Approved.getStatus());
					o.setReviewer(userid);
					o = update(o);
					splitReqAndGenerateTask(o);
				} catch (Exception e) {
					log.info("非主管审核失败:reqId:{},orderNo:{},状态{},{}", o.getId(), orderNo, o.getStatus(), e);
				}
			} else {
				// 主管审核
				log.info("主管审核通过:orderNo:{},状态{}", orderNo, o.getStatus());
				try {
					int updateResult = updateByIdAndStatus(o.getId(), userid);
					if (updateResult == 1) {
						o.setReviewer(userid);// 防止不立即生效往下传递值不正确
						o.setStatus(OutwardRequestStatus.Approved.getStatus());
						splitReqAndGenerateTask(o);
					}
				} catch (Exception e) {
					log.info("主管审核失败:orderNo:{},{}", orderNo, e);
				}
			}
		} else {
			log.info("订单号：{},请求id:{} ,tasks:{},tasks.size():{},出款任务已存在", o.getOrderNo(), o.getId(), tasks,
					tasks.size());
		}
	}

	/** 根据出款请求生成出款任务 */
	@Transactional(rollbackFor = Exception.class)
	@Override
	public void splitReqAndGenerateTask(BizOutwardRequest o) {
		log.debug("出款请求:id:{},金额{},订单{},状态:{},备注:{},审核人:{}。", o.getId(), o.getAmount(), o.getOrderNo(), o.getStatus(),
				o.getRemark(), o.getReviewer());
		BizOutwardTask taskDaifu = null;
		try {
			// 系统设置的外层拆单金额额基数
			BigDecimal maxAmountOutside = new BigDecimal(outwardTaskAllocateService.findSplitOut());
			// 系统设置的内层拆单金额额基数
			BigDecimal maxAmountInside = new BigDecimal(outwardTaskAllocateService.findSplitIn());
			// 第三方最低出款金额
			if (AppConstants.OUTDREW_THIRD
					&& o.getAmount().floatValue() >= outwardTaskAllocateService.findThirdLowBal()) {
				BizOutwardTask task = generatorTask(o, o.getAmount());
				BizLevel level = levelService.findFromCache(o.getLevel());
				task.setRemark(CommonUtils.genRemark(o.getRemark(),
						genTaskRemark(o, task.getAmount().floatValue(), true, level), new Date(), "系统"));
				task.setOutwardPayType(OutWardPayType.ThirdPay.getType());
				task = outwardTaskRepository.saveAndFlush(task);
				// 第三方出款
				outwardTaskAllocateService.rpush(o, task, true);
				return;
			}
			// 出款任务内外层
			Integer InsideOrOutside = null;
			if (o.getLevel() != null) {
				BizLevel bizLevel = levelService.findFromCache(o.getLevel());
				if (bizLevel != null) {
					InsideOrOutside = bizLevel.getCurrSysLevel();
				}
			}
			if (null == InsideOrOutside) {
				InsideOrOutside = CurrentSystemLevel.Outter.getValue();
			}
			// 判断是否开启代付 true 开启 false 不开启
			boolean enableThirdPay = CommonUtils.checkOutwardThirdInsteadPayEnabled(o.getHandicap());
			log.debug("检查是否开启第三方代付:订单:{},盘口id:{},是否开启:{}", o.getOrderNo(), o.getHandicap(), enableThirdPay);
			// 判断是否支持银行类型
			boolean bankTypeSurpported = enableThirdPay
					&& daifuTaskService.daifuBankTypeCondition(o.getToAccountBank());
			log.debug("判断是否支持银行类型：{},{}", o.getToAccountBank(), bankTypeSurpported);

			// 判断是否满足第三方代付
			boolean daifuCondition = false;
			if (bankTypeSurpported) {
				daifuCondition = o.getStatus().equals(OutwardRequestStatus.Approved.getStatus())
						&& StringUtils.isNotBlank(o.getRemark()) && o.getRemark().contains("系统(审核)")
						&& ObjectUtils.isEmpty(o.getReviewer());
			}
			log.debug("审核通过生成任务,是否可以调用第三方代付3个条件校验:{} ", daifuCondition);
			boolean daifuSupport = false;
			if (daifuCondition) {
				daifuSupport = daifuTaskService.daifuCondition(o);
				log.debug("校验3个条件返回结果:{}", daifuSupport);
			}
			// 拆单，根据系统设置，看是否需要拆分多个任务
			boolean splitAble = (CurrentSystemLevel.Outter.getValue() == InsideOrOutside
					&& o.getAmount().compareTo(maxAmountOutside) > 0)
					|| (CurrentSystemLevel.Inner.getValue() == InsideOrOutside
							&& o.getAmount().compareTo(maxAmountInside) > 0);
			log.debug("审核通过生成任务,InsideOrOutside 内外层值:{},是否拆单:{},", InsideOrOutside, splitAble);
			if (splitAble) {
				int splitAddAmount = 100;//拆单叠加基数，每单在上一单基础上增加100
				log.debug("拆单,第三方代付条件判断:出款请求:备注:{},审核人:{},审核状态:{} ", o.getRemark(), o.getReviewer(), o.getStatus());
				int multiples = (int) (o.getAmount().floatValue()
						/ (InsideOrOutside == CurrentSystemLevel.Outter.getValue() ? maxAmountOutside.floatValue()
								: maxAmountInside.floatValue()));
				float remainder = o.getAmount().floatValue()
						% (InsideOrOutside == CurrentSystemLevel.Outter.getValue() ? maxAmountOutside.floatValue()
								: maxAmountInside.floatValue());
				int splitAddAmountTotal = ((multiples - 1) * 50 + splitAddAmount) * multiples; //根据拆单的数量，计算出额外增加的金额总数
				//余数减去整单额外增加的基数总金额,如果为负数则第一单减去相应金额
				remainder = remainder - splitAddAmountTotal;
				for (int i = 0; i < multiples; i++) {
					BigDecimal amt = InsideOrOutside == CurrentSystemLevel.Outter.getValue() ? maxAmountOutside
							: maxAmountInside;
					amt = i == 0 && remainder < 5000 ? amt.add(new BigDecimal(remainder)) : amt;
					//第一单从100开始，每单在上一单增加的金额上+100，防止拆单出现相同的金额(解决相同金额过多，机器无法连续出相同金额的任务)
					amt = amt.add(new BigDecimal(splitAddAmount * (i + 1)));
					BizOutwardTask task = generatorTask(o, amt);
					task = outwardTaskRepository.saveAndFlush(task);
					boolean daifuCheck = false;
					if (daifuSupport) {
						daifuCheck = daifu4OutwardService.isRead(task);
						log.debug("拆单,是否满足代付调用isRead结果:{}", daifuCheck);
					}
					boolean daifuCallable = multiples <= 1 && daifuCheck;
					if (daifuCallable) {
						log.debug("拆单multiples:{},代付校验:isRead:{},调用代付:{}", multiples, daifuCheck, daifuCallable);
						// 不拆单才代付
						task.setThirdInsteadPay(1);
						task.setOutwardPayType(OutWardPayType.ThirdInsteadPay.getType());
						// 1 表示系统
						task.setOperator(1);
						task.setRemark(CommonUtils.genRemark(task.getRemark(), "系统设置为代付", new Date(), "系统"));
						task = outwardTaskRepository.saveAndFlush(task);
						taskDaifu = task;
						break;
					} else {
						log.debug("拆单multiples:{},代付校验:isRead:{},不调用代付:{}", multiples, daifuCheck, daifuCallable);
						// 待出款任务发送Redis队列，待出款人员认领
						outwardTaskAllocateService.rpush(o, task, false);
					}
				}
				// 拆单剩余金额生成一个任务
				if (remainder >= 5000) {
					BizOutwardTask task = generatorTask(o, new BigDecimal(remainder));
					task = outwardTaskRepository.saveAndFlush(task);
					// 待出款任务发送Redis队列，待出款人员认领
					outwardTaskAllocateService.rpush(o, task, false);
				}
				log.info("订单号：{}, 拆分任务数 {} , 余数 {}", o.getOrderNo(), multiples, remainder);
			} else {
				log.debug("不拆单,第三方代付条件判断:出款请求:备注:{},审核人:{},审核状态:{} ", o.getRemark(), o.getReviewer(), o.getStatus());
				BizOutwardTask task = generatorTask(o, o.getAmount());
				task = outwardTaskRepository.saveAndFlush(task);
				boolean daifuCheck = false;
				if (daifuSupport) {
					daifuCheck = daifu4OutwardService.isRead(task);
					log.debug("不拆单,是否满足代付调用isRead结果:{}", daifuCheck);
				}
				if (daifuCheck) {
					log.debug("不拆单,isRead结果满足代付:{}", daifuCheck);
					taskDaifu = task;
				} else {
					// 待出款任务发送Redis队列，待出款人员认领
					outwardTaskAllocateService.rpush(o, task, false);
				}
				if (taskDaifu != null) {
					log.debug("审核通过并且满足代付条件,立刻调用代付,任务：{}", taskDaifu);
					DaifuResult daifuResult = daifuTaskService.callDaifu(taskDaifu);
					log.debug("审核通过并且满足代付条件,立刻调用代付,返回结果:{},任务：{}", daifuResult, taskDaifu);
					if (daifuResult == null) {
						log.debug("审核通过并且满足代付条件,立刻调用代付,返回结果为空,单号:{},任务id:{},放入队列!", task.getOrderNo(), task.getId());
						task.setThirdInsteadPay(0);
						task.setOperator(null);
						task.setAccountId(null);
						task.setStatus(OutwardTaskStatus.Undeposit.getStatus());
						task.setRemark(CommonUtils.genRemark(task.getRemark(), "调用代付返回结果为空转正常流程", new Date(), "系统"));
						task = outwardTaskRepository.saveAndFlush(task);
						// 待出款任务发送Redis队列，待出款人员认领
						outwardTaskAllocateService.rpush(o, task, false);
					} else {
						boolean deal = daifuTaskService.daifuResultDeal(daifuResult, taskDaifu);
						log.debug("处理立即调用代付返回的结果:{},任务：{}", deal, taskDaifu);
						// 6.2.14 通知平台出款订单使用哪一个第三方出款
						daifuTaskService.selectDaifu(daifuResult, task);
					}
				}
				log.debug("代付任务:{}", taskDaifu);
				log.info("订单号：{}, 出款任务已生成", o.getOrderNo());
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("审核通过生成出款任务异常：", e);
			if (taskDaifu != null) {
				taskDaifu.setThirdInsteadPay(0);
				taskDaifu.setOperator(null);
				taskDaifu.setAccountId(null);
				taskDaifu.setStatus(OutwardTaskStatus.Undeposit.getStatus());
				taskDaifu.setRemark(
						CommonUtils.genRemark(taskDaifu.getRemark(), "审核通过生成出款任务并调用代付异常转正常流程", new Date(), "sys"));
				taskDaifu = outwardTaskRepository.saveAndFlush(taskDaifu);
				// 待出款任务发送Redis队列，待出款人员认领
				outwardTaskAllocateService.rpush(o, taskDaifu, false);
			}
		}
	}

	private String genTaskRemark(BizOutwardRequest o, Float amount, boolean isThird, BizLevel level) {
		String l = "未指定层级";
		if (Objects.nonNull(level) && Objects.nonNull(level.getCurrSysLevel())) {
			CurrentSystemLevel csl = CurrentSystemLevel.valueOf(level.getCurrSysLevel());
			if (Objects.nonNull(csl))
				l = csl.getName();
		}
		return isThird ? "生成任务(第三方出款-" + l + ")"
				: (outwardTaskAllocateService.checkManual(o, amount) ? "生成任务(人工出款-" + l + ")" : "生成任务(机器出款-" + l + ")");
	}

	private BizOutwardTask generatorTask(BizOutwardRequest o, BigDecimal amount) {
		BizOutwardTask task = new BizOutwardTask();
		BizLevel bizLevel = null;
		BizHandicap bizHandicap = null;
		if (o.getLevel() != null) {
			bizLevel = levelService.findFromCache(o.getLevel());
		}

		task.setAmount(amount);
		task.setOutwardRequestId(o.getId());
		task.setToAccount(o.getToAccount());
		task.setToAccountOwner(o.getToAccountOwner());
		task.setStatus(OutwardTaskStatus.Undeposit.getStatus());
		if (o.getHandicap() != null) {
			bizHandicap = handicapService.findFromCacheById(o.getHandicap());
		}
		task.setHandicap(
				bizHandicap != null && StringUtils.isNotBlank(bizHandicap.getCode()) ? bizHandicap.getCode() : null);
		task.setLevel(bizLevel != null && StringUtils.isNotBlank(bizLevel.getName()) ? bizLevel.getName() : null);
		if (bizLevel != null && bizLevel.getCurrSysLevel() != null) {
			task.setCurrSysLevel(bizLevel.getCurrSysLevel());
		} else {
			task.setCurrSysLevel(CurrentSystemLevel.Outter.getValue());
		}
		task.setOrderNo(o.getOrderNo());
		task.setMember(o.getMember());
		// 先设置outwardPayType，普通的设置为 OutWardPayType.REFUND.getType()，人工的设置为
		// OutWardPayType.MANUAL.getType()
		// 三方和三方代付在具体业务中修改
		boolean isManual = outwardTaskAllocateService.isManualTask(o, amount.floatValue());
		int outwardPayType = OutWardPayType.REFUND.getType();
		if (isManual)
			outwardPayType = OutWardPayType.MANUAL.getType();
		task.setOutwardPayType(outwardPayType);
		// task.setRemark(CommonUtils.genRemark(o.getRemark(), "生成任务", new
		// Date(), "系统"));
		task.setRemark(CommonUtils.genRemark(o.getRemark(),
				genTaskRemark(o, task.getAmount().floatValue(), false, bizLevel), new Date(), "系统"));
		return task;
	}

	@Override
	public void reportStatus2Platform(Long id, final int status, String remark, String memberCode, String orderNo,
			SysUser operator) {
		try {
			BizOutwardRequest entity = get(id);
			Date d = new Date();
			// 主管处理
			if (status == OutwardRequestStatus.ManagerProcessing.getStatus()) {
				entity.setStatus(status);
				entity.setRemark(CommonUtils.genRemark(null, remark, d, operator.getUid()));
				entity.setMemberCode(memberCode);
				Long time = System.currentTimeMillis() - entity.getCreateTime().getTime();
				entity.setTimeConsuming(time.intValue() / 1000);
				// entity.setUpdateTime(d);
				outwardRequestService.update(entity);
			} else if (status == OutwardRequestStatus.Reject.getStatus()) {
				outwardTaskAllocateService.alterStatusToRefuse(id, null, operator, remark);
			} else if (status == OutwardRequestStatus.Canceled.getStatus()) {
				outwardTaskAllocateService.alterStatusToCancel(id, null, operator, remark);
			}
		} catch (Exception e) {
			log.error("", e);
		}
	}

	/**
	 * 通过用户id查询用户权限下的盘口层级对应的提单
	 *
	 * @param userid
	 *            用户表主键ID
	 * @return
	 * @modify
	 */
	@Override
	@Transactional
	public BizOutwardRequest getApproveTask(int userid) {
		// 先查找已该用户已认领但未处理的任务
		List<BizOutwardRequest> historys = outwardRequestRepository.findByReviewerAndStatus(userid);
		if (null != historys && historys.size() > 0) {
			return historys.get(0);
		}
		List<SysDataPermission> sysDataPermissions = sysDataPermissionService.findSysDataPermission(userid);
		List<Integer> handicap = new ArrayList<>();
		List<Integer> level = new ArrayList<>();
		if (sysDataPermissions != null && sysDataPermissions.size() > 0) {
			sysDataPermissions.forEach((p) -> {
				if (SysDataPermissionENUM.HANDICAPCODE.getValue().equals(p.getFieldName())) {
					handicap.add(Integer.parseInt(p.getFieldValue()));
				}
				if (SysDataPermissionENUM.LEVELCODE.getValue().equals(p.getFieldName())) {
					level.add(Integer.parseInt(p.getFieldValue()));
					BizLevel bizLevel = levelService.findFromCache(Integer.parseInt(p.getFieldValue()));
					if (bizLevel != null && bizLevel.getHandicapId() != null) {
						handicap.add(bizLevel.getHandicapId());
					}
				}
			});
		}
		BizOutwardRequest bizOutwardRequest;
		BigDecimal amountLimit = null;
		SysUserProfile sysUserProfile = sysUserProfileService.findByUserIdAndPropertyKey(userid,
				"INCOME_SYSAUDITLIMIT");
		if (sysUserProfile != null && StringUtils.isNotBlank(sysUserProfile.getPropertyValue())) {
			amountLimit = new BigDecimal(sysUserProfile.getPropertyValue());
		}
		if (amountLimit == null || BigDecimal.ZERO.equals(amountLimit)) {
			bizOutwardRequest = new BizOutwardRequest();
			bizOutwardRequest.setStatus(99999);
			return bizOutwardRequest;
		}
		if (!CollectionUtils.isEmpty(handicap) && !CollectionUtils.isEmpty(level)) {
			bizOutwardRequest = outwardRequestRepository.getApproveTaskIn(distinct(handicap), level, amountLimit);
		} else {
			bizOutwardRequest = outwardRequestRepository.getApproveTask(amountLimit);
		}
		if (null == bizOutwardRequest) {
			return null;
		}
		if (bizOutwardRequest.getReviewer() != null && bizOutwardRequest.getReviewer() != userid) {
			return null;// 可以递归
		} else {
			try {
				int updateFlag = updateById(bizOutwardRequest.getId(), userid);
				if (updateFlag == 1) {
					// 即使updateFlag==1但是数据库并没有立即更新没成功， 所以先set再返回
					bizOutwardRequest.setReviewer(userid);
					return bizOutwardRequest;
				} else {
					return null;// 可以递归
				}
			} catch (Exception e) {
				log.error("获取出款审核失败,error:", e);
				return null;
			}
		}
	}

	public List<Integer> distinct(List<Integer> bizHandicapList) {
		if (bizHandicapList == null || bizHandicapList.size() == 0) {
			return Collections.emptyList();
		}
		Set<Integer> set = new TreeSet<>(Comparator.reverseOrder());
		set.addAll(bizHandicapList);
		return new ArrayList<>(set);
	}

	@Override
	public String getOutwardDetails(Long id) {
		if (id == null) {
			return "";
		}
		ThreadLocal<String> json = new ThreadLocal<String>();
		BizOutwardRequest o = get(id);
		log.info("传入的参数：id:{},查询后获取的参数：Handicap:{}，MemberCode:{}，OrderNo:{}", id, o.getHandicap(), o.getMemberCode(),
				o.getOrderNo());
		// new and old api compatibility
		String handicap = handicapService.findFromCacheById(o.getHandicap()).getCode();
		HttpClientNew.getInstance().getPlatformServiceApi()
				.withdrawalAudit(requestBodyParser.buildRequestBody(handicap, o.getOrderNo(), "")).subscribe(data -> {
					ObjectMapper mapper = new ObjectMapper();
					try {
						json.set(mapper.writeValueAsString(data));
						log.info("获取的信息:参数:{},结果:{}", o.getOrderNo(), mapper.writeValueAsString(data));
					} catch (Exception e1) {
						log.error("(new)Outward WithdrawAuditInfo error.", e1);
						json.set("系统异常:" + e1 + ",请稍后刷新页面。");
					}
				}, e -> {
					log.info("(new)Outward WithdrawAuditInfo error. orderNo: " + o.getOrderNo() + ", "
							+ e.getLocalizedMessage(), e);
					json.set("{Result=0, Desc=调用平台异常:" + e + ",请稍后刷新页面。");
				});
		// }
		return json.get();
	}

	private StringBuffer checkedInfo(WithdrawAuditInfo o, BizOutwardRequest entity, StringBuffer sb) {
		if (null != o && o.getResult() == 1) {
			if (StringUtils.isNotBlank(entity.getToAccountOwner()) && entity.getToAccountOwner().contains("方振跃")) {
				sb.append("黑名单方振跃");
				return sb;
			}
			// 1-是否大额出款,先获取系统设置，检查，超出人工审,在入库时已作判断
			// 2-首次出款，人工审
			if (o.getiWithdrawStatus() == 1) {
				sb.append("首次出款; ");
			} else {
				// 3-上次出款银行卡与本次出款银行卡不一致，人工审
				if (StringUtils.isNotBlank(o.getsUpWithdrawCard()) && StringUtils.isNotBlank(entity.getToAccount())
						&& !entity.getToAccount().equals(o.getsUpWithdrawCard())) {
					sb.append("本次出款与上次出款银行卡不一致; ");
				}
			}
			// 本次总入款（包括第三方，公司入款，优惠等）
			BigDecimal totalInAmount = new BigDecimal("0");
			if (null != o.getDepositLists() && o.getDepositLists().size() > 0) {
				// 旧平台可能有传这个
				for (Deposit in : o.getDepositLists()) {
					totalInAmount = totalInAmount.add(new BigDecimal(in.getfAmount()));
				}
			} else {
				if (StringUtils.isNotBlank(o.getRecentDepositTotalAmount())) {
					// 新平台传这个
					totalInAmount = new BigDecimal(o.getRecentDepositTotalAmount());
				}
			}
			// 4-检查打码情况，上次出款后余额+本次总入款（包括第三方，公司入款，优惠等）+盈利-本次出款金额=出款后余额，不符合规则，人工审
			// 或者：上次出款前余额-上次出款金额+最近入款明细+本次中奖金额-本次下注金额=出款前余额
			// EVM =
			// 上次出款前余额-上次出款金额+最近入款明细+本次中奖金额(如果没有 则使用获利金额)-本次下注金额
			String fWinAmount = StringUtils.isNotBlank(o.getfWinAmount()) ? o.getfWinAmount()
					: StringUtils.isNotBlank(o.getfOmProfit()) ? o.getfOmProfit() : "0";
			BigDecimal evm = new BigDecimal(o.getfUpWithdrawBalance())
					.subtract(new BigDecimal(o.getfUpWithdrawAmount())).add(totalInAmount)
					.add(new BigDecimal(fWinAmount)).subtract(new BigDecimal(o.getfBetAmount()));
			BigDecimal difference = evm.subtract(new BigDecimal(o.getfWithdrawBalance())).setScale(2,
					RoundingMode.HALF_UP);
			if (difference.intValue() != 0 && entity.getAmount().floatValue() >= 5000) {// 出款金额>=5000
				sb.append("打码情况与余额对不上，差额").append(difference).append("元;");
			}
			// 5-本次已达投注量/本次所有入款金额<=n倍打码量，人工审,
			// if (totalInAmount.intValue() > 0) {
			// // 出款审核几倍打码量限制
			// BigDecimal rate = new BigDecimal("0");
			// try {
			// rate = new
			// BigDecimal(MemCacheUtils.getInstance().getSystemProfile()
			// .get(UserProfileKey.OUTDRAW_CHECK_CODE.getValue()));
			// } catch (Exception e1) {
			// log.error("获取出款审核几倍打码量限制异常，采用默认值1倍打码量, key :{}",
			// UserProfileKey.OUTDRAW_CHECK_CODE.getValue());
			// }
			// // BigDecimal rawRate = (new
			// //
			// BigDecimal(o.getfBetAmount()).divide(totalInAmount)).setScale(2,
			// // RoundingMode.HALF_UP);
			// String rawRate = (new
			// BigDecimal(o.getfBetAmount())).divide(totalInAmount, 2,
			// BigDecimal.ROUND_HALF_UP)
			// .toString();
			// if (new BigDecimal(rawRate).subtract(rate).doubleValue() <= 0) {
			// sb.append(rawRate + "倍打码量，小于或等于系统设置" + rate);
			// }
			// }
		} else {
			log.info("{}, 获取审核信息失败.", entity.getOrderNo());
			sb.append("平台返回失败");
		}
		return sb;
	}

	@Override
	public Observable<String> autoCheckOutwardRequest(BizOutwardRequest entity) {
		return Observable.create(subscriber -> {
			// new and old api compatibility
			String handicap = handicapService.findFromCacheById(entity.getHandicap()).getCode();
			HttpClientNew.getInstance().getPlatformServiceApi()
					.withdrawalAudit(requestBodyParser.buildRequestBody(handicap, entity.getOrderNo(), ""))
					.subscribe(o -> {
						log.info("(new)autoAudit  withdrawalAudit   orderNo: {}, desc:{},error:{},result: {}",
								entity.getOrderNo(), o.getDesc(), o.getError(), o.getResult());
						StringBuffer sb = new StringBuffer();
						try {
							sb = checkedInfo(o, entity, sb);
						} catch (Exception e) {
							log.error("(new) autoAudit  withdrawalAudit orderNo: " + entity.getOrderNo() + " ,error :",
									e);
							sb.append("系统异常");
						}
						subscriber.onNext(sb.toString());
					}, e -> {
						log.error("(new)autoAudit WithdrawAuditInfo error. orderNo: " + entity.getOrderNo(), e);
						entity.setReview(e.getLocalizedMessage());
						outwardRequestService.save(entity);
						subscriber.onError(new RuntimeException("autoAudit withdrawalAudit fail." + e));
					});
			// }
		});
	}

	@Override
	public List<BizOutwardRequest> findAll(Specification<BizOutwardRequest> specification) {
		return outwardRequestRepository.findAll(specification);
	}

	@Override
	public List<BizOutwardRequest> findByAccountAndStatusAndAmount(String account, List<Integer> status, Float amount,
			int type) {
		return outwardRequestRepository.findByAccountAndStatusAndAmount(account, status, amount, type);
	}

	@Override
	public List<Object> quickBackToRush(String startTime, String endTime) {
		return outwardRequestRepository.quickBackToRush(startTime, endTime);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void saveThirdOut(int fromId, BigDecimal amount, BizOutwardRequest req, SysUser user, String remark) {
		BizHandicap bizHandicap = handicapService.findFromCacheById(req.getHandicap());
		BizLevel bizLevel = levelService.findFromCache(req.getLevel());
		outwardRequestRepository.saveThirdOut(fromId, req.getToAccount(), req.getToAccountOwner(),
				req.getToAccountBank(), amount, remark, bizHandicap.getCode(), req.getOrderNo(), req.getMember(),
				bizLevel.getName(), user != null ? user.getUid() : "系统");

	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void saveThirdOutWithFee(int fromId, BigDecimal amount, BigDecimal fee, BizOutwardRequest req, SysUser user,
			String remark) {
		BizHandicap bizHandicap = handicapService.findFromCacheById(req.getHandicap());
		BizLevel bizLevel = levelService.findFromCache(req.getLevel());
		outwardRequestRepository.saveThirdOutWithFee(fromId, req.getToAccount(), req.getToAccountOwner(),
				req.getToAccountBank(), amount, remark, bizHandicap.getCode(), req.getOrderNo(), req.getMember(),
				bizLevel.getName(), user != null ? user.getUid() : "系统", fee);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void saveThirdCashOut(int fromId, String toAccount, String toAccountOwner, String toAccountBank,
			String amount, String fee, String remark, SysUser user) {
		outwardRequestRepository.saveThirdCashOut(fromId, toAccount, toAccountOwner, toAccountBank, amount, fee, remark,
				user.getUid());
	}

	public void rpush(long taskId) {
		BizOutwardTask task = outwardTaskRepository.findById2(taskId);
		if (task != null) {
			BizOutwardRequest request = outwardRequestRepository.findById2(task.getOutwardRequestId());
			if (task != null && request != null) {
				if (AppConstants.OUTDREW_THIRD
						&& task.getAmount().floatValue() >= outwardTaskAllocateService.findThirdLowBal()) {
					outwardTaskAllocateService.rpush(request, task, true);
				} else {
					outwardTaskAllocateService.rpush(request, task, false);
				}
			}
		}
	}
}
