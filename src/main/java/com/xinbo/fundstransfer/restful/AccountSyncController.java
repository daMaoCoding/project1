package com.xinbo.fundstransfer.restful;

import java.util.*;

import javax.validation.Valid;

import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.component.net.http.RequestBodyParser;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizAccountSync;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.AccountSyncService;
import com.xinbo.fundstransfer.service.AllocateIncomeAccountService;

/**
 * 账号同步平台信息管理
 * 
 * @author 
 *
 */
@RestController
@RequestMapping("/r/accountSync")
public class AccountSyncController extends BaseController {
	private static final Logger logger = LoggerFactory.getLogger(AccountSyncController.class);
	@Autowired
	private AccountService accountService;
	@Autowired
	private AllocateIncomeAccountService incomeAccountAllocateService;
	@Autowired
	private AccountSyncService accountSyncService;

	private static ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * ' 保存设置信息
	 * 
	 * @param vo
	 * @return
	 * @throws JsonProcessingException
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@RequestMapping("/save")
	public String save(@Valid BizAccountSync vo) throws JsonProcessingException {
		String params = buildParams().toString();
		logger.trace(String.format("%s，参数：%s", "账号同步信息保存", params));
		try {
			GeneralResponseData<BizAccountSync> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			BizAccountSync accountSync = accountSyncService.findByAccountId(vo.getAccountId());
			// 解析json
			Object object = objectMapper.readValue(vo.getJson(), HashMap.class);
			if (null != object) {
				Map<String, Object> map = (Map<String, Object>) object;
				if (null != map) {
					Map<String, Object> info = (Map<String, Object>) map.get("info");
					RequestBodyParser requestBodyParser = new RequestBodyParser();
					String token = requestBodyParser.getmd5ByAccountHandicap(info.get("account").toString(),
							map.get("handicap").toString());
					if (null != map.get("token")) {
						map.remove("token");
					}
					map.put("token", token);
					vo.setJson(mapper.writeValueAsString(map));
					// 层级与账号关系存储
					List<Object> levelLists = (List<Object>) map.get("levelList");
					if (null != levelLists && levelLists.size() > 0) {
						List<String> levelCodes = new ArrayList();
						for (int i = 0; i < levelLists.size(); i++) {
							Map<String, Object> level = (Map<String, Object>) levelLists.get(i);
							if (null != level && null != level.get("levelCode")
									&& !level.get("levelCode").toString().equals("")) {
								levelCodes.add(level.get("levelCode").toString());
							}
						}
						Object handicapCode = map.get("handicap");
						if (null != handicapCode && !handicapCode.toString().equals("")) {
							// 存储层级关系
							accountSyncService.saveAccountLevelAndFlush(levelCodes, vo.getAccountId(),
									handicapCode.toString());
						} else {
							throw new Exception("操作失败，同步信息盘口不存在");
						}

					}
				}
			}
			if (null == accountSync || null == accountSync.getId()) {
				// 新增
				vo.setUpdateTime(new Date());
				vo.setOperator(operator.getUid());
				responseData.setData(accountSyncService.saveAndFlush(vo));
			} else {
				// 修改
				accountSync.setJson(vo.getJson());
				accountSync.setUpdateTime(new Date());
				accountSync.setOperator(operator.getUid());
				responseData.setData(accountSyncService.saveAndFlush(accountSync));
			}
			BizAccount newAccount = accountService.getById(vo.getAccountId());
			if (null != newAccount) {
				// 广播
				// accountService.broadCast(newAccount);
				// 分配
				incomeAccountAllocateService.update(newAccount.getId(), newAccount.getType(), newAccount.getStatus());
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(String.format("%s，参数：%s，结果：%s", "账号同步信息保存", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}

	}

	@RequestMapping("/getByAccountId")
	public String getByAccountId(@RequestParam(value = "accountId") Integer accountId) throws JsonProcessingException {
		String params = buildParams().toString();
		logger.trace(String.format("%s，参数：%s", "查询账号同步信息", params));
		try {
			GeneralResponseData<BizAccountSync> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			responseData.setData(accountSyncService.findByAccountId(accountId));
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error(String.format("%s，参数：%s，结果：%s", "查询账号同步信息", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询失败  " + e.getLocalizedMessage()));
		}

	}

}
