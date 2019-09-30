package com.xinbo.fundstransfer.domain.pojo;

/**
 * 平台响应结果
 * 
 * 
 *
 */
public class PlatformResponse {
	/** 调用的涵数名 */
	String Func;
	/** 0-失败，1-成功 */
	int Result;
	/** 错误码 */
	int Error;
	/** 描述信息 */
	String Desc;

	public String getFunc() {
		return Func;
	}

	public void setFunc(String func) {
		Func = func;
	}

	public int getResult() {
		return Result;
	}

	public void setResult(int result) {
		Result = result;
	}

	public String getDesc() {
		return Desc;
	}

	public void setDesc(String desc) {
		Desc = desc;
	}

}
