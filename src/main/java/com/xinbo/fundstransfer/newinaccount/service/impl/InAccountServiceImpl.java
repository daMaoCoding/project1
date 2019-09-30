package com.xinbo.fundstransfer.newinaccount.service.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.assign.Constants;
import com.xinbo.fundstransfer.component.net.http.newpay.HttpClient4NewPay;
import com.xinbo.fundstransfer.component.net.http.newpay.PlatformNewPayService;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.ResponseDataNewPay;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizAccountLevel;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.BizLevel;
import com.xinbo.fundstransfer.domain.enums.*;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.repository.AccountLevelRepository;
import com.xinbo.fundstransfer.domain.repository.AccountRepository;
import com.xinbo.fundstransfer.newinaccount.dto.PageDTO;
import com.xinbo.fundstransfer.newinaccount.dto.input.*;
import com.xinbo.fundstransfer.newinaccount.dto.output.*;
import com.xinbo.fundstransfer.newinaccount.service.InAccountService;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import com.xinbo.fundstransfer.report.SystemAccountManager;
import com.xinbo.fundstransfer.restful.v2.TokenValidation;
import com.xinbo.fundstransfer.service.*;
import com.xinbo.fundstransfer.unionpay.ysf.util.YSFLocalCacheUtil;
import com.xinbo.fundstransfer.utils.randutil.InAccountRandUtil;
import com.xinbo.fundstransfer.utils.randutil.InAccountRandUtilResponse;
import com.xinbo.fundstransfer.utils.randutil.NoAvailableRandomException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.*;
import rx.Observable;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Administrator
 */
@Service
@Slf4j
public class InAccountServiceImpl extends TokenValidation implements InAccountService {
	@Autowired
	@Lazy
	private AccountService accountService;
	@Autowired
	private AccountRepository accountRepository;
	@Autowired
	private HandicapService handicapService;
	@Autowired
	private AccountExtraService accountExtraService;
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private AccountLevelRepository accountLevelRepository;
	@Autowired
	private LevelService levelService;
	@Autowired
	private CabanaService cabanaService;
	@Autowired
	AllocateTransService transService;
	@Autowired
	private AllocateIncomeAccountService incomeAccountAllocateService;
	@Autowired
	private RedisService redisService;
	@Autowired
	@Lazy
	private AccountChangeService accountChangeService;
	@Autowired
	@Qualifier("selectAccountToPayScript")
	private RedisScript<String> selectAccountToPayScript;
	@Autowired
	private InAccountRandUtil randUtil;
	@Autowired
	private YSFLocalCacheUtil ySFLocalCacheUtil;
	@Autowired
	private SystemAccountManager systemAccountManager;
	/** 本地缓存-- 通道id->在用的入款银行卡id集合 */
	private LoadingCache<Number, List<Integer>> accountIdsAvailableCache = CacheBuilder.newBuilder().maximumSize(1000)
			.expireAfterWrite(7, TimeUnit.DAYS).initialCapacity(500).build(new CacheLoader<Number, List<Integer>>() {
				@Override
				public List<Integer> load(Number number) {
					if (ObjectUtils.isEmpty(accountRepository)) {
						return null;
					}
					try {
						TimeUnit.SECONDS.sleep(1L);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					List<Integer> pocIdAndAccountIds = accountRepository
							.findPassageIdAndAccountIdsMap(number.longValue(), System.currentTimeMillis());
					log.debug("刷新缓存:{},时间:{},结果:{}", number, System.currentTimeMillis(), pocIdAndAccountIds);
					return pocIdAndAccountIds;
				}
			});

	/** 本地缓存-- 通道id->在用的入款银行卡集合 */
	private LoadingCache<Number, List<BizAccount>> accountAvailableCache = CacheBuilder.newBuilder().maximumSize(1000)
			.expireAfterWrite(7, TimeUnit.DAYS).initialCapacity(500).build(new CacheLoader<Number, List<BizAccount>>() {
				@Override
				public List<BizAccount> load(Number number) {
					if (ObjectUtils.isEmpty(accountRepository)) {
						return null;
					}
					try {
						TimeUnit.SECONDS.sleep(1L);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					List<BizAccount> pocIdAndAccountIds = accountRepository.findAccountByPassageId(number.longValue(),
							System.currentTimeMillis());
					log.debug("刷新缓存:{},时间:{},结果:{}", number, System.currentTimeMillis(), pocIdAndAccountIds);
					return pocIdAndAccountIds;
				}
			});

	/** 本地缓存-- 通道id->在用的入款银行卡集合 */
	private LoadingCache<String, List<Integer>> handicapAvailableCache = CacheBuilder.newBuilder().maximumSize(1000)
			.expireAfterWrite(30, TimeUnit.SECONDS).initialCapacity(500)
			.build(new CacheLoader<String, List<Integer>>() {
				@Override
				public List<Integer> load(String key) {
					if (ObjectUtils.isEmpty(accountRepository)) {
						return null;
					}
					try {
						TimeUnit.SECONDS.sleep(1L);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					String[] handicapLevel = key.split("-");
					List<Integer> pocIdAndAccountIds = accountRepository.findAccountByHandicapCurrSysLevel(
							Integer.parseInt(handicapLevel[0]), Integer.parseInt(handicapLevel[1]),
							System.currentTimeMillis());
					log.debug("刷新缓存:{},时间:{},结果:{}", key, System.currentTimeMillis(), pocIdAndAccountIds);
					return pocIdAndAccountIds;
				}
			});

	/**
	 * 1.5.3 通道管理 – 新增入款通道/修改通道资料 – 查询银行卡列表
	 * 
	 * @param inputDTO
	 * @param handicap
	 * @return
	 */
	@Override
	public List<FindColOutputDTO> findCol(FindColInputDTO inputDTO, BizHandicap handicap) {
		try {
			List<BizAccount> accountList = accountRepository.findAll((root, criteriaQuery, criteriaBuilder) -> {
				List<Predicate> predicates = new ArrayList<>();
				Predicate p = criteriaBuilder.equal(root.get("handicapId").as(Integer.class), handicap.getId());
				predicates.add(p);
				if (Objects.equals(inputDTO.getType(), InBankSubType.IN_BANK_YSF.getSubType())) {
					Predicate p2 = criteriaBuilder.equal(root.get("subType").as(Integer.class), inputDTO.getType());
					predicates.add(p2);
				} else {
					Predicate p2 = criteriaBuilder.equal(root.get("subType").as(Integer.class), inputDTO.getType());
					Predicate p3 = criteriaBuilder.equal(root.get("flag").as(Integer.class),
							AccountFlag.PC.getTypeId());
					Predicate p4 = criteriaBuilder.or(p2, p3);
					predicates.add(p4);
				}

				// 需求 5715
				Predicate p7 = criteriaBuilder.equal(root.get("status").as(Integer.class),
						AccountStatus.Normal.getStatus());
				predicates.add(p7);

				Predicate p3 = criteriaBuilder.equal(root.get("type").as(Integer.class),
						AccountType.InBank.getTypeId());
				predicates.add(p3);
				Predicate p6 = criteriaBuilder.isNotNull(root.get("province").as(String.class));
				predicates.add(p6);
				Predicate p5 = criteriaBuilder.isNotNull(root.get("city").as(String.class));
				predicates.add(p5);
				if (!ObjectUtils.isEmpty(inputDTO.getBankName())) {
					Predicate p1 = criteriaBuilder.equal(root.get("bankType").as(String.class), inputDTO.getBankName());
					predicates.add(p1);
				}
				if (!ObjectUtils.isEmpty(inputDTO.getCardNo())) {
					Predicate p1 = criteriaBuilder.equal(root.get("account").as(String.class), inputDTO.getCardNo());
					predicates.add(p1);
				}
				if (!ObjectUtils.isEmpty(inputDTO.getIsBind())) {
					if (inputDTO.getIsBind().equals(0)) {
						Predicate p1 = criteriaBuilder.isNull(root.get("passageId").as(Long.class));
						predicates.add(p1);
					} else {
						Predicate p1 = criteriaBuilder.isNotNull(root.get("passageId").as(Long.class));
						predicates.add(p1);
						Predicate p4 = criteriaBuilder.equal(root.get("passageId").as(Long.class), inputDTO.getPocId());
						predicates.add(p4);
					}
				} else {
					Predicate p4 = criteriaBuilder.or(
							criteriaBuilder.equal(root.get("passageId").as(Long.class), inputDTO.getPocId()),
							criteriaBuilder.isNull(root.get("passageId").as(Long.class)));
					predicates.add(p4);
				}
				Predicate[] ps = new Predicate[predicates.size()];
				criteriaQuery.where(criteriaBuilder.and(predicates.toArray(ps)));
				return criteriaQuery.getRestriction();
			}, new Sort(Sort.Direction.DESC, "updateTime"));
			List<FindColOutputDTO> res = new ArrayList<>();
			if (!ObjectUtils.isEmpty(accountList)) {
				accountList.stream().forEach(p -> {
					FindColOutputDTO outputDTO = new FindColOutputDTO();
					outputDTO.setBankName(p.getBankType());
					outputDTO.setBankOpen(p.getBankName());
					outputDTO.setCardNo(p.getAccount());
					outputDTO.setMinInMoney(p.getMinInAmount());
					outputDTO.setOid(Integer.valueOf(handicap.getCode()));
					outputDTO.setPocId(p.getPassageId());
					outputDTO.setOpenName(p.getOwner());
					outputDTO.setProvince(p.getProvince());
					outputDTO.setCity(p.getCity());
					outputDTO.setStatus2(!ObjectUtils.isEmpty(p.getStatus())
							&& AccountStatus.Normal.getStatus().equals(p.getStatus())
							&& accountChangeService.currCredits(accountService.getFromCacheById(p.getId())) > 0
									? (byte) 1
									: 0);
					res.add(outputDTO);
				});
			}
			return res;
		} catch (Exception e) {
			log.error("新增入款通道/修改通道资料 – 查询银行卡列表 失败:", e);
			return null;
		}
	}

	/**
	 * 1.5.4 银行卡管理查询分页列表
	 * 
	 * @param inputDTO
	 * @param handicap
	 * @return
	 */
	@Override
	public PageDTO<FindPageOutputDTO> findPage(FindPageInputDTO inputDTO, BizHandicap handicap) {
		try {
			Page<BizAccount> bizAccountPage = accountRepository.findAll((root, criteriaQuery, criteriaBuilder) -> {
				List<Predicate> predicates = new ArrayList<>();
				Predicate p = criteriaBuilder.equal(root.get("handicapId").as(Integer.class), handicap.getId());
				predicates.add(p);
				Predicate p2 = criteriaBuilder.equal(root.get("type").as(Integer.class),
						AccountType.InBank.getTypeId());
				predicates.add(p2);
				Predicate p3 = criteriaBuilder.equal(root.get("status").as(Integer.class),
						AccountStatus.Normal.getStatus());
				predicates.add(p3);
				if (ObjectUtils.isEmpty(inputDTO.getType())) {
					CriteriaBuilder.In in = criteriaBuilder.in(root.get("subType").as(Integer.class));
					List<Integer> subTypes = InBankSubType.getAllSubType();
					for (int i = 0, len = subTypes.size(); i < len; i++) {
						in.value(subTypes.get(i));
					}
					predicates.add(in);
				} else {
					Predicate p6 = criteriaBuilder.equal(root.get("subType").as(Integer.class), inputDTO.getType());
					predicates.add(p6);
				}

				if (!ObjectUtils.isEmpty(inputDTO.getBankName())) {
					Predicate p1 = criteriaBuilder.like(root.get("bankType").as(String.class),
							inputDTO.getBankName() + "%");
					predicates.add(p1);
				}
				if (!ObjectUtils.isEmpty(inputDTO.getCardNo())) {
					Predicate p1 = criteriaBuilder.equal(root.get("account").as(String.class), inputDTO.getCardNo());
					predicates.add(p1);
				}
				if (!ObjectUtils.isEmpty(inputDTO.getMinInMoneyNoLimit())) {
					if (inputDTO.getMinInMoneyNoLimit().intValue() == 0) {
						Predicate p1 = criteriaBuilder.or(
								criteriaBuilder.isNull(root.get("minInAmount").as(BigDecimal.class)),
								criteriaBuilder.equal(root.get("minInAmount"), 0));
						predicates.add(p1);
					} else {
						Predicate p1 = criteriaBuilder.and(
								criteriaBuilder.isNotNull(root.get("minInAmount").as(BigDecimal.class)),
								criteriaBuilder.notEqual(root.get("minInAmount"), 0));
						predicates.add(p1);
					}
				}
				if (!ObjectUtils.isEmpty(inputDTO.getMinInMoneyStart())) {
					Predicate p4 = criteriaBuilder.ge(root.get("minInAmount").as(BigDecimal.class),
							new BigDecimal(inputDTO.getMinInMoneyStart()));
					predicates.add(p4);
				}
				if (!ObjectUtils.isEmpty(inputDTO.getMinInMoneyEnd())) {
					Predicate p5 = criteriaBuilder.le(root.get("minInAmount").as(BigDecimal.class),
							new BigDecimal(inputDTO.getMinInMoneyEnd()));
					predicates.add(p5);
				}
				if (!ObjectUtils.isEmpty(inputDTO.getBindStatus())) {
					if (inputDTO.getBindStatus().equals(0)) {
						Predicate p1 = criteriaBuilder.isNull(root.get("passageId").as(Long.class));
						predicates.add(p1);
					} else {
						Predicate p1 = criteriaBuilder.isNotNull(root.get("passageId").as(Long.class));
						predicates.add(p1);
					}
				}
				Predicate[] ps = new Predicate[predicates.size()];
				criteriaQuery.where(criteriaBuilder.and(predicates.toArray(ps)));
				return criteriaQuery.getRestriction();
			}, new PageRequest(ObjectUtils.isEmpty(inputDTO.getPageNo()) || inputDTO.getPageNo() == 0 ? 0
					: inputDTO.getPageNo() - 1, inputDTO.getPageSize()));
			List<FindPageOutputDTO> res = new ArrayList<>();
			List<BizAccount> accountList;
			PageDTO<FindPageOutputDTO> pageDTO = new PageDTO();
			if (!ObjectUtils.isEmpty(bizAccountPage)) {
				accountList = bizAccountPage.getContent();
				accountList.stream().forEach(p -> {
					FindPageOutputDTO outputDTO = new FindPageOutputDTO();
					outputDTO.setBankName(p.getBankType());
					outputDTO.setBankOpen(p.getBankName());
					outputDTO.setCardNo(p.getAccount());
					outputDTO.setMinInMoney(p.getMinInAmount());
					outputDTO.setOid(Integer.valueOf(handicap.getCode()));
					outputDTO.setPocId(p.getPassageId());
					outputDTO.setOpenName(p.getOwner());
					outputDTO.setStatus2(!ObjectUtils.isEmpty(p.getStatus())
							&& AccountStatus.Normal.getStatus().equals(p.getStatus()) && accountChangeService
									.currCredits(accountService.getFromCacheById(p.getId())).intValue() > 0 ? (byte) 1
											: 0);
					res.add(outputDTO);
				});
			}
			pageDTO.setResultList(res);
			pageDTO.setPageNo(inputDTO.getPageNo());
			pageDTO.setPageSize(inputDTO.getPageSize());
			pageDTO.setTotalPageNumber(bizAccountPage.getTotalPages());
			pageDTO.setTotalRecordNumber(Long.valueOf(bizAccountPage.getTotalElements()).intValue());
			return pageDTO;
		} catch (Exception e) {
			log.error("银行卡管理查询分页列表失败:", e);
			return null;
		}
	}

	/**
	 * 1.5.5 银行卡管理统计
	 * 
	 * @param inputDTO
	 * @param handicap
	 * @return
	 */
	@Override
	public StaticstiOutputDTO statisctic(StaticsticInputDTO inputDTO, BizHandicap handicap) {
		try {
			String sql = "select * from (\n"
					+ "(select count(1) as counts  from fundsTransfer.biz_account a where a.handicap_id=?1  and a.status=1 and a.sub_type in(0,1,2,3 ) ) as al ,\n"
					+ "(select count(1) as unbinds  from fundsTransfer.biz_account b where b.handicap_id=?1 and b.status=1 and b.sub_type in(0,1,2,3 ) and b.passage_id is null ) as ub,\n"
					+ "(select count(1) as binds  from fundsTransfer.biz_account c where c.handicap_id=?1 and c.status=1 and c.sub_type in(0,1,2,3 ) and c.passage_id is not null ) as b \n"
					+ ")   ";
			Object[] counts = (Object[]) entityManager.createNativeQuery(sql).setParameter(1, handicap.getId())
					.getSingleResult();
			if (!ObjectUtils.isEmpty(counts)) {
				StaticstiOutputDTO outputDTO = new StaticstiOutputDTO();
				if (!ObjectUtils.isEmpty(counts[0])) {
					outputDTO.setMnt1(NumberUtils.parseNumber(counts[0].toString(), Number.class));
				}
				if (!ObjectUtils.isEmpty(counts[1])) {
					outputDTO.setMnt2(NumberUtils.parseNumber(counts[1].toString(), Number.class));
				}
				if (!ObjectUtils.isEmpty(counts[2])) {
					outputDTO.setMnt3(NumberUtils.parseNumber(counts[2].toString(), Number.class));
				}
				return outputDTO;
			}
			return null;
		} catch (Exception e) {
			log.error("统计失败:", e);
			return null;
		}
	}

	/**
	 * 1.5.6 银行卡管理取消绑定
	 * 
	 * @param inputDTO
	 * @param handicap
	 * @return
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public String modifyBind(ModifyBindInputDTO inputDTO, BizHandicap handicap) {
		try {
			AccountBaseInfo account = accountService.getFromCacheByHandicapIdAndAccount(handicap.getId(),
					inputDTO.getCardNo());
			if (ObjectUtils.isEmpty(account)) {
				return "账号不存在!";
			}
			if (!account.getType().equals(AccountType.InBank.getTypeId())
					&& InBankSubType.getAllSubType().contains(account.getSubType())) {
				return "账号不属于入款卡不能取消绑定!";
			}
			BizAccount account1 = accountRepository.findByHandicapIdAndAccount(handicap.getId(), inputDTO.getCardNo());
			if (inputDTO.getCancelStatus().equals(0)) {
				account1.setPassageId(inputDTO.getPocId().longValue());
			} else {
				account1.setPassageId(null);
			}
			BizAccount account2 = accountRepository.saveAndFlush(account1);
			account.setPassageId(account2.getPassageId());
			accountService.flushCache(account);
			accountExtraService.saveAccountExtraLog(account1, account2, inputDTO.getOperator());
			return "OK";
		} catch (Exception e) {
			log.error("取消绑定失败:", e);
			return e.getLocalizedMessage();
		}
	}

	/**
	 * 描述 1.5.7 通道管理--删除通道/更新(取消,新增)银行卡绑定/绑定层级 逻辑: 根据传入的通道id和账号和层级信息 新增绑定 变更绑定 删除绑定
	 * 校验逻辑：主要以通道id 和 传入的账号为主 平台全量推送
	 * 
	 * @param inputDTO
	 * @param handicap
	 * @return
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public String updateBind(UpdateBindInputDTO inputDTO, BizHandicap handicap) {
		List<BizAccount> bizAccounts = findBySubTypeAndPassageId(inputDTO.getPocId().longValue());
		if (ObjectUtils.isEmpty(bizAccounts)) {
			// 新增的通道
			String ret = addOrModify(inputDTO, handicap, null);
			accountIdsAvailableCache.invalidate(inputDTO.getPocId());
			return ret;
		} else {
			// 已有通道修改绑定 根据传入的账号查询是否有绑定 有绑定则更新 无绑定则新增绑定
			if (!ObjectUtils.isEmpty(inputDTO.getCardNoCol())) {
				String ret = addOrModify(inputDTO, handicap, bizAccounts);
				accountIdsAvailableCache.invalidate(inputDTO.getPocId());
				return ret;
			} else {
				// 删除账号绑定
				bizAccounts.stream().forEach(p -> {
					p.setPassageId(null);
					if (!AccountStatus.Normal.getStatus().equals(p.getStatus())) {
						p.setStatus(AccountStatus.StopTemp.getStatus());
					}
				});
				accountService.saveIterable(bizAccounts);
				accountIdsAvailableCache.invalidate(inputDTO.getPocId());
				// noticeFreshCache(inputDTO.getPocId());
				return "OK";
			}
		}
	}

	/**
	 * // 新增或者改变通道
	 * 
	 * @param inputDTO
	 *            接口入参
	 * @param handicap
	 *            盘口
	 * @param oldBizAccounts
	 *            原有的绑定信息
	 * @return
	 */
	@Transactional(rollbackFor = Exception.class)
	String addOrModify(UpdateBindInputDTO inputDTO, BizHandicap handicap, List<BizAccount> oldBizAccounts) {
		List<Integer> accountIds = new ArrayList<>();
		if (!ObjectUtils.isEmpty(inputDTO.getCardNoCol())) {
			try {
				List<Map<String, Object>> cardNoCols = inputDTO.getCardNoCol();
				List<String> newAccounts = new ArrayList<String>();
				Map<String, Integer> newAccountMap = new HashMap<String, Integer>();
				if (!ObjectUtils.isEmpty(cardNoCols)) {
					cardNoCols.forEach(p -> {
						Object cardNo = p.get("cardNo");
						Object cardType = p.get("cardType");
						if (!ObjectUtils.isEmpty(cardNo) && !ObjectUtils.isEmpty(cardType)) {
							newAccounts.add(cardNo.toString());
							newAccountMap.put(cardNo.toString(), Integer.parseInt(cardType.toString()));
						}
					});
				}
				List<BizAccount> accountList = findByAccounts(newAccounts);
				if (ObjectUtils.isEmpty(accountList)) {
					return String.format("账号%s不存在不能绑定!", inputDTO.getCardNoCol());
				}
				List<String> normalCardNos = new ArrayList<>();
				accountList.stream().forEach(q -> {
					normalCardNos.add(q.getAccount());
					accountIds.add(q.getId());
				});
				List<BizAccount> accountListToSave = findAccountsByIdsForUpdate(accountIds);
				// 新增
				final List<BizAccount> accountListToAdd = new ArrayList<>();
				// 修改绑定通道
				final List<BizAccount> accountListToModify = new ArrayList<>();
				// 删除绑定
				List<BizAccount> bizAccountListToDeleteBind = new ArrayList<>();
				List<Integer> oldToDeleteBindAccountIds = new ArrayList<>();
				if (!ObjectUtils.isEmpty(oldBizAccounts)) {
					// 旧绑定
					oldBizAccounts.removeAll(accountListToSave);
					// 如果旧绑定记录比新增的少 则为空 如果旧记录比新增的多则不为空
					bizAccountListToDeleteBind = oldBizAccounts;
					if (!ObjectUtils.isEmpty(bizAccountListToDeleteBind)) {
						oldToDeleteBindAccountIds = bizAccountListToDeleteBind.stream().map(p -> p.getId())
								.collect(Collectors.toList());
					}
				}
				final List<Integer> oldBizAccountIdsFinal = oldToDeleteBindAccountIds;
				// 根据传入的账号查询的信息
				accountListToSave.stream().forEach(q -> {
					q.setInCardType(newAccountMap.get(q.getAccount()));
					q.setUpdateTime(new Date());
					if (ObjectUtils.isEmpty(q.getPassageId())) {
						// 如果通道id为空 则是新增绑定
						accountListToAdd.add(q);
						q.setPassageId(inputDTO.getPocId().longValue());
					} else {
						if (!ObjectUtils.nullSafeEquals(inputDTO.getPocId().longValue(),
								q.getPassageId().longValue())) {
							// 如果旧绑定的通道id与传入的通道id不一致 则更新
							accountListToModify.add(q);
							/** 删除使用记录 */
							deleteUsedRecord(q.getPassageId(), q.getId());
							q.setPassageId(inputDTO.getPocId().longValue());
						}
					}
					if (!ObjectUtils.isEmpty(oldBizAccountIdsFinal) && !oldBizAccountIdsFinal.contains(q.getId())) {
						q.setPassageId(inputDTO.getPocId().longValue());
					}
				});
				// 解绑部分
				if (!ObjectUtils.isEmpty(bizAccountListToDeleteBind)) {
					bizAccountListToDeleteBind.stream().forEach(p -> {
						/** 删除使用记录 */
						deleteUsedRecord(p.getPassageId(), p.getId());
						p.setPassageId(null);
						AccountBaseInfo baseInfo = accountService.getFromCacheById(p.getId());
						if (!AccountStatus.Normal.getStatus().equals(baseInfo.getStatus())) {
							p.setStatus(baseInfo.getStatus());
						}
						p.setUpdateTime(new Date());

					});
					// 保存删除绑定
					bizAccountListToDeleteBind = accountService.saveIterable(bizAccountListToDeleteBind);
					// TODO 是否删除层级绑定
					List<BizAccountLevel> accountLevelsOld = accountLevelRepository.findByAccountIdIsIn(
							bizAccountListToDeleteBind.stream().map(p -> p.getId()).collect(Collectors.toList()));
					if (!ObjectUtils.isEmpty(accountLevelsOld)) {
						accountLevelRepository.deleteInBatch(accountLevelsOld);
					}
				}
				// 保存新通道和银行卡绑定
				final List<BizAccount> savedAccounts = saveBindBatch(accountListToSave);
				// 保存绑定层级 该通道下的所有银行卡号都绑定传入的层级
				if (!ObjectUtils.isEmpty(inputDTO.getLevelCol())) {
					List<BizLevel> levels = levelService.findByHandicapIdAndCodes(handicap.getId(),
							inputDTO.getLevelCol());
					// 删除旧绑定
					List<BizAccountLevel> accountLevelsOld = accountLevelRepository.findByAccountIdIsIn(accountIds);
					if (!ObjectUtils.isEmpty(accountLevelsOld)) {
						accountLevelRepository.deleteInBatch(accountLevelsOld);
					}

					// 保存新绑定层级账号
					List<BizAccountLevel> accountLevels = new ArrayList<>();
					if (!ObjectUtils.isEmpty(levels)) {
						if (levels.size() > savedAccounts.size()) {
							levels.stream().forEach(p -> savedAccounts.stream().forEach(q -> {
								BizAccountLevel accountLevel = new BizAccountLevel();
								accountLevel.setLevelId(p.getId());
								accountLevel.setAccountId(q.getId());
								accountLevels.add(accountLevel);
							}));
						} else {
							savedAccounts.stream().forEach(p -> levels.stream().forEach(q -> {
								BizAccountLevel accountLevel = new BizAccountLevel();
								accountLevel.setAccountId(p.getId());
								accountLevel.setLevelId(q.getId());
								accountLevels.add(accountLevel);
							}));
						}
						accountLevelRepository.save(accountLevels);
					}
				}
				if (!ObjectUtils.isEmpty(savedAccounts)) {
					savedAccounts.stream().forEach(p -> {
						// 广播同步账号信息 层级信息改变
						accountService.broadCast(p);
						cabanaService.updAcc(p.getId());
					});
				}
				if (!ObjectUtils.isEmpty(accountListToAdd)) {
					// 新增分配
					accountListToAdd.stream()
							.forEach(p -> incomeAccountAllocateService.update(p.getId(), p.getType(), p.getStatus()));
				}
				if (!ObjectUtils.isEmpty(bizAccountListToDeleteBind)) {
					// 更新分配
					bizAccountListToDeleteBind.stream()
							.forEach(p -> incomeAccountAllocateService.update(p.getId(), p.getType(), p.getStatus()));
				}
				return "OK";
			} catch (Exception e) {
				log.error("更新或者新增绑定失败:", e);
				return e.getLocalizedMessage();
			}
		} else

		{
			return "OK";
		}
	}

	/**
	 * 1.5.8 会员支付通道上报选择银行卡
	 * 
	 * @param inputDTO
	 * @param handicap
	 * @return
	 */
	@Override
	public CardForPayOutputDTO cardForPay(CardForPayInputDTO inputDTO, BizHandicap handicap) {
		try {
			// 申请银行卡号 不用于扫码
			List<String> requestCardNos = new ArrayList<>();
			// 请求的通道id+ 申请扫码用的银卡账号
			List<String> requestCardNosForQr = new ArrayList<>();
			AtomicBoolean existQrPassageId = new AtomicBoolean(false);
			ThreadLocal<Long> pocIdCurrent = new ThreadLocal<>();

			// 通道id和缓存的卡号id map
			Map<Long, List<Integer>> pocIds = new HashMap<>(16);
			Map<Long, List<Integer>> pocIdsQr = new HashMap<>(16);
			/**
			 * 请求入款场景： 1、单通道普通 2、单通道QR 3、单通道混合 4、多通道普通
			 */
			// 标识 普通银行入款多个通道 场景4
			boolean type0 = inputDTO.getType().intValue() == InBankSubType.IN_BANK_DEFAULT.getSubType().intValue()
					&& inputDTO.getPocIdMapCardNosCol().size() > 1;

			ArrayList accountIds = new ArrayList() {
				{
					for (Map.Entry<String, String> pocId : inputDTO.getPocIdMapCardNosCol().entrySet()) {
						// 如果通道id 有+ 这个字符标识 表示查询扫码用的支转银
						boolean qrRequest = pocId.getKey().indexOf("+") >= 0;
						Number key = Long
								.valueOf(qrRequest ? pocId.getKey().split("\\+")[0].trim() : pocId.getKey().trim());
						if (!type0) {
							pocIdCurrent.set(key.longValue());
						}
						if (!type0 && qrRequest) {
							if (inputDTO.getPocIdMapCardNosCol().size() == 2) {
								existQrPassageId.set(qrRequest);
							}
							String[] accounts = pocId.getValue().indexOf(",") >= 0
									? org.apache.commons.lang3.StringUtils.split(pocId.getValue(), ",")
									: new String[] { pocId.getValue() };
							requestCardNosForQr.addAll(Stream.of(accounts).collect(Collectors.toList()));
						} else {
							// Arrays.asList((pocId.getValue().split(",")))
							String[] accounts = pocId.getValue().indexOf(",") >= 0
									? org.apache.commons.lang3.StringUtils.split(pocId.getValue(), ",")
									: new String[] { pocId.getValue() };
							requestCardNos.addAll(Stream.of(accounts).collect(Collectors.toList()));
						}
						List<Integer> accountIdsFromCache = accountIdsAvailableCache.get(key);
						log.debug("通道对应的缓存数据：通道id {}，结果集 {}", key, accountIdsFromCache.size());
						if (!ObjectUtils.isEmpty(accountIdsFromCache)) {
							addAll(accountIdsFromCache);
						}
						if (qrRequest) {
							pocIdsQr.put(key.longValue(), accountIdsFromCache);
						} else {
							pocIds.put(key.longValue(), accountIdsFromCache);
						}
					}
				}
			};
			log.debug("requestCardNos = {},requestCardNosForQr = {}", ObjectMapperUtils.serialize(requestCardNos),
					ObjectMapperUtils.serialize(requestCardNosForQr));
			log.debug("结果集数据，大小 {}：", accountIds.size());
			if (!ObjectUtils.isEmpty(accountIds)) {
				Set<Integer> failure = transService.buildFailureTrans();
				accountIds = (ArrayList) accountIds.stream().filter(p -> failure == null || !failure.contains(p))
						.distinct().collect(Collectors.toList());
				List<BizAccount> sourceAccountList = accountService.findByIds(accountIds);
				log.debug("sourceAccountList={}", ObjectMapperUtils.serialize(sourceAccountList));
				List<BizAccount> targetAccountList = new ArrayList<>();
				List<BizAccount> targetAccountListForQr = new ArrayList<>();
				if (!ObjectUtils.isEmpty(sourceAccountList)) {
					// 过滤条件1
					List<java.util.function.Predicate<BizAccount>> predicates = predicatesCommon1(inputDTO, true);
					targetAccountList = getBizAccounts(requestCardNos, sourceAccountList, targetAccountList,
							predicates);
					log.debug("过滤最小入款金额结果集数据 size {},卡信息:{}", targetAccountList.size(),
							ObjectMapperUtils.serialize(targetAccountList));
					if (CollectionUtils.isEmpty(targetAccountList)) {
						predicates = predicatesCommon1(inputDTO, false);
						targetAccountList = getBizAccounts(requestCardNos, sourceAccountList, targetAccountList,
								predicates);
					}
					log.debug("非二维码结果集数据 size {},卡信息:{}", targetAccountList.size(),
							ObjectMapperUtils.serialize(targetAccountList));

					targetAccountListForQr = getBizAccounts(requestCardNosForQr, sourceAccountList,
							targetAccountListForQr, predicates);
					log.debug("二维码结果集数据 size {},卡信息:{}", targetAccountListForQr.size(),
							ObjectMapperUtils.serialize(targetAccountListForQr));
				}
				if (existQrPassageId.get()) {
					// 如果有扫码的通道请求必须返回一对信息 不然丢弃
					if (ObjectUtils.isEmpty(targetAccountList) || ObjectUtils.isEmpty(targetAccountListForQr)) {
						return null;
					}
				}
				if (!ObjectUtils.isEmpty(targetAccountList) || !ObjectUtils.isEmpty(targetAccountListForQr)) {
					log.debug("cardForPay>> account {},accountForQr {}", targetAccountList, targetAccountListForQr);
					// 3.22 上线的调用
					// CardForPayOutputDTO outputDTO =
					// returnOutPutDto(pocIdCurrent, type0,
					// pocIdsQr, pocIds,
					// accountForQr, account, inputDTO);

					// 3.27 上线的调用
					CardForPayOutputDTO outputDTO = returnOutPutDtoNew(accountIds, pocIdCurrent, pocIdsQr, pocIds,
							targetAccountListForQr, targetAccountList, inputDTO);
					return outputDTO;
				} else {
					log.error("无可用卡！");
				}
			}
		} catch (ExecutionException e) {
			log.error("获取收款卡失败:", e);
			return null;
		}
		return null;
	}

	/**
	 * 1.5.8 会员支付通道上报选择银行卡
	 *
	 * @param inputDTO
	 * @param handicap
	 * @return
	 */
	@Override
	public CardForPayOutputDTO cardForPay1(CardForPayInputDTO inputDTO, BizHandicap handicap) {
		try {
			// 申请银行卡号 不用于扫码
			List<String> requestCardNos = new ArrayList<>();
			// 请求的通道id+ 申请扫码用的银卡账号
			List<String> requestCardNosForQr = new ArrayList<>();
			AtomicBoolean existQrPassageId = new AtomicBoolean(false);
			ThreadLocal<Long> pocIdCurrent = new ThreadLocal<>();
			/**
			 * 请求入款场景： 1、单通道普通 2、单通道QR 3、单通道混合 4、多通道普通
			 */
			// 标识 普通银行入款多个通道 场景4
			boolean type0 = inputDTO.getType().intValue() == InBankSubType.IN_BANK_DEFAULT.getSubType().intValue()
					&& inputDTO.getPocIdMapCardNosCol().size() > 1;
			ArrayList<Integer> accountIds = new ArrayList();
			Set<Integer> failure = transService.buildFailureTrans();
			Set<Integer> model = redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_NEW_MODEL).entries()
					.entrySet().stream().filter(p -> p.getValue().toString().equals(Constants.YSF_MODEL_OUT + ""))
					.map(p -> Integer.parseInt(p.getKey().toString())).collect(Collectors.toSet());
			for (Map.Entry<String, String> pocId : inputDTO.getPocIdMapCardNosCol().entrySet()) {
				// 如果通道id 有+ 这个字符标识 表示查询扫码用的支转银
				boolean qrRequest = pocId.getKey().indexOf("+") >= 0;
				Number key = Long.valueOf(qrRequest ? pocId.getKey().split("\\+")[0].trim() : pocId.getKey().trim());
				List<BizAccount> accountFromCache = accountAvailableCache.get(key);
				if (!type0) {
					pocIdCurrent.set(key.longValue());
				}
				if (!type0 && qrRequest) {
					if (inputDTO.getPocIdMapCardNosCol().size() == 2) {
						existQrPassageId.set(qrRequest);
					}
					List<String> list = Arrays.asList(pocId.getValue().split(","));
					accountIds
							.addAll(accountFromCache.stream()
									.filter(p -> list.contains(p.getAccount()) && !failure.contains(p.getId())
											&& !model.contains(p.getId()))
									.map(BizAccount::getId).collect(Collectors.toList()));
					requestCardNosForQr.addAll(accountFromCache.stream()
							.filter(p -> list.contains(p.getAccount()) && !failure.contains(p.getId())
									&& !model.contains(p.getId()))
							.map(BizAccount::getAccount).collect(Collectors.toList()));
				} else {
					List<String> list = Arrays.asList(pocId.getValue().split(","));
					accountIds
							.addAll(accountFromCache.stream()
									.filter(p -> list.contains(p.getAccount()) && !failure.contains(p.getId())
											&& !model.contains(p.getId()))
									.map(BizAccount::getId).collect(Collectors.toList()));
					requestCardNos.addAll(accountFromCache.stream()
							.filter(p -> list.contains(p.getAccount()) && !failure.contains(p.getId())
									&& !model.contains(p.getId()))
							.map(BizAccount::getAccount).collect(Collectors.toList()));
				}
				log.debug("通道对应的缓存数据：通道id {}，结果集 {}", key, accountIds.size());
			}
			log.debug("requestCardNos = {},requestCardNosForQr = {}", ObjectMapperUtils.serialize(requestCardNos),
					ObjectMapperUtils.serialize(requestCardNosForQr));
			log.debug("结果集数据，大小 {}：", accountIds.size());
			if (!ObjectUtils.isEmpty(accountIds)) {
				List<BizAccount> sourceAccountList = accountService.findByIds(accountIds);
				log.debug("sourceAccountList={}", ObjectMapperUtils.serialize(sourceAccountList));
				List<BizAccount> targetAccountList = new ArrayList<>();
				List<BizAccount> targetAccountListForQr = new ArrayList<>();
				if (!ObjectUtils.isEmpty(sourceAccountList)) {
					// 过滤条件1
					List<java.util.function.Predicate<BizAccount>> predicates = predicatesCommon1(inputDTO, true);
					targetAccountList = getBizAccounts(requestCardNos, sourceAccountList, targetAccountList,
							predicates);
					log.debug("过滤最小入款金额结果集数据 size {},卡信息:{}", targetAccountList.size(),
							ObjectMapperUtils.serialize(targetAccountList));
					if (CollectionUtils.isEmpty(targetAccountList)) {
						predicates = predicatesCommon1(inputDTO, false);
						targetAccountList = getBizAccounts(requestCardNos, sourceAccountList, targetAccountList,
								predicates);
					}
					log.debug("非二维码结果集数据 size {},卡信息:{}", targetAccountList.size(),
							ObjectMapperUtils.serialize(targetAccountList));

					targetAccountListForQr = getBizAccounts(requestCardNosForQr, sourceAccountList,
							targetAccountListForQr, predicates);
					log.debug("二维码结果集数据 size {},卡信息:{}", targetAccountListForQr.size(),
							ObjectMapperUtils.serialize(targetAccountListForQr));
				}
				if (existQrPassageId.get()) {
					// 如果有扫码的通道请求必须返回一对信息 不然丢弃
					if (ObjectUtils.isEmpty(targetAccountList) || ObjectUtils.isEmpty(targetAccountListForQr)) {
						return null;
					}
				}
				if (!ObjectUtils.isEmpty(targetAccountList) || !ObjectUtils.isEmpty(targetAccountListForQr)) {
					log.debug("cardForPay>> account {},accountForQr {}", targetAccountList, targetAccountListForQr);
					CardForPayOutputDTO outputDTO = returnOutPutDto(accountIds, pocIdCurrent, targetAccountListForQr,
							targetAccountList, inputDTO);
					return outputDTO;
				} else {
					log.error("无可用卡！");
				}
			}
		} catch (ExecutionException e) {
			log.error("获取收款卡失败:", e);
			return null;
		}
		return null;
	}

	/** 3.22版本的获取返回支付卡 调用方法 */
	private CardForPayOutputDTO returnOutPutDto(ThreadLocal<Long> pocIdCurrent, boolean type0,
			Map<Long, List<Integer>> pocIdsQr, Map<Long, List<Integer>> pocIds, List<BizAccount> accountForQr,
			List<BizAccount> account, CardForPayInputDTO inputDTO) {
		CardForPayOutputDTO outputDTO = new CardForPayOutputDTO();
		outputDTO.setAmount(inputDTO.getAmount());
		outputDTO.setOid(inputDTO.getOid());
		List<CardForPayOutputDTO.BankInfo> resAccountList = new ArrayList<>();

		if (!ObjectUtils.isEmpty(account)) {
			List<Integer> accountIds2Use = account.stream().map(p -> p.getId()).collect(Collectors.toList());
			String seletedId = selectAccountToUse(accountIds2Use, pocIds);

			if (!ObjectUtils.isEmpty(seletedId) && accountIds2Use.contains(Integer.valueOf(seletedId))) {
				BizAccount account2Use = account.stream().filter(p -> p.getId().equals(Integer.valueOf(seletedId)))
						.collect(Collectors.toList()).get(0);
				outputDTO.setPocId(account2Use.getPassageId());
				CardForPayOutputDTO.BankInfo bankInfo = new CardForPayOutputDTO.BankInfo();
				bankInfo.setBankType(account2Use.getBankType());
				bankInfo.setBankName(account2Use.getBankName());
				bankInfo.setOwner(account2Use.getOwner());
				bankInfo.setCardNo(account2Use.getAccount());
				bankInfo.setProvince(account2Use.getProvince());
				bankInfo.setCity(account2Use.getCity());
				resAccountList.add(bankInfo);
			}
			if (type0 || ObjectUtils.isEmpty(accountForQr)) {
				outputDTO.setAccountList(resAccountList);
				return outputDTO;
			}
		}

		if (!ObjectUtils.isEmpty(accountForQr)) {
			List<Integer> accountIds2Use = accountForQr.stream().map(p -> p.getId()).collect(Collectors.toList());
			pocIdsQr.put(pocIdCurrent.get(), accountIds2Use);
			String seletedId = selectAccountToUse(accountIds2Use, pocIdsQr);

			if (!ObjectUtils.isEmpty(seletedId) && accountIds2Use.contains(Integer.valueOf(seletedId))) {
				BizAccount account2Use = accountForQr.stream().filter(p -> p.getId().equals(Integer.valueOf(seletedId)))
						.collect(Collectors.toList()).get(0);
				outputDTO.setPocId(account2Use.getPassageId());
				CardForPayOutputDTO.BankInfo bankInfo = new CardForPayOutputDTO.BankInfo();
				bankInfo.setBankType(account2Use.getBankType());
				bankInfo.setBankName(account2Use.getBankName());
				bankInfo.setOwner(account2Use.getOwner());
				bankInfo.setCardNo(account2Use.getAccount());
				bankInfo.setProvince(account2Use.getProvince());
				bankInfo.setCity(account2Use.getCity());
				bankInfo.setBankMark(BankEnums.findDesc(account2Use.getBankType()).getLog());
				resAccountList.add(bankInfo);
			}
		}
		outputDTO.setAccountList(resAccountList);
		return outputDTO;
	}

	/** 3.27 版本的获取返回支付卡 调用方法 */
	protected CardForPayOutputDTO returnOutPutDtoNew(List<Integer> accountIds, ThreadLocal<Long> pocIdCurrent,
			Map<Long, List<Integer>> pocIdsQr, Map<Long, List<Integer>> pocIds, List<BizAccount> accountForQr,
			List<BizAccount> account, CardForPayInputDTO inputDTO) {
		log.debug("returnOutPutDtoNew>> accountIds {},pocIdsQr {},pocIds {},accountForQr {}, account{},inputDTO {}",
				accountIds, pocIdsQr, pocIds, accountForQr, account, inputDTO);
		CardForPayOutputDTO outputDTO = new CardForPayOutputDTO();
		outputDTO.setOid(inputDTO.getOid());
		List<CardForPayOutputDTO.BankInfo> resAccountList = new ArrayList<>();
		// 扫码+正常入款
		if (!ObjectUtils.isEmpty(account) && !ObjectUtils.isEmpty(accountForQr)) {
			List<String> accountNos = sortAccountToUse(accountIds, pocIds, account);
			List<String> accountNosQR = sortAccountToUse(accountIds, pocIdsQr, accountForQr);
			log.debug("线程{}：按使用时间倒序后结果为：accountNos={},accountNosQR={}", Thread.currentThread().getName(),
					Arrays.toString(accountNos.toArray()), Arrays.toString(accountNosQR.toArray()));
			try {
				InAccountRandUtilResponse res = randUtil.getRandomStr(inputDTO.getAmount().intValue(), accountNos,
						accountNosQR);
				log.debug("线程{}：随机数工具返回的结果：{}", Thread.currentThread().getName(), ObjectMapperUtils.serialize(res));
				if (ObjectUtils.isEmpty(res)) {
					log.debug("扫码+正常入款获取金额小数部分,参数:{},无返回:{}", ObjectMapperUtils.serialize(inputDTO),
							ObjectMapperUtils.serialize(res));
					return null;
				}
				if (ObjectUtils.isEmpty(res.getTransferCardNum())) {
					log.debug("扫码+正常入款获取金额小数部分,参数:{},无返回正常卡号:{}", ObjectMapperUtils.serialize(inputDTO),
							ObjectMapperUtils.serialize(res));
					return null;
				}
				if (ObjectUtils.isEmpty(res.getScanCardNum())) {
					log.debug("扫码+正常入款获取金额小数部分,参数:{},无返回扫码卡号:{}", ObjectMapperUtils.serialize(inputDTO),
							ObjectMapperUtils.serialize(res));
					return null;
				}
				if (ObjectUtils.isEmpty(res.getAmount())) {
					log.debug("扫码+正常入款获取金额小数部分,参数:{},无返回金额小数部分:{}", ObjectMapperUtils.serialize(inputDTO),
							ObjectMapperUtils.serialize(res));
					return null;
				}

				// 同一个金额小数部分
				BigDecimal decimalPart = res.getAmount();
				outputDTO.setAmount(inputDTO.getAmount());
				log.debug("扫码加正常入款最终金额:{}", decimalPart);
				outputDTO.setFinalAmount(decimalPart);
				// 同一个通道
				outputDTO.setPocId(pocIdCurrent.get());

				// 正常卡
				String accountNo = res.getTransferCardNum();
				BizAccount account2Use = account.stream().filter(p -> p.getAccount().equals(accountNo))
						.collect(Collectors.toList()).get(0);
				CardForPayOutputDTO.BankInfo bankInfo = new CardForPayOutputDTO.BankInfo();
				bankInfo.setBankType(account2Use.getBankType());
				bankInfo.setBankName(account2Use.getBankName());
				bankInfo.setOwner(account2Use.getOwner());
				bankInfo.setCardNo(account2Use.getAccount());
				bankInfo.setProvince(account2Use.getProvince());
				bankInfo.setCity(account2Use.getCity());
				resAccountList.add(bankInfo);

				// QR
				String accountNoQr = res.getScanCardNum();
				BizAccount account2UseQR = accountForQr.stream().filter(p -> p.getAccount().equals(accountNoQr))
						.collect(Collectors.toList()).get(0);
				CardForPayOutputDTO.BankInfo bankInfoQR = new CardForPayOutputDTO.BankInfo();
				bankInfoQR.setBankType(account2UseQR.getBankType());
				bankInfoQR.setBankName(account2UseQR.getBankName());
				bankInfoQR.setOwner(account2UseQR.getOwner());
				bankInfoQR.setCardNo(account2UseQR.getAccount());
				bankInfoQR.setProvince(account2UseQR.getProvince());
				bankInfoQR.setCity(account2UseQR.getCity());
				resAccountList.add(bankInfoQR);

				// 分配出去的数据写redis
				writeResultToRedis(res, inputDTO);

				outputDTO.setAccountList(resAccountList);
				return outputDTO;
			} catch (NoAvailableRandomException e) {
				e.printStackTrace();
				log.error("扫码+正常入款获取金额小数部分异常:", e);
				return null;
			}
		}
		// 正常入款
		if (!ObjectUtils.isEmpty(account)) {
			List<String> accountNos = sortAccountToUse(accountIds, pocIds, account);
			try {
				InAccountRandUtilResponse res = randUtil.getRandomStr(inputDTO.getAmount().intValue(), accountNos,
						null);
				if (ObjectUtils.isEmpty(res)) {
					log.debug("获取金额小数部分,参数:{},无返回:{}", ObjectMapperUtils.serialize(inputDTO),
							ObjectMapperUtils.serialize(res));
					return null;
				}
				if (ObjectUtils.isEmpty(res.getTransferCardNum())) {
					log.debug("获取金额小数部分,参数:{},无返回卡号:{}", ObjectMapperUtils.serialize(inputDTO),
							ObjectMapperUtils.serialize(res));
					return null;
				}
				if (ObjectUtils.isEmpty(res.getAmount())) {
					log.debug("获取金额小数部分,参数:{},无返回金额小数部分:{}", ObjectMapperUtils.serialize(inputDTO),
							ObjectMapperUtils.serialize(res));
					return null;
				}
				String accountNo = res.getTransferCardNum();
				BigDecimal decimalPart = res.getAmount();
				BizAccount account2Use = account.stream().filter(p -> p.getAccount().equals(accountNo))
						.collect(Collectors.toList()).get(0);
				outputDTO.setAmount(inputDTO.getAmount());
				log.debug("正常入款最终金额:{}", decimalPart);
				outputDTO.setFinalAmount(decimalPart);
				outputDTO.setPocId(account2Use.getPassageId());
				// 分配出去的数据写redis
				writeResultToRedis(res, inputDTO);

				CardForPayOutputDTO.BankInfo bankInfo = new CardForPayOutputDTO.BankInfo();
				bankInfo.setBankType(account2Use.getBankType());
				bankInfo.setBankName(account2Use.getBankName());
				bankInfo.setOwner(account2Use.getOwner());
				bankInfo.setCardNo(account2Use.getAccount());
				bankInfo.setProvince(account2Use.getProvince());
				bankInfo.setCity(account2Use.getCity());
				resAccountList.add(bankInfo);

			} catch (NoAvailableRandomException e) {
				e.printStackTrace();
				log.error("获取金额小数部分异常:", e);
				return null;
			}
			if (ObjectUtils.isEmpty(accountForQr)) {
				outputDTO.setAccountList(resAccountList);
				return outputDTO;
			}
		}
		// 扫码
		if (!ObjectUtils.isEmpty(accountForQr)) {
			List<String> accountNos = sortAccountToUse(accountIds, pocIdsQr, accountForQr);
			try {
				InAccountRandUtilResponse res = randUtil.getRandomStr(inputDTO.getAmount().intValue(), null,
						accountNos);
				if (ObjectUtils.isEmpty(res)) {
					log.debug("获取扫码金额小数部分,参数:{},无返回:{}", ObjectMapperUtils.serialize(inputDTO),
							ObjectMapperUtils.serialize(res));
					return null;
				}
				if (ObjectUtils.isEmpty(res.getScanCardNum())) {
					log.debug("获取扫码金额小数部分,参数:{},无返回扫码卡号:{}", ObjectMapperUtils.serialize(inputDTO),
							ObjectMapperUtils.serialize(res));
					return null;
				}
				if (ObjectUtils.isEmpty(res.getAmount())) {
					log.debug("获取扫码金额小数部分,参数:{},无返回扫码金额小数部分:{}", ObjectMapperUtils.serialize(inputDTO),
							ObjectMapperUtils.serialize(res));
					return null;
				}

				String accountNo = res.getScanCardNum();
				BigDecimal decimalPartQR = res.getAmount();
				BizAccount account2UseQR = accountForQr.stream().filter(p -> p.getAccount().equals(accountNo))
						.collect(Collectors.toList()).get(0);

				outputDTO.setAmount(inputDTO.getAmount());
				log.debug("扫码入款最终金额:{}", decimalPartQR);
				outputDTO.setFinalAmount(decimalPartQR);
				outputDTO.setPocId(account2UseQR.getPassageId());

				// 分配出去的数据写redis
				writeResultToRedis(res, inputDTO);

				CardForPayOutputDTO.BankInfo bankInfo = new CardForPayOutputDTO.BankInfo();
				bankInfo.setBankType(account2UseQR.getBankType());
				bankInfo.setBankName(account2UseQR.getBankName());
				bankInfo.setOwner(account2UseQR.getOwner());
				bankInfo.setCardNo(account2UseQR.getAccount());
				bankInfo.setProvince(account2UseQR.getProvince());
				bankInfo.setCity(account2UseQR.getCity());
				resAccountList.add(bankInfo);

				outputDTO.setAccountList(resAccountList);
				return outputDTO;
			} catch (NoAvailableRandomException e) {
				e.printStackTrace();
				log.error("获取扫码金额小数部分异常:", e);
				return null;
			}
		}
		return null;
	}

	/** 6.5 版本的获取返回支付卡 调用方法 */
	private CardForPayOutputDTO returnOutPutDto(List<Integer> accountIds, ThreadLocal<Long> pocIdCurrent,
			List<BizAccount> accountForQr, List<BizAccount> account, CardForPayInputDTO inputDTO) {
		log.debug("returnOutPutDtoNew>> accountIds {},pocIdsQr {},pocIds {},accountForQr {}, account{},inputDTO {}",
				accountIds, accountForQr, account, inputDTO);
		CardForPayOutputDTO outputDTO = new CardForPayOutputDTO();
		outputDTO.setOid(inputDTO.getOid());
		List<CardForPayOutputDTO.BankInfo> resAccountList = new ArrayList<>();
		// 扫码+正常入款
		if (!ObjectUtils.isEmpty(account) && !ObjectUtils.isEmpty(accountForQr)) {
			List<String> accountNos = sortAccountToUse(account);
			List<String> accountNosQR = sortAccountToUse(accountForQr);
			log.debug("线程{}：按使用时间倒序后结果为：accountNos={},accountNosQR={}", Thread.currentThread().getName(),
					Arrays.toString(accountNos.toArray()), Arrays.toString(accountNosQR.toArray()));
			try {
				InAccountRandUtilResponse res = randUtil.getRandomStr(inputDTO.getAmount().intValue(), accountNos,
						accountNosQR);
				log.debug("线程{}：随机数工具返回的结果：{}", Thread.currentThread().getName(), ObjectMapperUtils.serialize(res));
				if (ObjectUtils.isEmpty(res)) {
					log.debug("扫码+正常入款获取金额小数部分,参数:{},无返回:{}", ObjectMapperUtils.serialize(inputDTO),
							ObjectMapperUtils.serialize(res));
					return null;
				}
				if (ObjectUtils.isEmpty(res.getTransferCardNum())) {
					log.debug("扫码+正常入款获取金额小数部分,参数:{},无返回正常卡号:{}", ObjectMapperUtils.serialize(inputDTO),
							ObjectMapperUtils.serialize(res));
					return null;
				}
				if (ObjectUtils.isEmpty(res.getScanCardNum())) {
					log.debug("扫码+正常入款获取金额小数部分,参数:{},无返回扫码卡号:{}", ObjectMapperUtils.serialize(inputDTO),
							ObjectMapperUtils.serialize(res));
					return null;
				}
				if (ObjectUtils.isEmpty(res.getAmount())) {
					log.debug("扫码+正常入款获取金额小数部分,参数:{},无返回金额小数部分:{}", ObjectMapperUtils.serialize(inputDTO),
							ObjectMapperUtils.serialize(res));
					return null;
				}

				// 同一个金额小数部分
				BigDecimal decimalPart = res.getAmount();
				outputDTO.setAmount(inputDTO.getAmount());
				log.debug("扫码加正常入款最终金额:{}", decimalPart);
				outputDTO.setFinalAmount(decimalPart);
				// 同一个通道
				outputDTO.setPocId(pocIdCurrent.get());

				// 正常卡
				String accountNo = res.getTransferCardNum();
				BizAccount account2Use = account.stream().filter(p -> p.getAccount().equals(accountNo))
						.collect(Collectors.toList()).get(0);
				CardForPayOutputDTO.BankInfo bankInfo = new CardForPayOutputDTO.BankInfo();
				bankInfo.setBankType(account2Use.getBankType());
				bankInfo.setBankName(account2Use.getBankName());
				bankInfo.setOwner(account2Use.getOwner());
				bankInfo.setCardNo(account2Use.getAccount());
				bankInfo.setProvince(account2Use.getProvince());
				bankInfo.setCity(account2Use.getCity());
				resAccountList.add(bankInfo);

				// QR
				String accountNoQr = res.getScanCardNum();
				BizAccount account2UseQR = accountForQr.stream().filter(p -> p.getAccount().equals(accountNoQr))
						.collect(Collectors.toList()).get(0);
				CardForPayOutputDTO.BankInfo bankInfoQR = new CardForPayOutputDTO.BankInfo();
				bankInfoQR.setBankType(account2UseQR.getBankType());
				bankInfoQR.setBankName(account2UseQR.getBankName());
				bankInfoQR.setOwner(account2UseQR.getOwner());
				bankInfoQR.setCardNo(account2UseQR.getAccount());
				bankInfoQR.setProvince(account2UseQR.getProvince());
				bankInfoQR.setCity(account2UseQR.getCity());
				resAccountList.add(bankInfoQR);

				// 分配出去的数据写redis
				writeResultToRedis(res, inputDTO);

				outputDTO.setAccountList(resAccountList);
				return outputDTO;
			} catch (NoAvailableRandomException e) {
				e.printStackTrace();
				log.error("扫码+正常入款获取金额小数部分异常:", e);
				return null;
			}
		}
		// 正常入款
		if (!ObjectUtils.isEmpty(account)) {
			List<String> accountNos = sortAccountToUse(account);
			try {
				InAccountRandUtilResponse res = randUtil.getRandomStr(inputDTO.getAmount().intValue(), accountNos,
						null);
				if (ObjectUtils.isEmpty(res)) {
					log.debug("获取金额小数部分,参数:{},无返回:{}", ObjectMapperUtils.serialize(inputDTO),
							ObjectMapperUtils.serialize(res));
					return null;
				}
				if (ObjectUtils.isEmpty(res.getTransferCardNum())) {
					log.debug("获取金额小数部分,参数:{},无返回卡号:{}", ObjectMapperUtils.serialize(inputDTO),
							ObjectMapperUtils.serialize(res));
					return null;
				}
				if (ObjectUtils.isEmpty(res.getAmount())) {
					log.debug("获取金额小数部分,参数:{},无返回金额小数部分:{}", ObjectMapperUtils.serialize(inputDTO),
							ObjectMapperUtils.serialize(res));
					return null;
				}
				String accountNo = res.getTransferCardNum();
				BigDecimal decimalPart = res.getAmount();
				BizAccount account2Use = account.stream().filter(p -> p.getAccount().equals(accountNo))
						.collect(Collectors.toList()).get(0);
				outputDTO.setAmount(inputDTO.getAmount());
				log.debug("正常入款最终金额:{}", decimalPart);
				outputDTO.setFinalAmount(decimalPart);
				outputDTO.setPocId(account2Use.getPassageId());
				// 分配出去的数据写redis
				writeResultToRedis(res, inputDTO);

				CardForPayOutputDTO.BankInfo bankInfo = new CardForPayOutputDTO.BankInfo();
				bankInfo.setBankType(account2Use.getBankType());
				bankInfo.setBankName(account2Use.getBankName());
				bankInfo.setOwner(account2Use.getOwner());
				bankInfo.setCardNo(account2Use.getAccount());
				bankInfo.setProvince(account2Use.getProvince());
				bankInfo.setCity(account2Use.getCity());
				resAccountList.add(bankInfo);

			} catch (NoAvailableRandomException e) {
				e.printStackTrace();
				log.error("获取金额小数部分异常:", e);
				return null;
			}
			if (ObjectUtils.isEmpty(accountForQr)) {
				outputDTO.setAccountList(resAccountList);
				return outputDTO;
			}
		}
		// 扫码
		if (!ObjectUtils.isEmpty(accountForQr)) {
			List<String> accountNos = sortAccountToUse(accountForQr);
			try {
				InAccountRandUtilResponse res = randUtil.getRandomStr(inputDTO.getAmount().intValue(), null,
						accountNos);
				if (ObjectUtils.isEmpty(res)) {
					log.debug("获取扫码金额小数部分,参数:{},无返回:{}", ObjectMapperUtils.serialize(inputDTO),
							ObjectMapperUtils.serialize(res));
					return null;
				}
				if (ObjectUtils.isEmpty(res.getScanCardNum())) {
					log.debug("获取扫码金额小数部分,参数:{},无返回扫码卡号:{}", ObjectMapperUtils.serialize(inputDTO),
							ObjectMapperUtils.serialize(res));
					return null;
				}
				if (ObjectUtils.isEmpty(res.getAmount())) {
					log.debug("获取扫码金额小数部分,参数:{},无返回扫码金额小数部分:{}", ObjectMapperUtils.serialize(inputDTO),
							ObjectMapperUtils.serialize(res));
					return null;
				}

				String accountNo = res.getScanCardNum();
				BigDecimal decimalPartQR = res.getAmount();
				BizAccount account2UseQR = accountForQr.stream().filter(p -> p.getAccount().equals(accountNo))
						.collect(Collectors.toList()).get(0);

				outputDTO.setAmount(inputDTO.getAmount());
				log.debug("扫码入款最终金额:{}", decimalPartQR);
				outputDTO.setFinalAmount(decimalPartQR);
				outputDTO.setPocId(account2UseQR.getPassageId());

				// 分配出去的数据写redis
				writeResultToRedis(res, inputDTO);

				CardForPayOutputDTO.BankInfo bankInfo = new CardForPayOutputDTO.BankInfo();
				bankInfo.setBankType(account2UseQR.getBankType());
				bankInfo.setBankName(account2UseQR.getBankName());
				bankInfo.setOwner(account2UseQR.getOwner());
				bankInfo.setCardNo(account2UseQR.getAccount());
				bankInfo.setProvince(account2UseQR.getProvince());
				bankInfo.setCity(account2UseQR.getCity());
				resAccountList.add(bankInfo);

				outputDTO.setAccountList(resAccountList);
				return outputDTO;
			} catch (NoAvailableRandomException e) {
				e.printStackTrace();
				log.error("获取扫码金额小数部分异常:", e);
				return null;
			}
		}
		return null;
	}

	/** 6.24 版本的获取返回支付卡 调用方法 */
	private CardForPayOutputDTO returnOutPutDtoForLevelCode(List<AccountBaseInfo> idsForUse,
			CardForPayInputDTO inputDTO, Integer handicapId) {
		log.debug("returnOutPutDtoForLevelCode>> idsForUse {},inputDTO {}", idsForUse, inputDTO);
		CardForPayOutputDTO outputDTO = new CardForPayOutputDTO();
		outputDTO.setOid(inputDTO.getOid());
		List<CardForPayOutputDTO.BankInfo> resAccountList = new ArrayList<>();
		if (!ObjectUtils.isEmpty(idsForUse)) {
			String bankType = Objects.isNull(inputDTO.getBankTypeId()) ? null
					: BankEnums.findCode(inputDTO.getBankTypeId().intValue()).getDesc();
			List<String> accountNos = sortAccountForLevelCode(idsForUse, bankType);
			log.debug("returnOutPutDtoForLevelCode>>排序后的结果，参数{} 排序结果：{}", ObjectMapperUtils.serialize(inputDTO),
					ObjectMapperUtils.serialize(accountNos));
			try {
				InAccountRandUtilResponse res = randUtil.getRandomStr(inputDTO.getAmount().intValue(), accountNos,
						null);
				String accountNo = res.getTransferCardNum();
				BigDecimal decimalPart = res.getAmount();
				AccountBaseInfo account2Use = accountService.getFromCacheByHandicapIdAndAccount(handicapId, accountNo);
				outputDTO.setAmount(inputDTO.getAmount());
				log.debug("正常入款最终金额:{}", decimalPart);
				outputDTO.setFinalAmount(decimalPart);
				outputDTO.setPocId(account2Use.getPassageId());
				// 分配出去的数据写redis
				writeResultToRedis(res, inputDTO);

				CardForPayOutputDTO.BankInfo bankInfo = new CardForPayOutputDTO.BankInfo();
				bankInfo.setBankType(account2Use.getBankType());
				bankInfo.setBankName(account2Use.getBankName());
				bankInfo.setOwner(account2Use.getOwner());
				bankInfo.setCardNo(account2Use.getAccount());
				bankInfo.setProvince(account2Use.getProvince());
				bankInfo.setCity(account2Use.getCity());
				resAccountList.add(bankInfo);
				outputDTO.setAccountList(resAccountList);
				return outputDTO;
			} catch (NoAvailableRandomException e) {
				e.printStackTrace();
				log.error("获取金额小数部分异常:", e);
				return null;
			}
		}
		return null;
	}

	private List<String> sortAccountToUse(List<BizAccount> account) {
		Assert.notNull(account, "account must not be null");
		Map exceed = redisService.getStringRedisTemplate().boundHashOps(RedisKeys.INACCOUNT_EXCEED_CREDIT_COUNT)
				.entries();
		Map lastTime = redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACCOUNT_INCOME_LASTTIME).entries();
		List<String> accountNo2Use = account.stream().sorted((o1, o2) -> {
			if (Objects.equals(o1.getFlag(), o2.getFlag())) {
				Object l1 = lastTime.get(o1.getId().toString()), l2 = lastTime.get(o2.getId().toString());
				Long t1 = Objects.isNull(l1) ? 0L : Long.parseLong(l1.toString());
				Long t2 = Objects.isNull(l2) ? 0L : Long.parseLong(l2.toString());
				if (Objects.equals(o1.getFlag(), AccountFlag.PC.getTypeId())) {
					return t1.compareTo(t2);
				} else {
					int c1 = exceed.containsKey(o1.getId().toString()) ? 1 : 0;
					int c2 = exceed.containsKey(o2.getId().toString()) ? 1 : 0;
					if (c1 == c2) {
						return t2.compareTo(t1);
					} else {
						return c1 - c2;
					}
				}
			} else {
				return o2.getFlag() - o1.getFlag();
			}
		}).map(p -> p.getAccount()).collect(Collectors.toList());
		return accountNo2Use;
	}

	/**
	 * 入款卡排序<br>
	 * 1、bankType 不为空时，传入的银行类型优先<br>
	 * 2、先入后出卡优先，subType不为InBankSubType.IN_BANK_YSF<br>
	 * 3、边入边出卡其次，subType为InBankSubType.IN_BANK_YSF<br>
	 * 4、PC卡最后，flag为0<br>
	 *
	 * @param account
	 * @param bankType
	 * @return
	 */
	private List<String> sortAccountForLevelCode(List<AccountBaseInfo> account, String bankType) {
		Assert.notNull(account, "account must not be null");
		Map exceed = redisService.getStringRedisTemplate().boundHashOps(RedisKeys.INACCOUNT_EXCEED_CREDIT_LEVEL)
				.entries();
		Map lastTime = redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACCOUNT_INCOME_LASTTIME).entries();
		
		//需求 7876 要求云闪付卡 优先分配给当日累计流水最少的卡
		//获取云闪付入款卡每日入款金额
		List<Object> ysfAccountList = account.stream().filter(t->Objects.equals(t.getSubType(), InBankSubType.IN_BANK_YSF.getSubType())).map(AccountBaseInfo::getId).collect(Collectors.toList());
		Map<Integer, BigDecimal> ysfDayInMap = accountService.findAmountDaily(0,ysfAccountList);
		
		List<String> accountNo2Use = account.stream().sorted((o1, o2) -> {
			int b1 = bankType != null && Objects.equals(o1.getBankType(), bankType) ? 0 : 1;
			int b2 = bankType != null && Objects.equals(o2.getBankType(), bankType) ? 0 : 1;
			if (b1 == b2) {
				if (Objects.equals(o1.getFlag(), o2.getFlag())) {
					Object l1 = lastTime.get(o1.getId().toString()), l2 = lastTime.get(o2.getId().toString());
					Long t1 = Objects.isNull(l1) ? 0L : Long.parseLong(l1.toString());
					Long t2 = Objects.isNull(l2) ? 0L : Long.parseLong(l2.toString());
					if (Objects.equals(o1.getFlag(), AccountFlag.PC.getTypeId())) {
						return t1.compareTo(t2);
					} else {
						int s1 = Objects.equals(o1.getSubType(), InBankSubType.IN_BANK_YSF.getSubType()) ? 1 : 0;
						int s2 = Objects.equals(o2.getSubType(), InBankSubType.IN_BANK_YSF.getSubType()) ? 1 : 0;
						if (s1 == s2) {
							if (s1 == 1) {
								//需求 7876 要求云闪付卡 优先分配给当日累计流水最少的卡
								//return ySFLocalCacheUtil.getAccountLastUseTime(o1.getAccount())
								//		.compareTo(ySFLocalCacheUtil.getAccountLastUseTime(o2.getAccount()));
								return ysfDayInMap.get(o1.getId()).compareTo(ysfDayInMap.get(o2.getId()));
							} else {
								int c1 = exceed.containsKey(o1.getId().toString()) ? 1 : 0;
								int c2 = exceed.containsKey(o2.getId().toString()) ? 1 : 0;
								if (c1 == c2) {
									return t2.compareTo(t1);
								} else {
									return c1 - c2;
								}
							}
						} else {
							return s1 - s2;
						}
					}
				} else {
					return o2.getFlag() - o1.getFlag();
				}
			} else {
				return b1 - b2;
			}
		}).map(p -> p.getAccount()).collect(Collectors.toList());
		return accountNo2Use;
	}

	protected List<String> sortAccountToUse(List<Integer> accountIds, Map<Long, List<Integer>> pocIds,
			List<BizAccount> account) {
		Assert.notNull(accountIds, "accountIds must not be null");
		Assert.notNull(pocIds, "pocIds must not be null");
		Assert.notNull(account, "account must not be null");
		List<Integer> extractAccountIds = new ArrayList<>();
		account.forEach(t -> extractAccountIds.add(t.getId()));
		// key: 使用的时间值 val: map-> accountId:pocId
		TreeMap<Long, Map<String, Long>> usedCountAccountIds = new TreeMap<>();
		for (Map.Entry<Long, List<Integer>> entry : pocIds.entrySet()) {
			Long pocId = entry.getKey();
			List<Integer> accountIds2 = entry.getValue();
			for (int i = 0, size = accountIds2.size(); i < size; i++) {
				Integer accountId = accountIds2.get(i);
				if (!extractAccountIds.contains(accountId)) {
					continue;
				}
				Object model = redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_NEW_MODEL)
						.get(accountId + "");
				if (model != null && Objects.equals(model, Constants.YSF_MODEL_OUT + "")) {
					continue;
				}
				if (accountIds.contains(accountIds2.get(i))) {
					String key = RedisKeys.NEWINACCOUNT_PASSAGEID_ACCOUNTIDS_USED_KEY + pocId;
					String member = String.valueOf(accountId);
					// 判断是否使用过卡
					boolean exist = redisService.getStringRedisTemplate().boundHashOps(key).hasKey(member);
					AccountBaseInfo base = accountService.getFromCacheById(accountId);
					if (base == null)
						continue;
					if (!exist) {
						// 如果没有使用过的卡 则给使用分值 设置1-100的随机数 防止多个新卡
						Map<String, Long> map = new HashMap<>(1);
						map.put(member, pocId);
						// 5419 大额自购卡，将当前时间 + 随机数 设置为上次出款时间，使大额自购卡排到最后
						if (!base.checkMobile()) {
							usedCountAccountIds
									.put(new Double(Math.random() * 100).longValue() + System.currentTimeMillis(), map);
							log.debug("sortAccountToUse>>未使用的卡，大额卡放到结果集中,result {}", usedCountAccountIds);
						} else {
							usedCountAccountIds.put(new Double(Math.random() * 100).longValue(), map);
							log.debug("sortAccountToUse>>未使用的卡，非大额卡放到结果集中,result {}", usedCountAccountIds);
						}
					} else {
						Object val = redisService.getStringRedisTemplate().boundHashOps(key).get(member);
						Map<String, Long> map = new HashMap<>(1);
						map.put(member, pocId);
						// 5419 大额自购卡，将当前时间 + 当前时间 - 上次收款时间 设置为上次出款时间，使大额自购卡排到最后
						if (!base.checkMobile()) {
							usedCountAccountIds.put(System.currentTimeMillis() + Long.valueOf(val.toString()), map);
							log.debug("sortAccountToUse>>使用过，大额卡放到结果集中,result {}", usedCountAccountIds);
						} else {
							usedCountAccountIds.put(Long.valueOf(val.toString()), map);
							log.debug("sortAccountToUse>>使用过，非大额卡放到结果集中,result {}", usedCountAccountIds);
						}
					}
				}
			}

		}
		if (ObjectUtils.isEmpty(usedCountAccountIds)) {
			return null;
		}
		List<String> accountNo2Use = new LinkedList();
		Map<String, Long> accountIdPocIdMap2Use;
		for (Map.Entry<Long, Map<String, Long>> entry : usedCountAccountIds.entrySet()) {
			System.out.println(entry.getKey() + ": " + entry.getValue());
			accountIdPocIdMap2Use = entry.getValue();
			// accountId
			String key = accountIdPocIdMap2Use.keySet().iterator().next();
			AccountBaseInfo baseInfo = accountService.getFromCacheById(Integer.valueOf(key));
			if (!ObjectUtils.isEmpty(baseInfo) && !ObjectUtils.isEmpty(baseInfo.getAccount())) {
				accountNo2Use.add(baseInfo.getAccount());
			}
		}
		return accountNo2Use;
	}

	protected List<BizAccount> getBizAccounts(List<String> requestCardNos, List<BizAccount> sourceAccountList,
			List<BizAccount> targetAccountList, List<java.util.function.Predicate<BizAccount>> predicates) {
		if (!ObjectUtils.isEmpty(requestCardNos)) {
			List<java.util.function.Predicate<BizAccount>> predicates2 = new ArrayList<>();
			predicates2.addAll(predicates);
			predicates2.add(p -> requestCardNos.contains(p.getAccount()));
			targetAccountList = filterAccount(sourceAccountList, predicates2);
		}
		return targetAccountList;
	}

	protected List<java.util.function.Predicate<BizAccount>> predicatesCommon2(CardForPayInputDTO inputDTO) {
		List<java.util.function.Predicate<BizAccount>> predicates = new ArrayList<>();
		// 是否正在出款 true 正在出款 false 不在出款
		// predicates.add(p -> !transService.hasTrans(p.getId()));
		// 当前可用额度是否大于收款金额
		predicates.add(p -> accountChangeService.currCredits(accountService.getFromCacheById(p.getId()))
				- inputDTO.getAmount().intValue() > 0);
		// 账号当前是否在线
		predicates.add(p -> transService.isOnline(p.getId()));
		return predicates;
	}

	protected List<java.util.function.Predicate<BizAccount>> predicatesCommon1(CardForPayInputDTO inputDTO,
			boolean filterMinInAmount) {
		List<java.util.function.Predicate<BizAccount>> predicates = new ArrayList<>();
		// 已绑定通道
		predicates.add(p -> !ObjectUtils.isEmpty(p.getPassageId()));
		// 类型不为空
		predicates.add(p -> !ObjectUtils.isEmpty(p.getType()));
		// 单笔最小入款金额不为空
		// predicates.add(p -> !ObjectUtils.isEmpty(p.getMinInAmount()));
		// 有子类型
		predicates.add(p -> !ObjectUtils.isEmpty(p.getSubType()));
		// 状态
		predicates.add(p -> !ObjectUtils.isEmpty(p.getStatus()));
		predicates.add(p -> p.getStatus().equals(AccountStatus.Normal.getStatus()));
		// 属于入款卡
		predicates.add(p -> p.getType().intValue() == AccountType.InBank.getTypeId().intValue());
		// 账号子类型一致
		predicates.add(p -> p.getSubType() == inputDTO.getType().intValue()
				|| (inputDTO.getType().intValue() != InBankSubType.IN_BANK_YSF.getSubType()
						&& p.getFlag() == AccountFlag.PC.getTypeId()));
		// 账号是否在线
		predicates.add(p -> transService.isOnline(p.getId()));
		// 账号额度是否足够
		predicates.add(p -> accountChangeService.currCredits(accountService.getFromCacheById(p.getId()))
				- inputDTO.getAmount().intValue() > 0);
		if (filterMinInAmount) {
			// 单笔最小入款金额小于收款金额
			predicates.add(
					p -> ObjectUtils.isEmpty(p.getMinInAmount()) || p.getMinInAmount().compareTo(new BigDecimal(0)) == 0
							|| p.getMinInAmount().compareTo(new BigDecimal(inputDTO.getAmount().doubleValue())
									.setScale(2, BigDecimal.ROUND_HALF_UP)) <= 0);
		}
		return predicates;
	}

	/**
	 * 使用过滤规则 过滤list
	 * 
	 * @param bizAccount
	 * @param predicates
	 * @param <I>
	 * @return
	 */
	protected <I> List<I> filterAccount(Collection<I> bizAccount, List<java.util.function.Predicate<I>> predicates) {
		if (ObjectUtils.isEmpty(bizAccount)) {
			return Lists.newArrayList();
		}
		if (ObjectUtils.isEmpty(predicates)) {
			return (List<I>) bizAccount;
		}
		List<I> list = bizAccount.stream()
				.filter(ele -> predicates.stream().reduce(t -> true, java.util.function.Predicate::and).test(ele))
				.collect(Collectors.toList());
		return list;
	}

	/**
	 * 1.5.9 通道可用银行卡告警
	 * 
	 * @param inputDTO
	 * @return
	 */
	@Override
	public ResponseDataNewPay cardAvailAlarm(CardAvailAlarmInputDTO inputDTO) {
		if (Objects.isNull(inputDTO)) {
			return null;
		}
		ThreadLocal<ResponseDataNewPay> threadLocal = new ThreadLocal<>();
		PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
				.getPlatformNewPayServiceApi(false);
		if (platformNewPayService == null) {
			return null;
		}
		String json = "{\"oid\":" + inputDTO.getOid() + ",\"cardWarnFlag\":" + inputDTO.getCardWarnFlag()
				+ ",\"pocId\":" + inputDTO.getPocId() + "}";
		RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), json);
		Observable<ResponseDataNewPay> res = platformNewPayService.bankWarn(requestBody);
		res.subscribe(ret -> {
			log.info("银行卡可用数量告警结果 :code:{},msg:{}", ret.getCode(), ret.getMsg());
			threadLocal.set(ret);
		}, e -> log.error("银行卡可用数量告警结果 error : ", e));
		return threadLocal.get();
	}

	/**
	 * 1.5.10 修改银行卡卡号、删除银行卡、停用、冻结银行卡时调用平台接口
	 * 
	 * @param inputDTO
	 * @return
	 */
	@Override
	public ResponseDataNewPay bankModified(BankModifiedInputDTO inputDTO) {
		if (Objects.isNull(inputDTO)) {
			return null;
		}
		if (ObjectUtils.isEmpty(inputDTO.getPocId()) || ObjectUtils.isEmpty(inputDTO.getOid())
				|| ObjectUtils.isEmpty(inputDTO.getCardNo()) || ObjectUtils.isEmpty(inputDTO.getStatus())) {
			return new ResponseDataNewPay((byte) -1, "参数缺失!");
		}
		if (inputDTO.getStatus() == 2) {
			if (ObjectUtils.isEmpty(inputDTO.getCardNo2())) {
				return new ResponseDataNewPay((byte) -1, "新账号必传");
			}
		}
		ThreadLocal<ResponseDataNewPay> threadLocal = new ThreadLocal<>();
		PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
				.getPlatformNewPayServiceApi(false);
		if (platformNewPayService == null) {
			return null;
		}
		String json = "{\"oid\":" + inputDTO.getOid() + ",\"cardNo\":\"" + inputDTO.getCardNo() + "\",\"status\":"
				+ inputDTO.getStatus();
		if (!ObjectUtils.isEmpty(inputDTO.getCardNo2())) {
			json += ",\"cardNo2\":\"" + inputDTO.getCardNo2() + "\"";
		}
		json += ",\"pocId\":" + inputDTO.getPocId() + "}";
		RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), json);
		Observable<ResponseDataNewPay> res = platformNewPayService.bankModified(requestBody);
		res.subscribe(ret -> {
			log.info("账号状态修改通知结果 :code:{},msg:{}", ret.getCode(), ret.getMsg());
			threadLocal.set(ret);
		}, e -> log.error("账号状态修改通知 error : ", e));
		return threadLocal.get();
	}

	/**
	 * 1.5.11 统计通道可用卡数和不可用卡数
	 * 
	 * @param map
	 *            oid:[pocId]
	 * @return
	 */
	@Override
	public List<CardsStatisticOutputDTO> userOrNon(Map<Integer, List<Long>> map) {
		try {
			if (ObjectUtils.isEmpty(map)) {
				return null;
			}
			StringBuilder sqls = new StringBuilder();
			String sql = null;
			// 记录没有参数有的通道但是返回结果没有的通道
			Map<String, Integer[]> allHandicapPocIdMap = new HashMap<>();
			Integer[] mntarr = { 0, 0, 0, 0, };
			for (Map.Entry<Integer, List<Long>> entry : map.entrySet()) {
				if (ObjectUtils.isEmpty(entry.getKey()) || ObjectUtils.isEmpty(entry.getValue())) {
					continue;
				}
				BizHandicap handicap = handicapService.findFromCacheByCode(entry.getKey().toString());
				if (ObjectUtils.isEmpty(handicap)) {
					continue;
				}
				if (ObjectUtils.isEmpty(sql)) {
					sql = " select handicap_id ,passage_id, status ,id,CASE when in_card_type is null then 0 else in_card_type end from  fundsTransfer.biz_account where type=1  and sub_type in (0,1,2,3) ";
				} else {
					sql = " union all  select handicap_id ,passage_id, status ,id,CASE when in_card_type is null then 0 else in_card_type end from  fundsTransfer.biz_account where type=1  and sub_type in (0,1,2,3) ";
				}
				sql += " and handicap_id=" + handicap.getId() + " and passage_id ";
				if (entry.getValue().size() == 1) {
					Long pocId = entry.getValue().get(0);
					sql += "=" + pocId;
					StringBuilder key = new StringBuilder().append(handicap.getId()).append("|").append(pocId);
					allHandicapPocIdMap.put(key.toString(), mntarr);
				} else {
					sql += " in(";
					for (int i = 0, size = entry.getValue().size(); i < size; i++) {
						Long pocId = entry.getValue().get(i);
						StringBuilder key = new StringBuilder().append(handicap.getId()).append("|").append(pocId);
						allHandicapPocIdMap.put(key.toString(), mntarr);
						if (i < size - 1) {
							sql += pocId + ",";
						} else {
							sql += pocId + ")";
						}
					}
				}
				sqls.append(sql);
			}
			@SuppressWarnings("unchecked")
			List<Object[]> list = entityManager.createNativeQuery(sqls.toString()).getResultList();
			log.debug("组装未查询数据时候的map:{}", ObjectMapperUtils.serialize(allHandicapPocIdMap));
			log.debug("查询到的数据:{}", ObjectMapperUtils.serialize(list));
			if (!ObjectUtils.isEmpty(list)) {
				List<CardsStatisticOutputDTO> res = new ArrayList<>();
				/** key:handicap_id |passage_id, val: 可用 | 不可用 */
				Map<String, Integer[]> map2 = new HashMap<>();
				list.stream().forEach(p -> {
					/** 1可用卡数 (转账卡) 2不可用卡数 (转账卡) 3可用卡数 (扫码卡) 4不可用卡数 (扫码卡) */
					String status;
					Integer id = Integer.valueOf(p[3].toString());
					// || transService.hasTrans(id) || !transService.isOnline(id)
					// 需求 5726
					if (!ObjectUtils.nullSafeEquals(Integer.valueOf(p[2].toString()), AccountStatus.Normal.getStatus())
							|| accountChangeService.currCredits(accountService.getFromCacheById(id)) <= 0) {
						// 不可用卡（有转账任务||信用额度不大于0||不在线）
						if (!ObjectUtils.isEmpty(p[4])
								&& ObjectUtils.nullSafeEquals(Integer.valueOf(p[4].toString()), 1)) {
							// 扫码卡
							status = "4";
						} else {
							// 转账卡
							status = "2";
						}
					} else {
						// 可用卡
						if (!ObjectUtils.isEmpty(p[4])
								&& ObjectUtils.nullSafeEquals(Integer.valueOf(p[4].toString()), 1)) {
							// 扫码卡
							status = "3";
						} else {
							// 转账卡
							status = "1";
						}
					}
					/** key标识 handicapid pocId */
					String key = p[0] + "|" + p[1];
					/** value标识 1可用卡数 (转账卡) 2不可用卡数 (转账卡) 3可用卡数 (扫码卡) 4不可用卡数 (扫码卡) */
					Integer mnt1 = 0, mnt2 = 0, mnt3 = 0, mnt4 = 0;
					if (!ObjectUtils.isEmpty(map2) && map2.containsKey(key)) {
						// 返回集合有值 且 key已存在集合（此盘口与通道已存在）
						Integer valsArr[] = map2.get(key);
						mnt1 = valsArr[0];
						mnt2 = valsArr[1];
						mnt3 = valsArr[2];
						mnt4 = valsArr[3];
					}
					if (status.equals("1")) {
						mnt1++;
					} else if (status.equals("2")) {
						mnt2++;
					} else if (status.equals("3")) {
						mnt3++;
					} else if (status.equals("4")) {
						mnt4++;
					}
					Integer[] value = { mnt1, mnt2, mnt3, mnt4 };
					map2.put(key, value);
				});
				if (!ObjectUtils.isEmpty(map2)) {
					log.debug("组装查询到的数据:{}", ObjectMapperUtils.serialize(map2));
					for (Map.Entry<String, Integer[]> entry : map2.entrySet()) {
						CardsStatisticOutputDTO outputDTO = new CardsStatisticOutputDTO();
						/** key标识 handicapid| pocId ,val: 可用 数量| 不可用 数量 */
						String key = entry.getKey();
						String[] keyArray = StringUtils.split(key, "|");
						BizHandicap handicap = handicapService.findFromCacheById(Integer.valueOf(keyArray[0]));
						outputDTO.setOid(Integer.valueOf(handicap.getCode()));
						outputDTO.setPocId(Long.valueOf(keyArray[1]));
						Integer[] vals = entry.getValue();
						outputDTO.setMnt1(vals[0]);
						outputDTO.setMnt2(vals[1]);
						outputDTO.setMnt3(vals[2]);
						outputDTO.setMnt4(vals[3]);
						res.add(outputDTO);
						if (!CollectionUtils.isEmpty(allHandicapPocIdMap)
								&& allHandicapPocIdMap.keySet().contains(key)) {
							allHandicapPocIdMap.remove(key);
						}
					}
					if (!CollectionUtils.isEmpty(allHandicapPocIdMap)) {
						log.debug("组装未查询到的数据:{}", ObjectMapperUtils.serialize(allHandicapPocIdMap));
						for (Map.Entry<String, Integer[]> entry : allHandicapPocIdMap.entrySet()) {
							String[] key = StringUtils.split(entry.getKey(), "|");
							BizHandicap handicap = handicapService.findFromCacheById(Integer.valueOf(key[0]));
							CardsStatisticOutputDTO outputDTO = new CardsStatisticOutputDTO();
							outputDTO.setOid(Integer.valueOf(handicap.getCode()));
							outputDTO.setPocId(Long.valueOf(key[1]));
							outputDTO.setMnt1(0);
							outputDTO.setMnt2(0);
							outputDTO.setMnt3(0);
							outputDTO.setMnt4(0);
							res.add(outputDTO);
						}
					}
					return res;
				}
			}
			return null;
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/** 2.0.1 查询通道告警信息(需求 4807) */
	@Override
	public PageDTO<FindPageOutputDTO> findAlarmPage(FindAlarmInputDTO inputDTO, BizHandicap handicap) {
		try {
			List<BizAccount> bizAccountPage = accountRepository.findAll((root, criteriaQuery, criteriaBuilder) -> {
				List<Predicate> predicates = new ArrayList<>();
				Predicate p = criteriaBuilder.equal(root.get("handicapId").as(Integer.class), handicap.getId());
				predicates.add(p);
				Predicate p2 = criteriaBuilder.equal(root.get("type").as(Integer.class),
						AccountType.InBank.getTypeId());
				predicates.add(p2);
				Predicate p4 = criteriaBuilder.equal(root.get("status").as(Integer.class),
						AccountStatus.Normal.getStatus());
				predicates.add(p4);
				if (inputDTO.getPocIdList().size() == 1) {
					Predicate p3 = criteriaBuilder.equal(root.get("passageId").as(Long.class),
							inputDTO.getPocIdList().get(0));
					predicates.add(p3);
				} else {
					CriteriaBuilder.In in = criteriaBuilder.in(root.get("passageId").as(Long.class));
					for (int i = 0, len = inputDTO.getPocIdList().size(); i < len; i++) {
						in.value(inputDTO.getPocIdList().get(i));
					}
					predicates.add(in);
				}

				Predicate[] ps = new Predicate[predicates.size()];
				criteriaQuery.where(criteriaBuilder.and(predicates.toArray(ps)));
				return criteriaQuery.getRestriction();
			});

			// 返回记录
			Set<FindPageOutputDTO> res = new HashSet<>();
			// 查询到的记录
			List<BizAccount> accountList;
			Set<BizAccount> accountSet;
			if (!ObjectUtils.isEmpty(bizAccountPage)) {
				// 过滤查询记录
				accountList = bizAccountPage;
				List<java.util.function.Predicate<BizAccount>> predicates = new ArrayList<>();
				// predicates.add(p -> !transService.hasTrans(p.getId()));
				predicates.add(p -> accountChangeService.currCredits(accountService.getFromCacheById(p.getId())) > 0);
				predicates.add(p -> transService.isOnline(p.getId()));
				accountList = accountList.stream()
						.filter(p -> predicates.stream().reduce(t -> true, java.util.function.Predicate::and).test(p))
						.collect(Collectors.toList());
				if (!ObjectUtils.isEmpty(accountList)) {
					// 根据通道id 分组
					Map<Long, List<BizAccount>> map = accountList.parallelStream()
							.collect(Collectors.groupingBy(BizAccount::getPassageId));
					// 清除集合
					// accountList.clear();
					// accountSet = new HashSet<>();

					for (Map.Entry<Long, List<BizAccount>> entry : map.entrySet()) {
						// if (entry.getValue().size() <= 5) {
						// accountSet.addAll(entry.getValue());
						// }
						FindPageOutputDTO outputDTO = new FindPageOutputDTO();
						outputDTO.setOid(inputDTO.getOid());
						outputDTO.setPocId(entry.getKey());
						// 扫码卡
						int count1 = (int) entry.getValue().stream()
								.filter(p -> !ObjectUtils.isEmpty(p.getInCardType()) && p.getInCardType().equals(1))
								.count();
						// 转账卡
						int count = entry.getValue().size() - count1;
						outputDTO.setCount(count);
						outputDTO.setCount1(count1);
						res.add(outputDTO);
					}
					List<Long> queriedPocIds = new ArrayList<>(map.keySet());
					log.debug("查出数据的通道:{}", ObjectMapperUtils.serialize(queriedPocIds));
					inputDTO.getPocIdList().removeAll(queriedPocIds);
					log.debug("没有查出数据的通道:{}", ObjectMapperUtils.serialize(inputDTO.getPocIdList()));
					if (!ObjectUtils.isEmpty(inputDTO.getPocIdList())) {
						inputDTO.getPocIdList().stream().forEach(p -> {
							FindPageOutputDTO outputDTO = new FindPageOutputDTO();
							outputDTO.setOid(inputDTO.getOid());
							outputDTO.setPocId(p);
							outputDTO.setCount(0);
							outputDTO.setCount1(0);
							res.add(outputDTO);
						});
					}
					// if (!ObjectUtils.isEmpty(accountSet)) {
					// accountSet.stream().forEach(p -> {
					// FindPageOutputDTO outputDTO = new FindPageOutputDTO();
					// outputDTO.setPocId(p.getPassageId());
					// outputDTO.setOid(Integer.valueOf(handicap.getCode()));
					// res.add(outputDTO);
					// });
					// }
				}
			}
			log.debug("告警查询结果:{}", ObjectMapperUtils.serialize(res));
			// 封装返回
			PagingWrapForList list = new PagingWrapForList(Lists.newArrayList(res), inputDTO.getPageSize());
			log.debug("告警查询封装结果:{}", ObjectMapperUtils.serialize(list.getPageData()));
			PageDTO<FindPageOutputDTO> pageDTO = new PageDTO();
			pageDTO.setResultList((List<FindPageOutputDTO>) list.getPageData());
			pageDTO.setPageNo(inputDTO.getPageNo());
			pageDTO.setPageSize(inputDTO.getPageSize());
			pageDTO.setTotalPageNumber(list.getTotalPage());
			pageDTO.setTotalRecordNumber(list.getTotalRecord());
			return pageDTO;
		} catch (Exception e) {
			log.error("查询通道告警信息失败:", e);
			return null;
		}
	}

	/**
	 * 根据通道id查询账号信息
	 *
	 * @param passageId
	 * @return
	 */
	@Override
	public List<BizAccount> findBySubTypeAndPassageId(Long passageId) {
		return accountRepository.findByTypeAndPassageId(AccountType.InBank.getTypeId(), passageId);
	}

	/**
	 * 根据账号和子类型查询账号信息
	 * 
	 * @param accounts
	 * @return
	 */
	@Override
	public List<BizAccount> findAccountsByIdsForUpdate(List<Integer> accounts) {
		if (ObjectUtils.isEmpty(accounts)) {
			return null;
		}
		return accountRepository.findByAccountIdsForUpdate(accounts);
	}

	@Override
	public List<BizAccount> findByAccounts(List<String> accounts) {
		// String sql = "select * from biz_account where status=1 and type=1 ";
		// if (!ObjectUtils.isEmpty(subType)) {
		// sql += " and sub_type =" + subType;
		// }
		// if (!ObjectUtils.isEmpty(accounts)) {
		// if (accounts.size() == 1) {
		// sql += " and account= '" + accounts.get(0) + "'";
		// } else {
		// sql += " and account in (";
		// for (int i = 0, size = accounts.size(); i < size; i++) {
		// if (i < size - 1)
		// sql += accounts.get(i) + ",";
		// else
		// sql += accounts.get(i) + ")";
		// }
		// }
		// }
		// List<BizAccount> list =
		// entityManager.createNativeQuery(sql).getResultList();
		// return list;
		return accountRepository.findByAccounts(accounts);
	}

	@Override
	public List<BizAccount> findByAccountsForPay(CardForPayInputDTO inputDTO, Integer handicapId) {
		List<BizAccount> accountList = new ArrayList<>();
		for (Map.Entry<String, String> str : inputDTO.getPocIdMapCardNosCol().entrySet()) {
			List<String> accounts = Stream.of(str.getValue().split(",")).collect(Collectors.toList());
			accountList.addAll(
					accountRepository.findByStatusAndHandicapIdAndSubTypeAndAccountInAndMinInAmountGreaterThanEqual(
							AccountStatus.Normal.getStatus(), handicapId, inputDTO.getType().intValue(), accounts,
							new BigDecimal(inputDTO.getAmount().doubleValue()).setScale(2,
									BigDecimal.ROUND_HALF_DOWN)));
		}

		return accountList;
	}

	@Override
	public List<BizAccount> saveBindBatch(List<BizAccount> accounts) {
		return accountRepository.save(accounts);
	}

	@Override
	public List<BizAccountLevel> findAccountLevelsByAccountIds(List<Integer> accountIds) {
		return accountLevelRepository.findByAccountIdIsIn(accountIds);
	}

	@Override
	public BizHandicap getHandicap(Integer handicapCode) {
		return handicapService.findFromCacheByCode(handicapCode.toString());
	}

	/** 通知刷新缓存 */
	@Override
	public void noticeFreshCache(Number passageId) {
		redisService.convertAndSend(RedisTopics.FRESH_INACCOUNT_CACHE, passageId.toString());
	}

	@Override
	public void freshCache(Number passageId) {
		accountIdsAvailableCache.invalidate(passageId);
		accountIdsAvailableCache.refresh(passageId);
		accountAvailableCache.invalidate(passageId);
		accountAvailableCache.refresh(passageId);
	}

	@Override
	public void deleteUsedRecord(Long pocId, Integer accountId) {
		log.debug("解绑删除使用记录,参数:{},{}", pocId, accountId);
		String key = RedisKeys.NEWINACCOUNT_PASSAGEID_ACCOUNTIDS_USED_KEY + String.valueOf(pocId);
		String member = String.valueOf(accountId);
		boolean exist = redisService.getStringRedisTemplate().boundHashOps(key).hasKey(member);
		if (exist) {
			long ret = redisService.getStringRedisTemplate().boundHashOps(key).delete(member);
			log.debug("解绑删除使用记录,参数:{},{},删除结果:{}", pocId, accountId, ret);
		} else {
			log.debug("解绑删除使用记录不存在,参数:{},{}", pocId, accountId);
		}
	}

	/**
	 * 描述:从 可以使用的卡accountIds中筛选出某张卡
	 * 
	 * @param accountIds
	 *            可以使用的卡
	 * @param pocIds
	 *            该通道下缓存的卡
	 * @return
	 */
	private String selectAccountToUse(List<Integer> accountIds, Map<Long, List<Integer>> pocIds) {
		Assert.notNull(accountIds, "accountIds must not be null");
		Assert.notNull(pocIds, "pocIds must not be null");

		TreeMap<Long, Map<String, Long>> usedCountAccountIds = new TreeMap<>();
		for (Map.Entry<Long, List<Integer>> entry : pocIds.entrySet()) {
			Long pocId = entry.getKey();
			List<Integer> accountIds2 = entry.getValue();
			for (int i = 0, size = accountIds2.size(); i < size; i++) {
				Integer accountId = accountIds2.get(i);
				if (accountIds.contains(accountIds2.get(i))) {
					String key = RedisKeys.NEWINACCOUNT_PASSAGEID_ACCOUNTIDS_USED_KEY + pocId;
					String member = String.valueOf(accountId);
					boolean exist = redisService.getStringRedisTemplate().boundHashOps(key).hasKey(member);
					if (!exist) {
						redisService.getStringRedisTemplate().boundHashOps(key).put(member,
								System.currentTimeMillis() + "");
						return member;
					} else {
						Object val = redisService.getStringRedisTemplate().boundHashOps(key).get(member);
						Map<String, Long> map = new HashMap<>(16);
						map.put(member, pocId);
						usedCountAccountIds.put(Long.valueOf(val.toString()), map);
					}
				}
			}

		}
		if (!ObjectUtils.isEmpty(usedCountAccountIds)) {
			for (Map.Entry<Long, Map<String, Long>> entry : usedCountAccountIds.entrySet()) {
				System.out.println(entry.getKey() + ": " + entry.getValue());
				Map<String, Long> map = entry.getValue();
				// accountId
				String key = map.keySet().iterator().next();
				// pocId
				String val = map.values().iterator().next().toString();
				redisService.getStringRedisTemplate()
						.boundHashOps(RedisKeys.NEWINACCOUNT_PASSAGEID_ACCOUNTIDS_USED_KEY + val)
						.put(key, System.currentTimeMillis() + "");
				usedCountAccountIds.clear();
				return key;
			}

		}
		return null;
	}

	@Override
	public String selectAccountToUse(final List<Integer> accountIds, final Long pocId) {
		try {
			Assert.notNull(accountIds, "accountIds must not be null");
			List keys = new LinkedList();
			keys.add(RedisKeys.NEWINACCOUNT_PASSAGEID_ACCOUNTIDS_USED_KEY + String.valueOf(pocId));
			String val1 = accountIds.size() == 1 ? accountIds.get(0).toString()
					: String.join("|", accountIds.stream().map(p -> p.toString()).collect(Collectors.toList()));
			String[] argvs = new String[] { val1, System.currentTimeMillis() + "" };
			String ret = redisService.getStringRedisTemplate().execute(selectAccountToPayScript, keys, argvs);
			log.debug("执行脚本获取最近最少使用的卡,参数:pocId:{},accountIds:{},结果:{}", pocId, argvs, ret);
			return ret;
		} catch (Exception e) {
			log.error("执行脚本获取最近最少使用的卡异常:", e);
			return null;
		}
	}

	/**
	 * 描述:盘口通道可用入款卡是否大于3张(3张可配置)
	 * 
	 * @param pocId
	 *            通道id
	 * @return 小于3账号 true 否则 false
	 */
	@Override
	public boolean accessibleInAccountLessThan3(Number pocId) {
		try {
			if (!ObjectUtils.isEmpty(accountIdsAvailableCache.get(pocId))) {
				return accountIdsAvailableCache.get(pocId).size() < CommonUtils.inBankChangeBindCommModelCount();
			} else {
				freshCache(pocId);
				return accountIdsAvailableCache.get(pocId).size() < CommonUtils.inBankChangeBindCommModelCount();
			}
		} catch (ExecutionException e) {
			log.error(e.getLocalizedMessage(), e);
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public List<String> findAccountNosByIds(List<Integer> accountIds) {
		if (ObjectUtils.isEmpty(accountIds)) {
			return null;
		}
		List<BizAccount> accountList = accountService.findByIds(new ArrayList(accountIds));
		if (ObjectUtils.isEmpty(accountList)) {
			return null;
		}
		List<String> accountNos = accountList.stream().map(p -> p.getAccount()).collect(Collectors.toList());
		return accountNos;
	}

	@Override
	public String checkCardStatus(CheckCardStatusInputDTO inputDTO) {
		AccountBaseInfo baseInfo = accountService.getFromCacheByHandicapIdAndAccount(inputDTO.getHandicapId(),
				inputDTO.getCardNo());
		if (ObjectUtils.isEmpty(baseInfo) || AccountStatus.Freeze.getStatus().equals(baseInfo.getStatus())
				|| AccountStatus.Delete.getStatus().equals(baseInfo.getStatus())) {
			return "1";
		}
		return "-1";
	}

	private void writeResultToRedis(InAccountRandUtilResponse result, CardForPayInputDTO inputDTO) {
		log.debug("writeResultToRedis>> result {},inputDTO {}", result, inputDTO);
		BizHandicap handicap = getHandicap(inputDTO.getOid());
		if (handicap == null) {
			return;
		}
		String account = result.getTransferCardNum();
		writeResultToRedis(account, handicap.getId());
		String scanAcc = result.getScanCardNum();
		writeResultToRedis(scanAcc, handicap.getId());
	}

	private void writeResultToRedis(String account, int handicap) {
		log.debug("writeResultToRedis>> account {},handicap {}", account, handicap);
		Long currTM = System.currentTimeMillis();
		if (!StringUtils.isEmpty(account)) {
			AccountBaseInfo base = accountService.getFromCacheByHandicapIdAndAccount(handicap, account);
			if (base != null) {
				redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACCOUNT_INCOME_LASTTIME)
						.put(base.getId() + "", currTM + "");
			}
		}
	}

	/**
	 * @return the accountIdsAvailableCache
	 */
	protected LoadingCache<Number, List<Integer>> getAccountIdsAvailableCache() {
		return accountIdsAvailableCache;
	}

	/**
	 * TODO 待全部盘口切换到按银行卡类型入款，此方法可以废弃 入款时处理最大轮次的问题
	 *
	 * @param accId
	 */
	public void increaseInAccountExceed(Integer accId) {
		AccountBaseInfo base = accountService.getFromCacheById(accId);
		if (base == null || !Objects.equals(base.getType(), AccountType.InBank.getTypeId())
				|| Objects.isNull(base.getPassageId())) {
			return;
		}
		List<Integer> ids = accountIdsAvailableCache.getUnchecked(base.getPassageId());
		if (!CollectionUtils.isEmpty(ids)) {
			boolean allExceed = true;
			Map exceed = redisService.getStringRedisTemplate().boundHashOps(RedisKeys.INACCOUNT_EXCEED_CREDIT_COUNT)
					.entries();
			for (Integer id : ids) {
				AccountBaseInfo baseInfo = accountService.getFromCacheById(id);
				if (baseInfo != null && Objects.equals(baseInfo.getType(), AccountType.InBank.getTypeId())
						&& Objects.equals(baseInfo.getStatus(), AccountStatus.Normal.getStatus())
						&& !exceed.containsKey(id.toString()) && baseInfo.checkMobile()
						&& transService.isOnline(baseInfo.getId())) {
					allExceed = false;
					break;
				}
			}
			if (!allExceed) {
				redisService.getStringRedisTemplate().boundHashOps(RedisKeys.INACCOUNT_EXCEED_CREDIT_COUNT)
						.put(accId.toString(), "1");
			} else {
				redisService.getStringRedisTemplate().boundHashOps(RedisKeys.INACCOUNT_EXCEED_CREDIT_COUNT)
						.delete(ids.stream().map(p -> p.toString()).collect(Collectors.toList()).toArray());
			}
		}
	}

	/**
	 * 入款时处理最大轮次的问题，按层级
	 *
	 * @param accId
	 */
	public void increaseInAccountExceedByLevel(Integer accId) {
		AccountBaseInfo base = accountService.getFromCacheById(accId);
		if (base == null || !Objects.equals(base.getType(), AccountType.InBank.getTypeId())
				|| Objects.isNull(base.getCurrSysLevel())) {
			return;
		}
		String key = base.getHandicapId() + "-" + base.getCurrSysLevel();
		List<Integer> ids = handicapAvailableCache.getUnchecked(key);
		if (!CollectionUtils.isEmpty(ids)) {
			boolean allExceed = true;
			Map exceed = redisService.getStringRedisTemplate().boundHashOps(RedisKeys.INACCOUNT_EXCEED_CREDIT_LEVEL)
					.entries();
			for (Integer id : ids) {
				AccountBaseInfo baseInfo = accountService.getFromCacheById(id);
				if (baseInfo != null && Objects.equals(baseInfo.getType(), AccountType.InBank.getTypeId())
						&& Objects.equals(baseInfo.getStatus(), AccountStatus.Normal.getStatus())
						&& !exceed.containsKey(id.toString()) && baseInfo.checkMobile()
						&& !Objects.equals(baseInfo.getSubType(), InBankSubType.IN_BANK_YSF.getSubType())
						&& transService.isOnline(baseInfo.getId())) {
					allExceed = false;
					break;
				}
			}
			if (!allExceed) {
				redisService.getStringRedisTemplate().boundHashOps(RedisKeys.INACCOUNT_EXCEED_CREDIT_LEVEL)
						.put(accId.toString(), "1");
			} else {
				redisService.getStringRedisTemplate().boundHashOps(RedisKeys.INACCOUNT_EXCEED_CREDIT_LEVEL)
						.delete(ids.stream().map(p -> p.toString()).collect(Collectors.toList()).toArray());
			}
		}
	}

	public List<Map<String, Object>> bankList() {
		String forbiddenType = CommonUtils.getForbiddenUseAsInBank();
		List<Map<String, Object>> result = new ArrayList<>();
		for (BankEnums bank : BankEnums.values()) {
			if (!forbiddenType.contains("," + bank.getDesc() + ",")) {
				Map<String, Object> temp = new HashMap<>();
				temp.put("code", bank.getCode());
				temp.put("desc", bank.getDesc());
				result.add(temp);
			}
		}
		return result;
	}

	/**
	 * 2.0.4 根据层级和银行卡类型，返回入款卡
	 *
	 * @param inputDTO
	 * @param handicap
	 * @return
	 */
	@Override
	public CardForPayOutputDTO cardForPayByLevelCode(CardForPayInputDTO inputDTO, BizHandicap handicap) {
		try {
			String levelCode = inputDTO.getLevelCode();
			BizLevel level = levelService.findFromCache(handicap.getId(), levelCode);
			/**
			 * 1、有提供银行类型 1.1、取指定银行类型的先入后出卡、边入边出卡、PC自购卡
			 * 1.2、在1.1不能取到卡时，不限定银行取先入后出卡、边入边出卡、PC自购卡
			 */

			// 需过滤掉的卡
			Set<Integer> failure = transService.buildFailureTrans();
			Set<Integer> model = redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_NEW_MODEL).entries()
					.entrySet().stream().filter(p -> p.getValue().toString().equals(Constants.YSF_MODEL_OUT + ""))
					.map(p -> Integer.parseInt(p.getKey().toString())).collect(Collectors.toSet());
			Set<Integer> accAlarm = redisService.getStringRedisTemplate().boundHashOps(RedisKeys.PROBLEM_ACC_ALARM)
					.entries().entrySet().stream().map(p -> Integer.parseInt(p.getKey().toString()))
					.collect(Collectors.toSet());
			// 取盘口、层级的卡的缓存数据
			String key = handicap.getId() + "-" + level.getCurrSysLevel();
			List<Integer> idsFromCache = handicapAvailableCache.getUnchecked(key);
			log.debug("cardForPayByLevelCode>>缓存中的入款卡ID:{}", ObjectMapperUtils.serialize(idsFromCache));
			if (CollectionUtils.isEmpty(idsFromCache)) {
				log.error("无可用卡！");
				return null;
			}

			// 过滤掉连续转账失败、出款模式、账号设备告警的卡
			idsFromCache = idsFromCache.stream()
					.filter(p -> !failure.contains(p) && !model.contains(p) && !accAlarm.contains(p))
					.collect(Collectors.toList());
			List<AccountBaseInfo> accountBaseInfos = idsFromCache.stream().map(p -> accountService.getFromCacheById(p))
					.collect(Collectors.toList());
			log.debug("cardForPayByLevelCode>>过滤掉连续转账失败、模式、设备告警后入款卡ID:{}", ObjectMapperUtils.serialize(idsFromCache));
			if (CollectionUtils.isEmpty(idsFromCache)) {
				log.error("无可用卡！");
				return null;
			}

			// 过滤账目排查有问题的账号
			Set<Integer> invSet = systemAccountManager.accountingException();
			invSet.addAll(systemAccountManager.accountingSuspend());
			List<AccountBaseInfo> accountNoException = accountBaseInfos.stream()
					.filter(p -> !invSet.contains(p.getId())).collect(Collectors.toList());
			log.debug("cardForPayByLevelCode>>过滤账目排查有问题的账号后入款卡：{}", ObjectMapperUtils.serialize(accountNoException));
			if (CollectionUtils.isEmpty(accountNoException)) {
				log.error("无可用卡！");
				return null;
			}

			// 过滤掉状态不是在用、非入款卡、不在线、额度不够的卡;云闪付卡可用额度需要大于1万。
			List<AccountBaseInfo> idsForUse = accountNoException.stream()
					.filter(p -> p.getStatus().equals(AccountStatus.Normal.getStatus())
							&& p.getType().intValue() == AccountType.InBank.getTypeId().intValue()
							&& transService.isOnline(p.getId())
							&& accountChangeService.currCredits(p) - inputDTO.getAmount().intValue() > 0
							&& (Objects.equals(p.getSubType(), InBankSubType.IN_BANK_YSF.getSubType())
									? (accountChangeService.currCredits(p) - inputDTO.getAmount().intValue()) > 10000
									: true))
					.collect(Collectors.toList());
			log.debug("cardForPayByLevelCode>>过滤掉连续转账失败、模式、设备告警后入款卡：{}", ObjectMapperUtils.serialize(idsForUse));
			if (CollectionUtils.isEmpty(idsForUse)) {
				log.error("无可用卡！");
				return null;
			}

			// 过滤掉单笔最小入款金额不符合要求的卡
			List<AccountBaseInfo> filterMinIn = idsForUse.stream()
					.filter(p -> ObjectUtils.isEmpty(p.getMinInAmount())
							|| p.getMinInAmount().compareTo(new BigDecimal(0)) == 0
							|| p.getMinInAmount().compareTo(new BigDecimal(inputDTO.getAmount().doubleValue())
									.setScale(2, BigDecimal.ROUND_HALF_UP)) <= 0)
					.collect(Collectors.toList());
			log.debug("cardForPayByLevelCode>>过滤掉最小单笔入款金额的卡：{}", ObjectMapperUtils.serialize(idsForUse));
			filterMinIn = CollectionUtils.isEmpty(filterMinIn) ? idsForUse : filterMinIn;
			CardForPayOutputDTO outputDTO = returnOutPutDtoForLevelCode(filterMinIn, inputDTO, handicap.getId());
			return outputDTO;
		} catch (Exception e) {
			log.error("获取收款卡失败:", e);
			return null;
		}
	}
}
