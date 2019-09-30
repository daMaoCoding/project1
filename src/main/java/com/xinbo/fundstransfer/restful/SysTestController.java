package com.xinbo.fundstransfer.restful;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/r/test")
public class SysTestController {
	@Autowired
	private AccountService accSer;
	private ObjectMapper mapper = new ObjectMapper();

	public String init(@RequestParam(value = "accId") Integer accId) {
		return null;
	}

	/**
	 * 入款卡：场景1
	 * <p>
	 * 一个会员充值54.01元，PC/手机工具抓取到银行流水，并正常上报到服务端
	 */
	@RequestMapping("/inbank/sc1")
	public String inbanksc1(@RequestParam(value = "accId") Integer accId) throws JsonProcessingException {
		try {
			BizAccount acc = accSer.getById(accId);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			return mapper
					.writeValueAsString(new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue()));
		}
	}

	/**
	 * 其他卡：场景1
	 * <p>
	 * 一个会员充值589.01元,且另一个会员充值2400.01元, PC/手机工具抓取到这两天银行流水,并正常上报到服务端
	 */
	@RequestMapping("/other/sc1")
	public String othersc1(@RequestParam(value = "accId") Integer accId) throws JsonProcessingException {
		try {
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			return mapper
					.writeValueAsString(new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue()));
		}
	}

}
