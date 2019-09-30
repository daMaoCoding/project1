package com.xinbo.fundstransfer.component.net.http.cloud;

import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.net.http.RequestBodyParser;
import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.enums.MobileStatus;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import okhttp3.MediaType;
import okhttp3.RequestBody;

@Component
public class ReqCoudBodyParser {
	@Value("${funds.transfer.apicloudkey}")
	String keyscloud;
	@Autowired
	RequestBodyParser requestBodyParser;
	private static ObjectMapper mapper = new ObjectMapper();

	public RequestBody rMobilePage(int pageNo, int pageSize, String[] statusArray, String search_LIKE_mobile,
			String search_LIKE_owner, String search_LIKE_wechat, String search_LIKE_alipay, String search_LIKE_bank,
			String search_EQ_handicap, String search_EQ_type, String search_EQ_level) {
		try {
			Map<String, Object> params = new TreeMap<>(Comparator.naturalOrder());
			params.put("search_LIKE_mobile", search_LIKE_mobile);
			params.put("search_LIKE_owner", search_LIKE_owner);
			params.put("search_LIKE_wechat", search_LIKE_wechat);
			params.put("search_LIKE_alipay", search_LIKE_alipay);
			params.put("search_LIKE_bank", search_LIKE_bank);
			params.put("search_EQ_handicap", search_EQ_handicap);
			params.put("search_EQ_type", search_EQ_type);
			params.put("search_EQ_level", search_EQ_level);
			params.put("token", CommonUtils.md5digest(keyscloud, params));
			params.put("pageNo", pageNo);
			params.put("pageSize", pageSize);
			params.put("statusArray", statusArray);
			return RequestBody.create(MediaType.parse("application/json"), mapper.writeValueAsString(params));
		} catch (Exception e) {
			return null;
		}
	}

	public RequestBody rMobilePut(String handicap, String mobile, String owner, Integer type, Integer level,
			Integer creditLimit, String remark, String bankAcc, String bankOwner, String bankName, String bankType,
			BigDecimal bankLimitBalance, String wecAcc, String wecOwner, BigDecimal wecInLimitDaily,
			BigDecimal wecLimitBalance, Integer wechatTransOutType, String aliAcc, String aliOwner,
			BigDecimal aliInLimitDaily, BigDecimal alipayLimitBalance, Integer alipayTransOutType, String bonusCard,
			String bonusCardOwner, String bonusCardName, Integer bonusCardStatus) {
		try {
			Map<String, Object> params = new TreeMap<>(Comparator.naturalOrder());
			params.put("mobile", mobile);
			params.put("type", type);
			params.put("status", MobileStatus.StopTemp.getStatus());
			params.put("creditLimit", creditLimit);
			params.put("handicap", handicap);
			params.put("owner", owner);
			params.put("level", level);
			params.put("remark", remark);
			bankAcc = StringUtils.trimToNull(bankAcc);
			if (bankAcc != null) {
				params.put("bank", bankAcc);
				params.put("bankOwner", bankOwner);
				params.put("bankName", bankName);
				params.put("bankType", AccountType.BindCustomer.getTypeId());
				params.put("bankTypeName", bankType);
				params.put("bankStatus", AccountStatus.StopTemp.getStatus());
				params.put("bankLimitBalance", bankLimitBalance);
			}
			params.put("bonusCard", bonusCard);
			params.put("bonusCardOwner", bonusCardOwner);
			params.put("bonusCardName", bonusCardName);
			params.put("bonusCardStatus", bonusCardStatus);
			wecAcc = StringUtils.trimToNull(wecAcc);
			if (wecAcc != null) {
				params.put("wechat", wecAcc);
				params.put("wechatOwner", wecOwner);
				// params.put("wechatType", wechat.getBankType());
				params.put("wechatStatus", AccountStatus.StopTemp.getStatus());
				params.put("wechatInLimitDaily", wecInLimitDaily);
				params.put("wechatLimitBalance", wecLimitBalance);
				params.put("wechatTransOutType", wechatTransOutType);
			}
			aliAcc = StringUtils.trimToNull(aliAcc);
			if (aliAcc != null) {
				params.put("alipay", aliAcc);
				params.put("alipayOwner", aliOwner);
				// params.put("alipayType", alipay.getBankType());
				params.put("alipayStatus", AccountStatus.StopTemp.getStatus());
				params.put("alipayInLimitDaily", aliInLimitDaily);
				params.put("alipayLimitBalance", alipayLimitBalance);
				params.put("alipayTransOutType", alipayTransOutType);
			}
			params.put("token", CommonUtils.md5digest(keyscloud, params));
			return RequestBody.create(MediaType.parse("application/json"), mapper.writeValueAsString(params));
		} catch (Exception e) {
			return null;
		}
	}

	public RequestBody rMobilePwd(String bank, String singBank, String pingBank, String bingBank, String uingBank,
			String alipay, String singAlipay, String pingAlipay, String wechat, String singWechat, String pingWechat) {
		try {
			Map<String, Object> params = new TreeMap<>(Comparator.naturalOrder());
			params.put("bank", StringUtils.trimToNull(bank));
			params.put("singBank", StringUtils.trimToNull(singBank));
			params.put("pingBank", StringUtils.trimToNull(pingBank));
			params.put("bingBank", StringUtils.trimToNull(bingBank));
			params.put("uingBank", StringUtils.trimToNull(uingBank));
			params.put("alipay", StringUtils.trimToNull(alipay));
			params.put("singAlipay", StringUtils.trimToNull(singAlipay));
			params.put("pingAlipay", StringUtils.trimToNull(pingAlipay));
			params.put("wechat", StringUtils.trimToNull(wechat));
			params.put("singWechat", StringUtils.trimToNull(singWechat));
			params.put("pingWechat", StringUtils.trimToNull(pingWechat));
			params.put("token", CommonUtils.md5digest(keyscloud, params));
			return RequestBody.create(MediaType.parse("application/json"), mapper.writeValueAsString(params));
		} catch (Exception e) {
			return null;
		}
	}

	public RequestBody rMobileUpdAcc(String mobile, String account, Integer type, String owner, String accountName,
			String bankType, BigDecimal limitInDaily, BigDecimal limitBalance, Integer transOutType) {
		try {
			Map<String, Object> params = new TreeMap<>(Comparator.naturalOrder());
			params.put("mobile", StringUtils.trimToNull(mobile));
			params.put("account", StringUtils.trimToNull(account));
			params.put("bankType", StringUtils.trimToNull(bankType));
			params.put("type", type);
			params.put("owner", StringUtils.trimToNull(owner));
			params.put("accountName", StringUtils.trimToNull(accountName));
			params.put("limitInDaily", limitInDaily);
			params.put("limitBalance", limitBalance);
			params.put("transOutType", transOutType);
			params.put("token", CommonUtils.md5digest(keyscloud, params));
			return RequestBody.create(MediaType.parse("application/json"), mapper.writeValueAsString(params));
		} catch (Exception e) {
			return null;
		}
	}

	public RequestBody rMobileGet(String mobile) {
		try {
			Map<String, Object> params = new TreeMap<>(Comparator.naturalOrder());
			params.put("token", CommonUtils.md5digest(keyscloud, mobile));
			params.put("mobile", mobile);
			return RequestBody.create(MediaType.parse("application/json"), mapper.writeValueAsString(params));
		} catch (Exception e) {
			return null;
		}
	}

	public RequestBody rMobileUpdbase(Integer mobileId, String mobile, Integer status, BigDecimal creditLimit,
			Integer level, String owner, String bonusAccount, String bonusOwner, String bonusBankName) {
		try {
			Map<String, Object> params = new TreeMap<>(Comparator.naturalOrder());
			params.put("mobileId", mobileId);
			params.put("mobile", mobile);
			params.put("status", status);
			params.put("creditLimit", creditLimit);
			params.put("level", level);
			params.put("owner", owner);
			params.put("bonusAccount", bonusAccount);
			params.put("bonusOwner", bonusOwner);
			params.put("bonusBankName", bonusBankName);
			params.put("token", CommonUtils.md5digest(keyscloud, params));
			return RequestBody.create(MediaType.parse("application/json"), mapper.writeValueAsString(params));
		} catch (Exception e) {
			return null;
		}
	}

	public RequestBody rMobileUpdAccStatus(String mobile, String account, Integer type, Integer status) {
		try {
			Map<String, Object> params = new TreeMap<>(Comparator.naturalOrder());
			params.put("mobile", mobile);
			params.put("account", account);
			params.put("type", type);
			params.put("status", status);
			params.put("token", CommonUtils.md5digest(keyscloud, params));
			return RequestBody.create(MediaType.parse("application/json"), mapper.writeValueAsString(params));
		} catch (Exception e) {
			return null;
		}
	}

	public RequestBody rMobileBonusAccList(String mobile) {
		try {
			Map<String, Object> params = new TreeMap<>(Comparator.naturalOrder());
			params.put("token", CommonUtils.md5digest(keyscloud, mobile));
			params.put("mobile", mobile);
			return RequestBody.create(MediaType.parse("application/json"), mapper.writeValueAsString(params));
		} catch (Exception e) {
			return null;
		}
	}

	public RequestBody rMobileGetBal(String mobile) {
		try {
			Map<String, Object> params = new TreeMap<>(Comparator.naturalOrder());
			params.put("mobile", mobile);
			params.put("token", CommonUtils.md5digest(keyscloud, mobile));
			return RequestBody.create(MediaType.parse("application/json"), mapper.writeValueAsString(params));
		} catch (Exception e) {
			return null;
		}
	}

	public RequestBody rMobilePageForQr(String mobile, String account, Integer accountType, Integer amtBegin,
			Integer amtEnd, Integer pageSize, Integer pageNo) {
		try {
			Map<String, Object> params = new TreeMap<>(Comparator.naturalOrder());
			params.put("mobile", mobile);
			params.put("account", account);
			params.put("accountType", accountType);
			params.put("amtBegin", amtBegin);
			params.put("amtEnd", amtEnd);
			params.put("pageSize", pageSize);
			params.put("pageNo", pageNo);
			params.put("token", CommonUtils.md5digest(keyscloud, params));
			return RequestBody.create(MediaType.parse("application/json"), mapper.writeValueAsString(params));
		} catch (Exception e) {
			return null;
		}
	}

	public RequestBody rMobileGenQR(String mobile, String account, Integer accountType, Integer amtBegin,
			Integer amtEnd, Integer step) {
		try {
			Map<String, Object> params = new TreeMap<>(Comparator.naturalOrder());
			params.put("mobile", mobile);
			params.put("account", account);
			params.put("accountType", accountType);
			params.put("amtBegin", amtBegin);
			params.put("amtEnd", amtEnd);
			params.put("step", step);
			params.put("token", CommonUtils.md5digest(keyscloud, params));
			return RequestBody.create(MediaType.parse("application/json"), mapper.writeValueAsString(params));
		} catch (Exception e) {
			return null;
		}
	}

	public RequestBody rMobileGenQR(String mobile, String account, Integer accountType, Integer amt) {
		try {
			Map<String, Object> params = new TreeMap<>(Comparator.naturalOrder());
			params.put("mobile", mobile);
			params.put("account", account);
			params.put("accountType", accountType);
			params.put("amt", amt);
			params.put("token", CommonUtils.md5digest(keyscloud, params));
			return RequestBody.create(MediaType.parse("application/json"), mapper.writeValueAsString(params));
		} catch (Exception e) {
			return null;
		}
	}

	public RequestBody rMobileGenExactQR(String mobile, String account, Integer accountType, String exactQR) {
		try {
			Map<String, Object> params = new TreeMap<>(Comparator.naturalOrder());
			params.put("mobile", mobile);
			params.put("account", account);
			params.put("accountType", accountType);
			params.put("exactQR", exactQR);
			params.put("token", CommonUtils.md5digest(keyscloud, params));
			return RequestBody.create(MediaType.parse("application/json"), mapper.writeValueAsString(params));
		} catch (Exception e) {
			return null;
		}
	}

	public RequestBody rMobileGenZeroQR(String mobile, String account, Integer accountType) {
		try {
			Map<String, Object> params = new TreeMap<>(Comparator.naturalOrder());
			params.put("mobile", mobile);
			params.put("account", account);
			params.put("accountType", accountType);
			params.put("token", CommonUtils.md5digest(keyscloud, params));
			return RequestBody.create(MediaType.parse("application/json"), mapper.writeValueAsString(params));
		} catch (Exception e) {
			return null;
		}
	}

	public RequestBody rMobileDelQRList(String account, Integer accountType, List<Integer> amtList) {
		try {
			List<String> qrList = amtList.stream().filter(p -> p != null).distinct().mapToInt(p -> p)
					.mapToObj(p -> String.valueOf(p)).collect(Collectors.toList());
			Map<String, Object> params = new TreeMap<>(Comparator.naturalOrder());
			params.put("account", account);
			params.put("accountType", accountType);
			params.put("exactQR", String.join(",", qrList));
			params.put("token", CommonUtils.md5digest(keyscloud, params));
			return RequestBody.create(MediaType.parse("application/json"), mapper.writeValueAsString(params));
		} catch (Exception e) {
			return null;
		}
	}

	public RequestBody rMobileDel(String mobile) {
		try {
			Map<String, Object> params = new TreeMap<>(Comparator.naturalOrder());
			params.put("token", CommonUtils.md5digest(keyscloud, mobile));
			params.put("mobile", mobile);
			return RequestBody.create(MediaType.parse("application/json"), mapper.writeValueAsString(params));
		} catch (Exception e) {
			return null;
		}
	}

	public RequestBody rBankTransfer(int frType, String account, BigDecimal amount, String nickName, String toAccount,
			String toAccountBank) {
		try {
			Map<String, Object> params = new TreeMap<>(Comparator.naturalOrder());
			params.put("account", account);
			params.put("frType", frType);
			params.put("amount", amount);
			params.put("nickname", nickName);
			params.put("toAccount", toAccount);
			params.put("toAccountBank", toAccountBank);
			params.put("token", CommonUtils.md5digest(keyscloud, params));
			return RequestBody.create(MediaType.parse("application/json"), mapper.writeValueAsString(params));
		} catch (Exception e) {
			return null;
		}
	}

	public RequestBody rBonusFindIncomeLogByAliAcc(String mobile, BigDecimal startAmt, BigDecimal endAmt,
			Long startTime, Long endTime, Integer pageSize, Integer pageNo) {
		try {
			Map<String, Object> params = new TreeMap<String, Object>() {
				{
					put("mobile", mobile);
					put("startAmt", startAmt);
					put("endAmt", endAmt);
					put("startTime", startTime);
					put("endTime", endTime);
					put("pageSize", pageSize);
					put("pageNo", pageNo);
				}
			};
			params.put("token", CommonUtils.md5digest(keyscloud, params));
			return RequestBody.create(MediaType.parse("application/json"), mapper.writeValueAsString(params));
		} catch (Exception e) {
			return null;
		}
	}

	public RequestBody rBonusFindIncomeLogByWecAcc(String mobile, BigDecimal startAmt, BigDecimal endAmt,
			Long startTime, Long endTime, Integer pageSize, Integer pageNo) {
		try {
			Map<String, Object> params = new TreeMap<String, Object>() {
				{
					put("mobile", mobile);
					put("endAmt", endAmt);
					put("startAmt", startAmt);
					put("startTime", startTime);
					put("endTime", endTime);
					put("pageSize", pageSize);
					put("pageNo", pageNo);
				}
			};
			params.put("token", CommonUtils.md5digest(keyscloud, params));
			return RequestBody.create(MediaType.parse("application/json"), mapper.writeValueAsString(params));
		} catch (Exception e) {
			return null;
		}
	}

	public RequestBody rBonusBouns(String mobile, BigDecimal startAmt, BigDecimal endAmt, Long startTime, Long endTime,
			Integer pageSize, Integer pageNo) {
		try {
			Map<String, Object> params = new TreeMap<String, Object>() {
				{
					put("endAmt", endAmt);
					put("mobile", mobile);
					put("startAmt", startAmt);
					put("startTime", startTime);
					put("endTime", endTime);
					put("pageSize", pageSize);
					put("pageNo", pageNo);
				}
			};
			params.put("token", CommonUtils.md5digest(keyscloud, params));
			return RequestBody.create(MediaType.parse("application/json"), mapper.writeValueAsString(params));
		} catch (Exception e) {
			return null;
		}
	}

	public RequestBody rBonusTotalForEach(List<String> mobileList) {
		try {
			Map<String, Object> params = new TreeMap<String, Object>() {
				{
					put("mobileList", mobileList);
					put("token", CommonUtils.md5digest(keyscloud, String.join(StringUtils.EMPTY, mobileList)));
				}
			};
			return RequestBody.create(MediaType.parse("application/json"), mapper.writeValueAsString(params));
		} catch (Exception e) {
			return null;
		}
	}

	public RequestBody rSysSavePro(Map<String, Object> proMap) {
		try {
			proMap.put("token", CommonUtils.md5digest(keyscloud, proMap));
			return RequestBody.create(MediaType.parse("application/json"), mapper.writeValueAsString(proMap));
		} catch (Exception e) {
			return null;
		}
	}

	public RequestBody generalWechatLogGet(String pageNo, List<String> handicapList, String wechatNumber,
			String startTime, String endTime, String pageSize) throws NoSuchAlgorithmException {
		String token = requestBodyParser.md5digest(handicapList + wechatNumber + startTime + endTime + keyscloud);
		String json = "{\"pageNo\":\"" + pageNo + "\",\"handicap\":\"" + handicapList + "\",\"wechatNumber\":\""
				+ wechatNumber + "\",\"startTime\":\"" + startTime + "\",\"endTime\":\"" + endTime
				+ "\",\"pageSize\":\"" + pageSize + "\",\"token\":\"" + token + "\"}";
		return RequestBody.create(MediaType.parse("application/json"), json);
	}

	public RequestBody generalMBAndInvoiceGet(String account, String startTime, String endTime, String member,
			String orderNo, BigDecimal fromAmount, BigDecimal toAmount, String payer, String invoicePageNo,
			String logPageNo, String PageSize) throws NoSuchAlgorithmException {
		String token = requestBodyParser.md5digest(
				account + startTime + endTime + member + orderNo + fromAmount + toAmount + payer + keyscloud);
		String json = "{\"account\":\"" + account + "\",\"startTime\":\"" + startTime + "\",\"endTime\":\"" + endTime
				+ "\",\"member\":\"" + member + "\",\"orderNo\":\"" + orderNo + "\",\"fromAmount\":\"" + fromAmount
				+ "\",\"toAmount\":\"" + toAmount + "\",\"payer\":\"" + payer + "\",\"invoicePageNo\":\""
				+ invoicePageNo + "\",\"logPageNo\":\"" + logPageNo + "\",\"pageSize\":\"" + PageSize
				+ "\",\"token\":\"" + token + "\"}";
		return RequestBody.create(MediaType.parse("application/json"), json);
	}

	public RequestBody findWechatMatched(int pageNo, List<String> handicapList, String startTime, String endTime,
			String member, String orderNo, BigDecimal fromAmount, BigDecimal toAmount, String wechatNumber, int status,
			String pageSize) throws NoSuchAlgorithmException {
		String token = requestBodyParser.md5digest(handicapList + startTime + endTime + member + orderNo + fromAmount
				+ toAmount + wechatNumber + keyscloud);
		String json = "{\"pageNo\":\"" + pageNo + "\",\"handicap\":\"" + handicapList + "\",\"startTime\":\""
				+ startTime + "\",\"endTime\":\"" + endTime + "\",\"member\":\"" + member + "\",\"orderNo\":\""
				+ orderNo + "\",\"fromAmount\":\"" + fromAmount + "\",\"toAmount\":\"" + toAmount
				+ "\",\"wechatNumber\":\"" + wechatNumber + "\",\"status\":\"" + status + "\",\"pageSize\":\""
				+ pageSize + "\",\"token\":\"" + token + "\"}";
		return RequestBody.create(MediaType.parse("application/json"), json);
	}

	public RequestBody findWechatUnClaim(int pageNo, List<String> handicapList, String startTime, String endTime,
			String account, BigDecimal fromAmount, BigDecimal toAmount, String pageSise)
			throws NoSuchAlgorithmException {
		String token = requestBodyParser
				.md5digest(handicapList + startTime + endTime + account + fromAmount + toAmount + keyscloud);
		String json = "{\"pageNo\":\"" + pageNo + "\",\"handicap\":\"" + handicapList + "\",\"startTime\":\""
				+ startTime + "\",\"endTime\":\"" + endTime + "\",\"account\":\"" + account + "\",\"fromAmount\":\""
				+ fromAmount + "\",\"toAmount\":\"" + toAmount + "\",\"pageSize\":\"" + pageSise + "\",\"token\":\""
				+ token + "\"}";
		return RequestBody.create(MediaType.parse("application/json"), json);
	}

	public RequestBody findWechatCanceled(int pageNo, List<String> handicapList, String startTime, String endTime,
			String member, String orderNo, BigDecimal fromAmount, BigDecimal toAmount, String pageSize)
			throws NoSuchAlgorithmException {
		String token = requestBodyParser
				.md5digest(handicapList + startTime + endTime + member + orderNo + fromAmount + toAmount + keyscloud);
		String json = "{\"pageNo\":\"" + pageNo + "\",\"handicap\":\"" + handicapList + "\",\"startTime\":\""
				+ startTime + "\",\"endTime\":\"" + endTime + "\",\"member\":\"" + member + "\",\"orderNo\":\""
				+ orderNo + "\",\"fromAmount\":\"" + fromAmount + "\",\"toAmount\":\"" + toAmount + "\",\"pageSize\":\""
				+ pageSize + "\",\"token\":\"" + token + "\"}";
		return RequestBody.create(MediaType.parse("application/json"), json);
	}

	public RequestBody generalStatisticalAliLogGet(int pageNo, List<String> handicapList, String AliNumber,
			String startTime, String endTime, String pageSize) throws NoSuchAlgorithmException {
		String token = requestBodyParser.md5digest(handicapList + AliNumber + startTime + endTime + keyscloud);
		String json = "{\"pageNo\":\"" + pageNo + "\",\"handicap\":\"" + handicapList + "\",\"aliNumber\":\""
				+ AliNumber + "\",\"startTime\":\"" + startTime + "\",\"endTime\":\"" + endTime + "\",\"pageSize\":\""
				+ pageSize + "\",\"token\":\"" + token + "\"}";
		return RequestBody.create(MediaType.parse("application/json"), json);
	}

	public RequestBody generalAliMBAndInvoiceGet(String account, String startTime, String endTime, String member,
			String orderNo, BigDecimal fromAmount, BigDecimal toAmount, String payer, int invoicePageNo, int logPageNo,
			String PageSize) throws NoSuchAlgorithmException {
		String token = requestBodyParser.md5digest(
				account + startTime + endTime + member + orderNo + fromAmount + toAmount + payer + keyscloud);
		String json = "{\"account\":\"" + account + "\",\"startTime\":\"" + startTime + "\",\"endTime\":\"" + endTime
				+ "\",\"member\":\"" + member + "\",\"orderNo\":\"" + orderNo + "\",\"fromAmount\":\"" + fromAmount
				+ "\",\"toAmount\":\"" + toAmount + "\",\"payer\":\"" + payer + "\",\"invoicePageNo\":\""
				+ invoicePageNo + "\",\"logPageNo\":\"" + logPageNo + "\",\"pageSize\":\"" + PageSize
				+ "\",\"token\":\"" + token + "\"}";
		return RequestBody.create(MediaType.parse("application/json"), json);
	}

	public RequestBody findAliMatched(int pageNo, List<String> handicapList, String startTime, String endTime,
			String member, String orderNo, BigDecimal fromAmount, BigDecimal toAmount, String aliPayNumber, int status,
			String pageSize) throws NoSuchAlgorithmException {
		String token = requestBodyParser.md5digest(handicapList + startTime + endTime + member + orderNo + fromAmount
				+ toAmount + aliPayNumber + keyscloud);
		String json = "{\"pageNo\":\"" + pageNo + "\",\"handicap\":\"" + handicapList + "\",\"startTime\":\""
				+ startTime + "\",\"endTime\":\"" + endTime + "\",\"member\":\"" + member + "\",\"orderNo\":\""
				+ orderNo + "\",\"fromAmount\":\"" + fromAmount + "\",\"toAmount\":\"" + toAmount
				+ "\",\"aliNumber\":\"" + aliPayNumber + "\",\"status\":\"" + status + "\",\"pageSize\":\"" + pageSize
				+ "\",\"token\":\"" + token + "\"}";
		return RequestBody.create(MediaType.parse("application/json"), json);
	}

	public RequestBody findAliCanceled(int pageNo, List<String> handicapList, String startTime, String endTime,
			String member, String orderNo, BigDecimal fromAmount, BigDecimal toAmount, String pageSize)
			throws NoSuchAlgorithmException {
		String token = requestBodyParser
				.md5digest(handicapList + startTime + endTime + member + orderNo + fromAmount + toAmount + keyscloud);
		String json = "{\"pageNo\":\"" + pageNo + "\",\"handicap\":\"" + handicapList + "\",\"startTime\":\""
				+ startTime + "\",\"endTime\":\"" + endTime + "\",\"member\":\"" + member + "\",\"orderNo\":\""
				+ orderNo + "\",\"fromAmount\":\"" + fromAmount + "\",\"toAmount\":\"" + toAmount + "\",\"pageSize\":\""
				+ pageSize + "\",\"token\":\"" + token + "\"}";
		return RequestBody.create(MediaType.parse("application/json"), json);
	}

	public RequestBody findAliUnClaim(int pageNo, List<String> handicapList, String startTime, String endTime,
			String account, BigDecimal fromAmount, BigDecimal toAmount, String pageSise)
			throws NoSuchAlgorithmException {
		String token = requestBodyParser
				.md5digest(handicapList + startTime + endTime + account + fromAmount + toAmount + keyscloud);
		String json = "{\"pageNo\":\"" + pageNo + "\",\"handicap\":\"" + handicapList + "\",\"startTime\":\""
				+ startTime + "\",\"endTime\":\"" + endTime + "\",\"account\":\"" + account + "\",\"fromAmount\":\""
				+ fromAmount + "\",\"toAmount\":\"" + toAmount + "\",\"pageSize\":\"" + pageSise + "\",\"token\":\""
				+ token + "\"}";
		return RequestBody.create(MediaType.parse("application/json"), json);
	}

	public RequestBody generalWechatAckGet(int sysRequestId, int bankFlowId, String matchRemark, String userName)
			throws NoSuchAlgorithmException {
		String token = requestBodyParser.md5digest(sysRequestId + bankFlowId + matchRemark + userName + keyscloud);
		String json = "{\"sysRequestId\":\"" + sysRequestId + "\",\"bankFlowId\":\"" + bankFlowId
				+ "\",\"matchRemark\":\"" + matchRemark + "\",\"userName\":\"" + userName + "\",\"token\":\"" + token
				+ "\"}";
		return RequestBody.create(MediaType.parse("application/json"), json);
	}

	public RequestBody generalCancelGet(int sysRequestId, String handicap, String orderNo, String remark,
			String userName) throws NoSuchAlgorithmException {
		String token = requestBodyParser.md5digest(sysRequestId + handicap + remark + userName + keyscloud);
		String json = "{\"sysRequestId\":\"" + sysRequestId + "\",\"handicap\":\"" + handicap + "\",\"matchRemark\":\""
				+ remark + "\",\"userName\":\"" + userName + "\",\"token\":\"" + token + "\"}";
		return RequestBody.create(MediaType.parse("application/json"), json);
	}

	public RequestBody generateRequestOrder(String memberAccount, BigDecimal amount, String account, String remark,
			String createTime, int bankLogId, String handicap, String userName, int type)
			throws NoSuchAlgorithmException {
		String token = requestBodyParser.md5digest(
				memberAccount + amount + account + remark + createTime + bankLogId + handicap + userName + keyscloud);
		String json = "{\"handicap\":\"" + handicap + "\",\"amount\":\"" + amount + "\",\"type\":\"" + type
				+ "\",\"userId\":\"" + memberAccount + "\",\"account\":\"" + account + "\",\"remark\":\"" + remark
				+ "\",\"saveTime\":\"" + createTime + "\",\"bankLogId\":\"" + bankLogId + "\",\"userName\":\""
				+ userName + "\",\"token\":\"" + token + "\"}";
		return RequestBody.create(MediaType.parse("application/json"), json);
	}

	public RequestBody generalUpTimeGet(int id, Date time) throws NoSuchAlgorithmException {
		SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String json = "{\"sysRequestId\":\"" + id + "\",\"updateTime\":\"" + sd.format(time) + "\"}";
		return RequestBody.create(MediaType.parse("application/json"), json);
	}

	public RequestBody generalRemarkGet(Long sysRequestId, String Remark, String type, String userName)
			throws NoSuchAlgorithmException {
		String token = requestBodyParser.md5digest(sysRequestId + Remark + type + userName + keyscloud);
		String json = "{\"sysRequestId\":\"" + sysRequestId + "\",\"matchRemark\":\"" + Remark + "\",\"type\":\"" + type
				+ "\",\"userName\":\"" + userName + "\",\"token\":\"" + token + "\"}";
		return RequestBody.create(MediaType.parse("application/json"), json);
	}

	public RequestBody generalCountReceiptsGet(int pageNo, String handicapCode, String account, String fristTime,
			String lastTime, String type, String pageSize) throws NoSuchAlgorithmException {
		String token = requestBodyParser.md5digest(handicapCode + account + fristTime + lastTime + type + keyscloud);
		String json = "{\"pageNo\":\"" + pageNo + "\",\"handicap\":\"" + handicapCode + "\",\"account\":\"" + account
				+ "\",\"startTime\":\"" + fristTime + "\",\"endTime\":\"" + lastTime + "\",\"type\":\"" + type
				+ "\",\"pageSize\":\"" + pageSize + "\",\"token\":\"" + token + "\"}";
		return RequestBody.create(MediaType.parse("application/json"), json);
	}

	public RequestBody generalSysDetail(int pageNo, String account, String fristTime, String lastTime,
			BigDecimal startamount, BigDecimal endamount, String orderNo, String type, String pageSize)
			throws NoSuchAlgorithmException {
		String token = requestBodyParser
				.md5digest(account + fristTime + lastTime + startamount + endamount + orderNo + type + keyscloud);
		String json = "{\"pageNo\":\"" + pageNo + "\",\"account\":\"" + account + "\",\"startTime\":\"" + fristTime
				+ "\",\"endTime\":\"" + lastTime + "\",\"fromAmount\":\"" + startamount + "\",\"toAmount\":\""
				+ endamount + "\",\"orderNo\":\"" + orderNo + "\",\"type\":\"" + type + "\",\"pageSize\":\"" + pageSize
				+ "\",\"token\":\"" + token + "\"}";
		return RequestBody.create(MediaType.parse("application/json"), json);
	}

}
