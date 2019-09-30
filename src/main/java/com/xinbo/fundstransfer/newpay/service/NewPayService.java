package com.xinbo.fundstransfer.newpay.service;

import java.util.List;

import com.xinbo.fundstransfer.domain.ResponseDataNewPay;
import com.xinbo.fundstransfer.domain.entity.SysUser;
import com.xinbo.fundstransfer.newpay.inputdto.*;
import com.xinbo.fundstransfer.newpay.outdto.*;

/**
 * Created by Administrator on 2018/7/11.
 */
public interface NewPayService {
	ResponseDataNewPay<AddNewPayOutputDTO> add(AddNewPayInputDTO inputDTO);

	ResponseDataNewPay<ModifyInfoOutputDTO> modifyInfo(ModifyInfoInputDTO inputDTO);

	ResponseDataNewPay<ModifyAccountOutputDTO> modifyAccount(ModifyAccountInputDTO inputDTO);

	ResponseDataNewPay<ModifyStatusOutputDTO> modifyStatus(ModifyStatusInputDTO inputDTO);

	ResponseDataNewPay modifyPwd(ModifyPWDInputDTO inputDTO);

	ResponseDataNewPay remove(RemoveInputDTO inputDTO);

	ResponseDataNewPay<PageOutputDTO<FindByConditionOutputDTO>> findByCondition(FindByConditionInputDTO inputDTO);

	ResponseDataNewPay<ModifyFixOutputDTO> modifyFix(ModifyFixInputDTO inputDTO);

	ResponseDataNewPay<List<FindPOCForCrkOutputDTO>> findPOCForCrk(FindPOCForCrkInputDTO inputDTO);

	ResponseDataNewPay newpayAisleConfigBind(NewpayAisleConfigBindInputDTO inputDTO);

	ResponseDataNewPay newpayAisleConfigFindBind(NewpayAisleConfigFindBindInputDTO inputDTO);

	ResponseDataNewPay<FindBankOutputDTO> findBank(FindBankInputDTO inputDTO);

	ResponseDataNewPay<List<FindBalanceInfoOutputDTO>> findBalanceInfo(FindBalanceInfoInputDTO infoInputDTO);

	ResponseDataNewPay<FindAccountInfoOutputDTO> findAccountInfo(FindAccountInfoInputDTO infoInputDTO);

	ResponseDataNewPay<FindTelInfoOutputDTO> findTelInfo(FindTelInfoInputDTO infoInputDTO);

	ResponseDataNewPay<PageOutputDTO<Find8ByConditionOutputDTO>> find8ByCondition(Find8ByConditionInputDTO inputDTO);

	ResponseDataNewPay<PageOutputDTO<FindQRByConditionOutputDTO>> findQRByCondition(FindQRByConditionInputDTO inputDTO);

	ResponseDataNewPay batchAddQR(BatchAddQRInputDTO inputDTO);

	ResponseDataNewPay batchDeleteQR(BatchDeleteQRInputDTO inputDTO);

	ResponseDataNewPay<List<FindBankAllOutputDTO>> findBankAll();

	ResponseDataNewPay<FindPwdExistsOutputDTO> findPwdExists(FindPwdExistsInputDTO inputDTO);

	ResponseDataNewPay<PageOutputDTO<FindBankCardByConditionOutputDTO>> findBankCardByCondition(
			FindBankCardByConditionInputDTO inputDTO);

	ResponseDataNewPay<PageOutputDTO<FindAWByConditionOutputDTO>> findAWByCondition(FindAWByConditionInputDTO inputDTO);

	ResponseDataNewPay<PageOutputDTO<Find2ByConditionOutputDTO>> find2ByCondition(Find2ByConditioninputDTO inputDTO);

	ResponseDataNewPay<PageOutputDTO<FindAWIn2ByConditionOutputDTO>> findAWIN2ByCondition(
			FindAWIn2ByConditionInputDTO inputDTO);

	ResponseDataNewPay<PageOutputDTO<FindAWB3OutByConditionOutputDTO>> find3ByCondition(
			FindAWB3OutByConditionInputDTO inputDTO);

	ResponseDataNewPay<PageOutputDTO<FindAWLOG3ByConditionOutputDTO>> findAWLog3ByCondition(
			FindAWLOG3ByConditionInputDTO inputDTO);

	ResponseDataNewPay<PageOutputDTO<FindBLog9ByConditionOutputDTO>> find9ByCondition(
			FindBLog9ByConditionInputDTO inputDTO);

	ResponseDataNewPay verifyAccount(VerifyAccountInputDTO inputDTO);

	ResponseDataNewPay<PageOutputDTO<FindCommissionDetailOutputDTO>> findCommissionDetailByCondition(
			FindCommissionDetailInputDTO inputDTO);

	ResponseDataNewPay<FindAccountInfoOutputDTO> findAccountInfo2(FindAccountInfoInputDTO infoInputDTO);

	ResponseDataNewPay<PageOutputDTO<Find4ByConditionOutputDTO>> find4ByCondition(Find4ByConditionInputDTO inputDTO);

	ResponseDataNewPay<PageOutputDTO<Find5ByConditionOutputDTO>> find5ByCondition(Find5ByConditionInputDTO inputDTO);

	ResponseDataNewPay<PageOutputDTO<Find6ByConditionOutputDTO>> find6ByCondition(Find6ByConditionInputDTO inputDTO);

	ResponseDataNewPay<PageOutputDTO<Find7ByConditionOutputDTO>> find7ByCondition(Find7ByConditionInputDTO inputDTO);

	ResponseDataNewPay<PageOutputDTO<Find10ByConditionOutputDTO>> find10ByCondition(Find10ByConditionInputDTO inputDTO);

	ResponseDataNewPay<PageOutputDTO<Find11ByConditionOutputDTO>> find11ByCondition(Find11ByConditionInputDTO inputDTO);

	ResponseDataNewPay cancel(CancelInputDTO inputDTO);

	ResponseDataNewPay modifyRemark(ModifyRemarkInputDTO inputDTO);

	ResponseDataNewPay putPlus(PutPlusInputDTO inputDTO);

	ResponseDataNewPay matching(MatchingInputDTO inputDTO);

	ResponseDataNewPay addRemark(AddRemarkInputDTO inputDTO);

	ResponseDataNewPay<List<StatisticsOutputDTO>> statistics(StatisticsInputDTO inputDTO);

	ResponseDataNewPay<PageOutputDTO<Find4WByConditionOutputDTO>> find4WByCondition(Find4WByConditionInputDTO inputDTO);

	ResponseDataNewPay<PageOutputDTO<FindCRByConditionOutputDTO>> findCRByCondition(FindCRByConditionInputDTO inputDTO);

	ResponseDataNewPay<AddCROutputDTO> addCR(AddCRInputDTO inputDTO);

	ResponseDataNewPay<ModifyCROutputDTO> modifyCR(ModifyCRInputDTO inputDTO);

	ResponseDataNewPay removeCR(RemoveCRInputDTO inputDTO);

	ResponseDataNewPay confirm(ConfirmInputDTO inputDTO);

	ResponseDataNewPay reset(ResetInputDTO inputDTO);

	ResponseDataNewPay autoReset(AutoResetInputDTO inputDTO);

	ResponseDataNewPay syncBankBalance(SyncBankBalanceInputDTO inputDTO);

	ResponseDataNewPay<ContentOutputDTO> contentAdd(AddContentInputDTO inputDTO);

	ResponseDataNewPay<ContentOutputDTO> contentModify(ModifyContentInputDTO inputDTO);

	ResponseDataNewPay<ContentOutputDTO> contentEnable(EnableContentInputDTO inputDTO);

	ResponseDataNewPay contentRemove(RemoveContentInputDTO inputDTO);

	ResponseDataNewPay<PageOutputDTO<ContentOutputDTO>> findContentByCondition(FindContentByConditionInputDTO inputDTO);

	ResponseDataNewPay modifyUoFlag(ModifyUoFlagInputDTO inputDTO);

	ResponseDataNewPay genANMultQr(GenANMultQrInputDTO inputDTO);

	ResponseDataNewPay<StatisticsMWROutputDTO> statisticsMWR(StatisticsMWRInputDTO inputDTO);

	ResponseDataNewPay<WordTypeOutputDTO> addWordType(AddWordTypeInputDTO inputDTO);

	ResponseDataNewPay<List<WordTypeOutputDTO>> findWordType(FindWordTypeInputDTO inputDTO);

	ResponseDataNewPay removeWordType(RemoveWordTypeInputDTO inputDTO);

	ResponseDataNewPay bindingWordType(BindingWordTypeInputDTO inputDTO);

	ResponseDataNewPay<PageOutputDTO<BindOutputDTO>> findForBind(FindForBindInputDTO inputDTO);

	ResponseDataNewPay modifyBindCardStatus2(ModifyBindCardStatus2InputDTO inputDTO);

	ResponseDataNewPay updateStatus_sync(Integer accountId, Integer newStatus);

	String deleteInAccounts(Integer[] accountIds, SysUser sysUser);
}
