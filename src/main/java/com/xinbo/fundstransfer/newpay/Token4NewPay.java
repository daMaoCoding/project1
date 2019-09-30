package com.xinbo.fundstransfer.newpay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;

/**
 * Created by Administrator on 2018/9/25.
 */
@Component
public class Token4NewPay {
	private static final Logger log = LoggerFactory.getLogger(Token4NewPay.class);
	@Value("${funds.transfer.keystore}")
	private String tokenKey;

	public String md5digest(String content, String tokenKey) {
		try {
			log.info("contents.toString() : {},tokenKey:{}", content, tokenKey);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(content + tokenKey);
			oos.close();
			byte[] bToEnc = baos.toByteArray();
			MessageDigest e = MessageDigest.getInstance("MD5");
			e.update(bToEnc);
			return new BigInteger(1, e.digest()).toString(16);
		} catch (Exception e) {
			log.error("md5digest execute fail :", e);
			return null;
		}
	}

	public String getToken4NewPay(Object[] params) {
		if (params == null || params.length == 0) {
			return null;
		}
		StringBuilder contents = new StringBuilder();
		for (Object obj : params) {
			contents.append(obj);
		}
		String token = md5digest(contents.toString(), tokenKey);
		return token;
	}
}
