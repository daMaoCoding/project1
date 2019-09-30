package com.xinbo.fundstransfer.restful;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.component.net.http.RequestBodyParser;
import com.xinbo.fundstransfer.component.net.http.cloud.HttpClientCloud;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.BizLevel;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.service.HandicapService;
import com.xinbo.fundstransfer.service.LevelService;

import okhttp3.MediaType;
import okhttp3.RequestBody;

/**
 * Created by Owner on 2018/5/28. 新支付银行卡 微信 支付宝 交易明细查询类
 */
@RestController
@RequestMapping(value = "/r/newPay")
public class NewPayTradeInfoController extends BaseController {
	@Autowired
	private LevelService levelService;
	@Autowired
	private HandicapService handicapService;
	@Autowired
	private ObjectMapper mapper;
	@Autowired
	private RequestBodyParser requestBodyParser;
	@Autowired
	private Environment environment;

	/**
	 *
	 * @param mobile
	 * @param type
	 *            银行卡 1 支付宝 2 微信 3
	 * @param account
	 * @return
	 */
	@RequestMapping("/accout/launchCheck")
	public String launchCheck(@RequestParam(value = "mobile") String mobile, @RequestParam(value = "type") Integer type,
			@RequestParam(value = "account") String account) throws Exception {
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			GeneralResponseData<Object> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		if (StringUtils.isBlank(mobile) || StringUtils.isBlank(account) || type == null) {
			GeneralResponseData<Object> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"参数缺失!mobile:" + mobile + ",account:" + account + ",type:" + type);
			return mapper.writeValueAsString(responseData);
		}
		GeneralResponseData<Object> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "发起对账通知成功");
		String json = wrapJson4LaunchCheck(mobile, account, type);
		RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
		HttpClientCloud.getInstance().getCloudService().launchCheck(body).subscribe(res -> {
			log.info("launchCheck res:{}", res);
			JSONObject jsonObject;
			try {
				jsonObject = new JSONObject(JSONObject.wrap(res).toString());
				responseData.setMessage("通知成功:" + jsonObject.get("message"));
			} catch (JSONException e) {
				log.info("launchCheck 通知失败:{}", e.getStackTrace());
				responseData.setMessage("通知失败:" + e.getMessage());
			}
		});
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * @param type
	 *            银行卡 1 支付宝 2 微信 3 accountType 1 自用 2 客户 syslevel 1，2，4内外中
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/account/search")
	public String search(@RequestParam(value = "handicap") String handicap,
			@RequestParam(value = "level", required = false) Integer level,
			@RequestParam(value = "account", required = false) String account,
			@RequestParam(value = "accountType", required = false) String accountType,
			@RequestParam(value = "holder", required = false) String holder,
			@RequestParam(value = "owner", required = false) String owner,
			@RequestParam(value = "bankName", required = false) String bankName,
			@RequestParam(value = "status") String status, @RequestParam(value = "type") Integer type,
			@RequestParam(value = "syslevel", required = false) String syslevel,
			@RequestParam(value = "timeStart", required = false) String timeStart,
			@RequestParam(value = "timeEnd", required = false) String timeEnd,
			@RequestParam(value = "pageNo", required = false) Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws Exception {
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			GeneralResponseData<Object> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		GeneralResponseData<Object> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
		String json = wrapJsonStr(handicap, level, account, accountType, holder, owner, bankName, status, type,
				syslevel, pageNo, pageSize, timeStart, timeEnd);
		log.info("json :{}", json);
		RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
		HttpClientCloud.getInstance().getCloudService().search(body).subscribe(res -> {
			log.info("search res:{}", res);
			responseData.setData(res);
			responseData.setPage(new Paging());
		});
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * @param type
	 *            银行卡 1 支付宝 2 微信 3
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/account/searchCount")
	public String searchCount(@RequestParam(value = "handicap") String handicap,
			@RequestParam(value = "account", required = false) String account,
			@RequestParam(value = "accountType", required = false) String accountType,
			@RequestParam(value = "status") String status, @RequestParam(value = "type") Integer type,
			@RequestParam(value = "syslevel", required = false) String syslevel,
			@RequestParam(value = "timeStart", required = false) String timeStart,
			@RequestParam(value = "timeEnd", required = false) String timeEnd,
			@RequestParam(value = "pageNo", required = false) Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws Exception {
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			GeneralResponseData<Object> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		String json = wrapJsonStr(handicap, null, account, accountType, null, null, null, status, type, syslevel,
				pageNo, pageSize, timeStart, timeEnd);
		GeneralResponseData<Object> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
		RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
		HttpClientCloud.getInstance().getCloudService().searchCount(body).subscribe((Object res) -> {
			log.info("searchCount res:{}", res);
			JSONObject jsonObject;
			JSONObject jsonObject1;
			try {
				jsonObject = new JSONObject(JSONObject.wrap(res).toString());
				jsonObject.get("status");
				jsonObject.get("message");
				Object data = jsonObject.get("data");
				// data.toString();
				jsonObject1 = new JSONObject(JSONObject.wrap(data).toString());
				Integer count = (Integer) jsonObject1.get("count");
				log.info("searchCount status:{},message:{},data:{},count:{}", jsonObject.get("status"),
						jsonObject.get("message"), jsonObject.get("data"), jsonObject1.get("count"));
				Paging page;
				if (count != null) {
					page = CommonUtils.getPage(pageNo + 1, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
							String.valueOf(count));
				} else {
					page = CommonUtils.getPage(0, pageSize != null ? pageSize : AppConstants.PAGE_SIZE, "0");
				}
				Map map = new HashMap<>();
				if (type == 1) {
					BigDecimal sum = new BigDecimal(jsonObject1.get("sum").toString());
					map.put("sum", sum);
				}
				responseData.setData(map);
				responseData.setPage(page);
			} catch (JSONException e) {
				e.printStackTrace();
			}

		});
		return mapper.writeValueAsString(responseData);
	}

	/**
	 * @param type
	 *            银行卡 1 支付宝 2 微信 3
	 * @param account
	 *            输入的账号
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/trade/searchDetail")
	public String searchDetail(@RequestParam(value = "handicap") String handicap,
			@RequestParam(value = "level", required = false) Integer level,
			@RequestParam(value = "account", required = false) String account,
			@RequestParam(value = "accountF") String accountF, @RequestParam(value = "type") Integer type,
			@RequestParam(value = "member", required = false) String member,
			@RequestParam(value = "orderNo", required = false) String orderNo,
			@RequestParam(value = "status") String status, @RequestParam(value = "outIn") Integer outIn,
			@RequestParam(value = "timeStart", required = false) String timeStart,
			@RequestParam(value = "timeEnd", required = false) String timeEnd,
			@RequestParam(value = "amountFr", required = false) String amountFr,
			@RequestParam(value = "amountTo", required = false) String amountTo,
			@RequestParam(value = "pageNo", required = false) Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws Exception {
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			GeneralResponseData<Object> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		GeneralResponseData<Object> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
		String json = wrapJsonStrForDetail(handicap, level, accountF, account, null, member, amountFr, amountTo,
				timeEnd, timeStart, status, type, outIn, orderNo, pageNo, pageSize);
		log.info("json :{}", json);
		RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
		HttpClientCloud.getInstance().getCloudService().searchDetail(body).subscribe(res -> {
			log.info("searchDetail res:{}", res);
			responseData.setData(res);
			responseData.setPage(new Paging());
		});
		return mapper.writeValueAsString(responseData);
	}

	/** 查流水明细 */
	@RequestMapping("/account/logDetail")
	public String logDetail(@RequestParam(value = "handicap") String handicap,
			@RequestParam(value = "account", required = false) String account,
			@RequestParam(value = "status") String status, @RequestParam(value = "type") Integer type,
			@RequestParam(value = "timeStart", required = false) String timeStart,
			@RequestParam(value = "timeEnd", required = false) String timeEnd,
			@RequestParam(value = "amountFr", required = false) String amountFr,
			@RequestParam(value = "amountTo", required = false) String amountTo,
			@RequestParam(value = "pageNo", required = false) Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws Exception {

		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			GeneralResponseData<Object> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		GeneralResponseData<Object> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
		String json = wrapJsonStrForLogDetail(handicap, type, account, status, amountFr, amountTo, timeEnd, timeStart,
				pageNo, pageSize);
		log.info("logDetail json ---->  {}", json);
		RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
		HttpClientCloud.getInstance().getCloudService().logDetail(body).subscribe(res -> {
			log.info("logDetail res---->  {}", res);
			responseData.setData(res);
			responseData.setPage(new Paging());
		});
		return mapper.writeValueAsString(responseData);
	}

	/** 查流水明细总记录和总金额 */
	@RequestMapping("/account/logDetailCountAndSum")
	public String logDetailCountAndSum(@RequestParam(value = "handicap") String handicap,
			@RequestParam(value = "account", required = false) String account,
			@RequestParam(value = "status") String status, @RequestParam(value = "type") Integer type,
			@RequestParam(value = "timeStart", required = false) String timeStart,
			@RequestParam(value = "timeEnd", required = false) String timeEnd,
			@RequestParam(value = "amountFr", required = false) String amountFr,
			@RequestParam(value = "amountTo", required = false) String amountTo,
			@RequestParam(value = "pageNo", required = false) Integer pageNo,
			@RequestParam(value = "pageSize", required = false) Integer pageSize) throws Exception {

		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			GeneralResponseData<Object> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		GeneralResponseData<Object> responseData = new GeneralResponseData<>(
				GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
		String json = wrapJsonStrForLogDetail(handicap, type, account, status, amountFr, amountTo, timeEnd, timeStart,
				pageNo, pageSize);
		log.info("logDetailCountAndSum json ---->  {}", json);
		RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
		HttpClientCloud.getInstance().getCloudService().logDetailCountAndSum(body).subscribe(res -> {
			log.info("logDetailCountAndSum res---->  {}", res);
			JSONObject jsonObject;
			JSONObject jsonObject1;
			try {
				jsonObject = new JSONObject(JSONObject.wrap(res).toString());
				jsonObject.get("status");
				jsonObject.get("message");
				Object data = jsonObject.get("data");
				jsonObject1 = new JSONObject(JSONObject.wrap(data).toString());
				Integer count = (Integer) jsonObject1.get("count");
				log.info("logDetailCountAndSum result----> status:{},message:{},data:{},count:{},sum:{}",
						jsonObject.get("status"), jsonObject.get("message"), jsonObject.get("data"),
						jsonObject1.get("count"), jsonObject1.get("sum"));
				Paging page;
				if (count != null) {
					page = CommonUtils.getPage(pageNo + 1, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
							String.valueOf(count));
				} else {
					page = CommonUtils.getPage(0, pageSize != null ? pageSize : AppConstants.PAGE_SIZE, "0");
				}
				Map map = new HashMap<>();
				BigDecimal sum = new BigDecimal(jsonObject1.get("sum").toString());
				map.put("sum", sum);
				responseData.setData(map);
				responseData.setPage(page);
			} catch (Exception e) {
				log.info("logDetailCountAndSum fail---->  {}", e);
			}
		});
		return mapper.writeValueAsString(responseData);
	}

	private String wrapJsonStr(String handicap, Integer level, String account, String accountType, String holder,
			String owner, String bankName, String status, Integer type, String syslevel, Integer pageNo,
			Integer pageSize, String timeStart, String timeEnd) {
		pageNo = pageNo == null ? 0 : pageNo;
		pageSize = pageSize == null ? AppConstants.PAGE_SIZE : pageSize;
		// 盘口 状态 类型-银行卡 支付宝 微信 keymas 生成token
		String keyMas = environment.getProperty("funds.transfer.apicloudkey");
		String token = requestBodyParser.md5digest(handicap + status + type + keyMas);
		log.info("keyMas:{},token:{}", keyMas, token);
		String json = "{\"pageNo\":" + pageNo + ",\"pageSize\":" + pageSize + ",\"token\":\"" + token + "\"";
		if (Objects.nonNull(level)) {
			BizLevel bizLevel = levelService.findFromCache(level);
			BizHandicap bizHandicap = handicapService.findFromCacheById(bizLevel.getHandicapId());
			handicap = new StringBuffer(handicap).append(",").append(bizHandicap.getCode()).toString();
		}
		// 自用 客户
		if (Objects.nonNull(accountType)) {
			json += "," + "\"mobileType\":\"" + accountType + "\"";
		}
		// 内外层
		if (Objects.nonNull(syslevel)) {
			json += "," + "\"sysLevel\":\"" + syslevel + "\"";
		}
		if (StringUtils.isNotBlank(holder)) {
			// 持卡人->账号

		}
		if (StringUtils.isNotBlank(handicap)) {
			json += "," + "\"handicap\":\"" + handicap + "\"";
		}
		if (StringUtils.isNotBlank(account)) {
			json += "," + "\"account\":\"" + account + "\"";
		}
		if (StringUtils.isNotBlank(bankName)) {
			json += "," + "\"bankName\":\"" + bankName + "\"";
		}
		if (StringUtils.isNotBlank(owner)) {
			json += "," + "\"owner\":\"" + owner + "\"";
		}
		if (StringUtils.isNotBlank(status)) {
			json += "," + "\"status\":\"" + status + "\"";
		}
		if (Objects.nonNull(type)) {
			json += "," + "\"type\":\"" + type + "\"";
		}
		if (StringUtils.isNotBlank(timeStart)) {
			json += "," + "\"timeStart\":\"" + StringUtils.trim(timeStart) + "\"";
		}
		if (StringUtils.isNotBlank(timeEnd)) {
			json += "," + "\"timeEnd\":\"" + StringUtils.trim(timeEnd) + "\"";
		}
		json += "}";
		return json;
	}

	private String wrapJsonStrForDetail(String handicap, Integer level, String accountF, String account,
			String depositor, String member, String amountFr, String amountTo, String timeEnd, String timeStart,
			String status, Integer type, Integer outIn, String orderNo, Integer pageNo, Integer pageSize) {
		pageNo = pageNo == null ? 0 : pageNo;
		pageSize = pageSize == null ? AppConstants.PAGE_SIZE : pageSize;
		// 盘口 账号 转入转出 匹配状态 类型-银行 支付宝 微信 keyMas 生成token
		String keyMas = environment.getProperty("funds.transfer.apicloudkey");
		String token = requestBodyParser.md5digest(accountF + outIn + status + type + keyMas);
		log.info("keyMas:{},token:{}", keyMas, token);
		String json = "{\"pageNo\":" + pageNo + ",\"pageSize\":" + pageSize + ",\"token\":\"" + token + "\"";
		if (StringUtils.isNotBlank(status)) {
			json += "," + "\"status\":\"" + status + "\"";
		}
		if (Objects.nonNull(type)) {
			json += "," + "\"type\":\"" + type + "\"";
		}
		if (Objects.nonNull(outIn)) {
			json += "," + "\"outIn\":\"" + outIn + "\"";
		}
		if (StringUtils.isNotBlank(accountF)) {
			json += "," + "\"accountFrom\":\"" + accountF + "\"";
		}
		if (Objects.nonNull(level)) {
			json += "," + "\"sysLevel\":\"" + level + "\"";
		}
		if (StringUtils.isNotBlank(handicap)) {
			json += "," + "\"handicap\":\"" + handicap + "\"";
		}
		if (StringUtils.isNotBlank(account)) {
			json += "," + "\"account\":\"" + account + "\"";
		}
		if (StringUtils.isNotBlank(orderNo)) {
			json += "," + "\"orderNo\":\"" + orderNo + "\"";
		}
		if (StringUtils.isNotBlank(depositor)) {
			json += "," + "\"depositor\":\"" + depositor + "\"";
		}
		if (StringUtils.isNotBlank(member)) {
			json += "," + "\"member\":\"" + member + "\"";
		}
		if (StringUtils.isNotBlank(amountFr)) {
			json += "," + "\"amountStart\":\"" + amountFr + "\"";
		}
		if (StringUtils.isNotBlank(amountTo)) {
			json += "," + "\"amountEnd\":\"" + amountTo + "\"";
		}
		if (StringUtils.isNotBlank(timeStart)) {
			json += "," + "\"timeStart\":\"" + StringUtils.trim(timeStart) + "\"";
		}
		if (StringUtils.isNotBlank(timeEnd)) {
			json += "," + "\"timeEnd\":\"" + StringUtils.trim(timeEnd) + "\"";
		}
		json += "}";
		return json;
	}

	private String wrapJsonStrForLogDetail(String handicap, Integer type, String account, String status,
			String amountFr, String amountTo, String timeEnd, String timeStart, Integer pageNo, Integer pageSize) {
		pageNo = pageNo == null ? 0 : pageNo;
		pageSize = pageSize == null ? AppConstants.PAGE_SIZE : pageSize;
		// 盘口 账号 查询类型 keymas 生成token
		String keyMas = environment.getProperty("funds.transfer.apicloudkey");
		String token = requestBodyParser.md5digest(handicap + account + type + keyMas);
		log.info("keyMas:{},token:{}", keyMas, token);
		String json = "{\"pageNo\":" + pageNo + ",\"pageSize\":" + pageSize + ",\"token\":\"" + token + "\"";

		if (StringUtils.isNotBlank(handicap)) {
			json += "," + "\"handicap\":\"" + handicap + "\"";
		}
		if (type != null) {
			json += "," + "\"type\":\"" + type + "\"";
		}
		if (StringUtils.isNotBlank(status)) {
			json += "," + "\"status\":\"" + status + "\"";
		}
		if (StringUtils.isNotBlank(account)) {
			json += "," + "\"account\":\"" + account + "\"";
		}
		if (StringUtils.isNotBlank(amountFr)) {
			json += "," + "\"amountStart\":\"" + amountFr + "\"";
		}
		if (StringUtils.isNotBlank(amountTo)) {
			json += "," + "\"amountEnd\":\"" + amountTo + "\"";
		}
		if (StringUtils.isNotBlank(timeStart)) {
			json += "," + "\"timeStart\":\"" + StringUtils.trim(timeStart) + "\"";
		}
		if (StringUtils.isNotBlank(timeEnd)) {
			json += "," + "\"timeEnd\":\"" + StringUtils.trim(timeEnd) + "\"";
		}
		json += "}";
		return json;
	}

	private String wrapJson4LaunchCheck(String mobile, String account, Integer type) {
		String keyMas = environment.getProperty("funds.transfer.apicloudkey");
		String token = requestBodyParser.md5digest(mobile + account + type + keyMas);
		StringBuilder json = new StringBuilder();
		if (StringUtils.isNotBlank(token)) {
			json.append("{\"token\":\"").append(StringUtils.trim(token)).append("\"");
		}
		if (type != null) {
			json.append("," + "\"type\":\"").append(type).append("\"");
		}
		if (StringUtils.isNotBlank(mobile)) {
			json.append("," + "\"mobile\":\"").append(StringUtils.trim(mobile)).append("\"");
		}
		if (StringUtils.isNotBlank(account)) {
			json.append("," + "\"account\":\"").append(StringUtils.trim(account)).append("\"");
		}
		json.append("}");
		return json.toString();
	}
}
