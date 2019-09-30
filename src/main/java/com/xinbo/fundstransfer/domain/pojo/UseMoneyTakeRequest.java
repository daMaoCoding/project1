package com.xinbo.fundstransfer.domain.pojo;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 描述:新公司入款request
 *
 * @author cobby
 * @create 2019年08月24日9:36
 */
@Data
public class UseMoneyTakeRequest {

    private Long id;

    /** 盘口 */
    private Integer handicap;

    /** 第三方编号 */
    private String thirdCode;

    /** 公司用款表编码 */
    private String code;

    /** 状态，0：已确认，1：已取消，2：确认失败 */
    private String status;

    /** 金额 */
    private BigDecimal amount;

    /** 创建时间 */
    private Date createTime;

    /** 备注 */
    private String remark;

    /** 收款银行类型，像中国银行，招商银行 */
    private String toAccountBank;

    /** 收款卡的帐号 */
    private String toAccount;

    /** 收款卡的开户人 */
    private String toAccountOwner;

    /** 下发耗时，单位秒 */
    private Long timeConsuming;

    /** 当前页   */
    private Integer pageNo;

    /** 当前页显示数量   */
    private Integer pageSize;

    /** 申请时间Start */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date createTimeStart;

    /** 申请时间end   */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date createTimeEnd;


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

}
