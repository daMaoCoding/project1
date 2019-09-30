package com.xinbo.fundstransfer.service.impl;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xinbo.fundstransfer.domain.repository.ExportAccountRepository;
import com.xinbo.fundstransfer.service.ExportAccountService;

@Service
public class ExportAccountServiceImpl implements ExportAccountService {

	@Autowired
	private ExportAccountRepository exportAccountRepository;

	@Override
	public Object[] searchAccountByIds(List<Integer> accountIds) {
		return exportAccountRepository.searchAccountByIds(accountIds);
	}

	@Override
	public Object[] freezedOutwardLog(Integer fromId, Date startTime, Date endTime) {
		return exportAccountRepository.freezedOutwardLog(fromId, startTime, endTime);
	}

}
