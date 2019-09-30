package com.xinbo.fundstransfer.ali4enterprise.controller;

import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.ali4enterprise.inputdto.Ali4EpAccountInputDTO;
import com.xinbo.fundstransfer.ali4enterprise.service.Ali4EpAccountService;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.service.HandicapService;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/ali4epAccount")
@Slf4j
public class Ali4EpAccountController {

	@Autowired
	private Ali4EpAccountService service;
	@Autowired
	private HandicapService handicapService;

	/**
	 * 获取总记录数，不包含分页信息
	 * 
	 * @param inputDTO
	 * @return
	 */
	@RequestMapping("/list")
	private GeneralResponseData<List<BizAccount>> list(@RequestBody Ali4EpAccountInputDTO inputDTO) {
		GeneralResponseData<List<BizAccount>> responseData;
		try {
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser == null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆");
				return responseData;
			}
			List<BizHandicap> handicaps = handicapService.findByUserId(sysUser.getId());
			if (CollectionUtils.isEmpty(handicaps)) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "无数据权限");
				return responseData;
			}
			if (Objects.isNull(inputDTO.getHandicapId())) {
				inputDTO.setHandicaps(handicaps);
			}
			PageRequest pageRequest = new PageRequest((inputDTO.getPageNo() - 1) * inputDTO.getPageSize(),
					inputDTO.getPageSize(), new Sort(Sort.Direction.DESC, "createTime"));
			List<BizAccount> list = service.list(inputDTO, pageRequest);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
					" 查询企业支付宝账号成功");
			Paging paging = wrapPage(inputDTO.getPageSize(), inputDTO.getPageNo() - 1,
					CollectionUtils.isEmpty(list) ? 0 : list.size());
			responseData.setPage(paging);
			responseData.setData(list);

		} catch (Exception e) {
			log.error("查询企业支付宝账号失败:", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
					" 查询企业支付宝账号失败");
		}
		return responseData;
	}

	/**
	 * 获取分页信息
	 * 
	 * @param inputDTO
	 * @return
	 */
	@RequestMapping("/count")
	private GeneralResponseData count(@RequestBody Ali4EpAccountInputDTO inputDTO) {
		GeneralResponseData responseData;
		try {
			SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
			if (sysUser == null) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆");
				return responseData;
			}
			List<BizHandicap> handicaps = handicapService.findByUserId(sysUser.getId());
			if (CollectionUtils.isEmpty(handicaps)) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "无数据权限");
				return responseData;
			}
			if (Objects.isNull(inputDTO.getHandicapId())) {
				inputDTO.setHandicaps(handicaps);
			}
			Long count = service.count(inputDTO);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
					" 查询企业支付宝账号总记录数成功");
			Paging paging = wrapPage(inputDTO.getPageSize(), inputDTO.getPageNo() - 1, count.intValue());
			responseData.setPage(paging);
		} catch (Exception e) {
			log.error("查询企业支付宝账号总记录数失败:", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
					" 查询企业支付宝账号总记录数失败");
		}
		return responseData;
	}

	// 包装分页返回页面 count 总记录数
	private Paging wrapPage(Integer pageSize, int pageNo, Integer count) {
		Paging page;
		if (count != null) {
			page = CommonUtils.getPage(pageNo + 1, pageSize != null ? pageSize : AppConstants.PAGE_SIZE,
					String.valueOf(count));
		} else {
			page = CommonUtils.getPage(0, pageSize != null ? pageSize : AppConstants.PAGE_SIZE, "0");
		}
		return page;
	}
}
