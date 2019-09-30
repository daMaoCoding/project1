package com.xinbo.fundstransfer.report;

import org.apache.commons.lang3.StringUtils;

public class SnowFlake {

	/**
	 * 起始的时间戳
	 */
	private final static long START_STMP = 1546272000000L;// 2019-01-01 00:00:00

	private final static long TOTAL_BIT = 50;// 总位数 50位
	/**
	 * 每一部分占用的位数
	 */
	private final static long SEQUENCE_BIT = 12; // 序列号占用的位数

	/**
	 * 每一部分的最大值
	 */
	private final static long MAX_SEQUENCE = -1L ^ (-1L << SEQUENCE_BIT);

	private final static long MAX_TIMESTAMP = -1L ^ (-1L << (TOTAL_BIT - SEQUENCE_BIT));

	private long sequence = 0L; // 序列号
	private long lastStmp = -1L;// 上一次时间戳

	public SnowFlake() {
	}

	/**
	 * 产生下一个ID
	 *
	 * @return
	 */
	public synchronized String nextId(String prefix) {
		long currStmp = getNewstmp();
		if (currStmp < lastStmp) {
			throw new RuntimeException("Clock moved backwards.  Refusing to generate id");
		}
		if (currStmp == lastStmp) {
			sequence = (sequence + 1) & MAX_SEQUENCE;
			if (sequence == 0L) {
				currStmp = getNextMill();
			}
		} else {
			sequence = 0L;
		}
		lastStmp = currStmp;
		long id = ((currStmp - START_STMP) % MAX_TIMESTAMP) << SEQUENCE_BIT | sequence;
		String t = "0000000000000000" + id;
		Integer l = t.length();
		return StringUtils.trimToEmpty(prefix) + t.substring(l - 15, l);
	}

	private long getNextMill() {
		long mill = getNewstmp();
		while (mill <= lastStmp) {
			mill = getNewstmp();
		}
		return mill;
	}

	private long getNewstmp() {
		return System.currentTimeMillis();
	}

	public static void main(String[] args) throws Exception {
		SnowFlake flake = new SnowFlake();
		String d = flake.nextId("D");
		System.out.println("d:" + d + ";l:" + String.valueOf(d).length());
	}
}
