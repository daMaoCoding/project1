package com.xinbo.fundstransfer.restful.v2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.TreeMap;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public abstract class TokenValidation {
	private static final Logger log = LoggerFactory.getLogger(TokenValidation.class);
	@Value("${funds.transfer.keystore}")
	private String keystore;

	/**
	 * 新系统 md5生成规则
	 * 
	 * @param content
	 * @return
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	public String md5digest(String content, String salt) {
		try {
			// 将字符串返序列化成byte数组
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(content + salt);
			oos.close();
			byte[] bToEnc = baos.toByteArray();
			// 将byte数组进行“消息摘要”计算，得到加密串(result)
			MessageDigest e = MessageDigest.getInstance("MD5");
			e.update(bToEnc);
			return new BigInteger(1, e.digest()).toString(16);
		} catch (Exception e) {
			log.error("", e);
			return "";
		}
	}

	public String md5digest(TreeMap<String, String> params, String salt) {
		StringBuilder target = new StringBuilder();
		for (String val : params.values()) {
			if (Objects.nonNull(val)) {
				target.append(val.trim());
			}
		}
		return md5digest(target.toString(), salt);
	}

	public boolean checkTokenMapParam(TreeMap<String, String> params, String token) {
		StringBuilder target = new StringBuilder();
		for (String val : params.values()) {
			if (Objects.nonNull(val)) {
				target.append(val.trim());
			}
		}
		return !ObjectUtils.notEqual(token, md5digest(target.toString(), keystore));
	}

	public boolean checkToken(String token, String handicap) {
		try {
			if (md5digest(handicap, keystore).equals(token)) {
				return true;
			}
		} catch (Exception e) {
			return false;
		}
		return false;
	}

	public boolean checkToken(String token, String handicap, String orderNo) {
		try {
			if (md5digest(handicap + orderNo, keystore).equals(token)) {
				return true;
			}
		} catch (Exception e) {
			return false;
		}
		return false;
	}
}
