package com.xinbo.fundstransfer.service.impl;

import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizSysErr;
import com.xinbo.fundstransfer.domain.entity.BizSysInvst;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.enums.AccountFlag;
import com.xinbo.fundstransfer.domain.enums.CurrentSystemLevel;
import com.xinbo.fundstransfer.domain.enums.SysErrStatus;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.repository.AccountRepository;
import com.xinbo.fundstransfer.domain.repository.SysErrRepository;
import com.xinbo.fundstransfer.domain.repository.SysInvstRepository;
import com.xinbo.fundstransfer.service.RedisService;
import com.xinbo.fundstransfer.service.SysErrService;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.util.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class SysErrServiceImpl implements SysErrService {
	@Autowired
	private SysErrRepository sysErrDao;
	@Autowired
	private SysInvstRepository sysInvstDao;
	@Autowired
	private RedisService redisSer;
	@Autowired
	private AccountRepository accountDao;

	@Override
	public Page<BizSysErr> findPage(Specification<BizSysErr> specification, Pageable pageable) {
		return sysErrDao.findAll(specification, pageable);
	}

	@Override
	@Transactional
	public BizSysErr save(AccountBaseInfo base, BigDecimal sysBal, BigDecimal bankBal) {
		BizSysErr err = new BizSysErr();
		err.setTarget(base.getId());
		err.setTargetHandicap(base.getHandicapId());
		err.setTargetBankType(base.getBankType());
		err.setTargetAlias(base.getAlias());
		err.setTargetFlag(Objects.isNull(base.getFlag()) ? AccountFlag.PC.getTypeId() : base.getFlag());
		err.setTargetLevel(
				Objects.isNull(base.getCurrSysLevel()) ? CurrentSystemLevel.Outter.getValue() : base.getCurrSysLevel());
		err.setBalance(sysBal);
		err.setBankBalance(bankBal);
		err.setMargin(bankBal.subtract(sysBal));
		err.setOccurTime(new Date());
		err.setStatus(SysErrStatus.Locking.getStatus());
		err.setTargetType(base.getType());
		err = sysErrDao.saveAndFlush(err);
		if (StringUtils.isBlank(err.getBatchNo())) {
			err.setBatchNo(genBatchNo(err.getId()));
			err = sysErrDao.saveAndFlush(err);
		}
		return err;
	}

	@Override
	@Transactional
	public BizSysErr save(BizSysErr err, BigDecimal sysBal, BigDecimal bankBal) {
		err.setBalance(sysBal);
		err.setBankBalance(bankBal);
		err.setMargin(bankBal.subtract(sysBal));
		return sysErrDao.saveAndFlush(err);
	}

	@Override
	@Transactional
	public void delete(Long id) {
		sysErrDao.delete(id);
	}

	@Override
	public BizSysErr findById(Long id) {
		return sysErrDao.findOne(id);
	}

	@Override
	public BizSysErr findNotFinishedByAccId(Long accId) {
		return sysErrDao.findNotFinishedByAccId(accId);
	}

	@Override
	@Transactional
	public BizSysErr finished(BizSysErr err, SysUser operator, String remark) {
		if (Objects.isNull(err) || SysErrStatus.finish(err.getStatus()))
			return err;
		Date start = Objects.isNull(err.getCollectTime()) ? err.getOccurTime() : err.getCollectTime();
		long consuming = System.currentTimeMillis() - start.getTime();
		err.setStatus(SysErrStatus.FinishedNormarl.getStatus());
		err.setConsumeTime(consuming / 1000);
		String opr = Objects.isNull(operator) ? "系统" : operator.getUid();
		err.setRemark(CommonUtils.genRemark(err.getRemark(), remark, new Date(), opr));
		return sysErrDao.saveAndFlush(err);
	}

	@Override
	public void lock(Long errId, SysUser operator) throws Exception {
		BizSysErr err = sysErrDao.getOne(errId);
		if (Objects.isNull(err)) {
			throw new Exception("该记录不存在");
		}
		if (Objects.equals(err.getStatus(), SysErrStatus.Locked.getStatus())) {
			throw new Exception("该记录已锁定");
		}
		if (SysErrStatus.finish(err.getStatus())) {
			throw new Exception("该记录已被排查");
		}
		sysErrDao.updInf(errId, SysErrStatus.Locking.getStatus(), SysErrStatus.Locked.getStatus(), operator.getId(),
				new Date(), null, err.getRemark());
	}

	@Override
	@Transactional
	public void unlock(Long errId, SysUser operator) throws Exception {
		BizSysErr err = sysErrDao.getOne(errId);
		if (Objects.isNull(err)) {
			throw new Exception("该记录不存在");
		}
		if (Objects.equals(err.getStatus(), SysErrStatus.Locking.getStatus())) {
			throw new Exception("该记录未锁定");
		}
		if (SysErrStatus.finish(err.getStatus())) {
			throw new Exception("该记录已被排查");
		}
		if (System.currentTimeMillis() - err.getCollectTime().getTime() <= 3600000
				&& !Objects.equals(err.getCollector(), operator.getId())) {
			throw new Exception("1个小时内,非本人不能解锁");
		}
		sysErrDao.updInf(errId, SysErrStatus.Locked.getStatus(), SysErrStatus.Locking.getStatus(), null, null, null,
				err.getRemark());
	}

	@Override
	@Transactional
	public void clean(Long errorId) {
		BizSysErr err = sysErrDao.findOne(errorId);
		sysErrDao.delete(errorId);
		List<BizSysInvst> invstList = sysInvstDao.findByErrorId(errorId);
		if (!CollectionUtils.isEmpty(invstList)) {
			sysInvstDao.delete(invstList);
		}
		if (Objects.isNull(err) || Objects.isNull(err.getTarget())) {
			return;
		}
		redisSer.getStringRedisTemplate().boundHashOps(RedisKeys.SYS_ACC_RUNNING)
				.delete(String.valueOf(err.getTarget()));
		redisSer.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_SYS_INIT).delete(String.valueOf(err.getTarget()));
		BizAccount acc = accountDao.findById2(err.getTarget());
		if (Objects.isNull(acc)) {
			return;
		}
		acc.setBalance(acc.getBankBalance());
		accountDao.saveAndFlush(acc);

	}

	private String genBatchNo(long errId) {
		String d = String.valueOf(System.currentTimeMillis());
		return String.format("B%s%s%d", d.substring(0, d.length() - String.valueOf(errId).length()), "S", errId);
	}
}
