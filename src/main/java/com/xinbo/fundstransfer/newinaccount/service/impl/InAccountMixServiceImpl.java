package com.xinbo.fundstransfer.newinaccount.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.enums.InBankSubType;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.newinaccount.dto.input.CardForPayInputDTO;
import com.xinbo.fundstransfer.newinaccount.dto.output.CardForPayOutputDTO;
import com.xinbo.fundstransfer.newinaccount.dto.output.CardForPayOutputDTO.BankInfo;
import com.xinbo.fundstransfer.newinaccount.service.InAccountMixService;
import com.xinbo.fundstransfer.restful.v2.TokenValidation;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.RedisService;
import com.xinbo.fundstransfer.unionpay.ysf.service.impl.YSFServiceImpl;
import com.xinbo.fundstransfer.unionpay.ysf.util.YSFLocalCacheUtil;
import com.xinbo.fundstransfer.utils.randutil.InAccountRandUtil;
import com.xinbo.fundstransfer.utils.randutil.NoAvailableRandomException;

import lombok.extern.slf4j.Slf4j;

/**
 * @author blake
 */
@Service
@Slf4j
public class InAccountMixServiceImpl extends TokenValidation implements InAccountMixService {
	@Autowired
	private AccountService accountService;
	@Autowired
	private YSFServiceImpl ysfService;
	@Autowired
	private YSFLocalCacheUtil ySFLocalCacheUtil;
	@Autowired
	private InAccountRandUtil inAccountRandUtil;
	
	@Autowired
	RedisService redisService;
	
	/**
	 * 新公司入款与云闪付混合<br>
	 * 当inputDTO.getType()=4 时使用
	 */
	@Override
	public CardForPayOutputDTO cardForPayMix(CardForPayInputDTO inputDTO, BizHandicap handicap) {
		Assert.isTrue(inputDTO.getType()!=null && InBankSubType.IN_BANK_YSF_MIX.getSubType().equals(Integer.valueOf(inputDTO.getType())), "错误的使用");
		Assert.isTrue(!CollectionUtils.isEmpty(inputDTO.getPocIdMapCardNosCol()), "pocIdMapCardNosCol不能为空");
		//获取全部银行卡
		List<String> allBankCards = new ArrayList<>();
		//银行卡号与通道关系map
		Map<String,String> bankCardPocIdMap = new HashMap<String, String>();
		//从参数中取出所有的卡
		inputDTO.getPocIdMapCardNosCol().entrySet().stream().forEach(t ->{
			String tmpKey = t.getKey().replace("+", "");
			List<String> tmpBankCards = Arrays.asList(t.getValue().split(",")); 
			if(!StringUtils.isEmpty(tmpKey)) {
				tmpBankCards.forEach(bankCard->{bankCardPocIdMap.put(bankCard, tmpKey);});
			}
			allBankCards.addAll(tmpBankCards);
		});
		
		//获取可用银行卡
		List<String> allAvailableBankCards = ysfService.getAvailableBankAccountList(handicap.getId(), inputDTO.getAmount().intValue(), allBankCards);
		CardForPayOutputDTO result = null;
		if(!CollectionUtils.isEmpty(allAvailableBankCards)) {
			List<String> sortedAllAvailableBankCards = ysfService.availableBankAccountSort(handicap.getId(), allAvailableBankCards);
			//使用时间升序排列可用银行卡
			//对本次用到的所有银行卡进行排序
			for(String bankCard : sortedAllAvailableBankCards) {
				try {
					//根据新支付-云闪付的银行卡获取结构
					//根据新支付-银行卡转账的银行卡获取结构
					log.info("尝试使用云闪付银行卡{}的二维码",bankCard);
					result = getByYsfBankCard(handicap,bankCardPocIdMap.get(bankCard),bankCard,inputDTO.getUserName(),inputDTO.getAmount().intValue());
					return result;
				}catch (NoAvailableRandomException e) {
					log.error("银行卡{}未能获取到整数金额{}的随机金额",bankCard,inputDTO.getAmount());
					continue;
				}
			}
		}else {
			log.error("在当前传递进来的银行卡中无可用卡，参数：{}",Arrays.toString(allBankCards.toArray()));
		}
		return result;
	}
	
	/**
	 * 通过云闪付银行卡返回<br>
	 * 如果没有获取到二维码信息也要返回
	 * @param handicap 盘口号
	 * @param ocIdStr 通道字符串
	 * @param ysfBankCard 云闪付银行卡
	 * @param userName 平台用户名
	 * @param reqMoney 请求金额，整数
	 * @return
	 * @throws NoAvailableRandomException 无可用随机数时抛出该异常
	 */
	private CardForPayOutputDTO getByYsfBankCard(BizHandicap handicap,String ocIdStr, String ysfBankCard,String userName,Integer reqMoney) throws NoAvailableRandomException {
		CardForPayOutputDTO result = new CardForPayOutputDTO();
		AccountBaseInfo account = accountService.getFromCacheByHandicapIdAndAccount(handicap.getId(),
				ysfBankCard);
		if (account != null) {
			List<BankInfo> accountList = new ArrayList<>();
			BankInfo e = new BankInfo();
			e.setBankName(account.getBankName());
			e.setBankType(account.getBankType());
			e.setCardNo(ysfBankCard);
			e.setCity(account.getCity());
			e.setOwner(account.getOwner());
			e.setProvince(account.getProvince());
			accountList.add(e);
			result.setAccountList(accountList);
		}
		result.setAmount(reqMoney);
		result.setOid(Integer.parseInt(handicap.getCode()));
		if (!StringUtils.isEmpty(ocIdStr)) {
			if (ocIdStr.contains("+")) {
				ocIdStr = ocIdStr.replaceAll("\\+", "");
			}
			result.setPocId(Long.parseLong(ocIdStr));
		}
		result.setType(InBankSubType.IN_BANK_YSF_MIX.getSubType().byteValue());
		
		BigDecimal finalAmount = inAccountRandUtil.getRandomStr(reqMoney, ysfBankCard, userName);
		log.info("混合形式_尝试使用银行卡{}的随机金额{}",ysfBankCard,finalAmount);
		result.setFinalAmount(finalAmount);
		final ExecutorService exec = Executors.newFixedThreadPool(1);
		try {
			Callable<String> call = new Callable<String>() {
				@Override
				public String call() throws Exception {
					String key = ysfService.getCurrentBankQrCacheKey(ysfBankCard, reqMoney);
					log.debug("混合形式_获取key={}今日已经生成的二维码", key);
					Map<String, String> existsQrCodeMap = new HashMap<String, String>();
					StringRedisTemplate jedis = redisService.getStringRedisTemplate();
					if (jedis != null) {
						Map<Object, Object> existsQrCodeMap1 = jedis.boundHashOps(key).entries();
						if(!ObjectUtils.isEmpty(existsQrCodeMap1)) {
							for(Entry<Object, Object> o:existsQrCodeMap1.entrySet()) {
								existsQrCodeMap.put((String)o.getKey(), (String)o.getValue());
							}
						}
					}
					log.debug("混合形式_线程 {} 银行卡号{}今日对整数金额{}已经生成的二维码数量为{}", Thread.currentThread().getName(), ysfBankCard, reqMoney,
							existsQrCodeMap.size());
					String finalAmountStr = InAccountRandUtil.moneyFormat(finalAmount);
					// 成功获取到二维码并且为超时，设置用户金额锁
					ysfService.setUserLockStr(userName, result);
					String qrContent = existsQrCodeMap.get(finalAmountStr);
					log.debug("混合形式_线程 {} 获得银行卡号{}随机金额{}锁AAAAAAAAAAAAAAAAAA", Thread.currentThread().getName(), ysfBankCard,
							finalAmountStr);
					if (ObjectUtils.isEmpty(qrContent)) {
						log.debug("混合形式_线程 {} 银行卡号{}的随机金额{}对应二维码内容为空，下发app生成二维码", Thread.currentThread().getName(),
								ysfBankCard, finalAmountStr);
						// 如果二维码不存在，则去生成二维码，并且生成二维码后直接返回
						qrContent = ysfService.appGenQrCode(handicap.getId(), ysfBankCard, finalAmountStr);
					}
					return qrContent;
				}
			};
			Future<String> future = exec.submit(call);
			String qrCode = future.get(ySFLocalCacheUtil.getQrProcessTimeOutSeconds(), TimeUnit.SECONDS);
			log.info("混合形式_尝试使用银行卡{}的随机金额{}，获取到的二维码内容qrCode = {}",ysfBankCard,finalAmount,qrCode);
			result.getAccountList().iterator().next().setQrCode(qrCode);
			return result;
		} catch (Exception e) {
			log.error("混合形式_获取云闪付二维码时产生异常，将返回不带二维码内容的数据，异常信息:{}", e);
		} finally {
			exec.shutdown();
		}
		return result;
	}
}
