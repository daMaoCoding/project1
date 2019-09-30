package com.xinbo.fundstransfer.report.fail;

import com.xinbo.fundstransfer.domain.entity.BizBankLog;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.TransferEntity;
import com.xinbo.fundstransfer.report.up.ReportCheck;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;

public class EntityNotify {
	private Integer tarId;
	private String type;

	private AccountBaseInfo base = null;
	private TransferEntity entity = null;
	private ReportCheck check = null;
	private BizBankLog bankLog = null;
	private BizBankLog lastBankLog = null;
	private BigDecimal sysBal = null;
	private BigDecimal bankBal = null;
	private boolean result = false;
	private long occurTime = 0;

	public EntityNotify(String msg) {
		if (StringUtils.isBlank(msg)) {
			this.tarId = null;
			this.tarId = null;
		} else {
			String[] inf = msg.split(Common.SEPARATOR_COMMA);
			if (inf == null || inf.length != 2 || !StringUtils.isNumeric(inf[0])) {
				this.tarId = null;
				this.tarId = null;
			} else {
				this.tarId = Integer.valueOf(inf[0]);
				this.type = inf[1];
			}
		}
	}

	public EntityNotify(String type, Integer tarId, AccountBaseInfo base, BigDecimal sysBal, BigDecimal bankBal,
			long occurTime, ReportCheck check) {
		this.type = type;
		this.tarId = tarId;
		this.base = base;
		this.sysBal = sysBal;
		this.bankBal = bankBal;
		this.occurTime = occurTime;
		this.check = check;
	}

	public EntityNotify(String type, Integer tarId, AccountBaseInfo base, TransferEntity entity, ReportCheck check,
			BizBankLog bankLog, BizBankLog lastBankLog, BigDecimal bankBal) {
		this.tarId = tarId;
		this.type = type;
		this.base = base;
		this.entity = entity;
		this.check = check;
		this.bankLog = bankLog;
		this.lastBankLog = lastBankLog;
		this.result = false;
		this.bankBal = bankBal;
	}

	public Integer getTarId() {
		return tarId;
	}

	public void setTarId(Integer tarId) {
		this.tarId = tarId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public static String genKey(Integer tarId, String type) {
		return String.format("%d%s%s", tarId, Common.SEPARATOR_COMMA, type);
	}

	public AccountBaseInfo getBase() {
		return base;
	}

	public TransferEntity getEntity() {
		return entity;
	}

	public ReportCheck getCheck() {
		return check;
	}

	public BizBankLog getBankLog() {
		return bankLog;
	}

	public boolean getResult() {
		return result;
	}

	public void setResult(boolean result) {
		this.result = result;
	}

	public BigDecimal getBankBal() {
		return bankBal;
	}

	public void setBankBal(BigDecimal bankBal) {
		this.bankBal = bankBal;
	}

	public BigDecimal getSysBal() {
		return sysBal;
	}

	public void setSysBal(BigDecimal sysBal) {
		this.sysBal = sysBal;
	}

	public long getOccurTime() {
		return occurTime;
	}

	public void setOccurTime(long occurTime) {
		this.occurTime = occurTime;
	}

	public BizBankLog getLastBankLog() {
		return lastBankLog;
	}

	public void setLastBankLog(BizBankLog lastBankLog) {
		this.lastBankLog = lastBankLog;
	}
}
