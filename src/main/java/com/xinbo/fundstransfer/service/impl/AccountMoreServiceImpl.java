package com.xinbo.fundstransfer.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.entity.BizAccountMore;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.entity.SysUserProfile;
import com.xinbo.fundstransfer.domain.repository.AccountMoreRepository;
import com.xinbo.fundstransfer.service.AccountMoreService;
import com.xinbo.fundstransfer.service.RedisService;
import com.xinbo.fundstransfer.service.SysUserProfileService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.util.Assert;
import org.apache.shiro.util.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class AccountMoreServiceImpl implements AccountMoreService {
	@Autowired
	AccountMoreRepository accountMoreDao;
	@Autowired
	private RedisService redisSer;
	@PersistenceContext
	private EntityManager entityManager;
	@Autowired
	private SysUserProfileService sysUserProfileService;
	private static ObjectMapper mapper = new ObjectMapper();
	private static final String PREFIX_CACHE_ID = "I_";
	private static final String PREFIX_CACHE_UID = "U_";
	private static final String PREFIX_CACHE_MOBILE = "M_";
	private static final Cache<Object, BizAccountMore> CacheAccountMore = CacheBuilder.newBuilder().maximumSize(60000)
			.expireAfterWrite(30, TimeUnit.MINUTES).build();

	@Override
	@Transactional
	public void updateBalance(Integer id, BigDecimal balance) {
		accountMoreDao.updateBalance(id, balance);
		broadCast(accountMoreDao.getOne(id));
	}

	@Override
	@Transactional
	public BizAccountMore saveAndFlash(BizAccountMore arg0) {
		BizAccountMore more = accountMoreDao.saveAndFlush(arg0);
		broadCast(more);
		return more;
	}

	@Override
	@Transactional
	public BizAccountMore deleteAcc(String uid, Integer accId) {
		BizAccountMore more = getFromCacheByUid(uid);
		if (Objects.isNull(more) || StringUtils.isBlank(more.getAccounts()))
			return null;
		String accounts = StringUtils.trimToEmpty(more.getAccounts());
		Set<String> other = new HashSet<>();
		for (String acc : accounts.split(",")) {
			if (StringUtils.isNotBlank(acc) && !Objects.equals(acc, String.valueOf(accId)))
				other.add(acc);
		}
		accounts = CollectionUtils.isEmpty(other) ? StringUtils.EMPTY : String.join(",", other);
		if (!accounts.startsWith(","))
			accounts = "," + accounts;
		if (!accounts.endsWith(","))
			accounts = accounts + ",";
		more.setAccounts(accounts);
		// 如果账号删除完了 则把手机号清空
		if (",".equals(more.getAccounts()))
			more.setMoible(null);
		return saveAndFlash(more);
	}

	@Override
	public BizAccountMore getFromCacheByMobile(String mobile) {
		if (StringUtils.isBlank(mobile))
			return null;
		BizAccountMore more = CacheAccountMore.getIfPresent(cacheKey(PREFIX_CACHE_MOBILE, mobile));
		if (Objects.nonNull(more))
			return more;
		more = accountMoreDao.findByMoible(mobile);
		flushCache(more);
		return more;
	}

	@Override
	public Map<Integer, BizAccountMore> allFromCacheByMobile(List<Integer> accountIds) {
		Assert.notNull(accountIds, "账号id为空");
		Map<Integer, BizAccountMore> res = Maps.newLinkedHashMap();
		List<BizAccountMore> list = accountMoreDao.findByIdIn(accountIds);
		// log.debug("查询到的所有返利网账号信息:{}", all.toString());
		if (!CollectionUtils.isEmpty(list)) {
			list.stream().forEach(p -> res.put(p.getId(), p));
		}
		return res;
	}

	@Override
	public BizAccountMore findByMobile(String mobile) {
		return accountMoreDao.findByMoible(mobile);
	}

	@Override
	public BizAccountMore getFromByUid(String uid) {
		return accountMoreDao.findByUid(uid);
	}

	@Override
	public BizAccountMore getFromCacheByUid(String uid) {
		if (StringUtils.isBlank(uid))
			return null;
		BizAccountMore more = CacheAccountMore.getIfPresent(cacheKey(PREFIX_CACHE_UID, uid));
		if (Objects.nonNull(more))
			return more;
		more = accountMoreDao.findByUid(uid);
		flushCache(more);
		return more;
	}

	/**
	 * replace cache content by {@code more}
	 */
	@Override
	public void flushCache(BizAccountMore more) {
		if (Objects.isNull(more) || Objects.isNull(more.getId()) || Objects.isNull(more.getMoible())
				|| Objects.isNull(more.getUid()))
			return;
		CacheAccountMore.put(cacheKey(PREFIX_CACHE_ID, more.getId()), more);
		CacheAccountMore.put(cacheKey(PREFIX_CACHE_MOBILE, more.getMoible()), more);
		CacheAccountMore.put(cacheKey(PREFIX_CACHE_UID, more.getUid()), more);
	}

	@Override
	public void cleanCache() {
		CacheAccountMore.invalidateAll();
	}

	/**
	 * broadcast the updated {@link BizAccountMore} object to every web server by
	 * redis.
	 *
	 * @param more
	 *            refer to {@link BizAccountMore}
	 */
	private void broadCast(BizAccountMore more) {
		if (more == null || more.getId() == null || more.getMoible() == null || more.getUid() == null)
			return;
		try {
			redisSer.convertAndSend(RedisTopics.REFRESH_ACCOUNT_MORE, mapper.writeValueAsString(more));
		} catch (Exception e) {
			log.error("AccMore >> broadCast id: {} uid: {} mobile: {} accounts: {} error: {}", more.getId(),
					more.getUid(), more.getMoible(), more.getAccounts(), e.getLocalizedMessage());
		}
	}

	/**
	 * package key for {@link BizAccountMore}
	 *
	 * @return String.contact(prefix, fixVal);
	 */
	private String cacheKey(String prefix, Object fixVal) {
		return prefix + fixVal;
	}

	@Override
	public List<BizAccountMore> getAccountMoreByaccountId(int accountId) {
		return accountMoreDao.getAccountMoreByaccountId(accountId);
	}

	@Transactional
	@Override
	public void updateMobileByUid(String uid, String mobile) {
		accountMoreDao.updateMobileByUid(uid, mobile);
		BizAccountMore more = accountMoreDao.findByMoible(mobile);
		broadCast(more);
	}

	@Transactional
	@Override
	public void setToZeroCredit(SysUser operator, String mobile) {
		if (StringUtils.isEmpty(mobile)) {
			return;
		}
		BizAccountMore more = getFromCacheByMobile(mobile);
		if (Objects.isNull(more)) {
			return;
		}
		if (more.getMargin() != null && more.getMargin().intValue() >= 1000) {
			return;
		}
		accountMoreDao.updateMargin(more.getId(), BigDecimal.valueOf(1000));
		more.setMargin(BigDecimal.valueOf(1000));
		more.setLinelimit(BigDecimal.valueOf(1000));
		flushCache(more);
		broadCast(more);
		log.info("SetToZeroCredit >>> mobile {} operator {}", mobile, operator.getUsername());
	}

	@Transactional
	@Override
	public void updateMargin(Integer id, BigDecimal margin) {
		BizAccountMore more = accountMoreDao.getOne(id);
		accountMoreDao.updateMargin(more.getId(), margin);
		more.setMargin(margin);
		flushCache(more);
		broadCast(more);
	}

	@Transactional
	@Override
	public void updateAccountsByid(Integer id, String accounts) {
		BizAccountMore more = accountMoreDao.getOne(id);
		accountMoreDao.updateAccountsByid(more.getId(), accounts);
		more.setAccounts(accounts);
		flushCache(more);
		broadCast(more);
	}

	public Map<String, Object> findPage(String rebateUsername, BigDecimal marginMin, BigDecimal marginMax,
			BigDecimal currMarginMin, BigDecimal currMarginMax, BigDecimal totalRebateMin, BigDecimal totalRebateMax,
			String developType, String sortProperty, Integer sortDirection, Pageable request) {
		StringBuilder buffer = new StringBuilder(
				"select more.* from biz_account_more more,biz_rebate_user t where more.uid = t.uid and IFNULL(more.is_display,1) <> 0 ");
		StringBuilder summaryQuery = new StringBuilder(
				"select count(1) total,sum(IFNULL(margin,0)) margin,sum(IFNULL(total_out_flow,0)) outflow,sum(IFNULL(total_rebate,0)) rabate,sum(IFNULL(tmp_margin,0)) tmpMargin,sum(IFNULL(activity_total_amount,0)) activity,sum(IFNULL(linelimit,0)) linelimit from biz_account_more more ,biz_rebate_user t where more.uid = t.uid and IFNULL(more.is_display,1) <> 0 ");
		StringBuilder temp = new StringBuilder("");
		if (StringUtils.isNotBlank(rebateUsername)) {
			temp.append(" and more.uid = (select uid from biz_rebate_user t where t.user_name = '")
					.append(rebateUsername.trim()).append("')");
		}
		if (marginMin != null) {
			temp.append(" and IFNULL(more.margin,0) >=").append(marginMin);
		}
		if (marginMax != null) {
			temp.append(" and IFNULL(more.margin,0) <=").append(marginMax);
		}
		if (currMarginMin != null) {
			temp.append(" and IFNULL(more.linelimit,0)+IFNULL(more.tmp_margin,0) >=").append(currMarginMin);
		}
		if (currMarginMax != null) {
			temp.append(" and IFNULL(more.linelimit,0)+IFNULL(more.tmp_margin,0) <=").append(currMarginMax);
		}
		if (totalRebateMin != null) {
			temp.append(" and IFNULL(more.total_rebate,0) >=").append(totalRebateMin);
		}
		if (totalRebateMax != null) {
			temp.append(" and IFNULL(more.total_rebate,0) <=").append(totalRebateMax);
		}
		if ("1".equals(developType)) {
			// 额度提升再加上配置项的过滤条件
			StringBuilder setting = new StringBuilder("");
			boolean enableSetting = false; // 是否只启用一个配置项
			SysUserProfile zeroProfile = sysUserProfileService.findByUserIdAndPropertyKey(AppConstants.USER_ID_4_ADMIN,
					"MARGIN_ZERO_DAYS_MORE");
			if (zeroProfile != null && !StringUtils.equals(zeroProfile.getIsEnable(), "0")
					&& CommonUtils.isNumeric(zeroProfile.getPropertyValue())) {
				setting.append(" and (").append(" ( t.create_time < date_sub(now(), interval ")
						.append(zeroProfile.getPropertyValue()).append(" day) and IFNULL(margin,0) = 1000 )");
				enableSetting = true;
			}
			SysUserProfile less3000 = sysUserProfileService.findByUserIdAndPropertyKey(AppConstants.USER_ID_4_ADMIN,
					"MARGIN_LESS_3000_DAYS_MORE");
			if (less3000 != null && !StringUtils.equals(less3000.getIsEnable(), "0")
					&& CommonUtils.isNumeric(less3000.getPropertyValue())) {
				if (setting.length() == 0) {
					setting.append(" and (");
				} else {
					setting.append(" or ");
				}
				setting.append(" ( t.create_time < date_sub(now(), interval ").append(less3000.getPropertyValue())
						.append(" day) and IFNULL(margin,0) <> 1000 and IFNULL(margin,0) < 3000 )");
				enableSetting = true;
			}
			SysUserProfile m3000 = sysUserProfileService.findByUserIdAndPropertyKey(AppConstants.USER_ID_4_ADMIN,
					"MARGIN_3000_10000_DAYS_MORE");
			if (m3000 != null && !StringUtils.equals(m3000.getIsEnable(), "0")
					&& CommonUtils.isNumeric(m3000.getPropertyValue())) {
				if (setting.length() == 0) {
					setting.append(" and (");
				} else {
					setting.append(" or ");
				}
				setting.append(" ( t.create_time < date_sub(now(), interval ").append(m3000.getPropertyValue())
						.append(" day) and IFNULL(margin,0) >= 3000 and IFNULL(margin,0) < 10000)");
				enableSetting = true;
			}
			SysUserProfile margin = sysUserProfileService.findByUserIdAndPropertyKey(AppConstants.USER_ID_4_ADMIN,
					"MARGIN_ACTIVE_CARDS");
			if (margin != null && !StringUtils.equals(margin.getIsEnable(), "0")
					&& StringUtils.isNotBlank(margin.getPropertyValue())) {
				String marginStr = margin.getPropertyValue();
				String[] strs = marginStr.split("-");
				if (strs.length == 2 && CommonUtils.isNumeric(strs[0]) && CommonUtils.isNumeric(strs[1])) {
					if (setting.length() == 0) {
						setting.append(" and (");
					} else {
						setting.append(" or ");
					}
					setting.append(" ( IFNULL(margin,0) >=  ").append(strs[0]);
					setting.append(" and (LENGTH(more.accounts) - LENGTH( REPLACE(more.accounts,',','') )+1) < ")
							.append(strs[1]).append(")");
					enableSetting = true;
				}
			}
			if (enableSetting) {
				setting.append(")");
				temp.append(setting);
			}
			temp.append(" and IFNULL(more.display_after_date,now()-100) <= now()");
		} else if ("2".equals(developType)) {
			// 卡数提升再加上配置项的过滤条件
			SysUserProfile card = sysUserProfileService.findByUserIdAndPropertyKey(AppConstants.USER_ID_4_ADMIN,
					"ACTIVE_CARDS_JOIN_DAYS_MARGIN");
			if (card != null && !StringUtils.equals(card.getIsEnable(), "0")) {
				String cardStr = card.getPropertyValue();
				String[] cards = cardStr.split("-");
				if (cards.length == 3 && CommonUtils.isNumeric(cards[0]) && CommonUtils.isNumeric(cards[1])
						&& CommonUtils.isNumeric(cards[2])) {
					StringBuilder setting = new StringBuilder("");
					setting.append(" and (");
					setting.append(" ( IFNULL(more.margin,0) >= ").append(cards[2]);
					setting.append(" and t.create_time < date_sub(now(), interval ").append(cards[1]).append(" day)");
					setting.append(" and (LENGTH(more.accounts) - LENGTH( REPLACE(more.accounts,',','') )+1) <= ")
							.append(cards[0]).append(" )").append(" ) ");
					temp.append(setting);
				}
			}
			temp.append(" and IFNULL(more.display_after_date,now()-100) <= now() ");
		}
		summaryQuery.append(temp);
		buffer.append(temp);
		if (StringUtils.isNotBlank(sortProperty)) {
			if ("margin".equals(sortProperty)) {
				buffer.append(" order by IFNULL(more.margin,0) ");
			} else if ("lineLimit".equals(sortProperty)) {
				buffer.append(" order by IFNULL(more.linelimit,0)+IFNULL(more.tmp_margin,0) ");
			} else if ("currMargin".equals(sortProperty)) {
				buffer.append(" order by IFNULL(margin,0)+IFNULL(more.tmp_margin,0) ");
			} else if ("totalOutFlow".equals(sortProperty)) {
				buffer.append(" order by IFNULL(total_out_flow,0)");
			} else if ("totalRebate".equals(sortProperty)) {
				buffer.append(" order by IFNULL(total_rebate,0)");
			} else if ("create_time".equals(sortProperty)) {
				buffer.append(" order by IFNULL(t.create_time,0)");
			}
			if (sortDirection == 2) {
				buffer.append(" desc");
			} else {
				buffer.append(" asc");
			}
		}
		buffer.append(" limit ");
		buffer.append(request.getOffset()).append(",").append(request.getPageSize());

		// 查询汇总信息
		List<Object[]> summary = entityManager.createNativeQuery(summaryQuery.toString()).getResultList();
		Map<String, Object> totalinfo = new HashMap();
		BigInteger totalCount = null;
		for (Object[] o : summary) {
			totalCount = (BigInteger) o[0];
			totalinfo.put("totalCount", totalCount);
			totalinfo.put("margin", totalCount.intValue() == 0 ? BigDecimal.ZERO : ((BigDecimal) o[1]).floatValue());
			totalinfo.put("totalOutFlow",
					totalCount.intValue() == 0 ? BigDecimal.ZERO : ((BigDecimal) o[2]).floatValue());
			totalinfo.put("totalRebate",
					totalCount.intValue() == 0 ? BigDecimal.ZERO : ((BigDecimal) o[3]).floatValue());
			totalinfo.put("totalCurrMargin", totalCount.intValue() == 0 ? BigDecimal.ZERO
					: ((BigDecimal) o[1]).add((BigDecimal) o[4]).add((BigDecimal) o[5]).floatValue());
			totalinfo.put("totalLineLimit", totalCount.intValue() == 0 ? BigDecimal.ZERO
					: ((BigDecimal) o[6]).add((BigDecimal) o[4]).add((BigDecimal) o[5]).floatValue());
			break;
		}
		// 查询列表信息
		List<BizAccountMore> moreList = entityManager.createNativeQuery(buffer.toString(), BizAccountMore.class)
				.getResultList();
		Map<String, Object> result = new HashMap<>();
		result.put("total", totalinfo);
		result.put("page", new PageImpl(moreList, request, totalCount == null ? 0L : totalCount.longValue()));
		return result;
	}
}
