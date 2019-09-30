package com.xinbo.fundstransfer.ali4enterprise.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import com.xinbo.fundstransfer.ali4enterprise.inputdto.Ali4EpAccountInputDTO;
import com.xinbo.fundstransfer.domain.entity.BizAccount;

public interface Ali4EpAccountService {

	List<BizAccount> list(final Ali4EpAccountInputDTO inputDTO, Pageable pageable);

	Long count(final Ali4EpAccountInputDTO inputDTO);

}
