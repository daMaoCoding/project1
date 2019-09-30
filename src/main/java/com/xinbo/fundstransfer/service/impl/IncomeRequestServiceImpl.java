package com.xinbo.fundstransfer.service.impl;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.assign.Constants;
import com.xinbo.fundstransfer.component.net.http.RequestBodyParser;
import com.xinbo.fundstransfer.component.net.http.v2.HttpClientNew;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.*;
import com.xinbo.fundstransfer.domain.SearchFilter.Operator;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.*;
import com.xinbo.fundstransfer.domain.pojo.*;
import com.xinbo.fundstransfer.domain.repository.BankLogRepository;
import com.xinbo.fundstransfer.domain.repository.BizThirdRequestRepository;
import com.xinbo.fundstransfer.domain.repository.IncomeRequestRepository;
import com.xinbo.fundstransfer.domain.repository.QueryNoCountDao;
import com.xinbo.fundstransfer.exception.IncomRequestAckException;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import com.xinbo.fundstransfer.report.SystemAccountManager;
import com.xinbo.fundstransfer.report.exception.SysLogInvstErrorException;
import com.xinbo.fundstransfer.runtime.MemCacheUtils;
import com.xinbo.fundstransfer.service.*;
import com.xinbo.fundstransfer.unionpay.ysf.service.YSFService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.bouncycastle.util.Integers;
import org.hibernate.query.internal.QueryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class IncomeRequestServiceImpl implements IncomeRequestService {
	private static final Logger log = LoggerFactory.getLogger(IncomeRequestServiceImpl.class);
	private static final String CUSTOMER_SENDMESSAGE_FAILED = "fail";
	private static final String CUSTOMER_SENDMESSAGE_SUCCEED = "succeed";
	@Autowired
	@Lazy
	private IncomeRequestRepository incomeRequestRepository;
	@Autowired
	@Lazy
	private AccountService accountService;
	@Autowired
	@Lazy
	private HandicapService handicapService;
	@Autowired
	@Lazy
	private LevelService levelService;
	@Autowired
	@Lazy
	TransactionLogService transactionLogService;
	@Autowired
	@Lazy
	private BankLogService bankLogService;
	@Autowired
	@Lazy
	private RedisService redisService;
	@Autowired
	@Lazy
	RequestBodyParser requestBodyParser;
	@Autowired
	@Lazy
	SysUserService userService;
	@PersistenceContext
	private EntityManager entityManager;
	@Autowired
	@Lazy
	private QueryNoCountDao queryNoCountDao;
	@Autowired
	@Lazy
	IncomeRequestService incomeRequestService;
	@Autowired
	@Lazy
	BizThirdRequestRepository bizThirdRequestRepository;
	@Autowired
	@Lazy
	BankLogRepository bankLogRepository;
	@Autowired
	@Lazy
	private YSFService ysfService;
	@Autowired
	SystemAccountManager systemAccountManager;

	private static ObjectMapper mapper = new ObjectMapper();

	@Override
	@Transactional
	public List<BizIncomeRequest> saveCollection(List<BizIncomeRequest> list) {
		return incomeRequestRepository.saveAll(list);
	}

	@Override
	public BizIncomeRequest findByOrderNoAndHandicapAndAmount(String orderNo, Integer handicapId, BigDecimal amount) {
		return incomeRequestRepository.findDistinctByOrderNoAndAmountAndHandicap(orderNo, amount, handicapId);
	}

	@Transactional
	@Override
	public void updateRealName(String realName, Long id) {
		if (StringUtils.isBlank(realName) || null == id)
			return;
		incomeRequestRepository.updateRealNameById(id, realName);
	}

	/**
	 * 通过 缓存信息 查询一个单号对应多个收款账号的 入款记录
	 *
	 * @param str
	 * @return
	 */
	@Override
	public List<BizIncomeRequest> findByCacheStrMultiToAccount(String[] str) {
		log.debug("通过 缓存信息 查询一个单号对应多个收款账号的 入款记录 参数:{}", str);
		try {
			if (!ObjectUtils.isEmpty(str)) {
				int len = str.length;
				String orderNo = str[0];
				Integer handicap = Integer.valueOf(str[1]);
				Integer type = Integer.valueOf(str[2]);
				BigDecimal amount = new BigDecimal(str[3]);
				int start = 4;
				List<String> toAccount = new ArrayList<>(len - start);
				for (int i = start; i < len; i++) {
					String toAcc = str[i];
					toAccount.add(toAcc);
				}

				String sql = " from BizIncomeRequest where orderNo=:orderNo and handicap=:handicap and type=:type and amount=:amount and  status=0 and toAccount in (:toAccount)  and updateTime is null and time_consuming is null ";
				Query query = entityManager.createQuery(sql, BizIncomeRequest.class);
				query.setParameter("orderNo", orderNo);
				query.setParameter("handicap", handicap);
				query.setParameter("type", type);
				query.setParameter("amount", amount);
				query.setParameter("toAccount", toAccount);

				List<BizIncomeRequest> res = query.getResultList();
				log.debug("查询结果:{}", res.toString());
				return res;
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public List<Integer> findFinishedToIdByUserId(Integer userId) {
		String sql = " select to_id from biz_income_request where type=103 and status=1 ";
		if (null != userId) {
			sql += "and operator='" + userId + "'";
		}
		List<Integer> finished = entityManager.createQuery(sql).getResultList();
		return finished;
	}

	@Override
	@Transactional(rollbackOn = Exception.class)
	public BizIncomeRequest saveAndFlush(BizIncomeRequest incomeRequest) {
		return incomeRequestRepository.saveAndFlush(incomeRequest);
	}

	/***
	 * 下发任务 下发失败 按钮 根据第三方账号id 和 下发账号id 下发金额 手续费 查询记录
	 *
	 * @param
	 * @param toId
	 * @param amount
	 * @return
	 */
	@Override
	public BizIncomeRequest findOneThirdDrawRecord(Integer toId, Integer amount) {
		return incomeRequestRepository.findFirstByToIdAndStatusOrderByCreateTimeDesc(toId,
				IncomeRequestStatus.Matching.getStatus());
	}

	@Override
	public List<ThirdDrawFailStatisticOutputDTO> findThirdDrawFailDetail(ThirdDrawFailStatisticInputDTO inputDTO) {
		if (inputDTO != null) {
			StringBuilder sql = new StringBuilder(
					"SELECT  DATE_FORMAT(create_time ,'%Y-%m-%d') as time,from_account,to_account, from_id,  amount, operator  FROM biz_income_request  WHERE type = 103   AND STATUS = 3 ");
			if (null != inputDTO.getThirdAccountId()) {
				sql.append(" and from_id='" + inputDTO.getThirdAccountId() + "'");
			}
			if (null != inputDTO.getHandicaps() && inputDTO.getHandicaps().length > 0) {
				int len = inputDTO.getHandicaps().length;
				if (len == 1) {
					sql.append(" and handicap='" + inputDTO.getHandicaps()[0] + "'");
				} else {
					String[] handicaps = inputDTO.getHandicaps();
					sql.append(" and handicap in(");
					for (int i = 0; i < len; i++) {
						sql.append("'").append(handicaps[i]).append("'");
						if (i < len - 1) {
							sql.append(",");
						} else {
							sql.append(")");
						}
					}
				}
			}

			if (StringUtils.isNotBlank(inputDTO.getDrawToAccount())) {
				sql.append(" AND to_account= '").append(StringUtils.trim(inputDTO.getDrawToAccount())).append("'");
			}
			if (StringUtils.isNotBlank(inputDTO.getThirdAccount())) {
				sql.append(" AND from_account= '").append(StringUtils.trim(inputDTO.getDrawToAccount())).append("'");
			}
			if (null != inputDTO.getOperator()) {
				sql.append(" AND operator= '").append(inputDTO.getOperator()).append("'");
			}
			if (StringUtils.isNotBlank(inputDTO.getStartTime())) {
				sql.append(" AND DATE_FORMAT(create_time ,'%Y-%m-%d')>=' ")
						.append(StringUtils.trim(inputDTO.getStartTime())).append("'");
			}
			if (StringUtils.isNotBlank(inputDTO.getEndTime())) {
				sql.append(" AND DATE_FORMAT(create_time ,'%Y-%m-%d') <=' ")
						.append(StringUtils.trim(inputDTO.getEndTime())).append("'");
			}
			if (StringUtils.isNotBlank(inputDTO.getStartAmount())) {
				sql.append(" AND amount >=' ").append(inputDTO.getStartAmount()).append("'");
			}
			if (StringUtils.isNotBlank(inputDTO.getEndAmount())) {
				sql.append(" AND amount <=' ").append(inputDTO.getEndAmount()).append("'");
			}
			sql.append("  ORDER BY create_time DESC;  ");
			Query query = entityManager.createNativeQuery(sql.toString());

			List list = query.getResultList();
			List<ThirdDrawFailStatisticOutputDTO> res = Lists.newArrayList();
			// time,from_account,to_account, from_id, amount, operator
			list.stream().forEach(p1 -> {
				Object[] p = (Object[]) p1;
				ThirdDrawFailStatisticOutputDTO outputDTO = new ThirdDrawFailStatisticOutputDTO();
				outputDTO.setTime(p[0].toString());
				outputDTO.setDrawToAccount(p[2].toString());
				if (p[1] != null) {
					String thirdAccount = p[1].toString();
					int len1 = thirdAccount.length();
					if (len1 > 2) {
						thirdAccount = thirdAccount.substring(0, 2) + "******" + thirdAccount.substring(len1 - 2);
					} else {
						thirdAccount = thirdAccount + "******" + thirdAccount.substring(len1 - 1);
					}
					outputDTO.setThirdAccount(thirdAccount);
				} else {
					outputDTO.setThirdAccount("");
				}
				if (null != p[3]) {
					AccountBaseInfo baseInfo = accountService.getFromCacheById(Integer.valueOf(p[3].toString()));
					if (null != baseInfo) {
						outputDTO.setThirdName(baseInfo.getBankName());
					} else {
						outputDTO.setThirdName("");
					}
				} else {
					outputDTO.setThirdName("");
				}
				if (null != p[4]) {
					outputDTO.setAmount(new BigDecimal(p[4].toString()));
				} else {
					outputDTO.setAmount(BigDecimal.ZERO);
				}
				if (null != p[5]) {
					SysUser sysUser = userService.findFromCacheById(Integer.valueOf(p[5].toString()));
					if (null != sysUser) {
						outputDTO.setOperator(sysUser.getUid());
					} else {
						outputDTO.setOperator("");
					}
				} else {
					outputDTO.setOperator("");
				}
				res.add(outputDTO);
			});
			return res;
		}
		return Lists.newLinkedList();
	}

	@Override
	public List<ThirdDrawFailStatisticOutputDTO> findThirdDrawFail(ThirdDrawFailStatisticInputDTO inputDTO) {
		if (inputDTO != null) {
			StringBuilder sql;
			if (inputDTO.getQueryAccount() != null && inputDTO.getQueryAccount().intValue() == 1) {
				// 第三方统计
				sql = new StringBuilder(
						"SELECT DATE_FORMAT(i.create_time ,\\'%Y-%m-%d\\') as time ,a.bank_name,a.account,a.id ,COUNT(i.from_id) as count  "
								+ "  from biz_income_request i INNER JOIN biz_account a  ON i.from_id = a.id and i.type=103 ");
			} else {
				sql = new StringBuilder(
						"select operator ,from_id,from_account,to_account,create_time, amount,to_id from biz_income_request where type=103 and status=3  ");
			}
			// and DATE_FORMAT(i.create_time ,'%Y-%m-%d') ='"+inputDTO.getStartTime() +"'
			// GROUP BY i.from_id,time

			String[] handicaps = inputDTO.getHandicaps();
			int len = handicaps.length;
			if (len == 1) {
				sql.append(" and i.handicap ='" + handicaps[0] + "'");
			} else {
				sql.append(" and i.handicap in (");
				for (int i = 0; i < len; i++) {
					if (i < len - 1) {
						sql.append(handicaps[i]).append(",");
					} else {
						sql.append(handicaps[i]).append(")");
					}
				}
			}
			if (StringUtils.isNotBlank(StringUtils.trim(inputDTO.getStartTime()))) {
				sql.append(" and DATE_FORMAT(i.create_time ,\\'%Y-%m-%d\\') >='"
						+ StringUtils.trim(inputDTO.getStartTime()) + "'");
			}
			if (StringUtils.isNotBlank(StringUtils.trim(inputDTO.getEndTime()))) {
				sql.append(" and DATE_FORMAT(i.create_time ,\\'%Y-%m-%d\\') <='"
						+ StringUtils.trim(inputDTO.getEndTime()) + "'");
			}
			if (StringUtils.isNotBlank(StringUtils.trim(inputDTO.getCreateTime()))) {
				sql.append(" and DATE_FORMAT(i.create_time ,\\'%Y-%m-%d\\') ='"
						+ StringUtils.trim(inputDTO.getCreateTime()) + "'");
			}
			if (StringUtils.isNotBlank(inputDTO.getThirdAccount())) {
				sql.append(" and a.account ='" + inputDTO.getThirdAccount() + "'");
			}
			if (StringUtils.isNotBlank(inputDTO.getThirdName())) {
				sql.append(" and a.bank_name ='" + inputDTO.getThirdName() + "'");
			}
			sql.append(" GROUP BY i.from_id , time  ");
			Query query = entityManager.createNativeQuery(sql.toString());

			List list = query.getResultList();
			List<ThirdDrawFailStatisticOutputDTO> res = Lists.newArrayList();
			// time ,a.bank_name,a.account,a.id,count
			list.stream().forEach(p1 -> {
				Object[] p = (Object[]) p1;
				ThirdDrawFailStatisticOutputDTO outputDTO = new ThirdDrawFailStatisticOutputDTO();
				outputDTO.setTime(p[0].toString());
				outputDTO.setFromId(Integer.valueOf(p[3].toString()));
				if (p[2] != null) {
					String thirdAccount = p[2].toString();
					int len1 = thirdAccount.length();
					if (len1 > 2) {
						thirdAccount = thirdAccount.substring(0, 2) + "******" + thirdAccount.substring(len1 - 2);
					} else {
						thirdAccount = thirdAccount + "******" + thirdAccount;
					}
					outputDTO.setThirdAccount(thirdAccount);
				} else {
					outputDTO.setThirdAccount("");
				}
				if (p[1] != null) {
					String thirdName = p[1].toString();
					outputDTO.setThirdName(thirdName);
				} else {
					outputDTO.setThirdName("");
				}
				if (null != p[4]) {
					outputDTO.setFailCounts(Integer.valueOf(p[4].toString()));
				} else {
					outputDTO.setFailCounts(0);
				}
				res.add(outputDTO);
			});
			return res;
		}
		return Lists.newLinkedList();
	}

	@Override
	@Transactional(rollbackOn = Exception.class)
	public BizIncomeRequest saveThirdDraw(BizIncomeRequest thirdRequest) {
		return incomeRequestRepository.saveAndFlush(thirdRequest);
	}

	/**
	 * 通过 缓存信息 查询 单个订单
	 *
	 * @param cacheStr
	 *            订单号#盘口id#类型#金额
	 * @return
	 */
	@Override
	public BizIncomeRequest findOneByCacheStr(String[] cacheStr) {
		try {
			if (!ObjectUtils.isEmpty(cacheStr)) {
				String sql = " from BizIncomeRequest where orderNo=:orderNo and handicap=:handicap and type=:type and amount=:amount and  status=0 and updateTime is null and time_consuming is null ";
				Query query = entityManager.createQuery(sql, BizIncomeRequest.class);
				query.setParameter("orderNo", cacheStr[0]);
				query.setParameter("handicap", Integer.valueOf(cacheStr[1]));
				query.setParameter("type", Integer.valueOf(cacheStr[2]));
				query.setParameter("amount", new BigDecimal(cacheStr[3]));
				List<BizIncomeRequest> res = query.getResultList();
				log.debug("查询结果:{}", res.toString());
				if (!CollectionUtils.isEmpty(res)) {
					return res.get(0);
				}
				return null;
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public List<BizIncomeRequest> findByOrderNo(String orderNo) {
		return incomeRequestRepository.findByOrderNo(orderNo);
	}

	/**
	 * 查询微信账号记录:根据状态,是否有流水,是否有订单,盘口,层级,账号
	 */
	@Override
	public List<Object> findAlipayAccount(PageRequest pageRequest, Integer handicap, Integer level, String account,
			List<Integer> status, Integer type) {
		return incomeRequestRepository.findAlipayAccount(pageRequest, handicap, level, account, status, type);
	}

	/**
	 * 查询微信账号总记录:根据状态,是否有流水,是否有订单,盘口,层级,账号
	 */
	@Override
	public Long countAlipayAccount(Integer handicap, Integer level, String account, List<Integer> status,
			Integer type) {
		return incomeRequestRepository.countAlipayAccount(handicap, level, account, status, type);
	}

	/**
	 * 取消订单并通知平台
	 */
	@Transactional
	@Override
	public boolean cancelAndCallFlatform(Integer status, Long incomeRequestId, String remark, String orderNo,
			Integer handicap, Integer accountId, String memberCode, SysUser loginUser) {
		// 调用取消
		BizHandicap bizHandicap = handicapService.findFromCacheById(handicap);
		okhttp3.RequestBody requestBody;
		requestBody = requestBodyParser.buildRequestBody(bizHandicap.getCode(), orderNo, remark + "新平台取消");
		boolean flag = false;
		IncomeAuditWs noticeEntity = new IncomeAuditWs();
		noticeEntity.setAccountId(accountId);
		noticeEntity.setOwner(null == loginUser ? "sys" : loginUser.getUid());
		HttpClientNew.getInstance().getPlatformServiceApi().depositCancel(requestBody).subscribe((data2) -> {
			log.info("调用新平台取消返回结果, response: {}, orders:({})", data2, orderNo);
			// 若成功，则更新数据库
			if (data2.getStatus() == 1) {
				updateIncomeReqAfterCancel(incomeRequestId, remark, loginUser);
				noticeEntity.setIncomeAuditWsFrom(IncomeAuditWsEnum.SucceedCanceled.ordinal());
				noticeEntity.setMessage("取消成功!");
			} else {
				noticeEntity.setIncomeAuditWsFrom(IncomeAuditWsEnum.FailedCanceled.ordinal());
				noticeEntity.setMessage(data2.getMessage());
				return;
			}
		}, (e) -> {
			log.info("新平台取消失败:{} ", e);
			noticeEntity.setMessage(e.getMessage());
		});
		ObjectMapper mapper = new ObjectMapper();
		try {
			redisService.convertAndSend(RedisTopics.INCOME_REQUEST, mapper.writeValueAsString(noticeEntity));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		if (noticeEntity.getIncomeAuditWsFrom().equals(IncomeAuditWsEnum.SucceedCanceled.ordinal())) {
			flag = true;
		}
		return flag;
	}

	/**
	 * 先提流水再补提单金额不一致的，以流水金额为准通知平台补单子，补单成功之后取数据库查询新单子自动匹配再取消原来的单子
	 */
	@Transactional
	@Override
	public boolean matchForInconsistentAmount(BizHandicap bizHandicap, String name, String memberAccount, String amount,
			AccountBaseInfo accountBaseInfo, Integer type, String remark, String remarkWrap, BizBankLog bankLog,
			BizIncomeRequest incomeRequest, SysUser loginUser) {
		if (accountBaseInfo == null || bizHandicap == null || StringUtils.isBlank(name)
				|| StringUtils.isBlank(memberAccount) || StringUtils.isBlank(memberAccount)
				|| new BigDecimal(amount).equals(BigDecimal.ZERO) || StringUtils.isBlank(remark)) {
			return false;
		}
		String createTime = CommonUtils.getNowDate();
		int bankType = BankEnums.findDesc(accountBaseInfo.getBankType()).getCode();
		int subType = accountBaseInfo.getSubType();
		// 新平台
		Integer pfTypeSub = accountService.getPfTypeSubVal(accountBaseInfo.getId());
		log.info("金额不一致 匹配 先自动补单:账号id {} pfTypeSub:{} subType:{} ", accountBaseInfo.getId(), pfTypeSub,
				accountBaseInfo.getSubType());
		okhttp3.RequestBody requestBody = requestBodyParser.buildDepositRequestBody(pfTypeSub, bizHandicap.getCode(),
				memberAccount, new BigDecimal(amount), name, accountBaseInfo.getAccount(), remark, type, createTime,
				accountBaseInfo.getProvince(), accountBaseInfo.getCity(), bankType, subType);
		HttpClientNew httpClientNew = HttpClientNew.getInstance();
		ThreadLocal<Boolean> ret = new ThreadLocal<>();
		ret.set(false);
		try {
			rx.Observable<SimpleResponseData> observable = httpClientNew.getPlatformServiceApi().deposit(requestBody);
			if (observable != null) {
				observable.subscribe(data -> {
					if (data.getStatus() == 1) {
						// 新平台补提单之后把新单子推送到系统
						String newOrderNo = data.getMessage();
						log.info("新平台返回订单号:orderNo:{}", newOrderNo);
						if (StringUtils.isBlank(newOrderNo)) {
							log.info("新平台返回订单号为空:orderNo:{}", newOrderNo);
							return;
						}
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						BizIncomeRequest bizIncomeRequest1 = findByHandicapAndOrderNo(bizHandicap.getId(), newOrderNo);
						if (bizIncomeRequest1 == null) {
							long start = System.currentTimeMillis();
							int count = 0;
							Random random = new Random();
							while (true) {
								// 循环查询,防止订单未入库
								bizIncomeRequest1 = findByHandicapAndOrderNo(bizHandicap.getId(), newOrderNo);
								if (bizIncomeRequest1 != null) {
									break;
								}
								// 休眠防止查询频繁对数据库造成压力
								try {
									Thread.sleep((random.nextInt(5) + 1) * 100);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								count++;
								// 防止由于某种因素导致数据未入库,而产生死循环 查询20停止循环
								if (count > 20) {
									log.info("新平台金额不一致匹配未查询到新单子,查询20停止循环");
									break;
								}
							}
							log.info("循环查询耗时:" + (System.currentTimeMillis() - start) + " 毫秒");
							if (bizIncomeRequest1 != null) {
								// 延迟2秒中发起匹配
								// 新平台 匹配新订单不锁定
								try {
									Thread.sleep(2000);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								BizBankLog bizBankLog = bankLogService.get(bankLog.getId());
								if (bizBankLog != null
										&& bizBankLog.getStatus() == BankLogStatus.Matching.getStatus()) {
									ack(bankLog, bizIncomeRequest1, remark, remarkWrap, loginUser.getId());
								}
								ret.set(true);
								// 新平台 取消旧订单不锁定
								cancelAndCallFlatform(null, incomeRequest.getId(), remark, incomeRequest.getOrderNo(),
										bizHandicap.getId(), null, null, loginUser);
							} else {
								log.info("新平台金额不一致匹配未查询到新单子");
								return;
							}
						} else {
							// 新平台 匹配新订单不锁定 但需要延迟2-3秒
							try {
								Thread.sleep(2000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							BizBankLog bizBankLog = bankLogService.get(bankLog.getId());
							if (bizBankLog != null && bizBankLog.getStatus() == BankLogStatus.Matching.getStatus()) {
								ack(bankLog, bizIncomeRequest1, remark, remarkWrap, loginUser.getId());
							}
							ret.set(true);
							// 新平台 取消旧订单不锁定
							cancelAndCallFlatform(null, incomeRequest.getId(), remark, incomeRequest.getOrderNo(),
									bizHandicap.getId(), null, null, loginUser);

						}
					} else {
						log.info("新平台金额不一致先生成订单失败:{}", data.getMessage());
					}
				});
			}
		} catch (Exception e) {
			log.error("新平台金额不一致匹配失败:{}", e);
		}
		return ret.get();
	}

	@Override
	public BizIncomeRequest findById(Long id) {
		return incomeRequestRepository.findById2(id);
	}

	@Override
	@Transactional
	public BizIncomeRequest getBizIncomeRequestByIdForUpdate(Long id) {
		return incomeRequestRepository.getBizIncomeRequestByIdForUpdate(id);
	}

	/**
	 * 条件查询第三方系统提单 以便于统计金额
	 */
	@Override
	public BigDecimal[] findThirdSumAmountByConditions(Integer status, Integer[] handicap, Integer level, String member,
			String orderNo, String toAccount, BigDecimal fromMoney, BigDecimal toMoney, String startTime,
			String endTime) {
		return getSumAmountForThird(status, toAccount, handicap, level, orderNo, member, fromMoney, toMoney, startTime,
				endTime);
	}

	/**
	 * 查询第三方
	 */
	@Override
	public Page<BizThirdRequest> findThirdMatchedOrUnMatchNoCount(Integer type, Integer[] handicap, Integer level,
			String member, String orderNo, String toAccount, BigDecimal fromMoney, BigDecimal toMoney, String startTime,
			String endTime, PageRequest pageRequest) {

		Specification<BizThirdRequest> specification = wrapSpecificForThirdRequest(type, toAccount, handicap, level,
				orderNo, member, fromMoney, toMoney, startTime, endTime);
		return queryNoCountDao.findAll(specification, pageRequest, BizThirdRequest.class);
	}

	/**
	 * 查询第三方 总记录数
	 */
	@Override
	public Long findThirdMatchedOrUnMatchCount(Integer status, Integer[] handicap, Integer level, String member,
			String orderNo, String toAccount, BigDecimal fromMoney, BigDecimal toMoney, String startTime,
			String endTime, PageRequest pageRequest) {

		return getCountForThird(status, toAccount, handicap, level, orderNo, member, fromMoney, toMoney, startTime,
				endTime);
	}

	/**
	 * 查询公司入款(已匹配 已取消) 无总记录数
	 */
	@Override
	public Page<BizIncomeRequest> findMatchedOrCanceledCompanyInPageNocount(Integer status, Integer[] handicap,
			Integer level, Integer[] toAccountIds, String member, BigDecimal fromMoney, BigDecimal toMoney,
			String startTime, String endTime, String orderNo, Integer operator, PageRequest pageRequest) {
		Integer[] type = new Integer[1];// 银行入款 支付宝(暂时无法抓流水) 微信(暂时无法抓流水)
		type[0] = IncomeRequestType.PlatFromBank.getType();
		Specification<BizIncomeRequest> specification = wrapSpecificForCompanyIn(status, type, handicap, level,
				toAccountIds, member, null, fromMoney, toMoney, startTime, endTime, orderNo, operator);
		return queryNoCountDao.findAll(specification, pageRequest, BizIncomeRequest.class);
	}

	/**
	 * 查询公司入款(已匹配 已取消) 分页带总记录数
	 */
	@Override
	public String findMatchedOrCanceledCompanyInCount(Integer status, Integer[] handicap, Integer level,
			Integer[] toAccountIds, String member, BigDecimal fromMoney, BigDecimal toMoney, String startTime,
			String endTime, String orderNo, Integer operator) {
		Integer[] type = new Integer[1];// 银行入款 支付宝(暂时无法抓流水) 微信(暂时无法抓流水)
		type[0] = IncomeRequestType.PlatFromBank.getType();
		String count = getCountForCompanyIn(status, type, toAccountIds, handicap, level, orderNo, member, null,
				fromMoney, toMoney, startTime, endTime, operator);
		return count;
	}

	/**
	 * 查找公司入款(已匹配 已取消) 总金额
	 */
	@Override
	public String findMatchedOrCanceledCompanyInSum(Integer status, Integer[] handicap, Integer level,
			Integer[] toAccountIds, String member, BigDecimal fromMoney, BigDecimal toMoney, String startTime,
			String endTime, String orderNo, Integer operator) {
		Integer[] type = new Integer[1];
		type[0] = IncomeRequestType.PlatFromBank.getType();
		return getSumAmountForCompanyIn(status, type, toAccountIds, handicap, level, orderNo, member, null, fromMoney,
				toMoney, startTime, endTime, operator);
	}

	/**
	 * 查询正在匹配公司入款 入款请求 提单
	 */
	@Override
	public Page<BizIncomeRequest> findCompanyInNoCount(String memberRealName, Integer status, Integer[] accountId,
			Integer level, String orderNo, String member, BigDecimal fromMoney, BigDecimal toMoney, String startTime,
			String endTime, PageRequest pageRequest) {
		Integer type[] = new Integer[] { IncomeRequestType.PlatFromBank.getType() };
		Specification<BizIncomeRequest> specification = wrapSpecificForCompanyIn(status, type, null, level, accountId,
				member, memberRealName, fromMoney, toMoney, startTime, endTime, orderNo, null);
		return queryNoCountDao.findAll(specification, pageRequest, BizIncomeRequest.class);
	}

	/**
	 * 获取正在匹配 公司入款总记录
	 */
	@Override
	public String findMatchingRequestCount(String memberRealName, int status, Integer[] accountId, Integer level,
			String orderNo, String member, String fromMoney, String toMoney, String startTime, String endTime,
			PageRequest pageRequest) {
		BigDecimal fromM = null;
		if (fromMoney != null && !"".equals(fromMoney)) {
			fromM = new BigDecimal(fromMoney);
		}
		BigDecimal toM = null;
		if (toMoney != null && !"".equals(toMoney)) {
			toM = new BigDecimal(toMoney);
		}
		Integer type[] = new Integer[1];
		type[0] = IncomeRequestType.PlatFromBank.getType();
		String count = getCountForCompanyIn(status, type, accountId, null, level, orderNo, member, memberRealName,
				fromM, toM, startTime, endTime, null);
		return count;
	}

	/**
	 * 封装第三方查询条件
	 */
	private Specification<BizThirdRequest> wrapSpecificForThirdRequest(Integer type, String toAccout,
			Integer[] handicap, Integer level, String orderNo, String member, BigDecimal fromMoney, BigDecimal toMoney,
			String startTime, String endTime) {
		return (root, criteriaQuery, criteriaBuilder) -> {
			Predicate predicate = null;
			final Path<String> toAccountR = root.get("toAccount");
			final Path<String> orderNoR = root.get("orderNo");
			final Path<Integer> levelR = root.get("level");
			final Path<Integer> handicapR = root.get("handicap");
			final Path<Date> ackTimeR = root.get("ackTime");
			final Path<Date> createTimeR = root.get("createTime");
			final Path<String> memberUserNameR = root.get("memberUserName");
			final Path<BigDecimal> amountR = root.get("amount");
			if (fromMoney != null) {
				predicate = addAndPredicate(criteriaBuilder, null,
						criteriaBuilder.greaterThanOrEqualTo(amountR, fromMoney));
			}
			if (toMoney != null) {
				predicate = addAndPredicate(criteriaBuilder, predicate,
						criteriaBuilder.lessThanOrEqualTo(amountR, toMoney));
			}
			if (StringUtils.isNotBlank(toAccout)) {
				predicate = addAndPredicate(criteriaBuilder, predicate,
						criteriaBuilder.like(toAccountR, "%" + toAccout + "%"));
			}
			if (StringUtils.isNotBlank(member)) {
				predicate = addAndPredicate(criteriaBuilder, predicate,
						criteriaBuilder.like(memberUserNameR, "%" + member + "%"));
			}
			if (level != null) {
				predicate = addAndPredicate(criteriaBuilder, predicate, criteriaBuilder.equal(levelR, level));
			}
			if (handicap != null && handicap.length > 0) {
				if (handicap.length == 1)
					predicate = addAndPredicate(criteriaBuilder, predicate,
							criteriaBuilder.equal(handicapR, handicap[0]));
				else
					predicate = addAndPredicate(criteriaBuilder, predicate, handicapR.in(handicap));
			}
			if (StringUtils.isNotBlank(orderNo)) {
				predicate = addAndPredicate(criteriaBuilder, predicate,
						criteriaBuilder.like(orderNoR, "%" + orderNo + "%"));
			}
			if (StringUtils.isNotBlank(startTime)) {
				Date startTimeD = CommonUtils.string2Date(startTime);
				if (type == IncomeRequestStatus.Matched.getStatus().intValue()) {
					predicate = addAndPredicate(criteriaBuilder, predicate,
							criteriaBuilder.greaterThanOrEqualTo(ackTimeR, startTimeD));
				} else {
					predicate = addAndPredicate(criteriaBuilder, predicate,
							criteriaBuilder.greaterThanOrEqualTo(createTimeR, startTimeD));
				}
			}
			if (StringUtils.isNotBlank(endTime)) {
				Date endTimeD = CommonUtils.string2Date(endTime);
				if (type == IncomeRequestStatus.Matched.getStatus().intValue()) {
					predicate = addAndPredicate(criteriaBuilder, predicate,
							criteriaBuilder.lessThanOrEqualTo(ackTimeR, endTimeD));
				} else {
					predicate = addAndPredicate(criteriaBuilder, predicate,
							criteriaBuilder.lessThanOrEqualTo(createTimeR, endTimeD));
				}
			}
			if (type != null) {
				// 第三方入款 已对账和未对账：区分标准是否有更新时间
				if (type == IncomeRequestStatus.Matched.getStatus().intValue()) {
					predicate = addAndPredicate(criteriaBuilder, predicate, criteriaBuilder.and(ackTimeR.isNotNull()));
				} else if (type == IncomeRequestStatus.Matching.getStatus().intValue()) {
					predicate = addAndPredicate(criteriaBuilder, predicate, criteriaBuilder.and(ackTimeR.isNull()));
				}
			}
			criteriaQuery
					.multiselect(root.get("id"), root.get("handicap"), root.get("level"), root.get("memberUserName"),
							orderNoR, toAccountR, amountR, root.get("createTime"), ackTimeR, root.get("remark"))
					.getOrderList();
			return predicate;
		};
	}

	/**
	 * 获取第三方 已对账 未对账 总金额
	 */
	private BigDecimal[] getSumAmountForThird(Integer type, String toAccout, Integer[] handicap, Integer level,
			String orderNo, String member, BigDecimal fromMoney, BigDecimal toMoney, String startTime, String endTime) {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Tuple> query = criteriaBuilder.createQuery(Tuple.class);
		Root<BizThirdRequest> root = query.from(BizThirdRequest.class);
		Predicate predicate = criteriaBuilder.conjunction();
		final Path<String> toAccountR = root.get("toAccount");
		final Path<String> orderNoR = root.get("orderNo");
		final Path<Integer> levelR = root.get("level");
		final Path<Integer> handicapR = root.get("handicap");
		final Path<Date> ackTimeR = root.get("ackTime");
		final Path<Date> createTimeR = root.get("createTime");
		final Path<String> memberUserNameR = root.get("memberUserName");
		final Path<BigDecimal> amountR = root.get("amount");
		final Path<BigDecimal> feeR = root.get("fee");
		List<Expression<Boolean>> expressions = predicate.getExpressions();
		Expression<BigDecimal> sumTrxAmt = criteriaBuilder.sum(amountR);
		Expression<BigDecimal> sumTrxfee = criteriaBuilder.sum(feeR);
		if (type != null) { // 第三方入款 已对账和未对账：区分标准是否有更新时间
			if (type == IncomeRequestStatus.Matched.getStatus().intValue()) {
				expressions.add(criteriaBuilder.and(ackTimeR.isNotNull()));
			}
			if (type == IncomeRequestStatus.Matching.getStatus().intValue()) {
				expressions.add(criteriaBuilder.and(ackTimeR.isNull()));
			}
		}

		if (StringUtils.isNotBlank(startTime)) {
			Date startTimeD = CommonUtils.string2Date(startTime);
			if (type == IncomeRequestStatus.Matched.getStatus().intValue()) {
				expressions.add(criteriaBuilder.greaterThanOrEqualTo(ackTimeR, startTimeD));
			} else {
				expressions.add(criteriaBuilder.greaterThanOrEqualTo(createTimeR, startTimeD));
			}

		}
		if (StringUtils.isNotBlank(endTime)) {
			Date endTimeD = CommonUtils.string2Date(endTime);
			if (type == IncomeRequestStatus.Matched.getStatus().intValue()) {
				expressions.add(criteriaBuilder.lessThanOrEqualTo(ackTimeR, endTimeD));
			} else {
				expressions.add(criteriaBuilder.lessThanOrEqualTo(createTimeR, endTimeD));
			}
		}
		if (fromMoney != null) {
			expressions.add(criteriaBuilder.greaterThanOrEqualTo(amountR, fromMoney));
		}
		if (toMoney != null) {
			expressions.add(criteriaBuilder.lessThanOrEqualTo(amountR, toMoney));
		}
		if (StringUtils.isNotBlank(member)) {
			expressions.add(criteriaBuilder.like(memberUserNameR, "%" + member + "%"));
		}
		if (StringUtils.isNotBlank(toAccout)) {
			expressions.add(criteriaBuilder.like(toAccountR, toAccout));
		}
		if (level != null) {
			expressions.add(criteriaBuilder.equal(levelR, level));
		}
		if (handicap != null && handicap.length > 0) {
			if (handicap.length == 1)
				expressions.add(criteriaBuilder.equal(handicapR, handicap[0]));
			else
				expressions.add(handicapR.in(handicap));
		}
		if (StringUtils.isNotBlank(orderNo)) {
			expressions.add(criteriaBuilder.like(orderNoR, "%" + orderNo + "%"));
		}
		query.where(predicate);
		query.multiselect(sumTrxAmt, sumTrxfee);
		Object[] objArray = entityManager.createQuery(query).getSingleResult().toArray();
		return new BigDecimal[] { (BigDecimal) objArray[0], (BigDecimal) objArray[1] };
	}

	/**
	 * 获取第三方 已对账 未对账 总记录
	 */
	private Long getCountForThird(Integer type, String toAccout, Integer[] handicap, Integer level, String orderNo,
			String member, BigDecimal fromMoney, BigDecimal toMoney, String startTime, String endTime) {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
		Root<BizThirdRequest> root = query.from(BizThirdRequest.class);
		Predicate predicate = criteriaBuilder.conjunction();
		final Path<Long> idR = root.get("id");
		final Path<String> toAccountR = root.get("toAccount");
		final Path<String> orderNoR = root.get("orderNo");
		final Path<Integer> levelR = root.get("level");
		final Path<Integer> handicapR = root.get("handicap");
		final Path<Date> ackTimeR = root.get("ackTime");
		final Path<Date> createTimeR = root.get("createTime");
		final Path<String> memberUserNameR = root.get("memberUserName");
		final Path<BigDecimal> amountR = root.get("amount");
		List<Expression<Boolean>> expressions = predicate.getExpressions();
		Expression<Long> count = criteriaBuilder.count(idR);

		if (type != null) { // 第三方入款 已对账和未对账：区分标准是否有更新时间
			if (type == IncomeRequestStatus.Matched.getStatus().intValue()) {
				expressions.add(criteriaBuilder.and(ackTimeR.isNotNull()));
			}
			if (type == IncomeRequestStatus.Matching.getStatus().intValue()) {
				expressions.add(criteriaBuilder.and(ackTimeR.isNull()));
			}
		}

		if (StringUtils.isNotBlank(startTime)) {
			Date startTimeD = CommonUtils.string2Date(startTime);
			if (type == IncomeRequestStatus.Matched.getStatus().intValue()) {
				expressions.add(criteriaBuilder.greaterThanOrEqualTo(ackTimeR, startTimeD));
			} else {
				expressions.add(criteriaBuilder.greaterThanOrEqualTo(createTimeR, startTimeD));
			}
		}
		if (StringUtils.isNotBlank(endTime)) {
			Date endTimeD = CommonUtils.string2Date(endTime);
			if (type == IncomeRequestStatus.Matched.getStatus().intValue()) {
				expressions.add(criteriaBuilder.lessThanOrEqualTo(ackTimeR, endTimeD));
			} else {
				expressions.add(criteriaBuilder.lessThanOrEqualTo(createTimeR, endTimeD));
			}
		}
		if (fromMoney != null) {
			expressions.add(criteriaBuilder.greaterThanOrEqualTo(amountR, fromMoney));
		}
		if (toMoney != null) {
			expressions.add(criteriaBuilder.lessThanOrEqualTo(amountR, toMoney));
		}
		if (StringUtils.isNotBlank(member)) {
			expressions.add(criteriaBuilder.like(memberUserNameR, "%" + member + "%"));
		}
		if (StringUtils.isNotBlank(toAccout)) {
			expressions.add(criteriaBuilder.like(toAccountR, toAccout));
		}
		if (level != null) {
			expressions.add(criteriaBuilder.equal(levelR, level));
		}
		if (handicap != null && handicap.length > 0) {
			if (handicap.length == 1)
				expressions.add(criteriaBuilder.equal(handicapR, handicap[0]));
			else
				expressions.add(handicapR.in(handicap));
		}
		if (StringUtils.isNotBlank(orderNo)) {
			expressions.add(criteriaBuilder.like(orderNoR, "%" + orderNo + "%"));
		}
		query.where(predicate);
		query.select(count);
		Long result = entityManager.createQuery(query).getSingleResult();
		result = (null == result ? 0L : result);
		return result;
	}

	/**
	 * 获取公司入款总金额--正匹配 已匹配 已取消
	 */
	@Override
	public String getSumAmountForCompanyIn(Integer status, Integer[] type, Integer[] toAccountIds, Integer[] handicap,
			Integer level, String orderNo, String member, String payMan, BigDecimal fromMoney, BigDecimal toMoney,
			String startTime, String endTime, Integer operator) {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<BigDecimal> query = criteriaBuilder.createQuery(BigDecimal.class);
		Root<BizIncomeRequest> root = query.from(BizIncomeRequest.class);
		Predicate predicate = criteriaBuilder.conjunction();
		final Path<Integer> toAccountR = root.get("toId");
		final Path<String> memberRealNameR = root.get("memberRealName");
		final Path<String> orderNoR = root.get("orderNo");
		final Path<Integer> levelR = root.get("level");
		final Path<Integer> handicapR = root.get("handicap");
		final Path<Date> createTimeR = root.get("createTime");
		final Path<Integer> statusR = root.get("status");
		final Path<Integer> typeR = root.get("type");
		final Path<String> memberUserNameR = root.get("memberUserName");
		final Path<BigDecimal> amountR = root.get("amount");
		final Path<Integer> operatorR = root.get("operator");
		List<Expression<Boolean>> expressions = predicate.getExpressions();
		Expression<BigDecimal> sumTrxAmt = criteriaBuilder.sum(amountR);
		if (StringUtils.isNotBlank(startTime)) {
			Date startTimeD = CommonUtils.string2Date(startTime);
			expressions.add(criteriaBuilder.greaterThanOrEqualTo(createTimeR, startTimeD));
		}
		if (StringUtils.isNotBlank(endTime)) {
			Date endTimeD = CommonUtils.string2Date(endTime);
			expressions.add(criteriaBuilder.lessThanOrEqualTo(createTimeR, endTimeD));
		}
		if (toAccountIds != null && toAccountIds.length > 0) {
			if (toAccountIds.length > 1)
				expressions.add(toAccountR.in(toAccountIds));
			if (toAccountIds.length == 1)
				expressions.add(criteriaBuilder.equal(toAccountR, toAccountIds[0]));
		}
		if (status != null) {
			expressions.add(criteriaBuilder.equal(statusR, status));
		}
		if (type != null && type.length > 0) {
			if (type.length == 1)
				expressions.add(criteriaBuilder.equal(typeR, type[0]));
			else
				expressions.add(typeR.in(type));
		}

		if (fromMoney != null) {
			expressions.add(criteriaBuilder.greaterThanOrEqualTo(amountR, fromMoney));
		}
		if (toMoney != null) {
			expressions.add(criteriaBuilder.lessThanOrEqualTo(amountR, toMoney));
		}
		if (StringUtils.isNotBlank(member)) {
			expressions.add(criteriaBuilder.like(memberUserNameR, "%" + member + "%"));
		}
		if (StringUtils.isNotBlank(payMan)) {
			expressions.add(criteriaBuilder.like(memberRealNameR, "%" + payMan + "%"));
		}

		if (level != null) {
			expressions.add(criteriaBuilder.equal(levelR, level));
		}
		if (handicap != null && handicap.length > 0) {
			if (handicap.length == 1) {
				expressions.add(criteriaBuilder.equal(handicapR, handicap[0]));
			} else {
				expressions.add(handicapR.in(handicap));
			}

		}
		if (StringUtils.isNotBlank(orderNo)) {
			expressions.add(criteriaBuilder.equal(orderNoR, orderNo));
		}
		if (operator != null) {
			expressions.add(criteriaBuilder.equal(operatorR, operator));
		}
		query.where(predicate);
		query.select(sumTrxAmt);
		BigDecimal result = entityManager.createQuery(query).getSingleResult();
		result = (null == result ? BigDecimal.ZERO : result);
		return result.toString();
	}

	private Predicate addAndPredicate(final CriteriaBuilder criteriaBuilder, final Predicate oldPredicate,
			final Predicate newPredicate) {
		return oldPredicate != null ? criteriaBuilder.and(oldPredicate, newPredicate) : newPredicate;
	}

	@Override
	public Page<BizIncomeRequest> findAll(Specification<BizIncomeRequest> specification, Pageable pageable)
			throws Exception {
		Page<BizIncomeRequest> dataToPage = incomeRequestRepository.findAll(specification, pageable);
		for (BizIncomeRequest incomeRequest : dataToPage.getContent()) {
			bindExtForIncomeRequest(incomeRequest);
		}
		return dataToPage;
	}

	@Override
	public BigDecimal[] findAmountAndFeeByTotal(SearchFilter[] filterToArray) {
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Tuple> query = cb.createTupleQuery();
		Root<BizIncomeRequest> root = query.from(BizIncomeRequest.class);
		Predicate[] predicateArray = DynamicPredicate.build(cb, query, root, BizIncomeRequest.class, filterToArray);
		query.multiselect(cb.sum(root.<BigDecimal>get("amount")), cb.sum(root.<BigDecimal>get("fee")));
		query.where(predicateArray);
		Object[] objArray = entityManager.createQuery(query).getSingleResult().toArray();
		return new BigDecimal[] { (BigDecimal) objArray[0], (BigDecimal) objArray[1] };
	}

	@Override
	public BizIncomeRequest get(Long id) {
		BizIncomeRequest incomeVO = incomeRequestRepository.findOne(id);
		try {
			bindExtForIncomeRequest(incomeVO);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return incomeVO;
	}

	@Override
	public List<BizIncomeRequest> findByIdList(List<Long> idList) {
		List<BizIncomeRequest> dataList = incomeRequestRepository.findByIdIn(idList);
		for (BizIncomeRequest req : dataList) {
			try {
				bindExtForIncomeRequest(req);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return dataList;
	}

	@Override
	public BizIncomeRequest findByHandicapAndOrderNo(int handicap, String orderNo) {
		return incomeRequestRepository.findByHandicapAndOrderNo(handicap, orderNo);
	}

	/**
	 * @param o
	 * @param type
	 *            如果type是true 表示的是系统定时任务执行调用 如果其他操作失败则需要重新把订单放入redis false表示其他调用
	 * @return
	 */
	@Transactional
	@Override
	public BizIncomeRequest save(BizIncomeRequest o, boolean needSaveFirst) {
		if (null == o) {
			log.info(" 参数为空");
			return o;
		}
		if (needSaveFirst) {
			log.debug("Income[DB] save orderNo: {},amount:{},remark:{},type:{},toAccount:{}", o.getOrderNo(),
					o.getAmount(), o.getRemark(), IncomeRequestType.findByType(o.getType()).getMsg(), o.getToAccount());
			o = incomeRequestRepository.saveAndFlush(o);
		}
		boolean matchRes;
		if (o.getType() != null
				&& (o.getType().equals(IncomeRequestType.PlatFromAli.getType())
						|| o.getType().equals(IncomeRequestType.PlatFromWechat.getType()))
				&& o.getStatus() != null && o.getStatus().equals(IncomeRequestStatus.Matched.getStatus())) {
			log.info("微信 或者支付宝入款 状态已匹配  入款类型 type:{}, 单号 orderNo:{}",
					IncomeRequestType.findByType(o.getType()).getMsg(), o.getOrderNo());
			return o;
		}
		// 是否补提单
		boolean mendOrder = StringUtils.isNotBlank(o.getRemark())
				&& (o.getRemark().startsWith("补提单") || o.getRemark().contains("补提单"));
		if (mendOrder) {
			log.debug("补提单订单，人工审，订单号：{}", o.getOrderNo());
			IncomeAuditWs noticeEntity = new IncomeAuditWs();
			noticeEntity.setIncomeAuditWsFrom(IncomeAuditWsEnum.FromIncomeReq.ordinal());
			noticeEntity.setAccount(o.getToAccount());
			noticeEntity.setAccountId(o.getToId());
			noticeEntity.setMessage("");
			// 广播消息通知前端有新的记录，browser通过websocket事件刷新内容
			try {
				log.info("补提单redis广播发消息给前端：");
				redisService.convertAndSend(RedisTopics.INCOME_REQUEST, mapper.writeValueAsString(noticeEntity));
				// 整数进行匹配、且对方名字不能超过三个字
				if (StringUtils.isNotBlank(o.getMemberRealName())
						&& o.getAmount().intValue() == o.getAmount().floatValue()
						&& o.getMemberRealName().length() <= 3) {
					matchRes = match(o, 2);
					log.info("自动匹配结果 :{} ,补提单订单号:{} ", matchRes, o.getOrderNo());
				}
			} catch (JsonProcessingException e) {
				log.error("补提单redis广播发消息给前端：" + e);
			}
		} else {
			matchRes = match(o, 1);
			log.info("自动匹配结果 :{} ,订单号:{} ", matchRes, o.getOrderNo());
		}
		return o;
	}

	/**
	 * 单纯保存入库
	 *
	 * @param o
	 * @return
	 */
	@Override
	@Transactional(rollbackOn = Exception.class)
	public String saveOnly(BizIncomeRequest o) {
		log.info("入款单 入库 参数  orderNo: {},amount:{},remark:{},type:{},toAccount:{}", o.getOrderNo(), o.getAmount(),
				o.getRemark(), IncomeRequestType.findByType(o.getType()).getMsg(), o.getToAccount());
		String res = "ok";
		try {
			if (Objects.nonNull(o) && Objects.nonNull(o.getToAccount()) && o.getToAccount().contains("#")
					&& Objects.nonNull(o.getType()) && IncomeRequestType.isPlatform(o.getType())) {
				// # 表示平台入款单包含多个收款账号
				String toAccounts[] = o.getToAccount().split("#");
				List<BizIncomeRequest> toMatchIncomeReqList = new ArrayList<>();
				for (int i = 0, len = toAccounts.length; i < len; i++) {
					BizHandicap bizHandicap = handicapService.findFromCacheById(o.getHandicap());
					AccountBaseInfo account1 = accountService.getFromCacheByHandicapIdAndAccount(bizHandicap.getId(),
							toAccounts[i]);
					// {"handicap":"99","orderNo":"I396723526768131O99","amount":1.470,"token":"fd9376650a73b4d79795358f19c50e42",
					// "type":3,"createTime":"2019-02-09 16:34:36",
					// "toAccount":"13212312313212#625910091921880",
					// "username":"we12345","level":"L00005","realname":"第三方","fee":"0.0"}
					BizIncomeRequest incomeRequest = new BizIncomeRequest();
					incomeRequest.setHandicap(bizHandicap.getId());
					incomeRequest.setOrderNo(o.getOrderNo());
					incomeRequest.setAmount(o.getAmount());
					incomeRequest.setType(o.getType());
					incomeRequest.setCreateTime(o.getCreateTime());
					incomeRequest.setMemberUserName(o.getMemberUserName());
					incomeRequest.setMemberRealName(o.getMemberRealName());
					incomeRequest.setFee(o.getFee());
					incomeRequest.setToAccount(toAccounts[i]);
					incomeRequest.setToId(account1.getId());
					incomeRequest.setStatus(IncomeRequestStatus.Matching.getStatus());
					incomeRequest.setLevel(o.getLevel());
					incomeRequest.setLevelName(o.getLevelName());
					incomeRequest.setToAccountBank(account1.getBankName());
					toMatchIncomeReqList.add(incomeRequest);
				}
				toMatchIncomeReqList = incomeRequestRepository.save(toMatchIncomeReqList);
				log.info("收款账号长度 : {},收款账号:{},生成的订单大小 size:{},生成的订单:{}", toAccounts.length, toAccounts,
						toMatchIncomeReqList.size(), toMatchIncomeReqList);
				return res;
			}
			o = incomeRequestRepository.saveAndFlush(o);
			log.info("保存入款订单 结果:{}", o);
			return res;
		} catch (Exception e) {
			log.error("入款单 入库保存异常:", e);
			res = e.getLocalizedMessage();
		}
		return res;
	}

	@Transactional
	@Override
	public BizIncomeRequest update(BizIncomeRequest entity) {
		return incomeRequestRepository.saveAndFlush(entity);
	}

	@Transactional
	@Override
	public void cancelOrder(Integer handicap, List<String> orderNoList) {
		incomeRequestRepository.cancelOrder(handicap, orderNoList);
	}

	@Transactional
	@Override
	public void delete(Long id) {
		incomeRequestRepository.delete(id);
	}

	/**
	 * 工具抓取到流水,入库之后查询订单匹配
	 *
	 * @param parms
	 *            顺序:parms[0]
	 *            账号，parms[1]交易时间，parms[2]系统设置匹配时间范围，parms[3]状态0，状态1，金额，查询类型，账号类型
	 * @return
	 */
	@Override
	public List<BizIncomeRequest> findAll(Object[] parms) {
		try {
			SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			if (Integer.parseInt(parms[6].toString()) != 3) {
				log.debug("入款、下发流水匹配>>FromAccount:{} startTime:{} endTime:{}  Amount:{} ",
						Integer.parseInt(parms[0].toString()),
						null == parms[2] ? sd.format((DateUtils.addHours((Date) parms[1], -12)))
								: sd.format(
										(DateUtils.addHours((Date) parms[1], -Integer.parseInt(parms[2].toString())))),
						null == parms[2] ? sd.format((DateUtils.addHours((Date) parms[1], Integer.parseInt("12"))))
								: sd.format((Date) parms[1]),
						Float.parseFloat(parms[5].toString()));
				// (DateUtils.addHours((Date) parms[1],
				// Integer.parseInt(parms[2].toString())))
				// null == parms[2]表示下发卡

				return incomeRequestRepository.findAllByParms(Integer.parseInt(parms[0].toString()),
						null == parms[2] ? sd.format((DateUtils.addHours((Date) parms[1], -12)))
								: sd.format(
										(DateUtils.addHours((Date) parms[1], -Integer.parseInt(parms[2].toString())))),
						null == parms[2] ? sd.format((DateUtils.addHours((Date) parms[1], Integer.parseInt("13"))))
								: sd.format((Date) parms[1]),
						Integer.parseInt(parms[3].toString()), Integer.parseInt(parms[4].toString()),
						new BigDecimal(parms[5].toString()), Integer.parseInt(parms[6].toString()),
						Integer.parseInt(parms[7].toString()));// (DateUtils.addHours((Date)
				// parms[1],
				// Integer.parseInt(parms[2].toString())))
			} else {
				log.debug("第三方下发或者平台转账流水匹配>>FromAccount:{} startTime:{} endTime:{}  Amount:{} ",
						Integer.parseInt(parms[0].toString()),
						null == parms[2] ? sd.format((DateUtils.addHours((Date) parms[1], -12)))
								: sd.format((DateUtils.addHours((Date) parms[1], -1))),
						null == parms[2] ? sd.format((DateUtils.addHours((Date) parms[1], Integer.parseInt("12"))))
								: sd.format(
										(DateUtils.addHours((Date) parms[1], Integer.parseInt(parms[2].toString())))),
						Float.parseFloat(parms[5].toString()));

				return incomeRequestRepository.findAllByParmsByType(Integer.parseInt(parms[0].toString()),
						null == parms[2] ? sd.format((DateUtils.addHours((Date) parms[1], -12)))
								: sd.format((DateUtils.addHours((Date) parms[1], -1))),
						null == parms[2] ? sd.format((DateUtils.addHours((Date) parms[1], Integer.parseInt("13"))))
								: sd.format(
										(DateUtils.addHours((Date) parms[1], Integer.parseInt(parms[2].toString())))),
						Integer.parseInt(parms[3].toString()), Integer.parseInt(parms[4].toString()),
						new BigDecimal(parms[5].toString()), Integer.parseInt(parms[7].toString()));
			}
		} catch (NumberFormatException e) {
			log.error("查询异常:", e);
		}
		return null;
	}

	/**
	 * 1.1.38 锁定money在12小时内不能再次使用
	 *
	 * @param handicapCode
	 *            盘口编码
	 * @param cardNo
	 *            卡号
	 * @param money
	 *            金额
	 */
	@Override
	public void moneyBeUsed(Integer handicapCode, String cardNo, Number money) {
		if (Objects.isNull(handicapCode) || Objects.isNull(cardNo) || Objects.isNull(money)) {
			return;
		}
		log.info("moneyBeUsed param: oid:{},cardNo:{},money:{}", handicapCode, cardNo, money);
		HttpClientNew.getInstance().getPlatformServiceApi()
				.moneyBeUsed(requestBodyParser.moneyBeUsedRequestBody(handicapCode, cardNo, money)).subscribe(res -> {
					if (Objects.nonNull(res) && res.getCode() == 200 && Objects.nonNull(res.getData())
							&& res.getData()) {
						log.info("moneyBeUsed succeed:{}", res.getData());
					} else {
						log.info("moneyBeUsed failed:res:{},code:{},data:{}", res, res.getCode(), res.getData());
					}
				}, e -> log.error("moneyBeUsed ,error:", e));
	}

	/**
	 * 向平台确认入款请求
	 *
	 * @param bankLog
	 *            银行流水
	 * @param incomeRequest
	 *            入款请求
	 * @param remark
	 *            备注信息
	 * @param confirmor
	 *            确认人
	 */
	@Retryable(value = {
			IncomRequestAckException.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
	@Transactional(value = TxType.REQUIRES_NEW, rollbackOn = Exception.class)
	@Override
	public boolean ack(BizBankLog bankLog, BizIncomeRequest incomeRequest, String remark, String remarkWrap,
			Integer confirmor) {
		// new and old api compatibility
		AtomicBoolean res = new AtomicBoolean(true);
		String handicap1 = handicapService.findFromCacheById(incomeRequest.getHandicap()).getCode();
		String handicap2 = handicapService
				.findFromCacheById(accountService.getFromCacheById(bankLog.getFromAccount()).getHandicapId()).getCode();
		if (StringUtils.isNotBlank(handicap1) && StringUtils.isNotBlank(handicap2) && !handicap1.equals(handicap2)) {
			log.info("订单和流水盘口不一致，无法匹配!");
			return res.get();
		}
		AtomicBoolean ackException = new AtomicBoolean(false);
		// 新系统直接确认，不需锁定
		try {
			HttpClientNew.getInstance().getPlatformServiceApi()
					.depositAck(requestBodyParser.buildRequestBody(handicap1, incomeRequest.getOrderNo(), remark))
					.subscribe(o -> {
						log.info(
								"(new)Income acknowledged success-> response : orderNo: {}, 返回结果:{},Status: {},Message:{}",
								incomeRequest.getOrderNo(), o, o != null ? o.getStatus() : null,
								o != null ? o.getMessage() : null);
						ObjectMapper mapper = new ObjectMapper();
						IncomeAuditWs noticeEntity = new IncomeAuditWs();
						// 返回 1 表示匹配成功 成功匹配消息类型 3
						noticeEntity.setAccountId(bankLog.getFromAccount());
						noticeEntity.setIncomeAuditWsFrom(IncomeAuditWsEnum.FromMatched.ordinal());
						if (o != null) {
							if (o.getStatus() == 1) {
								// 新公司入款匹配之后释放金额
								AccountBaseInfo accountBaseInfo = accountService
										.getFromCacheById(bankLog.getFromAccount());
								if (CommonUtils.checkNewInComeEnabled(accountBaseInfo.getHandicapId())) {
									ysfService.recycleRandNum(accountBaseInfo.getAccount(), bankLog.getAmount());
								}
								noticeEntity.setMessage("1");
								// 确认成功需要插入系统交易流水，同时DB触发器会同步修改入款请求状态及更新时间，以及匹配的银行流水状态
								boolean ret = insertTransactionLog(bankLog, incomeRequest, confirmor, remarkWrap);
								log.info("insertTransactionLog result:{}, bankLogId:{},incomeReqId:{},orderNo:{}", ret,
										bankLog.getId(), incomeRequest.getToId(), incomeRequest.getOrderNo());
								res.set(ret);
							} else {
								noticeEntity.setMessage(o.getMessage());
								res.set(false);
							}
						} else {
							res.set(false);
						}
						// 匹配成功与否都广播消息通知前端有新的记录，browser通过websocket事件刷新内容
						String message = null;
						try {
							message = mapper.writeValueAsString(noticeEntity);
						} catch (JsonProcessingException e) {
							log.error("" + e);
						}
						if (StringUtils.isNotBlank(message)) {
							redisService.convertAndSend(RedisTopics.INCOME_REQUEST, message);
						}
					}, e -> {
						log.error("(new)Income acknowledged error. orderNo:{},exception:{} ",
								incomeRequest.getOrderNo(), e.getLocalizedMessage());
						ackException.set(true);
					});
		} catch (Exception e) {
			log.error(" ack error : ", e);
			res.set(false);
		}
		if (ackException.get()) {
			throw new IncomRequestAckException("平台确认失败");
		}
		return res.get();
	}

	@Recover
	public boolean recore(IncomRequestAckException e, BizBankLog bankLog, BizIncomeRequest incomeRequest, String remark,
			String remarkWrap, Integer confirmor) {
		String info = CommonUtils.genSysMsg4WS(confirmor, SystemWebSocketCategory.System,
				"向平台确认失败。 " + e.getLocalizedMessage());
		redisService.convertAndSend(RedisTopics.BROADCAST, info);
		return false;
	}

	/**
	 * 广播消息通知前端
	 */
	private void sendMessage(SysUser user, String message) {
		// 广播消息通知前端
		String info = CommonUtils.genSysMsg4WS(user != null ? user.getId() : null, SystemWebSocketCategory.System,
				message);
		redisService.convertAndSend(RedisTopics.BROADCAST, info);
	}

	@Override
	@Transactional
	public BizIncomeRequest reject2CurrSys(long incomeId, String remark, SysUser operator) {
		BizIncomeRequest entity = incomeRequestRepository.findById2(incomeId);
		entity.setStatus(IncomeRequestStatus.Canceled.getStatus());
		entity.setRemark(CommonUtils.genRemark(entity.getRemark(), remark, new Date(), operator.getUid()));
		entity = incomeRequestRepository.saveAndFlush(entity);
		return entity;
	}

	/**
	 * 新增入款订单记录时，调用匹配操作
	 *
	 * @param o
	 *            入款订单封装实体类
	 * @param type
	 *            match匹配类型 type=1,表示会员提单匹配 type=2 表示补提单过来的单子匹配
	 */
	@Transactional(rollbackOn = Exception.class)
	boolean match(BizIncomeRequest o, int type) {
		AtomicBoolean res = new AtomicBoolean(true);
		try {
			if (Objects.isNull(o)) {
				log.info("新增入款订单记录匹配参数空");
				return res.get();
			}
			IncomeAuditWs noticeEntity = new IncomeAuditWs();
			// 查流水，匹配规则，带小数的则去匹配，否则直接人工审
			if (o.getAmount().intValue() == o.getAmount().floatValue() && type != 2) {
				noticeEntity.setIncomeAuditWsFrom(IncomeAuditWsEnum.FromIncomeReq.ordinal());
				noticeEntity.setAccount(o.getToAccount());
				noticeEntity.setAccountId(o.getToId());
				noticeEntity.setMessage("");
				// 广播消息通知前端有新的记录，browser通过websocket事件刷新内容
				String json = null;
				try {
					json = mapper.writeValueAsString(noticeEntity);
				} catch (JsonProcessingException e) {
					log.error("入款整数金额 人工审核 JsonProcessingException : ", e);
				}
				if (StringUtils.isNotBlank(json)) {
					log.info("入款整数金额 redis 广播发消息给前端 :{}", json);
					redisService.convertAndSend(RedisTopics.INCOME_REQUEST, json);
				}
				return res.get();
			}
			List<BizIncomeRequest> incomeResquests = incomeRequestRepository.findIncomeCounts(o.getToId(),
					o.getAmount());
			if (null != incomeResquests && incomeResquests.size() > 1
					&& o.getAmount().intValue() != o.getAmount().floatValue()) {
				log.info("存在同一张入款卡有多笔相同金额未匹配的单,金额:{},订单:{}", o.getAmount(), o.getOrderNo());
				return res.get();
			}
			ObjectMapper mapper = new ObjectMapper();
			AccountBaseInfo bizAccount = accountService.getFromCacheById(o.getToId());
			// 入款与流水记录匹配最大时间间隔（小时）
			int validIntervalTimeHour = Integer
					.parseInt(MemCacheUtils.getInstance().getSystemProfile().getOrDefault("INCOME_MATCH_HOURS", "2"));
			// 规则：入款帐号相同--》入款金额相等--》入款时间在设置范围
			Date tradingStart = IncomeRequestType.isPlatform(o.getType())
					? CommonUtils.string2Date(CommonUtils.getDateFormat2Str(o.getCreateTime()))
					: CommonUtils.string2Date(CommonUtils
							.getDateFormat2Str(DateUtils.addHours(o.getCreateTime(), -validIntervalTimeHour)));
			Date tradingEnd = CommonUtils.string2Date(
					CommonUtils.getDateFormat2Str(DateUtils.addHours(o.getCreateTime(), validIntervalTimeHour)));
			Specification<BizBankLog> specification = null;
			if (type == 1) {
				// 小数正常匹配
				specification = DynamicSpecifications.build(null, BizBankLog.class,
						new SearchFilter(IncomeRequestType.isPlatform(o.getType()) ? "tradingTime" : "createTime",
								Operator.GTE, tradingStart),
						new SearchFilter(IncomeRequestType.isPlatform(o.getType()) ? "tradingTime" : "createTime",
								Operator.LTE, tradingEnd),
						new SearchFilter("fromAccount", Operator.EQ, o.getToId()),
						new SearchFilter("status", Operator.EQ, BankLogStatus.Matching.getStatus()),
						new SearchFilter("amount", Operator.EQ, o.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP)));
			} else if (type == 2) {
				// 补提单的整数，匹配把对方姓名加上去查找
				specification = DynamicSpecifications.build(null, BizBankLog.class,
						new SearchFilter(IncomeRequestType.isPlatform(o.getType()) ? "tradingTime" : "createTime",
								Operator.GTE, tradingStart),
						new SearchFilter(IncomeRequestType.isPlatform(o.getType()) ? "tradingTime" : "createTime",
								Operator.LTE, tradingEnd),
						new SearchFilter("fromAccount", Operator.EQ, o.getToId()),
						new SearchFilter("status", Operator.EQ, BankLogStatus.Matching.getStatus()),
						new SearchFilter("amount", Operator.EQ, o.getAmount()),
						new SearchFilter("toAccountOwner", Operator.EQ, o.getMemberRealName()));
			}

			List<BizBankLog> logs = bankLogService.findAll(specification);
			if (type == 1 && CollectionUtils.isEmpty(logs)) {
				// 如果银行流水交易时间是 00:00:00 则根据流水创建时间查询.(不论是交易时间和创建时间都应该大于订单创建时间)
				specification = DynamicSpecifications.build(null, BizBankLog.class,
						new SearchFilter("createTime", Operator.GTE, tradingStart),
						new SearchFilter("createTime", Operator.LTE, tradingEnd),
						new SearchFilter("fromAccount", Operator.EQ, o.getToId()),
						new SearchFilter("status", Operator.EQ, BankLogStatus.Matching.getStatus()),
						new SearchFilter("amount", Operator.EQ, o.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP)));
				logs = bankLogService.findAll(specification);
			}
			log.debug("先充值后提单查流水:tradingStart:{},tradingEnd:{},accountId:{},amount:{},{},{},{}", tradingStart,
					tradingEnd, o.getToId(), o.getAmount(), logs, logs.size());
			// 如果是工商入款卡 自动补提单 整数没有匹配 则拿流水的摘要去查询
			if (type == 2 && bizAccount.getBankType().equals("工商银行") && logs.size() <= 0) {
				logs = bankLogService.finBanks(tradingStart, tradingEnd, o.getToId(),
						BankLogStatus.Matching.getStatus(), o.getAmount(), o.getMemberRealName());
			}
			if (null != logs && logs.size() > 0) {
				log.info("进入提单自动匹配方法,金额:{},订单:{},查流水条件:start:{},end:{},流水记录:{},提单类型:{},", o.getAmount(), o.getOrderNo(),
						tradingStart, tradingEnd, logs.size(), o.getType());
				// 如果是平台会员入款
				if (IncomeRequestType.isPlatform(o.getType())) {
					// 如果是平台会员入款向平台反馈--> 确认
					boolean ret = ack(logs.get(0), o, IncomeRequestStatus.Matched.getMsg(),
							IncomeRequestStatus.Matched.getMsg(), null);
					if (ret) {
						dealAfterMatched(logs.get(0), o);
					} else {
						log.info("订单号:{},调用平台确认失败!");
					}
					res.set(ret);
					try {
						BizBankLog flow = logs.get(0);
						flow.setTaskId(o.getId());
						flow.setTaskType(SysBalTrans.TASK_TYPE_INCOME);
						flow.setOrderNo(o.getOrderNo());
						flow.setMatchWay(BankLogMatchWay.OrderFindFlow.getWay());
						systemAccountManager
								.rpush(new SysBalPush(flow.getFromAccount(), SysBalPush.CLASSIFY_BANK_LOG_, flow));
					} catch (Exception e) {
						log.error("SYSBALPUSH", e);
						res.set(false);
					}
				} else {
					boolean ret = insertTransactionLog(logs.get(0), o, null, null);
					log.info("插入交易记录表结果 :{},单号:{}", ret, o.getOrderNo());
					res.set(ret);
				}
			} else if (StringUtils.isNotBlank(o.getMemberRealName()) && o.getMemberRealName().length() <= 3
					&& o.getAmount().intValue() != o.getAmount().floatValue() && o.getType() != null
					&& IncomeRequestType.isPlatform(o.getType())) {
				// 如果小数无法自动匹配,去查找是否存在整数的流水、如果有则把这个单子取消，自动补一个整数的单子去匹配。
				Specification<BizBankLog> fication = DynamicSpecifications.build(null, BizBankLog.class,
						new SearchFilter(IncomeRequestType.isPlatform(o.getType()) ? "tradingTime" : "createTime",
								Operator.GTE, tradingStart),
						new SearchFilter(IncomeRequestType.isPlatform(o.getType()) ? "tradingTime" : "createTime",
								Operator.LTE, tradingEnd),
						new SearchFilter("fromAccount", Operator.EQ, o.getToId()),
						new SearchFilter("status", Operator.EQ, BankLogStatus.Matching.getStatus()),
						new SearchFilter("amount", Operator.EQ, o.getAmount().intValue()),
						new SearchFilter("toAccountOwner", Operator.EQ, o.getMemberRealName()));
				List<BizBankLog> bankLogs = bankLogService.findAll(fication);
				if (!CollectionUtils.isEmpty(bankLogs)) {
					BizBankLog bankLog = bankLogs.get(0);
					ThreadLocal<Boolean> flag = new ThreadLocal<>();
					boolean flag1 = incomeRequestService.cancelAndCallFlatform(IncomeRequestStatus.Canceled.getStatus(),
							o.getId(), "系统取消", o.getOrderNo(), o.getHandicap(), bankLog.getFromAccount(),
							o.getMemberCode(), null);
					flag.set(flag1);
					if (flag.get()) {
						boolean ret = incomeRequestService.generateOrderForIntegerBankAmount(bankLog, o);
						log.info("查到有整数金额的流水 并自动取消订单成功 自动补提单结果:{},订单号:{}", ret, o.getOrderNo());
						res.set(ret);
					} else {
						log.info("查到整数金额的流水 自动取消失败 ！单号：orderNo {}", o.getOrderNo());
					}
					res.set(flag1);
				} else {
					log.info("查整数金额的流水 流水不存在:单号:{}", o.getOrderNo());
				}

			} else {
				log.info("无法自动匹配发消息给前端:orderNo{},流水记录:{},", o.getOrderNo(), logs);
				noticeEntity.setIncomeAuditWsFrom(IncomeAuditWsEnum.FromIncomeReq.ordinal());
				noticeEntity.setAccount(o.getToAccount());
				noticeEntity.setAccountId(o.getToId());
				noticeEntity.setMessage("");
				// 广播消息通知前端有新的记录，browser通过websocket事件刷新内容
				String message = null;
				try {
					message = mapper.writeValueAsString(noticeEntity);
				} catch (JsonProcessingException e) {
					log.error("redis广播发消息给前端失败:{}", e);
				}
				if (StringUtils.isNotBlank(message)) {
					redisService.convertAndSend(RedisTopics.INCOME_REQUEST, message);
				}
			}
		} catch (Exception e) {
			log.error(" match orderNo :" + o.getOrderNo() + ", error:", e);
			res.set(false);
		}
		return res.get();
	}

	/**
	 * 插入系统流水
	 *
	 * @param confirmor
	 *            确认人
	 */
	@Transactional(rollbackOn = Exception.class)
	boolean insertTransactionLog(BizBankLog bankLog, BizIncomeRequest incomeRequest, Integer confirmor, String remark) {
		AtomicBoolean res = new AtomicBoolean(true);
		try {
			BizTransactionLog transactionLog = new BizTransactionLog();
			if (IncomeRequestType.isPlatform(incomeRequest.getType())) {
				// 会员帐号在系统中不存在，置0
				transactionLog.setFromAccount(0);
			}
			transactionLog.setToAccount(bankLog.getFromAccount());
			transactionLog.setOrderId(incomeRequest.getId());
			transactionLog.setType(incomeRequest.getType());
			transactionLog.setOperator(incomeRequest.getOperator());
			transactionLog.setAmount(bankLog.getAmount());
			transactionLog.setToBanklogId(bankLog.getId());
			transactionLog.setConfirmor(confirmor);// 系统自动匹配，不用记录操作人，为null
			transactionLog.setCreateTime(new Date());
			transactionLog.setRemark(remark);
			// 此场景是流水抓取的帐号被暂停或冻结，但会员提单在另一个可用帐号
			if (bankLog.getFromAccount() != incomeRequest.getToId().intValue()) {
				BigDecimal amount = incomeRequest.getAmount()
						.add(incomeRequest.getFee() == null ? BigDecimal.ZERO : incomeRequest.getFee());
				AccountBaseInfo newAccount = accountService.getFromCacheById(bankLog.getFromAccount());
				accountService.addBalance(amount.multiply(new BigDecimal(-1)), incomeRequest.getToId());
				accountService.addBalance(amount, bankLog.getFromAccount());
				incomeRequest.setToId(newAccount.getId());
				incomeRequest.setToAccount(newAccount.getAccount());

				incomeRequestService.save(incomeRequest, true);
			}
			transactionLogService.save(transactionLog);
			// 更新当日入款数
			incrementAmount(incomeRequest.getToId(), incomeRequest.getAmount());
			log.info("Matched! OrderId:{}, Banklog id:{}", incomeRequest.getOrderNo(), bankLog.getId());
			res.set(true);
		} catch (Exception e) {
			log.error("insertTransactionLog error:", e);
			res.set(false);
		}
		return res.get();
	}

	/**
	 * 保存停止接单原因
	 */
	@Override
	@Transactional
	public Map<String, Object> SaveStopOrder(String remark, String username, String type) throws Exception {
		Map<String, Object> map = new HashMap<>();
		if ("1".equals(type)) {
			incomeRequestRepository.SaveStopOrder(username, remark, new Date(), "公司入款开始接单");
		} else if ("2".equals(type)) {
			incomeRequestRepository.SaveStopOrder(username, remark, new Date(), "公司入款停止接单");
		} else if ("3".equals(type)) {
			incomeRequestRepository.SaveStopOrder(username, remark, new Date(), "出款审核开始接单");
		} else if ("4".equals(type)) {
			incomeRequestRepository.SaveStopOrder(username, remark, new Date(), "出款审核结束接单");
		} else if ("5".equals(type)) {
			incomeRequestRepository.SaveStopOrder(username, remark, new Date(), "出款任务开始接单");
			SysUser user = userService.findByUid(username);
			if (Objects.nonNull(user)) {
				redisService.convertAndSend(RedisTopics.ALLOC_OUT_TASK_SUSPEND, user.getId() + ":0:0");
				log.info("ScheduleOutTask (id: {} uid: {} ) >>  START", user.getId(), user.getUid());
			}
		} else if ("6".equals(type) || "7".equals(type)) {
			incomeRequestRepository.SaveStopOrder(username, remark, new Date(),
					"7".equals(type) ? "出款任务暂停接单" : "出款任务结束接单");
			SysUser user = userService.findByUid(username);
			if (Objects.nonNull(user)) {
				redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOCATING_NEW_OUTWARD_TARGET).remove(
						String.format(Constants.QUEUE_FORMAT_USER_OR_ROBOT, Constants.TARGET_TYPE_USER, user.getId()));
				redisService.convertAndSend(RedisTopics.ALLOC_OUT_TASK_SUSPEND, user.getId() + ":1:0");
				log.info("ScheduleOutTask (id: {} uid: {} ) >> END", user.getId(), user.getUid());
			}
		}
		map.put("message", "保存成功！");
		return map;
	}

	/**
	 * 更新当日入款数
	 */
	public void incrementAmount(Integer toId, BigDecimal amount) {
		redisService.increment(RedisKeys.AMOUNT_SUM_BY_DAILY_INCOME, String.valueOf(toId), amount.floatValue());
		Long expire = redisService.getFloatRedisTemplate().boundHashOps(RedisKeys.AMOUNT_SUM_BY_DAILY_INCOME)
				.getExpire();
		if (null != expire && expire < 0) {// 当日清零
			long currentTimeMillis = System.currentTimeMillis();
			long expireTime = CommonUtils.getExpireTime4AmountDaily() - currentTimeMillis;
			redisService.getFloatRedisTemplate().expire(RedisKeys.AMOUNT_SUM_BY_DAILY_INCOME, expireTime,
					TimeUnit.MILLISECONDS);
			log.debug("Reset IncomeDailyTotal expire : {}", expireTime);
		}
	}

	/**
	 * 查询停止接单原因
	 */
	@Override
	public Map<String, Object> SearStopOrder(String username, String type, String fristTime, String lastTime,
			PageRequest pageRequest) throws Exception {
		Page<Object> dataToPage = incomeRequestRepository.SearStopOrder(username, type, fristTime, lastTime,
				pageRequest);
		Map<String, Object> map = new HashMap<>();
		map.put("Page", dataToPage);
		return map;
	}

	@Override
	public List<String> findAllTimeout(Integer handicap, Date start, Date end) {
		return incomeRequestRepository.findAllTimeout(handicap, start, end);
	}

	@Override
	public void cancelAllTimeout(Integer handicap, Date timeout) {
		incomeRequestRepository.cancelAllTimeout(handicap, timeout);
	}

	@Override
	public Map<String, Object> findMatchedBySQL(String memberUsername, String fristTime, String lastTime,
			BigDecimal startamount, BigDecimal endamount, String orderNo, String toAccount, String operatorUid,
			String manual, String robot, List<Integer> handicapList, Pageable pageable) {
		Page<Object> dataToPage = incomeRequestRepository.findMatchedBySQL(memberUsername, fristTime, lastTime,
				startamount, endamount, orderNo, toAccount, operatorUid, manual, robot, handicapList, pageable);
		Object[] total = incomeRequestRepository.findMatchedBySQL_AmountTotal(memberUsername, fristTime, lastTime,
				startamount, endamount, orderNo, toAccount, operatorUid, manual, robot, handicapList);
		Map<String, Object> map = new HashMap<>();
		map.put("page", new Paging(dataToPage));
		map.put("dataToPage", dataToPage);
		map.put("header", total == null ? 0 : total);
		return map;
	}

	@Override
	@Transactional
	public void updateAccount(Integer accountId, String account) {
		incomeRequestRepository.updateAccountFrom(accountId, account);
		incomeRequestRepository.flush();
		incomeRequestRepository.updateAccountTo(accountId, account);
		incomeRequestRepository.flush();
	}

	@Override
	public BizIncomeRequest findRebateLimit(BigDecimal amount, int fromAccount, String startTime, String endTime) {
		return incomeRequestRepository.findRebateLimit(amount, fromAccount, startTime, endTime);
	}

	@Override
	@Transactional
	public void updateStatusById(Long id, Integer status) {
		incomeRequestRepository.updateStatusById(id, status);
	}

	@Override
	public BizIncomeRequest findIncome(int accountId, String toAccountOwner, BigDecimal amount, String startTime,
			String endTime, int status) {
		return incomeRequestRepository.findIncome(accountId, toAccountOwner, amount, startTime, endTime, status);
	}

	@Override
	@Transactional(rollbackOn = Exception.class)
	public boolean generateOrderForIntegerBankAmount(BizBankLog bankLog, BizIncomeRequest incom) {
		AtomicBoolean res = new AtomicBoolean(false);
		try {
			AccountBaseInfo accountBaseInfo = accountService.getFromCacheById(bankLog.getFromAccount());
			BizHandicap bizHandicap = handicapService.findFromCacheById(accountBaseInfo.getHandicapId());
			String remark = new StringBuilder("补提单:").append(CommonUtils.getNowDate()).append(" ").append("sys")
					.append("整数流水自动补提单！").append(bankLog.getToAccountOwner()).toString();
			Integer pfTypeSub = accountService.getPfTypeSubVal(accountBaseInfo.getId());
			String ret = incomeRequestService.generateIncomeRequestOrder(pfTypeSub, bizHandicap,
					bankLog.getToAccountOwner(), incom.getMemberUserName(), bankLog.getAmount().toString(),
					accountBaseInfo, 1, remark, "sys");
			log.info("流水整数金额自动补单 参数FromAccount :{}, pfTypeSub:{}, subType:{}, result:{} ", bankLog.getFromAccount(),
					pfTypeSub, accountBaseInfo.getSubType(), ret);
			if (!"success".equals(ret)) {
				res.set(false);
			}
		} catch (Exception e) {
			log.error("查到有整数金额的流水 补提单失败, error:", e);
			res.set(false);
		}
		return res.get();
	}

	@Override
	public int findIncomeCounts(int accountId, String startTime, String endTime, int status, BigDecimal amount) {
		return incomeRequestRepository.findIncomeCounts(accountId, startTime, endTime, status, amount);
	}

	/**
	 * 取消后更新
	 */
	@Transactional
	void updateIncomeReqAfterCancel(Long incomeRequestId, String remark, SysUser loginUser) {
		BizIncomeRequest incomeRequest = incomeRequestService.get(incomeRequestId);
		incomeRequest.setStatus(IncomeRequestStatus.Canceled.getStatus());
		incomeRequest.setUpdateTime(new Date());
		incomeRequest.setOperator(loginUser == null ? null : loginUser.getId());
		incomeRequest.setRemark(CommonUtils.genRemark(incomeRequest.getRemark(), remark + "(取消)", new Date(),
				loginUser == null ? "sys" : loginUser.getUid()));
		update(incomeRequest);
		dealAfterCanceled(incomeRequest);
	}

	/**
	 * redis发送消息到客户端
	 */
	private void redisSendMessage(String data, Integer accountId) {
		ObjectMapper mapper = new ObjectMapper();
		IncomeAuditWs noticeEntity = new IncomeAuditWs();
		noticeEntity.setMessage(data);
		noticeEntity.setAccountId(accountId);
		noticeEntity.setIncomeAuditWsFrom(IncomeAuditWsEnum.FromMatched.ordinal());
		// 匹配成功与否都广播消息通知前端有新的记录，browser通过websocket事件刷新内容
		try {
			redisService.convertAndSend(RedisTopics.INCOME_REQUEST, mapper.writeValueAsString(noticeEntity));
		} catch (JsonProcessingException e) {
			log.error("" + e);
		}
	}

	@Override
	public Object getMatchedInfo(Long requestId) {
		return incomeRequestRepository.getMatchedInfo(requestId);
	}

	/**
	 * 客服发送消息
	 *
	 * @param accountId
	 *            审核人审核的账号
	 * @param message
	 *            消息
	 */
	@Override
	public String customerSendMsg(Long requestId, Long accountId, String message, String customerName) {
		BizIncomeRequest bizIncomeRequest = findById(requestId);
		BizAccount bizAccount = accountService.getById(accountId.intValue());
		String orderNo = bizIncomeRequest.getOrderNo();
		String flag = CUSTOMER_SENDMESSAGE_FAILED;
		String messages = "来自客服：" + (customerName == null ? "" : customerName) + "<br>关于系统提单的消息<br>消息内容："
				+ (message == null ? "" : message) + "<br>账号编号："
				+ (bizAccount.getAlias() == null ? "" : bizAccount.getAlias()) + "<br>账号："
				+ (bizAccount.getAccount() == null ? ""
						: (bizAccount.getAccount().substring(0, 3) + "***")
								+ bizAccount.getAccount().substring(bizAccount.getAccount().length() - 3))
				+ "<br>订单号：" + (orderNo == null ? "" : orderNo) + "";
		String keyPattern = RedisKeys.genPattern4IncomeAuditAccountAllocateByAccountId(accountId);
		Set<String> keys = redisService.getStringRedisTemplate().keys(keyPattern);
		if (keys == null || keys.size() == 0) {
			return flag;
		}
		SysUser incomeAuditor = userService
				.findFromCacheById(Integer.valueOf(new ArrayList<>(keys).get(0).split(":")[1]));
		IncomeAuditWs noticeEntity = new IncomeAuditWs();
		noticeEntity.setIncomeAuditWsFrom(IncomeAuditWsEnum.CustomerMessage.ordinal());
		noticeEntity.setAccountId(accountId.intValue());
		noticeEntity.setMessage(messages);
		noticeEntity.setOwner(incomeAuditor.getUid());
		try {
			redisService.convertAndSend(RedisTopics.INCOME_REQUEST, mapper.writeValueAsString(noticeEntity));
			flag = CUSTOMER_SENDMESSAGE_SUCCEED;
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return flag;
	}

	/**
	 * 补提单 调用平台接口 生成公司入款提单
	 *
	 * @param bizHandicap
	 *            补提单盘口信息
	 * @param name
	 *            存款人
	 * @param memberAccount
	 *            会员账号
	 * @param amount
	 *            金额
	 * @param accountBaseInfo
	 *            收款账号信息
	 * @param type
	 *            存款类型
	 * @param remark
	 *            备注
	 */
	@Override
	public String generateIncomeRequestOrder(Integer pfTypeSub, BizHandicap bizHandicap, String name,
			String memberAccount, String amount, AccountBaseInfo accountBaseInfo, Integer type, String remark,
			String userName) {
		String ret;
		try {
			String createTime = CommonUtils.getNowDate();

			if (StringUtils.isNotBlank(remark)) {
				if (remark.length() > 190) {
					remark = remark.substring(190) + "(补提单)";
				} else {
					remark = remark + "(补提单)";
				}
			} else {
				remark = "补提单";
			}
			IncomeAuditWs noticeEntity = new IncomeAuditWs();
			ObjectMapper mapper = new ObjectMapper();
			noticeEntity.setAccountId(accountBaseInfo.getId());
			noticeEntity.setOwner(userName);
			noticeEntity.setIncomeAuditWsFrom(IncomeAuditWsEnum.CustomerAddOrder.ordinal());
			int bankType = BankEnums.findDesc(accountBaseInfo.getBankType()).getCode();
			if (null == accountBaseInfo.getSubType()) {
				return "账号子类型不存在!";
			}
			int subType = accountBaseInfo.getSubType();
			HttpClientNew.getInstance().getPlatformServiceApi()
					.deposit(requestBodyParser.buildDepositRequestBody(pfTypeSub, bizHandicap.getCode(),
							StringUtils.trim(memberAccount), new BigDecimal(amount), StringUtils.trim(name),
							accountBaseInfo.getAccount(), remark, type, createTime, accountBaseInfo.getProvince(),
							accountBaseInfo.getCity(), bankType, subType))
					.subscribe(response -> {
						log.info("new补提单, amount: {}, response: {}", amount, response);
						noticeEntity.setAccountId(accountBaseInfo.getId());
						noticeEntity.setOwner(userName);
						noticeEntity.setIncomeAuditWsFrom(IncomeAuditWsEnum.CustomerAddOrder.ordinal());
						if (response.getStatus() == 1) {
							noticeEntity.setStatus(1);
							noticeEntity.setMessage("补提单成功:金额：" + amount);
						} else {
							noticeEntity.setStatus(2);
							noticeEntity.setMessage(response.getMessage());
							// sendMessage(sysUser, "补提单金额:" + amount +
							// ",平台返回失败，请联系技术人员处理 -->" +
							// response.getMessage());
						}
					}, e -> {
						noticeEntity.setStatus(2);
						noticeEntity.setMessage(e.getLocalizedMessage());
						log.error("new调用平台补提单失败：{} ", e);
						// sendMessage(sysUser, "补提单操作失败，请联系技术人员处理");
					});
			if (noticeEntity.getStatus() == 1) {
				ret = "success";
			} else {
				ret = noticeEntity.getMessage();
				noticeEntity.setMessage("补提单失败:" + noticeEntity.getMessage() + ",金额：" + amount);
			}
			try {
				redisService.convertAndSend(RedisTopics.INCOME_REQUEST, mapper.writeValueAsString(noticeEntity));
			} catch (Exception e1) {
				log.error("new调用平台补提单发消息失败：{} ", e1);
			}
			return ret;
		} catch (Exception e) {
			log.error("调平台补提单失败：{}", e);
			ret = e.getLocalizedMessage();
			return ret;
		}
	}

	/**
	 * 公司入款正在匹配的 总金额
	 */
	@Override
	public String findMatchingInComeRequestSum(String payMan, int status, Integer[] accountId, Integer level,
			String orderNo, String member, String fromMoney, String toMoney, String startTime, String endTime) {
		BigDecimal fromM = null;
		if (fromMoney != null && !"".equals(fromMoney)) {
			fromM = new BigDecimal(fromMoney);
		}
		BigDecimal toM = null;
		if (toMoney != null && !"".equals(toMoney)) {
			toM = new BigDecimal(toMoney);
		}
		Integer type[] = new Integer[1];
		type[0] = IncomeRequestType.PlatFromBank.getType();// 公司银行卡入款
		String sum = getSumAmountForCompanyIn(status, type, accountId, null, level, orderNo, member, payMan, fromM, toM,
				startTime, endTime, null);
		log.debug("金额总和：" + sum);
		return sum;
	}

	/**
	 * 组装查询条件 公司入款 正在匹配 已匹配 已取消
	 */
	private Specification<BizIncomeRequest> wrapSpecificForCompanyIn(Integer status, Integer[] type, Integer[] handicap,
			Integer level, Integer[] toAccountIds, String member, String memberRealName, BigDecimal fromMoney,
			BigDecimal toMoney, String startTime, String endTime, String orderNo, Integer operator) {
		return (root, criteriaQuery, criteriaBuilder) -> {
			Predicate predicate = null;
			final Path<Integer> toAccountR = root.get("toId");
			final Path<String> memberRealNameR = root.get("memberRealName");
			final Path<String> orderNoR = root.get("orderNo");
			final Path<Integer> levelR = root.get("level");
			final Path<Integer> handicapR = root.get("handicap");
			final Path<Date> updateTimeR = root.get("updateTime");
			final Path<Date> createTimeR = root.get("createTime");
			final Path<Integer> statusR = root.get("status");
			final Path<Integer> typeR = root.get("type");
			final Path<String> memberUserNameR = root.get("memberUserName");
			final Path<BigDecimal> amountR = root.get("amount");
			final Path<Integer> operatorR = root.get("operator");
			if (toAccountIds != null && toAccountIds.length > 0) {
				if (toAccountIds.length > 1) {
					predicate = addAndPredicate(criteriaBuilder, predicate, toAccountR.in(toAccountIds));
				} else {
					predicate = addAndPredicate(criteriaBuilder, predicate,
							criteriaBuilder.equal(toAccountR, toAccountIds[0]));
				}
			}
			if (StringUtils.isNotBlank(startTime)) {
				Date startTimeD = CommonUtils.string2Date(startTime);
				predicate = addAndPredicate(criteriaBuilder, predicate,
						criteriaBuilder.greaterThanOrEqualTo(createTimeR, startTimeD));
			}
			if (StringUtils.isNotBlank(endTime)) {
				Date endTimeD = CommonUtils.string2Date(endTime);
				predicate = addAndPredicate(criteriaBuilder, predicate,
						criteriaBuilder.lessThanOrEqualTo(createTimeR, endTimeD));
			}
			if (status != null) {
				predicate = addAndPredicate(criteriaBuilder, predicate, criteriaBuilder.equal(statusR, status));
			}
			if (type != null && type.length > 0) {
				if (type.length == 1)
					predicate = addAndPredicate(criteriaBuilder, predicate, criteriaBuilder.equal(typeR, type[0]));
				else
					predicate = addAndPredicate(criteriaBuilder, predicate, typeR.in(type));
			}
			if (fromMoney != null) {
				predicate = addAndPredicate(criteriaBuilder, predicate,
						criteriaBuilder.greaterThanOrEqualTo(amountR, fromMoney));
			}
			if (toMoney != null) {
				predicate = addAndPredicate(criteriaBuilder, predicate,
						criteriaBuilder.lessThanOrEqualTo(amountR, toMoney));
			}

			if (StringUtils.isNotBlank(member)) {
				predicate = addAndPredicate(criteriaBuilder, predicate,
						criteriaBuilder.like(memberUserNameR, "%" + member + "%"));
			}
			if (StringUtils.isNotBlank(memberRealName)) {
				predicate = addAndPredicate(criteriaBuilder, predicate,
						criteriaBuilder.like(memberRealNameR, "%" + memberRealName + "%"));
			}
			if (level != null) {
				predicate = addAndPredicate(criteriaBuilder, predicate, criteriaBuilder.equal(levelR, level));
			}
			if (handicap != null && handicap.length > 0) {
				if (handicap.length == 1) {
					predicate = addAndPredicate(criteriaBuilder, predicate, criteriaBuilder.equal(handicapR, handicap));
				} else {
					predicate = addAndPredicate(criteriaBuilder, predicate, handicapR.in(handicap));
				}
			}
			if (StringUtils.isNotBlank(orderNo)) {
				predicate = addAndPredicate(criteriaBuilder, predicate, criteriaBuilder.equal(orderNoR, orderNo));
			}
			if (operator != null) {
				predicate = addAndPredicate(criteriaBuilder, predicate, criteriaBuilder.equal(operatorR, operator));
			}
			return predicate;
		};
	}

	/**
	 * 获取公司入款总记录数--正匹配 已匹配 已取消
	 */
	public String getCountForCompanyIn(Integer status, Integer[] type, Integer[] toAccountIds, Integer[] handicap,
			Integer level, String orderNo, String member, String payMan, BigDecimal fromMoney, BigDecimal toMoney,
			String startTime, String endTime, Integer operator) {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
		Root<BizIncomeRequest> root = query.from(BizIncomeRequest.class);
		Predicate predicate = criteriaBuilder.conjunction();
		final Path<Integer> toAccountR = root.get("toId");
		final Path<String> memberRealNameR = root.get("memberRealName");
		final Path<String> orderNoR = root.get("orderNo");
		final Path<Integer> levelR = root.get("level");
		final Path<Integer> handicapR = root.get("handicap");
		final Path<Date> createTimeR = root.get("createTime");
		final Path<Integer> statusR = root.get("status");
		final Path<Integer> typeR = root.get("type");
		final Path<String> memberUserNameR = root.get("memberUserName");
		final Path<BigDecimal> amountR = root.get("amount");
		final Path<Long> idR = root.get("id");
		final Path<Integer> operatorR = root.get("operator");
		List<Expression<Boolean>> expressions = predicate.getExpressions();
		Expression<Long> count = criteriaBuilder.count(idR);
		if (toAccountIds != null && toAccountIds.length > 0) {
			if (toAccountIds.length > 1)
				expressions.add(toAccountR.in(toAccountIds));
			if (toAccountIds.length == 1)
				expressions.add(criteriaBuilder.equal(toAccountR, toAccountIds[0]));
		}
		if (StringUtils.isNotBlank(startTime)) {
			Date startTimeD = CommonUtils.string2Date(startTime);
			expressions.add(criteriaBuilder.greaterThanOrEqualTo(createTimeR, startTimeD));
		}
		if (StringUtils.isNotBlank(endTime)) {
			Date endTimeD = CommonUtils.string2Date(endTime);
			expressions.add(criteriaBuilder.lessThanOrEqualTo(createTimeR, endTimeD));
		}
		if (status != null) {
			expressions.add(criteriaBuilder.equal(statusR, status));
		}
		if (type != null && type.length > 0) {
			if (type.length == 1)
				expressions.add(criteriaBuilder.equal(typeR, type[0]));
			else
				expressions.add(typeR.in(type));
		}
		if (fromMoney != null) {
			expressions.add(criteriaBuilder.greaterThanOrEqualTo(amountR, fromMoney));
		}
		if (toMoney != null) {
			expressions.add(criteriaBuilder.lessThanOrEqualTo(amountR, toMoney));
		}
		if (StringUtils.isNotBlank(member)) {
			expressions.add(criteriaBuilder.like(memberUserNameR, "%" + member + "%"));
		}
		if (StringUtils.isNotBlank(payMan)) {
			expressions.add(criteriaBuilder.like(memberRealNameR, "%" + payMan + "%"));
		}
		if (level != null) {
			expressions.add(criteriaBuilder.equal(levelR, level));
		}
		if (handicap != null && handicap.length > 0) {
			if (handicap.length == 1) {
				expressions.add(criteriaBuilder.equal(handicapR, handicap[0]));
			} else {
				expressions.add(handicapR.in(handicap));
			}

		}
		if (StringUtils.isNotBlank(orderNo)) {
			expressions.add(criteriaBuilder.equal(orderNoR, orderNo));
		}
		if (operator != null) {
			expressions.add(criteriaBuilder.equal(operatorR, operator));
		}
		query.where(predicate);
		query.select(count);
		Long result = entityManager.createQuery(query).getSingleResult();
		result = (null == result ? 0L : result);
		return result.toString();
	}

	/**
	 * @param list
	 *            入款订单未匹配的所有记录id集合
	 * @param updateType
	 *            {@link IncomeRequestStatus} 在这里只有 支转银匹配和取消操作 系统自动取消的，更新的状态是
	 *            IncomeRequestStatus.Canceled.getStatus()
	 * @param orderNo
	 *            订单号
	 */
	@Override
	@Transactional(rollbackOn = Exception.class)
	public void update4AliBankInOrders(List<Long> list, int updateType, String orderNo) {
		try {
			if (CollectionUtils.isEmpty(list)) {
				log.info("支转银入款记录id为空,入款单号:{}", orderNo);
				return;
			}
			if (updateType != IncomeRequestStatus.MATCHED4SUBINBANKALI.getStatus()
					&& updateType != IncomeRequestStatus.CANCELED4SUBINBANKALI.getStatus()) {
				log.info("支转银更新类型不属于匹配或取消操作:{},入款单号:{}", updateType, orderNo);
				return;
			}
			List<BizIncomeRequest> list2Match = incomeRequestRepository.getIncomReqListByIdAndOrderNo4Update(list,
					orderNo);
			if (CollectionUtils.isEmpty(list2Match)) {
				log.info("支转银根据入款id:{} 获待更新记录为空,入款单号:{}", list.toString(), orderNo);
				return;
			}
			list2Match.stream().forEach(p -> {
				p.setStatus(updateType);
				String msg = (updateType == IncomeRequestStatus.MATCHED4SUBINBANKALI.getStatus()
						? IncomeRequestStatus.MATCHED4SUBINBANKALI.getMsg()
						: IncomeRequestStatus.CANCELED4SUBINBANKALI.getMsg());
				p.setRemark("支转银多个订单同步更新:" + msg);
				p.setUpdateTime(new Date());
			});
			log.debug("支转银多个订单同步更新之前:{}", list2Match);
			list2Match = incomeRequestRepository.save(list2Match);
			log.debug("支转银多个订单同步更新之后:{}", list2Match);
		} catch (Exception e) {
			log.error("支转银多个订单同步更新失败:", e);
		}
	}

	private BizIncomeRequest bindExtForIncomeRequest(BizIncomeRequest incomeRequest) throws Exception {
		if (incomeRequest == null) {
			return null;
		}
		Integer fromId = incomeRequest.getFromId();
		if (fromId != null) {
			AccountBaseInfo fromAccount = accountService.getFromCacheById(fromId);
			// 这个字段和数据库有映射关系，事务提交时会把设置的值更新到数据库进入覆盖。
			// incomeRequest.setFromAccount(fromAccount != null ?
			// fromAccount.getAccount() : StringUtils.EMPTY);
			incomeRequest.setFromAlias(
					fromAccount != null ? StringUtils.trimToEmpty(fromAccount.getAlias()) : StringUtils.EMPTY);
			incomeRequest.setFromBankType(
					fromAccount != null ? StringUtils.trimToEmpty(fromAccount.getBankType()) : StringUtils.EMPTY);
			incomeRequest.setFromOwner(
					fromAccount != null ? StringUtils.trimToEmpty(fromAccount.getOwner()) : StringUtils.EMPTY);
			incomeRequest.setFromBankType(
					fromAccount != null ? StringUtils.trimToEmpty(fromAccount.getBankName()) : StringUtils.EMPTY);
		}
		Integer toId = incomeRequest.getToId();
		if (toId != null) {
			AccountBaseInfo toAccount = accountService.getFromCacheById(toId);
			/*
			 * 这两个字段和数据库有映射关系，事务提交时会把设置的值更新到数据库进入覆盖。(账号监控第三方下发匹配时会把触发器更新的状态覆盖)
			 * incomeRequest.setToAccount(toAccount != null ? toAccount.getAccount() :
			 * StringUtils.EMPTY); incomeRequest.setToAccountBank( toAccount != null ?
			 * StringUtils.trimToEmpty(toAccount.getBankName()) : StringUtils.EMPTY);
			 */
			incomeRequest
					.setToAlias(toAccount != null ? StringUtils.trimToEmpty(toAccount.getAlias()) : StringUtils.EMPTY);
			incomeRequest.setToBankType(
					toAccount != null ? StringUtils.trimToEmpty(toAccount.getBankType()) : StringUtils.EMPTY);
			incomeRequest
					.setToOwner(toAccount != null ? StringUtils.trimToEmpty(toAccount.getOwner()) : StringUtils.EMPTY);
		}
		if (incomeRequest.getHandicap() != null) {
			BizHandicap handicap = handicapService.findFromCacheById(incomeRequest.getHandicap());
			incomeRequest.setHandicapName(handicap != null ? handicap.getName() : StringUtils.EMPTY);
		}
		if (incomeRequest.getLevel() != null) {
			BizLevel level = levelService.findFromCache(incomeRequest.getLevel());
			incomeRequest.setLevelName(level != null ? level.getName() : StringUtils.EMPTY);
		}
		if (incomeRequest.getOperator() != null) {
			SysUser user = userService.findFromCacheById(incomeRequest.getOperator());
			incomeRequest.setOperatorUid(user != null ? user.getUid() : StringUtils.EMPTY);
		}
		IncomeRequestStatus reqStatus = IncomeRequestStatus.findByStatus(incomeRequest.getStatus());
		incomeRequest.setStatusName(reqStatus != null ? reqStatus.getMsg() : StringUtils.EMPTY);
		IncomeRequestType reqType = IncomeRequestType.findByType(incomeRequest.getType());
		incomeRequest.setTypeName(reqType != null ? reqType.getMsg() : StringUtils.EMPTY);
		return incomeRequest;
	}

	/**
	 * 根据订单号查询未匹配的记录
	 *
	 * @param orderNo
	 * @return
	 */
	@Override
	public List<BizIncomeRequest> findUnmatchedListByOrderNo(String orderNo) {
		return incomeRequestRepository.findByOrderNoAndStatus(orderNo, IncomeRequestStatus.Matching.getStatus());
	}

	/**
	 * 根据入款提单记录的toId 检测是否这些toId是否是支转银收款账号
	 *
	 * @param list
	 * @return
	 */
	@Override
	public boolean checkAliInAccountByToIds(List<Integer> list) {
		if (CollectionUtils.isEmpty(list))
			return false;
		List<Integer> checked = new ArrayList<>();
		for (int i = 0, size = list.size(); i < size; i++) {
			boolean isAlinBankType = false;
			AccountBaseInfo accountBaseInfo = accountService.getFromCacheById(list.get(i));
			if (Objects.nonNull(accountBaseInfo) && accountBaseInfo.getType() == AccountType.InBank.getTypeId()
					&& Objects.nonNull(accountBaseInfo.getSubType())
					&& accountBaseInfo.getSubType() == InBankSubType.IN_BANK_ALIIN.getSubType()) {
				isAlinBankType = true;
			}
			if (!isAlinBankType) {
				BizAccount account = accountService.getById(list.get(i));
				if (Objects.nonNull(account) && AccountType.InBank.getTypeId().equals(account.getType())
						&& Objects.nonNull(account.getSubType())
						&& InBankSubType.IN_BANK_ALIIN.getSubType().equals(account.getSubType())) {
					isAlinBankType = true;
				}
			}
			if (isAlinBankType) {
				checked.add(list.get(i));
			}
		}
		return checked.size() == list.size();
	}

	/**
	 * 如果取消成功,检查是否是支转银收款并且一个订单号对应多个收款账号,则更新其他收款账号记录 系统自动取消的，更新的状态是
	 * IncomeRequestStatus.Canceled.getStatus()
	 *
	 * @param incomeRequest
	 *            手工取消的入款记录
	 */
	@Transactional
	void dealAfterCanceled(BizIncomeRequest incomeRequest) {
		try {
			incomeRequest = get(incomeRequest.getId());
			boolean canceled = incomeRequest.getStatus().equals(IncomeRequestStatus.Canceled.getStatus());
			boolean existUnmatchOrders = false;
			List<BizIncomeRequest> unmatchedListByOrderNo = findUnmatchedListByOrderNo(incomeRequest.getOrderNo());
			if (!CollectionUtils.isEmpty(unmatchedListByOrderNo)) {
				existUnmatchOrders = true;
			}
			if (canceled && existUnmatchOrders) {
				List<Integer> unMatchToIds = new ArrayList<>();
				List<Long> unMatchIds = new ArrayList<>();
				unmatchedListByOrderNo.stream().forEach(p -> {
					unMatchToIds.add(p.getToId());
					unMatchIds.add(p.getId());
				});
				if (checkAliInAccountByToIds(unMatchToIds)) {
					update4AliBankInOrders(unMatchIds, IncomeRequestStatus.CANCELED4SUBINBANKALI.getStatus(),
							incomeRequest.getOrderNo());
				}
			}

		} catch (Exception e) {
			log.error("取消之后检查是否是支转银入款更新状态失败,error:", e);
		}
	}

	// 如果确认成功,检查是否是支转银收款并且一个订单号对应多个收款账号,则更新其他收款账号记录
	@Transactional
	void dealAfterMatched(BizBankLog bankLog, BizIncomeRequest incomeRequest) {
		try {
			incomeRequest = get(incomeRequest.getId());
			boolean matched = incomeRequest.getStatus().equals(IncomeRequestStatus.Matched.getStatus());
			boolean existUnmatchOrders = false;
			List<BizIncomeRequest> unmatchedListByOrderNo = findUnmatchedListByOrderNo(incomeRequest.getOrderNo());
			if (!CollectionUtils.isEmpty(unmatchedListByOrderNo)) {
				existUnmatchOrders = true;
			}
			if (matched && existUnmatchOrders) {
				List<Integer> fromAccount = new ArrayList<>();
				fromAccount.add(bankLog.getFromAccount());
				List<Integer> unMatchToIds = new ArrayList<>();
				List<Long> unMatchIds = new ArrayList<>();
				unmatchedListByOrderNo.stream().forEach(p -> {
					unMatchToIds.add(p.getToId());
					unMatchIds.add(p.getId());
				});
				if (checkAliInAccountByToIds(fromAccount) && checkAliInAccountByToIds(unMatchToIds)) {
					update4AliBankInOrders(unMatchIds, IncomeRequestStatus.MATCHED4SUBINBANKALI.getStatus(),
							incomeRequest.getOrderNo());
				}
			}

		} catch (Exception e) {
			log.error("匹配之后检查是否是支转银入款更新状态失败,error:", e);
		}
	}

	@Override
	public Integer getThirdIdLastUsedByUserId(@NotNull Integer userId) {
		String key = RedisKeys.DRAW_TASK_USER_USED_LASTIME;
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		HashOperations operations = template.opsForHash();
		if (operations.hasKey(key, userId.toString())) {
			Object obj = operations.get(key, userId.toString());
			if (null != obj) {
				return Integer.valueOf(obj.toString());
			}
		}
		return null;
	}

	@Override
	public void saveThirdIdUsedLatest(@NotNull Integer thirdId, @NotNull Integer userId) {
		String key = RedisKeys.DRAW_TASK_USER_USED_LASTIME;
		StringRedisTemplate template = redisService.getStringRedisTemplate();
		HashOperations operations = template.opsForHash();
		operations.put(key, userId.toString(), thirdId.toString());
	}

	/**
	 * 根据id 更新 更新时间和耗时
	 *
	 * @param id
	 * @param updateTime
	 * @param timeConsuming
	 */
	@Override
	@Transactional(rollbackOn = Exception.class)
	public void updateTimeconsumingAndUpdateTime(Long id, Date updateTime, Long timeConsuming) {
		incomeRequestRepository.updateTimeConsumingAndUpdateTime(id, updateTime, timeConsuming);
	}

	@Override
	public void match(BizBankLog bankLog, BizIncomeRequest incomeRequest, String remark, String remarkWrap,
			SysUser confirmor) {
		// 若是平台入款，先向平台确认，成功则插入流水，匹配成功，否则失败
		if (IncomeRequestType.isPlatform(incomeRequest.getType())) {
			// 向平台反馈--> 确认
			ack(bankLog, incomeRequest, remark, remarkWrap, confirmor.getId());
			// 如果确认成功,检查是否是支转银收款并且一个订单号对应多个收款账号,则更新其他收款账号记录
			dealAfterMatched(bankLog, incomeRequest);
			try {
				bankLog.setTaskId(incomeRequest.getId());
				bankLog.setTaskType(SysBalTrans.TASK_TYPE_INCOME);
				bankLog.setOrderNo(incomeRequest.getOrderNo());
				bankLog.setMatchWay(BankLogMatchWay.OrderFindFlow.getWay());
				systemAccountManager
						.rpush(new SysBalPush(bankLog.getFromAccount(), SysBalPush.CLASSIFY_BANK_LOG_, bankLog));
				dealOccuTempLimit(bankLog);
			} catch (Exception e) {
				log.error("SYSBALPUSH MANULAL", e);
			}
		} else {
			BizTransactionLog o = transactionLogService.findByOrderIdAndType(incomeRequest.getId(),
					incomeRequest.getType());
			if (o == null) {
				o = new BizTransactionLog();
				// fromBankLogId 设置
				AccountBaseInfo toBase = accountService.getFromCacheById(bankLog.getFromAccount());
				String toOwner = StringUtils.trimToEmpty(toBase.getOwner());
				String toAccount = StringUtils.trimToEmpty(toBase.getAccount());
				List<BizBankLog> fromBankLogList = bankLogRepository.findByFromAccountAndAmount(
						incomeRequest.getFromId(), bankLog.getAmount().abs().multiply(new BigDecimal(-1)));
				fromBankLogList = fromBankLogList.stream().filter(
						p -> p.getStatus() == null || p.getStatus() == BankLogStatus.Matching.getStatus().intValue())
						.collect(Collectors.toList());
				fromBankLogList.sort((o1, o2) -> -o1.getTradingTime().compareTo(o2.getTradingTime()));
				for (BizBankLog fromBankLog : fromBankLogList) {
					if (StringUtils.equals(StringUtils.trimToEmpty(fromBankLog.getToAccount()), toAccount)
							|| StringUtils.equals(StringUtils.trimToEmpty(fromBankLog.getToAccountOwner()), toOwner)) {
						o.setFromBanklogId(fromBankLog.getId());
						break;
					}
				}
			}
			try {
				log.info("IncomeReq >> match package before  data: {}", mapper.writeValueAsString(o));
				o.setAmount(bankLog.getAmount());
				o.setToBanklogId(bankLog.getId());
				o.setOrderId(incomeRequest.getId());
				o.setConfirmor(confirmor.getId());
				o.setOperator(incomeRequest.getOperator());
				o.setCreateTime(new Date());
				o.setType(incomeRequest.getType());
				o.setToAccount(incomeRequest.getToId());
				o.setDifference(incomeRequest.getAmount().subtract(bankLog.getAmount()));
				o.setFromAccount(incomeRequest.getFromId());
				o.setRemark(CommonUtils.genRemark(o.getRemark(), remark, o.getCreateTime(), confirmor.getUid()));
				log.info("IncomeReq >> match package after  data: {}", mapper.writeValueAsString(o));
				transactionLogService.save(o);
				// 更新当日入款数
				incrementAmount(incomeRequest.getToId(), incomeRequest.getAmount());
				// 手工匹配 添加耗时
				// incomeRequest = incomeRequestService.get(incomeRequest.getId());
				// incomeRequest.setUpdateTime(new Date());
				// 保存 秒
				// incomeRequest.setTimeconsume(
				// (incomeRequest.getUpdateTime().getTime() -
				// incomeRequest.getCreateTime().getTime()) / 1000);
				// saveAndFlush(incomeRequest);
				Date updateTime = new Date();
				Long timeConSuming = (updateTime.getTime() - incomeRequest.getCreateTime().getTime()) / 1000;
				incomeRequestService.updateTimeconsumingAndUpdateTime(incomeRequest.getId(), updateTime, timeConSuming);
			} catch (Exception e) {
				log.error("匹配异常:", e);
				log.info("IncomeReq >> match error : banklogId: {} incomeId: {}", bankLog.getId(),
						incomeRequest.getId());
			}

			try {
				AccountBaseInfo base = accountService.getFromCacheById(bankLog.getFromAccount());
				if (Objects.nonNull(base) && bankLog.getAmount().floatValue() > 0) {
					Integer type = base.getType();
					if (Objects.equals(type, AccountType.BindAli.getTypeId())
							|| Objects.equals(type, AccountType.BindWechat.getTypeId())
							|| Objects.equals(type, AccountType.BindCommon.getTypeId())
							|| Objects.equals(type, AccountType.ThirdCommon.getTypeId())
							|| Objects.equals(type, AccountType.OutBank.getTypeId())) {
						bankLog.setTaskId(incomeRequest.getId());
						bankLog.setTaskType(SysBalTrans.TASK_TYPE_THIRD_TRANSFER);
						bankLog.setOrderNo(incomeRequest.getOrderNo());
						bankLog.setMatchWay(BankLogMatchWay.OrderFindFlow.getWay());
						systemAccountManager.rpush(
								new SysBalPush(bankLog.getFromAccount(), SysBalPush.CLASSIFY_BANK_LOG_, bankLog));
					}
				}
			} catch (Exception e) {
				log.error("SYSBALPUSH MANULAL", e);
			}
			boolean isActiveFlow = null != bankLog && bankLog.getAmount() != null
					&& bankLog.getAmount().compareTo(BigDecimal.ZERO) > 0;
			// 下发任务 下发匹配
			if (isActiveFlow) {
				accountService.dealMatch(incomeRequest.getToId());
				// 第三方下发都出款卡 手工匹配订单流水
				log.debug("手工匹配 第三方卡 :{}  下发出款卡 :{}", incomeRequest.getFromId(), bankLog.getFromAccount());
				accountService.removeLockedRecordThirdDrawToOutCard(incomeRequest.getFromId(),
						bankLog.getFromAccount());
				// 手动匹配 删除未确认的缓存
				accountService.deleteUnfinishedDrawInThirdByMatched(incomeRequest.getToId());
				// 删除 提现时间
				accountService.saveDrawTime(incomeRequest.getToId(), false);
			}
		}
	}

	/**
	 * 描述:流水匹配之后,如果是支转银则处理未匹配的订单
	 *
	 * @param matchedReq
	 *            已匹配的订单
	 * @param bankLog
	 *            已匹配的流水
	 */
	@Override
	@Transactional
	public void match4FlowsByAliInBankAccount(BizIncomeRequest matchedReq, BizBankLog bankLog) {
		try {
			log.debug("支转银流水匹配订单之后处理未匹配订单:{},状态:{},流水id:{},流水状态:{}", matchedReq.getOrderNo(), matchedReq.getStatus(),
					bankLog.getId(), bankLog.getStatus());
			if (Objects.isNull(matchedReq) || Objects.isNull(bankLog)) {
				log.info("支转银流水匹配订单之后处理参数为空!");
				return;
			}
			if (!IncomeRequestStatus.Matched.getStatus().equals(matchedReq.getStatus())) {
				log.info("支转银流水匹配订单之后处理,订单未匹配!订单Id参数:{},订单号:{}", matchedReq.getId(), matchedReq.getOrderNo());
				return;
			}
			if (!BankLogStatus.Matched.getStatus().equals(bankLog.getStatus())) {
				log.info("支转银流水匹配订单之后处理,流水未匹配!流水Id参数:{}", bankLog.getId());
				return;
			}
			List<BizIncomeRequest> list = findUnmatchedListByOrderNo(matchedReq.getOrderNo());
			if (CollectionUtils.isEmpty(list)) {
				log.info("支转银流水匹配订单之后处理,查不到未匹配的订单!");
				return;
			}
			List<Long> unmatchReqIds = list.stream().map(p -> p.getId()).collect(Collectors.toList());
			if (CollectionUtils.isEmpty(unmatchReqIds)) {
				log.info("支转银流水匹配订单之后获取未匹配订单为空!参数:{}", matchedReq.getOrderNo());
			}
			update4AliBankInOrders(unmatchReqIds, IncomeRequestStatus.MATCHED4SUBINBANKALI.getStatus(),
					matchedReq.getOrderNo());
		} catch (Exception e) {
			log.error("支转银流水匹配订单之后处理失败!异常:", e);
		}
	}

	/**
	 * 如果是收款账号是支付宝转银行卡类型,且一个订单号有多个收款账号 则匹配的时候一起匹配
	 *
	 * @param toMatchIncomeReqList
	 */
	@Transactional(rollbackOn = Exception.class)
	@Override
	public void match4OrdersByAliInBankAccount(List<BizIncomeRequest> toMatchIncomeReqList) {
		try {
			if (CollectionUtils.isEmpty(toMatchIncomeReqList)) {
				return;
			}
			log.info("一个订单有多个收款账号 匹配参数:{}", toMatchIncomeReqList.toString());
			boolean matched = false;
			ThreadLocal<BizIncomeRequest> matchedReq = null;
			for (BizIncomeRequest o : toMatchIncomeReqList) {
				if (o.getAmount().intValue() != o.getAmount().floatValue()) {
					List<BizIncomeRequest> incomeResquests = incomeRequestRepository.findIncomeCounts(o.getToId(),
							o.getAmount());
					if (null != incomeResquests && incomeResquests.size() > 1
							&& o.getAmount().intValue() != o.getAmount().floatValue()) {
						log.info("存在同一张入款卡有多笔相同金额未匹配的单,金额:{},订单:{}", o.getAmount(), o.getOrderNo());
						return;
					}
					// 入款与流水记录匹配最大时间间隔（小时）
					int validIntervalTimeHour = Integer.parseInt(
							MemCacheUtils.getInstance().getSystemProfile().getOrDefault("INCOME_MATCH_HOURS", "2"));
					// 规则：入款帐号相同--》入款金额相等--》入款时间在设置范围
					Date tradingStart = CommonUtils.string2Date(CommonUtils.getDateFormat2Str(o.getCreateTime()));
					Date tradingEnd = CommonUtils.string2Date(CommonUtils
							.getDateFormat2Str(DateUtils.addHours(o.getCreateTime(), validIntervalTimeHour)));
					Specification<BizBankLog> specification = DynamicSpecifications.build(null, BizBankLog.class,
							new SearchFilter("tradingTime", Operator.GTE, tradingStart),
							new SearchFilter("tradingTime", Operator.LTE, tradingEnd),
							new SearchFilter("fromAccount", Operator.EQ, o.getToId()),
							new SearchFilter("status", Operator.EQ, BankLogStatus.Matching.getStatus()),
							new SearchFilter("amount", Operator.EQ, o.getAmount()));
					List<BizBankLog> logs = bankLogService.findAll(specification);
					if (CollectionUtils.isEmpty(logs)) {
						// 如果银行流水交易时间是 00:00:00 则根据流水创建时间查询.(不论是交易时间和创建时间都应该大于订单创建时间)
						specification = DynamicSpecifications.build(null, BizBankLog.class,
								new SearchFilter("createTime", Operator.GTE, tradingStart),
								new SearchFilter("createTime", Operator.LTE, tradingEnd),
								new SearchFilter("fromAccount", Operator.EQ, o.getToId()),
								new SearchFilter("status", Operator.EQ, BankLogStatus.Matching.getStatus()),
								new SearchFilter("amount", Operator.EQ, o.getAmount()));
						logs = bankLogService.findAll(specification);
					}
					if (!CollectionUtils.isEmpty(logs)) {
						// 如果是平台会员入款向平台反馈--> 确认
						BizBankLog flow = logs.get(0);
						matched = ack(flow, o, IncomeRequestStatus.Matched.getMsg(),
								IncomeRequestStatus.Matched.getMsg(), null);
						log.debug("一个订单号:{} 有多个收款账号 某一个订单 id:{} 流水id:{}  匹配结果:{}", o.getOrderNo(), o.getId(),
								flow.getId(), matched);
						matchedReq.set(o);
						flow.setTaskId(o.getId());
						flow.setTaskType(SysBalTrans.TASK_TYPE_INCOME);
						flow.setOrderNo(o.getOrderNo());
						flow.setMatchWay(BankLogMatchWay.OrderFindFlow.getWay());
						systemAccountManager
								.rpush(new SysBalPush(flow.getFromAccount(), SysBalPush.CLASSIFY_BANK_LOG_, flow));
						break;
					}
				}
			}
			if (matched && Objects.nonNull(matchedReq)) {
				// 如果其中某一条记录匹配了 则更新同一个订单号对应不同收款账号的 入款记录
				List<Long> list = toMatchIncomeReqList.stream().filter(p -> p.getId().equals(matchedReq.get().getId()))
						.map(p -> p.getId()).collect(Collectors.toList());
				update4AliBankInOrders(list, IncomeRequestStatus.MATCHED4SUBINBANKALI.getStatus(),
						matchedReq.get().getOrderNo());
				matchedReq.remove();
			}
		} catch (Exception e) {
			log.error("支付宝转银行卡订单号多个收款账号匹配异常, error:", e);
		}
	}

	/**
	 * 处理临时占用额度的数据
	 *
	 * @param bankLog
	 */
	public void dealOccuTempLimit(BizBankLog bankLog) {
		try {
			AccountBaseInfo base = accountService.getFromCacheById(bankLog.getFromAccount());
			if (base == null || !Objects.equals(base.getType(), AccountType.InBank.getTypeId())) {
				return;
			}
			Object obj = redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACCOUNT_INCOME_AMOUNT)
					.get(base.getId().toString());
			if (Objects.nonNull(obj)) {
				int expire = CommonUtils.getAccountInComeExpireTime() * 60 * 1000;
				long currTime = System.currentTimeMillis();
				List<AccountIncome> list = JSON.parseArray((String) obj, AccountIncome.class);
				list = list.stream()
						.filter(p -> currTime - p.getIncomeTime() < expire
								&& p.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP)
										.compareTo(bankLog.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP)) != 0)
						.collect(Collectors.toList());
				if (CollectionUtils.isEmpty(list)) {
					redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACCOUNT_INCOME_AMOUNT)
							.delete(base.getId().toString());
				} else {
					redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACCOUNT_INCOME_AMOUNT)
							.put(base.getId().toString(), JSON.toJSON(list).toString());
				}
			}
		} catch (Exception e) {
			log.error("dealOccuTempLimit>>处理临时占用额度数据失败,id {}", bankLog == null ? "" : bankLog.getFromAccount());
		}
	}

	/**
	 * 保存公司用款记录
	 *
	 * @param handicap
	 *            那个盘口的公司用款（无:则：为空）
	 *
	 * @param th3Account
	 *            第三方账号
	 * @param oppBankType
	 *            收款方银行类型
	 * @param oppAccount
	 *            收款方银行账号
	 * @param oppOwner
	 *            收款方姓名
	 * @param amt
	 *            汇款金额 (大于零)
	 * @param fee
	 *            汇款手续费(大于等于零)
	 * @param operator
	 *            操作者 （not null）
	 * @param remark
	 *            备注
	 */
	@Override
	@Transactional
	public BizIncomeRequest registCompanyExpense(BizHandicap handicap, BizAccount th3Account, String oppBankType,
			String oppAccount, String oppOwner, BigDecimal amt, BigDecimal fee, SysUser operator, String remark) {
		BizIncomeRequest o = new BizIncomeRequest();
		o.setToId(null);
		o.setFromId(th3Account.getId());
		o.setHandicap(Objects.isNull(handicap) ? null : handicap.getId());
		o.setLevel(0);
		o.setToBankType(oppBankType);
		o.setToOwner(oppOwner);
		o.setToAccount(oppAccount);
		o.setOperator(operator.getId());
		o.setAmount(amt);
		o.setFee(fee);
		o.setCreateTime(new Date());
		o.setOrderNo(System.currentTimeMillis() + StringUtils.EMPTY);
		o.setRemark(StringUtils.EMPTY);
		o.setType(IncomeRequestType.BizUseMoney.getType());
		o.setFromAccount(th3Account.getAccount());
		o.setMemberUserName(StringUtils.EMPTY);
		o.setMemberRealName(oppOwner);
		o.setStatus(IncomeRequestStatus.Matching.getStatus());
		BigDecimal thirdBal = Objects.nonNull(th3Account.getBalance()) ? th3Account.getBalance() : BigDecimal.ZERO;
		BigDecimal thirdBankBal = Objects.nonNull(th3Account.getBankBalance()) ? th3Account.getBankBalance()
				: BigDecimal.ZERO;
		o.setThirdBalance(thirdBal);
		o.setThirdBankBalance(thirdBankBal);
		o.setRemark(CommonUtils.genRemark(StringUtils.EMPTY, "公司用款-" + StringUtils.trimToEmpty(remark), new Date(),
				operator.getUid()));
		return incomeRequestRepository.saveAndFlush(o);
	}

	/**
	 * 确认：公司用款转账失败
	 *
	 * @param inReqId
	 *            参考
	 *            {@link IncomeRequestService#registCompanyExpense(BizHandicap,BizAccount, String, String, String, BigDecimal, BigDecimal, SysUser, String)}
	 *            返回值的主键
	 * @param operator
	 *            确认人
	 * @param remark
	 *            备注
	 */
	@Override
	@Transactional
	public void rollBackCompanyExpense(Long inReqId, SysUser operator, String remark) {
		Objects.requireNonNull(inReqId, "参数为空");
		Objects.requireNonNull(operator, "操作者为空");
		BizIncomeRequest req = incomeRequestRepository.findById2(inReqId);
		Objects.requireNonNull(req, "公司用款记录不存在");
		reject2CurrSys(inReqId, remark, operator);
		log.info(
				"CompanyExpense [ ROLLBACK ] >> reqId: {} th3: {} handicap: {} oppBank: {}  oppAccount: {} oppOwner: {} amount: {} fee: {}",
				inReqId, req.getFromId(), req.getHandicap(), req.getToBankType(), req.getToAccount(), req.getToOwner(),
				req.getAmount(), req.getFee());
	}

	/**
	 * 确认：公司用款转账成功
	 *
	 * @param inReqId
	 *            参考
	 *            {@link IncomeRequestService#registCompanyExpense(BizHandicap,BizAccount, String, String, String, BigDecimal, BigDecimal, SysUser, String)}
	 *            返回值的主键
	 * @param operator
	 *            确认人
	 * @param remark
	 *            备注
	 */
	@Override
	@Transactional
	public void commitCompanyExpense(Long inReqId, SysUser operator, String remark) {
		BizIncomeRequest req = incomeRequestRepository.findById2(inReqId);
		Objects.requireNonNull(req, "该公司用款记录不存在");
		BizTransactionLog o = transactionLogService.findByOrderIdAndType(inReqId, req.getType());
		if (Objects.nonNull(o))
			return;
		o = new BizTransactionLog();
		o.setAmount(req.getAmount());
		o.setFee(req.getFee());
		o.setToBanklogId(null);
		o.setOrderId(inReqId);
		o.setConfirmor(operator.getId());
		o.setOperator(req.getOperator());
		o.setCreateTime(new Date());
		o.setType(req.getType());
		o.setToAccount(req.getToId());
		o.setDifference(BigDecimal.ZERO);
		o.setFromAccount(req.getFromId());
		o.setRemark(CommonUtils.genRemark(o.getRemark(), remark, o.getCreateTime(), operator.getUid()));
		transactionLogService.save(o);
		log.info(
				"CompanyExpense [ COMMIT ] >> reqId: {} th3: {} handicap: {} oppBank: {}  oppAccount: {} oppOwner: {} amount: {} fee: {}",
				inReqId, req.getFromId(), req.getHandicap(), req.getToBankType(), req.getToAccount(), req.getToOwner(),
				req.getAmount(), req.getFee());
	}

}
