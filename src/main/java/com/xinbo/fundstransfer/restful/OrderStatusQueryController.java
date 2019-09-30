package com.xinbo.fundstransfer.restful;

import com.xinbo.fundstransfer.domain.GeneralResponseData;
import com.xinbo.fundstransfer.domain.entity.BizIncomeRequest;
import com.xinbo.fundstransfer.domain.entity.BizOutwardRequest;
import com.xinbo.fundstransfer.domain.entity.BizOutwardTask;
import com.xinbo.fundstransfer.domain.enums.IncomeRequestStatus;
import com.xinbo.fundstransfer.domain.enums.OutwardRequestStatus;
import com.xinbo.fundstransfer.domain.pojo.OrderStatusQueryInputDTO;
import com.xinbo.fundstransfer.domain.pojo.OrderStatusQueryOutputDTO;
import com.xinbo.fundstransfer.service.IncomeRequestService;
import com.xinbo.fundstransfer.service.OutwardRequestService;
import com.xinbo.fundstransfer.service.OutwardTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/order")
public class OrderStatusQueryController {

	@Autowired
	private IncomeRequestService requestService;
	@Autowired
	private OutwardRequestService outwardRequestService;
	@Autowired
	private OutwardTaskService taskService;

	@PostMapping("/status")
	public GeneralResponseData<List<OrderStatusQueryOutputDTO>> statusQuery(
			@Validated @RequestBody OrderStatusQueryInputDTO inputDTO, Errors errors) {
		if (errors.hasErrors()) {
			return new GeneralResponseData<>(-1, "参数必传!");
		}
		if (CollectionUtils.isEmpty(inputDTO.getList())) {
			return new GeneralResponseData<>(-1, "参数必传!");
		}
		final Byte queryInOrder = 1;
		final Byte queryOutOrder = 2;
		GeneralResponseData<List<OrderStatusQueryOutputDTO>> responseData = new GeneralResponseData<>(1, "查询成功!");
		List<OrderStatusQueryOutputDTO> res = new ArrayList<>();
		for (OrderStatusQueryInputDTO.OrderStatusQueryInputDetail detail : inputDTO.getList()) {
			if (queryInOrder.equals(detail.getType())) {
				List<BizIncomeRequest> income = requestService.findByOrderNo(detail.getOrderNo());
				if (!CollectionUtils.isEmpty(income)) {
					income.stream().forEach(p -> {
						OrderStatusQueryOutputDTO outputDTO = new OrderStatusQueryOutputDTO();
						outputDTO.setOrderNo(p.getOrderNo());
						outputDTO.setStatus(p.getStatus().byteValue());
						outputDTO.setStatusDesc(IncomeRequestStatus.findByStatus(p.getStatus()).getMsg());
						outputDTO.setType(detail.getType());
						res.add(outputDTO);
					});
				}
			}
			if (queryOutOrder.equals(detail.getType())) {
				BizOutwardRequest request = outwardRequestService.findByOrderNo(detail.getOrderNo());
				if (request != null) {
					List<BizOutwardTask> tasks = taskService.findByRequestId(request.getId());
					Byte status = CollectionUtils.isEmpty(tasks) ? request.getStatus().byteValue() : (byte) 7;
					String statusDesc = CollectionUtils.isEmpty(tasks)
							? OutwardRequestStatus.findByStatus(request.getStatus()).getMsg()
							: "出款处理中";
					OrderStatusQueryOutputDTO outputDTO = new OrderStatusQueryOutputDTO();
					outputDTO.setOrderNo(request.getOrderNo());
					outputDTO.setStatus(status);
					outputDTO.setStatusDesc(statusDesc);
					outputDTO.setType(detail.getType());
					res.add(outputDTO);
				}
			}
		}
		responseData.setData(res);
		return responseData;
	}
}
