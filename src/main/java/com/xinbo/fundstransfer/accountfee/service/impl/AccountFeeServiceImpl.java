/**
 * 
 */
package com.xinbo.fundstransfer.accountfee.service.impl;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.accountfee.exception.NoSuiteAccountFeeRuleException;
import com.xinbo.fundstransfer.accountfee.pojo.*;
import com.xinbo.fundstransfer.accountfee.service.AccountFee4PlatService;
import com.xinbo.fundstransfer.accountfee.service.AccountFeeService;
import com.xinbo.fundstransfer.component.redis.msgqueue.HandleException;
import com.xinbo.fundstransfer.domain.entity.AccountFee;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.repository.AccountFeeRepository;
import com.xinbo.fundstransfer.domain.repository.AccountRepository;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.HandicapService;
import com.xinbo.fundstransfer.service.RedisService;
import com.xinbo.fundstransfer.utils.randutil.JedisLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Blake
 *
 */
@Slf4j
@Service
public class AccountFeeServiceImpl implements AccountFee4PlatService, AccountFeeService {

	private static final ObjectMapper mapper = new ObjectMapper();
	static {
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private HandicapService handicapService;

	@Autowired
	private RedisService redisService;

	@Autowired
	private AccountFeeRepository accountFeeRepository;
	
	@Autowired
	private ApplicationContext applicationContext;

	private static String getAccountFeeRedisKey(Integer accountId) {
		return String.format("ZAC:%s", accountId);
	}

	private static String obj2JsonStr(Object o) {
		String value = null;
		if (!ObjectUtils.isEmpty(o)) {
			try {
				value = mapper.writeValueAsString(o);
			} catch (JsonProcessingException e) {
				log.error("AccountFeeServiceImpl.obj2JsonStr将对象转换为json字符串时异常", e);
			}
		}
		return value;
	}

	private <T> T deserialize(String jsonStr, Class<T> targetClasses) {
		T t = null;
		if (!StringUtils.isEmpty(jsonStr)) {
			try {
				t = mapper.readValue(jsonStr, targetClasses);
			} catch (IOException e) {
				log.error("AccountFeeServiceImpl.deserialize时异常，jsonStr={},targetClasses={}", jsonStr,
						targetClasses.getName(), e);
			}
		}
		return t;
	}

	private <T> T getObjectFromCache(Integer accountId, Class<T> ObjectClass) {
		String jsonStr = getObjectJsonStrFromCache(accountId);
		return deserialize(jsonStr, ObjectClass);
	}

	private AccountFee getFromCache(Integer accountId) {
		return getObjectFromCache(accountId, AccountFee.class);
	}

	private void accountFee2Cache(AccountFee af) {
		if (ObjectUtils.isEmpty(af)) {
			return;
		}
		StringRedisTemplate redis = redisService.getYsfStringRedisTemplate();
		if (!ObjectUtils.isEmpty(redis)) {
			String cacheKey = getAccountFeeRedisKey(af.getAccountId());
			String cacheValue = obj2JsonStr(af);
			if (!StringUtils.isEmpty(cacheValue)) {
				redis.opsForValue().set(cacheKey, cacheValue);
				redis.expire(cacheKey, 1, TimeUnit.HOURS);
			}
		}
	}

	private String getObjectJsonStrFromCache(Integer accountId) {
		String result = null;
		StringRedisTemplate redis = null;
		if (!ObjectUtils.isEmpty(accountId)
				&& !ObjectUtils.isEmpty(redis = redisService.getYsfStringRedisTemplate())) {
			Object cacheObject = redis.opsForValue().get(getAccountFeeRedisKey(accountId));
			if (cacheObject != null) {
				result = cacheObject.toString();
			}
		}
		return result;
	}

	private String getHandicapCode(final Integer handicapId) {
		BizHandicap bizHandicap = handicapService.findFromCacheById(handicapId);
		if (!ObjectUtils.isEmpty(bizHandicap)) {
			return (bizHandicap.getCode());
		}
		return null;
	}

	private AccountFee getAccountFee(Integer accountId) {
		AccountFee af = getFromCache(accountId);
		if (ObjectUtils.isEmpty(af)) {
			af = accountFeeRepository.findByAccountId(accountId);
			if (!ObjectUtils.isEmpty(af)) {
				accountFee2Cache(af);
			}
		}
		return af;
	}

	private BizAccount getAccountByHandicapAndBankTypeAndAccount(final String handicap, final String bankType,
			final String account) throws HandleException {
		BizHandicap bizHandicap = handicapService.findFromCacheByCode(handicap);
		if (ObjectUtils.isEmpty(bizHandicap)) {
			log.error(
					"AccountFeeServiceImpl.getAccountByHandicapAndBankTypeAndAccount通过handicapCode={}查询业主盘口信息时未能查询到具体信息",
					handicap);
			throw new HandleException(String.format("未知的handicap=%s", handicap));
		}
		BizAccount accountInfo = accountRepository.findByHandicapIdAndAccountAndBankType(bizHandicap.getId(), account,
				bankType);
		return accountInfo;
	}

	private AccountFeeConfig findByAccount(Integer accountId) {
		AccountFee af = getAccountFee(accountId);
		AccountFeeConfig result = af2afc(af);
		return result;
	}
	
	private AccountFeeConfig af2afc(AccountFee af) {
		AccountFeeConfig result = new AccountFeeConfig();
		List<AccountFeeCalFeeLevelMoney> calFeeLevelMoneyList = new ArrayList<AccountFeeCalFeeLevelMoney>();
		List<AccountFeeCalFeeLevelPercent> calFeeLevelPercentList = new ArrayList<AccountFeeCalFeeLevelPercent>();
		Byte calFeeLevelType = null;
		Float calFeePercent = null;
		Byte calFeeType = null;
		Byte feeType = null;
		if (!ObjectUtils.isEmpty(af)) {
			calFeeLevelType = ObjectUtils.isEmpty(af.getCalFeeLevelType()) ? null : af.getCalFeeLevelType();
			calFeePercent = ObjectUtils.isEmpty(af.getCalFeePercent()) ? null : af.getCalFeePercent().floatValue();
			calFeeType = ObjectUtils.isEmpty(af.getCalFeeType()) ? null : af.getCalFeeType();
			feeType = ObjectUtils.isEmpty(af.getFeeType()) ? null : af.getFeeType();
			if (!StringUtils.isEmpty(af.getCalFeeLevelMoney())) {
				List<AccountFeeCalFeeLevelMoney> calFeeLevelMoneyListTmp = null;
				try {
					calFeeLevelMoneyListTmp = jsonStr2ArrayList(af.getCalFeeLevelMoney(),
							AccountFeeCalFeeLevelMoney.class);
				} catch (IOException e) {
					log.error("AccountFeeServiceImpl.findByAccount将金额计费阶梯{}转换为对象时异常", af.getCalFeeLevelMoney());
				}
				if (!CollectionUtils.isEmpty(calFeeLevelMoneyListTmp)) {
					calFeeLevelMoneyList.addAll(calFeeLevelMoneyListTmp);
				}
			}
			if (!StringUtils.isEmpty(af.getCalFeeLevelPercent())) {
				List<AccountFeeCalFeeLevelPercent> calFeeLevelPercentListTmp = null;
				try {
					calFeeLevelPercentListTmp = jsonStr2ArrayList(af.getCalFeeLevelPercent(),
							AccountFeeCalFeeLevelPercent.class);
				} catch (IOException e) {
					log.error("AccountFeeServiceImpl.findByAccount将百分比计费阶梯{}转换为对象时异常", af.getCalFeeLevelPercent());
				}
				if (!CollectionUtils.isEmpty(calFeeLevelPercentListTmp)) {
					calFeeLevelPercentList.addAll(calFeeLevelPercentListTmp);
				}
			}
		}
		result.setCalFeeLevelMoneyList(calFeeLevelMoneyList);
		result.setCalFeeLevelPercentList(calFeeLevelPercentList);
		result.setCalFeeLevelType(calFeeLevelType);
		result.setCalFeePercent(calFeePercent);
		result.setCalFeeType(calFeeType);
		result.setFeeType(feeType);
		return result;
	}

	private <T> List<T> jsonStr2ArrayList(final String value, Class<T> targetClasses)
			throws JsonParseException, JsonMappingException, IOException {
		List<T> result = mapper.readValue(value,
				mapper.getTypeFactory().constructCollectionType(ArrayList.class, targetClasses));
		return result;
	}

	private void update(final Integer handicapId, final Integer accountId, final AccountFee4PlatUpdateReq requestBody)
			throws HandleException {
		StringRedisTemplate jedis = redisService.getYsfStringRedisTemplate();
		if (jedis != null) {
			JedisLock lock = new JedisLock(jedis, String.format(ACCOUNT_FEE_DISTRIBUTED_LOCK, accountId), 1000, 1000);
			try {
				if (lock.acquire()) {
					AccountFee af = getAccountFee(accountId);
					if (ObjectUtils.isEmpty(af)) {
						af = new AccountFee();
						af.setId(accountId);
						af.setHandicapId(handicapId);
						af.setAccountId(accountId);
						af.setCreateName(requestBody.getAdminName());
						af.setCreateTime(new Timestamp(System.currentTimeMillis()));
					}

					af.setCalFeeType(requestBody.getCalFeeType());
					af.setFeeType(requestBody.getFeeType());
					if (!ObjectUtils.isEmpty(requestBody.getCalFeePercent())) {
						af.setCalFeePercent(requestBody.getCalFeePercent());
					}
					if (!ObjectUtils.isEmpty(requestBody.getCalFeeLevelType())) {
						af.setCalFeeLevelType(requestBody.getCalFeeLevelType());
					}
					// 当使用阶梯式计费时，必须存在阶梯计费项目
					if (requestBody.getCalFeeType().intValue() == 1) {
						if (requestBody.getCalFeeLevelType() == 1) {
							List<AccountFeeCalFeeLevelMoney> calFeeLevelMoneyListTmp = null;
							if (!StringUtils.isEmpty(af.getCalFeeLevelMoney())) {
								try {
									calFeeLevelMoneyListTmp = jsonStr2ArrayList(af.getCalFeeLevelMoney(),
											AccountFeeCalFeeLevelMoney.class);
								} catch (IOException e) {
									log.error("AccountFeeServiceImpl.updateByPlat将金额计费阶梯{}转换为对象时异常",
											af.getCalFeeLevelMoney());
								}
							}
							if (CollectionUtils.isEmpty(calFeeLevelMoneyListTmp)) {
								throw new HandleException("修改失败，请先添加按金额计费的计费阶梯");
							}
						}
						if (requestBody.getCalFeeLevelType() == 0) {
							List<AccountFeeCalFeeLevelPercent> calFeeLevelPercentListTmp = null;
							if (!StringUtils.isEmpty(af.getCalFeeLevelPercent())) {
								try {
									calFeeLevelPercentListTmp = jsonStr2ArrayList(af.getCalFeeLevelPercent(),
											AccountFeeCalFeeLevelPercent.class);
								} catch (IOException e) {
									log.error("AccountFeeServiceImpl.updateByPlat将百分比计费阶梯{}转换为对象时异常",
											af.getCalFeeLevelPercent());
								}
							}
							if (CollectionUtils.isEmpty(calFeeLevelPercentListTmp)) {
								throw new HandleException("修改失败，请先添加按百分比计费的计费阶梯");
							}
						}
					}
					af.setUpdateName(requestBody.getAdminName());
					af.setUpdateTime(new Timestamp(System.currentTimeMillis()));
					af = centralizedUpdate(af);
					accountFee2Cache(af);
				} else {
					log.error("AccountFeeServiceImpl.update 修改手续费信息时未能获取到redis锁");
					throw new HandleException("修改失败，请稍后再试");
				}
			} catch (InterruptedException e) {
				log.error("AccountFeeServiceImpl.updateByPlat时产生InterruptedException异常", e);
				throw new HandleException("修改失败，请稍后再试");
			} finally {
				lock.release();
			}
		}
	}

	private void delCalFeeLevel(final Integer accountId,
			final AccountFee4PlatLevelDelReq requestBody) throws HandleException {
		StringRedisTemplate jedis = redisService.getYsfStringRedisTemplate();
		if (jedis != null) {
			JedisLock lock = new JedisLock(jedis, String.format(ACCOUNT_FEE_DISTRIBUTED_LOCK, accountId), 1000, 1000);
			try {
				if (lock.acquire()) {
					AccountFee af = getAccountFee(accountId);
					if (ObjectUtils.isEmpty(af)) {
						log.error("传入的bankType={} / account={}的通道尚未配置第三方下发手续费规则", requestBody.getBankType(),
								requestBody.getAccount());
						throw new HandleException(String.format("未知的 index =%s", requestBody.getIndex()));
					}
					af.setUpdateName(requestBody.getAdminName());
					af.setUpdateTime(new Timestamp(System.currentTimeMillis()));
					// 删除金额计费阶梯
					if (requestBody.getCalFeeLevelType() == 1) {
						List<AccountFeeCalFeeLevelMoney> calFeeLevelMoneyListTmp = null;
						if (!StringUtils.isEmpty(af.getCalFeeLevelMoney())) {
							try {
								calFeeLevelMoneyListTmp = jsonStr2ArrayList(af.getCalFeeLevelMoney(),
										AccountFeeCalFeeLevelMoney.class);
							} catch (IOException e) {
								log.error("AccountFeeServiceImpl.calFeeLevelAddByPlat将金额计费阶梯{}转换为对象时异常",
										af.getCalFeeLevelMoney());
							}
						}
						AccountFeeCalFeeLevelMoney remove = getByIndexId(requestBody.getIndex(),
								calFeeLevelMoneyListTmp);
						if (ObjectUtils.isEmpty(remove)) {
							log.error("从bankType={} / account={}的通道第三方下发手续费规则百分比阶梯中删除 index={}的数据时，未能找到具体的信息.现有数据为：{}",
									requestBody.getBankType(), requestBody.getAccount(), requestBody.getIndex(),
									af.getCalFeeLevelMoney());
							throw new HandleException("未知的 index 或者数据已经被删除");
						} else {
							calFeeLevelMoneyListTmp.remove(remove);
							af.setCalFeeLevelMoney(ObjectMapperUtils.serialize(calFeeLevelMoneyListTmp));
						}
					}
					// 删除百分比计费阶梯
					if (requestBody.getCalFeeLevelType() == 0) {
						List<AccountFeeCalFeeLevelPercent> calFeeLevelPercentListTmp = null;
						if (!StringUtils.isEmpty(af.getCalFeeLevelPercent())) {
							try {
								calFeeLevelPercentListTmp = jsonStr2ArrayList(af.getCalFeeLevelPercent(),
										AccountFeeCalFeeLevelPercent.class);
							} catch (IOException e) {
								log.error("AccountFeeServiceImpl.calFeeLevelAddByPlat将百分比计费阶梯{}转换为对象时异常",
										af.getCalFeeLevelPercent());
							}
						}
						AccountFeeCalFeeLevelPercent remove = getByIndexId(requestBody.getIndex(),
								calFeeLevelPercentListTmp);
						if (ObjectUtils.isEmpty(remove)) {
							log.error("从bankType={} / account={}的通道第三方下发手续费规则百分比阶梯中删除 index={}的数据时，未能找到具体的信息.现有数据为：{}",
									requestBody.getBankType(), requestBody.getAccount(), requestBody.getIndex(),
									af.getCalFeeLevelPercent());
							throw new HandleException("未知的 index 或者数据已经被删除");
						} else {
							calFeeLevelPercentListTmp.remove(remove);
							af.setCalFeeLevelPercent(ObjectMapperUtils.serialize(calFeeLevelPercentListTmp));
						}
					}
					af = centralizedUpdate(af);
					accountFee2Cache(af);
				} else {
					log.error("AccountFeeServiceImpl.calFeeLevelDelByPlat删除计费阶梯时未能获取到redis锁");
					throw new HandleException("删除失败，请稍后再试");
				}
			} catch (InterruptedException e) {
				log.error("AccountFeeServiceImpl.calFeeLevelDelByPlat时产生InterruptedException异常", e);
				throw new HandleException("删除失败，请稍后再试");
			} finally {
				lock.release();
			}
		}
	}
	
	/**
	 * 统一更新,并判断是否存在有效的下发规则，如果没有有效的下发规则，则清除 “我的设定” 需求 7994
	 * @param af
	 * @return
	 */
	private AccountFee centralizedUpdate(AccountFee af) {
		af =accountFeeRepository.saveAndFlush(af);
		try {
			AccountFeeConfig afc = af2afc(af);
			if(afc!=null && !afc.isEffect()) {
				AccountService accountService =applicationContext.getBean(AccountService.class);
				if(!ObjectUtils.isEmpty(accountService)) {
					//调用 删除他人设定的三方账号 接口
					accountService.releaseOtherSetup(af.getAccountId());
				}
			}
		}catch (Exception e) {
			log.error("centralizedUpdate 更新第三方账号下发规则后清除我的锁定时异常",e);
		}
		return af;
	}

	/**
	 * 从 exits 中获取属性index与入参index一致的元素
	 * 
	 * @param <T>
	 * @param index
	 * @param exits
	 * @return
	 */
	private <T extends AccountFeeCalFeeLevelBase> T getByIndexId(final Long index, final List<T> exits) {
		if (ObjectUtils.isEmpty(exits)) {
			return null;
		} else {
			T result = null;
			for (T t : exits) {
				if (index.equals(t.getIndex())) {
					result = t;
					break;
				}
			}
			return result;
		}
	}

	private void calFeeLevelAdd(final Integer handicapId, final Integer accountId,
			final AccountFee4PlatLevelAddReq requestBody) throws HandleException {
		Double reqMoneyBegin = requestBody.getMoneyBegin();
		Double reqMoneyEnd = requestBody.getMoneyEnd();
		StringRedisTemplate jedis = redisService.getYsfStringRedisTemplate();
		if (jedis != null) {
			JedisLock lock = new JedisLock(jedis, String.format(ACCOUNT_FEE_DISTRIBUTED_LOCK, accountId), 1000, 1000);
			try {
				if (lock.acquire()) {
					AccountFee af = getAccountFee(accountId);
					if (ObjectUtils.isEmpty(af)) {
						af = new AccountFee();
						af.setId(accountId);
						af.setHandicapId(handicapId);
						af.setAccountId(accountId);
						af.setCreateName(requestBody.getAdminName());
						af.setCreateTime(new Timestamp(System.currentTimeMillis()));
					}

					AccountFeeCalFeeLevelBase flag = null;

					// 添加金额计费阶梯
					if (requestBody.getCalFeeLevelType().intValue() == 1) {
						List<AccountFeeCalFeeLevelMoney> calFeeLevelMoneyListTmp = null;
						if (!StringUtils.isEmpty(af.getCalFeeLevelMoney())) {
							try {
								calFeeLevelMoneyListTmp = jsonStr2ArrayList(af.getCalFeeLevelMoney(),
										AccountFeeCalFeeLevelMoney.class);
							} catch (IOException e) {
								log.error("AccountFeeServiceImpl.calFeeLevelAddByPlat将金额计费阶梯{}转换为对象时异常",
										af.getCalFeeLevelMoney());
							}
						}
						if (calFeeLevelMoneyListTmp == null) {
							calFeeLevelMoneyListTmp = new ArrayList<AccountFeeCalFeeLevelMoney>();
						}
						AccountFeeCalFeeLevelMoney t = null;
						if (calFeeLevelMoneyListTmp.size() > 0) {
							t = getMixed(reqMoneyBegin, reqMoneyEnd, calFeeLevelMoneyListTmp);
						}
						if (ObjectUtils.isEmpty(t)) {
							AccountFeeCalFeeLevelMoney e = new AccountFeeCalFeeLevelMoney();
							e.setCreateName(requestBody.getAdminName());
							e.setCreateTime(System.currentTimeMillis());
							e.setFeeMoney(requestBody.getFeeMoney());
							e.setIndex(System.currentTimeMillis());
							e.setMoneyBegin(reqMoneyBegin);
							e.setMoneyEnd(reqMoneyEnd);
							calFeeLevelMoneyListTmp.add(e);
							Collections.sort(calFeeLevelMoneyListTmp, comparator);
							af.setCalFeeLevelMoney(ObjectMapperUtils.serialize(calFeeLevelMoneyListTmp));
						} else {
							flag = t;
						}
					}
					// 添加百分比计费阶梯
					if (requestBody.getCalFeeLevelType().intValue() == 0) {
						List<AccountFeeCalFeeLevelPercent> calFeeLevelPercentListTmp = null;
						if (!StringUtils.isEmpty(af.getCalFeeLevelPercent())) {
							try {
								calFeeLevelPercentListTmp = jsonStr2ArrayList(af.getCalFeeLevelPercent(),
										AccountFeeCalFeeLevelPercent.class);
							} catch (IOException e) {
								log.error("AccountFeeServiceImpl.calFeeLevelAddByPlat将百分比计费阶梯{}转换为对象时异常",
										af.getCalFeeLevelPercent());
							}
						}
						if (calFeeLevelPercentListTmp == null) {
							calFeeLevelPercentListTmp = new ArrayList<AccountFeeCalFeeLevelPercent>();
						}

						AccountFeeCalFeeLevelPercent t = null;
						if (calFeeLevelPercentListTmp.size() > 0) {
							t = getMixed(reqMoneyBegin, reqMoneyEnd, calFeeLevelPercentListTmp);
						}
						if (ObjectUtils.isEmpty(t)) {
							AccountFeeCalFeeLevelPercent e = new AccountFeeCalFeeLevelPercent();
							e.setCreateName(requestBody.getAdminName());
							e.setCreateTime(System.currentTimeMillis());
							e.setFeePercent(requestBody.getFeePercent());
							e.setIndex(System.currentTimeMillis());
							e.setMoneyBegin(reqMoneyBegin);
							e.setMoneyEnd(reqMoneyEnd);
							calFeeLevelPercentListTmp.add(e);
							Collections.sort(calFeeLevelPercentListTmp, comparator);
							af.setCalFeeLevelPercent(ObjectMapperUtils.serialize(calFeeLevelPercentListTmp));
						} else {
							flag = t;
						}
					}
					if (!ObjectUtils.isEmpty(flag)) {
						throw new HandleException(String.format("与现有规则：%s的金额范围重叠,不能新增", flag.getMoneyRangeStr()));
					}
					af.setUpdateName(requestBody.getAdminName());
					af.setUpdateTime(new Timestamp(System.currentTimeMillis()));
					af = centralizedUpdate(af);
					accountFee2Cache(af);
				} else {
					log.error("AccountFeeServiceImpl.calFeeLevelAddByPlat 修改手续费信息时未能获取到redis锁");
					throw new HandleException("新增失败，请稍后再试");
				}
			} catch (InterruptedException e) {
				log.error("AccountFeeServiceImpl.calFeeLevelAddByPlat时产生InterruptedException异常", e);
				throw new HandleException("新增失败，请稍后再试");
			} finally {
				lock.release();
			}
		}
	}

	/**
	 * 从现有的阶梯规则中获取与传入的 inBegin 和 inEnd 存在交集的数据
	 * 
	 * @param <T>
	 * @param inBegin
	 * @param inEnd
	 * @param exits
	 * @return
	 */
	private <T extends AccountFeeCalFeeLevelBase> T getMixed(final Double inBegin, final Double inEnd,
			final List<T> exits) {
		if (CollectionUtils.isEmpty(exits)) {
			return null;
		}
		T t = null;
		int i = 0;
		int j = exits.size();
		boolean isInfinity = Double.isInfinite(inEnd);
		do {
			T sfcflp = exits.get(i);
			boolean exitInfinity = Double.isInfinite(sfcflp.getMoneyEnd());
			// 传入结束金额是无穷大时，以下情况会出现重叠：
			// 1、现有结束金额也是无穷大 2、传入的开始金额小于现有结束金额
			if (isInfinity) {
				if (exitInfinity || inBegin.compareTo(sfcflp.getMoneyEnd()) < 0) {
					t = sfcflp;
					break;
				}
			} else {
				// 传入当结束金额不是无穷大时，以下情况会出现重叠：
				// 传入开始金额大于等于现有开始金额，传入结束金额小于等于现有结束金额，交叉
				if (inBegin.compareTo(sfcflp.getMoneyBegin()) >= 0 && inEnd.compareTo(sfcflp.getMoneyEnd()) <= 0) {
					t = sfcflp;
					break;
				}

				// 传入开始金额小于现有开始金额，传入结束金额大于现有开始金额，交叉
				if (inBegin.compareTo(sfcflp.getMoneyBegin()) < 0 && inEnd.compareTo(sfcflp.getMoneyBegin()) > 0) {
					t = sfcflp;
					break;
				}
				// 传入开始金额小于现有开始金额，传入结束金额大于现有结束金额，交叉
				if (inBegin.compareTo(sfcflp.getMoneyBegin()) < 0 && inEnd.compareTo(sfcflp.getMoneyEnd()) > 0) {
					t = sfcflp;
					break;
				}

				// 传入开始金额小于现有结束金额，传入结束金额大于现有结束金额，交叉
				if (inBegin.compareTo(sfcflp.getMoneyEnd()) < 0 && inEnd.compareTo(sfcflp.getMoneyEnd()) >= 0) {
					t = sfcflp;
					break;
				}
			}
			i++;
		} while (i <= j - 1 && ObjectUtils.isEmpty(t));
		return t;
	}

	/**
	 * 开始金额升序比较器
	 */
	private static MoneyBeginSortComparator comparator = new MoneyBeginSortComparator();

	/**
	 * 开始金额升序排列
	 * 
	 * @author Blake
	 *
	 */
	static class MoneyBeginSortComparator implements Comparator<AccountFeeCalFeeLevelBase> {
		@Override
		public int compare(AccountFeeCalFeeLevelBase o1, AccountFeeCalFeeLevelBase o2) {
			return o1.getMoneyBegin().equals(o2.getMoneyBegin()) ? 0
					: o1.getMoneyBegin().compareTo(o2.getMoneyBegin()) < 0 ? -1 : 1;
		}
	}

	@Override
	public AccountFeeConfig findByPlat(final String handicap, final String bankType, final String account)
			throws HandleException {
		BizAccount accountInfo = this.getAccountByHandicapAndBankTypeAndAccount(handicap, bankType, account);
		if (ObjectUtils.isEmpty(accountInfo)) {
			log.error("AccountFeeServiceImpl.findByPlat通过handicap={}、bankType={}、account={}查询账号信息时未能查询到具体结果", handicap,
					bankType, account);
			throw new HandleException(String.format("未知的商号信息：[提供商=%s,商号=%s]", bankType, account));
		}
		AccountFeeConfig result = this.findByAccount(accountInfo.getId());
		if (!ObjectUtils.isEmpty(result)) {
			result.setAccount(accountInfo.getAccount());
			result.setBankType(accountInfo.getBankType());
		}
		return result;
	}

	/**
	 * 分布式锁，加锁防止并发造成的数据更新错误
	 */
	private static final String ACCOUNT_FEE_DISTRIBUTED_LOCK = "ACCOUNT_FEE_DISTRIBUTED_LOCK_%s";

	@Transactional(rollbackOn = Exception.class, value = TxType.REQUIRED)
	@Override
	public void updateByPlat(@Valid final AccountFee4PlatUpdateReq requestBody) throws HandleException {
		if (requestBody.getCalFeeType().intValue() == 0) {
			Assert.isTrue(!ObjectUtils.isEmpty(requestBody.getCalFeePercent()), "calFeePercent不能为空");
		}
		if (requestBody.getCalFeeType().intValue() == 1) {
			Assert.isTrue(!ObjectUtils.isEmpty(requestBody.getCalFeeLevelType()), "calFeeLevelType不能为空");
		}
		BizAccount accountInfo = this.getAccountByHandicapAndBankTypeAndAccount(requestBody.getHandicap(),
				requestBody.getBankType(), requestBody.getAccount());
		if (ObjectUtils.isEmpty(accountInfo)) {
			log.error("AccountFeeServiceImpl.updateByPlat通过handicap={}、bankType={}、account={}查询账号信息时未能查询到具体结果",
					requestBody.getHandicap(), requestBody.getBankType(), requestBody.getAccount());
			throw new HandleException("未知的通道信息，请确保账号信息已经同步到出入款系统");
		}

		if (!Objects.equals(accountInfo.getStatus(), AccountStatus.StopTemp.getStatus())) {
			log.error("AccountFeeServiceImpl.updateByPlat时，账号状态为{}不能修改", accountInfo.getStatus());
			throw new HandleException("仅在停用状态下可以修改，请先在出入款系统停用该账号");
		}
		this.update(accountInfo.getHandicapId(), accountInfo.getId(), requestBody);
	}

	@Transactional(rollbackOn = Exception.class, value = TxType.REQUIRED)
	@Override
	public void calFeeLevelAddByPlat(@Valid final AccountFee4PlatLevelAddReq requestBody) throws HandleException {
		if (requestBody.getCalFeeLevelType().intValue() == 1) {
			Assert.isTrue(!ObjectUtils.isEmpty(requestBody.getFeeMoney()), "feeMoney不能为空");
		}
		if (requestBody.getCalFeeLevelType().intValue() == 0) {
			Assert.isTrue(!ObjectUtils.isEmpty(requestBody.getFeePercent()), "feePercent不能为空");
		}
		Assert.isTrue(requestBody.getMoneyBegin().compareTo(requestBody.getMoneyEnd()) < 0, "开始金额必须小于结束金额");
		BizAccount accountInfo = this.getAccountByHandicapAndBankTypeAndAccount(requestBody.getHandicap(),
				requestBody.getBankType(), requestBody.getAccount());
		if (ObjectUtils.isEmpty(accountInfo)) {
			log.error("AccountFeeServiceImpl.calFeeLevelAddByPlat通过handicap={}、bankType={}、account={}查询账号信息时未能查询到具体结果",
					requestBody.getHandicap(), requestBody.getBankType(), requestBody.getAccount());
			throw new HandleException("未知的通道信息，请确保账号信息已经同步到出入款系统");
		}
		this.calFeeLevelAdd(accountInfo.getHandicapId(), accountInfo.getId(), requestBody);
	}

	@Transactional(rollbackOn = Exception.class, value = TxType.REQUIRED)
	@Override
	public void calFeeLevelDelByPlat(@Valid final AccountFee4PlatLevelDelReq requestBody) throws HandleException {
		BizAccount accountInfo = this.getAccountByHandicapAndBankTypeAndAccount(requestBody.getHandicap(),
				requestBody.getBankType(), requestBody.getAccount());
		if (ObjectUtils.isEmpty(accountInfo)) {
			log.error("AccountFeeServiceImpl.calFeeLevelDelByPlat通过handicap={}、bankType={}、account={}查询账号信息时未能查询到具体结果",
					requestBody.getHandicap(), requestBody.getBankType(), requestBody.getAccount());
			throw new HandleException("未知的通道信息，请确保账号信息已经同步到出入款系统");
		}
		this.delCalFeeLevel(accountInfo.getId(), requestBody);
	}

	@Override
	public AccountFeeConfig findByAccountBaseInfo(final BizAccount accountBaseInfo) {
		Assert.isTrue(!ObjectUtils.isEmpty(accountBaseInfo), "accountBaseInfo不能为空");
		AccountFeeConfig result = findByAccount(accountBaseInfo.getId());
		if (!ObjectUtils.isEmpty(result)) {
			result.setAccount(accountBaseInfo.getAccount());
			result.setBankType(accountBaseInfo.getBankType());
		}
		return result;
	}

	/**
	 * 根据账号id查询第三方下发手续费规则
	 *
	 * @param base
	 */
	@Override
	public AccountFeeConfig findTh3FeeCfg(AccountBaseInfo base) {
		Objects.requireNonNull(base, "base不能为空");
		AccountFeeConfig result = findByAccount(base.getId());
		if (Objects.nonNull(result)) {
			result.setAccount(base.getAccount());
			result.setBankType(base.getBankType());
		}
		return result;
	}

	@Transactional(rollbackOn = Exception.class, value = TxType.REQUIRED)
	@Override
	public void update(final BizAccount accountBaseInfo, final String operationAdminName, final Byte feeType,
			final Byte calFeeType, final BigDecimal calFeePercent, final Byte calFeeLevelType) throws HandleException {
		Assert.isTrue(!ObjectUtils.isEmpty(accountBaseInfo), "accountBaseInfo不能为空");
		Assert.isTrue(!StringUtils.isEmpty(operationAdminName), "operationAdminName不能为空");
		Assert.isTrue(feeType != null && (feeType.intValue() == 0 || feeType.intValue() == 1), "feeType不能为空并且只能是0或者1");
		Assert.isTrue(calFeeType != null && (calFeeType.intValue() == 0 || calFeeType.intValue() == 1),
				"calFeeType不能为空并且只能是0或者1");
		if (calFeeType.intValue() == 0) {
			Assert.isTrue(!ObjectUtils.isEmpty(calFeePercent) && calFeePercent.compareTo(BigDecimal.ZERO) >= 0
					&& calFeePercent.compareTo(BigDecimal.ONE) < 0, "calFeePercent不能为空并且为大于等于0小于1的小数");
		}
		if (calFeeType.intValue() == 1) {
			Assert.isTrue(
					calFeeLevelType != null && (calFeeLevelType.intValue() == 0 || calFeeLevelType.intValue() == 1),
					"calFeeLevelType不能为空并且只能是0或者1");
		}

		if (!AccountStatus.StopTemp.getStatus().equals(accountBaseInfo.getStatus())) {
			throw new HandleException("账号停用是才可以进行修改。");
		}
		AccountFee4PlatUpdateReq requestBody = new AccountFee4PlatUpdateReq();
		requestBody.setHandicap(getHandicapCode(accountBaseInfo.getHandicapId()));
		requestBody.setBankType(accountBaseInfo.getBankType());
		requestBody.setAccount(accountBaseInfo.getAccount());
		requestBody.setAdminName(operationAdminName);
		requestBody.setFeeType(feeType);
		requestBody.setCalFeeType(calFeeType);
		requestBody.setCalFeePercent(calFeePercent);
		requestBody.setCalFeeLevelType(calFeeLevelType);
		this.update(accountBaseInfo.getHandicapId(), accountBaseInfo.getId(), requestBody);
	}

	@Transactional(rollbackOn = Exception.class, value = TxType.REQUIRED)
	@Override
	public void calFeeLevelAdd(final BizAccount accountBaseInfo, final String operationAdminName,
			final Byte calFeeLevelType, final Double moneyBegin, final Double moneyEnd, final BigDecimal feeMoney,
			final BigDecimal feePercent) throws HandleException {
		Assert.isTrue(!ObjectUtils.isEmpty(accountBaseInfo), "accountBaseInfo不能为空");
		Assert.isTrue(!StringUtils.isEmpty(operationAdminName), "operationAdminName不能为空");
		Assert.isTrue(calFeeLevelType != null && (calFeeLevelType.intValue() == 0 || calFeeLevelType.intValue() == 1),
				"calFeeLevelType不能为空并且只能是0或者1");
		Assert.isTrue(!ObjectUtils.isEmpty(moneyBegin), "moneyBegin不能为空");
		Assert.isTrue(!ObjectUtils.isEmpty(moneyEnd), "moneyEnd不能为空");
		Assert.isTrue(moneyEnd.compareTo(moneyBegin) > 0, "moneyEnd必须大于moneyBegin");
		if (calFeeLevelType.intValue() == 1) {
			Assert.isTrue(!ObjectUtils.isEmpty(feeMoney) && feeMoney.compareTo(BigDecimal.ZERO) >= 0,
					"feeMoney不能为空并且为大于等于0");
		}
		if (calFeeLevelType.intValue() == 0) {
			Assert.isTrue(!ObjectUtils.isEmpty(feePercent) && feePercent.compareTo(BigDecimal.ZERO) >= 0
					&& feePercent.compareTo(BigDecimal.ONE) < 0, "feePercent不能为空并且为大于等于0小于1的小数");
		}
		if (!AccountStatus.StopTemp.getStatus().equals(accountBaseInfo.getStatus())) {
			throw new HandleException("账号停用是才可以进行修改。");
		}
		AccountFee4PlatLevelAddReq requestBody = new AccountFee4PlatLevelAddReq();
		requestBody.setHandicap(getHandicapCode(accountBaseInfo.getHandicapId()));
		requestBody.setBankType(accountBaseInfo.getBankType());
		requestBody.setAccount(accountBaseInfo.getAccount());
		requestBody.setAdminName(operationAdminName);
		requestBody.setCalFeeLevelType(calFeeLevelType);
		requestBody.setMoneyBegin(moneyBegin);
		requestBody.setMoneyEnd(moneyEnd);
		requestBody.setFeeMoney(feeMoney);
		requestBody.setFeePercent(feePercent);
		this.calFeeLevelAdd(accountBaseInfo.getHandicapId(), accountBaseInfo.getId(), requestBody);
	}

	@Transactional(rollbackOn = Exception.class, value = TxType.REQUIRED)
	@Override
	public void calFeeLevelDel(final BizAccount accountBaseInfo, final String operationAdminName,
			final Byte calFeeLevelType, final Long indexId) throws HandleException {
		Assert.isTrue(!ObjectUtils.isEmpty(accountBaseInfo), "accountBaseInfo不能为空");
		Assert.isTrue(!StringUtils.isEmpty(operationAdminName), "operationAdminName不能为空");
		Assert.isTrue(calFeeLevelType != null && (calFeeLevelType.intValue() == 0 || calFeeLevelType.intValue() == 1),
				"calFeeLevelType不能为空并且只能是0或者1");
		Assert.isTrue(!ObjectUtils.isEmpty(indexId), "indexId不能为空");
		if (!AccountStatus.StopTemp.getStatus().equals(accountBaseInfo.getStatus())) {
			throw new HandleException("账号停用是才可以进行修改。");
		}
		AccountFee4PlatLevelDelReq requestBody = new AccountFee4PlatLevelDelReq();
		requestBody.setHandicap(getHandicapCode(accountBaseInfo.getHandicapId()));
		requestBody.setBankType(accountBaseInfo.getBankType());
		requestBody.setAccount(accountBaseInfo.getAccount());
		requestBody.setAdminName(operationAdminName);
		requestBody.setCalFeeLevelType(calFeeLevelType);
		requestBody.setIndex(indexId);
		this.delCalFeeLevel(accountBaseInfo.getId(), requestBody);
	}

	@Override
	public AccountFeeConfig findByAccountBaseInfo2(AccountBaseInfo accountBaseInfo) {
		Assert.isTrue(!ObjectUtils.isEmpty(accountBaseInfo), "accountBaseInfo不能为空");
		AccountFeeConfig result = findByAccount(accountBaseInfo.getId());
		if (!ObjectUtils.isEmpty(result)) {
			result.setAccount(accountBaseInfo.getAccount());
			result.setBankType(accountBaseInfo.getBankType());
		}
		return result;
	}

	@Override
	public AccountFeeCalResult calAccountFee2(AccountBaseInfo accountBaseInfo, BigDecimal inMoney)
			throws NoSuiteAccountFeeRuleException {
		log.debug("线程{} 尝试计算第三方下发手续费，账号id={}，计算金额={}", Thread.currentThread().getName(), accountBaseInfo.getId(),
				inMoney);
		AccountFeeConfig af = findByAccountBaseInfo2(accountBaseInfo);
		if (ObjectUtils.isEmpty(af.getFeeType())) {
			log.error("线程{} 尝试计算第三方下发手续费，账号id={}尚未配置第三方下发手续费规则", Thread.currentThread().getName(),
					accountBaseInfo.getId());
			throw new NoSuiteAccountFeeRuleException("尚未配置下发手续费规则！");
		}

		log.debug("线程{} 账号id={}的第三方手续费规则信息为:{}", Thread.currentThread().getName(), obj2JsonStr(af));
		log.debug("线程{} 账号id={}的第三方手续费收取方式为{},计算方式为{}", Thread.currentThread().getName(), accountBaseInfo.getId(),
				af.getFeeType().intValue() == 0 ? "从商户余额扣取手续费" : "从到账金额扣取手续费",
				af.getCalFeeType().intValue() == 0 ? "固定百分比" : "阶梯计费");
		if (af.getCalFeeType().intValue() == 1) {
			log.debug("线程{} 账号id={}的第三方手续费计算方式-阶梯计费使用{}", Thread.currentThread().getName(), accountBaseInfo.getId(),
					af.getCalFeeLevelType().intValue() == 0 ? "百分比" : "固定金额");
		}

		AccountFeeCalResult result = new AccountFeeCalResult();
		result.setAccount(af.getAccount());
		result.setBankType(af.getBankType());
		result.setCalMoney(inMoney);
		result.setFeeType(af.getFeeType());
		// 记录手续费计算结果
		BigDecimal fee = null;
		// 记录手续费计算规则
		String calDesc = null;

		// 手续费计算方式：0-固定百分比 1-阶梯计费
		if (af.getCalFeeType().intValue() == 0) {
			fee = inMoney.multiply(new BigDecimal(af.getCalFeePercent())).setScale(2, RoundingMode.HALF_UP);
			calDesc = String.format("%s商号%s对下发金额%s使用固定百分比%s计算手续费，计算所得%s", af.getBankType(), af.getAccount(), inMoney,
					af.getCalFeePercent(), fee);
		} else {
			Double inMoneyDouble = inMoney.doubleValue();
			// 百分比
			if (af.getCalFeeLevelType().intValue() == 0) {
				for (AccountFeeCalFeeLevelPercent a : af.getCalFeeLevelPercentList()) {
					if (a.getMoneyBegin().compareTo(inMoneyDouble) <= 0
							&& a.getMoneyEnd().compareTo(inMoneyDouble) >= 0) {
						fee = inMoney.multiply(a.getFeePercent()).setScale(2, RoundingMode.HALF_UP);
						calDesc = String.format("%s商号%s对下发金额%s使用按百分比计费阶梯规则%s,百分比%s计算手续费，计算所得%s", af.getBankType(),
								af.getAccount(), inMoney, a.getMoneyRangeStr(), a.getFeePercent(), fee);
						break;
					}
				}
				if (ObjectUtils.isEmpty(fee)) {
					log.debug("线程{} 账号id={}的第三方手续费规则-按百分比计费阶梯规则未能为金额{}匹配到具体规则", Thread.currentThread().getName(),
							inMoney);
					throw new NoSuiteAccountFeeRuleException("无适用规则");
				}
			} else {
				// 固定金额
				for (AccountFeeCalFeeLevelMoney a : af.getCalFeeLevelMoneyList()) {
					if (a.getMoneyBegin().compareTo(inMoneyDouble) <= 0
							&& a.getMoneyEnd().compareTo(inMoneyDouble) >= 0) {
						fee = a.getFeeMoney().setScale(2, RoundingMode.HALF_UP);
						calDesc = String.format("%s商号%s对下发金额%s使用按金额计费阶梯规则%s,费用%s，计算所得%s", af.getBankType(),
								af.getAccount(), inMoney, a.getMoneyRangeStr(), a.getFeeMoney(), fee);
						break;
					}
				}
				if (ObjectUtils.isEmpty(fee)) {
					log.debug("线程{} 账号id={}的第三方手续费规则-按金额计费阶梯规则未能为金额{}匹配到具体规则", Thread.currentThread().getName(),
							accountBaseInfo.getId(), inMoney);
					throw new NoSuiteAccountFeeRuleException("无适用规则");
				}
			}
		}

		result.setFee(fee);
		// 收费方式：0-从商户余额扣取手续费 1-从到账金额扣取手续费
		if (af.getFeeType().intValue() == 1) {
			result.setMoney(inMoney.subtract(fee));
			calDesc = calDesc.concat("。收费方式为从到账金额中获取");
		} else {
			result.setMoney(inMoney);
			calDesc = calDesc.concat("。收费方式为从商户余额扣取手续费");
		}
		result.setCalDesc(calDesc);
		return result;
	}

	@Override
	public AccountFeeCalResult calAccountFee(BizAccount accountBaseInfo, BigDecimal inMoney)
			throws NoSuiteAccountFeeRuleException {
		log.debug("线程{} 尝试计算第三方下发手续费，账号id={}，计算金额={}", Thread.currentThread().getName(), accountBaseInfo.getId(),
				inMoney);
		AccountFeeConfig af = findByAccountBaseInfo(accountBaseInfo);
		if (ObjectUtils.isEmpty(af.getFeeType())) {
			log.error("线程{} 尝试计算第三方下发手续费，账号id={}尚未配置第三方下发手续费规则", Thread.currentThread().getName(),
					accountBaseInfo.getId());
			throw new NoSuiteAccountFeeRuleException("尚未配置下发手续费规则！");
		}

		log.debug("线程{} 账号id={}的第三方手续费规则信息为:{}", Thread.currentThread().getName(), obj2JsonStr(af));
		log.debug("线程{} 账号id={}的第三方手续费收取方式为{},计算方式为{}", Thread.currentThread().getName(), accountBaseInfo.getId(),
				af.getFeeType().intValue() == 0 ? "从商户余额扣取手续费" : "从到账金额扣取手续费",
				af.getCalFeeType().intValue() == 0 ? "固定百分比" : "阶梯计费");
		if (af.getCalFeeType().intValue() == 1) {
			log.debug("线程{} 账号id={}的第三方手续费计算方式-阶梯计费使用{}", Thread.currentThread().getName(), accountBaseInfo.getId(),
					af.getCalFeeLevelType().intValue() == 0 ? "百分比" : "固定金额");
		}

		AccountFeeCalResult result = new AccountFeeCalResult();
		result.setAccount(af.getAccount());
		result.setBankType(af.getBankType());
		result.setCalMoney(inMoney);
		result.setFeeType(af.getFeeType());
		// 记录手续费计算结果
		BigDecimal fee = null;
		// 记录手续费计算规则
		String calDesc = null;

		// 手续费计算方式：0-固定百分比 1-阶梯计费
		if (af.getCalFeeType().intValue() == 0) {
			fee = inMoney.multiply(new BigDecimal(af.getCalFeePercent())).setScale(2, RoundingMode.HALF_UP);
			calDesc = String.format("%s商号%s对下发金额%s使用固定百分比%s计算手续费，计算所得%s", af.getBankType(), af.getAccount(), inMoney,
					af.getCalFeePercent(), fee);
		} else {
			Double inMoneyDouble = inMoney.doubleValue();
			// 百分比
			if (af.getCalFeeLevelType().intValue() == 0) {
				for (AccountFeeCalFeeLevelPercent a : af.getCalFeeLevelPercentList()) {
					if (a.getMoneyBegin().compareTo(inMoneyDouble) <= 0
							&& a.getMoneyEnd().compareTo(inMoneyDouble) >= 0) {
						fee = inMoney.multiply(a.getFeePercent()).setScale(2, RoundingMode.HALF_UP);
						calDesc = String.format("%s商号%s对下发金额%s使用按百分比计费阶梯规则%s,百分比%s计算手续费，计算所得%s", af.getBankType(),
								af.getAccount(), inMoney, a.getMoneyRangeStr(), a.getFeePercent(), fee);
						break;
					}
				}
				if (ObjectUtils.isEmpty(fee)) {
					log.debug("线程{} 账号id={}的第三方手续费规则-按百分比计费阶梯规则未能为金额{}匹配到具体规则", Thread.currentThread().getName(),
							inMoney);
					throw new NoSuiteAccountFeeRuleException("无适用规则");
				}
			} else {
				// 固定金额
				for (AccountFeeCalFeeLevelMoney a : af.getCalFeeLevelMoneyList()) {
					if (a.getMoneyBegin().compareTo(inMoneyDouble) <= 0
							&& a.getMoneyEnd().compareTo(inMoneyDouble) >= 0) {
						fee = a.getFeeMoney().setScale(2, RoundingMode.HALF_UP);
						calDesc = String.format("%s商号%s对下发金额%s使用按金额计费阶梯规则%s,费用%s，计算所得%s", af.getBankType(),
								af.getAccount(), inMoney, a.getMoneyRangeStr(), a.getFeeMoney(), fee);
						break;
					}
				}
				if (ObjectUtils.isEmpty(fee)) {
					log.debug("线程{} 账号id={}的第三方手续费规则-按金额计费阶梯规则未能为金额{}匹配到具体规则", Thread.currentThread().getName(),
							accountBaseInfo.getId(), inMoney);
					throw new NoSuiteAccountFeeRuleException("无适用规则");
				}
			}
		}

		result.setFee(fee);
		// 收费方式：0-从商户余额扣取手续费 1-从到账金额扣取手续费
		if (af.getFeeType().intValue() == 1) {
			result.setMoney(inMoney.subtract(fee));
			calDesc = calDesc.concat("。收费方式为从到账金额中获取");
		} else {
			result.setMoney(inMoney);
			calDesc = calDesc.concat("。收费方式为从商户余额扣取手续费");
		}
		result.setCalDesc(calDesc);
		return result;
	}

	@Override
	public List<Integer> filterNoEffectFeeConfig(List<Integer> thirdAccounts) {
		if (CollectionUtils.isEmpty(thirdAccounts)) {
			return new ArrayList<Integer>();
		}

		Map<Integer, AccountFeeConfig> sadfa = new HashMap<Integer, AccountFeeConfig>();
		Collection<String> multiGetKeys = new ArrayList<String>();
		thirdAccounts.stream().forEach(t -> multiGetKeys.add(String.valueOf(t)));
		
		List<Integer> noAfCacheAccountIdList = new ArrayList<Integer>();
		try {
			List<String> cacheValueList = redisService.getYsfStringRedisTemplate().opsForValue().multiGet(multiGetKeys);
			for (int i = 0, j = thirdAccounts.size(); i < j; i++) {
				String jsonStr = cacheValueList.get(i);
				if (!StringUtils.isEmpty(jsonStr)) {
					AccountFee af = deserialize(jsonStr, AccountFee.class);
					sadfa.put(thirdAccounts.get(i), af2afc(af));
				} else {
					noAfCacheAccountIdList.add(thirdAccounts.get(i));
				}
			}
		}catch (Exception e) {
			log.error("filterNoEffectFeeConfig 时异常",e);
		}

		if (!CollectionUtils.isEmpty(noAfCacheAccountIdList)) {
			List<AccountFee> List = accountFeeRepository.findAllById(noAfCacheAccountIdList);
			if (!CollectionUtils.isEmpty(List)) {
				for (AccountFee af2 : List) {
					sadfa.put(af2.getAccountId(), af2afc(af2));
					// 放到缓存中
					accountFee2Cache(af2);
				}
			}
		}

		return thirdAccounts.stream().filter(t -> {
			AccountFeeConfig afc = sadfa.get(t);
			return afc != null && afc.isEffect();
		}).collect(Collectors.toList());
	}

}
