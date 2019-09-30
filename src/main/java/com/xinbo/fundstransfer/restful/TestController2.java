package com.xinbo.fundstransfer.restful;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.domain.entity.BizIncomeRequest;
import com.xinbo.fundstransfer.domain.entity.BizThirdRequest;
import com.xinbo.fundstransfer.service.AccountBindingService;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.IncomeRequestService;
import com.xinbo.fundstransfer.service.ThirdRequestService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author Administrator
 */
@RestController
@RequestMapping("/test")
public class TestController2 extends BaseController {

	@Autowired
	private AccountService accountService;
	@Autowired
	private AccountBindingService accountBindingService;
	@Autowired
	private ThirdRequestService thirdRequestService;

	@Autowired
	private IncomeRequestService incomeRequestService;
	@Autowired
	private HttpServletRequest httpRequest;

	@RequestMapping("/income2")
	public List<BizIncomeRequest> income2() {
		log.info("请求 线程:{}", Thread.currentThread().getId());
		String orderNo = httpRequest.getParameter("orderNo");
		String handicap = httpRequest.getParameter("handicap");
		String type = httpRequest.getParameter("type");
		String amount = httpRequest.getParameter("amount");
		String[] toAccount = httpRequest.getParameter("toAccount").split("#");
		String[] str = new String[toAccount.length + 4];
		str[0] = orderNo;
		str[1] = handicap;
		str[2] = type;
		str[3] = amount;
		for (int i = 0; i < toAccount.length; i++) {
			str[4 + i] = toAccount[i];
		}

		List<BizIncomeRequest> res = incomeRequestService.findByCacheStrMultiToAccount(str);
		return res;

	}

	@RequestMapping("/income")
	public BizIncomeRequest income() {
		log.info("请求 线程:{}", Thread.currentThread().getId());
		String orderNo = httpRequest.getParameter("orderNo");
		String handicap = httpRequest.getParameter("handicap");
		String type = httpRequest.getParameter("type");
		String amount = httpRequest.getParameter("amount");
		BizIncomeRequest res = incomeRequestService.findOneByCacheStr(new String[] { orderNo, handicap, type, amount });
		return res;

	}

	@RequestMapping("/third")
	public BizThirdRequest third() {
		log.info("请求 线程:{}", Thread.currentThread().getId());
		String orderNo = httpRequest.getParameter("orderNo");
		String handicap = httpRequest.getParameter("handicap");
		BizThirdRequest res = thirdRequestService.findOneByCacheStr(orderNo + "#" + handicap);
		return res;

	}

	@RequestMapping("/addNeed")
	public String addNeed() {
		log.info("请求 线程:{}", Thread.currentThread().getId());
		String accountId = httpRequest.getParameter("accountId");
		String needAmount = httpRequest.getParameter("needAmount");
		String accountType = httpRequest.getParameter("accountType");
		accountService.addNeedThirdDrawToOutCardList(Integer.valueOf(accountId), Integer.valueOf(needAmount),
				Integer.valueOf(accountType));
		return "OK";

	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown = true)
	static class BindInputDTO {
		private List<Integer> issueAccountId;
		private Integer incomeAccountId;
	}

	@PostMapping("/bind")
	public String bind(@RequestBody BindInputDTO inputDTO) {

		// List<Integer> issueAccountId, Integer incomeAccountId, Integer bind1Unbind0
		try {
			accountBindingService.bindOrUnbind(inputDTO.getIssueAccountId(), inputDTO.getIncomeAccountId(), 1);
		} catch (Exception e) {
			e.printStackTrace();
			return e.getLocalizedMessage();
		}
		return "ok";
	}

	@RequestMapping("/unbind")
	public String unbind(@RequestBody BindInputDTO inputDTO) {
		try {
			accountBindingService.bindOrUnbind(inputDTO.getIssueAccountId(), inputDTO.getIncomeAccountId(), 0);
		} catch (Exception e) {
			e.printStackTrace();
			return e.getLocalizedMessage();
		}
		return "ok";
	}

	@RequestMapping("/lock")
	public String lock() {

		// operator.getId(), toId, fromId
		String userId = httpRequest.getParameter("userId");
		String toId = httpRequest.getParameter("toId");
		String fromId = httpRequest.getParameter("fromId");
		try {
			Long res = accountService.lockOutCardNeedAmount(Integer.valueOf(userId), Integer.valueOf(toId),
					Integer.valueOf(fromId));
			return res.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return "fail";
		}
	}

	@RequestMapping("/unlock")
	public String unlock() {

		// operator.getId(), toId, fromId
		String userId = httpRequest.getParameter("userId");
		String toId = httpRequest.getParameter("toId");
		String fromId = httpRequest.getParameter("fromId");
		try {
			Long res = accountService.unlockedThirdToDrawList(Integer.valueOf(userId), Integer.valueOf(toId),
					Integer.valueOf(fromId));
			return res.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return "fail";
		}
	}
}
