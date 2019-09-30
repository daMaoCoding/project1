package com.xinbo.fundstransfer.domain.entity.activity;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.xinbo.fundstransfer.domain.enums.ActivityEnums;
import com.xinbo.fundstransfer.restful.v3.pojo.ReqV3RateItem;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 返利网活动同步
 */
@Data
@Slf4j
@Entity
@Table(name = "biz_flw_activity_syn")
@JsonIgnoreProperties(ignoreUnknown = true)
public class BizFlwActivitySyn implements java.io.Serializable {
	@Transient
	private String token;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Integer id;

	/**
	 * 活动状态(1正常，2停止/删除/取消)-无需考虑到时间的停止
	 */
	@Column(name = "activity_status", nullable = false)
	private int activityStatus;


	/**
	 * 是否可提现（不可提现0，可以提现1）
	 */
	@Column(name = "allow_withdrawal", nullable = false)
	private int allowWithdrawal;


	/**
	 * 允许佣金当作额度使用（不允许0，允许1）
	 */
	@Column(name = "allow_use_commissions", nullable = false)
	private int allowUseCommissions;

	/**
	 * 返利网活动名称
	 */
	@Column(name = "activity_name", nullable = false)
	private String activityName;

	/**
	 * 返利网活动编号，唯一
	 */
	@Column(name = "activity_number", nullable = false)
	private String activityNumber;

	/**
	 * 返利网活动开始时间
	 */
	//@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "activity_start_time", nullable = false)
	private Date activityStartTime;


	/**
	 * 返利网活动结束时间
	 */
	//@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "activity_end_time")
	private Date activityEndTime;


	/**
	 * 活动任务结束最终额度(活动目的-2万)
	 */
	@Column(name = "top_margin", nullable = false)
	private BigDecimal topMargin;


	/**
	 * 参与活动，活动赠送临时额度(1500)
	 */
	@Column(name = "tmp_margin", nullable = false)
	private BigDecimal tmpMargin;


	/**
	 * 赠送佣金规则 [{"amount":3000.0,"rate":0.1,"uplimit":-1.0},{"amount":10000.0,"rate":0.05,"uplimit":-1.0},{"amount":20000.0,"rate":0.0,"uplimit":-1.0}]
	 */
	@Column(name = "gift_margin_rule", nullable = false)
	private String giftMarginRule;


	/**
	 * 创建时间
	 */
	@Column(name = "create_time", nullable = false)
	private Date createTime;


	/**
	 * 最后更新时间
	 */
	@Column(name = "update_time", nullable = false)
	private Date updateTime;


	/**
	 * 加入活动限制：额度(-1不限制)
	 */
	@Column(name = "condition_margin", nullable = false)
	private BigDecimal conditionMargin;


	/**
	 * 加入活动限制：账号数量(-1不限制)
	 */
	@Column(name = "condition_accounts", nullable = false)
	private Integer conditionAccounts;



	/**
	 * 加入活动限制：是否开通云闪付(-1不限制，1需要开通)
	 */
	@Column(name = "condition_ysf", nullable = false)
	private Integer conditionYsf;



	/**
	 * 活动返利佣金计算规则
	 */
	public  List<ReqV3RateItem> getGiftMarginRuleObj(){
		try {
			if(StringUtils.isNotBlank(this.giftMarginRule))
				return JSON.parseObject(this.giftMarginRule, new TypeReference<List<ReqV3RateItem>>() {});
			return null;
		}catch (Exception e){
			return null;
		}
	}


	/**
	 * 活动状态枚举
	 */
	public ActivityEnums.ActivityStatus getActivityStatusEnum(){
		return ActivityEnums.ActivityStatus.getByNumber(this.activityStatus);
	}

	/**
	 * 是否可提现枚举
	 */
	public ActivityEnums.ActivityAllowWithdrawal getAllowWithdrawalEnum(){
		return ActivityEnums.ActivityAllowWithdrawal.getByNumber(this.allowWithdrawal);
	}


	/**
	 * 是否基本佣金自动提额
	 */
	public ActivityEnums.ActivityAllowUseCommissions getAllowUseCommissionsEnum(){
		return ActivityEnums.ActivityAllowUseCommissions.getByNumber(this.allowUseCommissions);
	}
}
