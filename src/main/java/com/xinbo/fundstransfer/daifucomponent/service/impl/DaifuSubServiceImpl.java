/**
 * 
 */
package com.xinbo.fundstransfer.daifucomponent.service.impl;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.xinbo.fundstransfer.daifucomponent.dto.input.DaifuConfirmReqParamTo;
import com.xinbo.fundstransfer.daifucomponent.dto.output.DaifuResult;
import com.xinbo.fundstransfer.daifucomponent.dto.output.DaifuResult.ResultEnum;
import com.xinbo.fundstransfer.daifucomponent.entity.DaifuInfo;
import com.xinbo.fundstransfer.daifucomponent.exception.PayPlatNotCallAgainException;
import com.xinbo.fundstransfer.daifucomponent.service.Outward4DaifuService;
import com.xinbo.fundstransfer.daifucomponent.util.DaifuCacheUtil;
import com.xinbo.fundstransfer.domain.enums.OutwardTaskStatus;
import com.xinbo.fundstransfer.domain.repository.DaifuConfigRequestRepository;
import com.xinbo.fundstransfer.domain.repository.DaifuInfoRepository;
import com.xinbo.fundstransfer.service.OutwardTaskService;

import lombok.extern.slf4j.Slf4j;


/**
 * 第三方代付根据状态处理服务
 * @author blake
 *
 */
@Slf4j
@Service("daifuSubService")
public class DaifuSubServiceImpl {
	@Autowired
	private DaifuInfoRepository daifuInfoDao;
	
	@Autowired
	private Outward4DaifuService outward4DaifuService;
	
	@Autowired
	private DaifuConfigRequestRepository daifuConfigRequestDao;
	
	@Autowired
	private DaifuCacheUtil daifuCacheUtil;
	
	@Autowired
	private OutwardTaskService outwardTaskService;
	
	/**
	 * 通知 andrew.w 代付订单处理结果
	 * @param daifuInfo
	 */
	public void toNotify(DaifuInfo daifuInfo) {
		log.debug("获得出款任务id{}代付订单{}的结果{}",daifuInfo.getOutwardTaskId(),daifuInfo.getPlatPayCode(),daifuInfo.getPlatStatus());
		if(ObjectUtils.isEmpty(daifuInfo)) {
			return;
		}
		//当出款单已经完成出款或者取消出款，此时收到代付结果不应该再进行通知
		//否则有可能造成将已经完成的订单通知待排查
		Map<Long, Object[]> outwardTaskStatusMap = outwardTaskService.findStatusRemarkByTaskId(Arrays.asList(daifuInfo.getOutwardTaskId()));
		if(!ObjectUtils.isEmpty(outwardTaskStatusMap) 
				&& !ObjectUtils.isEmpty(outwardTaskStatusMap.get(daifuInfo.getOutwardTaskId()))) {
			Object[] outwardTaskStatus = outwardTaskStatusMap.get(daifuInfo.getOutwardTaskId());
			Integer status = (Integer.parseInt(outwardTaskStatus[0].toString()));
			if(OutwardTaskStatus.Deposited.getStatus().equals(status)
					|| OutwardTaskStatus.ManageCancel.getStatus().equals(status)) {
				log.debug("获得出款任务id{}的对应出款单{}目前状态为{},不再进行通知 outward4DaifuService.doAsDaifuResult",
						daifuInfo.getOutwardTaskId(),daifuInfo.getOutwardRequestOrderNo(),status,daifuInfo.getPlatStatus());
				return;
			}
		}
		
		//如果出款单存在了新的代付订单，则不将旧的代付订单结果通知 outward
		List<DaifuInfo> newDaifuInfoList = daifuInfoDao.findAll((root,criteriaQuery,criteriaBuilder)->{
			List<Predicate> predicateList = new ArrayList<>();
			Path<Integer> handicapPath = root.get("handicapId");
			Path<Long> outwardRequestOrderNoPath = root.get("outwardRequestOrderNo");
			Path<Timestamp> createTimePath = root.get("createTime");
			Predicate p1 = criteriaBuilder.equal(handicapPath, daifuInfo.getHandicapId());
			predicateList.add(p1);
			Predicate p2 = criteriaBuilder.equal(outwardRequestOrderNoPath, daifuInfo.getOutwardRequestOrderNo());
			predicateList.add(p2);
			Predicate p3 = criteriaBuilder.greaterThan(createTimePath, daifuInfo.getCreateTime());
			predicateList.add(p3);
			criteriaQuery.where(predicateList.toArray(new Predicate[predicateList.size()]));
			return null;
		});
		if(!CollectionUtils.isEmpty(newDaifuInfoList)) {
			log.debug("存在新的代付订单，本次代付结果将不通知outward");
		}else {
			log.debug("异步通知outward代付结果");
			DaifuResult result = daifuInfo.toResult4Outward();
			if(isToIntervene(result, daifuInfo)) {
				log.info("toNotify 设置转排查,盘口{} 代付订单：{} 对应出款单：{}",daifuInfo.getHandicapId(),daifuInfo.getPlatPayCode(),daifuInfo.getOutwardRequestOrderNo());
				result.setResult(ResultEnum.TO_INTERVENE);
			}
			new Thread(new Runnable() {
				@Override
				public void run() {
					outward4DaifuService.doAsDaifuResult(result);
				}
			}).start();
		}
	}

	/**
	 * 判断是否转排查
	 * @param result
	 * @return
	 */
	public boolean isToIntervene(DaifuResult result,DaifuInfo daifuInfo) {
		if((ResultEnum.UNKOWN.equals(result.getResult())||ResultEnum.PAYING.equals(result.getResult()))
				&& (intervene(daifuInfo.getCreateTime()))) {
			return true;
		}
		return false;
	}
	
	private boolean intervene(Timestamp createTime) {
		if(ObjectUtils.isEmpty(createTime)) {
			return true;
		}else {
			Calendar now = Calendar.getInstance();
			int timeMinute = daifuCacheUtil.getDaifu2InterveneTime();
			log.info("配置代付订单转排查时间为：{}分钟",timeMinute);
			now.add(Calendar.MINUTE,- timeMinute);
			return createTime.before(now.getTime());
		}
	}
	
	/**
	 * 支付平台代付失败，取消出入款系统代付订单
	 * @param param
	 * @param daifuInfo
	 * @throws PayPlatNotCallAgainException 
	 */
	public void doCancel(DaifuConfirmReqParamTo param, DaifuInfo daifuInfo) throws PayPlatNotCallAgainException {
		this.innerDoCancel(param, daifuInfo);
		this.toNotify(daifuInfo);
	}
	
	private void innerDoCancel(DaifuConfirmReqParamTo param, DaifuInfo daifuInfo) throws PayPlatNotCallAgainException {
		String errorMsg =getErrorMsgFromResMsg(param.getResponseDaifuErrorMsg());
		String error = "订单状态："+param.getResponseOrderState()+", 错误原因："+errorMsg;
		daifuInfo.setPlatStatus(DaifuResult.ResultEnum.ERROR.getValue());
		daifuInfo.setErrorMsg(error);
		int count = daifuInfoDao.setPlatStatusCancel(daifuInfo.getHandicapId(),daifuInfo.getId(),error);
		if(count!=1){
			throw new PayPlatNotCallAgainException("订单当前状态不允许再进行操作");
		}
	}
	
	public String getErrorMsgFromResMsg(String responseDaifuErrorMsg) {
		String regex = "([\u4e00-\u9fa5]+)";
		String str = "";
		Matcher matcher = Pattern.compile(regex).matcher(StringUtils.isEmpty(responseDaifuErrorMsg)?"":responseDaifuErrorMsg);
		while (matcher.find()) {
			str+= matcher.group(0);
		}
		String errorMsg = StringUtils.isEmpty(str)?"未知":str;
		return errorMsg;
	}
	

	/**
	 * 设置代付订单状态为3-正在处理
	 * @param daifuInfo
	 */
	public void doPaying(DaifuInfo daifuInfo) {
		this.innerDoPaying(daifuInfo);
		this.toNotify(daifuInfo);
	}
	private void innerDoPaying(DaifuInfo daifuInfo) {
		daifuInfo.setPlatStatus(DaifuResult.ResultEnum.PAYING.getValue());
		int count = daifuInfoDao.setPlatStatusPaying(daifuInfo.getHandicapId(),daifuInfo.getId());
		if(count !=1){
			throw new RuntimeException("支付平台通知出款单正在处理，更新第三方出款单状态时异常");
		}
	}

	/**
	 * 设置代付订单状态为处理完成并且更新代付通道出款累计值
	 * @param param
	 * @param daifuInfo
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
	public void doSuccess(DaifuConfirmReqParamTo param, DaifuInfo daifuInfo) {
		this.innerDoSuccess(param, daifuInfo);
		//通知处理结果。防止在通知过程中产生异常造成数据回滚，使用try-catch
		try {
			this.toNotify(daifuInfo);
		}catch (Exception e) {
			log.error("异步通知 outward代付结果时异常",e);
		}
	}
	private void innerDoSuccess(DaifuConfirmReqParamTo param, DaifuInfo daifuInfo) {
		daifuInfo.setPlatStatus(DaifuResult.ResultEnum.SUCCESS.getValue());
		int count = daifuInfoDao.setPlatStatusDone(daifuInfo.getHandicapId(),daifuInfo.getId());
		if(count !=1){
			throw new RuntimeException("支付平台通知出款单正在处理，更新第三方出款单状态时异常");
		}
		//增加通道出款累计值
		daifuConfigRequestDao.addStaticByDaifuSuccess(daifuInfo.getHandicapId(),daifuInfo.getDaifuConfigId(),daifuInfo.getExactMoney());
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
	public void saveDaifuInfoBeforeSend2PayCore(DaifuInfo daifuInfo) {
		daifuInfoDao.saveAndFlush(daifuInfo);
	}
	
}
