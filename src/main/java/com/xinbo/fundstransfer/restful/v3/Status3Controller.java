package com.xinbo.fundstransfer.restful.v3;

import javax.validation.Valid;

import com.xinbo.fundstransfer.restful.v3.pojo.ReqV3Status;
import com.xinbo.fundstransfer.service.CabanaService;
import org.apache.commons.lang3.StringUtils;
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
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.HandicapService;

@RestController
@RequestMapping("/api/v3")
@SuppressWarnings("WeakAccess unused")
public class Status3Controller extends Base3Common {
	private static final Logger log = LoggerFactory.getLogger(Status3Controller.class);
	private ObjectMapper mapper = new ObjectMapper();
	@Autowired
	private AppProperties appProperties;
	@Autowired
	private AccountService accountService;
	@Autowired
	private HandicapService handicapService;
	@Autowired
	private CabanaService cabanaService;

	/**
	 * add / update bank account information.
	 */
	@RequestMapping(value = "/status", method = RequestMethod.POST, consumes = "application/json")
	@ResponseBody
	public Object status(@Valid @RequestBody ReqV3Status requestBody, BindingResult result)
			throws JsonProcessingException {
		String paramBody = mapper.writeValueAsString(requestBody);
		log.trace("StatusV3 >> RequestBody:{}", paramBody);
		if (result.hasErrors()) {
			log.debug("StatusV3 >> invalid params. RequestBody:{}", paramBody);
			return ERROR_PARAM_INVALID;
		}
		if (!checkToken(requestBody))
			return ERROR_TOKEN_INVALID;
		return cabanaService.statusByAcc(requestBody.getAccList());
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
	private String deMobile(String mobile) {
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
	private boolean checkToken(ReqV3Status arg0) {
		if (StringUtils.isBlank(arg0.getToken()) || CollectionUtils.isEmpty(arg0.getAccList())) {
			log.debug("StatusV3 ( invalid token ) >> token param is null. ");
			return false;
		}
		String oriContent = String.join(StringUtils.EMPTY, arg0.getAccList());
		String calToken = CommonUtils.md5digest(oriContent + appProperties.getRebatesalt());
		if (StringUtils.equals(calToken, arg0.getToken()))
			return true;
		log.debug("StatusV3 ( invalid token ) >>   accountList: {} oriContent: {}  oriToken: {} calToekn: {}",
				String.join(",", arg0.getAccList()), oriContent, arg0.getToken(), calToken);
		return false;
	}
}
