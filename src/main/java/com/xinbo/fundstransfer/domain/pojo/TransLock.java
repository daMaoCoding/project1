package com.xinbo.fundstransfer.domain.pojo;

import java.math.BigDecimal;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.xinbo.fundstransfer.component.redis.RedisKeys;

@SuppressWarnings("unused")
public class TransLock {

	public static int STATUS_ALLOC = 0;
	public static int STATUS_CLAIM = 1;
	public static int STATUS_ACK = 2;
	public static int STATUS_DEL = 3;

	public static int STATUS_REGULAR_ = 0;
	public static int STATUS_NEED_ = 1;

	public static int STATUS_ACK_ = 20;

	private String msg;

	private Integer frId;
	private Integer toId;
	private Integer oprId;
	private BigDecimal transRadix;
	private BigDecimal transInt;
	private Long time;
	private Integer status;
	private Long ltTime;

	public TransLock() {
	}

	public static String genMsg(int frId, int toId, int oprId, BigDecimal transRadix, BigDecimal transInt, long time,
			int status, long ltTime) {
		return String.format("%s%d:%d:%d:%s:%d:%d:%d:%d", RedisKeys.TRANSFER_ACCOUNT_LOCK, frId, toId, oprId,
				transRadix.toString(), transInt.intValue(), time, status, ltTime);
	}

	public String toMsg() {
		return genMsg(getFrId(), getToId(), getOprId(), getTransRadix(), getTransInt(), getTime(), getStatus(),
				getLtTime());
	}

	public TransLock(String msg) {
		this(false, msg);
	}

	public TransLock(boolean ignore, String msg) {
		if (StringUtils.isBlank(msg)) {
			return;
		}
		this.msg = StringUtils.trimToEmpty(msg);
		String[] info = msg.split(":");
		int l = info.length;
		for (int index = 0; index < l; index++) {
			if (index == 1) {
				if (!ignore) {
					String fromId = info[1];
					if (StringUtils.isNotBlank(fromId)) {
						// 第三方下发到出款卡的 fromid=accountid+System.currentTimeMillis()
						if (fromId.length() > 13) {
							this.frId = Integer.valueOf(info[1].substring(0, fromId.length() - 13));
						} else {
							this.frId = Integer.valueOf(info[1]);
						}
					} else {
						this.frId = null;
					}

				}

			} else if (index == 2) {
				this.toId = StringUtils.isBlank(info[2]) ? null : Integer.valueOf(info[2]);
			} else if (index == 3) {
				this.oprId = StringUtils.isBlank(info[3]) ? null : Integer.valueOf(info[3]);
			} else if (index == 4) {
				this.transRadix = StringUtils.isBlank(info[4]) ? null : new BigDecimal(info[4]);
			} else if (index == 5) {
				this.transInt = StringUtils.isBlank(info[5]) ? null : new BigDecimal(info[5]);
			} else if (index == 6) {
				this.time = StringUtils.isBlank(info[6]) ? null : Long.valueOf(info[6]);
			} else if (index == 7) {
				this.status = StringUtils.isBlank(info[7]) ? null : Integer.valueOf(info[7]);
			} else if (index == 8) {
				this.ltTime = StringUtils.isBlank(info[8]) ? null : Long.valueOf(info[8]);
			}
		}
	}

	public Integer getFrId() {
		return frId;
	}

	public void setFrId(Integer frId) {
		this.frId = frId;
	}

	public Integer getToId() {
		return toId;
	}

	public void setToId(Integer toId) {
		this.toId = toId;
	}

	public Integer getOprId() {
		return oprId;
	}

	public void setOprId(Integer oprId) {
		this.oprId = oprId;
	}

	public BigDecimal getTransRadix() {
		return transRadix == null ? BigDecimal.ZERO : transRadix;
	}

	public void setTransRadix(BigDecimal transRadix) {
		this.transRadix = transRadix;
	}

	public BigDecimal getTransInt() {
		return transInt == null ? BigDecimal.ZERO : transInt;
	}

	public void setTransInt(BigDecimal transInt) {
		this.transInt = transInt;
	}

	public Long getTime() {
		return Objects.isNull(time) ? System.currentTimeMillis() : time;
	}

	public void setTime(Long time) {
		this.time = time;
	}

	public Integer getStatus() {
		return Objects.isNull(status) ? STATUS_ALLOC : status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public Long getLtTime() {
		return Objects.isNull(ltTime) ? System.currentTimeMillis() : ltTime;
	}

	public void setLtTime(Long ltTime) {
		this.ltTime = ltTime;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public static String getStatusMsg(int status) {
		if (STATUS_ALLOC == status) {
			return "已分配";
		} else if (STATUS_CLAIM == status) {
			return "下发中";
		} else if (STATUS_ACK == status) {
			return "已下发";
		} else if (STATUS_DEL == status) {
			return "下发失败";
		} else {
			return "未知";
		}
	}
}
