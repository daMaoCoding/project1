package com.xinbo.fundstransfer.domain.entity.activity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.xinbo.fundstransfer.domain.enums.ActivityEnums;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 返利网活动/兼职参与关联
 */
@Entity
@Slf4j
@Data
@Table(name = "biz_account_flw_activity")
@JsonIgnoreProperties(ignoreUnknown = true)
public class BizAccountFlwActivity implements java.io.Serializable {

	@Transient
	private String token;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Integer id;

	/**
	 * 兼职账号id
	 */
	@Column(name = "uid", nullable = false)
	private String uid;

	/**
	 * 活动编号
	 */
	@Column(name = "activity_number", nullable = false)
	private String activityNumber;

	/**
	 * 返利网活动同步表的活动id
	 */
	@Column(name = "activity_id", nullable = false)
	private int activityId;

	/**
	 * 返利网兼职参与时间
	 */
	@Column(name = "user_start_time", nullable = false)
	private Date userStartTime;

	/**
	 * 返利网兼职结束时间/空表示和活动结束时间一致
	 */
	@Column(name = "user_end_time", nullable = false)
	private Date userEndTime;

	/**
	 * 用户活动状态(0等待，1进行中，2完成任务结束，3时间结束，4退出,5系统活动取消）
	 */
	@Column(name = "user_status", nullable = false)
	private int userStatus;

	/**
	 * 活动额外赠送的累计佣金(退出活动不清零)
	 */
	@Column(name = "activity_amount", nullable = false)
	private BigDecimal activityAmount;


	/**
	 * 加入活动时返利网活动赠送的临时额度
	 */
	@Column(name = "activity_tmp_margin", nullable = false)
	private BigDecimal activityTmpMargin;


	/**
	 * 获取用户参与活动状态
	 */
	public ActivityEnums.UserActivityStatus getUserStatusEnum(){
		return ActivityEnums.UserActivityStatus.getByNumber(this.userStatus);
	}

}
