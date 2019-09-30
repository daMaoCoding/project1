package com.xinbo.fundstransfer.newpay.serviceImpl;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.xinbo.fundstransfer.component.net.http.RequestBodyParser;
import com.xinbo.fundstransfer.component.net.http.newpay.HttpClient4NewPay;
import com.xinbo.fundstransfer.component.net.http.newpay.PlatformNewPayService;
import com.xinbo.fundstransfer.component.net.http.v2.HttpClientNew;
import com.xinbo.fundstransfer.domain.ResponseDataNewPay;
import com.xinbo.fundstransfer.domain.entity.BizAccount;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.domain.enums.AccountStatus;
import com.xinbo.fundstransfer.domain.enums.AccountType;
import com.xinbo.fundstransfer.domain.pojo.AccountBaseInfo;
import com.xinbo.fundstransfer.domain.repository.HandicapRepository;
import com.xinbo.fundstransfer.newpay.RequestBodyNewPay;
import com.xinbo.fundstransfer.newpay.inputdto.*;
import com.xinbo.fundstransfer.newpay.inputdto.ModifyBindCardStatus2InputDTO.CardPayee;
import com.xinbo.fundstransfer.newpay.outdto.*;
import com.xinbo.fundstransfer.newpay.service.NewPayService;
import com.xinbo.fundstransfer.service.AccountService;
import com.xinbo.fundstransfer.service.HandicapService;

import lombok.extern.slf4j.Slf4j;
import okhttp3.RequestBody;
import rx.Observable;

/**
 * Created by Administrator on 2018/7/11.
 */
@Service
@Slf4j
public class NewPayServiceImpl implements NewPayService {
	@Autowired
	private RequestBodyNewPay requestBodyNewPay;
	@Autowired
	RequestBodyParser requestBodyParser;
	@Autowired @Lazy
	AccountService accountService;
	@Autowired
	private HandicapService handicapService;
	@Autowired
	private HandicapRepository handicapRepository;

	// 新增客户资料
	@Override
	public ResponseDataNewPay<AddNewPayOutputDTO> add(AddNewPayInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<AddNewPayOutputDTO>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.addRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<AddNewPayOutputDTO>> res = platformNewPayService.add(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute add success :code:{},msg:{}", ret.getCode(), ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute add fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute add fail : ", e);
		}
		return threadLocal.get();
	}

	// 修改基本信息
	@Override
	public ResponseDataNewPay<ModifyInfoOutputDTO> modifyInfo(ModifyInfoInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<ModifyInfoOutputDTO>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.modifyInfoRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<ModifyInfoOutputDTO>> res = platformNewPayService.modifyInfo(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute modifyInfo success :code:{},msg:{}", ret.getCode(), ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute modifyInfo fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute modifyInfo fail : ", e);
		}
		return threadLocal.get();
	}

	// 修改账号资料
	@Override
	public ResponseDataNewPay<ModifyAccountOutputDTO> modifyAccount(ModifyAccountInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<ModifyAccountOutputDTO>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.modifyAccountRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<ModifyAccountOutputDTO>> res = platformNewPayService
					.modifyAccount(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute modifyAccount success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute modifyAccount fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute modifyAccount fail : ", e);
		}
		return threadLocal.get();
	}

	// 修改状态
	@Override
	public ResponseDataNewPay<ModifyStatusOutputDTO> modifyStatus(ModifyStatusInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<ModifyStatusOutputDTO>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.modifyStatusRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<ModifyStatusOutputDTO>> res = platformNewPayService.modifyStatus(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute modifyStatus success :code:{},msg:{}", ret.getCode(), ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute modifyStatus fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute modifyStatus fail :{}", e);
		}
		return threadLocal.get();
	}

	// 修改密码
	@Override
	public ResponseDataNewPay modifyPwd(ModifyPWDInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.modifyPwdRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay> res = platformNewPayService.modifyPwd(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute modifyPwd success :code:{},msg:{}", ret.getCode(), ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute modifyPwd fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute modifyPwd fail :{}", e);
		}
		return threadLocal.get();
	}

	// 删除
	@Override
	public ResponseDataNewPay remove(RemoveInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.removeRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay> res = platformNewPayService.remove(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute remove success :code:{},msg:{}", ret.getCode(), ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute remove fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute remove fail : ", e);
		}
		return threadLocal.get();
	}

	// 条件查询分页列表
	@Override
	public ResponseDataNewPay<PageOutputDTO<FindByConditionOutputDTO>> findByCondition(
			FindByConditionInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<PageOutputDTO<FindByConditionOutputDTO>>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.findByConditionRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<PageOutputDTO<FindByConditionOutputDTO>>> res = platformNewPayService
					.findByCondition(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute findByCondition success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute findByCondition fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute findByCondition fail : ", e);
		}
		return threadLocal.get();
	}

	// 查询微信、支付宝绑定的银行卡
	@Override
	public ResponseDataNewPay<FindBankOutputDTO> findBank(FindBankInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<FindBankOutputDTO>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.findBankRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<FindBankOutputDTO>> res = platformNewPayService.findBank(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute findBank success :code:{},msg:{}", ret.getCode(), ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute findBank fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute findBank fail : ", e);
		}
		return threadLocal.get();
	}

	// 查询兼职人员微信、支付宝、银行卡的余额、转入转出金额
	@Override
	public ResponseDataNewPay<List<FindBalanceInfoOutputDTO>> findBalanceInfo(FindBalanceInfoInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<List<FindBalanceInfoOutputDTO>>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.findBalanceInfoRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<List<FindBalanceInfoOutputDTO>>> res = platformNewPayService
					.findBalanceInfo(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute findBalanceInfo success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute findBalanceInfo fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute findBalanceInfo fail : ", e);
		}
		return threadLocal.get();
	}

	// 查询账号信息，account带*
	@Override
	public ResponseDataNewPay<FindAccountInfoOutputDTO> findAccountInfo(FindAccountInfoInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<FindAccountInfoOutputDTO>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.findAccountInfoRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<FindAccountInfoOutputDTO>> res = platformNewPayService
					.findAccountInfo(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute findAccountInfo success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute findAccountInfo fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute findAccountInfo fail : ", e);
		}
		return threadLocal.get();
	}

	// 查询账号信息，account没有带*
	@Override
	public ResponseDataNewPay<FindAccountInfoOutputDTO> findAccountInfo2(FindAccountInfoInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<FindAccountInfoOutputDTO>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.findAccountInfo2RequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<FindAccountInfoOutputDTO>> res = platformNewPayService
					.findAccountInfo2(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute findAccountInfo2 success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute findAccountInfo2 fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute findAccountInfo2 fail : ", e);
		}
		return threadLocal.get();
	}

	// 查询手机信息
	@Override
	public ResponseDataNewPay<FindTelInfoOutputDTO> findTelInfo(FindTelInfoInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<FindTelInfoOutputDTO>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.findTelInfoRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<FindTelInfoOutputDTO>> res = platformNewPayService.findTelInfo(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute findTelInfo success :code:{},msg:{}", ret.getCode(), ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute findTelInfo fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute findTelInfo fail : ", e);
		}
		return threadLocal.get();
	}

	// 点击今日收款/佣金列
	@Override
	public ResponseDataNewPay<PageOutputDTO<Find8ByConditionOutputDTO>> find8ByCondition(
			Find8ByConditionInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<PageOutputDTO<Find8ByConditionOutputDTO>>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.find8ByConditionRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<PageOutputDTO<Find8ByConditionOutputDTO>>> res = platformNewPayService
					.find8ByCondition(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute find8ByCondition success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute find8ByCondition fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute find8ByCondition fail : ", e);
		}
		return threadLocal.get();
	}

	// 点击今日收款/佣金列，返佣记录
	@Override
	public ResponseDataNewPay<PageOutputDTO<FindCommissionDetailOutputDTO>> findCommissionDetailByCondition(
			FindCommissionDetailInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<PageOutputDTO<FindCommissionDetailOutputDTO>>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.findCommissionDetailByConditionRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<PageOutputDTO<FindCommissionDetailOutputDTO>>> res = platformNewPayService
					.findCommissionDetailByCondition(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute findCommissionDetailByCondition success :code:{},msg:{}",
						ret.getCode(), ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute findCommissionDetailByCondition fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute findCommissionDetailByCondition fail : ", e);
		}
		return threadLocal.get();
	}

	// 查询二维码分页列表
	@Override
	public ResponseDataNewPay<PageOutputDTO<FindQRByConditionOutputDTO>> findQRByCondition(
			FindQRByConditionInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<PageOutputDTO<FindQRByConditionOutputDTO>>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.findQRByConditionRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<PageOutputDTO<FindQRByConditionOutputDTO>>> res = platformNewPayService
					.findQRByCondition(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute findQRByCondition success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute findQRByCondition fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute findQRByCondition fail : ", e);
		}
		return threadLocal.get();
	}

	// 批量生成二维码地址
	@Override
	public ResponseDataNewPay batchAddQR(BatchAddQRInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.batchAddQRRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay> res = platformNewPayService.batchAddQR(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute batchAddQR success :code:{},msg:{}", ret.getCode(), ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute batchAddQR fail : ", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute batchAddQR fail  :", e);
		}
		return threadLocal.get();
	}

	// 批量删除二维码
	@Override
	public ResponseDataNewPay batchDeleteQR(BatchDeleteQRInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.batchRemoveAnymoreRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay> res = platformNewPayService.batchDeleteQR(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute batchDeleteQR success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute batchDeleteQR fail : ", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute batchDeleteQR fail : ", e);
		}
		return threadLocal.get();
	}

	// 查询银行下拉列表
	@Override
	public ResponseDataNewPay<List<FindBankAllOutputDTO>> findBankAll() {
		ThreadLocal<ResponseDataNewPay<List<FindBankAllOutputDTO>>> threadLocal = new ThreadLocal<>();
		try {
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<List<FindBankAllOutputDTO>>> res = platformNewPayService.findBankAll();
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute findBankAll success :code:{},msg:{}", ret.getCode(), ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute findBankAll fail : ", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute findBankAll fail : ", e);
		}
		return threadLocal.get();
	}

	// 1.1.19 查询密码是否已被设置
	@Override
	public ResponseDataNewPay<FindPwdExistsOutputDTO> findPwdExists(FindPwdExistsInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<FindPwdExistsOutputDTO>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.findPwdExistsRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<FindPwdExistsOutputDTO>> res = platformNewPayService
					.findPwdExists(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute findPwdExists success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute findPwdExists fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute findPwdExists fail :{}", e);
		}
		return threadLocal.get();
	}

	// 1.1.20 修改收款理由前缀后缀
	@Override
	public ResponseDataNewPay<ModifyFixOutputDTO> modifyFix(ModifyFixInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<ModifyFixOutputDTO>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.modifyInfoRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<ModifyFixOutputDTO>> res = platformNewPayService.modifyFix(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute modifyFix success :code:{},msg:{}", ret.getCode(), ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute modifyFix fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute modifyInfo fail :{}", e);
		}
		return threadLocal.get();
	}

	// 1.1.21 查询业主未删除的新支付通道
	@Override
	public ResponseDataNewPay<List<FindPOCForCrkOutputDTO>> findPOCForCrk(FindPOCForCrkInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<List<FindPOCForCrkOutputDTO>>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.findPOCForCrkRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<List<FindPOCForCrkOutputDTO>>> res = platformNewPayService
					.findPOCForCrk(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute findPOCForCrk success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute findPOCForCrk fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute findPOCForCrk fail :{}", e);
		}
		return threadLocal.get();
	}

	// 1.1.22 绑定支付通道和客户资料
	@Override
	public ResponseDataNewPay newpayAisleConfigBind(NewpayAisleConfigBindInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.newpayAisleConfigBindRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay> res = platformNewPayService.newpayAisleConfigBind(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute newpayAisleConfigBind success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute newpayAisleConfigBind fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute newpayAisleConfigBind fail :{}", e);
		}
		return threadLocal.get();
	}

	// 1.1.23 查询客户资料已绑定的支付通道
	@Override
	public ResponseDataNewPay newpayAisleConfigFindBind(NewpayAisleConfigFindBindInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.newpayAisleConfigFindBindRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay> res = platformNewPayService.newpayAisleConfigFindBind(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute newpayAisleConfigFindBind success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute newpayAisleConfigFindBind fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute newpayAisleConfigFindBind fail :{}", e);
		}
		return threadLocal.get();
	}

	// 1.1.24 银行的余额同步
	@Override
	public ResponseDataNewPay syncBankBalance(SyncBankBalanceInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay> threadLocal = new ThreadLocal<>();
		try {
			log.info("syncBankBalance param :Oid : {},Account:{},Balance:{},SysBalance:{}", inputDTO.getOid(),
					inputDTO.getAccount(), inputDTO.getBalance(), inputDTO.getSysBalance());
			RequestBody requestBody = requestBodyNewPay.syncBankBalanceRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay> res = platformNewPayService.syncBankBalance(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute syncBankBalance success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute syncBankBalance fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute syncBankBalance fail : ", e);
		}
		return threadLocal.get();
	}

	// 1.1.25 形容词名词 - 新增
	@Override
	public ResponseDataNewPay<ContentOutputDTO> contentAdd(AddContentInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<ContentOutputDTO>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.contentAdd(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<ContentOutputDTO>> res = platformNewPayService.contentAdd(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute modifyAccount success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute modifyAccount fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute modifyAccount fail :{}", e);
		}
		return threadLocal.get();
	}

	// 1.1.26 形容词名词 – 修改
	@Override
	public ResponseDataNewPay<ContentOutputDTO> contentModify(ModifyContentInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<ContentOutputDTO>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.contentModify(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<ContentOutputDTO>> res = platformNewPayService.contentModify(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute modifyAccount success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute modifyAccount fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute modifyAccount fail :{}", e);
		}
		return threadLocal.get();
	}

	// 1.1.27 形容词名词 – 启用、停用
	@Override
	public ResponseDataNewPay<ContentOutputDTO> contentEnable(EnableContentInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<ContentOutputDTO>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.contentEnable(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<ContentOutputDTO>> res = platformNewPayService.contentEnable(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute modifyAccount success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute modifyAccount fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute modifyAccount fail :{}", e);
		}
		return threadLocal.get();
	}

	// 1.1.28 形容词名词 – 删除
	@Override
	public ResponseDataNewPay contentRemove(RemoveContentInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.contentRemove(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay> res = platformNewPayService.contentRemove(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute modifyAccount success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute modifyAccount fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute modifyAccount fail :{}", e);
		}
		return threadLocal.get();
	}

	// 1.1.29 形容词名词 – 分页查询
	@Override
	public ResponseDataNewPay<PageOutputDTO<ContentOutputDTO>> findContentByCondition(
			FindContentByConditionInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<PageOutputDTO<ContentOutputDTO>>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.findContentByCondition(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<PageOutputDTO<ContentOutputDTO>>> res = platformNewPayService
					.findContentByCondition(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute modifyAccount success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute modifyAccount fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute modifyAccount fail :{}", e);
		}
		return threadLocal.get();
	}

	// 1.1.30 修改未确认出款金额开关
	@Override
	public ResponseDataNewPay modifyUoFlag(ModifyUoFlagInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.modifyUoFlag(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay> res = platformNewPayService.modifyUoFlag(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute modifyAccount success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute modifyAccount fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute modifyAccount fail :{}", e);
		}
		return threadLocal.get();
	}

	// 1.1.31 生成常用金额/非常用金额二维码
	@Override
	public ResponseDataNewPay genANMultQr(GenANMultQrInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.genANMultQr(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay> res = platformNewPayService.genANMultQr(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute modifyAccount success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute modifyAccount fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute modifyAccount fail :{}", e);
		}
		return threadLocal.get();
	}

	// 1.1.32 统计常用金额、非常用金额已生成二维码个数和总个数
	@Override
	public ResponseDataNewPay<StatisticsMWROutputDTO> statisticsMWR(StatisticsMWRInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<StatisticsMWROutputDTO>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.statisticsMWR(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<StatisticsMWROutputDTO>> res = platformNewPayService
					.statisticsMWR(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute modifyAccount success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute modifyAccount fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute modifyAccount fail :{}", e);
		}
		return threadLocal.get();
	}

	// 1.1.33 形容词类型 – 新增
	@Override
	public ResponseDataNewPay<WordTypeOutputDTO> addWordType(AddWordTypeInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<WordTypeOutputDTO>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.addWordType(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<WordTypeOutputDTO>> res = platformNewPayService.addWordType(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute modifyAccount success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute modifyAccount fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute modifyAccount fail :{}", e);
		}
		return threadLocal.get();
	}

	// 1.1.34 形容词类型 – 查询列表
	@Override
	public ResponseDataNewPay<List<WordTypeOutputDTO>> findWordType(FindWordTypeInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<List<WordTypeOutputDTO>>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.findWordType(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<List<WordTypeOutputDTO>>> res = platformNewPayService
					.findWordType(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute modifyAccount success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute modifyAccount fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute modifyAccount fail :{}", e);
		}
		return threadLocal.get();
	}

	// 1.1.35 形容词类型 – 删除
	@Override
	public ResponseDataNewPay removeWordType(RemoveWordTypeInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.removeWordType(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay> res = platformNewPayService.removeWordType(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute modifyAccount success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute modifyAccount fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute modifyAccount fail :{}", e);
		}
		return threadLocal.get();
	}

	// 1.1.36 兼职绑定词语
	@Override
	public ResponseDataNewPay bindingWordType(BindingWordTypeInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.bindingWordType(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay> res = platformNewPayService.bindingWordType(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute modifyAccount success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute modifyAccount fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute modifyAccount fail :{}", e);
		}
		return threadLocal.get();
	}

	// 1.1.37 查询词库绑定分页列表
	@Override
	public ResponseDataNewPay<PageOutputDTO<BindOutputDTO>> findForBind(FindForBindInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<PageOutputDTO<BindOutputDTO>>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.findForBind(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<PageOutputDTO<BindOutputDTO>>> res = platformNewPayService
					.findForBind(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute modifyAccount success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute modifyAccount fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute modifyAccount fail :{}", e);
		}
		return threadLocal.get();
	}

	// 1.2.1 查询银行卡分页列表
	@Override
	public ResponseDataNewPay<PageOutputDTO<FindBankCardByConditionOutputDTO>> findBankCardByCondition(
			FindBankCardByConditionInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<PageOutputDTO<FindBankCardByConditionOutputDTO>>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.findBankCardByConditionRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<PageOutputDTO<FindBankCardByConditionOutputDTO>>> res = platformNewPayService
					.findBankCardByCondition(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute findBankCardByCondition success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute findBankCardByCondition fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute findBankCardByCondition fail :{}", e);
		}
		return threadLocal.get();
	}

	// 询微信、支付宝分页列表
	@Override
	public ResponseDataNewPay<PageOutputDTO<FindAWByConditionOutputDTO>> findAWByCondition(
			FindAWByConditionInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<PageOutputDTO<FindAWByConditionOutputDTO>>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.findAWByConditionRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<PageOutputDTO<FindAWByConditionOutputDTO>>> res = platformNewPayService
					.findAWByCondition(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute findAWByCondition success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute findAWByCondition fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute findAWByCondition fail :{}", e);
		}
		return threadLocal.get();
	}

	// 银行卡转入记录点击数字
	@Override
	public ResponseDataNewPay<PageOutputDTO<Find2ByConditionOutputDTO>> find2ByCondition(
			Find2ByConditioninputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<PageOutputDTO<Find2ByConditionOutputDTO>>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.find2ByConditionRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<PageOutputDTO<Find2ByConditionOutputDTO>>> res = platformNewPayService
					.find2ByCondition(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute find2ByCondition success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute find2ByCondition fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute find2ByCondition fail :{}", e);
		}
		return threadLocal.get();
	}

	// 微信、支付宝转入记录点击数字
	@Override
	public ResponseDataNewPay<PageOutputDTO<FindAWIn2ByConditionOutputDTO>> findAWIN2ByCondition(
			FindAWIn2ByConditionInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<PageOutputDTO<FindAWIn2ByConditionOutputDTO>>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.findAWIN2ByConditionRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<PageOutputDTO<FindAWIn2ByConditionOutputDTO>>> res = platformNewPayService
					.findAWIN2ByCondition(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute findAWIN2ByCondition success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute findAWIN2ByCondition fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute findAWIN2ByCondition fail :{}", e);
		}
		return threadLocal.get();
	}

	// 微信、支付宝、银行卡转出记录点击数字
	@Override
	public ResponseDataNewPay<PageOutputDTO<FindAWB3OutByConditionOutputDTO>> find3ByCondition(
			FindAWB3OutByConditionInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<PageOutputDTO<FindAWB3OutByConditionOutputDTO>>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.find3ByConditionRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<PageOutputDTO<FindAWB3OutByConditionOutputDTO>>> res = platformNewPayService
					.find3ByCondition(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute find3ByCondition success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute find3ByCondition fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute find3ByCondition fail :{}", e);
		}
		return threadLocal.get();
	}

	// 微信、支付宝流水
	@Override
	public ResponseDataNewPay<PageOutputDTO<FindAWLOG3ByConditionOutputDTO>> findAWLog3ByCondition(
			FindAWLOG3ByConditionInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<PageOutputDTO<FindAWLOG3ByConditionOutputDTO>>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.findAWLog3ByConditionRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<PageOutputDTO<FindAWLOG3ByConditionOutputDTO>>> res = platformNewPayService
					.findAWLog3ByCondition(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute findAWLog3ByCondition success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute findAWLog3ByCondition fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute findAWLog3ByCondition fail :{}", e);
		}
		return threadLocal.get();
	}

	// 银行卡流水
	@Override
	public ResponseDataNewPay<PageOutputDTO<FindBLog9ByConditionOutputDTO>> find9ByCondition(
			FindBLog9ByConditionInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<PageOutputDTO<FindBLog9ByConditionOutputDTO>>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.find9ByConditionRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<PageOutputDTO<FindBLog9ByConditionOutputDTO>>> res = platformNewPayService
					.find9ByCondition(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute find9ByCondition success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute find9ByCondition fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute find9ByCondition fail :{}", e);
		}
		return threadLocal.get();
	}

	// 对账
	@Override
	public ResponseDataNewPay verifyAccount(VerifyAccountInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.verifyAccountRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			// platformNewPayService.verifyAccount(requestBody);
			log.info("NewPayServiceImpl execute verifyAccount succeed !");
			Observable<ResponseDataNewPay> res = platformNewPayService.verifyAccount(requestBody);
			log.info("NewPayServiceImpl execute verifyAccount succeed !");
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute verifyAccount success:code:{},msg:{}", ret.getCode(), ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute verifyAccount fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute verifyAccount fail :{}", e);
		}
		return threadLocal.get();
	}

	// 1.3.1 微信、支付宝正在匹配查询分页列表
	@Override
	public ResponseDataNewPay<PageOutputDTO<Find4ByConditionOutputDTO>> find4ByCondition(
			Find4ByConditionInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<PageOutputDTO<Find4ByConditionOutputDTO>>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.find4ByConditionRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<PageOutputDTO<Find4ByConditionOutputDTO>>> res = platformNewPayService
					.find4ByCondition(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute find4ByCondition success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute find4ByCondition fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute find4ByCondition fail :{}", e);
		}
		return threadLocal.get();
	}

	// 1.3.2 微信、支付宝未匹配、已匹配查询分页列表
	@Override
	public ResponseDataNewPay<PageOutputDTO<Find5ByConditionOutputDTO>> find5ByCondition(
			Find5ByConditionInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<PageOutputDTO<Find5ByConditionOutputDTO>>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.find5ByConditionRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<PageOutputDTO<Find5ByConditionOutputDTO>>> res = platformNewPayService
					.find5ByCondition(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute find5ByCondition success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute find5ByCondition fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute find5ByCondition fail :{}", e);
		}
		return threadLocal.get();
	}

	// 1.3.3 微信、支付宝未认领查询分页列表
	@Override
	public ResponseDataNewPay<PageOutputDTO<Find6ByConditionOutputDTO>> find6ByCondition(
			Find6ByConditionInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<PageOutputDTO<Find6ByConditionOutputDTO>>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.find6ByConditionRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<PageOutputDTO<Find6ByConditionOutputDTO>>> res = platformNewPayService
					.find6ByCondition(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute find6ByCondition success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute find6ByCondition fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute find6ByCondition fail :{}", e);
		}
		return threadLocal.get();
	}

	// 1.3.4 微信、支付宝已取消查询分页列表
	@Override
	public ResponseDataNewPay<PageOutputDTO<Find7ByConditionOutputDTO>> find7ByCondition(
			Find7ByConditionInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<PageOutputDTO<Find7ByConditionOutputDTO>>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.find7ByConditionRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<PageOutputDTO<Find7ByConditionOutputDTO>>> res = platformNewPayService
					.find7ByCondition(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute find7ByCondition success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute find7ByCondition fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute find7ByCondition fail :{}", e);
		}
		return threadLocal.get();
	}

	// 1.3.5 点击“待处理流水”列，第一个tab
	@Override
	public ResponseDataNewPay<PageOutputDTO<Find10ByConditionOutputDTO>> find10ByCondition(
			Find10ByConditionInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<PageOutputDTO<Find10ByConditionOutputDTO>>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.find10ByConditionRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<PageOutputDTO<Find10ByConditionOutputDTO>>> res = platformNewPayService
					.find10ByCondition(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute find7ByCondition success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute find7ByCondition fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute find7ByCondition fail :{}", e);
		}
		return threadLocal.get();
	}

	// 1.3.6 第一个tab取消
	@Override
	public ResponseDataNewPay cancel(CancelInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay> threadLocal = new ThreadLocal<>();
		try {
			// 取消接口 按银行卡入款取消调用
			okhttp3.RequestBody requestBody;
			requestBody = requestBodyParser.buildRequestBody(inputDTO.getOid().toString(), inputDTO.getCode(),
					inputDTO.getRemark() + "新平台取消");
			HttpClientNew.getInstance().getPlatformServiceApi().depositCancel(requestBody).subscribe((data) -> {
				log.info("NewPayServiceImpl execute cancel result: {}, orders:({})", data, inputDTO.getCode());
				// 若成功，则更新数据库
				ResponseDataNewPay responseDataNewPay = new ResponseDataNewPay();
				if (data.getStatus() == 1) {
					log.info("NewPayServiceImpl execute cancel success,status:{}", data.getStatus());
					responseDataNewPay.setStatus((byte) data.getStatus());
					responseDataNewPay.setCode(200);
					threadLocal.set(responseDataNewPay);
				} else {
					responseDataNewPay.setStatus((byte) -1);
					responseDataNewPay.setCode(250);
				}
			}, (e) -> log.error("NewPayServiceImpl execute cancel fail:exceptihon:{} ", e));
			// RequestBody requestBody = requestBodyNewPay.cancelRequestBody(inputDTO);
			// PlatformNewPayService platformNewPayService =
			// HttpClient4NewPay.getInstance().getPlatformNewPayServiceApi(false);
			// Observable<ResponseDataNewPay> res =
			// platformNewPayService.cancel(requestBody);
			// res.subscribe(ret -> {
			// log.info("NewPayServiceImpl execute cancel success :code:{},msg:{}",
			// ret.getCode(), ret.getMsg());
			// threadLocal.set(ret);
			// }, e -> log.info("NewPayServiceImpl execute cancel fail :{}",
			// e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute cancel fail :{}", e);
		}
		return threadLocal.get();
	}

	// 1.3.7 第一个tab新增备注
	@Override
	public ResponseDataNewPay modifyRemark(ModifyRemarkInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.modifyRemarkRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay> res = platformNewPayService.modifyRemark(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute modifyRemark success :code:{},msg:{}", ret.getCode(), ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute modifyRemark fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute modifyRemark fail :{}", e);
		}
		return threadLocal.get();
	}

	// 1.3.8 点击“待处理流水”列，第二个tab
	@Override
	public ResponseDataNewPay<PageOutputDTO<Find11ByConditionOutputDTO>> find11ByCondition(
			Find11ByConditionInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<PageOutputDTO<Find11ByConditionOutputDTO>>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.find11ByConditionRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<PageOutputDTO<Find11ByConditionOutputDTO>>> res = platformNewPayService
					.find11ByCondition(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute find11ByCondition success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute find11ByCondition fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute find11ByCondition fail :{}", e);
		}
		return threadLocal.get();
	}

	// 1.3.9 第二个tab新增备注
	@Override
	public ResponseDataNewPay putPlus(PutPlusInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.putPlusRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay> res = platformNewPayService.putPlus(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute putPlus success :code:{},msg:{}", ret.getCode(), ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute putPlus fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute putPlus fail :{}", e);
		}
		return threadLocal.get();
	}

	// 1.3.10 第二个tab补提单
	@Override
	public ResponseDataNewPay matching(MatchingInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.matchingRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay> res = platformNewPayService.matching(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute matching success :code:{},msg:{}", ret.getCode(), ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute matching fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute matching fail :{}", e);
		}
		return threadLocal.get();
	}

	// 1.3.11 第一个tab和第二个tab匹配
	@Override
	public ResponseDataNewPay addRemark(AddRemarkInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.addRemarkRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay> res = platformNewPayService.addRemark(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute addRemark success :code:{},msg:{}", ret.getCode(), ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute addRemark fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute addRemark fail :{}", e);
		}
		return threadLocal.get();
	}

	// 1.3.12 统计指定device的正在匹配总数
	@Override
	public ResponseDataNewPay<List<StatisticsOutputDTO>> statistics(StatisticsInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<List<StatisticsOutputDTO>>> threadLocal = ThreadLocal.withInitial(() -> {
			ResponseDataNewPay<List<StatisticsOutputDTO>> responseDataNewPay = new ResponseDataNewPay<>();
			responseDataNewPay.setData(null);
			responseDataNewPay.setCode(400);
			responseDataNewPay.setMsg("查询失败!");
			return responseDataNewPay;
		});
		ResponseDataNewPay<List<StatisticsOutputDTO>> responseDataNewPay = new ResponseDataNewPay<>();
		try {
			Map<Integer, String[]> map = accountService.getDeviceNoAndHandicapCodeMap(inputDTO.getDeviceCol());
			if (CollectionUtils.isEmpty(map)) {
				return null;
			}
			List<StatisticsOutputDTO> allList = new LinkedList<>();
			AtomicBoolean flag = new AtomicBoolean(true);
			ThreadLocal<String> error = new ThreadLocal() {
				private final String error = "查询失败!";

				@Override
				protected String initialValue() {
					return error;
				}
			};
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			for (Map.Entry<Integer, String[]> entry : map.entrySet()) {
				StatisticsInputDTO inputDTO2 = new StatisticsInputDTO();
				inputDTO2.setOid(entry.getKey());
				inputDTO2.setDeviceCol(entry.getValue());
				inputDTO2.setTimeStart(inputDTO.getTimeStart());
				inputDTO2.setTimeEnd(inputDTO.getTimeEnd());
				RequestBody requestBody = requestBodyNewPay.statisticsRequestBody(inputDTO2);
				Observable<ResponseDataNewPay<List<StatisticsOutputDTO>>> res = platformNewPayService
						.statistics(requestBody);
				res.subscribe(ret -> {
					log.info("NewPayServiceImpl execute statistics success :code:{},msg:{}", ret.getCode(),
							ret.getMsg());
					if (!CollectionUtils.isEmpty(ret.getData())) {
						allList.addAll(ret.getData());
					}
				}, e -> {
					log.error("NewPayServiceImpl execute statistics fail : ", e);
					error.set(e.getLocalizedMessage());
					flag.getAndSet(false);
				});
			}
			if (flag.get()) {
				responseDataNewPay.setData(allList);
				responseDataNewPay.setCode(200);
				responseDataNewPay.setMsg("查询成功");
			} else {
				threadLocal.get().setMsg(error.get());
				error.remove();
			}
			threadLocal.set(responseDataNewPay);
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute statistics fail  :", e);
		}
		responseDataNewPay = threadLocal.get();
		threadLocal.remove();
		return responseDataNewPay;
	}

	// 1.4.1 新支付下发记录
	@Override
	public ResponseDataNewPay<PageOutputDTO<Find4WByConditionOutputDTO>> find4WByCondition(
			Find4WByConditionInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<PageOutputDTO<Find4WByConditionOutputDTO>>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.find4WByConditionRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<PageOutputDTO<Find4WByConditionOutputDTO>>> res = platformNewPayService
					.find4WByCondition(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute find4WByCondition success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute find4WByCondition fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute find4WByCondition fail :{}", e);
		}
		return threadLocal.get();
	}

	// 1.4.2 返佣规则 – 查询分页列表
	@Override
	public ResponseDataNewPay<PageOutputDTO<FindCRByConditionOutputDTO>> findCRByCondition(
			FindCRByConditionInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<PageOutputDTO<FindCRByConditionOutputDTO>>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.findCRByConditionRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<PageOutputDTO<FindCRByConditionOutputDTO>>> res = platformNewPayService
					.findCRByCondition(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute findCRByCondition success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute findCRByCondition fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute findCRByCondition fail :{}", e);
		}
		return threadLocal.get();
	}

	// 1.4.3 返佣规则 – 新增
	@Override
	public ResponseDataNewPay<AddCROutputDTO> addCR(AddCRInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<AddCROutputDTO>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.addCRRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<AddCROutputDTO>> res = platformNewPayService.addCR(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute addCR success :code:{},msg:{}", ret.getCode(), ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute addCR fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute addCR fail :{}", e);
		}
		return threadLocal.get();
	}

	// 1.4.4 返佣规则 – 修改
	@Override
	public ResponseDataNewPay<ModifyCROutputDTO> modifyCR(ModifyCRInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay<ModifyCROutputDTO>> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.modifyCRRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay<ModifyCROutputDTO>> res = platformNewPayService.modifyCR(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute modifyCR success :code:{},msg:{}", ret.getCode(), ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute modifyCR fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute modifyCR fail :{}", e);
		}
		return threadLocal.get();
	}

	// 1.4.5 返佣规则 – 删除
	@Override
	public ResponseDataNewPay removeCR(RemoveCRInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.removeCRRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay> res = platformNewPayService.removeCR(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute removeCR success :code:{},msg:{}", ret.getCode(), ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute removeCR fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute removeCR fail :{}", e);
		}
		return threadLocal.get();
	}

	// 1.6.1 出款确认
	@Override
	public ResponseDataNewPay confirm(ConfirmInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.confirmRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay> res = platformNewPayService.confirm(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute confirm success :code:{},msg:{}", ret.getCode(), ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute confirm fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute confirm fail :{}", e);
		}
		return threadLocal.get();
	}

	// 1.6.2 重置信用额度
	@Override
	public ResponseDataNewPay reset(ResetInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay> threadLocal = new ThreadLocal<>();
		try {
			RequestBody requestBody = requestBodyNewPay.resetRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay> res = platformNewPayService.reset(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute reset success :code:{},msg:{}", ret.getCode(), ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute reset  fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute reset fail :{}", e);
		}
		return threadLocal.get();
	}

	// 1.6.3 自动重置信用额度
	@Override
	public ResponseDataNewPay autoReset(AutoResetInputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay> threadLocal = new ThreadLocal<>();
		try {
			log.info("NewPayServiceImpl.autoReset input params :openMan:{},account:{},inTime:{},money:{},oid:{}",
					inputDTO.getOpenMan(), inputDTO.getAccount(), inputDTO.getInTime(), inputDTO.getMoney(),
					inputDTO.getOid());
			if (StringUtils.isBlank(inputDTO.getOpenMan()) || StringUtils.isBlank(inputDTO.getAccount())
					|| null == inputDTO.getOid() || null == inputDTO.getInTime() || null == inputDTO.getMoney()) {
				log.info("NewPayServiceImpl.autoReset input params exist empty ,check again !");
				return null;
			}
			RequestBody requestBody = requestBodyNewPay.autoResetRequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay> res = platformNewPayService.autoReset(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute autoReset success :code:{},msg:{}", ret.getCode(), ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute autoReset  fail :{}", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute autoReset fail :{}", e);
		}
		return threadLocal.get();
	}

	// 1.6.4银行卡-修改状态
	@Override
	public ResponseDataNewPay modifyBindCardStatus2(ModifyBindCardStatus2InputDTO inputDTO) {
		ThreadLocal<ResponseDataNewPay> threadLocal = new ThreadLocal<>();
		try {
			log.info(
					"NewPayServiceImpl.modifyBindCardStatus2 input params :oid:{},CardPayeeCol :{},status:{},operationAdminName:{}",
					inputDTO.getOid(), inputDTO.getCardPayeeCol(), inputDTO.getStatus(),
					inputDTO.getOperationAdminName());
			if (StringUtils.isBlank(inputDTO.getOperationAdminName())) {
				ResponseDataNewPay responseDataNewPay = new ResponseDataNewPay();
				responseDataNewPay.setMsg("操作人必填!");
				threadLocal.set(responseDataNewPay);
				return threadLocal.get();
			}
			RequestBody requestBody = requestBodyNewPay.modifyBindCardStatus2RequestBody(inputDTO);
			PlatformNewPayService platformNewPayService = HttpClient4NewPay.getInstance()
					.getPlatformNewPayServiceApi(false);
			Observable<ResponseDataNewPay> res = platformNewPayService.modifyBindCardStatus2(requestBody);
			res.subscribe(ret -> {
				log.info("NewPayServiceImpl execute modifyBindCardStatus2 success :code:{},msg:{}", ret.getCode(),
						ret.getMsg());
				threadLocal.set(ret);
			}, e -> log.error("NewPayServiceImpl execute modifyBindCardStatus2  fail : ", e));
		} catch (Exception e) {
			log.error("NewPayServiceImpl execute modifyBindCardStatus2 fail : ", e);
		}
		return threadLocal.get();
	}

	/**
	 * 根据账号ID，将新状态同步到平台 在用 停用 冻结（删除不走此接口）
	 *
	 * @param accountId
	 * @param newStatus
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public ResponseDataNewPay updateStatus_sync(Integer accountId, Integer newStatus) {
		ResponseDataNewPay responseDataNewPay = new ResponseDataNewPay();
		responseDataNewPay.setStatus(new Byte("0"));
		SysUser operator = (SysUser) SecurityUtils.getSubject().getPrincipal();
		if (null == operator) {
			responseDataNewPay.setMsg("用户未登录");
			return responseDataNewPay;
		}
		BizAccount account = accountService.getById(accountId);
		// 非入款卡不进行同步
		if (null != account && account.getType().equals(AccountType.InBank.getTypeId())) {
			// 状态
			if (newStatus == AccountStatus.Normal.getStatus()) {
				newStatus = 1;
			} else if (newStatus == AccountStatus.StopTemp.getStatus()) {
				newStatus = 0;
			} else if (newStatus == AccountStatus.Freeze.getStatus()) {
				newStatus = 4;
			}
			// 盘口
			String handicapCode = null;
			List<BizHandicap> handicapList = handicapService.findAllToList();
			if (null != handicapList && handicapList.size() > 0) {
				for (int i = 0; i < handicapList.size(); i++) {
					if (null != handicapList.get(i) && handicapList.get(i).getId().equals(account.getHandicapId())) {
						handicapCode = handicapList.get(i).getCode();
						break;
					}
				}
			}
			if (StringUtils.isNotBlank(handicapCode)) {
				// 银行卡信息
				List<CardPayee> cardPayeeCol = new ArrayList<CardPayee>();
				CardPayee CardPayee = new CardPayee();
				CardPayee.setCardNo(account.getAccount());
				CardPayee.setPayeeName(account.getOwner());
				cardPayeeCol.add(CardPayee);
				// 参数
				ModifyBindCardStatus2InputDTO param = new ModifyBindCardStatus2InputDTO();
				param.setOid(Integer.parseInt(handicapCode));
				param.setOperationAdminName(operator.getUid());
				param.setStatus(newStatus);
				param.setCardPayeeCol(cardPayeeCol);
				ResponseDataNewPay res = modifyBindCardStatus2(param);
				responseDataNewPay.setCode(res.getCode());
				if (res.getCode() != 200) {
					responseDataNewPay.setMsg("修改失败，状态编码" + res.getCode() + ",原因：" + res.getMsg());
				} else {
					responseDataNewPay.setStatus(new Byte("1"));
				}
			}

		}
		return responseDataNewPay;
	}

	// 删除账号 调用平台接口
	@Override
	public String deleteInAccounts(Integer[] accountIds, SysUser sysUser) {
		try {
			List<String> ret = new ArrayList<>();
			List<BizAccount> params = Arrays.stream(accountIds).map(p -> {
				BizAccount bizAccount = new BizAccount();
				bizAccount.setId(p);
				return bizAccount;
			}).collect(Collectors.toList());
			Map<Integer, ModifyBindCardStatus2InputDTO> map = wrapModifyBindCardStatus2InputDTO(params, sysUser);
			for (Map.Entry<Integer, ModifyBindCardStatus2InputDTO> entry : map.entrySet()) {
				ModifyBindCardStatus2InputDTO inputDTO = entry.getValue();
				ResponseDataNewPay res = modifyBindCardStatus2(inputDTO);
				if (res.getCode() != 200) {
					ret.add("删除盘口:" + inputDTO.getOid() + "账号:" + inputDTO.getCardPayeeCol().toString() + ",结果:"
							+ res.getMsg());
				}
			}
			String message = CollectionUtils.isEmpty(ret) ? "ok" : ret.toString();
			return message;
		} catch (Exception e) {
			log.error("删除失败 ,error:", e);
			return e.getMessage();
		}
	}

	public Map<Integer, ModifyBindCardStatus2InputDTO> wrapModifyBindCardStatus2InputDTO(List<BizAccount> params,
			SysUser sysUser) {
		if (CollectionUtils.isEmpty(params)) {
			return null;
		}
		Map<Integer, ModifyBindCardStatus2InputDTO> map = new HashMap<>(params.size(), 0.75f);
		Loop1: for (BizAccount param : params) {
			AccountBaseInfo account = accountService.getFromCacheById(param.getId());
			BizAccount account1 = null;
			int accountType;
			if (Objects.isNull(account)) {
				account1 = accountService.getById(param.getId());
				if (Objects.isNull(account1)) {
					continue Loop1;
				}
				accountType = account1.getType();
			} else {
				accountType = account.getType();
				if (StringUtils.isBlank(account.getAccount()) || StringUtils.isBlank(account.getOwner())) {
					log.info("删除账号调用平台,账号 id:{} ,账号:{},开户人:{}", param.getId(), account.getAccount(),
							account.getOwner());
					continue Loop1;
				}
			}
			if (accountType != AccountType.InBank.getTypeId()) {
				log.info("删除账号调用平台,账号 id:{} ,类型:{}", param.getId(), AccountType.findByTypeId(accountType).getMsg());
				continue Loop1;
			}
			BizHandicap handicap = handicapService
					.findFromCacheById(Objects.nonNull(account) ? account.getHandicapId() : account1.getHandicapId());
			if (Objects.isNull(handicap)) {
				handicap = handicapRepository
						.findById2(Objects.nonNull(account) ? account.getHandicapId() : account1.getHandicapId());
				if (Objects.isNull(handicap)) {
					continue Loop1;
				}
			}
			if (StringUtils.isBlank(handicap.getCode()) || !NumberUtils.isDigits(handicap.getCode())) {
				log.info("删除账号调用平台,账号id:{},盘口编码:{}", param.getId(), handicap.getCode());
				continue Loop1;
			}
			ModifyBindCardStatus2InputDTO inputDTO;
			List<ModifyBindCardStatus2InputDTO.CardPayee> subList = new ArrayList<>();
			if (!CollectionUtils.isEmpty(map) && map.keySet().contains(Integer.valueOf(handicap.getCode()))) {
				inputDTO = map.get(Integer.valueOf(handicap.getCode()));
				subList = inputDTO.getCardPayeeCol();
			} else {
				inputDTO = new ModifyBindCardStatus2InputDTO();
				inputDTO.setOperationAdminName(sysUser.getUid());
				inputDTO.setStatus(5);
				inputDTO.setOid(Integer.valueOf(handicap.getCode()));
			}
			ModifyBindCardStatus2InputDTO.CardPayee cardPayee = new ModifyBindCardStatus2InputDTO.CardPayee();
			cardPayee.setCardNo(Objects.nonNull(account) ? account.getAccount() : account1.getAccount());
			cardPayee.setPayeeName(Objects.nonNull(account) ? account.getOwner() : account1.getOwner());
			subList.add(cardPayee);
			inputDTO.setCardPayeeCol(subList);
			map.put(Integer.valueOf(handicap.getCode()), inputDTO);
		}
		return map;
	}
}
