package com.xinbo.fundstransfer.domain;

import java.util.Map;

import org.springframework.data.domain.Page;

import com.xinbo.fundstransfer.AppConstants;
import org.springframework.data.domain.Pageable;

public class Paging {
	/**
	 * 每页显示条数
	 */
	private int pageSize;
	/**
	 * 当前页
	 */
	private int pageNo;
	/**
	 * 总页数
	 */
	private int totalPages;
	/**
	 * 总记录数
	 */
	private long totalElements;
	/**
	 * 前一页，分页下标从0开始
	 */
	private int previousPageNo;
	/**
	 * 后一页
	 */
	private int nextPageNo;

	/**
	 * 是否有下一页
	 */
	private boolean hasNext;
	/**
	 * 是否有上一页
	 */
	private boolean hasPrevious;
	/**
	 * 是否第一页
	 */
	private boolean isFirst;
	/**
	 * 是否最后一页
	 */
	private boolean isLast;

	private Map<String, Object> header;

	public Paging() {
		this.pageSize = AppConstants.PAGE_SIZE;
	}

	public Paging(int pageNo, int totalPages, long totalElements) {
		this.pageSize = AppConstants.PAGE_SIZE;
		this.pageNo = pageNo;
		this.totalPages = totalPages;
		this.totalElements = totalElements;
	}

	public Paging(int pageNo, int totalPages, long totalElements, Map<String, Object> header) {
		this.pageSize = AppConstants.PAGE_SIZE;
		this.pageNo = pageNo;
		this.totalPages = totalPages;
		this.totalElements = totalElements;
		this.header = header;
	}

	public Paging(Page page) {
		this.pageSize = page.getSize();
		if (page.hasContent()) {
			this.pageNo = page.getNumber() + 1;
		}
		this.totalPages = page.getTotalPages();
		this.totalElements = page.getTotalElements();
		if (null != page.previousPageable() && page.hasPrevious()) {
			Pageable pageable = page.previousPageable();
			this.previousPageNo = pageable.getPageNumber();
		}
		if (page.hasNext()) {
			this.nextPageNo = page.nextPageable().getPageNumber();
		} else {
			this.nextPageNo = page.getTotalPages() - 1;
		}
		this.hasNext = page.hasNext();
		this.hasPrevious = page.hasPrevious();
		this.isFirst = page.isFirst();
		this.isLast = page.isLast();
	}

	public Paging(Page page, Map<String, Object> header) {
		this.pageSize = page.getSize();
		if (page.hasContent()) {
			this.pageNo = page.getNumber() + 1;
		}
		this.totalPages = page.getTotalPages();
		this.totalElements = page.getTotalElements();
		if (null != page.previousPageable() && page.hasPrevious()) {
			this.previousPageNo = page.previousPageable().getPageNumber();
		}
		if (page.hasNext()) {
			this.nextPageNo = page.nextPageable().getPageNumber();
		} else {
			this.nextPageNo = page.getTotalPages() - 1;
		}
		this.hasNext = page.hasNext();
		this.hasPrevious = page.hasPrevious();
		this.isFirst = page.isFirst();
		this.isLast = page.isLast();
		this.header = header;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public int getPageNo() {
		return pageNo;
	}

	public void setPageNo(int pageNo) {
		this.pageNo = pageNo;
	}

	public int getTotalPages() {
		return totalPages;
	}

	public void setTotalPages(int totalPages) {
		this.totalPages = totalPages;
	}

	public long getTotalElements() {
		return totalElements;
	}

	public void setTotalElements(int totalElements) {
		this.totalElements = totalElements;
	}

	public int getPreviousPageNo() {
		return previousPageNo;
	}

	public void setPreviousPageNo(int previousPageNo) {
		this.previousPageNo = previousPageNo;
	}

	public int getNextPageNo() {
		return nextPageNo;
	}

	public void setNextPageNo(int nextPageNo) {
		this.nextPageNo = nextPageNo;
	}

	public boolean isHasNext() {
		return hasNext;
	}

	public void setHasNext(boolean hasNext) {
		this.hasNext = hasNext;
	}

	public boolean isHasPrevious() {
		return hasPrevious;
	}

	public void setHasPrevious(boolean hasPrevious) {
		this.hasPrevious = hasPrevious;
	}

	public boolean isFirst() {
		return isFirst;
	}

	public void setFirst(boolean isFirst) {
		this.isFirst = isFirst;
	}

	public boolean isLast() {
		return isLast;
	}

	public void setLast(boolean isLast) {
		this.isLast = isLast;
	}

	public void setTotalElements(long totalElements) {
		this.totalElements = totalElements;
	}

	public Map<String, Object> getHeader() {
		return header;
	}

	public void setHeader(Map<String, Object> header) {
		this.header = header;
	}
}
