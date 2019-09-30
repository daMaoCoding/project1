package com.xinbo.fundstransfer.daifucomponent.service.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.daifucomponent.dto.input.DaifuConfigSyncReqParamConfig;
import com.xinbo.fundstransfer.daifucomponent.dto.output.OutConfigDTO;
import com.xinbo.fundstransfer.daifucomponent.entity.DaifuConfigRequest;
import com.xinbo.fundstransfer.daifucomponent.http.Crk2PayCoreBalance;
import com.xinbo.fundstransfer.daifucomponent.service.DaifuConfigRequestService;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.repository.DaifuConfigRequestRepository;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import com.xinbo.fundstransfer.service.HandicapService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author blake
 *
 */
@Slf4j
@Service
public class DaifuConfigRequestServiceImpl implements DaifuConfigRequestService {

	private static final Byte ONE = new Byte("1");

	@Autowired
	private DaifuConfigRequestRepository daifuConfigRequestDao;
	@Autowired
	private DaifuExtService daifuExtService;

	@Autowired
	private HandicapService handicapService;

	@Override
	public Page<DaifuConfigRequest> findPage(Specification<DaifuConfigRequest> specification, Pageable pageable)
			throws Exception {
		return daifuConfigRequestDao.findAll(specification, pageable);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.xinbo.fundstransfer.daifucomponent.service.DaifuConfigRequestService#
	 * saveOrUpdate(com.xinbo.fundstransfer.daifucomponent.entity.
	 * DaifuConfigRequest)
	 */
	@Transactional
	@Override
	public void saveOrUpdate(DaifuConfigRequest req) {
		Timestamp currentTime = new Timestamp(System.currentTimeMillis());
		req.setSyncTime(currentTime);

		/**
		 * <pre>
		 * 保存或者更新代付通道配置信息，实现逻辑：
		 * 1、根据盘口+提供商+商号查询存量数据
		 *         1.1 如果不存在，则直接保存
		 *         1.2 如果存在存量数据，根据传递进来的参数判断是更新状态/配置 还是 删除了
		 *            根据 req.getPlatformDeleteFlag() 是否等于 1来判断平台是否删除了。
		 *                如果删除：更新删除标志req.setPlatformDeleteFlag(1). 平台状态设置为 req.setPlatformStatus(0)
		 *                如果未删除，直接更新
		 * </pre>
		 */
		// 查询现有数据（有可能是已经删除了的）
		DaifuConfigRequest exists = getExists(req.getHandicapId(), req.getChannelName(), req.getMemberId());
		log.debug("查询到现有数据：{}", exists == null ? null : ObjectMapperUtils.serialize(exists));
		// 判断相同商号的数据是否存在
		// 如果不存在，进行插入操作
		// 如果存在，并且没有删除，判断平台传递进来的状态是什么，
		// 如果存在，并且已经删除
		if (ObjectUtils.isEmpty(exists)) {
			log.debug("不存在存量数据，进行新增");
			req.setCrkOutMoney(BigDecimal.ZERO);
			req.setCrkOutTimes(0);
			req.setCrkOutMoneyHistory(BigDecimal.ZERO);
			req.setCrkOutTimesHistory(0);
			req.setCrkStatus(ONE);
			req.setCreateTime(currentTime);
			daifuConfigRequestDao.save(req);
		} else {
			// 现有的是删除的并且传递进来的是删除的，则不保存
			boolean existsDelFlag = ONE.equals(exists.getPlatformDeleteFlag());
			boolean reqInDelFlag = ONE.equals(req.getPlatformDeleteFlag());
			if (existsDelFlag && reqInDelFlag) {
				log.debug("存量数据目前是删除状态，传递进来的也是删除状态，不进行保存");
				return;
			} else if (existsDelFlag && !reqInDelFlag) {
				log.debug("存量数据目前是删除状态，传递进来的不是删除状态，进行新增");
				// 如果现有的是删除的，传递进来的不是删除的。则新增
				req.setCrkStatus(ONE);
				req.setCreateTime(currentTime);
				req.setCrkOutMoney(BigDecimal.ZERO);
				req.setCrkOutTimes(0);
				req.setCrkOutMoneyHistory(BigDecimal.ZERO);
				req.setCrkOutTimesHistory(0);
				daifuConfigRequestDao.save(req);
			} else {
				log.debug("存量数据目前不是删除状态，传递进来的也不是删除状态，更新");
				// 如果现有的不是删除的，传递进来的不是删除的。则更新
				req.setId(exists.getId());
				req.setCrkStatus(exists.getCrkStatus());
				req.setCreateTime(exists.getCreateTime());
				if(BigDecimal.ZERO.equals(req.getCrkOutMoney())
						&& req.getCrkOutTimes()==0 ){
					exists.setCrkOutMoney(BigDecimal.ZERO);
					exists.setCrkOutTimes(0);
				}else {
					req.setCrkOutMoney(exists.getCrkOutMoney());
					req.setCrkOutTimes(exists.getCrkOutTimes());
				}
				req.setCrkOutMoneyHistory(exists.getCrkOutMoneyHistory());
				req.setCrkOutTimesHistory(exists.getCrkOutTimesHistory());
				daifuConfigRequestDao.saveAndFlush(req);
			}
		}
	}

	/**
	 * 通过盘口 、提供商、商号 查询现有数据
	 *
	 * @param HandicapId
	 * @param channelName
	 * @param memberId
	 * @return
	 */
	private DaifuConfigRequest getExists(Integer handicapId, String channelName, String memberId) {
		DaifuConfigRequest exists = null;
		// 使用删除标记升序 + 状态倒序 取相同 盘口号 + 提供商 + 商号 的数据
		//Order orderPlatformDeleteFlagAsc = new Order(Direction.ASC,"platformDeleteFlag");
		Sort orderPlatformDeleteFlagAsc = new Sort(Direction.ASC,"platformDeleteFlag");
		//Order orderPlatformStatusDesc = new Order(Direction.DESC, "platformStatus");
		Sort orderPlatformStatusDesc = new Sort(Direction.DESC, "platformStatus");
		List<DaifuConfigRequest> dbExists = daifuConfigRequestDao.findAll((root, criteriaQuery, criteriaBuilder) -> {
			List<Predicate> predicateList = new ArrayList<>();
			Path<Integer> handicapPath = root.get("handicapId");
			Path<String> channelNamePath = root.get("channelName");
			Path<String> memberIdPath = root.get("memberId");
			Predicate p1 = criteriaBuilder.equal(handicapPath, handicapId);
			predicateList.add(p1);
			Predicate p2 = criteriaBuilder.equal(channelNamePath, channelName);
			predicateList.add(p2);
			Predicate p3 = criteriaBuilder.equal(memberIdPath, memberId);
			predicateList.add(p3);
			criteriaQuery.where(predicateList.toArray(new Predicate[predicateList.size()]));
			return null;
		}, orderPlatformDeleteFlagAsc.and(orderPlatformStatusDesc));
		if (!CollectionUtils.isEmpty(dbExists)) {
			exists = dbExists.get(0);
		}
		return exists;
	}

	@Override
	public BigDecimal getBalaceFromPayCore(Integer handicapId,Integer outConfigId) {
		BizHandicap bizHandicap = handicapService.findFromCacheById(handicapId);
		if(ObjectUtils.isEmpty(bizHandicap)) {
			throw new RuntimeException("未知的handicapId");
		}
		BigDecimal result = null;
		DaifuConfigRequest daifuConfig =null;
		Optional<DaifuConfigRequest> daifuConfigOptional = daifuConfigRequestDao.findById(outConfigId);
		if(!daifuConfigOptional.isPresent()) {
			throw new RuntimeException("未知的出款通道id");
		}else {
			daifuConfig = daifuConfigOptional.get();
		}
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		DaifuConfigSyncReqParamConfig config;
		try {
			config = mapper.readValue(daifuConfig.getConfig(),
					DaifuConfigSyncReqParamConfig.class);
		} catch (IOException e) {
			log.error("通道配置信息{}转换成DaifuConfigSyncReqParamConfig对象时异常",daifuConfig.getConfig(),e);
			throw new RuntimeException(e.getMessage());
		};
		String privateKey ="";
		String publicKey ="";
		if (config != null) {
			privateKey = config.getPrivateKey();
			publicKey = config.getPublicKey();
		}

		Crk2PayCoreBalance balanceResponse = daifuExtService.getBalance(bizHandicap.getCode(),
				daifuConfig.getChannelName(), daifuConfig.getMemberId(), daifuConfig.getThirdId(),
				privateKey,publicKey);
		if(DaifuServiceImpl.RESPONSE_DAIFU_CODE_SUCCESS.equals(balanceResponse.getRequestDaifuCode())) {
			result = new BigDecimal(balanceResponse.getRequestDaifuBalance()).divide(DaifuServiceImpl.bigDecimal100).setScale(2,RoundingMode.HALF_UP);
		}else {
			throw new RuntimeException(balanceResponse.getRequestDaifuErrorMsg());
		}
		return result;
	}

	/**
	 * 按代付通道id查询各状态的代付订单数量
	 */
	@Override
	public List<OutConfigDTO> findStaticByIdList(List<Integer> outConfigIdList) {
		List<Object> queryResult = daifuConfigRequestDao.findStaticByIdList(outConfigIdList);
		List<OutConfigDTO> result = null;
		if(!CollectionUtils.isEmpty(queryResult)) {
			result = new ArrayList<>();
			for(Object t:queryResult) {
				OutConfigDTO e = new OutConfigDTO();
				Object[] columnData = (Object[])t;
				e.setId(Integer.parseInt(columnData[0].toString()));
				e.setCountSuccess(Integer.parseInt(columnData[1].toString()));
				e.setCountError(Integer.parseInt(columnData[2].toString()));
				e.setCountPaying(Integer.parseInt(columnData[3].toString()));
				result.add(e);
			}
		}
		return result;
	}

}