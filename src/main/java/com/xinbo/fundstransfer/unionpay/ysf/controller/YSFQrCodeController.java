/**
 *
 */
package com.xinbo.fundstransfer.unionpay.ysf.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.Paging;
import com.xinbo.fundstransfer.domain.ResponseData;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.BizOtherAccountBindEntity;
import com.xinbo.fundstransfer.domain.entity.BizOtherAccountEntity;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.enums.InBankSubType;
import com.xinbo.fundstransfer.domain.enums.OtherAccountOwnType;
import com.xinbo.fundstransfer.domain.enums.OtherAccountStatus;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.repository.AccountRepository;
import com.xinbo.fundstransfer.report.ObjectMapperUtils;
import com.xinbo.fundstransfer.restful.v2.TokenValidation;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.HandicapService;
import com.xinbo.fundstransfer.service.OtherAccountBindService;
import com.xinbo.fundstransfer.unionpay.ysf.entity.YSFQResponseEntity;
import com.xinbo.fundstransfer.unionpay.ysf.entity.YSFQRrequestEntity;
import com.xinbo.fundstransfer.unionpay.ysf.entity.YSFQrCodeEntity;
import com.xinbo.fundstransfer.unionpay.ysf.inputdto.CreateYSFAccountInputDTO;
import com.xinbo.fundstransfer.unionpay.ysf.inputdto.CreateYSFAccountOutputDTO;
import com.xinbo.fundstransfer.unionpay.ysf.inputdto.InAccountBindedYSFInputDTO;
import com.xinbo.fundstransfer.unionpay.ysf.inputdto.QueryYSFInputDTO;
import com.xinbo.fundstransfer.unionpay.ysf.inputdto.UpdateYSFBasicInfoInputDTO;
import com.xinbo.fundstransfer.unionpay.ysf.inputdto.UpdateYSFBindAccountInputDTO;
import com.xinbo.fundstransfer.unionpay.ysf.inputdto.UpdateYSFPWDInputDTO;
import com.xinbo.fundstransfer.unionpay.ysf.inputdto.YSFGenQrCodeReqeustDto;
import com.xinbo.fundstransfer.unionpay.ysf.outputdto.QueryYSFOutputDTO;
import com.xinbo.fundstransfer.unionpay.ysf.outputdto.YSFGenerateQRRequestDTO;
import com.xinbo.fundstransfer.unionpay.ysf.outputdto.YSFQrCodeQueryDto;
import com.xinbo.fundstransfer.unionpay.ysf.service.YSFService;

import lombok.extern.slf4j.Slf4j;

/**
 * 银联云闪付-获取二维码 controller
 *
 * @author blake
 *
 */
@Slf4j
@RestController
@RequestMapping(value = "/ysfQrcode")
public class YSFQrCodeController extends TokenValidation {

	@Autowired
	private YSFService service;
	@Autowired
	private HandicapService handicapService;
	@Autowired
	private AccountRepository accountRepository;
	@Autowired
	private OtherAccountBindService bindingService;
	@Autowired
	private AccountService accountService;

	@Value("${funds.transfer.cabanasalt}")
	private String cabanasalt;

	@PostMapping("/getYSFLogInfo")
	public GeneralResponseData getYSFLogInfo(@Validated @RequestBody Map bankAccount, Errors error) {
		if (error.hasErrors()) {
			return new GeneralResponseData(-1, "参数校验不通过!");
		}
		AccountBaseInfo baseInfo = accountService.getFromCacheByTypeAndAccount(AccountType.InBank.getTypeId(),
				bankAccount.get("account").toString());
		if (ObjectUtils.isEmpty(baseInfo)) {
			return new GeneralResponseData(-1, "账号不存在或不属于云闪付绑定的银行卡!");
		}
		Map map = service.findOtherAccountByBankAccount(bankAccount.get("account").toString());
		GeneralResponseData res = new GeneralResponseData(1, "更新成功!");
		res.setData(map);
		return res;
	}

	@PostMapping("/updateYSFPwd")
	public GeneralResponseData updateYSFPwd(@Validated @RequestBody UpdateYSFPWDInputDTO inputDTO, Errors error) {
		try {
			if (error.hasErrors()) {
				return new GeneralResponseData(-1, "参数校验不通过!");
			}
			service.updateYSFPwd(inputDTO);
			return new GeneralResponseData(1, "更新成功!");
		} catch (Exception e) {
			log.error(String.format("更新失败,参数:%s,异常:%s", ObjectMapperUtils.serializeNonNull(inputDTO),
					e.getLocalizedMessage()));
			return new GeneralResponseData(-1, "更新失败!" + e.getLocalizedMessage());
		}
	}

	@PostMapping("/updateYSFBasicInfo")
	public GeneralResponseData updateYSFBasicInfo(@Validated @RequestBody UpdateYSFBasicInfoInputDTO inputDTO,
												  Errors error) {
		try {
			if (error.hasErrors()) {
				return new GeneralResponseData(-1, "参数校验不通过!");
			}
			service.updateYSFBasicInfo(inputDTO);
			return new GeneralResponseData(1, "更新成功!");
		} catch (Exception e) {
			log.error(String.format("更新失败,参数:%s,异常:%s", ObjectMapperUtils.serializeNonNull(inputDTO),
					e.getLocalizedMessage()));
			return new GeneralResponseData(-1, "更新失败!" + e.getLocalizedMessage());
		}
	}

	@PostMapping("/updateYSFBindAccount")
	public GeneralResponseData updateYSFBindAccount(@Validated @RequestBody UpdateYSFBindAccountInputDTO inputDTO,
													Errors error) {
		try {
			if (error.hasErrors()) {
				return new GeneralResponseData(-1, "参数校验不通过!");
			}
			service.updateYSFBindAccount(inputDTO);
			return new GeneralResponseData(1, "更新成功!");
		} catch (Exception e) {
			log.error(String.format("更新失败,参数:%s,异常:%s", ObjectMapperUtils.serializeNonNull(inputDTO),
					e.getLocalizedMessage()));
			return new GeneralResponseData(-1, "更新失败!" + e.getLocalizedMessage());
		}
	}

	/**
	 * 描述:根据某个云闪付账号查询绑定银行卡信息
	 *
	 * @param id
	 *            云闪付账号id
	 * @return
	 */
	@GetMapping("/getBindAccountInfo")
	public GeneralResponseData getBindAccountInfo(@RequestParam(value = "ysfId") Integer id) {
		try {
			List<BizOtherAccountBindEntity> bindEntityList = bindingService.findByOtherAccountId(id);
			if (CollectionUtils.isEmpty(bindEntityList)) {
				return new GeneralResponseData(1, "没有绑定信息");
			}
			ArrayList list = new ArrayList() {
				{
					addAll(bindEntityList.stream().map(p -> p.getAccountId()).collect(Collectors.toList()));
				}
			};
			List<BizAccount> accountList = accountService.findByIds(list);
			return new GeneralResponseData(1, "查询成功") {
				{
					setData(accountList);
				}
			};
		} catch (Exception e) {
			log.error("查询云闪付绑定账号明细失败:", e);
			return new GeneralResponseData(-1, "查询绑定信息失败:" + e.getLocalizedMessage());
		}
	}

	/**
	 * 描述:请求app生成二维码
	 *
	 * @param requestEntity
	 * @return
	 */
	@PostMapping("/callQR")
	public ResponseData<?> call4GenerateQRs(@RequestBody YSFQRrequestEntity requestEntity) {
		try {
			ResponseData<?> res = service.call4GenerateQRs(requestEntity);
			return new ResponseData(1, String.format("测试成功结果:%s", res.getMessage()));
		} catch (Exception e) {
			log.error("请求二维码测试失败:", e);
			return new ResponseData(-1, "测试失败!" + e.getLocalizedMessage());
		}
	}

	/**
	 * 描述:收到二维码
	 *
	 * @param responseEntity
	 * @return
	 */
	@PostMapping("/call4SendQRs")
	public ResponseData<?> receiveCabanaSendQRs(@RequestBody YSFQResponseEntity responseEntity) {
		try {
			log.debug("收到二维码信息:{}", responseEntity.toString());
			TreeMap param = new TreeMap() {
				{
					put("bindedBankAccount", responseEntity.getBankAccount());
					put("amount", responseEntity.getAmount());
				}
			};
			String expectToken = md5digest(param, cabanasalt);
			if (!Objects.equals(expectToken, responseEntity.getToken())) {
				log.info("收到二维码信息token:{}与计算值的token:{} ", responseEntity.getToken());
				return new ResponseData(-1,
						String.format("发送的token:%s与fund计算token:%s,不一致!", responseEntity.getToken(), expectToken));
			}
			YSFQrCodeEntity qrCode = new YSFQrCodeEntity();
			// DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-DD HH:mm:ss");
			// Date date = new Date(Long.valueOf(responseEntity.getDate()));
			// String dateStr = dateFormat.format(date);
			qrCode.setQrContent(responseEntity.getQrStreams());
			qrCode.setBindedBankAccount(responseEntity.getBankAccount());
			qrCode.setMoney(new BigDecimal(responseEntity.getAmount()));
			qrCode.setGenDate(Long.valueOf(responseEntity.getDate()));
			qrCode.setYsfAccount(responseEntity.getYsfAccount());
			service.saveQrCode(qrCode);
			// service.receiveCabanaSendQRs(responseEntity);
			return new ResponseData(1, String.format("保存二维码结果:%s", responseEntity.toString()));
		} catch (Exception e) {
			log.error("请求二维码测试失败:", e);
			return new ResponseData(-1, "保存二维码失败!" + e.getMessage());
		}
	}

	/**
	 * 描述:分页查询云闪付信息
	 *
	 * @param inputDTO
	 * @param bindingResult
	 * @return
	 */
	@RequestMapping(value = "/list", method = { RequestMethod.GET, RequestMethod.POST })
	public GeneralResponseData list(@Valid @RequestBody QueryYSFInputDTO inputDTO, BindingResult bindingResult) {
		try {
			if (bindingResult.hasErrors()) {
				return new GeneralResponseData(-1, "分页参数必传!");
			}
			GeneralResponseData<List<QueryYSFOutputDTO>> res = new GeneralResponseData<>(1, "查询成功");
			Page<BizOtherAccountEntity> entityPage = service.findPageByCriteria(inputDTO);
			if (Objects.isNull(entityPage)) {
				return new GeneralResponseData(1, "无数据!");
			}
			List<QueryYSFOutputDTO> data = new ArrayList<>();
			for (BizOtherAccountEntity entity : entityPage.getContent()) {
				QueryYSFOutputDTO outputDTO = new QueryYSFOutputDTO();
				outputDTO.setAccountNo(entity.getAccountNo());
				outputDTO.setCreateTime(entity.getCreateTime());
				outputDTO.setHandicap(handicapService.findFromCacheById(entity.getHandicapId()).getName());
				outputDTO.setUpdateTime(entity.getUpdateTime());
				outputDTO.setRemark(entity.getRemark());
				outputDTO.setStatusDesc(OtherAccountStatus.getByStatus(entity.getStatus()).getStatusDesc());
				outputDTO.setOwnTypeDesc(OtherAccountOwnType.getByOwnType(entity.getOwnType()).getOwnTypeDesc());
				outputDTO.setStatus(entity.getStatus());
				outputDTO.setOwnType(entity.getOwnType());
				outputDTO.setOwner(entity.getOwner());
				outputDTO.setId(entity.getId());
				/** 登陆密码 */
				outputDTO.setLoginPWD(StringUtils.isBlank(entity.getLoginPWD()) ? null : "****");
				/** 支付密码 */
				outputDTO.setPayPWD(StringUtils.isBlank(entity.getPayPWD()) ? null : "****");
				List<BizOtherAccountBindEntity> bindEntityList = bindingService.findByOtherAccountId(entity.getId());
				if (!ObjectUtils.isEmpty(bindEntityList)) {
					outputDTO.setBindAccountIds(
							bindEntityList.stream().map(p -> p.getAccountId()).collect(Collectors.toList()));
					outputDTO.setBindAccountList(
							accountService.findByIds(new ArrayList<>(outputDTO.getBindAccountIds())));
				}
				data.add(outputDTO);
			}
			res.setPage(new Paging(entityPage));
			res.setData(data);
			return res;
		} catch (Exception e) {
			log.error("查询失败:", e);
			return new GeneralResponseData(-1, "查询失败!" + e.getLocalizedMessage());
		}
	}

	/**
	 * 描述:新增云闪付账号
	 *
	 * @param inputDTO
	 * @param bindingResult
	 * @return
	 */
	@PostMapping("/addYSF")
	public GeneralResponseData addYSF(@Valid @RequestBody CreateYSFAccountInputDTO inputDTO,
									  BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			return new GeneralResponseData(-1, "参数校验不通过!");
		}
		try {
			if (Objects.nonNull(service.findByAccountNo(inputDTO.getYsfAccountInputDTO().getAccountNo()))) {
				return new GeneralResponseData(-1,
						String.format("云闪付账号:%s已存在!", inputDTO.getYsfAccountInputDTO().getAccountNo()));
			}
			Integer handicapId = inputDTO.getYsfAccountInputDTO().getHandicapId();
			BizHandicap handicap = handicapService.findFromCacheById(handicapId);
			if (Objects.isNull(handicap)) {
				return new GeneralResponseData(-1, String.format("盘口ID:%s 不存在! ", handicapId));
			}
			if (!CollectionUtils.isEmpty(inputDTO.getInAccounts())) {
				for (InAccountBindedYSFInputDTO account : inputDTO.getInAccounts()) {
					account.setHandicapId(handicapId);
					account.setHandicap(handicap.getCode());
					if (Objects.isNull(account.getType()) || Objects.isNull(account.getAccount())
							|| Objects.isNull(account.getBankType()) || Objects.isNull(account.getOwner())
							|| Objects.isNull(account.getHandicap()) || Objects.isNull(account.getLevelIds())) {
						return new GeneralResponseData(-1, "绑定的银行卡参数校验不通过!");
					}
					BizAccount account1 = accountRepository.findByAccountAndHandicapIdAndBankType(account.getAccount(),
							handicap.getId(), account.getBankType());
					if (Objects.nonNull(account1)) {
						return new GeneralResponseData(-1, String.format("银行卡号:%s,已存在,不能新增! ", account.getAccount()));
					}
				}
			}
			CreateYSFAccountOutputDTO outputDTO = service.add(inputDTO.getYsfAccountInputDTO(),
					inputDTO.getInAccounts());
			GeneralResponseData res = new GeneralResponseData(1, "添加成功!");
			res.setData(outputDTO);
			return res;
		} catch (Exception e) {
			log.error("新增云闪付账号失败:", e);
			return new GeneralResponseData(-1, "新增操作失败!" + e.getLocalizedMessage());

		}
	}

	/**
	 * 描述:编辑更新云闪付信息 解绑 绑定银行卡 修改银行卡信息在另外的逻辑
	 *
	 * @param inputDTO
	 * @param bindingResult
	 * @return
	 */
	@PostMapping("/updateYSF")
	public GeneralResponseData updateYSF(@Validated @RequestBody CreateYSFAccountInputDTO inputDTO,
										 BindingResult bindingResult) {
		try {
			if (bindingResult.hasErrors()) {
				return new GeneralResponseData(-1, "参数校验不通过!");
			}
			BizOtherAccountEntity entity = service.findById(inputDTO.getYsfAccountInputDTO().getId());
			if (Objects.isNull(entity)) {
				return new GeneralResponseData(-1,
						String.format("云闪付账号:%s不存在!", inputDTO.getYsfAccountInputDTO().getAccountNo()));
			}
			if (!entity.getAccountNo().equals(inputDTO.getYsfAccountInputDTO().getAccountNo())) {
				BizOtherAccountEntity entity1 = service
						.findByAccountNo(inputDTO.getYsfAccountInputDTO().getAccountNo());
				if (Objects.nonNull(entity1)) {
					return new GeneralResponseData(-1,
							String.format("更新的云闪付账号:%s已存在!", inputDTO.getYsfAccountInputDTO().getAccountNo()));
				}
			}
			BizHandicap handicap = handicapService.findFromCacheById(inputDTO.getYsfAccountInputDTO().getHandicapId());
			if (Objects.isNull(handicap)) {
				return new GeneralResponseData(-1,
						String.format("更改的盘口:%s 不存在! ", inputDTO.getYsfAccountInputDTO().getHandicapId()));
			}
			// 原有绑定银行卡 新增绑定银行卡
			List<InAccountBindedYSFInputDTO> oldAccountList = new ArrayList<>(), newAccountList = new ArrayList<>();
			if (!CollectionUtils.isEmpty(inputDTO.getInAccounts())) {
				for (InAccountBindedYSFInputDTO account : inputDTO.getInAccounts()) {
					account.setHandicap(handicap.getCode());
					account.setHandicapId(handicap.getId());
					if (Objects.isNull(account.getType()) || Objects.isNull(account.getAccount())
							|| Objects.isNull(account.getBankType()) || Objects.isNull(account.getOwner())
							|| Objects.isNull(account.getHandicap()) || Objects.isNull(account.getLevelIds())) {
						return new GeneralResponseData(-1, "更新绑定的银行卡参数校验不通过!");
					}
					BizAccount account1 = accountRepository.findByAccountAndHandicapIdAndBankType(account.getAccount(),
							handicap.getId(), account.getBankType());
					BizOtherAccountBindEntity bizAccountBinding = bindingService
							.findByAccountIdAndBindId(inputDTO.getYsfAccountInputDTO().getId(), account.getId());
					if (Objects.nonNull(account1)) {
						if (Objects.isNull(bizAccountBinding)) {
							return new GeneralResponseData(-1, String.format("银行卡号:%s,类型:%s,所属盘口:%s 已存在! ",
									account.getAccount(), account.getBankType(), account.getHandicap()));
						} else {
							// 原有的绑定银行卡校验是否被更改过
							if (Objects.nonNull(account1.getType())
									&& !account1.getType().equals(AccountType.InBank.getTypeId())) {
								return new GeneralResponseData(-1, String.format("银行卡号:%s不属于入款银行卡! ",
										account.getAccount(), account.getType(), account.getHandicap()));
							}
							if (Objects.nonNull(account1.getSubType())
									&& !account1.getSubType().equals(InBankSubType.IN_BANK_YSF.getSubType())) {
								return new GeneralResponseData(-1, String.format("银行卡号:%s属于其他类型入款银行卡不能绑定为云闪付入款卡! ",
										account.getAccount(), account.getSubType(), account.getHandicap()));
							}
							// 原有的绑定
							oldAccountList.add(account);
						}
					} else {
						// 新增的银行卡
						newAccountList.add(account);
					}
				}
			}
			CreateYSFAccountOutputDTO outputDTO = service.update(inputDTO.getYsfAccountInputDTO(), entity,
					oldAccountList, newAccountList);
			GeneralResponseData res = new GeneralResponseData(1, "更新成功!");
			res.setData(outputDTO);
			return res;
		} catch (Exception e) {
			log.error("更新失败:", e);
			return new GeneralResponseData(-1, "更新操作失败!" + e.getLocalizedMessage());
		}
	}

	/**
	 * 描述：更新云闪付状态
	 *
	 * @param map
	 */
	@PostMapping("/updateYSFStatus")
	public GeneralResponseData updateYSFStatus(@RequestBody Map map) {
		try {
			Integer id = Objects.nonNull(map.get("id")) ? (Integer) map.get("id") : null;
			Byte status = Objects.nonNull(map.get("status")) ? Byte.valueOf(map.get("status").toString()) : null;
			String remark = Objects.nonNull(map.get("remark")) ? (String) map.get("remark") : null;
			if (Objects.isNull(id)) {
				return new GeneralResponseData(-1, "参数id必传!");
			}
			if (Objects.isNull(OtherAccountStatus.getByStatus(status))) {
				return new GeneralResponseData(-1, "更新的状态不符合要求!");
			}
			if (Objects.isNull(status) && StringUtils.isBlank(remark)) {
				return new GeneralResponseData(1, "操作成功,更新状态不变!");
			}
			if (Objects.isNull(service.findById(id))) {
				return new GeneralResponseData(-1, "账号不存在无法更新状态!");
			}
			service.updateYSFStatus(id, status, remark);
			return new GeneralResponseData(1, " 更新状态成功!");
		} catch (Exception e) {
			log.error("更新状态操作失败:", e);
			return new GeneralResponseData(-1, "更新状态操作失败!" + e.getLocalizedMessage());
		}
	}

	/**
	 * 描述:删除云闪付账号信息
	 *
	 * @param id
	 * @return
	 */
	@DeleteMapping("/deleteYSF/{id}")
	public GeneralResponseData deleteYSF(@PathVariable("id") Integer id) {
		try {
			if (Objects.isNull(id)) {
				return new GeneralResponseData(-1, "参数id必传!");
			}
			if (Objects.isNull(service.findById(id))) {
				return new GeneralResponseData(1, "该账号已删除!");
			}
			service.deleteYSF(id);
			return new GeneralResponseData(1, "删除成功!");
		} catch (Exception e) {
			log.error("删除操作失败:", e);
			return new GeneralResponseData(-1, "删除操作失败!" + e.getLocalizedMessage());
		}
	}

	/**
	 * 记录平台请求信息
	 *
	 * @param param
	 * @param request
	 */
	private void logComfirmRequest(Object param, HttpServletRequest request) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			log.error(String.format("参数：%s", mapper.writeValueAsString(param)));
			log.error(String.format("请求地址：%s", request.getServletPath()));
			log.error(String.format("远程主机remoteAddr：%s", request.getRemoteAddr()));
			log.error(String.format("远程主机host：%s", request.getRemoteHost()));
			log.error(String.format("远程主机端口：%s", request.getRemotePort()));
			log.error(String.format("远程主机用户：%s", request.getRemoteUser()));
			Enumeration<String> headerNames = request.getHeaderNames();
			while (headerNames.hasMoreElements()) {
				String nextHeaderKey = headerNames.nextElement();
				log.error(String.format("请求头：%s : %s", nextHeaderKey, request.getHeader(nextHeaderKey)));
			}
			log.error(String.format("Referer:  %s", request.getHeader("Referer")));
		} catch (Exception e) {
			log.error("记录平台请求银联云闪付二维码参数时发生错误！！！！", e);
		}
	}

	/**
	 * 保存app生成的二维码
	 *
	 * @param qrCode
	 *            请求参数，必填
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/saveQrCode", method = RequestMethod.POST)
	@ResponseBody
	public GeneralResponseData saveQrCode(@RequestBody YSFQrCodeEntity qrCode, HttpServletRequest request) {
		this.logComfirmRequest(qrCode, request);
		GeneralResponseData<Boolean> responseData = null;
		try {
			Boolean saveFlag = service.saveQrCode(qrCode);
			responseData = new GeneralResponseData<Boolean>();
			responseData.setStatus(GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			responseData.setData(saveFlag);
			return responseData;
		} catch (Exception e) {
			log.error("ysfQrcode.get   fail : ", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"保存app生成的二维码失败:" + e.getLocalizedMessage());
			responseData.setData(false);
			return responseData;
		}
	}

	/**
	 * 获取需要生成二维码的随机数
	 *
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/getYSFRandomMoney", method = RequestMethod.POST)
	@ResponseBody
	public GeneralResponseData<YSFGenerateQRRequestDTO> getYSFRandomMoney(@RequestBody YSFGenQrCodeReqeustDto param,
																		  HttpServletRequest request) {
		this.logComfirmRequest(param, request);
		GeneralResponseData<YSFGenerateQRRequestDTO> responseData = null;
		try {
			YSFGenerateQRRequestDTO dto = service.getYSFRandomMoney(param);
			responseData = new GeneralResponseData<YSFGenerateQRRequestDTO>();
			responseData.setStatus(GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			responseData.setData(dto);
			return responseData;
		} catch (Exception e) {
			log.error("ysfQrcode.get   fail : ", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"获取需要生成的二维码随机数失败:" + e.getLocalizedMessage());
			return responseData;
		}
	}

	/**
	 * 描述:平台云闪付绑定银行卡的时候校验接口
	 *
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/checkBankAccountToBind", method = RequestMethod.POST)
	public GeneralResponseData<String> checkBankAccountToBind(HttpServletRequest request) {
		this.logComfirmRequest(null, request);
		GeneralResponseData<String> responseData;
		try {
			String param = request.getParameter("accounts");
			if (Objects.isNull(param)) {
				return new GeneralResponseData(-1, "参数accounts必传!");
			}
			String param2 = request.getParameter("oid");
			if (Objects.isNull(param)) {
				return new GeneralResponseData(-1, "参数oid必传!");
			}
			BizHandicap handicap = handicapService.findFromCacheByCode(param2);
			if (Objects.isNull(handicap)) {
				return new GeneralResponseData(-1, "参数oid:{}盘口信息不存在!");
			}
			log.info("绑定银行卡账号校验参数:oid:{},accounts:{}", param2, param);
			Map map = new HashMap();
			map.put("accounts", param);
			map.put("oid", handicap.getId());
			String res = service.checkBankAccountToBind(map);
			responseData = new GeneralResponseData(GeneralResponseData.ResponseStatus.SUCCESS.getValue(), "请求成功");
			responseData.setData(res);
			return responseData;
		} catch (Exception e) {
			log.error("checkBankAccountToBind   fail : ", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"绑定银行卡账号校验失败:" + e.getLocalizedMessage());
			return responseData;
		}
	}

	/**
	 * 描述:平台请求获取云闪付配置的常用金额（前端用户申请入款的可填入金额）
	 *
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/allowAmount", method = RequestMethod.GET)
	public GeneralResponseData<List<Integer>> allowAmount() {
		GeneralResponseData<List<Integer>> responseData = null;
		try {
			List<Integer> result = service.getYSFAllowMoney();
			responseData = new GeneralResponseData<List<Integer>>();
			responseData.setStatus(GeneralResponseData.ResponseStatus.SUCCESS.getValue());
			responseData.setData(result);
			return responseData;
		} catch (Exception e) {
			log.error("ysfQrcode.allowAmount   fail : ", e);
			responseData = new GeneralResponseData<>(GeneralResponseData.ResponseStatus.FAIL.getValue(),
					"获取云闪付配置的常用金额失败:" + e.getLocalizedMessage());
			return responseData;
		}
	}

	/**
	 * 描述:查询云闪付收款银行卡号今日已生成的二维码
	 *
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "/query/{bankAccount}", method = RequestMethod.GET)
	public GeneralResponseData<List<YSFQrCodeQueryDto>> allowAmount(@PathVariable("bankAccount") String bankAccount) {
		GeneralResponseData<List<YSFQrCodeQueryDto>> responseData = new GeneralResponseData<List<YSFQrCodeQueryDto>>();
		List<YSFQrCodeQueryDto> result = service.queryByBankAccount(bankAccount);
		responseData = new GeneralResponseData<List<YSFQrCodeQueryDto>>();
		responseData.setStatus(GeneralResponseData.ResponseStatus.SUCCESS.getValue());
		responseData.setData(result);
		return responseData;
	}

}
