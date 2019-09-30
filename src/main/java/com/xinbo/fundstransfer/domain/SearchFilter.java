package com.xinbo.fundstransfer.domain;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.servlet.ServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * 动态查询对象
 */
public class SearchFilter {

	/**
	 * SQL查询逻辑 ：EQ-equal等于；LIKE-like两端模糊查询；GT-greaterThan大于；LT-lessThan小于；GTE-
	 * greaterThanOrEqualTo大于或等于；LTE-lessThanOrEqualTo小于或等于；IN-in查询
	 * 
	 */
	public enum Operator {
		EQ, LIKE, GT, LT, GTE, LTE, IN, NOTEQ, NOTIN, ISNULL, ISNOTNULL
	}

	private String fieldName;
	private Object value;
	private Operator operator;

	public SearchFilter(String fieldName, Operator operator, Object value) {
		this.fieldName = fieldName;
		this.value = value;
		this.operator = operator;
	}

	/**
	 * 构建查询条件，从request请求中解析
	 */
	public static Map<String, SearchFilter> parse(ServletRequest request, String prefix) {
		Map<String, Object> searchParams = getParametersStartingWith(request, prefix);
		Map<String, SearchFilter> filters = new HashMap<String, SearchFilter>();

		for (Entry<String, Object> entry : searchParams.entrySet()) {
			// 过滤掉空值
			String key = entry.getKey();
			Object value = entry.getValue();
			if (StringUtils.isBlank((String) value)) {
				continue;
			}
			// 拆分operator与filedAttribute
			String[] names = StringUtils.split(key, "_");
			if (names.length < 2) {
				throw new IllegalArgumentException(key + " is not a valid search filter name");
			}

			String filedName = names[1];
			// 判断参数是否大于2，即该entity的查询条件包含外键列，例如：search_EQ_user_username，Entity
			// Task与用户表关联，页面封装的条件都以"_"分隔，需要特殊处理，此操作目前仅为了在页面输入查询条件在结果页把条件值显示出来
			if (names.length > 2) {
				for (int i = 2; i < names.length; i++) {
					filedName += "." + names[i];
				}
			}
			Operator operator = Operator.valueOf(names[0]);
			if (operator.equals(Operator.IN)) {
				value = ((String) value).split(",");
			}

			// 创建searchFilter
			SearchFilter filter = new SearchFilter(filedName, operator, value);
			filters.put(key, filter);
		}

		return filters;
	}

	/**
	 * 取得带相同前缀的Request Parameters
	 */
	@SuppressWarnings("rawtypes")
	public static Map<String, Object> getParametersStartingWith(ServletRequest request, String prefix) {
		Validate.notNull(request, "Request search parameters must not be null");
		Enumeration paramNames = request.getParameterNames();
		Map<String, Object> params = new TreeMap<String, Object>();
		if (prefix == null) {
			prefix = "";
		}
		while ((paramNames != null) && paramNames.hasMoreElements()) {
			String paramName = (String) paramNames.nextElement();
			if ("".equals(prefix) || paramName.startsWith(prefix)) {
				String unprefixed = paramName.substring(prefix.length());
				String[] values = request.getParameterValues(paramName);
				if ((values == null) || (values.length == 0)) {
					// Do nothing, no values found at all.
				} else if (values.length > 1) {
					params.put(unprefixed, values);
				} else {
					params.put(unprefixed, values[0]);
				}
			}
		}
		return params;
	}

	/**
	 * @return the fieldName
	 */
	public String getFieldName() {
		return fieldName;
	}

	/**
	 * @param fieldName
	 *            the fieldName to set
	 */
	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	/**
	 * @return the value
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * @param value
	 *            the value to set
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * @return the operator
	 */
	public Operator getOperator() {
		return operator;
	}

	/**
	 * @param operator
	 *            the operator to set
	 */
	public void setOperator(Operator operator) {
		this.operator = operator;
	}
}