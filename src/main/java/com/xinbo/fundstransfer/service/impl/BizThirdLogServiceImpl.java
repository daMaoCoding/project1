package com.xinbo.fundstransfer.service.impl;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.*;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.domain.entity.BizThirdLog;
import com.xinbo.fundstransfer.domain.repository.BizThirdLogRepository;
import com.xinbo.fundstransfer.domain.repository.QueryNoCountDao;
import com.xinbo.fundstransfer.service.BizThirdLogService;

/**
 * 第三方流水
 */
@Service
public class BizThirdLogServiceImpl implements BizThirdLogService {
	@Autowired
	private QueryNoCountDao queryNoCountDao;
	@Autowired
	private BizThirdLogRepository bizThirdLogRepository;
	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public Page<BizThirdLog> page(String orderNo, String channel, String startTime, String endTime, String startMoney,
			String endMoney, List<Integer> fromAccountIdList, PageRequest pageRequest) {
		Specification<BizThirdLog> specification = wrapSpecificationForThirdLog(orderNo, channel, startTime, endTime,
				startMoney, endMoney, fromAccountIdList);
		Page<BizThirdLog> page = bizThirdLogRepository.findAll(specification, pageRequest);
		return page;
	}

	@Override
	public Page<BizThirdLog> pageNoCount(String orderNo, String channel, String startTime, String endTime,
			String startMoney, String endMoney, List<Integer> fromAccountIdList, PageRequest pageRequest) {
		Specification<BizThirdLog> specification = wrapSpecificationForThirdLog(orderNo, channel, startTime, endTime,
				startMoney, endMoney, fromAccountIdList);
		Page<BizThirdLog> page = queryNoCountDao.findAll(specification, pageRequest, BizThirdLog.class);
		return page;
	}

	/**
	 * 查询第三方流水 总金额
	 */
	@Override
	public List sum(String orderNo, String channel, String startTime, String endTime, String startMoney,
			String endMoney, List<Integer> fromAccountIdList, PageRequest pageRequest) {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Object> query = criteriaBuilder.createQuery(Object.class);
		Root<BizThirdLog> root = query.from(BizThirdLog.class);
		Predicate predicate = criteriaBuilder.conjunction();
		final Path<Integer> fromAccountP = root.get("fromAccount");
		final Path<Date> tradingTimeP = root.get("tradingTime");
		final Path<BigDecimal> amountP = root.get("amount");
		final Path<BigDecimal> feeP = root.get("fee");
		final Path<BigDecimal> balanceP = root.get("balance");
		final Path<String> orderNoP = root.get("orderNo");
		final Path<String> channelP = root.get("channel");
		List<Expression<Boolean>> expressions = predicate.getExpressions();
		Expression<BigDecimal> sumTrxAmt1 = criteriaBuilder.sum(amountP);
		Expression<BigDecimal> sumTrxAmt2 = criteriaBuilder.sum(feeP);
		Expression<BigDecimal> sumTrxAmt3 = criteriaBuilder.sum(balanceP);
		if (fromAccountIdList != null && fromAccountIdList.size() > 0) {
			expressions.add(criteriaBuilder.and(fromAccountP.in(fromAccountIdList)));
		}
		if (StringUtils.isNotEmpty(startTime)) {
			Date startDate = CommonUtils.string2Date(startTime);
			expressions.add(criteriaBuilder.greaterThanOrEqualTo(tradingTimeP, startDate));
		}
		if (StringUtils.isNotEmpty(endTime)) {
			Date endDate = CommonUtils.string2Date(endTime);
			expressions.add(criteriaBuilder.lessThanOrEqualTo(tradingTimeP, endDate));
		}
		if (StringUtils.isNotEmpty(startMoney)) {
			expressions.add(criteriaBuilder.greaterThanOrEqualTo(amountP, new BigDecimal(startMoney)));
		}
		if (StringUtils.isNotEmpty(endMoney)) {
			expressions.add(criteriaBuilder.lessThanOrEqualTo(amountP, new BigDecimal(endMoney)));
		}
		if (StringUtils.isNotEmpty(orderNo)) {
			expressions.add(criteriaBuilder.like(orderNoP, "%" + orderNo + "%"));
		}
		if (StringUtils.isNotEmpty(channel)) {
			expressions.add(criteriaBuilder.like(channelP, "%" + channel + "%"));
		}
		query.where(predicate);
		query.multiselect(sumTrxAmt1, sumTrxAmt2, sumTrxAmt3);
		Object[] result1 = entityManager.createQuery(query).getResultList().toArray();
		return Arrays.asList(result1);
	}

	private Specification<BizThirdLog> wrapSpecificationForThirdLog(String orderNo, String channel, String startTime,
			String endTime, String startMoney, String endMoney, List<Integer> fromAccountIdList) {
		Specification specification = (root, criteriaQuery, criteriaBuilder) -> {
			Predicate predicate = null;
			final Path<Integer> fromAccountP = root.get("fromAccount");
			final Path<Date> tradingTimeP = root.get("tradingTime");
			final Path<BigDecimal> amountP = root.get("amount");
			final Path<String> orderNoP = root.get("orderNo");
			final Path<String> channelP = root.get("channel");
			if (fromAccountIdList != null && fromAccountIdList.size() > 0) {
				Expression<Integer> exp = fromAccountP;
				predicate = addAndPredicate(criteriaBuilder, predicate, exp.in(fromAccountIdList));
			}
			if (StringUtils.isNotEmpty(startTime)) {
				Date startDate = CommonUtils.string2Date(startTime);
				predicate = addAndPredicate(criteriaBuilder, predicate,
						criteriaBuilder.greaterThanOrEqualTo(tradingTimeP, startDate));
			}
			if (StringUtils.isNotEmpty(endTime)) {
				Date endDate = CommonUtils.string2Date(endTime);
				predicate = addAndPredicate(criteriaBuilder, predicate,
						criteriaBuilder.lessThanOrEqualTo(tradingTimeP, endDate));
			}
			if (StringUtils.isNotEmpty(startMoney)) {
				predicate = addAndPredicate(criteriaBuilder, predicate,
						criteriaBuilder.greaterThanOrEqualTo(amountP, new BigDecimal(startMoney)));
			}
			if (StringUtils.isNotEmpty(endMoney)) {
				predicate = addAndPredicate(criteriaBuilder, predicate,
						criteriaBuilder.lessThanOrEqualTo(amountP, new BigDecimal(endMoney)));
			}
			if (StringUtils.isNotEmpty(orderNo)) {
				predicate = addAndPredicate(criteriaBuilder, predicate,
						criteriaBuilder.like(orderNoP, "%" + orderNo + "%"));
			}
			if (StringUtils.isNotEmpty(channel)) {
				predicate = addAndPredicate(criteriaBuilder, predicate,
						criteriaBuilder.like(channelP, "%" + channel + "%"));
			}
			return predicate;
		};
		return specification;
	}

	private Predicate addAndPredicate(final CriteriaBuilder criteriaBuilder, final Predicate oldPredicate,
			final Predicate newPredicate) {
		return oldPredicate != null ? criteriaBuilder.and(oldPredicate, newPredicate) : newPredicate;
	}
}
