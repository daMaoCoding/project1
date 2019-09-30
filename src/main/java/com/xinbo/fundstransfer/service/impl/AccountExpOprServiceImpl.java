package com.xinbo.fundstransfer.service.impl;

import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.domain.DynamicSpecifications;
import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizAccountExpOpr;
import com.xinbo.fundstransfer.domain.entity.BizHost;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.repository.AccountExpOprRepository;
import com.xinbo.fundstransfer.domain.repository.AccountRepository;
import com.xinbo.fundstransfer.domain.repository.HostRepository;
import com.xinbo.fundstransfer.restful.api.OutwardTaskController;
import com.xinbo.fundstransfer.service.AccountExpOprService;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class AccountExpOprServiceImpl implements AccountExpOprService {
	private static final Logger log = LoggerFactory.getLogger(OutwardTaskController.class);
	@Autowired
	private AccountRepository accountDao;
	@Autowired
	private AccountExpOprRepository accountExpOprDao;
	@Autowired
	private HostRepository hostDao;

	@Override
	public Page<BizAccountExpOpr> findPage(Date stTm, Date edTm, Integer[] handiArray, String alias, String accLike,
			String bank, String opr, Pageable pageable) throws Exception {
		List<Integer> handiList = Objects.isNull(handiArray) || handiArray.length == 0 ? Arrays.asList(0)
				: Arrays.asList(handiArray);
		return accountExpOprDao.findByPage(stTm, edTm, handiList, alias, accLike, bank, opr, pageable);
	}

	@Override
	@Transactional
	public void record(BizAccountExpOpr expOpr) {
		List<SearchFilter> filters = new ArrayList<>();
		filters.add(new SearchFilter("accountId", SearchFilter.Operator.EQ, expOpr.getAccountId()));
		filters.add(new SearchFilter("content", SearchFilter.Operator.EQ, expOpr.getContent()));
		filters.add(new SearchFilter("clientTime", SearchFilter.Operator.EQ, expOpr.getClientTime()));
		Specification<BizAccountExpOpr> spec = DynamicSpecifications.build(BizAccountExpOpr.class,
				filters.toArray(new SearchFilter[filters.size()]));
		List<BizAccountExpOpr> finds = accountExpOprDao.findAll(spec);
		if (!CollectionUtils.isEmpty(finds)) {
			log.info("ExpOpr (record ) >>  accountId: {}  clientTime: {}  content: {} >>  already exist in db .",
					expOpr.getAccountId(), expOpr.getClientTime(), expOpr.getContent());
			return;
		}
		BizAccount account = accountDao.getOne(expOpr.getAccountId());
		if (Objects.nonNull(account)) {
			expOpr.setClientIp(account.getGps());
			if (StringUtils.isNotBlank(account.getGps())) {
				BizHost host = hostDao.findByIp(account.getGps().trim());
				if (Objects.nonNull(host)) {
					String positionInfo = String.format("%s (%s , %s)", StringUtils.trimToEmpty(host.getName()),
							StringUtils.trimToEmpty(host.getX()), StringUtils.trimToEmpty(host.getY()));
					expOpr.setClientPosition(positionInfo);
				}
			}
			expOpr.setType(1);// 密码输入时，键盘，手机 误操作
			expOpr.setCreateTime(new Date());
			accountExpOprDao.saveAndFlush(expOpr);
		} else {
			log.info("ExpOpr (record ) >>  accountId: {}  clientTime: {}  content: {} >>  the account doesn't exist .",
					expOpr.getAccountId(), expOpr.getClientTime(), expOpr.getContent());
		}
	}

	@Override
	@Transactional
	public void addOperator(Integer id, String operator, SysUser user) {
		BizAccountExpOpr opr = accountExpOprDao.findOne(id);
		if (opr == null) {
			return;
		}
		String remark = CommonUtils.genRemark(opr.getRemark(), (user.getUid() + "设置操作人为：" + operator), new Date(),
				user.getUid());
		opr.setOperator(operator);
		opr.setRemark(remark);
		accountExpOprDao.saveAndFlush(opr);
	}

	@Override
	@Transactional
	public void addRemark(Integer id, String remark, SysUser user) {
		BizAccountExpOpr opr = accountExpOprDao.findOne(id);
		if (opr == null) {
			return;
		}
		remark = CommonUtils.genRemark(opr.getRemark(), remark, new Date(), user.getUid());
		opr.setRemark(remark);
		accountExpOprDao.saveAndFlush(opr);
	}
}
