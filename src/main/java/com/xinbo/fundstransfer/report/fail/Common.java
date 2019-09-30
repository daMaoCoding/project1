package com.xinbo.fundstransfer.report.fail;

import java.text.SimpleDateFormat;

public class Common {

	public static final String WATCHER_4_BANK_BALANCE = "1";

	public static final String WATCHER_4_BANK_LOGS = "2";

	public static final String WATCHER_4_INIT_BAL = "3";

	public static final String WATCHER_4_CHECK_SUCCESS = "4";

	public static final String WATCHER_4_RESULT_EQ_3 = "5";

	public static final String WATCHER_4_KEY_WORDS = "6";

	public static final String WATCHER_4_KEY_REFUND_0 = "7";

	public static final String WATCHER_4_KEY_REFUND_1 = "8";

	public static final String WATCHER_4_KEY_REFUND_2 = "9";

	public static final String WATCHER_4_KEY_REFUND_MATCHED_IN_MEMERY = "REFUND_MATCHED_IN_MEMERY";

	public static final String WATCHER_4_KEY_IN_BANK_INCOME_TEST = "IN_BANK_INCOME_TEST";

	public static final String WATCHER_4_KEY_IN_BANK_INCOME_DUPLICATE_MATCHED = "IN_BANK_INCOME_DUPLICATE_MATCHED";

	public static final long MILLIS_CHECK_POINT = 600000;

	public static final String SEPARATOR_COMMA = ",";
	public static final String SEPARATOR_HYPHEN = "-";

	public static final ThreadLocal<SimpleDateFormat> yyyyMMddHHmmss = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		}
	};
}
