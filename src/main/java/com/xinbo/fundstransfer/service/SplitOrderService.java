package com.xinbo.fundstransfer.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface SplitOrderService {

	enum SubOrderStatus {
		SPLIT(0, "拆分完成"), REPEAT(1, "重新出款"), SUCCESS(2, "完成出款");
		private Integer status;
		private String desc;

		SubOrderStatus(Integer status, String desc) {
			this.status = status;
			this.desc = desc;
		}

		public static String getStatusDesc(Integer status) {
			for (SubOrderStatus status1 : SubOrderStatus.values()) {
				if (status1.status.equals(status)) {
					return status1.getDesc();
				}
			}
			return null;
		}

		public static Integer getStatus(SubOrderStatus status) {
			for (SubOrderStatus status1 : SubOrderStatus.values()) {
				if (status.getStatus().equals(status1.status)) {
					return status1.getStatus();
				}
			}
			return null;
		}

		public Integer getStatus() {
			return status;
		}

		public void setStatus(Integer status) {
			this.status = status;
		}

		public String getDesc() {
			return desc;
		}

		public void setDesc(String desc) {
			this.desc = desc;
		}
	}

	/**
	 * 重新出款操作 重置
	 * 
	 * @param orderNo
	 * @param subOrder
	 * @param operatorId
	 * @return
	 */
	String resetFinished(String orderNo, String subOrder, Integer operatorId);

	void saveHistoryFee(String orderNo, String subOrder, BigDecimal fee, boolean save);

	BigDecimal getHistoryFee(String orderNo, String subOrder);

	/**
	 * 更新 三方账号的系统余额<br>
	 * 根据三方账号手续费规则
	 * 
	 * @param amount
	 * @param fee
	 * @param thirdId
	 * @param add
	 * @return
	 */
	String updateThirdAccountBalance(BigDecimal amount, BigDecimal fee, Integer thirdId, boolean add);

	/**
	 * 根据订单号查询金额
	 * 
	 * @param orderNo
	 * @return
	 */
	BigDecimal getOrderAmountByOrderNo(String orderNo);

	/**
	 * 根据用户id 查询是否有第三方设定的账号 才能拆单
	 * 
	 * @param operatorId
	 * @return
	 */
	boolean accessibleSplit(Integer operatorId);

	/**
	 * 拆单:根据输入的 {@see expectedSplitNum} 拆单 并缓存在redis 生成子订单时间戳加拆分数量均值
	 * 如果输入的订单号已经拆分则直接覆盖原来的拆分 重新拆分
	 * 
	 * @param orderNo
	 * @param expectedSplitNum
	 * @param operatorId
	 * @return
	 */
	String splitOrder(String orderNo, Integer expectedSplitNum, Integer operatorId);

	/**
	 * 取消拆单
	 * 
	 * @param orderNo
	 * @param operatorId
	 * @return
	 */
	String cancelSplit(String orderNo, Integer operatorId);

	/**
	 * 查询 用户对订单的
	 * 
	 * @param orderNo
	 * @param userId
	 * @return
	 */
	List<Map<String, String>> splitList(String orderNo, Integer userId);

	/**
	 * 新增或者删除 某个子单
	 * 
	 * @param subOrder
	 * @param orderNo
	 * @param operatorId
	 * @param type
	 * @return
	 */
	String updateSplit(String subOrder, String orderNo, Integer operatorId, Byte type);

	/**
	 * 保存 子订单 使用的第三方账号 金额
	 * 
	 * @param orderNo
	 * @param operatorId
	 * @param subOrder
	 * @param thirdId
	 * @param amount
	 * @param save
	 */
	void saveThirdIdAndAmount(String orderNo, Integer operatorId, String subOrder, Integer thirdId, BigDecimal amount,
			BigDecimal fee, boolean save);

	/**
	 * 查询 某个订单号拆单之后子订单使用的三方账号id和金额
	 * 
	 * @param orderNo
	 * @param operatorId
	 * @return
	 */
	Map<String, Map<String, String>> getUsedThirdId(String orderNo, Integer operatorId);

	String updateSubOrderFinish(Integer thirdId, String orderNo, Integer operatorId, String subOrderNo,
			BigDecimal amount, BigDecimal fee);

	String checkFeeAndOrderAmount(Integer thirdId, String orderNo, Integer operatorId, String subOrderNo,
			BigDecimal amount, BigDecimal fee);

	/**
	 * 子订单 点击完成之后 对剩下的未完成的订单拆分<br>
	 * 
	 * @param orderNo
	 * @param operatorId
	 */
	String splitSubFinishPostProccess(String orderNo, Integer operatorId);

	String[] unfinishedSubOrderHkeys(String orderNo, Integer operatorId);

	void deleteUnfinishSubOrder(String orderNo, Integer operatorId);

	Long finishedNums(String orderNo, Integer userId);

	Long unfinishedNums(Integer operatorId, String orderNo);

	boolean isPartialFinished(String orderNo, Integer userId);

	boolean isExceededAmount(BigDecimal amount1, BigDecimal amount2);

	BigDecimal amountAlreadyFinished(String orderNo, Integer operatorId);

	/**
	 * 使用的三方信息:<br>
	 * 盘口:金龙彩票,商号:亿汇付,金额:10000,id:248702|盘口:金龙彩票,商号:汇隆,金额:10000,id:248720"<br>
	 * 
	 * @param orderNo
	 * @param operatorId
	 * @return
	 */
	String usedThirdAccount(String orderNo, Integer operatorId, String fee);

	/**
	 * 全部出款完成的时候 调用 <br>
	 * 删除所有拆单缓存 TODO <br>
	 * 删除使用的第三方账号id和金额
	 * 
	 * @param orderNo
	 */
	void updateOrderFinal(String orderNo, Integer operatorId);

	/**
	 * 是否可以 点击完成 <br>
	 * 真正完成出款<br>
	 * 在点击完成出款的时候加校验 调用 TODO
	 * 
	 * @param orderNo
	 * @param userId
	 * @return
	 */
	boolean finalFinishCapable(String orderNo, Integer userId);

	/**
	 * 最后点击完成 还有尚未完成出款的金额
	 * 
	 * @param orderNo
	 * @return
	 */
	BigDecimal finalUnFinishedAmount(String orderNo, Integer userId);

	BigDecimal getSplitAmountBySubOrder(String orderNo,String subOrder, Integer userId);
}
