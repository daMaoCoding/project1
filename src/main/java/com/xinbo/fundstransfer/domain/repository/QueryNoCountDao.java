package com.xinbo.fundstransfer.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.io.Serializable;
import java.util.List;

@Repository
public class QueryNoCountDao {
	@PersistenceContext
	protected EntityManager em;

	public <T, ID extends Serializable> Page<T> findAll(Specification<T> spec, Pageable pageable, Class<T> clazz) {
		SimpleJpaNoCountRepository<T, ID> noCountDao = new SimpleJpaNoCountRepository<T, ID>(clazz, em);
		return noCountDao.findAll(spec, pageable);
	}

	public static class SimpleJpaNoCountRepository<T, ID extends Serializable> extends SimpleJpaRepository<T, ID> {

		public SimpleJpaNoCountRepository(Class<T> domainClass, EntityManager em) {
			super(domainClass, em);
		}

		@Override
		protected <S extends T> Page<S> readPage(TypedQuery<S> query, final Class<S> domainClass, Pageable pageable,
				final Specification<S> spec) {
			query.setFirstResult(Long.valueOf(pageable.getOffset()).intValue());
			//query.setFirstResult(pageable.getOffset());

			query.setMaxResults(pageable.getPageSize());
			List<S> content = query.getResultList();
			Page<S> page = PageableExecutionUtils.getPage(content, pageable, () -> {
				return content.size();// SimpleJpaRepository.executeCountQuery(SimpleJpaRepository.this.getCountQuery(spec,
										// domainClass)).longValue();
			});
			return page;
		}
	}
}
