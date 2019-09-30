/**
 *
 */
package com.xinbo.fundstransfer.unionpay.ysf.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.xinbo.fundstransfer.domain.ResponseData;
import com.xinbo.fundstransfer.domain.entity.BizHandicap;
import com.xinbo.fundstransfer.domain.entity.BizOtherAccountEntity;
import com.xinbo.fundstransfer.newinaccount.dto.input.CardForPayInputDTO;
import com.xinbo.fundstransfer.newinaccount.dto.output.CardForPayOutputDTO;
import com.xinbo.fundstransfer.unionpay.ysf.entity.YSFQResponseEntity;
import com.xinbo.fundstransfer.unionpay.ysf.entity.YSFQRrequestEntity;
import com.xinbo.fundstransfer.unionpay.ysf.entity.YSFQrCodeEntity;
import com.xinbo.fundstransfer.unionpay.ysf.inputdto.AddYSFAccountInputDTO;
import com.xinbo.fundstransfer.unionpay.ysf.inputdto.CreateYSFAccountOutputDTO;
import com.xinbo.fundstransfer.unionpay.ysf.inputdto.InAccountBindedYSFInputDTO;
import com.xinbo.fundstransfer.unionpay.ysf.inputdto.QueryYSFInputDTO;
import com.xinbo.fundstransfer.unionpay.ysf.inputdto.UpdateYSFBasicInfoInputDTO;
import com.xinbo.fundstransfer.unionpay.ysf.inputdto.UpdateYSFBindAccountInputDTO;
import com.xinbo.fundstransfer.unionpay.ysf.inputdto.UpdateYSFPWDInputDTO;
import com.xinbo.fundstransfer.unionpay.ysf.inputdto.YSFGenQrCodeReqeustDto;
import com.xinbo.fundstransfer.unionpay.ysf.outputdto.YSFGenerateQRRequestDTO;
import com.xinbo.fundstransfer.unionpay.ysf.outputdto.YSFQrCodeQueryDto;

/**
 * 银联云闪付服务接口
 *
 * @author blake
 *
 */
public interface YSFService {
	/** 云闪付获取支付卡信息 */
	CardForPayOutputDTO cardForPayYSF(CardForPayInputDTO inputDTO, BizHandicap handicap);

	Map<String, String> findOtherAccountByBankAccount(String bankAccountNo);

	void noticeFreshCache(String bankAccountNo);

	void freshCache(String bankAccountNo);

	/** 更新云闪付密码 */
	void updateYSFPwd(UpdateYSFPWDInputDTO inputDTO);

	/** 更新云闪付基本信息 */
	void updateYSFBasicInfo(UpdateYSFBasicInfoInputDTO infoInputDTO);

	/** 更新云闪付绑定的银行卡信息 */
	void updateYSFBindAccount(UpdateYSFBindAccountInputDTO inputDTO);

	/***
	 * 描述 请求二维码
	 *
	 * @param requestEntity
	 * @return
	 */
	ResponseData<?> call4GenerateQRs(YSFQRrequestEntity requestEntity);

	/**
	 * 描述:接收返回的二维码信息
	 *
	 * @param responseEntity
	 */
	void receiveCabanaSendQRs(YSFQResponseEntity responseEntity);

	/** 描述:条件分页查询云闪付账号信息 */
	Page<BizOtherAccountEntity> findPageByCriteria(QueryYSFInputDTO inputDTO);

	/** 新增其他账号:如云闪付 */
	CreateYSFAccountOutputDTO add(AddYSFAccountInputDTO ysf, List<InAccountBindedYSFInputDTO> dto);

	/** 根据云闪付账号查询 */
	BizOtherAccountEntity findByAccountNo(String accountNo);

	/** 根据云闪付账号id查询 */
	BizOtherAccountEntity findById(Integer id);

	/** 根据云闪付id查询如果有记录就锁定如果没有不锁定 */
	BizOtherAccountEntity findByIdForUpdate(Integer id);

	/** 更新账号信息 */
	CreateYSFAccountOutputDTO update(AddYSFAccountInputDTO ysf, BizOtherAccountEntity entity,
			List<InAccountBindedYSFInputDTO> oldAccountList, List<InAccountBindedYSFInputDTO> newAccountList);

	/** 更新云闪付账号状态 */
	void updateYSFStatus(Integer id, Byte status, String remark);

	/** 操作时添加的备注 */
	String addRemark(String oldRemark, String newRemark);

	/** 删除云闪付账号 */
	int deleteYSF(Integer id);

	/** 绑定云闪付账号与银行卡账号关联 */
	void bind(BizOtherAccountEntity entity, List<Integer> accountIdList, String newRemark);

	/**
	 * 保存收款二维码
	 */
	Boolean saveQrCode(YSFQrCodeEntity qrCode);

	/**
	 * 获取生成二维码的金额 <br>
	 * 给app组装随机金额
	 *
	 * @param ysfAccount
	 *            云闪付账号 和 绑定的银行卡
	 * @return
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	YSFGenerateQRRequestDTO getYSFRandomMoney(YSFGenQrCodeReqeustDto ysfAccount)
			throws JsonProcessingException, IOException;

	/**
	 * 描述:平台云闪付绑定银行卡的时候校验接口
	 *
	 * @param param
	 *            需要绑定的银行账号字符串,格式：xxx,xxxx,xxx
	 * @return
	 */
	String checkBankAccountToBind(Map param);

	/**
	 * 回收随机数 <br>
	 * 当入款单订单确认或者取消时，释放随机数
	 */
	public void recycleRandNum(String bankAccount, BigDecimal orderMoney);

	/**
	 * 锁定随机数 <br>
	 * 当收到流水，但是没有对应的入款单时，锁定该随机数，不允许使用
	 */
	public void lockRandNum(String bankAccount, BigDecimal orderMoney);

	/**
	 * 获取云闪付配置的常用金额
	 *
	 * @return 常用金额的整数，由小到大排列
	 */
	public List<Integer> getYSFAllowMoney();

	/**
	 * 根据银行卡号查询收款二维码
	 *
	 * @param bankAccount
	 * @return
	 */
	List<YSFQrCodeQueryDto> queryByBankAccount(String bankAccount);

	/**
	 * 需求 6437 根据层级编码获取
	 * @param inputDTO
	 * @param handicap
	 * @return
	 */
	CardForPayOutputDTO cardForPayYsfByLevelCode(CardForPayInputDTO inputDTO, BizHandicap handicap);
}
