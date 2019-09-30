package com.xinbo.fundstransfer.domain.entity;


import javax.persistence.*;


@Entity
@Table(name = "biz_host")
public class BizHost implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	private Integer id;
	private String ip;
	private String x;
	private String y;
	private String name;
	private String operator;
	private Integer hostNum;
	private String hostInfo;//每个主机后必须跟一个英文逗号 如: 192.192.168.2,192.192.168.3,
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	@Column(name = "ip", length = 45)
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	@Column(name = "x", length = 45)
	public String getX() {
		return x;
	}
	public void setX(String x) {
		this.x = x;
	}
	@Column(name = "y", length = 45)
	public String getY() {
		return y;
	}
	public void setY(String y) {
		this.y = y;
	}
	@Column(name = "name", length = 45)
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	@Column(name = "operator", length = 45)
	public String getOperator() {
		return operator;
	}
	public void setOperator(String operator) {
		this.operator = operator;
	}
	@Column(name = "host_num")
	public Integer getHostNum() {
		return hostNum;
	}
	public void setHostNum(Integer hostNum) {
		this.hostNum = hostNum;
	}
	@Column(name = "host_info", length = 45)
	public String getHostInfo() {
		return hostInfo;
	}
	public void setHostInfo(String hostInfo) {
		this.hostInfo = hostInfo;
	}
	
	
	
}
