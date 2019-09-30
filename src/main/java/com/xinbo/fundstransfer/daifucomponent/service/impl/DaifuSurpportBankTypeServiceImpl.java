package com.xinbo.fundstransfer.daifucomponent.service.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.xinbo.fundstransfer.component.net.http.RequestBodyParser;
import com.xinbo.fundstransfer.component.net.http.v2.HttpClientNew;
import com.xinbo.fundstransfer.daifucomponent.dto.input.DaifuSurpportBankTypeInputDTO;
import com.xinbo.fundstransfer.daifucomponent.dto.input.DaifuSynSurpportBankTypeInputDTO;
import com.xinbo.fundstransfer.daifucomponent.service.DaifuSurpportBankTypeService;
import com.xinbo.fundstransfer.domain.entity.BizDaifuSurpportBanktypeEntity;
import com.xinbo.fundstransfer.domain.repository.DaifuSurpportBankTypeRepository;
import lombok.extern.slf4j.Slf4j;
import okhttp3.RequestBody;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.persistence.criteria.Predicate;
import javax.transaction.Transactional;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author Administrator
 */
@Slf4j
@Service
public class DaifuSurpportBankTypeServiceImpl implements DaifuSurpportBankTypeService {
	@Autowired
	private DaifuSurpportBankTypeRepository repository;
	@Autowired
	RequestBodyParser requestBodyParser;

	public final LoadingCache<String, String> bankTypeCache = CacheBuilder.newBuilder().maximumSize(2)
			.expireAfterWrite(7, TimeUnit.DAYS).initialCapacity(512).build(new CacheLoader<String, String>() {
				@Override
				public String load(String key) {
					String bankType = queryAllSurpportBankType();
					return bankType;
				}
			});

	@Override
	public boolean queryBankTypeIncluded(String bankType) {
		boolean exist = false;
		try {
			String[] bankTypes = bankTypeCache.get("bankType").split(",");
			if (bankTypes != null && bankType.length() > 0) {
				exist = Arrays.asList(bankTypes).contains(bankType);
			}
			log.debug("是否包含在支持的银行类型里:{},{}", bankTypes, exist);
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		return exist;
	}

	@Override
	@Transactional(rollbackOn = Exception.class)
	public void freshBankTypeCache() {
		bankTypeCache.invalidateAll();
		bankTypeCache.refresh("bankType");
	}

	@Override
	public String queryAllSurpportBankType() {
		List<BizDaifuSurpportBanktypeEntity> list = repository.findAll();
		if (CollectionUtils.isEmpty(list)) {
			return StringUtils.EMPTY;
		}
		Set<String> set1 = new HashSet<>();
		list.stream().filter(p -> StringUtils.isNotBlank(p.getSupportBankType())).map(p -> p.getSupportBankType())
				.forEach(p -> {
					String[] bankType = p.split(",");
					set1.addAll(Arrays.asList(bankType));
				});
		String res = String.join(",", set1);
		log.debug("所有支持的银行类型:{}", res);
		return res;
	}

	@Override
	@Transactional(rollbackOn = Exception.class)
	public String querSurpportBankType(DaifuSynSurpportBankTypeInputDTO inputDTO) {
		log.debug("查询平台供应商支持的银行类型 参数:{}", inputDTO);
		ThreadLocal<String> local = new ThreadLocal<>();
		local.set("无结果!");
		RequestBody requestBody = requestBodyParser.querySurpportBankType(inputDTO.getProvider(), "699");
		HttpClientNew.getInstance().getPlatformServiceApi().querySurpportBankType(requestBody)
				.subscribe(simpleResponseData -> {
					log.debug("查询平台供应商支持的银行类型 返回结果:{}", simpleResponseData);
					String ret = simpleResponseData.getMessage();
					local.set(ret);
					if (simpleResponseData.getStatus() == 1) {
						ret = "成功!";
						local.set(ret);
						if (!ObjectUtils.isEmpty(simpleResponseData.getData())) {
							Map<String, String> res = (Map<String, String>) simpleResponseData.getData();
							saveAllBankType(res);
						}
					}
				}, e -> {
					log.error("查询平台供应商支持的银行类型失败:", e);
					String ret = e.getLocalizedMessage();
					local.set(ret);
				});
		return local.get();
	}

	@Override
	@Transactional(rollbackOn = Exception.class)
	public Map<String, String> saveAllBankType(Map<String, String> map) {
		log.debug("保存供应商支持银行类型 参数:{}", map);
		if (CollectionUtils.isEmpty(map)) {
			return null;
		}
		if (CollectionUtils.isEmpty(map.keySet())) {
			return null;
		}
		if (CollectionUtils.isEmpty(map.values())) {
			return null;
		}
		Map<String, String> res = null;
		try {
			List<BizDaifuSurpportBanktypeEntity> entityList = new ArrayList<>();
			res = new HashMap<>(128);
			for (Map.Entry<String, String> entry : map.entrySet()) {
				String key = entry.getKey();
				String val = entry.getValue();
				log.debug("key:{},val:{}", key, val);
				if (StringUtils.isBlank(key) || StringUtils.isBlank(val) || "null".equals(key) || "null".equals(val)) {
					continue;
				}

				BizDaifuSurpportBanktypeEntity entity = repository.findByProvider(key);
				if (!ObjectUtils.isEmpty(entity)) {
					entity.setAllBankType(val);
					if (StringUtils.isBlank(entity.getSupportBankType())) {
						entity.setSupportBankType(val);
					}
					entityList.add(entity);
				} else {
					entity = new BizDaifuSurpportBanktypeEntity();
					entity.setAllBankType(val);
					entity.setSupportBankType(val);
					entity.setProvider(key);
					repository.saveAndFlush(entity);
				}
				res.put(key, val);
			}
			if (!CollectionUtils.isEmpty(entityList)) {
				repository.save(entityList);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}

	@Override
	public Page<BizDaifuSurpportBanktypeEntity> list(DaifuSurpportBankTypeInputDTO inputDTO, PageRequest pageRequest) {
		log.debug("分页查询 供应商支持银行类型 参数:{},{}", inputDTO, pageRequest);
		Page<BizDaifuSurpportBanktypeEntity> page = null;
		try {
			page = repository.findAll((root, criteriaQuery, criteriaBuilder) -> {
				List<Predicate> predicateList = new ArrayList<>();
				if (StringUtils.isNotBlank(inputDTO.getProvider())) {
					Predicate p = criteriaBuilder.equal(root.get("provider").as(String.class), inputDTO.getProvider());
					predicateList.add(p);
				}
				if (StringUtils.isNotBlank(inputDTO.getBankType())) {
					Predicate p = criteriaBuilder.like(root.get("supportBankType").as(String.class),
							inputDTO.getBankType());
					predicateList.add(p);
				}
				Predicate[] p = new Predicate[predicateList.size()];
				criteriaQuery.where(criteriaBuilder.and(predicateList.toArray(p)));
				return criteriaQuery.getRestriction();
			}, pageRequest);
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.debug("分页查询 供应商支持银行类型 结果:{} ", page);
		return page;
	}

	@Override
	@Transactional(rollbackOn = Exception.class)
	public BizDaifuSurpportBanktypeEntity updateSurpport(DaifuSynSurpportBankTypeInputDTO inputDTO) {
		log.debug("页面更新 支持银行类型 参数:{}", inputDTO);
		BizDaifuSurpportBanktypeEntity entity = null;
		BizDaifuSurpportBanktypeEntity res = null;
		try {
			entity = repository.findById2(inputDTO.getId());
			if (ObjectUtils.isEmpty(entity)) {
				return res;
			}
			if (!entity.getProvider().equals(inputDTO.getProvider())) {
				log.debug("共用商名称不一致,传来的:{},库里:{}.", inputDTO.getProvider(), entity.getProvider());
				return res;
			}
			entity.setSupportBankType(inputDTO.getSupportBankType());
			res = repository.saveAndFlush(entity);
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.debug("页面更新 支持银行类型 结果:{}", res);
		return entity;
	}
}
