package com.xinbo.fundstransfer.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.accountfee.exception.NoSuiteAccountFeeRuleException;
import com.xinbo.fundstransfer.accountfee.pojo.AccountFeeCalResult;
import com.xinbo.fundstransfer.accountfee.service.AccountFeeService;
import com.xinbo.fundstransfer.assign.AvailableCardCache;
import com.xinbo.fundstransfer.assign.Constants;
import com.xinbo.fundstransfer.component.common.TimeChangeCommon;
import com.xinbo.fundstransfer.component.net.http.RequestBodyParser;
import com.xinbo.fundstransfer.component.net.http.v2.HttpClientNew;
import com.xinbo.fundstransfer.component.net.socket.ActionEventEnum;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.*;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.*;
import com.xinbo.fundstransfer.domain.pojo.*;
import com.xinbo.fundstransfer.domain.repository.*;
import com.xinbo.fundstransfer.newinaccount.dto.input.BankModifiedInputDTO;
import com.xinbo.fundstransfer.newinaccount.service.InAccountService;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import com.xinbo.fundstransfer.restful.v2.pojo.RequestAccount;
import com.xinbo.fundstransfer.runtime.MemCacheUtils;
import com.xinbo.fundstransfer.sec.FundTransferEncrypter;
import com.xinbo.fundstransfer.service.*;
import com.xinbo.fundstransfer.unionpay.ysf.inputdto.InAccountBindedYSFInputDTO;
import com.xinbo.fundstransfer.utils.ServiceDomain;
import com.xinbo.fundstransfer.utils.randutil.JedisLock;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.shiro.SecurityUtils;
import org.hibernate.MappingException;
import org.hibernate.SQLQuery;
import org.hibernate.SessionFactory;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.transform.Transformers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.persistence.*;
import javax.persistence.criteria.*;
import javax.validation.constraints.NotNull;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 账号管理 *
 *
 * @author 000
 */
@Service
public class AccountServiceImpl implements AccountService {
    private static final Logger log = LoggerFactory.getLogger(AccountServiceImpl.class);
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AccountLevelRepository accountLevelRepository;
    @Autowired
    private HandicapService handicapService;
    @Autowired
    private SysUserService userService;
    @Autowired
    private SysDataPermissionService dataPermissionService;
    @Autowired
    private RedisService redisService;

    @Autowired
    private LevelRepository levelRepository;
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private IncomeRequestRepository incomeRequestRepository;
    @Autowired
    private AccountExtraRepository accountExtraRespository;
    @Autowired
    private AccountBindingService accountBindingService;
    @Autowired
    @Lazy
    private CabanaService cabanaService;
    @Autowired
    @Lazy
    private HostMonitorService hostMonitorService;
    @Autowired
    private AccountMoreService accountMoreService;
    @Autowired
    @Lazy
    private AccountChangeService accountChangeService;
    @Autowired
    private AccountExtraService accountExtraService;
    @Autowired
    private RebateUserService rebateUserService;
    @Autowired
    private LevelService levelService;
    @Autowired
    private AllocateIncomeAccountService incomeAccountAllocateService;
    @Autowired
    private FinLessStatService finLessStatService;
    @Autowired
    private InAccountService inAccountService;
    @Autowired
    private SystemSettingService systemSettingService;
    @Autowired
    private QuickPayService quickPayService;
    @Autowired
    private RebateApiService rebateApiService;
    @Autowired
    private AvailableCardCache availableCardCache;
    @Autowired
    private RequestBodyParser requestBodyParser;
    @Autowired
    private AllocateTransService allocateTransService;
    @Autowired
    private AllocateOutwardTaskService allocateOutwardTaskService;

    private static final String FORMAT_SQL_TRANSFER = "select %s id,sum(case when status=0 then 1 else 0 end)mapping,sum(case when status=1 then 1 else 0 end)mapped,sum(case when status=3 then 1 else 0 end)cancel,%d category from biz_income_request where false %s group by %s";

    private static final String FORMAT_SQL_OUTWARD = "select account_id id,sum(case when status=1 then 1 else 0 end)mapping,sum(case when status=5 then 1 else 0 end)mapped,sum(case when status=6 then 1 else 0 end)cancel,%d category from biz_outward_task where false %s group by account_id";

    private static final String FORMAT_SQL_OUTWARD_ = "select id,sum(case when status=1 then 1 else 0 end)mapping,sum(case when status=5 then 1 else 0 end)mapped,sum(case when status=6 then 1 else 0 end)cancel,%d category from (select account_id id,status from biz_outward_task where false %s union all select account_id id,status from biz_account_rebate where false %s)tab group by id";

    private static final String FORMAT_SQL_FROMTO = "select concat(from_id,':',to_id)fromTo,sum(case when status=0 then 1 else 0 end)mapping,sum(case when status=1 then 1 else 0 end)mapped,sum(case when status=3 then 1 else 0 end)cancel,%d category from biz_income_request where false %s group by from_id,to_id";

    private final static Integer[] INCOME_ACCOUNT_TYPE_ARRAY = new Integer[] { AccountType.InAli.getTypeId(),
            AccountType.InBank.getTypeId(), AccountType.InThird.getTypeId(), AccountType.InWechat.getTypeId() };

    // 本地账号信息缓存。需求7005 设置时间从原来的 4天 改为 30分钟
    private static final Cache<Object, AccountBaseInfo> accountBaseInfoCacheBuilder = CacheBuilder.newBuilder()
            .maximumSize(60000).expireAfterWrite(30, TimeUnit.MINUTES).build();

    // 需求7005 设置时间从原来的 4天 改为 30分钟
    private static final Cache<Object, List<Integer>> accountIdListByAliasCacheBuilder = CacheBuilder.newBuilder()
            .maximumSize(60000).expireAfterWrite(30, TimeUnit.MINUTES).build();

    // 需求7005 设置时间从原来的 4天 改为 30分钟
    private static final Cache<Object, List<Integer>> inComeAccountIdList4User = CacheBuilder.newBuilder()
            .maximumSize(60000).expireAfterWrite(30, TimeUnit.MINUTES).build();

    private static AccountStatInOut DEFAULT_STAT_IN = new AccountStatInOut(AccountStatInOut.Category.In),
            DEFAULT_STAT_OUT_TRANS = new AccountStatInOut(AccountStatInOut.Category.OutTranfer),
            DEFAULT_STAT_OUT_MEMBER = new AccountStatInOut(AccountStatInOut.Category.OutMember);

    private static final String CONSTANT_HTTP_MEDIA_TYPE = "application/json";
    /**
     * 服务标志
     */
    private static boolean checkHostRunRightForDrawTask = false;

    @Value("${service.tag}")
    public void setServiceTagForDrawTask(String serviceTag) {
        if (Objects.nonNull(serviceTag)) {
            checkHostRunRightForDrawTask = ServiceDomain.valueOf(serviceTag) == ServiceDomain.WEB
                    || ServiceDomain.valueOf(serviceTag) == ServiceDomain.ALL;
        }
    }

    /**
     * ###机器：TARGET_TYPE_ROBOT<br/>
     * ###手机：TARGET_TYPE_MOBILE</br>
     */
    private static final int TARGET_TYPE_ROBOT = 1, TARGET_TYPE_MOBILE = 4;

    private static ObjectMapper mapper = new ObjectMapper();
    /**
     * 第三方入款账号提现 锁定目标账号脚本
     */
    @Autowired
    @Qualifier("lockScript")
    private RedisScript<Long> lockScript;

    @Autowired
    @Qualifier("thirdDrawTasklockScript")
    private RedisScript<Long> thirdDrawTasklockScript;

    /**
     * 第三方入款账号提现 解锁目标账号脚本
     */
    @Autowired
    @Qualifier("unlockScript")
    private RedisScript<Long> unlockScript;
    @Autowired
    @Qualifier("searchLockedAccIdsByCurrentUserScript")
    private RedisScript<String> searchLockedAccIdsByCurrentUserScript;
    @Autowired
    @Qualifier("searchAllLockedAccIdsScript")
    private RedisScript<String> searchAllLockedAccIdsScript;
    @Autowired
    @Qualifier("searchLockerByAccountIdScript")
    private RedisScript<String> searchLockerByAccountIdScript;
    @Autowired
    private AllocateTransferService allocateTransferService;

    @Autowired
    private AccountFeeService accountFeeService;
    @Autowired
    private AccountClickService accountClickService;

    /**
     * 查询下发任务里 TransLock 的超时时间 超时了 就删除 锁定 删除 锁定时间 不删除添加时间 删除锁
     */
    @Scheduled(fixedDelay = 60000)
    public void unlockOutCardInDrawTask() {
        List<Integer> outCardIds = getLockedOutCardInDrawTask();
        if (!CollectionUtils.isEmpty(outCardIds)) {
            // 删除TransLock
            Object fromId = Integer.MAX_VALUE - 2;
            StringRedisTemplate template = redisService.getStringRedisTemplate();
            Map<String, Map<String, String>> drawAmountAndFeeByAccountId = getDrawAmountAndFeeByAccountId(null);
            for (Integer toId : outCardIds) {
                if (!CollectionUtils.isEmpty(drawAmountAndFeeByAccountId.get(toId + ""))) {
                    log.info("定时任务检测到是否已经超时 已有下发不能删除: toId:{}", toId);
                    continue;

                }
                TransLock transLock = allocateTransService.buildLockToId(false, toId);
                if (ObjectUtils.isEmpty(transLock)) {
                    // 解锁
                    allocateTransService.unLockForDrawTaskToOutCard(fromId, toId);
                    // 删除选定
                    deleteSelectThirdRecord(toId);
                    List<Integer> toDelete = new ArrayList() {
                        {
                            add(toId);
                        }
                    };
                    // 删除 锁定
                    lockedOrUnlockByDrawTask(null, toDelete, false);
                } else {
                    // 获取 过期时间
                    String transLockStr = transLock.getMsg();
                    Long ttl = template.getExpire(transLockStr);
                    if (null == ttl || ttl <= 0L) {
                        fromId = transLock.getMsg().split(":")[1];
                        // 解锁
                        allocateTransService.unLockForDrawTaskToOutCard(fromId, toId);
                        // 删除选定
                        deleteSelectThirdRecord(toId);
                        List<Integer> toDelete = new ArrayList() {
                            {
                                add(toId);
                            }
                        };
                        // 删除 锁定
                        lockedOrUnlockByDrawTask(null, toDelete, false);
                    }
                }
            }

        }
    }

    /**
     * 定时删除 下发任务里 超过30分钟锁定的 卡 删除TransLock
     */
    @Scheduled(fixedDelay = 60000)
    public void unlockDrawTask() {
        Map<String, Set<ZSetOperations.TypedTuple>> toUnlock = getAllLockedInDrawTask();
        if (CollectionUtils.isEmpty(toUnlock)) {
            return;
        }
        long now = System.currentTimeMillis();
        // 30分钟超时 转为毫秒
        long timeGap = 30 * 60 * 1000;
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        ZSetOperations operations = template.opsForZSet();
        // 锁定时间 key draw:task:user:locktime:5
        HashOperations hashOperations = template.opsForHash();
        for (Map.Entry<String, Set<ZSetOperations.TypedTuple>> entry : toUnlock.entrySet()) {
            // draw:task:user:lock:card:5
            String key = entry.getKey();
            for (ZSetOperations.TypedTuple tuple : entry.getValue()) {
                String keyTime = RedisKeys.LOCKED_USER_ACCOUNT + ":";
                Double score = tuple.getScore();
                if (null != score) {
                    long scoreToTime = score.longValue();
                    long subStrackTime = now - scoreToTime;
                    log.info("时间差:{},30分钟时间毫秒:{}", subStrackTime, timeGap);
                    if (subStrackTime - timeGap >= 0) {
                        Object val = tuple.getValue();
                        Integer toId = Integer.valueOf(val.toString());
                        // 如果下发中 不能删除 TransLock
                        Map<String, Map<String, String>> drawAmountAndFeeByAccountId = getDrawAmountAndFeeByAccountId(
                                null);
                        if (CollectionUtils.isEmpty(drawAmountAndFeeByAccountId.get(toId + ""))) {
                            AccountBaseInfo info = getFromCacheById(toId);
                            if (null != info && AccountType.OutBank.getTypeId().equals(info.getType())) {
                                // 删除TransLock
                                Object fromId = Integer.MAX_VALUE - 2;
                                TransLock transLock = allocateTransService.buildLockToId(false, toId);
                                if (!ObjectUtils.isEmpty(transLock)) {
                                    fromId = Integer.valueOf(transLock.getMsg().split(":")[1]);
                                }
                                allocateTransService.unLockForDrawTaskToOutCard(fromId, toId);
                            }

                            // 删除 锁定时间 key
                            keyTime = keyTime + key.split(":")[5];
                            hashOperations.delete(keyTime, val.toString());
                            // 解锁
                            operations.remove(key, val.toString());
                            // 删除 选中的第三方
                            deleteSelectThirdRecord(toId);
                        }

                    }

                }
            }
        }

    }

    @Override
    public boolean judgeLocked(Integer accountId) {
        String key = RedisKeys.LOCKED_USER_ACCOUNT + ":*";
        StringRedisTemplate template = redisService.getStringRedisTemplate();

        Set<String> keys = template.keys(key);
        if (!CollectionUtils.isEmpty(keys)) {
            for (String key2 : keys) {
                HashOperations hashOperations = template.opsForHash();
                Boolean existed = hashOperations.hasKey(key2, accountId.toString());
                if (null != existed && existed) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Map<String, String> allSelectedThirdIdsWithLockedId(List<Integer> accountIds) {
        String key = RedisKeys.SETUP_USER_THIRDACCOUNT_SELECTED;
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        Map<String, String> res = Maps.newLinkedHashMap();
        // String hkey = it.next().toString();
        // String hval = thirdId + "";
        if (template.hasKey(key)) {
            HashOperations operations = template.opsForHash();
            List list = operations.multiGet(key,
                    accountIds.stream().map(p -> p.toString()).collect(Collectors.toList()));
            if (!CollectionUtils.isEmpty(list)) {
                for (int i = 0, size = accountIds.size(); i < size; i++) {
                    if (Objects.nonNull(list.get(i)))
                        res.put(accountIds.get(i).toString(), list.get(i).toString());

                }
            }
        }
        return res;

    }

    @Override
    public Integer getSelectedThirdIdByLockedId(Integer accountId) {
        if (null != accountId) {
            String key = RedisKeys.SETUP_USER_THIRDACCOUNT_SELECTED;
            StringRedisTemplate template = redisService.getStringRedisTemplate();
            HashOperations operations = template.opsForHash();
            if (template.hasKey(key)) {
                Object val = operations.get(key, accountId.toString());
                if (null != val) {
                    return Integer.valueOf(val.toString());
                }
            }
        }
        return null;
    }

    @Override
    public Integer getSelectThirdRecordByToId(Integer toId) {
        if (null != toId) {
            String key = RedisKeys.SETUP_USER_THIRDACCOUNT_SELECTED;
            StringRedisTemplate template = redisService.getStringRedisTemplate();
            HashOperations operations = template.opsForHash();
            Object hval = operations.get(key, toId.toString());
            return null != hval ? Integer.valueOf(hval.toString()) : null;
        }
        return null;
    }

    @Override
    public void deleteSelectThirdRecord(Integer toId) {
        if (null == toId) {
            return;
        }
        String key = RedisKeys.SETUP_USER_THIRDACCOUNT_SELECTED;
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        HashOperations operations = template.opsForHash();
        if (template.hasKey(key)) {
            if (operations.hasKey(key, toId.toString())) {
                operations.delete(key, toId.toString());
            }
        }
    }

    @Override
    public List<String> getUnfinishedInThirdDraw() {
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        HashOperations operation = template.opsForHash();
        String key = RedisKeys.THIRD_INACCOUNT_DRAW_UNFINISHED;
        List<String> accountIds = Lists.newLinkedList();
        if (template.hasKey(key)) {
            Set<String> hkeys = operation.keys(key);
            if (!CollectionUtils.isEmpty(hkeys)) {
                accountIds.addAll(hkeys);
            }
        }
        return accountIds;
    }

    @Override
    public void deleteUnfinishedDrawInThirdByMatched(Integer accountId) {
        if (null == accountId)
            return;
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        HashOperations operation = template.opsForHash();
        String key = RedisKeys.THIRD_INACCOUNT_DRAW_UNFINISHED;
        String hkey = accountId.toString();
        if (template.hasKey(key)) {
            if (operation.hasKey(key, hkey)) {
                operation.delete(key, hkey);
            }
        }
    }

    @Override
    public BigDecimal getDailyInFlow(Integer accountId) {
        if (null == accountId)
            return BigDecimal.ZERO;
        AccountBaseInfo baseInfo = getFromCacheById(accountId);
        if (baseInfo == null)
            return BigDecimal.ZERO;
        BigDecimal res = BigDecimal.ZERO;
        BigDecimal currBalance = allocateTransService.getCurrBalance(accountId);
        if (currBalance != null && new BigDecimal("99999999").compareTo(currBalance) > 0)
            res = res.add(currBalance);
        BigDecimal outDaily = findAmountDailyByTotal(1, baseInfo.getId());
        if (outDaily != null)
            res = res.add(outDaily);
        return res;
    }

    @Override
    public Map<Integer, BigDecimal> allDailyInFlow(List<Integer> accountIds) {
        Map<Integer, BigDecimal> res = Maps.newLinkedHashMap();
        Map<Integer, BigDecimal> allCurrBalance = allocateTransService.allCurrBalance(accountIds);
        Map<Integer, BigDecimal> allAmountDailyTotal = allAmountDailyTotal(1, accountIds);
        java.util.function.Predicate<Integer> predicate1 = p -> !CollectionUtils.isEmpty(allCurrBalance)
                && allCurrBalance.get(p) != null && new BigDecimal("99999999").compareTo(allCurrBalance.get(p)) > 0;
        java.util.function.Predicate<Integer> predicate2 = p -> !CollectionUtils.isEmpty(allAmountDailyTotal)
                && allAmountDailyTotal.get(p) != null;
        accountIds.stream().forEach(p -> {
            BigDecimal flow = BigDecimal.ZERO;
            if (predicate1.test(p)) {
                flow = flow.add(allCurrBalance.get(p));
            }
            if (predicate2.test(p)) {
                flow = flow.add(allAmountDailyTotal.get(p));
            }
            res.put(p, flow);
        });
        return res;
    }

    @Override
    public boolean isOnline(Integer accountId, Integer type) {
        List<Integer> onlineList = onlineAccountIdsList(type);
        log.debug("加载 在线 账号id :{},type:{}", onlineList.toString(), type);
        return !CollectionUtils.isEmpty(onlineList) && onlineList.contains(accountId);

    }

    /**
     * 自动 或者手工 匹配处理
     *
     * @param toId
     */
    @Override
    public void dealMatch(Integer toId) {
        Map<String, Map<String, String>> drawAmountAndFeeByAccountId = getDrawAmountAndFeeByAccountId(toId);
        if (!ObjectUtils.isEmpty(drawAmountAndFeeByAccountId.get(toId + ""))) {
            // 删除 下发金额 手续费
            deleteDrawAmountAndFee(toId);
            // 删除锁定

            List<Integer> toIdToDelete = new ArrayList() {
                {
                    add(toId);
                }
            };
            lockedOrUnlockByDrawTask(null, toIdToDelete, false);

            // 匹配 解锁
            TransLock transLock = allocateTransService.buildLockToId(false, toId);
            if (!ObjectUtils.isEmpty(transLock)) {
                String fromId = transLock.getMsg().split(":")[1];
                allocateTransService.unLockForDrawTaskToOutCard(fromId, toId);
            }
            // 删除 添加的时间
            saveAddTimeForOutCard(toId, false);
            // 删除选定下发的第三方
            deleteSelectThirdRecord(toId);
            // 如果是出款卡 没有了添加时间则删除 需要钱的队列
            AccountBaseInfo baseInfo = getFromCacheById(toId);
            if (baseInfo != null && AccountType.OutBank.getTypeId().equals(baseInfo.getType())) {
                removeNeedAmountOutCard(toId);
            }
        }
    }

    @Override
    public void saveToUnfinishedDrawInThird(Integer accountId) {
        if (null == accountId)
            return;
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        HashOperations operation = template.opsForHash();
        String key = RedisKeys.THIRD_INACCOUNT_DRAW_UNFINISHED;
        String hkey = accountId.toString();
        String hval = System.currentTimeMillis() + "";
        operation.put(key, hkey, hval);
    }

    /**
     * 选定第三方账号 作为待定下发的时候 保存操作记录 删除原来的TransLock 重新生成TransLock
     *
     * @param thirdId
     * @param accountIds
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSelectThirdRecord(Integer thirdId, List<Integer> accountIds) {
        if (null != thirdId && !CollectionUtils.isEmpty(accountIds)) {
            AccountBaseInfo baseInfo = getFromCacheById(thirdId);
            if (null != baseInfo) {
                String key = RedisKeys.SETUP_USER_THIRDACCOUNT_SELECTED;
                StringRedisTemplate template = redisService.getStringRedisTemplate();
                HashOperations operations = template.opsForHash();
                Iterator it = accountIds.iterator();
                for (; it.hasNext();) {
                    String hkey = it.next().toString();
                    String hval = thirdId + "";
                    operations.put(key, hkey, hval);
                    Integer toId = Integer.valueOf(hkey);
                    AccountBaseInfo baseInfo2 = getFromCacheById(toId);
                    if (null != baseInfo2 && AccountType.OutBank.getTypeId().equals(baseInfo2.getType())) {
                        // 获取原来的 TTL
                        TransLock transLock = allocateTransService.buildLockToId(false, toId);
                        String keyLock = transLock.getMsg();
                        Long ttl = template.getExpire(keyLock);
                        if (StringUtils.isNotBlank(keyLock) && keyLock.length() < 13) {
                            // 删除TransLock
                            allocateTransService.unLockForDrawTaskToOutCard(Integer.MAX_VALUE - 2, toId);
                        } else {
                            // 删除TransLock
                            allocateTransService.unLockForDrawTaskToOutCard(keyLock.split(":")[1], toId);
                        }
                        // 重新生成 TransLock
                        String fromIdStr = thirdId + "" + System.currentTimeMillis();
                        allocateTransService.lockForDrawTaskToOutCard(fromIdStr, toId, transLock.getOprId(),
                                transLock.getTransInt().intValue(), ttl.intValue());
                    }
                }

                String account = baseInfo.getAccount();
                int len = account.length();
                if (len > 3) {
                    account = account.substring(0, 3) + "***" + account.substring(len - 3);
                } else {
                    account = account + "***" + account;
                }
                String bankName = baseInfo.getBankName();
                String remark = "指定下发第三方(" + account + "<br>" + bankName + ")";
                accountClickService.addClickLog(thirdId, remark);
            }
        }
    }

    /**
     * 判断是否已经被设定
     *
     * @param thirdIds
     * @return
     */
    @Override
    public List<Integer> filterAlreadySetted(@NotNull List<Integer> thirdIds) {
        if (CollectionUtils.isEmpty(thirdIds))
            return Lists.newLinkedList();
        List<Integer> allSetAccounts = getAllSetUpThirdAccount(null);
        if (CollectionUtils.isEmpty(allSetAccounts))
            return thirdIds;
        return thirdIds.parallelStream().filter(p -> !allSetAccounts.contains(p)).collect(Collectors.toList());
    }

    /**** 查询 全部设定的第三方账号 ***/
    @Override
    public List<Integer> getAllSetUpThirdAccount(Integer userId) {
        String key = userId == null ? RedisKeys.SETUP_USER_THIRDACCOUNT + ":*"
                : RedisKeys.SETUP_USER_THIRDACCOUNT + ":" + userId;
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        SetOperations operations = template.opsForSet();
        Set<String> keys = template.keys(key);
        List<Integer> res = Lists.newArrayList();
        if (!CollectionUtils.isEmpty(keys)) {
            Iterator it = keys.iterator();
            for (; it.hasNext();) {
                String val = it.next().toString();
                Set<String> members = operations.members(val);
                if (!CollectionUtils.isEmpty(members)) {
                    res.addAll(members.stream().map(p -> Integer.valueOf(p)).collect(Collectors.toList()));
                }
            }

        }
        return res;
    }

    /**** 查询 全部设定的第三方账号 ***/
    @Override
    public Map<Integer, String> getAllSetUpThirdAccount() {
        Map<Integer, String> result = new HashMap<>();
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        for (String key : template.keys(RedisKeys.SETUP_USER_THIRDACCOUNT + ":*")) {
            String opr = key.replace((RedisKeys.SETUP_USER_THIRDACCOUNT + ":"), StringUtils.EMPTY);
            if (!StringUtils.isNumeric(opr))
                continue;
            SysUser user = userService.findFromCacheById(Integer.valueOf(opr));
            if (Objects.isNull(user))
                continue;
            template.opsForSet().members(key).stream().filter(StringUtils::isNumeric).map(Integer::valueOf)
                    .forEach(p -> result.put(p, user.getUid()));
        }
        return result;
    }

    /***
     * 查询我的设定
     *
     * @param userId
     * @return
     */
    @Override
    public List<Integer> getMySetUpThirdAccount(Integer userId) {
        if (null != userId) {
            String key = RedisKeys.SETUP_USER_THIRDACCOUNT + ":" + userId;
            StringRedisTemplate template = redisService.getStringRedisTemplate();
            SetOperations operations = template.opsForSet();
            if (template.hasKey(key)) {
                Set<String> members = operations.members(key);
                if (!CollectionUtils.isEmpty(members)) {
                    return members.stream().map(p -> Integer.valueOf(p)).collect(Collectors.toList());
                }
            }
        }
        return Lists.newArrayList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setThirdAccount(List<Integer> thirdAccounts, Integer userId, boolean add) {
        if (CollectionUtils.isEmpty(thirdAccounts) || null == userId) {
            return;
        }
        String key = RedisKeys.SETUP_USER_THIRDACCOUNT + ":" + userId;
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        SetOperations operations = template.opsForSet();
        Iterator it = thirdAccounts.iterator();
        for (; it.hasNext();) {
            String val = it.next().toString();
            if (add) {
                Long ret = operations.add(key, val);
                log.info("添加我的设定 参数: {},结果:{} ", val, ret);
            } else {
                Long ret = operations.remove(key, val);
                log.info("删除我的设定 参数: {},结果:{} ", val, ret);
            }
        }

    }

    /**
     * 删除他人设定的三方账号
     *
     *
     * @param thirdId
     */
    @Override
    public void releaseOtherSetup(Integer thirdId) {
        Assert.notNull(thirdId, "三方账号id为空");
        String key = RedisKeys.SETUP_USER_THIRDACCOUNT + ":*";
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        SetOperations operations = template.opsForSet();
        log.debug("获取keys开始:", System.currentTimeMillis());
        Set<String> keys = template.keys(key);
        log.debug("获取keys结束:", System.currentTimeMillis());
        if (!CollectionUtils.isEmpty(keys)) {
            for (String key1 : keys) {
                if (operations.isMember(key1, thirdId.toString())) {
                    operations.remove(key1, thirdId.toString());
                }
            }
        }
    }

    /**
     * 出款卡 添加时间 删除添加时间 以计算总耗时
     *
     * @param accountId
     * @param add
     */
    @Override
    public void saveAddTimeForOutCard(Integer accountId, boolean add) {
        if (null == accountId) {
            return;
        }
        // 保存添加的时间
        String key3 = RedisKeys.DRAW_TASK_USER_ADD_CARD_TIME;
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        HashOperations hashOperations = template.opsForHash();
        String hkey = accountId.toString();
        String hval = System.currentTimeMillis() + "";
        if (add) {
            if (hashOperations.hasKey(key3, hkey)) {
                hashOperations.delete(key3, hkey);
            }
            hashOperations.put(key3, hkey, hval);
            log.info("出款卡:{} 保存添加队列时间 :{} 成功!", hkey, hval);
        } else {
            if (hashOperations.hasKey(key3, hkey)) {
                hashOperations.delete(key3, hkey);
                log.info("出款卡:{} 删除添加队列时间 :{} 成功!", hkey, hval);
            }
        }
    }

    /**
     * 获取所有锁定的id和锁定时间
     *
     * @return
     */
    @Override
    public Map<Integer, String> allLockedIdAndTime() {
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        String key = RedisKeys.LOCKED_USER_ACCOUNT + ":*";
        Set<String> keys = template.keys(key);
        Map<Integer, String> res = Maps.newLinkedHashMap();
        if (!CollectionUtils.isEmpty(keys)) {
            // draw:task:user:locktime:5 -> 123:156066606566
            HashOperations hashOperations = template.opsForHash();
            for (String hkey : keys) {
                Map<String, String> map = hashOperations.entries(hkey);
                if (!CollectionUtils.isEmpty(map)) {
                    for (Map.Entry<String, String> entry : map.entrySet()) {
                        res.put(Integer.valueOf(entry.getKey()), entry.getValue());
                    }
                }
            }
        }
        return res;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveLocked(Integer userId, List<Integer> accountId, boolean toLock) {
        Long threadId = Thread.currentThread().getId();
        if (null == accountId) {
            return;
        }
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        String key = RedisKeys.LOCKED_USER_ACCOUNT;
        if (userId == null && !toLock) {
            // 系统删除
            log.info("系统自动删除: userId: {}", userId);
            String key2 = key + ":*";
            Set<String> keys = template.keys(key2);
            if (!CollectionUtils.isEmpty(keys)) {
                for (String key3 : keys) {
                    HashOperations hashOperations = template.opsForHash();
                    Iterator it = accountId.iterator();
                    for (; it.hasNext();) {
                        String hkey = it.next().toString();
                        if (hashOperations.hasKey(key3, hkey)) {
                            Long del = hashOperations.delete(key, hkey);
                            log.info("threadId:{}  解锁  删除  key:{} hkey:{}    解锁结果:{} ", threadId, key, hkey, del);
                        }
                    }
                }
            }
        } else {

            key = key + ":" + userId;
            HashOperations hashOperations = template.opsForHash();
            Iterator it = accountId.iterator();
            Long time = System.currentTimeMillis();
            for (; it.hasNext();) {
                String hkey = it.next().toString();
                if (toLock) {
                    String hval = "" + time;
                    hashOperations.put(key, hkey, hval);
                    log.info("threadId:{} 锁定 添加 key:{} hkey:{} hval:{}   ", threadId, key, hkey, hval);
                } else {
                    Long del = hashOperations.delete(key, hkey);
                    log.info("threadId:{}  解锁  删除  key:{} hkey:{}    解锁结果:{} ", threadId, key, hkey, del);
                }
            }

        }

    }

    @Override
    public Map<String, Map<String, String>> getDrawAmountAndFeeByAccountIds(List<Integer> accountIds) {
        Map<String, Map<String, String>> res = Maps.newLinkedHashMap();
        if (CollectionUtils.isEmpty(accountIds))
            return res;
        String key = RedisKeys.THIRD_DRAW_AMOUNT_FEE;
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        if (template.hasKey(key)) {
            HashOperations operations = template.opsForHash();
            List list = operations.multiGet(key,
                    accountIds.stream().map(p -> p.toString()).collect(Collectors.toList()));
            if (!CollectionUtils.isEmpty(list)) {
                for (int i = 0, size = accountIds.size(); i < size; i++) {
                    Object hval = list.get(i);
                    if (Objects.nonNull(hval)) {
                        Map<String, String> mapVal = new HashMap(3);
                        String[] objStr = hval.toString().split(":");
                        mapVal.put("drawedAmount", objStr[0]);
                        mapVal.put("drawedFee", objStr[1]);
                        mapVal.put("thirdId", objStr[2]);
                        res.put(accountIds.get(i).toString(), mapVal);
                    } else {
                        res.put(accountIds.get(i).toString(), Maps.newHashMap());
                    }
                }
            }
        }
        return res;
    }

    @Override
    public Map<String, Map<String, String>> getDrawAmountAndFeeByAccountId(Integer accountId) {

        // String key = RedisKeys.THIRD_DRAW_AMOUNT_FEE;
        // StringRedisTemplate template = redisService.getStringRedisTemplate();
        // HashOperations operations = template.opsForHash();
        // String hkey = "" + accountId;
        // String hval = amount + ":" + fee + ":" + thirdId;
        String key = RedisKeys.THIRD_DRAW_AMOUNT_FEE;
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        Map<String, Map<String, String>> res = Maps.newLinkedHashMap();
        if (template.hasKey(key)) {
            HashOperations operations = template.opsForHash();
            if (accountId == null) {
                Map<String, String> map = operations.entries(key);
                if (!CollectionUtils.isEmpty(map)) {
                    for (Map.Entry<String, String> entry : map.entrySet()) {
                        String hkey = entry.getKey();
                        String hval = entry.getValue();
                        String[] objStr = hval.split(":");
                        Map<String, String> mapVal = new HashMap(3) {
                            {
                                put("drawedAmount", objStr[0]);
                                put("drawedFee", objStr[1]);
                                put("thirdId", objStr[2]);
                            }
                        };
                        res.put(hkey, mapVal);
                    }
                }
            } else {
                String hkey = accountId.toString();
                Object obj = operations.get(key, hkey);
                if (obj != null) {
                    String[] objStr = obj.toString().split(":");
                    Map<String, String> mapVal = new HashMap(3) {
                        {
                            put("drawedAmount", objStr[0]);
                            put("drawedFee", objStr[1]);
                            put("thirdId", objStr[2]);
                        }
                    };
                    res.put(hkey, mapVal);
                }
            }
        }
        return res;

    }

    @Override
    public void deleteDrawAmountAndFee(Integer accountId) {
        if (accountId == null) {
            return;
        }
        String key = RedisKeys.THIRD_DRAW_AMOUNT_FEE;
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        HashOperations operations = template.opsForHash();
        Long del = operations.delete(key, accountId.toString());
        log.info("匹配或者打回 根据账号id :{}   删除记录的下发金额和手续费 结果:{}", accountId, del);
    }

    @Override
    public List<Integer> allLockedId() {
        Map<Integer, String> allLockedIdAndTime = allLockedIdAndTime();
        List<Integer> res = Lists.newLinkedList();
        if (!CollectionUtils.isEmpty(allLockedIdAndTime)) {
            res.addAll(allLockedIdAndTime.keySet());
        }
        return res;
    }

    @Override
    public boolean isLocked(Integer accountId) {
        if (null == accountId) {
            return false;
        }
        String key = RedisKeys.LOCKED_USER_ACCOUNT + ":*";
        StringRedisTemplate template = redisService.getStringRedisTemplate();

        Set<String> keys = template.keys(key);
        if (!CollectionUtils.isEmpty(keys)) {
            HashOperations hashOperations = template.opsForHash();
            for (String key1 : keys) {
                if (hashOperations.hasKey(key1, accountId.toString())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public List<Integer> failedDrawIds(List<Integer> accountIds) {
        Assert.notEmpty(accountIds, "参数为空");
        String sql = " select status ,type , to_id from biz_income_request where type=103 and to_id in(:accountId) order by create_time desc limit 1";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("accountId", accountIds);
        List<Object[]> list = query.getResultList();
        if (!CollectionUtils.isEmpty(list)) {
            return list.parallelStream()
                    .filter(p -> p[0] != null && p[2] != null
                            && !IncomeRequestStatus.Matched.getStatus().equals(Integer.valueOf(p[0].toString())))
                    .map(p -> Integer.valueOf(p[2].toString())).collect(Collectors.toList());
        }
        return Lists.newLinkedList();
    }

    @Override
    public boolean isDrawFailed(Integer accountId) {
        String sql = " select status ,type , to_id from biz_income_request where type=103 and to_id =:accountId order by create_time desc limit 1";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("accountId", accountId);
        List<Object[]> list = query.getResultList();
        if (CollectionUtils.isEmpty(list)) {
            return false;
        }
        if (list.get(0)[0] != null && !IncomeRequestStatus.Matched.getStatus().equals(list.get(0)[0])) {
            return true;
        }
        return false;

    }

    @Override
    public List<Integer> allDrawingIds() {
        // draw:task:account:amount:fee
        // String hkey = "" + accountId;
        // String hval = amount + ":" + fee + ":" + thirdId;
        List<Integer> res = Lists.newLinkedList();
        String key = RedisKeys.THIRD_DRAW_AMOUNT_FEE;
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        if (template.hasKey(key)) {
            HashOperations operations = template.opsForHash();
            Map<String, String> map = operations.entries(key);
            res.addAll(map.keySet().parallelStream().map(p -> Integer.valueOf(p)).collect(Collectors.toList()));
        }
        return res;
    }

    @Override
    public boolean isDrawing(Integer accountId) {
        if (null != accountId) {
            String key = RedisKeys.THIRD_DRAW_AMOUNT_FEE;
            StringRedisTemplate template = redisService.getStringRedisTemplate();
            if (template.hasKey(key)) {
                HashOperations operations = template.opsForHash();
                String hkey = "" + accountId;
                return operations.hasKey(key, hkey);
            }
        }
        return false;
    }

    @Override
    public void saveDrawAmountAndFee(Integer thirdId, Integer accountId, BigDecimal amount, BigDecimal fee) {
        if (accountId == null || amount == null || fee == null) {
            return;
        }
        String key = RedisKeys.THIRD_DRAW_AMOUNT_FEE;
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        HashOperations operations = template.opsForHash();
        String hkey = "" + accountId;
        String hval = amount + ":" + fee + ":" + thirdId;
        Boolean ret = operations.putIfAbsent(key, hkey, hval);
        log.info("提现 保存出款卡id:{} 下发金额:{} 手续费:{} 第三方id:{} 结果:{}", accountId, amount, fee, thirdId, ret);
    }

    @Override
    public boolean isAlreadyAddedToTime(Integer accountId) {
        if (null != accountId) {
            String key = RedisKeys.DRAW_TASK_USER_ADD_CARD_TIME;
            StringRedisTemplate template = redisService.getStringRedisTemplate();
            HashOperations operations = template.opsForHash();
            return operations.hasKey(key, accountId.toString());
        }
        return false;
    }

    @Override
    public List<Integer> allOtherIdsAddedToTime() {
        String key = RedisKeys.DRAW_TASK_USER_ADD_CARD_TIME;
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        if (template.hasKey(key)) {
            HashOperations operations = template.opsForHash();
            Map<String, String> map = operations.entries(key);
            Set<String> hkeys = map.keySet();
            return hkeys.stream().map(p -> Integer.valueOf(p)).collect(Collectors.toList());
        }
        return Lists.newLinkedList();
    }

    /***
     * 获取出款卡 添加到队列的时间 (2019/7/29 也包括下发卡的时间)
     *
     * @param
     * @return
     */
    @Override
    public Map<String, String> getOutCardAddedTime() {
        // draw:task:user:addTime k:id v:时间
        String key = RedisKeys.DRAW_TASK_USER_ADD_CARD_TIME;
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        Map<String, String> map = Maps.newLinkedHashMap();
        if (template.hasKey(key)) {
            HashOperations operations = template.opsForHash();
            map = operations.entries(key);
        }
        return map;
    }

    /**
     * 保存提现时间
     *
     * @param accountId
     * @param saveFlag
     *            true 保存(提现的时候) false 删除(自动匹配 人工匹配 打回的时候)
     *
     */
    @Override
    public void saveDrawTime(Integer accountId, boolean saveFlag) {
        if (accountId == null)
            return;
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        HashOperations operations = template.opsForHash();
        String key = RedisKeys.SAVE_TRANS_TIME;
        if (saveFlag) {
            operations.put(key, accountId.toString(), System.currentTimeMillis() + "");
        } else {
            if (operations.hasKey(key, accountId.toString())) {
                operations.delete(key, accountId.toString());
            }
        }
    }

    @Override
    public Map<String, String> allDrawTimeMap(List<Integer> accountIds) {
        if (!CollectionUtils.isEmpty(accountIds)) {
            StringRedisTemplate template = redisService.getStringRedisTemplate();
            HashOperations operations = template.opsForHash();
            String key = RedisKeys.SAVE_TRANS_TIME;
            Map<String, String> map = Maps.newHashMap();
            if (template.hasKey(key)) {
                Map<String, String> map1 = operations.entries(key);
                if (!CollectionUtils.isEmpty(map1)) {
                    accountIds.parallelStream().forEach(p -> {
                        if (map1.containsKey(p.toString())) {
                            map.put(p.toString(), map1.get(p.toString()));
                        }
                    });
                }
            }
            return map;
        }
        return Collections.emptyMap();
    }

    /**
     * 获取 所有锁定人 和锁定时间
     *
     * @return Map<String, Map<String, String>> k:accid, v:map->k:name,v:time
     */
    @Override
    public Map<String, Map<String, String>> getLockerNameAndLockedTime() {
        String key = RedisKeys.LOCKED_USER_ACCOUNT + ":*";
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        Set<String> keys = template.keys(key);
        // k 账号id v:锁定人和锁定时间的map
        Map<String, Map<String, String>> res = Maps.newLinkedHashMap();
        if (!CollectionUtils.isEmpty(keys)) {
            HashOperations hashOperations = template.opsForHash();
            for (String key2 : keys) {
                // key2 --- draw:task:user:locktime:5
                String[] objStr = key2.split(":");
                Map<String, String> map = hashOperations.entries(key2);
                // 锁定id和时间
                if (!CollectionUtils.isEmpty(map)) {
                    for (Map.Entry<String, String> entry : map.entrySet()) {
                        String id = entry.getKey();
                        String time = entry.getValue();
                        Map<String, String> mapVal = new LinkedHashMap(2) {
                            {
                                put("lockedTime", time);// 锁定时间
                                put("lockerName", objStr[4]);// 锁定人
                            }
                        };
                        res.put(id, mapVal);
                    }

                }
            }
        }
        return res;
    }

    @Override
    public List<Integer> outCard5OrOtherCard13Online() {
        List<Integer> otherCardIdsOnline = onlineAccountIdsList(AccountType.BindCommon.getTypeId());
        List<Integer> outCardOnline = onlineAccountIdsList(AccountType.OutBank.getTypeId());
        List<Integer> res = Lists.newLinkedList();
        if (!CollectionUtils.isEmpty(otherCardIdsOnline)) {
            res.addAll(otherCardIdsOnline);
        }
        if (!CollectionUtils.isEmpty(outCardOnline)) {
            res.addAll(outCardOnline);
        }
        return res;
    }

    /**
     * 20 秒刷一次
     */
    @Scheduled(fixedDelay = 20000)
    protected void saveAddTimeToOtherCard() {
        if (!checkHostRunRightForDrawTask) {
            log.debug("不是WEB服务");
            return;
        }
        log.debug("定时任务 执行下发任务里的下发卡 添加耗时 ");
        List<Integer> otherCardIdsNeedMoney = findOtherNeedDrawCardIds();
        if (CollectionUtils.isEmpty(otherCardIdsNeedMoney)) {
            return;
        }
        log.debug(" 需要保存时间的卡:{}", otherCardIdsNeedMoney.toString());
        String key = RedisKeys.DRAW_TASK_USER_ADD_CARD_TIME;
        // 保存添加的时间
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        HashOperations hashOperations = template.opsForHash();
        // 2019-09-18 取出已经添加了的卡
        List<Integer> allOtherIdsAddedToTime = allOtherIdsAddedToTime();
        for (Integer id : otherCardIdsNeedMoney) {
            String hkey = id.toString();
            String hval = System.currentTimeMillis() + "";
            // 如果已经保存时间了 就不会再去保存
            if (!allOtherIdsAddedToTime.contains(id)) {
                hashOperations.put(key, hkey, hval);
            }
        }
        // 如果缓存时间里有 但是otherCardIdsNeedMoney这里没有的 要删除缓存时间
        allOtherIdsAddedToTime = allOtherIdsAddedToTime();
        if (!CollectionUtils.isEmpty(allOtherIdsAddedToTime)) {
            List<Integer> cachedIds2 = allOtherIdsAddedToTime.stream().filter(p -> {
                AccountBaseInfo baseInfo = getFromCacheById(Integer.valueOf(p));
                if (null != baseInfo) {
                    // 过滤掉出款卡
                    if (AccountType.OutBank.getTypeId().equals(baseInfo.getType())) {
                        return false;
                    } else {
                        return true;
                    }
                } else {
                    return false;
                }
            }).collect(Collectors.toList());

            // 如果查出来的新卡比redis里保存的添加时间少 那么删除redis里的缓存时间
            cachedIds2.removeAll(otherCardIdsNeedMoney);
            // 所有锁定的卡Id
            List<Integer> allLockedIds = allLockedId();
            // 所有正在下发未到账的Id
            List<Integer> allDrawingIds = allDrawingIds();
            if (!CollectionUtils.isEmpty(cachedIds2)) {
                for (Integer id : cachedIds2) {
                    if (allDrawingIds.contains(id) || allLockedIds.contains(id)) {
                    } else {
                        hashOperations.delete(key, id.toString());
                    }
                }
            }
        }
    }

    /**
     * 下发卡或者其他卡 需要下发的
     *
     * @return
     */
    @Override
    public List<Integer> findOtherNeedDrawCardIds() {
        // 全部在线的 下发卡
        List<Integer> otherCardIdsOnline = onlineAccountIdsList(AccountType.BindCommon.getTypeId());
        if (!CollectionUtils.isEmpty(otherCardIdsOnline)) {
            Integer[] type = new Integer[] { AccountType.BindCommon.getTypeId(), AccountType.BindWechat.getTypeId(),
                    AccountType.BindAli.getTypeId(), AccountType.ThirdCommon.getTypeId() };
            List<Integer> types = Arrays.asList(type);
            Map<Integer, Integer> allCurrCredits = accountChangeService.allCurrCredits(otherCardIdsOnline);
            boolean empty = CollectionUtils.isEmpty(allCurrCredits);
            String minAmount = MemCacheUtils.getInstance().getSystemProfile().getOrDefault("DRAW_TASK_SINGLE_AMOUNT",
                    "10000");
            // 是否所需的金额大于配置项
            java.util.function.Predicate<Integer> predicate = p -> {
                AccountBaseInfo baseInfo = getFromCacheById(p);
                Integer singleTimeAvailableAmount;
                if (empty || !allCurrCredits.containsKey(p)) {
                    singleTimeAvailableAmount = accountChangeService.currCredits(baseInfo);
                } else {
                    singleTimeAvailableAmount = allCurrCredits.get(p);
                }
                // log.debug("本次下发最小金额配置项的值:{}", minAmount);

                if (baseInfo != null && types.contains(baseInfo.getType())
                        && AccountStatus.Normal.getStatus().equals(baseInfo.getStatus())) {
                    if (StringUtils.isNotBlank(minAmount) && singleTimeAvailableAmount != null) {
                        if (singleTimeAvailableAmount.compareTo(Integer.valueOf(minAmount)) >= 0) {
                            // 只有本次下发金额大于10000 的返回
                            return true;
                        }
                        return false;
                    } else {
                        if (singleTimeAvailableAmount != null && singleTimeAvailableAmount > 0) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                } else {
                    return false;
                }

            };
            java.util.function.Predicate<Integer> predicate2 = predicate2(otherCardIdsOnline);
            otherCardIdsOnline = otherCardIdsOnline.stream().filter(predicate).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(otherCardIdsOnline))
                otherCardIdsOnline = otherCardIdsOnline.stream().filter(predicate2).collect(Collectors.toList());
            log.debug("过滤之后的卡:{}", otherCardIdsOnline.toString());
            return otherCardIdsOnline;
        }
        return Lists.newLinkedList();
    }

    private java.util.function.Predicate<Integer> predicate2(List<Integer> targetAccountIds) {
        // 2. 2019-09-16
        Map<Integer, BigDecimal> allCurrBalance = allocateTransService.allCurrBalance(targetAccountIds);
        // 2.1 当前余额大于0
        java.util.function.Predicate<Integer> predicate21 = !CollectionUtils.isEmpty(allCurrBalance)
                ? p -> !CollectionUtils.isEmpty(allCurrBalance) && allCurrBalance.get(p) != null
                && allCurrBalance.get(p).compareTo(BigDecimal.ZERO) >= 0
                : null;
        // 2.2 当日出款流水不为空
        Map<Integer, BigDecimal> allAmountDailyTotal = allAmountDailyTotal(1, targetAccountIds);
        java.util.function.Predicate<Integer> predicate22 = !CollectionUtils.isEmpty(allAmountDailyTotal)
                ? p -> !CollectionUtils.isEmpty(allAmountDailyTotal) && allAmountDailyTotal.get(p) != null
                : null;
        // 2.3 当日出款流水是否大于出款限额
        java.util.function.Predicate<Integer> predicate23 = p -> {
            AccountBaseInfo baseInfo = getFromCacheById(p);
            return baseInfo != null && baseInfo.getLimitOut() != null && baseInfo.getLimitOut() != 0
                    && ((!CollectionUtils.isEmpty(allAmountDailyTotal) && allAmountDailyTotal.get(p) != null
                    ? allAmountDailyTotal.get(p).intValue()
                    : 0)
                    + (!CollectionUtils.isEmpty(allCurrBalance) && allCurrBalance.get(p) != null
                    ? allCurrBalance.get(p).intValue()
                    : 0)
                    + CommonUtils.getLessThenSumDailyOutward()) < baseInfo.getLimitOut();
        };
        java.util.function.Predicate<Integer> predicate2 = p -> p != null;
        if (predicate21 != null)
            predicate2 = predicate2.and(predicate21);
        if (predicate22 != null)
            predicate2 = predicate2.and(predicate22);
        predicate2 = predicate2.and(predicate23);
        return predicate2;
    }

    // TODO 优化
    @Override
    public List<Integer> findNewDrawTask() {
        // 需要钱的出款卡 且未被锁定
        List<Integer> allOutCardIdsUnlocked = needThirdDrawToOutCardIds();
        // 下发卡
        List<Integer> otherCardIdsOnline = findOtherNeedDrawCardIds();

        // 未锁定的出款卡 + 未锁定的其他卡
        List<Integer> res = Lists.newLinkedList();
        if (!CollectionUtils.isEmpty(allOutCardIdsUnlocked)) {
            res.addAll(allOutCardIdsUnlocked);
        }
        // 所有锁定的卡
        List<Integer> allLockedIds = allLockedId();
        // 所有正在下发未到账的Id
        List<Integer> allDrawingIds = allDrawingIds();
        if (!CollectionUtils.isEmpty(otherCardIdsOnline)) {
            otherCardIdsOnline = otherCardIdsOnline.stream().filter(p -> {
                if (allDrawingIds.contains(p) || allLockedIds.contains(p))
                    return false;
                else
                    return true;
            }).collect(Collectors.toList());
            res.addAll(otherCardIdsOnline);
        }
        return res;
    }

    @Override
    public List<Integer> findLockedOrUnfinishedDrawTask() {
        List<Integer> allCardLockedInDrawTask = outCardsOrOtherCardsLockedByUserInDrawTask(null);
        log.debug("所有锁定和下发未匹配的记录id:{}", allCardLockedInDrawTask.toString());
        return allCardLockedInDrawTask;
    }

    @Override
    public List<Integer> findLockedByOneDrawTask(Integer userId) {
        if (null != userId) {
            List<Integer> locked = outCardsOrOtherCardsLockedByUserInDrawTask(userId);
            if (!CollectionUtils.isEmpty(locked)) {
                // 只锁定的 不包括已下发但未到账的
                // 所有正在下发未到账的Id
                List<Integer> allDrawingIds = allDrawingIds();
                locked = locked.stream().filter(p -> !allDrawingIds.contains(p)).collect(Collectors.toList());
            }
            log.debug("用户id :{} 锁定的 但没下发的记录:{}", userId, locked.toString());
            return locked;
        }
        return Lists.newLinkedList();
    }

    @Override
    public List<Integer> findUnfinishedByOneDrawTask(Integer userId) {
        if (null != userId) {
            List<Integer> locked = outCardsOrOtherCardsLockedByUserInDrawTask(userId);
            if (!CollectionUtils.isEmpty(locked)) {
                // 所有正在下发未到账的Id
                List<Integer> allDrawingIds = allDrawingIds();
                locked = locked.stream().filter(p -> allDrawingIds.contains(p)).collect(Collectors.toList());
            }
            log.info("用户id ：{}  下发的但未到账的记录:{}", userId, locked.toString());
            return locked;
        }
        return Lists.newLinkedList();
    }

    private void whereConditionAppend(FindDrawTaskInputDTO inputDTO, String[] handicapIds, StringBuilder sql,
                                      boolean queryLockedByOne) {
        if (queryLockedByOne) {
            sql.append(" and i.operator='").append(inputDTO.getSysUser().getId()).append("'");
        }
        if (handicapIds != null && handicapIds.length > 0) {
            int len = handicapIds.length;
            if (len == 1) {
                sql.append(" and i.handicap='").append(handicapIds[0]).append("'");
            } else {
                sql.append(" and i.handicap in(");
                for (int i = 0; i < len; i++) {
                    sql.append(handicapIds[i]);
                    if (i < len - 1) {
                        sql.append(" ,");
                    } else {
                        sql.append(" )");
                    }
                }
            }
        }
        String alias = StringUtils.trimToEmpty(inputDTO.getAlias());
        if (StringUtils.isNotBlank(alias)) {
            sql.append(" and a.alias='").append(alias).append("'");
        }
        String account = StringUtils.trimToEmpty(inputDTO.getAccount());
        if (StringUtils.isNotBlank(account)) {
            sql.append(" and a.account='").append(account).append("'");
        }
        String bank_type = StringUtils.trimToEmpty(inputDTO.getBankType());
        if (StringUtils.isNotBlank(bank_type)) {
            sql.append(" and a.bank_type='").append(bank_type).append("'");
        }
        String bank_name = StringUtils.trimToEmpty(inputDTO.getBankName());
        if (StringUtils.isNotBlank(bank_name)) {
            sql.append(" and a.bank_name='").append(bank_name).append("'");
        }
        String owner = StringUtils.trimToEmpty(inputDTO.getOwner());
        if (StringUtils.isNotBlank(owner)) {
            sql.append(" and a.owner='").append(owner).append("'");
        }
        Integer statusAcc = inputDTO.getStatus() != null ? inputDTO.getStatus().intValue() : null;
        if (statusAcc != null) {
            sql.append(" and a.status='").append(statusAcc).append("'");
        }
        Integer cardType = inputDTO.getCardType() != null ? inputDTO.getCardType().intValue() : null;
        if (null != cardType) {
            sql.append(" and a.type='").append(cardType).append("'");
        }
        if (StringUtils.isNotBlank(StringUtils.trim(inputDTO.getStartTime()))) {
            sql.append(" and  i.create_time >='").append(inputDTO.getStartTime()).append("'");
        }
        if (StringUtils.isNotBlank(StringUtils.trim(inputDTO.getEndTime()))) {
            sql.append(" and  i.create_time <='").append(inputDTO.getEndTime()).append("'");
        }
        // sql.append(" order by i.create_time desc ");
    }

    @Override
    public List<FindDrawTaskOutputDTO> findDrawTaskRecord(FindDrawTaskInputDTO inputDTO, String[] handicapIds) {
        if (null == inputDTO.getDrawRecordStatus()) {
            return Lists.newLinkedList();
        }
        boolean queryLockedByOne = null != inputDTO.getPageFlag() && inputDTO.getPageFlag().intValue() == 2;
        Integer status = inputDTO.getDrawRecordStatus().intValue() == 1 ? 1 : 3;
        StringBuilder sqlThirdToMemberSplitOrder = null;
        StringBuilder outterSelectSql = null;
        if (status == 11) {
            outterSelectSql = new StringBuilder("SELECT t.*  FROM (");
            // 查询完成的时候 查询给会员拆单完成的记录
            sqlThirdToMemberSplitOrder = new StringBuilder(
                    "SELECT i.from_id id,i.amount,i.fee,i.time_consuming ,i.status,i.from_id "
                            +" ,IFNULL( i.create_time, '0' ) AS create_time,IFNULL( i.update_time, '0' ) AS update_time,"
                            +" i.third_balance AS thirdBalance ,i.third_bank_balance AS thirdBankBalance,i.type AS inType , i.member_real_name  AS memberRealName, "
                            +" operator uid ,i.time_consuming timeConsuming ,i.update_time updateTime , i.create_time createTime"
                            +" from " + "biz_income_request i " + "INNER JOIN biz_account a  "
                            +" on i.from_id = a.id and i.type =115 and a.type=2  where i.status=1");
            sqlThirdToMemberSplitOrder.append("  UNION ALL  ");
            sqlThirdToMemberSplitOrder.append(" SELECT  i.from_id id, i.amount, i.fee, i.time_consuming, i.status, i.from_id , "
                    + " IFNULL(i.create_time, '0') AS create_time, IFNULL(i.update_time, '0') AS update_time, "
                    + " i.third_balance AS thirdBalance ,  i.third_bank_balance AS thirdBankBalance, i.type  AS inType , i.member_real_name  AS memberRealName, "
                    + " operator uid ,i.time_consuming timeConsuming ,i.update_time updateTime , i.create_time createTime"
                    + " from  biz_income_request i  INNER JOIN biz_account a on i.from_id = a.id "
                    + " and i.type = 114  and a.type = 2  where i.status = 1 ");
        }
        StringBuilder sql = new StringBuilder("SELECT a.id ,i.amount,i.fee,i.time_consuming ,i.status,i.from_id "
                + " ,IFNULL( i.create_time, '0' ) AS create_time,IFNULL( i.update_time, '0' ) AS update_time,"
                + " i.third_balance AS thirdBalance ,i.third_bank_balance  AS thirdBankBalance, i.type  AS inType , i.member_real_name  AS memberRealName,  "
                + " operator uid ,i.time_consuming timeConsuming ,i.update_time updateTime , i.create_time createTime"
                + " from " + "biz_income_request i " + "INNER JOIN biz_account a  "
                + " on i.to_id = a.id and i.type =103 and a.type in(5,10,11,12,13)  where i.status='" + status + "'");

        whereConditionAppend(inputDTO, handicapIds, sql, queryLockedByOne);
        Query query = null;
        if (status == 11 && StringUtils.isNotBlank(sqlThirdToMemberSplitOrder.toString())) {
            whereConditionAppend(inputDTO, handicapIds, sqlThirdToMemberSplitOrder, queryLockedByOne);
            sql.append("  UNION ALL  ").append(sqlThirdToMemberSplitOrder);
            outterSelectSql.append(sql).append(" ) t  ORDER BY t.create_time DESC;");
            query = entityManager.createNativeQuery(outterSelectSql.toString());
        } else {
            sql.append(" order by i.create_time desc ");
            query = entityManager.createNativeQuery(sql.toString());
        }
//        Map<String, String> map3 = getOutCardAddedTime();  //  查询redis获取出款卡添加到下发任务时间开始
        query.unwrap(SQLQuery.class).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
        List list = query.getResultList();
//        List<Object[]> list = query.getResultList();
        List<FindDrawTaskOutputDTO> res = Lists.newLinkedList();
        if (!CollectionUtils.isEmpty(list)) {
            for (Object obj : list) {
                Map row = (Map)obj;
                FindDrawTaskOutputDTO outputDTO = new FindDrawTaskOutputDTO();
                outputDTO = outputDTO.getOutPut(row, outputDTO);
                AccountBaseInfo baseInfo = getFromCacheById(outputDTO.getThirdAccountId());
                if (null != baseInfo) {
                    outputDTO.setThirdName(baseInfo.getBankName());
                    outputDTO.setThirdAccount(baseInfo.getAccount());
                }
                SysUser oper = userService.findFromCacheById(Integer.valueOf(row.get("uid").toString()));
                outputDTO.setLockerName(oper.getUid());
                //  收款方
                if("103".equalsIgnoreCase(row.get("inType").toString())){
                    if (baseInfo.getType()==2 && baseInfo.getFlag()==0){
                        outputDTO.setPayee(0);  //  下发卡 pc
                    }else if(baseInfo.getType()==2 && baseInfo.getFlag()==1){
                        outputDTO.setPayee(1);  //  下发卡 返利网
                    }
                }else if ("114".equalsIgnoreCase(row.get("inType").toString())){//  公司用款
                    outputDTO.setPayee(2);
                }else if ("115".equalsIgnoreCase(row.get("inType").toString())){//  会员下发
                    outputDTO.setPayee(3);
                }
                res.add(outputDTO);
//                FindDrawTaskOutputDTO outputDTO = new FindDrawTaskOutputDTO();
//                outputDTO = outputDTO.getOutPut(row, outputDTO);
//                AccountBaseInfo baseInfo = getFromCacheById(outputDTO.getThirdAccountId());
//                if (null != baseInfo) {
//                    outputDTO.setThirdName(baseInfo.getBankName());
//                    outputDTO.setThirdAccount(baseInfo.getAccount());
//                }
//                res.add(outputDTO);
            }
        }
        return res;
    }

    @Override
    public void wrapFinalData(FindDrawTaskInputDTO inputDTO, List<FindDrawTaskOutputDTO> toWrapData,
                              FindDrawTaskResult result) {
        if (CollectionUtils.isEmpty(toWrapData))
            return;
        List<Integer> accountIds = toWrapData.stream().map(p -> p.getId()).collect(Collectors.toList());
        // 包装 统计数量
        if (inputDTO.getQueryByAlias()) {
            // 如果是通过编号查询的 出现在新任务里
            List<Integer> newDrawTasks = inputDTO.getNewDrawTask();
            newDrawTasks.addAll(accountIds);
            Set<Integer> newDrawTasksSet = new HashSet<>(newDrawTasks);
            inputDTO.setNewDrawTask(Lists.newLinkedList(newDrawTasksSet));
        }
        result = wrapCounts(result, accountIds, inputDTO);
        // 返回最终某个页签的数据(四个页签中的一个)
        toWrapData = wrapResultData(toWrapData, inputDTO);
        if (CollectionUtils.isEmpty(toWrapData))
            return;
        accountIds = toWrapData.stream().map(p -> p.getId()).collect(Collectors.toList());
        // getDailyInFlow 收款流水
        Map<Integer, BigDecimal> allDailyInFlow = allDailyInFlow(accountIds);
        List<Integer> accountIdsToCheck = toWrapData.stream().map(p -> p.getId()).collect(Collectors.toList());
        List<Integer> failedDrawIds = failedDrawIds(accountIdsToCheck);
        log.debug("查询redis获取锁定人和锁定时间开始:{}", System.currentTimeMillis());
        Map<String, Map<String, String>> map4 = getLockerNameAndLockedTime();
        log.debug("查询redis获取锁定人和锁定时间结束:{}", System.currentTimeMillis());
        // 如果是出款卡 则查询 添加时间 (2019/7/29 也包括下发卡的时间)
        log.debug("查询redis获取出款卡添加到下发任务时间开始:{}", System.currentTimeMillis());
        Map<String, String> map3 = getOutCardAddedTime();
        log.debug("查询redis获取出款卡添加到下发任务时间结束:{}", System.currentTimeMillis());

        // 所有选定的三方账号id
        log.debug("查询redis选定的三方账号开始:{}", System.currentTimeMillis());
        Map<String, String> selectedThirdIdsAndAccountIds = allSelectedThirdIdsWithLockedId(accountIds);
        log.debug("查询redis选定的三方账号结束:{}", System.currentTimeMillis());
        // 所有下发金额和手续费
        log.debug("查询redis获取下发金额和手续费开始:{}", System.currentTimeMillis());
        Map<String, Map<String, String>> map2 = getDrawAmountAndFeeByAccountIds(accountIds);
        log.debug("查询redis获取下发金额和手续费结束:{}", System.currentTimeMillis());
        // 所有账号的信用额度
        log.debug("查询所有账号单次可以下发的金额 开始：", System.currentTimeMillis());
        Map<Integer, Integer> allCurrCredits = accountChangeService.allCurrCredits(accountIds);
        log.debug("查询所有账号单次可以下发的金额 结束：", System.currentTimeMillis());
        log.debug("查询所有账号单次可以下发的金额 结果：", allCurrCredits.toString());

        // 所有锁定的卡ID
        List<Integer> allLockedIds = allLockedId();
        // 所有正在下发未到账的Id
        List<Integer> allDrawingIds = allDrawingIds();

        // 出款卡
        List<FindDrawTaskOutputDTO> outCards = Lists.newArrayList();
        // 返利网的下发卡
        List<FindDrawTaskOutputDTO> otherCards1 = Lists.newArrayList();
        // 其他卡
        List<FindDrawTaskOutputDTO> otherCards2 = Lists.newArrayList();

        toWrapData.stream().forEach(p -> {
            if (!CollectionUtils.isEmpty(allDailyInFlow)) {
                p.setAllInFlowAmount(allDailyInFlow.get(p.getId()));
            }
            if (!CollectionUtils.isEmpty(map4)) {
                // 获取锁定人 锁定时间 下发耗时 (下发卡的下发耗时与总耗时一致 2019/7/29修改为addTime )
                // 下发耗时 改为 提现到匹配这段时间的耗时 2019-08-06 修改
                // 下发耗时 改为 锁定时间到匹配之间的耗时 2019-09-11
                // put("lockedTime", time);// 锁定时间
                // put("lockerName", objStr[4]);// 锁定人
                Map<String, String> mapVal = map4.get(p.getId().toString());
                if (!CollectionUtils.isEmpty(mapVal)) {
                    String time = mapVal.get("lockedTime");
                    if (StringUtils.isNotBlank(time)) {
                        // 下发耗时
                        p.setLockTime(System.currentTimeMillis() - Long.valueOf(time));
                    } else {
                        // 下发耗时
                        p.setLockTime(0L);
                    }
                    String userId = mapVal.get("lockerName");
                    SysUser user = StringUtils.isNotBlank(userId)
                            ? userService.findFromCacheById(Integer.valueOf(userId))
                            : null;
                    p.setLockerName(null != user ? user.getUid() : "");
                } else {
                    // 下发耗时
                    p.setLockTime(0L);
                    p.setLockerName("");
                }
            } else {
                // 下发耗时
                p.setLockTime(0L);
                p.setLockerName("");
            }
            if (!CollectionUtils.isEmpty(map3)) {
                String addTime = map3.get(p.getId().toString());
                // 出款卡下发卡的添加时间
                if (StringUtils.isNotBlank(addTime))
                    p.setAddTime(System.currentTimeMillis() - Long.valueOf(addTime));
            } else {
                p.setAddTime(0L);
            }
            if (AccountType.OutBank.getTypeId().equals(p.getType())) {
                outCards.add(p);
            } else if (null != p.getFlag() && p.getFlag() == 2) {
                otherCards1.add(p);
            } else {
                otherCards2.add(p);
            }

            // 新任务：如果上一次下发失败 但本次未锁定 则显示 未下发
            // 下发中: 如果上一次失败 但是本次是锁定的 则显示 下发中

            // 判断是否下发中 改为待到账
            if (allDrawingIds.contains(p.getId())) {
                p.setIsDrawing((byte) 3);
            }
            // 判断是否下发失败
            else if (failedDrawIds.contains(p.getId())) {
                if (!allLockedIds.contains(p.getId())) {
                    // 未锁定 则显示 未下发
                    p.setIsDrawing((byte) 1);
                } else {
                    // 锁定了 如果是正在下发页签 显示下发中 改为已锁定
                    if (inputDTO.getQueryLockedAndDrawingInAll() || inputDTO.getQueryLockedByOne()) {
                        p.setIsDrawing((byte) 2);
                    } else {
                        // 否则显示下发失败 改为未下发
                        p.setIsDrawing((byte) 1);
                    }
                }
            } else if (allLockedIds.contains(p.getId())) {
                // 判断是否锁定 已锁定
                p.setIsDrawing((byte) 2);
            } else {
                // 显示 未下发
                p.setIsDrawing((byte) 1);
            }

            Integer singleTimeAvailableAmount = null;
            if (!CollectionUtils.isEmpty(allCurrCredits)) {
                singleTimeAvailableAmount = allCurrCredits.get(p.getId());
            }
            if (singleTimeAvailableAmount == null) {
                AccountBaseInfo baseInfo = getFromCacheById(p.getId());
                singleTimeAvailableAmount = accountChangeService.currCredits(baseInfo);
            }
            // 选定的三方账号id
            String thirdIdSelectedStr = CollectionUtils.isEmpty(selectedThirdIdsAndAccountIds) ? null
                    : selectedThirdIdsAndAccountIds.get(p.getId().toString());
            Integer thirdIdSelected = StringUtils.isNotBlank(thirdIdSelectedStr) ? Integer.valueOf(thirdIdSelectedStr)
                    : null;

            String thirdName = null;
            AccountBaseInfo baseInfoThird = null;
            if (null != thirdIdSelected) {
                baseInfoThird = getFromCacheById(thirdIdSelected);
                // AccountBaseInfo baseInfo1 =
                // accountService.getFromCacheById(thirdIdSelected);
                if (null != baseInfoThird) {
                    thirdName = StringUtils.isNotBlank(baseInfoThird.getBankName()) ? baseInfoThird.getBankName()
                            : thirdName;
                }
            }
            p.setThirdName(thirdName);

            // 手续费规则
            AccountFeeCalResult feeCalResult = null;
            BigDecimal calMoney = BigDecimal.ZERO;
            BigDecimal fee = BigDecimal.ZERO;
            if (null != singleTimeAvailableAmount && null != baseInfoThird) {
                BizAccount account = getById(baseInfoThird.getId());
                p.setLimitOut(singleTimeAvailableAmount);
                p.setThirdAccountId(baseInfoThird.getId());
                p.setThirdAccount(baseInfoThird.getAccount());
                p.setThirdBalance(account.getBalance());
                p.setThirdBankBalance(account.getBankBalance());
                try {
                    /************** 计算可下发金额 和 手续费 *********************/
                    // 需求 7595
                    log.debug("计算手续费开始:{}", System.currentTimeMillis());
                    feeCalResult = accountFeeService.calAccountFee2(baseInfoThird,
                            CommonUtils.wrapDrawAmount(new BigDecimal(singleTimeAvailableAmount)));
                    log.debug("计算手续费结束:{}", System.currentTimeMillis());
                    log.debug("获取手续费结果:{}", feeCalResult);
                } catch (NoSuiteAccountFeeRuleException e) {
                    log.error("获取手续费异常:", e);
                }
            }

            if (null != feeCalResult) {
                calMoney = null == feeCalResult.getMoney() ? calMoney : feeCalResult.getMoney();
                fee = null == feeCalResult.getFee() ? fee : feeCalResult.getFee();
            }
            if (!CollectionUtils.isEmpty(map2)) {
                // 已保存的 下发金额
                Map<String, String> map1 = map2.get(p.getId().toString());
                if (!CollectionUtils.isEmpty(map1)) {
                    String drawedAmount = map1.get("drawedAmount");
                    p.setDrawedAmount(StringUtils.isNotBlank(drawedAmount) ? new BigDecimal(drawedAmount) : calMoney);
                    // 已保存的 下发手续费
                    String drawedFee = map1.get("drawedFee");
                    p.setDrawedFee(StringUtils.isNotBlank(drawedFee) ? new BigDecimal(drawedFee) : fee);
                    // 下发中就返回 下发的第三方账号id
                    p.setThirdAccountId(
                            StringUtils.isNotBlank(map1.get("thirdId")) ? Integer.valueOf(map1.get("thirdId")) : null);
                } else {
                    singleTimeAvailableAmount = singleTimeAvailableAmount == null ? 0 : singleTimeAvailableAmount;
                    p.setDrawedAmount(CommonUtils.wrapDrawAmount(new BigDecimal(singleTimeAvailableAmount)));
                    p.setDrawedFee(fee);
                    p.setLimitOut(singleTimeAvailableAmount);
                }

            } else {
                singleTimeAvailableAmount = singleTimeAvailableAmount == null ? 0 : singleTimeAvailableAmount;
                p.setDrawedAmount(CommonUtils.wrapDrawAmount(new BigDecimal(singleTimeAvailableAmount)));
                p.setDrawedFee(fee);
                p.setLimitOut(singleTimeAvailableAmount);
            }
        });
        // 金额统计
        wrapSumHeader(result, toWrapData);

        // 对某个页签数据按出款卡 返利网下发卡 其他下发卡 内部下发金额排序
        sortAndInnerSequence(toWrapData, accountIds, outCards, otherCards1, otherCards2);
        result.setList(toWrapData);
    }

    // 金额统计
    private void wrapSumHeader(FindDrawTaskResult result, List<FindDrawTaskOutputDTO> toWrapData) {
        // 本次下金额总计
        BigDecimal singleDrawSum = toWrapData.stream().map(p -> {
            if (null != p.getLimitOut()) {
                return new BigDecimal(p.getLimitOut());
            }
            return BigDecimal.ZERO;
        }).reduce(BigDecimal::add).get();
        // 银行余额总计
        BigDecimal sumBankBalance = toWrapData.stream().map(p -> {
            if (null == p.getBankBalance())
                p.setBankBalance(BigDecimal.ZERO);
            return p.getBankBalance();
        }).reduce(BigDecimal::add).get();
        result.getSumAmountMap().put("sumBankBalance", sumBankBalance);
        result.getSumAmountMap().put("singleDrawSum", singleDrawSum);
        result.setList(toWrapData);
    }

    private List<FindDrawTaskOutputDTO> wrapResultData(List<FindDrawTaskOutputDTO> toWrapData,
                                                       FindDrawTaskInputDTO inputDTO) {
        List<Integer> targetAccountIds;
        if (inputDTO.getQueryNewTaskInAll()) {
            targetAccountIds = inputDTO.getNewDrawTask();
            if (CollectionUtils.isEmpty(targetAccountIds)) {
                return Lists.newLinkedList();
            }
            return toWrapData.stream().filter(p -> targetAccountIds.contains(p.getId())).collect(Collectors.toList());
        }
        if (inputDTO.getQueryLockedAndDrawingInAll()) {
            targetAccountIds = inputDTO.getLockedOrUnfinishedDrawTask();
            if (CollectionUtils.isEmpty(targetAccountIds)) {
                return Lists.newLinkedList();
            }
            return toWrapData.stream().filter(p -> targetAccountIds.contains(p.getId())).collect(Collectors.toList());
        }
        if (inputDTO.getQueryLockedByOne()) {
            targetAccountIds = inputDTO.getLockedByOneDrawTask();
            if (CollectionUtils.isEmpty(targetAccountIds)) {
                return Lists.newLinkedList();
            }
            return toWrapData.stream().filter(p -> targetAccountIds.contains(p.getId())).collect(Collectors.toList());
        }
        if (inputDTO.getQueryUnfinishedByOne()) {
            targetAccountIds = inputDTO.getUnfinishedByOneDrawTask();
            if (CollectionUtils.isEmpty(targetAccountIds)) {
                return Lists.newLinkedList();
            }
            return toWrapData.stream().filter(p -> targetAccountIds.contains(p.getId())).collect(Collectors.toList());
        }
        return toWrapData;
    }

    private FindDrawTaskResult wrapCounts(FindDrawTaskResult result, List<Integer> accountIds,
                                          FindDrawTaskInputDTO inputDTO) {
        // 统计数量:新任务 正在下发 我的锁定:正在下发(锁定) 等待到账
        if (!CollectionUtils.isEmpty(inputDTO.getNewDrawTask())) {
            result.getCountsMap().put(result.queryNewInAllCount,
                    inputDTO.getNewDrawTask().stream().filter(p -> accountIds.contains(p)).count());
        }
        if (!CollectionUtils.isEmpty(inputDTO.getLockedOrUnfinishedDrawTask())) {
            result.getCountsMap().put(result.queryLockedOrDrawingInAllCount,
                    inputDTO.getLockedOrUnfinishedDrawTask().stream().filter(p -> accountIds.contains(p)).count());
        }
        if (!CollectionUtils.isEmpty(inputDTO.getLockedByOneDrawTask())) {
            result.getCountsMap().put(result.queryLockedByOneCount,
                    inputDTO.getLockedByOneDrawTask().stream().filter(p -> accountIds.contains(p)).count());
        }
        if (!CollectionUtils.isEmpty(inputDTO.getUnfinishedByOneDrawTask())) {
            result.getCountsMap().put(result.queryDrawingByOneCount,
                    inputDTO.getUnfinishedByOneDrawTask().stream().filter(p -> accountIds.contains(p)).count());
        }

        return result;
    }

    /** 按出款卡 返利网的下发卡 其他卡 顺序返回 并且各个排序内按照 本次下发金额降序 **/
    @Override
    public void sortAndInnerSequence(List<FindDrawTaskOutputDTO> data, List<Integer> accountIds,
                                     List<FindDrawTaskOutputDTO> outCards, List<FindDrawTaskOutputDTO> otherCards1,
                                     List<FindDrawTaskOutputDTO> otherCards2) {
        java.util.function.Predicate<Pair<List<FindDrawTaskOutputDTO>, List<Integer>>> predicate = pair -> !CollectionUtils
                .isEmpty(pair.getLeft()) && !CollectionUtils.isEmpty(pair.getRight())
                && pair.getLeft().size() == pair.getRight().size();
        Pair<List<FindDrawTaskOutputDTO>, List<Integer>> pair = Pair.of(data, accountIds);
        if (predicate.test(pair)) {
            /**************** 内部按照本次下发金额降序 ****************************/
            // 在这里账号实例的LimitOut保存的是本次下发金额
            Comparator<FindDrawTaskOutputDTO> drawableAmountComparator = (o1, o2) -> {
                if (null != o1.getLimitOut() && null != o2.getLimitOut()) {
                    if (o1.getLimitOut() < o2.getLimitOut()) {
                        return 1;
                    } else if (o1.getLimitOut() > o2.getLimitOut()) {
                        return -1;
                    } else {
                        return 0;
                    }
                }
                return 0;
            };

            data = Lists.newLinkedList();
            if (!CollectionUtils.isEmpty(outCards)) {
                Collections.sort(outCards, drawableAmountComparator);
                data.addAll(outCards);
            }
            if (!CollectionUtils.isEmpty(otherCards1)) {
                Collections.sort(otherCards1, drawableAmountComparator);
                data.addAll(otherCards1);
            }
            if (!CollectionUtils.isEmpty(otherCards2)) {
                Collections.sort(otherCards2, drawableAmountComparator);
                data.addAll(otherCards2);
            }
        }
    }

    @Override
    public List<String> excludedIds() {
        // 回收的卡
        Set<String> excluded = getRecycleBindComm();
        log.debug("不能下发不出现在未锁定和锁定的页面中的记录:{}", excluded.toString());
        // 在第三方入款里 提现 未匹配的记录也不能出现
        log.debug("查询未匹配的下发记录开始:{}", System.currentTimeMillis());
        List<String> unfinishedInThirdDraw = getUnfinishedInThirdDraw();
        log.debug("查询未匹配的下发记录结束:{}", System.currentTimeMillis());
        if (!CollectionUtils.isEmpty(unfinishedInThirdDraw)) {
            log.debug("在第三方入款提现里 尚未匹配的记录:{}", unfinishedInThirdDraw.toString());
            excluded.addAll(unfinishedInThirdDraw);
        }
        log.debug("查询旧的下发记录开始:{}", System.currentTimeMillis());
        List<Integer> lockedInThirdInAccountPage = allLockedInThirdInAccount();
        log.debug("查询旧的下发记录结束:{}", System.currentTimeMillis());
        log.debug("查询旧的下发记录结果:{}", lockedInThirdInAccountPage.toString());
        if (!CollectionUtils.isEmpty(lockedInThirdInAccountPage)) {
            excluded.addAll(Lists.transform(lockedInThirdInAccountPage, Functions.toStringFunction()));
        }
        return Lists.newLinkedList(excluded);
    }

    @Override
    public List<FindDrawTaskOutputDTO> filterExclued(List<FindDrawTaskOutputDTO> toFilterList) {
        Assert.notNull(toFilterList, "需要过滤的数据为空");
        List<String> excluded = excludedIds();
        if (!CollectionUtils.isEmpty(excluded)) {
            toFilterList = toFilterList.stream().filter(p -> !excluded.contains(p.getId().toString()))
                    .collect(Collectors.toList());
        }
        log.debug("过滤结果:{}", toFilterList.toString());
        return toFilterList;
    }

    /**
     * 查询 下发任务
     *
     * @param inputDTO
     * @param handicapIds
     *            用户的盘口权限
     * @return
     */
    @Override
    public FindDrawTaskResult findDrawTask(FindDrawTaskInputDTO inputDTO, String[] handicapIds) {
        FindDrawTaskResult res = new FindDrawTaskResult();
        // 所有的可以在下发任务里出现的卡id
        List<Integer> allCardIdsInDrawTask = inputDTO.getAllCardIds();

        try {
            Specification<BizAccount> specification1 = Specification.where(null);
            if (!CollectionUtils.isEmpty(allCardIdsInDrawTask)) {
                specification1 = getBizAccountSpecificationIdIn(specification1, allCardIdsInDrawTask);
            }
            if (null != handicapIds && handicapIds.length > 0) {
                if (handicapIds.length == 1) {
                    specification1 = specification1.and((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder
                            .equal(root.get("handicapId").as(Integer.class), Integer.valueOf(handicapIds[0])));
                } else {
                    specification1 = specification1.and((root, criteriaQuery, criteriaBuilder) -> {
                        CriteriaBuilder.In in = criteriaBuilder.in(root.get("handicapId").as(Integer.class));
                        for (String handicap : handicapIds) {
                            in.value(Integer.valueOf(handicap));
                        }
                        return in;
                    });
                }
            }

            if (null != inputDTO.getStatus()) {
                specification1 = specification1.and((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder
                        .equal(root.get("status").as(Integer.class), inputDTO.getStatus().intValue()));

            }
            if (null != inputDTO.getCardType()) {
                if (1 == inputDTO.getCardType().intValue()) {
                    // 出款卡
                    specification1 = specification1.and((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder
                            .equal(root.get("type").as(Integer.class), AccountType.OutBank.getTypeId()));
                }
                if (2 == inputDTO.getCardType().intValue()) {
                    // 下发卡其他卡
                    specification1 = specification1.and((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder
                            .notEqual(root.get("type").as(Integer.class), AccountType.OutBank.getTypeId()));
                }
            }
            if (StringUtils.isNotBlank(inputDTO.getAccount())) {
                specification1 = specification1.and((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder
                        .equal(root.get("account").as(String.class), inputDTO.getAccount()));

            }
            if (StringUtils.isNotBlank(inputDTO.getAlias())) {
                specification1 = specification1.and((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder
                        .equal(root.get("alias").as(String.class), inputDTO.getAlias()));

            }
            if (StringUtils.isNotBlank(inputDTO.getBankName())) {
                specification1 = specification1.and((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder
                        .equal(root.get("bankName").as(String.class), inputDTO.getBankName()));

            }
            if (StringUtils.isNotBlank(inputDTO.getBankType())) {
                specification1 = specification1.and((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder
                        .equal(root.get("bankType").as(String.class), inputDTO.getBankType()));

            }
            if (StringUtils.isNotBlank(inputDTO.getOwner())) {
                specification1 = specification1.and((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder
                        .equal(root.get("owner").as(String.class), inputDTO.getOwner()));

            }
            Sort sort= new Sort(Sort.Direction.ASC,"createTime");
            if (inputDTO.getQueryStatus()==1) {
                sort=new Sort(Sort.Direction.ASC,"type");
            }
            List<FindDrawTaskOutputDTO> list = Lists.newLinkedList();
            Specification<BizAccount> specification = Specification.where(specification1);
            List<BizAccount> data = CollectionUtils.isEmpty(allCardIdsInDrawTask) ? Lists.newLinkedList():
                    accountRepository.findAll(specification,sort);
//                    : accountRepository.findAll(specification);
            log.debug("数据库查询 结果:{},大小:{}", data.toString(), data.size());
            if (CollectionUtils.isEmpty(data) && StringUtils.isNotBlank(inputDTO.getAlias())) {
                // 根据编号查询 为了补单使用
                inputDTO.setQueryByAlias(true);
                list = findDrawTaskByAlias(inputDTO.getAlias(), inputDTO.getSysUser());
            }
            if (!inputDTO.getQueryByAlias() && !CollectionUtils.isEmpty(data)) {
                data = statisticsInto(data, inputDTO.getSysUser(), null);
                // thirdDrawSortData(data);
                list.addAll(data.stream().map(p -> new FindDrawTaskOutputDTO(p)).collect(Collectors.toList()));
            }
            log.info("查询参数:{}", inputDTO.toString());
            if (!CollectionUtils.isEmpty(list)) {
                // 过滤
                // list = inputDTO.getQueryByAlias() ? list
                // : inputDTO.getQueryStatus().compareTo((byte) 1) != 0 ? list :
                // filterExclued(list);
                // 组装数据:排序 ,统计金额,统计数量
                log.debug("组装开始:" + System.currentTimeMillis());
                wrapFinalData(inputDTO, list, res);
                log.debug("组装结束:" + System.currentTimeMillis());
            }
        } catch (Exception e) {
            log.error("查询异常:", e);
        }
        return res;
    }

    @Override
    public List<FindDrawTaskOutputDTO> findDrawTaskByAlias(@NotNull String alias, @NotNull SysUser user) {
        Assert.notNull(alias, "编号为空");
        Assert.notNull(user, "用户信息为空");
        try {
            List<BizAccount> data = findByAliasAndTypeInForDrawTask(StringUtils.trim(alias));
            if (!CollectionUtils.isEmpty(data)) {
                List<FindDrawTaskOutputDTO> res = Lists.newLinkedList();
                data = statisticsInto(data, user, null);
                // thirdDrawSortData(data);
                for (BizAccount account : data) {
                    FindDrawTaskOutputDTO outputDTO = new FindDrawTaskOutputDTO(account);
                    res.add(outputDTO);
                }
                return res;
            }
        } catch (Exception e) {
            log.error("根据编号查询账号信息 异常:", e);
        }
        return Lists.newLinkedList();
    }

    private Specification<BizAccount> setStatusSpecification(Specification<BizAccount> specification1) {
        specification1 = specification1.and((root, criteriaQuery, criteriaBuilder) -> {
            CriteriaBuilder.In in = criteriaBuilder.in(root.get("status").as(Integer.class));
            Iterator it = Arrays
                    .asList(new Integer[] { AccountStatus.Normal.getStatus(), AccountStatus.StopTemp.getStatus() })
                    .iterator();
            for (; it.hasNext();) {
                in.value(Integer.valueOf(it.next().toString()));
            }
            return in;
        });
        return specification1;
    }

    private Specification<BizAccount> setOutCardSpecification(Specification<BizAccount> specification2,
                                                              List<Integer> outCardMyLocked) {
        if (null != specification2) {
            if (CollectionUtils.isEmpty(outCardMyLocked)) {
                // specification2 = specification2.and((root, criteriaQuery,
                // criteriaBuilder) ->
                // criteriaBuilder
                // .equal(root.get("id").as(Integer.class), 0));
            } else {
                int size = outCardMyLocked.size();
                if (size > 0) {
                    specification2 = getUnLockedOrMyLockedSpecification(specification2, outCardMyLocked, size);
                }
            }
        }
        return specification2;
    }

    public static void thirdDrawSortData(List<BizAccount> data) {
        if (CollectionUtils.isEmpty(data)) {
            return;
        }
        Collections.sort(data, (o1, o2) -> {
            if (o1 == null && o2 == null) {
                return 0;
            }
            if (o1 == null) {
                return -1;
            }
            if (o2 == null) {
                return 1;
            }
            if ((o1.getBankBalance() == null ? new BigDecimal(0) : o1.getBankBalance())
                    .compareTo((o2.getBankBalance() == null ? new BigDecimal(0) : o2.getBankBalance())) == 0) {
                return 0;
            } else {
                if (null != o1.getInCount() && null != o2.getInCount()
                        && (o1.getBankBalance() == null ? new BigDecimal(0) : o1.getBankBalance())
                        .compareTo(new BigDecimal(5000)) < 0) {
                    return o1.getInCount().getMapping() - o2.getInCount().getMapping();
                } else if ((o2.getBankBalance() == null ? new BigDecimal(0) : o2.getBankBalance())
                        .compareTo(new BigDecimal(5000)) < 0) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
    }

    private Specification<BizAccount> getUnLockedOrMyLockedSpecification(Specification<BizAccount> specification,
                                                                         List<Integer> outCardMyLocked, int size) {
        if (size == 1) {
            specification = specification.and((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder
                    .equal(root.get("id").as(Integer.class), outCardMyLocked.get(0)));
        } else {
            specification = specification.and((root, criteriaQuery, criteriaBuilder) -> {
                CriteriaBuilder.In in = criteriaBuilder.in(root.get("id").as(Integer.class));
                Iterator it = outCardMyLocked.iterator();
                for (; it.hasNext();) {
                    in.value(Integer.valueOf(it.next().toString()));
                }
                return in;
            });
        }
        return specification;
    }

    private Specification<BizAccount> addLockedSpecification(Specification<BizAccount> specification,
                                                             List<Integer> outLocked) {
        if (CollectionUtils.isEmpty(outLocked)) {
            specification = specification
                    .and((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("id"), 0));
        } else {
            int size = outLocked.size();
            if (size == 1) {
                specification = specification.and((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder
                        .equal(root.get("id").as(Integer.class), outLocked.get(0)));
            } else {
                specification = getBizAccountSpecificationIdIn(specification, outLocked);
            }

        }
        return specification;
    }

    private Specification<BizAccount> getBizAccountSpecificationIdIn(Specification<BizAccount> specification,
                                                                     List<Integer> outLocked) {
        specification = specification.and((root, criteriaQuery, criteriaBuilder) -> {
            CriteriaBuilder.In in = criteriaBuilder.in(root.get("id").as(Integer.class));
            for (Integer id : outLocked) {
                in.value(id);
            }
            return in;
        });
        return specification;
    }

    /**
     * 下发任务里 某人提现 尚未到账的
     *
     * @param userId
     * @return
     */
    @Override
    public List<Integer> unfinishedInDrawTasksByOne(Integer userId) {
        if (null == userId) {
            return Lists.newArrayList();
        }
        List<Integer> lockedByOne = outCardsOrOtherCardsLockedByUserInDrawTask(userId);
        if (!CollectionUtils.isEmpty(lockedByOne)) {
            // 所有正在下发未到账的Id
            List<Integer> allDrawingIds = allDrawingIds();
            return lockedByOne.stream().filter(p -> allDrawingIds.contains(p)).collect(Collectors.toList());
        }
        return Lists.newArrayList();
    }

    /**
     * 在下发任务里 当前人(userId不为空) 或者 全部的(userId 为空)
     *
     * @param userId
     *            如果传null 则是全部的
     * @param
     * @return
     */
    @Override
    public List<Integer> outCardsOrOtherCardsLockedByUserInDrawTask(Integer userId) {
        List<Integer> res = Lists.newLinkedList();
        boolean queryAll = userId == null;
        String subFix = queryAll ? ":*" : ":" + userId.toString();
        String key = RedisKeys.DRAW_TASK_USER_LOCK_CARD + subFix;
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        ZSetOperations operations = template.opsForZSet();
        if (!queryAll) {
            if (template.hasKey(key)) {
                Set<String> locked = operations.range(key, 0, -1);
                if (!CollectionUtils.isEmpty(locked)) {
                    res.addAll(locked.stream().map(p -> Integer.valueOf(p)).collect(Collectors.toList()));
                }
            }
        } else {
            Set<String> keys = template.keys(key);
            if (!CollectionUtils.isEmpty(keys)) {
                Iterator it = keys.iterator();
                for (; it.hasNext();) {
                    String key1 = it.next().toString();
                    Set<String> locked = operations.range(key1, 0, -1);
                    if (!CollectionUtils.isEmpty(locked)) {
                        res.addAll(locked.stream().map(p -> Integer.valueOf(p)).collect(Collectors.toList()));
                    }
                }
            }
        }
        return res;
    }

    /**
     * 在下发任务里 锁定的 所有卡:包括 出款卡 下发卡
     *
     * @return
     */
    @Override
    public List<Integer> allLockedInDrawTask() {
        String key = RedisKeys.DRAW_TASK_USER_LOCK_CARD + ":*";
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        ZSetOperations operations = template.opsForZSet();
        List<Integer> res = Lists.newArrayList();
        Set<String> keys = template.keys(key);
        for (String key1 : keys) {
            Set<String> members = operations.range(key1, 0, -1);
            if (!CollectionUtils.isEmpty(members)) {
                res.addAll(members.stream().map(p -> Integer.valueOf(p)).collect(Collectors.toList()));
            }
        }
        return res;
    }

    @Override
    public Map<String, Set<ZSetOperations.TypedTuple>> getAllLockedInDrawTask() {
        String key = RedisKeys.DRAW_TASK_USER_LOCK_CARD + ":*";
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        ZSetOperations operations = template.opsForZSet();
        Set<String> keys = template.keys(key);
        Map<String, Set<ZSetOperations.TypedTuple>> res = new HashMap<>(1024);
        for (String key1 : keys) {
            if (template.hasKey(key1)) {
                Map<String, Set<ZSetOperations.TypedTuple>> map = new HashMap<>(256);
                Set<ZSetOperations.TypedTuple> val = operations.rangeWithScores(key1, 0, -1);
                if (!CollectionUtils.isEmpty(val)) {
                    map.put(key1, val);
                    res.putAll(map);
                }
            }
        }
        return res;
    }

    /**
     * 在第三方入款 提现里 锁定的所有卡 :包括 出款卡 和 下发卡
     *
     * @return
     */
    @Override
    public List<Integer> allLockedInThirdInAccount() {
        List<Integer> res = Lists.newArrayList();
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        // 所有在第三方入款里 锁定的下发卡
        Set<String> keys = template.keys(RedisKeys.LOCK_TARGETACCOUNT_FOR_THIRDACCOUNTDRAW_KEY_PREX + ":*:*");
        Set<String> lockedTargetAccounts = Sets.newHashSet();
        if (!CollectionUtils.isEmpty(keys)) {
            Iterator it = keys.iterator();
            for (; it.hasNext();) {
                String key = it.next().toString();
                BoundSetOperations operations = template.boundSetOps(key);
                lockedTargetAccounts.addAll(operations.members());
            }
            if (!CollectionUtils.isEmpty(lockedTargetAccounts)) {
                List<Integer> lockedTargetAccountsInt = Lists.transform(Lists.newArrayList(lockedTargetAccounts),
                        p -> Integer.valueOf(p));
                res.addAll(lockedTargetAccountsInt);
            }
        }
        // 所有在第三方入款里 锁定的 出款卡
        List<String> locked = allOutCardIdsLocked();
        if (!CollectionUtils.isEmpty(locked)) {
            res.addAll(Lists.transform(locked, p -> Integer.valueOf(p)));
        }
        return res;
    }

    @Override
    public List<Integer> getAccountIdLockedByUserIdInDrawTask(Integer userId) {
        if (null != userId) {
            StringRedisTemplate template = redisService.getStringRedisTemplate();
            ZSetOperations operations = template.opsForZSet();
            String key = RedisKeys.DRAW_TASK_USER_LOCK_CARD + ":" + userId;
            if (template.hasKey(key)) {
                Set<String> members = operations.range(key, 0, -1);
                if (!CollectionUtils.isEmpty(members)) {
                    return members.stream().map(p -> Integer.valueOf(p)).collect(Collectors.toList());
                }
            }

        }
        return null;
    }

    @Override
    public Long getOutcardLockedTime(Integer accountId) {
        if (null == accountId)
            return null;
        String key = RedisKeys.LOCKED_USER_ACCOUNT;
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        HashOperations operations = template.opsForHash();
        if (template.hasKey(key)) {
            if (operations.hasKey(key, accountId.toString())) {
                Object obj = operations.get(key, accountId.toString());
                if (null != obj) {
                    return Long.valueOf(obj.toString());
                }
            }
        }
        return null;
    }

    @Override
    public List<Integer> getLockedOutCardInDrawTask() {
        String key = RedisKeys.DRAW_TASK_USER_LOCK_OUTCARD;
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        HashOperations hashOperations = template.opsForHash();
        if (template.hasKey(key)) {
            Set<String> outIds = hashOperations.keys(key);
            if (!CollectionUtils.isEmpty(outIds)) {
                return outIds.stream().map(p -> Integer.valueOf(p)).collect(Collectors.toList());
            }
        }
        return Lists.newArrayList();
    }

    @Override
    public boolean saveLockedOutCardInDrawTask(Integer outId, boolean add) {
        String key = RedisKeys.DRAW_TASK_USER_LOCK_OUTCARD;
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        HashOperations hashOperations = template.opsForHash();
        if (add) {
            hashOperations.put(key, outId.toString(), System.currentTimeMillis() + "");
            return true;
        } else {
            if (template.hasKey(key)) {
                if (hashOperations.hasKey(key, outId.toString())) {
                    hashOperations.delete(key, outId.toString());
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 通过编号搜索账号 下发金额<br>
     * 所需金额 :可用额度 - 余额 or 今日出款限额 - 已出款限额 的最小值
     *
     * @param accountId
     * @return
     */
    @Override
    public BigDecimal availableAmountForDrawByAlias(Integer accountId) {
        log.info("账号id:{} ", accountId);
        Assert.notNull(accountId, "账号id为空");
        AccountBaseInfo baseInfo = getFromCacheById(accountId);
        if (baseInfo != null) {
            // 本次可下发金额
            Integer availAmount = accountChangeService.currCredits(baseInfo);
            log.info("账号id:{} 本次可下发金额:{}", accountId, availAmount);
            if (availAmount == null || availAmount < 0) {
                // BigDecimal currBalance =
                // allocateTransService.getCurrBalance(accountId);
                if (baseInfo.getLimitOut() == null || baseInfo.getLimitOut() == 0) {
                    log.info("账号id :{}今日出款限额:{}", accountId, baseInfo.getLimitOut());
                    return BigDecimal.ZERO;
                }
                BigDecimal outDaily = findAmountDailyByTotal(1, accountId);
                log.info("当日已出款限额:{}", outDaily);
                if (outDaily == null) {
                    log.info("当日已出款为空，返回当日出款限额LimitOut:{}", baseInfo.getLimitOut());
                    return new BigDecimal(baseInfo.getLimitOut());
                }
                BigDecimal availAmount3 = new BigDecimal(baseInfo.getLimitOut()).subtract(outDaily);
                log.info("可下发的金额:{}", availAmount3);
                return availAmount3.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : availAmount3;
            } else {
                return new BigDecimal(availAmount);
            }
        }
        return BigDecimal.ZERO;
    }

    /**
     * 下发任务 锁定 解锁 操作 可能多个锁定和解锁 所以需要批量操作
     *
     * @param userId
     * @param accountIds
     * @param type
     *            true 锁定 false 解锁
     * @return true 锁定成功 false 锁定失败
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean lockedOrUnlockByDrawTask(Integer userId, List<Integer> accountIds, boolean type) {
        if (CollectionUtils.isEmpty(accountIds)) {
            return false;
        }
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        ZSetOperations operations = template.opsForZSet();
        // draw:task:user:lock:card:5
        String key = RedisKeys.DRAW_TASK_USER_LOCK_CARD;
        boolean res = false;
        Object fromId = Integer.MAX_VALUE - 2;
        if (type) {
            // 锁定
            long ret = executeThirdDrawTaskScript(userId, accountIds);
            log.info("下发任务锁定参数:userId {} ,accountIds {} 结果:{} ", userId, accountIds.toString(), ret);
            if (ret == accountIds.size()) {
                res = true;
                saveLocked(userId, accountIds, true);
                // 生成TransLock
                String minutes = MemCacheUtils.getInstance().getSystemProfile()
                        .getOrDefault("OUTCARD_THIRD_DRAW_LOCKED_EXPIRETIME", "1440");
                for (Integer toId : accountIds) {
                    AccountBaseInfo baseInfo = getFromCacheById(toId);
                    if (null != baseInfo && AccountType.OutBank.getTypeId().equals(baseInfo.getType())) {
                        // 所需金额
                        Double needScore = template.opsForZSet().score(RedisKeys.ALLOC_NEW_OUT_NEED_ORI,
                                toId.toString());
                        Number[] score = null;
                        if (needScore == null) {
                            needScore = template.opsForZSet().score(RedisKeys.OUTCARD_NEED_THIRD_DRAW,
                                    baseInfo.getId().toString());
                            log.info("出款账号id:{},所需金额outcard:need:third:draw 缓存:{} ", toId, needScore);
                        } else {
                            score = deScore4Out(needScore);
                            log.info("出款账号id:{},所需金额AllocNewOutNeedOri缓存:{},解析后:{}", toId, needScore,
                                    ObjectMapperUtils.serialize(score));
                        }
                        // 如果是通过搜索编号获取账号 补单锁定的 needScore score 值为空 设置为任意值 88888
                        // 2019/09/09 如果是通过编号搜索的 所需金额: 可用额度 - 余额 or 今日出款限额 -
                        // 已出款限额 的最小值
                        Integer amount = (needScore == null || score == null)
                                ? availableAmountForDrawByAlias(toId).intValue()
                                : ((score == null || score.length < 3) ? needScore.intValue()
                                : Integer.valueOf(score[2].toString()));
                        log.info("出款账号id:{},所需金额 :{},金额整数:{}", toId, needScore, amount);
                        allocateTransService.lockForDrawTaskToOutCard(fromId, toId, userId, amount,
                                Integer.valueOf(minutes) * 60);
                        // 锁定之后 移除newNeedOri
                        removeNeedAmountOutCardForUnlocked(toId);
                        // 如果是出款卡 则保存锁定
                        saveLockedOutCardInDrawTask(toId, true);
                    }
                }
            }
        } else {
            // 解锁
            if (userId == null) {
                // 系统自动解锁
                log.info("系统自动解锁: userId {}", userId);
                key = key + ":*";
                Set<String> keys = template.keys(key);
                for (String key2 : keys) {
                    Iterator it = accountIds.iterator();
                    for (; it.hasNext();) {
                        String accountId = it.next().toString();
                        Long rank = operations.rank(key2, accountId);
                        if (null != rank && rank >= 0) {
                            Long rem = operations.remove(key2, accountId);
                            List<Integer> toDelete = new ArrayList() {
                                {
                                    add(Integer.valueOf(accountId));
                                }
                            };
                            // 删除锁定时间
                            saveLocked(Integer.valueOf(key2.split(":")[5]), toDelete, false);
                            // 删除 TransLock
                            Map<String, Map<String, String>> drawAmountAndFeeByAccountId = getDrawAmountAndFeeByAccountId(
                                    Integer.valueOf(accountId));
                            Map<String, String> map = drawAmountAndFeeByAccountId.get(accountId);
                            if (!CollectionUtils.isEmpty(map)) {
                                String thirId = map.get("thirdId");
                                if (StringUtils.isNotBlank(thirId)) {
                                    allocateTransService.unLockForDrawTaskToOutCard(fromId, Integer.valueOf(accountId));
                                    log.debug("下发任务页签  系统自动解锁 参数:userId :{} accountId:{}, 结果:{}", userId, accountId,
                                            rem);
                                }
                            }
                            // 如果是出款卡 删除锁定
                            saveLockedOutCardInDrawTask(Integer.valueOf(accountId), false);
                        }
                    }
                    res = true;
                }

            } else {
                key = key + ":" + userId;
                Iterator it = accountIds.iterator();
                for (; it.hasNext();) {
                    String accountId = it.next().toString();
                    Long rank = operations.rank(key, accountId);
                    if (null != rank && rank >= 0) {
                        Long rem = operations.remove(key, accountId);
                        // 删除锁定时间
                        List<Integer> toDelete = new ArrayList() {
                            {
                                add(Integer.valueOf(accountId));
                            }
                        };
                        saveLocked(userId, toDelete, false);
                        // 删除 设定
                        deleteSelectThirdRecord(Integer.valueOf(accountId));
                        // 删除 TransLock
                        TransLock transLock = allocateTransService.buildLockToId(false, Integer.valueOf(accountId));
                        if (!ObjectUtils.isEmpty(transLock)) {
                            fromId = Long.valueOf(transLock.getMsg().split(":")[1]);
                        }
                        allocateTransService.unLockForDrawTaskToOutCard(fromId, Integer.valueOf(accountId));
                        log.debug("下发任务页签  解锁 参数:userId :{} accountId:{}, 结果:{}", userId, accountId, rem);
                    }
                    // 如果是出款卡 则删除
                    AccountBaseInfo baseInfo = getFromCacheById(Integer.valueOf(accountId));
                    if (null != baseInfo && AccountType.OutBank.getTypeId().equals(baseInfo.getType())) {
                        saveLockedOutCardInDrawTask(Integer.valueOf(accountId), false);
                    }
                }
                res = true;
            }

        }
        return res;
    }

    /**
     * 判断出款卡 或者 下发卡 是否已经在 下发任务 页签锁定了
     *
     * @param accountId
     * @return
     */
    @Override
    public boolean isLockedInDrawTaskPage(Integer accountId) {
        if (null != accountId) {
            if (allLockedInDrawTask().contains(accountId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断出款卡 或者 下发卡 是否已经在 第三方入款 提现 页签锁定了
     *
     * @param accountId
     * @return
     */
    @Override
    public boolean isLockedInThirdInAccountPage(Integer accountId) {
        if (null != accountId) {
            if (allLockedInThirdInAccount().contains(accountId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Integer getPfTypeSubVal(Integer accountId) {
        if (accountId == null) {
            return null;
        }
        AccountBaseInfo baseInfo = getFromCacheById(accountId);
        if (null == baseInfo) {
            return null;
        }
        Integer subType = baseInfo.getSubType();
        if (null == subType) {
            return 0;
        }
        // null 0 0 13 1 14 2 15 3 16
        if (InBankSubType.IN_BANK_YSF.getSubType().equals(subType)) {
            return -16;
        }
        if (InBankSubType.IN_BANK_WECHATIN.getSubType().equals(subType)) {
            return -15;
        }
        if (InBankSubType.IN_BANK_ALIIN.getSubType().equals(subType)) {
            return -14;
        }
        if (InBankSubType.IN_BANK_DEFAULT.getSubType().equals(subType)) {
            return -13;
        }
        return null;
    }

    @Override
    public List<BizAccount> list(AccountListInputDTO inputDTO) {
        if (inputDTO == null) {
            return null;
        }
        try {
            StringBuilder sql = new StringBuilder(
                    "select  id ,account ,bank_name ,status ,type ,owner ,balance ,bank_balance ,handicap_id ,limit_in ,limit_out ,alias ,creator ,create_time ,update_time ,modifier ,bank_type ,holder ,sign ,hook ,curr_sys_level ,usable_balance ,hub ,remark ,bing ,lowest_out ,limit_balance ,peak_balance ,limit_out_one ,mobile ,gps ,flag ,limit_out_one_low ,limit_out_count ,sub_type ,dsf ,passage_id ,min_in_amount ,out_enable ,province ,city ,sign_ ,hook_ ,hub_ ,bing_ ,limit_percentage ,min_balance ,in_card_type from fundsTransfer.biz_account where 1=1   ");
            List<Integer> type = inputDTO.getTypeToArray();
            if (!CollectionUtils.isEmpty(type)) {
                if (type.size() == 1) {
                    sql.append(" and type=").append(type.get(0));
                } else {
                    sql.append(" and type in (");
                    commonSqlConcate(sql, type);
                }
            }
            List<Integer> status = inputDTO.getStatusToArray();
            if (!CollectionUtils.isEmpty(status)) {
                if (status.size() == 1) {
                    sql.append(" and status=").append(status.get(0));
                } else {
                    sql.append(" and status in (");
                    commonSqlConcate(sql, status);
                }
            }
            if (StringUtils.isNotBlank(inputDTO.getHolderType())) {
                if (inputDTO.getHolderType().equals("manual")) {
                    sql.append(" and holder is not null  ");
                } else if (inputDTO.getHolderType().equals("robot")) {
                    sql.append(" and holder is null ");
                }
            }
            List<Integer> level = inputDTO.getCurrSysLevel();
            if (!CollectionUtils.isEmpty(level)) {
                if (level.size() == 1) {
                    sql.append(" and curr_sys_level=").append(level.get(0));
                } else {
                    sql.append(" and curr_sys_level in (");
                    commonSqlConcate(sql, level);
                }
            }
            String bankType = inputDTO.getBankType();
            if (StringUtils.isNotBlank(bankType)) {
                sql.append(" and bank_type=" + bankType);
            }
            Sort.Direction direction = inputDTO.getSortDirection() != null
                    && Sort.Direction.ASC.ordinal() == inputDTO.getSortDirection() ? Sort.Direction.ASC
                    : Sort.Direction.DESC;
            String sort = direction.name();
            StringBuilder sortStr = new StringBuilder();
            // 冻结
            boolean flag = (null != inputDTO.getStatusToArray() && inputDTO.getStatusToArray().size() == 1
                    && inputDTO.getStatusToArray().get(0) == 3);
            // 第三方账号查询
            boolean third = !CollectionUtils.isEmpty(type) && type.contains(AccountType.InThird.getTypeId());
            sortStr.append("  order by ");
            if (flag) {
                sortStr.append("  update_time ").append(sort).append(",");
            } else {
                if (third) {
                    sortStr.append("  balance ").append(sort).append(",");
                } else {
                    sortStr.append("  bank_balance ").append(sort).append(",");
                }
            }
            sortStr.append(" curr_sys_level ").append(sort).append(",");
            sortStr.append(" handicap_id ").append(sort).append(",");
            sortStr.append(" status ").append(sort);

            String sortProperty = inputDTO.getSortProperty();
            if (!StringUtils.isBlank(sortProperty) && !"status".equals(sortProperty)) {
                sortStr = new StringBuilder();
                if ("statusImportant".equals(sortProperty)) {
                    sortStr.append(" order by status ").append(sort);
                } else {
                    if (!"incomeAmountDaily".equals(inputDTO.getSortProperty())
                            && !"outwardAmountDaily".equals(inputDTO.getSortProperty())) {
                        EntityManagerFactory entityManagerFactory = entityManager.getEntityManagerFactory();
                        SessionFactoryImpl sessionFactory = (SessionFactoryImpl) entityManagerFactory
                                .unwrap(SessionFactory.class);
                        EntityPersister entityPersister = sessionFactory.getEntityPersister("BizAccount");
                        SingleTableEntityPersister persister = (SingleTableEntityPersister) entityPersister;
                        sortProperty = persister.getPropertyColumnNames(sortProperty)[0];
                        sortStr.append(" order by " + sortProperty).append(sort);
                    }
                }
            }
            sql = sql.append(sortStr);
            log.debug("查询账号 sql:{}", sql.toString());
            List<BizAccount> res = entityManager.createNativeQuery(sql.toString(), BizAccount.class).getResultList();
            log.debug("查询结果:{}", res.toString());
            return res;
        } catch (MappingException e) {
            e.printStackTrace();
            log.debug("查询异常MappingException:", e);
        } catch (Exception e) {
            e.printStackTrace();
            log.debug("查询异常Exception:", e);
        }
        return null;
    }

    private void commonSqlConcate(StringBuilder sql, List<Integer> type) {
        for (int i = 0, size = type.size(); i < size; i++) {
            sql.append(type.get(i));
            if (i < size - 1) {
                sql.append(",");
            } else {
                sql.append(")");
            }
        }
    }

    /**
     * 从上一次执行完成之后 每1分钟执行一次 检查第三方下发锁定的出款卡是否已经下发完成 如果过期则解锁
     */
    @Scheduled(fixedDelay = 60000)
    public void unlockThirdDrawToOutCard() {
        String expireTimeSettedBySys = MemCacheUtils.getInstance().getSystemProfile()
                .getOrDefault("OUTCARD_THIRD_DRAW_LOCKED_EXPIRETIME", "1440");
        log.info("第三方下发到出款卡锁定出款卡的超时时间:{} 分钟!", expireTimeSettedBySys);
        if (StringUtils.isBlank(expireTimeSettedBySys)) {
            return;
        }
        Map<String, Map<String, Long>> lockedOutCard = allOutCardIdsLockedWithScoreTime();

        boolean noLockedOutCard = CollectionUtils.isEmpty(lockedOutCard);

        if (noLockedOutCard) {
            log.debug("没有锁定的出款卡!");
            return;
        }

        if (!noLockedOutCard) {
            long now = System.currentTimeMillis();
            for (Map.Entry<String, Map<String, Long>> entry : lockedOutCard.entrySet()) {
                String[] mKey = entry.getKey().split("\\|");
                log.debug(" 外层 map的key :{}", ObjectMapperUtils.serialize(mKey));
                String userId = mKey[0];
                String thirdAccountId = mKey[1];
                Map<String, Long> lockedByUser = entry.getValue();
                log.info("用户:{} 锁定的出款卡 ：{}", userId, ObjectMapperUtils.serialize(lockedByUser));
                if (CollectionUtils.isEmpty(lockedByUser)) {
                    continue;
                }
                for (Map.Entry<String, Long> entry1 : lockedByUser.entrySet()) {
                    String member = entry1.getKey();
                    Long lockedTime = entry1.getValue();
                    if (StringUtils.isNotBlank(member) && lockedTime > 0) {
                        boolean lockedExpired = (now - lockedTime) >= Long.valueOf(expireTimeSettedBySys) * 60 * 1000;
                        log.info("锁定的出款卡 id :{},锁定时间:{},系统设置超时时间:{},是否超时:{}", member, lockedTime, expireTimeSettedBySys,
                                lockedExpired);
                        if (lockedExpired) {
                            log.info("超时 执行删除解锁操作 !");
                            removeLockedExpiredBySystem(thirdAccountId, userId, member);
                            Integer accountId = Integer.valueOf(member);
                            // thirdAccountId+纳秒后3位
                            String thirdAccountNano = getFromIdWithNanoFromLockedHash(accountId);
                            // 把原来的锁定目标 解锁
                            allocateTransService.unLockForThirdDrawToOutCard(thirdAccountNano, accountId);
                            removeLockedHash(accountId);
                        }
                    }
                }

            }
        }
        // if (!noAllUnfinished) {
        // String key = RedisKeys.OUTCARD_NEED_THIRD_DRAW_UNFINISHED;
        // long now = System.currentTimeMillis();
        // StringRedisTemplate template = redisService.getStringRedisTemplate();
        // for (Map.Entry unfinished : allUnfinished.entrySet()) {
        // String hkey = unfinished.getKey().toString();
        // Long hval = Long.valueOf(unfinished.getValue().toString());
        // Long gap = now - hval;
        // boolean exceedTime = gap - Long.valueOf(expireTimeSettedBySys) * 60 *
        // 1000 >=
        // 0;
        // if (exceedTime) {
        // log.debug("超时 下发出款卡记录 未确认 出款卡id :{} ,下发时间 :{}", hkey, hval);
        // template.opsForHash().delete(key, hkey);
        // removeLockedHash(Integer.valueOf(hkey));
        // }
        // }
        // }

    }

    /**
     * 解锁 或者 提现完成之后 把缓存的系统余额更新金额删除
     *
     * @param accountId
     * @param toAccountId
     *            如果 为空 则可能是删除模态框或者最小化模态框导致的关闭
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAmountInputStored(Integer accountId, Integer toAccountId) {
        try {
            String key = RedisKeys.OUTCARD_NEED_THIRD_DRAW_LOCKED_AMOUNTTODRAW + ":" + accountId;
            StringRedisTemplate redisTemplate = redisService.getStringRedisTemplate();
            HashOperations operations = redisTemplate.opsForHash();
            if (toAccountId == null) {
                // boolean exist = redisTemplate.hasKey(key);
                // if (exist) {
                // Map<String, String> map = operations.entries(key);
                // if (!CollectionUtils.isEmpty(map)) {
                // BigDecimal amountAll = new BigDecimal(0);
                // for (Map.Entry<String, String> entry : map.entrySet()) {
                // amountAll = amountAll.add(new BigDecimal(entry.getValue()));
                // }
                // log.debug("模态框删除或者隐藏之后 加回系统余额 金额:{}", amountAll);
                // BizAccount account = getById(accountId);
                // if (null != account && amountAll.compareTo(BigDecimal.ZERO)
                // != 0) {
                // BigDecimal newBalance = account.getBalance()
                // .add(amountAll.setScale(0, BigDecimal.ROUND_DOWN));
                // account.setBalance(newBalance);
                // save(account);
                // log.debug("removeAmountInputStored>> id {}",
                // account.getId());
                // broadCast(account);
                // } else {
                // log.debug("第三方账号不存在 id:{}", accountId);
                // }
                // }
                redisTemplate.delete(key);
                // } else {
                // log.debug("模态框删除或者隐藏之后 要加回系统余额 但不存在缓存金额key:{}", key);
                // }
                return;
            }
            String hkey = "" + toAccountId;
            boolean exist = operations.hasKey(key, hkey);
            if (exist) {
                operations.delete(key, hkey);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 出款卡或者下发卡 解锁第三方账号
     *
     * @param userId
     * @param accountId
     * @param thirdAccountId
     * @return
     */
    @Override
    public boolean thirdAccountIdUnlockByOtherId(Integer userId, Integer accountId, Integer thirdAccountId) {
        log.debug("用户 :{} 出款卡或者下发卡:{}  解锁第三方账号:{} ", userId, accountId, thirdAccountId);
        if (null == accountId || null == thirdAccountId || null == userId) {
            return false;
        }
        try {
            AccountBaseInfo baseInfo = getFromCacheById(accountId);
            if (null == baseInfo) {
                log.debug("出款卡或者下发卡账号id:{} 不存在 ", accountId);
                return false;
            }
            AccountBaseInfo baseInfo1 = getFromCacheById(thirdAccountId);
            if (null == baseInfo1) {
                log.debug("第三方账号id :{}  不存在", thirdAccountId);
                return false;
            }
            StringRedisTemplate template = redisService.getStringRedisTemplate();
            JedisLock jedisLock = new JedisLock(template, "unlock_" + accountId + "_" + thirdAccountId, 5000, 10000);
            if (jedisLock == null) {
                log.debug("解锁 无法获取jedisLock ");
                return false;
            }
            if (jedisLock.acquire()) {
                if (AccountType.OutBank.getTypeId().equals(baseInfo.getType())) {
                    // 出款卡 解锁
                    long ret = unlockedThirdToDrawList(userId, accountId, thirdAccountId);
                    log.debug("出款卡id:{} 解锁第三方账号id:{} 结果:{}", accountId, thirdAccountId, ret);
                    if (ret == 1) {
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    // 下发卡或者其他卡 解锁
                    long ret = unlockThirdInAccount4Draw(userId, accountId, thirdAccountId);
                    log.debug("下发卡或者其他卡 id:{} 解锁第三方账号id:{} 结果:{}", accountId, thirdAccountId, ret);
                    if (ret == 1) {
                        return true;
                    } else {
                        return false;
                    }
                }
            } else {
                log.debug("解锁无法获取jedisLock 锁");
                return false;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            log.error("出款卡或者其他卡解锁第三方账号 异常:", e);
            return false;
        }

    }

    /**
     * 出款卡或者下发卡 锁定第三方账号
     *
     * @param userId
     * @param accountId
     * @param thirdAccountId
     * @return
     */
    @Override
    public boolean thirdAccountIdLockByOtherId(Integer userId, Integer accountId, Integer thirdAccountId) {
        if (null == accountId || null == thirdAccountId || null == userId) {
            return false;
        }
        try {
            AccountBaseInfo baseInfo = getFromCacheById(accountId);
            if (null == baseInfo) {
                log.debug("出款卡或者下发卡账号id:{} 不存在 ", accountId);
                return false;
            }
            AccountBaseInfo baseInfo1 = getFromCacheById(thirdAccountId);
            if (null == baseInfo1) {
                log.debug("第三方账号id :{}  不存在", thirdAccountId);
                return false;
            }
            StringRedisTemplate template = redisService.getStringRedisTemplate();
            JedisLock jedisLock = new JedisLock(template, "lock_" + accountId + "_" + thirdAccountId, 5000, 10000);
            if (jedisLock != null) {
                if (jedisLock.acquire()) {
                    if (!AccountType.OutBank.getTypeId().equals(baseInfo.getType())) {
                        long ret = lockThirdInAccount4Draw(userId, accountId, thirdAccountId);
                        log.debug("下发卡:{}, 锁定第三方卡:{},结果:{}", accountId, thirdAccountId, ret);
                        if (ret == 1L) {
                            return true;
                        } else {
                            return false;
                        }
                    } else {
                        long ret = lockOutCardNeedAmount(userId, accountId, thirdAccountId);
                        log.debug("出款卡:{},锁定第三方卡:{},结果:{}", accountId, thirdAccountId, ret);
                        if (ret == 1) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                } else {
                    log.debug("无法获取锁jedisLock");
                    return false;
                }
            } else {
                log.debug("出款卡或者下发卡 锁定第三方账号 获取jedisLock失败!");
                return false;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            log.error("出款卡或者下发卡 锁定第三方账号 异常:", e);
            return false;
        }
    }

    /**
     * 根据 被锁定的账号id 获取 第三方账号id
     *
     * @param accountIdLocked
     * @return
     */
    @Override
    public Integer getThirdAccountIdByLockedId(Integer accountIdLocked) {
        if (accountIdLocked == null) {
            return null;
        }
        AccountBaseInfo baseInfo = getFromCacheById(accountIdLocked);
        if (AccountType.OutBank.getTypeId().equals(baseInfo.getType())) {
            // 出款卡
            String key = RedisKeys.OUTCARD_NEED_THIRD_DRAW_LOCKED_HASH;
            StringRedisTemplate template = redisService.getStringRedisTemplate();
            Object thirdAccountId = template.opsForHash().get(key, accountIdLocked.toString());
            if (null != thirdAccountId) {
                String thirdAccountIdStr = thirdAccountId.toString();
                int len = thirdAccountIdStr.length();
                String res = thirdAccountIdStr.substring(0, len - 13);
                log.debug("出款账号id:{},第三方账号id:{}", accountIdLocked, thirdAccountId);
                return Integer.valueOf(thirdAccountIdStr);
            }
        } else {
            // 下发卡
            String key = RedisKeys.LOCK_TARGETACCOUNT_FOR_THIRDACCOUNTDRAW_KEY_PREX + ":*" + ":*";
            StringRedisTemplate redisTemplate = redisService.getStringRedisTemplate();
            Set<String> keys = redisTemplate.keys(key);

        }
        return null;
    }

    /**
     * 当在其他第三方 提现的时候 要加回原来的第三方系统余额 扣除当前提现的第三方系统余额
     *
     * @param oldThirdId
     * @param
     */
    @Override
    public void addSysBalanceByDrawOtherThirdAccount(Integer oldThirdId, Integer toAccountId) {
        String key = RedisKeys.OUTCARD_NEED_THIRD_DRAW_LOCKED_AMOUNTTODRAW + ":" + oldThirdId;
        StringRedisTemplate redisTemplate = redisService.getStringRedisTemplate();
        HashOperations operations = redisTemplate.opsForHash();
        String hkey = "" + toAccountId;
        if (redisTemplate.hasKey(key)) {
            Object oldAmount = operations.get(key, hkey);
            if (null != oldAmount) {
                BizAccount account = getById(oldThirdId);
                if (null != account) {
                    BigDecimal newBalance = account.getBalance().add(new BigDecimal(oldAmount.toString()));
                    account.setBalance(newBalance);
                    account = save(account);
                    log.debug("addSysBalanceByDrawOtherThirdAccount>> id {}", account.getId());
                    broadCast(account);
                    log.debug("在其他第三方提现 加回 系统余额 之后 账号信息: {}", account);
                } else {
                    log.debug("第三方账号信息为空 id:{}", oldThirdId);
                }
            }
        }
    }

    /**
     * 第三方账号 锁定和解锁之后 扣除系统余额 加回系统余额
     *
     * @param accountId
     *            第三方账号id
     * @param toAccountId
     *            出款账号id
     * @param amount
     *            金额 : 如果新的值等于缓存的值则不变，如果大于则扣除，如果小于则加回
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void dealSysBalanceLockedOrUnlocked(Integer accountId, Integer toAccountId, BigDecimal amount) {
        log.debug("锁定/解锁 扣除/加回 系统余额 参数:{}", accountId, toAccountId, amount);
        if (null == accountId) {
            return;
        }
        try {
            String key = RedisKeys.OUTCARD_NEED_THIRD_DRAW_LOCKED_AMOUNTTODRAW + ":" + accountId;
            StringRedisTemplate redisTemplate = redisService.getStringRedisTemplate();
            HashOperations operations = redisTemplate.opsForHash();
            String hkey = "" + toAccountId;
            String hval = amount.toString();
            if (amount.compareTo(BigDecimal.ZERO) >= 0) {
                log.debug("扣除系统余额 参数 :账号id {},金额 {}", accountId, amount);
                boolean exist = operations.hasKey(key, hkey);
                if (!exist) {
                    if (amount.compareTo(BigDecimal.ZERO) == 0) {
                        log.debug("首次输入 金额 为 0 ");
                        return;
                    }
                    // BizAccount account = getById(accountId);
                    // if (null != account) {
                    // BigDecimal newBalance =
                    // account.getBalance().subtract(amount);
                    // account.setBalance(newBalance);
                    // account = save(account);
                    // log.debug("dealSysBalanceLockedOrUnlocked>> id {}",
                    // account.getId());
                    // broadCast(account);
                    // log.debug("扣除 系统余额 之后:{}",
                    // ObjectMapperUtils.serialize(account));
                    // } else {
                    // log.debug("第三方账号为空 id:{}", accountId);
                    // }
                    operations.putIfAbsent(key, hkey, hval);
                } else {
                    log.debug("非首次输入 参数 :账号id {},金额 {}", accountId, amount);
                    Object amountStored = operations.get(key, hkey);
                    log.debug("从redis的key :{} 读取到的金额 :{}", hkey, amountStored);
                    if (null != amountStored) {
                        BigDecimal oldAmount = new BigDecimal(amountStored.toString());
                        // 旧值与新值的差值
                        BigDecimal gap = oldAmount.subtract(amount);
                        if (gap.compareTo(BigDecimal.ZERO) == 0) {
                            log.debug("缓存的金额 :{} 与本次传入的金额:{} 一致 ", oldAmount, amount);
                            return;
                        }
                        // BizAccount account = getById(accountId);
                        // if (null != account) {
                        // BigDecimal newBalance =
                        // account.getBalance().add(gap);
                        // account.setBalance(newBalance);
                        // account = save(account);
                        // log.debug("dealSysBalanceLockedOrUnlocked>> id {}",
                        // account.getId());
                        // broadCast(account);
                        if (amount.compareTo(BigDecimal.ZERO) == 0) {
                            // log.debug("非首次输入, 金额为0 ,删除缓存");
                            operations.delete(key, hkey);
                            return;
                        }
                        // log.debug("非首次输入, 金额为：{} ,重新缓存");
                        operations.put(key, hkey, hval);
                        // log.debug("加回系统余额 之后:{}",
                        // ObjectMapperUtils.serialize(account));
                        // } else {
                        // log.debug("第三方账号为空 id:{}", accountId);
                        // }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 查询是否开启 第三方下发到出款卡 或者 保存开启 关闭
     *
     * @param action
     *            没有值查询 有值则是前端开启或者关闭 1 开启 2 关闭
     * @return
     */
    @Override
    public String enableThirdDrawToOutCard(String action) {
        String key = RedisKeys.ENABLE_THIRD_DRAW_TO_OUTCARD;
        StringRedisTemplate redisTemplate = redisService.getStringRedisTemplate();
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        if (StringUtils.isNotBlank(action)) {
            // 保存开启 关闭
            valueOperations.set(key, StringUtils.trim(action));
            if ("2".equals(action)) {
                // 关闭
                redisTemplate.delete(RedisKeys.OUTCARD_NEED_THIRD_DRAW);
                redisTemplate.delete(RedisKeys.OUTCARD_NEED_THIRD_DRAW_LOCK);
            }
        }
        // 返回 值
        return valueOperations.get(key);
    }

    /**
     * 判断 出款卡 当日出款流水+余额 是否大于等于 当日出款限额
     *
     * @param accountId
     * @return
     */
    @Override
    public boolean isFlowBalaceLargerThanDailyOut(Integer accountId) {
        if (accountId == null) {
            return true;
        }
        boolean check = allocateTransService.exceedAmountSumDailyOutward(accountId);
        log.debug("校验账号id ：{} 当日出款流水 + 当前余额  大于等于 当日出款限额  结果:{}", accountId, check);
        return check;
    }

    /**
     * 判断 卡余额是否低于 返利网信用额度 PC使用 最低余额限制 lowest_out 不使用百分比
     *
     * @param accountId
     * @return
     */
    @Override
    public boolean belowPercentageNeedThirdDraw(Integer accountId) {
        // Preconditions.checkNotNull(accountId);
        // AccountBaseInfo baseInfo = getFromCacheById(accountId);
        // if (null == baseInfo) {
        // log.debug("账号不存在");
        // return false;
        // }
        // BigDecimal balance = allocateTransService.getCurrBalance(accountId);
        // log.debug("账号id:{},当前余额 :{}", accountId, balance);
        // if (balance == null || balance.compareTo(BigDecimal.ZERO) <= 0) {
        // return true;
        // }
        // String lowPercentage = MemCacheUtils.getInstance().getSystemProfile()
        // .getOrDefault("BIG_OUTCARD_NEED_THIRD_DRAW_PERCENT", "50");
        // log.debug("系统设置的最低百分比:{}", lowPercentage);
        // if (null != baseInfo.getFlag() && baseInfo.getFlag() == 2) {
        // log.debug("返利网账号");
        // Integer currentCredit = accountChangeService.currCredits(baseInfo);
        // log.debug("账号id:{},当前信用额度:{}", accountId, currentCredit);
        // if (currentCredit <= 0) {
        // log.debug("信用额度小于等于0");
        // return false;
        // }
        //
        // Float percent = currentCredit * (Integer.valueOf(lowPercentage) /
        // 100.00f);
        // int gap = balance.intValue() - percent.intValue();
        // log.debug("账号id:{},当前余额:{},当前信用额度百分比:{},差值:{}", accountId, balance,
        // percent,
        // gap);
        // if (gap <= 0) {
        // return true;
        // }
        // return false;
        // }
        // if (null != baseInfo.getFlag() && baseInfo.getFlag() == 0) {
        // Integer low = baseInfo.getLowestOut() == null ? 0 :
        // baseInfo.getLowestOut();
        // Integer peak = baseInfo.getPeakBalance() == null ? 0 :
        // baseInfo.getPeakBalance();
        // Float peakPercentage = peak * (Integer.valueOf(lowPercentage) /
        // 100.00F);
        // log.debug("pc账号:{},最低余额限制:{},当前余额:{},余额峰值:{},峰值百分比值:{}", accountId,
        // low,
        // balance, peak, peakPercentage);
        // boolean below = balance.intValue() - low <= 0;
        // boolean belowPer = balance.intValue() - peakPercentage.intValue() <=
        // 0;
        // if (below || belowPer) {
        // log.debug("当前余额小于最低出款额度lowestout:{},当前余额:{},小于峰值的百分比:{},{}", below,
        // balance,
        // peakPercentage, belowPer);
        // return true;
        // }
        // return false;
        // }
        return true;
    }

    /**
     * 页面提现完成之后 删除锁定的记录
     *
     * @param userId
     * @param outCardAccountId
     * @param thirdAccountId
     */
    @Override
    public void removeLockedRecordByUser(String userId, String outCardAccountId, String thirdAccountId) {
        if (StringUtils.isBlank(userId) || StringUtils.isBlank(outCardAccountId)
                || StringUtils.isBlank(thirdAccountId)) {
            log.debug("参数有空");
            return;
        }
        long threadId = Thread.currentThread().getId();
        String key = RedisKeys.OUTCARD_NEED_THIRD_DRAW_LOCK + userId + ":" + thirdAccountId;
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        ZSetOperations operations = template.opsForZSet();
        Long rank = operations.rank(key, outCardAccountId);
        log.info("threadId:{} 提现之后解锁 参数:outCardAccountId :{}  userId:{}  thirdAccountId:{} 判断是否是锁定的记录 rank :{}",
                threadId, outCardAccountId, userId, thirdAccountId, rank);
        if (null != rank && rank >= 0) {
            Long rem = operations.remove(key, outCardAccountId);
            log.info("threadId:{} 提现完成 删除锁定 结果 :{}", threadId, rem);
        } else {
            log.info("threadId:{}  在其他第三方提现  出款账号id:{} 第三方账号id:{} ", threadId, outCardAccountId, thirdAccountId);
            // 如果是其他第三方提现的 查询其他第三方账号 锁定的记录
            key = RedisKeys.OUTCARD_NEED_THIRD_DRAW_LOCK + userId + ":*";
            String member = outCardAccountId;
            Set<String> keys = template.keys(key);
            String oldThirId = null;
            if (!CollectionUtils.isEmpty(keys)) {
                for (String key2 : keys) {
                    rank = template.opsForZSet().rank(key2, member);
                    if (rank != null && rank >= 0) {
                        key = key2;
                        oldThirId = key2.split(":")[7];
                        break;
                    }
                }
            }
            // 如果有其他第三方锁定的 则更新
            if (rank != null && rank >= 0) {
                Integer outCardId = Integer.valueOf(outCardAccountId);
                Long rem = operations.remove(key, outCardAccountId);
                log.info("threadId:{}  提现 其他第三方：{} 锁定 删除原来的锁定 结果 :{}", threadId, oldThirId, rem);
                String oldThirdAccountIdWithTime = getFromIdWithNanoFromLockedHash(outCardId);
                log.info("threadId:{}  提现 其他第三方锁定 获取原来锁定 oldThirdAccountIdWithTime：{}", oldThirdAccountIdWithTime);
                // TransferAccountLock:10251562822586265:2380:5:0.01:10004:1562822586269:0:1562822586269
                TransLock lock = allocateTransService.buildLockToId(true, outCardId);
                log.info("threadId:{} 获取原来的锁TransLock :{} ", threadId, lock);
                boolean unlocked = allocateTransService.unLockForThirdDrawToOutCard(oldThirdAccountIdWithTime,
                        outCardId);
                log.info("threadId:{} 删除TransLok 结果:{}", threadId, unlocked);
                String minutes = MemCacheUtils.getInstance().getSystemProfile()
                        .getOrDefault("OUTCARD_THIRD_DRAW_LOCKED_EXPIRETIME", "1440");

                Long time = System.currentTimeMillis();
                String timeStr = time + "";
                String thirdAccountIdStr = thirdAccountId + timeStr;
                boolean llocked = allocateTransService.lockForThirdDrawToOutCard(thirdAccountIdStr, outCardId,
                        Integer.valueOf(userId), lock.getTransInt().intValue(), Integer.valueOf(minutes) * 60);
                log.info("threadId:{} 新增TransLok 结果:{}", threadId, llocked);
                removeLockedHash(outCardId);
                Boolean update = addLockedHash(outCardId, thirdAccountIdStr);
                // 处理系统余额
                addSysBalanceByDrawOtherThirdAccount(lock.getFrId(), outCardId);
                log.info(
                        "threadId:{}  提现 其他第三方锁定 更新outcard:need:third:draw:locked:hash  出款卡id:{} oldThirdAccountIdWithTime:{} , 新的thirdAccountIdStr:{} ,结果:{} ",
                        threadId, outCardId, oldThirdAccountIdWithTime, thirdAccountIdStr, update);
            }

        }
    }

    /**
     * 系统检测到锁定的出款卡过期了就删除
     *
     * @param member
     */
    @Override
    public void removeLockedExpiredBySystem(String thirdAccountId, String userId, String member) {
        String key = RedisKeys.OUTCARD_NEED_THIRD_DRAW_LOCK + userId + ":" + thirdAccountId;
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        ZSetOperations operations = template.opsForZSet();
        JedisLock jedisLock = null;
        try {
            Long rank = operations.rank(key, member);
            log.info("获取member的排名 以判断是否存在该member:{}", rank);
            if (rank == null || rank < 0) {
                return;
            }
            jedisLock = new JedisLock(template, "sys_remove_" + member, 5000, 60000);
            if (null == jedisLock) {
                log.info("系统删除过期锁定的出款卡无法获取jedislock实例!");
                return;
            }
            boolean locked = jedisLock.acquire();
            if (!locked) {
                log.info("系统删除过期锁定的出款卡无法获取jedis锁!");
                return;
            }
            Long rem = operations.remove(key, member);
            log.info("系统删除过期锁定的出款卡成功:rem:{},key:{},member:{}", rem, key, member);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("系统删除过期的锁定出款卡异常:", e);
            return;
        } finally {
            if (jedisLock != null && jedisLock.isLocked()) {
                jedisLock.release();
            }
        }
    }

    /**
     * 所有下发未确认的缓存
     *
     * @return
     */
    @Override
    public Map<String, Long> allUnfinished() {
        String key = RedisKeys.OUTCARD_NEED_THIRD_DRAW_UNFINISHED;
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        boolean exist = template.hasKey(key);
        if (!exist) {
            return new HashMap<>();
        }
        Map map = template.opsForHash().entries(key);
        log.debug("所有下发未确认的记录:{}", ObjectMapperUtils.serialize(map));
        return map;
    }

    /**
     * 第三方下发 --所有锁定的 出款卡 带有分数-锁定时间
     *
     * @return
     */
    @Override
    public Map<String, Map<String, Long>> allOutCardIdsLockedWithScoreTime() {
        String key = RedisKeys.OUTCARD_NEED_THIRD_DRAW_LOCK + "*:*";
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        ZSetOperations operations = template.opsForZSet();
        try {
            Set<String> keys = operations.getOperations().keys(key);
            if (CollectionUtils.isEmpty(keys)) {
                return new HashMap<>(0);
            }
            Map<String, Map<String, Long>> allLockedMap = new HashMap(4096);
            for (String key2 : keys) {
                Set<ZSetOperations.TypedTuple> locked = operations.rangeWithScores(key2, 0, -1);
                log.debug("某个用户锁定的出款卡集合:{}", ObjectMapperUtils.serialize(locked));
                if (CollectionUtils.isEmpty(locked)) {
                    continue;
                }
                String[] key2Array = key2.split(":");
                log.debug("某个用户的锁定的key:{}", ObjectMapperUtils.serialize(key2Array));
                String userId = key2Array[6];
                String thirdAccountId = key2Array[7];
                Map<String, Long> lockedMap = new HashMap<>(256);
                for (ZSetOperations.TypedTuple tuple : locked) {
                    String member = tuple.getValue().toString();
                    Double score = tuple.getScore();
                    log.debug("获取到当前人:{},第三方账号id:{},锁定的member:{}和score:{}", userId, thirdAccountId, member, score);
                    if (StringUtils.isNotBlank(member) && score > 0) {
                        lockedMap.put(member, score.longValue());
                    }
                }
                if (!CollectionUtils.isEmpty(lockedMap)) {
                    allLockedMap.put(userId + "|" + thirdAccountId, lockedMap);
                }
            }
            return allLockedMap;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("获取所有锁定的出款卡异常:", e);
            return new HashMap<>(0);
        }
    }

    /**
     * 判断 该出款账号是否还在下发待确认hash里
     *
     * @param accountId
     * @param accountType
     * @return true 表示在待确认的key里 false表示没有在待确认的队列里
     */
    @Override
    public boolean unfinished(Integer accountId, Integer accountType) {
        boolean res = false;
        Preconditions.checkNotNull(accountId);
        Preconditions.checkNotNull(accountType);
        Preconditions.checkState(AccountType.OutBank.getTypeId().equals(accountType), "非出款卡!");
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        HashOperations operations = template.opsForHash();
        try {
            String key = RedisKeys.OUTCARD_NEED_THIRD_DRAW_UNFINISHED;
            Set<String> hkeys = operations.keys(key);
            if (CollectionUtils.isEmpty(hkeys)) {
                return res;
            }
            for (String key1 : hkeys) {
                if (key1.equals(accountId.toString())) {
                    res = true;
                    log.debug("判断 该出款账号是否还在下发待确认hash里 返回结果:{}", key1);
                    break;
                }
            }
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("判断 该出款账号是否还在下发待确认hash里 异常:", e);
            return res;
        }
    }

    /**
     * 第三方下发--当前人锁定的所有出款卡
     *
     * @param userId
     * @return
     */
    @Override
    public List<String> outCardIdsLockedByUserId(Integer userId) {
        String key = RedisKeys.OUTCARD_NEED_THIRD_DRAW_LOCK + userId + ":*";
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        ZSetOperations zSetOperations = template.opsForZSet();
        try {
            Set<String> keys = zSetOperations.getOperations().keys(key);
            if (CollectionUtils.isEmpty(keys)) {
                return Lists.newArrayList();
            }
            List<String> locked = new LinkedList<>();
            for (String key1 : keys) {
                Set<String> locked1 = zSetOperations.reverseRange(key1, 0, -1);
                if (CollectionUtils.isEmpty(locked1)) {
                    continue;
                }
                locked1.stream().forEach(p -> {
                    // 如果满足 5865 条件2 则删除
                    Integer accountId = Integer.valueOf(p);
                    boolean below = belowPercentageNeedThirdDraw(accountId);
                    boolean isFlowBalaceLargerThanDailyOut = isFlowBalaceLargerThanDailyOut(accountId);
                    boolean isBlack = allocateTransService.checkBlack(accountId);
                    // 如果已被他人锁定 则删除锁定
                    boolean lockedByOtherMeans = isLockedByOtherMeans(accountId, userId);
                    boolean singleTimeDrawAble = singleTimeDrawAbleFlag(accountId);
                    log.debug("账号id:{}   是否 below:{}, 是否黑名单:{},单次可下发金额小于:{}", accountId, below, !isBlack,
                            singleTimeDrawAble);
                    removeOutcardId(zSetOperations, locked, key1, p, accountId, below, isFlowBalaceLargerThanDailyOut,
                            isBlack, lockedByOtherMeans, singleTimeDrawAble);
                });
            }
            log.debug("当前人锁定的出款卡集合:{}", ObjectMapperUtils.serialize(locked));
            return locked;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("获取当前人锁定的出款卡异常:", e);
            return Lists.newArrayList();
        }
    }

    /**
     * 第三方下发 --所有锁定的 出款卡
     *
     * @return
     */
    @Override
    public List<String> allOutCardIdsLocked() {
        String key = RedisKeys.OUTCARD_NEED_THIRD_DRAW_LOCK + "*:*";
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        ZSetOperations zSetOperations = template.opsForZSet();
        try {
            Set<String> keys = template.keys(key);
            if (CollectionUtils.isEmpty(keys)) {
                return Lists.newArrayList();
            }
            List<String> accountIds = new ArrayList<>();
            for (String key1 : keys) {
                Set<String> ids = zSetOperations.range(key1, 0, -1);
                log.debug("根据key:{},获取到的锁定的集合:{}", key1, ids);
                if (CollectionUtils.isEmpty(ids)) {
                    continue;
                }
                ids.stream().forEach(p -> {
                    Integer accountId = Integer.valueOf(p);
                    // 如果满足 5865 条件2 则删除
                    boolean below = belowPercentageNeedThirdDraw(accountId);
                    boolean isFlowBalaceLargerThanDailyOut = isFlowBalaceLargerThanDailyOut(accountId);
                    boolean isBlack = allocateTransService.checkBlack(accountId);
                    // 如果已被他人锁定 则删除锁定
                    boolean isLockedByOtherMeans = isLockedByOtherMeans(accountId, AppConstants.USER_ID_1_ADMIN);
                    boolean singleTimeDrawAble = singleTimeDrawAbleFlag(accountId);
                    log.debug("账号id:{} 是否被其他方式锁定:{}  是否 below:{}, 是否黑名单:{},单次可下发金额小于:{},流水+余额是否大于当日出款限额:{}", accountId,
                            isLockedByOtherMeans, below, !isBlack, singleTimeDrawAble, isFlowBalaceLargerThanDailyOut);
                    removeOutcardId(zSetOperations, accountIds, key1, p, accountId, below,
                            isFlowBalaceLargerThanDailyOut, isBlack, isLockedByOtherMeans, singleTimeDrawAble);
                });

            }
            log.debug("所有锁定的出款卡集合:{}", ObjectMapperUtils.serialize(accountIds));
            return accountIds;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("获取所有锁定的出款卡集合异常:", e);
            return Lists.newArrayList();
        }
    }

    private void removeOutcardId(ZSetOperations zSetOperations, List<String> accountIds, String key1, String p,
                                 Integer accountId, boolean below, boolean isFlowBalaceLargerThanDailyOut, boolean isBlack, boolean locked,
                                 boolean singleTimeDrawAble) {
        if (locked || !below || isFlowBalaceLargerThanDailyOut || !isBlack || !singleTimeDrawAble) {
            zSetOperations.remove(key1, p);
            removeNeedAmountOutCard(accountId);
            removeLockedHash(accountId);
        } else {
            accountIds.add(p);
        }
    }

    /**
     * 提现确认 删除锁定 添加到待确认队列
     *
     * @param unlocker
     * @param accountId
     * @param thirdAccountId
     */
    @Override
    public void unlockedAndAddUnfinished(Integer unlocker, Integer accountId, Integer thirdAccountId) {
        if (null == unlocker || null == accountId || null == thirdAccountId) {
            log.debug("提现确认 解锁并添加到待确认缓存中 参数有空 unlocker:{} accountId:{} thirdAccountId:{}", unlocker, accountId,
                    thirdAccountId);
            return;
        }
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        JedisLock jedisLock = null;
        HashOperations hashOperations = template.opsForHash();
        try {
            long time = System.currentTimeMillis();
            jedisLock = new JedisLock(template, "rm_locked_add_toconfirm_" + accountId);
            boolean lock = jedisLock.acquire();
            if (lock) {
                String key = RedisKeys.OUTCARD_NEED_THIRD_DRAW_UNFINISHED;
                String hkey = accountId.toString();
                String hval = time + "";
                Boolean hset = hashOperations.putIfAbsent(key, hkey, hval);
                log.debug("提现确认 解锁并添加到待确认缓存中 返回结果:{}", hset);
                if (hset != null && hset) {
                    // 移除锁定
                    removeLockedRecordByUser(unlocker.toString(), accountId.toString(), thirdAccountId.toString());
                    // 移除需要下发zset记录
                    removeNeedAmountOutCard(accountId);
                }

            } else {
                log.debug("提现确认 解锁并添加到待确认缓存中 获取不到jedislock:{}", lock);
            }
            return;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("提现确认 解锁并添加到待确认缓存中 异常:", e);
            return;
        } finally {
            if (jedisLock != null && jedisLock.isLocked()) {
                jedisLock.release();
            }
        }
    }

    /**
     * 删除 锁定 hash : key是出款卡id val是fromId加时间纳秒后三位
     *
     * @param accountId
     */
    @Override
    public void removeLockedHash(Integer accountId) {
        if (accountId == null) {
            return;
        }
        String key2 = RedisKeys.OUTCARD_NEED_THIRD_DRAW_LOCKED_HASH;
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        HashOperations hashOperations = template.opsForHash();
        boolean exist = template.hasKey(key2);
        if (exist) {
            boolean hkey = hashOperations.hasKey(key2, accountId.toString());
            if (hkey) {
                hashOperations.delete(key2, accountId.toString());
            }
        }
    }

    /**
     * 如果是内部转账 流水匹配之后 删除待确认队列里的记录 人工匹配 也需要调用此方法
     *
     * @param thirdAccountId
     * @param outAccountId
     */
    @Override
    public void removeLockedRecordThirdDrawToOutCard(Integer thirdAccountId, Integer outAccountId) {
        if (outAccountId == null || thirdAccountId == null) {
            return;
        }
        try {
            AccountBaseInfo baseInfo = getFromCacheById(outAccountId);
            if (baseInfo == null || null == baseInfo.getType()
                    || !AccountType.OutBank.getTypeId().equals(baseInfo.getType())) {
                log.debug("账号id :{} 不存在账号记录 或者 该账号不是出款卡");
                return;
            }
            AccountBaseInfo baseInfo1 = getFromCacheById(thirdAccountId);
            if (baseInfo1 == null) {
                log.debug("第三方账号不存在 id :{} ", thirdAccountId);
                return;
            }
            log.debug("流水 匹配 删除 锁定待确认的 第三方下发到出款卡:{} 记录.", outAccountId);
            removeBySystem(outAccountId);
            String thirdAccountNano = getFromIdWithNanoFromLockedHash(outAccountId);
            boolean unlocked = allocateTransService.unLockForThirdDrawToOutCard(thirdAccountNano, outAccountId);
            if (!unlocked) {
                log.error("第三方下发到出款卡 自动解锁 失败  fromId :{} , toId :{}", thirdAccountId, outAccountId);
                return;
            }
            removeLockedHash(outAccountId);
        } catch (Exception e) {
            log.error("下发处理 异常:", e);
        }
    }

    /**
     * 系统自动匹配的流水 从锁定的记录中删除掉
     *
     * @param accountId
     * @return
     */
    @Override
    public long removeBySystem(Integer accountId) {
        String key = RedisKeys.OUTCARD_NEED_THIRD_DRAW_UNFINISHED;
        String field = accountId.toString();
        long res = 1L;
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        HashOperations hashOperations = template.opsForHash();
        JedisLock jedisLock = null;
        try {
            boolean exist = template.hasKey(key);
            if (!exist) {
                log.debug("系统流水匹配或者其他原因 没有下发待确认的hash");
                return -2L;
            }
            Boolean existField = hashOperations.hasKey(key, field);
            if (null == existField || !existField) {
                log.debug("系统流水匹配或者其他原因 移除下发待确认队列 没有待确认的记录!");
                return res;
            }
            jedisLock = new JedisLock(template, "sys_remove_unfinished_lock" + accountId, 5000, 10000);
            Preconditions.checkNotNull(jedisLock);
            boolean jlock = jedisLock.acquire();
            if (!jlock) {
                log.debug("系统流水匹配或者其他原因 下发待确认的出款卡id:{}, 获取不到jedisLock!", accountId);
                return -2L;
            }
            Long hdel = hashOperations.delete(key, field);
            // removeLockedHash(accountId);
            log.debug("系统流水匹配或者其他原因 删除下发待确认记录:{} 结果:{}", accountId, hdel);
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("系统流水匹配或者其他原因 移除下发待确认队列 异常:", e);
            res = -3L;
        } finally {
            if (jedisLock != null && jedisLock.isLocked()) {
                jedisLock.release();
            }
        }
        return res;
    }

    /**
     * 页面解锁 或者 流水匹配 或者 系统解锁 调用
     *
     * @param unlocker
     *            系统自动匹配的流水 则是 -1 页面操作解锁的是操作人id
     * @param accountId
     * @param accountType
     * @return
     */
    @Override
    public long removeLockedNeedAmountOutCard(Integer unlocker, Integer accountId, Integer accountType,
                                              Integer thirdAccountId) {
        if (null == unlocker || null == accountId) {
            log.info("解锁参数有空:{},{}", unlocker, accountId);
            return -4L;
        }
        if (!AccountType.OutBank.getTypeId().equals(accountType)) {
            log.info("需要解锁的卡类型不是出款卡:{},{}", accountType, AccountType.findByTypeId(accountType).getMsg());
            return -5L;
        }
        if (unlocker.equals(AppConstants.USER_ID_1_ADMIN)) {
            log.info("系统流水匹配或者其他操作解锁,unlocker:{},accountid:{}", unlocker, accountId);
            long res = removeBySystem(accountId);
            // 把原来的锁定目标 解锁
            String thirdAccountNano = getFromIdWithNanoFromLockedHash(accountId);
            allocateTransService.unLockForThirdDrawToOutCard(thirdAccountNano, accountId);
            log.info("系统解锁 结果:{}", res);
            removeLockedHash(accountId);
            return res;
        }
        // 页面操作 解锁 可能是同一个第三方账号解锁 也可以能是其他地方解锁
        String key = RedisKeys.OUTCARD_NEED_THIRD_DRAW_LOCK + unlocker + ":" + thirdAccountId;
        String member = accountId.toString();
        long res = 1L;
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        boolean exist = template.hasKey(key);
        if (!exist) {
            // 查询其他第三方账号 锁定的记录
            key = RedisKeys.OUTCARD_NEED_THIRD_DRAW_LOCK + unlocker + ":*";
            Set<String> keys = template.keys(key);
            if (!CollectionUtils.isEmpty(keys)) {
                for (String key2 : keys) {
                    Long rank = template.opsForZSet().rank(key2, member);
                    if (rank != null && rank >= 0) {
                        key = key2;
                        exist = true;
                        thirdAccountId = Integer.valueOf(key2.split(":")[7]);
                        break;
                    }
                }
            }
            if (!exist) {
                log.info("锁定的key ：{} 不存在", key);
                return -6L;
            }
        }
        if (thirdAccountId == null) {
            log.info("锁定的key 和 key2 ：{} 不存在", key);
            return -6L;
        }
        ZSetOperations zSetOperations = template.opsForZSet();
        try {
            // 把原来的锁定目标 解锁
            String thirdAccountNano = getFromIdWithNanoFromLockedHash(accountId);
            boolean unlocked = allocateTransService.unLockForThirdDrawToOutCard(thirdAccountNano, accountId);
            if (!unlocked) {
                // 解锁失败 重试
                log.info("解锁 llockUpdStatus 失败 , fromId :{}, toId :{}", thirdAccountId, accountId);
                return -4L;
            }
            Long rank = zSetOperations.rank(key, member);
            if (null != rank && rank >= 0) {
                res = srem(zSetOperations, key, member);
                removeLockedHash(accountId);
                log.info("页面操作 解锁结果:{}", res);
                return res;
            }
            log.info("页面操作 不存在锁定记录:{}  解锁结果:{}", member, res);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("解锁异常:", e);
            res = -3L;
        }
        return res;
    }

    private long srem(ZSetOperations operations, String key, String member) {
        long res;
        try {
            Long ismember = operations.rank(key, member);
            if (ismember != null && ismember >= 0) {
                res = operations.remove(key, member);
                log.debug("删除结果:{}", res);
            } else {
                // 如果是解锁动作 返回-2说明锁不存在无法解锁
                log.debug("不存在该member不能删除:{}", ismember);
                res = -2L;
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("删除异常:", e);
            res = -3L;
        }
        return res;
    }

    /**
     * 解锁
     *
     * @param unlocker
     * @param accountId
     */
    @Override
    public Long unlockedThirdToDrawList(Integer unlocker, Integer accountId, Integer thirdAccountId) {
        Long unlocked = removeLockedNeedAmountOutCard(unlocker, accountId, AccountType.OutBank.getTypeId(),
                thirdAccountId);
        log.info("解锁参数 ：unlocker {},accountId {},thirdAccountId {} ,结果:{}", unlocker, accountId, thirdAccountId,
                unlocked);
        return unlocked;
    }

    /**
     * 第三方账号下发完成之后 移除出款账号所需金额队列
     *
     * @param accountId
     */
    @Override
    public void removeNeedAmountOutCardForUnlocked(Integer accountId) {
        log.debug("第三方入款锁定出款卡之后 从 AllocNewOutNeedOri  移除 accountId:{}", accountId);
        try {
            allocateTransService.removeNeedAmountOutCard(accountId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 第三方账号下发完成之后 移除出款账号所需金额队列
     *
     * @param accountId
     */
    @Override
    public void removeNeedAmountOutCard(Integer accountId) {
        String key = RedisKeys.OUTCARD_NEED_THIRD_DRAW;
        String member = accountId.toString();
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        ZSetOperations zSetOperations = template.opsForZSet();
        try {
            Long rem = srem(zSetOperations, key, member);
            log.debug("从队列中移除出款账号所需金额 结果:{}", rem);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("移除出款账号所需金额队列 异常!", e);
        }
    }

    /**
     * 判断 是否已经被其他人锁定了
     *
     * @param thirdAccountId
     * @param accountId
     * @return
     */
    @Override
    public boolean checkLockedByOther(Integer thirdAccountId, Integer accountId) {
        String key = RedisKeys.OUTCARD_NEED_THIRD_DRAW_LOCK + "*" + ":" + thirdAccountId;
        StringRedisTemplate redisTemplate = redisService.getStringRedisTemplate();
        Set<String> keys = redisTemplate.keys(key);
        if (CollectionUtils.isEmpty(keys)) {
            return false;
        }
        ZSetOperations operations = redisTemplate.opsForZSet();
        for (String key2 : keys) {
            Long rank = operations.rank(key2, accountId.toString());
            if (null != rank && rank >= 0) {
                log.debug("锁定记录里 存在出款账号id:{}", accountId);
                return true;
            }
        }
        return false;
    }

    @Override
    public String getFromIdWithNanoFromLockedHash(Integer accountId) {
        if (accountId != null) {
            String key = RedisKeys.OUTCARD_NEED_THIRD_DRAW_LOCKED_HASH;
            StringRedisTemplate template = redisService.getStringRedisTemplate();
            if (template.opsForHash().hasKey(key, accountId.toString())) {
                Object fromId = template.opsForHash().get(key, accountId.toString());
                log.info("根据toId:{} 获取缓存的fromId :{}", accountId, fromId);
                String fromIdStr = fromId.toString();
                return fromIdStr;
            }
        }
        return null;
    }

    @Override
    public Integer getFromIdFromLockedHash(Integer toId) {
        if (toId != null) {
            String key = RedisKeys.OUTCARD_NEED_THIRD_DRAW_LOCKED_HASH;
            StringRedisTemplate template = redisService.getStringRedisTemplate();
            if (template.opsForHash().hasKey(key, toId.toString())) {
                Object fromId = template.opsForHash().get(key, toId.toString());
                log.debug("根据toId:{} 获取缓存的fromId :{}", toId, fromId);
                String fromIdStr = fromId.toString();
                // 缓存 第三方账号+当前时间毫秒数13位
                fromIdStr = fromIdStr.substring(0, fromIdStr.length() - 13);
                log.debug("返回的 fromID :{}", fromIdStr);
                return Integer.valueOf(fromIdStr);
            }
        }
        return null;
    }

    @Override
    public Boolean addLockedHash(Integer accountId, String thirdAccountId) {
        if (accountId == null || thirdAccountId == null) {
            return false;
        }
        String key = RedisKeys.OUTCARD_NEED_THIRD_DRAW_LOCKED_HASH;
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        Boolean add = template.opsForHash().putIfAbsent(key, accountId.toString(), thirdAccountId);
        return add;
    }

    /**
     * 第三方账号下发到指定出款账号 锁定 如果在已下发待确认队列里 则需要等到流水匹配了之后才能锁定下一笔
     *
     * @param locker
     * @param accountId
     * @param thirdAccountId
     * @return
     */
    @Override
    public long lockOutCardNeedAmount(Integer locker, Integer accountId, Integer thirdAccountId) {
        long lockRes = 0L;
        if (locker == null || accountId == null || thirdAccountId == null) {
            return lockRes;
        }
        // boolean isInNewNeed = isInNewOutNeedOri(accountId);
        // log.debug("判断出款账号id:{} 在需要下发的集合里? {}", accountId, isInNewNeed);
        // if (!isInNewNeed) {
        // // 已经被其它锁定了
        // removeNeedAmountOutCard(accountId);
        // removeLockedNeedAmountOutCard(locker, accountId,
        // AccountType.OutBank.getTypeId(), thirdAccountId);
        // return -6L;
        // }
        // 判断是否已经绑定
        // BizAccountBinding byAccountIdAndBindedId =
        // accountBindingService.findByAccountIdAndBindedId(thirdAccountId,
        // accountId);
        // if (byAccountIdAndBindedId == null) {
        // log.debug("第三方账号 id:{},出款卡id:{} 还没绑定,不能锁定!", thirdAccountId,
        // accountId);
        // return -9L;
        // }
        // 判断 是否被其他方式锁定了
        boolean alreadyLocked = isLockedByOtherMeans(accountId, locker);
        if (alreadyLocked) {
            log.info("账号id:{} ,已被其他 锁定 ", accountId);
            // 已被其他方式锁定 就从需要钱的队列删除
            removeNeedAmountOutCard(accountId);
            return -6L;
        }
        // 判断是否超额 5865 条件2
        boolean below = belowPercentageNeedThirdDraw(accountId);
        log.info("检查是否在可以下发的金额范围内 below :{}", below);
        if (!below) {
            removeNeedAmountOutCard(accountId);
            return -7L;
        }
        boolean isFlowBalaceLargerThanDailyOut = isFlowBalaceLargerThanDailyOut(accountId);
        log.info("判断出款账号id:{},当日流水加余额是否大于出款限额:{}", accountId, isFlowBalaceLargerThanDailyOut);
        if (isFlowBalaceLargerThanDailyOut) {
            removeNeedAmountOutCard(accountId);
            return -7L;
        }
        // 判断是否被其他人锁定了
        boolean lockedByOther = checkLockedByOther(thirdAccountId, accountId);
        log.info("判断出款账号id:{},是否被其他人锁定:{}", accountId, lockedByOther);
        if (lockedByOther) {
            return -8L;
        }
        String key = RedisKeys.OUTCARD_NEED_THIRD_DRAW_LOCK + locker + ":" + thirdAccountId;
        String member = accountId.toString();
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        HashOperations operations = template.opsForHash();
        ZSetOperations zSetOperations = template.opsForZSet();
        JedisLock lock = null;
        try {
            // 判断是否在下发待确认队列里
            boolean unfinished = operations.hasKey(RedisKeys.OUTCARD_NEED_THIRD_DRAW_UNFINISHED, member);
            if (unfinished) {
                log.info("上一笔下发未匹配无法锁定!");
                return -5L;
            }
            lock = new JedisLock(template, "lock_third_draw_to_" + accountId, 5000, 10000);
            boolean locked = lock.acquire();
            if (locked) {
                Long rank = zSetOperations.rank(key, member);
                if (rank != null && rank >= 0) {
                    log.info("不能重复锁定账号id:{},key :{},member:{}", accountId, key, member);
                    // 不能重复锁定
                    lockRes = -1L;
                    return lockRes;
                } else {
                    String minutes = MemCacheUtils.getInstance().getSystemProfile()
                            .getOrDefault("OUTCARD_THIRD_DRAW_LOCKED_EXPIRETIME", "1440");
                    // 添加到原来的锁定目标里 为了使一个第三方可以下发多个出款卡 这里传入的 fromId =
                    // thirdAccountId
                    // +当前时间毫秒数
                    Long time = System.currentTimeMillis();
                    String timeStr = time + "";
                    String thirdAccountIdStr = thirdAccountId + timeStr;
                    Double needScore = template.opsForZSet().score(RedisKeys.ALLOC_NEW_OUT_NEED_ORI,
                            accountId.toString());
                    Number[] score = null;
                    if (needScore == null) {
                        needScore = template.opsForZSet().score(RedisKeys.OUTCARD_NEED_THIRD_DRAW,
                                accountId.toString());
                        log.info("出款账号id:{},所需金额outcard:need:third:draw 缓存:{} ", accountId, needScore);
                    } else {
                        score = deScore4Out(needScore);
                        log.info("出款账号id:{},所需金额AllocNewOutNeedOri缓存:{},解析后:{}", accountId, needScore,
                                ObjectMapperUtils.serialize(score));
                    }
                    Integer amount = (score == null || score.length < 3) ? needScore.intValue()
                            : Integer.valueOf(score[2].toString());
                    log.info("出款账号id:{},所需金额 :{},金额整数:{}", accountId, needScore, amount);
                    boolean llocked = allocateTransService.lockForThirdDrawToOutCard(thirdAccountIdStr, accountId,
                            locker, amount, Integer.valueOf(minutes) * 60);
                    if (!llocked) {
                        log.info("添加到 llock 失败! fromId :{} ,toId :{}", thirdAccountId, accountId);
                        return -10L;
                    }
                    addLockedHash(accountId, thirdAccountIdStr);
                    // 添加都锁定zset中
                    Boolean add = zSetOperations.add(key, member, time);
                    log.info("锁定参数:key {},member {}, scort:{} 结果:{}", key, member, time, add);
                    if (null != add && add) {
                        lockRes = 1L;
                        // 30分钟锁定超时 防止确认提现失败的时候不解锁
                        template.expire(key, 30, TimeUnit.MINUTES);
                        // 锁定之后 移除newNeedOri
                        removeNeedAmountOutCardForUnlocked(accountId);
                    }
                    log.info("锁定结果:{}", lockRes);
                }
            } else {
                log.info("第三方锁定下发到出款卡获取锁失败: locked:{}", locked);
                lockRes = -2L;
                return lockRes;
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("锁定异常:", e);
            lockRes = -3L;
            return lockRes;
        } finally {
            if (null != lock && lock.isLocked()) {
                lock.release();
            }
        }
        return lockRes;
    }

    private Number[] deScore4Out(Double score) {
        String sc = CommonUtils.getNumberFormat(7, 9).format(score);
        return new Number[] { Integer.valueOf(sc.substring(0, 1)),
                Long.valueOf(sc.substring(1, 7) + sc.substring(8, 12) + "000"), Integer.valueOf(sc.substring(12, 17)) };
    }

    /**
     * 所有 未锁定和已锁定 已下发待确认的出款卡账号id
     *
     * @return
     */
    @Override
    public List<Integer> allOutCardIdsNeedThirdDrawOrUnfinished() {
        String key = RedisKeys.OUTCARD_NEED_THIRD_DRAW;
        String key2 = RedisKeys.OUTCARD_NEED_THIRD_DRAW_UNFINISHED;
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        boolean exist = template.hasKey(key);
        boolean exist2 = template.hasKey(key2);
        if (!exist && !exist2) {
            return Lists.newArrayList();
        }
        List<Integer> ids = new ArrayList<>();
        if (exist) {
            Set<String> members = template.opsForZSet().range(key, 0, -1);
            if (!CollectionUtils.isEmpty(members)) {
                ids.addAll(members.stream().map(p -> Integer.valueOf(p)).collect(Collectors.toList()));
            }
        }
        if (exist2) {
            Set<Object> keys = template.opsForHash().keys(key2);
            if (!CollectionUtils.isEmpty(keys)) {
                ids.addAll(keys.stream().map(p -> Integer.valueOf(p.toString())).collect(Collectors.toList()));
            }
        }
        return ids;
    }

    /**
     * 第三方下发的时候 优先选择需要下发的出款卡 以所需金额大小降序获取
     *
     * @return
     */
    @Override
    public List<Integer> needThirdDrawToOutCardIds() {
        String key = RedisKeys.OUTCARD_NEED_THIRD_DRAW;
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        ZSetOperations operations = template.opsForZSet();
        try {
            boolean exist = template.hasKey(key);
            if (!exist) {
                log.debug("不存在出款卡需要下发");
                return Lists.newArrayList();
            }
            Set<ZSetOperations.TypedTuple> accountIds = operations.reverseRangeWithScores(key, 0, -1);
            log.debug("从缓存中获取到的需要下发的出款卡集合:{}", ObjectMapperUtils.serialize(accountIds));
            if (CollectionUtils.isEmpty(accountIds)) {
                return Lists.newArrayList();
            }
            // 所有需要下发的出款卡id
            List<Integer> accountIdsTarget = accountIds.stream().filter(p -> p.getValue() != null)
                    .map(p -> Integer.valueOf(p.getValue().toString())).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(accountIdsTarget))
                return Lists.newArrayList();

            List<String> locked = allOutCardIdsLocked();
            if (!CollectionUtils.isEmpty(locked)) {
                accountIdsTarget = accountIdsTarget.stream().filter(p -> !locked.contains(p.toString()))
                        .collect(Collectors.toList());
            }
            if (CollectionUtils.isEmpty(accountIdsTarget))
                return Lists.newArrayList();

            // 0. 状态是在用的
            java.util.function.Predicate<Integer> predicate0 = p -> {
                AccountBaseInfo baseInfo = getFromCacheById(p);
                return baseInfo != null && AccountStatus.Normal.getStatus().equals(baseInfo.getStatus());
            };
            // 1. 2019-09-16 是否被他人锁定
            // 1.1 这些账号id的transLock
            Map<Integer, TransLock> allToIdsTransLocks = allocateTransService.allToIdsTransLocks(accountIdsTarget);
            // 1.2 在第三方入款里锁定的下发卡 或者出款卡
            List<Integer> allToIdsLockedByThirdIn = allToIdsLockedByThirdIn(accountIdsTarget);
            java.util.function.Predicate<Integer> predicate11 = CollectionUtils.isEmpty(allToIdsLockedByThirdIn) ? null
                    : p -> !allToIdsLockedByThirdIn.contains(p);
            // 1.3 没有被锁的即没有TransLock
            java.util.function.Predicate<Integer> predicate12 = CollectionUtils.isEmpty(allToIdsTransLocks) ? null
                    : p -> allToIdsTransLocks.get(p) != null && allToIdsTransLocks.get(p).getTransInt() != null;
            java.util.function.Predicate predicate1 = p -> p != null;
            if (predicate11 != null)
                predicate1.and(predicate11);
            if (predicate12 != null)
                predicate1.and(predicate12);
            // 2 当日流水条件
            java.util.function.Predicate<Integer> predicate2 = predicate2(accountIdsTarget);

            // 3 2019-09-16
            Set<String> black = (Set<String>) allocateOutwardTaskService.getAllocNeetCache("BLACK");
            // 3.1 不在黑名单里
            java.util.function.Predicate<Integer> predicate3 = CollectionUtils.isEmpty(black) ? null
                    : p -> !black.contains(template.hasKey(RedisKeys.gen4TransBlack(0, p, 0)));

            // 4 2019-09-16
            Map<Integer, Integer> allCurrCredits = accountChangeService.allCurrCredits(accountIdsTarget);
            // 4.1 信用额度大于0
            java.util.function.Predicate<Integer> predicate4 = CollectionUtils.isEmpty(allCurrCredits) ? null
                    : p -> allCurrCredits.get(p) != null && allCurrCredits.get(p).compareTo(0) > 0;
            // 5 没有锁定的
            List<Integer> lockedInDrawTask = allLockedInDrawTask();
            java.util.function.Predicate<Integer> predicate5 = CollectionUtils.isEmpty(lockedInDrawTask) ? null
                    : p -> !lockedInDrawTask.contains(p);
            List<Integer> unlocked = Lists.newLinkedList();
            for (int i = 0, size = accountIdsTarget.size(); i < size; i++) {
                // 2019-09-16
                Integer accountId = accountIdsTarget.get(i);
                boolean test0 = predicate0.test(accountId);
                boolean test1 = predicate1 == null ? true : predicate1.test(accountId);
                boolean test2 = predicate2 == null ? true : predicate2.test(accountId);
                boolean test3 = predicate3 == null ? true : predicate3.test(accountId);
                boolean test4 = predicate4 == null ? true : predicate4.test(accountId);
                boolean test5 = predicate5 == null ? true : predicate5.test(accountId);
                boolean test = test0 && test1 && test2 && test3 && test4 && test5;
                if (test) {
                    unlocked.add(accountId);
                } else {
                    operations.remove(key, accountId + "");
                }
            }
            return unlocked;
        } catch (Exception e) {
            log.debug("从缓存中获取到的需要下发的出款卡集合异常:", e);
            return Lists.newArrayList();
        }
    }

    @Override
    public boolean needThirdDrawTo(Integer cardType, Integer needAmount) {
        if (cardType == null || needAmount == null || needAmount <= 0) {
            log.debug("需要第三方下发条件:cardType:{},needAmount:{}", cardType, needAmount);
            return false;
        }
        if (!AccountType.OutBank.getTypeId().equals(cardType)) {
            log.debug("卡类型:{},不是出款卡不满足第三方下发!", AccountType.findByTypeId(cardType).getMsg());
            return false;
        }
        Integer sysAmount = Integer.valueOf(CommonUtils.getThirdToOutLessBalance());
        log.debug("系统设置的出款卡至少所需金额:{},才能使用第三方下发!", sysAmount);
        return needAmount >= sysAmount;
    }

    /**
     * 判断 是否在 出款卡下发队列里
     *
     * @param accountId
     * @return
     */
    @Override
    public boolean isInNewOutNeedOri(Integer accountId) {
        if (accountId == null) {
            return false;
        }
        Long rank = redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOC_NEW_OUT_NEED_ORI)
                .rank(String.valueOf(accountId));
        log.debug("判断 出款卡 id ：{} 是否在ALLOC_NEW_OUT_NEED_ORI zset里 ,rank 结果:{}", accountId, rank);
        if (rank != null && rank >= 0) {
            return true;
        }
        return false;
    }

    /**
     * 判断是否在 已锁定的队列里了
     *
     * @param accountId
     * @return
     */
    @Override
    public boolean isLockedByOtherMeans(Integer accountId, Integer currentUserId) {
        log.debug("判断是否 被其他方式锁定 参数:accountId  {},currentUserId  {}", accountId, currentUserId);
        if (accountId == null) {
            return false;
        }
        TransLock lock = allocateTransService.buildLockToId(true, accountId);
        log.debug("获取 TransLock  :{} ", ObjectMapperUtils.serialize(lock));
        if (lock != null) {
            String key = RedisKeys.OUTCARD_NEED_THIRD_DRAW_LOCKED_HASH;
            StringRedisTemplate template = redisService.getStringRedisTemplate();
            // 是否是第三方锁定的
            boolean thirdLocked = template.opsForHash().hasKey(key, accountId.toString());
            if (currentUserId != null && currentUserId == AppConstants.USER_ID_1_ADMIN) {
                if (lock.getTransInt() != null && !thirdLocked) {
                    return true;
                }
            } else {
                if (lock.getOprId() != null && !lock.getOprId().equals(currentUserId) && !thirdLocked) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 在第三方入款里锁定的所有卡
     *
     * @return
     */
    @Override
    public List<Integer> allToIdsLockedByThirdIn(List<Integer> accountIdsTarget) {
        String key = RedisKeys.OUTCARD_NEED_THIRD_DRAW_LOCKED_HASH;
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        List<Integer> res = Lists.newLinkedList();
        if (template.hasKey(key)) {
            // key, accountId.toString(), thirdAccountId
            HashOperations operations = template.opsForHash();
            List list = operations.multiGet(key,
                    accountIdsTarget.stream().map(p -> p.toString()).collect(Collectors.toList()));
            if (!CollectionUtils.isEmpty(list)) {
                for (int i = 0, size = accountIdsTarget.size(); i < size; i++) {
                    Object obj = list.get(i);
                    if (obj != null) {
                        // 被锁定了就会有值
                        res.add(accountIdsTarget.get(i));
                    }
                }
            }
        }
        return res;
    }

    /**
     * 单次可下发金额是否为正的
     *
     * @param accountId
     * @return
     */
    @Override
    public boolean singleTimeDrawAbleFlag(Integer accountId) {
        AccountBaseInfo baseInfo = getFromCacheById(accountId);
        Integer singleTimeDrawAble = accountChangeService.currCredits(baseInfo);
        boolean singleTimeDrawAbleFlag = singleTimeDrawAble != null && singleTimeDrawAble > 0;
        log.debug("单次 可以下发的金额 :{}", singleTimeDrawAble);
        return singleTimeDrawAbleFlag;
    }

    /**
     * 出款需要下发的时候 放入队列中 便于第三方入款账号 绑定或者锁定下发
     *
     * @return
     */
    @Override
    public void addNeedThirdDrawToOutCardList(Integer accountId, Integer needAmount, Integer accountType) {
        log.debug("添加到第三方下发到出款卡 参数:{},{},{}", accountId, needAmount, accountType);
        if (accountId == null || needAmount == null || accountType == null) {
            log.debug("参数有空:{},{},{}", accountId, needAmount, accountType);
            return;
        }
        String enable = enableThirdDrawToOutCard(null);
        if (StringUtils.isBlank(enable) || !"1".equals(enable)) {
            log.debug("没有开启第三方下发到出款卡功能");
            return;
        }
        // 在回收集合里
//        Set<String> bindComm = getRecycleBindComm();
//        if (!CollectionUtils.isEmpty(bindComm) && bindComm.contains(accountId.toString())) {
//            log.debug("出款账号id:{} 在回收集合里", accountId);
//            return;
//        }
        // 在黑名单里
        boolean isBlack = allocateTransService.checkBlack(accountId);
        if (!isBlack) {
            log.debug("出款账号 id :{} 在黑名单里");
            return;
        }
        // 如果单次可下发余额小于等于0 不能下发
        boolean singleTimeDrawAble = singleTimeDrawAbleFlag(accountId);
        if (!singleTimeDrawAble) {
            log.debug("账号 id :{} 单次可下发额度 小于等于 0 ：{}", accountId, !singleTimeDrawAble);
            return;
        }
        boolean needThirdDrawTo = needThirdDrawTo(accountType, needAmount);
        log.debug("是否 满足 条件1:{}", needThirdDrawTo);
        if (!needThirdDrawTo) {
            log.debug("出款卡所需金额不满足需要第三方下发,accountId:{},needAmount:{},accountType:{}", accountId, needAmount,
                    accountType);
            return;
        }
        boolean unfinished = unfinished(accountId, accountType);
        log.debug("是否存在 上一笔 未完成出款 :{} ", unfinished);
        if (unfinished) {
            log.debug("出款卡所需金额需要第三方下发,上一笔尚未确认,不添加到未绑定列表中!参数:accountId:{},needAmount:{},accountType:{}", accountId,
                    needAmount, accountType);
            return;
        }
        // 当大额出款卡的余额小于设定
        boolean isBelow = belowPercentageNeedThirdDraw(accountId);
        log.debug("当大额出款卡的余额小于设定百分比 结果:{}", isBelow);
        if (!isBelow) {
            log.debug("大额出款卡的余额小于设定百分比 结果:{}", isBelow);
            return;
        }
        boolean isFlowBalaceLargerThanDailyOut = isFlowBalaceLargerThanDailyOut(accountId);
        log.debug("出款卡id:{} 当日出款流水+余额是否大于出款限额:{}", accountId, isFlowBalaceLargerThanDailyOut);
        if (isFlowBalaceLargerThanDailyOut) {
            return;
        }
        // 是否在已经被其他方式下发了
        boolean isExistedToDraw = isLockedByOtherMeans(accountId, AppConstants.USER_ID_1_ADMIN);
        if (isExistedToDraw) {
            log.debug("出款账号id :{} 已经被其他方式下发！");
            removeNeedAmountOutCard(accountId);
            removeBySystem(accountId);
            return;
        }
        String key = RedisKeys.OUTCARD_NEED_THIRD_DRAW;
        String member = accountId.toString();
        Double needAmountDouble = Double.valueOf(needAmount + ".00");
        log.debug("新增需要第三方下发的出款卡:id:{},金额:{}", member, needAmountDouble);
        boolean exist = redisService.getStringRedisTemplate().hasKey(key);
        JedisLock lock = null;
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        ZSetOperations zSetOperations = template.opsForZSet();
        try {

            lock = new JedisLock(template, "ADD_OUTCARD_NEEDAMOUNT_LOCK_" + accountId, 5000, 10000);
            boolean addLock = lock.acquire();
            if (!addLock) {
                log.debug("新增需要第三方下发的出款卡 获取不到jedis锁");
                return;
            }
            Boolean addRes = null;
            if (!exist) {
                addRes = zSetOperations.add(key, member, needAmountDouble);
            } else {
                Long rank = zSetOperations.rank(key, member);
                log.debug("新增需要第三方下发的出款卡 获取该memer的排名:{} 以判断是否存在!", rank);
                if (rank == null || rank < 0) {
                    addRes = zSetOperations.add(key, member, Double.valueOf(needAmount));
                } else {
                    Double oldNeedAmount = zSetOperations.score(key, member);
                    log.debug("需要第三方下发的出款卡:id:{},旧的金额:{}", member, oldNeedAmount);
                    if (oldNeedAmount != null && oldNeedAmount < needAmountDouble) {
                        Double add = needAmountDouble - oldNeedAmount;
                        Double newNeedAmount = zSetOperations.incrementScore(key, member, add);
                        addRes = true;
                        log.info("新增需要第三方下发的出款卡:id:{},新的金额:{}", member, newNeedAmount);
                    } else {
                        log.info("出款卡:{},需要相同的金额:{} 已在队列中!", member, needAmount);
                        return;
                    }
                }
            }
            if (null != addRes && addRes) {
                saveAddTimeForOutCard(accountId, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("新增需要第三方下发的出款卡 异常:", e);
        } finally {
            if (!ObjectUtils.isEmpty(lock) && lock.isLocked()) {
                lock.release();
            }
        }
    }

    @Override
    public boolean usingOrUsable(Integer id) {
        if (null == id) {
            return false;
        }
        AccountBaseInfo baseInfo = getFromCacheById(id);
        log.info("账号id :{} 缓存信息:{}", id, baseInfo);
        if (baseInfo == null) {
            return false;
        }
        Integer[] status = new Integer[] { AccountStatus.Normal.getStatus(), AccountStatus.Enabled.getStatus() };
        if (Arrays.asList(status).contains(baseInfo.getStatus())) {
            return true;
        }
        return false;
    }

    @Override
    public List<Integer> onlineAccountIdsList(Integer type) {
        try {
            if (!redisService.getStringRedisTemplate().hasKey(RedisKeys.INBANK_ONLINE)) {
                return Lists.newArrayList();
            }
            Integer[] commonType = new Integer[] { AccountType.BindCommon.getTypeId(),
                    AccountType.ThirdCommon.getTypeId(), AccountType.BindAli.getTypeId(),
                    AccountType.BindWechat.getTypeId() };
            Set<Object> keys = redisService.getStringRedisTemplate().boundHashOps(RedisKeys.INBANK_ONLINE).keys();
            List<Integer> list = keys.stream().filter(p -> {
                Integer id = Integer.valueOf(p.toString());
                boolean inCardCache = availableCardCache.checkBankOnline(id);
                // log.debug("账号 id:{} 是否在availableCardCache中:{}", id,
                // inCardCache);
                boolean queryType = type.equals(AccountType.OutBank.getTypeId())
                        || type.equals(AccountType.InBank.getTypeId())
                        || type.equals(AccountType.ReserveBank.getTypeId())
                        || type.equals(AccountType.ReserveBank.getTypeId()) || Arrays.asList(commonType).contains(type);
                if (queryType) {
                    if (inCardCache) {
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return true;
                }
            }).map(p -> Integer.valueOf(p.toString())).collect(Collectors.toList());
            return list;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Lists.newArrayList();
    }

    /**
     * 描述 保存或者删除账号id到缓存中
     *
     * @param accountId
     *            账号id
     * @param add
     *            true 表示保存 false 表示 删除
     */
    @Override
    public void saveOnlineAccontIds(Integer accountId, boolean add) {
        if (ObjectUtils.isEmpty(accountId)) {
            return;
        }
        HashOperations operations = redisService.getStringRedisTemplate().opsForHash();
        if (add) {
            if (!operations.hasKey(RedisKeys.INBANK_ONLINE, accountId.toString())) {
                operations.putIfAbsent(RedisKeys.INBANK_ONLINE, accountId.toString(),
                        String.valueOf(System.currentTimeMillis()));
            }
        } else {
            if (operations.hasKey(RedisKeys.INBANK_ONLINE, accountId.toString())) {
                Object value = operations.get(RedisKeys.INBANK_ONLINE, accountId.toString());
                if (value != null) {
                    operations.delete(RedisKeys.INBANK_ONLINE, accountId.toString());
                    // try {
                    // int EXPR_INBANK = 300000;
                    // if(System.currentTimeMillis() -
                    // Long.valueOf(value.toString()) >=
                    // EXPR_INBANK) {
                    // operations.delete(RedisKeys.INBANK_ONLINE,
                    // accountId.toString());
                    // }
                    // } catch (NumberFormatException e) {
                    // log.error(e.getMessage(), e);
                    // }
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BizAccount addInBankAccount(AddInBankAccountInputDTO inputDTO, BizAccount old) {
        SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
        BizAccount newAccount = ObjectUtils.isEmpty(old) ? new BizAccount() : old;
        org.springframework.beans.BeanUtils.copyProperties(inputDTO, newAccount);
        newAccount.setType(AccountType.InBank.getTypeId());
        newAccount.setStatus(AccountStatus.Inactivated.getStatus());
        /** 默认0 */
        newAccount.setFlag(ObjectUtils.isEmpty(newAccount.getFlag()) ? 0 : newAccount.getFlag());
        // 现在系统新增的都是PC 所以 out_enable应该都是默认0 不会其他值
        if (newAccount.getFlag().equals(0)) {
            newAccount.setOutEnable((byte) 0);
        } else {
            newAccount.setOutEnable((byte) 1);
        }
        if (ObjectUtils.isEmpty(old)) {
            newAccount.setCreateTime(new Date());
            newAccount = setAccountAlias(newAccount);
            newAccount.setCreator(operator == null ? 0 : operator.getId());
        } else {
            newAccount.setUpdateTime(new Date());
        }
        if (!ObjectUtils.isEmpty(newAccount.getRemark())) {
            newAccount.setRemark(CommonUtils.genRemark(newAccount.getRemark(), inputDTO.getRemark(), new Date(),
                    operator == null ? "sys" : operator.getUid()));
        }
        newAccount = save(newAccount);
        List<BizLevel> levels = levelRepository.findByHandicapIdAndCurrSysLevel(newAccount.getHandicapId(),
                newAccount.getCurrSysLevel());
        if (!ObjectUtils.isEmpty(levels)) {
            List<BizAccountLevel> accountLevels = new ArrayList<>();
            for (BizLevel bizLevel : levels) {
                BizAccountLevel bizAccountLevel = new BizAccountLevel();
                bizAccountLevel.setAccountId(newAccount.getId());
                bizAccountLevel.setLevelId(bizLevel.getId());
                accountLevels.add(bizAccountLevel);
            }
            accountLevelRepository.save(accountLevels);
        }
        log.debug("addInBankAccount>> id {}", newAccount.getId());
        broadCast(newAccount);
        incomeAccountAllocateService.update(newAccount.getId(), newAccount.getType(), newAccount.getStatus());
        cabanaService.updAcc(newAccount.getId());
        String remark = ObjectUtils.isEmpty(old) ? "新增入款银行卡" : "修改银行卡";
        accountExtraService.addAccountExtraLog(newAccount.getId(), remark);
        return newAccount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<BizAccount> saveIterable(List<BizAccount> list) {
        return accountRepository.save(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BizAccount save(BizAccount entity) {
        return accountRepository.saveAndFlush(entity);
    }

    @Override
    public List<BizAccount> findByIds(ArrayList<Integer> list) {
        Integer[] ids = new Integer[list.size()];
        for (int i = 0, size = list.size(); i < size; i++) {
            ids[i] = list.get(i);
        }
        return accountRepository.findByIdIn(ids);
    }

    @Override
    public BizAccount findByAccountNo(String accountNo, Integer subType) {
        return accountRepository.findByAccountAndSubType(accountNo, subType);
    }

    /**
     * 新增其他账号，如云山付账号的时候同时新增入款银行卡(参见平台同步入款银行卡) 以供绑定
     */
    @Override
    @Transactional
    public List<BizAccount> createIncomeAccount(List<InAccountBindedYSFInputDTO> dto) {
        if (CollectionUtils.isEmpty(dto))
            return null;
        List<BizAccount> res = new ArrayList<>();
        try {
            for (InAccountBindedYSFInputDTO requestBody : dto) {
                BizHandicap bizHandicap = handicapService.findFromCacheByCode(requestBody.getHandicap());
                String[] levelArray = { "unknown" };
                List<String> levelList;
                List<Integer> levelIdList;
                Integer accountId;
                // 当前层级，拥有多个时，指定层>外层>中层>内层
                int currentSystemLevel = 0;
                if (!ObjectUtils.isEmpty(requestBody.getLevelIds())) {
                    levelList = new LinkedList<>();
                    levelIdList = new LinkedList<>();
                    for (Integer levelId : requestBody.getLevelIds()) {
                        BizLevel bizLevel = levelService.findFromCache(levelId);
                        if (!ObjectUtils.isEmpty(bizLevel)) {
                            levelList.add(bizLevel.getCode());
                            levelIdList.add(levelId);
                            // 根据层级自动适配当前层级
                            if (null != bizLevel.getCurrSysLevel()) {// 此层级绑定的内中外层不为空
                                if (CurrentSystemLevel.Designated.getValue() == bizLevel.getCurrSysLevel()) {
                                    currentSystemLevel = CurrentSystemLevel.Designated.getValue();
                                } else if (CurrentSystemLevel.Outter.getValue() == bizLevel.getCurrSysLevel()) {
                                    currentSystemLevel = CurrentSystemLevel.Outter.getValue();
                                } else if (CurrentSystemLevel.Middle.getValue() == bizLevel.getCurrSysLevel()) {
                                    currentSystemLevel = CurrentSystemLevel.Middle.getValue();
                                } else if (CurrentSystemLevel.Inner.getValue() == bizLevel.getCurrSysLevel()) {
                                    currentSystemLevel = CurrentSystemLevel.Inner.getValue();
                                }
                            }
                        }
                    }
                    if (levelList.size() > 0) {
                        levelArray = levelList.toArray(levelArray);
                    }
                }
                BizAccount o = new BizAccount();
                // o.setCurrSysLevel(currentSystemLevel);
                o.setHandicapId(bizHandicap.getId());
                o.setStatus(
                        requestBody.getStatus() == null ? AccountStatus.Enabled.getStatus() : requestBody.getStatus());
                o.setAccount(requestBody.getAccount());
                o.setBankName(requestBody.getBankName());// 支付宝 微信 存设备号
                o.setBankType(requestBody.getBankType());// 做唯一性键,新增之后对于银行入款卡系统会设置类型,不以平台同步的为准
                o.setOwner(requestBody.getOwner());
                o.setSign_(StringUtils.isBlank(requestBody.getSign()) ? null
                        : FundTransferEncrypter.encryptDb(requestBody.getSign()));
                o.setHook_(StringUtils.isBlank(requestBody.getHook()) ? null
                        : FundTransferEncrypter.encryptDb(requestBody.getHook()));
                o.setHub_(StringUtils.isBlank(requestBody.getHub()) ? null
                        : FundTransferEncrypter.encryptDb(requestBody.getHub()));
                o.setBing_(StringUtils.isBlank(requestBody.getBing()) ? null
                        : FundTransferEncrypter.encryptDb(requestBody.getBing()));
                o.setFlag(0);
                o.setType(requestBody.getType());
                if (requestBody.getType().intValue() == AccountType.InBank.getTypeId().intValue()) {
                    o.setSubType(requestBody.getSubType() == null ? 0 : requestBody.getSubType());
                }
                Date date = new Date();
                o.setCreateTime(date);
                o.setUpdateTime(date);
                o = setAccountAlias(o);
                List<BizAccountLevel> accountLevelToList = levelService.wrapAccountLevel(levelArray, bizHandicap);
                BizAccount newAccount = create(accountLevelToList, o);// 保存账号信息
                // 和层级绑定关系
                log.debug("createIncomeAccount>> id {}", newAccount.getId());
                broadCast(newAccount);
                boolean type = newAccount.getType().equals(AccountType.InBank.getTypeId());
                if (type && incomeAccountAllocateService.checkHandicap(requestBody.getHandicap())) {
                    incomeAccountAllocateService.update(newAccount.getId(), newAccount.getType(),
                            newAccount.getStatus());
                }
                accountId = newAccount.getId();
                cabanaService.updAcc(newAccount.getId());
                accountExtraService.addAccountExtraLog(accountId, "新增云闪付添加银行卡");
                res.add(newAccount);
            }
        } catch (Exception e) {
            log.error("新增云闪付账号添加入款卡账号失败:", e);
        }
        return res;
    }

    /**
     * 更新其他账号，如云山付账号的时候同时更新入款银行卡(参见平台同步入款银行卡) 以供绑定
     */
    @Override
    public List<BizAccount> updateIncomeAccount(List<InAccountBindedYSFInputDTO> dto) {
        try {
            if (CollectionUtils.isEmpty(dto))
                return null;
            List<BizAccount> res = new ArrayList<>();
            for (InAccountBindedYSFInputDTO requestBody : dto) {
                AccountBaseInfo baseInfo = getFromCacheById(requestBody.getId());
                BizHandicap bizHandicap = handicapService.findFromCacheById(baseInfo.getHandicapId());
                // 检测 新卡状态 Enabled 是可用 新卡
                int enabled = AccountStatus.Enabled.getStatus();
                if (baseInfo.getStatus() != enabled && requestBody.getStatus() == enabled) {

                }
                BizAccount latestAccount;
                BizAccount db = getById(baseInfo.getId());
                BizAccount oldAccount = new BizAccount();
                BeanUtils.copyProperties(oldAccount, db);
                String[] levelArray = { "unknown" };
                List<String> levelList = null;
                List<Integer> levelIdList = null;
                // 当前层级，拥有多个时，指定层>外层>中层>内层
                int currentSystemLevel = 0;
                if (!ObjectUtils.isEmpty(requestBody.getLevelIds())) {
                    // levels 传的就是层级编码
                    levelList = new LinkedList<>();
                    levelIdList = new LinkedList<>();
                    for (Integer levelId : requestBody.getLevelIds()) {
                        BizLevel bizLevel = levelService.findFromCache(levelId);
                        if (!ObjectUtils.isEmpty(bizLevel)) {
                            levelList.add(bizLevel.getCode());
                            levelIdList.add(bizLevel.getId());
                            // 根据层级自动适配当前层级
                            if (null != bizLevel.getCurrSysLevel()) {// 此层级绑定的内中外层不为空
                                if (CurrentSystemLevel.Designated.getValue() == bizLevel.getCurrSysLevel()) {
                                    currentSystemLevel = CurrentSystemLevel.Designated.getValue();
                                } else if (CurrentSystemLevel.Outter.getValue() == bizLevel.getCurrSysLevel()) {
                                    currentSystemLevel = CurrentSystemLevel.Outter.getValue();
                                } else if (CurrentSystemLevel.Middle.getValue() == bizLevel.getCurrSysLevel()) {
                                    currentSystemLevel = CurrentSystemLevel.Middle.getValue();
                                } else if (CurrentSystemLevel.Inner.getValue() == bizLevel.getCurrSysLevel()) {
                                    currentSystemLevel = CurrentSystemLevel.Inner.getValue();
                                }
                            }
                        }
                    }
                    if (levelList.size() > 0) {
                        levelArray = levelList.toArray(levelArray);
                    }
                }
                List<BizAccountLevel> bizAccountLevelList = findByAccountId(db.getId());
                updateAccountLevels(bizAccountLevelList, levelIdList, db, levelArray);
                // 入款卡冻结时候拉倒亏损流水里面并且以前冻结的账号不拉倒冻结里面
                if (requestBody.getStatus().intValue() == AccountStatus.Freeze.getStatus()
                        && db.getStatus().intValue() != AccountStatus.Freeze.getStatus().intValue()) {
                    // 查询是否存在未处理的冻结数据
                    int count = finLessStatService.findCountsById(db.getId(), "portion");
                    if (count <= 0) {
                        finLessStatService.addTrace(db.getId(), db.getBankBalance());
                    }
                }
                // 入款账号分配 标识
                boolean updAllocate = false;
                // 修改账号 基本信息
                RequestAccount requestAccount = new RequestAccount();
                BeanUtils.copyProperties(requestAccount, requestBody);
                boolean updateAccount = checkUpdateAccount(bizHandicap, baseInfo, requestAccount);
                log.info("是否更新类型和状态:{}", updateAccount);
                if (updateAccount) {
                    updAllocate = incomeAccountAllocateService.checkHandicap(requestBody.getHandicap())
                            && (!db.getStatus().equals(requestBody.getStatus())
                            || !db.getType().equals(requestBody.getType()))
                            && (db.getType().equals(AccountType.InBank.getTypeId())
                            || baseInfo.getType().equals(AccountType.InBank.getTypeId()));
                }
                db.setStatus(requestBody.getStatus());
                db.setType(requestBody.getType());
                db.setSubType(requestBody.getSubType());
                db.setOwner(requestBody.getOwner());
                db.setBankName(requestBody.getBankName());
                if (!requestBody.getType().equals(AccountType.InBank.getTypeId())) {
                    // 入款银行卡银行类型在出入款系统设置
                    db.setBankType(requestBody.getBankType());
                }
                db.setUpdateTime(new Date());
                db.setHandicapId(bizHandicap.getId());
                // db.setCurrSysLevel(currentSystemLevel);
                db.setAccount(requestBody.getAccount());
                updateBaseInfo(db);
                latestAccount = db;
                // 广播同步账号信息
                log.debug("updateIncomeAccount>> id {}", latestAccount.getId());
                broadCast(latestAccount);
                cabanaService.updAcc(db.getId());
                // 入款账号 分配
                if (updAllocate) {
                    log.info("Account >> UpdateAlloc:{}", requestBody.getAccount());
                    incomeAccountAllocateService.update(latestAccount.getId(), latestAccount.getType(),
                            latestAccount.getStatus());
                }
                accountExtraService.saveAccountExtraLog(oldAccount, db, "更新云闪付绑定的银行卡");
                res.add(db);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void updateAccountLevels(List<BizAccountLevel> bizAccountLevelList, List<Integer> levelIdList, BizAccount db,
                                    String[] levelArray) {
        if (!CollectionUtils.isEmpty(levelIdList)) {
            if (!CollectionUtils.isEmpty(bizAccountLevelList)) {
                if (bizAccountLevelList.size() == levelIdList.size()) {
                    int count = 0;
                    for (Integer aLevelIdList : levelIdList) {
                        for (BizAccountLevel aBizAccountLevelList : bizAccountLevelList) {
                            if (aLevelIdList == aBizAccountLevelList.getLevelId()) {
                                count++;
                            }
                        }
                    }
                    if (count == levelIdList.size()) {
                        log.info("层级信息不变:count:{},levelIdList.size():{}", count, levelIdList.size(),
                                count == levelIdList.size());
                    } else {
                        // 先删除再新增
                        log.info("先删除:{}再新增:{}", bizAccountLevelList.size(), levelIdList.size());
                        deleteInBatch(bizAccountLevelList);
                        bindLevels(levelIdList, db, levelArray);
                    }
                } else {
                    // 先删除再新增
                    log.info("先删除:{}再新增:{}", bizAccountLevelList.size(), levelIdList.size());
                    deleteInBatch(bizAccountLevelList);
                    bindLevels(levelIdList, db, levelArray);
                }
            } else {
                // 新绑定层级
                log.info("新绑定层级:{}", levelIdList.size());
                bindLevels(levelIdList, db, levelArray);
            }
        } else {
            if (bizAccountLevelList != null && bizAccountLevelList.size() > 0) {
                // 直接删除
                log.info("先删除:{}", bizAccountLevelList.size());
                deleteInBatch(bizAccountLevelList);
            }
        }
    }

    @Override
    public boolean checkUpdateAccount(BizHandicap handicap, AccountBaseInfo b, RequestAccount o) {
        if (b.getType() != null && !b.getType().equals(o.getType())) {
            log.info("need update:handicap={},account={},to.type={},from.type={}", handicap.getCode(), b.getAccount(),
                    o.getType(), b.getType());
            return true;
        }
        if (b.getStatus() != null && !b.getStatus().equals(o.getStatus())) {
            log.info("need update:handicap={},account={},to.status={},from.status={}", handicap.getCode(),
                    b.getAccount(), o.getStatus(), b.getStatus());
            return true;
        }
        return false;
    }

    /**
     * 绑定层级
     */
    @Override
    @Transactional
    public void bindLevels(List<Integer> levelIdList, BizAccount db, String[] levelArray) {
        for (int i = 0, L = levelIdList.size(); i < L; i++) {
            BizLevel bizLevel = levelService.findFromCache(db.getHandicapId(), levelArray[i]);
            BizAccountLevel bizAccountLevel;
            // 删除状态的层级不可恢复 所以删除状态的层级不能绑定
            if (bizLevel != null && (bizLevel.getStatus() == 0 || bizLevel.getStatus() == 1)) {
                bizAccountLevel = new BizAccountLevel();
                bizAccountLevel.setLevelId(levelIdList.get(i));
                bizAccountLevel.setAccountId(db.getId());
                saveBizAccountLevel(bizAccountLevel);
            }
        }
    }

    /**
     * 检查 该下发卡或者出款卡是否已经锁定 是本人锁定才能提现
     *
     * @param userId
     * @param toAccountId
     *            锁定的账号
     * @param fromAccountId
     *            第三方账号
     * @return
     */
    @Override
    public boolean checkThirdInAccount4DrawLocked(Integer userId, Integer[] toAccountId, Integer fromAccountId) {
        try {
            if (userId == null || toAccountId == null || toAccountId.length == 0 || fromAccountId == null) {
                return false;
            }
            StringRedisTemplate template = redisService.getStringRedisTemplate();
            // 锁定的非出款卡的记录
            Set<String> keys = template
                    .keys(RedisKeys.LOCK_TARGETACCOUNT_FOR_THIRDACCOUNTDRAW_KEY_PREX + ":" + userId + ":*");
            // 锁定的出款卡的记录
            List<String> outCardIdsLockedByUserId = outCardIdsLockedByUserId(userId);
            // 在下发任务里的锁定
            List<Integer> drawTaskLockedByUserId = getAccountIdLockedByUserIdInDrawTask(userId);
            if (CollectionUtils.isEmpty(keys) && CollectionUtils.isEmpty(outCardIdsLockedByUserId)
                    && CollectionUtils.isEmpty(drawTaskLockedByUserId)) {
                log.debug("没有锁定的记录 ");
                return false;
            }
            Set<String> lockedTargetAccounts = new HashSet<>();
            if (!CollectionUtils.isEmpty(keys)) {
                Iterator it = keys.iterator();
                for (; it.hasNext();) {
                    String key = it.next().toString();
                    lockedTargetAccounts.addAll(template.boundSetOps(key).members());
                }
            } else {
                log.debug("没有锁定的下发卡或者备用卡 记录");
            }
            if (!CollectionUtils.isEmpty(outCardIdsLockedByUserId)) {
                lockedTargetAccounts.addAll(outCardIdsLockedByUserId);
            } else {
                log.debug("没有锁定出款卡记录");
            }
            if (!CollectionUtils.isEmpty(drawTaskLockedByUserId)) {
                lockedTargetAccounts
                        .addAll(drawTaskLockedByUserId.stream().map(p -> p.toString()).collect(Collectors.toList()));
            } else {
                log.debug("没有在下发任务里的记录");
            }
            if (CollectionUtils.isEmpty(lockedTargetAccounts)) {
                log.debug("没有锁定的记录");
                return false;
            }
            List<String> list = Lists.transform(Lists.newArrayList(toAccountId), Functions.toStringFunction());
            log.info(String.format("%s  %s,本次提现的账号:%s", "锁定的账号：", lockedTargetAccounts.toString(), list.toString()));
            if (new ArrayList<>(lockedTargetAccounts).containsAll(list)) {
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("检查 该下发卡或者出款卡是否已经锁定 是本人锁定才能提现 error:", e);
            return false;
        }
    }

    /**
     * 根据 账号查询锁定人 userId
     *
     * @param accountId
     * @return
     */
    private Integer getLockerId(Integer accountId) {
        Assert.notNull(accountId, "accountId must not be null");
        List keys = new ArrayList();
        keys.add(RedisKeys.LOCK_TARGETACCOUNT_FOR_THIRDACCOUNTDRAW_KEY_PREX + ":*");
        String[] argvs = new String[] { String.valueOf(accountId), String.valueOf(accountId) };
        String ret = redisService.getStringRedisTemplate().execute(searchLockerByAccountIdScript, keys, argvs);
        return StringUtils.isNotBlank(ret) ? Integer.valueOf(ret) : null;
    }

    /**
     * 入款第三方账号提现 锁定目标账号
     *
     * @param userId
     * @param accountId
     * @return
     */
    @Override
    public long lockThirdInAccount4Draw(int userId, int accountId, Integer incomeAccountId) {
        return executeScript(1, userId, accountId, incomeAccountId);
    }

    /**
     * 入款第三方账号提现 解锁目标账号
     *
     * @param userId
     * @param accountId
     * @return
     */
    @Override
    public long unlockThirdInAccount4Draw(int userId, int accountId, Integer incomeAccountId) {
        return executeScript(0, userId, accountId, incomeAccountId);
    }

    /**
     * 检查到下发卡 不在线就自动解锁
     *
     * @param userId
     * @param lockedIds
     * @return
     */
    @Override
    public void unlockThirdInAccount(Integer userId, List<Integer> lockedIds) {
        if (CollectionUtils.isEmpty(lockedIds)) {
            return;
        }
        // thirdInAccount:withdraw:lockTargetAccId:byUser:5:261114 set
        String key = RedisKeys.LOCK_TARGETACCOUNT_FOR_THIRDACCOUNTDRAW_KEY_PREX;
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        SetOperations operations = template.opsForSet();
        if (userId == null) {
            key = key + ":*:*";
        } else {
            key = key + ":" + userId + ":*";
        }
        for (Integer id : lockedIds) {
            Set<String> keys = template.keys(key);
            if (!CollectionUtils.isEmpty(keys)) {
                for (String key1 : keys) {
                    if (operations.isMember(key1, id.toString())) {
                        operations.remove(key1, id.toString());
                    }
                }
            }
        }
    }

    /**
     * 下发任务 锁定脚本
     *
     * @param userId
     * @param accountId
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    long executeThirdDrawTaskScript(Integer userId, List<Integer> accountId) {
        try {
            Assert.notNull(userId, "用户id不能为空!");
            Assert.notNull(accountId, "账号id 不能为空!");

            Long ret;
            List keys = new ArrayList();
            keys.add(RedisKeys.DRAW_TASK_USER_LOCK_CARD);
            String[] argvs = new String[4];
            argvs[0] = userId.toString();
            Iterator it = accountId.iterator();
            StringBuffer str = new StringBuffer();
            for (; it.hasNext();) {
                str.append(it.next().toString()).append(",");
            }
            String strs = str.toString();
            int len = strs.length();
            strs = strs.substring(0, len - 1);
            argvs[1] = strs;
            // 标识 是否使用旧的第三方下发 1 使用 其他值不使用
            argvs[2] = "1";
            argvs[3] = System.currentTimeMillis() + "";
            ret = redisService.getStringRedisTemplate().execute(thirdDrawTasklockScript, keys, argvs);
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private long executeScript(int type, int userId, int accountId, Integer incomeAccountId) {
        Assert.notNull(userId, "userId must not be null");
        Assert.notNull(accountId, "accountId must not be null");
        Long ret;
        List keys = new ArrayList();
        keys.add(RedisKeys.LOCK_TARGETACCOUNT_FOR_THIRDACCOUNTDRAW_KEY_PREX);
        String[] argvs;
        if (type == 1) {
            Assert.notNull(incomeAccountId, "incomeAccountId must not be null");
            argvs = new String[] { String.valueOf(accountId), String.valueOf(userId), String.valueOf(incomeAccountId) };
            ret = redisService.getStringRedisTemplate().execute(lockScript, keys, argvs);
        } else {
            argvs = new String[] { String.valueOf(accountId), String.valueOf(userId) };
            ret = redisService.getStringRedisTemplate().execute(unlockScript, keys, argvs);
        }
        return ret;
    }

    /**
     * 入款第三方账号提现时候 查询已锁定的目标账号
     *
     * @return
     */
    @Override
    public List<String> getTargetAccountLockedInRedis(Integer userId, Integer incomeAccountId) {
        return executeSearchScript(userId, incomeAccountId);
    }

    /**
     * 查询 当前第三方账号 当前人已锁定的目标账号
     *
     * @return
     */
    @Override
    public List<String> getTargetAccountLockedByCurrentUserInRedis(Integer userId, Integer incomeAccountId) {
        return executeSearchScript(userId, incomeAccountId);
    }

    /**
     * 入款第三方账号提现时候 查询当前人已锁定的目标账号
     *
     * @return
     */
    @Override
    public List<String> getLockedAccByUserId(Integer userId) {
        List<String> keys = new ArrayList(), ret = new ArrayList<>();
        String lockedTargetAccIds;
        keys.add(RedisKeys.LOCK_TARGETACCOUNT_FOR_THIRDACCOUNTDRAW_KEY_PREX + ":" + userId + ":*");
        lockedTargetAccIds = redisService.getStringRedisTemplate().execute(searchAllLockedAccIdsScript, keys);
        if (StringUtils.isNotBlank(lockedTargetAccIds)) {
            String[] subs = lockedTargetAccIds.split(",");
            ret = Arrays.stream(subs).collect(Collectors.toList());
        }
        return ret;
    }

    private List<String> executeSearchScript(Integer userId, Integer incomeAccountId) {
        List<String> keys = new ArrayList(), ret = new ArrayList<>();
        String lockedTargetAccIds;
        if (userId == null) {
            keys.add(RedisKeys.LOCK_TARGETACCOUNT_FOR_THIRDACCOUNTDRAW_KEY_PREX + ":*" + ":*");
            lockedTargetAccIds = redisService.getStringRedisTemplate().execute(searchAllLockedAccIdsScript, keys);
        } else {
            keys.add(RedisKeys.LOCK_TARGETACCOUNT_FOR_THIRDACCOUNTDRAW_KEY_PREX + ":" + userId + ":" + incomeAccountId);
            lockedTargetAccIds = redisService.getStringRedisTemplate().execute(searchLockedAccIdsByCurrentUserScript,
                    keys);
        }
        if (StringUtils.isNotBlank(lockedTargetAccIds)) {
            String[] subs = lockedTargetAccIds.split(",");
            ret = Arrays.stream(subs).collect(Collectors.toList());
        }
        return ret;
    }

    @Transactional
    @Override
    public void deleteInBatch(Iterable<BizAccountLevel> iterable) {
        accountLevelRepository.deleteInBatch(iterable);
    }

    @Transactional
    @Override
    public int updateBankBalance(BigDecimal bankBalance, int id) {
        return accountRepository.updateBankBalance(bankBalance, id);
    }

    /**
     * 根据账号 层级 id查询
     */
    @Override
    public List<BizAccountLevel> findByAccountId(Integer accountId) {
        return accountLevelRepository.findByAccountId(accountId);
    }

    /**
     * 保存账号层级信息
     */
    @Transactional
    @Override
    public void saveBizAccountLevel(BizAccountLevel bizAccountLevel) {
        accountLevelRepository.saveAndFlush(bizAccountLevel);
    }

    @Override
    public List<Integer> queryAccountIdsByAlias(String accountAlias) {
        return accountRepository.queryAccountIdsByAlias(accountAlias);
    }

    @Override
    public List<Integer> queryAccountIdsByAliasOrPhoneRobot(OutwardTaskTotalInputDTO inputDTO) {
        String sql = "select id from biz_account where 1=1 ";
        if (StringUtils.isNotBlank(inputDTO.getAccountAlias())) {
            sql += " and  ( alias =" + inputDTO.getAccountAlias() + " or id=" + inputDTO.getAccountAlias()
                    + " or account like  '%" + inputDTO.getAccountAlias() + "')   ";
        }
        if (inputDTO.getPhone() != null && inputDTO.getRobot() != null) {
            sql += " and  flag in(0,2) ";
        } else {
            if (inputDTO.getPhone() != null) {
                // 返利网
                sql += " and  flag=2 ";
            }
            if (inputDTO.getRobot() != null) {
                // PC
                sql += " and   flag=0 ";
            }
        }
        List<Integer> list = entityManager.createNativeQuery(sql).getResultList();
        return list;
    }

    @Override
    public List<Object> queryAccountForOutDrawing(String accountAlias, String bankType, String currentLevel) {
        String sql = "select distinct a.id from biz_account a   WHERE 1=1 ";
        if (StringUtils.isNotBlank(accountAlias)) {
            accountAlias = "%" + StringUtils.trim(accountAlias) + "%";
            sql += " and (a.alias like \"" + accountAlias + "\" or a.account like \"" + accountAlias
                    + "\" or a.id  like \"" + accountAlias + "\" )";
        }
        if (StringUtils.isNotBlank(bankType)) {
            bankType = "%" + StringUtils.trim(bankType) + "%";
            sql += " and a.bank_type like \"" + bankType + "\"";
        }
        if (StringUtils.isNotBlank(currentLevel)) {
            currentLevel = StringUtils.trim(currentLevel);
            sql += " and a.curr_sys_level = \"" + currentLevel + "\"";
        }
        List<Object> accountId = entityManager.createNativeQuery(sql).getResultList();
        return accountId;
    }

    /**
     * 获取账号表当前最大的编号
     *
     * @return
     */
    @Override
    public String getMaxAlias() {
        return accountRepository.getMaxAlias();
    }

    @Override
    public List<BizAccount> getAllOutAccount(Integer[] types) {
        return accountRepository.findAllByTypeIn(types);
    }

    @Override
    public List<Integer> findAccountIdInSameLevel(Integer accountId) {
        return accountRepository.findAccountIdInSameLevel(accountId);
    }

    /**
     * 根据层级id查询同层级下的其他账号Id
     *
     * @return accountId List
     */
    @Override
    public List<Integer> getBizAccountLevelList(Integer accountId) {
        if (accountId == null) {
            return Collections.emptyList();
        }
        List<Integer> list = new ArrayList<>();
        List<Object> bizAccountList = accountRepository.getAccountIdList(accountId);
        if (bizAccountList != null && bizAccountList.size() > 0) {
            Set<Integer> set = new HashSet<>();
            bizAccountList.forEach((p) -> {
                if (!p.equals(accountId)) {
                    set.add((Integer) p);
                }
            });
            list = new ArrayList<>(set);
        }
        return list;
    }

    /**
     * 获取账号基本信息
     */
    @Override
    public AccountBaseInfo getFromCacheById(Integer accountId) {
        if (accountId == null) {
            return null;
        }
        AccountBaseInfo baseInfo = accountBaseInfoCacheBuilder.getIfPresent(accountId);
        if (baseInfo != null) {
            return baseInfo;
        }
        baseInfo = packAccountBaseInfo(accountRepository.findById2(accountId));
        if (baseInfo != null) {
            accountBaseInfoCacheBuilder.put(accountId, baseInfo);
        }
        return baseInfo;
    }

    /**
     * 获取账号基本信息
     */
    @Override
    public AccountBaseInfo getFromCacheByHandicapIdAndAccount(int handicapId, String account) {
        if (StringUtils.isBlank(account)) {
            return null;
        }
        String key = handicapId + ":" + account;
        AccountBaseInfo baseInfo = accountBaseInfoCacheBuilder.getIfPresent(key);
        if (baseInfo != null) {
            return baseInfo;
        }
        baseInfo = packAccountBaseInfo(accountRepository.findByHandicapIdAndAccount(handicapId, account));
        if (baseInfo != null) {
            accountBaseInfoCacheBuilder.put(key, baseInfo);
        }
        return baseInfo;
    }

    @Override
    public AccountBaseInfo getFromCacheByHandicapIdAndAccountAndBankName(int handicapId, String account,
                                                                         String bankName) {
        if (StringUtils.isBlank(account)) {
            return null;
        }
        String key = handicapId + ":" + account + ":" + bankName;
        AccountBaseInfo baseInfo = accountBaseInfoCacheBuilder.getIfPresent(key);
        if (baseInfo != null) {
            return baseInfo;
        }
        baseInfo = packAccountBaseInfo(
                accountRepository.findByHandicapIdAndAccountAndBankType(handicapId, account, bankName));
        if (baseInfo != null) {
            accountBaseInfoCacheBuilder.put(key, baseInfo);
        }
        return baseInfo;
    }

    /**
     * 获取账号基本信息
     */
    @Override
    public AccountBaseInfo getFromCacheByTypeAndAccount(int type, String account) {
        if (StringUtils.isBlank(account)) {
            return null;
        }
        String key = type + "::" + account;
        AccountBaseInfo baseInfo = accountBaseInfoCacheBuilder.getIfPresent(key);
        if (baseInfo != null) {
            return baseInfo;
        }
        List<BizAccount> list = accountRepository.findByTypeAndAccount(type, account);
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        baseInfo = packAccountBaseInfo(list.get(0));
        if (baseInfo != null) {
            accountBaseInfoCacheBuilder.put(key, baseInfo);
        }
        return baseInfo;
    }

    /**
     * 广播通知：基本信息
     */
    @Override
    public void broadCast(BizAccount vo) {
        if (vo == null || vo.getId() == null) {
            return;
        }
        try {
            AccountBaseInfo baseInfo = packAccountBaseInfo(vo);
            redisService.convertAndSend(RedisTopics.REFRESH_ACCOUNT, mapper.writeValueAsString(baseInfo));
            // 同步状态到返利网
            rebateApiService.rebateUserStatus(vo.getAccount());
        } catch (Exception e) {
            log.error("");
        }

    }

    /**
     * 广播通知：批量存基本信息
     */
    @Override
    public void broadCast() throws Exception {
        redisService.convertAndSend(RedisTopics.REFRESH_ACCOUNT_LIST, "批量刷新在用出款卡账号信息");
    }

    /**
     * 删除本地缓存
     */
    @Override
    public void flushCache(AccountBaseInfo baseInfo) {
        if (baseInfo == null) {
            return;
        }
        accountBaseInfoCacheBuilder.put(baseInfo.getType() + "::" + baseInfo.getAccount(), baseInfo);
        accountBaseInfoCacheBuilder.put(baseInfo.getHandicapId() + ":" + baseInfo.getAccount(), baseInfo);
        accountBaseInfoCacheBuilder.put(baseInfo.getId(), baseInfo);
    }

    /**
     * 批量本地缓存
     */
    @Override
    public void flushCache() {
        List<Integer> typeInList = new ArrayList<>();
        typeInList.add(AccountType.InBank.getTypeId());
        typeInList.add(AccountType.OutBank.getTypeId());
        Integer status = AccountStatus.Normal.getStatus();
        Specification<BizAccount> specif = DynamicSpecifications.build(BizAccount.class,
                new SearchFilter("type", SearchFilter.Operator.IN, typeInList.toArray()),
                new SearchFilter("status", SearchFilter.Operator.EQ, status));
        List<BizAccount> bizAccountList = getAccountList(specif, null);
        for (int i = 0; i < bizAccountList.size(); i++) {
            BizAccount vo = bizAccountList.get(i);
            AccountBaseInfo baseInfo = packAccountBaseInfo(vo);
            accountBaseInfoCacheBuilder.put(baseInfo.getType() + "::" + baseInfo.getAccount(), baseInfo);
            accountBaseInfoCacheBuilder.put(baseInfo.getHandicapId() + ":" + baseInfo.getAccount(), baseInfo);
            accountBaseInfoCacheBuilder.put(baseInfo.getId(), baseInfo);
        }
    }

    /**
     * 根据ID查询账号信息(只返回数据库中有的信息)
     *
     * @param id
     *            账号ID
     */
    @Override
    public BizAccount getById(Integer id) {
        return accountRepository.findById2(id);
    }

    /**
     * 获取账号信息(数据库中有的信息)
     *
     * @param specification
     *            查询条件集合
     */
    @Override
    public List<BizAccount> getAccountList(Specification<BizAccount> specification, SysUser sysUser) {
        List<BizAccount> dataToList = accountRepository.findAll(specification);
        if (sysUser == null) {
            dataToList.forEach((p) -> prePackage(p));
            return dataToList;
        } else {
            return packAllForForAccount(sysUser, dataToList);
        }
    }

    /**
     * 查询入款账号List(只返回数据库中有的信息)
     *
     * @param handicapId
     *            盘口ID
     * @param levelIdList
     *            层级ID 集合
     * @param incomeTypeArray
     *            入款账号类型集合
     */
    @Override
    public List<BizAccount> getIncomeAccountList(Integer handicapId, List<Integer> levelIdList,
                                                 Integer[] incomeTypeArray) {
        List<BizAccount> result = new ArrayList<>();
        if (handicapId == null && CollectionUtils.isEmpty(levelIdList)) {
            return result;
        }
        List<SearchFilter> filterList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(levelIdList)) {
            List<Integer> accountIdList = findAccountIdList(levelIdList);
            if (CollectionUtils.isEmpty(accountIdList)) {
                return result;
            }
            filterList.add(new SearchFilter("id", SearchFilter.Operator.IN, accountIdList.toArray()));
        }
        if (handicapId != null) {
            filterList.add(new SearchFilter("handicapId", SearchFilter.Operator.EQ, handicapId));
        }
        if (incomeTypeArray == null || incomeTypeArray.length == 0) {
            filterList.add(new SearchFilter("type", SearchFilter.Operator.IN, INCOME_ACCOUNT_TYPE_ARRAY));
        } else if (incomeTypeArray.length == 1) {
            filterList.add(new SearchFilter("type", SearchFilter.Operator.EQ, incomeTypeArray[0]));
        } else if (incomeTypeArray.length > 1) {
            filterList.add(new SearchFilter("type", SearchFilter.Operator.IN, incomeTypeArray));
        }
        filterList.add(new SearchFilter("status", SearchFilter.Operator.NOTEQ, AccountStatus.Delete.getStatus()));
        List<BizAccount> dataToList = accountRepository.findAll(
                DynamicSpecifications.build(BizAccount.class, filterList.toArray(new SearchFilter[filterList.size()])));
        dataToList.forEach((p) -> prePackage(p));
        return dataToList;
    }

    /**
     * 根据ID获取账号信息(并返回附加信息)
     *
     * @param accountId
     *            账号ID
     */
    @Override
    public BizAccount findById(SysUser operator, Integer accountId) {
        BizAccount result = accountRepository.findById2(accountId);
        if (result != null) {
            if (null != result.getFlag() && result.getFlag() == 2) {
                packZeroLimit(result);
            }
            prePackage(result);
            packBaseInfoForAccount(result);
            // Map<String, String> lockerMap =
            // packLockerMap(RedisKeys.genPattern4TransferAccountLock_to(accountId));
            // String locker = lockerMap.get(String.valueOf(result.getId()));
            // Integer lockerId = StringUtils.isBlank(locker) ? null :
            // Integer.valueOf(locker);
            Integer lockerId = getLockerId(result.getId());
            packLockerForAccount((operator == null ? null : operator.getId()), result, lockerId);
            result.setIncomeAmountDaily(findAmountDailyByTotal(0, result.getId()));// income0outward1
            result.setOutwardAmountDaily(findAmountDailyByTotal(1, result.getId()));// income0outward1
        }
        return result;
    }

    /**
     * 获取账号信息List(并返回附加信息)
     *
     * @param operator
     *            操作者
     * @param specification
     *            查询条件
     */
    @Override
    public List<BizAccount> findList(SysUser operator, Specification<BizAccount> specification) {
        return packAllForForAccount(operator, accountRepository.findAll(specification));
    }

    @Override
    public List<BizAccount> findBySpecificAndSort(SysUser operator, Specification<BizAccount> specification,
                                                  Sort sort) {
        List<BizAccount> list = accountRepository.findAll(specification, sort);
        return list;
    }

    /**
     * 分页获取账号信息(并返回附加信息)
     *
     * @param operator
     *            操作者
     * @param specification
     *            查询条件
     * @param pageable
     *            分页信息
     */
    @Override
    public Page<BizAccount> findPage(SysUser operator, Specification<BizAccount> specification, Pageable pageable)
            throws Exception {
        Page<BizAccount> accountToPage = accountRepository.findAll(specification, pageable);
        List<BizAccount> accountToList = accountToPage.getContent();
        packAllForForAccount(operator, accountToList);
        return accountToPage;
    }

    /**
     * 分页获取账号信息按照当日收款/当日出款排序(并返回附加信息)
     *
     * @param operator
     *            操作者
     * @param accountIdArray
     *            账号ID集合
     * @param pageable
     *            分页信息
     */
    @Override
    public Page<BizAccount> findPageOrderByAmountDaily(SysUser operator, List<Integer> accountIdArray,
                                                       Pageable pageable) throws Exception {
        // 1.信息检测
        Sort.Order incomeDailyOrder, outwardDailyOrder = null;
        if (accountIdArray == null || accountIdArray.size() == 0 || pageable == null
                || ((incomeDailyOrder = pageable.getSort().getOrderFor("incomeAmountDaily")) == null
                && (outwardDailyOrder = pageable.getSort().getOrderFor("outwardAmountDaily")) == null)) {
            return null;
        }
        // 2. ID 按照 当日收款/当日出款 排序
        List<Object> idStringList = new ArrayList<>();
        accountIdArray.forEach((p) -> idStringList.add(String.valueOf(p)));
        Sort.Direction direction = incomeDailyOrder != null ? incomeDailyOrder.getDirection()
                : outwardDailyOrder.getDirection();
        List<Map.Entry<Integer, BigDecimal>> amountDailyList = new ArrayList<>(
                findAmountDaily((incomeDailyOrder != null ? 0 : 1), idStringList).entrySet());
        int positive = Sort.Direction.ASC.ordinal() == direction.ordinal() ? 1 : (-1);
        Collections.sort(amountDailyList, (o1, o2) -> positive * o1.getValue().compareTo(o2.getValue()));
        // 3. ID 截取（fromInex,toInex）
        List<Integer> targetIdList = new ArrayList<>();
        int fromIndex = pageable.getPageNumber() * pageable.getPageSize();
        int toIndex = fromIndex + pageable.getPageSize();
        toIndex = idStringList.size() < toIndex ? idStringList.size() : toIndex;
        amountDailyList.subList(fromIndex, toIndex).forEach((p) -> targetIdList.add(p.getKey()));
        // 4. 信息封装
        SearchFilter idInfilter = new SearchFilter("id", SearchFilter.Operator.IN, targetIdList.toArray());
        Specification<BizAccount> specif = DynamicSpecifications.build(BizAccount.class, idInfilter);
        Map<Integer, BizAccount> accountMap = new HashMap<>();
        accountRepository.findAll(specif).forEach((p) -> accountMap.put(p.getId(), p));
        List<BizAccount> cotentList = new ArrayList<>();
        targetIdList.forEach((p) -> cotentList.add(accountMap.get(p)));
        List<BizAccount> accountToList = packAllForForAccount(operator, cotentList);
        // 5. 分页信息封装
        return new PageImpl<>(accountToList, pageable, idStringList.size());
    }

    @Override
    @Transactional
    public BizAccount saveRebateAcc(BizAccount vo, BizAccountMore more, String uid, AccountFlag flag) {
        flag = Objects.isNull(flag) ? AccountFlag.REFUND : flag;
        vo.setAccount(StringUtils.trimToNull(vo.getAccount()));
        vo = accountRepository.saveAndFlush(vo);
        if (Objects.nonNull(more)) {
            Set<String> ids = new HashSet<>();
            for (String id : StringUtils.trimToEmpty(more.getAccounts()).split(",")) {
                if (StringUtils.isNotBlank(id) && StringUtils.isNumeric(id))
                    ids.add(id);
            }
            ids.add(vo.getId().toString());
            String accounts = CollectionUtils.isEmpty(ids) ? StringUtils.EMPTY : "," + String.join(",", ids) + ",";
            more.setUid(uid);
            more.setAccounts(accounts);
            more.setClassify(flag.getTypeId());
            if (StringUtils.isNotBlank(vo.getMobile()))
                more.setMoible(vo.getMobile());
        } else {
            more = new BizAccountMore();
            more.setUid(uid);
            more.setAccounts(String.format(",%d,", vo.getId()));
            more.setUpdateTime(new Date());
            more.setClassify(flag.getTypeId());
            more.setHandicap(vo.getHandicapId());
            // if (Objects.equals(AccountFlag.SPARETIME.getTypeId(),
            // vo.getFlag()) && vo.getPeakBalance() != null) {
            // more.setMargin(new BigDecimal(vo.getPeakBalance()));
            // }
            if (StringUtils.isNotBlank(vo.getMobile()))
                more.setMoible(vo.getMobile());
        }
        accountMoreService.saveAndFlash(more);
        return vo;
    }

    /**
     * 查找出款人员的出款账号(并返回附加信息)
     */
    @Override
    public List<BizAccount> find4OutwardAsign(Integer userId) {
        SysUser operator = userService.findFromCacheById(userId);
        List<SearchFilter> searchFilterList = new ArrayList<>();
        searchFilterList.add(new SearchFilter("type", SearchFilter.Operator.EQ, AccountType.OutBank.getTypeId()));
        searchFilterList.add(new SearchFilter("status", SearchFilter.Operator.EQ, AccountStatus.Normal.getStatus()));
        if (operator.getId() != 1) {
            searchFilterList.add(new SearchFilter("holder", SearchFilter.Operator.EQ, operator.getId()));
        }
        List<BizAccount> dataToList = accountRepository.findAll(DynamicSpecifications.build(BizAccount.class,
                searchFilterList.toArray(new SearchFilter[searchFilterList.size()])));
        return packAllForForAccount(operator, dataToList);
    }

    /**
     * 查找出款人员的出款账号(并返回附加信息)
     */
    @Override
    public Page<BizAccount> find4OutwardAsign(Integer userId, Integer type, String accountNo, PageRequest pageRequest) {
        SysUser operator = userService.findFromCacheById(userId);
        // List<SearchFilter> searchFilterList = new ArrayList<>();
        // if (StringUtils.isNotBlank(accountNo)) {
        // searchFilterList.add(new SearchFilter("account",
        // SearchFilter.Operator.LIKE,
        // accountNo));
        //
        // }
        // searchFilterList.add(new SearchFilter("type",
        // SearchFilter.Operator.EQ,
        // type));
        // searchFilterList.add(new SearchFilter("status",
        // SearchFilter.Operator.EQ,
        // AccountStatus.Normal.getStatus()));
        // if (userId != 1 && !Objects.equals(type,
        // AccountType.InThird.getTypeId())) {
        // searchFilterList.add(new SearchFilter("bankBalance",
        // SearchFilter.Operator.GTE, 0));
        // searchFilterList.add(new SearchFilter("holder",
        // SearchFilter.Operator.EQ,
        // userId));
        // }
        //
        // Page<BizAccount> page =
        // accountRepository.findAll(DynamicSpecifications.build(BizAccount.class,
        // searchFilterList.toArray(new SearchFilter[searchFilterList.size()])),
        // pageRequest);
        Page<BizAccount> page1 = accountRepository.findAll((root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicateList = new ArrayList<>();
            Path<String> accountPath = root.get("account");
            Path<String> bankNamePath = root.get("bankName");
            Path<Integer> typePath = root.get("type");
            Path<Integer> statusPath = root.get("status");
            Path<BigDecimal> bankBalancePath = root.get("bankBalance");
            Path<Integer> holderPath = root.get("holder");
            Predicate p1 = criteriaBuilder.equal(statusPath, AccountStatus.Normal.getStatus());
            if (Objects.equals(type, AccountType.InThird.getTypeId())) {
                Predicate p7 = criteriaBuilder.equal(statusPath, AccountStatus.StopTemp.getStatus());
                predicateList.add(criteriaBuilder.or(p1, p7));
            } else {
                predicateList.add(criteriaBuilder.and(p1));
            }
            Predicate p2 = criteriaBuilder.equal(typePath, type);
            predicateList.add(criteriaBuilder.and(p2));
            if (StringUtils.isNotBlank(accountNo)) {
                if (Objects.equals(type, AccountType.InThird.getTypeId())) {
                    Predicate p3 = criteriaBuilder.like(accountPath, accountNo);
                    Predicate p4 = criteriaBuilder.like(bankNamePath, accountNo);
                    predicateList.add(criteriaBuilder.or(p4, p3));
                } else {
                    Predicate p3 = criteriaBuilder.like(accountPath, accountNo);
                    predicateList.add(criteriaBuilder.and(p3));
                }
            }
            if (userId != 1 && !Objects.equals(type, AccountType.InThird.getTypeId())) {
                Predicate p5 = criteriaBuilder.ge(bankBalancePath, 0);
                Predicate p6 = criteriaBuilder.equal(holderPath, userId);
                predicateList.add(criteriaBuilder.and(p5));
                predicateList.add(criteriaBuilder.and(p6));
            }
            criteriaQuery.where(predicateList.toArray(new Predicate[predicateList.size()]));
            return null;
        }, pageRequest);

        packAllForForAccount(operator, page1.getContent());
        return page1;
    }

    /**
     * 查询出款账号总记录数
     */
    @Override
    public Long find4OutwardAsignCount(Integer userId, Integer type, String accountNo) {
        long count = accountRepository.count((root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicateList = new ArrayList<>();
            Path<String> accountPath = root.get("account");
            Path<String> bankNamePath = root.get("bankName");
            Path<Integer> typePath = root.get("type");
            Path<Integer> statusPath = root.get("status");
            Path<BigDecimal> bankBalancePath = root.get("bankBalance");
            Path<Integer> holderPath = root.get("holder");
            Predicate p1 = criteriaBuilder.equal(statusPath, AccountStatus.Normal.getStatus());
            if (Objects.equals(type, AccountType.InThird.getTypeId())) {
                Predicate p7 = criteriaBuilder.equal(statusPath, AccountStatus.StopTemp.getStatus());
                predicateList.add(criteriaBuilder.or(p1, p7));
            } else {
                predicateList.add(criteriaBuilder.and(p1));
            }
            Predicate p2 = criteriaBuilder.equal(typePath, type);
            predicateList.add(criteriaBuilder.and(p2));
            if (StringUtils.isNotBlank(accountNo)) {
                if (Objects.equals(type, AccountType.InThird.getTypeId())) {
                    Predicate p3 = criteriaBuilder.like(accountPath, accountNo);
                    Predicate p4 = criteriaBuilder.like(bankNamePath, accountNo);
                    predicateList.add(criteriaBuilder.or(p4, p3));
                } else {
                    Predicate p3 = criteriaBuilder.like(accountPath, accountNo);
                    predicateList.add(criteriaBuilder.and(p3));
                }
            }
            if (userId != 1 && !Objects.equals(type, AccountType.InThird.getTypeId())) {
                Predicate p5 = criteriaBuilder.ge(bankBalancePath, 0);
                Predicate p6 = criteriaBuilder.equal(holderPath, userId);
                predicateList.add(criteriaBuilder.and(p5));
                predicateList.add(criteriaBuilder.and(p6));
            }
            criteriaQuery.where(predicateList.toArray(new Predicate[predicateList.size()]));
            return null;
        });
        return count;// accountRepository.count4OutwardAsign(userId, type,
        // accountNo);
    }

    @Override
    @Transactional
    public BizAccount create(List<BizAccountLevel> accountLevelList, BizAccount vo) {
        try {
            vo.setAccount(StringUtils.trimToNull(vo.getAccount()));
            BizAccount result = accountRepository.saveAndFlush(vo);
            if (vo.getHandicapId() != null && !CollectionUtils.isEmpty(accountLevelList)) {
                for (BizAccountLevel accountLevel : accountLevelList) {
                    accountLevel.setAccountId(result.getId());
                }
                accountLevelRepository.save(accountLevelList);
            }
            return result;
        } catch (Exception e) {
            log.error("绑定银行卡账号和层级关系失败:", e);
        }
        return null;
    }

    @Override
    @Transactional
    public BizAccount create(BizHandicap handicap, AccountType accType, AccountStatus accStatus,
                             CurrentSystemLevel level, String acc, String owner, BigDecimal limitIn, String bankAddr, SysUser opr,
                             String remark) throws Exception {
        String account = StringUtils.trimToNull(acc);
        if (account == null || accType == null || accStatus == null || level == null || opr == null) {
            throw new Exception("账号/分类/状态/内外层/操着者不能为空");
        }
        List<BizAccount> hisList = accountRepository.findByAccount(account);
        if (Objects.equals(AccountType.InAli.getTypeId(), accType.getTypeId())) {
            if (hisList.stream().filter(p -> Objects.equals(p.getAccount(), account)).count() > 0) {
                throw new Exception("支付宝账号已存在");
            }
        } else if (Objects.equals(AccountType.InWechat.getTypeId(), accType.getTypeId())) {
            if (hisList.stream().filter(p -> Objects.equals(p.getAccount(), account)).count() > 0) {
                throw new Exception("微信账号账号已存在");
            }
        } else if (Objects.equals(AccountType.InThird.getTypeId(), accType.getTypeId())) {
            throw new Exception("不能添加responseDataNewPay != null && responseDataNewPay.getCode() == 200账号.");
        } else {
            if (hisList.stream()
                    .filter(p -> !Objects.equals(p.getType(), AccountType.InAli.getTypeId())
                            && !Objects.equals(p.getType(), AccountType.InWechat.getTypeId())
                            && !Objects.equals(p.getType(), AccountType.InThird.getTypeId()))
                    .count() > 0) {
                throw new Exception("银行卡账号已存在.");
            }
        }
        Date d = new Date();
        BizAccount vo = new BizAccount();
        vo.setHandicapId(handicap == null ? null : handicap.getId());
        vo.setAccount(account);
        vo.setStatus(accStatus.getStatus());
        vo.setType(accType.getTypeId());
        vo.setCurrSysLevel(level.getValue());
        vo.setOwner(owner);
        vo.setBankName(bankAddr);
        vo.setUpdateTime(d);
        vo.setCreateTime(d);
        vo.setHolder(opr.getId());
        vo.setCreator(opr.getId());
        vo.setModifier(opr.getId());
        vo.setBankBalance(BigDecimal.ZERO);
        vo.setRemark(CommonUtils.genRemark(null, "【账号创建】" + StringUtils.trimToEmpty(remark), d, opr.getUid()));
        vo.setAlias(null);
        if (!Arrays.asList(new Integer[] { AccountType.InThird.getTypeId(), AccountType.InAli.getTypeId(),
                AccountType.InWechat.getTypeId(), AccountType.OutThird.getTypeId() }).contains(vo.getType())) {
            String maxAlias = getMaxAlias();
            if (StringUtils.isEmpty(maxAlias) || maxAlias.equals("0")) {
                vo.setAlias("100000");
            } else {
                int alias = Integer.parseInt(maxAlias) + 1;
                vo.setAlias(Integer.toString(alias).replace("4", "5"));
            }
        }
        return accountRepository.save(vo);
    }

    @Transactional
    @Override
    @Deprecated
    public void addBalance(BigDecimal amount, Integer id) {
        accountRepository.addBalance(amount, id);
    }

    @Override
    @Transactional
    public void updateBaseInfo(BizAccount vo) {
        accountRepository.updateBaseInfo(vo.getHandicapId(), vo.getStatus(), StringUtils.trimToNull(vo.getAccount()),
                vo.getBankName(), vo.getOwner(), vo.getType(), vo.getBankType(), vo.getLimitIn(), vo.getLimitOut(),
                vo.getAlias(), vo.getModifier(), vo.getHolder(), vo.getCurrSysLevel(), vo.getLowestOut(), vo.getId(),
                vo.getLimitBalance(), vo.getPeakBalance(), vo.getRemark(), vo.getLimitOutOne(), vo.getMobile(),
                vo.getSubType());
    }

    @Override
    public List<Collection<Object>> findHandicapAndLevel(Integer... accountIdArray) {
        List<Collection<Object>> result = new ArrayList<>();
        if (accountIdArray == null || accountIdArray.length == 0) {
            return result;
        }
        Map<Integer, Object> handicapMap = new HashMap<>();
        Map<Integer, Object> levelMap = new HashMap<>();
        for (Integer accountId : accountIdArray) {
            AccountBaseInfo baseInfo = getFromCacheById(accountId);
            if (!CollectionUtils.isEmpty(baseInfo.getLevelList())) {
                baseInfo.getLevelList().forEach((p) -> levelMap.put(p.getId(), p));
            }
            BizAccount account = getById(accountId);
            BizHandicap handicap = handicapService.findFromCacheById(account.getHandicapId());
            handicapMap.put(handicap.getId(), handicap);
        }
        result.add(handicapMap.values());
        result.add(levelMap.values());
        return result;
    }

    /**
     * 根据条件集合查询账号ID
     *
     * @param filterToArray
     *            条件集合
     */
    @Override
    public List<Integer> findAccountIdList(SearchFilter... filterToArray) {
        List<Integer> result = new ArrayList<>();
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<BizAccount> root = query.from(BizAccount.class);
        javax.persistence.criteria.Predicate[] predicateArray = DynamicPredicate.build(cb, query, root,
                BizAccount.class, filterToArray);
        query.multiselect(root.<BigDecimal>get("id"));
        query.where(predicateArray);
        entityManager.createQuery(query).getResultList().forEach((p) -> result.add(((Integer) p.get(0))));
        return result;
    }

    @Override
    public List<Object[]> findAccountIdAndBalList(SearchFilter... filterToArray) {
        List<Object[]> result = new ArrayList<>();
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<BizAccount> root = query.from(BizAccount.class);
        javax.persistence.criteria.Predicate[] predicateArray = DynamicPredicate.build(cb, query, root,
                BizAccount.class, filterToArray);
        query.multiselect(root.<BigDecimal>get("id"), root.<BigDecimal>get("bankBalance"),
                root.<BigDecimal>get("balance"), root.<BigDecimal>get("type"), root.<BigDecimal>get("status"));
        query.where(predicateArray);
        entityManager.createQuery(query).getResultList().forEach((p) -> {
            if ((Integer) p.get(3) == 2) {
                result.add(new Object[] { ((Integer) p.get(0)), ((BigDecimal) p.get(2)) });
            } else {
                result.add(new Object[] { ((Integer) p.get(0)), ((BigDecimal) p.get(1)) });
            }
        });
        return result;
    }

    /**
     * 获取某一层级下的账号ID集
     *
     * @param levelId
     *            层级ID
     * @return 账号ID集
     */
    @Override
    public List<Integer> findAccountIdList(Integer levelId) {
        if (Objects.isNull(levelId)) {
            return Collections.emptyList();
        }
        return accountLevelRepository.findByLevelId(levelId).stream().parallel().mapToInt(p -> p.getAccountId())
                .mapToObj(p -> p).collect(Collectors.toList());
    }

    /**
     * 先查找缓存中是否有用户的入款账号，如果没有查询数据库，放在缓存返回。在更新用户权限的时候也会更新缓存
     */
    @Override
    public List<Integer> findIncomeAccountIdList4User(boolean update, int userId) {
        if (update) {
            List<Integer> list = findIncomeAccountIdList(null, null, userId);
            inComeAccountIdList4User.put("IncomeAccountIdListFor" + userId, list);
            return list;
        } else {
            List<Integer> list = inComeAccountIdList4User.getIfPresent("IncomeAccountIdListFor" + userId);
            if (list != null && list.size() > 0) {
                return list;
            } else {
                list = findIncomeAccountIdList(null, null, userId);
                inComeAccountIdList4User.put("IncomeAccountIdListFor" + userId, list);
                return list;
            }
        }
    }

    /**
     * 查询入款账号集合 </br>
     * <p>
     * operatorId 所能操作的账号ID集合
     * </p>
     *
     * @param handicapId
     *            盘口ID
     * @param levelId
     *            层级IDd
     * @param operatorId
     *            操作者ID 不为null
     */
    @Override
    public List<Integer> findIncomeAccountIdList(Integer handicapId, Integer levelId, Integer operatorId) {
        List<Integer> result = new ArrayList<>();
        if (operatorId == null) {
            return result;
        }
        List<Integer> levelIdToList = new ArrayList<>();
        levelIdToList.addAll(dataPermissionService.findLevelIdList(operatorId));
        if (levelId != null) {
            levelIdToList.add(levelId);
        }
        result.addAll(findAccountIdList(levelIdToList));
        if (handicapId != null) {
            List<SearchFilter> filterList = new ArrayList<>();
            filterList.add(new SearchFilter("id", SearchFilter.Operator.IN, result.toArray()));
            filterList.add(new SearchFilter("handicapId", SearchFilter.Operator.EQ, handicapId));
            filterList.add(new SearchFilter("type", SearchFilter.Operator.IN, INCOME_ACCOUNT_TYPE_ARRAY));
            Specification<BizAccount> specification = DynamicSpecifications.build(BizAccount.class,
                    filterList.toArray(new SearchFilter[filterList.size()]));
            result.clear();
            accountRepository.findAll(specification).forEach((p) -> result.add(p.getId()));
        }
        return result;
    }

    /**
     *
     */
    @Override
    public List<Integer> findLockAccountIdList(boolean isCurrentUser, int lockerId) {
        List<Integer> result = new ArrayList<>();
        Set<String> keys = redisService.getStringRedisTemplate().keys(RedisKeys.genPattern4TransferAccountLock());
        if (CollectionUtils.isEmpty(keys)) {
            return result;
        }
        String locker = String.valueOf(lockerId);
        for (String key : keys) {
            String[] inf = key.split(":");
            boolean flag = locker.equals(inf[3]);
            if ((isCurrentUser && flag) || (!isCurrentUser && !flag)) {
                result.add(Integer.valueOf(key.split(":")[2]));
            }
        }
        return result;
    }

    /**
     * 查询当日收款，当日出款的总计
     *
     * @param income0outward1
     *            0:入款；1：出款
     * @param accountIdArray
     *            账号集合
     */
    @Override
    public BigDecimal findAmountDailyByTotal(int income0outward1, Integer... accountIdArray) {
        BigDecimal result = BigDecimal.ZERO;
        if (accountIdArray == null || accountIdArray.length == 0 || (income0outward1 != 0 && income0outward1 != 1)) {
            return result;
        }
        List<Object> keyList = new ArrayList<>();
        for (Integer accountId : accountIdArray) {
            keyList.add(String.valueOf(accountId));
        }
        String k = income0outward1 != 0 ? RedisKeys.AMOUNT_SUM_BY_DAILY_OUTWARD : RedisKeys.AMOUNT_SUM_BY_DAILY_INCOME;
        List<Object> amountList = redisService.getFloatRedisTemplate().opsForHash().multiGet(k, keyList);
        DecimalFormat decimalFormat = new DecimalFormat(".00");
        for (Object amount : amountList) {
            result = result.add(amount == null ? BigDecimal.ZERO : new BigDecimal(decimalFormat.format(amount)));
        }
        return result;
    }

    /**
     * 单个账号的 当日出款入款总额 <br>
     * K:账号id V:金额
     *
     * @param income0outward1
     * @return
     */
    @Override
    public Map<Integer, BigDecimal> allAmountDailyTotal(int income0outward1, List<Integer> accountIds) {
        Assert.notNull(accountIds, "账号id为空");
        String key = income0outward1 != 0 ? RedisKeys.AMOUNT_SUM_BY_DAILY_OUTWARD
                : RedisKeys.AMOUNT_SUM_BY_DAILY_INCOME;
        Map<Integer, BigDecimal> res = Maps.newLinkedHashMap();
        // Map<Integer, AccountBaseInfo> accountBaseInfoMap =
        // allFromCacheById();
        StringRedisTemplate template = redisService.getStringRedisTemplate();
        if (template.hasKey(key)) {
            DecimalFormat decimalFormat = new DecimalFormat(".00");
            HashOperations operations = template.opsForHash();
            List list = operations.multiGet(key,
                    accountIds.stream().map(p -> p.toString()).collect(Collectors.toList()));
            if (!CollectionUtils.isEmpty(list)) {
                for (int i = 0, size = accountIds.size(); i < size; i++) {
                    Integer accountId = accountIds.get(i);
                    Object obj = list.get(i);
                    if (obj == null) {
                        res.put(accountId, BigDecimal.ZERO);
                    } else {
                        Number number = Double.valueOf(obj + "");
                        res.put(accountId, new BigDecimal(decimalFormat.format(number)));
                    }
                }
            }
        }
        return res;
    }

    /**
     * 条件查询系统余额与银行余额的总计
     *
     * @param filterToArray
     *            条件结果集合
     */
    @Override
    public BigDecimal[] findBalanceAndBankBalanceByTotal(SearchFilter[] filterToArray) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<BizAccount> root = query.from(BizAccount.class);
        javax.persistence.criteria.Predicate[] predicateArray = DynamicPredicate.build(cb, query, root,
                BizAccount.class, filterToArray);
        query.multiselect(cb.sum(root.<BigDecimal>get("balance")), cb.sum(root.<BigDecimal>get("bankBalance")));
        query.where(predicateArray);
        Object[] objArray = entityManager.createQuery(query).getSingleResult().toArray();
        return new BigDecimal[] { (BigDecimal) objArray[0], (BigDecimal) objArray[1] };
    }

    /**
     * 获取账号的统计信息
     *
     * @param category
     *            统计分类
     * @param accountIdList
     *            需统计账号ID集合
     */
    @Override
    public List<AccountStatInOut> findStatInOut(AccountStatInOut.Category category, List<Integer> accountIdList) {
        if (CollectionUtils.isEmpty(accountIdList)) {
            return new ArrayList<>();
        }
        StringBuilder inWhere = new StringBuilder(), outWhere = new StringBuilder(), outwardWhere = new StringBuilder();
        for (Integer id : accountIdList) {
            if (category.getValue() == AccountStatInOut.Category.In.getValue()) {
                inWhere.append(" or to_id=").append(id);
                inWhere.append(" and create_time >='").append(CommonUtils.getStartTimeOfCurrDay()).append("'");
            } else if (category.getValue() == AccountStatInOut.Category.OutMember.getValue()) {
                outwardWhere.append(" or account_id=").append(id);
                outwardWhere.append(" and asign_time >='").append(CommonUtils.getStartTimeOfCurrDay()).append("'");
            } else {
                outWhere.append(" or from_id=").append(id);
                outWhere.append(" and create_time >='").append(CommonUtils.getStartTimeOfCurrDay()).append("'");
            }
        }
        if (StringUtils.isNotBlank(outwardWhere.toString())) {
            // Query outwardQative =
            // entityManager.createNativeQuery(String.format(FORMAT_SQL_OUTWARD,
            // AccountStatInOut.Category.OutMember.getValue(),
            // outwardWhere.toString()));
            Query outwardQative = entityManager.createNativeQuery(String.format(FORMAT_SQL_OUTWARD_,
                    AccountStatInOut.Category.OutMember.getValue(), outwardWhere.toString(), outwardWhere.toString()));
            outwardQative.unwrap(SQLQuery.class).setResultTransformer(Transformers.aliasToBean(AccountStatInOut.class));
            return outwardQative.getResultList();
        } else if (StringUtils.isNotBlank(outWhere.toString())) {
            Query outQative = entityManager.createNativeQuery(String.format(FORMAT_SQL_TRANSFER, "from_id",
                    AccountStatInOut.Category.OutTranfer.getValue(), outWhere.toString(), "from_id"));
            outQative.unwrap(SQLQuery.class).setResultTransformer(Transformers.aliasToBean(AccountStatInOut.class));
            return outQative.getResultList();
        } else {
            Query inQative = entityManager.createNativeQuery(String.format(FORMAT_SQL_TRANSFER, "to_id",
                    AccountStatInOut.Category.In.getValue(), inWhere.toString(), "to_id"));
            inQative.unwrap(SQLQuery.class).setResultTransformer(Transformers.aliasToBean(AccountStatInOut.class));
            return inQative.getResultList();
        }
    }

    /**
     * 获取账号统计信息
     *
     * @param accountIdList
     *            账号ID集合
     * @return key:id </br>
     *         value[0] 转入未对账金额 </br>
     *         value[1] 转入未对账笔数</br>
     *         value[2] 转出未对账金额</br>
     *         value[3] 转出未对账笔数</br>
     *         value[4] 银行已确认金额</br>
     *         value[5] 银行已确认笔数</br>
     *         value[6] 平台已确认金额</br>
     *         value[7] 平台已确认笔数</br>
     */
    @Override
    @SuppressWarnings("unchecked")
    public Map<Integer, BigDecimal[]> findStat(List<Integer> accountIdList) {
        if (CollectionUtils.isEmpty(accountIdList)) {
            return Collections.emptyMap();
        }
        Map<Integer, BigDecimal[]> result = new HashMap<>();
        // 账号分类
        Set<String> inBank = new HashSet<>(), ouBank = new HashSet<>(), isBank = new HashSet<>(),
                rsvBank = new HashSet<>();
        accountIdList.forEach(p -> {
            try {
                AccountBaseInfo acc = getFromCacheById(p);
                if (Objects.nonNull(acc)) {
                    if (Objects.equals(acc.getType(), AccountType.InBank.getTypeId())) {
                        inBank.add(String.valueOf(p));
                    }
                    if (Objects.equals(acc.getType(), AccountType.OutBank.getTypeId())) {
                        ouBank.add(String.valueOf(p));
                    }
                    if (Objects.equals(acc.getType(), AccountType.BindWechat.getTypeId())
                            || Objects.equals(acc.getType(), AccountType.BindCommon.getTypeId())
                            || Objects.equals(acc.getType(), AccountType.BindAli.getTypeId())
                            || Objects.equals(acc.getType(), AccountType.ThirdCommon.getTypeId())) {
                        isBank.add(String.valueOf(p));
                    }
                    if (Objects.equals(acc.getType(), AccountType.ReserveBank.getTypeId())) {
                        rsvBank.add(String.valueOf(p));
                    }
                }
            } catch (Exception e) {
            }
            result.put(p, new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO });
        });
        String startTime = CommonUtils.getStartTimeOfCurrDay();
        // 转入未匹配
        // 转出未匹配
        // 银行已确认
        List<String> idStrList = accountIdList.stream().mapToInt(p -> p).mapToObj(String::valueOf)
                .collect(Collectors.toList());
        String fmtInOut = "select from_account frAcc,amount amt,status from biz_bank_log where status in (0,1) and create_time>='%s' and from_account in (%s)";
        String sqlInOut = String.format(fmtInOut, startTime, String.join(",", idStrList));
        log.debug("获取账号统计信息->转入未匹配,转出未匹配 sql:{}", sqlInOut);
        entityManager.createNativeQuery(sqlInOut).getResultList().forEach(p -> {
            Object[] vals = (Object[]) p;
            int id = (Integer) vals[0], status = (Integer) vals[2];
            BigDecimal amt = (BigDecimal) vals[1];
            String idStr = String.valueOf(id);
            BigDecimal[] data = result.get(id);
            if (status == BankLogStatus.Matching.getStatus()) {
                if (amt.compareTo(BigDecimal.ZERO) > 0) {
                    data[0] = data[0].add(amt);
                    data[1] = data[1].add(BigDecimal.ONE);
                } else {
                    data[2] = data[2].add(amt);
                    data[3] = data[3].add(BigDecimal.ONE);
                }
            } else {
                if (ouBank.contains(idStr)) {
                    if (amt.compareTo(BigDecimal.ZERO) < 0) {
                        data[4] = data[4].add(amt.abs());
                        data[5] = data[5].add(BigDecimal.ONE);
                    }
                } else if (inBank.contains(idStr)) {
                    if (amt.compareTo(BigDecimal.ZERO) > 0) {
                        data[4] = data[4].add(amt.abs());
                        data[5] = data[5].add(BigDecimal.ONE);
                    }
                } else if (isBank.contains(idStr)) {
                    data[4] = data[4].add(amt.abs());
                    data[5] = data[5].add(BigDecimal.ONE);
                } else if (rsvBank.contains(idStr)) {
                    data[4] = data[4].add(amt.abs());
                    data[5] = data[5].add(BigDecimal.ONE);
                }
            }
        });
        // 出款平台已确认
        List<Object> dataOList = null;
        if (!CollectionUtils.isEmpty(ouBank)) {
            String fmtO = "select task.account_id id,task.amount amount from biz_outward_task task,biz_outward_request req where task.outward_request_id=req.id  and req.status=%d and task.status!=%d  and req.update_time>='%s' ";
            if (ouBank.size() == 1) {
                fmtO += " and task.account_id =%s";
            } else {
                fmtO += " and task.account_id in (%s)";
            }
            String sqlO = String.format(fmtO, OutwardTaskStatus.Invalid.getStatus(),
                    OutwardRequestStatus.Acknowledged.getStatus(), startTime, String.join(",", ouBank));
            log.debug("获取账号统计信息->出款平台已确认 sql:{}", sqlO);
            dataOList = entityManager.createNativeQuery(sqlO).getResultList();
        }
        // 入款平台已确认
        if (!CollectionUtils.isEmpty(inBank)) {
            String fmtI = "select to_id,amount from biz_income_request req where type<100 and status=%d and req.update_time is not null and req.update_time>='%s' and to_id in (%s)";
            String sqlI = String.format(fmtI, IncomeRequestStatus.Matched.getStatus(), startTime,
                    String.join(",", inBank));
            log.debug("获取账号统计信息->入款平台已确认 sql:{}", sqlI);
            dataOList = entityManager.createNativeQuery(sqlI).getResultList();

        }
        if (!CollectionUtils.isEmpty(dataOList)) {
            dataOList.forEach(p -> {
                Object[] vals = (Object[]) p;
                int id = (Integer) vals[0];
                BigDecimal amt = (BigDecimal) vals[1];
                BigDecimal[] data = result.get(id);
                data[6] = data[6].add(amt.abs());
                data[7] = data[7].add(BigDecimal.ONE);
            });
        }
        return result;
    }

    /**
     * 获取客户绑定卡转入转出统计信息
     *
     * @param accountIdList
     *            客户绑定卡ID集合
     * @return key:id</br>
     *         value[0] 当日转入流水条数</br>
     *         value[1] 当日转出未匹配条数</br>
     *         value[2] 当日转出已匹配条数</br>
     */
    @Override
    public Map<Integer, BigDecimal[]> findStat4BindCustomer(List<Integer> accountIdList) {
        if (CollectionUtils.isEmpty(accountIdList))
            return Collections.emptyMap();
        Map<Integer, BigDecimal[]> ret = new HashMap<Integer, BigDecimal[]>() {
            {
                for (Integer accountId : accountIdList)
                    put(accountId, new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO });
            }
        };
        String startTime = CommonUtils.getStartTimeOfCurrDay();
        List<String> idStrList = accountIdList.stream().mapToInt(p -> p).mapToObj(String::valueOf)
                .collect(Collectors.toList());
        String fmtIn = "select from_account frAcc,amount amt,status from biz_bank_log where amount>0 and create_time>='%s' and from_account in (%s)";
        String sqlIn = String.format(fmtIn, startTime, String.join(",", idStrList));
        log.debug("获取账号统计信息 -> 转入未匹配,转出未匹配 sql:{}", sqlIn);
        entityManager.createNativeQuery(sqlIn).getResultList().forEach(p -> {
            Object[] vals = (Object[]) p;
            BigDecimal[] data = ret.get(vals[0]);
            data[0] = data[0].add(BigDecimal.ONE);
        });
        // 出款平台已确认
        List<AccountStatInOut> outList = findStatInOut(AccountStatInOut.Category.OutMember, accountIdList);
        if (!CollectionUtils.isEmpty(outList)) {
            for (AccountStatInOut item : outList) {
                BigDecimal[] stat = ret.get(item.getId());
                stat[1] = new BigDecimal(item.getMapping());
                stat[2] = new BigDecimal(item.getMapped());
            }
        }
        return ret;
    }

    @Override
    public List<AccountStatInOut> findStatFromTo(String[] fromToArray) {
        StringBuilder where = new StringBuilder();
        for (String fromTo : fromToArray) {
            String[] str = fromTo.split(":");
            if (str.length == 2) {
                where.append("or (from_id=").append(str[0]).append("  and to_id=").append(str[1]).append(")");
                where.append(" and create_time between").append(" '" + TimeChangeCommon.getTodayStartTime() + "'")
                        .append(" and").append(" '" + TimeChangeCommon.getTodayEndTime() + "'");
            }
        }
        Query outwardQative = entityManager.createNativeQuery(
                String.format(FORMAT_SQL_FROMTO, AccountStatInOut.Category.In.getValue(), where.toString()));
        outwardQative.unwrap(SQLQuery.class).setResultTransformer(Transformers.aliasToBean(AccountStatInOut.class));
        return outwardQative.getResultList();
    }

    /**
     * param List<Object> Object exclusive String</>
     */
    @Override
    public Map<Integer, BigDecimal> findAmountDaily(int income0outward1, List<Object> idList) {
        Map<Integer, BigDecimal> result = new HashMap<>();
        if (CollectionUtils.isEmpty(idList) || (income0outward1 != 0 && income0outward1 != 1)) {
            return result;
        }
        DecimalFormat decimalFormat = new DecimalFormat(".000");
        String k = income0outward1 != 1 ? RedisKeys.AMOUNT_SUM_BY_DAILY_INCOME : RedisKeys.AMOUNT_SUM_BY_DAILY_OUTWARD;
        List<Object> dataList = redisService.getFloatRedisTemplate().opsForHash().multiGet(k,
                idList.stream().map(p -> p.toString()).collect(Collectors.toList()));
        for (int index = 0; index < idList.size(); index++) {
            Float amount = (Float) dataList.get(index);
            result.put(Integer.valueOf(String.valueOf(idList.get(index))),
                    amount == null ? BigDecimal.ZERO : new BigDecimal(decimalFormat.format(amount)));
        }
        return result;
    }

    @Override
    public Map<String, Float> findOutCountDaily() {
        Map map = redisService.getFloatRedisTemplate().opsForHash().entries(RedisKeys.COUNT_SUM_BY_DAILY_OUTWARD);
        log.debug("redis缓存中的出款笔数缓存:{}", map);
        return map;
    }

    @Override
    public Float findOutCountDaily(int accId) {
        return (Float) (Object) (redisService.getFloatRedisTemplate().opsForHash()
                .get(RedisKeys.COUNT_SUM_BY_DAILY_OUTWARD, String.valueOf(accId)));
    }

    @Override
    public List<BizAccount> packAllForForAccount(SysUser operator, List<BizAccount> accountList) {
        if (!CollectionUtils.isEmpty(accountList)) {
            // Map<String, String> lockerMap =
            // packLockerMap(RedisKeys.genPattern4TransferAccountLock());
            List<Object> idList = new ArrayList<>();
            BizAccount accountType = accountList.get(0);
            for (BizAccount account : accountList) {
                idList.add(String.valueOf(account.getId()));
                prePackage(account);
                packBaseInfoForAccount(account);
                // String locker =
                // lockerMap.get(String.valueOf(account.getId()));
                // Integer lockerId = StringUtils.isBlank(locker) ? null :
                // Integer.valueOf(locker);
                Integer lockerId = getLockerId(account.getId());
                packLockerForAccount((operator == null ? null : operator.getId()), account, lockerId);
                account.setInCount(DEFAULT_STAT_IN);
                if (AccountType.OutThird.getTypeId().equals(account.getType())
                        || AccountType.OutBank.getTypeId().equals(account.getType())) {
                    account.setOutCount(DEFAULT_STAT_OUT_MEMBER);
                } else {
                    account.setOutCount(DEFAULT_STAT_OUT_TRANS);
                }
            }
            // 如果是第三方账号则统计第三方账号下发已匹配、未匹配的金额 因为账号是根据公司账号、第三方传过来的 所以第一个如果是第三方账号
            // 剩下的全是第三方账号
            List<Object[]> issuedAmounts = null;
            if (AccountType.InThird.getTypeId().equals(accountType.getType())) {
                issuedAmounts = accountExtraRespository.findIssuedAmounts(idList, TimeChangeCommon.getToday5StartTime(),
                        TimeChangeCommon.getToday5EndTime());
            }
            Map<Integer, BigDecimal> incomeDailyMap = findAmountDaily(0, idList);// income0outward1
            Map<Integer, BigDecimal> outwardDailyMap = findAmountDaily(1, idList);// income0outward1
            for (BizAccount account : accountList) {
                // 如果是第三方账号则统计第三方账号下发已匹配、未匹配的金额
                if (AccountType.InThird.getTypeId().equals(account.getType()) && issuedAmounts != null) {
                    for (int i = 0; i < issuedAmounts.size(); i++) {
                        Object[] objectValue = issuedAmounts.get(i);
                        if (account.getId() == (int) objectValue[0]) {
                            account.setMappingAmount(new BigDecimal(objectValue[1].toString()));
                            account.setMappedAmount(new BigDecimal(objectValue[2].toString()));
                        }
                    }
                }
                account.setIncomeAmountDaily(incomeDailyMap.get(account.getId()));
                account.setOutwardAmountDaily(outwardDailyMap.get(account.getId()));
            }
        }
        return accountList;
    }

    private BizAccount prePackage(BizAccount account) {
        if (account == null) {
            return null;
        }
        account.setSignAndHook(!StringUtils.isAnyBlank(account.getSign_(), account.getHook_()));
        // account.setHook(null);
        // account.setSign(null);
        return account;
    }

    private BizAccount packLockerForAccount(Integer operatorId, BizAccount account, Integer lockerId) {
        if (account == null) {
            return null;
        }
        account.setLockByOperator(0);
        if (lockerId != null) {
            SysUser locker = userService.findFromCacheById(lockerId);
            account.setLockByOperator(operatorId != null && operatorId.equals(locker.getId()) ? 1 : 0);
            account.setLockerStr(locker.getUid());
            account.setLocker(locker.getId());
        }
        return account;
    }

    private BizAccount packBaseInfoForAccount(BizAccount account) {
        if (account == null) {
            return null;
        }
        SysUser operator;
        if (account.getHolder() != null && (operator = userService.findFromCacheById(account.getHolder())) != null) {
            account.setHolderStr(operator.getUid());
        } else if (AccountType.OutBank.getTypeId().equals(account.getType())
                && AccountStatus.Normal.getStatus().equals(account.getStatus())) {
            if (Objects.equals(account.getFlag(), 1)) {
                account.setHolderStr("手机");
            } else {
                account.setHolderStr("PC");
            }
        }
        SysUser creator;
        if (account.getCreator() != null && (creator = userService.findFromCacheById(account.getCreator())) != null) {
            account.setCreatorStr(creator.getUid());
        }
        SysUser modifier;
        if (account.getModifier() != null
                && (modifier = userService.findFromCacheById(account.getModifier())) != null) {
            account.setModifierStr(modifier.getUid());
        }
        if (account.getHandicapId() != null) {
            BizHandicap handicap = handicapService.findFromCacheById(account.getHandicapId());
            account.setHandicapName(handicap != null ? handicap.getName() : StringUtils.EMPTY);
        }
        if (AccountType.isIncome(account.getType())) {
            AccountBaseInfo baseInfo = getFromCacheById(account.getId());
            if (!CollectionUtils.isEmpty(baseInfo.getLevelList())) {
                baseInfo.getLevelList().forEach((p) -> {
                    String temp = account.getLevelNameToGroup();
                    account.setLevelNameToGroup(StringUtils.isBlank(temp) ? p.getName() : temp + "|" + p.getName());
                });
            } else {
                account.setLevelNameToGroup(StringUtils.EMPTY);
            }
        }
        if (AccountType.InBank.getTypeId().equals(account.getType())) {
            String keyPattern = RedisKeys.genPattern4IncomeAuditAccountAllocateByAccountId(account.getId());
            Set<String> keys = redisService.getStringRedisTemplate().keys(keyPattern);
            for (String k : keys) {
                SysUser incomeAuditor = userService.findFromCacheById(Integer.valueOf(k.split(":")[1]));
                account.setIncomeAuditor(incomeAuditor == null ? StringUtils.EMPTY : incomeAuditor.getUid());
                break;
            }
            if (!Objects.equals(account.getSubType(), InBankSubType.IN_BANK_YSF)
                    && Objects.isNull(account.getOutEnable())) {
                List<SysUserProfile> profiles = systemSettingService
                        .findByPropertyKey(UserProfileKey.FLW_DEFAULT_INCOME_CAN_USE_OUT_ENBLED.getValue());
                if (!CollectionUtils.isEmpty(profiles)) {
                    byte enable = "1".equals(profiles.get(0).getPropertyValue()) ? (byte) 1 : (byte) 0;
                    account.setOutEnable(enable);
                }
            }
        }
        if (account.getCurrSysLevel() != null) {
            for (CurrentSystemLevel l : CurrentSystemLevel.values()) {
                if ((l.getValue() & account.getCurrSysLevel()) == l.getValue()) {
                    String n = StringUtils.isBlank(account.getCurrSysLevelName()) ? l.getName()
                            : account.getCurrSysLevelName() + "，" + l.getName();
                    account.setCurrSysLevelName(n);
                }
            }
        }
        // 第三方的账号把汇率查询出来、今日入款查询出来、今日下发已匹配 未匹配查询出来
        if (AccountType.InThird.getTypeId().equals(account.getType())) {
            String tAmount = "0";
            String fAmount = "0";
            String sAmount = "0";
            if (redisService.getFloatRedisTemplate().opsForHash().get(RedisKeys.AMOUNT_SUM_BY_DAILY_THIRD_TOTAL_INCOME,
                    account.getId().toString()) != null)
                tAmount = redisService.getFloatRedisTemplate().opsForHash()
                        .get(RedisKeys.AMOUNT_SUM_BY_DAILY_THIRD_TOTAL_INCOME, account.getId().toString()).toString();
            if (redisService.getFloatRedisTemplate().opsForHash().get(RedisKeys.AMOUNT_SUM_BY_DAILY_THIRD_FEE_INCOME,
                    account.getId().toString()) != null)
                fAmount = redisService.getFloatRedisTemplate().opsForHash()
                        .get(RedisKeys.AMOUNT_SUM_BY_DAILY_THIRD_FEE_INCOME, account.getId().toString()).toString();
            if (redisService.getFloatRedisTemplate().opsForHash().get(RedisKeys.AMOUNT_SUM_BY_DAILY_THIRD_AMOUNT_INCOME,
                    account.getId().toString()) != null)
                sAmount = redisService.getFloatRedisTemplate().opsForHash()
                        .get(RedisKeys.AMOUNT_SUM_BY_DAILY_THIRD_AMOUNT_INCOME, account.getId().toString()).toString();
            BigDecimal totalAmount = new BigDecimal(tAmount);
            BigDecimal feeAmount = new BigDecimal(fAmount);
            BigDecimal amount = new BigDecimal(sAmount);
            account.setTotalAmount(totalAmount);
            account.setFeeAmount(feeAmount);
            account.setAmount(amount);
            String[] values = accountExtraRespository.findByid(account.getId());
            if (values.length > 0) {
                account.setRate(Float.valueOf(values[0].split(",")[1].equals("null") ? "0" : values[0].split(",")[1]));
                account.setRateValue(values[0].split(",", 4)[3].equals("null") ? "" : values[0].split(",", 4)[3]);
                account.setRateType(Integer.parseInt(values[0].split(",")[2]));
            }
        }
        if (account.getFlag() != null && (account.getFlag() == AccountFlag.REFUND.getTypeId()
                || account.getFlag() == AccountFlag.SPARETIME.getTypeId())) {
            Integer peakBalance = accountChangeService.peakBalance(getFromCacheById(account.getId()));
            account.setPeakBalance(Objects.nonNull(peakBalance) ? peakBalance : 0);
        }

        if (Objects.equals(account.getType(), AccountType.InBank.getTypeId())) {
            Integer flagMoreStr = null;
            if (account.getFlag() == null || account.getFlag() == 0) {
                flagMoreStr = 2; // "自购卡（大额专用）";
            } else if (Objects.equals(account.getSubType(), InBankSubType.IN_BANK_YSF.getSubType())) {
                flagMoreStr = 1; // "边入边出";
            } else {
                Object model = redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_NEW_MODEL)
                        .get(account.getId() + "");
                flagMoreStr = model == null || "1".equals(model.toString()) ? 5 : 4;// "先入后出（正在入）"
                // :
                // "先入后出（正在出）";
            }
            account.setFlagMoreStr(flagMoreStr);
        }
        String idStr = account.getId().toString();
        if (redisService.getStringRedisTemplate().boundHashOps(RedisKeys.REAL_BAL_LASTTIME).hasKey(idStr)) {
            String time = (String) redisService.getStringRedisTemplate().boundHashOps(RedisKeys.REAL_BAL_LASTTIME)
                    .get(idStr);
            if (StringUtils.isNotBlank(time)) {
                account.setBankBalTime(CommonUtils.millionSeconds2DateStr(Long.parseLong(time)));
            }

        }
        if (redisService.getStringRedisTemplate().boundHashOps(RedisKeys.SYS_BAL_LASTTIME).hasKey(idStr)) {
            String time = (String) redisService.getStringRedisTemplate().boundHashOps(RedisKeys.SYS_BAL_LASTTIME)
                    .get(idStr);
            if (StringUtils.isNotBlank(time)) {
                account.setSysBalTime(CommonUtils.millionSeconds2DateStr(Long.parseLong(time)));
            }
        }
        if (account.getType() != null && account.getType().equals(AccountType.OutBank.getTypeId())) {
            if (redisService.getStringRedisTemplate().boundHashOps(RedisKeys.INBANK_ONLINE).hasKey(idStr)) {
                if (availableCardCache.checkBankOnline(account.getId())) {
                    account.setIsOnLine(1);
                } else {
                    account.setIsOnLine(2);
                }
            } else if (redisService.getStringRedisTemplate().boundHashOps(RedisKeys.APP_ACCOUNT_PAUSED_SET_KEY)
                    .hasKey(idStr)) {
                account.setIsOnLine(2);
            } else {
                account.setIsOnLine(0);
            }
        } else {
            if (redisService.getStringRedisTemplate().boundHashOps(RedisKeys.INBANK_ONLINE).hasKey(idStr)) {
                if (availableCardCache.checkBankOnline(account.getId())) {
                    account.setIsOnLine(1);
                } else {
                    account.setIsOnLine(2);
                }
            } else if (redisService.getStringRedisTemplate().boundHashOps(RedisKeys.APP_ACCOUNT_PAUSED_SET_KEY)
                    .hasKey(idStr)) {
                account.setIsOnLine(2);
            } else {
                account.setIsOnLine(0);
            }
        }

        // mark 2019-07-11 6720 (6739)
        account.setAccountFeeConfig(accountFeeService.findByAccountBaseInfo(account));

        return account;
    }

    private Map<String, String> packLockerMap(String patternKey) {
        StringRedisTemplate stringTemplate = redisService.getStringRedisTemplate();
        Map<String, String> result = new HashMap<>();
        Set keys;
        if (StringUtils.isBlank(patternKey) || CollectionUtils.isEmpty(keys = stringTemplate.keys(patternKey))) {
            return result;
        }
        for (Object k : keys) {
            String[] v = k.toString().split(":");
            result.put(v[2], v[3]);
        }
        return result;
    }

    private AccountBaseInfo packAccountBaseInfo(BizAccount account) {
        if (account == null) {
            return null;
        }
        List<BizLevel> levelList = null;
        List<BizAccountLevel> accountLevelToList = accountLevelRepository.findByAccountId(account.getId());
        if (!CollectionUtils.isEmpty(accountLevelToList)) {
            List<Integer> levelIdToList = new ArrayList<>();
            accountLevelToList.forEach((p) -> levelIdToList.add(p.getLevelId()));
            SearchFilter[] filterToArray = {
                    new SearchFilter("id", SearchFilter.Operator.IN, levelIdToList.toArray()) };
            Specification<BizLevel> specification = DynamicSpecifications.build(BizLevel.class, filterToArray);
            levelList = levelRepository.findAll(specification);
        }
        return new AccountBaseInfo(account, levelList);
    }

    /**
     * 获取某些层级下的账号ID集
     *
     * @param levelIdList
     *            层级ID集
     * @return 账号ID集
     */
    private List<Integer> findAccountIdList(List<Integer> levelIdList) {
        if (CollectionUtils.isEmpty(levelIdList)) {
            return Collections.emptyList();
        }
        List<Integer> result = new ArrayList<>();
        levelIdList.forEach(p -> result.addAll(findAccountIdList(p)));
        return result;
    }

    @Override
    public List<BizAccount> statisticsInto(List<BizAccount> bizAccountList, SysUser sysUser, Integer incomeAccountId)
            throws Exception {
        SimpleDateFormat sdFormatter = new SimpleDateFormat("yyyy-MM-dd");
        Date nowTime = new Date(System.currentTimeMillis());
        String startTime, endTime;
        Calendar c = Calendar.getInstance();
        c.setTime(nowTime);
        if (c.get(Calendar.HOUR_OF_DAY) >= 7) {
            c.add(Calendar.DAY_OF_MONTH, 1);
            Date tomorrow = c.getTime();
            startTime = sdFormatter.format(nowTime);
            endTime = sdFormatter.format(tomorrow);
        } else {
            c.add(Calendar.DAY_OF_MONTH, -1);
            Date tomorrow = c.getTime();
            startTime = sdFormatter.format(tomorrow);
            endTime = sdFormatter.format(nowTime);
        }
        List<BizAccount> statisticsIntoList = new ArrayList<>();
        List<Object> statisticsIntoDate = incomeRequestRepository.statisticsInto(startTime + " 06:59:59",
                endTime + " 07:00:00");
        // List<String> locked = getTargetAccountLockedInRedis(null, null),
        List<String> lockedByUserIdAndFrmId = getTargetAccountLockedByCurrentUserInRedis(sysUser.getId(),
                incomeAccountId), lockedAccByUserId = getLockedAccByUserId(sysUser.getId());
        // 当前人锁定的出款卡
        List<String> outCardLockedByUserId = outCardIdsLockedByUserId(sysUser.getId());
        if (!CollectionUtils.isEmpty(outCardLockedByUserId)) {
            if (CollectionUtils.isEmpty(lockedAccByUserId)) {
                lockedAccByUserId = Lists.newArrayList();
            }
            lockedAccByUserId.addAll(outCardLockedByUserId);
        }

        // 下发任务里 当前人锁定的 所有卡
        List<Integer> outCardLockedInDrawTaskByUser = outCardsOrOtherCardsLockedByUserInDrawTask(sysUser.getId());
        if (!CollectionUtils.isEmpty(outCardLockedInDrawTaskByUser)) {
            if (CollectionUtils.isEmpty(lockedAccByUserId)) {
                lockedAccByUserId = Lists.newArrayList();
            }
            lockedAccByUserId.addAll(Lists.transform(outCardLockedInDrawTaskByUser, Functions.toStringFunction()));
        }
        List<Integer> issueToList = null != incomeAccountId ? accountBindingService.findBindAccountId(incomeAccountId)
                : null;
        for (int i = 0; i < bizAccountList.size(); i++) {
            BizAccount account = bizAccountList.get(i);

            if (!CollectionUtils.isEmpty(lockedAccByUserId)) {
                if (lockedAccByUserId.contains(String.valueOf(account.getId()))) {
                    if (!CollectionUtils.isEmpty(lockedByUserIdAndFrmId)) {
                        if (lockedByUserIdAndFrmId.contains(String.valueOf(account.getId()))) {
                            account.setLockByOperator(999999 + sysUser.getId() + incomeAccountId);
                        } else {
                            if (!CollectionUtils.isEmpty(issueToList)) {
                                if (issueToList.contains(account.getId())) {
                                    account.setLockByOperator(999999 + sysUser.getId());
                                }
                            }
                        }
                    } else {
                        if (!CollectionUtils.isEmpty(issueToList)) {
                            if (issueToList.contains(account.getId())) {
                                account.setLockByOperator(999999 + sysUser.getId());
                            }
                        }
                    }
                }
            }

            for (int j = 0; j < statisticsIntoDate.size(); j++) {
                Object[] obj = (Object[]) statisticsIntoDate.get(j);
                if (null != obj[0] && account.getId().equals(Integer.valueOf(obj[0].toString()))) {
                    AccountStatInOut inOut = new AccountStatInOut();
                    inOut.setMapping(new BigDecimal(obj[1].toString()));
                    inOut.setMapped(new BigDecimal(obj[2].toString()));
                    inOut.setCancel(new BigDecimal(obj[3].toString()));
                    account.setInCount(inOut);
                    break;
                }
            }
            statisticsIntoList.add(account);
        }
        return statisticsIntoList;
    }

    @Transactional
    @Override
    public void deleteAndClear(Integer accountId) {
        AccountBaseInfo base = getFromCacheById(accountId);
        if (Objects.isNull(base)) {
            return;
        }
        accountRepository.deleteAndClear_1(accountId);
        accountRepository.flush();
        accountRepository.deleteAndClear_2(accountId);
        accountRepository.flush();
        accountRepository.deleteAndClear_3(accountId);
        accountRepository.flush();
        accountRepository.deleteAndClear_4(accountId);
        accountRepository.flush();
        accountRepository.deleteAndClear_5(accountId);
        accountRepository.flush();
        accountRepository.deleteAndClear_6(accountId);
        accountRepository.flush();
        accountRepository.deleteAndClear_7(accountId);
        accountRepository.flush();
        accountRepository.deleteAndClear_8(accountId);
        accountRepository.flush();
        if (Objects.equals(AccountFlag.REFUND.getTypeId(), base.getFlag())) {
            BizAccountMore more = accountMoreService.getFromCacheByMobile(base.getMobile());
            if (Objects.nonNull(more)) {
                for (String id : StringUtils.trimToEmpty(more.getAccounts()).split(",")) {
                    if (StringUtils.isBlank(id) || !StringUtils.isNumeric(id)) {
                        continue;
                    }
                    accountRepository.deleteAndClear_9(Integer.valueOf(id));
                    accountRepository.deleteAndClear_12(Integer.valueOf(id));
                }
                accountRepository.deleteAndClear_10(more.getUid());
                // accountRepository.deleteAndClear_11(more.getMoible());
            }
        }
    }

    @Override
    public Map<String, Object> findIssuedThird(int fromId, String startTime, String endTime, PageRequest pageRequest)
            throws Exception {
        Page<Object> dataToPage = accountRepository.findIssuedThird(fromId, startTime, endTime, pageRequest);
        Map<String, Object> map = new HashMap<>();
        map.put("Page", dataToPage);
        return map;
    }

    @Override
    public Map<String, Object> findEncashThird(int fromId, String startTime, String endTime, PageRequest pageRequest)
            throws Exception {
        Page<Object> dataToPage = accountRepository.findEncashThird(fromId, startTime, endTime, pageRequest);
        Map<String, Object> map = new HashMap<>();
        map.put("Page", dataToPage);
        return map;
    }

    @Override
    public Map<String, Object> findMembersThird(int fromId, String startTime, String endTime, String handicaoCode,
                                                PageRequest pageRequest) throws Exception {
        Page<Object> dataToPage = accountRepository.findMembersThird(fromId, startTime, endTime, handicaoCode,
                pageRequest);
        Map<String, Object> map = new HashMap<>();
        map.put("Page", dataToPage);
        return map;
    }

    @Override
    public Map<String, Object> findOutBankLog(String startTime, String endTime, String bankType,
                                              List<Integer> handicaps, PageRequest pageRequest) throws Exception {
        Page<Object> dataToPage = accountRepository.findOutBankLog(startTime, endTime, bankType, handicaps,
                pageRequest);
        Map<String, Object> map = new HashMap<>();
        map.put("Page", dataToPage);
        return map;
    }

    @Override
    public Map<String, Object> find7TimeBalance(String startTime, List<Integer> handicaps, PageRequest pageRequest)
            throws Exception {
        Page<Object> dataToPage = accountRepository.find7TimeBalance(startTime, handicaps, pageRequest);
        Map<String, Object> map = new HashMap<>();
        map.put("Page", dataToPage);
        return map;
    }

    @Transactional
    @Override
    public void updateOuterLimit(Integer hadicapId, Integer flag, Integer outerLimit, Integer middleLimit,
                                 Integer innerLimit, Integer specifyLimit, Integer type, Integer status, String bankType) throws Exception {
        String f = Objects.isNull(flag) ? StringUtils.EMPTY : flag.toString();
        if (StringUtils.isBlank(bankType)) {
            bankType = null;
        }
        if (Objects.isNull(hadicapId)) {
            accountRepository.updateOuterLimit(outerLimit, middleLimit, innerLimit, specifyLimit, type, status, f,
                    bankType);
        } else {
            accountRepository.updateOuterLimit(outerLimit, middleLimit, innerLimit, specifyLimit, type, status,
                    hadicapId, f, bankType);
        }
    }

    @Transactional
    @Override
    public void updateGPS(Integer accountId, String gps) {
        // 存储日志
        SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
        BizAccountExtra beVO = new BizAccountExtra();
        beVO.setAccountId(accountId);
        if (null != operator) {
            beVO.setOperator(operator.getUid());
        }
        beVO.setTime(new Date());
        BizAccount accountVo = accountRepository.getOne(accountId);
        String remark = "账号所在IP移动;原IP：";
        if (null != accountVo) {
            remark += (null != accountVo.getGps() ? accountVo.getGps() : "空");
        }
        remark += "&nbsp;&nbsp;新IP：" + (null != gps ? gps : "空");
        beVO.setRemark(remark);
        accountExtraRespository.save(beVO);
        // 执行更新
        accountRepository.updateGPS(accountId, gps);
    }

    @Transactional
    @Override
    public Map<String, List<Integer>> batchUpdateLimit(JSONArray data) {
        Map<String, List<Integer>> result = new HashMap<>();
        if (data != null) {
            List<Integer> fails = new ArrayList<>();
            List<Integer> success = new ArrayList<>();
            try {
                for (int i = 0; i < data.length(); i++) {
                    JSONObject obj = data.getJSONObject(i);
                    Integer accId = Integer.parseInt(obj.getString("bankId"));
                    Integer outEnable = Integer.parseInt(obj.getString("outEnable"));
                    if (2 == outEnable) {
                        int counts = quickPayService.getBindAccountIdNum(accId);
                        if (counts > 0) {
                            accountRepository.batchUpdateToQuickPay(accId);
                            success.add(accId);
                            AccountBaseInfo base = getFromCacheById(accId);
                            BizAccount acc = getById(accId);
                            if (!Objects.equals(base.getSubType(), InBankSubType.IN_BANK_YSF.getSubType())) {
                                boolean res = modifyInBankStatus(acc, acc.getStatus(), null, true);
                                if (res) {
                                    acc.setPassageId(null);
                                    acc = accountRepository.saveAndFlush(acc);
                                }
                            }
                            log.debug("batchUpdateLimit>> id {}", acc.getId());
                            broadCast(acc);
                            redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_NEW_MODEL)
                                    .delete(acc.getId().toString());
                            cabanaService.refreshAcc(accId);
                        } else {
                            fails.add(accId);
                        }
                    } else {
                        String limitPercentage = obj.getString("limitPercentage");
                        String minBalance = obj.getString("minBalance");
                        success.add(accId);
                        AccountBaseInfo base = getFromCacheById(accId);
                        Integer subType = base.getSubType() == null || base.getSubType() == 3 ? 0 : base.getSubType();
                        accountRepository.batchUpdateLimit(outEnable, accId, limitPercentage, minBalance, subType);
                        String value = "";
                        if (Objects.nonNull(base) && Objects.equals(AccountFlag.REFUND.getTypeId(), base.getFlag())) {
                            value = String.format(Constants.QUEUE_FORMAT_USER_OR_ROBOT, TARGET_TYPE_MOBILE, accId);
                        } else {
                            value = String.format(Constants.QUEUE_FORMAT_USER_OR_ROBOT, TARGET_TYPE_ROBOT, accId);
                        }
                        redisService.getStringRedisTemplate().boundZSetOps(RedisKeys.ALLOCATING_NEW_OUTWARD_TARGET)
                                .remove(value);
                        BizAccount acc = getById(accId);
                        if (Objects.equals(base.getSubType(), InBankSubType.IN_BANK_YSF.getSubType())) {
                            boolean res = modifyInBankStatus(acc, acc.getStatus(), null, true);
                            if (res) {
                                acc.setPassageId(null);
                                acc = accountRepository.saveAndFlush(acc);
                            }
                        }
                        log.debug("batchUpdateLimit>> id {}", acc.getId());
                        broadCast(acc);
                        redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_NEW_MODEL)
                                .delete(base.getId().toString());
                    }
                }
            } catch (Exception e) {
                log.info("batchUpdateLimit>> 批量更新入款卡额度失败 ", e);
            } finally {
                result.put("success", success);
                result.put("failed", fails);
                return result;
            }
        }
        return result;
    }

    /**
     * 模式设置
     *
     * @param accId
     *            account's identity
     * @param trans
     *            1:MOBILE;2:PC
     * @param crawl
     *            1:MOBILE;2:PC
     */
    @Override
    public void setModel(Integer accId, Integer trans, Integer crawl) throws Exception {
        if (trans != 1 && trans != 2 || crawl != 1 && crawl != 2) {
            throw new Exception("数值错误,[1,2]");
        }
        AccountBaseInfo base = getFromCacheById(accId);
        if (Objects.isNull(base)) {
            throw new Exception("账号不存在");
        }
        if (!base.checkMobile() && (trans == 1 || crawl == 1)) {
            throw new Exception("手机银行暂未开通,故抓流水，转账不能设置为手机模式");
        }
        String model = String.format("%d%d", trans, crawl);
        redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_MODEL).put(accId.toString(), model);
        cabanaService.setModel(accId, model);
        hostMonitorService.changeMode(accId, getModel4PC(accId));
    }

    @Override
    public String getModel(Integer accId) {
        return (String) redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_MODEL)
                .get(String.valueOf(accId));
    }

    @Override
    public Map<Object, Object> getModel() {
        return redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_MODEL).entries();
    }

    @Override
    public ActionEventEnum getModel4PC(int accId) {
        String model = StringUtils.trimToNull(getModel(accId));
        if (Objects.isNull(model))
            return ActionEventEnum.NORMALMODE;
        if (model.length() != 2 || !model.contains("2"))
            return ActionEventEnum.CAPTUREMODE;
        boolean first = model.startsWith("2"), second = model.endsWith("2");
        if (first && second)
            return ActionEventEnum.NORMALMODE;
        else if (first)
            return ActionEventEnum.TRANSMODE;
        return ActionEventEnum.CAPTUREMODE;
    }

    @Override
    public List<Object> findTransfers(String startTime, String endTime) {
        return accountRepository.findTransfers(startTime, endTime);
    }

    @Override
    public Page<Object> findIncomeAccountOrderByBankLog(Integer[] handicapList, String account, String alias,
                                                        String bankType, String owner, Integer[] search_IN_flag, Integer[] accountStatusList,
                                                        PageRequest pageRequest) throws Exception {
        // 传字符串时，会查不出toAccount空的结果集
        if (StringUtils.isEmpty(account)) {
            account = null;
        }
        if (StringUtils.isEmpty(alias)) {
            alias = null;
        }
        if (StringUtils.isEmpty(bankType)) {
            bankType = null;
        }
        if (StringUtils.isEmpty(owner)) {
            owner = null;
        }
        return accountRepository.findIncomeAccountOrderByBankLog(handicapList, account, alias, bankType, owner,
                search_IN_flag, accountStatusList, pageRequest);
    }

    @Override
    public List<BizAccount> findByAlias(String alias) {
        return accountRepository.findByAlias(alias);
    }

    @Override
    public List<BizAccount> findByAliasAndTypeInForDrawTask(String alias) {
        List<Integer> types = Lists.newLinkedList();
        types.add(AccountType.OutBank.getTypeId());
        types.add(AccountType.BindCommon.getTypeId());
        return accountRepository.findByAliasAndTypeIn(alias, types);
    }

    @Override
    public List<BizAccount> findByAccount(String account) {
        return accountRepository.findByAccount(account);
    }

    @Override
    public List<Integer> getAccountList(List<Integer> type, List<Integer> handicaps) {
        return accountRepository.findAccountList(type, handicaps);
    }

    // 新支付的根据设备号返回盘口编码和设备号的map
    @Override
    public Map<Integer, String[]> getDeviceNoAndHandicapCodeMap(String[] deviceCol) {
        if (deviceCol == null || deviceCol.length == 0) {
            return null;
        }
        String sql = "'select h.code,a.bank_name from biz_account a join biz_handicap  h on a.handicap_id=h.id and '";
        if (deviceCol.length == 1) {
            sql += " a.bank_name =\"" + deviceCol[0] + "\"";
        } else {
            sql += "a.bank_name in (";
            for (int i = 0, len = deviceCol.length; i < len; i++) {
                if (i < len - 1) {
                    sql += "\"" + deviceCol[i] + "\",";
                } else {
                    sql += "\"" + deviceCol[i] + "\")";
                }
            }
        }
        List<Object[]> list = entityManager.createNativeQuery(sql).getResultList();
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        Map<Integer, String[]> map = new HashMap<>();
        for (int i = 0, size = list.size(); i < size; i++) {
            Object[] obj = list.get(i);
            String[] deviceNoArray;
            Integer key = Integer.valueOf(obj[0].toString());// handicapCode
            String deviceNo = obj[1].toString();// deviceNo--bank_name
            if (!CollectionUtils.isEmpty(map)) {
                if (map.keySet().contains(key)) {
                    deviceNoArray = map.get(key);
                    int len = deviceNoArray.length;
                    if (!Arrays.asList(deviceNoArray).contains(deviceNo)) {
                        String[] deviceNoArray2 = Arrays.copyOf(deviceNoArray, len + 1);
                        // System.arraycopy(deviceNoArray, 0, deviceNoArray2, 0,
                        // len);
                        deviceNoArray2[len] = deviceNo;
                        map.put(key, deviceNoArray2);
                    }
                } else {
                    deviceNoArray = new String[] { deviceNo };
                    map.put(key, deviceNoArray);
                }
            } else {
                deviceNoArray = new String[] { deviceNo };
                map.put(key, deviceNoArray);
            }
        }
        return map;
    }

    @Override
    public Map<String, Object> findDeleteAccount(int handicap, String alias, String type, String flag, String status,
                                                 PageRequest pageRequest) throws Exception {
        Page<Object> dataToPage;
        java.lang.Object[] total;
        Map<String, Object> map = new HashMap<String, Object>();
        dataToPage = accountRepository.findDeleteAccount(handicap, alias, type, flag, status, pageRequest);
        // 查询总计进行返回
        total = accountRepository.totalFindDeleteAccount(handicap, alias, type, flag, status);
        map.put("Page", dataToPage);
        map.put("Total", total);
        return map;
    }

    @Transactional
    @Override
    public void updateAcountById(List<String> accountIds, int status) {
        accountRepository.updateAcountById(accountIds, status);
    }

    @Transactional
    @Override
    public void toStopTemp(int accountId, String remark, int uid) throws Exception {
        BizAccount account = getById(accountId);
        BizAccount oldAccount = new BizAccount();
        SysUser operator = userService.findFromCacheById(uid);
        BeanUtils.copyProperties(oldAccount, account);
        Date date = new Date();
        // 如果是从冻结转停用的，则加上备注
        if (account.getStatus().equals(AccountStatus.Freeze.getStatus())) {
            account.setRemark(CommonUtils.genRemark(account.getRemark(), "【" + AccountStatus.Freeze.getMsg() + "转"
                    + AccountStatus.StopTemp.getMsg() + "】" + (remark != null ? remark : ""), date, operator.getUid()));
        }
        account.setStatus(AccountStatus.StopTemp.getStatus());
        if (oldAccount.getType().equals(AccountType.InBank.getTypeId())) {
            // 入款卡不能有持卡人
            account.setHolder(null);
        } else {
            account.setHolder(operator.getId());
        }
        account.setUpdateTime(date);
        updateBaseInfo(account);
        accountExtraService.saveAccountExtraLog(oldAccount, account, operator.getUid());
        log.debug("toStopTemp>> id {}", account.getId());
        broadCast(account);
        hostMonitorService.update(account);
        cabanaService.updAcc(account.getId());
    }

    @Override
    public BizAccount setAccountAlias(BizAccount o) {
        // 是银行账号 且查出的数据无编号时，自动生成编号
        if (StringUtils.isEmpty(o.getAlias()) && Arrays
                .asList(new Integer[] { AccountType.InThird.getTypeId(), AccountType.InAli.getTypeId(),
                        AccountType.InWechat.getTypeId(), AccountType.OutThird.getTypeId() })
                .contains(o.getType()) == false) {
            // 编号六位数，跳过为4的数字 从100000开始递增
            String maxAlias = getMaxAlias();
            if (StringUtils.isEmpty(maxAlias) || maxAlias.equals("0")) {
                o.setAlias("100000");
            } else {
                int alias = Integer.parseInt(maxAlias) + 1;
                o.setAlias(Integer.toString(alias).replace("4", "5"));
            }
        }
        return o;
    }

    @Override
    public Page<BizAccount> findToBeRaisePage(SysUser operator, List<Integer> type, List<Integer> status,
                                              List<Integer> handicapId, List<Integer> currSysLevel, String bankType, String account, String owner,
                                              String alias, Pageable pageable) {
        Page<BizAccount> accountToPage = accountRepository.findToBeRaisePage(type, status, handicapId, currSysLevel,
                bankType, account, owner, alias, pageable);
        List<BizAccount> accountToList = accountToPage.getContent();
        packAllForForAccount(operator, accountToList);
        return accountToPage;
    }

    @Override
    public BigDecimal getTotalBankBalance(SysUser operator, List<Integer> type, List<Integer> status,
                                          List<Integer> handicapId, List<Integer> currSysLevel, String bankType, String account, String owner,
                                          String alias) {
        return accountRepository.getTotalBankBalance(type, status, handicapId, currSysLevel, bankType, account, owner,
                alias);
    }

    @Override
    public BigDecimal getTotalAmountsByAc(List<String> accountsList) {
        return accountRepository.getTotalAmountsByAc(accountsList);
    }

    @Transactional
    @Override
    public void updateFlagById(Integer id, Integer flag) {
        accountRepository.updateFlagById(id, flag);
    }

    @Override
    public void reportVersion(Integer accId, String curVer, String latestVer) {
        if (Objects.isNull(accId) || StringUtils.isEmpty(curVer) || StringUtils.isEmpty(latestVer)) {
            return;
        }
        AccountBaseInfo base = getFromCacheById(accId);
        if (Objects.isNull(base)) {
            return;
        }
        if (StringUtils.isNumeric(curVer) || StringUtils.isNumeric(latestVer)) {
            int cur = Integer.parseInt(curVer);
            int latest = Integer.parseInt(latestVer);
            int max = CommonUtils.getMaxVersionDiff4Mobile();
            if (latest - cur > max) {
                log.info("reportVersion >>> id: {},curVer.{},latestVer.{} need to upgrade the app", accId, cur, latest);
                redisService.getStringRedisTemplate().boundSetOps(RedisKeys.APP_NEED_UPGRADE).add(accId.toString());
                return;
            } else {
                if (redisService.getStringRedisTemplate().boundSetOps(RedisKeys.APP_NEED_UPGRADE).members()
                        .contains(accId.toString())) {
                    redisService.getStringRedisTemplate().boundSetOps(RedisKeys.APP_NEED_UPGRADE)
                            .remove(accId.toString());
                }
            }
        }
    }

    @Override
    public List<Account> loginByUserPass(String username, String password) {
        String checked = rebateUserService.checkUserAndPass(username, password);
        if (!Objects.equals(LoginMsg.Success.getMsg(), checked)) {
            return getErrorMessage(username, checked);
        }
        return getCurrAndList(username);
    }

    @Override
    public List<Account> getCurrAndList(String username) {
        BizRebateUser user = rebateUserService.getFromCacheByUserName(username);
        if (user == null) {
            log.info("getCurrAndList>>doesn't has user,username {}", username);
            return getErrorMessage(username, LoginMsg.UserNotExtOrStatusErr.getMsg());
        }
        BizAccountMore more = accountMoreService.getFromByUid(user.getUid());
        if (more == null || StringUtils.isBlank(more.getMoible()) || StringUtils.isBlank(more.getAccounts())) {
            log.info("getCurrAndList>>account more doesn't has data,username {}", username);
            return getErrorMessage(username, LoginMsg.UserHasNoCard.getMsg());
        }
        Object keys4all = redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_CHG).get(more.getMoible());
        String accounts = more.getAccounts();
        if (Objects.isNull(keys4all)) {
            log.info("getCurrAndList>>doesn't has current account,deal with acc,username {}", username);
            return dealWithAcc(accounts, null);
        } else {
            log.info("getCurrAndList>>has current account,deal with acc,username {}", username);
            ChgAcc acc = new ChgAcc(keys4all.toString());
            if (acc == null) {
                return dealWithAcc(accounts, null);
            }
            int currId = acc.getCurrAcc();
            BizAccount bizAccount = getById(currId);
            if (bizAccount != null) {
                return dealWithAcc(accounts, bizAccount);
            } else {
                return dealWithAcc(accounts, null);
            }
        }
    }

    private List<Account> getErrorMessage(String userName, String msg) {
        log.info("getErrorMessage>> user name or accounts {} ,msg {}", userName, msg);
        ArrayList<Account> res = new ArrayList<>();
        Account acc = new Account();
        acc.setOwner(msg);
        res.add(acc);
        return res;
    }

    private List<Account> dealWithAcc(String accounts, BizAccount currAcc) {
        if (StringUtils.isEmpty(accounts)) {
            log.info("dealWithAcc>> accounts is empty!");
            return getErrorMessage(accounts, LoginMsg.UserHasNoCard.getMsg());
        }
        List<Account> accList = new ArrayList<>();
        if (currAcc != null) {
            AccountBaseInfo base = getFromCacheById(currAcc.getId());
            if (base != null && (Objects.equals(base.getStatus(), AccountStatus.Inactivated.getStatus())
                    || Objects.equals(base.getStatus(), AccountStatus.Normal.getStatus())
                    || Objects.equals(base.getStatus(), AccountStatus.Enabled.getStatus()))) {
                log.info("dealWithAcc>> current account is not null,add to list !accid {}", currAcc.getId());
                Account acc = new Account(currAcc);
                acc.setHolder(1);
                accList.add(acc);
            } else {
                redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_CHG).delete(currAcc.getMobile());
                currAcc = null;
            }
        }
        String[] accs = accounts.split(",");
        for (String id : accs) {
            if (StringUtils.isNotEmpty(id)) {
                if (currAcc != null && currAcc.getId().toString().equals(id)) {
                    continue;
                }
                BizAccount bizAcc = getById(Integer.parseInt(id));
                if (bizAcc != null) {
                    if (Objects.equals(bizAcc.getStatus(), AccountStatus.Delete.getStatus())) {
                        continue;
                    }
                    Account acc = new Account(bizAcc);
                    if (Objects.equals(acc.getStatus(), AccountStatus.Inactivated.getStatus())
                            || Objects.equals(acc.getStatus(), AccountStatus.Normal.getStatus())
                            || Objects.equals(acc.getStatus(), AccountStatus.Enabled.getStatus())) {
                        acc.setHolder(2);
                    } else {
                        acc.setHolder(3);
                    }
                    accList.add(acc);
                }
            }
        }
        /**
         * 当前卡不为未激活、在用、可用卡时，将用户下的按在用、可用、未激活、其他状态进行排序，第一张卡作为当前在用卡
         */
        accList.sort((o1, o2) -> {
            if (o1.getHolder() != o2.getHolder()) {
                return o1.getHolder() - o2.getHolder();
            } else {
                return o1.getStatus() - o2.getStatus();
            }
        });

        // 如果当前在用账户为空，把结果集中的第一个账户拉起来
        if (currAcc == null && !CollectionUtils.isEmpty(accList) && accList.get(0) != null) {
            accountChangeService.firstUseToLogin(accList.get(0).getMobile(), accList.get(0).getId(),
                    System.currentTimeMillis());
        }
        log.info("dealWithAcc>>account size {}", accList.size());
        return accList;
    }

    @Override
    public List<BizAccount> findOutAccList4Manual() {
        return accountRepository.findOutAccList4Manual();
    }

    /**
     * 入款卡已绑定通道时，变更状态时需调接口同步到平台
     *
     * @param account
     * @param status
     * @param oldAccount
     */
    public boolean modifyInBankStatus(BizAccount account, Integer status, String oldAccount, boolean modifySubType) {
        if (account == null || !Objects.equals(account.getType(), AccountType.InBank.getTypeId())
                || (status == null && oldAccount == null) || account.getPassageId() == null
                || (oldAccount != null && !Objects.equals(status, AccountStatus.Normal.getStatus())
                && !Objects.equals(status, AccountStatus.Freeze.getStatus())
                && !Objects.equals(status, AccountStatus.StopTemp.getStatus())
                && !Objects.equals(status, AccountStatus.Delete.getStatus()))) {
            return true;
        }
        // 0.停用 1.删除 2.修改银行卡号 4.冻结 5.启用 6.修改类型
        BankModifiedInputDTO bid = new BankModifiedInputDTO();
        byte statusB = modifySubType ? (byte) 6
                : oldAccount != null ? (byte) 2
                : Objects.equals(status, AccountStatus.Normal.getStatus()) ? (byte) 5
                : Objects.equals(status, AccountStatus.Freeze.getStatus()) ? (byte) 4
                : Objects.equals(status, AccountStatus.StopTemp.getStatus()) ? (byte) 0
                : (byte) 1;
        Integer handcode = Integer.parseInt(handicapService.findFromCacheById(account.getHandicapId()).getCode());
        bid.setOid(handcode);
        bid.setPocId(account.getPassageId());
        bid.setCardNo(account.getAccount());
        bid.setStatus(statusB);
        if (oldAccount != null) {
            bid.setCardNo(oldAccount);
            bid.setCardNo2(account.getAccount());
        }
        ResponseDataNewPay response = inAccountService.bankModified(bid);
        return Objects.equals(200, response.getCode());
    }

    @Override
    public Map<String, Object> showRebateUser(String handicapId, String bankType, Integer[] typeToArray, String alias,
                                              String account, String owner, String currSysLevel, Integer[] statusToArray, String rebateUser,
                                              BigDecimal startAmount, BigDecimal endAmount, String subType, PageRequest pageRequest) throws Exception {
        Page<Object> dataToPage = accountRepository.showRebateUser(handicapId, bankType, typeToArray, alias, account,
                owner, currSysLevel, statusToArray, rebateUser, startAmount, endAmount, subType, pageRequest);
        Object[] obj = accountRepository.getBankBalanceTotal(handicapId, bankType, typeToArray, alias, account, owner,
                currSysLevel, statusToArray, rebateUser, startAmount, endAmount, subType);
        Map<String, Object> map = new HashMap<>();
        map.put("Page", dataToPage);
        map.put("Obj", obj);
        return map;
    }

    @Override
    public List<Account> getRebateAccListById(Integer accId) {
        log.debug("getRebateAccListById>> accId {}", accId);
        AccountBaseInfo base = getFromCacheById(accId);
        if (base == null || !base.checkMobile() || StringUtils.isBlank(base.getMobile())) {
            return null;
        }
        BizAccountMore accountMore = accountMoreService.getFromCacheByMobile(base.getMobile());
        if (accountMore == null) {
            return null;
        }
        BizRebateUser user = rebateUserService.getFromCacheByUid(accountMore.getUid());
        if (user == null || StringUtils.isBlank(user.getUserName())) {
            return null;
        }
        return getCurrAndList(user.getUserName());
    }

    @Override
    public void disableQuickPay(String account) {
        redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACCOUNT_DISABLE_QUICKPAY).put(account, "");
    }

    @Override
    public void enableQuickPay(String account) {
        redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACCOUNT_DISABLE_QUICKPAY).delete(account);
    }

    /**
     * 获取mobile端暂停的账号id
     *
     * @return
     */
    @Override
    public List<Integer> pausedMobileAccountIds(Integer type) {
        try {
            boolean exists = redisService.getStringRedisTemplate().hasKey(RedisKeys.APP_ACCOUNT_PAUSED_SET_KEY);
            if (!exists) {
                log.debug("redis不存在暂停 mobile:account:paused");
                return getAddtionalStop(type);
            }
            Set<Object> ids = redisService.getStringRedisTemplate().boundHashOps(RedisKeys.APP_ACCOUNT_PAUSED_SET_KEY)
                    .keys();
            List<Integer> list = ids.stream().map(p -> Integer.valueOf(p.toString())).collect(Collectors.toList());
            list.addAll(getAddtionalStop(type));
            return list;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Lists.newArrayList();
    }

    private List<Integer> getAddtionalStop(Integer type) {
        log.debug("获取availableCardCache 里的 账号 type:{}", type);
        Set<Object> idsOnline = redisService.getStringRedisTemplate().boundHashOps(RedisKeys.INBANK_ONLINE).keys();
        Integer[] commonType = new Integer[] { AccountType.BindCommon.getTypeId(), AccountType.ThirdCommon.getTypeId(),
                AccountType.BindAli.getTypeId(), AccountType.BindWechat.getTypeId() };
        List<Integer> list2 = idsOnline.stream().map(p -> Integer.valueOf(p.toString())).filter(p -> {
            boolean inCardCache = availableCardCache.checkBankOnline(p);
            log.debug("账号 id:{} ,是否在缓存中:{}", p, inCardCache);
            boolean typeQuery = type.equals(AccountType.OutBank.getTypeId())
                    || type.equals(AccountType.InBank.getTypeId()) || type.equals(AccountType.ReserveBank.getTypeId())
                    || Arrays.asList(commonType).contains(type);
            if (typeQuery) {
                if (!inCardCache) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        }).collect(Collectors.toList());
        return list2;
    }

    /**
     * 事件 88 22 调用fund保存或者移除账号状态缓存
     *
     * @param accountId
     *            需要保持或者移除的账号id
     * @param saveCode
     *            事件 88 表示保存 事件 22 移除
     */
    @Override
    public void savePauseOrResumeAccountId(Integer accountId, Integer saveCode) {
        if (ObjectUtils.isEmpty(accountId) || ObjectUtils.isEmpty(saveCode)) {
            return;
        }
        try {
            HashOperations operations = redisService.getStringRedisTemplate().opsForHash();
            if (ObjectUtils.nullSafeEquals(saveCode, Integer.valueOf(LAST_STATUS_88))) {
                savePaused(operations, accountId);
            } else if (ObjectUtils.nullSafeEquals(saveCode, Integer.valueOf(LAST_STATUS_22))) {
                if (operations.hasKey(RedisKeys.APP_ACCOUNT_PAUSED_SET_KEY, accountId.toString())) {
                    operations.delete(RedisKeys.APP_ACCOUNT_PAUSED_SET_KEY, accountId.toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 手机端 上报状态处理 22 88 400 405 999表示断网离线
     *
     * @param accountId
     * @param saveCode
     *            22 88 400 405
     */
    @Override
    public void savePauseOrResumeOrOnlineForMobile(Integer accountId, Integer saveCode) {
        if (ObjectUtils.isEmpty(accountId) || ObjectUtils.isEmpty(saveCode)) {
            return;
        }

        try {
            AccountBaseInfo info = getFromCacheById(accountId);
            log.debug("手机 暂停或者暂停恢复 云山付启用或者 禁用 账号信息 :{}", info);
            HashOperations operations = redisService.getStringRedisTemplate().opsForHash();
            if (saveCode.equals(DISCONNET_MOBILE_STATUS)) {
                // 删除暂停
                if (operations.hasKey(RedisKeys.APP_ACCOUNT_PAUSED_SET_KEY, accountId.toString())) {
                    operations.delete(RedisKeys.APP_ACCOUNT_PAUSED_SET_KEY, accountId.toString());
                }
                // 删除在线
                saveOnlineAccontIds(accountId, false);
                // 删除上次保存的值
                if (operations.hasKey(RedisKeys.APP_ACCOUNT_LAST_STATUS_KEY, accountId.toString())) {
                    operations.delete(RedisKeys.APP_ACCOUNT_LAST_STATUS_KEY, accountId.toString());
                }
                return;
            }
            boolean flag = onlineEnableForMobile(accountId, String.valueOf(saveCode));
            if (ObjectUtils.nullSafeEquals(saveCode, Integer.valueOf(LAST_STATUS_88))
                    || ObjectUtils.nullSafeEquals(saveCode, Integer.valueOf(LAST_STATUS_400))) {
                // 暂停 或者 禁用
                savePaused(operations, accountId);
                // 保存在线
                saveOnlineAccontIds(accountId, false);
            } else if (ObjectUtils.nullSafeEquals(saveCode, Integer.valueOf(LAST_STATUS_22))
                    || ObjectUtils.nullSafeEquals(saveCode, Integer.valueOf(LAST_STATUS_405))) {
                // 如果是手机端 先检测上一次存储的指令 是否可以恢复暂停 转在线
                if (flag) {
                    if (operations.hasKey(RedisKeys.APP_ACCOUNT_PAUSED_SET_KEY, accountId.toString())) {
                        operations.delete(RedisKeys.APP_ACCOUNT_PAUSED_SET_KEY, accountId.toString());
                    }
                    // 保存在线
                    saveOnlineAccontIds(accountId, true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("处理异常:", e);
        }
    }

    private void savePaused(HashOperations operations, Integer accountId) {
        if (!operations.hasKey(RedisKeys.APP_ACCOUNT_PAUSED_SET_KEY, accountId.toString())) {
            Object time = redisService.getStringRedisTemplate().boundHashOps(RedisKeys.REAL_BAL_LASTTIME)
                    .get(StringUtils.trim(accountId.toString()));
            String time2 = System.currentTimeMillis() + "";
            if (time != null) {
                time2 = (String) time;
            }
            operations.putIfAbsent(RedisKeys.APP_ACCOUNT_PAUSED_SET_KEY, accountId.toString(), StringUtils.trim(time2));
        }
    }

    private static final String LAST_STATUS_22 = "22";
    private static final String LAST_STATUS_88 = "88";
    private static final String LAST_STATUS_400 = "400";
    private static final String LAST_STATUS_405 = "405";

    /**
     * 判断是否 可以把 手机端的账号转在线 true 可以 false不可以
     *
     * @param accountId
     * @param code
     *            需要保持的 命令 22 88 400 405
     * @return
     */
    @Override
    public boolean onlineEnableForMobile(Integer accountId, String code) {
        HashOperations operations = redisService.getStringRedisTemplate().opsForHash();
        boolean flag = false;
        // 先检测上一次存储的指令
        boolean exitsTool = operations.hasKey(RedisKeys.APP_ACCOUNT_LAST_STATUS_KEY, accountId.toString());
        boolean exitsYsf = operations.hasKey(RedisKeys.YSF_ACCOUNT_LAST_STATUS_KEY, accountId.toString());

        if (!exitsTool && !exitsYsf) {
            log.debug("第一次  本次传来的值:{} ", code);
            flag = true;
            if (LAST_STATUS_22.equals(code) || LAST_STATUS_88.equals(code)) {
                operations.put(RedisKeys.APP_ACCOUNT_LAST_STATUS_KEY, accountId.toString(), code);
            } else if (LAST_STATUS_400.equals(code) || LAST_STATUS_405.equals(code)) {
                operations.put(RedisKeys.YSF_ACCOUNT_LAST_STATUS_KEY, accountId.toString(), code);
            }
            return flag;
        }
        String toolLastStatus = null, ysfLastStatus = null;
        if (exitsTool) {
            toolLastStatus = (String) operations.get(RedisKeys.APP_ACCOUNT_LAST_STATUS_KEY, accountId.toString());
        }
        if (exitsYsf) {
            ysfLastStatus = (String) operations.get(RedisKeys.YSF_ACCOUNT_LAST_STATUS_KEY, accountId.toString());
        }
        log.debug("本次传来的值:{},上次tool存值:{},上次云闪付存值:{}", code, toolLastStatus, ysfLastStatus);
        if (LAST_STATUS_22.equals(code)) {
            // 本次操作 工具恢复
            if (StringUtils.isBlank(ysfLastStatus) || LAST_STATUS_405.equals(ysfLastStatus)) {
                flag = true;
            } else {
                flag = false;
            }
            operations.put(RedisKeys.APP_ACCOUNT_LAST_STATUS_KEY, accountId.toString(), code);
        } else if (LAST_STATUS_88.equals(code)) {
            // 本次操作 工具暂停
            flag = false;
            operations.put(RedisKeys.APP_ACCOUNT_LAST_STATUS_KEY, accountId.toString(), code);
        } else if (LAST_STATUS_400.equals(code)) {
            // 本次操作 云闪付暂停
            flag = false;
            operations.put(RedisKeys.YSF_ACCOUNT_LAST_STATUS_KEY, accountId.toString(), code);
        } else if (LAST_STATUS_405.equals(code)) {
            // 本次操作 云闪付启用
            if (StringUtils.isBlank(toolLastStatus) || LAST_STATUS_22.equals(toolLastStatus)) {
                flag = true;
            } else {
                flag = false;
            }
            operations.put(RedisKeys.YSF_ACCOUNT_LAST_STATUS_KEY, accountId.toString(), code);
        }
        return flag;
    }

    @Override
    public void initQuickPayTime(String accountId, String initTime) {
        log.debug("AccountService initQuickPayTime accountId{} , initTime {}", accountId, initTime);
        AccountBaseInfo base = getFromCacheById(Integer.valueOf(accountId));
        if (base == null || ObjectUtils.isEmpty(base.getPassageId())) {
            log.info("base is null or base.getPassageId is null");
            return;
        }
        String strKey = RedisKeys.YSF_INIT_TIME + base.getPassageId();
        Set<ZSetOperations.TypedTuple<String>> timeSet = redisService.getStringRedisTemplate().opsForZSet()
                .reverseRangeWithScores(strKey, 0, -1);
        long lInitTime = Long.parseLong(initTime) + 46 * 60 * 1000;
        for (int i = 0; i < 12; i++) {
            boolean isok = checkTime(timeSet, accountId, lInitTime, strKey);
            if (isok) {
                log.debug("AccountService initQuickPayTime accountId {},  schedule time {}", accountId, lInitTime);
                redisService.getStringRedisTemplate().opsForZSet().add(strKey, accountId, lInitTime);
                return;
            } else {
                lInitTime = lInitTime + 300000;
            }
        }
        redisService.getStringRedisTemplate().opsForZSet().add(strKey, accountId, lInitTime);
        log.debug("AccountService initQuickPayTime accountId{},timeSet is not null", accountId);
    }

    @Override
    public void sendDeviceInfo(String accountId, String ip, String equIdent) {
        if (StringUtils.isBlank(accountId) || StringUtils.isBlank(ip) || StringUtils.isBlank(equIdent)) {
            return;
        }
        AccountBaseInfo base = getFromCacheById(Integer.parseInt(accountId));
        String mobile = base.getMobile();
        rebateApiService.usersDevice(mobile, ip, equIdent);
    }

    private final Integer DISCONNET_MOBILE_STATUS = 999;

    @Override
    public void disconnectEvent(Integer accountId) {
        savePauseOrResumeOrOnlineForMobile(accountId, DISCONNET_MOBILE_STATUS);
    }

    /**
     * 回收/取消回收 下发卡
     *
     * @param operator
     * @param accountId
     * @param recycleOrCancel
     */
    @Override
    public void recycle4BindComm(SysUser operator, Integer accountId, boolean recycleOrCancel) {
        if (recycleOrCancel) {
            redisService.getStringRedisTemplate().boundSetOps(RedisKeys.RECYCLE_BINDCOMM_SET).add(accountId.toString());
            unlockThirdInAccount4Draw(operator.getId(), accountId, null);
        } else {
            redisService.getStringRedisTemplate().boundSetOps(RedisKeys.RECYCLE_BINDCOMM_SET)
                    .remove(accountId.toString());
        }
    }

    /**
     * 获取已回收的下发卡结果集
     *
     * @return
     */
    @Override
    public Set<String> getRecycleBindComm() {
        return redisService.getStringRedisTemplate().boundSetOps(RedisKeys.RECYCLE_BINDCOMM_SET).members();
    }

    /**
     * 校验入款卡账号子类型修改时是否有告警信息
     *
     * @param data
     * @return
     */
    public List<String> checkTypeChangeAlarm(JSONArray data) {
        if (data != null) {
            Map<String, List<AccountBaseInfo>> needCheck = new HashMap<>();
            try {
                for (int i = 0; i < data.length(); i++) {
                    JSONObject obj = data.getJSONObject(i);
                    Integer accId = Integer.parseInt(obj.getString("bankId"));
                    Integer outEnable = Integer.parseInt(obj.getString("outEnable"));
                    AccountBaseInfo base = getFromCacheById(accId);
                    if (base != null) {
                        if (outEnable == 2) {
                            if (!Objects.equals(base.getSubType(), InBankSubType.IN_BANK_YSF.getSubType())) {
                                needCheck = buildNeedCheck(base, needCheck);
                            }
                        } else if (Objects.equals(base.getSubType(), InBankSubType.IN_BANK_YSF.getSubType())) {
                            needCheck = buildNeedCheck(base, needCheck);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("checkTypeChangeAlarm>>请求参数格式异常");
            }
            if (!CollectionUtils.isEmpty(needCheck)) {
                return checkTypeChangeAlarm(needCheck);
            }
        }
        return null;
    }

    /**
     * 校验入款卡账号盘口修改时是否有告警信息
     *
     * @param id
     * @param handicap
     * @return
     */
    public List<String> checkTypeChangeAlarm(Integer id, String handicap, String account, String type, String subType)
            throws Exception {
        /**
         * List<String> res = new ArrayList<>(); AccountBaseInfo base =
         * getFromCacheById(id); if (base != null && base.getHandicapId() != null &&
         * base.getPassageId() != null) { String code =
         * handicapService.findFromCacheById(base.getHandicapId()).getCode(); if
         * (Objects.equals(base.getType(), AccountType.InBank.getTypeId())) { if
         * (!code.equals(handicap)) { throw new Exception("入款卡已绑定通道，不允许切换盘口"); } if
         * (!type.equals("1")) { throw new Exception("入款卡已绑定通道，不允许改卡类型"); } if
         * (!Objects.equals(base.getSubType(), Integer.parseInt(subType))) { Map<String,
         * List<AccountBaseInfo>> needCheck = new HashMap<>(); List<AccountBaseInfo>
         * list = new ArrayList<>(); list.add(base); needCheck.put(code, list); return
         * checkTypeChangeAlarm(needCheck); } } }
         **/
        return new ArrayList();
    }

    /**
     * 调平台接口，看是否有告警信息
     *
     * @param need
     * @return
     */
    private List<String> checkTypeChangeAlarm(Map<String, List<AccountBaseInfo>> need) {
        List<String> result = new ArrayList<>();
        /**
         * if (!CollectionUtils.isEmpty(need)) { RequestBody requestBody =
         * requestBodyParser.buildRequestBodyForCheckAlarm(need);
         * ThreadLocal<List<LinkedHashMap<String, Object>>> local = new ThreadLocal<>();
         * try {
         * HttpClientNew.getInstance().getPlatformServiceApi().WithdrawSync(requestBody).subscribe(data
         * -> { log.debug("查询账户类型切换告警信息：结果:{}", data); if (data.getStatus() == 1) {
         * local.set((List<LinkedHashMap<String, Object>>) data.getData()); } }, e -> {
         * log.error("查询账户类型切换告警信息,失败:{}", e.getStackTrace()); }); } catch (Exception e)
         * { log.error("查询账户类型切换告警信息时产生异常，异常信息：{}", e); } List<LinkedHashMap<String,
         * Object>> res = local.get(); if (!CollectionUtils.isEmpty(res)) { for (int i =
         * 0; i < res.size(); i++) { LinkedHashMap<String, Object> checked = res.get(i);
         * if (Objects.equals(1, checked.get("warnFlag"))) { result.add((String)
         * checked.get("cardNo")); } } } }
         **/
        return result;
    }

    /**
     * @param base
     * @param need
     * @return
     */
    private Map<String, List<AccountBaseInfo>> buildNeedCheck(AccountBaseInfo base,
                                                              Map<String, List<AccountBaseInfo>> need) {
        if (base != null && base.getPassageId() != null) {
            String code = handicapService.findFromCacheById(base.getHandicapId()).getCode();
            List<AccountBaseInfo> list = need.get(code);
            if (list == null) {
                list = new ArrayList<>();
                list.add(base);
            } else {
                list.add(base);
            }
            need.put(code, list);
        }
        return need;
    }

    private boolean checkTime(Set<ZSetOperations.TypedTuple<String>> timeSet, String accountId, long initTime,
                              String strKey) {
        for (ZSetOperations.TypedTuple<String> valScr : timeSet) {
            long time = valScr.getScore().longValue();
            if (valScr.getValue() == accountId) {
                continue;
            }
            if (Math.abs(initTime - time) < 300000) {
                log.debug("AccountService initQuickPayTime time {},  initTime{}", time, initTime);
                return false;
            }
        }
        return true;
    }

    private static boolean checkHostRunRight = false;

    @Value("${service.tag}")
    public void setServiceTag(String serviceTag) {
        if (Objects.nonNull(serviceTag)) {
            checkHostRunRight = ServiceDomain.valueOf(serviceTag) == ServiceDomain.REPORT;
        }
    }

    // 每天早上7点定时清算昨天的出入款数据
    @Scheduled(cron = "0 0 7 * * *")
    public void executeStored() {
        if (!checkHostRunRight)
            return;
        SimpleDateFormat sdFormatter = new SimpleDateFormat("yyyy-MM-dd");
        Date nowTime = new Date(System.currentTimeMillis());
        Calendar c = Calendar.getInstance();
        c.setTime(nowTime);
        c.add(Calendar.DAY_OF_MONTH, -1);
        Date tomorrow = c.getTime();
        String data = sdFormatter.format(tomorrow) + " 07:00:00";
        log.info("执行每日清算脚本时间：" + data);
        try {
            accountRepository.executeStored(data);
        } catch (Exception e) {
            log.info("执行每日清算脚本时间：" + data + "失败！", e);
        }
    }

    private static final ConcurrentHashMap<Integer, Long> noticeMap = new ConcurrentHashMap();

    /**
     * 增加或提示账号提示信息
     *
     * @param accIds
     * @param addOrRemove
     */
    public void addOrRemoveAccountNotice(List<Integer> accIds, boolean addOrRemove) {
        if (addOrRemove) {
            if (!CollectionUtils.isEmpty(accIds)) {
                Long currTM = System.currentTimeMillis();
                accIds.forEach(p -> noticeMap.put(p, currTM));
            }
        } else {
            if (!CollectionUtils.isEmpty(accIds)) {
                Long currTM = System.currentTimeMillis();
                accIds.forEach(p -> noticeMap.remove(p));
            }
        }
    }

    public boolean hasAccountNotice(Integer accId) {
        return noticeMap.contains(accId);
    }

    public List<Integer> getAllAccIdByStatus(Integer status) {
        return accountRepository.findAllByStatus(status);
    }

    @Override
    public boolean recalculate(String uid) {
        BizAccountMore more = accountMoreService.getFromByUid(uid);
        // 判断是否有卡在跑 ，如果没有则不能计算额度
        Object chgStr = redisService.getStringRedisTemplate().opsForHash().get(RedisKeys.ACC_CHG, more.getMoible());
        String chgId = "";
        if (Objects.nonNull(chgStr)) {
            String[] accMag = chgStr.toString().split(":");
            chgId = accMag[1];
        } else {
            return false;
        }
        String[] accIds = more.getAccounts().substring(1).split(",");
        BigDecimal balances = new BigDecimal("0");
        if (accIds.length == 1) {
            more.setLinelimit(more.getMargin());
        } else {
            for (String accId : StringUtils.trimToEmpty(more.getAccounts()).split(",")) {
                if (StringUtils.isBlank(accId) || !StringUtils.isNumeric(accId) || accId.equals(chgId))
                    continue;
                BizAccount account = getById(Integer.valueOf(accId));
                if (more.getMargin() != null && Objects.nonNull(account) && account.getBankBalance().floatValue() > 0) {
                    balances = balances.add(account.getBankBalance());
                }
            }
            more.setLinelimit(more.getMargin().subtract(balances));
        }
        accountMoreService.saveAndFlash(more);
        return true;
    }

    private BizAccount packZeroLimit(BizAccount account) {
        BizAccountMore more = accountMoreService.getFromCacheByMobile(account.getMobile());
        if (null != more && (null == more.getMargin() ? BigDecimal.ZERO : more.getMargin())
                .compareTo(new BigDecimal("1000")) != 1) {
            String[] accounts = more.getAccounts().split(",");
            List<String> tmp = new ArrayList<String>();
            for (String str : accounts) {
                if (str != null && str.length() != 0) {
                    tmp.add(str);
                }
            }
            if (tmp.size() > 1) {
                BigDecimal peakBalance = accountRepository.getTotalAmountsByAc(tmp);
                peakBalance = peakBalance.floatValue() < 0 ? BigDecimal.ZERO : peakBalance;
                account.setDeposit(peakBalance);
            } else {
                if (null == account.getPeakBalance()) {
                    account.setDeposit(BigDecimal.ZERO);
                } else {
                    account.setDeposit((account.getPeakBalance() < 0 ? BigDecimal.ZERO
                            : new BigDecimal(account.getPeakBalance().toString())));
                }
            }
        }
        return account;
    }

    @Override
    public boolean syncThirdStatus(int handicap, int type, String account, String bankType) {
        String data = "";
        try {
            HashMap<String, Object> params = new HashMap<>();
            params.put("oid", handicap);
            params.put("stype", 2);
            params.put("account", account);
            params.put("bankType", bankType);
            data = mapper.writeValueAsString(params);
            SimpleResponseData[] ret = new SimpleResponseData[1];
            HttpClientNew.getInstance().getPlatformServiceApi().syncThirdStatus(buildReqBody(data)).subscribe(
                    d -> ret[0] = d, e -> log.error("syncThirdStatus >> errorExp  data: {}", "同步第三方账号失败！", e));
            if (Objects.nonNull(ret[0]) && ret[0].getStatus() == 1) {
                log.info("syncThirdStatus >> Receive Msg: {}  status: {}  data: {}", ret[0].getMessage(),
                        ret[0].getStatus(), data);
                return true;
            } else {
                log.info("syncThirdStatus >> Receive Msg: {}  status: {}  data: {}", ret[0].getMessage(),
                        ret[0].getStatus(), data);
                return false;
            }
        } catch (Exception e) {
            log.error("syncThirdStatus >> finalExp", e);
            return false;
        }
    }

    private RequestBody buildReqBody(String params) {
        try {
            return RequestBody.create(MediaType.parse(CONSTANT_HTTP_MEDIA_TYPE), params);
        } catch (Exception e) {
            return null;
        }
    }

}