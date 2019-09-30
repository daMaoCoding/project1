package com.xinbo.fundstransfer.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizAccountMore;
import com.xinbo.fundstransfer.domain.entity.BizOtherAccount;
import com.xinbo.fundstransfer.domain.entity.BizOtherAccountBindEntity;
import com.xinbo.fundstransfer.domain.entity.BizRebateUser;
import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.enums.InBankSubType;
import com.xinbo.fundstransfer.domain.pojo.Account;
import com.xinbo.fundstransfer.domain.repository.QuickPayRepository;
import com.xinbo.fundstransfer.service.AccountMoreService;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.CabanaService;
import com.xinbo.fundstransfer.service.HostMonitorService;
import com.xinbo.fundstransfer.service.OtherAccountBindService;
import com.xinbo.fundstransfer.service.QuickPayService;
import com.xinbo.fundstransfer.service.RebateApiService;
import com.xinbo.fundstransfer.service.RebateUserService;

@Service
public class QuickPayServiceImpl implements QuickPayService {
	@Autowired
	private QuickPayRepository quickPayRepository;
	@Autowired
	private RebateUserService rebateUserService;
	@Autowired
	private AccountMoreService accountMoreSer;
	@Autowired @Lazy
	AccountService accSer;
	@Autowired
	private HostMonitorService hostMonitorService;
	@Autowired
	private CabanaService cabanaService;
	@Autowired
	private OtherAccountBindService otherAccountBindService;
	@Autowired
	private RebateApiService rebateApiService;

	private static final Cache<Object, BizOtherAccount> QuickPayAcountCacheBuilder = CacheBuilder.newBuilder()
			.maximumSize(20000).expireAfterWrite(4, TimeUnit.DAYS).build();
	static final Logger log = LoggerFactory.getLogger(QuickPayServiceImpl.class);

	@Override
	@Transactional
	public BizOtherAccount save(BizOtherAccount data) {
		if (Objects.isNull(data)) {
			return null;
		}
		return quickPayRepository.saveAndFlush(data);
	}

	@Override
	public void flushCache(BizOtherAccount data) {
		if (Objects.nonNull(data)) {
			QuickPayAcountCacheBuilder.put(data.getAccountNo(), data);
		}

	}

	@Override
	@Transactional
	public void saveBind(Integer otherAccountId, Integer accountid) {
		quickPayRepository.saveBind(otherAccountId, accountid);
	}

	@Override
	public BizOtherAccount getFromCacheByAccountNo(String accountNo) {
		BizOtherAccount account = QuickPayAcountCacheBuilder.getIfPresent(accountNo);
		if (null == account) {
			account = quickPayRepository.findByAccountNo(accountNo);
			if (account != null)
				QuickPayAcountCacheBuilder.put(account.getAccountNo(), account);
		}
		return account;
	}

	@Override
	@Transactional
	public void deleteBind(Integer otherAccountId, Integer accountid) {
		quickPayRepository.deleteBind(otherAccountId, accountid);
	}

	@Override
	public int counts(Integer otherAccountId, Integer accountid) {
		return quickPayRepository.findCounts(otherAccountId, accountid);
	}

	@Override
	@Transactional
	public void deleteBindAll(Integer otherAccountId) {
		quickPayRepository.deleteBindAll(otherAccountId);
	}

	@Override
	@Transactional
	public void deleteById(Integer id, String accountNo) {
		QuickPayAcountCacheBuilder.invalidate(accountNo);
		quickPayRepository.deleteById(id);
	}

	@Override
	public void cleanCache() {
		QuickPayAcountCacheBuilder.invalidateAll();
	}

	@Override
	public BizOtherAccount getByUid(int uid) {
		return quickPayRepository.findByUid(uid);
	}

	@Override
	@Transactional
	public void bindingAndStatus(String rebateUserName, List<Account> accounts, String message) {
		try {
			// 兼职信息
			message = (message == null) ? "" : message;
			log.info("兼职云闪付绑定：" + rebateUserName + "；消息：" + message);
			BizRebateUser rebateUser = rebateUserService.getFromCacheByUserName(rebateUserName);
			if (null != rebateUser) {
				// 兼职绑定的云闪付
				BizOtherAccount otheracc = getByUid(Integer.parseInt(rebateUser.getUid()));
				// 兼职绑定的银行卡信息
				BizAccountMore more = accountMoreSer.getFromByUid(rebateUser.getUid());
				if (null == otheracc || null == more) {
					log.info("云闪付或兼职信息不存在：" + rebateUserName);
					return;
				}
				log.info("云闪付账号：" + otheracc.getAccountNo());
				// 需要绑定的数据
				List<BizAccount> bindAccounts = new ArrayList<>();
				for (String accId : StringUtils.trimToEmpty(more.getAccounts()).split(",")) {
					if (StringUtils.isBlank(accId) || !StringUtils.isNumeric(accId))
						continue;
					BizAccount acc = accSer.getById(Integer.valueOf(accId));
					String fristStr = "";
					String lastStr = "";
					String owner = "";
					if (Objects.nonNull(acc)) {
						// // 不是入款卡 不能绑定云闪付
						// if (acc.getType() != AccountType.InBank.getTypeId())
						// {
						// log.info("不是入款卡不能绑定：" + acc.getAccount());
						// continue;
						// }
						// 未激活的卡不能绑定
						if (acc.getStatus() == AccountStatus.Inactivated.getStatus()) {
							log.info("未激活的卡不能绑定：" + acc.getAccount() + "云闪付账号：" + otheracc.getAccountNo());
							continue;
						}
						// 账号前四位、后四位、银行名称、开户人
						fristStr = acc.getAccount().substring(0, 4);
						lastStr = acc.getAccount().substring(acc.getAccount().length() - 4, acc.getAccount().length());
						owner = acc.getOwner().substring(acc.getOwner().length() - 1, acc.getOwner().length());
					}
					log.info("more表绑定的卡号：" + acc.getAccount() + ",开户人：" + owner + ",银行类别：" + acc.getBankType()
							+ "云闪付账号：" + otheracc.getAccountNo());
					if (null != accounts && accounts.size() > 0) {
						for (Account iacc : accounts) {
							log.info("抓取云闪付绑定的卡号：" + iacc.getAccount() + ",开户人：" + (iacc.getOwner()) + ",银行类别："
									+ iacc.getBank() + "云闪付账号：" + otheracc.getAccountNo());
							String startStr = iacc.getAccount().substring(0, 4);
							String endStr = iacc.getAccount().substring(iacc.getAccount().length() - 4,
									iacc.getAccount().length());
							String ow = iacc.getOwner().substring(iacc.getOwner().length() - 1,
									iacc.getOwner().length());
							if (fristStr.equals(startStr) && lastStr.equals(endStr) && owner.equals(ow)
									&& acc.getBankType().equals(iacc.getBank())) {
								bindAccounts.add(acc);
							}
						}
					}
				}
				// 需要解绑的银行卡id
				List<Integer> unbundlingAcc = new ArrayList<>();
				// 查询这个云闪付下面绑定的银行卡,如果多余实际绑定的卡 则进行解绑
				List<BizOtherAccountBindEntity> bings = otherAccountBindService.findByOtherAccountId(otheracc.getId());
				if (bings.size() > 0) {
					for (BizOtherAccountBindEntity bind : bings) {
						int id = bind.getAccountId();
						boolean flag = true;
						for (BizAccount bindacc : bindAccounts) {
							if (id == bindacc.getId()) {
								flag = false;
								break;
							}
						}
						// 处理异常时候导致没有绑定数据 进行解绑操作
						if (flag && (null == message || "".equals(message)))
							unbundlingAcc.add(id);
					}
				}
				// 绑定
				String[] cardNos = new String[bindAccounts.size()];
				if (bindAccounts.size() > 0) {
					for (int i = 0; i < bindAccounts.size(); i++) {
						BizAccount bindacc = bindAccounts.get(i);
						cardNos[i] = bindacc.getAccount();
						// 不存在 才添加
						if (counts(otheracc.getId(), bindacc.getId()) <= 0) {
							log.info("绑定卡：" + bindacc.getAccount() + "云闪付账号：" + otheracc.getAccountNo());
							// 设置为 云闪付绑定的卡
							bindacc.setSubType(3);
							// 设置为入款卡
							bindacc.setType(AccountType.InBank.getTypeId());
							bindacc.setOutEnable((byte) 1);
							accSer.broadCast(bindacc);
							hostMonitorService.update(bindacc);
							cabanaService.updAcc(bindacc.getId());
							saveBind(otheracc.getId(), bindacc.getId());
						}

					}
					// 未激活的时候 才设置启用状态
					if (otheracc.getStatus() == 3) {
						otheracc.setStatus(1);
					}
					BizOtherAccount acc = save(otheracc);
					flushCache(acc);
					rebateApiService.activation(otheracc.getAccountNo(), more.getUid(), "2", cardNos, message);
				} else {
					// 如果是激活中的则激活失败，如果不是激活中则不改变状态
					if (otheracc.getStatus() == 3) {
						log.info("同步云闪付绑定信息：" + cardNos);
						rebateApiService.activation(otheracc.getAccountNo(), more.getUid(), "3", cardNos, message);
					} else {
						log.info("同步云闪付绑定信息：" + cardNos);
						rebateApiService.activation(otheracc.getAccountNo(), more.getUid(), "2", cardNos, message);
					}
				}
				// 解绑
				if (unbundlingAcc.size() > 0) {
					for (Integer id : unbundlingAcc) {
						BizAccount account = accSer.getById(id);
						if (null != account) {
							log.info("解绑卡：" + account.getAccount() + "云闪付账号：" + otheracc.getAccountNo());
							Date date = new Date();
							account.setSubType(InBankSubType.IN_BANK_DEFAULT.getSubType());
							account.setOutEnable((byte) 1);
							account.setStatus(AccountStatus.StopTemp.getStatus());
							account.setRemark(
									CommonUtils.genRemark(account.getRemark(), "云闪付解绑的卡需要等待两天后才能使用", date, "sys"));
							accSer.broadCast(account);
							hostMonitorService.update(account);
							cabanaService.updAcc(account.getId());
							deleteBind(otheracc.getId(), account.getId());
						} else {
							deleteBind(otheracc.getId(), id);
							log.info("解绑操作时卡id查询不到数据，直接解绑！：" + id);
						}
					}
				}
			} else {
				log.info("兼职不存在：" + rebateUserName);
			}
		} catch (Exception e) {
			log.error("同步绑定关系错误!", e);
		}

	}

	@Override
	public int getBindAccountIdNum(Integer accid){
		if(accid == null){
			return 0;
		}
		return quickPayRepository.findByAccountId(accid);
	}

}
