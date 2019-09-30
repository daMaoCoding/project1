package com.xinbo.fundstransfer.restful;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.net.socket.AccountEntity;
import com.xinbo.fundstransfer.component.net.socket.ActionEventEnum;
import com.xinbo.fundstransfer.component.net.socket.MessageEntity;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.domain.DynamicSpecifications;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.entity.SysUserProfile;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.CabanaService;
import com.xinbo.fundstransfer.service.HostMonitorService;

import com.xinbo.fundstransfer.service.SystemSettingService;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.types.RedisClientInfo;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/r/host")
public class HostMonitorController extends BaseController {
	@Autowired
	CabanaService cabanaService;
	@Autowired
	AccountService accountService;
	@Autowired
	HostMonitorService hostMonitorService;
	@Autowired
	private SystemSettingService systemSettingService;

	@RequestMapping("/alloc")
	public String alloc() throws JsonProcessingException {
		Set<String> allHost = new HashSet<>();
		List<RedisClientInfo> clientList = redisService.getStringRedisTemplate().getClientList();
		clientList.forEach((p -> allHost.add(p.get(RedisClientInfo.INFO.ADDRESS_PORT).split(":")[0])));
		String originHosts = redisService.getStringRedisTemplate().boundValueOps(RedisKeys.ONLINE_CLUSTER_HOST).get();
		String hosts = null;
		if (Objects.nonNull(originHosts)) {
			for (String host : originHosts.split(",")) {
				hosts = allHost.contains(host) ? (hosts == null ? host : hosts + "," + host) : hosts;
			}
		}
		Map<String, String> allocRec = new HashMap<>();
		allocRec.put("allHost", String.join(",", allHost));
		allocRec.put("originHosts", originHosts);
		allocRec.put("nativeHost", CommonUtils.getInternalIp());
		allocRec.put("hosts", StringUtils.trimToEmpty(hosts));
		try {
			GeneralResponseData<Map<String, String>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			responseData.setData(allocRec);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/list")
	public String list(@RequestParam(value = "host", required = false) String host,
			@RequestParam(value = "accountLike", required = false) String accountLike,
			@RequestParam(value = "statusArray", required = false) Integer[] statusArray)
			throws JsonProcessingException {
		try {
			GeneralResponseData<List<Map<String, Object>>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			List<Map<String, Object>> result = hostMonitorService.list(host, accountLike, statusArray);
			responseData.setData(result);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/findAccountListOfHost")
	public String findAccountListOfHost(@RequestParam(value = "host") String host) throws JsonProcessingException {
		try {
			GeneralResponseData<List<AccountEntity>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			responseData.setData(hostMonitorService.findAccountEntityList(host));
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/addAccountToHost")
	public String addAccountToHost(@RequestParam(value = "host") String host,
			@RequestParam(value = "accountId") int accountId) throws JsonProcessingException {
		try {
			hostMonitorService.addAccountToHost(host, accountId);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/removeAccountFromHost")
	public String removeAccountFromHost(@RequestParam(value = "host") String host,
			@RequestParam(value = "accountId") int accountId) throws JsonProcessingException {
		try {
			hostMonitorService.removeAccountFromHost(host, accountId);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/alterSignAndHook")
	public String alterSignAndHook(@RequestParam(value = "host") String host,
			@RequestParam(value = "accountId") int accountId, @RequestParam(value = "sign") String sign,
			@RequestParam(value = "hook") String hook, @RequestParam(value = "hub") String hub,
			@RequestParam(value = "bing") String bing, @RequestParam(value = "bankType") String bankType,
			@RequestParam(value = "interval") Integer interval) throws JsonProcessingException {
		try {
			BizAccount account = accountService.getById(accountId);
			if (StringUtils.isNotBlank(bankType) && !StringUtils.equals(account.getBankType(), bankType)) {
				account.setBankType(bankType);
				accountService.updateBaseInfo(account);
			}
			hostMonitorService.alterSignAndHook(host, accountId, sign, hook, hub, bing, interval);
			cabanaService.updAcc(accountId);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/alterPWD4Trans")
	public String alterPWD4Trans(@RequestParam(value = "accountId") int accountId,
			@RequestParam(value = "sign") String sign, @RequestParam(value = "hook") String hook,
			@RequestParam(value = "hub") String hub, @RequestParam(value = "bing", required = false) String bing)
			throws JsonProcessingException {
		try {
			if (StringUtils.isAnyBlank(sign, hook, hub)) {
				return mapper.writeValueAsString(new GeneralResponseData<>(
						GeneralResponseData.ResponseStatus.FAIL.getValue(), "录账号/密码或支付密码为空."));
			}
			hostMonitorService.alterSignAndHook(accountId, sign, hook, hub, bing);
			cabanaService.updAcc(accountId);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/alterPWD4Supervisor")
	public String alterPWD4Supervisor(@RequestParam(value = "accountId") int accountId,
			@RequestParam(value = "sign") String sign, @RequestParam(value = "hook") String hook,
			@RequestParam(value = "hub") String hub, @RequestParam(value = "bing", required = false) String bing)
			throws JsonProcessingException {
		try {
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (Objects.isNull(operator)) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆"));
			}
			List<SysUserProfile> ipList = systemSettingService.findByPropertyKey("SUPERVISOR_OPEN_ACCOUNT_PIN_IPS");
			String permIps = !CollectionUtils.isEmpty(ipList) ? ipList.get(0).getPropertyValue() : StringUtils.EMPTY;
			String clientIp = CommonUtils.getRemoteIp(request);
			boolean checkIp = StringUtils.isNotBlank(permIps) && StringUtils.isNotBlank(clientIp)
					&& permIps.contains(clientIp);
			if (!checkIp) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "没有权限修改"));
			}
			if (StringUtils.isBlank(sign) && StringUtils.isBlank(hook) && StringUtils.isBlank(hub)) {
				throw new Exception("登录账号,密码,支付密码不能同时为空.");
			}
			hostMonitorService.alterSignAndHook(accountId, sign, hook, hub, bing);
			cabanaService.updAcc(accountId);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/updateIinterval")
	public String updateIinterval(@RequestParam(value = "host") String host,
			@RequestParam(value = "accountId") int accountId, @RequestParam(value = "interval") Integer interval)
			throws JsonProcessingException {
		try {
			hostMonitorService.updateIinterval(host, accountId, interval);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/startByCommand")
	public String startByCommand(@RequestParam(value = "accountId") int accountId) throws JsonProcessingException {
		try {
			hostMonitorService.startByCommand(accountId);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/stopByCommand")
	public String stopByCommand(@RequestParam(value = "accountId") int accountId) throws JsonProcessingException {
		try {
			hostMonitorService.stopByCommand(accountId);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/pauseByCommand")
	public String pauseByCommand(@RequestParam(value = "accountId") int accountId) throws JsonProcessingException {
		try {
			hostMonitorService.pauseByCommand(accountId);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/resumeByCommand")
	public String resumeByCommand(@RequestParam(value = "accountId") int accountId) throws JsonProcessingException {
		try {
			hostMonitorService.resumeByCommand(accountId);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/changeMode")
	public String changeMode(@RequestParam(value = "accountId") int accountId,
			@RequestParam(value = "modeType") int modeType) throws JsonProcessingException {
		try {

			if (modeType == 18 || modeType == 19 || modeType == 20) {
				switch (modeType) {
				case 18:
					hostMonitorService.changeMode(accountId, ActionEventEnum.NORMALMODE);
					break;
				case 19:
					hostMonitorService.changeMode(accountId, ActionEventEnum.CAPTUREMODE);
					break;
				case 20:
					hostMonitorService.changeMode(accountId, ActionEventEnum.TRANSMODE);
					break;
				}
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
			} else {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
								"操作失败,指令modeType" + modeType + "不在18/19/20值范围"));
			}
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	// @RequestMapping("/set")
	// public String set(@RequestParam(value = "host") String host,
	// @RequestParam(value = "interval") int interval)
	// throws JsonProcessingException {
	// try {
	// hostMonitorService.set(host, interval);
	// return mapper.writeValueAsString(
	// new
	// GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
	// } catch (Exception e) {
	// return mapper.writeValueAsString(new GeneralResponseData<>(
	// GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败 " +
	// e.getLocalizedMessage()));
	// }
	// }

	@RequestMapping("/getmessageEntity")
	public String getMessageEntity(@RequestParam(value = "host") String host) throws JsonProcessingException {
		try {
			GeneralResponseData<MessageEntity<List<AccountEntity>>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			responseData.setData(hostMonitorService.getMessageEntity(host));
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/delete")
	public String delete(@RequestParam(value = "host") String host) throws JsonProcessingException {
		try {
			hostMonitorService.shutdown(host);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * @param host
	 * @param accountType
	 * @param currSysLevel
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/updateHostType")
	public String updateHostType(@RequestParam(value = "host") String host,
			@RequestParam(value = "accountType") Integer accountType,
			@RequestParam(value = "currSysLevel") Integer currSysLevel) throws JsonProcessingException {
		try {
			hostMonitorService.updateHostType(host, accountType, currSysLevel);
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}
	}
}
