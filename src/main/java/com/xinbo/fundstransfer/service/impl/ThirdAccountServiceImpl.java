package com.xinbo.fundstransfer.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.BizThirdAccountEntity;
import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import com.xinbo.fundstransfer.domain.pojo.*;
import com.xinbo.fundstransfer.domain.repository.BizThirdAccountEntityRepository;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.CommonRemarkService;
import com.xinbo.fundstransfer.service.HandicapService;
import com.xinbo.fundstransfer.service.ThirdAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.transaction.Transactional;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class ThirdAccountServiceImpl implements ThirdAccountService {

	@Autowired
	private BizThirdAccountEntityRepository repository;
	@Autowired
	private CommonRemarkService remarkService;
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private HandicapService handicapService;
	@Autowired
	private AccountService accountService;

	@Override
	public List<Integer> findBindedAccountIds() {
		List<BizThirdAccountEntity> list = repository.findAll();
		if (!CollectionUtils.isEmpty(list)) {
			return list.parallelStream().map(p -> p.getAccountId()).collect(Collectors.toList());
		}
		return Lists.newLinkedList();
	}

	@Override
	public List<UnBindThirdAccountOutputDTO> unbindThirdAccount(ThirdAccountInputDTO inputDTO) {
		if (inputDTO == null)
			return null;
		// and id not in IFNULL(( select account_id from biz_third_account ),0 )
		List<Integer> bindedAccountIds = findBindedAccountIds();
		StringBuilder sql = new StringBuilder(
				" select distinct  a.id,  a.handicap_id,  a.account,  a.bank_type ,  a.owner,h.name from biz_account a inner join biz_handicap h on a.handicap_id=h.id and h.status=1  where 1=1 and   a.type=2 and   a.status in (1,3,4)  ");
		if (!CollectionUtils.isEmpty(bindedAccountIds)) {
			sql.append(" and  a.id not in(");
			int size = bindedAccountIds.size();
			wrapInCondition(sql, bindedAccountIds, size);
		}
		if (!CollectionUtils.isEmpty(inputDTO.getHandicapId())) {
			sql.append(" and   a.handicap_id in(");
			List<Integer> handicapId = inputDTO.getHandicapId();
			int size = handicapId.size();
			wrapInCondition(sql, handicapId, size);
		}
		if (!StringUtils.isEmpty(inputDTO.getThirdName())) {
			sql.append(" and a.bank_type='").append(org.apache.commons.lang3.StringUtils.trim(inputDTO.getThirdName()))
					.append("'");
		}
		List<Object[]> list = entityManager.createNativeQuery(sql.toString()).getResultList();
		List<UnBindThirdAccountOutputDTO> res = Lists.newLinkedList();
		if (!CollectionUtils.isEmpty(list)) {
			res = list.parallelStream().map(p -> {
				UnBindThirdAccountOutputDTO outputDTO = new UnBindThirdAccountOutputDTO();
				outputDTO = outputDTO.wrapFromObj(p);
				return outputDTO;
			}).collect(Collectors.toList());
			res = res.parallelStream().filter(p -> org.apache.commons.lang.StringUtils.isNotBlank(p.getBankName()))
					.collect(Collectors.toList());
			boolean isOne = !CollectionUtils.isEmpty(inputDTO.getHandicapId()) && inputDTO.getHandicapId().size() == 1;
			res = StringUtils.isEmpty(inputDTO.getThirdName()) ? removeDuplicate(res)
					: !isOne ? removeDuplicate2(res) : res;
		}
		return res;
	}

	private List<UnBindThirdAccountOutputDTO> removeDuplicate(List<UnBindThirdAccountOutputDTO> list) {
		Set<UnBindThirdAccountOutputDTO> set = new TreeSet<>(
				Comparator.comparing(UnBindThirdAccountOutputDTO::getBankName));
		set.addAll(list);
		return Lists.newLinkedList(set);
	}

	private List<UnBindThirdAccountOutputDTO> removeDuplicate2(List<UnBindThirdAccountOutputDTO> list) {
		Set<UnBindThirdAccountOutputDTO> set2 = new TreeSet<>(
				Comparator.comparing(UnBindThirdAccountOutputDTO::getHandicapId));
		set2.addAll(list);
		return Lists.newLinkedList(set2);
	}

	@Override
	public BizThirdAccountOutputDTO findByAccountId(Integer accountId) {
		if (null == accountId)
			return null;
		BizThirdAccountEntity bizThirdAccountEntity = repository.findDistinctByAccountId(accountId);
		if (bizThirdAccountEntity != null) {
			BizThirdAccountOutputDTO outputDTO = new BizThirdAccountOutputDTO().wrapFromEntity(bizThirdAccountEntity);
			AccountBaseInfo baseInfo = accountService.getFromCacheById(outputDTO.getAccountId());
			if (baseInfo != null) {
				outputDTO.setHandicapId(baseInfo.getHandicapId());
				outputDTO.setThirdName(baseInfo.getBankName());
				outputDTO.setThirdNumber(baseInfo.getAccount());
				outputDTO.setStatus(baseInfo.getStatus().byteValue());
			}
			BizHandicap handicap = handicapService.findFromCacheById(outputDTO.getHandicapId());
			outputDTO.setHandicapName(handicap != null ? handicap.getName() : null);
			return outputDTO;
		}
		return null;
	}

	@Override
	public Map<String, Object> findByIdAndUnbind(ThirdAccountInputDTO inputDTO) {
		Map<String, Object> data = Maps.newHashMap();
		if (inputDTO != null) {
			BizThirdAccountOutputDTO outputDTO = findById(inputDTO);
			List<UnBindThirdAccountOutputDTO> unbindThirdAccount = unbindThirdAccount(inputDTO);
			data.put("binded", null);
			data.put("unBind", Lists.newArrayList());
			if (outputDTO != null) {
				data.put("binded", outputDTO);
			}
			if (!CollectionUtils.isEmpty(unbindThirdAccount)) {
				data.put("unBind", unbindThirdAccount);
			}
		}
		return data;
	}

	@Override
	public BizThirdAccountOutputDTO findById(ThirdAccountInputDTO inputDTO) {
		if (inputDTO == null)
			return null;
		Optional<BizThirdAccountEntity> entity = repository.findById(inputDTO.getId());
		if (entity.isPresent()) {
			BizThirdAccountEntity entity1 = entity.get();
			BizThirdAccountOutputDTO outputDTO = new BizThirdAccountOutputDTO();
			outputDTO = outputDTO.wrapFromEntity(entity1);
			AccountBaseInfo baseInfo = accountService.getFromCacheById(outputDTO.getAccountId());
			if (baseInfo != null) {
				outputDTO.setHandicapId(baseInfo.getHandicapId());
				outputDTO.setThirdName(baseInfo.getBankName());
				outputDTO.setThirdNumber(baseInfo.getAccount());
				outputDTO.setStatus(baseInfo.getStatus().byteValue());
			}
			BizHandicap handicap = handicapService.findFromCacheById(outputDTO.getHandicapId());
			outputDTO.setHandicapName(handicap == null ? null : handicap.getName());
			return outputDTO;
		}
		return null;
	}

	@Override
	@Transactional(rollbackOn = Exception.class)
	public BizThirdAccountOutputDTO edit(BizThirdAccountInputDTO inputDTO)
			throws InvocationTargetException, IllegalAccessException {
		BizThirdAccountOutputDTO outputDTO = new BizThirdAccountOutputDTO();
		if (inputDTO.getId() == null) {
			// 新增
			BizThirdAccountEntity entity = new BizThirdAccountEntity();
			entity = entity.wrapFromInputDTO(inputDTO);
			entity = repository.save(entity);
			outputDTO = outputDTO.wrapFromEntity(entity);
			AccountBaseInfo baseInfo = accountService.getFromCacheById(outputDTO.getAccountId());
			if (baseInfo != null) {
				outputDTO.setHandicapId(baseInfo.getHandicapId());
				outputDTO.setThirdName(baseInfo.getBankName());
				outputDTO.setThirdNumber(baseInfo.getAccount());
				outputDTO.setStatus(baseInfo.getStatus().byteValue());
			}
			BizHandicap handicap = handicapService.findFromCacheById(outputDTO.getHandicapId());
			outputDTO.setHandicapName(handicap != null ? handicap.getName() : null);
			return outputDTO;
		} else {
			// 修改
			Optional<BizThirdAccountEntity> entity = repository.findById(inputDTO.getId());
			if (entity.isPresent()) {
				BizThirdAccountEntity entity1 = entity.get();
				BizThirdAccountEntity newEntity = new BizThirdAccountEntity();
				newEntity = newEntity.wrapFromInputDTO(inputDTO, entity1);
				newEntity = repository.saveAndFlush(newEntity);

				outputDTO = outputDTO.wrapFromEntity(newEntity);
				AccountBaseInfo baseInfo = accountService.getFromCacheById(outputDTO.getAccountId());
				if (baseInfo != null) {
					outputDTO.setHandicapId(baseInfo.getHandicapId());
					outputDTO.setThirdName(baseInfo.getBankName());
					outputDTO.setThirdNumber(baseInfo.getAccount());
					outputDTO.setStatus(baseInfo.getStatus().byteValue());
				}
				BizHandicap handicap = handicapService.findFromCacheById(outputDTO.getHandicapId());
				outputDTO.setHandicapName(handicap != null ? handicap.getName() : null);
				return outputDTO;
			}
		}
		return null;
	}

	@Override
	public List<BizThirdAccountOutputDTO> list(BizThirdAccountInputDTO inputDTO) {
		return null;
	}

	@Override
	public Map<String, Object> page(ThirdAccountInputDTO inputDTO, PageRequest pageRequest) {
		Specification<BizThirdAccountEntity> specification = Specification.where(null);
		if (inputDTO.getStatus() == null) {
			specification = specification.and((root, criteriaQuery, criteriaBuilder) -> {
				CriteriaBuilder.In in = criteriaBuilder.in(root.get("status").as(Integer.class));
				Integer[] status = new Integer[] { AccountStatus.Normal.getStatus(), AccountStatus.Freeze.getStatus(),
						AccountStatus.StopTemp.getStatus() };
				for (Integer status1 : status) {
					in.value(status1);
				}
				return in;
			});
		} else {
			specification = specification.and((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder
					.equal(root.get("status").as(Integer.class), inputDTO.getStatus().intValue()));
		}

		boolean queryMine = inputDTO.getQueryPage() != null && inputDTO.getQueryPage().intValue() == 2;
		if (queryMine) {
			specification = specification.and((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder
					.equal(root.get("createUid").as(String.class), inputDTO.getSysUser().getUid()));
		}
		if (!ObjectUtils.isEmpty(inputDTO.getHandicapId())) {
			specification = specification.and((root, criteriaQuery, criteriaBuilder) -> {
				CriteriaBuilder.In in = criteriaBuilder.in(root.get("handicapCode").as(Integer.class));
				for (Integer handicap : inputDTO.getHandicapId()) {
					in.value(handicap);
				}
				return in;
			});
		}
		if (!StringUtils.isEmpty(inputDTO.getThirdName())) {
			specification = specification.and((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.or(
					criteriaBuilder.equal(root.get("thirdName").as(String.class), inputDTO.getThirdName()),
					criteriaBuilder.equal(root.get("thirdNumber").as(String.class), inputDTO.getThirdName())));
		}

		Page<BizThirdAccountEntity> page = repository.findAll(specification, pageRequest);
		List<BizThirdAccountOutputDTO> list = Lists.newLinkedList();
		List<BizThirdAccountOutputDTO> mine = Lists.newLinkedList();
		List<BizThirdAccountOutputDTO> other = Lists.newLinkedList();
		List<Integer> mySetUped = accountService.getMySetUpThirdAccount(inputDTO.getSysUser().getId());
		Map<String, Object> res = Maps.newHashMap();
		Paging paging = new Paging(page);
		res.put("page", paging);
		res.put("data", list);
		Comparator<BizThirdAccountOutputDTO> comparator = (o1, o2) -> {
			if (null != o1 && null != o2) {
				if (o1.getSysBalance() != null && o2.getSysBalance() != null) {
					if (o1.getSysBalance().compareTo(o2.getSysBalance()) > 0) {
						return -1;
					} else if (o1.getSysBalance().compareTo(o2.getSysBalance()) < 0) {
						return 1;
					} else {
						return 0;
					}
				}
				return 0;
			}
			return 0;
		};
		Predicate<BizThirdAccountOutputDTO> predicate = p -> p.getCreateUid().equals(inputDTO.getSysUser().getUid());
		// 我设定的第三方
		Predicate<BizThirdAccountOutputDTO> predicate2 = CollectionUtils.isEmpty(mySetUped) ? null
				: p -> mySetUped.contains(p.getAccountId());
		boolean predicate2IsNull = predicate2 == null;
		if (!CollectionUtils.isEmpty(page.getContent())) {
			page.getContent().stream().forEach(p -> {
				BizThirdAccountOutputDTO outputDTO = new BizThirdAccountOutputDTO();
				outputDTO = outputDTO.wrapFromEntity(p);
				AccountBaseInfo baseInfo = accountService.getFromCacheById(outputDTO.getAccountId());
				if (baseInfo != null) {
					outputDTO.setHandicapId(baseInfo.getHandicapId());
					outputDTO.setThirdName(baseInfo.getBankName());
					outputDTO.setThirdNumber(baseInfo.getAccount());
					outputDTO.setStatus(baseInfo.getStatus().byteValue());
				}
				BizHandicap handicap = handicapService.findFromCacheById(outputDTO.getHandicapId());
				outputDTO.setHandicapName(handicap != null ? handicap.getName() : null);
				outputDTO.setLatestRemark(remarkService.latestRemark(outputDTO.getId(), inputDTO.getType()));

				String handicapName = handicap == null ? "" : handicap.getName();
				outputDTO.setHandicapName(handicapName);
				BizAccount baseInfo1 = accountService.getById(outputDTO.getAccountId());
				BigDecimal balance = baseInfo1 == null ? BigDecimal.ZERO : baseInfo1.getBalance();
				outputDTO.setSysBalance(balance);
				if (!predicate2IsNull && predicate2.test(outputDTO)) {
					mine.add(outputDTO);
				} else {
					other.add(outputDTO);
				}
				// 第三方账号真实状态
				outputDTO.setStatus(baseInfo.getStatus().byteValue());
			});
			int countsMine = mine.size();
			int count = countsMine;
			Collections.sort(mine, comparator);
			list.addAll(mine);
			if (!queryMine) {
				Collections.sort(other, comparator);
				list.addAll(other);
				count += other.size();
			}
			if (count == 0) {
				paging = CommonUtils.getPage(0, inputDTO.getPageSize(), "0");
			} else {
				paging = CommonUtils.getPage(inputDTO.getPageNo() + 1, inputDTO.getPageSize(), String.valueOf(count));
			}
			res.put("page", paging);
			res.put("data", list);
		}
		return res;
	}

	@Override
	public Map<String, Object> pageBySql(ThirdAccountInputDTO inputDTO, PageRequest pageRequest) {
		if (null == inputDTO || null == pageRequest) {
			return null;
		}
		Map<String, Object> res = Maps.newHashMap();
		boolean queryMine = inputDTO.getQueryPage() != null && inputDTO.getQueryPage().intValue() == 2;
		List<Integer> mySetUped = accountService.getMySetUpThirdAccount(inputDTO.getSysUser().getId());
		if (queryMine && CollectionUtils.isEmpty(mySetUped)) {
			res.put("page", new Paging());
			res.put("data", Lists.newArrayList());
			return res;
		}
		StringBuilder sql = new StringBuilder(
				" select t.id,a.bank_name,a.account,a.status,t.login_account,t.login_pass,t.pay_pass,a.id account_id,t.third_name_url,a.handicap_id,t.create_time,t.create_uid,t.update_time,t.update_uid ,a.balance as balance,(case a.status when 1 then 1 when 5 then 2 when 4 then 3 when 3 then 4 else 5 end )tmp  from biz_account a left join biz_third_account t on a.id=t.account_id ");
		StringBuilder whereSql = new StringBuilder(" where 1=1  ");
		if (!StringUtils.isEmpty(inputDTO.getThirdName())) {
			whereSql.append(
					" and  (a.bank_name like '%" + StringUtils.trimAllWhitespace(inputDTO.getThirdName()) + "%'");
			whereSql.append(" or a.account like '%" + StringUtils.trimAllWhitespace(inputDTO.getThirdName()) + "%')");
		}
		if (queryMine && !CollectionUtils.isEmpty(mySetUped)) {
			whereSql.append(" and   a.id in(");
			int len = mySetUped.size();
			wrapInCondition(whereSql, mySetUped, len);
		}
		whereSql.append(" and a.type =2 ");
		if (inputDTO.getStatus() != null) {
			whereSql.append(" and a.status='" + inputDTO.getStatus().intValue() + "'");
		}
		whereSql.append(" and a.status in (1,3,4)");
		if (!ObjectUtils.isEmpty(inputDTO.getHandicapId())) {
			whereSql.append(" and a.handicap_id in(");
			List<Integer> handicaps = inputDTO.getHandicapId();
			int len = handicaps.size();
			wrapInCondition(whereSql, handicaps, len);
		}
		int start = pageRequest.getPageNumber() * pageRequest.getPageSize();
		int end = pageRequest.getPageSize();
		sql.append(whereSql).append(" order by tmp asc,a.balance desc ");
		Query query = entityManager.createNativeQuery(sql.toString());
		query.setFirstResult(start);
		query.setMaxResults(end);
		List<Object[]> list = query.getResultList();

		List<BizThirdAccountOutputDTO> data = Lists.newLinkedList();
		List<BizThirdAccountOutputDTO> mine = Lists.newLinkedList();
		List<BizThirdAccountOutputDTO> other = Lists.newLinkedList();

		StringBuilder count = new StringBuilder(
				"select count(1) from  biz_account a left join biz_third_account t on a.id=t.account_id ")
						.append(whereSql);
		Object counts = entityManager.createNativeQuery(count.toString()).getSingleResult();
		// 我设定的第三方
		Predicate<BizThirdAccountOutputDTO> predicate = CollectionUtils.isEmpty(mySetUped) ? null
				: p -> mySetUped.contains(p.getAccountId());

		Paging page = counts == null || Integer.valueOf(counts.toString()) == 0
				? CommonUtils.getPage(0, inputDTO.getPageSize(), "0")
				: CommonUtils.getPage(inputDTO.getPageNo() + 1, inputDTO.getPageSize(), String.valueOf(counts));
		res.put("page", page);
		res.put("data", data);
		list = list.stream().filter(Objects::nonNull).collect(Collectors.toList());
		if (!CollectionUtils.isEmpty(list)) {
			list.stream().forEach(p -> {
				BizThirdAccountOutputDTO outputDTO = new BizThirdAccountOutputDTO();
				outputDTO = outputDTO.wrapFromObj(p);
				if (outputDTO != null && !StringUtils.isEmpty(outputDTO.getThirdNameUrl())) {
					// 需求 7458
					if (!outputDTO.getThirdNameUrl().contains("http://")) {
						outputDTO.setThirdNameUrl("http://" + outputDTO.getThirdNameUrl());
					}
				}
				BizHandicap handicap = handicapService.findFromCacheById(outputDTO.getHandicapId());
				String handicapName = handicap == null ? "" : handicap.getName();
				outputDTO.setHandicapName(handicapName);
				String latestRemark = remarkService.latestRemark(outputDTO.getId(), inputDTO.getType());
				if (!StringUtils.isEmpty(latestRemark)) {
					outputDTO.setLatestRemark(latestRemark);
				}
				if (queryMine) {
					mine.add(outputDTO);
				} else {
					if (predicate != null && predicate.test(outputDTO)) {
						mine.add(outputDTO);
					} else {
						other.add(outputDTO);
					}
				}
			});
			if (!CollectionUtils.isEmpty(mine))
				data.addAll(mine);
			if (!CollectionUtils.isEmpty(other))
				data.addAll(other);
			res.put("data", data);
		}
		return res;
	}

	private void wrapInCondition(StringBuilder whereSql, List<Integer> vals, int len) {
		for (int i = 0; i < len; i++) {
			whereSql.append(vals.get(i));
			if (i < len - 1)
				whereSql.append(",");
			else
				whereSql.append(")");
		}
	}
}
