package com.xinbo.fundstransfer.chatPay.ptorder.repository;

import com.xinbo.fundstransfer.chatPay.inmoney.entity.BizAccountTranslog;
import com.xinbo.fundstransfer.component.jpa.BaseRepository;

public interface ChatPayAccountTranslogRepository extends BaseRepository<BizAccountTranslog, Long> {
	BizAccountTranslog findByCode(String code);
}
