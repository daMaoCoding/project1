package com.xinbo.fundstransfer.report.turn;

import com.xinbo.fundstransfer.domain.enums.AccountFlag;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.repository.AccountRebateRepository;
import com.xinbo.fundstransfer.domain.repository.OutwardTaskRepository;
import com.xinbo.fundstransfer.report.SystemAccountCommon;
import com.xinbo.fundstransfer.report.turn.observer.OutwardExpireAcknowledge;
import com.xinbo.fundstransfer.report.turn.observer.OutwardExpireUnknown;
import com.xinbo.fundstransfer.report.turn.observer.RebateExpireAcknowledge;
import com.xinbo.fundstransfer.report.turn.observer.RebateExpireUnknown;
import com.xinbo.fundstransfer.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Objects;
import java.util.Observable;

@Component
public class TurnHandler extends Observable {
	@Autowired
	private OutwardTaskRepository oTaskDao;
	@Autowired
	private SysUserService userSer;
	@Autowired
	private AccountService accSer;
	@Autowired
	private AllocateOutwardTaskService allocOSer;
	@Autowired
	private AccountRebateRepository accRebateDao;
	@Autowired
	private AccountRebateService accRebateSer;
	@Autowired
	private RebateApiService rebateApiSer;
	@Autowired
	private SystemAccountCommon common;
	@Autowired
	private RedisService redisSer;
	@Autowired
	private AllocateTransService allocateTransService;

	@PostConstruct
	protected void init() {
		this.addObserver(new OutwardExpireAcknowledge());
		this.addObserver(new RebateExpireAcknowledge());
		this.addObserver(new OutwardExpireUnknown());
		this.addObserver(new RebateExpireUnknown());
	}

	public void exe() throws InterruptedException {
		if (!SystemAccountCommon.checkHostRunRight()) {
			return;
		}
		this.setChanged();
		this.notifyObservers();
	}

	public RedisService getRedisSer() {
		return this.redisSer;
	}

	public SystemAccountCommon getCommon() {
		return this.common;
	}

	public OutwardTaskRepository getOTaskDao() {
		return this.oTaskDao;
	}

	public SysUserService getUserSer() {
		return this.userSer;
	}

	public AccountService getAccSer() {
		return this.accSer;
	}

	public AllocateOutwardTaskService getAllocOSer() {
		return this.allocOSer;
	}

	public AccountRebateRepository getAccRebateDao() {
		return this.accRebateDao;
	}

	public AccountRebateService getAccRebateSer() {
		return this.accRebateSer;
	}

	public RebateApiService getRebateApiSer() {
		return this.rebateApiSer;
	}

	public AllocateTransService getAllocateTransService() {
		return this.allocateTransService;
	}

	public boolean checkRefund(AccountBaseInfo target) {
		return Objects.nonNull(target) && Objects.equals(target.getFlag(), AccountFlag.REFUND.getTypeId());
	}
}
