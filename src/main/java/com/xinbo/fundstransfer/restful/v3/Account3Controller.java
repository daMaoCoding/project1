package com.xinbo.fundstransfer.restful.v3;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import javax.validation.Valid;

import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.entity.activity.BizFlwActivitySyn;
import com.xinbo.fundstransfer.domain.pojo.BizCommonRemarkInputDTO;
import com.xinbo.fundstransfer.sec.FundTransferEncrypter;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppProperties;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizAccountMore;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.enums.AccountFlag;
import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.enums.BankEnums;
import com.xinbo.fundstransfer.restful.v3.pojo.ReqV3Acc;
import com.xinbo.fundstransfer.service.*;

@RestController
@RequestMapping("/api/v3")
@SuppressWarnings("WeakAccess unused")
public class Account3Controller extends Base3Common {
	private static final Logger log = LoggerFactory.getLogger(Account3Controller.class);
	private ObjectMapper mapper = new ObjectMapper();
	@Autowired
	private AppProperties appProperties;
	@Autowired
	private AccountService accountService;
	@Autowired
	private HandicapService handicapService;
	@Autowired
	private AccountMoreService accountMoreSer;
	@Autowired
	private CabanaService cabanaService;
	@Autowired
	private RedisService redisSer;
	@Autowired
	private RebateApiService rebateApiService;
	@Autowired
	private CommonRemarkService remarkService;
	@Autowired
	private RebateActivitySynService synService;

	@RequestMapping("/flush")
	public String flush() throws JsonProcessingException {
		try {
			redisSer.convertAndSend(RedisTopics.ACCOUNT_MORE_CLEAN, StringUtils.EMPTY);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			return mapper
					.writeValueAsString(new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue()));
		}
	}

	/**
	 * add / update bank account information.
	 */
	@RequestMapping(value = "/acc", method = RequestMethod.POST, consumes = "application/json")
	@ResponseBody
	public SimpleResponseData acc(@Valid @RequestBody ReqV3Acc requestBody, BindingResult result)
			throws JsonProcessingException {
		String bodyJson = mapper.writeValueAsString(requestBody);
		log.info("AccV3 >> RequestBody:{}", bodyJson);
		if (result.hasErrors()) {
			log.debug("AccV3 >> invalid params. RequestBody:{}", bodyJson);
			return ERROR_PARAM_INVALID;
		}
		if (!checkToken(requestBody))
			return ERROR_TOKEN_INVALID;
		if (Objects.isNull(requestBody.getFlag())
				|| requestBody.getFlag() != 0 && requestBody.getFlag() != 1 && requestBody.getFlag() != 2) {
			log.debug("AccV3 >> flag in [0,1,3]. RequestBody:{}", bodyJson);
			return ERROR_PARAM_INVALID;
		}
		/*
		 * de account
		 */
		requestBody.setAcc(deAccouont(requestBody.getAcc()));
		/*
		 * retrieve history account
		 */
		List<BizAccount> hisList = accountService.findByAccount(StringUtils.trimToEmpty(requestBody.getAcc()));
		// 检查是否存在返利网之外的 卡
		List<Integer> types = new ArrayList<>();
		types.add(AccountFlag.PC.getTypeId());
		types.add(AccountFlag.SPARETIME.getTypeId());
		List<BizAccount> SparetimeAndPC = hisList.stream().filter(p -> types.contains(p.getFlag()))
				.collect(Collectors.toList());
		if (null != SparetimeAndPC && SparetimeAndPC.size() > 0) {
			log.info("AccV3Error >> message:{} RequestBody:{}", ERROR_LIMIT_NOT_REBATEACCOUNT, bodyJson);
			return ERROR_LIMIT_NOT_REBATEACCOUNT;
		}
		// 如果不是返利网的 继续查看 是否手机兼职的,手机兼职的可以直接转返利网
		List<BizAccount> sparetimeAndJZ = hisList.stream()
				.filter(p -> Objects.equals(p.getFlag(), AccountFlag.SPARETIME.getTypeId()))
				.collect(Collectors.toList());
		hisList = hisList.stream().filter(p -> Objects.equals(p.getFlag(), AccountFlag.REFUND.getTypeId()))
				.collect(Collectors.toList());
		BizAccount account = CollectionUtils.isEmpty(hisList) ? new BizAccount() : hisList.get(0);
		if (null != account && null != account.getStatus() && AccountStatus.Delete.getStatus() == account.getStatus()) {
			log.info("AccV3Error >> message:{} RequestBody:{}", "删除的卡不能同步", bodyJson);
			return new SimpleResponseData(304, "删除的卡不能同步");
		}
		if (StringUtils.isBlank(account.getMobile()) && StringUtils.isBlank(requestBody.getMobile())) {
			log.info("AccV3Error >> message:{} RequestBody:{}", ERROR_ACCOUNT_NONMOBILE, bodyJson);
			return ERROR_ACCOUNT_NONMOBILE;
		}
		// 如果以前是手机兼职 改为返利网 则校验手机是否一样
		if (hisList.size() <= 0 && sparetimeAndJZ.size() > 0) {
			account = sparetimeAndJZ.get(0);
			if (account.getFlag() == 1 && !Objects.equals(account.getMobile(), deMobile(requestBody.getMobile()))) {
				log.info("AccV3Error >> message:{} RequestBody:{}", "手机转返利网绑定银行卡账号与返利网添加的不一样", bodyJson);
				return ERROR_ACCOUNT_MOBILE_WRONG;
			}
		}
		BizAccountMore more = accountMoreSer.getFromByUid(requestBody.getUid());
		if (Objects.nonNull(more) && Objects.nonNull(more.getMoible())
				&& StringUtils.isNotBlank(requestBody.getMobile())
				&& !Objects.equals(more.getMoible(), deMobile(requestBody.getMobile()))) {
			log.info("AccV3Error >> message:{} RequestBody:{}", ERROR_ACCOUNT_MOBILE_WRONG, bodyJson);
			return ERROR_ACCOUNT_MOBILE_WRONG;
		}
		if (Objects.isNull(account.getId())) {
			account.setFlag(AccountFlag.REFUND.getTypeId());
			account.setBankBalance(BigDecimal.ZERO);
			account.setType(AccountType.OutBank.getTypeId());
			account.setStatus(AccountStatus.Inactivated.getStatus());
			account.setCreateTime(account.getUpdateTime());
			String maxAlias = accountService.getMaxAlias();
			if (StringUtils.isEmpty(maxAlias) || maxAlias.equals("0")) {
				account.setAlias("100000");
			} else {
				account.setAlias(Integer.toString(Integer.parseInt(maxAlias) + 1).replace("4", "5"));
			}
			if (Objects.nonNull(more) && Objects.nonNull(more.getHandicap())) {
				int handicap = more.getHandicap();
				account.setHandicapId(handicap);
			} else {
				String handicapCode = TRANS_REF.get(requestBody.getBid());
				if (StringUtils.isNotBlank(handicapCode)) {
					BizHandicap handicap = handicapService.findFromCacheByCode(handicapCode);
					if (Objects.nonNull(handicap)) {
						account.setHandicapId(handicap.getId());
					} else {
						log.info("AccV3Error >> message:{} RequestBody:{}", ERROR_ACCOUNT_NO_EXIST_HANDICAP, bodyJson);
						return ERROR_ACCOUNT_NO_EXIST_HANDICAP;
					}
				} else {
					log.info("AccV3Error >> message:{} RequestBody:{}", ERROR_ACCOUNT_NO_EXIST_HANDICAP, bodyJson);
					return ERROR_ACCOUNT_NO_EXIST_HANDICAP;
				}
			}
		} else {
			if (AccountFlag.SPARETIME.getTypeId() != account.getFlag()) {
				// 查询是否已经绑定其它兼职人员，如果已经绑定其它人，则不应许绑定
				List<BizAccountMore> mores = accountMoreSer.getAccountMoreByaccountId(account.getId());
				if (null != mores && mores.size() > 0) {
					for (int i = 0; i < mores.size(); i++) {
						BizAccountMore acc = mores.get(i);
						// 如果存在不同的兼职 拥有相同的卡则返回
						if (!acc.getUid().equals(requestBody.getUid())) {
							log.info("AccV3Error >> message:{} RequestBody:{}", ERROR_LIMIT_EXIST_ACCOUNT, bodyJson);
							return ERROR_LIMIT_EXIST_ACCOUNT;
						}
					}
				}
			}
		}
		if (requestBody.getFlag() == 0) {// add
			try {
				if (StringUtils.isBlank(requestBody.getAcctype()) || StringUtils.isBlank(requestBody.getMobile())
						|| StringUtils.isBlank(requestBody.getAccinfo()) || StringUtils.isBlank(requestBody.getLname())
						|| StringUtils.isBlank(requestBody.getLpwd()) || StringUtils.isBlank(requestBody.getTpwd())
						|| StringUtils.isBlank(requestBody.getUid())) {
					log.debug(
							"AccV3 >> check whether param acctype|mobile|accinfo|lname|lpwd|tpwd|uid is empty. accinfo:{} lname:{} lpwd:{} tpwd:{} bid:{} uid:{}",
							requestBody.getAccinfo(), requestBody.getLname(), requestBody.getLpwd(),
							requestBody.getTpwd(), requestBody.getUid());
					return ERROR_PARAM_INVALID;
				}
				BankEnums bank = BankEnums.findByLog(requestBody.getAcctype());
				if (Objects.isNull(bank)) {
					log.info("AccV3Error >> message:{} RequestBody:{}", ERROR_ACCOUNT_NO_EXIST_BANKTYPE, bodyJson);
					return ERROR_ACCOUNT_NO_EXIST_BANKTYPE;
				}
				account.setAccount(requestBody.getAcc());
				account.setOwner(requestBody.getHolder());
				account.setBankName(requestBody.getAccinfo());
				account.setBankType(bank.getDesc());
				account.setMobile(deMobile(requestBody.getMobile()));
				account.setSign_(
						FundTransferEncrypter.encryptDb(StringUtils.trimToEmpty(deLname(requestBody.getLname()))));
				account.setHook_(
						FundTransferEncrypter.encryptDb(StringUtils.trimToEmpty(deLpwd(requestBody.getLpwd()))));
				account.setHub_(
						FundTransferEncrypter.encryptDb(StringUtils.trimToEmpty(deTpwd(requestBody.getTpwd()))));
				account.setUpdateTime(account.getCreateTime());
				account.setCreateTime(new Date());
				account.setProvince(PROVINCE_CITY.get(requestBody.getProvince()));
				account.setCity(PROVINCE_CITY.get(requestBody.getCity()));
				// 如果uid查询不到,用mobile查询 如果mobile查询的到直接修改
				BizAccountMore mo = accountMoreSer.findByMobile(deMobile(requestBody.getMobile()));
				// 如果是手机兼职存信息到redis,需要把信用额度同步给返利网
				if (AccountFlag.SPARETIME.getTypeId() == account.getFlag() && Objects.isNull(more)
						&& Objects.nonNull(mo)) {
					log.info("AccV3 >> message:{} RequestBody:{}", "手机兼职改为返利网!", bodyJson);
					mo.setAccounts("," + account.getId() + ",");
					mo.setUid(requestBody.getUid());
					mo.setClassify(AccountFlag.REFUND.getTypeId());
					mo.setMargin(null);
					mo.setBalance(null);
					mo.setUpdateTime(new Date());
					accountMoreSer.saveAndFlash(mo);
					redisSer.setString("limitAck" + account.getId(), account.getId().toString());
					accountService.setModel(account.getId(), 1, 1);
					cabanaService.updAcc(account.getId());
				} else {
					BizAccount ret = accountService.saveRebateAcc(account, more, requestBody.getUid(),
							AccountFlag.REFUND);
					if (AccountFlag.SPARETIME.getTypeId() == account.getFlag()) {
						log.info("AccV3 >> message:{} RequestBody:{}", "手机兼职改为返利网!", bodyJson);
						redisSer.setString("limitAck" + account.getId(), account.getId().toString());
					}
					accountService.setModel(ret.getId(), 1, 1);
					cabanaService.updAcc(ret.getId());
				}
			} catch (Exception e) {
				log.error("AccV3 >>  error. RequestBody:{}", bodyJson, e.getMessage());
				return ERROR_INNER_EXCEPTION;
			}
		} else if (requestBody.getFlag() == 1 && Objects.nonNull(account.getId())) {// update
			try {
				account.setAccount(requestBody.getAcc());
				if (StringUtils.isNotBlank(requestBody.getHolder())) {
					account.setOwner(requestBody.getHolder());
				}
				if (StringUtils.isNotBlank(requestBody.getAccinfo())) {
					account.setBankName(requestBody.getAccinfo());
				}
				if (StringUtils.isNotBlank(requestBody.getProvince())) {
					account.setProvince(PROVINCE_CITY.get(requestBody.getProvince()));
				}
				if (StringUtils.isNotBlank(requestBody.getCity())) {
					account.setCity(PROVINCE_CITY.get(requestBody.getCity()));
				}
				if (StringUtils.isNotBlank(requestBody.getAcctype())) {
					BankEnums bank = BankEnums.findByLog(requestBody.getAcctype());
					if (Objects.isNull(bank)) {
						log.info("AccV3Error >> message:{} RequestBody:{}", ERROR_ACCOUNT_NO_EXIST_BANKTYPE, bodyJson);
						return ERROR_ACCOUNT_NO_EXIST_BANKTYPE;
					}
					account.setBankType(bank.getDesc());
				}
				if (StringUtils.isNotBlank(requestBody.getMobile())) {
					account.setMobile(deMobile(requestBody.getMobile()));
				}
				if (StringUtils.isNotBlank(requestBody.getLname())) {
					account.setSign_(
							FundTransferEncrypter.encryptDb(StringUtils.trimToEmpty(deLname(requestBody.getLname()))));
				}
				if (StringUtils.isNotBlank(requestBody.getLpwd())) {
					account.setHook_(
							FundTransferEncrypter.encryptDb(StringUtils.trimToEmpty(deLpwd(requestBody.getLpwd()))));
				}
				if (StringUtils.isNotBlank(requestBody.getTpwd())) {
					account.setHub_(
							FundTransferEncrypter.encryptDb(StringUtils.trimToEmpty(deTpwd(requestBody.getTpwd()))));
				}
				account.setUpdateTime(new Date());
				BizAccount ret = accountService.saveRebateAcc(account, more, requestBody.getUid(), AccountFlag.REFUND);
				accountService.setModel(ret.getId(), 1, 1);
				cabanaService.updAcc(ret.getId());
			} catch (Exception e) {
				log.error("AccV3 >> inner error. RequestBody:{}", bodyJson, e.getMessage());
				return ERROR_INNER_EXCEPTION;
			}
		} else if (requestBody.getFlag() == 2) {// delete
			if (Objects.isNull(account)) {
				log.info("AccV3 >> del account {}  doesn't exist . data：{} ", requestBody.getAcc(), bodyJson);
				return ERROR_ACCOUNT_NONEXIST;
			}
			if (!Objects.equals(account.getStatus(), AccountStatus.Inactivated.getStatus())
					&& !Objects.equals(account.getStatus(), AccountStatus.Delete.getStatus())) {
				log.info("AccV3 >> del account {}  is in use, can't be delete . data：{} ", requestBody.getAcc(),
						bodyJson);
				return ERROR_ACCOUNT_WRONG_STATUS;
			}
			accountService.deleteAndClear(account.getId());
			accountMoreSer.deleteAcc(requestBody.getUid(), account.getId());
			cabanaService.updAcc(account.getId());
		}
		return SUCCESS;
	}

	/*
	 * acc 银行帐号在第7位插入一个随机0-9的数字 然后用base64加密
	 */
	private String deAccouont(String acc) {
		acc = new String(org.apache.mina.util.Base64.decodeBase64(acc.getBytes()));
		return acc.substring(0, 7) + acc.substring(8);
	}

	/*
	 * mobile 在第5位插入一个随机0-9的数字 然后用base64加密
	 */
	private static String deMobile(String mobile) {
		if (StringUtils.isBlank(mobile))
			return null;
		mobile = new String(org.apache.mina.util.Base64.decodeBase64(mobile.getBytes()));
		return mobile.substring(0, 5) + mobile.substring(6);
	}

	/*
	 * lname 在第3位插入2个随机0-9的数字 然后用base64加密
	 */
	private String deLname(String lname) {
		if (StringUtils.isBlank(lname))
			return null;
		lname = new String(org.apache.mina.util.Base64.decodeBase64(lname.getBytes()));
		return lname.substring(0, 3) + lname.substring(5);
	}

	/*
	 * lpwd 在第5位插入3个随机0-9的数字 然后用base64加密
	 */
	private String deLpwd(String lped) {
		if (StringUtils.isBlank(lped))
			return null;
		lped = new String(org.apache.mina.util.Base64.decodeBase64(lped.getBytes()));
		return lped.substring(0, 5) + lped.substring(8);
	}

	/*
	 * tpwd 在第2位插入一个随机0-9的数字 然后用base64加密
	 */
	private String deTpwd(String tpwd) {
		if (StringUtils.isBlank(tpwd))
			return null;
		tpwd = new String(org.apache.mina.util.Base64.decodeBase64(tpwd.getBytes()));
		return tpwd.substring(0, 2) + tpwd.substring(3);
	}

	/**
	 * check whether token is valid.
	 * <p>
	 * calculate target : acc+holder+acctype+mobile+salt.
	 *
	 * @return {@code true} pass checkpoint {@code false } non-pass checkpoint
	 */
	private boolean checkToken(ReqV3Acc arg0) {
		if (StringUtils.isBlank(arg0.getToken())) {
			log.debug("AccV3 ( invalid token ) >> token param is null. acc: {} holder: {} acctype: {}  mobile: {}",
					arg0.getAcc(), arg0.getHolder(), arg0.getAcctype(), arg0.getMobile());
			return false;
		}
		String oriContent = StringUtils.trimToEmpty(arg0.getAcc()) + StringUtils.trimToEmpty(arg0.getHolder())
				+ StringUtils.trimToEmpty(arg0.getAcctype()) + StringUtils.trimToEmpty(arg0.getMobile());
		String calToken = CommonUtils.md5digest(oriContent + appProperties.getRebatesalt());
		if (StringUtils.equals(calToken, arg0.getToken()))
			return true;
		log.debug(
				"AccV3 ( invalid token ) >>   acc: {} holder: {} acctype: {}  mobile: {} oriContent: {}  oriToken: {} calToekn: {}",
				arg0.getAcc(), arg0.getHolder(), arg0.getAcctype(), arg0.getMobile(), oriContent, oriContent, calToken);
		return false;
	}

	@RequestMapping("/dealWithMore")
	public String dealWithMore(@RequestParam(value = "uid") String uid,
			@RequestParam(value = "isDisplay", required = false) String isDisplay,
			@RequestParam(value = "displayDate", required = false) String displayDate,
			@RequestParam(value = "joinActivity", required = false) String joinActivity,
			@RequestParam(value = "activityNumber", required = false) String activityNumber)
			throws JsonProcessingException {
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请登录!"));
		}
		if (StringUtils.isBlank(uid)) {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数非法!"));
		}
		BizAccountMore more = accountMoreSer.getFromCacheByUid(uid);
		if ("0".equals(isDisplay)) {
			more.setIsDisplay("0");
			more.setDisplayAfterDate(null);
			BizCommonRemarkInputDTO inputDDTO = new BizCommonRemarkInputDTO();
			inputDDTO.setBusinessId(Integer.parseInt(uid));
			inputDDTO.setRemark("经沟通，兼职用户不再在列表中显示");
			inputDDTO.setType("rebateUser");
			inputDDTO.setStatus((byte) 1);
			inputDDTO.setSysUser(sysUser);
			remarkService.add(inputDDTO);
		} else if (StringUtils.isNotBlank(displayDate) && CommonUtils.isNumeric(displayDate)) {
			Date currDate = CommonUtils.getZeroDate(new Date());
			Calendar rightNow = Calendar.getInstance();
			rightNow.setTime(currDate);
			rightNow.add(Calendar.DATE, Integer.parseInt(displayDate));
			more.setDisplayAfterDate(rightNow.getTime());
		}
		if ("1".equals(joinActivity)) {
			if (StringUtils.isBlank(activityNumber)) {
				List<BizFlwActivitySyn> activitySynList = synService.findAvailableActivity();
				if (activitySynList != null && activitySynList.size() > 0) {
					for (BizFlwActivitySyn activity : activitySynList) {
						activityNumber = activity.getActivityNumber();
						break;
					}
				}
			}
			rebateApiService.joinFlwActivity(uid, activityNumber);
			BizCommonRemarkInputDTO inputDDTO = new BizCommonRemarkInputDTO();
			inputDDTO.setBusinessId(Integer.parseInt(uid));
			inputDDTO.setRemark("经沟通，将兼职加入返利网优惠活动中");
			inputDDTO.setType("rebateUser");
			inputDDTO.setStatus((byte) 1);
			inputDDTO.setSysUser(sysUser);
			remarkService.add(inputDDTO);
		}
		accountMoreSer.saveAndFlash(more);
		return mapper.writeValueAsString(
				new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "处理成功"));
	}
}
