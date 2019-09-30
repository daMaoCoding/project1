package com.xinbo.fundstransfer;

import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;

import java.math.BigDecimal;
import java.util.Objects;

public class FeeUtil {

	public static BigDecimal fee(String bankType, BigDecimal amt) {
		if (Objects.equals("工商银行", bankType)) {
			if (amt.compareTo(new BigDecimal(5000)) < 0)
				return BigDecimal.ZERO;
			else if (amt.compareTo(new BigDecimal(10000)) <= 0)
				return new BigDecimal(5);
			else
				return new BigDecimal(7.5);
		} else if (Objects.equals("农业银行", bankType)) {
			if (amt.compareTo(new BigDecimal(5000)) < 0)
				return BigDecimal.ZERO;
			else if (amt.compareTo(new BigDecimal(10000)) <= 0)
				return new BigDecimal(4);
			else
				return new BigDecimal(6);
		} else if (Objects.equals("招商银行", bankType)) {
			return BigDecimal.ZERO;
		} else if (Objects.equals("建设银行", bankType)) {
			if (amt.compareTo(new BigDecimal(5000)) < 0)
				return BigDecimal.ZERO;
			else if (amt.compareTo(new BigDecimal(10000)) <= 0)
				return new BigDecimal(5);
			else
				return new BigDecimal(7.5);
		} else if (Objects.equals("浦发银行", bankType)) {
			return BigDecimal.ZERO;// 无
		} else if (Objects.equals("民生银行", bankType)) {
			return BigDecimal.ZERO;// 无
		} else if (Objects.equals("云南农信", bankType)) {
			return BigDecimal.ZERO;// ------无卡
		} else if (Objects.equals("交通银行", bankType)) {
			if (amt.compareTo(new BigDecimal(5000)) < 0)
				return BigDecimal.ZERO;
			else if (amt.compareTo(new BigDecimal(10000)) <= 0)
				return new BigDecimal(8);
			else
				return new BigDecimal(12);
		} else if (Objects.equals("中信银行", bankType)) {
			return BigDecimal.ZERO;// 无
		} else if (Objects.equals("平安银行", bankType)) {
			return BigDecimal.ZERO;// 无
		} else if (Objects.equals("柳州银行", bankType)) {
			return BigDecimal.ZERO;// 无
		} else if (Objects.equals("中国银行", bankType)) {
			if (amt.compareTo(new BigDecimal(5000)) < 0)
				return BigDecimal.ZERO;
			else if (amt.compareTo(new BigDecimal(10000)) <= 0)
				return new BigDecimal(12);
			else
				return new BigDecimal(18);
		} else if (Objects.equals("锦州银行", bankType)) {
			return BigDecimal.ZERO;// ------无卡
		} else if (Objects.equals("南京银行", bankType)) {
			return BigDecimal.ZERO;// 无
		} else if (Objects.equals("包商银行", bankType)) {
			return BigDecimal.ZERO;// 无
		} else if (Objects.equals("成都银行", bankType)) {
			return BigDecimal.ZERO;// 无
		} else if (Objects.equals("兴业银行", bankType)) {
			return BigDecimal.ZERO;// 无
		} else if (Objects.equals("中原银行", bankType)) {
			return BigDecimal.ZERO;// 无
		} else if (Objects.equals("天府银行", bankType)) {
			return BigDecimal.ZERO;// 无
		} else if (Objects.equals("哈尔滨银行", bankType)) {
			return BigDecimal.ZERO;// 无
		} else if (Objects.equals("北京农商", bankType)) {
			if (amt.compareTo(new BigDecimal(2000)) <= 0)
				return BigDecimal.ONE;
			else if (amt.compareTo(new BigDecimal(5000)) <= 0)
				return new BigDecimal(2.5);
			else if (amt.compareTo(new BigDecimal(10000)) <= 0)
				return new BigDecimal(5);
			else
				return new BigDecimal(7.5);
		} else if (Objects.equals("福建海峡银行", bankType)) {
			return BigDecimal.ZERO;// 无卡
		} else if (Objects.equals("兰州银行", bankType)) {
			return BigDecimal.ZERO;// 无
		} else if (Objects.equals("临商银行", bankType)) {
			return BigDecimal.ZERO;// 无卡
		} else if (Objects.equals("汉口银行", bankType)) {
			return BigDecimal.ZERO;// 无
		} else if (Objects.equals("广发银行", bankType)) {
			return BigDecimal.ZERO;// 无
		} else if (Objects.equals("威海市商业银行", bankType)) {
			return BigDecimal.ZERO;// 无
		} else if (Objects.equals("桂林银行", bankType)) {
			return BigDecimal.ZERO;// 无
		} else if (Objects.equals("广州农村商业银行", bankType)) {
			return BigDecimal.ZERO;// 无卡
		} else if (Objects.equals("盛京银行", bankType)) {
			return BigDecimal.ZERO;// 无
		} else if (Objects.equals("华夏银行", bankType)) {
			return BigDecimal.ZERO;// 无
		} else if (Objects.equals("武汉农村商业银行", bankType)) {
			return BigDecimal.ZERO;// 无
		} else if (Objects.equals("浙商银行", bankType)) {
			return BigDecimal.ZERO;// 无
		} else if (Objects.equals("广西北部湾银行", bankType)) {
			return BigDecimal.ZERO;// 无
		} else if (Objects.equals("江西银行", bankType)) {
			return BigDecimal.ZERO;// 无
		} else if (Objects.equals("沧州银行", bankType)) {
			return BigDecimal.ZERO;// 无
		} else if (Objects.equals("徽商银行", bankType)) {
			return BigDecimal.ZERO;// 无
		} else if (Objects.equals("内蒙古银行", bankType)) {
			return BigDecimal.ZERO;// 无
		} else if (Objects.equals("广西农村信用社", bankType)) {
			return BigDecimal.ZERO;// 无卡
		} else if (Objects.equals("光大银行", bankType)) {
			return BigDecimal.ZERO;// 无卡
		} else if (Objects.equals("四川农信", bankType)) {
			return BigDecimal.ZERO;// 无卡
		} else if (Objects.equals("廊坊银行", bankType)) {
			return BigDecimal.ZERO;// 无
		} else if (Objects.equals("重庆银行", bankType)) {
			return BigDecimal.ZERO;// 无卡
		} else if (Objects.equals("齐商银行", bankType)) {
			return BigDecimal.ZERO;// 无卡
		} else if (Objects.equals("东营银行", bankType)) {
			return BigDecimal.ZERO;// 无卡
		}
		return BigDecimal.ZERO;
	}

	/**
	 * 回冲手续费 中国银行，交通银行，北京农商，建设银行有手续费，回冲的时候手续费也回冲</br>
	 * 农业银行，回冲，手续费不会回冲
	 */
	public static BigDecimal fee4Refund(AccountBaseInfo base, BigDecimal fee) {
		if (Objects.isNull(base) || !Objects.equals("农业银行", base.getBankType()))
			return fee;
		return BigDecimal.ZERO;
	}
}
