package com.xinbo.fundstransfer.newpay.controller;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.CharStreams;
import com.xinbo.fundstransfer.AppConstants;
import com.xinbo.fundstransfer.CommonUtils;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.domain.ResponseDataNewPay;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.newpay.inputdto.*;
import com.xinbo.fundstransfer.newpay.outdto.*;
import com.xinbo.fundstransfer.newpay.service.NewPayService;
import com.xinbo.fundstransfer.newpay.serviceImpl.NewPayServiceImpl;

/**
 * Created by Administrator on 2018/7/11.
 */
@RestController
@RequestMapping("/newpay")
public class NewPayController {
	private static final Logger LOGGER = LoggerFactory.getLogger(NewPayController.class);
	@Autowired
	private NewPayService newPayService;
	@Autowired
	private NewPayServiceImpl newPayServiceimpl;

	private ObjectMapper mapper = new ObjectMapper();

	// 1.1.1 新增资料
	@RequestMapping(value = "/add", method = RequestMethod.POST, consumes = "application/json")
	public String add(@Valid @RequestBody AddNewPayInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<AddNewPayOutputDTO> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay<AddNewPayOutputDTO> responseDataNewPay = newPayService.add(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "新增成功");
				responseData.setData(responseDataNewPay.getData());
			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"新增失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute add fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"新增失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.1.2 修改基本信息
	@RequestMapping("/modifyInfo")
	public String modifyInfo(@Valid @RequestBody ModifyInfoInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<ModifyInfoOutputDTO> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay<ModifyInfoOutputDTO> responseDataNewPay = newPayService.modifyInfo(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "修改成功");
				responseData.setData(responseDataNewPay.getData());
			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"修改失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute modifyInfo fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"修改失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.1.3 修改账号资料
	@RequestMapping("/modifyAccount")
	public String modifyAccount(@Valid @RequestBody ModifyAccountInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<ModifyAccountOutputDTO> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay<ModifyAccountOutputDTO> responseDataNewPay = newPayService.modifyAccount(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "修改成功");
				responseData.setData(responseDataNewPay.getData());
			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"修改失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute modifyAccount fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"修改失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.1.4 修改状态
	@RequestMapping("/modifyStatus")
	public String modifyStatus(@Valid @RequestBody ModifyStatusInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<ModifyStatusOutputDTO> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay<ModifyStatusOutputDTO> responseDataNewPay = newPayService.modifyStatus(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "修改成功");
				responseData.setData(responseDataNewPay.getData());
			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"修改失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute modifyStatus fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"修改失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.1.5 修改密码
	@RequestMapping("/modifyPwd")
	public String modifyPwd(@Valid @RequestBody ModifyPWDInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<String> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay responseDataNewPay = newPayService.modifyPwd(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "修改成功");
				responseData.setData(responseDataNewPay.getData().toString());
			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"修改失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute modifyPwd fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"修改失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.1.6 删除
	@RequestMapping("/remove")
	public String remove(@Valid @RequestBody RemoveInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<String> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay responseDataNewPay = newPayService.remove(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "删除成功");
				responseData.setData(responseDataNewPay.getData().toString());
			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"删除失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute remove fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"删除失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.1.7 条件查询分页列表
	@RequestMapping("/findByCondition")
	public String findByCondition(@Valid @RequestBody FindByConditionInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<List<FindByConditionOutputDTO>> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay<PageOutputDTO<FindByConditionOutputDTO>> responseDataNewPay = newPayService
					.findByCondition(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
				responseData.setData(responseDataNewPay.getData().getResultList());
				Paging paging = wrapPage(inputDTO.getPageSize(), inputDTO.getPageNo() - 1,
						responseDataNewPay.getData().getTotalRecordNumber());
				Map map = new HashMap();
				map.put("other", responseDataNewPay.getData().getOther());
				paging.setHeader(map);
				responseData.setPage(paging);

			} else {
				// 失败时候 msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setPage(new Paging());
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute findByCondition fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.1.8 查询微信、支付宝绑定的银行卡
	@RequestMapping("/findBank")
	public String findBank(@Valid @RequestBody FindBankInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<FindBankOutputDTO> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay<FindBankOutputDTO> responseDataNewPay = newPayService.findBank(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
				responseData.setData(responseDataNewPay.getData());
			} else {
				// 失败时候 msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setPage(new Paging());
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute findBank fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.1.9 查询兼职人员微信、支付宝、银行卡的余额、转入转出金额
	@RequestMapping("/findBalanceInfo")
	public String findBalanceInfo(@Valid @RequestBody FindBalanceInfoInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<List<FindBalanceInfoOutputDTO>> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay<List<FindBalanceInfoOutputDTO>> responseDataNewPay = newPayService
					.findBalanceInfo(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
				responseData.setData(responseDataNewPay.getData());

			} else {
				// 失败时候 msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setPage(new Paging());
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute findBalanceInfo fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.1.10 查询账号信息，account带*
	@RequestMapping("/findAccountInfo")
	public String findAccountInfo(@Valid @RequestBody FindAccountInfoInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<FindAccountInfoOutputDTO> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay<FindAccountInfoOutputDTO> responseDataNewPay = newPayService.findAccountInfo(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
				responseData.setData(responseDataNewPay.getData());

			} else {
				// 失败时候 msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setPage(new Paging());
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute findAccountInfo fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.1.11 查询账号信息，account没有带*
	@RequestMapping("/findAccountInfo2")
	public String findAccountInfo2(@Valid @RequestBody FindAccountInfoInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<FindAccountInfoOutputDTO> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay<FindAccountInfoOutputDTO> responseDataNewPay = newPayService.findAccountInfo2(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
				responseData.setData(responseDataNewPay.getData());
			} else {
				// 失败时候 msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setPage(new Paging());
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute findAccountInfo2 fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.1.12 查询手机信息
	@RequestMapping("/findTelInfo")
	public String findTelInfo(@Valid @RequestBody FindTelInfoInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<FindTelInfoOutputDTO> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay<FindTelInfoOutputDTO> responseDataNewPay = newPayService.findTelInfo(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
				responseData.setData(responseDataNewPay.getData());
			} else {
				// 失败时候 msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setData(null);
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute findTelInfo fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.1.13 点击今日收款/佣金列
	@RequestMapping("/find8ByCondition")
	public String find8ByCondition(@Valid @RequestBody Find8ByConditionInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<List<Find8ByConditionOutputDTO>> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay<PageOutputDTO<Find8ByConditionOutputDTO>> responseDataNewPay = newPayService
					.find8ByCondition(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
				responseData.setData(responseDataNewPay.getData().getResultList());
				Paging paging = wrapPage(inputDTO.getPageSize(), inputDTO.getPageNo() - 1,
						responseDataNewPay.getData().getTotalRecordNumber());
				responseData.setPage(paging);
			} else {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setPage(new Paging());
				responseData.setData(null);
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute find8ByCondition fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.1.14 点击今日收款/佣金列，返佣记录
	@RequestMapping("/findCommissionDetailByCondition")
	public String findCommissionDetailByCondition(@Valid @RequestBody FindCommissionDetailInputDTO inputDTO,
			BindingResult result) throws JsonProcessingException {
		GeneralResponseData<List<FindCommissionDetailOutputDTO>> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay<PageOutputDTO<FindCommissionDetailOutputDTO>> responseDataNewPay = newPayService
					.findCommissionDetailByCondition(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
				responseData.setData(responseDataNewPay.getData().getResultList());
				Paging paging = wrapPage(inputDTO.getPageSize(), inputDTO.getPageNo() - 1,
						responseDataNewPay.getData().getTotalRecordNumber());
				responseData.setPage(paging);
			} else {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setPage(new Paging());
				responseData.setData(null);
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute find8ByCondition fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.1.15 查询二维码分页列表
	@RequestMapping("/findQRByCondition")
	public String findQRByCondition(@Valid @RequestBody FindQRByConditionInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<List<FindQRByConditionOutputDTO>> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay<PageOutputDTO<FindQRByConditionOutputDTO>> responseDataNewPay = newPayService
					.findQRByCondition(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
				responseData.setData(responseDataNewPay.getData().getResultList());
				Paging paging = wrapPage(inputDTO.getPageSize(), inputDTO.getPageNo() - 1,
						responseDataNewPay.getData().getTotalRecordNumber());
				responseData.setPage(paging);
			} else {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setPage(new Paging());
				responseData.setData(null);
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute findQRByCondition fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.1.16-1 批量生成二维码地址
	@RequestMapping("/batchAddQR")
	public String batchAddQR(@Valid @RequestBody BatchAddQRInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<String> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay responseDataNewPay = newPayService.batchAddQR(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						" 批量生成二维码地址成功");
				responseData.setData(responseDataNewPay.getData().toString());
			} else {
				// 失败时候 msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						" 批量生成二维码地址失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setData(null);
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute batchAddQR fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					" 批量生成二维码地址失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.1.16-2 范围值批量生成二维码地址(输入一个数字生成一个范围)
	// inclusiveLimit 表示是否包含最大值金额
	@RequestMapping("/batchAddQR2")
	public String batchAddQR2(@RequestParam(value = "oid") Integer oid,
			@RequestParam(value = "accountId") Long accountId, @RequestParam(value = "type") Byte type,
			@RequestParam(value = "amtBegin") Integer amtBegin, @RequestParam(value = "amtEnd") Integer amtEnd,
			@RequestParam(value = "step") Integer step) throws JsonProcessingException {
		GeneralResponseData<String> responseData;
		try {
			if (oid == null || accountId == null || type == null || amtBegin == null || amtEnd == null
					|| step == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数不能为空"));
			}
			Set<Double> amountSet = generateAmountStr4QR(amtBegin, amtEnd, step, true);
			if (amountSet == null || amountSet.size() == 0) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "金额参数不能为空"));
			}
			BatchAddQRInputDTO inputDTO = new BatchAddQRInputDTO();
			inputDTO.setAccountId(accountId);
			inputDTO.setOid(oid);
			inputDTO.setType(type);
			inputDTO.setMoneySet(amountSet);
			ResponseDataNewPay responseDataNewPay = newPayService.batchAddQR(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						"范围值批量生成二维码地址成功");
				responseData.setData(responseDataNewPay.getData().toString());
			} else {
				// 失败时候 msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"范围值批量生成二维码地址失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setData(null);
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute batchAddQR2 fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"范围值批量生成二维码地址失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.1.16-3 文本导入批量生成二维码地址(批量导入生成)
	@RequestMapping(value = "/batchAddQRText/{oid}/{accountId}/{type}/{qrCodeCount}", consumes = "multipart/form-data", method = RequestMethod.POST)
	public String batchAddQRText(@PathVariable(value = "oid") Integer oid,
			@PathVariable(value = "accountId") Long accountId, @PathVariable(value = "type") Byte type,
			@PathVariable(value = "qrCodeCount") Integer qrCodeCount, @RequestParam("file") MultipartFile multipartFile)
			throws JsonProcessingException {
		GeneralResponseData<String> responseData;
		try {
			if (oid == null || accountId == null || type == null) {
				return mapper.writeValueAsString(
						new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数不能为空"));
			}
			String amounts = "";
			InputStream inputStream = multipartFile.getInputStream();// 获取输入流
			if (null != inputStream) {
				String str = CharStreams.toString(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
				if (StringUtils.isNotEmpty(str)) {
					amounts = str.replaceAll("\\r\\n", ",");
				}
			}
			if (StringUtils.isEmpty(amounts)) {
				return mapper.writeValueAsString(new GeneralResponseData<>(
						GeneralResponseData.ResponseStatus.FAIL.getValue(), "内容为空" + amounts));
			}
			String[] amountsStrArray = amounts.split(",");
			Double[] amountsDArray = new Double[amountsStrArray.length];
			for (int i = 0; i < amountsStrArray.length; i++) {
				amountsDArray[i] = Double.valueOf(amountsStrArray[i]);
			}
			Set<Double> amountSet = new HashSet<>(Arrays.asList(amountsDArray));
			BatchAddQRInputDTO inputDTO = new BatchAddQRInputDTO();
			inputDTO.setAccountId(accountId);
			inputDTO.setOid(oid);
			inputDTO.setType(type);
			inputDTO.setMoneySet(amountSet);
			if (!qrCodeCount.equals(0)) {
				inputDTO.setQrCodeCount(qrCodeCount);
			}
			ResponseDataNewPay responseDataNewPay = newPayService.batchAddQR(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						"文本导入批量生成二维码地址成功");
				responseData.setData(responseDataNewPay.getData().toString());
			} else {
				// 失败时候 msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"文本导入批量生成二维码地址失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setData(null);
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute batchAddQRText fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"文本导入批量生成二维码地址失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.1.17 批量删除二维码地址
	@RequestMapping("/batchRemoveQR")
	public String batchRemoveQR(@Valid @RequestBody BatchDeleteQRInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<String> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay responseDataNewPay = newPayService.batchDeleteQR(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "删除成功");
				responseData.setData(responseDataNewPay.getData().toString());
			} else {
				// 失败时候 msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"删除失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setData(null);
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute batchRemoveQR fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"删除失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.1.18 查询银行下拉列表
	@RequestMapping("/findAll")
	public String findAll() throws JsonProcessingException {
		GeneralResponseData<List<FindBankAllOutputDTO>> responseData;
		try {
			ResponseDataNewPay<List<FindBankAllOutputDTO>> responseDataNewPay = newPayService.findBankAll();
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
			} else {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			responseData.setData(responseDataNewPay.getData());
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute findAll fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.1.19 查询密码是否已被设置
	@RequestMapping("/findPwdExists")
	public String findPwdExists(@Valid @RequestBody FindPwdExistsInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<FindPwdExistsOutputDTO> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay<FindPwdExistsOutputDTO> responseDataNewPay = newPayService.findPwdExists(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
				responseData.setData(responseDataNewPay.getData());
			} else {
				// 失败时候 msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setData(null);
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute findPwdExists fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.1.20 修改收款理由前缀后缀
	@RequestMapping("/modifyFix")
	public String modifyFix(@Valid @RequestBody ModifyFixInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<ModifyFixOutputDTO> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay<ModifyFixOutputDTO> responseDataNewPay = newPayService.modifyFix(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "修改成功");
				responseData.setData(responseDataNewPay.getData());
			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"修改失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute modifyInfo fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"修改失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.1.21 查询业主未删除的新支付通道
	@RequestMapping("/findPOCForCrk")
	public String findPOCForCrk(@Valid @RequestBody FindPOCForCrkInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<List<FindPOCForCrkOutputDTO>> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay<List<FindPOCForCrkOutputDTO>> responseDataNewPay = newPayService.findPOCForCrk(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
				responseData.setData(responseDataNewPay.getData());
			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute modifyInfo fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.1.22 绑定支付通道和客户资料
	@RequestMapping("/newpayAisleConfigBind")
	public String newpayAisleConfigBind(@Valid @RequestBody NewpayAisleConfigBindInputDTO inputDTO,
			BindingResult result) throws JsonProcessingException {
		GeneralResponseData<String> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		try {
			inputDTO.setOperationAdminId(sysUser.getId().longValue());
			inputDTO.setOperationAdminName(sysUser.getUid());
			ResponseDataNewPay responseDataNewPay = newPayService.newpayAisleConfigBind(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "绑定成功");
				responseData.setData(responseDataNewPay.getData().toString());
			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"绑定失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute modifyPwd fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"绑定失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.1.23 查询客户资料已绑定的支付通道
	@RequestMapping("/newpayAisleConfigFindBind")
	public String newpayAisleConfigFindBind(@Valid @RequestBody NewpayAisleConfigFindBindInputDTO inputDTO,
			BindingResult result) throws JsonProcessingException {
		GeneralResponseData<String> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay responseDataNewPay = newPayService.newpayAisleConfigFindBind(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
				responseData.setData(responseDataNewPay.getData().toString());
			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute modifyPwd fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.1.24 银行的余额同步
	@RequestMapping("/syncBankBalance")
	public String syncBankBalance(@Valid @RequestBody SyncBankBalanceInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<String> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay responseDataNewPay = newPayService.syncBankBalance(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						"syncBankBalance success");
				responseData.setData(responseDataNewPay.getData().toString());
			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"syncBankBalance fail :" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute syncBankBalance fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.1.25 形容词名词 - 新增
	@RequestMapping("/contentAdd")
	public String contentAdd(@Valid @RequestBody AddContentInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<ContentOutputDTO> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		try {
			inputDTO.setOperationAdminId(sysUser.getId().longValue());
			inputDTO.setOperationAdminName(sysUser.getUid());
			ResponseDataNewPay<ContentOutputDTO> responseDataNewPay = newPayService.contentAdd(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						"contentAdd success");
				responseData.setData(responseDataNewPay.getData());
			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"contentAdd fail :" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute contentAdd fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.1.26 形容词名词 – 修改
	@RequestMapping("/contentModify")
	public String contentModify(@Valid @RequestBody ModifyContentInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<ContentOutputDTO> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		try {
			inputDTO.setOperationAdminId(sysUser.getId().longValue());
			inputDTO.setOperationAdminName(sysUser.getUid());
			ResponseDataNewPay<ContentOutputDTO> responseDataNewPay = newPayService.contentModify(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						"contentModify success");
				responseData.setData(responseDataNewPay.getData());
			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"contentModify fail :" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute contentModify fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.1.27 形容词名词 – 启用、停用
	@RequestMapping("/contentEnable")
	public String contentEnable(@Valid @RequestBody EnableContentInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<ContentOutputDTO> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		try {
			inputDTO.setOperationAdminId(sysUser.getId().longValue());
			inputDTO.setOperationAdminName(sysUser.getUid());
			ResponseDataNewPay<ContentOutputDTO> responseDataNewPay = newPayService.contentEnable(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						"contentEnable success");
				responseData.setData(responseDataNewPay.getData());
			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"contentEnable fail :" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute contentEnable fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.1.28 形容词名词 – 删除
	@RequestMapping("/contentRemove")
	public String contentRemove(@Valid @RequestBody RemoveContentInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<String> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		try {
			inputDTO.setOperationAdminId(sysUser.getId().longValue());
			inputDTO.setOperationAdminName(sysUser.getUid());
			ResponseDataNewPay responseDataNewPay = newPayService.contentRemove(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						"contentRemove success");
				responseData.setData(responseDataNewPay.getData().toString());
			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"contentRemove fail :" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute contentRemove fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.1.29 形容词名词 – 分页查询
	@RequestMapping("/findContentByCondition")
	public String findContentByCondition(@Valid @RequestBody FindContentByConditionInputDTO inputDTO,
			BindingResult result) throws JsonProcessingException {
		GeneralResponseData<List<ContentOutputDTO>> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay<PageOutputDTO<ContentOutputDTO>> responseDataNewPay = newPayService
					.findContentByCondition(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
				responseData.setData(responseDataNewPay.getData().getResultList());
				Paging paging = wrapPage(inputDTO.getPageSize(), inputDTO.getPageNo() - 1,
						responseDataNewPay.getData().getTotalRecordNumber());
				Map map = new HashMap();
				map.put("other", responseDataNewPay.getData().getOther());
				paging.setHeader(map);
				responseData.setPage(paging);

			} else {
				// 失败时候 msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setPage(new Paging());
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute syncBankBalance fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.1.30 修改未确认出款金额开关
	@RequestMapping("/modifyUoFlag")
	public String modifyUoFlag(@Valid @RequestBody ModifyUoFlagInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<String> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		try {
			inputDTO.setOperationAdminId(sysUser.getId().longValue());
			inputDTO.setOperationAdminName(sysUser.getUid());
			ResponseDataNewPay responseDataNewPay = newPayService.modifyUoFlag(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						"modifyUoFlag success");
				responseData.setData(responseDataNewPay.getData().toString());
			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"modifyUoFlag fail :" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute modifyUoFlag fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.1.31 生成常用金额/非常用金额二维码
	@RequestMapping("/genANMultQr")
	public String genANMultQr(@Valid @RequestBody GenANMultQrInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<String> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay responseDataNewPay = newPayService.genANMultQr(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						"syncBankBalance success");
				responseData.setData(responseDataNewPay.getData().toString());
			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"syncBankBalance fail :" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute syncBankBalance fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.1.32 统计常用金额、非常用金额已生成二维码个数和总个数
	@RequestMapping("/statisticsMWR")
	public String statisticsMWR(@Valid @RequestBody StatisticsMWRInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<StatisticsMWROutputDTO> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay<StatisticsMWROutputDTO> responseDataNewPay = newPayService.statisticsMWR(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						"statisticsMWR success");
				responseData.setData(responseDataNewPay.getData());
			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"statisticsMWR fail :" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute statisticsMWR fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.1.33 形容词类型 – 新增
	@RequestMapping("/addWordType")
	public String addWordType(@Valid @RequestBody AddWordTypeInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<WordTypeOutputDTO> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		try {
			inputDTO.setOperationAdminId(sysUser.getId().longValue());
			inputDTO.setOperationAdminName(sysUser.getUid());
			ResponseDataNewPay<WordTypeOutputDTO> responseDataNewPay = newPayService.addWordType(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						"addWordType success");
				responseData.setData(responseDataNewPay.getData());
			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"addWordType fail :" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute addWordType fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"新增失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.1.34 形容词类型 – 查询列表
	@RequestMapping("/findWordType")
	public String findWordType(@Valid @RequestBody FindWordTypeInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<List<WordTypeOutputDTO>> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay<List<WordTypeOutputDTO>> responseDataNewPay = newPayService.findWordType(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
				responseData.setData(responseDataNewPay.getData());
			} else {
				// 失败时候 msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setPage(new Paging());
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute syncBankBalance fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.1.35 形容词类型 – 删除
	@RequestMapping("/removeWordType")
	public String removeWordType(@Valid @RequestBody RemoveWordTypeInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<String> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		try {
			inputDTO.setOperationAdminId(sysUser.getId().longValue());
			inputDTO.setOperationAdminName(sysUser.getUid());
			ResponseDataNewPay responseDataNewPay = newPayService.removeWordType(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						"removeWordType success");
				responseData.setData(responseDataNewPay.getData().toString());
			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"removeWordType fail :" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute removeWordType fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"删除失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.1.36 兼职绑定词语
	@RequestMapping("/bindingWordType")
	public String bindingWordType(@Valid @RequestBody BindingWordTypeInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<String> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		try {
			inputDTO.setOperationAdminId(sysUser.getId().longValue());
			inputDTO.setOperationAdminName(sysUser.getUid());
			ResponseDataNewPay responseDataNewPay = newPayService.bindingWordType(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						"bindingWordType success");
				responseData.setData(responseDataNewPay.getData().toString());
			} else {
				// 失败时候msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"bindingWordType fail :" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setData(null);
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute bindingWordType fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"绑定失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.1.37 查询词库绑定分页列表
	@RequestMapping("/findForBind")
	public String findForBind(@Valid @RequestBody FindForBindInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<List<BindOutputDTO>> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay<PageOutputDTO<BindOutputDTO>> responseDataNewPay = newPayService.findForBind(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
				Paging paging = wrapPage(inputDTO.getPageSize(), inputDTO.getPageNo() - 1,
						responseDataNewPay.getData().getTotalRecordNumber());
				Map map = new HashMap();
				map.put("other", responseDataNewPay.getData().getOther());
				paging.setHeader(map);
				responseData.setPage(paging);
				List<BindOutputDTO> list = responseDataNewPay.getData().getResultList();
				responseData.setData(list);
			} else {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setPage(new Paging());
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute findBankCardByCondition fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.2.1 查询银行卡分页列表
	@RequestMapping("/findBankCardByCondition")
	public String findBankCardByCondition(@Valid @RequestBody FindBankCardByConditionInputDTO inputDTO,
			BindingResult result) throws JsonProcessingException {
		GeneralResponseData<List<FindBankCardByConditionOutputDTO>> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay<PageOutputDTO<FindBankCardByConditionOutputDTO>> responseDataNewPay = newPayService
					.findBankCardByCondition(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
				Paging paging = wrapPage(inputDTO.getPageSize(), inputDTO.getPageNo() - 1,
						responseDataNewPay.getData().getTotalRecordNumber());
				Map map = new HashMap();
				map.put("other", responseDataNewPay.getData().getOther());
				paging.setHeader(map);
				responseData.setPage(paging);
				List<FindBankCardByConditionOutputDTO> list = responseDataNewPay.getData().getResultList();
				responseData.setData(list);
			} else {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setPage(new Paging());
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute findBankCardByCondition fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.2.2 询微信、支付宝分页列表
	@RequestMapping("/findAWByCondition")
	public String findAWByCondition(@Valid @RequestBody FindAWByConditionInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<List<FindAWByConditionOutputDTO>> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay<PageOutputDTO<FindAWByConditionOutputDTO>> responseDataNewPay = newPayService
					.findAWByCondition(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
				Paging paging = wrapPage(inputDTO.getPageSize(), inputDTO.getPageNo() - 1,
						responseDataNewPay.getData().getTotalRecordNumber());
				Map map = new HashMap();
				map.put("other", responseDataNewPay.getData().getOther());
				paging.setHeader(map);
				responseData.setPage(paging);
				responseData.setData(responseDataNewPay.getData().getResultList());
			} else {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setPage(new Paging());
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute findAWByCondition fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.2.3 银行卡转入记录点击数字
	@RequestMapping("/find2ByCondition")
	public String find2ByCondition(@Valid @RequestBody Find2ByConditioninputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<List<Find2ByConditionOutputDTO>> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay<PageOutputDTO<Find2ByConditionOutputDTO>> responseDataNewPay = newPayService
					.find2ByCondition(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
				Paging paging = wrapPage(inputDTO.getPageSize(), inputDTO.getPageNo() - 1,
						responseDataNewPay.getData().getTotalRecordNumber());
				Map map = new HashMap();
				map.put("other", responseDataNewPay.getData().getOther());
				paging.setHeader(map);
				responseData.setPage(paging);
				responseData.setData(responseDataNewPay.getData().getResultList());
			} else {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setPage(new Paging());
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute find2ByCondition fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.2.4 微信、支付宝转入记录点击数字
	@RequestMapping("/findAW2ByCondition")
	public String findAW2ByCondition(@Valid @RequestBody FindAWIn2ByConditionInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<List<FindAWIn2ByConditionOutputDTO>> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay<PageOutputDTO<FindAWIn2ByConditionOutputDTO>> responseDataNewPay = newPayService
					.findAWIN2ByCondition(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
				Paging paging = wrapPage(inputDTO.getPageSize(), inputDTO.getPageNo() - 1,
						responseDataNewPay.getData().getTotalRecordNumber());
				Map map = new HashMap();
				map.put("other", responseDataNewPay.getData().getOther());
				paging.setHeader(map);
				responseData.setPage(paging);
				responseData.setData(responseDataNewPay.getData().getResultList());
			} else {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setPage(new Paging());
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute findAW2ByCondition fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.2.5 微信、支付宝、银行卡转出记录点击数字
	@RequestMapping("/find3ByCondition")
	public String find3ByCondition(@Valid @RequestBody FindAWB3OutByConditionInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<List<FindAWB3OutByConditionOutputDTO>> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay<PageOutputDTO<FindAWB3OutByConditionOutputDTO>> responseDataNewPay = newPayService
					.find3ByCondition(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
				Paging paging = wrapPage(inputDTO.getPageSize(), inputDTO.getPageNo() - 1,
						responseDataNewPay.getData().getTotalRecordNumber());
				Map map = new HashMap();
				map.put("other", responseDataNewPay.getData().getOther());
				paging.setHeader(map);
				responseData.setPage(paging);
				responseData.setData(responseDataNewPay.getData().getResultList());
			} else {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setPage(new Paging());
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute find3ByCondition fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.2.6 微信、支付宝流水
	@RequestMapping("/findAWLog3ByCondition")
	public String findAWLog3ByCondition(@Valid @RequestBody FindAWLOG3ByConditionInputDTO inputDTO,
			BindingResult result) throws JsonProcessingException {
		GeneralResponseData<List<FindAWLOG3ByConditionOutputDTO>> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay<PageOutputDTO<FindAWLOG3ByConditionOutputDTO>> responseDataNewPay = newPayService
					.findAWLog3ByCondition(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
				Paging paging = wrapPage(inputDTO.getPageSize(), inputDTO.getPageNo() - 1,
						responseDataNewPay.getData().getTotalRecordNumber());
				Map map = new HashMap();
				map.put("other", responseDataNewPay.getData().getOther());
				paging.setHeader(map);
				responseData.setPage(paging);
				responseData.setData(responseDataNewPay.getData().getResultList());
			} else {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setPage(new Paging());
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute findAWLog3ByCondition fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.2.7 银行卡流水
	@RequestMapping("/find9ByCondition")
	public String find9ByCondition(@Valid @RequestBody FindBLog9ByConditionInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<List<FindBLog9ByConditionOutputDTO>> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay<PageOutputDTO<FindBLog9ByConditionOutputDTO>> responseDataNewPay = newPayService
					.find9ByCondition(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
				Paging paging = wrapPage(inputDTO.getPageSize(), inputDTO.getPageNo() - 1,
						responseDataNewPay.getData().getTotalRecordNumber());
				Map map = new HashMap();
				map.put("other", responseDataNewPay.getData().getOther());
				paging.setHeader(map);
				responseData.setPage(paging);
				responseData.setData(responseDataNewPay.getData().getResultList());
			} else {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setPage(new Paging());
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute find9ByCondition fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.2.8 对账
	@RequestMapping("/verifyAccount")
	public String verifyAccount(@Valid @RequestBody VerifyAccountInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<String> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			newPayService.verifyAccount(inputDTO);
			// ResponseDataNewPay responseDataNewPay = new ResponseDataNewPay();
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "对账成功");
			// if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
			//
			// } else {
			// // 失败时候 msg不为null
			// responseData = new
			// GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
			// "对账失败:" + responseDataNewPay.getMsg());
			// responseData.setMessage(responseDataNewPay.getMsg());
			// responseData.setData(null);
			// }
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute verifyAccount fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"对账失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.3.1 微信、支付宝正在匹配查询分页列表
	@RequestMapping("/find4ByCondition")
	public String find4ByCondition(@Valid @RequestBody Find4ByConditionInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<List<Find4ByConditionOutputDTO>> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay<PageOutputDTO<Find4ByConditionOutputDTO>> responseDataNewPay = newPayService
					.find4ByCondition(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
				Paging paging = wrapPage(inputDTO.getPageSize(), inputDTO.getPageNo() - 1,
						responseDataNewPay.getData().getTotalRecordNumber());
				responseData.setPage(paging);
				responseData.setData(responseDataNewPay.getData().getResultList());
			} else {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setPage(new Paging());
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info(" NewPayController execute find4ByCondition fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.3.2 微信、支付宝未匹配、已匹配查询分页列表
	@RequestMapping("/find5ByCondition")
	public String find5ByCondition(@Valid @RequestBody Find5ByConditionInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<List<Find5ByConditionOutputDTO>> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay<PageOutputDTO<Find5ByConditionOutputDTO>> responseDataNewPay = newPayService
					.find5ByCondition(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
				Paging paging = wrapPage(inputDTO.getPageSize(), inputDTO.getPageNo() - 1,
						responseDataNewPay.getData().getTotalRecordNumber());
				Map map = new HashMap();
				map.put("other", responseDataNewPay.getData().getOther());
				paging.setHeader(map);
				responseData.setPage(paging);
				responseData.setData(responseDataNewPay.getData().getResultList());
			} else {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setPage(new Paging());
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute find5ByCondition fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.3.3 微信、支付宝未认领查询分页列表
	@RequestMapping("/find6ByCondition")
	public String find6ByCondition(@Valid @RequestBody Find6ByConditionInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<List<Find6ByConditionOutputDTO>> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay<PageOutputDTO<Find6ByConditionOutputDTO>> responseDataNewPay = newPayService
					.find6ByCondition(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
				Paging paging = wrapPage(inputDTO.getPageSize(), inputDTO.getPageNo() - 1,
						responseDataNewPay.getData().getTotalRecordNumber());
				Map map = new HashMap();
				map.put("other", responseDataNewPay.getData().getOther());
				paging.setHeader(map);
				responseData.setPage(paging);
				responseData.setData(responseDataNewPay.getData().getResultList());
			} else {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setPage(new Paging());
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute find6ByCondition fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.3.4 微信、支付宝已取消查询分页列表
	@RequestMapping("/find7ByCondition")
	public String find7ByCondition(@Valid @RequestBody Find7ByConditionInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<List<Find7ByConditionOutputDTO>> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay<PageOutputDTO<Find7ByConditionOutputDTO>> responseDataNewPay = newPayService
					.find7ByCondition(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
				Paging paging = wrapPage(inputDTO.getPageSize(), inputDTO.getPageNo() - 1,
						responseDataNewPay.getData().getTotalRecordNumber());
				Map map = new HashMap();
				map.put("other", responseDataNewPay.getData().getOther());
				paging.setHeader(map);
				responseData.setPage(paging);
				responseData.setData(responseDataNewPay.getData().getResultList());
			} else {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setPage(new Paging());
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute find7ByCondition fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.3.5 点击“待处理流水”列，第一个tab
	@RequestMapping("/find10ByCondition")
	public String find10ByCondition(@Valid @RequestBody Find10ByConditionInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<List<Find10ByConditionOutputDTO>> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay<PageOutputDTO<Find10ByConditionOutputDTO>> responseDataNewPay = newPayService
					.find10ByCondition(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
				Paging paging = wrapPage(inputDTO.getPageSize(), inputDTO.getPageNo() - 1,
						responseDataNewPay.getData().getTotalRecordNumber());
				Map map = new HashMap();
				map.put("other", responseDataNewPay.getData().getOther());
				paging.setHeader(map);
				responseData.setPage(paging);
				responseData.setData(responseDataNewPay.getData().getResultList());
			} else {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setPage(new Paging());
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute find10ByCondition fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.3.6 第一个tab取消
	@RequestMapping("/cancel")
	public String cancel(@Valid @RequestBody CancelInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<String> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		try {
			inputDTO.setOperationAdminId(sysUser.getId().longValue());
			inputDTO.setOperationAdminName(sysUser.getUid());
			ResponseDataNewPay responseDataNewPay = newPayService.cancel(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						" 取消成功");
				// responseData.setData(responseDataNewPay.getData().toString());
			} else {
				// 失败时候 msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						" 取消失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setData(null);
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute cancel fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					" 取消失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.3.7 第一个tab新增备注
	@RequestMapping("/modifyRemark")
	public String modifyRemark(@Valid @RequestBody ModifyRemarkInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<String> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		try {
			inputDTO.setOperationAdminId(sysUser.getId().longValue());
			inputDTO.setOperationAdminName(sysUser.getUid());
			ResponseDataNewPay responseDataNewPay = newPayService.modifyRemark(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						" 新增备注成功");
				responseData.setData(responseDataNewPay.getData().toString());
			} else {
				// 失败时候 msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"新增备注失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setData(null);
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute modifyRemark fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"新增备注失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.3.8 点击“待处理流水”列，第二个tab
	@RequestMapping("/find11ByCondition")
	public String find11ByCondition(@Valid @RequestBody Find11ByConditionInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<List<Find11ByConditionOutputDTO>> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay<PageOutputDTO<Find11ByConditionOutputDTO>> responseDataNewPay = newPayService
					.find11ByCondition(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
				Paging paging = wrapPage(inputDTO.getPageSize(), inputDTO.getPageNo() - 1,
						responseDataNewPay.getData().getTotalRecordNumber());
				Map map = new HashMap();
				map.put("other", responseDataNewPay.getData().getOther());
				paging.setHeader(map);
				responseData.setPage(paging);
				responseData.setData(responseDataNewPay.getData().getResultList());
			} else {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setPage(new Paging());
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute find11ByCondition fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.3.9 第二个tab备注
	@RequestMapping("/addRemark")
	public String addRemark(@Valid @RequestBody AddRemarkInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<String> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		try {
			inputDTO.setOperationAdminId(sysUser.getId().longValue());
			inputDTO.setOperationAdminName(sysUser.getUid());
			ResponseDataNewPay responseDataNewPay = newPayService.addRemark(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						" 备注成功");
				responseData.setData(responseDataNewPay.getData().toString());
			} else {
				// 失败时候 msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"备注失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setData(null);
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute addRemark fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"备注失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.3.10 第二个tab补提单
	@RequestMapping("/putPlus")
	public String putPlus(@Valid @RequestBody PutPlusInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<String> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		try {
			inputDTO.setOperationAdminId(sysUser.getId().longValue());
			inputDTO.setOperationAdminName(sysUser.getUid());
			ResponseDataNewPay responseDataNewPay = newPayService.putPlus(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						" 补提单成功");
				responseData.setData(responseDataNewPay.getData().toString());
			} else {
				// 失败时候 msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"补提单失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setData(null);
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute putPlus fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"补提单失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.3.11 匹配
	@RequestMapping("/matching")
	public String matching(@Valid @RequestBody MatchingInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<String> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		try {
			inputDTO.setOperationAdminId(sysUser.getId().longValue());
			inputDTO.setOperationAdminName(sysUser.getUid());
			ResponseDataNewPay responseDataNewPay = newPayService.matching(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						" 匹配成功");
				responseData.setData(responseDataNewPay.getData().toString());
			} else {
				// 失败时候 msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"匹配失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setData(null);
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute matching fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"匹配失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.3.12 统计指定device的正在匹配总数
	@RequestMapping("/statistics")
	public String statistics(@Valid @RequestBody StatisticsInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<List<StatisticsOutputDTO>> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay<List<StatisticsOutputDTO>> responseDataNewPay = newPayService.statistics(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						" 查询成功");
				responseData.setData(responseDataNewPay.getData());
			} else {
				// 失败时候 msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setData(null);
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.error("NewPayController execute statistics fail : ", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.4.1 新支付下发记录
	@RequestMapping("/find4WByCondition")
	public String find4WByCondition(@Valid @RequestBody Find4WByConditionInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<List<Find4WByConditionOutputDTO>> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay<PageOutputDTO<Find4WByConditionOutputDTO>> responseDataNewPay = newPayService
					.find4WByCondition(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
				Paging paging = wrapPage(inputDTO.getPageSize(), inputDTO.getPageNo() - 1,
						responseDataNewPay.getData().getTotalRecordNumber());
				Map map = new HashMap();
				map.put("other", responseDataNewPay.getData().getOther());
				paging.setHeader(map);
				responseData.setPage(paging);
				responseData.setData(responseDataNewPay.getData().getResultList());
			} else {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setPage(new Paging());
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute find4WByCondition fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.4.2 返佣规则 – 查询分页列表
	@RequestMapping("/findCRByCondition")
	public String findCRByCondition(@Valid @RequestBody FindCRByConditionInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<List<FindCRByConditionOutputDTO>> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay<PageOutputDTO<FindCRByConditionOutputDTO>> responseDataNewPay = newPayService
					.findCRByCondition(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "查询成功");
				Paging paging = wrapPage(inputDTO.getPageSize(), inputDTO.getPageNo() - 1,
						responseDataNewPay.getData().getTotalRecordNumber());
				Map map = new HashMap();
				map.put("other", responseDataNewPay.getData().getOther());
				paging.setHeader(map);
				responseData.setPage(paging);
				responseData.setData(responseDataNewPay.getData().getResultList());
			} else {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"查询失败:" + (responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setMessage((responseDataNewPay != null ? responseDataNewPay.getMsg() : "null"));
				responseData.setPage(new Paging());
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute findCRByCondition fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"查询失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.4.3 返佣规则 – 新增
	@RequestMapping("/addCR")
	public String addCR(@Valid @RequestBody AddCRInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<AddCROutputDTO> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		try {
			inputDTO.setOperationAdminId(sysUser.getId().longValue());
			inputDTO.setOperationAdminName(sysUser.getUid());
			ResponseDataNewPay<AddCROutputDTO> responseDataNewPay = newPayService.addCR(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						" 新增成功");
				responseData.setData(responseDataNewPay.getData());
			} else {
				// 失败时候 msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"新增失败:" + responseDataNewPay.getMsg());
				responseData.setMessage(responseDataNewPay.getMsg());
				responseData.setData(null);
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute addCR fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"新增失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.4.4 返佣规则 – 修改
	@RequestMapping("/modifyCR")
	public String modifyCR(@Valid @RequestBody ModifyCRInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<ModifyCROutputDTO> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		try {
			inputDTO.setOperationAdminId(sysUser.getId().longValue());
			inputDTO.setOperationAdminName(sysUser.getUid());
			ResponseDataNewPay<ModifyCROutputDTO> responseDataNewPay = newPayService.modifyCR(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						" 修改成功");
				responseData.setData(responseDataNewPay.getData());
			} else {
				// 失败时候 msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"修改失败:" + responseDataNewPay.getMsg());
				responseData.setMessage(responseDataNewPay.getMsg());
				responseData.setData(null);
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute modifyCR fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"修改失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.4.5 返佣规则 – 删除
	@RequestMapping("/removeCR")
	public String removeCR(@Valid @RequestBody RemoveCRInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<String> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		try {
			inputDTO.setOperationAdminId(sysUser.getId().longValue());
			inputDTO.setOperationAdminName(sysUser.getUid());
			ResponseDataNewPay responseDataNewPay = newPayService.removeCR(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						" 删除成功");
				responseData.setData(responseDataNewPay.getData().toString());
			} else {
				// 失败时候 msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"删除失败:" + responseDataNewPay.getMsg());
				responseData.setMessage(responseDataNewPay.getMsg());
				responseData.setData(null);
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute removeCR fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"删除失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.6.1 结果确认
	@RequestMapping("/confirm")
	public String confirm(@Valid @RequestBody ConfirmInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<String> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay responseDataNewPay = newPayService.confirm(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						" 确认成功");
				responseData.setData(responseDataNewPay.getData().toString());
			} else {
				// 失败时候 msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"确认失败:" + responseDataNewPay.getMsg());
				responseData.setMessage(responseDataNewPay.getMsg());
				responseData.setData(null);
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute confirm fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"确认失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.6.2 重置信用额度
	@RequestMapping("/reset")
	public String reset(@Valid @RequestBody ResetInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<String> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay responseDataNewPay = newPayService.reset(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						"重置信用额度成功");
				responseData.setData(responseDataNewPay.getData().toString());
			} else {
				// 失败时候 msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"重置信用额度失败:" + responseDataNewPay.getMsg());
				responseData.setMessage(responseDataNewPay.getMsg());
				responseData.setData(null);
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute reset fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"重置信用额度失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.6.3 自动重置信用额度
	@RequestMapping("/autoReset")
	public String autoReset(@Valid @RequestBody AutoResetInputDTO inputDTO, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<String> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		try {
			ResponseDataNewPay responseDataNewPay = newPayService.autoReset(inputDTO);
			if (responseDataNewPay != null && responseDataNewPay.getCode() == 200) {
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(),
						"autoReset succeeded");
				responseData.setData(responseDataNewPay.getData().toString());
			} else {
				// 失败时候 msg不为null
				responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
						"autoReset failed:" + responseDataNewPay.getMsg());
				responseData.setMessage(responseDataNewPay.getMsg());
				responseData.setData(null);
			}
			return mapper.writeValueAsString(responseData);
		} catch (Exception e) {
			LOGGER.info("NewPayController execute autoReset fail :{}", e.getLocalizedMessage());
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"重置信用额度失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}
	}

	// 1.6.4 银行卡-修改状态--删除/批量删除
	@RequestMapping("/modifyBindCardStatus2")
	public String modifyBindCardStatus2(@Valid @RequestBody List<BizAccount> inputDTOList, BindingResult result)
			throws JsonProcessingException {
		GeneralResponseData<List<String>> responseData;
		if (result.hasErrors()) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "参数校验不通过");
			return mapper.writeValueAsString(responseData);
		}
		SysUser sysUser = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (sysUser == null) {
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(), "重新登陆");
			return mapper.writeValueAsString(responseData);
		}
		try {
			Map<Integer, ModifyBindCardStatus2InputDTO> map = newPayServiceimpl
					.wrapModifyBindCardStatus2InputDTO(inputDTOList, sysUser);
			List<String> ret = new ArrayList<>();
			for (Map.Entry<Integer, ModifyBindCardStatus2InputDTO> entry : map.entrySet()) {
				ModifyBindCardStatus2InputDTO inputDTO = entry.getValue();
				ResponseDataNewPay res = newPayService.modifyBindCardStatus2(inputDTO);// 对于失败和成功结果无法感知
				if (res.getCode() != 200) {
					ret.add("修改盘口:" + inputDTO.getOid() + "状态结果:" + res.getMsg());
				}
			}
			String message = CollectionUtils.isEmpty(ret) ? "操作成功" : ret.toString();
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), message);
			return mapper.writeValueAsString(responseData);

		} catch (Exception e) {
			LOGGER.info("NewPayController execute modifyBindCardStatus2 fail :", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"修改状态失败:" + e.getLocalizedMessage());
			return mapper.writeValueAsString(responseData);
		}

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

	/**
	 * 
	 * @param small
	 *            小额部分的金额
	 * @param limit
	 *            金额最大值
	 * @param step
	 *            大于小额部分之后增加步长
	 * @param limeInclusive
	 *            是否包含金额最大值
	 * @return
	 */
	public static final Set<Double> generateAmountStr4QR(Integer small, Integer limit, Integer step,
			boolean limeInclusive) {
		if (small < 0 || limit < 0 || step < 0) {
			return new HashSet<>();
		}
		if (small >= limit) {
			return new HashSet<>();
		}
		int quotient = (limit - small) / step, remainder = (limit - small) % step;
		Double[] amountSet;
		if (remainder > 0 && limeInclusive) {
			amountSet = new Double[small + quotient + 1];
		} else {
			amountSet = new Double[small + quotient];
		}
		for (int i = 1; i < small + quotient + 1; i++) {
			if (i < small + 1) {
				amountSet[i - 1] = Double.valueOf(i);
			} else {
				amountSet[i - 1] = Double.valueOf(small + step * (i - small));
			}
		}
		if (remainder > 0 && limeInclusive) {
			amountSet[small + quotient] = Double.valueOf(limit);
		}
		return new LinkedHashSet<>(Arrays.asList(amountSet));
	}
}
