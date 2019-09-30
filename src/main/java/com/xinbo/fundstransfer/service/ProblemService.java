package com.xinbo.fundstransfer.service;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.xinbo.fundstransfer.domain.pojo.DeviceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.xinbo.fundstransfer.domain.entity.SysUser;

public interface ProblemService {
	/**
	 * 工具端上报设备状态处理
	 *
	 * @param dsStr
	 */
	void reportDeviceStatus(String dsStr);

	boolean lock(String mobile,SysUser operator,String other,String id);

	void unlock(String mobile,SysUser operator);

	void deal(String mobile,SysUser operator,String remark);

	Map<String,String> getDeviceByMobile(String mobile);

	Page<List> getProblemInfoList(SysUser user, PageRequest pageRequest,List<Predicate<DeviceStatus>> rules);

	Map<String,String> getContractInfo(String mobile);

	DeviceStatus getDeviceStatusByMobile(String mobile);

	void deleteDeviceStatus(String mobile);

	void reportErrorMsg(String id,String errMsg);
}
