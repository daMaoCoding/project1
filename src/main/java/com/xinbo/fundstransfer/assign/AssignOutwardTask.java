package com.xinbo.fundstransfer.assign;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.assign.entity.OutAccount;
import com.xinbo.fundstransfer.assign.entity.OutInSameTime;
import com.xinbo.fundstransfer.assign.entity.OutInTurn;
import com.xinbo.fundstransfer.assign.entity.OutOnly;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.domain.entity.BizAccountRebate;
import com.xinbo.fundstransfer.domain.entity.BizLevel;
import com.xinbo.fundstransfer.domain.entity.BizOutwardRequest;
import com.xinbo.fundstransfer.domain.entity.BizOutwardTask;
import com.xinbo.fundstransfer.domain.enums.OutwardTaskStatus;
import com.xinbo.fundstransfer.domain.enums.TaskType;
import com.xinbo.fundstransfer.domain.enums.UserProfileKey;
import com.xinbo.fundstransfer.domain.enums.OutWardPayType;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.pojo.AllocatingOutwardTask;
import com.xinbo.fundstransfer.domain.repository.AccountRebateRepository;
import com.xinbo.fundstransfer.domain.repository.OutwardTaskRepository;
import com.xinbo.fundstransfer.runtime.MemCacheUtils;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.AllocateIncomeAccountService;
import com.xinbo.fundstransfer.service.AllocateOutwardTaskService;
import com.xinbo.fundstransfer.service.AllocateTransService;
import com.xinbo.fundstransfer.service.AsignFailedTaskService;
import com.xinbo.fundstransfer.service.LevelService;
import com.xinbo.fundstransfer.service.OutwardRequestService;
import com.xinbo.fundstransfer.service.RebateUserActivityService;
import com.xinbo.fundstransfer.service.RedisService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AssignOutwardTask {
	/**
	 * 常量：出款任务分配服务停止时间段</br>
	 * HALT_SERVICE_START_TIME 开始时间毫秒数；</br>
	 * HALT_SERVICE_END_TIME 结束时间毫秒数</br>
	 */
	private static volatile Long HALT_SERVICE_START_TIME = null, HALT_SERVICE_END_TIME = null;
	/**
	 * 上次分配分数
	 */
	private volatile static double LAST_ALLOCATE_SCORE = 0D;
	/**
	 * 上次同步未出款任务的时间
	 */
	private volatile static long LAST_SYNC_TASK_TIME = 0L;
	
	/**
	 * 上次清理任务的时间
	 */
	private volatile static long LAST_CLEAN_TASK_TIME = 0L;
	
	private static final long ONE_DAYS_MILIS = 2 * 24 * 60 * 60 * 1000;
	/**
	 * 已分配目标对象
	 */
	private static final Cache<Integer, Integer> atedTarget = CacheBuilder.newBuilder()
			.expireAfterWrite(1, TimeUnit.MINUTES).build();

	/**
	 * 有钱的入款卡，用于预留出款任务 key: accountId; value: 入款卡<->任务
	 */
	private static final Cache<Integer, Pair<OutAccount,AllocatingOutwardTask>> inAccountWithMoney = CacheBuilder.newBuilder()
			.expireAfterWrite(10, TimeUnit.MINUTES).build();
	/**
	 * 缓存的入款卡和最近一次任务缓存的时间
	 */
	private final Map<Integer, Long> inAccountWithMoneyUsed = new HashMap<Integer, Long>();
	
	@Autowired
	AllocateIncomeAccountService incomeAccountAllocateService;
	@Autowired
	private RedisService redisService;
	@Autowired
	private AllocateTransService allocateTransService;
	@Autowired
	private AccountService accountService;
	@Autowired
	private OutwardTaskRepository outwardTaskRepository;
	@Autowired
	private AccountRebateRepository accountRebateRepository;
	@Autowired
	private LevelService levelService;
	@Autowired
	private OutwardRequestService outwardRequestService;
	@Autowired
	private AsignFailedTaskService asignFailedTaskService;
	@Autowired
	private AvailableCardCache availableCardCache;
	@Autowired
	private OtherCache otherCache;
	@Autowired @Lazy
	private AllocateOutwardTaskService allocOutwardTaskSer;
	@Autowired
	private RebateUserActivityService rebateUserActivityService;
	private ExecutorService executor = Executors.newFixedThreadPool(20);
	 
	@Scheduled(fixedDelay = 100)
	public void assign() throws InterruptedException {
		// 1.检测 是否具有 出款任务分配权限
		if (!incomeAccountAllocateService.checkHostRunRight()) {
			log.trace("AllocOTask >> the host has no right to allocate. {}", CommonUtils.getInternalIp());
			Thread.sleep(5000L);
			return;
		}
		// 检测：是否停止出款任务分配；
		checkHaltService();
		// 2.检测 是否执行 出款任务分配
		// 检测 DUPLICATE_ALLOCATE 在Redis中zset中score 与上次执行的score是否相同<br/>
		// 相同：不执行；不相同 执行
		Double allocateScore = redisService.getStringRedisTemplate()
				.boundZSetOps(RedisKeys.ALLOCATING_NEW_OUTWARD_TARGET).score(Constants.DUPLICATE_ALLOCATE);
		if (allocateScore != null && allocateScore == LAST_ALLOCATE_SCORE) {
			log.debug("AllocOTask >> no need to allocate , due to the same score with last time. {}", allocateScore);
			Thread.sleep(2000L);
			return;
		}
		LAST_ALLOCATE_SCORE = allocateScore == null ? 0D : allocateScore;
		if (allocateScore == null) {
			redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOCATING_NEW_OUTWARD_TARGET)
					.add(Constants.DUPLICATE_ALLOCATE, LAST_ALLOCATE_SCORE);
		}
		pushTaskToRedis();
		int len = lorder();
		if (len == 0) {
			log.debug("AllocOTask >> no task to allocate .");
			Thread.sleep(2000L);
			return;
		}

		//1. 找出所有出款任务
		String tasksStr = lpop(len);
		if(StringUtils.isEmpty(tasksStr)) {
			Thread.sleep(2000L);
			return;
		}
		List<String> taskStrs = Arrays.asList(tasksStr.split(";"));
		Map<String, List<AllocatingOutwardTask>> taskMap = taskStrs.stream().map(AllocatingOutwardTask::new)
				.filter(p -> checkTaskIsValid(p)).collect(Collectors.groupingBy(s -> {
					if (null != s) {
						AllocatingOutwardTask task = (AllocatingOutwardTask) s;
						if (Objects.equals(task.getManualOut(), 0)) {
							Integer currLevel;
							if (task.getTaskType() == 1) {
								currLevel = Constants.INNER;
							} else {
								BizLevel level = levelService.findFromCache(task.getLevel());
								currLevel = level == null || level.getCurrSysLevel() == null ? Constants.OUTTER
										: level.getCurrSysLevel();
							}
							return task.getZone() + "-" + currLevel + "-" + task.getManualOut();
						} else {
							return task.getZone() + "-" + task.getManualOut();
						}
					} else {
						return "0-0-0";
					}
				}));
		

		//2. 用卡去出配对好的任务
		CountDownLatch latch = new CountDownLatch(taskMap.size());
		for(Entry<String, List<AllocatingOutwardTask>> entry : taskMap.entrySet()) {
			executor.submit(() -> subAssign(entry.getKey(), entry.getValue(), latch));
		}
		latch.await();
	}

	private void subAssign(String category, List<AllocatingOutwardTask> taskList, CountDownLatch latch) {
		try {
			Map<Long, AllocatingOutwardTask> allocatedTaskMap = new HashMap<Long, AllocatingOutwardTask>();
			Map<Long, AllocatingOutwardTask> notAllocatedTaskMap = new HashMap<Long, AllocatingOutwardTask>();
			long currentTime = System.currentTimeMillis();
			int taskNum = 0;
			for (AllocatingOutwardTask tasking : taskList) {
				AllocatingOutwardTask ated = null;
				Boolean peerTrans = null;// 同行转账
				boolean maintain = false;// 银行维护
				try {
					log.debug("subAssign>> Begin step1. task {}", tasking.getTaskId());
					BizLevel level = levelService.findFromCache(tasking.getLevel());
					Integer l = tasking.getTaskType() != TaskType.MemberOutTask.getTypeId() ? Constants.INNER
							: level.getCurrSysLevel();

					// int taskAmount = tasking.getTaskAmount().intValue();
					// 分配条件
					// 1.同行转账模式关闭中，且：收款银行不处理维护中
					// 2.同行转账模式处于打开中
					peerTrans = checkPeerTrans(tasking.getToAccountBank());
					if (peerTrans == null) {
						maintain = checkMaintain(tasking.getToAccountBank());
						if (!maintain) {
							if (ated == null && isTaskCanUseOutAccount(tasking, level)) {
								log.debug("subAssign>> Begin step2. peerTrans null,task id:{} handicap:{},manual out:{},first out:{}",
										tasking.getTaskId(), tasking.getHandicap(), tasking.getManualOut(),
										tasking.getFirstOut());

								Pair<AllocatingOutwardTask, AllocatingOutwardTask> pair = allocate4Out(currentTime, taskNum++, category, tasking, null, l);
								if(pair.getLeft() == null) {
									notAllocatedTaskMap.put(tasking.getTaskId(), tasking);
								} else {
									allocatedTaskMap.put(tasking.getTaskId(), tasking);
									ated = tasking;
								}
								if(pair.getRight() != null) {
									allocatedTaskMap.put(pair.getRight().getTaskId(), pair.getRight());
									ated = pair.getRight();									
								}
							}
						}
					} else if (peerTrans) {
						if (ated == null && isTaskCanUseOutAccount(tasking, level)) {
							log.debug("subAssign>> Begin step2. PeerOt,task id:{} handicap:{},manual out:{},first out:{}",
									tasking.getTaskId(), tasking.getHandicap(), tasking.getManualOut(),
									tasking.getFirstOut());
							Pair<AllocatingOutwardTask, AllocatingOutwardTask> pair = allocate4Out(currentTime, taskNum++, category, tasking, true, l);
							if(pair.getLeft() == null) {
								notAllocatedTaskMap.put(tasking.getTaskId(), tasking);
							} else {
								allocatedTaskMap.put(tasking.getTaskId(), tasking);
								ated = tasking;
							}
							if(pair.getRight() != null) {
								allocatedTaskMap.put(pair.getRight().getTaskId(), pair.getRight());
								ated = pair.getRight();
							}							
						}
					}
				} catch (Exception e) {
					log.error("subAssign >> Exception. taskId: {}", tasking.getTaskId(), e);
					notAllocatedTaskMap.put(tasking.getTaskId(), tasking);
				} finally {
					if (tasking.getMsg() != null) {
						if ((peerTrans != null && !peerTrans) || maintain) {         //银行维护//
							log.debug("subAssign >> Bank maintained. taskId: {} , taskAmount: {}",tasking.getTaskId(), tasking.getTaskAmount());
							notAllocatedTaskMap.put(tasking.getTaskId(), tasking);
							saveMaintain(tasking);
						}
					}
				}
			}
			log.debug("subAssign >> This Round: successed {}", allocatedTaskMap.keySet());
			for(Entry<Long, AllocatingOutwardTask> e : allocatedTaskMap.entrySet()) {
				notAllocatedTaskMap.remove(e.getKey());
			}
			log.debug("subAssign >> This Round: failed {}", notAllocatedTaskMap.keySet());
			if(log.isDebugEnabled()) {
				String reserved = "";
				for(Pair<OutAccount,AllocatingOutwardTask> v : inAccountWithMoney.asMap().values()) {
					if(v.getRight() != null && v.getLeft() != null) {
						reserved = (reserved.equals("") ? "" : (reserved + ", ")) + v.getLeft().getAccountId() + ":" + v.getRight().getTaskId();
					}
				}
				log.debug("subAssin >> This Round: reserved {}", reserved);
			}
			
			List<String> msg = new ArrayList<String>();
			for(Entry<Long, AllocatingOutwardTask> e : notAllocatedTaskMap.entrySet()) {
				msg.add(e.getValue().getMsg());
			}
			if(msg.size() > 0) {
				lback(null, msg.toArray(new String[msg.size()]), null);
			}
		} finally {
			latch.countDown();
		}
	}

	/**
	 * 出款任务分配
	 *
	 * @param tasking
	 *            待分配任务
	 * @param peerTrans
	 *            同行转账
	 * @param level
	 * @return 分配结果 AllocatingOutwardTask#fromId AllocatingOutwardTask#operator; pair中左边的任务是原始任务tasking；右边的任务是替换出来的缓存中的任务
	 * @see this#checkPeerTrans(String) 校验同行转账 返回值说明
	 */
	private Pair<AllocatingOutwardTask,AllocatingOutwardTask> allocate4Out(long currentTime, int taskNum, String category, AllocatingOutwardTask tasking, Boolean peerTrans, Integer level)
			throws Exception {
		Pair<List<OutAccount>, Integer> outAccountPair = availableCardCache.getOutAccountByCategory(atedTarget, category);
		if(outAccountPair == null || outAccountPair.getLeft() == null) {
			return Pair.of(null, null);
		}
		List<OutAccount> allOutAccount = outAccountPair.getLeft();
		Integer maxRound = outAccountPair.getRight();
		Set<String> activityAcc = rebateUserActivityService.getAllAccountsInActivity();
		String tarBankk = RedisKeys.gen4OutwardTaskBank(tasking.getTaskId());
		String tarBanks = StringUtils.trimToNull(redisService.getStringRedisTemplate().boundValueOps(tarBankk).get());

		ArrayList<String> outOrdered = otherCache.getCachedOutOrdered();
		/**
		 * 任务为非人工出款时，对选择的卡进行排序
		 * 排序规则：卡余额大于任务金额时 入款卡在出款卡前面，入款卡按金额升序，出款卡按轮次升序
		 *           卡余额小于任务金额时 PC的卡在手机卡前面，余额差距大的排在前面
		 */
		if (tasking.getManualOut() == Constants.ROBOT_OUT_YES) {
			Map<Object, Object> outwardPriority = redisService.getStringRedisTemplate().boundHashOps(RedisKeys.OUTWARD_HIGH_PRIORITY).entries();
			log.debug("outwardPriority is data:{}", outwardPriority);
			int amount = tasking.getTaskAmount().intValue() + 50;
			allOutAccount.sort((o1, o2) -> {
				boolean o1ActivityFlag = activityAcc.contains(String.valueOf(o1.getAccountId()));
				boolean o2ActivityFlag = activityAcc.contains(String.valueOf(o2.getAccountId()));
				boolean o1PriorityFlag = outwardPriority.containsKey(String.valueOf(o1.getAccountBaseInfo().getId()));
				boolean o2PriorityFlag = outwardPriority.containsKey(String.valueOf(o2.getAccountBaseInfo().getId()));
				if (o1.getAmount() > amount && o2.getAmount() > amount) {
					if (o1.getAccountBaseInfo().getType() == o2.getAccountBaseInfo().getType()) {
						if (o1PriorityFlag && !o2PriorityFlag) {
							return -1;
						} else if (!o1PriorityFlag && o2PriorityFlag) {
							return 1;
						} else {
							if (o1ActivityFlag && !o2ActivityFlag) {
								return -1;
							} else if (!o1ActivityFlag && o2ActivityFlag) {
								return 1;
							} else {
								return outOrdered.indexOf(o1.getAccountBaseInfo().getId().toString())
										- outOrdered.indexOf(o2.getAccountBaseInfo().getId().toString());
							}
						}
					} else {
						return o1.getAccountBaseInfo().getType() - o2.getAccountBaseInfo().getType();
					}
				} else if (o1.getAmount() > amount) {
					return -1;
				} else if (o2.getAmount() > amount) {
					return 1;
				} else {
					return o1.getAmount() - o2.getAmount();
				}
			});
			log.debug("allocate4Out>> allOutAccount: {}", allOutAccount);
		}
		
		// 0. 将allOutAccount分为入款卡和出款卡两个集合
		List<OutAccount> inAccountList = new ArrayList<>(); // 入款卡
		List<OutAccount> outOnlyList = new ArrayList<>(); // 出款卡
		for(OutAccount outAccount : allOutAccount) {			
			if(OutOnly.class.isInstance(outAccount)) {
				outOnlyList.add(outAccount);
			} else {
				inAccountList.add(outAccount);
			}
		}
		
		// 1. 先给入款卡分任务
		AllocatingOutwardTask result = null;
		for(OutAccount account : inAccountList) {
			int can = canOutTask(account, tasking, level);
			if (can == Constants.INBANK_OK || can == Constants.OK) {
				result = allocate(category, account, tasking, peerTrans, level, maxRound, tarBanks);
				if (result != null) {
					addToInAccountWithMoneyCache(account, null, tasking.getTaskAmount());
					return Pair.of(result, null);
				}
			} 
			log.debug("allocate4Out>> inAccount allocate failed");
			addToInAccountWithMoneyCache(account, null, 0F);
		}
		
		// 2. 再分预留的入款卡 (如果后面的任务可以预留就把原来预留的任务替换掉，原来预留的任务拿去用出款卡出)
		AllocatingOutwardTask todoTask = tasking;
		if(CommonUtils.reserveTask(tasking.getHandicap())) {
			if(System.currentTimeMillis() - tasking.getCreateTime() < 30000) {
				ArrayList<Integer> idList = new ArrayList<Integer>(inAccountWithMoney.asMap().keySet());
				idList.sort((o1, o2)->{
					Long o1order = inAccountWithMoneyUsed.get(o1) == null ? 0L : inAccountWithMoneyUsed.get(o1);
					Long o2order = inAccountWithMoneyUsed.get(o2) == null ? 0L : inAccountWithMoneyUsed.get(o2);
					return o1order.compareTo(o2order);
				});
				for(Integer accountId : idList) {
					Pair<OutAccount,AllocatingOutwardTask> e = inAccountWithMoney.getIfPresent(accountId);
					if(ObjectUtils.isEmpty(e)) {
						continue;
					}
					OutAccount inAccount = e.getLeft();
					AllocatingOutwardTask task = e.getRight();
					if(ObjectUtils.isEmpty(inAccount)) {
						continue;
					}
					int can = canOutTask(inAccount, tasking, level);
					if (can == Constants.INBANK_OK || can == Constants.OK) {
						if(!ObjectUtils.isEmpty(task) && task.getCreateTime() > tasking.getCreateTime()) {
							break;
						}
						inAccountWithMoneyUsed.put(inAccount.getAccountId(), currentTime + taskNum);
						addToInAccountWithMoneyCache(inAccount, tasking, 0F);
						if(ObjectUtils.isEmpty(task)) {
							log.debug("allocate4Out>> reserved successful, no old task");
							return Pair.of(null, null);
						} else {
							log.debug("allocate4Out>> reserved successful, has old task");
							todoTask = task;
						}
						break;
					}
				}
			}
		}
		
		// 3. 分出款卡
		List<OutAccount> retNeed = new ArrayList<>(); // 满足条件，但是余额不足		
		for(OutAccount account : outOnlyList) {
			int can = canOutTask(account, todoTask, level);
			if (can == Constants.INBANK_OK || can == Constants.OK) {
				result = allocate(category, account, todoTask, peerTrans, level, maxRound, tarBanks);
				if (result != null) {
					if(tasking == todoTask) {
						return Pair.of(result, null);
					} else {
						return Pair.of(null, result);
					}
				}
			} else if (can == Constants.OK_BUT_BALANCE) {
				retNeed.add(account);
			} 
		}
		
		// 4. 分出款卡，满足条件，但是余额不足的情况
		for (OutAccount outAccount : retNeed) {
			result = allocate(category, outAccount, todoTask, peerTrans, level, maxRound, tarBanks);
			if (result != null) {
				if(tasking == todoTask) {
					return Pair.of(result, null);
				} else {
					return Pair.of(null, result);
				}
			}
		}

		log.debug("allocate4Out>>task old id : {} , new id:{} all failed", todoTask.getTaskId(), tasking.getTaskId());

		return Pair.of(null, null);
	}

	private int canOutTask(OutAccount account, AllocatingOutwardTask tasking, Integer level) {
		int can = account.canOutTask(tasking, level);
		if(log.isDebugEnabled()) { 
			boolean success = (can == Constants.INBANK_OK || can == Constants.OK);
			boolean success2 = (can == Constants.OK_BUT_BALANCE);
			log.debug("allocate4Out>>task id:{},accountId {},handicap:{},manual out:{},first out:{}. check result = {}, result2 = {}",tasking.getTaskId(), account.getAccountId(), tasking.getHandicap(), tasking.getManualOut(), tasking.getFirstOut(), success, success2);
		}
		return can;
	}
	
	private AllocatingOutwardTask allocate(String category, OutAccount outAccount, AllocatingOutwardTask tasking, Boolean peerTrans, Integer level, Integer maxRound, String tarBanks) {
		AllocatingOutwardTask result = outAccount.allocate(tasking, level, peerTrans, maxRound, tarBanks);
		if (result != null) {
			try {
				log.debug(
						"allocate4Out>> Begin SAVE. taskId: {} , taskAmount: {} , ated FromId: {} , ated Operator: {}",
						tasking.getTaskId(), tasking.getTaskAmount(),
						(tasking != null ? tasking.getFromId() : null),
						(tasking != null ? tasking.getOperator() : null));
				long curr = System.currentTimeMillis();
				Integer fromId = result.getFromId();
				if (fromId != null) {                            //卡号加锁//
					AccountBaseInfo base = accountService.getFromCacheById(fromId);
					if (Objects.equals(Constants.INBANK, base.getType())
							|| Objects.equals(Constants.RESERVEBANK, base.getType())
							|| Objects.equals(Constants.BINDCUSTOMER, base.getType())) {
						allocateTransService.lockTrans(base.getId(), result.getTaskId().intValue(),
								AppConstants.USER_ID_4_ADMIN, result.getTaskAmount().intValue(), 120);    
					} else if (Objects.equals(Constants.OUTBANK, base.getType())
							&& CommonUtils.getTransLockFromTo()) {                                        
						allocateTransService.lockTrans(base.getId(), result.getTaskId().intValue(),
								AppConstants.USER_ID_4_ADMIN, result.getTaskAmount().intValue(), 120);
					}
				}
				saveAllocated(result);                        //保存数据库DB//
				removeFromInAccountWithMoneyCache(result);
				String[] msg = { tasking.getMsg() };
				lback(msg, null, result.getTarget());								
				log.info(
						"allocate4Out>> SAVE END. taskId: {} , taskAmount: {} , ated FromId: {} , ated Operator: {}   consume2: {}	FINAL SUCCESS",
						result.getTaskId(), result.getTaskAmount(),
						result.getFromId(), result.getOperator(), (System.currentTimeMillis() - curr));
			} catch (Exception e) {								
				log.error(
						"allocate4Out>> Excetion. taskId: {} , taskAmount: {} , ated FromId: {} , ated Operator: {}",
						tasking.getTaskId(), tasking.getTaskAmount(),
						(result != null ? result.getFromId() : null),
						(result != null ? result.getOperator() : null), e);
				if (result != null) {
					addOrRemoveAllocated(result.getOperator() == null ? result.getFromId() : result.getOperator(),true);    //去掉atedTargetcache
				}
				result = null;
			} finally {
				if (result != null) {
					addOrRemoveAllocated(result.getOperator() == null ? result.getFromId() : result.getOperator(), true); //去掉atedTargetcache
				}
			}			
			return result;
		} else {
			log.debug("allocate4Out>> balance enough, allocate failed");
			return null;
		}
	}
	
	/**
	 * 入款卡的余额超过1万，就放入缓存中，下一步用来预留出款任务。
	 * @param outAccount 预留的入款账号
	 * @param task	预留的任务
	 * @param outedAmount	正在出款的金额
	 */
	private void addToInAccountWithMoneyCache(OutAccount outAccount, AllocatingOutwardTask task, Float outedAmount) {
		if(task != null && !CommonUtils.reserveTask(task.getHandicap())) {
			return;
		}
		if(!OutInSameTime.class.isInstance(outAccount) && !OutInTurn.class.isInstance(outAccount)) {
			inAccountWithMoney.invalidate(outAccount.getAccountId());
			return;
		}  
		if(outAccount.getAmount() < 10000) {
			inAccountWithMoney.invalidate(outAccount.getAccountId());
			return;
		}
		int out = (outedAmount == null) ? 0 : outedAmount.intValue();
		if(outAccount.getAmount() - out < 10000) {
			inAccountWithMoney.invalidate(outAccount.getAccountId());
			return;
		}
		if(task != null) {
			for(Pair<OutAccount,AllocatingOutwardTask> e : inAccountWithMoney.asMap().values()) {
				if(e.getValue() != null && e.getValue().getTaskId().equals(task.getTaskId())) {
					return;
				}
			}
		}
		inAccountWithMoney.put(outAccount.getAccountId(), Pair.of(outAccount, task));
	}
	
	/**
	 * 删除缓存中的卡号
	 * @param taskId
	 */
	private void removeFromInAccountWithMoneyCache(AllocatingOutwardTask task) {
		if(!CommonUtils.reserveTask(task.getHandicap())) {
			return;
		}
		Integer accountId = null;
		OutAccount outAccount = null;
		for(Entry<Integer, Pair<OutAccount,AllocatingOutwardTask>> e : inAccountWithMoney.asMap().entrySet()) {
			if(e.getValue() != null && e.getValue().getRight() != null && e.getValue().getRight().getTaskId().equals(task.getTaskId())) {
				accountId = e.getKey();
				outAccount = e.getValue().getLeft();
				break;
			}
		}
		if(accountId != null && outAccount != null) {
			inAccountWithMoney.put(accountId, Pair.of(outAccount, null));
		}
	}
	
	/**
	 * 检测是否需要终止分配服务
	 * <p>
	 * 1.如果停服开始时间为空，或停服结束时间为空：则：设置停服开始时间，与停服结束时间</br>
	 * 2.如果当前时间处于停服时间段，则停服</br>
	 * 3.如果当前时间已停服时间段，则：置 停服开始时间，停服结束时间为空</br>
	 * </p>
	 */
	private void checkHaltService() {
		long curr = System.currentTimeMillis();
		if (HALT_SERVICE_START_TIME == null || HALT_SERVICE_END_TIME == null) {
			// 获取停服配置信息
			String val = MemCacheUtils.getInstance().getSystemProfile()
					.get(UserProfileKey.OUTDRA_HALT_ALLOC_START_TIME.getValue());
			val = StringUtils.isNotBlank(val) ? val : "06:50:00";
			String[] args = val.split(":");
			Calendar cal = Calendar.getInstance();
			// 设置停服开始时间
			cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(args[0]));
			cal.set(Calendar.MINUTE, Integer.parseInt(args[1]));
			cal.set(Calendar.SECOND, Integer.parseInt(args[2]));
			HALT_SERVICE_START_TIME = cal.getTime().getTime();
			// 设置停服结束时间
			cal.add(Calendar.HOUR_OF_DAY, 1);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			HALT_SERVICE_END_TIME = cal.getTime().getTime();
			// 如果当前时间已过停服时间段，则：停服时间段加一天
			if (curr >= HALT_SERVICE_END_TIME) {
				HALT_SERVICE_START_TIME = HALT_SERVICE_START_TIME + 86400000;
				HALT_SERVICE_END_TIME = HALT_SERVICE_END_TIME + 86400000;
			}
		}
		if (HALT_SERVICE_START_TIME <= curr && curr <= HALT_SERVICE_END_TIME) {// 不能把if改为elseif
			log.info("出款任务分配:停止服务开始...");
			try {
				Thread.sleep(HALT_SERVICE_END_TIME - System.currentTimeMillis() + 3000);
			} catch (Exception e) {
				log.debug("", e);
			}
			log.info("出款任务分配:停止服务结束...");
		} else if (curr > HALT_SERVICE_END_TIME) {
			HALT_SERVICE_START_TIME = null;
			HALT_SERVICE_END_TIME = null;
		}
	}

	private Integer lorder() {
		String ret = redisService.getStringRedisTemplate().execute(new RedisScript<String>() {
			@Override
			public String getSha1() {
				return "LUA_SCRIPT_ALLOCATING_OUTWARD_TASK_ORDER";
			}

			@Override
			public Class<String> getResultType() {
				return String.class;
			}

			@Override
			public String getScriptAsString() {
				return Constants.LUA_SCRIPT_ALLOCATING_OUTWARD_TASK_ORDER;
			}
		}, null, RedisKeys.ALLOCATING_NEW_OUTWARD_TASK, RedisKeys.ALLOCATING_NEW_OUTWARD_TASK_CAL);
		return Integer.valueOf(ret);
	}

	/**
	 * 执行lua script:批量取出待出款任务
	 * <p>
	 * 1.从 分配队列 移动一定数量的任务 到临时队列</br>
	 * 2.返回 从分配队列 移动到 临时队列 中的 出款任务
	 * </p>
	 *
	 * @return 待分配出款任务 返回信息格式请参考@see 说明
	 * @see com.xinbo.fundstransfer.domain.pojo.AllocatingOutwardTask#genMsg(int
	 *      zone, BizOutwardRequest r, BizOutwardTask t, boolean firstout, boolean
	 *      manualout, boolean thirdout)
	 */
	private String lpop(int len) {
		String ret = redisService.getStringRedisTemplate().execute(new RedisScript<String>() {
																	   @Override
																	   public String getSha1() {
																		   return "LUA_SCRIPT_ALLOCATING_OUTWARD_TASK_POP";
																	   }

																	   @Override
																	   public Class<String> getResultType() {
																		   return String.class;
																	   }

																	   @Override
																	   public String getScriptAsString() {
																		   return Constants.LUA_SCRIPT_ALLOCATING_OUTWARD_TASK_POP;
																	   }
																   }, null, len+"", RedisKeys.ALLOCATING_NEW_OUTWARD_TASK_CAL,
				RedisKeys.ALLOCATING_NEW_OUTWARD_TASK_TMP);
		return StringUtils.trimToNull(ret);
	}

	/**
	 * 检测同行转账
	 * <p>
	 * 1.检测同行转账模式是否处于打开状态,否：返回null,是：流程继续</br>
	 * 2.检测该银行是否处于同行转账模式中,否：返回false,是：返回true</br>
	 * </p>
	 *
	 * @param currBank
	 *            当前银行全称 或 当前银行简称
	 * @return null:同行转账模式关闭；true:同行转账模式打开且处于同行转账模式；false:同行转账模式打开且不处于同行转账模式
	 */
	private Boolean checkPeerTrans(String currBank) {
		String banks = MemCacheUtils.getInstance().getSystemProfile()
				.get(UserProfileKey.OUTDRAW_SYS_PEER_TRANSFER.getValue());// 同行转账银行简称集，以英文逗号分割
		if (StringUtils.isBlank(banks)) {
			return null;
		}
		if (StringUtils.isBlank(currBank)) {
			return false;
		}
		for (String bank : banks.split(",")) {
			if (currBank.contains(bank)) {
				return true;
			}
			if (bank.equals("云南农信") && "云南省农村信用社".contains(currBank)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 检测当前银行是否处于维护状态
	 *
	 * @param currBank
	 *            当前银行
	 * @return true:维护中；false:非维护中
	 */
	private boolean checkMaintain(String currBank) {
		if (StringUtils.isBlank(currBank)) {
			return false;
		}
		String banks = MemCacheUtils.getInstance().getSystemProfile()
				.get(UserProfileKey.OUTDRAW_SYS_MAINTAIN_BANKTYPE.getValue());
		return StringUtils.isNotBlank(banks) && banks.contains(currBank);
	}

	/**
	 * 保存 银行维护的任务
	 */
	private void saveMaintain(AllocatingOutwardTask tasking) {
		if (tasking == null || tasking.getTaskId() == null) {
			return;
		}
		BizOutwardTask outwardTask = outwardTaskRepository.findById2(tasking.getTaskId());
		if (outwardTask == null) {
			return;
		}
		String remark = addFlowChars(OutwardTaskStatus.DuringMaintain, Constants.BANK_MAINTAIN);
		remark = CommonUtils.genRemark(outwardTask.getRemark(), remark, new Date(), Constants.SYS);
		outwardTaskRepository.maintain(tasking.getTaskId(), OutwardTaskStatus.DuringMaintain.getStatus(),
				OutwardTaskStatus.Undeposit.getStatus(), remark);
		log.info("出款任务分配 银行维护 taskId:{} reqId:{} toAccountBank:{} amount:{}", tasking.getTaskId(), tasking.getReqId(),
				tasking.getToAccountBank(), tasking.getTaskAmount());
	}

	/**
	 * 执行lua script:返回未分配的出款任务
	 * <p>
	 * 1.已分配任务 从 临时队列中 删除</br>
	 * 2.未分配任务 从 临时队列中 移动到 待分配队列
	 * </p>
	 *
	 * @param allocated
	 *            已分配任务
	 * @param allocating
	 *            未分配任务
	 * @param target
	 *            被分配对象，当allocated为空，则：target为空；当当allocated不为空，则：target不为空
	 */
	private void lback(String[] allocated, String[] allocating, String target) {
		if ((allocated == null || allocated.length == 0) && (allocating == null || allocating.length == 0)) {
			return;
		}
		log.debug("lback>> allocated {},allocating {},target {}", allocated, allocating, target);
		String ated = allocated == null || allocated.length == 0 ? StringUtils.EMPTY : StringUtils.join(allocated, ";");
		String ating = allocating == null || allocating.length == 0 ? StringUtils.EMPTY
				: StringUtils.join(allocating, ";");
		String tar = target == null ? "" : target;
		redisService.getStringRedisTemplate().execute(new RedisScript<String>() {
														  @Override
														  public String getSha1() {
															  return "LUA_SCRIPT_ALLOCATING_OUTWARD_TASK_BACK";
														  }

														  @Override
														  public Class<String> getResultType() {
															  return String.class;
														  }

														  @Override
														  public String getScriptAsString() {
															  return Constants.LUA_SCRIPT_ALLOCATING_OUTWARD_TASK_BACK;
														  }
													  }, null, RedisKeys.ALLOCATING_NEW_OUTWARD_TASK, RedisKeys.ALLOCATING_NEW_OUTWARD_TASK_TMP,
				RedisKeys.ALLOCATING_NEW_OUTWARD_TARGET, ated, ating, tar);
	}

	/**
	 * 保存分配信息
	 */
	public void saveAllocated(AllocatingOutwardTask ated) {
		// 备注：逻辑处理顺序：1，2，3 请勿改动
		// 基础数据
		int mgrDeal = OutwardTaskStatus.ManagerDeal.getStatus(), undeposit = OutwardTaskStatus.Undeposit.getStatus();
		int cancel = OutwardTaskStatus.ManageCancel.getStatus(), refuse = OutwardTaskStatus.ManageRefuse.getStatus();
		// 会员出款任务保存分配逻辑
		if (ated.getTaskType() != TaskType.RebateTask.getTypeId()) {
			BizOutwardRequest req = outwardRequestService.get(ated.getReqId());
			if (Objects.isNull(req)) {
				log.info("AllocOTask >> MemberOutTask (REQ reqId: {} taskId: {} ) OutwardRequest doesn't exist.",
						ated.getReqId(), ated.getTaskId());
				return;
			}
			String orderNo = req.getOrderNo();
			Date now = new Date();
			List<BizOutwardTask> tasks = outwardTaskRepository.findByOutwardRequestId(ated.getReqId());
			if (CollectionUtils.isEmpty(tasks)) {
				log.info("AllocOTask >> MemberOutTask (TASK reqId: {} taskId: {} ) OutwardRequest doesn't exist.",
						ated.getReqId(), ated.getTaskId());
				return;
			}
			List<BizOutwardTask> atedList = tasks.stream().filter(p -> p.getId().equals(ated.getTaskId()))
					.collect(Collectors.toList());
			String hisRem = CollectionUtils.isEmpty(atedList) ? StringUtils.EMPTY : atedList.get(0).getRemark();
			String theRem;
			Integer outwardPayType;
			if (ated.getManualOut() == Constants.THIRD_OUT_YES) {
				theRem = Constants.LOCK_REQ_SUCCESS + ated.getOperator() + "-" + Constants.THIRD;
				outwardPayType = OutWardPayType.ThirdPay.getType();
			} else {
				AccountBaseInfo base = accountService.getFromCacheById(ated.getFromId());
				theRem = Constants.LOCK_REQ_SUCCESS
						+ (base == null || StringUtils.isBlank(base.getAlias()) ? ated.getFromId() : base.getAlias())
						+ "-" + (ated.getOperator() == null ? Constants.ROBOT : ated.getOperator());
				outwardPayType = ated.getOperator() == null
						? (base.checkMobile() ? OutWardPayType.REFUND.getType() : OutWardPayType.PC.getType())
						: OutWardPayType.MANUAL.getType();
			}
			// 1.公司用款 2.历史记录 有 锁定成功
			if (checkComOutward(req) || tasks.stream().filter(
					p -> StringUtils.isNotBlank(p.getRemark()) && p.getRemark().contains(Constants.LOCK_REQ_SUCCESS))
					.count() > 0) {
				String remark = CommonUtils.genRemark(hisRem, theRem, now, Constants.SYS);// 请勿去掉备注
				int rows = outwardTaskRepository.allocAccount(ated.getTaskId(), ated.getFromId(), ated.getOperator(),
						cancel, refuse, remark, undeposit,outwardPayType);// 分配
				if (rows == 1) {
					log.info(
							"AllocOTask >> MemberOutTask (REQ OrderNo: {} taskId: {} fromId: {} operator: {}) Allocate Success !!.",
							orderNo, ated.getTaskId(), ated.getFromId(), ated.getOperator());
				} else {
					BizOutwardTask task = outwardTaskRepository.findById2(ated.getTaskId());
					if (task == null) {
						log.error("AllocOTask >> task doesn't exists (REQ OrderNo: {} taskId: {})", orderNo,
								ated.getTaskId());
						return;
					}
					log.info("AllocOTask >> update error,task info(ID:{} ,account_id:{} ,operator:{} ,status:{} ) !!.",
							task.getId(), task.getAccountId(), task.getOperator(), task.getStatus());
				}
				return;
			}
			// 首次分配 判断 与 历史记录 有 重新分配 判断
			boolean check = tasks.stream().filter(p -> p.getAccountId() == null).count() == tasks.size();
			check = check || tasks.stream()
					.filter(p -> StringUtils.isNotBlank(p.getRemark()) && p.getRemark().contains(Constants.RE_ALLOCATE))
					.count() > 0;
			// 2.既不是首次分配 且 历史记录无重新分配 处理（转主管）
			if (!check) {
				String remark = CommonUtils.genRemark(StringUtils.EMPTY, Constants.LOCK_REQ_FAIL, now, Constants.SYS);// 请勿去掉备注
				remark = addFlowChars(OutwardTaskStatus.ManagerDeal, remark);// 备注中加流程标识（转主管）
				outwardTaskRepository.alterStatusToMgr(ated.getTaskId(), remark, mgrDeal, undeposit);
				asignFailedTaskService.asignOnTurnToFail(ated.getTaskId());
			}
			// 新系统直接分配，不需锁定
			String remark = CommonUtils.genRemark(hisRem, theRem, now, Constants.SYS);// 请勿去掉备注
			outwardTaskRepository.allocAccount(ated.getTaskId(), ated.getFromId(), ated.getOperator(), cancel, refuse,
					remark, undeposit,outwardPayType);
			log.info(
					"AllocOTask >> MemberOutTask (REQ OrderNo: {} taskId: {} fromId: {} operator: {}) Allocate Success !!.",
					orderNo, ated.getTaskId(), ated.getFromId(), ated.getOperator());
			// }
		} else {
			// 返利任务分配
			BizAccountRebate req = accountRebateRepository.findById2(ated.getTaskId());
			if (Objects.isNull(req)) {
				log.info("AllocOTask >> RebateTask (REQ reqId: {} taskId: {} ) BizAccountRebate doesn't exist.",
						ated.getReqId(), ated.getTaskId());
				return;
			}
			String hisRem = req.getRemark();
			AccountBaseInfo base = accountService.getFromCacheById(ated.getFromId());
			String theRem = Constants.LOCK_REQ_SUCCESS
					+ (base == null || StringUtils.isBlank(base.getAlias()) ? ated.getFromId() : base.getAlias()) + "-"
					+ (ated.getOperator() == null ? Constants.ROBOT : ated.getOperator());
			Date now = new Date();
			String remark = CommonUtils.genRemark(hisRem, theRem, now, Constants.SYS);// 请勿去掉备注
			accountRebateRepository.allocAccount(req.getId(), ated.getFromId(), ated.getOperator(), remark,
					ated.getTaskId(), undeposit);
			log.info("AllocOTask >> RebateTask (taskId: {} fromId: {}) Allocate Success !!.", ated.getTaskId(),
					ated.getFromId());
		}
	}

	/**
	 * 移除已经分配出去的数据
	 *
	 * @param atedId
	 *            已经分配出去的任务
	 * @param addOrRemove
	 *            添加或移除
	 */
	public void addOrRemoveAllocated(Integer atedId, boolean addOrRemove) {
		if (addOrRemove)
			atedTarget.put(atedId, atedId);
		else
			atedTarget.invalidate(atedId);
	}

	/**
	 * 备注信息添加流程说明
	 * <p>
	 * 返回信息格式： 备注信息（转+流程信息）
	 * </p>
	 *
	 * @param next
	 *            即将进入的流程，若无则为:null
	 * @param remark
	 *            备注信息
	 * @return 含流程说明的备注信息
	 */
	private String addFlowChars(OutwardTaskStatus next, String remark) {
		if (next == null) {
			return remark;
		}
		return StringUtils.trimToEmpty(remark) + "(转" + next.getMsg() + ")";
	}

	/**
	 * 检测 该出款请求是否为公司用款
	 *
	 * @param oReq
	 *            出款请求
	 * @return true：是;false:否
	 */
	private boolean checkComOutward(BizOutwardRequest oReq) {
		return Objects.nonNull(oReq) && StringUtils.isBlank(oReq.getMember());
	}

	/**
	 * 检测 指定的出款任务是否可以用出款款卡出款
	 *
	 * @return
	 */
	private boolean isTaskCanUseOutAccount(AllocatingOutwardTask tasking, BizLevel level) {
		if (tasking.getTaskType() != TaskType.RebateTask.getTypeId()
				&& (Objects.isNull(level) || Objects.isNull(level.getCurrSysLevel()))) {
			log.debug("allocate>> target level check false,task {}", tasking);
			return false;
		}
		return true;
	}
	
	private void pushTaskToRedis() {
		log.debug(">>AssignOutwardTask pushTaskToRedis");
		// 300秒同步一次
		if (System.currentTimeMillis() - LAST_SYNC_TASK_TIME > 300000) {
			log.debug(">>AssignOutwardTask pushTaskToRedis LAST_SYNC_TASK_TIME");
			long timeExpire = System.currentTimeMillis() - ONE_DAYS_MILIS;
			List<String> taskList = redisService.getStringRedisTemplate()
					.boundListOps(RedisKeys.ALLOCATING_NEW_OUTWARD_TASK).range(0, -1);
			log.debug(">>AssignOutwardTask pushTaskToRedis taskList:{}", taskList);
			List<Long> taskId = taskList.stream().map(p -> new AllocatingOutwardTask(p).getTaskId())
					.collect(Collectors.toList());

			//处理会员出款任务
			List<Long> undeposit = outwardTaskRepository
					.getAllUndepositTask(CommonUtils.millionSeconds2DateStr(timeExpire));
			log.debug(">>AssignOutwardTask pushTaskToRedis undeposit:{}", undeposit);
			undeposit = undeposit.stream().filter(p -> taskId == null || !taskId.contains(p))
					.collect(Collectors.toList());
			if (!CollectionUtils.isEmpty(undeposit)) {
				undeposit.forEach(p -> outwardRequestService.rpush(p));
			}

			//处理返利提现任务
			List<BizAccountRebate> rebateTaskList = accountRebateRepository
					.getAllUndepositTask(CommonUtils.millionSeconds2DateStr(timeExpire));
			log.debug(">>AssignOutwardTask pushTaskToRedis rebateTaskList:{}", rebateTaskList);
			rebateTaskList = rebateTaskList.stream().filter(p -> taskId == null || !taskId.contains(Long.valueOf(p.getTid())))
					.collect(Collectors.toList());
			if (!CollectionUtils.isEmpty(rebateTaskList)) {
				rebateTaskList.forEach(p -> allocOutwardTaskSer.rpush(p, false));
			}
			
			
			LAST_SYNC_TASK_TIME = System.currentTimeMillis();
		}
	}
	
	private boolean checkTaskIsValid(AllocatingOutwardTask task) {
		// 300秒同步一次
		if (System.currentTimeMillis() - LAST_CLEAN_TASK_TIME > 300000) {
			List<Long> validOutwardTask = outwardTaskRepository.findOutwardTaskStatusByIds();
			List<Long> validRebateTask = accountRebateRepository.findRebateTaskStatusByIds();
			if (Objects.isNull(task)) {
				return false;
			}
			if (Objects.equals(task.getTaskType(), 0) && !validOutwardTask.contains(task.getTaskId())) {
				return false;
			}
			if (Objects.equals(task.getTaskType(), 1) && !validRebateTask.contains(task.getTaskId())) {
				return false;
			}
			LAST_CLEAN_TASK_TIME = System.currentTimeMillis();
		}
		return true;
	}
}
