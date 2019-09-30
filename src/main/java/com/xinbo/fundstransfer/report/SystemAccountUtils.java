package com.xinbo.fundstransfer.report;

import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.enums.AccountFlag;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.enums.InBankSubType;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.report.up.ReportYSF;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Stream;

public class SystemAccountUtils {

	public static final long ONE_DAY_MILLIS = 86400000;
	public static final long SEVEN_HOURS_MILLIS = 25200000;
	public static final long THIRTY_MINUTES_MILLIS = 1800000;
	private static long CURR_DATE_START_MILLIS = 0, CURR_DATE_END_MILLIS = 0;
	private static int CURR_MONTH = 0, CURR_DAY_OF_MONTH = 0;
	private static final SnowFlake SNOW_FLAKE = new SnowFlake();

	public static boolean ysf(BizAccount acc) {
		return Objects.nonNull(acc) && ysf(acc.getFlag(), acc.getSubType(), acc.getType());
	}

	public static boolean ysf(AccountBaseInfo base) {
		return Objects.nonNull(base) && ysf(base.getFlag(), base.getSubType(), base.getType());
	}

	public static String generateId() {
		return SNOW_FLAKE.nextId("T");
	}

	public static int currentMonth() {
		computeTimeParamsIfNeed();
		return CURR_MONTH;
	}

	public static int currentDayOfMonth() {
		computeTimeParamsIfNeed();
		return CURR_DAY_OF_MONTH;
	}

	public static long currentDayStartMillis() {
		computeTimeParamsIfNeed();
		return CURR_DATE_START_MILLIS;
	}

	public static List<ReportYSF> combinate(BigDecimal diff, List<ReportYSF> ysfList) {
		if (CollectionUtils.isEmpty(ysfList) || ysfList.size() > 12)
			return new ArrayList<>();
		List<ReportYSF[]> combination = combinateRandom(ysfList.toArray(new ReportYSF[ysfList.size()]));
		combination = combinateReduce(diff, combination);
		List<ReportYSF> ret = (combination.size() == 1) ? Arrays.asList(combination.get(0)) : new ArrayList<>();
		ret.sort((o1, o2) -> SysBalUtils.oneZeroMinus(o1.getFlogId() - o2.getFlogId()));
		return ret;
	}

	private static List<ReportYSF[]> combinateReduce(BigDecimal diff, List<ReportYSF[]> combination) {
		List<ReportYSF[]> result = new ArrayList<>();
		for (ReportYSF[] ysfArr : combination) {
			BigDecimal sum = Stream.of(ysfArr).map(ReportYSF::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
			if (diff.compareTo(sum) == 0)
				result.add(ysfArr);
		}
		return result;
	}

	private static List<ReportYSF[]> combinateRandom(ReportYSF[] source) {
		List<ReportYSF[]> result = new ArrayList<>();
		if (source.length == 1) {
			result.add(source);
		} else {
			ReportYSF[] psource = new ReportYSF[source.length - 1];
			for (int i = 0; i < psource.length; i++)
				psource[i] = source[i];
			result = combinateRandom(psource);
			int len = result.size();
			result.add((new ReportYSF[] { source[source.length - 1] }));
			for (int i = 0; i < len; i++) {
				ReportYSF[] tmp = new ReportYSF[result.get(i).length + 1];
				for (int j = 0; j < tmp.length - 1; j++)
					tmp[j] = result.get(i)[j];
				tmp[tmp.length - 1] = source[source.length - 1];
				result.add(tmp);
			}
		}
		return result;
	}

	private static void computeTimeParamsIfNeed() {
		long curr = System.currentTimeMillis();
		if (curr >= CURR_DATE_START_MILLIS && curr <= CURR_DATE_END_MILLIS)
			return;
		Calendar calendar = Calendar.getInstance();
		CURR_MONTH = calendar.get(Calendar.MONTH);
		CURR_DAY_OF_MONTH = calendar.get(Calendar.DAY_OF_MONTH);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		CURR_DATE_START_MILLIS = calendar.getTime().getTime();
		CURR_DATE_END_MILLIS = CURR_DATE_START_MILLIS + ONE_DAY_MILLIS;
	}

	private static boolean ysf(Integer accountFlag, Integer accountSubType, Integer accountType) {
		return Objects.equals(accountFlag, AccountFlag.REFUND.getTypeId())
				&& (Objects.equals(accountSubType, InBankSubType.IN_BANK_YSF.getSubType())
						|| Objects.equals(accountSubType, InBankSubType.IN_BANK_YSF_MIX.getSubType()))
				&& Objects.equals(accountType, AccountType.InBank.getTypeId());
	}
}
