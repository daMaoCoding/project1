/**
 *
 */
package com.xinbo.fundstransfer.utils.randutil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import com.xinbo.fundstransfer.service.RedisService;
import com.xinbo.fundstransfer.unionpay.ysf.util.YSFLocalCacheUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * 入款银行卡随机金额工具类
 *
 * @author blake
 *
 */
@Slf4j
@Component
public class InAccountRandUtil {

	@Autowired
	private RedisService redisService;
	@Autowired
	private YSFLocalCacheUtil ySFLocalCacheUtil;

	/**
	 * 获取入款银行卡对应入款金额的随机金额
	 *
	 * @see InAccountRandUtil#getRandomStr(Integer, String)
	 * @param inMoney       欲入金额，整数金额，不能为空
	 * @param bankInAccount 入款银行卡号，不能为空
	 * @return 无可用随机金额时，返回null .有可用金额时，返回具体金额
	 * @throws NoAvailableRandomException 无可用随机金额时产生该异常
	 */
	public BigDecimal getRandomStr(BigDecimal inMoney, String bankInAccount) throws NoAvailableRandomException {
		return getRandomStr(inMoney.intValue(), bankInAccount);
	}

	/**
	 * 获取入款银行卡对应入款金额的随机金额
	 *
	 * @see InAccountRandUtil#getRandomStr(Integer, String, String)
	 * @param inMoney       欲入金额
	 * @param bankInAccount 入款银行卡号
	 * @return 无可用随机金额时，返回null .有可用金额时，返回具体金额
	 * @throws NoAvailableRandomException 无可用随机金额时产生该异常
	 */
	public BigDecimal getRandomStr(Integer inMoney, String bankInAccount) throws NoAvailableRandomException {
		return getRandomStr(inMoney, bankInAccount, null);
	}

	/**
	 * 获取入款银行卡对应入款金额的随机金额
	 *
	 * @param inMoney       欲入金额
	 * @param bankInAccount 入款银行卡号
	 * @param userName      用户名，可以为空
	 * @return 无可用随机金额时，返回null .有可用金额时，返回具体金额
	 * @throws NoAvailableRandomException 无可用随机金额时产生该异常
	 */
	public BigDecimal getRandomStr(Integer inMoney, String bankInAccount, String userName,Long currentTime)
			throws NoAvailableRandomException {
		List<String> inAccountRandStrList = getAvailableRandStrList(inMoney, bankInAccount);
		log.debug("线程{}：银行卡{}对整数金额{}的可用随机数列表为：{}",Thread.currentThread().getName(),bankInAccount,inMoney,Arrays.toString(inAccountRandStrList.toArray()));
		// 遍历未锁定的随机数，尝试锁定随机数，锁定后直接返回该随机数
		if (!CollectionUtils.isEmpty(inAccountRandStrList)) {
			// 过滤用户锁定的随机金额
			if (!StringUtils.isEmpty(userName)) {
				Set<String> userLockedStr = getUserLockStr(bankInAccount,userName);
				log.debug("线程{}：用户{}对银行卡{}锁定的随机数为有：{}",Thread.currentThread().getName(),userName,bankInAccount,Arrays.toString(userLockedStr.toArray()));
				if(!CollectionUtils.isEmpty(userLockedStr)) {
					//移除用户两个小时内使用过的金额
					inAccountRandStrList.removeAll(userLockedStr);
				}
			}
			for (String moneyStr : inAccountRandStrList) {
				log.debug("线程{}：尝试使用银行卡{}的随机金额：{}",Thread.currentThread().getName(),bankInAccount,moneyStr);
				if (setBankMoneyCacheLock(moneyStr, bankInAccount, currentTime)) {
					log.debug("线程{}：成功占用银行卡{}的随机金额：{}",Thread.currentThread().getName(),bankInAccount,moneyStr);
					return new BigDecimal(moneyStr);
				}
			}
		}
		throw new NoAvailableRandomException();
	}

	/**
	 * 获取入款银行卡对应入款金额的随机金额
	 *
	 * @param inMoney       欲入金额
	 * @param bankInAccount 入款银行卡号
	 * @param userName      用户名，可以为空
	 * @return 无可用随机金额时，返回null .有可用金额时，返回具体金额
	 * @throws NoAvailableRandomException 无可用随机金额时产生该异常
	 */
	public BigDecimal getRandomStr(Integer inMoney, String bankInAccount, String userName)
			throws NoAvailableRandomException {
		return getRandomStr(inMoney, bankInAccount, userName, null);
	}

	/**
	 * 用户随机金额锁<br>
	 * zrand:userlock:用户名:银行卡号
	 */
	private static final String USER_BANKACCOUNT_LOCK ="zrand:userlock:%s:%s";

	/**
	 * 获取用户一定时间内锁定的金额
	 * @param bankInAccount
	 * @param userName
	 * @return
	 */
	private Set<String> getUserLockStr(String bankInAccount,String userName){
		Set<String> result = null;
		result = redisService.getYsfStringRedisTemplate().boundZSetOps(String.format(USER_BANKACCOUNT_LOCK, userName,bankInAccount)).rangeByScore(System.currentTimeMillis() - ySFLocalCacheUtil.getYSFQrCodeLockTime(), System.currentTimeMillis());
		log.debug("用户{}对银行卡{}已经锁定的金额有：{}",userName,bankInAccount,result==null?"null":Arrays.toString(result.toArray()));
		return result;
	}

	/**
	 * 设置用户一定时间内锁定的金额
	 * @param bankInAccount
	 * @param userName
	 * @return
	 */
	public void setUserLockStr(String bankInAccount,String userName,BigDecimal amount){
		Long currentTime = System.currentTimeMillis();
		String redisUserBankaccountLockKey = String.format(USER_BANKACCOUNT_LOCK, userName,bankInAccount);
		String key = moneyFormat(amount);
		Double score = new Double(currentTime);
		log.debug("用户金额锁{}增加值{}",redisUserBankaccountLockKey,String.format("{key=%s,score=%s}", key,score));
		redisService.getYsfStringRedisTemplate().boundZSetOps(redisUserBankaccountLockKey).add(key, score);
		//设置有效期为三个小时
		redisService.getYsfStringRedisTemplate().boundZSetOps(redisUserBankaccountLockKey).expireAt(new Timestamp(System.currentTimeMillis() + 5*60*60*1000L));
	}

	/**
	 * 获取银行卡可用的随机金额
	 *
	 * @param inMoney
	 * @param bankInAccount
	 * @return
	 */
	public List<String> getAvailableRandStrList(Integer inMoney, String bankInAccount) {
		// 获取随机数序列
		List<String> inAccountRandStrList = getRandStrList(bankInAccount, inMoney);
		// 获取已经锁定的随机数
		Set<String> lockedStr = getBankInAccountLockStr(bankInAccount, inMoney);
		// 移除已经被锁定的随机数
		if (!CollectionUtils.isEmpty(lockedStr)) {
			inAccountRandStrList.removeAll(lockedStr);
		}
		return inAccountRandStrList;
	}

	/**
	 * 获取入款银行卡对应入款金额的随机金额
	 *
	 * @see InAccountRandUtil#getRandomStr(Integer, List, List)
	 * @param inMoney       欲入金额，不能为空
	 * @param transferCards 转账卡列表，必传，并且size >0
	 * @param scanQrCards   扫描卡列表，必传，并且size >0
	 * @return
	 * @throws NoAvailableRandomException 无可用随机金额时产生该异常
	 */
	public InAccountRandUtilResponse getRandomStr(BigDecimal inMoney, Collection<String> transferCards,
												  Collection<String> scanQrCards) throws NoAvailableRandomException {
		return getRandomStr(inMoney.intValue(), transferCards, scanQrCards);
	}

	/**
	 * 获取入款银行卡对应入款金额的随机金额
	 *
	 * @param inMoney       欲入金额
	 * @param transferCards 转账卡列表，必传，并且size >0
	 * @param scanQrCards   扫描卡列表，必传，并且size >0
	 * @return 选择卡片结果，包括随机金额+转账卡卡号+和扫描卡卡号
	 * @throws NoAvailableRandomException 无可用随机金额时产生该异常
	 */
	public InAccountRandUtilResponse getRandomStr(Integer inMoney, Collection<String> transferCards,
												  Collection<String> scanQrCards) throws NoAvailableRandomException {
		if (!CollectionUtils.isEmpty(transferCards) && !CollectionUtils.isEmpty(scanQrCards)) {
			log.debug("线程{}：获取随机数，使用转账卡加扫码卡的形式",Thread.currentThread().getName());
			return getRandomStrByMultBank(inMoney, transferCards, scanQrCards);
		} else if ((!CollectionUtils.isEmpty(transferCards)) && CollectionUtils.isEmpty(scanQrCards)) {
			log.debug("线程{}：获取随机数，使用转账卡的形式",Thread.currentThread().getName());
			for (String bankAccount : transferCards) {
				try {
					BigDecimal money = getRandomStr(inMoney, bankAccount);
					InAccountRandUtilResponse result = new InAccountRandUtilResponse();
					result.setAmount(money);
					result.setTransferCardNum(bankAccount);
					return result;
				} catch (NoAvailableRandomException e) {
					continue;
				}
			}
		} else if ((!CollectionUtils.isEmpty(scanQrCards)) && CollectionUtils.isEmpty(transferCards)) {
			log.debug("线程{}：获取随机数，使用扫码卡的形式",Thread.currentThread().getName());
			for (String bankAccount : scanQrCards) {
				try {
					BigDecimal money = getRandomStr(inMoney, bankAccount);
					InAccountRandUtilResponse result = new InAccountRandUtilResponse();
					result.setAmount(money);
					result.setScanCardNum(bankAccount);
					return result;
				} catch (NoAvailableRandomException e) {
					continue;
				}
			}
		}
		throw new NoAvailableRandomException();
	}

	public InAccountRandUtilResponse getRandomStrByMultBank(Integer inMoney, Collection<String> transferCards,
															Collection<String> scanQrCards) {
		InAccountRandUtilResponse result = new InAccountRandUtilResponse();
		Collection<String> allInAccountList = new ArrayList<>();
		allInAccountList.addAll(transferCards);
		allInAccountList.addAll(scanQrCards);
		final ExecutorService exec = Executors.newFixedThreadPool(allInAccountList.size());
		Map<String, List<String>> availableRandStrMap = new HashMap<>();
		try {
			for (String bankInAccount : allInAccountList) {
				Callable<List<String>> call = new Callable<List<String>>() {
					@Override
					public List<String> call() throws Exception {
						return getAvailableRandStrList(inMoney, bankInAccount);
					}
				};
				List<String> availableStr = null;
				try {
					Future<List<String>> future = exec.submit(call);
					availableStr = future.get();
				} catch (Exception e) {
					log.error("获取银行卡{} 整数金额{}的可用随机数时产生异常，异常信息{}", bankInAccount, inMoney, e.getMessage());
					availableStr = null;
				}
				if (!CollectionUtils.isEmpty(availableStr)) {
					availableRandStrMap.put(bankInAccount, availableStr);
				}
			}
		} finally {
			exec.shutdown();
		}
		for (String transferBankAccount : transferCards) {
			List<String> transferBankAvailable = availableRandStrMap.get(transferBankAccount);
			log.debug("线程{}：转账卡{}的可用随机数有：{}",Thread.currentThread().getName(),transferBankAccount,Arrays.toString(transferBankAvailable.toArray()));
			if (CollectionUtils.isEmpty(transferBankAvailable)) {
				continue;
			}
			for (String scanBankAccount : scanQrCards) {
				List<String> scanBankAvailable = availableRandStrMap.get(scanBankAccount);
				log.debug("线程{}：扫描卡{}的可用随机数有：{}",Thread.currentThread().getName(),scanBankAccount,Arrays.toString(scanBankAvailable.toArray()));
				if (CollectionUtils.isEmpty(scanBankAvailable)) {
					continue;
				}
				List<String> tmpTransferBankAvailable = new ArrayList<>();
				tmpTransferBankAvailable.addAll(transferBankAvailable);
				// 判断转账卡与扫描卡的随机金额是否有交集
				// boolean retailFlag = tmpTransferBankAvailable.retainAll(scanBankAvailable);
				tmpTransferBankAvailable.retainAll(scanBankAvailable);
				log.debug("线程{}：转账卡{}与扫描卡{}的可用随机数交集有：{}",Thread.currentThread().getName(),transferBankAccount,scanBankAccount,Arrays.toString(tmpTransferBankAvailable.toArray()));
				for (String randStrWillLock : tmpTransferBankAvailable) {
					boolean transferFlag = mulitLockStr(randStrWillLock, transferBankAccount, scanBankAccount);
					log.debug("线程{}：转账卡与扫描卡同时尝试锁定随机数{},结果{}",Thread.currentThread().getName(),randStrWillLock,transferFlag);
					if (transferFlag) {
						result.setAmount(new BigDecimal(randStrWillLock));
						result.setTransferCardNum(transferBankAccount);
						result.setScanCardNum(scanBankAccount);
						log.debug("线程{}：转账卡与扫描卡同时锁定随机数{}成功，返回转账卡{},扫码卡{}。",Thread.currentThread().getName(),randStrWillLock,transferBankAccount,scanBankAccount);
						return result;
					}
				}
			}
		}
		return result;
	}

	/**
	 * 两张卡同时锁定一个金额
	 *
	 * @param randStrWillLock
	 * @param transferBankAccount
	 * @param scanBankAccount
	 * @return
	 */
	private boolean mulitLockStr(String randStrWillLock, String transferBankAccount, String scanBankAccount) {
		log.debug("线程{}：{} 和 {}尝试同时锁定金额：{}", Thread.currentThread().getName(), transferBankAccount, scanBankAccount,
				randStrWillLock);
		boolean result = true;
		final ExecutorService exec = Executors.newFixedThreadPool(2);
		Long currentTime = System.currentTimeMillis();
		List<String> lockSuccessAccount = new ArrayList<>();
		try {
			for (String bankAccount : Arrays.asList(transferBankAccount, scanBankAccount)) {
				Callable<String> call = new Callable<String>() {
					@Override
					public String call() throws Exception {
						if (setBankMoneyCacheLock(randStrWillLock, bankAccount, currentTime)) {
							return bankAccount;
						}
						return null;
					}
				};
				Future<String> future = exec.submit(call);
				try {
					String account = future.get(1000, TimeUnit.MILLISECONDS);
					if (account != null) {
						lockSuccessAccount.add(account);
					}
				} catch (Exception e) {
					if (e instanceof java.util.concurrent.TimeoutException) {
						log.error("线程{}：尝试锁定金额时超时", Thread.currentThread().getName());
					} else {
						log.error("线程{}：尝试锁定金额时异常，异常信息：{}", Thread.currentThread().getName(), e.getMessage());
					}
				}
			}
		} finally {
			exec.shutdown();
		}
		if (ObjectUtils.isEmpty(lockSuccessAccount)) {
			result = false;
		}
		if (!lockSuccessAccount.contains(transferBankAccount) && lockSuccessAccount.contains(scanBankAccount)) {
			log.error("线程{}：金额：{}，{} 锁定成功，{}锁定失败", Thread.currentThread().getName(), randStrWillLock, scanBankAccount,
					transferBankAccount);
			// 释放锁定的金额
			String daKey = getCurrentBankQrCacheLockKey(scanBankAccount);
			String subKey = moneyFormat(randStrWillLock);
			StringRedisTemplate jedis = redisService.getYsfStringRedisTemplate();
			if (jedis != null) {
				Double score = jedis.boundZSetOps(daKey).score(subKey);
				if (score != null && score.equals(new Double(currentTime))) {
					jedis.boundZSetOps(daKey).remove(subKey);
					log.debug("卡{} 锁定金额：{}失败，释放卡{}锁定的金额", scanBankAccount, randStrWillLock, transferBankAccount);
				}
			}
		}
		if (lockSuccessAccount.contains(transferBankAccount) && !lockSuccessAccount.contains(scanBankAccount)) {
			log.error("线程{}：金额：{}，{} 锁定成功，{}锁定失败", Thread.currentThread().getName(), randStrWillLock,
					transferBankAccount, scanBankAccount);
			// 释放锁定的金额
			String daKey = getCurrentBankQrCacheLockKey(transferBankAccount);
			String subKey = moneyFormat(randStrWillLock);
			StringRedisTemplate jedis = redisService.getYsfStringRedisTemplate();
			if (jedis != null) {
				Double score = jedis.boundZSetOps(daKey).score(subKey);
				if (score != null && score.equals(new Double(currentTime))) {
					jedis.boundZSetOps(daKey).remove(subKey);
					log.debug("卡②{} 锁定金额：{}失败，释放卡①{}锁定的金额", scanBankAccount, randStrWillLock, transferBankAccount);
				}
			}
		}

		log.debug("线程{}：同时锁定结果：{}", Thread.currentThread().getName(), result);
		return result;
	}

	/**
	 * 获取已经锁定金额
	 *
	 * @param bankInAccount
	 * @param inMoney
	 * @return
	 */
	private Set<String> getBankInAccountLockStr(String bankInAccount, Integer inMoney) {
		// 移除已经被锁定的
		String daKey = getCurrentBankQrCacheLockKey(bankInAccount);
		StringRedisTemplate jedis = redisService.getYsfStringRedisTemplate();
		if (jedis != null) {
			jedis.opsForZSet().removeRangeByScore(daKey, 0, System.currentTimeMillis() - ySFLocalCacheUtil.getYSFQrCodeLockTime());
			Set<String> lockStrSet = jedis.opsForZSet().rangeByScore(daKey,
					new Double(System.currentTimeMillis() - ySFLocalCacheUtil.getYSFQrCodeLockTime()),
					new Double(System.currentTimeMillis() + TWELVE_HOURS_MILLISECOND));
			return lockStrSet;
		} else {
			return new HashSet<>();
		}
	}

	/**
	 * 银行卡随机金额分布式锁 <br>
	 * zrand_lock_银行卡号_随机金额
	 */
	public static final String BANK_ACCOUNT_RAND_DISTRIBUTED_LOCK = "zrand:lock_%s_%s";

	/**
	 * 银行卡号使用随机金额的时间 <br>
	 * zrand_lock_银行卡号
	 */
	private static final String BANK_ACCOUNT_RAND_LOCK = "zrand:lock:%s";

	/**
	 * 12小时毫秒值
	 */
	private static final Long TWELVE_HOURS_MILLISECOND = 12 * 60 * 60 * 1000L;

	/**
	 * 设置金额锁
	 *
	 * @param money       具体金额，带小数
	 * @param bankAccount 入款银行卡号
	 * @param currentTime 锁定时间戳，redis中的score.为空默认取当前时间戳
	 * @return
	 */
	private boolean setBankMoneyCacheLock(String money, String bankAccount, Long currentTime) {
		StringRedisTemplate jedis = redisService.getYsfStringRedisTemplate();
		if (jedis != null) {
			try {
				JedisLock lock = new JedisLock(jedis, String.format(BANK_ACCOUNT_RAND_DISTRIBUTED_LOCK, bankAccount, money),
						1000, 5000);
				try {
					if (lock.acquire()) {
						log.debug("获得锁{}", String.format(BANK_ACCOUNT_RAND_DISTRIBUTED_LOCK, bankAccount, money));
						String daKey = getCurrentBankQrCacheLockKey(bankAccount);
						jedis.boundZSetOps(daKey).removeRangeByScore(0, System.currentTimeMillis() - ySFLocalCacheUtil.getYSFQrCodeLockTime());
						String subKey = moneyFormat(money);

						Double score = jedis.boundZSetOps(daKey).score(subKey);
						if (score != null && score.compareTo(new Double(
								System.currentTimeMillis() - ySFLocalCacheUtil.getYSFQrCodeLockTime())) > 0) {
							log.debug(String.format("设置金额锁时，%s最近使用过该随机金额 %s ，使用时间 %s", bankAccount, subKey,new Timestamp(score.longValue())));
							return false;
						}
						log.debug(String.format("设置设备金额缓存%s，增加值%s", daKey, subKey));
						jedis.boundZSetOps(daKey).add(subKey, currentTime != null ? currentTime : System.currentTimeMillis());
						jedis.boundHashOps(daKey).expire(24L, TimeUnit.HOURS);
						//mark blake 2019-05-15 某一个银行卡的随机金额序列改用 hash 存储
						String key = getInAccountRandStrKey(bankAccount);
						String moneyIntKey = String.valueOf(new BigDecimal(money).intValue());
						Object moneyRandStrListObject = jedis.boundHashOps(key).get(moneyIntKey);
						if(!ObjectUtils.isEmpty(moneyRandStrListObject)) {
							try {
								List<String> moneyRandStrList = ObjectMapperUtils.deserialize((String)moneyRandStrListObject, ArrayList.class);
								if(!ObjectUtils.isEmpty(moneyRandStrList)) {
									//移除重发值
									moneyRandStrList.remove(subKey);
									//添加到最后
									moneyRandStrList.add(subKey);
									jedis.boundHashOps(key).put(moneyIntKey, ObjectMapperUtils.serialize(moneyRandStrList));
									jedis.boundHashOps(key).expire(24L, TimeUnit.HOURS);
								}
							}catch (Exception e) {
								log.error("成功锁定随机金额后，将随机金额移到随机序列末尾时异常",e);
							}
						}

						// 发送redis消息，告知其他服务器 bankAccount 的使用时间
						redisService.convertAndSend(RedisTopics.YSF_BANK_ACCOUNT_USE_TIME, "{\"bankAccount\":\"" + bankAccount
								+ "\",\"time\":" + System.currentTimeMillis() + "}");
						return true;
					} else {
						return false;
					}
				} finally {
					lock.release();
				}
			} catch (InterruptedException e) {
				return false;
			}
		}
		return false;
	}

	/**
	 * 获取收款银行卡金额锁缓存key
	 *
	 * @param bankAccount
	 * @return "zrand:lock:bankAccount"
	 */
	public static String getCurrentBankQrCacheLockKey(String bankAccount) {
		return String.format(BANK_ACCOUNT_RAND_LOCK, bankAccount);
	}

	/**
	 * 回收随机数 <br>
	 * 当入款单订单确认或者取消时，释放随机数
	 */
	public void recycleRandNum(String bankAccount, BigDecimal orderMoney) {
		/**
		 * 从 zset 缓存 中移除orderMoney
		 */
		String key = getCurrentBankQrCacheLockKey(bankAccount);
		StringRedisTemplate jedis = redisService.getYsfStringRedisTemplate();
		if (jedis != null) {
			jedis.boundZSetOps(key).remove(moneyFormat(orderMoney));
		}
	}

	/**
	 * 超时时释放占用随机数
	 * @param bankAccount
	 * @param finalAmountStr
	 * @param currentTime
	 */
	public void recycleRandNumByTimeOut(String bankAccount, String finalAmountStr, Long currentTime) {
		this.recycleRandNumByTimeOut(bankAccount, new BigDecimal(finalAmountStr), currentTime);
	}

	/**
	 * 下发app超时时释放占用随机数
	 * @param bankAccount
	 * @param finalAmountStr
	 * @param currentTime
	 */
	public void recycleRandNumByTimeOut(String bankAccount, BigDecimal finalAmount, Long currentTime) {
		StringRedisTemplate jedis = redisService.getYsfStringRedisTemplate();
		if (jedis != null) {
			JedisLock lock = new JedisLock(jedis, String.format(BANK_ACCOUNT_RAND_DISTRIBUTED_LOCK, bankAccount, finalAmount),
					1000, 1000);
			try {
				if (lock.acquire()) {
					log.debug("获得锁{}", String.format(BANK_ACCOUNT_RAND_DISTRIBUTED_LOCK, bankAccount, finalAmount));
					String daKey = getCurrentBankQrCacheLockKey(bankAccount);
					String subKey = moneyFormat(finalAmount);

					Double score = jedis.boundZSetOps(daKey).score(subKey);
					if (score != null && score.equals(new Double(currentTime))) {
						log.error(String.format("超时时释放占用随机数,设置%s删除 %s", bankAccount, subKey));
						jedis.boundZSetOps(daKey).remove(subKey);
					}
				}
			} catch (InterruptedException e) {
				log.error("app超时时释放占用随机数时异常",e);
			} finally {
				lock.release();
			}
		}
	}


	/**
	 * 锁定随机数 <br>
	 * 当收到流水，但是没有对应的入款单时，锁定该随机数，不允许使用 <br>
	 * 锁定时间，与平台银行入款那边一直，12小时
	 */
	public void lockRandNum(String bankAccount, BigDecimal orderMoney) {
		/**
		 * 1、使用 redis 的 hash 表来锁住随机金额 2、zset 以 bankAccount为外键，随机金额为key ,锁定时间为当前时间+12小时
		 */
		String key = getCurrentBankQrCacheLockKey(bankAccount);
		StringRedisTemplate jedis = redisService.getYsfStringRedisTemplate();
		if (jedis != null) {
			jedis.boundZSetOps(key).add(moneyFormat(orderMoney), System.currentTimeMillis() + TWELVE_HOURS_MILLISECOND);
		}
	}

	/**
	 * 金额格式话
	 *
	 * <pre>
	 * 因为存在如下差异，所以统一格式，保留两位小数
	 * BigDecimal b = new BigDecimal(60);  b.toString() => 60
	 * BigDecimal b2 = new BigDecimal("60.00");  b2.toString() => 60.00
	 * BigDecimal b3 = new BigDecimal("60.0");  b3.toString() => 60.0
	 * </pre>
	 *
	 * @return
	 */
	public static String moneyFormat(BigDecimal bigDecimal) {
		return bigDecimal == null ? "" : df.format(bigDecimal.setScale(2, RoundingMode.HALF_UP));
	}

	public static String moneyFormat(String bigDecimal) {
		return moneyFormat(new BigDecimal(bigDecimal));
	}

	private static final String INACCOUNT_RANDSTR = "zrand:randstr:%s";
	/**
	 * 获取随机数列表
	 *
	 * @param bankInAccount
	 * @param inMoney
	 * @return
	 */
	private List<String> getRandStrList(String bankInAccount, Integer inMoney) {
		List<String> result = new ArrayList<>();
		StringRedisTemplate jedis = redisService.getYsfStringRedisTemplate();
		if(jedis !=null) {
			String moneyKey = String.valueOf(inMoney);
			String key = getInAccountRandStrKey(bankInAccount);
			Object tmpListObj = jedis.boundHashOps(key).get(moneyKey);
			if(ObjectUtils.isEmpty(tmpListObj)) {
				JedisLock lock = new JedisLock(jedis, key.concat("_lock"),
						1000, 1000);
				try {
					if(lock.acquire()) {
						tmpListObj = jedis.boundHashOps(key).get(moneyKey);
						if(ObjectUtils.isEmpty(tmpListObj)) {
							List<String> randStr = new ArrayList<>();
							for (String str : str01_99) {
								randStr.add(String.format("%s.%s", inMoney, str));
							}
							Collections.shuffle(randStr);
							jedis.boundHashOps(key).put(moneyKey, ObjectMapperUtils.serialize(randStr));
						}
					}
				} catch (InterruptedException e) {
					log.error("获取银行卡随机序列时失败{}",bankInAccount);
				}
			}
			if(ObjectUtils.isEmpty(tmpListObj)) {
				tmpListObj = jedis.boundHashOps(key).get(moneyKey);
			}

			if(!ObjectUtils.isEmpty(tmpListObj)) {
				List<String> tmpList = ObjectMapperUtils.deserialize((String)tmpListObj, ArrayList.class);
				if(!ObjectUtils.isEmpty(tmpList)) {
					result.addAll(tmpList);
				}
			}
		}else {
			for (String str : str01_99) {
				result.add(String.format("%s.%s", inMoney, str));
			}
			Collections.shuffle(result);
		}
		return result;
	}

	private String getInAccountRandStrKey(String bankInAccount) {
		return String.format(INACCOUNT_RANDSTR, bankInAccount);
	}

	// 01-99的随机数
	private static List<String> str01_99 = new ArrayList<>();
	static {
		for (int i = 1; i < 100; i++) {
			str01_99.add(String.format("%s", i < 10 ? ("0" + i) : i));
		}
	}

	/**
	 * 金额统一保留 2位小数
	 */
	private static DecimalFormat df = new DecimalFormat("0.00");
}
