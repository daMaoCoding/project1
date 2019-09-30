package com.xinbo.fundstransfer.service;

import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.ResponseDataNewPay;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.pojo.NewOutWardRequest;
import com.xinbo.fundstransfer.domain.pojo.NewOutWardTypeManageRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * 新公司出款
 */
public interface OutwardRequestNewService {

    /**
     * 新公司出款 - 用途类型管理 - 新增
     * @param newOutWardEntity
     * @return
     */
    GeneralResponseData addOutUseManage(NewOutWardEntity newOutWardEntity);

    /**
     * 新公司出款 - 用途类型管理 - 查询(根据ID)
     * @param id
     * @return
     */
    NewOutWardEntity findOutUseManageById(Long id);

    /**
     * 新公司出款 - 用途类型管理 - 查询全部
     * @return
     */
    List<NewOutWardEntity> findOutUseManageAll();

    /**
     * 新公司出款 - 用途类型管理 - 修改
     * @param request
     * @return
     */
    GeneralResponseData<ResponseDataNewPay> modifyOutUseManage(NewOutWardTypeManageRequest request);

    /**
     * 新公司出款 - 用途类型管理 - 逻辑删除
     * @param request
     * @return
     */
    GeneralResponseData<ResponseDataNewPay> deleteOutUseManage(NewOutWardTypeManageRequest request);

    /**
     * 新公司出款 -  新增公司出款
     * @param request
     * @param operator
     * @return
     */
    GeneralResponseData addOutWardNew(NewOutWardRequest request, SysUser operator);

    /**
     * 新公司出款 -  查询公司出款List
     * @param pageRequest
     * @param specif
     * @return
     */
    GeneralResponseData<List<BizUsemoneyRequestEntity>> findOutWardNewList(PageRequest pageRequest, Specification<BizUsemoneyRequestEntity> specif, GeneralResponseData<List<BizUsemoneyRequestEntity>> responseData);

    /**
     * 新公司出款 -  财务审核
     * @param request
     * @param operator
     * @return
     */
    GeneralResponseData auditOutWardNew(NewOutWardRequest request, SysUser operator);

    /**
     * 新公司出款 -  添加备注信息
     * @param request
     * @param operator
     * @return
     */
    GeneralResponseData remark(NewOutWardRequest request, SysUser operator);

    /**
     * 新公司出款 -  出款详情
     * @param newOutWardRequest
     * @return
     */
    BizUsemoneyRequestEntity findOutWardNewInfo(NewOutWardRequest newOutWardRequest);

    /**
     * 新公司出款 -  下发审核
     * @param request
     * @param operator
     * @return
     */
    GeneralResponseData auditOutWardNewBeSent(NewOutWardRequest request, SysUser operator);

    /**
     * 新公司出款 -  下发锁定
     * @param request
     * @param operator
     * @return
     */
    GeneralResponseData outWardBeSentLock(NewOutWardRequest request, SysUser operator);

    /**
     * 新公司出款 -  绑定第三方
     * @param request
     * @param operator
     * @return
     */
    GeneralResponseData beSentThird(NewOutWardRequest request, SysUser operator);

    /**
     * 新公司用款 -  绑定第三方提现操作
     * @param request
     * @param operator
     * @return
     */
    GeneralResponseData takeMoneyThird(NewOutWardRequest request, SysUser operator);

	/**
	 * 新公司出款 -  第三方下发完成
	 * @param request
	 * @param operator
	 * @return
	 */
	GeneralResponseData thirdOutAccountFinish(NewOutWardRequest request, SysUser operator);

	/**
	 * 新公司用款 -  财务对账完成
	 * @param request
	 * @param operator
	 * @return
	 */
	GeneralResponseData cfoInAccountFinish(NewOutWardRequest request, SysUser operator);

    /**
     *新公司用款 -  第三方下发失败
     * @param request
     * @param operator
     * @return
     */
    GeneralResponseData thirdOutAccountFailing(NewOutWardRequest request, SysUser operator);

    /**
     * 新公司出款 - 统计
     * @param operator
     * @return
     */
    BizOutwardnewStatistics statistics(SysUser operator);

    /**
     * 新公司用款 -  成功失败查询
     * @param pageRequest
     * @param specif
     * @param responseData
     * @return
     */
    GeneralResponseData<List<BizUseMoneyTakeEntity>> findUsemoneyTakeList(PageRequest pageRequest, Specification<BizUseMoneyTakeEntity> specif, GeneralResponseData<List<BizUseMoneyTakeEntity>> responseData);
}
