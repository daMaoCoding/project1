package com.xinbo.fundstransfer.component.codec;

public class SaltPassword {
	public static final int SALT_SIZE = 8;
	public static final int INTERATIONS = 1024;
	public static final String ALGORITHM = "SHA-1";
	public String salt;
	public String password;

	/**
	 * 给密码加密，返回加密后的密码与对应的Salt值
	 * 
	 * @param password
	 * @return
	 */
	public static SaltPassword encryptPassword(String password) {
		SaltPassword saltPassword = new SaltPassword();
		byte[] salt = Digests.generateSalt(SALT_SIZE);
		saltPassword.salt = Encodes.encodeHex(salt);
		byte[] hashPassword = Digests.sha1(password.getBytes(), salt, INTERATIONS);
		saltPassword.password = Encodes.encodeHex(hashPassword);
		return saltPassword;
	}

	/**
	 * 校验密码
	 * 
	 * @param password
	 * @param hexPassword
	 * @param salt
	 * @return
	 */
	public static boolean checkPassword(String password, String hexPassword, String salt) {
		byte[] hashPassword = Digests.sha1(password.getBytes(), Encodes.decodeHex(salt), 1024);
		return hexPassword.equals(Encodes.encodeHex(hashPassword));
	}
}
