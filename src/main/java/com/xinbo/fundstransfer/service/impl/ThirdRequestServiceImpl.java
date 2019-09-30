package com.xinbo.fundstransfer.service.impl;

import com.xinbo.fundstransfer.domain.entity.BizThirdRequest;
import com.xinbo.fundstransfer.domain.repository.ThirdRequestRepository;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import com.xinbo.fundstransfer.service.ThirdRequestService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.query.internal.QueryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class ThirdRequestServiceImpl implements ThirdRequestService {
	@Autowired
	private ThirdRequestRepository thirdRequestRepository;
	@Autowired
	private EntityManager entityManager;

	/**
	 * 通过缓存的 订单号#盘口id 查询 订单
	 *
	 * @param cacheStr
	 * @return
	 */
	@Override
	public BizThirdRequest findOneByCacheStr(String cacheStr) {
		String spiltChar = "#";
		try {
			if (StringUtils.isNotBlank(cacheStr) && cacheStr.contains(spiltChar)) {
				String[] str = cacheStr.split(spiltChar);
				String sql = " from BizThirdRequest r  where r.orderNo =:orderNo and  r.handicap =:handicap and r.amount=:amount   ";
				Query query = entityManager.createQuery(sql, BizThirdRequest.class);
				query.setParameter("orderNo", str[0]);
				query.setParameter("handicap", Integer.valueOf(str[1]));
				query.setParameter("amount", new BigDecimal(str[2]));
				List<BizThirdRequest> res = query.getResultList();
				log.debug(" 查询结果:{}", ObjectMapperUtils.serialize(res));
				if (!CollectionUtils.isEmpty(res)) {
					return res.get(0);
				}
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public BizThirdRequest findByHandicapAndOrderNo(Integer handicapId, String orderNo) {
		return thirdRequestRepository.findByHandicapAndOrderNo(handicapId, orderNo);
	}

	@Override
	@Transactional
	public BizThirdRequest save(BizThirdRequest o) {
		return thirdRequestRepository.save(o);
	}

	@Override
	public Page<BizThirdRequest> findPage(Specification<BizThirdRequest> specification, Pageable pageable) {
		return thirdRequestRepository.findAll(specification, pageable);
	}

}
