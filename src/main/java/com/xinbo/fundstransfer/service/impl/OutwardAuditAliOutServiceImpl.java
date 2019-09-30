package com.xinbo.fundstransfer.service.impl;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.xinbo.fundstransfer.domain.repository.OutwardAuditAliOutRepository;
import com.xinbo.fundstransfer.service.OutwardAuditAliOutService;

@Service
public class OutwardAuditAliOutServiceImpl implements OutwardAuditAliOutService {
	static final Logger log = LoggerFactory.getLogger(OutwardAuditAliOutServiceImpl.class);
	@Autowired
	private OutwardAuditAliOutRepository outwardAuditAliOutRepository;
	

	/**
	 * 获取正在匹配的支付宝入款单
	 * @param pageRequest 分页对象
	 * @param handicap 盘口
	 * @param level 层级
	 * @param outMember 出款会员
	 * @param outOrder 出款单号
	 * @param timeStart 出款提单时间开始
	 * @param timeEnd 出款提单时间结束
	 * @return
	 */
	@Override
	public Map<String,Object> aliOutToMatch(PageRequest pageRequest, Integer handicap, Integer level, String outMember,
			String outOrder,String timeStart,String timeEnd) {
		Map<String,Object> map = new HashMap<String,Object>();
		Page<Object> dataToPage = outwardAuditAliOutRepository.aliOutToMatch(  handicap,  level,  outMember,
				outOrder, timeStart, timeEnd,pageRequest);
		Object[] o = outwardAuditAliOutRepository.totalAmountAliOutToMatch(  handicap,  level,  outMember,
				outOrder, timeStart, timeEnd);
		map.put("page", dataToPage);
		map.put("totalAmount", o[0]);
		return map;
	}
	
	/**
	 * 获取失败的支付宝入款单
	 * @param pageRequest 分页对象
	 * @param handicap 盘口
	 * @param level 层级
	 * @param outMember 出款会员
	 * @param outOrder 出款单号
	 * @param timeStart 出款提单时间开始
	 * @param timeEnd 出款提单时间结束
	 * @return
	 */
	@Override
	public Map<String,Object> aliOutFail(PageRequest pageRequest, Integer handicap, Integer level, String outMember,
			String outOrder,String timeStart,String timeEnd) {
		Map<String,Object> map = new HashMap<String,Object>();
		Page<Object> dataToPage = outwardAuditAliOutRepository.aliOutFail(  handicap,  level,  outMember,
				outOrder, timeStart, timeEnd,pageRequest);
		Object[] o = outwardAuditAliOutRepository.totalAmountAliOutFail(  handicap,  level,  outMember,
				outOrder, timeStart, timeEnd);
		map.put("page", dataToPage);
		map.put("totalAmount", o[0]);
		return map;
	}

	/**
	 * 获取成功的支付宝入款单
	 * @param pageRequest 分页对象
	 * @param handicap 盘口
	 * @param level 层级
	 * @param member 入款会员
	 * @param toMember 收款会员
	 * @param inOrderNo 入款单号
	 * @param outOrderNo 收款单号
	 * @param toHandicapRadio 收款类型：全部0，盘口1，返利网2
	 * @param timeStart 入款提单时间开始
	 * @param timeEnd 入款提单时间结束
	 * @return
	 */
	@Override
	public Map<String, Object> aliOutSuccess(PageRequest pageRequest, Integer handicap, Integer level, String member,
			String toMember, String inOrderNo, String outOrderNo, Integer toHandicapRadio, String timeStart,
			String timeEnd) {
		Map<String,Object> map = new HashMap<String,Object>();
		Page<Object> dataToPage = outwardAuditAliOutRepository.aliOutSuccess( handicap,  level,  member,
				toMember, inOrderNo,outOrderNo,toHandicapRadio,timeStart,timeEnd,pageRequest);
		Object[] totalAmount = outwardAuditAliOutRepository.totalAmountAliOutSuccess( handicap,  level,  member,
				toMember, inOrderNo,outOrderNo,toHandicapRadio,timeStart,timeEnd);
		Object[] totalToAmount = outwardAuditAliOutRepository.totalToAmountAliOutSuccess( handicap,  level,  member,
				toMember, inOrderNo,outOrderNo,toHandicapRadio,timeStart,timeEnd);
		map.put("page", dataToPage);
		map.put("totalAmount", totalAmount[0]);
		map.put("totalToAmount", totalToAmount[0]);
		return map;
	}

	/**
	 * 获取进行的支付宝入款单
	 * @param pageRequest 分页对象
	 * @param handicap 盘口
	 * @param level 层级
	 * @param member 入款会员
	 * @param toMember 收款会员
	 * @param inOrderNo 入款单号
	 * @param outOrderNo 收款单号
	 * @param toHandicapRadio 收款类型：全部0，盘口1，返利网2
	 * @param timeStart 入款提单时间开始
	 * @param timeEnd 入款提单时间结束
	 * @return
	 */
	@Override
	public Map<String, Object> aliOutMatched(PageRequest pageRequest, Integer handicap, Integer level, String member,
			String toMember, String inOrderNo, String outOrderNo, Integer toHandicapRadio, String timeStart,
			String timeEnd) {
		Map<String,Object> map = new HashMap<String,Object>();
		Page<Object> dataToPage = outwardAuditAliOutRepository.aliOutMatched( handicap,  level,  member,
				toMember, inOrderNo,outOrderNo,toHandicapRadio,timeStart, timeEnd,pageRequest);
		Object[] totalAmount = outwardAuditAliOutRepository.totalAmountAliOutMatched(handicap,  level,  member,
				toMember, inOrderNo,outOrderNo,toHandicapRadio,timeStart,timeEnd);
		Object[] totalToAmount = outwardAuditAliOutRepository.totalToAmountAliOutMatched(handicap,  level,  member,
				toMember, inOrderNo,outOrderNo,toHandicapRadio,timeStart,timeEnd);
		map.put("page", dataToPage);
		map.put("totalAmount", totalAmount[0]);
		map.put("totalToAmount", totalToAmount[0]);
		return map;
	}
	
}
