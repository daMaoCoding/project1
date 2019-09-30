package com.xinbo.fundstransfer.report.up;

import com.google.common.util.concurrent.Striped;
import com.xinbo.fundstransfer.RefundUtil;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.enums.BankLogStatus;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalTrans;
import com.xinbo.fundstransfer.report.SysBalUtils;
import org.apache.shiro.util.CollectionUtils;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

public class ReportCheck {
	private Integer target;
	private AccountBaseInfo base;

	private boolean checkAny = false;
	private boolean errorAny = true;
	private boolean checkLast = true;

	private int count = 0;
	private List<BizBankLog> errorBankLog = new ArrayList<>();
	private List<SysBalTrans> trans = new ArrayList<>();
	private List<SysBalTrans> transOutAll = null;
	private List<SysBalTrans> transInAll = null;

	private ReportCheckNoneInit tsAllOutAction = null;
	private ReportCheckNoneInit tsAllInAction = null;

	private static final Map<String, SysBalTrans> ORI_DATA = new ConcurrentHashMap<>();
	private static final Map<Integer, List<SysBalTrans>> ORI_DATA_OUT = new ConcurrentHashMap<>();
	private static final Map<Integer, List<SysBalTrans>> ORI_DATA_IN = new ConcurrentHashMap<>();
	private static final Striped<Lock> STRIPED = Striped.lazyWeakLock(1024);

	private static Map<String, String> REPORT_YSF = null;

	ReportCheck(AccountBaseInfo base, ReportCheckNoneInit tsAllOutAction, ReportCheckNoneInit tsAllInAction) {
		this.base = base;
		this.target = base.getId();
		this.tsAllOutAction = tsAllOutAction;
		this.tsAllInAction = tsAllInAction;
	}

	public static void clear(StringRedisTemplate template) {
		ORI_DATA.clear();
		ORI_DATA_OUT.clear();
		ORI_DATA_IN.clear();
		long curr = System.currentTimeMillis();
		if (Objects.nonNull(REPORT_YSF)) {
			Set<String> expireKey = new HashSet<>();
			for (Map.Entry<String, String> entity : REPORT_YSF.entrySet()) {
				if (curr - new ReportYSF(entity.getValue()).getCrawlTm() > 3600000)
					expireKey.add(entity.getKey());
			}
			for (String key : expireKey)
				REPORT_YSF.remove(key);
			template.delete(RedisKeys.SYS_BAL_YSF);
			if (REPORT_YSF.size() > 0)
				template.boundHashOps(RedisKeys.SYS_BAL_YSF).putAll(REPORT_YSF);
		}
		REPORT_YSF = null;
	}

	public boolean error(BizBankLog lg) {
		if (Objects.isNull(lg) || lg.getAmount().compareTo(BigDecimal.ZERO) > 0 || SysBalUtils.fee(lg)
				|| Objects.equals(lg.getStatus(), BankLogStatus.Interest.getStatus())
				|| Objects.equals(lg.getStatus(), BankLogStatus.Fee.getStatus())
				|| Objects.equals(lg.getStatus(), BankLogStatus.Refunded.getStatus())
				|| Objects.equals(lg.getStatus(), BankLogStatus.Refunded.getStatus()))
			return false;
		if (RefundUtil.refund(base.getBankType(), lg.getSummary()))
			return false;
		if (Objects.equals(base.getOwner(), lg.getToAccountOwner()))
			return false;
		if (RefundUtil.refund(base.getBankType(), lg.getToAccountOwner()))
			return false;
		if (Objects.nonNull(lg.getTaskId()) && lg.getTaskId() != 0)
			return false;
		if (lg.getAmount().abs().intValue() <= 10)
			return false;
		this.errorBankLog.add(lg);
		return true;
	}

	public void init(BigDecimal[] bs, SysBalTrans ts) {
		if (Objects.isNull(bs) || bs.length < 2 || Objects.isNull(bs[0]) || Objects.isNull(bs[1])
				|| bs[0].compareTo(BigDecimal.ZERO) == 0 || bs[1].compareTo(BigDecimal.ZERO) == 0) {
			return;
		}
		this.count = this.count + 1;
		this.checkLast = bs[0].compareTo(bs[1]) == 0;
		this.errorAny = this.errorAny && this.checkLast;
		this.checkAny = this.checkAny || this.checkLast;
		if (Objects.nonNull(ts) && ts.getGetTm() > 0 && bs[0].compareTo(bs[1]) == 0
				&& Objects.equals(target, ts.getFrId())) {
			trans.add(ts);
		}
	}

	public AccountBaseInfo getBase() {
		return base;
	}

	public Integer getTarget() {
		return target;
	}

	public void setTarget(Integer target) {
		this.target = target;
	}

	public List<SysBalTrans> getTrans() {
		return trans;
	}

	public int getCount() {
		return this.count;
	}

	public List<BizBankLog> getErrorLog() {
		return this.errorBankLog;
	}

	public boolean getCheckAny() {
		return this.checkAny;
	}

	public boolean getCheckLast() {
		return this.checkLast;
	}

	public boolean getErrorAny() {
		return this.errorAny;
	}

	public void reRegist(SysBalTrans ts) {
		if (Objects.isNull(ts))
			return;
		String uuid = SysBalUtils.uuid(ts);
		Lock lock = STRIPED.get(uuid);
		try {
			lock.lock();
			if (Objects.isNull(ORI_DATA.get(uuid))) {
				ORI_DATA.put(uuid, ts);
				List<SysBalTrans> outList, inList;
				if (ts.getFrId() != 0 && Objects.nonNull((outList = ORI_DATA_OUT.get(ts.getFrId()))))
					outList.add(ts);
				if (ts.getToId() != 0 && Objects.nonNull((inList = ORI_DATA_IN.get(ts.getToId()))))
					inList.add(ts);
			}
		} finally {
			lock.unlock();
		}
	}

	public void setYSF(StringRedisTemplate template, BizBankLog lg) {
		getYSF(template, lg.getId());
		REPORT_YSF.put(String.valueOf(lg.getId()), ReportYSF.genMsg(lg));
	}

	public void movYSF(StringRedisTemplate template, BizBankLog lg) {
		getYSF(template, lg.getId());
		REPORT_YSF.remove(String.valueOf(lg.getId()));
	}

	public List<ReportYSF> getYSF(StringRedisTemplate template, AccountBaseInfo base) {
		if (Objects.isNull(template) || Objects.isNull(base))
			return Collections.EMPTY_LIST;
		getYSF(template, 0L);
		return REPORT_YSF.values().stream().map(ReportYSF::new)
				.filter(p -> Objects.equals(base.getId(), p.getFromAccount())).collect(Collectors.toList());
	}

	public ReportYSF getYSF(StringRedisTemplate template, long logId) {
		if (REPORT_YSF != null)
			return new ReportYSF(REPORT_YSF.get(String.valueOf(logId)));
		Lock lock = STRIPED.get("ReportYSFIncomeStatementLock");
		try {
			lock.lock();
			if (REPORT_YSF != null)
				return new ReportYSF(REPORT_YSF.get(String.valueOf(logId)));
			Map<Object, Object> dataMap = template.boundHashOps(RedisKeys.SYS_BAL_YSF).entries();
			REPORT_YSF = new ConcurrentHashMap<>();
			if (Objects.isNull(dataMap) || dataMap.size() == 0)
				return new ReportYSF("");
			dataMap.forEach((hk, hv) -> REPORT_YSF.put(hk.toString(), hv.toString()));
			return new ReportYSF(REPORT_YSF.get(String.valueOf(logId)));
		} finally {
			lock.unlock();
		}
	}

	public List<SysBalTrans> getTransOutAll() {
		if (Objects.nonNull(this.transOutAll)) {
			return this.transOutAll;
		}
		if (Objects.isNull(tsAllOutAction)) {
			return new ArrayList<>();
		}
		List<SysBalTrans> tsList = load(target, tsAllOutAction);
		if (Objects.isNull(tsList)) {
			this.transOutAll = Collections.synchronizedList(new ArrayList<>());
		} else {
			this.transOutAll = Collections.synchronizedList(tsList);
		}
		ORI_DATA_OUT.put(target, this.transOutAll);
		return this.transOutAll;
	}

	public List<SysBalTrans> getTransInAll() {
		if (Objects.nonNull(this.transInAll)) {
			return this.transInAll;
		}
		if (Objects.isNull(tsAllInAction)) {
			return new ArrayList<>();
		}
		List<SysBalTrans> tsList = load(target, tsAllInAction);
		if (Objects.isNull(tsList)) {
			this.transInAll = Collections.synchronizedList(new ArrayList<>());
		} else {
			this.transInAll = Collections.synchronizedList(tsList);
		}
		ORI_DATA_IN.put(target, this.transInAll);
		return this.transInAll;
	}

	private List<SysBalTrans> load(int target, ReportCheckNoneInit noneInit) {
		List<SysBalTrans> result = new ArrayList<>();
		if (Objects.isNull(noneInit))
			return result;
		List<SysBalTrans> tsList = noneInit.ifAbsent(target);
		if (CollectionUtils.isEmpty(tsList))
			return result;
		tsList.forEach(p -> {
			String uuid = SysBalUtils.uuid(p);
			Lock lock = STRIPED.get(uuid);
			try {
				lock.lock();
				SysBalTrans ts = ORI_DATA.get(uuid);
				if (Objects.isNull(ts)) {
					ORI_DATA.put(uuid, p);
					result.add(p);
				} else {
					result.add(ts);
				}
			} finally {
				lock.unlock();
			}

		});
		return result;
	}
}
