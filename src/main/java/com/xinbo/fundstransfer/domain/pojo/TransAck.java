package com.xinbo.fundstransfer.domain.pojo;

import com.xinbo.fundstransfer.component.redis.RedisKeys;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.Objects;

@SuppressWarnings("unused")
public class TransAck {

	public static int INOUT_IN = 0;
	public static int INOUT_OUT = 1;

	// confirm to be failure
	public static int RET_NO = 0;
	// confirm to be success
	public static int RET_YES = 1;
	// to be confirmed
	public static int RET_TBC = 2;

	public static int SORT_OTASK = 1;
	public static int SORT_TRANS = 2;

	private int id;
	private int oppId;
	private int inOut;
	private BigDecimal tdAmt;
	private Long tdTm;
	private int sort = 0;
	private long order = 0;

	private int ret = RET_TBC;

	private String msg;

	public TransAck() {
	}

	public TransAck(String msg) {
		if (StringUtils.isBlank(msg)) {
			return;
		}
		this.msg = StringUtils.trimToEmpty(msg);
		String[] info = msg.split(":");
		int l = info.length;
		for (int index = 0; index < l; index++) {
			if (index == 1) {
				this.id = StringUtils.isBlank(info[1]) ? 0 : Integer.parseInt(info[1]);
			} else if (index == 2) {
				this.oppId = StringUtils.isBlank(info[2]) ? 0 : Integer.valueOf(info[2]);
			} else if (index == 3) {
				this.inOut = StringUtils.isBlank(info[3]) ? INOUT_IN : Integer.valueOf(info[3]);
			} else if (index == 4) {
				this.tdAmt = StringUtils.isBlank(info[4]) ? BigDecimal.ZERO : new BigDecimal(info[4]);
			} else if (index == 5) {
				this.tdTm = StringUtils.isBlank(info[5]) ? System.currentTimeMillis() : Long.parseLong(info[5]);
			} else if (index == 6) {
				this.sort = StringUtils.isBlank(info[6]) ? 0 : Integer.valueOf(info[6]);
			} else if (index == 7) {
				this.order = StringUtils.isBlank(info[7]) ? 0 : Long.valueOf(info[7]);
			}
		}
	}

	public static String genMsg(Integer id, Integer oppId, int inOut, BigDecimal tdAmt, Long tdTm, Integer sort,
			Long order) {
		oppId = Objects.nonNull(oppId) && oppId != 0 ? oppId : 0;
		sort = Objects.isNull(sort) ? 0 : sort;
		order = Objects.isNull(order) ? 0 : order;
		return Objects.nonNull(id) || id != 0 ? String.format("%s%d:%d:%d:%s:%d:%d:%d", RedisKeys.TRANS_ACK, id, oppId,
				inOut, tdAmt.setScale(2, BigDecimal.ROUND_HALF_UP), tdTm, sort, order) : null;
	}

	public static final String genPattern4Id(int id) {
		return RedisKeys.TRANS_ACK + id + ":*";
	}

	public static final String genPattern4IdAndOppIdAndInOutAndTdAmtAndTdTm(int id, int oppId, int inOut,
			BigDecimal tdAmt, long tdTm) {
		return RedisKeys.TRANS_ACK + id + ":" + oppId + ":" + inOut
				+ tdAmt.setScale(2, BigDecimal.ROUND_HALF_UP).toString() + ":*:" + tdTm;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getOppId() {
		return oppId;
	}

	public void setOppId(int oppId) {
		this.oppId = oppId;
	}

	public int getInOut() {
		return inOut;
	}

	public void setInOut(int inOut) {
		this.inOut = inOut;
	}

	public BigDecimal getTdAmt() {
		return tdAmt;
	}

	public void setTdAmt(BigDecimal tdAmt) {
		this.tdAmt = tdAmt;
	}

	public Long getTdTm() {
		return tdTm;
	}

	public void setTdTm(Long tdTm) {
		this.tdTm = tdTm;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public int getRet() {
		return ret;
	}

	public void setRet(int ret) {
		this.ret = ret;
	}

	public int getSort() {
		return sort;
	}

	public void setSort(int sort) {
		this.sort = sort;
	}

	public long getOrder() {
		return order;
	}

	public void setOrder(long order) {
		this.order = order;
	}
}
