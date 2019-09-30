package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import com.xinbo.fundstransfer.domain.entity.BizUseMoneyTakeEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 描述:新公司出款 - 成功失败记录
 *
 * @author cobby
 * @create 2019-09-14 13:17
 */
public interface BizUseMoneyTakeRepository extends BaseRepository<BizUseMoneyTakeEntity, Long> {

    /**
     * @param bankBalance  第三方后台余额
     * @param balance      系统余额
     * @param code         公司用款表编码
     * @return
     */
    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = "update biz_usemoneytake_request " +
            "set bank_balance = :bankBalance ,balance=:balance  where code = :code and status = 0")
    int updateThirdBankBalance(@Param("bankBalance")BigDecimal bankBalance, @Param("balance")BigDecimal balance,@Param("code") String code);
}
