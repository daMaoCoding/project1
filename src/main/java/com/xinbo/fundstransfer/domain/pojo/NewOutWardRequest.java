package com.xinbo.fundstransfer.domain.pojo;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 描述:新公司入款request
 *
 * @author cobby
 * @create 2019年08月24日9:36
 */
@Data
public class NewOutWardRequest {

    /** ID*/
    private Long id;

    /** IDS*/
    private  String ids;

    /** 用途类型 - 新增时必填*/
    @NotNull
    private Integer usetype;

    /** 第三方编号 */
    private String thirdCode;

    /** 编号 */
    private String code;

    /** 盘口ID  - 新增时必填*/
    @NotNull
    private Integer handicap;

    /** 出款金额  - 新增时必填*/
    @NotNull
    private BigDecimal amount;

    /** 收款方式 0-银行卡 1-第三方  - 新增时必填*/
    @NotNull
    private Integer receiptType;

    /** 收款卡 银行名称  - 新增时必填*/
    @NotNull
    private String toAccountBank;

    /** 收款卡 银行卡号  - 新增时必填*/
    @NotNull
    private String toAccount;

    /** 收款卡 开户人  - 新增时必填*/
    @NotNull
    private String toAccountOwner;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private Date createTime;

    /** 用款人(新增公司用款创建人) */
    private String member;

    /** 用款人(新增公司用款创建人)- 编码 */
    private Integer memberCode;

    /** 申请时间Start */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date createTimeStart;

    /** 申请时间end   */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date createTimeEnd;

    /** 0-待审核,1-财务审核通过,2-下发审核通过,3-财务审核不通过,4-下发审核不通过,5-等待到账 ,6-完成出款,7-确认对账 8-出款失败*/
    private String status;
    private Integer[] statusToArray;

    /** 3-财务审核不通过,4-下发审核不通过  ====  原因 */
    private String review;

    /**''锁定状态''*/
    private Integer lockStatus;

    /** 第三方费率计算 */
    private BigDecimal fee;

    /**
     * 下发任务
     *    新下发任务 下发审核通过2（flag="all_1"）
     *    正在下发 等待到账5（flag="all_2"）
     * 我已锁定
     *    正在下发 等待到账5（本人锁定） 锁定人：登录人的ID  （flag="mine_1"）
     *    等待到账 （flag="mine_2"）*/
    private String flag;

    /** 下发失败 0 可重复下发 1 下发失败停止出款*/
    private Integer outFailing;

    /** 当前页   */
    private Integer pageNo;

    /** 当前页显示数量   */
    private Integer pageSize;

    public Integer[] getStatus() {
        if (StringUtils.isBlank(status)){
            return null;
        }
        String[] str = status.split(",");
        Integer[] array = new Integer[str.length];
        for(int i=0;i<str.length;i++) {
            array[i] = Integer.parseInt(str[i]);
        }
        return array;
    }

    public Integer[] getIds() {
        if (StringUtils.isBlank(ids)){
            return null;
        }
        String[] str = ids.split(",");
        Integer[] array = new Integer[str.length];
        for(int i=0;i<str.length;i++) {
            array[i] = Integer.parseInt(str[i]);
        }
        return array;
    }

}
