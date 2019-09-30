package com.xinbo.fundstransfer.ali4enterprise.service.impl;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.xinbo.fundstransfer.ali4enterprise.inputdto.Ali4EpAccountInputDTO;
import com.xinbo.fundstransfer.ali4enterprise.service.Ali4EpAccountService;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.repository.AccountRepository;
import com.xinbo.fundstransfer.domain.repository.QueryNoCountDao;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class Ali4EpAccountServiceImpl implements Ali4EpAccountService {

	@Autowired
	private QueryNoCountDao dao;
	@Autowired
	private AccountRepository repository;

	public Ali4EpAccountServiceImpl() {
	}

	@Override
	public List<BizAccount> list(final Ali4EpAccountInputDTO inputDTO, Pageable pageable) {
		Specification<BizAccount> specification = getSpecification(inputDTO, 1);
		List<BizAccount> list = dao.findAll(specification, pageable, BizAccount.class).getContent();
		return list;
	}

	@Override
	public Long count(final Ali4EpAccountInputDTO inputDTO) {
		Specification<BizAccount> specification = getSpecification(inputDTO, 0);
		long count = repository.count(specification);
		return count;
	}

	private Specification<BizAccount> getSpecification(final Ali4EpAccountInputDTO inputDTO, int type) {
		return (root, criteriaQuery, criteriaBuilder) -> {
			if (type == 1) {
				criteriaQuery.multiselect(root.get("handicapId"), root.get("account"), root.get("bankName"),
						root.get("status"), root.get("createTime"));
			}
			List<Predicate> predicate = new ArrayList<>();
			predicate.add(
					criteriaBuilder.equal(root.get("type").as(Integer.class), AccountType.AliEnterPrise.getTypeId()));

			if (inputDTO.getHandicapId() != null) {
				predicate
						.add(criteriaBuilder.equal(root.get("handicapId").as(Integer.class), inputDTO.getHandicapId()));
			} else {
				if (!CollectionUtils.isEmpty(inputDTO.getHandicaps())) {
					if (inputDTO.getHandicaps().size() == 1) {
						predicate.add(criteriaBuilder.equal(root.get("handicapId").as(Integer.class),
								inputDTO.getHandicaps().get(0).getId()));
					} else {
						CriteriaBuilder.In in = criteriaBuilder.in(root.get("handicapId").as(Integer.class));
						for (BizHandicap handicap : inputDTO.getHandicaps()) {
							in.value(handicap.getId());
						}
						predicate.add(in);
					}
				}

			}
			if (StringUtils.isNotBlank(inputDTO.getAccount())) {
				predicate.add(criteriaBuilder.like(root.get("account").as(String.class), inputDTO.getAccount() + "%"));
			}
			if (StringUtils.isNotBlank(inputDTO.getBankName())) {
				predicate
						.add(criteriaBuilder.like(root.get("bankName").as(String.class), inputDTO.getBankName() + "%"));
			}
			if (null != inputDTO.getStatus() && inputDTO.getStatus().length > 0) {
				if (inputDTO.getStatus().length == 1) {
					predicate.add(criteriaBuilder.equal(root.get("status").as(Integer.class), inputDTO.getStatus()[0]));
				} else {
					CriteriaBuilder.In<Object> in = criteriaBuilder.in(root.get("status").as(Integer.class));
					Integer[] status = inputDTO.getStatus();
					for (int i = 0, len = status.length; i < len; i++) {
						in.value(status[i]);
					}
					predicate.add(in);
				}
			}
			if (null != inputDTO.getCreateTimeStart()) {
				predicate.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createTime").as(Date.class),
						inputDTO.getCreateTimeStart()));
			}
			if (null != inputDTO.getCreateTimeStart()) {
				predicate.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createTime").as(Date.class),
						inputDTO.getCreateTimeStart()));
			}
			Predicate[] pre = new Predicate[predicate.size()];
			Predicate predicate1 = criteriaQuery.where(predicate.toArray(pre)).getRestriction();
			return predicate1;
		};

	}
}
