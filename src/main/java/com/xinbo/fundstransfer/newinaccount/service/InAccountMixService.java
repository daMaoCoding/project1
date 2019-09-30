package com.xinbo.fundstransfer.newinaccount.service;

import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.newinaccount.dto.input.CardForPayInputDTO;
import com.xinbo.fundstransfer.newinaccount.dto.output.CardForPayOutputDTO;

/**
 * @author blake
 */
public interface InAccountMixService {
	
	/** 1.5.8 会员支付通道上报选择银行卡（新公司入款-银行卡转账与云闪付混合） */
	CardForPayOutputDTO cardForPayMix(CardForPayInputDTO inputDTO, BizHandicap handicap);

}
