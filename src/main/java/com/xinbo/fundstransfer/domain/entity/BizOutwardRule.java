package com.xinbo.fundstransfer.domain.entity;

import java.util.Date;

import javax.persistence.*;

/**
 * BizAccount generated by hbm2java
 */
@Entity
@Table(name = "biz_outward_rule")
public class BizOutwardRule implements java.io.Serializable {
	private Integer id;

	private String name;

	private Integer type;

	private String data;

	private String status;

	private Date creteTime;

	public BizOutwardRule() {
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column(name = "name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column(name = "type")
	public Integer getType() {
		return type;
	}

	public void setType(Integer type) {
		this.type = type;
	}

	@Column(name = "data")
	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	@Column(name = "status")
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Column(name = "create_time")
	public Date getCreteTime() {
		return creteTime;
	}

	public void setCreteTime(Date creteTime) {
		this.creteTime = creteTime;
	}
}
