package com.xinbo.fundstransfer.restful;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.domain.DynamicSpecifications;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.ResponseDataNewPay;
import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.entity.*;
import com.xinbo.fundstransfer.domain.pojo.NewOutWardRequest;
import com.xinbo.fundstransfer.domain.pojo.NewOutWardTypeManageRequest;
import com.xinbo.fundstransfer.domain.pojo.UseMoneyTakeRequest;
import com.xinbo.fundstransfer.service.CommonRemarkService;
import com.xinbo.fundstransfer.service.OutwardRequestNewService;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

@RestController
@RequestMapping(value = "/r/outNew")
public class OutwardRequestNewController extends BaseController {


    private static final Logger logger = LoggerFactory.getLogger(OutwardRequestNewController.class);

    private ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private OutwardRequestNewService owrnService;
    @Autowired
    private CommonRemarkService remarkService;

    /**
     * 新公司用款 -  新增公司出款 biz_usemoney_request
     * @param request
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/addOutWardNew", method = RequestMethod.POST, consumes = "application/json")
    public GeneralResponseData addOutWardNew(@Validated @RequestBody NewOutWardRequest request, Errors errors) throws Exception{
        if (errors.hasErrors()) {  // Errors  可以通过 @Validated 和 @NULL 校验参数
            return new GeneralResponseData(-1, "参数校验不通过!");
        }
        SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();  // 获取登陆人信息
        logger.debug("新公司用款 - 新增公司出款 参数:{}", request.toString());
        GeneralResponseData data = owrnService.addOutWardNew(request, operator);
        logger.debug("新公司用款 - 新增公司出款成功 返回参数:{}", data.toString());
        return data;
    }

    /**
     * 新公司用款 -  查询公司出款List
     * @param newOutWardRequest
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/findOutWardNewList", method = RequestMethod.POST, consumes = "application/json")
    public String  findOutWardNewList( @RequestBody NewOutWardRequest newOutWardRequest) throws Exception{
        String params = buildParams().toString();
        logger.trace(String.format("%s，参数：%s", "账号分页获取", params));
        try {
            GeneralResponseData<List<BizUsemoneyRequestEntity>> responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue());
            // 判断登陆是否失效
            SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
            if (sysUser == null) {
                responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆");
                return mapper.writeValueAsString(responseData);
            }
            //  分页条件    [,Sort.Direction.DESC, "updateTime"] - 根据字段排序
            String sort = "createTime";
            PageRequest pageRequest = null;
            if ("all_2".equals(newOutWardRequest.getFlag())){
                sort="status";
                pageRequest = new PageRequest(
                        newOutWardRequest.getPageNo(), newOutWardRequest.getPageSize() != null ?  newOutWardRequest.getPageSize() : AppConstants.PAGE_SIZE
                        , Sort.Direction.ASC, sort);
            }else {
                pageRequest = new PageRequest(
                        newOutWardRequest.getPageNo(), newOutWardRequest.getPageSize() != null ?  newOutWardRequest.getPageSize() : AppConstants.PAGE_SIZE
                        , Sort.Direction.DESC, sort);
            }


            // 封装查询条件
            List<SearchFilter> filterToList = queryConditions(newOutWardRequest,sysUser);
            SearchFilter[] filterToArray = filterToList.toArray(new SearchFilter[filterToList.size()]);

            // 返回参数构建
            Specification<BizUsemoneyRequestEntity> specif = DynamicSpecifications.build(BizUsemoneyRequestEntity.class, filterToArray);

            // 列表查询
            responseData = owrnService.findOutWardNewList(pageRequest ,specif ,responseData);

            // 参数返回
            return mapper.writeValueAsString(responseData);

        } catch (Exception e) {
            e.printStackTrace();
            logger.error(String.format("%s，参数：%s，结果：%s", "账号分页获取", params, e.getMessage()));
            return mapper.writeValueAsString(new GeneralResponseData<>(
                    GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
        }
    }


    /**
     * 新公司用款 -  出款详情
     * @param newOutWardRequest
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/findOutWardNewInfo", method = RequestMethod.POST, consumes = "application/json")
    public String  findOutWardNewInfo( @RequestBody NewOutWardRequest newOutWardRequest) throws Exception{
        String params = buildParams().toString();
        try {
            GeneralResponseData<BizUsemoneyRequestEntity> responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue());
            if (newOutWardRequest.getId()==null){
                responseData =  new GeneralResponseData(-1, "ID不能为空!");
                return mapper.writeValueAsString(responseData);
            }
            // 判断登陆是否失效
            SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
            if (sysUser == null) {
                responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆");
                return mapper.writeValueAsString(responseData);
            }
            // 出款详情
            BizUsemoneyRequestEntity entity = owrnService.findOutWardNewInfo(newOutWardRequest);
            responseData.setData(entity);
            // 参数返回
            return mapper.writeValueAsString(responseData);

        } catch (Exception e) {
            e.printStackTrace();
            logger.error(String.format("%s，参数：%s，结果：%s", "出款详情获取", params, e.getMessage()));
            return mapper.writeValueAsString(new GeneralResponseData<>(
                    GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
        }
    }


    /**
     * 新公司用款 -  财务审核
     * @param request
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/auditOutWardNew", method = RequestMethod.POST, consumes = "application/json")
    public GeneralResponseData auditOutWardNew(@RequestBody NewOutWardRequest request) throws Exception{
        if (request.getId() == null ) {
            return new GeneralResponseData(-1, "id不能为空!");
        }
        if (request.getStatus() == null ) {
            return new GeneralResponseData(-1, "审核状态不能为空!");
        }
        if (request.getStatus().equals(3) && StringUtils.isBlank(request.getReview()) ) {
            return new GeneralResponseData(-1, "财务审核不通过,原因不能为空!");
        }
        if (!request.getStatus()[0].equals(1) && !request.getStatus()[0].equals(3)){
            return new GeneralResponseData(-1, "财务审核状态错误!");
        }
        SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();  // 获取登陆人信息
        return owrnService.auditOutWardNew(request,operator);
    }


    /**
     * 新公司用款 -  下发审核
     * @param request
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/auditOutWardNewBeSent", method = RequestMethod.POST, consumes = "application/json")
    public GeneralResponseData auditOutWardNewBeSent(@RequestBody NewOutWardRequest request) throws Exception{
        if (request.getId() == null ) {
            return new GeneralResponseData(-1, "id不能为空!");
        }
        if (request.getStatus() == null ) {
            return new GeneralResponseData(-1, "审核状态不能为空!");
        }
        if (request.getStatus().equals(4) && StringUtils.isBlank(request.getReview()) ) {
            return new GeneralResponseData(-1, "下发审核不通过,原因不能为空!");
        }
        if (!request.getStatus()[0].equals(2) &&!request.getStatus()[0].equals(4)  ){
                return new GeneralResponseData(-1, "下发审核状态错误!");
        }
        SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();  // 获取登陆人信息
        return owrnService.auditOutWardNewBeSent(request,operator);
    }

    /**
     * 新公司用款 -  下发锁定
     * @param request
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/outWardBeSentLock", method = RequestMethod.POST, consumes = "application/json")
    public GeneralResponseData outWardBeSentLock(@RequestBody NewOutWardRequest request) throws Exception{
        if (ObjectUtils.isEmpty(request.getIds())) {
            return new GeneralResponseData(-1, "任务ID数组不能为空!");
        }
        if (request.getLockStatus() == null ) {
            return new GeneralResponseData(-1, "锁定状态不能为空!");
        }
        SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();  // 获取登陆人信息
        return owrnService.outWardBeSentLock(request,operator);
    }

    /**
     * 新公司用款 -  绑定第三方下发
     * @param request
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/beSentThird", method = RequestMethod.POST, consumes = "application/json")
    public GeneralResponseData beSentThird(@RequestBody NewOutWardRequest request) throws Exception{
        if (request.getIds() == null ) {
            return new GeneralResponseData(-1, "任务IDS不能为空!");
        }
        if (request.getThirdCode() == null ) {
            return new GeneralResponseData(-1, "第三方编码不能为空!");
        }
        SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();  // 获取登陆人信息
        return owrnService.beSentThird(request,operator);
    }

    /**
     * 新公司用款 -  操作第三方提现
     * @param request
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/takeMoneyThird", method = RequestMethod.POST, consumes = "application/json")
    public GeneralResponseData takeMoneyThird(@RequestBody NewOutWardRequest request) throws Exception{
        if (request.getId() == null ) {
            return new GeneralResponseData(-1, "任务ID不能为空!");
        }
        if (request.getFee() == null ) {
            return new GeneralResponseData(-1, "第三方费率必填!");
        }
        SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();  // 获取登陆人信息
        return owrnService.takeMoneyThird(request,operator);
    }

    /**
     * 新公司用款 -  第三方下发完成
     * @param request
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/thirdOutAccountFinish", method = RequestMethod.POST, consumes = "application/json")
    public GeneralResponseData thirdOutAccountFinish(@RequestBody NewOutWardRequest request) throws Exception{
        if (request.getId() == null ) {
            return new GeneralResponseData(-1, "任务ID不能为空!");
        }
        SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();  // 获取登陆人信息
        return owrnService.thirdOutAccountFinish(request,operator);
    }

    /**
     * 新公司用款 -  第三方下发失败
     * @param request
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/thirdOutAccountFailing", method = RequestMethod.POST, consumes = "application/json")
    public GeneralResponseData thirdOutAccountFailing(@RequestBody NewOutWardRequest request) throws Exception{
        if (request.getId() == null ) {
            return new GeneralResponseData(-1, "任务ID不能为空!");
        }if (request.getOutFailing() == null ) {
            return new GeneralResponseData(-1, "下发失败标识不能为空!");
        }
        SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();  // 获取登陆人信息
        return owrnService.thirdOutAccountFailing(request,operator);
    }


    /**
     * 新公司用款 -  财务对账完成
     * @param request
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/cfoInAccountFinish", method = RequestMethod.POST, consumes = "application/json")
    public GeneralResponseData cfoInAccountFinish(@RequestBody NewOutWardRequest request) throws Exception{
        if (request.getId() == null ) {
            return new GeneralResponseData(-1, "任务ID不能为空!");
        }
        SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();  // 获取登陆人信息
        return owrnService.cfoInAccountFinish(request,operator);
    }

    /**
     * 新公司出款 -  增加备注
     * @param request
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/remark", method = RequestMethod.POST, consumes = "application/json")
    public GeneralResponseData remark(@RequestBody NewOutWardRequest request) throws Exception{
        if (request.getId() == null ) {
            return new GeneralResponseData(-1, "当前ID不能为空!");
        }if (request.getRemark() == null ) {
            return new GeneralResponseData(-1, "备注内容不能为空!");
        }
        SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();  // 获取登陆人信息
        return owrnService.remark(request,operator);
    }

    /**
     * 新公司出款 - 用途类型管理 - 新增
     * @param request
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/addOutUseManage", method = RequestMethod.POST, consumes = "application/json")
    public GeneralResponseData addOutUseManage(@RequestBody NewOutWardTypeManageRequest request) throws Exception{
        SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();

        if (StringUtils.isBlank(request.getUseName())){
            return new GeneralResponseData<>(-1, "用途类型名称(useName)参数必传!");
        }
        // biz_outusemanage_request -- 新公司用款 - 用途类型管理表
        NewOutWardEntity newOutWardEntity = new NewOutWardEntity();
        newOutWardEntity.setCreateId(Long.valueOf(operator.getId()));   // 用户Id
        newOutWardEntity.setCreateName(operator.getUid());              // 用户名称
        newOutWardEntity.setUseName(request.getUseName());              // 类型名称

        return owrnService.addOutUseManage(newOutWardEntity);
    }


    /**
     * 新公司出款 - 用途类型管理 - 查询
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/findOutUseManage", method = RequestMethod.GET)
    public GeneralResponseData findOutUseManage() throws Exception{

        List<NewOutWardEntity> entity = owrnService.findOutUseManageAll();

        GeneralResponseData responseData = new GeneralResponseData(1, "查询成功!");
        responseData.setData(entity);
        return responseData;
    }


    /**
     * 新公司出款 - 用途类型管理 - 修改
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/modifyOutUseManage", method = RequestMethod.POST, consumes = "application/json")
    public GeneralResponseData modifyOutUseManage(@RequestBody NewOutWardTypeManageRequest request) throws Exception{
        SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
        if (request.getId() == null || StringUtils.isBlank(request.getUseName())){
            return new GeneralResponseData<>(-1, "用途类型 ( id,useName ) 参数必传!");
        }
        NewOutWardEntity entity = owrnService.findOutUseManageById(request.getId());
        if (entity== null){
            return new GeneralResponseData<>(-1, "用途类型已不存在,请关闭窗口重新操作!");
        }
        request.setHandelId(Long.valueOf(sysUser.getId()));
        request.setHandelName(sysUser.getUid());
        GeneralResponseData<ResponseDataNewPay> responseData;
        try {
            responseData = owrnService.modifyOutUseManage(request);
            return responseData;
        } catch (Exception e) {
            logger.error("modifyOutUseManage   fail : ", e);
            responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
                    "修改失败:" + e.getLocalizedMessage());
            return responseData;
        }
    }

    /**
     * 新公司出款 - 用途类型管理 - 删除(逻辑删除)
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/deleteOutUseManage", method = RequestMethod.POST, consumes = "application/json")
    public GeneralResponseData deleteOutUseManage(@RequestBody NewOutWardTypeManageRequest request) throws Exception{
        SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
        if (request.getId() == null){
            return new GeneralResponseData<>(-1, "用途类型(id)参数不能为空!");
        }
        request.setHandelId(Long.valueOf(operator.getId()));
        request.setHandelName(operator.getUid());
        GeneralResponseData<ResponseDataNewPay> responseData;
        try {
            responseData = owrnService.deleteOutUseManage(request);
            return responseData;
        } catch (Exception e) {
            logger.error("deleteOutUseManage   fail : ", e);
            responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
                    "删除失败:" + e.getLocalizedMessage());
            return responseData;
        }
    }

    /**
     * 新公司出款 - 统计
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/statistics", method = RequestMethod.GET)
    public String statistics() throws Exception{
        SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
        GeneralResponseData<BizOutwardnewStatistics> responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue());

        BizOutwardnewStatistics entity = owrnService.statistics(operator);
        responseData.setData(entity);
        // 参数返回
        return mapper.writeValueAsString(responseData);

    }



    /**
     * 新公司用款 -  成功失败查询List
     * @param request
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/findUsemoneyTakeList", method = RequestMethod.POST, consumes = "application/json")
    public String  findUsemoneyTakeList( @RequestBody UseMoneyTakeRequest request) throws Exception{
        String params = buildParams().toString();
        logger.trace(String.format("%s，参数：%s", "账号分页获取", params));
        try {
            GeneralResponseData<List<BizUseMoneyTakeEntity>> responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue());
            // 判断登陆是否失效
            SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
            if (sysUser == null) {
                responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆");
                return mapper.writeValueAsString(responseData);
            }
            //  分页条件    [,Sort.Direction.DESC, "updateTime"] - 根据字段排序
            PageRequest pageRequest = new PageRequest(
                    request.getPageNo(), request.getPageSize() != null ?  request.getPageSize() : AppConstants.PAGE_SIZE
                    , Sort.Direction.DESC, "updateTime");

            // 封装查询条件
            List<SearchFilter> filterToList = queryConditions1(request,sysUser);
            SearchFilter[] filterToArray = filterToList.toArray(new SearchFilter[filterToList.size()]);
            // 返回参数构建
            Specification<BizUseMoneyTakeEntity> specif = DynamicSpecifications.build(BizUseMoneyTakeEntity.class, filterToArray);

            // 列表查询
            responseData = owrnService.findUsemoneyTakeList(pageRequest ,specif ,responseData);
            // 参数返回
            return mapper.writeValueAsString(responseData);

        } catch (Exception e) {
            e.printStackTrace();
            logger.error(String.format("%s，参数：%s，结果：%s", "账号分页获取", params, e.getMessage()));
            return mapper.writeValueAsString(new GeneralResponseData<>(
                    GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
        }
    }







    /**  封装 - 新公司出款的列表查询条件 */
    public List<SearchFilter> queryConditions ( NewOutWardRequest newOutWardRequest,SysUser sysUser){
        List<SearchFilter> filterToList = DynamicSpecifications.build(request);

        if (newOutWardRequest.getHandicap()!=null){          //   盘口查询
            filterToList.add(new SearchFilter("handicap", SearchFilter.Operator.EQ, newOutWardRequest.getHandicap()));
        }
        if (newOutWardRequest.getCreateTimeStart()!=null){   //   申请时间 大于或等于
            filterToList.add(new SearchFilter("createTime", SearchFilter.Operator.GTE, newOutWardRequest.getCreateTimeStart()));
        }
        if (newOutWardRequest.getCreateTimeEnd()!=null){     //   申请时间 小于或等于
            Date date = newOutWardRequest.getCreateTimeEnd();
            Calendar calendar = new GregorianCalendar();
            calendar.setTime(date);
            calendar.add(calendar.DATE,1);//把日期往后增加一天.整数往后推,负数往前移动
            date=calendar.getTime();   //这个时间就是日期往后推一天的结果
            filterToList.add(new SearchFilter("createTime", SearchFilter.Operator.LTE, date));
        }
        if (newOutWardRequest.getUsetype()!=null){           //   用途类型ID
            filterToList.add(new SearchFilter("usetype", SearchFilter.Operator.EQ, newOutWardRequest.getUsetype()));
        }
        if (newOutWardRequest.getMember()!=null){            //   申请人
            filterToList.add(new SearchFilter("member", SearchFilter.Operator.LIKE, newOutWardRequest.getMember()));
        }
        if (newOutWardRequest.getReceiptType()!=null){       //   收款方式 0-银行卡 1-第三方
            filterToList.add(new SearchFilter("receiptType", SearchFilter.Operator.EQ, newOutWardRequest.getReceiptType()));
        }
//        String[] statusToArray = newOutWardRequest.getStatus().split(",");
//        if (statusToArray != null && statusToArray.length > 0) { // 多状态查询
//            filterToList.add(new SearchFilter("status", SearchFilter.Operator.IN, statusToArray));
//        }
        if (newOutWardRequest.getToAccountBank()!=null) {  // 开户行
            filterToList.add(new SearchFilter("toAccountBank", SearchFilter.Operator.LIKE, newOutWardRequest.getToAccountBank()));
        }
        if (newOutWardRequest.getToAccountOwner()!=null) { // 开户人
            filterToList.add(new SearchFilter("toAccountOwner", SearchFilter.Operator.LIKE, newOutWardRequest.getToAccountOwner()));
        }
        if (newOutWardRequest.getCode()!=null) {           // 编码
            filterToList.add(new SearchFilter("code", SearchFilter.Operator.EQ, newOutWardRequest.getCode()));
        }
        /**
         * 下发任务
         *    新下发任务 下发审核通过2（flag="all_1"）
         *    正在下发 等待到账5（flag="all_2"）
         * 我已锁定
         *    正在下发 等待到账5（本人锁定） 锁定人：登录人的ID  （flag="mine_1"）
         *    等待到账 （flag="mine_2"）*/
        Integer[] sts = new Integer[]{2,5};
        if (newOutWardRequest.getFlag()!=null) {
            /** 0-待审核,1-财务审核通过,2-下发审核通过,3-财务审核不通过,4-下发审核不通过,5-等待到账 ,6-完成出款,7-确认对账 8-出款失败*/
            String flag = newOutWardRequest.getFlag();
            if ("all_1".equals(flag)){
                filterToList.add(new SearchFilter("status", SearchFilter.Operator.EQ, "2"));     //  下发人员审核通过
                filterToList.add(new SearchFilter("lockStatus", SearchFilter.Operator.EQ, "0")); //  未锁定
            }else if ("all_2".equals(flag)){
                filterToList.add(new SearchFilter("status", SearchFilter.Operator.IN, sts));          //  下发人员审核通过 || 等待到账
                filterToList.add(new SearchFilter("lockStatus", SearchFilter.Operator.EQ, "1")); //   已锁定
            }else if ("mine_1".equals(flag)){
                filterToList.add(new SearchFilter("status", SearchFilter.Operator.EQ, 2));
                filterToList.add(new SearchFilter("lockId", SearchFilter.Operator.EQ, sysUser.getId()));
                filterToList.add(new SearchFilter("lockStatus", SearchFilter.Operator.EQ, 1));
            }else if ("mine_2".equals(flag)){
                filterToList.add(new SearchFilter("status", SearchFilter.Operator.EQ, 5));
                filterToList.add(new SearchFilter("lockId", SearchFilter.Operator.EQ, sysUser.getId()));
                filterToList.add(new SearchFilter("lockStatus", SearchFilter.Operator.EQ, 1));
            }
        }else {
            if ( newOutWardRequest.getStatus() != null ) {        //   状态查询
                filterToList.add(new SearchFilter("status", SearchFilter.Operator.IN, newOutWardRequest.getStatus()));
            }
        }
        return filterToList;
    }



    /**  封装 - 新公司出款的列表查询条件 */
    public List<SearchFilter> queryConditions1 ( UseMoneyTakeRequest useMoneyTakeRequest,SysUser sysUser){
        List<SearchFilter> filterToList = DynamicSpecifications.build(request);

        if (useMoneyTakeRequest.getHandicap()!=null){          //   盘口查询
            filterToList.add(new SearchFilter("handicap", SearchFilter.Operator.EQ, useMoneyTakeRequest.getHandicap()));
        }
        if (useMoneyTakeRequest.getCreateTimeStart()!=null){   //   申请时间 大于或等于
            filterToList.add(new SearchFilter("createTime", SearchFilter.Operator.GTE, useMoneyTakeRequest.getCreateTimeStart()));
        }
        if (useMoneyTakeRequest.getCreateTimeEnd()!=null){     //   申请时间 小于或等于
            Date date = useMoneyTakeRequest.getCreateTimeEnd();
            Calendar calendar = new GregorianCalendar();
            calendar.setTime(date);
            calendar.add(calendar.DATE,1);//把日期往后增加一天.整数往后推,负数往前移动
            date=calendar.getTime();   //这个时间就是日期往后推一天的结果
            filterToList.add(new SearchFilter("createTime", SearchFilter.Operator.LTE, date));
        }
//        if (newOutWardRequest.getUsetype()!=null){           //   用途类型ID
//            filterToList.add(new SearchFilter("usetype", SearchFilter.Operator.EQ, newOutWardRequest.getUsetype()));
//        }
//        if (newOutWardRequest.getMember()!=null){            //   申请人
//            filterToList.add(new SearchFilter("member", SearchFilter.Operator.LIKE, newOutWardRequest.getMember()));
//        }
//        if (newOutWardRequest.getReceiptType()!=null){       //   收款方式 0-银行卡 1-第三方
//            filterToList.add(new SearchFilter("receiptType", SearchFilter.Operator.EQ, newOutWardRequest.getReceiptType()));
//        }
//        String[] statusToArray = newOutWardRequest.getStatus().split(",");
//        if (statusToArray != null && statusToArray.length > 0) { // 多状态查询
//            filterToList.add(new SearchFilter("status", SearchFilter.Operator.IN, statusToArray));
//        }
        if (useMoneyTakeRequest.getToAccountBank()!=null) {  // 开户行
            filterToList.add(new SearchFilter("toAccountBank", SearchFilter.Operator.LIKE, useMoneyTakeRequest.getToAccountBank()));
        }
        if (useMoneyTakeRequest.getToAccountOwner()!=null) { // 开户人
            filterToList.add(new SearchFilter("toAccountOwner", SearchFilter.Operator.LIKE, useMoneyTakeRequest.getToAccountOwner()));
        }
        if (StringUtils.isNotBlank(useMoneyTakeRequest.getCode())) {           // 编码
            filterToList.add(new SearchFilter("code", SearchFilter.Operator.EQ, useMoneyTakeRequest.getCode()));
        }
        if (useMoneyTakeRequest.getStatus() != null ) {        //   状态查询
            filterToList.add(new SearchFilter("status", SearchFilter.Operator.IN, useMoneyTakeRequest.getStatus()));
        }
        return filterToList;
    }

}
