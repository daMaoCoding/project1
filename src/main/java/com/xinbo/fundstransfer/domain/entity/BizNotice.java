package com.xinbo.fundstransfer.domain.entity;


import java.util.Date;

import javax.persistence.*;

/**
 * 系统公告
 * @author Administrator
 *
 */
@Entity
@Table(name = "biz_notice")
public class BizNotice implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	private Integer id;
	private Integer type;//类型（1：自动出入款系统:2：返利网工具:3：PC工具）
	private Integer status;//状态（1正常   -1删除）
	private String publishNo;//版本号
	private String title;//标题
	private String contant;//内容
	private Date publishTime;//上线时间（版本上线时间）
	private Date updateTime;//最后更新时间（公告时间）
	private String operator;//最后更新人
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	
	@Column(name = "type")
	public Integer getType() {
		return type;
	}
	public void setType(Integer type) {
		this.type = type;
	}
	
	@Column(name = "status")
	public Integer getStatus() {
		return status;
	}
	public void setStatus(Integer status) {
		this.status = status;
	}

	@Column(name = "publish_no")
	public String getPublishNo() {
		return publishNo;
	}
	public void setPublishNo(String publishNo) {
		this.publishNo = publishNo;
	}

	@Column(name = "title")
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}

	@Column(name = "contant")
	public String getContant() {
		return contant;
	}
	public void setContant(String contant) {
		this.contant = contant;
	}

	@Column(name = "publish_time")
	public Date getPublishTime() {
		return publishTime;
	}
	public void setPublishTime(Date publishTime) {
		this.publishTime = publishTime;
	}

	@Column(name = "update_time")
	public Date getUpdateTime() {
		return updateTime;
	}
	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	@Column(name = "operator")
	public String getOperator() {
		return operator;
	}
	public void setOperator(String operator) {
		this.operator = operator;
	}
	
	
	
}
