package com.xinbo.fundstransfer.report.fail;

import com.xinbo.fundstransfer.report.SysBalUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.ZSetOperations;

import java.math.BigDecimal;
import java.util.Date;
import java.util.StringJoiner;

public class EntityRecord {

	private long occurMillis;

	private String type;

	private String occurTime;

	private BigDecimal sysBal;

	private BigDecimal bankBal;

	public EntityRecord(ZSetOperations.TypedTuple<String> arg0) {
		this.occurMillis = arg0.getScore().longValue();
		String v = arg0.getValue();
		if (StringUtils.isNotBlank(v)) {
			String[] info = v.split(Common.SEPARATOR_COMMA);
			int l = info.length;
			for (int index = 0; index < l; index++) {
				if (index == 0) {
					this.type = info[0];
				} else if (index == 1) {
					this.occurTime = info[1];
				} else if (index == 2) {
					this.bankBal = new BigDecimal(info[2]);
				} else if (index == 3) {
					this.sysBal = new BigDecimal(info[3]);
				}
			}
		}
	}

	public static String genMsg(String type, BigDecimal bankBal, BigDecimal sysBal, long occurMillis) {
		StringJoiner result = new StringJoiner(Common.SEPARATOR_COMMA);
		result.add(type);
		result.add(Common.yyyyMMddHHmmss.get().format(new Date(occurMillis)));
		result.add(SysBalUtils.radix2(bankBal).toString());
		result.add(SysBalUtils.radix2(sysBal).toString());
		return result.toString();
	}

	public long getOccurMillis() {
		return occurMillis;
	}

	public String getType() {
		return type;
	}

	public String getOccurTime() {
		return occurTime;
	}

	public BigDecimal getSysBal() {
		return sysBal;
	}

	public BigDecimal getBankBal() {
		return bankBal;
	}

	@Override
	public String toString() {
		return String.format("{} b:{} s:{}|", this.getOccurTime(), this.bankBal, this.sysBal);
	}
}
