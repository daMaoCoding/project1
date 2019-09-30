package com.xinbo.fundstransfer.restful.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.component.redis.RedisKeys;
import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.enums.InBankSubType;
import com.xinbo.fundstransfer.domain.enums.IncomeRequestStatus;
import com.xinbo.fundstransfer.domain.enums.IncomeRequestType;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.restful.v2.pojo.RequestIncome;
import com.xinbo.fundstransfer.service.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

/**
 * 新平台入款
 */
@RestController
@RequestMapping("/api/v2/income")
public class Income2Controller extends TokenValidation {
	private static final Logger log = LoggerFactory.getLogger(Income2Controller.class);
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
	private ObjectMapper mapper = new ObjectMapper();

	SimpleResponseData error400 = new SimpleResponseData(400, "Error:参数丢失");
	SimpleResponseData error401 = new SimpleResponseData(401, "Error:token校验不通过");
	SimpleResponseData error500 = new SimpleResponseData(500, "Error:处理订单异常");
	SimpleResponseData success = new SimpleResponseData(1, "OK");

	/**
	 * 新平台入款
	 *
	 * @throws JsonProcessingException
	 */
	@RequestMapping(value = "/put", method = RequestMethod.POST, consumes = "application/json")
	public @ResponseBody SimpleResponseData put(@Valid @RequestBody RequestIncome requestBody, BindingResult result)
			throws JsonProcessingException {
		log.info("入款订单 参数  :{}", mapper.writeValueAsString(requestBody));
		if (result.hasErrors()) {
			return error400;
		}
		if (!checkToken(requestBody.getToken(), requestBody.getHandicap(), requestBody.getOrderNo())) {
			return error401;
		}

		try {
			BizIncomeRequest o;
			// 要判断是否平台入款，若为中转类型不作盘口与层级校验
			if (IncomeRequestType.isPlatform(requestBody.getType())) {
				BizHandicap bizHandicap = handicapService.findFromCacheByCode(requestBody.getHandicap());
				if (null == bizHandicap || !Objects.equals(bizHandicap.getStatus(), 1)) {
					log.info("{} 盘口不存在,订单号：{}", requestBody.getHandicap(), requestBody.getOrderNo());
					return error400;
				}
				if (IncomeRequestType.PlatFromThird.getType().equals(requestBody.getType())) {
					if (StringUtils.isBlank(requestBody.getFromAccount())) {
						log.info("盘口:{},第三方订单号：{},商家:{}", requestBody.getHandicap(), requestBody.getOrderNo(),
								requestBody.getFromAccount());
						return new SimpleResponseData(400, "第三方订单商家名称必传");
					}
				}
				AccountBaseInfo account = null;
				if (IncomeRequestType.PlatFromThird.getType().equals(requestBody.getType())) {
					account = accountService.getFromCacheByHandicapIdAndAccountAndBankName(bizHandicap.getId(),
							requestBody.getToAccount(), requestBody.getFromAccount());
					if (null == account) {
						log.info(" 平台第三方入款帐号 {} 不存在,订单号：{}", requestBody.getToAccount(), requestBody.getOrderNo());
						return error400;
					}
				} else {
					if (requestBody.getToAccount().contains("#")) {
						// 支付宝转银行卡的入款订单可能对应多个收款账号，并且这些收款账号必须都是支付宝银行卡类型
						String[] toAccounts = requestBody.getToAccount().split("#");
						for (String toAcc : toAccounts) {
							AccountBaseInfo account1 = accountService
									.getFromCacheByHandicapIdAndAccount(bizHandicap.getId(), toAcc);
							if (Objects.isNull(account1)) {
								return new SimpleResponseData(400, " 收款账号为空,请核实!");

							}
							if (Objects.isNull(account1.getSubType())
									|| !InBankSubType.IN_BANK_ALIIN.getSubType().equals(account1.getSubType())) {
								log.info("盘口:{},入款订单：{},入款账号:{}", requestBody.getHandicap(), requestBody.getOrderNo(),
										requestBody.getToAccount());
								return new SimpleResponseData(400, "收款账号:" + account1.getAccount() + "不属于支付宝入款卡,请核实!");
							}
						}
					} else {
						account = accountService.getFromCacheByHandicapIdAndAccount(bizHandicap.getId(),
								requestBody.getToAccount());
						if (null == account) {
							log.info("{} 入款帐号不存在,订单号：{}", requestBody.getToAccount(), requestBody.getOrderNo());
							return error400;
						}
					}
				}
				// 分开处理，公司入款与第三方
				if (IncomeRequestType.PlatFromThird.getType().equals(requestBody.getType())) {
					BizThirdRequest third = new BizThirdRequest();
					third.setHandicap(bizHandicap.getId());
					if (StringUtils.isNotEmpty(requestBody.getLevel())) {
						BizLevel bizLevel = levelService.findFromCache(bizHandicap.getId(), requestBody.getLevel());
						if (null != bizLevel) {
							third.setLevel(bizLevel.getId());
						}
					}
					third.setToAccount(account.getAccount());
					third.setAmount(requestBody.getAmount());
					third.setCreateTime(DateUtils.parseDate(requestBody.getCreateTime(), "yyyy-MM-dd HH:mm:ss"));
					if (StringUtils.isNotEmpty(requestBody.getAckTime())) {
						third.setAckTime(DateUtils.parseDate(requestBody.getAckTime(), "yyyy-MM-dd HH:mm:ss"));
					}
					third.setOrderNo(requestBody.getOrderNo());
					third.setRemark(requestBody.getRemark());
					// 保存商家
					third.setFromAccount(requestBody.getFromAccount());
					third.setMemberUserName(requestBody.getUsername());
					third.setMemberRealName(requestBody.getRealname());
					if (StringUtils.isNotBlank(requestBody.getFee())) {
						third.setFee(new BigDecimal(requestBody.getFee()));
					} else {
						// 平台传空 则设置0.0
						third.setFee(BigDecimal.ZERO);
					}
					String res = saveThirdOrder(third, log, redisService, thirdRequestService);
					if ("ok".equals(res)) {
						return success;
					} else {
						error500.setMessage(res);
						return error500;
					}
				} else {
					// 平台过来的 公司入款单
					BizIncomeRequest record = alreadyExisted(requestBody.getOrderNo(), bizHandicap.getId(),
							requestBody.getAmount());
					if (record != null) {
						log.info("修改订单会员真实姓名:已存在的订单:{},新同步的:{}", mapper.writeValueAsString(requestBody),
								mapper.writeValueAsString(requestBody));
						if (!record.getMemberRealName().equals(requestBody.getRealname())) {
							record.setMemberRealName(requestBody.getRealname());
							updateExisted(record);
							success.setMessage("修改真实姓名同步成功!");
						}
						return success;
					}

					o = new BizIncomeRequest();
					if (!requestBody.getToAccount().contains("#")) {
						// 如果订单对应多个收款账号,先不保存toId
						o.setToId(account.getId());
						o.setToAccount(account.getAccount());
					} else {
						o.setToAccount(requestBody.getToAccount());
					}
					o.setHandicap(bizHandicap.getId());
					if (StringUtils.isNotBlank(requestBody.getLevel())) {
						BizLevel bizLevel = levelService.findFromCache(bizHandicap.getId(), requestBody.getLevel());
						if (null != bizLevel) {
							o.setLevel(bizLevel.getId());
						}
					}
					o.setAmount(requestBody.getAmount());
					o.setCreateTime(DateUtils.parseDate(requestBody.getCreateTime(), "yyyy-MM-dd HH:mm:ss"));
					if (StringUtils.isNotBlank(requestBody.getAckTime())) {
						o.setUpdateTime(DateUtils.parseDate(requestBody.getAckTime(), "yyyy-MM-dd HH:mm:ss"));
					}
					o.setOrderNo(requestBody.getOrderNo());
					o.setRemark(requestBody.getRemark());
					o.setType(requestBody.getType());
					o.setFromAccount(requestBody.getFromAccount());
					o.setMemberUserName(requestBody.getUsername());
					o.setMemberRealName(requestBody.getRealname());
					if (o.getType() != null && (o.getType().equals(IncomeRequestType.PlatFromAli.getType())
							|| o.getType().equals(IncomeRequestType.PlatFromWechat.getType()))) {
						o.setStatus(IncomeRequestStatus.Matched.getStatus());
					} else {
						o.setStatus(IncomeRequestStatus.Matching.getStatus());
					}
					if (StringUtils.isNotBlank(requestBody.getFee())) {
						o.setFee(new BigDecimal(requestBody.getFee()));
					}
				}
			} else {
				SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
				if (operator == null) {
					throw new Exception("会话超时，请重新登陆.");
				}
				BizAccount fromAccount = accountService.getById(requestBody.getFromId());
				if (fromAccount == null) {
					throw new Exception("账号不存在.");
				}
				// 转账总金额=转账金额+转账手续费
				float tranfer = requestBody.getAmount().floatValue()
						+ (StringUtils.isBlank(requestBody.getFee()) ? 0F : Float.parseFloat(requestBody.getFee()));
				// 系统余额
				float balance = fromAccount.getBalance() == null ? 0F : fromAccount.getBalance().floatValue();
				// 银行余额
				float bankBalance = fromAccount.getBankBalance() == null ? 0F
						: fromAccount.getBankBalance().floatValue();
				boolean flag = (!fromAccount.getType().equals(AccountType.InThird.getTypeId()))
						&& ((bankBalance < tranfer & balance < balance) || (balance - tranfer) * (-1) > bankBalance);
				if (flag) {
					// 系统余额与银行余额都小于转账总金额或（系统余额-转账总金额）的相反数大于银行余额
					throw new Exception("系统余额或银行余额不足.");
				}
				AccountBaseInfo to = accountService.getFromCacheById(requestBody.getToId());
				o = new BizIncomeRequest();
				o.setToId(requestBody.getToId());
				o.setFromId(requestBody.getFromId());
				o.setHandicap(0);
				o.setLevel(0);
				o.setToAccount(requestBody.getToAccount());
				o.setOperator(requestBody.getOperator());
				o.setAmount(requestBody.getAmount());
				o.setCreateTime(new Date());
				o.setOrderNo(requestBody.getOrderNo());
				o.setRemark(requestBody.getRemark());
				o.setType(requestBody.getType());
				o.setFromAccount(requestBody.getFromAccount());
				o.setMemberUserName(requestBody.getUsername());
				o.setMemberRealName(to != null ? to.getOwner() : requestBody.getRealname());
				if (o.getType() != null && (o.getType().equals(IncomeRequestType.PlatFromAli.getType())
						|| o.getType().equals(IncomeRequestType.PlatFromWechat.getType()))) {
					o.setStatus(IncomeRequestStatus.Matched.getStatus());
				} else {
					o.setStatus(IncomeRequestStatus.Matching.getStatus());
				}
				if (null != requestBody.getFee() && StringUtils.isNotEmpty(requestBody.getFee())) {
					o.setFee(new BigDecimal(requestBody.getFee()));
				}
				if (!IncomeRequestType.WithdrawThirdToCustomer.getType().equals(requestBody.getType())) {
					// 提现到客户卡无需解锁
					allocateTransferService.unlockTrans(requestBody.getFromId(), o.getToId(), operator.getId());
				} else {
					// 客户卡
					o.setToAccountBank(requestBody.getToAccountBank());
				}
			}
			String res = incomeRequestService.saveOnly(o);
			log.info(" 入款单 ,单号:{} 入库保存结果:{}", o.getOrderNo(), res);

			if ("ok".equals(res)) {
				StringRedisTemplate template = redisService.getStringRedisTemplate();
				ListOperations operations = template.opsForList();
				boolean multiToAccount = Objects.nonNull(o.getToAccount()) && o.getToAccount().contains("#")
						&& Objects.nonNull(o.getType()) && IncomeRequestType.isPlatform(o.getType());
				String orderNo = o.getOrderNo();
				Integer handicap = o.getHandicap();
				Integer type = o.getType();
				// 订单号#盘口id#订单类型#金额 如果对应多个收款账号(多个收款账号是以#分割) 则 ...#toAccount1#toAccount2...
				// I399818354779395O699#1#3#100.16
				StringBuilder stringBuilder = new StringBuilder(orderNo).append("#").append(handicap).append("#")
						.append(type).append("#").append(o.getAmount());
				String pushStr = !multiToAccount ? stringBuilder.toString()
						: stringBuilder.append("#").append(o.getToAccount()).toString();
				long ret = operations.leftPush(RedisKeys.INCOME_REQUEST, pushStr);
				log.info("单号 + handicap+type  :{} 存入 redis结果:{} ", pushStr, ret);
				// 保存成功才能返回 OK
				return success;
			}
			error500.setMessage(res);
			return error500;
		} catch (Exception e) {
			log.error(" 处理入款订单 异常:", e);
			error500.setMessage(e.getLocalizedMessage());
			return error500;
		}
	}

	public static String saveThirdOrder(BizThirdRequest third, Logger log, RedisService redisService,
			ThirdRequestService thirdRequestService) {
		String res = "ok";
		BizThirdRequest exist;
		log.info("直接保存第三方订单,参数:{}", third);
		Integer handicap = third.getHandicap();
		String orderNo = third.getOrderNo();
		BigDecimal amount = third.getAmount();
		try {
			exist = thirdRequestService.findByHandicapAndOrderNo(handicap, orderNo);
			if (null == exist) {
				thirdRequestService.save(third);
				log.info("单号 orderNo :{} handicap:{} amount :{} 存入 redis  ", orderNo, handicap, amount);
				String cacheStr = orderNo + "#" + handicap + "#" + amount;
				redisService.getStringRedisTemplate().opsForList().rightPush(RedisKeys.INCOME_THIRD_REQUEST, cacheStr);
				log.info("把第三方单号 :{} 缓存信息 {} 放入redis 成功!", orderNo, cacheStr);
			}
		} catch (Exception e) {
			log.error("直接保存第三方订单:{},并把单号放入redis 异常:{}", orderNo, e.getLocalizedMessage());
			res = e.getLocalizedMessage();
		}
		return res;
	}

	/**
	 * 查询是否已经同步过了
	 * 
	 * @param orderNo
	 * @param handicapId
	 * @param amount
	 * @return true 已存在 false 不存在
	 */
	private BizIncomeRequest alreadyExisted(String orderNo, Integer handicapId, BigDecimal amount) {
		BizIncomeRequest record = incomeRequestService.findByOrderNoAndHandicapAndAmount(orderNo, handicapId, amount);
		return record;
	}

	private void updateExisted(BizIncomeRequest incomeRequest) {
		incomeRequestService.updateRealName(incomeRequest.getMemberRealName(), incomeRequest.getId());
	}
}
