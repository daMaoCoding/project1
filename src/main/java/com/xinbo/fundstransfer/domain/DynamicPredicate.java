package com.xinbo.fundstransfer.domain;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

public class DynamicPredicate {

	public static <T> Predicate[] build(CriteriaBuilder builder, CriteriaQuery<Tuple> query, Root<T> root,
			final Class<T> entityClazz, final SearchFilter... searchFilters) {
		Set<SearchFilter> searchFilterSet = new HashSet<>();
		if (null != searchFilters) {
			for (SearchFilter searchFilter : searchFilters) {
				if (null != searchFilter) {
					searchFilterSet.add(searchFilter);
				}
			}
		}
		return buildPredicate(builder, query, root, entityClazz, searchFilterSet);
	}

	private static <T> Predicate[] buildPredicate(CriteriaBuilder builder, CriteriaQuery<Tuple> query, Root<T> root,
			final Class<T> entityClazz, Set<SearchFilter> searchFilterSet) {
		return DynamicSpecifications.buildPredicate(root, query, builder, searchFilterSet);
	}

}
