package com.xinbo.fundstransfer.domain.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import lombok.Data;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * 下发任务 输入参数
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class FindDrawTaskInputDTO implements Serializable {
	private Integer pageNo;
	private Integer pageSize;
	private String alias;
	private String account;
	private String bankType;
	private String bankName;
	private String owner;
	private Byte status;
	/**
	 * 全部页签 查询 锁定 1 未锁定 2
	 */
	private Byte locked;
	/**
	 * 当前人id
	 */
	private SysUser sysUser;
	/**
	 * 查询 页签 标识 1 全部 2 我锁定的
	 */
	private Byte pageFlag;
	/**
	 * 卡类型 出款卡 1 下发卡 2
	 */
	private Byte cardType;
	/**
	 * 在线的 下发卡
	 */
	private Collection onLines;
	/**
	 * 全部 各个子页签传 queryStatus 全部: 新的下发任务 1 锁定和下发中2 下发完成匹配(放在下发统计查询里 1 ) 已锁定: 已锁定1
	 * 下发中2 下发完成匹配的(放在下发统计查询里 1)
	 *
	 */
	private Byte queryStatus;
	/**
	 * 查询下发记录 完成 1 失败 2
	 */
	private Byte drawRecordStatus;
	/**
	 * 测试 1
	 */
	private Byte testFlag;
	/**
	 * 盘口id
	 */
	private Integer handicap;
	/**
	 * 创建时间 起始时间
	 */
	private String startTime;
	/**
	 * 创建时间 截止时间
	 */
	private String endTime;

	private List<Integer> allCardIds;

	private transient List<Integer> newDrawTask;
	private transient List<Integer> lockedOrUnfinishedDrawTask;
	private transient List<Integer> lockedByOneDrawTask;
	private transient List<Integer> unfinishedByOneDrawTask;

	private transient Boolean queryNewTaskInAll;
	private transient Boolean queryLockedAndDrawingInAll;
	private transient Boolean queryLockedByOne;
	private transient Boolean queryUnfinishedByOne;

	private transient Boolean queryByAlias = false;

	@Override
	public String toString() {
		return "FindDrawTaskInputDTO{" + "pageNo=" + pageNo + ", pageSize=" + pageSize + ", alias='" + alias + '\''
				+ ", account='" + account + '\'' + ", bankType='" + bankType + '\'' + ", bankName='" + bankName + '\''
				+ ", owner='" + owner + '\'' + ", status=" + status + ", locked=" + locked + ", sysUser=" + sysUser
				+ ", pageFlag=" + pageFlag + ", cardType=" + cardType + ", onLines=" + onLines + ", queryStatus="
				+ queryStatus + ", drawRecordStatus=" + drawRecordStatus + ", testFlag=" + testFlag + ", handicap="
				+ handicap + ", startTime='" + startTime + '\'' + ", endTime='" + endTime + '\'' + ", allCardIds="
				+ allCardIds + ", newDrawTask=" + newDrawTask + ", lockedOrUnfinishedDrawTask="
				+ lockedOrUnfinishedDrawTask + ", lockedByOneDrawTask=" + lockedByOneDrawTask
				+ ", unfinishedByOneDrawTask=" + unfinishedByOneDrawTask + ", queryNewTaskInAll=" + queryNewTaskInAll
				+ ", queryLockedAndDrawingInAll=" + queryLockedAndDrawingInAll + ", queryLockedByOne="
				+ queryLockedByOne + ", queryUnfinishedByOne=" + queryUnfinishedByOne + '}';
	}
}
