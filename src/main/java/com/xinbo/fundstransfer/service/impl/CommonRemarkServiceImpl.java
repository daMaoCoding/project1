package com.xinbo.fundstransfer.service.impl;

import com.google.common.collect.Maps;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.domain.entity.BizCommonRemarkEntity;
import com.xinbo.fundstransfer.domain.pojo.BizCommonRemarkInputDTO;
import com.xinbo.fundstransfer.domain.pojo.BizCommonRemarkOutputDTO;
import com.xinbo.fundstransfer.domain.repository.BizCommonRemarkEntityRepository;
import com.xinbo.fundstransfer.service.CommonRemarkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CommonRemarkServiceImpl implements CommonRemarkService {
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private BizCommonRemarkEntityRepository repository;

	@Override
	@Transactional(rollbackOn = Exception.class)
	public BizCommonRemarkOutputDTO add(BizCommonRemarkInputDTO inputDTO) {
		if (inputDTO != null) {
			try {
				BizCommonRemarkEntity entity = new BizCommonRemarkEntity();
				entity = entity.wrapFromInputDTO(inputDTO);
				entity = repository.saveAndFlush(entity);
				BizCommonRemarkOutputDTO outputDTO = new BizCommonRemarkOutputDTO();
				outputDTO = outputDTO.wrapFromEntity(entity);
				return outputDTO;
			} catch (Exception e) {
				log.error("添加异常:", e);
			}
		}
		return null;
	}

	@Override
	@Transactional(rollbackOn = Exception.class)
	public BizCommonRemarkOutputDTO delete(BizCommonRemarkInputDTO inputDTO) {
		if (inputDTO != null) {
			try {
				Optional<BizCommonRemarkEntity> entity = repository.findById(inputDTO.getId());
				if (entity.isPresent()) {
					BizCommonRemarkEntity entity1 = entity.get();
					if (!BizCommonRemarkInputDTO.RemarkStatus.DELETE.getCode().equals(entity1.getStatus().intValue())) {
						entity1.setStatus(BizCommonRemarkInputDTO.RemarkStatus.DELETE.getCode().byteValue());
						entity1.setUpdateTime(Timestamp.from(Instant.now()));
						entity1.setUpdateUid(inputDTO.getSysUser() == null ? "sys" : inputDTO.getSysUser().getUid());
						entity1 = repository.saveAndFlush(entity1);
						BizCommonRemarkOutputDTO outputDTO = new BizCommonRemarkOutputDTO();
						outputDTO = outputDTO.wrapFromEntity(entity1);
						return outputDTO;
					}
				}
			} catch (Exception e) {
				log.error("删除异常:", e);
			}
		}
		return null;
	}

	@Override
	public Map<String, Object> list(BizCommonRemarkInputDTO inputDTO, PageRequest pageRequest) {
		if (null == inputDTO || null == inputDTO.getBusinessId())
			return null;
		List<BizCommonRemarkEntity> list = null;
		Page<BizCommonRemarkEntity> page = null;
		Map<String, Object> res = Maps.newHashMap();
		res.put("page", new Paging());
		try {
			Specification specification = Specification.where(null);
			specification = specification.and((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder
					.equal(root.get("businessId").as(Integer.class), inputDTO.getBusinessId()));
			specification = specification.and((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder
					.equal(root.get("type").as(String.class), inputDTO.getType()));
			specification = specification.and(
					(root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("status").as(Byte.class),
							BizCommonRemarkInputDTO.RemarkStatus.NORMAL.getCode().byteValue()));
			page = repository.findAll(specification, pageRequest);
			if (!ObjectUtils.isEmpty(page)) {
				list = page.getContent();
			}
		} catch (Exception e) {
			log.error("查询异常:", e);
		}
		if (!CollectionUtils.isEmpty(list)) {
			Paging paging = new Paging(page);
			res.put("page", paging);
			List<BizCommonRemarkOutputDTO> data = list.stream().map(p -> {
				BizCommonRemarkOutputDTO outputDTO = new BizCommonRemarkOutputDTO();
				outputDTO = outputDTO.wrapFromEntity(p);
				return outputDTO;
			}).collect(Collectors.toList());
			res.put("data", data);
		}
		return res;
	}

	@Override
	public String latestRemark(Integer businessId, String type) {
		if (null == businessId)
			return null;
		BizCommonRemarkEntity entity = repository.findFirstByBusinessIdAndTypeAndStatusOrderByCreateTimeDesc(businessId,
				type, BizCommonRemarkInputDTO.RemarkStatus.NORMAL.getCode().byteValue());
		return null == entity ? null : entity.getRemark();
	}
}
