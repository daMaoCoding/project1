package com.xinbo.fundstransfer;

import com.xinbo.fundstransfer.domain.enums.AccountFlag;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;

import java.util.Objects;

public class BankLogOrderUtils {
	public static boolean reverse(AccountBaseInfo base, String bankType) {
		if (Objects.equals("工商银行", bankType)) {
			return true;
		} else if (Objects.equals("农业银行", bankType)) {
			return true;
		} else if (Objects.equals("招商银行", bankType)) {
			return true;
		} else if (Objects.equals("建设银行", bankType)) {
			return true;
		} else if (Objects.equals("浦发银行", bankType)) {
			return false;// 最新数据在第一条
		} else if (Objects.equals("民生银行", bankType)) {
			if (base.getFlag() == null || base.getFlag() == AccountFlag.PC.getTypeId()) {
				return false;// 最新数据在第一条
			} else {
				return true;// 手机需要反转
			}
		} else if (Objects.equals("云南农信", bankType)) {
			return true;
		} else if (Objects.equals("交通银行", bankType)) {
			return true;// 最新数据在第一条 2019-08-10确认
		} else if (Objects.equals("中信银行", bankType)) {
			return true;
		} else if (Objects.equals("平安银行", bankType)) {
			return true;
		} else if (Objects.equals("柳州银行", bankType)) {
			return false;// 最新数据在第一条
		} else if (Objects.equals("中国银行", bankType)) {
			return true;// 最新数据在第一条 2019-07-04 确定
		} else if (Objects.equals("锦州银行", bankType)) {
			return false;// 最新数据在第一条
		} else if (Objects.equals("南京银行", bankType)) {
			return false;/// 最新数据在第一条
		} else if (Objects.equals("包商银行", bankType)) {
			return false;// 最新数据在第一条
		} else if (Objects.equals("成都银行", bankType)) {
			return true;
		} else if (Objects.equals("兴业银行", bankType)) {
			if (base.getFlag() == null || base.getFlag() == AccountFlag.PC.getTypeId()) {
				return false;// 最新数据在第一条
			} else {
				return true;// 手机需要反转
			}
		} else if (Objects.equals("中原银行", bankType)) {
			return true;
		} else if (Objects.equals("天府银行", bankType)) {
			return true;
		} else if (Objects.equals("哈尔滨银行", bankType)) {
			return true;
		} else if (Objects.equals("北京农商", bankType)) {
			return false;// 最新数据在第一条
		} else if (Objects.equals("福建海峡银行", bankType)) {
			return true;
		} else if (Objects.equals("兰州银行", bankType)) {
			return true;
		} else if (Objects.equals("临商银行", bankType)) {
			return true;
		} else if (Objects.equals("汉口银行", bankType)) {
			return true;
		} else if (Objects.equals("广发银行", bankType)) {
			return true;
		} else if (Objects.equals("威海市商业银行", bankType)) {
			return true;
		} else if (Objects.equals("桂林银行", bankType)) {
			return true;
		} else if (Objects.equals("广州农村商业银行", bankType)) {
			return false;// 最新数据在第一条
		} else if (Objects.equals("盛京银行", bankType)) {
			return true;
		} else if (Objects.equals("华夏银行", bankType)) {
			return true;
		} else if (Objects.equals("武汉农村商业银行", bankType)) {
			return true;
		} else if (Objects.equals("浙商银行", bankType)) {
			return false;
		} else if (Objects.equals("广西北部湾银行", bankType)) {
			return true;
		} else if (Objects.equals("江西银行", bankType)) {
			return false;// 最新数据在第一条
		} else if (Objects.equals("沧州银行", bankType)) {
			return false;// 最新数据在第一条
		} else if (Objects.equals("徽商银行", bankType)) {
			return false;// 最新数据在第一条
		} else if (Objects.equals("内蒙古银行", bankType)) {
			return true;
		} else if (Objects.equals("广西农村信用社", bankType)) {
			return true;
		} else if (Objects.equals("光大银行", bankType)) {
			return true;
		} else if (Objects.equals("四川农信", bankType)) {
			return true;
		} else if (Objects.equals("廊坊银行", bankType)) {
			return true;
		} else if (Objects.equals("重庆银行", bankType)) {
			return true;
		} else if (Objects.equals("齐商银行", bankType)) {
			return true;
		} else if (Objects.equals("东营银行", bankType)) {
			return true;
		}
		return false;
	}
}
