package com.xinbo.fundstransfer.ali4enterprise;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.xinbo.fundstransfer.ali4enterprise.inputdto.*;

@Component
public class Ali4EnterpriseReqJson {
	public final String getAddEpAisleDetailReqJson(AddEpAisleDetailInputDTO inputDTO) {
		String jsonStr = "{\"oid\":" + inputDTO.getOid();
		if (inputDTO.getId() != null) {
			jsonStr += ",\"id\":" + inputDTO.getId();
		}
		return commonJson(inputDTO, jsonStr);
	}

	public final String getFindEpDetailListReqJson(CommonInputDTO inputDTO) {
		String jsonStr = "{\"oid\":" + inputDTO.getOid() + ",\"id\":" + inputDTO.getId() + "}";
		return jsonStr;
	}

	public final String getFindEpDetailReqJson(CommonInputDTO inputDTO) {
		String jsonStr = "{\"oid\":" + inputDTO.getOid() + ",\"id\":" + inputDTO.getId() + "}";
		return jsonStr;
	}

	public final String getFindEpByConditionReqJson(FindEpByConditionInputDTO inputDTO) {
		String jsonStr = "{\"oid\":" + inputDTO.getOid() + ",\"pageNo\":" + inputDTO.getPageNo() + ",\"pageSize\":"
				+ inputDTO.getPageSize();

		if (inputDTO.getCreateTimeStart() != null) {
			jsonStr += ",\"createTimeStart\":" + inputDTO.getCreateTimeStart();
		}
		if (inputDTO.getCreateTimeEnd() != null) {
			jsonStr += ",\"createTimeEnd\":" + inputDTO.getCreateTimeEnd();
		}
		if (StringUtils.isNotBlank(inputDTO.getName())) {
			jsonStr += ",\"name\":\"" + inputDTO.getName() + "\"";
		}
		if (StringUtils.isNotBlank(inputDTO.getCode())) {
			jsonStr += ",\"code\":\"" + inputDTO.getCode() + "\"";
		}
		if (inputDTO.getMinMoneyStart() != null) {
			jsonStr += ",\"minMoneyStart\":" + inputDTO.getMinMoneyStart();
		}
		if (inputDTO.getMinMoneyStart() != null) {
			jsonStr += ",\"minMoneyEnd\":" + inputDTO.getMinMoneyEnd();
		}
		if (inputDTO.getMaxMoneyStart() != null) {
			jsonStr += ",\"maxMoneyStart\":" + inputDTO.getMaxMoneyStart();
		}
		if (inputDTO.getMaxMoneyEnd() != null) {
			jsonStr += ",\"maxMoneyEnd\":" + inputDTO.getMaxMoneyEnd();
		}
		if (inputDTO.getStopMoneyStart() != null) {
			jsonStr += ",\"stopMoneyStart\":" + inputDTO.getStopMoneyStart();
		}
		if (inputDTO.getStopMoneyEnd() != null) {
			jsonStr += ",\"stopMoneyEnd\":" + inputDTO.getStopMoneyEnd();
		}
		if (inputDTO.getStatus() != null) {
			jsonStr += ",\"status\":" + inputDTO.getStatus();
		}
		if (inputDTO.getAisleId() != null) {
			jsonStr += ",\"aisleId\":" + inputDTO.getAisleId();
		}
		if (StringUtils.isNotBlank(inputDTO.getAisleName())) {
			jsonStr += ",\"aisleName\":\"" + inputDTO.getAisleName() + "\"";
		}
		if (StringUtils.isNotBlank(inputDTO.getOrderField())) {
			jsonStr += ",\"orderField\":\"" + inputDTO.getOrderField() + "\"";
		}
		if (StringUtils.isNotBlank(inputDTO.getOrderSort())) {
			jsonStr += ",\"orderSort\":\"" + inputDTO.getOrderSort() + "\"";
		}
		if (inputDTO.getBingFlag() != null) {
			jsonStr += ",\"bingFlag\":\"" + inputDTO.getBingFlag() + "\"";
		}
		if (StringUtils.isNotBlank(inputDTO.getOnlyCode())) {
			jsonStr += ",\"onlyCode\":\"" + inputDTO.getOnlyCode() + "\"";
		}
		jsonStr += "}";
		return jsonStr;
	}

	public final String getFindEpAisleReqJson(FindEpAisleInputDTO inputDTO) {
		String jsonStr = "{\"oid\":" + inputDTO.getOid();
		if (StringUtils.isNotBlank(inputDTO.getName())) {
			jsonStr += ",\"name\":\"" + inputDTO.getName() + "\"";
		}
		jsonStr += "}";
		return jsonStr;
	}

	public final String getBindEpAisleReqJson(BindEpAisleInputDTO inputDTO) {
		String jsonStr = "{\"oid\":" + inputDTO.getOid() + ",\"id\":" + inputDTO.getId() + ",\"flag\":"
				+ inputDTO.getFlag();
		if (!CollectionUtils.isEmpty(inputDTO.getIdList())) {
			jsonStr += ",\"idList\":[";
			List<Long> list = inputDTO.getIdList();
			for (int i = 0, size = list.size(); i < size; i++) {
				if (i < size - 1) {
					jsonStr += list.get(i) + ",";
				} else {
					jsonStr += list.get(i) + "]";
				}
			}
		}
		jsonStr += ",\"operationAdminId\":" + inputDTO.getOperationAdminId() + ",\"operationAdminName\":\""
				+ inputDTO.getOperationAdminName() + "\"}";
		return jsonStr;
	}

	public final String getModifyEpAisleDetailReqJson(ModifyEpAisleDetailInputDTO inputDTO) {
		String jsonStr = "{\"oid\":" + inputDTO.getOid() + ",\"id\":" + inputDTO.getId();
		return commonJson(inputDTO, jsonStr);
	}

	public final String getCleanEpInDataReqJson(CommonInputDTO inputDTO) {
		String jsonStr = "{\"oid\":" + inputDTO.getOid() + ",\"id\":" + inputDTO.getId() + "}";
		return jsonStr;
	}

	public final String getFindEpStopAlarmReqJson(FindEpStopAlarmInputDTO inputDTO) {
		String jsonStr = "{\"oid\":" + inputDTO.getOid() + ",\"id\":" + inputDTO.getId() + "}";
		return jsonStr;
	}

	public final String getModifyEpStopAlarmReqJson(ModifyEpStopAlarmInputDTO inputDTO) {
		String jsonStr = "{\"oid\":" + inputDTO.getOid() + ",\"id\":" + inputDTO.getId();
		if (inputDTO.getMoney() != null) {
			jsonStr += ",\"money\":" + inputDTO.getMoney();
		}
		if (inputDTO.getRate() != null) {
			jsonStr += ",\"rate\":" + inputDTO.getRate();
		}
		jsonStr += "}";
		return jsonStr;
	}

	public final String getModifyEpStatusReqJson(ModifyEpStatusInputDTO inputDTO) {
		String jsonStr = "{\"oid\":" + inputDTO.getOid() + ",\"id\":" + inputDTO.getId() + ",\"status\":"
				+ inputDTO.getStatus() + "}";
		return jsonStr;
	}

	private String commonJson(CommonFor1A8InputDTO inputDTO, String jsonStr) {
		jsonStr += ",\"name1\":\"" + inputDTO.getName1() + "\",\"code\":\"" + inputDTO.getCode() + "\",\"epKey\":\""
				+ inputDTO.getEpKey() + "\",\"epUrl\":\"" + inputDTO.getEpUrl() + "\",\"epUrl1\":\""
				+ inputDTO.getEpUrl1() + "\",\"proxyPort\":\"" + inputDTO.getProxyPort() + "\",\"pubKey\":\""
				+ inputDTO.getPubKey() + "\",\"apiGateway\":\"" + inputDTO.getApiGateway() + "\"";
		if (inputDTO.getMaxMoney() != null) {
			jsonStr += ",\"maxMoney\": " + inputDTO.getMaxMoney();
		}
		if (inputDTO.getMinMoney() != null) {
			jsonStr += ",\"minMoney\": " + inputDTO.getMinMoney();
		}
		if (inputDTO.getStopMoney() != null) {
			jsonStr += ",\"stopMoney\": " + inputDTO.getStopMoney();
		}
		jsonStr += "}";
		return jsonStr;
	}

	public final String getFindPayOwnerWordListReqJson(FindPayOwnerWordListInputDTO inputDTO) {
		String jsonStr = "{\"oid\":" + inputDTO.getOid() + ",\"pageNo\":" + inputDTO.getPageNo() + ",\"pageSize\":"
				+ inputDTO.getPageSize();

		if (inputDTO.getTypeId() != null) {
			jsonStr += ",\"typeId\":" + inputDTO.getTypeId();
		}
		if (StringUtils.isNotBlank(inputDTO.getContent())) {
			jsonStr += ",\"content\":\"" + inputDTO.getContent() + "\"";
		}
		if (StringUtils.isNotBlank(inputDTO.getTypeName())) {
			jsonStr += ",\"typeName\":\"" + inputDTO.getTypeName() + "\"";
		}
		if (StringUtils.isNotBlank(inputDTO.getAdminName())) {
			jsonStr += ",\"adminName\":\"" + inputDTO.getAdminName() + "\"";
		}
		if (inputDTO.getAdminTimeStart() != null) {
			jsonStr += ",\"adminTimeStart\":" + inputDTO.getAdminTimeStart();
		}
		if (inputDTO.getAdminTimeEnd() != null) {
			jsonStr += ",\"adminTimeEnd\":" + inputDTO.getAdminTimeEnd();
		}
		if (StringUtils.isNotBlank(inputDTO.getOrderField())) {
			jsonStr += ",\"orderField\":\"" + inputDTO.getOrderField() + "\"";
		}
		if (StringUtils.isNotBlank(inputDTO.getOrderSort())) {
			jsonStr += ",\"orderSort\":\"" + inputDTO.getOrderSort() + "\"";
		}
		jsonStr += "}";
		return jsonStr;
	}

	public final String getAddPayOwnerWordReqJson(AddPayOwnerWordInputDTO inputDTO) {
		String jsonStr = "{\"oid\":" + inputDTO.getOid() + ",\"typeId\":" + inputDTO.getTypeId() + ",\"content\":\""
				+ inputDTO.getContent() + "\",\"operationAdminId\":" + inputDTO.getOperationAdminId()
				+ ",\"operationAdminName\":\"" + inputDTO.getOperationAdminName() + "\"}";
		return jsonStr;
	}

	public final String getModifyPayOwnerWordReqJson(ModifyPayOwnerWordInputDTO inputDTO) {
		String jsonStr = "{\"oid\":" + inputDTO.getOid() + ",\"typeId\":" + inputDTO.getTypeId() + ",\"content\":\""
				+ inputDTO.getContent() + "\",\"id\":" + inputDTO.getId() + ",\"operationAdminId\":"
				+ inputDTO.getOperationAdminId() + ",\"operationAdminName\":\"" + inputDTO.getOperationAdminName()
				+ "\"}";
		return jsonStr;
	}

	public final String getFindPayOwnerWordTypeListReqJson(FindPayOwnerWordTypeListInputDTO inputDTO) {
		String jsonStr = "{\"oid\":" + inputDTO.getOid() + ",\"pageNo\":" + inputDTO.getPageNo() + ",\"pageSize\":"
				+ inputDTO.getPageSize();
		if (inputDTO.getTypeId() != null) {
			jsonStr += ",\"typeId\":" + inputDTO.getTypeId();
		}
		if (StringUtils.isNotBlank(inputDTO.getAdminName())) {
			jsonStr += ",\"adminName\":\"" + inputDTO.getAdminName() + "\"";
		}
		if (inputDTO.getAdminTimeStart() != null) {
			jsonStr += ",\"adminTimeStart\":" + inputDTO.getAdminTimeStart();
		}
		if (inputDTO.getAdminTimeEnd() != null) {
			jsonStr += ",\"adminTimeEnd\":" + inputDTO.getAdminTimeEnd();
		}
		if (StringUtils.isNotBlank(inputDTO.getOrderField())) {
			jsonStr += ",\"orderField\":\"" + inputDTO.getOrderField() + "\"";
		}
		if (StringUtils.isNotBlank(inputDTO.getOrderSort())) {
			jsonStr += ",\"orderSort\":\"" + inputDTO.getOrderSort() + "\"";
		}
		jsonStr += "}";
		return jsonStr;
	}

	public final String getRemovePayOwnerWordReqJson(CommonInputDTO inputDTO) {
		String jsonStr = "{\"oid\":" + inputDTO.getOid() + ",\"id\":" + inputDTO.getId() + "}";
		return jsonStr;
	}

	public final String getFindPayOwnerWordTypeNameListReqJson(FindPayOwnerWordTypeNameListInputDTO inputDTO) {
		String jsonStr = "{\"oid\":" + inputDTO.getOid() + "}";
		return jsonStr;
	}

	public final String getAddPayOwnerWordTypeReqJson(AddPayOwnerWordTypeInputDTO inputDTO) {
		String jsonStr = "{\"oid\":" + inputDTO.getOid() + ",\"typeName\":\"" + inputDTO.getTypeName()
				+ "\",\"operationAdminId\":" + inputDTO.getOperationAdminId() + ",\"operationAdminName\":\""
				+ inputDTO.getOperationAdminName() + "\"}";
		return jsonStr;
	}

	public final String getRemovePayOwnerWordTypeReqJson(RemovePayOwnerWordTypeInputDTO inputDTO) {
		String jsonStr = "{\"oid\":" + inputDTO.getOid() + ",\"id\":" + inputDTO.getId() + "}";
		return jsonStr;
	}

	public final String getFindPayOwnerWordBindListReqJson(CommonInputDTO inputDTO) {
		String jsonStr = "{\"oid\":" + inputDTO.getOid() + ",\"id\":" + inputDTO.getId() + "}";
		return jsonStr;
	}

	public final String getSavePayOwnerWordBindReqJson(SavePayOwnerWordBindInputDTO inputDTO) {
		String jsonStr = "{\"oid\":" + inputDTO.getOid() + ",\"id\":" + inputDTO.getId();
		if (!CollectionUtils.isEmpty(inputDTO.getTypeIdList())) {
			jsonStr += ",\"typeIdList\":[";
			List<Long> list = inputDTO.getTypeIdList();
			for (int i = 0, size = list.size(); i < size; i++) {
				if (i < size - 1) {
					jsonStr += list.get(i) + ",";
				} else {
					jsonStr += list.get(i) + "]";
				}
			}
		}
		jsonStr += " }";
		return jsonStr;
	}

	public final String getFindPayOwnerWordStaReqJson(FindPayOwnerWordStaInputDTO inputDTO) {
		String jsonStr = "{\"oid\":" + inputDTO.getOid() + "}";
		return jsonStr;
	}
}
