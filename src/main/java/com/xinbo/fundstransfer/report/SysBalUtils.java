package com.xinbo.fundstransfer.report;

import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.enums.BankLogStatus;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.SysBalTrans;
import com.xinbo.fundstransfer.domain.pojo.TransferEntity;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.Objects;

public class SysBalUtils {

	public static String SEPARATOR = "#########";

	public static String ERROR_MSG = "无法识别开户行,";

	public static boolean failTaggedWord(String target) {
		return StringUtils.isNotBlank(target) && ERROR_MSG.contains(target.trim());
	}

	/**
	 * 获取字符串最后三位
	 */
	public static String last3letters(String target) {
		target = StringUtils.trimToEmpty(target);
		int l = target.length();
		return l < 3 ? StringUtils.EMPTY : target.substring(l - 3, l);
	}

	/**
	 * 获取字符串最后二位
	 */
	public static String last2letters(String target) {
		target = StringUtils.trimToEmpty(target);
		int l = target.length();
		return l < 2 ? StringUtils.EMPTY : target.substring(l - 2, l);
	}

	/**
	 * 获取字符串最后一位
	 */
	public static String last1letters(String target) {
		target = StringUtils.trimToEmpty(target);
		int l = target.length();
		return l < 1 ? StringUtils.EMPTY : target.substring(l - 1, l);
	}

	/**
	 * 获取转账类型
	 */
	public static int taskType(TransferEntity transfer) {
		if (Objects.isNull(transfer.getTaskId()) || transfer.getTaskId() == 0)
			return SysBalTrans.TASK_TYPE_INNER;
		if (StringUtils.isNotBlank(transfer.getRemark()) && transfer.getRemark().contains("返利任务"))
			return SysBalTrans.TASK_TYPE_OUTREBATE;
		return SysBalTrans.TASK_TYPE_OUTMEMEBER;
	}

	/**
	 * get fromAccountId from {@link TransferEntity}
	 */
	public static int frId(TransferEntity transfer) {
		return Objects.nonNull(transfer.getFromAccountId()) ? transfer.getFromAccountId() : 0;
	}

	/**
	 * get toAccountId from {@link TransferEntity}
	 */
	public static int toId(TransferEntity transfer) {
		return Objects.nonNull(transfer.getToAccountId()) ? transfer.getToAccountId() : 0;
	}

	/**
	 * get taskId from {@link TransferEntity}
	 */
	public static long taskId(TransferEntity trasnfer) {
		return Objects.isNull(trasnfer.getTaskId()) ? 0 : trasnfer.getTaskId();
	}

	/**
	 * get transaction amount from {@link TransferEntity}
	 */
	public static BigDecimal transAmt(TransferEntity trasnfer) {
		return new BigDecimal(trasnfer.getAmount()).setScale(2, BigDecimal.ROUND_HALF_UP).abs();
	}

	/**
	 * get balance from {@link TransferEntity} before transaction
	 */
	public static BigDecimal beforeBal(TransferEntity trasnfer) {
		try {
			if (Objects.nonNull(trasnfer) && StringUtils.isNotBlank(trasnfer.getRemark())
					&& trasnfer.getRemark().startsWith("#")) {
				String t = trasnfer.getRemark().substring(1).split("#")[0];
				if (StringUtils.isNotBlank(t))
					return new BigDecimal(t).setScale(2, BigDecimal.ROUND_HALF_UP);
			}
		} catch (Exception e) {
			return BigDecimal.ZERO;
		}
		return BigDecimal.ZERO;
	}

	public static int oneZeroMinus(long result) {
		return result > 0 ? 1 : (result < 0 ? -1 : 0);
	}

	public static boolean radix(BigDecimal target) {
		return Objects.nonNull(target) && target.floatValue() > target.intValue();
	}

	public static boolean fee(BizBankLog lg) {
		if (Objects.equals(BankLogStatus.Fee.getStatus(), lg.getStatus()))
			return true;
		BigDecimal tmp = radix2(lg.getAmount());
		if (tmp.compareTo(BigDecimal.ZERO) >= 0 | (tmp = tmp.abs()).compareTo(BigDecimal.TEN) >= 0)
			return false;
		int l = StringUtils.trimToEmpty(lg.getToAccountOwner()).length();
		if (l == 2 || l == 3) {// 这个时候及有可能是带有对方姓名，非手续费
			return false;
		}
		return tmp.divideAndRemainder(new BigDecimal("0.5"))[1].compareTo(BigDecimal.ZERO) == 0;
	}

	/**
	 * 手续费 是否被0.5整除 且 金额小于0
	 */
	public static boolean fee050(BigDecimal fee) {
		if (Objects.isNull(fee) || fee.compareTo(BigDecimal.ZERO) == 0)
			return true;
		fee = radix2(fee);
		return fee.abs().compareTo(BigDecimal.ONE) >= 0 && fee.compareTo(BigDecimal.ZERO) < 0
				&& fee.divideAndRemainder(new BigDecimal("0.5"))[1].compareTo(BigDecimal.ZERO) == 0;
	}

	public static boolean radix(SysBalTrans ts) {
		return SysBalTrans.TASK_TYPE_INNER == ts.getTaskType() && ts.getAmt().floatValue() > ts.getAmt().intValue();
	}

	public static BigDecimal radix2(BigDecimal target) {
		return Objects.isNull(target) ? BigDecimal.ZERO : target.setScale(2, BigDecimal.ROUND_HALF_UP);
	}

	/**
	 * get balance from {@link TransferEntity} after transaction
	 */
	public static BigDecimal afterBal(TransferEntity trasnfer) {
		if (Objects.nonNull(trasnfer) && Objects.nonNull(trasnfer.getBalance()))
			return new BigDecimal(trasnfer.getBalance()).setScale(2, BigDecimal.ROUND_HALF_UP);
		return BigDecimal.ZERO;
	}

	public static boolean noneExpire(SysBalTrans t, long expireTm) {
		return Objects.nonNull(t) && t.getGetTm() > 0 && expireTm > 0
				&& (System.currentTimeMillis() - expireTm <= t.getGetTm());
	}

	public static boolean valid(SysBalTrans t, long validPeriods) {
		return Objects.nonNull(t) && t.getGetTm() > 0 && validPeriods > 0
				&& (System.currentTimeMillis() - validPeriods >= t.getGetTm());
	}

	public static String uuid(SysBalTrans ts) {
		// frId-toId-taskId-getTm-amt
		return String.format("%d-%d-%d-%d-%s", ts.getFrId(), ts.getToId(), ts.getTaskId(), ts.getGetTm(),
				ts.getAmt().toString());
	}

	public static long expireRegRobot() {
		return 30;
	}

	public static long expireRegMan() {
		return 360;
	}

	public static long expireRegTh3() {
		return 360;
	}

	public static long expireAck(long registTm) {
		if (registTm <= 0)
			return 1L;
		long ret = 360 - (System.currentTimeMillis() - registTm) / 60000;
		return ret <= 0 ? 1 : ret;
	}

	public static long expireEnd() {
		return 5;
	}

	public static boolean equal(SysBalTrans arg0, SysBalTrans arg1) {
		if (Objects.isNull(arg0) || Objects.isNull(arg1))
			return false;
		if (arg0.getTaskId() > 0 && arg0.getTaskType() == arg1.getTaskType() && arg0.getTaskId() == arg1.getTaskId())
			return true;
		return radix2(arg0.getAmt()).compareTo(radix2(arg1.getAmt())) == 0
				&& (arg0.getFrId() != 0 && arg0.getFrId() == arg1.getFrId()
						|| arg0.getFrId() == 0 && (Objects.equals(arg0.getFrAcc3Last(), arg1.getFrAcc3Last())
								|| Objects.equals(arg0.getFrOwn2Last(), arg1.getFrOwn2Last())))
				&& (arg0.getToId() != 0 && arg0.getToId() == arg1.getToId()
						|| arg0.getToId() == 0 && (Objects.equals(arg0.getToAcc3Last(), arg1.getToAcc3Last())
								|| Objects.equals(arg0.getToOwn2Last(), arg1.getToOwn2Last())));
	}

	public static boolean refund(BizBankLog arg0, BizBankLog arg1, BigDecimal fee) {
		if (Objects.isNull(arg0) || Objects.isNull(arg1))
			return false;
		BigDecimal amt0 = SysBalUtils.radix2(arg0.getAmount()), amt1 = SysBalUtils.radix2(arg1.getAmount());
		// 注意：此处不能使用 {@code amt0.abs().compareTo(amt1.abs())!=0} 作为判定条件
		if (amt0.add(amt1).compareTo(BigDecimal.ZERO) != 0)
			return false;
		fee = SysBalUtils.radix2(fee);
		// 如果：手续费>=0,不满足条件，直接返回
		if (fee.compareTo(BigDecimal.ZERO) > 0)
			return false;
		BigDecimal bal0 = SysBalUtils.radix2(arg0.getBalance()), bal1 = SysBalUtils.radix2(arg1.getBalance());
		String arg0Own2 = SysBalUtils.last2letters(arg0.getToAccountOwner());
		String arg1Own2 = SysBalUtils.last2letters(arg1.getToAccountOwner());
		String arg0Acc3 = SysBalUtils.last3letters(arg0.getToAccount());
		String arg1Acc3 = SysBalUtils.last3letters(arg1.getToAccount());
		if (bal0.compareTo(BigDecimal.ZERO) != 0 && bal1.compareTo(BigDecimal.ZERO) != 0) {
			if (bal0.abs().subtract(bal1.abs()).abs().compareTo(amt0.abs().add(fee.abs())) != 0)
				return false;
			if (StringUtils.isNotBlank(arg0Own2) && Objects.equals(arg0Own2, arg1Own2))
				return true;
			if (StringUtils.isNotBlank(arg0Acc3) && Objects.equals(arg0Acc3, arg1Acc3))
				return true;
		} else if (bal0.compareTo(BigDecimal.ZERO) == 0 || bal1.compareTo(BigDecimal.ZERO) == 0) {
			if (StringUtils.isNotBlank(arg0Own2) && Objects.equals(arg0Own2, arg1Own2))
				return true;
			if (StringUtils.isNotBlank(arg0Acc3) && Objects.equals(arg0Acc3, arg1Acc3))
				return true;
		}
		return false;
	}

	public static String autoRemark4Refund(int flag, long lgId) {
		return autoRemark("回冲" + flag + "流水号" + lgId);
	}

	public static String autoRemark(String remark) {
		return "自动-" + remark;
	}

	/**
	 * return {@code true } recreate new one; {@code false} need to deal by
	 * manual
	 */
	public static boolean transOut(AccountBaseInfo base, SysBalTrans ts) {
		if (Objects.isNull(base) || Objects.isNull(ts) || ts.getGetTm() == 0 || !ts.getInvalidTransferTime())
			return true;
		long tmSt = SystemAccountUtils.currentDayStartMillis();
		long tmEd = tmSt + 86400000;
		long tmBe = 60000 * 5, tmAf = 60000 * 5;
		String bankType = StringUtils.trimToEmpty(base.getBankType());
		if ("华夏银行".contains(bankType)) {// 21:30:00-24:00:00->21:26:59-00:04:59
			tmBe = 60000 * 155;
		} else if ("建设银行".contains(bankType)) {// 22:30:00-24:00:00
			tmBe = 60000 * 95;
		} else if ("工商银行".contains(bankType)) {// 23:30:00-24:00:00->23:26:59-00:04:59
			tmBe = 60000 * 35;
		} else if ("内蒙古银行".contains(bankType)) {// 20:30:00-24:00:00->20:26:59-00:04:59
			tmBe = 60000 * 215;
		} else if ("中国银行".contains(bankType)) {// 22:00:00-03:00:00->21:56:59-03:04:59
			tmBe = 60000 * 125;
		} else if ("平安银行".contains(bankType)) {// 23:30:00->01:40:00
			tmBe = 60000 * 50;
			tmAf = 60000 * 60;
		}
		long getTm = ts.getGetTm();
		if (getTm >= (tmSt - tmBe) && getTm <= (tmSt + tmAf) || getTm >= (tmEd - tmBe))
			return false;
		long tmAk = ts.getAckTm();
		if (tmAk != 0 && (tmAk >= (tmSt - tmBe) && tmAk <= (tmSt + tmAf) || tmAk >= (tmEd - tmBe))) {
			return false;
		}
		return true;
	}

	/**
	 * 计算该任务耗时多少，如果该银行在某个时间段，不能产生流水，耗时应该不计算该时间段
	 */
	public static long consumeMillis(String bankType, SysBalTrans ts) {
		if (Objects.isNull(ts) || ts.getAckTm() == 0)
			return 0;
		long tmSt = SystemAccountUtils.currentDayStartMillis();// 当日开始时间
		long tmEd = tmSt + 86400000;// 当日结束时间
		long tarSt, tarEd, now = System.currentTimeMillis();
		long diff = now - tmSt;
		if ("民生银行".contains(bankType)) {// 0:00:00-02:00:00->~-02:04:59
			if (diff < 43200000) {// 中午12点分解线
				tarSt = tmSt - 300000;// 23:55:00
				tarEd = tmSt + 7500000;// 02:05:59
			} else {
				tarSt = tmEd - 300000;// 23:55:00
				tarEd = tmEd; // 00:00:00
			}
		} else if ("交通银行".contains(bankType)) {// 00:00:00-01:00:00->~-01:04:59
			if (diff < 43200000) {// 中午12点分解线
				tarSt = tmSt - 300000;// 23:55:00
				tarEd = tmSt + 3900000;// 01:05:00
			} else {
				tarSt = tmEd - 300000;// 23:55:00
				tarEd = tmEd; // 00:00:00
			}
		} else if ("华夏银行".contains(bankType)) {// 21:30:00-24:00:00->21:26:59-00:04:59
			if (diff < 43200000) {// 中午12点分解线
				tarSt = tmSt - 9300000;// 21:25:00
				tarEd = tmSt + 300000;// 00:05:00
			} else {
				tarSt = tmEd - 300000;// 23:55:00
				tarEd = tmEd; // 00:00:00
			}
		} else if ("建设银行".contains(bankType)) {// 22:00:00-24:00:00->21:56:59-00:04:59
			if (diff < 43200000) {// 中午12点分解线
				tarSt = tmSt - 7500000;// 21:56:00
				tarEd = tmSt + 300000;// 00:05:00
			} else {
				tarSt = tmEd - 300000;// 23:55:00
				tarEd = tmEd; // 00:00:00
			}
		} else if ("工商银行".contains(bankType)) {// 23:30:00-24:00:00->23:26:59-00:04:59
			if (diff < 43200000) {// 中午12点分解线
				tarSt = tmSt - 2100000;// 23:26:00
				tarEd = tmSt + 300000;// 00:05:59
			} else {
				tarSt = tmEd - 300000;// 23:55:00
				tarEd = tmEd; // 00:00:00
			}
		} else if ("内蒙古银行".contains(bankType)) {// 20:30:00-24:00:00->20:26:59-00:04:59
			if (diff < 43200000) {// 中午12点分解线
				tarSt = tmSt - 12900000;// 20:26:00
				tarEd = tmSt + 300000;// 00:05:59
			} else {
				tarSt = tmEd - 300000;// 23:55:00
				tarEd = tmEd; // 00:00:00
			}
		} else if ("中国银行".contains(bankType)) {// 22:00:00-03:00:00->21:56:59-03:04:59
			if (diff < 43200000) {// 中午12点分解线
				tarSt = tmSt - 7500000;// 21:56:00
				tarEd = tmSt + 11100000;// 03:05:00
			} else {
				tarSt = tmEd - 7500000;// (21:56:00)
				tarEd = tmEd; // (00:00:00)
			}
		} else if ("北京农商".contains(bankType)) {// :00:00-00:30:00->
			if (diff < 43200000) {// 中午12点分解线
				tarSt = tmSt - 300000;// 23:56:00
				tarEd = tmSt + 2100000;// 00:35:00
			} else {
				tarSt = tmEd - 300000;// 23:56:00
				tarEd = tmEd; // 00:00:00
			}
		} else if ("农业银行".contains(bankType)) {// 下午4点、晚上12点整，银行维护
			// (12:00:00-->20:00:00)
			if (diff >= 43200000 && diff <= 72000000) {// 下发四点
				tarSt = tmSt + 57600000 - 300000;// 15:56:00
				tarEd = tmSt + 57600000 + 300000; // 16:05:59
			} else if (diff < 43200000) {// 中午12点分解线
				tarSt = tmSt - 300000;// 23:56:00
				tarEd = tmSt + 300000;// 00:05:59
			} else {
				tarSt = tmEd - 300000;// 23:56:00
				tarEd = tmEd; // 00:00:00
			}
		} else {
			if (diff < 43200000) {// 中午12点分解线
				tarSt = tmSt - 300000;// 23:56:00
				tarEd = tmSt + 300000;// 00:05:59
			} else {
				tarSt = tmEd - 300000;// 21:56:00
				tarEd = tmEd; // 00:00:00
			}
		}
		long occurTm = ts.getAckTm(), ret;
		if (occurTm < tarSt) {
			if (now < tarSt) {
				ret = now - occurTm;
			} else if (tarSt <= now && now <= tarEd) {
				ret = tarSt - occurTm;
			} else {
				ret = (tarSt - occurTm) + (now - tarEd);
			}
		} else if (tarSt <= occurTm && occurTm <= tarEd) {
			ret = (tarSt <= now && now <= tarEd) ? 0 : (now - tarEd);
		} else {
			ret = now - occurTm;
		}
		return ret;
	}
}
