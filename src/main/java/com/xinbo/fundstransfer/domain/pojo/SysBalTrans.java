package com.xinbo.fundstransfer.domain.pojo;

import java.math.BigDecimal;
import java.util.Objects;

import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import org.apache.commons.lang3.StringUtils;

import com.xinbo.fundstransfer.component.redis.RedisKeys;

public class SysBalTrans {

	public static final int SYS_NONE = 0;
	public static final int SYS_SUB = 1;
	public static final int SYS_REFUND = 4;

	public static final int ACK_NONE = 0;
	public static final int ACK_ACK = 1;
	public static final int ACK_CANCEL = 2;

	public static final int TASK_TYPE_INNER = 0;
	public static final int TASK_TYPE_OUTMEMEBER = 1;
	public static final int TASK_TYPE_OUTREBATE = 2;
	/* 入款 */
	public static final int TASK_TYPE_INCOME = 3;
	/* 入款第三方下发 */
	public static final int TASK_TYPE_THIRD_TRANSFER = 4;

	public static final int REGIST_WAY_AUTO = 0;
	public static final int REGIST_WAY_MAN = 1;
	public static final int REGIST_WAY_TH3 = 2;
	public static final int REGIST_WAY_RE = 3;
	public static final int REGIST_WAY_MAN_MGR = 4;

	private int sys;

	private int frId;
	private int toId;
	private BigDecimal amt;
	private String frAcc3Last;
	private String toAcc3Last;

	private long getTm = 0;
	private long ackTm = 0;

	private int ackByCurrTrans = 0;
	private int ackByNextTrans = 0;
	private int ackByCurrBal = 0;
	private int ackByCurrFlow = 0;
	private int ackByCurrResult1 = 0;
	private int ackByOppBal = 0;
	private int ackByOppFlow = 0;
	private long taskId = 0;
	private int taskType = 0;
	private BigDecimal before = BigDecimal.ZERO;
	private BigDecimal after = BigDecimal.ZERO;
	private BigDecimal beopp = BigDecimal.ZERO;

	private String frOwn2Last;
	private String toOwn2Last;

	private long sysLgId = 0;

	private long oppSysLgId = 0;

	private long bankLgId = 0;

	private long oppBankLgId = 0;

	/**
	 * 工具转账实体中 result 状态 （null，1，3，4）</br>
	 * 0:表示工具未上报</br>
	 * 1:成功</br>
	 * 3:失败</br>
	 * 4:未知</br>
	 * null:</br>
	 */
	private Integer reslt = 0;

	private int registWay = 0;

	private String msg;

	public SysBalTrans() {
	}

	public SysBalTrans(String msg) {
		if (StringUtils.isBlank(msg))
			return;
		this.msg = msg;
		String[] info = msg.split(":");
		int l = info.length;
		for (int index = 0; index < l; index++) {
			if (index == 1) {
				this.setSys(Integer.parseInt(info[1]));
			} else if (index == 2) {
				this.setFrId(Integer.parseInt(info[2]));
			} else if (index == 3) {
				this.setToId(Integer.parseInt(info[3]));
			} else if (index == 4) {
				this.setAmt(new BigDecimal(info[4]).setScale(2, BigDecimal.ROUND_HALF_UP));
			} else if (index == 5) {
				this.setFrAcc3Last(StringUtils.trimToEmpty(info[5]));
			} else if (index == 6) {
				this.setToAcc3Last(StringUtils.trimToEmpty(info[6]));
			} else if (index == 7) {
				String[] st = info[7].split("-");
				int s = st.length;
				for (int i = 0; i < s; i++) {
					if (i == 0)
						this.setGetTm(Long.parseLong(st[0]));
					else if (i == 1)
						this.setAckTm(Long.parseLong(st[1]));
					else if (i == 2)
						this.setAckByCurrTrans(Integer.parseInt(st[2]));
					else if (i == 3)
						this.setAckByNextTrans(Integer.parseInt(st[3]));
					else if (i == 4)
						this.setAckByCurrBal(Integer.parseInt(st[4]));
					else if (i == 5)
						this.setAckByCurrFlow(Integer.parseInt(st[5]));
					else if (i == 6)
						this.setAckByOppBal(Integer.parseInt(st[6]));
					else if (i == 7)
						this.setAckByOppFlow(Integer.parseInt(st[7]));
					else if (i == 8)
						this.setTaskId(Long.parseLong(st[8]));
					else if (i == 9)
						this.setTaskType(Integer.parseInt(st[9]));
					else if (i == 10)
						this.setBefore(new BigDecimal(st[10]).setScale(2, BigDecimal.ROUND_HALF_UP));
					else if (i == 11)
						this.setAfter(new BigDecimal(st[11]).setScale(2, BigDecimal.ROUND_HALF_UP));
					else if (i == 12)
						this.setBeopp(new BigDecimal(st[12]).setScale(2, BigDecimal.ROUND_HALF_UP));
					else if (i == 13) {
						this.setFrOwn2Last(StringUtils.trimToEmpty(st[13]));
					} else if (i == 14) {
						this.setToOwn2Last(StringUtils.trimToEmpty(st[14]));
					} else if (i == 15) {
						this.setSysLgId(StringUtils.isNumeric(st[15]) ? Long.parseLong(st[15]) : null);
					} else if (i == 16) {
						this.setOppSysLgId(StringUtils.isNumeric(st[16]) ? Long.parseLong(st[16]) : null);
					} else if (i == 17) {
						this.setBankLgId(StringUtils.isNumeric(st[17]) ? Long.parseLong(st[17]) : null);
					} else if (i == 18) {
						this.setOppBankLgId(StringUtils.isNumeric(st[18]) ? Long.parseLong(st[18]) : null);
					} else if (i == 19) {
						if (StringUtils.isBlank(st[19])) {
							this.reslt = null;
						} else {
							this.setReslt(Integer.valueOf(st[19]));
						}
					} else if (i == 20) {
						this.setAckByCurrResult1(Integer.parseInt(st[20]));
					} else if (i == 21) {
						this.setRegistWay(Integer.parseInt(st[21]));
					}
				}
			}
		}
	}

	public static String genMsg(int frId, int toId, BigDecimal amt, String frAcc3Last, String toAcc3Last, long taskId,
			int taskType, BigDecimal beopp, BigDecimal before, String frOwn2Last, String toOwn2Last, int registWay) {
		before = Objects.isNull(before) ? BigDecimal.ZERO : before.setScale(2, BigDecimal.ROUND_HALF_UP);
		return genMsg(SYS_NONE, frId, toId, amt, before, BigDecimal.ZERO, frAcc3Last, toAcc3Last,
				System.currentTimeMillis(), 0, ACK_NONE, ACK_NONE, ACK_NONE, ACK_NONE, ACK_NONE, ACK_NONE, taskId,
				taskType, beopp, frOwn2Last, toOwn2Last, 0, 0, 0, 0, 0, 0, registWay);
	}

	public static String genMsg(SysBalTrans t) {
		return genMsg(t.getSys(), t.getFrId(), t.getToId(), t.getAmt(), t.getBefore(), t.getAfter(), t.getFrAcc3Last(),
				t.getToAcc3Last(), t.getGetTm(), t.getAckTm(), t.getAckByCurrTrans(), t.getAckByNextTrans(),
				t.getAckByCurrBal(), t.getAckByCurrFlow(), t.getAckByOppBal(), t.getAckByOppFlow(), t.getTaskId(),
				t.getTaskType(), t.getBeopp(), t.getFrOwn2Last(), t.getToOwn2Last(), t.getSysLgId(), t.getOppSysLgId(),
				t.getBankLgId(), t.getOppBankLgId(), t.getReslt(), t.getAckByCurrResult1(), t.getRegistWay());
	}

	public static String genMsg(int sys, int frId, int toId, BigDecimal amt, BigDecimal before, BigDecimal after,
			String frAcc3Last, String toAcc3Last, long getTm, long ackTm, int ackByCurrTrans, int ackByNextTrans,
			int ackByCurrBal, int ackByCurrFlow, int ackByOppBal, int ackByOppFlow, long taskId, int taskType,
			BigDecimal beopp, String frOwn2Last, String toOwn2Last, long sysLgId, long oppSysLgId, long bankLgId,
			long oppBankLgId, Integer result, int ackByCurrResult1, int registWay) {
		amt = amt.setScale(2, BigDecimal.ROUND_HALF_UP).abs();
		before = before.setScale(2, BigDecimal.ROUND_HALF_UP).abs();
		after = after.setScale(2, BigDecimal.ROUND_HALF_UP).abs();
		beopp = beopp.setScale(2, BigDecimal.ROUND_HALF_UP).abs();
		String resultStr = Objects.isNull(result) ? StringUtils.EMPTY : result.toString();
		return String.format("%s%d:%d:%d:%s:%s:%s:%d-%d-%d-%d-%d-%d-%d-%d-%d-%d-%s-%s-%s-%s-%s-%d-%d-%d-%d-%s-%d-%d",
				RedisKeys.SYS_BAL, sys, frId, toId, amt, frAcc3Last, toAcc3Last, getTm, ackTm, ackByCurrTrans,
				ackByNextTrans, ackByCurrBal, ackByCurrFlow, ackByOppBal, ackByOppFlow, taskId, taskType, before, after,
				beopp, frOwn2Last, toOwn2Last, sysLgId, oppSysLgId, bankLgId, oppBankLgId, resultStr, ackByCurrResult1,
				registWay);
	}

	public static String genSusMsg(SysBalTrans t) {
		String msg = genMsg(t.getSys(), t.getFrId(), t.getToId(), t.getAmt(), t.getBefore(), t.getAfter(),
				t.getFrAcc3Last(), t.getToAcc3Last(), t.getGetTm(), System.currentTimeMillis(), t.getAckByCurrTrans(),
				t.getAckByNextTrans(), t.getAckByCurrBal(), t.getAckByCurrFlow(), t.getAckByOppBal(),
				t.getAckByOppFlow(), t.getTaskId(), t.getTaskType(), t.getBeopp(), t.getFrOwn2Last(), t.getToOwn2Last(),
				t.getSysLgId(), t.getOppSysLgId(), t.getBankLgId(), t.oppBankLgId, t.getReslt(),
				t.getAckByCurrResult1(), t.getRegistWay());
		return String.format("_%s", msg);
	}

	public static String genPatternSus() {
		return String.format("_%s*", RedisKeys.SYS_BAL);
	}

	public static String genPatternSus(int frId, String frAcc3Last) {
		return String.format("_%s*:%d:*:*:%s:*:*", RedisKeys.SYS_BAL, frId, frAcc3Last);
	}

	public static String genPatternSus(int frId, int toId, BigDecimal amt, String frAcc3Last, String toAcc3Last) {
		amt = amt.setScale(2, BigDecimal.ROUND_HALF_UP).abs();
		return String.format("_%s*:%d:%d:%s:%s:%s:*", RedisKeys.SYS_BAL, frId, toId, amt, frAcc3Last, toAcc3Last);
	}

	public static String genPatternSys(int sys) {
		return String.format("%s%d:*:*:*:*:*:*", RedisKeys.SYS_BAL, sys);
	}

	public static String genPatternFrId(int frId) {
		return String.format("%s*:%d:*:*:*:*:*", RedisKeys.SYS_BAL, frId);
	}

	public static String genPatternFrIdAndFrAcc(int frId, String frAcc3Last) {
		return String.format("%s*:%d:*:*:%s:*:*", RedisKeys.SYS_BAL, frId, frAcc3Last);
	}

	public static String genPatternToIdAndToAcc(int toId, String toAcc3Last) {
		return String.format("%s*:*:%d:*:*:%s:*", RedisKeys.SYS_BAL, toId, toAcc3Last);
	}

	public static String genPatternToId(int toId) {
		return String.format("%s*:*:%d:*:*:*:*", RedisKeys.SYS_BAL, toId);
	}

	public static String genPatternToIdAndToAccAndAmt(int toId, String toAcc3Last, BigDecimal amt) {
		amt = amt.setScale(2, BigDecimal.ROUND_HALF_UP).abs();
		return String.format("%s*:*:%d:%s:*:%s:*", RedisKeys.SYS_BAL, toId, amt, toAcc3Last);
	}

	public static String genPatternFrIdAndToIdAndAmt(int frId, int toId, BigDecimal amt) {
		amt = amt.setScale(2, BigDecimal.ROUND_HALF_UP).abs();
		return String.format("%s*:%d:%d:%s:*:*:*", RedisKeys.SYS_BAL, frId, toId, amt);
	}

	public static String genPatternFrIdAndToIdAndAmtAndFrAccAndToAcc(int frId, int toId, BigDecimal amt,
			String frAcc3Last, String toAcc3Last) {
		amt = amt.setScale(2, BigDecimal.ROUND_HALF_UP).abs();
		return String.format("%s*:%d:%d:%s:%s:%s:*", RedisKeys.SYS_BAL, frId, toId, amt, frAcc3Last, toAcc3Last);
	}

	public int getSys() {
		return sys;
	}

	public void setSys(int sys) {
		this.sys = sys;
	}

	public boolean ackFrByFlow() {
		return this.ackByCurrFlow == SysBalTrans.ACK_ACK;
	}

	public boolean ackFr() {
		return this.ackByCurrTrans == SysBalTrans.ACK_ACK || this.ackByNextTrans == SysBalTrans.ACK_ACK
				|| this.ackByCurrBal == SysBalTrans.ACK_ACK || this.ackByCurrFlow == SysBalTrans.ACK_ACK
				|| this.ackByCurrResult1 == SysBalTrans.ACK_ACK;
	}

	public boolean ackTo() {
		return this.ackByOppBal == SysBalTrans.ACK_ACK || this.ackByOppFlow == SysBalTrans.ACK_ACK;
	}

	public int getFrId() {
		return frId;
	}

	public void setFrId(int frId) {
		this.frId = frId;
	}

	public int getToId() {
		return toId;
	}

	public void setToId(int toId) {
		this.toId = toId;
	}

	public BigDecimal getAmt() {
		return amt;
	}

	public void setAmt(BigDecimal amt) {
		this.amt = amt;
	}

	public String getFrAcc3Last() {
		return frAcc3Last;
	}

	public void setFrAcc3Last(String frAcc3Last) {
		this.frAcc3Last = frAcc3Last;
	}

	public String getToAcc3Last() {
		return toAcc3Last;
	}

	public void setToAcc3Last(String toAcc3Last) {
		this.toAcc3Last = toAcc3Last;
	}

	public long getGetTm() {
		return getTm;
	}

	public void setGetTm(long getTm) {
		this.getTm = getTm;
	}

	public long getAckTm() {
		return ackTm;
	}

	public void setAckTm(long ackTm) {
		this.ackTm = ackTm;
	}

	public int getAckByCurrTrans() {
		return ackByCurrTrans;
	}

	public void setAckByCurrTrans(int ackByCurrTrans) {
		this.ackByCurrTrans = ackByCurrTrans;
	}

	public int getAckByNextTrans() {
		return ackByNextTrans;
	}

	public void setAckByNextTrans(int ackByNextTrans) {
		this.ackByNextTrans = ackByNextTrans;
	}

	public int getAckByCurrBal() {
		return ackByCurrBal;
	}

	public void setAckByCurrBal(int ackByCurrBal) {
		this.ackByCurrBal = ackByCurrBal;
	}

	public int getAckByCurrFlow() {
		return ackByCurrFlow;
	}

	public void setAckByCurrFlow(int ackByCurrFlow) {
		this.ackByCurrFlow = ackByCurrFlow;
	}

	public int getAckByCurrResult1() {
		return ackByCurrResult1;
	}

	public void setAckByCurrResult1(int ackByCurrResult1) {
		this.ackByCurrResult1 = ackByCurrResult1;
	}

	public int getAckByOppBal() {
		return ackByOppBal;
	}

	public void setAckByOppBal(int ackByOppBal) {
		this.ackByOppBal = ackByOppBal;
	}

	public int getAckByOppFlow() {
		return ackByOppFlow;
	}

	public void setAckByOppFlow(int ackByOppFlow) {
		this.ackByOppFlow = ackByOppFlow;
	}

	public long getTaskId() {
		return taskId;
	}

	public void setTaskId(long taskId) {
		this.taskId = taskId;
	}

	public int getTaskType() {
		return taskType;
	}

	public void setTaskType(int taskType) {
		this.taskType = taskType;
	}

	public BigDecimal getBefore() {
		return before;
	}

	public void setBefore(BigDecimal before) {
		this.before = before;
	}

	public BigDecimal getAfter() {
		return after;
	}

	public void setAfter(BigDecimal after) {
		this.after = after;
	}

	public BigDecimal getBeopp() {
		return beopp;
	}

	public void setBeopp(BigDecimal beopp) {
		this.beopp = beopp;
	}

	public String getFrOwn2Last() {
		return frOwn2Last;
	}

	public void setFrOwn2Last(String frOwn2Last) {
		this.frOwn2Last = frOwn2Last;
	}

	public String getToOwn2Last() {
		return toOwn2Last;
	}

	public void setToOwn2Last(String toOwn2Last) {
		this.toOwn2Last = toOwn2Last;
	}

	public long getSysLgId() {
		return sysLgId;
	}

	public void setSysLgId(long sysLgId) {
		this.sysLgId = sysLgId;
	}

	public long getOppSysLgId() {
		return oppSysLgId;
	}

	public void setOppSysLgId(long oppSysLgId) {
		this.oppSysLgId = oppSysLgId;
	}

	public long getBankLgId() {
		return bankLgId;
	}

	public void setBankLgId(long bankLgId) {
		this.bankLgId = bankLgId;
	}

	public long getOppBankLgId() {
		return oppBankLgId;
	}

	public void setOppBankLgId(long oppBankLgId) {
		this.oppBankLgId = oppBankLgId;
	}

	public Integer getReslt() {
		return reslt;
	}

	public void setReslt(Integer reslt) {
		this.reslt = reslt;
	}

	public int getRegistWay() {
		return registWay;
	}

	public void setRegistWay(int registWay) {
		this.registWay = registWay;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	/***/

	private boolean invalidTransferTime = false;

	public void setInvalidTransferTime(boolean invalidTransferTime) {
		this.invalidTransferTime = invalidTransferTime;
	}

	public boolean getInvalidTransferTime() {
		return this.invalidTransferTime;
	}

	/***/
	private BizBankLog frBankLog = null;

	public BizBankLog getFrBankLog() {
		return frBankLog;
	}

	public void setFrBankLog(BizBankLog frBankLog) {
		this.frBankLog = frBankLog;
	}
}
