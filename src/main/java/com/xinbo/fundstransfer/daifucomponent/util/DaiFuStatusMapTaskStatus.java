package com.xinbo.fundstransfer.daifucomponent.util;

import com.google.common.base.Preconditions;
import com.xinbo.fundstransfer.daifucomponent.dto.output.DaifuResult;

/**
 * 代付状态 {@link DaifuResult#getResult()} 映射到
 * {@link com.xinbo.fundstransfer.domain.enums.OutwardTaskStatus}
 */
public class DaiFuStatusMapTaskStatus {
	public enum DaiFuResultStatusEnum {
		DEAL_DAIFU_OLD_METHOD_0(0, "不能代付按原流程处理状态"), DEAL_DAIFU_TODO_1(1, "代付但未出款"), DEAL_DAIFU_DOING_2(2,
				"代付且正在出款"), DEAL_DAIFU_SUCCESS_3(3,
						"代付成功转完成"), DEAL_DAIFU_FAILURE_4(4, "代付失败转排查"), DEAL_DAIFU_ORDDER_NOT_EXISTS_5(5, "代付订单不存在不处理");
		private Integer daifuStatus;
		private String daifuStatusDesc;

		DaiFuResultStatusEnum(Integer daifuStatus, String daifuStatusDesc) {
			this.daifuStatus = daifuStatus;
			this.daifuStatusDesc = daifuStatusDesc;
		}

		public Integer getDaifuStatus() {
			return daifuStatus;
		}

		public String getDaifuStatusDesc() {
			return daifuStatusDesc;
		}
	}

	public static final String getDaifuStausMapDesc(Integer status) {
		Preconditions.checkNotNull(status);
		for (DaiFuResultStatusEnum daiFuResultStatusEnum : DaiFuResultStatusEnum.values()) {
			if (daiFuResultStatusEnum.getDaifuStatus().compareTo(status) == 0) {
				return daiFuResultStatusEnum.getDaifuStatusDesc();
			}
		}
		return null;
	}

	public static final Integer getTaskStatusFromDaifuStatus(Byte daifuStatus) {
		if (daifuStatus == null) {
			return null;
		}
		switch (daifuStatus) {
		case 2:
		case -127:
		case -125:
		case -124:
			// 2代付取消或其他情况 按原流程出款 0
			return DaiFuResultStatusEnum.DEAL_DAIFU_OLD_METHOD_0.daifuStatus;
		case 0:
			// 0 代付初始化状态 未知
			return DaiFuResultStatusEnum.DEAL_DAIFU_TODO_1.daifuStatus;
		case 3:
			// 正在代付转正在出款
			return DaiFuResultStatusEnum.DEAL_DAIFU_DOING_2.daifuStatus;
		case 1:
			// 代付成功转待完成 3
			return DaiFuResultStatusEnum.DEAL_DAIFU_SUCCESS_3.daifuStatus;
		case 4:
			// 转待排查 4
			return DaiFuResultStatusEnum.DEAL_DAIFU_FAILURE_4.daifuStatus;
		case -126:
			// 订单不存在 5 不处理
			return DaiFuResultStatusEnum.DEAL_DAIFU_ORDDER_NOT_EXISTS_5.daifuStatus;
		default:
			return null;
		}
	}

}
