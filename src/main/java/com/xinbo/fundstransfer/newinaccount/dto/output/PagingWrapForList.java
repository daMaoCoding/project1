package com.xinbo.fundstransfer.newinaccount.dto.output;

import lombok.Data;
import org.apache.poi.ss.formula.functions.T;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 描述:根据lambda获取数据集合之后 实现分页返回
 */
@Data
public class PagingWrapForList implements Serializable {
	/**
	 * 总页数
	 */
	private int totalPage = 0;
	/**
	 * 总记录数
	 */
	private int totalRecord = 0;

	/**
	 * 当前是第几页
	 */
	private int curPageNo = 0;

	/**
	 * 每页的大小
	 */
	private int pageSize;

	/**
	 * 每页默认大小
	 */
	private static final int DEFAULT_PAGE_SIZE = 10;

	private List<? extends Object> pageData;

	public PagingWrapForList(List<?> pageResult, int pageSize) {
		this.pageSize = pageSize;
		this.pageData = pageResult;
		init(pageResult, pageSize);
	}

	public PagingWrapForList(List<?> pageResult) {
		this(pageResult, DEFAULT_PAGE_SIZE);
	}

	private void init(List<?> pageResult, int pageSize) {
		if (pageSize <= 0) {
			throw new IllegalArgumentException("Paging size must be greater than zero.");
		}
		if (null == pageResult) {
			throw new NullPointerException("Paging resource list must be not null.");
		}
		if (pageResult.size() % pageSize > 0) {
			this.totalPage = (pageResult.size() / pageSize) + 1;
		} else {
			this.totalPage = pageResult.size() / pageSize;
		}
		this.totalRecord = pageResult.size();
	}

	/**
	 * 返回当前剩余页数
	 *
	 * @return
	 */
	private int getSurplusPage() {
		if (pageData.size() % pageSize > 0) {
			return (pageData.size() / pageSize) + 1;
		} else {
			return pageData.size() / pageSize;
		}

	}

	/**
	 * 返回是否还有下一页数据
	 *
	 * @return
	 */
	public boolean hasNext() {
		return pageData.size() > 0;
	}

	/**
	 * 获取分页后，总的页数
	 *
	 * @return
	 */
	public int getTotalPage() {
		return totalPage;
	}

	public List<?> next() {
		List<?> pagingData = pageData.stream().limit(pageSize).collect(Collectors.toList());
		pageData = pageData.stream().skip(pageSize).collect(Collectors.toList());
		return pagingData;
	}

	/**
	 * 返回当前页数
	 *
	 * @return
	 */
	public int getCurPageNo() {
		return totalPage - getSurplusPage();
	}

}
