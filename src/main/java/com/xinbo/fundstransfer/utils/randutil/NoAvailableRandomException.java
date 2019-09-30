/**
 * 
 */
package com.xinbo.fundstransfer.utils.randutil;

/**
 * 无可用随机金额时产生该异常
 * @author blake
 *
 */
public class NoAvailableRandomException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4257665701635509530L;
	
	public NoAvailableRandomException() {
		super("暂无可用随机数");
	}
	
	public NoAvailableRandomException(String msg) {
		super(msg);
	}
}
