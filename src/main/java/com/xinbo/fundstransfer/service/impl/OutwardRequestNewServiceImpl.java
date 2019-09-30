package com.xinbo.fundstransfer.service.impl;

import com.alibaba.fastjson.JSON;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.accountfee.exception.NoSuiteAccountFeeRuleException;
import com.xinbo.fundstransfer.accountfee.pojo.AccountFeeCalResult;
import com.xinbo.fundstransfer.accountfee.service.AccountFeeService;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.domain.ResponseDataNewPay;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.pojo.*;
import com.xinbo.fundstransfer.domain.repository.*;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.IncomeRequestService;
import com.xinbo.fundstransfer.service.OutwardRequestNewService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SQLQuery;
import org.hibernate.transform.Transformers;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 描述:新公司出款
 * @author cobby
 * @create 2019年08月24日11:22
 */
@Service
@Slf4j
public class OutwardRequestNewServiceImpl implements OutwardRequestNewService {

    @Autowired
    private OutwardRequestNewRepository requestNewRepository;
    @Autowired
    private BizOutwardnewRequestRepository bizOutwardnewRequestRepository;
    @Autowired
    private BizCommonRemarkEntityRepository bizCommonRemarkEntityRepository;
    @Autowired
    private HandicapRepository handicapRepository;
    @Autowired
    private CommonRemarkServiceImpl remarkService;
    @Autowired
    @Lazy
    private AccountService accountService;
    @Autowired
    @Lazy
    private IncomeRequestService inReqSer;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AccountFeeService accountFeeService;
    @Autowired
    private BizUseMoneyTakeRepository bizUseMoneyTakeRepository;
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private IncomeRequestRepository incomeRequestRepository;

    @Override
    @Transactional
    public GeneralResponseData addOutUseManage(NewOutWardEntity entity){
        List<NewOutWardEntity> newOutWardEntities = requestNewRepository.findAllByUseName(entity.getUseName());
        if (CollectionUtils.isNotEmpty(newOutWardEntities)){
            return new GeneralResponseData<>(-1, "类型已存在,请重新输入类型名称!");
        }
        Date date = new Date();
        entity.setHandelId(entity.getCreateId());
        entity.setHandelName(entity.getCreateName());
        entity.setCreateTime(date);
        entity.setHandelTime(date);
        entity.setStatus(0);
        NewOutWardEntity save = requestNewRepository.save(entity);
        GeneralResponseData responseData = new GeneralResponseData();
        if (save != null ){
            responseData.setData(save);
            responseData.setStatus(1);
            responseData.setMessage("新增成功!");
        }else{
            responseData.setStatus(-1);
            responseData.setMessage("新增失败!");
        }
        return responseData;
    }

    @Override
    public NewOutWardEntity findOutUseManageById(Long id) {
        return requestNewRepository.findById2(id);
    }

    @Override
    public List<NewOutWardEntity> findOutUseManageAll() {
        return requestNewRepository.findAllByStatus();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GeneralResponseData<ResponseDataNewPay> modifyOutUseManage(NewOutWardTypeManageRequest request) {

        NewOutWardEntity entity1 = requestNewRepository.findByUseNameOrId(request.getId(),request.getUseName());
        if (entity1 != null ){
            return new GeneralResponseData<>(-1, "用途类型存在或已被删除,请重新命名!");
        }

        int mnt = requestNewRepository.updateByIdAndUseName(request.getId(),request.getUseName(),request.getHandelName());

        GeneralResponseData<ResponseDataNewPay> responseData;
        if (mnt == 1 ) {
            responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "修改成功");
        } else {
            // 失败时候msg不为null
            responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
                    "修改失败" );
            responseData.setMessage("修改失败");
        }
        return responseData;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GeneralResponseData<ResponseDataNewPay> deleteOutUseManage(NewOutWardTypeManageRequest request) {

        NewOutWardEntity entity1 = requestNewRepository.findById2(request.getId());
        if (entity1 == null ){
            return new GeneralResponseData<>(-1, "类型已不存在,请关闭窗口重新操作!");
        }
        if (entity1.getStatus() == 1 ){
            return new GeneralResponseData<>(-1, "类型已被删除,请刷新页面重新操作!");
        }

        int mnt = requestNewRepository.updateForStatus(request.getId(),1,request.getHandelName());
        GeneralResponseData<ResponseDataNewPay> responseData;

        if (mnt == 1) {
            responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "删除成功");
        } else {
            // 失败时候msg不为null
            responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
                    "删除失败" );
            responseData.setMessage("删除失败");
        }
        return responseData;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GeneralResponseData addOutWardNew(NewOutWardRequest request, SysUser sysUser) {

        request.setCreateTime(new Date());
        request.setMember(sysUser.getUid());
        request.setMemberCode(sysUser.getId());
        BizUsemoneyRequestEntity entity = new BizUsemoneyRequestEntity();
        BeanUtils.copyProperties(request,entity);
        //  根据 userId 或者用户权限
//        Integer userId = sysUser.getId();
//        SysUser oper = userService.findFromCacheById(userId);
//        if ( UserCategory.Finance.getCode() == sysUser.getCategory()) {  //
            entity.setStatus(1);  //  财务人员新增则直接审核通过进入下发人员审核状态
            entity.setFinanceReviewerName(sysUser.getUid());
            entity.setFinanceReviewerTime(new Date());
//        }else {
//            entity.setStatus(0);
//        }
        entity.setLockStatus(0);
        String code = format_yyyyMMddHHmmss(new Date())+randomStr(5); //编号 {yyyyMMddHHmmss}+{5位随机数字}
        entity.setCode("U"+code);
        BizUsemoneyRequestEntity save         = bizOutwardnewRequestRepository.save(entity);
        GeneralResponseData      responseData = new GeneralResponseData();
        if (save != null ){
            responseData.setData(save);
            responseData.setStatus(1);
            responseData.setMessage("新增成功!");
        }else{
            responseData.setStatus(-1);
            responseData.setMessage("新增失败!");
        }
        return responseData;

    }

    @Override
    public GeneralResponseData<List<BizUsemoneyRequestEntity>> findOutWardNewList(PageRequest pageRequest,
                                                                                  Specification<BizUsemoneyRequestEntity> specif, GeneralResponseData<List<BizUsemoneyRequestEntity>> responseData) {

        Page<BizUsemoneyRequestEntity> all        = bizOutwardnewRequestRepository.findAll(specif, pageRequest);
        List<BizUsemoneyRequestEntity> all1 = bizOutwardnewRequestRepository.findAll(specif);
        Double feeSum = all1.stream()
                .filter(w -> w.getFee() !=null)
                .mapToDouble(w -> w.getFee().doubleValue())
                .sum();
        Double amounSum = all1.stream()
                .filter(w -> w.getAmount() !=null)
                .mapToDouble(w -> w.getAmount().doubleValue())
                .sum();

        Map headerMap = new HashMap();
        headerMap.put("sumFee",feeSum);
        headerMap.put("sumAmount",amounSum);
        List<BizUsemoneyRequestEntity> entityList = all.getContent();
        if (org.springframework.util.CollectionUtils.isEmpty(entityList)) {  // 如果数据为空 返回空数据
            responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "无记录");
            responseData.setPage(new Paging());
            responseData.setData(null);
            return responseData;
        }

        List<NewOutWardEntity> newOutWardEntityList = requestNewRepository.findAll();
        Map<String ,String>  useMap = new HashMap<>();
        for (NewOutWardEntity wentityList : newOutWardEntityList){
            useMap.put(wentityList.getId().toString(),wentityList.getUseName());
        }

        List<BizHandicap> bizHandicaps = handicapRepository.findAll();
        Map<String ,String>  handicpMap = new HashMap<>();
        for (BizHandicap bizHandicap : bizHandicaps){
            handicpMap.put(bizHandicap.getId().toString(),bizHandicap.getName());
        }

        BigDecimal sumAmount = new BigDecimal(Double.toString(0));
        for (BizUsemoneyRequestEntity entity : entityList){
            entity.setUseName(useMap.get(entity.getUsetype().toString()));
            entity.setHandicapName(handicpMap.get(entity.getHandicap().toString()));
            if (StringUtils.isNotBlank(entity.getThirdCode())){  //  已绑定第三方 返回对应数据
                BizAccount bizAccount = accountRepository.findById2(Integer.valueOf(entity.getThirdCode()));
                if (bizAccount == null){
                    continue;
                }
                    entity.setBalance(bizAccount.getBalance());           // 第三方后台余额
                    entity.setBankBalance(bizAccount.getBankBalance());   // 第三方在系统内余额
                    entity.setThirdName(bizAccount.getBankName());

            }

            if (entity.getSentTime() == null  ){
                // 下发耗时 sentConsumingTime
                entity.setSentConsumingTime(entity.getCashTime()==null ? 0:new Date().getTime()-entity.getCashTime().getTime());
                // 下发总耗时  ConsumingTime
                entity.setConsumingTime(entity.getTaskReviewerTime() == null ? 0 :new Date().getTime()-entity.getTaskReviewerTime().getTime());
            }else {
                // 下发耗时 sentConsumingTime
                entity.setSentConsumingTime(entity.getSentTime().getTime()-entity.getLockTime().getTime());
                // 下发总耗时  ConsumingTime
                entity.setConsumingTime(entity.getSentTime().getTime()-entity.getTaskReviewerTime().getTime());
            }
            sumAmount = entity.getAmount().add(sumAmount);  // 查询当前页 下发金额小计
        }

        responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "获取列表数据成功");
        responseData.setData(entityList);
        Long count = all.getTotalElements();
        Paging page;
        Integer pageNo = pageRequest.getPageNumber();
        Integer pageSize = pageRequest.getPageSize();
        page = CommonUtils.getPage(pageNo + 1, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
                String.valueOf(count));

        page.setHeader(headerMap);
        responseData.setPage(page);
        return responseData;

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GeneralResponseData auditOutWardNew(NewOutWardRequest request, SysUser operator) {

        // 根据ID 查询当前数据
        BizUsemoneyRequestEntity entity = bizOutwardnewRequestRepository.findById2(request.getId());
        if (entity == null){
            return new GeneralResponseData(-1, "出款记录已不存在,请刷新页面重新操作!");
        }
        if (!entity.getStatus().equals(0)){
            return new GeneralResponseData(-1, "非审核状态,不能审核！");
        }
        GeneralResponseData responseData;
         int mnt = bizOutwardnewRequestRepository.auditOutWardNew(
                    request.getId(),request.getStatus()[0],request.getReview(),operator.getUid());

        if (mnt == 1 ){
            responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "财务审核成功!");
        }else{
            return new GeneralResponseData(-1, "财务审核失败！");

        }
        return responseData;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GeneralResponseData remark(NewOutWardRequest request, SysUser operator) {
        GeneralResponseData responseData;
        // 根据ID 查询当前数据
        BizUsemoneyRequestEntity entity = bizOutwardnewRequestRepository.findById2(request.getId());
        if (entity == null){
            return new GeneralResponseData(-1, "出款记录已不存在,请刷新页面重新操作!");
        }
        String remark = CommonUtils.genRemark(entity.getRemark(), request.getRemark(), new Date(), operator.getUid());
        int mnt = bizOutwardnewRequestRepository.genRemark(remark,request.getId());

        if (mnt == 1 ){
            responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "备注成功!");
        }else{
            // 失败时候msg不为null
            responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),"备注失败!" );
            responseData.setMessage("备注失败!");
        }
        return responseData;
    }

    @Override
    public BizUsemoneyRequestEntity findOutWardNewInfo(NewOutWardRequest newOutWardRequest) {
        BizUsemoneyRequestEntity entity           = bizOutwardnewRequestRepository.findById2(newOutWardRequest.getId());
        BizHandicap              bizHandicap      = handicapRepository.findById2(entity.getHandicap());
        NewOutWardEntity         newOutWardEntity = requestNewRepository.findById2(Long.valueOf(entity.getUsetype()));
        if (newOutWardEntity !=null){
            entity.setUseName(newOutWardEntity.getUseName());
        }
        if (bizHandicap!=null){
            entity.setHandicapName(bizHandicap.getName());
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GeneralResponseData auditOutWardNewBeSent(NewOutWardRequest request, SysUser operator) {

        // 根据ID 查询当前数据
        BizUsemoneyRequestEntity entity = bizOutwardnewRequestRepository.findById2(request.getId());
        if (entity == null){
            return new GeneralResponseData(-1, "出款记录已不存在,请刷新页面重新操作!");
        } // 0-待审核,1-财务审核通过,2-下发审核通过,3-财务审核不通过,4-下发审核不通过,5-等待到账 ,6-完成出款,7-确认对账
        if (!entity.getStatus().equals(1)){
            return new GeneralResponseData(-1, "财务还未审核或财务审核失败,不能下发！");
        }

        GeneralResponseData responseData;
        int mnt = bizOutwardnewRequestRepository.auditOutWardNewBeSent(
                request.getId(),request.getStatus()[0],request.getReview(),operator.getUid());

        if (entity.getStatus().equals(2)){
            addRemark(Integer.valueOf(request.getId().toString()),"审核公司用款通过",operator);
        }else if (entity.getStatus().equals(4)){
            addRemark(Integer.valueOf(request.getId().toString()),"审核公司用款不通过",operator);
        }
        if (mnt == 1 ){
            responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "下发审核成功!");
        }else{
            return new GeneralResponseData(-1, "下发审核失败！");
        }
        return responseData;
    }

    @Override
    public GeneralResponseData outWardBeSentLock(NewOutWardRequest request, SysUser operator) {

        // 根据ID 查询当前数据
        List<BizUsemoneyRequestEntity> entityList = bizOutwardnewRequestRepository.findAllIds(request.getIds());

        if (request.getIds().length != entityList.size()){
            return new GeneralResponseData(-1, "公司用款下发任务已不存在,请刷新页面重新操作!");
        }
        for (BizUsemoneyRequestEntity entity :entityList){
            if (!entity.getStatus().equals(2)){
                return new GeneralResponseData(-1, "编码："+entity.getCode()+"下发还未审核或审核失败,不能锁定！");
            }
            if (request.getLockStatus().equals(0) && entity.getLockStatus().equals(0)){
                return new GeneralResponseData(-1, "编码："+entity.getCode()+"已是解锁状态,请重新选择操作！");
            }
            if (request.getLockStatus().equals(0) && !entity.getLockName().equals(operator.getUid())){
                return new GeneralResponseData(-1, "编码："+entity.getCode()+"非本人锁定任务不能解锁！");
            }
        }

        GeneralResponseData responseData;

        if (request.getLockStatus() == 1) {
            // 下发人员锁定操作
            int mnt = bizOutwardnewRequestRepository.outWardBeSentLock(
                    request.getIds(),request.getLockStatus(),operator.getId(),operator.getUid());

            if (mnt == request.getIds().length ){
                responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "锁定成功!");
                for (Integer id :request.getIds()){
                    addRemark(id,"锁定公司用款任务",operator);
                }
            }else{
                // 失败时候msg不为null
                return new GeneralResponseData(-1, "锁定失败！");
            }
        }else{
            // 下发人员解锁操作 -- 只能 当前用户操作
            int mnt = bizOutwardnewRequestRepository.outWardBeSentCancelLock( request.getIds());
            if (mnt == request.getIds().length ){
                responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "解锁成功!");
                for (Integer id :request.getIds()){
                    addRemark(id,"解锁公司用款任务",operator);
                }
            }else{
                return new GeneralResponseData(-1, "锁定失败！");
            }
        }
        return responseData;

    }

    @Override
    public GeneralResponseData beSentThird(NewOutWardRequest request, SysUser operator) {

        // 根据ID 查询当前数据
        Integer[]                      ids                = request.getIds();
        List<BizUsemoneyRequestEntity> entityList         = bizOutwardnewRequestRepository.findAllIds(ids);
        BizAccount                     bizAccount = accountService.getById(Integer.valueOf(request.getThirdCode()));
        if (bizAccount == null ){
            return new GeneralResponseData(-1, "第三方资料获取异常,请重新选择第三方!");
        }
        if (request.getIds().length != entityList.size()){
            return new GeneralResponseData(-1, "公司用款下发任务已不存在,请刷新页面重新操作!");
        }
        for (BizUsemoneyRequestEntity entity :entityList){

            if (!entity.getStatus().equals(2)){
                return new GeneralResponseData(-1, "编号:"+entity.getCode()+"下发还未审核或审核失败,不能锁定！");
            }
            if (!entity.getLockStatus().equals(1)){
                return new GeneralResponseData(-1, "编号:"+entity.getCode()+"下发还未被锁定,不能绑定第三方下发！");
            }
            if ( !entity.getLockName().equals(operator.getUid())){
                return new GeneralResponseData(-1, "编号:"+entity.getCode()+"非本人锁定任务不能绑定三方出款！");
            }
        }
        GeneralResponseData responseData;
            // 下发人员 - 绑定第三方出款
        AccountFeeCalResult accountFeeCalResult = null;
        for (BizUsemoneyRequestEntity entity : entityList){
                addRemark(Integer.valueOf(entity.getId().toString()),"绑定第三方("+bizAccount.getBankName()+")成功",operator);
                try {
                    accountFeeCalResult = accountFeeService.calAccountFee(bizAccount, entity.getAmount());
                } catch (NoSuiteAccountFeeRuleException e) {
                    e.printStackTrace();
                    return new GeneralResponseData(-1,  e.getMessage());
                }
            }
        int mnt = bizOutwardnewRequestRepository.beSentThird(request.getIds(),request.getThirdCode(),accountFeeCalResult.getFee());
        if (mnt == request.getIds().length ){

                responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "绑定第三方("+bizAccount.getBankName()+")成功!");
            }else{
                // 失败时候msg不为null
                return new GeneralResponseData(-1, "绑定第三方("+bizAccount.getBankName()+")失败！");
            }
        return responseData;
    }

    @Override
    public GeneralResponseData takeMoneyThird(NewOutWardRequest request, SysUser operator) {

        // 根据ID 查询当前数据
        BizUsemoneyRequestEntity entity = bizOutwardnewRequestRepository.findById2(request.getId());
        if (entity == null){
            return new GeneralResponseData(-1, "公司用款下发任务已不存在,请刷新页面重新操作!");
        }
        if (StringUtils.isBlank(entity.getThirdCode())){
            return new GeneralResponseData(-1, "还未绑定第三方,请绑定后在操作!");
        }
        BizAccount thirdAccountEntity = accountService.getById(Integer.valueOf(entity.getThirdCode()));
        if (thirdAccountEntity == null ){
            return new GeneralResponseData(-1, "第三方资料获取异常,请重新选择第三方!");
        }
        if (!entity.getLockName().equals(operator.getUid())){
            return new GeneralResponseData(-1, "非本人锁定任务不能提现！");
        }
        if (entity.getStatus().equals(5)){
            return new GeneralResponseData(-1, "已提现,正在出款中,不能重复操作！");
        }
        GeneralResponseData responseData;
        // 下发人员 - 绑定第三方出款  5-等待到账 ,6-完成出款,7-确认对账
        int mnt = bizOutwardnewRequestRepository.takeMoneyThird(request.getId(),5);
        addRemark(Integer.valueOf(request.getId().toString()),"公司用款开始提现",operator);
        if (mnt == 1 ){

            /**
             * 保存公司用款记录
             * @param handicap    那个盘口的公司用款（无:则：为空）
             * @param th3Account  第三方账号
             * @param oppBankType 收款方银行类型
             * @param oppAccount  收款方银行账号
             * @param oppOwner    收款方姓名
             * @param amt         汇款金额 (大于零)
             * @param fee         汇款手续费(大于等于零)
             * @param operator    操作者 （not null）
             * @param remark      备注
             */
            BizHandicap bizHandicap = handicapRepository.findById2(entity.getHandicap());
            BizIncomeRequest bizIncomeRequest = inReqSer.registCompanyExpense(bizHandicap, thirdAccountEntity, entity.getToAccountBank()
                    , entity.getToAccount(), entity.getToAccountOwner(), entity.getAmount(), entity.getFee(), operator, "点击提现");
            if (bizIncomeRequest!= null){
                bizOutwardnewRequestRepository.updataBizIncomeRequest(bizIncomeRequest.getId(),request.getId());
            }else {
                return new GeneralResponseData(-1, "插入Income操作提现失败！");
            }
            responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作提现成功,等待出款中!");
        }else{
            // 失败时候msg不为null
            return new GeneralResponseData(-1, "操作提现失败！");
        }
        return responseData;
    }

    @Override
    @Transactional
    public GeneralResponseData thirdOutAccountFinish(NewOutWardRequest request, SysUser operator) {

        // 根据ID 查询当前数据
        BizUsemoneyRequestEntity entity = bizOutwardnewRequestRepository.findById2(request.getId());
        if (entity == null){
            return new GeneralResponseData(-1, "公司用款下发任务已不存在,请刷新页面重新操作!");
        }
        if ( !entity.getLockName().equals(operator.getUid())){
            return new GeneralResponseData(-1, "非本人锁定任务不能选择完成出款！");
        }
        GeneralResponseData responseData;
        // ,6-完成出款,7-确认对账
        int mnt = bizOutwardnewRequestRepository.thirdOutAccountFinish(request.getId(),6);
        addRemark(Integer.valueOf(request.getId().toString()),"",operator);
        BizAccount thirdAccountEntity = accountService.getById(Integer.valueOf(entity.getThirdCode()));
        if (thirdAccountEntity == null ){
            return new GeneralResponseData(-1, "第三方资料获取异常,请重新选择第三方!");
        }

        if (mnt == 1 ){
            createBizUseMoneyTakeEntity(thirdAccountEntity,entity,"点击下发成功 -- 第三方("+thirdAccountEntity.getBankName()+")",0);
            responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "完成出款成功!");
        }else{
            return new GeneralResponseData(-1, "点击完成出款失败！");
        }
        return responseData;
    }

    @Override
    @Transactional
    public GeneralResponseData thirdOutAccountFailing(NewOutWardRequest request, SysUser operator) {

        // 根据ID 查询当前数据
        BizUsemoneyRequestEntity entity = bizOutwardnewRequestRepository.findById2(request.getId());
        if (entity == null){
            return new GeneralResponseData(-1, "公司用款下发任务已不存在,请刷新页面重新操作!");
        }
        if ( !entity.getLockName().equals(operator.getUid())){
            return new GeneralResponseData(-1, "非本人锁定任务不能点击下发失败！");
        }
        if ( !entity.getStatus().equals(5)){
            return new GeneralResponseData(-1, "只有等待到账状态才可点击下发失败！");
        }
        GeneralResponseData responseData;

        // 公司用款财务 ( 下发人员 )确认失败
        inReqSer.rollBackCompanyExpense(entity.getBizIncomeId(),operator,"下发人员确认回滚");
        int mnt;
        BizAccount thirdAccountEntity = accountService.getById(Integer.valueOf(entity.getThirdCode()));
        if (thirdAccountEntity == null ){
            return new GeneralResponseData(-1, "第三方资料获取异常,请查看第三方当前状态或联系技术人员!");
        }
        if (request.getOutFailing() == 0 ){
            // 点击下发失败 回退到 1审核成功 2本人已锁定 3未绑定第三方状态
            mnt = bizOutwardnewRequestRepository.thirdOutAccountFailing(request.getId(),2,null);
            addRemark(Integer.valueOf(request.getId().toString()),"点击下发失败 -- 第三方("+thirdAccountEntity.getBankName()+")",operator);
            if (mnt == 1 ){
                createBizUseMoneyTakeEntity(thirdAccountEntity,entity,"点击下发失败 -- 第三方("+thirdAccountEntity.getBankName()+")",1);
                responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功!");
            }else{
                return new GeneralResponseData(-1, "点击下发失败异常！");
            }
        }else {
            if (StringUtils.isNotBlank(request.getReview())){
                return new GeneralResponseData(-1, "失败原因不能为空！");
            }
            // 点击下发失败 并标识失败不能恢复
            mnt = bizOutwardnewRequestRepository.thirdOutAccountFailing(
                    request.getId(),8 , "公司用款收款卡信息错误(操作人:"+operator.getUid()+")!");
            if (mnt == 1 ){
                createBizUseMoneyTakeEntity(thirdAccountEntity,entity,"点击失败并取消下发 -- 第三方("+thirdAccountEntity.getBankName()+")",2);
                responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "操作成功!");
            }else{
                return new GeneralResponseData(-1, "点击下发失败异常！");
            }

        }

        return responseData;
    }

    @Override
    public BizOutwardnewStatistics statistics(SysUser operator) {
        BizOutwardnewStatistics statistics = new BizOutwardnewStatistics();
        StringBuilder sql = new StringBuilder("select " +
                "count(if(status = 0, 1, null)) as status0 ,                                     "+  // 财务待审核
                "count(if(status = 1, 1, null)) as status1 ,                                     "+  // 下发待审核
                "count(if(status = 2 and lock_id is null, 1, null)) as status2 ,                 "+  // 审核通过未锁定
                "count(if(status in(2, 5) and lock_id is not null, 1, null)) as status3 ,        "+  // 锁定未下发完成
                "count(if(lock_id = "+operator.getId()+" and status = 2, 1, null)) as status4 ,  "+  // 我已锁定未下发完成
                "count(if(lock_id = "+operator.getId()+" and status = 5, 1, null)) as status5    "+  // 我已锁定绑定点击第三方提现正在出款中
                " from biz_usemoney_request");
        Query query = entityManager.createNativeQuery(sql.toString());
        query.unwrap(SQLQuery.class).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
        List rows = query.getResultList();
        for (Object obj :rows) {
            Map row = (Map) obj;
            statistics.setStatus0(Integer.valueOf(row.get("status0").toString()));
            statistics.setStatus1(Integer.valueOf(row.get("status1").toString()));
            statistics.setStatus2(Integer.valueOf(row.get("status2").toString()));
            statistics.setStatus3(Integer.valueOf(row.get("status3").toString()));
            statistics.setStatus4(Integer.valueOf(row.get("status4").toString()));
            statistics.setStatus5(Integer.valueOf(row.get("status5").toString()));
        }
        return statistics;
    }

    @Override
    public GeneralResponseData<List<BizUseMoneyTakeEntity>> findUsemoneyTakeList(PageRequest pageRequest, Specification<BizUseMoneyTakeEntity> specif, GeneralResponseData<List<BizUseMoneyTakeEntity>> responseData) {
        Page<BizUseMoneyTakeEntity> all = bizUseMoneyTakeRepository.findAll(specif, pageRequest);
        List<BizUseMoneyTakeEntity> bizUseMoneyTakeAll = bizUseMoneyTakeRepository.findAll(specif);

        Double feeSum = bizUseMoneyTakeAll.stream()
                .filter(w -> w.getFee() !=null)
                .mapToDouble(w -> w.getFee().doubleValue())
                .sum();
        Double amounSum = bizUseMoneyTakeAll.stream()
                .filter(w -> w.getAmount() !=null)
                .mapToDouble(w -> w.getAmount().doubleValue())
                .sum();

        Map headerMap = new HashMap();
        headerMap.put("sumFee",feeSum);
        headerMap.put("sumAmount",amounSum);

        List<BizUseMoneyTakeEntity> entityList = all.getContent();
        if (org.springframework.util.CollectionUtils.isEmpty(entityList)) {  // 如果数据为空 返回空数据
            responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "无记录");
            responseData.setPage(new Paging());
            responseData.setData(null);
            return responseData;
        }
//        List<NewOutWardEntity> newOutWardEntityList = requestNewRepository.findAll();
//        Map<String ,String>  useMap = new HashMap<>();
//        for (NewOutWardEntity wentityList : newOutWardEntityList){
//            useMap.put(wentityList.getId().toString(),wentityList.getUseName());
//        }

        List<BizHandicap> bizHandicaps = handicapRepository.findAll();
        Map<String ,String>  handicpMap = new HashMap<>();
        for (BizHandicap bizHandicap : bizHandicaps){
            handicpMap.put(bizHandicap.getId().toString(),bizHandicap.getName());
        }


        for (BizUseMoneyTakeEntity  entity : entityList){
            entity.setHandicapName(handicpMap.get(entity.getHandicap().toString()));
            if (StringUtils.isNotBlank(entity.getThirdCode())){  //  已绑定第三方 返回对应数据
                BizAccount bizAccount = accountRepository.findById2(Integer.valueOf(entity.getThirdCode()));
                if (bizAccount == null){
                    continue;
                }
                    entity.setThirdName(bizAccount.getBankName());
                if (entity.getBalance()==null){
                    entity.setBalance(bizAccount.getBalance());
                    entity.setBankBalance(bizAccount.getBankBalance());
                }

            }
            entity.setTimeConsuming(entity.getUpdateTime().getTime()/1000-entity.getCreateTime().getTime()/1000);   // 下发耗时
        }

        responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "获取列表数据成功");
        responseData.setData(entityList);
        Long count = all.getTotalElements();
        Paging page;
        Integer pageNo = pageRequest.getPageNumber();
        Integer pageSize = pageRequest.getPageSize();
        page = CommonUtils.getPage(pageNo + 1, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
                String.valueOf(count));

        page.setHeader(headerMap);
        responseData.setPage(page);
        return responseData;
    }

    @Override
    @Transactional
    public GeneralResponseData cfoInAccountFinish(NewOutWardRequest request, SysUser operator) {

        // 根据ID 查询当前数据
        BizUsemoneyRequestEntity entity = bizOutwardnewRequestRepository.findById2(request.getId());
        if (entity == null){
            return new GeneralResponseData(-1, "公司用款下发任务已不存在,请刷新页面重新操作!");
        }
        if (!entity.getStatus().equals(6)){
            return new GeneralResponseData(-1, "下发人员未完成出款,不能确认对账！");
        }
        GeneralResponseData responseData;
        // ,6-完成出款,7-确认对账
        int mnt = bizOutwardnewRequestRepository.thirdOutAccountFinish(request.getId(),7);
        inReqSer.commitCompanyExpense(entity.getBizIncomeId(),operator,"财务确认对账");  //  新增
        incomeRequestRepository.updateStatusById(entity.getBizIncomeId(),1);//修改BizInComeRequest
        addRemark(Integer.valueOf(request.getId().toString()),"财务确认对账",operator);
        BizAccount bizAccount = accountService.getById(Integer.valueOf(entity.getThirdCode()));
        if (bizAccount == null ){
            return new GeneralResponseData(-1, "第三方资料获取异常,请查看第三方当前状态或联系技术人员!");
        }
        bizUseMoneyTakeRepository.updateThirdBankBalance(bizAccount.getBankBalance(),bizAccount.getBalance(),entity.getCode());
        if (mnt == 1 ){
            responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "已确认对账!");
        }else{
            return new GeneralResponseData(-1, "点击确认对账失败！");
        }
        return responseData;
    }

    /** 新增备注 */
    public  BizCommonRemarkOutputDTO addRemark(Integer id ,String info, SysUser sysUser) {
        BizCommonRemarkInputDTO inputDDTO = new BizCommonRemarkInputDTO();
        inputDDTO.setBusinessId(id);
        inputDDTO.setRemark(info);
        inputDDTO.setType("BizUseMoneyRequestData");
        inputDDTO.setStatus((byte) 1);
        inputDDTO.setSysUser(sysUser);
        return remarkService.add(inputDDTO);
    }

    /** 插入BizUseMoneyTake表  */
    public  BizUseMoneyTakeEntity createBizUseMoneyTakeEntity(
            BizAccount thirdAccountEntity,BizUsemoneyRequestEntity rEntity , String remark , Integer status) {
//        AccountFeeCalResult accountFeeCalResult = null;
//        try {
//            accountFeeCalResult = accountFeeService.calAccountFee(bizAccount, rEntity.getAmount());
//        } catch (NoSuiteAccountFeeRuleException e) {
//            e.printStackTrace();
//        }
        BizUseMoneyTakeEntity  entity = new BizUseMoneyTakeEntity();
        entity.setFee(rEntity.getFee());// 计算第三方手续费
        entity.setAmount(rEntity.getAmount());
        entity.setCode(rEntity.getCode());
        entity.setUpdateTime(new Date());            //  下发耗时/总耗时的结束时间
        entity.setHandicap(rEntity.getHandicap());
        entity.setRemark(remark);
        entity.setThirdCode(rEntity.getThirdCode());
        entity.setStatus(status);
        entity.setToAccount(rEntity.getToAccount());
        entity.setToAccountBank(rEntity.getToAccountBank());
        entity.setToAccountOwner(rEntity.getToAccountOwner());
        entity.setCreateTime(rEntity.getCashTime());  //  下发开始时间 （点击提现时间 - 每次点击提现会重置）
        entity.setCreateTimeTotal(rEntity.getTaskReviewerTime());        //  总耗时开始时间 （下发人员审核完成时间 ）
        entity.setBalance(thirdAccountEntity.getBalance());
        entity.setBankBalance(thirdAccountEntity.getBankBalance());
        entity.setLockerName(rEntity.getLockName());
        entity.setConsumingTime(entity.getUpdateTime().getTime()/1000-entity.getCreateTimeTotal().getTime()/1000);  //  总耗时
        return bizUseMoneyTakeRepository.save(entity);
    }

    /** 格式化时间 */
    public static String format_yyyyMMddHHmmss(Date date) {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(date);
    }

    /** 生成随机字符 */
    public static String randomStr(int num) {
        char[] randomMetaData = new char[] {  '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };
        Random random = new Random();
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < num; i++) {
            buf.append(randomMetaData[random.nextInt(randomMetaData.length - 1)]);
        }
        return buf.toString();
    }
}
