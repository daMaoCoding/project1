/**
 * 
 */
package com.xinbo.fundstransfer.daifucomponent.exception;
/**
 * @author blake
 *
 */
public class PayPlatNotCallAgainException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8725850301223297374L;
	
	public PayPlatNotCallAgainException(String errorMsg) {
		super(errorMsg);
	}
	
	public PayPlatNotCallAgainException(String errorMsg,Throwable e) {
		super(errorMsg,e);
	}

}
