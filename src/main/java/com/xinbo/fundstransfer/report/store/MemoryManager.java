package com.xinbo.fundstransfer.report.store;

import com.google.common.util.concurrent.Striped;
import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.entity.BizSysLog;
import com.xinbo.fundstransfer.domain.repository.BankLogRepository;
import com.xinbo.fundstransfer.domain.repository.SysLogRepository;
import com.xinbo.fundstransfer.report.SysBalUtils;
import com.xinbo.fundstransfer.utils.ServiceDomain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

@Component
public class MemoryManager {
	@PersistenceContext
	private EntityManager entityManager;
	@Autowired
	private SysLogRepository sysLogDao;
	@Autowired
	private BankLogRepository bankLogDao;
	// 标识：是否加载完毕
	private static boolean FINISH_LOAD = false;
	// 每个账号缓存最大数据（系统账目）
	private static final int FIX_LIST_SIZE_SYS_LOG = 400;
	// 每个账号缓存最大数据（银行流水）
	private static final int FIX_LIST_SIZE_BANK_LOG = 200;
	// 缓存：系统账目
	private static final Map<Integer, List<BizSysLog>> MEM_SYS_LOG = new ConcurrentHashMap<>();
	// 缓存：银行流水
	private static final Map<Integer, List<BizBankLog>> MEM_BANK_LOG = new ConcurrentHashMap<>();
	private static final Striped<Lock> STRIPED_SYS = Striped.lazyWeakLock(1024);
	private static final Striped<Lock> STRIPED_BANK = Striped.lazyWeakLock(1024);

	private static boolean checkHostRunRight = false;

	@Value("${service.tag}")
	public void setServiceTag(String serviceTag) {
		if (Objects.nonNull(serviceTag))
			checkHostRunRight = ServiceDomain.valueOf(serviceTag) == ServiceDomain.ACCOUNTING;
	}

	@PostConstruct
	public void initWhenReboot() {
		if (!checkHostRunRight)
			return;
		// 加载最近一天系统账目数据到内存中
		loadSysLog();
		// 加载最近一天银行流水数据到内存中
		loadBankLog();
		FINISH_LOAD = true;
	}

	protected boolean finishLoad() {
		return FINISH_LOAD;
	}

	protected List<BizSysLog> findSysLog(Integer accountId) {
		if (Objects.isNull(accountId))
			return Collections.EMPTY_LIST;
		Lock lock = STRIPED_SYS.get(accountId);
		try {
			lock.lock();
			List<BizSysLog> list = MEM_SYS_LOG.get(accountId);
			if (Objects.isNull(list)) {
				list = Collections.synchronizedList(new ArrayList<>());
				MEM_SYS_LOG.put(accountId, list);
			}
			return list.stream().sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o2.getId() - o1.getId()))
					.collect(Collectors.toList());
		} finally {
			lock.unlock();
		}
	}

	protected List<BizBankLog> findBankLog(Integer accountId) {
		if (Objects.isNull(accountId))
			return Collections.EMPTY_LIST;
		Lock lock = STRIPED_BANK.get(accountId);
		try {
			lock.lock();
			List<BizBankLog> list = MEM_BANK_LOG.get(accountId);
			if (Objects.isNull(list)) {
				list = Collections.synchronizedList(new ArrayList<>());
				MEM_BANK_LOG.put(accountId, list);
			}
			return list.stream().sorted((o1, o2) -> SysBalUtils.oneZeroMinus(o2.getId() - o1.getId()))
					.collect(Collectors.toList());
		} finally {
			lock.unlock();
		}
	}

	protected void addSysLog(BizSysLog lg) {
		if (Objects.isNull(lg) || Objects.isNull(lg.getAccountId()))
			return;
		Lock lock = STRIPED_SYS.get(lg.getAccountId());
		try {
			lock.lock();
			List<BizSysLog> lgList = MEM_SYS_LOG.get(lg.getAccountId());
			if (Objects.isNull(lgList)) {
				lgList = Collections.synchronizedList(new ArrayList<>());
				MEM_SYS_LOG.put(lg.getAccountId(), lgList);
			}
			int index = lgList.indexOf(lg);
			if (index >= 0) {
				lgList.set(index, lg);
			} else {
				lgList.add(lg);
				if (lgList.size() > FIX_LIST_SIZE_SYS_LOG) {
					lgList.remove(0);
				}
			}
		} finally {
			lock.unlock();
		}
	}

	protected void addBankLog(BizBankLog lg) {
		if (Objects.isNull(lg) || Objects.isNull(lg.getId()) || lg.getFromAccount() == 0)
			return;
		Lock lock = STRIPED_BANK.get(lg.getFromAccount());
		try {
			lock.lock();
			List<BizBankLog> lgList = MEM_BANK_LOG.get(lg.getFromAccount());
			if (Objects.isNull(lgList)) {
				lgList = Collections.synchronizedList(new ArrayList<>());
				MEM_BANK_LOG.put(lg.getFromAccount(), lgList);
			}
			int index = lgList.indexOf(lg);
			if (index >= 0) {
				lgList.set(index, lg);
			} else {
				lgList.add(lg);
				if (lgList.size() > FIX_LIST_SIZE_BANK_LOG)
					lgList.remove(0);
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 加载最近一天系统账目数据到内存中
	 *
	 */
	private void loadSysLog() {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		Date startTime = new Date(System.currentTimeMillis() - 3600000 * 24);
		CriteriaQuery<Tuple> query = cb.createTupleQuery();
		Root<BizSysLog> root = query.from(BizSysLog.class);
		Path<Long> idPath = root.get("id");
		query.multiselect(cb.min(idPath), cb.max(idPath));
		query.where(cb.greaterThanOrEqualTo(root.get("createTime"), cb.literal(startTime)));
		entityManager.createQuery(query);
		TypedQuery<Tuple> q = entityManager.createQuery(query);
		List<Tuple> result = q.getResultList();
		Tuple tuple = result.get(0);
		Long minId = (Long) tuple.get(0);
		Long maxId = (Long) tuple.get(1);
		if (Objects.nonNull(minId) && Objects.nonNull(maxId)) {
			maxId = maxId + 1000;
			while (minId <= maxId) {
				List<BizSysLog> sysList = sysLogDao.findFirst5000ByIdBetween(minId, minId + 5000);
				if (!CollectionUtils.isEmpty(sysList))
					sysList.forEach(this::addSysLog);
				minId = minId + 5000;
			}
		}
	}

	/**
	 * 加载最近一天银行流水到内存中
	 *
	 */
	private void loadBankLog() {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		Date startTime = new Date(System.currentTimeMillis() - 3600000 * 24);
		CriteriaQuery<Tuple> query = cb.createTupleQuery();
		Root<BizBankLog> root = query.from(BizBankLog.class);
		Path<Long> idPath = root.get("id");
		query.multiselect(cb.min(idPath), cb.max(idPath));
		query.where(cb.greaterThanOrEqualTo(root.get("createTime"), cb.literal(startTime)));
		entityManager.createQuery(query);
		TypedQuery<Tuple> q = entityManager.createQuery(query);
		List<Tuple> result = q.getResultList();
		Tuple tuple = result.get(0);
		Long minId = (Long) tuple.get(0);
		Long maxId = (Long) tuple.get(1);
		if (Objects.nonNull(minId) && Objects.nonNull(maxId)) {
			maxId = maxId + 1000;
			while (minId <= maxId) {
				List<BizBankLog> bankList = bankLogDao.findFirst5000ByIdBetween(minId, minId + 5000);
				if (!CollectionUtils.isEmpty(bankList))
					bankList.forEach(this::addBankLog);
				minId = minId + 5000;
			}
		}
	}
}
