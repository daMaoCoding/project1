package com.xinbo.fundstransfer.restful.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.component.redis.RedisTopics;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.GeneralResponseData.ResponseStatus;
import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.enums.IncomeRequestStatus;
import com.xinbo.fundstransfer.domain.enums.IncomeRequestType;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.restful.BaseController;
import com.xinbo.fundstransfer.restful.api.pojo.ApiIncome;
import com.xinbo.fundstransfer.restful.api.pojo.ApiIncomeCancel;
import com.xinbo.fundstransfer.service.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * 入款请求接口
 * 
 * @author Dom
 *
 */
@Deprecated
@Description(value = "用于旧的入款请求,现在已废弃")
@RestController("apiIncomeRequestController")
@RequestMapping("/api/income")
public class IncomeRequestController extends BaseController {
	Logger log = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private RedisService redisService;
	@Autowired
	LevelService levelService;
	@Autowired
	HandicapService handicapService;
	@Autowired
	IncomeRequestService incomeRequestService;
	@Autowired
	ThirdRequestService thirdRequestService;
	@Autowired
	AccountService accountService;
	@Autowired
	AllocateTransferService allocateTransferService;
	@Autowired
	AlipayRequestService alipayRequestService;
	@Autowired
	CloudService cloudService;

	@Autowired
	Environment environment;
	private ObjectMapper mapper = new ObjectMapper();

	@RequestMapping("/put")
	public String put(@RequestBody String bodyJson) throws JsonProcessingException {
		try {
			log.info("Income >> RequestBody:{}", bodyJson);
			ApiIncome entity = mapper.readValue(bodyJson, ApiIncome.class);

			if (null == entity.getType()) {
				log.info("未知类型入款,订单号：{}", entity.getOrderNo());
				return mapper
						.writeValueAsString(new SimpleResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
								"Failure, Unknown type. orderNo:" + entity.getOrderNo()));
			}
			BizIncomeRequest o = null;
			// 要判断是否平台入款，若为中转类型不作盘口与层级校验
			if (IncomeRequestType.isPlatform(entity.getType())) {
				BizHandicap bizHandicap = handicapService.findFromCacheByCode(entity.getHandicap());
				if (null == bizHandicap) {
					log.info("{} 盘口不存在,订单号：{}", entity.getHandicap(), entity.getOrderNo());
					return mapper.writeValueAsString(new SimpleResponseData(
							GeneralResponseData.ResponseStatus.FAIL.getValue(), "Failure, handicap does not exist."));
				}

				AccountBaseInfo account = accountService.getFromCacheByHandicapIdAndAccount(bizHandicap.getId(),
						entity.getToAccount());
				if (null == account) {
					log.info("{} 入款帐号不存在,订单号：{}", entity.getToAccount(), entity.getOrderNo());
					return mapper.writeValueAsString(new SimpleResponseData(
							GeneralResponseData.ResponseStatus.FAIL.getValue(), "Failure, toAccount does not exist."));
				}
				// 计算token
				Map<String, String> parameters = new TreeMap<String, String>(new Comparator<String>() {
					@Override
					public int compare(String obj1, String obj2) {
						return obj1.compareTo(obj2);
					}
				});
				parameters.put("handicap", entity.getHandicap());
				parameters.put("amount", entity.getAmount());
				parameters.put("remark", entity.getRemark());
				parameters.put("orderno", entity.getOrderNo());
				parameters.put("usercode", entity.getUsercode() + "");
				parameters.put("type", entity.getType() + "");
				parameters.put("createtime", entity.getCreateTime());
				parameters.put("toaccount", entity.getToAccount());
				parameters.put("username", entity.getUsername());
				if (StringUtils.isNotEmpty(entity.getAckTime())) {
					parameters.put("acktime", entity.getAckTime());
				}
				if (StringUtils.isNotEmpty(entity.getRealname())) {
					parameters.put("realname", entity.getRealname());
				}
				if (StringUtils.isNotEmpty(entity.getPaymentCode())) {
					parameters.put("paymentcode", entity.getPaymentCode());
				}
				if (StringUtils.isNotEmpty(entity.getLevel())) {
					parameters.put("level", entity.getLevel());
				}
				if (StringUtils.isNotEmpty(entity.getFromAccount())) {
					parameters.put("fromaccount", entity.getFromAccount());
				}

				StringBuilder sb = new StringBuilder();
				Set<Map.Entry<String, String>> entrySet = parameters.entrySet();
				for (Map.Entry<String, String> entry : entrySet) {
					sb.append(entry.getValue());
				}
				if (!checkToken(sb.toString(), entity.getToken())) {
					log.info("Token error. RequestToken:{}, md5:{}", entity.getToken(), sb.toString());
					return mapper.writeValueAsString(new SimpleResponseData(
							GeneralResponseData.ResponseStatus.FAIL.getValue(), "Failure, invalid Token."));
				}

				// 分开处理，公司入款与第三方
				if (entity.getType() == 4) {
					BizThirdRequest third = new BizThirdRequest();
					third.setHandicap(bizHandicap.getId());
					if (StringUtils.isNotEmpty(entity.getLevel())) {
						BizLevel bizLevel = levelService.findFromCache(bizHandicap.getId(), entity.getLevel());
						if (null != bizLevel) {
							third.setLevel(bizLevel.getId());
						}
					}
					third.setToAccount(account.getAccount());
					third.setAmount(new BigDecimal(entity.getAmount()));
					third.setCreateTime(DateUtils.parseDate(entity.getCreateTime(), "yyyy-MM-dd HH:mm:ss"));
					if (StringUtils.isNotEmpty(entity.getAckTime())) {
						third.setAckTime(DateUtils.parseDate(entity.getAckTime(), "yyyy-MM-dd HH:mm:ss"));
					}
					third.setOrderNo(entity.getOrderNo());
					third.setRemark(entity.getRemark());
					third.setFromAccount(entity.getFromAccount());
					third.setMemberCode(entity.getUsercode());
					third.setMemberUserName(entity.getUsername());
					third.setMemberRealName(entity.getRealname());
					if (null != entity.getFee() && StringUtils.isNotEmpty(entity.getFee())) {
						third.setFee(new BigDecimal(entity.getFee()));
					}
					try {
						redisService.rightPush(RedisTopics.INCOME_THIRD_REQUEST, mapper.writeValueAsString(third));
					} catch (Exception e) {
						log.error("", e);
						if (null == thirdRequestService.findByHandicapAndOrderNo(third.getHandicap(),
								third.getOrderNo())) {
							thirdRequestService.save(third);
							log.info("Income third [DB] direct put in db. orderNo: {}", third.getOrderNo());
						} else {
							log.info("Income third error, orderNo: {} already exist.", third.getOrderNo());
						}
					}
					return mapper
							.writeValueAsString(new SimpleResponseData(ResponseStatus.SUCCESS.getValue(), "Success."));
				} else if (entity.getType() == 1) {// 支付宝入款
					BizAlipayRequest req = new BizAlipayRequest();
					req.setAlipayid(account.getId());
					req.setHandicap(bizHandicap.getId());
					if (StringUtils.isNotEmpty(entity.getLevel())) {
						BizLevel bizLevel = levelService.findFromCache(bizHandicap.getId(), entity.getLevel());
						if (null != bizLevel) {
							req.setLevel(bizLevel.getId());
						}
					}
					req.setAmount(new BigDecimal(entity.getAmount()));
					req.setCreateTime(DateUtils.parseDate(entity.getCreateTime(), "yyyy-MM-dd HH:mm:ss"));
					if (StringUtils.isNotEmpty(entity.getAckTime())) {
						// req.setUpdateTime(DateUtils.parseDate(entity.getCreateTime(),
						// "yyyy-MM-dd HH:mm:ss"));
						// req.setUpdateTime(DateUtils.parseDate(entity.getAckTime(),
						// "yyyy-MM-dd HH:mm:ss"));
					}
					req.setOrderNo(entity.getOrderNo());
					req.setRemark(entity.getRemark());
					req.setMemberId(String.valueOf(entity.getUsercode()));
					req.setMemberName(entity.getUsername());
					req.setStatus(IncomeRequestStatus.Matching.getStatus());
					alipayRequestService.save(req);
				} else if (entity.getType() == 2) {// 微信入款
				} else {
					o = new BizIncomeRequest();
					o.setToId(account.getId());
					o.setHandicap(bizHandicap.getId());
					if (StringUtils.isNotEmpty(entity.getLevel())) {
						BizLevel bizLevel = levelService.findFromCache(bizHandicap.getId(), entity.getLevel());
						if (null != bizLevel) {
							o.setLevel(bizLevel.getId());
						}
					}
					o.setToAccount(account.getAccount());
					o.setAmount(new BigDecimal(entity.getAmount()));
					o.setCreateTime(DateUtils.parseDate(entity.getCreateTime(), "yyyy-MM-dd HH:mm:ss"));
					if (StringUtils.isNotEmpty(entity.getAckTime())) {
						o.setUpdateTime(DateUtils.parseDate(entity.getAckTime(), "yyyy-MM-dd HH:mm:ss"));
					}
					o.setOrderNo(entity.getOrderNo());
					o.setRemark(entity.getRemark());
					o.setType(entity.getType());
					o.setFromAccount(entity.getFromAccount());
					o.setMemberCode(entity.getUsercode()+"");
					o.setMemberUserName(entity.getUsername());
					o.setMemberRealName(entity.getRealname());
					o.setStatus(IncomeRequestStatus.Matching.getStatus());
					if (null != entity.getFee() && StringUtils.isNotEmpty(entity.getFee())) {
						o.setFee(new BigDecimal(entity.getFee()));
					}
				}
			} else {
				SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
				if (operator == null) {
					throw new Exception("会话超时，请重新登陆.");
				}
				if (StringUtils.isEmpty(entity.getAmount())) {
					throw new Exception("转账金额应大于等于0.");
				}
				BizAccount fromAccount = accountService.getById(entity.getFromId());
				if (fromAccount == null) {
					throw new Exception("账号不存在.");
				}
				// 转账总金额=转账金额+转账手续费
				float tranfer = Float.parseFloat(entity.getAmount())
						+ (StringUtils.isEmpty(entity.getFee()) ? 0F : Float.parseFloat(entity.getFee()));
				// 系统余额
				float balance = fromAccount.getBalance() == null ? 0F : fromAccount.getBalance().floatValue();
				// 银行余额
				float bankBalance = fromAccount.getBankBalance() == null ? 0F
						: fromAccount.getBankBalance().floatValue();
				if (fromAccount.getType().equals(AccountType.BindCustomer.getTypeId())) {
					BizAccount toAccount = accountService.getById(entity.getToId());
					cloudService.rBankTransfer(fromAccount.getType(), entity.getFromAccount(),
							new BigDecimal(entity.getAmount()), toAccount.getOwner(), toAccount.getAccount(),
							toAccount.getBankName());
					return mapper
							.writeValueAsString(new SimpleResponseData(ResponseStatus.SUCCESS.getValue(), "Success."));
				}
				if (fromAccount.getType().equals(AccountType.InAli.getTypeId())) {
					BizAccount toAccount = accountService.getById(entity.getToId());
					cloudService.rBankTransfer(fromAccount.getType(), entity.getFromAccount(),
							new BigDecimal(entity.getAmount()), toAccount.getOwner(), toAccount.getAccount(),
							toAccount.getBankName());
					return mapper
							.writeValueAsString(new SimpleResponseData(ResponseStatus.SUCCESS.getValue(), "Success."));
				}
				if (fromAccount.getType().equals(AccountType.InWechat.getTypeId())) {
					BizAccount toAccount = accountService.getById(entity.getToId());
					cloudService.rBankTransfer(fromAccount.getType(), entity.getFromAccount(),
							new BigDecimal(entity.getAmount()), toAccount.getOwner(), toAccount.getAccount(),
							toAccount.getBankName());
					return mapper
							.writeValueAsString(new SimpleResponseData(ResponseStatus.SUCCESS.getValue(), "Success."));
				}
				if ((!fromAccount.getType().equals(AccountType.InThird.getTypeId())
						&& !fromAccount.getType().equals(AccountType.InAli.getTypeId())
						&& !fromAccount.getType().equals(AccountType.InWechat.getTypeId())
						&& !fromAccount.getType().equals(AccountType.BindCustomer.getTypeId()))
						&& ((bankBalance < tranfer))) {
					// 系统余额与银行余额都小于转账总金额或（系统余额-转账总金额）的相反数大于银行余额
					throw new Exception("系统余额或银行余额不足.");
				}
				AccountBaseInfo to = accountService.getFromCacheById(entity.getToId());
				o = new BizIncomeRequest();
				o.setToId(entity.getToId());
				o.setFromId(entity.getFromId());
				if (AccountType.InBank.getTypeId() == fromAccount.getType()
						|| AccountType.InThird.getTypeId() == fromAccount.getType()
						|| AccountType.InWechat.getTypeId() == fromAccount.getType()
						|| AccountType.InThird.getTypeId() == fromAccount.getType()) {
					o.setHandicap(fromAccount.getHandicapId());
				} else {
					o.setHandicap(0);
				}
				o.setLevel(0);
				o.setToAccount(entity.getToAccount());
				o.setOperator(entity.getOperator());
				o.setAmount(new BigDecimal(entity.getAmount()));
				o.setCreateTime(new Date());
				o.setOrderNo(entity.getOrderNo());
				o.setRemark(entity.getRemark());
				o.setType(entity.getType());
				o.setFromAccount(entity.getFromAccount());
				o.setMemberUserName(entity.getUsername());
				o.setMemberRealName(to != null ? to.getOwner() : entity.getRealname());
				o.setStatus(IncomeRequestStatus.Matching.getStatus());
				if (null != entity.getFee() && StringUtils.isNotEmpty(entity.getFee())) {
					o.setFee(new BigDecimal(entity.getFee()));
				}
				if (IncomeRequestType.WithdrawThirdToCustomer.getType() != entity.getType()) {
					allocateTransferService.unlockTrans(entity.getFromId(), o.getToId(), operator.getId());// 提现到客户卡无需解锁
				} else {
					o.setToAccountBank(entity.getToAccountBank());// 客户卡
				}
			}

			String json = mapper.writeValueAsString(o);

			try {
				log.debug("保存到RedisTopics.INCOME_REQUEST,start:{}", json);
				redisService.rightPush(RedisTopics.INCOME_REQUEST, json);
				log.debug("保存到RedisTopics.INCOME_REQUEST,finish:{}", json);
			} catch (Exception e) {
				log.error("", e);
				// 特殊处理，若redis
				// sentinel作主从切换，或当前master出现异常挂掉，切换时会有几秒钟的延迟连接不可用，不重复后直接入库
				if (null == incomeRequestService.findByHandicapAndOrderNo(o.getHandicap(), o.getOrderNo())) {
					log.info("Income[DB]保存到数据库 direct put in db. start orderNo: {}", o.getOrderNo());
					o = incomeRequestService.save(o, true);
					log.info("Income[DB]保存到数据库 direct put in db. end orderNo: {}", o.getOrderNo());
				} else {
					log.info("Income error, orderNo: {} already exist.", o.getOrderNo());
				}
			}
			return mapper.writeValueAsString(new SimpleResponseData(ResponseStatus.SUCCESS.getValue(), "Success."));
		} catch (Exception e) {
			log.error("Income error.", e);
			return mapper.writeValueAsString(new SimpleResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"Failure, " + e.getMessage()));
		}
	}

	@RequestMapping("/cloud/put")
	public String putToCloud(@RequestParam("frType") Integer frType, @RequestParam("account") String account,
			@RequestParam("amount") BigDecimal amount, @RequestParam("nickName") String nickName,
			@RequestParam("toAccount") String toAccount, @RequestParam("toAccountBank") String toAccountBank)
			throws JsonProcessingException {
		try {
			return mapper.writeValueAsString(
					cloudService.rBankTransfer(frType, account, amount, nickName, toAccount, toAccountBank));
		} catch (Exception e) {
			return mapper.writeValueAsString(new SimpleResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"Failure, " + e.getMessage()));
		}
	}

	@RequestMapping("/cancel")
	public String cancel(@RequestBody String bodyJson) throws JsonProcessingException {
		log.debug("Income cancel >> RequestBody:{}", bodyJson);
		try {
			ApiIncomeCancel entity = mapper.readValue(bodyJson, ApiIncomeCancel.class);
			BizHandicap bizHandicap = handicapService.findFromCacheByCode(entity.getHandicap());
			if (null == bizHandicap) {
				log.info("{} 盘口不存在", entity.getHandicap());
				return mapper.writeValueAsString(new SimpleResponseData(
						GeneralResponseData.ResponseStatus.FAIL.getValue(), "Failure, handicap does not exist."));
			}
			// 校验token
			StringBuilder sb = new StringBuilder();
			sb.append(entity.getHandicap());
			sb.append(entity.getOrderNo());
			if (!checkToken(sb.toString(), entity.getToken())) {
				log.info("Token error. RequestToken:{}, md5:{}", entity.getToken(), sb.toString());
				return mapper.writeValueAsString(new SimpleResponseData(
						GeneralResponseData.ResponseStatus.FAIL.getValue(), "Failure, invalid Token."));
			}
			// TODO 取消这些订单，通知前端
			incomeRequestService.cancelOrder(bizHandicap.getId(), Arrays.asList(entity.getOrderNo().split(",")));
			return mapper.writeValueAsString(new SimpleResponseData(ResponseStatus.SUCCESS.getValue(), "Success."));
		} catch (Exception e) {
			log.error("Income cancel error.", e);
			return mapper.writeValueAsString(new SimpleResponseData(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"Failure, " + e.getMessage()));
		}
	}

	private boolean checkToken(String content, String token) throws NoSuchAlgorithmException {
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		md5.update((content + environment.getProperty("funds.transfer.apikey")).getBytes());
		// 进行哈希计算并返回结果
		byte[] btResult = md5.digest();
		// 进行哈希计算后得到的数据的长度
		StringBuffer md5Token = new StringBuffer();
		for (byte b : btResult) {
			int bt = b & 0xff;
			if (bt < 16) {
				md5Token.append(0);
			}
			md5Token.append(Integer.toHexString(bt));
		}
		if (md5Token.toString().equals(token)) {
			return true;
		}
		return false;
	}

}
