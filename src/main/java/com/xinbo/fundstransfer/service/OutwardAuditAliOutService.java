package com.xinbo.fundstransfer.service;

import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public interface OutwardAuditAliOutService {

	/**
	 * 获取正在匹配的支付宝入款单
	 * @param pageRequest 分页对象
	 * @param handicap 盘口
	 * @param level 层级
	 * @param incomeMember 入款会员
	 * @param incomeOrder 入款单号
	 * @param timeStart 入款提单时间开始
	 * @param timeEnd 入款提单时间结束
	 * @return
	 */
	Map<String,Object> aliOutToMatch(PageRequest pageRequest, Integer handicap, Integer level, String incomeMember,String incomeOrder,String timeStart,String timeEnd);

	/**
	 * 获取失败的支付宝入款单
	 * @param pageRequest 分页对象
	 * @param handicap 盘口
	 * @param level 层级
	 * @param incomeMember 入款会员
	 * @param incomeOrder 入款单号
	 * @param timeStart 入款提单时间开始
	 * @param timeEnd 入款提单时间结束
	 * @return
	 */
	Map<String,Object> aliOutFail(PageRequest pageRequest, Integer handicap, Integer level, String incomeMember,String incomeOrder,String timeStart,String timeEnd);
	
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
	Map<String,Object> aliOutSuccess(PageRequest pageRequest, Integer handicap, Integer level, String member,String toMember,String inOrderNo,String outOrderNo,Integer toHandicapRadio,String timeStart,String timeEnd);
	
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
	Map<String,Object> aliOutMatched(PageRequest pageRequest, Integer handicap, Integer level, String member,String toMember,String inOrderNo,String outOrderNo,Integer toHandicapRadio,String timeStart,String timeEnd);

}
