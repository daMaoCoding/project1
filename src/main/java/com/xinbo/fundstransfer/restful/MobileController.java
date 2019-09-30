package com.xinbo.fundstransfer.restful;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.io.CharStreams;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.BizLevel;
import com.xinbo.fundstransfer.domain.entity.SysDataPermission;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import com.xinbo.fundstransfer.domain.enums.SysDataPermissionENUM;
import com.xinbo.fundstransfer.service.CloudService;
import com.xinbo.fundstransfer.service.HandicapService;
import com.xinbo.fundstransfer.service.LevelService;
import com.xinbo.fundstransfer.service.SysDataPermissionService;

/**
 * 手机账号管理
 */
@RestController
@RequestMapping("/r/mobile/cloud")
public class MobileController extends BaseController {
	private static final Logger logger = LoggerFactory.getLogger(MobileController.class);
	@Autowired
	private CloudService cloudService;
	@Autowired
	private HandicapService handicapService;
	@Value("${funds.transfer.multipart.location}")
	private String uploadPathNew;
	@Autowired
	private SysDataPermissionService sysDataPermissionService;
	@Autowired
	private LevelService levelService;

	/**
	 * 从云端分页获取手机账号信息
	 */
	@RequestMapping("/page")
	public String pageFromCloud(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "statusArray", required = false) String[] statusArray,
			@RequestParam(value = "search_LIKE_mobile", required = false) String search_LIKE_mobile,
			@RequestParam(value = "search_LIKE_owner", required = false) String search_LIKE_owner,
			@RequestParam(value = "search_LIKE_wechat", required = false) String search_LIKE_wechat,
			@RequestParam(value = "search_LIKE_alipay", required = false) String search_LIKE_alipay,
			@RequestParam(value = "search_LIKE_bank", required = false) String search_LIKE_bank,
			@RequestParam(value = "search_EQ_handicapId", required = false) Integer search_EQ_handicapId,
			@RequestParam(value = "search_EQ_type", required = false) Integer search_EQ_type,
			@RequestParam(value = "search_EQ_level", required = false) Integer search_EQ_level)
			throws JsonProcessingException {
		try {
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆"));
			}
			pageSize = Objects.isNull(pageSize) ? AppConstants.PAGE_SIZE : pageSize;
			BizHandicap handicap;
			Set<String> handicapCodeSet = new HashSet<>();
			if (search_EQ_handicapId != null) {
				handicap = handicapService.findFromCacheById(search_EQ_handicapId);
				if (handicap != null && StringUtils.isNotBlank(handicap.getCode())) {
					handicapCodeSet.add(handicap.getCode());
				}
			} else {
				List<SysDataPermission> permList = sysDataPermissionService.findSysDataPermission(sysUser.getId());
				if (!CollectionUtils.isEmpty(permList)) {
					permList.forEach(p -> {
						BizHandicap handicap1 = null;
						if (SysDataPermissionENUM.LEVELCODE.getValue().equals(p.getFieldName())) {
							BizLevel bizLevel = levelService.findFromCache(Integer.valueOf(p.getFieldValue()));
							if (bizLevel != null && bizLevel.getHandicapId() != null) {
								handicap1 = handicapService.findFromCacheById(bizLevel.getHandicapId());
							}
						}
						if (SysDataPermissionENUM.HANDICAPCODE.getValue().equals(p.getFieldName())) {
							handicap1 = handicapService.findFromCacheById(Integer.valueOf(p.getFieldValue()));
						}
						if (handicap1 != null && StringUtils.isNotBlank(handicap1.getCode())) {
							handicapCodeSet.add(handicap1.getCode());
						}
					});

				}
			}
			String handicap_str = null;
			if (!CollectionUtils.isEmpty(handicapCodeSet)) {
				StringBuilder stringBuilder = new StringBuilder();
				Iterator iterator = handicapCodeSet.iterator();
				if (handicapCodeSet.size() > 1) {
					while (iterator.hasNext()) {
						stringBuilder.append(iterator.next()).append(",");
					}
				} else {
					stringBuilder.append(iterator.next());
				}
				handicap_str = stringBuilder.toString();
			}
			handicap_str = Objects.isNull(handicap_str) ? StringUtils.EMPTY : handicap_str;
			String type_str = Objects.isNull(search_EQ_type) ? StringUtils.EMPTY : search_EQ_type.toString();
			String level_str = Objects.isNull(search_EQ_level) ? StringUtils.EMPTY : search_EQ_level.toString();

			return mapper.writeValueAsString(cloudService.rMobilePage(pageNo, pageSize, statusArray, search_LIKE_mobile,
					search_LIKE_owner, search_LIKE_wechat, search_LIKE_alipay, search_LIKE_bank, handicap_str, type_str,
					level_str));
		} catch (Exception e) {
			logger.error("从云端分页获取手机账号信息 Error,", e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getLocalizedMessage()));
		}
	}

	/**
	 * @see com.xinbo.fundstransfer.domain.enums.AccountType#InAli
	 * @see com.xinbo.fundstransfer.domain.enums.AccountType#InWechat
	 */
	@RequestMapping("/pageForQr")
	public String pageForQrFromCloud(@RequestParam(value = "pageNo") int pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize,
			@RequestParam(value = "mobile") String mobile, @RequestParam(value = "account") String account,
			@RequestParam(value = "accountType") Integer accountType,
			@RequestParam(value = "amtBegin", required = false) Integer amtBegin,
			@RequestParam(value = "amtEnd", required = false) Integer amtEnd) throws JsonProcessingException {
		try {
			pageSize = Objects.isNull(pageSize) ? AppConstants.PAGE_SIZE : pageSize;
			return mapper.writeValueAsString(
					cloudService.rMobilePageForQr(mobile, account, accountType, amtBegin, amtEnd, pageSize, pageNo));
		} catch (Exception e) {
			logger.error("pageForQr Error,", e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getLocalizedMessage()));
		}
	}

	/**
	 * @see com.xinbo.fundstransfer.domain.enums.AccountType#InAli
	 * @see com.xinbo.fundstransfer.domain.enums.AccountType#InWechat
	 */
	@RequestMapping("/genQR")
	public String genQRFromCloud(@RequestParam(value = "mobile") String mobile,
			@RequestParam(value = "account") String account, @RequestParam(value = "accountType") Integer accountType,
			@RequestParam(value = "amtBegin") Integer amtBegin, @RequestParam(value = "amtEnd") Integer amtEnd,
			@RequestParam(value = "step") Integer step) throws JsonProcessingException {
		try {
			return mapper.writeValueAsString(
					cloudService.rMobileGenQR(mobile, account, accountType, amtBegin, amtEnd, step));
		} catch (Exception e) {
			logger.error("genQR Error,", e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getLocalizedMessage()));
		}
	}

	/**
	 * 根据金额生成二维码 amounts传入多个整数金额，以逗号隔开
	 * 
	 * @see com.xinbo.fundstransfer.domain.enums.AccountType#InAli
	 * @see com.xinbo.fundstransfer.domain.enums.AccountType#InWechat
	 */
	@RequestMapping("/genExactQR")
	public String genExactQRFromCloud(@RequestParam(value = "mobile") String mobile,
			@RequestParam(value = "account") String account, @RequestParam(value = "accountType") Integer accountType,
			@RequestParam(value = "amounts") String amounts) throws JsonProcessingException {
		try {
			return mapper.writeValueAsString(cloudService.rMobileGenExactQR(mobile, account, accountType, amounts));
		} catch (Exception e) {
			logger.error("genQR Error,", e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getLocalizedMessage()));
		}
	}

	/**
	 * 生成0元二维码
	 *
	 * @see com.xinbo.fundstransfer.domain.enums.AccountType#InAli
	 * @see com.xinbo.fundstransfer.domain.enums.AccountType#InWechat
	 */
	@RequestMapping("/genZeroQR")
	public String genZeroQRFromCloud(@RequestParam(value = "mobile") String mobile,
			@RequestParam(value = "account") String account, @RequestParam(value = "accountType") Integer accountType)
			throws JsonProcessingException {
		try {
			return mapper.writeValueAsString(cloudService.rMobileGenZeroQR(mobile, account, accountType));
		} catch (Exception e) {
			logger.error("genZeroQR Error,", e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getLocalizedMessage()));
		}
	}

	/**
	 * 根据导入txt解析导入的金额
	 * 
	 * @see com.xinbo.fundstransfer.domain.enums.AccountType#InAli
	 * @see com.xinbo.fundstransfer.domain.enums.AccountType#InWechat
	 */
	@RequestMapping(value = "/genImportQR/{mobile}/{account}/{accountType}", consumes = "multipart/form-data", method = RequestMethod.POST)
	public String genImportQRFromCloud(@PathVariable(value = "mobile") String mobile,
			@PathVariable(value = "account") String account, @PathVariable(value = "accountType") Integer accountType,
			@RequestParam("file") MultipartFile multipartFile) throws JsonProcessingException {
		String params = buildParams().toString();
		logger.debug(String.format("%s，参数：%s", "导入二维码生成金额信息", params));
		try {
			String amounts = "";
			InputStream inputStream = multipartFile.getInputStream();// 获取输入流
			if (null != inputStream) {
				String str = CharStreams.toString(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
				if (StringUtils.isNotEmpty(str)) {
					amounts = str.replaceAll("\\r\\n", ",");
				}
			}
			if (StringUtils.isEmpty(amounts)) {
				return mapper.writeValueAsString(new GeneralResponseData<>(
						GeneralResponseData.ResponseStatus.FAIL.getValue(), "内容为空" + amounts));
			}
			return mapper.writeValueAsString(cloudService.rMobileGenExactQR(mobile, account, accountType, amounts));
		} catch (Exception e) {
			logger.error("genQR Error,", e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getLocalizedMessage()));
		}
	}

	/**
	 * @see com.xinbo.fundstransfer.domain.enums.AccountType#InAli
	 * @see com.xinbo.fundstransfer.domain.enums.AccountType#InWechat
	 */
	@RequestMapping("/delQR")
	public String delQRFromCloud(@RequestParam(value = "mobile") String mobile,
			@RequestParam(value = "account") String account, @RequestParam(value = "accountType") Integer accountType,
			@RequestParam(value = "amt") Integer amt) throws JsonProcessingException {
		try {
			return mapper.writeValueAsString(cloudService.rMobileDelQR(mobile, account, accountType, amt));
		} catch (Exception e) {
			logger.error("delQR Error,", e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getLocalizedMessage()));
		}
	}

	/**
	 * @see com.xinbo.fundstransfer.domain.enums.AccountType#InAli
	 * @see com.xinbo.fundstransfer.domain.enums.AccountType#InWechat
	 */
	@RequestMapping("/delQRList")
	public String delQRList(@RequestParam(value = "account") String account,
			@RequestParam(value = "accountType") Integer accountType,
			@RequestParam(value = "amtList") List<Integer> amtList) throws JsonProcessingException {
		try {
			return mapper.writeValueAsString(cloudService.rMobileDelQRList(account, accountType, amtList));
		} catch (Exception e) {
			logger.error("delQR Error,", e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getLocalizedMessage()));
		}
	}

	/**
	 * 新增手机账号信息
	 */
	@RequestMapping("/put")
	public String save(@RequestParam(value = "handicapId") Integer handicapId,
			@RequestParam(value = "mobile") String mobile, @RequestParam(value = "owner") String owner,
			@RequestParam(value = "type") Integer type, @RequestParam(value = "level") Integer level,
			@RequestParam(value = "creditLimit", required = false) Integer creditLimit,
			@RequestParam(value = "remark", required = false) String remark,
			@RequestParam(value = "bankAcc", required = false) String bankAcc,
			@RequestParam(value = "bankOwner", required = false) String bankOwner,
			@RequestParam(value = "bankName", required = false) String bankName,
			@RequestParam(value = "bankType", required = false) String bankType,
			@RequestParam(value = "bankLimitBalance", required = false) BigDecimal bankLimitBalance,
			@RequestParam(value = "wecAcc", required = false) String wecAcc,
			@RequestParam(value = "wecOwner", required = false) String wecOwner,
			@RequestParam(value = "wecInLimitDaily", required = false) BigDecimal wecInLimitDaily,
			@RequestParam(value = "wechatLimitBalance", required = false) BigDecimal wechatLimitBalance,
			@RequestParam(value = "wechatTransOutType", required = false) Integer wechatTransOutType,
			@RequestParam(value = "aliAcc", required = false) String aliAcc,
			@RequestParam(value = "aliOwner", required = false) String aliOwner,
			@RequestParam(value = "aliInLimitDaily", required = false) BigDecimal aliInLimitDaily,
			@RequestParam(value = "alipayLimitBalance", required = false) BigDecimal alipayLimitBalance,
			@RequestParam(value = "alipayTransOutType", required = false) Integer alipayTransOutType,
			@RequestParam(value = "bonusCard") String bonusCard,
			@RequestParam(value = "bonusCardOwner") String bonusCardOwner,
			@RequestParam(value = "bonusCardName") String bonusCardName) throws JsonProcessingException {
		try {
			BizHandicap handicap = handicapService.findFromCacheById(handicapId);
			Object ret = cloudService.rMobilePut(handicap.getCode(), mobile, owner, type, level, creditLimit, remark,
					bankAcc, bankOwner, bankName, bankType, bankLimitBalance, wecAcc, wecOwner, wecInLimitDaily,
					wechatLimitBalance, wechatTransOutType, aliAcc, aliOwner, aliInLimitDaily, alipayLimitBalance,
					alipayTransOutType, bonusCard, bonusCardOwner, bonusCardName, AccountStatus.Normal.getStatus());
			return mapper.writeValueAsString(ret);
		} catch (Exception e) {
			logger.error("新增手机账号信息 Error. mobile:{} bank:{} ali:{} wec:{}", mobile, bankAcc, aliAcc, wecAcc, e);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getMessage()));
		}
	}

	/**
	 * 从云端获取手机状态信息
	 * 
	 * @param mobile
	 *            手机号码
	 */
	@RequestMapping("/get")
	public String findFromCloud(@RequestParam(value = "mobile") String mobile) throws JsonProcessingException {
		try {
			return mapper.writeValueAsString(cloudService.rMobileGet(mobile));
		} catch (Exception e) {
			logger.error("从云端获取手机状态信息 Error.参数:%s", mobile, e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 从云端获取： 根据手机号获取该手机号绑定的银行卡，支付宝，微信的当日收款，当日提现，当前金额
	 * 
	 * @param mobile
	 *            手机号码
	 */
	@RequestMapping("/getBal")
	public String findBalFromCloud(@RequestParam(value = "mobile") String mobile) throws JsonProcessingException {
		try {
			return mapper.writeValueAsString(cloudService.rMobileGetBal(mobile));
		} catch (Exception e) {
			logger.error("从云端获取:根据手机号获取该手机号绑定的银行卡,支付宝,微信的当日收款,当日提现,当前金额，Error.参数:%s", mobile, e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 修改
	 */
	@RequestMapping("/updBase")
	public String updBaseFromCloud(@RequestParam(value = "mobileId") Integer mobileId,
			@RequestParam(value = "mobile") String mobile,
			@RequestParam(value = "status", required = false) Integer status,
			@RequestParam(value = "creditLimit", required = false) BigDecimal creditLimit,
			@RequestParam(value = "level", required = false) Integer level,
			@RequestParam(value = "owner", required = false) String owner,
			@RequestParam(value = "bonusAccount", required = false) String bonusAccount,
			@RequestParam(value = "bonusOwner", required = false) String bonusOwner,
			@RequestParam(value = "bonusBankName", required = false) String bonusBankName)
			throws JsonProcessingException {
		try {
			Object ret = cloudService.rMobileUpdBase(mobileId, mobile, status, creditLimit, level, owner, bonusAccount,
					bonusOwner, bonusBankName);
			return mapper.writeValueAsString(ret);
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 修改
	 * 
	 * @see com.xinbo.fundstransfer.domain.enums.AccountStatus
	 * @see com.xinbo.fundstransfer.domain.enums.AccountType#BindCustomer
	 * @see com.xinbo.fundstransfer.domain.enums.AccountType#InWechat
	 * @see com.xinbo.fundstransfer.domain.enums.AccountType#InAli
	 */
	@RequestMapping("/updAccStatus")
	public String updAccStatusFromCloud(@RequestParam(value = "mobile") String mobile,
			@RequestParam(value = "account") String account, @RequestParam(value = "type") Integer type,
			@RequestParam(value = "status") Integer status) throws JsonProcessingException {
		try {
			Object ret = cloudService.rMobileUpdAccStatus(mobile, account, type, status);
			return mapper.writeValueAsString(ret);
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 修改账号信息
	 * 
	 * @see com.xinbo.fundstransfer.domain.enums.AccountType#InAli
	 * @see com.xinbo.fundstransfer.domain.enums.AccountType#InWechat
	 * @see com.xinbo.fundstransfer.domain.enums.AccountType#BindCustomer
	 */
	@RequestMapping("/updAcc")
	public String updAccFromCloud(@RequestParam(value = "mobile") String mobile,
			@RequestParam(value = "account") String account, @RequestParam(value = "type") Integer type,
			@RequestParam(value = "owner", required = false) String owner,
			@RequestParam(value = "accountName", required = false) String accountName,
			@RequestParam(value = "bankType", required = false) String bankType,
			@RequestParam(value = "limitInDaily", required = false) BigDecimal limitInDaily,
			@RequestParam(value = "limitBalance", required = false) BigDecimal limitBalance,
			@RequestParam(value = "transOutType", required = false) Integer transOutType)
			throws JsonProcessingException {
		try {
			Object ret = cloudService.rMobileUpdAcc(mobile, account, type, owner, accountName, bankType, limitInDaily,
					limitBalance, transOutType);
			return mapper.writeValueAsString(ret);
		} catch (Exception e) {
			logger.error("账号修改，Error.", e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 密码修改
	 */
	@RequestMapping("/updPwd")
	public String updPwd(@RequestParam(value = "bank", required = false) String bank,
			@RequestParam(value = "singBank", required = false) String singBank,
			@RequestParam(value = "pingBank", required = false) String pingBank,
			@RequestParam(value = "bingBank", required = false) String bingBank,
			@RequestParam(value = "uingBank", required = false) String uingBank,
			@RequestParam(value = "alipay", required = false) String alipay,
			@RequestParam(value = "singAlipay", required = false) String singAlipay,
			@RequestParam(value = "pingAlipay", required = false) String pingAlipay,
			@RequestParam(value = "wechat", required = false) String wechat,
			@RequestParam(value = "singWechat", required = false) String singWechat,
			@RequestParam(value = "pingWechat", required = false) String pingWechat) throws JsonProcessingException {
		try {
			return mapper.writeValueAsString(cloudService.rMobilePwd(bank, singBank, pingBank, bingBank, uingBank,
					alipay, singAlipay, pingAlipay, wechat, singWechat, pingWechat));
		} catch (Exception e) {
			logger.error("密码修改，Error.", e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 删除
	 * 
	 * @param
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequiresPermissions(value = { "IncomePhoneNumber:deleteMobile" })
	@RequestMapping("/delete")
	public String delete(@RequestParam(value = "mobile") String mobile) throws JsonProcessingException {
		String params = buildParams().toString();
		logger.trace(String.format("%s，参数：%s", "手机号删除", params));
		try {
			return mapper.writeValueAsString(cloudService.rMobileDel(mobile));
		} catch (Exception e) {
			logger.error(String.format("%s，参数：%s，结果：%s", "手机号删除", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 返点账号查询
	 */
	@RequestMapping("/bonusAccList")
	public String bonusAccList(@RequestParam(value = "mobile", required = false) String mobile)
			throws JsonProcessingException {
		try {
			return mapper.writeValueAsString(cloudService.rMobileBonusAccList(mobile));
		} catch (Exception e) {
			logger.error("密码修改，Error.", e);
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}
}
