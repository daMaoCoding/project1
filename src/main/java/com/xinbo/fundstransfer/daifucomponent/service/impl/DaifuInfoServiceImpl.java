/**
 * 
 */
package com.xinbo.fundstransfer.daifucomponent.service.impl;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.daifucomponent.dto.input.DaifuInfoFindColInputDTO;
import com.xinbo.fundstransfer.daifucomponent.dto.output.DaifuInfoDTO;
import com.xinbo.fundstransfer.daifucomponent.dto.output.DaifuResult.ResultEnum;
import com.xinbo.fundstransfer.daifucomponent.entity.DaifuInfo;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.repository.DaifuInfoRepository;
import com.xinbo.fundstransfer.service.OutwardTaskService;
import com.xinbo.fundstransfer.service.SysUserService;

/**
 * @author blake
 *
 */
@Component
public class DaifuInfoServiceImpl {

	@Autowired
	private DaifuInfoRepository daifuInfoDao;
	
	@Autowired
	private OutwardTaskService outwardTaskService;
	
	@Autowired
	private SysUserService userService;
	/**
	 * 查询通道的代付订单信息
	 * @param inputDTO
	 * @return
	 */
	public GeneralResponseData<List<DaifuInfoDTO>> findByOutConfigId(DaifuInfoFindColInputDTO inputDTO) {
		//排序方式,确定以更新时间倒序排列。
		//因为出款任务表中没有记录代付信息的信息，无法确定哪些出款任务是使用了这个出款通道，所有这里无法使用出款任务表的时间来排序
		Sort sort = new Sort(Direction.DESC, "uptime");
		Pageable pageable = new PageRequest(inputDTO.getPageNo(),
				(inputDTO.getPageSize() == null || inputDTO.getPageSize() <= 0) ? AppConstants.PAGE_SIZE
						: inputDTO.getPageSize(),
				sort);
		
		Page<DaifuInfo> page = daifuInfoDao.findAll((root, criteriaQuery, criteriaBuilder) -> {
			List<Predicate> predicateList = new ArrayList<>();
			Path<Integer> handicapPath = root.get("handicapId");
			Path<Integer> daifuConfigIdPath = root.get("daifuConfigId");
			Predicate p1 = criteriaBuilder.equal(handicapPath, inputDTO.getHandicapId());
			predicateList.add(p1);
			Predicate p2 = criteriaBuilder.equal(daifuConfigIdPath, inputDTO.getOutConfigId());
			predicateList.add(p2);
			
			if(!ObjectUtils.isEmpty(inputDTO.getCreateTimeBegin())) {
				Path<Timestamp> createTimePath = root.get("createTime");
				Predicate createTimeBegin = criteriaBuilder.greaterThanOrEqualTo(createTimePath, inputDTO.getCreateTimeBegin());
				predicateList.add(createTimeBegin);
			}
			
			if(!ObjectUtils.isEmpty(inputDTO.getCreateTimeEnd())) {
				Path<Timestamp> createTimePath = root.get("createTime");
				Predicate createTimeEnd = criteriaBuilder.lessThanOrEqualTo(createTimePath, inputDTO.getCreateTimeEnd());
				predicateList.add(createTimeEnd);
			}
			
			if(!ObjectUtils.isEmpty(inputDTO.getPlatStatus())) {
				Path<Byte> platStatusPath = root.get("platStatus");
				//对于页面而言：0-未知 3-正在支付 都属于待处理。1-完成  2-取消
				//所以这里当传递进来 0时，进行 plat_status = 0 或者 3的判断
				if(ResultEnum.UNKOWN.getValue().equals(inputDTO.getPlatStatus())) {
					Predicate platStatus0 = criteriaBuilder.equal(platStatusPath, ResultEnum.UNKOWN.getValue());
					Predicate platStatus3 = criteriaBuilder.equal(platStatusPath, ResultEnum.PAYING.getValue());
					Predicate platStatus = criteriaBuilder.or(platStatus0,platStatus3);
					predicateList.add(platStatus);
				}else {
					Predicate platStatus = criteriaBuilder.equal(platStatusPath, inputDTO.getPlatStatus());
					predicateList.add(platStatus);
				}
			}
			
			if(!ObjectUtils.isEmpty(inputDTO.getExactMoneyBegin())) {
				Path<BigDecimal> exactMoneyPath = root.get("exactMoney");
				Predicate exactMoneyBegin = criteriaBuilder.greaterThanOrEqualTo(exactMoneyPath, inputDTO.getExactMoneyBegin());
				predicateList.add(exactMoneyBegin);
			}
			
			if(!ObjectUtils.isEmpty(inputDTO.getExactMoneyEnd())) {
				Path<BigDecimal> exactMoneyPath = root.get("exactMoney");
				Predicate exactMoneyEnd = criteriaBuilder.lessThanOrEqualTo(exactMoneyPath, inputDTO.getExactMoneyEnd());
				predicateList.add(exactMoneyEnd);
			}
			
			if(!ObjectUtils.isEmpty(inputDTO.getOutwardRequestOrderNo())) {
				Path<String> outwardRequestOrderNoPath = root.get("outwardRequestOrderNo");
				Predicate outwardRequestOrderNo = criteriaBuilder.equal(outwardRequestOrderNoPath, inputDTO.getOutwardRequestOrderNo());
				predicateList.add(outwardRequestOrderNo);
			}
			if(!ObjectUtils.isEmpty(inputDTO.getPlatPayCode())) {
				Path<String> platPayCodePath = root.get("platPayCode");
				Predicate platOutCode = criteriaBuilder.equal(platPayCodePath, inputDTO.getPlatPayCode());
				predicateList.add(platOutCode);
			}
			criteriaQuery.where(predicateList.toArray(new Predicate[predicateList.size()]));
			return null;
		}, pageable);
		
		GeneralResponseData<List<DaifuInfoDTO>> result = new GeneralResponseData<List<DaifuInfoDTO>>();
		List<DaifuInfoDTO> resultList = new ArrayList<>();
		if(!CollectionUtils.isEmpty(page.getContent())) {
			//查询出款任务当前状态 和 备注
			List<Long> taskIdList = page.getContent().stream().map(DaifuInfo::getOutwardTaskId).collect(Collectors.toList());
			Map<Long, Object[]> taskStatusRemarkMap = outwardTaskService.findStatusRemarkByTaskId(taskIdList);
			//最近操作人id ，用于查找操作人uid
			List<Integer> operatorIdList = new ArrayList<Integer>();
			for(DaifuInfo info:page.getContent()) {
				info.setRemark(null);
				DaifuInfoDTO e = new DaifuInfoDTO();
				BeanUtils.copyProperties(info, e);
				if(!ObjectUtils.isEmpty(taskStatusRemarkMap.get(info.getOutwardTaskId()))) {
					Object[] statusRemark = taskStatusRemarkMap.get(info.getOutwardTaskId());
					e.setStatus(Byte.parseByte(statusRemark[0].toString()));
					e.setRemark(statusRemark[1].toString());
					//最近操作人，如果出款任务获取到的操作人为空，取代付信息表的 createAdminId
					if(statusRemark[2]!=null) {
						e.setOperator(Integer.parseInt(statusRemark[2].toString()));
					}else {
						e.setOperator(info.getCreateAdminId());
					}
				}
				operatorIdList.add(e.getOperator());
				resultList.add(e);
			}
			Map<Integer,String> operatorUidMap = new HashMap<Integer, String>();
			for(Integer operator:operatorIdList) {
				SysUser tmp = userService.findFromCacheById(operator);
				if(!ObjectUtils.isEmpty(tmp)) {
					operatorUidMap.put(operator, tmp.getUid());
				}
			}
			for(DaifuInfoDTO e: resultList) {
				e.setOperatorUid(operatorUidMap.get(e.getOperator()));
			}
			
		}
		result.setData(resultList);
		result.setStatus(1);
		result.setPage(new Paging(page));
		return result;
	}
	
	/**
	 * 根据出款任务查询代付订单信息 <br>
	 * 创建时间倒序排列。
	 * @param handicapId
	 * @param outwardTaskId
	 * @return 出款任务的代付订单列表，当无对应outwardTaskId的代付订单时，返回 size =0的列表 
	 */
	public List<DaifuInfo> getDaifuInfoByOutwardTaskId(Integer handicapId,Long outwardTaskId){
		Sort sort = new Sort(Direction.DESC, "createTime");
		List<DaifuInfo> result = daifuInfoDao.findAll((root, criteriaQuery, criteriaBuilder) -> {
			List<Predicate> predicateList = new ArrayList<>();
			Path<Integer> handicapPath = root.get("handicapId");
			Path<Long> outwardTaskIdPath = root.get("outwardTaskId");
			Predicate p1 = criteriaBuilder.equal(handicapPath, handicapId);
			predicateList.add(p1);
			Predicate p2 = criteriaBuilder.equal(outwardTaskIdPath, outwardTaskId);
			predicateList.add(p2);
			criteriaQuery.where(predicateList.toArray(new Predicate[predicateList.size()]));
			return null;
		}, sort);
		return result;
	} 
}
