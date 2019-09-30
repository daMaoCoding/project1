package com.xinbo.fundstransfer.domain.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class OrderStatusQueryInputDTO implements Serializable {
	@NotNull
	private List<OrderStatusQueryInputDetail> list;

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown = true)
	@Data
	public static class OrderStatusQueryInputDetail implements Serializable {
		@NotNull
		private String orderNo;
		@NotNull
		private Byte type;
	}

}
