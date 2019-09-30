package com.xinbo.fundstransfer.service;

import com.xinbo.fundstransfer.component.net.socket.ActionEventEnum;
import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.AccountFlag;
import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.enums.CurrentSystemLevel;
import com.xinbo.fundstransfer.domain.pojo.*;
import com.xinbo.fundstransfer.restful.v2.pojo.RequestAccount;
import com.xinbo.fundstransfer.unionpay.ysf.inputdto.InAccountBindedYSFInputDTO;
import org.json.JSONArray;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.ZSetOperations;

import java.math.BigDecimal;
import java.util.*;

/**
 * 账号管理
 *
 * @author Eden
 */
public interface AccountService {
	/**
	 * 获取账号收款流水
	 *
	 * @param accountId
	 * @return
	 */
	BigDecimal getDailyInFlow(Integer accountId);

	Map<Integer, BigDecimal> allDailyInFlow(List<Integer> accountIds);

	/**
	 * 判断账号是否在线
	 *
	 * @param accountId
	 * @return true 在线 false 不在线
	 */
	boolean isOnline(Integer accountId, Integer accountType);

	void dealMatch(Integer toId);

	/**
	 * 选定第三方账号 作为待定下发的时候 保存操作记录
	 */
	void saveSelectThirdRecord(Integer thirdId, List<Integer> accountIds);

	void saveToUnfinishedDrawInThird(Integer accountId);

	void deleteUnfinishedDrawInThirdByMatched(Integer accountId);

	List<String> getUnfinishedInThirdDraw();

	void deleteSelectThirdRecord(Integer toId);

	/**
	 * 根据 toid 获取选定的第三方账号
	 *
	 * @param toId
	 * @return
	 */
	Integer getSelectThirdRecordByToId(Integer toId);

	Integer getSelectedThirdIdByLockedId(Integer accountId);

	Map<String, String> allSelectedThirdIdsWithLockedId(List<Integer> accountIds);

	/***
	 * 查询我的设定
	 *
	 * @param userId
	 * @return
	 */
	List<Integer> getMySetUpThirdAccount(Integer userId);

	/**** 查询 全部设定的第三方账号 ***/
	List<Integer> getAllSetUpThirdAccount(Integer userId);

	/**** 查询 全部设定的第三方账号 ***/
	Map<Integer, String> getAllSetUpThirdAccount();

	/**
	 * 判断是否已经被设定
	 *
	 * @param thirdIds
	 * @return
	 */
	List<Integer> filterAlreadySetted(List<Integer> thirdIds);

	void setThirdAccount(List<Integer> thirdAccounts, Integer userId, boolean add);

	void releaseOtherSetup(Integer thirdId);

	/**
	 * 锁定 的时候 保存 锁定人 和时间 解锁删除 从锁定到 提单完成匹配 才会删除 不然一直显示总耗时
	 *
	 * @param userId
	 * @param accountId
	 * @param locked
	 */
	void saveLocked(Integer userId, List<Integer> accountId, boolean locked);

	/**
	 * 获取所有锁定的id和锁定时间
	 *
	 * @return
	 */
	Map<Integer, String> allLockedIdAndTime();

	/**
	 * 出款卡 添加时间 删除添加时间 以计算总耗时
	 *
	 * @param accountId
	 * @param locked
	 */
	void saveAddTimeForOutCard(Integer accountId, boolean locked);

	boolean judgeLocked(Integer accountId);

	/**
	 * 是否已经被添加到 耗时里
	 *
	 * @param accountId
	 * @return
	 */
	boolean isAlreadyAddedToTime(Integer accountId);

	List<Integer> allOtherIdsAddedToTime();

	Map<String, String> getOutCardAddedTime();

	/**
	 * 保存提现时间
	 *
	 * @param accountId
	 * @param saveFlag
	 */
	void saveDrawTime(Integer accountId, boolean saveFlag);

	/**
	 * 获取 保存的提现时间
	 */
	Map<String, String> allDrawTimeMap(List<Integer> accountIds);

	/**
	 * 获取 所有锁定人 和锁定时间
	 *
	 * @return
	 */
	Map<String, Map<String, String>> getLockerNameAndLockedTime();

	/**
	 * 判断是否 下发中
	 *
	 * @param accountId
	 * @return
	 */
	boolean isDrawing(Integer accountId);

	List<Integer> allDrawingIds();

	/**
	 * 判定是否 下发失败
	 *
	 * @param accountId
	 * @return
	 */
	boolean isDrawFailed(Integer accountId);

	List<Integer> failedDrawIds(List<Integer> accountIds);

	/**
	 * 判断 是否被锁定
	 *
	 * @param accountId
	 * @return
	 */
	boolean isLocked(Integer accountId);

	List<Integer> allLockedId();

	/***
	 * 保存 提现的时候 出款卡id 金额 手续费
	 *
	 * @param thirdId
	 * @param accountId
	 * @param amount
	 * @param fee
	 */
	void saveDrawAmountAndFee(Integer thirdId, Integer accountId, BigDecimal amount, BigDecimal fee);

	/**
	 * 匹配 或者 打回之后 要删除 下发的金额 手续费
	 *
	 * @param accountId
	 */
	void deleteDrawAmountAndFee(Integer accountId);

	/**
	 * 根据账号id 查询 下发的金额 手续费
	 *
	 * @return
	 */
	Map<String, Map<String, String>> getDrawAmountAndFeeByAccountId(Integer accountId);

	Map<String, Map<String, String>> getDrawAmountAndFeeByAccountIds(List<Integer> accountIds);

	/**
	 * 查询 下发任务 下发完成 失败 记录
	 *
	 * @param inputDTO
	 * @param handicapIds
	 *            用户的盘口权限
	 * @return
	 */
	List<FindDrawTaskOutputDTO> findDrawTaskRecord(FindDrawTaskInputDTO inputDTO, String[] handicapIds);

	/**
	 * 查询 下发任务
	 *
	 * @param inputDTO
	 * @param handicapIds
	 *            用户的盘口权限
	 * @return
	 */
	FindDrawTaskResult findDrawTask(FindDrawTaskInputDTO inputDTO, String[] handicapIds);

	List<FindDrawTaskOutputDTO> filterExclued(List<FindDrawTaskOutputDTO> toFilterList);

	List<String> excludedIds();

	void sortAndInnerSequence(List<FindDrawTaskOutputDTO> toFilterList, List<Integer> accountIds,
			List<FindDrawTaskOutputDTO> outCards, List<FindDrawTaskOutputDTO> otherCards1,
			List<FindDrawTaskOutputDTO> otherCards2);

	void wrapFinalData(FindDrawTaskInputDTO inputDTO, List<FindDrawTaskOutputDTO> toWrapData,
			FindDrawTaskResult result);

	/**
	 * 根据编号查询任务
	 *
	 * @param alias
	 * @return
	 */
	List<FindDrawTaskOutputDTO> findDrawTaskByAlias(String alias, SysUser user);

	List<Integer> findOtherNeedDrawCardIds();

	/**
	 * 新的下发任务
	 *
	 * @return
	 */
	List<Integer> findNewDrawTask();

	/**
	 * 在线的出款卡 和 下发卡
	 *
	 * @return
	 */
	List<Integer> outCard5OrOtherCard13Online();

	/**
	 * 全部下发中的 锁定和下发但未到账的
	 *
	 * @return
	 */
	List<Integer> findLockedOrUnfinishedDrawTask();

	/**
	 * 某人锁定的 下发中
	 *
	 * @param userId
	 * @return
	 */
	List<Integer> findLockedByOneDrawTask(Integer userId);

	/**
	 * 某人已经提现但是未到账的
	 *
	 * @param userId
	 * @return
	 */
	List<Integer> findUnfinishedByOneDrawTask(Integer userId);

	/**
	 * 在下发任务里 所有锁定的卡
	 *
	 * @return
	 */
	List<Integer> allLockedInDrawTask();

	Map<String, Set<ZSetOperations.TypedTuple>> getAllLockedInDrawTask();

	/**
	 * 在下发任务里 当前人(userId不为空) 或者 全部的(userId 为空)
	 *
	 * @param userId
	 *            如果传null 则是全部的
	 *
	 * @return
	 */
	List<Integer> outCardsOrOtherCardsLockedByUserInDrawTask(Integer userId);

	/**
	 * 下发任务里 某人提现 尚未到账的
	 *
	 * @param userId
	 * @return
	 */
	List<Integer> unfinishedInDrawTasksByOne(Integer userId);

	/**
	 * 在第三方入款 提现里 锁定的所有卡 :包括 出款卡 和 下发卡
	 *
	 * @return
	 */
	List<Integer> allLockedInThirdInAccount();

	/**
	 * 下发任务 锁定 解锁 操作
	 *
	 * @param userId
	 * @param accountIds
	 * @param type
	 *            true 锁定 false 解锁
	 * @return true 锁定成功 false 锁定失败
	 */
	boolean lockedOrUnlockByDrawTask(Integer userId, List<Integer> accountIds, boolean type);

	/**
	 * 通过编号搜索账号 下发金额<br>
	 * 所需金额 :可用额度 - 余额 or 今日出款限额 - 已出款限额 的最小值
	 *
	 * @param accountId
	 * @return
	 */
	BigDecimal availableAmountForDrawByAlias(Integer accountId);

	boolean saveLockedOutCardInDrawTask(Integer outId, boolean add);

	List<Integer> getLockedOutCardInDrawTask();

	/**
	 * 获取出款卡 锁定时间
	 *
	 * @param accountId
	 * @return
	 */
	Long getOutcardLockedTime(Integer accountId);

	/**
	 * 下发任务里 查询用户锁定的出款卡 下发卡
	 *
	 * @param userId
	 * @return
	 */
	List<Integer> getAccountIdLockedByUserIdInDrawTask(Integer userId);

	/**
	 * 判断出款卡 或者 下发卡 是否已经在 下发任务 页签锁定了
	 *
	 * @param accountId
	 * @return
	 */
	boolean isLockedInDrawTaskPage(Integer accountId);

	/**
	 * 判断出款卡 或者 下发卡 是否已经在 第三方入款 提现 页签锁定了
	 *
	 * @param accountId
	 * @return
	 */
	boolean isLockedInThirdInAccountPage(Integer accountId);

	/**
	 * 根据账号 id 查询 PfTypeSub
	 *
	 * @param accountId
	 * @return
	 */
	Integer getPfTypeSubVal(Integer accountId);

	/**
	 * 查询账号信息
	 *
	 * @param inputDTO
	 * @return
	 */
	List<BizAccount> list(AccountListInputDTO inputDTO);

	/**
	 * 出款卡或者下发卡 解锁第三方账号
	 *
	 * @param userId
	 * @param accountId
	 * @param thirdAccountId
	 * @return
	 */
	boolean thirdAccountIdUnlockByOtherId(Integer userId, Integer accountId, Integer thirdAccountId);

	/**
	 * 出款卡或者下发卡 锁定第三方账号
	 *
	 * @param userId
	 * @param accountId
	 * @param thirdAccountId
	 * @return
	 */
	boolean thirdAccountIdLockByOtherId(Integer userId, Integer accountId, Integer thirdAccountId);

	/**
	 * 根据 被锁定的账号id 获取 第三方账号id
	 *
	 * @param accountIdLocked
	 * @return
	 */
	Integer getThirdAccountIdByLockedId(Integer accountIdLocked);

	/**
	 * 第三方账号 锁定和解锁之后 扣除系统余额 加回系统余额
	 *
	 * @param accountId
	 *            第三方账号id
	 * @param toAccountId
	 *            出款账号id
	 * @param amount
	 *            金额 : 金额 : 如果新的值等于缓存的值则不变，如果大于则扣除，如果小于则加回
	 */
	void dealSysBalanceLockedOrUnlocked(Integer accountId, Integer toAccountId, BigDecimal amount);

	void addSysBalanceByDrawOtherThirdAccount(Integer oldThirdId, Integer toAccountId);

	/**
	 * 解锁 或者 提现完成之后 把缓存的系统余额更新金额删除
	 *
	 * @param accountId
	 * @param toAccountId
	 *            如果 为空 则可能是删除模态框或者最小化模态框导致的关闭
	 */
	void removeAmountInputStored(Integer accountId, Integer toAccountId);

	/**
	 * 查询是否开启 第三方下发到出款卡 或者 保存开启 关闭
	 *
	 * @param action
	 *            没有值查询 有值则是前端开启或者关闭 1 开启 2 关闭
	 * @return
	 */
	String enableThirdDrawToOutCard(String action);

	/**
	 * 判断 出款卡 当日出款流水+余额 是否大于等于 当日出款限额
	 *
	 * @param accountId
	 * @return
	 */
	boolean isFlowBalaceLargerThanDailyOut(Integer accountId);

	/**
	 * 判断 卡是否低于 返利网信用额度 PC使用 最低余额限制 lowest_out 不使用百分比
	 *
	 * @param accountId
	 * @return
	 */
	boolean belowPercentageNeedThirdDraw(Integer accountId);

	/**
	 * 页面提现完成之后 删除锁定的记录
	 *
	 * @param userId
	 * @param accountId
	 * @param thirdAccountId
	 */
	void removeLockedRecordByUser(String userId, String accountId, String thirdAccountId);

	/**
	 * 系统检测到锁定的出款卡过期了就删除
	 *
	 * @param userId
	 * @param member
	 */
	void removeLockedExpiredBySystem(String thirdAccountId, String userId, String member);

	/**
	 * 第三方下发--当前人锁定的所有出款卡
	 *
	 * @param userId
	 * @return
	 */
	List<String> outCardIdsLockedByUserId(Integer userId);

	/**
	 * 第三方下发 --所有锁定的 出款卡 带有分数-锁定时间
	 *
	 * @return
	 */
	Map<String, Map<String, Long>> allOutCardIdsLockedWithScoreTime();

	/**
	 * 所有下发未确认的缓存
	 *
	 * @return
	 */
	Map<String, Long> allUnfinished();

	/**
	 * 第三方下发 --所有锁定的 出款卡
	 *
	 * @return
	 */
	List<String> allOutCardIdsLocked();

	/**
	 * 第三方账号下发完成之后 流水匹配之后 从锁定的记录中删除掉
	 *
	 * @param unlocker
	 *            系统自动匹配的流水 则是 -1 页面操作解锁的是操作人id
	 * @param accountId
	 * @param accountType
	 * @return
	 */
	long removeLockedNeedAmountOutCard(Integer unlocker, Integer accountId, Integer accountType,
			Integer thirdAccountId);

	void removeLockedRecordThirdDrawToOutCard(Integer thirdAccountId, Integer outAccountId);

	void removeLockedHash(Integer accountId);

	Integer getFromIdFromLockedHash(Integer accountId);

	String getFromIdWithNanoFromLockedHash(Integer accountId);

	/**
	 * 系统自动匹配的流水 从锁定的记录中删除掉
	 *
	 * @param accountId
	 * @return
	 */
	long removeBySystem(Integer accountId);

	/**
	 * 提现确认 删除锁定 添加到待确认队列
	 *
	 * @param unlocker
	 * @param accountId
	 * @param thirdAccountId
	 */
	void unlockedAndAddUnfinished(Integer unlocker, Integer accountId, Integer thirdAccountId);

	/**
	 * 解锁
	 *
	 * @param unlocker
	 * @param accountId
	 * @return
	 */
	Long unlockedThirdToDrawList(Integer unlocker, Integer accountId, Integer thirdAccountId);

	/**
	 * 第三方账号下发完成之后 移除出款账号所需金额队列
	 *
	 * @param accountId
	 */
	void removeNeedAmountOutCardForUnlocked(Integer accountId);

	void removeNeedAmountOutCard(Integer accountId);

	/**
	 * 判断 该出款账号是否还在下发待确认hash里
	 *
	 * @param accountId
	 * @param accountType
	 * @return
	 */
	boolean unfinished(Integer accountId, Integer accountType);

	/**
	 * 第三方账号下发到指定出款账号 锁定
	 *
	 * @param locker
	 * @param accountId
	 * @param thirdAccountId
	 * @return
	 */
	long lockOutCardNeedAmount(Integer locker, Integer accountId, Integer thirdAccountId);

	/**
	 * 锁定之后 记录 toid fromid
	 *
	 * @param accountId
	 *            出款卡id
	 * @param thirdAccountId
	 *            第三方账号id
	 * @return
	 */
	Boolean addLockedHash(Integer accountId, String thirdAccountId);

	/**
	 * 判断 是否已经被其他人锁定了
	 *
	 * @param thirdAccountId
	 * @param accountId
	 * @return
	 */
	boolean checkLockedByOther(Integer thirdAccountId, Integer accountId);

	/**
	 * 所有 未锁定和已锁定 已下发待确认的出款卡账号id
	 *
	 * @return
	 */
	List<Integer> allOutCardIdsNeedThirdDrawOrUnfinished();

	/**
	 * 返回 可以绑定或者锁定的 出款卡id集合
	 *
	 * @return
	 */
	List<Integer> needThirdDrawToOutCardIds();

	/**
	 * 判断 是否满足可以使用第三方下发
	 *
	 * @param cardType
	 * @param needAmount
	 * @return
	 */
	boolean needThirdDrawTo(Integer cardType, Integer needAmount);

	/**
	 * 判断 是否在 出款卡下发队列里
	 *
	 * @param accountId
	 * @return
	 */
	boolean isInNewOutNeedOri(Integer accountId);

	/**
	 * 判断是否在 已锁定的队列里了
	 *
	 * @param accountId
	 * @param currentUserId
	 * @return
	 */
	boolean isLockedByOtherMeans(Integer accountId, Integer currentUserId);

	List<Integer> allToIdsLockedByThirdIn(List<Integer> accountIdsTarget);

	/**
	 * 添加到需要绑定或者可以锁定的 出款账号id 缓存中
	 *
	 * @param accountId
	 * @param needAmount
	 * @param accountType
	 */
	void addNeedThirdDrawToOutCardList(Integer accountId, Integer needAmount, Integer accountType);

	/**
	 * 单次可下发金额是否为正的
	 *
	 * @param accountId
	 * @return
	 */
	boolean singleTimeDrawAbleFlag(Integer accountId);

	/**
	 * 判断账号是否是 可用
	 *
	 * @param id
	 * @return
	 */
	boolean usingOrUsable(Integer id);

	/**
	 * 获取缓存的在线的账号id
	 */
	List<Integer> onlineAccountIdsList(Integer type);

	/**
	 * 保存或者删除缓存的(出入款)账号id
	 */
	void saveOnlineAccontIds(Integer accountId, boolean add);

	/**
	 * 新增入款卡
	 */
	BizAccount addInBankAccount(AddInBankAccountInputDTO inputDTO, BizAccount old);

	/**
	 * 多条记录更新保存
	 */
	List<BizAccount> saveIterable(List<BizAccount> list);

	/**
	 * 单条记录更新保存
	 */
	BizAccount save(BizAccount entity);

	/**
	 * 通过id集合查询
	 */
	List<BizAccount> findByIds(ArrayList<Integer> list);

	/**
	 * 通过账号查询
	 */
	BizAccount findByAccountNo(String accountNo, Integer subType);

	/**
	 * 新增其他账号，如云山付账号的时候同时新增入款银行卡(参见平台同步入款银行卡) 以供绑定
	 */
	List<BizAccount> createIncomeAccount(List<InAccountBindedYSFInputDTO> dto);

	/**
	 * 更新其他账号，如云山付账号的时候同时更新入款银行卡(参见平台同步入款银行卡) 以供绑定
	 */
	List<BizAccount> updateIncomeAccount(List<InAccountBindedYSFInputDTO> dto);

	boolean checkUpdateAccount(BizHandicap handicap, AccountBaseInfo b, RequestAccount o);

	void updateAccountLevels(List<BizAccountLevel> bizAccountLevelList, List<Integer> levelIdList, BizAccount db,
			String[] levelArray);

	void bindLevels(List<Integer> levelIdList, BizAccount db, String[] levelArray);

	boolean checkThirdInAccount4DrawLocked(Integer userId, Integer[] toAccountId, Integer fromAccountId);

	long lockThirdInAccount4Draw(int userId, int accountId, Integer incomeAccountId);

	long unlockThirdInAccount4Draw(int userId, int accountId, Integer incomeAccountId);

	void unlockThirdInAccount(Integer userId, List<Integer> lockedIds);

	List<String> getTargetAccountLockedInRedis(Integer userId, Integer incomeAccountId);

	List<String> getTargetAccountLockedByCurrentUserInRedis(Integer userId, Integer incomeAccountId);

	List<String> getLockedAccByUserId(Integer userId);

	/**
	 * 根据账号删除层级账号绑定信息
	 */
	void deleteInBatch(Iterable<BizAccountLevel> iterable);

	/**
	 * 根据账号 层级 id查询
	 */
	List<BizAccountLevel> findByAccountId(Integer accountId);

	/**
	 * 根据账号 别名
	 */
	List<BizAccount> findByAlias(String alias);

	List<BizAccount> findByAliasAndTypeInForDrawTask(String alias);

	/**
	 * 根据账号
	 */
	List<BizAccount> findByAccount(String account);

	/**
	 * 保存账号层级信息
	 */
	void saveBizAccountLevel(BizAccountLevel bizAccountLevel);

	/**
	 * 刷新余额
	 */
	int updateBankBalance(BigDecimal bankBalance, int id);

	List<Integer> queryAccountIdsByAlias(String accountAlias);

	List<Integer> queryAccountIdsByAliasOrPhoneRobot(OutwardTaskTotalInputDTO inputDTO);

	List<Object> queryAccountForOutDrawing(String accountAlias, String bankType, String currentLevel);

	/**
	 * 获取账号表当前最大的编号
	 *
	 * @return
	 */
	String getMaxAlias();

	/**
	 * 查询该账号下同一层级的其他 冻结 暂停的账号
	 */
	List<Integer> findAccountIdInSameLevel(Integer accountId);

	List<BizAccount> getAllOutAccount(Integer[] types);

	/**
	 * 根据层级id查询同层级下的其他账号Id
	 *
	 * @return accountId List
	 */
	List<Integer> getBizAccountLevelList(Integer accountId);

	/**
	 * 获取账号基本信息
	 */
	AccountBaseInfo getFromCacheById(Integer accountId);

	/**
	 * 获取账号基本信息
	 */
	AccountBaseInfo getFromCacheByHandicapIdAndAccount(int handicapId, String account);

	/**
	 * 获取账号基本信息
	 */
	AccountBaseInfo getFromCacheByHandicapIdAndAccountAndBankName(int handicapId, String account, String bankName);

	/**
	 * 获取账号基本信息
	 */
	AccountBaseInfo getFromCacheByTypeAndAccount(int type, String account);

	/**
	 * 广播通知：基本信息
	 */
	void broadCast(BizAccount vo);

	/**
	 * 广播通知：批量刷新基本信息
	 */
	void broadCast() throws Exception;

	/**
	 * 接受通知：账号基本信息
	 */
	void flushCache(AccountBaseInfo baseInfo);

	/**
	 * 接受通知：批量刷新账号基本信息
	 */
	void flushCache();

	/**
	 * 获取账号信息(数据库中有的信息)
	 */
	BizAccount getById(Integer id);

	/**
	 * 获取账号信息(数据库中有的信息)
	 *
	 * @param specification
	 *            查询条件集合
	 */
	List<BizAccount> getAccountList(Specification<BizAccount> specification, SysUser sysUser);

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
	List<BizAccount> getIncomeAccountList(Integer handicapId, List<Integer> levelIdList, Integer... incomeTypeArray);

	/**
	 * 根据ID获取账号信息(并返回附加信息)
	 *
	 * @param accountId
	 *            账号ID
	 */
	BizAccount findById(SysUser operator, Integer accountId);

	/**
	 * 获取账号信息（并附加信息)
	 *
	 * @param operator
	 *            操作者
	 * @param specification
	 *            查询条件
	 */
	List<BizAccount> findList(SysUser operator, Specification<BizAccount> specification);

	List<BizAccount> findBySpecificAndSort(SysUser operator, Specification<BizAccount> specification, Sort sort);

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
	Page<BizAccount> findPage(SysUser operator, Specification<BizAccount> specification, Pageable pageable)
			throws Exception;

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
	Page<BizAccount> findPageOrderByAmountDaily(SysUser operator, List<Integer> accountIdArray, Pageable pageable)
			throws Exception;

	/**
	 * 查找出款人员的出款账号(并返回附加信息)
	 *
	 * @param operator
	 *            出款人
	 * @param type
	 *            账号类型
	 * @param accountNo
	 *            账号
	 * @return
	 */
	Page<BizAccount> find4OutwardAsign(Integer operator, Integer type, String accountNo, PageRequest pageRequest);

	/**
	 * 查询出款账号总记录数
	 */
	Long find4OutwardAsignCount(Integer operator, Integer type, String accountNo);

	List<BizAccount> find4OutwardAsign(Integer userId);

	BizAccount saveRebateAcc(BizAccount vo, BizAccountMore more, String uid, AccountFlag flag);

	BizAccount create(List<BizAccountLevel> accountLevelList, BizAccount vo) throws Exception;

	BizAccount create(BizHandicap handicap, AccountType accType, AccountStatus accStatus, CurrentSystemLevel level,
			String acc, String owner, BigDecimal limitIn, String bankAddr, SysUser opr, String remark) throws Exception;

	/**
	 * 此方法：限制调用
	 */
	@Deprecated
	void addBalance(BigDecimal amount, Integer id);

	void updateBaseInfo(BizAccount vo);

	/**
	 * Collection[0] -->Collection<BizHandicap><br/>
	 * Collection[1] -->Collection<BizLevel>
	 */
	List<Collection<Object>> findHandicapAndLevel(Integer... accountId);

	/**
	 * 根据条件集合查询账号ID
	 *
	 * @param filterToArray
	 *            条件集合
	 */
	List<Integer> findAccountIdList(SearchFilter... filterToArray);

	List<Object[]> findAccountIdAndBalList(SearchFilter... filterToArray);

	/**
	 * 获取某一层级下的账号ID集
	 *
	 * @param levelId
	 *            层级ID
	 * @return 账号ID集
	 */
	List<Integer> findAccountIdList(Integer levelId);

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
	List<Integer> findIncomeAccountIdList(Integer handicapId, Integer levelId, Integer operatorId);

	/**
	 * 查询当前人的入款账号id，update是true的时候，更新缓存，否则从缓存取
	 */
	List<Integer> findIncomeAccountIdList4User(boolean update, int userId);

	/**
	 *
	 */
	List<Integer> findLockAccountIdList(boolean isCurrentUser, int lockerId);

	/**
	 * 查询当日收款，当日出款的总计
	 *
	 * @param income0outward1
	 *            0:入款；1：出款
	 * @param accountIdArray
	 *            账号集合
	 */
	BigDecimal findAmountDailyByTotal(int income0outward1, Integer... accountIdArray);

	Map<Integer, BigDecimal> allAmountDailyTotal(int income0outward1, List<Integer> accountIds);

	/**
	 * 条件查询系统余额与银行余额的总计
	 *
	 * @param filterToArray
	 *            条件结果集合
	 */
	BigDecimal[] findBalanceAndBankBalanceByTotal(SearchFilter[] filterToArray);

	/**
	 * 获取账号的统计信息
	 *
	 * @param category
	 *            统计分类
	 * @param accountIdList
	 *            账号Id集合
	 */
	List<AccountStatInOut> findStatInOut(AccountStatInOut.Category category, List<Integer> accountIdList);

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
	Map<Integer, BigDecimal[]> findStat(List<Integer> accountIdList);

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
	Map<Integer, BigDecimal[]> findStat4BindCustomer(List<Integer> accountIdList);

	/**
	 * 获取账号的统计信息
	 *
	 * @param fromToArray
	 *            from:to
	 */
	List<AccountStatInOut> findStatFromTo(String[] fromToArray);

	/**
	 * param List<Object> Object exclusive String</>
	 */
	Map<Integer, BigDecimal> findAmountDaily(int income0outward1, List<Object> idList);

	Map<String, Float> findOutCountDaily();

	List<BizAccount> packAllForForAccount(SysUser operator, List<BizAccount> accountList);

	Float findOutCountDaily(int accId);

	List<BizAccount> statisticsInto(List<BizAccount> bizAccountList, SysUser sysUser, Integer incomeAccountId)
			throws Exception;

	/**
	 * 彻底删除账号信息
	 *
	 * @param accountId
	 */
	void deleteAndClear(Integer accountId);

	// 第三方下发记录
	Map<String, Object> findIssuedThird(int fromId, String startTime, String endTime, PageRequest pageRequest)
			throws Exception;

	// 第三方出现金记录
	Map<String, Object> findEncashThird(int fromId, String startTime, String endTime, PageRequest pageRequest)
			throws Exception;

	// 第三方出会员记录
	Map<String, Object> findMembersThird(int fromId, String startTime, String endTime, String handicaoCode,
			PageRequest pageRequest) throws Exception;

	// 导出出款卡银行流水信息
	Map<String, Object> findOutBankLog(String startTime, String endTime, String bankType, List<Integer> handicaps,
			PageRequest pageRequest) throws Exception;

	// 导出所有银行卡七点的流水
	Map<String, Object> find7TimeBalance(String startTime, List<Integer> handicaps, PageRequest pageRequest)
			throws Exception;

	// 修改出款卡当日出款限额
	void updateOuterLimit(Integer handicapId, Integer flag, Integer outerLimit, Integer middleLimit, Integer innerLimit,
			Integer specifyLimit, Integer type, Integer status, String bankType) throws Exception;

	/**
	 * 更新GPS信息
	 *
	 * @param accountId
	 * @param gps
	 *            清空传null 新增、修改传ip
	 * @throws Exception
	 */
	void updateGPS(Integer accountId, String gps);

	Map<String, List<Integer>> batchUpdateLimit(JSONArray data);

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
	void setModel(Integer accId, Integer trans, Integer crawl) throws Exception;

	String getModel(Integer accId);

	Map<Object, Object> getModel();

	ActionEventEnum getModel4PC(int accId);

	// 查询备用卡的系统记录表
	List<Object> findTransfers(String startTime, String endTime);

	Page<Object> findIncomeAccountOrderByBankLog(Integer[] handicapList, String account, String alias, String bankType,
			String owner, Integer[] search_IN_flag, Integer[] accountStatusList, PageRequest pageRequest)
			throws Exception;

	List<Integer> getAccountList(List<Integer> type, List<Integer> handicaps);

	// 新支付的根据设备号返回盘口编码和设备号的map
	Map<Integer, String[]> getDeviceNoAndHandicapCodeMap(String[] deviceCol);

	Map<String, Object> findDeleteAccount(int handicap, String alias, String type, String flag, String status,
			PageRequest pageRequest) throws Exception;

	void updateAcountById(List<String> accountIds, int status) throws Exception;

	void toStopTemp(int accountId, String remark, int uid) throws Exception;

	BizAccount setAccountAlias(BizAccount account);

	/**
	 * 分页获取待提额信息
	 *
	 * @param pageable
	 *            分页信息
	 */
	Page<BizAccount> findToBeRaisePage(SysUser operator, List<Integer> type, List<Integer> status,
			List<Integer> handicapId, List<Integer> currSysLevel, String bankType, String account, String owner,
			String alias, Pageable pageable) throws Exception;

	/**
	 * 获取总余额信息
	 */
	BigDecimal getTotalBankBalance(SysUser operator, List<Integer> type, List<Integer> status, List<Integer> handicapId,
			List<Integer> currSysLevel, String bankType, String account, String owner, String alias) throws Exception;

	BigDecimal getTotalAmountsByAc(List<String> accountsList);

	void updateFlagById(Integer id, Integer flag);

	void reportVersion(Integer accId, String curVer, String latestVer);

	List<Account> loginByUserPass(String username, String password);

	List<Account> getCurrAndList(String username);

	/**
	 * 获取人工出款账号
	 *
	 * @return
	 */
	List<BizAccount> findOutAccList4Manual();

	/**
	 * 入款卡状态变更时，通知平台
	 *
	 * @param account
	 * @param status
	 * @param oldAccount
	 */
	boolean modifyInBankStatus(BizAccount account, Integer status, String oldAccount, boolean modifySubType);

	Map<String, Object> showRebateUser(String handicapId, String bankType, Integer[] typeToArray, String alias,
			String account, String owner, String currSysLevel, Integer[] status, String rebateUser,
			BigDecimal startAmount, BigDecimal endAmount, String subType, PageRequest pageRequest) throws Exception;

	List<Account> getRebateAccListById(Integer accId);

	void disableQuickPay(String account);

	void enableQuickPay(String account);

	List<Integer> pausedMobileAccountIds(Integer type);

	void savePauseOrResumeAccountId(Integer accountId, Integer saveCode);

	void savePauseOrResumeOrOnlineForMobile(Integer accountId, Integer saveCode);

	boolean onlineEnableForMobile(Integer accountId, String code);

	void initQuickPayTime(String accountId, String initTime);

	void sendDeviceInfo(String accountId, String ip, String equIdent);

	void disconnectEvent(Integer accountId);

	/**
	 * 回收/取消回收 下发卡
	 *
	 * @param accountId
	 * @param recycleOrCancel
	 */
	void recycle4BindComm(SysUser operator, Integer accountId, boolean recycleOrCancel);

	/**
	 * 获取回收的下发卡结果集
	 *
	 * @return
	 */
	Set<String> getRecycleBindComm();

	/**
	 * 校验入款卡账号子类型修改时是否有告警信息
	 *
	 * @param data
	 * @return
	 */
	List<String> checkTypeChangeAlarm(JSONArray data);

	/**
	 * 校验入款卡账号盘口修改时是否有告警信息
	 *
	 * @param id
	 * @param handicap
	 * @return
	 */
	List<String> checkTypeChangeAlarm(Integer id, String handicap, String account, String type, String subType)
			throws Exception;

	/**
	 * 账号是否在系统账目提示信息中
	 *
	 * @param accId
	 * @return
	 */
	boolean hasAccountNotice(Integer accId);

	/**
	 * 账号添加到系统账目提示信息中或从提示信息中移除
	 *
	 * @param accIds
	 * @param addOrRemove
	 */
	void addOrRemoveAccountNotice(List<Integer> accIds, boolean addOrRemove);

	boolean recalculate(String uid);

	List<Integer> getAllAccIdByStatus(Integer status);

	boolean syncThirdStatus(int handicap, int type, String account, String bankType);

}