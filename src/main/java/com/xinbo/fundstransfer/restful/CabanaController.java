package com.xinbo.fundstransfer.restful;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.service.CabanaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
@RequestMapping("/r/cabana")
@SuppressWarnings("WeakerAccess unused")
public class CabanaController extends BaseController {
	@Autowired
	private CabanaService cabanaService;
	private ObjectMapper mapper = new ObjectMapper();

	@RequestMapping("/status")
	public String status(@RequestParam(value = "accIdArray") Integer[] accIdArray) throws JsonProcessingException {
		return mapper.writeValueAsString(cabanaService.status(Arrays.asList(accIdArray)));
	}

	@RequestMapping("/login")
	public String login(@RequestParam(value = "accId") Integer accId) throws JsonProcessingException {
		return mapper.writeValueAsString(cabanaService.login(accId));
	}

	@RequestMapping("/conciliate")
	public String conciliate(@RequestParam(value = "accId") Integer accId, @RequestParam(value = "date") String date)
			throws JsonProcessingException {
		return mapper.writeValueAsString(cabanaService.conciliate(accId, date));
	}
	
	@RequestMapping("/getCacheFlow")
	public String getCacheFlow(@RequestParam(value = "accId") Integer accId, @RequestParam(value = "date") String date)
			throws JsonProcessingException {
		return mapper.writeValueAsString(cabanaService.getCacheFlow(accId, date));
	}

	/**
	 * 描述 :向cabana发起抓流水请求(APP请求指令201)
	 *
	 * @param accId
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/conciliateINCR")
	public String conciliateINCR(@RequestParam(value = "accId") Integer accId) throws JsonProcessingException {
		return mapper.writeValueAsString(cabanaService.conciliateINCR(accId));
	}

	@RequestMapping("/reAck")
	public String reAck(@RequestParam(value = "accId") Integer accId) throws JsonProcessingException {
		return mapper.writeValueAsString(cabanaService.reAck(accId));
	}

	@RequestMapping("/logs")
	public String logs(@RequestParam(value = "accId") Integer accId, @RequestParam(value = "date") String date)
			throws JsonProcessingException {
		return mapper.writeValueAsString(cabanaService.logs(accId, date));
	}

	@RequestMapping("/screen")
	public String screen(@RequestParam(value = "accId") Integer accId) throws JsonProcessingException {
		return mapper.writeValueAsString(cabanaService.screen(accId));
	}

	@RequestMapping("/error")
	public String error(@RequestParam(value = "accId") Integer accId) throws JsonProcessingException {
		return mapper.writeValueAsString(cabanaService.error(accId));
	}

	@RequestMapping("/hisSMS")
	public String hisSMS(@RequestParam(value = "mobile") String mobile) throws JsonProcessingException {
		return mapper.writeValueAsString(cabanaService.hisSMS(mobile));
	}

	@RequestMapping("/logLevel")
	public String logLevel(@RequestParam(value = "level", required = false) String level) throws Exception {
		return mapper.writeValueAsString(cabanaService.logLevel(level));
	}
}
