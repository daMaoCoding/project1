package com.xinbo.fundstransfer.domain.repository;

import com.xinbo.fundstransfer.component.jpa.BaseRepository;
import com.xinbo.fundstransfer.domain.entity.BizDaifuSurpportBanktypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * @author Administrator
 */
public interface DaifuSurpportBankTypeRepository extends BaseRepository<BizDaifuSurpportBanktypeEntity, Integer> {

	BizDaifuSurpportBanktypeEntity findById2(Integer id);

	BizDaifuSurpportBanktypeEntity findByProvider(String provider);
}
