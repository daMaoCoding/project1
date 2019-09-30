/**
 * 
 */
package com.xinbo.fundstransfer.daifucomponent.util;

import java.util.List;
import java.util.Map;

import com.xinbo.fundstransfer.daifucomponent.entity.DaifuConfigRequest;

/**
 * @author blake
 *
 */
public class OutConfigHolder {
	
	private static final ThreadLocal<List<DaifuConfigRequest>> contextHolder = new ThreadLocal<>();
	private static final ThreadLocal<Map<Integer, Integer>> contextHolder2 = new ThreadLocal<>();

	public static void setReadyDaifuConfigList(List<DaifuConfigRequest> daifuConfigList,Map<Integer, Integer> userUseCount) {
		contextHolder.set(daifuConfigList);
		contextHolder2.set(userUseCount);
	}

	/**
	 * 获取满足适用的通道list
	 * @return
	 */
	public static List<DaifuConfigRequest> getReadyDaifuConfigList() {
		return contextHolder.get();
	}
	
	/**
	 * 获取用户今日通道适用次数
	 * @return
	 */
	public static Map<Integer, Integer> getReadyUserUseCount() {
		return contextHolder2.get();
	}

	public static void clearReadyDaifuConfigList() {
		contextHolder.remove();
		contextHolder2.remove();
	}

}
