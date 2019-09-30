package com.xinbo.fundstransfer.ali4enterprise.service;

import java.util.List;

import com.xinbo.fundstransfer.ali4enterprise.inputdto.*;
import com.xinbo.fundstransfer.ali4enterprise.outputdto.*;
import com.xinbo.fundstransfer.domain.ResponseDataNewPay;

public interface Ali4EnterpriseService {

	ResponseDataNewPay addEpAisleDetail(AddEpAisleDetailInputDTO inputDTO);

	ResponseDataNewPay<FindEpDetailListOutputDTO> findEpDetailList(CommonInputDTO inputDTO);

	ResponseDataNewPay<FindEpDetailOutputDTO> findEpDetail(CommonInputDTO inputDTO);

	ResponseDataNewPay<PageOutPutDTO<FindEpByConditionOutputDTO>> findEpByCondition(FindEpByConditionInputDTO inputDTO);

	ResponseDataNewPay<List<FindEpAisleOutputDTO>> findEpAisle(FindEpAisleInputDTO inputDTO);

	ResponseDataNewPay bindEpAisle(BindEpAisleInputDTO inputDTO);

	ResponseDataNewPay modifyEpAisleDetail(ModifyEpAisleDetailInputDTO inputDTO);

	ResponseDataNewPay cleanEpInData(CommonInputDTO inputDTO);

	ResponseDataNewPay<FindEpStopAlarmOutputDTO> findEpStopAlarm(FindEpStopAlarmInputDTO inputDTO);

	ResponseDataNewPay modifyEpStopAlarm(ModifyEpStopAlarmInputDTO inputDTO);

	ResponseDataNewPay<ModifyEpStatusOutputDTO> modifyEpStatus(ModifyEpStatusInputDTO inputDTO);

	ResponseDataNewPay<PageOutPutDTO<FindPayOwnerWordListOutputDTO>> findPayOwnerWordList(
			FindPayOwnerWordListInputDTO inputDTO);

	ResponseDataNewPay addPayOwnerWord(AddPayOwnerWordInputDTO inputDTO);

	ResponseDataNewPay<ModifyPayOwnerWordOutputDTO> modifyPayOwnerWord(ModifyPayOwnerWordInputDTO inputDTO);

	ResponseDataNewPay removePayOwnerWord(RemovePayOwnerWordInputDTO inputDTO);

	ResponseDataNewPay findPayOwnerWordSta(FindPayOwnerWordStaInputDTO inputDTO);

	ResponseDataNewPay<PageOutPutDTO<FindPayOwnerWordTypeListOutputDTO>> findPayOwnerWordTypeList(
			FindPayOwnerWordTypeListInputDTO inputDTO);

	ResponseDataNewPay<List<FindPayOwnerWordTypeNameListOutputDTO>> findPayOwnerWordTypeNameList(
			FindPayOwnerWordTypeNameListInputDTO inputDTO);

	ResponseDataNewPay<AddPayOwnerWordTypeOutputDTO> addPayOwnerWordType(AddPayOwnerWordTypeInputDTO inputDTO);

	ResponseDataNewPay removePayOwnerWordType(RemovePayOwnerWordTypeInputDTO inputDTO);

	ResponseDataNewPay<List<FindPayOwnerWordBindListOutputDTO>> findPayOwnerWordBindList(CommonInputDTO inputDTO);

	ResponseDataNewPay savePayOwnerWordBind(SavePayOwnerWordBindInputDTO inputDTO);
}
