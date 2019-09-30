package com.xinbo.fundstransfer;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

public class RefundUtil {

	public static boolean refund(String bankType, String summary) {
		if (StringUtils.isBlank(summary))
			return false;
		if (summary.contains("支付宝") && Objects.equals("工商银行", bankType))
			return false;
		if (summary.contains("冲正") || summary.contains("冲销") || summary.contains("抹帐") || summary.contains("冲账")
				|| summary.contains("退汇") || summary.contains("冲转") || summary.contains("退款"))
			return true;
		if (Objects.equals("工商银行", bankType)) {
			return summary.contains("退汇") || summary.contains("账号误") || summary.contains("冲");
		} else if (Objects.equals("农业银行", bankType)) {
			return false;
		} else if (Objects.equals("招商银行", bankType)) {
			return false;
		} else if (Objects.equals("建设银行", bankType)) {
			return summary.contains("账号误");
		} else if (Objects.equals("浦发银行", bankType)) {
			return summary.contains("抹帐");
		} else if (Objects.equals("民生银行", bankType)) {
			return summary.contains("户名有误");
		} else if (Objects.equals("云南农信", bankType)) {
			return false;
		} else if (Objects.equals("交通银行", bankType)) {
			return false;
		} else if (Objects.equals("中信银行", bankType)) {
			return false;
		} else if (Objects.equals("平安银行", bankType)) {
			return summary.contains("冲销");
		} else if (Objects.equals("柳州银行", bankType)) {
			return false;
		} else if (Objects.equals("中国银行", bankType)) {
			return false;
		} else if (Objects.equals("锦州银行", bankType)) {
			return false;
		} else if (Objects.equals("南京银行", bankType)) {
			return false;
		} else if (Objects.equals("包商银行", bankType)) {
			return false;
		} else if (Objects.equals("成都银行", bankType)) {
			return summary.contains("网贷往冲");
		} else if (Objects.equals("兴业银行", bankType)) {
			return false;
		} else if (Objects.equals("中原银行", bankType)) {
			return false;
		} else if (Objects.equals("天府银行", bankType)) {
			return false;
		} else if (Objects.equals("哈尔滨银行", bankType)) {
			return false;
		} else if (Objects.equals("北京农商", bankType)) {
			return false;
		} else if (Objects.equals("福建海峡银行", bankType)) {
			return false;
		} else if (Objects.equals("兰州银行", bankType)) {
			return false;
		} else if (Objects.equals("临商银行", bankType)) {
			return summary.contains("电汇退汇") || summary.contains("调账");
		} else if (Objects.equals("汉口银行", bankType)) {
			return false;
		} else if (Objects.equals("广发银行", bankType)) {
			return false;
		} else if (Objects.equals("威海市商业银行", bankType)) {
			return false;
		} else if (Objects.equals("桂林银行", bankType)) {
			return false;
		} else if (Objects.equals("广州农村商业银行", bankType)) {
			return false;
		} else if (Objects.equals("盛京银行", bankType)) {
			return false;
		} else if (Objects.equals("华夏银行", bankType)) {
			return summary.contains("冲正");
		} else if (Objects.equals("武汉农村商业银行", bankType)) {
			return false;
		} else if (Objects.equals("浙商银行", bankType)) {
			return summary.contains("冲正");
		} else if (Objects.equals("广西北部湾银行", bankType)) {
			return false;
		} else if (Objects.equals("江西银行", bankType)) {
			return false;
		} else if (Objects.equals("沧州银行", bankType)) {
			return summary.contains("冲实时汇");
		} else if (Objects.equals("徽商银行", bankType)) {
			return false;
		} else if (Objects.equals("内蒙古银行", bankType)) {
			return false;
		} else if (Objects.equals("广西农村信用社", bankType)) {
			return false;
		} else if (Objects.equals("光大银行", bankType)) {
			return false;
		} else if (Objects.equals("四川农信", bankType)) {
			return false;
		} else if (Objects.equals("廊坊银行", bankType)) {
			return false;
		} else if (Objects.equals("重庆银行", bankType)) {
			return false;
		} else if (Objects.equals("齐商银行", bankType)) {
			return false;
		} else if (Objects.equals("东营银行", bankType)) {
			return false;
		}
		return false;
	}

	public static boolean noneRefund(String summary) {
		if (StringUtils.isBlank(summary)) {
			return true;
		}
		return !(summary.contains("手续费") || summary.contains("汇费") || summary.contains("收费") || summary.contains("短信费")
				|| summary.contains("工本费") || summary.contains("年费") || summary.contains("信使费")
				|| summary.contains("开换卡费"));
	}
}
