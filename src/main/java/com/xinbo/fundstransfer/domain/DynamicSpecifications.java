package com.xinbo.fundstransfer.domain;

import com.xinbo.fundstransfer.AppConstants;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;
import javax.servlet.ServletRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class DynamicSpecifications {
	private static final Logger logger = LoggerFactory.getLogger(DynamicSpecifications.class);

	private static final String SHORT_DATE = "yyyy-MM-dd";
	private static final String LONG_DATE = "yyyy-MM-dd HH:mm:ss";
	private static final String TIME = "HH:mm:ss";

	public static List<SearchFilter> build(ServletRequest request) {
		List<SearchFilter> result = new ArrayList<>();
		if (null != request) {
			result.addAll(SearchFilter.parse(request, AppConstants.SEARCH_PREFIX).values());
		}
		return result;
	}

	public static <T> Specification<T> build(ServletRequest request, final Class<T> entityClazz,
			final SearchFilter... searchFilters) {
		Set<SearchFilter> searchFilterSet = new HashSet<SearchFilter>();
		if (null != request) {
			Collection<SearchFilter> filters = SearchFilter.parse(request, AppConstants.SEARCH_PREFIX).values();
			searchFilterSet.addAll(filters);
		}

		for (SearchFilter searchFilter : searchFilters) {
			searchFilterSet.add(searchFilter);
		}
		if (null != searchFilters) {
			for (SearchFilter searchFilter : searchFilters) {
				if (null != searchFilter) {
					searchFilterSet.add(searchFilter);
				}
			}
		}
		return buildSpecification(entityClazz, searchFilterSet);
	}

	public static <T> Specification<T> build(final Class<T> entityClazz, final SearchFilter... searchFilters) {
		Set<SearchFilter> searchFilterSet = new HashSet<>();
		if (null != searchFilters) {
			for (SearchFilter searchFilter : searchFilters) {
				if (null != searchFilter) {
					searchFilterSet.add(searchFilter);
				}
			}
		}
		return buildSpecification(entityClazz, searchFilterSet);
	}

	public static <T> Specification<T> buildAndOr(SearchFilter[] searchFilters, SearchFilter[] searchFiltersOr) {
		Set<SearchFilter> searchFilterSet = new HashSet<>();
		Set<SearchFilter> searchFilterSetOr = new HashSet<>();
		if (null != searchFilters) {
			for (SearchFilter searchFilter : searchFilters) {
				if (null != searchFilter) {
					searchFilterSet.add(searchFilter);
				}
			}
		}
		if (null != searchFiltersOr) {
			for (SearchFilter searchFilter : searchFiltersOr) {
				if (null != searchFilter) {
					searchFilterSetOr.add(searchFilter);
				}
			}
		}
		return buildSpecificationAndOr(searchFilterSet, searchFilterSetOr);
	}

	private static <T> Specification<T> buildSpecificationAndOr(Set<SearchFilter> searchFilterSet,
			Set<SearchFilter> searchFilterSetOr) {
		return (root, query, builder) -> {
			Predicate[] predicateArray = buildPredicate(root, query, builder, searchFilterSet);
			Predicate[] predicateArrayOr = buildPredicate(root, query, builder, searchFilterSetOr);
			if (predicateArray != null && predicateArray.length > 0 && predicateArrayOr != null
					&& predicateArrayOr.length > 0) {
				// 第一部分条件 和 第二部分条件是 or 关系 但是每一部分直接的条件是and的关系
				Predicate and = builder.and(predicateArray);
				Predicate or = builder.and(predicateArrayOr);
				return builder.or(and, or);
			}
			return builder.conjunction();
		};
	}

	private static <T> Specification<T> buildSpecification(final Class<T> entityClazz,
			Set<SearchFilter> searchFilterSet) {
		return new Specification<T>() {
			@Override
			public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
				Predicate[] predicateArray = buildPredicate(root, query, builder, searchFilterSet);
				if (predicateArray != null && predicateArray.length > 0) {
					return builder.and(predicateArray);
				}
				return builder.conjunction();
			}
		};
	}

	private static <T> Specification<T> buildSpecificationForThirdDraw(final Class<T> entityClazz,
			Set<SearchFilter> searchFilterSet, Set<SearchFilter> orsearchFilterSet) {
		return new Specification<T>() {
			@Override
			public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
				Predicate[] predicateArray = buildPredicate(root, query, builder, searchFilterSet);
				Predicate[] orPredicateArray = buildPredicate(root, query, builder, orsearchFilterSet);
				boolean and = predicateArray != null && predicateArray.length > 0;
				boolean or = orPredicateArray != null && orPredicateArray.length > 0;
				if (or && and) {
					Predicate andPredicate = builder.and(predicateArray);
					Predicate orPrediccate = builder.or(orPredicateArray);
					return builder.and(andPredicate, orPrediccate);
				} else if (and) {
					return builder.and(predicateArray);
				}
				return builder.conjunction();
			}
		};
	}

	protected static <T> Predicate[] buildPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder,
			Set<SearchFilter> searchFilterSet) {
		List<Predicate> predicates = new ArrayList<Predicate>();
		if (searchFilterSet != null && !searchFilterSet.isEmpty()) {
			for (SearchFilter filter : searchFilterSet) {
				String[] names = StringUtils.split(filter.getFieldName(), ".");
				Path expression = root.get(names[0]);
				for (int i = 1; i < names.length; i++) {
					expression = expression.get(names[i]);
				}
				Class clazz = expression.getJavaType();
				if (Date.class.isAssignableFrom(clazz) && !filter.getValue().getClass().equals(clazz)) {
					filter.setValue(convert2Date((String) filter.getValue()));
				} else if (Enum.class.isAssignableFrom(clazz) && !filter.getValue().getClass().equals(clazz)) {
					filter.setValue(convert2Enum(clazz, (String) filter.getValue()));
				}
				switch (filter.getOperator()) {
				case EQ:
					predicates.add(builder.equal(expression, filter.getValue()));
					break;
				case ISNULL:
					predicates.add(builder.isNull(expression));
					break;
				case ISNOTNULL:
					predicates.add(builder.isNotNull(expression));
					break;
				case NOTEQ:
					predicates.add(builder.notEqual(expression, (Comparable) filter.getValue()));
					break;
				case LIKE:
					predicates.add(builder.like(expression, "%" + filter.getValue() + "%"));
					break;
				case GT:
					predicates.add(builder.greaterThan(expression, (Comparable) filter.getValue()));
					break;
				case LT:
					predicates.add(builder.lessThan(expression, (Comparable) filter.getValue()));
					break;
				case GTE:
					predicates.add(builder.greaterThanOrEqualTo(expression, (Comparable) filter.getValue()));
					break;
				case LTE:
					predicates.add(builder.lessThanOrEqualTo(expression, (Comparable) filter.getValue()));
					break;
				case IN:
					predicates.add(builder.and(expression.in((Object[]) filter.getValue())));
					break;
				case NOTIN:
					Object[] obj = null;
					try {
						obj = (Object[]) filter.getValue();
					} catch (Exception e) {
						e.printStackTrace();
					}
					if (null != obj) {
						predicates.add(builder.not(expression.in(obj)));
					}
					break;
				}
			}
		}
		return predicates.toArray(new Predicate[predicates.size()]);
	}

	protected static Date convert2Date(String dateString) {
		SimpleDateFormat sFormat = new SimpleDateFormat(LONG_DATE);
		try {
			return sFormat.parse(dateString);
		} catch (ParseException e) {
			try {
				sFormat = new SimpleDateFormat(SHORT_DATE);
				return sFormat.parse(dateString);
			} catch (ParseException e1) {
				try {
					sFormat = new SimpleDateFormat(TIME);
					return sFormat.parse(dateString);
				} catch (ParseException e2) {
					logger.error("Convert time is error! The dateString is " + dateString + "."
							+ ExceptionUtils.getMessage(e2));
				}
			}
		}

		return null;
	}

	protected static <E extends Enum<E>> E convert2Enum(Class<E> enumClass, String enumString) {
		return EnumUtils.getEnum(enumClass, enumString);
	}

}
