/**
 *
 */
package com.xinbo.fundstransfer.daifucomponent.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.daifucomponent.dto.input.DaifuConfigSyncReqParam;
import com.xinbo.fundstransfer.daifucomponent.dto.input.DaifuConfigSyncReqParamConfig;
import com.xinbo.fundstransfer.daifucomponent.dto.input.DaifuConfigSyncReqParamLevelConfig;
import com.xinbo.fundstransfer.daifucomponent.dto.output.OutConfigDTO;
import com.xinbo.fundstransfer.daifucomponent.entity.DaifuConfigRequest;
import com.xinbo.fundstransfer.daifucomponent.service.DaifuConfigRequestService;
import com.xinbo.fundstransfer.domain.DynamicSpecifications;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.GeneralResponseData.ResponseStatus;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.SearchFilter.Operator;
import com.xinbo.fundstransfer.domain.SimpleResponseData;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.restful.BaseController;
import com.xinbo.fundstransfer.service.HandicapService;
import com.xinbo.fundstransfer.service.SysDataPermissionService;
import com.xinbo.fundstransfer.utils.randutil.JedisLock;

import lombok.extern.slf4j.Slf4j;

/**
 * 平台代付通道同步接口
 *
 * @author blake
 *
 */
@Slf4j
@RestController
@RequestMapping("/passage")
public class DaifuConfigController extends BaseController {
	private static final Logger logger = LoggerFactory.getLogger(DaifuConfigController.class);

	@Autowired
	private HandicapService handicapService;
	@Autowired
	private SysDataPermissionService dataPermissionService;
	@Autowired
	private DaifuConfigRequestService daifuConfigRequestService;

	/**
	 * 代付通道信息同步分布式锁<br>
	 * PASSAGE_SYNC_DISTRIBUTE_LOCK_盘口号_提供商_商号
	 */
	private static final String PASSAGE_SYNC_DISTRIBUTE_LOCK = "PASSAGE_SYNC_DISTRIBUTE_LOCK_%s_%s_%s";

	@RequestMapping("/list")
	public String list(@RequestParam(value = "pageNo") Integer pageNo,
					   @RequestParam(value = "pageSize", required = false) Integer pageSize) throws JsonProcessingException {
		String params = buildParams().toString();
		logger.trace(String.format("%s，参数：%s", "账号分页获取", params));
		try {
			GeneralResponseData<List<OutConfigDTO>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser == null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆");
				return mapper.writeValueAsString(responseData);
			}
			PageRequest pageRequest = PageRequest.of(pageNo, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					Sort.Direction.DESC, "syncTime");
			// 查询条件
			List<SearchFilter> filterToList = DynamicSpecifications.build(request);
			//盘口权限
			List<BizHandicap> dataToList = dataPermissionService.getOnlyHandicapByUserId(sysUser);
			if (CollectionUtils.isEmpty(dataToList)) {
				return mapper.writeValueAsString(new GeneralResponseData<>(
						GeneralResponseData.ResponseStatus.FAIL.getValue(), "当前用户未配置盘口权限，请配置后再进行查询！"));
			} else {
				List<Integer> handicapIdToList = new ArrayList<Integer>();
				for (int i = 0; i < dataToList.size(); i++) {
					if (null != dataToList.get(i) && null != dataToList.get(i).getId()) {
						handicapIdToList.add(dataToList.get(i).getId());
					}
				}
				if (CollectionUtils.isEmpty(handicapIdToList)) {
					return mapper.writeValueAsString(responseData);
				} else {
					filterToList
							.add(new SearchFilter("handicapId", SearchFilter.Operator.IN, handicapIdToList.toArray()));
				}
			}
			//不包含平台已经删除的
			SearchFilter notPlatformDeleteFlagFilter = new SearchFilter("platformDeleteFlag", Operator.NOTEQ, new Byte("1"));
			filterToList.add(notPlatformDeleteFlagFilter);

			SearchFilter[] filterToArray = filterToList.toArray(new SearchFilter[filterToList.size()]);
			Specification<DaifuConfigRequest> specif = DynamicSpecifications.build(DaifuConfigRequest.class,
					filterToArray);
			List<OutConfigDTO> data = new ArrayList<>();
			// 执行查询
			Page<DaifuConfigRequest> page = daifuConfigRequestService.findPage(specif,pageRequest);
			//组装数据返回
			if(!CollectionUtils.isEmpty(page.getContent())) {
				List<Integer> outConfigIdList = page.getContent().stream().map(DaifuConfigRequest::getId).collect(Collectors.toList());

				//查询统计值
				Map<Integer,OutConfigDTO> outConfigStaticMap = new HashMap<>();
				List<OutConfigDTO> outConfigStatic = daifuConfigRequestService.findStaticByIdList(outConfigIdList);
				if(!CollectionUtils.isEmpty(outConfigStatic)) {
					outConfigStatic.stream().forEach(t->outConfigStaticMap.put(t.getId(), t));
				}

				Map<Integer,String> handicapNameMap = new HashMap<>();
				//查询盘口名称
				List<BizHandicap> handicapList = handicapService.findAllToList();
				if(!CollectionUtils.isEmpty(handicapList)) {
					handicapList.stream().forEach(t->handicapNameMap.put(t.getId(), t.getName()));
				}

				for(DaifuConfigRequest t:page.getContent()) {
					OutConfigDTO e = new OutConfigDTO();
					BeanUtils.copyProperties(t, e);
					OutConfigDTO tmp = outConfigStaticMap.get(t.getId());
					if(!ObjectUtils.isEmpty(tmp)) {
						e.setCountSuccess(tmp.getCountSuccess());
						e.setCountError(tmp.getCountError());
						e.setCountPaying(tmp.getCountPaying());
					}
					if(ObjectUtils.isEmpty(e.getCountError())) {
						e.setCountError(0);
					}
					if(ObjectUtils.isEmpty(e.getCountSuccess())) {
						e.setCountSuccess(0);
					}
					if(ObjectUtils.isEmpty(e.getCountPaying())) {
						e.setCountPaying(0);
					}
					//盘口名称
					e.setHandicapName(handicapNameMap.get(e.getHandicapId()));
					//使用的层级名称
					if(!StringUtils.isEmpty(t.getLevelConfig())) {
						Map<String,DaifuConfigSyncReqParamLevelConfig> levelUsingMap = mapper.readValue(t.getLevelConfig(), mapper.getTypeFactory().constructMapType(HashMap.class, String.class, DaifuConfigSyncReqParamLevelConfig.class));
						if(!ObjectUtils.isEmpty(levelUsingMap)) {
							e.setLevelNameList(new ArrayList<>());
							levelUsingMap.values().stream().forEach(o->{
								e.getLevelNameList().add(o.getLevelName());
							});
						}
					}

					//使用的银行名称
					if(!StringUtils.isEmpty(t.getBankConfig())) {
						Map<String,String> bankUsingMap = mapper.readValue(t.getBankConfig(), mapper.getTypeFactory().constructMapType(HashMap.class, String.class, String.class));
						if(!ObjectUtils.isEmpty(bankUsingMap)) {
							e.setBankNameList(new ArrayList<>());
							e.getBankNameList().addAll(bankUsingMap.keySet());
						}
					}
					data.add(e);
				}
			}
			responseData.setData(data);
			responseData.setPage(new Paging(page));
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error(String.format("%s，参数：%s，结果：%s", "账号分页获取", params, e.getMessage()));
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "操作失败  " + e.getLocalizedMessage()));
		}

	}

	@RequestMapping("/sync")
	public String put(@RequestBody String bodyJson) throws JsonProcessingException {
		log.debug("平台请求同步第三方出款通道信息，传递参数：{}", bodyJson);
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
		try {
			DaifuConfigSyncReqParam entity = mapper.readValue(bodyJson, DaifuConfigSyncReqParam.class);
			StringBuffer errorMsg = new StringBuffer();
			if (ObjectUtils.isEmpty(entity.getHandicap())) {
				errorMsg.append("handicap不能为空，");
			}
			if (ObjectUtils.isEmpty(entity.getChannelName())) {
				errorMsg.append("channelName不能为空，");
			}
			if (ObjectUtils.isEmpty(entity.getMemberId())) {
				errorMsg.append("memberId不能为空，");
			}
			if (ObjectUtils.isEmpty(entity.getPrivateKey())) {
				errorMsg.append("privateKey不能为空，");
			}
			if (entity.getPublicKey() == null) {
				errorMsg.append("publicKey不能为空，");
			}
			if (!((!ObjectUtils.isEmpty(entity.getStatus()))
					|| DaifuConfigSyncReqParam.Status.Active.getValue().equals(entity.getStatus())
					|| DaifuConfigSyncReqParam.Status.Disable.getValue().equals(entity.getStatus())
					|| DaifuConfigSyncReqParam.Status.Delete.getValue().equals(entity.getStatus()))) {
				errorMsg.append("status不能为空或者值不正确，");
			}
			if(ObjectUtils.isEmpty(entity.getThirdId())) {
				errorMsg.append("thirdId不能为空，");
			}
			if(ObjectUtils.isEmpty(entity.getAisleName())) {
				errorMsg.append("aisleName不能为空，");
			}
			if (ObjectUtils.isEmpty(entity.getOutMoney())) {
				errorMsg.append("outMoney不能为空，");
			}
			if (ObjectUtils.isEmpty(entity.getOutTimes())) {
				errorMsg.append("outTimes不能为空，");
			}
			if (entity.getSupportBankConfig() == null) {
				errorMsg.append("supportBankConfig不能为空，");
			}
			if (entity.getSupportLevelConfig() == null) {
				errorMsg.append("supportLevelConfig不能为空，");
			}
			if (ObjectUtils.isEmpty(entity.getFeeConfig())) {
				errorMsg.append("feeConfig不能为空，");
			}
			if (ObjectUtils.isEmpty(entity.getOutConfigSet())) {
				errorMsg.append("outConfigSet不能为空，");
			}
			if (ObjectUtils.isEmpty(entity.getWarnConfig())) {
				errorMsg.append("warnConfig不能为空，");
			}

			if (errorMsg.length() > 0) {
				return mapper.writeValueAsString(new SimpleResponseData(
						GeneralResponseData.ResponseStatus.FAIL.getValue(), errorMsg.toString()));
			}
			BizHandicap bizHandicap = handicapService.findFromCacheByCode(entity.getHandicap());
			if (null == bizHandicap) {
				log.info("{} 盘口不存在", entity.getHandicap());
				return mapper.writeValueAsString(
						new SimpleResponseData(ResponseStatus.FAIL.getValue(), "Failure, handicap does not exist."));
			}

			DaifuConfigRequest req = new DaifuConfigRequest();
			if (!ObjectUtils.isEmpty(entity.getSupportBankConfig())) {
				req.setBankConfig(mapper.writeValueAsString(entity.getSupportBankConfig()));
			} else {
				req.setBankConfig(mapper.writeValueAsString(new HashMap<>()));
			}
			req.setThirdId(entity.getThirdId());
			req.setChannelName(entity.getChannelName());
			DaifuConfigSyncReqParamConfig config = new DaifuConfigSyncReqParamConfig();
			config.setNotifyUrl(entity.getNotifyUrl());
			config.setPrivateKey(entity.getPrivateKey());
			config.setPublicKey(entity.getPublicKey());
			config.setStoreMaxMoney(entity.getStoreMaxMoney());
			config.setStoreMinMoney(entity.getStoreMinMoney());
			config.setStoreStopMoney(entity.getStoreStopMoney());
			req.setConfig(mapper.writeValueAsString(config));
			req.setFeeConfig(entity.getFeeConfig().toString());
			req.setHandicapId(bizHandicap.getId());
			req.setLevelConfig(mapper.writeValueAsString(entity.getSupportLevelConfig()));
			req.setMemberId(entity.getMemberId());
			req.setAliasName(entity.getAisleName());
			req.setOutConfigSet(mapper.writeValueAsString(entity.getOutConfigSet()));
			req.setPlatformOutMoney(entity.getOutMoney());
			req.setPlatformOutTimes(entity.getOutTimes());
			//平台执行清空累计值时，设置对应值为 0
			if(BigDecimal.ZERO.equals(entity.getOutMoney())
					&& entity.getOutTimes()==0) {
				req.setCrkOutMoney(BigDecimal.ZERO);
				req.setCrkOutTimes(0);
			}
			if (DaifuConfigSyncReqParam.Status.Delete.getValue().equals(entity.getStatus())) {
				req.setPlatformStatus(Byte.parseByte("0"));
				req.setPlatformDeleteFlag(Byte.parseByte("1"));
			} else if (DaifuConfigSyncReqParam.Status.Active.getValue().equals(entity.getStatus())) {
				req.setPlatformDeleteFlag(Byte.parseByte("0"));
				req.setPlatformStatus(Byte.parseByte("1"));
			} else {
				req.setPlatformDeleteFlag(Byte.parseByte("0"));
				req.setPlatformStatus(Byte.parseByte("0"));
			}
			req.setWarnConfig(entity.getWarnConfig().toString());
			StringRedisTemplate jedis = redisService.getStringRedisTemplate();
			if (jedis != null) {
				JedisLock lock = new JedisLock(jedis, String.format(PASSAGE_SYNC_DISTRIBUTE_LOCK,
						req.getHandicapId(), req.getChannelName(), req.getMemberId()), 3000, 5000);
				if (lock.acquire()) {
					// 进行新增 或者 更新操作。判断唯一标准是 盘口号 + 提供商名称 + 商号
					daifuConfigRequestService.saveOrUpdate(req);
					return mapper.writeValueAsString(
							new SimpleResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue()));
				} else {
					log.error("同步业主代付配置，获取redisLock失败");
					throw new RuntimeException("获取锁失败，请重试");
				}
			} else {
				throw new RuntimeException("获取redis失败");
			}

		} catch (Exception e) {
			log.error("出款通道信息同步时产生异常. >>", e);
			return mapper.writeValueAsString(
					new SimpleResponseData(ResponseStatus.FAIL.getValue(), "Failure, " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 出款通道列表-查询通道余额
	 * @return
	 */
	@RequestMapping(value = "/getDaifuBalance", method = RequestMethod.GET)
	@ResponseBody
	public GeneralResponseData<BigDecimal> getDaifuBalance(@RequestParam(value = "handicapId") Integer handicapId,
														   @RequestParam(value = "outConfigId") Integer outConfigId){
		GeneralResponseData<BigDecimal> result = new GeneralResponseData<>(-1);
		try {
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser == null) {
				result = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "请重新登陆");
				return result;
			}
			BigDecimal balance = daifuConfigRequestService.getBalaceFromPayCore(handicapId,outConfigId);
			if(!ObjectUtils.isEmpty(balance)) {
				result.setData(balance);
				result.setStatus(1);
			}
		}catch (Exception e) {
			result.setStatus(-1);
			log.error("获取出款通道第三方余额失败",e);
			result.setMessage(e.getMessage());
		}
		return result;
	}

}