package com.xinbo.fundstransfer.component.jpa;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.lang.Nullable;

import javax.persistence.EntityManager;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * ************************
 *
 * @author tony 3556239829
 */
public class BaseRepositoryImpl<T, ID extends Serializable>  extends SimpleJpaRepository<T,ID> implements BaseRepository<T, ID> {
    private final Class<T> domainClass;

    public BaseRepositoryImpl(Class<T> domainClass, EntityManager entityManager) {
        super(domainClass, entityManager);
        this.domainClass = domainClass;
    }

    @Override
    public boolean support(String modelType) {
        return domainClass.getName().equals(modelType);
    }

    @Override
    public  T findOne(ID id){
        return super.findById(id).orElse(null);
   }


   @Override
    public  T findById2(ID id){
        return super.findById(id).orElse(null);
    }


    @Override
    public void delete(ID id){
         super.deleteById(id);
    }

    @Override
    public T findOne2(@Nullable Specification<T> spec){
        return super.findOne(spec).orElse(null);
    }

    @Override
    public <S extends T> List<S> save(Iterable<S> entities) {
        return  super.saveAll(entities);
    }

    @Override
    public void delete(Iterable<? extends T> entities) {
        super.deleteAll(entities);
    }

    @Override
    public List<T> findAll(Iterable<ID> ids) {
        return super.findAllById(ids);
    }
}
