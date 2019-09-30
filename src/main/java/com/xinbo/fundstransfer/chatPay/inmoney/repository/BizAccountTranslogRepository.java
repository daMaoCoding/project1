package com.xinbo.fundstransfer.chatPay.inmoney.repository;

import com.xinbo.fundstransfer.chatPay.inmoney.entity.BizAccountTranslog;
import com.xinbo.fundstransfer.component.jpa.BaseRepository;

/**
 * 	兼职交易记录 DB仓库
 * @author ERIC
 *
 */
public interface BizAccountTranslogRepository extends BaseRepository<BizAccountTranslog, Long> {
	BizAccountTranslog findByCode(String code);
}
