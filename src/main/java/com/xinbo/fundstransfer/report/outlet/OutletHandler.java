package com.xinbo.fundstransfer.report.outlet;

import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizSysLog;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.enums.SysLogStatus;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalTrans;
import com.xinbo.fundstransfer.domain.repository.AccountRepository;
import com.xinbo.fundstransfer.report.Balancing;
import com.xinbo.fundstransfer.report.store.StoreHandler;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class OutletHandler {
	@Autowired
	AccountRepository accountDao;
	@Autowired
	@Lazy
	AccountService accSer;
	@Autowired
	StoreHandler storeHandler;
	@PersistenceContext
	private EntityManager entityManager;
	@Lazy
	@Autowired
	RedisService redisService;

	private static List<ReportCheck> CHECK_LIST = Collections.synchronizedList(new ArrayList<ReportCheck>());

	public Set<Integer> alarm4AccountingInOut(StringRedisTemplate template, boolean isOutward) {
		Set<Integer> result = new HashSet<>();
		if (isOutward) {
			template.boundHashOps(RedisKeys.SYS_BAL_OUT).entries().forEach((k, v) -> {
				Integer id = Integer.valueOf((String) k);
				AccountBaseInfo base = accSer.getFromCacheById(id);
				if (Objects.nonNull(base) && (!Objects.equals(base.getType(), AccountType.InBank.getTypeId())
						&& !Objects.equals(base.getType(), AccountType.ReserveBank.getTypeId())
						&& !(Objects.equals(base.getType(), AccountType.OutBank.getTypeId())
								&& Objects.nonNull(base.getHolder())))) {
					result.add(Integer.valueOf((String) k));
				}
			});
		} else {
			template.boundHashOps(RedisKeys.SYS_BAL_IN).entries().forEach((k, v) -> {
				Integer id = Integer.valueOf((String) k);
				AccountBaseInfo base = accSer.getFromCacheById(id);
				if (Objects.nonNull(base) && (!Objects.equals(base.getType(), AccountType.InBank.getTypeId())
						&& !Objects.equals(base.getType(), AccountType.ReserveBank.getTypeId())
						&& !(Objects.equals(base.getType(), AccountType.OutBank.getTypeId())
								&& Objects.nonNull(base.getHolder())))) {
					result.add(Integer.valueOf((String) k));
				}
			});
		}
		return result;
	}

	public Map<Integer, Balancing> balanceing(StringRedisTemplate template, List<Integer> idList) {
		Map<Integer, Balancing> result = new HashMap<>();
		if (CollectionUtils.isEmpty(idList))
			return result;
		List<Object> ids = idList.stream().filter(Objects::nonNull).map(String::valueOf).collect(Collectors.toList());
		List<Object> vals = template.boundHashOps(RedisKeys.SYS_BAL_BALANCING).multiGet(ids);
		if (CollectionUtils.isEmpty(vals))
			return result;
		vals.forEach(hv -> {
			if (Objects.nonNull(hv)) {
				Balancing balancing = new Balancing(hv.toString());
				result.put(balancing.getTarget(), balancing);
			}
		});
		return result;
	}

	public void pushReprotCheck(ReportCheck check) {
		if (Objects.isNull(check) || Objects.isNull(check.getTarget()))
			return;
		CHECK_LIST.add(check);
	}

	public boolean computeBalancing(StringRedisTemplate template) {
		if (Objects.isNull(template)) {
			CHECK_LIST.clear();
			return false;
		}
		if (CollectionUtils.isEmpty(CHECK_LIST))
			return false;
		Map<String, String> result = new HashMap<>();
		Map<Integer, Balancing> accMap = findAcc(CHECK_LIST);
		long now = System.currentTimeMillis();
		for (ReportCheck check : CHECK_LIST) {
			Balancing bal = accMap.get(check.getTarget());
			if (Objects.isNull(bal))
				continue;
			List<SysBalTrans> tsList = check.getTransInAll().stream()
					.filter(p -> SysBalTrans.SYS_REFUND != p.getSys() && !p.ackTo()).collect(Collectors.toList());
			// 汇款方正在转的金额
			// 1.2.5分钟内，工具取走没有确认的订单
			// 2.2.5分钟内，工具完成上报但是没有确认的订单
			BigDecimal transferring = tsList.stream()
					.filter(p -> !p.ackFr() && (p.getAckTm() == 0 && now - p.getGetTm() < 150000
							|| p.getAckTm() > 0 && now - p.getAckTm() < 150000))
					.map(SysBalTrans::getAmt).reduce(BigDecimal.ZERO, BigDecimal::add);
			// 汇款方已确认转出的金额
			BigDecimal transferred = tsList.stream().filter(SysBalTrans::ackFr).map(SysBalTrans::getAmt)
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			BigDecimal[] checked = findCheckedBalance(bal.getTarget());
			result.put(String.valueOf(bal.getTarget()), Balancing.genMsg(check.getTarget(), now, bal.getDbSysBal(),
					bal.getDbBankBal(), checked[0], checked[1], transferring, transferred));
		}
		if (result.size() > 0)
			template.boundHashOps(RedisKeys.SYS_BAL_BALANCING).putAll(result);
		CHECK_LIST.clear();
		return true;
	}

	private BigDecimal[] findCheckedBalance(Integer id) {
		List<BizSysLog> sysList = storeHandler.findSysLogFromCache(id);
		if (CollectionUtils.isEmpty(sysList))
			return new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO };
		BigDecimal checkedSysBal = BigDecimal.ZERO, checkedBankBal = BigDecimal.ZERO;
		int l = sysList.size();
		for (int index = 0; index < l; index++) {
			BizSysLog item = sysList.get(index);
			if (Objects.nonNull(item) && Objects.equals(item.getStatus(), SysLogStatus.Valid.getStatusId())) {
				checkedSysBal = item.getBalance();
				checkedBankBal = item.getBankBalance();
				break;
			}
		}
		return new BigDecimal[] { checkedSysBal, checkedBankBal };
	}

	private Map<Integer, Balancing> findAcc(List<ReportCheck> checkList) {
		Map<Integer, Balancing> result = new HashMap<>();
		List<Integer> idList = checkList.stream()
				.filter(p -> Objects.nonNull(p) && Objects.nonNull(p.getTarget()) && p.getTarget() > 0)
				.map(ReportCheck::getTarget).collect(Collectors.toList());
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Tuple> query = cb.createTupleQuery();
		Root<BizAccount> root = query.from(BizAccount.class);
		Path<Long> idPath = root.get("id");
		Path<BigDecimal> balancePath = root.get("balance");
		Path<BigDecimal> bankBalancePath = root.get("bankBalance");
		query.multiselect(idPath, balancePath, bankBalancePath);
		query.where(idPath.in(idList));
		TypedQuery<Tuple> q = entityManager.createQuery(query);
		for (Tuple tuple : q.getResultList()) {
			Integer id = tuple.get(0, Integer.class);
			result.put(id, new Balancing(id, tuple.get(1, BigDecimal.class), tuple.get(2, BigDecimal.class)));
		}
		return result;
	}
}
