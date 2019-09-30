package com.xinbo.fundstransfer.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.domain.enums.TransactionLogType;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xinbo.fundstransfer.domain.DynamicPredicate;
import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizTransactionLog;
import com.xinbo.fundstransfer.domain.repository.AccountRepository;
import com.xinbo.fundstransfer.domain.repository.TransactionLogRepository;
import com.xinbo.fundstransfer.service.TransactionLogService;
import org.springframework.util.CollectionUtils;

@Service
public class TransactionLogServiceImpl implements TransactionLogService {
	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(TransactionLogServiceImpl.class);

	@Autowired
	private TransactionLogRepository transactionLogRepository;
	@Autowired
	private AccountRepository accountRepository;
	@PersistenceContext
	private EntityManager entityManager;

	private static final Cache<Integer, Boolean> AccCreditCacheBuilder = CacheBuilder.newBuilder().maximumSize(20000)
			.expireAfterWrite(10, TimeUnit.MINUTES).build();

	@Override
	public Page<BizTransactionLog> findAll(Pageable pageable) throws Exception {

		return null;
	}

	@Override
	public Page<BizTransactionLog> findAll(Specification<BizTransactionLog> specification, Pageable pageable)
			throws Exception {
		return transactionLogRepository.findAll(specification, pageable);
	}

	@Override
	public BigDecimal[] findAmountAndFeeByTotal(SearchFilter[] filterToArray) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Tuple> query = cb.createTupleQuery();
		Root<BizTransactionLog> root = query.from(BizTransactionLog.class);
		javax.persistence.criteria.Predicate[] predicateArray = DynamicPredicate.build(cb, query, root,
				BizTransactionLog.class, filterToArray);
		query.multiselect(cb.sum(root.<BigDecimal>get("amount")), cb.sum(root.<BigDecimal>get("fee")));
		query.where(predicateArray);
		Object[] objArray = entityManager.createQuery(query).getSingleResult().toArray();
		return new BigDecimal[] { (BigDecimal) objArray[0], (BigDecimal) objArray[1] };
	}

	@Override
	public BizTransactionLog get(Long id) {
		return transactionLogRepository.findOne(id);
	}

	@Transactional
	@Override
	public BizTransactionLog save(BizTransactionLog entity) {
		return transactionLogRepository.saveAndFlush(entity);
	}

	/**
	 * 平账用，同步修改系统余额
	 */
	@Override
	@Transactional
	public BizTransactionLog flatBalance(BizTransactionLog entity) {
		BizAccount account = accountRepository.findById2(entity.getFromAccount());
		account.setBalance(account.getBalance().add(entity.getAmount()));
		accountRepository.saveAndFlush(account);
		return transactionLogRepository.save(entity);
	}

	@Transactional
	@Override
	public BizTransactionLog update(BizTransactionLog entity) {
		return transactionLogRepository.saveAndFlush(entity);
	}

	@Transactional
	@Override
	public void delete(Long id) {
		transactionLogRepository.delete(id);
	}

	@Override
	public BizTransactionLog findByReqId(Long OrderId) {
		List<BizTransactionLog> logList = transactionLogRepository.findByOrderId(OrderId);
		logList = logList.stream().filter(p -> !TransactionLogType.OUTWARD.getType().equals(p.getType()))
				.collect(Collectors.toList());
		return CollectionUtils.isEmpty(logList) ? null : logList.get(0);
	}

	@Override
	public BizTransactionLog findByOrderIdAndType(Long orderId, Integer type) {
		return transactionLogRepository.findByOrderIdAndType(orderId, type);
	}

	@Override
	public BizTransactionLog findByToBanklogId(Long toBanklogId) {
		return transactionLogRepository.findByToBanklogId(toBanklogId);
	}

	@Override
	public BizTransactionLog findByFromBanklogId(Long fromBanklogId) {
		return transactionLogRepository.findByFromBanklogId(fromBanklogId);
	}

	@Override
	public List<BizTransactionLog> findByTypeNotAndOrderIdIn(int typeNot, List<Long> orderIdIn) {
		return transactionLogRepository.findByTypeNotAndOrderIdIn(typeNot, orderIdIn);
	}

	@Override
	public void updateByFromIdToIdAmount(Integer fromId, Integer toId, BigDecimal amount) {
		float famount = amount.setScale(2, RoundingMode.HALF_UP).floatValue();
		String fmt = "select id from biz_transaction_log where from_account = %d and to_account=%d and create_time>='%s' and amount = %f order by id desc limit 1";
		List<Object> idList = entityManager
				.createNativeQuery(String.format(fmt, fromId, toId, CommonUtils.getStartTimeOfCurrDay(), famount))
				.getResultList();
		if (!CollectionUtils.isEmpty(idList)) {
			transactionLogRepository.updateToBanklogIdZero(Integer.parseInt(idList.get(0).toString()));
		}
		AccCreditCacheBuilder.put(toId, null);
	}

	@Override
	public boolean checkNonCredit(AccountBaseInfo base) {
		if (Objects.isNull(base))
			return true;
		Boolean check = AccCreditCacheBuilder.getIfPresent(base.getId());
		if (Objects.nonNull(check)) {
			return check;
		}
		int[] cnt = new int[] { 0, 0 };
		String fmt = "select to_banklog_id from biz_transaction_log where to_account=%d and create_time>='%s' order by id desc limit 2";
		entityManager.createNativeQuery(String.format(fmt, base.getId(), CommonUtils.getStartTimeOfCurrDay()))
				.getResultList().forEach(p -> {
					cnt[0] = cnt[0] + 1;
					if (Objects.nonNull(p))
						cnt[1] = cnt[1] + 1;
				});
		check = !(cnt[0] == 2 && cnt[1] == 0);
		AccCreditCacheBuilder.put(base.getId(), check);
		if (!check) {
			log.info("TransactionLogService >> checkNonCredit is false acc id {}", base.getId());
		}
		return check;
	}
}
