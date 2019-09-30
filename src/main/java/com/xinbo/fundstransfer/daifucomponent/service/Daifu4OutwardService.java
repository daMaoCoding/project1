/**
 * 
 */
package com.xinbo.fundstransfer.daifucomponent.service;

import com.xinbo.fundstransfer.daifucomponent.dto.output.DaifuResult;
import com.xinbo.fundstransfer.daifucomponent.dto.output.DaifuResult.ResultEnum;
import com.xinbo.fundstransfer.domain.entity.BizOutwardTask;

/**
 * @author blake
 *
 */
public interface Daifu4OutwardService {
	/**
	 * 判断某个盘口 会员提单的收款银行卡是否在支持的银行卡类型中
	 * @param handicapId 订单所属盘口id
	 * @param bankType 订单收款银行卡类型
	 * @return true 表示支持 false 表示不支持
	 */
	boolean isSupportedBankType(Integer handicapId, String bankType);
	/**
	 * 是否可以使用第三方代付
	 * @param outward 出任任务。<br>
	 * 	outward对象以下参数不能为空:handicap,id,outwardRequestId,orderNo,amount<br>
	 * 	其中：amount必须大于0
	 * @return
	 */
	boolean isRead(BizOutwardTask outward);
	
	/**<pre>
	 * 请求使用第三方代付并返回代付结果
	 * 调用方需要根据返回结果的result来进行不同处理
	 * result 可能的返回值有：
	 * ResultEnum.NO_OUT_REQUEST 数据异常，出款单不存在。
	 * ResultEnum.NO_READ 不满足使用第三方代付的条件
	 * ResultEnum.PROC_EXCEPTION 处理异常，可以重试
	 * ResultEnum.ERROR 第三方代付取消（包括第三方回调取消或者干预取消）
	 * ResultEnum.SUCCESS 第三方代付支付完成（包括第三方回调支付完成或者干预处理完成）
	 * ResultEnum.PAYING 第三方正在支付
	 * ResultEnum.UNKOWN 未能获取一个明确的结果
	 * </pre>
	 * @param outward 出任任务。<br>
	 * 	outward对象以下参数不能为空:handicap,operator,id,outwardRequestId,orderNo,amount<br>
	 * 	其中：operator为操作任意id，如果系统自动分配，请约定一个固定值；amount必须大于0
	 * @return
	 */
	DaifuResult daifu(BizOutwardTask outward);
	
	/**<pre>
	 * 查询代付结果
	 * 返回结果：
	 * ResultEnum.NO_OUT_REQUEST 数据异常，造成原因是：出款单不存在。
	 * ResultEnum.NO_DAIFU_INFO 数据异常，造成原因是：代付订单不存在。
	 * ResultEnum.SUCCESS 第三方代付支付完成（包括第三方回调支付完成或者干预处理完成）
	 * ResultEnum.ERROR 第三方代付取消（包括第三方回调取消或者干预取消）
	 * ResultEnum.PAYING 第三方正在支付
	 * </pre>
	 * @param outward 出任任务。<br>
	 * 	outward对象以下参数不能为空:handicap,id,outwardRequestId,orderNo<br>
	 * @return
	 */
	DaifuResult query(BizOutwardTask outward);
	
	/**<pre>
	 * 客服干预代付结果
	 * 用于代付无法回调时客服登陆第三方查询代付结果后进行的确认出款/取消出款/支付中等操作
	 * 调用方需要根据返回值的result进行不同操作：
	 * result 的值可能与传入的入参 result不一致。
	 * result 可能的返回值有：
	 * ResultEnum.PROC_EXCEPTION 处理异常，可以重试
	 * ResultEnum.NO_OUT_REQUEST 数据异常，造成原因是：出款单不存在。
	 * ResultEnum.NO_DAIFU_INFO 数据异常，造成原因是：代付订单不存在。
	 * ResultEnum.SUCCESS 第三方代付支付完成（包括第三方回调支付完成或者干预处理完成）
	 * ResultEnum.ERROR 第三方代付取消（包括第三方回调取消或者干预取消）
	 * ResultEnum.PAYING 第三方正在支付
	 * </pre>
	 * @param outward 出任任务。
	 * 	outward对象以下参数不能为空:handicap,operator,outwardRequestId,orderNo
	 * @param result 操作人员从第三方获取到的支付结果，不能为空，取值范围[ResultEnum.SUCCESS|ResultEnum.PAYING|ResultEnum.ERROR]
	 * @return 代付处理结果，非空。
	 */
	DaifuResult intervene(BizOutwardTask outward,ResultEnum result);
}
