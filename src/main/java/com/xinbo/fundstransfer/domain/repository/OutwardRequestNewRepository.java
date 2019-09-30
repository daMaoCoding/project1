package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import com.xinbo.fundstransfer.domain.entity.NewOutWardEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 描述:  新公司入款
 * @author cobby
 * @create 2019-08-24 11:28
 */
public interface OutwardRequestNewRepository extends BaseRepository<NewOutWardEntity, Long> {

    /**
     * 新公司入款 - 用途类型管理 - 查询未被删除的用途类型
     * @return
     */
    @Query(nativeQuery = true, value = " SELECT * from biz_outusemanage_request where status = 0 order by create_time ")
    List<NewOutWardEntity> findAllByStatus();

    /**
     * 新公司入款 - 用途类型管理 - 查询名称除本条记录外的数据
     * @param id
     * @param useName
     * @return
     */
    @Query(nativeQuery = true, value = " SELECT * from biz_outusemanage_request where 1=1 and id !=:id  and  use_name=:useName ")
    NewOutWardEntity findByUseNameOrId(@Param("id") Long id, @Param("useName") String useName);

    /**
     * 新公司入款 - 用途类型管理 - 修改
     * @return
     */
    @Modifying
    @Query(nativeQuery = true, value = " update biz_outusemanage_request " +
            "set use_name=:useName, handel_time=now(), handel_name=:handelName where id=:id ")
    int updateByIdAndUseName(@Param("id") Long id, @Param("useName") String useName,
                             @Param("handelName") String handelName);

    /**
     * 新公司入款 - 用途类型管理 - 根据类型名称查询
     * @param useName
     * @return
     */
    @Query(nativeQuery = true, value = " SELECT * from biz_outusemanage_request where 1=1 and use_name=:useName ")
    List<NewOutWardEntity> findAllByUseName(@Param("useName")String useName);

    /**
     * 新公司入款 - 用途类型管理 - 逻辑删除(修改类型状态)
     */
    @Modifying
    @Query(nativeQuery = true, value = " update biz_outusemanage_request b " +
            "set b.status=:status, b.handel_time=now(), b.handel_name=:handelName where b.id=:id ")
    int updateForStatus(@Param("id")Long id, @Param("status")int status, @Param("handelName")String handelName);

}
