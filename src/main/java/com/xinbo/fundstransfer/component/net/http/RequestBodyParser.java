package com.xinbo.fundstransfer.component.net.http;

import com.alibaba.fastjson.JSON;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.service.HandicapService;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * 系统调平台 生成token和请求内容 注意各个环境的 key uri
 */
@Component
public class RequestBodyParser {
	private static final Logger log = LoggerFactory.getLogger(RequestBodyParser.class);
	@Autowired
	private Environment environment;
	@Autowired
	private HandicapService handicapService;
	@Value("${funds.transfer.keystore}")
	String keystore;

	/**
	 * 根据盘口编码获取大部份通用场景body内容
	 *
	 * @param handicapId
	 *            盘口ID
	 * @param memberCode
	 *            会员编码
	 * @param orderNo
	 *            订单号
	 * @param remark
	 *            备注信息
	 * @return
	 */
	public RequestBody general(Integer handicapId, String memberCode, String orderNo, String remark) {
		return general(handicapService.findFromCacheById(handicapId).getCode(), memberCode, orderNo, remark);
	}

	/**
	 * 生成批量取消参数
	 *
	 * @param handicap
	 * @param sOrderIdList
	 * @param remark
	 * @return
	 */
	public RequestBody general(String handicap, String sOrderIdList, String remark) {
		try {
			String pwd = environment.getProperty("funds.transfer.pwd." + handicap);
			String uid = environment.getProperty("funds.transfer.uid." + handicap);
			Map<String, String> map = new TreeMap<String, String>(new Comparator<String>() {
				@Override
				public int compare(String obj1, String obj2) {
					return obj1.compareTo(obj2);
				}
			});
			if (StringUtils.isNotEmpty(remark)) {
				map.put("sRemark", remark);
			}
			map.put("sOrderIdList", sOrderIdList);
			map.put("CurUserID", uid);
			map.put("CurPassword", pwd);
			StringBuilder sb = new StringBuilder();
			Set<Map.Entry<String, String>> entrySet = map.entrySet();
			for (Map.Entry<String, String> entry : entrySet) {
				sb.append(entry.getValue());
			}
			String json = "{\"CurUserID\":\"" + uid + "\",\"CurPassword\":\"" + pwd + "\",\"sOrderIdList\":\""
					+ sOrderIdList + "\",\"Token\":\"" + generalToken(sb.toString(), handicap);
			if (StringUtils.isNotEmpty(remark)) {
				json += "\",\"sRemark\":\"" + remark + "\"}";
			} else {
				json += "\"}";
			}
			log.debug("Build DepositMoneyBatchCancel RequestBody, {}, URL({})", json,
					environment.getProperty("funds.transfer.url." + handicap));
			return RequestBody.create(MediaType.parse("application/json"), json);
		} catch (Exception e) {
			log.error("", e);
			return null;
		}
	}

	/**
	 * 根据盘口编码获取大部份通用场景body内容
	 *
	 * @param handicap
	 *            盘口编码
	 * @param memberCode
	 *            会员编码
	 * @param orderNo
	 *            订单号
	 * @param remark
	 *            备注信息
	 * @return
	 */
	public RequestBody general(String handicap, String memberCode, String orderNo, String remark) {
		try {
			String pwd = environment.getProperty("funds.transfer.pwd." + handicap);
			String uid = environment.getProperty("funds.transfer.uid." + handicap);
			Map<String, String> map = new TreeMap<String, String>(new Comparator<String>() {
				@Override
				public int compare(String obj1, String obj2) {
					return obj1.compareTo(obj2);
				}
			});
			map.put("iUserKey", memberCode);
			if (StringUtils.isNotEmpty(remark)) {
				map.put("sRemark", remark);
			}
			map.put("sOrderId", orderNo);
			map.put("CurUserID", uid);
			map.put("CurPassword", pwd);
			StringBuilder sb = new StringBuilder();
			Set<Map.Entry<String, String>> entrySet = map.entrySet();
			for (Map.Entry<String, String> entry : entrySet) {
				sb.append(entry.getValue());
			}

			String json = "{\"CurUserID\":\"" + uid + "\",\"CurPassword\":\"" + pwd + "\",\"iUserKey\":" + memberCode
					+ ",\"sOrderId\":\"" + orderNo + "\",\"Token\":\"" + generalToken(sb.toString(), handicap);
			if (StringUtils.isNotEmpty(remark)) {
				json += "\",\"sRemark\":\"" + remark + "\"}";
			} else {
				json += "\"}";
			}
			log.debug("Build RequestBody, {}, URL({})", json,
					environment.getProperty("funds.transfer.url." + handicap));
			return RequestBody.create(MediaType.parse("application/json"), json);
		} catch (NoSuchAlgorithmException e) {
			log.error("", e);
			return null;
		}
	}

	/**
	 * 旧平台 公司入款补提单，生成请求body <br/>
	 * handicap，盘口编码， sUserID，会员帐号 fAmount，充值金额 sDepositName，存款人姓名 sBankAccount，收款卡号
	 * sBankType，收款银行 sReceiptsHuman，收款人姓名 sBankAddr，收款人开户行 sRemark，备注
	 * iSaveType，存款方式 ,1网银转账，2ATM自动柜员机，3ATM现金入款，4银行柜台，5手机银行，10其他
	 * sSaveTime，存款时间，格式必须是：YYYY-MM-DD HH:MM:SS
	 *
	 * @param handicap
	 * @param sUserID
	 * @param sRemark
	 * @param sDepositName
	 * @param sBankAccount
	 * @param sBankType
	 * @param sBankAddr
	 * @param sReceiptsHuman
	 * @param sSaveTime
	 * @param fAmount
	 * @param iSaveType
	 * @return
	 */
	public RequestBody generalDeposit(String handicap, String sUserID, String sRemark, String sDepositName,
			String sBankAccount, String sBankType, String sBankAddr, String sReceiptsHuman, String sSaveTime,
			float fAmount, int iSaveType) {
		try {
			int From = 5;// From(1网页，2WAP，3安卓，4苹果，5外部其他系统调用)
			Map<String, String> map = new TreeMap<>(Comparator.naturalOrder());
			// 注意大小写排序
			map.put("sUserID", sUserID);
			map.put("sRemark", sRemark);
			map.put("fAmount", String.valueOf(fAmount));
			map.put("sDepositName", sDepositName);
			map.put("sBankAccount", sBankAccount);
			map.put("sBankType", sBankType);
			map.put("sReceiptsHuman", sReceiptsHuman);
			map.put("sBankAddr", sBankAddr);
			map.put("iSaveType", String.valueOf(iSaveType));
			map.put("sSaveTime", sSaveTime);
			map.put("sCorpCode", handicap);
			map.put("from", String.valueOf(From));
			StringBuilder sb = new StringBuilder();
			Set<Map.Entry<String, String>> entrySet = map.entrySet();
			for (Map.Entry<String, String> entry : entrySet) {
				sb.append(entry.getValue());
			}

			String json = "{\"sUserID\":\"" + sUserID + "\",\"sRemark\":\"" + sRemark + "\",\"sDepositName\":\""
					+ sDepositName + "\",\"sBankAccount\":\"" + sBankAccount + "\",\"sBankType\":\"" + sBankType
					+ "\",\"sBankAddr\":\"" + sBankAddr + "\",\"sReceiptsHuman\":\"" + sReceiptsHuman
					+ "\",\"sSaveTime\":\"" + sSaveTime + "\",\"sCorpCode\":\"" + handicap + "\",\"From\":" + From
					+ ",\"fAmount\":" + fAmount + ",\"iSaveType\":" + iSaveType + ",\"Token\":\""
					+ generalToken(sb.toString(), handicap) + "\"}";
			log.debug("Build RequestBody, {}, URL({})", json,
					environment.getProperty("funds.transfer.url." + handicap));
			return RequestBody.create(MediaType.parse("application/json"), json);
		} catch (NoSuchAlgorithmException e) {
			log.error("", e);
			return null;
		}
	}

	/**
	 * 根据盘口，对字符串进行加密
	 *
	 * @param content
	 * @param handicap
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	private String generalToken(String content, String handicap) throws NoSuchAlgorithmException {
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		md5.update((content + environment.getProperty("funds.transfer.token." + handicap)).getBytes());
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
		return md5Token.toString();
	}

	/**
	 * 新系统 md5生成规则
	 *
	 * @param content
	 * @return
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	public String md5digest(String content) {
		try {
			// 将字符串返序列化成byte数组
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(content);
			oos.close();
			MessageDigest e = MessageDigest.getInstance("MD5");
			e.update(baos.toByteArray());
			return new BigInteger(1, e.digest()).toString(16);
		} catch (Exception e) {
			return "";
		}
	}

	/**
	 * 新平台 入款补提单
	 *
	 * @param handicap
	 *            盘口编码
	 * @param userId
	 *            会员帐号
	 * @param amount
	 *            充值金额
	 * @param depositName
	 *            存款人姓名
	 * @param bankAccount
	 *            收款卡号
	 * @param remark
	 *            备注
	 * @param saveType
	 *            存款方式，1网银转账，2ATM自动柜员机，3ATM现金入款，4银行柜台，5手机银行，10其他
	 * @param saveTime
	 *            存款时间，格式必须是：YYYY-MM-DD HH:MM:SS
	 * @return
	 */
	public RequestBody buildDepositRequestBody(Integer pfTypeSub, String handicap, String userId, BigDecimal amount,
			String depositName, String bankAccount, String remark, int saveType, String saveTime, String province,
			String city, int bankType, int subType) {
		String json = "{\"handicap\":\"" + handicap + "\",\"pfTypeSub\":\"" + pfTypeSub + "\",\"userId\":\"" + userId
				+ "\",\"amount\":" + amount + ",\"depositName\":\"" + depositName + "\",\"bankAccount\":\""
				+ bankAccount + "\",\"saveTime\":\"" + saveTime + "\",\"saveType\":" + saveType + ",\"token\":\""
				+ md5digest(handicap + keystore);
		if (StringUtils.isNotBlank(province)) {
			json += "\",\"province\":\"" + province;
		}
		if (StringUtils.isNotBlank(city)) {
			json += "\",\"city\":\"" + city;
		}
		if (StringUtils.isNotEmpty(remark)) {
			json += "\",\"remark\":\"" + remark;
		}
		json += "\",\"bankTypeId\":" + bankType;
		json += ",\"inBankSubType\":" + subType;
		json += "}";
		log.debug(json);
		return RequestBody.create(MediaType.parse("application/json"), json);
	}

	/**
	 * 同步body
	 *
	 * @param handicap
	 *            盘口编码
	 * @param type
	 *            类型1-入款，2-出款，3-帐号信息，4-层级信息 5-出款通道信息
	 * @return
	 */
	public RequestBody buildSyncRequestBody(String handicap, int type) {
		String json = "{\"handicap\":\"" + handicap + "\",\"type\":" + type + ",\"token\":\""
				+ md5digest(handicap + keystore) + "\"}";
		log.debug(json);
		return RequestBody.create(MediaType.parse("application/json"), json);
	}

	/**
	 * 新平台 回调通用body，看文档说明
	 *
	 * @param handicap
	 *            盘口编码
	 * @param orderNo
	 *            订单号
	 * @param remark
	 *            备注
	 * @return
	 */
	public RequestBody buildRequestBody(String handicap, String orderNo, String remark) {
		String json = "{\"handicap\":\"" + handicap + "\",\"orderNo\":\"" + orderNo + "\",\"token\":\""
				+ md5digest(handicap + orderNo + keystore);
		if (StringUtils.isNotEmpty(remark)) {
			json += "\",\"remark\":\"" + remark + "\"}";
		} else {
			json += "\"}";
		}
		log.debug(json);
		return RequestBody.create(MediaType.parse("application/json"), json);
	}

	/**
	 * 新平台 回调通用body，看文档说明
	 *
	 * @param handicapCode
	 *            盘口编码
	 * @param orderNo
	 *            订单号
	 * @param remark
	 *            备注
	 * @return
	 */
	public RequestBody buildRequestBodyForDaifu(String handicapCode, String orderNo, String remark, String channelName,
			String memberId, String platPayCode) {
		String json = "{\"handicap\":\"" + handicapCode + "\",\"orderNo\":\"" + orderNo + "\",\"token\":\""
				+ md5digest(handicapCode + orderNo + keystore);
		if (StringUtils.isNotEmpty(remark)) {
			json += "\",\"remark\":\"" + remark;
		}
		if (StringUtils.isNotBlank(memberId) && StringUtils.isNotBlank(channelName)
				&& StringUtils.isNotBlank(platPayCode)) {
			json += "\",\"memberId\":\"" + memberId;
			json += "\",\"channelName\":\"" + channelName;
			json += "\",\"platPayCode\":\"" + platPayCode;
		}
		json += " \" }";
		log.debug("代付成功 调用通知平台 参数:{}", json);
		return RequestBody.create(MediaType.parse("application/json"), json);
	}

	/**
	 * 6.2.14 通知平台出款订单使用哪一个第三方出款
	 * 
	 * @param handicapCode
	 * @param orderNo
	 * @param platPayCode
	 * @param channelName
	 * @param memberId
	 * @return
	 */
	public RequestBody buildRequestBodyForSelectDaifu(String handicapCode, String orderNo, String platPayCode,
			String channelName, String memberId) {
		String json = "{\"handicap\":\"" + handicapCode + "\",\"orderNo\":\"" + orderNo + "\",\"platPayCode\":\""
				+ platPayCode + "\",\"memberId\":\"" + memberId + "\",\"channelName\":\"" + channelName + " \" }";
		log.debug("代付调用后  通知平台出款订单使用哪一个第三方出款 调用通知平台 参数:{}", json);
		return RequestBody.create(MediaType.parse("application/json"), json);
	}

	/**
	 * 1.1.38 锁定money在12小时内不能再次使用
	 *
	 * @param handicapCode
	 * @param cardNo
	 * @param money
	 * @return
	 */
	public RequestBody moneyBeUsedRequestBody(Integer handicapCode, String cardNo, Number money) {
		if (Objects.isNull(handicapCode) || Objects.isNull(cardNo) || Objects.isNull(money)) {
			return null;
		}
		String json = "{\"oid\":" + handicapCode + ",\"cardNo\":\"" + cardNo + "\",\"money\":" + money + "}";
		log.info("moneyBeUsedRequestBody params:{}", json);
		return RequestBody.create(MediaType.parse("application/json"), json);
	}

	public RequestBody querySurpportBankType(String provider, String oid) {
		log.debug("查询平台供应商支持银行类型参数:{},{}", provider, oid);
		String json = "{\"oid\":\"" + oid;
		if (StringUtils.isNotBlank(provider)) {
			json += "\",\"provider\":\"" + provider;
		}
		json += "\"}";
		log.debug("查询平台供应商支持银行类型参数 json:{}", json);
		return RequestBody.create(MediaType.parse("application/json"), json);
	}

	public String getmd5ByAccountHandicap(String account, String handicap) {
		if (null == account && null == handicap) {
			return null;
		}
		return md5digest(handicap + account + keystore);
	}

	/**
	 * 校验切换账号类型是否有告警
	 *
	 * @param need
	 * @return
	 */
	public RequestBody buildRequestBodyForCheckAlarm(Map<String, List<AccountBaseInfo>> need) {
		List<Map<String, Object>> list = new ArrayList<>();
		for (Map.Entry<String, List<AccountBaseInfo>> entry : need.entrySet()) {
			Map<String, Object> res = new HashMap<>();
			res.put("oid", entry.getKey());
			List<Map<String, String>> list1 = new ArrayList<>();
			for (AccountBaseInfo base : entry.getValue()) {
				Map<String, String> map = new HashMap<>();
				map.put("pocId", base.getPassageId() + "");
				map.put("cardNo", base.getAccount());
				list1.add(map);
			}
			res.put("subList", list1);
			list.add(res);
		}
		/**
		 * StringBuffer buffer = new StringBuffer(); buffer.append("["); for
		 * (Map.Entry<String, List<AccountBaseInfo>> entry : need.entrySet()) {
		 * buffer.append("{"); buffer.append("\"oid\":"); buffer.append("\"");
		 * buffer.append(entry.getKey()); buffer.append("\",");
		 * buffer.append("\"subList\":["); for(AccountBaseInfo base:entry.getValue()){
		 * buffer.append("{"); buffer.append("\"pocId\":"); buffer.append("\"");
		 * buffer.append(base.getPassageId()); buffer.append("\",");
		 * buffer.append("\"cardNo\":"); buffer.append("\"");
		 * buffer.append(base.getAccount()); buffer.append("\""); buffer.append("},"); }
		 * if(buffer.lastIndexOf(",")==buffer.length()-1){
		 * buffer.deleteCharAt(buffer.lastIndexOf(",")); } buffer.append("]");
		 * buffer.append("},"); } if(buffer.lastIndexOf(",")==buffer.length()-1){
		 * buffer.deleteCharAt(buffer.lastIndexOf(",")); } buffer.append("]");
		 **/
		return RequestBody.create(MediaType.parse("application/json"), JSON.toJSON(list).toString());
	}
}
