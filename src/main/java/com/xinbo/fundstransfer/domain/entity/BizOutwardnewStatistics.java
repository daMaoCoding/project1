package com.xinbo.fundstransfer.domain.entity;

import lombok.Data;

/**
 * 描述:公司用款统计
 *
 * @author cobby
 * @create 2019年09月12日14:32
 */
@Data
public class BizOutwardnewStatistics {

    /** 财务待审核 */
    private int status0;

    /** 下发待审核 */
    private int status1;

    /** 审核通过未锁定 */
    private int status2;

    /** 锁定未下发完成 */
    private int status3;

    /** 我已锁定未下发完成 */
    private int status4;

    /** 我已锁定绑定点击第三方提现正在出款中 */
    private int status5;

}
