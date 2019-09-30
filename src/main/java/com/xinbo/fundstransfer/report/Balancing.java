package com.xinbo.fundstransfer.report;

import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;

public class Balancing {
	private int target = 0;
	private long analyseTime = 0;
	private BigDecimal dbSysBal = BigDecimal.ZERO;
	private BigDecimal dbBankBal = BigDecimal.ZERO;
	private BigDecimal checkedSysBal = BigDecimal.ZERO;
	private BigDecimal checkedBankBal = BigDecimal.ZERO;
	private BigDecimal transferingBal = BigDecimal.ZERO;
	private BigDecimal transferredBal = BigDecimal.ZERO;

	public Balancing(String msg) {
		if (StringUtils.isBlank(msg))
			return;
		String[] info = msg.split(":");
		int l = info.length;
		for (int index = 0; index < l; index++) {
			if (index == 0) {
				this.target = Integer.parseInt(info[0]);
			} else if (index == 1) {
				this.analyseTime = Long.parseLong(info[1]);
			} else if (index == 2) {
				this.dbSysBal = new BigDecimal(info[2]);
			} else if (index == 3) {
				this.dbBankBal = new BigDecimal(info[3]);
			} else if (index == 4) {
				this.checkedSysBal = new BigDecimal(info[4]);
			} else if (index == 5) {
				this.checkedBankBal = new BigDecimal(info[5]);
			} else if (index == 6) {
				this.transferingBal = new BigDecimal(info[6]);
			} else if (index == 7) {
				this.transferredBal = new BigDecimal(info[7]);
			}
		}
	}

	public Balancing(Integer target, BigDecimal dbSysBal, BigDecimal dbBankBal) {
		this.target = target;
		this.dbSysBal = dbSysBal;
		this.dbBankBal = dbBankBal;
	}

	public static String genMsg(int target, long analyseTime, BigDecimal dbSysBal, BigDecimal dbBankBal,
			BigDecimal checkedSysBal, BigDecimal checkedBankBal, BigDecimal transferingBal, BigDecimal transferredBal) {
		String targetStr = String.valueOf(target);
		String analyseTimeStr = String.valueOf(analyseTime);
		String dbSysBalStr = SysBalUtils.radix2(dbSysBal).toString();
		String dbBankBalStr = SysBalUtils.radix2(dbBankBal).toString();
		String checkedSysBalStr = SysBalUtils.radix2(checkedSysBal).toString();
		String checkedBankBalStr = SysBalUtils.radix2(checkedBankBal).toString();
		String transferingBalStr = SysBalUtils.radix2(transferingBal).toString();
		String transferredBalStr = SysBalUtils.radix2(transferredBal).toString();
		return String.format("%s:%s:%s:%s:%s:%s:%s:%s", targetStr, analyseTimeStr, dbSysBalStr, dbBankBalStr,
				checkedSysBalStr, checkedBankBalStr, transferingBalStr, transferredBalStr);
	}

	public int getTarget() {
		return target;
	}

	public long getAnalyseTime() {
		return analyseTime;
	}

	public BigDecimal getDbSysBal() {
		return dbSysBal;
	}

	public BigDecimal getDbBankBal() {
		return dbBankBal;
	}

	public BigDecimal getCheckedSysBal() {
		return checkedSysBal;
	}

	public BigDecimal getCheckedBankBal() {
		return checkedBankBal;
	}

	public BigDecimal getTransferingBal() {
		return transferingBal;
	}

	public BigDecimal getTransferredBal() {
		return transferredBal;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Balancing other = (Balancing) obj;
		if (target != other.getTarget())
			return false;
		return true;
	}

	public Balancing clone() {
		Balancing o = null;
		try {
			o = (Balancing) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return o;
	}
}
