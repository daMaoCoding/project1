package com.xinbo.fundstransfer.domain.entity;

import java.math.BigDecimal;

public class BizFeedBack implements java.io.Serializable {

	private int id;
	private String createTime;
	private String updateTime;
	private String level;
	private String status;
	private String issue;
	private String creator;
	private String acceptor;
	private String imgs;
	private String name;
	private String base64;

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @return the createTime
	 */
	public String getCreateTime() {
		return createTime;
	}

	/**
	 * @param createTime
	 *            the createTime to set
	 */
	public void setCreateTime(String createTime) {
		this.createTime = createTime;
	}

	/**
	 * @return the updateTime
	 */
	public String getUpdateTime() {
		return updateTime;
	}

	/**
	 * @param updateTime
	 *            the updateTime to set
	 */
	public void setUpdateTime(String updateTime) {
		this.updateTime = updateTime;
	}

	/**
	 * @return the issue
	 */
	public String getIssue() {
		return issue;
	}

	/**
	 * @param issue
	 *            the issue to set
	 */
	public void setIssue(String issue) {
		this.issue = issue;
	}

	/**
	 * @return the creator
	 */
	public String getCreator() {
		return creator;
	}

	/**
	 * @param creator
	 *            the creator to set
	 */
	public void setCreator(String creator) {
		this.creator = creator;
	}

	/**
	 * @return the acceptor
	 */
	public String getAcceptor() {
		return acceptor;
	}

	/**
	 * @param acceptor
	 *            the acceptor to set
	 */
	public void setAcceptor(String acceptor) {
		this.acceptor = acceptor;
	}

	/**
	 * @return the imgs
	 */
	public String getImgs() {
		return imgs;
	}

	/**
	 * @param imgs
	 *            the imgs to set
	 */
	public void setImgs(String imgs) {
		this.imgs = imgs;
	}

	/**
	 * @return the level
	 */
	public String getLevel() {
		return level;
	}

	/**
	 * @param level
	 *            the level to set
	 */
	public void setLevel(String level) {
		this.level = level;
	}

	/**
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * @param status
	 *            the status to set
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the base64
	 */
	public String getBase64() {
		return base64;
	}

	/**
	 * @param base64
	 *            the base64 to set
	 */
	public void setBase64(String base64) {
		this.base64 = base64;
	}

	public BizFeedBack(int id, String createTime, String updateTime, String level, String status, String issue,
			String creator, String acceptor, String imgs, String name, String base64) {
		super();
		this.id = id;
		this.createTime = createTime;
		this.updateTime = updateTime;
		this.level = level;
		this.status = status;
		this.issue = issue;
		this.creator = creator;
		this.acceptor = acceptor;
		this.imgs = imgs;
		this.name = name;
		this.base64 = base64;
	}

	public BizFeedBack() {
		super();
	}

}
