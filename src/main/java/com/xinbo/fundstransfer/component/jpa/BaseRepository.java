package com.xinbo.fundstransfer.component.jpa;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.util.List;

/**
 * ************************
 *
 * @author tony 3556239829
 */
@NoRepositoryBean
public interface BaseRepository<T,ID extends Serializable>  extends JpaRepository<T, ID> , JpaSpecificationExecutor<T> {
      boolean support(String modelType);

      T findById2(ID id);
      T findOne(ID id);
      void delete(ID id);
      T findOne2(@Nullable Specification<T> spec);
      <S extends T> List<S> save(Iterable<S> entities);
      void delete(Iterable<? extends T> entities);
      List<T> findAll(Iterable<ID> ids);
}
