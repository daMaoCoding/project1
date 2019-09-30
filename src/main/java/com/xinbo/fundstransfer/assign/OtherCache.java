package com.xinbo.fundstransfer.assign;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import com.xinbo.fundstransfer.report.SystemAccountManager;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.runtime.MemCacheUtils;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.AllocateTransService;
import com.xinbo.fundstransfer.service.RedisService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class OtherCache {
	@Autowired
	private RedisService redisService;
	@Autowired
	private AllocateTransService allocateTransService;
	@Autowired
	private SystemAccountManager systemAccountManager;
	@Autowired @Lazy
	protected AccountService accountService;
	@Autowired
	private EntityManager entityManager;
	
	@SuppressWarnings("unchecked")
	public boolean isCachedUnMatched(Integer accountId) {
		try {
			Object obj = otherCache.get("unMatched");
			if(! (obj instanceof Set)) {
				return false;
			}
			Set<Integer> invSet = (Set<Integer>)obj;
			if(invSet.contains(accountId)) {
				return true;
			} else {
				return false;
			}			
		} catch (ExecutionException e) {
			log.error(e.getMessage());
			return true;
		}
	}
	
	@SuppressWarnings("unchecked")
	public boolean isCachedAccIdSetInValTm(Integer accountId) {
		try {
			Object obj = otherCache.get("accIdSetInValTm");
			if(! (obj instanceof Set)) {
				return false;
			}
			Set<Integer> invSet = (Set<Integer>)obj;
			if(invSet.contains(accountId)) {
				return true;
			} else {
				return false;
			}			
		} catch (ExecutionException e) {
			log.error(e.getMessage());
			return true;
		}
	}	
	
	@SuppressWarnings("unchecked")
	public boolean isCachedIdsByNonExprie(Integer accountId) {
		try {
			Object obj = otherCache.get("idsByNonExprie");
			if(! (obj instanceof Set)) {
				return false;
			}
			Set<Integer> invSet = (Set<Integer>)obj;
			if(invSet.contains(accountId)) {
				return true;
			} else {
				return false;
			}			
		} catch (ExecutionException e) {
			log.error(e.getMessage());
			return true;
		}
	}	
	
	@SuppressWarnings("unchecked")
	public boolean isCachedInvSetOut(Integer accountId) {
		try {
			Object obj = otherCache.get("invSetOut");
			if(! (obj instanceof Set)) {
				return false;
			}
			Set<Integer> invSet = (Set<Integer>)obj;
			if(invSet.contains(accountId)) {
				return true;
			} else {
				return false;
			}			
		} catch (Exception e) {
			log.error(e.getMessage());
			return true;
		}
	}	
	
	@SuppressWarnings("unchecked")
	public boolean isCachedInvSetIn(Integer accountId) {
		try {
			Object obj = otherCache.get("invSetIn");
			if(! (obj instanceof Set)) {
				return false;
			}
			Set<Integer> invSet = (Set<Integer>)obj;
			if(invSet.contains(accountId)) {
				return true;
			} else {
				return false;
			}			
		} catch (Exception e) {
			log.error(e.getMessage());
			return true;
		}
	}	

	@SuppressWarnings("unchecked")
	public Map<Integer, List<BizAccount>> getCachedUSERACCMAP() {
		Object obj;
		try {
			obj = otherCache.get("USERACCMAP");
			if(!(obj instanceof Map)) {
				return null;
			}
			return (Map<Integer, List<BizAccount>>)obj;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	public Map<Object, Object> getCachedLAST() {
		Object obj;
		try {
			obj = otherCache.get("LAST");
			if(!(obj instanceof Map)) {
				return null;
			}
			return (Map<Object, Object>)obj;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}	
	
	@SuppressWarnings("unchecked")
	public Set<String> getCachedBLACK() {
		Object obj;
		try {
			obj = otherCache.get("BLACK");
			if(!(obj instanceof Set)) {
				return null;
			}
			return (Set<String>)obj;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public ArrayList<String> getCachedOutOrdered() {
		Object obj;
		try {
			obj = otherCache.get("OUT_ORDERED");
			if(!(obj instanceof ArrayList)){
				return null;
			}
			return (ArrayList<String>)obj;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return new ArrayList<>();
		}
	}

	public Set<Integer> getTransFailure(){
		Object obj;
		try {
			obj = otherCache.get("TRANSFAILURE");
			if(!(obj instanceof Set)){
				return new HashSet<>();
			}
			return (Set<Integer>)obj;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return new HashSet<>();
		}
	}
	
	private LoadingCache<String, Object> otherCache = CacheBuilder.newBuilder()
			.concurrencyLevel(1).refreshAfterWrite(30, TimeUnit.SECONDS) // 每30秒自动刷新
			.build(new CacheLoader<String, Object>() {
				public Object load(String key) {
					if("unMatched".equals(key)) {
						// 未匹配流水告警数据
						Set<Integer> unMatched = buildAllUnMatched();
						log.trace("cache unMatched>> {}", unMatched);
						return unMatched;
					}
					if("accIdSetInValTm".equals(key)) {
						// 银行余额上报有效时间
						Set<Integer> accIdSetInValTm = allocateTransService.buildValidAcc();
						log.trace("cache accIdSetInValTm>> {}", accIdSetInValTm);
						return accIdSetInValTm;
					} 
					if("idsByNonExprie".equals(key)) {
						// 出款频率限制
						Set<Integer> idsByNonExprie = buildIdByNonExprie();
						log.trace("cache idsByNonExprie>> {}", idsByNonExprie);
						return idsByNonExprie;
					}
					if("invSetOut".equals(key)) {
						// 转出限制
						Set<Integer> invSet = systemAccountManager.alarm4AccountingInOut(true);
						log.trace("cache invSet>> {}", invSet);
						return invSet;
					}
					if("invSetIn".equals(key)) {
						// 转入限制
						Set<Integer> invSet = systemAccountManager.alarm4AccountingInOut(false);
						log.trace("cache invSet>> {}", invSet);
						return invSet;
					}
					if ("USERACCMAP".equals(key)) {
						Map<Integer, List<BizAccount>> userAccMap = findOutAccList4Manual();
						if (!CollectionUtils.isEmpty(userAccMap)) {
							return userAccMap;
						}
					} 
					if ("LAST".equals(key)) {
						Map<Object, Object> last = redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ALLOC_OTASK_LAST).entries();
						if (!CollectionUtils.isEmpty(last)) {
							return last;
						}
					}
					if ("BLACK".equals(key)) {
						Set<String> black = findBlackList();
						if (!CollectionUtils.isEmpty(black)) {
							return black;
						}
					}
					if("OUT_ORDERED".equals(key)){
						Set<String> scores = redisService.getStringRedisTemplate().boundZSetOps(Constants.OUT_ACCOUNT_ORDERED).range(0,
								-1);
						return new ArrayList<>(scores);
					}
					return null;
				}
			});
	public void invalidCache(String key){
		otherCache.invalidate(key);
	}

	private Map<Integer, List<BizAccount>> findOutAccList4Manual() {
		List<BizAccount> accList = accountService.findOutAccList4Manual();
		Map<Integer, List<BizAccount>> accMap = new HashMap<>();
		for (BizAccount acc : accList) {
			List<BizAccount> temp = accMap.get(acc.getHolder());
			if (temp == null) {
				temp = new ArrayList<>();
			}
			temp.add(acc);
			accMap.put(acc.getHolder(), temp);
		}
		return accMap;
	}
	
	private Set<String> findBlackList() {
		return redisService.getStringRedisTemplate().keys(RedisKeys.genPattern4TransBlack());
	}	
	
	@SuppressWarnings("unchecked")
	private Set<Integer> buildAllUnMatched() {
		String sql = String.format(
				"select case when operator is null then account_id else operator end from biz_outward_task where status in (0,1,2) and account_id is not null and asign_time>='%s' group by case when operator is null then account_id else operator end having count(1) >5",
				CommonUtils.getStartTimeOfCurrDay());
		List<Object> result = entityManager.createNativeQuery(sql).getResultList();
		if (!CollectionUtils.isEmpty(result)) {
			Set<Integer> unmatched = new HashSet<>();
			for (Object obj : result) {
				unmatched.add(Integer.parseInt(obj.toString()));
			}
			return unmatched;
		}
		return null;
	}	
	
	@SuppressWarnings("unchecked")
	private Set<Integer> buildIdByNonExprie() {
		String rateLimitStr = MemCacheUtils.getInstance().getSystemProfile().getOrDefault("OUTDRAW_RATE_LIMIT", "0");
		int rateLimit = StringUtils.isNumeric(rateLimitStr) ? Integer.parseInt(rateLimitStr) : 0;
		if (rateLimit <= 0)
			return Collections.EMPTY_SET;
		rateLimit = (rateLimit > 3600 ? 3600 : rateLimit) * 1000;
		Object obj = redisService.getStringRedisTemplate().opsForHash().entries(RedisKeys.LAST_TIME_OUTWARD);
		Map<String, String> data = (Map<String, String>) obj;
		if (CollectionUtils.isEmpty(data))
			return Collections.EMPTY_SET;
		Set<Integer> ret = new HashSet<>();
		long curr = System.currentTimeMillis();
		Iterator<Map.Entry<String, String>> it = data.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, String> item = it.next();
			String key = item.getKey(), value = item.getValue();
			if (Objects.nonNull(key) && Objects.nonNull(value)) {
				if (curr < (Long.parseLong(value) + rateLimit)) {
					ret.add(Integer.valueOf(key));
					it.remove();
				}
			}
		}
		if (!CollectionUtils.isEmpty(data))
			redisService.getStringRedisTemplate().opsForHash().delete(RedisKeys.LAST_TIME_OUTWARD,
					data.keySet().toArray());
		return ret;
	}	
}
