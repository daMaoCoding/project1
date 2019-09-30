/**
 *
 */
package com.xinbo.fundstransfer.unionpay.ysf.service.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.assign.Constants;
import com.xinbo.fundstransfer.component.net.http.cabana.HttpClientCabana;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.ResponseData;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.*;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.repository.AccountLevelRepository;
import com.xinbo.fundstransfer.domain.repository.AccountRepository;
import com.xinbo.fundstransfer.domain.repository.BizOtherAccountRepository;
import com.xinbo.fundstransfer.domain.repository.OtherAccountBindRepository;
import com.xinbo.fundstransfer.newinaccount.dto.input.CardForPayInputDTO;
import com.xinbo.fundstransfer.newinaccount.dto.output.CardForPayOutputDTO;
import com.xinbo.fundstransfer.newinaccount.dto.output.CardForPayOutputDTO.BankInfo;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import com.xinbo.fundstransfer.report.SystemAccountManager;
import com.xinbo.fundstransfer.sec.FundTransferEncrypter;
import com.xinbo.fundstransfer.service.*;
import com.xinbo.fundstransfer.unionpay.ysf.entity.YSFQResponseEntity;
import com.xinbo.fundstransfer.unionpay.ysf.entity.YSFQRrequestEntity;
import com.xinbo.fundstransfer.unionpay.ysf.entity.YSFQrCodeEntity;
import com.xinbo.fundstransfer.unionpay.ysf.inputdto.*;
import com.xinbo.fundstransfer.unionpay.ysf.outputdto.YSFGenerateQRRequestDTO;
import com.xinbo.fundstransfer.unionpay.ysf.outputdto.YSFQrCodeQueryDto;
import com.xinbo.fundstransfer.unionpay.ysf.outputdto.YSFResponseDto;
import com.xinbo.fundstransfer.unionpay.ysf.service.YSFService;
import com.xinbo.fundstransfer.unionpay.ysf.util.YSFLocalCacheUtil;
import com.xinbo.fundstransfer.unionpay.ysf.util.YSFQrCodeBase64StrReader;
import com.xinbo.fundstransfer.utils.randutil.InAccountRandUtil;
import com.xinbo.fundstransfer.utils.randutil.JedisLock;
import com.xinbo.fundstransfer.utils.randutil.NoAvailableRandomException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * 银联云闪付服务接口实现
 *
 * @author blake
 *
 */
@Slf4j
@Component
public class YSFServiceImpl implements YSFService {
	@Autowired
	private BizOtherAccountRepository repository;
	@Autowired
	private CabanaService cabanaService;
	@Autowired
	private AllocateTransService allocateTransService;
	@Value("${funds.transfer.cabanasalt}")
	private String cabanasalt;
	@Autowired
	private SystemAccountManager systemAccountManager;
	/**
	 * 银行卡号-二维码随机金额 hash 缓存外键
	 */
	private static final String BANK_ACCOUNT_RAND_STR = "zrand:random_global";

	/**
	 * 收款银行卡每日生成二维码保存的位置 <br>
	 * zrand:日期:银行卡号:金额（整数部分）
	 */
	private static final String BANK_ACCOUNT_RAND_DATE_QR = "zrand:%s:%s:%s";

	/**
	 * 银行账号随机金额时间锁，默认2
	 */
	@Value("${moneyAddCount:2}")
	private Integer moneyDeviceExpireHours;

	@Autowired
	RedisService redisService;
	@Autowired @Lazy
	AccountService accountService;

	@Autowired
	private YSFLocalCacheUtil ySFLocalCacheUtil;
	@Autowired
	private OtherAccountBindRepository bindingRepository;
	@Autowired
	private OtherAccountBindService bindingService;
	@Value("${funds.transfer.multipart.location}")
	private String uploadPathNew;
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private AccountChangeService accountChangeService;
	@Autowired
	private AccountLevelRepository accountLevelRepository;
	@Autowired
	private YSFQrcodeGenServiceImpl ySFQrcodeGenServiceImpl;

	@Autowired
	private InAccountRandUtil inAccountRandUtil;
	
	@Autowired
	private LevelService levelService;
	@Autowired
	private AccountRepository accountRepository;

	/** 本地缓存-- 银行卡账号--云闪付账号和登陆密码 */
	private LoadingCache<String, Map<String, String>> ysfAccountsLoginInfoCache = CacheBuilder.newBuilder()
			.maximumSize(1000).expireAfterWrite(7, TimeUnit.DAYS).initialCapacity(500)
			.build(new CacheLoader<String, Map<String, String>>() {
				@Override
				public Map<String, String> load(String bankAccountNo) {
					Map<String, String> map = findYsfLoginInfoByBankAccount(bankAccountNo);
					if (!ObjectUtils.isEmpty(map)) {
						return map;
					}
					return null;
				}
			});

	/** 云闪付的银行卡账号变更解绑的时候需要发起缓存刷新通知 */
	@Override
	public void noticeFreshCache(String bankAccountNo) {
		redisService.convertAndSend(RedisTopics.FRESH_INACCOUNT_YSFLOGIN_CACHE, bankAccountNo);
	}

	@Override
	public void freshCache(String bankAccountNo) {
		Map<String, String> map = findYsfLoginInfoByBankAccount(bankAccountNo);
		if (!ObjectUtils.isEmpty(map)) {
			ysfAccountsLoginInfoCache.refresh(bankAccountNo);
		}
	}

	/** 云闪付获取支付卡信息 */
	@Override
	public CardForPayOutputDTO cardForPayYSF(CardForPayInputDTO inputDTO, BizHandicap handicap) {
		CardForPayOutputDTO result = getQrCodeByReal(inputDTO, handicap);
		return result;
	}

	/**
	 * 设置用户金额锁
	 *
	 * @param userName
	 * @param result
	 */
	public void setUserLockStr(String userName, CardForPayOutputDTO result) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				if (!StringUtils.isEmpty(userName) && result != null && result.getFinalAmount() != null
						&& !CollectionUtils.isEmpty(result.getAccountList())) {
					BigDecimal amount = new BigDecimal(result.getFinalAmount().toString());
					String bankInAccount = result.getAccountList().iterator().next().getCardNo();
					inAccountRandUtil.setUserLockStr(bankInAccount, userName, amount);
				}
			}
		}).start();
	}

	/**
	 * 描述:根据银行卡账号查询 其他类型账号信息 如根据云闪付的查询云闪付的登陆信息
	 *
	 * @param bankAccountNo
	 *            银行卡账号
	 * @return k-v :ysfAccount - login password
	 */
	@Override
	public Map<String, String> findOtherAccountByBankAccount(String bankAccountNo) {
		if (ObjectUtils.isEmpty(bankAccountNo)) {
			return null;
		}
		Map<String, String> ysfLoginInfo = null;
		try {
			ysfLoginInfo = ysfAccountsLoginInfoCache.get(bankAccountNo);
			return ysfLoginInfo;
		} catch (Exception e) {
			log.error("根据银行卡账号:{},获取云闪付账号和登陆密码异常!异常信息：{}", bankAccountNo,e);
		}
		return ysfLoginInfo;
	}

	/** 根据银行卡账号查询云闪付账号和登陆密码 */
	private Map<String, String> findYsfLoginInfoByBankAccount(String bankAccountNo) {
		try {
			if (ObjectUtils.isEmpty(bankAccountNo)) {
				return null;
			}
			String sql = "select o.account_no,o.login_pwd from biz_other_account o,biz_other_account_bind oai,biz_account a where o.id=oai.other_account_id and oai.account_id=a.id and  a.account='"
					+ StringUtils.trim(bankAccountNo) + "' and a.status=1 and a.type=1 limit 1; ";
			Object res = entityManager.createNativeQuery(sql).getSingleResult();
			if (!ObjectUtils.isEmpty(res)) {
				Object[] ret = (Object[]) res;
				Map<String, String> map = new HashMap<>(2);
				map.put(ret[0].toString(), ret[1].toString());
				return map;
			}

		} catch (Exception e) {
			log.debug("缓存初始化或者刷新异常:", e);
			e.printStackTrace();
		}
		return null;
	}

	/** 更新云闪付密码 */
	@Override
	public void updateYSFPwd(UpdateYSFPWDInputDTO inputDTO) {

	}

	/** 更新云闪付基本信息 */
	@Override
	public void updateYSFBasicInfo(UpdateYSFBasicInfoInputDTO infoInputDTO) {

	}

	/** 更新云闪付绑定的银行卡信息 */
	@Override
	public void updateYSFBindAccount(UpdateYSFBindAccountInputDTO inputDTO) {

	}

	/**
	 * 描述:收到二维码 二维码以字符串的格式而不是url
	 *
	 * @param responseEntity
	 */
	@Override
	public void receiveCabanaSendQRs(YSFQResponseEntity responseEntity) {
		try {
			byte[] decodeBase64 = Base64.getDecoder().decode(responseEntity.getQrStreams());
			String path = System.getProperty("user.dir") + uploadPathNew + File.separator + "qr";
			File dir = new File(path);
			if (!dir.exists()) {
				dir.mkdirs();
			}
			path += File.separator + responseEntity.getAmount() + ".png";
			FileOutputStream outputStream = new FileOutputStream(path);
			outputStream.write(decodeBase64, 0, decodeBase64.length);
			outputStream.flush();
			outputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 描述：请求生成二维码
	 *
	 * @param requestEntity
	 * @return
	 */
	@Override
	public ResponseData<?> call4GenerateQRs(YSFQRrequestEntity requestEntity) {
		if (Objects.isNull(requestEntity)) {
			log.info("请求二维码参数不能为空 ");
			return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数为空.");
		}
		if (Objects.isNull(requestEntity.getHandicapId())) {
			return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "盘口不能为空.");
		}
		if (Objects.isNull(requestEntity.getBankAccount())) {
			return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "银行账号不能为空.");
		}
		if (Objects.isNull(requestEntity.getExpectAmounts())) {
			return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请求金额不能为空.");
		}
		if (Objects.isNull(requestEntity.getLoginPWD())) {
			return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "登陆密码不能为空.");
		}
		if (Objects.isNull(requestEntity.getYsfAccount())) {
			return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "云闪付账号不能为空.");
		}

		try {
			BizOtherAccountEntity entity = findByAccountNo(requestEntity.getYsfAccount());
			if (Objects.isNull(entity)) {
				return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "云闪付账号不存在.");
			}
			AccountBaseInfo account = accountService.getFromCacheByHandicapIdAndAccount(entity.getHandicapId(),
					requestEntity.getBankAccount());
			BizAccount account1 = null;
			if (Objects.isNull(account)) {
				account1 = accountService.findByAccountNo(requestEntity.getBankAccount(),
						InBankSubType.IN_BANK_YSF.getSubType());
				if (Objects.isNull(account1)) {
					return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "收款银行卡账号不存在.");
				}
			}
			Integer type = Objects.isNull(account) ? account1.getType() : account.getType();
			Integer status = Objects.isNull(account) ? account1.getStatus() : account.getStatus();
			Integer subType = Objects.isNull(account) ? account1.getSubType() : account.getSubType();
			if (Objects.isNull(type) || !AccountType.InBank.getTypeId().equals(type)) {
				return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "银行卡不属于入款卡.");
			}
			if (Objects.isNull(subType) || !InBankSubType.IN_BANK_YSF.getSubType().equals(subType)) {
				return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "银行卡不属于云闪付收款卡.");
			}
			if (Objects.isNull(status) || !AccountStatus.Normal.getStatus().equals(status)) {
				return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "银行卡状态不是在用.");
			}
			ArrayList accountIds = new ArrayList();
			accountIds.add(!ObjectUtils.isEmpty(account) ? account.getId() : account1.getId());
			// 检查app状态
			List<String> appStatus = allocateTransService.onLineAcc(accountIds);
			if (CollectionUtils.isEmpty(appStatus)) {
				return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "APP不在线请核实.");
			}

			ResponseData<?>[] ret = new ResponseData<?>[1];
			HttpClientCabana.getInstance().getCabanaService().call4GenerateQRs(
					cabanaService.buildReqBody(cabanasalt, new TreeMap<String, String>(Comparator.naturalOrder()) {
						{
							put("handicapId", requestEntity.getHandicapId().toString());
							put("bankAccount", requestEntity.getBankAccount());
							put("ysfAccount", requestEntity.getYsfAccount());
							put("expectAmounts", requestEntity.getExpectAmounts());
							// put("loginPWD",
							// FundTransferEncrypter.encryptCabana(requestEntity.getLoginPWD()));
						}
					})).subscribe(d -> {
				if (d.getStatus() != GeneralResponseData.ResponseStatus.SUCCESS.getValue()) {
					log.info(String.format("请求生成二维码失败%s,参数%s"), d.getMessage(), requestEntity);
				}
				ret[0] = d;
			}, e -> {
				log.error(String.format("请求生成二维码异常%s,参数%s"), e.getLocalizedMessage(), requestEntity);
				ret[0] = new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请求生成二维码异常");
			});
			log.debug("下发app生成二维码时返回的内容为：{}",ObjectMapperUtils.serialize(ret[0]));
			return ret[0];
		} catch (Exception e) {
			log.error(String.format("请求生成二维码异常,参数%s", requestEntity));
			log.error("请求生成二维码异常:", e);
			return new ResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请求生成二维码异常");
		}
	}

	/**
	 * 描述:条件分页查询云闪付账号信息
	 *
	 * @param inputDTO
	 * @return
	 */
	@Override
	public Page<BizOtherAccountEntity> findPageByCriteria(QueryYSFInputDTO inputDTO) {
		Sort.Direction direction = (Objects.isNull(inputDTO.getOrderSort()) || "desc".equals(inputDTO.getOrderSort()))
				? Sort.Direction.DESC
				: Sort.Direction.ASC;
		String orderField = Objects.isNull(inputDTO.getOrderField()) ? "createTime" : inputDTO.getOrderField();
		PageRequest pageRequest = new PageRequest(inputDTO.getPageNo(), inputDTO.getPageSize(), direction, orderField);
		Page<BizOtherAccountEntity> entityPage = repository.findAll((root, query, criteriaBuilder) -> {
			List<Predicate> predicateList = new ArrayList<>();
			if (Objects.nonNull(inputDTO.getId())) {
				Predicate p = criteriaBuilder.equal(root.get("id").as(Integer.class), inputDTO.getId());
				predicateList.add(p);
			}
			if (Objects.nonNull(inputDTO.getHandicapId())) {
				Predicate p = criteriaBuilder.equal(root.get("handicapId").as(Integer.class), inputDTO.getHandicapId());
				predicateList.add(p);
			}
			if (StringUtils.isNotBlank(inputDTO.getAccountNo())) {
				Predicate p = criteriaBuilder.equal(root.get("accountNo").as(String.class), inputDTO.getAccountNo());
				predicateList.add(p);
			}
			if (StringUtils.isNotBlank(inputDTO.getOwner())) {
				Predicate p = criteriaBuilder.equal(root.get("owner").as(String.class), inputDTO.getOwner());
				predicateList.add(p);
			}
			if (null != inputDTO.getOwnType() && inputDTO.getOwnType().length > 0) {
				if (inputDTO.getOwnType().length == 1) {
					Predicate p = criteriaBuilder.equal(root.get("ownType").as(Byte.class), inputDTO.getOwnType()[0]);
					predicateList.add(p);
				} else {
					CriteriaBuilder.In in = criteriaBuilder.in(root.get("ownType").as(Byte.class));
					for (int i = 0, len = inputDTO.getOwnType().length; i < len; i++) {
						in.value(inputDTO.getOwnType()[i]);
					}
					predicateList.add(in);
				}
			}
			if (null != inputDTO.getStatus() && inputDTO.getStatus().length > 0) {
				if (inputDTO.getStatus().length == 1) {
					Predicate p = criteriaBuilder.equal(root.get("status").as(Byte.class), inputDTO.getStatus()[0]);
					predicateList.add(p);
				} else {
					CriteriaBuilder.In in = criteriaBuilder.in(root.get("status").as(Byte.class));
					for (int i = 0, len = inputDTO.getStatus().length; i < len; i++) {
						in.value(inputDTO.getStatus()[i]);
					}
					predicateList.add(in);
				}

			}
			if (null != inputDTO.getUpdateTime()) {
				Predicate p = criteriaBuilder.equal(root.get("updateTime").as(Timestamp.class),
						inputDTO.getUpdateTime());
				predicateList.add(p);
			}
			Predicate[] p = new Predicate[predicateList.size()];
			query.where(criteriaBuilder.and(predicateList.toArray(p)));
			return query.getRestriction();
		}, pageRequest);
		return entityPage;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public CreateYSFAccountOutputDTO add(AddYSFAccountInputDTO ysf, List<InAccountBindedYSFInputDTO> dto) {
		try {
			if (Objects.isNull(ysf)) {
				return null;
			}
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			CreateYSFAccountOutputDTO outputDTO = new CreateYSFAccountOutputDTO();
			BizOtherAccountEntity entity = new BizOtherAccountEntity();
			entity.setAccountNo(ysf.getAccountNo());
			entity.setCreater(Objects.nonNull(operator) ? operator.getUid() : "SYSTEM");
			entity.setCreateTime(new Timestamp(System.currentTimeMillis()));
			entity.setUpdateTime(entity.getCreateTime());
			entity.setHandicapId(ysf.getHandicapId());
			entity.setOwner(ysf.getOwner());
			entity.setLoginPWD(FundTransferEncrypter.encryptDb(ysf.getLoginPWD()));
			entity.setPayPWD(FundTransferEncrypter.encryptDb(ysf.getPayPWD()));
			entity.setType(OtherAccountType.YSF.getType().byteValue());
			entity.setOwnType(
					ysf.getOwnType() == null ? OtherAccountOwnType.OWN.getOwnType().byteValue() : ysf.getOwnType());
			entity.setStatus(
					ysf.getStatus() == null ? OtherAccountStatus.STOP.getStatus().byteValue() : ysf.getStatus());
			String remark = addRemark(entity.getRemark(),
					StringUtils.isBlank(ysf.getRemark()) ? "新增" : ysf.getRemark());
			entity.setRemark(remark);
			entity = repository.saveAndFlush(entity);
			if (!CollectionUtils.isEmpty(dto)) {
				List<BizAccount> accountList = accountService.createIncomeAccount(dto);
				bind(entity, accountList.stream().map(p -> p.getId()).collect(Collectors.toList()),
						StringUtils.isBlank(ysf.getRemark()) ? "新增并绑定" : "");
				outputDTO.setBizAccounts(accountList);
			}
			outputDTO.setEntity(entity);
			return outputDTO;
		} catch (Exception e) {
			log.error("新增失败:", e);
		}
		return null;
	}

	@Override
	public BizOtherAccountEntity findByAccountNo(String accountNo) {
		return repository.findByAccountNo(accountNo);
	}

	@Override
	public BizOtherAccountEntity findById(Integer id) {
		return repository.findById2(id);
	}

	@Override
	public BizOtherAccountEntity findByIdForUpdate(Integer id) {
		return repository.getByIdForLock(id);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public CreateYSFAccountOutputDTO update(AddYSFAccountInputDTO ysf, BizOtherAccountEntity entity,
											List<InAccountBindedYSFInputDTO> oldAccountList, List<InAccountBindedYSFInputDTO> newAccountList) {
		try {
			if (Objects.isNull(ysf) || Objects.isNull(entity)) {
				return null;
			}
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			bindingService.deleteByAccountId(entity.getId());
			entity.setOwner(ysf.getOwner());
			entity.setLoginPWD(FundTransferEncrypter.encryptDb(ysf.getLoginPWD()));
			entity.setPayPWD(FundTransferEncrypter.encryptDb(ysf.getPayPWD()));
			entity.setHandicapId(ysf.getHandicapId());
			entity.setOwnType(ysf.getOwnType());
			entity.setStatus(ysf.getStatus());
			String remark = addRemark(entity.getRemark(), ysf.getRemark());
			entity.setRemark(remark);
			entity.setOperator(operator == null ? "SYSTEM" : operator.getUid());
			entity.setUpdateTime(new Timestamp(System.currentTimeMillis()));
			entity = repository.saveAndFlush(entity);
			List<Integer> bindAccountIds = new ArrayList<>();
			List<BizAccount> addAccounts = new ArrayList<>();
			if (!CollectionUtils.isEmpty(newAccountList)) {
				addAccounts = accountService.createIncomeAccount(newAccountList);
				bindAccountIds.addAll(addAccounts.stream().map(p -> p.getId()).collect(Collectors.toList()));
			}
			if (!CollectionUtils.isEmpty(oldAccountList)) {
				// 可能会更改了盘口和层级 先把盘口和层级绑定关系更新
				oldAccountList.stream().forEach(p -> {
					// 盘口校验
					BizAccount bizAccount = accountService.getById(p.getId());
					if (!ObjectUtils.isEmpty(bizAccount) && !ysf.getHandicapId().equals(bizAccount.getHandicapId())) {
						// 更新盘口
						bizAccount.setHandicapId(ysf.getHandicapId());
						accountService.save(bizAccount);
					}
					List<BizAccountLevel> accountLevels = accountLevelRepository.findByAccountId4Delete(p.getId());
					if (!CollectionUtils.isEmpty(accountLevels)) {
						// 删除旧的账号层级关系
						accountLevelRepository.deleteInBatch(accountLevels);
					}
					if (!ObjectUtils.isEmpty(p.getLevelIds())) {
						// 绑定新的账号层级关系
						List<BizAccountLevel> newAccountLevel = new ArrayList<>();
						for (Integer levelId : p.getLevelIds()) {
							BizAccountLevel bizAccountLevel = new BizAccountLevel();
							bizAccountLevel.setAccountId(p.getId());
							bizAccountLevel.setLevelId(levelId);
							newAccountLevel.add(bizAccountLevel);
						}
						accountLevelRepository.saveAll(newAccountLevel);
					}
					// 绑定云闪付银行卡id
					bindAccountIds.add(p.getId());
				});
			}
			if (!CollectionUtils.isEmpty(bindAccountIds)) {
				// 绑定云闪付和银行卡
				bind(entity, bindAccountIds, ysf.getRemark());
			}
			CreateYSFAccountOutputDTO res = new CreateYSFAccountOutputDTO();
			res.setEntity(entity);
			res.setBizAccounts(addAccounts);
			return res;
		} catch (Exception e) {
			log.error("更新云闪付账号信息失败:", e);
		}
		return null;
	}

	/**
	 * 描述:如果备注大于512个字符则去除旧的一部分备注
	 *
	 * @param oldRemark
	 * @param newRemark
	 * @return
	 */
	@Override
	public String addRemark(String oldRemark, String newRemark) {
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (StringUtils.isBlank(newRemark)) {
			return oldRemark;
		}
		String remark = CommonUtils.genRemark(oldRemark, newRemark, new Date(),
				Objects.nonNull(operator) ? operator.getUid() : "SYSTEM");
		int remarkLen = remark.length();
		if (remarkLen > 512) {
			remark = remark.substring(remarkLen - 512);
		}
		return remark;
	}

	/** 更新云闪付账号状态 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateYSFStatus(Integer id, Byte status, String remark) {
		try {
			if (Objects.isNull(id)) {
				return;
			}
			BizOtherAccountEntity entity = findByIdForUpdate(id);
			if (Objects.isNull(entity)) {
				return;
			}
			entity.setStatus(status);
			remark = addRemark(entity.getRemark(), remark);
			entity.setRemark(remark);
			repository.saveAndFlush(entity);
		} catch (Exception e) {
			log.error("更新云闪付账号状态失败:", e);
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public int deleteYSF(Integer id) {
		try {
			log.info("删除参数:{}", id);
			BizOtherAccountEntity entity = findByIdForUpdate(id);
			// FIXME 云闪付账号删除之后 绑定的银行卡怎么处理?
			List<BizOtherAccountBindEntity> bindEntityList = bindingService.findByOtherAccountId(entity.getId());
			if (!ObjectUtils.isEmpty(bindEntityList)) {
				if (bindEntityList.size() > 1) {
					ArrayList list = new ArrayList() {
						{
							addAll(bindEntityList.stream().map(p -> p.getAccountId()).collect(Collectors.toList()));
						}
					};
					List<BizAccount> accountList = accountService.findByIds(list);
					if (!CollectionUtils.isEmpty(accountList)) {
						accountList.stream().forEach(p -> p.setStatus(AccountStatus.StopTemp.getStatus()));
						accountService.saveIterable(accountList);
						accountList.stream().forEach(p -> accountService.broadCast(p));
					}
				} else {
					BizAccount account = accountService.getById(bindEntityList.get(0).getAccountId());
					if (Objects.nonNull(account)) {
						account.setStatus(AccountStatus.StopTemp.getStatus());
						accountService.save(account);
						accountService.broadCast(account);
					}
				}

			}
			// 删除绑定关系
			bindingService.deleteByAccountId(entity.getId());
			// FIXME 云闪付账号可以直接删除还是逻辑删除?
			repository.delete(entity);
			return 1;
		} catch (Exception e) {
			log.error("删除失败:", e);
		}
		return 0;
	}

	/**
	 * 描述:绑定银行卡
	 *
	 * @param entity
	 *            云闪付实体信息
	 * @param accountIdList
	 *            需要绑定的银行卡账号id集合
	 * @return
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void bind(BizOtherAccountEntity entity, List<Integer> accountIdList, String newRemark) {
		try {
			if (CollectionUtils.isEmpty(accountIdList)) {
				return;
			}

			List<BizOtherAccountBindEntity> bizAccountBindingList = new ArrayList<>();
			accountIdList.stream().forEach(p -> {
				BizOtherAccountBindEntity bizAccountBinding = new BizOtherAccountBindEntity();
				bizAccountBinding.setOtherAccountId(entity.getId());
				bizAccountBinding.setAccountId(p);
				bizAccountBindingList.add(bizAccountBinding);

			});
			bindingRepository.saveAll(bizAccountBindingList);
			if (StringUtils.isNotBlank(newRemark)) {
				entity.setRemark(addRemark(entity.getRemark(), newRemark));
				repository.saveAndFlush(entity);
			}
		} catch (Exception e) {
			log.error("绑定失败:", e);
		}
	}

	/**
	 * 获取收款银行卡二维码存储缓存key
	 *
	 * @param bankAccount
	 * @param amount
	 * @return "zrand:当前日期:bankAccount:amount（整数部分）"
	 */
	public String getCurrentBankQrCacheKey(String bankAccount, Object amount) {
		return String.format(BANK_ACCOUNT_RAND_DATE_QR, sdf.format(new Date()), bankAccount,
				amount == null ? "" : new BigDecimal(amount.toString()).intValue());
	}

	/**
	 * <pre>
	 * 可用银行账号排序
	 * 最近收款时间倒序
	 * 从本地缓存中获取银行卡最近使用的时间，通过时间大小比较，时间小的排在前面
	 * </pre>
	 *
	 * @param handicap
	 * @param availableAccount
	 * @return
	 */
	public List<String> availableBankAccountSort(Integer handicap, List<String> availableAccount) {
		/**
		 * 实现逻辑： 1、使用本地缓存记录每一个账号最近使用的时间 2、取使用时间升序排列，未使用的排列在前
		 */
		List<String> sortedList = availableAccount.stream().sorted(new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				Long t1 = ySFLocalCacheUtil.getAccountLastUseTime(o1);
				Long t2 = ySFLocalCacheUtil.getAccountLastUseTime(o2);
				// 未使用的优先，时间小的优先
				return t1 == null && t2 != null ? -1
						: t1 != null && t2 == null ? 1 : t1.equals(t2) ? 0 : t1.compareTo(t2) > 0 ? 1 : -1;
			}
		}).collect(Collectors.toList());
		return sortedList;
	}

	/**
	 * 获取在线的收款卡号
	 *
	 * @param handicap
	 *            盘口号
	 * @param accountIds
	 *            通道收款银行卡对应id
	 * @return
	 */
	private List<String> getOnlineBankAccountList(Integer handicap, List<Integer> accountIds) {
		List<String> result = new ArrayList<String>();
		if (!CollectionUtils.isEmpty(accountIds)) {
			// 检查app状态
			List<String> onlineBankAccountList = allocateTransService.onLineAcc(accountIds);
			if (!CollectionUtils.isEmpty(onlineBankAccountList)) {
				result.addAll(onlineBankAccountList);
			}
		}
		return result;
	}

	/**
	 * 获取可用收款账号列表
	 *
	 * <pre>
	 *1、是否系统中的收款账号
	 *2、兼职当前可用信用额度 必须或等于大于 请求金额
	 *3、设备是否在线
	 * </pre>
	 *
	 * @param handicap
	 *            盘口号
	 * @param requestMoney
	 *            请求金额（用于判断可用信用额度）
	 * @param accountList
	 *            通道绑定的收款银行卡账号列表
	 * @return
	 */
	public List<String> getAvailableBankAccountList(Integer handicap, Integer requestMoney, List<String> accountList) {
		// mark blake 2019-04-13 增加云闪付银行卡是否在初始化
		// 从缓存中获取正在初始化的银行卡号
		Set<Object> ysfInitCards = redisService.getStringRedisTemplate()
				.boundHashOps(RedisKeys.ACCOUNT_DISABLE_QUICKPAY).keys();
		//需求 7005 .判处对模式的判断
		//Map<Object, Object> model = redisService.getStringRedisTemplate().boundHashOps(RedisKeys.ACC_NEW_MODEL).entries();
		//if (!CollectionUtils.isEmpty(model)) {
		//	log.debug("移除当前为出款模式的卡");
		//	List<String> ids = model.entrySet().stream()
		//			.filter(p -> Objects.nonNull(p.getKey()) && Objects.nonNull(p.getValue())
		//					&& Objects.equals(p.getValue(), "2"))
		//			.map(p -> p.getKey().toString()).collect(Collectors.toList());
		//	if (!CollectionUtils.isEmpty(ids)) {
		//		accountList.removeAll(ids);
		//	}
		//}
		log.debug("从缓存中获取正在初始化的云闪付银行卡号，正在初始化的银行卡号有：{}", Arrays.toString(ysfInitCards.toArray()));
		if (!CollectionUtils.isEmpty(ysfInitCards)) {
			log.debug("移除正在初始化的云闪付银行卡号", Arrays.toString(ysfInitCards.toArray()));
			List<String> ysfInitCardsList = new ArrayList<String>();
			ysfInitCards.stream().forEach(t -> {
				ysfInitCardsList.add(t.toString());
			});
			accountList.removeAll(ysfInitCardsList);
		}
		
		log.debug("移除正在初始化的云闪付银行卡后，传递进来的银行卡中剩余：{}", Arrays.toString(accountList.toArray()));
		if (CollectionUtils.isEmpty(accountList)) {
			log.error("移除正在初始化的云闪付银行卡后，传递进来的银行卡号中无可用卡");
			return new ArrayList<>();
		}
		List<Integer> accountIds = new ArrayList<>();
		Set<Integer> failure = allocateTransService.buildFailureTrans();
		// 通过银行卡账号 查询账号信息并比较当前可用信用额度
		for (String bankAccount : accountList) {
			AccountBaseInfo account = accountService.getFromCacheByHandicapIdAndAccount(handicap, bankAccount);
			if (ObjectUtils.isEmpty(account)) {
				log.error("未能通过{}查询到AccountBaseInfo", bankAccount);
				continue;
			}
			if(!Objects.equals(account.getStatus(),AccountStatus.Normal.getStatus())){
				log.error("银行卡不在在用状态 {}", bankAccount);
				continue;
			}
			if(failure != null && failure.contains(account.getId())){
				log.error("银行卡连续多次转账失败 {}", bankAccount);
				continue;
			}
			// 取当前剩余额度
			Integer currentCredit = accountChangeService.currCredits(account);
			if (currentCredit == null || currentCredit < requestMoney) {
				log.error("获取到{}当前可用信用额度为{},不满足入款请求金额{}", bankAccount, currentCredit, requestMoney);
				continue;
			}
			log.info("{}当前信用额度{}，满足入款请求金额{}", bankAccount, currentCredit, requestMoney);
			accountIds.add(account.getId());
		}
		if (accountIds.size()==0) {
			log.error("暂无满足条件的收款银行卡");
			return new ArrayList<>();
		}
		// 获取在线的收款银行卡列表
		List<String> onlineList = getOnlineBankAccountList(handicap, accountIds);
		log.info("获取到的在线的云闪付银行卡号有：{}", Arrays.toString(onlineList.toArray()));
		return onlineList;
	}

	/**
	 * 保存二维码 <br>
	 * 用于云闪付app生成二维码后保存二维码<br>
	 * 参数：
	 *
	 * <pre>
	 * handicap 盘口号，必填
	 * device 设备，必填
	 * account 银行卡号，必填
	 * money 金额（最多2位小数），必填
	 * qrContent 二维码内容，必填
	 * genDate app生成二维码日期，如 2019-02-18，必填
	 * </pre>
	 *
	 * @author blake
	 * @param qrCode
	 */
	@Override
	public Boolean saveQrCode(YSFQrCodeEntity qrCode) {
		Assert.isTrue(!StringUtils.isEmpty(qrCode.getYsfAccount()), "ysfAccount不能为空");
		Assert.isTrue(!StringUtils.isEmpty(qrCode.getBindedBankAccount()), "account不能为空");
		Assert.isTrue(qrCode.getMoney() != null && qrCode.getMoney().compareTo(BigDecimal.ZERO) > 0, "money不能为空并且大于0");
		Assert.isTrue(!StringUtils.isEmpty(qrCode.getQrContent()), "qrContent不能为空");
		Assert.isTrue(qrCode.getGenDate() != null, "genDate不能为空");
		/**
		 * 实现：
		 *
		 * <pre>
		 * 1、根据设备号、收款账号、生成日期、金额 校验是否重复，如果重复，不再保存，否则保存
		 * 2、保存时，除了表中存一份之外，redis中也存一份。以使用 日期:收款银行卡号:金额做外键的hash存储，key为二维码金额，value为二维码内容
		 * 3、在缓存中设置二维码使用时间。使用时间为 System.currentTimestamp() - ySFLocalCacheUtil.getYSFQrCodeLockTime()
		 * </pre>
		 */
		Boolean result = null;
		try {
			// 是否对app传递过来的二维码内容进行解码
			if (ySFLocalCacheUtil.decodeQrContent()) {
				log.debug("解码二维码内容");
				String decodeResult = YSFQrCodeBase64StrReader.getQrContent(qrCode.getQrContent());
				log.debug("解码所得的二维码内容为{}", decodeResult);
				qrCode.setQrContent(decodeResult);
			}
			// 发布消息
			redisService.convertAndSend(RedisTopics.YSF_QR_CODE_MSG, ObjectMapperUtils.serialize(qrCode));
			// 保存到缓存中
			this.qrCode2Cache(qrCode);
			result = true;
		} catch (Exception e) {
			log.error("将云闪付app生成的二维码保存到缓存中时异常", e);
			result = false;
		}
		return result;
	}

	/**
	 * 将二维码信息保存到缓存中
	 */
	private void qrCode2Cache(YSFQrCodeEntity qrCode) {
		log.debug("准备将银行卡{} 金额为 {}的二维码保存到缓存中", qrCode.getBindedBankAccount(), qrCode.getMoney());
		Calendar expireTime = Calendar.getInstance();
		Calendar genDate = Calendar.getInstance();
		genDate.setTime(new Timestamp(qrCode.getGenDate()));
		// 如果生成的二维码日期与现在服务器时间不是同一天，不保存到缓存中
		if (genDate.get(Calendar.YEAR) != expireTime.get(Calendar.YEAR)
				|| genDate.get(Calendar.DAY_OF_YEAR) < expireTime.get(Calendar.DAY_OF_YEAR)) {
			log.error("二维码生成时间{}与服务器时间{}不是同一天，不保存", qrCode.getGenDate(), expireTime.getTime());
			throw new RuntimeException("app生成二维码的日期与服务器当前日期不是同一天，不进行保存");
		}

		StringRedisTemplate jedis = redisService.getYsfStringRedisTemplate();
		if (jedis != null) {
			// 缓存外键,日期_银行卡号_金额（整数部分）
			String key = getCurrentBankQrCacheKey(qrCode.getBindedBankAccount(), qrCode.getMoney().intValue());
			String moneyKey = InAccountRandUtil.moneyFormat(qrCode.getMoney());
			boolean exitFlag = jedis.boundHashOps(key).hasKey(moneyKey);
			if (!exitFlag) {
				log.debug("将 key ={},value={}添加到{}中", moneyKey, qrCode.getQrContent(), key);
				jedis.boundHashOps(key).put(moneyKey, qrCode.getQrContent());
				// 设置 key 的过期时间
				// 因为云闪付的二维码第二天就会失效。
				// 所以设置时间缓存失效时间为当日 23:59:59.999
				expireTime.set(Calendar.HOUR_OF_DAY, 23);
				expireTime.set(Calendar.MINUTE, 59);
				expireTime.set(Calendar.SECOND, 59);
				expireTime.set(Calendar.MILLISECOND, 999);
				log.debug("设置{}的过期时间为{}", key, expireTime.getTime());
				jedis.boundHashOps(key).expireAt(expireTime.getTime());
			} else {
				log.error("缓存key={}已经存在了金额为{}的信息，不再更新", key, moneyKey);
			}
			// 将新产生的二维码随机金额添加到收款银行卡的使用时间 zset 中。
			// score 为 当前时间 减去 ySFLocalCacheUtil.getYSFQrCodeLockTime()
			String lockKey = InAccountRandUtil.getCurrentBankQrCacheLockKey(qrCode.getBindedBankAccount());
			jedis.boundZSetOps(lockKey).removeRangeByScore(0,ySFLocalCacheUtil.getYSFQrCodeLockTime());
			// 分布式锁
			JedisLock lock = new JedisLock(jedis,
					String.format(InAccountRandUtil.BANK_ACCOUNT_RAND_DISTRIBUTED_LOCK,
							qrCode.getBindedBankAccount(), InAccountRandUtil.moneyFormat(qrCode.getMoney())),
					1000, 1000);
			try {
				if (lock.acquire()) {
					Double score = jedis.boundZSetOps(lockKey).score(moneyKey);
					if (score != null) {
						log.debug(String.format("%s中已经存在了%s，该收款银行卡%s曾经生成过该随机数的二维码", lockKey, moneyKey,
								qrCode.getBindedBankAccount()));
					} else {
						log.debug(String.format("设置银行卡%s缓存%s，增加值%s", qrCode.getBindedBankAccount(), lockKey,
								moneyKey));
						jedis.boundZSetOps(lockKey).add(moneyKey, System.currentTimeMillis() - ySFLocalCacheUtil.getYSFQrCodeLockTime());
						jedis.boundZSetOps(lockKey).expire(24, TimeUnit.HOURS);
					}
				} else {
					log.error("获取分布式锁失败");
				}
			} catch (InterruptedException e) {
				log.error(String.format("将收款银行卡%s生成的二维码金额%s设置到zset缓存%s时发生异常", qrCode.getBindedBankAccount(),
						qrCode.getMoney(), lockKey), e);
			} finally {
				lock.release();
			}
		}

	}

	/**
	 *
	 * @author blake
	 * @param param
	 *            云闪付账号
	 * @throws IOException
	 *
	 */
	@Override
	public YSFGenerateQRRequestDTO getYSFRandomMoney(YSFGenQrCodeReqeustDto param) throws IOException {
		Assert.isTrue(!StringUtils.isEmpty(param.getYsfAccount()), "云闪付账号不能为空");
		Assert.isTrue(!StringUtils.isEmpty(param.getBindedBankAccount()), "收款银行卡账号不能为空");
		/**
		 * 1、 根据云闪付账号，查询兼职信用额度 2、取配置生成二维码的金额 和 随机数生成数量 3、取小于兼职信用额度的金额取生成二维码
		 * 4、因每一个银行卡号每天每个金额生成的随机数不变，所以将生成过的随机金额放于缓存中
		 */
		BizAccount bizAccount = accountService.findByAccountNo(param.getBindedBankAccount(),
				InBankSubType.IN_BANK_YSF.getSubType());
		if (bizAccount == null) {
			throw new RuntimeException(String.format("未能查询到云闪付收款银行卡%s的信息", param.getBindedBankAccount()));
		}
		AccountBaseInfo target = accountService.getFromCacheByHandicapIdAndAccount(bizAccount.getHandicapId(),
				param.getBindedBankAccount());
		Integer creditInt = accountChangeService.margin(target);
		if (creditInt == 0) {
			log.error("getYSFRandomMoney 获取到 信用额度为 0 ，不生成二维码");
			YSFGenerateQRRequestDTO result = new YSFGenerateQRRequestDTO();
			result.setBindedBankAccount(param.getBindedBankAccount());
			result.setExpectAmounts(new Double[0]);
			result.setYsfAccount(param.getYsfAccount());
			return result;
		}

		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		Object cacheRandomListObject = redisService.getYsfStringRedisTemplate().boundHashOps(BANK_ACCOUNT_RAND_STR)
				.get(param.getBindedBankAccount());
		List<String> cacheRandomList = null;
		if (!ObjectUtils.isEmpty(cacheRandomListObject)) {
			cacheRandomList = mapper.readValue(cacheRandomListObject.toString(),
					mapper.getTypeFactory().constructCollectionType(ArrayList.class, String.class));
		}
		// 金额-随机数列表 分组
		Map<Integer, List<String>> randGroupMap = new HashMap<>();
		if (!CollectionUtils.isEmpty(cacheRandomList)) {
			cacheRandomList.forEach(t -> {
				String[] tmp = t.split("\\.");
				if (tmp.length > 1) {
					Integer tk = Integer.parseInt(tmp[0]);
					List<String> tmpMoneyList = randGroupMap.get(tk);
					if (tmpMoneyList == null) {
						tmpMoneyList = new ArrayList<>();
					}
					tmpMoneyList.add(t);
					randGroupMap.put(tk, tmpMoneyList);
				}
			});
		}

		// 系统配置的金额和生成二维码数量
		Map<Integer, Integer> configMoneyCount = ySFLocalCacheUtil.getMoneyMaxCount();
		// 正在下发的随机金额
		List<Double> moneyList = new ArrayList<Double>();
		// 如果数量与系统配置的不一致，使用系统配置的（有可能删，也有可能增加）
		for (Entry<Integer, Integer> o : configMoneyCount.entrySet()) {
			if (o.getKey() > creditInt) {
				continue;
			}
			List<String> cacheList = randGroupMap.get(o.getKey());
			if (cacheList == null) {
				cacheList = new ArrayList<>();
			}
			List<Double> tmpList = new ArrayList<>();
			if (cacheList.size() < o.getValue()) {
				// 缓存中数量比系统配置的数量少，添加随机数，最多99个
				while (cacheList.size() < 99 && cacheList.size() < o.getValue()) {
					for (int i = 0; i < str01_99.size() && cacheList.size() < o.getValue(); i++) {
						// 组成整数金额.随机小数
						String tmp = String.format("%s.%s", o.getKey(), str01_99.get(i));
						if (!cacheList.contains(tmp)) {
							cacheList.add(tmp);
						}
					}
				}
				cacheList.forEach(t -> {
					tmpList.add(new Double(t));
				});
				randGroupMap.put(o.getKey(), cacheList);
			} else if (cacheList.size() > o.getValue() && o.getValue() != 0) {
				// 缓存中数量比系统配置的数量多，取前 o.getValue()个
				cacheList.subList(0, o.getValue()).forEach(t -> {
					tmpList.add(new Double(t));
				});
			} else {
				cacheList.forEach(t -> {
					tmpList.add(new Double(t));
				});
			}
			if (tmpList.size() > 0) {
				moneyList.addAll(tmpList);
			}

		}

		Double[] expectAmounts = new Double[moneyList.size()];
		expectAmounts = moneyList.toArray(expectAmounts);
		YSFGenerateQRRequestDTO result = new YSFGenerateQRRequestDTO();
		result.setBindedBankAccount(param.getBindedBankAccount());
		result.setExpectAmounts(expectAmounts);
		result.setYsfAccount(param.getYsfAccount());

		// 放到缓存中,用于下次生成
		cacheRandomList = new ArrayList<>();
		for (List<String> o : randGroupMap.values()) {
			if (o.size() > 0) {
				cacheRandomList.addAll(o);
			}
		}
		redisService.getYsfStringRedisTemplate().boundHashOps(BANK_ACCOUNT_RAND_STR).put(result.getBindedBankAccount(),
				mapper.writeValueAsString(cacheRandomList));
		return result;
	}

	// 01-99的随机数
	private static List<String> str01_99 = new ArrayList<>();
	static {
		for (int i = 1; i < 100; i++) {
			str01_99.add(String.format("%s", i < 10 ? ("0" + i) : i));
		}
		Collections.shuffle(str01_99);
	}

	@Override
	public String checkBankAccountToBind(Map param) {
		if (CollectionUtils.isEmpty(param)) {
			return null;
		}
		String[] accounts = param.get("accounts").toString().split(",");
		Integer handicapId = (Integer) param.get("oid");
		StringBuilder sb = new StringBuilder();
		for (int i = 0, len = accounts.length; i < len; i++) {
			String account = accounts[i];
			AccountBaseInfo info = accountService.getFromCacheByHandicapIdAndAccount(handicapId, account);
			if (checkAccount(info)) {
				if (i == 0 || i == len - 1) {
					sb.append(account);
				} else {
					sb.append(account).append(",");
				}
			}
		}
		return sb.toString();
	}

	private boolean checkAccount(AccountBaseInfo info) {
		return Objects.nonNull(info) && Objects.nonNull(info.getType())
				&& AccountType.InBank.getTypeId().equals(info.getType()) && Objects.nonNull(info.getSubType())
				&& InBankSubType.IN_BANK_YSF.getSubType().equals(info.getSubType())
				&& (AccountStatus.Normal.getStatus().equals(info.getStatus())
				|| AccountStatus.Enabled.getStatus().equals(info.getStatus())
				|| AccountStatus.Activated.getStatus().equals(info.getStatus()));
	}

	/**
	 * 回收随机数 <br>
	 * 当入款单订单确认或者取消时，释放随机数
	 */
	@Override
	public void recycleRandNum(String bankAccount, BigDecimal orderMoney) {
		inAccountRandUtil.recycleRandNum(bankAccount, orderMoney);
	}

	/**
	 * 锁定随机数 <br>
	 * 当收到流水，但是没有对应的入款单时，锁定该随机数，不允许使用 <br>
	 * 锁定时间，与平台银行入款那边一直，12小时
	 */
	@Override
	public void lockRandNum(String bankAccount, BigDecimal orderMoney) {
		/**
		 * 1、使用 redis 的 hash 表来锁住随机金额 2、zset 以 bankAccount为外键，随机金额为key ,锁定时间为当前时间+12小时
		 */
		inAccountRandUtil.lockRandNum(bankAccount, orderMoney);
	}

	@Override
	public List<Integer> getYSFAllowMoney() {
		List<Integer> result = new ArrayList<Integer>();
		Map<Integer, Integer> map = ySFLocalCacheUtil.getMoneyMaxCount();
		if (map != null) {
			map.keySet().forEach(t -> {
				result.add(t);
			});
			Collections.sort(result);
		}
		return result;
	}

	/**
	 * 根据银行卡账号查询收款银行卡账号的二维码
	 */
	@Override
	public List<YSFQrCodeQueryDto> queryByBankAccount(String bankAccount) {
		List<YSFQrCodeQueryDto> result = new ArrayList<YSFQrCodeQueryDto>();
		if (!StringUtils.isEmpty(bankAccount)) {
			StringRedisTemplate jedis = redisService.getYsfStringRedisTemplate();
			if (jedis != null) {
				for (Integer amount : getYSFAllowMoney()) {
					String key = getCurrentBankQrCacheKey(bankAccount, amount);
					Map<Object, Object> amountQrMap = jedis.opsForHash().entries(key);
					if (!ObjectUtils.isEmpty(amountQrMap)) {
						for (Entry<Object, Object> qrCode : amountQrMap.entrySet()) {
							YSFQrCodeQueryDto qr = new YSFQrCodeQueryDto();
							if(qrCode.getKey()!=null) {
								qr.setAmount(new BigDecimal(qrCode.getKey().toString()));
							}
							qr.setQrContent((String)qrCode.getValue());
							result.add(qr);
						}
					}
				}
				// 二维码金额升序排列
				result.sort(new Comparator<YSFQrCodeQueryDto>() {
					@Override
					public int compare(YSFQrCodeQueryDto o1, YSFQrCodeQueryDto o2) {
						return o1.getAmount().compareTo(o2.getAmount()) > 0 ? 1 : -1;
					}
				});
			}
		}

		return result;
	}

	/**
	 * 银联云闪付获取支付二维码 <br>
	 * 1、通过银行卡号获取设备 <br>
	 * 2、判断设备是否可用 <br>
	 * 3、将可用设备按最近收款时间倒序排列 <br>
	 * 4、根据设备获取设备当日生成的二维码,二维码不存在时下发app生成<br>
	 *
	 * @param requestDto
	 * @return
	 */
	private CardForPayOutputDTO getQrCodeByReal(CardForPayInputDTO inputDTO, BizHandicap handicap) {
		Assert.isTrue(!ObjectUtils.isEmpty(handicap) && !ObjectUtils.isEmpty(handicap.getId()), "盘口号不能为空");
		Assert.isTrue(!ObjectUtils.isEmpty(inputDTO) && !ObjectUtils.isEmpty(inputDTO.getPocIdMapCardNosCol())
				&& !CollectionUtils.isEmpty(inputDTO.getPocIdMapCardNosCol().values()), "收款账号不能为空");

		String inBankAccountListStr = null;
		String ocIdStr = null;
		for (Entry<String, String> o : inputDTO.getPocIdMapCardNosCol().entrySet()) {
			ocIdStr = o.getKey();
			inBankAccountListStr = o.getValue();
			break;
		}
		Assert.isTrue(!StringUtils.isEmpty(ocIdStr), "通道id不能为空");
		Assert.isTrue(!StringUtils.isEmpty(inBankAccountListStr), "收款账号不能为空");
		Assert.isTrue(inputDTO.getAmount() != null, "申请入款金额不能为空");
		YSFRequestDto requestDto = new YSFRequestDto();
		List<String> reqAccountList = new ArrayList<String>();
		reqAccountList.addAll(Arrays.asList(inBankAccountListStr.split(",")));
		requestDto.setAccountList(reqAccountList);
		requestDto.setAmount(inputDTO.getAmount().intValue());
		requestDto.setHandicap(handicap.getId());
		requestDto.setHandicapCode(handicap.getCode());
		requestDto.setUserName(inputDTO.getUserName());
		YSFResponseDto response = this.getQrCodeByReal(requestDto);
		CardForPayOutputDTO result = null;
		if (response != null) {
			result = new CardForPayOutputDTO();
			AccountBaseInfo account = accountService.getFromCacheByHandicapIdAndAccount(handicap.getId(),
					response.getAccount());
			if (account != null) {
				List<BankInfo> accountList = new ArrayList<>();
				BankInfo e = new BankInfo();
				e.setBankName(account.getBankName());
				e.setBankType(account.getBankType());
				e.setCardNo(response.getAccount());
				e.setCity(account.getCity());
				e.setOwner(account.getOwner());
				e.setProvince(account.getProvince());
				e.setQrCode(response.getQrContent());
				accountList.add(e);
				result.setAccountList(accountList);
			}
			result.setAmount(inputDTO.getAmount());
			Number dou = new BigDecimal(response.getAmount());
			result.setFinalAmount(dou);
			result.setOid(Integer.parseInt(response.getHandicapCode()));
			if (!StringUtils.isEmpty(ocIdStr)) {
				if (ocIdStr.contains("+")) {
					ocIdStr = ocIdStr.replaceAll("\\+", "");
				}
				result.setPocId(Long.parseLong(ocIdStr));
			}
			result.setType(InBankSubType.IN_BANK_YSF.getSubType().byteValue());
		}else {
			log.error("getQrCodeByReal 获取的返回值为空，参数：{}",ObjectMapperUtils.serialize(inputDTO));
		}
		return result;
	}

	/**
	 * 银联云闪付获取支付二维码 <br>
	 * 1、通过银行卡号获取设备 <br>
	 * 2、判断设备是否可用 <br>
	 * 3、将可用设备按最近收款时间倒序排列 <br>
	 * 4、根据设备获取设备当日生成的二维码,二维码不存在时下发app生成<br>
	 *
	 * @param requestDto
	 * @return
	 */
	private YSFResponseDto getQrCodeByReal(YSFRequestDto requestDto) {
		Assert.isTrue(requestDto.getHandicap() != null, "盘口号不能为空");
		Assert.isTrue(!CollectionUtils.isEmpty(requestDto.getAccountList()), "收款账号不能为空");
		Assert.isTrue(requestDto.getAmount() != null, "申请入款金额不能为空");

		/**
		 * <pre>
		 * mark blake 2019-02-20 最终确定实现逻辑
		 * 1、通过传入的银行卡号获取云闪付账号
		 * 2、通过云闪付账号和银行卡号获得每一张银行卡实时的剩余的信用额度
		 * 3、获得满足信用额度的银行卡号
		 * 4、获取可用的银行卡号（在线，并且状态正常）
		 * 5、将步骤获得的银行卡号按最近使用时间升序排列
		 * 6、按排序结果所得的银行卡号分别取二维码，如果第一个设备 传入整数金额的二维码 使用完了，使用第二个设备的，以此类推。
		 * 7、取用二维码具体规则：
		 *     1)、二维码保存规则：
		 *         以日期:银行卡号:金额为外键的hash存储银行卡每日生成的二维码
		 *     2)、银行卡号随机金额锁规则：
		 *         以银行卡号为外键，key为具体金额，使用时间为分数的zset存储。使用时间比 （当前时间 -x 小时）大的数据为锁定（占用）
		 *     3)、要取用一个具体银行卡的二维码时，使用分布式锁 JedisLock 锁住健" 银行卡号_具体金额"
		 *         从步骤 2) 的 zset 中获取该二维码的使用时间，根据 2)的规则判断二维码是否被占用。
		 *         如果二维码被占用，取另外一个二维码
		 *         如果银行卡的二维码全部被占用，换另外一张银行卡
		 *         如果二维码未被占用，将该二维码设置使用时间（步骤2)的锁），发送redis消息告知其他服务器该银行卡最近使用时间，然后将该二维码返回给ForeRest
		 *     4)、需要注意的时，二维码按生成日期保存，但是二维码锁不区分日期
		 *
		 * </pre>
		 */

		log.debug(String.format("线程 {} 0.平台申请云闪付收款二维码，银行卡参数： %s", Thread.currentThread().getName(), Arrays.toString(requestDto.getAccountList().toArray())));

		// 获取可用收款账号列表
		List<String> availableDeviceList = getAvailableBankAccountList(requestDto.getHandicap(), requestDto.getAmount(),
				requestDto.getAccountList());
		log.debug("线程 {} 1.根据通道收款银行账号列表获取到的可用收款账号有{}", Thread.currentThread().getName(), Arrays.toString(availableDeviceList.toArray()));
		if (CollectionUtils.isEmpty(availableDeviceList)) {
			log.error("线程 {} 1.1请求银联云闪付付款二维码时，未能找到可用设备，参数：{}", Thread.currentThread().getName(), Arrays.toString(requestDto.getAccountList().toArray()));
			throw new RuntimeException("当前无可用设备");
		}

		// 如果可用银行卡号不为空，取银行卡号的二维码返回
		if (!CollectionUtils.isEmpty(availableDeviceList)) {
			// 需求 7876 收款账号按照每日入款流水总额排序，这个排序在获取账号时已经排序，所以这里不再排序。
			//List<String> availableDeviceList = getAvailableBankAccountList(requestDto.getHandicap(), requestDto.getAmount(),
			//		requestDto.getAccountList());
			List<String> sortedBankListList = availableDeviceList;
			
			log.debug("线程 {} 时间升序排列后银行卡列表为：{}", Thread.currentThread().getName(), Arrays.toString(sortedBankListList.toArray()));
			Integer bankIndex = 0;
			//尝试次数
			Integer bankTryCount = ySFLocalCacheUtil.getTimeOutChangeBankAccountCount();
			//金额累加次数
			Integer moneyTryCount = ySFLocalCacheUtil.getYsfQrCodeMoneyIncrement();
			Long currentTime = System.currentTimeMillis();
			List<String> releaseBankRandStr = new ArrayList<String>();
			String qrContent =  null;
			try {
				for(String bankAccount:sortedBankListList) {
					bankIndex ++;
					Integer tryIndex = 0;
					Integer tryMoney = new Integer(requestDto.getAmount());
					while (tryIndex <= moneyTryCount && StringUtils.isEmpty(qrContent)) {
						log.debug("线程 {} 尝试使用银行卡号{},整数金额{}的随机金额二维码",Thread.currentThread().getName(),bankAccount, tryMoney);
						BigDecimal finalAmount = null;
						try {
							finalAmount = inAccountRandUtil.getRandomStr(tryMoney, bankAccount, requestDto.getUserName(),currentTime);
						} catch (NoAvailableRandomException e) {
							log.error("线程 {} 银行卡号{}的对整数金额{}的随机金额已经全部被占用", Thread.currentThread().getName(), bankAccount,tryMoney);
							tryIndex++;
							tryMoney = tryMoney + 1;
							continue;
						}
						String finalAmountStr = InAccountRandUtil.moneyFormat(finalAmount);
						//标记超时需要释放的银行卡随机金额
						releaseBankRandStr.add(String.format(releaseBankRandStrFormat, bankAccount,finalAmountStr,currentTime));
						log.debug("线程 {} 获得银行卡号{}随机金额{}锁AAAAAAAAAAAAAAAAAA", Thread.currentThread().getName(), bankAccount,
								finalAmountStr);
						
						// 取该银行卡今日生成的二维码
						String key = getCurrentBankQrCacheKey(bankAccount, tryMoney);
						log.debug("线程 {} 获取key={}今日已经生成的二维码", Thread.currentThread().getName(), key);
						Map<String, String> existsQrCodeMap = new HashMap<String, String>();
						Map<Object, Object> existsQrCodeMap1 = null;
						StringRedisTemplate jedis = redisService.getYsfStringRedisTemplate();
						if (jedis != null) {
							existsQrCodeMap1 = jedis.opsForHash().entries(key);
						}
						if (existsQrCodeMap1 == null) {
							existsQrCodeMap1 = new HashMap<Object, Object>();
						}
						for(Entry<Object, Object> o:existsQrCodeMap1.entrySet()) {
							existsQrCodeMap.put((String)o.getKey(), (String)o.getValue());
						}
						log.debug("线程 {} 银行卡号{}今日对整数金额{}已经生成的二维码数量为{}", Thread.currentThread().getName(), bankAccount, tryMoney,
								existsQrCodeMap.size());
						qrContent = existsQrCodeMap.get(finalAmountStr);
						
						if (ObjectUtils.isEmpty(qrContent)) {
							log.debug("线程 {} 银行卡号{}的随机金额{}对应二维码内容为空，下发app生成二维码", Thread.currentThread().getName(),
									bankAccount, finalAmountStr);
							// 如果二维码不存在，则去生成二维码，并且生成二维码后直接返回
							try {
								qrContent = appGenQrCode(requestDto.getHandicap(), bankAccount, finalAmountStr);
								log.debug("线程 {} bankIndex = {}时，银行卡号{}的随机金额{}下发app生成二维码获得结果为:{}。", Thread.currentThread().getName(),
										bankIndex, bankAccount, finalAmountStr,qrContent);
								break;
							} catch (TimeoutException e) {
								qrContent = null;
								log.error("线程 {} bankIndex = {}时，银行卡号{}的随机金额{}下发app生成二维码超时。", Thread.currentThread().getName(),
										bankIndex, bankAccount, finalAmountStr,e);
								break;
							}
						}
					} ;
					
					if(!StringUtils.isEmpty(qrContent)) {
						log.debug("线程 {} 获取到银行卡{}的二维码,内容:{}", Thread.currentThread().getName(),bankAccount,qrContent);
						break;
					}
					
					//获取到具体银行卡或者达到尝试次数时，跳出循环
					if(bankIndex>=bankTryCount) {
						log.debug("线程 {}使用银行卡{}未能获取获取到二维码，但是已经达到尝试次数{}，不再使用新卡尝试", Thread.currentThread().getName(),bankAccount,bankTryCount);
						break;
					}else {
						log.debug("线程 {}使用银行卡{}未能获取获取到二维码，尚未达到尝试次数{}，使用新卡尝试", Thread.currentThread().getName(),bankAccount,bankTryCount);
					}
				}
				if(CollectionUtils.isEmpty(releaseBankRandStr)) {
					log.error("线程{} 使用银行卡{} 均未能获取到可用的随机金额，本次请求返回空",Thread.currentThread().getName(), Arrays.toString(sortedBankListList.toArray()));
					return null;
				}
				YSFResponseDto result = new YSFResponseDto();
				result.setHandicap(requestDto.getHandicap());
				result.setHandicapCode(requestDto.getHandicapCode());
				//如果尝试了多次，都超时，需要取第一个银行卡的随机二维码返回
				if (ObjectUtils.isEmpty(qrContent)) {
					//需求 5820 全部超时未能返回二维码时，取第一个银行卡信息返回。
					//要使用该银行卡的这个随机金额，所以这里使用 remove(0).确保该随机金额锁不被释放
					String str = releaseBankRandStr.remove(0);
					String[] tmp = str.split(releaseBankRandStrFormatSplit);
					result.setAccount(tmp[0]);
					result.setAmount(tmp[1]);
					log.error("线程 {}未能获取具体二维码.取第一个银行卡信息返回,银行卡号{},金额{}", Thread.currentThread().getName(),
							result.getAccount(), result.getAmount());
				}else {
					//该银行卡能够在规定时间内返回二维码，此时需要使用该随机金额的二维码，不是释放金额锁
					String str = releaseBankRandStr.remove(releaseBankRandStr.size()-1);
					String[] tmp = str.split(releaseBankRandStrFormatSplit);
					result.setAccount(tmp[0]);
					result.setAmount(tmp[1]);
					log.debug("线程 {} 获取到具体二维码，对应内容为：银行卡号{},金额{},二维码{}", Thread.currentThread().getName(), result.getAccount(),result.getAmount(),qrContent);
					result.setQrContent(qrContent);
				}
				inAccountRandUtil.setUserLockStr(result.getAccount(), requestDto.getUserName(), new BigDecimal(result.getAmount()));
				// 获取到了，可以直接返回
				return result;
			}finally {
				releaseBankRandStr(releaseBankRandStr);
			}
		}

		return null;
	}

	private static final String releaseBankRandStrFormatSplit = "_";
	private static final String releaseBankRandStrFormat ="%s_%s_%s";
	
	/**
	 * 释放锁定金额
	 * @param releaseBankRandStr
	 */
	private void releaseBankRandStr(List<String> releaseBankRandStr) {
		if(CollectionUtils.isEmpty(releaseBankRandStr)) {
			return;
		}
		log.debug("线程{} 释放银行卡的随机金额锁,参数{}", Thread.currentThread().getName(),Arrays.toString(releaseBankRandStr.toArray()));
		for(String str:releaseBankRandStr) {
			try {
				String[] tmp = str.split(releaseBankRandStrFormatSplit);
				if(tmp.length>=2) {
					inAccountRandUtil.recycleRandNumByTimeOut(tmp[0], tmp[1],Long.parseLong(tmp[2]));
				}
			}catch (Exception e) {
				log.error("线程{} 释放银行卡的随机金额锁时异常,参数{}。此异常不影响程序运行", Thread.currentThread().getName(),str);
			}
		}
	}
	
	/**
	 * 下发app生成二维码
	 *
	 * @param handicapId
	 * @param bankAccount
	 * @param expectAmounts
	 * @return
	 * @throws TimeoutException
	 * @throws Exception
	 */
	public String appGenQrCode(Integer handicapId, String bankAccount, String expectAmounts) throws TimeoutException {
		String qrContent = null;
		try {
			YSFQrCodeEntity result = ySFQrcodeGenServiceImpl.genYunSfQrCode(handicapId, bankAccount, expectAmounts);
			if (result != null) {
				qrContent = result.getQrContent();
			}
		} catch (TimeoutException e) {
			log.error("app生产二维码时在规定时间内没有返回，TimeoutException", e);
			throw e;
		} catch (InterruptedException e) {
			log.error("app生产二维码时产生异常（InterruptedException）", e);
		} catch (ExecutionException e) {
			log.error("app生产二维码时产生异常（ExecutionException）", e);
		}
		return qrContent;
	}

	/**
	 * 日期格式 yyyy-MM-dd
	 */
	private static DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

	@Override
	public CardForPayOutputDTO cardForPayYsfByLevelCode(CardForPayInputDTO inputDTO, BizHandicap handicap) {
		Assert.isTrue(!ObjectUtils.isEmpty(handicap) && !ObjectUtils.isEmpty(handicap.getId()), "盘口号不能为空");
		List<String> levelYsfAccountList = new ArrayList<String>();
		//根据层级编码获取云闪付银行卡号列表
		List<String> ysfAccountList = getYsfAccountByLevelCode(handicap,inputDTO.getLevelCode(),inputDTO.getAmount());
		if(!CollectionUtils.isEmpty(ysfAccountList)) {
			log.debug("通过层级编码获取到的云闪付入款账号列表未：{}",Arrays.toString(ysfAccountList.toArray()));
			levelYsfAccountList.addAll(ysfAccountList);
		}
		if(levelYsfAccountList.size()==0) {
			log.debug("通过层级编码未能获取到获取到云闪付入款账号，直接返回空");
			return null;
		}
		YSFRequestDto requestDto = new YSFRequestDto();
		List<String> reqAccountList = new ArrayList<String>();
		reqAccountList.addAll(levelYsfAccountList);
		requestDto.setAccountList(reqAccountList);
		requestDto.setAmount(inputDTO.getAmount().intValue());
		requestDto.setHandicap(handicap.getId());
		requestDto.setHandicapCode(handicap.getCode());
		requestDto.setUserName(inputDTO.getUserName());
		YSFResponseDto response = this.getQrCodeByReal(requestDto);
		CardForPayOutputDTO result = null;
		if (response != null) {
			result = new CardForPayOutputDTO();
			AccountBaseInfo account = accountService.getFromCacheByHandicapIdAndAccount(handicap.getId(),
					response.getAccount());
			if (account != null) {
				List<BankInfo> accountList = new ArrayList<>();
				BankInfo e = new BankInfo();
				e.setBankName(account.getBankName());
				e.setBankType(account.getBankType());
				e.setCardNo(response.getAccount());
				e.setCity(account.getCity());
				e.setOwner(account.getOwner());
				e.setProvince(account.getProvince());
				e.setQrCode(response.getQrContent());
				accountList.add(e);
				result.setAccountList(accountList);
			}
			result.setAmount(inputDTO.getAmount());
			Number dou = new BigDecimal(response.getAmount());
			result.setFinalAmount(dou);
			result.setOid(Integer.parseInt(response.getHandicapCode()));
			result.setType(InBankSubType.IN_BANK_YSF.getSubType().byteValue());
			result.setPocId(inputDTO.getOcId());
		}else {
			log.error("cardForPayYsfByLevelCode 获取的返回值为空，参数：{}",ObjectMapperUtils.serialize(inputDTO));
		}
		return result;
	}
	
	/**
	 * 通过层级编码获取云闪付入款卡列表
	 * @param handicap
	 * @param levelCode
	 * @return
	 */
	private List<String> getYsfAccountByLevelCode(BizHandicap handicap, String levelCode,Number amount) {
		List<String> result = new ArrayList<String>();
		//1、通过层级编码查询（biz_level）层级是内层还是外层 ->curr_sys_level 。如果在表中没有获取到，那么默认值取 1
		//获取层级-内层 或者 外层
		Integer currSysLevel = null;
		BizLevel level = levelService.findFromCache(handicap.getId(), levelCode);
		currSysLevel =(ObjectUtils.isEmpty(level)||ObjectUtils.isEmpty(level.getCurrSysLevel()))?1:level.getCurrSysLevel();
		//2、查询biz_account 获得云闪付入款卡，
		BigDecimal amountBigDecimal = new BigDecimal(amount.toString()); 
		List<AccountBaseInfo> accountBaseList = this.getAccountBaseInfoByLevelAndAmount(handicap.getId(), currSysLevel, amountBigDecimal);
		log.debug("getAccountBaseInfoByLevelAndAmount 获取到的accountBaseList.size = {}",accountBaseList==null?0:accountBaseList.size());
		//3、使用 AllocateTransServiceImpl.checkDailyIn
		//过滤已经到达每日累计入款限额的入款账号 
		if(!ObjectUtils.isEmpty(accountBaseList)) {
			List<AccountBaseInfo> accountList = accountBaseList.stream().filter(t -> checkAccountException(t)).collect(Collectors.toList());
			if(!ObjectUtils.isEmpty(accountList)) {
				//需求 7876 根据每日入款流水总金额升序排列
				List<Object> ysfAccountIdList = accountList.stream().map(AccountBaseInfo::getId).collect(Collectors.toList());
				Map<Integer, BigDecimal> ysfDayInMap = accountService.findAmountDaily(0,ysfAccountIdList);
				BigDecimal sysYsfDayInLimit = ySFLocalCacheUtil.getSysYsfDayInLimit();
				result.addAll(accountList.stream().filter(t -> this.checkDailyIn(t, ysfDayInMap,amountBigDecimal,sysYsfDayInLimit))
						.sorted((o1, o2) -> ysfDayInMap.get(o1.getId()).compareTo(ysfDayInMap.get(o2.getId())))
						.map(AccountBaseInfo::getAccount).collect(Collectors.toList()));
			}
			log.debug("经过 checkDailyIn 后获取到的入款账号为：{}",Arrays.toString(result.toArray()));
		}
		return result;
	}
	
	/**
	 * 查询满足条件的云闪付入款账号
	 * @param handicapId 盘口id
	 * @param currSysLevel 内外层
	 * @param amount 入款金额
	 * @return
	 */
	private List<AccountBaseInfo> getAccountBaseInfoByLevelAndAmount(int handicapId,Integer currSysLevel,BigDecimal amount){
		List<AccountBaseInfo> result = new ArrayList<AccountBaseInfo>();
		List<BizAccount> accountList = accountRepository.findAll((root, criteriaQuery, criteriaBuilder) -> {
			List<Predicate> predicateList = new ArrayList<>();
			//查询条件：
			//盘口id = handicapId
			Path<Integer> handicapPath = root.get("handicapId");
			Predicate handicapPredicate = criteriaBuilder.equal(handicapPath, handicapId);
			predicateList.add(handicapPredicate);
			//status = 1、
			Path<Byte> statusPath = root.get("status");
			Predicate statusPredicate = criteriaBuilder.equal(statusPath, new Byte("1"));
			predicateList.add(statusPredicate);
			//type = 1、
			Path<Integer> typePath = root.get("type");
			Predicate typePredicate = criteriaBuilder.equal(typePath, Integer.parseInt("1"));
			predicateList.add(typePredicate);
			//min_in_amount = null or 0 or min_in_amount <= amount
			Path<BigDecimal> minInAmountPath = root.get("minInAmount");
			Predicate minInAmountPredicateNull = criteriaBuilder.isNull(minInAmountPath);
			Predicate minInAmountPredicate0 = criteriaBuilder.equal(minInAmountPath, BigDecimal.ZERO);
			Predicate minInAmountPredicateGt = criteriaBuilder.lessThanOrEqualTo(minInAmountPath, amount);
			Predicate minInAmountPredicate = criteriaBuilder.or(minInAmountPredicateNull, minInAmountPredicate0,minInAmountPredicateGt);
			predicateList.add(minInAmountPredicate);
			
			//sub_type = 3
			Path<Integer> subTypePath = root.get("subType");
			Predicate subTypePredicate = criteriaBuilder.equal(subTypePath, Integer.parseInt("3"));
			predicateList.add(subTypePredicate);
			
			//curr_sys_level=步骤1获得的curr_sys_level、
			Path<Integer> currSysLevelPath = root.get("currSysLevel");
			Predicate currSysLevelPredicate = criteriaBuilder.equal(currSysLevelPath, currSysLevel);
			predicateList.add(currSysLevelPredicate);

			criteriaQuery.where(predicateList.toArray(new Predicate[predicateList.size()]));
			return null;
		});
		if(!ObjectUtils.isEmpty(accountList)) {
			accountList.stream().forEach(t->{
				AccountBaseInfo e = new AccountBaseInfo(t, null);
				result.add(e);
			});
		}
		return result;
	} 
	
	/**
	 * 校验入款账号是否达到每日入款上限
	 * @param base
	 * @return
	 */
	private boolean checkDailyIn(AccountBaseInfo base,Map<Integer, BigDecimal> ysfDayInMap,BigDecimal amountBigDecimal,BigDecimal sysYsfDayInLimit ) {
		if (base == null) {
			return true;
		}
		// 入款卡才校验入款限额
		if (base == null || !Objects.equals(Constants.INBANK, base.getType())) {
			return true;
		}
		BigDecimal inDaily = ysfDayInMap.get(base.getId());
		//需求 7876 云闪付卡入款限额
		if(sysYsfDayInLimit!=null && sysYsfDayInLimit.compareTo(BigDecimal.ZERO)>0 
				&& inDaily.add(amountBigDecimal).compareTo(sysYsfDayInLimit)>0) {
			return false;
		}
		if (base.getLimitIn() != null) {
			return new BigDecimal(base.getLimitIn()).compareTo(inDaily.add(new BigDecimal(CommonUtils.getLessThenSumDailyIncome())))>0;
		}
		return true;
	}
	
	/**
	 * 校验入款账号是否在账目排查中属于有问题的账号
	 * @param base
	 * @return
	 */
	private boolean checkAccountException(AccountBaseInfo base) {
		Set<Integer> invSet = systemAccountManager.accountingException();
		invSet.addAll(systemAccountManager.accountingSuspend());
		boolean result = !invSet.contains(base.getId());
		log.debug("getYsfAccountByLevelCode checkAccountException 账号：{} result:{}",base.getAccount(),result);
		return result;
	}
}