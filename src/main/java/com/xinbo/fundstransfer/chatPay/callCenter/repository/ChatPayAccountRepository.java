package com.xinbo.fundstransfer.chatPay.callCenter.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import com.xinbo.fundstransfer.domain.entity.BizAccount;

import java.util.List;

/**
 * ************************
 *
 * @author tony
 */

public interface ChatPayAccountRepository extends BaseRepository<BizAccount, Integer> {

    /**
     * 通过id 和 卡类型查找账号信息
     */
    List<BizAccount> findAllByIdInAndTypeIn(Integer[] ids, Integer[] type);
}
