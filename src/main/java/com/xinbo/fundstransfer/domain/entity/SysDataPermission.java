package com.xinbo.fundstransfer.domain.entity;
// Generated 2017-6-27 10:11:57 by Hibernate Tools 5.2.3.Final

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * SysDataPermission generated by hbm2java
 */
@Entity
@Table(name = "sys_data_permission")
public class SysDataPermission implements java.io.Serializable {

	private Long id;
	private String name;
	private String description;
	private String className;
	private String fieldName;// LEVELCODE 表示层级 HANDICAPCODE 表示盘口编码
	private String fieldValue;// 层级 id 或者 盘口id
	private String operator;
	private String nativeSql;
	private Integer userId;

	public SysDataPermission() {
	}

	public SysDataPermission(String name, String description, String className, String fieldName, String fieldValue,
			String operator, String nativeSql) {
		this.name = name;
		this.description = description;
		this.className = className;
		this.fieldName = fieldName;
		this.fieldValue = fieldValue;
		this.operator = operator;
		this.nativeSql = nativeSql;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "name", length = 45)
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column(name = "description", length = 200)
	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Column(name = "class_name", length = 100)
	public String getClassName() {
		return this.className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	@Column(name = "field_name", length = 45)
	public String getFieldName() {
		return this.fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	@Column(name = "field_value", length = 45)
	public String getFieldValue() {
		return this.fieldValue;
	}

	public void setFieldValue(String fieldValue) {
		this.fieldValue = fieldValue;
	}

	@Column(name = "operator", length = 45)
	public String getOperator() {
		return this.operator;
	}

	public void setOperator(String operator) {
		this.operator = operator;
	}

	@Column(name = "native_sql", length = 200)
	public String getNativeSql() {
		return this.nativeSql;
	}

	@Column(name = "user_id", length = 11, nullable = false)
	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public void setNativeSql(String nativeSql) {
		this.nativeSql = nativeSql;
	}

}