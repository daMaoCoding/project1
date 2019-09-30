package com.xinbo.fundstransfer.restful;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.domain.DynamicSpecifications;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.domain.SearchFilter;
import com.xinbo.fundstransfer.domain.entity.BizHost;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.HostService;

@RestController
@RequestMapping("/r/newhost")
public class HostController extends BaseController {
	private static final Logger logger = LoggerFactory.getLogger(HostController.class);
	@Autowired
	AccountService accountService;
	@Autowired
	HostService hostService;

	/**
	 * 根据查询条件筛选
	 * 
	 * @param seachStr
	 *            完整匹配：编号/IP 模糊匹配：主机名/账号
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/list")
	public String list(@RequestParam(value = "seachStr", required = false) String seachStr)
			throws JsonProcessingException {
		try {
			GeneralResponseData<List<BizHost>> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			if (StringUtils.isEmpty(seachStr)) {
				// 未输入查询条件
				PageRequest pageRequest = new PageRequest(0, AppConstants.PAGE_SIZE_MAX, Sort.Direction.ASC, "x", "y");
				List<SearchFilter> filterToList = DynamicSpecifications.build(request);
				SearchFilter[] filterToArray = filterToList.toArray(new SearchFilter[filterToList.size()]);
				Specification<BizHost> specif = DynamicSpecifications.build(BizHost.class, filterToArray);
				Page<BizHost> page = hostService.findPage(specif, pageRequest);
				responseData.setData(page.getContent());
				responseData.setPage(new Paging(page));
			} else {
				// 已输入查询条件
				responseData.setData(hostService.findList(seachStr));
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/findbyid")
	public String findById(@RequestParam(value = "id") Integer id) throws JsonProcessingException {
		try {
			GeneralResponseData<BizHost> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			BizHost bizHost = hostService.findById(id);
			responseData.setData(bizHost);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询失败  " + e.getLocalizedMessage()));
		}
	}

	/**
	 * 根据IP查询主机信息
	 * 
	 * @param id
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/findbyIP")
	public String findbyIP(@RequestParam(value = "ip") String ip) throws JsonProcessingException {
		try {
			GeneralResponseData<BizHost> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			BizHost bizHost = hostService.findByIP(ip);
			responseData.setData(bizHost);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/loadHostTotal")
	public String loadHostTotal() throws JsonProcessingException {
		try {
			GeneralResponseData<String[]> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			String[] totals = hostService.loadHostTotal();
			responseData.setData(totals);
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			return mapper.writeValueAsString(new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.FAIL.getValue(), "查询失败  " + e.getLocalizedMessage()));
		}
	}

	@RequestMapping("/create")
	public String create(@Valid BizHost bizHost) throws JsonProcessingException {
		String params = buildParams().toString();
		String errorInfo = "添加主机失败：";
		try {
			if (StringUtils.isEmpty(bizHost.getX()) || StringUtils.isEmpty(bizHost.getY())
					|| StringUtils.isEmpty(bizHost.getName()) || StringUtils.isEmpty(bizHost.getIp())) {
				log.error(errorInfo + "参数不能为空.");
				throw new Exception(errorInfo + "参数不能为空");
			}
			if (!CollectionUtils.isEmpty(hostService.findIdList(
					new SearchFilter("x", SearchFilter.Operator.EQ, StringUtils.trimToEmpty(bizHost.getX() + ",")),
					new SearchFilter("y", SearchFilter.Operator.EQ, StringUtils.trimToEmpty(bizHost.getY() + ","))))) {
				log.error(errorInfo + "坐标已存在！请重新调整排或列");
				throw new Exception(errorInfo + "坐标已存在！请重新调整排或列");
			}
			String[] hosts = bizHost.getHostInfo().split(",");
			if (hosts.length > 9) {
				log.error(errorInfo + "虚拟机IP数不可超过9个");
				throw new Exception(errorInfo + "虚拟机IP数不可超过9个");
			}
			// 比较当前录入的主机与IP是否重复
			if (isRepeat(hosts)) {
				log.error(errorInfo + "虚拟机有重复IP");
				throw new Exception(errorInfo + "虚拟机有重复IP");
			}
			if (Arrays.asList(hosts).contains(bizHost.getIp())) {
				log.error(errorInfo + "主机IP不可与虚拟机重复");
				throw new Exception(errorInfo + "主机IP不可与虚拟机重复");
			}
			// 主机IP在主机列和虚拟机列唯一校验
			if (!CollectionUtils.isEmpty(hostService.findIdList(
					new SearchFilter("ip", SearchFilter.Operator.EQ, StringUtils.trimToEmpty(bizHost.getIp()))))) {
				log.error(errorInfo + "主机IP已存在");
				throw new Exception(errorInfo + "主机IP已存在");
			}
			if (!CollectionUtils.isEmpty(hostService.findIdList(new SearchFilter("hostInfo", SearchFilter.Operator.LIKE,
					StringUtils.trimToEmpty(bizHost.getIp() + ","))))) {
				log.error(errorInfo + "主机IP已作为虚拟机存在");
				throw new Exception(errorInfo + "主机IP已作为虚拟机存在");
			}
			if (hosts.length > 0) {
				for (String temp : hosts) {
					if (StringUtils.isNotEmpty(temp)) {
						// 虚拟机IP在虚拟机列和虚拟机列唯一校验
						if (!CollectionUtils.isEmpty(hostService.findIdList(
								new SearchFilter("ip", SearchFilter.Operator.EQ, StringUtils.trimToEmpty(temp))))) {
							log.error(errorInfo + "虚拟机IP已作为主机存在");
							throw new Exception(errorInfo + "虚拟机IP已作为主机存在");
						}
						if (!CollectionUtils.isEmpty(hostService.findIdList(new SearchFilter("hostInfo",
								SearchFilter.Operator.LIKE, StringUtils.trimToEmpty(temp + ","))))) {
							log.error(errorInfo + "虚拟机IP已存在");
							throw new Exception(errorInfo + "虚拟机IP已存在");
						}
					}
				}
				// 虚拟机IP(最后一个IP也必须带英文逗号，检索用)
				String hostInfoStr = String.join(",", hosts) + ",";
				if (StringUtils.isNotEmpty(hostInfoStr) && !hostInfoStr.equals(",")) {
					bizHost.setHostInfo(hostInfoStr);
					bizHost.setHostNum(hosts.length + 1);
				} else {
					bizHost.setHostInfo(null);
					bizHost.setHostNum(1);
				}
			}
			hostService.saveAndFlush(bizHost);
			GeneralResponseData<BizHost> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error(String.format("%s，参数：%s，结果：%s", errorInfo, params, e.getMessage()));
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getMessage()));
		}

	}

	@RequestMapping("/update")
	public String update(@Valid BizHost bizHost) throws JsonProcessingException {
		String params = buildParams().toString();
		String errorInfo = "修改主机失败：";
		try {
			if (null == bizHost.getId() || StringUtils.isEmpty(bizHost.getX()) || StringUtils.isEmpty(bizHost.getY())
					|| StringUtils.isEmpty(bizHost.getName()) || StringUtils.isEmpty(bizHost.getIp())) {
				log.error(errorInfo + "参数不能为空.");
				throw new Exception(errorInfo + "参数不能为空");
			}
			List<Integer> searchBy_xy = hostService.findIdList(
					new SearchFilter("x", SearchFilter.Operator.EQ, StringUtils.trimToEmpty(bizHost.getX() + ",")),
					new SearchFilter("y", SearchFilter.Operator.EQ, StringUtils.trimToEmpty(bizHost.getY() + ",")));
			if (!CollectionUtils.isEmpty(searchBy_xy) && !searchBy_xy.get(0).equals(bizHost.getId())) {
				log.error(errorInfo + "坐标已存在！请重新调整排或列");
				throw new Exception(errorInfo + "坐标已存在！请重新调整排或列");
			}
			String[] hosts = bizHost.getHostInfo().split(",");
			if (hosts.length > 9) {
				log.error(errorInfo + "虚拟机IP数不可超过9个");
				throw new Exception(errorInfo + "虚拟机IP数不可超过9个");
			}
			// 比较当前录入的主机与IP是否重复
			if (isRepeat(hosts)) {
				log.error(errorInfo + "虚拟机有重复IP");
				throw new Exception(errorInfo + "虚拟机有重复IP");
			}
			if (Arrays.asList(hosts).contains(bizHost.getIp())) {
				log.error(errorInfo + "主机IP不可与虚拟机重复");
				throw new Exception(errorInfo + "主机IP不可与虚拟机重复");
			}
			// 主机IP在主机列和虚拟机列唯一校验
			List<Integer> searchBy_ip = hostService.findIdList(
					new SearchFilter("ip", SearchFilter.Operator.EQ, StringUtils.trimToEmpty(bizHost.getIp())));
			if (!CollectionUtils.isEmpty(searchBy_ip) && !searchBy_ip.get(0).equals(bizHost.getId())) {
				log.error(errorInfo + "主机IP已存在");
				throw new Exception(errorInfo + "主机IP已存在");
			}
			List<Integer> searchBy_hostInfo = hostService.findIdList(new SearchFilter("hostInfo",
					SearchFilter.Operator.LIKE, StringUtils.trimToEmpty(bizHost.getIp() + ",")));
			if (!CollectionUtils.isEmpty(searchBy_hostInfo)) {
				log.error(errorInfo + "主机IP已作为虚拟机存在");
				throw new Exception(errorInfo + "主机IP已作为虚拟机存在");
			}
			if (hosts.length > 0) {
				for (String temp : hosts) {
					if (StringUtils.isNotEmpty(temp)) {
						List<Integer> searchBy_temp_ip = hostService.findIdList(
								new SearchFilter("ip", SearchFilter.Operator.EQ, StringUtils.trimToEmpty(temp)));
						List<Integer> searchBy_temp_hostInfo = hostService.findIdList(new SearchFilter("hostInfo",
								SearchFilter.Operator.LIKE, StringUtils.trimToEmpty(temp + ",")));
						// 虚拟机IP在虚拟机列和虚拟机列唯一校验
						if (!CollectionUtils.isEmpty(searchBy_temp_ip)
								&& !searchBy_temp_ip.get(0).equals(bizHost.getId())) {
							log.error(errorInfo + "虚拟机IP已作为主机存在");
							throw new Exception(errorInfo + "虚拟机IP已作为主机存在");
						}
						if (!CollectionUtils.isEmpty(searchBy_temp_hostInfo)
								&& !searchBy_temp_hostInfo.get(0).equals(bizHost.getId())) {
							log.error(errorInfo + "虚拟机IP已存在");
							throw new Exception(errorInfo + "虚拟机IP已存在");
						}
					}
				}
				// 虚拟机IP(最后一个IP也必须带英文逗号，检索用)
				String hostInfoStr = String.join(",", hosts) + ",";
				if (StringUtils.isNotEmpty(hostInfoStr) && !hostInfoStr.equals(",")) {
					bizHost.setHostInfo(hostInfoStr);
					bizHost.setHostNum(hosts.length + 1);
				} else {
					bizHost.setHostInfo(null);
					bizHost.setHostNum(1);
				}
			}
			hostService.saveAndFlush(bizHost);
			GeneralResponseData<BizHost> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error(String.format("%s，参数：%s，结果：%s", errorInfo, params, e.getMessage()));
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getMessage()));
		}

	}

	/**
	 * 
	 * @param hostId
	 * @param lockStatus
	 *            锁定：1/解锁：0
	 * @return
	 * @throws JsonProcessingException
	 */
	@RequestMapping("/lockOrUnlock")
	public String lockOrUnlock(@RequestParam(value = "hostId", required = false) Integer hostId,
			@RequestParam(value = "lockStatus", required = false) Integer lockStatus) throws JsonProcessingException {
		String params = buildParams().toString();
		try {
			if (!lockStatus.equals(0) && !lockStatus.equals(1)) {
				log.error("操作指令" + lockStatus + "无效，锁定：1/解锁：0");
				throw new Exception("操作指令" + lockStatus + "无效，锁定：1/解锁：0");
			}
			// 主机信息
			SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (null == operator || StringUtils.isEmpty(operator.getUid())) {
				log.error("未登录，请登录后再操作");
				throw new Exception("未登录，请登录后再操作");
			}
			BizHost bizHost = hostService.findById(hostId);
			if (lockStatus.equals(0)) {// 解锁
				// 的确被锁定，且锁定人是当前用户才可以解锁
				if (StringUtils.isEmpty(bizHost.getOperator())) {
					log.error("当前主机已处于解锁状态");
					throw new Exception("当前主机已处于解锁状态");
				}
				if (!operator.getUid().trim().equals(bizHost.getOperator().trim())) {
					log.error("当前登录用户（" + operator.getUid() + "）不是此主机锁定人" + bizHost.getOperator());
					throw new Exception("当前登录用户（" + operator.getUid() + "）不是此主机锁定人" + bizHost.getOperator());
				}
				bizHost.setOperator(null);
			} else if (lockStatus.equals(1)) {// 锁定
				// 后端验证主机是否是锁定状态，非锁定状态才可以锁定
				if (StringUtils.isNotEmpty(bizHost.getOperator())) {
					log.error("当前主机已处于锁定状态");
					throw new Exception("当前主机已处于锁定状态");
				}
				bizHost.setOperator(operator.getUid());
				// 启用机器人远程操作当前主机IP
				/*
				 * try { Runtime.getRuntime().exec("mstsc /v: " +
				 * bizHost.getIp() + " /admin"); } catch (IOException e) {
				 * logger.error(String.format("远程桌面连接失败", e.getMessage())); }
				 */
			}
			hostService.saveAndFlush(bizHost);

			GeneralResponseData<BizHost> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error(String.format("%s，参数：%s，结果：%s", "操作失败", params, e.getMessage()));
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getMessage()));
		}

	}

	@RequestMapping("/delete")
	public String delete(@RequestParam(value = "hostId") Integer hostId) throws JsonProcessingException {
		String params = buildParams().toString();
		String errorInfo = "删除主机失败：";
		try {
			BizHost host = hostService.findById(hostId);
			if (null == host) {
				log.error(errorInfo + "主机不存在，请刷新页面");
				throw new Exception(errorInfo + "主机不存在，请刷新页面");
			}
			if (StringUtils.isNotEmpty(host.getOperator())) {
				log.error(errorInfo + "主机已被" + host.getOperator() + "锁定");
				throw new Exception(errorInfo + "主机已被" + host.getOperator() + "锁定");
			}
			hostService.delete(hostId);
			GeneralResponseData<BizHost> responseData = new GeneralResponseData<>(
					GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			String result = mapper.writeValueAsString(responseData);
			logger.debug(String.format("%s，参数：%s，结果：%s", "主机删除", params, result));
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			logger.error(String.format("%s，参数：%s，结果：%s", "操作失败", params, e.getMessage()));
			return mapper.writeValueAsString(
					new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), e.getMessage()));
		}

	}

	/**
	 * check whether the target {@code arg0} contains same elements.
	 * 
	 * @param arg0
	 *            to check
	 * @return {@code true}:duplicate ,otherwise,{@code false}
	 */
	private boolean isRepeat(String[] arg0) {
		return new HashSet<String>() {
			{
				addAll(Arrays.asList(arg0));
			}
		}.size() != arg0.length;
	}
}
