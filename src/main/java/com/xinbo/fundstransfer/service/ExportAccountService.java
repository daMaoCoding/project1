package com.xinbo.fundstransfer.service;

import java.util.Date;
import java.util.List;

public interface ExportAccountService {
	Object[] freezedOutwardLog(Integer fromId, Date startTime, Date endTime);

	Object[] searchAccountByIds(List<Integer> accountIds);
}
