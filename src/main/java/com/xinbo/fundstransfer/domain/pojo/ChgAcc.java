package com.xinbo.fundstransfer.domain.pojo;

import com.xinbo.fundstransfer.component.redis.RedisKeys;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

public class ChgAcc {

	public static final int STATUS_NORMAL = 0;
	public static final int STATUS_SUSPEND_DISBURSE_TO_MEMBER = 1;
	public static final int STATUS_FINISH_CONCILIATE = 2;

	/**
	 * mobile number
	 */
	private String mobile;

	/**
	 * current usage account
	 */
	private int currAcc;

	/**
	 * {@code 0} : normal {@code 1} : suspend disburse to member {@code 2} :
	 * finish reconciliation {@code 3} :
	 */
	private int status = 0;

	/**
	 * the number of account that can refer to {@code mobile}
	 */
	private int total = 1;

	/**
	 * usage account next time
	 * 是否已重新计算可用额度：0-否；1-是
	 */
	private Integer nextAcc;

	/**
	 * already used accounts today.
	 */
	private List<Integer> usedAcc;

	private Long chgTm;

	private String msg;

	//过期时间
	private Long expTm;

	public ChgAcc() {
	}

	public static String genMsg(String mobile, int currAcc, int status, int total, Integer nextAcc,
			List<Integer> usedAccSet, Long chgTm,Long expTm) {
		String mobileS = StringUtils.trimToEmpty(mobile);
		String currAccS = String.valueOf(currAcc);
		String statusS = String.valueOf(status);
		String totalS = String.valueOf(total);
		String nextAccS = Objects.isNull(nextAcc) ? StringUtils.EMPTY : String.valueOf(nextAcc);
		String usedAccS = StringUtils.EMPTY;
		String chgTms = Objects.isNull(chgTm) ? StringUtils.EMPTY : String.valueOf(chgTm);
		if (!CollectionUtils.isEmpty(usedAccSet)) {
			Set<String> set = usedAccSet.stream().filter(Objects::nonNull).map(p -> p.toString())
					.collect(Collectors.toSet());
			if (!CollectionUtils.isEmpty(set))
				usedAccS = String.join(",", set);
		}
		return String.format("%s:%s:%s:%s:%s:%s:%s:%s", mobileS, currAccS, statusS, totalS, nextAccS,
				usedAccS, chgTms,expTm);
	}

	public ChgAcc(String msg) {
		if (StringUtils.isBlank(msg))
			return;
		this.msg = msg;
		String[] info = msg.split(":");
		int l = info.length;
		for (int index = 0; index < l; index++) {
			if (index == 0) {
				this.mobile = StringUtils.isBlank(info[0]) ? null : info[0];
			} else if (index == 1) {
				this.currAcc = StringUtils.isBlank(info[1]) ? 0 : Integer.valueOf(info[1]);
			} else if (index == 2) {
				this.status = StringUtils.isBlank(info[2]) ? 0 : Integer.valueOf(info[2]);
			} else if (index == 3) {
				this.total = StringUtils.isBlank(info[3]) ? 1 : Integer.valueOf(info[3]);
			} else if (index == 4) {
				this.nextAcc = StringUtils.isBlank(info[4]) ? null : Integer.valueOf(info[4]);
			} else if (index == 5) {
				if (StringUtils.isNotBlank(info[5])) {
					if (Objects.isNull(this.usedAcc))
						this.usedAcc = new ArrayList<>();
					for (String id : info[5].split(",")) {
						if (StringUtils.isNumeric(id))
							this.usedAcc.add(Integer.valueOf(id));
					}
					if (CollectionUtils.isEmpty(this.usedAcc))
						this.usedAcc = null;
				} else {
					this.usedAcc = null;
				}
			} else if (index == 6) {
				if (StringUtils.isNotBlank(info[6]))
					this.chgTm = Long.valueOf(info[6]);
			} else if(index == 7){
				if (StringUtils.isNotBlank(info[7]))
					this.expTm = Long.valueOf(info[7]);
			}
		}
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public int getCurrAcc() {
		return currAcc;
	}

	public void setCurrAcc(int currAcc) {
		this.currAcc = currAcc;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public Integer getNextAcc() {
		return nextAcc;
	}

	public void setNextAcc(Integer nextAcc) {
		this.nextAcc = nextAcc;
	}

	public List<Integer> getUsedAcc() {
		return usedAcc;
	}

	public void setUsedAcc(List<Integer> usedAcc) {
		this.usedAcc = usedAcc;
	}

	public Long getChgTm() {
		return Objects.isNull(chgTm) ? System.currentTimeMillis() : chgTm;
	}

	public void setChgTm(Long chgTm) {
		this.chgTm = chgTm;
	}

	public Long getExpTm() {
		return expTm;
	}

	public void setExpTm(Long expTm) {
		this.expTm = expTm;
	}
}
