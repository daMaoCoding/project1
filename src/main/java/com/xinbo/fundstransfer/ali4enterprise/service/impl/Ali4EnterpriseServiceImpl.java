package com.xinbo.fundstransfer.ali4enterprise.service.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinbo.fundstransfer.ali4enterprise.Ali4EnterpriseReqJson;
import com.xinbo.fundstransfer.ali4enterprise.inputdto.*;
import com.xinbo.fundstransfer.ali4enterprise.outputdto.*;
import com.xinbo.fundstransfer.ali4enterprise.service.Ali4EnterpriseService;
import com.xinbo.fundstransfer.component.net.http.ali4enterprise.OkHttpUtils;
import com.xinbo.fundstransfer.domain.ResponseDataNewPay;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class Ali4EnterpriseServiceImpl implements Ali4EnterpriseService {
	@Autowired
	private OkHttpUtils httpUtils;
	@Autowired
	private Ali4EnterpriseReqJson reqJson;
	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public ResponseDataNewPay addEpAisleDetail(AddEpAisleDetailInputDTO inputDTO) {
		ResponseDataNewPay responseDataNewPay = new ResponseDataNewPay();
		try {
			String url = "/payOwnerConfig/addEpAisleDetail";
			String json = reqJson.getAddEpAisleDetailReqJson(inputDTO);
			log.info("addEpAisleDetail json value:{}", json);
			if (StringUtils.isBlank(json)) {
				log.info("addEpAisleDetail json value is null");
				return responseDataNewPay;
			}
			String ret = httpUtils.post(url, json);
			if (StringUtils.isNotBlank(ret)) {
				responseDataNewPay = objectMapper.readValue(ret, new TypeReference<ResponseDataNewPay>() {
				});
				return responseDataNewPay;
			} else {
				log.info("addEpAisleDetail call url:{},result is :{}", url, ret);
				return responseDataNewPay;
			}
		} catch (Exception e) {
			log.error("addEpAisleDetail error:", e);
			return responseDataNewPay;
		}
	}

	@Override
	public ResponseDataNewPay<FindEpDetailListOutputDTO> findEpDetailList(CommonInputDTO inputDTO) {
		ResponseDataNewPay<FindEpDetailListOutputDTO> responseDataNewPay = new ResponseDataNewPay();
		try {
			String url = "/payOwnerConfig/findEpDetailList";
			String json = reqJson.getFindEpDetailListReqJson(inputDTO);
			log.info("findEpDetailList json value:{}", json);
			if (StringUtils.isBlank(json)) {
				log.info("findEpDetailList json value is null");
				return responseDataNewPay;
			}
			String ret = httpUtils.post(url, json);
			if (StringUtils.isNotBlank(ret)) {
				responseDataNewPay = objectMapper.readValue(ret,
						new TypeReference<ResponseDataNewPay<FindEpDetailListOutputDTO>>() {
						});
				return responseDataNewPay;
			} else {
				log.info("findEpDetailList call url:{},result is :{}", url, ret);
				return responseDataNewPay;
			}
		} catch (Exception e) {
			log.error("findEpDetailList error:", e);
			return responseDataNewPay;
		}
	}

	@Override
	public ResponseDataNewPay<FindEpDetailOutputDTO> findEpDetail(CommonInputDTO inputDTO) {
		ResponseDataNewPay<FindEpDetailOutputDTO> responseDataNewPay = new ResponseDataNewPay();
		try {
			String url = "/payOwnerConfig/findEpDetail";
			String json = reqJson.getFindEpDetailReqJson(inputDTO);
			log.info("findEpDetail json value:{}", json);
			if (StringUtils.isBlank(json)) {
				log.info("findEpDetail json value is null");
				return responseDataNewPay;
			}
			String ret = httpUtils.post(url, json);
			if (StringUtils.isNotBlank(ret)) {
				responseDataNewPay = objectMapper.readValue(ret,
						new TypeReference<ResponseDataNewPay<FindEpDetailOutputDTO>>() {
						});
				return responseDataNewPay;
			} else {
				log.info("findEpDetail call url:{},result is :{}", url, ret);
				return responseDataNewPay;
			}
		} catch (Exception e) {
			log.error("findEpDetail error:", e);
			return responseDataNewPay;
		}
	}

	@Override
	public ResponseDataNewPay<PageOutPutDTO<FindEpByConditionOutputDTO>> findEpByCondition(
			FindEpByConditionInputDTO inputDTO) {
		ResponseDataNewPay<PageOutPutDTO<FindEpByConditionOutputDTO>> responseDataNewPay = new ResponseDataNewPay();
		try {
			String url = "/payOwnerConfig/findEpByCondition";
			String json = reqJson.getFindEpByConditionReqJson(inputDTO);
			log.info("findEpByCondition json value:{}", json);
			if (StringUtils.isBlank(json)) {
				log.info("findEpByCondition json value is null");
				return responseDataNewPay;
			}
			String ret = httpUtils.post(url, json);
			if (StringUtils.isNotBlank(ret)) {
				responseDataNewPay = objectMapper.readValue(ret,
						new TypeReference<ResponseDataNewPay<PageOutPutDTO<FindEpByConditionOutputDTO>>>() {
						});
				return responseDataNewPay;
			} else {
				log.info("findEpByCondition call url:{},result is :{}", url, ret);
				return responseDataNewPay;
			}

		} catch (Exception e) {
			log.error("findEpByCondition error:", e);
			return responseDataNewPay;
		}

	}

	@Override
	public ResponseDataNewPay<List<FindEpAisleOutputDTO>> findEpAisle(FindEpAisleInputDTO inputDTO) {
		ResponseDataNewPay<List<FindEpAisleOutputDTO>> responseDataNewPay = new ResponseDataNewPay();
		try {
			String url = "/payOwnerConfig/findEpAisle";
			String json = reqJson.getFindEpAisleReqJson(inputDTO);
			log.info("findEpAisle json value:{}", json);
			if (StringUtils.isBlank(json)) {
				log.info("findEpAisle json value is null");
				return responseDataNewPay;
			}
			String ret = httpUtils.post(url, json);
			if (StringUtils.isNotBlank(ret)) {
				responseDataNewPay = objectMapper.readValue(ret,
						new TypeReference<ResponseDataNewPay<List<FindEpAisleOutputDTO>>>() {
						});
				return responseDataNewPay;
			} else {
				log.info("findEpAisle call url:{},result is :{}", url, ret);
				return responseDataNewPay;
			}

		} catch (Exception e) {
			log.error("findEpAisle error:", e);
			return responseDataNewPay;
		}

	}

	@Override
	public ResponseDataNewPay bindEpAisle(BindEpAisleInputDTO inputDTO) {
		ResponseDataNewPay responseDataNewPay = new ResponseDataNewPay();
		try {
			String url = "/payOwnerConfig/bindEpAisle";
			String json = reqJson.getBindEpAisleReqJson(inputDTO);
			log.info("bindEpAisle json value:{}", json);
			if (StringUtils.isBlank(json)) {
				log.info("bindEpAisle json value is null");
				return responseDataNewPay;
			}
			String ret = httpUtils.post(url, json);
			if (StringUtils.isNotBlank(ret)) {
				responseDataNewPay = objectMapper.readValue(ret, new TypeReference<ResponseDataNewPay>() {
				});
				return responseDataNewPay;
			} else {
				log.info("bindEpAisle call url:{},result is :{}", url, ret);
				return responseDataNewPay;
			}

		} catch (Exception e) {
			log.error("bindEpAisle error:", e);
			return responseDataNewPay;
		}

	}

	@Override
	public ResponseDataNewPay modifyEpAisleDetail(ModifyEpAisleDetailInputDTO inputDTO) {
		ResponseDataNewPay responseDataNewPay = new ResponseDataNewPay();
		try {
			String url = "/payOwnerConfig/modifyEpAisleDetail";
			String json = reqJson.getModifyEpAisleDetailReqJson(inputDTO);
			log.info("modifyEpAisleDetail json value:{}", json);
			if (StringUtils.isBlank(json)) {
				log.info("modifyEpAisleDetail json value is null");
				return responseDataNewPay;
			}
			String ret = httpUtils.post(url, json);
			if (StringUtils.isNotBlank(ret)) {
				responseDataNewPay = objectMapper.readValue(ret, new TypeReference<ResponseDataNewPay>() {
				});
				return responseDataNewPay;
			} else {
				log.info("modifyEpAisleDetail call url:{},result is :{}", url, ret);
				return responseDataNewPay;
			}

		} catch (Exception e) {
			log.error("modifyEpAisleDetail error:", e);
			return responseDataNewPay;
		}

	}

	@Override
	public ResponseDataNewPay cleanEpInData(CommonInputDTO inputDTO) {
		ResponseDataNewPay responseDataNewPay = new ResponseDataNewPay();
		try {
			String url = "/payOwnerConfig/cleanEpInData";
			String json = reqJson.getCleanEpInDataReqJson(inputDTO);
			log.info("cleanEpInData json value:{}", json);
			if (StringUtils.isBlank(json)) {
				log.info("cleanEpInData json value is null");
				return responseDataNewPay;
			}
			String ret = httpUtils.post(url, json);
			if (StringUtils.isNotBlank(ret)) {
				responseDataNewPay = objectMapper.readValue(ret, new TypeReference<ResponseDataNewPay>() {
				});
				return responseDataNewPay;
			} else {
				log.info("cleanEpInData call url:{},result is :{}", url, ret);
				return responseDataNewPay;
			}

		} catch (Exception e) {
			log.error("cleanEpInData error:", e);
			return responseDataNewPay;
		}

	}

	@Override
	public ResponseDataNewPay<FindEpStopAlarmOutputDTO> findEpStopAlarm(FindEpStopAlarmInputDTO inputDTO) {
		ResponseDataNewPay<FindEpStopAlarmOutputDTO> responseDataNewPay = new ResponseDataNewPay();
		try {
			String url = "/payOwnerConfig/findEpStopAlarm";
			String json = reqJson.getFindEpStopAlarmReqJson(inputDTO);
			log.info("findEpStopAlarm json value:{}", json);
			if (StringUtils.isBlank(json)) {
				log.info("findEpStopAlarm json value is null");
				return responseDataNewPay;
			}
			String ret = httpUtils.post(url, json);
			if (StringUtils.isNotBlank(ret)) {
				responseDataNewPay = objectMapper.readValue(ret,
						new TypeReference<ResponseDataNewPay<FindEpStopAlarmOutputDTO>>() {
						});
				return responseDataNewPay;
			} else {
				log.info("findEpStopAlarm call url:{},result is :{}", url, ret);
				return responseDataNewPay;
			}

		} catch (Exception e) {
			log.error("findEpStopAlarm error:", e);
			return responseDataNewPay;
		}
	}

	@Override
	public ResponseDataNewPay modifyEpStopAlarm(ModifyEpStopAlarmInputDTO inputDTO) {
		ResponseDataNewPay responseDataNewPay = new ResponseDataNewPay();
		try {
			String url = "/payOwnerConfig/modifyEpStopAlarm";
			String json = reqJson.getModifyEpStopAlarmReqJson(inputDTO);
			log.info("modifyEpStopAlarm json value:{}", json);
			if (StringUtils.isBlank(json)) {
				log.info("modifyEpStopAlarm json value is null");
				return responseDataNewPay;
			}
			String ret = httpUtils.post(url, json);
			if (StringUtils.isNotBlank(ret)) {
				responseDataNewPay = objectMapper.readValue(ret, new TypeReference<ResponseDataNewPay>() {
				});
				return responseDataNewPay;
			} else {
				log.info("modifyEpStopAlarm call url:{},result is :{}", url, ret);
				return responseDataNewPay;
			}

		} catch (Exception e) {
			log.error("modifyEpStopAlarm error:", e);
			return responseDataNewPay;
		}

	}

	@Override
	public ResponseDataNewPay<ModifyEpStatusOutputDTO> modifyEpStatus(ModifyEpStatusInputDTO inputDTO) {
		ResponseDataNewPay<ModifyEpStatusOutputDTO> responseDataNewPay = new ResponseDataNewPay();
		try {
			String url = "/payOwnerConfig/modifyEpStatus";
			String json = reqJson.getModifyEpStatusReqJson(inputDTO);
			log.info("modifyEpStatus json value:{}", json);
			if (StringUtils.isBlank(json)) {
				log.info("modifyEpStatus json value is null");
				return responseDataNewPay;
			}
			String ret = httpUtils.post(url, json);
			if (StringUtils.isNotBlank(ret)) {
				responseDataNewPay = objectMapper.readValue(ret,
						new TypeReference<ResponseDataNewPay<ModifyEpStatusOutputDTO>>() {
						});
				return responseDataNewPay;
			} else {
				log.info("modifyEpStatus call url:{},result is :{}", url, ret);
				return responseDataNewPay;
			}

		} catch (Exception e) {
			log.error("modifyEpStatus error:", e);
			return responseDataNewPay;
		}

	}

	@Override
	public ResponseDataNewPay<PageOutPutDTO<FindPayOwnerWordListOutputDTO>> findPayOwnerWordList(
			FindPayOwnerWordListInputDTO inputDTO) {
		ResponseDataNewPay<PageOutPutDTO<FindPayOwnerWordListOutputDTO>> responseDataNewPay = new ResponseDataNewPay();
		try {
			String url = "/payOwnerWord/findPayOwnerWordList";
			String json = reqJson.getFindPayOwnerWordListReqJson(inputDTO);
			log.info("findPayOwnerWordList json value:{}", json);
			if (StringUtils.isBlank(json)) {
				log.info("findPayOwnerWordList json value is null");
				return responseDataNewPay;
			}
			String ret = httpUtils.post(url, json);
			if (StringUtils.isNotBlank(ret)) {
				responseDataNewPay = objectMapper.readValue(ret,
						new TypeReference<ResponseDataNewPay<PageOutPutDTO<FindPayOwnerWordListOutputDTO>>>() {
						});
				return responseDataNewPay;
			} else {
				log.info("findPayOwnerWordList call url:{},result is :{}", url, ret);
				return responseDataNewPay;
			}

		} catch (Exception e) {
			log.error("findPayOwnerWordList error:", e);
			return responseDataNewPay;
		}
	}

	@Override
	public ResponseDataNewPay addPayOwnerWord(AddPayOwnerWordInputDTO inputDTO) {
		ResponseDataNewPay responseDataNewPay = new ResponseDataNewPay();
		try {
			String url = "/payOwnerWord/addPayOwnerWord";
			String json = reqJson.getAddPayOwnerWordReqJson(inputDTO);
			log.info("addPayOwnerWord json value:{}", json);
			if (StringUtils.isBlank(json)) {
				log.info("addPayOwnerWord json value is null");
				return responseDataNewPay;
			}
			String ret = httpUtils.post(url, json);
			if (StringUtils.isNotBlank(ret)) {
				responseDataNewPay = objectMapper.readValue(ret, new TypeReference<ResponseDataNewPay>() {
				});
				return responseDataNewPay;
			} else {
				log.info("addPayOwnerWord call url:{},result is :{}", url, ret);
				return responseDataNewPay;
			}
		} catch (Exception e) {
			log.error("addPayOwnerWord error:", e);
			return responseDataNewPay;
		}

	}

	@Override
	public ResponseDataNewPay<ModifyPayOwnerWordOutputDTO> modifyPayOwnerWord(ModifyPayOwnerWordInputDTO inputDTO) {
		ResponseDataNewPay<ModifyPayOwnerWordOutputDTO> responseDataNewPay = new ResponseDataNewPay();
		try {
			String url = "/payOwnerWord/modifyPayOwnerWord";
			String json = reqJson.getModifyPayOwnerWordReqJson(inputDTO);
			log.info("modifyPayOwnerWord json value:{}", json);
			if (StringUtils.isBlank(json)) {
				log.info("modifyPayOwnerWord json value is null");
				return responseDataNewPay;
			}
			String ret = httpUtils.post(url, json);
			if (StringUtils.isNotBlank(ret)) {
				responseDataNewPay = objectMapper.readValue(ret,
						new TypeReference<ResponseDataNewPay<ModifyPayOwnerWordOutputDTO>>() {
						});
				return responseDataNewPay;
			} else {
				log.info("modifyPayOwnerWord call url:{},result is :{}", url, ret);
				return responseDataNewPay;
			}

		} catch (Exception e) {
			log.error("modifyPayOwnerWord error:", e);
			return responseDataNewPay;
		}
	}

	@Override
	public ResponseDataNewPay removePayOwnerWord(RemovePayOwnerWordInputDTO inputDTO) {
		ResponseDataNewPay responseDataNewPay = new ResponseDataNewPay();
		try {
			String url = "/payOwnerWord/removePayOwnerWord";
			String json = reqJson.getRemovePayOwnerWordReqJson(inputDTO);
			log.info("removePayOwnerWord json value:{}", json);
			if (StringUtils.isBlank(json)) {
				log.info("removePayOwnerWord json value is null");
				return responseDataNewPay;
			}
			String ret = httpUtils.post(url, json);
			if (StringUtils.isNotBlank(ret)) {
				responseDataNewPay = objectMapper.readValue(ret, new TypeReference<ResponseDataNewPay>() {
				});
				return responseDataNewPay;
			} else {
				log.info("removePayOwnerWord call url:{},result is :{}", url, ret);
				return responseDataNewPay;
			}

		} catch (Exception e) {
			log.error("removePayOwnerWord error:", e);
			return responseDataNewPay;
		}
	}

	@Override
	public ResponseDataNewPay<PageOutPutDTO<FindPayOwnerWordTypeListOutputDTO>> findPayOwnerWordTypeList(
			FindPayOwnerWordTypeListInputDTO inputDTO) {
		ResponseDataNewPay<PageOutPutDTO<FindPayOwnerWordTypeListOutputDTO>> responseDataNewPay = new ResponseDataNewPay();
		try {
			String url = "/payOwnerWord/findPayOwnerWordTypeList";
			String json = reqJson.getFindPayOwnerWordTypeListReqJson(inputDTO);
			log.info("findPayOwnerWordTypeList json value:{}", json);
			if (StringUtils.isBlank(json)) {
				log.info("findPayOwnerWordTypeList json value is null");
				return responseDataNewPay;
			}
			String ret = httpUtils.post(url, json);
			if (StringUtils.isNotBlank(ret)) {
				responseDataNewPay = objectMapper.readValue(ret,
						new TypeReference<ResponseDataNewPay<PageOutPutDTO<FindPayOwnerWordListOutputDTO>>>() {
						});
				return responseDataNewPay;
			} else {
				log.info("findPayOwnerWordTypeList call url:{},result is :{}", url, ret);
				return responseDataNewPay;
			}

		} catch (Exception e) {
			log.error("findPayOwnerWordTypeList error:", e);
			return responseDataNewPay;
		}
	}

	@Override
	public ResponseDataNewPay<List<FindPayOwnerWordTypeNameListOutputDTO>> findPayOwnerWordTypeNameList(
			FindPayOwnerWordTypeNameListInputDTO inputDTO) {
		ResponseDataNewPay<List<FindPayOwnerWordTypeNameListOutputDTO>> responseDataNewPay = new ResponseDataNewPay();
		try {
			String url = "/payOwnerWord/findPayOwnerWordTypeNameList";
			String json = reqJson.getFindPayOwnerWordTypeNameListReqJson(inputDTO);
			log.info("findPayOwnerWordTypeNameList json value:{}", json);
			if (StringUtils.isBlank(json)) {
				log.info("findPayOwnerWordTypeNameList json value is null");
				return responseDataNewPay;
			}
			String ret = httpUtils.post(url, json);
			if (StringUtils.isNotBlank(ret)) {
				responseDataNewPay = objectMapper.readValue(ret,
						new TypeReference<ResponseDataNewPay<List<FindPayOwnerWordTypeNameListOutputDTO>>>() {
						});
				return responseDataNewPay;
			} else {
				log.info("findPayOwnerWordTypeNameList call url:{},result is :{}", url, ret);
				return responseDataNewPay;
			}
		} catch (Exception e) {
			log.error("findPayOwnerWordTypeNameList error:", e);
			return responseDataNewPay;
		}
	}

	@Override
	public ResponseDataNewPay<AddPayOwnerWordTypeOutputDTO> addPayOwnerWordType(AddPayOwnerWordTypeInputDTO inputDTO) {
		ResponseDataNewPay<AddPayOwnerWordTypeOutputDTO> responseDataNewPay = new ResponseDataNewPay();
		try {
			String url = "/payOwnerWord/addPayOwnerWordType";
			String json = reqJson.getAddPayOwnerWordTypeReqJson(inputDTO);
			log.info("addPayOwnerWordType json value:{}", json);
			if (StringUtils.isBlank(json)) {
				log.info("addPayOwnerWordType json value is null");
				return responseDataNewPay;
			}
			String ret = httpUtils.post(url, json);
			if (StringUtils.isNotBlank(ret)) {
				responseDataNewPay = objectMapper.readValue(ret,
						new TypeReference<ResponseDataNewPay<AddPayOwnerWordTypeOutputDTO>>() {
						});
				return responseDataNewPay;
			} else {
				log.info("addPayOwnerWordType call url:{},result is :{}", url, ret);
				return responseDataNewPay;
			}

		} catch (Exception e) {
			log.error("addPayOwnerWordType error:", e);
			return responseDataNewPay;
		}
	}

	@Override
	public ResponseDataNewPay removePayOwnerWordType(RemovePayOwnerWordTypeInputDTO inputDTO) {
		ResponseDataNewPay responseDataNewPay = new ResponseDataNewPay();
		try {
			String url = "/payOwnerWord/removePayOwnerWordType";
			String json = reqJson.getRemovePayOwnerWordTypeReqJson(inputDTO);
			log.info("removePayOwnerWordType json value:{}", json);
			if (StringUtils.isBlank(json)) {
				log.info("removePayOwnerWordType json value is null");
				return responseDataNewPay;
			}
			String ret = httpUtils.post(url, json);
			if (StringUtils.isNotBlank(ret)) {
				responseDataNewPay = objectMapper.readValue(ret, new TypeReference<ResponseDataNewPay>() {
				});
				return responseDataNewPay;
			} else {
				log.info("removePayOwnerWordType call url:{},result is :{}", url, ret);
				return responseDataNewPay;
			}
		} catch (Exception e) {
			log.error("removePayOwnerWordType error:", e);
			return responseDataNewPay;
		}
	}

	@Override
	public ResponseDataNewPay<List<FindPayOwnerWordBindListOutputDTO>> findPayOwnerWordBindList(
			CommonInputDTO inputDTO) {
		ResponseDataNewPay<List<FindPayOwnerWordBindListOutputDTO>> responseDataNewPay = new ResponseDataNewPay();
		try {
			String url = "/payOwnerWord/findPayOwnerWordBindList";
			String json = reqJson.getFindPayOwnerWordBindListReqJson(inputDTO);
			log.info("findPayOwnerWordBindList json value:{}", json);
			if (StringUtils.isBlank(json)) {
				log.info("findPayOwnerWordBindList json value is null");
				return responseDataNewPay;
			}
			String ret = httpUtils.post(url, json);
			if (StringUtils.isNotBlank(ret)) {
				responseDataNewPay = objectMapper.readValue(ret,
						new TypeReference<ResponseDataNewPay<List<FindPayOwnerWordBindListOutputDTO>>>() {
						});
				return responseDataNewPay;
			} else {
				log.info("findPayOwnerWordBindList call url:{},result is :{}", url, ret);
				return responseDataNewPay;
			}

		} catch (Exception e) {
			log.error("findPayOwnerWordBindList error:", e);
			return responseDataNewPay;
		}
	}

	@Override
	public ResponseDataNewPay savePayOwnerWordBind(SavePayOwnerWordBindInputDTO inputDTO) {
		ResponseDataNewPay responseDataNewPay = new ResponseDataNewPay();
		try {
			String url = "/payOwnerWord/savePayOwnerWordBind";
			String json = reqJson.getSavePayOwnerWordBindReqJson(inputDTO);
			log.info("savePayOwnerWordBind json value:{}", json);
			if (StringUtils.isBlank(json)) {
				log.info("savePayOwnerWordBind json value is null");
				return responseDataNewPay;
			}
			String ret = httpUtils.post(url, json);
			if (StringUtils.isNotBlank(ret)) {
				responseDataNewPay = objectMapper.readValue(ret, new TypeReference<ResponseDataNewPay>() {
				});
				return responseDataNewPay;
			} else {
				log.info("savePayOwnerWordBind call url:{},result is :{}", url, ret);
				return responseDataNewPay;
			}

		} catch (Exception e) {
			log.error("savePayOwnerWordBind error:", e);
			return responseDataNewPay;
		}
	}

	@Override
	public ResponseDataNewPay findPayOwnerWordSta(FindPayOwnerWordStaInputDTO inputDTO) {
		ResponseDataNewPay responseDataNewPay = new ResponseDataNewPay();
		try {
			String url = "/payOwnerWord/findPayOwnerWordSta";
			String json = reqJson.getFindPayOwnerWordStaReqJson(inputDTO);
			log.info("findPayOwnerWordSta json value:{}", json);
			if (StringUtils.isBlank(json)) {
				log.info("findPayOwnerWordSta json value is null");
				return responseDataNewPay;
			}
			String ret = httpUtils.post(url, json);
			if (StringUtils.isNotBlank(ret)) {
				responseDataNewPay = objectMapper.readValue(ret, new TypeReference<ResponseDataNewPay>() {
				});
				return responseDataNewPay;
			} else {
				log.info("findPayOwnerWordSta call url:{},result is :{}", url, ret);
				return responseDataNewPay;
			}

		} catch (Exception e) {
			log.error("findPayOwnerWordSta error:", e);
			return responseDataNewPay;
		}
	}
}
