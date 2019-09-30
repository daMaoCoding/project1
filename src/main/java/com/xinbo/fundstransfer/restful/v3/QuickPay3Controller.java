package com.xinbo.fundstransfer.restful.v3;

import java.util.Date;
import javax.validation.Valid;
import com.xinbo.fundstransfer.restful.v3.pojo.QuickPayV3Acc;
import com.xinbo.fundstransfer.sec.FundTransferEncrypter;
import com.xinbo.fundstransfer.service.AccountMoreService;
import com.xinbo.fundstransfer.service.QuickPayService;
import com.xinbo.fundstransfer.service.RedisService;
import org.apache.commons.lang3.StringUtils;
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
import com.xinbo.fundstransfer.domain.entity.BizAccountMore;
import com.xinbo.fundstransfer.domain.entity.BizOtherAccount;
import com.xinbo.fundstransfer.domain.enums.OtherAccountStatus;

@RestController
@RequestMapping("/api/v3")
@SuppressWarnings("WeakAccess unused")
public class QuickPay3Controller {
	private static final Logger log = LoggerFactory.getLogger(QuickPay3Controller.class);
	private ObjectMapper mapper = new ObjectMapper();
	@Autowired
	private AppProperties appProperties;
	@Autowired
	private AccountMoreService accMoreSer;
	@Autowired
	private QuickPayService quickPayService;
	@Autowired
	private RedisService redisSer;

	@RequestMapping("/flushOtherAccount")
	public String flush() throws JsonProcessingException {
		try {
			redisSer.convertAndSend(RedisTopics.OTHER_ACCOUNT_CLEAN, StringUtils.EMPTY);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			return mapper
					.writeValueAsString(new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue()));
		}
	}

	@RequestMapping(value = "/quickpay", method = RequestMethod.POST, consumes = "application/json")
	public String put(@Valid @RequestBody QuickPayV3Acc requestBody, BindingResult result)
			throws JsonProcessingException {
		String bodyJson = mapper.writeValueAsString(requestBody);
		log.info("quickpay >> RequestBody:{}", bodyJson);
		if (result.hasErrors()) {
			log.info("quickpayError >> invalid params. RequestBody:{}", bodyJson);
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验失败。"));
		}
		if (!checkToken(requestBody)) {
			log.info("quickpayError >> token校验失败. RequestBody:{}", bodyJson);
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "token校验失败。"));
		}

		BizAccountMore more = accMoreSer.getFromByUid(requestBody.getUid());
		if (null == more) {
			log.info("quickpayError >> 兼职不存在. RequestBody:{}", bodyJson);
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "兼职不存在。"));
		}
		if (!more.getMoible().equals(requestBody.getAccountNo()) && requestBody.getFlag() == 0) {
			log.info("quickpayError >> 云闪付账号需要和手机号保存一致. RequestBody:{}", bodyJson);
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "云闪付账号需要和手机号保存一致。"));
		}
		// 新增
		try {
			if (requestBody.getFlag() == 0) {
				BizOtherAccount ysfAccount = quickPayService.getFromCacheByAccountNo(requestBody.getAccountNo());
				if (null != ysfAccount) {
					log.info("quickpayError >> 云闪付账号已经存在，不能新增   RequestBody:{}", bodyJson);
					return mapper.writeValueAsString(new GeneralResponseData(
							GeneralResponseData.ResponseStatus.FAIL.getValue(), "云闪付账号已经存在，不能新增。"));
				}
				BizOtherAccount ysf = new BizOtherAccount();
				ysf.setAccountNo(requestBody.getAccountNo());
				ysf.setOwner(requestBody.getOwner());
				ysf.setLoginPwd(
						FundTransferEncrypter.encryptDb(StringUtils.trimToEmpty(deLpwd(requestBody.getLoginPwd()))));
				ysf.setPayPwd(
						FundTransferEncrypter.encryptDb(StringUtils.trimToEmpty(deTpwd(requestBody.getPayPwd()))));
				ysf.setCreater("返利网");
				ysf.setOwnType(2);
				ysf.setType(1);
				ysf.setStatus(OtherAccountStatus.Inactivated.getStatus());
				ysf.setHandicapId(more.getHandicap());
				ysf.setUid(Integer.parseInt(requestBody.getUid()));
				ysf.setCreateTime(new Date());
				BizOtherAccount ysfAcc = quickPayService.save(ysf);
				quickPayService.flushCache(ysfAcc);
			} else if (requestBody.getFlag() == 1) {// 修改
				BizOtherAccount ysfAcc = quickPayService.getByUid(Integer.parseInt(requestBody.getUid()));
				if (null == ysfAcc) {
					log.info("quickpayError >> 云闪付账号绑定的兼职不正确   RequestBody:{}", bodyJson);
					return mapper.writeValueAsString(new GeneralResponseData(
							GeneralResponseData.ResponseStatus.FAIL.getValue(), "云闪付账号绑定的兼职不正确。"));
				}
				// 未激活
				if (ysfAcc.getStatus() == 3) {
					ysfAcc.setAccountNo(requestBody.getAccountNo());
				}
				if (StringUtils.isNotBlank(requestBody.getOwner())) {
					ysfAcc.setOwner(requestBody.getOwner());
				}
				if (StringUtils.isNotBlank(requestBody.getLoginPwd())) {
					ysfAcc.setLoginPwd(FundTransferEncrypter
							.encryptDb(StringUtils.trimToEmpty(deLpwd(requestBody.getLoginPwd()))));
				}
				if (StringUtils.isNotBlank(requestBody.getPayPwd())) {
					ysfAcc.setPayPwd(
							FundTransferEncrypter.encryptDb(StringUtils.trimToEmpty(deTpwd(requestBody.getPayPwd()))));
				}
				ysfAcc.setOperator("返利网");
				ysfAcc.setUpdateTime(new Date());
				BizOtherAccount acc = quickPayService.save(ysfAcc);
				quickPayService.flushCache(acc);
			} else if (requestBody.getFlag() == 2) {// 删除
				BizOtherAccount ysfAcc = quickPayService.getFromCacheByAccountNo(requestBody.getAccountNo());
				// 只有未激活的账号才能删除
				if (null != ysfAcc && ysfAcc.getStatus() == OtherAccountStatus.Inactivated.getStatus()) {
					quickPayService.deleteById(ysfAcc.getId(), ysfAcc.getAccountNo());
				} else {
					log.info("quickpayError >> 云闪付账号已经激活不能删除   RequestBody:{}", bodyJson);
					return mapper.writeValueAsString(new GeneralResponseData(
							GeneralResponseData.ResponseStatus.FAIL.getValue(), "云闪付账号已经激活不能删除。"));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.info("quickpayError >>  error. RequestBody:{}", bodyJson, e.getMessage());
			return mapper.writeValueAsString(
					new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(), "内部错误。"));
		}

		return mapper.writeValueAsString(
				new GeneralResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "成功"));
	}

	private boolean checkToken(QuickPayV3Acc arg0) {
		if (StringUtils.isBlank(arg0.getToken())) {
			log.info("quickpayToken  >> token param is null.");
			return false;
		}
		String oriContent = StringUtils.trimToEmpty(arg0.getUid()) + StringUtils.trimToEmpty(arg0.getAccountNo())
				+ StringUtils.trimToEmpty(arg0.getOwner()) + StringUtils.trimToEmpty(arg0.getLoginPwd())
				+ StringUtils.trimToEmpty(arg0.getPayPwd()) + arg0.getFlag();

		String calToken = CommonUtils.md5digest(oriContent + appProperties.getRebatesalt());
		if (StringUtils.equals(calToken, arg0.getToken()))
			return true;
		log.info(
				"quickpayInvalidToken>>   uid: {} accountNo: {} owner: {}  loginPwd: {} payPwd: {}  flag: {} status: {}  token:{}",
				arg0.getUid(), arg0.getAccountNo(), arg0.getOwner(), arg0.getLoginPwd(), arg0.getPayPwd(),
				arg0.getFlag(), arg0.getStatus(), calToken);
		return false;
	}

	/*
	 * loginPwd 在第5位插入3个随机0-9的数字 然后用base64加密
	 */
	private String deLpwd(String lped) {
		if (StringUtils.isBlank(lped))
			return null;
		lped = new String(org.apache.mina.util.Base64.decodeBase64(lped.getBytes()));
		return lped.substring(0, 5) + lped.substring(8);
	}

	/*
	 * payPwd 在第2位插入一个随机0-9的数字 然后用base64加密
	 */
	private String deTpwd(String tpwd) {
		if (StringUtils.isBlank(tpwd))
			return null;
		tpwd = new String(org.apache.mina.util.Base64.decodeBase64(tpwd.getBytes()));
		return tpwd.substring(0, 2) + tpwd.substring(3);
	}
}
