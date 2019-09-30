package com.xinbo.fundstransfer.service.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.domain.entity.BizIncomeRequest;
import com.xinbo.fundstransfer.domain.entity.BizOutwardTask;
import com.xinbo.fundstransfer.domain.enums.UserProfileKey;
import com.xinbo.fundstransfer.domain.pojo.*;
import com.xinbo.fundstransfer.runtime.MemCacheUtils;
import com.xinbo.fundstransfer.service.*;

@Service
public class TransMonitorServiceImpl implements TransMonitorService {
	private static final Logger log = LoggerFactory.getLogger(TransMonitorServiceImpl.class);
	@Autowired @Lazy
	private AccountService accSer;
	@Autowired
	private RedisService redisSer;
	@Autowired
	private OutwardTaskService oTaskSer;
	@Autowired
	private AllocateTransService transSer;
	private ObjectMapper mapper = new ObjectMapper();

	private volatile static Thread THREAD_TRANS_REAL_AMT = null;
	private static final ConcurrentLinkedQueue<Integer> ACC_QUENE = new ConcurrentLinkedQueue<>();

	private static final Cache<Integer, BigDecimal> ACC_REAL_AMT = CacheBuilder.newBuilder().maximumSize(5000)
			.expireAfterWrite(90, TimeUnit.SECONDS).build();
	// 用于存放分析后，确认正确的元素下标
	private static Map<String, BigDecimal> analyse = new HashMap<>();
	@SuppressWarnings("all")
	private static Exception analyseException = new Exception();

	/**
	 * report the transaction's result.
	 *
	 * @param entity
	 *            transaction's information
	 */
	@Override
	public void reportTransResult(TransferEntity entity) {
		if (Objects.isNull(entity) || !Objects.equals(entity.getResult(), 1)) {
			log.trace("TransAck >>  the record is empty | the transaction is failure.");
			return;
		}
		Integer frId = entity.getFromAccountId(), toId = entity.getToAccountId();
		if (Objects.isNull(frId) && Objects.isNull(toId)) {
			log.trace("TransAck >>  frId and the toId can't be empty at the same time.");
			return;
		}
		BigDecimal tdAmt = new BigDecimal(entity.getAmount()).abs().setScale(2, BigDecimal.ROUND_HALF_UP);
		Long tdTm = Objects.nonNull(entity.getTime()) ? entity.getTime().getTime() : System.currentTimeMillis();
		Integer sort = Objects.nonNull(entity.getTaskId()) ? TransAck.SORT_OTASK : TransAck.SORT_TRANS;
		Long order = Objects.nonNull(entity.getTaskId()) ? entity.getTaskId() : 0;
		Long ackExpireTM = buildAckExprTm();
		if (Objects.nonNull(frId) && frId != 0) {
			String k = TransAck.genMsg(frId, toId, TransAck.INOUT_OUT, tdAmt, tdTm, sort, order);
			redisSer.getStringRedisTemplate().boundValueOps(k).set(StringUtils.EMPTY, ackExpireTM, TimeUnit.MINUTES);
			log.info("TransAck >>  id:{} oppId:{} amt:{} tdTm:{}", frId, toId, tdAmt.negate(), tdTm);
		}
		if (Objects.nonNull(toId) && toId != 0) {
			String k = TransAck.genMsg(toId, frId, TransAck.INOUT_IN, tdAmt, tdTm, sort, order);
			redisSer.getStringRedisTemplate().boundValueOps(k).set(StringUtils.EMPTY, ackExpireTM, TimeUnit.MINUTES);
			log.info("TransAck >>  id:{} oppId:{} amt:{} tdTm:{}", toId, frId, tdAmt, tdTm);
		}
	}

	/**
	 * report the transaction's success result
	 *
	 * @param taskId
	 *            outward task's identity
	 */
	@Override
	public void reportTransResult(Long taskId) {
		BizOutwardTask task = oTaskSer.findById(taskId);
		if (Objects.isNull(task) || Objects.isNull(task.getOperator()) || Objects.isNull(task.getAccountId())) {
			return;
		}
		TransferEntity entity = new TransferEntity();
		entity.setFromAccountId(task.getAccountId());
		entity.setAmount(task.getAmount().floatValue());
		entity.setTaskId(taskId);
		entity.setResult(1);
		reportTransResult(entity);
	}

	/**
	 * report the transaction's success result by income request.
	 *
	 * @param o
	 *            income request
	 */
	@Override
	public void reportTransResult(BizIncomeRequest o) {
		if (Objects.isNull(o) || Objects.isNull(o.getToId()) || Objects.isNull(o.getAmount())) {
			return;
		}
		TransferEntity entity = new TransferEntity();
		entity.setFromAccountId(o.getToId());
		entity.setAmount(o.getAmount().floatValue());
		entity.setResult(1);
		reportTransResult(entity);
	}

	/**
	 * reset the MonitorRisk information.
	 *
	 * <p>
	 * the account need to be reconciled for transaction records
	 * <p>
	 * the method would clean the account's transaction acknowledge records ,
	 * the last reconciled result except the last reconciled snapshot.
	 *
	 * @param accId
	 *            the account's identity to be reset
	 */
	@Override
	public void resetMonitorRisk(int accId) {
		String accIdStr = String.valueOf(accId);
		StringRedisTemplate template = redisSer.getStringRedisTemplate();
		if (Objects.nonNull(template.boundHashOps(RedisKeys.ACC_BAL_ALARM).get(accIdStr))) {
			template.boundHashOps(RedisKeys.ACC_BAL_ALARM).delete(accIdStr);
			template.boundHashOps(RedisKeys.ACC_LAST_REAL_BAL).delete(accIdStr);
			template.boundHashOps(RedisKeys.ACC_SYS_BAL).delete(accIdStr);
			Set<String> keys = template.keys(TransAck.genPattern4Id(accId));
			keys = keys.stream().filter(p -> Objects.equals(new TransAck(p).getInOut(), TransAck.INOUT_OUT))
					.collect(Collectors.toSet());
			if (!CollectionUtils.isEmpty(keys))
				template.delete(keys);
			log.info("ResetMonitorRisk (accId: {} ) >> danger...", accId);
		}
	}

	/**
	 * report account's real balance
	 *
	 * @param id
	 *            the account's ID
	 * @param bal
	 *            the account's real balance at present.
	 */
	@Override
	public void reportRealBal(Integer id, BigDecimal bal) {
		if (true) {
			return;
		}
		if (checkRealBal(id, bal)) {
			try {
				ACC_REAL_AMT.put(id, bal);
				ACC_QUENE.add(id);
			} catch (Exception e) {
				log.error("TransMonitor reportRealBal(id:{} bal:{}) >> Error. msg:{}", id, bal, e);
			}
		} else {
			log.debug("TransMonitor reportRealBal(id:{} bal:{}) >> the account's real balance is invalid.", id, bal);
		}
		if (AllocateIncomeAccountServiceImpl.APP_STOP && Objects.nonNull(THREAD_TRANS_REAL_AMT)) {
			try {
				THREAD_TRANS_REAL_AMT.interrupt();
			} catch (Exception e) {
				log.error("TransMonitor reportRealBal(id:{} bal:{}) >> the thread throws an exception when stopped.",
						id, bal);
			} finally {
				THREAD_TRANS_REAL_AMT = null;
			}
			return;
		} else if (Objects.nonNull(THREAD_TRANS_REAL_AMT) && THREAD_TRANS_REAL_AMT.isAlive()) {
			log.trace("TransMonitor reportRealBal(id:{} bal:{}) >> the thread already started.", id, bal);
			return;
		}
		THREAD_TRANS_REAL_AMT = new Thread(() -> {
			for (;;) {
				try {
					Integer accId = ACC_QUENE.poll();
					if (Objects.isNull(accId)) {
						if (!AllocateIncomeAccountServiceImpl.APP_STOP && Objects.nonNull(THREAD_TRANS_REAL_AMT)) {
							try {
								Thread.sleep(2000);
							} catch (Exception e) {
								log.trace("TransMonitor reportRealBal >> sleep 2 sec,due to no data need to deal with");
							}
							continue;
						} else {
							break;
						}
					}
					AccountBaseInfo base = accSer.getFromCacheById(accId);
					BigDecimal relBal = ACC_REAL_AMT.getIfPresent(accId);
					if (Objects.nonNull(base) && Objects.nonNull(relBal)) {
						monitorRisk(base, relBal);
					}
				} catch (Exception e) {
					log.error("TransMonitor reportRealBal >> throw an exception when deal with the data. Error:{}", e);
				}
			}
		});
		THREAD_TRANS_REAL_AMT.setName("THREAD_TRANS_REAL_AMT");
		THREAD_TRANS_REAL_AMT.start();
	}

	/**
	 * get account's system balance and alarm information.
	 *
	 * @param accList
	 *            the account's ID collection.
	 * @return key:id </br>
	 *         value[0] the account's system balance </br>
	 *         value[1] </br>
	 *         BigDecimal.ZERO :no alarm.</br>
	 *         BigDecimal.ONE :has alarm.</br>
	 */
	@Override
	public Map<Integer, BigDecimal[]> findSysBalAndAlarm(List<Integer> accList) {
		if (CollectionUtils.isEmpty(accList))
			return Collections.emptyMap();
		Map<Integer, BigDecimal[]> ret = new HashMap<Integer, BigDecimal[]>() {
			{
				accList.forEach(p -> put(p, new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO }));
			}
		};
		List<Object> tmpList = accList.stream().distinct().mapToInt(p -> p).mapToObj(String::valueOf)
				.collect(Collectors.toList());
		StringRedisTemplate template = redisSer.getStringRedisTemplate();
		List<Object> sysList = template.boundHashOps(RedisKeys.ACC_SYS_BAL).multiGet(tmpList);
		List<Object> almList = template.boundHashOps(RedisKeys.ACC_BAL_ALARM).multiGet(tmpList);
		int l = tmpList.size();
		for (int i = 0; i < l; i++) {
			String sys = (String) sysList.get(i), alm = (String) almList.get(i);
			if (Objects.isNull(sys) && Objects.isNull(alm))
				continue;
			Integer id = Integer.valueOf(tmpList.get(i).toString());
			BigDecimal[] v = ret.get(id);
			v[0] = Objects.nonNull(sys) ? new BigDecimal(sys).setScale(2, BigDecimal.ROUND_HALF_UP) : v[0];
			v[1] = Objects.nonNull(alm) ? BigDecimal.ONE : v[1];
		}
		return ret;
	}

	/**
	 * set the result of Monitor transaction records.
	 *
	 * @param acc
	 *            the account be monitored
	 * @param lastRealBal
	 *            the last real balance.
	 * @param thisRealBal
	 *            the real balance at the present.
	 * @param ret
	 *            {@code true} if the transaction records are monitored
	 *            successfully;{@code false} otherwise
	 * @param retList
	 *            the list of transaction records
	 */
	@Override
	public void setMonitorRiskResult(AccountBaseInfo acc, BigDecimal lastRealBal, BigDecimal thisRealBal, boolean ret,
			List<TransAck> retList) {
		try {
			String accIdStr = String.valueOf(acc.getId());
			StringRedisTemplate template = redisSer.getStringRedisTemplate();
			long riskMillis = System.currentTimeMillis();
			if (ret) {
				BigDecimal sysBal = thisRealBal;
				for (TransAck ack : retList) {
					if (TransAck.INOUT_IN == ack.getInOut() && TransAck.RET_TBC == ack.getRet())
						sysBal = sysBal.add(ack.getTdAmt().abs());
				}
				setSysBal(acc.getId(), sysBal);
				setLastRealBal(acc.getId(), thisRealBal);
				template.boundHashOps(RedisKeys.ACC_BAL_ALARM).delete(accIdStr);
			} else {
				template.boundHashOps(RedisKeys.ACC_BAL_ALARM).put(accIdStr, String.valueOf(riskMillis));
				log.info("MonitorRisk (id: {} , acc: {} , last: {} ， this: {} ) >> Danger", acc.getId(),
						acc.getAccount(), lastRealBal, thisRealBal);
			}
			TransMonitorResult<TransAckResult> res = new TransMonitorResult<>(acc, ret, riskMillis, lastRealBal,
					thisRealBal, new ArrayList<TransAckResult>());
			if (!CollectionUtils.isEmpty(retList)) {
				for (TransAck ack : retList) {
					AccountBaseInfo opp = ack.getOppId() != 0 ? accSer.getFromCacheById(ack.getOppId()) : null;
					BigDecimal tdAmt = Objects.equals(TransAck.INOUT_OUT, ack.getInOut()) ? ack.getTdAmt().negate()
							: ack.getTdAmt();
					res.getRetList().add(
							new TransAckResult(opp, tdAmt, ack.getTdTm(), ack.getSort(), ack.getRet(), ack.getOrder()));
				}
			}
			template.boundValueOps(RedisKeys.genKey4AccLastRiskLog(acc.getId())).set(mapper.writeValueAsString(res), 24,
					TimeUnit.HOURS);
		} catch (Exception e) {
			log.error("TransMonitor setMonitorRiskResult(accId: {},last: {} , this: {} , ret: {}) >> Error. msg:{}",
					acc.getId(), lastRealBal, thisRealBal, ret, e.getMessage());
		} finally {
			transSer.sendAlarmToFrontDesk();
		}
	}

	/**
	 * get the result of transaction records monitored latestly.
	 *
	 * @param accId
	 *            the account be monitored
	 */
	@Override
	public TransMonitorResult<TransAckResult> getMonitorRiskResult(int accId) throws IOException {
		String o = redisSer.getStringRedisTemplate().boundValueOps(RedisKeys.genKey4AccLastRiskLog(accId)).get();
		if (StringUtils.isBlank(o))
			return null;
		JavaType type = mapper.getTypeFactory().constructParametricType(TransMonitorResult.class, TransAckResult.class);
		return mapper.readValue(o, type);
	}

	/**
	 * Tells whether or not the specified account has matching bank flows
	 *
	 * @param accId
	 *            the specified account's identity
	 *
	 * @return <code>true</code> if and only if the specified account doesn't
	 *         have matching bank flows;<code>false</code> otherwise.
	 */
	@Override
	public boolean checkAccAlarm4Flow(int accId) {
		return Objects.isNull(
				redisSer.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_BAL_ALARM).get(String.valueOf(accId)));
	}

	/**
	 * monitor the risk and control it.
	 * 
	 * @param base
	 *            the account monitored
	 * @param thisRealBal
	 *            the account's real balance at present.
	 *
	 */
	private void monitorRisk(AccountBaseInfo base, BigDecimal thisRealBal) throws JsonProcessingException {
		BigDecimal lastRealBal = getLastRealBal(base.getId());
		if (Objects.isNull(lastRealBal)) {
			setLastRealBal(base.getId(), thisRealBal);
			return;
		}
		lastRealBal = lastRealBal.setScale(2, BigDecimal.ROUND_HALF_UP);
		thisRealBal = thisRealBal.setScale(2, BigDecimal.ROUND_HALF_UP);
		if (lastRealBal.compareTo(thisRealBal) == 0)
			return;
		boolean regular = true;
		StringRedisTemplate template = redisSer.getStringRedisTemplate();
		List<TransAck> ackList = template.keys(TransAck.genPattern4Id(base.getId())).stream().map(p -> new TransAck(p))
				.collect(Collectors.toList());
		try {
			if (!CollectionUtils.isEmpty(ackList)) {
				ackList = analyse(base, ackList, lastRealBal, thisRealBal);
				// assemble the TransAck
				boolean clean = false;
				Collections.sort(ackList, (o1, o2) -> o2.getTdTm().compareTo(o1.getTdTm()));
				for (TransAck ack : ackList) {
					clean = clean || TransAck.INOUT_OUT == ack.getInOut() && TransAck.RET_TBC != ack.getRet();
					if (clean && TransAck.INOUT_OUT == ack.getInOut() && TransAck.RET_TBC == ack.getRet())
						ack.setRet(TransAck.RET_NO);
				}
				// process the transAck data.
				for (TransAck ack : ackList) {
					if (TransAck.RET_TBC != ack.getRet())
						template.delete(ack.getMsg());
					if (TransAck.INOUT_OUT != ack.getInOut() || TransAck.RET_TBC == ack.getRet())
						continue;
					if (TransAck.RET_NO == ack.getRet() && ack.getOppId() != 0) {
						String kPtn = TransAck.genPattern4IdAndOppIdAndInOutAndTdAmtAndTdTm(ack.getOppId(), ack.getId(),
								TransAck.INOUT_IN, ack.getTdAmt(), ack.getTdTm());
						template.delete(template.keys(kPtn));
						BigDecimal sysBal = getSysBal(ack.getOppId());
						setSysBal(ack.getOppId(), Objects.isNull(sysBal) ? null : sysBal.subtract(ack.getTdAmt()));
					}
				}
			} else if (lastRealBal.subtract(thisRealBal).compareTo(BigDecimal.TEN) > 0) {
				regular = false;
			}
		} catch (Exception e) {
			regular = false;
		} finally {
			setMonitorRiskResult(base, lastRealBal, thisRealBal, regular, ackList);
		}
	}

	/**
	 * analyse the transaction record collection
	 * 
	 * @param base
	 *            the account base information
	 * @param dataList
	 *            the transaction record collection.
	 * @param lastRealBal
	 *            the real balance at last analysis time.
	 * @param thisRealBal
	 *            the instant real balance at present.
	 * @throws Exception
	 *             throw any exception when the program can't produce result.
	 * @see TransAck#RET_NO
	 * @see TransAck#RET_YES
	 * @see TransAck#RET_TBC
	 * @see TransAck#getRet()
	 * @see TransAck#setRet(int)
	 */
	private List<TransAck> analyse(AccountBaseInfo base, List<TransAck> dataList, BigDecimal lastRealBal,
			BigDecimal thisRealBal) throws Exception {
		ArrayList<TransAck> ret = new ArrayList<>();
		// 首次上线时，上次余额为空，出款确认成功，入款待确认
		if (Objects.isNull(lastRealBal)) {
			log.trace("acc {} first time use sys balance,all transIn To Be Comfirm,all transOut Comfirm OK",
					base.getId());
			for (TransAck ack : dataList) {
				ack.setRet(ack.getInOut() == TransAck.INOUT_IN ? TransAck.RET_TBC : TransAck.RET_YES);
				ret.add(ack);
			}
			return ret;
		}
		int length = dataList.size();
		int[] arr = new int[length];
		for (int i = 0; i < length; i++) {
			arr[i] = i;
		}
		// 分别从 dataList 中 取出 length、length -1......1 个数，加上 lastRealBal 和
		// thisRealBal
		// 如果误差在可接受范围内，将下标拼接成字符串 analyse
		for (int j = length; j > 0; j--) {
			combine(arr, j, dataList, lastRealBal, thisRealBal);
			if (!CollectionUtils.isEmpty(analyse)) {
				break;
			}
		}
		if (!CollectionUtils.isEmpty(analyse)) {
			log.trace("acc {} some record are be Comfirmed OK", base.getId());
			String key = "";
			if (analyse.size() == 1) {
				for (Map.Entry<String, BigDecimal> entry : analyse.entrySet()) {
					key = entry.getKey();
				}
			} else {
				List<Map.Entry<String, BigDecimal>> resList = new ArrayList<>(analyse.entrySet());
				Collections.sort(resList, Comparator.comparing(Map.Entry::getValue));
				for (Map.Entry<String, BigDecimal> entry : resList) {
					if (entry.getValue().remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
						key = entry.getKey();
						break;
					}
				}
				if (StringUtils.isBlank(key)) {
					key = resList.get(0).getKey();
				}
			}
			for (int i = 0; i < dataList.size(); i++) {
				if (key.contains("," + i + ",")) {
					TransAck ack = dataList.get(i);
					ack.setRet(TransAck.RET_YES);
					ret.add(ack);
					log.trace("TransAck {} Comfirm OK!", ack.getMsg());
				} else {
					TransAck ack = dataList.get(i);
					if (ack.getInOut() == TransAck.INOUT_OUT) {
						ack.setRet(TransAck.RET_NO);
					} else {
						ack.setRet(TransAck.RET_TBC);
					}
					ret.add(ack);
				}
			}
			analyse.clear();
			return ret;
		} else {
			throw analyseException;
		}
	}

	/**
	 * check if one account's real balance is available at present.
	 * 
	 * @param id
	 *            the account's ID
	 * @param bal
	 *            the account's real balance at present.
	 * @return true:if the real balance is valid</br>
	 *         false:if the real balance is invalid</br>
	 */
	private boolean checkRealBal(Integer id, BigDecimal bal) {
		if (Objects.isNull(id) || Objects.isNull(bal) || bal.compareTo(BigDecimal.ZERO) <= 0)
			return false;
		BigDecimal lastReal = getLastRealBal(id);
		return Objects.isNull(lastReal) || bal.setScale(2, BigDecimal.ROUND_HALF_UP).compareTo(lastReal) != 0;
	}

	/**
	 * get one account's system balance at present.
	 * <p>
	 * <code>if Objects.isNull(id)</code> null as the result.
	 * </p>
	 * 
	 * @param id
	 *            the account's ID
	 * @return the account's system balance
	 */
	@Override
	public BigDecimal getSysBal(Integer id) {
		if (Objects.isNull(id))
			return null;
		String ret = (String) redisSer.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_SYS_BAL).get(id.toString());
		return StringUtils.isBlank(ret) ? null : new BigDecimal(ret);
	}

	/**
	 * set one account's system balance.
	 * <p>
	 * Notice: the params can't be empty.
	 * </p>
	 *
	 * @param id
	 *            the account's ID
	 * @param sysBal
	 *            the account's system balance
	 */
	@Override
	public void setSysBal(Integer id, BigDecimal sysBal) {
		if (Objects.isNull(id) || Objects.isNull(sysBal))
			return;
		redisSer.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_SYS_BAL).put(id.toString(),
				sysBal.setScale(2, BigDecimal.ROUND_HALF_UP).toString());
	}

	/**
	 * account's ID that has been in Risk status by monitoring the real balance
	 * transformation.
	 */
	@Override
	public Set<Integer> buildAcc4MonitorRisk() {
		return redisSer.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_BAL_ALARM).keys().stream()
				.map(p -> Integer.valueOf(p.toString())).collect(Collectors.toSet());
	}

	/**
	 * get one account's last real balance.
	 * <p>
	 * <code>if Objects.isNull(id)</code> null as the result.
	 * </p>
	 *
	 * @param id
	 *            the account's ID
	 * @return the account's last real balance.
	 */
	private BigDecimal getLastRealBal(Integer id) {
		if (Objects.isNull(id))
			return null;
		String ret = (String) redisSer.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_LAST_REAL_BAL)
				.get(id.toString());
		return StringUtils.isBlank(ret) ? null : new BigDecimal(ret);
	}

	/**
	 * set one account's system balance.
	 * <p>
	 * Notice: the params can't be empty.
	 * </p>
	 *
	 * @param id
	 *            the account's ID
	 * @param lastRealBal
	 *            the account's last real balance.
	 */
	private void setLastRealBal(Integer id, BigDecimal lastRealBal) {
		if (Objects.isNull(id) || Objects.isNull(lastRealBal))
			return;
		redisSer.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_LAST_REAL_BAL).put(id.toString(),
				lastRealBal.setScale(2, BigDecimal.ROUND_HALF_UP).toString());
	}

	private void combine(int[] dataIndex, int num, List<TransAck> dataList, BigDecimal lastBal, BigDecimal thisBal) {
		if (null == dataIndex || dataIndex.length == 0 || num <= 0 || num > dataIndex.length)
			return;
		int[] b = new int[num];// 辅助空间，保存待输出组合数
		getCombination(dataIndex, num, 0, b, 0, dataList, lastBal, thisBal);
	}

	private void getCombination(int[] dataIndex, int num, int begin, int[] b, int index, List<TransAck> dataList,
			BigDecimal lastBal, BigDecimal thisBal) {
		BigDecimal temp = lastBal;
		if (num == 0) {
			// 如果够n个数了，从集合中取出对应的数据，进行运算，得到结果
			for (int i : b) {
				temp = temp.add(dataList.get(i).getInOut() == TransAck.INOUT_OUT ? dataList.get(i).getTdAmt().negate()
						: dataList.get(i).getTdAmt());
			}
			// 比较取出的数据和当前余额的差值，如果差值在一定范围，将下标设置到结果中
			if (temp.subtract(thisBal).abs().intValue() <= 50) {
				StringBuilder s = new StringBuilder(",");
				for (int i : b) {
					s.append(i).append(",");
				}
				String keys = s.toString();
				analyse.put(keys, temp.subtract(thisBal).abs());
			}
			return;
		}
		for (int i = begin; i < dataIndex.length; i++) {
			b[index] = dataIndex[i];
			getCombination(dataIndex, num - 1, i + 1, b, index + 1, dataList, lastBal, thisBal);
		}
	}

	/**
	 * 获取ack数据的过期时间
	 * 
	 */
	private Long buildAckExprTm() {
		String expTM = StringUtils.trimToEmpty(
				MemCacheUtils.getInstance().getSystemProfile().get(UserProfileKey.ACK_RESULT_EXPIRE_TIME.getValue()));
		Long exprTm = Long.parseLong(StringUtils.isBlank(expTM) ? "20" : expTM);
		exprTm = exprTm > 60 ? 60 : exprTm;
		return exprTm;
	}
}